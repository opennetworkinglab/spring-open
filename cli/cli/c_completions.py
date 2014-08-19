#
# Copyright (c) 2011,2012,2013 Big Switch Networks, Inc.
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

import os
import re
import modi
import error
import command
import collections
import utif

from midw import *

def check_rest_result(result, message=None):
    if isinstance(result, collections.Mapping):
        error_type = result.get('error_type')
        if error_type:
            raise error.CommandRestError(result, message)


def pretty(text):
    """
    For object-type's, remove dashes, capitalize first character
    """
    return text.replace('-', ' ').capitalize()


#
# COMPLETION PROCS
#
# 'completions' is a dictionary, where the keys are the actual text
# of the completion, while the value is the reason why this text 
# was added.  The 'reason' provides the text for the two-column
# help printed for the '?' character.
#

def collect_object_fields(obj_type, field, data, mode, completions, 
                          prefix = None, other = None, 
                          parent_field = None, parent_id = None, scoped = None):
    """
    Returns the list of possible completions for a particular obj-type.
    """

    data = dict(data)
    if parent_field:
        data[parent_field] = parent_id
    if prefix:
        data[field + '__startswith'] = prefix

    key = mi.pk(obj_type)
    if scoped:
        obj_id = sdnsh.get_current_mode_obj()
        if sdnsh.current_mode() != mode:
            # XXX needs to be covered, shouldn't reach in like this
            for x in sdnsh.mode_stack:
                if x['mode_name'] == mode:
                    obj_id = x['obj']
        obj_d = { key : obj_id }

        if obj_type in mi.alias_obj_types:
            # the submode ought to identify the foreign key
            data[mi.alias_obj_type_field(obj_type)] = obj_id
        else:
            mi.split_compound_into_dict(obj_type, key, obj_d, is_prefix = True)
            for (k,v) in obj_d.items():
                if k != key and not k in data:
                    data[k] = v
 

    # if this is one of the obj_type's associated with aliases, should
    # the list of values be back-transformed into alias names?
    # yes, because if the current value has an inverse alias, the existing
    # inverse for the type implies that during a previous insert of this
    # value, it was converted from its alias name to the current name.
    #
    # collect the complete collection of aliases, since its likely
    # more than one back-to-alias conversion will be required, and query
    # its value before obj_type in the hope that it was recently cached.
    #
    alias_obj_type = mi.obj_type_related_config_obj_type(obj_type)
    if other and other in mi.alias_obj_type_xref:
        alias_obj_type = mi.alias_obj_type_xref[other][0]
    elif field != mi.pk(obj_type):
        if mi.is_foreign_key(obj_type, field):
            (alias_obj_type, fk_name) = mi.foreign_key_references(obj_type, field)
            alias_obj_type = mi.obj_type_related_config_obj_type(alias_obj_type)
            if alias_obj_type in mi.alias_obj_type_xref:
                alias_obj_type = mi.alias_obj_type_xref[alias_obj_type][0]
            else:
                alias_obj_type = None
        else:
            if sdnsh.description:   # description debugging
                print 'collect_object_fields: no alias for %s ' \
                      'field %s not pk, and not fk' % (obj_type, field)
            alias_obj_type = None
    elif obj_type in mi.alias_obj_type_xref:
        alias_obj_type = mi.alias_obj_type_xref[obj_type][0]
    else:
        alias_obj_type = None

    alias_dict = {}
    if alias_obj_type:
        foreign_field = mi.alias_obj_type_field(alias_obj_type)
        alias_dict = create_obj_type_dict(alias_obj_type, foreign_field)
        alias_key = mi.pk(alias_obj_type)

    # Remove any data fields which have values of None, these are fields
    # which are getting reset.
    for reset_fields in [x for x in data.keys() if data[x] == None]:
        del data[reset_fields]
        
    # collect complete obj_type
    if not mi.obj_type_has_model(obj_type):
        result = rest_to_model.get_model_from_url(obj_type, data)
    else:
        result = sdnsh.rest_query_objects(obj_type, data)
    check_rest_result(result)
    if sdnsh.description:   # description debugging
        print "collect_object_fields:", obj_type, field, data, result

    is_compound = mi.is_compound_key(obj_type, key)
    d = {}
    for item in result:
        if is_compound:
            mi.split_compound_into_dict(obj_type, key, item)
        value = item.get(field)
        # XXX hack to correctly format tag completions
        if obj_type == 'tag' and field == 'id':
            value = '%s.%s=%s' % tuple(value.split('|'))
        # remember to only add new items
        if value:
            if type(value) == list:
                # Need a mechanism to select values from the list, field's not enough
                for item in value:
                    if utif.quote_string(str(item)) not in completions:
                        if str(item) in alias_dict:
                            alias_item = alias_dict[str(item)][0][alias_key]
                            if alias_item.startswith(prefix):
                                item = alias_item
                        d[utif.quote_string(str(item))] = None
            elif utif.quote_string(str(value)) not in completions:
                if str(value) in alias_dict:
                    alias_value = alias_dict[str(value)][0][alias_key]
                    if alias_value.startswith(prefix):
                        value = alias_value
                d[utif.quote_string(str(value))] = None

    # if there's an alias for this object, and a prefix is included,
    # then the alias'es which match also need to be directly included,
    # since its not clear whether the prefix applies to the actual
    # id or the alias.  since alias_dict is already the complete
    # collection of aliases for this obj-type, use it for matching names
    if alias_obj_type and prefix and prefix != '':
        alias_pk = mi.pk(alias_obj_type)
        for (n,v) in alias_dict.items():
            # 'n' here is the foreign key reference to this obj-type
            for item in [x[alias_pk] for x in v if x[alias_pk].startswith(prefix)]:
                if utif.quote_string(str(item)) not in completions:
                    d[utif.quote_string(str(item))] = None

    return utif.add_delim(list(d), ' ')


