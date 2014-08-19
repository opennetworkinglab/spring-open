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

#
# DATA HANDLERS
#

import re
import modi
import utif
import error
import command
import time
import datetime
import rest_to_model
import traceback

from midw import *

COMMAND_CIDR_RE = re.compile(r'^((\d{1,3}\.){3}\d{1,3})/(\d{1,2}?)$')


def split_cidr_data_handler(value, data,
                            dest_ip='ip', dest_netmask='netmask', neg = False):
    """
    Split a cidr address (e.g. 192.168.1.1/24) into separate IP address
    and netmask value. The names of the ip and netmask fields are
    specified (typically directly in the same block/dictionary where
    the argument data handler is specifed) with a 'dest-ip' and a
    'dest-netmask' values.
    """
    global sdnsh

    m = COMMAND_CIDR_RE.match(value)
    if m:
        bits = int(m.group(3))
        if bits > 32:
            raise error.ArgumentValidationError("max cidr block is 32")

        data[dest_ip] = m.group(1)
        if neg:
            data[dest_netmask] = utif.inet_ntoa(~(0xffffffff << (32 - bits)))
        else:
            data[dest_netmask] = utif.inet_ntoa((0xffffffff << (32 - bits)))


def alias_to_value_handler(value, obj_type, data, field, other = None):
    """
    Compute the alias value for the named field for the obj_type.
    Place the resulting converted field into the data dictionary.

    Since this is a data-handler, the data dict must be updated
    even if this isn't an alias, otherwise the field value is lost.
    """
    global sdnsh
    if sdnsh.description:
        print 'alias_to_value_handler: ', value, obj_type, data, field

    if field != mi.pk(obj_type):
        # if this is a forgeign key, use the obj_type of the fk.
        if mi.is_foreign_key(obj_type, field):
            (obj_type, fk_name) = mi.foreign_key_references(obj_type, field)
        else:
            # XXX possibly other choices to determine alias_obj_type?
            if sdnsh.description:
                print 'alias_to_value_handler: field %s no obj-type ref %s ' % \
                      (field, obj_type)
        
    if other:
        parts = other.split('|')
        key = mi.pk(parts[0]) # parts[0] <- first part of other
        if len(parts) > 0:
            other = parts[0]
            key = field
            # not clear whether the part[1] is useful, two parts used
            # in other functions, example complete-from-another()
        other = mi.obj_type_related_config_obj_type(other)
        converted_value = convert_alias_to_object_key(other, value)

        pk = mi.pk(other)
        if mi.is_compound_key(other, pk) and converted_value != value:
            pk_dict = { pk : converted_value }
            mi.split_compound_into_dict(other, pk, pk_dict, is_prefix = True)
            for (k,v) in pk_dict.items():
                if k != pk:
                    data[k] = v
                    if sdnsh.description:
                        print "alias_to_value_handler: compound (other) %s:%s <- %s:%s" % \
                               (k, data[k], other, converted_value)
        else:
            case = mi.get_obj_type_field_case_sensitive(other, field)
            data[field] = utif.convert_case(case, converted_value)

        if sdnsh.description:
            print "alias_to_value_handler: (other) %s:%s <- %s:%s" % \
                   (key, data[key], other, value)
    else:
        # Some obj_types, for example, host, have no cassandra data,
        # but do have a related obj_type which is in the store
        obj_type = mi.obj_type_related_config_obj_type(obj_type)
        converted_value = convert_alias_to_object_key(obj_type, value)

        pk = mi.pk(obj_type)
        if mi.is_compound_key(obj_type, pk) and converted_value != value:
            pk_dict = { pk : converted_value }
            split_obj_type = other if other != None else obj_type 
            mi.split_compound_into_dict(split_obj_type, pk, pk_dict, is_prefix = True)
            for (k,v) in pk_dict.items():
                if k != pk:
                    data[k] = v
                    if sdnsh.description:
                        print "alias_to_value_handler: compound %s:%s <- %s:%s" % \
                               (k, data[k], obj_type, converted_value)
        else:
            case = mi.get_obj_type_field_case_sensitive(obj_type, field)
            data[field] = utif.convert_case(case, converted_value)

            if sdnsh.description:
                print "alias_to_value_handler:  %s:%s <- %s:%s" % (field,
                                                               data[field], obj_type, value)


