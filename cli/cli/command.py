#
# Copyright (c) 2011,2012,2013 Big Switch Networks, Inc.
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

import re
import numbers
import collections
import traceback
import types
import json
import time
import sys
import datetime
import os
import c_data_handlers
import c_validations
import c_completions
import c_actions
import utif
import error
import doc


# TODO list
#
# - support for 'min' and 'max' as boundary values in range/length restrictions
# - distinguish between completion help string and syntax/doc string
# - allow multiple validation functions for typedefs/args
# - verify that it handles unambiguous prefixes in subcommands and argument tags
# - support for getting next available sequence number (e.g. ip name-server)
# - test code to handle REST errors
# - figure out how to deal with arguments that are optional for 'no' commands
# - tests for length/range validations, both for strings and integers
# - add validation patterns for domain-name and ip-address-or-domain-name typedefs
# - get syntax help for "no" commands
# - test using unambiguous prefix for command/subcommand names
# - test using unambiguous prefix for argument tags
# - test case insensitivity for command/subcommand/tag names (i.e. any fixed token)
# - Allow/ignore missing command arguments for "no" commands (e.g. "no ip address")
# - support for case-sensitive enum types (do we need this?)
# - clean up 'description' vs. 'descriptor' terminology (i.e. pick one)
# - return "<cr>" as completion if there are no args to complete
# - be consistent about using _xyz function naming convention for private functions
# - better handling of exceptions in do_command_completions. catch CommandErrors,
#   distinguish between expected error (e.g. ValidationError) and errors that
#   indicate bug (e.g. base Exception raised)
# - sort issues with sequence numbers. They are treated as strings by the Django
#   Cassandra backend instead of integers. This means that, for example, the sequence
#   number 1,2,10 are incorrectly sorted in the order 1, 10, 2. Need to fix this in
#   the Django backend.
# - Shouldn't allow "no" variant of a command if there's no default value for an argument.
#   For example, shouldn't allow "no clock set 01:30:00".
# - treat scope stack as a stack with append & pop operations and search in reverse order
# - rename the "data" item in the scopes to be "args" (or maybe not?)
# - clean up syntax help message; includes "Syntax: " twice in message.
# - clean up syntax help messages to not include extraneous white space characters
#   For example "test [foo <foo-value>]" instead of "test [ foo <foo-value> ]"
#   Or maybe not?
# - support for explicit command abbreviations (e.g. "s" = "show" even if there are
#   other available commands that begin with "s".
# - automated generation of running config
# - rename 'config' command-type to 'config-fields'
# - need to fix getting completions with alternation if any of the alternates don't
#   match the partial completion text (e.g. "firewall allow op<tab>").
# - allow command description that don't have any args to omit the 'args' setting
#   (workaround for now is to just have an empty args tuple/list

# - support "repeatable" attribute for args to support repetition
# - Support for single token arguments with a custom action and/or arg handler
# - allow multiple "name"s for a command description and allow each name to optionally
#   set a field in the arg data. Syntax would probably be that the "name" field could
#   be a list of names. So the name could either be a simple string, a dict (which
#   would support pattern-based names and setting a field with the command word, or a
#   list of names (where each name could be either the simple string or the dict)
# - maybe get rid of specifying mode in the command description. Instead a command
#   description would be enabled for a mode by adding it to the list of commands that
#   are enabled for that mode. This would happen in some data (or code) that's separate
#   from the command description. I think this would be better so that we can prune
#   the number of command description we need to parse for a given command. Otherwise
#   we always need to check against all command descriptions.
# - distinguish between exact match vs. prefix match in a choices block. If there's
#   only a single exact match then that should get precedence over other matches
#   and not be considered an ambiguous command
# - add 'default' attribute for the default value of an optional field if it's not
#   specified. Should this be treated as mutually exclusive with 'optional'?
# - convert reset-fields action to use fields instead of arg_data. If we're using
#   new-style hack then we probably need to have it be new-reset-fields or something
#   like that.
# - clean up usage of return values for the command handling methods, i.e.
#   handle_one_command and all of the other handle_* methods. Should maybe
#   just not return anything for any of those and just capture the
#   matches/completion in the data member.
# - fix behavior for printing <cr> as a completion at the end of a
#   complete command. Right now there's a bug where it will still print out
#   the syntax help.
# - support string token argument as just a simple string in the argument list
# - custom completion proc for cidr address. Currently if a command description
#   supports either cidr or ip+netmask it will handle either for command execution
#   but completion will only handle the ip+netmask variant. With a custom
#   completion proc you could combine the ip+netmask from the db and format as
#   a cidr address and return that as a completion



command_type_defaults = {}


# We rely on the higher level CLI object for certain operations, so we're initialized
# with a reference that's used to call back to it. This is initialized in init_command.
sdnsh = None

# command_registry is the list of command descriptions that have been registered
# via add_command. The reason this is a list (i.e. instead of a dictionary that's
# keyed by the command name) is that's it's possible that you could have different
# command descriptions with the same name but enabled for different modes.
command_registry = []

# command_submode_tree is a dictionary, where each submode, and its children
# are saved.
#
command_submode_tree = { 'login' : { 'enable' : { 'config' : {} } } }

# validation_registry is the dictionary of validation functions that have been
# registered via add_validation. The key is the name of the function as specified
# in typedef and command description. The value is a 2 item tuple where the first
# item is the validation function to invoke and the second item is a dictionary
# describing the arguments that are used when invoking the validation function.
# See the comments for _call_proc for more information about the format of the 
# data describing the args.
# invoke.
validation_registry = {}

# typedef_registry is the dictionary of typedef descriptions. The key is the
# name of the typedef and the value is the typedef description.
# value if the function to invoke
typedef_registry = {}

# action_registry is the dictionary of action functions that have been registered
# via add_action. The key is the name of the function as specified in
# command descriptions. The value is a 2 item tuple where the first
# item is the action function to invoke and the second item is a dictionary
# describing the arguments that are used when invoking the validation function.
# See the comments for _call_proc for more information about the format of the 
# data describing the args.
action_registry = {}

# completion_registry is the dictionary of completion functions that have been
# registered via add_completion. The key is the name of the function as specified
# in command descriptions. The value is a 2 item tuple where the first
# item is the completion function to invoke and the second item is a dictionary
# describing the arguments that are used when invoking the validation function.
# See the comments for _call_proc for more information about the format of the 
# data describing the args.
completion_registry = {}


# argument_data_handler_registry is the dictionary of custom argument data
# handlers that have been registered via add_argument_data_handler. These are
# used if custom processing is required to convert the argument value to the
# data that is added to the argument data dictionary. The key is the name of
# the function as specified in command descriptions. The value is a 2 item
# tuple where the first item is the handler function to invoke and the second
# item is a dictionary describing the arguments that are used when invoking
# the validation function. See the comments for _call_proc for more information
# about the format of the data describing the args.
argument_data_handler_registry = {}

#
# keep a list of all the command module names
command_added_modules = {}

#
command_syntax_version = {}

def add_command(command):
    command_registry.append(command)


def model_obj_type_disable_submode(obj_type):
    mi.obj_type_source_set_debug_only(obj_type)


def model_obj_type_enable_cascade_delete(obj_type):
    mi.cascade_delete_set_enable(obj_type)

def model_obj_type_weak_with_cascade_delete(obj_type):
    mi.cascade_delete_enable_force(obj_type)

def model_obj_type_set_title(obj_type, title):
    mi.obj_type_set_title(obj_type, title)


def model_obj_type_disable_edit(obj_type, field):
    mi.obj_type_disable_edit(obj_type, field)


def model_obj_type_set_case(obj_type, field, case):
    mi.set_obj_type_field_case_sensitive(obj_type, field, case)

def model_obj_type_set_show_this(obj_type, show_this_list):
    """
    When an obj-type gets displayed via 'show this', complex
    objects may require several different items to be displayed.
    The second parameter is a list, where each item has several
    fields: the first is the name of the object to display, and
    the second is the format to use for that display
    """
    mi.obj_type_set_show_this(obj_type, show_this_list)


def command_dict_minus_lec():
    # lec -- 'login', 'enable', 'config'.
    return [x for x in sdnsh.command_dict.keys()
            if x not in ['login', 'enable', 'config', 'config-']]


def command_dict_submode_index(submode):
    if submode in ['login', 'enable', 'config', 'config-']:
        return submode
    if submode.startswith('config-'):
        return submode
    if sdnsh.description:
        print _line(), "unknown submode ", submode
    

def add_command_mode_recurse(mode_dict, mode, submode):
    if mode in mode_dict:
        if not submode in mode_dict[mode]:
            mode_dict[mode][submode] = {}
    else:
        for d in mode_dict.keys():
            add_command_mode_recurse(mode_dict[d], mode, submode)


def add_command_submode(mode, submode):
    add_command_mode_recurse(command_submode_tree, mode, submode)

def show_command_submode_tree_recurse(mode_dict, level):
    if len(mode_dict):
        for d in mode_dict.keys():
            index = command_dict_submode_index(d)
            items = [x if _is_string(x) else x.pattern for x in sdnsh.command_dict.get(index, '')]
            print " " * level, d, ': ' + ', '.join(items)
            show_command_submode_tree_recurse(mode_dict[d], level + 2)


def show_command_submode_tree():
    show_command_submode_tree_recurse(command_submode_tree, 0)


def command_dict_append(command, mode, name):
    if not mode in sdnsh.command_dict:
        sdnsh.command_dict[mode] = []
    if type(name) == dict:
        # this is a regular expression name, add the re.
        if not 'pattern' in name:
            print '%s: missing pattern' % command['self']
        name['re'] = re.compile(name['pattern'])
    sdnsh.command_dict[mode].append(name)


def command_nested_dict_append(command, mode, name):
    if not mode in sdnsh.command_nested_dict:
        sdnsh.command_nested_dict[mode] = []
    if type(name) == dict:
        # this is a regular expression name, add the re.
        if not 'pattern' in name:
            print '%s: missing pattern' % command['self']
        name['re'] = re.compile(name['pattern'])
    if not name in sdnsh.command_nested_dict[mode]:
        sdnsh.command_nested_dict[mode].append(name)


def add_command_to_command_dict(command, mode, name, var):
    #
    if mode in ['login', 'enable']:
        command_nested_dict_append(command, mode, name)
    elif mode == 'config' or mode == 'config-':
        command_dict_append(command, mode, name)
    elif mode.endswith('*'):
        index = mode[:-1]
        command_nested_dict_append(command, index, name)
    elif mode.startswith("config"):
        command_dict_append(command, mode, name)



def add_command_using_dict(command_description, value):
    """
    """
    if not 'self' in value:
        value['self'] = command_description

    if not 'name' in value:
        print '%s: missing \'name\': in description' % command_description
        return
    if not 'mode' in value:
        print '%s: missing \'mode\': in description' % command_description
        return

    name = value['name']
    mode = value['mode']
    command_type = value.get('command-type')
    if command_type == 'config-submode':
        submode = value.get('submode-name')
        # add_command_submode(mode,submode)

    # XXX this needs improving
    feature = value.get('feature')
    if feature and name not in ['show']: 
        if not name in sdnsh.command_name_feature:
            sdnsh.command_name_feature[name] = []
        if not feature in sdnsh.command_name_feature[name]:
            sdnsh.command_name_feature[name].append(feature)
    #
    # populate sdnsh.controller_dict based on the modes described for this command
    #
    if 'mode' in value:
        if _is_list(mode):
            for m in mode:
                add_command_to_command_dict(value, m, name, command_description)
        else:
            add_command_to_command_dict(value, mode, name, command_description)
    add_command(value)


def add_commands_from_class(aclass):
    """
    """
    for (var, value) in vars(aclass).items():
        if re.match(r'.*COMMAND_DESCRIPTION$', var):
            add_command_using_dict(var, value)


def add_command_module_name(version, module):
    """
    Save some state about the version/module
    """
    if version not in command_syntax_version:
        command_syntax_version[version] = [module]
    else:
        command_syntax_version[version].append(module)


def add_commands_from_module(version, module, dump_syntax = False):
    add_command_module_name(version, module)
    for name in dir(module):
        if re.match(r'.*COMMAND_DESCRIPTION$', name):
            if module.__name__ not in command_added_modules:
                command_added_modules[module.__name__] = [name]
            if name not in command_added_modules[module.__name__]:
                command_added_modules[module.__name__].append(name)
            command = getattr(module, name)
            add_command_using_dict(name, command)
            if dump_syntax:
                handler = CommandSyntaxer(name)
                handler.add_known_command(command)
                print handler.handle_command_results()


def add_validation(name, validation_proc, default_args={'kwargs':
        {'typedef': '$typedef', 'value': '$value'}}):
    validation_registry[name] = (validation_proc, default_args)
    

def add_typedef(typedef):
    typedef_registry[typedef['name']] = typedef


def add_action(name, action_proc, default_args={}):
    action_registry[name] = (action_proc, default_args)

def action_invoke(name, action_args):
    # no validation to match args.
    action = action_registry[name] 
    action[0](action_args)
    
def add_completion(name, completion_proc, default_args= {'kwargs':
        {'words': '$words', 'text': '$text', 'completions': '$completions'}}):
    completion_registry[name] = (completion_proc, default_args)

def add_argument_data_handler(name, handler_proc, default_args = 
        {'kwargs': {'name': '$name', 'value': '$value',
                    'data': '$data', 'scopes': '$scopes'}}):
    argument_data_handler_registry[name] = (handler_proc, default_args)
    
def add_command_type(name, command_defaults):
    command_type_defaults[name] = command_defaults
 
def _line():
    # pylint: disable=W0212
    f = sys._getframe().f_back
    return '[%s:%d]' % (f.f_code.co_filename, f.f_lineno)

#
# VALIDATION PROCS
#

def _is_range_boundary(boundary):
    """
    Returns whether or not the given value is a valid boundary value.
    Valid values are integers or the special strings 'min' or 'max'
    (case-insensitive)
    """
    return (isinstance(boundary, numbers.Integral) or
        (_is_string(boundary) and (boundary.lower() in ('min','max'))))


def _convert_range_boundary(boundary, test_value):
    """
    Converts the given boundary value to an appropriate integral
    boundary value. This is used to handle the special "min' and
    'max' string values. The test_value argument should be the value
    which is being tested for whether or not it's in a range. Since
    long integral values have an unlimited precision there is no
    actual MIN or MAX value, so we just make the value one less or
    one more than the test_value, so that the range check in
    validate_integer against the test_value will fail or succeed
    appropriately. Note that this means that this function must be
    called again for each different test_value (i.e. you can't
    pre-process the ranges to remove the 'min' and 'max' values).
    """
    if _is_string(boundary):
        if boundary.lower() == 'min':
            boundary = test_value - 1
        elif boundary.lower() == 'max':
            boundary = test_value + 1
        else:
            raise error.CommandDescriptionError('Invalid range boundary constant; must be "min", "max" or integer value')
        
    return boundary


def _is_single_range(r):
    """
    Returns whether or not the argument is a valid single range.
    A valid range is either a single integral value or else a
    sequence (tuple or list) with 2 elements, each of which is a valid
    range boundary value.
    """
    return  (isinstance(r, numbers.Integral) or
            (isinstance(r, collections.Sequence) and (len(r) == 2) and
             _is_range_boundary(r[0]) and _is_range_boundary(r[1])))


def _check_one_range(r):
    """
    Checks that the argument is a valid single range as determined
    by _is_single_range.
    If it is, then the function returns None.
    It it's not, then a RangeSyntaxError exception is raised.
    """
    if not _is_single_range(r):
        raise error.RangeSyntaxError(str(r))


def _check_range(r):
    """
    Checks that the argument is a valid range.
    If it is, then the function returns None.
    It it's not, then a RangeSyntaxError exception is raised.
    FIXME: This doesn't seem to be used anymore. Remove?
    """ 
    if _is_single_range(r):
        _check_one_range(r)
    elif isinstance(r, collections.Sequence):
        for r2 in r:
            _check_one_range(r2)
    else:
        raise error.RangeSyntaxError(str(r))


