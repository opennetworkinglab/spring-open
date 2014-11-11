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

#  sdnsh - The Controller Shell

import urllib
import urllib2

import base64
import struct
import time
import re
from datetime import datetime, timedelta, tzinfo
from calendar import timegm
from timesince import timesince, timesince_sec
import doctest
import traceback
import utif
import command
import json


# Timezone constants
class Utc(tzinfo):
    def utcoffset(self, dt):
        return timedelta(0)
    def tzname(self, dt):
        return "UTC"
    def dst(self, dt):
        return timedelta(0)
UTC = Utc()


class Pst(tzinfo):
    def utcoffset(self, dt):
        return timedelta(0,0,0,0,0,-8)
    def tzname(self, dt):
        return "PST"
    def dst(self, dt):
        return timedelta(0,0,0,0,0,-7)
PST = Pst()


alias_dicts = {}


def get_switches_names(object, data =None):
    """
    return the switches name (for ONOS)
    """
    switches_dpid_name ={}
    url = "http://127.0.0.1:8000/rest/v1/switches"
    result = urllib2.urlopen(url).read()
    #result = command.sdnsh.store.rest_simple_request(query_url)
    entries = result
    entries = json.loads(result)
    #eprint entries
    for switch in entries:
        #print switch
        switches_dpid_name[switch.get("dpid")] = switch.get("stringAttributes").get('name')
        #print switch.get("dpid")
        #print switch.get("stringAttributes").get('name')
    return switches_dpid_name


def update_alias_dict(obj_type, dict):
    """
    Update alias dictionaries: for switch, host, etc.
    """
    global alias_dicts
    alias_dicts[obj_type] = dict


def convert_mac_in_base64_byte_array_to_hex_string(i, data=None):
    # hmmm - tasty python
    if i == "*" or i == "": 
        return "*"
    return ":".join(["%0.2x" % x for x in struct.unpack('BBBBBB', base64.b64decode(i))])


def convert_mac_in_decimal_to_hex_string(i, data=None):
    if i == "*" or i == "":
        return "*"
    mac = hex(i)
    # some python implementations append 'L' to hex() so we need to remove it
    if mac[len(mac)-1] == 'L':
        mac = mac[:len(mac)-1]
    mac = ('0' * (14 - len(mac)) + mac[2:])
    return ':'.join([mac[x:x+2] for x in xrange(0, len(mac), 2)])


def convert_ip_in_integer_to_dotted_decimal(i, data=None):
    if i == "*" or i == "": 
        return "*"
    return '.'.join(reversed([ (str((i >> (8*x)) & 0xff)) for x in range(0,4)]))


def convert_integer_to_bitmask(i, data=None):
    if i == "*" or i == "": 
        return "*"
    return bin(i)


def convert_long_to_dpid(i, data=None):
    if i == "*" or i == "": 
        return "*"
    i = int(i)
    return ':'.join(reversed([ "%-.2x" % ((i >> (8*x)) & 0xff) for x in range(0,8)]))


def convert_signed_short_to_unsigned(i, data=None):
    try:
        i = int(i)
    except:
        return i

    if i < 0: 
        return i + 2**16
    else: 
        return i


def convert_signed_short_for_vlan(i, data=None):
    if i == -1:
        return "-"
    else:
        return convert_signed_short_to_unsigned(i, data)


def convert_to_string(i, data=None):
    if type(i) == list:
        return ', '.join(i)
    elif type(i) == dict:
        return ', '.join(["%s:%s" % x for x in i.dict()])
    else:
        return str(i)


def print_hex(i, data=None):
    if i == "" or not int(i): 
        return ""
    else: 
        return hex(i)


def timestamp_to_local_timestr(i, data=None):
    if i == '':
        return ''
    return time.strftime("%Y-%m-%d %H:%M:%S %Z", time.localtime(i/1000))


def utc_timestr_to_timestamp(s):
    return timegm(time.strptime(s, "%Y-%m-%d %H:%M:%S.%f")) * 1000


def print_from_utc_timestr(s, data=None):
    """Converts from a UTC time string like 2010-12-12 15:27:41.650000
    to time string in local timezone like 2011-06-23 02:51:41 PDT

    # Doctest only valid in Pacific Time ;-|
    >>> print_from_utc_timestr('2010-12-12 15:27:41.650000')
    '2010-12-12 07:27:41 PST'
    >>> print_from_utc_timestr('2010-06-12 15:27:41.650000')
    '2010-06-12 08:27:41 PDT'
    >>> print_from_utc_timestr('')
    ''
    >>> print_from_utc_timestr(None)
    ''
    >>> print_from_utc_timestr('Not a valid timestamp')
    ''
    """
    ret = ''
    try:
        ret = timestamp_to_local_timestr(utc_timestr_to_timestamp(s))
    except:
        pass
    return ret


def print_time_since_utc(i, data=None):
    if i == None or i == '':
        return ''
    return timesince(datetime.fromtimestamp((i/1000)))


