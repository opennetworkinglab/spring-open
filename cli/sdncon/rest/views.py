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

from django.db.models import AutoField, BooleanField, IntegerField, FloatField, ForeignKey
from django.forms import ValidationError
from django.http import HttpResponse
from django.utils import simplejson
from functools import wraps
#from sdncon.controller.models import Controller
#from sdncon.controller.models import ControllerAlias, ControllerDomainNameServer, ControllerInterface, FirewallRule
#from sdncon.controller.models import Switch, Port, PortAlias, Link
from sdncon.rest.models import UserData
from sdncon.controller.notification import begin_batch_notification, end_batch_notification
from django.views.decorators.csrf import csrf_exempt
import base64
import urllib
import urllib2
import sys
import json
import os
import re
import time
import traceback
import collections
import uuid
import subprocess
from datetime import datetime
from sdncon.controller.oswrapper import exec_os_wrapper
from sdncon.controller.config import get_local_controller_id
from sdncon.rest.config import config_check_state
from sdncon.controller.oswrapper import get_system_version_string
import sdncon

TEXT_PLAIN_CONTENT_TYPE = 'text/plain'
TEXT_JAVASCRIPT_CONTENT_TYPE = 'text/javascript'
JSON_CONTENT_TYPE = 'application/json'
BINARY_DATA_CONTENT_TYPE = 'application/octet-stream'

onos = 1

if onos == 1:
    #CONTROLLER_URL_PREFIX = 'http://localhost:9000/wm/'
    CONTROLLER_URL_PREFIX = 'http://localhost:8080/wm/'
else:
    CONTROLLER_URL_PREFIX = 'http://localhost:8080/wm/'

def controller_url(*elements):
    return CONTROLLER_URL_PREFIX + '/'.join(elements)

class RestException(Exception):
    pass
        
class RestInvalidDataTypeException(RestException):
    def __init__(self, name):
        super(RestInvalidDataTypeException,self).__init__('Invalid data type: ' + name)

class RestInvalidMethodException(RestException):
    def __init__(self):
        super(RestInvalidMethodException,self).__init__('Invalid HTTP method')

class RestResourceNotFoundException(RestException):
    def __init__(self, url):
        super(RestResourceNotFoundException,self).__init__('Resource not found: ' + url)

class RestDatabaseConnectionException(RestException):
    def __init__(self):
        super(RestDatabaseConnectionException,self).__init__('Error connecting to database')
        
class RestAuthenticationRequiredException(RestException):
    def __init__(self):
        super(RestAuthenticationRequiredException,self).__init__('Authentication required')

class RestInvalidQueryParameterException(RestException):
    def __init__(self, param_name):
        super(RestInvalidQueryParameterException, self).__init__('Invalid query parameter: ' + str(param_name))

class RestInvalidFilterParameterException(RestException):
    def __init__(self, param_name):
        super(RestInvalidFilterParameterException, self).__init__('Filter query parameters not allowed when URL contains resource iD: ' + str(param_name))

class RestNoListResultException(RestException):
    def __init__(self):
        super(RestNoListResultException, self).__init__('The query result must be a single instance if the "nolist" query param is set')

class RestInvalidPutDataException(RestException):
    def __init__(self):
        super(RestInvalidPutDataException, self).__init__('The request data for a PUT request must be a JSON dictionary object')

class RestMissingRequiredQueryParamException(RestException):
    def __init__(self, param_name):
        super(RestMissingRequiredQueryParamException, self).__init__('Missing required query parameter: ' + str(param_name))

class RestValidationException(RestException):
    def __init__(self, model_error=None, field_errors=None):
        # Build the exception message from model error and field errors
        message = 'Validation error'
        if model_error:
            message = message + '; ' + model_error
        if field_errors:
            message += '; invalid fields: {'
            first_time = True
            for field_name, field_message in field_errors.items():
                if not first_time:
                    message += '; '
                else:
                    first_time = False
                message = message + field_name + ': ' + field_message
                
            message += '}'
            
        super(RestValidationException, self).__init__(message)
        self.model_error = model_error
        self.field_errors = field_errors
        
class RestModelException(RestException):
    def __init__(self, exc):
        super(RestModelException, self).__init__('Error: ' + str(exc))

class RestSaveException(RestException):
    def __init__(self, exc):
        super(RestSaveException, self).__init__('Error saving data: ' + str(exc))

class RestInvalidOrderByException(RestException):
    def __init__(self,field_name):
        super(RestInvalidOrderByException, self).__init__('Invalid orderby field: ' + field_name)
        
class RestInternalException(RestException):
    def __init__(self, exc):
        super(RestInternalException,self).__init__('Unknown REST error: ' + unicode(exc))

class RestUpgradeException(RestException):
    def __init__(self, exc):
        super(RestUpgradeException, self).__init__('Error: ' + str(exc))

class RestProvisionException(RestException):
    def __init__(self, exc):
        super(RestProvisionException, self).__init__('Error: ' + str(exc))

class RestDecommissionException(RestException):
    def __init__(self, exc):
        super(RestDecommissionException, self).__init__('Error: ' + str(exc))

class RestInvalidLog(RestException):
    def __init__(self, exc):
        super(RestInvalidLog, self).__init__('Error: ' + str(exc))

def handle_validation_error(model_info, validation_error):
    model_error = None
    field_errors = None
    if hasattr(validation_error, 'message_dict'):
        # The field errors we get in the ValidationError are a bit different
        # then what we want for the RestValidationException. First, we
        # need to convert the Django field name to the (possibly renamed)
        # REST field name. Second, the per-field error message is possibly a
        # list of messages, which we concatenate into a single string for the
        # RestValidationException
        for field_name, field_message in validation_error.message_dict.items():
            if type(field_message) in (list, tuple):
                converted_field_message = ''
                for msg in field_message:
                    converted_field_message = converted_field_message + msg + ' '
            else:
                converted_field_message += unicode(field_message)
            if field_name == '__all__':
                model_error = converted_field_message
            else:
                if not field_errors:
                    field_errors = {}
                field_info = model_info.field_name_dict.get(field_name)
                if field_info:
                    field_errors[field_info.rest_name] = converted_field_message
                else:
                    field_errors[field_name] = 'Private field invalid; ' + converted_field_message
    elif hasattr(validation_error, 'messages'):
        model_error = ':'.join(validation_error.messages)
    else:
        model_error = str(validation_error)
    raise RestValidationException(model_error, field_errors)

def get_successful_response(description=None, status_code=200):
    content = get_successful_response_data(description)
    return HttpResponse(content, JSON_CONTENT_TYPE, status_code)

def get_successful_response_data(description = 'success'):
    obj = {'description': description}
    return simplejson.dumps(obj)
 
def get_sdnplatform_response(url, timeout = None):
    
    try:
        response_text = urllib2.urlopen(url, timeout=timeout).read()
        return HttpResponse(response_text, JSON_CONTENT_TYPE)
    except urllib2.HTTPError, e:
        response_text = e.read()
        response = simplejson.loads(response_text)
        response['error_type'] = "SDNPlatformError"
        return HttpResponse(content=simplejson.dumps(response), 
                            status=e.code, 
                            content_type=JSON_CONTENT_TYPE)

def get_sdnplatform_query(request, path):
    """
    This returns controller-level storage table list
    """
    if request.method != 'GET':
        raise RestInvalidMethodException()
    url = controller_url(path) + '/?%s' % request.META['QUERY_STRING']
    return get_sdnplatform_response(url)        

def safe_rest_view(func):
    """
    This is a decorator that takes care of exception handling for the
    REST views so that we return an appropriate error HttpResponse if
    an exception is thrown from the view
    """
    @wraps(func)
    def _func(*args, **kwargs):
        try:
            response = func(*args, **kwargs)
        except Exception, exc:
            end_batch_notification(True)
            if not isinstance(exc, RestException):
                # traceback.print_exc()
                exc = RestInternalException(exc)
            response_obj = {'error_type': exc.__class__.__name__, 'description': unicode(exc)}
            if isinstance(exc, RestValidationException):
                if exc.model_error:
                    response_obj['model_error'] = exc.model_error
                if exc.field_errors:
                    response_obj['field_errors'] = exc.field_errors
            content = simplejson.dumps(response_obj)
            content_type = JSON_CONTENT_TYPE
            
            if isinstance(exc, RestInvalidMethodException):
                status_code = 405
            elif isinstance(exc, RestResourceNotFoundException):
                status_code = 404
            elif isinstance(exc, RestInternalException):
                status_code = 500
            else:
                status_code = 400
            response = HttpResponse(content, content_type, status_code)
            if isinstance(exc, RestInvalidMethodException):
                response['Allow'] = "GET, PUT, DELETE"
        return response
    return _func

rest_model_info_dict = {}

class RestFieldInfo(object):
    def __init__(self, name, django_field_info, hidden=False,
                 rest_name=None, json_serialize=False):
        self.name = name
        self.django_field_info = django_field_info
        self.rest_name = rest_name
        self.json_serialize = json_serialize
        
