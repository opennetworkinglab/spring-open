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

import setuptools, os
import sys

os.environ['DJANGO_SETTINGS_MODULE'] = 'sdncon.settings'

def find_files(dirs):
    files = []
    for dir in dirs:
        l = len(os.path.join(*os.path.split(dir)[:-1])) + 1
        for dirpath, dirnames, filenames in os.walk(dir):
            for fn in filenames:
                files.append(os.path.join(dirpath, fn)[l:])
    return files

want_smoke = False

if sys.argv[-1] == '--smoke-tests-only':
    want_smoke = True
    del sys.argv[-1]

setuptools.setup(
    name="sdncon",
    version="0.1.0",
    package_dir={"sdncon": "."},
    packages=["sdncon", "sdncon.controller", "sdncon.rest",
              "sdncon.clusterAdmin", "sdncon.coreui", "sdncon.coreui.templatetags",
              "sdncon.stats", "sdncon.stats_metadata",
              "sdncon.statdropd", "sdncon.syncd",
              "sdncon.apps.cstats", "sdncon.apps.logs",  
              "sdncon.apps.docs", 
             ],
    package_data={"sdncon.coreui": find_files(["coreui/templates", "coreui/img", "coreui/static"]),
                  "sdncon.clusterAdmin": find_files(["clusterAdmin/templates"]),
                  "sdncon.apps.cstats": find_files(["apps/cstats/templates", "apps/cstats/img", "apps/cstats/static"]),
                  "sdncon.apps.logs": find_files(["apps/logs/templates", "apps/logs/img", "apps/logs/static"]),
                  "sdncon.apps.docs": find_files(["apps/docs/templates", "apps/docs/static"]),
                  },
    test_suite= "sdncon.runtests.runtests_smoke" if want_smoke \
                    else "sdncon.runtests.runtests"
    )