def print_time_since_utc_timestr(s, data=None, now=None):
    """Converts from a UTC time string like 2010-12-12 15:27:41.65000
    to human readabel string like 'Last seen 4 minutes ago'

    >>> now = datetime(*(time.strptime('2010-12-12 15:27:41.650000', "%Y-%m-%d %H:%M:%S.%f")[0:6]+(0, PST)))
    >>> print_time_since_utc_timestr('2010-12-12 15:27:41.650000', now=now)
    '8 hours'
    >>> print_time_since_utc_timestr('')
    ''
    >>> print_time_since_utc_timestr(None)
    ''
    >>> print_time_since_utc_timestr('Not a valid timestamp')
    ''
    """
    ret = ''
    if s == '':
        return ''
    try:
        date_obj = datetime(*(time.strptime(s, "%Y-%m-%d %H:%M:%S.%f")[0:6] + (0, UTC)))
        ret = timesince(date_obj, now)
    except Exception, e:
        try:
            date_obj = datetime(*(time.strptime(s, "%Y-%m-%d %H:%M:%S")[0:6] + (0, UTC)))
            ret = timesince(date_obj, now)
        except Exception, e:
            try:
                date_obj = datetime(*(time.strptime(s, "%Y-%m-%dT%H:%M:%S.%fZ")[0:6] + (0, UTC)))
                ret = timesince(date_obj, now)
            except Exception, e:
                ret = "<fail>"
                pass
    return ret


def print_timesince_msec_since(i, data=None):
    if i == '':
        return i
    return timesince_sec(int(i)/1000)

def print_enum_string_and_int(values, i, not_found_val = None):
    if values and i in values:
        return "%s(%s)" % (values[i], i)
    else:
        if not_found_val:
            return not_found_val
        else:
            return i


def print_enum_string_and_hex_int(values, i, not_found_val = None):
    if values and i in values:
        return "%s(%#x)" % (values[i], int(i))
    else:
        if not_found_val:
            return not_found_val
        else:
            return i


def print_mask_enum_string_and_int(values, bits):
    if bits == "": 
        return bits
    bits_copy = bits
    enums = []
    bit_test = 1
    while bits_copy:
        if bit_test & bits_copy:
            if bit_test in values:
                enums.append(values[bit_test])
            bits_copy &= bits_copy-1
        bit_test <<= 1
    if enums:
        return ",".join(enums) + "(%s)" % (hex(bits) if bits > 9 else bits)
    else:
        if bits > 9:
            return hex(int(bits))
        return "0"


def print_physical_port(i, data=None, switch_key=None):
    global alias_dicts

    if not data:
        return str(i)
    name_dict = alias_dicts.get("portNames")
    if not name_dict:
        return str(i)
    if switch_key:
        key_string = data[switch_key] + '.' + "%d" % i
    elif 'switch' in data:
        key_string = data['switch'] + '.' + "%d" % i
    elif 'Switch' in data:
        key_string = data['Switch'] + '.' + "%d" % i
    # return the physical name if it exists, otherwise i
    return str(name_dict.get(key_string, i))


def print_byte_unit(i, data=None, suffix=None):
    """
    Convert the value of 'i' into a byte rate value,
    for example '10 GB'
    """
    try:
        value = int(i)
    except:
        if i == '':
            return 'Unknown'
        return i

    converter = (
                    (1024 * 1028 * 1024 * 1024 * 1024 , ' PB'),
                    (1024 * 1028 * 1024 * 1024 ,        ' TB'),
                    (1024 * 1028 * 1024 ,               ' GB'),
                    (1028 * 1024 ,                      ' MB'),
                    (1024 ,                             ' KB'),
                )
    if suffix == None:
        suffix = ''

    for idx, (boundary, name) in enumerate(converter):
        value = i / boundary
        if value:
            return '%s%s%s' % (value, name, suffix)
    else:
        return '%s B%s' % (i, suffix)

def print_byte_rate(i, data=None):
    return print_byte_unit(i, data, 'ps')

def print_bit_unit(i, data=None, suffix=None):
    """
    Convert the value of 'i' into a bit rate value,
    for example '10 Gb'
    """
    try:
        value = int(i)
    except:
        if i == '':
            return 'Unknown'
        return i

    converter = (
                    (1000 * 1000 * 1000 * 1000 * 1000 , ' Pb'),
                    (1000 * 1000 * 1000 * 1000 ,        ' Tb'),
                    (1000 * 1000 * 1000 ,               ' Gb'),
                    (1000 * 1000 ,                      ' Mb'),
                    (1000 ,                             ' Kb'),
                )
    if suffix == None:
        suffix = ''

    for idx, (boundary, name) in enumerate(converter):
        value = i / boundary
        if value:
            return '%s%s%s' % (value, name, suffix)
    else:
        return '%s b%s' % (i, suffix)

def print_bit_rate(i, data=None):
    return print_bit_unit(i, data, 'ps')


