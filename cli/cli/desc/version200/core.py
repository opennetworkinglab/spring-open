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
import run_config

"""
SHOW_RUNNING_CONFIG_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'short-help'   : 'Show the current active configuration',
    'action'       : 'legacy-cli',
    'no-supported' : False,
    'obj-type'     : 'running-config',
    'doc'          : 'running-config|show',
    'doc-example'  : 'running-config|show-example',
    'args'         : (
        'running-config',
        run_config.running_config_command_choices,
    )
}

SHOW_CLOCK_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'short-help'   : 'Show the current date and time',
    'action'       : 'display-rest',
    'no-supported' : False,
    'url'          : 'system/clock/local',
    'rest-type'    : 'dict-with-time|tz|year|month|day|hour|minute|second',
    'format'       : 'system-clock',
    'doc'          : 'clock|show',
    'doc-example'  : 'clock|show-example',
    'args'         : (
        'clock',
        {
            'optional'   : True,
            'field'      : 'detail',
            'type'       : 'enum',
            'values'     : ('details'),
            'short-help' : 'Show more date/time information',
            'doc'        : 'clock|show-format-details',
        },
    )
}

SHOW_CONFIG_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'short-help'   : 'Show saved configs (ex: startup-config, etc)',
    'action'       : 'legacy-cli',
    'obj-type'     : 'config',
    'no-supported' : False,
    'doc'          : 'config|show',
    'doc-example'  : 'config|show-example',
    'args'         : (
        'config',
        {
            'optional'  : True,
            'choices'   : (
            # <first> [ <version> ]
            # <first> diff <second> [ version ]
               ( # 1nd choice 'diff' <config> <config> [ version ]
                   {
                       'optional'   : False,
                       'field'      : 'first',
                       'type'       : 'config',
                       'completion' : 'complete-config',
                       'doc'        : 'config|show-first',
                   },
                   {
                       'optional' : False,
                       'token'    : 'diff',
                       # XXX implies config's aren't called 'diff'
                   },
                   {
                       'optional'   : False,
                       'field'      : 'second',
                       'type'       : 'config',
                       'completion' : 'complete-config',
                       'doc'        : 'config|show-second',
                   },
                   {
                       'optional' : True,
                       'field'    : 'version',
                       'type'     : 'string',
                       'doc'      : 'config|show-version',
                   },

               ),
               ( # 2st choice  <name> [ version ]
                   {
                       'optional'    : False,
                       'field'       : 'config',
                       'type'        : 'config',
                       'completion'  : 'complete-config',
                       'syntax-help' : 'Config file name',
                       'doc'         : 'config|show-single',
                   },
                   {
                       'optional' : True,
                       'field'    : 'version',
                       'type'     : 'string',
                       'doc'      : 'config|show-version',
                   },
               ),
           )
        }
    )
}

SHOW_CONFIG_FILE_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'short-help'   : 'Show a specific saved config file',
    'doc'          : 'config-file|show',
    'doc-example'  : 'config-file|show-example',
    'obj-type'     : 'config-file',
    'action'       : 'legacy-cli',
    'no-supported' : False,
    'args'         : (
        'config-file',
        {
            'optional'    : True,
            'field'       : 'config',
            'type'        : 'config',
            'completion'  : 'complete-config',
            'syntax-help' : 'Config file name',
        }
    ),
}

SHOW_TECH_SUPPORT_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'short-help'   : 'Show tech-support, collect output of various commands',
    'doc'          : 'tech-support|show',
    'doc-example'  : 'tech-support|show-example',
    'obj-type'     : 'tech-support',
    'action'       : 'legacy-cli',
    'no-supported' : False,
    'args'         : (
         'tech-support',
    ),
}

SHOW_FLOW_ENTRY_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'obj-type'     : 'flow-entry',
    'short-help'   : 'Show configured static flow-entries',
    'doc'          : 'flow-entry|show',
    'doc-example'  : 'flow-entry|show-example',
    'mode'         : 'login',
    'command-type' : 'display-table',
    'args'         : (
        'flow-entry',
        {
            'field'        : 'name',
            'optional'     : True,
            'base-type'    : 'identifier',
            'reserved'     : [ 'switch' ],
            'completion'   : 'complete-object-field',
        },
        {
            'field'           : 'switch',
            'tag'             : 'switch',
            'optional'        : True,
            'type'            : 'dpid',
            'completion'      : 'complete-from-another',
            'other'           : 'switches',
            'syntax-help'     : 'switch dpid or alias',
            'short-help'      : 'Show flow entries for a specific switch'
        }

    )
}

SHOW_FIREWALL_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'obj-type'     : 'firewall-rule',
    'short-help'   : 'Show firewall rules for controller interfaces',
    'doc'          : 'firewall-rule|show',
    'doc-example'  : 'firewall-rule|show-example',
    'mode'         : 'login',
    'command-type' : 'display-table',
    'args'         : (
        'firewall-rule',
        {
            'optional'  : True,
            'tag'       : 'controller',
            'field'     : 'controller',
            'type'      : 'string',
            'short-help': 'Show rules from a specific controller',
            'doc'       : 'firewall-rule|show-controller',
        },
        {
            'optional'  : True,
            'tag'       : 'type',
            'field'     : 'type',
            'type'      : 'string',
            'short-help': 'Show rules with a specific rule type',
            'doc'       : 'firewall-rule|show-type',
        },
        {
            'optional'  : True,
            'tag'       : 'number',
            'field'     : 'number',
            'type'      : 'integer',
            'short-help': 'Show rules with a specific rule number',
            'doc'       : 'firewall-rule|show-number',
        },
        {
            'optional'  : True,
            'tag'       : 'port',
            'field'     : 'port',
            'type'      : 'integer',
            'short-help': 'Show rules with a specific port number',
            'doc'       : 'firewall-rule|show-port',
        },
        {
            'optional'  : True,
            'tag'       : 'proto',
            'field'     : 'proto',
            'type'      : 'enum',
            'values'    : ('tcp', 'udp', 'vrrp'),
            'short-help': 'Show rules with a specific IP protocol',
            'doc'       : 'firewall-rule|show-proto',
        },
        {
            'optional'   : True,
            'field'      : 'src-ip',
            'tag'        : 'src-ip',
            'type'       : 'ip-address',
            'short-help' : 'Show rules with this src-ip configured',
            'doc'        : 'firewall-rule|show-src-ip',
        },
        {
            'optional'   : True,
            'field'      : 'vrrp-ip',
            'tag'        : 'local-ip',
            'type'       : 'ip-address',
            'short-help' : 'Show rules with this src-ip configured',
            'doc'        : 'firewall-rule|show-src-ip',
        },
    )
}

SHOW_THIS_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'obj-type'     : 'this',
    'mode'         : 'config-*',
    'short-help'   : 'Show the object associated with the current submode',
    'doc'          : 'show-this',
    'doc-example'  : 'show-this-example',
    'action'       : 'legacy-cli',
    'no-supported' : False,
    'args'         : (
        'this',
    )
}

SHOW_SWITCH_CLUSTER_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'obj-type'     : 'switch-cluster',
    'short-help'   : 'Show groups of interconnected openflow switches',
    'doc'          : 'switch-cluster|show',
    'doc-example'  : 'switch-cluster|show-example',
    'command-type' : 'display-rest',
    'url'          : 'realtimestatus/network/cluster/',
    'rest-type'    : 'dict-of-list-of-cluster-id|[switches]',
    'format'       : 'switch-cluster',
    'args'         : (
        {
            'token' : 'switch-cluster',
        }
    )
}

SHOW_EXTERNAL_PORTS_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'obj-type'     : 'external-ports',
    'short-help'   : 'Show switch ports connected to external L2 networks',
    'doc'          : 'external-ports|show',
    'doc-example'  : 'external-ports|show-example',
    'command-type' : 'display-rest',
    'url'          : 'realtimestatus/network/externalports/',
    'rest-type'    : 'dict-of-list-of-id|[ports]',
    'format'       : 'external-ports',
    'args'         : (
        {
            'token' : 'external-ports',
        }
    )
}


SHOW_VERSION_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'short-help'   : 'Show current build version number',
    'doc'          : 'core|version',
    'doc-example'  : 'core|version-example',
    'action'       : 'display-rest',
    'no-supported' : False,
    'url'          : 'system/version',
    'format'       : 'version',
    'detail'       : 'details',
    'args'         : (
        'version',
    )
}


SHOW_LOGGING_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'short-help'   : 'Show various controller logs',
    'doc'          : 'show-logging',
    'doc-example'  : 'show-logging-example',
    'no-supported' : False,
    'obj-type'     : 'logging',
    'args'         : (
        'logging',
        {
            'optional'   : 'true',
            'args'       : (
                {
                    'token' : 'controller',
                },
                {
                    'choices' : (
                        {
                            'field'      : 'controller-node',
                            'type'       : 'enum',
                            'values'     : 'all',
                        },
                        {
                            'field'        : 'controller-node',
                            'type'         : 'identifier',
                            'completion'   : 'complete-from-another',
                            'other'        : 'controller-node|id',
                            'data-handler' : 'alias-to-value',
                        }
                    ),
                }
            ),
        },
        {
            'field'    : 'log-name',
            'type'     : 'string',
            'completion' : 'complete-log-names',
            'doc'      : 'show-logging-+',
            'action'   : 'dump-log',
        },
    ),
}

SHOW_EVENT_HISTORY_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'short-help'   : 'Show recent network or system events',
    'doc'          : 'event-history|show',
    'doc-example'  : 'event-history|show-example',
    'action'       : 'legacy-cli',
    'no-supported' : False,
    'obj-type'     : 'event-history',
    'args'         : (
        'event-history',
        {
            'field'    : 'event',
            'type'     : 'enum',
            'values'   : (
                          # 'attachment-point', not currently available
                          # 'packet-in', not currently available
                          'topology-link',
                          'topology-switch',
                          'topology-cluster',
                         ),
            'doc'      : 'show-event-history-+',
        },
        {
            'optional'  : True,
            'field'     : 'count',
            'tag'       : 'last',
            'base-type' : 'integer',
            'range'     : (1,10000),
            'doc'       : 'show-event-history-count',
        }
    ),
}

SHOW_FLOW_CACHE_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'short-help'   : 'Show the contents of the controller flow cache',
    'doc'          : 'flow-cache|show',
    'doc-example'  : 'flow-cache|show-example',
    'action'       : 'display-rest',
    'url'          : 'flow-cache/vns/all/all',
    'rest-type'    : 'dict-with-flows',
    'format'       : 'flow-cache',
    'no-supported' : False,
    #'obj-type'     : 'flow-cache',
    'args'         : (
        'flow-cache',
        {
            'optional' : True,
            'choices' : (
                (
                    {
                        'optional' : False,
                        'field'    : 'application',
                        'tag'      : 'app',
                        'type'     : 'string',
                        'short-help': 'Show the entries associated with a specific application',
                        'doc'       : 'flow-cache|show-application',
                    },
                    {
                        'optional' : False,
                        'field'    : 'instance',
                        'tag'      : 'app-instance',
                        'type'     : 'string',
                        'doc'      : 'flow-cache|show-instance',
                        'action'   : 'display-rest',
                        'url'      : 'flow-cache/%(application)s/%(instance)s/all',
                        'rest-type': 'dict-with-flows',
                    },
                ),
                {
                    'field'     : 'counters',
                    'type'      : 'enum',
                    'values'    : 'counters',
                    'action'    : 'display-rest',
                    'url'       : 'flow-cache/vns/all/counters',
                    'format'    : 'flow-cache-counters',
                    'rest-type' : 'dict-with-counters|status',
                    'detail'    : 'details',
                    'short-help': 'Show the counters for the flow cache',
                    'doc'       : 'flow-cache|show-counters',
                },
            )
        }
    ),
}

FEATURE_COMMAND_DESCRIPTION = {
    'name'         : 'feature',
    'mode'         : 'config',
    'short-help'   : 'Enable features for controller',
    'doc'          : 'feature',
    'doc-example'  : 'feature-example',
    'command-type' : 'config',
    'obj-type'     : 'feature',
    'args': {
        'choices': (
            {   
                'token'            : 'vns',
                'short-help'       : 'Enable the VNS feature',
                'doc'              : 'feature-vns',
                'action'           : (
                    {
                        'proc'     : 'write-object',
                        'data'     : {
                                       # The primary key ('id') must be present to
                                       # read the old row value, and update items,
                                       # otherwise a 'new' row is written with default
                                       # values

                                       'id'             : 'feature',
                                       'netvirt-feature'    : True,
                                     },
                    },
                    {
                        'proc'     : 'wait-for-controller',
                    },
                ),
                'no-action'        : (
                    {
                        'proc'     : 'write-object',
                        'data'     : {
                                       'id'          : 'feature',
                                       'netvirt-feature' : False ,
                                     },
                    },
                    {
                        'proc'     : 'wait-for-controller',
                    },
                )
            },              
            {                   
                'token'            : 'flow-pusher',
                'short-help'       : 'Enable the static flow pusher feature',
                'doc'              : 'feature-flow-pusher',
                'action'           : (
                    {
                        'proc'     : 'write-object',
                        'data'     : {
                                       'id'                         : 'feature',
                                       'static-flow-pusher-feature' : True,
                                     },
                    },
                    {
                        'proc'     : 'wait-for-controller',
                    },

                ),
                'no-action'        : (
                    {
                        'proc'     : 'write-object',
                        'data'     : {
                                       'id'                         : 'feature',
                                       'static-flow-pusher-feature' : False,
                                     },
                    },
                    {
                        'proc'     : 'wait-for-controller',
                    },
                )
            },                  
            {                   
                'token'            : 'performance-monitor',
                'short-help'       : 'Enable the performance monitor feature',
                'doc'              : 'feature-performance-monitor',
                'action'           : (
                    {
                        'proc'     : 'write-object',
                        'data'     : {
                                       'id'                          : 'feature',
                                       'performance-monitor-feature' : True,
                                     },
                    },
                    {
                        'proc'     : 'wait-for-controller',
                    },
                ),
                'no-action'        : (
                    {
                        'proc'     : 'write-object',
                        'data'     : {
                                       'id'                          : 'feature',
                                       'performance-monitor-feature' : False,
                                     },
                    },
                    {
                        'proc'     : 'wait-for-controller',
                    },
                )
            },       
        ),       
    },      
}


SHOW_FEATURE_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'short-help'   : 'Show enabled and disabled features',
    'doc'          : 'show-feature',
    'doc-example'  : 'show-feature-example',
    'command-type' : 'display-table',
    'obj-type'     : 'feature',
    'data'         : { 'id' : 'feature' },
    'args'         : (
        'feature',
    )
}


VERSION_COMMAND_DESCRIPTION = {
    'name'                : 'version',
    'no-supported'        : False,
    'short-help'          : 'Move to a specific version of command syntax',
    'doc'                 : 'core|version',
    'doc-example'         : 'core|version-example',
    'mode'                : 'config*',
    'action'              : 'version',
    'args': {
        'field'      : 'version',
        'type'       : 'string',
        'completion' : 'description-versions'
    }
}

"""
CLEAR_COMMAND_DESCRIPTION = {
    'name'                : 'clearterm',
    'no-supported'        : False,
    'short-help'          : 'Clears and resets the terminal screen',
    'doc'                 : 'clearterm',
    'doc-example'         : 'clearterm-example',
    'mode'                : 'login',
    'action'              : 'clearterm',
    'args'                : {}
}
"""

COPY_COMMAND_DESCRIPTION = {
    'name'                : 'copy',
    'no-supported'        : False,
    'short-help'          : 'Copy configs to other configs',
    'doc'                 : 'copy|copy',
    'doc-example'         : 'copy|copy-example',
    'mode'                : 'enable',
    'action'              : 'legacy-cli',
    'obj-type'            : 'copy',
    'args': (
        {
            'choices' : (
                {
                    'field'       : 'source',
                    'type'        : 'config',
                    'completion'  : 'complete-config-copy',
                    'help-name'   : 'source specifier',
                    'doc'         : 'copy|copy-source',
                },
            ),
        },
        {
            'optional': True,
            'choices' : (
                {
                    'field'       : 'dest',
                    'type'        : 'config',
                    'completion'  : 'complete-config-copy',
                    'help-name'   : 'destination specifier',
                    'doc'         : 'copy|copy-dest',
                },
            )
        },
    )
}


WRITE_COMMAND_DESCRIPTION = {
    'name'                : 'write',
    'no-supported'        : False,
    'short-help'          : 'Write config to memory or terminal, or clear',
    'doc'                 : 'core|write',
    'doc-example'         : 'core|write-example',
    'mode'                : 'enable',
    'action'              : 'legacy-cli',
    'obj-type'            : 'write',
    'args': (
        {
            'field'       : 'source',
            'type'        : 'enum',
            'values'      : {'terminal' : 'running-config'},
            'short-help'  : 'Show the current active configuration',
            'doc'         : 'core|write-terminal',
            'action'      : 'legacy-cli',
            'obj-type'    : 'copy',
            'doc-example' : 'write-source-+',
        },
    ),
}

WRITE_ERASE_COMMAND_DESCRIPTION = {
    'name'           : 'write',
    'no-supported'   : False,
    'mode'           : 'enable',
    'args'           : (
        {
            'field'      : 'erase',
            'type'       : 'enum',
            'values'     : ('erase'),
            'short-help' : 'Erase settings and restore to factory defaults',
            'doc'        : 'core|factory-default',
        }
    ),
    'action'         : (
        {
            'proc' : 'factory-default',
        },
    )
}
"""
ENABLE_SUBMODE_COMMAND_DESCRIPTION = {
    'name'                : 'enable',
    'mode'                : 'login',
    'no-supported'        : False,
    'help'                : 'Enter enable mode',
    'short-help'          : 'Enter enable mode',
    'doc'                 : 'enable',
    'doc-example'         : 'enable-example',
    'command-type'        : 'config-submode',
    'obj-type'            : None,
    'parent-field'        : None,
    'submode-name'        : 'enable',
    'args'                : (),
}

