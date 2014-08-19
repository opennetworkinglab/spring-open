#
# Copyright (c) 2012,2013 Big Switch Networks, Inc.
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

from tech_support import add_to_show_tech_support

# SNMP commands to be included in 'show tech-support' output
snmp_show_tech_support_commands = [
     ['cli', 'show snmp'],
     ['shell', 'pgrep snmpd'],
     ['shell', 'cat /etc/snmp/snmpd.conf'],
     ['shell', 'cat /etc/default/snmpd'],
]

# add commands to central command table used for 'show tech-support'
for entry in snmp_show_tech_support_commands:
    add_to_show_tech_support(str(entry[0]), str(entry[1]))
