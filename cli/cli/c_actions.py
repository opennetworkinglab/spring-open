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

import re
import numbers
import collections
import traceback
import types
import json
import time
import sys
import datetime
import os
import subprocess
import socket
import urllib2 # exception, dump_log()

import modi
import error
import command
import run_config
import rest_to_model
import url_cache

from midw import *
from vnsw import *
#from html5lib.constants import DataLossWarning

onos=1
#
# ACTION PROCS
#Format actions for stats per table
def remove_unicodes(actions):
   
    if actions:
        #TODO: Check:- Why I have to remove last two character from string
        #instead of 1 character to get rid of comma from last aciton
        a=''
        b=''
        newActions=''
        isRemoved_u = False
        for ch in actions:
            if ch =='u':
                a= 'u'
            if ch =='\'':
                b= '\''
                if isRemoved_u:
                    isRemoved_u=False
                    continue
                if (a+b) == 'u\'':
                    newActions = newActions[:-1]
                    a= ''
                    isRemoved_u = True
            else:
                newActions += ch
        return newActions
    else:
        ''
def renameActions(actions):
   
    actions = actions.replace('GOTO_TABLE','GOTO')
    actions = actions.replace('WRITE_ACTIONS','WRITE')
    actions = actions.replace('APPLY_ACTIONS','APPLY')
    actions = actions.replace('DEC_NW_TTL: True','DEC_NW_TTL')
    actions = actions.replace('POP_MPLS: True','POP_MPLS')
    actions = actions.replace('COPY_TTL_IN: True','COPY_TTL_IN')
    actions = actions.replace('COPY_TTL_OUT: True','COPY_TTL_OUT')
    actions = actions.replace('DEC_MPLS_TTL: True','DEC_MPLS_TTL')
    actions = actions.replace('SET_DL_SRC','SRC_MAC')
    actions = actions.replace('SET_DL_DST','DST_MAC')
    actions = actions.replace('SET_NW_SRC','SRC_IP')
    actions = actions.replace('SET_NW_DST','DST_IP')
    actions = actions.replace('CLEAR_ACTIONS: {CLEAR_ACTIONS: True}','CLEAR_ACTIONS')
    
    return actions

def check_rest_result(result, message=None):
    if isinstance(result, collections.Mapping):
        error_type = result.get('error_type')
        if error_type:
            raise error.CommandRestError(result, message)

tunnel_id=None
tunnel_dict={}
def tunnel_create(data=None):
    global tunnel_id,tunnel_dict
    if sdnsh.description:   # description debugging
        print "tunnel_create:" , data
    if data.has_key('tunnel-id'):
        if (tunnel_id != None):
            if sdnsh.description:   # description debugging
                print "tunnel_create: previous data is not cleaned up"
            tunnel_id=None
            tunnel_dict={}
        tunnel_id=data['tunnel-id']
        tunnel_dict[tunnel_id]=[]
    if data.has_key('node-label'):    
        tunnel_dict[tunnel_id].append(data['node-label'])
    if data.has_key('adjacency-label'):    
        tunnel_dict[tunnel_id].append(data['adjacency-label'])
    if sdnsh.description:   # description debugging
        print "tunnel_create:" , tunnel_id, tunnel_dict

def tunnel_config_exit():
    global tunnel_id,tunnel_dict
    if sdnsh.description:   # description debugging
        print "tunnel_config_exit entered", tunnel_dict
    if tunnel_dict:
        url_str = ""
        entries = tunnel_dict[tunnel_id]
        url_str = "http://%s/rest/v1/tunnel/" % (sdnsh.controller)
        obj_data = {}
        obj_data['tunnel_id']=tunnel_id
        obj_data['label_path']=entries
        result = "fail"
        try:
            result = sdnsh.store.rest_post_request(url_str,obj_data)
        except Exception, e:
            errors = sdnsh.rest_error_to_dict(e)
            print sdnsh.rest_error_dict_to_message(errors)
        # LOOK! successful stuff should be returned in json too.
        tunnel_dict = {}
        tunnel_id = None
        if result != "success":
            print "command failed"
    else:
        print "empty command"
    #Clear the transit information    
            
def tunnel_remove(data=None):
    if sdnsh.description:   # description debugging
        print "tunnel_remove:" , data
    tunnel_id=data['tunnel-id']
    url_str = "http://%s/rest/v1/tunnel/" % (sdnsh.controller)
    obj_data = {}
    obj_data['tunnel_id']=data['tunnel-id']
    result = "fail"
    try:
        result = sdnsh.store.rest_post_request(url_str,obj_data,'DELETE')
    except Exception, e:
        errors = sdnsh.rest_error_to_dict(e)
        print sdnsh.rest_error_dict_to_message(errors)
    if not result.startswith("SUCCESS"):
        print result

policy_obj_data = {}
def policy_create(data=None):
    global policy_obj_data
    if sdnsh.description:   # description debugging
        print "policy_create:" , data
    if data.has_key('policy-id'):
        if policy_obj_data:
            if sdnsh.description:   # description debugging
                print "policy_create: previous data is not cleaned up"
            policy_obj_data = {}
        policy_obj_data['policy_id'] = data['policy-id']
        policy_obj_data['policy_type'] = data['policy-type']
    if data.has_key('src_ip'):
        for key in data:
            policy_obj_data[key] = data[key]
    if data.has_key('priority'):
        policy_obj_data['priority'] = data['priority']
    if data.has_key('tunnel-id'):
        policy_obj_data['tunnel_id'] = data['tunnel-id']
    
    if sdnsh.description:   # description debugging
        print policy_obj_data
      
def policy_config_exit():
    global policy_obj_data
    if sdnsh.description:   # description debugging
        print "policy_config_exit entered", policy_obj_data
    if policy_obj_data:
        url_str = "http://%s/rest/v1/policy/" % (sdnsh.controller)
        result = "fail"
        try:
            result = sdnsh.store.rest_post_request(url_str,policy_obj_data)
        except Exception, e:
            errors = sdnsh.rest_error_to_dict(e)
            print sdnsh.rest_error_dict_to_message(errors)
        if result != "success":
            print "command failed"
        policy_obj_data = {}
    else:
        print "empty command"
    #Clear the transit information    
            
def policy_remove(data=None):
    if sdnsh.description:   # description debugging
        print "policy_remove:" , data
    policy_id=data['policy-id']
    url_str = "http://%s/rest/v1/policy/" % (sdnsh.controller)
    obj_data = {}
    obj_data['policy_id']=data['policy-id']
    result = "fail"
    try:
        result = sdnsh.store.rest_post_request(url_str,obj_data,'DELETE')
    except Exception, e:
        errors = sdnsh.rest_error_to_dict(e)
        print sdnsh.rest_error_dict_to_message(errors)
    if result != "deleted":
        print "command failed"
  
    

def write_fields(obj_type, obj_id, data):
    """
    Typical action to update fields of a row in the model

    @param obj_type a string, the name of the db table to update
    @param obj_id a string, the value of the primary key in for the table
    @param data a dict, the name:value pairs of data to update in the table
    """
    if sdnsh.description:   # description debugging
        print "write_fields:", obj_type, obj_id, data

    pk_name = mi.pk(obj_type)
    if not pk_name:
        raise error.CommandDescriptionError("Can't find primary key name for type: %s" % obj_type)
    if sdnsh.description:   # description debugging
        print "write_fields:", obj_type, pk_name, obj_id, data
    for fk in mi.obj_type_foreign_keys(obj_type):
        if fk in data and mi.is_null_allowed(obj_type, fk):
            if data[fk] == 'default': # XXX much too magic, seems an option here would be good
                data[fk] = None
            
    result = sdnsh.rest_update_object(obj_type, pk_name, obj_id, data)
    check_rest_result(result)


def verify_row_includes(obj_type, pk_value, data, verify):
    """
    Intended to raise an exception when a user enters 'no field value',
    and the field isn't currently set to value,  for example:
    'address-space as1 ; no address-space as2', should complain
    that the 'address-space' field isn't currently set to 'as2'.

    @param obj_type a string, identifies the db table
    @param pk_value a string, identifies the value for the primary key
    @param data is a dict, collecting the name:value pairs from the description
    @verify the string or list of field names to be verified
    """
    if sdnsh.description:   # description debugging
        print "validate_row_includes:", obj_type, pk_value, data, verify

    if type(verify) == str:
        verify = [verify] # if not a list, make it a list

    try:
        row = sdnsh.get_object_from_store(obj_type, pk_value)
    except Exception, e:
        if sdnsh.debug or sdnsh.debug_backtrace:
            print 'Failed lookup of %s:%s:%s', (obj_type, pk_value, e)
            traceback.print_exc()
        raise error.ArgumentValidationError("%s: '%s' doesn't exist" %
                                            (obj_type, pk_value))
        return
    
    if sdnsh.description:   # description debugging
        print "validate_includes: ", row
    for field in [x for x in verify if x in data and x in row]:
        if row[field] != data[field]:
            raise error.ArgumentValidationError("%s: %s found '%s' current value '%s'" %
                                                (obj_type, field, data[field], row[field]))


def reset_fields(obj_type, arg_data,
                 obj_id = None, fields = None, match_for_no = None):
    """
    For an obj_type, revert fields back to their default value.
    This is the typical action for 'no' commands.

    When verify is set, this is a string or list of fields who's values
    must match in the table for the primary key associated with the reset.
    This allows command descriptions to identify any fields which need to
    be checked against, when they are explicidly named in the 'no' command,
    so that 'no XXX value' will verify that 'value' matches the current 
    row's value before allowing the reset to continue

    @param obj_type a string, identifies the db table
    @param obj_id a string, identifies the value for the primary key of the row in the table,
            possibly unset, the key is looked for in the arg_data in that case.
    @param arg_data a dict, collection of name:value pairs from the description
    @param fields a list, collection of fields to update in the table
    @param match_for_no a string or list, list of fields to check for matched values in arg_data
    """

    if obj_type == None:
        raise error.CommandDescriptionError("No object to reset (missing obj-type)")
        
    pk_name = mi.pk(obj_type)
    # If the fields aren't specified explicitly, then derive from the arg_data
    if fields is None:
        fields = []
        for field in arg_data.keys():
            # Only add arguments that correspond to valid fields in the object
            if mi.obj_type_has_field(obj_type, field):
                if field != pk_name: # don't reset primary keys
                    fields.append(field)

    if len(fields) == 0:
        raise error.CommandDescriptionError("No fields to reset: type: %s" % obj_type)
 
    # Get the primary key name
    if not pk_name:
        raise error.CommandDescriptionError("Can't find primary key name for type: %s" % obj_type)
    if obj_id == None:
        if pk_name in arg_data:
            obj_id = arg_data[pk_name]
        elif mi.field_default_value(obj_type, pk_name):
            # unusual, but not impossible for singletons
            obj_id = mi.field_default_value(obj_type, pk_name)
        else:
            raise error.CommandDescriptionError("Can't find id value name for type: %s"
                                                " field %s" % (obj_type, pk_name))
    
    if match_for_no:
        verify_row_includes(obj_type, obj_id, arg_data, match_for_no)

    # Get the default values of the specified field from CLI model info
    data = {}
    for field in fields:
        if field == pk_name:
            continue
        type_info = mi.cli_model_info.get_field_info(obj_type, field)
        if type_info == None:
            raise error.CommandDescriptionError("Can't find field details for "
                                                "field %s in type %s" % (field, obj_type))
        data[field] = type_info.get('default')
        if data[field] == None and type_info.get('type') == 'BooleanField':
            data[field] = False
        # why does boolean not respect the default in the model?!?
        # data[field] = type_info.get('default') if type_info.get('type') != 'BooleanField' else False
    
    if sdnsh.description:   # description debugging
        print "reset_fields:", obj_type, pk_name, obj_id, data, match_for_no

    # Invoke the REST API to set the default values
    try:
        result = sdnsh.rest_update_object(obj_type, pk_name, obj_id, data)
    except Exception, e:
        errors = sdnsh.rest_error_to_dict(e, obj_type)
        raise error.CommandError('REST', sdnsh.rest_error_dict_to_message(errors))


def obj_type_fields_have_default_value(obj_type, row, data):
    """
    Return True when all the fields have a default value,
    row is the queried data from the store, 
    data is the data to be updated.

    The goal is to determine whether to delete or update
    the row in the store.

    """

    ckf = []
    if mi.is_compound_key(obj_type, mi.pk(obj_type)):
        # XXX primitive compound keys' too?
        ckf = mi.compound_key_fields(obj_type, mi.pk(obj_type))

    for field in mi.obj_type_fields(obj_type):
        if mi.is_primary_key(obj_type, field):
            continue
        if mi.is_foreign_key(obj_type, field):
            # perhaps only allow a single foreign key?
            continue
        # also any fields which are used to compound the ident.
        if field in ckf:
            continue
        # Needs a better way to identify non-model-fields
        if field == 'Idx':
            continue
        if mi.is_null_allowed(obj_type, field):
            # does this need to be more complex?
            if field in data and data[field] != None:
                return False
            continue # next field
        default_value = mi.field_default_value(obj_type, field)
        if default_value == None:
            if sdnsh.description:   # description debugging
                print 'default_value: no default: %s %s' % (obj_type, field)
            return False
        # check to see if the updated value would be the default
        if field in data and data[field] != default_value:
            if sdnsh.description:   # description debugging
                print 'default_value: not default %s %s %s' % \
                        (field, data[field], default_value)
            return False
        elif row.get(field, default_value) != default_value:
            if field in data and data[field] == default_value:
                if sdnsh.description:   # description debugging
                    print 'default_value: db not default %s %s %s' \
                          ' new value in data %s is default' % \
                            (field, row[field], default_value, data[field])
                continue
            if sdnsh.description:   # description debugging
                print 'default_value: db not default %s %s %s' % \
                        (field, row[field], default_value)
            return False
    return True


def update_config(obj_type, obj_id, data, no_command):
    """
    update_config is intended to write a row when the described data
    is different from the default values of the fields of  the row.

    When the data described in the call updates the field's values
    to all default values, the row associated with the obj_id is
    deleted. 

    This is intended to be used for models which contain configuration
    row data, and that every field has a default value,
    so that when the config data is transitioned to the default
    state, the row is intended to be removed.  For these sorts of
    command descriptions, updating a field to some default value
    may result in the row getting deleted.
    """

    c_data = dict(data)  # make a local copy
    if sdnsh.description:   # description debugging
        print "update_config: ", obj_type, obj_id, c_data, no_command
    
    if not mi.obj_type_exists(obj_type):
        raise error.CommandDescriptionError("Unknown obj-type: %s" % obj_type)
        
    # collect any dict.key names which aren't fields in the object
    for unknown_field in [x for x in c_data.keys() if not mi.obj_type_has_field(obj_type, x)]:
        del c_data[unknown_field]

    # if its a no command, set the value to 'None' if it's allowed,
    # of to its default value otherwise
    if no_command:
        for field in c_data.keys():
            if mi.is_null_allowed(obj_type, field):
                c_data[field] = None
            else:
                # required to have a default value
                c_data[field] = mi.field_default_value(obj_type, field)
            
    # Get the primary key name
    pk_name = mi.pk(obj_type)
    if not pk_name:
        raise error.CommandDescriptionError("Can't find primary key name for type: %s" % obj_type)
    pk_value = obj_id
    if pk_name in data:
        pk_value = data[pk_name]
    if pk_name in c_data:
        del c_data[pk_name]
    
    # Query for the row, if it doesn't exist, create the item if any item isn't default
    if sdnsh.description:   # description debugging
        print "update_config: query:", obj_type, pk_value

    result = sdnsh.rest_query_objects(obj_type, { pk_name : pk_value })
    check_rest_result(result)
    if len(result) == 0:
        # result[0] -> dictionary of field:value pairs
        # check to ensure c_data isn't just default row values
        if not obj_type_fields_have_default_value(obj_type, {}, c_data):
            if sdnsh.description:   # description debugging
                print "update_config: create:", obj_type, c_data
            # populate the create dictionary
            create_dict = dict(c_data)
            create_dict[pk_name] = pk_value
            result = sdnsh.rest_create_object(obj_type, create_dict)
            check_rest_result(result)
        else:
            if sdnsh.description:   # description debugging
                print "update_config: no current row"
        return
    else:
        if sdnsh.description:   # description debugging
            print "update_config: found row", result[0]

    if len(result) > 1:
        raise error.CommandInternalError("Multiple rows for obj-type: %s: pk %s" % 
                                           (obj_type, pk_value))

    # See if the complete row needs to be deleted.
    # For each of the current fields, if a field's default doesn't exist,
    # skip the row delete, or if any field has a non-default value, update
    # the requested fields instead of deleting the row.
    if obj_type_fields_have_default_value(obj_type, result[0], c_data):
        # if the table has foreign keys, check no children refer to this table.
        no_foreign_keys_active = True
        if obj_type in mi.foreign_key_xref:
            for (fk_obj_type, fk_fn) in mi.foreign_key_xref[obj_type][mi.pk(obj_type)]:
                try:
                    rows = sdnsh.get_table_from_store(fk_obj_type, fk_fn,
                                                      pk_value, "exact")
                except Exception, e:
                    rows = []
                if len(rows):
                    if sdnsh.description:   # description debugging
                        print "update_config: foreign key active:", \
                               fk_obj_type, fk_fn, pk_value
                    no_foreign_keys_active = False
                    break

        if no_foreign_keys_active:
            if sdnsh.description:   # description debugging
                print "update_config: delete:", obj_type, pk_value
            try:
                delete_result = sdnsh.rest_delete_objects(obj_type, { pk_name : pk_value })
                check_rest_result(delete_result)
            except Exception, e:
                errors = sdnsh.rest_error_to_dict(e)
                raise error.CommandInvocationError(sdnsh.rest_error_dict_to_message(errors))
            return
        # XXX if a row from some table is removed, and that table is using
        # foreign keys, then the table which is refered to ought to be
        # reviewed, to see if all the entries of the row which this table
        # refer's to are default, and if that parent table is a config-style
        # table, with all default values for every field, there's a good
        # argument that the row ought to be removed.
        
    # See if any of the c_data items in the matching row are different
    # (ie: is this update really necessary?)
    update_necessary = False
    for (name, value) in c_data.items():
        if name in result[0]:
            if value != result[0][name]:
                update_necessary = True
                if sdnsh.description:   # description debugging
                    print "update_config: update necessary:", name, result[0][name], value
        else:
            update_necessary = True

    if not update_necessary:
        if sdnsh.description:   # description debugging
            print "update_config: no update needed", obj_type, pk_name, pk_value
        return

    if sdnsh.description:   # description debugging
        print "update_config: update:", obj_type, pk_name, pk_value, c_data
    # Invoke the REST API to set the default values
    result = sdnsh.rest_update_object(obj_type, pk_name, pk_value, c_data)
    check_rest_result(result)


