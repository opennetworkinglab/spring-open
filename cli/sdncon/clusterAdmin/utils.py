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

import os
import sdncon

BSN_BASE = sdncon.SDN_ROOT
PERSONALITY = 'vm_personality'

"""
This is a simple conditional decorator, suggested by Claudiu on Stackoverflow.com
"""
def conditionally(decorator, condition):
    def moddec(f):
        if not condition:
            return f
        return decorator(f)
    return moddec

def isCloudBuild():
    pf = os.path.join(BSN_BASE, PERSONALITY)
    return os.path.exists(pf) and "cloud" in open(pf).read()

def uicontext(request):
    expiredSession = False
    debug = False
    try:
        hostname = request.META['HTTP_HOST']
        referer = request.META['HTTP_REFERER'].split('/')
        if referer[2] == hostname:
            expiredSession = referer[3] not in ('account', 'logout')
    except:
        # Ignore any exceptions, like missing REFERER, HOST, etc.
        pass
    return {
        'isCloudBuild': isCloudBuild(),
        'expiredSession': expiredSession,
        'debug': debug,
    }

def abc(f):
    def blah(*args, **kwargs):
        print 'abc'
        return f(*args, **kwargs)
    return blah

@conditionally(abc, isCloudBuild())
def test(): pass

if __name__ == '__main__':
    test()

    
