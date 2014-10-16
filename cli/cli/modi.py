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

import sys
# from midw import *

#
# MODel Information (modi)
#

class Modi():

    def __init__(self, sdnsh, cli_model_info):
        self.sdnsh = sdnsh

        self.obj_type_info_dict = {}
        self.obj_types = []
        self.obj_keys = {}

        # self.cli_model_info = CliModelInfo()
        self.cli_model_info = cli_model_info

        self.init_obj_type_info_dict(cli_model_info)
        self.init_foreign_key_xref()
        self.init_alias_obj_type_xref()

    @staticmethod
    def _line():
        # pylint: disable=W0212
        f = sys._getframe().f_back
        return '[%s:%d]' % (f.f_code.co_filename, f.f_lineno)
    #
    # --------------------------------------------------------------------------------

    def init_obj_type_info_dict(self, cli_model_info):
        """
        When obj_type_info_dict is indexed by the table/model name,
        the value is a dictionary.  That dictionary has several keys:
        
        - 'cascade_delete' True/False, set to true when foreign key's in this
                           obj_type will be used o identify rows to delete based
                           on rows removed from the parent table.
        - 'force_delete' True/False, set to true when foreign keys in this
                         obj_type are allowed to be null, but when they are not,
                         the rows identified by cascade_delete must be deleted
                         when rows are removed from the parent table
        - 'source' set to 'user-config' for obj_types which user's configure
                   set to 'debug-only' to enable viewing object only in debug mode
      
        The 'fields' indexed result returns a dictionary, each of which
        is indexed by the names of the fields within the table/model. further
        indicies describe details about that field within the table:
        
        Some of the key's are intended to be added to the dictionary
        to further decorate details about the field.  The objective is
        to modify cli behavior, or override the model details.  These
        values, tho, ought to really come from the model description.

        - 'verbose-name' identfies an additonal name for the field, inteneded
                         to be more descriptive

        - 'hex' True/False, True for fields when a hex value can
                         be replaced with a decimal value
        - 'edit' True/Falue,  False when a field cannot be edited within
                         the nested config mode.

        - 'validate' function to call to validate the field value

        Various key's use '_' vs the typical '-' since they're 
        currently constructed via the django model description

        - 'primary_key' identifes this field as the key for the table/model.
        - 'has_rest_model'  True/False
        - 'json_serialize_string'  True/False
        - 'help_text'  String providing more clues about the intent of this field
        - 'type' various values, intended to be populated by tools/extract_model.py
                         allowing access of the type to the cli.  the allows the
                         cli to find foreign_keys, and possibly determine
        - 'max_length' 
        
        Other key decorations:  sorting

        The 'field_ordering' dictionary associated with the name of the table/model
        is used to order the output.
        
        Also populates the self.obj_keys[], which is a dictionary mapping the table/model
        name to the name of the storage key (table/model's column) for that table.

        @param cli_model_info instantiated class CliModelInfo()
        """
        self.obj_type_info_dict = self.cli_model_info.get_complete_obj_type_info_dict()
        self.obj_types = [k for (k, v) in self.obj_type_info_dict.items()
                          if 'has_rest_model' in v]
        for (k, d) in self.obj_type_info_dict.items():
            if not 'fields' in d:
                print '%s: Missing "fields"' % k
                continue
            candidate_keys = [f for f in d['fields'].keys()
                              if d['fields'][f].get('primary_key', False)]
            if len(candidate_keys) > 0:
                self.obj_keys[k] = candidate_keys[0]

    #
    # --------------------------------------------------------------------------------
    
    def init_foreign_key_xref(self):
        """
        Walk through the obj_type_info_dict, looking for foreign keys.
        Build a cross references, so that a <obj_type, field> can be
        used to identify all the obj_types which reference that field.
      
        To allow for both <obj_type, field> to be used to index
        self.foreign_key_xref, two levels of dictionaries are built.
        The first maps from obj_type to a dictionary of fields, and
        the fields dictionary has values which are lists.  Each of the
        lists has two members: the <obj_type, field>, which will be
        a foreign key to the original pair.
      
        This allows identificaion of fields which have single foreign
        key xref's, which can then be used to identify "sub-modes"
        for particular obj_types.
        """

        self.foreign_key_xref = {}
        for (obj_type, obj_type_dict) in self.obj_type_info_dict.items():
            if not 'fields' in obj_type_dict:
                print '%s: Missing "fields"' % obj_type
                continue
            for (fn, fd) in obj_type_dict['fields'].items():
                if 'type' in fd:
                    if fd['type'] == 'ForeignKey':
                        ref_foreign_obj_type = fd['rel_obj_type']
                        ref_foreign_field = fd['rel_field_name']
                        if not ref_foreign_obj_type in self.foreign_key_xref:
                            self.foreign_key_xref[ref_foreign_obj_type] = {}
                        if not ref_foreign_field in self.foreign_key_xref[ref_foreign_obj_type]:
                            self.foreign_key_xref[ref_foreign_obj_type][ref_foreign_field] = []
                        self.foreign_key_xref[ref_foreign_obj_type][ref_foreign_field].append(
                            [obj_type, fn])

    #
    # --------------------------------------------------------------------------------
    
    def init_alias_obj_type_xref(self):
        """
        Alias obj types have a non-compound primary key, a foreign key, and
        possibly 'DateTimeField' fields, but no other fields.  These can
        be identified by scanning the dictionary.

        The alias_obj_type_xref dictionary is indexed by the obj_type referenced
        by the foreign key of the original obj_type (its a cross ref)
        """

        self.alias_obj_type_xref = {}
        self.alias_obj_types = []
        for (obj_type, obj_type_dict) in self.obj_type_info_dict.items():
            if not 'fields' in obj_type_dict:
                print '%s: Missing "fields"' % obj_type
                continue
            foreign_key_obj_type = None
            foreign_key_count = 0
            other_types = False
            for (fn, fd) in obj_type_dict['fields'].items():
                # 'Idx' field is only for display counting of rows
                if fn != 'Idx' and 'type' in fd:
                    if fd['type'] == 'ForeignKey':
                        foreign_key_count += 1
                        foreign_key_obj_type = fd['rel_obj_type']
                    elif 'primary_key' in fd:
                        if self.is_compound_key(obj_type, fn):
                            other_types = True
                    elif fd['type'] != 'DateTimeField':
                        other_types = True
            if foreign_key_count == 1 and other_types == False:
                self.alias_obj_types.append(obj_type)
                if not foreign_key_obj_type in self.alias_obj_type_xref:
                    self.alias_obj_type_xref[foreign_key_obj_type] = []
                self.alias_obj_type_xref[foreign_key_obj_type].append(
                    obj_type
                )
            #
            # if the primariy key is a compound key, and the first item
            # is also a foreign key here, then allow the first item to
            # match up with the alias.  currenly, only use the first item
            # since the 'startswith' can use used to find the assocaited
            # members
            #
            elif foreign_key_count >= 1:
                key = self.obj_keys[obj_type]
                compound_fields = self.compound_key_fields(obj_type, key)
                if compound_fields:
                    first_field = compound_fields[0]
                    if first_field in self.obj_type_foreign_keys(obj_type):
                        #
                        # This is nasty -- assuming that the foreign_key's table
                        # name will then have an alias table associated with it.
                        # Perhaps some model field can help describe these
                        # associations, something like 'allow-alias: <obj_type>'
                        if first_field == 'vns':
                            pass
                        if not "%s-alias" % first_field in self.obj_type_info_dict:
                            # only build references to tables which exist.
                            pass
                        elif obj_type in self.alias_obj_type_xref:
                            self.alias_obj_type_xref[obj_type] += ["%s-alias" % first_field]
                        else:
                            self.alias_obj_type_xref[obj_type] = ["%s-alias" % first_field]

    #
    # --------------------------------------------------------------------------------

    def pk(self, obj_type):
        """
        Return the primary key name for the object

        @param obj_type string, name of the object-type (ie: 'host', 'switch')
        """

        # Raise an exception when the name doesn't exist?
        return self.obj_keys.get(obj_type, None)

    #
    # --------------------------------------------------------------------------------

    def obj_type_exists(self, obj_type):
        """
        Return True if there's details about obj_type in obj_types_info_dict
        """
        return obj_type in self.obj_type_info_dict.keys()
    
    #
    # --------------------------------------------------------------------------------

    def obj_type_has_model(self, obj_type):
        """
        Return True if the obj_type is serviced via the model rest api
        (in other words, there's a db table which supports this model)
        """
        return obj_type in self.obj_types
    
    #
    # --------------------------------------------------------------------------------

    def obj_type_has_url(self, obj_type):
        """
        Returns a url suffix describing a path which returns the
        data associated with thie obj_type
        """
        if obj_type in self.obj_type_info_dict:
            return self.obj_type_info_dict[obj_type].get('url', None)
        return None
    
    #
    # --------------------------------------------------------------------------------
    
    def is_foreign_key(self, obj_type, field):
        """
        Return True when the field within the obj_type is a foreign key.

        @param obj_type string, name of the object-type
        @param field string, field within the object-type
        """
        if not obj_type in self.obj_type_info_dict:
            return False
        if not 'fields' in self.obj_type_info_dict[obj_type]:
            return False

        field_info = self.obj_type_info_dict[obj_type]['fields'].get(field, [])
        if 'type' in field_info:
            if field_info['type'] == 'ForeignKey':
                return True
        return False

    #
    # --------------------------------------------------------------------------------
    
    def is_integer_field(self, obj_type, field):
        """
        Return True when the type associated with the obj_type's field is an integer

        @param obj_type string, name of the object-type
        @param field string, field within the object-type
        """

        if not obj_type in self.obj_type_info_dict:
            return False
        if not 'fields' in self.obj_type_info_dict[obj_type]:
            return False

        field_info = self.obj_type_info_dict[obj_type].get('fields', [])[field]
        if 'type' in field_info:
            if field_info['type'] == 'IntegerField':
                return True
        return False


    #
    # --------------------------------------------------------------------------------

    def is_primary_key(self, obj_type, field):
        """
        Return true when the obj_type's field is a primary key

        @param obj_type string, name of the object-type
        @param field string, field within the object-type
        """

        if not obj_type in self.obj_type_info_dict:
            return False
        if not 'fields' in self.obj_type_info_dict[obj_type]:
            return False

        field_info = self.obj_type_info_dict[obj_type].get('fields',[])[field]
        if 'primary_key' in field_info:
            return field_info['primary_key'] # should be true if exists
        return False

    #
    # --------------------------------------------------------------------------------
    
    def is_field_editable(self, obj_type, field):
        """
        Return True if the field is editable.  Default is True.

        @param obj_type string, name of the object-type
        @param field string, field within the object-type
        """

        if not obj_type in self.obj_type_info_dict:
            return False
        if not 'fields' in self.obj_type_info_dict[obj_type]:
            return False

        field_info = self.obj_type_info_dict[obj_type].get('fields',[])[field]
        if 'edit' in field_info:
            return field_info['edit'] # should be False if exists
        return True

    #
    # --------------------------------------------------------------------------------
    
    def is_editable(self, obj_type, field):
        """
        Return true if the obj_type/field is available for editing
        Excludes foreign keys, and primary keys (unless edit: True
        is specifically enabled for the field)

        @param obj_type string, name of the object-type
        @param field string, field within the object-type
        """

        if not obj_type in self.obj_type_info_dict:
            return False
        if not 'fields' in self.obj_type_info_dict[obj_type]:
            return False

        if not field in self.obj_type_info_dict[obj_type]['fields']:
            return False
        field_info = self.obj_type_info_dict[obj_type]['fields'][field]
        if 'edit' in field_info and field_info['edit'] == True:
            return True
        if self.is_foreign_key(obj_type, field):
            return False
        if self.is_primary_key(obj_type, field):
            return False
        if not self.is_field_editable(obj_type, field):
            return False
        return True

    #
    # --------------------------------------------------------------------------------

    def obj_type_disable_edit(self, obj_type, field):
        """
        Mark an obj_type's field as not being directly editable.
        When the command descriptions are used for all obj_type's edit, the need
        for 'disabling edit' ought to disappear
        """
        if not obj_type in self.obj_type_info_dict:
            return False
        if not 'fields' in self.obj_type_info_dict[obj_type]:
            self.obj_type_info_dict[obj_type]['fields'] = {}
        if not field in self.obj_type_info_dict[obj_type]['fields']:
            self.obj_type_info_dict[obj_type]['fields'][field] = {}
        self.obj_type_info_dict[obj_type]['fields'][field]['edit'] = False

    #
    # --------------------------------------------------------------------------------

    def is_marked_searchable(self, obj_type, field):
        """
        Return true if a field is searchable.

        This ought to be true for any fields which is part of the
        primary key construction.

        This predicate, however, in intended to look for fields
        which are identified by the model as being searchable,
        even when the field doesn't appear in the primary key.

        @param obj_type
        @param field
        """

        if not obj_type in self.obj_type_info_dict:
            return False
        if not 'fields' in self.obj_type_info_dict[obj_type]:
            return False
        if not field in self.obj_type_info_dict[obj_type]['fields']:
            return False

        key = self.pk(obj_type)
        if self.is_compound_key(obj_type, key):
            if field in self.compound_key_fields(obj_type, key):
                return True
            
        field_info = self.obj_type_info_dict[obj_type]['fields'][field]
        if 'searchable' in field_info:
            return field_info['searchable'] == True
        return False

    #
    # --------------------------------------------------------------------------------
    
    def get_obj_type_field_case_sensitive(self, obj_type, field):
        """
        Return true if a field is case sensitive.

        @param obj_type
        @param field
        """

        if not obj_type in self.obj_type_info_dict:
            return None
        if not 'fields' in self.obj_type_info_dict[obj_type]:
            return None
        if not field in self.obj_type_info_dict[obj_type]['fields']:
            return None

        field_info = self.obj_type_info_dict[obj_type]['fields'][field]
        if 'case' in field_info:
            return field_info['case']
        return None


    #
    # --------------------------------------------------------------------------------
    
    def set_obj_type_field_case_sensitive(self, obj_type, field, case):
        """
        Set case sensitivity for a field in an obj_type

        @param obj_type
        @param field
        @param case either 'upper', or 'lower'
        """

        if case not in ['upper', 'lower']:
            print 'set_case_sensitive: obj_type %s field %s case %s ' \
                'case not upper/lower' % (obj_type, field, case)
            return

        if not obj_type in self.obj_type_info_dict:
            self.obj_type_info_dict[obj_type] = {}
        if not 'fields' in self.obj_type_info_dict[obj_type]:
            self.obj_type_info_dict[obj_type]['fields'] = {}
        if not field in self.obj_type_info_dict[obj_type]['fields']:
            self.obj_type_info_dict[obj_type]['fields'][field] = {}
        self.obj_type_info_dict[obj_type]['fields'][field]['case'] = case


    #
    # --------------------------------------------------------------------------------
    
    def is_hex_allowed(self, obj_type, field):
        """
        Return true if the obj_type/field allows hex instead of decimal

        @param obj_type string, name of the object-type
        @param field string, field within the object-type
        """

        if not obj_type in self.obj_type_info_dict:
            return False
        if not 'fields' in self.obj_type_info_dict[obj_type]:
            return False

        field_info = self.obj_type_info_dict[obj_type]['fields'][field]
        if 'hex' in field_info:
            return field_info['hex'] # likely False if exists
        if self.is_integer_field(obj_type, field):
            return True
        return False

    #
    # --------------------------------------------------------------------------------

    def is_field_boolean(self, obj_type, field):
        """
        Return true if the obj_type/field is a boolean type

        @param obj_type string, name of the object-type
        @param field string, field within the object-type
        """

        if not obj_type in self.obj_type_info_dict:
            return False
        if not 'fields' in self.obj_type_info_dict[obj_type]:
            return False

        field_info = self.obj_type_info_dict[obj_type]['fields'][field]
        if 'type' in field_info:
            return field_info['type'] == 'BooleanField'
        return False

    #
    # --------------------------------------------------------------------------------
    # is_field_string
    #  Return true if the obj_type/field is a character (CharType) type
    #
    def is_field_string(self, obj_type, field):
        """

        @param obj_type string, name of the object-type
        @param field string, field within the object-type
        """

        if not obj_type in self.obj_type_info_dict:
            return False
        if not 'fields' in self.obj_type_info_dict[obj_type]:
            return False

        field_info = self.obj_type_info_dict[obj_type]['fields'][field]
        if 'type' in field_info:
            return field_info['type'] == 'CharField'
        return False

    #
    # --------------------------------------------------------------------------------
    # is_null_allowed
    #  Return true if the obj_type/field is allowed to be null.
    #
    def is_null_allowed(self, obj_type, field):
        """

        @param obj_type string, name of the object-type
        @param field string, field within the object-type
        """

        if not obj_type in self.obj_type_info_dict:
            return False
        if not 'fields' in self.obj_type_info_dict[obj_type]:
            return False

        field_info = self.obj_type_info_dict[obj_type]['fields'][field]
        if 'null' in field_info:
            return field_info['null']
        return False

    #
    # --------------------------------------------------------------------------------
    
    def obj_type_fields(self, obj_type):
        """
        Return a list of field names (strings) for the obj_type,
        includes all fields, including primary keys and foreign keys
        """

        if obj_type in self.obj_type_info_dict:
            if not 'fields' in self.obj_type_info_dict[obj_type]:
                return []
            return self.obj_type_info_dict[obj_type]['fields'].keys()
        return []
    #
    # --------------------------------------------------------------------------------
    
    def obj_type_has_field(self, obj_type, field):
        """
        Return true if the obj_type has a field named 'field'

        @param obj_type string, name of the object-type
        @param field string, field within the object-type
        """

        if not obj_type in self.obj_type_info_dict:
            return False
        if not 'fields' in self.obj_type_info_dict[obj_type]:
            return False

        if obj_type in self.obj_type_info_dict:
            fields_info = self.obj_type_info_dict[obj_type]['fields']
            if field in fields_info:
                return True
        return False

    #
    # --------------------------------------------------------------------------------

    def obj_type_config_fields(self, obj_type):
        """
        For an obj_type, return a list of fields which are possibly user configurable.

        @param obj_type string, name of the object-type
        """

        if obj_type in self.obj_type_info_dict:
            if not 'fields' in self.obj_type_info_dict[obj_type]:
                return []
            fields_info = self.obj_type_info_dict[obj_type]['fields']
            return [x for x in fields_info if self.is_editable(obj_type, x)]
        return []


    #
    # --------------------------------------------------------------------------------

    def obj_type_show_this(self, obj_type):
        """
        Return a list of addition types to display for 'show this'

        @param obj_type string, name of the object-type
        """

        if obj_type in self.obj_type_info_dict:
            if 'show-this' in self.obj_type_info_dict[obj_type]:
                result = self.obj_type_info_dict[obj_type]['show-this']
                if type(result) == str or type(result) == 'unicode':
                    return [result]
                return result
        return []

    #
    # --------------------------------------------------------------------------------
    
    def is_cascade_delete_enabled(self, obj_type):
        """
        Cascade is enabled for an obj_type by setting 'cascade_enabled': True for
        the primary key of an obj_type.

        @param obj_type string, name of the object-type
        """

        if not obj_type in self.obj_type_info_dict:
            return False

        if 'cascade_delete' in self.obj_type_info_dict[obj_type]:
            return self.obj_type_info_dict[obj_type]['cascade_delete']
        return False

    def is_force_delete_enabled(self, obj_type):
        """
        Force delete is enabled for an obj_type by setting 'force_delete': True
        for the primary key of an obj_type.

        @param obj_type string, name of the object-type
        """

        if not obj_type in self.obj_type_info_dict:
            return False

        if 'force_delete' in self.obj_type_info_dict[obj_type]:
            return self.obj_type_info_dict[obj_type]['force_delete']
        return False

    #
    # --------------------------------------------------------------------------------
    
    def cascade_delete_set_enable(self, obj_type):
        """
        Enable cascade_delete for an obj_type
        """
        if not obj_type in self.obj_type_info_dict:
            self.obj_type_info_dict[obj_type] = {}
        self.obj_type_info_dict[obj_type]['cascade_delete'] = True

    def cascade_delete_enable_force(self, obj_type):
        """
        Force cascade_delete for an obj_type
        """
        if not obj_type in self.obj_type_info_dict:
            self.obj_type_info_dict[obj_type] = {}
        self.obj_type_info_dict[obj_type]['cascade_delete'] = True
        self.obj_type_info_dict[obj_type]['force_delete'] = True

    #
    # --------------------------------------------------------------------------------
    
    def has_display_field(self, obj_type, field):
        """
        Determine if a particular obj_type has a particular field on the
        display (ie: during a show command).  Uses the 'field_orderings'
        of the obj_type_info_dict.
      
        Currently used to deterine whether an alias table needs to be
        re-cached in preparation for the display of some obj_type

        @param obj_type string, name of the object-type
        """

        if obj_type in self.obj_type_info_dict and \
          'field_orderings' in obj_type in self.obj_type_info_dict[obj_type]:
            order = self.obj_type_info_dict[obj_type]['field_orderings']['default']
            return field in order
        return False

    #
    # --------------------------------------------------------------------------------
    
    def is_obj_type_source_not_user_config(self, obj_type):
        """
        Return True if the obj_type is intended to be configured,
        some tables are intended to be written by sdnplatform as a way
        of presenting information; it makes no sense for the cli
        to present these tables to the user as configurable.
      
        keep in mind that this returns True only if 'source' exists,
        and the source isn't get to user-config.  This means that
        for a table to be excluded, 'source' must be added, and
        it must be set to something other than 'user-config'

        @param obj_type string, name of the object-type
        """

        if obj_type in self.obj_type_info_dict and \
          'source' in self.obj_type_info_dict[obj_type]:
            if self.obj_type_info_dict[obj_type]['source'] != 'user-config':
                if self.sdnsh.debug and \
                  self.obj_type_info_dict[obj_type]['source'] == 'debug-only':
                    return False
                return True
        return False

    #
    # --------------------------------------------------------------------------------
    
    def is_obj_type_source_debug_only(self, obj_type):
        """
        Return True if the obj_type is marked to be viewed only in debug mode

        @param obj_type string, name of the object-type
        """

        if obj_type in self.obj_type_info_dict and \
          'source' in self.obj_type_info_dict[obj_type]:
            if self.obj_type_info_dict[obj_type]['source'] == 'debug-only':
                return True
        return False

    #
    # --------------------------------------------------------------------------------

    def obj_type_source_set_debug_only(self, obj_type):
        """
        Set the source for the obj-type
        """

        if obj_type in self.obj_type_info_dict:
            self.obj_type_info_dict[obj_type]['source'] = 'debug-only'

    #
    # --------------------------------------------------------------------------------
    
    def compound_key_text(self, obj_type, field):
        """
        Return a text string which describes the construction of a compound key.
        The first character is a '#' when this returned field describes a
        compound key.  The second character is the separator for the field
        itself (not the separator for the fields described by this text
        field).  The remainder is a concatenation of the fields names which
        are used to construct this field.
      
        Currently compound keys are identified through the help_text.
        The help text isn't displayed to the user for primary since the value
        of the key for compound key's must be constructed by the cli, and then
        the user doesn't directly modify or search the compound value.
      
        When the field is itself a foreign key, and no text string exists to
        describe the construction of the compound key, its possible that the foreign
        key's value is itself a compound key.  Peek at the original foreign
        key to see if it has a field identifying the layout of the compound key.

        @param obj_type string, name of the object-type
        @param field string, field within the object-type
        """

        if not obj_type in self.obj_type_info_dict:
            return None
        if not 'fields' in self.obj_type_info_dict[obj_type]:
            return None
        if not field in self.obj_type_info_dict[obj_type]['fields']:
            return None

        field_info = self.obj_type_info_dict[obj_type]['fields'][field]
        if 'help_text' in field_info:
            if field_info['help_text'][0] == '#':
                return field_info['help_text']
        if self.is_foreign_key(obj_type, field):
            (fk_obj_type, fk_name) = self.foreign_key_references(obj_type, field)
            field_info = self.obj_type_info_dict[fk_obj_type]['fields'][fk_name]
            if field_info.get('help_text', ' ')[0] == '#':
                return field_info['help_text']
        return None

    #
    # --------------------------------------------------------------------------------
    
    def is_compound_key(self, obj_type, field):
        """
        Return true if the obj_type/field is a compound key,
      
        The first character of the compound key's text '#', the
        second character identifies the separator characer for the
        fields value.  The fields are separated by '|' within the text.

        @param obj_type string, name of the object-type
        @param field string, field within the object-type
        """

        text = self.compound_key_text(obj_type, field)
        if text:
            return True
        return False

    #
    # --------------------------------------------------------------------------------
    # is_primitive_compound_key
    #
    def is_primitive_compound_key(self, obj_type, field):
        """
        Returns True for a primitive compound key.

        Primitive means compound-keys which don't use CassandraSetting's
        COMPOUND_KEY_FIELDS, rather the help text describes the fields which
        are cobbled together to create a primary key.

        For the cassandraSetting's COMPOUND_KEY_FIELDS, searches for 
        rows can be done by setting values of the fields of the
        COMPOUND_KEY_FIELDS, for primitive_compound_keys, 


        @param obj_type string, name of the object-type
        @param field string, field within the object-type
        """

        if self.is_compound_key(obj_type, field):
            field_info = self.obj_type_info_dict[obj_type]['fields'][field]
            if 'type' in field_info:
                if field_info['type'] != 'compound-key':
                    return True
        return False

    #
    # --------------------------------------------------------------------------------
    # compound_key_separator
    #
    def compound_key_separator(self, obj_type, field):
        """
        Return the single character which is used to separate the values
        of the different field parts for a field which is a compound key.

        Return None when the

        @param obj_type string, name of the object-type
        @param field string, field within the object-type
        """

        text = self.compound_key_text(obj_type, field)
        if text:
            return text[1]
        return None

    #
    # --------------------------------------------------------------------------------
    # compound_key_fields
    #
    def compound_key_fields(self, obj_type, field):
        """
        Return's a list of strings, where each is intended to be the
        name of a field within the obj_typ (this is not validated)

        @param obj_type string, name of the object-type
        @param field string, field within the object-type
        """

        text = self.compound_key_text(obj_type, field)
        if text:
            return text[2:].split('|')
        return None

    #
    # --------------------------------------------------------------------------------
    
    def deep_compound_key_fields(self, obj_type, field):
        """
        Similar to compound_key_fields(), but when any field is also a foreign
        key, the references obj_type's field is checked, and if that field is
        also a compound key, it is also expanded.

        @param obj_type string, name of the object-type
        @param field string, field within the object-type
        """

        def recurse_compound_key_fields(obj_type, field, parts):
            if self.is_foreign_key(obj_type, field):
                (fk_ot, fk_fn) = self.foreign_key_references(obj_type, field)
                if self.is_compound_key(fk_ot, fk_fn):
                    recurse_compound_key_fields(fk_ot, fk_fn, parts)
            elif self.is_compound_key(obj_type, field):
                for cf in self.compound_key_fields(obj_type, field):
                    if (self.obj_type_has_field(obj_type, cf) and
                        self.is_foreign_key(obj_type, cf)):

                        (fk_ot, fk_fn) = self.foreign_key_references(obj_type, cf)
                        if self.is_compound_key(fk_ot, fk_fn):
                            recurse_compound_key_fields(fk_ot, fk_fn, parts)
                        else:
                            parts.append(cf)
                    else:
                        parts.append(cf)
            return parts
        return recurse_compound_key_fields(obj_type, field, [])


    #
    # --------------------------------------------------------------------------------
    
    def split_compound_into_dict(self, obj_type, key, target_dict, is_prefix = False):
        """
        To be used to convert a compound key in a row intended for display,
        into separate component name:value pairs.

        The original dict is from a row of a table. the 'key' parameter
        identifies a compound key in the row, the procedure splits the
        value of that key/field, and uses the compound_key_fields() to
        determine what names ought to be associated with the field's
        values, and add's these fields into the original dict.
        """
        def add_to_dict(a, b):
            if a in target_dict and a != key:
                if str(target_dict[a]) != b:
                    if self.is_foreign_key(obj_type, a):
                        target_dict[a] = b
                    else:
                        print self.sdnsh.error_msg("compound split dict has different value: "
                                                   "%s found %s expected %s" %
                                                   (a, target_dict[a], b))
            else:
                target_dict[a] = b

        names = self.deep_compound_key_fields(obj_type, key)
        separator = self.compound_key_separator(obj_type, key)
        values = target_dict[key].split(separator)

        if len(names) != len(values):
            if not is_prefix:
                print self.sdnsh.error_msg("%s: %s: compound length mismatch: %s %s" %
                                           (obj_type, key, names, values))
            min_len = len(names)
            if len(values) < min_len:
                min_len = len(values)
            map(add_to_dict, names[:min_len], values[:min_len])
        else:
            map(add_to_dict, names, values)

    #
    # --------------------------------------------------------------------------------
    
    def foreign_key_references(self, obj_type, field):
        """
        For a field which is a foreign key, return the pair of
        [obj_type, field] describing where the foreign key references

        @param obj_type string, name of the object-type
        @param field string, field within the object-type
        """
        if not obj_type in self.obj_type_info_dict:
            return None
        if not 'fields' in self.obj_type_info_dict[obj_type]:
            return None
        if not field in self.obj_type_info_dict[obj_type]['fields']:
            return None

        field_info = self.obj_type_info_dict[obj_type]['fields'][field]
        if 'type' in field_info:
            if field_info['type'] == 'ForeignKey':
                return [field_info['rel_obj_type'],
                        field_info['rel_field_name']]
        return False

    #
    # --------------------------------------------------------------------------------
    
    def obj_type_foreign_keys(self, obj_type):
        """
        Return a list of foreign keys for this obj_type

        @param field string, field within the object-type
        """
        if not obj_type in self.obj_type_info_dict:
            return []
        return [x for x in self.obj_type_info_dict[obj_type]['fields']
                if self.is_foreign_key(obj_type, x)]

    #
    # --------------------------------------------------------------------------------
    
    def obj_type_show_title(self, obj_type):
        """
        Return a string, the display title for this table,
        never return None, only displayable values
        """
        if not obj_type in self.obj_type_info_dict:
            return ''
        if 'title' in self.obj_type_info_dict[obj_type]:
            return self.obj_type_info_dict[obj_type]['title']
        return obj_type

    #
    # --------------------------------------------------------------------------------

    def obj_type_set_title(self, obj_type, title):
        if not obj_type in self.obj_type_info_dict:
            self.obj_type_info_dict[obj_type] = {}
        self.obj_type_info_dict[obj_type]['title'] = title
        
    #
    # --------------------------------------------------------------------------------
    
    def obj_type_set_show_this(self, obj_type, this_list):
        if not obj_type in self.obj_type_info_dict:
            self.obj_type_info_dict[obj_type] = {}
        self.obj_type_info_dict[obj_type]['show-this'] = this_list
        
    #
    # --------------------------------------------------------------------------------
    
    def field_default_value(self, obj_type, field):
        """
        Return non null value of the default value of a field if its configured

        @param obj_type string, name of the object-type
        @param field string, field within the object-type
        """
        if not obj_type in self.obj_type_info_dict:
            return None
        if not field in self.obj_type_info_dict[obj_type]['fields']:
            return None

        field_info = self.obj_type_info_dict[obj_type]['fields'][field]
        if 'default' in field_info:
            return field_info['default']
        return None

    #
    # --------------------------------------------------------------------------------
    
    def field_current_obj_type_default_value(self, field):
        """
        Return non null value of the default value of a field if its configured

        @param field string, field within the object-type
        """
        current_obj_type = self.sdnsh.get_current_mode_obj_type()

        if current_obj_type:
            if self.obj_type_has_field(current_obj_type, field):
                default = self.field_default_value(current_obj_type, field)
                if default == '':
                    return None
                return default
        return None

    #
    # --------------------------------------------------------------------------------
    
    def field_validation(self, obj_type, field):
        """
        Return the field validation function

        @param obj_type string, name of the object-type
        @param field string, field within the object-type
        """
        if 'validate' in self.obj_type_info_dict[obj_type]['fields'][field]:
            validate = self.obj_type_info_dict[obj_type]['fields'][field]['validate']
            return getattr(self, validate, None)
        return None

    #
    # --------------------------------------------------------------------------------
    
    def obj_type_prepare_row_update(self, obj_type):
        """
        Return the row update function is one is described in climodelinfo
        These callouts are intended to do fixup for the primary key's
        in a table where the field members are used to build the primary key

        @param field string, field within the object-type
        """
        if not obj_type in self.obj_type_info_dict:
            return None

        if 'update' in self.obj_type_info_dict[obj_type]:
            update = self.obj_type_info_dict[obj_type]['update']
            return getattr(self, update, None)
        return None

    
    #
    # --------------------------------------------------------------------------------

    def obj_type_show_sort(self, obj_type):
        """
        Return a sort-type for the obj-type during show.  The value is extracted
        from the obj_type_info_dict, and configured for the primary key for an
        obj-type, for example vns-access-list-entry's 'id' field shows
        'sort' : 'integer' to describe that the table's items
        are sorted by integer.
     
        Currently the sort-by field is not described in the
        cassandra model description

        @param field string, field within the object-type
        """

        if not obj_type in self.obj_keys:
            return None

        key = self.obj_keys[obj_type]
        if 'sort' in self.obj_type_info_dict[obj_type].get('fields', [])[key]:
            return self.obj_type_info_dict[obj_type]['fields'][key]['sort']
        return None

    #
    # --------------------------------------------------------------------------------
    
    def alias_obj_type_field(self, alias_obj_type):
        """
        Return a single field name for an obj_type, usually a foreign key
        in the model, which is the field associated with an alias.
        """
        foreign_keys = self.obj_type_foreign_keys(alias_obj_type)
        if len(foreign_keys) == 1:
            return foreign_keys[0]
        return None


    #
    # --------------------------------------------------------------------------------

    def not_default_value(self, obj_type, field, value):
        """
        Return True when the value passed in is not the default value.
        """
        default_value = self.field_default_value(obj_type, field)
    
        if (self.is_null_allowed(obj_type, field) and value != '') or \
          (not self.is_null_allowed(obj_type, field) and default_value != None
          and (default_value != value)):
            return True
        return False

    #
    # --------------------------------------------------------------------------------

    def obj_type_related_config_obj_type(self, obj_type):
        """
        If an obj-type doesn't have a rest model, for example - host, the obj_type
        may have a related config-table in the database, where additional configured
        data for the discovered data can be found.  Return the name of that table,
        for host, its host-condig
        """
        if not obj_type in self.obj_type_info_dict:
            return None

        if self.obj_type_has_model(obj_type):
            return obj_type

        if 'config-obj-type' in self.obj_type_info_dict[obj_type]:
            return self.obj_type_info_dict[obj_type]['config-obj-type']
        return None

    #
    # --------------------------------------------------------------------------------
    
    def obj_type_in_use_as_related_config_type(self, config_obj_type):
        """
        Return the obj_type is in use by the pased in config_obj_type
        or None otherwise.    Inverse of obj_type_related_config_obj_type
        """
        if config_obj_type == None:
            return None

        if not config_obj_type in self.obj_type_info_dict:
            return None

        for (ot, ov) in self.obj_type_info_dict.items():
            if ov.get('config-obj-type') == config_obj_type:
                return ot
        return None
