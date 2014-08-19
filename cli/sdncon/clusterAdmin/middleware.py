#
# Copyright (c) 2013 Big Switch Networks, Inc.
#
# Licensed under the Eclipse Public License, Version 1.0 (the
# "License"); you may not use this file except in compliance with the
# License. You may obtain a copy of the License at
#
#      http://www.eclipse.org/legal/epl-v10.html
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
# implied. See the License for the specific language governing
# permissions and limitations under the License.
#

import sys, re
from django.conf import settings
from django.contrib.auth.views import login
from django.http import HttpResponseRedirect, HttpResponse
from django.utils import simplejson
from django.contrib import auth
from django.contrib.auth.models import User, AnonymousUser

from .utils import isCloudBuild
from .models import CustomerUser, Customer, Cluster, AuthToken

import logging
debugLevel = logging.INFO
logfile = None

# For testing, uncomment as required
#debugLevel = logging.DEBUG
#logfile = 'middleware.log'

def initLogger():
    logger = logging.getLogger('middleware')
    formatter = logging.Formatter('%(asctime)s [%(name)s] %(levelname)s %(message)s')
    logger.setLevel(debugLevel)

    # Add a file handler
    if logfile:
        file_handler = logging.FileHandler(logfile)
        file_handler.setFormatter(formatter)
        logger.addHandler(file_handler)

    # Add a console handler
    console_handler = logging.StreamHandler()
    console_handler.setFormatter(formatter)
    console_handler.setLevel(debugLevel)
    logger.addHandler(console_handler)
    return logger

logger = initLogger()

def is_localhost(request):
    return request.META['REMOTE_ADDR'] in ['127.0.0.1', '127.0.1.1', 'localhost']

class RequireAuthMiddleware(object):
    """RequireAuthMiddleware: Middleware to enforce authentication

    If it is enabled, every Django-powered page, except LOGIN_URL and the list of EXEMPT_URLS,
    will require authentication

    Unautenticated user requests are redirected to the login page set (LOGIN_URL in settings)
    Unautenticated REST calls which are returned a JSON error as a 403 forbidden response
    """
    
    def __init__(self):
        self.enforce_auth = isCloudBuild() # For now, enforce authentication only if we are a cloud instance
        self.login_url = getattr(settings, 'LOGIN_URL', '/accounts/login/')
        self.exempt_urls = [self.login_url]
        self.exempt_urls += getattr(settings, 'EXEMPT_URLS', [] )
        self.rest_prefix = getattr(settings, 'REST_PREFIX', '/rest/')
        if self.enforce_auth:
            logger.info('RequireAuthMiddleware: Enforcing Authentication')
    
    def process_request(self, request):
        if self.enforce_auth:
            if is_localhost(request):
                return None
            if request.user.is_anonymous():
                for url in self.exempt_urls:
                    if request.path.startswith(url):
                        return None
                return self.redirect(request)
        return None

    def process_response(self, request, response):
        if request.path.startswith(self.login_url):
            response['x-bsc-auth-status'] = 'required'
        return response    

    def redirect(self, request):
        if self.rest_prefix in request.path:
            logger.warn('RequireAuthMiddleware: Unauthenticated REST request: %s, %s, %s' % (
                request.path, str(request.user), request.META['REMOTE_ADDR']))
            json_content_type = 'application/json'
            json_error_response = {'error_type': 'auth', 'description': 'Authentication error'}
            return HttpResponse(simplejson.dumps(json_error_response), json_content_type, 403)

        logger.debug('RequireAuthMiddleware: Redirecting to login page: %s, %s, %s' % (
            request.path, str(request.user), request.META['REMOTE_ADDR']))
        return HttpResponseRedirect('%s?next=%s' % (self.login_url, request.path))


