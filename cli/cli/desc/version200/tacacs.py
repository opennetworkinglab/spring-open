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
TACACS_SERVER_HOST_COMMAND_DESCRIPTION = {
    'name'         : 'tacacs',
    'mode'         : 'config*',
    'short-help'   : 'TACACS timeout, ip server address',
    'doc'          : 'tacacs|tacacs',
    'doc-example'  : 'tacacs|tacacs-example',
    'command-type' : 'config-object',
    'obj-type'     : 'tacacs-plus-host',
    'parent-field' : None,
    'args'         : (
        'server',
        {
            'choices' : (
                (
                    {
                        'token'           : 'timeout',
                        'data'            : {
                                               'id' : 'tacacs',
                                               'timeout' : 0
                                            },
                        'obj-type'        : 'tacacs-plus-config',
                        'no-action'       : 'write-object',
                        'short-help'      : 'configure timeout',
                        'doc'             : 'tacacs|tacacs-timeout',
                    },
                    {
                        'field'           : 'timeout',
                        'type'            : 'integer',
                        'data'            : {
                                              'id' : 'tacacs'
                                            },
                        'obj-type'        : 'tacacs-plus-config',
                        'action'          : 'write-object',
                        'optional-for-no' : True,
                    }, 
                ),
                (
                    {
                        'token'           : 'key',
                        'data'            : {
                                              'id' : 'tacacs',
                                              'key' : '',
                                            },
                        'no-action'       : 'write-object',
                        'obj-type'        : 'tacacs-plus-config',
                        'short-help'      : 'set shared TACACS server key',
                        'doc'             : 'tacacs|tacacs-key',
                    },
                    {
                        'field'           : 'key',
                        'type'            : 'string',
                        'data'            : { 'id' : 'tacacs' },
                        'obj-type'        : 'tacacs-plus-config',
                        'action'          : 'write-object',
                        'optional-for-no' : True,
                    },
                ),
                (
                    {
                        'field'           : 'ip',
                        'tag'             : 'host',
                        'type'            : 'ip-address-not-mask',
                        'completion'      : 'complete-object-field',
                        'short-help'      : 'set specific TACACS host properties',
                        'doc'             : 'tacacs|tacacs-host',
                    },
                    {
                        'optional'                : True,
                        'optional-for-no'         : True,
                        'args' : (
                            {
                                'token'           : 'key',
                                'no-action'       : 'reset-fields-explicit',
                                'data'            : { 'key' : '' },
                                'obj-type'        : 'tacacs-plus-host',
                                'short-help'      : 'set shared TACACS server key',
                                'doc'             : 'tacacs|tacacs-host-key',
                            },
                            {
                                'optional-for-no'     : True,
                                'args'                : {
                                    'field'           : 'key',
                                    'type'            : 'string',
                                    'obj-type'        : 'tacacs-plus-host',
                                },
                            }
                        ),
                    },
                ),
            ),
        },
    )
}

TACACS_AAA_AUTHORIZATION_EXEC_COMMAND_DESCRIPTION = {
    'name'         : 'aaa',
    'mode'         : 'config*',
    'short-help'   : 'Configure authorization parameters',
    'command-type' : 'config',
    'doc'          : 'aaa|aaa-authorization',
    'doc-example'  : 'aaa|aaa-authorization-example',
    'obj-type'     : 'tacacs-plus-config',
    'no-action'    : 'reset-fields-explicit',
    'fields'       : [ 'tacacs-plus-authz', 'local-authz' ],
    'args'         : (
        'authorization',
        {
            'token'      : 'exec',
            'short-help' : 'Configure authorization for starting a shell',
            'doc'        : 'aaa|aaa-authorization-exec',
        },
        {
            'token'      : 'default',
            'short-help' : 'Configure authorization for the default channel',
            'doc'        : 'aaa|aaa-authorization-exec-use-default',
        },
        {
            'optional-for-no' : True,
            'choices'         : (
                {
                    'token'      : 'local',
                    'short-help' : 'Use local database for authorization',
                    'doc'        : 'aaa|aaa-authorization-exec-use-local',
                    'action'     : {
                        'proc'   : 'write-object',
                        'data'   : {
                                     'id'                : 'tacacs',
                                     'local-authz'       : True,
                                     'tacacs-plus-authz' : False,
                                   },
                        },
                    },
                (
                    {
                        'token'      : 'group',
                        'short-help' : 'Use a server-group',
                    },
                    {
                        'token'      : 'tacacs+',
                        'short-help' : 'Use list of all defined TACACS+ hosts',
                        'doc'        : 'aaa|aaa-authorization-exec-use-tacacs',
                        'action'     : {
                            'proc'   : 'write-object',
                            'data'   : {
                                        'id'                : 'tacacs',
                                        'local-authz'       : False,
                                        'tacacs-plus-authz' : True,
                                       },
                        },
                    },
                    {
                        'token'      : 'local',
                        'short-help' : 'Use local database for authorization',
                        'doc'        : 'aaa|aaa-authorization-exec-use-local',
                        'optional'   : True,
                        'action'     : {
                            'proc'   : 'write-object',
                            'data'   : {
                                        'id'                : 'tacacs',
                                        'local-authz'       : True,
                                        'tacacs-plus-authz' : True,
                                       },
                        },
                    },
                ),
            ),
        },
    ),
}

