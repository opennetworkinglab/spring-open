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

# The database/model descriptions exist to meet particular
# needs, for example, switch-alias exist to provide an 
# alternate name space from dpid's, to allow for a more
# readable and human usable form of the same dpid.  Aliases
# would then naturally need a alias->dpid conversion, and
# at the same time, a dpid->alias (at least for the display
# of dpid's).
#
# The functions in this file provide these type of abstractions,
# taking the output from model lookup's in the rest api, and
# supplying some service used by the cli.
#

import rest_to_model
import fmtcnv
import json
import utif

def init_midware(bs, modi):
    global sdnsh, mi
    sdnsh = bs
    mi = modi

#
# --------------------------------------------------------------------------------

def create_obj_type_dict(obj_type, field, key = None, value = None):
    """
    Return a dictionary from a table search, where the key is one of the
    fields.  This doesn't manage multiple field matches.

    Typically, the field selected is a foreign key for the obj_type.

    For ('host-network-address', 'host'), this creates a dict
    indexed by the mac address, returning the row in the table associated
    with the mac (since the primary key for 'host' is a mac address).

    For ('tag-mapping', 'host'), this creates a dict indexed by
    the mac, returning the matching row in the table.

    note: This gets the whole table
    """
    if not mi.obj_type_has_field(obj_type, field):
        return {}

    if not mi.obj_type_has_model(obj_type):
        data = {}
        if key and value:
            data[key] = value
        rows = rest_to_model.get_model_from_url(obj_type, data)
    elif not type(key) is dict:
        try:
            rows = sdnsh.get_table_from_store(obj_type, key, value)
        except Exception, e:
            errors = sdnsh.rest_error_to_dict(e)
            print sdnsh.rest_error_dict_to_message(errors)
            rows = []
    else:
        try:
            rows = sdnsh.rest_query_objects(obj_type, key)
        except Exception, e:
            errors = sdnsh.rest_error_to_dict(e)
            print sdnsh.rest_error_dict_to_message(errors)
            rows = []

    s_dict = {}
    for row in rows:
        if row[field] in s_dict:
            s_dict[row[field]].append(row)
        else:
            s_dict[row[field]] = [row]

    return s_dict


#
# ALIAS
#

#
# --------------------------------------------------------------------------------

def alias_lookup(alias_obj_type, alias_id):
    """
    Return the value for the alias replacement by looking it up in the store.
    When there is no alias replacement, return None.
    """
    field = mi.alias_obj_type_field(alias_obj_type)
    if not field:
        print sdnsh.error_msg("Error: no field for alias")
        return None
    try:
        alias_key = mi.pk(alias_obj_type)
        # use an exact search instead of a 'get_object...()' since
        # a miss for an exact search can lead to a 404 error, which
        # gets recorded in the error logs
        alias_row = sdnsh.get_table_from_store(alias_obj_type,
                                               alias_key,
                                               alias_id,
                                               "exact")
        if len(alias_row) == 1:
            return alias_row[0][field]
        # only len(alias_row) == 0 at this point
    except:
        pass
    return None

#
# --------------------------------------------------------------------------------

def convert_alias_to_object_key(obj_type, name_or_alias):
    """
    For a specific obj_type (table/model) which may have an alias 'row',
    return the alias when it exists for this name_or_alias.
    """
    if obj_type in mi.alias_obj_type_xref:
        if name_or_alias in sdnsh.reserved_words:
            return name_or_alias
        for alias in mi.alias_obj_type_xref[obj_type]:
            alias_value = alias_lookup(alias, name_or_alias)
            if alias_value:
                return alias_value
    return name_or_alias

#
# --------------------------------------------------------------------------------

def alias_choices_for_alias_obj_type(entries, obj_type, text):
    """
    Used to return all choices of entries for an alias.  Remove any original
    items which appear in the entries list passed in  preventing duplication
    of entries

    Also see cp_alias_choices(), which is similar, but includes
    the current mode.
    """
    if obj_type in mi.alias_obj_type_xref:
        for alias in mi.alias_obj_type_xref[obj_type]:
            try:
                key = mi.pk(alias)
                alias_dict = create_obj_type_dict(alias, key, key, text)
                #
                # remove the alias name if the dpid is in the
                # list of entries... In all cases the alias is added,
                # especially since the alias_dict may only contain selected
                # entries from the 'text' query, and entries may already
                # exclude those items.
                alias_field = mi.alias_obj_type_field(alias)
                if not alias_field:
                    continue
                for item in alias_dict:
                    if alias_dict[item][0][alias_field] in entries:
                        entries.remove(alias_dict[item][0][alias_field])
                    entries.append(item)

            except Exception, e:
                pass
    return entries

#
# --------------------------------------------------------------------------------

def alias_lookup_with_foreign_key(alias_obj_type, foreign_key):
    """
    Find the alias name for some alias based on the foreign key's
    value it's associaed with.
    """
    foreign_field = mi.alias_obj_type_field(alias_obj_type)
    try:
        rows = sdnsh.get_table_from_store(alias_obj_type,
                                          foreign_field,
                                          foreign_key,
                                          "exact")
    except Exception, e:
        errors = sdnsh.rest_error_to_dict(e)
        print sdnsh.rest_error_dict_to_message(errors)
        rows = []
    if len(rows) == 1:
        return rows[0][mi.pk(alias_obj_type)]
    return None


