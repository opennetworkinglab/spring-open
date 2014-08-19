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

# topology config.
#

import run_config

#
# --------------------------------------------------------------------------------

def running_config_topology(context, config, words):
    t_obj_type = 'topology-config'
    
    topology_config = context.get_table_from_store(t_obj_type)
    if len(topology_config) > 1:
        print 'running_config_topology: more than one topology record'
    if len(topology_config) == 0:
        return

    topology_config = topology_config[0]

    if run_config.not_default_value(t_obj_type, 'autoportfast',
                                topology_config.get('autoportfast')):
        config.append('\nno topology autoportfast\n')

#
# --------------------------------------------------------------------------------

topology_running_config_tuple = (
    (
        {
            'optional'   : False,
            'field'      : 'running-config',
            'type'       : 'enum',
            'values'     : 'topology',
            'short-help' : 'Topology Configuration',
            'doc'        : 'running-config|show-topology',
        },
    ),
)

run_config.register_running_config('topology', 2100,  None,
                                   running_config_topology,
                                   topology_running_config_tuple)