class RestModelInfo(object):
    def __init__(self, rest_name, model_class):
        self.rest_name = rest_name
        self.model_class = model_class
        self.primary_key = None
        self.field_name_dict = {}
        self.rest_name_dict = {}
        
        for field in model_class._meta.local_fields:
            field_name = field.name
            rest_name = field.name
            if field.primary_key:
                self.primary_key = field_name
            # TODO: Are there other field types that should be included here?
            json_serialize =  type(field) not in (AutoField, BooleanField, IntegerField, FloatField)
            self.set_field_info(field_name, rest_name, field, json_serialize)

    
    # this is how a RestFieldInfo is created - pass in django_field_info
    def get_field_info(self, field_name, django_field_info=None):
        field_info = self.field_name_dict.get(field_name)
        if not field_info and django_field_info:
            field_info = RestFieldInfo(field_name, django_field_info)
            self.field_name_dict[field_name] = field_info
        return field_info
    
    def hide_field(self, field_name):
        field_info = self.get_field_info(field_name)
        del self.field_name_dict[field_name]
        del self.rest_name_dict[field_info.rest_name]
        
    def set_field_info(self, field_name, rest_name, django_field_info, json_serialize=None):
        field_info = self.get_field_info(field_name, django_field_info)
        if field_info.rest_name in self.rest_name_dict:
            del self.rest_name_dict[field_info.rest_name]
        field_info.rest_name = rest_name
        if json_serialize != None:
            field_info.json_serialize = json_serialize
        self.rest_name_dict[rest_name] = field_info

def get_rest_model_info(name):
    return rest_model_info_dict[name]

def add_rest_model_info(info):
    if rest_model_info_dict.get(info.rest_name):
        raise RestException('REST model info already exists')
    rest_model_info_dict[info.rest_name] = info
    
rest_initialized = False

def get_default_rest_name(model):
    # TODO: Ideally should do something a bit smarter here.
    # Something like convert from camel-case class names to hyphenated names:
    # For example:
    #  MyTestClass => my-test-class
    #  MyURLClass => my-url-class
    #
    # This isn't super-important for now, since you can set it explicitly
    # with the nested Rest class.
    return model.__name__.lower()

def initialize_rest():
    global rest_initialized
    if rest_initialized:
        return
    
    from django.db.models import get_models
    for model in get_models():
                
        # If the model class has a nested class named 'Rest' then that means
        # the model should be exposed in the REST API.
        if hasattr(model, 'Rest'):
            # By default the REST API uses the lower-case-converted name
            # of the model class as the name in the REST URL, but this can
            # be overridden by defining the 'NAME' attribute in the Rest class.
            if hasattr(model.Rest, 'NAME'):
                rest_name = model.Rest.NAME
            else:
                rest_name = get_default_rest_name(model)

            if model._meta.proxy:
                # This is a proxy class, drop through to the real one
                base_model = model._meta.proxy_for_model
            else:
                base_model = model
            
            # OK, we have the basic REST info, so we can create the info class
            rest_model_info = RestModelInfo(rest_name, base_model)
            
            # Check if there are any private or renamed fields
            if hasattr(model.Rest, 'FIELD_INFO'):
                for field_info in model.Rest.FIELD_INFO:
                    field_name = field_info['name']
                    rest_field_info = rest_model_info.get_field_info(field_name)
                    # Check if field exists in models - don't allow field only here in FIELD_INFO)
                    if not rest_field_info:
                        # LOOK! This field only exists in FIELD_INFO - skip
                        print "ERROR: %s for %s only in FIELD_INFO" % (field_name, rest_name)
                        continue
                    
                    if field_info.get('private', False):
                        rest_model_info.hide_field(field_name)
                    else:
                        rest_name = field_info.get('rest_name')
                        if rest_name:
                            rest_model_info.set_field_info(field_name, rest_name, rest_field_info.django_field_info)
            
            # Finished setting it up, so now add it to the list
            add_rest_model_info(rest_model_info)
    
    rest_initialized = True

initialize_rest()

@safe_rest_view
def do_model_list(request):
    """
    This returns the list of models available in the REST API.
    """
    
    json_model_list = []
    for model_name in rest_model_info_dict.keys():
        json_model_info = {}
        json_model_info["name"] = model_name
        json_model_info["url_path"] = "rest/v1/model/" + model_name + "/"
        json_model_list.append(json_model_info)

    json_data = simplejson.dumps(json_model_list)
    return HttpResponse(json_data, JSON_CONTENT_TYPE)

@safe_rest_view
def do_realtimestats(request, stattype, dpid):
    """
    This returns realtime statistics (flows, ports, table, aggregate,
    desc, ...)  for a dpid by calling the localhost sdnplatform
    """
    #raise RestInvalidMethodException()
    if request.method != 'GET':
        raise RestInvalidMethodException()
    #url = controller_url('core', 'switch', dpid, stattype, 'json')
    if stattype == 'group':
        stattype = 'groupStats'
    if stattype == 'groupdesc':
        stattype = 'groupDesc'
    url = "http://localhost:8080/wm/floodlight/core/switch/%s/%s/json" % (dpid, stattype)
    return get_sdnplatform_response(url)

@safe_rest_view
def do_realtimegroupstats(request, groupId, dpid ):
    """
    This returns realtime group statistics for specified groupId
    for a dpid by calling the localhost sdnplatform
    """
    #raise RestInvalidMethodException()
    if request.method != 'GET':
        raise RestInvalidMethodException()
    #url = controller_url('core', 'switch', dpid, stattype, 'json')
    #import error
    #raise error.ArgumentValidationError('\n\n\n %s' % (groupId))
    url = "http://localhost:8080/wm/floodlight/core/switch/%s/groupStats/%s/json" % (dpid, groupId)
    return get_sdnplatform_response(url)

@safe_rest_view
def do_tablerealtimestats(request, tabletype, dpid):
    """
    This returns realtime statistics per table (flows (only)
    current implementation)  for a dpid by calling the localhost sdnplatform
    """
    #raise RestInvalidMethodException()
    if request.method != 'GET':
        raise RestInvalidMethodException()
    #url = controller_url('core', 'switch', dpid, stattype, 'json')
    url = "http://localhost:8080/wm/floodlight/core/switch/%s/table/%s/flow/json" % (dpid,tabletype)
    #url ="http://localhost:8080/wm/floodlight/core/switch/00:00:00:00:00:00:00:01/flow/json"
    return get_sdnplatform_response(url)

@safe_rest_view
def do_sdnplatform_realtimestats(request, stattype, dpid=None, portid=None):
    """
    This returns realtime statistics from sdnplatform
    """
    if request.method != 'GET':
        raise RestInvalidMethodException()
    if dpid == None:
        url = controller_url('core', 'counter', stattype, 'json')
    elif portid == None:
        url = controller_url('core', 'counter', dpid, stattype, 'json')
    else:
        url = controller_url('core', 'counter', dpid, portid, stattype, 'json')
    return get_sdnplatform_response(url)

@safe_rest_view
def do_topology_tunnel_verify(request, srcdpid=None, dstdpid=None):
    """
    This initiates a liveness detection of tunnels.
    """
    if request.method != 'GET':
        raise RestInvalidMethodException()

    urlstring = srcdpid + '/' + dstdpid
    url = controller_url('topology/tunnelverify', urlstring, 'json')

    response_text = urllib2.urlopen(url).read()
    time.sleep(4)
    return do_topology_tunnel_status(request, srcdpid, dstdpid)

@safe_rest_view
def do_topology_tunnel_status(request, srcdpid='all', dstdpid='all'):
    """
    This returns the list of tunnels that have failed over the last observation interval.
    """
    if request.method != 'GET':
        raise RestInvalidMethodException()

    urlstring = srcdpid + '/' + dstdpid
    url = controller_url('topology/tunnelstatus', urlstring, 'json')
    response_text = urllib2.urlopen(url).read()
    return HttpResponse(response_text, JSON_CONTENT_TYPE)

@safe_rest_view
def do_sdnplatform_realtimestatus(request, category=None, subcategory=None, srcdpid=None, dstdpid = None):
    """
    This returns realtime status of sdnplatform
    """

    if request.method != 'GET':
        raise RestInvalidMethodException()

    response_text = None
    url = None
    if category == 'network':
        if subcategory == 'cluster':
            url = controller_url('topology', 'switchclusters', 'json')
        if subcategory == 'externalports':
            url = controller_url('topology', 'externalports', 'json')
        if subcategory == 'tunnelverify':
            urlstring = subcategory+ '/' + srcdpid + '/' + dstdpid
            url = controller_url('topology', urlstring, 'json')
        if subcategory == 'tunnelstatus':
            url = controller_url('topology', 'tunnelstatus', 'json')

    if url:
        response_text = urllib2.urlopen(url).read()
    return HttpResponse(response_text, JSON_CONTENT_TYPE)

@safe_rest_view
def do_sdnplatform_realtimetest(http_request, category=None, subcategory=None):
    """
    This does a realtime test by sending an "explain packet" as packet in 
    and collecting the operations performed on the packet
    """

    if http_request.method != 'PUT':
        raise RestInvalidMethodException()

    response_text = None
    url = None
    if category == 'network':
        if subcategory == 'explain-packet':
            # set up the sdnplatform URL for explain packet (at internal port 8080
            url = controller_url('vns', 'explain-packet', 'json')
            post_data = http_request.raw_post_data
            request = urllib2.Request(url, post_data)
            request.add_header('Content-Type', 'application/json')
            response = urllib2.urlopen(request)
            response_text = response.read()
        elif subcategory == 'path':
            post_data = json.loads(http_request.raw_post_data)
            url = controller_url('topology', 'route',
                                 post_data['src-switch'],
                                 str(post_data['src-switch-port']),
                                 post_data['dst-switch'],
                                 str(post_data['dst-switch-port']),
                                 'json')

            return get_sdnplatform_response(url)

    return HttpResponse(response_text, JSON_CONTENT_TYPE)

