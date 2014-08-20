#
# Copyright (c) 2011,2013 Big Switch Networks, Inc.
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
'''
def address_space_origin_external (data):
    """
    Return origin-name when the address space wasn't created by the cli,
    return None otherwise.
    """
    pk = command.mi.pk('address-space')
    if not pk in data:
        return;

    address_space = command.sdnsh.get_table_from_store('address-space', pk,
                                                       data[pk])
    if len(address_space) == 0:
        return None

    local = ['cli', 'rest']
    if 'origin' in address_space[0] and not address_space[0]['origin'] in local:
        return address_space[0]['origin']
    return None
    

def address_space_warn_external_config (data):
    """
    From the named address-space, look up the entry, if it exists in the
    database, validate the 'origin' is either null, or 'cli',
    otherwise provide a warning about this particular address-space
    (along with the originator name)
    """
    external_origin = address_space_origin_external(data)
    if external_origin:
        command.sdnsh.warning(
            'address-space %s may not be intended for cli update, '
            'origin/creator "%s" ' % (data['name'], external_origin))


command.add_action('address-space-warn-external-config',
                   address_space_warn_external_config,
                   {'kwargs': {'data' : '$data',}})


def address_space_confirm_external_delete (data):
    """
    From the named address-space, look up the entry, if it exists in the
    database, validate the 'origin' is either null, or 'cli',
    otherwise provide a warning about this particular address-space
    (along with the originator name)
    """
    external_origin = address_space_origin_external(data)
    if external_origin:
        confirm = command.action_registry['confirm'][0] # XXX accessor?
        confirm('address-space %s may not be intended for cli delete, '
                'origin/creator "%s"\nEnter y or yes to continue delete: '
                % (data['name'], external_origin))

command.add_action('address-space-confirm-external-delete',
                   address_space_confirm_external_delete,
                   {'kwargs': {'data'      : '$data',}})
'''
"""
#
# ----------------------------------------------------------------------
# address-space submode configuration commands
#

#
# conf: [no] address-space <address-space-name>
#
ADDRESS_SPACE_COMMAND_DESCRIPTION = {
    'name'         : 'address-space',
    'help'         : 'Enter Address-space submode',
    'mode'         : 'config*',
    'command-type' : 'config-submode',
    'obj-type'     : 'address-space',
    'submode-name' : 'config-address-space',
    'short-help'   : 'Enter address space submode',
    'doc'          : 'address-space|address-space',
    'doc-example'  : 'address-space|address-space-example',
    'args' : (
        {
            'field'        : 'name',
            'type'         : 'identifier',
            'completion'   : 'complete-object-field',
            'syntax-help'  : 'Enter an address-space name',
            'doc'          : 'address-space|identifier',
            'doc-include'  : [ 'type-doc' ],
            'action'       : (
                {
                    'proc' : 'address-space-warn-external-config',
                },
                {
                    'proc' : 'push-mode-stack',
                },
            ),
            'no-action'    : (
                {
                    'proc' : 'address-space-confirm-external-delete',
                },
                {
                    'proc' : 'delete-objects',
                },
            )
        }
    ),
}

#
# conf/address-space <as>/active {True | False}
#
ADDRESS_SPACE_ACTIVE_COMMAND_DESCRIPTION = {
    'name'        : 'active',
    'short-help'  : 'Set address-space active',
    'mode'        : 'config-address-space',
    'doc'         : 'address-space|active',
    'doc-example' : 'address-space|active-example',
    'doc-include' : [ 'default' ],
    'args' : (),
    'action' : (
        {
            'proc' : 'write-fields',
            'data' : { 'active' : True },
            'syntax-help' : 'mark the address-space as active',
        }
    ),
    'no-action' : (
        {
            'proc' : 'write-fields',
            'data' : { 'active' : False },
            'syntax-help' : 'mark the address-space as inactive',
        }
    )
}

#
# conf/address-space <as>/origin {cli | rest | ... }
#
ADDRESS_SPACE_ORIGIN_COMMAND_DESCRIPTION = {
    'name'         : 'origin',
    'short-help'   : 'Describe address-space origin',
    'mode'         : 'config-address-space',
    'command-type' : 'config',
    'doc'          : 'address-space|origin',
    'doc-example'  : 'address-space|origin-example',
    'args' : (
        {
            'field'  : 'origin',
            'type'   : 'string',
            'action' : (
                {
                    'proc' : 'address-space-warn-external-config',
                },
                {
                    'proc' : 'write-fields',
                },
            ),
        },
    ),
}

#
# conf/address-space <as>/priority <pri>
#
ADDRESS_SPACE_PRIORITY_COMMAND_DESCRIPTION = {
    'name'         : 'priority',
    'short-help'   : 'Set address-space priority',
    'mode'         : 'config-address-space',
    'command-type' : 'config',
    'doc'          : 'address-space|priority',
    'doc-example'  : 'address-space|priority-example',
    'doc-include'  : [ 'range', 'default' ],
    'args' : (
        {
            'field'     : 'priority',
            'base-type' : 'integer',
            'range'     : (0, 65535),
        }
    )
}
"""

