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
import json
import fmtcnv

"""
SWITCH_SUBMODE_COMMAND_DESCRIPTION = {
    'name'          : 'switch',
    'short-help'    : 'Enter switch submode, configure switch details',
    'mode'          : 'config*',
    'obj-type'      : 'switch-config',
    'command-type'  : 'config-submode',
    'parent-field'  : None,
    'submode-name'  : 'config-switch',
    'doc'           : 'switch|switch',
    'doc-example'   : 'switch|switch-example',
    'args' : (
        {
            'field'        : 'dpid',
            'type'         : 'dpid',
            'other'        : 'switches|dpid',
            'completion'   : [
                              'complete-alias-choice',
                              'complete-from-another',
                             ],
            'data-handler' : 'alias-to-value',
            'syntax-help'  : 'Configure a new switch with dpid',
        }
    )
}
"""
#
# ------------------------------------------------------------------------------
# show switch
#

show_switch_pipeline = (
    {
        'proc'     : 'query-table',
        'obj-type' : 'switches', # synthetic, supported by rest_to_model
        'key'      : 'dpid',
    },
    {
        'proc'       : 'join-table',
        'obj-type'   : 'switch-alias',
        'key'        : 'switch',
        'join-field' : '@',
        'add-field'  : 'switch-alias|id',
    },
    {
        'proc'       : 'join-rest',
        'url'        : 'tunnel-manager/all',
        'rest-type'  : 'dict-of-dict-of-switch',
        'key'        : 'switch',
        'join-field' : '@',
    },
    {
        'proc'     : 'display',
        'format'   : 'switch',
    },
)

SWITCH_SHOW_COMMAND_DESCRIPTION = {
    'name'                : 'show',
    'mode'                : 'login',
    'command-type'        : 'display-table',
    'all-help'            : 'Show switch information',
    'short-help'          : 'Show switch summary',
    'obj-type'            : 'switches',
    'doc'                 : 'switch|show',
    'doc-example'         : 'switch|show-example',
    'args' : (
        {
            'token'  : 'switch',
            'action' : show_switch_pipeline,
            'doc'    : 'switch|show',
        },
    )
}