def replace_value_handler(value, obj_type, data, field, other = None):
    """
    Use the other field when its present to find a related obj_type,
    look for the field in that structure to populate data.
    """
    global sdnsh

    table = obj_type

    if sdnsh.description:
        print "replace_value_handler: obj_type: %s value: %s data: %s field %s other %s" % \
                (obj_type, value, data, field, other)
    fields = [field]
    if other:
        parts = other.split('|')
        table = parts[0]
        fields = parts[1:]

    try:
        row = sdnsh.get_object_from_store(table, value)
    except Exception, e:
        raise error.ArgumentValidationError("Unknown value %s (%s)" % 
                                              (value, obj_type))
    for field in fields:
        if field not in row:
            raise error.ArgumentValidationError("Unknown field %s (%s)" % 
                                                (field, obj_type))
        if sdnsh.description:
            print 'replace_value_handler: set %s <- %s from obj-type %s' %\
                  (field, row[field], table)
        data[field] = row[field]


def enable_disable_to_boolean_handler(value, data, field):
    if value == 'enable':
        data[field] = True
    if value == 'disable':
        data[field] = False


def date_to_integer_handler(value, data, field):
    if (value == 'now' or value == 'current'):
        data[field] = int(time.time()*1000)

    try:
        data[field] = int(value)
    except:
        pass

    for f,pre in [('%Y-%m-%dT%H:%M:%S', None),
                  ('%Y-%m-%d %H:%M:%S', None),
                  ('%Y-%m-%dT%H:%M:%S%z', None),
                  ('%Y-%m-%d %H:%M:%S%z', None),
                  ('%Y-%m-%d', None),
                  ('%m-%d', '%Y-'),
                  ('%H:%M', '%Y-%m-%dT')]:
        try:
            t = value
            if pre:
                pref = datetime.datetime.now().strftime(pre)
                f = pre + f
                t = pref + t

            thetime = datetime.datetime.strptime(t, f)
            data[field] = int(time.mktime(thetime.timetuple())*1000)
        except:
            pass


HEX_RE = re.compile(r'^0x[0-9a-fA-F]+$')

def hex_to_integer_handler(value, data, field):
    if HEX_RE.match(str(value)):
        _value = str(int(value, 16))
    else:
        _value = str(int(value))
    data[field] = _value


def _invert_netmask(value):
    split_bytes = value.split('.')
    return "%s.%s.%s.%s" % (255-int(split_bytes[0]),
                            255-int(split_bytes[1]),
                            255-int(split_bytes[2]),
                            255-int(split_bytes[3]))


def convert_inverse_netmask_handler(value, data, field):
    data[field] = _invert_netmask(value)


