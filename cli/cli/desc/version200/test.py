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

TEST_PACKET_IN_COMMAND_DESCRIPTION = {
    'name'         : 'test',
    'mode'         : 'login',
    'no-supported' : False,
    'obj-type'     : 'test-command',   # fake obj-type, legacy-cli descriminator
    'short-help'   : 'Perform various tests on the network',
    'doc'          : 'test|test',
    'doc-example'  : 'test|test-packet-in-example',
    'action'       : 'legacy-cli',
    'args'         : (
        {
            'field'          : 'test-type',
            'type'           : 'enum',
            'values'         : 'packet-in',
            'doc'            : 'test|test-packet-in',
        },
        {
            # src-host
            'field'        : 'src-host',
            'tag'          : 'src-host',
            'type'         : 'host',
            'completion'   : 'complete-from-another',
            'other'        : 'host|mac',
            'data-handler' : 'alias-to-value',
            'doc'          : 'test|test-packet-in-mac',
        },
        {
            # dst-host
            'field'        : 'dst-host',
            'tag'          : 'dst-host',
            'type'         : 'host',
            'completion'   : 'complete-from-another',
            'other'        : 'host|mac',
            'data-handler' : 'alias-to-value',
            'doc'          : 'test|test-packet-in-mac',
        },
        {
            # src-switch
            'optional'   : True,
            'args'       : (
                {
                    'field'        : 'src-switch',
                    'tag'          : 'src-switch',
                    'type'         : 'dpid',
                    'completion'   : 'complete-from-another',
                    'other'        : 'switches|dpid', # field isn't dpid
                    'data-handler' : 'alias-to-value',
                    'doc'          : 'test|test-packet-in-dpid',
                },
                {
                    'field'        : 'src-switch-port',
                    'type'         : 'string',
                    'completion'   : 'complete-from-another',
                    'other'        : 'interfaces|portName',
                    'scoped'       : 'src-switch',
                    'data-handler' : 'convert-interface-to-port',
                    'doc'          : 'test|test-packet-in-if',
                },
            ),
        },
        {
            'field'     : 'vlan',
            'tag'       : 'vlan',
            'base-type' : 'integer',
            'range'     : (0,4095),
            'optional'  : True,
            'doc'       : 'test|test-packet-in-vlan',
        },
        # currently ether-type isn't implemented
        #{
            #'optional' : True,
            #'choices' : (
                #{
                    #'field'         : 'ether-type',
                    #'tag'           : 'ether-type',
                    #'base-type'     : 'hex-or-decimal-integer',
                    #'range'         : (1536,65536),
                    #'data-handler'  : 'hex-to-integer',
                #},
                #{
                    #'field'         : 'ether-type',
                    #'tag'           : 'ether-type',
                    #'type'          : 'enum',
                    #'values'        : fmtcnv.ether_type_to_number_dict,
                #},
            #),
        #},
        {
            'field'     : 'priority',
            'tag'       : 'priority',
            'base-type' : 'integer',
            'range'     : (0,7),
            'optional'  : True,
            'doc'       : 'test|test-packet-in-priority',
        },
        {
            'field'    : 'src-ip-address',
            'tag'      : 'src-ip-address',
            'type'     : 'ip-address-not-mask',
            'optional' : True,
            'doc'      : 'test|test-packet-in-ip',
        },
        {
            'field'    : 'dst-ip-address',
            'tag'      : 'dst-ip-address',
            'type'     : 'ip-address-not-mask',
            'optional' : True,
            'doc'      : 'test|test-packet-in-ip',
        },
        {
            'field'     : 'protocol',
            'tag'       : 'protocol',
            'base-type' : 'integer',
            'range'     : (1,255),
            'optional'  : True,
            'doc'      : 'test|test-packet-in-proto',
        },
        {
            'field'     : 'tos',
            'tag'       : 'tos',
            'base-type' : 'integer',
            'range'     : (1,255),
            'optional'  : True,
            'doc'      : 'test|test-packet-in-tos',
        },
        {
            'field'     : 'src-port',
            'tag'       : 'src-port',
            'base-type' : 'integer',
            'range'     : (0,65535),
            'optional'  : True,
            'doc'      : 'test|test-packet-in-port',
        },
        {
            'field'     : 'dst-port',
            'tag'       : 'dst-port',
            'base-type' : 'integer',
            'range'     : (0,65535),
            'optional'  : True,
            'doc'      : 'test|test-packet-in-port',
        },
    ),
}

