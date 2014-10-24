#!/usr/bin/python
#
# Copyright (c) 2010,2011,2012,2013 Big Switch Networks, Inc.
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
# --------------------------------------------------------------------------------
#
# LOOK! TODO --
#
# create tokens for modify actions in StaticFlowEntryPusher and here
#
# extend completion to show help text
#
# need to verify we can talk to the controller
#
# flow-entry naming - unique across all switches?
#
# joins on show commands (port names, ids, etc.)
#
# accept enums in addition to numbers - "ether-type arp" or "ether-type ip"
#
# cli commands not mapping to database model (this is an -undo-)
#
# simple options/singletons in the CLI ("logging level 5" "twitter username blah")
#
# csv, left/right justification in columns, sorts
#
# completion with multi command lines
#
# disallow 'switch' field and name as options flow-entry ('key' fields in general)
#
# delayed commit semantics (depends on 1?)
#
# delete config files?
#
# --------------------------------------------------------------------------------
#
# Notes:
#
#  support functions for a command, consider, for example, 'vns-definition'
#
#   For a new command, for example 'vns-definition', there will be a variety
#   of different support functions to manage different required functionaly.
#   do_vns_definition() describes what to do to execute the actual command.
#   cp_vns_definition() describes how to perform command completion for the same.
#   do_show_vns_definition() would describe how to show the vns-definitions
#     (this procedure doesn't exist, but is implemented by 'show vns' via
#     do_show_vns), while
#   cp_show_vns_definition() would describe how to complete the show command for
#   this particular command.  Other functions may also be needed, for example
#   specialized changes to the 'no' command for the same command.
#
#  command completion
#
#   currently in many cases, there's a specialized completion handler
#   it would make sense to manage 'no' completion by overloading the
#   same completion handler.
#
#
#  error processing:
#
#   Keep in mond that al errors ought to use a similar prefix so that
#   all the error conditions acan be identifyed by the automated test
#   procedures.  these prefixes are regular expressions in bigtest/cli.py
#   The prefixed currently recognized as errors are 'Error: Not Found",
#   'Error running command ', 'Syntax error:', and 'Syntax: ' the last
#   of which is used to display both usage for some commands, and 
#   various specific syntax errors
#
#  alias management:
#
#   Aliases are managed by having a separate table/model for each
#   of the aliases, for example the host table has an associated alias
#   table called 'host-alias'.  These alias models are nothing more
#   than an id, which is the alias name and a foreign key for the other
#   table, in the 'host-alias' model, the associated foreign key would 
#   point to the host.
#   
#   Alias tables or models are not directly configured as alias tables,
#   the cli can find these tables by noticing the only fields within
#   the model is a foreign key for another table.   The cli builds the
#   alias_obj_type_xref dictionary during initialization, and later uses
#   that association to manage aliases.   Knowing which of the tables
#   are alias tables also allows the cli to exclude these tables from
#   modification in typical config mode.
#
#   Alias are created when within the submode for a particular obj_type,
#   for example, the host-alias table is available within the host
#   config submode, and it appears as the 'host-alias' table there.
#   However no further submode for the host-alias is managed; it is
#   treated as if it were a field in the submode config object.
#   So if 'host-alias xxx' were entered while in host config submode,
#   the cli converts the request into a host-alias table entry.
#
#   Feature management.  The cli is intended to manage the availability
#   of specific feature groups.  This isn't meant to prevent a user from
#   getting at specific sku features (ie: licensing), but instead meant
#   to allow a user to disable specific sub-groups of commands to 
#   prevent unintended enabling of features.  For example,
#   to prevent a 'static-flow' cli configured controller from enabling
#   vns features likely resulting in a misconfigured environment.
#
#  changes:
#
#   -- new source field for obj_types in models, obj_types which are
#      written based on discovered details are typically not editable.
#      don't allow a config submode entry. and don't
#      advertise such an entry is possible
#
#   -- integrated alias support, no special cases through the use
#      of a separated alias tables.  this allows more integrated
#      cli support for tables specifically constructed to manage
#      alias names for other fields.   Currently code in the cli
#      prevents multiple alias for a single foreign key, but this
#      behavior can be relaxed (perhaps it ought to be configurable?)
#
#   -- perform alias substitution for 'no' commands obj_type's
#
#   -- support for primary key/id changes within a row.
#      if an update occurs which modifies the primary key of a row,
#      the cli now identifies whether any foreign keys point at
#      the existing entry, and updates the foreign key to reference
#      the updated primary key.
#
#   -- support for cascaded delete.
#      when a row is deleted, if there are foreign keys associated
#      with the row, the cli will remove them.  the climodelinfo
#      configures which obj_types are included in the cascade deletes.
#
#   -- vns interface creation.
#      vns interfaces are required to have a vns-interface-rule associated.
#      sdnplatform writes both the interface name and the rule, but the cli needed
#      to have code added to determine and validate the associated rule.
#      The validation attempts to ensure the correct rule is associated with
#      the use of the interface name (id).
#

import subprocess
import os, atexit, stat
import sys, traceback                   # traceback.print_exc()
from optparse import OptionParser
from types import StringType
import collections
import datetime
import json
import re
import time
import urllib2
import httplib                          # provides error processing for isinstance
import socket
import select
import fcntl
import posixpath
import random
import copy
import utif
import fmtcnv
import run_config
import imp
import locale

from pkg_resources import resource_filename
from modi import Modi
from midw import *
from vnsw import *
from prettyprint import PrettyPrinter
from storeclient import StoreClient
from climodelinfo import CliModelInfo
from vendor import VendorDB
import error
import command
import rest_to_model
import tech_support
import url_cache
import doc
import sdndb

#
# --------------------------------------------------------------------------------
# 

class ParamException(Exception):
    def __init__(self, value):
        self.value = value
    def __str__(self):
        return repr(self.value)

class TooManyVNSException(Exception):
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


# This is a bit of a hack. The completion and validation functions for time zones
# are static methods, but they need to access the SDNSh object to get at the
# controller field to be able to do a REST call to load the list of time zone
# strings. So we set up a global for the single SDNSh instance we have. An
# alternative would have been to have a global variable for the list of time
# zone strings that's initialized when the CLI is initialized, but it seems
# nicer to only load it on demand, especially since setting the time zone
# will be a pretty infrequently invoked command.
cli = None

#
# Constants for packet tracing
#
SESSIONID = 'sessionId'
FT_VNS = 'vns'
FT_PORT = 'port'
FT_PORT_DPID = 'dpid'
FT_PORT_PORT = 'port'
FT_OUTPUT = 'output'
FT_DIRECTION = 'direction'
FT_PERIOD = 'period'
FT_TIMEOUT = "FilterTimeout"
FT_PERIOD_DEFAULT = 300

onos=1
#
# --------------------------------------------------------------------------------
# 
class NotFound(Exception):
    def __init__(self, obj_type, name):
        self.obj_type = obj_type
        self.name = name
    def __str__(self):
        return "Not Found: %s %s" % (self.obj_type, self.name)


# LOOK!: The next two functions are copied from the sdncon code.
# Should ideally figure out a way to share the code for utility
# functions like this.

def dotted_decimal_to_int(ip):
    """
    Converts a dotted decimal IP address string to a 32 bit integer
    """
    bytes = ip.split('.')
    ip_int = 0
    for b in bytes:
        ip_int = (ip_int << 8) + int(b)
    return ip_int
    

def same_subnet(ip1, ip2, netmask):
    """
    Checks whether the two ip addresses are on the same subnet as
    determined by the netmask argument. All of the arguments are
    dotted decimal IP address strings.
    """
    ip1_int = dotted_decimal_to_int(ip1)
    ip2_int = dotted_decimal_to_int(ip2)
    netmask_int = dotted_decimal_to_int(netmask)
    return (ip1_int & netmask_int) == (ip2_int & netmask_int)

#
# --------------------------------------------------------------------------------
# 