def complete_object_field(obj_type, field, data, completions,
                          mode = None,
                          prefix = None, other = None, parent_field = None, parent_id = None, scoped = None):
    """
    Populate 'completions' with  the values of the primary key for
    the particular obj_type
    """
    if sdnsh.description:   # description debugging
        print "complete_object_field: ", obj_type, mode, field, data, scoped, other

    if not mi.obj_type_exists(obj_type):
        raise error.CommandDescriptionError("Unknown obj-type: %s" % obj_type)
    result = collect_object_fields(obj_type, field, data, mode, completions,
                                    prefix, other, parent_field, parent_id, scoped)
    completions.update(dict([[x, "%s selection" % pretty(obj_type)]
                              for x in result]))


def complete_tag_mapping(obj_type, field, data, completions,
                         prefix = None, other = None, mode = None,
                         parent_field = None, parent_id = None, scoped = None):
    """
    Translate the completion results from complete_object_field into
    tag values of syntax <namespace.name>=<value
    """
    if not mi.obj_type_exists(obj_type):
        raise error.CommandDescriptionError("Unknown obj-type: %s" % obj_type)

    # since the prefix can contrict the search, and its not clear
    # what the prefix applies to, collect all the possible values,
    # compute wht the item would look like then match the prefix.
    collection = collect_object_fields(obj_type, field, data, mode, completions,
                                       '', other, parent_field, parent_id, scoped)
    if prefix != "":
        collection = [x for x in collection if x.startswith(prefix)]
    completions.update(dict([[x, "tag selection"] for x in collection]))


