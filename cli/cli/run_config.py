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

#
# show running-config
#  and associated
#

import datetime
import re
import utif
import modi

from midw import *
from vnsw import *


def init_running_config(bs, modi):
    global sdnsh, mi
    sdnsh = bs
    mi = modi


running_config_registry = {}

running_config_command_choices = {
    'optional' : True,
    'choices'  : (
    )
}

#
# --------------------------------------------------------------------------------

def register_running_config(name, order, feature, running_config_proc, command_tuple = None):
    """
    Register a callback to manage the display of component running configs

    @feature a predicate to call, returns True/False to enable/disable this entry
    """
    running_config_registry[name] = { 'order'   : order,
                                      'feature' : feature,
                                      'proc'    : running_config_proc }
    if command_tuple:
        global running_config_command_choices
        running_config_command_choices['choices'] += command_tuple

#
# --------------------------------------------------------------------------------

def registry_items_enabled():
    """
    Return a list of active running config entries, this is a subset of
    the registered items, only items which are currently enabled via features
    """
    return [name for name in running_config_registry.keys()
            if running_config_registry[name]['feature'] == None or
               running_config_registry[name]['feature'](sdnsh) == True]


#
# --------------------------------------------------------------------------------

def perform_running_config(name, context, config, words):
    """
    Callout to append to config
    """
    if name in running_config_registry:
        running_config_registry[name]['proc'](context, config, words)

#
# --------------------------------------------------------------------------------

def running_config_include_alias(config, indent, obj_type, key):
    """
    Given an obj_type and an id, add the alias for this obj_type
    to the config.

    The table lookup is a bit difficult to understand; keep in
    mind, for example, the 'host-alias' table has a primary
    key called id, and a foreign key called 'host'.

    Return False if no alias exists, and True when one was added to 'config'
    """
    if obj_type in mi.alias_obj_type_xref:
        for alias in mi.alias_obj_type_xref[obj_type]:
            field = mi.alias_obj_type_field(alias)
            try:
                row = sdnsh.get_table_from_store(alias, field, key)
            except:
                row = []

            if len(row) > 1:
                sdnsh.warning('%s %s: alias count > 1' % (alias, key))
            elif len(row) == 1:
                config.append("%s%s %s\n" %
                              ('  ' * indent, alias, row[0]['id']))


#
# --------------------------------------------------------------------------------

def not_default_value(obj_type, field, value):
    """
    Return True when the value passed in is not the default value.
    """
    default_value = mi.field_default_value(obj_type, field)
    if (mi.is_null_allowed(obj_type, field) and value != None) or \
      (not mi.is_null_allowed(obj_type, field) and default_value != None
      and (default_value != value)):
        return True
    return False


#
# --------------------------------------------------------------------------------

def running_config_include_field(config, obj_type, field, value,
                                 indent, prefix = ""):
    """
    Identify fields of obj_types who's values differ from the default
    values, since these need to be included into the running-config
    """
    if mi.not_default_value(obj_type, field, value):
        #
        if mi.is_field_string(obj_type, field):
            config.append(' ' * (indent + indent) + prefix + field +
                          " %s\n" % utif.quote_string(value))
        else:
            config.append(' ' * (indent + indent) + prefix + field +
                          ' %s\n' % value)


#
# --------------------------------------------------------------------------------

def running_config_vns_acl(config, vns_name, acl, vns_acl_entries,indent=0):
    """
    factored out due to excessive indent level in do_show_running_config
    and to allow re-use in 'show vns <n> running-config'
    """
    key = mi.pk('vns-access-list')

    config.append(' ' *2*indent + 'access-list %s\n' % acl['name'])

    #
    vns_acl_fields = ['priority', 'description']
    for field in vns_acl_fields:
        running_config_include_field(config,
                                     'vns-access-list', field,
                                     acl.get(field, ''), indent+1)

    vns_acl_entries = sorted(vns_acl_entries,
                             cmp=lambda x,y: int(x['rule']) - int(y['rule']))

    for acl_entry in vns_acl_entries:
        config.append(' ' *2*(indent+1) + '%s %s %s %s\n' % (
                      acl_entry['rule'],
                      acl_entry['action'],
                      acl_entry['type'],
                      vns_acl_entry_to_text(acl_entry)))



#
# --------------------------------------------------------------------------------

