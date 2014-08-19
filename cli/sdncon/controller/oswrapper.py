#!/usr/bin/python
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

import os, atexit
import glob
from subprocess import Popen, PIPE, check_output, CalledProcessError
import sys, traceback, socket
from optparse import OptionParser
from types import StringType
import datetime
import json
import re
import time
import urllib2
import httplib
import fcntl, shutil
import sys
import tempfile
import stat

from string import Template
from django.forms import ValidationError

# Can't import from `import sdncon` -- causes circular dependency!
SDN_ROOT = "/opt/sdnplatform" if not 'SDN_ROOT' in os.environ else os.environ['SDN_ROOT']

# Big Switch Networks Enterprise OID
BSN_ENTERPRISE_OID = '.1.3.6.1.4.1.37538'
BSN_ENTERPRISE_OID_CONTROLLER = BSN_ENTERPRISE_OID + '.1'


class OsWrapper():
    """ This base class abstracts executing os binaries without using shell in a secure, hardened way.
        Things to keep in mind when composing command templates - because we dont use shell, args to the 
        binaries are presented as items in the list, so no need to de-specialize special characters , for 
        example, if you want to echo something into a file, command is "echo -e abc\ndef\n" and not 
        "echo -e \"abc\ndef\n\"", which would then result in the quotes also to be echo'ed.
    """
    name = "none"
    cmds_lst_for_set = []
    cmds_lst_for_get = []
    sudo_required_for_set = True
    sudo_required_for_get = False
    def __init__(self, name, this_cmds_list_for_set, this_cmds_list_for_get, is_sudo_reqd_for_set=True, is_sudo_required_for_get=False):
        self.name = name
        self.cmds_lst_for_set = this_cmds_list_for_set
        self.cmds_lst_for_get = this_cmds_list_for_get
        self.sudo_required_for_set = is_sudo_reqd_for_set
        self.sudo_required_for_get = is_sudo_required_for_get

        self.IP_RE = re.compile(r'^(\d{1,3}\.){3}\d{1,3}$')
        self.DomainName_RE = re.compile(r'^([a-zA-Z0-9-]+.?)+$')

    def validate_ip(self, value):
        if not self.IP_RE.match(value) or len([x for x in value.split('.') if int(x) < 256]) != 4:
            return False, "IP must be in dotted decimal format, 234.0.59.1"
        return True, ""

    def validate_domain(self, value):
        if not self.DomainName_RE.match(value):
            return False, "Invalid domain name"
        return True, ""

    def exec_cmds(self, cmds_lst, cmds_args, stdout_file_lst):
        # traverse through the list of commands needed to set this
        # cmd_args is a list of lists of args for each of the commands in the set list.
        ret_out_err = {'err': [], 'out': []}
        if len(cmds_lst) != len(cmds_args):
           # commands to args mismatch, error out and return
           # possibly throw an exception here as well
           ret_out_err['err'].append("Command and args mismatch")
           return ret_out_err
        for indx in range(len(cmds_lst)):
            #if self.sudo_required_for_set:
            #   cmd_string = "sudo "
            #else:
            #   cmd_string = "" 
            cmd_string = ""
            cmd_template = Template(cmd_string + cmds_lst[indx])
            args_map = dict({}) 
            for args_indx in range(len(cmds_args[indx])):
                args_map["arg%d"%(args_indx+1)] = cmds_args[indx][args_indx]
                cmd_string += cmds_args[indx][args_indx] + " "             
            full_cmd_string = cmd_template.substitute(args_map)
            file_for_stdout = PIPE
            if stdout_file_lst != None and stdout_file_lst[indx] != "":
                file_for_stdout = open(stdout_file_lst[indx], 'a')
            sub_proc_output = Popen(full_cmd_string.rsplit(" "), shell=False, stdin=PIPE, stdout=file_for_stdout, stderr=PIPE, close_fds=True)
            ret_out_err['err'].append([sub_proc_output.stderr.read()])
            if file_for_stdout == PIPE:
                ret_out_err['out'].append([sub_proc_output.stdout.read()])
            else:
                file_for_stdout.close()
            # not fit for pipe io - os.system(cmd_string) # need to check for errors in commands.
        return ret_out_err
    
    def exec_cmds_new(self, cmds_lst, cmds_args, useShell=False, appendStdOut=True):
        # traverse through the list of commands needed to set this
        # cmd_args is a list of lists of args for each of the commands in the set list.
        ret_out_err = {'err': [], 'out': []}
        if len(cmds_lst) != len(cmds_args):
           # commands to args mismatch, error out and return
           # possibly throw an exception here as wella
           # cmds_list is a list of dict maps - [{'bin_name': <bin>, 'args_lst': <args-list>, 'stdoutfile':<filename>},...]
           ret_out_err['err'].append("Command and args mismatch")
           print 'cmds_lst:', cmds_lst
           print 'cmd_args:', cmds_args
           return ret_out_err
        for indx in range(len(cmds_lst)):
            #if self.sudo_required_for_set:
            #   cmd_string = "sudo "
            #else:
            #   cmd_string = "" 
            #cmd_string = ""
            cmd_args_lst = [cmds_lst[indx]['bin_name']]
            
            args_map = dict({}) 
            for args_indx in range(len(cmds_args[indx])):
                args_map["arg%d"%(args_indx+1)] = cmds_args[indx][args_indx]
                #cmd_string += cmds_args[indx][args_indx] + " "  
            if 'args_lst' in cmds_lst[indx]:  
                for args_indx in range(len(cmds_lst[indx]['args_lst'])):
                    arg_template = Template(cmds_lst[indx]['args_lst'][args_indx])
                    full_arg_string = arg_template.substitute(args_map)
                    cmd_args_lst.append(full_arg_string)
            file_for_stdout = PIPE
            if 'stdoutfile' in cmds_lst[indx] and cmds_lst[indx]['stdoutfile'] != "":
                fMode = 'a'
                if not appendStdOut:
                    fMode = 'r+'
                file_for_stdout = open(cmds_lst[indx]['stdoutfile'] , fMode)
            sub_proc_output = Popen(cmd_args_lst, shell=useShell, stdin=PIPE, stdout=file_for_stdout, stderr=PIPE, close_fds=True)
            ret_out_err['err'].append(sub_proc_output.stderr.read())
            if file_for_stdout == PIPE:
                ret_out_err['out'].append(sub_proc_output.stdout.read())
            else:
                file_for_stdout.close()
            # not fit for pipe io - os.system(cmd_string) # need to check for errors in commands.
        return ret_out_err
    
    def set(self, cmds_args, stdout_file_lst):
        return self.exec_cmds(self.cmds_lst_for_set, cmds_args, stdout_file_lst)
    def get(self, cmds_args, stdout_file_lst):
        return self.exec_cmds(self.cmds_lst_for_get, cmds_args, stdout_file_lst)
    
    def set_new(self, cmds_args_lst = [], cmds_args = [], useShell=False, appendStdOut = False):
        if cmds_args_lst == []:
            cmds_args_lst = self.cmds_lst_for_set
        return self.exec_cmds_new(cmds_args_lst, cmds_args, useShell, appendStdOut)
    def get_new(self, cmds_args_lst = [], cmds_args = [], useShell=False, appendStdOut = False):
        if cmds_args_lst == []:
            cmds_args_lst = self.cmds_lst_for_get 
        return self.exec_cmds_new(cmds_args_lst, cmds_args, useShell, appendStdOut)
    

