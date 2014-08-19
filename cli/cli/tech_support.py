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

#
#  (c) in 2012 by Big Switch Networks
#  All rights reserved
#

#
# show tech-support
#
## Command table for show tech-support command
tech_support_commands = [
    ['cli', 'show version', None],
    ['cli', 'show controller-node localhost summary', None],
    ['cli', 'show clock', None],
    ['cli', 'show config', None],
    ['cli', 'show config-file', None],
    ['cli', 'show controller-node', None],
    ['cli', 'show controller-node all interfaces', None],
    ['cli', 'show controller-node localhost stats', None],
    ['cli', 'show running-config', None],
    ['cli', 'show switch', None],
    ['cli', 'show switch-cluster', None],
    ['cli', 'show link', None],
    ['cli', 'show tunnel all', None],
    ['cli', 'show host', None],
    ['cli', 'show host all attachment-point', None],
    ['cli', 'show host all ip-address', None],
    # ['cli', 'show flow-cache', None],
    ['cli', 'show flow-entry', None],
    ['cli', 'show network-service', None],
    ['cli', 'show event-history topology-cluster', None],
    ['cli', 'show event-history topology-link', None],
    ['cli', 'show event-history topology-switch', None],
    ['cli', 'show logging all', None],
    # ['cli', 'show system internal performance-data'], wasn't working
    ['cli', 'history 100', None],
    ['shell',  'uptime', None],
    ['shell',  'ps -elf', None],
    ['shell',  'pstree', None],
    ['shell',  'cat /proc/meminfo', None],
    ['shell',  'df -m', None],
    ['shell',  'top -n 1 -b', None],
    ['shell',  'dmesg', None],
    ]

#-------------------------------------------
# add_to_show_tech_support
#-------------------------------------------
def add_to_show_tech_support(shell, cmd, feature=None):
    tech_support_commands.append([shell, cmd, feature])

def print_show_tech():
    for entry in tech_support_commands:
       print "Shell %s"%entry[0] + ",Cmd %s"%entry[1] + ",Feature %s"%entry[2]

def get_show_tech_cmd_table():
    return tech_support_commands
