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
"""
HOST_SUBMODE_COMMAND_DESCRIPTION = {
    'name'                : 'host',
    'obj-type'            : 'host-config',
    'mode'                : 'config*',
    'command-type'        : 'config-submode',
    'parent-field'        : None,
    'current-mode-obj-id' : None,
    'submode-name'        : 'config-host',
    'short-help'          : 'Host submode, configure host details',
    'doc'                 : 'host|host',
    'doc-example'         : 'host|host-example',
    'data'                : {
                                'address-space' : 'default',
                                'vlan'          : '',
                            },
    'args'                : (
         {
            'field'           : 'address-space',
            'tag'             : 'address-space',
            'type'            : 'string',
            'optional'        : True,
            'optional-for-no' : True,
            'completion'      : 'complete-from-another',
            'other'           : 'address-space',
            'doc'             : 'host|host-address-space',
         },
         {
            'field'           : 'vlan',
            'tag'             : 'vlan',
            'type'            : 'integer',
            'range'           : (0,4095),
            'optional'        : True,
            'optional-for-no' : True,
            'doc'             : 'host|host-vlan',
         },
         {
             'field'          : 'mac',
             'type'           : 'host',
             'completion'     : [ 'complete-alias-choice',
                                ],
             'other'          : 'host',
             'scoped'         : True,
             'data-handler'   : 'alias-to-value',
         }
    )
}
"""

host_show_mac_action = (
    {
        'proc'       : 'query-table',
        'obj-type'   : 'host',
    },
    {   # add any associated tags
        'proc'       : 'join-table',
        'obj-type'   : 'tag-mapping',
        'key'        : 'mac',
        'key-value'  : 'tag', # causes list creation for multiple matches
        'join-field' : 'mac',
        'add-field'  : 'tag',
    },
    {
        'proc'       : 'display',
        'format'     : 'host',
    },
)


HOST_SHOW_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'short-help'   : 'Show host summaries',
    'doc'          : 'host|show',
    'doc-example'  : 'host|show-example',
    'no-supported' : False,
    'command-type' : 'display-table',
    'args' : (
         {
            'token'  : 'host',
            'action' : host_show_mac_action,
         }
    )
}

HOST_SHOW_MAC_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'short-help'   : 'Show host details based on query',
    'doc'          : 'host|show-host',
    'doc-example'  : 'host|show-host-example',
    'no-supported' : False,
    'command-type' : 'display-table',
    'obj-type'     : 'host',
    'action'       : host_show_mac_action,
    'args' : (
        'host',
        {
            'choices' : (
                {
                    'field'        : 'mac',
                    'type'         : 'host',
                    'short-help'   : 'Show the hosts with the given MAC or alias',
                    'help-name'    : 'host mac or alias',
                    'completion'   : 'complete-alias-choice',
                    'data-handler' : 'alias-to-value',
                    'doc'          : 'host|show-host-mac',
                },
                {
                    'field'        : 'address-space',
                    'short-help'   : 'Show the hosts with the given IPv4 address',
                    'tag'          : 'address-space',
                    'type'         : 'identifier',
                    'help-name'    : 'address space',
                    'completion'   : 'complete-object-field',
                    'doc'          : 'host|show-host-address-space',
                },
                {
                    'field'        : 'ipv4',
                    'short-help'   : 'Show the hosts with the given IPv4 address',
                    'tag'          : 'ip-address',
                    'type'         : 'ip-address',
                    'help-name'    : 'ip address',
                    'completion'   : 'complete-object-field',
                    'doc'          : 'host|show-host-ipv4',
                },
                {
                    'field'        : 'dpid',
                    'short-help'   : 'Show the hosts attached to the given switch',
                    'tag'          : 'switch',
                    'type'         : 'dpid',
                    'help-name'    : 'switch dpid or alias',
                    'completion'   : 'complete-object-field',
                    #'completion'   : 'complete-alias-choice',
                    'data-handler' : 'alias-to-value',
                    'doc'          : 'host|show-host-switch',
                },
                {
                   'field'     : 'host',
                   'short-help': 'Show all hosts',
                   'type'      : 'enum',
                   'values'    : 'all',
                   'doc'       : 'host|show-host-all',
                },
            )
        },
        {
            'optional' : True,
            'choices' : (
                (
                    {
                        'token'      : 'by',
                        'short-help' : 'Sort displayed hosts',
                        'doc'        : 'reserved|by',
                    },
                    {
                        'token'    : 'last-seen',
                        'short-help': 'Sort by the last seen time',
                        'sort'     : '-last-seen',
                        'action'   : 'display-table',
                        'doc'      : 'host|show-host-by-last-seen',
                    },
                ),
            )
        },
        {
            'optional'   : True,
            'field'      : 'detail',
            'type'       : 'enum',
            'short-help' : 'Display either detailed or brief information',
            'values'     : ('details','brief'),
            'doc'        : 'format|+',
        },
    )
}


