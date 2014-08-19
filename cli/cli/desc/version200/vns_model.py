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

command.model_obj_type_disable_submode('vns-interface-config')
command.model_obj_type_disable_submode('vns-access-list-entry')

command.model_obj_type_disable_submode('vns-interface-rule-network-service-node-mapping')

command.model_obj_type_disable_edit('vns-definition', 'description')
command.model_obj_type_disable_edit('vns-definition', 'origin')
command.model_obj_type_disable_edit('vns-definition', 'priority')
command.model_obj_type_disable_edit('vns-definition', 'vnsname')
command.model_obj_type_disable_edit('vns-definition', 'tenant')


command.model_obj_type_disable_edit('vns-access-list', 'priority')
command.model_obj_type_disable_edit('vns-access-list', 'description')

command.model_obj_type_set_show_this('vns-access-list', [
                ['vns-access-list', 'vns-access-list', 'default' ],
                ['vns-access-list-entry', 'vns-access-list-entry', 'scoped-acl-brief' ],
                        ])

command.model_obj_type_set_title('vns-access-list-entry', 'Access List Rules')