def _lookup_typedef_value(typedef, name):
    """
    Look up the given name in a typedef.
    If it's not found in the given typedef it recursively searches
    for the value in the base typedefs.
    Returns None if the value is not found.
    FIXME: Note that this means there's currently no way to distinguish
    between an explicit None value and the name not being specified.
    """
    assert typedef is not None
    assert name is not None
    
    # Check if the typedef has the attribute
    value = typedef.get(name)
    if value:
        return value
    
    # Otherwise, see if it's defined in the base type(s)
    base_type_name = typedef.get('base-type')
    if base_type_name:
        base_typedef = typedef_registry.get(base_type_name)
        if not base_typedef:
            raise error.CommandDescriptionError('Unknown type name: %s' % base_type_name)
        return _lookup_typedef_value(base_typedef, name)

    return None


def _raise_argument_validation_exception(typedef, value, detail, expected_tokens=None):
    """
    Called when an argument doesn't match the expected type. Raises an
    ArgumentValidationError exception. The message for the exception
    can be customized by specifying a error format string in the
    type definition. The format string is called ' validation-error-format'.
    The format string can use the following substitution variables:
    - 'typedef': The name of the typedef
    - 'value': The value that didn't match the typedef restrictions
    - 'detail': Additional message for the nature of the type mismatch
    The default format string is: 'Invalid %(typedef)s: %(value)s; %(detail)s'.
    """
    typedef_name = typedef.get('help-name')
    if typedef_name is None:
        typedef_name = typedef.get('name')
        if typedef_name is None:
            typedef_name = typedef.get('field')
            if typedef_name is None:
                typedef_name = '<unknown-type>'
    if detail is None:
        detail = ''
    validation_error_format = typedef.get('validation-error-format',
                                          'Invalid %(typedef)s: %(value)s; %(detail)s')
    validation_error = (validation_error_format %
                        {'typedef': typedef_name, 'value': str(value), 'detail': detail})
    raise error.ArgumentValidationError(validation_error, expected_tokens)


#
# COMMAND MODULE FUNCTIONS
#

def _get_args(item):
    """
    Gets the args item from the given item. The item may be a command
    or subcommand description or a nested arg list inside a 'choice'
    argument. If there's no item names "args", then it returns None.
    If the args item is a single argument, then it's converted into
    a tuple containing that single argument, so that the caller can
    always treat the return value as an iterable sequence of args.
    It doesn't do any additional validation of the args value
    (e.g. it doesn't check that the individual arguments are dict objects).
    """
    args = item.get('args')
    if args and not isinstance(args, collections.Sequence):
        args = (args,)
    return args


def _get_choice_args(choice):
    if isinstance(choice, collections.Sequence):
        args = choice
    else:
        args = _get_args(choice)
        if not args:
            args = (choice,)
    return args


def _get_command_args_syntax_help_string(command, is_no_command, args):
    """
    Get the syntax help string for an argument list.
    """
    syntax_string = ''
    if args:
        for i, arg in enumerate(args):
            if i > 0:
                syntax_string += ' '
                
            if _is_string(arg):
                syntax_string += arg
                continue

            if type(arg) == tuple:
                if sdnsh.description:
                    print _line(), command['self'], arg

            if is_no_command:
                optional = arg.get('optional-for-no', False)
            else:
                optional = arg.get('optional', False)
            if optional:
                syntax_string += '['
                
            choices = arg.get('choices')
            nested_args = arg.get('args')
            if nested_args:
                if choices:
                    raise error.CommandDescriptionError('An argument can\'t have both '
                            '"choices" and "args" attributes', command)
                choices = (nested_args,)
            if choices:
                # Suppress choice delimiters if we've already emitted the square
                # brackets to indicate an optional argument. This is so we get
                # something simpler (e.g. "[this | that]" ) instead of getting
                # doubled delimiters (e.g. "[{this | that}]").
                if not optional:
                    syntax_string += '{'
                
                for j, choice in enumerate(choices):
                    if j > 0:
                        syntax_string += ' | '
                    choice_args = _get_choice_args(choice)
                    choice_syntax_string = _get_command_args_syntax_help_string(command,
                                                                                is_no_command,
                                                                                choice_args)
                    syntax_string += choice_syntax_string
                    
                if not optional:
                    syntax_string += '}'
            else:
                field = arg.get('field')
                
                tag = arg.get('tag')
                if tag:
                    syntax_string += tag + ' '
                            
                token = arg.get('token')
                if token:
                    syntax_string += token

                if (field != None) and (arg.get('type') != 'boolean'):
                    help_name = arg.get('help-name')
                    if help_name:
                        help_name = '<' + help_name + '>'
                    else:
                        if arg.get('type') == 'enum':
                            values = arg.get('values')
                            if values:
                                if _is_string(values):
                                    values = (values,)
                                help_name = ' | '.join(values)
                                if len(values) > 1:
                                    help_name = '{' + help_name + '}'
                        if not help_name:
                            help_name = '<' + field + '>'
                    syntax_string += help_name
            if optional:
                syntax_string += ']'
                
    return syntax_string


def _get_command_syntax_help_string(command, command_prefix_string):
    try:
        parts = []
        if command_prefix_string:
            parts.append(command_prefix_string)
        command_name = _get_command_title(command)
        parts.append(command_name)
        args = _get_args(command)
        args_syntax_help_string=''
        if args:
            args_syntax_help_string = _get_command_args_syntax_help_string(command, False, args)
            parts.append(args_syntax_help_string)

        name = ''
        if sdnsh.debug or sdnsh.description:
            name = command['self'] + "\n  "

        if is_no_command_supported(command) and command_prefix_string != 'no':
            no_syntax = _get_command_args_syntax_help_string(command, True, args)
            if ' '.join(no_syntax) == ' '.join(args_syntax_help_string):
                command_syntax_help_string = name + '[no] ' +' '.join(parts)
            else:
                return [    
                        name + ' '.join(parts),
                        name + 'no ' + command_name + ' ' + no_syntax
                       ]
        else:
            command_syntax_help_string = name + ' '.join(parts)

    except Exception, e:
        if sdnsh.debug or sdnsh.debug_backtrace:
            traceback.print_exc()
        raise error.CommandDescriptionError(str(e))
    
    return command_syntax_help_string


def _quick_command_syntax(command):
    title = _get_command_title(command)
    args = _get_args(command)
    args_syntax_help_string = _get_command_args_syntax_help_string(command, False, args)
    return ' '.join((title, args_syntax_help_string))


def reformat_line(string, width, indent, recurse):

    result = []
    split_chars = "0123456789"
    depth = 0
    align = 0
    next_mark = False

    if recurse > 8 or indent > width:
        return [string]

    string = ' ' * indent + string

    for c in string:
        if c in '{([<':
            if align == 0:
                align = len(result)
            depth += 1
            result.append(' ')
            next_mark = False
        elif c in '})]>':
            next_mark = False
            result.append(' ')
            depth -= 1
            if depth == 0:
                next_mark = True
        elif c == '|':
            result.append(split_chars[depth])
            next_mark = False
        else:
            if next_mark:
                result.append(split_chars[depth])
            else:
                result.append(' ')
            next_mark = False

    if len(string) > width:
        #split_depth = 0
        r = ''.join(result)
        splits = None
        for s in range(0,9):
            if split_chars[s] in r:
                splits = ''.join(result).split(split_chars[s])
                if len(splits[-1]) > 0:
                    break

        if not splits:
            return [string]

        parts = []
        first = len(splits[0])+1
        f = ' ' * align
        collect = string[0:len(splits[0])+1]
        more = False
        for s in splits[1:]:
            if len(s) == 0:
                continue
            if len(collect) + len(s) > width:
                parts.append(collect)
                collect = f
                more = False
            # the next split can still be too wide
            if len(collect) + len(s) > width:
                reformat = reformat_line(string[first:first+len(s)+1], width, align, recurse+1)
                first += len(s)+1
                parts += reformat
                collect = f
                more = False
            else:
                collect += string[first:first+len(s)+1]
                first += len(s)+1
                more = True
        if more:
            parts.append(collect)
        return parts

    return [string]


def isCommandFeatureActive(command, features):
    if not features:
        return True # this command is usable anywhere
    if features:
        if not _is_list(features):
            features = (features,)
        every_features_enabled = True
        for feature in features:
            if not sdnsh.feature_enabled(feature):
                every_features_enabled = False
                break
        if not every_features_enabled:
            return False
        return True


def is_no_command_supported(command):
    """
    Predicate to provide some indication of whether or not
    the command allows the 'no' prefix
    """
    command_type = command.get('command-type')
    if command_type:
        if command_type in ['display-table','display-rest']:
            return False
    no_supported = command.get('no-supported', True)
    if no_supported == False:
        return False
    return True


