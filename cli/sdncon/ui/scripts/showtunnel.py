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
# Python script to query tunnel states from REST API
#
#

#Importing modules
import re
import sys
import time
import json
import urllib2
import switchalias
from sdncon.rest.views import do_sdnplatform_tunnel_manager

def show_tunnel_data(request):

    # Query JSON from API and load into dictionary
    tunnels = json.loads(do_sdnplatform_tunnel_manager(request,'all').content)
    
    # Dictionaries
    sorteddict = []
    aliasDict = switchalias.aliasDict(request)
    unsortdict = []
    statedict = {0: 'FORWARDING', 1: 'DOWN', 2: 'FORWARD', 3: 'BLOCK', 4: 'MASK'}
     
    # Step through master 'tunnels' list, extract entry for each dictionary.
    for index_tunnels,value1_tunnels in tunnels.iteritems():
      tempdict = {}
      temptunneldict = {}
    
      # get needed entries in 'links' 
      tempdict['dpid'] = value1_tunnels.get('hexDpid','')
      tempdict['tunnelEnabled'] = value1_tunnels.get('tunnelEnabled','')
      tempdict['tunnelIPAddr'] = value1_tunnels.get('tunnelIPAddr','')
      tempdict['tunnelActive'] = value1_tunnels.get('tunnelActive','')
      tempdict['tunnelState'] = value1_tunnels.get('tunnelState','')
      tempdict['tunnelIf'] = value1_tunnels.get('tunnelEndPointIntfName','')
      tempdict['tunnelCapable'] = value1_tunnels.get('tunnelCapable','')
      # append to final sorted output.
      if value1_tunnels.get('tunnelCapable',''):
        unsortdict.append(tempdict)
    
    sorteddict = sorted(unsortdict, key=lambda elem: "%s %s" % (elem['dpid'], elem['tunnelIPAddr']))
    
    result = ''
    # Print table output
    result += '<table id="showtunneloutput" >'
    result += '<tbody>'
    result += '<tr><td>ID</td><td>Switch</td><td>Tunnel Source IP</td><td>Tunnel Interface</td><td>Tunnel Enabled</td><td>Tunnel State</td></tr>'
    
    for index_output,value_output in enumerate(sorteddict):
      result += '<tr>'
      result += '  <td>' + str(index_output + 1) + '</td>'
      result += '  <td>' 
      result += aliasDict.get(value_output.get('dpid', 'UNKNOWN'), value_output.get('dpid', 'UNKNOWN'))
      result += '</td>'
      result += '  <td>' + str(value_output.get('tunnelIPAddr','')) + '</td>'
      result += '  <td>' + value_output.get('tunnelIf','UNKNOWN') + '</td>'
      result += '  <td>' + str(value_output.get('tunnelActive','')) + '</td>'
      result += '  <td>' + value_output.get('tunnelState','') + '</td>'
      result += '</tr>'
    result += '</tbody>'
    result += '</table>'
    return result
    
      