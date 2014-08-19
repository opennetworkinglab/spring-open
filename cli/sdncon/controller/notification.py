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
import urllib2
import json

# FIXME: It's not thread-safe to use globals here,
# but we currently only allow a single thread per Django process,
# so this shouldn't cause a problem. If/when we support multiple
# threads we could fix this by using thread local variables.
# To support true batch notifications across multiple REST calls
# we'd need to actually store the batched up actions in the DB,
# since the individual REST calls would be dispatched to
# multiple processes, so we couldn't use in-memory batching of
# the actions like we do now. The current batch support only
# works for single REST updates/deletes with query parameters to
# select the records that can affect multiple records.
notifications_inited = False
batch_level = 0
actions = None


def begin_batch_notification():
    global batch_level
    global actions
    
    if batch_level == 0:
        actions = []
    batch_level += 1


# FIXME: Could generalize this so it's not hard-coded for SDNPlatform, but
# since this is supposed to be a short-term mechanism for triggering the
# storage notifications in SDNPlatform it's not clear if it makes sense to
# invest time in cleaning this up.
def end_batch_notification(reset=False):
    global batch_level
    global actions
    
    if reset:
        batch_level = 0
    elif batch_level > 0:
        batch_level -= 1
    
    if batch_level == 0:
        if actions:
            url = 'http://localhost:8080/wm/storage/notify/json'
            post_data = json.dumps(actions)
            actions = None
            request = urllib2.Request(url, post_data, {'Content-Type':'application/json'})
            try:
                response = urllib2.urlopen(request)
                # FIXME: Log error if request had an error
                _response_text = response.read() 
            except Exception, _e: 
                # SDNPlatform may not be running, but we don't want that to be a fatal
                # error, so we just ignore the exception in that case.
                pass
        actions = None


def do_action(sender, instance, action):
    # If we're not already in a batch operation, start a local one
    # for just this one change. This is so the code that actually
    # sends the notifications to SDNPlatform can be bottle-necked through
    # end_batch_notification
    local_batch = batch_level == 0
    if local_batch:
        begin_batch_notification()  
    
    last_action = actions[-1] if actions else None
    if (last_action is None or
        # pylint: disable=W0212
        last_action['tableName'] != sender._meta.db_table or
        last_action['action'] != action):
        # pylint: disable=W0212
        last_action = {'tableName': sender._meta.db_table, 'action': action, 'keys': []}
        actions.append(last_action)
    
    keys = last_action['keys']
    if instance.pk not in keys:
        keys.append(instance.pk)
        
    if local_batch:
        end_batch_notification()


def do_modify_notification(sender, instance):
    do_action(sender, instance, 'MODIFY')
    
def do_delete_notification(sender, instance):
    do_action(sender, instance, 'DELETE')

def notification_post_save_handler(sender, **kwargs):
    from sdncon.rest.config import is_config_model
    if not kwargs['raw'] and (kwargs['using'] == "default") and not is_config_model(sender):
        do_modify_notification(sender, kwargs['instance'])


def notification_post_delete_handler(sender, **kwargs):
    from sdncon.rest.config import is_config_model
    if kwargs['using'] == 'default' and not is_config_model(sender):
        do_delete_notification(sender, kwargs['instance'])


def init_notifications():
    global notifications_inited
    if not notifications_inited:
        post_save.connect(notification_post_save_handler)
        post_delete.connect(notification_post_delete_handler)
        notifications_inited = True

