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
"""
TUNNEL_COMMAND_DESCRIPTION = {
    'name'          : 'tunnel-link',
    'feature'      : 'vns',
    'mode'          : 'login',
    'command-type'  : 'display-table',
    'doc'           : 'tunnel-link|verify-example',
    'obj-type'      : None,
    'args'  : {
        'choices': (
            (
                 {
                     'token'         : 'verify',
                     'short-help'    : 'Verify status of tunnel between two switches',
                     'action'        : 'display-rest',
                     'format'        : 'tunnel-event',
                     'url'           : [
                                        'realtimestatus/network/tunnelverify/%(src-dpid)s/%(dst-dpid)s/',
                                        ],
                 },
                 {
                     'field'         : 'src-dpid',
                     'type'          : 'dpid',
                     'completion'    : 'complete-from-another',
                     'other'         : 'switches|dpid',
                     'data-handler'  : 'alias-to-value',
                     'help-name'     : 'switch dpid or alias',
                 },
                 {
                     'field'         : 'dst-dpid',
                     'type'          : 'dpid',
                     'completion'    : 'complete-from-another',
                     'other'         : 'switches|dpid',
                     'data-handler'  : 'alias-to-value',
                     'help-name'     : 'switch dpid or alias',
                 }
            ),
        )
    }
}

TUNNEL_LINK_FAILURES_SHOW_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'mode'         : 'login',
    'feature'      : 'vns',
    'short-help'   : 'Show tunnel link failures',
    'command-type' : 'display-table',
    'url'          : 'realtimestatus/network/tunnelstatus/all/all',
    'obj-type'     : None,
    'format'       : 'tunnel-event',
    'action'       : 'display-rest',
    'args'  : (
        'tunnel-link-failures',
    ),
}

TUNNEL_EVENT_FORMAT = {
    'tunnel-event' : {
        'source'        : 'controller',
        'url'           : 'tunnel-event',
        'field-orderings': {
                 'default' : [ 'Idx', 'src-dpid', 'dst-dpid', 'status', 'last-verified' ],
        },
        'fields' : {
            'Idx' : {
                     'verbose-name': '#',
                     'edit' : False,
                     'type' : 'CharField',
                     'primary-key': True,
                  },
            'src-dpid':{
                'verbose-name': 'Src DPID',
                'json_serialize_string': True,
                'type': 'CharField',
                },
            'dst-dpid': {'json_serialize_string': True,
                'verbose-name': 'Dst DPID',
                'type': 'CharField',
                },
            'status': {
                'json_serialize_string': True,
                'type': 'CharField',
                },
            'last-verified': {
                'verbose-name': 'Last Verified',
                'formatter' : fmtcnv.print_time_since_utc,
                }
            }
        },
}
"""