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
def tenant_origin_external(data):
    #Return origin-name when the tenant wasn't created by the cli,
    #return None otherwise.
    pk = command.mi.pk('tenant')
    if not pk in data:
        return None;

    tenant = command.sdnsh.get_table_from_store('tenant',
                                             pk,
                                             data[pk])
    if len(tenant) == 0:
        return None

    local = ['cli', 'rest']
    if 'origin' in tenant[0] and not tenant[0]['origin'] in local:
        return tenant[0]['origin']
    return None

def tenant_warn_external_config(data):
    """
    From the named tenant, look up the entry, if it exists in the
    database, validate the 'origin' is either null, or 'cli',
    otherwise provide a warning about this particular tenant
    (along with the originator name)
    """
    external_origin = tenant_origin_external(data)
    if external_origin:
        command.sdnsh.warning('tenant %s may not be intended for cli update, '
                              'origin/creator "%s" ' % (data['name'], external_origin))

def tenant_show_preprocess(obj_type, data,scoped=None):
    data['tenant']='default'
    settingscope=True
    if 'name' in data:
        data['tenant']= data['name']
        del data['name']
    else:
        current_mode=command.sdnsh.current_mode()
        if not current_mode.startswith('config-tenant'):
            data['tenant']='default'
        if current_mode.startswith('config-tenant'):
            settingscope=False
            for x in command.sdnsh.mode_stack:
                if x['mode_name'] == 'config-tenant':
                    data['tenant'] = x['obj']
    if obj_type=='vns-definition' and data['tenant']=='default' and scoped=='True' and settingscope:
        data['scoped']=False
    if 'vnsname' in data and data['vnsname']!='all':
        if (obj_type!='vns-definition'):
            data['vns']=data['tenant'] +'|'+data['vnsname']
    if 'vnsname' in data and data['vnsname']=='all':
        data['vns']='all'
        del data['vnsname']
    if 'vns' in data and data['vns']=='all':
        if scoped=='True':
            data['scoped']=False
        del data['tenant']
    if 'vrname' in data:
        if (obj_type!='virtualrouter'):
            data['virtual-router']=data['tenant'] +'|'+data['vrname']
    if 'vriname' in data:
        if (obj_type!='virtualrouter-interface'):
            data['virtual-router-interface']=data['tenant'] +'|'+data['vrname'] +'|' + data['vriname']
    if 'vrgwname' in data:
        if (obj_type!='virtualrouter-gwpool'):
            data['virtual-router-gwpool']=data['tenant'] +'|'+data['vrname'] +'|' + data['vrgwname']

