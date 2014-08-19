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

command.model_obj_type_disable_submode('host-security-ip-address')
command.model_obj_type_disable_submode('host-security-attachment-point')

command.model_obj_type_disable_edit('host', 'last-seen')

command.model_obj_type_set_case('host', 'mac', 'lower')
command.model_obj_type_set_case('host-config', 'mac', 'lower')

command.model_obj_type_enable_cascade_delete('host-config')
command.model_obj_type_enable_cascade_delete('host-alias')
command.model_obj_type_enable_cascade_delete('host-security-ip-address')
command.model_obj_type_enable_cascade_delete('host-security-attachment-point')

command.model_obj_type_disable_edit('host-config', 'vlan')
command.model_obj_type_disable_edit('host-config', 'last-seen')