class CommandHandler(object):
    
    def __init__(self):
        self.actions = None
        self.parse_errors = set()
        self.prefix = ''
        
    def handle_command_error(self, e):
        pass
    
    def handle_exception(self, e):
        pass
    
    def handle_unspecified_command(self):
        pass
    
    # These are the values to use for the priority argument to
    # handle_parse_error
    UNEXPECTED_ADDITIONAL_ARGUMENTS_PRIORITY = 10
    UNEXPECTED_END_OF_ARGUMENTS_PRIORITY = 20
    UNEXPECTED_TOKEN_PRIORITY = 30
    VALIDATION_ERROR_PRIORITY = 40
    
    def handle_parse_error(self, error_message, word_index,
                                 priority, expected_tokens=None):
        # Need to convert a list to a tuple first to make it hashable so that we
        # can add it to the parse_errors set.
        if isinstance(expected_tokens, list):
            expected_tokens = tuple(expected_tokens)
        self.parse_errors.add((error_message, word_index, priority, expected_tokens))
        
    def handle_incomplete_command(self, arg, arg_scopes, arg_data,
                                        fields, parsed_tag, command):
        pass
    
    def handle_complete_command(self, prefix_matches, scopes, arg_data,
                                      fields, actions, command):
        pass
    
    def handle_command_results(self):
        pass
    
    def handle_first_matched_result(self, command):
        pass

    def handle_matched_result(self, command, result, arg_scopes):
        pass

    def get_default_value(self, arg):
        default = None
        if self.is_no_command:
            default = arg.get('default-for-no')
            if default is None and 'field' in arg:
                default = mi.field_current_obj_type_default_value(arg['field'])
                if default != None:
                    default = str(default)
        if default is None:
            default = arg.get('default')
        return default
    

    def get_matching_commands(self, command_word, is_no_command, command_list):
        """
        Returns the list of matching command candidates from the command list.
        
        To be a candidate a command description must satisfy three criteria:
        
        1) If it specifies a feature (or a list of features), then all of
           those features must be enabled.
        2) Its 'mode' value (possibly a list of modes or a mode pattern)
           must match the current mode.
        3) It's name must begin with the command prefix
        4) 'No' or not 'no', some commands allow 'no', other's dont
        
        Each item in the candidate list is a 2 item tuple where the first item
        is the command description and the second item is a boolean for whether
        the command description was an exact match for the command word
        (i.e. as opposed to a prefix match).
        """
        candidates = []
        current_mode = sdnsh.current_mode()
        command_word_lower = command_word.lower()
        
        try:
            for command in command_list:
                # If this command is tied to a feature then check that the
                # feature is enabled.
                if isCommandFeatureActive(command, command.get('feature')) == False:
                    continue
            
                # Check that the command is enabled for the current mode
                modes = command.get('mode')
                if not modes:
                    raise error.CommandDescriptionError(
                        'Command description must specify a mode', command)
                if not _is_list(modes):
                    modes = (modes,)
                if not _match_current_modes(command, current_mode, modes):
                    continue
                
                # If a 'no' command was requested, verify this command
                # support 'no' (can we tell from the type?)
                if is_no_command:
                    if not is_no_command_supported(command):
                        continue

                # Check that the name matches the command word
                name = command['name']
                if _is_string(name):
                    name = name.lower()
                    if name.startswith(command_word_lower):
                        prefix_match = len(command_word) < len(name)
                        candidates.append((command, prefix_match))
                elif isinstance(name, collections.Mapping):
                    # FIXME: Should support dict-based names that aren't
                    # patterns. Will be useful when we support lists of names
                    # for a command description where the arg_data can be set with
                    # different fields based on which name was matched for the command
                    if 're' not in name:
                        command['name']['re'] = re.compile(name['pattern'])
                    if name['re'].match(command_word):
                        candidates.append((command, True))
                # FIXME: Probably should get rid of the following pattern code
                # and just use the above pattern compilation mechanism.
                # The following won't work when we require the command
                # descriptions to be pure data, e.g. derived from JSON data
                # or something like that.
                elif type(name) == dict and \
                        name['re'].match(command_word):
                    candidates.append((command, False))
                else:
                    raise error.CommandDescriptionError('Command description name '
                        'must be either string, dict, or pattern', command)
                        
        except Exception, _e:
            if sdnsh.debug or sdnsh.debug_backtrace:
                traceback.print_exc()
            raise error.CommandDescriptionError('Missing mode or name:', command)
        
        return candidates
    
    def check_command_type_and_actions(self, current_scope, scopes, actions):
        
        # Push the scopes for the command defaults and the command itself
        # onto the scope stack.
        command_type = current_scope.get('command-type')
        command_defaults = command_type_defaults.get(command_type, {})
        if command_defaults:
            # Replace the command defaults with the new ones
            scopes = scopes[:-1] + [command_defaults]

        # Get the actions for the command
        action_name = 'no-action' if self.is_no_command else 'action'
        current_scope_actions = current_scope.get(action_name)
        if current_scope_actions or command_defaults:
            actions = _lookup_in_scopes(action_name, scopes)
            if actions is None:
                actions = []
            elif not _is_list(actions):
                actions = [actions]

            # Update the actions with the scopes that were in effect at the point
            # we looked up that action. This is so we can re-create the
            # appropriate scopes later when we execute the action.
            actions = [(action, scopes) for action in actions]
        
        return scopes, actions
    
    def parse_arguments(self, args, words, start_word_index, scopes,
                        arg_data, fields, actions, prefix_matches, command):
        """
        Parse the given args from the given command words.
        """

        if len(args) == 0:
            return [[0, [], scopes, arg_data, fields, actions]]

        parse_results = []

        arg = args[0]

        if _is_string(arg):
            arg = {'token': arg}

        remaining_args = args[1:]

        arg_scopes = [arg] + scopes
        arg_parse_results = []

        # Get the attributes we need from the arg
        # FIXME: Should possibly get rid of the 'data' mechanism
        # and handle it via a custom dta handler
        choices = arg.get('choices')
        nested_args = arg.get('args')
        if nested_args:
            # Convert the nested argument list into a choices argument with
            # a single choice, so that we can leverage the code below that
            # handles choices
            if choices:
                raise error.CommandDescriptionError('An argument can\'t have both '
                        '"choices" and "args" attributes', command)
            choices = (nested_args,)

        # Determine whether or not this argument is optional.
        # Default to making arguments optional for no commands, except for if
        # it's a choices argument. In that case it will probably be ambiguous
        # about which fields should be reset to the default values, so we just
        # don't try to handle that.
        optional_name = 'optional-for-no' if self.is_no_command else 'optional'
        #optional_default_value = self.is_no_command
        #optional_name = 'optional'
        optional_default_value = False
        #optional = arg.get(optional_name, optional_default_value)
        # FIXME: Disabling the special handling of optional arguments for no
        # command. That's causing spurious completions to be included. Not sure
        # how to fix that right now. Do we really need the special optional
        # handling anyway? Does Cisco actually support that.
        # For example, being able to use "no ip address" rather than
        # "no ip address 192.168.2.2 255.255.255.0". I haven't actually tried
        # both forms on a Cisco switch to see what it does.
        optional = arg.get(optional_name, optional_default_value)

        # Check to see if this arg overrides either the command type or action
        # Note that we don't want to set the "actions" variable with the
        # updated actions yet until we know that the current argument
        # actually matched against the command words and wasn't an optional
        # argument that was skipped.
        arg_scopes, arg_actions = self.check_command_type_and_actions(
                arg, arg_scopes, actions)

        if choices:
            if not _is_list(choices):
                raise error.CommandDescriptionError('"choices" argument must be a list '
                        'or tuple of argument descriptions from which to choose',
                        command)

            for choice in choices:
                choice_args = _get_choice_args(choice)
                choice_arg_scopes = arg_scopes
                choice_actions = list(arg_actions)
                choice_prefix_matches = list(prefix_matches)
                if isinstance(choice, collections.Mapping):
                    choice_arg_scopes = [choice] + choice_arg_scopes
                    choice_optional = choice.get(optional_name, False)
                    if choice_optional:
                        optional = True
                    choice_arg_scopes, choice_actions = \
                        self.check_command_type_and_actions(
                            choice, choice_arg_scopes, choice_actions)
                choice_arg_data = dict(arg_data)
                choice_fields = list(fields)

                choice_parse_results = self.parse_arguments(choice_args,
                    words, start_word_index, choice_arg_scopes,
                    choice_arg_data, choice_fields, choice_actions,
                    choice_prefix_matches, command)
                for choice_parse_result in choice_parse_results:
                    words_matched = choice_parse_result[0]
                    new_arg_data = choice_parse_result[3]
                    # FIXME: Not sure if the code below is the best way to
                    # handle things, but the idea is that we want to detect
                    # the case where any of the choices in a choice block
                    # is composed of all optional arguments. In that case
                    # the overall choice block thus becomes optional. The
                    # reason we propagate the optional attribute is that if
                    # there are multiple choices that consist entirely of
                    # optional arguments then we'd get mlutiple redundant
                    # matches with exactly the same arg_data and prefix_matches
                    # which would lead to an ambiguous command when we 
                    # process the results at the end. So by not adding a
                    # result for each of those cases and instead just adding
                    # a single result for the overall choice block.
                    # The other thing we need to do is distinguish between
                    # optional args and default args which will both lead to
                    # cases where words_matched == 0. For the default arg
                    # case though we will add the match in the nested call
                    # since it will have changes to the arg_data which are
                    # significant in the processing of the command action.
                    # Since we've already added a result, we don't want to
                    # set the overall choice to be optional or else again
                    # we'll get multiple amibuous results. The way we detect
                    # that case is if the arg_data from the parse_result is
                    # different than the arg_data that was passed in. So
                    # that's why we use the following test.
                    if words_matched == 0 and new_arg_data == arg_data:
                        # FIXME: I don't think this will work correctly
                        # if/when we support default values for args. In that
                        # case the choice may have matched 0 words, but it
                        # may have updated the arg_data with some default
                        # argument values, which we'll if we don't add the
                        # parse_result at this point. Need to think more
                        # about this.
                        optional = True
                    else:
                        arg_parse_results.append(choice_parse_result)
        else:
            token = arg.get('token')
            field = arg.get('field')
            arg_type = arg.get('type')
            tag = arg.get('tag')
            default = self.get_default_value(arg)
        
            tag_prefix_match = None
            parsed_tag = False
            is_match = True
            words_matched = 0
            results = None

            # First try to parse the tag if there is one
            if tag and len(words) > 0:
                word = words[0]
                if tag.lower().startswith(word.lower()):
                    if tag.lower() != word.lower():
                        tag_prefix_match = [start_word_index+words_matched, tag]
                    words_matched += 1
                    parsed_tag = True
                else:
                    self.handle_parse_error("Unexpected argument at \"%s\"" % word,
                            start_word_index, CommandHandler.UNEXPECTED_TOKEN_PRIORITY, tag)
                    is_match = False

            # Handle incomplete argument matching
            if is_match:
                if words_matched < len(words):
                    word = words[words_matched]
                else:
                    self.handle_incomplete_command(arg, arg_scopes,
                            arg_data, fields, parsed_tag, command)
                    if default:
                        word = default
                    else:
                        self.handle_parse_error("Unexpected end of command",
                                start_word_index + words_matched,
                                CommandHandler.UNEXPECTED_END_OF_ARGUMENTS_PRIORITY)
                        is_match = False

            # Handle the argument value
            if is_match:
                if token:
                    if token.lower().startswith(word.lower()):
                        value = True if arg_type == 'boolean' else token
                        results = [(value, token)]
                    else:
                        self.handle_parse_error(
                                "Unexpected argument at \"%s\"" % word,
                                start_word_index + words_matched,
                                CommandHandler.UNEXPECTED_TOKEN_PRIORITY, token)
                        is_match = False
                else:
                    # Check that the argument is valid
                    try:
                        results = validate_argument(arg, word, arg_scopes, command)
                    except error.ArgumentValidationError, e:
                        expected_tokens = e.expected_tokens
                        if expected_tokens:
                            if _is_string(expected_tokens):
                                expected_tokens = (expected_tokens,)
                            self.handle_parse_error(str(e),
                                    start_word_index + words_matched,
                                    CommandHandler.UNEXPECTED_TOKEN_PRIORITY,
                                    expected_tokens)
                        else:
                            self.handle_parse_error(str(e),
                                    start_word_index + words_matched,
                                    CommandHandler.VALIDATION_ERROR_PRIORITY)
                        is_match = False

            if is_match:
                assert results is not None
                assert _is_list(results)
                assert len(results) > 0
                # If we reach here we've successfully matched the word. The word
                # may have come from the commands words or it may have come from
                # the default value for the argument. We only want to bump the
                # words_matched in the former case, which is why we need to check
                # against the length of the words array. Note that we don't want
                # to bump words_matched in the code above where we get it from 
                # the command words, because then the word offset we pass to
                # handle_parse_error would be off by 1 if the validation fails.
                if words_matched < len(words):
                    words_matched += 1
                data = arg.get('data')
                arg_data_handler = _lookup_in_scopes('data-handler', arg_scopes)
                self.handle_first_matched_result(command)

                for result in results:
                    value, match_token = result
                    new_arg_data = dict(arg_data)
                    if data:
                        new_arg_data.update(data)
                    # XXX should the mode passed in here to the handler be
                    # the mode of the command, or the current mode ?
                    # (mode-of-the-command in case its a higher submode push)
                    if arg_data_handler:
                        invocation_scope = {
                            # FIXME: The 'name' attribute is deprecated. Remove once
                            # everything's been converted.
                            'name': field,
                            'field': field,
                            'value': value,
                            'data': new_arg_data,
                            'is-no-command': self.is_no_command,
                            'current-mode-obj-type': sdnsh.get_current_mode_obj_type(),
                            'current-mode-obj-id': sdnsh.get_current_mode_obj()
                        }
                        new_arg_scopes = [invocation_scope] + arg_scopes
                        try:
                            result = _call_proc(arg_data_handler,
                                    argument_data_handler_registry, new_arg_scopes,
                                    command)
                        except Exception, e:
                            # XXX ought to not manage parameter exceptions for _call_proc
                            if sdnsh.debug or sdnsh.debug_backtrace:
                                traceback.print_exc()
                            self.handle_parse_error(str(e),
                                    start_word_index + words_matched,
                                    CommandHandler.VALIDATION_ERROR_PRIORITY)
                            return parse_results
                    elif field is not None:
                        new_arg_data[field] = value

                    self.handle_matched_result(command, result, arg_scopes)

                    # FIXME: Do we still need the separate fields dict?
                    # If so, I don't think this is actually correct, since
                    # we want fields to not necessarily be kept exactly in
                    # sync with arg_data. Need to think about this more.
                    new_fields = new_arg_data.keys()
                    new_prefix_matches = list(prefix_matches)
                    if tag_prefix_match:
                        new_prefix_matches.append(tag_prefix_match)
                    if len(match_token) > len(word):
                        new_prefix_matches.append(
                            [start_word_index+words_matched-1, match_token])
                    arg_parse_results.append([words_matched, new_prefix_matches,
                            arg_scopes, new_arg_data, new_fields, arg_actions])

        if optional:
            arg_parse_results.append([0, prefix_matches, scopes,
                                      arg_data, fields, actions])

        for arg_parse_result in arg_parse_results:
            (words_matched, prefix_matches, arg_scopes, arg_data,
             fields, actions) = arg_parse_result
            remaining_words = words[words_matched:]
            remaining_parse_results = self.parse_arguments(
                    remaining_args, remaining_words,
                    start_word_index + words_matched, scopes, arg_data,
                    fields, actions, prefix_matches, command)
            # The first item in each tuple is the words consumed, but
            # that's relative to the remaining args that we passed to
            # it. For the parse results from this invocation of
            # parse args we also need to include the counts of the args
            # that we've already parsed plus the args that were parsed
            # for the current choice.
            for parse_result in remaining_parse_results:
                parse_result[0] += words_matched
#                parse_prefix_matches = parse_result[1]
#                for match in parse_prefix_matches:
#                    match[0] += words_matched
                parse_result[1] = prefix_matches + parse_result[1]
                parse_results.append(parse_result)

        return parse_results


    def handle_one_command(self, command, prefix_match, words):

        assert words is not None
        assert len(words) > 0
        assert command is not None

        fields = {}
        command_word = words[0]
        command_name = command.get('name')
        arg_data = dict(command.get('data', {}))

        # The first two elements in the scopes are the command and the command
        # defaults. The command defaults will be populated with the real
        # defaults when we call check_command_defaults_and_actions
        scopes = [command, {}]
        actions = []
        prefix_matches = []
        if prefix_match:
            prefix_matches.append([0, command_name])

        if isinstance(command_name, collections.Mapping):
            field = command_name['field']
            if field:
                fields[field] = True
                arg_data[field] = command_word
        elif _is_string(command_name):
            assert command_name.lower().startswith(command_word.lower())
        else:
            raise error.CommandDescriptionError('Command name must be either'
                                          'a string or a dict', command)

        # We've checked that the first word matches what we expect from the
        # command object and now we just care about the remaining words, so
        # we strip off the initial command word. Note that we slice/copy
        # the word list instead of deleting the first word in place, so that
        # we don't affect the word list of the calling function.
        words = words[1:]

        scopes, actions = self.check_command_type_and_actions(command, scopes, actions)

        # Parse the arguments
        args = _get_args(command)
        parse_results = self.parse_arguments(args, words, 1, scopes,
                arg_data, fields, actions, prefix_matches, command)

        if parse_results:
            for parse_result in parse_results:
                words_consumed, prefix_matches, scopes, arg_data, fields, actions = parse_result
                if words_consumed == len(words):
                    self.handle_complete_command(prefix_matches, scopes, arg_data, fields, actions, command)
                else:
                    word = words[words_consumed]
                    self.handle_parse_error(
                            'Unexpected additional arguments at \"%s"' % word,
                            words_consumed+1,
                            CommandHandler.UNEXPECTED_ADDITIONAL_ARGUMENTS_PRIORITY)
                    
        return None

        
    def handle_command(self, words, text=None):

        # Check if it's a no command and, if so, adjust the words
        self.is_no_command = len(words) > 0 and words[0].lower() == 'no'
        if self.is_no_command:
            self.prefix = words[0]
            words = words[1:]

        self.words = words
        self.text = text

        result = None

        try:
            if len(words) > 0:
                command_word = words[0]
                matching_commands = self.get_matching_commands(command_word,
                                                               self.is_no_command,
                                                               command_registry)
                for matching_command in matching_commands:
                    command, prefix_match = matching_command
                    self.handle_one_command(command, prefix_match, words)
            else:
                result = self.handle_unspecified_command()
            result = self.handle_command_results()
        except Exception, e:
            if sdnsh.debug or sdnsh.debug_backtrace:
                traceback.print_exc()
            if isinstance(e, error.CommandError):
                result = self.handle_command_error(e)
            else:
                result = self.handle_exception(e)

        return result


class CommandExecutor(CommandHandler):

    def __init__(self):
        super(CommandExecutor, self).__init__()
        self.matches = []

    #def get_default_attribute_name(self):
    #    return 'default-for-no' if self.is_no_command else 'default'
    
    def handle_unspecified_command(self):
        raise error.CommandInvocationError('No command specified')

    def handle_command_error(self, e):
        result = sdnsh.error_msg(str(e))
        return result

    def handle_exception(self, e):
        if sdnsh.debug or sdnsh.debug_backtrace:
            traceback.print_exc()
        raise e

    def handle_complete_command(self, prefix_matches, scopes, arg_data, fields, actions, command):
        self.matches.append((prefix_matches, scopes, arg_data, fields, actions, command))

    def handle_command_results(self):

        if len(self.matches) == 0:
            # No matching command. We need to run through the parse
            # errors we've collected to try to figure out the best error
            # message to use. The simple heuristic we use is that we'll
            # assume that errors that matched the most command words
            # before hitting a parse error are the most relevant, so
            # those are the ones we'll print. If it's a parse error
            # because it didn't match a literal token (i.e. as opposed
            # to a validation error), then we'll collect all of the
            # valid tokens and print a single error message for all of
            # them. Otherwise we'll print out all of the individual
            # error message. This isn't great, because in a lot of
            # cases there we'll be multiple error messages, which 
            # may be either conflicting or almost identical (identical
            # messages are eliminated because we use a set when we
            # collect the parse errors).
            most_words_matched = -1
            highest_priority = -1
            accumulated_error_messages = []
            accumulated_expected_tokens = []
            for error_message, words_matched, priority, expected_tokens in self.parse_errors:
                if words_matched > most_words_matched:
                    # We found an error that matched more words than
                    # the other errors we've seen so far, so reset
                    # the highest priority, so we'll start looking for
                    # the new highest priority at the current word match level.
                    # the variable we're using to collect the error
                    # messages and expected tokens
                    most_words_matched = words_matched
                    highest_priority = -1
                if words_matched == most_words_matched:
                    if priority > highest_priority:
                        # We found a higher priority error, so clear the
                        # accumulated errors and expected tokens.
                        accumulated_error_messages = []
                        accumulated_expected_tokens = []
                        highest_priority = priority
                    if words_matched == most_words_matched and priority == highest_priority:
                        # Collect the error message and check to see if there's
                        # an expected token and if all of the errors at this level
                        # so far also had expected tokens. If there's any error
                        # that doesn't have an expected token, then we revert
                        # to the mode where we'll print all of the individual
                        # error messages.
                        if expected_tokens is not None:
                            if _is_string(expected_tokens):
                                expected_tokens = [expected_tokens]
                            elif isinstance(expected_tokens, tuple):
                                expected_tokens = list(expected_tokens)
                            accumulated_expected_tokens += expected_tokens
                        else:
                            accumulated_error_messages.append(error_message)
            
            # We've collected the errors and the expected tokens. Now we
            # just need to format the error message to use with the
            # CommandSyntaxError exception.
            error_message = ''
            if accumulated_expected_tokens:
                word = self.words[most_words_matched]
                error_message += 'Unexpected argument \"%s\"; expected ' % word
                expected_tokens = ['"' + expected_token + '"'
                                   for expected_token in accumulated_expected_tokens]
                if len(expected_tokens) > 1:
                    expected_tokens.sort()
                    error_message += 'one of '
                error_message += '(' + ', '.join(expected_tokens) + ')'
            for message in accumulated_error_messages:
                if error_message:
                    error_message += '\n'
                error_message += message
                    
            raise error.CommandSyntaxError(error_message)

        # There may be multiple results. We need to figure out it there's
        # an unambiguous match. The heuristic we use is to iterate over
        # the command words and try to find a single result which has an
        # exact match on the word while all the others are prefix matches.
        # If there are multiple results that are exact matches, then we
        # discard the other results that were prefix matches and continue
        # with the next command word. If at an iteration all of the
        # results are prefix matches for the current word, then we check
        # all of the full tokens that were prefix matched by the command
        # word. If all of the results have the same full token, then we
        # continue. If there are multiple full tokens, then that's
        # indicative of an ambiguous command, so we break out of the loop
        # and raise an ambiguous command exception.
        unambiguous_match = None
        if len(self.matches) > 1:
            narrowed_matches = self.matches
            for i in range(len(self.words)):
                exact_match_matches = []
                prefix_match_matches = []
                prefix_match_full_tokens = {}
                for match in narrowed_matches:
                    prefix_matches = match[0]
                    for prefix_match in prefix_matches:
                        if prefix_match[0] == i:
                            prefix_match_matches.append(match)
                            if type(prefix_match[1]) == dict: # regular expression
                                # similar to _get_command_title()
                                full_token = prefix_match[1]['title']
                            else:
                                full_token = prefix_match[1].lower()
                            prefix_match_full_tokens[full_token] = None
                            break
                    else:
                        exact_match_matches.append(match)
                if len(exact_match_matches) == 1:
                    unambiguous_match = exact_match_matches[0]
                    break
                elif len(exact_match_matches) > 1:
                    narrowed_matches = exact_match_matches
                else:
                    # No exact matches, check to see if the prefix matches
                    # are all prefixes for the same token. If so, then let
                    # the process continue with the next word in the command.
                    # Otherwise it's an ambiguous command.
                    assert len(prefix_match_matches) > 1
                    if len(prefix_match_full_tokens) > 1:
                        break
                    narrowed_matches = prefix_match_matches
            else:
                # If we reach the end of the words without either finding an
                # unambiguous result or the word which was ambiguous then that
                # means that multiple command descriptions (or multiple
                # choices/path through a single command description) had the
                # exact same match tokens, which means that there are
                # conflicting command descriptions, which is a bug in the
                # command descriptions.
                if sdnsh.debug or sdnsh.debug_backtrace or sdnsh.description:
                    for n in narrowed_matches:
                        print _line(), n[1][0]['self']
                raise error.CommandAmbiguousError('Multiple command description match')

            if unambiguous_match is None:
                assert prefix_match_full_tokens is not None
                assert len(prefix_match_full_tokens) > 1
                quoted_full_tokens = ['"' + full_token + '"'
                                      for full_token in prefix_match_full_tokens]
                # pylint: disable=W0631
                raise error.CommandAmbiguousError(
                        'Ambiguous command word "%s"; matches [%s]' %
                        (self.words[i], ', '.join(quoted_full_tokens)))

        else:
            unambiguous_match = self.matches[0]

        prefix_matches, scopes, arg_data, fields, actions, command = unambiguous_match

        # XXX Possibly a better method of deciding when to loook deeper
        # in the scopes would be a good idea.
        if fields == None or len(fields) == 0:
            fields = _lookup_in_scopes('fields', scopes)

        invocation_scope = {
            'data': arg_data,
            'fields': fields,
            'is-no-command': self.is_no_command,
            'current-mode-obj-type': sdnsh.get_current_mode_obj_type(),
            'current-mode-obj-id': sdnsh.get_current_mode_obj()
        }

        #scopes = [invocation_scope] + scopes
        assert actions is not None
        if len(actions) == 0:
            raise error.CommandDescriptionError('No action specified', command)
        #if valid_args:
        #    scopes = valid_args + scopes
        if sdnsh.description:   # debug details
            print "Command Selected: %s" % command['self']

        combined_result = None
        for action in actions:
            action_proc, action_scopes = action
            scopes = [invocation_scope] + action_scopes
            if sdnsh.description:
                print 'Action: ', action_proc
            result = _call_proc(action_proc, action_registry, scopes, command)
            if result is not None:
                combined_result = combined_result + result if combined_result else result

        # Since a successful 'no' command may remove an object,
        # after a no command complete's all its actions, verify
        # the existance of every object in the mode stack.  
        # Pop any nested modes where the object is missing.
        if self.is_no_command:
            # starting at the most nested submode level, determine if the
            # current submode object still exists
            any_removed = False
            for index in reversed(range(len(sdnsh.mode_stack))):
                mode = sdnsh.mode_stack[index]
                if mi.obj_type_in_use_as_related_config_type(mode.get('obj_type')):
                    if sdnsh.description:
                        print 'CONFIG TYPE:', \
                              mode.get('obj_type'), \
                              mi.obj_type_in_use_as_related_config_type(mode.get('obj_type'))
                    continue

                if mode.get('obj_type') and mode.get('obj'):
                    try:
                        sdnsh.get_object_from_store(mode['obj_type'], mode['obj'])
                    except:
                        # pop this item
                        if sdnsh.description:
                            print 'NO LOST OBJECT ', mode.get('obj_type'), mode.get('obj')
                        del sdnsh.mode_stack[index]
                        any_removed = True

            if any_removed:
                sdnsh.warning("submode exited due to deleted object")
                sdnsh.update_prompt()

        return combined_result


