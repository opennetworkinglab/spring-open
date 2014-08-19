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
# Build a simple JSON of the topology for use in the ForceDirected 
# Visualization in the InfoVis Toolkit
#

#Importing modules
import re
import sys
import time
import json
import urllib2
import switchalias
from sdncon.rest.views import do_switches, do_model_list, do_instance, do_device, do_links

def build_topology_data(request):
    
    # Query JSON from API and load into dictionary
    switches = json.loads(do_switches(request).content)
    devices = json.loads(do_device(request).content)
    links = json.loads(do_links(request).content)
    aliasDict = switchalias.aliasDict(request)
    
    # Dictionaries
    parsedswitch = []
    parseddevices = []
    parsedlinks = []
    
    # Step through master 'switches' list, extract entry for each dictionary.
    for index_switches,value1_switches in enumerate(switches):
      tempdict = {}
    
      # get needed entries in 'switches'
      tempdict['dpid'] = value1_switches.get('dpid','')
      tempdict['inetAddress'] = value1_switches.get('inetAddress','')
      
      # append to final sorted output.
      parsedswitch.append(tempdict)
     
    # Step through master 'device' list, extract entry for each dictionary.
    for index_devices,value1_devices in enumerate(devices):
      tempdict = {}
    
      # get needed entries in 'devices'
      for index_mac,value_mac in enumerate(value1_devices['mac']):
        tempdict['mac'] = value_mac
      tempdict['ipv4'] = value1_devices.get('ipv4','Unknown')
      for index_switch2,value_switch2 in enumerate(value1_devices['attachmentPoint']):
        switchconnlist = []
        switchtempdict = {}
        if value_switch2.get('switchDPID', 'UNKNOWN') != '':
          if value_switch2.get('port', 'UNKNOWN') != '':
            switchtempdict['port'] = value_switch2['port']
            switchtempdict['DPID'] = value_switch2['switchDPID']
            switchconnlist.append(switchtempdict)
            tempdict['attachments'] = switchconnlist
      # append to final sorted output.
      parseddevices.append(tempdict)
    
    
    # Step through master 'links' list, extract entry for each dictionary.
    for index_links,value1_links in enumerate(links):
      tempdict = {}
    
      # get needed entries in 'links'
      tempdict['src-switch'] = value1_links.get('src-switch','')
      tempdict['src-port'] = value1_links.get('src-port','')
      tempdict['src-port-state'] = value1_links.get('src-port-state','')
      tempdict['dst-switch'] = value1_links.get('dst-switch','')
      tempdict['dst-port'] = value1_links.get('dst-port','')
      tempdict['dst-port-state'] = value1_links.get('dst-port-state','')
    
      # append to final sorted output.
      parsedlinks.append(tempdict)
    
    #Begin puting the data in the JSON list
    jsonoutput = []
    result = ''
    
    
    # Print switches by themselves to handle orphaned switches.
    for index_switch,value_switch in enumerate(parsedswitch):
      adjacenciestmp = []
      datatmp = {}
      
      datatmp = { '$color': '#f15922', '$type': 'square', '$dim': '10'}
      jsonoutput.append({'adjacencies': [], 'data': datatmp, 'id': value_switch['dpid'], 'name': aliasDict.get(value_switch['dpid'], value_switch['dpid'])})
    
    
    # Determine host -> Switch links
    for index_devices,value_devices in enumerate(parseddevices):
      adjacenciestmp = []
      datatmp = {}
      
      for index_adj,value_adj in enumerate(value_devices.get('attachments', '')):
        adjacenciestmp.append({'nodeTo': str(value_adj['DPID']), 'nodeFrom': value_devices['mac'], 'data': { '$color': '#fdb813', '$lineWidth': '3' }})
      datatmp = { '$color': '#fdb813', '$type': 'circle', '$dim': '10'}
      jsonoutput.append({'adjacencies': adjacenciestmp, 'data': datatmp, 'id': value_devices['mac'], 'name': value_devices['mac']})
    
    # Determine Switch -> Switch links
    for index_link,value_link in enumerate(parsedlinks):
      adjacenciestmp = []
      datatmp = {}
      adjacenciestmp.append({'nodeTo': str(value_link['src-switch']), 'nodeFrom': value_link['dst-switch'], 'data': { '$color': '#f15922', '$lineWidth': '3' }})
      datatmp = { '$color': '#f15922', '$type': 'square', '$dim': '10'}
      jsonoutput.append({'adjacencies': adjacenciestmp, 'data': datatmp, 'id': value_link['dst-switch'], 'name': aliasDict.get(value_link['dst-switch'], value_link['dst-switch'])})
    
    result += 'var json =' + json.dumps(jsonoutput, sort_keys=True, indent=4, separators=(',', ': ')) + ';'
    return result 