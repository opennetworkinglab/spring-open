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

class CommandError(Exception):
    """
    Base class for exceptions thrown by the CLI command module
    """
    def __init__(self, kind, message = None):
        if kind:
            message = kind + ': ' + message
        super(CommandError, self).__init__(message)

class ArgumentValidationError(CommandError):
    def __init__(self, message=None, expected_tokens=None):
        kind = "Invalid argument"
        super(ArgumentValidationError, self).__init__(kind, message)
        self.expected_tokens = expected_tokens


class CommandSyntaxError(CommandError):
    def __init__(self, message):
        #kind = 'Error'
        kind = None
        super(CommandSyntaxError,self).__init__(kind, message)


class RangeSyntaxError(CommandError):
    def __init__(self, message):
        kind = "Value outside length/range"
        super(RangeSyntaxError,self).__init__(kind, message)


class CommandDescriptionError(CommandError):
    def __init__(self, message, command = None):
        self.command = None
        kind = "Bad command description"
        if command:
            self.command = command
            message += ': ' + command['self']
        super(CommandDescriptionError,self).__init__(kind, message)
    

class CommandCompletionError(CommandError):
    def __init__(self, message):
        kind = "Command completion"
        super(CommandCompletionError,self).__init__(kind, message)


class CommandAmbiguousError(CommandError):
    def __init__(self, message):
        kind = "Ambiguous command"
        super(CommandAmbiguousError,self).__init__(kind, message)


class CommandMissingError(CommandError):
    def __init__(self, message):
        kind = "No such command"
        super(CommandMissingError,self).__init__(kind, message)


class CommandInvocationError(CommandError):
    def __init__(self, message):
        kind = "Invalid command invocation"
        super(CommandInvocationError,self).__init__(kind, message)


class CommandSemanticError(CommandError):
    def __init__(self, message):
        kind = "Invalid Use"
        super(CommandSemanticError,self).__init__(kind, message)


class CommandInternalError(CommandError):
    def __init__(self, message):
        kind = "Internal (bug)"
        super(CommandInternalError,self).__init__(kind, message)


class CommandRestError(CommandError):
    def __init__(self, rest_info=None, message=None):
        s = 'Error: REST API'
        if rest_info:
            error_type = rest_info.get('error_type')
            if error_type:
                s += '; type = ' + error_type
            description = rest_info.get('description')
            if description:
                s += '; ' + description
        if message:
            s += ': ' + message
        super(CommandRestError,self).__init__("REST", s)
        