class CommandCompleter(CommandHandler):

    def __init__(self, meta_completion = False):
        super(CommandCompleter, self).__init__()
        self.meta_completion = meta_completion
        self.completions = {}

    def get_default_value(self, arg):
        default = arg.get('default')
        if self.is_no_command and default is None and 'field' in arg:
            default = mi.field_current_obj_type_default_value(arg['field'])
            if default != None:
                default = str(None)
        return default

    def complete_help(self, arg_scopes):
        """
        Select the most appropriate localized text to associate with
        the completion-reason for some completion-text
        """
        c_help = _lookup_in_scopes('conpletion-text', arg_scopes)
        if c_help:
            return c_help
        c_help = _lookup_in_scopes('syntax-help', arg_scopes)
        if c_help:
            return c_help
        c_help = _lookup_in_scopes('all-help', arg_scopes)
        if c_help:
            return '!' + c_help
        c_help = _lookup_in_scopes('short-help', arg_scopes)
        if c_help:
            return c_help
        return ' <help missing> %s' %  _lookup_in_scopes('self', arg_scopes)


    def completion_set(self, completions, text, reason):
        """
        In situations where multiple commands match for some completion
        text, the last command-iterated-over will populate the text for
        the completion dictionary.  Use the first character of the 
        completion string for this command to identify when the text
        represents the most "base" variant of the command to display
        for completion help text.

        When the first character of the string is a '!', the printing
        procedures must drop the first character.
        """
        if completions == None:
            completions = {text : reason}
            return
        current_text = completions.get(text)
        # current_text could possibly be None from the lookup
        if current_text != None and current_text[0] == '!':
            return
        completions[text] = reason


    def command_help_text(self, command):
        c_help = command.get('all-help')
        if c_help:
            if c_help[0] != '!':
                return '!' + c_help
            return c_help
        c_help = command.get('short-help')
        return c_help


    def handle_unspecified_command(self):
        candidates = self.get_matching_commands(self.text, self.is_no_command, command_registry)
        for candidate in candidates:
            self.completion_set(self.completions, candidate[0]['name'] + ' ',
                                                  self.command_help_text(candidate[0]))

    def handle_incomplete_command(self, arg, arg_scopes, arg_data, fields,
                                  parsed_tag, command):

        completions = None
        tag = arg.get('tag')
        lower_text = self.text.lower()

        if tag is not None and not parsed_tag:
            if tag.lower().startswith(lower_text):
                if completions == None:
                    completions = {}
                completions[tag + ' '] = self.complete_help(arg_scopes)
        else:
            token = arg.get('token')
            type_name = arg.get('type')
            if type_name == None:
                type_name = arg.get('base-type')
            typedef = typedef_registry.get(type_name) if type_name else None

            # See if this argument is an enumerated value. This can either
            # be by a type/values specified inline in the argument or via
            # an enum typedef. So first we check for those two cases and
            # get the object that contains the values, either the argument
            # itself or the typedef.
            enum_obj = None
            if token is not None:
                if token.lower().startswith(lower_text):
                    if completions == None:
                        completions = {}
                    completions[token + ' '] = self.complete_help(arg_scopes)
            elif type_name == 'enum':
                enum_obj = arg
            elif typedef:
                # Note: This doesn't recursively check up the type hierarchy,
                # but it doesn't seem like a valid use case to have more than
                # one level of inheritance for enumerations, so it should be OK.
                base_type = typedef.get('base-type')
                if base_type == 'enum':
                    enum_obj = typedef

            # If we have an enum object, then get the values and
            # build the completion list
            if enum_obj:
                enum_values = enum_obj.get('values')
                if enum_values:
                    if _is_string(enum_values):
                        enum_values = (enum_values,)
                    # enum_values may be either a array/tuple of strings or
                    # else a dict whose keys are the enum strings. In both
                    # cases we can use the for loop to iterate over the
                    # enum string names to be used for completion.
                    completions = {}
                    for enum_value in enum_values:
                        if enum_value.lower().startswith(lower_text):
                            completions[enum_value + ' '] = \
                                   self.complete_help(arg_scopes)

            if completions == None or len(completions) == 0:
                # Check to see if there's a custom completion proc
                completion_proc = _lookup_in_scopes('completion', arg_scopes)
                if completion_proc:
                    completions = {}
                    curr_obj_type = sdnsh.get_current_mode_obj_type()
                    curr_obj_id   = sdnsh.get_current_mode_obj()
                    command_mode = command.get('mode')
                    if command_mode and command_mode[-1] == '*':
                        command_mode = command_mode[:-1]
                    if command_mode and sdnsh.current_mode() != command_mode:
                        # this is completing a different item on the stack.
                        # XXX needs better api's here.
                        found_in_mode_stack = False
                        for x in sdnsh.mode_stack:
                            if x['mode_name'] == command_mode:
                                found_in_mode_stack = True
                                curr_obj_type = x['obj_type']
                                curr_obj_id   = x['obj']
                                break
                        if not found_in_mode_stack:
                            raise error.CommandDescriptionError(
                                    'Unable to find mode %s' % command_mode, 
                                    command)

                    invocation_scope = {
                        'words': self.words,
                        'text': self.text,
                        'data': arg_data,
                        'completions': completions,
                        'is-no-command': self.is_no_command,
                        'mode' : command_mode,
                        'current-mode-obj-type': curr_obj_type,
                        'current-mode-obj-id': curr_obj_id,
                    }
                    arg_scopes.insert(0, invocation_scope)
                    if sdnsh.description: # for deubugging, print command name
                        print command['self'], completion_proc
                    try:
                        _result = _call_proc(completion_proc, completion_registry,
                                            arg_scopes, command)
                    except TypeError, e:
                        if sdnsh.debug or sdnsh.debug_backtrace:
                            print "Issue: ", e
                            traceback.print_exc()
                        raise error.CommandDescriptionError("Unable to call completion",
                                                      command)

            if (completions == None or len(completions) == 0 or self.meta_completion) and not self.text:
                # TODO: It's not clear in this case how to detect if a
                # partially entered argument can really match this argument,
                # so we (conservatively) only include these completion text
                # strings when there's no partial text for the argument, which
                # is why we have the above check for self.text

                # Note: We want to allow None for the value of completion-text
                # to mean that completion should be disabled for the argument,
                # so we use False for the default value of the get method.
                # Then if the completion-text is not set in the arg the
                # "if not completion_text" checks will be True, so we'll try to
                # set the completion text to the help-name or field value.
                completion_text = arg.get('completion-text', False)
                if completion_text is not None:
                    # FIXME: Probably shouldn't use syntax help here. That
                    # should probably be reserved for printing additional
                    # syntax help for the argument, duh (presumably some longer
                    # descriptive text for the argument).
                    if not completion_text:
                        completion_text = arg.get('help-name')
                    if not completion_text:
                        completion_text = tag if tag else arg.get('field')
                    syntax_help = arg.get('syntax-help')
                    if syntax_help == None:
                        if sdnsh.description:
                            syntax_help = 'Add syntax-help %s at %s' % (command['self'],
                                                                        completion_text)
                        # exclude enum, since the values will included as text
                        if typedef and type_name != 'enum':
                            syntax_help = typedef.get('help-name')
                            if syntax_help == None:
                                syntax_help = ' <' + type_name + '> ' # add delim

                    if completion_text and syntax_help:
                        #and completion_text.startswith(lower_text):
                        completion_text = ' <' + completion_text + '> ' # add_delim
                        if completions == None:
                            completions = {}
                        # The use of tuple here as he index provides a method
                        # of identifying the completion values which are 
                        # 'meta-information', and not to be completed against
                        completions[(completion_text, syntax_help)] = syntax_help

        if completions and len(completions) > 0:
            for (completion, text) in completions.items():
                self.completion_set(self.completions, completion, text)

        return None

    def handle_complete_command(self, prefix_matches, scopes,
                                      arg_data, fields, actions, command):
        if not self.text:
            # complete command, intentionally use the short-help associated
            # with the top level of this command.
            # The use of tuple here as he index provides a method
            # of identifying the completion values which are 
            # 'meta-information', and not to be completed against
            self.completions[(' <cr> ', '<cr>' )] = command.get('short-help')

    def handle_command_error(self, e):
        # For completion ignore all errors exception for ones that are
        # indicative of an error in the command description, which will
        # (hopefully) be found by the developer while debugging the
        # command description.
        if isinstance(e, error.CommandDescriptionError):
            sdnsh.print_completion_help(sdnsh.error_msg(str(e)))
            if sdnsh.debug or sdnsh.debug_backtrace:
                traceback.print_exc()


    def print_completions(self, completions):
        """
        Print the completions ourselves in sorted multiple column format.
        We use this when the completions are are pseudo/help completions
        that aren't real tokens that we want readline to use for
        completions.
        """
        meta_completions = [completion[0] if isinstance(completion, tuple)
                            else completion for completion in completions]
        meta_completions.sort()

        sdnsh.print_completion_help(sdnsh.choices_text_builder(meta_completions))


    def handle_command_results(self):
        if self.meta_completion == True:
            # two-column display mode
            sdnsh.completion_print = False
            if len(self.completions):
                max_token_len = 0
                for completion in self.completions:
                    item = completion
                    if isinstance(completion, tuple):
                        item = completion[0]
                    max_token_len = max(max_token_len, len(item))
            text = []
            for completion in sorted(self.completions.keys(),
                                     cmp=utif.completion_trailing_integer_cmp):
                help = self.completions[completion]
                if help and help[0] == '!':
                    help = help[1:]

                left_side = completion
                if isinstance(completion, tuple):
                    left_side = completion[0]
                text.append('%-*s %s\n' % (max_token_len, left_side.strip(), help.strip()))
            sdnsh.print_completion_help(''.join(text))

        completions = list(self.completions)
        if len(completions) == 1:
            completion = completions[0]
            if isinstance(completion, tuple):
                help_text = completion[1]
                if not help_text:
                    help_text = completion[0]
                if self.meta_completion == False:
                    sdnsh.print_completion_help(help_text)
                completions = None
        elif len(completions) > 1:
            # Some of the completion results are really pseudo completions
            # that are meant to be more like a help prompt for the user rather
            # than an actual token that should be used for completion by readline
            # If all of the completions are like that and we return them to
            # readline, then it will actually complete the leading prefix, which
            # will be at least the leading '<'. To avoid that situation we print
            # out the completions ourselves using print_completion_help and then
            # return None for the completions.
            all_pseudo_completions = True
            for completion in completions:
                name = completion[0] if isinstance(completion, tuple) else completion
                if not name.endswith('> ') and not name.endswith('>'):
                    all_pseudo_completions = False
                    break
            if all_pseudo_completions:
                if self.meta_completion == False:
                    self.print_completions(completions)
                completions = None
            else:
                completions = [completion[0] if isinstance(completion, tuple)
                               else completion for completion in completions]

        return completions


def do_command_completion(words, text):
    completer = CommandCompleter()
    completions = completer.handle_command(words, text)
    return completions


def do_command_completion_help(words, text):
    completer = CommandCompleter(meta_completion = True)
    completions = completer.handle_command(words, text)
    return completions


def _canonicalize_validation_result(result):
    """
    The canonical (i.e. most general) representation of a validation
    result is a list of 2-item tuples where the first item in the tuple
    is the validated value that should be used to update the arg_data
    and the second item is the actual token that was matched, which may
    not be the same as the word in the command. For example, in the
    results for an enum the word from the command may be a prefix for
    one of the allowed value for the enum. In that case the first item
    would be the return value for the enum value and the second item
    will be the full enum value, i.e. not the prefix that was typed
    in the command.

    But there are a couple of simpler representations of the validation
    results. First, the result can be a simple scalar validated value.
    This is converted to the canonical representation by turning it
    into [(<value>, <value>)]. Second, the input result can be just
    the 2-item tuple. To canonicalize that we just enclose it in a
    list: [<tuple-result>].
    """
    if result is None:
        result = []
    elif _is_list(result):
        if len(result) > 0 and not _is_list(result[0]):
            result = [result]
    else:
        matching_token = result if _is_string(result) else str(result)
        result = [(result, matching_token)]

    return result


