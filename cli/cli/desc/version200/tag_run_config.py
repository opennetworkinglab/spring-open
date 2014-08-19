#
# Copyright (c) 2012,2013 Big Switch Networks, Inc.
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

# show running tag [as]
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


def running_config_tag(context, config, words):
    # waiting for new tag-mapping to arrive to rewrite
    try:
        tag_mapping = create_obj_type_dict(context,
                                           'tag-mapping',
                                           'tag')
    except Exception, e:
        tag_mapping = {}


    t_config = []

    for tag in context.get_table_from_store('tag'):
        tag_name = '%s.%s=%s' % (tag['namespace'],
                                 tag['name'],
                                 utif.quote_string(tag['value']))
        if len(words) == 1 and tag_name != words[0]:
            continue
        if tag.get('persist', False): 
            t_config.append('tag ' + tag_name + '\n')
            tms = tag_mapping.get(tag['id'], [])
            for tm in tms:
                m = '  match'
                if tm.get('mac', '') != '':
                    m  += ' mac %s' % tm['mac']
                if tm.get('vlan', '') != '':
                    m += ' vlan %s' % tm['vlan']
                if tm.get('dpid', '') != '':
                    m += ' switch %s %s' % (tm['dpid'], tm.get('ifname', ''))
                t_config.append(m + '\n')

    if len(t_config):
        config.append('!\n')
        config += t_config


tag_running_config_tuple = (
    (
        {
            'optional'   : False,
            'field'      : 'running-config',
            'type'       : 'enum',
            'values'     : 'tag',
            'short-help' : 'Configuration for controller object metadata',
            'doc'        : 'running-config|show-tag',
        },
        {
            'field'      : 'word',
            'type'       : 'string',
            'completion' : 'complete-from-another',
            'other'      : 'tag|id',
            'action'     : 'legacy-cli',
            'optional'   : True,
        },
    ),
)

#
# Register with run_config module, our callback to process running configs for
# address-space configuration items
#
run_config.register_running_config('tag', 4000, None,
                                   running_config_tag,
                                   tag_running_config_tuple)