CONFIGURE_SUBMODE_COMMAND_DESCRIPTION = {
    'name'                : 'configure',
    'mode'                : 'enable',
    'no-supported'        : False,
    'help'                : 'Enter configure mode',
    'short-help'          : 'Enter configure mode',
    'doc'                 : 'config',
    'doc-example'         : 'config-example',
    'command-type'        : 'config-submode',
    'obj-type'            : None,
    'parent-field'        : None,
    'submode-name'        : 'config',
    'args'                : {
        'token'           : 'terminal',
        'optional'        : 'true',
    },
}
"""
DEBUG_CLI_COMMAND_DESCRIPTION = {
    'name'                : 'debug',
    'mode'                : ['login', 'enable', 'config*'],
    'short-help'          : 'Manage various cli debugging features',
    'doc'                 : 'debug|debug-cli',
    'doc-example'         : 'debug|debug-cli-example',
    'args'                : {
        'choices' : (
            {
                'token'      : 'cli',
                'action'     : 'cli-set',
                'no-action'  : 'cli-unset',
                'variable'   : 'debug',
                'short-help' : 'Display more detailed information on errors',
                'doc'        : 'debug|cli',
            },
            {
                'token'      : 'cli-backtrace',
                'action'     : 'cli-set',
                'no-action'  : 'cli-unset',
                'variable'   : 'cli-backtrace',
                'short-help' : 'Display backtrace information on errors',
                'doc'        : 'debug|cli-backtrace',
            },
            {
                'token'      : 'cli-batch',
                'action'     : 'cli-set',
                'no-action'  : 'cli-unset',
                'variable'   : 'cli-batch',
                'short-help' : 'Disable any prompts to allow simpler batch processing',
                'doc'        : 'debug|cli-batch',
            },
            {
                'token'      : 'description',
                'action'     : 'cli-set',
                'no-action'  : 'cli-unset',
                'variable'   : 'description',
                'short-help' : 'Display verbose debug information while processing commands',
                'doc'        : 'debug|description',
            },
            (
                {
                    'token'      : 'rest',
                    'action'     : 'cli-set',
                    'no-action'  : 'cli-unset',
                    'variable'   : 'rest',
                    'short-help' : 'Display URLs of any information retrieved via REST',
                    'doc'        : 'debug|rest',
                },
                {
                    'optional'        : True,
                    'optional-for-no' : True,
                    'choices' : (
                        {
                            'field'           : 'detail',
                            'type'            : 'enum',
                            'values'          : ('details', 'brief'),
                            'short-help'      : 'Display both URLs and returned content for REST requests',
                            'doc'             : 'debug|debug-cli-rest-format',
                        },
                        {
                            'field'           : 'record',
                            'tag'             : 'record',
                            'type'            : 'string',
                            'short-help'      : 'record rest api activitiy',
                        },
                    ),
                },
            ),
            {
                'token'      : 'python',
                'action'     : 'shell-command',
                'command'    : 'python',
                'short-help' : 'Enter a python shell',
                'doc'        : 'debug|python',
            },
            {
                'token'      : 'bash',
                'action'     : 'shell-command',
                'command'    : 'bash',
                'short-help' : 'Enter a bash shell',
                'doc'        : 'debug|bash',
            },
            {
                'token'      : 'cassandra-cli',
                'action'     : 'shell-command',
                'command'    : 'cassandra-cli',
                'short-help' : 'Enter a cassandra shell',
                'doc'        : 'debug|assandra-cli',
            },
            {
                'token'      : 'netconfig',
                'action'     : 'shell-command',
                'command'    : 'netconfig',
                'short-help' : 'Enter a netconfig shell',
                'doc'        : 'debug|netconfig',
            },
            #  tcpdump requires that the 'tail' of the debug command be tcpdump syntax,
            #  but that would mean describing the complete tcpdump syntax here, and parsing it
            # {
                # 'token'   : 'tcpdump',
                # 'action'  : 'shell-command',
                # 'command' : '/opt/sdnplatform/sys/bin/bscnetconfig',
            # },
        )
    }
}


HA_CONFIG_CLI_COMMAND_DESCRIPTION = {
    'name'                : 'ha',
    'short-help'          : 'Configure high availability',
    'doc'                 : 'ha|ha-vrrp',
    'doc-example'         : 'ha|ha-vrrp-example',
    'mode'                : 'config*',
    'feature'             : 'ha',
    'args'                : {
        'choices' : (
            {
                'field'           : 'cluster-number',
                'tag'             : 'cluster-number',
                'base-type'       : 'integer',
                'range'           : (1, 255),
                'completion-text' : '1-255',
                'syntax-help'     : "Enter a small integer (1-255) to distinguish different controller clusters",
                'short-help'      : 'Set the VRRP cluster number',
                'doc'             : 'ha|ha-cluster-number',
                'obj-type'        : 'global-config',
                'obj-id'          : 'global',
                'default-for-no'  : 1,
                'action'          : 'write-fields-explicit',
                'no-action'       : 'reset-fields-explicit',
            },
        )
    }
}

HA_CLI_COMMAND_DESCRIPTION = {
    'name'                : 'ha',
    'no-supported'        : False,
    'short-help'          : 'Perform actions related to high availability',
    'doc'                 : 'ha|ha-failover',
    'doc-example'         : 'ha|ha-failover-example',
    'feature'             : 'ha',
    'mode'                : 'enable',
    'args'                : {
        'choices' : (
            {
                'token'   : 'failover',
                'short-help': 'Trigger a failure of the current node',
                'path'    : 'system/ha/failback',
                'action'  : (
                    {
                        'proc'   : 'confirm',
                        'prompt' : 'Fallback will change the HA operating mode,'
                                   'enter "yes" (or "y") to continue:',
                    },
                    {
                        'proc' : 'rest-post-data',
                    },
                ),
            },
            (
                {
                    'token' : 'provision',
                },
                {
                    'field'  : 'ip',
                    'type'   : 'ip-address-not-mask',
                    'action' : (
                        {
                            'proc'   : 'confirm',
                            'prompt' : "Confirm to continue addition of new ip, "
                                       'enter "yes" (or "y") to continue:',
                        },
                        {
                            'proc' : 'rest-post-data',
                            'path' : 'system/ha/provision'
                        },
                     ),
                },
            ),
            (
                {
                    'token' : 'decommission',
                },
                {
                    'action'        : 'controller-decommission',
                    'obj-type'      : 'controller-node',
                    'optional'      : False,
                    'field'         : 'id',
                    'type'          : 'identifier',
                    'completion'    : 'complete-alias-choice',
                    'data-handler'  : 'alias-to-value',
                    'doc'           : 'controller|controller-node-name',
                },
            ),
        )
    }
}


SHOW_HA_COMMAND_DESCRIPTION = {
    'name'                : 'show',
    'no-supported'        : False,
    'short-help'          : 'Show high availability configuration',
    'doc'                 : 'ha|show',
    'doc-example'         : 'ha|show-example',
    'feature'             : 'ha',
    'mode'                : 'login',
    'obj-type'            : 'global-config',
    'command-type'        : 'display-table',
    'action'              : (
        {
            'proc' : 'display-table',
        },
        # borrowed from controller-node, would be better to
        # have this in common code
        {
            'proc'     : 'query-table',
            'obj-type' : 'controller-node',
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
            'title'      : '\nController Nodes\n',
            'format'     : 'controller-node',
        }
    ),
    'args'                : (
        'ha',
        {
            'optional'    : True,
            'field'       : 'detail',
            'type'        : 'enum',
            'values'      : 'details',
            'doc'         : 'format|details',
        },

    )
}

BOOT_COMMAND_DESCRIPTION = {
    'name'           : 'boot',
    'no-supported'   : False,
    'short-help'     : 'Configure system boot options',
    'doc'            : 'core|boot',
    'doc-example'    : 'core|boot-example',
    'mode'           : 'enable',
    'args'           : (
        {
            'field'      : 'factory-default',
            'type'       : 'enum',
            'values'     : ('factory-default'),
            'short-help' : 'Erase settings and restore to factory defaults',
            'doc'        : 'core|factory-default',
        }
    ),
    'action'         : (
        {
            'proc' : 'factory-default',
        },
    )
}

RELOAD_COMMAND_DESCRIPTION = {
    'name'           : 'reload',
    'no-supported'   : False,
    'short-help'     : 'Reload and reboot',
    'doc'            : 'core|reload',
    'doc-example'    : 'core|reload-example',
    'mode'           : 'enable',
    'args'           : (),
    'obj-type'       : 'reload',
    'action'         : (
        {
            'proc'   : 'confirm',
            'prompt' : 'Confirm Reload (yes to continue) '
        },
        # XXX what about checking to see if the running-config
        # has been copied to the startup config?
        {
            'proc' : 'query-rest',
            'url'  : 'system/reload'
        },
    )
}

UPGRADE_COMMAND_DESCRIPTION = {
    'name'           : 'upgrade',
    'no-supported'   : False,
    'short-help'     : 'Manage the controller upgrade process',
    'doc'            : 'core|upgrade',
    'doc-example'    : 'core|upgrade-example',
    'mode'           : 'enable',
    'args'           : (
                        {
                            'optional'   : True,
                            'type'       : 'enum',
                            'field'      : 'force',
                            'values'     : ('force'),
                            'short-help' : 'Ignore validation errors and upgrade anyway',
                            'doc'        : 'core|upgrade-force'
                        },
                        {
                            'optional'   : True,
                            'type'       : 'enum',
                            'field'      : 'details',
                            'values'     : ('details'),
                            'short-help' : 'Display more information on the upgrade',
                            'doc'        : 'core|upgrade-details'
                        }
    ),
    'obj-type'       : 'upgrade',
    'action'         : 'controller-upgrade',
}

UPGRADE_ABORT_COMMAND_DESCRIPTION = {
    'name'           : 'upgrade',
    'no-supported'   : False,
    'short-help'     : 'Manage the controller upgrade process',
    'mode'           : 'enable',
    'doc-example'    : 'core|upgrade-abort-example',
    'obj-type'       : 'upgrade',
    'args'           : (
                        {
                            'type'       : 'enum',
                            'field'      : 'abort',
                            'values'     : 'abort',
                            'short-help' : 'Abort an upgrade already installed',
                            'doc'        : 'core|upgrade-abort'
                        },
                       ),
    'action'         : (
        {
            'proc'   : 'confirm',
            'prompt' : 'Confirm to abort an upgrade already installed'\
                ' onto second partition\n'\
                'enter "yes" (or "y") to continue:',
        },
        {
            'proc' : 'rest-post-data',
            'path' : 'system/upgrade/abort'
        },
    )
}

ROLLBACK_COMMAND_DESCRIPTION = {
    'name'           : 'rollback',
    'no-supported'   : False,
    'short-help'     : 'rollback cluster to specified config',
    'mode'           : 'enable',
    'doc-example'    : 'core|rollback-config-example',
    'obj-type'       : 'rollback',
    'args'           : (
                            {
                                'type'       : 'enum',
                                'field'      : 'dir',
                                'short-help' : 'source directory',
                                'values'     : ('images://', 'saved-configs://'),
                            },
                            {
                                'field'    : 'file',
                                'type'     : 'string',
                                'short-help' : 'config file name',
                            },
                       ),
    'action'         : 'controller-config-rollback'
}


command.add_action('implement-connect', command.sdnsh.implement_connect,
                    {'kwargs': {'data'      : '$data',}})


CONNECT_COMMAND_DESCRIPTION = {
    'name'         : 'connect',
    'mode'         : 'login',
    'no-supported' : False,
    'short-help'   : 'Connect to a controller\'s REST API',
    'doc'          : 'connect|connect',
    'doc-example'  : 'connect|connect-example',
    'obj-type'     : 'controller-node',
    'action'       : 'implement-connect',
    'args'         : (
        {
            'choices': (
                {
                    'field'           : 'controller-id',
                    'type'            : 'obj-type',
                    'completion'      : 'complete-alias-choice',
                    'completion-text' : 'controller id or alias',
                    'short-help'      : 'controller-id',
                    'doc'             : 'connect|connect-id',
                },
                {
                    'field'           : 'ip-address',
                    'type'            : 'ip-address',
                    'completion'      : 'complete-alias-choice',
                    'completion-text' : 'ip address',
                    'short-help'      : 'controller-id',
                    'doc'             : 'connect|connect-ip',
                },
            ),
        },
        {
            'field'    : 'port',
            'tag'      : 'port',
            'type'     : 'integer',
            'range'    : (1,65535),
            'optional' : True,
            'doc'      : 'connect|connect-port'
        },
    )
}

command.add_action('implement-ping', command.sdnsh.implement_ping,
                    {'kwargs': {'data'      : '$data',}})

PING_COMMAND_DESCRIPTION = {
    'name'         : 'ping',
    'mode'         : 'login',
    'no-supported' : False,
    'short-help'   : 'ping a switch or ip address',
    'doc'          : 'ping|ping',
    'doc-example'  : 'ping|ping-example',
    'obj-type'     : 'switches',
    'action'       : 'implement-ping',
    'args'         : (
        {
            'field'           : 'count',
            'type'            : 'integer',
            'tag'             : 'count',
            'optional'        : True,
            'completion-text' : 'ping count',
            'doc'             : 'ping|ping-count'
        },
        {
            'field'           : 'ip-address',
            'type'            : 'resolvable-ip-address',
            'completion-text' : 'ip-address ping target',
            'other'           : 'switches|dpid',
            'completion'      : [
                                 'complete-alias-choice',
                                 'complete-from-another',
                                ],
            'data-handler'    : 'alias-to-value',
            'doc'             : 'types|resolvable-ip-address'
        },
    ),
}


command.add_action('implement-traceroute', command.sdnsh.implement_traceroute,
                    {'kwargs': {'data'      : '$data',}})

TRACEROUTE_COMMAND_DESCRIPTION = {
    'name'         : 'traceroute',
    'mode'         : 'login',
    'no-supported' : False,
    'short-help'   : 'Determine the L3 path to some destination',
    'doc'          : 'traceroute|traceroute',
    'doc-example'  : 'traceroute|traceroute-example',
    'obj-type'     : 'switches',
    'action'       : 'implement-traceroute',
    'args'         : (
        {
            'field'           : 'ip-address',
            'type'            : 'resolvable-ip-address',
            'completion-text' : 'ip-address ping target',
            'other'           : 'switches|dpid',
            'completion'      : [
                                 'complete-alias-choice',
                                 'complete-from-another',
                                ],
            'data-handler'    : 'alias-to-value',
            'doc'             : 'types|resolvable-ip-address'
        },
    ),
}

SET_CLI_COMMAND_DESCRIPTION = {
    'name'         : 'set',
    'mode'         : 'login',
    'no-supported' : False,
    'short-help'   : 'Manage CLI sessions settings',
    'doc'          : 'set|set',
    'doc-example'  : 'set|set-example',
    'action'       : 'cli-set',
    'variable'     : 'set',
    'args'         : (
        'length',
        {
            'choices' : (
                {
                    'field'     : 'length',
                    'base-type' : 'integer',
                    'range'     : (0,512),
                },
                {
                    'field'     : 'length',
                    'type'      : 'enum',
                    'values'    : 'term',
                },
            )
        },
    ),
}
#"""
#
# FORMATS 
#

