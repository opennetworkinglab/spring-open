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

from django.shortcuts import render_to_response
from sdncon.apploader import AppLoader, AppLister
from sdncon.controller.models import Switch
import os

def bsc_app_init():  
    # By default, App Name is the same as directory name. Change if needed.
    APP_NAME = os.path.dirname(__file__).split("/")[-1]   
    
    # Create the App. Parameters are 
    # - Name: the id, lowercase letters only
    # - Label: Human readable discription for the menu to the left
    # - Priority: determines ranking the menu to the left), One-line description
    # - Description: One line description of the app
    app = AppLister(APP_NAME, "Controller Stats", 5, "Controller Stats")

    # Add Tabs. Parameters are:
    # - Name: the id, lowercase letters only
    # - Label: Human readable discription for the menu to the left
    # - View: name of the python function that contains the django view (see below)
    app.addTab("openflow_graphs", "OpenFlow Graphs", flow_graphs_view)
    app.addTab("system_graphs", "System Graphs", system_stats_graph_view)
    AppLoader.addApp(app)

def system_stats_graph_view(request):
    return render_to_response('apps/cstats/templates/graphs.html', {} )

def flow_graphs_view(request):
    return render_to_response('apps/cstats/templates/openflowgraphs.html', {} )