@safe_rest_view
def do_sdnplatform_performance_monitor(http_request, category=None,
                    subcategory=None, type='all'):
    """
    This API returns performance related information from the sdnplatform and
    sdnplatform components
    """

    if http_request.method != 'GET':
        raise RestInvalidMethodException()

    response_text = None
    url = None
    if category == 'performance-monitor':
        # set up the sdnplatform URL for explain packet (at internal port 8080
        url = controller_url('performance', type, 'json')
        request = urllib2.Request(url)
        response = urllib2.urlopen(request)
        response_text = response.read()
    return HttpResponse(response_text, JSON_CONTENT_TYPE)

@safe_rest_view
def do_sdnplatform_internal_debugs(http_request, category=None, subcategory=None,
                              query='all', component='device-manager'):
    """
    This API returns debugging related information from the sdnplatform and
    sdnplatform components
    """

    if http_request.method != 'GET':
        raise RestInvalidMethodException()

    response_text = None
    url = None
    if category == 'internal-debugs':
        # set up the sdnplatform URL for explain packet (at internal port 8080
        url = controller_url('vns', 'internal-debugs', component, query, 'json')
        request = urllib2.Request(url)
        response = urllib2.urlopen(request)
        response_text = response.read()
    return HttpResponse(response_text, JSON_CONTENT_TYPE)

@safe_rest_view
def do_sdnplatform_event_history(http_request, category=None, subcategory=None,
                              evHistName='all', count='100'):
    """
    This API returns real-time event-history information from the sdnplatform and
    sdnplatform components
    """

    if http_request.method != 'GET':
        raise RestInvalidMethodException()

    response_text = None
    url = None
    if category == 'event-history':
        # set up the sdnplatform URL for explain packet (at internal port 8080
        url = controller_url('core', 'event-history', evHistName, count, 'json')
        request  = urllib2.Request(url)
        response = urllib2.urlopen(request)
        response_text = response.read()
    return HttpResponse(response_text, JSON_CONTENT_TYPE)

@safe_rest_view
def do_flow_cache(http_request, category=None, subcategory=None,
                          applName='None', applInstName='all', queryType='all'):
    """
    This API returns real-time event-history information from the sdnplatform and
    sdnplatform components
    """

    if http_request.method != 'GET':
        raise RestInvalidMethodException()

    response_text = None
    url = None
    if category == 'flow-cache':
        # set up the sdnplatform URL for explain packet (at internal port 8080
        url = controller_url('vns', 'flow-cache', applName, applInstName, queryType, 'json')
        request  = urllib2.Request(url)
        response = urllib2.urlopen(request)
        response_text = response.read()
    return HttpResponse(response_text, JSON_CONTENT_TYPE)


@safe_rest_view
def do_vns_realtimestats_flow(http_request, category=None, vnsName="all"):
    """
    This gets realtime flows for one or more vnses
    """

    if http_request.method != 'GET':
        raise RestInvalidMethodException()

    # set up the sdnplatform URL for per-vns flow (at internal port 8080
    url = controller_url('vns', 'flow', vnsName, 'json')
    return get_sdnplatform_response(url)

@safe_rest_view
def do_sdnplatform_counter_categories(request, stattype, layer, dpid=None, portid=None):
    """
    This returns counter categories from sdnplatform
    """
    if request.method != 'GET':
        raise RestInvalidMethodException()
    if dpid == None:
        url = controller_url('core', 'counter', 'categories', stattype, layer, 'json')
    elif portid == None:
        url = controller_url('core', 'counter', 'categories', dpid, stattype, layer, 'json')
    else:
        url = controller_url('core', 'counter', 'categories', dpid, portid, stattype, layer, 'json')

    return get_sdnplatform_response(url)

@safe_rest_view
@csrf_exempt
def do_packettrace(request):
    """
    This sets a packet trace in sdnplatform.
    period: 
        . >0 starts a trace session with the period
        . =0 starts a trace session with no timeout
        . <0 ends an ongoing session

    The request method has to be POST since each request gets an unique sessionID
    """
    SESSIONID = 'sessionId'
    sessionId = ""
    filter = ""
    if request.method != 'POST':
        raise RestInvalidMethodException()

    url = 'http://localhost:8080/wm/vns/packettrace/json'
    request = urllib2.Request(url, request.raw_post_data, {'Content-Type':'application/json'})
    try:
        response = urllib2.urlopen(request)
        response_text = response.read()
    except Exception, e:
        # SDNPlatform may not be running, but we don't want that to be a fatal
        # error, so we just ignore the exception in that case.
        pass

    #response_data = json.loads(response_text) 
    #if SESSIONID in response_data:
    #    sessionId = response_data[SESSIONID]
    #response_text = {SESSIONID:sessionId}

    return HttpResponse(response_text, mimetype=JSON_CONTENT_TYPE)

@safe_rest_view
def do_controller_stats(request, stattype):
    """
    This returns controller-level statistics/info from sdnplatform
    """
    if request.method != 'GET':
        raise RestInvalidMethodException()
    url = 'http://127.0.0.1:8080/wm/core/controller/%s/json' % stattype
    return get_sdnplatform_response(url)

@safe_rest_view
def do_controller_storage_table_list(request):
    """
    This returns controller-level storage table list
    """
    if request.method != 'GET':
        raise RestInvalidMethodException()
    url = 'http://127.0.0.1:8080/wm/core/storage/tables/json'
    return get_sdnplatform_response(url)

@safe_rest_view
def do_device(request):
    if onos == 0:
        return get_sdnplatform_query(request, "device")
    else:
        url = controller_url("onos", "topology", "hosts")
        if request.META['QUERY_STRING']:
            url += '?' + request.META['QUERY_STRING']
        return get_sdnplatform_response(url)        

@safe_rest_view
def do_switches(request):
    if onos == 0:
        url = controller_url("core", "controller", "switches", "json")
    else:
        url = controller_url("onos", "topology", "switches")
    if request.META['QUERY_STRING']:
        url += '?' + request.META['QUERY_STRING']
    return get_sdnplatform_response(url)        

@safe_rest_view
def do_routers(request):
    if onos == 0:
        url = controller_url("core", "controller", "switches", "json")
    else:
        url = controller_url("onos","segmentrouting", "routers")
    if request.META['QUERY_STRING']:
        url += '?' + request.META['QUERY_STRING']
    return get_sdnplatform_response(url)        

@safe_rest_view
def do_router_stats(request, stattype, dpid):
    """
    This returns the subnets info about the specifed
    router dpid. statetype should be 'port'.
    """
    #raise RestInvalidMethodException()
    if request.method != 'GET':
        raise RestInvalidMethodException()
    #url = controller_url('core', 'switch', dpid, stattype, 'json')
    url = "http://localhost:8080/wm/onos/segmentrouting/router/%s/%s" % (dpid, stattype)
    #raise RestInvalidMethodException(url)
    #url = "http://localhost:8080/wm/onos/segementrouting/router/00:00:00:00:00:00:00:01/port"
    return get_sdnplatform_response(url)

@safe_rest_view
def do_mastership(request):
    url = controller_url("onos", "registry", "switches" ,"json")
    #url = "http://127.0.0.1:8080/wm/onos/registry/switches/json"
    if request.META['QUERY_STRING']:
        url += '?' + request.META['QUERY_STRING']
    return get_sdnplatform_response(url)

@safe_rest_view
def do_controller(request):
    url = controller_url("onos", "registry", "controllers" ,"json")
    #url = "http://127.0.0.1:8080/wm/onos/registry/switches/json"
    if request.META['QUERY_STRING']:
        url += '?' + request.META['QUERY_STRING']
    return get_sdnplatform_response(url) 
#'''

@safe_rest_view
def do_links(request):
    if onos == 0:
        url = controller_url("topology", "links", "json")
    else:
        url = controller_url("onos", "topology", "links")
    if request.META['QUERY_STRING']:
        url += '?' + request.META['QUERY_STRING']
    return get_sdnplatform_response(url)        

@safe_rest_view
def do_vns_device_interface(request):
    return get_sdnplatform_query(request, "vns/device-interface")

@safe_rest_view
def do_vns_interface(request):
    return get_sdnplatform_query(request, "vns/interface")

@safe_rest_view
def do_vns(request):
    return get_sdnplatform_query(request, "vns")

@safe_rest_view
def do_system_version(request):
    if request.method != 'GET':
        raise RestInvalidMethodException()
    version = get_system_version_string()
    response_text = simplejson.dumps([{ 'controller' : version }])
    return HttpResponse(response_text, JSON_CONTENT_TYPE)

available_log_files = {
    'syslog'           : '/var/log/syslog',
    'sdnplatform'       : '/opt/sdnplatform/sdnplatform/log/sdnplatform.log',
    'console-access'   : '/opt/sdnplatform/con/log/access.log',
    'cassandra'        : '/opt/sdnplatform/db/log/system.log',
    'authlog'          : '/var/log/auth.log',
    'pre-start'        : '/tmp/pre-start',
    'post-start'       : '/tmp/post-start',
    # 'ftp'              : '/var/log/ftp.log',
}

available_log_commands = {
    'dmesg'     : 'dmesg',
    'process'   : 'ps lax'
}

@safe_rest_view
def do_system_log_list(request):
    if request.method != 'GET':
        raise RestInvalidMethodException()
    existing_logs = []
    for (log_name, log_path) in available_log_files.items():
        try:
            log_file = open(log_path, 'r')
            existing_logs.append({ 'log' : log_name })
            log_file.close()
        except Exception, e:
            pass

    print '??'
    for log_name in available_log_commands.keys():
        print 'ADD', log_name
        existing_logs.append({ 'log' : log_name })
    response_text = simplejson.dumps(existing_logs)
    return HttpResponse(response_text, JSON_CONTENT_TYPE)


