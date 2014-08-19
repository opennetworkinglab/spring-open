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

import command 

command.model_obj_type_disable_submode('flow-entry')

command.model_obj_type_enable_cascade_delete('flow-entry')

command.model_obj_type_disable_edit('flow-entry', 'hard-timeout')
command.model_obj_type_disable_edit('flow-entry', 'idle-timeout')
command.model_obj_type_disable_edit('flow-entry', 'actions')
command.model_obj_type_disable_edit('flow-entry', 'active')
command.model_obj_type_disable_edit('flow-entry', 'priority')
command.model_obj_type_disable_edit('flow-entry', 'cookie')
command.model_obj_type_disable_edit('flow-entry', 'wildcards')
command.model_obj_type_disable_edit('flow-entry', 'ingress-port')
command.model_obj_type_disable_edit('flow-entry', 'src-mac')
command.model_obj_type_disable_edit('flow-entry', 'dst-mac')
command.model_obj_type_disable_edit('flow-entry', 'vlan-id')
command.model_obj_type_disable_edit('flow-entry', 'vlan-priority')
command.model_obj_type_disable_edit('flow-entry', 'ether-type')
command.model_obj_type_disable_edit('flow-entry', 'tos-bits')
command.model_obj_type_disable_edit('flow-entry', 'protocol')
command.model_obj_type_disable_edit('flow-entry', 'src-ip')
command.model_obj_type_disable_edit('flow-entry', 'dst-ip')
command.model_obj_type_disable_edit('flow-entry', 'src-port')
command.model_obj_type_disable_edit('flow-entry', 'dst-port')


