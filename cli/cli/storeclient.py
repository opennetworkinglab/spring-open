#
# Copyright (c) 2010,2011,2012,2013 Big Switch Networks, Inc.
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

#
#  module: storeclient.py
#
# This module manages communication with the console, i.e. the REST interface
# of a Big Switch Controller node.

import urllib
import urllib2
import ftplib
import json
import datetime
import time
import traceback
import url_cache


class StringReader():
    # used for ftp, as a replacement for read from an existing file
    def __init__(self, value):
        """
        Value can be a string, or a generator.
        """
        self.value = value
        self.offset = 0
        if type(value) == str or type(value) == unicode:
            self.len = len(value)
        else:
            self.last = None

    def read(self, size = None):
        if size:
            if size > self.len - self.offset:
                size = self.len - self.offset
            result = self.value[self.offset:size]
            self.offset += size
            return result
        # supporing generators.
        if self.last:   # use remainder
            if size > self.len - self.offset:
                size = self.len - self.offset
            result = self.last[self.offset:size]
            self.offset += size
            if self.offset == self.len:
                self.last = None
            return result
        item = value.next()
        len_item = len(item)
        if len_item <= size:
            return item
        # set up remainder
        result = item[:size]
        self.last = item[size:]
        self.offset = 0
        self.len = len(self.last)
        return result