def generate_subprocess_output(cmd):

    process = subprocess.Popen(cmd, shell=True,
                                   stdout=subprocess.PIPE,
                                   stderr=subprocess.STDOUT,
                                   bufsize=1)
    while True:
        line = process.stdout.readline()
        if line != None and line != "":
            yield line
        else:
            break


@safe_rest_view
def do_system_log(request, log_name):
    if request.method != 'GET':
        raise RestInvalidMethodException()
    print 'do system log', log_name

    # manage command ouput differently
    if log_name in available_log_commands:
        cmd = available_log_commands[log_name]
        print 'DOING COMMAND', cmd

        return HttpResponse(generate_subprocess_output(cmd),
                            TEXT_PLAIN_CONTENT_TYPE)
        return

    log_path = available_log_files.get(log_name)
    if log_name == None:
        raise RestInvalidLog('No such log: %s' % log_name)
    
    try:
        log_file = open(log_path, 'r')
    except Exception,e:
        raise RestInvalidLog('Log does not exist: %s' % log_name)
        
    # use a generator so that the complete log is not ever held in memory
    def response(log_name, file):
        for line in file:
            yield line
        file.close()

    return HttpResponse(response(log_name, log_file), TEXT_PLAIN_CONTENT_TYPE)


@safe_rest_view
def do_system_uptime(request):
    if request.method != 'GET':
        raise RestInvalidMethodException()
    url = controller_url('core', 'system', 'uptime', 'json')
    return get_sdnplatform_response(url)        


def _collect_system_interfaces(lo = False):
    from netifaces import interfaces, ifaddresses, AF_INET, AF_LINK
    result = []
    for iface in interfaces():
        if iface.startswith('lo') and not lo:
            continue # ignore loopback
        addrs = ifaddresses(iface)
        if AF_INET in addrs:
            for addr in ifaddresses(iface)[AF_INET]:
                result.append({'name'      : iface,
                               'addr'      : addr.get('addr', ''),
                               'netmask'   : addr.get('netmask', ''),
                               'broadcast' : addr.get('broadcast', ''),
                               'peer'      : addr.get('peer', '')})
    return result


def do_system_inet4_interfaces(request):
    if request.method != 'GET':
        raise RestInvalidMethodException()
    response_text = simplejson.dumps(_collect_system_interfaces(lo = True))
    return HttpResponse(response_text, JSON_CONTENT_TYPE)


@safe_rest_view
def do_system_time_zone_strings(request, list_type):
    import pytz
    if list_type == 'common':
        string_list = pytz.common_timezones
    elif list_type == "all":
        string_list = pytz.all_timezones
    else:
        raise RestResourceNotFoundException(request.path)
    
    response_text = simplejson.dumps(string_list)
    
    return HttpResponse(response_text, JSON_CONTENT_TYPE)

@safe_rest_view
def do_check_config(request):
    config_check_state()
    return get_successful_response('checked config')

@safe_rest_view
def do_local_controller_id(request):
    if request.method == 'GET':
        controller_id = get_local_controller_id()
        if not controller_id:
            raise Exception("Unspecified local controller id")
        response_text = simplejson.dumps({'id': controller_id})
    elif request.method == 'PUT':
        put_data = json.loads(request.raw_post_data)
        controller_id = put_data.get('id')
        _result = exec_os_wrapper("ControllerId", 'set', [controller_id])
        response_text = get_successful_response_data('updated')
    else:
        raise RestInvalidMethodException()
    
    response = HttpResponse(response_text, JSON_CONTENT_TYPE)
    
    return response

@safe_rest_view
def do_ha_failback(request):
    if request.method != 'PUT':
        raise RestInvalidMethodException()
    _result = exec_os_wrapper("HAFailback", 'set', [])
    response_text = get_successful_response_data('forced failback')
    response = HttpResponse(response_text, JSON_CONTENT_TYPE)
    return response

def delete_ha_firewall_rules(ip):
    rules = FirewallRule.objects.filter(action='allow', src_ip=ip)
    rules.filter(port=80).delete()
    rules.filter(proto='tcp', port=7000).delete()
    rules.filter(proto='vrrp').delete()

def cascade_delete_controller_node(controller_id):
    ControllerAlias.objects.filter(controller=controller_id).delete()
    ControllerDomainNameServer.objects.filter(controller=controller_id).delete()
    for iface in ControllerInterface.objects.filter(controller=controller_id):
        FirewallRule.objects.filter(interface=iface.id).delete()
    ControllerInterface.objects.filter(controller=controller_id).delete()
    Controller.objects.filter(id=controller_id).delete()

# FIXME: this assume a single-interface design and will look for the IP on eth0
#        need to fix this when we have a proper multi-interface design
def get_controller_node_ip(controller_id):
    node_ip = ''
    iface = ControllerInterface.objects.filter(controller=controller_id, type='Ethernet', number=0)
    if iface:
        node_ip = iface[0].discovered_ip
    return node_ip

# This method is "external" facing
# It is designed to be called by CLI or other REST clients
# This should only run on the master node, where decommissioning of a remote node is initiated
@safe_rest_view
def do_decommission(request):
    if request.method != 'PUT':
        raise RestInvalidMethodException()
    data = simplejson.loads(request.raw_post_data)
    node_id = data['id']

    # Disallow self-decommissioning
    local_id = get_local_controller_id()
    if local_id == node_id:
        raise RestDecommissionException("Decommissioning of the master node is not allowed. " + \
                                        "Please perform a failover first.")

    try :
        controller = Controller.objects.get(id=node_id)
    except Controller.DoesNotExist:
        raise RestDecommissionException("No controller found")

    node_ip = get_controller_node_ip(node_id)

    # Case 1: controller node has IP
    if node_ip:
        result = exec_os_wrapper("Decommission", 'set', [node_ip])
        output = result['out'].strip()
        if result['out'].strip().endswith('is already decommissioned'):
            delete_ha_firewall_rules(node_ip)
            cascade_delete_controller_node(node_id)

    # Case 2: controller node has NO IP
    else:
        output = '%s is already decommissioned' % node_id
        cascade_delete_controller_node(node_id)

    jsondict = {}
    jsondict['status'] = "OK"
    jsondict['description'] = output
    return HttpResponse(simplejson.dumps(jsondict), JSON_CONTENT_TYPE)

# This method is "internal" facing
# It is designed to be called only by sys/remove-node.sh
# This should only run on the node being decommissioned (slave)
@safe_rest_view
def do_decommission_internal(request):
    if request.method != 'PUT':
        raise RestInvalidMethodException()
    data = simplejson.loads(request.raw_post_data)
    node_ip = data['ip']
    exec_os_wrapper("DecommissionLocal", 'set', [node_ip])

    jsondict = {}
    jsondict['status'] = "OK"
    return HttpResponse(simplejson.dumps(jsondict), JSON_CONTENT_TYPE)

@safe_rest_view
def do_ha_provision(request):
    if request.method != 'PUT':
        raise RestInvalidMethodException()
    data = simplejson.loads(request.raw_post_data)
    node_ip = data['ip']
    
    try :
        ci = ControllerInterface.objects.get(ip=node_ip)
        id = ci.controller.id
        print 'got id', id
        try:
            a = ControllerAlias.objects.get(controller=id)
            alias = a.alias
        except:
            alias = '(no controller alias)'

        print 'alias:', alias
        raise RestProvisionException('ip address already in controller %s %s' %
                                     (id, alias))
        
    except ControllerInterface.DoesNotExist:
        id = uuid.uuid4().urn[9:]
        print "generated id = ", id
        c = Controller(id=id)
        try:
            c.save()
        except:
            # describe failure
            raise RestProvisionException('can\t save controller')
            pass
        print "save controller"
        ci = ControllerInterface(controller=c,
                                 ip=node_ip,
                                 discovered_ip=node_ip)
        try:
            ci.save()
        except:
            # describe failure
            raise RestProvisionException('can\t save controllar interfacer')

    for c in Controller.objects.all():
        if c.id != id:
            #if there are multiple interfaces, assume the
            # ethernet0 interface is for management purpose 
            # XXX this could be better.
            iface = ControllerInterface.objects.get(controller=c.id,
                                                    type='Ethernet',
                                                    number=0)
            ip = iface.ip
            fw = FirewallRule(interface=iface, action='allow',
                              src_ip=node_ip, port=80, proto='tcp')
            try:
                fw.save()
            except:
                # describe failure
                raise RestProvisionException('can\t save firewall rule from master')
                
            fw = FirewallRule(interface=ci, action='allow',
                              src_ip=ip, port=80, proto='tcp')
            try:
                fw.save()
            except:
                raise RestProvisionException('can\t save firewall from slave')
        

    response_text = get_successful_response_data(id)
    response = HttpResponse(response_text, JSON_CONTENT_TYPE)

    return response


def get_clustername():
    name = os.popen("grep cluster_name /opt/sdnplatform/db/conf/cassandra.yaml | awk  '{print $2}'").readline()
    # name may be '', perhaps this ought to None?
    return name


@safe_rest_view
def do_clustername(request):
    if request.method != 'GET':
        raise RestInvalidMethodException()
    response_text = simplejson.dumps([{ 'clustername' : get_clustername() }])
    return HttpResponse(response_text, JSON_CONTENT_TYPE)
    
    
