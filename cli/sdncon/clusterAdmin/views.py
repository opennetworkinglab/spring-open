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

# Create your views here.

from django.contrib.auth.decorators import login_required
from django.utils import simplejson
from django.http import HttpResponse
from sdncon.clusterAdmin.models import CustomerUser
from sdncon.clusterAdmin.utils import conditionally, isCloudBuild
from sdncon.rest.views import safe_rest_view

DEFAULT_TENANT = 'default'
JSON_DATA_TYPE = 'application/json'

@safe_rest_view
def get_node_personality(request):
    isCloud = isCloudBuild()
    response_data = {'cloud' : isCloud, 'controller-node' : not isCloud}
    response_data = simplejson.dumps(response_data)
    return HttpResponse(response_data, JSON_DATA_TYPE)

@conditionally(login_required, isCloudBuild())
@safe_rest_view
def session_clusterAdmin(request):
    """
    This returns the customer and clusters, which the current session is associated with.
    """
    customer = {}
    customer['user'] = request.user.username
    customer['customer'] = DEFAULT_TENANT
    customer['cluster'] = []
    if request.user.is_authenticated():
        cus = CustomerUser.objects.all()
        for cu in cus:
            if cu.user.username == request.user.username:
                customer['customer'] = cu.customer.customername
                for cluster in cu.customer.cluster_set.all():
                    customer['cluster'].append(cluster.clustername)

    response_data = simplejson.dumps(customer)
    return HttpResponse(response_data, JSON_DATA_TYPE) 


import random
from django.shortcuts import render_to_response, redirect
from .models import AuthToken

@conditionally(login_required, isCloudBuild())
def token_view(request):
    token=''
    for t in AuthToken.objects.filter(user=request.user):
        token=t.id
    return render_to_response('registration/token_view.html', {'token': token})

################################################################

VALID_TOKEN_CHARS = ['3', '4', '6', '7', '9', 'A', 'C', 'E', 'F', 'G',
                     'H', 'K', 'M', 'N', 'P', 'R', 'T', 'W', 'X', 'Y']
TOKEN_LENGTH = 16
TOKEN_GEN_ATTEMPTS = 16  # Number of times generated token can be in DB before giving up

#
# Token Specification
#   16 characters, hyphens every 4 characters
#   ....-....-....-....
#   Key is actually this string, including the -s
#   Characters used must be in the VALID_TOKEN_CHARS array above
# This gives a token space of 20^16
#

@conditionally(login_required, isCloudBuild())
def token_generate(request):
    """
    Generate, validate and add an auth token
    """

    for try_count in range(TOKEN_GEN_ATTEMPTS):
        token_string = ""
        for i in range(TOKEN_LENGTH):
            if i > 0 and i % 4 == 0:
                token_string += '-'
            token_string += random.choice(VALID_TOKEN_CHARS)
        print "Generated token " + token_string
                
        # See if the token already exists in the DB; race condition here, but should be minimal
        try:
            AuthToken.objects.get(id=token_string)
            print "Token already present in DB, try " + str(try_count)
            token_string = ""
        except AuthToken.DoesNotExist:  # This is what we want
            break

    if not token_string:
        # Should probably raise an exception here
        print "Failed to generate new auth token"
        # Return internal server error
        return

    # Map the token to a customer or cluster
    # For now, just map one token to the user

    # Delete any existing tokens
    for t in AuthToken.objects.filter(user=request.user):
        print 'Deleting token', t
        t.delete()

    # Add the new token to the DB
    token = AuthToken(id=token_string, user=request.user)
    token.save()

    return redirect('sdncon.clusterAdmin.views.token_view')