def decode_openflow_port(i, data=None, switch_key=None):
    if i == "*" or i == "": 
        return "*"
    # If the first character of the port name is a plus sign ('+')
    # this is intended to bypass any lookup -- its a way to indicate
    # the field is already an openflow interface.
    if isinstance(i, unicode) and  i[0] == '+':
        return i[1:]
    i = convert_signed_short_to_unsigned(i)
    i &= 0xffff
    values = { 
        0xfff8 : "input",
        0xfff9 : "table",
        0xfffa : "normal",
        0xfffb : "flood",
        0xfffc : "all",
        0xfffd : "controller",
        0xfffe : "local",
        0xffff : "none",
        }
    if i >= 0xff00:
        if i in values:
            return "%s (%s)" % (i, values[i])
        else:
            return "%s" % values[i]
    phys_port = print_physical_port(i, data, switch_key)
    if str(i) != phys_port:
        return str(i) + ' (' + phys_port + ')'
    return str(i)


def decode_openflow_port_src_switch(i, data=None):
    return decode_openflow_port(i, data, 'src-switch')


def decode_openflow_port_source_switch(i, data=None):
    return decode_openflow_port(i, data, 'Source-Switch')


def decode_openflow_port_dst_switch(i, data=None):
    return decode_openflow_port(i, data, 'dst-switch')


def decode_openflow_port_dest_switch(i, data=None):
    return decode_openflow_port(i, data, 'Dest-Switch')


def decode_openflow_port_inputSwitch(i, data=None):
    return decode_openflow_port(i, data, 'inputSwitch')


def decode_openflow_port_dpid(i, data=None):
    return decode_openflow_port(i, data, 'dpid')


# well known cookie id's managed directly, other's can be registered
cookie_app_ids = {
    1: "lswitch",
    2: "FL:forw",
    3: "TUN:forw",
    4: "VTA:forw",
   10: "static",
}

flow_cookie_registry = { }

def register_flow_cookie_decoder(name, app_id, encoder_proc, decoder_proc, conversion_dict_name):
    """
    Register a flow encoding/decoding strategy
    """
    # should be provide a return code?

    cookie_app_ids[app_id] = name
    flow_cookie_registry[int(app_id)] = {
                                          'name'    : name,
                                          'encoder' : encoder_proc,
                                          'decoder' : decoder_proc,
                                          'cvt'     : conversion_dict_name
                                        }

def callout_flow_encoders(context):
    for (registrant, registry) in flow_cookie_registry.items():
        dict_name = registry['cvt']
        alias_dicts[dict_name] = registry['encoder'](context)


def decode_flow_cookie(i, data=None):
    cookie = i
    new_cookie = None

    # 12 bits app id - 20 bits flow hash - 32 bits user cookie
    app_id = (cookie >> 52) & ((1 << 12) - 1)

    flow_hash = (cookie >> 32) & ((1 << 20) - 1)
    if app_id in cookie_app_ids:
        new_cookie = cookie_app_ids[app_id]
        if cookie_app_ids[app_id] == "static":
            global alias_dicts
            flow_map = alias_dicts.get("staticflow", [])
            if flow_hash in flow_map:
                new_cookie += "-%s" % flow_map[flow_hash]
            else:
                new_cookie += "-flow_hash: %s" % flow_hash
        else:
            if flow_hash != 0:
                new_cookie += "-flow_hash: %s" % flow_hash
        user_cookie = (cookie & ((1 << 32) - 1))

        if user_cookie:
            if app_id in flow_cookie_registry:
                # Call registered function with the conversion dictionary
                # and the cookie
                conversion_dict_name = flow_cookie_registry[app_id]['cvt']
                new_cookie = (flow_cookie_registry[app_id]['decoder'])(
                                alias_dicts.get(conversion_dict_name), cookie)
            else:
                new_cookie += ", cookie: %#x" % user_cookie
    else:
        # if the app_id isn't known, display all 64 bits of the cookie
        new_cookie = "unknown %s, cookie %s" % (app_id, cookie)

    if new_cookie:
        return new_cookie

    return '%#x' % cookie


ether_type_to_name_dict = {
    0x0800 : "ip",
    0x0806 : "arp",
    0x8035 : "rarp",
    0x809B : "appletalk",
    0x809B : "appletalk-aarp",
    0x8100 : "802.1Q",
    0x8137 : "ipx",
    0x8138 : "novell",
    0x86dd : "ipv6",
    0x8847 : "mpls",
    0x8848 : "mpls-mc",
    0x88cc : "lldp",
}


ether_type_to_number_dict = dict([[v, n] for (n,v) in ether_type_to_name_dict.items()])


def decode_ether_type(i, data=None):
    if i == "*" or i == "": 
        return "*"
    if type(i) != int and i.startswith("0x"):
        i = int(i, 16) # i is in hex
    i = convert_signed_short_to_unsigned(i)

    try:
        i &= 0xffff
    except:
        return '*cvt %s %s*' % (i, type(i))

    return print_enum_string_and_hex_int(ether_type_to_name_dict, i)


