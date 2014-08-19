#
# Copyright (c) 2011,2012,2013 Big Switch Networks, Inc.
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

import setuptools
import os


def all_files_in_subdir(dir, mod, full_list):
    for f in os.listdir(dir):
        path = os.path.join(dir, f)
        if os.path.isdir(path):
            all_files_in_subdir(path, "%s.%s" % (dir, f), full_list)
        elif f.endswith('.py') and f != '__init__.py':
            full_list.append("%s.%s" % (mod, f[:-3]))
    return full_list


def all_modules_in_subdir(dir):
    mod = all_files_in_subdir(dir, '', [])
    return mod


def all_doc_in_subdirs(dir, doc_collect):
    for f in os.listdir(dir):
        path = os.path.join(dir, f)
        if os.path.isdir(path):
            all_doc_in_subdirs(path, doc_collect)
        else:
            doc_collect.append((dir, [path]))
    return doc_collect


def all_documentation(dir):
    return all_doc_in_subdirs(dir, [])


setuptools.setup(
    name="cli",
    version="0.1.0",
    zip_safe=True,
    py_modules=["cli", "model_info_list", "prettyprint", "storeclient",
                "climodelinfo", "vendor", "trace", "timesince", "command",
                "c_actions", "c_data_handlers",  "c_completions", "c_validations",
                "error", "modi", "utif", "midw", "vnsw", "fmtcnv",
                "rest_to_model", "run_config", "tech_support",
                "url_cache", "doc", "sdndb",
                ] + all_modules_in_subdir('desc'),
    data_files=[("data", ["data/oui.txt"])] + all_documentation("documentation"),
    entry_points=dict(console_scripts=["cli = cli:main", "trace = trace:main"]),
    )