SWITCH_SHOW_WITH_DPID_COMMAND_DESCRIPTION = {
    'name'                : 'show',
    'mode'                : 'login',
    'short-help'          : 'Show switch details via query',
    'no-supported'        : False,
    'parent-field'        : None,
    'obj-type'            : 'switches',
    'args' : (
        {
            'token'        : 'switch',
            'obj-type'     : 'switches',
            'command-type' : 'display-table',
            # 'command-type' : 'display-rest',
            'action'       : show_switch_pipeline,
        },
        {
            'choices' : (
                {
                    'field'        : 'dpid',
                    'completion'   : 'complete-alias-choice',
                    'type'         : 'dpid',
                    'help-name'    : 'switch dpid or alias',
                    'data-handler' : 'alias-to-value',
                },
                {
                    'token'        : 'all',
                },
            ),
        },
        {
            'optional'   : True,
            'choices' : (
                (
                    {
                        'token' : 'by',
                        'doc'   : 'switch|show-switch-order',
                    },
                    {
                        'choices' : (
                            {
                                'token'      : 'ip-address',
                                'sort'       : 'ip-address',
                                'action'     : 'display-table',
                                'short-help' : 'Sort by ip-address',
                            },
                            {
                                'token'      : 'connect-time',
                                'sort'       : '-connected-since',
                                'action'     : 'display-table',
                                'short-help' : 'Sort by connect time',
                            },
                        )
                    }
                ),
            )
        },
        {
            'field'      : 'detail',
            'type'       : 'enum',
            'values'     : ('details','brief'),
            'optional'   : True,
            'doc'        : 'switch|show-switch-format-+',
        },
    )
}
SWITCH_SHOW_REALTIME_STATS_COMMAND_DESCRIPTION = {
    'name'                : 'show',
    'mode'                : 'login',
    'short-help'          : 'Show switch stats via direct query to switch',
    'no-supported'        : False,
    'short-help'          : 'Show realtime stats for switch',
    'parent-field'        : None,
    'obj-type'            : 'switches',
    'args'                : (
        {
            'token'        : 'switch',
            'obj-type'     : 'switches',
            'command-type' : 'display-table',
            # 'command-type' : 'display-rest',
        },
        {
            'choices' : (
                {
                    'field'        : 'dpid',
                    'completion'   : 'complete-alias-choice',
                    'type'         : 'dpid',
                    'help-name'    : 'switch dpid or alias',
                    'data-handler' : 'alias-to-value',
                    'data'         : { 'detail' : 'scoped' },
                },
                {
                    'token'        : 'all',
                    'doc'          : 'reserved|all',
                },
            ),
        },
        {
            'choices' : (
                (
                    {
                        'field'      : 'realtimestats',
                        'type'       : 'enum',
                        'values'     : ('aggregate',
                                        'port',
                                        'desc',
                                        'queue',
                                        'group'
                                        ),
                        'action'     : 'display-rest',
                        'sort'       : ['groupid',],
                        'url'        : [
                                        'realtimestats/%(realtimestats)s/%(dpid)s/',
                                        'realtimestats/%(realtimestats)s/all/',
                                       ],
                        'rest-type'  : 'dict-of-list-of-switch',
                        'format'     : 'realtime_%(realtimestats)s',
                        'short-help' : 'Show requested item by querying switch',
                        'doc'        : 'switch|realtime-+',
                    },
                ),
                (
                    {
                        'field'      : 'realtimestats',
                        'type'       : 'enum',
                        'values'     : 'features',
                        'action'     : 'display-rest',
                        'url'        : [
                                         'realtimestats/%(realtimestats)s/%(dpid)s/',
                                         'realtimestats/%(realtimestats)s/all/',
                                       ],
                        'rest-type'  : 'dict-of-dict-of-switch|ports',
                        'format'     : 'realtime_%(realtimestats)s',
                        'doc'        : 'switch|realtime_features',
                    },
                    {
                        'field'    : 'detail',
                        'optional' : True,
                        'type'     : 'enum',
                        'values'   : ('details','brief'),
                        'doc'      : 'format|+',
                    },
                ),
                (
                    {
                        'field'      : 'realtimestats',
                        'type'       : 'enum',
                        'values'     : 'table',
                    },
                    {
                        'field'    : 'tabletype',
                        'type'     : 'enum',
                        'values'     : ('ip',
                                        'acl',
                                        'mpls'
                                        ),
                        'sort'       : ['mplsLabel','priority',],
                        'action'     : 'display-rest',
                        'url'        : 'realtimestats/%(realtimestats)s/%(tabletype)s/flow/%(dpid)s/',
                        'rest-type'  : 'dict-of-list-of-switch',
                        'format'     : 'realtime_%(realtimestats)s_%(tabletype)s_flow',
                        'doc'      : 'format|+',
                    },
                ),
                (
                    {
                        'field'      : 'realtimestats',
                        'type'       : 'enum',
                        'values'     : 'group',
                    },
                    {
                        'field'    : 'groupId',
                        #'data'     : {
                        #              'dpid'     : '$dpid',
                        #              },
                        #'type'     : 'groupId',
                        'completion' : 'group-id-completion',
                        #'sort'       : ['mplsLabel','priority',],
                        'action'     : 'display-rest',
                        'url'        : 'realtimestats/%(realtimestats)s/%(groupId)s/%(dpid)s/',
                        'rest-type'  : 'dict-of-list-of-switch',
                        'format'     : 'realtime_group',
                        'doc'      : 'format|+',
                    },
                ),
            )
        }
    )
}

SWITCH_SHOW_STATS_COMMAND_DESCRIPTION = {
    'name'                : 'show',
    'mode'                : 'login',
    'short-help'          : 'Show switch stats',
    'short-help'          : 'show stats for selected switch',
    'no-supported'        : False,
    'parent-field'        : None,
    'obj-type'            : 'switches',
    'args'                : (
        {
            'token'        : 'switch',
            # 'command-type' : 'display-rest',
        },
        {
            'field'        : 'dpid',
            'completion'   : 'complete-alias-choice',
            'type'         : 'dpid',
            'help-name'    : 'switch dpid or alias',
            'data-handler' : 'alias-to-value',
        },
        {
            'token'        : 'stats',
            'action'       : 'legacy-cli',
            'obj-type'     : 'switch-stats',
        },
    )
}

