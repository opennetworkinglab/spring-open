#
# Copyright (c) 2010,2011,2012,2013 Big Switch Networks, Inc.
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
import command

INTERNAL_SUBMODE_COMMAND_DESCRIPTION = {
    'name'                : 'internal',
    'mode'                : 'config',
    'no-supported'        : False,
    'help'                : 'Enter Internal CLI debugging mode',
    'short-help'          : 'Enter CLI internal debugging mode',
    'doc'                 : 'internal|internal',
    'doc-example'         : 'internal|internal-example',
    'command-type'        : 'config-submode',
    'obj-type'            : None,
    'current-mode-obj-id' : None,
    'parent-field'        : None,
    'submode-name'        : 'config-internal',
    'args'                : (),
}

INTERNAL_SHOW_INTERFACES_COMMAND_DESCRIPTION = {
    'name'              : 'show',
    'mode'              : 'config-internal',
    'no-supported'      : False,
    'short-help'        : 'Show system interfaces',
    'doc'               : 'internal|show-interfaces',
    'doc-example'       : 'internal|show-interfaces-example',
    'args' : (
        'system',
        {
            'token'  : 'interfaces',
            'action' : 'display-rest' ,
            'url'    : 'system/interfaces/',
            'format' : 'system-interfaces',
        },
        {
            'field'    : 'detail',
            'type'     : 'enum',
            'values'   : ('details','brief'),
            'optional' : True,
            'doc'      : 'format|+'
        }
    )
}
DEBUG_INTERNAL_TOPOLOGY_COMMAND_DESCRIPTION = {
    'name'         : 'internal',
    'mode'         : 'config-internal',
    'command-type' : 'display-rest',
    'url'          : [
                        'internal-debugs/topology-manager/%(dpid)s',
                        'internal-debugs/topology-manager/',
                     ],
    'format'       : 'switch',
    'short-help'   : 'internal topology display',
    'doc'          : 'internal|internal-topology',
    'args'         : (
        'topology',
        {
            'choices' : (
                {
                    'field'    : 'dpid',
                    'type'     : 'dpid',
                    'optional' : 'true',
                },
            )
        }
    )
}

DEBUG_INTERNAL_CLUSTER_COMMAND_DESCRIPTION = {
    'name'         : 'internal',
    'mode'         : 'config-internal',
    'command-type' : 'display-rest',
    'url'          : 'realtimestatus/network/cluster',
    'format'       : 'switch',
    'short-help'   : 'internal switch cluster display',
    'args'         : (
        'cluster',
    )
}


SHOW_VNS_ACCESS_LIST_ENTRY_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'obj-type'     : 'vns-access-list-entry',
    'short-help'   : 'Show vns-access-list-entry db table',
    'doc'          : 'internal|show-vns-access-list-entry',
    'mode'         : 'config-internal',
    'command-type' : 'display-table',
    'args'         : (
        'vns-access-list-entry',
        {
            'optional'   : True,
            'field'      : 'vns',
            'type'       : 'identifier',
            'completion' : 'complete-from-another',
            'other'      : 'vns-definition',
            'help-name'  : "<vns-id>",
            'detail'     : 'default',
        },
        {
            'optional'   : True,
            'field'      : 'acl',
            'tag'        : 'acl',
        },
        {
            'optional'   : True,
            'field'      : 'rule',
            'tag'        : 'rule',
        },
    )
}

    
SHOW_PORT_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'obj-type'     : 'port',
    'short-help'   : 'Show port db table',
    'doc'          : 'internal|show-port',
    'mode'         : 'config-internal',
    'command-type' : 'display-table',
    'args'         : (
        'port',
        {
            'field'      : 'id',
            'optional'   : True,
            'type'       : 'obj-type',
            'completion' : 'complete-object-field',
            'parent-id'  : None,
        },
        {
            'field'        : 'dpid',
            'optional'     : True,
            'tag'          : 'switch',
            'completion'   : 'complete-from-another',
            'other'        : 'switches',
            'data-handler' : 'alias-to-value',
        },
        {
            'field'        : 'number',
            'tag'          : 'port',
            'optional'     : True,
            'completion'   : 'complete-object-field',
        },
        {
            'field'      : 'detail',
            'optional'   : True,
            'type'       : 'enum',
            'values'     : ('details', 'brief'),
        }
    )
}