def decode_network_protocol(i, data=None):
    if i == "*" or i == "": 
        return "*"
    ether_type = "ip" # default
    if data:
        if 'ether-type' in data:
            ether_type = decode_ether_type(data['ether-type'])
        elif 'dataLayerType' in data:
            ether_type = decode_ether_type(data['dataLayerType'])
        ether_type = str(ether_type).split('(')[0]
    net_proto_hash = {
        "ip" : {
            1  : "icmp",
            2  : "igmp",
            3  : "ggp",
            4  : "ip-in-ip",
            6  : "tcp",
            17 : "udp",
            50 : "esp",
            51 : "ah"
            },
        "arp": {
            1  : "arp-req",
            2  : "arp-rep",
            3  : "rarp-req",
            4  : "rarp-rep",
            5  : "drarp-req",
            6  : "drarp-rep",
            7  : "drarp-err",
            8  : "inarp-req",
            9  : "inarp-rep",
            }
        }
    return print_enum_string_and_int(net_proto_hash.get(ether_type, None), i)

tcp_decode_port_dict = {
    22   : "ssh",
    53   : "dns",
    80   : 'http',
    443  : 'https'
}

tcp_name_to_number_dict = dict([[v, n] for (n,v) in tcp_decode_port_dict.items()])

# fill in values if we want!
def decode_tcp_port(i): 
    return print_enum_string_and_int(tcp_decode_port_dict, i)


udp_decode_port_dict = {
    53 : "dns",
}

udp_name_to_number_dict = dict([[v, n] for (n,v) in udp_decode_port_dict.items()])

def decode_udp_port(i): 
    values = { 
        }
    return print_enum_string_and_int(values, i)


icmp_type_code_hash = {
    0 : { "name"  : "echo-rep" },
    3 : { "name"  : "dest-unreach",
          "codes" : { 0 : "net-unreach",
                      1 : "host-unreach",
                      2 : "proto-unreach",
                      3 : "port-unreach",
                      4 : "frag-needed",
                      5 : "src-rt-fail",
                      6 : "dest-net-unk",
                      7 : "dest-host-unk",
                      8 : "src-host-isol",
                      9 : "prohibit-dest-net",
                      10 : "prohibit-dest-host",
                      11 : "dest-net-unreach-for-tos",
                      12 : "dest-host-unreach-for-tos",
                      13 : "prohibit-comm" } },
    4 : { "name"  : "src-quench" },
    5 : { "name"  : "redirect",
          "codes" : { 0 : "redir-for-net",
                      1 : "redir-for-host",
                      2 : "redir-for-net-for-tos",
                      3 : "redir-for-host-for-tos" } },
    6 : { "name"  : "alt-addr-for-host" },
    8 : { "name"  : "echo-req" },
    9 : { "name"  : "router-advert" },
    10: { "name"  : "router-sel" },
    11: { "name"  : "time-exceeded",
          "codes" : { 0 : "ttl-exceeded",
                      1 : "frag-reassemble-exceeded" } },
    12: { "name"  : "param-problem",
          "codes" : { 0 : "pointer-error",
                      1 : "missing-opt",
                      2 : "bad-length" } },
    13: { "name" : "timestamp-request" },
    14: { "name" : "timestamp-reply" },
    15: { "name" : "info-request" },
    16: { "name" : "info-reply" },
    17: { "name" : "addr-mask-request" },
    18: { "name" : "addr-mask-reply" },
    30: { "name" : "traceroute" }
}
                      

def decode_icmp_type(i, data=None):
    if i in icmp_type_code_hash:
        return "%s(%s)" % (icmp_type_code_hash[i]['name'], i)
    else:
        return i


def decode_icmp_code(i, data=None):
    if data and 'transportSource' in data:
        icmp_type = decode_icmp_type(data['transportSource'], data)
    elif data and 'src-port' in data:
        icmp_type = decode_icmp_type(data['src-port'], data)
    try:
        return print_enum_string_and_int(icmp_type_code_hash[icmp_type]["codes"], i, "-")
    except:
        return "-"


def decode_src_port(i, data=None):
    i = convert_signed_short_to_unsigned(i)
    if data:
        if 'networkProtocol' in data:
            net_proto = decode_network_protocol(data['networkProtocol'], data)
        elif 'protocol' in data:
            net_proto = decode_network_protocol(data['protocol'], data)
        elif 'Protocol' in data:
            net_proto = decode_network_protocol(data['Protocol'], data)
        else:
            return i
        net_proto = str(net_proto).split('(')[0]
        if net_proto == "icmp":
            return decode_icmp_type(i)
        elif net_proto == "tcp":
            return decode_tcp_port(i)
        elif net_proto == "udp":
            return decode_udp_port(i)
    return i


def decode_dst_port(i, data=None):
    i = convert_signed_short_to_unsigned(i)
    if data:
        if 'networkProtocol' in data:
            net_proto = decode_network_protocol(data['networkProtocol'], data)
        elif 'protocol' in data:
            net_proto = decode_network_protocol(data['protocol'], data)
        elif 'Protocol' in data:
            net_proto = decode_network_protocol(data['Protocol'], data)
        else:
            return i
        net_proto = str(net_proto).split('(')[0]
        if net_proto == "icmp":
            return decode_icmp_code(i, data)
        elif net_proto == "tcp":
            return decode_tcp_port(i)
        elif net_proto == "udp":
            return decode_udp_port(i)
    return i