def complete_from_another(other, obj_type, field, data, completions, no_command,
                          prefix = None,
                          parent_field = None, parent_id = None, scoped = None,explicit=None):
    """
    Completion function used when another obj_type is used to populate
    values for the current obj_type

    the 'other' field identifies the obj_type to use to collect choices from,
    it can consist of two parts  other|field.  When field isn't described here,
    it comes from the description parameter, however, the 'field' value there may
    be in use to describe the value of the associated action.

    """
    if sdnsh.description:   # description debugging
        print "complete_from_another:", other, field, data, parent_field, parent_id, scoped

    # complete_from_another is intended to include other fields, which
    # shouldn't apply for a no command.
    if no_command:
        return

    if other.find('|') >= 0:
        parts = other.split('|')
        other = parts[0]
        field = parts[1]

    if not mi.obj_type_exists(other):
        raise error.CommandDescriptionError("Unknown obj-type/other: %s" % other)

    id = mi.pk(other)
    data = dict(data)
    if parent_field and parent_id:
        data[parent_field] = parent_id
    if prefix:
        data[field + '__startswith'] = prefix
    key = mi.pk(other)
    if scoped:
        key = mi.pk(other)
        if type(scoped) == str and scoped in data:
            obj_d = { key : data[scoped] }
        else:
            obj_d = { key : sdnsh.get_current_mode_obj() }
        mi.split_compound_into_dict(other, key, obj_d, is_prefix = True)
        for (k,v) in obj_d.items():
            if k != key and not k in data:
                data[k] = v
    if mi.is_primitive_compound_key(other, key):
        # try to use the field values to populate the primary key...
        value = ""
        s = mi.compound_key_separator(other, key)
        missing = None
        for kf in mi.deep_compound_key_fields(other, key):
            if kf in data:
                value += data[kf] + s
            else:
                # the fields must appear in order
                missing = kf
                break
        # For prefix extention to work here, the other field must have
        # named the field, for example switch's interface completion,
        # uses "other : 'port|number'"
        post_prefix_match = False
        if prefix:
            post_prefix_match = True
            if missing == field:
                value += prefix
                post_prefix_match = False
        if mi.obj_type_has_model(other):
            result = sdnsh.get_table_from_store(other, key, value)
        else:
            result = rest_to_model.get_model_from_url(other, { key : value } )
        
        if post_prefix_match:
            # try to match the missing field, more work ought to be done
            # to identify whether the 'missing' field is the correect to match against
            # 
            result = [x for x in result
                      if field in x and str(x[field]).startswith(prefix)]
    elif mi.is_compound_key(other, key):
        search = {}
        if parent_id:
            from_id = {mi.pk(obj_type) : parent_id}
            mi.split_compound_into_dict(obj_type,
                                        mi.pk(obj_type),
                                        from_id,
                                        is_prefix = True)
            # the field name used to collapse the result is the last
            # field in the compound key (id of 'other'),  this may need
            # improvement for other commands
            for deep_field in mi.deep_compound_key_fields(other, key):
                if deep_field in from_id:
                    search[deep_field] = from_id[deep_field]
                if deep_field in data:
                    search[deep_field] = data[deep_field]
        if scoped:
            # move known compound fields from obj_d into search.
            for deep_field in mi.deep_compound_key_fields(other, key):
                if deep_field in obj_d:
                    search[deep_field] = obj_d[deep_field]
        #
        # possibly other search keys?
        if prefix:
            search[field + '__startswith'] = prefix
        if explicit:
            search.clear()
            search[scoped]=data[scoped]
            if prefix:
                search[field + '__startswith'] = prefix
        if mi.obj_type_has_model(other):
            result = sdnsh.rest_query_objects(other, search)
        else:
            result = rest_to_model.get_model_from_url(other, search )
    elif mi.obj_type_has_field(other, field) and mi.is_primary_key(other, field):
        result = utif.add_delim(objects_starting_with(other, prefix), ' ')
        completions.update(dict([[x, "%s selection" % pretty(other)]
                                  for x in result]))
        return
    elif mi.obj_type_has_field(obj_type, field) and \
      mi.is_foreign_key(obj_type, field):
        # look up the values of the foreign key's from the other table
        (fk_obj_type, fk_fn) = mi.foreign_key_references(obj_type, field)
        result = sdnsh.get_table_from_store(fk_obj_type, fk_fn, prefix)
        field = fk_fn
    elif mi.obj_type_has_field(obj_type, field) and field == other:
        # In this situation, this obj_type has a field, which seems to be named
        # based on the other model's name, which seems to be requesting to
        # search the other model.
        field = mi.pk(other)
        result += utif.add_delim(objects_starting_with(other, prefix), ' ')
        completions.update(dict([[x, "%s selection" % pretty(other)]
                                  for x in result]))
        return
    else:
        if mi.obj_type_has_model(other):
            result = sdnsh.rest_query_objects(other, data)
        else:
            result = rest_to_model.get_model_from_url(other, data)

    check_rest_result(result)
    if sdnsh.description:   # description debugging
        print "complete_from_another:", other, field, data, len(result)

    d = {}
    for item in result:
        value = item.get(field)
        # XXX hack to correctly format tag completions
        if other == 'tag':
            value = '%s.%s=%s' % tuple(value.split('|'))
        # assume that 'values' are 'unique' within results
        if value and utif.quote_string(value) not in completions:
            d[utif.quote_string(str(value))] = None

    if sdnsh.description:   # description debugging
        print "complete_from_another: final", other, field, data, d.keys()

    result = utif.add_delim(list(d), ' ')
    completions.update(dict([[x, "%s selection" % pretty(other)]
                              for x in result]))


