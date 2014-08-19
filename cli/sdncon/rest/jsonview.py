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

from django.http import HttpResponseForbidden, HttpResponse
from django.utils.simplejson import JSONEncoder
from django.utils.encoding import force_unicode
from django.db.models.base import ModelBase
from django.utils.simplejson import dumps

class JsonResponse(HttpResponse):
    """A HttpResponse subclass that creates properly encoded JSON response"""

    def __init__(self, content='', json_opts={},
                 mimetype="application/json", *args, **kwargs):
        
        if content is None: content = []
        content = serialize_to_json(content,**json_opts)
        super(JsonResponse,self).__init__(content,mimetype,*args,**kwargs)

class JsonEncoder(JSONEncoder):
    """A JSONEncoder subclass that also handles querysets and models objects."""

    def default(self,o):
        # this handles querysets and other iterable types
        try: iterable = iter(o)
        except TypeError: pass
        else: return list(iterable)
 
        # this handlers Models objects
        try: isinstance(o.__class__,ModelBase)
        except Exception: pass
        else: return force_unicode(o)
 
        # delegate the rest to JSONEncoder
        return super(JsonEncoder,self).default(obj)
 
def serialize_to_json(obj,*args,**kwargs):
    """A wrapper for dumps with defaults as:
        ensure_ascii=False
        cls=JsonEncoder"""
 
    kwargs['ensure_ascii'] = kwargs.get('ensure_ascii', False)
    kwargs['cls'] = kwargs.get('cls', JsonEncoder)
    return dumps(obj,*args,**kwargs)

def as_kwargs(qdict):
    kwargs = {}
    for k,v in qdict.items():
        kwargs[str(k)] = v
    return kwargs