def running_config_vns_if_rule(config,
                               vns_if_rule_entries,indent=0):
    """
    factored out due to excessive indent level in do_show_running_config
    and to allow re-use in 'show vns <n> running-config'

    'vns_if_rule_entries' are all the entries to concatenate to the confi
    """
    for rule in vns_if_rule_entries:
        config.append(' ' *2*indent +'interface-rule %s\n' % rule['rule'])

        #
        # mi.obj_type_config_fields('vns-interface-rule') isn't used
        # here since switch/port is collected together (managed together)
        for field in ['description', 'active', 'priority', ]:
            running_config_include_field(config,
                                         'vns-interface-rule', field,
                                         rule.get(field, ''), indent+1,)

        for field in [ 'mac', 'ip-subnet', 'vlans', 'tags']:
            running_config_include_field(config,
                                         'vns-interface-rule', field,
                                         rule.get(field, ''), indent+1, 'match ')
        if rule.get('allow-multiple', False):
            config.append(' ' * 2*(indent+1) + 'allow-multiple\n');
        if rule.get('vlan-tag-on-egress', False):
            config.append(' ' * 2*(indent+1) + 'vlan-tag-on-egress\n');

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
                config.append(' ' *2*(indent+1) + 'match switch %s %s\n' %
                                   (dpid_or_alias, rule['ports']))
            else:
                config.append(' ' *2*(indent+1) + 'match switch %s\n' % dpid_or_alias)

#
# --------------------------------------------------------------------------------

def running_config_active_vns_interfaces(vns_name, vns_interface_acl):
    """
    Return a list of interfaces which have configuration information needing
    to be saved
    """
    active = {}
    key = mi.pk('vns-interface-access-list')

    for if_acl in vns_interface_acl:
        id_fields = if_acl[key].split('|')
        vns_id= id_fields[0] +'|' + id_fields[1]
        if vns_id == vns_name:
            active[id_fields[2]] = None

    return active.keys()
#
#--------------------------------------------------------------
#
def running_config_tenant_details(config, tenant):
    """
    Display the details for the fields of a tenant which may have
    non-default values.
    """
    if tenant['active'] != mi.field_default_value('tenant', 'active'):
        config.append('no active\n')
    # mi.obj_type_config_fields('vns-definition') only shows fields which are
    # user editable, which is none, since only the command descriptions 
    # modify the fields.  XXX func which returns fields which can be updated
    # via command descriptions?
    tenant_fields = ['description', 'origin']

    for field in sorted(tenant_fields):
        running_config_include_field(config,
                                     'tenant', field,
                                     tenant.get(field,''), 1)

#
#--------------------------------------------------------------
#
def running_config_tenant_router_details(config, vrouter,indent=0):
    """
    Display the details for the fields of a vrouter which may have
    non-default values.
    """
    vrouter_fields = ['description', 'origin']

    for field in sorted(vrouter_fields):
        running_config_include_field(config,
                                     'virtualrouter', field,
                                     vrouter.get(field,''), indent+1)

#
#--------------------------------------------------------------
#
def running_config_router_interface_details(config, vr_interface, indent=0):
    """
    Display the details for the fields of a router interface which may have
    non-default values.
    """
    if 'vns-connected' in vr_interface and vr_interface['vns-connected'] is not None:
        vns=vr_interface['vns-connected'].split('|')
        vns_name=vns[1]
        config.append(' ' *2*indent + 'interface %s vns %s \n' % (vr_interface['vriname'],vns_name))
    elif 'router-connected' in vr_interface and vr_interface['router-connected'] is not None:
            router=vr_interface['router-connected'].split('|')
            tenant_name=router[0]
            router_name=router[1]
            config.append(' ' *2*indent +'interface %s tenant %s %s \n' % (vr_interface['vriname'],tenant_name, router_name))

    if vr_interface['active'] != mi.field_default_value('virtualrouter-interface', 'active'):
        config.append(' ' *2*(indent+1) + 'no active\n')

    vri_fields = ['origin']

    for field in sorted(vri_fields):
        running_config_include_field(config,
                                     'virtualrouter-interface', field,
                                     vr_interface.get(field,''), indent+1)
    try:
        ip_address_pool = sdnsh.get_table_from_store('interface-address-pool','virtual-router-interface', vr_interface['id'], "exact")
    except Exception:
        ip_address_pool = {}
        pass
    for ip_address in ip_address_pool:
        config.append(' ' *2*(indent+1) + 'ip %s\n' % (utif.ip_and_neg_mask(ip_address['ip-address'],ip_address['subnet-mask'])))