def complete_alias_choice(obj_type, field, data, prefix, completions, no_command,
                          other = None, scoped = None):
    """
    Complete selections from an external object (unlreated to this
    object stack's details), only returning unique keys, either
    aliases for the obj_type, or primary keys.

    This ought to be improved, objects_starting_with() in
    the cli.py, is primarily intended to be use within cli.py
    """
    if not mi.obj_type_exists(obj_type):
        raise error.CommandDescriptionError("Unknown obj-type: %s" % obj_type)

    if sdnsh.description:   # description debugging
        print "complete_alias_choice:", obj_type, field, other, data, prefix, scoped

    if other and no_command == False:
        parts = other.split('|')
        obj_type = parts[0]
        if len(parts) > 1:
            # what to do with more parts?
            field = parts[1]

    if not mi.obj_type_has_field(obj_type, field):
        raise error.CommandDescriptionError("Unknown field %s for obj-type: %s"
                                             % (field, obj_type))


    # quote string?  alias choices ought to never have special characters
    result = utif.add_delim(objects_starting_with(obj_type, prefix, field), ' ')
    completions.update(dict([[x, "%s alias selection" % pretty(obj_type)]
                              for x in result]))


def complete_config(prefix, data, completions, copy = False):
    """
    Complete selections for the 'copy' command.
    """

    configs = sdnsh.store.get_user_data_table('', "latest")

    # exclude source if its in the data
    source = data.get('source','')
    src_dst = 'source' if source == '' else 'destination'

    any = False
    any_config = False

    if copy:
        if 'running-config'.startswith(prefix):
            if source != 'running-config':
                completions['running-config '] = 'running-config %s' % src_dst

    for c in configs:
        if ('config://' + c['name']).startswith(prefix):
            if source != "config://" + c['name']:
                completions["config://" + c['name'] + ' '] = \
                    'Saved Configuration %s' % src_dst
            any_config = True

    if source != '' and 'config://'.startswith(prefix):
        completions['config://'] = 'config prefix %s' % src_dst

    if copy:
        for additions in ["http://", "file://", "ftp://", "tftp://", 'config://' ]:
            if additions.startswith(prefix):
                completions[additions] = 'other %s' % src_dst


def complete_interface_list(prefix, data, completions):
    """
    Interface lists are comma separated interfaces or range
    of interfaces.  

    The prefix here plays an important role in determining what
    ought to appear nest.
    """
    if not 'switch' in data:
        return

    def switch_interfaces_startingwith(interfaces, intf, prefix, completions):
        result = [prefix + x for x in interfaces.keys() if x.startswith(intf)]
        completions.update(dict([[x, "known interface"] for x in result]))
        return

    def higher_interfaces(interfaces, intf, prefix, completions):
        # depend on having an integer as the last component
        last_digits = re.compile(r'(.*)(\d+)$')
        match = last_digits.search(intf)
        if match: 
            if_name = match.group(1)
            first = int(match.group(2))
            for i in interfaces:
                match = last_digits.search(i)
                if match and match.group(1) == if_name and int(match.group(2)) > first:
                    completions[prefix + match.group(2)] = 'inteface choice.'

 
    ports = rest_to_model.get_model_from_url('interfaces', data)
    interfaces = dict([[x['name'], x] for x in ports])
    sic = sdnsh.get_table_from_store('switch-interface-config',
                                     'switch', data['switch'])
    interfaces.update(dict([[x['name'], x] for x in sic]))

    # peek at the last character in the prefix:
    #  if it's a dash, then choose interfaces with the same prefix,
    #  if its a comma, then chose another interface
    
    front_item = ''
    if len(prefix) > 0:
        if prefix[-1] == '-':
            # complete more choices
            previous = prefix[:-1]
            if len(previous):
                last_item = previous.split(',')[-1]
                if last_item in interfaces:
                    higher_interfaces(interfaces, last_item, prefix, completions)
            return
                
        if prefix[-1] != ',':
            if len(prefix) > 2:
                parts = prefix.split(',')
                last_item = parts[-1]
                # see if the last_item of prefix is a known interface.
                if last_item in interfaces:
                    completions[prefix + ',']     = 'List of interfaces'
                    completions[prefix + '-']     = 'Range of interfaces'
                    completions[prefix + ' <cr>'] = 'Current interfaces selection'
                    return
                # see if the last item is a range (intf in front, then a dash)
                c = [y for y in [x for x in interfaces if last_item.startswith(x)]
                                if len(last_item) > len(y) and last_item[len(y)] == '-']
                if len(c):
                    # found interface with a dash afterwards
                    # could actually check that everything after '-' is digits
                    completions[prefix + ',']     = 'List of interfaces'
                    completions[prefix + ' <cr>'] = 'Current interfaces selection'
                    return

                first_items = ''.join(['%s,' % x for x in parts[:-1]])
                switch_interfaces_startingwith(interfaces,
                                               last_item,
                                               first_items,
                                               completions)
                return

            # single token prefix
            switch_interfaces_startingwith(interfaces, prefix, '', completions)
            return
        # last character is a comma
        if len(prefix) == 1:
            return # just a comma

        # crack into parts, see if the last is a range, if so, then
        # the choices are a comma or a <cr>
        parts = prefix.split(',')
        front_item = ','.join(parts[:-1]) + ','
        prefix = parts[-1]
        # fall through

    switch_interfaces_startingwith(interfaces, prefix, front_item, completions)
    return