def decode_actions(action_list, data=None):
    TTL_DECREMENT_SUBTYPE = 18
    SET_TUNNEL_DST_SUBTYPE = 2
    NICIRA_VENDOR_ID=8992  #0x00002320
    BSN_VENDOR_ID=6035143  #0x5c16c7
    decoded_actions = []
    for a in action_list:
        decoded_action = a['type']
        if decoded_action == "OUTPUT":
            port = decode_openflow_port(a['port'], data)
            decoded_action = "output=%s" % (port,)
        elif decoded_action == "OPAQUE_ENQUEUE":
            # LOOK! Not decoded to physical port since the configuration part of CLI does not do the translation either
            decoded_action = "enqueue=%d:0x%02x" % (a['port'], a['queueId'])
        elif decoded_action == "SET_VLAN_ID":
            decoded_action = "set-vlan-id=%d" % a['virtualLanIdentifier']
        elif decoded_action == "SET_VLAN_PCP":
            decoded_action = "set-vlan-priority=0x%02x" % a['virtualLanPriorityCodePoint']
        elif decoded_action == "STRIP_VLAN":
            decoded_action = "strip-vlan"
        elif decoded_action == "SET_DL_SRC":
            decoded_action = "set-src-mac=%s" % a['dataLayerAddress']
        elif decoded_action == "SET_DL_DST":
            decoded_action = "set-dst-mac=%s" % a['dataLayerAddress']
        elif decoded_action == "SET_NW_SRC":
            decoded_action = "set-src-ip=%s" % decode_ipaddr(a['networkAddress'])
        elif decoded_action == "SET_NW_DST":
            decoded_action = "set-dst-ip=%s" % decode_ipaddr(a['networkAddress'])
        elif decoded_action == "SET_NW_TOS":
            decoded_action = "set-tos-bits=0x%02x" % a['networkTypeOfService']
        elif decoded_action == "SET_TP_SRC":
            decoded_action = "set-src-port=%d" % a['transportPort']
        elif decoded_action == "SET_TP_DST":
            decoded_action = "set-dst-port=%d" % a['transportPort']
        elif decoded_action =="VENDOR":
            if 'vendor' in a:
                if a['vendor']==NICIRA_VENDOR_ID:
                    if 'vendorData' in a:
                        vendordata=a['vendorData'].decode('base64','strict')
                        vendordata=vendordata.encode('hex')
                        if vendordata.startswith('0012'):
                            decoded_action="nicira_vendor:dec TTL"
                        else:
                            decoded_action="nicira_vendor:unknown"
                if a['vendor']==BSN_VENDOR_ID:
                    if 'vendorData' in a:
                        vendordata=a['vendorData'].decode('base64','strict')
                        vendordata=vendordata.encode('hex')
                        if vendordata.startswith('00000001'):  #BSN_ACTION_MIRROR
                            decoded_action="bsn_vendor:MIRROR"
                        elif vendordata.startswith('00000002'): #SET_TUNNEL_DST_SUBTYPE
                            ip=vendordata[8:]
                            ipaddr=[]
                            for i in [6,4,2,0]:
                                ipint= ip[i:i+2]
                                ipint= str(int(ipint,16))
                                ipaddr.append(ipint)
                            ipstr='.'.join(ipaddr)
                            decoded_action="bsn_vendor:TUNNL:" + ipstr
                        else:
                            decoded_action="bsn_vendor:unknown"
        decoded_actions.append(decoded_action)
    if len(decoded_actions) == 0:
        return "drop"
    else:
        return ",".join(decoded_actions)


def decode_port_counter(i, data=None):
    if i == -1: 
        return "n/a"
    else: 
        return i


def decode_macaddr(addr):
    ret = 'invalid-mac-addr'
    try:
        macbytes = base64.b64decode(addr)
        ret = ':'.join(["%02x" % ord(b) for b in macbytes])
    except:
        # ignore exception, the address is already set to invalid
        pass
    return ret


def decode_ipaddr(addr):
    ret = 'invalid-ip-addr'
    try:
        ipbytes = [0, 0, 0, 0]
        for i in range(4):
            ipbytes[3-i] = addr & 0x000000ff
            addr = addr>>8
        ret = '.'.join(["%d" % b for b in ipbytes])
    except:
        # ignore exception, the address is already set to invalid
        pass
    return ret


def decode_port_config(i, data=None):
    i = abs(i)
    values = {
        1 << 0 : "port-down",
        1 << 1 : "no-stp",
        1 << 2 : "no-recv",
        1 << 3 : "no-recv-stp",
        1 << 4 : "no-flood",
        1 << 5 : "no-fwd",
        1 << 6 : "no-pkt-in",
        1 << 31 : "mirror",
        }
    if i == 0 or i == '':
        return ''
    return print_mask_enum_string_and_int(values, i)

def decode_port_state(i, data=None):
    if i == '':
        return 'Unknown'

    # The two bits at (3 << 8) are a mask for the STP value
    # which are listed in the following dictionary. Note that these
    # aren't simply bit masks, so we can't use print_mask_enum_string_and_int
    # here. So instead we special case the formatting of this field. We could
    # probably factor some of this logic into a utility function, but
    # this works for now.
    stp_values = {
        0 << 8 : ": stp-listen",
        1 << 8 : ": stp-learn-no-relay",
        2 << 8 : ": stp-forward",
        3 << 8 : ": stp-block-broadcast",
        }
    value = "link-down" if i == 1 else "link-up" + stp_values.get(i, '')
    return "%s(%s)" % (value, hex(i) if i > 9 else i)
 

