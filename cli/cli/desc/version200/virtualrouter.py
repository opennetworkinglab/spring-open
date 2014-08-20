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
#

import command
import fmtcnv
import error

def virtualrouter_origin_external(data):
    """
    Return origin-name when the virtualrouter wasn't created by the cli,
    return None otherwise.
    """
    pk = command.mi.pk('virtualrouter')
    if not pk in data:
        return None;

    virtualrouter = command.sdnsh.get_table_from_store('virtualrouter',
                                             pk,
                                             data[pk])
    if len(virtualrouter) == 0:
        return None

    local = ['cli', 'rest']
    if 'origin' in virtualrouter[0] and not virtualrouter[0]['origin'] in local:
        return virtualrouter[0]['origin']
    return None

def virtualrouter_warn_external_config(data):
    """
    From the named virtualrouter, look up the entry, if it exists in the
    database, validate the 'origin' is either null, or 'cli',
    otherwise provide a warning about this particular virtualrouter
    (along with the originator name)
    """
    external_origin = virtualrouter_origin_external(data)
    if external_origin:
        command.sdnsh.warning('router %s may not be intended for cli update, '
                              'origin/creator "%s" ' % (data['id'], external_origin))
def verify_router_intf_ip(data):
    if data['ip-address']=='0.0.0.0':
        raise error.ArgumentValidationError("0.0.0.0 is not a valid router interface ip address")
    if data['subnet-mask']=='255.255.255.255':
        raise error.ArgumentValidationError("0.0.0.0 is not a valid router interface ip subnet mask")

def verify_router_gw_ip(data):
    if data['ip-address']=='0.0.0.0':
        raise error.ArgumentValidationError("0.0.0.0 is not a valid router interface ip address")

def virtualrouter_preprocess(data):
    current_mode=command.sdnsh.current_mode()
    if current_mode.startswith('config-tenant'):
        for x in command.sdnsh.mode_stack:
            if x['mode_name'] == 'config-tenant':
                tenant = x['obj']
    if current_mode.startswith('config-tenant-router'):
        for x in command.sdnsh.mode_stack:
            if x['mode_name'] == 'config-tenant-router':
                data['virtual-router'] = x['obj']
    if 'outgoing-intf' in data:
        current_obj=command.sdnsh.get_current_mode_obj()
        if current_mode.startswith('config-tenant-router-intf') or current_mode.startswith('config-tenant-router-gw'):
            for x in command.sdnsh.mode_stack:
                if x['mode_name'] == 'config-tenant-router':
                    current_obj = x['obj']
        data['outgoing-intf']=current_obj+'|'+data['outgoing-intf']
    if 'gateway-pool' in data:
        current_obj=command.sdnsh.get_current_mode_obj()
        if current_mode.startswith('config-tenant-router-intf') or current_mode.startswith('config-tenant-router-gw'):
            for x in command.sdnsh.mode_stack:
                if x['mode_name'] == 'config-tenant-router':
                    current_obj = x['obj']
        data['gateway-pool']=current_obj+'|'+data['gateway-pool']
    if 'src-vns' in data:
        if not'src-tenant' in data:
            data['src-tenant']=tenant
        data['src-vns']= data['src-tenant'] + '|' + data ['src-vns']
    if 'dst-vns' in data:
        if not 'dst-tenant' in data:
            data['dst-tenant']=tenant
        data['dst-vns']= data['dst-tenant'] + '|' + data ['dst-vns']
    if 'vns-connected' in data:
        data['vns-connected']=tenant+'|'+data['vns-connected']
    if 'router-connected-tenant' in data:
        if tenant !='system' and data['router-connected-tenant']!='system':
            command.sdnsh.warning('Tenant router interface can only connected to system tenant router\n')
        data['router-connected']= data['router-connected-tenant'] +'|'+ data['router-connected']
        del data['router-connected-tenant']

command.add_action('virtualrouter-warn-external-config', virtualrouter_warn_external_config,
                    {'kwargs': {'data'      : '$data',}})
command.add_action('virtualrouter-preprocess', virtualrouter_preprocess,
                    {'kwargs': {'data'      : '$data',}})
command.add_action('verify-router-intf-ip', verify_router_intf_ip,
                    {'kwargs': {'data'      : '$data',}})