#
#--------------------------------------------------------------
#
def running_config_router_rule_details(config, vr_route,indent=0):
    """
    Display the details for the fields of a router interface which may have
    non-default values.
    """
    config_str=' ' * 2*indent
    if 'src-tenant' in vr_route and vr_route['src-tenant'] is not None:
        config_str+=('route from tenant %s' % vr_route['src-tenant'])
        if 'src-vns' in vr_route and vr_route['src-vns'] is not None:
            vns=vr_route['src-vns'].split('|')
            vns_name=vns[1]
            config_str+=(' vns %s' % vns_name)
    else:
        if 'src-ip' in vr_route and vr_route['src-ip'] is not None:
            config_str+=('route from %s' % (utif.ip_and_neg_mask(vr_route['src-ip'],vr_route['src-ip-mask'])))
        else:
            config_str+=('route from any')

    config_str+=(' to')

    if 'dst-tenant' in vr_route and vr_route['dst-tenant'] is not None:
        config_str+=(' tenant %s' % vr_route['dst-tenant'])
        if 'dst-vns' in vr_route and vr_route['dst-vns'] is not None:
            vns=vr_route['dst-vns'].split('|')
            vns_name=vns[1]
            config_str+=(' vns %s' % vns_name)
    else:
        if 'dst-ip' in vr_route and vr_route['dst-ip'] is not None:
            config_str+=(' %s' % (utif.ip_and_neg_mask(vr_route['dst-ip'],vr_route['dst-ip-mask'])))
        else:
            config_str+=(' any')

    if 'nh-ip' in vr_route and vr_route['nh-ip'] is not None:
        config_str+=(' %s' % vr_route['nh-ip'])
    if 'gateway-pool' in vr_route and vr_route['gateway-pool'] is not None:
        gwpool= vr_route['gateway-pool'].split('|')
        gwpool_name=gwpool[-1]
        config_str+=(' gw-pool %s' % gwpool_name)
    if 'outgoing-intf' in vr_route and vr_route['outgoing-intf'] is not None:
        intf= vr_route['outgoing-intf'].split('|')
        intf_name=intf[-1]
        config_str+=(' %s' % intf_name)

    config_str+=(' %s\n' % vr_route['action'])
    config.append(config_str)

#
#--------------------------------------------------------------
#
def running_config_router_gwpool_details(config, vr_gwpool, indent=0):
    """
    Display the details for the fields of a router gateway pool which may have
    non-default values.
    """
    config.append(' ' *2*indent + 'gateway-pool %s\n' % (vr_gwpool['vrgwname']))

    try:
        gw_address_pool = sdnsh.get_table_from_store('gateway-address-pool','virtual-router-gwpool', vr_gwpool['id'], "exact")
    except Exception:
        gw_address_pool = {}
        pass
    for gw_address in gw_address_pool:
        config.append(' ' *2*(indent+1) + 'ip %s\n' % gw_address['ip-address'])

#
# --------------------------------------------------------------------------------


def running_config_vns_details(config, vns,indent=0):
    """
    Display the details for the fields of a vns which may have
    non-default values.
    """
    if vns['active'] != mi.field_default_value('vns-definition', 'active'):
        config.append(' ' *2*indent + 'no active\n')
    # mi.obj_type_config_fields('vns-definition') only shows fields which are
    # user editable, which is none, since only the command descriptions 
    # modify the fields.  XXX func which returns fields which can be updated
    # via command descriptions?
    vns_fields = ['description', 'priority', 'origin',
                  'arp-mode', 'dhcp-mode', 'dhcp-ip', 'broadcast']

    for field in sorted(vns_fields):
        running_config_include_field(config,
                                     'vns-definition', field,
                                     vns.get(field,''), indent)
    vns_use_fields = ['address-space']
    for field in sorted(vns_use_fields):
        running_config_include_field(config,
                                     'vns-definition', field,
                                     vns.get(field,''), indent, "use ")


#
# --------------------------------------------------------------------------------

def running_config_vns_if_and_access_group(config, vns_name, if_name, vns_interface_acl,indent=0):
    """
    Decorate the config with access_group details
    """
    obj_type = 'vns-interface-access-list'
    key = mi.pk(obj_type)

    for if_acl in vns_interface_acl:
        # add compound key field names ('vns', 'name', etc) into if_acl
        mi.split_compound_into_dict(obj_type, key, if_acl)
        vns_id=if_acl['tenant'] +'|' + if_acl['vnsname']
        if vns_id == vns_name and if_acl['interface'] == if_name:
            config.append(' '*2*indent + "access-group %s %s\n" %
                                (if_acl['name'], if_acl['in-out']))



