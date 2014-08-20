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

# (c) in 2012 by Big Switch Networks - All rights reserved
#
# vCenter commands
#
"""
import os
VCENTER_COMMAND_DESCRIPTION = {
    'name'         : 'vcenter',
    #'no-help'      : 'Remove vcenter configuration',
    'mode'         : 'config*',
    'feature'      : 'experimental',
    'command-type' : 'config-submode',
    'short-help'   : 'Enter vcenter submode, configure vcenter details',
    'doc'          : 'vcenter|vcenter',
    'doc-example'  : 'vcenter|vcenter-example',
    'obj-type'     : 'vcenter',
    'parent-field' : None,
    'current-mode-obj-id' : None,
    'submode-name' : 'config-vcenter',
    # 'parent-id'           : 'vcenter-name',
    'args'     : (
        { 'field'        : 'vcenter-name',
          'type'         : 'identifier',
          'completion'   : 'complete-object-field',
          'syntax-help'  : 'Enter a vcenter name',
        }
    ),
    'action': 'push-mode-stack',
    'no-action': (
        {
           'proc'   : 'confirm',
           'prompt' : "Deleting a vcenter will cause vcenter" \
                      " DVS's to also be deleted, continue (y or yes)? "
        },
        {
            'proc' : 'delete-objects',
        }
    )
}


SHOW_VCENTER_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'feature'      : 'experimental',
    'short-help'   : 'Show vcenter configurations',
    'doc'          : 'vcenter|show',
    'doc-example'  : 'vcenter|show-example',
    'obj-type'     : 'vcenter',
    'command-type' : 'display-table',

    'args' : (
        'vcenter',
    )
}


SHOW_VCENTER_NAME_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'feature'      : 'experimental',
    'short-help'   : 'Show vcenter description by name',
    'doc'          : 'vcenter|show-name',
    'doc-example'  : 'vcenter|show-name-example',
    'obj-type'     : 'vcenter',
    'command-type' : 'display-table',
    'args' : (
        'vcenter',
        {
            'choices' : (
                {
                    'field'   : 'vcenter-name',
                    'type'    : 'identifier',
                },
                {
                    'field'        : 'vcenter-name',
                    'type'         : 'enum',
                    'values'       : 'all',
                    'completion'   : 'complete-object-field',
                },
            )
        },
        {
            'optional' : True,
            'choices' : (
                {
                    'field'      : 'dvs',
                    'type'       : 'enum',
                    'values'     : 'dvs',
                    'obj-type'   : 'dvs',
                    'action'     : 'display-table',
                    'short-help' : 'Select dvs for display',
                    'doc'        : 'vcenter|show-name-dvs',
                },
                {
                    'field'      : 'dvs-port-group',
                    'type'       : 'enum',
                    'values'     : 'dvs-port-group',
                    'obj-type'   : 'dvs-port-group',
                    'action'     : 'display-table',
                    'short-help' : 'Select dvs-port-group for display',
                    'doc'        : 'vcenter|show-name-dvs-port-group',
                },
                {
                    'field'      : 'status',
                    'type'       : 'enum',
                    'values'     : 'status',
                    'action'     : 'display-rest',
                    'url'        : 'vcenter/status/%(vcenter-name)s/',
                    'format'     : 'vcenter-status',
                    'detail'     : 'details',
                    'short-help' : 'Display vcenter active state',
                    'doc'        : 'vcenter|show-vcenter-status',
                },
                {
                    'field'    : 'detail',
                    'type'     : 'enum',
                    'values'   : 'details',
                }
            )
        }
    )
}


SHOW_VCENTER_NAME_STATUS_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'feature'      : 'experimental',
    'short-help'   : 'Show vcenter operational status by name',
    'doc'          : 'vcenter|show-vcenter-dvs-status',
    'doc-example'  : 'vcenter|show-vcenter-dvs-status-example',
    'obj-type'     : 'vcenter',
    'command-type' : 'display-rest',
    'args' : (
        'vcenter',
        {
            'field'        : 'vcenter-name',
            'type'         : 'identifier',
            'completion'   : 'complete-object-field',
        },
        {
            'field'      : 'dvs-name',
            'tag'        : 'dvs',
            'obj-type'   : 'dvs',
            'completion' : 'complete-object-field',
        },
        {
            'field'    : 'status',
            'type'     : 'enum',
            'values'   : 'status',
            'url'      : 'vcenter/status/%(vcenter-name)s/%(dvs-name)s',
            'format'   : 'vcenter-dvs-status',
            'detail'   : 'details',
            'action'   : 'display-rest',

        },
    )
}

VCENTER_IP_ADDRESS_COMMAND_DESCRIPTION = {
    'name'         : 'ip',
    'mode'         : 'config-vcenter',
    'short-help'   : 'Associate ip address for vcenter connection',
    'doc'          : 'vcenter|ip',
    'doc-example'  : 'vcenter|ip-example',
    'command-type' : 'config',
    'no-supported' : True,
    'fields'       : [ 'ip' ], # for field reset
    'args'         : {
        'choices' : (
            (
                'address',
                {
                    'field'           : 'ip',
                    'type'            : 'ip-address-not-mask',
                    'optional-for-no' : True,
                    'syntax-help'     : 'Enter an IP Address',
                },
            ),
        )
    }
}

VCENTER_PORT_COMMAND_DESCRIPTION = {
    'name'         : 'port',
    'short-help'   : 'Associate http port for vcenter connection',
    'doc'          : 'vcenter|port',
    'doc-example'  : 'vcenter|port-example',
    'command-type' : 'config',
    'mode'         : 'config-vcenter',
    'no-supported' : False,
    'args' : (
        {
            'field'           : 'port',
            'type'            : 'integer',
            'syntax-help'     : 'Enter an http port number',
            'optional-for-no' : True,
        }
    )
}

VCENTER_DVS_COMMAND_DESCRIPTION = {
    'name'         : 'dvs',
    'mode'         : 'config-vcenter*',
    'command-type' : 'config-submode',
    'parent-field' : 'vcenter-name',
    'obj-type'     : 'dvs',
    'no-supported' : True,
    'submode-name' : 'config-vcenter-dvs',
    'short-help'   : 'Enter vcenter-dvs submode, describe port groups',
    'doc'          : 'vcenter|dvs',
    'doc-example'  : 'vcenter|dvs-example',
    'args' : (
        {
            'field'       : 'datacenter',
            'tag'         : 'datacenter-name',
            'type'        : 'string',
            'completion'  : 'complete-object-field',
            'syntax-help' : 'Enter the vcenter datacenter name',
        },
        {
            'field'       : 'dvs-name',
            'tag'         : 'dvs-name',
            'type'        : 'string',
            'completion'  : 'complete-object-field',
            'syntax-help' : 'Enter the vcenter dvs name',
        },
        {
            'field'       : 'switch-class',
            'tag'         : 'switch-class',
            'type'        : 'string',
            'optional'    : True,
            'optional-for-no': True,
            'completion'  : 'complete-object-field',
            'syntax-help' : 'Enter vmware',
        },
    ),
}

SHOW_DVS_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'feature'      : 'experimental',
    'mode'         : 'login',
    'short-help'   : 'Show vcenter dvs details',
    'doc'          : 'vcenter|show-dvs',
    'doc-example'  : 'vcenter|show-dvs-example',
    'obj-type'     : 'dvs',
    'command-type' : 'display-table',
    'args'         : (
        'dvs',
        {
            'field'    : 'vcenter',
            'tag'      : 'vcenter',
            'optional' : True,
            'type'     : 'string',
        },
        {
            'field'    : 'datacenter',
            'tag'      : 'datacenter',
            'optional' : True,
            'type'     : 'string',
        },
        {
            'field'    : 'dvs',
            'tag'      : 'dvs',
            'optional' : True,
            'type'     : 'string',
        },
    )
}

VCENTER_PORTGROUP_COMMAND_DESCRIPTION = {
    'name'         : 'portgroup',
    'mode'         : 'config-vcenter-dvs',
    'short-help'   : 'Describe dvs portgroup',
    'doc'          : 'vcenter|dvs-portgroup',
    'doc-example'  : 'vcenter|dvs-portgroup-example',
    'command-type' : 'config-object',
    'parent-field' : 'dvs-name',
    'obj-type'     : 'dvs-port-group',
    'no-supported' : True,
    'args' : (
        {
            'field'           : 'portgroup-name',
            'syntax-help'     : 'Enter the vcenter portgroup name for the associated dvs',
            'optional-for-no' : True
        }
    )
}


SHOW_DVS_PORT_GROUP_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'feature'      : 'experimental',
    'mode'         : 'login',
    'short-help'   : 'Show vcenter dvs port-group details',
    'doc'          : 'vcenter|show-dvs-port-group',
    'doc-example'  : 'vcenter|show-dvs-port-group-example',
    'obj-type'     : 'dvs-port-group',
    'command-type' : 'display-table',
    'no-supported' : True,
    'args'         : (
        'dvs-port-group',
        {
            'field'    : 'vcenter',
            'tag'      : 'vcenter',
            'optional' : True,
            'type'     : 'string',
        },
        {
            'field'    : 'datacenter',
            'tag'      : 'datacenter',
            'optional' : True,
            'type'     : 'string',
        },
        {
            'field'    : 'dvs',
            'tag'      : 'dvs',
            'optional' : True,
            'type'     : 'string',
        },
        {
            'field'    : 'portgroup',
            'tag'      : 'portgroup',
            'optional' : True,
            'type'     : 'string',
        },
    )
}

VCENTER_USERNAME_COMMAND_DESCRIPTION = {
    'name'         : 'username',
    'mode'         : 'config-vcenter',
    'short-help'   : 'Configure vcenter username for login',
    'doc'          : 'vcenter|username',
    'doc-example'  : 'vcenter|username-example',
    'command-type' : 'config',
    'fields'       : [ 'username' ], # for field reset
    'no-supported' : True,
    'args' : (
        {
            'field'           : 'username',
            'type'            : 'string',
            'syntax-help'     : 'Enter the vcenter login username',
            'optional-for-no' : True,
        }
    )
}

VCENTER_PASSWORD_COMMAND_DESCRIPTION = {
    'name'         : 'password',
    'mode'         : 'config-vcenter',
    'command-type' : 'config',
    'short-help'   : 'Configure vcenter password for login',
    'doc'          : 'vcenter|password',
    'doc-example'  : 'vcenter|password-example',
    'fields'       : [ 'password' ], # for field reset
    'no-supported' : True,
    'args' : (
        {
            'field'           : 'password',
            'type'            : 'string',
            'syntax-help'     : 'Enter the vcenter login password',
            'optional-for-no' : True,
        }
    )
}

VCENTER_CONNECT_COMMAND_DESCRIPTION = {
    'name'         : 'connect',
    'mode'         : 'config-vcenter',
    'short-help'   : 'Enable vcenter connect',
    'doc'          : 'vcenter|connect',
    'doc-example'  : 'vcenter|connect-example',
    'command-type' : 'config',
    'syntax-help'  : 'Enter to attempt a vcenter connect',
    # 'data' : {'connect' : True },
    'args' : (),
    'action': (
        {
            'proc' : 'write-fields',
            'data' : {'connect' : True}
        },
    ),
    'no-action': (
        {
            'proc' : 'reset-fields',
            'fields' : [ 'connect' ]
        }
    )
}


#
# FORMATS
#

import fmtcnv


VCENTER_FORMAT = {
    'vcenter' : {
        'field-orderings' : {
            'default' : [
                          'Idx',
                          'vcenter-name',
                          'ip', 'port',
                          'username', 'password',
                          'connect',
                        ]
            },
        'fields' : {
            'ip'                          : {
                                            },
            'port'                        : {
                                            },
            'username'                    : {
                                            },
            'password'                    : {
                                            },
            'connect'                     : {
                                            },
            }
        },
}


VCENTER_STATUS_FORMAT = {
    'vcenter-status' : {
        'field-orderings' : {
                'default' : [
                               'Idx',
                               'vcenter',
                               'Status',
                               'DvsList',
                            ],
                'details' : [
                               'vcenter',
                               'Status',
                               'VCenterError',
                               'DvsList',
                            ]
            },

        'fields' : {
            'vcenter'      : { 'verbose-name' : 'VCenter'
                             },
            'Status'       : { 'verbose-name' : 'Status'
                             },
            'VCenterError' : { 'verbose-name' : 'VCenter error'
                             },
            'DvsList'      : { 'verbose-name' : 'Dvses',
                             }
            },
    },
}


VCENTER_DVS_STATUS_FORMAT = {
    'vcenter-dvs-status'  : {
        'field-orderings' : {
            'default'  :    [
                               'Idx',
                               'vcenter',
                               'dataCenter',
                               'dvs',
                               'portgroups',
                           ],
        },

        'fields' : {
            'vcenter'      : { 'verbose-name' : 'VCenter'
                             },
            'dvs'          : { 'verbose-name' : 'Dvs Name',
                             },
            'dataCenter'   : { 'verbose-name' : 'Data Center',
                             },
            'portgroups'   : { 'verbose-name' : 'Port Groups Active',
                             },
        }
    },
}

VCENTER_DVS_FORMAT = {
    'dvs' : {
        'show-this'       : [
                                [ 'dvs', 'default' ],
                                [ 'dvs-port-group', 'scoped' ]
                            ],

        'field-orderings' : {
            'default' : [ 'vcenter-name',
                          'datacenter',
                          'dvs-name',
                          'switch-class',
                        ]
            },
        'fields' : {
            'vcenter-name' : {
                             },
            'datacenter'   : {
                             },
            'dvs-name'     : {
                             },
            'switch-class' : {
                             },
            }
        },
}


VCENTER_DVS_PORT_GROUP_FORMAT = {
    'dvs-port-group' : {
        'field-orderings' : {
                'default' : [ 'vcenter-name',
                              'datacenter',
                              'dvs-name',
                              'portgroup-name'
                            ],
                'scoped'  : [ 'portgroup-name'
                            ],
            },
        'fields' : {
            'dvs-name'       : {
                               },
            'portgroup-name' : {
                               },
            },

        },
}
"""