command.add_action('verify-router-gw-ip', verify_router_gw_ip,
                    {'kwargs': {'data'      : '$data',}})


def complete_virtualrouter_preprocess(data):
    obj_id = command.sdnsh.get_current_mode_obj()
    if '|' in obj_id:
        parts=obj_id.split('|')
        data['tenant']=parts[0]
        data['virtual-router']=parts[0]+'|' + parts[1]
    else:
        data['tenant']=obj_id
        if 'vrname' in data:
            data['virtual-router']=obj_id+'|'+data['vrname']
    if 'router-connected-tenant' in data:
        data['tenant'] = data['router-connected-tenant']
    if 'src-tenant' in data:
        data['tenant']=data['src-tenant']
    if 'dst-tenant' in data:
        data['tenant']=data['dst-tenant']

def complete_virtualrouter_postprocess(data,completions):
    obj_id = command.sdnsh.get_current_mode_obj()
    parts=obj_id.split('|')
    tenant=parts[0]
    if tenant !='system': #non-system virtual router can only connected to system virtual router
        completions.clear()
        completions['system ']='Tenant Selection'
    else: # system virtual router can't connect to itself
        if 'system ' in completions:
            del completions['system ']

command.add_completion('complete-virtualrouter-postprocess', complete_virtualrouter_postprocess,
                           {'kwargs': {'data': '$data',
                                       'completions'  : '$completions',}})
command.add_completion('complete-virtualrouter-preprocess', complete_virtualrouter_preprocess,
                           {'kwargs': {'data': '$data',
                                       }})

def virtualrouter_confirm_external_delete(data):
    """
    From the named virtualrouter, look up the entry, if it exists in the
    database, validate the 'origin' is either null, or 'cli',
    otherwise provide a warning about this particular virtualrouter
    (along with the originator name)
    """
    external_origin = virtualrouter_origin_external(data)
    if external_origin:
        confirm = command.action_registry['confirm'][0] # XXX accessor?
        confirm('virtual router %s may not be intended for cli delete, '
                'origin/creator "%s"\nEnter y or yes to continue delete: '
                % (data['id'], external_origin))

command.add_action('virtualrouter-confirm-external-delete', virtualrouter_confirm_external_delete,
                    {'kwargs': {'data'      : '$data',}})

