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

import error
import command
import collections
import numbers
import re
import traceback
import utif
import rest_to_model
import datetime
import socket

from midw import *

def validate_string(typedef, value):
    """
    Validate a string, check length.
    """

    # Check to see if the typedef has any pattern restrictions. If it does,
    # then match against all of the patterns. If it matches any
    # of the patterns (i.e. not all of the patterns), then the argument
    # value is considered to be valid.
    patterns = typedef.get('pattern')
    if patterns:
        # If it's a single pattern convert it to a tuple, so we can handle
        # it in a common way below. Note that we don't use collection.Sequence
        # in the isinstance call because strings are sequences
        if command._is_string(patterns):
            patterns = (patterns,)

        for pattern in patterns:
            if re.match(pattern, str(value)):
                break
        else:
            command._raise_argument_validation_exception(typedef, value,
                                                        'invalid string pattern')

    # Check for any length restrictions. This can be a single scalar length or
    # a single length range or a sequence of either of those. For scalar lengths
    # the length must match it exactly. For length ranges the length of the argument
    # must be between the two values (inclusive). For a list of length specs, the
    # length must match any of the lengths (either scalar or range) in the list.
    lengths = typedef.get('length')
    if lengths:
        if command._is_single_range(lengths):
            lengths = (lengths,)
        for length in lengths:
            command._check_one_range(length)
            if isinstance(length, numbers.Integral):
                if len(value) == length:
                    break
            if len(value) >= length[0] and len(value) <= length[1]:
                break
        else:
            command._raise_argument_validation_exception(typedef, value,
                                                        'invalid string length')

    return value

def validate_integer_range(typedef, value, ranges):
    # Check for any range restrictions. This can be a single scalar value or
    # a single range or a list/tuple of a combination of those. For scalar ranges
    # the arg must match it exactly. For ranges the value of the argument
    # must be between the two values (inclusive). For a list of range specs, the
    # value must match any of the lengths (either scalar or range) in the list.
    if ranges:
        if command._is_single_range(ranges):
            ranges = (ranges,)
        for r in ranges:
            command._check_one_range(r)
            if isinstance(r, numbers.Integral):
                if value == r:
                    break
            else:
                lower_boundary = command._convert_range_boundary(r[0], value)
                upper_boundary = command._convert_range_boundary(r[1], value)
                if value >= lower_boundary and value <= upper_boundary:
                    break
        else:
            command._raise_argument_validation_exception(typedef, value,
                                                        'value is outside specified '
                                                        'range: (%d-%d)' % (r[0], r[1]))

def validate_integer(typedef, value):
    # Check that the value is actually an integer
    try:
        value = int(value)
    except (ValueError, TypeError):
        command._raise_argument_validation_exception(typedef, value,
                                                    'value is not an integer')
    validate_integer_range(typedef, value, typedef.get('range'))

    return value

HEX_RE = re.compile(r'^0x[0-9a-fA-F]+$')

def validate_hex_or_dec_integer(typedef, value):
    # Check that the value is actually an hex or decimal integer
    if HEX_RE.match(value):
        # if it matches, it ought to succeed conversion, below's really not needed
        try:
            _value = int(value, 16)
        except (ValueError, TypeError):
            command._raise_argument_validation_exception(typedef, value,
                                                        'value is not a hex integer')
    else:
        try:
            _value = int(value)
        except (ValueError, TypeError):
            command._raise_argument_validation_exception(typedef, value,
                                                        'value is not an integer')
    validate_integer_range(typedef, _value, typedef.get('range'))

    return value


IP_ADDR_RE = re.compile(r'^(\d{1,3}\.){3}\d{1,3}$')

def is_netmask(value):
    """
    return True or False, True for a dotted-quad which can
    be interpreted as a netmask, False otherwise
    """
    if not IP_ADDR_RE.match(value):
        return False

    # convert the netmask string to an integer
    split_bytes = value.split('.')
    netmask_int = 0
    for b in split_bytes:
        netmask_int = (netmask_int << 8) + int(b)

    # Check that the value is composed of leading 1 bits followed by
    # trailing 0 bits.
    valid_netmasks = [0xffffffff - ((1 << i) - 1) for i in range(0,33)]
    if netmask_int in valid_netmasks:
        return True
    return False

def validate_netmask(typedef, value):
    if not is_netmask(value):
        command._raise_argument_validation_exception(typedef, value,
                                                    'invalid netmask')

    return value

def is_inverse_netmask(value):
    """
    return True or False, True for a dotted-quad which
    can be interpreted as inverse netmasks (like 0.0.0.255),
    False otherwise. The full mask of (255.255.255.255) is not
    treated as inverse_netmask, but a valid netmask. This helps
    disambiguate inverse_netmask from regular netmask
    """
    if not IP_ADDR_RE.match(value):
        return False

    # First convert the netmask string to an integer
    split_bytes = value.split('.')
    netmask_int = 0
    for b in split_bytes:
        netmask_int = (netmask_int << 8) + int(b)

    # Check that the value is composed of leading 0 bits followed by
    # trailing 1 bits. To disambiguate inverse from regular netmask,
    # we mandate a leading zero bit in the netmask
    valid_netmasks = [((1 << i) - 1) for i in range(0,32)]
    if netmask_int in valid_netmasks:
        return True
    return False

