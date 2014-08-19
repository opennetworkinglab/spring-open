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

# snmp.
#

import run_config
import utif


#
# --------------------------------------------------------------------------------

def create_obj_type_dict(context, obj_type, field):
    """
    Should there be some collections of middeleware functions for use by packages?
    """
    entries = context.get_table_from_store(obj_type)
    result_dict = {}
    for entry in entries:
        if entry[field] in result_dict:
            result_dict[entry[field]].append(entry)
        else:
            result_dict[entry[field]] = [entry]

    return result_dict

#
# --------------------------------------------------------------------------------

def running_config_snmp(context, config, words):
    s_obj_type  = 'snmp-server-config'

    snmp_config = context.get_table_from_store(s_obj_type)
    if len(snmp_config) > 1:
        print 'running_config_snmp: more than one snmp record'
    if len(snmp_config) == 0:
        return

    snmp_config = snmp_config[0]


    s_config = []
    if run_config.not_default_value(s_obj_type, 'community', snmp_config.get('community')):
        s_config.append('snmp-server community ro %s\n' %
                        utif.quote_string(snmp_config['community']))

    if run_config.not_default_value(s_obj_type, 'location', snmp_config.get('location')):
        s_config.append('snmp-server location %s\n' %
                        utif.quote_string(snmp_config['location']))

    if run_config.not_default_value(s_obj_type, 'contact', snmp_config.get('contact')):
        s_config.append('snmp-server contact %s\n' %
                        utif.quote_string(snmp_config['contact']))
    if run_config.not_default_value(s_obj_type, 'server-enable', snmp_config['server-enable']):
        s_config.append('snmp-server enable\n')

    if len(s_config):   
        config.append('!\n')
        config += s_config


#
# --------------------------------------------------------------------------------

snmp_running_config_tuple = (
    (
        {
            'optional'   : False,
            'field'      : 'running-config',
            'type'       : 'enum',
            'values'     : 'snmp',
            'short-help' : 'Configuration for SNMP',
            'doc'        : 'running-config|show-snmp',
        },
    ),
)

run_config.register_running_config('snmp', 2300,  None,
                                   running_config_snmp,
                                   snmp_running_config_tuple)
