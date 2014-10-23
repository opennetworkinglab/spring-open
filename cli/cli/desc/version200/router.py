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
    #'obj-type'            : 'switches',
    'doc'                 : 'switch|show',
    'doc-example'         : 'switch|show-example',
    'args' : (
        {
            'token'  : 'router',
            'field'  : 'router',
            'action' : 'display-rest',
            'sort'   : ['dpid',],
            'doc'    : 'switch|show',
            'url'    : [
                        'routers',
                       ],
            'format' : 'router',
        },
    )
}



ROUTER_SHOW_REALTIME_STATS_COMMAND_DESCRIPTION = {
    'name'                : 'show',
    'mode'                : 'login',
    'short-help'          : 'Show router stats via direct query to switch',
    'no-supported'        : False,
    'short-help'          : 'Show realtime stats for router',
    'parent-field'        : None,
    'obj-type'            : 'switches',
    'args'                : (
        {
            'token'        : 'router',
            'obj-type'     : 'switches',
            'command-type' : 'display-table',
            # 'command-type' : 'display-rest',
        },
        {
            'choices' : (
                {
                    'field'        : 'dpid',
                    'completion'   : 'complete-alias-choice',
                    'type'         : 'dpid',
                    'help-name'    : 'switch dpid or alias',
                    'data-handler' : 'alias-to-value',
                    'data'         : { 'detail' : 'scoped' },
                },
                {
                    'token'        : 'all',
                    'doc'          : 'reserved|all',
                },
            ),
        },
        {
            'choices' : (
                (
                    {
                        'field'      : 'routerrealtimestats',
                        'type'       : 'enum',
                        'values'     : (
                                        'port', 'adjacency'
                                        ),
                        'action'     : 'display-rest',
                        'sort'       : ['portNo'],
                        'url'        : 'router/%(dpid)s/%(routerrealtimestats)s',
                        'rest-type'  : 'dict-of-list-of-switch',
                        'format'     : 'router_%(routerrealtimestats)s',
                        'short-help' : 'Show requested item by querying router/switch',
                        'doc'        : 'switch|realtime-+',
                    },
                    {
                        'field'    : 'detail',
                        'optional' : True,
                        'type'     : 'enum',
                        'values'   : ('details','brief'),
                        'doc'      : 'format|+',
                    },
                ),
                
            )
        }
    )
}
