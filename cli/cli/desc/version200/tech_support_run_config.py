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

# show running tech_support_config [as]
#

import run_config
import utif

def running_config_tech_support(context, config, words):
    # waiting for new tag-mapping to arrive to rewrite
    try:
        tech_support_entries = context.get_table_from_store('tech-support-config')
    except Exception, e:
        tech_support_entries = []
    t_config = []
    for ts_entry in tech_support_entries:
        t_config.append("tech-support-config %s '%s'\n" % (ts_entry['cmd-type'], utif.quote_string(ts_entry['cmd'])))
    if len(t_config) > 0:
        config.append('!\n')
        config += t_config


tag_running_config_tuple = (
                            (
                             {
                             'optional'   : False,
                             'field'      : 'running-config',
                             'type'       : 'enum',
                             'values'     : 'tech-support',
                             'short-help' : 'Configuration for tech support',
                             'doc'        : 'running-config|show-tech-support',
                             },
                             ),
                            )

#
# Register with run_config module, our callback to process running configs for
# address-space configuration items
#
run_config.register_running_config('tech-support', 20000, None,
                                   running_config_tech_support,
                                   tag_running_config_tuple)