class SDNSh():

    debug = False               # general cli debugging
    debug_backtrace = False     # backtrace on failures
    description = False         # help debug command descriptions
    display_rest = False        # display rest call details
    display_reply_rest = False  # display rest call replies details

    command_dict = {}
    mode_stack = []
    reserved_words = ['unknown', 'all']

    known_controllers = ["127.0.0.1:8000"]
    controller = None
    # LOOK! manage this separately until we can cetainly
    #       talk to eth0 addr instead of 127.0.0.1
    controller_for_prompt = "127.0.0.1:8000"
    cluster = "default"

    run = True

    # Are we running in batch mode, i.e. "cat commands | cli"
    batch = False

    # used for parsing as an arg
    local_name_pattern = "config://([A-Za-z0-9_:@\-\.\/]+$)"

    # name of boot/startup-config
    boot_config_filename = '/opt/sdnplatform/run/boot-config'
    saved_configs_dirname = '/opt/sdnplatform/run/saved-configs/'
    feature_allow_experimental = '/opt/sdnplatform/feature/experimental'

    # contained objects
    pp = None
    store = None
    model_handler = None

    # cached metadata
    stats_metadata = None
    stats_type_metadata = None
    stats_restapi_map = {'controller-node': 'controller'}

    stats_optional_params = {
        'start-time': {'type': 'string',
                       'syntax_help': TIMESTAMP_HELP},
        'end-time': {'type': 'string',
                     'syntax_help': TIMESTAMP_HELP},
        'duration': {'type': 'string',
                     'syntax_help': 
                     "Enter an integer followed by a time unit\n" +
                     "[integer]weeks  [integer]days  [integer]hours\n" +
                     "[integer]mins   [integer]secs  [integer]ms"},
        'sample-interval': {'type': 'int',
                            'syntax_help': 'Enter an integer number of milliseconds'},
        'sample-count': {'type': 'int',
                         'syntax_help': 'Enter an integer target sample count'},
        'sample-window': {'type': 'string',
                          'syntax_help': 'Enter an integer number of milliseconds for sampling window'},
        'data-format': {'type': 'enum', 'values': ['value', 'rate']},
        'display': {'type': 'enum', 'values': ['latest-value', 'graph', 'table']},
        #'limit': {'type': 'int'}
        }

    netvirt_feature_enabled_cached = False
    warning_suppress = False            #

    #
    # --------------------------------------------------------------------------------
    # warning
    #  When a config is getting replayed, warnings are suporessed.  Other situations
    #  may also require supression of warnings.
    #
    def warning(self, message):
        if not self.warning_suppress:
            print "Warning: %s" % message
    
    #
    # --------------------------------------------------------------------------------
    # supress_warnings(self)
    #
    def suppress_warnings(self):
        self.warning_suppress = True

    #
    # --------------------------------------------------------------------------------
    # enable_warnings(self)
    #
    def enable_warnings(self):
        self.warning_suppress = False


    config_replay = False               # only true while replaying config files
    #
    # --------------------------------------------------------------------------------
    # config_replay_start
    #  Behavior is different during config replay.  Warnings are
    #  suppressed, and other error conditions may be relaxed, for example
    #  the switch submode interface command requires that the interface exist,
    #  and the interface names are discovered by sdnplatform, which means that during
    #  config replay for statup, an unknown-named-interface may need to be accepted
    #  since that interface may appear once the switch connected to sdnplatform, and
    #  sdnplatform writes the ports for this switch.
    #
    def config_replay_start(self):
        self.suppress_warnings()
        self.config_replay = True
    
    #
    # --------------------------------------------------------------------------------
    # config_replay_done
    #
    def config_replay_done(self):
        self.enable_warnings()
        self.config_replay = False

    #
    # --------------------------------------------------------------------------------
    # config_replay_active
    #  Returns true when a configuration is getting replayed
    #
    def config_replay_active(self):
        return self.config_replay

    #
    # --------------------------------------------------------------------------------
    # note
    #  When a config is getting replayed, warnings/notes are suporessed.  Other situations
    #  may also require supression of warnings.
    #
    def note(self, message):
        if not self.warning_suppress:
            print "Note: %s" % message

    #
    # --------------------------------------------------------------------------------
    # debug_msg
    #  Debugging message cover.  Only enabled for display once 'debug cli' is performed.
    #
    def debug_msg(self, message):
        if self.debug:
            print "debug_msg: %s" % message
            
    #
    # --------------------------------------------------------------------------------
    # error_msg
    #  Error message cover, to ensure consistent error messages
    #
    @staticmethod
    def error_msg(message):
        return "Error: %s" % message

    #
    # --------------------------------------------------------------------------------
    # syntax_msg
    #  Syntax error message cover, to ensure consistent error messages
    #
    @staticmethod
    def syntax_msg(message):
        return "Syntax: %s" % message 

    #
    # --------------------------------------------------------------------------------
    # completion_error_msg
    #  Error message cover during completion, to ensure consistent error messages
    #
    def completion_error_msg(self, message):
        self.print_completion_help("Error: %s" % message)

    #
    # --------------------------------------------------------------------------------
    # completion_syntax_msg
    #  Syntax error message cover during completion, to ensure consistent error messages
    #
    def completion_syntax_msg(self, message):
        self.print_completion_help("Syntax: %s" % message)

    #
    # --------------------------------------------------------------------------------
    # init_history_file
    #
    def init_history_file(self):
        history_file = os.path.join(os.environ["HOME"], ".sdnsh_history")
        # create the initial history file group-writable,
        # so that the tacacs users can write to it
        if os.path.exists(history_file):
            st = os.stat(history_file)
            if not (st[stat.ST_MODE] & stat.S_IWGRP):
                buf = open(history_file).read()
                os.rename(history_file, history_file + "~")
                mask = os.umask(007)
                fno = os.open(history_file, os.O_WRONLY | os.O_CREAT, 0660)
                os.umask(mask)
                os.write(fno, buf)
                os.close(fno)
        else:
            mask = os.umask(007)
            fno = os.open(history_file, os.O_WRONLY | os.O_CREAT, 0660)
            os.umask(mask)
            os.close(fno)
        try:
            readline.read_history_file(history_file)
        except IOError:
            pass
        atexit.register(readline.write_history_file, history_file)
    
    #
    # --------------------------------------------------------------------------------
    # init_command_dict
    #
    def init_command_dict(self):

        #
        # command_submode_dict saves a little information about
        # submode commands.  The index is 'mode', and the resulting
        # list contains strings, each of which is a submode command
        # available in that mode.
        self.command_submode_dict = {}

        #
        # associate command names with features, so that when
        # help is provided, command names which have associated
        # features are validated to determine whether the feature
        # is enabled or not.
        self.command_name_feature = {}

        #
        # command_nested_dict is indexed by mode, and contains
        # command configured with a trailing '*', for example
        # 'mode' : 'config-*', intended to mean "all nested
        # submodes after config-'.  This can't be computed until
        # all submodes are known, which means all the command
        # describption must be configured., for these to make sense.
        self.command_nested_dict = {}

        #
        # config mode commands
        #
        #self.command_dict['config'] = [ "boot" ]

        #
        # commands which start at 'login'
        #
        if onos == 0:
            self.command_nested_dict['login'] = [ 'show', 'logout', 'exit',
                                              'history', 'help', 'echo',
                                              'date', 'trace', 'traceroute',
                                              'ping', 'test', 'version',
                                              'connect', 'watch', 'no' ]
        else:
            self.command_nested_dict['login'] = [ 'show', 'logout', 'exit',
                                              'history', 'help', 'echo',
                                              'date', 'trace',
                                              'ping', 'test', 'version',
                                              'connect', 'watch', 'no' ]

        self.command_nested_dict['enable'] = [ 'clear', 'end' ]

        #self.command_dict['config-internal'] = ['lint', 'permute']

    #
    # --------------------------------------------------------------------------------
    # make_unit_formatter
    #
    @staticmethod
    def make_unit_formatter(units):
        return lambda x, y: '%s %s' % (x, units)

    #
    # --------------------------------------------------------------------------------
    # init_stats_metadata
    #
    def init_stats_metadata(self):
        # Initialize the stats metadata by pulling it from the rest api
        json_data = self.rest_simple_request_to_dict('http://%s/rest/v1/stats/metadata/%s/' % \
                                                     (self.controller, self.cluster))
        metadata = {}
        fields = {}
        for item in json_data:
            # XXX Requesting this stat causes a long delay in reading; bug in sdncon
            # once this is fixed we can add back in; also add to field_orderings below
            #if json_data[item]['name'] == 'OFActiveFlow':
            #    continue

            if json_data[item]['target_type'] in metadata:
                metadata[json_data[item]['target_type']].append(json_data[item])
            else:
                metadata[json_data[item]['target_type']] = [json_data[item]]
                fields[json_data[item]['target_type']] = {}

            formatter = self.make_unit_formatter(json_data[item]['units'])
            fields[json_data[item]['target_type']][json_data[item]['name']] = \
                {'verbose-name': json_data[item]['verbose_name'],
                 'units': json_data[item]['units'],
                 'formatter': formatter}

        self.stats_metadata = json_data
        self.stats_type_metadata = metadata

        self.pp.add_format('STATS-CONTROLLER',
                           'cli.init_stats_metadata()', # origina
                           {'stats-controller' : {
                'field-orderings' : {'default': 
                                     ['cpu-user', 'cpu-nice', 'cpu-system', 
                                      'cpu-idle'
                                      'mem-free', 'mem-used', 'swap-used',
                                      'disk-root', 'disk-log', 'disk-boot',
                                      'sdnplatform-cpu', 'database-cpu', 
                                      'apache-cpu', 'cli-cpu', 'statd-cpu']},
                'fields' : fields['controller']
                }})

        self.pp.add_format('STATS-SWITCH',
                           'cli.init_stats_metadata()', # origina
                           { 'stats-switch' : {
                'field-orderings' : {'default': 
                                     ['OFPacketIn', 'OFFlowMod', 'OFActiveFlow']},
                'fields' : fields['switch']
                }})
                           
        for obj_type in fields:
            for field in fields[obj_type]:
                for data_format in ['rate', 'value']:
                    units = fields[obj_type][field]['units']
                    if data_format == 'rate':
                        units += '/s'
                    self.pp.add_format('STATS-%s-%s-%s' % (obj_type,field,data_format), 
                                       'cli.init_stats_metadata()',
                            { 'stats-%s-%s-%s' % (obj_type,field,data_format) : { \
                            'field-orderings' : {'default': ['value', 'timestamp']},
                            'fields' : {'timestamp': {'verbose-name': 'Timestamp',
                                                      'formatter': 
                                                      lambda x,y: time.ctime(x/1000)},
                                        'value': {'units': units,
                                                  'verbose-name': 
                                                  '%s (%s)' % 
                                                  (fields[obj_type][field]['verbose-name'],
                                                   units)}}
                            }})

    #
    # --------------------------------------------------------------------------------
    #
    def command_packages_path(self):
        command_descriptions = 'desc'
        desc_path = resource_filename(__name__, command_descriptions)
        if desc_path:
            # print "desc_path %s" % desc_path
            return desc_path
        desc_path = os.path.join(os.path.dirname(__file__), command_descriptions)
        if os.path.exists(desc_path):
            return desc_path
        return None


    #
    # --------------------------------------------------------------------------------
    #
    def command_packages_exists(self, version):
        desc_path = self.command_packages_path()
        if desc_path == None:
            return None
        version_path = os.path.join(desc_path, version)
        if os.path.exists(version_path):
            return version_path
        return None


    #
    # --------------------------------------------------------------------------------
    #
    def add_command_packages(self, version):
        """
        Add all command and output format components

        """
        desc_path = self.command_packages_path()
        if desc_path == None:
            print 'No Command Descriptions subdirectory'
            return

        for path_version in [x for x in os.listdir(desc_path) if x.startswith(version)]:
            print path_version
            result_tuple = imp.find_module(path_version, [desc_path])
            imp.load_module(version, result_tuple[0], result_tuple[1], result_tuple[2])
            new_path = result_tuple[1]
            cmds_imported = []
            for cmds in os.listdir(new_path):
                (prefix, suffix) = os.path.splitext(cmds)
                if (suffix == '.py' or suffix == '.pyc') and prefix not in cmds_imported and prefix != '__init__':
                    cmds_imported.append(prefix)
                    result_tuple = imp.find_module(prefix, [new_path])
                    module = imp.load_module(prefix, result_tuple[0], result_tuple[1], result_tuple[2])
                    command.add_commands_from_module(version, module, self.dump_syntax)
                    self.pp.add_formats_from_module(version, module)
            # print cmds_imported

    #
    # --------------------------------------------------------------------------------
    #
    def choices_text_builder(self, matches, max_len = None, col_width = None):
        """
        @param max_len integer, describes max width of any entry in matches
        @param col_width integer, colun width
        """
        # Sort the choices alphabetically, but if the token ends
        # in a number, sort that number numerically.
        try:
            entries = sorted(matches, utif.completion_trailing_integer_cmp)
        except Exception, e:
            traceback.print_exc()

        if col_width == None:
            # request to look it up
            (col_width, line_length) = self.pp.get_terminal_size()
            col_width = min(120, col_width)

        if max_len == None:
            # request to compute the max length
            max_len = len(max(matches, key=len))

        count = len(matches)
        max_len += 1 # space after each choice
        if max_len > col_width:
            # one per line?
            pass
        else:
            per_line = col_width / max_len
            lines = (count + (per_line - 1)) / per_line
            if lines == 1:
                return ''.join(['%-*s' % (max_len, m) for m in entries])
            else:
                # fill the columns, which means skipping around the entries
                result = []
                for l in range(lines):
                    result.append(['%-*s' % (max_len, entries[i])
                                   for i in range(l, count, lines)])
                lines = [''.join(x) for x in result]
                return '\n'.join(lines)


    #
    # --------------------------------------------------------------------------------
    #
    def matches_hook(self, subs, matches, max_len):

        # one shot disabler, used to disable printing of completion
        # help for two-column help display (for '?' character). 
        # completion printing here can only display the possible selections,
        # for two-column mode, the reason why each keyword was added
        # needs to be displayed, which is no longer available here.
        if self.completion_print == False:
            self.completion_print = True
            return
        
        choices_text = self.choices_text_builder(matches, max_len)
        self.print_completion_help(choices_text)

    #
    # --------------------------------------------------------------------------------
    #
    def pre_input_hook(self):
        """
        """
        pass

    
    #
    # --------------------------------------------------------------------------------
    # 
    def desc_version_to_path_elem(self, version):
        """
        Version numbers like 1.0 need to be converted to the path
        element associated with the number, like version100
        """
        try:
            version_number = float(version)
            version = 'version%s' % int(version_number * 100)
        except:
            pass

        # temporary, use until version100 exists.
        if version == 'version100' and not self.command_packages_exists(version):
            version = 'version200'

        return version
        

    #
    # --------------------------------------------------------------------------------
    # init
    #
    def init(self):
        global mi
        
        self.completion_print = True
        self.vendordb = VendorDB()
        self.vendordb.init()

        parser = OptionParser()
        parser.add_option("-c", "--controller", dest="controller",
                          help="set default controller to CONTROLLER", 
                          metavar="CONTROLLER", default=self.controller)
        parser.add_option("-S", "--syntax", dest='dump_syntax',
                          help="display syntax of loaded commands",
                          action='store_true', default=False)
        parser.add_option("-i", "--init", dest='init',
                          help="do not perform initialization checks",
                          action='store_true', default=False)
        parser.add_option("-d", "--debug", dest='debug',
                          help='enable debug for cli (debug cli)',
                          action='store_true', default=False)
        parser.add_option("-v", "--version", dest='desc_version',
                          help='select command versions (description group)',
                          default=None)
        parser.add_option('-m', "--mode", dest='starting_mode',
                          help='once the cli starts, nest into this mode')
        parser.add_option('-q', "--quiet", dest='quiet',
                          help='suppress warning messages',
                          action='store_true', default=False)
        (self.options, self.args) = parser.parse_args()
        self.controller = self.options.controller
        if not self.controller:
            self.controller = "127.0.0.1:8000"
        self.dump_syntax = self.options.dump_syntax
        if not self.dump_syntax:
            self.dump_syntax = False
        self.debug = self.options.debug

        # command option, then env, then default
        self.desc_version = self.options.desc_version
        if self.desc_version == None:
            self.desc_version = os.getenv('CLI_COMMAND_VERSION')
        if self.desc_version == None:
            self.desc_version = '2.0'   # 'version200'

        if self.desc_version:
            self.desc_version = self.desc_version_to_path_elem(self.desc_version)

        self.length = 0 # screen length.

        self.set_controller_for_prompt()

        if not sys.stdin.isatty():
            self.batch = True

        self.store = StoreClient()
        self.store.set_controller(self.controller)

        mi = Modi(self, CliModelInfo())
        self.mi = mi
        self.init_command_dict()

        self.completion_reset()
        self.completion_skip = False
        self.completion_cache = True
        readline.set_completer(self.completer)
        readline.set_completer_delims("\t ")
        # readline.set_pre_input_hook(self.pre_input_hook)
        readline.set_completion_display_matches_hook(self.matches_hook)
        if not self.batch:
            self.init_history_file()
        self.push_mode("login")

        # starting mode, 
        starting_mode = self.options.starting_mode
        if starting_mode == None:
            starting_mode = os.getenv('CLI_STARTING_MODE')

        if starting_mode:
            if starting_mode == 'login':
                pass
            elif starting_mode == 'enable':
                self.push_mode("enable")
            elif starting_mode == 'config':
                self.push_mode("enable")
                self.push_mode("config")
            else:
                print 'Only login, enable or config allowed as starting modes'

        # process quiet option
        quiet = self.options.quiet
        if quiet == None:
            quiet = os.getenv('CLI_SUPPRESS_WARNING')

        if quiet:
            self.supress_warnings()

        #
        self.pp = PrettyPrinter(self)

        #
        # pattern matches
        #
        self.IP_ADDR_RE = re.compile(r'^(\d{1,3}\.){3}\d{1,3}$')
        self.CIDR_RE = re.compile(r'^((\d{1,3}\.){3}\d{1,3})/(\d{1,2}?)$')
        self.MAC_RE = re.compile(r'^(([A-Fa-f\d]){2}:?){5}[A-Fa-f\d]{2}$')
        self.DPID_RE = re.compile(r'^(([A-Fa-f\d]){2}:?){7}[A-Fa-f\d]{2}$')
        self.ACL_RE = re.compile(r'^\d+$')  # just the leading digits
        self.DIGITS_RE = re.compile(r'^\d+$')
        self.HEX_RE = re.compile(r'^0x[0-9a-fA-F]+$')

        # Initialize any middleware layers
        init_midware(self, mi)
        init_vnsw(self, mi)
        rest_to_model.rest_to_model_init(self, mi)
        run_config.init_running_config(self, mi)

        # Initialize all doc tags, use a subdirectory based on
        # the locale.   This may be incorect if there's nothing
        # in the configured locale for the returned locale value.
        lc = locale.getdefaultlocale()
        if lc == None or lc[0] == None:
            print 'Locale not configured ', lc
            lc = ('en_US', 'UTF8')
        doc_dir = resource_filename(__name__, 'documentation/%s' % lc[0])
        if doc_dir is None:
            doc_dir = os.path.join(os.path.dirname(__file__), 'documentation', lc[0])
        # print "doc_dir %s" % doc_dir
        doc.add_doc_tags(doc_dir)

        # Initialize the command module, than add command packages
        command.init_command(self, mi)
        self.add_command_packages(self.desc_version)

        # save for possible later use
        #print self.pp.format_table(self.pp.format_details())
        
        #
        if self.debug:
            for (n,v) in self.command_submode_dict.items():
                print "SUBMODE %s %s" % (n,v)
            for (n,v) in self.command_dict.items():
                print "MODE %s %s" % (n,v)


        self.time_zone_list = None
        command.add_completion('time-zone-completion', SDNSh.time_zone_completion)
        command.add_validation('time-zone-validation', SDNSh.time_zone_validation)
        command.add_action('set-clock', SDNSh.set_clock_action, {'kwargs': {'data': '$data'}})
        command.add_action('begin-default-gateway-check', SDNSh.begin_default_gateway_check_action)
        command.add_action('end-default-gateway-check', SDNSh.end_default_gateway_check_action)
    
        command.add_command_type('config-with-default-gateway-check', {
            'action': ('begin-default-gateway-check', 'write-fields', 'end-default-gateway-check'),
            'no-action': ('begin-default-gateway-check', 'reset-fields', 'end-default-gateway-check')
        })

        #
        if self.feature_enabled('experimental'):
            print '***Warning: experimental features enabled***'
            if self.debug:
                self.sdndb = sdndb.SDNDB(self, self, self.pp)
                print '--BEGIN--'
                self.sdndb.display('core/switch', style='table')
                self.sdndb.display('core/controller', style='table')
                self.sdndb.display('core/controller', style='list')
                print '--LINK--', self.pp.table_info['link']['field-orderings']
                self.sdndb.display('topology/link')
                self.sdndb.display('topology/switch-cluster')
                self.sdndb.display('topology/switch-cluster', style='list')
                self.sdndb.display('topology/enabled-port')

    #
    # --------------------------------------------------------------------------------
    # check_rest_result
    @staticmethod
    def check_rest_result(result, message=None):
        if isinstance(result, collections.Mapping):
            error_type = result.get('error_type')
            if error_type:
                raise error.CommandRestError(result, message)

    #
    # --------------------------------------------------------------------------------
    # get_table_from_store
    #  Caches the last result to try to cut down on the number of times large tables
    #  are loaded from the REST API
    #
    def get_table_from_store(self, obj_type, key = None, val = None, match = None):
        return self.store.get_table_from_store(obj_type, key, val, match)

    #
    # --------------------------------------------------------------------------------
    # get_object_from_store
    #   cover for call to get a specific object from a table, only a convenience.
    #
    def get_object_from_store(self, obj_type, obj_name):
        return self.store.get_object_from_store(obj_type, obj_name)

    #
    # --------------------------------------------------------------------------------
    # rest_query_objects
    #   cover for call to query objects from the database, only a convenience.
    #
    def rest_query_objects(self, obj_type, query_params=None):
        return self.store.rest_query_objects(obj_type, query_params)

    #
    # --------------------------------------------------------------------------------
    # rest_create_object
    #  REST API call to create an object, if this is the last table which
    #  was fetched, then discard that table.
    #
    def rest_create_object(self, obj_type, ident):
        return self.store.rest_create_object(obj_type, ident)

    #
    # --------------------------------------------------------------------------------
    # rest_delete_objects
    #
    def rest_delete_objects(self, obj_type, delete_params):
        return self.store.rest_delete_objects(obj_type, delete_params)

    #
    # --------------------------------------------------------------------------------
    # rest_delete_object
    #
    def rest_delete_object(self, obj_type, name):
        if type(name) == dict:
            return self.store.rest_delete_object(obj_type, name)

        return self.store.rest_delete_object(obj_type, mi.pk(obj_type), name)

    #
    # --------------------------------------------------------------------------------
    # rest_update_object
    #
    def rest_update_object(self, obj_type, name, obj_key_val, obj_key_data):
        return self.store.rest_update_object(obj_type, name, obj_key_val, obj_key_data)

    #
    # --------------------------------------------------------------------------------
    # rest_simple_resquest_to_dict
    #  Issue a store_simple_request and covert the result into a dict using json.load
    #  Performs no exception handling
    #
    def rest_simple_request_to_dict(self, url):
        return json.loads(self.store.rest_simple_request(url))

    #
    # --------------------------------------------------------------------------------
    # rest_post_request
    #  Forward a low-level REST request to the store
    #
    def rest_post_request(self, url, obj, verb='PUT'):
        return self.store.rest_post_request(url, obj, verb)
    
    #  
    # --------------------------------------------------------------------------------
    # create_object
    #  Helper function to help identify call sites which create objects
    #  Errors result in printing an error message, then returning True
    #
    def create_object(self, obj_type, field_dict):
        try:
            self.rest_create_object(obj_type, field_dict)
            return False
        except Exception, e:
            errors = self.rest_error_to_dict(e, obj_type)
           
        if errors != None:
            print self.rest_error_dict_to_message(errors)
            return True
        
    #  
    # --------------------------------------------------------------------------------
    # create_row
    #  Helper function to help identify call sites which create new rows
    #
    #  Return a pair of bool's, the first is an error indication, while the second
    #  is True when the row needed to be created.
    #
    #
    def create_row(self, obj_type, name, row_dict = None):
        # create the row with a dictionary which includes all the
        # populated values for the foreign keys
        # the row is created.
        if row_dict == None:
            updated_dict = {}
        else:
            updated_dict = row_dict

        #
        # Only set the primary key for obj_keys which aren't managed
        # by the CassandraSetting/COMPOUND_KEY_FIELDS (this is currently
        # a slightly different predicate than is_compound_key)
        #
        key = mi.pk(obj_type)
        type_info = mi.obj_type_info_dict[obj_type]['fields'][key]['type']
        if type_info != 'compound-key':
            updated_dict[mi.pk(obj_type)] = name
        updated_dict = self.associate_foreign_keys_with_dict(obj_type, updated_dict)

        if self.create_object(obj_type, updated_dict):
            return [True, False]
        return [False, True]

    #
    # --------------------------------------------------------------------------------
    # find_or_create_row
    #  Return a pair of bool's, the first is an error indication, while the second
    #  is True when the row needed to be created.
    #
    #  When a new row is created, if the obj_type has a private_key, an attempt
    #  is made to identify all the private keys, and populate values for each
    #  of them.
    #
    def find_or_create_row(self, obj_type, name, row_dict = None):
        if row_dict == None:
            row_dict = {}
        try:
            result = self.get_object_from_store(obj_type, name)
            if result[mi.pk(obj_type)] == name:
                return [False, False]
        except Exception:
            pass

        return self.create_row(obj_type, name, row_dict)

    #
    # --------------------------------------------------------------------------------
    # associate_foreign_keys_with_dict
    #  Passed an obj_type, and a dictionary, returns a dictionary with updated
    #  key:value pairs for all the known private keys.
    #  
    #
    def associate_foreign_keys_with_dict(self, obj_type, create_dict):
        #
        # Private key names are 'well known', so the names are used here to
        # idenify how the field ought to be populated.
        #
        for foreign_key_id in mi.obj_type_foreign_keys(obj_type):
            if foreign_key_id in create_dict:
                # already set in the dictionary
                pass
            elif mi.is_null_allowed(obj_type, foreign_key_id):
                continue
            elif foreign_key_id == 'vns':
                create_dict[foreign_key_id] = self.vns_name()
            elif foreign_key_id == 'vns-access-list' and \
              self.get_current_mode_obj_type() == 'vns-access-list':
                create_dict[foreign_key_id] = self.get_current_mode_obj()
            elif foreign_key_id == 'rule':
                # LOOK! this seems to need more work, what's the rule relationship?
                if obj_type == 'vns-interface':
                    create_dict = associate_foreign_key_for_vns_interface(dict)
            else:
                traceback.print_stack()
                print self.error_msg("Internal: %s can't determine value " \
                                     "for foreign key %s" % (obj_type, foreign_key_id))
 
        return create_dict

    #
    # methods to manage the mode_stack, prompts, and mode_obj/mode_obj_type
    #

    #
    # --------------------------------------------------------------------------------
    # set_contoller_for_prompt
    #
    def set_controller_for_prompt(self):
        if self.controller == "127.0.0.1:8000":
            self.controller_for_prompt = socket.gethostname()
        else:
            self.controller_for_prompt = self.controller
            self.update_prompt()

    #
    # --------------------------------------------------------------------------------
    # update_prompt
    #  There are several different prompts depending on the current mode:
    #  'host'>                                  -- login mode
    #  'host'#                                  -- enable mode
    #  'host'(config)#                          -- config mode
    #  'host'(config-vns-definition-name)#      -- vns-definition mode
    #  'host'(config-vns-definition-interface-rule-name)# -- vns-defn,
    #                                              interface-rule mode
    #
    def update_prompt(self):
        if self.current_mode().startswith("config"):
            current_mode = "(" + self.current_mode()
            if self.get_current_mode_obj() != None:
                if self.in_config_submode("config-vns"):
                    # substitute '|' with '-'
                    parts = self.get_current_mode_obj().split('|')
                    current_mode += "-" + '-'.join(parts)
            self.prompt = str(self.controller_for_prompt) + current_mode + ")# "
        elif self.current_mode() == "enable":
            self.prompt = str(self.controller_for_prompt) + "# "
        else:
            self.prompt = str(self.controller_for_prompt) + "> "

    #
    # --------------------------------------------------------------------------------
    # push_mode
    #  Every pushed mode is a quad: <modeName, tableName, specificRow, exitCallback>
    #
    #  obj_type is the name of an associated table/model (tableName)
    #  obj is the key's value for the table/model's key (specificRow)
    # 
    #  The cli currently supports a mode of table/model row edit, where the
    #  name of the table/mode is entered, along with an associated value for
    #  key-column of the table.   Once in that mode, other fields of the table
    #  can be edited by entering the name of the field, along with a new value.
    #
    #  The exitCallback is the nane of a method to call when the current pushed
    #  level is getting pop'd.
    #
    def push_mode(self, mode_name, obj_type=None, obj=None, exitCallback=None):
        self.mode_stack.append( { 'mode_name' : mode_name,
                                  'obj_type' : obj_type,
                                  'obj' : obj,
                                  'exit' : exitCallback} )
        self.update_prompt()

    #
    # --------------------------------------------------------------------------------
    # pop_mode
    #  Pop the top of the stack of mode's.
    #
    def pop_mode(self): 
        m = self.mode_stack.pop()
        if len(self.mode_stack) == 0:
            self.run = False
        else:
            self.update_prompt()
        return m
    
    #
    # --------------------------------------------------------------------------------
    # mode_stack_to_rest_dict
    #  Convert the stack of pushed modes into a collection of keys.
    #  Can be used to build the rest api dictionary used for row creates
    #
    def mode_stack_to_rest_dict(self, rest_dict):
        #
        for x in self.mode_stack:
            if x['mode_name'].startswith('config-'):
                rest_dict[x['obj_type']] = x['obj']

        return rest_dict

    #
    # --------------------------------------------------------------------------------
    # current_mode
    #  Return the string describing the current (top) mode.
    #
    def current_mode(self): 
        if len(self.mode_stack) < 1:
            return ""
        return self.mode_stack[-1]['mode_name']

    #
    # --------------------------------------------------------------------------------
    # in_config_submode
    #  Return true when the current mode is editing the contents of one of the
    #  rows of the table/model store.
    #
    def in_config_submode(self, prefix = "config-"):
        return self.current_mode().startswith(prefix)

    #
    # --------------------------------------------------------------------------------
    # in_config_mode
    #  Returns true for any config mode; this is any nested edit mode,
    #  along with the base config mode
    #
    def in_config_mode(self):
        return self.current_mode().startswith("config")
    #
    # --------------------------------------------------------------------------------
    # in_config_switch_mode
    #  Returns true when the switch mode has been entered via the 'switch <dpid>' command.
    #
    def in_config_switch_mode(self):
        return self.current_mode() == "config-switch"
    #
    # --------------------------------------------------------------------------------
    # in_config_switch_if_mode
    #  Returns true when the switch-interface mode has been entered via the
    # 'switch <dpid>' command, then the interface <name> command
    #
    def in_config_switch_if_mode(self):
        return self.current_mode() == "config-switch-if"

    #
    # --------------------------------------------------------------------------------
    # vns_name
    #
    def vns_name(self):
        mode_dict = self.mode_stack_to_rest_dict({})
        if 'vns' in mode_dict:
            return mode_dict['vns']
        if 'vns-definition' in mode_dict:
            return mode_dict['vns-definition']
        return None

    #
    # --------------------------------------------------------------------------------
    # in_config_vns_mode
    #  Returns true when the vns mode has been entered via the 'vns <vnsId>' command.
    #
    def in_config_vns_mode(self):
        return self.current_mode() == "config-vns"
    #
    # --------------------------------------------------------------------------------
    # in_config_vns_def_mode
    #  Returns true when the vns mode has been entered via the
    #  'vns-definition <vnsId>' command.
    #
    def in_config_vns_def_mode(self):
        return self.current_mode() == "config-vns-def"
    #
    # --------------------------------------------------------------------------------
    # in_config_vns_def_if_rule_mode
    #  Returns true when the vns mode has been entered via the
    #  'vns-definition <vnsId>' command, then into interface-rule mode.
    #
    def in_config_vns_def_if_rule_mode(self):
        return self.current_mode() == "config-vns-def-if-rule"

    #
    # --------------------------------------------------------------------------------
    # in_config_vns_acl_mode
    #  Returns true when the vns mode has been entered via the
    #  'vns <vnsId>' command, then into acl mode.
    #
    def in_config_vns_acl_mode(self):
        return self.current_mode() == "config-vns-acl"

    #
    # --------------------------------------------------------------------------------
    # in_config_vns_if_mode
    #  Returns true when the vns mode has been entered via the
    #  'vns <vnsId>' command, then into interface mode.
    #
    def in_config_vns_if_mode(self):
        return self.current_mode() == "config-vns-if"

    #
    # --------------------------------------------------------------------------------
    # in_config_host_mode
    #  Returns true when the vns mode has been entered via the
    #  'host <host-id>' command
    #
    def in_config_host_mode(self):
        return self.current_mode() == "config-host"

    #
    # --------------------------------------------------------------------------------
    # in_config_controller_node_mode
    #  Returns true when the vns mode has been entered via the
    #  'controller-node <controller-node-id>' command
    #
    def in_config_controller_node_mode(self):
        return self.current_mode() == "config-controller-node"

    #
    # --------------------------------------------------------------------------------
    # in_config_controller_interface_mode
    #  Returns true when the controller interface mode has been entered via the
    #  'interface Ethernet <#>' command from the controller-node config mode
    #
    def in_config_controller_interface_mode(self):
        return self.current_mode() == "config-controller-if"

    #
    # --------------------------------------------------------------------------------
    # in_config_port_channel_mode
    #
    def in_config_port_channel_mode(self):
        return self.current_mode() == "config-port-channel"

    # --------------------------------------------------------------------------------
    # set_current_mode_obj
    #  Sets the name of the selected row (key's value)
    #
    def set_current_mode_obj(self, obj):
        self.mode_stack[-1]['obj'] = obj
        
    #
    # --------------------------------------------------------------------------------
    # get_current_mode_obj
    #  Gets the name of the current mode's selected row value (key's value)
    #  This can return None.
    #
    def get_current_mode_obj(self):
        return self.mode_stack[-1]['obj']

    #
    # --------------------------------------------------------------------------------
    # set_current_mode_obj_type
    #  Set the table/model name for this current mode.
    #
    def set_current_mode_obj_type(self, obj_type):
        self.mode_stack[-1]['obj_type'] = obj_type

    #
    # --------------------------------------------------------------------------------
    # get_current_mode_obj_type
    #  Get the table/model name for this current mode.
    #
    def get_current_mode_obj_type(self):
        return self.mode_stack[-1]['obj_type']

    #
    # --------------------------------------------------------------------------------
    # get_nested_mode_obj
    #  Get the id of the object that matches the given object type
    #  starting from the current mode and working backwards to the
    #  top-level mode. If there's no mode that matches the given
    #  object type, then return None (maybe should handle as exception?).
    #
    def get_nested_mode_obj(self, obj_type):
        for i in range(1, len(self.mode_stack)+1):
            # Use negative index so we search from the top/end of the stack
            mode = self.mode_stack[-i]
            if mode['obj_type'] == obj_type:
                return mode['obj']
        return None
    
    #
    # helper functions to access commands, obj_types, and fields
    #

    #
    # --------------------------------------------------------------------------------
    # all_obj_types_starting_with
    #  Returns a list of all the current object types starting with the text
    #  parameter.
    #
    #
    def all_obj_types_starting_with(self, text=""):
        if onos == 0:
            netvirt_feature = self.netvirt_feature_enabled()
        else:
            netvirt_feature = False

        matched_obj_types = [x for x in mi.obj_types
                             if x.startswith(text) and
                                self.debug_obj_type(x) and
                                (netvirt_feature or (x != 'vns-definition')) ]
        # synthetic object type based on the vns config submode.
        if self.in_config_vns_def_mode() and "interface-rule".startswith(text):
            matched_obj_types.append("interface-rule")
        if self.in_config_vns_mode() and "access-list".startswith(text):
            matched_obj_types.append("access-list")
        if self.in_config_vns_mode() and "interfaces".startswith(text):
            matched_obj_types.append("interfaces")
        return matched_obj_types
    #
    # --------------------------------------------------------------------------------
    # feature_enabled
    #  Return True when a particular feature is enabled.
    #
    def feature_enabled(self, feature):
        # features not managed via store
        #
        if feature == 'experimental':
            return os.path.exists(self.feature_allow_experimental)

        if feature == 'ha':
            try:
                with open(self.boot_config_filename, "r") as f:
                    for line in f:
                        if line.startswith('ha-config='):
                            parts = line.split('=')
                            if len(parts) > 0 and parts[1] == 'enabled\n':
                                return True
            except:
                pass
            return False

        # only current features which can be enabled disabled
        if feature not in [
                'vns', 'static-flow-pusher', 'performance-monitor']:
            return False

        #
        try:
            entry = self.get_table_from_store('feature')
        except Exception, e:
            errors = self.rest_error_to_dict(e, 'feature')
            print self.rest_error_dict_to_message(errors)
            return True

        feature_to_field_dict = {
            'vns'                 : 'netvirt-feature',
            'static-flow-pusher'  : 'static-flow-pusher-feature',
            'performance-monitor' : 'performance-monitor-feature',
        }

        if len(entry) == 0:
            if feature in feature_to_field_dict:
                field = feature_to_field_dict[feature]
                return mi.field_default_value('feature', field)
            return True

        if len(entry) != 1:
            # need a mechanism to select one from the list
            return True

        if feature in feature_to_field_dict:
            return entry[0][feature_to_field_dict[feature]]

        # use the default value from the model (every feature needs a default value)
        defauilt_value = mi.field_default_value('feature', feature_to_field_dict[feature])
        if default_value == None:
            print self.error_msg('Feature %s missing default value' % feature)
            return True
        return default_value

    '''
    #
    # --------------------------------------------------------------------------
    # address_space_default_create
    #
    #  Create a default address space if it doesn't exist, which enables
    #  'address-space default',
    #
    def address_space_default_create(self):
        key = mi.pk('address-space')
        try:
            entries = self.get_table_from_store('address-space',
                                                key, 'default', 'exact')
            errors = None
        except Exception, e:
            errors = self.rest_error_to_dict(e, 'address-space')
            if not 'see_other' in errors:
                print self.rest_error_dict_to_message(errors)
            return

        if len(entries) == 0:
            self.create_row('address-space', 'default')

    #
    # --------------------------------------------------------------------------
    # tenant_default_create
    #
    #  Create default tenant and system tenant if it doesn't exist, which enables
    #  'tenant default' and tenant system, router vrsystem,
    #
    def tenant_default_create(self):
        key = mi.pk('tenant')
        try:
            entries = self.get_table_from_store('tenant',
                                                key, 'default', 'exact')
            errors = None
        except Exception, e:
            errors = self.rest_error_to_dict(e, 'tenant')
            if not 'see_other' in errors:
                print self.rest_error_dict_to_message(errors)
            return

        if len(entries) == 0:
            self.create_row('tenant', 'default')
        #external tenant
        try:
            entries = self.get_table_from_store('tenant',
                                                key, 'external', 'exact')
            errors = None
        except Exception, e:
            errors = self.rest_error_to_dict(e, 'tenant')
            if not 'see_other' in errors:
                print self.rest_error_dict_to_message(errors)
            return

        if len(entries) == 0:
            self.create_row('tenant', 'external')

        #system tenant and system router: vrsystem
        try:
            entries = self.get_table_from_store('tenant',
                                                key, 'system', 'exact')
            errors = None
        except Exception, e:
            errors = self.rest_error_to_dict(e, 'tenant')
            if not 'see_other' in errors:
                print self.rest_error_dict_to_message(errors)
            return

        if len(entries) == 0:
            self.create_row('tenant', 'system')
        
        try:
            entries = self.get_table_from_store('virtualrouter',
                                                key, 'vrsystem', 'exact')
            errors = None
        except Exception, e:
            errors = self.rest_error_to_dict(e, 'virtualrouter')
            if not 'see_other' in errors:
                print self.rest_error_dict_to_message(errors)
            return

        if len(entries) == 0:
            self.create_row('virtualrouter', 'vrsystem',{"tenant":"system","vrname":"vrsystem"})
 
    #
    # --------------------------------------------------------------------------
    # netvirt_feature_init
    #  perform vns featrue enablement:
    #  Create a default vns if it doesn't exist, which enables 'vns default',
    #
    def netvirt_feature_init(self):
        self.address_space_default_create()
        self.tenant_default_create()
        key = mi.pk('vns-definition')
        try:
            entries = self.get_table_from_store('vns-definition', key, 'default|default', 'exact')
            errors = None
        except Exception, e:
            errors = self.rest_error_to_dict(e, 'vns-definition')
            if not 'see_other' in errors:
                print self.rest_error_dict_to_message(errors)
            return

        if len(entries) == 0:
            self.create_row('vns-definition', 'default', {"tenant":"default","vnsname":"default"})

    #
    # --------------------------------------------------------------------------------
    # netvirt_feature_enabled
    #  Return True when the vns feature is enabled
    #  
    #  When vns is disabled, particular vns commands are no longer
    #  available, along with a few specific objects.  Particular
    #  code sites which are specifically concerned with these issues
    #  disable these items.  Another possible approach would be to
    #  modify the base information sources  when a feature is
    #  enabled or disabled, ie: mi.obj_types and command_dict.
    #  This more active approach isn't taken since during
    #  the initialization process, a variety of different object
    #  relationships are constructed; foreign key xref's,  alias
    #  key xref's,  object key dictionaries, etc.  If the feature
    #  enabled were more active, then all these relationships would also
    #  need to be recomputed.
    #
    def netvirt_feature_enabled(self):
        controller_netvirt_feature = self.feature_enabled("vns")
        if self.netvirt_feature_enabled_cached != controller_netvirt_feature:
            self.netvirt_feature_enabled_cached = controller_netvirt_feature
            if self.netvirt_feature_enabled_cached:
                self.netvirt_feature_init()
        if controller_netvirt_feature == False:
            return False
        return True
    '''

    #
    # --------------------------------------------------------------------------------
    # get_obj_of_type
    #  Return a single row, or None, of the table who's name is passed as obj_type
    #  Returns None when when there's multiple matches of the nane in the table/model
    #  Performs no error processing
    #
    def get_obj_of_type(self, obj_type, name):
        key = mi.pk(obj_type)
        if key:
            errors = None
            try:
                entries = self.get_table_from_store(obj_type)
            except Exception, e:
                errors = self.rest_error_to_dict(e, obj_type)

            if errors:
                print self.rest_error_dict_to_message(errors)
                return None

            entries = [x for x in entries if x.get(key, 'None') == name]
            if len(entries) != 1:
                return None
            return entries[0]
        else:
            return None
        
    #
    # --------------------------------------------------------------------------------
    # fields_for_current_submode_starting_with
    #  Return a list of choices which are fields names of model/store objects
    #  Any fields which are primary keys are excluded from edit, as are any
    #  foreign keys, this is managed through is_editable()
    #
    def fields_for_current_submode_starting_with(self, start_text=""):
        if not self.in_config_submode():
            return []
        if self.in_config_vns_mode():
            return []
        obj_type = self.get_current_mode_obj_type()
        if not obj_type in mi.obj_type_info_dict:
            return []
        fields = [x for x in mi.obj_type_info_dict[obj_type]['fields'].keys() \
                  if mi.is_editable(obj_type, x)]

        return [x for x in fields if x.startswith(start_text)]

    #
    # --------------------------------------------------------------------------------
    # obj_types_for_config_mode_starting_with
    #
    # what obj types are available in the given submode
    # e.g., top-level config mode has switch and config-switch has
    # flow-entry but flow-entry should not be available in top-level
    # config nor should switch be available in config-switch submode!
    # LOOK! hardwired for now to test parsing - need to merge with model
    #
    def obj_types_for_config_mode_starting_with(self, start_text=""):
        if not self.in_config_mode():
            return []
        #
        # vns features
        if onos == 0:
            netvirt_features = self.netvirt_feature_enabled()
        else:
            netvirt_features = False
        vns_objects = [ 'vns-definition' ]

        ret_list = [x for x in mi.obj_types
                    if self.debug_obj_type(x) and 
                       not mi.is_obj_type_source_not_user_config(x)]
        return [x for x in ret_list if x.startswith(start_text) and
                    (netvirt_features or not x in vns_objects) ]

    @staticmethod
    def title_of(command):
        return command['title'] if type(command) is dict else command

    #
    # --------------------------------------------------------------------------------
    # commands_feature_enabled
    #
    def commands_feature_enabled(self, commands):
        return [self.title_of(x) for x in commands
                if (not self.title_of(x) in self.command_name_feature) or
                    command.isCommandFeatureActive(self.title_of(x),
                           self.command_name_feature[self.title_of(x)])]

    #
    # --------------------------------------------------------------------------------
    # commands_for_mode
    #
    def commands_for_mode(self, mode):
        """
        Walk the command dict, using interior submodes and compiling
        the list of available commands (could rebuild command_dict()
        to contain all the possible commands, but its good to know
        exactly which commands apply to this submode)
        """

        # make a new list, so that items don't get added to the source
        ret_list = list(self.command_nested_dict.get('login', []))
        if mode == 'login':
            ret_list += self.command_dict.get('login', [])
            return ret_list
        ret_list += self.command_nested_dict.get('enable', [])
        if mode == 'enable':
            ret_list += self.command_dict.get('enable', [])
            return ret_list

        if mode == 'config':
            ret_list += self.command_nested_dict.get('config', [])
            ret_list += self.command_dict.get('config', [])
            return ret_list
        
        for idx in [x for x in self.command_nested_dict.keys() if mode.startswith(x)]:
            ret_list += self.command_nested_dict.get(idx, [])

        ret_list += self.command_dict.get(mode, [])

        # manage command who's names are regular expressions
        result = [x['re'] if type(x) == dict else x  for x in ret_list]

        return result

    #
    # --------------------------------------------------------------------------------
    # commands_for_current_mode_starting_with
    #
    def commands_for_current_mode_starting_with(self,
                                                start_text = "", completion = None):
        """
        One of the difficult issues here is when the first item
        isn't a token, but rather a regular expression.  This currently occur
        in a few places in the command description, and the mechanism for
        dealing with the issue here is ... uhm ... poor.  The code here is
        a stopgap, and assumes the only regular expression supported
        is the <digits> one.  This could be make a bit better based on
        the submode, but really, this entire first-token management should
        be improved.
        """
        if completion == None:
            completion = False
        #
        # vns features commands include:
        netvirt_feature_commands = ['vns', 'vns-definition']
        if onos == 0:
            netvirt_feature = self.netvirt_feature_enabled()
        else:
            netvirt_feature = False

        mode_list = self.commands_for_mode(self.current_mode())
        ret_list = self.commands_feature_enabled(utif.unique_list_from_list(mode_list))

        def prefix(x, start_text, completion):
            if type(x) == str and x.lower().startswith(start_text.lower()):
                return True
            if not completion and type(x) == re._pattern_type:
                return x.match(start_text)
            return False

        def pattern_items(ret_list, prefix):
            matches = []
            for p in [x for x in ret_list if type(x) == re._pattern_type]:
                for c in command.command_registry:
                    if c['mode'] != self.current_mode():
                        continue
                    if type(c['name']) != dict:
                        continue
                    first_word = c['name']
                    if 'completion' not in first_word:
                        continue
                    completion = first_word['completion']
                    if first_word['pattern'] == p.pattern:
                        result = {}
                        scopes = [ first_word,
                                   {
                                    'completions' : result,
                                    'data'        : {},
                                    'text'        : prefix,
                                   },
                                 ]
                        command._call_proc(completion,
                                           command.completion_registry,
                                           scopes, c)
                        matches = result.keys()
            return matches

        matches = [x for x in ret_list if prefix(x, start_text, completion)]
        if completion:
               matches += pattern_items(ret_list, start_text)
        return matches


    #
    # --------------------------------------------------------------------------------
    # complete_optional_parameters
    #
    # Parse optional parameters.  These can occur in any order.
    # Params argument is a hash mapping a parameter name to type
    # information.
    # words argument are remaining words in the command
    # line that aren't yet parsed
    #
    def complete_optional_parameters(self, params, words, text):
        i = 0
        while i < len(words):
            final = i+1 >= len(words)
            word = words[i]
            possible = [x for x in params if x.startswith(word)]
            param_name = possible[0]
            param = params[param_name]
             
            if (param['type'] != 'flag'):
                if (final):
                    argument = text
                    if (param['type'] == 'enum'):
                        return [x 
                                for x in param['values'] 
                                if x.startswith(argument)]
                    elif argument == '':
                        if ('syntax_help' in param):
                            self.print_completion_help(param['syntax_help'])
                        else:
                            self.print_completion_help('[%s argument]' % word)
                        return
                i += 1
            i += 1

        return [x for x in params if x.startswith(text)]

    #
    # --------------------------------------------------------------------------------
    # parse_optional_parameters
    #
    @staticmethod
    def parse_optional_parameters(params, words):
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
    # annotated_obj_type_id
    #
    #  Given an id's value for a compound key, crack the value into
    #  parts, and annotate each part with the compound key name.
    #
    #  For 'Not Found' errors, the obj_type is known, along with the value
    #  for the id which wasn't found.  If this is an id with a compound key,
    #  leaking information about the key's implementiion should be avoided. 
    #  Here the key's value is cracked based on the composition character,
    #  and the items are displayed with their key-value name.
    #  eg: Error: Not Found vns:xXx rule-id:snapple  (vns-interface-rule)
    #
    def annotated_obj_type_id(self, obj_type, error):
        key = mi.pk(obj_type)
        if mi.is_compound_key(obj_type, key):
            result = { key : error}
            mi.split_compound_into_dict(obj_type, key, result)
            return ', '.join(["%s:%s" % tuple(x) for x in result.items()
                             if x[0] != key])
        return error

    #
    # --------------------------------------------------------------------------------
    #
    def find_master(self):
        """
        Return a dictionary with two items: 'master', and 'ips'
        'master' identifes the ip address of the master, a string
        which is an ip address, or None.
        'ips' is a list of interface rows with populated ip addresses.
        """
        # collect all the controller-interfaces
        ips = [x for x in local_interfaces_firewall_open('tcp', [80, 8000], 'all')
               if (x['ip'] != '' or x['discovered-ip'] != '')]

        master = None
        for ip in ips:
            if ip['ip'] != '':
                url = "http://%s/rest/v1/system/ha/role" % ip['ip']
            if ip['discovered-ip'] != '': # select discovered ip over ip
                url = "http://%s/rest/v1/system/ha/role" % ip['discovered-ip']
            result = self.store.rest_simple_request(url, use_cache = False)
            self.check_rest_result(result)
            ha_role = json.loads(result)
            if ha_role['role'] == 'MASTER':
                if ip['ip'] != '':
                    master = ip['ip']
                if ip['discovered-ip'] != '':
                    master = ip['discovered-ip']

        return {'master': master, 'ips': ips}

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
                # traceback.print_stack()
                errors = {'connection_error' : 'REST API server %s failure: %s' %
                                                (self.controller, error_returned)}
            elif code == 400:
                try:
                    errors = json.loads(error_returned)
                except:
                    # if the error can't be converted into a dictionary, then imbed the complete
                    # errors as the value for a specific error key.
                    errors = {'error_result_error': "Can't convert returned error: %s" % error_returned}
            elif code == 301:
                errors = {'moved_error': 'HttpError Moved Permanently %s' % e}
            elif code == 303:
                # Attempt to determine what list of ip addresses is for
                # the controller, try to find the Master.  if any failure 
                # occurs anywhere, drop down to a simple error message.
                try:
                    minfo = self.find_master()
                    ips = [x['ip'] for x in minfo['ips']]
                    master = minfo['master']
                
                    if master:
                        errors = {'see_other': 'This controller is in SLAVE '
                                  'mode. The command must be run in MASTER mode.'
                                  '\nMASTER at %s' % master}
                    elif len(ips):
                        errors = {'see_other': 'This controller is in SLAVE mode. '
                                    'The command must be run in MASTER mode.'
                                    '\nNo MASTER found, possible ip\'s %s' %
                                    (', '.join(ips))}
                    else:
                        errors = {'see_other': 'This controller is in SLAVE mode. '
                                    'The command must be run in MASTER mode.'
                                    '\nNo MASTER currently exists '}
                except:
                    errors = {'see_other': 'This controller is in SLAVE mode. '
                              'The command must be run in MASTER mode.'
                              '\nNo ip addresses for controller available'}
            else:
                errors = {'unknown_error': 'HttpError %s' % error_returned}
        elif isinstance(e, NotFound):
            if e.obj_type in mi.obj_type_info_dict:
                errors = {'not_found_error' : 'Not Found: %s (%s)' % 
                                              (self.annotated_obj_type_id(e.obj_type,
                                                                          e.name),
                                               e.obj_type)}
            else:
                errors = {'not_found_error' : 'Not Found: %s (%s)' % 
                                              (e.name, e.obj_type)}
        else:
            if self.debug or self.debug_backtrace:
                traceback.print_stack()
            errors = {'unknown_error': "Need error management for error %s" % type(e)}

        return errors

    #
    #
    # --------------------------------------------------------------------------------
    # rest_error_is_not_found
    #  Return True if this specific error was the result of a not-found situation
    #
    @staticmethod
    def rest_error_is_not_found(rest_error_dict):
        if 'not_found_error' in rest_error_dict:
            return True
        return False

    #
    # --------------------------------------------------------------------------------
    # rest_error_dict_to_message
    #   Turn an rest_error_dict returned from rest_error_to_dict
    #   into an error message which can ge printed.  Code assumes multiple errors
    #   won't occur; if a 'field_error' exists, for example, a 'model_error' won't
    #   also be posted in the error
    #
    def rest_error_dict_to_message(self, rest_error_dict):
        if rest_error_dict == None:
            return rest_error_dict
        error_msg = ""
        if 'field_errors' in rest_error_dict:
            for (k, v) in rest_error_dict['field_errors'].items():
                error_msg += "Syntax error: field %s: %s" % (k, v)
            # traceback.print_stack(), to find out why the error occured
        elif 'model_error' in rest_error_dict:
            error_msg += "%s" % rest_error_dict['model_error']
        elif 'not_found_error' in rest_error_dict:
            error_msg += "Error: %s" % rest_error_dict['not_found_error']
        elif 'connection_error' in rest_error_dict:
            error_msg += rest_error_dict['connection_error']
        elif 'error_result_error' in rest_error_dict:
            error_msg += rest_error_dict['error_result_error']
        elif 'moved_error' in rest_error_dict:
            error_msg += rest_error_dict['moved_error']
        elif 'see_other' in rest_error_dict:
            error_msg += rest_error_dict['see_other']
        elif 'unknown_error' in rest_error_dict:
            error_msg += rest_error_dict['unknown_error']
        elif 'description' in rest_error_dict and  \
          rest_error_dict["description"] == 'saved':
            # not an error: { 'description' : 'saved' }
            return None
        elif 'error_type' in rest_error_dict: # http 400 errors
            description = rest_error_dict['description']
            if description.startswith('Error: '):
                description = description[len('Error: '):]
            return '%s (%s)' % (description,
                                rest_error_dict['error_type'])
        else:
            error_msg = "REST API server on controller-node %s " % self.controller
            error_msg += "had %s error:\n" % rest_error_dict['error_type']
            error_msg += rest_error_dict['description']
        return error_msg

    #
    # --------------------------------------------------------------------------------
    #
    # Commands of the CLI. A command "foo" should have two methods:
    #   cp_foo(self, text, state) - completion for this command, return list of options
    #   do_foo(self, line) - execute the command
    #
    # Given a string 'word', not containing any spaces, nor any break
    #  characters associated with python, return the a invocable method
    #  note that '-' are permitted, and converted to '_''s
    #
    # When in the vns or vns-definition mode, the selected functions
    #  to call back have an additional component added to the called
    #  back function.  In the non-vns case, for example, the "show"
    #  command would result in a method called do_show, while in the
    #  vns modes, 'vns_' in inserted between the 'verb' and the noun,
    #  for example, resulting in 'do_vns_show'

    #
    # --------------------------------------------------------------------------------
    # method_from_name
    #
    def method_from_name(self, prefix, name):
        if type(name) != str:
            return None
        if self.in_config_submode("config-vns") and \
           getattr(self, prefix + "vns_" + name.replace("-","_"), None):
            return getattr(self, prefix + "vns_" + name.replace("-","_"))
        return getattr(self, prefix+name.replace("-","_"), None)
        
    #
    # --------------------------------------------------------------------------------
    # command_method_from_name
    #
    def command_method_from_name(self, name):
        return self.method_from_name("do_", name)               # do_XXX methods

    #
    # --------------------------------------------------------------------------------
    # command_show_method_from_name
    #
    def command_show_method_from_name(self, name):
        return self.method_from_name("do_show_", name)          # do_show_XXX methods

    #
    # --------------------------------------------------------------------------------
    # cloud_method_from_name
    #
    def cloud_method_from_name(self, name):
        return self.method_from_name("cloud_", name)            # cloud_XXX methods

    #
    # --------------------------------------------------------------------------------
    # completion_method_from_name
    #
    def completion_method_from_name(self, name):
        return self.method_from_name("cp_", name)               # cp_XXX (completion) methods

    #
    # --------------------------------------------------------------------------------
    # completion_show_method_from_name
    #
    def completion_show_method_from_name(self, name):
        return self.method_from_name("cp_show_", name)          # cp_show_XXX (completion) methods

    #
    # --------------------------------------------------------------------------------
    # unique_key_from_non_unique
    #
    # Primary keys for cassandra for some keys are contenations of
    #  several non-unique keys separated by a character not used for
    #  any other purpose (in this case '|').  The concatenation
    #  of non-unique keys is intended to create a unique key.
    #
    def unique_key_from_non_unique(self, words):
        return "|".join(words)
    
    #
    # --------------------------------------------------------------------------------
    # prefix_search_key
    #  Prefix's of primary keys for keys's built through unique_key_from_non_unique()
    #

    def prefix_search_key(self, words):
        return self.unique_key_from_non_unique(words) + "|"

    #
    # --------------------------------------------------------------------------------
    # implement_connect
    #
    def implement_connect(self, data):
        new_ip = data.get('ip-address')
        if new_ip == None:
            new_ip = data.get('controller-id')
        if not self.IP_ADDR_RE.match(new_ip):
            controller_id = alias_lookup('controller-alias', new_ip)

            if controller_id == None:
                controller_id = new_ip

            try:
                ifs = self.rest_query_objects('controller-interface',
                                              { 'controller' : controller_id } )
                if len(ifs) == 1:
                    new_ip = ifs[0]['ip']
            except:
                return self.error_msg('Can\'t find controller named %s' % new_ip)
                pass

        if 'port' in data:
            new_ip += str(data['port'])
        else:
            new_ip += ":80"
        #
        # request the version, if it fails, then don't allow the switch
        try:
            version_url = 'http://%s/rest/v1/system/version' % new_ip
            data = self.rest_simple_request_to_dict(version_url)
        except Exception, e:
            return self.error_msg('Could not connect to %s' % new_ip)

        self.controller = new_ip
        self.store.set_controller(new_ip)
        self.set_controller_for_prompt()
        while (self.current_mode() != "login"):
            self.pop_mode()
        print "Switching to controller %s" % self.controller

        return self.pp.format_entry(data[0], 'version')

    def cp_exit(self, words, text, completion_char):
        self.print_completion_help("<cr>")

    def do_exit(self, words=None):
        if self.mode_stack[-1]['exit']:
            method = self.mode_stack[-1]['exit']
            method()
        self.pop_mode()

    def cp_logout(self, words, text, completion_char):
        self.print_completion_help("<cr>")

    def do_logout(self, words=None):
        self.run = False

    def cp_end(self, words, text, completion_char):
        self.print_completion_help("<cr>")

    def do_end(self, words=None):
        while self.current_mode().startswith("config"):
            self.pop_mode()


    #
    # --------------------------------------------------------------------------------
    # do_show_tunnel
    #  Command descriptions has no practical support for show commands yet.
    #
    def do_show_tunnel(self, words):
        if len(words) < 2:
            print self.syntax_msg("Usage: show tunnel [ <switch> | all ] active")
            return
        if words[1] != "active":
            return self.error_msg("Unknown request %s" % words[1])

        # active --

        if words[0] != 'all':
            words[0] = convert_alias_to_object_key("switch", words[0])

        try:
            data = self.rest_simple_request_to_dict(
                            'http://%s/rest/v1/tunnel-manager/%s/' %
                             (self.controller, words[0]))
        except:
            return self.error_msg('Could not load tunnel-manager details')

        if 'error' in data and data['error'] != None:
            return self.error_msg('tunnel-manager response: %s' % data['error'])

        entries = []
        for entry in data['tunnMap'].keys():
            item = data['tunnMap'][entry]
            for remote in item['tunnelPorts']:
                remoteIp = "%s.%s.%s.%s" % ( int(remote[3:6]),
                                             int(remote[6:9]),
                                             int(remote[9:12]),
                                             int(remote[12:]))
                entries.append({ 'dpid': entry,
                                 'localTunnelIPAddr' : item['localTunnelIPAddr'],
                                 'tunnelPorts' : remoteIp
                               })

        return self.display_obj_type_rows('tunnel-details', entries)
    

    #
    # --------------------------------------------------------------------------------
    # cp_show_vns_access_list_entry
    #
    def cp_show_vns_access_list_entry(self, words, text, completion_char):
        return self.cp_vns_access_list_entry(words, text)

    #
    # --------------------------------------------------------------------------------
    # do_show_vns_access_list_entry
    #
    def do_show_vns_access_list_entry(self, words):
        with_search_key = "<no_key>"
        if self.vns_name() is None:
            if len(words) > 0:
                search_object = ["vns-access-list-entry", words[0]]
            else:
                search_object = ["vns-access-list-entry"] 
        elif len(words) > 0:
            with_search_key = '-'.join(words)
            words.insert(0, self.get_current_mode_obj())
            search_object = ["vns-access-list-entry", 
                             self.unique_key_from_non_unique(words)]
        else:
            search_object = ["vns-access-list-entry",
                             self.prefix_search_key([self.get_current_mode_obj()])]
        return self.do_show_object(search_object, with_search_key)

    #
    # --------------------------------------------------------------------------------
    # do_vns_no
    #  implement the no command while in the context of vns/vns-definition
    #
    def do_vns_no(self, words):
        if len(words) < 1:
            return "Syntax: no <item> <key> : delete from the named item the specific key"
        else:
            vns_name_to_table_name = { "interfaces"        : "vns-interface",
                                       "interface-rule"    : "vns-interface-rule",
                                       "access-list"       : "vns-access-list",
                                       "access-list-entry" : "vns-access-list-entry",
                                       "access-group"      : "vns-interface-access-list",
                                     }
            if words[0] in vns_name_to_table_name:
                if len(words) < 2:
                    return "Syntax: no %s <item> <key>" % words[0]
                #
                # the item to remove in for access-list-entry is an integer,
                # canonicalize the name before its used to search
                if words[0] == 'access-list-entry':
                    if self.ACL_RE.match(words[1]):
                        words[1] = str(int(words[1]))
                    else:
                        return self.error_msg('%s key %s must be an integer' % 
                                             (words[0], words[1]))

                name = self.unique_key_from_non_unique([self.get_current_mode_obj(), words[1]])
                table = vns_name_to_table_name[words[0]]
                errors = None

                if words[0] == "entry" and not self.in_config_vns_acl_mode():
                    return self.do_no(words) # let do_no() print error messages
                elif words[0] == "access-group" and not self.in_config_vns_if_mode():
                    return self.do_no(words) # let do_no() print error messages

                # access-group/vns-interface-access-list has 'id's with an additional field,
                # the in-out parameter, which is required to be able to delete the entry
                if words[0] == "access-group":
                    if len(words) < 3:
                        return self.syntax_msg('no access-group <acl-name> <in|out>')
                    choices = [x for x in ["in", "out"] if x.startswith(words[2])]
                    if len(choices) != 1:
                        return self.syntax_msg('no access-group <acl-name> <in|out>')
                    name = self.unique_key_from_non_unique([self.get_current_mode_obj(),
                                                            self.vns_name(),
                                                            words[1]])
                    name = name + "|" + choices[0]

                try:
                    self.rest_delete_object(table, name)
                except Exception, e:
                    errors = self.rest_error_to_dict(e, words[0] + " " + words[1])
                    
                if errors:
                    return self.rest_error_dict_to_message(errors)
                #
                # cascade delete?
                if not errors:
                    self.cascade_delete(table, name)

                return None

            return self.do_no(words)

    #
    # --------------------------------------------------------------------------------
    # do_no_tag
    #  Since the name and value must be present, this is currently a special
    #  case.  additionally, the two tables 'tag-mapping' and 'tag' are 
    #  intermingled.  the 'tag' table manages the <namespace><name><value>
    #  tuple, while the tag-mapping add <host> to that relationship  When
    #  the removal of the entry from the tag-mapping table leaves no further
    #  <namespace><name><value>'s for any hosts, then the row from the
    #  'tag' table must be removed
    #
    def implement_no_tag(self, words):
        if not words or len(words) != 2 or words[1].find('=') == -1:
            return  "Syntax: no tag <namespace.name>=<value>"
        else:
            name_and_value = words[1].split("=")
            if len(name_and_value) > 2:
                return "Syntax: no tag <namespace.name>=<value>"

            name_part = name_and_value[0].split('.')
            if len(name_part) == 1:
                namespace = 'default'
                name = name_part[0]
            elif len(name_part) >= 2:
                namespace = '.'.join(name_part[:-1])
                name = name_part[-1]

            value = name_and_value[1]

            item = self.unique_key_from_non_unique([namespace,
                                                    name,
                                                    value,
                                                    self.get_current_mode_obj()])
            #
            # To prevent leaking the unique key value in the 'Not Found'
            # error message, look up the item first, and display a unique
            # error message for this case
            #
            errors = None
            try:
                self.get_object_from_store('tag-mapping', item)
            except Exception, e:
                errors = self.rest_error_to_dict(e, "tag " + item)

            if errors:
                if self.rest_error_is_not_found(errors):
                    return self.error_msg("tag %s.%s=%s not found" %
                                          (namespace, name, value))
                else:
                    return self.rest_error_dict_to_message(errors)
                
            try:
                self.rest_delete_object('tag-mapping', item)
            except Exception, e:
                errors = self.rest_error_to_dict(e, "tag " + item)

            if errors:
                return self.rest_error_dict_to_message(errors)
            #
            # if there are no more items in tag-mapping for the
            # <namespace>|<name>|<value>, then an entry needs to
            # removed from the 'tag' table (for cleanup)
            #
            key = self.unique_key_from_non_unique([namespace,
                                                   name,
                                                   value])
            try:
                tags = self.get_table_from_store('tag-mapping', 'tag', key)
            except Exception, e:
                errors = self.rest_error_to_dict(e, "tag-mapping tag " + key)

            if errors:
                print self.rest_error_dict_to_message(errors)
                return
            if len(tags) == 0:
                try:
                    self.rest_delete_object('tag', key)
                except Exception, e:
                    errors = self.rest_error_to_dict(e, "tag " + item)
                    
                if errors:
                    print self.rest_error_dict_to_message(errors)
                    return
            #
            # cascade delete?

    #
    # --------------------------------------------------------------------------------
    # vns_interface_rule_tags_validate
    #  Provides a warning when the associated tag doesn't exist
    #
    def vns_interface_rule_tags_validate(self, obj_type, field, value):
        for tag_pair in value.split(','):
            if tag_pair.find('=') == -1 or len(tag_pair.split('=')) != 2:
                # field validation won't match the regular expression
                return 
            (tag_name, tag_value) = tag_pair.split('=')
            tag_name_parts = tag_name.split('.')
            if len(tag_name_parts) == 1:
                tag_namespace = 'default'
                tag_name = tag_name_parts[0]
            elif len(tag_name_parts) >= 2:
                tag_namespace = '.'.join(tag_name_parts[:-1])
                tag_name = tag_name_parts[-1]
            else:
                # XXX not reached
                pass

            # Validating using the 'tag' model to determine whether the
            # tag exists depends on having the tag value removed when there's
            # no more references to the entry.  the cli manages the 
            # 'tag'/'tag-mapping' models that way, but other uses of the 
            # rest api may not.
            #
            key = self.unique_key_from_non_unique([tag_namespace, tag_name, tag_value])
            try:
                table = self.get_object_from_store('tag', key)
            except Exception, e:
                errors = self.rest_error_to_dict(e, 'tag')
                # special case for 'not found' error message
                if self.rest_error_is_not_found(errors):
                    self.warning("tag `%s.%s %s' not yet defined" % \
                                 (tag_namespace, tag_name, tag_value))
                else:
                    print self.rest_error_dict_to_message(errors)
        return None

    #
    # --------------------------------------------------------------------------------
    # validate_switch_core_switch
    #  Intended to display a warning message when the value is set to True
    #
    def validate_switch_core_switch(self, obj_type, field, value):
        if value == 'True': # XXX odd that the value needs to be quoted.
            self.warning("enabling core-switch on a switch with "
                         "directly connected hosts will cause the same to "
                         "be unable to connect")

    #
    # --------------------------------------------------------------------------------
    # handle_specific_tag_fields
    #  Called by handle_specific_obj_type to convert fields values from the
    #  rest api into a displayable table
    #
    @staticmethod
    def handle_specific_tag_fields(entries):
        for entry in entries:
            fields = entry['id'].split('|')
            # XXX error if there's not four parts
            entry['namespace'] = fields[0]
            entry['name'] = fields[1]
            entry['value'] = fields[2]

    #
    # --------------------------------------------------------------------------------
    # append_when_missing
    #
    @staticmethod
    def append_when_missing(unique_list, item):
        if not item in unique_list:
            unique_list.append(item)

    #
    # --------------------------------------------------------------------------------
    # show_command_prefix_matches
    #  show command's prefix matches, return True when the command matches
    #  fpr the prefix described, used by get_valid_show_options() during
    #  cp_show() to collect available command choice.
    #
    def show_command_prefix_matches(self, command, prefix):
        # convert any '-'s in the prefix tp "_"'s since func's have _ separators
        prefix = prefix.replace("-", "_")
        if command.startswith("do_show_"+prefix) and not command.startswith("do_show_vns_"):
            return True
        elif command.startswith("do_show_vns_" + prefix):
            if self.in_config_submode("config-vns"):
                if command == 'do_show_vns_access_list' and self.in_config_vns_mode():
                    return True
                return self.vns_debug_show_commands(command)
            return False
        return False

    #
    # --------------------------------------------------------------------------------
    # get_valid_show_options 
    #  used by cp_show to identify completion optinos
    #
    # returns a dictionary with two elements: 'commands', and 'objects'.
    # the 'commands' element is a list of possible commands while
    # the 'objects' element is a list of possible objects (tables/models in the store)
    #
    def get_valid_show_options(self, text):
        ret_hash = {}
        # first get commands
        opts = []
        matching_methods = [x for x in dir(self)
                            if self.show_command_prefix_matches(x, text)]

        if onos == 0:
            netvirt_feature = self.netvirt_feature_enabled()
        else:
            netvirt_feature = False

        for method in matching_methods:
            m = re.search("do_show_(.*)", method)
            n = re.search("do_show_vns_(.*)", method)
            if n and self.in_config_submode('config-vns'):
                self.append_when_missing(opts, n.group(1).replace("_","-"))
            elif m:
                if netvirt_feature or m.group(1) != 'vns':
                    self.append_when_missing(opts, m.group(1).replace("_","-"))
        #
        # remove command cases
        opts = [x for x in opts if x not in ["statistics", "object"]]
        if "access-group" in opts and not self.in_config_vns_if_mode():
            if not self.vns_debug_show_commands('vns-access-group'):
                opts.remove("access-group")
        if "access-list" in opts and not self.in_config_vns_mode():
            if not self.vns_debug_show_commands('vns-access-list'):
                opts.remove("access-list")
        if "interface" in opts and not self.in_config_vns_mode():
            if not self.vns_debug_show_commands('vns-interface'):
                opts.remove("interface")
        if "interface-rule" in opts and not self.in_config_vns_def_mode():
            if not self.vns_debug_show_commands('vns-interface-rule'):
                opts.remove("interface-rule")
        if "firewall" in opts and not self.in_config_controller_interface_mode():
            opts.remove("firewall")
        # synthetic object type based on the vnsconfig submode.
        if "access-list-entry" in opts and not self.in_config_vns_acl_mode():
            if not self.vns_debug_show_commands('vns-access-list-entry'):
                opts.remove("access-list-entry")
        ret_hash["commands"] = opts

        # now get obj_types we can show
        opts = self.all_obj_types_starting_with(text)
        if self.in_config_submode() and "this".startswith(text):
            opts.append("this")
        ret_hash["objects"] = opts
        return ret_hash


    #
    # --------------------------------------------------------------------------------
    # cp_show_object
    #  show <obj_type> (words[0] == obj_type)
    #
    def cp_show_object(self, words, text, completion_char):
        if len(words) == 1:
            return objects_starting_with(words[0], text)
        else:
            self.print_completion_help("<cr>")
    #
    # --------------------------------------------------------------------------------
    # get_attachment_points
    #
    @staticmethod
    def get_attachment_points(host, host_dap_dict):
        items = host_dap_dict.get(host['mac'])
        if not items or len(items) <= 1:
            return items
        #
        # sort by most recent items, if the last-seen field
        # exists for all entries
        #
        for item in items:
            if not 'last-seen' in item or item['last-seen'] == '':
                return items

        ordered = sorted(items,
                         key=lambda i: i['last-seen'],
                         cmp=lambda x,y: cmp(y,x))
        ordered[0]['prime'] = True
        return ordered

    #
    # --------------------------------------------------------------------------------
    # create_attachment_dict
    #
    def create_attachment_dict(self):
        return create_obj_type_dict('host-attachment-point', 'mac')

    #
    # --------------------------------------------------------------------------------
    # get_ip_addresses
    #
    @staticmethod
    def get_ip_addresses(host, host_ip_dict):
        items = host_ip_dict.get(host['mac'])
        if not items or len(items) <= 1:
            return items

        for item in items:
            if not 'last-seen' in item or item['last-seen'] == '':
                return items
        #
        # sort by most recent items, if every row has a 'last-seen' field
        #
        ordered = sorted(items,
                         key=lambda i: i['last-seen'],
                         cmp=lambda x,y: cmp(y,x))
        ordered[0]['prime'] = True
        return ordered

    #
    # --------------------------------------------------------------------------------
    # create_ip_dict
    #  Returns a dictionary with the host id (mac address) as the key,
    #  and a list of associated rows for that host id.
    #
    def create_ip_dict(self):
        return create_obj_type_dict('host-network-address', 'mac')

    #
    # --------------------------------------------------------------------------------
    # show_sort_obj_type
    #  Returns a sorted list of entries based on the sort described by
    #  the obj-dict
    #
    def show_sort_obj_type(self, obj_type, entries):
        sort = mi.obj_type_show_sort(obj_type)
        if not sort:
            return entries
        if sort == 'integer':
            key = mi.pk(obj_type)
            return sorted(entries, key=lambda k: k[key].split('|')[-1],
                                   cmp=lambda x,y: cmp(utif.try_int(x), utif.try_int(y)))
        elif sort == 'tail-integer':
            key = mi.pk(obj_type)
            def comparer(left, right):
                prefix = cmp(left[:-1], right[:-1])
                if prefix != 0:
                    return prefix
                return cmp(utif.try_int(left[-1]), utif.try_int(right[-1]))

            return sorted(entries, key=lambda k: k[key].split('|'), cmp=comparer)
        else:
            return self.error_msg("Unknown sort %s" % sort)

    #
    # --------------------------------------------------------------------------------
    # display_obj_type_rows
    #  Given an obj_type, rows, and whether or not a key was provided to search
    #  with, generate the output table.
    #
    def display_obj_type_rows(self, obj_type, rows, with_key = '<no_key>', detail = "default"):
        #
        # handle spedicific issues with particular tables returned from storage
        # - change vns-id's with "|" into separate columns
        # - the host case does a 'merge' for attachment-points and IPs
        #   (used in 'show host')
        def handle_specific_obj_type(obj_type, entries, field_orderings = 'default'):
            if obj_type == "host":
                # host_tag_dict = create_obj_type_dict('tag-mapping', 'host')
                for host in entries:
                    host['vendor'] = self.vendordb.get_vendor(host['mac'])
                    # host['tag'] = host_tag_dict.get(host['mac'], [])
            if obj_type == "host-config":
                host_dap_dict = self.create_attachment_dict()
                host_ip_dict = self.create_ip_dict()
                host_tag_dict = create_obj_type_dict('tag-mapping', 'mac')
                for host in entries:
                    host['vendor'] = self.vendordb.get_vendor(host['mac'])
                    host['attachment-points'] = self.get_attachment_points(host, host_dap_dict)
                    host['ips'] = self.get_ip_addresses(host, host_ip_dict)
                    host['tag'] = host_tag_dict.get(host['mac'], [])
            if obj_type == 'controller-node':
                for controller_node in entries:
                    domain_name_servers = []
                    domain_name_servers = self.rest_query_objects(
                        'controller-domain-name-server',
                        {'controller': controller_node['id'],
                         'orderby' : 'timestamp'})
                    controller_node['domain-name-servers'] = [domain_name_server['ip']
                        for domain_name_server in domain_name_servers]
                    controller_node['id'] = ' '.join(controller_node['id'].split('|'))
            if obj_type == 'controller-interface':
                for intf in entries:
                    rules = [x['rule'] for x in self.get_firewall_rules(intf['id'])]
                    intf['firewall'] = ', '.join(rules)
                    # del intf['id'] XXX should the id really get displayed?
            tables_with_vns_ids = [ "vns-interface-rule", "vns-interface",
                                    "host-vns-interface", "vns-access-list",
                                    "vns-access-list-entry", "vns-interface-access-list" ]
            if obj_type in tables_with_vns_ids:
                self.vns_table_id_to_vns_column(obj_type, entries)
                if self.in_config_submode("config-vns") and field_orderings == 'default':
                    field_orderings = 'vns-config'
                self.vns_join_host_fields(obj_type, entries)
                self.vns_foreign_key_to_base_name(obj_type, entries)
            if obj_type == 'vns-access-list-entry':
                # display the alternative access-list-text format 
                vns_acl_entries_to_brief(entries)
                # field_orderings = 'acl-brief'
            if obj_type == 'vns-access-list-entry-brief':
                # display the alternative access-list-text format 
                vns_acl_entries_to_brief(entries)
                field_orderings = 'acl-brief'
            #if obj_type == 'tag-mapping':
                #self.handle_specific_tag_fields(entries)

            # XXX may need to revisit to prevent don't leak vns information

            if obj_type in ['dvs-port-group', 'firewall-rule', 'tag-mapping']:
                for entry in entries:
                    mi.split_compound_into_dict(obj_type,
                                                mi.pk(obj_type),
                                                entry)
            #
            # objects where foreigh keys are references to
            # compount key's, needing to be split. 
            # XXX these can be identified by scanning the objects during init
            if obj_type in ['switch-interface-alias',
                           ]:
                # only pick foreign keys which are compound keys
                fks = [x for x in mi.obj_type_foreign_keys(obj_type)
                       if mi.is_compound_key(obj_type,x)]
                for entry in entries:
                    for fk in fks:
                        if fk in entry: # fk may be null-able
                            mi.split_compound_into_dict(obj_type, fk, entry)
                    if 'switch-interface' in entry:
                        si = entry['switch-interface']
 
            #
            if obj_type == 'firewall-rule':
                self.firewall_rule_add_rule_to_entries(entries)
            return field_orderings
     
        field_orderings = handle_specific_obj_type(obj_type, rows, detail)
        #
        # join alias names.  there is a good chance the alias's tables
        #  are small.  since iteration over all the rows is needed,
        #  first identify the alias tables involved, then snap the
        #  complete table into memory, and then decorae with the
        #  specific alias fields.  XXX it would be even better to
        #  get a size of 'rows' vs the 'alias' table, and choose
        #  the more performance path...
        #
        if obj_type in mi.alias_obj_type_xref:
            obj_key = mi.pk(obj_type)
            for alias in mi.alias_obj_type_xref[obj_type]:
                alias_field = mi.alias_obj_type_field(alias)
                if not alias_field:
                    print self.error_msg("internal alias in display_obj_type")
                try:
                    alias_rows = self.get_table_from_store(alias)
                except Exception, e:
                    errors = self.rest_error_to_dict(e, alias)
                    print self.rest_error_dict_to_message(errors)
                    alias_rows = []

                # invert the row once, moving the key to the data.
                alias_key = mi.pk(alias)
                rows_alias = dict([[a[alias_field], a[alias_key]] for a in alias_rows])

                # collect the name of the field to join (fk_name)
                # 
                (fk_obj_type, fk_name) = \
                    mi.foreign_key_references(alias, alias_field)
                # now associate alias's with the row's field:
                for row in rows:
                    if fk_name in row:
                        if row[fk_name] in rows_alias:
                            row[alias] = rows_alias[row[fk_name]]
                    elif fk_obj_type in row:
                        if row[fk_obj_type] in rows_alias:
                            row[alias] = rows_alias[row[fk_obj_type]]
            #
            # this choice is better if the alias's table is large.
            if 0: #for row in rows:
                key = mi.pk(obj_type)
                for alias in mi.alias_obj_type_xref[obj_type]:
                    field = mi.alias_obj_type_field(alias)
                    if not field:
                        print self.error_msg("internal alias in display_obj_type")
                    alias_row = self.get_table_from_store(alias,
                                                          field,
                                                          row[key],
                                                          "exact")
                    if len(alias_row) > 1:
                        print 'Error: multiple aliases for %s' % \
                              alias
                    elif len(alias_row) == 1:
                        row[alias] = alias_row[0][mi.pk(alias)]

        if len(rows) == 0:
            if with_key != '<no_key>':
                return self.error_msg("No %s %s found" % (obj_type, with_key))
            else:
                return self.pp.format_table(rows, obj_type, field_orderings)
        elif len(rows) == 1 and with_key != '<no_key>':
            return self.pp.format_entry(rows[0], obj_type,
                                        debug = self.debug)

        return self.pp.format_table(self.show_sort_obj_type(obj_type, rows),
                                    obj_type,
                                    field_orderings)

    #
    # --------------------------------------------------------------------------------
    # do_show_object
    #  Implements the show command for objects (table/model output of rows)
    #
    def do_show_object(self, words, with_search_key = None):
        obj_type = words[0]

        #
        # The cli replaces some user names with table names
        if obj_type == 'tag':
            obj_type = 'tag-mapping'

        match = "startswith"
        #
        # Allow "this" reference if we are in config-submode, change the
        # match to an exact match to prevent prefix matching
        if obj_type == "this":
            if self.in_config_submode():
                obj_type = self.get_current_mode_obj_type()
                words = [obj_type, "this"]
                match = "exact"
            else:
                return self.error_msg("\"this\" can only be used in configuration"
                                      " submode, such as switch, port, host, etc.")
        #
        # complete table lookup: 'show <table>'
        if len(words) == 1:
            if with_search_key == None:
                with_search_key = "<no_key>"
            errors = None
            try:
                entries = self.get_table_from_store(obj_type)
            except Exception, e:
                errors = self.rest_error_to_dict(e, obj_type)
                
            if errors:
                return self.rest_error_dict_to_message(errors)
            return self.display_obj_type_rows(obj_type, entries, with_search_key)

        # table with key:  "show <table> <key>"
        if len(words) == 2:
            obj = words[1]
            if with_search_key == None:
                with_search_key = obj

            # Allow "this" reference if we are in config-submode
            if obj == "this" and \
              obj_type == self.get_current_mode_obj_type() \
              and self.in_config_submode():
                obj = self.get_current_mode_obj()

            key = mi.pk(obj_type)
            if key:
                obj = convert_alias_to_object_key(obj_type, obj)
                if mi.obj_type_has_model(obj_type):
                    entries = self.get_table_from_store(obj_type, key, obj, match)
                else:
                    # XXX raise internal error?  model doesn't exist.
                    print command._line(), obj_type
                    entries = []
                return self.display_obj_type_rows(obj_type, entries, with_search_key)
            else:
                return self.pp.format_table(self.get_table_from_store(obj_type),
                                            obj_type)
        elif "stats".startswith(words[2]):
            return self.helper_show_object_stats(words)
        else:
            return "Syntax: show <table> <key>:  " \
                    "Unrecognized input: " + " ".join(words[2:])

    #
    # --------------------------------------------------------------------------------
    # helper_show_object_stats
    #
    def helper_show_object_stats(self, words):
        if self.stats_metadata == None:
            self.init_stats_metadata()

        # get object type
        object_type = words[0]

        # The rest stats api differs from the CLI in some of the
        # object names, so this map allows us to fix it
        if object_type in self.stats_restapi_map:
            object_type = self.stats_restapi_map[object_type]

        if object_type not in self.stats_type_metadata:
            return self.error_msg('no statistics available for object type %s' % 
                                  object_type)

        # Get the specific object to operate on
        sobject = words[1];
        
        # the controller-node objects don't currently have any real
        # concept of a unique ID for a controller; for now the stats
        # are always labeled 'localhost' even though that will never
        # appear in the controller-node list
        if ('controller-node'.startswith(object_type)):
            sobject = 'localhost'

        tocheck = []
        # get statistic name
        if len(words) > 3:
            if (words[3] not in self.stats_metadata or 
                self.stats_metadata[words[3]]['target_type'] != object_type):
                return self.error_msg('no statistic %s available for object type %s' % 
                                      (words[3], object_type))
            tocheck.append(words[3])
        else:
            tocheck = [item['name'] 
                       for item in self.stats_type_metadata[object_type]]

        parsed_options = {}
        if (len(words) > 4):
            try:
                parsed_options = \
                    self.parse_optional_parameters(self.stats_optional_params, 
                                                   words[4:])
            except ParamException as e:
                return self.error_msg(e)

        display = 'latest-value'
        timespec = False
        now = int(time.time()*1000)
        query_params = {'start-time': now - 3600000,
                        'end-time': now,
                        'sample-count': 25,
                        'data-format': 'value'}

        if 'start-time' in parsed_options:
            timespec = True
            try:
                query_params['start-time'] = self.parse_date(parsed_options['start-time'])
            except Exception as e:
                return self.error_msg('invalid start time %s: %s' % 
                                      (parsed_options['start-time'], e))

        if 'end-time' in parsed_options:
            timespec = True
            try:
                query_params['end-time'] = self.parse_date(parsed_options['end-time'])
            except Exception as e:
                return self.error_msg('invalid end time %s: %s' % 
                                      (parsed_options['end-time'], e))

        if 'duration' in parsed_options:
            timespec = True
            try:
                query_params['duration'] = parsed_options['duration']
                if 'start-time' in parsed_options:
                    del query_params['end-time']
                else:
                    del query_params['start-time']

            except:
                return self.error_msg('invalid duration %s' % 
                                      parsed_options['duration'])

        if 'display' in parsed_options:
            display = parsed_options['display']

        if timespec and display == 'latest-value':
            display = 'graph'

        if display == 'graph':
            query_params['sample-count'] = self.pp.get_terminal_size()[0]

        for p in ['sample-count', 'limit', 'sample-window', 
                  'sample-interval', 'data-format']:
            if p in parsed_options:
                query_params[p] = parsed_options[p]

        if display == 'latest-value':
            data = {}
            for item in tocheck:
                url = ('http://%s/rest/v1/stats/data/%s/%s/%s/%s/' % 
                       (self.controller, self.cluster, object_type, 
                        sobject, item))
                try:
                    (timestamp, value) = self.rest_simple_request_to_dict(url)
                    data[self.stats_metadata[item]['name']] = "%d" % value
                except:
                    data[self.stats_metadata[item]['name']] = "[Null]"

            return self.pp.format_entry(data, 'stats-%s' % object_type)
        else:
            item = tocheck[0]
            url = ('http://%s/rest/v1/stats/data/%s/%s/%s/%s/?%s' % 
                       (self.controller, self.cluster, object_type, 
                        sobject, item, "&".join(['%s=%s' % (k,v)
                                                 for k,v 
                                                 in query_params.iteritems()])))
            try:
                json_data = self.rest_simple_request_to_dict(url)
            except Exception as e:
                if self.debug_backtrace:
                    print 'FAILED URL', url
                return self.error_msg('could not load stats data: %s' % e)

            if display == 'table':
                data = []
                for (timestamp, value) in json_data:
                    data.append({'timestamp': int(timestamp),
                             'value': value})
                return self.pp.format_table(data, 'stats-%s-%s-%s' % 
                                            (object_type, item, 
                                             query_params['data-format']))
            elif display == 'graph':
                return self.pp.format_time_series_graph(json_data, 
                                                        'stats-%s-%s-%s' % 
                                                        (object_type, item, 
                                                         query_params['data-format']))
    #
    # --------------------------------------------------------------------------------
    # parse_date
    #
    @staticmethod
    def parse_date(text):
        if (text == 'now' or text == 'current'):
            return int(time.time()*1000)

        try:
            return int(text)
        except: 
            pass

        for f, pre in [('%Y-%m-%dT%H:%M:%S', None),
                       ('%Y-%m-%d %H:%M:%S', None),
                       ('%Y-%m-%dT%H:%M:%S%z', None),
                       ('%Y-%m-%d %H:%M:%S%z', None),
                       ('%Y-%m-%d', None),
                       ('%m-%d', '%Y-'),
                       ('%H:%M', '%Y-%m-%dT')]:
            try:
                t = text
                if pre:
                    pref = datetime.datetime.now().strftime(pre)
                    f = pre + f
                    t = pref + t

                thetime = datetime.datetime.strptime(t, f)
                return int(time.mktime(thetime.timetuple())*1000)
            except: 
                pass

        raise ValueError('count not parse %s as a timestamp' % text)


    #
    # --------------------------------------------------------------------------------
    # display_vns_interface
    #  Search dist is a collection of compund key entities
    #
    def display_vns_interface(self, vns_name, search_dict, with_search_key, detail = 'default'):
        obj_type = 'vns-interface'
        obj_key = mi.pk(obj_type)
        try:
            entries = rest_to_model.get_model_from_url(obj_type, search_dict)
            #entries = self.rest_query_objects(obj_type,
                                              #search_dict)
            errors = None
        except Exception, e:
            errors = self.rest_error_to_dict(e, obj_type)
                
        if errors:
            return self.rest_error_dict_to_message(errors)
        vns_rules = create_obj_type_dict('vns-interface-rule',
                                         mi.pk('vns-interface-rule'),
                                         search_dict
                                         )
        #
        # divide up the result into two parts, the first part is
        # the physical interfaces, the second is the mac interfaces...
        phys = []
        macs = []
            
        for entry in entries:
            fields = entry[obj_key].split('|')
            entry['tenant'] = fields[0]
            entry['vns']=fields[1]
            # all these fields must have two parts.
            if len(fields) != 3:
                continue
            
            # choose between switch based rules and mac/ip based rules
            rule = 'default'
            switch = None
            virt = None
            #
            # Each address-space would have a default vns named
            # named <as>-default, except the 'default' adddress-space which
            # has a default vns named just 'default'
            #
            if fields[1] == "default" or fields[1].endswith("-default"):
                virt = 'default'
                if fields[2].find('/') >= 0:
                    virt = fields[2].split('/')[1]
            else:
                if not 'rule' in entry or entry['rule']=='default':
                    if_rule = []
                    virt = True
                    # continue
                else:
                    rule = entry['rule']
                    if_rule = vns_rules[rule][0]
                
                if 'switch' in if_rule:
                    switch = if_rule['switch']
                elif 'mac' in if_rule:
                    virt = if_rule['mac']
                    entry['mac'] = virt
                elif 'ip-subnet' in if_rule:
                    virt = {'ip-address': if_rule['ip-subnet']}
                    if 'ips' in entry:
                        entry['ips'].append(virt)
                    else:
                        entry['ips'] = [virt]
                elif 'tags' in if_rule:
                    virt = if_rule['tags']
                    entry['tags'] = virt
                elif 'vlans' in if_rule:
                    virt = "vlan %s" % if_rule['vlans']
                    entry['vlans'] = virt

            if switch:
                phys.append(entry)
            if virt:
                entry['veth'] = entry[obj_key].split('|')[2]
                macs.append(entry)

        output = ''
        if len(phys):
            self.vns_join_switch_fields(vns_name, phys)
            output += self.display_obj_type_rows('vns-interface-phys', phys, with_search_key, detail)
        if len(macs):
            if len(phys):
                output += '\n'
            self.vns_join_host_fields(obj_type, macs)
            output += self.display_obj_type_rows('vns-interface-macs', macs, with_search_key, detail)

        if output != '':
            return output
        return self.pp.format_table([],'display-vns-interface')

    #
    # --------------------------------------------------------------------------------
    # find_and_display_vns_interfaces
    #
    def find_and_display_vns_interface(self, vns_name, words):
        with_search_key = "<no_key>"
        search_dict = { 'vns' : vns_name }
        if len(words) == 1:
            search_dict['interface'] = words[0]
        elif len(words) > 1:
            return self.error_msg("Additional search keys after interace")

        return self.display_vns_interface(vns_name, search_dict, with_search_key)
        
    #
    # --------------------------------------------------------------------------------
    # do_show_vns_interfaces
    #  This definition is needed to allow the vns-interface table/model to
    #  be displayed in basic config mode.  do_show() searches names of functions
    #  using the prefix of the 'show' argument; without this function,
    #  it find 'do_show_vns_interface_rule' and selects that function to
    #  display results for 'show vns-interface`.
    #
    def do_show_vns_interfaces(self, words):
        with_search_key = "<no_key>"
        local_vns_name = self.vns_name()
        if self.vns_name() is None:
            if len(words) > 0:
                local_vns_name = words[0]
                search_dict = { 'vns' : local_vns_name }
            else:
                # only case where the vns name is included is the output 
                return self.do_show_object(['vns-interface'], with_search_key)
        else:
            search_dict = { 'vns' : local_vns_name }
            if len(words) == 1:
                with_search_key = words
                search_dict['interface'] = words[0]
            elif len(words) > 2:
                return self.error_msg("Additional search keys after interace")

        return self.display_vns_interface(local_vns_name, search_dict, with_search_key)


    #
    # --------------------------------------------------------------------------------
    # display_vns_mac_address_table
    #  Used by 'show vns <n> mac-address-table', and vns mode 'show mac-address-table'
    #
    def display_vns_mac_address_table(self, vns_name, words):
        # note: 'config-vns' matches both vns and vns-definition mode,
        #  while 'config-vns-' matches only vns-definition mode.
        # the id for the host-vns-interface table has the host as the 'prefix',
        # preventing searching based on the prefix.  the vns is not even a foreign
        # key, which means the complete table must be read, then the vns association
        # must be determined, and then matched
        filter = { 'vns' : vns_name } if vns_name != 'all' else {}
        if len(words):
            filter['mac'] = words[0]
        entries = rest_to_model.get_model_from_url('host-vns-interface', filter)
        for entry in entries:
            fields=entry['vns'].split('|')
            entry['tenant']=fields[0]
            entry['vns']=fields[1]
        with_key = '<no_key>'
        if len(words) > 0:
            with_key = words[0]

        #detail = 'vns'          # exclude vns column, vns is named
        detail = 'default'          # exclude vns column, vns is named
        if vns_name == 'all':
            detail = 'default'  # include vns column, vns not named

        # self.vns_join_host_fields('host-vns-interface', entries)
        return self.display_obj_type_rows('host-vns-interface-vns', entries, with_key, 'default')
        
    #
    # --------------------------------------------------------------------------------
    # do_show_vns_mac_address_table
    #  config and vns mode 'show mac-address-table' command
    #
    def do_show_vns_mac_address_table(self, words):
        if words == None or len(words) > 2:
            return self.syntax_msg('show mac-address-table <host>')

        if self.in_config_submode('config-vns'):
            return self.display_vns_mac_address_table(self.vns_name(), words)

        if len(words) > 0:
            return self.do_show_object(['host'] + words, 'with_key')
        return self.do_show_object(['host'])

    #
    # --------------------------------------------------------------------------------
    # do_show_switch_cluster
    #
    def do_show_switch_cluster(self, words):
        data = self.rest_simple_request_to_dict(
                    'http://%s/rest/v1/realtimestatus/network/cluster/' % self.controller)

        formatted_data = []
        for (clusterid, switchids) in data.items():
            row = {}
            row['switches'] = switchids
            row['cluster-id'] = clusterid
            formatted_data.append(row)
        return self.display_obj_type_rows('switch-cluster', formatted_data)
    
    #
    # --------------------------------------------------------------------------------
    # do_show_controller_summary
    #
    def do_show_controller_summary(self, words):
        data = self.rest_simple_request_to_dict(
                    'http://%s/rest/v1/controller/summary' % self.controller)

        # XXX Need a way to sort the data in a way that makes sense
        for (key, value) in data.items():
            yield key + ":  " + str(value) + "\n"
    
    #
    # --------------------------------------------------------------------------------
    # get_statistics_type
    #
    @staticmethod
    def get_statistics_type(stat_type):
        for s in ["flow", "port", "table", "desc", "aggregate", "features", "trace",]:
            if s.startswith(stat_type):
                return s
        return None

    #
    # --------------------------------------------------------------------------------
    # fix_realtime_flows_wildcard_fields
    #
    def fix_realtime_flow_wildcard_fields(self, in_data):
        for l in in_data:
            if 'wildcards' in l:
                wildcards = l['wildcards']
                bit_field_map = {
                    0 : 'inputPort',
                    1 : 'dataLayerVirtualLan',
                    2 : 'dataLayerSource',
                    3 : 'dataLayerDestination',
                    4 : 'dataLayerType',
                    5 : 'networkProtocol',
                    6 : 'transportSource',
                    7 : 'transportDestination',
                    13 : 'networkSource',
                    19 : 'networkDestination',
                    20 : 'dataLayerVirtualLanPriorityCodePoint',
                    21 : 'networkTypeOfService',
                    }
                for (k, v) in bit_field_map.items():
                    if wildcards & (1 << k):
                        l[v] = "*"

    #
    # --------------------------------------------------------------------------------
    # fix_realtime_flows
    #
    def fix_realtime_flows(self, in_data):
        if not in_data:
            return
        # flatten match data
        for l in in_data:
            match_dict = l['match']
            l.update(match_dict)
            del(l['match'])

        self.fix_realtime_flow_wildcard_fields(in_data)
        return in_data

    #
    # --------------------------------------------------------------------------------
    # add_dpid_to_data
    #
    @staticmethod
    def add_dpid_to_data(data):
        if not type(data) is dict: # backward-compatible
            return data 
        formatted_list = []
        for (dpid, rows) in data.items():
            if rows != None:
                for row in rows:
                    row['switch'] = dpid
                    formatted_list.append(row)
        ## TODO - Alex - handle timeouts + sorting
        return formatted_list

    #
    # --------------------------------------------------------------------------------
    # do_show_statistics
    #
    # LOOK! we could get rid of show statistics at this point as it is all under
    # show switch!
    #
    # it's confusing to remap it over here...
    # we could remove show flow-entry realtime too for now
    #
    def do_show_statistics(self, words):
        if len(words) < 1:
            return "Syntax: show statistics < flow | port | table | desc | aggregate | " \
                                              "features | trace > < Switch DPID | Switch alias | all >"
        stat_type = self.get_statistics_type(words[0])
        dpid = None
        if len(words) > 1:
            words[1] = convert_alias_to_object_key("switch", words[1])
            dpid = words[1] # LOOK! Eventually optional so we can show across switches
            if not dpid in objects_starting_with("switch", dpid) and dpid != "all": 
                return self.error_msg("No switch %s found" % dpid)
        if stat_type == "flow":
            data = self.rest_simple_request_to_dict(
                             'http://%s/rest/v1/realtimestats/flow/%s/' % (self.controller,dpid))            
            data = self.add_dpid_to_data(data)            
            data = self.fix_realtime_flows(data)            

            # 
            # it may make sense to move ['details', 'brief'] tp some sort of
            # 'show_object_optional_format_keywords' list
            field_ordering = "default"
            if len(words) == 3:
                choices = [x for x in ["details", "brief"] if x.startswith(words[2])]
                if len(choices) != 1:
                    return self.error_msg("switch flow options are either 'details' or 'brief'")
                else:
                    field_ordering = choices[0]
            table_data = self.pp.format_table(data, "realtime_flow", field_ordering)
            return table_data
        elif stat_type == "trace":
            return self.do_trace(words)
        elif words[0] == "switches":
            data = self.rest_simple_request_to_dict(
                            'http://%s/rest/v1/controller/stats/%s/' % (self.controller, words[0]))
            table_data = self.pp.format_table(data, "controller_"+words[0])
            return table_data
        else:
            data = self.rest_simple_request_to_dict(
                            'http://%s/rest/v1/realtimestats/%s/%s/' % (self.controller, stat_type, dpid))
            if stat_type == "features":
                for dpid in data:
                    data[dpid] = data[dpid]["ports"]
            data = self.add_dpid_to_data(data)
            table_data = self.pp.format_table(data, "realtime_"+stat_type)
            return table_data

    #
    # --------------------------------------------------------------------------------
    # do_show_version
    #
    def do_show_version(self, words):
        # LOOK! this should probably come from the controller, but for now
        # we assume the CLI/controller are from the same build
        version_string = "SDNOS 1.0 - custom version"
        try:
            fh = open("/opt/sdnplatform/release") 
            version_string = fh.readline().rstrip()
            fh.close()
        except:
            pass
        return version_string

    #
    # --------------------------------------------------------------------------------
    # do_date
    #
    @staticmethod
    def do_date(words):
        return time.strftime("%Y-%m-%d %H:%M:%S %Z", time.localtime())

    #
    # --------------------------------------------------------------------------------
    # cp_show_logging
    #
    def cp_show_logging(self, words, text, completion_char):
        if len(words) == 1:
            return [x for x in ["sdnplatform", "cassandra", "console-access",
                                 "syslog", "authlog", "orchestrationlog",
                                 "all" ] if x.startswith(text)]
        else:
            self.print_completion_help("<cr>")

    #
    # --------------------------------------------------------------------------------
    # generate_subpprocess_output
    #
    def generate_subprocess_output(self, cmd):
        process = subprocess.Popen(cmd, shell=True,
                                   stdout=subprocess.PIPE, 
                                   stderr=subprocess.STDOUT,
                                   bufsize=1)
        while True:
            line = process.stdout.readline()
            if line != None and line != "":
                yield line
            else:
                break

    #
    # --------------------------------------------------------------------------------
    #
    def implement_show_logging(self, words):
        log_files = { "sdnplatform" : "cat /opt/sdnplatform/sdnplatform/log/sdnplatform.log",
                      "cassandra" : "cat /opt/sdnplatform/db/log/system.log",
                      "console-access" : "cat /opt/sdnplatform/con/log/access.log",
                      "syslog" : "cat /var/log/syslog",
                      "authlog" : "cat /var/log/auth.log",
                      "orchestrationlog" : "cat /tmp/networkservice.log",
                      "dmesg" : "dmesg",
                      "all" : ""  # special
                      }
        if len(words) < 1:
            yield "Syntax: show logging < %s > " % " | ".join(log_files.keys())
            return
        for (f, cmd) in log_files.items():
            if (f.startswith(words[0]) or "all".startswith(words[0])) and f != "all":
                yield "*"*80 + "\n"
                yield "output of %s for %s\n" % (cmd, f)
                yield "*"*80 + "\n"
                for item in self.generate_subprocess_output(cmd):
                    yield item

    #
    # --------------------------------------------------------------------------------
    # do_show_tech_support
    #
    def do_show_tech_support(self, words):

        oldd = self.debug
    
        tech_support_table = []

        # Add the user customized command in tech-support-config table
        entries = self.get_table_from_store('tech-support-config')
        for entry in entries:
            # need explicit type casting str() to make command handling work
            tech_support_table.append([str(entry['cmd-type']), str(entry['cmd']), None])

        # Add the list of command configured for the basic support
        for entry in tech_support.get_show_tech_cmd_table():
            tech_support_table.append(entry)

    
        for entry in tech_support_table:
            cmd_type = entry[0]
            cmd = entry[1]
            feature = entry[2]
    
            # If feature is non-null, then check feature is enabled 
            # before executing the command
            if feature != None and self.feature_enabled(feature) == False :
                continue
    
            if cmd_type == 'cli':
                yield "\n\n" + "-"*80 + "\nExecute cli: " + cmd + "\n\n"
                try:
                    for item in self.generate_command_output(self.handle_single_line(cmd)):
                        yield item
                except Exception as e:
                    yield "Failed to execute %s: %s\n" % (cmd, e)
            elif cmd_type == 'shell':
                yield "\n\n" + "-"*80 + "\nExecuting os command: " + cmd + "\n\n"
                try:
                    for item in self.generate_subprocess_output(cmd):
                        yield item
                except Exception as e:
                    yield "Failed to run command %s: %s\n" % (cmd, e)
            else:
                yield "Unknown command type %s: %s\n" % (cmd_type, cmd)
    
        self.debug = oldd



    #
    # --------------------------------------------------------------------------------
    # ell_ess_minus_r
    #  Do "ls -R", returing the list of files underneath "prefix".
    #  The intended external call is 
    #  ell_ess_minus_r(returned_items, "/opt/sdnplatform/...",  ""),
    #  where the path parameter is null to begin the walk.
    #
    #  The returned items won't have the original prefix in the items
    #
    #  XXX: There's no bounding for the recursive descent.
    #
    def ell_ess_minus_r(self, items, prefix, path):
        p = os.path.join(prefix, path)
        if os.path.isdir(p):
            for entry in os.listdir(p):
                new_path = os.path.join(path, entry)
                if os.path.isdir(os.path.join(prefix, new_path)):
                    self.ell_ess_minus_r(items, prefix, new_path)
                else:
                    items.append(os.path.join(path, entry))
        return items
            
    #
    # --------------------------------------------------------------------------------
    # implement_show_config_file
    #  'show config file' displays the local files under self.saved_configs_dirname
    #   currently /opt/sdnplatform/run/saved-configs
    #
    def implement_show_config_file(self, words):
        items = []
        self.ell_ess_minus_r(items, self.saved_configs_dirname, "")

        if len(words) == 1:
            entries = []
            for i in items:
                cf = os.path.join(self.saved_configs_dirname, i)
                t = time.strftime("%Y-%m-%d.%H:%M:%S",
                                  time.localtime(os.path.getmtime(cf)))
                entries.append({'name' : i,
                                'timestamp': t,
                                'length' : str(os.path.getsize(cf)),
                               })
            return self.pp.format_table(entries,'config')
        elif len(words) == 2:
            if words[1] in items:
                src_file = self.path_concat(self.saved_configs_dirname, words[1])
                if src_file == "":
                    return self.error_msg("file %s could not be " 
                                          "interpreted as a valid source" % words[1])
                try:
                    return self.store.get_text_from_url("file://" + src_file)
                except Exception, e:
                    return self.error_msg(words[1] + ":" + str(e))
            else:
                return self.error_msg("%s file unknown" % words[1])
    #
    #
    # --------------------------------------------------------------------------------
    # implement_show_config
    #
    def implement_show_config(self, words):
        name = None
        show_version = "latest"
        # handle sh config [ <name> [ all | <version #> ] ]

        if len(words) == 1:
            if words[0] == "all":
                show_version = "all"
            elif words[0] == "file":
                return self.implement_show_config_file(words)
            elif words[0].startswith('config://'):
                name = words[0][len('config://'):]
            else:
                name = words[0]
        elif len(words) == 2:
            if words[0] == 'file':
                return self.implement_show_config_file(words)
            name = words[0]
            show_version = words[1]
        elif len(words) == 3 and words[1] == "diff":
            return self.diff_two_configs(words[0], words[2])
        elif len(words) == 4 and words[1] == "diff":
            return self.diff_two_versions(words[0], words[2], words[3])

        data = self.store.get_user_data_table(name, show_version)
        if data and len(data) > 1 or name == None:
            return self.pp.format_table(data,'config')
        elif data and len(data) == 1:
            return self.store.get_user_data_file(data[0]['full_name'])[:-1]

    #
    # --------------------------------------------------------------------------------
    # diff_two_versions
    #
    def diff_two_versions(self, name, va, vb):
        va_info = self.store.get_user_data_table(name, va)
        if len(va_info) == 0:
            return self.error_msg("Version %s missing for %s" %  (va, name))
        va_txt = self.store.get_user_data_file(va_info[0]['full_name'])
        vb_info = self.store.get_user_data_table(name, vb)
        if len(vb_info) == 0:
            return self.error_msg("Version %s missing for %s" % (vb, name))
        vb_txt = self.store.get_user_data_file(vb_info[0]['full_name'])
        import tempfile
        fa = tempfile.NamedTemporaryFile(delete=False)
        fa.write(va_txt)
        fa.close()
        fb = tempfile.NamedTemporaryFile(delete=False)
        fb.write(vb_txt)
        fb.close()
        cmd = 'diff %s %s ' % (fa.name, fb.name)
        process = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE)
        (output, stderr) = process.communicate()
        os.unlink(fa.name)
        os.unlink(fb.name)
        return output

    #
    # --------------------------------------------------------------------------------
    # diff_two_configs
    #
    def diff_two_configs(self, config_a, config_b):
        items = self.store.get_user_data_table(config_a, "latest")
        if len(items) == 0:
            if config_a == 'running-config':
                txt_a = run_config.implement_show_running_config([]) + "\n"
            else:
                return self.error_msg("%s config missing" % config_a)
        else:
            txt_a = self.store.get_user_data_file(items[0]['full_name'])

        items = self.store.get_user_data_table(config_b, "latest")
        if len(items) == 0:
            if config_b == 'running-config':
                txt_b = run_config.implement_show_running_config([]) + "\n"
            else:
                return self.error_msg("%s config missing" % config_b)
        else:
            txt_b = self.store.get_user_data_file(items[0]['full_name'])

        import tempfile
        fa = tempfile.NamedTemporaryFile(delete=False)
        fa.write(txt_a)
        fa.close()
        fb = tempfile.NamedTemporaryFile(delete=False)
        fb.write(txt_b)
        fb.close()
        cmd = 'diff %s %s ' % (fa.name, fb.name)
        process = subprocess.Popen(cmd, shell=True, stdout=subprocess.PIPE)
        (output, stderr) = process.communicate()
        os.unlink(fa.name)
        os.unlink(fb.name)
        return output

    #
    # --------------------------------------------------------------------------
    #

    def get_host_attachment_switch_port (self, query_dict):
        """ 
        Get a host's attachment point if it is not already provided.
        see if the host has an attachment point, use the switch and port
        of the attachment point, if its available
        """
        host = rest_to_model.get_model_from_url('host', query_dict)
        if len(host) == 0:
            print self.error_msg('Can\'t identify host details: %s' % query_dict)
            return (False,None,None)
        attachment_points = host[0]['attachment-points']
        if attachment_points == None or len(attachment_points) == 0:
            print self.error_msg('Can\'t identify attachment point %s' % query_dict)
            return (False,None,None)
        if len(attachment_points) == 1:
            src_sw      = attachment_points[0]['switch']
            src_sw_port = attachment_points[0]['ingress-port']
            return (True, src_sw, src_sw_port)

        interface_dict = create_obj_type_dict('interfaces', 'id')
        show_list = []
        for ap in attachment_points:
            dpid = ap['switch']
            ingress_port = ap['ingress-port']

            # alias for dpid
            switch_name = alias_lookup_with_foreign_key('switch-alias', dpid)
            if not switch_name:
                switch_name = dpid
            # interface name for attachment point
            ap_port_id = dpid + '|' + str(ingress_port)
            if ap_port_id in interface_dict:
                show_list.append("%s/%s" % (switch_name,
                                            interface_port[ap_port_od][0]['portName']))
            else:
                show_list.append("%s/port %s" % (switch_name, ingress_port))
                
        print self.error_msg('src-host %s: multiple attachment points: %s' %
                             (src_mac, ', '.join(show_list)))
        print self.error_msg('rerun, and identify src-switch and interface')
        return (False,None,None)


    #
    # --------------------------------------------------------------------------
    # test packet-in: swap source and destination attributes 
    #
    def test_pkt_in_swap_src_dest_attributes(self, post_data):
        # Remove the source switch and port if needed
        if ("srcSwitchDpid" in post_data):
            post_data.pop("srcSwitchDpid")
        if ("srcSwitchInPort" in post_data):
            post_data.pop("srcSwitchInPort")

        #
        # Get destination host's attachment switch/port information
        #
        (result, src_sw, src_sw_port) = self.get_host_attachment_switch_port(
                                            { 'mac' : post_data["destinationMACAddress"]})
        if not result:
            return
        post_data["srcSwitchDpid"] = src_sw
        post_data["srcSwitchInPort"] = src_sw_port

        # Swap the macs
        temp = post_data["sourceMACAddress"]
        post_data["sourceMACAddress"] = post_data["destinationMACAddress"]
        post_data["destinationMACAddress"] = temp
        # swap the IP addresses if specified
        tempSrc = None
        tempDst = None
        if ("sourceIpAddress" in post_data):
            tempSrc = post_data["sourceIpAddress"]
            post_data.pop("sourceIpAddress")
        if ("destinationIpAddress" in post_data):
            tempDst = post_data["destinationIpAddress"]
            post_data.pop("destinationIpAddress")
        if (tempSrc is not None):
            post_data["destinationIpAddress"] = tempSrc
        if (tempDst is not None):
            post_data["sourceIpAddress"] = tempDst
        # swap the IP ports if specified
        tempSrc = None
        tempDst = None
        if ("ipL4SourcePort" in post_data):
            tempSrc = post_data["ipL4SourcePort"]
            post_data.pop("ipL4SourcePort")
        if ("ipL4DestinationPort" in post_data):
            tempDst = post_data["ipL4DestinationPort"]
            post_data.pop("ipL4DestinationPort")
        if (tempSrc is not None):
            post_data["ipL4DestinationPort"] = tempSrc
        if (tempDst is not None):
            post_data["ipL4SourcePort"] = tempDst

    #
    # --------------------------------------------------------------------------
    # test packet-in: display packet-in processing result for test packet-in
    #
    def test_pkt_in_result_display(self, response):
        out =  "\nResult of packet-in processing\n"
        out += ("------------------------------\n")
        src_vns_name = response['srcVNSName']
        dst_vns_name = response['destVNSName']

        if (response['explanation'] == "Source switch not found"):
            out += response['explanation']
            return out

        out +=  "\nVirtual Routing Processing iterations\n"
        out += ("--------------------------------------\n")
        ep_vrouting = response['expPktVRouting']
        for ep_vr in ep_vrouting:
            srcIface = ep_vr['srcIface']
            dstIface = ep_vr['dstIface']
            action = ep_vr['action']
            out += ("src VNS iface           : %s\n" % srcIface)
            out += ("dst VNS iface           : %s\n" % dstIface)
            out += ("action                  : %s\n" % action)
            if action == 'DROP':
                dropReason = ep_vr['dropReason']
                out += ("drop reason             : %s\n" % dropReason)
            else:
                nextHopIp = ep_vr['nextHopIp']
                out += ("next hop IP             : %s\n" % nextHopIp)
                nextHopGatewayPool = ep_vr['nextHopGatewayPool']
                if (nextHopGatewayPool != None):
                    out += ("next hop Gateway Pool   : %s\n" % nextHopGatewayPool)
            out += "\n"

        if (src_vns_name == None) and (dst_vns_name == None):
            out += "Source and destination hosts are not in the same vns"
            return out

        out += ("Status      : %s\n" % response['status'])
        out += ("Explanation : %s\n" % response['explanation'])

        out +=  "\nResult of Virtual Routing\n"
        out += ("--------------------------\n")
        out += ("Source VNS  : %s\n" % src_vns_name)
        out += ("Dest. VNS   : %s\n" % dst_vns_name)
        out += ("Input ACL:\n")
        out += ("  Input ACL Name    : %s\n" % response['inputAcl']['aclName'])
        out += ("  Input ACL Entry   : %s\n" % response['inputAcl']['aclEntry'])
        out += ("  Input ACL Action  : %s\n" % response['inputAcl']['aclResult'])
        out += ("Output ACL:\n")
        out += ("  Output ACL Name   : %s\n" % response['outputAcl']['aclName'])
        out += ("  Output ACL Entry  : %s\n" % response['outputAcl']['aclEntry'])
        out += ("  Output ACL Action : %s\n" % response['outputAcl']['aclResult'])

        if response['expPktRoute'] == None:
            out += "\n"
            return out

        out += ("Routing Action : %s\n" % response['routingAction'])

        obj_type_show_alias_update('test-pktin-route')
        out += ('\nFlow Path:\n')
        ep_route = response['expPktRoute']
        for ep_cluster in ep_route:
            ep_cluster_path = ep_cluster['path']
            hop = 1
            route_data = []
            for ep_path in ep_cluster_path:
                oneHop = {}
                oneHop['cluster'] = ep_cluster['clusterNum']
                oneHop['hop'] = hop
                hop += 1
                oneHop.update(ep_path)
                route_data.append(oneHop)
            route_data = self.pp.format_table(route_data, 'test-pktin-route')
            out += route_data
            out += "\n"
        return out

    #
    # --------------------------------------------------------------------------------
    # implement_test_packet_in
    #
    def implement_test_packet_in(self, data):
        # Currently only supports <test packet-in> for "explain packet" feature
        self.debug_msg("In do_test(): data=%s" % (data))

        # REST API expects MAC not host aliases
        if not 'src-host' in data:
            print self.error_msg("Missing src-host")
            return
        src_mac = data['src-host']

        if not 'dst-host' in data:
            print self.error_msg("Missing dst-host")
            return
        dst_mac = data['dst-host']

        # Check that src and dest hosts are not the same
        if src_mac == dst_mac:
            print self.error_msg("source and destination hosts can not be same")
            return

        #
        if 'src-switch' in data:
            src_sw      = data['src-switch']
            src_sw_port = data['src-switch-port']
        else:

            #
            # Get host's attachment switch/port information
            #
            (result, src_sw, src_sw_port) = \
                self.get_host_attachment_switch_port({'mac' : src_mac})
            if not result:
                return

        post_data = {
            "sourceMACAddress": src_mac, "destinationMACAddress": dst_mac,
            "srcSwitchDpid"   : src_sw,  "srcSwitchInPort"      : src_sw_port
        }

        if 'vlan' in data:
            post_data["vlanID"] = data['vlan']
        if 'ether-type' in data:
            post_data["etherType"] = data['ether-type']
            if (post_data['etherType'] != "2048"):
                yield(self.error_msg("Supported ethertypes: 2048 (ip)"))
                return
        if 'priority' in data:
            post_data["priorityCode"] = data['priority']
        if 'src-ip-address' in data:
            post_data["sourceIpAddress"] = data['src-ip-address']
        if 'dst-ip-address' in data:
            post_data["destinationIpAddress"] = data['dst-ip-address']
        if 'protocol' in data:
            post_data["ipv4Protocol"] = data['protocol']
        if 'tos' in data:
            #Uncomment the line below once we support tos in the REST API
            #post_data["tos"] = data['tos']
            pass
        if 'src-port' in data:
            post_data["ipL4SourcePort"] = data['src-port']
        if 'dst-port' in data:
            post_data["ipL4DestinationPort"] = data['dst-port']

        self.debug_msg("Post Data = %s" % post_data)

        url = "http://%s/rest/v1/realtimetest/network/explain-packet" % self.controller

        try:
            jresponse = self.store.rest_post_request(url, post_data)
        except Exception, e:
            errors = self.rest_error_to_dict(e, url)
            yield(self.error_msg(self.rest_error_dict_to_message(errors)))
            return

        response = json.loads(jresponse)
        self.debug_msg("Response = %s" % response)


        yield ("Input packet\n")
        yield ("------------\n")
        explPkt = response['explainPktParams']
        switch_alias   = alias_lookup_with_foreign_key("switch-alias", explPkt['srcSwitchDpid'])
        src_host_alias = alias_lookup_with_foreign_key("host-alias", explPkt['sourceMACAddress'])
        dst_host_alias = alias_lookup_with_foreign_key("host-alias", explPkt['destinationMACAddress'])

        if (src_host_alias):
            yield ("Source host        : %s (%s), ip=%s\n" % 
                   (explPkt['sourceMACAddress'], src_host_alias, explPkt['sourceIpAddress']))
        else:
            yield ("Source host        : %s, ip=%s\n" % (explPkt['sourceMACAddress'], explPkt['sourceIpAddress']))

        if (dst_host_alias):
            yield ("Destination host   : %s (%s), ip=%s\n" % 
                    (explPkt['destinationMACAddress'], dst_host_alias, explPkt['destinationIpAddress']))
        else:
            yield "Destination host   : %s, ip=%s\n" % (explPkt['destinationMACAddress'], explPkt['destinationIpAddress'])
        if (explPkt['vlanID'] == '-1'):
            yield "VLAN               : %s (Untagged)\n" % explPkt['vlanID']
        else:
            yield "VLAN               : %s\n" % explPkt['vlanID']        
        yield "802.1Q priority    : %s\n" % explPkt['priorityCode']
        yield "Ether type         : %s\n" % explPkt['etherType']
        if (switch_alias):
            yield "Source switch/port : %s (%s)/%s\n" % (explPkt['srcSwitchDpid'], switch_alias, explPkt['srcSwitchInPort'])
        else:
            yield "Source switch/port : %s/%s\n" % (explPkt['srcSwitchDpid'], explPkt['srcSwitchInPort'])
        yield "Protocol           : %s\n" % explPkt['ipv4Protocol']
        yield "L4 source port     : %s\n" % explPkt['ipL4SourcePort']
        yield "L4 destination port: %s\n" % explPkt['ipL4DestinationPort']                

        yield("\nForward path:")
        yield("=============\n")
        yield(self.test_pkt_in_result_display(response))

        # Now send the reverse test packet
        self.test_pkt_in_swap_src_dest_attributes(post_data)
        try:
            jresponse = self.store.rest_post_request(url, post_data)
        except Exception, e:
            errors = self.rest_error_to_dict(e, url)
            yield(self.error_msg(self.rest_error_dict_to_message(errors)))
            return

        response = json.loads(jresponse)
        self.debug_msg("Response = %s" % response)
        yield("\nReverse path:")
        yield("=============\n")
        yield(self.test_pkt_in_result_display(response))

    #
    # --------------------------------------------------------------------------------
    # 
    def implement_test_path(self, data):

        # compute source
        #
        if 'src-switch' in data:
            src_sw      = data['src-switch']
            src_sw_port = data['src-switch-port']
        elif 'src-host' in data:
            (result, src_sw, src_sw_port) = \
                self.get_host_attachment_switch_port({ 'mac' : data['src-host']})
            if not result:
                return
        elif 'src-ip' in data:
            # look up the ip in the device api
            (result, src_sw, src_sw_port) = \
                self.get_host_attachment_switch_port({ 'ipv4' : data['src-ip']})
            if not result:
                return
        else:
            return self.error_msg('No source attachment point in request')

        # compute dest
        if 'dst-switch' in data:
            dst_sw      = data['dst-switch']
            dst_sw_port = data['dst-switch-port']
        elif 'dst-host' in data:
            (result, dst_sw, dst_sw_port) = \
                self.get_host_attachment_switch_port({ 'mac' : data['dst-host']})
            if not result:
                return
        elif 'dst-ip' in data:
            # look up the ip in the device api
            (result, dst_sw, dst_sw_port) = \
                self.get_host_attachment_switch_port({ 'ipv4' : data['dst-ip']})
            if not result:
                return
        else:
            return self.error_msg('No dest attachment point in request')

        url = "http://%s/rest/v1/realtimetest/network/path" % self.controller
        request = {
                    'src-switch'      : src_sw,
                    'src-switch-port' : src_sw_port,
                    'dst-switch'      : dst_sw,
                    'dst-switch-port' : dst_sw_port,
                  }

        try:
            response = self.store.rest_post_request(url, request)
            self.debug_msg("Response = %s" % response)
            if response != '':
                command.query_result = json.loads(response)
            else:
                command.query_result = None

        except Exception, e:
            errors = self.rest_error_to_dict(e, url)
            return self.error_msg(self.rest_error_dict_to_message(errors))


    #
    # --------------------------------------------------------------------------------
    #
    def cp_help(self, words, text, completion_char):
        """
        Completion for the help command must be done using the collection
        of command descriptions; ie: help uses the command descriptions
        to complete the help commands.
        """
        if completion_char == ord('?'):
            if len(words) > 1:
                command.do_command_completion_help(words[1:], text)
            else:
                print self.help_splash([], text)
            return
        if len(words) == 1:
            items = self.commands_for_current_mode_starting_with(text)
            return utif.add_delim(items, ' ')
        else:   
            return command.do_command_completion(words[1:], text)


    #
    # --------------------------------------------------------------------------------
    # command_short_help
    #  Associate short help strings with known commands
    #  These ought to be associated with the COMMAND_DESCRIPTIONs,
    #  but since not all command use that mechanism yet, provide
    #  these short descriptions here.
    #
    command_short_help = {
        'access-list'    : 'Define access-list',
        'access-group'   : 'Configure access-list to interface association',
        'boot'           : "Configure boot configuration",
        'clock'          : 'Configure timezone or set clock',
        'clear'          : 'Reset counters',
        'connect'        : "Connect to a controller's REST API via ip[:port]",
        'date'           : 'Display current date and time',
        'debug'          : 'Enter various debug modes',
        'echo'           : 'Echo remaining arguments',
        'enable'         : 'Move to enable mode',
        'exit'           : 'Exit current mode',
        'feature'        : 'Enable or disable features',
        'firewall'       : 'Enable controller interfaces acls',
        'help'           : 'Help on commands or topics',
        'history'        : 'Display history of commands',
        'ip'             : 'Configure various controller node ip values',
        'logout'         : 'Exit from cli',
        'logging'        : 'Configure logging/syslog',
        'ntp'            : 'Configure ntp',
        'ping'           : 'Ping request from controller to switch or ip address',
        'show'           : 'Display configuration or settings',
        'test'           : 'Test various behaviors',
        'trace'          : 'Trace various streams',
        'traceroute'     : 'Traceroute from controller to switch or ip address',
        'configure'      : 'Enter configuration mode',
        'reload'         : 'Reboot controller, reload configuration',
        'write'          : 'Write configuraion',
        'no'             : 'Delete or disable configuration parameters',
        'vns-definition' : 'Enter vns-definiton submode, describe membership',
        'vns'            : 'Enter vns submode, manage access lists',
        'copy'           : 'Copy configurations',
        'switchto'       : 'Switch to another vns definition',
        'interface-rule' : 'VNS Membership rule',
        'end'            : 'End all nested configuration modes',
        'interface'      : 'Enter interface submode',
        'watch'          : 'Iterate indicated command displaying results',
    }

    #
    # --------------------------------------------------------------------------------
    # obj_type_short_help
    #
    obj_type_short_help = {
        'controller-node'           : 'Configure specific controller nodes',
        'host'                      : 'Configure host details',
        'statd-config'              : 'Statd Configuration',
        'statdropd-config'          : 'Stat dropd configuration',
        'statdropd-progress-info'   : 'Stat dropd progress configuration',
        'switch'                    : 'Switch Configuration',

        # -- debug only tables
        'vns-access-list'           : 'VNS Access List object',
        'vns-access-list-entry'     : 'VNS Access List Entry object',
        'vns-interface'             : 'VNS Interface object',
        'vns-interface-access-list' : 'VNS Interface Access List object',
        'vns-interface-rule'        : 'VNS Interface Rule object',
        'host-alias'                : 'Host Alias object',
        'port-alias'                : 'Port Alias object',
        'switch-alias'              : 'Switch Alias object',
    }

    #
    # --------------------------------------------------------------------------------
    # help_splash
    #
    def help_splash(self, words, text):
        ret = ""
        if not words:
            if text == "":
                ret += "For help on specific commands type help <topic>\n"

            count = 0
            longest_command = 0
            # this submode commands
            s_ret = ""
            mode = self.current_mode()
            nested_mode_commands = [self.title_of(x) for x in
                                    self.command_nested_dict.get(mode, [])]
            possible_commands = [self.title_of(x) for x in
                                 self.command_dict.get(mode, []) ] + \
                                nested_mode_commands
            available_commands = self.commands_feature_enabled(possible_commands)
            submode_commands = sorted(utif.unique_list_from_list(available_commands))
            if len(submode_commands):
                longest_command = len(max(submode_commands, key=len))
            for i in submode_commands:
                if not i.startswith(text):
                    continue
                count += 1
                short_help = command.get_command_short_help(i)
                if not short_help:
                    short_help = self.command_short_help.get(i, None)
                if short_help:
                    s_ret += "  %s%s%s\n" % (i,
                                          ' ' * (longest_command - len(i) + 1),
                                          short_help)
                else:
                    s_ret += "  %s\n" % i

            # commands
            c_ret = ""
            upper_commands = [x for x in self.commands_for_current_mode_starting_with()
                              if not x in submode_commands]
            commands = sorted(upper_commands)
            if len(commands):
                longest_command = max([len(x) for x in commands] + [longest_command])
            for i in commands:
                if not i.startswith(text):
                    continue
                count += 1
                short_help = command.get_command_short_help(i)
                if not short_help:
                    short_help = self.command_short_help.get(i, None)
                if short_help:
                    c_ret += "  %s%s%s\n" % (i,
                                          ' ' * (longest_command - len(i) + 1),
                                          short_help)
                else:
                    c_ret += "  %s\n" % i
                
            # objects
            o_ret = ""
            if self.in_config_mode():
                obj_types = sorted(self.obj_types_for_config_mode_starting_with())
                if len(obj_types) > 0:
                    for i in obj_types:
                        longest_command = max([len(x) for x in commands] +
                                              [longest_command])
                    for i in obj_types:
                        if i in commands:
                            continue
                        if i.startswith(text):
                            count += 1
                            short_help = self.obj_type_short_help.get(i, None)
                            if short_help:
                                o_ret += "  %s%s%s\n" % (i,
                                            ' ' * (longest_command - len(i) + 1),
                                            short_help)
                            else:
                                o_ret += "  %s\n" % i

            # fields
            f_ret = ""
            if self.in_config_submode():
                # try to get both the fields and the commands to line up
                longest_field = longest_command
                for i in self.fields_for_current_submode_starting_with():
                    if i.startswith(text):
                        longest_field = max(longest_field, len(i))
 
                f_count = 0
                # LOOK! could be getting the help text for each of these...
                for i in sorted(self.fields_for_current_submode_starting_with()):
                    if not i.startswith(text):
                        continue
                    count += 1
                    field_info = mi.obj_type_info_dict[
                                    self.get_current_mode_obj_type()]['fields'].\
                                        get(i, None)
                    if not field_info and self.debug:
                        print 'no field for %s:%s' % (self.get_current_mode_obj_type(), i)
                    if field_info and field_info.get('help_text', None):
                        f_ret += "  %s%s%s\n" % (i,
                                          ' ' * (longest_field - len(i) + 1),
                                          field_info.get('help_text'))
                    else:
                        f_ret += "  %s\n"% i

            if (text == "" or count > 1) and s_ret != "":
                ret += "Commands:\n"
                ret += s_ret

            if (text == "" or count > 1) and c_ret != "":
                ret += "All Available commands:\n"
                ret += c_ret

            if (text == "" or count > 1) and o_ret != "":
                ret += "\nAvailable config submodes:\n"
                ret += o_ret
            
            if (text == "" or count > 1) and f_ret != "":
                ret += "\nAvailable fields for %s:\n" % self.get_current_mode_obj_type() 
                ret += f_ret
        elif words[0] in ["show", "help", "copy", "watch" ]:
            method = self.command_method_from_name(words[0])
            if method:
                ret = method(None)
            else:
                #try:
                    #ret = command.get_command_syntax_help(words, 'Command syntax:')
                #except:
                    #if self.debug or self.debug_backtrace:
                        #traceback.print_exc()
                    #ret = "No help available for command %s" % words[0]

                try:
                    ret = command.get_command_documentation(words)
                except:
                    if self.debug or self.debug_backtrace:
                        traceback.print_exc()
                    ret = "No help available for command %s" % words[0]
        else:
            #try:
                #ret = command.get_command_syntax_help(words, 'Command syntax:')
            #except:
                #if self.debug or self.debug_backtrace:
                    #traceback.print_exc()
                #ret = "No help available for command %s" % words[0]
            try:
                ret = command.get_command_documentation(words)
            except:
                if self.debug or self.debug_backtrace:
                    traceback.print_exc()
                ret = "No help available for command %s" % words[0]
        return ret

    #
    # --------------------------------------------------------------------------------
    # do_help
    #
    def do_help(self, words):
        return self.help_splash(words, "")
        
    #
    # --------------------------------------------------------------------------------
    # do_lint
    #
    def do_lint(self, words):
        return command.lint_command(words)


    #
    # --------------------------------------------------------------------------------
    # cp_history
    #
    def cp_history(self, words, text, completion_char):
        if len(words) == 1:
            ret_val = "  <num>      - to display a specific number of commands (default:all)\n"
            ret_val += "  <cr>\n"
            self.print_completion_help(ret_val)
        else:
            self.print_completion_help("<cr>")

    #
    # --------------------------------------------------------------------------------
    # do_history
    #
    def do_history(self, words = None):
        ret_val = ""
        how_many = num_commands = readline.get_current_history_length()
        if words:
            how_many = words[0]
        for i in range(num_commands-int(how_many) + 1, num_commands):
            yield "%s: %s\n" % (i, readline.get_history_item(i))
        return


    debug_cmd_options = ["python", "bash", "cassandra-cli", "netconfig", "tcpdump",
                         "cli", "cli-backtrace", "cli-batch", "cli-interactive"]

    #
    # --------------------------------------------------------------------------------
    # do_debug
    #
    def implement_debug(self, words):
        if len(words) < 1 or len([x for x in self.debug_cmd_options if x.startswith(words[0])]) < 1:
            return "Syntax: debug < %s >" % " | ".join(self.debug_cmd_options)
        def shell(args):
            subprocess.call(["env", "SHELL=/bin/bash", "/bin/bash"] + list(args), cwd=os.environ.get("HOME"))
            print
        print "\n***** Warning: this is a debug command - use caution! *****"
        if "python".startswith(words[0]):
            print '***** Type "exit()" or Ctrl-D to return to the SDNOS CLI *****\n'
            shell(["-l", "-c", "python"])
        elif "bash".startswith(words[0]):
            print '***** Type "exit" or Ctrl-D to return to the SDNOS CLI *****\n'
            shell(["-l", "-i"])
        elif "cassandra-cli".startswith(words[0]):
            print '***** Type "exit" or Ctrl-D to return to the SDNOS CLI *****\n'
            shell(["-l", "-c", "/opt/sdnplatform/db/bin/cassandra-cli --host localhost"])
        elif "netconfig".startswith(words[0]):
            if not re.match("/dev/ttyS?[\d]+$", os.ttyname(0)):
                print '***** You seem to be connected via SSH or another remote protocol;'
                print '***** reconfiguring the network interface may disrupt the connection!'
            print '\n(Press Control-C now to leave the network configuration unchanged)\n'
            subprocess.call(["sudo", "env", "SHELL=/bin/bash", "/opt/sdnplatform/sys/bin/bscnetconfig", "eth0"], cwd=os.environ.get("HOME"))
        elif "tcpdump".startswith(words[0]):
            print '***** Type Ctrl-C to return to the SDNOS CLI *****\n'
            try:
                shell(["-l", "-c", "sudo /opt/openflow/sbin/tcpdump " + " ".join(words[1:])])
            except:
                pass # really, we need to ignore this unconditionally!
            time.sleep(0.2)
        elif "cli".startswith(words[0]):
            if self.debug:
                self.debug = False
                print "debug disabled"
            else:
                self.debug = True
                print "debug enabled"
        elif "cli-backtrace".startswith(words[0]):
            # This feature is separated from the cli debug mode so that backtraces
            # can be collected during cli qa
            self.debug_backtrace = True
            print '***** Enabled cli debug backtrace *****'
        elif "cli-batch".startswith(words[0]):
            self.batch = True
            print '***** Enabled cli batch mode *****'
        elif "cli-interactive".startswith(words[0]):
            self.batch = False
            print '***** Disabled cli batch mode *****'
        else:
            return self.error_msg("invoking debug")
    #
    # --------------------------------------------------------------------------------
    # do_echo
    #
    def do_echo(self, words):
        print " ".join(words)

    #
    # --------------------------------------------------------------------------------
    # cp_trace
    #
    trace_optional_params = {
        'screen': {'type': 'flag'},
#        'port': {'type': 'string',
#                     'syntax_help': "Enter <switch dpid> <physical port#>, where the packets traces are spanned."},
        'file': {'type': 'string',
                     'syntax_help': "Enter <filename> where the packets traces are captured."},
        }

    trace_cmds = ["detail", "oneline", "vns", "in", "out", "both", 
                      "echo_reply",  "echo_request", "features_rep", "flow_mod", "flow_removed", "get_config_rep", "hello", 
                      "packet_in", "packet_out", "port_status", "set_config", "stats_reply", "stats_reques"]

    def cp_trace(self, words, text, completion_char):
        vns_cache = objects_starting_with('vns-definition')
        if len(words) == 1:
            return [x for x in self.trace_cmds if x.startswith(text)]
        elif len(words) == 2 and words[1].lower().startswith("vns"):
            return vns_cache
        elif len(words) == 2 and words[1].lower().startswith("output"):
            return self.complete_optional_parameters(self.trace_optional_params,
                                                         words[len(words):], text)
        elif (len(words) == 3 and 
             words[1].lower().startswith("vns") and
             words[2].lower() in vns_cache):
            return [x for x in ["in", "out", "both", "output"] if x.startswith(text)] 
        elif (len(words) == 4 and 
             words[1].lower().startswith("vns") and
             words[2].lower() in objects_starting_with('vns-definition') and
             words[3].lower() in ["in", "out", "both"]):
            return [x for x in ["output"] if x.startswith(text)] 
        elif len(words) == 2 and words[1].lower()in ["in", "out", "both"]:
            return [x for x in ["output"] if x.startswith(text)] 
        elif (len(words) > 2 and 
             (words[1].lower().startswith("vns") or
              words[1].lower().startswith("output") or
              words[2].lower().startswith("output"))) :
            offset = len(words)
            if words[len(words)-1].lower() == 'screen':
                self.print_completion_help("<cr>")
                return []
            elif words[len(words)-1].lower() in ['file']:
                offset = len(words) - 1
            return self.complete_optional_parameters(self.trace_optional_params, words[offset:], text)

        self.print_completion_help("<cr>")

    #
    # --------------------------------------------------------------------------------
    # do_trace
    #
    def do_trace(self, *args):
        dpid = None
        if len(args) > 0:
            args = args[0]  # first element is the array of 'real' args
        if len(args) > 1 and args[0].lower() in ['vns', 'in', 'out', 'both', 'output']:
            try :
                return self.do_sdnplatform_trace(args)
            except KeyboardInterrupt, e:
                return

        if len(args) > 1 and args[0].lower() == 'trace':
            dpid = args[1]
            args = args[2:] # And in this case, args are after the first 2 parameters
        return self.do_tcpdump_trace(dpid, args)

    def do_sdnplatform_trace(self, args):
        vns = None
        direction = 'both'
        output = 'screen'
        dpid = None
        port = None
        file_name = None
        sessionId = None
        error = 'Error: Invalid command syntax.\n' + \
                'trace [vns <vnsId>] [in|out|both] output [screen]'
