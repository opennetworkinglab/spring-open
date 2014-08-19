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

from django import template
register = template.Library()

from hashlib import md5
from sdncon.clusterAdmin.utils import isCloudBuild

@register.filter
def gravatar(email):
    try:
        secure = isCloudBuild()
        options='?d=mm&s=48'
        url = 'http://www.gravatar.com/avatar/%s.jpg%s'
        if secure:
            url = 'https://secure.gravatar.com/avatar/%s.jpg%s'
        return  url % (md5(email.lower().strip()).hexdigest(), options)
    except:
        # Filters must fail silently
        return '#'
