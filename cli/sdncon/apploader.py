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

from django.db import models
from django.conf.urls.defaults import *

# This is a class that is used to keep a list of apps and their tabs in memory
class AppLister():
    
    def __init__(self, name, label, priority, description=""):
        self.name = name
        self.label = label
        self.priority = priority
        self.description = description
        self.tabs = []

    def orderTabs(self, a, b):
        return cmp(int(a["priority"]), int(b["priority"]))
        
    def addTab(self, id, label, view, priority=1000):
        for t in self.tabs:
            if t["id"] == id:
                self.tabs.remove(t)
        self.tabs.append( { 'id':id, 'label':label, "priority":priority, "view":view } )
        self.tabs.sort(self.orderTabs)

class AppLoader():
    
    # List of all registered applications
    #
    # This is a list of dictionaries
    #
    
    firstInit = True
    apps = []

    @classmethod    
    def orderApps(cls, a, b):
            return cmp(int(a.priority), int(b.priority))

    @classmethod
    def addApp(cls,app):
        for a in cls.apps:
            if a.name == app.name:
                return False
        cls.apps.append(app)
        cls.apps.sort(cls.orderApps)
            
    @classmethod
    def getApps(cls):
        return cls.apps

    @classmethod    
    def getApp(cls, name):
        for a in cls.apps:
            if a.name == name:
                return a
        return None
        
    @classmethod
    def registerAllApps(cls):
        if cls.firstInit:
            from django.conf import settings
            if "sdncon.coreui" not in settings.INSTALLED_APPS:
                print "****** panic, sdncon.coreui is missing!"
            for app in settings.INSTALLED_APPS:
                if not app.startswith("django"):
                    init_func = __import__("%s.views" % app, fromlist=["bsc_app_init"])
                    if 'bsc_app_init' in dir(init_func):
                        init_func.bsc_app_init()
            cls.firstInit=False
