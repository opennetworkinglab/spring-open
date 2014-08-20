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
'''
def vns_origin_external(data):
    """
    Return origin-name when the vns wasn't created by the cli,
    return None otherwise.
    """
    pk = command.mi.pk('vns-definition')
    if not pk in data:
        return;

    vns = command.sdnsh.get_table_from_store('vns-definition',
                                             pk,
                                             data[pk])
    if len(vns) == 0:
        return None

    local = ['cli', 'rest']
    if 'origin' in vns[0] and not vns[0]['origin'] in local:
        return vns[0]['origin']
    return None
    

def vns_warn_external_config(data):
    """
    From the named vns, look up the entry, if it exists in the
    database, validate the 'origin' is either null, or 'cli',
    otherwise provide a warning about this particular vns
    (along with the originator name)
    """
    external_origin = vns_origin_external(data)
    if external_origin:
        command.sdnsh.warning('vns %s may not be intended for cli update, '
                              'origin/creator "%s" ' % (data['id'], external_origin))

command.add_action('vns-warn-external-config', vns_warn_external_config,
                    {'kwargs': {'data'      : '$data',}})

def complete_tenantname_preprocess(data):
    data['tenant']='default'
    if 'name' in data:
        data['tenant']= data['name']
        del data['name']
    else:
        current_mode=command.sdnsh.current_mode()
        if not current_mode.startswith('config-tenant'):
            data['tenant']='default'
        if current_mode.startswith('config-tenant'):
            for x in command.sdnsh.mode_stack:
                if x['mode_name'] == 'config-tenant':
                    data['tenant'] = x['obj']


command.add_completion('complete-tenantname-preprocess', complete_tenantname_preprocess,
                           {'kwargs': {'data': '$data',
                                       }})

def vns_confirm_external_delete(data):
    """
    From the named vns, look up the entry, if it exists in the
    database, validate the 'origin' is either null, or 'cli',
    otherwise provide a warning about this particular vns
    (along with the originator name)
    """
    external_origin = vns_origin_external(data)
    if external_origin:
        confirm = command.action_registry['confirm'][0] # XXX accessor?
        confirm('vns %s may not be intended for cli delete, '
                'origin/creator "%s"\nEnter y or yes to continue delete: '
                % (data['id'], external_origin))

command.add_action('vns-confirm-external-delete', vns_confirm_external_delete,
                    {'kwargs': {'data'      : '$data',}})

command.add_action('vns-confirm-external-delete', vns_confirm_external_delete,
                    {'kwargs': {'data'      : '$data',}})
'''
"""
#
# ----------------------------------------------------------------------
# vns-definition submode commands
#

# notice the submode is called 'config-def-vns', this was
# changed from 'config-vns-def' so that the vns submode,
# which is called 'config-vns', wouldn't be a string-prefix
# submode of config-vns.  Various command, such as the 
# vns-submode access-list command were showing up in the
# vns-definition submode due to this prefixing.

VNS_DEFINITION_COMMAND_DESCRIPTION = {
    'name'         : 'vns-definition',
    'help'         : 'Enter VNS definition submode',
    'mode'         : 'config*',
    'command-type' : 'config-submode',
    'obj-type'     : 'vns-definition',
    'submode-name' : 'config-tenant-def-vns',
    'feature'      : 'vns',
    'short-help'   : 'Enter VNS definition submode',
    'doc'          : 'vns|vns-definition',
    'doc-example'  : 'vns|vns-definition-example',
    'args' : (
        {
            'field'        : 'vnsname',
            'type'         : 'identifier',
            'completion' : ['complete-tenantname-preprocess',
                            'complete-object-field'],
            'other'      : 'vns-definition|vnsname',
#            'scoped'     : 'tenant',
            'syntax-help'  : 'Enter a vns name',
            'doc'          : 'vns|vns-id',
            'doc-example'  : 'vns|vns-id-example',
            'doc-include'  : [ 'type-doc' ],
            'action'       : (
                {
                    'proc' : 'vns-warn-external-config',
                },
                {
                   'proc'  : 'tenant-show-preprocess',
                },
                {
                    'proc' : 'push-mode-stack',
                },
            ),
            'no-action'    : (
                {
                    'proc' : 'vns-confirm-external-delete',
                },
                {
                   'proc'  : 'tenant-show-preprocess',
                },
                {
                    'proc' : 'delete-objects',
                },
            )
        }
    ),
}

vns_show_action = (
    {
        'proc'  : 'tenant-show-preprocess',
     },
    {
        'proc'  : 'query-table',
    },
   {
        'proc'   : 'display',
        'format' : 'vns-definition',
    },
)

VNS_SHOW_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'all-help'     : 'Show VNS details',
    'short-help'   : 'Show all defined VNSs belong to current tenant',
    'command-type' : 'display-table',
    'obj-type'     : 'vns-definition',
    'action'       : vns_show_action,
    'doc'          : 'vns|show',
    'doc-example'  : 'vns|show-example',
    'args'         : (
        'vns',
    ),
}
VNS_SHOW_ID_COMMAND_DESCRIPTION = {       
    'name'         : 'show',
    'obj-type'     : 'vns-definition',
    'mode'         : 'login',
    'command-type' : 'display-table',
    'action'       : vns_show_action,
    'short-help'   : 'Show specific VNS, identified by name',
    'doc'          : 'vns|show-id',
    'doc-example'  : 'vns|show-id-example',
    'args'         : (
        'vns',
        {
            'choices' : (
                {
                    'field'      : 'vnsname',
                    'type'       : 'identifier',
#                    'completion' : 'complete-object-field',
                    'completion' : ['complete-tenantname-preprocess',
                                    'complete-from-another'],
                    'other'      : 'vns-definition|vnsname',
                    'scoped'     : 'tenant',
                    'help-name'  : "vns-id",
                },
                {
                    'field'     : 'vnsname',
                    'type'      : 'enum',
                    'values'    : 'all',
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

VNS_SHOW_ID_DETAILS_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'obj-type'     : 'vns-definition',
    'mode'         : 'login',
    'command-type' : 'display-table',
    'short-help'   : 'Show VNS associated details based on name',
    'doc'          : 'vns|show-id',
    'doc-example'  : 'vns|show-id-example',
    'args'         : (
        'vns',
        {
            'choices' : (
                {
                    'field'      : 'vnsname',
                    'type'       : 'identifier',
                    'completion' : ['complete-tenantname-preprocess',
                                    'complete-from-another'],
                    'other'      : 'vns-definition|vnsname',
                    'scoped'     : 'tenant',
                    'help-name'  : "vns-id",
                },
                {
                    'field'     : 'vnsname',
                    'type'      : 'enum',
                    'values'    : 'all',
                }
            ),
        },
        {
            'choices' : (
                {
                    'field'      : 'vns-interface',
                    'type'       : 'enum',
                    'values'     : 'interfaces',
                    'obj-type'   : 'vns-interface',
                    'action'     : ({'proc'   : 'tenant-show-preprocess'},
                                    'legacy-cli'),
                    'short-help' : 'Show VNS associated interfaces',
                },
                {
                    'field'      : 'mac-address-table',
                    'type'       : 'enum',
                    'values'     : 'mac-address-table',
                    'obj-type'   : 'host-vns-interface-vns',
                    'action'     : ({'proc'   : 'tenant-show-preprocess'},
                                    'legacy-cli'),
                    'short-help' : 'Show VNS associated mac addresses',
                },
                {
                    'field'      : 'interface-rules',
                    'type'       : 'enum',
                    'values'     : 'interface-rules',
                    'action'     : ({'proc'   : 'tenant-show-preprocess'},
                                    'display-table'),
                    'obj-type'   : 'vns-interface-rule',
                    'short-help' : 'Show VNS configured interfaces-rules',
                    'doc'        : 'vns|show-id-interface-rules',
                },
                {
                    'field'      : 'access-lists',
                    'type'       : 'enum',
                    'values'     : 'access-lists',
                    'action'     : ({'proc'   : 'tenant-show-preprocess'},
                                    'display-table'),
                    'obj-type'   : 'vns-access-list-entry',
                    'detail'     : 'acl-brief',
                    'short-help' : 'Show VNS configured access-lists',
                    'doc'        : 'vns|show-id-access-lists',
                },
                {
                    'field'      : 'running-config',
                    'type'       : 'enum',
                    'values'     : 'running-config',
                    'action'     : ({'proc'   : 'tenant-show-preprocess'},
                                    'legacy-cli'),
                    'short-help' : 'Show VNS running-config',
                },
                {
                    'field'      : 'legacy-cli',
                    'type'       : 'enum',
                    'values'     : 'switch',
                    'action'     : ({'proc'   : 'tenant-show-preprocess'},
                                    'legacy-cli'),
                    'obj-type'   : 'vns-switch-ports',
                    'short-help' : 'Show VNS associated switches',
                },
                (
                    {
                        'field'      : 'legacy-cli',
                        'type'       : 'enum',
                        'values'     : 'flow',
                        'obj-type'   : 'vns-flow',
                        'action'     : ({'proc'   : 'tenant-show-preprocess'},
                                         'legacy-cli'),
                        'short-help' : 'Show VNS associated flows',
                        'doc'        : 'vns|show-id-flow',
                    },
                    {
                        'field'      : 'detail',
                        'type'       : 'enum',
                        'values'     : ('brief',
                                        'full-detail',
                                        'details',
                                        'summary',
                                       ),
                        'optional'   : True,
                        'short-help' : 'Detail level',
                        'doc'        : 'format|+'
                    }
                )
            )
        }
    ),
}

HOST_VNS_INTERFACE_VNS_FORMAT = {
    'host-vns-interface-vns' : { # with vns know, no vns column
        'source'          : 'only display',

        'field-orderings' : {
            'default'    : [ 'Idx', 'tenant', 'vns', 'address-space', 'host', 'vlan', 'ips', 'attachment-points', 'last-seen' ],
            'vns'        : [ 'Idx',        'address-space', 'host', 'vlan', 'ips', 'attachment-points', 'last-seen' ],
            },
        'fields' : {
            'Idx'               : { 'verbose-name' : '#',
                                  },
            'host'              : { 'verbose-name' : 'MAC Address',
                                    'formatter' : fmtcnv.print_host_and_alias,
                                  },
            'address-space'     : {
                                     'verbose-name' : 'Address Space',
                                  },
            'vlan'              : {
                                     'verbose-name' : 'VLAN',
                                  },
            'ips'               : { 'verbose-name' : 'IP Address',
                                    'formatter' : fmtcnv.print_ip_addresses,
                                    'entry_formatter' : fmtcnv.print_all_ip_addresses,
                                  },
            'attachment-points' : { 'verbose-name' : 'Attachment Point',
                                    'formatter' : fmtcnv.print_host_attachment_point,
                                    'entry_formatter' : fmtcnv.print_all_host_attachment_points,
                                  },
            'last-seen'         : { 'verbose-name' : 'Last Seen',
                                    'formatter' : fmtcnv.print_time_since_utc
                                  },
            'tenant'            : {'verbose-name' : 'Tenant',
                                   }
            }
        },
}



VNS_DEF_DESCRIPTION_COMMAND_DESCRIPTION = {
    'name'         : 'description',
    'mode'         : 'config-tenant-def-vns',
    'command-type' : 'config',
    'short-help'   : 'Provide description for a VNS instance',
    'doc'          : 'vns|description',
    'doc-example'  : 'vns|description-example',
    'args'         : (
        {
            'field' : 'description',
            'type'  : 'string',
        }
    ),
}


VNS_DEF_ACTIVE_COMMAND_DESCRIPTION = {
    'name'        : 'active',
    'mode'        : 'config-tenant-def-vns',
    'short-help'  : 'Set vns active',
    'doc'          : 'vns|active',
    'doc-example'  : 'vns|active-example',
    'doc-include'  : [ 'default' ],
    'args' : (),
    'action' : (
        {
            'proc' : 'write-fields',
            'data' : { 'active' : True },
            'syntax-help' : 'mark the vns as active',
        }
    ),
    'no-action' : (
        {
            'proc' : 'write-fields',
            'data' : { 'active' : False },
            'syntax-help' : 'mark the vns as inactive',
        }
    )
}

VNS_DEF_ORIGIN_COMMAND_DESCRIPTION = {
    'name'         : 'origin',
    'mode'         : 'config-tenant-def-vns',
    'command-type' : 'config',
    'short-help'   : 'Describe vns origin',
    'doc'          : 'vns|origin',
    'doc-example'  : 'vns|origin-example',
    'args' : (
        {
            'field'  : 'origin',
            'type'   : 'string',
            'action' : (
                {
                    'proc' : 'vns-warn-external-config',
                },
                {
                    'proc' : 'write-fields',
                },
            ),
        },
    ),
}

VNS_DEF_USE_ADDRESS_SPACE_COMMAND_DESCRIPTION = {
    'name'         : 'use',
    'short-help'   : 'Associate address space',
    'all-help'     : 'Associate VNS with other objects',
    'mode'         : 'config-tenant-def-vns',
    'command-type' : 'config',
    'obj-type'     : 'vns-definition',
    'doc'          : 'vns|use-address-space',
    'doc-example'  : 'vns|use-address-space-example',
    'doc-include'  : 'default', 
    'args' : (
        'address-space',
        {
            'field'           : 'address-space',
            'type'            : 'identifier',
            'completion'      : [ 
                                    'complete-object-field',
                                    'complete-from-another',
                                ],
            'other'           : 'address-space',
            'syntax-help'     : 'Enter associated address-space name',
            'optional-for-no' : False,
            'match-for-no'    : 'address-space',
        }
    )
}

VNS_DEF_ARP_MODE_COMMAND_DESCRIPTION = {
    'name'         : 'arp-mode',
    'short-help'   : 'Configure arp mode',
    'doc'          : 'vns|arp-mode',
    'doc-example'  : 'vns|arp-mode-example',
    'mode'         : 'config-tenant-def-vns',
    'command-type' : 'config',
    'doc-include'  : [ 'default' ],
    'args' : (
        {
            'field'  : 'arp-mode',
            'type'   : 'enum',
            'values' : ('always-flood', 'flood-if-unknown', 'drop-if-unknown'),
            'doc'    : 'vns|arp-mode-value-+',
        }
    ),
}

VNS_DEF_BROADCAST_COMMAND_DESCRIPTION = {
    'name'         : 'broadcast',
    'mode'         : 'config-tenant-def-vns',
    'command-type' : 'config',
    'short-help'   : 'Configure broadcast mode',
    'doc'          : 'vns|broadcast',
    'doc-example'  : 'vns|broadcast-example',
    'doc-include'  : [ 'default' ],
    'args' : (
        {
            'field'  : 'broadcast',
            'type'   : 'enum',
            'values' : ('always-flood', 'forward-to-known', 'drop'),
            'doc'    : 'vns|broadcast-value-+',
        }
    ),
}

VNS_DEF_DHCP_IP_COMMAND_DESCRIPTION = {
    'name'         : 'dhcp-ip',
    'mode'         : 'config-tenant-def-vns',
    'command-type' : 'config',
    'short-help'   : 'Configure dhcp ip address',
    'doc'          : 'vns|dhcp-ip',
    'doc-example'  : 'vns|dhcp-ip-example',
    'args' : (
        {
            'field'  : 'dhcp-ip',
            'type'   : 'ip-address',
            'syntax-help' : 'Enter an IP Address',
        }
    ),
}

VNS_DEF_DHCP_MODE_COMMAND_DESCRIPTION = {
    'name'         : 'dhcp-mode',
    'mode'         : 'config-tenant-def-vns',
    'command-type' : 'config',
    'short-help'   : 'Set dhcp mode',
    'doc'          : 'vns|dhcp-mode',
    'doc-example'  : 'vns|dhcp-mode-example',
    'doc-include'  : [ 'default' ],
    'args' : (
        {
            'field'  : 'dhcp-mode',
            'type'   : 'enum',
            'values' : ('always-flood', 'flood-if-unknown', 'static'),
            'doc'    : 'vns|dhcp-value-+',
        }
    ),
}

VNS_INTERFACE_RULE_COMMAND_DESCRIPTION = {
    'name'         : 'interface-rule',
    'mode'         : 'config-tenant-def-vns*',
    'command-type' : 'config-submode',
    'obj-type'     : 'vns-interface-rule',
    'parent-field' : 'vns',
    'submode-name' : 'config-tenant-def-vns-if-rule',
    'short-help'   : 'Enter interface-rule submode, configure vns details',
    'doc'          : 'vns|interface-rule',
    'doc-example'  : 'vns|interface-rule-example',
    'args' : (
        {
            'field'        : 'rule',
            'type'         : 'identifier',
            'syntax-help'  : 'Enter a vns interface rule name',
            'completion'   : 'complete-object-field',
            'scoped'       : True,
        }
    )
}

VNS_PRIORITY_COMMAND_DESCRIPTION = {
    'name'         : 'priority',
    'mode'         : 'config-tenant-def-vns',
    'command-type' : 'config',
    'short-help'   : 'Set vns priority',
    'doc'          : 'vns|priority',
    'doc-example'  : 'vns|priority-example',
    'args' : (
        {
            'field'     : 'priority',
            'base-type' : 'integer',
            'range'     : (0, 65535),
        }
    )
}

#
# ----------------------------------------------------------------------
# vns-def-if submode commands
#  (vns definition interface rules submode)
#

VNS_DEF_IF_ACTIVE_COMMAND_DESCRIPTION = {
    'name'         : 'active',
    'mode'         : 'config-tenant-def-vns-if-rule',
    'command-type' : 'config',
    'short-help'   : 'Set rule to Active',
    'doc'          : 'vns|interface-rule-active',
    'doc-example'  : 'vns|interface-rule-active-example',
    'doc-inlcude'  : [ 'default' ],
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

VNS_DEF_IF_ALLOW_MULTIPLE_COMMAND_DESCRIPTION = {
    'name'         : 'allow-multiple',
    'mode'         : 'config-tenant-def-vns-if-rule',
    'command-type' : 'config',
    'short-help'   : 'Enable multiple interface rule matches',
    'doc'          : 'vns|interface-rule-allow-multiple',
    'doc-example'  : 'vns|interface-rule-allow-multiple-example',
    'doc-include'  : [ 'default' ],
    'args'         : (),
    'action'       : (
        {
            'proc' : 'write-fields',
            'data' : { 'allow-multiple' : True }
        }
    ),
    'no-action'    : (
        {
            'proc'   : 'reset-fields',
            'fields' : [ 'allow-multiple' ]
        }
    )
}

#
# XXX vlan-tag-on-egress required in future releases when multiple vlans
# tagging are supported in vns egress.
#
#VNS_DEF_IF_VLAN_TAG_ON_EGRESS_TMP_COMMAND_DESCRIPTION = {
#    'name'         : 'vlan-tag-on-egress',
#    'short-help'   : 'Enable vlan tagging on egress',
#    'mode'         : 'config-def-vns-if-rule',
#    'command-type' : 'config',
#    'args'         : (),
#    'action'       : (
#        {
#            'proc' : 'write-fields',
#            'data' : { 'vlan-tag-on-egress' : True }
#        }
#    ),
#    'no-action'    : (
#        {
#            'proc'   : 'reset-fields',
#            'fields' : [ 'vlan-tag-on-egress' ]
#        }
#    )
# }

VNS_DEF_IF_DESCRIPTION_COMMAND_DESCRIPTION = {
    'name'         : 'description',
    'mode'         : 'config-tenant-def-vns-if-rule',
    'short-help'   : 'Provide description for interface rule',
    'doc'          : 'vns|interface-rule-description',
    'doc-example'  : 'vns|interface-rule-description-example',
    'command-type' : 'config',
    'args'         : (
        {
            'field' : 'description',
            'type'  : 'string',
        }
    ),
}

VNS_DEF_IF_PRIORITY_COMMAND_DESCRIPTION = {
    'name'         : 'priority',
    'mode'         : 'config-tenant-def-vns-if-rule',
    'command-type' : 'config',
    'short-help'   : 'Describe priority for interface rule',
    'doc'          : 'vns|interface-rule-priority',
    'doc-example'  : 'vns|interface-rule-priority-example',
    'args'         : (
        {
            'field' : 'priority',
            'value' : 'integer',
        }
    ),
}


VNS_DEF_IF_MATCH_MAC_COMMAND_DESCRIPTION = {
    'name'         : 'match',
    'mode'         : 'config-tenant-def-vns-if-rule',
    'command-type' : 'config',
    'short-help'   : 'Associate mac (host) with interface rule',
    'all-help'     : 'Configure a match for this interface rule',
    'doc'          : 'vns|interface-rule-match-mac',
    'doc-example'  : 'vns|interface-rule-match-mac-example',
    'doc-all'      : 'address-space|match',
    'data'         : { 'mac' : None }, # for no command reset
    'obj-type'     : 'vns-interface-rule',
    'args'         : (
        {
            'token'        : 'mac',
        },
        {
            'field'           : 'mac',
            'type'            : 'host',
            'help-name'       : 'host mac or alias',
            'completion'      : ['complete-object-field',
                                 'complete-from-another',
                                ],
            'scoped'          : True,
            'data-handler'    : 'alias-to-value',
            'other'           : 'host',
            'optional-for-no' : True,
            'doc'             : 'vns|interface-rule-match-mac-field',
        }
    ),
}


VNS_DEF_IF_MATCH_IP_SUBNET_COMMAND_DESCRIPTION = {
    'name'         : 'match',
    'mode'         : 'config-tenant-def-vns-if-rule',
    'command-type' : 'config',
    'short-help'   : 'Associate ip-subnet (ip or cidr range) for interface rule',
    'doc'          : 'vns|interface-rule-match-ip-subnet',
    'doc-example'  : 'vns|interface-rule-match-ip-subnet-example',
    'data'         : { 'ip-subnet' : None }, # for no command reset
    'args'         : (
        {
            'token'        : 'ip-subnet',
        },
        {
            'field'           : 'ip-subnet',
            'type'            : 'cidr-range',
            'help-name'       : 'ip address (10.10.10.10), or cidr (10.20.30.0/24)',
            'optional-for-no' : True,
            'doc'             : 'vns|interface-rule-match-ip-subnet-field',
        }
    ),
}


VNS_DEF_IF_MATCH_SWITCH_COMMAND_DESCRIPTION = {
    'name'         : 'match',
    'mode'         : 'config-tenant-def-vns-if-rule',
    'command-type' : 'config',
    'obj-type'     : 'vns-interface-rule',
    'short-help'   : 'Associate switch with interface rule',
    'doc'          : 'vns|interface-rule-match-switch',
    'doc-example'  : 'vns|interface-rule-match-switch-example',
    'data'         : { # for no command reset
                       'switch' : None,
                       'ports'  : None,
                     },
    'args'         : (
        {
            'token'        : 'switch',
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
                    'data-handler'    : 'warn-missing-interface',
                    'optional-for-no' : True,
                },
            ),
        },
    ),
}


VNS_DEF_IF_MATCH_TAGS_COMMAND_DESCRIPTION = {
    'name'         : 'match',
    'mode'         : 'config-tenant-def-vns-if-rule',
    'command-type' : 'config',
    'short-help'   : 'Associate tags with interface rule',
    'doc'          : 'vns|interface-rule-match-tag',
    'doc-example'  : 'vns|interface-rule-match-tag-example',
    'data'         : { # for no command reset
                       'tags' : None,
                     },
    'args' : (
        {
            'token'        : 'tags',
        },
        {
            'field'           : 'tags',
            'type'           : 'string',
            'optional-for-no' : True,
        }
    ),
}

# Remove match vlan command temporarily on gregor's requres
# the running config can still generate these commands, but
# they won't get read correctly
VNS_DEF_IF_MATCH_VLANS_COMMAND_DESCRIPTION = {
    'name'         : 'match',
    'mode'         : 'config-tenant-def-vns-if-rule',
    'command-type' : 'config',
    'short-help'   : 'Associate vlans with interface rule',
    'doc'          : 'vns|interface-rule-match-vlans',
    'doc-example'  : 'vns|interface-rule-match-vlans-example',
    'data'         : { # for no command reset
                       'vlans' : None,
                     },
    'args'         : (
        {
            'token'        : 'vlans',
        },
        {
            'field'           : 'vlans',
            'value'           : 'string',
            'help-name'       : 'Vlan number (0-4096) or range, or list',
            'optional-for-no' : True,
        }
    ),    
}

#
# ----------------------------------------------------------------------
# vns submode commands
#


VNS_COMMAND_DESCRIPTION = {
    'name'                : 'vns',
    'short-help'          : 'Enter VNS submode, manage access lists',
    'mode'                : 'config*',
    'feature'             : 'vns',
    'no-supported'        : False,
    'command-type'        : 'config-submode',
    'obj-type'            : 'vns-definition',
    'submode-name'        : 'config-tenant-vns',
    'syntax-help'         : 'Enter a vns name',
    'create'              : False,
    'doc'                 : 'vns|vns',
    'doc-example'         : 'vns|vns-example',
    'args' : (
        {
            'field'        : 'vnsname',
            'type'         : 'identifier',
            'completion'   : ['complete-tenantname-preprocess',
                              'complete-from-another'],
            'other'        : 'vns-definition|vnsname',
            'scoped'       : 'tenant',
            'syntax-help'  : 'Enter a vns name',
            'doc'          : 'vns|vns-id',
            'doc-include'  : [ 'type-doc' ],
            'action'       : (               
                              {
                               'proc'  : 'tenant-show-preprocess',
                               },
                              {
                               'proc' : 'push-mode-stack',
                               }
                             ),

        }
    )
}

VNS_INTERFACE_COMMAND_DESCRIPTION = {
    'name'                : 'interface',
    'short-help'          : 'Enter VNS-if submode',
    'mode'                : 'config-tenant-vns*',
    'command-type'        : 'config-submode',
    'obj-type'            : 'vns-interface-config',
    'parent-field'        : 'vns',
    'current-mode-obj-id' : 'vns-definition',
    'submode-name'        : 'config-tenant-vns-if',
    'syntax-help'         : 'Enter an interface name',
    'doc'                 : 'vns|vns-interface',
    'doc-example'         : 'vns|vns-interface-example',
    'args' : (
        {
            'field'        : 'interface',
            'completion'   : 'complete-object-field',
            'doc'          : 'vns|vns-interface-field',
        }
    )
}

VNS_ACCESS_GROUP_COMMAND_DESCRIPTION = {
    'name'         : 'access-group',
    'short-help'   : 'Associate interface with access-list',
    'mode'         : 'config-tenant-vns-if',
    'command-type' : 'config',
    'obj-type'     : 'vns-interface-access-list',
    'parent-field' : 'vns-interface',
    # 'current-mode-obj-id' : 'vns-definition',
    'submode-name' : 'config-tenant-vns-if',
    'doc'          : 'vns|vns-access-group',
    'doc-example'  : 'vns|vns-access-group-example',
    'args' : (
        {
            'field'           : 'vns-access-list',
            'type'            : 'string',
            'completion'      : 'complete-from-another',
            'other'           : 'vns-access-list|name',
            'optional-for-no' : False,
        },
        {
            'field'           : 'in-out',
            'type'            : 'enum',
            'values'          : ('in', 'out'),
            'syntax-help'     : 'apply access-list in this direction',
            'optional-for-no' : False,
            'doc'             : 'vns|vns-access-group-direction-+',
        }
    ),
    'action' : (
        {
            'proc'      : 'convert-vns-access-list',
        },
        {
            'proc'      : 'write-object',
        },
    ),
    'no-action' : (
        {
            'proc'      : 'convert-vns-access-list',
        },
        {
            'proc'      : 'delete-objects',
        },
    )
}

SHOW_VNS_ACCESS_GROUP_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'config-tenant-vns-if*',
    'action'       : 'display-table',
    'command-type' : 'display-table',
    'scoped'       : True, # displayed entries filtered by pushed obj id
    'obj-type'     : 'vns-interface-access-list',
    'short-help'   : 'show access-group details',
    'args'         : (
        'access-group',
    )
}


SHOW_VNS_ACCESS_LIST_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : ['config-tenant-vns-*'],
    'short-help'   : 'Show VNS access lists',
    'action'       : 'display-table',
    'command-type' : 'display-table',
    'obj-type'     : 'vns-access-list',
    'scoped'       : True, # displayed entries filtered by pushed obj id
    'args'         : (
        'access-list',
    )
}

SHOW_VNS_INTERFACES_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : ['config-tenant-vns', 'config-tenant-vns-if'],
    'short-help'   : 'Show VNS associated interfaces',
    'doc'          : 'vns|vns-show-interface',
    'obj-type'     : 'vns-interface',
    'args'         : ( 
        {
            'token'  : 'interfaces',
            'scoped' : True,
            'action' :  ({'proc'   : 'tenant-show-preprocess'},
                         'legacy-cli'),
        }
    )
}

SHOW_VNS_ACCESS_LIST_ENTRY_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'config-tenant-vns*',
    'short-help'   : 'show VNS access list rules',
    'action'       : 'display-table',
    'command-type' : 'display-table',
    'obj-type'     : 'vns-access-list-entry',
    'detail'       : 'acl-brief',
    'args'         : (
        'access-list-entry',
    ),
}

VNS_ACCESS_LIST_COMMAND_DESCRIPTION = {
    'name'         : 'access-list',
    'mode'         : 'config-tenant-vns*',
    'command-type' : 'config-submode',
    'obj-type'     : 'vns-access-list',
    'parent-field' : 'vns',
    'submode-name' : 'config-tenant-vns-acl',
    'syntax-help'  : 'Enter an access list name',
    'short-help'   : 'Enter vns access-list submode',
    'doc'          : 'vns|vns-access-list',
    'doc-example'  : 'vns|vns-access-list-example',
    'args' : (
        {
            'field'        : 'name',
            'completion'   : 'complete-object-field',
            'doc'          : 'vns|vns-access-list-name'
        }
    )
}


VNS_ACCESS_LIST_DESCRIPTION_COMMAND_DESCRIPTION = {
    'name'         : 'description',
    'mode'         : 'config-tenant-vns-acl',
    'short-help'   : 'Provide a description for a VNS access list',
    'doc'          : 'vns|vns-access-list-description',
    'doc-example'  : 'vns|vns-access-list-description-example',
    'command-type' : 'config',
    'args'         : (
        {
            'field' : 'description',
            'type'  : 'string',
        }
    ),
}


VNS_ACCESS_LIST_PRIORITY_COMMAND_DESCRIPTION = {
    'name'         : 'priority',
    'mode'         : 'config-tenant-vns-acl',
    'short-help'   : 'Set VNS priority',
    'doc'          : 'vns|vns-access-list-priority',
    'doc-example'  : 'vns|vns-access-list-priority-example',
    'doc-include'  : [ 'default', 'range' ],
    'command-type' : 'config',
    'args' : (
        {
            'field'     : 'priority',
            'base-type' : 'integer',
            'range'     : (0, 65535),
        }
    )
}


#
# ----------------------------------------------------------------------
# vns access list command
#

SRC_IP_MATCH = {
    'choices' : (
        (
            {
                'field' : 'src-ip',
                'type'  : 'ip-address-not-mask',
                'doc'   : 'vns|vns-access-list-ip-and-mask-ip',
            },
            {
                'field'        : 'src-ip-mask',
                'type'         : 'inverse-netmask',
                'data'         : {
                                  'dst-ip'      : '0.0.0.0',
                                  'dst-ip-mask' : '255.255.255.255',
                                 },
                'doc'          : 'vns|vns-access-list-ip-and-mask-mask',
            },
        ),
        (
            {
                'field'    : 'src-ip',
                'type'     : 'ip-address-not-mask',
                'data'     : {
                               'src-ip-mask' : '0.0.0.0',
                               'dst-ip'      : '0.0.0.0',
                               'dst-ip-mask' : '255.255.255.255',
                             },
                'doc'      : 'vns|vns-access-list-ip-only',
            },
        ),
        (
            {
                'field'        : 'src-ip',
                'type'         : 'cidr-range',
                'help-name'    : 'src-cidr',
                'data-handler' : 'split-cidr-data-inverse',
                'dest-ip'      : 'src-ip',
                'dest-netmask' : 'src-ip-mask',
                'data'         : {
                                  'dst-ip'      : '0.0.0.0',
                                  'dst-ip-mask' : '255.255.255.255',
                                 },
                'doc'          : 'vns|vns-access-list-cidr-range',
            }
        ),
        (
            {
                'token'  : 'any',
                'data'   : {
                              'src-ip'      : '0.0.0.0',
                              'src-ip-mask' : '255.255.255.255',
                              'dst-ip'      : '0.0.0.0',
                              'dst-ip-mask' : '255.255.255.255',
                           },
                'doc'    : 'vns|vns-access-list-ip-any',
            }
        ),
    )
}


SRC_PORT_MATCH = (
    {
        'field'  : 'src-tp-port-op',
        'type'   : 'enum',
        'values' : ('eq', 'neq'),
        'doc'    : 'vns|vns-access-list-port-op-+',
    },
    {
        'choices' : (
            {
                'field'        : 'src-tp-port',
                'base-type'    : 'hex-or-decimal-integer',
                'range'        : (0,65535),
                'data-handler' : 'hex-to-integer',
                'doc'          : 'vns|vns-access-list-port-hex',
                'doc-include'  : [ 'range' ],
            },
            {
                'field'   : 'src-tp-port',
                'type'    : 'enum',
                'values'  : fmtcnv.tcp_name_to_number_dict,
                'permute' : 'skip',
                'doc'     : 'vns|vns-access-list-port-type',
            },
        ),
    },
)


DST_IP_MATCH = {
    'choices' : (
        (
            {
                'field' : 'dst-ip',
                'type'  : 'ip-address-not-mask',
                'doc'   : 'vns|vns-access-list-ip-and-mask-ip',
            },
            {
                'field' : 'dst-ip-mask',
                'type'  : 'inverse-netmask',
                'doc'   : 'vns|vns-access-list-ip-and-mask-mask',
            },
        ),
        (
            {
                'field'    : 'dst-ip',
                'type'     : 'ip-address-not-mask',
                'data'     : {
                                'dst-ip-mask' : '0.0.0.0',
                             },
                'doc'      : 'vns|vns-access-list-ip-only',
            },
        ),
        (
            {
                'field'        : 'dst-ip',
                'type'         : 'cidr-range',
                'help-name'    : 'dst-cidr',
                'data-handler' : 'split-cidr-data-inverse',
                'dest-ip'      : 'dst-ip',
                'dest-netmask' : 'dst-ip-mask',
                'doc'          : 'vns|vns-access-list-cidr-range',
            },
        ),
        (
            {
                'token'  : 'any',
                'data'   : {
                              'dst-ip'      : '0.0.0.0',
                              'dst-ip-mask' : '255.255.255.255',
                           },
                'doc'    : 'vns|vns-access-list-ip-any',
            }
        ),
    )
}


DST_PORT_MATCH = (
    {
        'field' : 'dst-tp-port-op',
        'type'  : 'enum',
        'values' : ('eq', 'neq'),
        'doc'          : 'vns|vns-access-list-port-op+',
    },
    {
        'choices' : (
            {
                'field'        : 'dst-tp-port',
                'base-type'    : 'hex-or-decimal-integer',
                'range'        : (0,65535),
                'data-handler' : 'hex-to-integer',
                'doc'          : 'vns|vns-access-list-port-hex',
            },
            {
                'field'   : 'dst-tp-port',
                'type'    : 'enum',
                'values'  : fmtcnv.tcp_name_to_number_dict,
                'permute' : 'skip'
            },
        ),
    }
)


# Unfortunatly, the model defines the compound key using the
# rule field as a character field, not an integer. 
# Since '00' is then the same rule as '0', two rows could be
# created describing the same integer rule. 
# Here the character string is normalized.  
# (This may be the result of having a requirement that all fields used to
# construct compound keys must be character fields)
#
# Additionally, for tcp and udp matches, the *tp_port_op needs
# to be set.
#
def vns_normalize_rule(data):
    data['rule'] = str(int(data['rule']))

    if data.get('type', '') in ['tcp', 'udp']:
        if not 'src-tp-port-op' in data or data['src-tp-port-op'] == None:
           data['src-tp-port-op'] = 'any'
        if not 'dst-tp-port-op' in data or data['dst-tp-port-op'] == None:
           data['dst-tp-port-op'] = 'any'


command.add_action('vns-normalize-rule', vns_normalize_rule,
                   {'kwargs' : { 'data' : '$data' }})
#

VNS_ACCESS_LIST_ENTRY_COMMAND_DESCRIPTION = {
    'name'         : { 'type'       : 'pattern',
                       'pattern'    : r'\d',
                       'field'      : 'rule',
                       'title'      : '<acl rule number>',
                       'completion' : 'complete-object-field',
                       'obj-type'   : 'vns-access-list-entry',
                     },
    'mode'         : 'config-tenant-vns-acl',
    'short-help'   : 'Define ACL details for this access-list',
    'doc'          : 'vns|vns-access-list-entry',
    'doc-example'  : 'vns|vns-access-list-entry-example',
    'command-type' : 'config-object',
    'parent-field' : 'vns-access-list',
    'obj-type'     : 'vns-access-list-entry',
    'data'         : {
                        'ether-type'     : None,
                        'src-ip'         : None,
                        'src-ip-mask'    : None,
                        'dst-ip'         : None,
                        'dst-ip-mask'    : None,
                        'src-tp-port'    : None,
                        'src-tp-port-op' : None,
                        'dst-tp-port'    : None,
                        'dst-tp-port-op' : None,
                        'src-mac'        : None,
                        'dst-mac'        : None,
                        'vlan'           : None,
                        'icmp-type'      : None,
                     },
    'args'  : (
        {
            'field'  : 'action',
            'type'   : 'enum',
            'values' : ( 'permit', 'deny' ),
            'doc'    : 'vns|vns-access-list-entry-action-+',
        },
        {
            'choices' : (
                (
                    {
                        'choices' : (
                            {
                                'field'  : 'type',
                                'type'   : 'enum',
                                'values' : ('ip','tcp','udp'),
                                'doc'    : 'vns|vns-access-list-entry-type-+',
                            },
                            {
                                'field'        : 'type',
                                'base-type'    : 'hex-or-decimal-integer',
                                'range'        : (0,255),
                                'help-name'    : 'ip protocol',
                                'data-handler' : 'hex-to-integer',
                                'doc'          : 'vns|vns-access-entry-type-ip-protocol',
                                'doc-include'  : [ 'range' ],
                            },
                        )
                    },
                    # Complexity arises from the SRC_IP match part 
                    # being, required, while the port match
                    # is optional, as is the DST_IP match, but the
                    # DST_PORT_MATCH is only possible to describe when
                    # the DST_IP part is included
                    SRC_IP_MATCH,
                    {
                        'optional' : True,
                        'optional-for-no' : True,
                        'args' : SRC_PORT_MATCH,
                    },
                    {
                        'optional' : True,
                        'optional-for-no' : True,
                        'args' : (
                            DST_IP_MATCH,
                            {
                                'optional' : True,
                                'optional-for-no' : True,
                                'args' : DST_PORT_MATCH,
                            },
                        ),
                    },
                ),
                (
                    # Only allow the icmp type for icmp syntax
                    # this further increases the syntax complexity
                    {
                        'field'  : 'type',
                        'type'   : 'enum',
                        'values' : 'icmp',
                        'doc'    : 'vns|vns-access-list-ip-+',
                    },
                    SRC_IP_MATCH,
                    {
                        'optional' : True,
                        'optional-for-no' : True,
                        'args' : SRC_PORT_MATCH
                    },
                    {
                        'optional' : True,
                        'optional-for-no' : True,
                        'args' : (
                            DST_IP_MATCH,
                            {
                                'optional' : True,
                                'optional-for-no' : True,
                                'args' : DST_PORT_MATCH,
                            },
                        ),
                    },
                    {
                        'field'           : 'icmp-type',
                        'optional'        : True,
                        'optional-for-no' : True,
                        'base-type'       : 'hex-or-decimal-integer',
                        'range'           : (0,255),
                        'data-handler'    : 'hex-to-integer',
                    },
                ),
                (
                    {
                        'field'     : 'type',
                        'type'      : 'enum',
                        'values'    : 'mac',
                        'doc'       : 'vns|vns-access-list-mac',
                    },
                    {
                        'choices'   : (
                            {
                                'token' : 'any',
                            },
                            {
                                'field' : 'src-mac',
                                'type'  : 'host',
                                'completion'   : [ 'complete-alias-choice',
                                                 ],
                                'other'        : 'host|mac',
                                'data-handler' : 'alias-to-value',
                            },
                        )
                    },
                    {
                        'choices'   : (
                            {
                                'token' : 'any',
                            },
                            {
                                'field' : 'dst-mac',
                                'type'  : 'host',
                                'completion'   : [ 'complete-alias-choice',
                                                 ],
                                'other'        : 'host|mac',
                                'data-handler' : 'alias-to-value',
                            },
                        ),
                    },
                    {
                        'optional'  : True,
                        'choices' : (
                            {
                                'field'           : 'ether-type',
                                'base-type'       : 'hex-or-decimal-integer',
                                'range'           : (1536,65536),
                                'data-handler'    : 'hex-to-integer',
                                'doc'             : 'vns|vns-access-list-ether-type',
                            },
                            {
                                'field'           : 'ether-type',
                                'type'            : 'enum',
                                'values'          : fmtcnv.ether_type_to_number_dict,
                                'permute'         : 'skip'
                            },
                        )
                    },
                    {
                        'field'           : 'vlan',
                        'tag'             : 'vlan',
                        'base-type'       : 'hex-or-decimal-integer',
                        'range'           : (1,4096), # no zero?  really?
                        'data-handler'    : 'hex-to-integer',
                        'optional'        : True,
                        'optional-for-no' : True,
                    },
                ),
            )
        }
    ),
    'action' : (
                    {
                        'proc'  : 'vns-normalize-rule',
                        'field' : 'rule',
                    },
                    {
                        'proc' : 'write-object',
                    },
        ),
}

VNS_DEFINITION_FORMAT = {
    'vns-definition' : {
        'field-orderings' : {
            'default'     : [ 'Idx', 'tenant','vnsname', 'active', 'priority', 'arp-mode',
                              'dhcp-mode', 'dhcp-ip', 'broadcast',
                              'address-space' ],
            'vns-config'  : [ 'Idx', 'tenant','vnsname','active', 'priority', 'arp-mode',
                              'dhcp-mode', 'dhcp-ip', 'broadcast',
                              'address-space', ],
            'brief'       : [ 'Idx', 'tenant', 'vnsname', 'address-space', ]
            },
        'fields' : {
           'vnsname'              : { 'verbose-name' : 'VNS ID',
                               },
           'active'          : {
                               },
           'address-space'   : { 'verbose-name' : 'Address Space',
                               },
           'priority'        : {
                               },
           'arp-mode'        : { 'verbose-name' : 'ARP Config Mode',
                               },
           'dhcp-mode'       : { 'verbose-name' : 'DHCP Config Mode' ,
                               },
           'dhcp-ip'         : { 'verbose-name' : 'DHCP Ip' ,
                               },
           'broadcast'       : { 'verbose-name' : 'Broadcast Mode' ,
                               },
           'address-space'   : {
                                 'verbose-name' : 'Address Space',
                               },
           'tenant'   : {
                                 'verbose-name' : 'Tenant',
                               },
                    
           }
        },
}

VNS_INTERFACE_RULE_FORMAT = {
    'vns-interface-rule' : {
        'field-orderings': {
            'default'    : [ 'Idx', 'tenant', 'vnsname', 'rule', 'description',
                             'allow-multiple', 'active', 'priority',
                             'mac', 'vlans', 'ip-subnet', 'switch', 'ports', 'tags'
                           ],
            'vns-config' : [ 'Idx', 'rule', 'description', 'allow-multiple', 'active',
                             'priority', 'mac', 'vlans', 'ip-subnet', 'switch', 'ports', 'tags'
                           ],
            },
        'fields': {
            'rule'           : { 'verbose-name' : 'VNS Rule ID',
                               },
            'tenant'         : {
                                'verbose-name' : 'Tenant',
                                },
            'vnsname'            : { 'verbose-name' : 'VNS ID'
                               },
            'mac'            : {
                                 'formatter' : fmtcnv.print_host_and_alias
                               },
            'tags'           : {
                               },
            'active'         : {
                               },
            'allow-multiple' : {
                               },
            'description'    : {
                               },
            'ip-subnet'      : {
                               },
            'ports'          : {
                               },
            'priority'       : {
                               },
            'rule'           : {
                               },
            'switch'         : {
                               },
            'tags'           : {
                               },
            'vlans'          : {
                               },
            }
        },
}


VNS_DISPLAY_VNS_INTERFACE_FORMAT = {
    'display-vns-interface' : {
        'field-orderings' : {
            'default'    : ['Idx', 'tenant', 'vnsname', 'address-space', 'id', 'rule', 'mac', 'vlan',
                            'ips', 'attachment-points', 'last-seen'],
            'vns-config' : ['Idx',        'id', 'rule', 'mac', 'vlan',
                             'ips', 'attachment-points', 'last-seen']
            },
        'fields': {
            'tenant'            : {
                                'verbose-name' : 'Tenant',
                                },
            'vnsname'           : {
                                'verbose-name' : 'VNS ID',
                                },
            'id'                : {
                                  },
            'rule'              : {
                                  },
            'address-space'     : {
                                     'verbose-name' : 'Address Space',
                                  },
            'mac'               : { 'verbose-name' : 'MAC Address',
                                    'formatter' : fmtcnv.print_host_and_alias,
                                  },
            'vlan'              : { 'verbose-name' : 'VLAN',
                                  },
            'ips'               : {
                                    'formatter' : fmtcnv.print_ip_addresses,
                                    'entry-formatter' : fmtcnv.print_all_ip_addresses,
                                  },
            'attachment-points' : { 'formatter' : fmtcnv.print_host_attachment_point,
                                    'entry-formatter' : fmtcnv.print_all_host_attachment_points,
                                  },
            'last-seen'         : {
                                    'formatter' : fmtcnv.print_time_since_utc_timestr
                                  }
            }
        },
}


VNS_INTERFACE_DISPLAY_FORMAT = {
    'vns-interface-display' : {
        'field-orderings' : {
            'default'    : ['Idx', 'tenant', 'vnsname', 'address-space', 'id', 'rule', 'mac', 'vlan',
                            'ips', 'attachment-points', 'last-seen'],
            'vns-config' : ['Idx',        'id', 'address-space', 'rule', 'mac', 'vlan',
                             'ips', 'attachment-points', 'last-seen']
            },
        'fields': {
            'id'                : {
                                  },
            'tenant'            : {
                                'verbose-name' : 'Tenant',
                                  },
            'vnsname'           : {
                                'verbose-name' : 'VNS ID',
                                  },
            'rule'              : {
                                  },
            'mac'               : { 'verbose-name' : 'MAC Address',
                                    'formatter' : fmtcnv.print_host_and_alias,
                                  },
            'vlan'              : { 'verbose-name' : 'VLAN',
                                  },
            'ips'               : {
                                    'formatter' : fmtcnv.print_ip_addresses,
                                    'entry-formatter' : fmtcnv.print_all_ip_addresses,
                                  },
            'attachment-points' : { 'formatter' : fmtcnv.print_host_attachment_point,
                                    'entry-formatter' : fmtcnv.print_all_host_attachment_points,
                                  },
            'address-space'     : {
                                     'verbose-name' : 'Address Space',
                                  },
            'last-seen'         : {
                                    'formatter' : fmtcnv.print_time_since_utc
                                  }
            }
        },
}


VNS_INTERFACE_MACS_FORMAT = {
    'vns-interface-macs'  : {
        'field-orderings' : {
            'default'    : [ 'Idx', 'tenant', 'vns', 'address-space',
                             'veth', 'mac', 'vlan', 'ips', 'tags', 'last-seen'],
            'scoped'     : [ 'Idx',        'address-space',
                             'veth', 'mac', 'vlan', 'ips', 'tags', 'last-seen'],
            },
        'fields': {
            'veth'              : {
                                    'verbose-name' : 'Virtual I/F',
                                  },
            'tenant'            : {
                                    'verbose-name' : 'Tenant',
                                  },
            'vnsname'               : {
                                  'verbose-name': 'VNS ID'
                                  },
            'mac'               : {
                                    'verbose-name' : 'MAC Address',
                                    'formatter' : fmtcnv.print_host_and_alias,
                                  },
            'vlan'              : {
                                     'verbose-name' : 'VLAN',
                                  },
            'ips'               : {
                                     'verbose-name' : 'IP Address',
                                     'formatter' : fmtcnv.print_ip_addresses,
                                     'entry-formatter' : fmtcnv.print_all_ip_addresses,
                                  },
            'tags'              : {
                                  },
            'address-space'     : {
                                     'verbose-name' : 'Address Space',
                                  },
            'last-seen'         : {
                                    'verbose-name' : 'Last Seen',
                                    'formatter' : fmtcnv.print_time_since_utc
                                  }
            }
        },
}


VNS_INTERFACE_PHYS_FORMAT = {
    'vns-interface-phys'  : {
        'field-orderings' : {
            'default'     : ['Idx', 'id', 'address-space', 'switch', 'last-seen' ],
            },

        'fields'          : {
            'id'            : { 'formatter' : fmtcnv.print_vns_physical_interface_id,
                                'verbose-name' : 'Physical I/F',
                              },
            'switch'        : {
                                'formatter'  : fmtcnv.print_switch_and_alias
                              },
            'address-space'     : {
                                     'verbose-name' : 'Address Space',
                                  },
            'last-seen'     : {
                                'verbose-name' : 'Last Seen',
                                'formatter' : fmtcnv.print_time_since_utc
                              }
        }
    },
}


VNS_VNS_SWITCH_PORTS_FORMAT = {
    'vns-switch-ports'    : {
        'field-orderings' : {
            'default'     : [ 'Idx', 'tenant','vns', 'switch', 'port', 'reason' ],
            },

        'fields'          : {
            'tenant'      : {
                                'verbose-name' : 'Tenant',
                            },
            'vnsname'     : {
                                'verbose-name' : 'VNS ID',
                            },
            'switch'      : {
                              'formatter' : fmtcnv.replace_switch_with_alias
                            },
            'port'        : {
                              'verbose-name' : 'OF Port #',
                              'formatter' : fmtcnv.decode_openflow_port
                            },
            'switch-port' : {
                              'verbose-name' : 'Phy I/F'
                            },
            'reason'      : {
                              'formatter' : fmtcnv.print_host_list_and_alias
                            },
            }
    },
}


VNS_SWITCH_PORTS_VNS_PORTS_FORMAT = {
    'switch-ports-vns'    : {
        'field-orderings' : {
            'default'     : [ 'Idx', 'switch', 'port', 'tenant','vnsname', 'reason' ],
            },

        'fields'          : {
            'tenant'      : {
                                'verbose-name' : 'Tenant',
                                  },
            'vnsname'     : { 'verbose-name' : 'VNS',
                              'formatter' : fmtcnv.print_vns_count_dict
                              },
            'switch'      : {
                              'formatter' : fmtcnv.replace_switch_with_alias
                            },
            'port'        : {
                               'verbose-name' : 'OF Port #',
                              'formatter' : fmtcnv.decode_openflow_port
                            },
            'reason'      : {
                              'formatter' : fmtcnv.print_host_list_and_alias
                            },
            }
    },
}


VNS_VNS_ACCESS_LIST_FORMAT = {
    'vns-access-list': {
        'show-this'       : [
                                ['vns-access-list', 'default'],
                                ['vns-access-list-entry', 'acl-brief']
                            ],

        'field-orderings' : {
            'default'    : [ 'tenant', 'vns', 'name', 'priority', 'description' ],
            'vns-config' : [        'name', 'priority', 'description' ]
            },
        'fields' : {
            'name'       : {
                           },
            }
        },
}


VNS_ACCESS_LIST_ENTRY_FORMAT = {
    'vns-access-list-entry': {
        'field-orderings' : {
            'default'         : [ 'Idx', 'vns', 'vns-access-list', 'rule', 'action', 'type',
                                   'src-ip', 'src-ip-mask',
                                   'dst-ip', 'dst-ip-mask',
                                        'src-tp-port-op', 'src-tp-port',
                                   'dst-tp-port-op', 'dst-tp-port',
                                   'icmp-type',
                                   'src-mac', 'dst-mac', 'ether-type',
                                   'vlan'
                                 ],
            'vns-config'       : ['Idx',
                                  'vns-access-list', 'rule', 'action', 'type',
                                  'src-ip', 'src-ip-mask',
                                   'dst-ip', 'dst-ip-mask' ,
                                   'src-tp-port-op', 'src-tp-port',
                                   'dst-tp-port-op', 'dst-tp-port',
                                   'icmp-type',
                                   'src-mac', 'dst-mac', 'ether-type',
                                   'vlan'
                                 ],
            'acl-brief'        : ['vns-access-list', 'rule', 'action', 'type', 'acl-text'
                                 ],
            'scoped-acl-brief' : [ 'rule', 'action', 'type', 'acl-text'
                                 ],
            },
        'fields' : {
            #
            # vns-acl-entry fields are a bit uncommon.  the action and type
            # can only be configured via the create portion of the command,
            # while many other fields requre specific validation so that
            # alternate values can be replaced for some keywords
            #
            # the values of the 'validate' field is the name of the
            # def to call (in the SDNSh class)
            #
            'vns-access-list'  : { 'verbose-name' : 'Acl'
                                 },
            'rule'             : { 'verbose-name' : 'Seq',
                                 },
            'action'           : { 'verbose-name' : 'Action',
                                 },
            'type'             : { 'verbose-name' : 'Type',
                                 },
            'src-ip'           : {
                                   'verbose-name' : 'Src-Ip'
                                 },
            'src-ip-mask'      : {
                                    'verbose-name' : 'Src-Ip-Mask'
                                 },
            'dst-ip'           : {
                                   'verbose-name' : 'Dst-Ip' },
            'dst-ip-mask'      : {
                                   'verbose-name' : 'Dst-Ip-Mask' },
            'src-tp-port-op'   : {
                                   'verbose-name' : 'Src-Port-Op' },
            'src-tp-port'      : {
                                   'verbose-name' : 'Src-Port' },
            'dst-tp-port-op'   : {
                                   'verbose-name' : 'Dst-Port-Op' },
            'dst-tp-port'      : {
                                   'verbose-name' : 'Dst-Port' },
            'icmp-type'        : {
                                 },
            'ether-type'       : {
                                 },
            'vns'              : {
                                   'verbose-name' : 'VNS ID',
                                 }
            }
        },
}


VNS_INTERFACE_ACCESS_LIST_FORMAT = {
    'vns-interface-access-list': {
        'field-orderings' : {
            'default'   : [ 'Idx', 'vnsname', 'vns-interface', 'name',  'in-out' ],
            'vns-config': [ 'Idx',        'vns-interface', 'name',  'in-out' ]
            },
        'fields' : {
            'vnsname'                : {
                                     'verbose-name' : 'VNS ID',
                                   },
            'vns-interface-name' : {
                                     'verbose-name' : 'VNS Interface ID',
                                   },
            'name'               : {
                                     'verbose-name' : 'VNS Acl name',
                                   },
            }
        },
}


VNS_INTERFACE_FORMAT = {
    'vns-interface' : {
        'field-orderings' : {
            'default'    : ['Idx', 'vns', 'address-space', 'interface', 'rule', 'last-seen', ],
            'vns-config' : ['Idx', 'address-space', 'interface', 'rule', 'last-seen', ]
        },
        'fields' : {
            'id'         : {
                             'verbose-name': 'ID',
                           },
            'vns'        : {
                            'verbose-name': 'VNS ID'
                           },
            'interface'  : {
                             'verbose-name': 'VNS Interface Name',
                            },
            'rule'        : {
                              'verbose-name': 'VNS Rule ID',
                            },
            'address-space'     : {
                                     'verbose-name' : 'Address Space',
                                  },
            'last-seen'   : {
                              'verbose-name': 'Last Seen',
                              'formatter' : fmtcnv.timestamp_to_local_timestr,
                            },
            }
        },
}
"""