SHOW_VNS_INTERFACE_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'obj-type'     : 'vns-interface',
    'short-help'   : 'Show vns-interface db table',
    'doc'          : 'internal|show-vns-interface',
    'mode'         : 'config-internal',
    'command-type' : 'display-table',
    'args'         : (
        'vns-interface',
        {
            'field'      : 'id',
            'tag'        : 'vns',
            'type'       : 'string',
            'optional'   : True,
            'parent-id'  : 'vns',
            'completion' : 'complete-object-field',
        },
        {
            'field'    : 'switch',
            'tag'      : 'switch',
            'type'     : 'dpid',
            'optional' : True,
        },
        {
            'field'     : 'number',
            'tag'       : 'number',
            'type'      : 'integer',
            'help-name' : 'OF Port Number',
            'optional'  : True,
        },
    )
}


SHOW_VNS_INTERFACE_RULE_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'obj-type'     : 'vns-interface-rule',
    'short-help'   : 'Show vns-interface-rule db table',
    'doc'          : 'internal|show-vns-interface-rule',
    'mode'         : 'config-internal',
    'command-type' : 'display-table',
    'args'         : (
        'vns-interface-rule',
        {
            'field'      : 'vns',
            'tag'        : 'vns',
            'optional'   : True,
            'type'       : 'string',
            'parent-id'  : 'vns',
            'completion' : 'complete-object-field',
        },
        {
            'field'    : 'rule',
            'tag'      : 'rule',
            'type'     : 'string',
            'optional' : True,
            'completion' : 'complete-object-field',
        },
    )
}


INTERNAL_SHOW_DEVICE_COMMAND_DESCRIPTION = {
    'name'              : 'show',
    'mode'              : 'config-internal',
    'no-supported'      : False,
    'short-help'        : 'Show controller devices',
    'doc'               : 'internal|show-controller-devices',
    'args' : (
        'controller',
        {
            'choices' : (
                {
                    'token'     : 'devices',
                    'action'    : 'display-rest' ,
                    'url'       : 'device',
                    'format'    : 'controller-devices',
                },
                (
                    {
                    'token'     : 'vns',
                    'action'    : 'display-rest' ,
                    'url'       : 'vns',
                    'format'    : 'controller-vns',
                    },
                    { 
                        'optional' : True,
                        'choices' : (
                            {
                                'token'     : 'interfaces',
                                'optional'  : True,
                                'action'    : 'display-rest' ,
                                'url'       : 'vns/interface',
                                'format'    : 'controller-vns-interfaces',
                            },
                            {
                                'token'     : 'device-interfaces',
                                'optional'  : True,
                                'action'    : 'display-rest' ,
                                'url'       : 'vns/device-interface',
                                'format'    : 'controller-vns-device-interface',
                            },
                        )
                    },
                ),
            )
        },
        {
            'field'    : 'detail',
            'type'     : 'enum',
            'values'   : ('details','brief'),
            'optional' : True,
        }
    )
}


def display_schema(data):
    return command.sdnsh.sdndb.schema_detail(data['path'])

command.add_action('display-schema',  display_schema,
                   {'kwargs' : { 'data' : '$data' } } )

INTERNAL_SHOW_SCHEMA_COMMAND_DESCRIPTION = {
    'name'              : 'show',
    'mode'              : 'config-internal',
    'feature'           : 'experimental',
    'no-supported'      : False,
    'short-help'        : 'Show controller devices',
    'action'            : 'display-schema',
    'args' : (
        'schema',
        {
            'field'     : 'path',
            'type'      : 'string',
        },
    ),
}


def lint_action(data):
    words = []
    if 'command' in data:
        words.append(data['command'])
    command.lint_command(words)

command.add_action('lint-action',  lint_action,
                   {'kwargs' : { 'data' : '$data' } } )

INTERNAL_LINT_COMMAND_DESCRIPTION = {
    'name'         : 'lint',
    'mode'         : 'config-internal',
    'no-supported' : False,
    'action'       : 'lint-action',
    'args'         : {
        'optional' : True,
        'field'    : 'command',
        'type'     : 'string',
    }
}