import fmtcnv

"""
GLOBAL_CONFIG_FORMAT = {
    'global-config' : {
        'field-orderings' : {
            'details'  : [
                          'cluster-name',
                          'cluster-number',
                          'ha-enabled',
                         ],
            'default'  : [
                          'cluster-name',
                          'cluster-number',
                          'ha-enabled',
                         ],
        },

        'fields'              : {
            'cluster-name'    : {
                                 'verbose-name' : 'Cluster Name',
                                },
            'cluster-number'  : {
                                 'verbose-name' : 'Cluster Number',
                                },
            'ha-enabled'      : {
                                 'verbose-name' : 'HA Enabled',
                                },
        },
    },
}


FEATURE_FORMAT = {
    'feature' : {
        'field-orderings' : {
            'default'  : [
                          'netvirt-feature',
                          'static-flow-pusher-feature',
                          'performance-monitor-feature',
                         ],
        },

        'fields'          : {
            'netvirt-feature'                 : { 'verbose-name' : 'VNS Feature',
                                              'formatter' : fmtcnv.replace_boolean_with_enable_disable,
                                            },
            'static-flow-pusher-feature'  : { 'verbose-name' : 'Static Flow Pusher Feature',
                                              'formatter' : fmtcnv.replace_boolean_with_enable_disable,
                                            },
            'performance-monitor-feature' : { 'verbose-name' : 'Performance Monitoring Feature',
                                              'formatter' : fmtcnv.replace_boolean_with_enable_disable,
                                            },
        }
    },
}

LINK_FORMAT = {
    'link' : {
        'field-orderings' : {
            'default' : [ 'Idx', 'src-switch', 'src-port', 'src-port-state',
                          'Idx', 'dst-switch', 'dst-port', 'dst-port-state', 'link-type' ]
             },
        'fields' : {
            'src-port'       : { 'formatter' : fmtcnv.decode_openflow_port_src_switch },
            'dst-port'       : { 'formatter' : fmtcnv.decode_openflow_port_dst_switch },
            'src-switch'     : { 'formatter' : fmtcnv.replace_switch_with_alias },
            'dst-switch'     : { 'formatter' : fmtcnv.replace_switch_with_alias },
            'src-port-state' : { 'formatter' : fmtcnv.decode_port_state },
            'dst-port-state' : { 'formatter' : fmtcnv.decode_port_state },
            'link-type'      : { 'verbose-name' : 'Link Type',
                               },
            }
        },
}

CONFIG_FORMAT = {
    'config' : {
        'field-orderings' : {
            'default' : ['Idx', 'name', 'length', 'version', 'timestamp' ],
            },
        'fields' : {
            'name'     : { 'primary_key': True
                         },
            'version'  : {
                         },
            'length'   : {
                         },
            'timestamp': {
                         },
            },
        },
}


TEST_PKTIN_ROUTE_FORMAT = {
    'test-pktin-route' : {
        'field-orderings' : {
            'default' : ['Idx', 'cluster', 'hop', 'dpid', 'inPort', 'outPort'],
            },
        'fields' : {
            'cluster' : {'verbose-name' : 'Cluster',
                         'formatter' : fmtcnv.print_cluster_id
                        },
            'hop'     : {'verbose-name' : 'Hop'
                        },
            'dpid'    : {'verbose-name' : 'Switch',
                        'formatter'    : fmtcnv.print_switch_and_alias,
                        },
            'inPort' :  {'verbose-name' : 'Input-Intf',
                         'formatter'   : fmtcnv.decode_openflow_port_dpid,
                        },
            'outPort':  {'verbose-name': 'Output-Intf',
                         'formatter'   : fmtcnv.decode_openflow_port_dpid,
                        },
            },
        },
}


PERFORMANCE_DATA_FORMAT = {
    'performance-data' : {
        'field-orderings' : {
            'default' : ['BktNo', 'StartTime', 'CompName', 'Pkts', 'Avg',
                         'Min', 'Max', 'StdDev'],
            },
        'fields' : {
            'Pkts'     : {'verbose-name': 'Pkts*'
                         },
            'CompName' : {'verbose-name' : 'Component'
                         },
            'StartTime': {'verbose-name' : 'Start Time'
                         }
            }
        },
}


FLOW_CACHE_COUNTERS_FORMAT = {
    'flow-cache-counters' : {
        'field-orderings' : {
            'details' : [
                            'applName',
                            'maxFlows',
                            'activeCnt',
                            'inactiveCnt',
                            'addCnt',
                            'delCnt',
                            'activatedCnt',
                            'deactivatedCnd',
                            'cacheHitCnt',
                            'missCnt',
                            'flowModRemovalMsgLossCnt',
                            'notStoredFullCnt',
                            'fcObjFreedCnt',
                            'unknownOperCnt',
                            'flowCacheAlmostFull',
                        ],

        },

        'fields' : {
            'applName'                 : { 'verbose-name' : 'Application name',
                                         },
            'maxFlows'                 : { 'verbose-name' : 'Max flow cache size',
                                         },
            'activeCnt'                : { 'verbose-name' : 'Active entries',
                                         },
            'inactiveCnt'              : { 'verbose-name' : 'Inactive entries',
                                         },
            'addCnt'                   : { 'verbose-name' : 'Add operations',
                                         },
            'delCnt'                   : { 'verbose-name' :'Del operations',
                                         },
            'activatedCnt'             : { 'verbose-name' : 'Activated',
                                         },
            'deactivatedCnd'           : { 'verbose-name' : 'Deactivated',
                                         },
            'cacheHitCnt'              : { 'verbose-name' : 'Cache hits',
                                         },
            'missCnt'                  : { 'verbose-name' : 'Cache misses',
                                         },
            'flowModRemovalMsgLossCnt' : { 'verbose-name' : 'Lost removal messages',
                                         },
            'notStoredFullCnt'         : { 'verbose-name' : 'Not stored; cache full',
                                         },
            'fcObjFreedCnt'            : { 'verbose-name' : 'Free count',
                                         },
            'unknownOperCnt'           : { 'verbose-name' : 'Unknown operations',
                                         },
            'flowCacheAlmostFull'      : { 'verbose-name' : 'Cache near full',
                                         },
        },
    },
}


FLOW_CACHE_FORMAT = {
    'flow-cache' : {
        'field-orderings' : {
            'default': ['Idx', 'Appl', 'AppInst', 'VLAN', 'PCP', 'SrcMAC',
                        'DestMAC', 'EtherType', 'SrcIPAddr', 'DstIPAddr',
                        'Protocol', 'TOS', 'SrcPort', 'DstPort',
                        'Source-Switch', 'InputPort', 'Wildcards', 'Action',
                        'Cookie', 'State', 'Time', 'SC'],
            },
        'fields' : {
            'VLAN'           : {'formatter' : fmtcnv.convert_signed_short_for_vlan
                               },
            'Source-Switch'  : {'formatter' : fmtcnv.print_switch_and_alias
                               },
            'InputPort'      : {'formatter' : fmtcnv.decode_openflow_port_source_switch
                               },
            'SrcMAC'         : {'formatter' : fmtcnv.print_host_and_alias
                               },
            'DestMAC'        : {'formatter' : fmtcnv.print_host_and_alias
                               },
            'EtherType'      : {'formatter' : fmtcnv.decode_ether_type
                               },
            'Protocol'       : {'formatter' : fmtcnv.decode_network_protocol
                               },
            'SrcPort'        : {'formatter' : fmtcnv.decode_src_port
                               },
            'DstPort'        : {'formatter' : fmtcnv.decode_dst_port
                               },
            'Cookie'         : {'formatter' : fmtcnv.decode_flow_cookie
                               },
            'Time'           : {'formatter' : fmtcnv.timestamp_to_local_timestr
                               },
           }
        },
}


EV_HIST_TOPOLOGY_FORMAT = {
    'ev-hist-topology-switch' : {
        'field-orderings' : {
            'default' : ['Idx', 'Time', 'Switch' , 'IpAddr', 'Port',
                         'State', 'Action', 'Reason' ],
            },
        'fields' : {
            'Time'    : {
                         'verbose-name' : 'Time'
                        },
            'Switch'  : {'verbose-name' : 'Switch',
                         'formatter'    : fmtcnv.print_switch_and_alias
                        },
            'Port'    : {'verbose-name' : 'Port'
                        },
            'IpAddr'  : {'verbose-name' : 'IpAddr.'
                        },
            'Action'  : {'verbose-name' : 'Action'
                        },
            'Reason'  : {'verbose-name' : 'Reason'
                        },
            'State'   : {'verbose-name' : 'State'
                        },
            }
        },
}



EV_HIST_TOPOLOGY_CLUSTER_FORMAT = {
    'ev-hist-topology-cluster' : {
        'field-orderings' : {
            'default' : ['Idx', 'Time', 'Switch' , 'OldClusterId',
                         'NewClusterId', 'State', 'Action', 'Reason' ],
            },
        'fields' : {
            'Time'    : {
                         'verbose-name' : 'Time'
                        },
            'Switch'  : {'verbose-name' : 'Switch',
                         'formatter'    : fmtcnv.print_switch_and_alias
                        },
            'Action'  : {'verbose-name' : 'Action'
                        },
            'Reason'  : {'verbose-name' : 'Reason'
                        },
            'State'   : {'verbose-name' : 'State'
                        },
            }
        },
}


EV_HIST_TOPOLOGY_LINK_FORMAT = {
    'ev-hist-topology-link' : {
        'field-orderings' : {
            'default' : ['Idx', 'Time', 'Source-Switch' , 'Dest-Switch',
                         'SrcPort', 'DstPort',
                         'SrcPortState', 'DstPortState',
                         'State', 'Action', 'Reason' ],
            },
        'fields' : {
            'Time'          : {
                                'verbose-name' : 'Time'
                              },
            'Source-Switch' : {'verbose-name' : 'Source-Switch',
                               'formatter'    : fmtcnv.print_switch_and_alias
                              },
            'Dest-Switch'   : {'verbose-name' : 'Dest-Switch',
                                'formatter'    : fmtcnv.print_switch_and_alias
                               },
            'SrcPort'       : {'verbose-name' : 'SrcPort',
                               'formatter' : fmtcnv.decode_openflow_port_source_switch
                              },
            'DstPort'       : {'verbose-name' : 'DstPort',
                               'formatter' : fmtcnv.decode_openflow_port_dest_switch
                              },
            'SrcPortState'  : {'verbose-name' : 'SrcPrtSt'
                              },
            'DstPortState'  : {'verbose-name' : 'DstPrtSt'
                              },
            'Action'        : {'verbose-name' : 'Action'
                              },
            'Reason'        : {'verbose-name' : 'Reason'
                              },
            'State'         : {'verbose-name' : 'State'
                              },
            }
        },
}


EV_HIST_ATTACHMENT_POINT_FORMAT = {
    'ev-hist-attachment-point' : {
        'field-orderings' : {
            'default' : ['Idx', 'Time', 'Host', 'Switch' , 'Port', 'VLAN',
                         'Action', 'Reason' ],
            },
        'fields' : {
            'Time_ns'    : {
                             'verbose-name' : 'Time'
                             #'formatter'    : fmtcnv.timestamp_ns_to_local_timestr
                           },
            'Host'       : {'verbose-name' : 'Host',
                           'formatter'    : fmtcnv.print_host_and_alias
                           },
            'Switch'     : {'verbose-name' : 'Switch',
                           'formatter'    : fmtcnv.print_switch_and_alias
                           },
            'Port'       : {'verbose-name' : 'IF',
                            'formatter' : fmtcnv.decode_openflow_port,
                           },
            'VLAN'       : {'verbose-name' : 'VLAN'
                           },
            'Action'     : {'verbose-name' : 'Action'
                           },
            'Reason'     : {'verbose-name' : 'Reason'
                           },
            }
        },
}


EV_HIST_PACKET_IN_FORMAT = {
    'ev-hist-packet-in' : {
        'field-orderings' : {
            'default' : ['Idx', 'Time',
                         'dataLayerSource',
                         'dataLayerDestination',
                         'dataLayerType',
                         'dataLayerVirtualLan',
                         #'dataLayerVirtualLanPriorityCodePoint',
                         'inputSwitch',
                         'inputPort',
                         'networkSource',
                         'networkDestination',
                         #'networkSourceMaskLen',
                         #'networkDestinationMaskLen',
                         'networkProtocol',
                         'networkTypeOfService',
                         'transportSource',
                         'transportDestination',
                         #'Action', 'reason'
                         ],
            },
        'fields' : {
            'Time'                      : {
                                            'verbose-name' : 'Time'
                                            #'formatter'   : fmtcnv.timestamp_ns_to_local_timestr
                                          },
            'wildcards'                 : { 'verbose-name' : 'Wildcards',
                                             'formatter' : fmtcnv.convert_integer_to_bitmask
                                          },
            'dataLayerSource'           : { 'verbose-name' : 'Src MAC',
                                            'formatter' : fmtcnv.print_host_and_alias
                                          },
            'dataLayerDestination'      : { 'verbose-name' : 'Dst MAC',
                                            'formatter' : fmtcnv.print_host_and_alias
                                          },
            'dataLayerType'             : { 'verbose-name' : 'Eth Type',
                                            'formatter' : fmtcnv.decode_ether_type
                                          },
            'dataLayerVirtualLan'       : { 'verbose-name' : 'VLAN',
                                            'formatter' : fmtcnv.convert_signed_short_for_vlan
                                          },
            'dataLayerVirtualLanPriorityCodePoint' : { 'verbose-name' : 'VLAN PCP'
                                          },
            'inputSwitch'               : { 'verbose-name' : 'Switch',
                                             'formatter'  : fmtcnv.print_switch_and_alias
                                          },
            'inputPort'                 : { 'verbose-name' : 'Port',
                                             'formatter' : fmtcnv.decode_openflow_port_inputSwitch
                                          },
            'networkSource'             : { 'verbose-name' : 'Src IP'
                                          },
            'networkDestination'        : { 'verbose-name' : 'Dst IP'
                                          },
            'networkSourceMaskLen'      : { 'verbose-name' : 'Src IP Bits'
                                          },
            'networkDestinationMaskLen' : { 'verbose-name' : 'Dst IP Bits'
                                          },
            'networkProtocol'           : { 'verbose-name' : 'Proto.',
                                            'formatter' : fmtcnv.decode_network_protocol
                                          },
            'networkTypeOfService'      : { 'verbose-name' : 'TOS'
                                          },
            'transportSource'           : { 'verbose-name' : 'NwSPort',
                                            'formatter' : fmtcnv.decode_src_port
                                          },
            'transportDestination'      : { 'verbose-name' : 'NwDPort',
                                            'formatter' : fmtcnv.decode_dst_port
                                          },
            'Action'                    : {'verbose-name' : 'Action'
                                          },
            'Reason'                    : {'verbose-name' : 'Reason'
                                          },
            }
        },
}
"""