def validate_input1(validator, value): #temporarily disabling this - TBD
    try:
        validator(value)
    except ValidationError, _err:
        return False
    return True


def validate_input(validator, value):
    a = True
    #try:
        #a, b = validator(value)
    #except ValidationError, err:
    #    return False
    return a


def dotted_decimal_to_int(ip):
    """
    Converts a dotted decimal IP address string to a 32 bit integer
    """
    bytes = ip.split('.')
    ip_int = 0
    for b in bytes:
        ip_int = (ip_int << 8) + int(b)
    return ip_int


def same_subnet(ip1, ip2, netmask):
    """
    Checks whether the two ip addresses are on the same subnet as
    determined by the netmask argument. All of the arguments are
    dotted decimal IP address strings.
    """
    if ip1 == '' or ip2 == '' or netmask == '':
        return False
    ip1_int = dotted_decimal_to_int(ip1)
    ip2_int = dotted_decimal_to_int(ip2)
    netmask_int = dotted_decimal_to_int(netmask)
    return (ip1_int & netmask_int) == (ip2_int & netmask_int)


class NetworkConfig(OsWrapper):
    def __init__(self, name="network_config"):
        OsWrapper.__init__(self, name, [], [])


    def rewrite_etc_network_interfaces(self, controller, interfaces, ret_result):
        """
        Return True when the /etc/network/interfaces is rewritten. Return False otherwise.
        The file is rewritten only when the intended new contents is different from
        the old contents, this is an attempt to not purturb the network unless something
        really changed.
        """

        gateway = controller['fields']['default_gateway']
        if (gateway != ''):
            (r, m) = self.validate_ip(gateway)
            if not r:
                ret_result['err'].append("Default gateway: %s" % m)
                gateway = ''

        changed = False
        new_conf = []
        new_conf.append("# WARNING this file is automanaged by BSN controller\n")
        new_conf.append("# DO NOT EDIT here, use CLI with 'configure'\n")
        new_conf.append("auto lo\niface lo inet loopback\n\n")
        for interface in interfaces:
            if (interface['fields']['controller'] == controller['pk']):
                num = interface['fields']['number']
                new_conf.append("auto eth{0}\n".format(num))
                if (interface['fields']['mode'] == 'dhcp'):
                    new_conf.append("iface eth{0} inet dhcp\n".format(num))
                else:
                    ip = interface['fields']['ip']
                    netmask = interface['fields']['netmask']
                    if (ip != ""):
                        (r, m) = self.validate_ip(ip)
                        if not r:
                            ret_result['err'].append(
                                "Ethernet %s IP address %s: %s" % (num, ip, m))
                            ip = ""
                    if (netmask != ""):
                        (r, m) = self.validate_ip(netmask)
                        if not r:
                            ret_result['err'].append(
                                "Ethernet %s netmask %s: %s" % (num, netmask, m))
                            netmask = ""
        
                    new_conf.append("iface eth{0} inet static\n".format(num))
                    if (ip != ""):
                        new_conf.append("    address {0}\n".format(ip))
                    if (netmask != ""):
                        new_conf.append("    netmask {0}\n".format(netmask))
                    if same_subnet(gateway, ip, netmask):
                        new_conf.append("    gateway {0}\n".format(gateway))
                new_conf.append("\n")

        f = open("/etc/network/interfaces", "r")
        if (''.join(new_conf) != f.read()):
            f.close()
            f = open("/etc/network/interfaces", "w")
            f.write(''.join(new_conf))
            changed = True
        f.close()
        return changed

    def rewrite_etc_resolve_conf(self, controller, dns_servers, ret_result):
        """
        Return True when the /etc/resolv.conf is rewritten. Return False otherwise.
        The file is rewritten only when the intended new contents is different from
        the old contents, this is an attempt to not purturb the network unless something
        really changed.
        """

        changed = False
        new_conf = []
        domain_name = controller['fields']['domain_name']
        if (domain_name != ""):
            new_conf.append("domain {0}\nsearch {1}\n".format(domain_name,
                                                              domain_name))
        
        if (controller['fields']['domain_lookups_enabled'] == True):
            for dns in dns_servers:
                if (dns['fields']['controller'] == controller['pk']):
                    ip = dns['fields']['ip']
                    if (ip != ""):
                        (r, m) = self.validate_ip(ip)
                        if not r:
                            ret_result['err'].append("Name server %s: %s" % (ip, m))
                            ip = ""
                    if (ip != ""):
                        new_conf.append("nameserver {0}\n".format(ip))
        
        f = open("/etc/resolv.conf", "r")
        if (''.join(new_conf) != f.read()):
            f.close()
            f = open("/etc/resolv.conf", "w")
            f.write(''.join(new_conf))
            changed = True

        f.close()
        return changed

    def set(self, args_list):
        # args_list: [controllers, controlleDomainServers, controllerInterfaces]
        # controllerInterfaces may be empty, which requess no rewrite
        # of /etc/network/insterfaces
        ifs_rewrite = False if len(args_list) < 3 else True

        ret_result = {'err': [], 'out': []}
        controller = json.loads(args_list[0])[0]
        network_restart = True

        rc_changed = False
        ni_changed = False

        try:
            domain_name = controller['fields']['domain_name']
            new_rc = True
            if domain_name != "":
                (r, m) = self.validate_domain(domain_name)
                if not r:
                    ret_result['err'].append("Search domain %s: %s"
                                             % (domain_name, m))
                    new_rc = False

            if new_rc:
                rc_changed = self.rewrite_etc_resolve_conf(controller,
                                                           json.loads(args_list[1]),
                                                           ret_result)
            
            if ifs_rewrite:
                ni_changed = self.rewrite_etc_network_interfaces(controller,
                                                                 json.loads(args_list[2]),
                                                                 ret_result)
        except Exception, _e:
            network_restart = False
            traceback.print_exc()

        # don't restart the network config if resolv.conf was only updated
        if network_restart and ni_changed:
            # Kill any dhclients that might be hanging around.  When
            # switching from dhcp to static config, networking restart
            # won't kill these since the file has already been
            # rewritten with a config that doesn't include DHCP.  A
            # cleaner fix for this is to stop networking before
            # writing the file and then start it after writing the
            # file.  Longer-term, it might be better to use
            # NetworkManager APIs for all of this rather than trying
            # to manage this config file.
            k = Popen(["/usr/bin/killall", "dhclient3"],
                      stdout=PIPE, stderr=PIPE)
            k.wait()
            
            p = Popen(["/usr/sbin/invoke-rc.d",
                       "networking",
                       "restart"],
                       stdout=PIPE, stderr=PIPE)
            p.wait()
        
            if (0 != p.returncode):
                out = p.stdout.read()
                ret_result['err'].append("Network restart failed:"
                                         "%d: %s" % (p.returncode, out))

            # Restart the discover-ip since the controller-interface table has been modified
            ip = Popen(["/usr/sbin/service",
                        "discover-ip",
                        "restart"],
                        stdout=PIPE, stderr=PIPE)

            # not concerned with error messages from the requested command
            
        return ret_result