def delete_objects(obj_type, data, parent_field=None, parent_id=None):
    """
    Delete a row in the table.

    @param obj_type a string, the name of the table to update
    @param data a dictionary, name:value pairs to describe the delete
    @param parent_field a string, the name of a field in the obj_type,
            identifying a relationship between this table, and another table
    @param parent_id a string, the value of the parent_field, to identify
            another row in the other table identified by a field in this table
    """

    pk_name = mi.pk(obj_type)
    if not pk_name:
        raise error.CommandDescriptionError("Can't find primary key name for type: %s" % obj_type)

    query_data = dict(data)
    if parent_field:
        query_data[parent_field] = parent_id

    # case conversion
    for field in data:
        if mi.obj_type_has_field(obj_type, field):
            case = mi.get_obj_type_field_case_sensitive(obj_type, field)
            if case:
                if sdnsh.description:   # description debugging
                    print 'delete_objects: case convert %s:%s to %s' % \
                          (obj_type, field, case)
                data[field] = utif.convert_case(case, data[field])

    query_result = sdnsh.rest_query_objects(obj_type, query_data)
    check_rest_result(query_result)
    #
    # if there were no results, try to delete by removing any
    # items which have "None" values
    if len(query_result) == 0:
        for key in query_data.keys():
            if query_data[key] == None:
                del query_data[key]
        query_result = sdnsh.rest_query_objects(obj_type, query_data)
        check_rest_result(query_result)

    if sdnsh.description:   # description debugging
        print "delete_objects:", obj_type, query_data
    delete_result = sdnsh.rest_delete_objects(obj_type, query_data)
    check_rest_result(delete_result)

    for item in query_result:
        key = item[pk_name]
        sdnsh.cascade_delete(obj_type, key)


def set_data(data, key, value):
    """
    Action to associate a new name:value pair with 'data', the dictionary used 
    to pass to REST API's.   Allows the action to describe a value for a field
    which wasn't directly named in the description.

    """
    if sdnsh.description:   # description debugging
        print "set_data:", data, key, value
    data[key] = value


def write_object(obj_type, data, parent_field=None, parent_id=None):
    """
    Write a new row into a specific table.

    """
    # If we're pushing a config submode with an object, then we need to extend the
    # argument data that was entered explicitly in the command with the information
    # about the parent object (by default obtained by looking at the obj info on
    # the mode stack -- see default arguments for this action when it is added).

    if sdnsh.description:   # description debugging
        print 'write_object: params ', obj_type, data, parent_field, parent_id
    data = dict(data) # data is overwriten in various situations below
    if parent_field:
        if not parent_id:
            raise error.CommandDescriptionError('Invalid command description;'
                                              'improperly configured parent info for create-object')
        data[parent_field] = parent_id

    pk_name = mi.pk(obj_type)
    if not pk_name:
        raise error.CommandDescriptionError("Can't find primary key name for type: %s" % obj_type)

    # look for unpopulated foreign keys.
    fks = mi.obj_type_foreign_keys(obj_type)
    if fks:
        for fk in fks:
            (fk_obj, fk_nm) = mi.foreign_key_references(obj_type, fk)
                
            if not fk in data or \
               (mi.is_compound_key(fk_obj, fk_nm) and data[fk].find('|') == -1):
                # use various techniques to populate the foreign key
                # - if the foreign key is for class which has a compound key, see if all the
                #   parts of the compound key are present

                if mi.is_compound_key(fk_obj, fk_nm):
                    kfs = mi.deep_compound_key_fields(fk_obj, fk_nm)
                    missing = [x for x in kfs if not x in data]
                    if len(missing) == 0:
                        # remove the entries, build the compound key for the foreign key reference
                        new_value = mi.compound_key_separator(fk_obj, fk_nm).\
                                          join([data[x] for x in kfs])
                        # verify the foreign key exists, if not complain and return,
                        # preventing a error during the create request
                        query_result = sdnsh.rest_query_objects( fk_obj, { fk_nm : new_value })
                        check_rest_result(query_result)
                        if len(query_result) == 0:
                            joinable_name = ["%s: %s" % (x, data[x]) for x in kfs]
                            raise error.CommandSemanticError("Reference to non-existant object: %s " %
                                                               ', '.join(joinable_name))
                        for rfn in kfs: # remove field name
                            del data[rfn]
                        data[fk] = new_value
                else:
                    qr = sdnsh.rest_query_objects(fk_obj, data)
                    if len(qr) == 1:
                        data[fk] = qr[0][mi.pk(fk_obj)]

    if pk_name in data:
        if sdnsh.description:   # description debugging
            print command._line(), 'write_object: query pk_name ', obj_type, pk_name, data
        case =  mi.get_obj_type_field_case_sensitive(obj_type, pk_name)
        if case:
            data[pk_name] = utif.convert_case(case, data[pk_name])
        query_result = sdnsh.rest_query_objects(obj_type, { pk_name : data[pk_name]})
    else:
        query_data = dict([[n,v] for (n,v) in data.items() if v != None])
        if sdnsh.description:   # description debugging
            print command._line(), 'write_object: query ', obj_type, query_data
        query_result = sdnsh.rest_query_objects(obj_type, query_data)
    check_rest_result(query_result)
 
    # Consider checking to see if all the fields listed here
    # already match a queried result, if so, no write is needed

    if (len(query_result) > 0) and (pk_name in data):
        if sdnsh.description:   # description debugging
            print "write_object: update object", obj_type, pk_name, data
        result = sdnsh.rest_update_object(obj_type, pk_name, data[pk_name], data)
    else:
        if sdnsh.description:   # description debugging
            print "write_object: create_object", obj_type, data
        result = sdnsh.rest_create_object(obj_type, data)
        
    check_rest_result(result)

    for item in query_result:
        key = item[pk_name]
        sdnsh.cascade_delete(obj_type, key)


def delete_object(obj_type, data, parent_field=None, parent_id=None):
    global sdnsh

    data = dict(data)
    if parent_field:
        if not parent_id:
            raise error.CommandDescriptionError('Invalid command description;'
                            'improperly configured parent info for delete-object')
        data[parent_field] = parent_id

    # case conversion
    for field in data:
        if mi.obj_type_has_field(obj_type, field):
            case = mi.get_obj_type_field_case_sensitive(obj_type, field)
            if case:
                if sdnsh.description:   # description debugging
                    print 'delete_object: case convert %s:%s to %s' % \
                          (obj_type, field, case)
                data[field] = utif.convert_case(case, data[field])

    if sdnsh.description:   # description debugging
        print "delete_object: ", obj_type, data
    result = sdnsh.rest_delete_objects(obj_type, data)
    check_rest_result(result)


def push_mode_stack(mode_name, obj_type, data, parent_field = None, parent_id = None, create=True):
    """
    Push a submode on the config stack.
    """
    global sdnsh, modi

    # Some few minor validations: enable only in login, config only in enable,
    # and additional config modes must also have the same prefix as the
    # current mode.
    current_mode = sdnsh.current_mode()

    if sdnsh.description:   # description debugging
        print "push_mode: ", mode_name, obj_type, data, parent_field, parent_id

    # See if this is a nested submode, or whether some current modes
    # need to be popped.
    if (mode_name.startswith('config-') and 
      (not mode_name.startswith(current_mode) or (mode_name == current_mode))):

        sdnsh.pop_mode()
        current_mode = sdnsh.current_mode()
        # pop until it it matches
        while not mode_name.startswith(current_mode):
            if len(sdnsh.mode_stack) == 0:
                raise error.CommandSemanticError('%s not valid within %s mode' %
                                           (mode_name, current_mode))
            sdnsh.pop_mode()
            current_mode = sdnsh.current_mode()

        # if there's a parent id, it is typically the parent, and audit
        # ought to be done to verify this
        if parent_field:
            data = dict(data)
            data[parent_field] = sdnsh.get_current_mode_obj()

    elif mode_name in ['config', 'enable', 'login']:
        # see if the mode is in the stack
        if mode_name in [x['mode_name'] for x in sdnsh.mode_stack]:
            if sdnsh.description:   # description debugging
                print 'push_mode: popping stack for', mode_name
            current_mode = sdnsh.current_mode()
            while current_mode != mode_name:
                sdnsh.pop_mode()
                current_mode = sdnsh.current_mode()
            return


    # If we're pushing a config submode with an object, then we need to extend the
    # argument data that was entered explicitly in the command with the information
    # about the parent object (by default obtained by looking at the obj info on
    # the mode stack -- see default arguments for this action when it is added).
    elif parent_field:
        if not parent_id:
            raise error.CommandDescriptionError('Invalid command description; '
                                          'improperly configured parent info for push-mode-stack')
        data = dict(data)
        data[parent_field] = parent_id
        
    key = None
    if obj_type:
        for field in data:
            if mi.obj_type_has_field(obj_type, field):
                case = mi.get_obj_type_field_case_sensitive(obj_type, field)
                if case:
                    if sdnsh.description:   # description debugging
                        print 'push_mode: case convert %s:%s to %s' % \
                              (obj_type, field, case)
                    data[field] = utif.convert_case(case, data[field])


        # Query for the object both to see if it exists and also to determine
        # the pk value we're going to push on the stack. We need to do
        # the query in the case where the model uses compound keys and we're
        # specifying the individual fields that compose the compound key.
        result = sdnsh.rest_query_objects(obj_type, data)
        check_rest_result(result)
        if len(result) == 0 and create:
            #
            # For vns-interface, the association of 'rule' with the data dict
            # is difficult to explain via the command description.  This is
            # obviously a poor method of dealing with the issue, but until
            # a better one arises (possibly REST api create?  possibly
            # model validation code?), this solution works.
            if obj_type == 'vns-interface':
                data = associate_foreign_key_for_vns_interface(data)

            # Create the object and re-query to get the id/pk value
            # FIXME: Could probably optimize here if the data already
            # contains the pk value.
            if sdnsh.description:   # description debugging
                print "push_mode: create ", obj_type, data
            result = sdnsh.rest_create_object(obj_type, data)
            check_rest_result(result)
            result = sdnsh.rest_query_objects(obj_type, data)
            check_rest_result(result)
        else:
            if sdnsh.description:   # description debugging
                print "push_mode: object found", obj_type, result
        
        # Check (again) to make sure that we have an object
        if len(result) == 0:
            raise error.CommandSemanticError('Object not found; type = %s' % obj_type)
        
        # Check to make sure there aren't multiple matching objects. If there
        # are that would indicate a problem in the command description.
        if len(result) > 1:
            raise error.CommandDescriptionError('Push mode info must identify a single object;'
                                          'type = %s; data = %s'  %
                                          (obj_type, str(data)))
        
        # Get the id/pk value from the object info
        pk_name = mi.pk(obj_type)
        if not pk_name:
            raise error.CommandDescriptionError("Can't find primary key name for type: %s" % obj_type)
        key = result[0][pk_name]
    else:
        pk_name = '<none>'
    
    if sdnsh.description:   # description debugging
        print "push_mode: ", mode_name, obj_type, pk_name, key
    exitCallback = None
    if (mode_name == 'config-tunnel'):
        exitCallback = tunnel_config_exit
    if (mode_name == 'config-policy'):
        exitCallback = policy_config_exit
    sdnsh.push_mode(mode_name, obj_type, key, exitCallback)

    
def pop_mode_stack():
    global sdnsh

    if sdnsh.description:   # description debugging
        print "pop_mode: "
    sdnsh.pop_mode()

def confirm_request(prompt):
    global sdnsh

    if sdnsh.batch:
        return
    result = raw_input(prompt)
    if result.lower() == 'y' or result.lower() == 'yes':
        return
    raise error.ArgumentValidationError("Expected y or yes, command: ")

import c_data_handlers

def convert_vns_access_list(obj_type, key, data):
    """
    For vns-access-group's, the access list which is the first parameter
    needs to be converted into a vns-access-list foreign key.  This is
    possible since the vns name is part of the current object id.
    """
    global sdnsh, modi

    key_parts = key.split('|')
    if len(key_parts) != 3:
        raise error.ArgumentValidationError("invalid id")
    if not 'vns-access-list' in data:
        raise error.ArgumentValidationError("missing vns-access-list")
    try:
        key_parts.pop()
        vnskey='|'.join(key_parts)
        entry = sdnsh.rest_query_objects('vns-access-list',
                                        { 'vns' : vnskey,
                                          'name' : data['vns-access-list']
                                        })
    except Exception, _e:
        entry = []

    if len(entry) != 1:
        raise error.ArgumentValidationError("unknown acl %s" % data['vns-access-list'])
    data['vns-access-list'] = entry[0]['id']
          
def command_query_object(obj_type, data, scoped, sort):
    """
    Return model entries (db rows) via the REST API.  Try to be
    very smart about using parameters and the model definition to
    figure out how to query for the entries.
    """

    if sdnsh.description:
        print 'command_query_object: ', obj_type, data, scoped, sort
        
    skipforeignsearch=False
    if (obj_type=='virtualrouter-routingrule' or obj_type=='virtualrouter-interface'):
        skipforeignsearch=True
    # big_search describes a related search which must be done to
    # satisfy this request, see the relationship of tag-mapping to tag
    # as an example.
    big_search = []

    key = mi.pk(obj_type)
    #
    if mi.is_compound_key(obj_type, key):
        if sdnsh.description:   # description debugging
            print "command_query_object: %s compound %s" % (obj_type, key)
        #
        # collect compound key names, look for these in the data,
        # if any of the values are 'all', remove the item from
        # the group of data.
        #
        # XXX needs work: we ought to check to see if the
        # compound key is part of some other key.
        #
        if scoped:
            obj_d = { key : sdnsh.get_current_mode_obj() }
            mi.split_compound_into_dict(obj_type, key, obj_d, is_prefix = True)
            for (k,v) in obj_d.items():
                if k != key and not k in data:
                    data[k] = v

        new_data = {}
        dckfs = mi.deep_compound_key_fields(obj_type, key)
        if key in data:
            mi.split_compound_into_dict(obj_type, key, data, is_prefix = True)
        foreign_obj_type_search = {}
        
        for kf in dckfs:
            if mi.obj_type_has_field(obj_type, kf) and kf in data and data[kf] != 'all':
                new_data[kf] = data[kf]
            elif not mi.obj_type_has_field(obj_type, kf):
                # deep_compound_keys returns references via foreign keys.
                # if the field is missing in obj_type, its likely from
                # some related fk.
                for fk in mi.obj_type_foreign_keys(obj_type):
                    (_fk_obj_type, fk_name) = mi.foreign_key_references(obj_type,
                                                                        fk)
                    if kf == fk_name:
                        # print "FOUND MATCH ", kf, _fk_obj_type, fk_name
                        continue
                    elif not mi.is_compound_key( _fk_obj_type, fk_name):
                        continue
                    for fkcf in mi.compound_key_fields(_fk_obj_type, fk_name):
                        if fkcf in data and data[fkcf] != 'all':
                            # assume all models use COMPOUND_KEY_FIELDS
                            if _fk_obj_type not in foreign_obj_type_search:
                                foreign_obj_type_search[_fk_obj_type] = {}
                            foreign_obj_type_search[_fk_obj_type][fkcf] = data[fkcf]
                pass
            # see if foreign key fields are indirectly named
            elif mi.is_foreign_key(obj_type, kf):
                (_fk_obj_type, fk_name) = mi.foreign_key_references(obj_type,
                                                                    kf)
                if fk_name in data and data[fk_name] != 'all':
                    new_data[kf] = data[fk_name]
        if (not skipforeignsearch): #skip foreign key search for routingrule type
            if len(foreign_obj_type_search):
                # This means to collect the entries, a search though a
                # related obj_type (through foreign key) will need to be done
                # a single query isn't enough, unless all entries are collected
                # consider the relationship between tag-mapping and tags
                # 
                # This code seems to handle single indirected foreign key
                # lookup, but if deep_compound_key_fields() found more than
                # three layers deep (the obj-type has a fk reference to a
                # table, which had a fk reference to another table, which
                # had a value to search with), this won't do the trick.
                # at that point some sort of recursive building of the
                # foreign keys would be needed to collect up the required
                # final seraches
                for (_fk_obj_type, search) in foreign_obj_type_search.items():
                    fk_entries = sdnsh.rest_query_objects(_fk_obj_type, search)
                    # need to identify the name associated foreign key in this model
                    for fk in mi.obj_type_foreign_keys(obj_type):
                        (fk_obj, fk_name) = mi.foreign_key_references(obj_type, fk)
                        if fk_obj == _fk_obj_type:
                            obj_type_field = fk
                            break
                    else:
                        raise error.CommandSemanticError("bigsearch: can't find fk reference"
                                                         "for %s for obj-type %s" %
                                                         (fk, obj_type))
                    big_search += [{obj_type_field:
                                    x[mi.pk(_fk_obj_type)]} for x in fk_entries]
                # big_search would return id's for the _fk_obj_type,
                # which can be used to search this obj_type
        # look for fields which are set in new_data, which aren't in data.
        for (field, value) in data.items():
            if field not in new_data:
                if mi.is_marked_searchable(obj_type, field) and value!='all':
                    new_data[field] = value

        data = new_data
    else:
        # Only allow fields which are searchable (XXX need a prediate)
        # only save primary key's and foreigh keys.
        new_data = {}
        if key in data and mi.is_primary_key(obj_type, key):
            new_data[key] = data[key]
        for fk in mi.obj_type_foreign_keys(obj_type):
            if fk in data:
                new_data[fk] = data[fk]
            (_fk_obj, fk_fn) = mi.foreign_key_references(obj_type, fk)
            if fk_fn in data:
                new_data[fk_fn] = data[fk_fn]
        for f in mi.obj_type_fields(obj_type):
            if f in data and f not in new_data:
                new_data[f] = data[f]
            
        data = new_data

        if scoped:
            data[key] = sdnsh.get_current_mode_obj()
        
    if key in data and (data[key]=='all' or data[key]==None):
        del data[key]
    #
    # Now that the fields have been disassembled as much as possible, see
    # if some of the entries need to be cobbled back together.
    fks = mi.obj_type_foreign_keys(obj_type)
    if sdnsh.description:   # description debugging
        print "command_query_object: %s foreign-key %s" % (obj_type, fks)
    if fks:
        for fk in fks:
            (fk_obj, fk_nm) = mi.foreign_key_references(obj_type, fk)
                
            if not fk in data or \
               (mi.is_compound_key(fk_obj, fk_nm) and data[fk].find('|') == -1):

                # use various techniques to populate the foreign key
                # - if the foreign key is for class which has a compound key, see if all the
                #   parts of the compound key are present
                if mi.is_compound_key(fk_obj, fk_nm):
                    kfs = mi.deep_compound_key_fields(fk_obj, fk_nm)
                    missing = [x for x in kfs if not x in data]
                    if len(missing) == 0:
                        # remove the entries, build the compound key for the foreign key reference
                        new_value = mi.compound_key_separator(fk_obj, fk_nm).\
                                          join([data[x] for x in kfs])
                        # verify the foreign key exists, if not complain and return,
                        # preventing a error during the create request
                        query_result = sdnsh.rest_query_objects( fk_obj, { fk_nm : new_value })
                        check_rest_result(query_result)
                        if len(query_result) == 0:
                            joinable_name = ["%s: %s" % (x, data[x]) for x in kfs]
                            raise error.CommandSemanticError("Reference to non-existant object: %s " %
                                                               ', '.join(joinable_name))
                        for rfn in kfs: # remove field name
                            del data[rfn]
                        data[fk] = new_value
        if sdnsh.description:   # description debugging
            print "command_query_object: %s foreign key construction " % obj_type, data
    #
    # Do something for alias displays, for obj_types which sdnsh says
    # are aliases, find the foreign reference in the alias obj_type,
    # and use that to determine the field name (fk_fn) in the parent.
    # Do lookups based on either the alias field name, or the parent's
    # fk_fn when set in data{}
    if obj_type in mi.alias_obj_types:
        field = mi.alias_obj_type_field(obj_type)
        (_fk_obj, fk_fn) = mi.foreign_key_references(obj_type, field)
        new_data = {}
        if fk_fn in data and data[fk_fn] != 'all':
            new_data[field] = data[fk_fn]
        elif field in data and data[field] != 'all':
            new_data[field] = data[field]
        data = new_data

    #
    # The sort value ought to be a command separated list of fields within the model
    #
    if sort:
        data['orderby'] = sort

    if not mi.obj_type_has_model(obj_type):
        return rest_to_model.get_model_from_url(obj_type, data)

    if sdnsh.description:   # description debugging
        print "command_query_object: ", obj_type, data

    if len(big_search):
        entries = []
        if sdnsh.description:   # description debugging
            print "command_query_object: big search", big_search
        for bs in big_search:
            search = dict(list(bs.items()) + list(data.items()))
            entries += sdnsh.rest_query_objects(obj_type, search)
        # XXX needs to be re-sorted
        return entries
        
    return sdnsh.rest_query_objects(obj_type, data)


