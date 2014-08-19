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

command.model_obj_type_enable_cascade_delete('address-space')
command.model_obj_type_enable_cascade_delete('address-space-identifier-rule')

command.model_obj_type_disable_submode('address-space')
command.model_obj_type_disable_submode('address-space-identifier-rule')

command.model_obj_type_disable_edit('address-space', 'active')
command.model_obj_type_disable_edit('address-space', 'description')
command.model_obj_type_disable_edit('address-space', 'priority')
command.model_obj_type_disable_edit('address-space', 'vlan-tag-on-egress')
command.model_obj_type_disable_edit('address-space', 'origin')

command.model_obj_type_disable_edit('address-space-identifier-rule', 'rule')
command.model_obj_type_disable_edit('address-space-identifier-rule', 'active')
command.model_obj_type_disable_edit('address-space-identifier-rule', 'ports')
command.model_obj_type_disable_edit('address-space-identifier-rule', 'mac')
command.model_obj_type_disable_edit('address-space-identifier-rule', 'priority')
command.model_obj_type_disable_edit('address-space-identifier-rule', 'switch')
command.model_obj_type_disable_edit('address-space-identifier-rule', 'tag')
command.model_obj_type_disable_edit('address-space-identifier-rule', 'vlans')
