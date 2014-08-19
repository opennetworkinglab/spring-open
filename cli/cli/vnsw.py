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

import re
import utif

#
# VNSW -- any functions used to support vns
#

def init_vnsw(bs, modi):
    global sdnsh, mi
    sdnsh = bs
    mi = modi


MAC_RE = re.compile(r'^(([A-Fa-f\d]){2}:?){5}[A-Fa-f\d]{2}$')
DIGITS_RE = re.compile(r'^\d+$')


#
# --------------------------------------------------------------------------------

def create_default_vns():
    """
    In some situations, for example, when a blank cassandra db/store
    is having a running-config pushed.
    """
    (error, created) = sdnsh.find_or_create_row('vns-definition', 'default')
    if error:
        return False

    return True


#
# --------------------------------------------------------------------------------

def associate_foreign_key_for_vns_interface(create_dict):
    """
    Association of the foreign key 'rule' for vns-interfaces requires some
    substantial work, along with additional validation
    """

    # determine the best candidate for the current rule
    if not 'interface' in create_dict or not 'vns' in create_dict:
        return create_dict
    rule = None
    items = create_dict['interface'].split('/')
    mac = None
    port = None
    if len(items) == 1:
        if items[0].startswith('Eth'):
            rule = items[0][3:]
        elif items[0].startswith('VEth'):
            rule = items[0][4:]
        else:
            rule = items[0]
    elif len(items) == 2 and MAC_RE.match(items[1]):
        mac = items[1]
        if items[0].startswith('VEth'):
            rule = items[0][4:]
        else:
            rule = items[0]
    elif len(items) == 2 and DIGITS_RE.match(items[1]):
        port = items[1]
        if items[0].startswith('Eth'):
            rule = items[0][3:]
        else:
            rule = items[0]
    else:
        # Assume this is some abstract port name
        port = items[1]
        rule = items[0]

    if not rule:
        return create_dict

    if rule and rule == 'default':
        if not create_default_vns():
            return

    # try to look up the particular rule
    rule_id = sdnsh.unique_key_from_non_unique([create_dict['vns'],
                                               rule])
    if_obj_type = 'vns-interface-rule'
    if_key = mi.pk(if_obj_type)
    try:
        row = sdnsh.get_table_from_store(if_obj_type,
                                        if_key,
                                        rule_id)
        errors = None
    except Exception, e:
        errors = sdnsh.rest_error_to_dict(e)
        print sdnsh.rest_error_dict_to_message(errors)

    if errors:
        return create_dict

    #
    # validate the rule referenced matches the expected
    # 'id' created here.
    #
    if len(row) == 0:
        return create_dict
    if_rule = row[0]

    #
    # Perform various validations to ensure the created interface
    # would makes sense to sdnplatform
    rule_is_number = False
    if DIGITS_RE.match(if_rule['id'].split('|')[1]):
        rule_is_number = True

    if mac and 'mac' in if_rule and mac != if_rule['mac']:
        sdnsh.warning('mac %s doesn\'t match mac for interface rule %s' %
            (mac, if_rule['mac']))
        return create_dict
    #
    # If the rule is a mac rule, and the associated rule is a number,
    # make sure the 'Eth' vs 'VEth' lines up with the kind of rule
    #
    if ('mac' or 'ip-subnet' in if_rule or 'tags' in if_rule) and rule_is_number:
        if not items[0].startswith('VEth'):
            sdnsh.warning('interface name %s ought to start with VEth' %
                items[0])
            return create_dict
    if ('switch' in if_rule or 'vlans' in if_rule) and rule_is_number:
        if not items[0].startswith('Eth'):
            sdnsh.warning('interface name %s must ought to start with Eth' %
                items[0])
            return create_dict
    #
    #
    if ('switch' in if_rule and 'ports' in if_rule) and port:
        if not sdnsh.switch_port_match(port, if_rule['ports']):
            sdnsh.warning('port name %s does not match interface-rule ports %s' %
                (port, if_rule['ports']))
            return create_dict
    #
    # associate the rule_id with the interface
    create_dict['rule'] = rule_id
    return create_dict


#
# --------------------------------------------------------------------------------

def port_ntoa(op, port):
    """
    Pass in the op and the port number, and return a string for the pair,
    Both parameters are strings.  (note the leading space)
    """
    if not op in ['eq', 'neq']:
        return ''
    return '%s %s ' % (op, port)


#
# --------------------------------------------------------------------------------

def vns_acl_entry_to_text(acl):
    """
    Return a short string for a specific acl entry.  Used for both short acl display
    formats (vns-access-list-entry within a vns subconfig mode), and for show running config
    """
    if acl['type'] in ['tcp', 'udp']:
        if not acl.get('src-ip') or not acl.get('src-ip-mask'):
            return '[broken src ip or mask (a) ]'
        if not acl.get('dst-ip') or not acl.get('dst-ip-mask'):
            return '[broken src ip or mask (b) ]'
        return "%s%s%s%s" % (utif.ip_and_neg_mask(acl['src-ip'],
                                                  acl['src-ip-mask']),
                             port_ntoa(acl.get('src-tp-port-op', ''),
                                       acl.get('src-tp-port', '')),
                             utif.ip_and_neg_mask(acl['dst-ip'],
                                                  acl['dst-ip-mask']),
                             port_ntoa(acl.get('dst-tp-port-op', ''),
                                       acl.get('dst-tp-port', '')))
    elif acl['type'] == 'ip' or DIGITS_RE.match(acl['type']):
        if not acl.get('src-ip') or not acl.get('src-ip-mask'):
            return '[broken src ip or mask (c)]'
        if not acl.get('dst-ip') or not acl.get('dst-ip-mask'):
            return '[broken src ip or mask (d)]'
        return "%s%s" % (utif.ip_and_neg_mask(acl['src-ip'],
                                              acl['src-ip-mask']),
                         utif.ip_and_neg_mask(acl['dst-ip'],
                                              acl['dst-ip-mask']))
    elif acl['type'] == 'icmp':
        if not acl.get('src-ip') or not acl.get('src-ip-mask'):
            return '[broken src ip or mask (e)]'
        if not acl.get('dst-ip') or not acl.get('dst-ip-mask'):
            return '[broken src ip or mask (f)]'
        return "%s%s%s" % (utif.ip_and_neg_mask(acl['src-ip'],
                                                acl['src-ip-mask']),
                           utif.ip_and_neg_mask(acl['dst-ip'],
                                                acl['dst-ip-mask']),
                                                 acl.get('icmp-type', ""))
    elif acl['type'] == 'mac':
        if 'vlan' in acl and acl['vlan'] != None and acl['vlan'] != '':
            if 'ether-type' in acl and\
               acl['ether-type'] != None and acl['ether-type'] != '':
                return "%s %s %s vlan %s" % (acl.get('src-mac', 'any'),
                                             acl.get('dst-mac', 'any'),
                                             acl.get('ether-type'),
                                             acl['vlan'])
            else:
                return "%s %s vlan %s" % (acl.get('src-mac', 'any'),
                                          acl.get('dst-mac', 'any'),
                                          acl['vlan'])
        else:
            return "%s %s %s" % (acl.get('src-mac', 'any'),
                                 acl.get('dst-mac', 'any'),
                                 acl.get('ether-type', ''))
    else:
        return '[unrecognized acl format]'


#
# --------------------------------------------------------------------------------
            
def vns_acl_entries_to_brief(entries):
    for acl in entries:
        acl['acl-text'] = vns_acl_entry_to_text(acl)