class DirectNetworkConfig(NetworkConfig):
    def __init__(self, name="direct_network_config"):
        NetworkConfig.__init__(self, name)

    def set(self, args_list):
        gateway = ''
        ip = ''
        netmask = ''
        domain_name = ''
        dns1 = ''
        dns2 = ''
        domain_lookups_enabled = False
        if args_list[0] == 'static':
            ip = args_list[1]
            netmask = args_list[2]
            gateway = args_list[3]
            if len(args_list) >= 5:
                domain_name = args_list[4]
                if len(args_list) >= 6:
                    domain_lookups_enabled = True
                    dns1 = args_list[5]
                    if len(args_list) >= 7:
                        dns2 = args_list[6]
        else:
            if len(args_list) >= 2:
                domain_name = args_list[1]
                if len(args_list) >= 3:
                    domain_lookups_enabled = True
                    dns1 = args_list[2]
                    if len(args_list) >= 4:
                        dns2 = args_list[3]

        controller_interface=[{'fields' : {'id' : "localhost|ethernet|0", 
                                          'type' : "ethernet", 'number' : 0, 
                                          'mode' : args_list[0], 'ip' : ip, 
                                          'netmask' : netmask, 'controller' : 'localhost'}}]
        nameservers = []
        if dns1 != '':
            nameservers.append({ 'fields' : {'controller' : "localhost", 'priority' : 1, 'ip' : dns1}})
            if dns2 != '':
                nameservers.append({ 'fields' : {'controller' : "localhost", 'priority' : 2, 'ip' : dns2}})
        controller=[{'fields' : {'id' : "localhost", 'domain_name' : domain_name,
                                 'default_gateway' : gateway, 
                                 'domain_lookups_enabled' : domain_lookups_enabled}, 'pk' : 'localhost'}]
        return NetworkConfig.set(self, [json.dumps(controller), json.dumps(nameservers), 
                              json.dumps(controller_interface)])               


class UfwCommand(OsWrapper):
    def __init__(self, name = "executeufwcommand"):
        OsWrapper.__init__(self, name, [], [])
    def set(self, arg_list):
        args = arg_list[0].split(" ")
        self.cmds_lst_for_set = [{'bin_name' : '/usr/sbin/ufw', 'args_lst' : args}]
        return OsWrapper.set_new(self, [], [[]])

NTP_CONF = """tinker panic 0
driftfile /var/lib/ntp/ntp.drift

# Enable this if you want statistics to be logged.
#statsdir /var/log/ntpstats/

statistics loopstats peerstats clockstats
filegen loopstats file loopstats type day enable
filegen peerstats file peerstats type day enable
filegen clockstats file clockstats type day enable

# Specify one or more NTP servers.

# Use servers from the NTP Pool Project. Approved by Ubuntu Technical Board
# on 2011-02-08 (LP: #104525). See http://www.pool.ntp.org/join.html for
# more information.
server %s

# Access control configuration; see /usr/share/doc/ntp-doc/html/accopt.html for
# details.  The web page <http://support.ntp.org/bin/view/Support/AccessRestrictions>
# might also be helpful.
#
# Note that "restrict" applies to both servers and clients, so a configuration
# that might be intended to block requests from certain clients could also end
# up blocking replies from your own upstream servers.

# By default, exchange time with everybody, but don't allow configuration.
restrict -4 default kod notrap nomodify nopeer noquery
restrict -6 default kod notrap nomodify nopeer noquery

# Local users may interrogate the ntp server more closely.
restrict 127.0.0.1
restrict ::1

# Clients from this (example!) subnet have unlimited access, but only if
# cryptographically authenticated.
#restrict 192.168.123.0 mask 255.255.255.0 notrust


# If you want to provide time to your local subnet, change the next line.
# (Again, the address is an example only.)
#broadcast 192.168.123.255

# If you want to listen to time broadcasts on your local subnet, de-comment the
# next lines.  Please do this only if you trust everybody on the network!
#disable auth
#broadcastclient
"""

class SetNtpServer(OsWrapper):
    def __init__(self, name = "setntpserver"):
        OsWrapper.__init__(self, name, [], [])
    def set(self, args_list):
        ret_result = {'err': [], 'out': []}
        server = "127.127.1.0"
        if (len(args_list) > 0 and 
            args_list[0] is not None and 
            args_list[0] != ""):
            server = str(args_list[0])
        ntpconf = NTP_CONF % server
        changed = False

        f = open("/etc/ntp.conf", "r")
        if (''.join(ntpconf) != f.read()):
            f.close()
            f = open("/etc/ntp.conf", "w")
            f.write(''.join(ntpconf))
            changed = True

        if changed:
            ntp = Popen(["/usr/sbin/service",
                        "ntp",
                        "restart"],
                        stdout=PIPE, stderr=PIPE)
        return ret_result


class SetTimezone(OsWrapper):
    def __init__(self, name = "settimezone"):
        OsWrapper.__init__(self, name, [], [])
    def set(self, args_list):
        self.cmds_lst_for_set = [
            {'bin_name' : '/bin/bash',
             'args_lst' : ['-c', 'echo "%s" >/etc/timezone' % (str(args_list[0]), )]},
            {'bin_name' : '/bin/bash',
             'args_lst' : ['-c', '/usr/sbin/dpkg-reconfigure -f noninteractive tzdata 2>&1']},
            {'bin_name' : '/sbin/initctl',
             'args_lst' : ['restart', 'rsyslog', ]},
        ]
        return OsWrapper.set_new(self, [], [[], [], []])


class UnsetTimezone(OsWrapper):
    def __init__(self, name = "unsettimezone"):
        OsWrapper.__init__(self, name, [], [])
    def set(self, args_list):
        self.cmds_lst_for_set = [
            {'bin_name' : '/bin/bash',
             'args_lst' : ['-c', 'echo "Etc/UTC" >/etc/timezone']},
            {'bin_name' : '/bin/bash',
             'args_lst' : ['-c', '/usr/sbin/dpkg-reconfigure -f noninteractive tzdata 2>&1']},
        ]
        return OsWrapper.set_new(self, [], [[], []])

class SetSyslogServer(OsWrapper):
    def __init__(self, name = "setsyslogserver"):
        OsWrapper.__init__(self, name, [], [])
    def set(self, args_list):
        self.cmds_lst_for_set = [
            {'bin_name' : '/bin/sed',
             'args_lst' : ['-i', '/^*.* @/d', '/etc/rsyslog.conf']},
            {'bin_name' : '/bin/echo',
             'args_lst' : ['*.' + str(args_list[1]) + ' @' + str(args_list[0])], 'stdoutfile' : '/etc/rsyslog.conf'},
            {'bin_name' : '/sbin/initctl',
             'args_lst' : ['restart', 'rsyslog']},
        ]
        return OsWrapper.set_new(self, [], [[], [], []])