def command_display_table_join_entries(obj_type, data, entries, detail):
    """
    """
    if obj_type == 'tag-mapping':
        # lift persist from the parent tag
        if len(entries) == 1:
            entry = entries[0]
            tag = sdnsh.rest_query_objects('tag', { mi.pk('tag') : entry['tag']})
            entry['persist'] = tag[0]['persist']
        else:
            # key? value? for the _dict?
            tags = create_obj_type_dict('tag', mi.pk('tag'))
            for entry in entries:
                entry['persist'] = tags[entry['tag']][0]['persist']

    if obj_type == 'controller-node':
        # This is a big odd, since the current node needs to be asked
        # which controller node it is
        url = "http://%s/rest/v1/system/controller" % sdnsh.controller

        result = sdnsh.store.rest_simple_request(url)
        check_rest_result(result)
        iam = json.loads(result)

        cluster_url = ("http://%s/rest/v1/system/ha/clustername"
                       % sdnsh.controller)
        result = sdnsh.store.rest_simple_request(cluster_url)
        check_rest_result(result)
        # perhaps ought to assert on lenresult) == 1
        clustername = json.loads(result)[0]['clustername']

        for entry in entries:
            controller = None
            if entry['id'] == iam['id']:
                controller = sdnsh.controller
            else:
                # find interfaces which hacve a firewall rule open for
                # tcp/80.  ie: ip for the interface with rest-api role
                ips = local_interfaces_firewall_open("tcp", 80, entry)

                # controller-interfaces needs to be examined to determine
                # if there's an ip address to use to discover the ha-role
                if len(ips) == 1:
                    # Not even certain if this is reachable
                    if ips[0]['discovered-ip'] != '':
                        controller = ips[0]['discovered-ip']
                    elif ips[0]['ip'] != '':
                        controller = ips[0]['ip']
                    else:
                        entry['ha-role'] = 'no-ip'
                        entry['errors'] = 'No IP Address'
                else:
                    entry['errors'] = 'No IP Address'

            if controller == None:
                entry['errors'] = 'No ip address configured'
                entry['ha-role'] = 'unknown'
                continue

            try:
                url = "http://%s/rest/v1/system/ha/role" % controller
                result = sdnsh.store.rest_simple_request(url, timeout = 2)
                check_rest_result(result)
                ha_role = json.loads(result)
                entry['ha-role'] = ha_role['role']
                if not 'clustername' in ha_role:
                    entry['errors'] = 'no clustername in ha-role rest api'
                    entry['ha-role'] = 'Untrusted: %s' % ha_role['role']
                elif ha_role['clustername'] != clustername:
                    entry['errors'] = 'Not in HA Cluster, requires decomission'
                    entry['ha-role'] = 'External Cluster: %s' % ha_role['role']
                if 'change-date-time' in ha_role:
                    entry['change-date-time'] = ha_role['change-date-time']
                if 'change-description' in ha_role:
                    entry['change-description'] = ha_role['change-description']
            except urllib2.HTTPError, e: # timeout?
                entry['errors'] = e.reason
                entry['ha-role'] = 'unknown'
                continue
            except urllib2.URLError, e: # timeout?
                entry['errors'] = '%s: %s' % (controller, e.reason)
                entry['ha-role'] = 'unknown'
                continue # dontt try the uptime, it will fail too
            except Exception, e:
                entry['errors'] = str(e)
                entry['ha-role'] = 'unknown'

            url = "http://%s/rest/v1/system/uptime" % controller
            try:
                result = sdnsh.store.rest_simple_request(url)
                check_rest_result(result)
                uptime = json.loads(result)
                entry['uptime'] = uptime['systemUptimeMsec']

            except Exception, e:
                pass

    return detail


def command_display_table(obj_type, data, detail = 'default',
                          table_format = None, title = None, scoped = None, sort = None):

    """
    Display entries from a obj_type, with some filtering done via data,
    and the output format described by table_format, with the devel of detail in detail

    @param obj_type string name of the object type
    @param data dictionary of configured data items from the description
    @param table_format string describing table format to use for output
    @param detail string describing the detail-flavor for format
    @param scoped string, when not null, indicates the submode level is used to filter query request
    @param sort string, describes sort to append to the query request
    """

    if not mi.obj_type_exists(obj_type):
        raise error.CommandDescriptionError("Unknown obj-type: %s" % obj_type)
        
    if sdnsh.description:   # description debugging
        print "command_display_table:", obj_type, data, table_format, detail, scoped, sort

    if 'detail' in data:
        detail = data['detail']

    if not table_format:
        if 'format' in data:
            table_format = data['format']
        else:
            table_format = obj_type
    if 'scoped' in data:
        scoped=data['scoped']
        del data['scoped']
    entries = command_query_object(obj_type, data, scoped, sort)
    if sdnsh.description:   # description debugging
        print "command_display_table: %d entries found, using %s" % (len(entries), data)

    # update any of the pretty-printer tables based on the obj_type
    obj_type_show_alias_update(obj_type)

    # with_key manages whether a 'detail' or table is displayed.
    with_key = '<with_key>' if detail == 'details' and len(entries) > 0 else '<no_key>'

    # pick foreign keys which are compound keys, explode these into fields
    fks = [x for x in mi.obj_type_foreign_keys(obj_type) if mi.is_compound_key(obj_type,x)]
    for entry in entries:
        for fk in fks:
            if fk in entry: # fk may be null-able
                mi.split_compound_into_dict(obj_type, fk, entry, True)
    #
    detail = command_display_table_join_entries(obj_type, data, entries, detail)

    # use display_obj_type_rows since it (currently) joins fields for obj_types.
    display = sdnsh.display_obj_type_rows(table_format, entries, with_key, detail)
    if title:
        return title + display
    return display


def command_display_rest_join_entries(table_format, data, entries, detail):
    """
    @param table_format string, identifying the final table output
    @param data   dict, used to query the rest api output
    @param entries list of dicts, ready to be displayed
    @return string replacing detail

    """

    if sdnsh.description:   # description debugging
        print "command_display_rest_join_entries: ", table_format, data, detail
        
    if table_format == 'controller-interface':
        # join firewall rules for these interfaces
        for intf in entries:
            rules = [x['rule'] for x in sdnsh.get_firewall_rules(intf['id'])]
            intf['firewall'] = ', '.join(rules)

    if table_format == 'system-clock':
        # join the 'time' string, possibly remove 'tz' from entries[0]
        entries[0]['time'] = sdnsh.get_clock_string(entries[0], data.get('detail'))
        return 'details' # force table format

    return detail


def command_display_rest_type_converter(table_format, rest_type, data, entries):
    """
    the expected display table_format is a list of dictionaries
    each dictionary has the field : value pairs.  Many rest api's
    return a dictionary of different layers, the description
    provides a rest-type, which is used to describe the form
    of the value returned from the rest api.
    """

    if sdnsh.description:   # description debugging
        print "command_display_rest_type_converter: ", table_format, rest_type
        
    if rest_type.startswith('dict-of-list-of-'):
        # entries look like { row_name : [value, ...], ... more-row-value-pairs }
        #
        # dict-of-list-of: a dict with key's which are given
        # the name of the first token, then the dict's value is
        # a list which can be given an associated name.
        # for example 'dict-of-list-of-cluster-id|[switches]'
        #
        # 'dict-of-list-of-switch' is a dict with key : value's
        # where the value is a list.  The member's of the list
        # are dictionaries.  the key of the outer dict is added to
        # each of the dicts, and this interior dict is added to
        # the final output list.

        # identify the added key from the rest_type
        key = rest_type.replace('dict-of-list-of-','')
        parts = key.split('|')
        names = None
        build_list = False
        if len(parts) > 0:
            key = parts[0]
            names = parts[1:] # should only be one name
            if len(names) > 0 and names[0][0] == '[':
                build_list = True
        formatted_list = []
        for (row_name, rows) in entries.items():
            if not rows:
                continue
            # use the names as ways of describing each of the list's items
            if type(rows) == list and build_list:
                # name[0] looks like '[switches]', requesting that this
                # list become switches : [rows] 
                formatted_list.append({key : row_name, names[0][1:-1] : rows})
            elif type(rows) == list:
                for row in rows:
                    add_dict = {key : row_name}
                    if type(row) == str or type(row) == unicode:
                        add_dict[names[0]] = row
                    elif type(row) == dict:
                        # addition names make no difference
                        add_dict.update(row)
                    formatted_list.append(add_dict)
            elif type(rows) == dict:
                do_append = True
                new_row = { key : row_name }
                for name in [x for x in names.keys() if x in row]:
                    item = row[name]
                    if type(item) == str or type(item) == unicode:
                        new_row[name] = item
                    if type(item) == dict:
                        new_row[name].update(item)
                    if type(item) == list:
                        do_append = False
                        for i_row in item:
                            new_row.update(i_row)
                            formatted_list.append(new_row)
                            new_row = { key : row_name }
                if do_append:
                    formatted_list.append(new_row)

        entries = formatted_list
    elif rest_type.startswith('dict-of-dict-of-'):
        # entries looks like { row_name : { [ { }, ... ] }  }
        #                                      ^
        #                            want this |
        # ie: dict with a value which is a dict, whose
        # 'dict-of-dict-of-switch|ports'  The dict has key : values
        # where the value is a dict.  That dict has the 'switch' : key
        # added, and it becomes the final output dict.
        #
        # if a second name is included, then the outer dict is 
        # examined to find these values (ie: values[names]), and these
        # get added to the final output dict.
        # 
        # identify the added key from the rest_type
        key = rest_type.replace('dict-of-dict-of-','')
        parts = key.split('|')
        name = None
        if len(parts) > 0:
            names = parts[1:]
            key = parts[0]
        formatted_list = []
        for (row_name, row) in entries.items():
            row[key] = row_name
            do_append = False
            if names:
                new_row = {}
                for name in names:
                    if name in row:
                        item = row[name]
                        if type(item) == str or type(item) == unicode:
                            new_row[name] = item
                            do_append = True
                        elif type(item) == dict:
                            if name == row_name:
                                do_append = True
                        elif type(item) == list:
                            for i_row in item:
                                row_items = {}
                                row_items[key] = row_name
                                row_items.update(i_row)
                                formatted_list.append(row_items)
                if do_append:
                    formatted_list.append(row)
                    
            else:
                formatted_list.append(row)
                                
        entries = formatted_list
    elif rest_type.startswith('dict-with-'):
        # rest result looks like: { k : v, k : { } }
        #                                       ^
        #                             want this |
        # dict-with: typically used for dict returns which have
        # nested dict's who's values are promoted to a single
        # list with a dict with these values.
        #
        # identify the added key from the rest_type
        key = rest_type.replace('dict-with-','')
        names = key.split('|')
        collect_row = {}
        formatted_list = []
        for name in names:
            if name in entries:
                item = entries[name]
                if type(item) == str or type(item) == unicode or \
                  type(item) == int or type(item) == long: # XXX float?
                    collect_row[name] = item
                elif type(item) == list:
                    for i_row in item:
                        row_items = {}
                        formatted_list.append(i_row)
                elif type(item) == dict:
                    collect_row.update(item)

        if len(collect_row) == 0:
            entries = formatted_list
        else:
            entries = [collect_row] + formatted_list

    elif rest_type == 'dict':
        entries = [entries]
    else:
        raise error.CommandDescriptionError("Unknown rest-type: %s" % rest_type)
    return entries


def missing_part(key_parts, entry, key_case = False):
    """
    Return the name of the missing field of one of the strings
    in the key_parts list when it  doesn't appear in the 'entry' dictionary.

    Return None otherwise.

    This is used to identify rows which don't have all the
    parts needed to constrcut a join key, or a db-table or
    query "key" to support addition of two different tables.

    @key_parts list of strings, 
    @entry dictionary, needs to contains each string in key_parts
    @key_case True when all key_parts may contain a leading '~' to
        denote the field needs to be lower cased for joining
    """
    for kn in key_parts:
        if not kn in entry:
            if key_case == False:
                return kn
            if kn[0] != '~':
                return kn
            if kn[1:] not in entry:
                return kn[1:]

    return None


def case_cvt(fn, f_dict):
    """
    For join operations, the fields in the partial result can no longer
    be associated with any obj-type, which means its impossible to determine
    whether the associated field is case sensitive.

    One approach to this problem is to case-normalize the obj-type's
    field values when they're first added to the row.   That doesn't
    help for rest-api's, which means it can only be a partial solution.
    In addition, it makes sense to preserve these values when possible,
    but still join based on case-normalization.
    """
    if fn[0] == '~':
        return str(f_dict.get(fn[1:], '').lower())
    return str(f_dict.get(fn, ''))


def obj_type_field_case(data, obj_type, field):
    """
    For objects where the case-normalization is identifed,
    manage conversion of the value associated with the field
    """
    case = mi.get_obj_type_field_case_sensitive(obj_type, field)
    return str(data[field]) if not case else str(utif.convert_case(case, data[field]))


def add_fields(dest, src):
    """
    These should both be dictionaries, leave the original entries in place
    when the 'dest' entries are populated from 'src'.   This operation is
    handy since the original 'dest' entries may differ from the 'src' due
    to case normalization.   Since having consistent names is a plus, by
    not updating the value with the 'src' entries, 'dest' retains its original
    values.
    """
    for (n,v) in src.items():
        if n not in dest:
            dest[n] = v
        elif str(dest[n]).lower() == str(v).lower:
            # should have better controls for when the case matters
            if sdnsh.description:
                print 'ADD %s skipping updating %s <-> %s' % (n, dest[n], v)
        else:
            dest[n] = v


