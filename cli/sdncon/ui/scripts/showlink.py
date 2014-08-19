#!/usr/bin/python
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
#
# Python script to query link states from REST API
#
#

#Importing modules
import re
import sys
import time
import json
import urllib2
import switchalias
from sdncon.rest.views import do_switches, do_model_list, do_instance, do_device, do_links


def show_link_data(request):

    # Query JSON from API and load into dictionary
    switches = json.loads(do_switches(request).content)
    switchlinks = json.loads(do_links(request).content)
    
    # Dictionaries
    sorteddict = []
    aliasDict = switchalias.aliasDict(request)
    unsortdict = []
    statedict = {0: 'FORWARDING', 1: 'DOWN', 2: 'FORWARD', 3: 'BLOCK', 4: 'MASK'}
     
    # Step through master 'links' list, extract entry for each dictionary.
    for index_links,value1_links in enumerate(switchlinks):
        tempdict = {}
        tempswitchdict = {}
      
        # get needed entries in 'links'
        tempdict['src-switch'] = value1_links.get('src-switch','')
        tempdict['src-port'] = value1_links.get('src-port','')
        tempdict['src-port-state'] = value1_links.get('src-port-state','')
        tempdict['dst-switch'] = value1_links.get('dst-switch','')
        tempdict['dst-port'] = value1_links.get('dst-port','')
        tempdict['dst-port-state'] = value1_links.get('dst-port-state','')
        tempdict['type'] = value1_links.get('type','')
      
        # append to final sorted output.
        unsortdict.append(tempdict)
    
    
    sorteddict = sorted(unsortdict, key=lambda elem: "%s %02d" % (elem['src-switch'], elem['src-port']))
    
    result = ''
    # Print table output
    result += '<table id="showlinkoutput" >'
    result += '<tbody>'
    result += '<tr><td>ID</td><td>Source Switch</td><td>Source Port</td><td>Source Port State</td><td>Destination Switch</td><td>Destination Port</td><td>Destination Port State</td><td>Connection Type</td></tr>'
    for index_output,value_output in enumerate(sorteddict):
        result += '<tr>'
        result += '  <td>' + str(index_output + 1) + '</td>'
        result += '  <td>' 
        result += aliasDict.get(value_output.get('src-switch', 'UNKNOWN'), value_output.get('src-switch', 'UNKNOWN'))
        result += '</td>'
        result += '  <td>' + str(value_output.get('src-port','')) + '</td>'
        result += '  <td>' + statedict.get(value_output.get('src-port-state',0),'DOWN') + '</td>'
        result += '  <td>' 
        result += aliasDict.get(value_output.get('dst-switch', 'UNKNOWN'), value_output.get('dst-switch', 'UNKNOWN'))
        result += '</td>'
        result += '  <td>' + str(value_output.get('dst-port','')) + '</td>'
        result += '  <td>' + statedict.get(value_output.get('dst-port-state',0),'DOWN') + '</td>'
        result += '  <td>' + value_output.get('type','') + '</td>'
        result += '</tr>'
    result += '</tbody>'
    result += '</table>'
    return result
  
