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

import fmtcnv

"""
FLOW_ENTRY_SUBMODE_COMMAND_DESCRIPTION = {
    'name'            : 'flow-entry',
    'mode'            : 'config-switch*',
    'command-type'    : 'config-submode',
    'short-help'      : 'Enter flow-entry submode, configure single static flow entry',
    'doc'             : 'flow-entry|flow-entry',
    'doc-example'     : 'flow-entry|flow-entry-example',
    'obj-type'        : 'flow-entry',
    'parent-field'    : 'switch',
    'submode-name'    : 'config-switch-flow-entry',
    'args' : {
        'field'       : 'name',
        'type'        : 'identifier',
        'completion'  : 'complete-object-field',
        'scoped'      : True,
        'syntax-help' : 'Enter name for a flow-entry',
    },
}

# obj_type flow-entry field hard-timeout
FLOW_ENTRY_EDIT_HARD_TIMEOUT_COMMAND_DESCRIPTION = {
    'name'                : 'hard-timeout',
    'mode'                : 'config-switch-flow-entry',
    'short-help'          : 'Set hard-timeout for this flow',
    'doc'                 : 'flow-entry|hard-timeout',
    'doc-example'         : 'flow-entry|hard-timeout-example',
    'command-type'        : 'config',
    'args'                : (
         {
             'field'      : 'hard-timeout',
             'base-type'  : 'integer',
             'range'      : (0, 65535),
             'doc'        : 'flow-entry|hard-timeout-field',
         }
    )
}


# obj_type flow-entry field idle-timeout
FLOW_ENTRY_EDIT_IDLE_TIMEOUT_COMMAND_DESCRIPTION = {
    'name'                : 'idle-timeout',
    'mode'                : 'config-switch-flow-entry',
    'short-help'          : 'Set idle-timout for this flow',
    'doc'                 : 'flow-entry|idle-timeout',
    'doc-example'         : 'flow-entry|idle-timeout-example',
    'command-type'        : 'config',
    'args'                : (
         {
             'field'      : 'idle-timeout',
             'base-type'  : 'integer',
             'range'      : (0, 65535),
             'doc'        : 'flow-entry|idle-timeout-field',
         }
    )
}


# obj_type flow-entry field actions
FLOW_ENTRY_EDIT_ACTIONS_COMMAND_DESCRIPTION = {
    'name'                : 'actions',
    'mode'                : 'config-switch-flow-entry',
    'short-help'          : 'Set actions for this flow',
    'doc'                 : 'flow-entry|actions',
    'doc-example'         : 'flow-entry|actions-example',
    'command-type'        : 'config',
    'args'                : (
         {
             'field'      : 'actions',
             'type'       : 'string',
             'completion' : 'complete-staticflow-actions',
             'doc'        : 'flow-entry|action-field',
         }
    )
}


# obj_type flow-entry field active
FLOW_ENTRY_EDIT_ACTIVE_COMMAND_DESCRIPTION = {
    'name'                : 'active',
    'mode'                : 'config-switch-flow-entry',
    'short-help'          : 'Set flow active',
    'doc'                 : 'flow-entry|active',
    'doc-example'         : 'flow-entry|active-example',
    'command-type'        : 'config',
    'args'                : (
         {
             # this should be improved, but this is the old syntax
             'field'  : 'active',
             'type'   : 'enum',
             'values' : ('True', 'False'),
             'doc'    : 'flow-entry|active-field-+',
             # XXX roth -- does not show up in wiki
         }
    )
}


# obj_type flow-entry field priority
FLOW_ENTRY_EDIT_PRIORITY_COMMAND_DESCRIPTION = {
    'name'                : 'priority',
    'mode'                : 'config-switch-flow-entry',
    'short-help'          : 'Set priority of the flow',
    'doc'                 : 'flow-entry|priority',
    'doc-example'         : 'flow-entry|priority-example',
    'command-type'        : 'config',
    'args'                : (
         {
             'field'      : 'priority',
             'base-type'  : 'integer',
             'range'      : (0, 65535),
             'doc'        : 'flow-entry|priority-field',
         }
    )
}


# obj_type flow-entry field cookie
FLOW_ENTRY_EDIT_COOKIE_COMMAND_DESCRIPTION = {
    'name'                : 'cookie',
    'mode'                : 'config-switch-flow-entry',
    'command-type'        : 'config',
    'short-help'          : 'Associate cookie for flow',
    'doc'                 : 'flow-entry|cookie',
    'doc-example'         : 'flow-entry|cookie-example',
    'args'                : (
                                 {
                                     'field' : 'cookie',
                                     'type'  : 'integer',
                                 }
                            )
}


# obj_type flow-entry field wildcards
FLOW_ENTRY_EDIT_WILDCARDS_COMMAND_DESCRIPTION = {
    'name'                : 'wildcards',
    'mode'                : 'config-switch-flow-entry',
    'command-type'        : 'config',
    'short-help'          : 'Configure wildcards for flow',
    'doc'                 : 'flow-entry|wildcards',
    'doc-example'         : 'flow-entry|wildcards-example',
    'args'                : (
         {
             'field'      : 'wildcards',
             'base-type'  : 'integer',
             'range'      : (0, 2**32-1),
             'doc'        : 'flow-entry|wildcards-field',
         }
    )
}


# obj_type flow-entry field ingress-port
FLOW_ENTRY_EDIT_INGRESS_PORT_COMMAND_DESCRIPTION = {
    'name'                : 'ingress-port',
    'mode'                : 'config-switch-flow-entry',
    'command-type'        : 'config',
    'short-help'          : 'Configure wildcards for flow',
    'doc'                 : 'flow-entry|ingress-port',
    'doc-example'         : 'flow-entry|ingress-port-example',
    'args'                : (
         {
             'field'      : 'ingress-port',
             'base-type'  : 'integer',
             'range'      : (0, 2**16-1), # OF V2 uses 2^32 for ports
             'doc'        : 'flow-entry|ingress-port-field',
         }
    )
}


# obj_type flow-entry field src-mac
FLOW_ENTRY_EDIT_SRC_MAC_COMMAND_DESCRIPTION = {
    'name'                : 'src-mac',
    'mode'                : 'config-switch-flow-entry',
    'command-type'        : 'config',
    'short-help'          : 'Configure src-mac match for flow',
    'doc'                 : 'flow-entry|src-mac',
    'doc-example'         : 'flow-entry|src-mac-example',
    'obj-type'            : 'flow-entry',
    'args'                : (
         {
             'field'        : 'src-mac',
             'type'         : 'host',
             'completion'   : 'complete-alias-choice',
             'other'        : 'host|mac',
             'data-handler' : 'alias-to-value',
             'doc'          : 'flow-entry|src-mac-field',
         }
    )
}


# obj_type flow-entry field dst-mac
FLOW_ENTRY_EDIT_DST_MAC_COMMAND_DESCRIPTION = {
    'name'                : 'dst-mac',
    'mode'                : 'config-switch-flow-entry',
    'command-type'        : 'config',
    'short-help'          : 'Configure dst-mac match for flow',
    'doc'                 : 'flow-entry|dst-mac',
    'doc-example'         : 'flow-entry|dst-mac-example',
    'obj-type'            : 'flow-entry',
    'args'                : (
         {
             'field'        : 'dst-mac',
             'type'         : 'host',
             'completion'   : 'complete-alias-choice',
             'other'        : 'host|mac',
             'data-handler' : 'alias-to-value',
             'doc'          : 'flow-entry|dst-mac-field',
         }
    )
}


# obj_type flow-entry field vlan-id
FLOW_ENTRY_EDIT_VLAN_ID_COMMAND_DESCRIPTION = {
    'name'                : 'vlan-id',
    'mode'                : 'config-switch-flow-entry',
    'short-help'          : 'Configure vlan-id match for flow',
    'doc'                 : 'flow-entry|vlan-id',
    'doc-example'         : 'flow-entry|vlan-id-example',
    'command-type'        : 'config',
    'args'                : (
         {
             'field'      : 'vlan-id',
             'base-type'  : 'integer',
             'range'      : (0, 2**12-1),
             'doc'        : 'flow-entry|vlan-id-field',
         }
    )
}


# obj_type flow-entry field vlan-priority
FLOW_ENTRY_EDIT_VLAN_PRIORITY_COMMAND_DESCRIPTION = {
    'name'                : 'vlan-priority',
    'mode'                : 'config-switch-flow-entry',
    'short-help'          : 'Configure vlan-priority match for flow',
    'doc'                 : 'flow-entry|vlan-priority',
    'doc-example'         : 'flow-entry|vlan-priority-example',
    'command-type'        : 'config',
    'args'                : (
         {
             'field'      : 'vlan-priority',
             'base-type'  : 'integer',
             'range'      : (0,7),
             'doc'        : 'flow-entry|vlan-priority-field',
         }
    )
}


# obj_type flow-entry field ether-type
FLOW_ENTRY_EDIT_ETHER_TYPE_COMMAND_DESCRIPTION = {
    'name'                : 'ether-type',
    'mode'                : 'config-switch-flow-entry',
    'short-help'          : 'Configure ether-type match for flow',
    'doc'                 : 'flow-entry|ether-type',
    'doc-example'         : 'flow-entry|ether-type-example',
    'command-type'        : 'config',
    'args'                : {
        'choices' : (
            {
                'field'           : 'ether-type',
                'base-type'       : 'hex-or-decimal-integer',
                'range'           : (1536,65536),
                'data-handler'    : 'hex-to-integer',
                'doc'             : 'flow-entry|ether-type-field',
            },
            {
            'field'           : 'ether-type',
            'type'            : 'enum',
            'values'          : fmtcnv.ether_type_to_number_dict,
            },
        ),
    },
}


# obj_type flow-entry field tos-bits
FLOW_ENTRY_EDIT_TOS_BITS_COMMAND_DESCRIPTION = {
    'name'                : 'tos-bits',
    'mode'                : 'config-switch-flow-entry',
    'short-help'          : 'Configure ether-type match for flow',
    'doc'                 : 'flow-entry|tos-bits',
    'doc-example'         : 'flow-entry|tos-bits-example',
    'command-type'        : 'config',
    'args'                : (
         {
             'field'      : 'tos-bits',
             'base-type'  : 'integer',
             'range'      : (0,63),
             'doc'        : 'flow-entry|tos-bits-field',
         }
    )
}


# obj_type flow-entry field protocol
FLOW_ENTRY_EDIT_PROTOCOL_COMMAND_DESCRIPTION = {
    'name'                : 'protocol',
    'mode'                : 'config-switch-flow-entry',
    'short-help'          : 'Configure ether-type match for flow',
    'doc'                 : 'flow-entry|protocol',
    'doc-example'         : 'flow-entry|protocol-example',
    'command-type'        : 'config',
    'args'                : (
         {
             'field'      : 'protocol',
             'base-type'  : 'integer',
             'range'      : (0,255),
             'doc'        : 'flow-entry|protocol-field',
         }
    )
}


# obj_type flow-entry field src-ip
FLOW_ENTRY_EDIT_SRC_IP_COMMAND_DESCRIPTION = {
    'name'                : 'src-ip',
    'mode'                : 'config-switch-flow-entry',
    'short-help'          : 'Configure src-ip match for flow',
    'doc'                 : 'flow-entry|src-ip',
    'doc-example'         : 'flow-entry|src-ip-example',
    'command-type'        : 'config',
    'args'                : {
        'choices' : (
            {
                'field'           : 'src-ip',
                'help-name'       : 'ip-address',
                'type'            : 'ip-address',
                'completion-text' : 'src-ip-address',
                'doc'             : 'flow-entry|src-ip-field',
            },
            {
                'field'           : 'src-ip',
                'help-name'       : 'cidr-range',
                'type'            : 'cidr-range',
                'completion-text' : 'src-ip-cidr-range',
                'doc'             : 'flow-entry|src-ip-field',
            },
        )
    },
}


# obj_type flow-entry field dst-ip
FLOW_ENTRY_EDIT_DST_IP_COMMAND_DESCRIPTION = {
    'name'                : 'dst-ip',
    'mode'                : 'config-switch-flow-entry',
    'short-help'          : 'Configure dst-ip match for flow',
    'doc'                 : 'flow-entry|dst-ip',
    'doc-example'         : 'flow-entry|dst-ip-example',
    'command-type'        : 'config',
    'args'                : {
        'choices' : (
            {
                'field'           : 'dst-ip',
                'help-name'       : 'ip-address',
                'type'            : 'ip-address',
                'completion-text' : 'dst-ip-address',
                'doc'             : 'flow-entry|dst-ip-field',
            },
            {
                'field'           : 'dst-ip',
                'help-name'       : 'cidr-range',
                'type'            : 'cidr-range',
                'completion-text' : 'dst-ip-cidr-range',
                'doc'             : 'flow-entry|dst-ip-field',
            },
        )
    },
}


# obj_type flow-entry field src-port
FLOW_ENTRY_EDIT_SRC_PORT_COMMAND_DESCRIPTION = {
    'name'                : 'src-port',
    'mode'                : 'config-switch-flow-entry',
    'short-help'          : 'Configure src-port match for flow',
    'doc'                 : 'flow-entry|src-port',
    'doc-example'         : 'flow-entry|src-port-example',
    'command-type'        : 'config',
    'args'                : {
        'choices' : (
            {
                'field'        : 'src-port',
                'base-type'    : 'hex-or-decimal-integer',
                'range'        : (0,65535),
                'data-handler' : 'hex-to-integer',
                'doc'          : 'flow-entry|src-port-field',
            },
            {
                'field'    : 'src-port',
                'type'     : 'enum',
                'values'   : fmtcnv.tcp_name_to_number_dict,
            },
        )
    }
}


# obj_type flow-entry field dst-port
FLOW_ENTRY_EDIT_DST_PORT_COMMAND_DESCRIPTION = {
    'name'                : 'dst-port',
    'mode'                : 'config-switch-flow-entry',
    'short-help'          : 'Configure dst-port match for flow',
    'doc'                 : 'flow-entry|dst-port',
    'doc-example'         : 'flow-entry|dst-port-example',
    'command-type'        : 'config',
    'args'                : {
        'choices' : (
            {
                'field'        : 'dst-port',
                'base-type'    : 'hex-or-decimal-integer',
                'range'        : (0,65535),
                'data-handler' : 'hex-to-integer',
                'doc'          : 'flow-entry|dst-port-field',
            },
            {
                'field'    : 'dst-port',
                'type'     : 'enum',
                'values'   : fmtcnv.tcp_name_to_number_dict,
            },
        )
    },
}
#
# FORMATS
#

import fmtcnv


FLOW_ENTRY_FORMAT = {
    'flow-entry' : {
        'field-orderings' : {
            'default' : [ 'Idx',
                          'switch',
                          'name',
                          'active',
                          'idle-timeout',
                          'hard-timeout',
                          'cookie',
                          'priority',
                          'wildcards',
                          'ingress-port',
                          'src-mac',
                          'dst-mac',
                          'ether-type',
                          'vlan-id',
                          'vlan-priority',
                          'src-ip',
                          'dst-ip',
                          'protocol',
                          'tos-bits',
                          'src-port',
                          'dst-port',
                          'actions',
                        ]
            },
        'fields' :  {
            'switch'       : { 'formatter' : fmtcnv.replace_switch_with_alias },
            'ether-type'   : { 'formatter' : fmtcnv.decode_ether_type },
            'protocol'     : { 'formatter' : fmtcnv.decode_network_protocol },
            'src-port'     : { 'formatter' : fmtcnv.decode_src_port },
            'dst-port'     : { 'formatter' : fmtcnv.decode_dst_port },
            'ingress-port' : { 'formatter' : fmtcnv.decode_openflow_port },
            },
        },
}
"""