def realtime_flow_timeout(i, data=None):
    return ('%s/%s' % 
            (fmtcnv.convert_signed_short_to_unsigned(data['hardTimeout']),
            fmtcnv.convert_signed_short_to_unsigned(data['idleTimeout'])))

REALTIME_FLOW_FORMAT = {
    'realtime_flow' : {
        'field-orderings' : {
            'default' : [
                          'Idx', 'switch', 'cookie', 
                          'byteCount', 'packetCount',
                          'actions', 'dataLayerSource', 'dataLayerDestination',
                          'flow-brief',
                        ],
           'scoped'  : [
                          'Idx', 'cookie', 
                          'byteCount', 'packetCount',
                          'actions', 'dataLayerSource', 'dataLayerDestination',
                          'flow-brief',

                        ],
            'brief'   : [ 'Idx', 'switch', 'cookie', 'dataLayerSource', 'dataLayerDestination',
                          'networkSource', 'networkDestination', 'networkProtocol',
                          'transportSource', 'transportDestination'],
            'default' : [ 'Idx', 'switch', 'byteCount', 'packetCount', 'durationSeconds',
                          'cookie', 'inputPort', 'dataLayerSource', 'dataLayerDestination',
                          'dataLayerType', 'networkSource', 'networkDestination', 'networkProtocol',
                          'transportSource', 'transportDestination'],
            'scoped'  : [ 'Idx', 'switch', 'byteCount', 'packetCount', 'durationSeconds',
                          'cookie', 'inputPort', 'dataLayerSource', 'dataLayerDestination',
                          'dataLayerType', 'networkSource', 'networkDestination', 'networkProtocol',
                          'transportSource', 'transportDestination'],
            'details' : [ 'Idx', 'switch', 'byteCount', 'packetCount', 'durationSeconds',
                          'cookie', 'hardTimeout', 'idleTimeout', 'priority', 'tableId', 'inputPort',
                          'dataLayerSource', 'dataLayerDestination', 'dataLayerType', 'dataLayerVirtualLan',
                          'dataLayerVirtualLanPriorityCodePoint', 'networkSource', 'networkDestination',
                          'networkProtocol', 'transportSource', 'transportDestination',
                          'networkTypeOfService', 'actions'],
            'vns_flow': [ 'Idx', 'dataLayerSource', 'dataLayerDestination', 'dataLayerVirtualLan', 'dataLayerType'],
            'summary' : [ 'Idx', 'vnsName', 'vnsFlowCnt', 'flowEntryCnt'],
            },
        'fields': {
            'switch'           : { 'verbose-name' : 'Switch',
                                   'formatter' : fmtcnv.replace_switch_with_alias
                                 },
            'flow-brief'       : {
                                    'verbose-name' : 'Match',
                                    'formatter' : fmtcnv.realtime_flow_brief,
                                 },
            'flow-timeout'     : {
                                    'verbose-name' : 'H/I',
                                    'formatter' : realtime_flow_timeout,
                                 },
            'byteCount'        : { 'verbose-name': 'Bytes',
                                   'primary_key':True
                                 },
            'packetCount'      : { 'verbose-name' : 'Packets'
                                 },
            'cookie'           : { 'verbose-name' : 'Author',
                                   'formatter' : fmtcnv.decode_flow_cookie,
                                 },
            'durationSeconds'  : { 'verbose-name' : 'Dur(s)'
                                 },
            'durationNanoseconds' : { 'verbose-name' : 'Dur(ns)'
                                 },
            'hardTimeout'      : { 'verbose-name' : 'Hard Timeout',
                                   'formatter' : fmtcnv.convert_signed_short_to_unsigned
                                 },
            'idleTimeout'      : { 'verbose-name' : 'Idle Timeout',
                                   'formatter' : fmtcnv.convert_signed_short_to_unsigned
                                 },
            'length'           : { 'verbose-name' : 'Length'
             },
            'priority'         : { 'verbose-name' : 'Priority',
                                   'formatter' : fmtcnv.convert_signed_short_to_unsigned
                                 },
            'tableId'          : { 'verbose-name' : 'Table' },
            'wildcards'        : { 'verbose-name' : 'Wildcards',
                                    'formatter' : fmtcnv.convert_integer_to_bitmask
                                 },
            'dataLayerSource'  : { 'verbose-name' : 'Src MAC',
                                   'formatter' : fmtcnv.replace_host_with_alias
                                 },
            'dataLayerDestination' : { 'verbose-name' : 'Dst MAC',
                                       'formatter' : fmtcnv.replace_host_with_alias
                                 },
            'dataLayerType'    : { 'verbose-name' : 'Ether Type',
                                   'formatter' : fmtcnv.decode_ether_type
                                 },
            'dataLayerVirtualLan' : { 'verbose-name' : 'VLAN ID',
                                      'formatter' : fmtcnv.convert_signed_short_to_unsigned
                                 },
            'dataLayerVirtualLanPriorityCodePoint' : { 'verbose-name' : 'VLAN PCP'
                                 },
            'inputPort'        : { 'verbose-name' : 'In Port',
                                    'formatter' : fmtcnv.decode_openflow_port
                                 },
            'networkSource'    : { 'verbose-name' : 'Src IP'
                                 },
            'networkDestination' : { 'verbose-name' : 'Dst IP'
                                 },
            'networkSourceMaskLen' : { 'verbose-name' : 'Src IP Bits'
                                 },
            'networkDestinationMaskLen' : { 'verbose-name' : 'Dst IP Bits'
                                 },
            'networkProtocol'  : { 'verbose-name' : 'Protocol',
                                    'formatter' : fmtcnv.decode_network_protocol
                                 },
            'networkTypeOfService' : { 'verbose-name' : 'TOS Bits'
                                 },
            'transportSource'  : { 'verbose-name' : 'Src Port',
                                   'formatter' : fmtcnv.decode_src_port
                                 },
            'transportDestination' : { 'verbose-name' : 'Dst Port',
                                       'formatter' : fmtcnv.decode_dst_port
                                 },
            'actions'          : { 'verbose-name' : 'Actions',
                                    'formatter' : fmtcnv.decode_actions
                                 },
            'vnsName'          : { 'verbose-name' : 'VNS'
                                 },
            'vnsFlowCnt'       : { 'verbose-name' : 'Flows'
                                 },
            'flowEntryCnt'     : { 'verbose-name' : 'Flow-Entries'
                                 },
            }
        },
}