def command_query_table(obj_type, data,
                        clear = True,
                        key = None, append = None, scoped = None, sort = None, crack = None):
    """
    Leave the result in command's global query_result, which can
    be used by other c_action steps

    'key' is one or more fields which are concatenated together to form
    the display-pipeline's version of a primary key.   It could be the
    actual primary key of the table, or it could be some fields which
    appear in all the rows.   Once the 'key' is constructed, it used to
    determine how results are added to the command.query_result.

    If the existing entries are to be 'cleared', then te primary key's
    are simply added to the table.  When the entries aren't cleared, then
    the computed primary key is used to join against existing items.

    Finally, the dict field name for the primary key is a single character: '@'
    This name was picked since its not possible for the database to ever
    use that name.
    """

    if not mi.obj_type_exists(obj_type):
        raise error.CommandDescriptionError("Unknown obj-type: %s" % obj_type)
        
    if sdnsh.description:   # description debugging
        print "command_query_table:", obj_type, data, clear, key, append, scoped, sort, crack

    if 'scoped' in data:
        scoped=data['scoped']
        del data['scoped']

    result = command_query_object(obj_type, data, scoped, sort)
    if sdnsh.description:   # description debugging
        print "command_query_table: %d entries found, using %s" % \
                (len(result), data)

    if crack:
        if crack == True:
            crack = mi.pk(obj_type)
        for entry in result:
            mi.split_compound_into_dict(obj_type, crack, entry, True)

    if append:
        for entry in result:
            if type(append) == dict:
                entry.update(append)
            elif type(append) == list:
                entry.update(dict(append))
            else:
                entry[append] = True

    # all the field from all the rows need to be present.
    if key:
        fields = key.split('|')

    if clear:
        command.query_result = result
        if key:
            for r in result:
                missing =  missing_part(fields, r)
                if missing:
                    if sdnsh.description:
                        print "command_query_table: ' \
                              ' missing field in row %s (%s) " % (missing, obj_type)
                    continue
                r['@'] = '|'.join([obj_type_field_case(r, obj_type, f) for f in fields])
    else:
        if key == None:
            if command.query_resuls != None:
                command.query_result += result
            else:
                command.query_result = result
        else:
            r_dict = {}
            for r in result:
                missing =  missing_part(fields, r)
                if missing:
                    if sdnsh.description:
                        print "command_query_table: ' \
                              ' missing field in row %s (%s) " % (missing, obj_type)
                    continue
                pk = '|'.join([r[f] for f in fields])
                r_dict[pk] = r
            if hasattr(command, 'query_result') and command.query_result:               
                for qr in command.query_result:
                    if '@' in qr and qr['@'] in r_dict:
                        add_fields(qr, r_dict[qr['@']])
                        del r_dict[qr['@']]
                command.query_result += r_dict.values()
            else:
                for (r, value) in r_dict.items():
                    value['@'] = '|'.join([value[f] for f in fields])
                command.query_result = r_dict.values()


def command_query_rest(data,
                       url = None, path = None, clear = True,
                       key = None, rest_type = None, scoped = None, sort = None, append = None):
    """
    Leave the result in command's global query_result, which can
    be used by other c_action steps (query-table, join-table, join-rest, display)

    'key' is one or more fields which are concatenated together to form
    the display-pipeline's version of a primary key.   It could be the
    actual primary key of the table, or it could be some fields which
    appear in all the rows.   Once the 'key' is constructed, it used to
    determine how results are added to the command.query_result.

    If the existing entries are to be 'cleared', then te primary key's
    are simply added to the table.  When the entries aren't cleared, then
    the computed primary key is used to join against existing items.

    Finally, the dict field name for the primary key is a single character: '@'
    This name was picked since its not possible for the database to ever
    use that name.

    """

    if sdnsh.description:   # description debugging
        print "command_query_rest:", url, path, rest_type, data, scoped, sort, append

    if url == None and path == None:
        raise error.CommandDescriptionError("missing url or path")
        
    if path:
        schema = sdnsh.sdndb.schema_detail(path)
        if schema:
            result = sdnsh.sdndb.data_rest_request(path)
        if key:
            # create a key dictionary, with the key values, pointing to
            # a psth in the schema.
            pass
        print 'PATH', path, result
    else:
        # if url is a list, pick the first one which can be build from the data
        if type(url) == list:
            select_url = url
        else:
            select_url = [url]

        use_url = None
        for u in select_url:
            try:
                use_url = (u % data)
                break
            except:
                pass

        if use_url == None:
            if sdnsh.description:   # description debugging
                print "command_query_rest: no url found"
            return

        query_url = "http://%s/rest/v1/" % sdnsh.controller + use_url

        if sdnsh.description:   # description debugging
            print "command_query_rest: query ", query_url
        try:
            result = sdnsh.store.rest_simple_request(query_url)
            check_rest_result(result)
            entries = json.loads(result)
        except Exception, e:
            if sdnsh.description or sdnsh.debug:
                print 'command_query_rest: ERROR url %s %s' % (url, e)
            entries = []

        if entries == None or len(entries) == 0:
            if sdnsh.description:   # description debugging
                print "command_query_rest: no new entries ", query_url
            if clear:
                command.query_result = None
            return

        # It certainly seems possible to map from url's to the type associated,
        # with the result, but it also makes sense to encode that type information
        # into the description
        if rest_type:
            result = command_display_rest_type_converter(None,
                                                         rest_type,
                                                         data,
                                                         entries)
            if sdnsh.description:   # description debugging
                print "command_query_rest: %s #entries %d " % (url, len(entries))
                print result
        else:
            result = []
            import fmtcnv
            if (onos == 1) and (url == 'links'):
                for entry in entries:
                    src = entry.get('src')
                    dst = entry.get('dst')
                    for tempEntry in entries:
                        if cmp(src, tempEntry.get('dst')) == 0:
                            if cmp(dst, tempEntry.get('src')) == 0:
                                entries.remove(tempEntry)
                    result.append({
                       'src-switch'          : fmtcnv.print_switch_and_alias(entry['src']['dpid']),
                       'src-port'            : entry['src']['portNumber'],
                       'src-port-state'      : 0,
                       'dst-switch'          : fmtcnv.print_switch_and_alias(entry['dst']['dpid']),
                       'dst-port'            : entry['dst']['portNumber'],
                       'dst-port-state'      : 0,
                       'type'                : entry['type'],
                    })
            else:
                result = entries
        
    if append:
        for entry in result:
            if type(append) == dict:
                entry.update(append)
            elif type(append) == list:
                entry.update(dict(append))
            else:
                entry[append] = True
    
    if key:
        fields = key.split('|')

    if clear:
        command.query_result = result
        if key:
            for r in result:
                r['@'] = '|'.join([r[f] for f in fields])
    else:
        if key == None:
            if command.query_result != None:
                command.query_result += result
            else:
                command.query_result = result
        else:
            r_dict = {}
            for r in result:
                missing =  missing_part(fields, r, key_case = True)
                if missing:
                    if sdnsh.description:
                        print "command_query_rest: missing field %s in row %s" % (missing, r)
                    continue
                pk = '|'.join([case_cvt(f, r) for f in fields])
                r_dict[pk] = r
            for qr in command.query_result:
                if '@' in qr and qr['@'] in r_dict:
                    add_fields(qr, r_dict[qr['@']])


def command_join_rest(url, data, key, join_field,
                      add_field = None, rest_type = None, crack = None, url_key = None):
    
    """
    url-key allows single row results to have a name:value added to the
    entry in situations where a single dictionary is computed after the
    rest-type conversion.   this allows simple results from the url to
    have a keyword added to allow joins.
    """
    if not hasattr(command, 'query_result'):
        if sdnsh.description:   # description debugging
            print "command_join_rest: no entries found"
        return
        
    if command.query_result == None:
        if sdnsh.description:   # description debugging
            print "command_join_rest: query_result: None"
        return

    if sdnsh.description:   # description debugging
        print "command_join_rest: %d entries found, using %s, url %s" % \
                (len(command.query_result), data, url)
        print "command_join_rest:", data, key, join_field

    if url == None:
        return
    if join_field == None:
        return
    if key == None:
        return


    # Collect all the queries, removing any duplicates
    queries = {}
    for entry in command.query_result:
        # if url is a list, pick the first one which can be build from the data
        if type(url) == list:
            select_url = url
        else:
            select_url = [url]

        use_url = None
        for u in select_url:
            try:
                use_url = (u % entry)
                break
            except:
                pass

        if use_url == None:
            if sdnsh.description:   # description debugging
                print "command_join_rest: no url found", url
            continue
        query_url = "http://%s/rest/v1/" % sdnsh.controller + use_url

        if sdnsh.description:   # description debugging
            print "command_join_rest: query ", query_url, entry
        if query_url in queries:
            continue
        
        try:
            result = sdnsh.store.rest_simple_request(query_url)
            check_rest_result(result)
            entries = json.loads(result)
        except Exception, e:
            entries = []

        if entries == None or len(entries) == 0:
            continue

        # It certainly seems possible to map from url's to the type associated,
        # with the result, but it also makes sense to encode that type information
        # into the description
        if rest_type:
            queries[query_url] = command_display_rest_type_converter(None,
                                                                     rest_type,
                                                                     data,
                                                                     entries)
            #
            # url_key allows the addition of a key for joining for single results
            if url_key and len(queries[query_url]) == 1:
                queries[query_url][0][url_key] = entry.get(url_key)
                
            if sdnsh.description:   # description debugging
                print "command_join_rest: %s #entries %d #result %s" % \
                        (url, len(entries), len(queries[query_url]))
        else:
            queries[query_url] = entries
            
    # From the query results, generate the dictionary to join through

    key_parts = key.split('|')      # all the fields needed to make a key
    key_dict = {}                   # resulting key dictionary
    for (url, value) in queries.items():
        for entry in value:
            # see if all the key parts are in the entry
            missing =  missing_part(key_parts, entry)
            if missing:
                if sdnsh.description:
                    print 'command_join_rest: missing field %s in %s' % (missing, entry)
                continue
            new_key = '|'.join([str(entry[kn]) for kn in key_parts])
            if sdnsh.description:   # description debugging
                print 'command_join_rest: new-key', new_key
            key_dict[new_key] = entry

    # Using the key-dictinoary, look for matches from the original entries

    if add_field:
        parts = add_field.split('|')
        from_fields = None
        if len(parts):
            add_field = parts[0]
            from_fields = parts[1:]

    join_parts = join_field.split('|')
    for entry in command.query_result:
        if len(join_parts):
            missing = missing_part(join_parts, entry, key_case = True)
            if missing:
                if sdnsh.description:   # description debugging
                    print "command_join_rest: missing field %s in %s" % (missing, entry)
                continue
                
            joiner = '|'.join([case_cvt(kn, entry) for kn in join_parts])
        else:
            if sdnsh.description:   # description debugging
                print "command_join_rest: joining ",  entry, join_field, entry.get(join_field)
            if not join_field in entry:
                continue
            joiner = case_cvt(join_field, entry)

        if sdnsh.description:   # description debugging
            print "command_join_rest: joining ",  entry, joiner, key_dict.get(joiner)

        if joiner in key_dict:
            # add all the entries from the key_dict 
            if sdnsh.description:   # description debugging
                print 'command_join_rest: ADD', key_dict[joiner]
            if add_field == None:
                add_fields(entry, key_dict[joiner])
            elif from_fields:
                if len(from_fields) == 1:
                    # add a single field
                    if from_fields[0] in key_dict[joiner]:
                        entry[add_field] = key_dict[joiner][from_fields[0]]
                else:
                    # add a dictionary
                    entry[add_field] = dict([[ff, key_dict[joiner][ff]]
                                              for ff in from_fields])
            else:
                entry[add_field] = key_dict[joiner]

    if sdnsh.description:   # description debugging
        print "command_join_rest: ", command.query_result


def command_join_table(obj_type, data, key, join_field,
                       key_value = None, add_field = None, crack = None):
    """
    Add fieds to the current command.query_result by looking up the entry in
    the db/store.   key represents the value of the index to use from
    the entries read from the database.  The key can be composed of
    multiple fields within the entry.   The join_field is the name
    of the field within the command.query_result to use as the value to match
    against the key field.

    When key_value is None, the matched entry from the join_field's is
    treated as a dictionary, and all the pair of name:values are added
    directly to the new entry.   

    When key_value is a field name, the joined entries are collected 
    as a list, and added to the new entry a the key_value name.
    (see the use of tag-mapping as an example)
    """
    if not hasattr(command, 'query_result'):
        if sdnsh.description:   # description debugging
            print "command_join_table: no entries found"
        return

    if command.query_result == None:
        if sdnsh.description:   # description debugging
            print "command_join_table: query_result: None"
        return
    
    if sdnsh.description:   # description debugging
        print "command_join_table: %d entries found, using %s, obj_type %s %s %s" % \
                (len(command.query_result), data, obj_type, key, join_field)
        print "command_join_table:", data, key, join_field

    if join_field == None:
        return
    if key == None:
        return

    if not mi.obj_type_exists(obj_type):
        raise error.CommandDescriptionError("Unknown obj-type: %s" % obj_type)

    # build the join_dict, which will have keys for the items to
    # add into the entries
    if not mi.obj_type_has_model(obj_type):
        entries = rest_to_model.get_model_from_url(obj_type, data)
    else:
        entries = sdnsh.get_table_from_store(obj_type)

    # determine whether specific field names are added
    if add_field:
        parts = add_field.split('|')
        from_fields = None
        if len(parts):
            add_field = parts[0]
            from_fields = parts[1:]

    # constuct the join key for each row from the db table
    key_parts = key.split('|')      # all the fields needed to make a key
    key_dict = {}                   # resulting key dictionary
    for entry in entries:
        # see if all the key parts are in the entry
        missing = missing_part(key_parts, entry)
        if missing:
            if sdnsh.description:   # description debugging
                print "command_join_table: missing field %s in %s" % (missing, entry)
            continue

        new_key = '|'.join([obj_type_field_case(entry, obj_type, kn) for kn in key_parts])
        if sdnsh.description:   # description debugging
            print 'command_join_table: new-key', new_key, key_value
        if key_value:
            if not new_key in key_dict:
                key_dict[new_key] = [entry]
            else:
                key_dict[new_key].append(entry)
        else:
            key_dict[new_key] = entry


    # let 'crack' contain the field's name, not a boolean.
    if crack and crack == True:
        crack = mi.pk(obj_type)

    # Using the key-dictinoary, look for matches from the original entries

    join_parts = join_field.split('|')
    for entry in command.query_result:
        if len(join_parts):
            missing =  missing_part(join_parts, entry, key_case = True)
            if missing:
                if sdnsh.description:   # description debugging
                    print "command_join_table: missing field %s in %s" % (missing, entry)
                continue
                
            joiner = '|'.join([case_cvt(kn, entry) for kn in join_parts])
        else:
            if sdnsh.description:   # description debugging
                print "command_join_table: joining ",  entry, join_field, entry.get(join_field)
            if not join_field in entry:
                continue
            joiner = case_cvt(join_field, entry)

        if joiner in key_dict:
            if crack:
                if not crack in key_dict[entry[joiner]]:
                    if sdnsh.description:   # description debugging
                        print "command_join_table: field %s not in entry" % crack, key_dict[joiner]
                else:
                    mi.split_compound_into_dict(obj_type, crack, key_dict[joiner], True)

            # add all the entries from the key_dict 
            if sdnsh.description:   # description debugging
                print 'command_join_table: ADD %s as %s ' % (key_dict[joiner], add_field)
            if add_field == None:
                if key_value:
                    entry[key_value] = key_dict[joiner]
                else:
                    add_fields(entry, key_dict[joiner])
            elif from_fields:
                if len(from_fields) == 1:
                    # add a single field
                    if type(key_dict[joiner]) == list:
                        entry[add_field] = [x[from_fields[0]] for x in key_dict[joiner]]
                    else:
                        entry[add_field] = key_dict[joiner][from_fields[0]]
                else:
                    # add a dictionary with named fields
                    if type(key_dict[joiner]) == list:
                        for item in key_dict[joiner]:
                            entry[add_field] = dict([[ff, item[ff]]
                                                      for ff in from_fields])
                    else:
                        entry[add_field] = dict([[ff, key_dict[joiner][ff]]
                                                  for ff in from_fields])
                    
            else:
                entry[add_field] = key_dict[joiner]

    if sdnsh.description:   # description debugging
        print "command_join_table: ", command.query_result


