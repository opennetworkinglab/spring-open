#
# Copyright (c) 2013 Big Switch Networks, Inc.
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

from sdncon.rest.config import add_config_handler
"""
from sdncon.controller.models import Feature, GlobalConfig, Controller, \
    ControllerInterface, ControllerDomainNameServer, \
    FirewallRule, ControllerAlias, SnmpServerConfig, ImageDropUser
from sdncon.controller.models import TacacsPlusConfig, TacacsPlusHost
"""
from oswrapper import exec_os_wrapper
import os
import re
import sdncon
from django.core import serializers

# FIXME: Can probably get rid of default_id when the rest of the code is
# in place for supporting multiple controller IDs. But what about
# unit tests where we shouldn't rely on the boot-config file existing
def get_local_controller_id(default_id='localhost'):
    local_controller_id = default_id
    f = None
    try:
        f = open("%s/run/boot-config" % sdncon.SDN_ROOT, 'r')
        data = f.read()
        match = re.search("^controller-id=([0-9a-zA-Z\-]*)$", data, re.MULTILINE)
        if match:
            local_controller_id = match.group(1)
    except Exception, _e:
        # If there was any error, then just leave the controller ID as the
        # default value.
        pass
    finally:
        if f:
            f.close()
    return local_controller_id

"""
# Add the config handlers here. Check the comments for add_config_handler in rest/config.py
# for a description of the calling conventions for config handlers.

def network_config_handler(op, old_instance, new_instance, modified_fields):
    valid_instance = old_instance if (op == 'DELETE') else new_instance
    if isinstance(valid_instance, Controller):
        controller_node = valid_instance
        controller_id = controller_node.id
        if op == 'DELETE':
            # no further configuration here
            return
    elif isinstance(valid_instance, ControllerDomainNameServer) \
      or isinstance(valid_instance, ControllerInterface):
        controller_id = valid_instance.controller_id
        try:
            controller_node = Controller.objects.get(pk=controller_id)
        except Exception, _e:
            # unknown controller node during delete, no need to
            # do anything with any of these interfaces
            return
    else:
        raise Exception('Unknown model change trigger network config handler')

    if controller_id != get_local_controller_id():
        return

    if op == 'DELETE':
        # don't reconfigure the interfaces during delete, since
        # for deletes, the values of ip/netmask don't get updated
        dns = ControllerDomainNameServer.objects.filter(
                        controller=controller_node).order_by('timestamp')
        exec_os_wrapper('NetworkConfig', 'set', 
                        [serializers.serialize("json", [controller_node]),
                         serializers.serialize("json", dns)])
    else:
        # op != 'DELETE'
        #
        # XXX what about HA?
        # 'ifs' below isn't filtered by controller, the NetConfig
        # target will only select interfaces assocaited with the
        # controller-node 'localhost.
        dns = ControllerDomainNameServer.objects.filter(
                        controller=controller_node).order_by('-priority')
        ifs = ControllerInterface.objects.filter(controller=controller_node)
        exec_os_wrapper('NetworkConfig', 'set', 
                        [serializers.serialize("json", [controller_node]),
                         serializers.serialize("json", dns),
                         serializers.serialize("json", ifs)])

def firewall_entry_handler(op, old_instance, new_instance, modified_fields=None):
    #allow in on eth0 proto tcp from any to any port 80
    print "XXX: firewall handler called-1" 
    command = ""
    if op == "DELETE" and str(old_instance.interface.controller) == get_local_controller_id():
        command += "delete "
        instance = old_instance
    elif (op == "INSERT" or op == "UPDATE") and str(new_instance.interface.controller) == get_local_controller_id():
        instance = new_instance
    else:
        return

    print instance.action
    print instance.proto
    print instance.port
    print instance.src_ip
    print instance.vrrp_ip
    print "XXX: firewall handler called-2" 
    controller_interface = instance.interface
    eth = 'eth' + str(controller_interface.number) #LOOK! Hardcoded to eth interface
    proto_str = ""
    if instance.proto != '' and instance.proto != 'vrrp':
        proto_str = " proto " + instance.proto
    action_str = instance.action
    src_str = " from any"
    if instance.src_ip != '':
        src_str = " from " + instance.src_ip
    dst_str = " to any"
    if instance.vrrp_ip != '':
        dst_str = " to " + instance.vrrp_ip 
    print "dst_str = ", dst_str
    port_str = ""
    if instance.port != 0:
        port_str = " port " + str(instance.port)

    command += (action_str + " in on " + eth + proto_str + src_str + dst_str + port_str)
    print command

    exec_os_wrapper('ExecuteUfwCommand', 'set', [command])
    if instance.port == 6633 and action_str == 'reject' and op != 'DELETE':
        exec_os_wrapper('RestartSDNPlatform', 'set', [])

def ntp_config_handler(op, old_instance, new_instance, modified_fields=None):
    if new_instance != None:
        exec_os_wrapper("SetNtpServer", 'set', [new_instance.ntp_server])

def tz_config_handler(op, old_instance, new_instance, modified_fields=None):
    if op == "DELETE":
        if str(old_instance.id) != get_local_controller_id():
            return
        exec_os_wrapper("UnsetTimezone", 'set')
    elif op == "INSERT" or op == "UPDATE":
        if str(new_instance.id) != get_local_controller_id():
            return
        if new_instance.time_zone != None and str(new_instance.time_zone) != "":
            exec_os_wrapper("SetTimezone", 'set', [new_instance.time_zone])

def logging_server_config_handler(op, old_instance, new_instance, modified_fields=None): 
    if op == "DELETE":
        if str(old_instance.id) != get_local_controller_id():
            return
        exec_os_wrapper("UnsetSyslogServer", 'set',
                        [old_instance.logging_server, old_instance.logging_level])
    elif op == "INSERT" or op == "UPDATE":
        if str(new_instance.id) != get_local_controller_id():
            return
        if new_instance.logging_server != "" and new_instance.logging_enabled:
            exec_os_wrapper("SetSyslogServer", 'set',
                            [new_instance.logging_server, new_instance.logging_level])
        else:
            exec_os_wrapper("UnsetSyslogServer", 'set',
                            [new_instance.logging_server, new_instance.logging_level])

def vrrp_virtual_router_id_config_handle(op, old_instance, new_instance, modified_fields=None):
    if op == "INSERT" or op == "UPDATE":
        exec_os_wrapper("SetVrrpVirtualRouterId", 'set',
                        [new_instance.cluster_number])
    
def netvirt_feature_config_handler(op, old_instance, new_instance, modified_fields=None):
    if op == "INSERT" or op == "UPDATE":
        if new_instance.netvirt_feature:
            exec_os_wrapper("SetDefaultConfig", 'set', [new_instance.netvirt_feature])
        else:
            exec_os_wrapper("SetStaticFlowOnlyConfig", 'set', [new_instance.netvirt_feature])

def controller_alias_config_handler(op, old_instance, new_instance, modified_fields=None):
    if op == 'INSERT' or op == 'UPDATE':
        if str(new_instance.controller) == get_local_controller_id():
            exec_os_wrapper("SetHostname", 'set', [new_instance.alias])

def tacacs_plus_config_handler(op, old_instance, new_instance, modified_fields=None):

    if isinstance(old_instance, TacacsPlusConfig):
        if op == 'DELETE':
            # deleting the config singleton (presumably during shutdown?)
            return

    if isinstance(old_instance, TacacsPlusConfig):
        config_id = old_instance.id
    else:
        config_id = 'tacacs'
    try:
        config = TacacsPlusConfig.objects.get(pk=config_id)
    except TacacsPlusConfig.DoesNotExist:
        # cons up a dummy config object, not necessary to save it
        config = TacacsPlusConfig()

    # get current list of hosts (op==DELETE ignored here)
    ##hosts = TacacsPlusHost.objects.order_by('timestamp')
    def timestampSort(h1, h2):
        return cmp(h1.timestamp, h2.timestamp)
    hosts = sorted(TacacsPlusHost.objects.all(), timestampSort)

    # XXX roth -- config is passed as-is, not as a single-element list
    cj = serializers.serialize("json", [config])
    hj = serializers.serialize("json", hosts)
    print "Calling oswrapper with:", [cj, hj]
    exec_os_wrapper('TacacsPlusConfig', 'set', [cj, hj])
        
def snmp_server_config_handler(op, old_instance, new_instance, modified_fields=None):
    if op == 'DELETE':
        exec_os_wrapper('UnsetSnmpServerConfig', 'set', [])
    elif op == 'INSERT' or op == 'UPDATE':
        # enable_changed is true if operation is INSERT, else compare with old instance
        if (op == 'INSERT'):
            enable_changed = (new_instance.server_enable is True) #since default is False
            print 'operation= insert, enable_changed = ', enable_changed
        else:
            enable_changed = (new_instance.server_enable != old_instance.server_enable)
        server_enable = new_instance.server_enable 
        community  = '' if new_instance.community is None else new_instance.community 
        location = '' if new_instance.location is None else new_instance.location 
        contact  = '' if new_instance.contact is None else new_instance.contact

        print "Calling oswrapper with:", [server_enable, community, location, contact, enable_changed]
        exec_os_wrapper('SetSnmpServerConfig', 'set',
                        [server_enable, community, location, contact, enable_changed])
"""
def test_config_handler(op, old_instance, new_instance, modified_fields=None):
    pass