#                'trace [vns <vnsId>] [in|out|both] output [screen|port <dpid> <physical port>|file <filename>]'

        if args[0].lower() == 'vns':
            if len(args) > 1:
                vns = args[1]
                args = args[2:]
            else:
                return error

        while args:
            if args[0].lower() == 'output':
                args = args[1:]
                continue
            if args[0].lower() in ['in', 'out', 'both']:
                direction = args[0].lower() 
                args = args[1:]
                continue
            if args[0].lower() == 'screen':
                output = 'screen'
                break
            if args[0].lower() == 'port':
                output = 'port'
                if len(args) > 2:
                    dpid = args[1]
                    port = args[2:]
                    break
                else:
                    return error
            if args[0].lower() == 'file':
                output = 'file'
                if len(args) > 1:
                    file_name = args[1]
                    break
                else:
                    return error

        filter_request = {FT_DIRECTION:direction, FT_OUTPUT:output, FT_PERIOD:FT_PERIOD_DEFAULT}
        if vns :
            filter_request[FT_VNS] = vns
        if dpid and port :
            filter_request[FT_PORT] = {FT_PORT_DPID:dpid, FT_PORT_PORT:port}


        post_data = json.dumps(filter_request)

        while 1:
            response_text = None
            sessionId = None
            url = 'http://%s/rest/v1/packettrace/' % cli.controller
            request = urllib2.Request(url, post_data, {'Content-Type':'application/json'})
            try:
                response = urllib2.urlopen(request)
                response_text = response.read()
                response_text = json.loads(response_text)
            except Exception, e:
                return "Error: failed to get a response for a trace request. %s" % e

            if response_text and SESSIONID in response_text:
                sessionId = response_text[SESSIONID]
            else :
                return "Error: failed to get a trace session. %s" % response_text

            # Span to a physical port
            # No data is shown on cli. ^C to stop the trace
            if output.lower() == 'port':
                while 1:
                    try :
                        time.sleep(1000)
                    except KeyboardInterrupt, e:
                        self.terminateTrace(sessionId)
                        return

            lpurl = None

            if sessionId:
                lpurl = 'http://%s/poll/packets/%s' % (cli.controller, sessionId)
            else:
                return "Error: failed to start a trace session. %s" % response_text

            FILE = None
            if file_name:
                FILE = open(file_name, "w")

            flagTimeout = False
            while 1:
                try:
                    response_text = urllib2.urlopen(lpurl).read()
                    data = json.loads(response_text)
                    if output.lower() == "screen":
                        flagTimeout = self.dumpPktToScreen(data)
                    elif output.lower() == 'file' and FILE != None:
                        self.dumpPktToFile(FILE, data)
                except KeyboardInterrupt, e:
                    self. terminateTrace(sessionId)
                    return
                except Exception, e:
                    return "Error: failed to start a trace session. %s" % e

                if flagTimeout:
                    continueString = raw_input("Do you want to continue tracing? [y/n]:  ")
                    if 'y' in continueString.lower():
                        break
                    else:
                        return


    @staticmethod
    def dumpPktToScreen(data):
        for entry in data:
            if FT_TIMEOUT in entry:
                return True
            else:
                print entry
        return False

    @staticmethod
    def dumpPktToFile(FILE, data):
        FILE.writelines(data)

    @staticmethod
    def terminateTrace(sid):
        post_data = json.dumps({SESSIONID:sid, FT_PERIOD:-1})
        url = 'http://%s/rest/v1/packettrace/' % cli.controller
        request = urllib2.Request(url, post_data, {'Content-Type':'application/json'})
        try:
            response = urllib2.urlopen(request)
            response_text = response.read()
        except Exception, e:
            # Sdnplatform may not be running, but we don't want that to be a fatal
            # error, so we just ignore the exception in that case.
            pass

    def do_tcpdump_trace(self, dpid, args):
        ret = ''
        bsc_port = '6633' # LOOK: listen addr/port are wrong in the controller-node, so hard code 6633 for now
        trace_cmd = 'sudo /opt/sdnplatform/cli/bin/trace --filter \'%s\' %s'
        trace_rule = '((tcp) and (port %s))' % (bsc_port,)
        single_session = False

        trace_args = '--alias'
        if 'detail' in args:
            trace_args += ' --detail'
            args.remove('detail')
        if 'oneline' in args:
            trace_args += ' --oneline'
            args.remove('oneline')
        if 'single_session' in args:
            single_session = True
            args.remove('single_session')

        if dpid:
            query_dict = { 'dpid' : dpid }
            row = rest_to_model.get_model_from_url('switches', query_dict)
            if len(row) >= 1:
                if not 'ip-address' in row[0] or row[0].get('ip-address') == '':
                    return self.error_msg("switch %s currently not connected " % dpid)
            addr = row[0]['ip-address']
            port = row[0]['tcp-port']
            if single_session:
                trace_rule = '((tcp) and (port %s) and (host %s) and (port %s))' % (bsc_port, addr, port)
            else:
                trace_rule = '((tcp) and (port %s) and (host %s))' % (bsc_port, addr)
        if len(args) > 0:
            trace_args += (' ' + ' '.join(args))
        try:
            # print args, dpid, trace_cmd % (trace_rule, trace_args)
            process = subprocess.Popen(trace_cmd % (trace_rule, trace_args), shell=True)
            status = os.waitpid(process.pid, 0)[1]
            if status != 0:
                ret = 'Errno: %d' % (status, )
        except:
            pass # really, we need to ignore this unconditionally!

        time.sleep(0.2)
        return ret

    
    #
    # --------------------------------------------------------------------------------
    # get_auth_token_file_path
    #
    @staticmethod
    def get_auth_token_file_path():
        auth_token_path = os.environ.get('BSC_AUTH_TOKEN_PATH')
        if auth_token_path is None:
            auth_token_path = '/opt/sdnplatform'
        auth_token_path += '/auth_token'
        return auth_token_path
    
    #
    # --------------------------------------------------------------------------------
    # play_config_lines
    #
    def play_config_lines(self, src_text):
        """
        The complete collection of lines to replay are in src_text.
        In the future, this would provide for a way to first identify
        what needs to be removed, then apply what needs to be added.

        Some smart diffing from the exiting running-config would
        be needed to support that.   The stanza groups would need to
        be collected, the diff'd
        """
        print "Updating running-config ..."

        num_lines_played = 0
        # scan the lines looking for the version number.
        #
        version = None
        for line in src_text.splitlines():
            m = re.match("(\s*)(\w+)\s*", line) # only match non-blank lines
            if m:
                if m.group(0).startswith('version '):
                    line_parts = line.split()
                    if line_parts > 1:
                        version = line_parts[1]
                        break

        # construct the command to run.
        command = ['env']
        if version:
            command.append('CLI_COMMAND_VERSION=%s' % version)
        command.append('CLI_STARTING_MODE=config')
        command.append('CLI_SUPPRESS_WARNING=True')

        if os.path.exists('/opt/sdnplatform/cli/bin/cli'):
            # controller VM
            command.append('/opt/sdnplatform/cli/bin/cli')
            command.append('--init')
        else:
            # developer setup
            base = os.path.dirname(__file__)
            command.append(os.path.join(base, 'cli.py'))
            command.append('--init')

        # create a subprocess for the configuration, then push
        # each of the lines into that subprocess.
        p = subprocess.Popen(command,
                             stdin=subprocess.PIPE, 
                             stdout=subprocess.PIPE,
                             stderr=subprocess.STDOUT,
                             bufsize=1)
        postprocesslines=[]
        for line in src_text.splitlines(1):
            m = re.match("(\s*)(\w+)\s*", line) # only match non-blank lines
            if m:
                if 'tenant' or 'router' in line:  #special handling for routing rule commands VNS-226
                    postprocesslines.append(line)
                p.stdin.write(line)
                num_lines_played += 1
        for line in postprocesslines:
            p.stdin.write(line)
            num_lines_played += 1
        p.stdin.close()
        output = p.stdout.read()
        if output and (self.debug or self.debug_backtrace or self.description):  
            print output
        p.stdout.close()
        return "Num lines applied: %s\n" % num_lines_played

    #
    # --------------------------------------------------------------------------------
    # path_concat
    #  Concatenate a suffix path to a prefix, making the guarantee that the
    #  suffix will always be "under" the prefix. the suffix is normalized,
    #  then any prefix up-dirs ("..") are removed.
    #
    def path_concat(self, prefix, path):
        suffix = posixpath.normpath(path.lstrip('/').rstrip('/'))
        (head, tail) = os.path.split(suffix)
        tails = [tail]
        while head:
            (head, tail) = os.path.split(head)
            tails.insert(0, tail)
        drop = ''
        while tails[0] == '..':
            drop = os.path.join(drop, tails.pop(0))
        if drop != '':
            self.note("Ignoring %s prefix" % drop)
        for t in tails:
            prefix = os.path.join(prefix, t)
        return prefix

    #
    # --------------------------------------------------------------------------------
    # implement_copy
    #  startup-config and update-config are simply abbreviations for 
    #  config://startup-config and config://update_config
    #
    def implement_copy(self, words):
        if not words or len(words) > 3:
            print "Syntax: copy <src> <dst>"
            print "where <src> and <dst> can be"
            print "  running-config startup-config update-config"
            print "  config://<name-of-config-in-db> - A-Za-z0-9_-.@ "
            print "  <url> - starting with either tftp://, ftp://, http:// or file://"
            return
        if len(words) == 1:
            src = words[0]
            dst = "terminal"
        elif len(words) == 2:
            (src, dst) = (words[0], words[1])

        # Complete as required.
        # NOTE: No completion for upgrade-config as only our scripts should be using it
        if "running-config".startswith(src):
            src = "running-config"
        elif "startup-config".startswith(src):
            src = "startup-config"
        if "running-config".startswith(dst):
            dst = "running-config"
        elif "startup-config".startswith(dst):
            dst = "startup-config"
        elif "terminal".startswith(dst):
            dst = "terminal"

        if src == dst:
            print "src and dst for copy command cannot be equal"
            return

        # get from the source first
        src_text = ""

        # map any abbreviations
        abbreviations = {'startup-config': 'config://startup-config',
                         'upgrade-config': 'config://upgrade-config'
                        }
        src = abbreviations.get(src, src)
        data = None

        if src == "running-config": # get running
            src_text = run_config.implement_show_running_config([]) + "\n"
        elif src.startswith("config://"): # get local file
            m = re.search(self.local_name_pattern, src)
            if m:
                data = self.store.get_user_data_table(m.group(1), "latest")
                if data and len(data) > 0:
                    src_text = self.store.get_user_data_file(data[0]['full_name'])
            else:
                print self.error_msg("src name does not match %s" % self.local_name_pattern)
                return
        elif src.startswith("ftp") or src.startswith("http"): # get URL
            src_text = self.store.get_text_from_url(src)
        elif src.startswith("file://"):
            #src_file = self.saved_configs_dirname + posixpath.normpath(src[7:]).lstrip('/').rstrip('/')
            src_file = self.path_concat(self.saved_configs_dirname, src[7:])
            if src_file == "":
                return self.error_msg("src %s could not be " 
                                      "interpreted as a valid source for a file" % src)
            try:
                src_text = self.store.get_text_from_url("file://" + src_file)
            except Exception, e:
                return self.error_msg(src + ":" + str(e))
        else:
            return self.error_msg("Unknown configuration")

        message = ""
        if len(words) > 2:
            message = " ".join(words[2:]).strip('"')

        #
        # map any abbreviations
        dst = abbreviations.get(dst, dst)

        # now copy to dest
        if dst == "running-config":
            return self.play_config_lines(src_text)
        elif dst == "terminal":
            return src_text # returning src_text here allow '|' to work
        elif dst.startswith("config://"):
            m = re.search(self.local_name_pattern, dst)
            if m:
                store_result = self.store.set_user_data_file(m.group(1), src_text)
                if store_result:
                    result = json.loads(store_result)
                else:
                    return self.error_msg("rest store result not json format")
                if 'status' in result and result['status'] == 'success':
                    return None
                elif 'message' not in result:
                    return self.error_msg("rest store result doesn't contain error message")
                else:
                    return self.error_msg(result['message'])
            else:
                return self.error_msg("dst name does not match %s" % self.local_name_pattern)
        elif dst.startswith("ftp") or dst.startswith("http"):
            self.store.copy_text_to_url(dst, src_text, message)
        elif dst.startswith("file://"):
            dst_file = self.path_concat(self.saved_configs_dirname, dst[7:])
            dst_dir = '/'.join(dst_file.split('/')[:-1])
            if not os.path.exists(dst_dir):
                try:
                    os.makedirs(dst_dir)
                except Exception as e:
                    return self.error_msg("in creating destination directory: %s [%s]"
                                          % (str(e), dst_dir))
            if not os.path.isdir(dst_dir):
                return self.error_msg("destination directory is not a "
                                      "valid directory: %s" % dst_dir)
            with open(dst_file, 'w') as f:
                try:
                    f.write(src_text)
                except Exception as e:
                    return self.error_msg("in saving config file: %s [%s]" %
                                          (str(e), dst_file))
        elif dst == 'trash':
            if data:
                result = self.store.delete_user_data_file(data[0]['full_name'])
                if 'status' in result and result['status'] == 'success':
                    return None
                elif 'message' not in result:
                    return self.error_msg("rest store result doesn't contain error message")
                else:
                    return self.error_msg(result['message'])
        else:
            print self.error_msg("source file retrieved,"
                                 "but could not copy to destination %s" % dst)
        return

    #
    # --------------------------------------------------------------------------------
    # cp_conf_object_type
    #   Completion function for a named object, returns names of
    #   available obj_types
    #
    def cp_conf_object_type(self, words, text):
        if words[0] in mi.obj_types and len(words) == 1:
            return utif.add_delim(objects_starting_with(words[0], text), ' ')
        self.print_completion_help("<cr>")
        return []

    #
    # --------------------------------------------------------------------------------
    # cp_conf_field
    #  Handle if we've got a bunch of fields being concatenated,
    #  e.g., priority 42 src-port 80
    #
    def cp_conf_field(self, obj_type, words, text):
        if len(words) % 2 == 1: 
            latest_field_name = words[-1]
                    
            field_info = mi.obj_type_info_dict[obj_type]['fields'].get(latest_field_name, None)
            # if there was no shallow match of text, do deep match of xrefs if any
            if field_info is None:
                if obj_type in mi.alias_obj_type_xref:
                    if latest_field_name in mi.alias_obj_type_xref[obj_type]:
                        field_info = mi.obj_type_info_dict[latest_field_name]['fields']['id']

            if field_info:
                completion_text = "< %s >: " % field_info['verbose-name']
                completion_text += field_info.get('help_text', "")
                self.print_completion_help(completion_text)


            # return choices based on the types
            if mi.is_field_boolean(obj_type, latest_field_name):
                return utif.add_delib([x for x in ["True", "False"] if x.startswith(text)],
                                      ' ')

            # Use the 'Values:' prefix in the help_text as a way
            # of enumerating the different choices
            if field_info.get('help_text', "").startswith('Values:'):
                values = field_info.get('help_text').replace("Values:", "")
                return utif.add_delim([x for x in values.replace(" ","").split(',')
                                      if x.startswith(text)], ' ')

            #
            # special cases:
            if obj_type == 'vns-interface-rule' and latest_field_name == 'mac':
                return utif.add_delim(objects_starting_with('host', text), ' ')
            elif obj_type == 'vns-interface-rule' and latest_field_name == 'switch':
                return utif.add_delim(objects_starting_with('switch', text), ' ')
        else:
            # we've got an even number of field-value pairs, so just give the list
            # of fields for this obj_type
            return utif.add_fields(
                    self.fields_for_current_submode_starting_with(text),
                    ' ')

    #
    # --------------------------------------------------------------------------------
    # vns methods.
    #

    #
    # --------------------------------------------------------------------------------
    # vns_obj_type
    #  return True for an obj_type associated with vns, intended to identify
    #  tables which are or aren't available in debug mode 
    #
    @staticmethod
    def vns_obj_type(obj_type):
        if obj_type in [ 'vns-interface', 'vns-access-list-entry', 'vns-access-list',
                         'vns-interface-access-list', 'vns-interface-rule',
                         'vns-access-group', 'vns-interfaces', 'host-vns-interface']:
            return True
        return False

    #
    # --------------------------------------------------------------------------------
    # vns_debug_obj_type
    #  return True if the obj_type is available in the non-debug mode
    #
    def vns_debug_obj_type(self, obj_type):
        if self.debug:
            return True
        if self.in_config_submode("config-vns-") and obj_type == 'vns-access-list-entry':
            return False
        return not self.vns_obj_type(obj_type)

    #
    # --------------------------------------------------------------------------------
    # vns_debug_show_commands
    #  return True if the show command vns option is availabe in the current debug mode
    #
    def vns_debug_show_commands(self, show_command):
        if self.debug:
            return True
        if self.in_config_submode() and show_command == 'do_show_vns_interface_rule':
            return True
        if self.in_config_vns_if_mode() and show_command == 'do_show_vns_access_group':
            return True
        if show_command == 'do_show_vns':
            return True
        return not self.vns_obj_type(show_command.replace('_', '-').replace('do-show-', '', 1))

    #
    # --------------------------------------------------------------------------------
    # vns_find_name
    #  Return True when found, False otherwise
    #
    def vns_find_name(self, name):
        try:
            self.get_object_from_store("vns-definition", name)
            return True
        except Exception, e:
            pass
        return False

    #
    # --------------------------------------------------------------------------------
    # vns_find_and_set_name
    #  Return True when the name is known, as a side effect, set self.vns_name
    #  Invalid names are managed by the model, see VnsNameValidator
    #
    def vns_find_and_set_name(self, words):
        if len(words) == 0:
            print self.error_msg("vns <name> required")
            return False
        elif len(words) > 1:
            print self.error_msg("Additional text after vns name ignored")
        name = words[0]
        if self.vns_find_name(name):
            return True
        return False
        
    #
    # --------------------------------------------------------------------------------
    # vns_find_and_set_existing_name
    #  Return True when the name is known, as a side effect, set self.vns_name
    #  And display an error message concerning the failure.
    #
    def vns_find_and_set_existing_name(self, words):
        if self.vns_find_and_set_name(words):
            return True
        if len(words) > 0:
            print self.error_msg("Not Found: vns '%s'" % words[0])
        return False
        
    #
    # --------------------------------------------------------------------------------
    # vns_find_or_add_name
    #  Return True when either the vns name currently exists, or of a create
    #  for the vns name succeeded.
    #
    def vns_find_or_add_name(self, words):
        if self.vns_find_and_set_name(words):
            return True
        name = words[0]
        errors = None
        ident = {mi.pk("vns-definition"): name }
        try:
            self.rest_create_object("vns-definition", ident)
        except Exception, e:
            errors = self.rest_error_to_dict(e, 'vns-definition')
            print self.rest_error_dict_to_message(errors)
            return False

        return True
   

    #
    # --------------------------------------------------------------------------------
    # vns_all_ids
    #
    def vns_all_ids(self):
        return [x['id'] for x in self.get_table_from_store("vns-definition")]

    #
    # --------------------------------------------------------------------------------
    # vns_convert_table_id
    #  return a list of two entries, the first is the vns name, the second is the
    #  updated column value
    #
    def vns_convert_table_id(self, obj_type, row, id):
        fields = row[id].split('|')
        if len(fields) == 1:
            # Just in case there's an error in the table
            return [fields[0], "<error>"]
        if len(fields) == 2:
            return fields
        elif len(fields) == 3:
            if obj_type == 'host-vns-interface':
                return [fields[1], fields[2]]
            elif obj_type == 'vns-access-list-entry':
                row['vns-access-list'] = fields[1]
                return [fields[0], fields[2]]
            else:
                print self.error_msg("3: vns_convert_table_id case %s" % obj_type)
        elif len(fields) == 4:
            if obj_type == 'vns-interface-access-list':
                # XXX ought to validate
                if row['vns-access-list'].split('|')[1] != fields[2]:
                    print self.error_msg('vns-interface-access-list inconsistent %s and id %d' % \
                                         (row['vns-access-list'], row[id]))
                row['vns-access-list'] = fields[2]
                row['vns-interface'] = fields[1]
                return [fields[0], fields[2]]
            else:
                print self.error_msg("4: vns_convert_table_id case %s" % obj_type)
        else:
            print self.error_msg("vns_convert_table_id: length %d not managed" % len(fields))
    #
    # --------------------------------------------------------------------------------
    # vns_table_id_to_vns_column
    #
    #  vns related tables in the model have unique id's created by concatenating the
    #  vns name with other fields.  For those tables, separate the vns id's from the
    #  associated fields, and transform the vns association into its own column.
    #
    #  obj_type is the name of the table.
    #
    #  for the vns_interface, the vns name is extracted from several fields,
    #  validated, updated to exclude the vns name, then a new column is added
    #
    def vns_table_id_to_vns_column(self, obj_type, entries):
        id = mi.pk(obj_type)
        for row in entries:
            if row[id]:
                type_info = mi.obj_type_info_dict[obj_type]['fields'][id]['type']
                if type_info == 'compound-key':
                    mi.split_compound_into_dict(obj_type, id, row)
                else:
                    convert_id = self.vns_convert_table_id(obj_type, row, id)
                    row['vns'] = convert_id[0]
                    row[id] = convert_id[1]

    #
    # --------------------------------------------------------------------------------
    # vns_foreign_key_to_base_name
    #  For foreign key's which vns wants to 'show', convert the
    #  foriegn key to the last entry for the field, and display
    #  that entry
    #
    def vns_foreign_key_to_base_name(self, obj_type, entries):
        foreign_keys = mi.obj_type_foreign_keys(obj_type)
        for row in entries:
            # convert the rule id into the rule suffix
            for foreign_key in foreign_keys:
                if foreign_key in row and row[foreign_key].find('|'):
                    fields = row[foreign_key].split('|')
                    row[foreign_key] = fields[-1]

    #
    # --------------------------------------------------------------------------------
    # vns_join_host_fields
    #  For vns-interfaces new fields are joined from the host table.
    #
    def vns_join_host_fields(self, obj_type, entries):
        if obj_type == 'vns-interface':
            #
            host_dap_dict = self.create_attachment_dict()
            host_ip_dict = self.create_ip_dict()
            #
            vns_interface_key = mi.pk('vns-interface')
            #
            # for vns-interface, currently vlan is shown in the output,
            # and this must be joined from the host's table.  Again, it
            # is better to collect the complee host table, then create
            # a host dictionary

            hosts_rows_dict = create_obj_type_dict('host', 'mac')

            #
            # re-index host_rows_dict based not on mac, but on attachment point,
            #  which is the sum of the switch+port.  the objective here is to
            #  identify the number of items on a specific-switch port.
            #
            ap_dict = {} # indexed by dpid|port
            for (mac, values) in host_dap_dict.items():
                for value in values:
                    key = "%s|%s" % (value['switch'], value['ingress-port'])
                    if not key in ap_dict:
                        ap_dict[key] = 0
                    ap_dict[key] += 1
            #
            # the ap_dict counts the number of mac's associated with an ap.
            # sort the host_rows_dict based on the lowest ap in each group.
            #
            for (mac, value) in host_dap_dict.items():
                new_value = sorted(value,
                                   key=lambda k:ap_dict["%s|%s" % (
                                                        k['switch'],
                                                        k['ingress-port'])],
                                   cmp=lambda x,y: int(x) - int(y))
                # add a tag to the dictionary of the first list item
                # when the ap_dict's entry's count is 1.
                first = new_value[0]
                if ap_dict['%s|%s' % (first['switch'], first['ingress-port'])] == 1:
                    new_value[0]['prime'] = True
                host_dap_dict[mac] = new_value
                    
            for row in entries:
                fields = row['id'].split('/')
                if not 'rule' in row:
                    row['rule'] = fields[0]
                if len(fields) != 2:
                    continue # silently ignore any id's not containing hosts
                host = fields[1]

                row['mac'] = host
                host_dict = {'mac' : host}
                row['attachment-points'] = self.get_attachment_points(
                                                   host_dict,
                                                   host_dap_dict)
                row['ips'] = self.get_ip_addresses(host_dict, host_ip_dict)

                host_row = hosts_rows_dict.get(host, None)
                if host_row and len(host_row) == 1 and 'vlan' in host_row[0]:
                    row['vlan'] = host_row[0]['vlan']
  
    #
    # --------------------------------------------------------------------------------
    # vns_join_switch_fields
    #  For vns-interfaces with entries arising from switch interface rules,
    #  join in the switch and port.
    #
    def vns_join_switch_fields(self, vns_name, entries):
        #
        # store all the rules associated with a specific vns,
        # index them by the primary key.
        key = mi.pk('vns-interface-rule')
        if vns_name == None:
            # vns_name == None, this means 'all'
            vns_ifr_dict = create_obj_type_dict('vns-interface-rule',
                                                key)
        else:
            search_key = self.unique_key_from_non_unique([vns_name])
            vns_ifr_dict = create_obj_type_dict('vns-interface-rule',
                                                key,
                                                key,
                                                search_key)
        for entry in entries:
            if 'rule' in entry:
                rule = entry['rule']
                if rule in vns_ifr_dict:
                    entry['switch'] = vns_ifr_dict[rule][0].get('switch', "")
                    entry['ports'] = vns_ifr_dict[rule][0].get('ports', "")

    #
    # end of vns methods.
    # --------------------------------------------------------------------------------
    #

    #
    #
    # --------------------------------------------------------------------------------
    # debug_obj_type
    #  Return True when the object is available, in self.debug mode, more objects
    #  are available.
    #
    def debug_obj_type(self, obj_type):
        if self.debug:
            return True
        if mi.is_obj_type_source_debug_only(obj_type):
            return False
        if self.vns_debug_obj_type(obj_type):
            if not obj_type in mi.alias_obj_types:
                return True
        return False

    #
    # generic parsing routines
    # 

    #
    # --------------------------------------------------------------------------------
    # completion_reset
    #
    def completion_reset(self):
        self.last_line = None
        self.last_options = None
        self.completion_cache = True
        self.last_completion_char = readline.get_completion_type()


    #
    # --------------------------------------------------------------------------------
    # completer
    #  This is the main function that is called in order to complete user input
    #
    def completer(self, text, state):
        question_mark = ord('?')
        if readline.get_completion_type() == question_mark:
            if len(readline.get_line_buffer()) == 0:
                #
                # manage printing of help text during command completion
                help_text = self.help_splash(None, text)
                if help_text != "":
                    self.print_completion_help(help_text)
                    return
        
        try:
            origline = readline.get_line_buffer()
            # See if we have a cached reply already
            if (self.completion_cache and origline == self.last_line and 
                self.last_completion_char == readline.get_completion_type() and
                self.last_options):

                if state < len(self.last_options):
                    return self.last_options[state]
                else:
                    # apparently, for the linux VM choice don't print 
                    if self.last_options and \
                      len(self.last_options) > 1 and \
                      self.last_completion_char == ord('\t'):
                        choices_text = self.choices_text_builder(self.last_options)
                        self.print_completion_help(choices_text)

                    if self.completion_skip:
                        self.completion_cache = False
                        self.completion_skip = False
                    return None

            self.completion_reset()

            # parse what user has typed so far

            begin = readline.get_begidx()
            end = readline.get_endidx()

            # Find which command we're in for a semicolon-separated list of single commands
            # LOOK! This doesn't handle cases where an earlier command in the line changed
            # the mode so the completion for later commands in the line should be different.
            # For example, if you typed "enable; conf" it won't detect that it should be
            # able to complete "conf" to "configure" because the enable command has not been
            # executed yet, so you're not in enable mode yet. Handling that case would be
            # non-trivial I think, at least with the current CLI framework.
            command_begin = 0
            command_end = 0
            while True:
                command_end = self.find_with_quoting(origline, ';', start_index=command_begin)
                if command_end < 0:
                    command_end = len(origline)
                    break
                if begin >= command_begin and end <= command_end:
                    break
                command_begin = command_end + 1
            
            # Skip past any leading whitespace in the command
            while command_begin < begin and origline[command_begin].isspace():
                command_begin += 1
            
            words = origline[command_begin:end].split()
            
            # remove last term if it is the one being matched
            if begin != end:
                words.pop()

            # LOOK! there are at least three places that try to parse the valid options:
            # 1. When actually handling a command
            # 2. When trying to show completions (here)
            # 3. When displaying help

            # complete the first word in a command line            
            if not words or begin == command_begin:
                options = self.commands_for_current_mode_starting_with(text, completion = True)
                if self.in_config_mode():
                    for item in self.obj_types_for_config_mode_starting_with(text):
                        self.append_when_missing(options, item)
                if self.in_config_submode():
                    for item in self.fields_for_current_submode_starting_with(text):
                        self.append_when_missing(options, item)
                options = [x if x.endswith(' ') else x + ' '  for x in sorted(options)]
            # Complete the 2nd word or later
            else:
                commands = self.commands_for_current_mode_starting_with(words[0])
                obj_types = self.obj_types_for_config_mode_starting_with(words[0]) if self.in_config_mode() else []
                fields = self.fields_for_current_submode_starting_with(words[0]) if self.in_config_submode() else []
                if len(commands) + len(obj_types) + len(fields) > 1:
                    if len(fields) > 1:
                        fields = [words[0]] if words[0] in fields else []
                    if len(commands) > 1:
                        commands = [words[0]] if words[0] in commands else []
                    if len(obj_types) > 1:
                        obj_types = [words[0]] if words[0] in obj_types else []
                
                if len(fields) == 1:
                    options = self.cp_conf_field(self.get_current_mode_obj_type(), words, text)
                elif len(obj_types) == 1:
                    options = self.cp_conf_object_type(obj_types + words[1:], text)
                elif len(commands) == 1: 
                    try:
                        # options[0] is expanded while words[0] is not
                        method = self.completion_method_from_name(commands[0]) 
                        if method:
                            options = method(words, text, readline.get_completion_type())
                            if not options:
                                # no match
                                return None
                        else:
                            if readline.get_completion_type() == question_mark:
                                options = command.do_command_completion_help(words, text)
                            else:
                                options = command.do_command_completion(words, text)
                            #else:
                                #if options:
                                    #print syntax_help
                                #else:
                                    #pass
                                    #self.print_completion_help(syntax_help)
                            
                    except AttributeError:
                        if self.debug or self.debug_backtrace:
                            traceback.print_exc()
                        return None

                else:
                    options = None
                    
        except Exception, e:
            if self.debug or self.debug_backtrace:
                traceback.print_exc()
            # errors in connect are caught silently, complain here
            # TODO - Maybe we should log this in a file we can review
            # at a later date?
        
        try:
            if options:
                self.last_line = origline
                self.last_options = options
                self.last_completion_char = readline.get_completion_type()
                return options[state]
        except IndexError:
            return None

        return None

    #
    # --------------------------------------------------------------------------------
    # handle_alias
    #  Manage alias creation for an obj_type, only manage one alias for each of the
    #  targets, although that requirement exists so that a single alias
    #  can be displayed for output from (show) commands
    #
    #  Note that setting the alias to its current value currently causes the
    #  alias to be created (ie: there's no specific search to find the item)
    #
    def handle_alias(self, obj_type, alias_obj_type, alias_value):

        if alias_value in self.reserved_words:
            return self.error_msg("alias value %s is a reserved word (%s)" % 
                                  (alias_value, ', '.join(self.reserved_words)))
        #
        # allow the use of 'alias' when only one alias entry
        # exists for the obj_type.  only-one ought to be typical.
        # this may need to be removed after a compatability period.
        if alias_obj_type == 'alias':
            # there should be only one alias table for this obj_type
            aliases = mi.alias_obj_type_xref[obj_type]
            if len(aliases) != 1:
                print self.error_msg("Internal more than one alias choice")
            alias_obj_type = aliases[0]

        obj = self.get_current_mode_obj()
        alias_key = mi.pk(alias_obj_type)

        #
        # create row for table with foreign key
        # find the name of the field which is the foreign key...
        foreign_field =  mi.alias_obj_type_field(alias_obj_type)
        if not foreign_field:
            print self.error_msg("internal handle_alias: alias_obj_type_field")
            return None

        #
        # delete the current alias if it exists
        try:
            exists = self.get_object_from_store(alias_obj_type, alias_value)
            if len(exists):
                self.rest_delete_object(alias_obj_type, alias_value)
        except:
            pass

        try:
            create_dict = { alias_key : alias_value, foreign_field : obj }
            self.rest_create_object(alias_obj_type, create_dict)
            errors = None
        except Exception, e:
            errors = self.rest_error_to_dict(e, alias_obj_type)

        if errors:
            return self.error_msg("could not create %s for %s (%s): %s" %
                                  (alias_obj_type, alias_value, obj_type,
                                 self.rest_error_dict_to_message(errors)))
        #
        # remove other existing alias for the same foreign key
        # find any other alias for this config object, then remove them
        #
        try:
            rows = self.get_table_from_store(alias_obj_type,
                                             foreign_field,
                                             obj,
                                             "exact")
        except Exception, e:
            errors = self.rest_error_to_dict(e, alias_obj_type)
            print self.rest_error_dict_to_message(errors)
            rows = []
        #
        #
        for row in rows:
            #
            # skip the entry which was just inserted
            if row[alias_key] == alias_value and row[foreign_field] == obj:
                continue
            try:
                self.rest_delete_object(alias_obj_type, row[alias_key])
                self.warning("removed other alias '%s' for %s '%s'" %
                             (row[alias_key], foreign_field, row[foreign_field]))
            except:
                pass

        return None
        

    #
    # --------------------------------------------------------------------------------
    # update_foreign_key
    #  Given an obj_type, its primary key name, and the old and new
    #  id values, find any associated tables which have foreign keys
    #  associated with this table, and update the foreign key so that
    #  the table again points to a valid id.
    #
    #  its unlikely, although not impossible, for the foreign key
    #  update to need to cascade.
    #
    def update_foreign_key(self, obj_type, obj_key, old_id, new_id):
        if obj_type in mi.foreign_key_xref and \
           obj_key in mi.foreign_key_xref[obj_type]:
            for (fk_obj_type, fk_name) in mi.foreign_key_xref[obj_type][obj_key]:
                #
                # find any affected rows
                try:
                    rows = self.get_table_from_store(fk_obj_type,
                                                     fk_name,
                                                     old_id,
                                                     "exact")
                except:
                    rows = []

                key = mi.pk(fk_obj_type)
                for row in rows:
                    self.warning("updating %s key %s field %s to %s" % \
                                (fk_obj_type, row[key], fk_name, new_id))
                    try:
                        self.rest_update_object(fk_obj_type,
                                                key,
                                                row[key],
                                                { fk_name : new_id })
                        errors = None
                    except Exception, e:
                        errors = self.rest_error_to_dict(e, fk_obj_type)

                    if errors:
                        print self.rest_error_dict_to_message(errors)

    #
    # --------------------------------------------------------------------------------
    # handle_field
    #
    # handle_field calls itself recursively to parse fields and build
    # a field dict. It then passes the field dict to one rest call.
    #
    def handle_field(self, words, accumulated_data=None):
        if accumulated_data is None:
            accumulated_data = {}
        if len(words) < 2:
            return self.error_msg("incorrect number of args (must be <field> <value>)")
        obj_type = self.get_current_mode_obj_type()
        obj = self.get_current_mode_obj()
        last_word = 2
        field = words[0]
        value = words[1]
        
        # complete the field if needed
        field_choices = self.fields_for_current_submode_starting_with(words[0])
        # LOOK!: robv: Fix to work with a field names which is a prefix of another
        if len(field_choices) > 1:
            for field_choice in field_choices:
                if field_choice == words[0]:
                    field_choices = [field_choice]
                    break
        if len(field_choices) > 1:
            return "Multiple matches for field name %s." % field
        if len(field_choices) == 0:
            return self.error_msg("%s has no field named %s." % (obj_type, field))
        field = field_choices[0]

        #
        # handle special case alias's which aren't understood via xref
        # (this is because vns-interface-rule doesn't foreignKey neigther
        #  switch or mac references to the switch/host tables)
        #  
        if obj_type == 'vns-interface-rule' and field == 'switch':
            if not self.DPID_RE.match(value):
                alias_value = alias_lookup('switch-alias', value)
                if alias_value:
                    value = alias_value
                else:
                    return "Syntax: Unknown switch alias '%s'; Specify switch as DPID or alias" % value
        if obj_type == 'vns-interface-rule' and field == 'mac':
            if not self.MAC_RE.match(value):
                alias_value = alias_lookup('host-alias', value)
                if alias_value:
                    value = alias_value
                else:
                    return "Syntax: Unknown host alias '%s'; Specify host as MAC or alias" % value
                
        #
        # Replace common type values with expected types, for example true or false
        #  for True/False. 
        if mi.is_field_boolean(self.get_current_mode_obj_type(), field):
            if value.lower() == 'false':
                value = 'False'
            elif value.lower() == 'true':
                value = 'True'
        # 
        elif mi.is_hex_allowed(obj_type, field) and self.HEX_RE.match(value):
            value = str(int(value, 16))
        #
        # Look up any validation or value changing callouts...
        validate = mi.field_validation(self.get_current_mode_obj_type(), field)
        if validate:
            validate_error = validate(obj_type, field, value)
            if validate_error:
                return validate_error

        accumulated_data[field] = value

        ret_val = None

        if len(words) > last_word:
            # more to munch
            ret_val = self.handle_field(words[last_word:], accumulated_data)
        else:
            # munched everything, now update
            obj_key = mi.pk(obj_type)

            prepare_update = mi.obj_type_prepare_row_update(obj_type)
            if prepare_update:
                accumulated_data = prepare_update(obj_type, obj_key, obj,
                                                  accumulated_data)
                # in case the obj_type's key is updated
                obj = accumulated_data[obj_key]

            #
            # Note: if all the fields for a row are all manually reverted
            # to a default value, perhaps the row ought to be deleted.
            errors = None
            try:
                self.rest_update_object(obj_type, obj_key, obj, accumulated_data)
            except Exception, e:
                errors = self.rest_error_to_dict(e, obj_type)

            if errors:
                ret_val = self.rest_error_dict_to_message(errors)
            else:
                #
                # if the primary key was part of the update. find relatred
                # tables and update the foreign key.  XXX perhaps this is
                # the rest api's problem?
                #
                if obj_key in accumulated_data and \
                   accumulated_data[obj_key] != obj:

                    self.update_foreign_key(obj_type, obj_key, obj,
                                            accumulated_data[obj_key])
                    #
                    # delete the old key.
                    self.rest_delete_object(obj_type, obj)

                    #
                    # move the the current id
                    self.set_current_mode_obj(accumulated_data[obj_key])

        return ret_val

    #
    # --------------------------------------------------------------------------------
    # convert_hex_to_decimal
    #  Review the contents of all the values, looking for hex
    #  valued fields.  When a hex value is identified, change
    #  it to decimal when 'is_hex_allowed' is true.
    #
    def convert_hex_to_decimal(self, obj_type, field_dict):
        for key in field_dict.keys():
            if mi.is_hex_allowed(obj_type, key) and \
               self.HEX_RE.match(field_dict[key]):
                field_dict[key] = int(field_dict[key], 16)

    #
    # --------------------------------------------------------------------------------
    # handle_obj_type
    #  In config mode, this handles if first word is an object-type
    #  (ex: "flow-entry catch-web")
    #
    def handle_obj_type(self, words):
        if len(words) < 2:
            return "Syntax: %s <key>: where key is an id of %s" % (words[0], words[0])

        (obj_type, obj_name) = (words[0], words[1])

        obj_name = convert_alias_to_object_key(obj_type, obj_name)
        # First check if this is something we can actually configure
        if obj_type not in mi.obj_types:
            return self.error_msg("unknown command or object.")

        # Next, find the object
        #  Deal with any changes to the lookup name based on the 'concatenation'
        #  of the config mode name to the named identifer.
        #
        found_obj = self.get_obj_of_type(obj_type, obj_name) 

        if found_obj == None:
            # need to create object, obj_data is the rest api dictionary used
            # to create the new row.
            obj_data = { mi.pk(obj_type): obj_name }

            if self.in_config_submode():
                # add in all the key value from modes in the stack
                # LOOK! this is brittle as it depends on the name of 
                # the mode matching the name of the attribute.
                #
                obj_data = self.mode_stack_to_rest_dict(obj_data)

                # 
                self.convert_hex_to_decimal(obj_type, obj_data)

                # make sure all the foreign keys are populated
                #
                for key in mi.obj_type_foreign_keys(obj_type):
                    if not key in obj_data:
                        print self.error_msg("foreign key '%s' missing from mode stack %s" %
                                             (key, obj_data))

            prepare_update = mi.obj_type_prepare_row_update(obj_type)
            if prepare_update:
                obj_data = prepare_update(obj_type,
                                          mi.pk(obj_type), obj_name,
                                          obj_data)

            # create the object
            errors = None
            try:
                self.rest_create_object(obj_type, obj_data)
            except Exception, e:
                errors = self.rest_error_to_dict(e, obj_type)

            if errors:
                return self.rest_error_dict_to_message(errors)

            # get it back, populating found_obj
            found_obj = self.get_obj_of_type(obj_type, obj_name) 
            
        # push on the submode for the object 
        # LOOK! assumes all object commands drop you into a submode...
        if found_obj:
            sub_mode = "config-%s" % obj_type
            self.push_mode(sub_mode, obj_type, obj_name)

            # hand off the rest of the line to handle_field
            if len(words) > 2:
                return self.handle_field(words[2:])
        else:
            return self.error_msg("No %s with %s = %s" %
                                  (obj_type, mi.pk(obj_type), obj_name))

    #
    # --------------------------------------------------------------------------------
    # print_completion_help
    #
    def print_completion_help(self, completion_help_text):
        origline = readline.get_line_buffer()
        end = readline.get_endidx()
        cur_command = origline[0:end]

        help_text = "\n%s\n%s%s" % ( completion_help_text, 
                                    self.prompt, 
                                    cur_command)
        self.completion_skip = True
        sys.stdout.write(help_text)
        
    #
    # --------------------------------------------------------------------------------
    # get_ip_from_switch_dpid_or_alias
    #    Returns the IP associated with the switch if it exists
    #    or the original string if it's not a dpid or alias.
    #
    def get_ip_from_switch_dpid_or_alias(self, alias):
        dpid = convert_alias_to_object_key("switches", alias)
        if self.DPID_RE.match(dpid):
            query_dict = { 'dpid' : dpid }
            row = rest_to_model.get_model_from_url('switches', query_dict)
            if len(row) >= 1:
                if not 'ip-address' in row[0] or row[0].get('ip-address') == '':
                    return self.error_msg("switch %s currently not connected "
                                          % alias)
                return row[0].get('ip-address')
        return alias

    #
    # --------------------------------------------------------------------------------
    # implement_write
    #
    def implement_write(self, words):
        if len(words) == 1:
            if "memory".startswith(words[0]):
                return self.implement_copy(["running-config", "startup-config"])
            elif "erase".startswith(words[0]):
                print "This will clear the startup-config and set it to be empty."
                resp = raw_input("Are you sure that want to proceed? [n]")
                if resp and "yes".startswith(resp.lower()):
                    print "Erasing startup config ..."
                    result = self.store.delete_user_data_file("startup-config/time/len/version")
                    if 'status' in result and result['status'] == 'success':
                        return None
                    elif 'message' not in result:
                        return self.error_msg("rest store result doesn't contain error message")
                    else:
                        return self.error_msg(result['message'])
                else:
                    print "Command aborted by user: write erase"
                return
            elif "terminal".startswith(words[0]):
                return self.implement_copy(["running-config", "terminal"])
        return "Syntax: write < terminal | memory | erase>"

    #
    # --------------------------------------------------------------------------------
    # 
    
    @staticmethod
    def set_clock_action(data):
        time_values = data['time'].split(':')
        if len(time_values) != 3:
            raise error.CommandError('Invalid time; must be HH:MM:SS')
        hour = int(time_values[0])
        minute = int(time_values[1])
        second = int(time_values[2])
        
        MONTH_NAMES =  ('January', 'February', 'March', 'April', 'May', 'June',
                        'July', 'August', 'September', 'October', 'November', 'December')

        day_of_month = int(data['day-of-month'])
        month_name = data['month']
        if month_name not in MONTH_NAMES:
            raise error.CommandError('Invalid month name (e.g. January, May, July)')
        month = MONTH_NAMES.index(month_name) + 1
        year = int(data['year'])
        
        date_time_info = {
            'year': year,
            'month': month,
            'day': day_of_month,
            'hour': hour,
            'minute': minute,
            'second': second
        }
        url = 'http://%s/rest/v1/system/clock/local' % cli.controller
        result = cli.store.rest_post_request(url, date_time_info)
        date_time_info = json.loads(result)
        clock_string = SDNSh.get_clock_string(date_time_info, False)
        return clock_string
    
    #
    # --------------------------------------------------------------------------------
    # get_misconfigured_default_gateway
    #  
    @staticmethod
    def get_misconfigured_default_gateway():
        """
        Determine if the controller is configured with a default gateway
        setting that doesn't match (i.e. is not on the same subnet)
        any of the interfaces configured with static IP addresses.
        This is used by the begin_default_gateway_check_action and
        end_default_gateway_check_action actions that check if a command
        results in a misconfigured default gateway setting and, if so,
        emit an error message.
        If the default gateway is misconfigured, the return value is the
        default gateway value. Otherwise, the return value is None.
        """
        # First figure out which controller-node we're working with.
        # This is a little kludgy right now. We check the mode stack
        # looking for a controller node element.
        controller_id = cli.get_nested_mode_obj('controller-node')
        if not controller_id:
            raise error.CommandDescriptionError('check_default_gateway_action must be called from a (possibly nested) controller node config mode')
        
        controller = cli.get_object_from_store('controller-node', controller_id)
        if not controller:
            # This shouldn't really happen unless someone/thing has mucked with the
            # controller object out from under this instance of the CLI
            # but just to be safe...
            raise error.CommandInvocationError('Current controller settings have been deleted.')
        
        # Check if the controller node is configured with a default gateway.
        # If not, then there's no possible misconfiguration and we're done.
        default_gateway = controller.get('default-gateway')
        if default_gateway == '':
            return
        
        # There is a default gateway configured, so we need to check if there's
        # an interface that matches (i.e. is on the same subnet as) the default
        # gateway
        interfaces = cli.rest_query_objects('controller-interface', {'controller': controller_id})
        for interface in interfaces:
            mode = interface.get('mode')
            if mode == 'static':
                ip = interface.get('ip')
                netmask = interface.get('netmask')
                if (ip != '') and (netmask != '') and same_subnet(default_gateway, ip, netmask):
                    return None

        return default_gateway
        
    #
    # --------------------------------------------------------------------------------
    # check_default_gateway_action
    #  
    @staticmethod
    def begin_default_gateway_check_action():
        """
        This is an action proc for the data-driven command module that is used
        in conjunction with the end_default_gateway_check_action to check if a
        CLI command results in an improperly configured default gateway setting.
        Currently the backend code that maps the network/interface settings
        for the controller to the /etc/network/interfaces file will only
        apply the default gateway setting if it's on the same subnet as an
        interface that's configured with a static IP address/netmask.
        So if there's no interface that matches the default gateway (either
        because it's configured to use DHCP or the subnet doesn't match)
        then the default gateway setting will be ignored and we want to warn
        the user about the misconfiguration. The check should be performed
        on any configuration change that could affect this check. This includes:
        
        1) setting the default gateway
        2) setting the mode of an interface to DHCP
        3) setting/resetting the ip/netmask of an interface
        4) deleting an interface
        
        We only want to emit the warning if the default gateway was previously
        not misconfigured and the current command resulted in a misconfiguration,
        so we want to check for a misconfiguration before applying the command
        and then check again after the settings have been updated. The
        begin_default_gateway_check_action is the action that is performed before
        the settings are updated and the corresponding end_default_gateway_check_action
        action is the one that is performed after the settings are updated.
        
        LOOK!: Ideally we should be doing this check in the sdncon code instead
        of here in the CLI code. That way the warning would also be returned
        for clients that are changing these settings directly via the REST
        API instead of via the CLI. But there isn't a really good mechanism
        currently for the REST API to return a warning but still apply the
        config change.
        """
        # Stash away if the gateway setting was already misconfigured at the
        # beginning of the current command. 
        # LOOK! This is a bit kludgy to stash this away in the CLI object.
        # When the command module action API is changed to allow access to the
        # context, this code should be updated to stash the value in the context
        # instead of the CLI.
        cli.begin_misconfigured_default_gateway = SDNSh.get_misconfigured_default_gateway()

    @staticmethod
    def end_default_gateway_check_action():
        """
        This is an action proc for the data-driven command module that is used
        in conjunction with the begin_default_gateway_check_action to check if a
        CLI command results in an improperly configured default gateway setting.
        Check the doc comments for begin_default_gateway_check_action for more
        details about how the two actions are intended to be used.
        """
        end_misconfigured_default_gateway = SDNSh.get_misconfigured_default_gateway()
        if (end_misconfigured_default_gateway and
            (end_misconfigured_default_gateway != cli.begin_misconfigured_default_gateway)):
            return  ('The controller is configured with a default gateway setting (%s), that\n'
                     'is not on the same subnet as any of the interfaces configured to use a\n'
                     'static IP address. The default gateway setting will be ignored.' %
                     end_misconfigured_default_gateway)
 
 
    #
    # --------------------------------------------------------------------------------
    # load_time_zone_list
    #  Use the rest api to collect all the known time zones
    #
    @staticmethod
    def load_time_zone_list():
        if not cli.time_zone_list:
            url = "http://%s/rest/v1/system/timezones/all" % cli.controller
            cli.time_zone_list = cli.rest_simple_request_to_dict(url)
 
    #
    # --------------------------------------------------------------------------------
    # time_zone_completion
    #
    @staticmethod
    def time_zone_completion(words, text, completions):
        SDNSh.load_time_zone_list()
        completion_dict = {}
        tz_completions = []
        for tz in cli.time_zone_list:
            if tz.lower().startswith(text.lower()):
                tz_completions.append(tz)
                for index in range(len(text), len(tz)):
                    if tz[index] == '/':
                        tz = tz[:index+1]
                        break
                completion_dict[tz] = 'Timezone Choice'
        
        if len(completion_dict) >= 1:
            completions.update(completion_dict)
    
    #
    # --------------------------------------------------------------------------------
    # time_zone_validation
    #
    @staticmethod
    def time_zone_validation(typedef, value):
        SDNSh.load_time_zone_list()
        if value not in cli.time_zone_list:
            raise error.ArgumentValidationError('Invalid time zone')
    
    #
    # --------------------------------------------------------------------------------
    # cp_clock
    #
    @staticmethod
    def cp_clock(words, text, completion_char):
        return command.do_command_completion(words, text)
                
    #
    # --------------------------------------------------------------------------------
    # do_clock
    #
    @staticmethod
    def do_clock(words):
        return command.do_command(['clock'] + words)
    
    #
    # --------------------------------------------------------------------------------
    # do_show_clock
    #
    def cp_show_clock(self, words, text, completion_char):
        if len(words) == 1 and 'detail'.startswith(text.lower()):
            return ['detail']
    
    
    @staticmethod
    def get_clock_string(time_info, detail = None):
        # The tz item contains the abbreviated time zone string (e.g. PST, EST)
        # This is different from the full time zone string (e.g. America/Los_Angeles)
        # which we get from the controller-node object below when we're showing
        # detail info.
        tz = time_info['tz']
        del time_info['tz']
        dt = datetime.datetime(**time_info)
        if detail:
            time_info['tz'] = tz
        
        # Arista-style format
        #show_result = dt.strftime('%c')
        
        # Cisco-style format
        clock_string = dt.strftime('%H:%M:%S '+ tz + ' %a %b %d %Y')
        
        return clock_string

    #
    # --------------------------------------------------------------------------------
    # do_show_clock
    #
    @staticmethod
    def do_show_clock(words):
        syntax_help = "Syntax: show clock [detail]"
        
        if len(words) == 0:
            detail = False
        elif len(words) == 1:
            if not 'detail'.startswith(words[0].lower()):
                return syntax_help
            detail = True
        else:
            return syntax_help

        url = "http://%s/rest/v1/system/clock/local/" % cli.controller
        time_info = cli.rest_simple_request_to_dict(url)
        clock_string = SDNSh.get_clock_string(time_info, detail)
        return clock_string

    #
    # --------------------------------------------------------------------------------
    # cp_show_vns
    #
    def cp_show_vns(self, words, text, completion_char):
        if len(words) == 1:
            return objects_starting_with('vns-definition', text)
        elif len(words) == 2:
            return [x for x in
                    [ 'interface', 'mac-address-table', 'interface-rules',  \
                       'access-lists', 'running-config', 'switch', 'flow' ]
                    if x.startswith(text)]
        elif len(words) == 3 and 'switch'.startswith(words[2]):
            return objects_starting_with("switch", text)
        elif (len(words) >= 3) and (words[2] == 'flow'):
            return self.cp_show_vns_flow(words, text)
        else:
            self.print_completion_help("<cr>")

    #
    # --------------------------------------------------------------------------------
    # show_vns_definition_running_config
    # Display the vns-definition -- the interface rules for the vns.
    #
    def show_vns_definition_running_config(self, config, vns_name,tenant=None,indent=0):
        if tenant==None and vns_name!='all':
            vns_id='default|'+vns_name
        else:
            vns_id=tenant +'|'+vns_name
        try:
            vns = self.get_object_from_store('vns-definition', vns_id)
        except:
            return self.error_msg('no vns named %s' % vns_name)

        config.append(' ' *2*indent + "vns-definition %s\n" % vns_name)
        run_config.running_config_vns_details(config, vns, indent+1)

        vns_rules = None
        try:
            vns_rules = self.get_table_from_store('vns-interface-rule',
                                                  'vns', vns_id, "exact")
        except Exception:
            pass
        
        if vns_rules:
            run_config.running_config_vns_if_rule(config,
                                                  vns_rules,indent+1)

    #
    # --------------------------------------------------------------------------------
    # show_vns_running_config
    #  In the larger 'running-config' for vns, the complete vns tables are read
    #  since the complete 'running-config' will display all the data from the
    #  fields.  Here, the tables are searched only for specific vns entries.
    #
    #  The procedure's name is choosen carefully, since its not intended to
    #  part of any of show command, completion, or command processing
    #
    def show_vns_running_config(self, vns_name, tenant=None, indent=0):
        config=[]
        preconfig=[' '*2*indent +"vns %s\n" % vns_name]
        if tenant==None and vns_name!='all':
            vns_name='default|'+vns_name
        if tenant!=None:
            vns_name=tenant+'|'+vns_name
        try:
            self.get_object_from_store('vns-definition', vns_name)
        except:
            return self.error_msg('no vns named %s' % vns_name)
        vns_acls = []
        try:
            vns_acls = self.get_table_from_store('vns-access-list',
                                                 'vns', vns_name, 'exact')
        except Exception, e:
            pass

        for acl in vns_acls:
            # note use of key initialized above

            vns_acl_entries = None
            try:
                vns_acl_entries = self.get_table_from_store('vns-access-list-entry',
                                                            'vns-access-list',
                                                            acl['id'],
                                                            'exact')
            except Exception:
                pass
            run_config.running_config_vns_acl(config, vns_name, acl, vns_acl_entries,indent+1)

        vns_interface_acl = None
        try:
            vns_interface_acl = self.get_table_from_store('vns-interface-access-list')
        except Exception:
            pass

        for vns_if in run_config.running_config_active_vns_interfaces(vns_name, vns_interface_acl):
            config.append(' ' *2*(indent+1) + "interface %s\n" % vns_if)
            run_config.running_config_vns_if_and_access_group(config,
                                                              vns_name,
                                                              vns_if,
                                                              vns_interface_acl,indent+2)
        if len(config) > 0:
            config = preconfig + config
        return ''.join(config)

    #generate tenant xxx running config
    def show_tenant_running_config(self, config, tenant_name):
        try:
                tenants = self.get_object_from_store('tenant', tenant_name)
        except:
                return self.error_msg('no tenant named %s' % tenant_name)
    
        config.append("!\ntenant %s\n" % tenant_name)
        run_config.running_config_tenant_details(config,tenants)

        try:
            vnses = self.get_table_from_store('vns-definition','tenant',tenant_name,"exact")
        except Exception:
            vnses = {}
            pass
        try:
            virtual_routers = self.get_table_from_store('virtualrouter','tenant', tenant_name, "exact")
        except Exception:
            virtual_routers = {}
            pass

        for vns in vnses:
            vns_name=vns['vnsname']
            self.show_vns_definition_running_config(config, vns_name, tenant=tenant_name,indent=1)
            config += self.show_vns_running_config(vns_name, tenant=tenant_name,indent=1)
            
        for virtual_router in virtual_routers:
            virtual_router_name=virtual_router['vrname']
            virtual_router_id=virtual_router['id']
            config.append("  router %s\n" % virtual_router_name)
            run_config.running_config_tenant_router_details(config,virtual_router,indent=1)
            try:
                vr_interfaces = self.get_table_from_store('virtualrouter-interface','virtual-router', virtual_router_id, "exact")
            except Exception:
                vr_interfaces = {}
            pass
            try:
                vr_routes = self.get_table_from_store('virtualrouter-routingrule','virtual-router', virtual_router_id, "exact")
            except Exception:
                vr_routes = {}
            pass
            try:
                vr_gwpools = self.get_table_from_store('virtualrouter-gwpool','virtual-router', virtual_router_id, "exact")
            except Exception:
                vr_gwpools = {}
            pass

            for vr_interface in vr_interfaces:
                run_config.running_config_router_interface_details(config,vr_interface,indent=2)
            for vr_route in vr_routes:
                run_config.running_config_router_rule_details(config,vr_route,indent=2)
            for vr_gwpool in vr_gwpools:
                run_config.running_config_router_gwpool_details(config,vr_gwpool,indent=2)
      
    #
    # --------------------------------------------------------------------------------
    # switch_port_match
    #  Return True when the port matches a port specirfication "pattern"
    #
    #  The port specification of an interface rule is a command separated list,
    #  which can contain a tail of "-<d>".  That tail describes a range.
    #  With a port range, determine whether or not the 'port' parameter
    #  is a match.
    #
    def switch_port_match(self, port, pattern):
        # XXX validate port is something which ends in a number?
        for field in pattern.split(','):
            m = re.match(r'^([A-Za-z0-9-]*?)(\d+)-(\d+)$', pattern)
            if not m:
                if field == port:
                    return True
                continue
            prefix = m.group(1)
            startport = int(m.group(2))
            endport = int(m.group(3))
            if not port.startswith(prefix):
                continue
            tail = port[len(prefix):]
            if not self.DIGITS_RE.match(tail):
                print self.error_msg("port name tail %s must be all digits" % tail)
                continue
            m = re.search(r'(\d+)$', port)
            if not m:
                continue
            port = m.group(1)
            if int(port) >= int(startport) and int(port) <= int(endport):
                return True
            else:
                print self.error_msg("port %s out of port-spec range %s-%s" %
                                     (port, startport, endport))
        return False

    #
    # ---------------------------------------------------------------------------
    # cp_show_event_history
    #  Command completion for the following command to see
    #  last <n> events of <name> events
    #  Used in "show event-history <name> [ last <n> ]"
    #
    def cp_show_event_history(self, words, text, completion_char):
        options = ['attachment-point', 
                   'packet-in',
                   'topology-link',
                   'topology-switch',
                   'topology-cluster',
                   # Add more event history options above this line
                   # Add the same item in do_show_event_history() below also
                  ]
        completion_help = ''
        for op in options:
            ev_help = '%s  show %s events\n' % (op, op)
            completion_help = completion_help + ev_help

        # syntax: show event-history <name> [ last <n> ]
        if (len(words) == 1) and text=='':
            self.print_completion_help(completion_help)
            return options
        elif (len(words) == 1):
            return [op for op in options if op.startswith(text)]
        elif (len(words) == 2):
            if text == '':
                self.print_completion_help("last  Show lastest <n> events\n" +
                                           "<cr>  Enter")
                return ['last', '<cr>']
            elif "last".startswith(text):
                return ["last"]
        elif (len(words) == 3):
            self.print_completion_help("<number>  Enter number of latest events to display (1-10000)")
            return range(0, 10000)
        else:
            self.print_completion_help('<cr>')

    #
    # --------------------------------------------------------------------------
    # do_show_event_history
    #
    # Show the event history of the specified events
    # Syntax: show event-history <event-history-name> [last <count>]
    # <event-history-name> is words[0], <count> is words[2]
    #
    def do_show_event_history(self, words):
        self.debug_msg("do-show-event-history words: %s" % words)

        # Check syntax of the command
        syntax = self.syntax_msg('show event-history <name> [ last <n> ]')
        options = ['attachment-point', 
                   'packet-in',
                   'topology-link',
                   'topology-switch',
                   'topology-cluster',
                   # Add more event history options above this line
                   # Add the same item in do_cp_event_history() above also
                  ]
        if (words[0] not in options):
            yield("Event history name %s not found" % words[0])
            return
        if (len(words) >= 3) and (words[1] != 'last'):
            yield(syntax)
            return
        if (len(words) == 2) or (len(words) > 4):
            yield(syntax)
            return
        if (len(words) >= 3) and (not words[2].isdigit()):
            yield(syntax)
            return
        if (len(words) >= 3) and (words[2].isdigit()):
            count = int(words[2])
            if (count < 1) or (count > 10000):
                yield("Number of events must be between 1 and 10000")
                return

        last_n_count = 1024 # default
        if (len(words) >= 3):
            last_n_count = words[2]
        ev_hist_name = words[0]
        ev_hist_field_name = 'ev-hist-' + ev_hist_name
        url = 'http://%s/rest/v1/event-history/%s/%s' % \
                               (self.controller, ev_hist_name, last_n_count)
        ev_hist = self.rest_simple_request_to_dict(url)
        #ev_hist - json.dumps(ev_hist)
        events = ev_hist['events']
        tableData = []
        for ev in events:
            info      = ev['info']
            base_info = ev['base_info']
            info.update(base_info)
            tableData.append(info)
        # Prepare the date for CLI display
        tableData = self.pp.format_table(tableData, ev_hist_field_name)
        yield("\n")
        yield(tableData)

    #
    # -------------------------------------------------------------------------------
    # cp_show_vns
    #  Command completion for the following command
    #  Used in "show vns { <vns-name> | all } flow [ brief | full-detail | detail | summary ]
    #
    def cp_show_vns_flow(self, words, text, completion_char):
        cmd1 = 'brief        show flow information in brief format'
        cmd2 = 'detail       show detailed flow information'
        cmd3 = 'full-detail  show full details of flow information'
        cmd4 = 'summary      show summarized flow information'

        syntax = self.syntax_msg('show vns {<vns-name> | all} flow [ brief | detail | full-detail | summary ]')
        if (len(words) == 3) and text=='':
            self.print_completion_help(cmd1+'\n'+cmd2+'\n'+cmd3+'\n'+cmd4)
            return ['brief', 'detail', 'full-detail', 'summary', '<cr>' ]
        elif (len(words) == 3) and ('detail'.startswith(text)):
            return ['detail']
        elif (len(words) == 3) and ('full-detail'.startswith(text)):
            return ['full-detail']
        elif (len(words) == 3) and ('summary'.startswith(text)):
            return ['summary']
        elif (len(words) == 3) and ('brief'.startswith(text)):
            return ['brief']
        elif (len(words) == 4) and (words[3] not in ['brief', 'detail', 'full-detail', 'summary']):
            self.print_completion_help(syntax)
        elif (len(words) >= 3) and (text != ''):
            self.print_completion_help(syntax)
        else:
            self.print_completion_help('<cr>')

    #
    # --------------------------------------------------------------------------------
    # show_vns_flow_annotated
    #
    # For a given vns-name, show all the active flows in that vns 
    # or
    # for show active-flows in all vnses, categorized by vns
    # Used in "show vns { <vns-name> | all } flow [ brief | full-detail | detail | summary ]
    # flow records are annotated in the sdnplatform with vns name
    #
    def show_vns_flow_annotated(self, words):

        syntax = self.syntax_msg('show vns {<vns-name> | all} flow [ brief | detail | full-detail |summary ]')
        if (len(words) == 3) and (words[2] not in ['brief', 'details', 'full-detail', 'summary']):
            print syntax
            return

        if (len(words) > 3):
            print syntax
            return
        
        # all = False is this used anywhere
        
        vns = words[0]
        
        option = 'none'
        if (len(words) == 3):
            option = words[2]        

        # Get all the flows in the network
        annotated_flow_data = {}
        url = 'http://%s/rest/v1/vns/realtimestats/flow/%s' % (self.controller, vns)
        annotated_flow_data = self.rest_simple_request_to_dict(url)
        #print "Data=%s" % json.dumps(annotated_flow_data, sort_keys=True, indent=4)
        # annotated_flow_data response is in the following format:
        # Data={
        #   "vnsCount": 1, 
        #   "vnsFlowMap": {
        #            "two": {
        #                "flowCount": 10, 
        #                "flowList": [
        #                     {
        #                       "dpid": "00:00:00:00:00:00:00:0f", 
        #                       "flowEntry": {
        #                          "actions": [
        #                               {

        # vnsCount = annotated_flow_data["vnsCount"]
        vnsFlowMap = annotated_flow_data["vnsFlowMap"]
        vnsAddressSpaceMap = annotated_flow_data["vnsAddressSpaceMap"]
        
        if (option == "brief"):
            table_field_ordering = "vns_flow"
        elif (option == "detail"):
            table_field_ordering = "details"
        elif (option == "full-detail"):
            table_field_ordering = "undefined" # prints everything!
        elif (option == "summary"):
            table_field_ordering = "summary"            
        else:
            table_field_ordering = "default"        
        
        summaryTable = [] # used for holding the data for "summary" option
        for vnsName in vnsFlowMap:
            if (option == 'brief') or (option == "summary"):
                flow_tuple_list = []
                briefFlowCnt = 0
            # Table Data will hold all the flow entries, one list element per row of output
            # It is reinitilized for each VNS   
            tableData  = []
            vnsFlowCnt = vnsFlowMap[vnsName]["flowCount"]                    
            vnsFlowList = vnsFlowMap[vnsName]["flowList"]
            for vnsFlowEntry in vnsFlowList:                
                flowEntry = vnsFlowEntry["flowEntry"]
                dpid      = vnsFlowEntry["dpid"]
                if (option == "brief") or (option == "summary"):                    
                    src_mac   = flowEntry['match']['dataLayerDestination']
                    dst_mac   = flowEntry['match']['dataLayerSource']
                    vlan      = flowEntry['match']['dataLayerVirtualLan']
                    etherType = flowEntry['match']['dataLayerType']
                    flowList  = []
                    flowList.append(src_mac)
                    flowList.append(dst_mac)
                    flowList.append(vlan)
                    flowList.append(etherType)
                    if flowList in flow_tuple_list:
                        # duplicate flow (due to same flow entry on multiple switches along the path, 
                        # skip it if the option if brief or summary
                        continue
                    else:
                        flow_tuple_list.append(flowList)
                        briefFlowCnt += 1
                #print "DPID = %s" % dpid
                #print " New Data = %s" % json.dumps(flowEntry, indent=4)
                if (option != "summary"):
                    tableEntry = {}
                    tableEntry[dpid] = []
                    tableEntry[dpid].append(flowEntry)
                    tableEntry = self.add_dpid_to_data(tableEntry)
                    tableEntry = self.fix_realtime_flows(tableEntry)
                    #print "Table Entry : %s" % json.dumps(tableEntry, indent=4)
                    tableData.append(tableEntry[0])
            #print "*** Table Data: %s" % json.dumps(tableData[0], indent=4)
            #printf tenant + vnsname
            names=vnsName.split('|')
            tenant=names[0]
            vns=names[1]
            if (option == "brief"):
                yield("\nTenant %s VNS %s (address-space %s) flows (count: %s)" % (tenant, vns, vnsAddressSpaceMap[vnsName], briefFlowCnt))
            elif (option != "summary"):
                yield("\nTenant %s VNS %s (address-space %s) flow entries (count: %s)" % (tenant, vns, vnsAddressSpaceMap[vnsName], vnsFlowCnt))
            # Print flow data for one vns, unless option is summary
            if (option == "summary"):
                summaryData = dict()