def _combine_validation_results(result1, result2, typedef, value):
    if result1 is not None:
        result1 = _canonicalize_validation_result(result1)
    if result2 is not None:
        result2 = _canonicalize_validation_result(result2)

    if result1 is not None:
        if result2 is not None:
            result = [r for r in result1 if r in result2]
            if len(result) == 0:
                _raise_argument_validation_exception(typedef, value, "no match")
            #elif len(result) == 1:
            #    result = result[0]
            return result
        else:
            return result1
    elif result2 is not None:
        return result2
    else:
        return [(value, value)]


def validate_type(type_name, value, scopes, command):
    """
    Validate that the specified value matches the validation in the
    specified type definition and any inherited type definitions
    """
    # Look up the type definition and perform any validation specified there
    typedef = typedef_registry.get(type_name)
    if not typedef:
        raise error.CommandDescriptionError('Unknown type: %s' % type_name)

    type_result = None
    validation_result = None

    # If it's a subtype, validate the base type first
    base_type_name = typedef.get('base-type')
    if base_type_name:
        type_result = validate_type(base_type_name, value, scopes, command)

    # FIXME: Seems like we shouldn't be handling obj-type here, since
    # that's just an attribute that's specific to certain validation
    # procs. Shouldn't that value already be available in the scopes
    # to be able to pass to the proc?
    obj_type = _lookup_in_scopes('obj-type', scopes)

    # Now validate the subtype
    # _call_proc requires that the first scope be an invocation scope that
    # it possibly modifies by settings the 'scopes' variable, so we need
    # to include an empty invocation scope here
    invocation_scope = {}
    parameter_scope = {'typedef': typedef, 'value': value}
    if obj_type:
        parameter_scope['obj_type'] = obj_type
    scopes = [invocation_scope, parameter_scope, typedef]
    while base_type_name is not None:
        base_typedef = typedef_registry.get(base_type_name)
        if not base_typedef:
            raise error.CommandDescriptionError('Invalid base-type name: %s' % base_type_name)
        scopes.append(base_typedef)
        base_type_name = base_typedef.get('base-type')

    #command_type = command.get('command-type')
    #command_defaults = command_type_defaults.get(command_type, {})

    #scopes.append([command, command_defaults])
    validation = _lookup_in_scopes('validation', scopes)
    if validation:
        validation_result = _call_proc(validation, validation_registry,
                                       scopes, command)
        if validation_result is None:
            validation_result = value

    result = _combine_validation_results(type_result, validation_result,
                                         typedef, value)

    return result


def validate_argument(arg, value, scopes, command):

    type_result = None
    validation_result = None

    # Validate based on the type of the argument
    type_name = arg.get('type')
    if type_name:
        if type_name == 'enum':
            #values = arg.get('values')
            #result = validate_enum({'values': values}, value)
            type_result = c_validations.validate_enum(arg, value)
        else:
            type_result = validate_type(type_name, value, scopes, command)
    else:
        # If the argument description specifies a base-type value, then
        # we treat it as an anonymous typedef.
        # FIXME: Should probably be able to refactor this code a bit to
        # reduce code duplication.
        base_type_name = arg.get('base-type')
        if base_type_name:
            base_typedef = typedef_registry.get(base_type_name)
            if base_typedef:
                validation = _lookup_typedef_value(base_typedef, 'validation')
                if validation:
                    scopes = [{'typedef': arg, 'value': value}] + scopes
                    type_result = _call_proc(validation, validation_registry,
                                             scopes, command)
                    if type_result is None:
                        type_result = value

    # Perform any custom validate proc in the argument
    validation = arg.get('validation')
    if validation:
        # Include 'typedef' in the local scope so that we can use
        # typedef validation functions inline for arguments (as opposed to
        # requiring a typedef definition).
        scopes = [{'arg': arg, 'typedef': arg, 'value': value}] + scopes
        validation_result = _call_proc(validation, validation_registry,
                                       scopes, command)
        if validation_result is None:
            validation_result = value

    result = _combine_validation_results(type_result, validation_result,
                                         arg, value)

    return result

#
# CommandSyntaxer: Collect all the commands which "prefix match" the words
#
class CommandSyntaxer(CommandHandler):
    
    def __init__(self, header):
        super(CommandSyntaxer, self).__init__()
        if header is None:
            header = ''
        self.header = header
        self.is_no_command = False
        self.commands = []

    def add_known_command(self, command):
        self.commands.append(command)

    def handle_incomplete_command(self, arg, arg_scopes, arg_data, fields, parsed_tag, command):
        if command not in self.commands:
            self.commands.append(command)

    def handle_complete_command(self, prefix_matches, scopes, arg_data, fields, actions, command):
        if command not in self.commands:
            self.commands.append(command)
    def handle_command_results(self):
        if len(self.commands) == 0:
            return 'No applicable command: %s' % ' '.join(self.words)
        
        help_lines = ['']

        if self.header:
            help_lines.append(self.header)
            help_lines.append('')

        help_strings = []
        for command in self.commands:
            command_help_string = _get_command_syntax_help_string(
                    command, self.prefix)
            if type(command_help_string) == list:
                help_strings += command_help_string
            else:
                help_strings.append(command_help_string)
            
        (term_cols, term_rows) = sdnsh.pp.get_terminal_size()
        for h in sorted(help_strings):
            # use 75% of the width, since reformat_line isn't perfect.
            smaller_term_cols = (term_cols * 3) / 4
            if len(h) > smaller_term_cols:
                help_lines += reformat_line(h, smaller_term_cols, 0, 0)
            else:
                help_lines.append(h)
        
        if len(self.commands) == 1:
            short_help = None
            if self.is_no_command:
                #
                # Cobble together a help string for a no command, build
                # a default value for subcommand's, possibly other command
                # descriptions
                #
                short_help = self.commands[0].get('no-help')
                if not short_help:
                    command_type = self.commands[0].get('command-type')
                    action = self.commands[0].get('action')
                    if ((command_type and command_type == 'config-submode') or
                            (action and action == 'push-mode-stack')):
                        mode_name = self.commands[0].get('name')
                        short_help = 'Remove %s configuration' % mode_name
            else:
                short_help = self.commands[0].get('short-help')
            
            if short_help:
                #help_lines.append('')
                help_lines.append(short_help)
        
        return '\n'.join(help_lines)


def get_command_syntax_help(words, header=None):
    handler = CommandSyntaxer(header)
    # Hack to make "help no" work
    if len(words) == 1 and words[0] == 'no':
        words =  words + ['']
    result = handler.handle_command(words)
    return result


def get_command_short_help_for_pattern(word):
    # try to find the command based on the pattern.
    for command in command_registry:
        if type(command['name']) == dict:
            if command['name']['title'] == word:
                return command.get('short-help')
    

def get_command_short_help(word):
    handler = CommandHandler()
    matching_commands = handler.get_matching_commands(word, False,
                                                      command_registry)
    if len(matching_commands) == 0:
        return get_command_short_help_for_pattern(word)

    if len(matching_commands) >= 1:
        # This will only work once all commands have command descriptions.
        #matching_commands[0][0]['self'], _exact_mode_match(current_mode,
        #(matching_commands[0][0]).get('mode'))
        return (matching_commands[0][0]).get('short-help')
    return None


def dumb_formatter(out, text, left, right = None):
    """
    Build output lines from the passed in text.  If the
    text has leading spaces, then leave the spaces intact
    (starting at the indent).
    """
    if right == None:
        (right, line_length) = sdnsh.pp.get_terminal_size()
        if right - 20 > left: # XXX needs work
            right = right - 20
        right = min(right, 120)

    left_indent = ' ' * left
    out_len = left
    out_line = left_indent

    for line in text.split('\n'):
        if len(line) == 0:
            if out_len > left:
                out.append(out_line)
            out_len = left
            out_line = left_indent
            out.append('')
        elif line[0] == ' ':  # leading spaces
            if out_len > left:
                out.append(out_line)
            out_len = left
            out_line = left_indent
            out.append( left_indent + line )
        else: # text formatting

            for word in line.split():
                sep = ' '
                if word.endswith('.'):
                    sep = '  '
                if len(word) + out_len + len(sep) > right:
                    if out_len > left:
                        out.append(out_line)
                    out_len = left + len(word) + len(sep)
                    out_line = left_indent + word + sep
                else:
                    out_line += word + sep
                    out_len += len(sep) + len(word)
    if out_len > left:
        out.append(out_line)