#
# Interface between the cli and table output requires dictionaries
# which map between low item type values (for example, dpid's) and
# alias names for the items (for example, switch aliases), to be
# updated before display.   If cassandra could provide some version
# number (or hash of the complete table), the lookup could be avoided
# by valiating that the current result is up-to-date.
#

#
# --------------------------------------------------------------------------------

def update_show_alias(obj_type):
    """
    Update alias associations for the pretty printer, used for the
    'show' of tables
    """
    if obj_type in mi.alias_obj_type_xref:
        for alias in mi.alias_obj_type_xref[obj_type]:
            field = mi.alias_obj_type_field(alias)
            if not field:
                print sdnsh.error_msg("update show alias alias_obj_type_field")
                return
            try:
                table = sdnsh.get_table_from_store(alias)
            except Exception, e:
                table = []

            new_dict = {}
            key = mi.pk(alias)
            # (foreign_obj, foreign_field) = \
                # mi.foreign_key_references(alias, field)
            for row in table:
                new_dict[row[field]] = row[key]
            fmtcnv.update_alias_dict(obj_type, new_dict)
    return

#
# --------------------------------------------------------------------------------

def update_switch_alias_cache():
    """
    Update the cliModeInfo prettyprinting switch table
    """
    return update_show_alias('switch-config')

#
# --------------------------------------------------------------------------------

def update_switch_port_name_cache():
    """
    Update the cliModeInfo prettyprinting portNames table
    """
    # return update_show_alias('port')

    errors = None
    switch_port_to_name_dict = {}

    try:
        ports = rest_to_model.get_model_from_url('interfaces', {})
    except Exception, e:
        errors = sdnsh.rest_error_to_dict(e)

    if errors:
        print sdnsh.rest_error_dict_to_message(errors)
        return

    for port in ports:
        key_string = port['switch'] + "." + "%d" % port['portNumber']
        switch_port_to_name_dict[key_string] = port['portName']
    fmtcnv.update_alias_dict("portNames", switch_port_to_name_dict)

#
# --------------------------------------------------------------------------------

def update_host_alias_cache():
    """
    Update the cliModeInfo prettyprinting host table
    """
    return update_show_alias('host-config')


#
# --------------------------------------------------------------------------------
# update_flow_cookie_hash

def update_flow_cookie_hash():
    """
    The formatter keeps a map for static flow entries.
    """
    # iterate through all the static flows and get their hashes once
    flow_map = {}
    prime = 211
    for sf in sdnsh.get_table_from_store("flow-entry"):
        flow_hash = 2311
        for i in range(0, len(sf['name'])):
            flow_hash = flow_hash * prime + ord(sf['name'][i])
        flow_hash = flow_hash & ( (1 << 20) - 1)
        flow_map[flow_hash] = sf['name']

    fmtcnv.update_alias_dict("staticflow", flow_map)

    fmtcnv.callout_flow_encoders(sdnsh)


#
# --------------------------------------------------------------------------------
#

def update_controller_node_alias_cache():
    return update_show_alias('controller-node')

#
# --------------------------------------------------------------------------------
#
def obj_type_show_alias_update(obj_type):
    """
    When some item is about to be displayed, particular 'alias'
    items for the display may require updating.  instead of just
    updating everything all the time, peek at the different formatting
    functions and use those function names to determine what needs to
    be updated.

    Also see formatter_to_update in climodelinfo, since it may
    need to include new formatting functions.
    """
    update = {}
    sdnsh.pp.format_to_alias_update(obj_type, update)

    # select objects from 'update' dict
    if 'host' in update:
        update_host_alias_cache()
    if 'switch' in update:
        update_switch_alias_cache()
    if 'port' in update:
        update_switch_port_name_cache()
    if 'flow' in update:
        update_flow_cookie_hash()
    if 'controller-node' in update:
        update_controller_node_alias_cache()


#
# OBJECTs middleware.
#


#
# --------------------------------------------------------------------------------