def complete_staticflow_actions(prefix, data, completions):
    # peek at the last character in the prefix:
    #  if it's a comma, then choose all the possible actions
    #  if its a equal, then display the choices for this option
    
    prefix_parts = []

    actions = {
        'output='            : 'Describe packet forwarding',
        'enqueue='           : 'Enqueue packet',
        'strip-vlan='        : 'Strip Vlan',
        'set-vlan-id='       : 'Set Vlan',
        'set-vlan-priority=' : 'Set Priority',
        'set-src-mac='       : 'Set Src Mac',
        'set-dst-mac='       : 'Set Dst Mac',
        'set-tos-bits='      : 'Set TOS Bits',
        'set-src-ip='        : 'Set IP Src',
        'set-dst-ip='        : 'Set IP Dst',
        'set-src-port='      : 'Set Src IP Port',
        'set-dst-port='      : 'Set dst IP Port',
    }

    action_choices = {
        ('output=', 'all')          : 'Forward to all ports',
        ('output=', 'controller')   : 'Forward to controller',
        ('output=', 'local')        : 'Forward to local',
        ('output=', 'ingress-port') : 'Forward to ingress port',
        ('output=', 'normal')       : 'Forward to ingress port',
        ('output=', 'flood')        : 'Forward, flood ports',
        ('output=', ('<number>', '<number>'))  : 'Forward, to a specific port',

        ('enqueue=', ('<portNumber>.<queueID>', '<portNumber>.<queueID>')) : 'Enqueue to port, queue id',

        ('set-vlan-id=',('<vlan number>','<vlan number>')) : 'Set vlan to <vlan number>',
        
        ('set-vlan-priority=',('<vlan prio>','<vlan prio>')) : 'Set vlan priority to <prio>',

        ('set-tos-bits=',('<number>',)) : 'Set TOS bits',
        ('set-src-mac=',('<src-mac-address>',)) : 'Set src mac address',

        ('set-dst-mac=',('<dst-mac-address>',)) : 'Set dst mac address',
        
        ('set-src-ip=',('<src-ip-address>',)) : 'Set src mac address',
        
        ('set-dst-ip=',('<src-ip-address>',)) : 'Set dst ip address',
    }

    for ps in prefix.split(','):
        ps_parts = ps.split('=')
        if len(ps_parts) == 1 and ps_parts[0] != '':
            # possibly incomplete item before the '='
            for choice in [x for x in actions.keys() if x.startswith(ps_parts[0])]:
                completions[choice] = actions[choice]
            return
        elif len(ps_parts) == 2:
            if len(ps_parts[0]) and len(ps_parts[1]):
                prefix_parts.append((ps_parts[0], ps_parts[1]))
            elif len(ps_parts[0]) and len(ps_parts[1]) == 0:
                prefix_parts.append((ps_parts[0], ))

    if prefix == '' or prefix.endswith(','):
        completions.update(actions)
    elif prefix.endswith('='):
        last = prefix_parts[-1]
        for ((match, next), desc) in action_choices.items():
            if match[:-1] != last[0]:
                continue
            if type(next) == str:
                completions[match + next] = desc
            elif type(next) == tuple:
                completions[(match + next[0], match + next[0])] = desc
            # else?  display error?
    elif len(prefix_parts):
        last = prefix_parts[-1]
        if len(last) == 1:
            pass
        elif len(last) == 2:
            # try to find the left item
            for ((match, next), desc) in action_choices.items():
                if match[:-1] != last[0]:
                    continue
                if type(next) == str and next == last[1]:
                    eol = prefix + ' <cr>'
                    completions[(eol, eol)] = 'Complete Choice'
                    another = prefix + ','
                    completions[(another, another)] = 'Add another action'
                elif type(next) == str and next.startswith(last[1]):
                    base_part = ''.join(prefix.rpartition(',')[:-1])
                    completions[base_part + last[0] + '=' + next] = 'Complete selection'
                elif len(last[1]):
                    # hard to say what choices can be added here,
                    # there are some characters after '=', but none
                    # which match some prefix.
                    pass

                # how to match the values?