#
# Documentations: Collect all the commands which "prefix match" the words
#
class CommandDocumentor(CommandHandler):
    
    def __init__(self, header, format = None):
        super(CommandDocumentor, self).__init__()
        if header is None:
            header = ''
        self.header = header
        self.commands = []
        self.docs = {}
        self.docs_stack = []
        self.is_no_command = False
        self.words = None
        self.format = format
        (self.term_cols, self.term_rows) = sdnsh.pp.get_terminal_size()


    def add_docs(self, name, value):
        if name in sdnsh.reserved_words and value == None:
            value = 'reserved|%s' % name
        if name in self.docs:
            if value != self.docs[name]:
                if value != None and None in self.docs[name] and \
                  len(self.docs[name]) == 1 :
                    self.docs[name] = [value]
                elif value != None and value not in self.docs[name]:
                    self.docs[name].append(value)
            # if the value is already there, don't do nuttn
        else:
            self.docs[name] = [value]

    def add_dict_doc(self, arg):
        if 'choices' in arg:
            self.add_tuple_docs(arg['choices'])
            return
        if 'args' in arg:
            args_arg = arg['args']
            if type(args_arg) == tuple:
                self.add_tuple_docs(args_arg)
            elif type(args_arg) == dict:
                self.add_dict_doc(args_arg)
            else:
                print 'add_dict_doc ', type(args_arg), args_arg
            return

        tag = arg.get('tag')
        if 'field' in arg:
            if tag:
                name = '%s <%s>' % (tag, arg['field'])
            else:
                name = arg['field']
        elif 'token' in arg:
            name = arg['token']

        doc = arg.get('doc')

        if doc and 'type' in arg:
            values = arg.get('values')
            if arg['type'] == 'enum' and values:
                if isinstance(values,str):
                    self.add_docs(values,doc)
                else:
                    for v in values:
                        if doc and doc[-1] == '+':
                            self.add_docs(v, doc[:-1] + v)
                        else:
                            self.add_docs(v, doc)
                return
                
        if doc:
            self.add_docs(name, doc)


    def add_tuple_docs(self, tuple_arg):
        for t in tuple_arg:
            if type(t) == tuple:
                self.add_tuple_docs(t)
            elif type(t) == dict:
                self.add_dict_doc(t)

    def add_command(self, command):
        self.commands.append(command)
        args = command.get('args')
        self.add_tuple_docs((args,))

    def handle_first_matched_result(self, command):
        self.docs_stack.append({})

    def handle_matched_result(self, command, result, arg_scopes):
        matched_doc =  _lookup_in_scopes('doc', arg_scopes)
        if matched_doc and matched_doc[-1] == '+':
            item = result
            if type(item) == tuple:
                item = item[0]
            matched_doc = matched_doc[:-1] + item
        self.docs_stack[-1][command['self']] = (result, matched_doc)

    def handle_incomplete_command(self, arg, arg_scopes, arg_data,
                                        fields, parsed_tag, command):
        tag = arg.get('tag')
        doc = arg.get('doc')
        doc_any = doc
        if doc_any == None:
            doc_any = _lookup_in_scopes('doc', arg_scopes)
        # note: doc may still be None

        token = arg.get('token')

        type_name = arg.get('type')
        if type_name == None:
            type_name = arg.get('base-type')
        typedef = typedef_registry.get(type_name) if type_name else None

        field = arg.get('field')
        help_name = arg.get('help-name', field)

        field_doc = arg.get('completion-text', False)
        if field_doc == None:
            field_doc = arg.get('help-name')
        if field_doc == None:
            field_doc = arg.get('syntax-help')

        def manage_enum(arg, doc):
            enum_values = arg.get('values')
            if _is_string(enum_values):
                enum_values = [enum_values]
            for enum in enum_values:
                if doc and doc[-1] == '+':
                    self.add_docs(enum, doc[:-1] + enum)
                else:
                    self.add_docs(enum, doc)

        if token != None:  # token is a loner.
            self.add_docs(token.lower(), doc_any)
        elif tag and field and field_doc:
            self.add_docs(tag + ' <' + help_name + '>', doc if doc else field_doc)
        elif field and typedef and typedef.get('name') == 'enum':
            manage_enum(arg, doc)
        elif field and doc:
            self.add_docs(help_name, doc)
        elif tag and field and typedef: 
            self.add_docs(tag + ' <' + help_name + '>', 'types|' +  typedef.get('name'))
        elif field and field_doc:
            self.add_docs(help_name, field_doc)
        elif typedef:
            typedef_name = typedef.get('name').lower()
            if typedef_name != 'enum':
                if field:
                    # magic reference to doc:  types/<typename>
                    self.add_docs(field, 'types|' + typedef.get('name'))
                else:
                    self.add_docs(typedef.get('name').lower(), doc)
            else:
                manage_enum(arg, doc)

        if command not in self.commands:
            self.commands.append(command)


    def handle_complete_command(self, prefix_matches, scopes, arg_data,
                                      fields, actions, command):
        # only provide help for exact command matches
        if len(prefix_matches):
            return

        if command not in self.commands:
            if sdnsh.description:
                print 'COMPLETE COMMAND DOC',  command['self'], arg_data, prefix_matches
            self.commands.append(command)

    def handle_command_results_wiki(self):
        def handle_plaintext_wiki(inputstr):
            inputstr=inputstr.replace("{","\{")
            inputstr=inputstr.replace("}","\}")
            inputstr=inputstr.replace("[","\[")
            inputstr=inputstr.replace("]","\]")
            inputstr=inputstr.replace("\\\\","<br>")
            inputstr=inputstr.replace("\n\n", "\\@")
            inputstr=inputstr.replace("\n"," ")
            inputstr=inputstr.replace("\\@", "\n\n")
            inputstr=inputstr.replace("<br>", "\n")
            
            return inputstr

        if len(self.commands) == 0:
            if self.words:
                return 'No applicable command: %s' % ' '.join(self.words)
            else:
                return '\nNo command for  ' + self.header
        help_lines=[]
        help_lines.append('')
        last_desc_name = None
        last_syntax_name = None
        # Keep track of paragraphs displayed to prevent repeated
        # display of the same doc tags.
        shown_items = [] 
        for command in self.commands:
            doc_tag = command.get('doc')
            name = _get_command_title(command)
            short_help = command.get('short-help')
            if short_help:
                if self.header:
                    help_lines.append('\nh2. %s Command' % name.capitalize())
                help_lines.append('\nh4. %s' % short_help.capitalize())
                cmdmode=command.get('mode')
                if isinstance(cmdmode,list):
                    cmdmodestr=''
                    for cmdmodemem in cmdmode:
                        if cmdmodemem[-1]=='*':
                            cmdmodemem=cmdmodemem[:-1]
                        cmdmodestr= cmdmodestr +' ' + cmdmodemem
                else:
                    cmdmodestr=cmdmode
                    if cmdmodestr[-1]=='*':
                        cmdmodestr=cmdmodestr[:-1]
                help_lines.append('\n*Command Mode:*          %s mode' % cmdmodestr)
                last_syntax_name = None # display syntax header

            help_strings = []
            command_help_string = _get_command_syntax_help_string(
                                            command, self.prefix)
            help_strings.append(command_help_string)
            for h in sorted(help_strings):
                if isinstance(h,list):
                    h = handle_plaintext_wiki(h[0])
                else:
                    h = handle_plaintext_wiki(h)
                if last_syntax_name != name:
                    help_lines.append('\n*Command Syntax:*        {{%s}}' % h)
                last_syntax_name = name
                last_desc_name = None # print description header

            if doc_tag:
                text = doc.get_text(self, doc_tag)
                if text != '' and doc_tag not in shown_items:
                    shown_items.append(doc_tag)
                    if last_desc_name != name:
                        help_lines.append('\n*Command Description:*')
                        last_desc_name = name
                    text=handle_plaintext_wiki(text)
                    help_lines.append(text)
                    #dumb_formatter(help_lines, text, 3)
                    last_syntax_name = None # print 'Syntax' header
        if len(self.commands) == 1:
            short_help = None
            if self.is_no_command:
                #
                # Cobble together a help string for a no command, build
                # a default value for subcommand's, possibly other command
                # descriptions
                #
                short_help = self.commands[0].get('no-help')
                if not short_help:
                    command_type = self.commands[0].get('command-type')
                    action = self.commands[0].get('action')
                    if ((command_type and command_type == 'config-submode') or
                            (action and action == 'push-mode-stack')):
                        mode_name = self.commands[0].get('name')
                        short_help = 'Remove %s configuration' % mode_name
            else:
                short_help = self.commands[0].get('short-help')
            for last_doc in self.docs_stack:
                if command['self'] in last_doc:
                    (keyword, last_doc) = last_doc[command['self']]
                    text = doc.get_text(self, last_doc)
                    if type(keyword) == tuple:
                        keyword = keyword[0]
                    if sdnsh.description:
                        help_lines.append("\t%s: %s %s" %
                                          (command['self'], keyword, last_doc))
                    if text != '' and last_doc not in shown_items:
                        shown_items.append(last_doc)
                        help_lines.append('\nKeyword %s Description:' %
                                           keyword.capitalize())
                        text=handle_plaintext_wiki(text)
                        help_lines.append(text)
                        #dumb_formatter(help_lines, text, 4)

            if len(self.docs) > 0:
                help_lines.append('\n*Next Keyword Descriptions:*')
                for (doc_name, doc_tags) in self.docs.items():
                    if len(doc_tags) == 1 and doc_tags[0] == None:
                        if sdnsh.description:
                            help_lines.append("\t%s: %s missing doc attribute" %
                                              (command['self'], doc_name))
                        help_lines.append('*  %s:' % doc_name)
                    elif len(doc_tags) == 1 and doc_tags[0].split('|')[0] == 'types' and \
                      not doc_name.startswith(doc_tags[0].split('|')[1]):
                        type_name = doc_tags[0].split('|')[1].capitalize()
                        help_lines.append('  %s: type %s' %
                                            (doc_name, type_name))
                    else:
                        help_lines.append('*  %s:' % doc_name)
                    for doc_tag in doc_tags:
                        if sdnsh.description:
                            help_lines.append("\t%s: %s %s" %
                                              (command['self'], doc_name, doc_tag))
                        text = doc.get_text(self, doc_tag)
                        if text == '':
                            help_lines.append('')
                        if text != '' and doc_tag not in shown_items:
                            if len(doc_tags) > 1 and doc_tag.split('|')[0] == 'types':
                                type_name = doc_tag.split('|')[1].capitalize()
                                help_lines.append('\tType: %s' % type_name)
                            shown_items.append(doc_tag)
                            text=handle_plaintext_wiki(text)
                            help_lines.append(text)
                            #dumb_formatter(help_lines, text, 4)
            doc_example = command.get('doc-example')
            if doc_example:
                text = doc.get_text(self, doc_example)
                if text != '':
                    help_lines.append('\n*Command Examples:*')
                    help_lines.append('{noformat}')
                    help_lines.append(text)
                    #dumb_formatter(help_lines, text, 0)
                    help_lines.append('{noformat}')
        return '\n'.join(help_lines)


    def handle_command_results(self):
        if len(self.commands) == 0:
            if self.words:
                return 'No applicable command: %s' % ' '.join(self.words)
            else:
                return '\nNo command for  ' + self.header

        help_lines = []
        # Deal with too much output
        if len(self.commands) > 10: # 10 is a magic number,
            help_lines.append('Help: Too many commands match (%d), '
                              'Please choose more detail from item: ' %
                              len(self.commands))
            help_lines.append(sdnsh.choices_text_builder(self.docs))
            return '\n'.join(help_lines)


        if self.header:
            help_lines.append(self.header)
            help_lines.append('')

        (term_cols, term_rows) = sdnsh.pp.get_terminal_size()
        last_desc_name = None
        last_syntax_name = None

        # all-help in intended to provide an overall short-help for
        # a collection of similarly syntax'ed commands.  
        all_help = {}
        for command in self.commands:
            if command.get('all-help'):
                all_help[command.get('name')] = command.get('all-help')
        # when len(all_help) == 1, only ONE command collection group
        # is getting described
        if len(all_help) == 1:
            name = all_help.keys()[0]
            help_lines.append('%s Command: %s' %
                              (name.capitalize(), all_help[name].capitalize()))
            all_help = True
        else:
            all_help = False

        # Keep track of paragraphs displayed to prevent repeated
        # display of the same doc tags.
        shown_items = [] 

        for command in self.commands:

            doc_tag = command.get('doc')
            name = _get_command_title(command)
            short_help = command.get('short-help')
            if not all_help and short_help:
                help_lines.append('\n%s Command: %s\n' %
                                  (name.capitalize(), short_help.capitalize()))
                last_syntax_name = None # display syntax header

            help_strings = []
            command_help_string = _get_command_syntax_help_string(
                                            command, self.prefix)

            if type(command_help_string) == list:
                help_strings += command_help_string
            else:
                help_strings.append(command_help_string)
        
            for h in sorted(help_strings):
                if last_syntax_name != name:
                    if len(self.commands) > 1:
                        help_lines.append(' Command Syntax:')
                    else:
                        help_lines.append('%s Command Syntax:' %
                                          name.capitalize())
                    last_syntax_name = name

                # use 75% of the width, since reformat_line isn't perfect.
                smaller_term_cols = (self.term_cols * 3) / 4
                if len(h) > smaller_term_cols:
                    help_lines += reformat_line(h, smaller_term_cols, 2, 0)
                else:
                    help_lines.append('  %s' % h)
                last_desc_name = None # print description header

            if doc_tag:
                text = doc.get_text(self, doc_tag)
                if text != '' and doc_tag not in shown_items:
                    shown_items.append(doc_tag)
                    if last_desc_name != name:
                        if self.format == None:
                            help_lines.append('\n%s Command Description:' %
                                               name.capitalize())
                        elif self.format == 'clidoc':
                            help_lines.append('\n  Description:')
                        last_desc_name = name
                    dumb_formatter(help_lines, text, 4)
                    last_syntax_name = None # print 'Syntax' header
            
            
            if len(self.commands) > 1:
                for last_doc in self.docs_stack:
                    if command['self'] in last_doc:
                        (keyword, cmd_doc) = last_doc[command['self']]
                        text = doc.get_text(self, cmd_doc)
                        if text != '' and cmd_doc not in shown_items:
                            shown_items.append(cmd_doc)
                            if type(keyword) == tuple:
                                keyword = keyword[0]
                            help_lines.append('\nKeyword %s:' % keyword.capitalize())
                            dumb_formatter(help_lines, text, 4)

            doc_example = command.get('doc-example')
            if len(self.commands) > 1 and doc_example:
                text = doc.get_text(self, doc_example)
                if text != '' and doc_example not in shown_items:
                    help_lines.append('Examples:')
                    shown_items.append(doc_example)
                    dumb_formatter(help_lines, text, 4)

        if len(self.commands) == 1:
            short_help = None
            if self.is_no_command:
                #
                # Cobble together a help string for a no command, build
                # a default value for subcommand's, possibly other command
                # descriptions
                #
                short_help = self.commands[0].get('no-help')
                if not short_help:
                    command_type = self.commands[0].get('command-type')
                    action = self.commands[0].get('action')
                    if ((command_type and command_type == 'config-submode') or
                            (action and action == 'push-mode-stack')):
                        mode_name = self.commands[0].get('name')
                        short_help = 'Remove %s configuration' % mode_name
            else:
                short_help = self.commands[0].get('short-help')
            
            for last_doc in self.docs_stack:
                if command['self'] in last_doc:
                    (keyword, last_doc) = last_doc[command['self']]
                    text = doc.get_text(self, last_doc)
                    if type(keyword) == tuple:
                        keyword = keyword[0]
                    if sdnsh.description:
                        help_lines.append("\t%s: %s %s" %
                                          (command['self'], keyword, last_doc))
                    if text != '' and last_doc not in shown_items:
                        shown_items.append(last_doc)
                        help_lines.append('\nKeyword %s Description:' %
                                           keyword.capitalize())
                        dumb_formatter(help_lines, text, 4)

            if len(self.docs) > 0:
                if self.format == None:
                    indent = '  '
                    help_lines.append('\nNext Keyword Descriptions;')
                elif self.format == 'clidoc':
                    indent = '    '

                for (doc_name, doc_tags) in sorted(self.docs.items()):
                    if len(doc_tags) == 1 and doc_tags[0] == None:
                        if sdnsh.description:
                            help_lines.append("\t%s: %s missing doc attribute" %
                                              (command['self'], doc_name))
                        help_lines.append('%s%s:' % (indent, doc_name))
                    elif len(doc_tags) == 1 and doc_tags[0].split('|')[0] == 'types' and \
                      not doc_name.startswith(doc_tags[0].split('|')[1]):
                        type_name = doc_tags[0].split('|')[1].capitalize()
                        help_lines.append('%s%s: type %s' %
                                            (indent, doc_name, type_name))
                    else:
                        help_lines.append('%s%s:' % (indent, doc_name))
                    for doc_tag in doc_tags:
                        if sdnsh.description:
                            help_lines.append("\t%s: %s %s" %
                                              (command['self'], doc_name, doc_tag))
                        text = doc.get_text(self, doc_tag)
                        if text == '':
                            help_lines.append('')
                        if text != '' and doc_tag not in shown_items:
                            if len(doc_tags) > 1 and doc_tag.split('|')[0] == 'types':
                                type_name = doc_tag.split('|')[1].capitalize()
                                help_lines.append('\tType: %s' % type_name)
                            shown_items.append(doc_tag)
                            dumb_formatter(help_lines, text, 8)

            doc_example = command.get('doc-example')
            if doc_example:
                text = doc.get_text(self, doc_example)
                if text != '':
                    help_lines.append('Examples:')
                    dumb_formatter(help_lines, text, 4)
        
        if len(self.commands) > 1 and len(self.docs) > 0:
            if self.format == None:
                help_lines.append('\nNext Keyword Descriptions;')
            for (doc_name, doc_tags) in sorted(self.docs.items()):
                if len(doc_tags) == 1 and doc_tags[0] == None:
                    if sdnsh.description:
                        help_lines.append("\t%s: missing doc attribute" %
                                          command['self'])
                    help_lines.append('  %s:' % doc_name)
                elif len(doc_tags) == 1 and doc_tags[0].split('|')[0] == 'types' and \
                  not doc_name.startswith(doc_tags[0].split('|')[1]):
                    type_name = doc_tags[0].split('|')[1].capitalize()
                    help_lines.append('  %s: type %s' %
                                        (doc_name, type_name))
                else:
                    help_lines.append('  %s:' % doc_name)
                for doc_tag in doc_tags:
                    if doc_tag:
                        if len(doc_tags) > 1 and doc_tag.split('|')[0] == 'types':
                            type_name = doc_tag.split('|')[1].capitalize()
                            help_lines.append('\tType: %s' % type_name)
                        if sdnsh.description:
                            help_lines.append("\t%s: %s %s" %
                                              (command['self'], doc_name, doc_tag))
                        text = doc.get_text(self, doc_tag)
                        if text != '' and doc_tag not in shown_items:
                            shown_items.append(doc_tag)
                            dumb_formatter(help_lines, text, 8)
                        else:
                            help_lines.append('')
                    else:
                        help_lines.append('')

        return '\n'.join(help_lines)


def get_command_doc_tag(word):
    handler = CommandHandler()
    matching_commands = handler.get_matching_commands(word, False,
                                                      command_registry)
    if len(matching_commands) == 1:
        return (matching_commands[0][0]).get('doc')
    if len(matching_commands) > 1:
        # error? retur value for multiple commands?
        pass
    return None


def get_command_documentation(words, header=None):
    handler = CommandDocumentor(header)
    # Hack to make "help no" work
    if len(words) == 1 and words[0] == 'no':
        words =  words + ['']
    result = handler.handle_command(words)
    return result


def command_submode_dictionary(modes = None):
    if modes == None:
        modes = sdnsh.command_dict.keys() + sdnsh.command_nested_dict.keys()
    #
    # try to find all submode commands, build a dictionary
    reached_modes = []
    mode_nodes = {}
    mode_entries = []
    for c in command_registry:
        c_type = c.get('command-type')
        if c_type and c_type == 'config-submode':
            from_mode = c.get('mode')
            to_mode = c.get('submode-name')
            reached_modes.append(to_mode)
            if not from_mode in mode_nodes:
                mode_nodes[from_mode] = []
            mode_nodes[from_mode].append((to_mode, _get_command_title(c)))
            mode_entries.append({'mode'    : from_mode,
                                 'submode' : to_mode,
                                 'command' : _get_command_title(c)})
    if sdnsh.description:
        print ', '.join(mode_nodes)
        print mode_nodes
        if [x for x in modes if not x in reached_modes]:
            print 'Missing ', [x for x in modes if not x in reached_modes]

    return mode_entries


def get_clidoc(words):
    if len(words):
        handler = CommandDocumentor(header = None, format = 'clidoc')
        for word in words:
            for c in command_registry:
                if word == c['self']:
                    handler.add_command(c)
        result = handler.handle_command_results()
        return result
    else:
        clidoc = []
        def commands_for_mode(mode):
            clidoc.append('\n\n\n============================= MODE ' + mode + '\n\n')
            for c in command_registry:
                if mode == c['mode']:
                    handler = CommandDocumentor(header = '\n\n\n ---- MODE ' + mode,
                                                format = 'clidoc')
                    handler.add_command(c)
                    clidoc.append(handler.handle_command_results())

        mode = 'login'
        commands_for_mode(mode)

        # select commands by submode.
        mode_entries = command_submode_dictionary()
        for mode_entry in mode_entries:
            mode = mode_entry['submode']
            if mode[-1] == '*':
                mode = mode[:-1]
            if mode == 'config-':
                mode = 'config'

            commands_for_mode(mode)

        return ''.join(clidoc)

def get_cliwiki(words):
    def wiki_special_character_handling(inputstr):
        inputstr=inputstr.replace('{','\{')
        inputstr=inputstr.replace('}','\}')
        inputstr=inputstr.replace('[','\[')
        inputstr=inputstr.replace(']','\]')
        return inputstr
    cliwikifile=open('cliwiki.txt', 'w')
    if not cliwikifile:
        print 'File creation failed \n'
        return
    cliwikifile.write('{toc:printable=true|style=disc|maxLevel=3|minLevel=1|class=bigpink|exclude=[1//2]|type=list|include=.*}')
    #write the introduction part
    introductionfile=open('documentation/en_US/introduction')
    str1=introductionfile.read()
    cliwikifile.write('\n')
    cliwikifile.write(str1)
    str1='\nh1. CLI Commands'
    cliwikifile.write(str1)
    #generate all commands in login/enable mode except show cmds
    mode=['login','enable']
    category=['show']
    for c in command_registry:
        name = c['name']
        if name not in category and c['mode'] in mode:
            category.append(name)
            handler = CommandDocumentor(header = 'login')
            for cc in command_registry:
                if name==cc['name'] and cc['mode'] in mode:
                    handler.add_command(cc)
                    str1 = handler.handle_command_results_wiki()
                    str1= str1+ '\n'
                    cliwikifile.write(str1)
                    handler = CommandDocumentor(header = None)
    #generate all configuration commands: exclude internal commands
    mode=['config', 'config*']
    category=['internal'] #prune out all internal commands
    str1='\nh2. Configuration Commands'
    cliwikifile.write(str1)
    for c in command_registry:
        name = c['name']
        if name not in category and c['mode'] in mode:
            category.append(name)
            submode='config-' + name
            submode1=c.get('submode-name')
            if submode1 is not None:
                if submode1.find(submode)==-1:
                    submode=submode1
            if name=='vns-definition':
                print submode
            str1="%s" % name
            str1=wiki_special_character_handling(str1)
            str1="\nh3. %s Commands " % str1.capitalize()
            cliwikifile.write(str1)
            handler = CommandDocumentor(header = None)
            handler.add_command(c)
            str1 = handler.handle_command_results_wiki()
            str1= str1+ '\n'
            cliwikifile.write(str1)
            for cc in command_registry:
                cmdmode=cc['mode']
                if (isinstance(cmdmode, str)):
                    if (cmdmode.find(submode)!=-1) and (cc['name']!='show'):
                        #special handling: prune vns command from tenant mode
                        if (name != 'tenant') or ((name == 'tenant') and cmdmode.find('config-tenant-vns')==-1 and cmdmode.find('config-tenant-def-vns')==-1):
                            handler = CommandDocumentor(header = None)
                            handler.add_command(cc)
                            str1 = handler.handle_command_results_wiki()
                            str1= str1+ '\n'
                            cliwikifile.write(str1)
    #generate all show commands
    name='show'
    str1='\nh2. Show Commands'
    cliwikifile.write(str1)
    mode=['config-internal'] #prune out all internal commands
    obj_type=[]
    for c in command_registry:
        if c['name']==name and c['mode'] not in mode:
            obj=c.get('obj-type')
            if obj and obj not in obj_type:
                obj_type.append(obj)
                str1="%s" % obj
                str1=wiki_special_character_handling(str1)
                str1="\nh3. Show %s Commands " % obj.capitalize()
                cliwikifile.write(str1)
                for cc in command_registry:
                    if name==cc['name'] and obj==cc.get('obj-type') and cc['mode'] not in mode:
                        handler = CommandDocumentor(header = None)
                        handler.add_command(cc)
                        str1 = handler.handle_command_results_wiki()
                        str1= str1+ '\n'
                        cliwikifile.write(str1)

    cliwikifile.close()
    print 'CLI reference wiki markup file is generated successfully at: cli/cliwiki.txt.\n '