SWITCH_SHOW_STATS_OBJECT_DETAILS_COMMAND_DESCRIPTION = {
    'name'                : 'show',
    'mode'                : 'login',
    'short-help'          : 'Show statistics for a given switch',
    'no-supported'        : False,
    'parent-field'        : None,
    'obj-type'            : 'switches',
    'args'                : (
        {
            'token'        : 'switch',
            # 'command-type' : 'display-rest',
        },
        {
            'field'        : 'dpid',
            'completion'   : 'complete-alias-choice',
            'type'         : 'dpid',
            'help-name'    : 'switch dpid or alias',
            'data-handler' : 'alias-to-value',
        },
        {
            'token'        : 'stats',
            'action'       : 'legacy-cli',
            'obj-type'     : 'switch-stats',
        },
        {
            'field'        : 'stats-type',
            'type'         : 'enum',
            'values'       : (
                                'OFActiveFlow',
                                'OFFlowMod',
                                'OFPacketIn',
                             ),
        },
        {
            'field'        : 'start-time',
            'tag'          : 'start-time',
            'type'         : 'date',
            'data-handler' : 'date-to-integer',
            'short-help'   : 'Start time for displaying the stats',
            'optional'     : True,
        },
        {
            'field'        : 'end-time',
            'tag'          : 'end-time',
            'type'         : 'date',
            'data-handler' : 'date-to-integer',
            'short-help'   : 'End time for displaying the stats',
            'optional'     : True,
        },
        {
            'field'        : 'duration',
            'tag'          : 'duration',
            'type'         : 'duration',
            'short-help'   : 'Duration from the start or end for displaying the stats',
            'optional'     : True,
        },
        {
            'field'        : 'sample-interval',
            'tag'          : 'sample-interval',
            'type'         : 'integer',
            'short-help'   : 'Spacing between sampling windows',
            'optional'     : True,
        },
        {
            'field'        : 'sample-count',
            'tag'          : 'sample-count',
            'type'         : 'integer',
            'short-help'   : 'Number of samples in each window',
            'optional'     : True,
        },
        {
            'field'        : 'sample-window',
            'tag'          : 'sample-window',
            'type'         : 'integer',
            'short-help'   : 'Window length for sampling',
            'optional'     : True,
        },
        {
            'field'        : 'data-format',
            'tag'          : 'data-format',
            'type'         : 'enum',
            'values'       : ('value', 'rate',),
            'short-help'   : 'Whether to display as a raw value or rate',
            'optional'     : True,
        },
        {
            'field'        : 'display',
            'tag'          : 'display',
            'type'         : 'enum',
            'values'       : ('latest-value', 'graph', 'table'),
            'short-help'   : 'Display the latest value, a graph, or a table',
            'optional'     : True,
        },
    ),
}


show_switch_interfaces_pipeline = (
    {
        'proc'     : 'query-table',
        'obj-type' : 'interfaces',
        'key'      : 'id',
    },
    {
        'proc'     : 'query-table-append',
        'obj-type' : 'switch-interface-config',
        'key'      : 'id',
    },
    {
        'proc'       : 'join-rest',
        'url'        : [
                          # this is using 'switch' from previous results,
                          # not from the data items
                          'realtimestats/port/%(switch)s/',
                       ],
        'rest-type'  : 'dict-of-list-of-switch|portNumber',
        'key'        : 'switch|portNumber',
        'join-field' : 'switch|portNumber'
    },
    {
        'proc'     : 'display',
        'sort'     : ['switch', 'portName'],
        'format'   : 'switch-interfaces',
    },
)