TACACS_AAA_AUTHENTICATION_LOGIN_COMMAND_DESCRIPTION = {
    'name'         : 'aaa',
    'mode'         : 'config*',
    'short-help'   : 'Configure authentication parameters',
    'doc'          : 'aaa|aaa-authentication',
    'doc-example'  : 'aaa|aaa-authentication-example',
    'command-type' : 'config',
    'no-action'    : 'reset-fields-explicit',
    'obj-type'     : 'tacacs-plus-config',
    'fields'       : [ 'tacacs-plus-authn', 'local-authn' ],
    'args'         : (
        'authentication',
        {
            'token'      : 'login',
            'short-help' : 'Configure authentication for starting a shell',
            'doc'        : 'aaa|aaa-authentication-login',
        },
        {
            'token'      : 'default',
            'short-help' : 'Configure authentication via the default channel',
            'doc'        : 'aaa|aaa-authentication-login-use-default',
        },
        {
            'optional-for-no' : True,
            'choices'         : (
                (
                    {
                        'token'      : 'local',
                        'short-help' : 'Use local database for authentication',
                        'doc'        : 'aaa|aaa-authentication-login-use-local',
                        'action'     : {
                            'proc'   : 'write-object',
                            'data'   : {
                                         'id'                : 'tacacs',
                                         'local-authn'       : True,
                                         'tacacs-plus-authn' : False,
                                       },
                        },
                    },
                ),
                (
                    'group',
                    {
                        'token'      : 'tacacs+',
                        'short-help' : 'Use list of all defined TACACS+ hosts',
                        'doc'        : 'aaa|aaa-authentication-login-use-tacacs',
                        'action'     : {
                            'proc'   : 'write-object',
                            'data'   : {
                                         'id'                : 'tacacs',
                                         'local-authn'       : False,
                                         'tacacs-plus-authn' : True,
                                       },
                        },
                    },
                    {
                        'token'      : 'local',
                        'optional'   : True,
                        'short-help' : 'Use local database for authentication',
                        'doc'        : 'aaa|aaa-authentication-login-use-local',
                        'action'     : {
                            'proc'   : 'write-object',
                            'data'   : {
                                         'id'                : 'tacacs',
                                         'local-authn'       : True,
                                         'tacacs-plus-authn' : True,
                                       },
                        },
                    },
                ),
            ),
        },
    ),
}

TACACS_AAA_ACCOUNTING_EXEC_COMMAND_DESCRIPTION = {
    'name'         : 'aaa',
    'mode'         : 'config*',
    'short-help'   : 'Configure accounting parameters',
    'doc'          : 'aaa|aaa-accounting',
    'doc-example'  : 'aaa|aaa-accounting-example',
    'all-help'     : 'Authentication, Authorization, and Accounting',
    'command-type' : 'config',
    'obj-type'     : 'tacacs-plus-config',
    'no-action'    : 'reset-fields-explicit',
    'fields'       : [ 'tacacs-plus-acct' ],
    'args'         : (
        'accounting',
        {
            'token'      : 'exec',
            'short-help' : 'Configure accounting for starting a shell',
            'doc'        : 'aaa|aaa-accounting-exec',
        },
        {
            'token'      : 'default',
            'short-help' : 'Configure accounting for the default channel',
            'doc'        : 'aaa|aaa-authorization-exec-use-default',
        },
        {
            'optional-for-no' : True,
            'choices' : (
                (
                    {
                        'token'      : 'none',
                        'short-help' : 'No accounting',
                        'doc'        : 'aaa|aaa-accounting-exec-use-none',
                        'action'     : {
                            'proc'   : 'write-object',
                            'data'   : {
                                          'id'                : 'tacacs',
                                          'tacacs-plus-acct'  : False,
                                       },
                            },
                        },
                    ),
                (
                    {
                        'token'      : 'start-stop',
                        'short-help' : 'Record start and stop',
                        'doc'        : 'aaa|aaa-accounting-exec-use-start-stop',
                    },
                    {
                        'token'      : 'group',
                        'short-help' : 'Use a server-group',
                        'doc'        : 'aaa|aaa-accounting-exec-use-server-group',
                    },
                    {
                        'token'      : 'tacacs+',
                        'short-help' : 'Use list of all defined TACACS+ hosts',
                        'doc'        : 'aaa|aaa-accounting-exec-use-tacacs',
                        'action'     : {
                            'proc'   : 'write-object',
                            'data'   : {
                                         'id'                : 'tacacs',
                                         'tacacs-plus-acct'  : True,
                                       },
                            },
                        },
                    ),
                ),
            },
        ),
    }

SHOW_TACACS_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'short-help'   : 'Show TACACS operational state',
    'doc'          : 'tacacs|show',
    'doc-example'  : 'tacacs|show-example',
    'command-type' : 'display-table',
    'obj-type'     : 'tacacs-plus-config',
    'parent-field' : None,
    'args'         : (
        'tacacs',
    ),
    'action'       : (
        {
            'proc'     : 'display-table',
        },
        {
            'proc'     : 'display-table',
            'title'    : '\nTACACS Server Hosts\n',
            'sort'     : 'timestamp',
            'obj-type' : 'tacacs-plus-host',
        },
    ),
}

SHOW_TACACS_PLUS_CONFIG_FORMAT = {
    'tacacs-plus-config' : {
        'field-orderings' : {
            'default' :  [ 'Idx',
                          'tacacs-plus-authn',
                          'tacacs-plus-authz',
                          'tacacs-plus-acct',
                          'local-authn',
                          'local-authz',
                          'timeout',
                          'key',
                         ],
        },
    },
}

SHOW_TACACS_PLUS_HOST_FORMAT = {
    'tacacs-plus-host' : {
        'field-orderings' : {
            'default' :  [ 'Idx', 'ip', 'key',
                         ],
        },
        'fields' : {
            'Idx' : {
                        'verbose-name' : '#',
                    },
            'ip'  : {
                        'verbose-name' : 'Ip Address',
                    },
            'key' : {
                    },

        }
    }
}
"""