REALTIME_TABLE_IP_FLOW_FORMAT = {
    'realtime_table_ip_flow' : {
        'field-orderings' : {
            'default' : [
                          'Idx',  'cookie', 
                          'byteCount', 'packetCount','priority',
                          'networkDestination',
                          'flow-brief','actions'
                        ],
            'scoped'  : [
                          'Idx',  'byteCount', 'packetCount', 'durationSeconds',
                          'cookie','priority',
                          'networkDestination', 'actions'
                        ],
            'brief'   : [ 'Idx',  'byteCount', 'packetCount', 'durationSeconds',
                          'cookie','priority',
                          'networkDestination', 'actions'
                          ],
            'default' : [ 'Idx', 'byteCount', 'packetCount', 'durationSeconds',
                          'cookie','priority',
                          'networkDestination', 'actions'
                          ],
            'scoped'  : [ 'Idx', 'byteCount', 'packetCount', 'durationSeconds',
                          'cookie','priority',
                          'networkDestination', 'actions'
                          ],
            'details' : [ 'Idx', 'byteCount', 'packetCount', 'durationSeconds',
                          'cookie','priority'
                          'networkDestination', 'actions'
                          ],
            #'vns_flow': [ 'Idx', 'dataLayerSource', 'dataLayerDestination', 'dataLayerVirtualLan', 'dataLayerType'],
            #'summary' : [ 'Idx', 'vnsName', 'vnsFlowCnt', 'flowEntryCnt'],
            },
        'fields': {
            'switch'           : { 'verbose-name' : 'Switch',
                                   'formatter' : fmtcnv.replace_switch_with_alias
                                 },
            'flow-brief'       : {
                                    'verbose-name' : 'Match',
                                    'formatter' : fmtcnv.realtime_flow_brief,
                                 },
            'flow-timeout'     : {
                                    'verbose-name' : 'H/I',
                                    'formatter' : realtime_flow_timeout,
                                 },
            'byteCount'        : { 'verbose-name': 'Bytes',
                                   'primary_key':True
                                 },
            'packetCount'      : { 'verbose-name' : 'Packets'
                                 },
            'cookie'           : { 'verbose-name' : 'Cookie'#,
                                   #'formatter' : fmtcnv.decode_flow_cookie,
                                 },
            'durationSeconds'  : { 'verbose-name' : 'Dur(s)'
                                 },
            'durationNanoseconds' : { 'verbose-name' : 'Dur(ns)'
                                 },
            'hardTimeout'      : { 'verbose-name' : 'Hard Timeout',
                                   'formatter' : fmtcnv.convert_signed_short_to_unsigned
                                 },
            'idleTimeout'      : { 'verbose-name' : 'Idle Timeout',
                                   'formatter' : fmtcnv.convert_signed_short_to_unsigned
                                 },
            'length'           : { 'verbose-name' : 'Length'
                                 },
            'priority'         : { 'verbose-name' : 'Priority',
                                   'formatter' : fmtcnv.convert_signed_short_to_unsigned
                                 },
            'tableId'          : { 'verbose-name' : 'Table' },
            'wildcards'        : { 'verbose-name' : 'Wildcards',
                                    'formatter' : fmtcnv.convert_integer_to_bitmask
                                 },
            'dataLayerSource'  : { 'verbose-name' : 'Src MAC',
                                   'formatter' : fmtcnv.replace_host_with_alias
                                 },
            'dataLayerDestination' : { 'verbose-name' : 'Dst MAC',
                                       'formatter' : fmtcnv.replace_host_with_alias
                                 },
            'dataLayerType'    : { 'verbose-name' : 'Ether Type',
                                   'formatter' : fmtcnv.decode_ether_type
                                 },
            'dataLayerVirtualLan' : { 'verbose-name' : 'VLAN ID',
                                      'formatter' : fmtcnv.convert_signed_short_to_unsigned
                                 },
            'dataLayerVirtualLanPriorityCodePoint' : { 'verbose-name' : 'VLAN PCP'
                                 },
            'inputPort'        : { 'verbose-name' : 'In Port',
                                    'formatter' : fmtcnv.decode_openflow_port
                                 },
            'networkSource'    : { 'verbose-name' : 'Src IP'
                                 },
            'networkDestination' : { 'verbose-name' : 'Dst IP'
                                 },
            'networkSourceMaskLen' : { 'verbose-name' : 'Src IP Bits'
                                 },
            'networkDestinationMaskLen' : { 'verbose-name' : 'Dst IP Bits'
                                 },
            'networkProtocol'  : { 'verbose-name' : 'Protocol',
                                    'formatter' : fmtcnv.decode_network_protocol
                                 },
            'networkTypeOfService' : { 'verbose-name' : 'TOS Bits'
                                 },
            'transportSource'  : { 'verbose-name' : 'Src Port',
                                   'formatter' : fmtcnv.decode_src_port
                                 },
            'transportDestination' : { 'verbose-name' : 'Dst Port',
                                       'formatter' : fmtcnv.decode_dst_port
                                 },
            'actions'          : { 'verbose-name' : 'Instructions',
                                 },
            'vnsName'          : { 'verbose-name' : 'VNS'
                                 },
            'vnsFlowCnt'       : { 'verbose-name' : 'Flows'
                                 },
            'flowEntryCnt'     : { 'verbose-name' : 'Flow-Entries'
                                 },
            }
        },
}