#
# --------------------------------------------------------------------------------

def firewall_rule(rule):
    """
    Return a string, the command which enabled this particular firewall rule
    """
    tcp_ports = {6633:  "openflow",
                 80:     "web",
                 443:    "ssl",
                 22:     "ssh"}

    src = ''
    if 'src-ip' in rule and rule['src-ip'] != '':
        src = 'from %s ' % rule['src-ip']
    dst = ''
    if 'vrrp-ip' in rule and rule['vrrp-ip'] != '':
        dst =  'local-ip %s ' % rule['vrrp-ip']

    if rule['proto'] == 'tcp' and rule['port'] in tcp_ports:
        allow = tcp_ports[rule['port']]
        return "firewall allow %s%s%s" % (src, dst, allow)
    elif rule['proto'] == 'vrrp':
        return "firewall allow %s%svrrp" % (src, dst)
    else:
        return ("firewall allow %s%s%s %s" % 
                     (src, dst, rule['proto'], rule['port']))

#
# --------------------------------------------------------------------------------

def running_config_firewall_rule(config, rule):
    """
    Return a string, the command which enabled this particular firewall rule
    """
    config.append("    %s\n" % firewall_rule(rule))

#
# --------------------------------------------------------------------------------

def running_config_feature(context, config, words):
    """
    Decorate config with the feature commands
    """
    feature = sdnsh.get_table_from_store('feature')

    if len(feature) == 0:
        return

    f_config = []

    for field in mi.obj_type_fields('feature'):
        if field == mi.pk('feature'):
            continue
        if mi.is_foreign_key('feature', field):
            continue
        default_value = mi.field_default_value('feature', field)
        name = field.replace('-feature','')
        if mi.not_default_value('feature', field, feature[0][field]):
            if default_value == True:
                f_config.append('no feature %s\n' % name)
            else:
                f_config.append('feature %s\n' % name)

    if len(f_config) > 0:
        config.append('! features enabled/disabled\n')
        config += f_config

feature_running_config_tuple = (
(
       {
           'optional'   : False,
           'field'      : 'running-config',
           'type'       : 'enum',
           'values'     : 'feature',
           'short-help' : 'Configuration for enabled/disabled features',
           'doc'        : 'running-config|show-feature',
       },
    ),
)

register_running_config('feature', 1000, None, running_config_feature, feature_running_config_tuple)


#
# --------------------------------------------------------------------------------

