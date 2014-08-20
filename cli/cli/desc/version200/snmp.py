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

# (c) in 2012 by Big Switch Networks - All rights reserved
#
# snmp-server commands
#

import command
from midw import local_interfaces_firewall_open

def snmp_validate_firewall(data):
    intfs = local_interfaces_firewall_open( [ 'udp', 'tcp' ], 161)
    if len(intfs) == 0:
        # by issueing a warning, running-config doesn't display the
        # error during replay.
        command.sdnsh.warning('SNMP server port not open on any controller-node '
                              'interface. Use firewall rule configuration to open '
                              'SNMP UDP port 161')

    

command.add_action('snmp-validate-firewall', snmp_validate_firewall,
                    {'kwargs': { 'data'   : '$data',
                               }})

"""
#
# --------------------------------------------------------------------------------
# SNMP command descriptions 
#
#  [no] snmp-server enable
#  [no] snmp-server community <string>
#  [no] snmp-server location <string>
#  [no] snmp-server contact <string>
#
SNMP_SERVER_COMMAND_DESCRIPTION = {
    'name'         : 'snmp-server',
    'mode'         : 'config*',
    'short-help'   : 'SMNP configuration, enable server, configure parameters',
    'doc'          : 'snmp|snmp-server',
    'doc-example'  : 'snmp|snmp-server-example',
    'command-type' : 'update-config',
    'obj-type'     : 'snmp-server-config',
    'data'         : { 'id' : 'snmp' },
    'args'         : (
        {
            'choices' : (
                {
                    'token'      : 'enable',
                    'short-help' : 'Enable SNMP',
                    'doc'        : 'snmp|snmp-server-enable',
                    'action'     : (
                                       {
                                          'proc' : 'update-config',
                                          'data' : {
                                                      'id'            : 'snmp',
                                                      'server-enable' : True,
                                                   },
                                       },
                                       {
                                          'proc' : 'snmp-validate-firewall'
                                       },
                    ),
                    'no-action'  : {
                                      'proc' : 'update-config',
                                      'data' : {
                                                  'id'            : 'snmp',
                                                  'server-enable' : False,
                                               },
                                   },
                },
                (
                    {
                        'token'           : 'community',
                        'short-help'      : 'Set community string and access privs',
                        'data'            : { 'community' : None },
                        'doc'             : 'snmp|snmp-server-community',
                    },
                    {
                        'token'           : 'ro',
                        'short-help'      : 'Read-only access with this community string',
                        'optional-for-no' : True,
                    },
                    {
                        'field'           : 'community',
                        'type'            : 'string',
                        'optional-for-no' : True,
                        'syntax-help'     : 'Value for the SNMP commuity string',
                    },
                ),
                (
                    {
                        'token'           : 'location',
                        'short-help'      : 'Text for mib object sysLocation',
                        'data'            : { 'location' : None },
                        'doc'             : 'snmp|snmp-server-location',
                    },
                    {
                        'field'           : 'location',
                        'type'            : 'string',
                        'optional-for-no' : True,
                        'syntax-help'     : 'Value for the SNMP location string',
                    },
                ),
                (
                    {
                        'token'           : 'contact',
                        'short-help'      : 'Text for mib object sysContact',
                        'data'            : { 'contact' : None },
                        'doc'             : 'snmp|snmp-server-contact',
                    },
                    {
                        'field'           : 'contact',
                        'type'            : 'string',
                        'optional-for-no' : True,
                        'syntax-help'     : 'Value for the SNMP contact string',
                    },
                ),
            ),
        },
    )
}
"""

def snmp_firewall_interfaces(data):
    """
    There is currently only one row of data in the query,
    add to that a 'interfaces' entity, which lists the interfaces
    which have the snmp port open
    """
    if not hasattr(command, 'query_result'):
        return
    if command.query_result == None:
        return

    intfs = local_interfaces_firewall_open( [ 'udp', 'tcp' ], 161)

    intfs_text = ', '.join([x['discovered-ip'] if x['discovered-ip'] != ''
                            else x['ip'] for x in intfs])

    for q in command.query_result:
        q['interfaces'] = intfs_text


command.add_action('snmp-firewall-interfaces', snmp_firewall_interfaces,
                    {'kwargs': { 'data'   : '$data',
                               }})

"""
SNMP_SERVER_SHOW_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'short-help'   : 'Show configured snmp details',
    'doc'          : 'snmp|show',
    'doc-example'  : 'snmp|show-example',
    'command-type' : 'display-table',
    'obj-type'     : 'snmp-server-config',
    'short-help'   : 'Show SNMP configuration',
    'format'       : 'snmp-config-summary',
    'args' : (
        'snmp',
    ),
    'action'       : (
        {
            'proc' : 'query-table',
        },
        {
            'proc'  : 'snmp-firewall-interfaces'
        },
        {
            'proc'   : 'display-table',
        },
    ),
}

import fmtcnv

SNMP_CONFIG_SUMMARY_FORMAT = {
    'snmp-config-summary' : {
        'field-orderings' : {
            'default'     : [
                                'server-enable',
                                'community',
                                'location',
                                'contact',
                                'interfaces',
                            ],
        },
        'fields' : {
            'server-enable'           : {
                                          'verbose-name' : 'SNMP server status',
                                          'formatter'    : fmtcnv.replace_boolean_with_enable_disable,
                                        },
            'community'               : { 'verbose-name' : 'Community string' },
            'location'                : { 'verbose-name' : 'System location' },
            'contact'                 : { 'verbose-name' : 'System contact' },
        },
    }
}
"""