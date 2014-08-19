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

import os

#
# doc, interface to manage 'markdown' text segments based on 
# documentation tag's.
#

doc = {}

def register_doc(tag, path):
    """
    Its not clear whether the registratio api ought to only name
    the available tags, or whether the api ought to also associate
    text, but it seems that it would be wiser to not keep all the
    documentation in memory
    """

    doc[tag] = path

def add_doc_tags(base, tag_prefix = None):
    path = base
    if tag_prefix == None:
        tag_prefix = []
    elif type(tag_prefix) == str:
        tag_prefix = [tag_prefix]

    if os.path.exists(path):
        for elem in os.listdir(path):
            this_tag_prefix = list(tag_prefix)
            this_tag_prefix.append(elem)
            full_path = os.path.join(path, elem)
            if os.path.isdir(full_path):
                add_doc_tags(full_path, this_tag_prefix)
            elif os.path.isfile(full_path):
                register_doc('|'.join(this_tag_prefix), full_path)
            else:
                print 'add_doc_tags: unknown file type for ', full_path


def get_text(context, tag):
    path = doc.get(tag)
    if path:
        if os.path.exists(path):
            with open(path, 'r') as f:
                return f.read()
        else:
            if context.sdnsh.description:
                print 'doc: tag %s: missing ' % tag
            
    return ''