@safe_rest_view
def do_local_ha_role(request):
    if request.method != 'GET':
        raise RestInvalidMethodException()

    url = controller_url('core', 'role', 'json')
    try:
        response_text = urllib2.urlopen(url, timeout=2).read()
        response = json.loads(response_text)
    except Exception:
        response = HttpResponse('{"role":"UNAVAILABLE"}', JSON_CONTENT_TYPE)
        return response
    # now determine our controller id
    controller_id = get_local_controller_id()

    # find all the interfaces
    ifs = ControllerInterface.objects.filter(controller=controller_id)

    for intf in ifs:
        firewall_id = '%s|%s|%s' % (controller_id, intf.type, intf.number)

        rules = FirewallRule.objects.filter(interface=firewall_id)
        for rule in rules:
            if rule.action == 'reject' and rule.proto == 'tcp' and rule.port == 6633:
                if response['role'] in {'MASTER', 'SLAVE'}:
                    response['role']+='-BLOCKED'

    response['clustername'] = get_clustername()
        
    return HttpResponse(simplejson.dumps(response), JSON_CONTENT_TYPE)

@safe_rest_view
def do_system_clock(request, local=True):
    local_or_utc_str = 'local' if local else 'utc'
    if request.method == 'GET':
        result = exec_os_wrapper("DateTime", 'get', [local_or_utc_str])
    elif request.method == 'PUT':
        time_info = simplejson.loads(request.raw_post_data)
        dt = datetime(**time_info)
        new_date_time_string = dt.strftime('%Y:%m:%d:%H:%M:%S')
        result = exec_os_wrapper("DateTime", 'set', [local_or_utc_str, new_date_time_string])
    else:
        raise RestInvalidMethodException()

    if len(result) == 0:
        raise Exception('Error executing date command')
    
    # The DateTime OS wrapper only has a single command so the return
    # date/time is the first line of the first element of the out array
    values = result['out'].strip().split(':')
    date_time_info = {
        'year': int(values[0]),
        'month': int(values[1]),
        'day': int(values[2]),
        'hour': int(values[3]),
        'minute': int(values[4]),
        'second': int(values[5]),
        'tz': values[6]
    }
    response_text = simplejson.dumps(date_time_info)
    response = HttpResponse(response_text, JSON_CONTENT_TYPE)

    return response

@safe_rest_view
def do_instance(request, model_name,id=None):
    """
    This function handles both GET and PUT methods.
    
    For a GET request it returns a list of all of the instances of the
    model corresponding to the specified type that match the specified
    query parameters. If there are no query parameters then it returns
    a list of all of the instances of the specified type. The names of
    the query parameters can use the Django double underscore syntax
    for doing more complicated tests than just equality
    (e.g. mac__startswith=192.168).
    
    For a PUT request it can either update an existing instance or
    insert one or more new instances. If there are any query parameters
    then it assumes that its the update case and that the query
    parameters identify exactly one instance. If that's not the case
    then an error response is returned. For the update case any subset
    of the fields can be updated with the PUT data. The format of the
    PUT data is a JSON dictionary
    """
    
    # FIXME: Hack to remap 'localhost' id for the controller-node model
    # to the real ID for this controller
    if model_name == 'controller-node' and id == 'localhost':
        id = get_local_controller_id()
        
    # Lookup the model class associated with the specified name
    model_info = rest_model_info_dict.get(model_name)
    if not model_info:
        raise RestInvalidDataTypeException(model_name)
            
    # Set up the keyword argument dictionary we use to filter the QuerySet.
    filter_keyword_args = {}

    jsonp_prefix = None
    nolist = False
    order_by = None
    
    # Now iterate over the query params and add further filter keyword arguments
    query_param_dict = request.GET
    for query_param_name, query_param_value in query_param_dict.items():
        add_query_param = False
        query_param_name = str(query_param_name)
        query_param_value = str(query_param_value)
        if query_param_name == 'callback': #switching to match up with jquery getJSON call naming convention.
            jsonp_prefix = query_param_value
        elif query_param_name == 'nolist':
            if query_param_value not in ('False', 'false', '0', ''):
                nolist = True
        elif query_param_name == 'orderby':
            order_by = query_param_value.split(',')
            for i in range(len(order_by)):
                name = order_by[i]
                if name.startswith('-'):
                    descending = True
                    name = name[1:]
                else:
                    descending = False
                field_info = model_info.rest_name_dict.get(name)
                if not field_info:
                    raise RestInvalidOrderByException(name)
                name = field_info.name
                if descending:
                    name = '-' + name
                order_by[i] = name
        elif query_param_name in model_info.rest_name_dict:
            field_info = model_info.rest_name_dict.get(query_param_name)
            # For booleans, translate True/False strings into 0/1.
            if field_info and type(field_info.django_field_info) == BooleanField:
                if query_param_value.lower() == 'false':
                    query_param_value = 0
                elif query_param_value.lower() == 'true':
                    query_param_value = 1
            query_param_name = field_info.name
            if model_name == 'controller-node' and \
              query_param_name == 'id' and query_param_value == 'localhost':
                query_param_value = get_local_controller_id()
            if model_name in 'controller-interface' and \
              query_param_name == 'controller' and \
              query_param_value == 'localhost':
                query_param_value = get_local_controller_id()
            add_query_param = True
        else:
            double_underscore_start = query_param_name.find("__")
            if double_underscore_start >= 0:
                rest_name = query_param_name[:double_underscore_start]
                field_info = model_info.rest_name_dict.get(rest_name)
                if field_info:
                    operation = query_param_name[double_underscore_start:]
                    query_param_name = field_info.name
                    if type(field_info.django_field_info) == ForeignKey:
                        query_param_name = query_param_name + '__' + field_info.django_field_info.rel.field_name
                    # Substitute in the model field name for the (possible renamed) rest name
                    query_param_name += operation
                    add_query_param = True
        if add_query_param:
            filter_keyword_args[query_param_name] = query_param_value

    if id != None:
        if len(filter_keyword_args) > 0:
            raise RestInvalidFilterParameterException(filter_keyword_args.keys()[0])
        try:
            get_args = {model_info.primary_key:id}
            instance = model_info.model_class.objects.get(**get_args)
            instance_list = (instance,)
            nolist = True
        except model_info.model_class.DoesNotExist,e:
            raise RestResourceNotFoundException(request.path)
        except model_info.model_class.MultipleObjectsReturned, exc:
            # traceback.print_exc()
            raise RestInternalException(exc)
    elif (request.method != 'PUT') or (len(filter_keyword_args) > 0):
        # Get the QuerySet based on the keyword arguments we constructed
        instance_list = model_info.model_class.objects.filter(**filter_keyword_args)
        if order_by:
            instance_list = instance_list.order_by(*order_by)
    else:
        # We're inserting new objects, so there's no need to do a query
        instance_list = None
        
    response_content_type = JSON_CONTENT_TYPE
    
    if request.method == 'GET':
        json_response_data = []
        for instance in instance_list:
            json_instance = {}
            for field_info in model_info.field_name_dict.values():
                # Made some minor edits to deal with a) fields that are empty and b) fields that are not strings -Kyle
                # Changed this to only do an explicit string conversion if it's a unicode string.
                # The controller is expecting to get the unstringified value (e.g. for boolean values)
                # Not sure if this will break things in the UI, but we'll need to resolve how
                # we want to handle this. Also, how do we want to handle unicode strings? -- robv
                field_name = field_info.name
                if type(field_info.django_field_info) == ForeignKey:
                    field_name += '_id'
                value = instance.__dict__.get(field_name)
                if value != None:
                    if field_info.json_serialize:
                        value = str(value)
                    json_instance[field_info.rest_name] = value
            json_response_data.append(json_instance)
        
        # If the nolist query param was enabled then check to make sure
        # that there was only a single instance in the response list and,
        # if so, unpack it from the list
        if nolist:
            if len(json_response_data) != 1:
                raise RestNoListResultException()
            json_response_data = json_response_data[0]
        
        # Convert to json
        response_data = simplejson.dumps(json_response_data)
        
        # If the jsonp query parameter was specified, wrap the data with
        # the jsonp prefix
        if jsonp_prefix:
            response_data = jsonp_prefix + '(' + response_data + ')'
            # We don't really know what the content type is here, but it's typically javascript
            response_content_type = TEXT_JAVASCRIPT_CONTENT_TYPE
    elif request.method == 'PUT':
        response_data = get_successful_response_data('saved')
        response_content_type = JSON_CONTENT_TYPE
        
        begin_batch_notification()
        json_object = simplejson.loads(request.raw_post_data)
        if instance_list is not None:

            # don't allow the ip address of the first interface to
            # be updated once it is set.  This really applies to
            # the interface cassandra uses to sync the db.
            if model_name == 'controller-interface':
                for instance in instance_list:
                    if instance.number == 0 and instance.ip != '':
                        if 'ip' in json_object and json_object['ip'] != instance.ip:
                            raise RestModelException("In this version, ip-address of primary interface can't be updated after initial configuration")

            # In this case the URL includes query parameter(s) which we assume
            # narrow the request to the instances of the model to be updated
            # updated with the PUT data. So we're updating existing instances
            
            # If it's a list with one element then extract the single element
            if (type(json_object) == list) and (len(json_object) == 1):
                json_object = json_object[0]
                
            # We're expecting a dictionary where the keys match the model field names
            # If the data isn't a dictionary then return an error
            if type(json_object) != dict:
                raise RestInvalidPutDataException() # TODO: Should return something different here
            
            # Set the fields in the model instance with the data from the dictionary
            for instance in instance_list:
                for rest_name, value in json_object.items():
                    if not rest_name in model_info.rest_name_dict:
                        raise RestModelException("Model '%s' has no field '%s'" % 
                                                 (model_name, rest_name))
                    field_info = model_info.rest_name_dict[rest_name]
                    field_name = str(field_info.name)   # FIXME: Do we need the str cast?
                    if type(field_info.django_field_info) == ForeignKey:
                        field_name += '_id'
                    # TODO: Does Django still not like unicode strings here?
                    if type(value) == unicode:
                        value = str(value)
                    instance.__dict__[field_name] = value
                # Save the updated model instance
                try:
                    instance.full_clean()
                    instance.save()
                except ValidationError, err:
                    handle_validation_error(model_info, err)
                    #raise RestValidationException(err)
                except Exception, exc:
                    raise RestSaveException(exc)
        else:
            # In this case no query parameters or id were specified so we're inserting new
            # instances into the database. The PUT data can be either a list of new
            # items to add (i.e. top level json object is a list) or else a single
            # new element (i.e. top-level json object is a dict).
            #print "creating object(s)"

            # To simplify the logic below we turn the single object case into a list
            if type(json_object) != list:
                json_object = [json_object]
            
            # Create new model instances for all of the items in the list
            for instance_data_dict in json_object:
                # We expect the data to be a dictionary keyed by the field names
                # in the model. If it's not a dict return an error
                if type(instance_data_dict) != dict:
                    raise RestInvalidPutDataException()
                
                converted_dict = {}
                
                # Now add the fields specified in the PUT data
                for rest_name, value in instance_data_dict.items():

                    #print "  processing " + str(name) + " " + str(value)

                    if not rest_name in model_info.rest_name_dict:
                        raise RestModelException("Model '%s' has no field '%s'" % 
                                                 (model_name, rest_name))
                    field_info = model_info.rest_name_dict[rest_name]
                    # simplejson uses unicode strings when it loads the objects which
                    # Django doesn't like that, so we convert these to ASCII strings
                    if type(rest_name) == unicode:
                        rest_name = str(rest_name)
                    if type(value) == unicode:
                        value = str(value)
                    field_name = field_info.name
                    # FIXME: Hack to remap localhost controller node id alias to the actual
                    # ID for the controller node. We shouldn't be doing this here (this code
                    # shouldn't have anything about specific models), but it's the easiest
                    # way to handle it for now and this code is likely going away sometime
                    # pretty soon (written in May, 2012, let's see how long "pretty soon"
                    # is :-) )
                    if model_name == 'controller-node' and field_name == 'id' and value == 'localhost':
                        value = get_local_controller_id()
                    if type(field_info.django_field_info) == ForeignKey:
                        field_name += '_id'
                    converted_dict[field_name] = value
                
                try:
                    instance = model_info.model_class(**converted_dict)
                    instance.full_clean()
                    instance.save()
                except ValidationError, err:
                    handle_validation_error(model_info, err)
                    #raise RestValidationException(err)
                except Exception, e:
                    # traceback.print_exc()
                    raise RestSaveException(e)
                
        end_batch_notification()
    elif request.method == 'DELETE':
        begin_batch_notification()
        for instance in instance_list:
            try:
                instance.delete()
            except ValidationError, err:
                handle_validation_error(model_info, err)
            except Exception, e:
                raise RestException(e)
        end_batch_notification()
        response_data = "deleted"
        response_content_type = 'text/plain'
    else:
        raise RestInvalidMethodException()
    
    return HttpResponse(response_data, response_content_type)