class UnsetSyslogServer(OsWrapper):
    def __init__(self, name = "unsetsyslogserver"):
        OsWrapper.__init__(self, name, [], [])
    def set(self, args_list):
        self.cmds_lst_for_set = [
            {'bin_name' : '/bin/sed',
             'args_lst' : ['-i', '/^*./d', '/etc/rsyslog.conf']},
            {'bin_name' : '/sbin/initctl',
             'args_lst' : ['restart', 'rsyslog']}
        ]
        return OsWrapper.set_new(self, [], [[], []])

class DateTime(OsWrapper):
    def __init__(self, name = "getdatetime"):
        OsWrapper.__init__(self, name, [], [])
    def set(self, args_list):
        # If we're getting or setting the clock then the first argument
        # should be either 'utc' or 'local' to indicate whether or not to
        # use local time.
        # If we're setting the clock there's a second argument which is the
        # time to set, formatted as shown in set_cmd_args below.
        set_parts = args_list[1].split(':')
        set_param = '%2.2s%2.2s%2.2s%2.2s%4.4s.%2.2s' % (
                        set_parts[1],
                        set_parts[2],
                        set_parts[3],
                        set_parts[4],
                        set_parts[0],
                        set_parts[5],
                        )
        cmd_args = [str(set_param)]
        if str(args_list[0]).lower() == 'utc':
            cmd_args.insert(0, '-u')
        #date_info = args_list[0]
        #is_utc = args_list[1]
        #date_str = "%s:%s:%s:%s:%s:%s:%s"
        self.cmds_lst_for_set = [
            {'bin_name' : '/bin/date', 'args_lst' : cmd_args},
        ]
        OsWrapper.set_new(self, [], [[]])
        return self.get(args_list)
    
    def get(self, args_list):
        cmd_args = ['+%Y:%m:%d:%H:%M:%S:%Z']
        if str(args_list[0]).lower() == 'utc':
            cmd_args.insert(0, '-u')
        self.cmds_lst_for_get = [
            {'bin_name' : '/bin/date', 'args_lst' : cmd_args},
        ]
        return OsWrapper.get_new(self, [], [[]])
        
class SetControllerId(OsWrapper):
    def __init__(self, name = "setcontrollerid"):
        OsWrapper.__init__(self, name, [], [])
    def set(self, args_list):
        self.cmds_lst_for_set = [
            {'bin_name' : '/bin/sed',
             'args_lst' : ['-i', '/^controller-id=/d', "%s/run/boot-config" % SDN_ROOT]},
            {'bin_name' : '/bin/bash',
             'args_lst' : ['-c', 'echo "controller-id=%s" >> %s/run/boot-config' % (
                                str(args_list[0]), SDN_ROOT)
                          ]
             },
        ]
        return OsWrapper.set_new(self, [], [[], []])

class HAFailback(OsWrapper):
    def __init__(self, name = "hafailback"):
        OsWrapper.__init__(self, name, [], [])
    def set(self, args_list):
        self.cmds_lst_for_set = [
            {'bin_name' : '/bin/touch',
             'args_lst' : ["%s/force-one-time-health-check-failure" % SDN_ROOT]},
        ]
        return OsWrapper.set_new(self, [], [[]])

#class HAFailover(OsWrapper):
#    def __init__(self, name = "hafailover"):
#        OsWrapper.__init__(self, name, [], [])
#    def set(self, args_list):
#        self.cmds_lst_for_set = [
#            {'bin_name' : '/bin/bash',
#             'args_lst' : ["%s/sys/bin/ha-failover.sh" % SDN_ROOT, str(args_list[0])]},
#        ]
#        return OsWrapper.set_new(self, [], [[]])

class SetVrrpVirtualRouterId(OsWrapper):
    def __init__(self, name = "setvrrpvirtualrouterid"):
        OsWrapper.__init__(self, name, [], [])
    def set(self, args_list):
        self.cmds_lst_for_set = [
            {'bin_name' : '/bin/bash',
             'args_lst' : ["%s/sys/bin/set-vrrp-virtual-router-id.sh" % SDN_ROOT, str(args_list[0])]},
        ]
        return OsWrapper.set_new(self, [], [[]])


def write_controller_restarted():
    """
    Write the controller started file with the current timestamp + 5 seconds. This will
    ensure that health check script ignores the state of sdnplatform for 5 seconds
    from current time
    """
    f = open("/var/run/sdnplatform-healthcheck-disabled", "w")
    # write time converted to int and then string
    f.write(str(5 + long(time.time())))
    f.close()


class SetStaticFlowOnlyConfig(OsWrapper):
    def __init__(self, name = "setstaticflowonlyconfig"):
        OsWrapper.__init__(self, name, [], [])
    def set(self, args_list):

        # write the controller started file. do it before actually resetting
        # so that there is no race condition with health check script
        try:
            write_controller_restarted()
        except Exception, _e:
            traceback.print_exc()

        self.cmds_lst_for_set = [
            {'bin_name' : '/bin/touch',
             'args_lst' : ["%s/feature/staticflowonlyconfig" % SDN_ROOT]},
            {'bin_name' : '/sbin/initctl',
             'args_lst' : ['restart', 'sdnplatform']},
        ]
        return OsWrapper.set_new(self, [], [[], [], []])

class RestartSDNPlatform(OsWrapper):
    def __init__(self, name = "restartsdnplatform"):
        OsWrapper.__init__(self, name, [], [])
    def set(self, args_list):

        # write the controller started file. do it before actually resetting
        # so that there is no race condition with health check script
        try:
            write_controller_restarted()
        except Exception, _e:
            traceback.print_exc()

        self.cmds_lst_for_set = [
            {'bin_name' : '/sbin/initctl',
             'args_lst' : ['restart', 'sdnplatform']},
        ]
        return OsWrapper.set_new(self, [], [[]])

class AbortUpgrade(OsWrapper):
    def __init__(self, name = "abortupgrade"):
        OsWrapper.__init__(self, name, [], [])
    def set(self, args_list):

        self.cmds_lst_for_set = [
            {'bin_name' : '/opt/sdnplatform/sys/bin/abort_upgrade.sh',
             'args_lst' : []},
        ]
        return OsWrapper.set_new(self, [], [[]])

class SetDefaultConfig(OsWrapper):
    def __init__(self, name = "setdefaultconfig"):
        OsWrapper.__init__(self, name, [], [])
    def set(self, args_list):

        # write the controller started file. do it before actually resetting
        # so that there is no race condition with health check script
        try:
            write_controller_restarted()
        except Exception, _e:
            traceback.print_exc()

        self.cmds_lst_for_set = [
            {'bin_name' : '/bin/rm',
             'args_lst' : ['-f', "%s/feature/staticflowonlyconfig" % SDN_ROOT]},
            {'bin_name' : '/sbin/initctl',
             'args_lst' : ['restart', 'sdnplatform']},
            # if there are other configs their flag files need to be deleted here too
        ]
        return OsWrapper.set_new(self, [], [[], [], []])