def running_config_controller_node(context, config, words):
    """
    Decorate config with controller-node details.
    """
    #
    controller_nodes = sdnsh.get_table_from_store("controller-node")
    controller_ifs =   sdnsh.get_table_from_store("controller-interface")
    controller_dnses = sdnsh.get_table_from_store("controller-domain-name-server")
    firewall_rules =   sdnsh.get_table_from_store("firewall-rule")

    controller_alias_dict = create_obj_type_dict('controller-alias', 'controller')

    key = mi.pk('controller-node')
    if len(words) > 0:
        if not words[0] in [c[key] for c in controller_nodes]:
            return sdnsh.error_msg('No such controller "%s"' % words[0])

    for c in controller_nodes:
        if len(words) > 0 and c[key] != words[0]:
            continue

        c_config = []

        if c[key] in controller_alias_dict:
            c_config.append('  controller-alias %s\n' %
                                controller_alias_dict[c[key]][0]['alias'])

        # mi.obj_type_config_fields() doesn't work, since
        # these fields aren't directly editable.
        for field in ['ntp-server',
                      'time-zone',
                      'domain-lookups-enabled',
                      'domain-name']:
            default_value = mi.field_default_value('controller-node', field)
            if field == 'ntp-server' and c.get(field, '') != default_value:
                c_config.append('  ntp server %s\n' % c.get(field,''))
            elif field == 'time-zone' and c.get(field, '') != default_value:
                c_config.append('  clock timezone %s\n' % c.get(field, ''))
            elif field == 'domain-lookups-enabled' and c.get(field, '') != default_value:
                c_config.append('  ip domain lookup\n')
            elif field == 'domain-name' and c.get(field, '') != default_value:
                c_config.append('  ip domain name %s\n' % c.get(field, ''))

        dns_key = mi.pk('controller-domain-name-server')
        related_dns = [x for x in controller_dnses if x[dns_key].startswith(c[key])]
        if related_dns:
            sorted_dns = sorted(related_dns, key=lambda x: x['timestamp'],
                                             cmp=lambda x,y: cmp(int(x), int(y)))
            for dns in sorted_dns:
                c_config.append('  ip name-server %s\n' % dns['ip'])

        for field in ['default-gateway',
                      'logging-enabled']:
            default_value = mi.field_default_value('controller-node', field)
            if field == 'logging-enabled' and c.get(field, '') != default_value:
                c_config.append('  logging on\n')
            elif field == 'default-gateway' and c.get(field, '') != default_value:
                c_config.append('  ip default-gateway %s\n' % c.get(field, ''))

        default_logging_server = mi.field_default_value('controller-node', 'logging-server')
        default_logging_level = mi.field_default_value('controller-node', 'logging-level')
        logging_server = c.get('logging-server', '')
        logging_level = c.get('logging-level', '')
        if ((logging_server != default_logging_server) or
            (logging_level != default_logging_level)):
            c_config.append('  logging server %s' % logging_server)
            if logging_level != default_logging_level:
                c_config.append(' level %s' % logging_level)
            c_config.append('\n')

        open_ports = [22, 6633]
        cif_key = mi.pk('controller-interface')
        fr_key = mi.pk('firewall-rule')
        for cif in controller_ifs:
            if cif[cif_key].startswith(c[key]):
                fields = cif[cif_key].split('|')
                cconfig = []
                if len(fields) > 2:
                    if_n = int(fields[2]) # interface number is the 3rd field
                    ip_mode = 'static'
                    if 'mode' in cif and cif['mode'] != '':
                        if cif['mode'] != mi.field_default_value('controller-interface',
                                                                    'mode'):
                            #
                            # currently no checks are made to warn
                            # the user when 'dhcp' mode has been
                            # selected, and ip/netmask are set.
                            cconfig.append('    ip mode %s\n' % cif['mode'])
                            ip_mode = cif['mode']

                    if (ip_mode == 'static' and
                      'ip' in cif and cif['ip'] != '' and
                      'netmask' in cif and cif['netmask'] != ''):
                        cconfig.append('    ip address %s %s\n' %
                                      (cif['ip'], cif['netmask']))

                    rules_for_cif = [r for r in firewall_rules
                                     if r['interface'] == cif[cif_key]]
                    # look for deleted default firewall rules for first interface
                    if if_n == 0:
                        default_rule_missing = list(open_ports)
                        for rule in rules_for_cif:
                            if rule['proto'] == 'tcp' and rule['port'] in default_rule_missing:
                                default_rule_missing = filter(lambda p: p != rule['port'],
                                                              default_rule_missing)
                        for dr in default_rule_missing:
                            cconfig.append('    no firewall %s tcp\n' % dr)

                    for rule in rules_for_cif:
                        # ignore default rules
                        if if_n == 0 and rule['proto'] == 'tcp' and rule['port'] in open_ports:
                            continue
                        running_config_firewall_rule(cconfig, rule)
                if len(cconfig) > 0 or cif[cif_key] != 'localhost|Ethernet|0':
                    # mi.obj_type_config_fields() doesn't work, since
                    # these fields aren't directly editable.
                    c_config.append('  interface %s %s\n' %
                                  (fields[1], fields[2]))

                    c_config += cconfig
        if len(c_config) != 0 or c[key] != 'localhost':
            config.append('!\ncontroller-node %s\n' % c[key])
            config.append(''.join(c_config))

controller_node_running_config_tuple = (
   (
       {
           'optional' : False,
           'field'      : 'running-config',
           'type'       : 'enum',
           'values'     : 'controller-node',
           'short-help' : 'Configuration for controller nodes',
           'doc'        : 'running-config|show-controller-node',
       },
       {
           'field'        : 'word',
           'type'         : 'identifier',
           'completion'   : 'complete-from-another',
           'other'        : 'controller-node|id',
           'parent-field' : None,
           'action'       : 'legacy-cli',
           'optional'     : True,
       }
    ),
)

register_running_config('controller-node', 2000, None,
                        running_config_controller_node, controller_node_running_config_tuple)


#
# --------------------------------------------------------------------------------

