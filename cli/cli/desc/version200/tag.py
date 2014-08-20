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
#
#

TAG_SUBMODE_COMMAND_DESCRIPTION = {
    'name'          : 'tag',
    'short-help'    : 'Enter tag, configure switch details',
    'doc'           : 'tag|tag',
    'doc-example'   : 'tag|tag-example',
    'mode'          : 'config*',
    'obj-type'      : 'tag',
    'command-type'  : 'config-submode',
    'submode-name'  : 'config-tag',
    'doc'           : 'tag|tag',
    'args' : (
        {
            'field'         : 'id',
            'type'          : 'string',
            #'completion'    : 'complete-object-field',
            'completion'    : 'complete-tag-mapping',
            'data-handler'  : 'convert-tag-to-parts',
            'namespace-key' : 'namespace',
            'name-key'      : 'name',
            'value-key'     : 'value',
            'syntax-help'   : 'Enter tag: [namespace].name=value',
        }
    )
}

TAG_MATCH_COMMAND_DESCRIPTION = {
    'name'         : 'match',
    'mode'         : 'config-tag',
    'command-type' : 'config-object',
    'parent-field' : 'tag',
    'obj-type'     : 'tag-mapping',
    'short-help'   : 'Set the match rule for this tag',
    'doc'          : 'tag|match',
    'doc-example'  : 'tag|match-example',
    'args'         : (
        {
            'field'           : 'mac',
            'tag'             : 'mac',
            'type'            : 'host',
            'completion'      : [ 'complete-alias-choice',
                                ],
            'other'           : 'host',
            'data-handler'    : 'alias-to-value',
            'optional'        : True,
            'optional-for-no' : True,
            'short-help'      : 'Match on MAC address',
        },
        {
            'field'           : 'vlan',
            'tag'             : 'vlan',
            'type'            : 'integer',
            'range'           : (0,4095),
            'optional'        : True,
            'optional-for-no' : True,
            'short-help'      : 'Match on VLAN tag',
        },
        {
            'optional'        : True,
            'optional-for-no' : True,
            'args'            : (
                {
                    'field'        : 'dpid',
                    'tag'          : 'switch',
                    'type'         : 'dpid',
                    'short-help'   : 'Match on switch DPID',
                    'completion'   : [
                                       'complete-object-field',
                                       'complete-alias-choice',
                                     ],
                    'other'        : 'switches',
                    'data-handler' : 'alias-to-value',
                    'syntax-help'  : 'Enter a switch dpid or alias',
                },
                {
                    'field'           : 'ifname',
                    'short-help'      : 'Match on switch interface name',
                    'optional'        : True,
                    'completion'      : [ 'complete-object-field',
                                          'complete-from-another',
                                        ],
                    'other'           : 'port|name',
                    'scoped'          : 'dpid',
                    'data-handler'    : 'warn-missing-interface',
                    'optional'        : True,
                    'optional-for-no' : True,
                },
            )
        },
    ),
}


SHOW_TAG_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'short-help'   : 'Show configured tags',
    'no-supported' : False,
    'command-type' : 'display-table',
    'obj-type'     : 'tag',
    'doc'          : 'tag|show-tag',
    'args' : (
        'tag',
        {
            'field'        : 'namespace',
            'short-help'   : 'Show tags in a given namespace',
            'tag'          : 'namespace',
            'optional'     : True,
            'completion'   : 'complete-object-field',
            'parent-field' : 'tag',
            'other'        : 'tag',
            'action'       : 'display-table',
        },
        {
            'field'        : 'name',
            'tag'          : 'name',
            'short-help'   : 'Show tags with a given name',
            'optional'     : True,
            'completion'   : 'complete-object-field',
            'parent-field' : 'tag',
            'other'        : 'tag',
            'action'       : 'display-table',
        },
        {
            'field'        : 'value',
            'tag'          : 'value',
            'short-help'   : 'Show tags with a given value',
            'optional'     : True,
            'completion'   : 'complete-object-field',
            'parent-field' : 'tag',
            'other'        : 'tag',
            'action'       : 'display-table',
        },
    ),
}

TAG_FORMAT = {
    'tag' : {
        'field-orderings' : {
            'default'   : [ 'Idx', 'namespace', 'name', 'value', 'persist' ]
            },
        'fields' : {
                'namespace' :  {
                                 'verbose-name': 'Namespace',
                               },
                'name'      :  {
                                 'verbose-name': 'Name',
                               },
                'value'     :  {
                                 'verbose-name': 'Value',
                               },
        },
    },
}
"""