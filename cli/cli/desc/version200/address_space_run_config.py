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

#  address_space_run_config.py
#
# show running address-space [as]
#

import run_config
import command
import fmtcnv
import utif
import sys
from midw import *

def address_space_running_config_include_field(context, config,
        address_space, obj_type, field, value, indent, prefix = ""):
    """
    Identify fields of obj_types who's values differ from the default
    values, since these need to be included into the running-config
    """
    if context.mi.not_default_value(obj_type, field, value):
        if context.mi.is_field_string(obj_type, field):
            config.append(' ' * (indent + indent) + prefix + field +
                          " %s\n" % utif.quote_string(value))
        else:
            config.append(' ' * (indent + indent) + prefix + field +
                          ' %s\n' % value)

#
# address_space_running_config_details
#
# Show an address-space configuration in detail
#
def address_space_running_config_details (context, config, address_space):
    """
    Display the details for the fields of a address_space which may have
    non-default values.
    """

    #
    # 'active' is True by default. Prepend 'no' into the generated config
    # if this item is not active.
    #
    if address_space['active'] != \
        context.mi.field_default_value('address-space', 'active'):
        config.append('  no active\n')

    #
    # Show the rest of the fields
    #
    address_space_fields = ['description', 'origin', 'priority',
                            'vlan-tag-on-egress']

    for field in sorted(address_space_fields):
        address_space_running_config_include_field(context, config,
            address_space, 'address-space', field,
            address_space.get(field,''), 1)

#
# address_space_running_config_id_rule
#
# Generate indentifier-rule under address-space configuration
# 
def address_space_running_config_id_rule (context, config, address_space,
                                          address_space_id_rule_entries):

    #
    # Iterate through each identifier-rule and show its contents
    #
    for rule in address_space_id_rule_entries:

        #
        # Show the rule header line
        #
        config.append('  identifier-rule %s\n' % rule['rule'])

        #
        # Iterate through each field configurable and generate the config
        # if present.
        #
        for field in ['description', 'active', 'priority']:
            address_space_running_config_include_field(context, config,
                address_space, 'address-space-identifier-rule', field,
                rule.get(field, ''), 2)

        for field in ['vlans', 'tag']:
            address_space_running_config_include_field(context, config,
                address_space, 'address-space-identifier-rule', field,
                rule.get(field, ''), 2, "match ")

        #
        # Manage switch and ports differently, placing both on the
        # same line when ports exist, and replacing the switch alias
        # when its available.
        if 'switch' in rule:
            dpid_or_alias = alias_lookup_with_foreign_key('switch-alias',
                                                          rule['switch'])
            if dpid_or_alias == None:
                dpid_or_alias = rule['switch'] # dpid
            if 'ports' in rule:
                config.append('    match switch %s %s\n' % \
                              (dpid_or_alias, rule['ports']))
            else:
                config.append('    match switch %s\n' % dpid_or_alias)

        #
        # This configuration section complete, print the trailer.
        #


#
# running_config_specific_address_space
#
# Show running configuration of one particular address-space
#
def running_config_specific_address_space (context, config, address_space_name):
    
    #
    # Have a temporary holder to store generated configuration.
    #
    tmp_config = [ ]

    #
    # Retrieve the configuration from the data store. Catch all possible
    # exceptions and report back as appropriate.
    #
    try:
        address_space = context.get_object_from_store(
                            'address-space', address_space_name)
    except:

        #
        # This particular address-space is not available in the
        # data base. Report error back.
        #
        return ('Error: no address-space name %s, Error %s' %  \
               (address_space_name, sys.exc_info()[0]))

    #
    # Show the configured sub items under this address-space.
    #
    address_space_running_config_details(context, tmp_config, address_space)

    #
    # Retrieve all the identifier rules configured. There may not be any
    # rule present. Handle it gracefully.
    #
    address_space_rules = None
    try:
        address_space_rules = context.get_table_from_store(
                                  'address-space-identifier-rule',
                                  'address-space',
                                  address_space_name, "exact")
    except Exception:
        pass

    #
    # Show each rule configured in detail
    #
    if address_space_rules:
        address_space_running_config_id_rule(context, tmp_config, address_space,
                                             address_space_rules)

    #
    # Don't shown empty configuration for default address-space.
    #
    if len(tmp_config) > 0 or address_space['name'] != 'default':
        config.append("address-space %s\n" % address_space_name)
        config += tmp_config

#
# running_config_address_space
#
# Show running configuration for 'all' address-space configured
#
def running_config_address_space (context, config, words):
    """
    Add the Address Space configuration detils
    """

    #
    # Check if this is request for a specific address-space
    #
    if len(words) > 0:
        return running_config_specific_address_space(context, config, words[0])

    #
    # Retrieve all address-space configurations
    #
    address_space_list = None
    try:
        address_space_list = \
            context.get_table_from_store('address-space')
    except Exception:
        pass

    #
    # Retrieve all address-spaces' identifier rules
    #
    address_space_rules = None
    try:
        address_space_rules = \
            context.get_table_from_store('address-space-identifier-rule')
    except Exception:
        pass

    #
    # Walk each address-space found and print its contents
    #
    if address_space_list:
        for address_space in address_space_list:
            tmp_config = [ ]

            #
            # Now print its contents in details
            # 
            address_space_running_config_details(context, tmp_config,
                                                 address_space)

            #
            # If there is any identifier-rule configured, show each of them.
            #
            if address_space_rules:
                address_space_running_config_id_rule(context, tmp_config,
                    address_space,
                    [rule for rule in address_space_rules \
                    if rule['address-space'] == address_space['name']])


            #
            # Don't shown empty configuration for default address-space.
            #
            if len(tmp_config) > 0 or address_space['name'] != 'default':
                config.append("!\naddress-space %s\n" % address_space['name'])
                config += tmp_config

#
# address_space_running_config_tuple
#
# address-space configuration tuple that we intend to process wrt
# generating their running configuration
#
address_space_running_config_tuple = (
    (
        #
        # show running-configuration address-space
        #
        {
            'optional'   : False,
            'field'      : 'running-config',
            'type'       : 'enum',
            'values'     : 'address-space',
            'short-help' : 'Configuration for address spaces',
            'doc'        : 'running-config|show-address-space',
        },

        #
        # show running-configuration address-space <address-space>
        #
        {
            'field'        : 'word',
            'type'         : 'identifier',
            'completion'   : 'complete-from-another',
            'other'        : 'address-space|name',
            'parent-field' : None,
            'action'       : 'legacy-cli',
            'optional'     : True,
        }
    ),
)

#
# Register with run_config module, our callback to process running configs for
# address-space configuration items
#
run_config.register_running_config('address-space', 4000, None,
                                   running_config_address_space,
                                   address_space_running_config_tuple)

