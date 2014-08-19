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

#import time

import datetime
import django.core.management

try:
    import settings # Assumed to be in the same directory.
except ImportError:
    import sys
    sys.stderr.write("Error: Can't find the file 'settings.py' in the directory containing %r. It appears you've customized things.\nYou'll have to run django-admin.py, passing it your settings module.\n(If the file settings.py does indeed exist, it's causing an ImportError somehow.)\n" % __file__)
    sys.exit(1)

django.core.management.setup_environ(settings)

from django.db import models
from django.db.models.fields import AutoField, BooleanField, IntegerField
from django.db.models.fields.related import ForeignKey

import sdncon.rest.views

print "# Generated automatically from controller source"
print "# via tools/extract_model.py date:  %s" % datetime.datetime.now().ctime()

# we iterate over rest_name_dict, limiting CLI access to those models
# and model fields accessible via rest

rest_map = {}
for (model, model_info) in sdncon.rest.views.rest_model_info_dict.items():
    rest_map[model] = {}
    for (rest_field_name, rest_field_info) in model_info.rest_name_dict.items():
        django_field_name = rest_field_info.name
        rest_map[model][django_field_name] = rest_field_name

model_info_dict = {}
for (model, model_info) in sdncon.rest.views.rest_model_info_dict.items():
    django_model_class = model_info.model_class 
    field_info_dict = {}
    for (rest_field_name, rest_field_info) in model_info.rest_name_dict.items():
        django_field_name = rest_field_info.name
        django_field_info = rest_field_info.django_field_info
        # now that we have the django info and our own rest info, create a field info to dump
        json_serialize_string = type(django_field_info) not in (AutoField, BooleanField, IntegerField)
        field_info = {}
        field_info['json_serialize_string'] = json_serialize_string
        if django_field_info.verbose_name != django_field_name:
            # Check if this is a proxy class
            if type(django_field_info.verbose_name) is str:
                field_info['verbose-name'] = django_field_info.verbose_name
        if django_field_info.primary_key == True:
            field_info['primary_key'] = True
        if django_field_info.help_text != "":
            field_info['help_text'] = django_field_info.help_text
        field_info['null'] = django_field_info.null
        if type(django_field_info.default) in [int, bool, str]:
            field_info['default'] = django_field_info.default
        field_info['type'] = str(type(django_field_info)).split('.')[-1].replace("'>", "")
        if field_info['type'] == 'AutoField':
            # Re-label the cassandra compound key for easier consumption
            if hasattr(django_model_class, 'CassandraSettings'):
                cassandra_settings = django_model_class.CassandraSettings
                if hasattr(cassandra_settings, 'COMPOUND_KEY_FIELDS'):
                    compound_key_fields = cassandra_settings.COMPOUND_KEY_FIELDS
                    rest_key_fields = [rest_map[model].get(x, x) for x in compound_key_fields]
                    field_info['compound_key_fields'] = rest_key_fields
                    field_info['type'] = 'compound-key'
                    field_info['help_text'] = '#|%s' % \
                                              '|'.join(rest_key_fields)
        if field_info['type'] == 'ForeignKey':
            other_object = django_field_info.rel.to.Rest.NAME
            if django_field_info.rel.field_name in rest_map[other_object]:
                field_info['rel_field_name'] = \
                    rest_map[other_object][django_field_info.rel.field_name]
            else:
                field_info['rel_field_name'] = django_field_info.rel.field_name
            field_info['rel_obj_type'] = django_field_info.rel.to.Rest.NAME
        if field_info['type'] == 'CharField':
            field_info['max_length'] = django_field_info.max_length
        #if django_field_info.validators:
            #field_info['validators'] = django_field_info.validators
        #if isinstance(type, django.PositiveIntegerField):
            #type_name = 'PositiveIntegerField'
            #print type_name

        field_info_dict[rest_field_name] = field_info
    model_info_dict[model]={'fields':field_info_dict, 'has_rest_model': True}

import pprint
pp = pprint.PrettyPrinter(indent=2)
print "model_info_dict = ",pp.pprint(model_info_dict)
