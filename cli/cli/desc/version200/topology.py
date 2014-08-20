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
"""

TOPOLOGY_COMMAND_DESCRIPTION = {
    'name'         : 'topology',
    'mode'         : 'config',
    'short-help'   : 'Enable features for controller',
    'doc'          : 'topology|topology',
    'doc-example'  : 'topology|topology-example',
    'command-type' : 'config',
    'obj-type'     : 'topology-config',
    'args': {
        'choices': (
            {
                'token'            : 'autoportfast',
                'short-help'       : 'Enable the auto PortFast feature',
                'doc'              : 'topology|topology-autoportfast',
                'action'           : (
                    {
                        'proc'     : 'write-object',
                        'data'     : {
                                       # The primary key ('id') must be present to
                                       # read the old row value, and update items,
                                       # otherwise a 'new' row is written with default
                                       # values

                                       'id'            : 'topology',
                                       'autoportfast'  : True,
                                     },
                    },
                ),
               'no-action'        : (
                    {
                        'proc'     : 'write-object',
                        'data'     : {
                                       'id'           : 'topology',
                                       'autoportfast' : False ,
                                     },
                    },
                )
            },
        )
    }
}
"""