REALTIME_TABLE_MPLS_FLOW_FORMAT = {
    'realtime_table_mpls_flow' : {
        'field-orderings' : {
            'default' : [
                          'Idx',   'priority',
                          'byteCount', 'packetCount',
                          'mplsLabel','mplsBos','mplsTc',
                          'flow-brief', 'actions'
                        ],
            'scoped'  : [
                          'Idx', 'byteCount', 'packetCount', 'durationSeconds',
                            'priority',
                           'mplsLabel','mplsBos','mplsTc','actions'
                        ],
            'brief'   : [ 'Idx', 'byteCount', 'packetCount', 'durationSeconds',
                           'priority',
                          'mplsLabel','mplsBos','mplsTc','actions'
                          ],
            'default' : [ 'Idx',  'byteCount', 'packetCount', 'durationSeconds',
                           'priority',
                           'mplsLabel','mplsBos','mplsTc','actions'
                          ],
            'scoped'  : [ 'Idx',  'byteCount', 'packetCount', 'durationSeconds',
                           'priority',
                           'mplsLabel','mplsBos','mplsTc','actions'
                          ],
            'details' : [ 'Idx', 'byteCount', 'packetCount', 'durationSeconds',
                           'priority',
                         'mplsLabel','mplsBos','mplsTc','actions'
                          ],
            #'vns_flow': [ 'Idx', 'dataLayerSource', 'dataLayerDestination', 'dataLayerVirtualLan', 'dataLayerType'],
            #'summary' : [ 'Idx', 'vnsName', 'vnsFlowCnt', 'flowEntryCnt'],
            },
        'fields': {
            'switch'           : { 'verbose-name' : 'Switch',
                                   'formatter' : fmtcnv.replace_switch_with_alias
                                 },
            'flow-brief'       : {
                                    'verbose-name' : 'Match',
                                    'formatter' : fmtcnv.realtime_flow_brief,
                                 },
            'flow-timeout'     : {
                                    'verbose-name' : 'H/I',
                                    'formatter' : realtime_flow_timeout,
                                 },
            'byteCount'        : { 'verbose-name': 'Bytes',
                                   'primary_key':True
                                 },
            'packetCount'      : { 'verbose-name' : 'Packets'
                                 },
            'cookie'           : { 'verbose-name' : 'Cookie',
                                   #'formatter' : fmtcnv.decode_flow_cookie,
                                 },
            'durationSeconds'  : { 'verbose-name' : 'Dur(s)'
                                 },
            'durationNanoseconds' : { 'verbose-name' : 'Dur(ns)'
                                 },
            'hardTimeout'      : { 'verbose-name' : 'Hard Timeout',
                                   'formatter' : fmtcnv.convert_signed_short_to_unsigned
                                 },
            'idleTimeout'      : { 'verbose-name' : 'Idle Timeout',
                                   'formatter' : fmtcnv.convert_signed_short_to_unsigned
                                 },
            'length'           : { 'verbose-name' : 'Length'
                                 },
            'priority'         : { 'verbose-name' : 'Priority',
                                   'formatter' : fmtcnv.convert_signed_short_to_unsigned
                                 },
            'tableId'          : { 'verbose-name' : 'Table' },
            'wildcards'        : { 'verbose-name' : 'Wildcards',
                                    'formatter' : fmtcnv.convert_integer_to_bitmask
                                 },
            'dataLayerSource'  : { 'verbose-name' : 'Src MAC',
                                   'formatter' : fmtcnv.replace_host_with_alias
                                 },
            'dataLayerDestination' : { 'verbose-name' : 'Dst MAC',
                                       'formatter' : fmtcnv.replace_host_with_alias
                                 },
            'dataLayerType'    : { 'verbose-name' : 'Ether Type',
                                   'formatter' : fmtcnv.decode_ether_type
                                 },
            'dataLayerVirtualLan' : { 'verbose-name' : 'VLAN ID',
                                      'formatter' : fmtcnv.convert_signed_short_to_unsigned
                                 },
            'dataLayerVirtualLanPriorityCodePoint' : { 'verbose-name' : 'VLAN PCP'
                                 },
            'inputPort'        : { 'verbose-name' : 'In Port',
                                    'formatter' : fmtcnv.decode_openflow_port
                                 },
            'networkSource'    : { 'verbose-name' : 'Src IP'
                                 },
            'networkDestination' : { 'verbose-name' : 'Dst IP'
                                 },
            'networkSourceMaskLen' : { 'verbose-name' : 'Src IP Bits'
                                 },
            'networkDestinationMaskLen' : { 'verbose-name' : 'Dst IP Bits'
                                 },
            'networkProtocol'  : { 'verbose-name' : 'Protocol',
                                    'formatter' : fmtcnv.decode_network_protocol
                                 },
            'networkTypeOfService' : { 'verbose-name' : 'TOS Bits'
                                 },
            'transportSource'  : { 'verbose-name' : 'Src Port',
                                   'formatter' : fmtcnv.decode_src_port
                                 },
            'transportDestination' : { 'verbose-name' : 'Dst Port',
                                       'formatter' : fmtcnv.decode_dst_port
                                 },
            'actions'          : { 'verbose-name' : 'Instructions'
                                 },
            'vnsName'          : { 'verbose-name' : 'VNS'
                                 },
            'vnsFlowCnt'       : { 'verbose-name' : 'Flows'
                                 },
            'flowEntryCnt'     : { 'verbose-name' : 'Flow-Entries'
                                 },
            'mplsTc'           : { 'verbose-name' : 'MPLS TC'
                                 },
            'mplsLabel'        : { 'verbose-name' : 'MPLS Label'
                                 },
            'mplsBos'          : { 'verbose-name' : 'MPLS BOS'
                                 },
            }
        },
}




