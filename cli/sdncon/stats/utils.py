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

from thrift.transport import TTransport
from thrift.transport import TSocket
from thrift.protocol import TBinaryProtocol
from cassandra import Cassandra
from cassandra.ttypes import *

# FIXME: This class is derived from the django_cassandra backend.
# Should refactor this code better.

class CassandraConnection(object):
    def __init__(self, host, port, keyspace, user, password):
        self.host = host
        self.port = port
        self.keyspace = keyspace
        self.user = user
        self.password = password
        self.transport = None
        self.client = None
        self.keyspace_set = False
        self.logged_in = False
        
    def set_keyspace(self):
        if not self.keyspace_set:
            self.client.set_keyspace(self.keyspace)
            self.keyspace_set = True
    
    def login(self):
        # TODO: This user/password auth code hasn't been tested
        if not self.logged_in:
            if self.user:
                credentials = {'username': self.user, 'password': self.password}
                self.client.login(AuthenticationRequest(credentials))
            self.logged_in = True
            
    def connect(self, set_keyspace=False, login=False):
        if self.transport == None:
            # Create the client connection to the Cassandra daemon
            socket = TSocket.TSocket(self.host, int(self.port))
            transport = TTransport.TFramedTransport(TTransport.TBufferedTransport(socket))
            protocol = TBinaryProtocol.TBinaryProtocolAccelerated(transport)
            transport.open()
            self.transport = transport
            self.client = Cassandra.Client(protocol)
            
        if login:
            self.login()
        
        if set_keyspace:
            self.set_keyspace()
                
    def disconnect(self):
        if self.transport != None:
            self.transport.close()
            self.transport = None
            self.client = None
            self.keyspace_set = False
            self.logged_in = False
            
    def is_connected(self):
        return self.transport != None
    
    def reconnect(self):
        self.disconnect()
        self.connect(True, True)
    
    def get_client(self):
        if self.client == None:
            self.connect(True,True)
        return self.client