HOST_SHOW_MAC_ITEMS_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'short-help'   : 'Show various host related details by query',
    'doc'          : 'host|show-host-items',
    'doc-example'  : 'host|show-host-items-example',
    'no-supported' : False,
    'command-type' : 'display-table',
    'obj-type'     : 'host',
    'args' : (
        'host',
        {
            'choices' : (
                {
                    'field'        : 'mac',
                    'type'         : 'host',
                    'short-help'   : 'Show the hosts with the given MAC or alias',
                    'help-name'    : 'host mac or alias',
                    'completion'   : 'complete-alias-choice',
                    'data-handler' : 'alias-to-value',
                    'doc'          : 'host|show-host-mac',
                },
                {
                    'field'        : 'ipv4',
                    'tag'          : 'ip-address',
                    'short-help'   : 'Show the hosts with the given IPv4 address',
                    'type'         : 'ip-address',
                    'help-name'    : 'ip address',
                    'completion'   : 'complete-object-field',
                    'doc'          : 'host|show-host-ipv4',
                },
                {
                    'field'        : 'dpid',
                    'short-help'   : 'Show the hosts attached to the given switch',
                    'tag'          : 'switch',
                    'type'         : 'dpid',
                    'help-name'    : 'switch dpid or alias',
                    'completion'   : 'complete-object-field',
                    #'completion'   : 'complete-alias-choice',
                    'data-handler' : 'alias-to-value',
                    'doc'          : 'host|show-host-switch',
                },
                {
                   'field'     : 'host',
                   'short-help': 'Show all hosts',
                   'type'      : 'enum',
                   'values'    : 'all',
                   'doc'       : 'host|show-host-all',
                },
            )
        },
        {
            'choices' : (
                 (
                     {
                        'field'      : 'attachment-point',
                        'type'       : 'enum',
                        'values'     : 'attachment-point',
                        'obj-type'   : 'host-attachment-point',
                        'action'     : 'display-table',
                        'short-help' : 'Show host attachment points',
                        'doc'        : 'host|show-host-item-attachment-point',
                    },
                    {
                        'optional' : True,
                        'choices' : (
                            (
                                {
                                    'token'      : 'by',
                                    'short-help' : 'Sort displayed hosts',
                                    'doc'        : 'reserved|by',
                                },
                                {
                                    'choices' : (
                                        {
                                            'token'      : 'host-last-seen',
                                            'sort'       : 'host,-last-seen',
                                            'obj-type'   : 'host-attachment-point',
                                            'action'     : 'display-table',
                                            'short-help' : 'Sort by the last seen time for the host',
                                            'doc'        : 'host|show-host-by-host-last-seen',
                                        },
                                        {
                                            'token'      : 'last-seen',
                                            'sort'       : '-last-seen',
                                            'obj-type'   : 'host-attachment-point',
                                            'action'     : 'display-table',
                                            'short-help' : 'Sort by the last seen time for the attachment point',
                                            'doc'        : 'host|show-host-by-last-seen',
                                        },
                                    ),
                                },
                            ),
                        ),
                    },
                    {
                        'field'      : 'detail',
                        'type'       : 'enum',
                        'values'     : ('details', 'brief'),
                        'optional'   : True,
                        'short-help' : 'Display either detailed or brief information',
                        'doc'        : 'format|+',
                    }
                 ),
                 (
                    {
                       'field'    : 'network-address',
                       'type'     : 'enum',
                       'values'   : 'ip-address',
                       'obj-type' : 'host-network-address',
                       'action'   : 'display-table',
                       'doc'      : 'host|show-host-item-network-address',
                    },
                    {
                        'optional' : True,
                        'choices' : (
                            (
                                {'token': 'by',
                                 'short-help': 'Sort displayed hosts'
                                 },
                                {
                                    
                                    'choices' : (
                                       {
                                           'token'      : 'host-last-seen',
                                           'sort'       : 'host,-last-seen',
                                           'short-help' : 'Sort by the last seen time for the host',
                                            'obj-type'  : 'host-network-address',
                                            'action'    : 'display-table',
                                            'doc'       : 'host|show-host-by-host-last-seen',
                                       },
                                       {
                                           'token'      : 'last-seen',
                                           'sort'       : '-last-seen',
                                           'short-help' : 'Sort by the last seen time for the network address',
                                           'obj-type'   : 'host-network-address',
                                           'action'     : 'display-table',
                                           'doc'        : 'host|show-host-by-last-seen',
                                       }
                                    )
                                },
                            ),
                        ),
                    },
                    {
                        'field'      : 'detail',
                        'type'       : 'enum',
                        'values'     : ('details', 'brief'),
                        'optional'   : True,
                        'short-help' : 'Display either detailed or brief information',
                        'doc'        : 'format|+'
                    }
                 ),
                 {
                    'field'      : 'alias',
                    'type'       : 'enum',
                    'values'     : 'alias',
                    'obj-type'   : 'host-alias',
                    'action'     : 'display-table',
                    'short-help' : 'Display host alias mappings',
                    'doc'        : 'host|show-host-item-alias',
                 },
            ),
        },
    )
}

