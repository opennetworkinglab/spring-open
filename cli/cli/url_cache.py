#
# Copyright (c) 2012,2013 Big Switch Networks, Inc.
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

import datetime
import json
import utif
import re

#
#
#

cached_urls = {}
cached_urls_age = {}
record_urls = None


def clear_cached_urls():
    """
    Called when all the url's ought to be discarded.

    This is currently done at every command start to reclaim any 
    memory resources.

    Also issued when any update or delete is issued against the
    database, since that means one of our results may be different
    
    """
    global cached_urls, cached_urls_age

    cached_urls = {}
    cached_urls_age = {}


def reset_url(url):
    """
    Remove a url from the cache
    """
    if url in cached_urls:
        del cached_urls[url]
        del cached_urls_age[url]


def get_cached_url(url):
    """
    Simple cache to manage multiple url requests issued within one command cycle
    """
    if url in cached_urls:
        # age doesn't get updated when its touched
        if url in cached_urls_age and \
          cached_urls_age[url]  < datetime.datetime.now():
            # too old
            reset_url(url)
            return None

        return cached_urls[url]
    return None


def record(path):
    global record_urls
    if path == None:
        if record_urls:
            record_urls.close()
        record_urls = None
        return
    # need to validate path
    try:
        record_urls = open(path, 'w')
    except Exception, e:
        print 'Error: can\'t open %s' % path


def save_url(url, entries, age = None):
    """
    Save a url and the response in the cache, associate an age with the
    entry, currently two seconds.
    """
    cached_urls[url] = entries
    if record_urls:
        if type(entries) == str:
            record_urls.write('REST %s %s %s\n' %
                (url, 'STR', entries))
        else:
            # assume its a complex type
            record_urls.write('REST %s %s %s\n' %
                (url, 'JSON', json.dumps(entries)))

    # age out the url in two seconds
    if age == None:
        age = 2

    cached_urls_age[url] = datetime.datetime.now() + datetime.timedelta(0, age)


def command_finished(words):
    if record_urls:
        record_urls.write('COMMAND "%s"\n' %
                          ' '.join([utif.quote_string(x) for x in words]))
