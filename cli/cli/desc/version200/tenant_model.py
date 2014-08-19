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
command.model_obj_type_enable_cascade_delete('tenant')
command.model_obj_type_enable_cascade_delete('virtualrouter')
command.model_obj_type_weak_with_cascade_delete('virtualrouter-interface')
command.model_obj_type_weak_with_cascade_delete('virtualrouter-routingrule')
command.model_obj_type_enable_cascade_delete('interface-address-pool')
command.model_obj_type_weak_with_cascade_delete('virtualrouter-gwpool')
command.model_obj_type_enable_cascade_delete('gateway-address-pool')

command.model_obj_type_disable_submode('virtualrouter')
command.model_obj_type_disable_submode('virtualrouter-interface')
command.model_obj_type_disable_submode('virtualrouter-gwpool')
command.model_obj_type_disable_submode('virtualrouter-routingrule')
command.model_obj_type_disable_submode('interface-address-pool')
command.model_obj_type_disable_submode('gateway-address-pool')
command.model_obj_type_disable_submode('static-arp')
command.model_obj_type_disable_submode('tenant')

command.model_obj_type_disable_edit('tenant', 'active')
command.model_obj_type_disable_edit('tenant', 'description')
command.model_obj_type_disable_edit('tenant', 'origin')