def running_config_switch(context, config, words):
    """
    Switch running-config
    """

    switches =                 sdnsh.get_table_from_store("switch-config")
    flow_table_entries =       sdnsh.get_table_from_store('flow-entry')

    switch_interface_configs_dict = create_obj_type_dict('switch-interface-config',
                                                         'switch')

    switch_interface_alias_dict = create_obj_type_dict('switch-interface-alias',
                                                       'switch-interface')
    key = mi.pk('switch-config')
    if len(words) > 0:
        if not words[0] in [s[key] for s in switches]:
            return sdnsh.error_msg('No such switch "%s"' % words[0])

    for s in switches:
        #
        #
        if len(words) > 0 and s[key] != words[0]:
            continue

        sw_config = []
        sw_config.append('!\nswitch %s\n' % s[key])
        #
        # add any alias for this id if it exists
        running_config_include_alias(sw_config, 1, 'switch-config', s[key])

        if mi.not_default_value('switch-config', 'core-switch', s.get('core-switch','')):
            sw_config.append("  core-switch\n") # default is not enabled

        if mi.not_default_value('switch-config', 'tunnel-termination', s.get('tunnel-termination','')):
            sw_config.append("  tunnel termination %s\n" %
                                s['tunnel-termination'])

        #
        # Look for matching switch interface config's
        sic_key = mi.pk('switch-interface-config')
        for sic in switch_interface_configs_dict.get(s[key], []):
            fields = sic[sic_key].split('|')
            if len(fields) > 1:
                sic_config = []
                # XXX perhaps mi.obj_type_config_fields() here?
                running_config_include_field(sic_config,
                                             'switch-interface-config',
                                             'mode',
                                             sic['mode'], 2,
                                                  'switchport ')

                if sic[sic_key] in switch_interface_alias_dict:
                    sic_config.append('    interface-alias %s\n' % 
                                       switch_interface_alias_dict[sic[sic_key]][0]['id'])

                if len(sic_config) > 0:
                    sw_config.append('  interface %s\n' % fields[1])
                    sw_config += sic_config

        for fte in flow_table_entries: #
            # how to handle defaults?
            if fte['switch'] == s[key]:
                sw_config.append('  flow-entry %s\n' % fte['name'])
                fields_to_print = mi.cli_model_info.get_fields_to_print('flow-entry')
                for f in fields_to_print:
                    v = fte.get(f, "")
                    if v != "" and f != 'name' and f != 'switch':
                        field_info = mi.cli_model_info.get_field_info('flow-entry', f)
                        if v == field_info.get('default', None) and f != 'active':
                            pass # literally, don't print this one
                        else:
                            sw_config.append('    %s %s\n' % (f, v))
        config += sw_config

switch_running_config_tuple = (
    (
       {
           'optional'   : False,
           'field'      : 'running-config',
           'type'       : 'enum',
           'values'     : 'switch',
           'short-help' : 'Configuration for switches',
           'doc'        : 'running-config|show-switch',
       },
       {
           'field'        : 'word',
           'type'         : 'dpid',
           'completion'   : 'complete-from-another',
           'other'        : 'switch|dpid',
           'parent-field' : None,
           'action'       : 'legacy-cli',
           'data-handler' : 'alias-to-value',
           'optional'     : True,
       }
    ),
)

register_running_config('switch', 3000, None, running_config_switch, switch_running_config_tuple)


#
# --------------------------------------------------------------------------------

def running_config_host(context, config, words):
    """
    Add host details, including tags.
    """

    # When there are no tags, and no host-aliases, the hosts don't need
    # to be read.  If it were possible to determine the number of entries
    # without querying, then it would make sense to consider enumerating
    # the host-aliases and tags if they were substantially smaller than
    # the number of hosts.
    #

    host_alias_dict = create_obj_type_dict("host-alias", 'host')
    host_security_ip = create_obj_type_dict("host-security-ip-address", 'host')
    host_security_ap = create_obj_type_dict("host-security-attachment-point", 'host')
    switch_config = create_obj_type_dict('switch-interface-config', 'id')

    # Notice that only hosts configured are enumerated here, since
    # discovered hosts can't have configuration.   All configuration
    # is attached to configured hosts, which are joined after discovered
    # hosts for show commands.
    hosts = sdnsh.get_table_from_store("host-config")
    key = mi.pk('host-config')

    if len(words) > 0:
        if not words[0] in [h[key] for h in hosts]:
            return sdnsh.error_msg('No such host "%s"' % words[0])

    for h in hosts:
        if len(words) > 0 and words[0] != h[key]:
            continue

        # host_config holds 'config' only for host.
        host_config = []
        a_s = ''
        if h.get('address-space','') != 'default':
            a_s = 'address-space %s ' % h['address-space']
        vlan = ''
        if h.get('vlan', '') != '':
            vlan = 'vlan %s ' % h['vlan']
        host_config.append('!\nhost %s%s%s\n' % (a_s, vlan, h['mac']))
        #
        # add the host alias for this id if it exists
        if h[key] in host_alias_dict:
            host_config.append("  host-alias %s\n" %
                               host_alias_dict[h[key]][0]['id'])

        #
        # check for security policies
        if h[key] in host_security_ip:
            for ip in host_security_ip[h[key]]:
                host_config.append("  security policy bind ip-address %s\n" % ip['ip-address'])

        if h[key] in host_security_ap:
            for ap in host_security_ap[h[key]]:
                host_config.append("  security policy bind attachment-point %s %s\n" %
                                    (ap.get('dpid', 'all'),
                                    utif.quote_string(ap['if-name-regex'])))

        config += host_config


