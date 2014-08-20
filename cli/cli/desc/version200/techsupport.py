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
# tech-support commands
#

#
# --------------------------------------------------------------------------------
# TECH_SUPPORT_COMMAND_DESCRIPTION
#
#  tech-support-config type command
#  no tech-support-config type command
#
"""
TECH_SUPPORT_COMMAND_DESCRIPTION = {
    'name'         : 'tech-support-config',
    'mode'         : 'config',
    'short-help'   : 'Manage command output for show tech-support',
    'doc'          : 'tech-support|tech-support-config',
    'doc-example'  : 'tech-support|tech-support-config-example',
    'command-type' : 'config-object',
    'obj-type'     : 'tech-support-config',
    'new-style'    : True,
    'args'         : (
        {
            'field'       : 'cmd-type',
            'type'        : 'enum',
            'values'      : ('cli', 'shell'),
            'syntax-help' : 'Enter command type (cli or shell)',
            'doc'         : 'tech-support|tech-support-config-+',
        },              
        {
            'field'       : 'cmd',
            'type'        : 'string',
            'syntax-help' : 'Enter command string'
        },
    )
}

TECH_SUPPORT_CONF_SHOW_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'short-help'   : 'Show tech-support configuration',
    'doc'          : 'tech-support|show-tech-support-config',
    'doc-example'  : 'tech-support|show-tech-support-config-example',
    'obj-type'     : 'tech-support-config',
    'command-type' : 'display-table',
    'args'         : (
        'tech-support-config',
        {
            'field'        : 'cmd-type',
            'optional'     : True,
            'completion'   : 'complete-object-field',
            'type'         : 'enum',
            'values'       : ('cli', 'shell'),
            'syntax-help'  : 'Enter command type (cli or shell)',
            'action'       : 'display-table',
        },
        {
             'field'    : 'cmd',
             'tag'      : 'cmd',
             'type'     : 'string',
             'optional' : True,
        },
    )
}
"""