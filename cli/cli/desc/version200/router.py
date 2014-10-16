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
import fmtcnv
SWITCH_ROUTER_COMMAND_DESCRIPTION = {
    'name'                : 'show',
    'mode'                : 'login',
    'command-type'        : 'display-table',
    'all-help'            : 'Show switch information',
    'short-help'          : 'Show switch summary',
    'obj-type'            : 'switches',
    'doc'                 : 'switch|show',
    'doc-example'         : 'switch|show-example',
    'args' : (
        {
            'token'  : 'router',
            'field'  : 'router',
            'action' : 'display-rest',
            'doc'    : 'switch|show',
            'url'    : [
                        'routers',
                       ],
            'format' : 'router',
        },
    )
}