def synthetic_controller_interface(model_name, query_param_dict, json_response_data):
    # ---
    if model_name == 'controller-interface':
        # For controller-interfaces, when an ip address (netmask too)
        # is left unconfigured, then it may be possible to associate
        # ifconfig details with the interface.
        #
        # Since controller-interfaces has no mechanism to associate 
        # specific ifconfig interfaces with rows, it's only possible to
        # associate ip's when a single unconfigured ip address exists,
        # using a process of elimination.  For all ip address in the
        # ifconfig output, all statically configured controller-interface
        # items are removed.  If only one result is left, and only
        # one controller-interface has an unconfigured ip address
        # (either a dhcp acquired address, or a static address where
        # the ip address is uncofigured), the ifconfig ip address
        # is very-likely to be the one associated with the 
        # controller-interface row.

        # Check the list of values to see if any are configured as dhcp
        dhcp_count = 0
        unconfigured_static_ip = 0
        this_host = get_local_controller_id()

        for entry in json_response_data:
            if 'mode' in entry and entry['mode'] == 'dhcp' and \
               'controller' in entry and entry['controller'] == this_host:
                    dhcp_count += 1
            if 'mode' in entry and entry['mode'] == 'static' and \
               'ip' in entry and entry['ip'] == '':
                    unconfigured_static_ip += 1
        if dhcp_count + unconfigured_static_ip != 1:
            for entry in json_response_data:
                entry['found-ip']        = entry['ip']
            return

        need_controller_query = False
        # determine whether the complete list of interfaces needs
        # to be collected to associate the dhcp address.
        for query_param_name, query_param_value in query_param_dict.items():
            if query_param_name != 'controller':
                need_controller_query = True
            if query_param_name == 'controller' and \
               query_param_value != this_host:
                need_controller_query = True

        if need_controller_query == False:
            model_interfaces = [x for x in json_response_data
                                if 'controller' in x and x['controller'] == this_host]
        else:
            # print 'need to collect all interfaces'
            filter_keyword_args = {'controller' : this_host}
            model_info = rest_model_info_dict.get(model_name)
            instance_list = model_info.model_class.objects.filter(**filter_keyword_args)
            response_data = []
            for instance in instance_list:
                data = {}
                for field_info in model_info.field_name_dict.values():
                    field_name = field_info.name
                    if type(field_info.django_field_info) == ForeignKey:
                        field_name += '_id'
                    value = instance.__dict__.get(field_name)
                    if value != None:
                        if field_info.json_serialize:
                            value = str(value)
                        data[field_info.rest_name] = value
                response_data.append(data)
            model_interfaces = response_data

        # Recompute the number of dhcp configured interfaces,
        # model_interfaces is the collection of interface for 'this_host'
        dhcp_count = 0
        unconfigured_static_ip = 0
        for ifs in model_interfaces:
            if 'mode' in ifs and ifs['mode'] == 'dhcp':
                dhcp_count += 1
            if 'mode' in ifs and ifs['mode'] == 'static' and \
               'ip' in ifs and ifs['ip'] == '':
                    unconfigured_static_ip += 1

        if dhcp_count + unconfigured_static_ip != 1:
            # print "Sorry, %s dhcp + %s unconfigured static interfaces on %s" % \
                  # (dhcp_count, unconfigured_static_ip, this_host)
            # copy over static ip's
            for entry in json_response_data:
                entry['found-ip']        = entry['ip']
            return

        # collect current details for all the network interfaces
        inet4_ifs = _collect_system_interfaces()

        # iterate over the model_interfaces's interfaces, and 
        # remove ip addresses from inet4_ifs which are static, and
        # have the correct static value.

        report_static = False
        match_id = ''

        for ifs in model_interfaces:
            if 'mode' in ifs and ifs['mode'] == 'static':
                if 'ip' in ifs and ifs['ip'] == '':
                    # print "Unconfigured static ip for %s", ifs['id']
                    match_id = ifs['id']
                if 'ip' in ifs and ifs['ip'] != '':
                    # find this address in the known addresses
                    remove_entry = -1
                    for index, inet4_if in enumerate(inet4_ifs):
                        if inet4_if['addr'] == ifs['ip']:
                            remove_entry = index
                            break
                    if remove_entry == -1:
                        # print "Static ip %s not found" % ifs['ip']
                        pass 
                    else:
                        del inet4_ifs[remove_entry]
            elif 'mode' in ifs and ifs['mode'] == 'dhcp':
                match_id = ifs['id']
            else:
                # ought to assert here, not_reached()
                pass

        # When only one entry is left in inet, its possible to do the assocation
        if len(inet4_ifs) != 1:
            # print "Incorrect number %s of inet4 interfaces left" % len(inet4_ifs)
            pass

        for entry in json_response_data:
            entry['found-ip']        = entry['ip']
            entry['found-netmask']   = entry['netmask']

            if entry['id'] == match_id:
                # make sure the address isn't set
                if entry['ip'] == '':
                    entry['found-ip']        = inet4_ifs[0]['addr']
                    entry['found-netmask']   = inet4_ifs[0]['netmask']
                    entry['found-broadcast'] = inet4_ifs[0]['broadcast']
                else:
                    # ought to assert here, not_reached()
                    pass

