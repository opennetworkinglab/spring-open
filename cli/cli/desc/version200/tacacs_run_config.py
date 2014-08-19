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

# tacacs.
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

def running_config_tacacs(context, config, words):
    t_obj_type  = 'tacacs-plus-config'
    th_obj_type = 'tacacs-plus-host'

    tacacs = context.get_table_from_store(t_obj_type)
    if len(tacacs) > 1:
        print 'running_config_tacacs: more than one tacacs record'

    tacacs_host = context.rest_query_objects(th_obj_type,
                                             {'orderby' : 'timestamp'})


    t_config = []

    if tacacs:
        tacacs = tacacs[0]

        if run_config.not_default_value(t_obj_type, 'tacacs-plus-authn', tacacs['tacacs-plus-authn']) and \
           run_config.not_default_value(t_obj_type, 'local-authn', tacacs['local-authn']):
            t_config.append('aaa authentication login default group tacacs+\n')
        elif run_config.not_default_value(t_obj_type, 'tacacs-plus-authn', tacacs['tacacs-plus-authn']):
            t_config.append('aaa authentication login default group tacacs+ local\n')

        if run_config.not_default_value(t_obj_type, 'tacacs-plus-authz', tacacs['tacacs-plus-authz']) and \
           run_config.not_default_value(t_obj_type, 'local-authz', tacacs['local-authz']):
            t_config.append('aaa authorization exec default group tacacs+\n')
        elif run_config.not_default_value(t_obj_type, 'tacacs-plus-authz', tacacs['tacacs-plus-authz']):
            t_config.append('aaa authorization exec default group tacacs+ local\n')

        if run_config.not_default_value(t_obj_type, 'tacacs-plus-acct', tacacs['tacacs-plus-acct']):
            t_config.append('aaa accounting exec default start-stop group tacacs+\n')

        if run_config.not_default_value(t_obj_type, 'key', tacacs['key']):
            t_config.append('tacacs server key %s\n' % tacacs['key'])

        if run_config.not_default_value(t_obj_type, 'timeout', tacacs['timeout']):
            t_config.append('tacacs server timeout %s\n' % tacacs['timeout'])

    for h in tacacs_host:
        if run_config.not_default_value(th_obj_type, 'key', h['key']):
            key = ' key %s' %  utif.quote_string(h['key'])
        else:
            key = ''

        t_config.append('tacacs server host %s%s\n' % (h['ip'], key))

    if len(t_config):   
        config.append('!\n')
        config += t_config


#
# --------------------------------------------------------------------------------

tacacs_running_config_tuple = (
    (
        {
            'optional' : False,
            'field'    : 'running-config',
            'type'     : 'enum',
            'values'   : 'tacacs',
            'doc'      : 'running-config|show-tacacs',
            'short-help': 'Configuration for TACACS authentication'
        },
    ),
)

run_config.register_running_config('tacacs', 2000,  None,
                                   running_config_tacacs,
                                   tacacs_running_config_tuple)