#
# Lint: Verify requested commands
#
class CommandLint(CommandHandler):
    
    def __init__(self):
        super(CommandLint, self).__init__()
        self.commands = []

    def handle_incomplete_command(self, arg, arg_scopes, arg_data, fields, parsed_tag, command):
        if command not in self.commands:
            self.commands.append(command)

    def handle_complete_command(self, prefix_matches, scopes, arg_data, fields, actions, command):
        if command not in self.commands:
            self.commands.append(command)
        
    def lint_action_query_table(self, c_self, command, c_scope):
        # look for items which are not parameters
        attrs = ['proc', 'key', 'scoped', 'sort', 'crack', 'obj-type', 'proc']
        for a in c_scope:
            if not a in attrs:
                print '%s: unknown attribute %s for query-table' % (c_self, a)
        req = ['obj-type']
        for r in req:
            if not r in c_scope:
                print '%s: missing required attribute %s for query-table' % (
                    c_self, r)
        if 'obj-type' in c_scope:
            if not mi.obj_type_exists(c_scope['obj-type']):
                print '%s: query-table no such obj-type %s' % (
                        c_self, c_scope['obj-type'])

    def lint_action_query_rest(self, c_self, command, c_scope):
        # look for items which are not parameters
        attrs = ['proc', 'url', 'rest-type', 'key', 'scoped', 'sort', 'append']
        for a in c_scope:
            if not a in attrs:
                print '%s: unknown attribute %s for query-rest' % (c_self, a)
        req = ['url']
        for r in req:
            if not r in c_scope:
                print '%s: missing required attribute %s for query-rest' % (
                    c_self, r)
        if 'append' in c_scope:
            if type(c_scope['append']) != dict:
                print '%s: append parameter in query_rest' \
                      'required to be a dict' % (c_self)

    def lint_action_join_table(self, c_self, command, c_scope):
        # look for items which are not parameters
        attrs = ['proc', 'obj-type', 'key', 'key-value', 'add-field', 'join-field', 'crack']
        for a in c_scope:
            if not a in attrs:
                print '%s: unknown attribute %s for join-table' % (c_self, a)
        req = ['obj-type', 'key', 'join-field']
        for r in req:
            if not r in c_scope:
                print '%s: missing required attribute %s for query-table' % (
                    c_self, r)
        if 'obj-type' in c_scope:
            if not mi.obj_type_exists(c_scope['obj-type']):
                print '%s: join-table no such obj-type %s' % (
                        c_self, c_scope['obj-type'])

    def lint_dict_action(self, c_self, command, c_action, c_scope):
        if not 'proc' in c_action:
            print '%s: "proc" expected for dict in action' % c_self
            return
        action = c_action['proc']
        if not action in action_registry:
            print '%s: action %s unknown' % (c_self, c_action)
        if c_action == 'display-rest':
            if not 'url' in c_action:
                print '%s: missing "url" for display-rest' % c_self
        if action == 'display-table':
            if not 'obj-type' in c_action:
                # could be in c_scope, or other nests.
                print '%s: missing "obj-type" for display-table' % c_self
        if action in ['query-table', 'query-table-append']:
            self.lint_action_query_table(c_self, command, c_action)
        if action in ['query-rest', 'query-table-rest']:
            self.lint_action_query_rest(c_self, command, c_action)
        if action == 'join-table':
            self.lint_action_join_table(c_self, command, c_action)
        if action == 'legacy-cli':
            print '%s: LEGACY-CLI, consider reimplementation' % c_self

    def lint_action(self, c_self, command, c_action, c_type, c_scope):
        if type(c_action) == tuple:
            for t in c_action:
                if type(t) == list:
                    print '%s: LIST nost supported as a member of actions' % c_self
                elif type(t) == dict:
                    self.lint_dict_action(c_self, command, t, c_scope)
                    
        if type(c_action) == dict:
            self.lint_dict_action(c_self, command, c_action, c_scope)
        if type(c_action) == str:
            if not c_action in action_registry:
                print '%s: action %s unknown' % (c_self, c_action)
            if c_action == 'display-rest':
                if not 'url' in c_scope:
                    print '%s: missing "url" for display-rest' % c_self
            if c_action in ['query-table', 'query-table-append']:
                self.lint_action_query_table(c_self, command, c_scope)
            if c_action in ['query-rest', 'query-table-rest']:
                self.lint_action_query_rest(c_self, command, c_scope)
            if c_action == 'join-table':
                self.lint_action_join_table(c_self, command, c_scope)
            if c_action == 'legacy-cli':
                print '%s: LEGACY-CLI, consider reimplementation' % c_self

    def lint_choice(self, c_self, command, c_choice, c_type):
        for choice in c_choice:
            if type(choice) == tuple:
                # in order collection of terms, these aren't choices, but 
                # here e can treat them that way
                for t in choice:
                    if type(t) == list:
                        print '%s: LIST nost supported as a member of choices' % c_self
                    elif type(t) == dict:
                        self.lint_choice(c_self, command, (t,), c_type)
                    elif type(t) == str:
                        # token, ought to validate char's in token
                        pass
                    else:
                        print '%s: bad element type -> %s' % (c_self, type(t))

            if 'command-type' in choice:
                print '%s: CHOICE contains "command-type", only' \
                    ' intended to be used at top level' % c_self
                if not choice['command-type'] in command_type_defaults:
                    print '%s: missing command-type %s' % (c_self, choice['command-type'])
            if 'action' in choice:
                self.lint_action(c_self, command, choice['action'], c_type, choice)
            if 'choices' in choice:
                self.lint_choice(c_self, command, choice['choices'], c_type)

    def lint_command(self,command):
        c_self = command.get('self', 'UNKNOWN SELF NANE')
        print command['self']

        c_name = command.get('name', None)
        if c_name == None:
            print '%s: no name defined' % c_self

        c_mode = command.get('mode', None)
        if c_mode == None:
            print '%s: no submode defined' % c_self

        c_short_help = command.get('short-help', None)
        if c_short_help == None:
            print '%s: no short-help defined' % c_self

        c_obj_type = command.get('obj-type', None)

        c_current_mode_obj_id = command.get('current-mode-obj-id', None)
        if 'current-mode-obj-id' in command:
            if c_current_mode_obj_id == None:
                print '%s: "current-mode-obj-id" not needed at top level' % c_self

        c_parent_id = command.get('parent-id', None)
        if 'parent-id' in command:
            if c_parent_id == None:
                print '%s: "parent-id" not needed at top level' % c_self

        c_type = command.get('command-type', None)

        c_args = command.get('args', None)
        if c_args == None:
            print '%s: no args defined' % c_args

        c_action = command.get('action', None)

        if c_action or c_type:
            self.lint_action(c_self, command, c_action, c_type, command)

        if type(c_args) == dict:
            if 'action' in c_args:
                self.lint_action(c_self, command, c_args['action'], c_type, c_args)
            if 'choices' in c_args:
                self.lint_choice(c_self, command, c_args['choices'], c_type)
            if 'choice' in c_args:
                print '%s: "choices" not "choice"' % c_self
        elif type(c_args) == tuple or type(c_args) == list:
            for a in c_args:
                if 'action' in a:
                    self.lint_action(c_self, command, a['action'], c_type, a)
                if 'choices' in a:
                    self.lint_choice(c_self, command, a['choices'], c_type)
                if 'choice' in a:
                    print '%s: "choices" not "choice"' % c_self
                if 'field' in a:
                    if c_obj_type:
                        if not mi.obj_type_has_field(c_obj_type, a['field']):
                            print '%s: %s MISSING FIELD %s' % (c_self,
                                    c_obj_type, a['field'])


    def handle_command_results(self):
        if len(self.commands) == 0:
            return 'No applicable command: %s' % ' '.join(self.words)
        
        for command in self.commands:
            self.lint_command(command)

        return '--'


def lint_command(words):
    lint = CommandLint()
    if len(words) == 0:
        for command in command_registry:
            lint.lint_command(command)
    else:
        for command in command_registry:
            if command['self'] in words:
                lint.lint_command(command)
    return ''


class CommandPermutor(CommandHandler):

    def __init__(self, qualify = False):
        """
        @param qualify Generate qualify version of the permutations
        """
        super(CommandPermutor, self).__init__()
        self.commands = []
        self.collect = []
        self.qualify = qualify

        self.obj_type = None
        self.obj_type_other = None


    def collect_complete_command(self, line):
        if self.qualify:
            # collect together parts, when a token appears,
            # replace the token with a procedure call.
            if len(line) == 0:
                return
            new_line = ['"']
            quoted = True
            if line[0][0] == '+': # unusual
                new_line = ['%s ' % line[0][1:]]
                line = line[1:]
                quoted = False
            for item in line:
                if quoted:
                    if item[0] == '+':
                        new_line.append('" + %s ' % item[1:])
                        quoted = False
                    else:
                        new_line.append('%s ' % item)
                else:
                    if item[0] == '+':
                        new_line.append(' + " " + %s ' % item[1:])
                        quoted = False
                    else:
                        new_line.append('+ " %s ' % item)
                        quoted = True
            if quoted:
                new_line.append('"')

            self.collect.append(''.join(new_line))
        else:
            self.collect.append(' '.join(line))


    def field_to_generator(self, field):
        """
        Convert the field name to a text field.
        When 'qualify' is set, replace the field name
        with a likely procedure to call instead
        """
        if self.qualify:
            # Many of the fields are actually fields in
            # the named obj-type.  Take a shot at seeing if that's
            # the case, and call a sample value collector

            if self.obj_type_other:
                # These are typically an obj_type|field names.
                parts = self.obj_type_other.split('|')
                o_field = field
                if len(parts) > 1:
                    o_field = parts[1]
                # by using obj_type_has_model() instead of
                # obj_type_exists(), 'curl(1)' can be used in
                # the testing to find sample objects.
                if mi.obj_type_has_model(parts[0]):
                    sample_obj_type = 'sample_obj_type'
                    if self.is_no_command:
                        sample_obj_type = 'sample_no_obj_type'
                    if mi.obj_type_has_field(parts[0], o_field):
                        return '+id.%s("%s", "%s")' \
                                % (sample_obj_type, parts[0], o_field)

            python_field = field.replace("-", "_")
            if self.obj_type:
                sample_obj_type = 'sample_obj_type'
                if self.is_no_command:
                    sample_obj_type = 'sample_no_obj_type'
                if mi.obj_type_has_field(self.obj_type, field):
                    return '+id.%s("%s", "%s")' \
                            % (sample_obj_type, self.obj_type, field)
                else:
                    return '+id.%s("%s")' % (python_field, self.obj_type)
            else:
                return '+id.%s()' % python_field

        else:
            return "<%s>" % field
        

    def permute_item(self, item, line, next_args):
        """
        Deals with a single dictionary item.  This can be:
        a choice, an arg, a token, or a field.
        """

        if type(item) == str:
            line.append(item)
            self.permute_in_order(next_args[0], line, next_args[1:])
            return

        if self.is_no_command:
            optional = item.get('optional-for-no', False)
        else:
            optional = item.get('optional', False)


        skip = item.get('permute')      # XXX permute vs qualify skip?

        if skip != 'skip' and optional:
            if len(next_args):
                self.permute_in_order(None, line, next_args)
            else:
                self.collect_complete_command(line)

        choices = item.get('choices')
        if choices:
            self.permute_choices(choices, list(line), next_args)
            return
        
        if skip == 'skip':
            return

        token = item.get('token')
        tag = item.get('tag')
        field = item.get('field')
        args = item.get('args')
        obj_type = item.get('obj-type')
        if obj_type:
            self.obj_type = obj_type

        # must do this after the recursive 'optional' calls.
        self.obj_type_other = item.get('other')

        if args:
            if type(args) == dict:
                self.permute_item(args, line, next_args)
            elif type(args) == tuple:
                self.permute_in_order(args, list(line), next_args)
            else:
                print 'TYPE ARGS ', type(args)
            return
        elif token:
            line.append(token)
        elif field:
            item_type = item.get('type')
            if item_type == None:
                item_type = item.get('base-type')
            if item_type == 'enum':
                values = item.get('values')
                if values:
                    if tag:
                        line.append(tag)
                    # convert the values into a tuple of dicts, 
                    # so that permute_choices can manage it
                    if type(values) == str:
                        values = [values]
                    choices = tuple([{'token' : x} for x in values])
                    self.permute_choices(choices, list(line), next_args)
                    return
            else:
                    
                if tag:
                    line.append(tag)
                line.append(self.field_to_generator(field))

        #
        # fall-through to this common code, which should only be
        # reached when no other recursive descent is performed.

        if len(next_args):
            self.permute_in_order(None, line, next_args)
        else:
            self.collect_complete_command(line)
        

    def permute_choices(self, choices, line, next_args):
        """
        When a 'choices': is reached, enumerate all the choices,
        and continue forward.  
        """
        for pick in choices:
            if type(pick) == dict:
                new_line = list(line)
                self.permute_item(pick, new_line, next_args)
                if next_args == None:
                    self.collect_complete_command(line)
            elif type(pick) == tuple:
                self.permute_in_order(pick, list(line), next_args)
            else:
                print "CHOICE? ", type(pick)


    def permute_in_order(self, items, line, next_args):
        if items == None and len(next_args):
            items = next_args
            next_args = []
        if len(items) == 0:
            # done
            self.collect_complete_command(line)
            return
        # see if the item is optional, if so then don't add the item,
        # and go to the next item in a recursive call

        while len(items) and type(items[0]) == str:
            line.append(items[0])
            items = items[1:]

        if len(items):
            item = items[0]
                
            if len(items) > 1:
                if len(next_args) == 0:
                    next_args = items[1:]
                else:
                    if type(items[1:]) == tuple:
                        next_args = list(items[1:]) + next_args
                    else:
                        next_args = items[1:] + next_args
            else:
                next_args = []

            token = False
            if type(item) == tuple:
                self.permute_in_order(item, line, list(next_args))
            elif type(item) == dict:
                self.permute_item(item, list(line), list(next_args))
            else:
                print 'permute_in_order: need type', type(item)
        else:
            self.collect_complete_command(line)

        return

    def permute_command_common(self, command):
        args = command.get('args')
        if args == None:
            if sdnsh.description:
                print 'PERMUTE: missing args for %s' % command['self']
            return
        name = _get_command_title(command)

        obj_type = command.get('obj-type')
        if obj_type:
            self.obj_type = obj_type

        if type(command.get('name')) == dict:
            if self.qualify:
                name = self.field_to_generator(command['name']['field'])

        if self.is_no_command:
            line = ['no', name]
        else:
            line = [name]

        if type(args) == tuple:
            self.permute_in_order(args, line, [])
        elif type(args) == dict:
            self.permute_item(args, line, [])
        else:
            print 'PC ', type(args)

        return '\n'.join(self.collect)


    def permute_command(self, command):
        print 'PERMUTE', command['self'], is_no_command_supported(command)

        self.is_no_command = False
        result = self.permute_command_common(command)

        if is_no_command_supported(command):
            # Now add the command for 'no'
            self.is_no_command = True
            result = self.permute_command_common(command)

        return result


    def handle_command_results(self):
        collections = []
        for command in self.commands:
            collections.append(self.permute_command(command))
        return '\n'.join(collections)


    def handle_command(self, words):
        for word in words:
            for c in command_registry:
                if word == c['self']:
                    return self.permute_command(c)