class SetHostname(OsWrapper):
    def __init__(self, name = "sethostname"):
        OsWrapper.__init__(self, name, [], [])
    def set(self, args_list):
        hostname = str(args_list[0])
        self.cmds_lst_for_set = [
            # replace hostname from /etc/hosts
            {'bin_name' : '/bin/sed',
             'args_lst' : ['-i',
                           r's/^127\.0\.1\.1 .*$$/127.0.1.1 %s/' % hostname,
                           '/etc/hosts']},
            # populate /etc/hosts
            {'bin_name' : '/bin/bash',
             'args_lst' : ['-c', 'echo "%s" >/etc/hostname' % hostname]},
            # tell the system about the hostname
            {'bin_name' : '/bin/hostname',
             'args_lst' : ['-b', '-F', '/etc/hostname'] },
        ]
        return OsWrapper.set_new(self, [], [[], [], [],])

# In PAM, "auth" == authentication (surprise!)
# XXX roth -- maybe use PAP instead?

# 'sufficient' --> tacacs+ and local authentication are enabled
AUTHN_TPL = """\
auth [default=1 success=ignore] pam_succeed_if.so uid >= 10000
auth sufficient pam_tacplus.so %(servers)s %(secrets)s %(timeout)s service=login protocol=ip login=login
"""

# In PAM, "account" == authorization
AUTHZ_TPL = """\
account [default=1 success=ignore] pam_succeed_if.so uid >= 10000
account sufficient pam_tacplus.so %(secrets)s %(timeout)s service=login service_av=shell protocol=ip login=login
"""

# In PAM, "session" == accounting
ACCT_TPL = """\
session sufficient pam_tacplus.so %(servers)s %(secrets)s %(timeout)s service=login service_av=shell protocol=ip
"""

# XXX roth -- keep this in sync with the current release
# of /etc/pam.d/sshd
SSHD_TACPLUS_TPL = """\
# PAM configuration for the Secure Shell service

# Read environment variables from /etc/environment and
# /etc/security/pam_env.conf.
auth       required     pam_env.so # [1]
# In Debian 4.0 (etch), locale-related environment variables were moved to
# /etc/default/locale, so read that as well.
auth       required     pam_env.so envfile=/etc/default/locale

# Standard Un*x authentication.
%(authn)s

# Disallow non-root logins when /etc/nologin exists.
account    required     pam_nologin.so

# Uncomment and edit /etc/security/access.conf if you need to set complex
# access limits that are hard to express in sshd_config.
# account  required     pam_access.so

# Standard Un*x authorization.
%(authz)s

# Standard Un*x session setup and teardown.
%(acct)s

# Print the message of the day upon successful login.
session    optional     pam_motd.so # [1]

# Print the status of the user's mailbox upon successful login.
session    optional     pam_mail.so standard noenv # [1]

# Set up user limits from /etc/security/limits.conf.
session    required     pam_limits.so

# Set up SELinux capabilities (need modified pam)
# session  required     pam_selinux.so multiple

# Standard Un*x password updating.
@include common-password
"""

class TacacsPlusConfig(OsWrapper):

    def __init__(self, name="tacacs_plus_config"):
        OsWrapper.__init__(self, name, [], [])
        self.config = {}
        self.hosts = []
        self.result = dict(err=[], out=[])

    def isEnabled(self):
        """Is TACACS+ enabled?

        If any of authn/authz/acct is set, *and* there is a non-empty
        set of TACACS+ hosts, then we should enable the PAM plugin.
        """

        if (self.hosts 
            and (self.config['fields']['tacacs_plus_authn']
                 or self.config['fields']['tacacs_plus_authz']
                 or self.config['fields']['tacacs_plus_acct'])):
            return True

        return False

    def disablePamDefault(self):
        """Disable the default PAM setup.

        This is installed by the initial configuration scripts
        for the libpam-tacplus DEB.
        """

        if not os.path.exists("/usr/share/pam-configs/tacplus"):
            return

        cmd = ("/usr/sbin/pam-auth-update", "--remove", "tacplus",)
        pipe = Popen(cmd, stdout=PIPE, stderr=PIPE)
        out, err = pipe.communicate()
        code = pipe.wait()

        out = (out or "").strip().split("\n")
        err = (err or "").strip().split("\n")

        if not code:
            self.result['out'].append("disabled tacplus via pam-auth-update\n")
        else:
            self.result['out'].extend([l + "\n" for l in out])
            self.result['err'].extend([l + "\n" for l in err])
            self.result['err'].append("pam-auth-update failed\n")

    def disablePam(self):
        """Disable the TACACS+ PAM plugin."""
        m = dict(authn="@include common-auth",
                 authz="@include common-account",
                 acct="@include common-session")
        self.writeLocked("/etc/pam.d/sshd", SSHD_TACPLUS_TPL % m)

    def readLocked(self, path):

        fd = open(path, "r")

        fcntl.lockf(fd, fcntl.LOCK_SH)
        try:
            buf = fd.read()
        except Exception, what:
            fcntl.lockf(fd, fcntl.LOCK_UN)
            fd.close()
            self.result['err'].append(str(what) + "\n")
            self.result['err'].append("cannot read %s\n" % path)
            return None
        fcntl.lockf(fd, fcntl.LOCK_UN)

        fd.close()

        return buf

    def writeLocked(self, path, buf, backup=True):
        
        if backup:
            shutil.copy2(path, path + "-")

        fd = open(path, "w")

        fcntl.lockf(fd, fcntl.LOCK_EX)
        try:
            fd.write(buf)
        except Exception, what:
            fcntl.lockf(fd, fcntl.LOCK_UN)
            fd.close()
            self.result['err'].append(str(what) + "\n")
            self.result['err'].append("cannot write %s\n" % path)
            return
        fcntl.lockf(fd, fcntl.LOCK_UN)

        fd.close()

    def enableNss(self):
        """Enable the NSS plugin."""
        
        buf = self.readLocked("/etc/nsswitch.conf")
        if buf is None: return

        p = buf.find("\npasswd:")
        q = buf.find("\n", p+8)
        if p < 0 or q < 0:
            self.result['err'].append("cannot find passwd entry"
                                      " in /etc/nsswitch.conf\n")
            return

        f = buf[p+8:q]
        if "remoteuser" in f:
            self.result['out'].append("remoteuser already enabled"
                                      " in /etc/nssswitch.conf\n")
            return

        self.result['out'].append("enabling remoteuser"
                                  " in /etc/nssswitch.conf\n")
        f = " " + f.strip() + " remoteuser"
        buf = buf[:p+8] + f + buf[q:]

        self.writeLocked("/etc/nsswitch.conf", buf)

    def disableNss(self):
        """Disable the NSS plugin."""

        buf = self.readLocked("/etc/nsswitch.conf")
        if buf is None: return
        
        p = buf.find("\npasswd:")
        q = buf.find("\n", p+8)
        if p < 0 or q < 0:
            self.result['err'].append("cannot find passwd entry"
                                      " in /etc/nsswitch.conf\n")
            return

        l = buf[p+8:q].strip().split()
        if "remoteuser" not in l:
            self.result['out'].append("remoteuser already disabled"
                                      " in /etc/nssswitch.conf\n")
            return

        self.result['out'].append("disabling remoteuser"
                                  " in /etc/nssswitch.conf\n")
        l.remove("remoteuser")
        f = " " + " ".join(l)
        buf = buf[:p+8] + f + buf[q:]

        self.writeLocked("/etc/nsswitch.conf", buf)

    def enablePam(self):
        """Enable the TACACS+ PAM plugin.

        * construct consolidate server and secret lists
        * generate a timeout line
        * pick sufficient/required clauses as indicated by enable
          flags in the JSON

        See
        http://tacplus.git.sourceforge.net/git/gitweb.cgi?p=tacplus/tacplus;a=blob_plain;f=README;hb=HEAD
        """
        
        # disable TACACS+ while updating
        self.disableNss()
        self.disablePam()

        # XXX roth -- field is 'ip', but since it is the primary key,
        # it get mapped to 'pk'.  Go figure.
        def svrClause(h):
            self.result['out'].append("enabling host %s\n" % h['pk'])
            return 'server=%s' % h['pk']
        
        servers = " ".join([svrClause(h) for h in self.hosts])

        def keyClause(h):
            """Secret for this host, possibly the global one."""
            if h['fields']['key']:
                return "secret=%s" % h['fields']['key']
            if self.config['fields']['key']:
                return "secret=%s" % self.config['fields']['key']
            return ""

        secrets = " ".join([keyClause(h) for h in self.hosts])

        m = dict(servers=servers, secrets=secrets)

        if self.config['fields']['timeout']:
            m['timeout'] = 'timeout=%s' % self.config['fields']['timeout']
        else:
            m['timeout'] = ''

        isLocal = self.config['fields']['local_authn']
        isTacacs = self.config['fields']['tacacs_plus_authn']

        authn = []
        if isTacacs:
            authn.append(AUTHN_TPL % m)
        if isLocal:
            authn.append("@include common-auth")

        isLocal = self.config['fields']['local_authz']
        isTacacs = self.config['fields']['tacacs_plus_authz']

        authz = []
        if isTacacs:
            authz.append(AUTHZ_TPL % m)
        if isLocal:
            authz.append("@include common-account")

        isTacacs = self.config['fields']['tacacs_plus_acct']

        acct = []
        if isTacacs:
            acct.append(ACCT_TPL % m)
        acct.append("@include common-session")

        # enable userid lookups
        self.enableNss()

        # write out sshd PAM config as a final step to enable it
        m = dict(authn="\n".join(authn),
                 authz="\n".join(authz),
                 acct="\n".join(acct))
        self.writeLocked("/etc/pam.d/sshd", SSHD_TACPLUS_TPL % m)

    def set(self, args_list):

        self.config = json.loads(args_list[0])[0]
        self.hosts = json.loads(args_list[1])

        self.disablePamDefault()

        if not self.isEnabled():
            try:
                self.disableNss()
                self.disablePam()
                self.result['out'].append('TACACS+ (via PAM) is now disabled\n')
            except Exception:
                traceback.print_exc()
                self.result['err'].append('TACACS+ (via PAM) disable failed\n')
            return self.result

        # else, enable the PAM module
        try:
            self.enableNss()
            self.enablePam()
            self.result['out'].append('TACACS+ (via PAM) is now enabled\n')
        except Exception:
            # XXX roth -- maybe back out here and *disable* PAM
            # so that we do not end up with a broken PAM config
            traceback.print_exc()
            self.result['err'].append('TACACS+ (via PAM) enable failed\n')

        return self.result

