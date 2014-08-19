#!/usr/bin/python
#
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
# Python script for querying REST API and displaying connected switches in
# a table 
#

#Importing modules
import re
import sys
import time
import json
import urllib2
from sdncon.rest.views import do_switches, do_model_list, do_instance

def show_switch_data(request):
    
    switches = json.loads(do_switches(request).content)
    switchaliases = json.loads(do_instance(request, 'switch-alias').content)
    switchconfig = json.loads(do_instance(request, "switch-config").content)

    # Dictionaries
    sorteddict = []
     
    # Step through master 'switches' list, extract entry for each dictionary.
    for index_switches,value1_switches in enumerate(switches):
      tempdict = {}
      tempaliasdict = {}
      tempconfigdict = {}
    
      # get needed entries in 'switches'
      tempdict['dpid'] = value1_switches.get('dpid','')
      tempdict['inetAddress'] = value1_switches.get('inetAddress','')
      tempdict['connectedSince'] = value1_switches.get('connectedSince','')
    
      # get related entries in other JSON queries
      for index_switchaliases,value1_switchaliases in enumerate(switchaliases):
        if value1_switchaliases['switch'] == value1_switches['dpid']:
          tempaliasdict['alias'] = value1_switchaliases.get('id','')
      tempdict['alias'] = tempaliasdict.get('alias','')
    
      for index_switchconfig,value1_switchconfig in enumerate(switchconfig):
        if value1_switchconfig['dpid'] == value1_switches['dpid']:
          tempconfigdict['core-switch'] = value1_switchconfig.get('core-switch','')
          tempconfigdict['tunnel-termination'] = value1_switchconfig.get('tunnel-termination','')
      tempdict['core-switch'] = tempconfigdict.get('core-switch','')
      tempdict['tunnel-termination'] = tempconfigdict.get('tunnel-termination','')
      
      # append to final sorted output.
      sorteddict.append(tempdict)
    sorteddict.reverse()
    
    result = ''
    # Print table output
    result += '<table id="showswitchoutput">'
    result += '<tbody>'
    result += '<tr><td>ID</td><td>Alias</td><td>Switch DPID</td><td>IP Address</td><td>Connected Since</td></tr>'
    for index_output,value_output in enumerate(sorteddict):
      formatIPresult = re.search('/(.*):',value_output['inetAddress'])
      result += '<tr>'
      result += '  <td>' + str(index_output + 1) + '</td>'
      result += '  <td>' + value_output.get('alias','') + '</td>'
      result += '  <td>' + value_output.get('dpid','') + '</td>'
      result += '  <td>' + formatIPresult.group(1) + '</td>'
      if value_output['connectedSince'] != '':
        result += '  <td>' + time.strftime('%Y-%m-%d %H:%M:%S %Z', time.gmtime(value_output['connectedSince'] / float(1000))) + '</td>'
      else:
        result += '  <td>Disconnected</td>'
      result += '</tr>'
    result += '</tbody>'
    result += '</table>'
    
    return result

