#!/usr/bin/env python
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

import os
import subprocess
import re

VALIDATE_DIR = os.path.dirname(os.path.realpath(__file__))
CLI_PATH = os.path.realpath(os.path.join(VALIDATE_DIR, "../cli.py"))
INPUT_DIR = os.path.join(VALIDATE_DIR, "input_files")
OUTPUT_DIR = "cli_validate"

def validator(input_path, output_path):
    print "Validator started."
    print "Input path: %s" % input_path
    print "Output path: %s" % output_path
    subprocess.call("python %s < %s > %s" % (CLI_PATH, input_path, output_path), shell=True)


def sanitize(path):
    filters = [r"^[^<>#()]+(> .*)", r"^[^<>#()]+(# .*)", r"^[^<>#()]+(\([^()]+\)# .*)",
               r"^Need error", r"^SDNShell", r"^default controller:",
               r"^PERMUTE", r"^SUBMODE", r"^Exiting"]
    patterns = [re.compile(f) for f in filters]
    tmp_path = "%s.tmp" % path
    f1 = open(path)
    f2 = open(tmp_path, "w")

    def matchRegexList(compiledRegexList, line):
        for cr in compiledRegexList:
            if cr.search(line):
                return True
        return False

    for line in f1:
        if matchRegexList(patterns, line):
            continue
        f2.write(line)
    f1.close()
    f2.close()
    os.remove(path)
    os.rename(tmp_path, path)


def main():
    subprocess.call(["rm", "-rf", OUTPUT_DIR])
    subprocess.call(["mkdir", "-p", OUTPUT_DIR])
    for f in os.listdir(INPUT_DIR):
        in_path = os.path.join(INPUT_DIR, f)
        out_path = os.path.join(OUTPUT_DIR, "%s_output" % f)
        validator(in_path, out_path)
        sanitize(out_path)

if __name__ == "__main__":
    main()