def validate_inverse_netmask(typedef, value):
    """
    These look like 0.0.0.255, and are intended to only match
    when the there is one group of 1's and one group of zero's.
    """
    if is_inverse_netmask(value):
        return value
        
    command._raise_argument_validation_exception(typedef, value,
                                                'invalid netmask')

def validate_ip_address_not_netmask(typedef, value):
    """
    For ip addresses associated with interfaces, the configured value
    must not be one of the many masks.
    """
    # if its not an ip address, then its not a netmask, requiring
    # a test for ip address match.
    if not IP_ADDR_RE.match(value):
        command._raise_argument_validation_exception(typedef, value,
                                                    'not an ip-address')

    # must be four dotted quad's, validate each is within 2^8 range
    if len([x for x in value.split('.') if int(x) < 256]) != 4:
        command._raise_argument_validation_exception(typedef, value,
                                                    'not an ip-address')

    if (not is_inverse_netmask(value)) and (not is_netmask(value)):
        return value

    command._raise_argument_validation_exception(typedef, value,
                                                'must not be a mask')


CIDR_RE = re.compile(r'^((\d{1,3}\.){3}\d{1,3})/(\d{1,2}?)$')

def validate_cidr_range(typedef, value):
    match = CIDR_RE.match(value)
    if not match:
        command._raise_argument_validation_exception(typedef, value,
                                                    'not cidr syntax: ip/n')
    if int(match.group(3)) > 32:
        command._raise_argument_validation_exception(typedef, value,
                                                    'cidr range above 32')

    if len([x for x in match.group(1).split('.') if int(x) < 256]) != 4:
        command._raise_argument_validation_exception(typedef, value,
                                                    'cidr range above 32')

    return value


def validate_resolvable_ip_address(typedef, value):
    # see if this is an object we know about, for exameple a switch.
    dpid = convert_alias_to_object_key('switch-config', value)
    try:
        validate_dpid(typedef, dpid)
    except:

        try:
            socket.gethostbyname(value)
        except:
            msg = 'unresolvable name'
            command._raise_argument_validation_exception(typedef, value,
                                                         msg)
                                        


def validate_identifier(typedef, value, reserved = None):
    """
    When identifier is used as a 'type' in the description, it will
    validate against the sdnsh reserved_words.

    When identifier is used as a 'base-type' in the description, it
    will also use any 'reserved' attributes in the description.

    This allows some control over whether the 'reserved' attribute
    takes effect at the syntax-layer or at the action-layer
    """
    if not re.match(r'^[a-zA-Z0-9_-]+$', str(value)):
        msg = 'Invalid characters in identifier'
        command._raise_argument_validation_exception(typedef, value, msg)
    if value in sdnsh.reserved_words:
        msg = 'reserved word "%s" in "%s"' % (value, ', '.join(sdnsh.reserved_words))
        command._raise_argument_validation_exception(typedef, value, msg)
    if reserved and value in reserved:
        msg = 'reserved word "%s" in "%s"' % (value, ', '.join(reserved))
        command._raise_argument_validation_exception(typedef, value, msg)
    return value 


def validate_date(typedef, value):
    if (value == 'now' or value == 'current'):
        return value

    try:
        return str(int(value))
    except (ValueError, TypeError):
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

            return value
        except:
            pass

    command._raise_argument_validation_exception(typedef, value, 'invalid date')


def validate_duration(typedef, value):
    for s in ['weeks', 'days', 'hours', 'mins', 'secs', 'ms']:
        if value.endswith(s):
            return value
    command._raise_argument_validation_exception(typedef, value,
                                                'invalid duration')
  

def validate_enum(typedef, value):
    # FIXME: Sort of a hack. The base enum class doesn't have a values
    # field, so there's nothing to check against. We really just use it
    # as a base type to indicate the validation function (i.e. this function)
    # to use.
    name = typedef.get('name')
    if name == 'enum':
        return

    enum_values = typedef.get('values')
    if not enum_values:
        raise error.CommandDescriptionError('Unspecified enum values')

    if not isinstance(enum_values, collections.Mapping):
        # If it's not a dictionary then it should be an array
        # or tuple where the (string) elements are both the key
        # and the value for the enum items. So here we convert
        # to a dictionary so we can use the same logic for both
        # cases below.
        if command._is_string(enum_values):
            enum_values = (enum_values,)
        if isinstance(enum_values, collections.Sequence):
            enum_values = dict((v,v) for v in enum_values)
        else:
            raise error.CommandDescriptionError(
                    'Enum values must be either a string, dict, tuple, or list')

    prefix_matches = []
    lower_value = value.lower()
    for enum_value, return_value in enum_values.items():
        lower_enum_value = enum_value.lower()
        if lower_enum_value == lower_value:
            return (return_value, enum_value)
        if lower_enum_value.startswith(lower_value):
            prefix_matches.append((return_value, enum_value))

    if len(prefix_matches) == 0:
        command._raise_argument_validation_exception(typedef, value,
                'unexpected value for enum', enum_values.keys())

    return prefix_matches