def decode_port_up_down(i, data=None):
    if i == '':
        return 'Unknown'
    return "down" if i == 1 else 'up'


def decode_port_stp_state(i, data=None):
    if i == '':
        return ''
    stp_values = {
        0 << 8 : "listen",
        1 << 8 : "learn-no-relay",
        2 << 8 : "forward",
        3 << 8 : "block-broadcast",
        }
    value = '' if i == 1 else stp_values.get(i)
    return "%s(%s)" % (value, hex(i) if i > 9 else i)


def decode_port_linkrate(i, data=None):
    values = {
        1 << 0 : "10mb-hd",
        1 << 1 : "10mb-fd",
        1 << 2 : "100mb-hd",
        1 << 3 : "100mb-fd",
        1 << 4 : "1gb-hd",
        1 << 5 : "1gb-fd",
        1 << 6 : "10gb-fd",
        }
    if type(i) != int:
        return "Unknown"
    if (int(i) & 0x7f) == 0:
        return "Unknown"
    return print_mask_enum_string_and_int(values, int(i) & 0x7f)


def decode_port_features(i, data=None):
    values = {
        1 << 0 : "10mb-hd",
        1 << 1 : "10mb-fd",
        1 << 2 : "100mb-hd",
        1 << 3 : "100mb-fd",
        1 << 4 : "1gb-hd",
        1 << 5 : "1gb-fd",
        1 << 6 : "10gb-fd",
        1 << 7 : "copper",
        1 << 8 : "fiber",
        1 << 9 : "autoneg",
        1 << 10 : "pause",
        1 << 11 : "pause-asym",
        }
    return print_mask_enum_string_and_int(values, i)


def decode_switch_capabilities(i, data=None):
    values = {
        1 << 0 : "flow",
        1 << 1 : "tbl",
        1 << 2 : "port",
        1 << 3 : "stp",
        1 << 5 : "ip-reasm",
        1 << 6 : "queue-stats",
        1 << 7 : "match-ip-in-arp",
        }
    return print_mask_enum_string_and_int(values, i)


def decode_switch_actions(i, data=None):
    values = {
        1 << 0 : "output",
        1 << 1 : "vlan-vid",
        1 << 2 : "vlan-pcp",
        1 << 3 : "strip-vlan",
        1 << 4 : "dl-src",
        1 << 5 : "dl-dst",
        1 << 6 : "nw-src",
        1 << 7 : "nw-dst",
        1 << 8 : "nw-tos",
        1 << 9 : "tp-src",
        1 << 10 : "tp-dst",
        1 << 11 : "enqueue",
        }
    if (i == 0):
        return "mirror"
    return print_mask_enum_string_and_int(values, i)


wildcards = {
    1 << 0 : 'in port',
    1 << 1 : 'vlan',
    1 << 2 : 'pri',
    1 << 3 : 'eth',
    1 << 4 : 'tos',
    1 << 5 : 'proto',
    1 << 6 : 'src',
    1 << 7 : 'dst',
    1 << 8 : 'mpls',
    1 << 9 : 'tc',
}

def realtime_flow_brief(i, data=None):
    brief = []
    wildcard = data['wildcards']
    # wild = print_mask_enum_string_and_int(wildcards, wildcard)
    try:
        i = int(wildcard)
    except:
        return ''

    if not wildcard & 0x1:
        brief.append('rx=%s' % decode_openflow_port(data['inputPort'], data))
    if (not wildcard & 0x2) and (data['dataLayerVirtualLan'] != -1):
        brief.append('vlan:%s' % data['dataLayerVirtualLan'])
    if (not wildcard & 0x4) and (data['dataLayerVirtualLanPriorityCodePoint'] != '*'):
        brief.append('pri:%s' % data['dataLayerVirtualLanPriorityCodePoint'])
    if (not wildcard & 0x8) and (data['dataLayerType'] != '*') :
        brief.append('eth:%s' % data['dataLayerType'])
    if not wildcard & 0x10:
        brief.append('tos:%s' % data['networkTypeOfService'])
    if not wildcard & 0x20:
        brief.append('ip:%s' % data['networkProtocol'])
    if data['networkSourceMaskLen']:
        brief.append('src:%s' % utif.ip_and_neg_mask(data['networkSource'],
                                                  data['networkSourceMaskLen']))
    if not wildcard & 0x40:
        brief.append('sport:%s' % data['transportSource'])
    if data['networkSourceMaskLen']:
        brief.append('src:%s' % utif.ip_and_neg_mask(data['networkSource'],
                                                  data['networkSourceMaskLen']))
    if not wildcard & 0x80:
        brief.append('dport:%s' % data['transportDestination'])
    # mpls not in OF1.0
    #if not wildcard & 0x100:
        #brief.append('mpls: ?')
    #if not wildcard & 0x200:
        #brief.append('mpls-tc: ?')

    return ', '.join(brief)