def interface_ranges(names):
    """
    Given a list of interfaces (strings), in any order, with a numeric suffix,
    collect together the prefix components, and create ranges with
    adjacent numeric interfaces, so that a large collection of names
    becomes easier to read.  At the worst, the list will be as
    complicated as the original (which would typically be very unlikely)

    Example: names <- ['Eth0', 'Eth1', 'Eth2',  'Eth4', 'Eth5', 'Eth8']
             result <- ['Eth0-2', 'Eth4-5', 'Eth8']

             names <- ['1','2','3']
             result <- ['1-3']

    """
    # collect the interfaces into dictionaries based on prefixes
    # ahead of groups of digits.
    groups = {}

    def is_digit(c):
        c_ord = ord(c)
        if c_ord >= ord('0') and c_ord <= ord('9'):
            return True
        return False

    for name in names:
        if is_digit(name[-1]):
            for index in range(-2, -len(name)-1, -1):
                if not is_digit(name[index]):
                    index += 1
                    break;
            else:
                index = -len(name)

            prefix = name[:index]
            number = int(name[index:])
            if not prefix in groups:
                groups[prefix] = []
            groups[prefix].append(number)
        else:
            groups[name] = []

    for prefix in groups:
        groups[prefix] = sorted(utif.unique_list_from_list(groups[prefix]))
    
    ranges = []
    for (prefix, value) in groups.items():
        if len(value) == 0:
            ranges.append(prefix)
        else:
            low = value[0]
            prev = low
            for next in value[1:] + [value[-1] + 2]: # +[] flushes last item
                if next > prev + 1:
                    if prev == low:
                        ranges.append('%s%s' % (prefix, low))
                    else:
                        ranges.append('%s%s-%s' % (prefix, low, prev))
                    low = next
                prev = next
 
    return ranges


#print interface_ranges(['1','2','3', 'oe'])
#print interface_ranges(['Eth1','Eth2','Eth3', 'Eth4', 'o5', 'o6'])


def check_missing_interface(switch, interface):
    #
    # The switch value could be a compound key reference to a
    # switch, if there's a '|' in the switch valud, try to guess
    # which entry is the switch

    parts = switch.split('|')
    if len(parts) > 1:
        for part in parts:
            if utif.COMMAND_DPID_RE.match(part):
                switch = part
                break
        else:
            switch = part[0]

    try:
        row = rest_to_model.get_model_from_url('switches', {'dpid' : switch })
    except:
        if sdnsh.description or sdnsh.debug_backtrace:
            traceback.print_exc()
        row = []

    if len(row) == 0 or row[0].get('ip-address', '') == '':
        sdnsh.warning('switch %s currently not active, '
                      'interface %s may not exist' %
                      (switch, interface))
        return

    try:
        ports = rest_to_model.get_model_from_url('interfaces', {'dpid' : switch })
    except:
        # can't validate current list of interface names
        return

    if_names = [x['portName'].lower() for x in ports]
    if not interface.lower() in if_names:
        # pre-servce case, try to identify unique ranges
        ranges = interface_ranges([x['portName'] for x in ports])

        sdnsh.warning( 'active switch has no interface "%s", ' 
                  'known: %s' % (interface, ', '.join(ranges)) + 
                  '\nUse \"exit; no interface %s\" to remove' % interface)


def warn_missing_interface(value, data, field, is_no, obj_type, obj_value ):
    if not is_no:
        # need switch, if_name 
        pk_data = { mi.pk(obj_type) : obj_value }
        mi.split_compound_into_dict(obj_type, mi.pk(obj_type), pk_data, True)
        switch = pk_data.get('switch')
        if switch == None:
            switch = pk_data.get('dpid')
        if switch == None:
            switch = data.get('switch')
        if switch == None:
            switch = data.get('dpid')
        if switch == None:
            raise error.ArgumentValidationError("Can't identify switch for validation")
        force = True if data.get('force', '') != '' else False
        check_missing_interface(switch, value)
    data[field] = value

def convert_interface_to_port(value, data, field, other = None, scoped = None):
    # look for the switch name in data
    if scoped:
        dpid = data.get(scoped)
    elif 'dpid' in data:
        dpid = data['dpid']
    else:
        dpid = data.get('switch', '') # possibly other choices

    # if its not a specific switch, no conversion is possible
    # should the value be passed through?
    if dpid == '':
        data[field] = value
        return

    ports = rest_to_model.get_model_from_url('interfaces', {'dpid' : dpid})
    for port in ports:
        if port['portName'] == value:
            data[field] = port['portNumber'] # should this be a string?
            break
    else:
        raise error.ArgumentValidationError("Can't find port %s on switch %s" %
                (value, dpid))