def address_space_check_unique_vlan(data):
    """
    Peek to see if the vlan is in use by any other address-space
    """
    vlan_field = 'vlan-tag-on-egress'
    in_use = command.sdnsh.get_table_from_store('address-space',
                                                vlan_field,
                                                str(data[vlan_field]))
    vlan_field = 'vlan-tag-on-egress'
    if len(in_use) > 0:
        current_as = command.sdnsh.get_current_mode_obj()
        other_as = [x['name'] for x in in_use if x['name'] != current_as]
        if len(other_as):
            command.sdnsh.warning(
                    'vlan %s currently in use by other address-spaces: %s' %
                    (data[vlan_field], ', '.join(other_as)))


command.add_action('address-space-check-unique-vlan',
                   address_space_check_unique_vlan,
                   {'kwargs': {'data' : '$data',}})

"""
#
# conf/address-space <as>/vlan-tag-on-egress <tag>
#
ADDRESS_SPACE_VLAN_TAG_ON_EGRESS_COMMAND_DESCRIPTION = {
    'name'         : 'vlan-tag-on-egress',
    'short-help'   : 'Egress VLAN tag',
    'mode'         : 'config-address-space',
    'command-type' : 'config',
    'doc'          : 'address-space|vlan-tag-on-egress',
    'doc-example'  : 'address-space|vlan-tag-on-egress-example',
    'args' : (
        {
            'field'     : 'vlan-tag-on-egress',
            'base-type' : 'integer',
            'range'     : (1, 4096),
            'action'    : (
                {
                    'proc' : 'address-space-check-unique-vlan',
                },
                {
                    'proc' : 'write-fields',
                },
            ),
        }
    )
}

#
# conf/address-space <as>/description <descr>
#
ADDRESS_SPACE_DESCRIPTION_COMMAND_DESCRIPTION = {
    'name'         : 'description',
    'short-help'   : 'Provide description for this address-space',
    'mode'         : 'config-address-space',
    'command-type' : 'config',
    'doc'          : 'address-space|description',
    'doc-example'  : 'address-space|description-example',
    'args'         : (
        {
            'field' : 'description',
            'type'  : 'string',
        }
    ),
}

#
# conf/address-space <as>/identifier-rule <ir>
#
ADDRESS_SPACE_IDENTIFIER_RULE_COMMAND_DESCRIPTION = {
    'name'         : 'identifier-rule',
    'short-help'   : 'Set an address-space identifier rule',
    'mode'         : 'config-address-space*',
    'command-type' : 'config-submode',
    'obj-type'     : 'address-space-identifier-rule',
    'parent-field' : 'address-space',
    'submode-name' : 'config-address-space-id-rule',
    'doc'          : 'address-space|identifier-rule',
    'doc-example'  : 'address-space|identifier-rule-example',
    'args' : (
        {
            'field'        : 'rule',
            'type'         : 'identifier',
            'syntax-help'  : 'Enter an address-space identifier rule name',
            'completion'   : 'complete-object-field',
            'doc-include'  : [ 'type-doc' ],
            'doc'          : 'address-space|id-rule-identifier',
            'scoped'       : True,
        }
    )
}

#
# ----------------------------------------------------------------------
# address-space identifier rules submode
#

#
# conf/address-space <as>/identifier-rule <ir>/active {True | False}
#
ADDRESS_SPACE_ID_ACTIVE_COMMAND_DESCRIPTION = {
    'name'         : 'active',
    'mode'         : 'config-address-space-id-rule',
    'short-help'   : 'Set rule to Active',
    'command-type' : 'config',
    'doc'          : 'address-space|id-active',
    'doc-example'  : 'address-space|id-active-example',
    'doc-include'  : [ 'default' ],
    'args'         : (),
    'action'       : (
        {
            'proc' : 'write-fields',
            'data' : { 'active' : True }
        }
    ),
    'no-action'    : (
        {
            'proc' : 'write-fields',
            'data' : { 'active' : False }
        }
    )
}

#
# conf/address-space <as>/identifier-rule <ir>/description <descr>
#
ADDRESS_SPACE_ID_DESCRIPTION_COMMAND_DESCRIPTION = {
    'name'         : 'description',
    'short-help'   : 'Provide description for identifier rule',
    'mode'         : 'config-address-space-id-rule',
    'command-type' : 'config',
    'doc'          : 'address-space|id-description',
    'doc-example'  : 'address-space|id-description-example',
    'args'         : (
        {
            'field' : 'description',
            'type'  : 'string',
        }
    ),
}

#
# conf/address-space <as>/identifier-rule <ir>/priority <pri>
#
ADDRESS_SPACE_ID_PRIORITY_COMMAND_DESCRIPTION = {
    'name'         : 'priority',
    'short-help'   : 'Describe priority for identifier rule',
    'mode'         : 'config-address-space-id-rule',
    'command-type' : 'config',
    'doc'          : 'address-space|id-priority',
    'doc-include'  : [ 'range', 'default' ],
    'doc-example'  : 'address-space|id-priority-example',
    'args'         : (
        {
            'field' : 'priority',
            'value' : 'integer',
        }
    ),
}

#
# conf/address-space <as>/identifier-rule <ir>/switch <swt-dpid>
#
ADDRESS_SPACE_ID_MATCH_SWITCH_COMMAND_DESCRIPTION = {
    'name'         : 'match',
    'short-help'   : 'Associate switch with identifier rule',
    'all-help'     : 'Configure a match for this identifier rule',
    'mode'         : 'config-address-space-id-rule',
    'command-type' : 'config',
    'obj-type'     : 'address-space-identifier-rule',
    'doc-all'      : 'address-space|match',
    'doc-example'  : 'address-space|match-switch-example',
    'data'         : { # for no command reset
                       'switch' : None,
                       'ports'  : None,
                     },
    'args'         : (
        {
            'token'        : 'switch',
            'doc'          : 'address-space|match-switch',
        },
        {
            # constructed as args to ensure that the ports isn't
            # completed along with swtich values for 'no match switch'
            'optional-for-no' : True,
            'args' : (
                {
                    'field'           : 'switch',
                    'type'            : 'string',
                    'parent-field'    : None,
                    'completion'      : [
                                          'complete-object-field',
                                          'complete-alias-choice',
                                        ],
                    'help-name'       : 'switch dpid or switch alias',
                    'data-handler'    : 'alias-to-value',
                    'other'           : 'switches|dpid',
                },
                {
                    'field'           : 'ports',
                    'optional'        : True,
                    'type'            : 'string',
                    'help-name'       : 'switch interface, or range, or list',
                    'completion'      : 'complete-interface-list',
                    'optional-for-no' : True,
                    'doc'             : 'address-space|match-switch-ports',
                },
            ),
        },
    ),
}


#
# conf/address-space <as>/identifier-rule <ir>/tag <name-val-pair>
#
ADDRESS_SPACE_ID_MATCH_TAG_COMMAND_DESCRIPTION = {
    'name'         : 'match',
    'short-help'   : 'Associate tag with identifier rule',
    'mode'         : 'config-address-space-id-rule',
    'command-type' : 'config',
    'data'         : { # for no command reset
                       'tag' : None,
                     },
    'doc-example'  : 'address-space|match-tag-example',
    'args' : (
        {
            'token'        : 'tags',
            'doc'          : 'address-space|match-tag',
        },
        {
            'field'           : 'tag',
            'type'            : 'string',
            'optional-for-no' : True,
        }
    )
}

#
# conf/address-space <as>/identifier-rule <ir>/vlans <vl>
#
ADDRESS_SPACE_ID_MATCH_VLANS_COMMAND_DESCRIPTION = {
    'name'         : 'match',
    'short-help'   : 'Associate vlans with identifier rule',
    'mode'         : 'config-address-space-id-rule',
    'command-type' : 'config',
    'data'         : { # for no command reset
                       'vlans' : None,
                     },
    'doc-example'  : 'address-space|match-vlan-example',
    'args'         : (
        {
            'token'        : 'vlans',
            'doc'          : 'address-space|match-vlan',
        },
        {
            'field'           : 'vlans',
            'value'           : 'string',
            'help-name'       : 'Vlan number (1-4095) or range, or list',
            'optional-for-no' : True,
        }
    ),
}

#
# ----------------------------------------------------------------------
# show address-space commands
#

#
# show address-space <cr>
#
ADDRESS_SPACE_SHOW_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'obj-type'     : 'address-space',
    'mode'         : 'login',
    'command-type' : 'display-table',
    'all-help'     : 'Show address space information',
    'short-help'   : 'Show all address spaces',
    'doc'          : 'address-space|show',
    'doc-example'  : 'address-space|show-example',
    'args'         : (
        'address-space',
    ),
}

#
# show address-space <as> [brief | details] <cr>
#
ADDRESS_SPACE_SHOW_ID_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'obj-type'     : 'address-space',
    'mode'         : 'login',
    'command-type' : 'display-table',
    'short-help'   : 'Show a specific address space',
    'doc'          : 'address-space|show-id',
    'doc-example'  : 'address-space|show-id-example',
    'args'         : (
        'address-space',
        {
            'choices' : (
                {
                    'field'      : 'name',
                    'type'       : 'identifier',
                    'completion' : 'complete-object-field',
                    'help-name'  : "address-space-name",
                },
                {
                    'field'     : 'name',
                    'type'     : 'enum',
                    'values'   : 'all',
                }
            ),
        },
        {
            'field'    : 'detail',
            'optional' : True,
            'type'     : 'enum',
            'values'   : ('details', 'brief'),
            'doc'      : 'format|+',
        }
    )
}

#
# show address-space <as> [ identifier-rules ] ...
#
ADDRESS_SPACE_SHOW_ID_DETAILS_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'obj-type'     : 'address-space',
    'mode'         : 'login',
    'command-type' : 'display-table',
    'short-help'   : 'Show the configured identifier-rules for a specific address space',
    'doc'          : 'address-space|show-id-rules',
    'doc-example'  : 'address-space|show-id-rules-example',
    'args'         : (
        'address-space',
        {
            'choices' : (
                {
                    'field'      : 'name',
                    'type'       : 'identifier',
                    'completion' : 'complete-object-field',
                    'help-name'  : "address-space-name",
                },
                {
                    'field'     : 'name',
                    'type'      : 'enum',
                    'values'    : 'all',
                }
            ),
        },
        {
            # show address-space <as> { identifier-rules }
            'choices' : (
                {
                    'field'    : 'identifier-rules',
                    'type'     : 'enum',
                    'values'   : 'identifier-rules',
                    'action'   : 'display-table',
                    'obj-type' : 'address-space-identifier-rule',
                },
            )
        }
    ),
}

#
# FORMATS
#  perhaps these ought be moved next to their respective show commands
#

ADDRESS_SPACE_FORMAT = {
    'address-space' : {
        'field-orderings' : {
            'default'               : ['Idx', 'name', 'active', 'priority',
                                       'vlan-tag-on-egress'],
            'address-space-config'  : ['Idx', 'name', 'active',
                                       'vlan-tag-on-egress'],
            'brief'                 : ['Idx', 'name', 'vlan-tag-on-egress'],
            'details'               : ['Idx', 'name', 'active', 'priority',
                                        'description', 'vlan-tag-on-egress'],
            },
        'fields' : {
           'name'              : { 'verbose-name' : 'Address Space',
                                 },
           'active'            : {
                                 },
           'priority'          : {
                                 },
           'vlan-tag-on-egress': {
                                   'verbose-name' : 'Vlan Tag on Egress',
                                 },
           'description'       : {
                                 },
           }
        },
}


ADDRESS_SPACE_IDENTIFIER_RULE_FORMAT = {
    'address-space-identifier-rule' : {
        'field-orderings': {
            'default'              : [ 'Idx',    'address-space',  'rule',
                                       'active', 'priority', 'switch', 'ports',
                                       'tag',    'vlans'
                                     ],
            'address-space-config' : [ 'Idx',    'rule',     'description',
                                       'active', 'priority', 'switch', 'ports',
                                       'tag', 'vlans'
                                     ],
            },
        'fields': {
            'rule'           : { 'verbose-name' : 'Address Space Rule ID',
                               },
            'address-space'  : {
                                 'verbose-name' : 'Address Space'
                               },
            'active'         : {
                               },
            'description'    : {
                               },
            'ports'          : {
                               },
            'priority'       : {
                               },
            'rule'           : {
                               },
            'switch'         : {
                               },
            'tag'            : {
                               },
            'vlans'          : {
                               },
            }
        },
}
"""
