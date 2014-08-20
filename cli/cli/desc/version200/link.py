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

LINK_SHOW_COMMAND_DESCRIPTION = {
    'name'         : 'show',
    'obj-type'     : 'link',
    'short-help'   : 'Show links, controller managed switch to switch interfaces',
    'doc'          : 'show-link',
    'doc-example'  : 'show-link-example',
    'mode'         : 'login',
    'command-type' : 'display-rest',
    'url'          : 'links',
    'action'       : (
        {
            'proc' : 'query-rest',
        },
        {
            'proc'   : 'display',
            'sort'   : [ 'src-switch', 'src-port' ],
            'format' : 'links',
        },
    ),
    'args'         : (
        'link',
        #{
            #'field'      : 'id',
            #'optional'   : True,
            #'type'       : 'obj-type',
            #'completion' : 'complete-object-field',
        #},
        # These parameters may be re-introduced if the
        # underlying rest api provides some search parameters
        #{
            #'field'      : 'src-switch',
            #'tag'        : 'src-switch',
            #'optional'   : True,
            #'type'       : 'string',
            #'completion' : 'complete-object-field',
        #},
        #{
            #'field'      : 'dst-switch',
            #'tag'        : 'dst-switch',
            #'optional'   : True,
            #'type'       : 'string',
            #'completion' : 'complete-object-field',
        #}
    )
}

import fmtcnv


LINKS_FORMAT = {
    'links' : {
        'field-orderings' : {
            'default' : [ 'Idx',
                          'src-switch', 'src-port', 
                          'dst-switch', 'dst-port', 'type' ]
             },
        'fields' : {
            'src-switch'     : {
                                'verbose-name' : 'Src Switch DPID',
                                'formatter' : fmtcnv.replace_switch_with_alias
                                # 'formatter' : fmtcnv.print_switch_and_alias
                               },
            'src-port'       : {
                                'verbose-name' : 'Src Port',
                                'formatter' : fmtcnv.decode_openflow_port_src_switch
                               },
            'src-port-state' : {
                                'verbose-name' : 'Src Port State',
                                'formatter' : fmtcnv.decode_port_state
                               },
            'dst-switch'     : {
                                'verbose-name' : 'Dst Switch DPID',
                                'formatter' : fmtcnv.replace_switch_with_alias
                                # 'formatter' : fmtcnv.print_switch_and_alias
                               },
            'dst-port'       : {
                                'verbose-name' : 'Dst Port',
                                'formatter' : fmtcnv.decode_openflow_port_dst_switch
                               },
            'dst-port-state' : {
                                'verbose-name' : 'Dst Port State',
                                'formatter' : fmtcnv.decode_port_state
                               },
            'link-type'      : {
                                'verbose-name' : 'Link Type',
                               },
            }
        },
}