def convert_tag_to_parts(value, data, namespace_key, name_key, value_key):
    """
    Split a tag of the form [ns].name=value into the three
    component parts
    """

    if sdnsh.description:
        print "convert_tag_to_parts: %s %s %s %s %s" % (
                value, data, namespace_key, name_key, value_key)

    tag_and_value = value.split('=')
    if len(tag_and_value) != 2:
        raise error.ArgumentValidationError("tag <[tag-namespace.]name>=<value>")

    tag_parts = tag_and_value[0].split('.')
    if len(tag_parts) == 1:
        tag_namespace = "default"
        tag_name = tag_parts[0]
    elif len(tag_parts) >= 2:
        tag_namespace = '.'.join(tag_parts[:-1])
        tag_name = tag_parts[-1]

    # should the names have some specific validation?
    data[namespace_key] = tag_namespace
    data[name_key]      = tag_name
    data[value_key]     = tag_and_value[1]


def init_data_handlers(bs, modi):
    global sdnsh, mi
    sdnsh = bs
    mi = modi

    command.add_argument_data_handler('split-cidr-data', split_cidr_data_handler,
                        {'kwargs': {'value': '$value',
                                    'data': '$data',
                                    'dest_ip': '$dest-ip',
                                    'dest_netmask': '$dest-netmask'}})

    command.add_argument_data_handler('split-cidr-data-inverse', split_cidr_data_handler,
                        {'kwargs': {'value': '$value',
                                    'data': '$data',
                                    'dest_ip': '$dest-ip',
                                    'dest_netmask': '$dest-netmask',
                                    'neg' : True}})

    command.add_argument_data_handler('alias-to-value', alias_to_value_handler,
                        {'kwargs': {'value': '$value',
                                    'data': '$data',
                                    'field': '$field',
                                    'other': '$other',
                                    'obj_type' : '$obj-type'}})

    command.add_argument_data_handler('replace-value', replace_value_handler,
                        {'kwargs': {'value': '$value',
                                    'data': '$data',
                                    'field': '$field',
                                    'other': '$other',
                                    'obj_type' : '$obj-type'}})

    command.add_argument_data_handler('enable-disable-to-boolean', enable_disable_to_boolean_handler,
                        {'kwargs': {'value': '$value',
                                    'data': '$data',
                                    'field': '$field'}})

    command.add_argument_data_handler('date-to-integer', date_to_integer_handler,
                        {'kwargs': {'value' : '$value',
                                    'data'  : '$data',
                                    'field' : '$field'}})

    command.add_argument_data_handler('hex-to-integer', hex_to_integer_handler,
                        {'kwargs': {'value' : '$value',
                                    'data'  : '$data',
                                    'field' : '$field'}})

    command.add_argument_data_handler('convert-inverse-netmask', convert_inverse_netmask_handler,
                        {'kwargs': {'value' : '$value',
                                    'data'  : '$data',
                                    'field' : '$field'}})

    command.add_argument_data_handler('warn-missing-interface', warn_missing_interface,
                        {'kwargs': {'value'     : '$value',
                                    'data'      : '$data',
                                    'field'     : '$field',
                                    'is_no'     : '$is-no-command',
                                    'obj_type'  : '$current-mode-obj-type',
                                    'obj_value' : '$current-mode-obj-id'}})

    command.add_argument_data_handler('convert-interface-to-port', convert_interface_to_port,
                        {'kwargs': {'value'     : '$value',
                                    'data'      : '$data',
                                    'field'     : '$field',
                                    'other'     : '$other',
                                    'scoped'    : '$scoped'}})

    command.add_argument_data_handler('convert-tag-to-parts', convert_tag_to_parts,
                        {'kwargs': {'value'          : '$value',
                                    'data'           : '$data',
                                    'namespace_key'  : '$namespace-key',
                                    'name_key'       : '$name-key',
                                    'value_key'      : '$value-key'}})