SWITCH_SHOW_SWITCH_DPID_INTERFACES_COMMAND_DESCRIPTION = {
    'name'                : 'show',
    'mode'                : 'login',
    'no-supported'        : False,
    'short-help'          : 'Show interfaces for selected switch',
    'parent-field'        : None,
    'obj-type'            : 'switches',
    'args' : (
        {
            'token'        : 'switch',
            'command-type' : 'display-table',
        },
        {
            'choices' : (
                {
                    'field'        : 'dpid',
                    'completion'   : 'complete-alias-choice',
                    'type'         : 'dpid',
                    'help-name'    : 'switch dpid or alias',
                    'data-handler' : 'alias-to-value',
                },
                {
                    'field'        : 'dpid',
                    'type'         : 'enum',
                    'values'       : 'all',
                    'short-help'   : 'Show interfaces for all switches',
                },
            ),
        },
        {
            'choices' : (
                (
                    {
                        'token'      : 'interfaces',
                        'action'     : 'legacy-cli',
                        'obj-type'   : 'switch-interfaces',
                        'action'     : show_switch_interfaces_pipeline,
                        'short-help' : 'Show interfaces for switches',
                        'doc'        : 'switch|show-interfaces',
                    },
                    {
                        'token'      : 'stats',
                        'action'     : 'display-rest',
                        'url'        : 'realtimestats/port/%(dpid)s/',
                        'rest-type'  : 'dict-of-list-of-switch',
                        'format'     : 'realtime_port',
                        'rest-type'  : 'dict-of-list-of-switch',
                        'optional'   : True,
                        'short-help' : 'Show interfaces stats for switches',
                    },
                    {
                        'token'      : 'alias',
                        'action'     : 'display-table',
                        'obj-type'   : 'switch-interface-alias',
                        'optional'   : True,
                        'short-help' : 'Show interface aliases for switches',
                        'scoped'     : True,
                        'doc'        : 'switch|show-interfaces-alias',
                    }
                ),
                #{
                #    'field'      : 'vns',
                #    'type'       : 'enum',
                #    'values'     : 'vns',
                #    'action'     : 'legacy-cli',
                #    'obj-type'   : 'switch-ports-vns'
                #},
                {
                    'field'     : 'alias',
                    'type'      : 'enum',
                    'values'    : 'alias',
                    'obj-type'  : 'switch-alias',
                    'action'    : 'display-table',
                    'doc'       : 'switch|show-switch-alias',
                },
            )
        },
        {
            'optional' : True,
            'field'    : 'detail',
            'type'     : 'enum',
            'values'   : ('details', 'brief',),
            'short-help' : 'Show switch output format level',
        }
    )
}
"""
SWITCH_SUBMODE_SHOW_INTERFACE_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'config-switch*',
    'command-type' : 'display-table',
    'obj-type'     : 'switch-interfaces',
    'short-help'   : 'Show interfaces for switch associated with current submode',
    'args' : (
        {
            'token'    : 'interfaces',
            'action'   : 'legacy-cli',
            'scoped'   : True,
            'action'   : show_switch_interfaces_pipeline,
        },
    )
}


SWITCH_SHOW_TCPDUMP_COMMAND_DESCRIPTION = {
    'name'                : 'show',
    'mode'                : 'login',
    'short-help'          : 'Show switch tcpdump via controller',
    'no-supported'        : False,
    'parent-field'        : None,
    'current-mode-obj-id' : None,
    'obj-type'            : 'switches',
    'args' : (
        {
            'token'        : 'switch',
            'obj-type'     : 'switches',
        },
        {
            'field'        : 'dpid',
            'optional'     : False,
            'completion'   : 'complete-alias-choice',
            'type'         : 'dpid',
            'help-name'    : 'switch dpid or alias',
            'data-handler' : 'alias-to-value',
        },
        {
            'field'      : 'tcpdump',
            'optional'   : False,
            'type'       : 'enum',
            'values'     : 'trace',
            'obj-type'   : 'switch-tcpdump',
            'action'     : 'legacy-cli',
        },
        {
            'field'     : 'oneline',
            'type'      : 'enum',
            'values'    : 'oneline',
            'optional'  : True,
        },
        {
            'field'     : 'single_session',
            'type'      : 'enum',
            'values'    : 'single-session',
            'optional'  : True,
        },
        {
            'field'     : 'echo_reply',
            'type'      : 'enum',
            'values'    : 'echo-reply',
            'optional'  : True,
            'permute'   : 'skip',
        },
        {
            'field'     : 'echo_request',
            'type'      : 'enum',
            'values'    : 'echo-request',
            'optional'  : True,
            'permute'   : 'skip',
        },
        {
            'field'     : 'features_rep',
            'type'      : 'enum',
            'values'    : 'features-rep',
            'optional'  : True,
            'permute'   : 'skip',
        },
        {
            'field'     : 'flow_mod',
            'type'      : 'enum',
            'values'    : 'flow-mod',
            'optional'  : True,
            'permute'   : 'skip',
        },
        {
            'field'     : 'flow_removed',
            'type'      : 'enum',
            'values'    : 'flow-removed',
            'optional'  : True,
            'permute'   : 'skip',
        },
        {
            'field'     : 'get_config_rep',
            'type'      : 'enum',
            'values'    : 'get-config-rep',
            'optional'  : True,
            'permute'   : 'skip',
        },
        {
            'field'     : 'hello',
            'type'      : 'enum',
            'values'    : 'hello',
            'optional'  : True,
            'permute'   : 'skip',
        },
        {
            'field'     : 'packet_in',
            'type'      : 'enum',
            'values'    : 'packet-in',
            'optional'  : True,
            'permute'   : 'skip',
        },
        {
            'field'     : 'packet_out',
            'type'      : 'enum',
            'values'    : 'packet-out',
            'optional'  : True,
            'permute'   : 'skip',
        },
        {
            'field'     : 'port_status',
            'type'      : 'enum',
            'values'    : 'port-status',
            'optional'  : True,
            'permute'   : 'skip',
        },
        {
            'field'     : 'set_config',
            'type'      : 'enum',
            'values'    : 'set-config',
            'optional'  : True,
            'permute'   : 'skip',
        },
        {
            'field'     : 'stats_reply',
            'type'      : 'enum',
            'values'    : 'stats-reply',
            'optional'  : True,
            'permute'   : 'skip',
        },
        {
            'field'     : 'stats_request',
            'type'      : 'enum',
            'values'    : 'stats-request',
            'optional'  : True,
            'permute'   : 'skip',
        },
        {
            'field'     : 'detail',
            'type'      : 'enum',
            'values'    : 'detail',
            'optional'  : True,
        },
   )
}

#
# ------------------------------------------------------------------------------
# SWITCH_TUNNEL_SHOW_COMMAND_DESCRIPTION
#

show_tunnel_pipeline = (
    {
        'proc'      : 'query-rest',
        'url'       : [ 
                            'tunnel-manager/%(dpid)s',
                            'tunnel-manager/all',
                      ],
        'rest-type' : 'dict-of-dict-of-dpid',
    },
    {
        'proc'       : 'join-table',
        'obj-type'   : 'interfaces',
        'key'        : 'switch|portName',
        'join-field' : 'hexDpid|tunnelEndPointIntfName',
        'add-field'  : 'portNumber|portNumber',
    },
    {
        'proc'       : 'join-rest',
        'url'        : 'realtimestats/port/%(dpid)s/',
        'rest-type'  : 'dict-of-list-of-switch|portNumber',
        'key'        : 'switch|portNumber',
        'join-field' : 'hexDpid|portNumber',
    },
    {
        'proc'     : 'display',
        'format'   : 'tunnel-pipeline',
    }
)
"""
import fmtcnv
"""
TUNNEL_PIPELINE_FORMAT = {
    'tunnel-pipeline' : {
        'field-orderings' : {
            'default' : [ 'Idx',
                          'dpid',
                          'tunnelCapable',
                          'tunnelEnabled',
                          'tunnelEndPointIntfName',
                          'tunnelIPAddr',
                          'tunnelState',
                          'receivePackets',
                          'receiveBytes',
                          'transmitPackets',
                          'transmitBytes',
                        ]
        },

        'fields' : {
            'dpid'               : {
                                     'verbose-name' : 'Switch DPID',
                                     'primary_key': True,
                                     'formatter' : fmtcnv.replace_switch_with_alias,
                                  },
            'tunnelCapable'     : {
                                    'verbose-name' : 'Capable',
                                  },
            'tunnelEnabled'     : {
                                     'verbose-name' : 'Enabled',
                                  },
            'tunnelEndPointIntfName' : {
                                     'verbose-name' : 'IF Name',
                                  },
            'tunnelIPAddr'      : {
                                     'verbose-name' : 'IP Address',
                                  },
            'tunnelState'       : {
                                     'verbose-name' : 'State',
                                  },
            'receivePackets'    : {
                                     'verbose-name' : 'Rx Pkts',
                                  },
            'receiveBytes'      : {
                                     'verbose-name' : 'Rx Bytes',
                                  },
            'transmitPackets'   : {
                                     'verbose-name' : 'Tx Pkts',
                                  },
            'transmitBytes'     : {
                                     'verbose-name' : 'Tx Bytes',
                                  },
        },
    },
}



SWITCH_TUNNEL_SHOW_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'feature'      : 'vns',
    'short-help'   : 'Show tunnels for all switches',
    'command-type' : 'display-rest',
    'url'          : 'tunnel-manager/all',
    'obj-type'     : 'switch',
    'action'       : show_tunnel_pipeline,
    'args'         : (
        'tunnel',
    ),
}


SWITCH_TUNNEL_SHOW_WITH_DPID_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'feature'      : 'vns',
    'short-help'   : 'Show tunnels for selected switches',
    'command-type' : 'display-rest',
    'obj-type'     : 'switch',
    'url'          : 'tunnel-manager/%(dpid)s',
    'format'       : 'tunnel-details',
    'action'       : show_tunnel_pipeline,
    'args'         : (
        'tunnel',
        {
            'choices' : (
                {
                    'field'      : 'dpid',
                    'type'       : 'enum',
                    'values'     : 'all',
                },
                {
                    'field'      : 'dpid',
                    'completion' : 'complete-object-field',
                    'type'       : 'dpid',
                },
            ),
        },
    ),
}
#
# ------------------------------------------------------------------------------
# SWITCH_CORE_SWITCH_TERMINATION_COMMAND_DESCRIPTION
#

SWITCH_CORE_SWITCH_COMMAND_DESCRIPTION = {
    'name'         : 'core-switch',
    'short-help'   : 'Enable core-switch property for this switch',
    'mode'         : 'config-switch',
    'parent-field' : 'dpid',
    'obj-type'     : 'switch-config',
    'doc'          : 'switch|core-switch',
    'doc-example'  : 'switch|core-switch-example',
    'args' : (),
    'action': (
        {
            'proc' : 'update-config',
            'data' : {'core-switch' : True}
        },
    ),
    'no-action': (
        {
            'proc' : 'update-config',
            'data' : {'core-switch' : False},
        }
    )
}
#
# ------------------------------------------------------------------------------
# SWITCH_TUNNEL_TERMINATION_COMMAND_DESCRIPTION
#

SWITCH_TUNNEL_TERMINATION_COMMAND_DESCRIPTION = {
    'name'         : 'tunnel',
    'short-help'   : 'Enable/Disable tunnel creation for this switch',
    'mode'         : 'config-switch',
    'command-type' : 'update-config',
    'parent-field' : 'dpid',
    'obj-type'     : 'switch-config',
    'doc'          : 'switch|tunnel',
    'doc-example'  : 'switch|tunnel-example',
    'data'         : { 'tunnel-termination' : 'default' }, # for no command
    'args'         : (
        'termination',
        {
            'field'           : 'tunnel-termination',
            'type'            : 'enum',
            'values'          : ( "enabled", "disabled" ),
            'optional-for-no' : True,
        }
    )
}
#
# ------------------------------------------------------------------------------
# SWITCH_ALIAS_COMMAND_DESCRIPTION
#

SWITCH_SWITCH_ALIAS_COMMAND_DESCRIPTION = {
    'name'         : 'switch-alias',
    'mode'         : 'config-switch',
    'short-help'   : 'Attach alias to switch',
    'doc'          : 'switch|alias',
    'doc-example'  : 'switch|alias-example',
    'command-type' : 'manage-alias',
    'obj-type'     : 'switch-alias',
    'scoped'       : True,
    'args'         : (
        {
            'field'           : 'id',
            'optional-for-no' : False,
            'completion'      : 'complete-object-field',
        }
    )
}

#
# ------------------------------------------------------------------------------
# SWITCH_INTERFACE_COMMAND_DESCRIPTION
#  enter config-switch-if submode
#

SWITCH_INTERFACE_COMMAND_DESCRIPTION = {
    'name'                : 'interface',
    'mode'                : 'config-switch*',
    'short-help'          : 'Enter switch-if submode, configure switch interface',
    'command-type'        : 'config-submode',
    'obj-type'            : 'switch-interface-config',
    'parent-field'        : 'switch',
    'current-mode-obj-id' : 'switch',
    'submode-name'        : 'config-switch-if',
    'syntax-help'         : 'Enter an interface name',
    'doc'                 : 'switch|interface',
    'doc-example'         : 'switch|interface-example',
    'args' : (
        {
            'field'        : 'name',
            'completion'   : [ 'complete-object-field',
                               'complete-from-another',
                             ],
            'other'        : 'interfaces|portName',
            'scoped'       : 'dpid',
            'data-handler' : 'warn-missing-interface',
        }
    )
}

#
# ------------------------------------------------------------------------------
# SWITCHPORT_COMMAND_DESCRIPTION
#  'switchport mode external'
#  'no switchport mode external'
#

SWITCHPORT_COMMAND_DESCRIPTION = {
    'name'         : 'switchport',
    'short-help'   : 'Configure interface as connected to an external network',
    'mode'         : 'config-switch-if',
    'command-type' : 'config',
    'obj-type'     : 'switch-interface-config',
    'fields'       : ('broadcast', 'mode',),
    'action'       : 'write-fields',
    'no-action'    : 'reset-fields',
    'doc'          : 'switch|switchport',
    'doc-example'  : 'switch|switchport-example',
    'args'         : (
        'mode',
        {
            'field'       : 'mode',
            'type'        : 'enum',
            'values'      : 'external',
            'help-name'   : 'interface connects to external network',
            'short-help'  : 'interface connects to external network',
            'syntax-help' : 'external'
        },
    )
}

#
# ------------------------------------------------------------------------------
#


SWITCH_INTERFACE_INTERFACE_ALIAS_COMMAND_DESCRIPTION = {
    'name'         : 'interface-alias',
    'mode'         : 'config-switch-if',
    'short-help'   : 'Attach alias to switch interface',
    'command-type' : 'manage-alias',
    'obj-type'     : 'switch-interface-alias',
    'scoped'       : True,
    'doc'          : 'switch|interface-alias',
    'doc-example'  : 'switch|interface-alias-example',
    'args'         : (
        {
            'field'           : 'id',
            'optional-for-no' : False,
            'completion'      : 'complete-object-field',
            'short-help'      : 'Alias string',
        }
    )
}
"""
#
# FORMATS
#


