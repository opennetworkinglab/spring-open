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

# This i data file for testing Rest APIs
# This file is read by RestApiTests.py

test_dpid="11:22:33:44:55:66:77:01"
test_dpid2="11:22:33:44:55:66:77:01"
test_mac ="11:22:33:44:55:01"
test_vns_id   ="test_vns_id_1"
test_flow_entry_name = "test_flow_entry_name"
test_vns_acl_name = "test_vns_acl"
test_syncd_config_id = "test_syncd_config_id"
test_syncd_transport_config_test_data="test_sync_transport_config_id"
test_syncd_transport_config_name = "test_syncd_transport_cfg_name"
test_vns_interface_rule_name = "test_vns_intf_rule_name"
test_vns_interface_name = "test_vns_intf_name"
test_vns_interface_acl_direction = "in"
test_vns_interface_acl_id = "test_vns_interface_acl_id"
test_vns_acl_name_id = test_vns_id+"|"+test_vns_acl_name
test_vns_acl_entry_type = "ip"
test_acl_entry_seq_no = "10"
test_vns_acl_entry_id = test_vns_acl_name_id+"|"+test_acl_entry_seq_no
test_vns_acl_entry_src_ip="11.22.33.44"
test_vns_acl_entry_action="deny"
test_vns_acl_entry_type="deny"
test_tag_name = "test_tag_name"
test_tag_value = "test_tag_value"
test_tag_id = test_vns_id+"|"+test_tag_name+"|"+test_tag_value
test_tag_mapping_id = test_tag_id+"|"+test_mac

model_switch_test_data={"path":"model/switch", "data":{"dpid":test_dpid}, "id":"dpid"}
model_host_test_data  ={"path":"model/host",   "data":{"mac":test_mac}, "id":"mac"}
model_flow_entry_test_data = {"path":"model/flow-entry", "data":{"switch":test_dpid, "name":test_flow_entry_name}, "id":"name"}
model_vns_definition_test_data = {"path":"model/vns-definition", "data":{"id":test_vns_id}, "id":"id"}
model_vns_acl_test_data = {"path":"model/vns-access-list", "data":{"vns":test_vns_id, "name":test_vns_acl_name_id}, "id":"name"}
model_host_network_address_test_data = {"path":"model/host-network-address", \
    "data":{"id":"ea:81:27:2a:6b:6b-10.0.0.1"}, "id":"id"}
model_syncd_config_test_data = {"path":"model/syncd-config", "data":{"id":test_syncd_config_id}, "id":"id"}
model_tag_test_data = {"path":"model/tag", "data":{"id":test_tag_id, \
    "name":"tagname", "value":"tagvalue", "namespace":test_vns_id}, "id":"id"}
model_tag_mapping_test_data = {"path":"model/tag-mapping", "data":{"id": test_tag_mapping_id, \
                               "tag": test_tag_id, "type": "host", "host": test_mac}, "id":"id"}
model_syncd_transport_config_test_data = {"path":"model/syncd-transport-config", 
    "data":{"id":"test_syncd_transport_config_test_data", "type":"random1", "args":"test_args", \
    "config":test_syncd_config_id, "target-cluster":"Testcluster", "name":test_syncd_transport_config_name}, "id":"id"}
model_syncd_progress_info_test_data = {"path":"model/syncd-progress-info", "data":{"id":test_syncd_config_id}, "id":"id"}
model_vns_interface_rule_test_data ={"path":"model/vns-interface-rule", \
    "data":{"id":test_vns_id+"|"+test_vns_interface_rule_name, "vns":test_vns_id}, "id":"id"}
model_vns_interface_acl_test_data = {"path":"model/vns-interface-access-list", "data":{"id":test_vns_interface_acl_id, \
    "in-out":test_vns_interface_acl_direction, "vns-interface":test_vns_interface_name, "vns-access-list":test_vns_acl_name_id}, "id":"id"}
model_vns_acl_entry_test_data = {"path":"model/vns-access-list-entry", \
    "data":{"id":test_vns_acl_entry_id, "src-ip":test_vns_acl_entry_src_ip, \
    "action":test_vns_acl_entry_action, "vns-access-list":test_vns_acl_name_id, "type":test_vns_acl_entry_type}, "id":"id"}

test_index=0
rest_api_test_data = {}
rest_api_test_data[test_index] = model_switch_test_data ; test_index += 1
rest_api_test_data[test_index] = model_host_test_data; test_index += 1
rest_api_test_data[test_index] = model_flow_entry_test_data; test_index += 1
rest_api_test_data[test_index] = model_vns_definition_test_data; test_index += 1
rest_api_test_data[test_index] = model_vns_acl_test_data; test_index += 1
rest_api_test_data[test_index] = model_host_network_address_test_data; test_index += 1
rest_api_test_data[test_index] = model_syncd_config_test_data; test_index += 1
rest_api_test_data[test_index] = model_tag_test_data; test_index += 1
rest_api_test_data[test_index] = model_tag_mapping_test_data; test_index += 1
rest_api_test_data[test_index] = model_syncd_transport_config_test_data; test_index += 1
rest_api_test_data[test_index] = model_syncd_progress_info_test_data; test_index += 1
rest_api_test_data[test_index] = model_vns_interface_rule_test_data; test_index += 1
rest_api_test_data[test_index] = model_vns_interface_acl_test_data; test_index += 1
rest_api_test_data[test_index] = model_vns_acl_entry_test_data; test_index += 1