"""
HOST_HOST_ALIAS_COMMAND_DESCRIPTION = {
    'name'         : 'host-alias',
    'mode'         : 'config-host',
    'short-help'   : 'Attach alias to host',
    'doc'          : 'host|host-alias',
    'doc-example'  : 'host|host-alias-example',
    'command-type' : 'manage-alias',
    'obj-type'     : 'host-alias',
    'scoped'       : True,
    'reserved'     : [ 'switch', 'ip-address' ], 
    'args'         : (
        {
            'field'           : 'id',
            'base-type'       : 'identifier',
            'optional-for-no' : False,
            'completion'      : 'complete-object-field',
        }
    )
}


HOST_SECURITY_POLICY_BIND_IP_ADDRESS_COMMAND_DESCRIPTION = {
    'name'         : 'security',
    'mode'         : 'config-host',
    'short-help'   : 'Configure security policies for host',
    'doc'          : 'host|security',
    'doc-example'  : 'host|security-example',
    'command-type' : 'config-object',
    'parent-field' : 'host',
    'args'         : (
        {
            'token' : 'policy',
            'doc'   : 'host|security-policy',
        },
        {
            'token' : 'bind',
            'doc'   : 'host|security-bind',
        },
        {
            'choices' : (
                {
                    'field'           : 'ip-address',
                    'tag'             : 'ip-address',
                    'base-type'       : 'ip-address',
                    'optional-for-no' : False,
                    # could possibly complete all ip-addresses
                    'completion'      : 'complete-object-field',
                    'obj-type'        : 'host-security-ip-address',
                    'action'          : 'write-object',
                    'no-action'       : 'delete-objects',
                    'short-help'      : 'restrict host access to ip-address',
                    'doc'             : 'host|security-ip-address',
                },
                (
                    {
                        'token'           : 'attachment-point',
                        'short-help'      : 'restrict host access to attachment point',
                        'doc'             : 'host|security-attachment-point',
                    },
                    {
                        'choices' :       (
                            {
                                'token'           : 'all'
                            },
                            {
                                'field'           : 'dpid',
                                'type'            : 'dpid',
                                'completion'      : [
                                                      'complete-object-field',
                                                      'complete-alias-choice',
                                                    ],
                                'obj-type'        : 'switches',
                                'other'           : 'switches|dpid',
                                'help-name'       : 'switch dpid or alias',
                                'data-handler'    : 'alias-to-value',
                                'optional-for-no' : False,
                                'short-help'      : 'identify switch for attachment point',
                                'doc'             : 'host|security-attachment-point-switch',
                            },
                        )
                    },
                    {
                        'field'           : 'if-name-regex',
                        'optional-for-no' : False,
                        'syntax-help'     : 'Regular expression match for interfaces',
                        'action'          : 'write-object',
                        'no-action'       : 'delete-objects',
                        'obj-type'        : 'host-security-attachment-point',
                        'completion'      : [
                                              'complete-object-field',
                                              'complete-from-another',
                                            ],
                        'other'           : 'interfaces|portName',
                        'scoped'          : 'switch',
                        'short-help'      : 'identify interface for attachment point',
                        'doc'             : 'host|security-attachment-point-interface',
                    },
                ),
            ),
        },
    ),
}
"""