host_running_config_tuple = (
    (
       {
           'optional'   : False,
           'field'      : 'running-config',
           'type'       : 'enum',
           'values'     : 'host',
           'short-help' : 'Configuration for hosts',
           'doc'        : 'running-config|show-host',
       },
       {
            'field'        : 'word',
            'type'         : 'host',
            'completion'   : 'complete-from-another',
            'other'        : 'host|mac',
            'parent-field' : None,
            'data-handler' : 'alias-to-value',
            'action'       : 'legacy-cli',
            'optional'     : True,
       }
    ),
)

register_running_config('host', 5000, None, running_config_host, host_running_config_tuple)

def running_config_vns(context, config, words):
    """
    Add the VNS configuration detils
    this command will only generate vns under default tenant 
    other vnss will be generated under running_config_tenant
    """
    #generate specific vns configuration, used in tenant config generation too
    if len(words) > 0:
        # XXX should the vns-definiiton also be included?
        sdnsh.show_vns_definition_running_config(config, words[0])
        config += sdnsh.show_vns_running_config(words[0])
        return

    #generate all VNSs for default tenant, this is used backward compatible as "show running-config vns"
    try:
        vns_def = sdnsh.get_table_from_store('vns-definition')
    except Exception:
        vns_def = []

    try:
        vns_rules = create_obj_type_dict('vns-interface-rule', 'vns')
    except Exception:
        vns_rules = {}

    try:
        vns_acls = create_obj_type_dict('vns-access-list', 'vns')
    except Exception:
        vns_acls = {}

    try:
        vns_acl_entries = create_obj_type_dict('vns-access-list-entry',
                                               'vns-access-list')
    except Exception:
        vns_acl_entries = {}

    try:
        vns_interface = create_obj_type_dict('vns-interface-config', 'vns')
    except Exception:
        vns_interface = {}

    try:
        vns_interface_acl = create_obj_type_dict('vns-interface-access-list',
                                                 'vns-interface')
    except Exception:
        vns_interface_acl = {}
        
    #
    # vns-definition
    for vns in vns_def:
        if vns['tenant']=='default':
            vns_name = vns['id']
            config.append('!\nvns-definition %s\n' % vns['vnsname'])
            running_config_vns_details(config, vns)
            if vns_name in vns_rules:
                running_config_vns_if_rule(config,
                                       vns_rules[vns_name],indent=1)
    #
    # vns
    acl_key = mi.pk('vns-access-list')
    if_key = mi.pk('vns-interface-config')

    for vns in vns_def:
        if vns['tenant'] =='default':
            vns_name = vns['id']
            vns_config = []
    
            for acl in vns_acls.get(vns_name, []):
                acl_id = acl[acl_key]       # compound primary key value
                running_config_vns_acl(vns_config, vns_name, acl, vns_acl_entries[acl_id],indent=1)
    
            for vns_if in vns_interface.get(vns_name, []):
                vns_if_acl_config = []
                vns_if_id = vns_if[if_key]  # compound primary key value
                if vns_if_id in vns_interface_acl:
                    running_config_vns_if_and_access_group(vns_if_acl_config,
                                                           vns_name,
                                                           vns_if['interface'],
                                                           vns_interface_acl[vns_if_id],indent=1)
                if vns_if_acl_config:
                    vns_config.append("  interface %s\n" % vns_if['interface'])
                    vns_config += vns_if_acl_config
    
            if len(vns_config) > 0:
                config.append('!\nvns %s\n' % vns['vnsname'])
                config += vns_config
                vns_config = []


