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

#  Views for the application
#

import os
from django.shortcuts import render_to_response
from sdncon.apploader import AppLoader, AppLister
from sdncon.clusterAdmin.utils import isCloudBuild

docs_enabled = False

def bsc_app_init():  
    if not docs_enabled:
        return

    APP_NAME = os.path.dirname(__file__).split("/")[-1]   
    app = AppLister(APP_NAME, "Documentation", 9, "User Guide")
    app.addTab("docs", "User Guide", user_guide)
    AppLoader.addApp(app)

def user_guide(request):
    url = '/docs/static/pdf/user-guide.pdf'
    return render_to_response('apps/docs/templates/pdf_doc.html', {'url' : url})
