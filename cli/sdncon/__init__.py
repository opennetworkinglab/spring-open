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
# this line must be before the next imports
import constants
SDN_ROOT = constants.SDN_ROOT
from sdncon.apploader import AppLoader
from sdncon.urls import urlpatterns
from django.conf.urls.defaults import *
from django.http import HttpResponse
import os.path

coreui_inited = False

ROLE_PATH = "%s/current_role" % SDN_ROOT
UPGRADE_PATH = "%s/upgrading" % SDN_ROOT
CASSANDRA_SENTINEL = "%s/run/starting" %SDN_ROOT

SLAVE_RO_URL_WHITELIST = ["/rest/v1/system",
                       "/rest/v1/controller/storage/tables",
                       "/rest/v1/model/controller-node",
                       "/rest/v1/model/controller-alias",
                       "/rest/v1/model/syncd-config/default",
                       "/rest/v1/model/feature",
                       "/rest/v1/model/statdropd-progress-info",
                       "/rest/v1/model/statdropd-config/default",
                       "/rest/v1/model/firewall-rule",
                       "/rest/v1/model/controller-domain-name-server",
                       "/rest/v1/stats/metadata",
                       "/rest/v1/system/ha/clustername",
                       "/rest/v1/system/ha/role",
                       "/rest/v1/system/reload",
                       ]

SLAVE_RW_URL_WHITELIST = [
                         "/rest/v1/stats/data/default",
                         # Need to make controller-interface RW because the
                         # discover_ip daemon monitors changes to the network
                         # interfaces and updates the "discovered_ip" and "mac"
                         # fields in the controller-interface, even on the slave.
                         # FIXME: Really those fields should be in a different model so
                         # we wouldn't be mixing configuration and discovered state,
                         # but it's too late in the release cycle to make that change.
                         "/rest/v1/model/controller-interface",
                         # Needs to be RW, to allow cluster number update
                         # even if all the controllers are slaves
                         "/rest/v1/model/global-config",
                         "/rest/v1/model/firewall-rule",
                         "/rest/v1/model/controller-node",
                         "/rest/v1/system/upgrade/image-name",
                         "/rest/v1/system/upgrade/extract-image-manifest",
                         "/rest/v1/system/upgrade/execute-upgrade-step",
                         "/rest/v1/system/upgrade/abort",
                         "/rest/v1/system/rollback/config",
                         "/rest/v1/system/ha/decommission",
                         "/rest/v1/system/ha/role",
                         "/rest/v1/system/reload",
                         "/rest/v1/system/resetbsc",
                         "/rest/v1/system/upload-data",
                         ]

class SDNConAppLoaderMiddleWare(object):
    def process_request(self, request):
        global coreui_inited, urlpatterns
        if not coreui_inited:
            try:
                AppLoader.registerAllApps()
                coreui_inited = True
            except Exception, e:
                print "**** panic! AppLoader.registerAllApps() threw this Exception:"
                print e
            for index, app in enumerate(AppLoader.apps):
                # Redirect / to the first app in the list
                if (index == 0):
                    urlpatterns += patterns( '', (r'^$', 'django.views.generic.simple.redirect_to', {'url': '/'+app.name}),)
                urlpatterns += patterns( '', (r'^'+app.name+'/?$', 'sdncon.coreui.views.show_application_tabs', {'app':app.name}) )
                urlpatterns += patterns( '', (r'^'+app.name+'/', include(app.name+'.urls')), )
                for t in app.tabs:
                    urlpatterns += patterns(app.name+'.views', (r'^'+app.name+'/'+t["id"]+'/?$', t["view"]) )

class HARedirectMiddleWare(object):
    def process_request (self, request):
        pinfo = request.path_info
        if pinfo[-1] == "/" :
            pinfo = pinfo[:-1]

        if request.method == "GET":
            for match in SLAVE_RO_URL_WHITELIST:
                if pinfo.startswith(match):
                    return None

        for match in SLAVE_RW_URL_WHITELIST:
            if pinfo.startswith(match):
                return None

        role = "MASTER"
        try:
            with open(ROLE_PATH, "r") as f:
                for line in f:
                    if line.startswith("sdnplatform.role"):
                        role = line.split("=")[1].strip()
        except IOError, e: # Firstboot doesn't have the file
            return None
        if role == "SLAVE" and os.path.exists(UPGRADE_PATH) == False:
            return HttpResponse(status = 303)
        else:
            return None
