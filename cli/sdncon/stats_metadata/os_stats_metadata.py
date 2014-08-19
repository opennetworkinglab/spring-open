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

STATS_METADATA = [
    {'name': 'cpu-idle', 'type':'float', 'target_type':'controller', 'verbose_name': 'CPU Idle', 'units': '%'},
    {'name': 'cpu-nice', 'type':'float', 'target_type':'controller', 'verbose_name': 'CPU Nice', 'units': '%'},
    {'name': 'cpu-user', 'type':'float', 'target_type':'controller', 'verbose_name': 'CPU User', 'units': '%'},
    {'name': 'cpu-system', 'type':'float', 'target_type':'controller', 'verbose_name': 'CPU System', 'units': '%'},
    {'name': 'mem-used', 'type':'int', 'target_type':'controller', 'verbose_name': 'Memory Used', 'units': 'kB'},
    {'name': 'mem-free', 'type':'int', 'target_type':'controller', 'verbose_name': 'Memory Free', 'units': 'kB'},
    {'name': 'swap-used', 'type':'int', 'target_type':'controller', 'verbose_name': 'Swap Used', 'units': 'kB'},
    {'name': 'disk-root', 'type':'int', 'target_type':'controller', 'verbose_name': '/ Used', 'units': '%'},
    {'name': 'disk-log', 'type':'int', 'target_type':'controller', 'verbose_name': '/log Used', 'units': '%'},
    {'name': 'disk-boot', 'type':'int', 'target_type':'controller', 'verbose_name': '/sysboot Used', 'units': '%'},
    {'name': 'sdnplatform-cpu', 'type':'float', 'target_type':'controller', 'verbose_name': 'SDNPlatform CPU', 'units': '%'},
    {'name': 'database-cpu', 'type':'float', 'target_type':'controller', 'verbose_name': 'Database CPU', 'units': '%'},
    {'name': 'apache-cpu', 'type':'float', 'target_type':'controller', 'verbose_name': 'Apache CPU', 'units': '%'},
    {'name': 'cli-cpu', 'type':'float', 'target_type':'controller', 'verbose_name': 'Cli CPU', 'units': '%'},
    {'name': 'statd-cpu', 'type':'float', 'target_type':'controller', 'verbose_name': 'Statd CPU', 'units': '%'},
]
