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

import command
import fmtcnv
"""
CONTROLLER_NODE_FORMAT = {
    'controller-node' : {
        'field-orderings' : {
            'default'       : [
                             'Idx',
                             'alias',
                             'me',
                             'ha-role',
                             'change-date-time',
                             'change-description',
                             'status',
                             'uptime',
                             'errors'
                             #'domain-lookups-enabled',
                             #'logging-enabled',
                            ],
            'brief'   :      [
                             'Idx',
                             'alias',
                             'me',
                             'ha-role',
                             'change-date-time',
                             'change-description',
                             'status',
                             'uptime',
                             'errors',
                            ],
            'details' :    [
                             'Idx',
                             'alias',
                             'id',
                             'me',
                             'ha-role',
                             'change-date-time',
                             'change-description',
                             'status',
                             'uptime',
                             'errors',
                             'domain-lookups-enabled', 'domain-name-servers',
                             'domain-name', 'default-gateway',
                             'ntp-server', 'time-zone',
                             'logging-enabled', 'logging-server',
                            ],
            'without-vns' : [
                             'Idx',
                             'id',
                             'me',
                             'alias',
                             'ha-role',
                             'change-date-time',
                             'change-description',
                             'uptime',
                             'domain-lookups-enabled', 'domain-name-servers',
                             'domain-name', 'default-gateway',
                             'ntp-server', 'time-zone',
                             'logging-enabled', 'logging-server', 'logging-level',
                            ]
            },
        'fields': {
            'id'                     : {
                                          'verbose-name' : 'Controller ID',
                                       },
            'me'                     : {
                                          'verbose-name' : '@',
                                          'formatter' : fmtcnv.controller_node_me,
                                          'entry-formatter' : fmtcnv.controller_node_me_entry
                                       },
            'domain-lookups-enabled' : {
                                          'verbose-name' : 'DNS',
                                          'formatter' : fmtcnv.replace_boolean_with_enable_disable,
                                       },
            'domain-name-servers'    : {
                                          'verbose-name' : 'DNS Servers',
                                         'formatter' : fmtcnv.print_domain_name_servers,
                                         'entry-formatter' : fmtcnv.print_all_domain_name_servers,
                                       },
            'domain-name'            : {
                                       },
            'default-gateway'        : {
                                       },
            'ntp-server'             : {
                                       },
            'time-zone'              : {
                                       },
            'logging-enabled'        : { 'verbose-name' : 'Logging',
                                         'formatter' : fmtcnv.replace_boolean_with_enable_disable,
                                       },
            'logging-server'         : {
                                       },
            'logging-level'          : {
                                       },
            'ha-role'                : {
                                         'verbose-name' : 'HA Role',
                                       },
            'change-date-time'       : {
                                         'verbose-name' : 'HA Last Change Time',
                                         'formatter' : fmtcnv.print_time_since_utc_timestr,
                                       },
            'change-description'     : {
                                         'verbose-name' : 'HA Change Reason',
                                       },
            'uptime'                 : {
                                         'verbose-name' : 'Uptime',
                                         'formatter'    : fmtcnv.print_timesince_msec_since
                                       },
            },
        },
}

controller_node_show_actions = (
    {
        'proc'     : 'query-table',
    },
    {
        'proc'         : 'join-table',
        'obj-type'     : 'controller-alias',
        'key'          : 'controller',
        'join-field'   : 'id', # field in queried items
    },
    {
        'proc'       : 'join-rest',
        'url'        : 'system/controller',
        'rest-type'  : 'dict-with-id',
        'key'        : 'id',
        'join-field' : 'id',
        'add-field'  : 'me',
    },
    # need a description mechanism to join ha-role, which
    # needs to send urls to controller-node interfaces 
    # ie: the rest actions alwaus use the local controller
    # (uptime has the same issue)
    {
        'proc'       : 'display',
        'format'     : 'controller-node',
    },
),


CONTROLLER_NODE_SHOW_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'obj-type'     : 'controller-node',
    'mode'         : 'login',
    'command-type' : 'display-table',
    'all-help'     : 'Show controller node information',
    'short-help'   : 'Show controller nodes summaries',
    'doc'          : 'controller|show',
    'args'         : (
         {
            'token'  : 'controller-node',
            'action' : controller_node_show_actions,
         }
    ),
}
CONTROLLER_NODE_INTERFACE_FORMAT = {
    'controller-interface' : {
        'field-orderings': {
            'default' : ['Idx',
                         'controller',
                         'type',
                         'number',
                         'mode',
                         'ip',
                         'discovered-ip',
                         'netmask',
                         'mac',
                         'firewall']
            },
        'fields' : {
            'controller'    : {
                                'formatter' : fmtcnv.replace_controller_node_with_alias,
                              },
            'number'        : {
                              },
            'ifname'        : {
                                'formatter' : lambda i, data: '%s%s' % (data['type'],
                                                                        data['number'])
                              },
            'ip'            : {
                              },
            'discovered-ip' : { 'verbose-name' : 'Found IP',
                              },
            'mode'          : {
                              },
            'netmask'       : {
                              },
            'type'          : {
                              },
            'firewall'      : {
                                'verbose-name' : 'Firewall',
                              },
            }
        },
}


CONTROLLER_NODE_SUMMARY_FORMAT = {
        'controller-summary' : {
            'field-orderings' : {
                'default' : [
                              '# inter-switch links',
                              '# hosts',
                              '# attachment points',
                              '# IP Addresses',
                              '# VNSes',
                              '# Access Control Lists',
                              '# VNS Interfaces with ACL applied',
                              '# VNS Interfaces',
                ]
            },

            'fields' : {
                '# Access Control Lists'            : { 'verbose-name' : 'Access Control Lists'
                                                      },
                '# VNS Interfaces'                  : { 'verbose-name' : 'VNS Interfaces'
                                                      },
                '# hosts'                           : { 'verbose-name' : 'Hosts'
                                                      },
                '# VNSes'                           : { 'verbose-name' : 'VNSesx'
                                                      },
                '# attachment points'               : { 'verbose-name' : 'Attachment Points'
                                                      },
                '# inter-switch links'              : { 'verbose-name' : 'Links'
                                                      },
                '# IP Addresses'                    : { 'verbose-name' : 'Ip Addresses'
                                                      },
                '# VNS Interfaces with ACL applied' : { 'verbose-name' : 'VNS If with Acl Applied'
                                                      },
            },
        },
}


COTROLLER_NODE_SWITCHES_FORMAT = {
    'controller-switches' : {
        'field-orderings' : {
            'default'     : [
                'Idx', 'dpid',
            ],
        },
        'fields' : {
            'dpid' : { 'primary_key' : True,
                       'verbose-name': 'Connected Switches',
                       'formatter' : fmtcnv.print_switch_and_alias }
            }
        },

}


CONTROLLER_NODE_FIREWALL_RULE_FORMAT = {
    'firewall-rule' : {

       'field-orderings': {
         'default' : ['Idx',
                      'controller',
                      'type',
                      'number',
                      'src-ip',
                      'vrrp-ip',
                      'port',
                      'proto',
                      'rule']
        },
        'fields' : {

            'controller' : {
                            'formatter' : fmtcnv.replace_controller_node_with_alias,
                           },
            'interface'  : {
                           },
            'port'       : {
                           },
            'proto'      : {
                           },
            'rule'       : {
                             'verbose-name' : 'Firewall Rule'
                           },
            }
       },
}


def controller_completion_add_localhost(completions, prefix):
    if completions == None:
        completions = {}
    if not 'localhost ' in completions and 'localhost'.startswith(prefix) :
        completions['localhost '] = 'Select currently logged in controller'


command.add_completion('controller-add-localhost', controller_completion_add_localhost,
                      {'kwargs': { 'completions' : '$completions',
                                   'prefix'      : '$text',
                      }})

CONTROLLER_NODE_SHOW_ID_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'obj-type'     : 'controller-node',
    'mode'         : 'login',
    'short-help'   : 'Show controller-node associated details by name',
    'no-supported' : False,
    'action'       : controller_node_show_actions,
    'doc'          : 'controller|show-details',
    'args'         : (
        'controller-node',
        {
            'choices' : (
                {
                    'field'        : 'id',
                    'type'         : 'identifier',
                    'completion'   : [
                                      'complete-object-field',
                                      'controller-add-localhost',
                                     ],
                    'data-handler' : 'alias-to-value',
                    'action'       : controller_node_show_actions,
                    'doc'          : 'controller|show-id',
                },
                {
                    'token'        : 'all',
                    'doc'          : 'reserved|all',
                }
            ),

        },
        {
            'optional' : True,
            'choices' : (
                {
                   'token'      : 'interfaces',
                   'obj-type'   : 'controller-interface',
                   'action'     : 'display-table',
                   'short-help' : 'Show network interface information for controller node',
                   'doc'        : 'controller|show-interfaces',
                },
                {
                   'token'      : 'firewall',
                   'obj-type'   : 'firewall-rule',
                   'action'     : 'display-table',
                   'short-help' : 'Show firewall rules for controller node',
                   'doc'        : 'controller|show-firewall',
                },
                {
                   'token'      : 'summary',
                   'action'     : 'display-rest',
                   'url'        : 'controller/summary',
                   'rest-type'  : 'dict',
                   'format'     : 'controller-summary',
                   'short-help' : 'Show a summary of configuration for controller node',
                   'doc'        : 'controller|show-summary',
                },
                {
                   'token'      : 'switches',
                   'action'     : 'display-rest',
                   'url'        : 'controller/stats/switches',
                   'format'     : 'controller-switches',
                   'short-help' : 'Show switches connected to controller node',
                   'doc'        : 'controller|show-switches',
                },
                {
                   'token'      : 'alias',
                   'action'     : 'display-table',
                   'obj-type'   : 'controller-alias',
                   'short-help' : 'Show the alias for controller node',
                   'doc'        : 'controller|show-alias',
                },
            )
        },
        {
            'optional' : True,
            'field'    : 'detail',
            'type'     : 'enum',
            'values'   : ('details','brief'),
            'doc'      : 'controller|show-detail',
        }
    ),
}

CONTROLLER_NODE_SHOW_STATS_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'obj-type'     : 'controller-node',
    'mode'         : 'login',
    'short-help'   : 'Show statistics for a given controller node',
    'command-type' : 'display-table',
    'doc'          : 'show-stats',
    'args'         : (
        'controller-node',
        {
            'choices' : (
                {
                    'field'        : 'id',
                    'type'         : 'identifier',
                    'completion'   : [
                                       'complete-object-field',
                                       'controller-add-localhost',
                                     ],
                    'action'       : 'display-table',
                    'data-handler' : 'alias-to-value',
                    'doc'          : 'controller|id',
                },
                {
                    'field'        : 'id',
                    'type'         : 'enum',
                    'values'       : 'all',
                    'doc'          : 'reserved|all',
                }
            ),
       },
       {
          'field'     : 'stats',
          'type'      : 'enum',
          'values'    : 'stats',
          'obj-type'  : 'controller-stats',
          'action'    : 'legacy-cli',
       },
    ),
}

CONTROLLER_NODE_SHOW_OBJECT_STATS_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'obj-type'     : 'controller-node',
    'mode'         : 'login',
    'short-help'   : 'Show detailed controller-node related statistics',
    'command-type' : 'display-table',
    'args'         : (
        'controller-node',
        {
            'choices' : (
                {
                    'field'        : 'id',
                    'type'         : 'identifier',
                    'completion'   : [
                                       'complete-object-field',
                                       'controller-add-localhost',
                                     ],
                    'action'       : 'display-table',
                    'data-handler' : 'alias-to-value',
                },
                {
                    'field'        : 'id',
                    'type'         : 'enum',
                    'values'       : 'all',
                    'doc'          : 'reserved|all',
                }
            ),
       },
       {
          'field'     : 'stats',
          'type'      : 'enum',
          'values'    : 'stats',
          'obj-type'  : 'controller-stats',
          'action'    : 'legacy-cli',
       },
       {
          'field'     : 'stats-type',
          'type'      : 'enum',
          'values'    : (
                            'cpu-user',
                            'disk-root',
                            'mem-used',
                            'mem-free',
                            'cli-cpu',
                            'database-cpu',
                            'swap-used', 
                            'disk-boot', 
                            'statd-cpu', 
                            'cpu-system', 
                            'cpu-idle', 
                            'apache-cpu', 
                            'cpu-nice', 
                            'disk-log', 
                            'sdnplatform-cpu'
                        ),
          'doc'       : 'controller|show-stats-type',
       },
       {
          'field'     : 'start-time',
          'tag'       : 'start-time',
          'type'      : 'string',
          'short-help'   : 'Start time for displaying the stats',
          'optional'  : True,
          'doc'       : 'controller|show-stats-start-time',
       },
       {
          'field'     : 'end-time',
          'tag'       : 'end-time',
          'type'      : 'string',
          'short-help'   : 'End time for displaying the stats',
          'optional'  : True,
          'doc'       : 'controller|show-stats-end-time',
       },
       {
          'field'     : 'duration',
          'tag'       : 'duration',
          'type'      : 'string',
          'short-help'   : 'Duration from the start or end for displaying the stats',
          'optional'  : True,
          'doc'       : 'controller|show-stats-duration',
       },
       {
          'field'     : 'sample-interval',
          'tag'       : 'sample-interval',
          'type'      : 'integer',
          'short-help'   : 'Spacing between sampling windows',
          'optional'  : True,
          'doc'       : 'controller|show-stats-sample-interval',
       },
       {
          'field'     : 'sample-count',
          'tag'       : 'sample-count',
          'type'      : 'integer',
          'short-help'   : 'Number of samples to show',
          'optional'  : True,
          'doc'       : 'controller|show-stats-sample-count',
       },
       {
          'field'     : 'sample-window',
          'tag'       : 'sample-window',
          'type'      : 'integer',
          'optional'  : True,
          'short-help'   : 'Number of raw sample values to average around each down-sampled data point',
          'doc'       : 'controller|show-stats-sample-window',
       },
       {
          'field'     : 'data-format',
          'tag'       : 'data-format',
          'type'      : 'enum',
          'values'    : ('value', 'rate'),
          'short-help'   : 'Whether to display as a raw value or rate',
          'optional'  : True,
          'doc'       : 'controller|show-stats-data-format',
       },
       {
          'field'     : 'display',
          'tag'       : 'display',
          'type'      : 'enum',
          'values'    : ('latest-value',
                         'graph',
                         'table',
                        ),
          'short-help'   : 'Display the latest value, a graph, or a table',
          'optional'  : True,
          'doc'       : 'controller|show-stats-display',
       },
   ),
}

CONTROLLER_INTERFACES_SHOW_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'obj-type'     : 'controller-interface',
    'short-help'   : 'Show controller-node associated interfaces',
    'mode'         : 'config-controller',
    'command-type' : 'display-table',
    'scoped'       : True, # entries displayed filtered by pushed obj id
    'doc'          : 'controller|show-interfaces',
    'args'         : (
        'interfaces',
        {
           'field'     : 'type',
           'tag'       : 'type',
           'type'      : 'string',
           'optional'  : True,
           'doc'       : 'controller|show-interfaces-type',
        },
        {
           'field'    : 'number',
           'tag'      : 'number',
           'type'     : 'integer',
           'optional' : True,
           'doc'       : 'controller|show-interfaces-number',
        },
    )
}


CONTROLLER_NODE_SUBMODE_COMMAND_DESCRIPTION = {
    'name'                : 'controller-node',
    'obj-type'            : 'controller-node',
    'mode'                : 'config*',
    'short-help'          : 'Enter configuration submode for controller-nodes',
    'no-supported'        : False,          # don't allow removal of controller-nodes
    # 'create'            : False,         # don't allow creation of new controller-nodes
    'command-type'        : 'config-submode',
    'parent-field'        : None,
    'current-mode-obj-id' : None,
    'submode-name'        : 'config-controller',
    'doc'                 : 'controller|controller-node',
    'doc-example'         : 'controller|controller-node-example',
    'args'                : (
         {
             'field'           : 'id',
             'type'            : 'identifier',
             'completion'      : [
                                    'complete-alias-choice',
                                    'controller-add-localhost',
                                 ],
             'data-handler'    : 'alias-to-value',
             'optional-for-no' : False,
             'doc'             : 'controller|controller-node-name',
         }
    )
}


CONTROLLER_NODE_ALIAS_COMMAND_DESCRIPTION = {
    'name'         : 'controller-alias',
    'mode'         : 'config-controller',
    'short-help'   : 'Attach alias to controller',
    'command-type' : 'manage-alias',
    'obj-type'     : 'controller-alias',
    'scoped'       : True,
    'reserved'     : 'localhost',
    'doc'          : 'controller|alias',
    'args'         : (
        {
            'field'           : 'alias',
            'optional-for-no' : True,
            'completion'      : 'complete-object-field',
            'doc'             : 'controller|alias-name',
            'action'          : (
                {
                    'proc'    : 'create-alias',
                },
                {
                    'proc'    : 'prompt-update',
                },
            ),
            'no-action'       : (
                {
                    'proc'    : 'delete-alias',
                },
                {
                    'proc'    : 'prompt-update',
                },
            )
        }
    )
}


CONTROLLER_NODE_INTERFACE_COMMAND_DESCRIPTION = {
    'name'        : 'interface',
    'new-style'   : True,
    'mode'        : 'config-controller*',
    'short-help'  : 'Enter interface submode, configure controller interface',
    'command-type': 'config-submode',
    'submode-name': 'config-controller-if',
    'obj-type': 'controller-interface',
    'parent-field': 'controller',
    'doc'         : 'controller|interface',
    'args': (
        {
            'field'       : 'type',
            'help-name'   : 'interface-type',
            'type'        : 'enum',
            'values'      : ('Ethernet',),
            'syntax-help' : 'Enter the interface type (e.g. Ethernet)',
            'doc'         : 'controller|interface-type',
        },
        {
            'field'       : 'number',
            'help-name'   : 'interface-number',
            'base-type'   : 'integer',
            'range'       : (0,1000000),
            #'completion' : 'complete-object-field',
            'syntax-help' : 'Enter the interface number, a small non-negative number (e.g. 0 or 1)',
            'doc'         : 'controller|interface-number',
        }
    ),
    'no-action': ('begin-default-gateway-check', 'delete-objects', 'end-default-gateway-check'),
}

CONTROLLER_NODE_DOMAIN_NAME_SERVER_FORMAT = {
    'controller-domain-name-server' : {
        'field-orderings': {
            'default' : ['Idx', 'id', 'controller', 'priority', 'ip']
            },
        'fields' : {
            'ip'                : {
                                  },
            'controller'        : {
                                   'formatter' : fmtcnv.replace_controller_node_with_alias,
                                  },
            }
        },
}

CONTROLLER_IP_COMMAND_DESCRIPTION = {
    'name'       : 'ip',
    'mode'       : 'config-controller',
    'short-help' : 'Associate dns, default gateway with the controller node',
    'doc'        : 'controller|ip',
    'args'       : {
        'choices': (
            {
                'command-type': 'config',
                'obj-type': 'controller-node',
                'doc': 'controller|ip-domain',
                'args': (
                    'domain',
                    {
                        'choices': (
                            {
                                'field'     : 'domain-lookups-enabled',
                                'token'     : 'lookup',
                                'type'      : 'boolean',
                                'doc'       : 'controller|ip-domain-lookups',
                                'no-action' : {
                                    'proc'  : 'write-fields',
                                    'data'  : { 
                                                 'domain-lookups-enabled' : False,
                                              },
                                },
                            },
                            {
                                'field': 'domain-name',
                                'tag': 'name',
                                'type': 'domain-name',
                                'syntax-help': "Enter the network's domain name",
                                'default-for-no': 'xyz.com',
                                'doc': 'controller|ip-domain-name',
                            },
                        ),
                     },
                 ),
            },
            (
            {
                'command-type': 'config-object',
                'obj-type': 'controller-domain-name-server',
                'parent-field': 'controller',
                'doc': 'controller|ip-name-server',
                'args': (
                    {
                        'tag'             : 'name-server',
                        'field'           : 'ip',
                        'type'            : 'ip-address-not-mask',
                        'syntax-help'     : 'Enter the IP address of the domain name server',
                        'doc'             : 'controller|ip-name-server-value',
                    },
                ),
            },
            ),
            {
                'field': 'default-gateway',
                'command-type': 'config-with-default-gateway-check',
                'obj-type': 'controller-node',
                'doc': 'controller|ip-default-gateway',
                'args': {
                    'tag': 'default-gateway',
                    'field': 'default-gateway',
                    'type': 'ip-address-not-mask',
                    'syntax-help': 'Enter the IP address of the default gateway',
                    'default-for-no': '10.10.10.10',
                    'doc': 'controller|ip-default-gateway-value',
                },
            },
        ),
    },
}

INTERFACE_IP_COMMAND_DESCRIPTION = {
    'name'         : 'ip',
    'mode'         : 'config-controller-if',
    'command-type' : 'config-with-default-gateway-check',
    'obj-type'     : 'controller-interface',
    'short-help'   : 'Associate ip address with interface',
    'doc'          : 'controller|ip',
    'args'         : {
        'choices'  : (
            {
                'fields': ('ip', 'netmask'),
                'doc': 'controller|ip-address',
                'args': (
                    'address',
                    {
                        'choices': (
                            (
                                {
                                    'field': 'ip',
                                    'help-name': 'ip-address',
                                    'type': 'ip-address-not-mask',
                                    'syntax-help': 'Enter an IP or CIDR address',
                                    'default-for-no' : '10.10.10.10',
                                    'doc': 'controller|ip-address-value',
                                },
                                {
                                    'field': 'netmask',
                                    'type': 'netmask',
                                    'syntax-help': 'Enter a netmask',
                                    'default-for-no' : '0.0.0.0',
                                    'doc': 'controller|ip-address-netmask',
                                }
                            ),
                            {
                                'field': 'cidr',
                                'help-name': 'cidr-address',
                                'type': 'cidr-range',
                                'data-handler': 'split-cidr-data',
                                'completion-text': None,
                                'dest-ip': 'ip',
                                'dest-mask': 'mask',
                                'doc': 'controller|ip-address-cidr',
                            }
                        )
                    },
                ),
            },
            {
                'args': {
                    'tag': 'mode',
                    'field': 'mode',
                    'type': 'enum',
                    'values': ('dhcp', 'static'),
                    'syntax-help': 'Enter the IP address configuration mode, either "dhcp" or "static"',
                    'default-for-no': 'static',
                    'doc': 'controller|ip-mode',
                },
            },
        ),
     }
}

NTP_COMMAND_DESCRIPTION = {
    'name'        : 'ntp',
    'mode'        : 'config-controller',
    'short-help'  : 'Configure ntp for controller-node',
    'command-type': 'config',
    'obj-type'    : 'controller-node',
    'doc'         : 'controller|ntp',
    'doc-example' : 'controller|ntp-example',
    'args': {
        'tag'         : 'server',
        'field'       : 'ntp-server',
        'type'        : 'ip-address-or-domain-name',
        'syntax-help' : 'Enter the IP address or domain name of the NTP server',
        'short-help'  : 'Configure the NTP server name',
        'default-for-no': 'xyz.com',
        'completion' : 'complete-object-field',
        'doc'        : 'controller|ntp-server',
    }
}

LOGGING_COMMAND_DESCRIPTION = {
    'name'        : 'logging',
    'short-help'  : 'Configure logging (syslog) for controller-node',
    'mode'        : 'config-controller',
    'command-type': 'config',
    'obj-type'    : 'controller-node',
    'doc'         : 'controller|logging',
    'doc-example' : 'controller|logging-example',
    'args': {
        'choices': (
            {
                'field'      : 'logging-enabled',
                'token'      : 'on',
                'type'       : 'boolean',
                'short-help' : 'Enable remote logging',
                'doc'        : 'controller|logging-enable',
            },
            (
                {
                    'token'           : 'server',
                    'short-help'      : 'Set the remote syslog server name',
                    'doc'             : 'controller|logging-server',
                },
                {
                    'field'           : 'logging-server',
                    'help-name'       : 'server-ip-or-domain',
                    'type'            : 'ip-address-or-domain-name',
                    'syntax-help'     : 'Enter the IP address or domain name of the syslog server',
                    'completion'      : 'complete-object-field',
                    'match-for-no'    : 'logging-server',
                    'no-action'       : 'reset-fields',
                },
                {
                    'field'      : 'logging-level',
                    'tag'        : 'level',
                    'short-help' : 'Set the logging level for remote syslog',
                    'type'       : 'enum',
                    'optional'   : True,
                    'values'     : ('emerg', 'alert', 'crit', 'err',
                                    'warning', 'notice', 'info', 'debug',
                                    '0', '1', '2', '3', '4', '5', '6', '7'
                                   ),
                    'doc'        : 'controller|logging-level',
                 }
            ),
        )
    }
}

FIREWALL_COMMAND_DESCRIPTION = {
    'name'         : 'firewall',
    'short-help'   : 'Configure firewall rule for controller-node',
    'command-type' : 'config-object',
    'mode'         : 'config-controller-if',
    'obj-type'     : 'firewall-rule',
    'doc'          : 'controller|firewall',
    'parent-field' : 'interface',
    'args': (
        # This arg/token is really just syntactic sugar for the command.
        # There's no 'deny' value, because things are denied by default.
        # Instead you just use a 'no' command to delete the firewall rule.
        {
            'token'      : 'allow',
            'short-help' : 'Allow a given set of traffic',
            'doc'        : 'controller|firewall-allow',
        },
        {
            'field'           : 'src-ip',
            'tag'             : 'from',
            'type'            : 'ip-address',
            'optional'        : True,
            'optional-for-no' : True,
            'match-for-no'    : True,
            'doc'             : 'controller|firewall-src-ip',
        },
        {
            'field'           : 'vrrp-ip',
            'tag'             : 'local-ip',
            'type'            : 'ip-address',
            'optional'        : True,
            'optional-for-no' : True,
            'match-for-no'    : True,
            'doc'             : 'controller|firewall-local-ip',
        },
        {
            'choices'         : (
                {
                    'field'      : 'port',
                    'type'       : 'enum',
                    'short-help' : 'Configure rule for a specific service port',
                    'doc'        : 'controller|firewall-port',
                    'data'       : { 'proto' : 'tcp' },
                    'values': {
                        'openflow' : 6633,
                        'web'      : 80,
                        'ssl'      : 443,
                        'ssh'      : 22
                    },
                    'match-for-no'    : True,
                    'doc'             : 'controller|firewall-+',
                },
                (
                    {
                        'field'        : 'proto',
                        'type'         : 'enum',
                        'short-help'   : 'Configure rule for protocol (TCP, UDP)',
                        'values'       : ('udp', 'tcp'),
                        'doc'          : 'controller|firewall-+',
                        'match-for-no' : True,
                    },
                    {
                        'optional-for-no' : False,
                        'choices': (
                            {
                                'field'      : 'port',
                                'type'       : 'enum',
                                'short-help' : 'Configure rule for a specific service port',
                                'doc': 'controller|firewall-port',
                                'values': {
                                    'openflow' : 6633,
                                    'web'      : 80,
                                    'ssl'      : 443,
                                    'ssh'      : 22
                                },
                                'match-for-no'    : True,
                                'doc'             : 'controller|firewall-+',
                            },
                            {
                                'field'           : 'port',
                                'base-type'       : 'integer',
                                'range'           : (0, 65535),
                                'completion-text' : '0-65535',
                                'doc'             : 'controller|firewall-port',
                                'match-for-no'    : True,
                            },
                        )
                    },
                ),
                {
                    'field'        : 'proto',
                    'type'         : 'enum',
                    'short-help'   : 'Configure rule for protocol (TCP, UDP)',
                    'values'       : 'vrrp',
                    'doc'          : 'controller|firewall-+',
                    'match-for-no' : True,
                },
            ),
        },
    )
}

MONTH_NAMES =  ('January', 'February', 'March', 'April', 'May', 'June',
                'July', 'August', 'September', 'October', 'November', 'December')
                           


CLOCK_TIMEZONE_COMMAND_DESCRIPTION = {
    'name'         : 'clock',
    'mode'         : 'config-controller',
    'short-help'   : 'Configure time zone',
    'doc'          : 'controller|clock-timezone',
    'doc-example'  : 'controller|clock-timezone-example',
    'command-type' : 'config',
    'obj-type'     : 'controller-node',
    'args'         : {
        'tag'            : 'timezone',
        'field'          : 'time-zone',
        'type'           : 'string',
        'default-for-no' : 'UTC',
        'completion'     : 'time-zone-completion',
        'validation'     : 'time-zone-validation',
        'syntax-help'    : 'Enter the time zone',
        'short-help'     :  'Configure the time zone',
    }
}

CLOCK_SET_COMMAND_DESCRIPTION = {
    'name'         : 'clock',
    'mode'         : 'config-controller',
    'no-supported' : False,
    'short-help'   : 'Set clock',
    'doc'          : 'controller|clock-set',
    'doc-example'  : 'controller|clock-set-example',
    'command-type' : 'realtime',
    'action'       : 'set-clock',
    'args'         : (
        {
            'token'       : 'set',
            'short-help'  : 'Set the current time and date'
        },
        {
            'field'       : 'time',
            'base-type'   : 'string',
            'pattern'     : '^[0-9]{1,2}:[0-9]{1,2}:[0-9]{2}$',
            'syntax-help' : 'Enter the time (HH:MM:SS)'
        },
        {
            'field'       : 'day-of-month',
            'base-type'   : 'integer',
            'range'       : (1,31),
            'syntax-help' : 'Enter the day of the month (1-31)'
        },
        {
            'field'       : 'month',
            'type'        : 'enum',
            'values'      : MONTH_NAMES,
            'syntax-help' : 'Enter the month (e.g. January, March)'
        },
        {
            'field'       : 'year',
            'base-type'   : 'integer',
            'range'       : (0,10000),
            'syntax-help' : 'Enter the year'
        },
    )
}
"""
