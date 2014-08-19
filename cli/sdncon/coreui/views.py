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

#  Views for the core controller UI
#

from django.shortcuts import render_to_response
from django.utils import simplejson
from django.http import HttpResponse
from django.contrib.auth.decorators import login_required
from django.contrib.auth.models import User
from django.template import RequestContext
from sdncon.apploader import AppLoader, AppLister
from sdncon.clusterAdmin.utils import conditionally, isCloudBuild
from sdncon.clusterAdmin.models import Customer, Cluster, CustomerUser
import os

JSON_CONTENT_TYPE = 'application/json'

# --- View for the root page of any application
@conditionally(login_required, isCloudBuild())
def show_application_tabs(request, app):
    clusterlist = []
    cus = CustomerUser.objects.all()
    for cu in cus:
        if cu.user.username == request.user.username:
            for cluster in cu.customer.cluster_set.all():
                clusterlist.append({'customer': cluster.customer.customername,
                                    'clustername': cluster.clustername,
                                    'clusterid': unicode(cluster)})

    return render_to_response('coreui/templates/showapp.html', 
                {'apps':[x for x in AppLoader.getApps() if not hasattr(x,'invisible')],'currentapp':app,'tabs':AppLoader.getApp(app).tabs,'clusterlist':clusterlist, 'isCloudBuild': isCloudBuild()},
                context_instance=RequestContext(request))

# --- Generic Views
def embedded_datatable(request, model, options=None, skip_columns=[]): 

    model_name = model.__name__
    title = model_name + " Table"
    tablename = model_name+"-embed"
    table_data = Switch.objects.all()

    # Find headers
    table_headers = []
    for field in model._meta.local_fields:
        table_headers.append(field.name)
    print table_headers

    # Skip unwanted columns
    for c in skip_columns:
        if c in table_headers:
            table_headers.remove(c)

    # Populate data
    table_data = []
    for instance in model.objects.all():
        table_row = []
        for field in table_headers:
            value = instance.__dict__.get(field)
            table_row.append(value)
        table_data.append(table_row)

    # Beautify Headers
    ##TODO ALEX query model info directly, don't use model info list then remove import and delete file
    #print model_name
    #if model_name.lower() in model_info_list.model_info_dict:
    #    print "yes"
    #    table_headers_c = []
    #    field_dict = model_info_list.model_info_dict[model_name.lower()]["fields"]
    #    for h in table_headers:
    #        h = h.replace("_","-")
    #        print h, field_dict
    #        if h in field_dict:
    #            table_headers_c.append(field_dict[h]['verbose_name'])
    #        else:
    #            table_headers_c.append(h)
    #    table_headers = table_headers_c

    return render_to_response('coreui/templates/datatable-embed.html', {
        'tabletitle': title,
        'tablename' : tablename,
        'tableheaders' : table_headers, 
        'tabledata' : table_data,
        'dataTableOptions': options
    })

def embedded_datatable_from_model(request, model, options=None, skip_columns=[]): 
    
    model_name = model.__name__
    title = model_name + " Table"
    tablename = model_name+"-embed"
    table_data = Switch.objects.all()
    
    # Find headers
    table_headers = []
    for field in model._meta.local_fields:
        table_headers.append(field.name)
    print table_headers
    
    # Skip unwanted columns
    for c in skip_columns:
        if c in table_headers:
            table_headers.remove(c)

    # Populate data
    table_data = []
    for instance in model.objects.all():
        table_row = []
        for field in table_headers:
            value = instance.__dict__.get(field)
            table_row.append(value)
        table_data.append(table_row)

    # Beautify Headers
    print model_name
    if model_name.lower() in model_info_list.model_info_dict:
        print "yes"
        table_headers_c = []
        field_dict = model_info_list.model_info_dict[model_name.lower()]["fields"]
        for h in table_headers:
            h = h.replace("_","-")
            print h, field_dict
            if h in field_dict:
                if 'verbose_name' in field_dict[h]:
                    table_headers_c.append(field_dict[h]['verbose_name'])
                else:
                    table_headers_c.append(h)
            else:
                table_headers_c.append(h)
        table_headers = table_headers_c
    
    return render_to_response('coreui/templates/datatable-embed.html', {
        'tabletitle': title,
        'tablename' : tablename,
        'tableheaders' : table_headers, 
        'tabledata' : table_data,
        'dataTableOptions': options
    })
   