@safe_rest_view
def do_synthetic_instance(request, model_name, id=None):
    
    if request.method != 'GET':
        raise RestInvalidMethodException()

    # Lookup the model class associated with the specified name
    model_info = rest_model_info_dict.get(model_name)
    if not model_info:
        raise RestInvalidDataTypeException(model_name)
            
    # Set up the keyword argument dictionary we use to filter the QuerySet.
    filter_keyword_args = {}

    jsonp_prefix = None
    nolist = False
    order_by = None
    
    # Now iterate over the query params and add further filter keyword arguments
    query_param_dict = request.GET
    for query_param_name, query_param_value in query_param_dict.items():
        add_query_param = False
        query_param_name = str(query_param_name)
        query_param_value = str(query_param_value)
        if query_param_name == 'callback': #switching to match up with jquery getJSON call naming convention.
            jsonp_prefix = query_param_value
        elif query_param_name == 'nolist':
            if query_param_value not in ('False', 'false', '0', ''):
                nolist = True
        elif query_param_name == 'orderby':
            order_by = query_param_value.split(',')
            for i in range(len(order_by)):
                name = order_by[i]
                if name.startswith('-'):
                    descending = True
                    name = name[1:]
                else:
                    descending = False
                field_info = model_info.rest_name_dict.get(name)
                if not field_info:
                    raise RestInvalidOrderByException(name)
                name = field_info.name
                if descending:
                    name = '-' + name
                order_by[i] = name
        elif query_param_name in model_info.rest_name_dict:
            field_info = model_info.rest_name_dict.get(query_param_name)
            query_param_name = field_info.name
            add_query_param = True
        else:
            double_underscore_start = query_param_name.find("__")
            if double_underscore_start >= 0:
                rest_name = query_param_name[:double_underscore_start]
                field_info = model_info.rest_name_dict.get(rest_name)
                if field_info:
                    operation = query_param_name[double_underscore_start:]
                    query_param_name = field_info.name
                    if type(field_info.django_field_info) == ForeignKey:
                        query_param_name = query_param_name + '__' + field_info.django_field_info.rel.field_name
                    # Substitute in the model field name for the (possible renamed) rest name
                    query_param_name += operation
                    add_query_param = True
        if add_query_param:
            filter_keyword_args[query_param_name] = query_param_value

    if id != None:
        if len(filter_keyword_args) > 0:
            raise RestInvalidFilterParameterException(filter_keyword_args.keys()[0])
        try:
            get_args = {model_info.primary_key:id}
            instance = model_info.model_class.objects.get(**get_args)
            instance_list = (instance,)
            nolist = True
        except model_info.model_class.DoesNotExist,e:
            raise RestResourceNotFoundException(request.path)
        except model_info.model_class.MultipleObjectsReturned, exc:
            # traceback.print_exc()
            raise RestInternalException(exc)
    elif (request.method != 'PUT') or (len(filter_keyword_args) > 0):
        # Get the QuerySet based on the keyword arguments we constructed
        instance_list = model_info.model_class.objects.filter(**filter_keyword_args)
        if order_by:
            instance_list = instance_list.order_by(*order_by)
    else:
        # We're inserting new objects, so there's no need to do a query
        instance_list = None
        
    response_content_type = JSON_CONTENT_TYPE
    
    # Syntheric types only do requests --
    json_response_data = []
    for instance in instance_list:
        json_instance = {}
        for field_info in model_info.field_name_dict.values():
            # Made some minor edits to deal with a) fields that are empty and b) fields that are not strings -Kyle
            # Changed this to only do an explicit string conversion if it's a unicode string.
            # The controller is expecting to get the unstringified value (e.g. for boolean values)
            # Not sure if this will break things in the UI, but we'll need to resolve how
            # we want to handle this. Also, how do we want to handle unicode strings? -- robv
            field_name = field_info.name
            if type(field_info.django_field_info) == ForeignKey:
                field_name += '_id'
            value = instance.__dict__.get(field_name)
            if value != None:
                if field_info.json_serialize:
                    value = str(value)
                json_instance[field_info.rest_name] = value
        json_response_data.append(json_instance)
    
    # ---
    if model_name == 'controller-interface':
        synthetic_controller_interface(model_name, query_param_dict, json_response_data)

    # Convert to json
    response_data = simplejson.dumps(json_response_data)
    
    # If the nolist query param was enabled then check to make sure
    # that there was only a single instance in the response list and,
    # if so, unpack it from the list
    if nolist:
        if len(json_response_data) != 1:
            raise RestNoListResultException()
        json_response_data = json_response_data[0]
    
    # If the jsonp query parameter was specified, wrap the data with
    # the jsonp prefix
    if jsonp_prefix:
        response_data = jsonp_prefix + '(' + response_data + ')'
        # We don't really know what the content type is here, but it's typically javascript
        response_content_type = TEXT_JAVASCRIPT_CONTENT_TYPE

    return HttpResponse(response_data, response_content_type)

@safe_rest_view
def do_user_data_list(request):
    # Now iterate over the query params and add any valid filter keyword arguments
    filter_keyword_args = {}
    for query_param_name, query_param_value in request.GET.items():
        query_param_name = str(query_param_name)
        double_underscore_start = query_param_name.find("__")
        if double_underscore_start >= 0:
            attribute_name = query_param_name[:double_underscore_start]
        else:
            attribute_name = query_param_name
            
        # In the future, if we add support for things like mod_date, creation_date, etc.
        # which would be supported in query params, then they'd be added to this list/tuple.
        if attribute_name not in ('name',):
            raise RestInvalidFilterParameterException(query_param_name)
        filter_keyword_args[query_param_name] = query_param_value

    instance_list = UserData.objects.filter(**filter_keyword_args)
    
    if request.method == 'GET':
        user_data_info_list = []

        # FIXME: robv: It's incorrect to *always* add this to the user data,
        # because it means we're not respecting the filter query parameters.
        # To work completely correctly we'd need to duplicate a lot of logic
        # for processing the query parameters, which would be tedious.
        # Should talk to Mandeep about why it was done this way. Maybe we
        # should expose these special cases in a different URL/view.
        for fn in ['startup-config', 'upgrade-config']:
            try:
                sc = "%s/run/%s" % (sdncon.SDN_ROOT, fn)
                f = open(sc, 'r')
                f.close()
                t = time.strftime("%Y-%m-%d.%H:%M:%S",
                                  time.localtime(os.path.getmtime(sc)))
                instance_name = fn + '/timestamp=' + t + \
                                '/version=1/length=' + \
                                str(os.path.getsize(sc))
                url_path = 'rest/v1/data/' + instance_name + '/'
                            
                user_data_info = { 'name' : instance_name,
                                   'url_path' : url_path, }
                user_data_info_list.append(user_data_info)
            except:
                pass

        for instance in instance_list:
            user_data_info = {'name': instance.name,
                              'url_path': 'rest/v1/data/' + instance.name + '/'}
            user_data_info_list.append(user_data_info)
        
        response_data = simplejson.dumps(user_data_info_list)
    elif request.method == 'DELETE':
        instance_list.delete()
        response_data = {}
        response_data['status'] = 'success'
        response_data['message'] = 'user data deleted'
        response_data = simplejson.dumps(response_data)
        response_content_type = JSON_CONTENT_TYPE
    else:
        raise RestInvalidMethodException()
        
    return HttpResponse(response_data, JSON_CONTENT_TYPE)

@safe_rest_view
def do_user_data(request, name):
    query_param_dict = request.GET
    #
    # Manage startup-config/update-config differently
    if name.find('/') >= 0 and \
       name.split('/')[0] in ['startup-config', 'upgrade-config']:
        path = "%s/run/%s" % (sdncon.SDN_ROOT, name.split('/')[0])
        response_data = {}

        if request.method == 'GET':
            with open(path, 'r') as f:
                response_data = f.read()
            response_content_type = "text/plain"
        elif request.method == 'PUT':
            try:
                with open(path, 'w') as f:
                    f.write(request.raw_post_data)
                response_data['status'] = 'success'
                response_data['message'] = 'user data updated'
            except:
                response_data['status'] = 'failure'
                response_data['message'] = "can't write file"
            response_content_type = JSON_CONTENT_TYPE
            response_data = simplejson.dumps(response_data)
        elif request.method == 'DELETE':
            try:
                f = open(path, "r")
                f.close()
            except:
                raise RestResourceNotFoundException(request.path)

            try:
                os.remove(path)
                response_data['status'] = 'success'
                response_data['message'] = 'user data deleted'
            except:
                response_data['status'] = 'failure'
                response_data['message'] = "can't delete file"
            response_data = simplejson.dumps(response_data)
            response_content_type = JSON_CONTENT_TYPE
        else:
            raise RestInvalidMethodException()
            
        return HttpResponse(response_data, response_content_type)
            
    
    # Default values for optional query parameters
    #private = False
    binary = False
    
    for param_name, param_value in query_param_dict.items():
        if param_name == 'binary':
            if request.method != 'PUT':
                raise RestInvalidQueryParameterException(name)
            binary = param_value.lower() == 'true' or param_value == '1'
        #elif param_name == 'private':
        #    private = param_value
        else:
            raise RestInvalidQueryParameterException(param_name)
    
    # FIXME: Need HTTP basic/digest auth support for the following
    # code to work.
    #if private:
    #    user = request.user
    #else:
    #    user = None
    #if user != None and not user.is_authenticated():
    #    raise RestAuthenticationRequiredException()
    user = None
    
    # There's currently an issue with filtering on the user when using the
    # Cassandra database backend. Since we don't support private per-user
    # data right now, I'm just disabling filtering on the user and only
    # filter on the name
    #user_data_query_set = UserData.objects.filter(user=user, name=name)
    user_data_query_set = UserData.objects.filter(name=name)

    count = user_data_query_set.count()
    if count > 1:
        raise RestInternalException('Duplicate user data values for the same name')
    
    if request.method == 'GET':
        if count == 0:
            raise RestResourceNotFoundException(request.path)
        user_data = user_data_query_set[0]
        response_data = user_data.data
        if user_data.binary:
            response_data = base64.b64decode(response_data)
        response_content_type = user_data.content_type
    elif request.method == 'PUT':
        content_type = request.META['CONTENT_TYPE']
        if content_type == None:
            if binary:
                content_type = BINARY_DATA_CONTENT_TYPE
            else:
                content_type = JSON_CONTENT_TYPE
        response_data = {}
        if count == 1:
            response_data['status'] = 'success'
            response_data['message'] = 'user data updated'
            user_data = user_data_query_set[0]
        else:
            response_data['status'] = 'success'
            response_data['message'] = 'user data created'
            user_data = UserData(user=user,name=name)
        user_data.binary = binary
        user_data.content_type = content_type
        data = request.raw_post_data
        if binary:
            data = base64.b64encode(data)
        user_data.data = data
        user_data.save()
        response_data = simplejson.dumps(response_data)
        response_content_type = JSON_CONTENT_TYPE
    elif request.method == 'DELETE':
        if count == 0:
            raise RestResourceNotFoundException(request.path)
        user_data = user_data_query_set[0]
        user_data.delete()
        response_data = {}
        response_data['status'] = 'success'
        response_data['message'] = 'user data deleted'
        response_data = simplejson.dumps(response_data)
        response_content_type = JSON_CONTENT_TYPE
    else:
        raise RestInvalidMethodException()
   
    return HttpResponse(response_data, response_content_type)

