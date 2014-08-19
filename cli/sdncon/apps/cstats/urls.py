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

from django.conf.urls.defaults import *
import os

# Add the URLs that you need here e.g.:

urlpatterns = patterns('',
#   (r'^tab_mytab/', 'views.mytab'),
)

# Uncomment this if you have bundeled in static resources (e.g. javascript) or images
# They will be served from:
#   app-name/static
#   app-name/img

urlpatterns += patterns('',
    (r'^static/(?P<path>.*)$', 'django.views.static.serve', \
        {'document_root': os.path.join(os.path.dirname(__file__),'static')}),
#    (r'^img/(?P<path>.*)$', 'django.views.static.serve', \
#        {'document_root': os.path.join(os.path.dirname(__file__),'img')}),
)