def permute_action(data):
    words = []
    if 'command' in data:
        words.append(data['command'])
    return command.permute_command(words, data.get('qualify'))

command.add_action('permute-action',  permute_action,
                   {'kwargs' : { 'data' : '$data' } } )


INTERNAL_PERMUTE_COMMAND_DESCRIPTION = {
    'name'         : 'permute',
    'mode'         : 'config-internal',
    'no-supported' : False,
    'action'       : 'permute-action',
    'data'         : { 'qualify' : False },
    'args'         : (
        {
            'optional' : True,
            'field'    : 'command',
            'type'     : 'string',
        },
    )
}


INTERNAL_QUALIFY_COMMAND_DESCRIPTION = {
    'name'         : 'qualify',  # berate
    'mode'         : 'config-internal',
    'no-supported' : False,
    'action'       : 'permute-action',
    'data'         : { 'qualify' : True },
    'args'         : (
        {
            'optional' : True,
            'field'    : 'command',
            'type'     : 'string',
        },
    )
}


def clidoc_action(data):
    words = []
    if 'command' in data:
        words.append(data['command'])
    return command.get_clidoc(words)

command.add_action('clidoc-action',  clidoc_action,
                   {'kwargs' : { 'data' : '$data' }, } )


INTERNAL_CLIDOC_COMMAND_DESCRIPTION = {
    'name'         : 'clidoc',
    'mode'         : 'config-internal',
    'no-supported' : False,
    'action'       : 'clidoc-action',
    'args'         : {
        'optional' : True,
        'field'    : 'command',
        'type'     : 'string',
    }
}

def cliwiki_action(data):
    words = []
    if 'command' in data:
        words.append(data['command'])
    return command.get_cliwiki(words)

command.add_action('cliwiki-action',  cliwiki_action,
                   {'kwargs' : { 'data' : '$data' }, } )
INTERNAL_CLIWIKI_COMMAND_DESCRIPTION = {
    'name'         : 'cliwiki',
    'mode'         : 'config-internal',
    'no-supported' : False,
    'action'       : 'cliwiki-action',
    'args'         : {
        'optional' : True,
        'field'    : 'command',
        'type'     : 'string',
    }
}

SHOW_CLI_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'config-internal',
    'short-help'   : 'Show CLI details',
    'doc'          : 'internal|show-cli',
    'action'       : 'display-cli',
    'no-supported' : False,
    'args'         : (
        'cli',
        {
            'optional' : True,
            'field'    : 'detail',
            'type'     : 'enum',
            'values'   : ('details'),
        },
    )
}


#
# FORMATS
#

import fmtcnv


SYSTEM_INTERFACES_FORMAT = {
    'system-interfaces' : {
        'field-orderings': {
            'default' : [
                            'Idx',
                            # 'name',
                            'addr',
                            'netmask',
                            'broadcast',
                            # 'peer',
                        ],
            'details' : [
                            'Idx',
                            'name',
                            'addr',
                            'netmask',
                            'broadcast',
                            'peer',
                        ],
            'brief'   : [
                            'Idx',
                            'addr',
                        ]
        },
        'fields' : {
            'name'       : {
                            'verbose-name' : 'IF Name',
                           },
            'addr'       : {
                            'verbose-name' : 'IP Address',
                           },
            'peer'       : {
                           },
            'netmask'    : {
                           },
            'broadcast'  : {
                           },
        }
    },
}


PORT_FORMAT = {
    'port' : {
        'field-orderings' : {
            'default' : [ 'Idx', 'switch', 'number', 'name', 'hardware-address',
                          'config', 'state', 'current-features',
                          'advertised-features', 'supported-features', 'peer-features' ],
            },
        'fields' : {
            'switch'              : { 'formatter' : fmtcnv.replace_switch_with_alias
                                    },
            'number'              : { 'formatter' : fmtcnv.decode_openflow_port
                                    },
            'config'              : { 'formatter' : fmtcnv.decode_port_config
                                    },
            'state'               : { 'formatter' : fmtcnv.decode_port_state
                                    },
            'current-features'    : { 'formatter' : fmtcnv.decode_port_features
                                    },
            'advertised-features' : { 'formatter' : fmtcnv.decode_port_features
                                    },
            'supported-features'  : { 'formatter' : fmtcnv.decode_port_features
                                    },
            'peer-features'       : { 'formatter' : fmtcnv.decode_port_features
                                    },
            }
        },
}