def command_display_rest(data, url = None, sort = None, rest_type = None,
                         table_format = None, title = None, detail = None):
    """
    Perform a call to the rest api, and format the result.

    When sort isn't None, it names a field whose's value are sorted on.
    """
    #just a hack check to implement decending sorting
    descending = False
    #raise error.ArgumentValidationError('\n\n\n %s' % (descending))
    if sdnsh.description:   # description debugging
        print "command_display_rest: ", data, url, rest_type, table_format, detail

    if not url:
        url = data.get('url')
    if not table_format:
        table_format = data.get('format')

    check_single_entry = True

    # if url is a list, pick the first one which can be build from the data
    select_url = url
    if url and type(url) == list:
        for u in url:
            try:
                select_url = (u % data)
                select_url = u # select this url from the list
                break
            except:
                pass

    if not detail:
        detail = data.get('detail', 'default')
    url = "http://%s/rest/v1/" % sdnsh.controller + (select_url % data)

    result = sdnsh.store.rest_simple_request(url)
    check_rest_result(result)
    if sdnsh.description:   # description debugging
        print "command_display_rest: result ", result
    entries = json.loads(result)
        #rest_type = None
        #raise error.ArgumentValidationError('\n\n\n %s' % (attributes))
    #if 'realtimestats' in data and data['realtimestats'] == 'group':

    entries2 = None


    if 'realtimestats' in data and data['realtimestats'] == 'group':
        url2 = "http://%s/rest/v1/" % sdnsh.controller + ("realtimestats/groupdesc/%(dpid)s/" % data)
        result2 = sdnsh.store.rest_simple_request(url2)
        check_rest_result(result2)
        if sdnsh.description:   # description debugging
            print "command_display_rest: groupdesc result ", result2
        entries2 = json.loads(result2)
        
    # It certainly seems possible to map from url's to the type associated,
    # with the result, but it also makes sense to encode that type information
    # into the description
    if 'routerrealtimestats' in data  and data['routerrealtimestats'] == 'adjacency':
        rest_type =False
    if rest_type:
        entries = command_display_rest_type_converter(table_format,
                                                      rest_type,
                                                      data,
                                                      entries)
        if 'realtimestats' in data and data['realtimestats'] == 'group':
            if entries2 is not None:
                entries2 = command_display_rest_type_converter(table_format,
                                                      rest_type,
                                                      data,
                                                      entries2)

    if 'router' in data  and data['router'] == 'router':
        combResult = []
        for entry in entries:
            attributes = entry.get('stringAttributes')
            #raise error.ArgumentValidationError('\n\n\n %s' % (attributes))
            combResult.append({
                       'dpid'           : entry.get('dpid'),
                       'routerIP'       : attributes['routerIp'],
                       'name'           : attributes['name'],
                       'isEdgeRouter'   : attributes['isEdgeRouter'],
                       'routerMac'      : attributes['routerMac'],
                       'nodeSId'        : attributes['nodeSid'],
                       },)
        entries = combResult
    #raise error.ArgumentValidationError('\n\n\n %s' % (entries))
    if 'routerrealtimestats' in data  and data['routerrealtimestats'] == 'port':
        #raise error.ArgumentValidationError('\n\n\n %s' % (data))
        combResult = []
        portList = entries
        for port in portList:
            portData = port.get("port")
            name = portData.get("stringAttributes").get('name')
            portNo = portData.get("portNumber") & 0xFFFF # converting to unsigned16int
            subnetIp = port.get("subnetIp")
            adjacency = str(port.get('adjacency'))
            combResult.append({
                               'name'            :name,
                               'portNo'           : portNo,
                               'subnetIp'         : subnetIp,
                               'adjacency'        : adjacency,
                               })
        entries = combResult
    if 'routerrealtimestats' in data  and data['routerrealtimestats'] == 'adjacency':
        #raise error.ArgumentValidationError('\n\n\n %s' % (entries))
        #raise error.ArgumentValidationError('\n\n\n %s' % (entries))
        combResult = []
        adjacencyPairList = entries
        for adjacencyPair in adjacencyPairList:
            adjacencySid = adjacencyPair.get("adjacencySid")
            ports = adjacencyPair.get("ports")
            combResult.append({
                               'adjacencySid'    : adjacencySid,
                               'ports'           : ports,
                               })
        entries = combResult
    #raise error.ArgumentValidationError('\n\n\n %s' % (data))

    if 'showtunnel' in data  and (data['showtunnel'] == 'tunnel' or data['detail'] == 'details'):
        #eraise error.ArgumentValidationError('\n\n\n %s' % (entries))
        combResult = []
        tunnelList = entries
        for tunnel in tunnelList:
            labelStackList = (tunnel.get('labelStack'))
            labelStackString = str(labelStackList)
            labelStackString = remove_unicodes(labelStackString)
            #labelStackList = (tunnel.get('labelStack'))
            #labelStackString ='['
            #for labelSack in labelStackList:
            #    for label in labelSack:
            #        labelStackString += (label + ',')
            #if labelStackString == '[':
            #    labelStackString = ''
            #else:
            #    labelStackString = labelStackString[:-1]
            #    labelStackString += ']'
            tunnelId = tunnel.get('tunnelId')
            tunnelPath = tunnel.get('tunnelPath')
            dpidGroup = str(tunnel.get('dpidGroup'))
            dpidGroup= remove_unicodes(dpidGroup)
            policies = tunnel.get('policies')
            combResult.append({
                               'tunnelId'       : tunnelId,
                               'labelStack'     : labelStackString,
                               'dpidGroup'      : dpidGroup,
                               'tunnelPath'     : tunnelPath,
                               'policies'       : policies,
                               })
        entries = combResult

    if 'showpolicy' in data  and data['showpolicy'] == 'policy':
        #raise error.ArgumentValidationError('\n\n\n %s' % (data))
        combResult = []
        portList = entries
        for policy in portList:
            policyId = policy.get("policyId")
            policyType = policy.get("policyType")
            priority = policy.get("priority")
            tunnelId = policy.get('tunnelId')
            match = policy.get("match")
            dstIpAddress = match.get('dstIpAddress')['value'] if match.get('dstIpAddress') else '*'
            dstMacAddress = match.get('dstMacAddress')['value'] if match.get('dstMacAddress') else '*'
            dstTcpPortNumber = match.get('dstTcpPortNumber') if match.get('dstTcpPortNumber') else '*'
            etherType = ('0x'+ str(match.get('etherType'))) if match.get('etherType') else '*'
            ipProtocolNumber = match.get('ipProtocolNumber') if match.get('ipProtocolNumber') else '*'
            srcIpAddress = match.get('srcIpAddress')['value'] if match.get('srcIpAddress') else '*'
            srcMacAddress = match.get('srcMacAddress')['value'] if match.get('srcMacAddress') else '*'
            srcTcpPortNumber = match.get('srcTcpPortNumber') if match.get('srcTcpPortNumber') else '*'
            combResult.append({
                               'policyId'        : policyId,
                               'policyType'      : policyType,
                               'tunnelId'        : tunnelId,
                               'priority'        : priority,
                               'dstIpAddress'    : dstIpAddress,
                               'dstMacAddress'   : dstMacAddress,
                               'dstTcpPortNumber': dstTcpPortNumber,
                               'etherType'       : etherType,
                               'ipProtocolNumber': ipProtocolNumber,
                               'srcIpAddress'    : srcIpAddress,
                               'srcMacAddress'   : srcMacAddress,
                               'srcTcpPortNumber': srcTcpPortNumber,
                               
                               })
        entries = combResult

    if 'realtimestats' in data and 'tabletype' in data and data['realtimestats'] == 'table':
        combResult = []
        if data['tabletype'] == 'ip':
            #for decending sorting
            descending = True
            for ipTableEntry in entries:
                match = ipTableEntry['match']
                networkDestination = '*'
                if match :
                    networkDestination = match.get('networkDestination') if match.get('networkDestination') else '*'
                    #raise error.ArgumentValidationError('\n\n\n %s' % json.tool(entries))
                instructions = ipTableEntry['instructions']
                actions = str(instructions[0]) if instructions[0] else None
                if actions != None:
                    actions = remove_unicodes(actions)
                    actions = renameActions(actions)
                    actions = actions.lower()
                else:
                    actions =''
                combResult.append({
                       'switch'        : ipTableEntry['switch'],
                       'byteCount'     : ipTableEntry['byteCount'],
                       'packetCount'   : ipTableEntry['packetCount'],
                       'priority'      : ipTableEntry['priority'],
                       'cookie'        : ipTableEntry['cookie'],
                       'durationSeconds'         : ipTableEntry['durationSec'],
                       'networkDestination'      : networkDestination,
                       'actions'                 : actions,
                    })
        elif data['tabletype'] == 'mpls':
            for ipTableEntry in entries:
                match = ipTableEntry['match']
                mplsTc =  '*'
                mplsLabel = '*'
                mplsBos = '*'
                if match :
                    mplsTc = match.get('mplsTc') if match.get('mplsTc') else '*'
                    mplsLabel = match.get('mplsLabel') if match.get('mplsLabel') else '*'
                    mplsBos = match.get('mplsBos') if match.get('mplsBos') else '*'
                instructions = ipTableEntry['instructions']
                #raise error.ArgumentValidationError('\n\n\n %s' %len(actions))
                actions = str(instructions[0])if instructions[0] else None
                if actions != None:
                    actions = remove_unicodes(actions)
                    actions = renameActions(actions)
                    actions = actions.lower()
                else:
                    actions =''
                combResult.append({
                       'switch'        : ipTableEntry['switch'],
                       'byteCount'     : ipTableEntry['byteCount'],
                       'packetCount'   : ipTableEntry['packetCount'],
                       'cookie'        : ipTableEntry['cookie'],
                       'priority'      : ipTableEntry['priority'],
                       'mplsTc'         : mplsTc,
                       'mplsLabel'      : mplsLabel,
                       'mplsBos'        : mplsBos,
                       'durationSeconds'        : ipTableEntry['durationSec'],
                       'actions'        : actions
                    })
        elif data['tabletype'] == 'acl':
            descending = True
            for ipTableEntry in entries:
                match = ipTableEntry['match']
                networkDestination ='*'
                networkProtocol = '*'
                networkSource = '*'
                mplsTc = '*'
                mplsLabel = '*'
                mplsBos = '*'
                transportDestination = '*'
                inputPort = '*'
                transportSource = '*'
                dataLayerSource = '*'
                dataLayerDestination = '*'
                dataLayerType = '*'
                if match :
                    networkDestination = match.get('networkDestination') if match.get('networkDestination') else '*'
                    networkProtocol = match.get('networkProtocol') if match.get('networkProtocol') else '*'
                    networkSource = match.get('networkSource') if match.get('networkSource') else '*'
                    mplsTc = match.get('mplsTc') if match.get('mplsTc') else '*'
                    mplsLabel = match.get('mplsLabel')if match.get('mplsLabel') else '*'
                    transportDestination = match.get('transportDestination') if match.get('transportDestination') else '*'
                    transportSource = match.get('transportSource') if match.get('transportSource') else '*'
                    inputPort = match.get('inputPort') if match.get('inputPort') else '*'
                    dataLayerSource = match.get('dataLayerSource') if match.get('dataLayerSource') else '*'
                    dataLayerDestination = match.get('dataLayerDestination') if match.get('dataLayerDestination') else '*'
                    dataLayerType= match.get('dataLayerType') if match.get('dataLayerType') else '*'
                    mplsBos = match.get('mplsBos') if match.get('mplsBos') else '*'
                instructions = ipTableEntry['instructions']
                actions = str(instructions[0])if instructions[0] else None
                if actions != None:
                    actions = remove_unicodes(actions)
                    actions = renameActions(actions)
                    actions = actions.lower()
                else:
                    actions = ''
                combResult.append({
                       'switch'        : ipTableEntry['switch'],
                       'byteCount'     : ipTableEntry['byteCount'],
                       'packetCount'   : ipTableEntry['packetCount'],
                       'cookie'        : ipTableEntry['cookie'],
                       'priority'      : ipTableEntry['priority'],
                       'inputPort'               : inputPort,
                       'durationSeconds'         : ipTableEntry['durationSec'],
                       'networkSource'           : networkSource,
                       'networkDestination'      : networkDestination,
                       'networkProtocol'         : networkProtocol,
                       'dataLayerType'           : dataLayerType,
                       'dataLayerSource'         : dataLayerSource,
                       'dataLayerDestination'    : dataLayerDestination,
                       'mplsTc'                  : mplsTc,
                       'mplsLabel'               : mplsLabel,
                       'mplsBos'                 : mplsBos,
                       'transportDestination'    : transportDestination,
                       'transportSource'         : transportSource,
                       'actions'                 : actions
                    })
        entries = combResult

    if 'realtimestats' in data and data['realtimestats'] == 'group':
        combResult = []
        for groupStatEntry in entries:
            groupId = groupStatEntry["groupId"]
            groupDescEntry = None
            for entry in entries2:
                if groupId == entry["groupId"]:
                    groupDescEntry = entry
                    break
            if groupDescEntry is '':
                print "command_display_rest: missing group desc for group id %s" % (groupId)
                continue
            
            if (len(groupStatEntry['bucketStats']) > 0):
                for bucketId in range(len(groupStatEntry['bucketStats'])):
                    setsrcmac = ''
                    if 'SET_DL_SRC' in groupDescEntry['bucketsActions'][bucketId]:
                        setsrcmac = groupDescEntry['bucketsActions'][bucketId]['SET_DL_SRC']
                    setdstmac = ''
                    if 'SET_DL_DST' in groupDescEntry['bucketsActions'][bucketId]:
                        setdstmac = groupDescEntry['bucketsActions'][bucketId]['SET_DL_DST']
                    pushmpls = ''
                    if 'PUSH_MPLS_LABEL' in groupDescEntry['bucketsActions'][bucketId]:
                        pushmpls = groupDescEntry['bucketsActions'][bucketId]['PUSH_MPLS_LABEL']
                    popmpls = ''
                    if 'POP_MPLS' in groupDescEntry['bucketsActions'][bucketId]:
                        popmpls = groupDescEntry['bucketsActions'][bucketId]['POP_MPLS']
                    outport = ''
                    if 'OUTPUT' in groupDescEntry['bucketsActions'][bucketId]:
                        outport = groupDescEntry['bucketsActions'][bucketId]['OUTPUT']
                    goToGroup = ''
                    if 'goToGroup' in groupDescEntry['bucketsActions'][bucketId]:
                        goToGroup = groupDescEntry['bucketsActions'][bucketId]['goToGroup']
                    setBos= ''
                    if 'PUSH_MPLS_BOS' in groupDescEntry['bucketsActions'][bucketId]:
                        setBos = groupDescEntry['bucketsActions'][bucketId]['PUSH_MPLS_BOS']
                    COPY_TTL_IN= ''
                    if 'COPY_TTL_IN' in groupDescEntry['bucketsActions'][bucketId]:
                        COPY_TTL_IN = groupDescEntry['bucketsActions'][bucketId]['COPY_TTL_IN']
                    COPY_TTL_OUT= ''
                    if 'COPY_TTL_OUT' in groupDescEntry['bucketsActions'][bucketId]:
                        COPY_TTL_OUT = groupDescEntry['bucketsActions'][bucketId]['COPY_TTL_OUT']
                    DEC_MPLS_TTL= ''
                    if 'DEC_MPLS_TTL' in groupDescEntry['bucketsActions'][bucketId]:
                        DEC_MPLS_TTL = groupDescEntry['bucketsActions'][bucketId]['DEC_MPLS_TTL']
                    DEC_NW_TTL= ''
                    if 'DEC_NW_TTL' in groupDescEntry['bucketsActions'][bucketId]:
                        DEC_NW_TTL = groupDescEntry['bucketsActions'][bucketId]['DEC_NW_TTL']
    
                    combResult.append({
                           'groupid'       : groupId,
                           'grouptype'     : groupDescEntry['groupType'],
                           'totalpktcnt'   : groupStatEntry['packetCount'],
                           'totalbytecnt'  : groupStatEntry['byteCount'],
                           'bucketpktcnt'  : groupStatEntry['bucketStats'][bucketId]['pktCount'],
                           'bucketbytecnt' : groupStatEntry['bucketStats'][bucketId]['byteCount'],
                           'setsrcmac'     : setsrcmac,
                           'setdstmac'     : setdstmac,
                           'pushMplsLabel' : pushmpls,
                           'popmpls'       : popmpls,
                           'outport'       : outport,
                           'goToGroup'     : goToGroup,
                           'setBos'        : setBos,
                           'COPY_TTL_IN'   : COPY_TTL_IN,
                           'COPY_TTL_OUT'  : COPY_TTL_OUT,
                           'DEC_MPLS_TTL'  : DEC_MPLS_TTL,
                           'DEC_NW_TTL'    : DEC_NW_TTL,
                        })
            else:
                combResult.append({
                           'groupid'       : groupId,
                           'grouptype'     : groupDescEntry['groupType'],
                           'totalpktcnt'   : groupStatEntry['packetCount'],
                           'totalbytecnt'  : groupStatEntry['byteCount'],
                           'bucketpktcnt'  : '',
                           'bucketbytecnt' : '',
                           'setsrcmac'     : '',
                           'setdstmac'     : '',
                           'pushMplsLabel' : '',
                           'popmpls'       : '',
                           'outport'       : '',
                           'goToGroup'     : '',
                           'setBos'        : '',
                           'COPY_TTL_IN'   : '',
                           'COPY_TTL_OUT'  : '',
                           'DEC_MPLS_TTL'  : '',
                           'DEC_NW_TTL'    : '',
                        })
        entries = combResult
    #
    if format:
        #
        detail = command_display_rest_join_entries(table_format, data, entries, detail)
        #if 'realtimestats' in data and data['realtimestats'] == 'flow':
        #    entries = sdnsh.fix_realtime_flows(entries)
        #    check_single_entry = False

        if 'realtimestats' in data and data['realtimestats'] == 'features':
            for entry in entries:
                entry['stp-state'] = entry['state']

        # update any of the pretty-printer tables based on the table_format (obj_type)
        obj_type_show_alias_update(table_format % data)

        if check_single_entry and entries and len(entries) == 1 and detail == 'details':
            return sdnsh.pp.format_entry(entries[0],
                                        table_format % data,
                                        detail,
                                        sdnsh.debug)
        if sort:
            if descending:
                reverse = True
            else:
                reverse = False
            def sort_cmp(x,y):
                for f in sort:
                    if f in x:
                        c = cmp(x.get(f), y.get(f)) 
                        if c != 0:
                            return c
                return 0
            entries = sorted(entries,  cmp=sort_cmp, reverse=reverse )
        if 'realtimestats' in data and data['realtimestats'] == 'group':
            repeatGroupId = -1
            length = len(entries)
            for i in range(0, length):
                entry = entries[i]
                groupId = entry.get('groupid')
                if groupId == repeatGroupId:
                    entries[i]['groupid'] = ''
                else:
                    repeatGroupId = groupId

        display = sdnsh.pp.format_table(entries, table_format % data, detail)
    else:
        display = entries

    if title:
        return title + display
    return display


def command_crack(field):
    """
    Part of the show pipeline, split is typically used with rest api's
    not associated with the model (database), since the cli has enough
    details of the relationships between model fields to understand 
    which of the fields has a compound key value, and has options to
    crack those into component parts.

    The operation is called 'crack' (not split), since the other
    options for some of the actions is called 'crack'

    The field identifies the name of the field in the entry to
    split into parts, and the remaining '|' separated fields list
    the labels to associate in the result from each of the 
    split components.   Currently, the 'crack' character is '|',
    although this could be parameterized.
    """
    if sdnsh.description:   # description debugging
        print "command_split: ", field

    if hasattr(command, 'query_result'):
        entries = command.query_result
        if command.query_result == None:
            entries = []
    else:
        if sdnsh.description:   # description debugging
            print "command_join_table: no entries found"
        entries = []
    
    parts = field.split('|')
    if len(parts) == 0:
        if sdnsh.description:   # description debugging
            print "command_join_table: field doesn't contain labels" \
                   " use field|label1|label2|..."
        return

    field = parts[0]
    label = parts[1:]
    many = len(label)

    for entry in entries:
        if field in entry:
            parts = entry[field].split('|')
            if len(parts) and many >= len(parts) :
                # use enumerate to create a tuple for each item in parts,
                # assocaiting an index, which can be used to identify the
                # label to use for each of the elements; from that create
                # a dictionay, which is then used to update the entry
                entry.update(dict([[label[n],p] for (n,p) in enumerate(parts)]))