#
# get_system_version_string
#
# Gets the version string of the controller.
# Reference implementation is in sdncon/rest/views/do_system_version
#
def get_system_version_string():
    version = "SDN OS 1.0 - custom version"
    try:
        f = open("%s/release" % SDN_ROOT, 'r')
        version = f.read()
        f.close()
    except:
        pass
    return version

#
# rewrite_etc_snmpd_conf
#
# API to rewrite the /etc/snmp/snmpd.conf file based on latest config
#
def rewrite_etc_snmpd_conf(community, location, contact, ret_result):
    """
    Return True when the /etc/snmp/snmpd.conf is rewritten. Return False 
    otherwise. The file is rewritten only when the intended new contents
    is different from the old contents, this is an attempt to not restart
    the snmp agent unless something really changed.
    """

    changed = False
    new_conf = []
    # start with default configuration of the file
    new_conf.append("# Default Configuration for the SNMP daemon\n")
    new_conf.append("# Agent address\n")
    new_conf.append("agentAddress udp:161,udp6:[::1]:161\n")
    new_conf.append("# System Object ID\n")
    new_conf.append("sysObjectID %s\n" % (BSN_ENTERPRISE_OID_CONTROLLER))
    new_conf.append("# System Description\n")
    new_conf.append("sysDescr %s\n"%(get_system_version_string()))

    #add community, location, contact information to the file if not there already
    if community != '':
        new_conf.append("rocommunity %s\n" % community)
    if location != '': 
        new_conf.append("sysLocation %s\n" % location)
    if contact != '': 
        new_conf.append("sysContact %s\n" % contact)

    f = open("/etc/snmp/snmpd.conf", "r")
    if (''.join(new_conf) != f.read()):
        f.close()
        f = open("/etc/snmp/snmpd.conf", "w")
        f.write(''.join(new_conf))
        changed = True

    f.close()

    return changed

#
# One of the entry in the snmp server configuration changed
#
class SetSnmpServerConfig(OsWrapper):
    def __init__(self, name = "setsnmpserverconfig"):
        OsWrapper.__init__(self, name, [], [])


    def set(self, args_list):
        # args_list: [server_enable, community, location, contact, enable_changed]
        print "SnmpServerConfig Args List: ", args_list
        server_enable = args_list[0]
        community = args_list[1]
        location = args_list[2]
        contact = args_list[3]
        enable_changed = args_list[4]

        ret_result = {'err': [], 'out': []}

        try:
            # rewrite /etc/snmp/snmpd.conf file
            need_restart = rewrite_etc_snmpd_conf(community, location, contact, ret_result)
        except Exception, _e:
            need_restart = False
            traceback.print_exc()

        if server_enable == 'True' and (need_restart or enable_changed == 'True'):
            self.cmds_lst_for_set = [
                # set snmpdrun=yes
                {'bin_name' : '/bin/sed',
                 'args_lst' : ['-i', 's/SNMPDRUN=no/SNMPDRUN=yes/',
                               '/etc/default/snmpd']},
                # restart snmpd service 
                {'bin_name' : '/usr/sbin/service',
                 'args_lst' : ['snmpd', 'restart']},
            ]
            return OsWrapper.set_new(self, [], [[], []])

        elif server_enable == 'False' and enable_changed == 'True':
            self.cmds_lst_for_set = [
                # set snmpdrun=no
                {'bin_name' : '/bin/sed',
                 'args_lst' : ['-i', 's/SNMPDRUN=yes/SNMPDRUN=no/',
                               '/etc/default/snmpd']},
                # stop snmpd service 
                {'bin_name' : '/usr/sbin/service',
                 'args_lst' : ['snmpd', 'stop']},
            ]
            return OsWrapper.set_new(self, [], [[], []])

        return ret_result