"""
command.add_action('tenant-show-preprocess', tenant_show_preprocess,
                    {'kwargs': {'obj_type': '$obj-type',
                                'data'    : '$data',
                                'scoped'  : '$scoped',}})
command.add_action('tenant-warn-external-config', tenant_warn_external_config,
                    {'kwargs': {'data'      : '$data',}})

def complete_tenant_preprocess(data):
    if 'name' in data:
        data['tenant'] =data['name']

command.add_completion('complete-tenant-preprocess', complete_tenant_preprocess,
                           {'kwargs': {'data': '$data',
                                       }})
#
# ----------------------------------------------------------------------
# tenant submode commands
#
TENANT_COMMAND_DESCRIPTION = {
    'name'         : 'tenant',
    'help'         : 'Enter tenant definition submode',
    'mode'         : 'config*',
    'command-type' : 'config-submode',
    'obj-type'     : 'tenant',
    'parent-field' : None,
    'current-mode-obj-id' : None,
    'submode-name' : 'config-tenant',
    'feature'      : 'vns',
    'short-help'   : 'Enter tenant definition submode',
    'doc'          : 'vns|tenant',
    'doc-example'  : 'vns|tenant-example',
    'args' : (
        {
            'field'        : 'name',
            'type'         : 'identifier',
            'completion'   : 'complete-object-field',
            'syntax-help'  : 'Enter a tenant name',
            'doc'          : 'tenant|tenant-id',
            'doc-example'  : 'tenant|tenant-id-example',
            'doc-include'  : [ 'type-doc' ],
            'action'       : (
                {
                    'proc' : 'tenant-warn-external-config',
                },
                {
                    'proc' : 'push-mode-stack',
                },
            ),
            'no-action'    : (
                {
                    'proc' : 'tenant-warn-external-config',
                },
                {
                   'proc' : 'delete-objects',
                },
            )
        }
    ),
}


tenant_show_action = (
    {
        'proc'  : 'query-table',
    },
    {
        'proc'       : 'join-table',
        'obj-type'   : 'virtualrouter',
        'key'        : 'tenant',
        'join-field' : 'name',
        'add-field'  : 'virtualrouter|vrname',
    },
    {
        'proc'   : 'display',
        'format' : 'tenant',
    },
)

TENANT_SHOW_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'all-help'     : 'Show tenant details',
    'short-help'   : 'Show defined tenants',
    'command-type' : 'display-table',
    'obj-type'     : 'tenant',
    'action'       : tenant_show_action,
    'args'         : (
        'tenant',
    )
}

TENANT_SHOW_ID_INFO_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'obj-type'     : 'tenant',
    'mode'         : 'login',
    'command-type' : 'display-table',
    'action'       : tenant_show_action,
    'short-help'   : 'Show specific tenant, identified by name',
    'doc'          : 'vns|show-tenant',
    'doc-example'  : 'vns|show-tenant-example',
    'args'         : (
        'tenant',
        {
            'choices' : (
                {
                    'field'      : 'name',
                    'type'       : 'identifier',
                    'completion' : 'complete-object-field',
                    'help-name'  : "tenant-id",
                },
                {
                    'field'     : 'name',
                    'type'      : 'enum',
                    'values'    : 'all',
                }
            ),
        },
    )
}

TENANT_SHOW_ID_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'obj-type'     : 'tenant',
    'mode'         : 'login',
    'command-type' : 'display-table',
    'action'       : tenant_show_action,
    'short-help'   : 'Show specific tenant, identified by name',
    'doc'          : 'vns|show-tenant',
    'args'         : (
        'tenant',
        {
            'field'      : 'name',
            'type'       : 'identifier',
            'completion' : 'complete-object-field',
            'help-name'  : "tenant-id",
        },
        {
         'choices'     : (
        (
            {
                    'field'      : 'vns-field',
                    'type'       : 'enum',
                    'values'     : 'vns',
                    'action'     : ({   'proc'     : 'tenant-show-preprocess'},
                                    {   'proc'     : 'display-table'},
                                    ),
                    'obj-type'   : 'vns-definition',
                    'short-help' : 'Show VNS belonged to this tenant',
                    'doc'        : 'vns|show-tenant-id-vns',
            }, 
            {       'optional' : True,
                    'args'     : (
                         {
                              'field'      : 'vnsname',
                              'type'       : 'identifier',
                              'scoped'     : 'tenant',
                              'explicit'   : True,
                              'completion' : ['complete-tenant-preprocess',
                                              'complete-from-another'],
                              'other'      : 'vns-definition|vnsname',
                              'help-name'  : "vns-id",
                         },
                        {   'optional'     : True, 
                            'choices' : (
                                         {
                                          'field'      : 'vns-interface',
                                          'type'       : 'enum',
                                          'values'     : 'interfaces',
                                          'obj-type'   : 'vns-interface',
                                          'action'     : (
                                                          {'proc'   : 'tenant-show-preprocess'},
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
                                        'action'     : (
                                                         {
                                                          'proc' : 'tenant-show-preprocess',
                                                          },
                                                        'display-table',
                                                        ),
                                        'obj-type'   : 'vns-interface-rule',
                                        'short-help' : 'Show VNS configured interfaces-rules',
                                        'doc'        : 'vns|show-id-interface-rules',
                                        },
                                        {
                                        'field'      : 'access-lists',
                                        'type'       : 'enum',
                                        'values'     : 'access-lists',
                                        'action'     : (
                                                         {
                                                          'proc' : 'tenant-show-preprocess',
                                                          },
                                                        'display-table',
                                                        ),
                                        'obj-type'   : 'vns-access-list',
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
                        )
                    }
        ),  
        (
        {
         'field'        : 'router',
         'type'         : 'enum',
         'values'       : 'router',
         'doc'          : 'vns|show-tenant-id-router',
         }, 
        {   'optional'   :  True,
            'args'       : (
                    {
                     'field'      : 'vrname',
                     'type'       : 'identifier',
                     'scoped'     : 'tenant',
                     'explicit'   : True,
                     'completion' : ['complete-tenant-preprocess',
                                     'complete-from-another'],     
                     'other'      : 'virtualrouter|vrname',
                     'help-name'  : "virtualrouter-id",
                    },
                    {'optional'  : True,
                     'choices'   : (
                      {
                       'token'      : 'ip-address-pool',
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
                      {
                       'field'     : 'interfaces',
                       'type'      : 'enum',
                       'values'    : 'interfaces',
                       'obj-type'  : 'virtualrouter-interface',
                       'doc'       : 'vns|show-tenant-id-router-interfaces',
                       'action'    : (
                                   {   'proc'     : 'tenant-show-preprocess'},
                                   {   'proc'     : 'query-table'},
                                   {   'proc'     : 'display',
                                       'format'   : 'virtualrouter-interface'},
                                   ),
                       },
                      {
                       'field'     : 'gateway-pools',
                       'type'      : 'enum',
                       'values'    : 'gateway-pools',
                       'obj-type'  : 'virtualrouter-gwpool',
                       'doc'       : 'vns|show-tenant-id-router-gwpools',
                       'action'    : (
                                   {   'proc'     : 'tenant-show-preprocess'},
                                   {   'proc'     : 'query-table'},
                                   {   'proc'     : 'display',
                                       'format'   : 'virtualrouter-gwpool'},
                                   ),
                       },
                      {
                       'token'      : 'gw-address-pool',
                       'obj-type'   : 'gateway-address-pool',
                       'doc'        : 'vns|show-tenant-id-router-gwippool',
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
         },     
         ),
        (
          { 'field'     :'running-config',
            'type'      : 'enum',
            'values'    : 'running-config',
            'action'     : ({'proc'   : 'tenant-show-preprocess'},
                             'legacy-cli'),
         }
         ),
        (
         {
            'field'    : 'detail',
            'type'     : 'enum',
            'values'   : ('details', 'brief'),
            'doc'      : 'format|+',
        }
         )
                          )
        }
    )
}

TENANT_DEF_DESCRIPTION_COMMAND_DESCRIPTION = {
    'name'         : 'description',
    'mode'         : 'config-tenant',
    'command-type' : 'config',
    'short-help'   : 'Provide description for a tenant instance',
    'doc'          : 'vns|tenant-description',
    'doc-example'  : 'vns|tenant-description-example',
    'args'         : (
        {
            'field' : 'description',
            'type'  : 'string',
        }
    ),
}


TENANT_DEF_ACTIVE_COMMAND_DESCRIPTION = {
    'name'        : 'active',
    'mode'        : 'config-tenant',
    'short-help'  : 'Set tenant active',
    'doc'          : 'vns|tenant-active',
    'doc-example'  : 'vns|tenant-active-example',
    'doc-include'  : [ 'default' ],
    'args' : (),
    'action' : (
        {
            'proc' : 'write-fields',
            'data' : { 'active' : True },
            'syntax-help' : 'mark the tenant as active',
        }
    ),
    'no-action' : (
        {
            'proc' : 'write-fields',
            'data' : { 'active' : False },
            'syntax-help' : 'mark the tenant as inactive',
        }
    )
}

TENANT_DEF_ORIGIN_COMMAND_DESCRIPTION = {
    'name'         : 'origin',
    'mode'         : 'config-tenant',
    'command-type' : 'config',
    'short-help'   : 'Describe tenant origin',
    'doc'          : 'tenant|origin',
    'doc-example'  : 'tenant|origin-example',
    'args' : (
        {
            'field'  : 'origin',
            'type'   : 'string',
            'action' : (
                {
                    'proc' : 'tenant-warn-external-config',
                },
                {
                    'proc' : 'write-fields',
                },
            ),
        },
    ),
}

#
# FORMATS
TENANT_FORMAT = {
    'tenant' : {
        'field-orderings' : {
            'default'     : [ 'Idx', 'name', 'active', 'description', 'virtualrouter'],
            'details'     : [ 'Idx', 'name', 'active', 'description', 'virtualrouter'],
            'brief'       : [ 'Idx', 'name', 'active'],
            },
        'fields' : {
           'name'              : { 'verbose-name' : 'Tenant ID',
                               },
           'active'          : {
                               },
           'description'     : {
                               },
           'virtualrouter'   : {'verbose-name' : 'Router ID',
                               }
                    
           }
        },
}
"""
