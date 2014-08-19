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

from django.db.models.signals import post_save, post_delete
import json
import os
import traceback
from django.core import serializers
import sdncon

# FIXME: This code is not thread-safe!!!
# Currently we don't run Django with multiple threads so this isn't a problem,
# but if we were to try to enable threading this code would need to be fixed.

pre_save_instance = None
config_handlers = []
config_models = set()


def add_config_handler(dependencies, handler):
    """
    The dependencies argument is a dictionary where the key is a model
    (i.e. the actual model, not the name of the mode) and the value of the
    key is a tuple (or list) of field names whose modification should
    trigger the config handler. The value can also be None to indicate
    that the config handler should be triggered when any field in the
    model is modified.
    
    The calling convention of a config handler is:
    
    def my_config_handler(op, old_instance, new_instance, modified_fields)
    
    The 'op' argument is one of 'INSERT', 'UPDATE' or 'DELETE', corresponding
    to an insertion, update or deletion of a row in the model.
    The 'old_instance' argument is the instance of the model before the changes.
    For an 'INSERT' op, old_instance and modified_fields are both None.
    The 'new_instance' argument is the instance of the model after the changes.
    For a 'DELETE' op, new_instance and modified_fields are both None.
    the 'modified_fields' is a dictionary containing the fields that were changed.
    The dictionary key is the name of the field and the value is the new value.
    """
    assert dependencies != None
    assert handler != None
    
    for model in dependencies.keys():
        config_models.add(model)
            
    config_handlers.append((dependencies, handler))

last_config_state_path = None

def get_last_config_state_path():
    global last_config_state_path
    if not last_config_state_path:
        last_config_state_dir = "%s/run/" % sdncon.SDN_ROOT
        if not os.path.exists(last_config_state_dir):
            last_config_state_dir = '/tmp'
        last_config_state_path = os.path.join(last_config_state_dir, 'last-config-state')
    return last_config_state_path

def reset_last_config_state():
    path = get_last_config_state_path()
    try:
        os.unlink(path)
    except Exception, _e:
        pass
    

def config_read_state():
    last_config_state = None
    f = None
    try:
        f = open(get_last_config_state_path(), 'r')
        last_config_state_text = f.read()
        last_config_state = json.loads(last_config_state_text)
    except Exception, _e:
        pass
    finally:
        if f:
            f.close()
    return last_config_state

def config_write_state(config_state):
    f = None
    try:
        config_state_text = json.dumps(config_state)
        f = open(get_last_config_state_path(), 'w')
        f.write(config_state_text)
    except Exception, e:
        print "Error writing last config state: %s" % str(e)
    finally:
        if f:
            f.close()

def config_do_insert(sender, new_instance):
    for config_handler in config_handlers:
        dependencies = config_handler[0]
        if sender in dependencies:
            handler = config_handler[1]
            try:
                handler(op='INSERT', old_instance=None, new_instance=new_instance, modified_fields=None)
            except Exception, _e:
                traceback.print_exc()
                
def config_do_update(sender, old_instance, new_instance):
    for config_handler in config_handlers:
        dependencies = config_handler[0]
        if sender in dependencies:
            handler = config_handler[1]
            modified_fields = {}
            fields = dependencies.get(sender)
            if not fields:
                # If no fields were specified for the model then check all of the fields.
                fields = [field.name for field in sender._meta.fields]
                
            for field in fields:
                old_value = getattr(old_instance, field)
                new_value = getattr(new_instance, field)
                if new_value != old_value:
                    modified_fields[field] = new_value
            if modified_fields:
                try:
                    handler(op='UPDATE', old_instance=old_instance, new_instance=new_instance, modified_fields=modified_fields)
                except Exception, _e:
                    traceback.print_exc()


def config_do_delete(sender, instance):
    for config_handler in config_handlers:
        dependencies = config_handler[0]
        if sender in dependencies:
            handler = config_handler[1]
            try:
                handler(op='DELETE', old_instance=instance, new_instance=None, modified_fields=None)
            except Exception, _e:
                traceback.print_exc()

def model_instances_equal(instance1, instance2):
    if instance1 == None:
        return instance1 == instance2
    
    for field in instance1._meta.fields:
        value1 = field.value_from_object(instance1)
        value2 = field.value_from_object(instance2)
        if value1 != value2:
            return False
    return True

def config_check_state():
    from sdncon.controller.notification import do_modify_notification, do_delete_notification
    last_config_state = config_read_state()
    try:
        last_config_instances = last_config_state.get('instances')
    except Exception, _e:
        last_config_instances = {}
    
    current_config_instances = {}
    
    for config_model in config_models:
        try:
            serialized_old_instances = json.dumps(last_config_instances.get(config_model.__name__,[]))
            old_instance_info = serializers.deserialize('json', serialized_old_instances)
            old_instances = [info.object for info in old_instance_info]
        except Exception, _e:
            old_instances = []
            
        new_instances = config_model.objects.all()
        
        for new_instance in new_instances:
            for index, old_instance in enumerate(old_instances):
                if new_instance.pk == old_instance.pk:
                    if not model_instances_equal(new_instance, old_instance):
                        config_do_update(config_model, old_instance, new_instance)
                        do_modify_notification(config_model, new_instance)
                    del old_instances[index]
                    break
            else:
                config_do_insert(config_model, new_instance)
                do_modify_notification(config_model, new_instance)
        
        for deleted_instance in old_instances:
            config_do_delete(config_model, deleted_instance)
            do_delete_notification(config_model, deleted_instance)

        try:
            serialized_new_instances = serializers.serialize("json", new_instances)
            current_config_instances[config_model.__name__] = json.loads(serialized_new_instances)
        except:
            print 'Failed to serialize', config_model.__name__
        
    config_write_state({'instances': current_config_instances})

def config_post_save_handler(sender, **kwargs):
    if not kwargs['raw'] and (kwargs['using'] == "default") and sender in config_models:
        config_check_state()
    
def config_post_delete_handler(sender, **kwargs):
    if kwargs['using'] == 'default' and sender in config_models:
        config_check_state()

def is_config_model(model):
    return model in config_models
 
def init_config():
    post_save.connect(config_post_save_handler)
    post_delete.connect(config_post_delete_handler)