def command_display(data, table_format, detail = 'default', sort = None, title = None):
    
    if sdnsh.description:   # description debugging
        print "command_display: ", data, table_format, detail

    if 'detail' in data:
        detail = data['detail']

    if hasattr(command, 'query_result'):
        entries = command.query_result
        if command.query_result == None:
            entries = []
    else:
        if sdnsh.description:   # description debugging
            print "command_join_table: no entries found"
        entries = []
    
    if sdnsh.description:   # description debugging
        print "command_display: #entries ", len(entries)

    # XXX controller-node has an odd url, join-rest needs to be able to
    # be handed a complete url, and replace the ip address with the controller's
    # ip address.
    detail = command_display_table_join_entries(table_format, data, entries, detail)

    # update any of the pretty-printer tables based on the table_format (obj_type)
    obj_type_show_alias_update(table_format)

    # with_key manages whether a 'detail' or table is displayed.
    with_key = '<with_key>' if detail == 'details' and len(entries) > 0 else '<no_key>'

    # 
    if sort:
        def sort_cmp(x,y):
            for f in sort:
                if f in x:
                    c = utif.trailing_integer_cmp(x.get(f),y.get(f))
                    if c:
                        return c
            return 0
        entries = sorted(entries, cmp=sort_cmp)

    # use display_obj_type_rows since it (currently) joins fields for obj_types.
    display = sdnsh.display_obj_type_rows(table_format, entries, with_key, detail)

    if title:
        return title + display
    return display


def command_legacy_cli(obj_type, data, detail = 'default', scoped = None, sort = None):
    """
    Unfortunatly, the command descriptions don't have enough different
    detail to describe how to join specific distinct fields.  In the future,
    there will be rest api's for each of the cli requests; that should cause
    this trampoline code to become obsolete.
    """

    if sdnsh.description:   # description debugging
        print "command_legacy_cli: ", obj_type, data, detail, scoped, sort

    # update any of the pretty-printer tables based on the obj_type
    obj_type_show_alias_update(obj_type)

    #
    #
    # Various show command 'join' data to create a table not
    # directly available in the REST API, someday in the future,
    # these joins will be directly implemented in the REST API,
    # but these special cases still exist:
    #
    if 'running-config' in data:
        result = sdnsh.error_msg("No running-config choice")
        words = []
        if 'word' in data and data['word'] != 'all':
            words = [data['word']]

        if data['running-config'] == 'running-config':
            # 'show vns XXX running-config'
            if 'vnsname' in data and data['vnsname'] != 'all':
                return sdnsh.show_vns_running_config(data['vnsname'],data['tenant'])
            elif 'vns' in data and data['vns']=='all':
                data['running-config'] = 'vns'
            elif 'tenant' in data:
                data['running-config']='tenant'
                words=[data['tenant']]
        if data['running-config'] in run_config.registry_items_enabled():
            result = run_config.perform_running_config(data['running-config'], sdnsh, config, words)

        if result:
            return result
        return ''.join(config)

    if obj_type == 'running-config':
        return run_config.implement_show_running_config([])
            
    if obj_type == 'vns-interface':
        if scoped:
            # should check for missing 'id' in data
            data['vns'] = sdnsh.get_current_mode_obj()

        if 'vns' in data:
            if data['vns'] == 'all':
                return sdnsh.display_vns_interface(None, {}, '<no_key>')
            vns_name=data['vns']
            return sdnsh.display_vns_interface(vns_name, {'vns': vns_name },
                                           '<no_key>', detail = 'scoped')
        
    if obj_type == 'vns-switch-ports':
        if 'vns' in data:
            return sdnsh.show_vns_switch_ports([data['vns']])
        return sdnsh.show_vns_switch_ports([])

    if obj_type == 'switch-ports-vns':
        if 'dpid' in data:
            return sdnsh.show_switch_ports_vns([data['dpid']])
        return sdnsh.show_switch_ports_vns([])

    if obj_type == 'switch-interfaces':
        key = mi.pk(obj_type)
        if scoped:
            data['dpid'] = sdnsh.get_current_mode_obj()

        # in legacy_cli to join the switch-interfaces with port stats
        port_obj = 'port'
        entries = sdnsh.show_sort_obj_type(obj_type, 
                            command_query_object(port_obj, data, scoped, sort))

        # switch-interfaces is really class Port, and the primary key
        # is '#|switch|number, not name.

        entries_dict = dict([['%s|%s' % (x['switch'], x['name']), x] for x in entries])
        # collect switch-interface-config
        sic = 'switch-interface-config'
        if 'dpid' in data and data['dpid'] != 'all':
            sic_dict = create_obj_type_dict(sic, mi.pk(sic), mi.pk(sic), data['dpid'])
        else:
            sic_dict = create_obj_type_dict(sic, mi.pk(sic))

        # add switch-interface-config names when missing
        for (sic_id, sic_value) in sic_dict.items():
            if not sic_id in entries_dict:
                # add 'state' to this item for prettyprinting column width computation
                for sv in sic_value:
                    sv['state'] = ''
                entries += sic_value
                                                  
        # collect the stats for the interfaces
        stats_url = 'realtimestats/port/%(dpid)s/' % data
        url = "http://%s/rest/v1/" % sdnsh.controller + stats_url
        try:
            result = sdnsh.store.rest_simple_request(url)
            check_rest_result(result)
            stats = json.loads(result)

        except Exception, e:
            stats = {}

        # join realtimestats
        for entry in entries:
            if 'state' in entry:
                entry['stp-state'] = entry['state']
            stats_list = stats.get(entry['switch'])
            # Note, 'number' may be missing from entry if the switch
            # matches for switch-interface-config but the interface name
            # doesn't show up.
            if stats_list and 'number' in entry:
                ifn = entry['number']
                # Notice that the realtime stat's use a int for the 2^16 value here
                # The & 0xffff converts the "-x" to a positive 2^16 value
                item = [x for x in stats_list if (x['portNumber'] & 0xffff) == ifn]
                if len(item) == 1:
                    entry.update(item[0])
            if entry['id'] in sic_dict:
                entry.update(sic_dict[entry['id']][0])

        # Update the alias mappings for display
        obj_type_show_alias_update(obj_type)

        return sdnsh.pp.format_table(entries, obj_type, detail)

    if obj_type == 'tunnel-interfaces':
        # Use the active tunnels to identify the interfaces on the
        # switches which are the tunneling interfaces, with that
        # collect to port -> if_name mappings from 'port', then
        # find all the switches interfaces, convert those port numbers to
        # if names, to collect only tunneling interfaces.  Now collect
        # realtimestats for the switch's ports, and associate those
        # stats with any filtered interfaces, finally display the result
        tunnel_url = "tunnel-manager/%(dpid)s" % data
        url = "http://%s/rest/v1/" % sdnsh.controller + tunnel_url
        result = sdnsh.store.rest_simple_request(url)
        check_rest_result(result)
        tunnels = json.loads(result)

        # use the active tunnels to
        # collect dpid's, convert the remote ip's to interface names.
        tunnel_ports = {}
        for t in tunnels:
            quad = t['tunnelPorts'].split('.')
            if_name = "vta%03d%03d%03d%03d" % (int(quad[0]), int(quad[1]),
                                               int(quad[2]), int(quad[3]))
            key = "%s|%s" % (t['dpid'], if_name)
            if not key in tunnel_ports:
                tunnel_ports[key] = {t['dpid']: t['tunnelPorts']}

        # Collect interfaces on associated switch
        port_obj = 'port'
        entries = sdnsh.show_sort_obj_type(port_obj,
                            command_query_object(port_obj, data, scoped, sort))
        # Associate port names with interface names
        port_to_if_name = {}

        try:
            ports = sdnsh.get_table_from_store("port")
        except Exception, e:
            port = []

        for port in ports:
            key_string = '%s|%s' % (port['switch'], port['number'])
            port_to_if_name[key_string] = port['name']

        # Filter elements, 'filtered' only contains active tunnel interfaces
        filtered = []
        for e in entries:
            e['ifname'] = port_to_if_name[e['id']]
            key = '%s|%s' % (e['switch'], e['ifname'])
            if sdnsh.description:   # description debugging
                print command._line(), key
            if key in tunnel_ports:
                if sdnsh.description:   # description debugging
                    print command._line(), "Found ", e['id']
                filtered.append(e)
        entries = filtered

        # collect switch-interface-config
        sic = 'switch-interface-config'
        if 'dpid' in data:
            sic_dict = create_obj_type_dict(sic, mi.pk(sic), mi.pk(sic), data['dpid'])
        else:
            sic_dict = create_obj_type_dict(sic, mi.pk(sic))
                                                  
        # collect the stats for the interfaces
        stats_url = 'realtimestats/port/%(dpid)s/' % data
        url = "http://%s/rest/v1/" % sdnsh.controller + stats_url
        try:
            result = sdnsh.store.rest_simple_request(url)
            check_rest_result(result)
            stats = json.loads(result)
        except Exception, e:
            stats = {}

        # join realtimestats
        for entry in entries:
            if 'state' in entry:
                entry['stp-state'] = entry['state']
            stats_list = stats.get(entry['switch'])
            if stats_list and 'number' in entry:
                ifn = entry['number']
                # Notice that the realtime stat's use a int for the 2^16 value here
                # The & 0xffff converts the "-x" to a positive 2^16 value
                item = [x for x in stats_list if (x['portNumber'] & 0xffff) == ifn]
                if len(item) == 1:
                    entry.update(item[0])
            if entry['id'] in sic_dict:
                entry.update(sic_dict[entry['id']][0])

        obj_type_show_alias_update('switch-interfaces')

        return sdnsh.pp.format_table(entries, 'switch-interfaces', detail)
        
    if obj_type == 'host-vns-interface-vns':
        words = []
        for w in []: # list of options to display_vns_mac_address_table
            if w in data:
                words[w] = data[w]

        return sdnsh.display_vns_mac_address_table(data['vns'], words)

    if obj_type == 'config':
        if 'config' in data:
            if 'version' in data:
                return sdnsh.implement_show_config([data['config'],data['version']])
            return sdnsh.implement_show_config([data['config']])

        if 'config-diff' in data:
            if 'version' in data:
                return sdnsh.implement_show_config([ data['first'],
                                                     'diff',
                                                      data['second'],
                                                      data['version']])
            return sdnsh.implement_show_config([data['first'],
                                                'diff',
                                                data['second'], ])
        return sdnsh.implement_show_config([])

    if obj_type == 'vns-flow':
        if 'detail' in data:
            return sdnsh.show_vns_flow_annotated([data['vns'],
                                                  'flow',
                                                  data['detail']])
        return sdnsh.show_vns_flow_annotated([data['vns'], 'flow'])

    if obj_type == 'tech-support':
        return sdnsh.do_show_tech_support([])

    if obj_type == 'config-file':
        if 'file' in data:
            return sdnsh.implement_show_config_file(['config-file', data['config']])
        return sdnsh.implement_show_config_file(['config-file', ])

    if obj_type == 'logging':
        if 'log-name' in data:
            return sdnsh.implement_show_logging([data['log-name']])
        return sdnsh.implement_show_logging([])

    if obj_type == 'event-history':
        if 'count' in data:
            return sdnsh.do_show_event_history([data['event'],
                                                'last',
                                                str(data['count'])])
        return sdnsh.do_show_event_history([data['event']])

    if obj_type == 'flow-cache':
        words = []
        if 'counters' in data:
            words.append('counters')
        elif 'application' in data:
            words.append('app')
            words.append(data['application'])
            words.append('app-instance')
            words.append(data['instance'])

        return sdnsh.do_show_flow_cache(words)

    if obj_type in ['controller-stats', 'switch-stats']:
        #
        # data['id'] is the name of the controller
        helper_item = obj_type.replace('-stats','')
        if helper_item == 'controller':
            helper_item = 'controller-node'
        key = mi.pk(helper_item)
        words = [helper_item, data[key], 'stats']
        if 'stats-type' in data:
            words.append(data['stats-type'])
        for (n,v) in data.items():
            if not n in [key, 'stats', 'stats-type']:
                words.append(n)
                words.append(v)
        return sdnsh.helper_show_object_stats(words)

    if obj_type == 'switch-tcpdump':
        words = ['trace', data['dpid']]
        for (n,v) in data.items():
            if not n in ['tcpdump', 'dpid']:
                words.append(n)
        return sdnsh.do_trace(words)

    if obj_type == 'copy':
        words = [data['source']]
        if 'dest' in data:
            words.append(data['dest'])
        return sdnsh.implement_copy(words)

    if obj_type == 'write':
        return sdnsh.implement_write([data['target']])

    if obj_type == 'this':
        obj_type = sdnsh.get_current_mode_obj_type()
        show_this = mi.obj_type_show_this(obj_type)
        if not show_this:
            return sdnsh.do_show_object(['this'])
        result = []
        for show in show_this:
            if type(show) is list and len(show) >= 3:
                # [ object, format, detail ]
                if len(result) > 0:
                    result.append(mi.obj_type_show_title(show[0]))
                sort = None
                if len(show) > 3:
                    sort = show[3]
                result.append(command_display_table(show[0], {},
                                                    table_format = show[1],
                                                    detail = show[2],
                                                    sort = sort,
                                                    scoped = True))
            elif type(show) is list and len(show) == 2:
                # [ object, detail ]
                if len(result) > 0:
                    result.append(mi.obj_type_show_title(show[0]))
                result.append(command_display_table(show[0], {}, detail = show[1], scoped = True))
            else:
                result.append(sdnsh.do_show_object([show]))
        return '\n'.join(result)

    if obj_type == 'version':
        return sdnsh.do_show_version([])

    if obj_type == 'reload':
        return sdnsh.implement_reload()

    if obj_type == 'test-command':
        if data['test-type'] == 'packet-in':
            return sdnsh.implement_test_packet_in(data)
        if data['test-type'] == 'path':
            return sdnsh.implement_test_path(data)

    print 'command_legacy_cli: obj-type unknown: ', obj_type


def command_legacy_cli_no(obj_type, data, detail = 'default', scoped = None, sort = None):
    """
    Implement no command for trampoline code back to the original code
    """
    if obj_type == 'tag-mapping':
        return sdnsh.implement_no_tag(['tag', data['tag']])


def command_version(data):
    """
    The version command will later manage changing the syntax to match
    the requested version.
    """
    new_version = data.get('version')
    if new_version == None:
        return

    version = new_version # save for error message
    new_version = sdnsh.desc_version_to_path_elem(new_version)

    # skip version change is this is the current version.
    if sdnsh.desc_version == new_version:
        return

    # see if the requested version exists
    if not sdnsh.command_packages_exists(new_version):
        print 'No command description group for version %s' % version
        return

    # run 'env [envriron_vars] ... cli.py'
    command = ['env']
    command.append('CLI_COMMAND_VERSION=%s' % version)
    command.append('CLI_STARTING_MODE=config')
    if os.path.exists('/opt/sdnplatform/cli/bin/cli'):
        # controller VM
        command.append('/opt/sdnplatform/cli/bin/cli --init')
    else:
        # developer setup
        base = os.path.dirname(__file__)
        command.append(os.path.join(base, 'cli.py'))
        command.append('--init')

    # dump the command descriptions, and read a new set.
    # open a subshell with a new command version
    subprocess.call(command, cwd=os.environ.get("HOME"))

    return


def command_clearterm():
    """
    Print reset characters to the screen to clear the console
    """
    subprocess.call("reset")

def command_display_cli(data):
    """
    Display various cli details
    (this may need to be re-factored into some general "internal" state show
    """
    debug = []
    if sdnsh.debug:
        debug.append('debug')
    if sdnsh.debug_backtrace:
        debug.append('backtrace')

    modes = sdnsh.command_dict.keys() + sdnsh.command_nested_dict.keys()

    entry = {
               'version' : ', '.join(command.command_syntax_version.keys()),
               'desc'    : ', '.join(sorted(command.command_added_modules.keys())),
               'format'  : ', '.join(sorted(sdnsh.pp.format_added_modules.keys())),
               'modes'   : ', '.join(sorted(utif.unique_list_from_list(modes))),
               'debug'   : ', '.join(debug),
            }
    basic = sdnsh.pp.format_entry(entry, 'cli')

    mode_entries = command.command_submode_dictionary(modes)
    mode_table = sdnsh.pp.format_table(mode_entries, 'cli-modes')

    return basic + '\n\nCommand Submode Transition\n' + mode_table

    return


def delete_alias_by_id(alias_obj_type, alias_value):
    """
    Common delete operation for alias, based on primary key

    @param alias_obj_type string, name of table where single entry is removed
    @param alias_value string, value of primary key to delete
    """
    xref = mi.foreign_key_xref.get(alias_obj_type)
    if xref:
        # look for any referecnes to this alias_value.  Since this
        # is an alias table, only the pk ought to exist in the xref.
        # When the alias is getting removed, any references to it
        # via foreign keys must also get removed.
        if len(xref) > 1 or not mi.pk(alias_obj_type) in xref:
            print 'Internal Inconsistency'
        else:
            for (fk_obj_type, fk_field) in xref[mi.pk(alias_obj_type)]:
                rows = sdnsh.get_table_from_store(fk_obj_type,
                                                  fk_field,
                                                  alias_value,
                                                  'exact')
                for row in rows:
                    sdnsh.rest_delete_object(fk_obj_type, row[mi.pk(fk_obj_type)])
    sdnsh.rest_delete_object(alias_obj_type, alias_value)