REALTIME_TABLE_ACL_FLOW_FORMAT = {
    'realtime_table_acl_flow' : {
        'field-orderings' : {
            'default' : [ 'Idx','byteCount', 'packetCount', 'durationSeconds',
                          'cookie','priority', 'inputPort', 'dataLayerSource', 'dataLayerDestination',
                          'dataLayerType', 'networkSource', 'networkDestination', 'networkProtocol',
                          'transportSource', 'transportDestination''mplsLabel','actions'],
            'scoped'  : [ 'Idx','byteCount', 'packetCount', 'durationSeconds',
                          'cookie','priority', 'inputPort', 'dataLayerSource', 'dataLayerDestination',
                          'dataLayerType', 'networkSource', 'networkDestination', 'networkProtocol',
                          'transportSource', 'transportDestination','actions'],
            },
        'fields': {
            'switch'           : { 'verbose-name' : 'Switch',
                                   'formatter' : fmtcnv.replace_switch_with_alias
                                 },
            'flow-brief'       : {
                                    'verbose-name' : 'Match',
                                    'formatter' : fmtcnv.realtime_flow_brief,
                                 },
            'flow-timeout'     : {
                                    'verbose-name' : 'H/I',
                                    'formatter' : realtime_flow_timeout,
                                 },
            'byteCount'        : { 'verbose-name': 'Bytes',
                                   'primary_key':True
                                 },
            'packetCount'      : { 'verbose-name' : 'Packets'
                                 },
            'cookie'           : { 'verbose-name' : 'Cookie',
                                   #'formatter' : fmtcnv.decode_flow_cookie,
                                 },
            'durationSeconds'  : { 'verbose-name' : 'Dur(s)'
                                 },
            'durationNanoseconds' : { 'verbose-name' : 'Dur(ns)'
                                 },
            'hardTimeout'      : { 'verbose-name' : 'Hard Timeout',
                                   'formatter' : fmtcnv.convert_signed_short_to_unsigned
                                 },
            'idleTimeout'      : { 'verbose-name' : 'Idle Timeout',
                                   'formatter' : fmtcnv.convert_signed_short_to_unsigned
                                 },
            'length'           : { 'verbose-name' : 'Length'
             },
            'priority'         : { 'verbose-name' : 'Priority',
                                   'formatter' : fmtcnv.convert_signed_short_to_unsigned
                                 },
            'tableId'          : { 'verbose-name' : 'Table' },
            'wildcards'        : { 'verbose-name' : 'Wildcards',
                                    'formatter' : fmtcnv.convert_integer_to_bitmask
                                 },
            'dataLayerSource'  : { 'verbose-name' : 'Src MAC',
                                   'formatter' : fmtcnv.replace_host_with_alias
                                 },
            'dataLayerDestination' : { 'verbose-name' : 'Dst MAC',
                                       'formatter' : fmtcnv.replace_host_with_alias
                                 },
            'dataLayerType'    : { 'verbose-name' : 'EthType',
                                   'formatter' : fmtcnv.decode_ether_type
                                 },
            'dataLayerVirtualLan' : { 'verbose-name' : 'VLAN ID',
                                      'formatter' : fmtcnv.convert_signed_short_to_unsigned
                                 },
            'dataLayerVirtualLanPriorityCodePoint' : { 'verbose-name' : 'VLAN PCP'
                                 },
            'inputPort'        : { 'verbose-name' : 'In Port',
                                    'formatter' : fmtcnv.decode_openflow_port
                                 },
            'networkSource'    : { 'verbose-name' : 'Src IP'
                                 },
            'networkDestination' : { 'verbose-name' : 'Dst IP'
                                 },
            'networkSourceMaskLen' : { 'verbose-name' : 'Src IP Bits'
                                 },
            'networkDestinationMaskLen' : { 'verbose-name' : 'Dst IP Bits'
                                 },
            'networkProtocol'  : { 'verbose-name' : 'Protocol',
                                    'formatter' : fmtcnv.decode_network_protocol
                                 },
            'networkTypeOfService' : { 'verbose-name' : 'TOS Bits'
                                 },
            'transportSource'  : { 'verbose-name' : 'Src Port',
                                   'formatter' : fmtcnv.decode_src_port
                                 },
            'transportDestination' : { 'verbose-name' : 'Dst Port',
                                       'formatter' : fmtcnv.decode_dst_port
                                 },
            'actions'          : { 'verbose-name' : 'Instructions'
                                 },
            'vnsName'          : { 'verbose-name' : 'VNS'
                                 },
            'vnsFlowCnt'       : { 'verbose-name' : 'Flows'
                                 },
            'flowEntryCnt'     : { 'verbose-name' : 'Flow-Entries'
                                 },
            'mplsTc'           : { 'verbose-name' : 'MPLS TC'
                                 },
            'mplsLabel'        : { 'verbose-name' : 'MPLS LABEL'
                                 },
            'mplsBos'          : { 'verbose-name' : 'MPLS BOS'
                                 },
            }
        },
}


REALTIME_AGGREGATE_FORMAT = {
    'realtime_aggregate' : {
        'field-orderings' : {
            'default' : [ 'Idx', 'switch', 'flowCount', 'byteCount', 'packetCount' ],
            'scoped'  : [ 'Idx', 'flowCount', 'byteCount', 'packetCount' ],
            'brief'   : [ 'Idx', 'switch', 'flowCount', 'byteCount', 'packetCount' ],
            },
        'fields': {
            'switch'      : { 'verbose-name' : 'Switch',
                               'formatter' : fmtcnv.replace_switch_with_alias
                            },
            'length'      : { 'verbose-name': 'Length'
                            },
            'flowCount'   : { 'verbose-name' : 'Flows'
                            },
            'byteCount'   : { 'verbose-name' : 'Bytes'
                            },
            'packetCount' : { 'verbose-name' : 'Packets'
                            },
            }
        },
}


REALTIME_DESC_FORMAT = {
    'realtime_desc' : {
        'field-orderings' : {
            'default' : [ 'Idx', 'switch', 'serialNumber', 'manufacturerDescription',
                          'hardwareDescription', 'datapathDescription',
                          'softwareDescription' ],
            'scoped'  : [ 'Idx', 'serialNumber', 'manufacturerDescription',
                          'hardwareDescription', 'datapathDescription',
                          'softwareDescription' ],
            'brief'   : [ 'Idx', 'switch', 'serialNumber', 'manufacturerDescription',
                          'hardwareDescription', 'datapathDescription',
                          'softwareDescription' ],
            },
        'fields': {
            'switch'                  : { 'verbose-name' : 'Switch',
                                          'formatter' : fmtcnv.replace_switch_with_alias
                                        },
            'softwareDescription'     : { 'verbose-name': 'SW Version'
                                        },
            'datapathDescription'     : { 'verbose-name' : 'Model'
                                        },
            'hardwareDescription'     : { 'verbose-name' : 'Make'
                                        },
            'manufacturerDescription' : { 'verbose-name' : 'Vendor'
                                        },
            'serialNumber'            : { 'verbose-name' : 'Serial #'
                                        },
            }
        },
}


REALTIME_PORT_FORMAT = {
    'realtime_port' : {
        'field-orderings' : {
            'default' : [ 'Idx', 'switch', 'portNumber','portStatus', 'receiveBytes',
                          'receivePackets', 'receiveErrors', 'receiveDropped',
                          'receiveCRCErrors', 'receiveOverrunErrors',
                          'receiveFrameErrors', 'transmitBytes',
                          'transmitPackets', 'transmitErrors',
                          'transmitDropped', 'collisions' ],
            'details' : [ 'Idx', 'switch', 'portNumber','portStatus', 'receiveBytes',
                          'receivePackets', 'receiveErrors', 'receiveDropped',
                          'receiveCRCErrors', 'receiveOverrunErrors',
                          'receiveFrameErrors', 'transmitBytes',
                          'transmitPackets', 'transmitErrors',
                          'transmitDropped', 'collisions' ],
            'scoped'  : [ 'Idx', 'portNumber','portStatus', 'receiveBytes',
                          'receivePackets', 'receiveErrors', 'receiveDropped',
                          'receiveCRCErrors', 'receiveOverrunErrors',
                          'receiveFrameErrors', 'transmitBytes',
                          'transmitPackets', 'transmitErrors',
                          'transmitDropped', 'collisions' ],
            'brief' :  [ 'Idx', 'switch', 'portNumber','portStatus',
                         'receiveBytes', 'receivePackets', 'receiveErrors',
                         'transmitBytes', 'transmitPackets', 'transmitErrors',
                       ]
            },
        'fields': {
            'switch'               : { 'verbose-name' : 'Switch',
                                        'formatter' : fmtcnv.replace_switch_with_alias
                                     },
            'receiveBytes'         : { 'verbose-name' : 'Rcv Bytes',
                                       'formatter' : fmtcnv.decode_port_counter
                                     },
            'receivePackets'       : { 'verbose-name' : 'Rcv Pkts',
                                        'formatter' : fmtcnv.decode_port_counter
                                      },
            'receiveErrors'        : { 'verbose-name' : 'Rcv Errs',
                                       'formatter' : fmtcnv.decode_port_counter
                                     },
            'receiveDropped'       : { 'verbose-name' : 'Rcv Dropped',
                                       'formatter' : fmtcnv.decode_port_counter
                                     },
            'receiveCRCErrors'     : { 'verbose-name' : 'Rcv CRC',
                                        'formatter' : fmtcnv.decode_port_counter
                                      },
            'receiveOverrunErrors' : { 'verbose-name' : 'Rcv Overruns',
                                        'formatter' : fmtcnv.decode_port_counter
                                      },
            'receiveFrameErrors'   : { 'verbose-name' : 'Rcv Frame Errs',
                                       'formatter' : fmtcnv.decode_port_counter
                                     },
            'transmitBytes'        : { 'verbose-name' : 'Xmit Bytes',
                                       'formatter' : fmtcnv.decode_port_counter
                                     },
            'transmitPackets'      : { 'verbose-name' : 'Xmit Pkts',
                                       'formatter' : fmtcnv.decode_port_counter
                                     },
            'transmitErrors'       : { 'verbose-name' : 'Xmit Errs',
                                       'formatter' : fmtcnv.decode_port_counter
                                     },
            'transmitDropped'      : { 'verbose-name' : 'Xmit Dropped',
                                       'formatter' : fmtcnv.decode_port_counter
                                     },
            'collisions'           : { 'verbose-name' : 'Collisions',
                                       'formatter' : fmtcnv.decode_port_counter
                                     },
            'portNumber'           : { 'verbose-name' : 'OF Port #',
                                        'formatter' : fmtcnv.decode_openflow_port
                                     },
            'length'               : { 'verbose-name' : 'Length'
                                     },
            'portStatus'           : { 'verbose-name' : 'Status'
                                     },
            }
                       
        },
}


REALTIME_GROUP_FORMAT = {
    'realtime_group' : {
        'field-orderings' : {
            'default' : [ 'Idx', 'grouptype','groupid' , 'totalpktcnt', 'totalbytecnt',
                          'bucketpktcnt', 'bucketbytecnt', 
                          'setsrcmac', 'setdstmac',
                          'pushMplslabel', 'setBos',
                          'COPY_TTL_OUT','DEC_MPLS_TTL','outport','goToGroup',
                           ],
            'scoped' : [ 'Idx', 'grouptype','groupid','totalpktcnt', 'totalbytecnt',
                          'bucketpktcnt', 'bucketbytecnt', 
                          'setsrcmac', 'setdstmac',
                          'pushMplsLabel','setBos',
                          'COPY_TTL_OUT','DEC_MPLS_TTL','outport','goToGroup',
                          ],
            },
        'fields': {
            'groupid'               : { 'verbose-name' : 'Group Id',
                                     },
            'grouptype'               : { 'verbose-name' : 'Group Type',
                                     },
            'totalpktcnt'         : { 'verbose-name' : 'Pkts',
                                       'formatter' : fmtcnv.decode_port_counter
                                     },
            'totalbytecnt'       : { 'verbose-name' : 'Bytes',
                                        'formatter' : fmtcnv.decode_port_counter
                                      },
            'bucketpktcnt'        : { 'verbose-name' : 'Bucket Pkts',
                                       'formatter' : fmtcnv.decode_port_counter
                                     },
            'bucketbytecnt'       : { 'verbose-name' : 'Bucket Bytes',
                                       'formatter' : fmtcnv.decode_port_counter
                                     },
            'setsrcmac'           : { 'verbose-name' : 'Set Src Mac',
                                      },
            'setdstmac'           : { 'verbose-name' : 'Set Dst Mac',
                                      },
            'pushMplsLabel'            : { 'verbose-name' : 'Push Mpls',
                                     },
            'setBos'              : { 'verbose-name' : 'Set Bos',
                                     },
            'outport'             : { 'verbose-name' : 'Outport',
                                     },
            'goToGroup'           : { 'verbose-name' : 'Group',
                                    },
            'COPY_TTL_OUT'           : { 'verbose-name' : 'COPY TTL',
                                    },
            'DEC_MPLS_TTL'           : { 'verbose-name' : 'Dec Mpls TTL',
                                    },
            }
        },
}