COMMAND_MAC_ADDRESS_RE = re.compile(r'^(([A-Fa-f\d]){2}:?){5}[A-Fa-f\d]{2}$')
COMMAND_MAC_ALT_RE = re.compile(r'^(([A-Fa-f\d]){4}\.?){2}[A-Fa-f\d]{4}$')

def _is_mac_address(value):
    if COMMAND_MAC_ALT_RE.match(value):
        #
        # Convert 1234.5678.9012 into 01:34:56:78:90:12
        _mac = '%s:%s:%s:%s:%s:%s' % (
                    value[0:2],   value[2:4],
                    value[5:7],   value[7:9],
                    value[10:12], value[12:14])
        return True
    elif COMMAND_MAC_ADDRESS_RE.match(value):
        return True
    return False

def validate_mac_address(typedef, value):
    if _is_mac_address(value):
        return value
    command._raise_argument_validation_exception(typedef, value, 'mac address')

def validate_host(typedef, value):
    """
    """

    if _is_mac_address(value):
        return value

    pk = mi.pk('host-config')
    key = convert_alias_to_object_key('host-config', value)
    key_dict = { pk : key }
    mi.split_compound_into_dict('host-config', pk, key_dict, is_prefix = True)
    if 'mac' in key_dict and _is_mac_address(key_dict['mac']):
        try:
            _exists = rest_to_model.get_model_from_url('host', key_dict)
            return value
        except:
            raise error.ArgumentValidationError(
                          'host "%s": doesn\'t exist' % value)

    command._raise_argument_validation_exception(typedef, value,
                                'not host alias nor mac address')

def validate_dpid(typedef, value):
    if not utif.COMMAND_DPID_RE.match(value):
        command._raise_argument_validation_exception(typedef, value, 'switch dpid')
    return value


def validate_switch_dpid(typedef, value):
    """
    Either the value must be a syntactic dpid (eight-hex-bytes),
    Or the value must be a switch alias, and the underlying dpid
    must exist.
    """

    try:
        validate_dpid(typedef, value)
        return value
    except error.ArgumentValidationError, e:
        dpid = convert_alias_to_object_key('switch-config', value)
        try:
            validate_dpid(typedef, dpid)
            try:
                _exists = sdnsh.get_object_from_store('switch-config', dpid)
                return value
            except e:
                command._raise_argument_validation_exception(typedef, value,
                                            'switch "%s" doesn\'t exist' % value)
        except error.ArgumentValidationError, e:
            command._raise_argument_validation_exception(typedef, value,
                                        'not switch alias nor dpid')

def validate_existing_obj(typedef, value, obj_type):
    """
    Lookup the specific obj_type's primary key value for validation.
    """

    lookup_value = convert_alias_to_object_key(obj_type, value)

    try:
        sdnsh.get_object_from_store(obj_type, lookup_value)
        return value
    except Exception, _e:
        command._raise_argument_validation_exception(typedef, value, "doesn't exist ")


def validate_config(typedef, value):
    if value.startswith('config://'):
        return value
    elif utif.full_word_from_choices(value, ['running-config', 
                                             'upgrade-config',
                                             'trash' ]):
        return value
    else:
        for prefixes in ['http://', 'ftp://', 'tftp://', 'file://']:
            if value.startswith(prefixes):
                return value
        data = sdnsh.store.get_user_data_table(value, 'latest')
        if len(data):
            return value
        
    msg = 'not a valid copy, must be (running-config, upgrade-config ' \
          'or must start with config://, http://, ftp://, or tftp://'
    command._raise_argument_validation_exception(typedef, value, msg)


def init_validations(bs, modi):
    global sdnsh, mi
    sdnsh = bs
    mi = modi

    # Initialize validation functions
    command.add_validation('validate-string', validate_string)
    command.add_validation('validate-integer', validate_integer)
    command.add_validation('validate-hex-or-dec-integer', validate_hex_or_dec_integer)
    command.add_validation('validate-netmask', validate_netmask)
    command.add_validation('validate-inverse-netmask', validate_inverse_netmask)
    command.add_validation('validate-ip-address-not-mask', validate_ip_address_not_netmask)
    command.add_validation('validate-cidr-range', validate_cidr_range)
    command.add_validation('validate-resolvable-ip-address',
                            validate_resolvable_ip_address)
    command.add_validation('validate-identifier', validate_identifier,
                           {'kwargs': {'typedef'   : '$typedef',
                                       'value'    : '$value',
                                       'reserved' : '$reserved'}})
    command.add_validation('validate-date', validate_date)
    command.add_validation('validate-duration', validate_duration)
    command.add_validation('validate-enum', validate_enum)
    command.add_validation('validate-mac-address', validate_mac_address)
    command.add_validation('validate-host', validate_host)
    command.add_validation('validate-switch-dpid', validate_switch_dpid)
    command.add_validation('validate-existing-obj', validate_existing_obj,
                        {'kwargs': {'typedef'   : '$typedef',
                                    'value'    : '$value',
                                    'obj_type' : '$obj_type'}})
    command.add_validation('validate-config', validate_config)