"""
#
# ----------------------------------------------------------------------
# router submode commands
#
VROUTER_COMMAND_DESCRIPTION = {
    'name'         : 'router',
    'help'         : 'Enter virtual router definition submode',
    'mode'         : 'config-tenant*',
    'command-type' : 'config-submode',
    'obj-type'     : 'virtualrouter',
    'submode-name' : 'config-tenant-router',
    'feature'      : 'vns',
    'short-help'   : 'Enter virtual router definition submode',
    'doc'          : 'vns|tenant-vrouter',
    'doc-example'  : 'vns|tenant-vrouter-example',
    'args' : (
        {
            'field'        : 'vrname',
            'type'         : 'identifier',
            'completion'   : 'complete-object-field',
            'scoped'       :  True,
            'syntax-help'  : 'Enter a router name',
            'action'       : (
                {
                    'proc' : 'virtualrouter-warn-external-config',
                },
                {   'proc' : 'tenant-show-preprocess'
                 },
                {
                    'proc' : 'push-mode-stack',
                },
            ),
            'no-action'    : (
                {
                    'proc' : 'virtualrouter-confirm-external-delete',
                },
                {   'proc' : 'tenant-show-preprocess'
                },
                {
                    'proc' : 'delete-objects',
                },
            )
        }
    ),
}

virtualrouter_show_action = (
    {   'proc'     : 'tenant-show-preprocess'},
    {
        'proc'     : 'query-table',
    },
    {
        'proc'     : 'display',
        'format'   : 'virtualrouter',
    },
)

VIRTUALROUTER_SHOW_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'obj-type'     : 'virtualrouter',
    'mode'         : 'config-tenant*',
    'command-type' : 'display-table',
    'action'       :  virtualrouter_show_action,
    'short-help'   : 'Show specific virtual router, identified by name',
    'doc'          : 'vns|show-tenant-id-router',
    'doc-example'  : 'vns|show-tenant-id-router-example',
    'args'         : (
        'router',
                    )
}

VIRTUALROUTER_SHOW_ID_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'obj-type'     : 'virtualrouter',
    'mode'         : 'config-tenant*',
    'command-type' : 'display-table',
    'action'       :  virtualrouter_show_action,
    'short-help'   : 'Show specific virtual router, identified by name',
    'doc'          : 'vns|show-tenant-vrouter-id',
    'doc-example'  : 'vns|show-tenant-vrouter-id-example',
    'args'         : (
        'router',
        {
            'field'      : 'vrname',
            'type'       : 'identifier',
            'completion' : 'complete-object-field',
            'help-name'  : "virtualrouter-id",
            'scoped'     : True,
        },
        {
            'optional'  : True,
            'choices'   : (
             {
               'token'     : 'ip-address-pool',
               'obj-type'   : 'interface-address-pool',
               'doc'        : 'vns|show-tenant-id-router-ippool',
               'action'     : (
                           {   'proc'     : 'tenant-show-preprocess'},
                           {   'proc'     : 'query-table'},
                           {   'proc'     : 'display',
                               'format'   : 'interface-address-pool'},
                           ),
              },
              {
               'field'  : 'route',
               'type'   : 'enum',
               'values' : 'route',
               'obj-type'   : 'virtualrouter-routingrule',
               'doc'        : 'vns|show-tenant-id-router-route',
               'action' : (
                           {   'proc'     : 'tenant-show-preprocess'},
                           {   'proc'     : 'query-table',},
                           {
                               'proc'     : 'display',
                               'format'   : 'virtualrouter-routingrule'},
                           ),
               },
              (
                   {
                   'field'      : 'interfaces',
                   'type'       : 'enum',
                   'values'     : 'interfaces',
                   'obj-type'   : 'virtualrouter-interface',
                   'doc'        : 'vns|show-tenant-id-router-interfaces',
                   'action' : (
                               {   'proc'     : 'tenant-show-preprocess'},
                               {   'proc'     : 'query-table'},
                               {   'proc'     : 'display',
                                   'format'   : 'virtualrouter-interface'},
                               ),
                   },
                  {
                    'optional' : True,
                    'args' : (
                             {
                              'field'      : 'vriname',
                              'obj-type'   : 'virtualrouter-interface',
                              'completion'   : ['complete-virtualrouter-preprocess',
                                                'complete-from-another'],
                              'other'        : 'virtualrouter-interface|vriname',
                              'scoped'     : 'virtual-router',
                              'explicit'   : True, 
                              'action'     : (
                                           {   'proc'     : 'tenant-show-preprocess'},
                                           {   'proc'     : 'query-table'},
                                           {   'proc'     : 'display',
                                               'format'   : 'virtualrouter-interface'},
                                           ),
                              },
                              {'optional'   : True,
                               'token'     : 'ip-address-pool',
                               'obj-type'   : 'interface-address-pool',
                               'action'     : (
                                           {   'proc'     : 'tenant-show-preprocess'},
                                           {   'proc'     : 'query-table'},
                                           {   'proc'     : 'display',
                                               'format'   : 'interface-address-pool'},
                                           ),
                               },  
                        ),
                  },
               ),       
              (
                   {
                   'field'      : 'gateway-pools',
                   'type'       : 'enum',
                   'values'     : 'gateway-pools',
                   'obj-type'   : 'virtualrouter-gwpool',
                   'doc'        : 'vns|show-tenant-id-router-gwpools',
                   'action' : (
                               {   'proc'     : 'tenant-show-preprocess'},
                               {   'proc'     : 'query-table'},
                               {   'proc'     : 'display',
                                   'format'   : 'virtualrouter-gwpool'},
                               ),
                   },
                  {
                    'optional' : True,
                    'args' : (
                             {
                              'field'      : 'vrgwname',
                              'obj-type'   : 'virtualrouter-gwpool',
                              'completion'   : ['complete-virtualrouter-preprocess',
                                                'complete-from-another'],
                              'other'        : 'virtualrouter-gwpool|vrgwname',
                              'scoped'     : 'virtual-router',
                              'explicit'   : True,
                              'action'     : (
                                           {   'proc'     : 'tenant-show-preprocess'},
                                           {   'proc'     : 'query-table'},
                                           {   'proc'     : 'display',
                                               'format'   : 'virtualrouter-gwpool'},
                                           ),
                              },
                              {'optional'   : True,
                               'token'     : 'gw-address-pool',
                               'obj-type'   : 'gateway-address-pool',
                               'action'     : (
                                           {   'proc'     : 'tenant-show-preprocess'},
                                           {   'proc'     : 'query-table'},
                                           {   'proc'     : 'display',
                                               'format'   : 'gateway-address-pool'},
                                           ),
                               },
                        ),
                  },
               ),
            ),
         },
   )
}

VIRTUALROUTER_DEF_DESCRIPTION_COMMAND_DESCRIPTION = {
    'name'         : 'description',
    'mode'         : 'config-tenant-router',
    'command-type' : 'config',
    'short-help'   : 'Provide description for a virtual router instance',
    'doc'          : 'vns|tenant-router-description',
    'doc-example'  : 'vns|tenant-router-description-example',
    'args'         : (
        {
            'field' : 'description',
            'type'  : 'string',
        }
    ),
}


VIRTUALROUTER_DEF_ORIGIN_COMMAND_DESCRIPTION = {
    'name'         : 'origin',
    'mode'         : 'config-tenant-router',
    'command-type' : 'config',
    'short-help'   : 'Describe virtual router origin',
    'doc'          : 'virtualrouter|origin',
    'doc-example'  : 'virtualrouter|origin-example',
    'args' : (
        {
            'field'  : 'origin',
            'type'   : 'string',
            'action' : (
                {
                    'proc' : 'virtualrouter-warn-external-config',
                },
                {
                    'proc' : 'write-fields',
                },
            ),
        },
    ),
}

VIRTUALROUTER_INTF_DEF_ORIGIN_COMMAND_DESCRIPTION = {
    'name'         : 'origin',
    'mode'         : 'config-tenant-router-intf',
    'command-type' : 'config',
    'short-help'   : 'Describe virtual router interface origin',
    'doc'          : 'virtualrouter|origin',
    'doc-example'  : 'virtualrouter|origin-example',
    'args' : (
        {
            'field'  : 'origin',
            'type'   : 'string',
            'action' : (
                {
                    'proc' : 'virtualrouter-warn-external-config',
                },
                {
                    'proc' : 'write-fields',
                },
            ),
        },
    ),
}

#
# ----------------------------------------------------------------------
# router interface submode commands
#
VROUTER_INTERFACE_COMMAND_DESCRIPTION = {
    'name'         : 'interface',
    'help'         : 'Enter virtual router interface definition submode',
    'mode'         : 'config-tenant-router*',
    'command-type' : 'config-submode',
    'obj-type'     : 'virtualrouter-interface',
    'submode-name' : 'config-tenant-router-intf',
    'short-help'   : 'Enter virtual router interface definition submode',
    'doc'          : 'vns|tenant-router-interface',
    'doc-example'  : 'vns|tenant-router-interface-example',
    'args' : (
        {
            'field'        : 'vriname',
            'type'         : 'identifier',
            'scoped'       : 'virtual-router',
            'explicit'     : True,
            'completion'   : [ 'complete-virtualrouter-preprocess',
                               'complete-object-field',
                             ],
            'other'        : 'virtualrouter-interface|vriname',
            'syntax-help'  : 'Enter a router interface name',
            'action'       : (
                {
                    'proc' : 'virtualrouter-preprocess',
                },
                {
                    'proc' : 'push-mode-stack',
                },
            ),
            'no-action'    : (
                {
                    'proc' : 'virtualrouter-preprocess',
                },
                {
                    'proc' : 'delete-objects',
                },
            )
        },
        {
         'optional' : True,
         'optional-for-no' : True,
         'choices' : (
            (
                 {
                    'token'        : 'vns',
                    'doc'          : 'vns|interface-vns-connected', 
                 },
                 {
                    'field'        :'vns-connected',
                    'type'         : 'identifier',
                    'completion'   : ['complete-virtualrouter-preprocess',
                                      'complete-from-another'],
                    'other'        : 'vns-definition|vnsname',
                    'syntax-help'  : 'Enter a VNS to connect',
                    'scoped'       : 'tenant',  
                    
                    'explicit'     : True,                
                  }
             ),
            (
                {
                    'token'        : 'tenant',
                    'doc'          : 'vns|interface-router-connected', 
                },
                {   'field'        :'router-connected-tenant',
                    'type'         : 'identifier',
                    'completion'   : ['complete-from-another',
                                      'complete-virtualrouter-postprocess'],
                    'other'        : 'tenant|name',
                    'syntax-help'  : 'Enter a Tenant Router to connect',
                },
                {
                    'field'        :'router-connected',
                    'type'         : 'identifier',
                    'scoped'       : 'tenant',
                    'explicit'     : True,
                    'completion'   : ['complete-virtualrouter-preprocess',
                                      'complete-from-another'],
                    'other'        : 'virtualrouter|vrname',
                    'syntax-help'  : 'Enter a Tenant Router to connect',
                 },
             )
        ),
         },
        
    ),
}

VRI_DEF_ACTIVE_COMMAND_DESCRIPTION = {
    'name'        : 'active',
    'mode'        : 'config-tenant-router-intf',
    'short-help'  : 'Set Virtual Router Interface active',
    'doc'          : 'vns|tenant-router-active',
    'doc-example'  : 'vns|tenant-router-active-example',
    'doc-include'  : [ 'default' ],
    'obj-type'     : 'virtualrouter-interface',
    'args' : (),
    'action' : (
        {
            'proc' : 'write-fields',
            'data' : { 'active' : True },
            'syntax-help' : 'mark the interface as active',
        }
    ),
    'no-action' : (
        {
            'proc' : 'write-fields',
            'data' : { 'active' : False },
            'syntax-help' : 'mark the interface as inactive',
        }
    )
}

VRI_DEF_IP_COMMAND_DESCRIPTION = {
    'name'        : 'ip',
    'mode'        : 'config-tenant-router-intf',
    'short-help'  : 'Set Virtual Router Interface IP address',
    'doc'          : 'vns|tenant-router-interfaceip',
    'doc-example'  : 'vns|tenant-router-interfaceip-example',
    'doc-include'  : [ 'default' ],
    'parent-field' : 'virtual-router-interface',
    'command-type' : 'config-object',
    'obj-type'     : 'interface-address-pool',
    'data'         : {
                        'ip-address'       : None,
                        'subnet-mask'      : None,
                     },
    'args' : (
        { 'choices': (
        (
            {
                'field' : 'ip-address',
                'type'  : 'ip-address-not-mask',
                'doc'   : 'vns|vns-access-list-ip-and-mask-ip',
            },
            {
                'field'        : 'subnet-mask',
                'type'         : 'netmask',
                'data-handler' : 'convert-inverse-netmask',
                'doc'          : 'vns|vns-access-list-ip-and-mask-mask',
            },
        ),
        (
            {
                'field'        : 'ip-address',
                'type'         : 'cidr-range',
                'help-name'    : 'src-cidr',
                'data-handler' : 'split-cidr-data-inverse',
                'dest-ip'      : 'ip-address',
                'dest-netmask' : 'subnet-mask',
                'doc'          : 'vns|vns-access-list-cidr-range',
            }
        ),
              )
         }
        ),
    'action' : (    {
                        'proc'    : 'verify-router-intf-ip'
                     },
                    {
                        'proc' : 'write-object',
                    },
        ),
    'no-action' : (
                    {
                        'proc' : 'delete-objects',
                    },
        ),
}

#
# ----------------------------------------------------------------------
# gateway pool submode commands
#
VROUTER_GWPOOL_COMMAND_DESCRIPTION = {
    'name'         : 'gateway-pool',
    'help'         : 'Enter virtual router gateway pool submode',
    'mode'         : 'config-tenant-router*',
    'command-type' : 'config-submode',
    'obj-type'     : 'virtualrouter-gwpool',
    'submode-name' : 'config-tenant-router-gw',
    'short-help'   : 'Enter virtual router gateway pool definition submode',
    'doc'          : 'vns|tenant-router-gwpool',
    'doc-example'  : 'vns|tenant-router-gwpool-example',
    'args' : (
        {
            'field'        : 'vrgwname',
            'type'         : 'identifier',
            'scoped'       : 'virtual-router',
            'explicit'     : True,
            'completion'   : [ 'complete-virtualrouter-preprocess',
                               'complete-object-field',
                             ],
            'other'        : 'virtualrouter-gwpool|vrgwname',
            'syntax-help'  : 'Enter a router gateway pool name',
            'action'       : (
                {
                    'proc' : 'virtualrouter-preprocess',
                },
                {
                    'proc' : 'push-mode-stack',
                },
            ),
            'no-action'    : (
                {
                    'proc' : 'virtualrouter-preprocess',
                },
                {
                    'proc' : 'delete-objects',
                },
            )
        },
    ),
}

VRGW_DEF_IP_COMMAND_DESCRIPTION = {
    'name'        : 'ip',
    'mode'        : 'config-tenant-router-gw',
    'short-help'  : 'Add IP address to the gateway pool',
    'doc'          : 'vns|tenant-router-gwip',
    'doc-example'  : 'vns|tenant-router-gwip-example',
    'doc-include'  : [ 'default' ],
    'parent-field' : 'virtual-router-gwpool',
    'command-type' : 'config-object',
    'obj-type'     : 'gateway-address-pool',
    'data'         : {
                        'ip-address'       : None,
                     },
    'args' : (
        {
            'field' : 'ip-address',
            'type'  : 'ip-address-not-mask',
            'doc'   : 'vns|vns-access-list-ip-and-mask-ip',
            'action' : (
                {
                    'proc'    : 'verify-router-gw-ip'
                },
                {
                    'proc' : 'write-object',
                },
            ),
            'no-action' : (
                {
                    'proc' : 'delete-objects',
                },
            ),
         },
    ),
}


STATIC_ARP_COMMAND_DESCRIPTION = {
    'name'        : 'arp',
    'mode'        : 'config*',
    'short-help'  : 'Set Static ARP',
    'doc'          : 'static-arp',
    'doc-example'  : 'static-arp-example',
    'doc-include'  : [ 'default' ],
    'command-type' : 'config',
    'obj-type'     : 'static-arp',
    'data'         : {
                        'ip'       : None,
                        'mac'      : None,
                     },
    'args' : (
        
            {
                'field' : 'ip',
                'type'  : 'ip-address-not-mask',
            },
            {
                'field'        : 'mac',
                'type'         : 'mac-address',
            },

        ),
    'action' : (
                    {
                        'proc' : 'write-object',
                    },
        ),
    'no-action' : (
                    {
                        'proc' : 'delete-objects',
                    },
        ),
}
tenant_show_action = (
    {
        'proc'  : 'query-table',
    },
    {
        'proc'   : 'display',
        'format' : 'staticarp',
    },
)

STATIC_ARP_SHOW_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'all-help'     : 'Show static ARP details',
    'short-help'   : 'Show all configured static ARPs',
    'command-type' : 'display-table',
    'obj-type'     : 'static-arp',
    'doc'          : 'show-arp',
    'doc-example'  : 'show-arp-example',
    'action'       :  tenant_show_action,
    'args'         : (
        'arp',
    )
}
#
# FORMATS
ARP_FORMAT = {
    'staticarp' : {
        'field-orderings' : {
            'default'     : [ 'Idx', 'ip', 'mac'],
            },
        'fields' : {
           'ip'              : { 
                               },
           'mac'          : {
                               },                    
           }
        },
}
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
                'type'         : 'netmask',
                'doc'          : 'vns|vns-access-list-ip-and-mask-mask',
                'data-handler' : 'convert-inverse-netmask',
            },
        ),
        (
            {
                'field'    : 'src-ip',
                'type'     : 'ip-address-not-mask',
                'data'     : {
                               'src-ip-mask' : '0.0.0.0',
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
                'doc'          : 'vns|vns-access-list-cidr-range',
            }
        ),
       (
            {
                'token'  : 'any',
                'data'   : {
                              'src-ip'      : '0.0.0.0',
                              'src-ip-mask' : '255.255.255.255',
                           },
                'doc'    : 'vns|vns-access-list-ip-any',
            }
        ),
    )
}

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
                'type'  : 'netmask',
                'doc'   : 'vns|vns-access-list-ip-and-mask-mask',
                'data-handler' : 'convert-inverse-netmask',
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

ROUTING_RULE_COMMAND_DESCRIPTION = {
    'name'         : 'route',
    'mode'         : 'config-tenant-router*',
    'short-help'   : 'Set Routing Rule',
    'doc'          : 'vns|tenant-router-route',
    'doc-example'  : 'vns|tenant-router-route-example',
    'doc-include'  : [ 'default' ],
    'command-type' : 'config-object',
    'obj-type'     : 'virtualrouter-routingrule',
    'args' : (
            {'token'     : 'from'
            },
            {
             'choices': (
                ({'token'        : 'tenant'},
                 {'field'        : 'src-tenant',
                  'type'         : 'identifier',
                  'completion'   : 'complete-from-another',
                  'other'        : 'tenant|name',
                  'help-name'    : 'source tenant'
                  },
                 {
                  'optional'     : True,
                  'optional-for-no' : True,
                  'args'         : (
                     {'token'        : 'vns',
                     },
                     {'field'        : 'src-vns',
                      'type'         : 'identifier',
                      'completion'   : ['complete-virtualrouter-preprocess',
                                        'complete-from-another'],
                      'other'        : 'vns-definition|vnsname',
                      'help-name'    : 'source VNS',
                      },
                   ),
                },
                ),
                ({'token'        : 'vns',
                 },
                 {'field'        : 'src-vns',
                  'type'         : 'identifier',
                  'completion'   : 'complete-from-another',
                  'other'        : 'vns-definition|vnsname',
                  'help-name'    : 'source VNS',
                  },
                ),
                (  SRC_IP_MATCH,
                )
                )
            },                 
            {'token'        : 'to',
            },
            {
             'choices': (
                ({'token'        : 'tenant'},
                 {'field'        : 'dst-tenant',
                  'type'         : 'identifier',
                  'completion'   : 'complete-from-another',
                  'other'        : 'tenant|name',
                  'help-name'    : 'destination tenant'
                  },
                 {
                  'optional'     : True,
                  'optional-for-no' : True,
                  'args'         : (
                     {'token'        : 'vns',
                     },
                     {'field'        : 'dst-vns',
                      'type'         : 'identifier',
                      'completion'   : ['complete-virtualrouter-preprocess',
                                        'complete-from-another'],
                      'other'        : 'vns-definition|vnsname',
                      'help-name'    : 'destination VNS',
                      },
                   ),
                },
                 ),
                ({'token'        : 'vns',
                 },
                 {'field'        : 'dst-vns',
                  'type'         : 'identifier',
                  'completion'   : 'complete-from-another',
                  'other'        : 'vns-definition|vnsname',
                  'help-name'    : 'destination VNS',
                  },
                ),
                (  DST_IP_MATCH,
                )
                )
            },
            {
             'optional' : True,
             'optional-for-no' : True,
             'choices' : (
                (
                 {
                  'field'     : 'nh-ip',
                  'type'      : 'ip-address-not-mask',
                  'help-name': 'next hop ip address',
                  'doc'      : 'vns|vns-access-list-ip-only',
                  },
                 ),
                (
                 {'token'        : 'gw-pool',
                  },
                 {
                  'field'        : 'gateway-pool',
                  'type'         : 'identifier',
                  'scoped'       : 'virtual-router',
                  'explicit'     : True,
                  'completion'   : ['complete-virtualrouter-preprocess',
                                    'complete-from-another'],
                  'other'        : 'virtualrouter-gwpool|vrgwname',
                  'help-name'    : 'gateway pool name',
                  'doc'          : 'vns|gateway-pool',
                  },
                 ),
                ),
             },
             {
              'field'        : 'outgoing-intf',
              'type'         : 'identifier',
              'scoped'       : 'virtual-router',
              'explicit'     : True,
              'completion'   : ['complete-virtualrouter-preprocess',
                                'complete-from-another'],
              'other'        : 'virtualrouter-interface|vriname',
              'optional'     : True,
              'optional-for-no' : True,
              'help-name'    : 'outgoing interface',
              'doc'          : 'vns|outgoing-interface',
              },
            {'field'         : 'action',
             'type'          : 'enum',
             'values'        : ('deny', 'permit'),
             }         
            ), 
    'action' : (
                    {
                        'proc' : 'virtualrouter-preprocess'
                     },
                    {
                        'proc' : 'write-object',
                    },
        ),
    'no-action' : (
                    {
                        'proc' : 'virtualrouter-preprocess'
                     },
                    {
                        'proc' : 'delete-objects',
                    },
        ),
}

# FORMATS
VIRTUALROUTER_FORMAT = {
    'virtualrouter' : {
        'field-orderings' : {
            'default'     : [ 'Idx', 'tenant', 'vrname', 'description'],
            'details'     : [ 'Idx', 'tenant', 'vrname', 'description'],
            'brief'       : [ 'Idx', 'tenant', 'vrname'],
            },
        'fields' : {
           'tenant'              : { 'verbose-name' : 'Tenant ID',
                               },
           'vrname'          : {
                               },
           'description'     : {
                               },          
           }
        },
}
VIRTUALROUTER_INTERFACE_FORMAT = {
    'virtualrouter-interface' : {
        'field-orderings' : {
            'default'     : [ 'Idx', 'virtual-router', 'vriname', 'active', 'vns-connected','router-connected'],
            'details'     : [ 'Idx', 'virtual-router', 'vriname', 'active', 'vns-connected','router-connected'],
            'brief'       : [ 'Idx', 'virtual-router', 'vriname', 'active'],
            },
        'fields' : {
           'virtual-router'     : {},
           'vriname'            : {},
           'active'             : {},   
            'vns-connected'     : {},
            'router-connected'  : {},       
           }
        },
}
VIRTUALROUTER_GWPOOL_FORMAT = {
    'virtualrouter-gwpool' : {
        'field-orderings' : {
            'default'     : [ 'Idx', 'virtual-router', 'vrgwname'],
            'details'     : [ 'Idx', 'virtual-router', 'vrgwname'],
            'brief'       : [ 'Idx', 'virtual-router', 'vrgwname'],
            },
        'fields' : {
           'virtual-router'     : {},
           'vrgwname'           : {},
           }
        },
}
GATEWAY_ADDRESS_POOL_FORMAT = {
    'gateway-address-pool' : {
        'field-orderings' : {
            'default'     : [ 'Idx', 'virtual-router-gwpool', 'ip-address'],
            },
        'fields' : {
           'virtual-router-gwpool'        : {'verbose-name' : 'Virtual Router Gateway Pool',},
           'ip-address'                   : {'verbose-name' : 'IP Address',},
           }
        },
}
VIRTUALROUTER_ROUTINGRULE_FORMAT = {
    'virtualrouter-routingrule' : {
        'field-orderings' : {
            'default'     : [ 'Idx', 'virtual-router','action','src-tenant','src-vns','src-ip','src-ip-mask','dst-tenant','dst-vns','dst-ip','dst-ip-mask','nh-ip','gateway-pool','outgoing-intf'],
            },
        'fields' : {
           'virtual-router'       : {},
           'src-tenant'           : {},
           'src-vns'              : {},   
           'src-ip'               : {},
           'src-ip-mask'          : {'formatter'    : fmtcnv.convert_inverse_netmask_handler,},
           'dst-tenant'           : {},  
           'dst-vns'              : {},   
           'dst-ip'               : {},
           'dst-ip-mask'          : {'formatter'    : fmtcnv.convert_inverse_netmask_handler,},
           'nh-ip'                : {},  
           'gateway-pool'         : {},
           'outgoing-intf'        : {},  
           'action'               : {},     
           }
        },
}
INTERFACE_ADDRESS_POOL_FORMAT = {
    'interface-address-pool' : {
        'field-orderings' : {
            'default'     : [ 'Idx', 'virtual-router-interface', 'ip-address', 'subnet-mask'],
            },
        'fields' : {
           'virtual-router-interface'     : {'verbose-name' : 'Virtual Router Interface',},
           'ip-address'                   : {'verbose-name' : 'IP Address',},
           'subnet-mask'                  : {'formatter'    : fmtcnv.convert_inverse_netmask_handler,},
           }
        },
}
"""