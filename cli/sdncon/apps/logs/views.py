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

#  Views for the application
#

from django.http import HttpResponse
from django.shortcuts import render_to_response
from django.utils import simplejson
from sdncon.rest.jsonview import JsonResponse
from sdncon.apploader import AppLoader, AppLister
from sdncon.stats.views import init_db_connection
from sdncon.stats.data import get_log_event_data
import os
import time
import sdncon

ComponentXlation = {
    'cassandra': 'database',
    'sdncon': 'sdncon',
    'sdnplatform': 'controller'
}

def log_collection_enabled(config_file="%s/statd/statd.conf" % sdncon.SDN_ROOT):
    import json
    enabled = False
    try:
        with open(config_file, 'r') as fp:
            conf = json.load(fp)
            if conf.get('input'):
                for i in conf['input']:
                    if i.get('name') == 'LogCollector' and i.get('modules'):
                        enabled = True
                        break;
    except:
        pass
    return enabled

def bsc_app_init():
    # Check if logging is enabled, else do not start the app
    if not log_collection_enabled():
        return

    # By default, App Name is the same as directory name. Change if needed.
    APP_NAME = os.path.dirname(__file__).split("/")[-1]   
    
    # Create the App. Parameters are 
    # - Name: the id, lowercase letters only
    # - Label: Human readable discription for the menu to the left
    # - Priority: determines ranking the menu to the left), One-line description
    # - Description: One line description of the app
    app = AppLister(APP_NAME, "Logs", 5, "Controller Logs")

    # Add Tabs. Parameters are:
    # - Name: the id, lowercase letters only
    # - Label: Human readable discription for the menu to the left
    # - View: name of the python function that contains the django view (see below)
    app.addTab("controllerlogs", "Controller Logs", controller)
    app.addTab("dblogs", "Database Logs", db)
    app.addTab("sdnconlogs", "SDNCon Logs", sdncon)
    app.addTab("alllogs", "All Logs", all)
    AppLoader.addApp(app)

# Views - functions that serve the HTML that goes into each tab
def controller(request):
    return render_to_response('apps/logs/templates/logs.html', { 'source' : 'controller' } )

def db(request):
    return render_to_response('apps/logs/templates/logs.html', { 'source' : 'db' } )

def sdncon(request):
    return render_to_response('apps/logs/templates/logs.html', { 'source' : 'sdncon' } )

def all(request):
    return render_to_response('apps/logs/templates/logs.html', { 'source' : 'all' } )

# Views defined in urls.py
def controller_log_data_all(request, cluster, controller):
    init_db_connection()
    time_now = time.time()*1000 #conver to ms
    # we only get one day for now, this will be fixed when we move to an actually scalable solution
    data_dict = get_log_event_data(cluster, controller, time_now - 86400000, time_now)
    arr = get_array_from_dict(data_dict, True)
    return generic_server_side_datatable(request, arr)

def controller_log_data_controller(request, cluster, controller):
    init_db_connection()
    time_now = time.time()*1000 #conver to ms
    # we only get one day for now, this will be fixed when we move to an actually scalable solution
    data_dict = get_log_event_data(cluster, controller, time_now - 86400000, time_now)
    filtered_dict = []
    for e in data_dict:
        if e['component'] == 'sdnplatform' or e['component'] == 'sdnplatform.request':
            filtered_dict.append(e)
    arr = get_array_from_dict(filtered_dict, False)
    return generic_server_side_datatable(request, arr)

def controller_log_data_db(request, cluster, controller):
    init_db_connection()
    time_now = time.time()*1000 #conver to ms
    # we only get one day for now, this will be fixed when we move to an actually scalable solution
    data_dict = get_log_event_data(cluster, controller, time_now - 86400000, time_now)
    filtered_dict = []
    for e in data_dict:
        if e['component'] == 'cassandra':
            filtered_dict.append(e)
    arr = get_array_from_dict(filtered_dict, False)
    return generic_server_side_datatable(request, arr)

def controller_log_data_sdncon(request, cluster, controller):
    init_db_connection()
    time_now = time.time()*1000 #conver to ms
    # we only get one day for now, this will be fixed when we move to an actually scalable solution
    data_dict = get_log_event_data(cluster, controller, time_now - 86400000, time_now)
    filtered_dict = []
    for e in data_dict:
        if e['component'] == 'sdncon':
            filtered_dict.append(e)
    arr = get_array_from_dict(filtered_dict, False)
    return generic_server_side_datatable(request, arr)


# Helper functions
def generic_server_side_datatable(request, data_array): 
    start   = request.GET.get("iDisplayStart",0)
    length  = request.GET.get("iDisplayLength",10)
    search_str  = request.GET.get("sSearch",None)
    
    if search_str:
        data_array_f = []
        for d in data_array:
            for e in d:
                try:
                    if e.find(search_str) > -1:
                        data_array_f.append(d)
                        break
                except Exception:
                    pass
    else:
        data_array_f = data_array

    data_subset = data_array_f[int(start):int(start)+int(length)]
    response_data = '{ "aaData":'+simplejson.dumps(data_subset) + ', "iTotalDisplayRecords": ' + str(len(data_array_f)) + ', "iTotalRecords": ' + str(len(data_array_f)) +'}'
    return HttpResponse(response_data, 'application/json')

# Takes a dictionary that includes 
# { "timestamp": unix_ts, "level" : "string", "message": "msg" }
# and turns it into a 2D array that includes
# [ [unix_ts, level, message], [etc], [etc] ] 
def get_array_from_dict(dict, includeComp):
    array = []
    for element in reversed(dict):
        if 'component' in element and element['component'] == 'sdnplatform.request':
            entry = parse_sdnplatform_request_log_entry(element)
        else:
            entry = []
            if 'timestamp' in element:
                entry.append(element['timestamp'])
            else:
                entry.append('-')
            if 'log-level' in element:
                entry.append(element['log-level'])
            else:
                entry.append('-')
            if 'message' in element:
                entry.append(element['message'])
            else:
                entry.append('-')

        if includeComp:
            if 'component' in element:
                entry.insert(1, ComponentXlation[element['component']])
            else:
                entry.append(1, '-')

        array.append(entry)
    return array

# Parses a SDNPlatform request log entry into an array
# Format is similar to
# {
#   'package': 'Python-urllib/2.7', 
#   'timestamp': 1306967011000L, 
#   'component': 'sdnplatform.request', 
#   'request': 'GET /wm/core/counter/00:00:00:00:00:73:28:02_OFPacketIn_L3_IPv4/json HTTP/1.1', 
#   'responseLen': '37', 
#   'client': '127.0.0.1', 
#   'responseCode': '200'
# }
# Output array is [unix_ts, level, message]
def parse_sdnplatform_request_log_entry(element):
    entry = []
    if 'timestamp' in element:
        entry.append(element['timestamp'])
    else:
        entry.append('-')
    if 'responseCode' in element:
        rc = element['responseCode']
        if rc == '404' or rc == '500':
            entry.append('ERROR')
        elif rc == '200':
            entry.append('INFO')
        else:
            entry.append('-')
    else:
        entry.append('-')
    msg = ''
    if 'request' in element:
        msg += element['request']
    if 'client' in element:
        msg += ' CLIENT ' + element['client']
    if 'responseCode' in element:
        msg += ' ' + element['responseCode']
    if 'package' in element:
        msg += ' ' + element['package']
    entry.append(msg)
    return entry

