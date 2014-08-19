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

command.model_obj_type_disable_edit('switch-config', 'tunnel-termination')
command.model_obj_type_disable_edit('switch-config', 'core-switch')
command.model_obj_type_disable_edit('switch-config', 'dpid')

command.model_obj_type_disable_submode('switch')

command.model_obj_type_set_case('switch',        'dpid', 'lower')
command.model_obj_type_set_case('switch-config', 'dpid', 'lower')

command.model_obj_type_set_case('switch-interface-config', 'name', 'lower')



# rest-to-model model-like results from the switches/devices rest api
command.model_obj_type_set_case('switches',   'id', 'lower')
command.model_obj_type_set_case('switches',   'dpid', 'lower')

command.model_obj_type_set_case('interfaces', 'id', 'lower')
command.model_obj_type_set_case('interfaces', 'name', 'lower')