def complete_description_versions(prefix, completions):
    for element in os.listdir(sdnsh.command_packages_path()):
        if element == '__init__.py':
            pass
        elif element.startswith('version'):
            # len('element') -> 7
            version = "%2.2f" % (float(element[7:]) / 100)
            if version[-2:] == '00':
                version = version[:2] + '0'
            if version.startswith(prefix):
                completions[version] = 'VERSION'
            if version == '2.0':    # currently if 2.0 exists, so does 1.0
                if '1.0'.startswith(prefix):
                    completions['1.0'] = 'VERSION'
        else:
            if element.startswith(prefix):
                completions[element] = 'VERSION'


def complete_log_names(prefix, data, completions):
    """
    Enumerate all the log file choices based on replies from the REST API.
    """
    controller = data.get('controller')
    for ip_port in controller_ip_and_port(controller):
        url = log_url(ip_and_port = ip_port)
        log_names = command.sdnsh.rest_simple_request_to_dict(url)
        for log in log_names:
            log_name = log['log']
            if log_name.startswith(prefix):
                completions[log_name + ' '] = 'Log Selection'



def init_completions(bs, modi):
    global sdnsh, mi
    sdnsh = bs
    mi = modi

    command.add_completion('complete-object-field', complete_object_field,
                           {'kwargs': {'obj_type'     : '$obj-type',
                                       'parent_field' : '$parent-field',
                                       'parent_id'    : '$current-mode-obj-id',
                                       'field'        : '$field',
                                       'prefix'       : '$text',
                                       'data'         : '$data',
                                       'scoped'       : '$scoped',
                                       'other'        : '$other',
                                       'mode'         : '$mode',
                                       'completions'  : '$completions'}})

    command.add_completion('complete-tag-mapping', complete_tag_mapping,
                           {'kwargs': {'obj_type'     : '$obj-type',
                                       'parent_field' : '$parent-field',
                                       'parent_id'    : '$current-mode-obj-id',
                                       'field'        : '$field',
                                       'prefix'       : '$text',
                                       'data'         : '$data',
                                       'scoped'       : '$scoped',
                                       'other'        : '$other',
                                       'mode'         : '$mode',
                                       'completions'  : '$completions'}})

    command.add_completion('complete-from-another', complete_from_another,
                           {'kwargs': {'other'        : '$other',
                                       'obj_type'     : '$obj-type',
                                       'parent_field' : '$parent-field',
                                       'parent_id'    : '$current-mode-obj-id',
                                       'field'        : '$field',
                                       'prefix'       : '$text',
                                       'data'         : '$data',
                                       'scoped'       : '$scoped',
                                       'completions'  : '$completions',
                                       'no_command'   : '$is-no-command',
                                       'explicit'     : '$explicit', }})

    command.add_completion('complete-alias-choice', complete_alias_choice,
                           {'kwargs': {'obj_type'    : '$obj-type',
                                       'field'       : '$field',
                                       'other'       : '$other',
                                       'prefix'      : '$text',
                                       'data'        : '$data',
                                       'scoped'      : '$scoped',
                                       'completions' : '$completions',
                                       'no_command'  : '$is-no-command', }})

    command.add_completion('complete-config', complete_config,
                           {'kwargs': {'prefix': '$text',
                                       'data': '$data',
                                       'completions': '$completions'}})

    command.add_completion('complete-config-copy', complete_config,
                           {'kwargs': {'prefix': '$text',
                                       'data': '$data',
                                       'completions': '$completions',
                                       'copy' : True }})

    command.add_completion('complete-interface-list', complete_interface_list,
                           {'kwargs': {'prefix': '$text',
                                       'data': '$data',
                                       'completions': '$completions'}})

    command.add_completion('complete-staticflow-actions', complete_staticflow_actions,
                           {'kwargs': {'prefix': '$text',
                                       'data': '$data',
                                       'completions': '$completions'}})

    command.add_completion('description-versions', complete_description_versions,
                           {'kwargs': {'prefix': '$text',
                                       'completions': '$completions'}})

    command.add_completion('complete-log-names', complete_log_names,
                           {'kwargs': {'prefix'     : '$text',
                                       'data'       : '$data',
                                       'completions': '$completions'}})

