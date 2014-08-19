#!/usr/bin/python
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

import subprocess
import os, atexit
import sys, traceback                   # traceback.print_exc()
from optparse import OptionParser
from types import StringType
import datetime
import json
import traceback
import re
import time
import urllib2
import httplib                          # provides error processing for isinstance
import socket

from prettyprint import PrettyPrinter
from storeclient import StoreClient
from climodelinfo import CliModelInfo
from vendor import VendorDB

class ParamException(Exception):
    def __init__(self, value):
        self.value = value
    def __str__(self):
        return repr(self.value)

TIMESTAMP_HELP = \
    ('Enter an integer timestamp in milliseconds since the epoch or formatted\n' +
     'as one of the following options in your local timezone:\n'
     '"YYYY-MM-DD HH:MM:SS"      YYYY-MM-DDTHH:MM:SS\n' + 
     '"YYYY-MM-DD HH:MM:SS+TTT"  YYYY-MM-DDTHH:MM:SS+TTT\n' +
     'YYYY-MM-DD                 MM-DD\n' + 
     'HH:MM                      now')


class Validate():

    # initialize in init_obj_type_info
    obj_type_info_dict = {}         # see init_obj_type_info_dict
    obj_types = [] 
    obj_keys = {}
    
    known_controllers = ["127.0.0.1:8000"]
    controller = None
    # LOOK! manage this separately until we can for sure talk to eth0 addr instead of 127.0.0.1
    controller_for_prompt = "127.0.0.1:8000"
    cluster = "default"

    run = True

    # Cached options for last completion
    last_line = None
    last_options = None
    
    # Are we running in batch mode, i.e. "cat commands | cli.py"
    batch = False

    # used for parsing as an arg
    local_name_pattern = "localhost://([A-Za-z0-9_:@\-\.\/]+$)"

    # contained objects
    pp = None
    store = None
    model_handler = None

    #
    # --------------------------------------------------------------------------------
    # init_obj_type_info_dict
    #
    #  Builds the init_obj_type_info_dict, which is a dictionary indexed by
    #  the current mode, which also results in a dictionary, then indexed
    #  by a either a 'field' or 'field_ordering' key.
    #
    #  When init_obj_type_info_dict is indexed by the table/model name, the
    #  value is a dictionary.  That dictionary has several keys:
    #  - 'fields'
    #  - 'field_orderings'
    #
    #  The 'fields' indexed result returns a dictionary, each of which
    #  is indexed by the names of the fields within the table/model. further
    #  indicies describe details about that field within the table:
    #  - 'formatter' desscribes the prettyPrinter function for that field
    #  - 'primary_key' identifes this field as the key for the table/model.
    #  - 'verbose_name' identfies an additonal name for the field, inteneded
    #                   to be more descriptive
    #  - 'has_rest_model'  True/False
    #  - 'json_serialize_string'  True/False
    #  - 'help_text'  String providing more clues about the intent of this field
    #
    #  The 'field_ordering' dictionary associated with the name of the table/model
    #  is used to order the output.
    #
    #  Also populates the self.obj_keys[], which is a dictionary mapping the table/model
    #  name to the name of the storage key (table/model's column) for that table.
    #
    def init_obj_type_info_dict(self):
        self.cli_model_info = CliModelInfo()
        self.obj_type_info_dict = self.cli_model_info.get_complete_obj_type_info_dict()
        self.obj_types = [k for (k,v) in self.obj_type_info_dict.items()
                          if 'has_rest_model' in v]
        for (k,d) in self.obj_type_info_dict.items():
            candidate_keys = [f for f in d['fields'].keys() 
                              if d['fields'][f].get('primary_key', False)]
            if len(candidate_keys) > 0:
                self.obj_keys[k] = candidate_keys[0]


    #
    # --------------------------------------------------------------------------------
    # make_unit_formatter
    #
    def make_unit_formatter(self, units):
        return lambda x,y: '%s %s' % (x, units)

    #
    # --------------------------------------------------------------------------------
    # init_obj_type_info_dict
    #
    #  Builds the init_obj_type_info_dict, which is a dictionary indexed by
    #  the current mode, which also results in a dictionary, then indexed
    #  by a either a 'field' or 'field_ordering' key.
    #
    #  When init_obj_type_info_dict is indexed by the table/model name, the
    #  value is a dictionary.  That dictionary has several keys:
    #  - 'fields'
    #  - 'field_orderings'
    #
    #  The 'fields' indexed result returns a dictionary, each of which
    #  is indexed by the names of the fields within the table/model. further
    #  indicies describe details about that field within the table:
    #  - 'formatter' desscribes the prettyPrinter function for that field
    #  - 'primary_key' identifes this field as the key for the table/model.
    #  - 'verbose_name' identfies an additonal name for the field, inteneded
    #                   to be more descriptive
    #  - 'has_rest_model'  True/False
    #  - 'json_serialize_string'  True/False
    #  - 'help_text'  String providing more clues about the intent of this field
    #
    #  The 'field_ordering' dictionary associated with the name of the table/model
    #  is used to order the output.
    #
    #  Also populates the self.obj_keys[], which is a dictionary mapping the table/model
    #  name to the name of the storage key (table/model's column) for that table.
    #
    def init_obj_type_info_dict(self):
        self.cli_model_info = CliModelInfo()
        self.obj_type_info_dict = self.cli_model_info.get_complete_obj_type_info_dict()
        self.obj_types = [k for (k,v) in self.obj_type_info_dict.items()
                          if 'has_rest_model' in v]
        for (k,d) in self.obj_type_info_dict.items():
            candidate_keys = [f for f in d['fields'].keys() 
                              if d['fields'][f].get('primary_key', False)]
            if len(candidate_keys) > 0:
                self.obj_keys[k] = candidate_keys[0]

    #
    # --------------------------------------------------------------------------------
    # init
    #
    def init(self):
        
        self.vendordb = VendorDB()
        self.vendordb.init()
        
        parser = OptionParser()
        parser.add_option("-c", "--controller", dest="controller",
                          help="set default controller to CONTROLLER", 
                          metavar="CONTROLLER", default=self.controller)
        (options, args) = parser.parse_args()
        self.controller = options.controller
        if not self.controller:
            self.controller = "127.0.0.1:8000"

        if not sys.stdin.isatty():
            self.batch = True

        self.init_obj_type_info_dict()

        self.pp = PrettyPrinter(self.obj_type_info_dict)
        self.store = StoreClient()
        self.store.set_controller(self.controller)


    #
    # --------------------------------------------------------------------------------
    # parse_optional_parameters
    #
    def parse_optional_parameters(self, params, words):
        parsed = {}
        i = 0
        while i < len(words):
            word = words[i]
            possible = [x for x in params if x.startswith(word)]
            if len(possible) == 0:
                raise ParamException('unknown option: %s' % word)
            elif len(possible) > 1:
                raise ParamException('ambiguous option: %s\n%s' % 
                                     (word, "\n".join(possible)))
            else:
                param_name = possible[0]
                param = params[param_name]
                if (param['type'] == 'flag'):
                    parsed[param_name] = True
                else:
                    if i+1 < len(words):
                        argument = words[i+1]
                        if (param['type'] == 'string'):
                            parsed[param_name] = argument
                        elif (param['type'] == 'int'):
                            try:
                                parsed[param_name] = int(argument)
                            except ValueError:
                                raise ParamException('option %s requires ' + 
                                                     'integer argument'
                                                     % word)
                        elif (param['type'] == 'enum'):
                            arg_possible = [x 
                                            for x in param['values'] 
                                            if x.startswith(argument)]
                            if (len(arg_possible) == 0):
                                raise ParamException('option %s value must be in (%s)' % 
                                                     (word,", ".join(param['values'])))
                            elif (len(arg_possible) > 1):
                                raise ParamException('ambiguous option %s value:\n%s' % 
                                                     (word, "\n".join(arg_possible)))
                            else:
                                parsed[param_name] = arg_possible[0]
                        i += 1
                    else:
                        raise ParamException('option %s requires an argument' 
                                                 % word)
            i += 1
        return parsed

    #
    # --------------------------------------------------------------------------------
    # rest_error_to_dict
    #   Turn an exception into an error dictionary, which can later be printed.
    #   using rest_error_dict_to_message().  
    #
    def rest_error_to_dict(self, e, detail=None):
        errors = None
        # try to identifify validation requests
        if isinstance(e, httplib.BadStatusLine):
            errors = {'connection_error' : 'REST API server %s: '
                                           'server not running' %
                                           self.controller}
            return errors

        elif isinstance(e, urllib2.HTTPError):
            code = e.code
            error_returned = e.readline()

            if code == 404:
                if detail:
                    errors = {'not_found_error' : 'Not Found: %s' % detail}
                else:
                    errors = {'not_found_error' : 'Not Found: %s' % error_returned}
            elif code == 500 or code == 403:
                errors = {'connection_error' : 'REST API server %s unable to connect: '
                                               'Cassandra possibly not running' %
                                                self.controller}
            elif code == 400:
                try:
                    errors = json.loads(error_returned)
                except:
                    # if the error can't be converted into a dictionary, then imbed the complete
                    # errors as the value for a specific error key.
                    errors = {'error_result_error': "Can't convert returned error: %s" % error_returned}
                    pass
            else:
                errors = {'unknown_error': 'HttpError %s' % error_returned}
        else:
            errors = {'unknown_error': "Need error managmenet for error %s" % type(e)}

        return errors

    #
    # --------------------------------------------------------------------------------
    # rest_error_dict_to_message
    #   Turn an rest_error_dict returned from rest_error_to_dict
    #   into an error message which can ge printed.  Code assumes multiple errors
    #   won't occur; if a 'field_error' exists, for example, a 'model_error' won't
    #   also be posted in the error
    #
    def rest_error_dict_to_message(self, rest_error_dict):
        error_msg = ""
        if 'field_errors' in rest_error_dict:
            for (k,v) in rest_error_dict['field_errors'].items():
                error_msg += "Syntax error: field %s: %s" % (k,v)
            # traceback.print_stack(), to find out why the error occured
        elif 'model_error' in rest_error_dict:
            error_msg += "Error: %s" % rest_error_dict['model_error']
        elif 'not_found_error' in rest_error_dict:
            error_msg += "Error: %s" % rest_error_dict['not_found_error']
        elif 'connection_error' in rest_error_dict:
            error_msg += rest_error_dict['connection_error']
        elif 'error_result_error' in rest_error_dict:
            error_msg += rest_error_dict['error_result_error']
        elif 'unknown_error' in rest_error_dict:
            error_msg += rest_error_dict['unknown_error']
        else:
            error_msg = "REST API server on controller-node %s " % self.controller
            error_msg += "had %s error:\n" % rest_error_dict['error_type']
            error_msg += rest_error_dict['description']
        return error_msg
    
    #
    # --------------------------------------------------------------------------------
    # method_from_name
    #
    def method_from_name(self, name):
       return getattr(self, "validate_" + name.replace("-","_"), None)

    #
    # --------------------------------------------------------------------------------
    # validate_port
    #
    # - validate port foreign key (to switch)
    #
    def validate_port(self):
        error = None
        print "validate_port"

        # collect known switches
        switch_ids = None
        switch_key = self.obj_keys["switch"]
        try:
            switch_table = self.store.get_table_from_store("switch")
            switch_ids = [x[switch_key] for x in switch_table]
        except Exception, e:
            error = self.rest_error_to_dict(e)
            print self.rest_error_dict_to_message(error)
            pass
        
        if error:
            print "Unable to collect switch, no switch dpid validation for port table"
            error = None

        # collect known ports
        port_table = None
        port_key = self.obj_keys["port"]
        try:
            port_table = self.store.get_table_from_store("port")
        except Exception, e:
            error = self.rest_error_to_dict(e)
            print self.rest_error_dict_to_message(error)
            pass
        
        if error:
            print "Unable to collect ports"
            return
    
        for port in port_table:
            if not port_key in port:
                print "No port id in row"
            else:
                port_id = port[port_key]
                if not 'switch' in port:
                    print 'port %s No switch in port (foreign key)' % port_id

    #
    # --------------------------------------------------------------------------------
    # validate_switch
    #
    # - validate switch foreign key (to controller)
    #
    def validate_switch(self):
        print "validate_switch"
        error = None

        # collect known controllers
        controller_ids = None
        controller_key = self.obj_keys["controller-node"]
        try:
            controller_table = self.store.get_table_from_store("controller-node")
            controller_ids = [x[contoller_key] for x in controller_table if controller_key in controller_table]
        except Exception, e:
            error = self.rest_error_to_dict(e)
            print self.rest_error_dict_to_message(error)
            pass
        
        if error:
            print "Unable to collect controller, no controller validation for switches"
            error = None
        if len(controller_ids) == 0:
            print "Unable to collect any controller ids"

        # collect known ports
        switch_table = None
        switch_key = self.obj_keys["switch"]
        try:
            switch_table = self.store.get_table_from_store("switch")
        except Exception, e:
            error = self.rest_error_to_dict(e)
            print self.rest_error_dict_to_message(error)
            pass
        
        if error:
            print "Unable to collect switches"
            return

        if len(switch_table) == 0:
            print "switch table empty"
    
        for switch in switch_table:
            if not switch_key in switch:
                print "No switch id in row"
            else:
                switch_id = switch[switch_key]
                if not 'switch' in switch:
                    print 'switch %s No controller foreign key' % switch_id
                else:
                    controller = switch['controller']
                    if not controller in controller_ids:
                        print "switch %s missing controller (foreign key) %s " % (switch_id, controller)

    #
    # --------------------------------------------------------------------------------
    # validate_host_vns_interface
    #
    # - validate host-vns-interface foreigb key (to vns-interface)
    # - validate host-vns-interface foreigb key (to host)
    # - crack the id into three fragments, and validate each of the
    #   fragments references the expected componont (ie: matches the foreign key)
    #
    def validate_host_vns_interface(self):
        print "host_vns_interface"

        error = None
        # collect host's
        host_ids = None
        host_key = self.obj_keys["host"]
        try:
            host_table = self.store.get_table_from_store("host")
            host_ids = [x[host_key] for x in host_table]
        except Exception, e:
            error = self.rest_error_to_dict(e)
            print self.rest_error_dict_to_message(error)
            pass

        if error:
            print "Unable to collect hosts, no host-vns-interface host validation"
            error = None
        
        # collect vns-interfaces
        vns_interface_ids = None
        vns_interface_key = self.obj_keys["vns-interface"]
        try:
            vns_interface_table = self.store.get_table_from_store("vns-interface")
            vns_interface_ids = [x[vns_interface_key] for x in vns_interface_table]
        except Exception, e:
            error = self.rest_error_to_dict(e)
            print self.rest_error_dict_to_message(error)
            pass

        if error:
            print "Unable to collect vns-interfaces, no host-vns-interface validation for vns-interfaces"
            error = None

        # collect host-vns-interface
        host_vns_interface_ids = None
        host_vns_interface_key = self.obj_keys["host-vns-interface"]
        try:
            host_vns_interface_table = self.store.get_table_from_store("host-vns-interface")
            host_vns_interface_ids = [x[host_vns_interface_key] for x in host_vns_interface_table]
        except Exception, e:
            error = self.rest_error_to_dict(e)
            print self.rest_error_dict_to_message(error)
            pass

        if error:
            print "Unable to collect host-vns-interface"
            return

        if len(host_vns_interface_table) == 0:
            print "host_vns_interface_table empty"
    
        host_vns_interface_id = self.obj_keys['host-vns-interface']
        for host_vns_interface in host_vns_interface_table:
            if not host_vns_interface_id in host_vns_interface:
                print "host_vns_interface no primary key"
            this_host_vns_interface_id = host_vns_interface[host_vns_interface_id]
            if not 'host' in host_vns_interface:
                print "host_vns_interface %s no host foreign key" % this_host_vns_interface_id
            else:
                host_foreign_key = host_vns_interface['host']
                if not host_foreign_key in host_ids:
                    print "host_vns_interface %s foreign key %s references missing host" % \
                        (this_host_vns_interface_id, host_foreign_key)

            if not 'interface' in host_vns_interface:
                print "host_vns_interface %s no vns-interface foreign key %s" % \
                        (this_host_vns_interface_id, foreign_key)
            else:
                interface_foreign_key = host_vns_interface['interface']
                if not interface_foreign_key in vns_interface_ids:
                    print "host_vns_interface %s foreign key %s" % \
                        (this_host_vns_interface, interface_foreign_key)

            parts = this_host_vns_interface_id.split("|")
            if len(parts) != 3:
                print "host_vns_interface_id %d needs to be three fields split by '|'"
            else:
                if parts[0] != host_foreign_key:
                    print "host_vns_interface %s related host foreign key %s isn't part of id" % \
                        (this_host_vns_interface, host_foreign_key)
                # the interface_foreign_key should have two parts.
                interface_parts = interface_foreign_key.split('|')
                if len(interface_parts) != 2:
                    print "host_vns_interface %s related vns-interface foreign key %s " \
                          "needs to be two words split by '|'" % \
                          (this_host_vns_interface_id, interface_foreign_key)
                elif interface_parts[0] != parts[1]:
                    print "host_vns_interface %s related vns_interface foreign key %s " \
                          "doesn't match host id part %s" % \
                          (this_host_vns_interface, interface_part[0], parts[1])
                elif interface_parts[1] != parts[2]:
                    print "host_vns_interface %s related vns_interface foreign key %s " \
                          "doesn't match interface long name part %s" % \
                          (this_host_vns_interface, interface_part[1], parts[2])

    #
    # --------------------------------------------------------------------------------
    # validate_vns_interface
    #
    def validate_vns_interface(self):
        print "vns-interface"

    # --------------------------------------------------------------------------------
    # validate_vns_interface_rule
    #
    # - id exists, 
    # - each row has a foreign key
    # - each foreign key exists
    # - the id, which is a concatenation of vns name and row id, has the correct foreign key
    #
    def validate_vns_interface_rule(self):
        print "vns_interface_rule"

        error = None
        # collect known vns's
        try:
            vns_table = self.store.get_table_from_store("vns-definition")
        except Exception, e:
            error = self.rest_error_to_dict(e)
            pass
        if error:
            print "Unable to collect vns-definition"
            return

        # collect known switches
        switch_ids = None
        switch_key = self.obj_keys["switch"]
        try:
            switch_table = self.store.get_table_from_store("switch")
            switch_ids = [x[switch_key] for x in switch_table]
        except Exception, e:
            error = self.rest_error_to_dict(e)
            print self.rest_error_dict_to_message(error)
            pass

        if error:
            print "Unable to collect switch, no switch dpid validation for vns rules"
            error = None

        try:
            vns_interface_rules_table = self.store.get_table_from_store("vns-interface-rule")
        except Exception, e:
            error = self.rest_error_to_dict(e)
            pass
        if error:
            print "Unable to collect vns-interface-rule"
            return
        vns_id = self.obj_keys["vns-interface-rule"]

        for rule in vns_interface_rules_table:
            if not vns_id in rule:
                print "rule has missing rule id"
            rule_id = rule[vns_id]
            parts = rule_id.split("|")
            if len(parts) < 2:
                print "rule %s has invalid id" % rule_id
            vns_part = parts[0]
            if not 'vns' in rule:
                print "rule %s has missing vns foreign key" % rule_id
            else:
                if rule['vns'] != vns_part:
                    print "rule %s has inconsistent vns foreign key: %s" % (rule_id, rule['vns'])
            if 'ports' in rule and not 'switch' in rule:
                print "rule %s has a ports field populated but no switch" % rule_id
            elif 'switch' in rule and not 'ports' in rule:
                print "rule %s has a switch field populated but no switch" % rule_id
            if 'switch' in rule and not rule['switch'] in switch_ids:
                print "rule %s has an unknown switch dpid %s" % (rule_id, rule['switch'])
                

    #
    # --------------------------------------------------------------------------------
    # validate
    #
    def validate(self):
        print "store validation"

        tables = self.obj_type_info_dict.keys()
        for table in tables:
            method = self.method_from_name(table)
            if method:
                method()
            

#
# --------------------------------------------------------------------------------
# Initialization crazyness to make it work across platforms. Many platforms don't include
# GNU readline (e.g. mac os x) and we need to compensate for this

import sys
try:
    import readline
except ImportError:
    try:
        import pyreadline as readline
    except ImportError:
        print "Can't find any readline equivalent - aborting."
else:
    import rlcompleter
    if(sys.platform == 'darwin'):
        # needed for Mac, please fix Apple
        readline.parse_and_bind ("bind ^I rl_complete")
    else:
        readline.parse_and_bind("tab: complete")
        readline.parse_and_bind("?: possible-completions")

  
#
# --------------------------------------------------------------------------------
# Run the shell
 
def main():
    # Uncomment the next two lines to enable remote debugging with PyDev
    #import pydevd
    #pydevd.settrace()
    validate = Validate()
    validate.init()
    validate.validate()              

if __name__ == '__main__':
    main()
