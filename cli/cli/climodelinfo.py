#
# Copyright (c) 2010,2013 Big Switch Networks, Inc.
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

#  sdnsh - The Controller Shell
#  module: climodelinfo.py
#
# This module contains classes that help fill out more of the CLI's model
# this will do a merge with the model_info_list object that is generated 
# directly from the django models
#
# the motivation is that commands/objects that are not based in the
# model should be able to take advantage of functions like prettyprint
# or completion
#
# also, this lets us add info to the model that is CLI-specific.
# In that respect, it is a lot like a django html view where the
# columns are presented in a certain order in tables, and attributes
# are grouped together in fieldsets

# field-orderings must have a 'default' entry variants for brief/verbose

# LOOK!
# - sort hints for show running 
# - justification for columns
# - have proper class for obj_type_info ?

import model_info_list # load the automatically generated model definitions from the controller
import base64
import struct
import re
import doctest

onos=1
class CliModelInfo(): 
    # so the model is accessible from the formatter functions
    singleton = None


    django_model_dict = {}
    complete_obj_type_info_dict = {}


    def __init__(self):
        CliModelInfo.singleton = self
        self.django_model_dict = dict(model_info_list.model_info_dict)
        self.complete_obj_type_info_dict = dict(self.django_model_dict)

        # Clean up the tables for the user field

        # merge from additional_model_info_dict

        # LOOK! should be able to do this by implementing a recursive deep copy/update
        for obj_type in self.additional_model_info_dict.keys():
            # if a new obj_type, just copy it
            if not obj_type in self.complete_obj_type_info_dict:
                self.complete_obj_type_info_dict[obj_type] = dict(self.additional_model_info_dict[obj_type])
            else:
                # if an existing obj_type, then go through keys (we know it's a dict)
                for obj_info in self.additional_model_info_dict[obj_type].keys():
                    if not obj_info in self.complete_obj_type_info_dict[obj_type]:
                        if obj_info == 'field-orderings':
                            self.complete_obj_type_info_dict[obj_type][obj_info] = dict(self.additional_model_info_dict[obj_type][obj_info])
                        else:
                            # shallow copy
                            self.complete_obj_type_info_dict[obj_type][obj_info] = self.additional_model_info_dict[obj_type][obj_info]
                    else:
                        if obj_info == "fields":
                            model_fields = self.complete_obj_type_info_dict[obj_type]['fields']
                            addl_fields = self.additional_model_info_dict[obj_type]['fields']

                            # both objects have fields - be intelligent
                            for f in addl_fields.keys():
                                if not f in model_fields:
                                    model_fields[f] = dict(addl_fields[f])
                                else:
                                    for attr in addl_fields[f].keys():
                                        model_fields[f][attr] = addl_fields[f][attr]
        #print self.complete_obj_type_info_dict

    def add_type_info(self, name, type_info):
        self.complete_obj_type_info_dict[name] = type_info

    def get_complete_obj_type_info_dict(self):
        return self.complete_obj_type_info_dict

    def get_fields_to_print(self, obj_type, field_ordering='default'):
        fields_to_print = []
        obj_type_info = self.complete_obj_type_info_dict.get(obj_type, None)
        if obj_type_info:
            if 'field-orderings' in obj_type_info:
                fields_to_print = obj_type_info['field-orderings'].get(field_ordering, [])
            if len(fields_to_print) == 0: # either no field-orderings or couldn't find specific
                fields_to_print = obj_type_info['fields'].keys()
        return fields_to_print

    def get_field_info(self, obj_type, field_name):
        obj_type_info = self.complete_obj_type_info_dict.get(obj_type, None)
        return obj_type_info['fields'].get(field_name, None)

    # LOOK! Refactor - Should we merge this with model_info_list?
    #if (onos == 1):
        # do nothing
    #    additional_model_info_dict = {}
    #elif (onos==2):
    if (onos == 1):
        additional_model_info_dict = {
        'switches' : {
            # switches are now directly fetched via rest api.
            # 'has_rest_model' isn't set
            # for this table.
            # XXX perhaps these types of descriptions need to be
            # in a file other than climodelinfo?
            'source'          : 'user-config',
            'source'          : 'display',
            'url'             : 'switches',
            'config-obj-type' : 'switch-config',

            'fields' : {
                'dpid'               : {
                                         'edit' : False,
                                         'max_length': 32,
                                         'null': False,
                                         'primary_key': True,
                                         'type': 'CharField',
                                         'edit' : False,
                                       },
                'active'             : {
                                         'edit' : False
                                       },
                'core-switch'        : {
                                         'edit' : False,
                                       },
                'connected-since'    : { 
                                         'edit' : False
                                       },
                'capabilities'       : {
                                         'edit' : False
                                       },
                'actions'            : {
                                         'edit' : False
                                       },
                'ip-address'         : {
                                         'edit' : False
                                       },
                'socket-address'     : {
                                         'edit' : False
                                       },
                'buffers'            : {
                                         'edit' : False
                                       },
                'controller'         : {
                                         'edit' : False,
                                       },
                'tables'             : {
                                         'edit' : False
                                       },
                'switch-alias'       : {
                                         'help_text' : 'Switch alias for DPID',
                                         'edit' : False,
                                       },
                'tunnels'            : {
                                         'edit' : False,
                                       },
                'tunnel-supported'   : {
                                         'edit' : False,
                                       },
                'tunnel-termination' : {
                                         'edit' : False,
                                       },
                'tunnel-active'      : {
                                         'edit' : False,
                                       },
                'dp-desc'            : {
                                         'edit' : False,
                                       },
                'hw-desc'            : {
                                         'edit' : False,
                                       },
                'sw-desc'            : {
                                         'edit' : False,
                                       },
                'serial-num'         : {
                                         'edit' : False,
                                       },
                }
            },
        'interfaces' : {
            # switches are now directly fetched via rest api.
            # 'has_rest_model' isn't set
            # for this table.
            # XXX perhaps these types of descriptions need to be
            # in a file other than climodelinfo?
            'source'          : 'display',
            'url'             : 'switches',
            'config-obj-type' : 'switch-interface-config',

            'fields' : {
                'id'                  : {
                                         'edit' : False,
                                         'max_length': 48,
                                         'null': False,
                                         'primary_key': True,
                                         'type' : 'compound-key',
                                         'edit' : False,
                                         'help_text' : '#!dpid',
                                         'compound_key_fields': [ 'dpid' ]
                                        },
                'dpid'                : {
                                           #'type'           : 'ForeignKey',
                                           #'rel_field_name' : 'dpid',
                                           #'rel_obj_type'   : 'switch',
                                        },
                'name'                : {
                                        },
                'number'              : {
                                        },
                'config'              : {
                                        },
                'state'               : {
                                        },
                'current-features'    : {
                                        },
                'advertised-features' : {
                                        },
                'supported-features'  : {
                                        },
                'peer-features'       : {
                                        },
                }
            },


        'switch-config' : {
            'source' : 'debug-only',
        },


        'tunnel-config' : {
            'source'          : 'user-config',
            'source'          : 'display',
            'url'             : 'tunnel-config',
            'config-obj-type' : 'tunnel-config',

            'fields' : {
                'tunnel-id'               : {
                                         'edit' : False,
                                         'max_length': 32,
                                         'null': False,
                                         'primary_key': True,
                                         'type': 'CharField',
                                         'edit' : False,
                                       },
                        },
        },
                                      
        'policy-config' : {
            'source'          : 'user-config',
            'source'          : 'display',
            'url'             : 'policy-config',
            'config-obj-type' : 'policy-config',

            'fields' : {
                'policy-id'               : {
                                         'edit' : False,
                                         'max_length': 32,
                                         'null': False,
                                         'primary_key': True,
                                         'type': 'CharField',
                                         'edit' : False,
                                       },
                        },
        },
                                      
        'switch-alias' : {
            'source'          : 'user-config',
            'cascade_delete'  : True,
            },


        'switch-interface-config' : {
            'source'          : 'debug-only',
            'cascade_delete'  : True,

            'fields'          : {
                'broadcast'           : { 'edit' : False
                                        },
                'name'                : { 'edit' : False
                                        },
                'mode'                : { 
                                          'edit' : False
                                        },
                },
            },


        'host-alias' : {
            'source'          : 'user-config',
            'cascade_delete'  : True,
            'fields' : {
                }
            },


        'link' : { 
            'source'          : 'switch',

            'fields' : {
                'link-type'      : {
                                    'type' : 'CharField'
                                   },
                }
            },


        'port' : {
            'source'          : 'debug-only',

            'fields' : {
                'switch'              : {
                                        },
                'number'              : {
                                        },
                'config'              : {
                                        },
                'state'               : {
                                        },
                'current-features'    : {
                                        },
                'advertised-features' : {
                                        },
                'supported-features'  : {
                                        },
                'peer-features'       : {
                                        },
                }
            },

        'switch-interfaces' : {
            'source'    : 'switch',

            'fields' : {
                'Idx'                 : { 
                                           'edit' : False,
                                          'type' : 'CharField'
                                        },
                'id'                  : {
                                          'primary_key': True,
                                          # format's out of the sorting business
                                          # 'sort' : 'tail-integer',
                                        },
                'switch'              : {
                                        },
                'name'                : {
                                          'type' : 'CharField',
                                          'max_length' : 32,
                                        },
                'hardware-address'    : {
                                        },
                'config'              : { 
                                        },
                'state'               : {
                                        },
                'stp-state'           : { 
                                        },
                'current-features'    : { 
                                        },
                'advertised-features' : {
                                        },
                'supported-features'  : {
                                        },
                'peer-features'       : {
                                        },
                'receiveBytes'        : {
                                        },
                'receivePackets'      : {
                                        },
                'receiveErrors'       : {
                                        },
                'transmitBytes'       : {
                                        },
                'transmitPackets'     : {
                                        },
                'transmitErrors'      : {
                                        },
                },
            },

        'controller_switches' : {
            'source'          : 'user-config',
            
            'fields' : {
                'dpid' : { 'primary_key' : True,
                         },
                }
            },
    
        'host-config' : {
            'source'          : 'show',
            'url'             : 'device',

            'fields' : {
                'Idx'                : {
                                          'edit' : False,
                                          'type' : 'CharField'
                                       },
                'mac'                : { 
                                          'json_serialize_string': True,
                                          'max_length': 17,
                                          'null': False,
                                          'type': 'CharField',
                                          'edit' : False,
                                        },
                'vendor'              : {
                                          'edit' : False,
                                        },
                'ips'                 : {
                                          'edit' : False
                                        },
                'attachment-points'   : {
                                          'edit' : False
                                        },
                'tag'                 : {
                                          'edit' : False
                                        },
                'host-alias'          : { 'help_text' : 'Host alias',
                                          'edit' : False,
                                        },
                'last-seen'           : {
                                          'help_text': 'Last time a packet was seen by the controller',
                                          'json_serialize_string': True,
                                          'null': True,
                                          'type': 'DateTimeField',
                                        },
                },
            },

        'host': {
            # hosts are now directly fetched via rest api. 'has_rest_model' isn't set
            # for this table.
            # XXX perhaps these types of descriptions need to be
            # in a file other than climodelinfo?
            'source'          : 'user-config',
            'source'          : 'display',
            'url'             : 'device',
            'config-obj-type' : 'host-config',

            'fields': {
                'mac'                : { 
                                          'json_serialize_string': True,
                                          'max_length': 17,
                                          'null': False,
                                          'primary_key': True,
                                          'type': 'CharField',
                                          'edit' : False,
                                        },
                'vlan'                : {
                                          'help_text'            : 'VLAN Associated with host',
                                          'json_serialize_string': False,
                                          'null'                 : True,
                                          'type'                 : 'IntegerField',
                                          'edit'                 : False,
                                          'searchable'           : True,
                                        },
                'dpid'                : {
                                           'edit'           : False,
                                           'type'           : 'ForeignKey',
                                           'rel_field_name' : 'dpid',
                                           'rel_obj_type'   : 'switches',
                                        },
                'ipv4'                : {
                                           'edit' : False,
                                        },
                # Attachment point isn't searchable
                #'attachment-points'  : {
                                           #'edit' : False
                                        #},
                'last-seen'           : {
                                          'help_text': 'Last time a packet was seen by the controller',
                                          'json_serialize_string': True,
                                          'null': True,
                                          'type': 'DateTimeField',
                                        },
                 },
            },


        'host-attachment-point' : {
            # directly fetched via rest api. 
            # 'has_rest_model' isn't set for this table.
            'source'          : 'controller',
            'url'             : 'device',

             'fields': {
                'mac'          : {
                                  'json_serialize_string': True,
                                  'null': True,
                                  'rel_field_name': 'mac',
                                  'rel_obj_type': 'host',
                                  'type': 'ForeignKey',
                                 },
                'vlan'         : {
                                   'help_text': 'VLAN Associated with host',
                                   'json_serialize_string': False,
                                   'null': True,
                                   'type': 'IntegerField',
                                   'edit' : False,
                                    'searchable' : True,
                                 },
                'id'           : {
                                  'help_text': '#|mac|dpid|ingress-port',
                                  'json_serialize_string': True,
                                  'max_length': 64,
                                  'null': False,
                                  'primary_key': True,
                                  'type': 'CharField',
                                 },
                'dpid'         : { 'json_serialize_string': True,
                                   'null': True,
                                   'rel_field_name': 'dpid',
                                   'rel_obj_type': 'switch',
                                   'type': 'ForeignKey',
                                 },
                'ingress-port' : {
                                  'help_text': 'Open flow port number of ingress port',
                                  'json_serialize_string': False,
                                  'null': True,
                                  'type': 'IntegerField',
                                 },
                'status'       : {
                                   'help_text': 'Error description, blank mean no error ',
                                   'json_serialize_string': True,
                                   'max_length': 64,
                                   'null': True,
                                   'type': 'CharField',
                                 },
                'last-seen'    : {
                                   'help_text': 'Last time a packet was seen by the controller',
                                   'json_serialize_string': True,
                                   'null': True,
                                   'type': 'DateTimeField',
                                 },
                'ipv4'         : { # added to enable searching
                                    'edit' : False,
                                    'searchable' : True,
                                 },
                 },
             },

        'host-network-address': {
            # directly fetched via rest api. 
            # 'has_rest_model' isn't set for this table.
            'source'          : 'switch',
            'url'             : 'device',

            'fields': {
                'mac'        : { 'json_serialize_string': True,
                                 'null': True,
                                 'rel_field_name': 'mac',
                                 'rel_obj_type': 'host',
                                 'type': 'ForeignKey',
                                 'edit' : False,
                               },
                'vlan'       : {
                                 'help_text': 'VLAN Associated with host',
                                 'json_serialize_string': False,
                                 'null': True,
                                 'type': 'IntegerField',
                                 'edit' : False,
                                 'searchable' : True,
                               },
                'id'         : {
                                 'help_text': '#|mac|ipv4',
                                 'json_serialize_string': True,
                                 'max_length': 64,
                                 'null': False,
                                 'primary_key': True,
                                 'type': 'CharField',
                               },
                'ip-address':  {
                                 'help_text': 'IP Address of host',
                                 'json_serialize_string': True,
                                 'max_length': 15,
                                 'null': True,
                                 'type': 'CharField',
                                 'edit' : False,
                              },
                'last-seen' : {
                                'help_text': 'Last time a packet was seen by the controller',
                                'json_serialize_string': True,
                                'null': True,
                                'type': 'DateTimeField',
                              },
                'ipv4'      : { # added to enable searching
                                 'max_length': 15,
                                 'null': True,
                                 'type': 'CharField',
                                 'edit' : False,
                              },
                'dpid'      : {
                                 'edit' : False,
                                 'searchable' : True,
                              },
                },
            }
        }
    else:
        additional_model_info_dict = {
        'flow-entry' : {
            'source'          : 'user-config',
           'update-alias'    : ['switch', 'port'],
            },


        'feature' : {
            'source'          : 'debug-only',

            'fields'          : {
                'netvirt-feature'                   : {
                                                    'edit' : False},
                'static-flow-pusher-feature'    : { 
                                                    'edit' : False },
                'performance-monitor-feature'   : { 
                                                    'edit' : False},
            }
        },


        'system-interfaces' : {
            'source'    : '',

            'fields' : {
                'name'      : {
                                'type' : 'CharField' },
                'addr'      : {
                                'type' : 'CharField' },
                'peer'       : {
                                'type' : 'CharField' },
                'netmask'   : {
                                'type' : 'CharField' },
                'broadcast' : {
                                'type' : 'CharField' },
                }
            },


        'switch-cluster' : {
            'source'          : 'user-config',

            'fields' : {
                'cluster-id' : {
                                 'primary_key': True
                               },
                'switches'   : {
                               },
                }
            },

        'switches' : {
            # switches are now directly fetched via rest api.
            # 'has_rest_model' isn't set
            # for this table.
            # XXX perhaps these types of descriptions need to be
            # in a file other than climodelinfo?
            'source'          : 'user-config',
            'source'          : 'display',
            'url'             : 'switches',
            'config-obj-type' : 'switch-config',

            'fields' : {
                'dpid'               : {
                                         'edit' : False,
                                         'max_length': 32,
                                         'null': False,
                                         'primary_key': True,
                                         'type': 'CharField',
                                         'edit' : False,
                                       },
                'active'             : {
                                         'edit' : False
                                       },
                'core-switch'        : {
                                         'edit' : False,
                                       },
                'connected-since'    : { 
                                         'edit' : False
                                       },
                'capabilities'       : {
                                         'edit' : False
                                       },
                'actions'            : {
                                         'edit' : False
                                       },
                'ip-address'         : {
                                         'edit' : False
                                       },
                'socket-address'     : {
                                         'edit' : False
                                       },
                'buffers'            : {
                                         'edit' : False
                                       },
                'controller'         : {
                                         'edit' : False,
                                       },
                'tables'             : {
                                         'edit' : False
                                       },
                'switch-alias'       : {
                                         'help_text' : 'Switch alias for DPID',
                                         'edit' : False,
                                       },
                'tunnels'            : {
                                         'edit' : False,
                                       },
                'tunnel-supported'   : {
                                         'edit' : False,
                                       },
                'tunnel-termination' : {
                                         'edit' : False,
                                       },
                'tunnel-active'      : {
                                         'edit' : False,
                                       },
                'dp-desc'            : {
                                         'edit' : False,
                                       },
                'hw-desc'            : {
                                         'edit' : False,
                                       },
                'sw-desc'            : {
                                         'edit' : False,
                                       },
                'serial-num'         : {
                                         'edit' : False,
                                       },
                }
            },

        'interfaces' : {
            # switches are now directly fetched via rest api.
            # 'has_rest_model' isn't set
            # for this table.
            # XXX perhaps these types of descriptions need to be
            # in a file other than climodelinfo?
            'source'          : 'display',
            'url'             : 'switches',
            'config-obj-type' : 'switch-interface-config',

            'fields' : {
                'id'                  : {
                                         'edit' : False,
                                         'max_length': 48,
                                         'null': False,
                                         'primary_key': True,
                                         'type' : 'compound-key',
                                         'edit' : False,
                                         'help_text' : '#!dpid',
                                         'compound_key_fields': [ 'dpid' ]
                                        },
                'dpid'                : {
                                           #'type'           : 'ForeignKey',
                                           #'rel_field_name' : 'dpid',
                                           #'rel_obj_type'   : 'switch',
                                        },
                'name'                : {
                                        },
                'number'              : {
                                        },
                'config'              : {
                                        },
                'state'               : {
                                        },
                'current-features'    : {
                                        },
                'advertised-features' : {
                                        },
                'supported-features'  : {
                                        },
                'peer-features'       : {
                                        },
                }
            },


        'switch-config' : {
            'source' : 'debug-only',
        },


        'switch-alias' : {
            'source'          : 'user-config',
            'cascade_delete'  : True,
            },


        'switch-interface-config' : {
            'source'          : 'debug-only',
            'cascade_delete'  : True,

            'fields'          : {
                'broadcast'           : { 'edit' : False
                                        },
                'name'                : { 'edit' : False
                                        },
                'mode'                : { 
                                          'edit' : False
                                        },
                },
            },


        'host-alias' : {
            'source'          : 'user-config',
            'cascade_delete'  : True,
            },


        'link' : { 
            'source'          : 'switch',

            'fields' : {
                'link-type'      : {
                                    'type' : 'CharField'
                                   },
                }
            },


        'port' : {
            'source'          : 'debug-only',

            'fields' : {
                'switch'              : {
                                        },
                'number'              : {
                                        },
                'config'              : {
                                        },
                'state'               : {
                                        },
                'current-features'    : {
                                        },
                'advertised-features' : {
                                        },
                'supported-features'  : {
                                        },
                'peer-features'       : {
                                        },
                }
            },

        'switch-interfaces' : {
            'source'    : 'switch',

            'fields' : {
                'Idx'                 : { 
                                           'edit' : False,
                                          'type' : 'CharField'
                                        },
                'id'                  : {
                                          'primary_key': True,
                                          # format's out of the sorting business
                                          # 'sort' : 'tail-integer',
                                        },
                'switch'              : {
                                        },
                'name'                : {
                                          'type' : 'CharField',
                                          'max_length' : 32,
                                        },
                'hardware-address'    : {
                                        },
                'config'              : { 
                                        },
                'state'               : {
                                        },
                'stp-state'           : { 
                                        },
                'current-features'    : { 
                                        },
                'advertised-features' : {
                                        },
                'supported-features'  : {
                                        },
                'peer-features'       : {
                                        },
                'receiveBytes'        : {
                                        },
                'receivePackets'      : {
                                        },
                'receiveErrors'       : {
                                        },
                'transmitBytes'       : {
                                        },
                'transmitPackets'     : {
                                        },
                'transmitErrors'      : {
                                        },
                },
            },

        'config' : {
            'source'          : 'user-config',
            
            'fields' : {
                'name'     : { 'primary_key': True },
                'version'  : { },
                'length'   : { },
                'timestamp': { },
                },
            },

        'controller_switches' : {
            'source'          : 'user-config',
            
            'fields' : {
                'dpid' : { 'primary_key' : True,
                         },
                }
            },

        'test-pktin-route' : {
            'source' : 'debug-only',
            
            'fields' : {
                'cluster' : {
                            },
                'hop'     : {
                            },
                'dpid'    : {
                            },
                'inPort' :  {
                            },
                'outPort':  {
                            },
                },
            },


        'performance-data' : {
            'source' : 'debug-only' ,

            'fields' : {
                'Pkts'     : {
                             },
                'CompName' : {
                             },
                'StartTime': {
                             },
                }
            },


        'flow-cache-counters' : {
            'source' : 'sdnplatform',
            'field-orderings' : {
                'details' : [
                                'applName',
                                'maxFlows',
                                'activeCnt',
                                'inactiveCnt',
                                'addCnt',
                                'delCnt',
                                'activatedCnt',
                                'deactivatedCnd',
                                'cacheHitCnt',
                                'missCnt',
                                'flowModRemovalMsgLossCnt',
                                'notStoredFullCnt',
                                'fcObjFreedCnt',
                                'unknownOperCnt',
                                'flowCacheAlmostFull',
                            ],

            },

            'fields' : {
                'applName'                 : {
                                             },
                'maxFlows'                 : {
                                             },
                'activeCnt'                : {
                                             },
                'inactiveCnt'              : {
                                             },
                'addCnt'                   : {
                                             },
                'delCnt'                   : {
                                             },
                'activatedCnt'             : {
                                             },
                'deactivatedCnd'           : {
                                             },
                'cacheHitCnt'              : {
                                             },
                'missCnt'                  : {
                                             },
                'flowModRemovalMsgLossCnt' : {
                                             },
                'notStoredFullCnt'         : {
                                             },
                'fcObjFreedCnt'            : {
                                             },
                'unknownOperCnt'           : {
                                             },
                'flowCacheAlmostFull'      : {
                                             },
            },
        },


        'flow-cache' : {
            'source' : 'debug-only',

            'fields' : {
                'Source-Switch'  : {
                                   },
                'InputPort'      : {
                                   },
                'SrcMAC'         : {
                                   },
                'DestMAC'        : {
                                   },
                'EtherType'      : {
                                   },
                'Protocol'       : {
                                   },
                'SrcPort'        : {
                                   },
                'DstPort'        : {
                                   },
                'Time'           : {
                                   },
               }
            },


        'ev-hist-topology-switch' : {
            'source' : 'debug-only',

            'fields' : {
                'Idx'     : {
                                'primary_key'  : True,
                            },
                'Time'    : {
                            },
                'Switch'  : {
                            },
                'Port'    : {
                            },
                'IpAddr'  : {
                            },
                'Action'  : {
                            },
                'Reason'  : {
                            },
                'State'   : {
                            },
                }
            },


        'ev-hist-topology-cluster' : {
            'source' : 'debug-only',
            
            'fields' : {
                'Time'    : {
                                'primary_key'  : False, 
                            },
                'Switch'  : {
                            },
                'Action'  : {
                            },
                'Reason'  : {
                            },
                'State'   : {
                            },
                }
            },


        'ev-hist-topology-link' : {
            'source' : 'debug-only',
            
            'fields' : {
                'Time'           : {
                                    'primary_key'  : False, 
                                   },
                'Source-Switch'  : {
                                   },
                'Dest-Switch'    : {
                                   },
                'SrcPort'        : {
                                   },
                'DstPort'        : {
                                   },
                'SrcPortState'   : {
                                   },
                'DstPortState'   : {
                                   },
                'Action'         : {
                                   },
                'Reason'         : {
                                   },
                'State'          : {
                                   },
                }
            },


        'ev-hist-attachment-point' : {
            'source' : 'debug-only',
            
            'fields' : {
                'Time_ns' : {
                                'primary_key'  : False, 
                            },
                'Host'    : {
                            },
                'Switch'  : {
                            },
                'Port'    : {
                            },
                'VLAN'    : {
                            },
                'Action'  : {
                            },
                'Reason'  : {
                            },
                }
            },


        'ev-hist-packet-in' : {
            'source' : 'debug-only',
            
            'fields' : {
                'Time'             : {'primary_key'  : False, 
                                     },
                'wildcards'        : {
                                     },
                'dataLayerSource'  : {
                                     },
                'dataLayerDestination' : {
                                     },
                'dataLayerType'    : {
                                     },
                'dataLayerVirtualLan' : {
                                     },
                'dataLayerVirtualLanPriorityCodePoint' : { 
                                     },
                'inputSwitch'      : {
                                     },
                'inputPort'        : { 
                                     },
                'networkSource'    : { 
                                     },
                'networkDestination' : {
                                     },
                'networkSourceMaskLen' : {
                                     },
                'networkDestinationMaskLen' : {
                                     },
                'networkProtocol'  : {
                                     },
                'networkTypeOfService' : {
                                     },
                'transportSource'  : {
                                     },
                'transportDestination' : {
                                     },
                'Action         '  : {
                                     },
                'Reason'           : {
                                     },
                }                   
            },


        'vns-definition' : {
            'source'          : 'user-config',
            'source'          : 'show',
            'cascade_delete'  : True,
            'show-this'       : [
                                    ['vns-definition', 'default'],
                                    ['vns-interface-rule', 'vns-config' ],
                                ],

            'fields' : {
               'active'       : {
                                  'edit' : False,
                                },
               'priority'     : {
                                  'edit' : False,
                                },
               'arp-mode'     : {
                                  'edit' : False,
                                },
               'dhcp-mode'    : {
                                  'edit' : False,
                                },
               'dhcp-ip'      : {
                                  'edit' : False,
                                },
               'broadcast'    : {
                                  'edit' : False,
                                },
               }
            },


        'vns-interface-rule' : {
            'source'          : 'debug-only',
            'cascade_delete'  : True,
            'title'           : 'VNS Interface Rules',

            'fields': {
                'rule'               : { 
                                         'type' : 'CharField',
                                         'max_length' : 32,
                                       },
                'vns'                : {
                                       },
                'mac'                : { 'edit' : False,
                                       },
                'tags'               : { 
                                         'type' : 'CharField',
                                         'max_length' : 32,
                                       },
                'active'             : { 'edit' : False },
                'allow-multiple'     : { 'edit' : False },
                'vlan-tag-on-egress' : { 'edit' : False },
                'description'        : { 'edit' : False },
                'ip-subnet'          : { 'edit' : False },
                'ports'              : { 'edit' : False },
                'priority'           : { 'edit' : False },
                'rule'               : { 'edit' : False },
                'switch'             : { 'edit' : False },
                'tags'               : { 'edit' : False },
                'vlans'              : { 'edit' : False },
                }
            },


        'display-vns-interface' : {
            'source'          : 'user-config',
            'cascade_delete'  : True,

            'fields': {
                'id'                : { 
                                        'edit' : False,
                                        'type' : 'CharField',
                                      },
                'rule'              : {
                                        'edit' : False,
                                        'type' : 'CharField',
                                      },
                'mac'               : {
                                        'type' : 'CharField',
                                        'max_length' : 32,
                                        'edit' : False },
                'vlan'              : { 
                                        'type' : 'Charfield',
                                        'max_length' : 32,
                                        'edit' : False },
                'ips'               : { 
                                        'type' : 'CharField',
                                        'max_length' : 32,
                                        'edit' : False },
                'attachment-points' : {
                                        'type' : 'CharField',
                                        'max_length' : 32,
                                        'edit' : False },
                'last-seen'         : { 'edit' : False,
                                      }
                }
            },


        'vns-interface-display' : {
            'source'          : 'user-config',
            'cascade_delete'  : True,

            'fields': {
                'id'                : {
                                        'edit' : False,
                                        'type' : 'CharField',
                                      },
                'rule'              : {
                                        'edit' : False,
                                        'type' : 'CharField',
                                      },
                'mac'               : {
                                        'type' : 'CharField',
                                        'max_length' : 32,
                                        'edit' : False },
                'vlan'              : {
                                        'type' : 'Charfield',
                                        'max_length' : 32,
                                        'edit' : False },
                'ips'               : { 
                                        'type' : 'CharField',
                                        'max_length' : 32,
                                        'edit' : False },
                'attachment-points' : { 
                                        'type' : 'CharField',
                                        'max_length' : 32,
                                        'edit' : False },
                'last-seen'         : { 'edit' : False,
                                      }
                }
            },


        'vns-access-list': {
            'source'          : 'user-config',
            'cascade_delete'  : True,

            'fields' : {
                'name'       : {
                                 'edit' : False,
                                 'cascade_delete' : True
                               },
                }
            },


        'vns-access-list-entry': {
            'source'          : 'user-config',
            'cascade_delete'  : True,

            'fields' : {
                #
                # vns-acl-entry fields are a bit uncommon.  the action and type
                # can only be configured via the create portion of the command,
                # while many other fields requre specific validation so that
                # alternate values can be replaced for some keywords
                #
                # the values of the 'validate' field is the name of the
                # def to call (in the bigSh class)
                #
                'vns-access-list'  : {
                                     },
                'rule'             : {
                                       'cascade_delete' : True,
                                       'sort' : 'integer'
                                     },
                'action'           : {
                                       'edit' : False
                                     },
                'type'             : {
                                       'edit' : False
                                     },
                'src-ip'           : {
                                     },
                'src-ip-mask'      : {
                                     },
                'dst-ip'           : {
                                     },
                'dst-ip-mask'      : {
                                     },
                'src-tp-port-op'   : {
                                     },
                'src-tp-port'      : {
                                     },
                'dst-tp-port-op'   : {
                                     },
                'dst-tp-port'      : {
                                     },
                'icmp-type'        : { 
                                     },
                'ether-type'       : { 
                                     },
                }
            },


        'vns-interface-access-list': {
            'source'          : 'user-config',
            'cascade_delete'  : True,

            'fields' : {
                'vns'                : {
                                         'type'       : 'CharField',
                                         'max_length' : 32
                                       },
                'vns-interface-name' : {
                                         'type'       : 'CharField',
                                         'max_length' : 32
                                       },
                'name'               : {
                                         'type'       : 'CharField',
                                         'max_length' : 32
                                       },
                }
            },


        'tag' : {
            'source'          : 'user-config',
            'source'          : 'switch',
            },

        
        'tag-mapping' : {
            'source'          : 'user-config',
            'source'          : 'switch',

            'fields' : {
                'type'      : {
                              },
                'host'      : {
                              },
                }
            },


        'syncd-config' : {
            'source'          : 'debug-only',
            },

        'syncd-progress-info' : {
            'source'          : 'debug-only',
            },

        'syncd-transport-config' : {
            'source'          : 'debug-only',
            },

        'statd-config' : {
            'source'          : 'debug-only',
            },

        'statdropd-config' : {
            'source'          : 'debug-only',
            },

        'statdropd-progress-info' : {
            'source'          : 'debug-only',
            },

        'tech-support-config' : {
            },

        'tunnel-details' : {
            'source' : 'switch',

            'fields' : {
                'dpid'              : {
                                         'primary_key': True,
                                      },
                'localTunnelIPAddr' : {
                                      },
                'tunnelPorts'       : {
                                      },
                },
            },


        'controller-summary' : {
            'source' : 'switch',

            'fields' : {
                '# Access Control Lists'            : {
                                                      },
                '# VNS Interfaces'                  : {
                                                      },
                '# hosts'                           : {
                                                      },
                '# VNSes'                           : {
                                                      },
                '# attachment points'               : {
                                                      },
                '# inter-switch links'              : {
                                                      },
                '# IP Addresses'                    : {
                                                      },
                '# VNS Interfaces with ACL applied' : {
                                                      },
                },
            },


        'switch-interface-alias' : {
            'source'          : 'commands',
            'cascade_delete'  : True,

            'fields' : {
                'id'          : {
                                }
                }
            },


        'host-config' : {
            'source'          : 'show',
            'url'             : 'device',

            'fields' : {
                'Idx'                : {
                                          'edit' : False,
                                          'type' : 'CharField'
                                       },
                'mac'                : { 
                                          'json_serialize_string': True,
                                          'max_length': 17,
                                          'null': False,
                                          'type': 'CharField',
                                          'edit' : False,
                                        },
                'vendor'              : {
                                          'edit' : False,
                                        },
                'ips'                 : {
                                          'edit' : False
                                        },
                'attachment-points'   : {
                                          'edit' : False
                                        },
                'tag'                 : {
                                          'edit' : False
                                        },
                'host-alias'          : { 'help_text' : 'Host alias',
                                          'edit' : False,
                                        },
                'last-seen'           : {
                                          'help_text': 'Last time a packet was seen by the controller',
                                          'json_serialize_string': True,
                                          'null': True,
                                          'type': 'DateTimeField',
                                        },
                },
            },

        'host': {
            # hosts are now directly fetched via rest api. 'has_rest_model' isn't set
            # for this table.
            # XXX perhaps these types of descriptions need to be
            # in a file other than climodelinfo?
            'source'          : 'user-config',
            'source'          : 'display',
            'url'             : 'device',
            'config-obj-type' : 'host-config',

            'fields': {
                'mac'                : { 
                                          'json_serialize_string': True,
                                          'max_length': 17,
                                          'null': False,
                                          'primary_key': True,
                                          'type': 'CharField',
                                          'edit' : False,
                                        },
                'address-space'       : {
                                           'edit'           : False,
                                           'type'           : 'ForeignKey',
                                           'rel_field_name' : 'name',
                                           'rel_obj_type'   : 'address-space',
                                        },
                'vlan'                : {
                                          'help_text'            : 'VLAN Associated with host',
                                          'json_serialize_string': False,
                                          'null'                 : True,
                                          'type'                 : 'IntegerField',
                                          'edit'                 : False,
                                          'searchable'           : True,
                                        },
                'dpid'                : {
                                           'edit'           : False,
                                           'type'           : 'ForeignKey',
                                           'rel_field_name' : 'dpid',
                                           'rel_obj_type'   : 'switches',
                                        },
                'ipv4'                : {
                                           'edit' : False,
                                        },
                # Attachment point isn't searchable
                #'attachment-points'  : {
                                           #'edit' : False
                                        #},
                'last-seen'           : {
                                          'help_text': 'Last time a packet was seen by the controller',
                                          'json_serialize_string': True,
                                          'null': True,
                                          'type': 'DateTimeField',
                                        },
                 },
            },


        'host-attachment-point' : {
            # directly fetched via rest api. 
            # 'has_rest_model' isn't set for this table.
            'source'          : 'controller',
            'url'             : 'device',

             'fields': {
                'mac'          : {
                                  'json_serialize_string': True,
                                  'null': True,
                                  'rel_field_name': 'mac',
                                  'rel_obj_type': 'host',
                                  'type': 'ForeignKey',
                                 },
                'vlan'         : {
                                   'help_text': 'VLAN Associated with host',
                                   'json_serialize_string': False,
                                   'null': True,
                                   'type': 'IntegerField',
                                   'edit' : False,
                                    'searchable' : True,
                                 },
                'id'           : {
                                  'help_text': '#|mac|dpid|ingress-port',
                                  'json_serialize_string': True,
                                  'max_length': 64,
                                  'null': False,
                                  'primary_key': True,
                                  'type': 'CharField',
                                 },
                'dpid'         : { 'json_serialize_string': True,
                                   'null': True,
                                   'rel_field_name': 'dpid',
                                   'rel_obj_type': 'switch',
                                   'type': 'ForeignKey',
                                 },
                'ingress-port' : {
                                  'help_text': 'Open flow port number of ingress port',
                                  'json_serialize_string': False,
                                  'null': True,
                                  'type': 'IntegerField',
                                 },
                'status'       : {
                                   'help_text': 'Error description, blank mean no error ',
                                   'json_serialize_string': True,
                                   'max_length': 64,
                                   'null': True,
                                   'type': 'CharField',
                                 },
                'last-seen'    : {
                                   'help_text': 'Last time a packet was seen by the controller',
                                   'json_serialize_string': True,
                                   'null': True,
                                   'type': 'DateTimeField',
                                 },
                'ipv4'         : { # added to enable searching
                                    'edit' : False,
                                    'searchable' : True,
                                 },
                 },
             },

        'host-network-address': {
            # directly fetched via rest api. 
            # 'has_rest_model' isn't set for this table.
            'source'          : 'switch',
            'url'             : 'device',

            'fields': {
                'mac'        : { 'json_serialize_string': True,
                                 'null': True,
                                 'rel_field_name': 'mac',
                                 'rel_obj_type': 'host',
                                 'type': 'ForeignKey',
                                 'edit' : False,
                               },
                'vlan'       : {
                                 'help_text': 'VLAN Associated with host',
                                 'json_serialize_string': False,
                                 'null': True,
                                 'type': 'IntegerField',
                                 'edit' : False,
                                 'searchable' : True,
                               },
                'id'         : {
                                 'help_text': '#|mac|ipv4',
                                 'json_serialize_string': True,
                                 'max_length': 64,
                                 'null': False,
                                 'primary_key': True,
                                 'type': 'CharField',
                               },
                'ip-address':  {
                                 'help_text': 'IP Address of host',
                                 'json_serialize_string': True,
                                 'max_length': 15,
                                 'null': True,
                                 'type': 'CharField',
                                 'edit' : False,
                              },
                'last-seen' : {
                                'help_text': 'Last time a packet was seen by the controller',
                                'json_serialize_string': True,
                                'null': True,
                                'type': 'DateTimeField',
                              },
                'ipv4'      : { # added to enable searching
                                 'max_length': 15,
                                 'null': True,
                                 'type': 'CharField',
                                 'edit' : False,
                              },
                'dpid'      : {
                                 'edit' : False,
                                 'searchable' : True,
                              },
                },
            },

        'host-vns-interface' : {
            # directly fetched via rest api. 
            # 'has_rest_model' isn't set for this table.
            'source'          : 'controller',
            'url'             : 'vns/device-interface',
            'cascade_delete'  : True,

            'field-orderings' : {
                'default'    : [ 'Idx', 'vns', 'host', 'ips',  ],
                'vns-config' : [ 'Idx',        'host', 'ips',  ]
                },
            'fields' : {
                'id'             : {
                                     'compound_key_fields': [
                                                              'vns',
                                                              'vlan',
                                                              'mac',
                                                              'interface'
                                                            ],
                                     'help_text': '#|host|interface',
                                     'json_serialize_string': False,
                                     'null': False,
                                     'primary_key': True,
                                     'type': 'compound-key',
                                   },
                'address-space'  : { 
                                     'json_serialize_string': True,
                                     'null': False,
                                     'rel_field_name': 'mac',
                                     'rel_obj_type': 'host',
                                     'type': 'ForeignKey',
                                     'edit' : False,
                                   },
                'vlan'           : {
                                     'type' : 'CharField',
                                     'max_length' : 32,
                                     'edit' : False
                                   },
                'mac'            : { 
                                     'json_serialize_string': True,
                                     'null': False,
                                     'rel_field_name': 'mac',
                                     'rel_obj_type': 'host',
                                     'type': 'ForeignKey',
                                     'edit' : False,
                                   },
                'interface':       {
                                     'json_serialize_string': True,
                                     'null': False,
                                     'rel_field_name': 'id',
                                     'rel_obj_type': 'vns-interface',
                                     'type': 'ForeignKey',
                                   },
                'ips'            : {
                                     'type' : 'CharField',
                                     'max_length' : 32,
                                     'edit' : False
                                   },
                }
            },

        'vns-interface' : {
            # directly fetched via rest api. 
            # 'has_rest_model' isn't set for this table.
            'source'        : 'controller',
            'url'           : 'vns/interface',

            'field-orderings' : {
                'default'    : ['Idx', 'vns', 'interface', 'rule', 'last-seen', ],
                'vns-config' : ['Idx',        'interface', 'rule', 'last-seen', ]
            },
            'fields' : {
                'id'         : {
                                'compound_key_fields': [ 'vns',
                                                         'interface'],
                                 'help_text': '#|vns|interface',
                                 'json_serialize_string': False,
                                 'null': False,
                                 'primary_key': True,
                                 'type': 'compound-key',
                               },
                'vns'        : {
                                'json_serialize_string': True,
                                'null': False,
                                'rel_field_name': 'id',
                                'rel_obj_type': 'vns-definition',
                                'type': 'ForeignKey',
                               },
                'interface'  : {
                                 'json_serialize_string': True,
                                 'max_length': 32,
                                 'null': False,
                                 'type': 'CharField',
                                },
                'rule'        : { 'json_serialize_string': True,
                                  'null': True,
                                  'rel_field_name': 'id',
                                  'rel_obj_type': 'vns-interface-rule',
                                  'type': 'ForeignKey',
                                },
                'last-seen'   : {
                                  'help_text': 'Last time a packet was seen by the controller on this interface',
                                  'json_serialize_string': True,
                                  'null': True,
                                  'type': 'DateTimeField',
                                },
                }
            }

        # done.
        }