#
# The row entry in the snmp server config table was default and deleted
#
class UnsetSnmpServerConfig(OsWrapper):
    def __init__(self, name = "unsetsnmpserverconfig"):
        OsWrapper.__init__(self, name, [], [])

    def set(self, args_list):
        # args_list: []
        ret_result = {'err': [], 'out': []}

        try:
            # rewrite /etc/snmp/snmpd.conf to default
            rewrite_etc_snmpd_conf('', '', '', ret_result)
        except Exception, _e:
            traceback.print_exc()

        # now stop the server if its there
        self.cmds_lst_for_set = [
            # set snmpdrun=no
            {'bin_name' : '/bin/sed',
             'args_lst' : ['-i', 's/SNMPDRUN=yes/SNMPDRUN=no/',
                           '/etc/default/snmpd']},
            # stop snmpd service 
            {'bin_name' : '/usr/sbin/service',
             'args_lst' : ['snmpd', 'stop']},
        ]
        return OsWrapper.set_new(self, [], [[], []])

class SetImagesUserSSHKey(OsWrapper):
    def __init__(self, name = 'setimagesusersshkey'):
        OsWrapper.__init__(self, name, [], [])

    def set(self, args_list):
        sshkey = str(args_list[0]) 
        # cat the ssh key to the file
        # set the images user shell to be scponly
        self.cmds_lst_for_set = [
            {'bin_name' : '/usr/sbin/usermod',
             'args_lst' : ['-s', '/usr/bin/scponly', 'images']},
            {'bin_name' : '/bin/echo',
             'args_lst' : [sshkey], 
             'stdoutfile' : '/home/images/.ssh/authorized_keys'},
        ]
        return OsWrapper.set_new(self, [], [[], []])

class ReloadController(OsWrapper):
    def __init__(self, name = 'reloadcontroller'):
        OsWrapper.__init__(self, name, [], [])

    def set(self, args_list):
        self.cmds_lst_for_set = [
            {'bin_name' : '/sbin/reboot',
             'args_list' : []},
        ]
        return OsWrapper.set_new(self, [], [[]])

UPGRADE_IMAGE_FILE_PATH = '/tmp/upgrade-images'
UPGRADE_IMAGE_MANIFEST = '/tmp/upgrade-image-manifest'
UPGRADE_PACKAGE_DIRECTORY = '/tmp/upgrade-package/'

class ExtractUpgradePkgManifest(OsWrapper):
    def __init__(self, name = 'extractupgradepkg'):
        OsWrapper.__init__(self, name, [], [])

    def set(self, args_list):
        imageName = str(args_list[0])
        self.cmds_lst_for_set = [
            {'bin_name' : '/bin/rm',
             'args_lst' : [UPGRADE_IMAGE_MANIFEST]},
            {'bin_name' : '/bin/touch',
             'args_lst' : [UPGRADE_IMAGE_MANIFEST]},
            {'bin_name' : '/usr/bin/unzip',
             'args_lst' : ['-p', imageName, 'Manifest'],
             'stdoutfile' : UPGRADE_IMAGE_MANIFEST},
        ]
        return OsWrapper.set_new(self, [], [[], [], []], useShell=False, appendStdOut=False)

    def get(self, args_list):
        self.cmds_lst_for_get = [
            {'bin_name' : '/bin/cat',
             'args_lst': [UPGRADE_IMAGE_MANIFEST]},
        ]
        return OsWrapper.get_new(self, [], [[]])

class GetLatestUpgradePkg(OsWrapper):
    def __init__(self, name = 'getlatestupgradepkg'):
        OsWrapper.__init__(self, name, [], [])

    # TODO -
    # This is ghetto, it just finds the last zip
    # file in the dir. FIX THIS!
    def get(self, args_list):
        execStr = 'ls -t /home/images/*.pkg | grep pkg | head -1 > ' + UPGRADE_IMAGE_FILE_PATH
        self.cmds_lst_for_get = [
            {'bin_name' : execStr, 
             'args_lst' : []},
        ]
        return OsWrapper.get_new(self, [], [[]], useShell=True)

class CatUpgradeImagesFile(OsWrapper):
    def __init__(self, name = 'catupgradeimagesfile'):
        OsWrapper.__init__(self, name, [], [])

    def get(self, args_list):
        self.cmds_lst_for_get = [
            {'bin_name' : '/bin/cat',
             'args_lst' : [UPGRADE_IMAGE_FILE_PATH]},
        ]
        return OsWrapper.get_new(self, [], [[]])

class ExecuteUpgradeStep(OsWrapper):
    def __init__(self, name = 'executeupgradestep'):
        OsWrapper.__init__(self, name, [], [])

    def get(self, args_list):
        ret_result = {'err': [], 'out': []}
        
        try:
            manifest = json.loads(exec_os_wrapper("ExtractUpgradePkgManifest", 'get')['out'])
        except ValueError:
            ret_result['err'].append("Corrupted manifest!")
            return ret_result

        stepToExec = None
        for step in manifest:
            if step['step'] == int(args_list[0]):
                stepToExec = step['action']
                break;

        if stepToExec == None:
            ret_result['err'].append("Step %s not found in upgrade package manifest!" %
                str(args_list[0]))
            return ret_result
        
        upgradePkg = args_list[1]
        stepScript = tempfile.NamedTemporaryFile(delete=False)
        scriptName = "scripts/%s" % step['action'].strip()
        step = check_output(["unzip", "-p", upgradePkg, scriptName])
        stepScript.write(step)
        stepScript.flush()
        stepScript.close()
        os.chmod(stepScript.name, stat.S_IXUSR | stat.S_IWUSR | stat.S_IRUSR)

        try:
            ret = check_output([stepScript.name] + args_list[1:],
                               stderr=PIPE)
            ret_result['out'].append(stripped(ret.strip()))
        except CalledProcessError, exception:
            ret_result['err'].append("Error running %s\nreturn code %s\nOutput:\n%s" %
                    (exception.cmd, exception.returncode, stripped(exception.output)))
        return ret_result

class CleanupOldUpgradeImages(OsWrapper):
    def __init__(self, name = 'cleanupoldupgradeimages'):
        OsWrapper.__init__(self, name, [], [])

    def get(self, args_list):
        # Removes all the .pkg files execpt for the newest one.
        # It handles the case where there is only 1 package, we don't delete it.
        execStr = 'c=`ls /home/images/*.pkg | wc -l`; if [ "$c" -gt 1 ]; then ls -t -r /home/images/*.pkg | head -n -1 | xargs rm; fi'
        self.cmds_lst_for_get = [
            {'bin_name' : execStr, 
             'args_lst' : []},
        ]
        return OsWrapper.get_new(self, [], [[]], useShell=True)

