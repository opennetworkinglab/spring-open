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
# Python script for querying API and displaying connected hosts in a table
#
#

#Importing modules
import re
import sys
import time
import datetime
import json
import urllib2
import switchalias
from sdncon.rest.views import do_switches, do_model_list, do_instance, do_device

def show_host_data(request):

    # Query JSON from API and load into dictionary
    switches = json.loads(do_switches(request).content)
    switchdevices = json.loads(do_device(request).content)
    
    # Dictionaries
    sorteddict = []
    aliasDict = switchalias.aliasDict(request)
    unsortdict = []
     
    # Step through master 'device' list, extract entry for each dictionary.
    for index_devices,value1_devices in enumerate(switchdevices):
      tempdict = {}
      tempswitchdict = {}
    
      # get needed entries in 'devices'
      tempdict['mac'] = value1_devices.get('mac','')
      tempdict['entityClass'] = value1_devices.get('entityClass','')
      tempdict['vlan'] = value1_devices.get('vlan','')
      tempdict['ipv4'] = value1_devices.get('ipv4','Unknown')
      tempdict['switch'] = value1_devices.get('attachmentPoint','')
      tempdict['lastSeen'] = value1_devices.get('lastSeen','')
    
      # append to final sorted output.
      unsortdict.append(tempdict)
    
    
    sorteddict = sorted(unsortdict, key=lambda elem: "%s" % (elem['mac']))
    
    #print sorteddict
    #print time.strftime('%Y-%m-%d %H:%M:%S %Z', time.gmtime(sorteddict[0]['connectedSince'] / float(1000)))
    
    result = ''
    # Print table output
    result += '<table id="showdeviceoutput" >'
    result += '<tbody style="border-top: 0px;">'
    result += '<tr><td>ID</td><td>MAC Address</td><td>Address Space</td><td>VLAN</td><td>IP</td><td>Switch</td><td>Last Seen</td></tr>'
    for index_output,value_output in enumerate(sorteddict):
        result += '<tr>'
        result += '  <td>' + str(index_output + 1) + '</td>'
        result += '  <td>'
        for tmp_index,tmp_value in enumerate(value_output['mac']):
            if tmp_index > 0:
              result += ', ',
            result += str(tmp_value)
        result += '</td>'
        result += '  <td>' + value_output.get('entityClass','') + '</td>'
        result += '  <td>'
        for tmp_index,tmp_value in enumerate(value_output['vlan']):
            if tmp_index > 0:
              result += ', ',
            result += str(tmp_value)
        result += '</td>'
        result += '  <td>'
        for tmp_index,tmp_value in enumerate(value_output['ipv4']):
            if tmp_index > 0:
              result += ', ',
            result += str(tmp_value)
        result += '</td>'
        result += '  <td>'
        for tmp_index,tmp_value in enumerate(value_output['switch']):
            if tmp_index > 0:
              result += ', ',
            result += aliasDict.get(tmp_value.get('switchDPID', 'UNKNOWN'), tmp_value.get('switchDPID', 'UNKNOWN')) + ' Port ' + str(tmp_value.get('port', 'UNKNOWN'))
        result += '</td>'
        delta = round(time.time(),0) - (round(value_output.get('lastSeen',time.time()) / int(1000)))
        if delta <= 0:
          result += '  <td> Now </td>'
        else:
          result += '  <td>' + str(datetime.timedelta(seconds=delta))  + '</td>'
        result += '</tr>'
    result += '</tbody>'
    result += '</table>'  
    return result