@safe_rest_view
def do_sdnplatform_tunnel_manager(request, dpid=None):
    """
    This returns realtime statistics from sdnplatform
    """

    if request.method != 'GET':
        raise RestInvalidMethodException()
    if dpid == None:
        raise RestInvalidMethodException()

    print 'DPID', dpid
    if dpid == 'all':
        url = controller_url('vns', 'tunnel-manager', 'all', 'json')
    else:
        url = controller_url('vns', 'tunnel-manager', 'switch='+dpid, 'json')

    response_text = urllib2.urlopen(url).read()
    entries = simplejson.loads(response_text)

    if 'error' in entries and entries['error'] != None:
        RestInternalException(entries['error'])

    return HttpResponse(json.dumps(entries['tunnMap']), JSON_CONTENT_TYPE)

@safe_rest_view
def do_sdnplatform_controller_summary(request):
    """
    This returns summary statistics from sdnplatform modules
    """
    if request.method != 'GET':
        raise RestInvalidMethodException()

    url = controller_url('core', 'controller', 'summary', 'json')
    return get_sdnplatform_response(url)

def filter_queries(choice_list, param_dict):
    return dict([[x, param_dict[x]] for x in choice_list
                if x in param_dict and param_dict[x] != 'all'])

@safe_rest_view
def do_reload(request):
    """
    This calls an oswrapper that reloads the box.
    """
    if request.method != 'GET':
        raise RestInvalidMethodException()
    exec_os_wrapper("ReloadController", 'set', [])
    response_text = '{"status":"reloading"}'
    return HttpResponse(response_text, JSON_CONTENT_TYPE)

@safe_rest_view
def do_resetbsc(request):
    """
    This calls an oswrapper that resets the box.
    """
    if request.method != 'PUT':
        raise RestInvalidMethodException()
    exec_os_wrapper("ResetBsc", 'set', [])
    response_text = '{"status":"resetting"}'
    return HttpResponse(response_text, JSON_CONTENT_TYPE)

@safe_rest_view
def do_abort_upgrade(request):
    """
    This calls an oswrapper that reloads the box.
    """
    if request.method != 'PUT':
        raise RestInvalidMethodException()
    controller_id = get_local_controller_id()
    controller = Controller.objects.get(id=controller_id)
    if controller.status != 'Upgrading':
        raise RestUpgradeException("No Upgrade pending")
    exec_os_wrapper("AbortUpgrade", 'set', [])
    controller.status = 'Ready'
    controller.save()
    response_text = '{"status":"Ready"}'
    return HttpResponse(response_text, JSON_CONTENT_TYPE)

@safe_rest_view
def do_config_rollback(request):
    data = simplejson.loads(request.raw_post_data)
    path = data['path']
    print "Executing config rollback with config @", path
    
    if request.method != 'PUT':
        raise RestInvalidMethodException()
    exec_os_wrapper("RollbackConfig", 'set', [path])
    response_text = get_successful_response_data('prepared config rollbacked')
    return HttpResponse(response_text, JSON_CONTENT_TYPE)

@safe_rest_view
def do_upload_data(request):
    if request.method != 'PUT':
        raise RestInvalidMethodException()
    data = simplejson.loads(request.raw_post_data)
    content = data['data']
    path = data['dst']
    print "Executing config rollback with config @", path
    
    exec_os_wrapper("WriteDataToFile", 'set', [content, path])
    response_text = get_successful_response_data('written data')
    return HttpResponse(response_text, JSON_CONTENT_TYPE)

@safe_rest_view
def do_diff_config(request):
    if request.method != 'PUT':
        raise RestInvalidMethodException()
    data = simplejson.loads(request.raw_post_data)
    config1 = data['config-1']
    config2 = data['config-2']
    print "diffing '%s' with '%s'" %(config1, config2)
    
    result = exec_os_wrapper("DiffConfig", 'set', [config1, config2])
    return HttpResponse(simplejson.dumps(result), JSON_CONTENT_TYPE)

@safe_rest_view
def do_extract_upgrade_pkg_manifest(request):
    """
    This calls an oswrapper that extracts the upgrade package.
    This returns the install package 'manifest'.
    """
    if request.method != 'GET':
        raise RestInvalidMethodException()
    exec_os_wrapper("GetLatestUpgradePkg", 'get', [])
    output = exec_os_wrapper("CatUpgradeImagesFile", 'get')
    upgradePkg = output['out'].strip()
    exec_os_wrapper("ExtractUpgradePkgManifest", 'set', [upgradePkg])
    output = exec_os_wrapper("ExtractUpgradePkgManifest", 'get')
    manifest = output['out']
    return HttpResponse(manifest, JSON_CONTENT_TYPE)

@safe_rest_view
def do_extract_upgrade_pkg(request):
    if request.method != 'GET':
        raise RestInvalidMethodException()
    exec_os_wrapper("GetLatestUpgradePkg", 'get', [])
    output = exec_os_wrapper("CatUpgradeImagesFile", 'get')
    upgradePkg = output['out'].strip()
    exec_os_wrapper("ExtractUpgradePkg", 'set', [upgradePkg])
    return HttpResponse('{"status": "OK"}', JSON_CONTENT_TYPE)

@safe_rest_view
def do_get_upgrade_pkg(request):
    """
    This calls an oswrapper to get the latest upgrade
    package uploaded to the controller.
    """
    if request.method != 'GET':
        raise RestInvalidMethodException()
    exec_os_wrapper("GetLatestUpgradePkg", 'get')
    result = exec_os_wrapper("CatUpgradeImagesFile", 'get')
    jsondict = {'file': result['out'].strip()}
    return HttpResponse(simplejson.dumps(jsondict), JSON_CONTENT_TYPE)

@safe_rest_view
def do_cleanup_old_pkgs(request):
    if request.method != 'GET':
        raise RestInvalidMethodException()
    exec_os_wrapper("CleanupOldUpgradeImages", 'get')
    return HttpResponse('{"status": "OK"}', JSON_CONTENT_TYPE)

@safe_rest_view
def do_execute_upgrade_step(request):
    """
    Executes a particular upgrade step according to the
    upgrade package manifest.
    """
    if request.method != 'PUT':
        raise RestInvalidMethodException()

    put_data = json.loads(request.raw_post_data)
    imageName = put_data.get('imageName')
    stepNum = put_data.get('step')
    force = put_data.get('force')
    
    args = [stepNum, imageName]
    if force:
        args.append("--force")
    result = exec_os_wrapper("ExecuteUpgradeStep", 'get', 
                             args)
    jsondict = {}
    if len(str(result['err']).strip()) > 0:
        jsondict['status'] = "ERROR"
        jsondict['description'] = str(result['err']).strip()
    else:
        jsondict['status'] = "OK"
        jsondict['description'] = str(result['out']).strip()

    return HttpResponse(simplejson.dumps(jsondict), JSON_CONTENT_TYPE)

@safe_rest_view
def do_sdnplatform_tunnel_config(request):    
    if request.method != 'PUT' and request.method != 'DELETE':
        raise RestInvalidMethodException()

    url = controller_url('onos', 'segmentrouting', 'tunnel')
    post_data = request.raw_post_data
    put_request = urllib2.Request(url, post_data)
    method = request.method
    if method == 'PUT':
        method = 'POST'
    put_request.get_method = lambda: method
    put_request.add_header('Content-Type', 'application/json')
    response = urllib2.urlopen(put_request)
    response_text = response.read()
    response = HttpResponse(response_text, JSON_CONTENT_TYPE)
    
    return response

@safe_rest_view
def do_sdnplatform_policy_config(request):    
    if request.method != 'PUT' and request.method != 'DELETE':
        raise RestInvalidMethodException()

    url = controller_url('onos', 'segmentrouting', 'policy')
    post_data = request.raw_post_data
    put_request = urllib2.Request(url, post_data)
    method = request.method
    if method == 'PUT':
        method = 'POST'
    put_request.get_method = lambda: method
    put_request.add_header('Content-Type', 'application/json')
    response = urllib2.urlopen(put_request)
    response_text = response.read()
    response = HttpResponse(response_text, JSON_CONTENT_TYPE)
    
    return response

@safe_rest_view
def do_show_tunnel(request):    
    #if request.method != 'GET':
    #    raise RestInvalidMethodException()

    url = controller_url('onos', 'segmentrouting','tunnel')
    if request.META['QUERY_STRING']:
        url += '?' + request.META['QUERY_STRING']
    return get_sdnplatform_response(url)

@safe_rest_view
def do_show_policy(request):    
    #if request.method != 'GET':
    #    raise RestInvalidMethodException()

    url = controller_url('onos', 'segmentrouting','policy')
    if request.META['QUERY_STRING']:
        url += '?' + request.META['QUERY_STRING']
    return get_sdnplatform_response(url)