#                summaryData['tenant']       =tenant
                summaryData["vnsName"]      = vns
                summaryData["flowEntryCnt"] = vnsFlowCnt
                summaryData["vnsFlowCnt"]   = briefFlowCnt
                summaryTable.append(summaryData)
            else:
                yield("\n") 
                # print "*** TD = %s" % tableData
                tableData = self.pp.format_table(tableData, "realtime_flow", table_field_ordering)
                yield(tableData)
                
        if (option == "summary"):            
            # print "*** ST %s" % summaryTable
            yield("\n")
            summaryTable = self.pp.format_table(summaryTable, "realtime_flow", table_field_ordering)            
            yield(summaryTable)
        print(" ") 
        return
  
    #
    # --------------------------------------------------------------------------------
    # show_vns_switch_ports
    #  For some vns-name, collect all the interfaces, and from that,
    #  collect all the attachment points.  The objective here is to
    #  name all the switch's and ports associated with a vns
    #
    def show_vns_switch_ports(self, words):
        if len(words) < 1:
            return

        # words:  [ vns_name, 'switch', switch_name ]
        vns_name = words[0]
        switch = None
        if len(words) > 2:
            switch = convert_alias_to_object_key('switch', words[2])
        port = None
        if len(words) > 3:
            port = words[3]

        vns_interface_key = mi.pk('vns-interface')
        if vns_name == 'all':
            vns_ifs = create_obj_type_dict('vns-interface',
                                           vns_interface_key,
                                           'id')
            vns_rules = create_obj_type_dict('vns-interface-rule',
                                             mi.pk('vns-interface-rule'),
                                             'id',)
        else:
            vns_ifs = create_obj_type_dict('vns-interface',
                                           vns_interface_key,
                                           'vns',
                                           vns_name)
            vns_rules = create_obj_type_dict('vns-interface-rule',
                                             mi.pk('vns-interface-rule'),
                                             'vns',
                                             vns_name)
        host_dap_dict = self.create_attachment_dict()

        bsp = {}  # _b_vs _s_witch _p_ort
        for ifs in vns_ifs:
            fields = ifs.split('|')
            # all these fields must have two parts.
            if len(fields) != 3:
                continue
            # choose between switch based rules and mac/ip based rules
            rule = 'default'
            vns_name = vns_ifs[ifs][0]['vns']
            switch_rule = None
            if fields[1] != 'default':
                rule = vns_ifs[ifs][0]['rule']
                if not rule in vns_rules:
                    continue
                if_rule = vns_rules[rule][0]
                if 'switch' in if_rule:
                    switch_rule = if_rule['switch']
            else:
                if_rule = { 'mac' : 'defaultLie' } # for default rules

            # There's two major classes of rules: host and switch
            # Use existance of a 'switch' to differentiate between the two.
            if switch_rule:
                # if_rule/switch are both set, part after the '/' is the port
                if fields[2].find('/') < 0:
                    # skip Eth<n> general interface
                    continue
                #
                # Prefix this port with a plus sign, which will be recognized
                # in the printing code as already describing an openflow port name
                port = '+' + fields[2].split('/')[1]
                tenant_vns=vns_name.split('|')
                tenant_name=tenant_vns[0]
                vns_real_name=tenant_vns[1] 
                key = "%s|%s|%s|%s" % (tenant_name,vns_real_name, switch, port)

                if not key in bsp:
                    bsp[key] = {
                            'tenant'      : tenant_name,
                            'vns'         : vns_real_name,
                            'switch'      : switch_rule,
                            'port'        : port,
                            'reason'      : ["Rule:%s" % rule.split('|')[-1], fields[1]],
                    }
                else:
                    pass # should only be one.
            else:
                # the second part (fields[1]) is the interface-long name,
                items = fields[2].split('/')
                if len(items) == 1:
                    if items[0].startswith('VEth'):
                        if not 'mac' in rule:
                            continue
                        mac = if_rule['mac']
                    elif items[0].startswith('Eth'):
                        self.debug_msg('should have been a switch in if_rule')
                        continue
                    else:
                        self.debug_msg('item length of 1')
                        continue
                else:
                    mac = items[1]
                    if not re.match(r'^(([A-Fa-f\d]){2}:?){5}[A-Fa-f\d]{2}$', mac):
                        print 'Not a mac address: %s' % mac
                # currently just use the mac to find the attachment points.
                if mac in host_dap_dict:
                    for attachment_point in host_dap_dict[mac]:
                        # if switch is named, skip any which don't match
                        if switch and attachment_point['switch'] != switch:
                            continue
                        if port and attachment_point['ingress-port'] != port:
                            continue

                        tenant_vns=vns_name.split('|')
                        tenant_name=tenant_vns[0]
                        vns_real_name=tenant_vns[1] 
                        key = "%s|%s|%s|%s" % (tenant_name,
                                            vns_real_name,
                                            attachment_point['switch'],
                                            attachment_point['ingress-port'])
                        if not key in bsp:
                            bsp[key] = {
                                    'tenant' : tenant_name,
                                    'vns'    : vns_real_name,
                                    'switch' : attachment_point['switch'],
                                    'port'   : utif.try_int(attachment_point['ingress-port']),
                                    'reason' : ["Rule:%s" % rule.split('|')[-1], mac]
                                    }
                        else:
                            self.append_when_missing(bsp[key]['reason'],
                                                    "Rule:%s" % rule.split('|')[-1])
                            self.append_when_missing(bsp[key]['reason'], mac)

        sort = [bsp[x] for x in sorted(bsp.keys(),
                                       cmp=lambda a,b: cmp(utif.try_int(a.split('|')[2]),
                                                           utif.try_int(b.split('|')[2]))
                                           if a.split('|')[1] == b.split('|')[1]
                                           else cmp(b.split('|')[1],a.split('|')[1]))]

        return self.display_obj_type_rows('vns-switch-ports', sort)


    #
    # --------------------------------------------------------------------------------
    # show_switch_ports_vns
    #
    def show_switch_ports_vns(self, words):
        switch = None
        if len(words) > 1 and words[0] != 'all':
            switch = words[0]
            #
            # switch alias conversion
            value = alias_lookup('switch-alias', switch)
            if value:
                switch = value

        #
        # dictionary from the vns side, indexed by host.
        vns_ifs_dict = create_obj_type_dict('vns-interface',
                                            mi.pk('vns-interface'))
        #
        # dictionary of the interface-rules, by rule
        vns_rules = create_obj_type_dict('vns-interface-rule',
                                         mi.pk('vns-interface-rule'))
        #
        # from the list of hosts, identify the attachment points
        host_dap_dict = self.create_attachment_dict()

        #
        # there can be multiple attachment points for each of the
        # hosts.  iterate over the hosts,  find all the attachment points,
        # manage an association for the entries
        spb = {}  # _s_witch _p_ort _b_vs
        for ifs in vns_ifs_dict:
            fields = vns_ifs_dict[ifs][0]['id'].split('|')
            # all these fields must have two parts.
            if len(fields) != 2:
                continue
            # id parts are vns|interface
            vns_name = fields[0]

            # choose between switch based rules and mac/ip based rules
            rule = 'default'

            switch_rule = None
            mac_rule = None
            rule_fields = [rule]
            if vns_name == 'default':
                if fields[1].find('/') >= 0:
                    mac_rule = fields[1].split('/')[1]
            else:
                if not 'rule' in vns_ifs_dict[ifs][0]:
                    continue
                rule = vns_ifs_dict[ifs][0]['rule']
                rule_fields = rule.split('|')
                if_rule = vns_rules[rule][0]
                if 'switch' in if_rule:
                    if switch and if_rule['switch'] != switch:
                        continue
                    switch_rule = if_rule['switch'] # switch may be None.
                elif 'mac' in if_rule:
                    mac_rule = if_rule['mac']
                elif 'ip-subnet' in if_rule:
                    mac_rule = if_rule['ip-subnet']
                elif 'tags' in if_rule:
                    mac_rule = if_rule['tags']
                elif 'vlans' in if_rule:
                    mac_rule = if_rule['vlans']
            if mac_rule:
                if not mac_rule in host_dap_dict:
                    self.debug_msg("Unknown attachment point for %s" % mac_rule)
                    continue

                for attachment_point in host_dap_dict[mac_rule]:
                    key = "%s|%s" % (attachment_point['switch'],
                                      attachment_point['ingress-port'])
                    if switch and attachment_point['switch'] != switch:
                        continue

                    if not key in spb:
                        spb[key] = {
                                'switch' : attachment_point['switch'],
                                'port'   : utif.try_int(attachment_point['ingress-port']),
                                'vns'    : {vns_name : 1},
                                'reason' : ["Rule:%s" % rule_fields[-1], mac_rule]
                                }
                    else:
                        if vns_name in spb[key]['vns']:
                            spb[key]['vns'][vns_name] += 1
                        else:
                            spb[key]['vns'][vns_name] = 1
                        self.append_when_missing(spb[key]['reason'],
                                                 "Rule:%s" % rule_fields[-1])
                        self.append_when_missing(spb[key]['reason'], mac_rule)
            if switch_rule:
                if fields[1].find('/') >= 0:
                    #
                    # Prefix this port with a plus sign, which will be recognized
                    # in the printing code as already describing an openflow port name
                    port = '+' + fields[1].split('/')[1]
                    key = "%s|%s" % (switch_rule, port)

                    if not key in spb:
                        spb[key] = {
                                'switch' : switch_rule,
                                'port'   : port,
                                'vns'    : {vns_name : 1},
                                'reason' : ["Rule:%s" % rule_fields[-1], fields[1]]
                        }

        sort = [spb[x] for x in sorted(spb.keys(),
                                        cmp=lambda a,b: cmp(utif.try_int(a.split('|')[1]),
                                                            utif.try_int(b.split('|')[1]))
                                            if a.split('|')[0] == b.split('|')[0]
                                            else cmp(b.split('|')[0],a.split('|')[0]))]

        return self.display_obj_type_rows('switch-ports-vns', sort)
                                          # [spb[x] for x in sorted(spb.keys())])

    #
    # --------------------------------------------------------------------------------
    # do_show_vns
    #
    def do_show_vns(self, words):
        choices = ['interface', 'mac-address-table',
                   'interface-rules', 'access-lists', 'running-config', 'switch', 'flow' ]
        if words == None or len(words) > 2:
            if words[1] not in ['switch', 'flow']:
                return self.syntax_msg('show vns <vns-name> [ %s ] ' %
                                       ' | '.join(choices))
        if len(words) in [3, 4]:
            if words[1] == 'switch':
                return self.show_vns_switch_ports(words)
            elif words[1] == 'flow':
                return self.show_vns_flow_annotated(words)
            else:
                return self.syntax_msg('show vns <vns-name> [ %s ] ' %
                                       ' | '.join(choices))
        elif len(words) == 2:
            # words[0] ought to an existing vns
            # Allow show vns all flow [detail]
            if (not self.vns_find_name(words[0])) and (words[1] != 'flow'):
                return self.error_msg("show vns '%s': vns Not Found" % words[0])

            vns_key = self.prefix_search_key([words[0]])
            selection = utif.full_word_from_choices(words[1], choices)
            if not selection:
                return self.syntax_msg('show vns <vns-name> [ %s ' %
                                       ' | '.join(choices))
            if selection == 'interface':
                return self.find_and_display_vns_interface(words[0], words[2:])
            elif selection == 'mac-address-table':
                # the id for the host-vns-interface table has the host as the 'prefix',
                # preventing searching based on the prefix.  the vns is not even a foreign
                # key, which means the complete table must be read, then the vns association
                # must be determined, and then matched
                return self.display_vns_mac_address_table(words[0], words[2:])
            elif selection == 'interface-rules':
                return self.do_show_object(['vns-interface-rule', vns_key], "<no_key>")
            elif selection == 'access-lists':
                return self.do_show_object(['vns-access-list-entry', vns_key], "<no_key>")
            elif selection == 'running-config':
                return self.show_vns_running_config(words[0])
            elif selection == 'switch':
                return self.show_vns_switch_ports(words)
            elif selection == 'flow':
                return self.show_vns_flow_annotated(words)
        elif len(words) == 1:
            return self.do_show_object(["vns-definition", words[0]])
        else:
            return self.do_show_object(["vns-definition"])

    #
    # --------------------------------------------------------------------------------
    # do_show_vns_interface_rule
    #  do_show_object is used to construct the output for the table search.
    #  However, since a vns prefix key search is used to display the table,
    #  and since do_show_object behaves differently when displaying tables
    #  depending on whether the table is a key search or not, the 'with_search_key'
    #  parameter is set to "with_key" when the caller included an additional
    #  prefix for (key) for the rule, and otherwise '<no_key>' is used when
    #  the caller wants the complete interface-rule table for a single vns
    #  (which obvioulsy also implies a key search for 'vns|' prefied rules.
    #
    def do_show_vns_interface_rule(self, words):
        with_search_key = "<no_key>"
        if self.vns_name() is None:
            if len(words) > 0:
                search_object = ["vns-interface-rule", words[0]]
            else:
                search_object = ["vns-interface-rule"] 
        elif len(words) > 0:
            with_search_key = '-'.join(words)
            words.insert(0, self.vns_name())
            search_object = ["vns-interface-rule", 
                             self.unique_key_from_non_unique(words)]
        else:
            search_object = ["vns-interface-rule",
                             self.prefix_search_key([self.vns_name()]) ] 
            obj_type = 'vns-interface-rule'

            # --- this ought to be promoted to be used more ..
            s_dict = {}
            # for all foreign keys in this obj_type
            mode_dict = self.mode_stack_to_rest_dict({})

            # for fk in mi.obj_type_foreign_keys(obj_type):
            for kf in mi.compound_key_fields(obj_type, mi.pk(obj_type)):
                if mi.is_foreign_key(obj_type, kf):
                    (ref_ot, ref_fn) = mi.foreign_key_references(obj_type, kf)
                    if ref_ot in mode_dict:
                        s_dict[kf] = mode_dict[ref_ot]

            # for (n,v) in self.mode_stack_to_rest_dict({}).items():
                # for (fk_ot, fk_fn) in mi.foreign_key_xref[n][mi.pk(n)]:
                    # print n, fk_ot, fk_fn
                    # if fk_ot == obj_type:
                        # s_dict[fk_fn] = v
            entries = self.store.rest_query_objects(obj_type, s_dict)
            return self.display_obj_type_rows(obj_type, entries, with_search_key)
        return self.do_show_object(search_object, with_search_key)

    #
    # --------------------------------------------------------------------------------
    # cp_is_alias
    #  Used in cp_no to determine if the particular request is an alias completion
    #
    def cp_is_alias(self, words, text):
        if not self.in_config_submode() or len(words) != 2:
            return False
        obj_type = self.get_current_mode_obj_type()
        # LOOK! robv: Tweaked this from the commented out version below
        return obj_type in mi.alias_obj_type_xref
 

    #
    # --------------------------------------------------------------------------------
    # cascade_delete
    #  Delete interior objects' rows related via foreign keys
    #
    def cascade_delete(self, obj_type, id_value):
        global modi

        obj_key = mi.pk(obj_type)
        if obj_type in mi.foreign_key_xref and \
           obj_key in mi.foreign_key_xref[obj_type]:
            for (fk_obj_type, fk_name) in mi.foreign_key_xref[obj_type][obj_key]:
                if mi.is_cascade_delete_enabled(fk_obj_type):
                    #
                    # find any affected rows
                    try:
                        rows = self.get_table_from_store(fk_obj_type,
                                                         fk_name,
                                                         id_value,
                                                         "exact")
                    except Exception, e:
                        if self.debug or self.debug_backtrace:
                            errors = self.rest_error_to_dict(e, fk_obj_type)
                            print self.rest_error_dict_to_message(errors)
                        rows = []

                    # determine whether the foreign key can have a null
                    # value, in which case the row doesn't have to be deleted,
                    # just updated the foreign key to `None'.
                    if mi.is_force_delete_enabled(fk_obj_type):
                        delete = True
                    elif mi.is_null_allowed(fk_obj_type, fk_name):
                        delete = False
                    else:
                        delete = True

                    key = mi.pk(fk_obj_type)
                    for row in rows:
                        try:
                            if delete:
                                self.rest_delete_object(fk_obj_type, row[key])
                            else: # update
                                self.rest_update_object(fk_obj_type, key, row[key], 
                                                        { fk_name : None } )
                            errors = None
                        except Exception, e:
                            errors = self.rest_error_to_dict(e,
                                                             fk_obj_type + " "
                                                             + row[key])
                        if errors:
                            return self.rest_error_dict_to_message(errors)
                        self.debug_msg("cascade delete: %s: %s" % \
                                       (fk_obj_type, ', '.join(row[key].split('|'))))
                        if delete:
                            self.cascade_delete(fk_obj_type, row[key])

    #
    # --------------------------------------------------------------------------------
    # do_no
    #  Delete rows from a table, or update a field.  For fields which
    #  have defaults, 'no' referts the field to its default value.
    #
    def do_no(self, words):
        # format is no < obj_type > < id >
        if not self.in_config_mode():
            if len(words) < 1 or words[0] != 'debug':
                return self.error_msg("'no' command only valid in config mode")
        obj_type = self.get_current_mode_obj_type()
        option = words[0]
        #
        # manage deletion of complete rows in obj_types'
        #
        if option in self.obj_types_for_config_mode_starting_with():
            if not len(words) > 1:
                return self.error_msg("<id> must be specified for the object to delete")
            item = convert_alias_to_object_key(option, words[1])
 
            try:
                self.rest_delete_object(option, item)
                errors = None
            except Exception, e:
                errors = self.rest_error_to_dict(e, option + " " + item)
            #
            # cascade delete?
            if not errors:
                self.cascade_delete(option, item)
        #
        # manage removal/set-to-default of values in fields of an obj_type
        #
        elif option in self.fields_for_current_submode_starting_with(option):
            #
            # replace the obj_type with the alias obj_type when for
            # fields called 'alias' when the obj_type has one alias 
            if obj_type in mi.alias_obj_type_xref:
                aliases = mi.alias_obj_type_xref[obj_type]
                if option in aliases:
                    obj_type = option
                if len(aliases) == 1 and option == 'alias':
                    obj_type = aliases[0]
            if len(aliases) != 1:
                print self.error_msg("Internal 'no' more than one alias choice")
                if len(words) < 2:
                    return "Syntax: no %s <value>" % option
                item = words[1]
                try:
                    self.rest_delete_object(obj_type, item)
                    errors = None
                except Exception, e:
                    errors = self.rest_error_to_dict(e, obj_type + " " + item)
                return self.rest_error_dict_to_message(errors)
 
            key = mi.pk(obj_type)
            #
            # note: field_default_value returns None when no default is
            # provided.  its not clear whether default values are always
            # provided for fields which don't accept null values.
            #
            default_value = mi.field_default_value(obj_type, option)

            if mi.is_null_allowed(obj_type, option) and default_value:
                self.warning("'%s' accepts null and had a default "
                             "value; %s is set to the default value '%s" %
                             (obj_type, option, default_value))

            errors = self.rest_update_object(obj_type,
                                             key,
                                             self.get_current_mode_obj(),
                                             {option:default_value})
            # fall through to return
        elif self.in_config_vns_acl_mode() and self.ACL_RE.match(option):
            return self.do_vns_no(['access-list-entry'] + words)
        #elif self.in_config_controller_interface_mode() and 'firewall'.startswith(option):
        #    return self.do_no_firewall(words)
        else:
            try:
                command_words = ['no'] + words
                # Just try to execute the command. It will either succeed or
                # throw an exception
                return command.do_command(command_words)
            except urllib2.HTTPError, e:
                raise e
            except Exception, e:
                if self.debug or self.debug_backtrace:
                    traceback.print_exc()
                return self.error_msg("'%s' must be either a valid object type or a field" %
                                  option)

        # when errors == None, rest_error_dict_to_message returns None.
        return self.rest_error_dict_to_message(errors)


    #
    # --------------------------------------------------------------------------------
    # implement_ping
    #    Performs a traceroute to a switch or host.
    #    Input can either be a DPID, switch alias, or host (IP or domain).
    def implement_ping(self, data):
        
        count = '-c %d ' % data.get('count', 5)
        if not 'ip-address' in data:
            yield('Can\'t determine ping target')
            return

        ip_host = data['ip-address']
        if self.DPID_RE.match(ip_host):
            ip_host = self.get_ip_from_switch_dpid_or_alias(ip_host)

        cmd = 'ping %s%s' % (count, self.shell_escape(ip_host))
        for item in self.generate_subprocess_output(cmd):
            yield item


    #
    # --------------------------------------------------------------------------------
    # implement_traceroute
    #    Performs a traceroute to a switch or host.
    #    Input can either be a DPID, switch alias, or host (IP or domain).
    def implement_traceroute(self, data):

        if not 'ip-address' in data:
            yield('Can\'t determine traceroute target')
            return

        ip_host = data['ip-address']
        if self.DPID_RE.match(ip_host):
            ip_host = self.get_ip_from_switch_dpid_or_alias(ip_host)

        cmd = 'traceroute %s' % self.shell_escape(ip_host)
        for item in self.generate_subprocess_output(cmd):
            yield item

 
    #
    # --------------------------------------------------------------------------------
    # do_no_firewall
    #
    #def do_no_firewall(self, words):
    #    return self.do_firewall(words[1:], True)
        

    #
    # --------------------------------------------------------------------------------
    # firewall_rule_add_rule_to_entries
    #
    def firewall_rule_add_rule_to_entries(self, entries):
        for entry in entries:
            entry['rule'] = run_config.firewall_rule(entry)
        return entries


    #
    # --------------------------------------------------------------------------------
    # get_firewall_rules
    #  Returns a list of strings describing the firewall rules for a particular
    #  controller-node name.
    #
    def get_firewall_rules(self, node):
        key = mi.pk('firewall-rule')
        key_value = self.unique_key_from_non_unique([node])
        entries = self.get_table_from_store('firewall-rule', key, key_value)

        return self.firewall_rule_add_rule_to_entries(entries)

    #
    # --------------------------------------------------------------------------------
    # do_show_firewall
    #  Depends on get_firewall_rules to add 'rule' to entries so that
    #  the 'firewall-rule' table can display the rule's value in a
    #  easily identified syntax.
    #
    def do_show_firewall(self, words):
        return self.display_obj_type_rows('firewall-rule',
                                          self.get_firewall_rules(self.get_current_mode_obj()))

    # --------------------------------------------------------------------------------
    # handle_command
    #
    def handle_command(self, command_word, words):
        if type(command_word) == str:
            method = self.command_method_from_name(command_word)
            if method:
                return method(words)
        # XXX It would be better to only call do_command if it
        # was clear that this command actually existed.
        return command.do_command([command_word] + words)


    #
    # --------------------------------------------------------------------------------
    # find_with_quoting
    #
    #  Assumes start_index is not inside a quoted string.
    #
    @staticmethod
    def find_with_quoting(line, find_char, reverse=False, start_index=0):
        in_quoted_arg = False
        line_length = len(line)
        i = start_index
        found_index = -1;
        while i < line_length:
            c = line[i]
            if c in "\"'":
                if not in_quoted_arg:
                    quote_char = c
                    in_quoted_arg = True
                elif c == quote_char:
                    in_quoted_arg = False
                # otherwise leave in_quoted_arg True
            elif c == "\\" and in_quoted_arg:
                i += 1
            elif (c == find_char) and not in_quoted_arg:
                found_index = i
                if not reverse:
                    break
            i += 1
            
        return found_index
    
    #
    # --------------------------------------------------------------------------------
    # split_with_quoting
    #
    @staticmethod
    def split_with_quoting(line, separators=" \t"):
        word_list = []
        current_word = ""
        in_quoted_arg = False
        line_length = len(line)
        i = 0
        while i < line_length:
            c = line[i]
            i += 1
            if c in "\"'":
                if not in_quoted_arg:
                    in_quoted_arg = True
                    quote_char = c
                elif c == quote_char:
                    in_quoted_arg = False
                    word_list.append(current_word)
                    current_word = ""
                else:
                    current_word += c
            elif c == "\\" and in_quoted_arg:
                if i < line_length:
                    c = line[i]
                    current_word += c
                    i += 1
            elif (c in separators) and not in_quoted_arg:
                if current_word:
                    word_list.append(current_word)
                    current_word = ""
            else:
                current_word += c
                
        if current_word:
            word_list.append(current_word)
        
        return word_list
    
    #
    # --------------------------------------------------------------------------------
    # quote_item
    #  Some of the new model columns available as choices to select have the '|' 
    #  character as a separator.  For these choices to word, they need to be
    #  quoted
    #
    @staticmethod
    def quote_item(obj_type, item):
        if item.find("|") >= 0:
            return  '"' + str(item) + '"' 
        else:
            return str(item)

    #
    # --------------------------------------------------------------------------------
    # 
    def replay(self, file, verbose = True, command_replay = False):
        # Only replay the STR values, since the code ought to
        # stuff back the JSON values.
        play = open(file)
        rest_line_format = re.compile(r'^REST ([^ ]*)(  *)([^ ]*)(  *)(.*)$')
        cmd_line_format = re.compile(r'^COMMAND (.*)$')
        skip_command = True
        for line in play.read().split('\n'):
            # the format ought to be url<space>[STR|JSON]<space> ...
            match = rest_line_format.match(line)
            if match:
                if match.group(3) == 'STR':
                    if verbose:
                        print 'REST STR', match.group(1)
                    url_cache.save_url(match.group(1), match.group(5), 1000000)
                elif match.group(3) == 'JSON':
                    if verbose:
                        print 'REST JSON', match.group(1)
                    entries = json.loads(match.group(5))
                    url_cache.save_url(match.group(1), entries, 1000000)
                else:
                    print 'REST REPLAY NOT STR|JSON'
            elif len(line):
                match = cmd_line_format.match(line)
                if command_replay and match:
                    # skip the first command since it ought to be the replay enablement
                    if skip_command:
                        if verbose:
                            print 'SKIP COMMAND %s' % match.group(1)
                        skip_command = False
                    else:
                        line = self.split_with_quoting(match.group(1))
                        if verbose:
                            print 'COMMAND %s' % line
                        output = self.handle_multipart_line(line[0])
                        if output != None:
                            print output
                else:
                    print 'no MATCH +%s+' % line
        play.close()

    #
    # --------------------------------------------------------------------------------
    # handle_single_line
    #
    def handle_single_line(self, line):
        ret_val = None
        if len(line) > 0 and line[0]=="!": # skip comments
            return
        words = self.split_with_quoting(line)
        if not words:
            return
        #
        self.completion_reset()

        # Look for the replay keyword, use the first two tokens if the replay
        # keyword is in the first part of the command.
        if self.debug and len(words) >= 2:
            if words[0] == 'replay':
                # replay the file, remove the first two keywords
                self.replay(words[1], command_replay = len(words) == 2)
                if len(words) == 2:
                    return
                words = words[2:]

        # the first word of a line is either:
        # - a command - dependent on mode (show anywhere but configure only in enable)
        # - an object type - if we're in a config mode (either config or config submode)
        # - a field for an object - if we're in a config submode
        matches = [(x, "command") for x in self.commands_for_current_mode_starting_with(words[0])]
        matches.extend([(x, "config object") for x in self.obj_types_for_config_mode_starting_with(words[0])])
        matches.extend([(x, "field") for x in self.fields_for_current_submode_starting_with(words[0])])
        # LOOK!: robv Fix to work with field names where one name is a prefix of another
        if len(matches) > 1:
            for match_tuple in matches:
                if match_tuple[0] == words[0]:
                    matches = [match_tuple]
                    break
        if len(matches) == 1:
            match = matches[0]
            # Replace the (possibly) abbreviated argument with the full name.
            # This is so that the handlers don't need to all handle abbreviations.
            if type(match[0]) == str:
                words[0] = match[0]

            if match[1] == "field":
                ret_val = self.handle_field(words)
            elif match[1] == "config object":
                ret_val = self.handle_obj_type(words)
            else:
                ret_val = self.handle_command(words[0], words[1:])
                #ret_val = self.handle_command(match[0], words[1:])
        elif len(matches) > 1:
            ret_val = self.error_msg("%s is ambiguous\n" % words[0])
            for m in matches:
                ret_val += "%s (%s)\n" % m
        else:
            ret_val = self.error_msg("Unknown command: %s\n" % words[0])

        url_cache.command_finished(words)
        return ret_val

    #
    # --------------------------------------------------------------------------------
    # generate_pipe_output
    #
    def generate_pipe_output(self, p, output):
        fl = fcntl.fcntl(p.stdout, fcntl.F_GETFL)
        fcntl.fcntl(p.stdout, fcntl.F_SETFL, fl | os.O_NONBLOCK)

        for item in output:
            try:
                p.stdin.write(item)
            except IOError:
                break

            try:
                out_item = p.stdout.read()
                yield out_item
            except IOError:
                pass
            
        p.stdin.close()

        fcntl.fcntl(p.stdout, fcntl.F_SETFL, fl)
        while True:
            out_item = p.stdout.read()
            if (out_item):
                yield out_item
            else:
                p.stdout.close()
                break
        p.wait()

    #
    # --------------------------------------------------------------------------------
    # write_to_pipe
    #
    def write_to_pipe(self, p, output):
        for item in output:
            try:
                p.stdin.write(item)
            except IOError:
                break
        p.stdin.close()
        p.wait()
            
    #
    # --------------------------------------------------------------------------------
    # shell_escape
    #  Return a string, quoting the complete string, and correctly prefix any
    #  quotes within the string.
    #
    def shell_escape(self, arg):
        return "'" + arg.replace("'", "'\\''") + "'"

    #
    # --------------------------------------------------------------------------------
    # handle_pipe_and_redirect
    #
    def handle_pipe_and_redirect(self, pipe_cmds, redirect_target, output):
        # if redirect target is tftp/ftp/http/file, then we should actually stick
        # curl at the end of the pipe_cmds so it gets handled below
        if redirect_target:
            if redirect_target.startswith("tftp") or redirect_target.startswith("ftp") or \
               redirect_target.startswith("http") or redirect_target.startswith("file"):
                redirect_target = self.shell_escape(redirect_target)
                # add so it can be used below
                if pipe_cmds == None:
                    pipe_cmds = ""
                else:
                    pipe_cmds += " | "

                if redirect_target.startswith("ftp"): # shell_escape added quote
                    pipe_cmds += " curl -T - %s" % self.shell_escape(redirect_target)
                else:
                    pipe_cmds += " curl -X PUT -d @- %s" % self.shell_escape(redirect_target)

        if pipe_cmds:
            new_pipe_cmd_list = []
            for pipe_cmd in [x.strip() for x in pipe_cmds.split('|')]:
                # doing it this way let us handles spaces in the patterns
                # as opposed to using split/join which would compress space
                new_pipe_cmd = pipe_cmd
                m = re.search('^(\w+)(.*)$', pipe_cmd) 
                if m: 
                    first_tok = m.group(1)
                    rest_of_cmd = m.group(2).strip()
                    if first_tok.startswith("in"):
                        new_pipe_cmd = "grep -e " + rest_of_cmd
                    elif first_tok.startswith("ex"):
                        new_pipe_cmd = "grep -v -e" + rest_of_cmd
                    elif first_tok.startswith("begin"):
                        new_pipe_cmd =  "awk '/%s/,0'" % rest_of_cmd
                new_pipe_cmd_list.append(new_pipe_cmd)

            new_pipe_cmds = "|".join(new_pipe_cmd_list)
            if new_pipe_cmds:
                if redirect_target:
                    p = subprocess.Popen(new_pipe_cmds, 
                                         shell=True, 
                                         stdin=subprocess.PIPE, 
                                         stdout=subprocess.PIPE,
                                         stderr=subprocess.STDOUT)
                    output = self.generate_pipe_output(p, output)
                else:
                    p = subprocess.Popen(new_pipe_cmds, 
                                         shell=True, 
                                         stdin=subprocess.PIPE)
                    self.write_to_pipe(p, output)
                    output = None

        # only handle local file here as http/ftp were handled above via pipe
        if redirect_target:
            if redirect_target.startswith("config://"):
                m = re.search(self.local_name_pattern, redirect_target)
                if m:
                    join_output = ''.join(iter(output))
                    store_result = self.store.set_user_data_file(m.group(1), join_output)
                    if store_result:
                        result = json.loads(store_result)
                    else:
                        return self.error_msg("rest store result not json format")
                    if 'status' in result and result['status'] == 'success':
                        return None
                    elif 'message' not in result:
                        return self.error_msg("rest store result doesn't contain error message")
                    else:
                        return self.error_msg(result['message'])
                else:
                    print self.error_msg("invalid name-in-db (%s)\n" % redirect_target)
            else:
                return output

        return None

    #
    # --------------------------------------------------------------------------------
    # generate_command_output
    #
    @staticmethod
    def generate_command_output(ret_val):
        if (isinstance(ret_val, str) or \
            isinstance(ret_val, buffer) or \
            isinstance(ret_val, bytearray) or \
            isinstance(ret_val, unicode)):

            # yield ret_val
            if len(ret_val) and ret_val[-1] == '\n':
                ret_val = ret_val[:-1]
            for line in ret_val.split('\n'):
                yield line + '\n'
        elif ret_val != None:
            for item in ret_val:
                yield item

    #
    # --------------------------------------------------------------------------------
    # generate_line_output
    #
    # This is a generator that will generate the output of the 
    # command either as a string, or by iterating over a returned
    # iterable.  This way, a subcommand can return an iterable to 
    # lazily evaluate large amounts of output
    #
    def generate_line_output(self, line, dont_ask):
        while line:
            subline_index = self.find_with_quoting(line, ';')
            if subline_index < 0:
                subline_index = len(line)
            subline = line[:subline_index]
            line = line[subline_index+1:]
            ret_val = self.handle_single_line(subline)
            cnt = 1
            total_cnt = 0

            (col_width, screen_length) = self.pp.get_terminal_size()
            if type(self.length) == int:
                screen_length = self.length

            for item in self.generate_command_output(ret_val):
                if not dont_ask:
                    incr = 1 + (max((len(item.rstrip()) - 1), 0) / col_width)
                    if screen_length and cnt + incr >= screen_length:
                        raw_input('-- hit return to continue, %s) --' % total_cnt)
                        cnt = 0
                    cnt += incr
                    total_cnt += incr
                yield item

    #
    # --------------------------------------------------------------------------------
    # handle_multipart_line
    #
    # this is the outermost handler that should print
    #
    def handle_multipart_line(self, line):
        pipe_cmds = None
        redirect_target = None
        output = None

        # pattern is:
        # single line [; single line]* [| ...] [> {conf|ftp|http}]

        # first take off the potential redirect part then the pipe cmds
        redirect_index = self.find_with_quoting(line, '>', True)
        if redirect_index >= 0:
            redirect_target = line[redirect_index+1:].strip()
            line = line[:redirect_index].strip()
        pipe_index = self.find_with_quoting(line, '|')
        if pipe_index >= 0:
            pipe_cmds = line[pipe_index+1:].strip()
            line = line[:pipe_index].strip()
        # remaining text is single lines separated by ';' - handle them
        output = self.generate_line_output(line, pipe_cmds or redirect_target)

        # now output everything
        if pipe_cmds or redirect_target:
            output = self.handle_pipe_and_redirect(pipe_cmds, redirect_target, output)

        if output != None:
            for line in output:
                print line.rstrip()

    #
    # --------------------------------------------------------------------------------
    # cp_watch
    #
    def cp_watch(self, words, text, completion_char):
        if completion_char == ord('?'):
            if len(words) > 1:
                command.do_command_completion_help(words[1:], text)
            else:
                self.print_completion_help(self.help_splash([], text))
            return
        if len(words) == 1:
            items = self.commands_for_current_mode_starting_with(text)
            return utif.add_delim(items, ' ')
        else:   
            return command.do_command_completion(words[1:], text)

    #
    # --------------------------------------------------------------------------------
    # do_watch
    #   only called to display help
    #
    def do_watch(self, words):
        return 'watch: repeat indicated command after watch keyword'

    #
    # --------------------------------------------------------------------------------
    # handle_watch_command
    #
    # handle this here because this is a CLI-only command
    # LOOK! This could be using curses, but that has some complications with 
    # potentially messing up the terminal.  This is cheap but downside
    # is it uses up the scrollbuffer...
    #
    def handle_watch_command(self, line):
        #
        words = line.split()
        if len(words) == 0:
            return self.syntax_msg('watch: command to watch missing')
            
        if len(words) and words[0] == 'watch':
            return self.error_msg('watch command not supported for watch')

        while True: # must control-C to get out of this
            output = self.handle_multipart_line(line) 
            if output:
                os.system("clear")
                print "Executing %s " % line
                print output
            else:
                print "Executing %s " % line
            time.sleep(2)

    #
    #
    # --------------------------------------------------------------------------------
    # loop
    #  this is just dispatching the command and handling errors
    #
    def loop(self):
        command.action_invoke('wait-for-controller', (5))

        if self.controller:
            try:
                version_url = 'http://%s/rest/v1/system/version' % self.controller
                version = self.rest_simple_request_to_dict(version_url)
            except Exception, e:
                version = [{'controller' : 'REST API FAILURE\n'}]

            print "default controller: %s, %s" % (self.controller, 
                                                  version[0]['controller'])
            #
            # vns feature enablement.
            #  when vns is enabled, call a init procedure
            #
            if onos == 0:
                self.netvirt_feature_enabled()

        while self.run:
            # Get command line - this will use the command completion above
            try:
                #rest_to_model.validate_switch()
                url_cache.clear_cached_urls()
                line = raw_input(self.prompt)
                if self.batch:
                    print line
                m = re.search('^watch (.*)$', line) 
                if m:
                    self.handle_watch_command(m.group(1))
                else:
                    self.handle_multipart_line(line)
            except KeyboardInterrupt:
                self.completion_reset()
                print "\nInterrupt."
            except EOFError:
                print "\nExiting."
                return
            except urllib2.HTTPError, e:
                errors = self.rest_error_to_dict(e)
                print self.error_msg("%s" % self.rest_error_dict_to_message(errors))
            except urllib2.URLError, e:
                print self.error_msg("communicating with REST API server on %s " 
                                     "- Network error: %s" %
                                     (self.controller, e.reason))
            except Exception, e:
                print "\nError running command '%s'.\n" % line
                if self.debug or self.debug_backtrace:
                    traceback.print_exc()


