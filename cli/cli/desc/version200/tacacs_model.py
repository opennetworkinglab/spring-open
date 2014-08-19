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

command.model_obj_type_disable_submode('tacacs-plus-config')
command.model_obj_type_disable_submode('tacacs-plus-host')

command.model_obj_type_disable_edit('tacacs-plus-config', 'tacacs-plus-authn')
command.model_obj_type_disable_edit('tacacs-plus-config', 'tacacs-plus-authz')
command.model_obj_type_disable_edit('tacacs-plus-config', 'tacacs-plus-acct')
command.model_obj_type_disable_edit('tacacs-plus-config', 'local-authn')
command.model_obj_type_disable_edit('tacacs-plus-config', 'local-authz')
command.model_obj_type_disable_edit('tacacs-plus-config', 'timeout')
command.model_obj_type_disable_edit('tacacs-plus-config', 'key')

command.model_obj_type_disable_edit('tacacs-plus-host', 'ip')
command.model_obj_type_disable_edit('tacacs-plus-host', 'key')