def permute_single_submode(submode_command, qualify):
    """
    Permute command for a submode, takes the command, finds
    all the related submodesmodes, permutes all the commands
    in the submode, then a recursive call to any submodes
    found within this one.

    @param submode_command command dictionary of the submode command.
        Can be set to None as an indication of "root"

    @param qualify boolean, describes whether to generate "permute"
        output or "qualify" output.  Permute output is a human readable
        command permutation, while "qualify" is intended for script building

    """
    permuted = []
    permute_submodes = []

    if submode_command == None:
        mode = 'login'
    else:
        # first, the submode.  There ought to be only one permutation.
        permute = CommandPermutor(qualify)
        permuted.append(permute.permute_command(submode_command))
        mode = submode_command.get('submode-name')
        print 'SUBMODE PERMUTE', submode_command['self'], 'MODE', mode

    def submode_match(want, cmd):
        if type(cmd) == str:
            cmd = [cmd]
        if want.startswith('config'):
            for c in cmd:
                if c.endswith('*'):
                    c = c[:-1]
                if c == 'config-':
                    c = 'config'
                if want == c:
                    return True
            else:
                return False
        for c in cmd:
            if want == c:
                return True
        return False

    # there's no command to enter login.
    for c in command_registry:
        if submode_match(mode, c['mode']):
            # no submode commands.
            if c.get('command-type') == 'config-submode':
                print 'SUBMODE POSTPONE', c['self']
                permute_submodes.append(c)
            else:
                permute = CommandPermutor(qualify)
                permuted.append(permute.permute_command(c))

    # now any submodes found
    for c in permute_submodes:
        permuted.append(permute_single_submode(c, qualify))

    return '\n'.join(permuted)


def permute_command(words, qualify):
    """
    Permute all the commands (with no parameters), or a
    sigle command.   The command is named via the name
    of the dictionary, for example: 
    permute COMMAND_DESCRIPTION
    """
    if len(words) == 0:
        return permute_single_submode(None, qualify)
    else:
        permute = CommandPermutor(qualify)
        return permute.handle_command(words)


def _is_string(arg):
    """
    Returns whether or not the argument is a string (either a "str" or "unicode")
    """
    return isinstance(arg, types.StringTypes)


def _is_list(arg):
    """
    Returns whether or not the argument is a list. This means that it's an
    instance of a sequence, but not including the string types
    """
    return isinstance(arg, collections.Sequence) and not _is_string(arg)


def _lookup_in_scopes(name, scopes, default=None):
    """
    Look up the given name in the given list of scopes.
    Returns the default value if the name is not defined in any of the scopes.
    """
    assert name
    assert scopes is not None
    
    # We iterate over the items in the scope (rather than using 'get')
    # so we can do a case-insensitive lookup.
    name_lower = name.lower()
    for scope in scopes:
        for key, value in scope.items():
            if key.lower() == name_lower:
                return value
    return default


def _call_proc(proc_descriptor, proc_registry, scopes, command):
    assert proc_descriptor is not None
    assert proc_registry is not None
    
    if isinstance(proc_descriptor, collections.Sequence) and not _is_string(proc_descriptor):
        proc_descriptors = proc_descriptor
    else:
        proc_descriptors = (proc_descriptor,)
    
    combined_result = None
    
    saved_scopes = scopes
    
    for proc_descriptor in proc_descriptors:
        args_descriptor = None
        scopes = saved_scopes
        if isinstance(proc_descriptor, collections.Mapping):
            # Look up the proc in the specified registry
            proc_name = proc_descriptor.get('proc')
            if not proc_name:
                raise error.CommandDescriptionError('Unspecified proc name: ',
                                              command)
            if proc_descriptor.get('args') or proc_descriptor.get('kwargs'):
                args_descriptor = proc_descriptor
            scopes = [proc_descriptor] + scopes
        else:
            proc_name = proc_descriptor
    
        # Push an empty scope on the scope stack to hold a variable for the scopes
        # FIXME: This should be cleaned up when we change the calling conventions
        # for procs.
        scopes = [{}] + scopes
        scopes[0]['scopes'] = scopes
        
        proc_and_arg = proc_registry.get(proc_name)
        if not proc_and_arg:
            raise error.CommandDescriptionError('Unknown proc name: %s' % proc_name, command)
        proc, default_args_descriptor = proc_and_arg
        
        if not args_descriptor:
            args_descriptor = default_args_descriptor
        
        converted_args = []
        converted_kwargs = {}
        
        # Convert the positional args
        args = _get_args(args_descriptor)
        if args:
            for arg in args:
                if _is_string(arg) and len(arg) > 1 and arg[0] == '$':
                    name = arg[1:]
                    for scope in scopes:
                        if name in scope:
                            value = scope[name]
                            break
                    else:
                        # FIXME: Disabling treating args that can't be resolved as errors, so that
                        # they can instead be interpreted as just using the default value for the 
                        #raise error.CommandDescriptionError('Invalid proc argument: %s ' %
                        #                                name, command)
                        continue
                converted_args.append(value)
                
        # Convert the keyword args
        kwargs = args_descriptor.get('kwargs')
        if kwargs:
            for key, value in kwargs.iteritems():
                if _is_string(value) and len(value) > 1 and value[0] == '$':
                    name = value[1:]
                    for scope in scopes:
                        if name in scope:
                            value = scope[name]
                            break
                    else:
                        # FIXME: Don't treat args that can't be resolved as errors, so that
                        # they can instead be interpreted as just using the default value for the 
                        #raise error.CommandDescriptionError('Invalid proc argument: %s ' %
                        #                                name, command)
                        continue
                converted_kwargs[key] = value
        
        # Call the proc
        # pylint: disable=W0142
        result = proc(*converted_args, **converted_kwargs)

        if result is not None:
            combined_result = combined_result + result if combined_result else result
            
    return combined_result


def _add_applicable_modes(command, mode_dict):
    """
    Helper function for _get_applicable_modes to add any modes for this command
    """
    feature = command.get('feature')
    if feature:
        if not sdnsh.feature_enabled(feature):
            return

    mode = command.get('mode')
    if mode:
        if type(mode) == list:
            for m in mode:
                mode_dict[m] = None
        else:
            mode_dict[mode] = None


def _get_applicable_modes(command):
    """
    Returns a list of all of the modes that are specified for this command
    """
    mode_dict = {}
    _add_applicable_modes(command, mode_dict)
    return mode_dict.keys()


def _exact_mode_match(current_mode, command_modes):
    """
    Return True when this command_mode is an exact match
    for this current mode (used to partition list of commands
    into two groups, one exact level, and for for related mode
    matches)
    """
    if not type(command_modes) == list:
        command_modes = [command_modes]
    for mode in command_modes:
        if mode == current_mode:
            return True
        if mode.endswith('*') and mode[:-1] == current_mode:
            return True
    return False


def _match_current_modes(command, current_mode, modes):
    """
    Even when the current mode isn't in the list of modes,
    there's a few modes in the mode list which are intended to
    be a collection of modes, eg: "config*", and "config-*",
    For any mode which ends in an ('*'), the current mode
    will match when the current mode is prefix of the mode
    (without the last '*' character)
    'config*' is intended to match any config mode, while
    'config-*' is intended to match any config submode.  
    """
    if current_mode in modes:
        return True
    #
    # if the modes is enable, this works everywhere
    #
    if 'login' in modes:
        return True
    #
    # if the modes is login, and the mode is anything but login,
    # then this is true
    #
    if 'enable' in modes and current_mode != 'login':
        return True
    for mode in modes:
        if mode.endswith('*') and current_mode.startswith(mode[:-1]):
            return True
    #if command.get('command-type') == 'config-submode':
    #    for mode in modes:
    #        if current_mode.startswith(mode):
    #            print "_match_current_modes: current command type is config-submode",current_mode,mode
    #            return True
        
    return False
    

def _get_command_title(command):
    if type(command['name']) == str:
        return command['name']
    if type(command['name']) == dict:
        return command['name']['title']
    else:
        raise Exception("Command %s has unknown title" % command['name'])


def _lookup_command_candidates(command_prefix, command_list):
    """
    Returns the list of command candidates from the given command list.
    A candidate must have a 'mode' value that matches the current mode,
    and its name must begin with the given command prefix.
    """
    candidates = []
    current_mode = sdnsh.current_mode()
    try:
        for command in command_list:
            modes = _get_applicable_modes(command)
            if _match_current_modes(command, current_mode, modes):
                name = command['name']
                if (type(name) == str and
                    name.startswith(command_prefix.lower())):
                    candidates.append(command)
                # should check the type of command_prefix,
                # and for str, ame.match(command_prefix):
                if type(name) == dict:
                    if 're' not in name:
                        command['name']['re'] = re.compile(name['pattern'])
                    if name['re'].match(command_prefix):
                        candidates.append(command)
                if type(name) == dict and \
                        name['re'](command_prefix):
                    candidates.append(command)
                    
    except Exception, _e:
        raise error.CommandDescriptionError('Missing mode or name: ', command)
    
    return candidates

def do_command(words):
    executor = CommandExecutor()
    result = executor.handle_command(words)
    return result

def init_command(bs, modi):
    # FIXME HACK: sdnsh global to access top-level CLI object
    global sdnsh, mi
    sdnsh = bs
    mi = modi

    c_actions.init_actions(bs, modi)
    c_data_handlers.init_data_handlers(bs, modi)
    c_completions.init_completions(bs, modi)
    c_validations.init_validations(bs, modi)


    add_command_type('config', {
        'action': 'write-fields',
        'no-action': 'reset-fields'
    })

    add_command_type('config-object', {
        'action': 'write-object',
        'no-action': 'delete-objects',
    })

    add_command_type('config-submode', {
        'action': 'push-mode-stack',
        #'no-action': 'delete-child-objects',
        'no-action': 'delete-objects',
    })

    add_command_type('create-tunnel', {
        'action': 'create-tunnel'
    })
    
    add_command_type('remove-tunnel', {
        'action': 'remove-tunnel'
    })
    
    add_command_type('create-policy', {
        'action': 'create-policy'
    })
    
    add_command_type('remove-policy', {
        'action': 'remove-policy'
    })
    
    add_command_type('display-table', {
        'action': 'display-table'
    })
    
    add_command_type('display-rest', {
        'action': 'display-rest'
    })
    
    add_command_type('manage-alias', {
        'action'    : 'create-alias',
        'no-action' : 'delete-alias',
    })

    add_command_type('manage-tag', {
        'action'    : 'create-tag',
        'no-action' : 'delete-tag',
    })

    add_command_type('update-config', {
        'action'    : 'update-config',
        'no-action' : 'update-config',
    })

    # Initialize typedefs
    add_typedef({
        'name': 'string',
        'validation': 'validate-string'
    })
    
    add_typedef({
        'name': 'integer',
        'validation': 'validate-integer'
    })

    add_typedef({
        'name': 'hex-or-decimal-integer',
        'validation': 'validate-hex-or-dec-integer'
    })

    add_typedef({
        'name': 'date',
        'validation': 'validate-date'
    })

    add_typedef({
        'name': 'duration',
        'validation': 'validate-duration'
    })

    add_typedef({
        'name': 'enum',
        'validation': 'validate-enum'
    })

    add_typedef({
        'name'       : 'mac-address',
        'help-name'  : 'MAC Address',
        'base-type'  : 'string',
        'validation' : 'validate-mac-address',
    })
    
    add_typedef({
        'name'       : 'host',
        'help-name'  : 'Host Alias or MAC Address',
        'base-type'  : 'string',
        'validation' : 'validate-host',
    })
    
    add_typedef({
        'name'       : 'vlan',
        'help-name'  : 'Vlan',
        'base-type'  : 'integer',
        'validation' : 'validate-integer',
    })

    add_typedef({
        'name'       : 'label',
        'help-name'  : 'Segment Label',
        'base-type'  : 'integer',
        'validation' : 'validate-integer',
    })
    
    add_typedef({
        'name'       : 'dpid',
        'help-name'  : 'switch id (8-hex bytes)',
        'base-type'  : 'string',
        'validation' : 'validate-switch-dpid',
    })

    add_typedef({
        'name'      : 'ip-address',
        'help-name' : 'IP address (dotted quad)',
        'base-type' : 'string',
        'pattern'   : r'^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])$',
    })
    
    add_typedef({
        'name'        : 'ip-address-not-mask',
        'help-name'   : 'IP Address',
        'base-type'   : 'string',
        'pattern'     : r'^(\d{1,3}\.){3}\d{1,3}$',
        'validation'  : 'validate-ip-address-not-mask'
    })
    
    add_typedef({
        'name'        : 'cidr-range',
        'help-name'   : 'cidr range (ip-address/int)',
        'base-type'   : 'string',
        'pattern'     : r'^(\d{1,3}\.){3}\d{1,3}/\d{1,2}?$',
        'validation'  : 'validate-cidr-range',
    })

    add_typedef({
        'name'        : 'netmask',
        'help-name'   : 'netmask (eg: 255.255.255.0)',
        'base-type'   : 'ip-address',
        'validation'  : 'validate-netmask'
    })
    
    add_typedef({
        'name'       : 'obj-type',
        'help-name'  : 'configured object',
        'base-type'  : 'string',
        'validation' : 'validate-existing-obj'
    })

    add_typedef({
        'name'       : 'inverse-netmask',
        'help-name'  : 'inverse netmask (eg: 0.0.0.255)',
        'base-type'  : 'ip-address',
        'validation' : 'validate-inverse-netmask'
    })
    
    add_typedef({
        'name'      : 'domain-name',
        'help-name' : 'Domain name',
        # Simple domain name checking.
        # Allows some things that aren't valid domain names, but doesn't
        # disallow things liky punycode and fully qualified domain names.
        'pattern'   : r'^([a-zA-Z0-9-]+.?)+$',
        'base-type' : 'string'
    })

    add_typedef({
        'name': 'ip-address-or-domain-name',
        'help-name': 'IP address or domain name',
        'base-type': 'string',
        'pattern': (
            r'^([a-zA-Z0-9-]+.?)+$', # simple domain name
            r'^(\d{1,3}\.){3}\d{1,3}$' # IP address
        ),
        # for ip addresses, ought to validate non-mask values, ie: 0.0.0.0, 255.255.255.255
        # ought to also validate ip addresses which aren't ip address, the dhcp 169.x values.
    })
    
    add_typedef({
        'name'       : 'resolvable-ip-address',
        'help-name'  : 'resolvable ip address',
        'base-type'  : 'string',
        'pattern'    : (
            r'^([a-zA-Z0-9-]+.?)+$', # simple domain name
            r'^(\d{1,3}\.){3}\d{1,3}$' # IP address
        ),
        'validation' : 'validate-resolvable-ip-address',
    })

    add_typedef({
        'name': 'enable-disable-flag',
        'help-name': 'Enter "enable" or "disable"',
        'base-type': 'enum',
        'values': ('disable', 'enable'),
    })

    add_typedef({
        'name'       : 'identifier',
        'help-name'  : 'Alphabetic character, followed by alphanumerics',
        'base-type'  : 'string',
        'validation' : 'validate-identifier',
    })

    add_typedef({
        'name'       : 'config',
        'help-name'  : 'name of config file',
        'base-type'  : 'string',
        'validation' : 'validate-config',
    })