def delete_alias_by_fk(alias_obj_type, foreign_key):
    """
    Common delete operation for alias, by foreign key

    @param alias_obj_type string, name of table where single entry is removed
    @param alias_value string, value of primary key to delete
    """
    # find all the id's based on the foreign key, then delete them all.
    # note: see similar midw alias_lookup_with_foreign_key()

    foreign_field = mi.alias_obj_type_field(alias_obj_type)
    try:
        rows = sdnsh.get_table_from_store(alias_obj_type,
                                          foreign_field,
                                          foreign_key,
                                          "exact")
    except Exception, e:
        raise error.CommandInternalError("Can't fetch %s:%s" %
                                           (foreign_field, foreign_key))
    pk = mi.pk(alias_obj_type)
    for row in rows:
        delete_alias_by_id(alias_obj_type, row[pk])


def command_delete_alias(obj_type, data):
    """
    Action for delete-alias

    A single row is deleted from an alias table.
    Current alias tables include host-alias, switch-alias, port-alias

    @param obj_type string, name of alias table to manage
    @param data dict, collection of field:value's from command description
    """
    if sdnsh.description:   # description debugging
        print "command_delete_alias: ", obj_type, data

    parent_id = sdnsh.get_current_mode_obj()

    key = mi.pk(obj_type)
    if key not in data:
        delete_alias_by_fk(obj_type, parent_id)
    else:
        delete_alias_by_id(obj_type, data[key])


def command_create_alias(obj_type, data, reserved = None, fail_if_exists = False):
    """
    Action for create-alias

    Current alias tables include host-alias, switch-alias, port-alias

    @param obj_type string, name of alias table to manage
    @param data dict, collection of field:value's from the command description
    """
    if sdnsh.description:   # description debugging
        print "command_create_alias: ", obj_type, data, reserved, fail_if_exists

    parent_obj_type = sdnsh.get_current_mode_obj_type()
    parent_id = sdnsh.get_current_mode_obj()

    key = mi.pk(obj_type)
    if key not in data:
        raise error.CommandInternalError("Alias table '%s': description "
                                           "doesn't populate correct '%s' field as data" %
                                           (obj_type, key))
    alias_value = data[key]
    #
    # Determine if the alias name is allowed.
    if alias_value in sdnsh.reserved_words:
        raise error.ArgumentValidationError('reserved name "%s" in "%s"'
                                              % (alias_value, ','.join(sdnsh.reserved_words)))
    if reserved and type(reserved) != list:
        reserved = [reserved]

    if reserved and alias_value in reserved:
        raise error.ArgumentValidationError('reserved name "%s" in "%s"'
                                              % (alias_value, ','.join(reserved)))

    # Walk the foreign key's in the (alias) obj-type, looking
    # for the parent reference.

    alias_fk = None
    obj_type_foreign_keys = mi.obj_type_foreign_keys(obj_type)
    if len(obj_type_foreign_keys) == 1:
        alias_fk = obj_type_foreign_keys[0]
    else:
        for alias_fn in obj_type_foreign_keys:
            (fk_ot, fk_fn) = mi.foreign_key_references(obj_type, alias_fn)
            if fk_ot == parent_obj_type:
                alias_fk = alias_fn

    if not alias_fk:
        raise error.CommandInternalError("Alias table '%s' has no foreign key to '%s'" %
                                           (obj_type, parent_obj_type))

    try:
        sdnsh.get_object_from_store(obj_type, alias_value)
        if sdnsh.description:   # description debugging
            print "command_create_alias: delete ", obj_type, alias_value
        if fail_if_exists:
            raise error.ArgumentValidationError("Interface name '%s' already in use - cannot reassign" %(alias_value))
        delete_alias_by_id(obj_type, alias_value)
    except:
        pass

    # Remove other existing alias for the same foreign key
    # (ie: only one alias per each item, this could be relaxed)
    # XXX improve method of managing errors here
    try:
        rows = sdnsh.get_table_from_store(obj_type,
                                          alias_fk,
                                          parent_id,
                                          "exact")
    except Exception, e:
        errors = sdnsh.rest_error_to_dict(e)
        print sdnsh.rest_error_dict_to_message(errors)
        rows = []

    for row in rows:
        try:
            delete_alias_by_id(obj_type, row[key])
            if row[alias_fk] != parent_id:
                sdnsh.warning("Removed additional alias '%s'"
                              ", also refers to %s '%s'" %
                              (row[key], parent_obj_type, parent_id))
        except:
            if sdnsh.debug or sdnsh.debug_backtrace:
                traceback.print_exc()

    # This set's the foreign key to allow the create to succeed
    c_dict = {
                key      : alias_value,
                alias_fk : parent_id,
             }

    if sdnsh.description:   # description debugging
        print "command_create_alias: create ", obj_type, c_dict
    result = sdnsh.rest_create_object(obj_type, c_dict)
    check_rest_result(result)
    result = sdnsh.rest_query_objects(obj_type, c_dict)
    check_rest_result(result)

    return None


def command_create_tag(obj_type, data):
    """
    obj_type needs to be one of the objects which implements
    a relationship to 'tag', for example:  tag-mac-mapping
    """

    item = sdnsh.get_current_mode_obj_type()
    fks = mi.obj_type_foreign_keys(obj_type)
    for fk in fks:
        (fk_obj, fk_name) = mi.foreign_key_references(obj_type, fk)
        if fk_obj == item:
            break
    else:
        raise error.CommandSemanticError( "type mapping %s doesn't have "
                        "relationship to the current object %s" %
                        (obj_type, item))

    if sdnsh.description:   # description debugging
        print "command_create_tag: create ", obj_type, data

    tag_and_value = data['tag'].split('=')
    if len(tag_and_value) != 2:
        # deal with tag_and_value's 'va=vb=vc...'
        raise error.CommandSemanticError("tag <[tag-namespace.]name>=<value> " 
                                         ": associate tag with host")

    tag_parts = tag_and_value[0].split('.')
    if len(tag_parts) == 0:
        raise error.CommandSemanticError("tag <[tag-namespace.]name>"
                                         ": must have a name")
    elif len(tag_parts) == 1:
        tag_namespace = "default"
        tag_name = tag_parts[0]
    elif len(tag_parts) >= 2:
        # the tag_name is not allowed to have '.' 
        # collect all the '.'s together into the namespace
        tag_namespace = '.'.join(tag_parts[:-1])
        tag_name = tag_parts[-1]

    tag_value = tag_and_value[1]

    # first manage the tag ... 
    tag_dict = { 
               'namespace' : tag_namespace,
               'name'      : tag_name,
               'value'     : tag_value,
               }   

    query = sdnsh.rest_query_objects('tag', tag_dict)
    sdnsh.check_rest_result(query)
    tag_dict['persist'] = True
    if len(query) == 0:
        result = sdnsh.rest_create_object('tag', tag_dict)
        sdnsh.check_rest_result(result)
    elif len(query) == 1:
        update = sdnsh.rest_update_object('tag',
                                          mi.pk('tag'),
                                          query[0][mi.pk('tag')],
                                          tag_dict)
        sdnsh.check_rest_result(update)

    del tag_dict['persist']
    query = sdnsh.rest_query_objects('tag', tag_dict)
    sdnsh.check_rest_result(query)
    tag_id = query[0][mi.pk('tag')]

    # now create the tag-mapping
    tag_dict = { 
               fk    : sdnsh.get_current_mode_obj(), # fk from early for loop
               'tag' : tag_id,
               }   

    query = sdnsh.rest_query_objects(obj_type, tag_dict)
    sdnsh.check_rest_result(query)
    if len(query) == 0:
        result = sdnsh.rest_create_object(obj_type, tag_dict)
        sdnsh.check_rest_result(result)


def command_delete_tag(obj_type, data):
    """
    obj_type describes the tag-XXX-mapping which is getting
    managed, data has the tag 'string' to delete.
    """
    item = sdnsh.get_current_mode_obj_type()
    fks = mi.obj_type_foreign_keys(obj_type)
    for fk in fks:
        (fk_obj, fk_name) = mi.foreign_key_references(obj_type, fk)
        if fk_obj == item:
            break
    else:
        raise error.CommandSemanticError( "type mapping %s doesn't have "
                        "relationship to the current object %s" %
                        (obj_type, item))

    if 'tag' not in data:
        raise error.CommandSemanticError('Tag value missing')

    tag = data['tag']
    name_and_value = tag.split('=')

    name_part = name_and_value[0].split('.')
    if len(name_part) == 1:
        namespace = 'default'
        name = name_part[0]
    elif len(name_part) >= 2:
        namespace = '.'.join(name_part[:-1])
        name = name_part[-1]

    value = name_and_value[1]
    pk_value = sdnsh.unique_key_from_non_unique([namespace,
                                                 name,
                                                 value,
                                                 sdnsh.get_current_mode_obj()])
    try:
        sdnsh.get_object_from_store(obj_type, pk_value)
    except Exception:
        raise error.CommandSemanticError('%s No such tag %s' % (obj_type, tag))

    sdnsh.rest_delete_object(obj_type, pk_value)

    # with that entry removed, check to see if any other
    # foreign keys assocaited with class Tag exist.

    fk_value = sdnsh.unique_key_from_non_unique([namespace,
                                                 name,
                                                 value])

    for tag_fields in mi.foreign_key_xref['tag']:
        for (fk_obj_type, fk_name) in mi.foreign_key_xref['tag'][tag_fields]:
            try:
                sdnsh.get_table_from_store(fk_obj_type, fk_name, fk_value)
                break
            except Exception, e:
                pass
        else:
            continue
        break
    else:
        try:
            sdnsh.rest_delete_object('tag', fk_value)
        except Exception, e:
            raise error.CommandSemanticError('base tag missing' % fk_value)


def command_rest_post_data(path, data=None, verb='PUT'):
    """
    """
    url = 'http://%s/rest/v1/%s' % (sdnsh.controller, path)
    result = sdnsh.rest_post_request(url, data, verb)
    check_rest_result(result)
    return None


def command_cli_variables_set(variable, value, data):
    global sdnsh

    if variable == 'debug':
        print '***** %s cli debug *****' % \
                ('Enabled' if value else 'Disabled')
        sdnsh.debug = value
    elif variable == 'cli-backtrace':
        print '***** %s cli debug backtrace *****' % \
                ('Enabled' if value else 'Disabled')
        sdnsh.debug_backtrace = value
    elif variable == 'cli-batch':
        print '***** %s cli batch mode *****' % \
                ('Enabled' if value else 'Disabled')
        sdnsh.batch = value
    elif variable == 'description':
        print '***** %s command description mode *****' % \
                ('Enabled' if value else 'Disabled')
        sdnsh.description = value
    elif variable == 'rest':
        if 'record' in data and value:
            print '***** Eanbled rest record mode %s *****' % \
                (data['record'])
            url_cache.record(data['record'])
            return
        print '***** %s display rest mode *****' % \
                ('Enabled' if value else 'Disabled')
        if 'detail' in data and data['detail'] == 'details':
            if value == True:
                sdnsh.disply_rest_detail = value
                sdnsh.store.display_reply_mode(value)
        sdnsh.display_rest = value
        sdnsh.store.display_mode(value)
        if value == False:
            sdnsh.disply_rest_detail = value
            sdnsh.store.display_reply_mode(value)
            url_cache.record(None)
    elif variable == 'set':
        if 'length' in data:
            sdnsh.length = utif.try_int(data['length'])


def command_cli_set(variable, data):
    command_cli_variables_set(variable, True, data)

def command_cli_unset(variable, data):
    command_cli_variables_set(variable, False, data)


def command_shell_command(script):

    def shell(args):
        subprocess.call(["env", "SHELL=/bin/bash", "/bin/bash"] + list(args),
                        cwd=os.environ.get("HOME"))
        print

    print "\n***** Warning: this is a debug command - use caution! *****"
    if script == 'bash':
        print '***** Type "exit" or Ctrl-D to return to the CLI *****\n'
        shell(["-l", "-i"])
    elif script == 'python':
        print '***** Type "exit()" or Ctrl-D to return to the CLI *****\n'
        shell(["-l", "-c", "python"])
    elif script == 'cassandra-cli':
        print '***** Type "exit" or Ctrl-D to return to the CLI *****\n'
        shell(["-l", "-c", "/opt/sdnplatform/db/bin/cassandra-cli --host localhost"])
    elif script == 'netconfig':
        if not re.match("/dev/ttyS?[\d]+$", os.ttyname(0)):
            print '***** You seem to be connected via SSH or another remote protocol;'
            print '***** reconfiguring the network interface may disrupt the connection!'
        print '\n(Press Control-C now to leave the network configuration unchanged)\n'
        subprocess.call(["sudo",
                         "env",
                         "SHELL=/bin/bash",
                         "/opt/sdnplatform/sys/bin/bscnetconfig",
                         "eth0"],
                         cwd=os.environ.get("HOME"))
    else:
        # XXX possibly run the script directly?
        print "Unknown debug choice %s" % script


def command_prompt_update():
    """
    Action to recompute the prompt, used when there's some possibility
    the prompt has changes after some other action (hostname update, for example)
    """
    sdnsh.set_controller_for_prompt()
    sdnsh.update_prompt()

def command_controller_decommission(data):
    """
    Decommission the controller using the REST API
    """
    id = data.get('id')
    confirm_request("Decommission controller '%s'?\n(yes to continue) " % id)

    while True:
        url = 'http://%s/rest/v1/system/ha/decommission' % (sdnsh.controller)
        result = sdnsh.rest_post_request(url, {"id": id}, 'PUT')
        status = json.loads(result)

        if (status['status'] == 'OK') and status['description'].endswith('is already decommissioned') == True:
            print 'Decommission finished'
            print
            break
        else:
            print 'Decommission in progress'
            
        time.sleep(10)

def command_controller_upgrade(data = None):
    """
    Upgrade the controller using the REST API
    """

    force = 'force' in data
    details = 'details' in data

    if force:
        print "WARNING: Ignoring any validation errors during upgrade"
    url = "http://%s/rest/v1/system/upgrade/image-name" % sdnsh.controller
    result = sdnsh.store.rest_simple_request(url)
    check_rest_result(result)
    iname = json.loads(result)
    if (iname['file'] is None or iname['file'] == ""):
        print "Error: No upgrade image present."
        print ""
        print """To perform upgrade, an upgrade image package needs to be uploaded (with scp) to the controller's \"images\" user."""
        print """Upgrade image package is a file with name of format \"upgrade-YYYY.MM.DD.XXXX.pkg\"."""
        print ""
        print "Following is an example to prepare upgrade for controller with IP address 192.168.67.141:"
        print "scp $path/upgrade-2013.02.13.0921.pkg images@192.168.67.141:"
        print ""
        return

    confirm_request("Upgrade controller from image '%s'?\n(yes to continue) "
                    % iname['file'])
    
    url = "http://%s/rest/v1/system/upgrade/extract-image-manifest" % sdnsh.controller
    result = sdnsh.store.rest_simple_request(url)
    check_rest_result(result)
    manifest = json.loads(result)

    print "Executing upgrade..."
    for step in manifest:
        print "%s - %s" % (step['step'], step['description'])
        url = 'http://%s/rest/v1/system/upgrade/execute-upgrade-step' % \
            (sdnsh.controller)
        result = sdnsh.rest_post_request(url, {"step": step['step'],
                                               "imageName": iname['file'],
                                               "force": force}, 
                                         'PUT')
        check_rest_result(result)
        status = json.loads(result)
        
        if (status['status'] == "OK"):
            print "  Succeeded"
            if details:
                print "\nDetailed output:"
                print status['description']
                print
        else:
            print "  Failed to execute upgrade step %d" % step['step']
            print "\nDetailed output:"
            print status['description']
            print
            return

    print """Controller node upgrade complete.
Upgrade will not take effect until system is rebooted. Use 'reload' to
reboot this controller node. To revert, select the appropriate image 
from the boot menu"""