#
# FORMATS
#

import fmtcnv


HOST_FORMAT = {
    'host': {
        'field-orderings' : {
            'default' : [ 'Idx', 'id',  
                          'ips', 'attachment-points' ],
            'details' : [ 'Idx', 'id',  
                          'vendor', 'ips', 'attachment-points'
                          ],
            'brief'   : [ 'Idx', 'id', 'ips'],
            },


        'fields': {
            'id'                 : {
                                      'verbose-name': 'MAC Address',
                                      'formatter' : fmtcnv.print_host_and_alias,
                                    },
            'mac'                : {
                                      'verbose-name': 'MAC Address',
                                      'formatter' : fmtcnv.print_host_and_alias,
                                    },
            'address-space'       : {
                                      'verbose-name' : 'Address Space',
                                    },
            'vendor'              : {
                                      'formatter' : fmtcnv.sanitize_unicode,
                                    },
            'vlan'                : {
                                      'verbose-name': 'VLAN',
                                      'formatter' : fmtcnv.convert_to_string,
                                    },
            'ips'                 : {
                                      'verbose-name' : 'IP Address',
                                      'formatter' : fmtcnv.print_ip_addresses,
                                      'entry-formatter' : fmtcnv.print_all_ip_addresses,
                                    },
            'attachment-points'   : {
                                      'verbose-name' : 'Switch/OF Port (Physical Port)',
                                      'formatter' : fmtcnv.print_host_attachment_point,
                                      'entry-formatter' : fmtcnv.print_all_host_attachment_points,
                                    },
            'tag'                 : {
                                      'formatter' : fmtcnv.print_host_tags,
                                      'entry-formatter' : fmtcnv.print_all_host_tags,
                                    },
            'dhcp-client-name'    : {
                                      'verbose-name' : 'DHCP Client Name',
                                    },
            'last-seen'           : {
                                      'verbose-name': 'Last Seen',
                                      'formatter' : fmtcnv.print_time_since_utc,
                                    },
             },
        },
}


