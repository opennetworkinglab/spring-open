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

# CLI for forwarding configuration
#

# --------------------------------------------------------------------------------
# Update running config
#

import run_config
def running_config_forwarding(context, config, words):
    t_obj_type = 'forwarding-config'

    forwarding_table = context.get_table_from_store(t_obj_type)
    if len(forwarding_table) > 1:
        print 'running_config_forwarding: more than one forwarding record'
    if len(forwarding_table) == 0:
        return

    forwarding_config = forwarding_table[0]
    newline_printed = False
    if run_config.not_default_value(
            t_obj_type, 'access-priority',
            forwarding_config.get('access-priority')):
        if newline_printed is False:
            newline_printed = True
            config.append("!\n")
        config.append(
                'forwarding access-priority %s\n' %
                forwarding_config.get('access-priority'))
    if run_config.not_default_value(
            t_obj_type, 'core-priority',
            forwarding_config.get('core-priority')):
        if newline_printed is False:
            newline_printed = True
            config.append("!\n")
        config.append(
                'forwarding core-priority %s\n' %
                forwarding_config.get('core-priority'))

forwarding_running_config_tuple = (
    (
        {
            'optional'   : False,
            'field'      : 'running-config',
            'type'       : 'enum',
            'values'     : 'forwarding',
            'short-help' : 'Forwarding Configuration',
            'doc'        : 'running-config|forwarding-show',
        },
    ),
)
run_config.register_running_config('forwarding', 2200,  None,
                                   running_config_forwarding,
                                   forwarding_running_config_tuple)

# --------------------------------------------------------------------------------