def images_user_ssh_key_config_handler(op, old_instance, new_instance, modified_fields=None):
    if op == 'INSERT' or op == 'UPDATE':
        sshkey = "\"" + str(new_instance.images_user_ssh_key) + "\""
        exec_os_wrapper('SetImagesUserSSHKey', 'set', [sshkey])

def init_config():
    # 
    # Associate the config handlers with specific callout for each of the fields
    #  Keep in mind that these are the django names, NOT the rest api names,
    #
    """
    disabled_by_shell_variable = os.environ.get('SDNCON_CONFIG_HANDLERS_DISABLED', False)
    disabled_by_file = os.path.exists("%s/sdncon_config_handlers_disabled" % sdncon.SDN_ROOT)
    if not disabled_by_shell_variable and not disabled_by_file:
        add_config_handler({Controller: ['ntp_server']}, ntp_config_handler)
        add_config_handler({Controller: ['time_zone']}, tz_config_handler)
        add_config_handler(
            {
                Controller: ['domain_lookups_enabled', 'domain_name', 'default_gateway'],
                ControllerDomainNameServer: None,
                ControllerInterface: ['ip', 'netmask', 'mode'],
            }, network_config_handler)
        add_config_handler({ControllerAlias: ['alias']}, controller_alias_config_handler)
        add_config_handler({Controller: ['logging_enabled', 'logging_server', 'logging_level']}, logging_server_config_handler)
        add_config_handler({Feature: ['netvirt_feature']}, netvirt_feature_config_handler)
        add_config_handler({FirewallRule: None}, firewall_entry_handler)
        add_config_handler({GlobalConfig: ['cluster_number']}, vrrp_virtual_router_id_config_handle)
        add_config_handler({ TacacsPlusConfig: ["tacacs_plus_authn", "tacacs_plus_authz", "tacacs_plus_acct",
                                                "local_authn", "local_authz",
                                                "timeout", "key",],
                             TacacsPlusHost: ['ip', 'key'],
                             },
                           tacacs_plus_config_handler)
        add_config_handler({SnmpServerConfig: ['server_enable', 'community', 'location', 'contact']}, snmp_server_config_handler)
        add_config_handler({ImageDropUser: ['images_user_ssh_key']}, images_user_ssh_key_config_handler)
    else:
        add_config_handler(
            {
                Controller: ['domain_lookups_enabled', 'domain_name', 'default_gateway'],
                ControllerDomainNameServer: None,
                ControllerInterface: ['ip', 'netmask', 'mode'],
                ControllerAlias: ['alias'],
                FirewallRule: None,
                Feature: None,
                GlobalConfig: ['ha-enabled', 'cluster-number'],
            }, test_config_handler)
    """