#
# --------------------------------------------------------------------------------
# Initialization crazyness to make it work across platforms. Many platforms don't include
# GNU readline (e.g. mac os x) and we need to compensate for this

try:
    import readline
except ImportError:
    try:
        import pyreadline as readline
    except ImportError:
        print "Can't find any readline equivalent - aborting."
else:
    if 'libedit' in readline.__doc__:
        # needed for Mac, please fix Apple
        readline.parse_and_bind ("bind ^I rl_complete")
    else:
        readline.parse_and_bind("tab: complete")
        readline.parse_and_bind("?: possible-completions")

  
#
# --------------------------------------------------------------------------------
# Run the shell

def main():
    global cli
    # Uncomment the next two lines to enable remote debugging with PyDev
    # LOOK! Should have logic here that enables/disables the pydevd stuff
    # automatically without requiring uncommenting (and, more importantly,
    # remembering to recomment before committing).
    # (e.g. checking environment variable or something like that)
    #python_path = os.environ.get('PYTHONPATH', '')
    #if 'pydev.debug' in python_path:
    try:
        import pydevd
        pydevd.settrace()
    except Exception, e:
        pass

    # Process '--init' argument since it  gates progress to rest of processing
    # as this is a special argument, it is required to be first argument (if used)
    check_ready_file = True
    if len(sys.argv) > 1:
        if sys.argv[1] == '--init':
            check_ready_file = False
    
    # Do not start CLI till the systemn is ready
    # (allow user to ^C and get to SDNSh for debug etc)
    not_ready_file = '/opt/sdnplatform/run/starting'
    if check_ready_file:
        try:
            while True:
                if os.path.exists(not_ready_file):
                    with open(not_ready_file, "r") as f:
                        message = f.read()
                        if len(message):
                            print message,
                        else:
                            print 'Controller not yet ready ... waiting 5 sec'
                    time.sleep(5)
                else:
                    break
        except:
            if os.path.exists(not_ready_file):
                resp = raw_input('Controller is not yet ready. Do you still want to continue to the CLI? [n]')
                if resp and "yes".startswith(resp.lower()):
                    print 'Continuing with CLI despite initialization error ...'
                else:
                    print 'Aborting Controller CLI login.'
                    time.sleep(1)
                    return

    # Start CLI
    cli = SDNSh()
    cli.init()
    cli.loop()              

if __name__ == '__main__':
    main()