def formatter_to_alias_update(formatter, update):
    """
    Associate the items which need updating with the formatter.
    the update parameter is a dict, allowing multiple updates
    over several distinct fields

    Update this procedure as new decoding procedures are
    added which require some alias translation to be updated

    @param formatter function which is called to format a field
    @param update dictionry passed in, updated with alias types to update
    """
    name = formatter.__name__
    if name in ['print_host_and_alias',
                'print_all_host_attachment_points',
                'print_devicemanager_attachment_points',
                'replace_host_with_alias',
                ]:
        update['host'] = True
    if name in ['print_host_attachment_point',
                'print_vns_physical_interface_id',
                'decode_openflow_port',
                'decode_openflow_port_src_switch',
                'decode_openflow_port_dst_switch',
                'decode_openflow_port_source_switch',
                'decode_openflow_port_inputSwitch',
                'decode_openflow_port_dpid',
                'realtime_flow_brief',
                ]:
        update['switch'] = True # port implies switch
        update['port'] = True
    if name in ['print_switch_and_alias',
                'print_switch_and_alias_dpid_as_long',
                'print_switches',
                'replace_switch_with_alias',
                ]:
        update['switch'] = True
    if name in ['decode_flow_cookie'
                ]:
        update['flow'] = True
    if name in ['replace_controller_node_with_alias',
               ]:
        update['controller-node'] = True
    return update

def print_switch_port_list(i, data=None):
    if i == None:
        return ''
    return ' '.join([replace_switch_with_alias(x['switch']) + \
                    '/' + decode_openflow_port(x['port']) for x in i])

def print_switch_port(i):
    if i == None:
        return ''
    return replace_switch_with_alias(x['switch']) + \
                    '/' + decode_openflow_port(x['port'])

def print_host_attachment_point(i, data=None):
    if i == None:
        return "Inactive"
    if len(i) == 1:
        if_name = decode_openflow_port(i[0]['ingress-port'],
                                       data['attachment-points'][0])
        dpid =  str(i[0]['switch'])
        #print print_switch_and_alias(dpid, data)
        return print_switch_and_alias(i[0]['switch']) + '/' + if_name
    if len(i) == 0:
        return ""
    first = i[0]
    if 'prime' in first:
        if_name = decode_openflow_port(i[0]['ingress-port'],
                                            data['attachment-points'][0])
        return "%s/%s+[%s]" % (replace_switch_with_alias(i[0]['switch']),
                               if_name, str(len(i)-1))
    return "multiple (" + str(len(i)) + ")"


def print_all_host_attachment_points(i, data=None):
    if i == None:
        return ''
    return ' '.join([replace_switch_with_alias(x['switch']) + \
                     '/' + decode_openflow_port(x['ingress-port'],
                                                data['attachment-points'][0])
                        for x in i])


def print_devicemanager_attachment_points(i, data=None):
    if i == None:
        return ''
    return ' '.join([replace_switch_with_alias(x['switch']) + \
                     '/' + decode_openflow_port(x['port'],
                                                data['attachment-points'][0])
                        for x in i])


def print_cluster_id(i, data=None):
    return str(i)


def print_switches(i, data=None):
    return ' '.join([replace_switch_with_alias(dpid) for dpid in i])


def print_single_tag(tag):
    fields = tag.split('|')
    return "%s.%s=%s" % (fields[0],fields[1],fields[2])

def print_host_tags(i, data=None):
    if i == None or len(i) == 0:
        return ""
    tag = i
    if type(tag) == dict:
        tag = [i]
        
    if len(tag) == 1:
        # 'tag' is a complete row from the store, specifically 'tag-mapping'
        # the 'tag' key holds a name which looks like: <namespace>|<name>|<value>
        # which is then split, and recombined for display
        return print_single_tag(tag[0]['tag'])
    if len(tag) == 0:
        return ""
    return "multiple (" + str(len(tag)) + ")"


def print_all_host_tags(i, data=None):
    if i == None:
        return ''
    if i == dict:
        return print_single_tag(i['tag'])
    return ' '.join([print_single_tag(x['tag']) for x in i])


def print_ip_addresses(i, data=None):
    if i != None:
        if len(i) == 1:
            return str(i[0]['ip-address'])
        if len(i) == 0:
            return ""
        # if there are multiple addressed, see if some of the addresses
        # are "less" important than others.
        less_interesting = 0
        more_interesting = None
        for ips in i:
            if ips['ip-address'] == '0.0.0.0':
                less_interesting += 1
            elif re.match(r'169.254.*', ips['ip-address']):
                less_interesting += 1
            else:
                more_interesting = ips['ip-address']

        if len(i) == less_interesting + 1:
            return "%s+(%d)" % (more_interesting, less_interesting)
        s = sorted(i, key=lambda k: k['last-seen'])
        return "%s+(%d)" % (s[-1]['ip-address'], len(s) - 1)
        # return "multiple (" + str(len(i)) + ")"    
    return "Unknown"


def print_all_ip_addresses(i, data=None):
    if i == None:
        return ''
    return ' '.join([x['ip-address'] for x in i])


def print_devicemanager_ip_addresses(i, data=None):
    if i == None:
        return ''
    return ' '.join([x['ip'] for x in i])