class StoreClient():

    controller = None
    display_rest = False
    display_rest_reply = False
    
    table_read_url = "http://%s/rest/v1/model/%s/"
    entry_post_url = "http://%s/rest/v1/model/%s/"
    user_data_url = "http://%s/rest/v1/data/"

    def set_controller(self,controller):
        self.controller = controller

    def display_mode(self, mode):
        self.display_rest = mode

    def display_reply_mode(self, mode):
        self.display_rest_reply = mode

    def rest_simple_request(self,url, use_cache = None, timeout = None):
        # include a trivial retry mechanism ... other specific
        #  urllib2 exception types may need to be included
        retry_count = 0
        if use_cache == None or use_cache:
            result = url_cache.get_cached_url(url)
            if result != None:
                return result
        while retry_count > 0:
            try:
                return urllib2.urlopen(url, timeout = timeout).read()
            except urllib2.URLError:
                retry_count -= 1
                time.sleep(1)
        # try again without the try...
        if self.display_rest:
            print "REST-SIMPLE:", 'GET', url
        result = urllib2.urlopen(url, timeout = timeout).read()
        if self.display_rest_reply:
            print 'REST-SIMPLE: %s reply "%s"' % (url, result)
        url_cache.save_url(url, result)
        return result
        
    
    def rest_json_request(self, url):
        entries =  url_cache.get_cached_url(url)
        if entries != None:
            return entries

        result = self.rest_simple_request(url)
        # XXX check result
        entries = json.loads(result)
        url_cache.save_url(url, entries)

        return entries

    def rest_post_request(self, url, obj, verb='PUT'):
        post_data = json.dumps(obj)
        if self.display_rest:
            print "REST-POST:", verb, url, post_data
        request = urllib2.Request(url, post_data, {'Content-Type':'application/json'})
        request.get_method = lambda: verb
        response = urllib2.urlopen(request)
        result = response.read()
        if self.display_rest_reply:
            print 'REST-POST: %s reply: "%s"' % (url, result)
        return result
    
    
    def get_table_from_store(self, table_name, key=None, val=None, match=None):
        if not self.controller:
            print "No controller specified. Set using 'controller <server:port>'."
            return
        url = self.table_read_url % (self.controller, table_name)
        if not match:
            match = "startswith"
        if key and val:
            url = "%s?%s__%s=%s" % (url, key, match, urllib.quote_plus(val))
        result = url_cache.get_cached_url(url)
        if result != None:
            return result
        data = self.rest_simple_request(url)
        entries = json.loads(data)
        url_cache.save_url(url, entries)
        return entries


    def get_object_from_store(self, table_name, pk_value):
        if not self.controller:
            print "No controller specified. Set using 'controller <server:port>'."
            return
        url = self.table_read_url % (self.controller, table_name)
        url += (pk_value + '/')
        result = url_cache.get_cached_url(url)
        if result != None:
            return result
        if self.display_rest:
            print "REST-MODEL:", url
        response = urllib2.urlopen(url)
        if response.code != 200:
            # LOOK! Should probably raise exception here instead.
            # In general we need to rethink the store interface and how
            # we should use exceptions.
            return None
        data = response.read()
        result = json.loads(data)
        if self.display_rest_reply:
            print 'REST-MODEL: %s reply: "%s"' % (url, result)
        url_cache.save_url(url, result)
        return result


    # obj_data must contain a key/val and any other required data
    def rest_create_object(self, obj_type, obj_data):
        if not self.controller:
            print "No controller specified. Set using 'controller <server:port>'."
            return
        url_cache.clear_cached_urls()
        url = self.entry_post_url % (self.controller, obj_type)
        data = self.rest_post_request(url, obj_data)
        # LOOK! successful stuff should be returned in json too.
        if data != "saved":
            result = json.loads(data)
            return result
        url_cache.clear_cached_urls()

    def find_object_from_store(self, obj_type, key, val):
        if not self.controller:
            print "No controller specified. Set using 'controller <server:port>'."
            return
        url = self.table_read_url % (self.controller, obj_type)
        result = url_cache.get_cached_url(url)
        if result != None:
            return result
        data = self.rest_simple_request("%s?%s__exact=%s" % (url, key, urllib.quote_plus(val)))
        entries = json.loads(data)
        url_cache.save_url(url, entries)
        return entries

    def rest_query_objects(self, obj_type, query_params=None):
        if not self.controller:
            print "No controller specified. Set using 'controller <server:port>'."
            return
        url = self.table_read_url % (self.controller, obj_type)
        if query_params:
            url += '?'
            # Convert any data:None fields to <id>__isnull=True
            non_null_query_params = dict([[n,v] if v != None else [n + '__isnull', True]
                                          for (n,v) in query_params.items()])
            url += urllib.urlencode(non_null_query_params)
        result = url_cache.get_cached_url(url)
        if result != None:
            return result
        data = self.rest_simple_request(url)
        entries = json.loads(data)
        url_cache.save_url(url, entries)
        return entries
        
    #
    # either must contain a key/val and any other required data
    # of the key must be a dictionary identifying the item to delete.
    def rest_delete_object(self, obj_type, key, val = None):
        dict_ = {}
        url = self.entry_post_url % (self.controller, obj_type)
        if val == None:
            if not type(key) == type(dict_):
                return None
            dict_ = key
        else:
            url += "?%s__exact=%s" % (key, urllib.quote_plus(val))

        # LOOK! I'm not sure this works the way it seems to me it's
        # designed to work. I think the intent is that you can specify
        # query parameters in the key argument which controls which
        # instance(s) should be deleted. But when I try it it seems to
        # always delete all instances, so it seems like the parameters
        # don't filter properly when passed via the POST data as opposed
        # to being specified as query parameters in the URL. The latter
        # way does work -- see rest_delete_objects that follows this.
        data = self.rest_post_request(url, dict_, 'DELETE')
        # LOOK! successful stuff should be returned in json too.
        if data != "deleted":
            dict_ = json.loads(data)
            return dict_
        url_cache.clear_cached_urls()

    def rest_delete_objects(self, obj_type, query_params):
        url = self.entry_post_url % (self.controller, obj_type)
        if query_params:
            url += '?'
            # Convert any data:None fields to <id>__isnull=True
            non_null_query_params = dict([[n,v] if v != None else [n + '__isnull', True]
                                          for (n,v) in query_params.items()])
            url += urllib.urlencode(non_null_query_params)

        data = self.rest_post_request(url, {}, 'DELETE')
        # LOOK! successful stuff should be returned in json too.
        if data != "deleted":
            result = json.loads(data)
            return result
        url_cache.clear_cached_urls()
        
    def rest_update_object(self, obj_type, obj_key_name, obj_key_val, obj_data):
        if not self.controller:
            print "No controller specified. Set using 'controller <server:port>'."
            return
        url = self.entry_post_url % (self.controller, obj_type)
        url += "?%s=%s" % (obj_key_name, urllib.quote_plus(obj_key_val)) # add a query string
        data = self.rest_post_request(url, obj_data)
        # LOOK! successful stuff should be returned in json too.
        result = json.loads(data)
        if result.get('description', '') != "saved":
            return result
        url_cache.clear_cached_urls()

    def set_user_data_file(self, name, text):
        url = self.user_data_url % (self.controller)
        version = 1 # default
        # find the latest version for a name
        existing_data = self.get_user_data_table(name, "latest") 
        if len(existing_data) > 0: # should be at most 1, but just in case...
            version = max([int(f['version']) for f in existing_data]) + 1 # LOOK! race?
        length = len(text)
        # LOOK! what to do about time in a distributed system!
        timestamp = datetime.datetime.utcnow().strftime("%Y-%m-%d.%H:%M:%S")
        url += "%s/timestamp=%s/version=%s/length=%s/" % (name, timestamp, version, length)
        return self.copy_text_to_url(url, text)

    def get_user_data_file(self, name):
        url = self.user_data_url % (self.controller)
        url += name + "/"
        return self.rest_simple_request(url)

    def delete_user_data_file(self, name):
        url = self.user_data_url % (self.controller)
        url += name + "/"
        data = self.rest_post_request(url, {}, 'DELETE')
        if data != "deleted":
            result = json.loads(data)
            return result

    def get_user_data_table(self, name=None, show_version="latest"):
        if not self.controller:
            print "No controller specified. Set using 'controller <server:port>'."
            return None
        url = self.user_data_url % self.controller
        if name:
            url += "?name__startswith=%s" % name
        data = self.rest_simple_request(url)
        new_data = []
        data = json.loads(data)
        latest_versions = {}  # dict of latest version per name
        for d in data:  # list of dicts
            l = d['name'].split('/') # ex: startup/timestamp=2010-11-03.05:51:27/version=1/length=2038
            nd = dict([item.split('=') for item in l[1:]])
            nd['name'] = l[0]
            nd['full_name'] = d['name']
            new_data.append(nd)
            if not nd['name'] in latest_versions or int(nd['version']) > int(latest_versions[nd['name']]):
                latest_versions[nd['name']] = nd['version'] # initialize first time

        # prune if needed to a name or a particular version
        if name:
            new_data = [ nd for nd in new_data if nd['name'].startswith(name) ]
        if show_version == "latest":
            new_data = [ nd for nd in new_data if not int(nd['version']) < int(latest_versions[nd['name']]) ]
        elif show_version != "all":
            new_data = [ nd for nd in new_data if nd['version'] == show_version ]
        return new_data


    # LOOK! looks a lot like a rest_post_request except we don't jsonify and we handle
    # errors differently... refactor?  Same with get_text and rest_simple_request
    def copy_text_to_url(self, url, src_text, message = None):
        post_data = src_text
        if url.startswith('ftp://'):
            url_suffix = url[6:]
            user = 'anonymous'
            password = ''
            if url_suffix.find('@') != -1:
                url_parts = url_suffix.split('@')
                url_user_and_password = url_parts[0]
                url_suffix = url_parts[1]
                if url_user_and_password.find(':') != -1:
                    user_and_password = url_user_and_password.split(':')
                    user = user_and_password[0]
                    password = user_and_password[1]
                else:
                    user = url_user_and_password

            host = url_suffix
            path = None
            if url_suffix.find('/'):
                url_suffix_parts = url_suffix.split('/')
                host = url_suffix_parts[0]
                path = url_suffix_parts[1]
            ftp_target = ftplib.FTP(host, user, password)

            ftp_target.storbinary('STOR %s' % path, StringReader(post_data))
            # apparently, storbinary doesn't provide a return value
            result = { "result" : "success" } # don't display any other error messages
        else:
            request = urllib2.Request(url, post_data, {'Content-Type':'text/plain'})
            request.get_method = lambda: 'PUT'
            if self.display_rest:
                print "REST-TEXT-TO:", request
            response = urllib2.urlopen(request)
            result = response.read()
            if self.display_rest_reply:
                print 'REST-TEXT-TO: %s reply "%s"' % (request, result)
        return result

    def get_text_from_url(self, url):
        if self.display_rest:
            print "REST-TEXT-FROM:", url
        result = urllib2.urlopen(url).read()
        if self.display_rest_reply:
            print 'REST-TEXT-FROM: %s result:"%s"' % (url, result)
        return result
