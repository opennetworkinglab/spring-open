#!/usr/bin/python
#
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
# Query REST API, return dictionary with alias to DPID mappings. 
#

#Importing modules
import re
import sys
import json
import urllib2
from sdncon.rest.views import do_instance

def aliasDict(request):
  
  # Query JSON from API and load into dictionary
  rawdict = json.loads(do_instance(request, 'switch-alias').content)

  # Dictionaries
  aliasdict = {}
 
  # Step through master 'alias' list, extract entry for each dictionary.
  for index_query,value1_query in enumerate(rawdict):

    # get needed entries in 'alias'
    aliasdict[value1_query.get('switch','ERR')] = value1_query.get('id',' ')

  return aliasdict