def command_cluster_config_rollback(data):
    path = ''
    if data.get('dir') == 'images://':
        path += '/home/images/'
    elif data.get('dir') == 'saved-configs://':
        path += '/opt/sdnplatform/run/saved-configs/'
    path += data.get('file')

    url = "http://%s/rest/v1/system/ha/role" % sdnsh.controller
    result = sdnsh.store.rest_simple_request(url, use_cache = False)
    ha_role = json.loads(result)
    if ha_role['role'] != 'MASTER':
        print "Command can only be run on Master"
        return
   
    command_legacy_cli('copy', {'dest': 'file://running-config-copy', 'source': 'running-config'})
    print "INFO: Checking config '%s'" % path
    url = "http://%s/rest/v1/system/rollback/diffconfig" % sdnsh.controller
    result = sdnsh.rest_post_request(url, {"config-1": "/opt/sdnplatform/run/saved-configs/running-config-copy", "config-2": path}, 'PUT')
    check_rest_result(result)
    if json.loads(result)['out'].startswith('Found differences'):
        print json.loads(result)['out']
        print "Rollback aborted"
        return
        
    url = "http://%s/rest/v1/system/controller" % sdnsh.controller
    result = sdnsh.store.rest_simple_request(url, use_cache = False)
    controller_id = json.loads(result)['id']

    url = "http://%s/rest/v1/model/controller-interface?controller=%s" % (sdnsh.controller, controller_id)
    result = sdnsh.store.rest_simple_request(url)
    local_iface = json.loads(result)[0]['discovered-ip']
    
    url = "http://%s/rest/v1/model/controller-interface" % sdnsh.controller
    result = sdnsh.store.rest_simple_request(url)
    check_rest_result(result)
    ifaces = json.loads(result)

    nodeCount = len(ifaces)
    cutover = nodeCount/2
    if nodeCount%2 == 1:
        cutover = cutover + 1
    
    rollbackedNodes = []

    # remove and add object for local node at the end of the list
    for index, iface in enumerate(ifaces):
        if iface['discovered-ip'] == local_iface:
            break
    del ifaces[index]
    ifaces.append(iface)

    config=open(path, 'r').read()
    url = 'http://%s/rest/v1/system/upload-data' % ifaces[0]['discovered-ip']
    result = sdnsh.rest_post_request(url, {"data": config, "dst" : "/tmp/rollback.conf"}, 'PUT')
    check_rest_result(result)
    
    while len(ifaces) > 0:
        if sdnsh.batch == False:
            while True:
                confirm = raw_input("Rollback controller at '%s'. [yes/no] ?" % ifaces[0]['discovered-ip'])
                if confirm.lower() == 'n' or confirm.lower() == 'no':
                    if len(rollbackedNodes) == 0:
                        print "INFO: Rollback aborted"
                        return
                        
                    print "INFO: Undoing Rollback on previously rollbacked nodes"
                    for node in rollbackedNodes:
                        print "INFO: Resetting database on '%s'" % node['discovered-ip']
                        url = 'http://%s/rest/v1/system/resetbsc' % (node['discovered-ip'])
                        result = sdnsh.rest_post_request(url, {}, 'PUT')
                        check_rest_result(result)
                        print "INFO: Rebooting '%s'" % node['discovered-ip']
                        url = 'http://%s/rest/v1/system/reload' % (node['discovered-ip'])
                        result = sdnsh.rest_post_request(url, {}, 'GET')
                        check_rest_result(result)
                    
                    if len(rollbackedNodes) >= cutover:
                        # delete the REJECT rules
                        url="http://localhost/rest/v1/model/firewall-rule?port=6633"
                        result = sdnsh.rest_post_request(url, {}, 'DELETE')
                        # enable allow openflow on all controllers not rollbacked.
                        url="http://localhost/rest/v1/model/firewall-rule"
                        for iface in ifaces:
                            pk_id = '%s|Ethernet|0' % iface['controller']
                            data = {
                                'action': 'allow',
                                'interface': pk_id,
                                'src-ip': '',
                                'port': '6633',
                                'proto': 'tcp',
                                'vrrp-ip': '',
                            }
                            print "INFO: re-allow openflow on %s" % iface['discovered-ip']
                            result = sdnsh.rest_post_request(url, data, 'PUT')
                            check_rest_result(result)
                    
                    print "Rollback aborted"
                    return
                elif confirm.lower() == 'y' or confirm.lower() == 'yes':
                    break
        
        url = 'http://%s/rest/v1/system/rollback/config' % (ifaces[0]['discovered-ip'])
        result = sdnsh.rest_post_request(url, {"path": "/tmp/rollback.conf"}, 'PUT')
        check_rest_result(result)
        time.sleep(10)
       
        print "INFO: Rebooting ", ifaces[0]['discovered-ip']
        url = "http://%s/rest/v1/system/reload" % ifaces[0]['discovered-ip']
        result = sdnsh.store.rest_simple_request(url)
        
        if ifaces[0]['discovered-ip'] == local_iface:
            break
        
        print "INFO: Waiting for %s to come back up" % ifaces[0]['discovered-ip']
        url = "http://%s/rest/v1/system/ha/role" % ifaces[0]['discovered-ip']
        while True:
            time.sleep(30)
            try:
                result = sdnsh.store.rest_simple_request(url, use_cache = False)
                status = json.loads(result)
                if status['role'] == 'SLAVE' or status['role'] == 'MASTER':
                    print "INFO: Rollback complete on '%s'" % ifaces[0]['discovered-ip']
                    break
                print "INFO: Waiting for 30 seconds" 
            except:
                print "INFO: Waiting for 30 seconds" 
                
         
        iface = ifaces.pop(0)
        rollbackedNodes.append(iface)

    print "Rollback completed"

def command_wait_for_controller(delay = None, sdnplatform_check = False,
                                within_command = False):
    """
    For various commands, it makes sense for the command to verify that
    the controller restart has been completed.   In the situation where
    a single controller is configured, it also makes sense to verify the
    controller is now configured as MASTER.

    This is especially true for command which are known to cause the
    controller to restart, for exampe the 'feature' command.

    The procedure is also used during CLI startup (see cli.py)
    to verify that the controller is in MASTER mode.  Its normal
    for the HA role to transition from SLAVE to master during 
    system startup.
    """

    # if the CLI was started with --init, skip the wait, the
    # controller isn't running.
    if sdnsh.options.init:
        return
    
    def is_ready(sdnsh, verbose, duration):
        """
        Be loud-as-_ean when the duration is greater then 15 seconds.
        Display the gory details for all to know.
        """
        too_long = 90
        try:
            url = "http://%s/rest/v1/system/ha/role" % sdnsh.controller
            result = sdnsh.store.rest_simple_request(url, use_cache = False)
            ha_role = json.loads(result)
            if duration > too_long:
                print 'Long delay: reason', \
                    ', '.join(['%s: %s' % (n,v) for (n,v) in ha_role.items()
                               if v != ''])
            if (ha_role['role'] == 'MASTER' or
                sdnsh.find_master()['master'] is not None):
                if verbose:
                    print 'Current role is MASTER'
                return True
            return False
        except error.CommandRestError,e:
            print "REST error whileUnable to determine controller HA role."
            errors = self.rest_error_to_dict(e, obj_type)
            print self.rest_error_dict_to_message(errors)
            return True
        except Exception, e:
            if duration > too_long:
                print 'MASTER Transition Failure: ', e
                traceback.print_exc()
                return True
            return False

    # if this isn't a typical environment (ie: running remotely)
    # don't bother trying to determine the role
    if not os.path.exists('/opt/sdnplatform/current_role'):
        return
    
    # now vadalidate the rest api port is working 
    ip_and_port = sdnsh.controller.split(':')
    if len(ip_and_port) == 2:
        # first ensure the REST API is answering
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        try:
            s.connect((ip_and_port[0], int(ip_and_port[1])))
            s.close()
        except Exception, e:
            print 'REST API not running, emergency CLI access'
            if sdnsh.debug: # enable debug to see messages
                print 'Exception:', e
            return

    # issue a REST API request directed at the model.
    try:
        entry = sdnsh.get_table_from_store('feature')
    except Exception, e:
        print 'REST API/Database not responding, emergency CLI access'
        if sdnsh.debug: # enable debug to see messages
            print 'Exception:', e
        return 
    
    if sdnplatform_check:
        # the REST API request for ha-role will return UNAVAILABLE
        # when sdnplatform isn't running.
        url = "http://%s/rest/v1/system/ha/role" % sdnsh.controller
        result = sdnsh.store.rest_simple_request(url, use_cache = False)
        ha_role = json.loads(result)
        if ha_role['role'] == 'UNAVAILABLE':
            print 'REST API/SDN platform not responding, emergency CLI access'
            return 


    if delay == None:
        delay = 1
    delay_str = 'a sec' if delay == 1 else '%d seconds' % delay

    duration = 0
    while True:
        try:
            verbose = False
            while not is_ready(sdnsh, verbose, duration):
                if within_command:
                    print 'Waiting %s to complete command execution, ' \
                          'Hit Ctrl-C to exit early' % delay_str
                    verbose = False
                else:
                    print 'Waiting %s while current role is SLAVE mode, ' \
                          'Hit Ctrl-C to exit early' % delay_str
                    verbose = True
                time.sleep(delay)
                duration += delay
            return
        except:
            if is_ready(sdnsh, True, duration):
                if duration > 15:
                    print 'MASTER Transition: %s sec' % duration
                return
            try:
                resp = raw_input('Controller is not yet ready.'
                                 'Do you still want to continue to the CLI? [n]')
                if resp and "yes".startswith(resp.lower()):
                    print 'Continuing with CLI despite initialization error ...'
                    return
            except KeyboardInterrupt:
                return


def command_factory_default():
    print "Re-setting controller to factory defaults ..."
    os.system("sudo /opt/sdnplatform/sys/bin/resetbsc")
    return


def command_dump_log(data):
    controller = data.get('controller-node') # can be None.
    controller_dict = { 'id' : controller }
    for ip_port in controller_ip_and_port(controller_dict):
        log_name = data['log-name']
        if log_name == 'all':
            url = log_url(ip_and_port = ip_port)
            log_names = command.sdnsh.rest_simple_request_to_dict(url)
            for log in log_names:
                yield '*' * 40 + ip_port + ' ' + log['log'] + '\n'
                for item in command_dump_log({ 'log-name' : log['log'] }):
                    yield item
            return

        # use a streaming method so the complete log is not in memory
        url = log_url(ip_and_port = ip_port, log = log_name)
        request = urllib2.urlopen(url)
        for line in request:
            yield line
        request.close()


#
# Initialize action functions 
#
#

def init_actions(bs, modi):
    global sdnsh, mi
    sdnsh = bs
    mi = modi

    command.add_action('create-tunnel',
                       tunnel_create,
                       {'kwargs': {'data' : '$data',}})

    command.add_action('remove-tunnel',
                       tunnel_remove,
                       {'kwargs': {'data' : '$data',}})

    command.add_action('create-policy',
                       policy_create,
                       {'kwargs': {'data' : '$data',}})
    
    command.add_action('remove-policy',
                       policy_remove,
                       {'kwargs': {'data' : '$data',}})
    
    command.add_action('write-fields', write_fields,
                       {'kwargs': {'obj_type': '$current-mode-obj-type',
                                   'obj_id': '$current-mode-obj-id',
                                   'data': '$data'}})

    command.add_action('reset-fields', reset_fields,
                       {'kwargs': {'obj_type'     : '$current-mode-obj-type',
                                   'obj_id'       : '$current-mode-obj-id',
                                   'arg_data'     : '$data',
                                   'match_for_no' : '$match-for-no',
                                   'fields'       : '$fields'}})

    command.add_action('write-fields-explicit', write_fields,
                       {'kwargs': {'obj_type' : '$obj-type',
                                   'obj_id'   : '$obj-id',
                                   'data'     : '$data'}})

    command.add_action('reset-fields-explicit', reset_fields,
                       {'kwargs': {'obj_type'     : '$obj-type',
                                   'obj_id'       : '$obj-id',
                                   'arg_data'     : '$data',
                                   'match_for_no' : '$match-for-no',
                                   'fields'       : '$fields'}})

    command.add_action('update-config', update_config,
                       {'kwargs': {'obj_type'      : '$obj-type',
                                   'obj_id'        : '$current-mode-obj-id',
                                   'data'          : '$data',
                                   'no_command'    : '$is-no-command', }})

    command.add_action('delete-objects', delete_objects,
                       {'kwargs': {'obj_type': '$obj-type',
                                   'data': '$data',
                                   'parent_field': '$parent-field',
                                   'parent_id': '$current-mode-obj-id'}})

    command.add_action('write-object', write_object,
                       {'kwargs': {'obj_type': '$obj-type',
                                   'data': '$data',
                                   'parent_field': '$parent-field',
                                   'parent_id': '$current-mode-obj-id'}})
        
    command.add_action('set-data', set_data,
                       {'kwargs': {'data': '$data',
                                   'key': '$key',
                                   'value': '$value'}})

    command.add_action('push-mode-stack', push_mode_stack,
                       {'kwargs': {'mode_name': '$submode-name',
                                   'obj_type': '$obj-type',
                                   'parent_field': '$parent-field',
                                   'parent_id': '$current-mode-obj-id',
                                   'data': '$data',
                                   'create': '$create'}})

    command.add_action('pop-mode-stack', pop_mode_stack)

    command.add_action('confirm', confirm_request,
                        {'kwargs': {'prompt': '$prompt'}})

    command.add_action('convert-vns-access-list', convert_vns_access_list,
                        {'kwargs': {'obj_type': '$obj-type',
                                    'key' : '$current-mode-obj-id',
                                    'data' : '$data'}})
    command.add_action('display-table', command_display_table,
                        {'kwargs': {'obj_type'      : '$obj-type',
                                    'data'          : '$data',
                                    'table_format'  : '$format',
                                    'title'         : '$title',
                                    'detail'        : '$detail',
                                    'scoped'        : '$scoped',
                                    'sort'          : '$sort',
                                   }})

    command.add_action('display-rest', command_display_rest,
                        {'kwargs': { 'data'          : '$data',
                                     'url'           : '$url',
                                     'path'          : '$path',
                                     'rest_type'     : '$rest-type',
                                     'sort'          : '$sort',
                                     'title'         : '$title',
                                     'table_format'  : '$format',
                                     'detail'        : '$detail',
                                   }})

    command.add_action('query-table', command_query_table,
                        {'kwargs': {'obj_type'      : '$obj-type',
                                    'data'          : '$data',
                                    'key'           : '$key',
                                    'scoped'        : '$scoped',
                                    'sort'          : '$sort',
                                    'crack'         : '$crack',
                                    'append'        : '$append',
                                    'clear'         : True,
                                   }})

    command.add_action('query-table-append', command_query_table,
                        {'kwargs': {'obj_type'      : '$obj-type',
                                    'data'          : '$data',
                                    'key'           : '$key',
                                    'scoped'        : '$scoped',
                                    'sort'          : '$sort',
                                    'crack'         : '$crack',
                                    'append'        : '$append',
                                    'clear'         : False,
                                   }})


    command.add_action('query-rest', command_query_rest,
                        {'kwargs': {'url'           : '$url',
                                    'path'          : '$path',
                                    'rest_type'     : '$rest-type',
                                    'data'          : '$data',
                                    'key'           : '$key',
                                    'scoped'        : '$scoped',
                                    'sort'          : '$sort',
                                    'append'        : '$append',
                                    'clear'         : True,
                                   }})

    command.add_action('query-rest-append', command_query_rest,
                        {'kwargs': {'url'           : '$url',
                                    'path'          : '$path',
                                    'rest_type'     : '$rest-type',
                                    'data'          : '$data',
                                    'key'           : '$key',
                                    'scoped'        : '$scoped',
                                    'sort'          : '$sort',
                                    'crack'         : '$crack',
                                    'append'        : '$append',
                                    'clear'         : False,
                                   }})

    command.add_action('join-rest', command_join_rest,
                        {'kwargs': {'url'           : '$url',
                                    'key'           : '$key',
                                    'join_field'    : '$join-field',
                                    'rest_type'     : '$rest-type',
                                    'add_field'     : '$add-field',
                                    'data'          : '$data',
                                    'crack'         : '$crack',
                                    'url_key'       : '$url-key',
                                   }})

    command.add_action('join-table', command_join_table,
                        {'kwargs': {'obj_type'      : '$obj-type',
                                    'data'          : '$data',
                                    'key'           : '$key',
                                    'key_value'     : '$key-value',
                                    'add_field'     : '$add-field',
                                    'join_field'    : '$join-field',
                                    'crack'         : '$crack',
                                   }})

    command.add_action('crack', command_crack,
                        {'kwargs': {
                                    'field'         : '$field',
                                   }})

    command.add_action('display', command_display,
                        {'kwargs': {'data'         : '$data',
                                    'table_format' : '$format',
                                    'sort'         : '$sort',
                                    'detail'       : '$detail',
                                    'title'        : '$title',
                                   }})

    command.add_action('legacy-cli', command_legacy_cli,
                        {'kwargs': {'obj_type' : '$obj-type',
                                    'data'     : '$data',
                                    'detail'   : '$detail',
                                    'sort'     : '$sort',
                                    'scoped'   : '$scoped',
                                   }})

    command.add_action('legacy-cli-no', command_legacy_cli_no,
                        {'kwargs': {'obj_type' : '$obj-type',
                                    'data'     : '$data',
                                    'detail'   : '$detail',
                                    'sort'     : '$sort',
                                    'scoped'   : '$scoped',
                                   }})

    command.add_action('version',  command_version,
                        {'kwargs': {'data' : '$data',
                                   }})

    command.add_action('clearterm',  command_clearterm)

    command.add_action('display-cli',  command_display_cli,
                        {'kwargs': {'data'   : '$data',
                                    'detail' : '$detail',
                                   }})

    command.add_action('create-alias', command_create_alias,
                        {'kwargs': {'obj_type' : '$obj-type',
                                    'data'     : '$data',
                                    'reserved' : '$reserved',
                                    'fail_if_exists' : '$fail-if-exists',
                                   }})

    command.add_action('delete-alias', command_delete_alias,
                        {'kwargs': {'obj_type' : '$obj-type',
                                    'data'   : '$data',
                                   }})

    command.add_action('create-tag', command_create_tag,
                        {'kwargs': {'obj_type' : '$obj-type',
                                    'data'   : '$data',
                                   }})

    command.add_action('delete-tag', command_delete_tag,
                        {'kwargs': {'obj_type' : '$obj-type',
                                    'data'   : '$data',
                                   }})
 
    command.add_action('cli-set', command_cli_set,
                        {'kwargs': {'variable' : '$variable',
                                    'data'     : '$data',
                                   }})

    command.add_action('cli-unset', command_cli_unset,
                        {'kwargs': {'variable' : '$variable',
                                    'data'     : '$data',
                                   }})

    command.add_action('shell-command', command_shell_command,
                        {'kwargs': {'script' : '$command',
                                   }})
    
    command.add_action('rest-post-data', command_rest_post_data,
                        {'kwargs': {'path': '$path',
                                    'data': '$data',
                                    'verb': '$verb'
                                    }})

    command.add_action('prompt-update', command_prompt_update,)

    command.add_action('controller-upgrade', command_controller_upgrade,
                       {'kwargs': {'data': '$data'}})
    
    command.add_action('controller-config-rollback', command_cluster_config_rollback,
                       {'kwargs': {'data': '$data'}})
    
    command.add_action('controller-decommission', command_controller_decommission,
                       {'kwargs': {'data': '$data'}})

    command.add_action('wait-for-controller', command_wait_for_controller,
                       {'kwargs': {'within_command': True}})

    command.add_action('factory-default', command_factory_default)

    command.add_action('dump-log', command_dump_log,
                        {'kwargs' : { 'data' : '$data', }})