class Decommission(OsWrapper):
    def __init__(self, name = "decommission"):
        OsWrapper.__init__(self, name, [], [])
    def set(self, args_list):
        self.cmds_lst_for_set = [
            {'bin_name' : '/bin/bash',
             'args_lst' : ["%s/sys/bin/remove-node.sh" % SDN_ROOT, str(args_list[0])]},
        ]
        return OsWrapper.set_new(self, [], [[]])

class DecommissionLocal(OsWrapper):
    def __init__(self, name = "decommissionlocal"):
        OsWrapper.__init__(self, name, [], [])
    def set(self, args_list):
        self.cmds_lst_for_set = [
            {'bin_name' : '/bin/bash',
             'args_lst' : ["%s/sys/bin/remove-node-local.sh" % SDN_ROOT, str(args_list[0])]},
        ]
        return OsWrapper.set_new(self, [], [[]])

class ResetBsc(OsWrapper):
    def __init__(self, name = "resetbsc"):
        OsWrapper.__init__(self, name, [], [])
    def set(self, args_list):
        self.cmds_lst_for_set = [
            {'bin_name' : '/bin/bash',
             'args_lst' : ["%s/sys/bin/resetbsc" % SDN_ROOT, '--force']},
        ]
        return OsWrapper.set_new(self, [], [[]])

class WriteDataToFile(OsWrapper):
    def __init__(self, name = 'writedatatofile'):
        OsWrapper.__init__(self, name, [], [])

    def set(self, args_list):
        data = str(args_list[0])
        path = str(args_list[1])
        self.cmds_lst_for_set = [
            {'bin_name' : '/bin/touch',
             'args_lst' : [path]},
            {'bin_name' : '/bin/echo',
             'args_lst' : [data], 
             'stdoutfile' : path},
        ]
        return OsWrapper.set_new(self, [], [[], []])

class DiffConfig(OsWrapper):
    def __init__(self, name = "scpconfig"):
        OsWrapper.__init__(self, name, [], [])
    def set(self, args_list):
        self.cmds_lst_for_set = [
            {'bin_name' : '/opt/sdnplatform/sys/bin/diff_config.py',
             'args_lst' : [str(args_list[0]), str(args_list[1])]},
        ]
        return OsWrapper.set_new(self, [], [[]])

class RollbackConfig(OsWrapper):
    def __init__(self, name = "upgradeconfig"):
        OsWrapper.__init__(self, name, [], [])
    def set(self, args_list):
        self.cmds_lst_for_set = [
            {'bin_name' : '/bin/bash',
             'args_lst' : ["%s/sys/bin/rollback-config.sh" % SDN_ROOT, str(args_list[0])]},
        ]
        return OsWrapper.set_new(self, [], [[]])

def stripped(x):
    # remove ascii escape
    return "".join([i for i in x if ord(i) != 27])
#
# exec_os_wrapper
#
def exec_os_wrapper(obj_type, oper, args_list = None):
    """
    Execute the oswrapper.py using sudo(), raising an exception
    for any stderr output from the executed script
    """
    # Safety check; only run if this file exists
    if not os.path.exists("%s/con" % SDN_ROOT):
        print "exec_os_wrapper: not an installed controller environment"
        return {'out' : '', 'err' : ''}
    if os.path.exists('/etc/not-controller'):
        # XXX should issue some alert here
        print "exec_os_wrapper: /etc/not-controller exists"
        return {'out' : '', 'err' : ''}

    oswrapper = os.path.dirname(__file__) + "/oswrapper.py"
    full_cmd_string = ["/usr/bin/sudo", oswrapper, obj_type, oper]
    if args_list:
        full_cmd_string += [str(arg) for arg in args_list]

    sub_proc_output = Popen(full_cmd_string, shell=False,
                            stdin=PIPE, stdout=PIPE, stderr=PIPE, close_fds=True)

    sub_proc_output.wait()
    stderr = sub_proc_output.stderr.read()
    stdout = sub_proc_output.stdout.read()
    returncode = sub_proc_output.returncode

    if returncode:
        raise Exception("oswrapper: %s %s: exit code %d: %s" %
                        (obj_type, oper, returncode, stderr))
    
    if len(stderr) != 0 and not stderr.isspace():
        print " ".join(full_cmd_string), stderr

    # ?!?
    return {'out' : stdout, 'err' : stderr}


def main(argv):
    obj_type_map = {'ExecuteUfwCommand'       : UfwCommand,
                    'SetNtpServer'            : SetNtpServer,
                    'SetTimezone'             : SetTimezone,
                    'UnsetTimezone'           : UnsetTimezone,
                    'NetworkConfig'           : NetworkConfig,
                    'SetSyslogServer'         : SetSyslogServer,
                    'UnsetSyslogServer'       : UnsetSyslogServer,
                    'DateTime'                : DateTime,
                    'ControllerId'            : SetControllerId,
                    'HAFailback'              : HAFailback,
                    'SetHostname'             : SetHostname,
                    'SetVrrpVirtualRouterId'  : SetVrrpVirtualRouterId,
                    'SetStaticFlowOnlyConfig' : SetStaticFlowOnlyConfig,
                    'SetDefaultConfig'        : SetDefaultConfig,
                    'TacacsPlusConfig'        : TacacsPlusConfig,
                    'SetSnmpServerConfig'     : SetSnmpServerConfig,
                    'UnsetSnmpServerConfig'   : UnsetSnmpServerConfig,
                    'SetImagesUserSSHKey'     : SetImagesUserSSHKey,
                    'ReloadController'        : ReloadController,
                    'ExtractUpgradePkgManifest' : ExtractUpgradePkgManifest,
                    'GetLatestUpgradePkg'     : GetLatestUpgradePkg,
                    'CatUpgradeImagesFile'    : CatUpgradeImagesFile,
                    'ExecuteUpgradeStep'      : ExecuteUpgradeStep,
                    'DirectNetworkConfig'     : DirectNetworkConfig,
                    'CleanupOldUpgradeImages' : CleanupOldUpgradeImages,
                    'RestartSDNPlatform'       : RestartSDNPlatform,
                    'AbortUpgrade'            : AbortUpgrade,
                    'Decommission'            : Decommission,
                    'DecommissionLocal'       : DecommissionLocal,
                    'RollbackConfig'          : RollbackConfig, 
                    'DiffConfig'              : DiffConfig,
                    'ResetBsc'                : ResetBsc,
                    'WriteDataToFile'         : WriteDataToFile,
                    }
    ret_result = {'err': ["insufficient or invalid args"], 'out': []}
    if len(argv) >= 3:
        if argv[1] in obj_type_map:
            obj_type = obj_type_map[argv[1]]
            x = obj_type()
            if argv[2] == 'set':
                ret_result = x.set(argv[3:])
            elif argv[2] == 'get':
                ret_result = x.get(argv[3:])
    
    # The ret_result entries are lists of strings from the output's of
    # various commands.
    print >>sys.stdout, ''.join(ret_result['out'])
    print >>sys.stderr, ''.join(ret_result['err'])

if __name__ == '__main__':
    main(sys.argv)
