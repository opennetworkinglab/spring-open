#!/usr/bin/env python
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
# CLI for forwarding configuration
#

# --------------------------------------------------------------------------------
# Create new CLI command for forwarding
#

FORWARDING_CONFIG_COMMAND_DESCRIPTION = {
    'name'         : 'forwarding',
    'mode'         : 'config',
    'short-help'   : 'Configure forwarding service properties',
    'doc'          : 'forwarding|forwarding',
    'doc-example'  : 'forwarding|forwarding-example',
    'command-type' : 'config-object',
    'obj-type'     : 'forwarding-config',
    'args': (
        {
            'choices' : (
                {
                    'args' : (
                        {
                            'token'            : 'access-priority',
                            'completion-text'  : 'access-priority',
                            'short-help'       : 'Set proprity for flows created by the forwarding service on an access switch',
                            'doc'              : 'forwarding|forwarding-access-priority',
                            'match-for-no'     : True,
                        },
                        {
                            'field'            : 'access-priority',
                            'short-help'       : 'Set proprity for flows created by the forwarding service on an access switch',
                            'doc'              : 'forwarding|forwarding-access-priority',
                            'type'             : 'integer',
                            'range'            : (0, 2**15-1),
                            'completion-text'  : '0-32767',
                            'match-for-no'     : True,
                            'optional-for-no'  : True,
                            'data'             : {
                                'id'             : 'forwarding',
                                'access-priority': '$data',
                            },
                        },
                    ),
                },
                {
                    'args' : (
                        {
                            'token'            : 'core-priority',
                            'completion-text'  : 'core-priority',
                            'short-help'       : 'Set proprity for flows created by the forwarding service on a core switch',
                            'doc'              : 'forwarding|forwarding-core-priority',
                            'match-for-no'     : True,
                        },
                        {
                            'field'            : 'core-priority',
                            'short-help'       : 'Set proprity for flows created by the forwarding service on a core switch',
                            'doc'              : 'forwarding|forwarding-core-priority',
                            'type'             : 'integer',
                            'range'            : (0, 2**15-1),
                            'completion-text'  : '0-32767',
                            'match-for-no'     : True,
                            'optional-for-no'  : True,
                            'data'             : {
                                'id'             : 'forwarding',
                                'core-priority'  : '$data',
                            },
                        },
                    ),
                },
            ),
        },
    ),
}
"""
# --------------------------------------------------------------------------------