REALTIME_TABLE_FORMAT = {
    'realtime_table' : {
        'field-orderings' : {
            'default' : [ 'Idx', 'switch', 'name', 'tableId', 'wildcards',
                          'maximumEntries', 'lookupCount', 'matchedCount', 'activeCount' ],
            'scoped'  : [ 'Idx','name', 'tableId', 'wildcards',
                          'maximumEntries', 'lookupCount', 'matchedCount', 'activeCount' ],
            'brief'   : [ 'Idx', 'switch', 'name', 'tableId', 'wildcards',
                          'maximumEntries', 'lookupCount', 'matchedCount', 'activeCount' ],
            },
        'fields': {
            'switch'         : { 'verbose-name' : 'Switch',
                                 'formatter' : fmtcnv.replace_switch_with_alias
                               },
            'wildcards'      : { 'verbose-name': 'Wildcards'
                               },
            'matchedCount'   : { 'verbose-name' : '# Matched'
                               },
            'maximumEntries' : { 'verbose-name' : 'Maximum Entries'
                               },
            'name'           : { 'verbose-name' : 'Name'
                               },
            'activeCount'    : { 'verbose-name' : '# Active'
                               },
            'length'         : { 'verbose-name' : 'Length'
                               },
            'tableId'        : { 'verbose-name' : 'Table ID'
                               },
            'lookupCount'    : { 'verbose-name' : '# Lookups'
                               },
            }
        },
}

SHOW_TUNNEL_FORMAT = {
    'show_tunnel' : {
        'field-orderings' : {
            'default' : [ 'Idx', 'tunnelId', 'policies','tunnelPath','labelStack',],
            'details' : [ 'Idx', 'tunnelId', 'policies','tunnelPath','labelStack', 'dpidGroup',],
            },
        'fields': {
            'tunnelId'         : { 'verbose-name' : 'Id',
                               },
            'dpidGroup'         : { 'verbose-name' : 'Dpid(Node Id)/Group',
                               },
            'labelStack'         : { 'verbose-name' : 'Label Stack [Outer-->Inner]',
                               },
            'tunnelPath'         : { 'verbose-name' : 'Tunnel Path [Head-->Tail]',
                               },
                   }
        },
}

SHOW_POLICY_FORMAT = {
    'show_policy' : {
        'field-orderings' : {
            'default' : [ 'Idx', 'policyId', 'policyType','priority','dstMacAddress','srcMacAddress',
                        'etherType','dstIpAddress' ,'ipProtocolNumber','srcIpAddress', 'dstTcpPortNumber',
                        'srcTcpPortNumber','tunnelId'
                         ]
            },
            'fields': {
                       'policyId'             : { 'verbose-name' : 'Policy Id',
                                               },
                       'policyType'         : { 'verbose-name': 'Policy Type',
                                               },
                       'dstMacAddress'         : { 'verbose-name': 'Dst Mac',
                                               },
                       'srcMacAddress'         : { 'verbose-name': 'Src Mac',
                                               },
                       'dstIpAddress'         : { 'verbose-name': 'Dst IP',
                                               },
                       'srcIpAddress'         : { 'verbose-name': 'Src IP',
                                               },
                       'dstTcpPortNumber'         : { 'verbose-name': 'Dst TcpPort',
                                               },
                       'srcTcpPortNumber'         : { 'verbose-name': 'Src TcpPort',
                                               },
                       'etherType'         : { 'verbose-name': 'Ether Type',
                                               },

                       'ipProtocolNumber'         : { 'verbose-name': 'IP Protocol',
                                               },
                       'tunnelId'                : { 'verbose-name': 'Tunnel Used',
                                               },
                       
                                               }
        },
}



REALTIME_FEATURES_FORMAT = {
    'realtime_features' : {
        'field-orderings' : {
            'default' : [ 'Idx', 'switch', 'portNumber', 'name', 'hardwareAddress',
                          'config', 'state', 'currentFeatures', 'advertisedFeatures',
                          'supportedFeatures', 'peerFeatures'],
            'scoped'  : [ 'Idx', 'portNumber', 'name', 'hardwareAddress',
                          'config', 'state', 'currentFeatures', 'advertisedFeatures',
                          'supportedFeatures', 'peerFeatures'],
            'brief' : [ 'Idx', 'switch', 'portNumber', 'name',
                          'currentFeatures', 'advertisedFeatures',
                          'supportedFeatures', 'peerFeatures'],
            },
        'fields': {
            'switch'             : { 'verbose-name' : 'Switch',
                                     'formatter' : fmtcnv.replace_switch_with_alias
                                   },
            'portNumber'         : { 'verbose-name': 'OF Port #',
                                     'formatter' : fmtcnv.decode_openflow_port
                                   },
            'hardwareAddress'    : { 'verbose-name' : 'HW Mac Address'
                                   },
            'name'               : { 'verbose-name' : 'Name'
                                   },
            'config'             : { 'verbose-name' : 'Config',
                                     'formatter' : fmtcnv.decode_port_config
                                   },
            'state'              : { 'verbose-name' : 'State',
                                     'formatter' : fmtcnv.decode_port_up_down
                                   },
            'stp-state'          : { 'verbose-name' : 'STP State',
                                     'formatter' : fmtcnv.decode_port_stp_state
                                   },
            'currentFeatures'    : { 'verbose-name' : 'Current',
                                     'formatter' : fmtcnv.decode_port_features
                                   },
            'advertisedFeatures' : { 'verbose-name' : 'Advertised',
                                     'formatter' : fmtcnv.decode_port_features
                                   },
            'supportedFeatures'  : { 'verbose-name' : 'Supported',
                                     'formatter' : fmtcnv.decode_port_features
                                   },
            'peerFeatures'       : { 'verbose-name' : 'Peer',
                                     'formatter' : fmtcnv.decode_port_features
                                   },
            }
        },
}


REALTIME_QUEUE_FORMAT = {
    'realtime_queue' : {
        'field-orderings' : {
            'default' : [ 'Idx', 'switch', 'portNumber',
                          'queueId', 'transmitBytes', 'transmitPackets', 'transmitErrors',
                        ],
            'scoped'  : [ 'Idx', 'portNumber',
                          'queueId', 'transmitBytes', 'transmitPackets', 'transmitErrors',
                        ],
            'brief'   : [ 'Idx', 'switch', 'portNumber',
                          'queueId', 'transmitBytes', 'transmitPackets', 'transmitErrors',
                        ],
        },

        'fields' : {
            'switch'             : { 'verbose-name' : 'Switch',
                                     'formatter' : fmtcnv.replace_switch_with_alias
                                   },
            'portNumber'         : { 'verbose-name': 'OF Port #',
                                     'formatter' : fmtcnv.decode_openflow_port
                                   },
            'queueId'            : { 'verbose-name' : 'Queue ID',
                                   },
            'transmitBytes'      : { 'verbose-name' : 'Xmit Bytes'
                                   },
            'transmitPackets'    : { 'verbose-name' : 'Xmit Pkts'
                                   },
            'transmitErrors'     : { 'verbose-name' : 'Xmit Errors'
                                   },
        }
    },
}




ROUTER_FORMAT = {
    'router'     : {
        'field-orderings' : {
            'default' : ['Idx','dpid','name', 'routerIP','routerMac','isEdgeRouter','nodeSId'
                         ],
            },
        'fields': {
            'dpid'               : { 'verbose-name' : 'Router DPID',
                                    #'formatter' : fmtcnv.eprint_switch_and_alias,
                                        },
            'name'                 : { 'verbose-name' : 'Router Name',
                                       #'formatter' : fmtcnv.decode_port_counter
                                       },
            'routerIP'                   : { 'verbose-name' : 'Router IP',
                                      },
            'routerMac'                  : { 'verbose-name' : 'Router Mac',
                                       #'formatter' : fmtcnv.decode_port_counter
                                     },
            'isEdgeRouter'                  : { 'verbose-name' : 'Edge Router',
                                       #'formatter' : fmtcnv.decode_port_counter
                                     },
            'nodeSId'                     : { 'verbose-name' : 'Node SId',
                                       #'formatter' : fmtcnv.decode_port_counter
                                     },
            }
        },
}

#adjacency
ROUTER_ADJACENCY_FORMAT= {
    'router_adjacency' : {
        'field-orderings' : {
            'default' : [ 'Idx', 'adjacencySid', 'ports'],
            'scoped'  : [ 'Idx', 'adjacencySid', 'ports'],
            },
        'fields': {
            'adjacencySid'               : { 'verbose-name' : 'Adjacency Sid(s)',
                                     },

        },

        },
}

ROUTER_PORT_FORMAT = {
    'router_port' : {
        'field-orderings' : {
            'default' : [ 'Idx', 'name', 'portNo', 'subnetIp','adjacency'],
            'scoped' : [ 'Idx', 'name', 'portNo', 'subnetIp','adjacency'],
            },
        'fields': {
            'adjacency'               : { 'verbose-name' : 'Adjacency Sid(s)',
                                     },
            'portNo'               : { 'verbose-name' : 'Port #',
                                     },
            'subnetIp'               : { 'verbose-name' : 'Subnet',
                                     },

        },
                     }
}

"""
SWITCH_CLUSTER_FORMAT = {
    'switch-cluster' : {
        'field-orderings' : {
            'default' : [ 'Idx', 'cluster-id', 'switches', ],
            },
        'fields' : {
            'cluster-id' : { 'formatter' : fmtcnv.print_cluster_id,
                           },
            'switches'   : { 'formatter' : fmtcnv.print_switches
                           },
            }
        },
}

BROADCAST_DOMAIN_FORMAT = {
    'external-ports' : {
        'field-orderings' : {
            'default' : [ 'Idx', 'ports', ],
            },
        'fields' : {
            'ports'   : { 'verbose-name': 'Switch Ports',
			     'formatter' : fmtcnv.print_switch_port_list,
                        },
            }
        },
}

TECH_SUPPORT_CONFIG_FORMAT = {
    'tech-support-config' : {
        'field-orderings' : {
                'default' : [ 'cmd-type',
                              'cmd'
                            ],
            },
        },
}


VERSION_FORMAT = {
    'version' : {
        'field-orderings' : {
            'details' : [ 'controller' ]
        },

        'fields' : {
            'controller' : {
                             'verbose-name' : 'Controller version',
                           }
        }
    },
}


SYSTEM_CLOCK_FORMAT = {
    'system-clock' : {
        'field-orderings' : {
            'default' : [ 'time' ],
            'details' : [ 'time', 'tz' ]
        },

        'fields'          : {
            'time' : {
                     },
            'tz'   : { 'verbose-name' : 'Timezone'
                     },
        },
    },
}
"""
