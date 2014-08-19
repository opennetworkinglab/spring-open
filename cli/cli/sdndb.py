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

#
#

import storeclient
import json
import utif

# TODO:
# don't use the python type of the string, use the 
#  schema's type description
#
# associate the complex type with the schema so that
#  during leaf generation, the complex schema type
#  can be used to display the value
#


def string_type(value):
    if type(value) == str or type(value) == unicode:
        return True


def integer_type(value):
    if type(value) == int or type(value) == long:
        return True


def numeric_type(value):
    if (integer_type(value) or 
      type(value) == float or type(value) == complex):
        return True


def atomic_type(value):
    if (string_type(value) or
     numeric_type(value) or
     type(value) == bool):
        return True


def path_adder(prefix, nextfix):
    if prefix == '':
        return nextfix
    return '%s/%s' % (prefix, nextfix)


class SDNDB():
    
    # Notes:
    #
    # The item order of the dictionary-like structures in the schema
    # is actually not order-ed.  The columns of these items, then
    # can't be derived from the schema.
    #

    def __init__(self, modi, sdnsh, pp):
        self.modi = modi
        self.sdnsh = sdnsh              # access to rest apu, needs help
        self.pp = pp                    # access to formats, needs help

        self.known_types = [
                            'INTEGER',
                            'STRING',
                            'BOOLEAN',     # True/False
                            'BINARY',      # bit array
                            'LEAF',
                            'LEAF_LIST',
                            'LIST',
                            'CONTAINER',
                            'REFERENCE',
                           ]

        self.known_data_sources = [
                                    'sdnplatform-module-data-source',
                                    'switch-data-source',
                                    'controller-data-source',
                                    'topology-data-source',
                                    'config'
                                  ]
        self.controller = 'localhost'
        self.sdndb_port = 8082
        self.schema_request()
        self.int64max = 2**63 - 1
        self.int64min = -2**63


    def mm(self, v):
        """
        Quick converter for those values which are some
        variation of int_max for a 64 bit java signed integer,
        and int_min for a 64 bit java signed integer
        """
        if v == self.int64max:
            return '' # '2^63'
        if v == self.int64min:
            return '' # '-2^63'
        return v


    # isolate references to outside entities, ie:
    # sdnsh and pp references need to be collected here
    # in preparation for better times.
    def controller(self):
        return self.sdnsh.controller


    def schema_request(self):
        self.sdndb_port = 8082
        url = ('http://%s:%s/api/v1/schema/controller' %
                (self.controller, self.sdndb_port))
        self.schema = {}
        try:
            print url
            self.schema = self.sdnsh.store.rest_json_request(url)
        except Exception, e:
            print 'BIG TROUBLE IN SDNDB', e
            return
        print self.schema.keys()

        # for types: /api/v1/module/controller

        self.crack_type(self.schema)


    def data_rest_request(self, item):
        url = ('http://%s:%s/api/v1/data/controller/%s' %
               (self.controller, self.sdndb_port, item))
        try:
            rest_result = self.sdnsh.store.rest_simple_request(url)
        except Exception, e:
            print 'URL', url
            print 'Exception: ', item, e
            return

        result = json.loads(rest_result)
        # print result
        # print self.format_table(result, rest_item)
        return result


    def crack_field(self, model, field, field_desc):
        print model, field
        (name, type, base_type_name, base_typedef, module) = \
            (None, None, None, None, None)
        (attributes, child_nodes, data_sources, description) = \
            (None, None, None, None)
        (key_node_nanmes, validator, defaultValueString) = (None, None, None)
        (leaf_type, leaf_schema_node, type_schema_node) = (None, None, None)
        (list_schema_node, mandatory) = (None, None)
        # three fields seem to identify type:
        # 'nodeType', 'baseTypeName', 'baseTypedef'

        for (attr, attr_value) in field_desc.items():   
            if attr == 'name':
                if attr_value != field:
                    print 'Warning: schema %s "name" %s ' \
                            'doesn\'t match field name %s' % \
                            (model, attr_value, field)
            elif attr == 'nodeType':
                type = attr_value
                if type not in self.known_types:
                    print 'Warning: schema: %s:%s unknown type %s' % \
                            (model, field, type)
                else:
                    print model, field, type
            elif attr == 'dataSources':
                data_sources = attr_value
                for source in data_sources:
                    if source not in self.known_data_sources:
                        print 'Warning: schema: %s:%s unknown data source %s' % \
                                (model, field, source)

            elif attr == 'mandatory':
                mandatory = attr_value
            elif attr == 'childNodes':
                child_nodes = attr_value
            elif attr == 'leafType':
                leaf_type = attr_value
            elif attr == 'typeSchemaNode':
                type_schema_node = attr_value
            elif attr == 'keyNodeNames':
                key_node_names = attr_value
            elif attr == 'listElementSchemaNode':
                list_schema_node = attr_value
            elif attr == 'leafSchemaNode':
                leaf_schema_node = attr_value
            elif attr == 'validator':
                validator = attr_value
                print model, field, 'VALIDATOR', validator
            elif attr == 'defaultValueString':
                defaultValueString = attr_value
            elif attr == 'baseTypeName':
                base_type_name = attr_value
            elif attr == 'baseTypedef':
                base_typedef = attr_value
            elif attr == 'attributes':
                attributes = attr_value
            elif attr == 'description':
                description = attr_value
            elif attr == 'module':
                module = attr_value
            else:
                print 'Warning: schema: %s:%s unknown attribute %s' % \
                        (model, field, attr)
                print "   --", attr, attr_value
        

    def crack_container(self, container):
        for (model, model_details) in container.items():
            print 'Model', model, model_details.keys()
            type        = model_details['nodeType']
            name        = model_details['name']
            module      = model_details['module']


            if type == 'LIST':
                child_nodes = model_details['listElementSchemaNode']
                child_nodes = child_nodes['childNodes']
                print '-- ', name, type, module, child_nodes.keys()

                for (field, field_value) in child_nodes.items():
                    self.crack_field(name, field, field_value)
                    #print field, field_value.items()
            elif type == 'CONTAINER':
                child_nodes = model_details['childNodes']

                for (field, field_value) in child_nodes.items():
                    self.crack_field(name, field, field_value)
                    #print field, field_value.items()


    def crack_type(self, item):
        type = item.get('nodeType')
        if type == None:
            return

        if type == 'CONTAINER':
            # REST API envelope.
            container = item.get('childNodes')
            for (envelope_name, envelope_value) in container.items():
                envelope_type = envelope_value.get('nodeType')
                print 'ENVELOPE', container.keys(), envelope_type
                if envelope_type == 'CONTAINER':
                    self.crack_container(envelope_value['childNodes'])


    def post_leaf_node_to_row(self, path, schema, results, row_dict, name = None):
        leaf_type = schema.get('leafType')
        if name == None:
            name = schema.get('name')
        if leaf_type == 'ENUMERATION':
            type_node = schema.get('typeSchemaNode')
            print 'LEAF ENUM', type_node, type_node != None
            enum_result = results
            if type_node:
                if type_node.get('leafType'):
                    enum_values = type_node.get('enumerationSpecifications')
                    if enum_values:
                        for name in enum_values:
                            if name['value'] == enum_result:
                                enum_result = name
            print path, 'LEAF ENUM %s <- %s from %s' % (name, enum_result, results)
            row_dict[name] = str(enum_result)
        elif leaf_type == 'UNION':
            row_dict[name] = str(results)
        elif atomic_type(results):
            print path, 'LEAF %s <- %s' % (name, results)
            row_dict[name] = str(results)
        else:
            print path, 'LEAF MORE DETAILS', schema, type(results), results


    def schema_to_results(self, path, schema, results, row_dict = None, indices = None):
        """
        Generator (iterator) for items in the results, associated with the
        schema passed in.

        'index' is a list of dictionary of items which are intended to be columns in the
        table which must appear for every interior table.
        """
        node_type = schema.get('nodeType')
        name = schema.get('name')
        print path, name, 'TYPE', node_type

        if row_dict == None:
            row_dict = dict()
        if indices == None:
            indices = list()

        if node_type in ['LEAF']:
            self.post_leaf_node_to_row(path, schema, results, row_dict)
        elif node_type == 'LIST':
            row = {} if row_dict == None else dict(row_dict)

            daughter = schema.get('listElementSchemaNode')
            index = daughter.get('keyNodeNames')
            # verify index in list_fields
            list_items = daughter.get('childNodes')
            print path, 'LIST', name, index, list_items.keys()
            yield ('LIST-BEGIN', name, path, indices, row)
            # spath = '%s/%s/%s' % (path, name, index_value)
            # add_fields(depth+1, list_fields)
            for (index_value, result) in results.items():
                print '[]', '%s:%s' % (index, index_value)
                new_row = dict(row)
                new_row['|'.join(index)] = index_value
                new_indices = list(indices) + [{name : index_value}]
                spath = '%s/%s' % (path_adder(path, name), index_value)
                for (item_name, item_value) in list_items.items():
                    if item_name in result:
                        for item in self.schema_to_results(spath,
                                                           item_value, result[item_name],
                                                           new_row, new_indices):
                            yield item
                print 'HERE', new_row
                yield ('LIST-ITEM', name, path, indices, row, new_row)
            yield ('LIST-END', name, path, indices + [{'|'.join(index) : None}], row)
            return
        elif node_type == 'LEAF_LIST':
            #row = {} if row_dict == None else dict(row_dict)
            row = {}
            # verify index in list_fields
            daughter = schema.get('leafSchemaNode')
            last_index = indices[-1]
            if len(last_index.keys()) == 1:
                parent_name = last_index[last_index.keys()[0]]
            print path, 'LEAF-LIST', parent_name, indices, daughter.keys(), last_index
            yield ('LIST-BEGIN', parent_name, path, indices, row)
            # spath = '%s/%s/%s' % (path, name, index_value)
            # add_fields(depth+1, list_fields)
            new_row = dict(row)
            item_schema = daughter.get('typeSchemaNode')
            leaf_node_type = item_schema.get('nodeType')
            if leaf_node_type != 'TYPE':
                print 'LEAF-LIST without interior TYPE node: %s' % leaf_node_type
            else:
                leaf_type = item_schema.get('leafType')
                print 'XXX', results, name
                for item in results:
                    new_indices = list(indices) + [{name : item}]
                    self.post_leaf_node_to_row(path, item_schema, item, row, name)
                    yield ('LIST-ITEM', parent_name, path, new_indices, row, new_row)

            new_indices = list(indices) + [{name : None}]
            print 'XYZ', name, new_indices
            yield ('LIST-END', parent_name, path, new_indices, row)
            return
            
        elif node_type == 'CONTAINER':
            # should abstract name types be added?
            child_nodes = schema.get('childNodes')
            print path, 'CONTAINER', name, child_nodes.keys()
            yield ('CONTAINER-BEGIN', name, path, indices, row_dict)
            spath = path_adder(path, name)
            # add_fields(spath, child_nodes)
            base_dict = dict(row_dict)
            for (child_name, child_value) in child_nodes.items():
                print path, 'CONTAINER PART', child_name, child_name in results
                if child_name in results:
                    for item in self.schema_to_results(spath, child_value, results[child_name],
                                                       row_dict, indices):
                        yield item
            print path, 'CONTAINER DONE', name, row_dict
            yield ('CONTAINER-END', name, path, indices, base_dict, row_dict)
        else:
            print 'TYPE %s NEEDS HELP' % node_type
            print schema
            print results


    def format_table(self, result, name):
        #
        # format a table: paint a table as if generating some
        # table output format from a hierarchial object.
        # 
        # columns are the names of any path members down to the
        # most interior entry.
        # 
        #

        def add_fields(depth, fields):
            for (field_name, field_details) in fields.items():
                # if the column is a list, all the entries underneath need to be added.
                node_type = field_details.get('nodeType')
                type_schema = field_details.get('typeSchemaNode')
                print depth, 'FIELD', field_name, node_type, type_schema
                if node_type in ['LEAF']:
                    if not field_name in columns:
                        columns.append(field_name)
                    else:
                        print 'XXXX IN THERE', field_name
                    if type_schema and field_name not in column_type:
                        column_type[field_name] = type_schema
                elif node_type == 'LIST':
                    daughter = field_details.get('listElementSchemaNode')
                    index = daughter.get('keyNodeNames')
                    # verify index in list_fields
                    list_fields = daughter.get('childNodes')
                    print depth, 'LIST', field_name, index, list_fields.keys()
                    add_fields(depth+1, list_fields)
                elif node_type == 'CONTAINER':
                    # should abstract name types be added?
                    name = field_details.get('name')
                    child_nodes = field_details.get('childNodes')
                    print depth, 'CONTAINER', field_name, child_nodes.keys()
                    add_fields(depth+1, child_nodes)

        schema = self.schemas.get(name)
        if schema == None:
            print 'Missing Schema', name
            print 'Known:', ','.join(self.schemas.keys())
            return
        print schema.keys()
        description = schema.get('description')
        daughter = schema.get
        if schema.get('nodeType') == 'LIST':
            daughter = schema.get('listElementSchemaNode')
            index = daughter.get('keyNodeNames')
            fields = daughter.get('childNodes')
            columns = ['|'.join(index)]
        elif schema.get('nodeType') == 'CONTAINER':
            index = schema.get('keyNodeNames')
            fields = schema.get('childNodes')
            columns = [] # no index for CONTAINER
        else:
            print 'Schema %s, NodeType %s needs root' % (name, schema.get('nodeType'))
        print name, schema.get('nodeType')
        if description:
            print 'Model %s "%s", key: %s, fields: %s' % (name, description, index, fields.keys())
        else:
            print 'Model %s, key %s, fields %s' % (name, index, fields.keys())
        if index:
            # verify the the index items are 
            found_index = [x for x in index if x in fields]
            print 'Verified index', found_index
        column_type = {}
        add_fields(1, fields)
        print columns
        print 'COLUMN TYPE', len(column_type)
        for (column, schema_type) in column_type.items():
            print 'COLUMN TYPE', column, schema_type

        # second verse, same as the first.
        def table_maker(depth, schema, results, a_row):

            node_type = schema.get('nodeType')
            name = schema.get('name')
            print depth, name, 'TYPE', node_type

            if node_type in ['LEAF']:
                leaf_type = schema.get('leafType')
                if leaf_type == 'ENUMERATION':
                    type_mode = schema.get('typeSchemaNode')
                    print 'LEAF ENUM', type_mode != None
                    enum_result = results
                    if type_node:
                        if type_node.get('leafType'):
                            enum_values = type_node.get('enumerationSpecifications')
                            if enum_values:
                                for name in enum_values:
                                    if name['value'] == enum_result:
                                        enum_result = name
                    print depth, 'LEAF ENUM %s <- %s from %s' % (name, enum_result, results)
                    a_row[name] = str(enum_result)
                elif atomic_type(results):
                    print depth, 'LEAF %s <- %s' % (name, results)
                    a_row[name] = str(results)
                else:
                    print depth, 'LEAF MORE DETAILS', schema, type(results), results
            elif node_type == 'LIST':
                row = {} if a_row == None else dict(a_row)

                daughter = schema.get('listElementSchemaNode')
                index = daughter.get('keyNodeNames')
                # verify index in list_fields
                list_items = daughter.get('childNodes')
                print depth, 'LIST', name, index, list_items.keys()
                # add_fields(depth+1, list_fields)
                for (index_value, result) in results.items():
                    print '[]', '%s:%s' % (index, index_value)
                    row['|'.join(index)] = index_value
                    for (item_name, item_value) in list_items.items():
                        if item_name in result:
                            table_maker(depth+1, item_value, result[item_name], row)
                    print 'AROW', row
                    table.append(dict(row))
            elif node_type == 'CONTAINER':
                # should abstract name types be added?
                child_nodes = schema.get('childNodes')
                print depth, 'CONTAINER', name, child_nodes.keys()
                # add_fields(depth+1, child_nodes)
                for (child_name, child_value) in child_nodes.items():
                    print depth, 'CONTAINER PART', child_name
                    if child_name in results:
                        table_maker(depth+1, child_value, results[child_name], a_row)

        table = []
        cols_width = {}

        if type(result) == list and len(result) == 1:
            print 'PRUNE LIST'
            result = result[0]
        table_maker(1, schema, result, {})

        print '+++++++++++++'
        print table
        print '+++++++++++++'

        for column in columns:
            cols_width[column] = len(column)
        for row in table:
            for (item, value) in row.items():
                if item not in cols_width:
                    cols_width[item] = len(value)
                elif len(value) > cols_width[item]:
                    cols_width[item] = len(value)
        
        print 'COLUMNS->', columns
        print 'COLS_WIDTH->', cols_width

        # column header
        line = ''
        for column in columns:
            if column in cols_width:
                line += '%-*s ' % (cols_width[column], column)
        print line
        print '=' * len(line)

        line = ''
        for column in columns:
            type_info = ' ' * cols_width[column]
            if column in column_type:
                ct = column_type[column]
                print column, ct
                if type(ct) == str or type(ct) == unicode:
                    type_info = '%*s' % (cols_width[column], ct)
                elif ct.get('leafType'):
                    leaf_type = ct.get('leafType')
                    if type(leaf_type) == unicode or type(leaf_type) == str:
                        type_info = '%*s' % (cols_width[column], leaf_type)
                    else:
                        print 'CT LEAF_TYPE', ct
                if ct.get('nodeType'):
                    node_type = ct.get('nodeType')
                    if type(node_type) == str or type(node_type) == unicode:
                        if node_type != 'LEAF':
                            type_info = '%*s' % (cols_width[column], node_type)
                    else:
                        type_info = '%*s' % (cols_width[column], node_type['name'])
                if ct.get('name'):
                    type_info = '%*s' % (cols_width[column], ct['name'])

            line += type_info
        print line

        line = ''
        for column in columns:
            if column in cols_width:
                line += '%s|' % ('-' * cols_width[column],)
        print line
        for row in table:
            line = ''
            for column in columns:
                line += '%*s ' % (cols_width[column], row.get(column, ''))
            print line

        return table


    def name_is_compound_key(self, name):
        if name.find('|') != -1:
            return True
        return False


    def table_body_sorter(self, table, sort_columns):
        def sort_cmp(x,y):
            for f in sort_columns:
                if f in x:
                    c = utif.trailing_integer_cmp(x.get(f), y.get(f))
                    if c:
                        return c
            return 0
        return sorted(table, cmp=sort_cmp)


    def table_columns_width(self, table, columns):
        """
        Table is a list of dictionaries.

        Columns is a list of column header names.
        """
        cols_width = {}
        for column in columns:
            cols_width[column] = len(column)
        for row in table:
            for (item, value) in row.items():
                if item not in cols_width:
                    cols_width[item] = len(value)
                elif len(value) > cols_width[item]:
                    cols_width[item] = len(value)
        return cols_width


    def table_header(self, cols_width, title = None, columns = None):
        """
        Print the table headers.
        """
        # column header
        line = ''
        for column in columns:
            if self.name_is_compound_key(column):
                continue
            if column in cols_width:
                line += '%-*s ' % (cols_width[column], column)

        # table title
        if title:
            len_dash_left = (len(line) - len(title) - 2)
            half_left = len_dash_left / 2
            slop = ''
            if len_dash_left & 1:
                slop = ' '
            yield  '=' * half_left + ' %s%s ' % (title, slop) + '=' * half_left

        # finally print the column header
        if line == '':
            yield '--cols empty--'
        else:
            yield line

        line = ''
        for column in columns:
            if self.name_is_compound_key(column):
                continue
            if column in cols_width:
                line += '%s|' % ('-' * cols_width[column],)
        yield line


    def all_columns_except(self, table, except_columns = None):
        all_columns = []
        if except_columns == None:
            except_columns = []
        # now ensure all columns are represented
        for row in table:
            for field in row.keys():
                if self.name_is_compound_key(field):
                    continue
                if field not in except_columns and field not in all_columns:
                    all_columns.append(field)
        return sorted(all_columns)

        
    def table_body(self, table, title = None, columns = None):
        """
        The input table is a list of dictionaries.  From the
        name:value pairs, build a simple output formatter.

        """

        if columns == None:
            columns = []
        else: # use the columns passed in as a basis for sorting
            table = self.table_body_sorter(table, columns)

        # now ensure all columns are represented
        columns += self.all_columns_except(table, columns)

        cols_width = self.table_columns_width(table, columns)

        # waiting for 'yield from'
        # yield from table_header(cols_width, title, column
        for item in self.table_header(cols_width, title, columns):
            yield item

        for row in table:
            line = ''
            for column in columns:
                if not self.name_is_compound_key(column):
                    line += '%-*s ' % (cols_width[column], row.get(column, ''))
            yield line

        return


    def table_title_builder(self, name, indices_list):
        """
        Build a title, based on the table name, then
        added to that are any name:value paris in the
        indices_list, in order, whose value isn't None
        (None currently means the index is from the name of
        a 'CONTAINER', which doesn't require an index)
        """
        title = [name]
        if indices_list:
            for index_dict in indices_list:
                # not using a comprehension here to 
                # keep the text width small.
                for (n,v) in index_dict.items():
                    if v != None:
                        title.append('%s:%s' % (n,v))
        return ' '.join(title)


    def table_index_columns(self, name, indices_list):
        """
        The 'index columns' are the columns which have been
        used as 'keyNodeNames' for each of the 'LIST's.  These
        are handy to move towards the 'left' side of the table
        """
        columns = []
        if indices_list:
            for index_dict in indices_list:
                columns += index_dict.keys()
        return columns


    def schema_of_path(self, path):
        """
        Return the child tree based on a requested path.
        """
        if type(path) == str:
            path = path.split('/')
        
        curr = self.schema
        for element in path:
            node_type = curr.get('nodeType')
            if node_type == 'CONTAINER':
                child_nodes = curr.get('childNodes')
                next = child_nodes.get(element)
            else:
                print 'schema_of_path: need help for ', node_type
                print 'FIND', node_type, path, curr.keys()
                next = None
            if next == None:
                return None
            curr = next
        return curr


    def display(self, path, style = 'table'):
        schema = self.schema_of_path(path)
        if schema == None:
            print 'Unknown Item', path
            return

        result = self.data_rest_request(path)
        if result == None:
            print 'No result for %s' % path
            return

        # print result
        # print self.format_table(result, rest_item)

        print 'SCHEMA-2-RESULT', path
        #print 'SCHEMA-2-RESULT RESULT', result

        tables_names = []   # table names in order.
        tables = {}         # dictionary of tables, indexed by name
        titles = {}         # dictionary of titles, indexed by name
        columns = {}        # dictionary of columns, indexed by name

        # Apply the result to the schema.
        # 'schema_to_results' is an iterator (generator), which
        # returns tuples.
        for row in self.schema_to_results('', schema, result):
            print '^', row
            # return tuple:
            # (action, name, path, indices, row, new_row)
            #    0      1     2      3       4     5
            action = row[0]
            name = row[1]
            if action == 'LIST-BEGIN':
                if name not in tables_names:
                    tables_names.append(name)
                    tables[name] = []
                # ensure table is empty
                # if name in tables:
                    # tables[name] = []
            elif action == 'LIST-ITEM':
                # add the items to the table.
                table_row = dict(row[5])
                for index in row[3]:
                    table_row.update(index) # indices
                if name in tables:
                    tables[name].append(table_row)
                else:
                    tables[name] = [table_row]
            elif action == 'LIST-END':
                # display the result
                if name in tables:
                    titles[name] = self.table_title_builder(name, row[3])
                    columns[name] = self.table_index_columns(name, row[3])
                    print 'INDEX', name, row[3], columns[name]

        # validation --
        for table in tables_names:
            if not table in tables.keys():
                print 'List of tables doesn''t match tables keys'
                print tables_names, len(tables_names)
                print tables.keys(), len(tables.keys())

        separator = None
        # select style.
        if style == 'list':
            prefix = '    '
            for (table_name, table_details) in tables.items():
                cols = 79
                first_columns = columns[table_name]
                last_columns = self.all_columns_except(table_details,
                                                       first_columns)
                if separator != None:
                    print separator
                for row in table_details:
                    row_lines = 0
                    line = table_name + ' '
                    for item_name in first_columns + last_columns:
                        item_value = row.get(item_name)
                        if item_value == None:
                            continue
                        next_item = '%s: %s ' % (item_name, item_value)
                        if len(line) + len(next_item) > cols:
                            print line
                            line = prefix
                            row_lines += 1
                        line += next_item
                    if line != prefix:
                        print line
                    if row_lines:
                        print ''
                separator = ''

        elif style == 'table':
            # now print the tables.
            for table_name in tables_names:
                if separator != None:
                    print separator
                if len(table_name) > 1:
                    title = titles[table_name]

                if len(tables[table_name]) == 0:
                    if len(table_name) > 1:
                        print table_name, 'None.'
                    else:
                        print 'None.'
                else:
                    title = table_name
                    for item in self.table_body(tables[table_name],
                                                title,
                                                columns[table_name]):
                        print item
                separator = ''
        else:
            print 'sdndb:display unknown style %s' % style

    def schema_detailer_validators(self, type_schema_node):
        """
        Result is a dictionary of validator_name:...

        To display these, use somethng like:
        ' '.join(['%s:%s' % (n,v) for (n,v) in v_dict]
        """
        v_dict = {}

        for validator in type_schema_node.get('typeValidator', []):
            kind = validator.get('type')
            if kind == 'RANGE_VALIDATOR':
                kind = 'range'
            elif kind == 'LENGTH_VALIDATOR':
                kind = 'length'
            elif kind == 'ENUMERATION_VALIDATOR':
                kind = 'enum'
            elif kind == 'PATTERN_VALIDATOR':
                kind = 'pattern'
            else:
                print 'Validator Kind unknown:', kind
                continue

            if not kind in v_dict:
                v_dict[kind] = []

            if kind == 'range' or kind == 'length':
                for range in validator.get('ranges', []):
                    v_dict[kind].append('(%s:%s)' %
                                        (self.mm(range['start']),
                                         self.mm(range['end'])))
            elif kind == 'pattern':
                v_dict[kind].append(validator.get('pattern'))
            elif kind == 'enum':
                name_dict = validator.get('names')
                v_dict[kind].append(','.join(['[%s:%s]' % (n,v) for (n,v) in name_dict.items()]))
        return v_dict
  

    def schema_detailer(self, schema, depth = None):
        if depth == None:
            depth = 0
        indent = '  ' * depth

        name = schema.get('name')
        node_type = schema.get('nodeType')
        if node_type == 'LEAF':
            if self.sdnsh.description:
                print indent, 'LEAF', schema
            leaf_type = schema.get('leafType')
            mandatory = schema.get('mandatory')
            type_schema_node = schema.get('typeSchemaNode', {})
            v_dict = self.schema_detailer_validators(type_schema_node)
            yield ('%s%s LEAF type: %s mandatory: %s %s' %
                   (indent, name, leaf_type, mandatory,
                    ' '.join(["%s:%s" % (n,','.join(v)) for (n,v) in v_dict.items()])))

            if leaf_type == 'UNION':
                nodes = type_schema_node.get('typeSchemaNodes')
                for node in nodes:
                    v_dict = self.schema_detailer_validators(node)
                    yield ('  %s%s TYPE %s' % (indent, node.get('name'),
                           ' '.join(["%s:%s" % (n,','.join(v)) for (n,v) in v_dict.items()])))

        elif node_type == 'LEAF_LIST':
            leaf_node = schema.get('leafSchemaNode')
            mandatory = schema.get('mandatory')
            base_type = leaf_node.get('leafType')
            type_schema_node = leaf_node.get('typeSchemaNode', {})
            v_dict = self.schema_detailer_validators(type_schema_node)

            yield ('%s%s: LEAF-LIST mandatory %s LIST of %s %s' %
                    (indent, name, mandatory, base_type,
                     ' '.join(["%s:%s" % (n,','.join(v)) for (n,v) in v_dict.items()])))
        elif node_type == 'LIST':
            node = schema.get('listElementSchemaNode')
            elements_key = ''
            if node:
                key = node.get('keyNodeNames')
                if key:
                    elements_key = ' of %s' % ', '.join(key)

            child_nodes = node.get('childNodes', [])
            yield '%s%s: LIST %s ITEMS <%s>' % (indent, name, elements_key,
                                              ', '.join(child_nodes))
            for (child, value) in child_nodes.items():
                for item in self.schema_detailer(value, depth + 1):
                    yield item
        elif node_type == 'CONTAINER':
            child_nodes = schema.get('childNodes', [])
            yield '%s%s: CONTAINER ITEMS <%s>' % (indent, name,
                                                  ', '.join(child_nodes.keys()))
            for (child, value) in child_nodes.items():
                for item in self.schema_detailer(value, depth + 1):
                    yield item
        else:
            print 'unknown type', node_type


    def schema_detail(self, path):
        print 'schema_detail:', path
        schema = self.schema_of_path(path)
        for item in self.schema_detailer(schema):
            yield item
        
        return 