TEST_PATH_COMMAND_DESCRIPTION = {
    'name'         : 'test',
    'mode'         : 'login',
    'no-supported' : False,
    'obj-type'     : 'test-command',   # fake obj-type, legacy-cli descriminator
    'short-help'   : 'Perform various tests on the network',
    'doc'          : 'test|test',
    'doc-example'  : 'test|test-path-example',
    'action'       : (
        # collect the entries
        {
            'proc' : 'legacy-cli',
        },
        # join the interface names, possibly add stats?
        {
            'proc'       : 'join-table',
            'obj-type'   : 'interfaces',
            'key'        : 'switch|portNumber',
            'join-field' : 'switch|port',
            'add-field'  : 'portName|portName',
        },
        # add basic interface's stats
        {
            'proc'       : 'join-rest',
            'url'        : 'realtimestats/port/%(switch)s/',
            'rest-type'  : 'dict-of-list-of-switch|portNumber',
            'key'        : 'switch|portNumber',
            'join-field' : 'switch|port'
        },
        # display the result
        {
            'proc'       : 'display',
            'format'     : 'test-path',
        }
    ),
    'args'         : (
        {
            'field'        : 'test-type',
            'type'         : 'enum',
            'values'       : 'path',
            'doc'          : 'test|test-path',
        },
        {
            'choices' : (
                # source device (need address space, vlan)
                {
                    'field'        : 'src-host',
                    'tag'          : 'src-host',
                    'type'         : 'host',
                    'completion'   : 'complete-from-another',
                    'other'        : 'host|mac',
                    'data-handler' : 'alias-to-value',
                    'doc'          : 'test|test-path-host',
                },
                # source ip
                {
                    'field'        : 'src-ip',
                    'tag'          : 'src-ip',
                    'type'         : 'ip-address',
                    'completion'   : 'complete-from-another',
                    'other'        : 'host-network-address|ip-address',
                    'doc'          : 'test|test-path-ip',
                },
                # source attachment point
                (
                    {
                        'field'        : 'src-switch',
                        'tag'          : 'src-switch',
                        'type'         : 'dpid',
                        'completion'   : 'complete-from-another',
                        'other'        : 'switches|dpid', # field isn't dpid
                        'data-handler' : 'alias-to-value',
                        'doc'          : 'test|test-path-switch',
                    },
                    {
                        'field'        : 'src-switch-port',
                        'type'         : 'string',
                        'completion'   : 'complete-from-another',
                        'other'        : 'interfaces|portName',
                        'scoped'       : 'src-switch',
                        'data-handler' : 'convert-interface-to-port',
                        'doc'          : 'test|test-path-if',
                    },
                ),
            )
        },
        {
            'choices' : (
                # dest mac (need address space, vlan)
                {
                    'field'        : 'dst-host',
                    'tag'          : 'dst-host',
                    'type'         : 'host',
                    'completion'   : 'complete-from-another',
                    'other'        : 'host|mac',
                    'data-handler' : 'alias-to-value',
                    'doc'          : 'test|test-path-host',
                },
                # dest ip (need address space, vlan)
                {
                    'field'        : 'dst-ip',
                    'tag'          : 'dst-ip',
                    'type'         : 'ip-address',
                    'completion'   : 'complete-from-another',
                    'other'        : 'host-network-address|ip-address',
                    'doc'          : 'test|test-path-ip',
                },
                # dest attachment point
                (
                    {
                        'field'        : 'dst-switch',
                        'tag'          : 'dst-switch',
                        'type'         : 'dpid',
                        'completion'   : 'complete-from-another',
                        'other'        : 'switches|dpid', # field isn't dpid
                        'data-handler' : 'alias-to-value',
                        'doc'          : 'test|test-path-switch',
                    },
                    {
                        'field'        : 'dst-switch-port',
                        'type'         : 'string',
                        'completion'   : 'complete-from-another',
                        'other'        : 'interfaces|portName',
                        'scoped'       : 'dst-switch',
                        'data-handler' : 'convert-interface-to-port',
                        'doc'          : 'test|test-path-if',
                    },
                ),
            )
        },
    ),
}

TEST_PATH_FORMAT = {
    'test-path' : {
        'field-orderings' : {
            'default' : [ 'Idx', 'switch', 'portName',
                          'receiveBytes', 'receivePackets', 'receiveErrors',
                          'transmitBytes', 'transmitPackets', 'transmitErrors',
                        ]
        },
        'fields' : {
            'switch'          : {
                                  'verbose-name' : 'Switch',
                                  'formatter' : fmtcnv.replace_switch_with_alias
                                },
            'portName'        : {
                                  'verbose-name' : 'IF',
                                },
            'receiveBytes'    : {
                                  'verbose-name' : 'Rx Bytes',
                                },
            'receivePackets'  : {
                                  'verbose-name' : 'Rx Pkts',
                                },
            'receiveErrors'   : {
                                  'verbose-name' : 'Rx Errs',
                                },
            'transmitBytes'   : {
                                  'verbose-name' : 'Tx Bytes',
                                },
            'transmitPackets' : {
                                  'verbose-name' : 'Tx Pkts',
                                },
            'transmitErrors' :  {
                                  'verbose-name' : 'Tx Errs',
                                },
        },
    },
}