HOST_ATTACHMENT_POINT_FORMAT = {
    'host-attachment-point' : {
        'field-orderings' : {
            'default' : [ 'Idx', 'mac', 'vlan', 'address-space', 'switch', 'ingress-port', 'status', ],
            'details' : [ 'Idx', 'mac', 'vlan', 'address-space', 'switch', 'ingress-port', 'status', 'last-seen'],
            },

         'fields': {
            'mac'          : {
                              'verbose-name' : 'MAC Address',
                              'formatter' : fmtcnv.print_host_and_alias,
                             },
            'vlan'         : {
                                'verbose-name': 'VLAN',
                                'formatter' : fmtcnv.convert_to_string,
                             },

            'address-space' : {
                                'verbose-name' : 'Address Space',
                              },
            'switch'       : {
                              'verbose-name' : 'Switch ID',
                              'formatter' : fmtcnv.print_switch_and_alias
                             },
            'ingress-port' : {
                              'verbose-name': 'Port',
                              'formatter' : fmtcnv.decode_openflow_port,
                             },
            'status'       : {
                               'verbose-name': 'Error Status (if any)'
                             },
            'last-seen'    : {
                               'verbose-name': 'Last Seen',
                               'formatter' : fmtcnv.timestamp_to_local_timestr,
                             },
             },
         },
}


HOST_NETWORK_ADDRESS_FORMAT = {
    'host-network-address': {
        'field-orderings' : {
            'default' : [ 'Idx', 'mac', 'address-space', 'vlan', 'ip-address',  ],
            'details' : [ 'Idx', 'mac', 'address-space', 'vlan', 'ip-address', 'last-seen'  ],
            },
        'fields': {
            'mac'        : {
                             'verbose-name': 'MAC Address',
                           },
            'vlan'       : {
                             'verbose-name': 'VLAN',
                           },
            'address-space' : {
                                'verbose-name' : 'Address Space',
                              },
            'id'         : {
                           },
            'ip-address':  {
                             'verbose-name': 'IP Address',
                          },
            'last-seen' : {
                            'verbose-name': 'Last Seen',
                            'formatter' : fmtcnv.timestamp_to_local_timestr,
                          },
            },
        },
}


HOST_ALIAS_FORMAT = {
    'host-alias' : {
        'field-orderings' : {
            'default'     : [ 'Idx', 'id', 'address-space', 'vlan', 'mac' ]
            },
        },
}

"""
HOST_CONFIG_FORMAT = {
    'host-config' : {
        'field-orderings' : {
            'default' : [ 'Idx', 'mac', 'vlan', 'vendor', 'ips',
                          'attachment-points', 'tag', 'last-seen' ],
            'brief'   : [ 'Idx', 'mac', 'vlan', 'ips', 'last-seen'],
            },
        'fields' : {
            'mac'                : {
                                      'verbose-name': 'MAC Address',
                                      'formatter' : fmtcnv.print_host_and_alias,
                                    },
            'vendor'              : {
                                      'formatter' : fmtcnv.sanitize_unicode,
                                    },
            'vlan'                : {
                                      'verbose-name': 'VLAN',
                                      'formatter' : fmtcnv.convert_to_string,
                                    },
            'ips'                 : {
                                      'verbose-name' : 'IP Address',
                                      'formatter' : fmtcnv.print_ip_addresses,
                                      'entry-formatter' : fmtcnv.print_all_ip_addresses,
                                    },
            'attachment-points'   : {
                                      'verbose-name' : 'Switch/OF Port (Physical Port)',
                                      'formatter' : fmtcnv.print_host_attachment_point,
                                      'entry-formatter' : fmtcnv.print_all_host_attachment_points,
                                    },
            'tag'                 : {
                                      'formatter' : fmtcnv.print_host_tags,
                                      'entry-formatter' : fmtcnv.print_all_host_tags,
                                    },
            'host-alias'          : {
                                    },
            'last-seen'           : {
                                      'verbose-name': 'Last Seen',
                                      'formatter' : fmtcnv.print_time_since_utc,
                                    },
        },
    },
}
"""