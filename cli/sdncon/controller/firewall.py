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

#from sdncon.controller.models import ControllerAclEntry

def map_controller_acl_entry_to_ufw_string(acl_entry, in_acl, interface=None, delete=False):
    # TODO optimize this method by building an array and then joining it
    command = "ufw "
    
    if delete:
        command += "delete "
        
    if acl_entry['action'] == "permit":
        command += "allow "
    else:
        command += "deny "
    
    if in_acl:
        command += "in "
    else:
        command += "out "
        
    command += ("on " + interface + " ")
    
    if acl_entry['type'] == 'ip':
        pass
    elif acl_entry['type'] == 'tcp' or acl_entry['type'] == 'udp':
        command += ("proto " + acl_entry['type'] + " from ")
        if acl_entry['src_ip'] != None: # TODO check none
            command += acl_entry['src_ip']
            if acl_entry['src_ip_mask'] != None:
                command += ("/" + acl_entry['src_ip_mask'] + " ")
            else:
                command += " "
        else:
            command += "any "
            
        if acl_entry['src_tp_port_op'] == 'eq':
            command += ("port " + acl_entry['src_tp_port'] + " ")

        command += "to "
        if acl_entry['dst_ip'] != None: #TODO check none
            command += acl_entry['dst_ip']
            if acl_entry['dst_ip_mask'] != None:
                command += ("/" + acl_entry['dst_ip_mask'] + " ")
            else:
                command += " "
        else:
            command += "any "
            
        if acl_entry['dst_tp_port_op'] == 'eq':
            command += ("port " + acl_entry['dst_tp_port'] + " ")
    return command
    
        