def print_join_list(i, data=None):
    return ', '.join(i)


def replace_with_alias(object_type, i, data=None):
    """
    use when we have aliases for other objects
    """
    global alias_dicts
    alias_dict = alias_dicts.get(object_type, {i : i})
    if not alias_dict:
        return i
    return alias_dict.get(i, i)


def print_switch_and_alias(i, data=None):
    """
    """
    dpid = str(i)
    alias = replace_switch_with_alias(i, data)
    if dpid == alias:
        dpid_name = get_switches_names(object, data)
        if i in dpid_name:
            alias =  dpid_name[i]
            return dpid + ' (' + alias + ')'
        else:
            return dpid
    else:
        return dpid + ' (' + alias + ')'

 
def print_switch_and_alias_dpid_as_long(i, data=None):
    dpid = convert_long_to_dpid(i)
    return print_switch_and_alias(dpid)


def print_switch_interface_and_alias(i, data=None):
    """
    """
    switch_interface = str(i)
    return switch_interface


def print_host_and_alias(i, data=None):
    # The value of 'i' here ought to be the primary key
    # for the host, which is a compound key.  Try hard to
    # not deal with the construction of the key.
    mac = str(i)
    # if the host address is eight hex values, chop off the prefix
    # nasty hack to deal with the use of a int64 to hold a host
    if len(mac) == 23:
        mac = mac[6:]
        i = mac
    name = replace_host_with_alias(mac, data)
    if data == None:
        data = {}

    mac_in_data  = data.get('mac')
    suffix = ''
    if name != mac:
        suffix = ' (%s)' % name

    if mac_in_data:
        return mac_in_data + suffix
    return mac + suffix


def print_vlan_and_alias(i, data=None):
    vlan = int(i)
    # if the host address is eight hex values, chop off the prefix
    # nasty hack to deal with the use of a int64 to hold a host
    return vlan


def print_host_and_alias_mac_as_long(i, data=None):
    mac = convert_mac_in_decimal_to_hex_string(i)
    return print_host_and_alias(mac, data)

def print_host_and_alias_mac_as_long_str(i, data=None):
    # i is long decimal as string, e.g. "1342"
    mac_decimal = int(i)
    mac = convert_mac_in_decimal_to_hex_string(mac_decimal)
    return print_host_and_alias(mac, data)


# host list may also contain other items (ie: not macs)
def print_host_list_and_alias(i, data=None):
    # use at most 60 columns

    more = 0
    output = ""
    for host in i:
        mac = str(host)
        name = replace_host_with_alias(host, data)
        if name != mac:
            mac +=  ' (' + name + ')'
        if len(output) + len(mac) > 60:
            more += 1
        else:
            output += mac
            output += ' '
    if more:
        return "%s+(%s)" % (output, more)
    return output


def replace_switch_with_alias(i, data=None):
    return replace_with_alias("switch-config", i, data)

def convert_inverse_netmask_handler(i, data=None):
    if not '.' in i:
        return i
    split_bytes = i.split('.')
    return "%s.%s.%s.%s" % (255-int(split_bytes[0]),
                            255-int(split_bytes[1]),
                            255-int(split_bytes[2]),
                            255-int(split_bytes[3]))

def replace_host_with_alias(i, data=None):
    return replace_with_alias("host-config", i, data)


def replace_controller_node_with_alias(i, data=None):
    return replace_with_alias("controller-node", i, data)


def print_vns_count_dict(i, data=None):
    output = ""
    more = 0
    for vns in i:
        # possibly vns:refs instead?
        #item = '%s:%s ' % (vns, i[vns])
        item = '%s ' % vns
        if len(output) > 40:
            more += 1
        else:
            output += item
    if more:
        return "%s +(%s)" % (output, more)
    return output


def print_vns_physical_interface_id(i, data=None):
    return i.split('|')[2]


def print_domain_name_servers(i, data=None):
    if len(i) == 0:
        return 'None'
    result = i[0]
    if len(i) > 1:
        result += ' +(%d)' % (len(i)-1)
    return result


def print_all_domain_name_servers(i, data=None):
    if i == None:
        return ''
    return ', '.join(i)


def print_clock_string(i, data = None):
    print i, data


def replace_boolean_with_enable_disable(i, data=None):
    return { True : 'enabled', False : 'disabled' } [i]


def sanitize_unicode(i, data=None):
    return unicode(i, errors='ignore')


def controller_node_me_entry(i, data=None):
    if data.get('me', '') == '':
        return 'Not current controller'
    return 'current controller'

def controller_node_me(i, data=None):
    if 'me' in data:
        return '*'
    return ''


def print_ipv4addr(i, data=None):
    ipaddr = data['ip']
    mask = data['masklen']
    if (ipaddr != "*"):
        ipaddr_int = reduce(lambda a,b: a<<8 | b, map(int, ipaddr.split(".")))
        mask = (1 << mask) - 1
        ipaddr_int = ipaddr_int & ~mask
        ipaddr = ".".join(map(lambda n: str(ipaddr_int >>n & 0xFF), [24,16,8,0]))
    return ipaddr