class ClusterAuthenticate(object):
    """ClusterAuthenticate: Middleware that authenticates/sets credentials for the REST requests

    If this is a REST request:
        If request is from localhost
            authorize it and set the user as admin
        If a the user already has an authenticated session
            validate that this user is authorized to access the requested cluster
        If it is from an anonynous user:
            extract auth token from the request parameters and
            verify that the token is in authorized for the requested cluster
            (for now, just validate that the user associated with the token
             is authorized to access the requested cluster. In future, we may do more)
    """

    token_param_name = 'auth-token'     # Name of the auth token parameter in the query string
    enforce_auth = isCloudBuild()       # For now, enfore auth only for the cloud instance
    bypass_localhost_check = False      # Set to true to force auth token check even on localhost

    def process_request(self, request):
        logger.debug('url: ' + request.path)
        if not self.enforce_auth:
            logger.debug('ClusterAuthenticate ignored: is disabled')
            return
        if is_localhost(request) and not self.bypass_localhost_check:
            logger.debug('ClusterAuthenticate ignored: is local request')
            return
        if self.get_req_cluster_name(request) is None:
            logger.debug('ClusterAuthenticate ignored: no cluster name in request');
            return

        # Find user and autorized clusters
        cluster_name = self.get_req_cluster_name(request)
        cluster = self.get_cluster_from_name(cluster_name)
        user = AnonymousUser()
        if hasattr(request, 'session'):
            user = auth.get_user(request)
            logger.debug('User from request: ' + str(user))
        if user.is_authenticated():
            allowed_clusters = self.get_allowed_clusters_for_user(user)
        else:
            token_string = self.get_req_token_string(request)
            user = self.get_user_for_token(token_string)
            allowed_clusters = self.get_allowed_clusters_for_token(token_string)
        logger.debug('Checking authorization for cluster ' + str(cluster) +
                     ' in cluster list ' + str(allowed_clusters) +
                     ' for ' + str(user))

        # Do validation/set user for request
        if hasattr(request, 'user'):
            request.user = AnonymousUser()
            request._cached_user = AnonymousUser()
        if cluster and allowed_clusters:
            if cluster in allowed_clusters:
                request.user = user
                request._cached_user = user
        return

    def validate_session(self, request):
        return True

    def get_req_cluster_name(self, request):
        # Find last entry in path, remove parameters
        path = request.path
        cluster_name = None
        try:
            pathcomps = path.split('/')
            if 'rest' in (pathcomps[1],pathcomps[2]):
                cluster_name_idx = 5
                if pathcomps[cluster_name_idx].find(':') > -1:
                    cluster_name = pathcomps[cluster_name_idx]
        except (TypeError, IndexError):
            logger.debug('Type error parsing path for customer: ' + str(path))
            return None
        except Exception:
            logger.debug('Unknown error parsing path for customer: ' + str(path))
            return None
        return cluster_name

    def get_cluster_from_name(self, cluster_name):
        if cluster_name:
            for cluster in Cluster.objects.all():
                if cluster_name == cluster.id:
                    logger.debug('Mapped request to cluster "%s"' % cluster_name)
                    return cluster
        logger.warn('Failed to map request to cluster: "%s"' % cluster_name)
        return None

    def get_req_token_string(self, request):
        token_string = None
        try:
            token_string = request.REQUEST[self.token_param_name]
            token_string = token_string.upper()
        except KeyError:
            logger.debug('No token param in request')
        return token_string

    def get_token_from_name(self, token_string):
        token = None
        if token_string:
            try:
                token = AuthToken.objects.get(id=token_string)
            except AuthToken.DoesNotExist:
                logger.debug('Token not found in DB: ' + str(token_string))
                token = None
            except Exception:
                logger.debug('Auth Tokens not configured in DB? - ' + str(token_string))
                token = None
        return token

    def get_user_for_token(self, token_string):
        user = AnonymousUser()
        token = self.get_token_from_name(token_string)
        if token:
            user = token.user
        return user

    def get_allowed_clusters_for_token(self, token_string):
        """Given an auth token, generate the list of clusters for which it gives credentials

        If the cluster entry in the token object is present, use that
        If the customer entry in the token object is present, return all clusters for the customer
        If the user entry in the token object is present, use that to get a list of clusters.

        In the future, DB changes may provide varying granularity.
        """

        logger.debug('Got token: ' + str(token_string))

        cluster_list = []
        token = self.get_token_from_name(token_string)
        if token:
            if token.cluster is not None:
                logger.debug('token mapped to cluster ' + str(token.cluster))
                cluster_list = [token.cluster]
            elif token.customer is not None:
                logger.debug('token mapped to customer ' + str(token.customer))
                cluster_list = Cluster.objects.filter(customer=token.customer)
            else:
                logger.debug('token mapped to user ' + str(token.user))
                cluster_list = self.get_allowed_clusters_for_user(token.user)

        logger.debug('Returning clust list ' + str(cluster_list))
        return cluster_list

    def get_allowed_clusters_for_user(self, user):
        """Given a user, generate the list of clusters for which it gives credentials
        Use user to map to a customer (list) and from there to a list of clusters.
        """

        logger.debug('Got user: ' + str(user))

        cluster_list = []
        if user:
            for cust_user in CustomerUser.objects.filter(user=user):
                # Currently the query below isn't working.  Brute force alternative given
                #cluster_list.extend(Cluster.objects.filter(customer=cust_user.customer))
                for cluster in Cluster.objects.all():
                    if cluster.customer == cust_user.customer:
                        cluster_list.append(cluster)

        logger.debug('Returning cluster list ' + str(cluster_list))
        return cluster_list
