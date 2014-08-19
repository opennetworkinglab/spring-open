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

command.model_obj_type_enable_cascade_delete('controller-node')
command.model_obj_type_enable_cascade_delete('controller-interface')
command.model_obj_type_enable_cascade_delete('controller-domain-name-server')
command.model_obj_type_enable_cascade_delete('controller-alias')
command.model_obj_type_enable_cascade_delete('firewall-rule')

command.model_obj_type_disable_submode('controller-node')
command.model_obj_type_disable_submode('controller-interface')
command.model_obj_type_disable_submode('controller-domain-name-server')
command.model_obj_type_disable_submode('controller-alias')
command.model_obj_type_disable_submode('firewall-rule')

command.model_obj_type_disable_edit('controller-node', 'domain-lookups-enabled')
command.model_obj_type_disable_edit('controller-node', 'domain-name')
command.model_obj_type_disable_edit('controller-node', 'default-gateway')
command.model_obj_type_disable_edit('controller-node', 'ntp-server')
command.model_obj_type_disable_edit('controller-node', 'time-zone')
command.model_obj_type_disable_edit('controller-node', 'logging-enabled')
command.model_obj_type_disable_edit('controller-node', 'logging-server')
command.model_obj_type_disable_edit('controller-node', 'logging-level')

command.model_obj_type_disable_edit('controller-interface', 'type')
command.model_obj_type_disable_edit('controller-interface', 'number')
command.model_obj_type_disable_edit('controller-interface', 'ip')
command.model_obj_type_disable_edit('controller-interface', 'netmask')
command.model_obj_type_disable_edit('controller-interface', 'mac')
command.model_obj_type_disable_edit('controller-interface', 'discovered-ip')
command.model_obj_type_disable_edit('controller-interface', 'mode')
command.model_obj_type_disable_edit('controller-interface', 'status')

command.model_obj_type_disable_edit('controller-domain-name-server', 'ip')
command.model_obj_type_disable_edit('controller-domain-name-server', 'sequence-number')
command.model_obj_type_disable_edit('controller-domain-name-server', 'controller')

command.model_obj_type_disable_edit('firewall-rule', 'interface')
command.model_obj_type_disable_edit('firewall-rule', 'port')
command.model_obj_type_disable_edit('firewall-rule', 'proto')
command.model_obj_type_disable_edit('firewall-rule', 'rule')