def objects_starting_with(obj_type, text = "", key = None):
    """
    The function returns a list of matching keys from table/model
    identified by the 'obj_type' parameter

    If the table/model has a 'alias' field, then this field's
    values are also examined for matches

    The first argument is the name of a table/model in the store,
    while the second argument is a prefix to filter the results.
    The filter is applied to the key of the table/model, which
    was previously populated.
    """

    if key:
        if not mi.obj_type_has_field(obj_type, key):
            sdnsh.warning("objects_starting_with: %s doesn't have field %s" %
                          (obj_type, key))
    else:
        key = mi.pk(obj_type)
        if key == None:
            sdnsh.warning("objects_starting_with: %s doesn't have pk" %
                          (obj_type))
    key_entries = []

    # Next, find the object
    #  Deal with any changes to the lookup name based on the 'contenation'
    #  of the config mode name to the named identifer.
    #


    case = mi.get_obj_type_field_case_sensitive(obj_type, key)
    id_value = utif.convert_case(case, text)

    if mi.obj_type_has_model(obj_type):
        # from the database
        try:
            entries = sdnsh.get_table_from_store(obj_type, key, id_value)
            errors = None
        except Exception, e:
            errors = sdnsh.rest_error_to_dict(e)
        if errors:
            print sdnsh.rest_error_dict_to_message(errors)
            return key_entries
    else:
        if id_value == '':
            entries = rest_to_model.get_model_from_url(obj_type, {})
        else:
            entries = rest_to_model.get_model_from_url(obj_type, { key + "__startswith" : id_value })

    if key and entries:
        # Expand any key values which are lists (hosts, for example)
        items = [x[key] for x in entries if x.get(key)]
        entries = []
        for item in items:
            if type(item) == list:
                entries += item
            else:
                entries.append(item)
        key_entries = [sdnsh.quote_item(obj_type, x)
                       for x in entries if x.startswith(id_value)]
    #
    # for some specific tables which have id's concatenated from multiple other
    # components, only part of the id is available for completion.
    #
    if mi.is_compound_key(obj_type, key):
        separator_character = mi.compound_key_separator(obj_type, key)
        keyDict = {}
        for key in key_entries:
#            keyDict[key.split(separator_character)[0]] = ''
            keyDict[key] = ''
        key_entries = keyDict.keys()

    alias_obj_type = obj_type
    if key != mi.pk(alias_obj_type):
        # if this is a forgeign key, use the obj_type of the fk.
        if mi.is_foreign_key(alias_obj_type, key):
            (alias_obj_type, fk_name) = mi.foreign_key_references(alias_obj_type, key)
        else:
            # XXX possibly other choices to determine alias_obj_type?
            alias_obj_type = None

    if alias_obj_type:
        obj_type_config = mi.obj_type_related_config_obj_type(alias_obj_type)

        # alias_choices_for_alias_obj_type() removes switch dpid's which
        # have associated alias names,
        key_entries = alias_choices_for_alias_obj_type(key_entries,
                                                       obj_type_config,
                                                       text)

    return key_entries


#
# --------------------------------------------------------------------------------

def local_interfaces_firewall_open(protos, ports, controller_id = None):
    """
    Return a list of interfaces, which have the proto and port currently enabled

    @param proto a string, or list of strings, identifying the protocol
    @param port a strings, or list of strings or ints
    """

    # first collect all associated rules
    if type(protos) != list:
        protos = [protos]
    if type(ports) != list:
        ports = [ports]

    rules = []
    for proto in protos:
        for port in ports:
            query_dict = { 'proto' : proto, 'port' : port }
            rules += sdnsh.rest_query_objects('firewall-rule', query_dict)

    # create a dictionary indexed by the interface, which is part of the pk 'id'
    rules_of_interface = dict([[x['interface'], x] for x in rules])

    if controller_id == None:
        # request 'this' controller
        controller_url = "http://%s/rest/v1/system/controller" % sdnsh.controller
        result = sdnsh.store.rest_simple_request(controller_url)
        sdnsh.check_rest_result(result)
        controller_id = json.loads(result)

    if controller_id != 'all':
        query_dict = { 'controller' : controller_id['id'] }

    ifs = sdnsh.rest_query_objects('controller-interface', query_dict)

    return [ifn for ifn in ifs if ifn['id'] in rules_of_interface]

#
# --------------------------------------------------------------------------------


def log_url(ip_and_port = None, log = None):
    """
    Returns the url of the log's on the named ip_and_port.
    """
    log_path = 'http://%s/rest/v1/system/log' % ip_and_port
    if log:
        log_path += '/' + log
    return log_path


#
# --------------------------------------------------------------------------------


def controller_ip_and_port(controller):
    """
    Return a list of ip:port values for named controllers,
    to use to build urls for REST API's.   If a controller of 'all'
    is passed in, then all the controllers ar enumerated.

    If both port 80, and 8000 are open, then two ip:port 
    pairs will be returned for a controller.  This returns
    ALL values which match,  not a single ip:port for each
    controller.
    """
    url = 'http://%s/rest/v1/system/controller' % sdnsh.controller
    rest_dict = sdnsh.rest_simple_request_to_dict(url)
    this_controller = rest_dict['id']

    ips_80 = [x for x in local_interfaces_firewall_open('tcp', 80,
                                                     controller)
           if (x['ip'] != '' or x['discovered-ip'] != '')]

    ips_8000 = [x for x in local_interfaces_firewall_open('tcp', 8000,
                                                          controller)
           if (x['ip'] != '' or x['discovered-ip'] != '')]

    return ['%s:80' % '127.0.0.1' if x['controller'] == this_controller else
                x['discovered-ip'] if x['discovered-ip'] != '' else x['ip']
            for x in ips_80] + ['%s:8000' %
                '127.0.0.1' if x['controller'] == this_controller else
                x['discovered-ip'] if x['discovered-ip'] != '' else x['ip']
            for x in ips_8000]
