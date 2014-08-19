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

# Django settings for sdncon project.

import os, sys
#from cassandra.ttypes import ConsistencyLevel

DEBUG = True
DEBUG_PROPAGATE_EXCEPTIONS = DEBUG
TEMPLATE_DEBUG = DEBUG

ADMINS = (
    # ('Your Name', 'your_email@domain.com'),
)

MANAGERS = ADMINS

DATABASES = {
    'default': {
        #'ENGINE': 'django_cassandra.db',
        'ENGINE': 'django.db.backends.sqlite3',
        'NAME': 'sdncon',
        'USER': '',                      # Not used with sqlite3.
        'PASSWORD': '',                  # Not used with sqlite3.
        'HOST': '',                      # Set to empty string for localhost. Not used with sqlite3.
        'PORT': '',                      # Set to empty string for default. Not used with sqlite3.
    }
}

# Settings for accessing the stats database/keyspace.
# This does not go through the usual Django model/database layer, but
# instead accesses Cassandra directly.
STATS_DATABASE = {
    'NAME': 'sdnstats',
    #'HOST': '',
    #'USER': '',
    #'PASSWORD': '',
    #'CASSANDRA_REPLICATION_FACTOR': 1
}

# Local time zone for this installation. Choices can be found here:
# http://en.wikipedia.org/wiki/List_of_tz_zones_by_name
# although not all choices may be available on all operating systems.
# On Unix systems, a value of None will cause Django to use the same
# timezone as the operating system.
# If running in a Windows environment this must be set to the same as your
# system time zone.
#TIME_ZONE = 'America/Los_Angeles'
TIME_ZONE = None

# Language code for this installation. All choices can be found here:
# http://www.i18nguy.com/unicode/language-identifiers.html
LANGUAGE_CODE = 'en-us'

SITE_ID = '1'

# Tell Django where is the profile object
#AUTH_PROFILE_MODULE = "sdncon.account.UserProfile"

# If you set this to False, Django will make some optimizations so as not
# to load the internationalization machinery.
USE_I18N = True

# If you set this to False, Django will not format dates, numbers and
# calendars according to the current locale
USE_L10N = True

# Absolute path to the directory that holds media.
# Example: "/home/media/media.lawrence.com/"
SDN_ROOT = "/opt/sdnplatform" if not 'SDN_ROOT' in os.environ else os.environ['SDN_ROOT']
MEDIA_ROOT = "%s/con/lib/python/django/contrib/admin/media/" % SDN_ROOT

# URL that handles the media served from MEDIA_ROOT. Make sure to use a
# trailing slash if there is a path component (optional in other cases).
# Examples: "http://media.lawrence.com", "http://example.com/media/"
MEDIA_URL = ''
STATIC_URL = '/static/'

# URL prefix for admin media -- CSS, JavaScript and images. Make sure to use a
# trailing slash.
# Examples: "http://foo.com/media/", "/media/".
ADMIN_MEDIA_PREFIX = '/media/'

# URL prefixes exempted from login requirements
EXEMPT_URLS = ['/coreui/static/', '/coreui/img/', '/favicon.ico']

# Make this unique, and don't share it with anybody.
SECRET_KEY = 'p+7!f6@8ry2%faunco@u@$wzq7_jpyw4w7+sn=&xpuvo%2!uw('

# List of callables that know how to import templates from various sources.
TEMPLATE_LOADERS = (
    'django.template.loaders.filesystem.Loader',
    'django.template.loaders.app_directories.Loader',
#     'django.template.loaders.eggs.Loader',
)

from django.conf.global_settings import TEMPLATE_CONTEXT_PROCESSORS as GLOBAL_TEMPLATE_CONTEXT_PROCESSORS
TEMPLATE_CONTEXT_PROCESSORS = GLOBAL_TEMPLATE_CONTEXT_PROCESSORS + (
    'sdncon.clusterAdmin.utils.uicontext',
)

MIDDLEWARE_CLASSES = (
    'sdncon.HARedirectMiddleWare',
    'django.middleware.common.CommonMiddleware',
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.middleware.csrf.CsrfViewMiddleware',
    'django.contrib.auth.middleware.AuthenticationMiddleware',
    'sdncon.clusterAdmin.middleware.ClusterAuthenticate',
    'sdncon.clusterAdmin.middleware.RequireAuthMiddleware',
    'django.contrib.messages.middleware.MessageMiddleware',
    'sdncon.SDNConAppLoaderMiddleWare',
)

# Django uses browser-length cookies that expire as soon as the user closes the browser.
SESSION_EXPIRE_AT_BROWSER_CLOSE = True
SESSION_COOKIE_AGE = 2592000 # 30 days, in seconds

ROOT_URLCONF = 'sdncon.urls'

TEMPLATE_DIRS = (
    # Put strings here, like "/home/html/django_templates" or "C:/www/django/templates".
    # Always use forward slashes, even on Windows.
    # Don't forget to use absolute paths, not relative paths.
    os.path.dirname(__file__),
    os.path.join(os.path.dirname(__file__),'clusterAdmin/templates'),
)

# Include applications dynamically from the apps folder

APP_DIR = os.path.join(os.path.dirname(__file__),'apps')
sys.path.append(APP_DIR)

apps_array = [
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.staticfiles',
    #'django.contrib.sites',
    'django.contrib.messages',
    # Uncomment the next line to enable the admin:
    'django.contrib.admin',
    'djangotoolbox',
    'sdncon.rest',
    'sdncon.controller',
    #'django_cassandra',
    'sdncon.coreui',
    'sdncon.clusterAdmin',
    #'sdncon.account',
    'sdncon.ui',
    'sdncon.stats',
    'sdncon.syncd',
    'sdncon.statdropd'
]

directory = os.listdir(APP_DIR)
for filename in directory:
    if not filename.startswith('.'):
        apps_array.append(os.path.basename(filename))

INSTALLED_APPS = tuple(apps_array)

STATS_METADATA_MODULES = []

_stats_meatadata_dir_name = 'stats_metadata'
_stats_metadata_dir = os.path.join(os.path.dirname(__file__), _stats_meatadata_dir_name)
_directory = os.listdir(_stats_metadata_dir)

for _filename in _directory:
    _basename = os.path.basename(_filename)
    if _basename.endswith('.py') and _basename != '__init__.py':
        _module_name = os.path.splitext(_basename)[0]
        STATS_METADATA_MODULES.append('sdncon.%s.%s' % (_stats_meatadata_dir_name, _module_name))
