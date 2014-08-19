#!/usr/bin/env python
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

"""Polling interface for the polling services setup by the rest API"""

import sys, os, time, random, json

from packetstreamer import PacketStreamer
from packetstreamer.ttypes import *

from thrift import Thrift
from thrift.transport import TSocket
from thrift.transport import TTransport
from thrift.protocol import TBinaryProtocol

def sample_func(request, environ):
    data = {
        'pid': os.getpid(),
        'random': int(random.random()*100),
    }

    return {
        'status':    '200 OK',
        'body':      json.dumps(data),
        'headers':   [('Content-type', 'application/json')], # list of tuples
    }

def getPackets_func(request, environ, sessionid):
    retData = {}

    try:
        # Make socket
        transport = TSocket.TSocket('localhost', 9090)
        # Buffering is critical. Raw sockets are very slow
        transport = TTransport.TFramedTransport(transport)
        # Wrap in a protocol
        protocol = TBinaryProtocol.TBinaryProtocol(transport)
        # Create a client to use the protocol encoder
        client = PacketStreamer.Client(protocol)
        # Connect!
        transport.open()

        packets = client.getPackets(sessionid)

        # Close!
        transport.close()

        retData = {
          'status':    '200 OK',
          'body':       json.dumps(packets),
          'headers':    [('Content-type', 'application/json')], # list of tuples
        }

    except Thrift.TException, tx:
        retData = {
          'status':    '500 Internal Server Error',
          'body':      'error: %s'%tx.message,
          'headers':   [('Content-type', 'application/json')], # list of tuples
        }

    return retData

# The polling service
#   The service is driven by the urlpatterns tuples that list the specific function
#   to use for a matching pattern in URL. The pattern should not include initial '^/poll'.
#   For Example:
#     (r'^/sample', sample_func)
#from sdncon.rest.poll import sample_func
urlpatterns = [
    (r'^/packets/(?P<sessionid>[A-Za-z0-9_.\-]+)/?$', getPackets_func),
    (r'^/sample', sample_func)
]

import re, wsgiref.util

def compile_patterns(urlpatterns):
    return map(lambda pat: (re.compile(pat[0]), pat[1]), urlpatterns)

def main(environ, start_response):
    patterns = compile_patterns(urlpatterns)
    request = environ['PATH_INFO']
    response = None
    status, body = '404 Not Found', 'Sorry, the requested resource "%s" was not found."' % request
    headers = []
    for (pat, func) in patterns:
        m = pat.match(request)
        if m:
            args = m.groups()
            if args:
                response = func(request, environ, *args)
            else:
                response = func(request, environ)

            status, body = response['status'], response['body']
            if 'headers' in response:
                headers.extend(response['headers'])
            break
    if 'Cache-Control' not in headers:
        headers.append(('Cache-Control', 'no-cache'))
    if 'Content-type' not in headers:
         headers.append(('Content-type', 'text/plain'))
    if 'Content-Length' not in headers:
         headers.append(('Content-Length', str(len(body))))
    start_response(status, headers)
    return [body]

if __name__ == "__main__":
    def start_response(x, y):
        print 'Status:', x
        print 'Headers:', y

    print 'Sample', sample_func(None, None)
    print 'Response:', main({'PATH_INFO':'/sample/100'}, start_response)
    print 'Response:', main({'PATH_INFO':'/xsample/100'}, start_response)
