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

#

#
#  vendor.py - parse OUI information and look up vendors

import re
import os
from pkg_resources import resource_stream

class VendorDB():
    
    vendors = {}
    
    def init(self):
            f = resource_stream(__name__, 'data/oui.txt')
            if f is None:
                p = os.path.join(os.path.dirname(__file__), "data", "oui.txt")
                if not os.path.exists(p):
                    print "Warning: Vendor OUI file could not be located!"
                    return
                f = open(p)

            while True:
                l = f.readline()
                if len(l) ==0:
                    break # EOF
                if l.count("(base 16)"):
                    oui = l[0:6]
                    vendor = l[22:]
                    self.add_vendor(oui,vendor)
                
    def add_vendor(self, oui, vendor):
        oui = oui.strip()
        vendor = vendor.strip()
        if len(oui) == 6 and len(vendor) > 1:
            self.vendors[oui]=vendor
        
    def get_vendor(self, mac):
        if len(mac) < 6:
            return "unknown"
        mac = mac.upper()  # To upper case
        mac = re.sub(':','',mac) # filter any ":"
        mac = mac[0:6]
    
        # Strip broadcast and local bit
        b = hex(int(mac[1:2],16) & int('1100',2))[2:3]
        mac = mac[0:1]+b.upper()+mac[2:6]
        
        if mac in self.vendors:
            return self.vendors[mac]
        else:
            return "Unknown"
        
# Show all records

if __name__ == '__main__':
    db = VendorDB()
    db.init()
    print db.vendors