CONTROLLER_VNS_FORMAT = {
    'controller-vns' : {
        'field-orderings' : {
            'default' : [ 'Idx',
                          'name',
                          'active',
                          'priority',
                          'arpManagerMode',
                          'broadcastMode',
                          'dhcpManagerMode',
                          'dhcpIp',
                        ],
        },
        'fields' : {
            'name'              : {
                                    'verbose-name' : 'Name',
                                  },
            'active'            : {
                                    'verbose-name' : 'Active',
                                  },
            'priority'          : {
                                    'verbose-name' : 'Priority',
                                  },
            'arpManagerMode'    : {
                                    'verbose-name' : 'Arp Mode',
                                  },
            'broadcastMode'     : {
                                    'verbose-name' : 'Broadcast Mode',
                                  },
            'dhcpManagerMode'   : {
                                    'verbose-name' : 'Dhcp Modd',
                                  },
            'dhcpIp'            : {
                                    'verbose-name' : 'dhcpIp',
                                  },
        },
    },
}


CONTROLLER_VNS_INTERFACES = {
    'controller-vns-interfaces' : {
        'field-orderings' : {
            'default' : [ 'Idx',
                        ],
        },
    },
}


CONTROLLER_VNS_DEFICE_INTERFACES = {
        'controller-vns-device-interfaces' : {
            'field-orderings' : {
                'default' : [ 'Idx',
                              'device',
                              'iface',
                            ],
            },
        },
}


CONTROLLER_DEVICES_FORMAT = {
    'controller-devices' : {
        'field-orderings' : {
            'default' : [ 'Idx',
                          'mac',
                          'attachment-point',
                          'ipv4',
                          'vlan',
                        ]
        },

        'fields' : {
            'mac'               : {
                                    'verbose-name' : 'MAC Address',
                                    'formatter' : fmtcnv.convert_to_string,
                                  },
            'ipv4'              : {
                                    'verbose-name' : 'IP Address',
                                    'formatter' : fmtcnv.convert_to_string,
                                  },
            'attachment-point'  : {
                                    'verbose-name' : 'Attachment Point',
                                    'formatter' : fmtcnv.convert_to_string,
                                  },
            'vlan'              : {
                                    'verbose-name' : 'Vlans',
                                    'formatter' : fmtcnv.convert_to_string,
                                  },
        },
    },
}

DEVICEMANAGER_DEVICE_FORMAT = {
    'devicemanager-device' : {
        'field-orderings' : {
            'default' : [
                            'Idx',
                            'host',
                            'attachment-points',
                            'network-addresses'
                        ]
        },

        'fields' : {
            'host'              : { 'formatter' : fmtcnv.print_host_and_alias,
                                  },
            'attachment-points' : {
                                    'verbose-name' : 'Attachment Point',
                                    'formatter' : fmtcnv.print_devicemanager_attachment_points,
                                  },
            'network-addresses' : {
                                    'verbose-name' : 'IP Address',
                                    'formatter' : fmtcnv.print_devicemanager_ip_addresses,
                                  },

        },
    },
}


CLI_FORMAT = {
    'cli'  : {
        'field-orderings' : {
            'default' : [
                'version',
                'debug',
                'desc'
                'format',
                'modes',
            ]
        },

        'fields' : {
            'version' : { 'verbose-name' : 'Syntax Version',
                        },
            'debug'   : { 'verbose-name' : 'Debug Level',
                        },
            'desc'    : { 'verbose-name' : 'Desc Modules',
                        },
            'format'  : { 'verbose-name' : 'Format Modules',
                        },
            'modes'   : { 'verbose-name' : 'Submodes',
                        },
        },
    },
}

CLI_MODES_FORMAT = {
    'cli-modes' : {
        'field-orderings' : {
            'default' : [ 'Idx', 'mode', 'command', 'submode',
                        ]
        }
    },
}
"""