SWITCH_FORMAT = {
    'switch' : {
        'field-orderings' : {
            'default' : [ 'Idx', '@', 'switch-alias', 'connected-since',
                          'ip-address', 'type', 'controller'],
            'details' : [ 'Idx','@', 'switch-alias', 'connected-since',
                          'ip-address', 'type', 'controller'],
            'brief'   : [ 'Idx', '@', 'switch-alias', 'controller'],
            },
        'fields' : {
            '@'                  : {
                                     'verbose-name' : 'Switch DPID',
                                   },
            'active'             : {
                                   },
            'core-switch'        : {
                                     'verbose-name' : 'Core Switch',
                                     'validate'  : 'validate_switch_core_switch',
                                   },
            'connected-since'    : {
                                     'verbose-name' : 'Connected Since',
                                     #'formatter' : fmtcnv.timestamp_to_local_timestr,
                                   },
            'capabilities'       : {
                                     'formatter' : fmtcnv.decode_switch_capabilities,
                                   },
            'actions'            : {
                                     'formatter' : fmtcnv.decode_switch_actions,
                                   },
            'ip-address'         : {
                                     'verbose-name' : 'Connected At',
                                   },
            'socket-address'     : {
                                   },
            'buffers'            : {
                                   },
            'controller'         : {
                                     'verbose-name' : 'Controller',
                                   },
            'tables'             : {
                                   },
            'switch-alias'       : {

                                     'verbose-name' : 'Alias'
                                   },
            'tunnelCapable'      : {
                                     'verbose-name' : 'Tun Capable',
                                   },
            'tunnelEnabled'      : {
                                     'verbose-name' : '-Enabled',
                                   },
            'tunnelState'        : {
                                      'verbose-name' : '-State',
                                   },
            'dp-desc'            : {
                                   },
            'hw-desc'            : {
                                   },
            'sw-desc'            : {
                                   },
            'serial-num'         : {
                                   },
            'type'               : {
                                   },
            }
        },
}
"""
SWITCH_CONFIG_FORMAT = {
    'switch-config' : {
        'field-orderings' : {
            'default' : [
                          'Idx',
                          'dpid',
                          'tunnel-termination',
                          'core-switch',
                        ],
        },
    },
}


SWITCH_ALIAS_FORMAT = {
    'switch-alias' : {
        'field-orderings' : {
            'default'     : [ 'Idx', 'id', 'switch' ],
            'brief'       : [ 'Idx', 'id', 'switch' ],
            },
        },
}


SWITCH_INTERFACE_CONFIG_FORMAT = {
    'switch-interface-config' : {
        'field-orderings' : {
            'default' :     [ 'Idx', 'if-name', 'mode'  ]
            },
        'fields'          : {
            'broadcast'           : {
                                    },
            'name'                : {
                                    },
            'mode'                : {
                                      'verbose-name' : 'Switchport Mode',
                                    },
            },
        },
}


SWITCH_INTERFACE_ALIAS_FORMAT = {
    'switch-interface-alias' : {
        'field-orderings' : {
            'default'     : [ 'Idx', 'id', 'switch', 'name' ]
            },
        'fields' : {
            'id'          : { 'verbose-name' : 'Alias',
                            }
            }
        },
}

SWITCH_INTERFACES_FORMAT = {
    'switch-interfaces' : {
        'field-orderings' : {
            'default' : [ 'Idx', 'switch', 'portName', 'state', 'config',
                          'receiveBytes', 'receivePackets', 'receiveErrors',
                          'transmitBytes', 'transmitPackets', 'transmitErrors',
                          'mode', 'broadcast',
                        ],
            'details' : [ 'Idx', 'switch', 'portName', 'hardwareAddress',
                          'config', 'stp-state', 'state', 'currentFeatures',
                          'advertisedFeatures', 'supportedFeatures',
                          'peer-features', 'mode', 'broadcast',
                        ],
            'brief'   : [ 'Idx', 'switch', 'portName', 'state', 'config' ],
            },

        'fields' : {
            'id'                  : {
                                    },
            'switch'              : { 'formatter' : fmtcnv.replace_switch_with_alias
                                    },
            'portName'            : { 'verbose-name' : 'IF',
                                    },
            'hardwareAddress'     : { 'verbose-name' : 'MAC Address'
                                    },
            'config'              : {
                                      'formatter' : fmtcnv.decode_port_config
                                    },
            'state'               : { 'verbose-name' : 'Link',
                                      'formatter' : fmtcnv.decode_port_up_down,
                                    },
            'stp-state'           : {
                                      'formatter' : lambda i, data : 
                                                    fmtcnv.decode_port_stp_state(data['state'],
                                                                                 data),
                                    },
            'currentFeatures'     : { 'verbose-name' : 'Curr Features',
                                      'formatter' : fmtcnv.decode_port_features
                                    },
            'advertisedFeatures'  : { 'verbose-name' : 'Adv Features',
                                      'formatter' : fmtcnv.decode_port_features
                                    },
            'supportedFeatures'   : { 'verbose-name' : 'Supp Features',
                                      'formatter' : fmtcnv.decode_port_features
                                    },
            'peer-features'       : { 'verbose-name' : 'Peer Features',
                                      'formatter' : fmtcnv.decode_port_features
                                    },
            'receiveBytes'         : { 'verbose-name' : 'Rcv Bytes',
                                       'formatter' : fmtcnv.decode_port_counter},
            'receivePackets'       : { 'verbose-name' : 'Rcv Pkts',
                                        'formatter' : fmtcnv.decode_port_counter},
            'receiveErrors'        : { 'verbose-name' : 'Rcv Errs',
                                       'formatter' : fmtcnv.decode_port_counter},
            'transmitBytes'        : { 'verbose-name' : 'Xmit Bytes',
                                       'formatter' : fmtcnv.decode_port_counter},
            'transmitPackets'      : { 'verbose-name' : 'Xmit Pkts',
                                       'formatter' : fmtcnv.decode_port_counter},
            'transmitErrors'       : { 'verbose-name' : 'Xmit Errs',
                                       'formatter' : fmtcnv.decode_port_counter},
            },
        },
}

TUNNEL_DETAILS_FORMAT = {
    'tunnel-details' : {
        'field-orderings' : {
            'default' : [ 'Idx', 'dpid', 'localTunnelIPAddr',
                           'tunnelPorts',
                        ]
        },

        'fields' : {
            'dpid'              : {
                                     'verbose-name' : 'Switch DPID',
                                     'primary_key': True,
                                     'formatter' : fmtcnv.replace_switch_with_alias,
                                  },
            'localTunnelIPAddr' : {
                                    'verbose-name' : 'Local tunnel IP',
                                  },
            'tunnelPorts' :       {
                                     'verbose-name' : 'Remote tunnel IP',
                                  },
        },
    },
}
"""

def group_id_completion(prefix, data, completions):
    dpid = data.get('dpid')
    #print dpid
    query_url = "http://127.0.0.1:8000/rest/v1/realtimestats/group/%s/" %(dpid)
    result = command.sdnsh.store.rest_simple_request(query_url)
    entries = json.loads(result)
    entries = entries[dpid]
    #print "result", entries
    for entry in entries:
        #print entry['groupId']
        if str(entry['groupId']).startswith(prefix):
            completions[str(entry['groupId'])+' '] = entry['groupId']
    return

command.add_completion('group-id-completion', group_id_completion,
                       {'kwargs': { 'prefix'       : '$text',
                                    'data'         : '$data',
                                    'completions'  : '$completions',
                                    }})
