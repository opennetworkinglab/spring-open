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

import os, sys
os.environ['DJANGO_SETTINGS_MODULE'] = 'sdncon.settings'
sys.path.insert(0, os.path.dirname(__file__))

test_labels = ['clusterAdmin', "contenttypes", "sessions", "messages"]
smoke_test_labels = ["admin", "rest", "controller", "stats"]

def runtests(tests=test_labels):
    import django.test.utils, django.conf
    test_runner = django.test.utils.get_runner(django.conf.settings)
    if hasattr(test_runner, "run_tests"):
        test_runner = test_runner(verbosity=2).run_tests
    failures = test_runner(tests)
    sys.exit(0 if failures == 0 else 1)

def runtests_smoke():
    return runtests(smoke_test_labels)