def is_vns_enabled(context):
    return context.netvirt_feature_enabled()

vns_running_config_tuple = (
    (
        {
            'optional'   : False,
            'field'      : 'running-config',
            'type'       : 'enum',
            'values'     : 'vns',
            'short-help' : 'Configuration for network virtualization',
            'doc'        : 'running-config|show-vns',
        },
        {
            'field'        : 'word',
            'type'         : 'identifier',
            'completion'   : 'complete-from-another',
            'other'        : 'vns-definition|vnsname',
            'parent-field' : None,
            'action'       : 'legacy-cli',
            'optional'     : True,
        }
    ),
)

register_running_config('vns', 7000, is_vns_enabled, running_config_vns, vns_running_config_tuple)

#tenant running config
def running_config_tenant(context, config, words):

    if len(words) > 0:
        # XXX should the vns-definiiton also be included?
        sdnsh.show_tenant_running_config(config, words[0])
        return

    try:
        tenants = sdnsh.get_table_from_store('tenant')
    except Exception:
        tenants = []

    for tenant in tenants:
        sdnsh.show_tenant_running_config(config, tenant['name'])


tenant_running_config_tuple = (
    (
        {
            'optional'   : False,
            'field'      : 'running-config',
            'type'       : 'enum',
            'values'     : 'tenant',
            'short-help' : 'Configuration for Tenant',
            'doc'        : 'running-config|show-tenant',
        },
        {
            'field'        : 'word',
            'type'         : 'identifier',
            'completion'   : 'complete-from-another',
            'other'        : 'tenant|name',
            'parent-field' : None,
            'action'       : 'legacy-cli',
            'optional'     : True,
        }
    ),
)

register_running_config('tenant', 7000, is_vns_enabled, running_config_tenant, tenant_running_config_tuple)

def running_config_static_arp(context, config, words):

    try:
        staticARPs = sdnsh.get_table_from_store('static-arp')
    except Exception:
        staticARPs = []
    first=True
    for arp in staticARPs:
        if first:
            config.append('!\narp %s %s\n' % (arp['ip'], arp['mac']))
            first=False
        else:
            config.append('arp %s %s\n' % (arp['ip'], arp['mac']))

staticARP_running_config_tuple = (
    (
        {
            'optional'   : False,
            'field'      : 'running-config',
            'type'       : 'enum',
            'values'     : 'static-arp',
            'short-help' : 'Configuration for static ARP entry',
            'doc'        : 'running-config|show-static-arp',
        },
    ),
)

register_running_config('static-arp', 6000, None, running_config_static_arp, staticARP_running_config_tuple)


#
# --------------------------------------------------------------------------------

def implement_show_running_config(words):
    """
    Manager for the 'show running-config' command, which calls the
    specific detail functions for any of the parameters.
    """

    # LOOK! hardwired - need to use the obj_type_info and the field_list
    # LOOK! how are these sorted?
    config = []
    if len(words) > 0:
        # pick the word
        choice = utif.full_word_from_choices(words[0],
                                             registry_items_enabled())
        if sdnsh.netvirt_feature_enabled() and 'vns'.startswith(words[0]):
            if choice:
                return sdnsh.error_msg("%s and %s ambiguous" % (choice, 'vns'))
            choice = 'vns'

        if choice: 
            perform_running_config(choice, sdnsh, config, words)
        else:
            return sdnsh.error_msg("unknown running-config item: %s" % words[0])
        # config[:-1] removes the last trailing newline
        return ''.join(config)[:-1]
    else:
        # Create the order based on the registration value
        running_config_order = sorted(registry_items_enabled(),
                                      key=lambda item: running_config_registry[item]['order'])
        exclude_list=['vns']
        for rc in running_config_order:
            if rc not in exclude_list:
                perform_running_config(rc, sdnsh, config, words)

        prefix = []
        if len(config) > 0:
            date_string = datetime.datetime.now().strftime("%Y-%m-%d.%H:%M:%S %Z")
            prefix.append("!\n! ")
            prefix.append(sdnsh.do_show_version(words))
            prefix.append("\n! Current Time: ")
            prefix.append(date_string)
            prefix.append("\n!\n")
            prefix.append("version 1.0\n") # need a better determination of command syntax version

        # config[:-1] removes the last trailing newline
        return ''.join(prefix)  + ''.join(config)[:-1]


