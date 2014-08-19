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

from django.core.validators import validate_ipv4_address, MinValueValidator
from django.db import models
from django.forms import ValidationError
from django.test import TestCase, Client
from django.utils import simplejson
import copy
import urllib
import urllib2
from sdncon.rest.validators import validate_mac_address, RangeValidator, ChoicesValidator
from sdncon.rest.config import add_config_handler, reset_last_config_state, model_instances_equal

# These are the test models used in the unit tests
# When running the unit tests, syncdb looks for model definitions
# in this file (i.e. tests.py), so these will be in the complete
# list of models used by the REST code

class RestTestModel(models.Model):
    name = models.CharField(max_length=32, primary_key=True)
    enabled = models.BooleanField()
    min = models.IntegerField(validators=[MinValueValidator(100)])
    range = models.IntegerField(validators=[RangeValidator(10,100)])
    internal = models.CharField(max_length=24,default='foo')
    ip = models.IPAddressField(validators=[validate_ipv4_address])
    mac = models.CharField(max_length=20, validators=[validate_mac_address])
    ratio = models.FloatField()
    COLOR_CHOICES = (
        ('red', 'Dark Red'),
        ('blue', 'Navy Blue'),
        ('green', 'Green'),
        ('white', 'Ivory White')
    )
    color = models.CharField(max_length=20, validators=[ChoicesValidator(COLOR_CHOICES)])
    
    def clean(self):
        # This is just a dummy check for testing purposes. Typically you'd only
        # use the model-level clean function for validating things across multiple fields
        if self.range == 50:
            raise ValidationError("Dummy model-level validation failure.")
    
    class Rest:
        NAME = 'rest-test'
        FIELD_INFO = ({'name':'internal', 'private': True},)

class RestTestTagModel(models.Model):
    rest_test = models.ForeignKey(RestTestModel)
    name = models.CharField(max_length=32)
    value = models.CharField(max_length=256)
    
    class Rest:
        NAME = 'rest-test-tag'
        FIELD_INFO = ({'name':'rest_test', 'rest_name':'rest-test'},)
        
class RestTestRenamedFieldModel(models.Model):
    test = models.CharField(max_length=16,primary_key=True)
    
    class Rest:
        NAME = 'rest-test-renamed'
        FIELD_INFO = ({'name':'test', 'rest_name':'renamed'},)
        
class RestDisabledModel(models.Model):
    dummy = models.CharField(max_length=32)

class RestCompoundKeyModel(models.Model):
    name = models.CharField(max_length=32)
    index = models.IntegerField()
    extra = models.CharField(max_length=32, default='dummy')
    
    class CassandraSettings:
        COMPOUND_KEY_FIELDS = ['name', 'index']
        
    class Rest:
        NAME = 'rest-compound-key'
        
#############################################################################
# These are some client-side utility functions for getting, putting, deleting
# data using the REST API
#############################################################################

def construct_rest_url(path, query_params=None):
    url = 'http://localhost:8000/rest/v1/%s/' % path
    if query_params:
        url += '?'
        url += urllib.urlencode(query_params)
    return url

def get_rest_data(type, query_param_dict=None):
    url = construct_rest_url(type, query_param_dict)
    request = urllib2.Request(url)
    response = urllib2.urlopen(request)
    response_text = response.read()
    obj = simplejson.loads(response_text)
    return obj

def put_rest_data(obj, type, query_param_dict=None):
    url = construct_rest_url(type, query_param_dict)
    post_data = simplejson.dumps(obj)
    request = urllib2.Request(url, post_data, {'Content-Type':'application/json'})
    request.get_method = lambda: 'PUT'
    response = urllib2.urlopen(request)
    return response.read()

def delete_rest_data(type, query_param_dict=None):
    url = construct_rest_url(type, query_param_dict)
    request = urllib2.Request(url)
    request.get_method = lambda: 'DELETE'
    response = urllib2.urlopen(request)
    return response.read()

def test_construct_rest_url(path, query_params=None):
    url = '/rest/v1/%s/' % path
    if query_params:
        url += '?'
        url += urllib.urlencode(query_params)
    return url

def test_construct_rest_model_url(path, query_params=None):
    return test_construct_rest_url('model/' + path, query_params)
    
def test_get_rest_model_data(type, query_params=None):
    url = test_construct_rest_model_url(type,query_params)
    c = Client()
    response = c.get(url)
    return response

def test_put_rest_model_data(obj, type, query_params=None):
    url = test_construct_rest_model_url(type, query_params)
    data = simplejson.dumps(obj)
    c = Client()
    response = c.put(url , data, 'application/json')
    return response

def test_delete_rest_model_data(type, query_params=None):
    url = test_construct_rest_model_url(type, query_params)
    c = Client()
    response = c.delete(url)
    return response

def get_sorted_list(data_list, key_name, create_copy=False):
    if create_copy:
        data_list = copy.deepcopy(data_list)
    if key_name:
        key_func = lambda item: item.get(key_name)
    else:
        key_func = lambda item: item[0]
    data_list.sort(key = key_func)
    return data_list

#############################################################################
# The actual unit test classes
#############################################################################

class BasicFunctionalityTest(TestCase):
    
    REST_TEST_DATA = [
        {'name':'foobar','enabled':True,'min':100,'range':30,'ip':'192.168.1.1','mac':'00:01:22:34:56:af','ratio':4.56,'color':'red'},
        {'name':'sdnplatform','enabled':False,'min':400,'range':45,'ip':'192.168.1.2','mac':'00:01:22:CC:56:DD','ratio':8.76,'color':'green'},
        {'name':'foobar2','enabled':True,'min':1000,'range':100,'ip':'192.168.1.3','mac':'00:01:22:34:56:af','ratio':1,'color':'white'},
    ]
    
    REST_TEST_TAG_DATA = [
        {'id':'1','rest-test':'foobar','name':'one','value':'testit'},
        {'id':'2','rest-test':'foobar','name':'two','value':'test2'},
        {'id':'3','rest-test':'sdnplatform','name':'three','value':'three'},
    ]
    
    def setUp(self):
        # Create the RestTestModel instances
        rest_test_list = []
        for rtd in self.REST_TEST_DATA:
            rt = RestTestModel(**rtd)
            rt.save()
            rest_test_list.append(rt)
        
        # Create the RestTestTagModel instances
        for rttd in self.REST_TEST_TAG_DATA:
            rttd_init = rttd.copy()
            for rt in rest_test_list:
                if rt.name == rttd['rest-test']:
                    del rttd_init['rest-test']
                    rttd_init['rest_test'] = rt
                    break
            else:
                raise Exception('Invalid initialization data for REST unit tests')
            rtt = RestTestTagModel(**rttd_init)
            rtt.save()
        
        rtrf = RestTestRenamedFieldModel(test='test')
        rtrf.save()
        
        # Create the RestDisabledModel instance. We're only going to test that
        # we can't access this model via the REST API, so we don't need to keep
        # track of the data used for the instance
        #rdm = RestDisabledModel(dummy='dummy')
        #rdm.save()
    
    def check_rest_test_data_result(self, data_list, expected_data_list, message=None):
        data_list = get_sorted_list(data_list, 'name', True)
        expected_data_list = get_sorted_list(expected_data_list, 'name', True)
        self.assertEqual(len(data_list), len(expected_data_list), message)
        for i in range(len(data_list)):
            data = data_list[i]
            expected_data = expected_data_list[i]
            self.assertEqual(data['name'], expected_data['name'], message)
            self.assertEqual(data['enabled'], expected_data['enabled'], message)
            self.assertEqual(data['min'], expected_data['min'], message)
            self.assertEqual(data['range'], expected_data['range'], message)
            self.assertEqual(data['ip'], expected_data['ip'], message)
            self.assertEqual(data['mac'], expected_data['mac'], message)
            self.assertAlmostEqual(data['ratio'], expected_data['ratio'], 7, message)
            self.assertEqual(data['color'], expected_data['color'], message)
    
    def check_rest_test_tag_data_result(self, data_list, expected_data_list, message=None):
        data_list = get_sorted_list(data_list, 'name', True)
        expected_data_list = get_sorted_list(expected_data_list, 'name', True)
        self.assertEqual(len(data_list), len(expected_data_list), message)
        for i in range(len(data_list)):
            data = data_list[i]
            expected_data = expected_data_list[i]
            self.assertEqual(str(data['id']), expected_data['id'], message)
            self.assertEqual(data['rest-test'], expected_data['rest-test'], message)
            self.assertEqual(data['name'], expected_data['name'], message)
            self.assertEqual(data['value'], expected_data['value'], message)
            
    def test_put_one(self):
        test_put_rest_model_data({'min':200,'color':'white'}, 'rest-test/foobar')
        response = test_get_rest_model_data('rest-test')
        data = simplejson.loads(response.content)
        rtd_copy = copy.deepcopy(self.REST_TEST_DATA)
        rtd_copy[0]['min'] = 200
        rtd_copy[0]['color'] = 'white'
        self.check_rest_test_data_result(data, rtd_copy)
        
    def test_put_query(self):
        test_put_rest_model_data({'min':200,'color':'white'}, 'rest-test', {'enabled':True})
        response = test_get_rest_model_data('rest-test')
        data = simplejson.loads(response.content)
        rtd_copy = copy.deepcopy(self.REST_TEST_DATA)
        for rtd in rtd_copy:
            if rtd['enabled']:
                rtd['min'] = 200
                rtd['color'] = 'white'
        self.check_rest_test_data_result(data, rtd_copy)
    
    def test_create_one(self):
        rtd_copy = copy.deepcopy(self.REST_TEST_DATA)
        create_rtd = rtd_copy[0].copy()
        create_rtd['name'] = 'test-create'
        rtd_copy.append(create_rtd)
        test_put_rest_model_data(create_rtd, 'rest-test')
        response = test_get_rest_model_data('rest-test')
        data = simplejson.loads(response.content)
        self.check_rest_test_data_result(data, rtd_copy)
    
    def test_create_many(self):
        rtd_copy = copy.deepcopy(self.REST_TEST_DATA)
        create_rtd1 = rtd_copy[0].copy()
        create_rtd1['name'] = 'test-create1'
        create_rtd1['min'] = 2000
        rtd_copy.append(create_rtd1)
        create_rtd2 = rtd_copy[0].copy()
        create_rtd2['name'] = 'test-create2'
        create_rtd1['min'] = 4000
        rtd_copy.append(create_rtd2)
        test_put_rest_model_data([create_rtd1,create_rtd2], 'rest-test')
        response = test_get_rest_model_data('rest-test')
        data = simplejson.loads(response.content)
        self.check_rest_test_data_result(data, rtd_copy)
        
    def test_get(self):
        RTD = self.REST_TEST_DATA
        RTTD = self.REST_TEST_TAG_DATA
        GET_TEST_VECTORS = (
            #model_name, query_params, expected_data
            ('rest-test', None, RTD, 'Failure getting all rest-test instances'),
            ('rest-test-tag', None, RTTD, 'Failure getting all rest-test-tag instances'),
            ('rest-test/foobar', None, RTD[0], 'Failure getting single rest-test instance by URL'),
            ('rest-test', {'name':'foobar'}, [RTD[0]], 'Failure querying for single rest-test instance'),
            ('rest-test', {'name':'foobar','nolist':True}, RTD[0], 'Failure querying for single rest-test instance using nolist'),
            ('rest-test', {'enabled':True}, [RTD[0],RTD[2]], 'Failure querying for multiple rest-test instances'),
            ('rest-test', {'name__startswith':'foobar'}, [RTD[0],RTD[2]], 'Failure querying for multiple rest-test instances using startswith'),
            ('rest-test', {'orderby':'ratio'}, [RTD[2],RTD[0],RTD[1]], 'Failure querying with ascending orderby'),
            ('rest-test', {'orderby':'-ratio'}, [RTD[1],RTD[0],RTD[2]], 'Failure querying with descending orderby'),
            ('rest-test', {'orderby':'enabled,-ratio'}, [RTD[1],RTD[0],RTD[2]], 'Failure querying with multi-field orderby'),
            ('rest-test-tag', {'rest-test':'foobar'}, [RTTD[0],RTTD[1]], 'Failure querying by ForeignKey value'),
            ('rest-test-tag', {'rest-test__startswith':'foo'}, [RTTD[0],RTTD[1]], 'Failure querying by ForeignKey value'),
            ('rest-test-tag', {'rest-test__contains':'swi'}, [RTTD[2]], 'Failure querying by ForeignKey value'),
        )
        
        for model_name, query_params, expected_data, message in GET_TEST_VECTORS:
            response = test_get_rest_model_data(model_name, query_params)
            data = simplejson.loads(response.content)
            #print model_name
            #self.check_rest_test_data_result(data, expected_data, message)
            if type(data) is not list:
                data = [data]
                #data = get_sorted_list(data, 'name', False)
            if type(expected_data) is not list:
                expected_data = [expected_data]
                #expected_data = get_sorted_list(expected_data, 'name', True)
            if model_name.startswith('rest-test-tag'):
                self.check_rest_test_tag_data_result(data, expected_data, message)
            else:
                self.check_rest_test_data_result(data, expected_data, message)
            
    def test_renamed_field(self):
        response = test_get_rest_model_data('rest-test-renamed/test')
        self.assertEqual(response.status_code,200)
        data = simplejson.loads(response.content)
        self.assertEqual(data['renamed'], 'test')
        
    def test_delete_one(self):
        test_delete_rest_model_data('rest-test/foobar')
        response = test_get_rest_model_data('rest-test')
        data = simplejson.loads(response.content)
        self.check_rest_test_data_result(data, self.REST_TEST_DATA[1:])
    
    def test_delete_query(self):
        test_delete_rest_model_data('rest-test', {'name__startswith':'foobar'})
        response = test_get_rest_model_data('rest-test')
        data = simplejson.loads(response.content)
        self.check_rest_test_data_result(data, [self.REST_TEST_DATA[1]])
    
    def test_delete_all(self):
        test_delete_rest_model_data('rest-test')
        response = test_get_rest_model_data('rest-test')
        data = simplejson.loads(response.content)
        self.assertEqual(len(data),0)

class NegativeTest(TestCase):
    
    def test_invalid_model_name(self):
        response = test_get_rest_model_data('foobar')
        self.assertEqual(response.status_code,400)
        response_obj = simplejson.loads(response.content)
        self.assertEqual(response_obj['error_type'],'RestInvalidDataTypeException')
        
    def test_resource_not_found(self):
        response = test_get_rest_model_data('rest-test/foobar')
        self.assertEqual(response.status_code,404)
        response_obj = simplejson.loads(response.content)
        self.assertEqual(response_obj['error_type'],'RestResourceNotFoundException')

    def test_invalid_orderby(self):
        response = test_get_rest_model_data('rest-test', {'orderby':'foobar'})
        self.assertEqual(response.status_code,400)
        response_obj = simplejson.loads(response.content)
        self.assertEqual(response_obj['error_type'],'RestInvalidOrderByException')
    
    def test_hidden_model(self):
        response = test_get_rest_model_data('restdisabledmodel')
        self.assertEqual(response.status_code,400)
        response_obj = simplejson.loads(response.content)
        self.assertEqual(response_obj['error_type'],'RestInvalidDataTypeException')

class ValidationTest(TestCase):
    
    DEFAULT_DATA = {'name':'foobar','enabled':True,'min':100,'range':30,'ip':'192.168.1.1','mac':'00:01:22:34:56:af','ratio':4.56,'color':'red'}
    TEST_VECTORS = (
        #update-dict, error-type, error-messgae
        ({'min':99}, ['min'],'Min value validation failed'),
        ({'range':8}, ['range'],'Min range validation failed'),
        ({'range':2000}, ['range'],'Max range validation failed'),
        ({'range':50}, None,'Model-level clean validation failed'),
        ({'mac':'00:01:02:03:05'}, ['mac'],'Too short MAC address validation failed'),
        ({'mac':'00:01:02:03:05:99:45'}, ['mac'],'Too long MAC address validation failed'),
        ({'mac':'00,01,02,03,05,99'}, ['mac'],'MAC address separator char validation failed'),
        ({'mac':'0r:01:02:03:05:99'}, ['mac'],'MAC address character validation failed'),
        ({'mac':'123:01:02:03:05:99'}, ['mac'],'MAC address byte length validation failed'),
        ({'color':'purple'}, ['color'],'Choices validation failed'),
        ({'ip':'foobar'}, ['ip'], 'Invalid IP address char validation failed'),
        ({'ip':'192.168.1'}, ['ip'], 'Too short IP address char validation failed'),
        ({'ip':'192.168.1.0.5'}, ['ip'], 'Too long IP address char validation failed'),
        ({'ip':'192,168,1,0'}, ['ip'], 'IP address separator char validation failed'),
        ({'ip':'256.168.1.0'}, ['ip'], 'Out of range IP address byte validation failed'),
        ({'min':99, 'ip':'256.168.1.0'}, ['min', 'ip'], 'Multiple field validation failed'),
    )
    
    def check_response(self, response, invalid_fields, message):
        self.assertEqual(response.status_code,400)
        response_obj = simplejson.loads(response.content)
        self.assertEqual(response_obj['error_type'], 'RestValidationException', message)
        if invalid_fields:
            self.assertEqual(response_obj.get('model_error'), None)
            field_errors = response_obj.get('field_errors')
            self.assertNotEqual(field_errors, None)
            self.assertEqual(len(invalid_fields), len(field_errors), message)
            for field_name in field_errors.keys():
                self.assertTrue(field_name in invalid_fields, message)
        else:
            self.assertEqual(response_obj.get('field_errors'), None)
            self.assertNotEqual(response_obj.get('model_error'), None)
        # FIXME: Maybe check the description here too?
        
    def test_create_validation(self):
        for test_data, invalid_fields, message in self.TEST_VECTORS:
            data = self.DEFAULT_DATA.copy()
            for name, value in test_data.items():
                data[name] = value
            put_response = test_put_rest_model_data(data, 'rest-test')
            self.check_response(put_response, invalid_fields, message)
            
    def test_update_validation(self):
        # First add an instance with the default data and check that it's OK
        put_response = test_put_rest_model_data(self.DEFAULT_DATA, 'rest-test')
        self.assertEqual(put_response.status_code,200)
        
        for test_data, invalid_fields, message in self.TEST_VECTORS:
            put_response = test_put_rest_model_data(test_data, 'rest-test/foobar')
            self.check_response(put_response, invalid_fields, message)


class ConfigHandlerTest(TestCase):

    TEST_DATA = {'name':'foobar','enabled':True,'min':100,'range':30,'ip':'192.168.1.1','mac':'00:01:22:34:56:af','ratio':4.56,'color':'red'}
    
    test_op = None
    test_old_instance = None
    test_new_instance = None
    test_modified_fields = None
    
    @staticmethod
    def reset_config_handler():
        ConfigHandlerTest.test_op = None
        ConfigHandlerTest.test_old_instance = None
        ConfigHandlerTest.test_new_instance = None
        ConfigHandlerTest.test_modified_fields = None
    
    @staticmethod
    def config_handler(op, old_instance, new_instance, modified_fields):
        ConfigHandlerTest.test_op = op
        ConfigHandlerTest.test_old_instance = old_instance
        ConfigHandlerTest.test_new_instance = new_instance
        ConfigHandlerTest.test_modified_fields = modified_fields
    
    def check_config_handler(self, expected_op, expected_old_instance, expected_new_instance, expected_modified_fields):
        self.assertEqual(ConfigHandlerTest.test_op, expected_op)
        self.assertTrue(model_instances_equal(ConfigHandlerTest.test_old_instance, expected_old_instance))
        self.assertTrue(model_instances_equal(ConfigHandlerTest.test_new_instance, expected_new_instance))
        self.assertEqual(ConfigHandlerTest.test_modified_fields, expected_modified_fields)
        
    def test_config(self):
        reset_last_config_state()
        # Install the config handler
        field_list = ['internal', 'min', 'mac']
        add_config_handler({RestTestModel: field_list}, ConfigHandlerTest.config_handler)
        
        # Check that config handler is triggered on an insert
        ConfigHandlerTest.reset_config_handler()
        test = RestTestModel(**self.TEST_DATA)
        test.save()
        self.check_config_handler('INSERT', None, test, None)
        
        # Check that config handler is triggered on an update
        ConfigHandlerTest.reset_config_handler()
        expected_old = RestTestModel.objects.get(pk='foobar')
        test.internal = 'check'
        test.min = 125
        test.save()
        self.check_config_handler('UPDATE', expected_old, test, {'internal': 'check', 'min': 125})
        
        # Check that config handler is not triggered on an update to a field that
        # it's not configured to care about
        ConfigHandlerTest.reset_config_handler()
        test.max = 500
        test.save()
        self.check_config_handler(None, None, None, None)
        
        # Check that config handler is triggered on a delete
        ConfigHandlerTest.reset_config_handler()
        test.delete()
        # delete() clears the pk which messes up the instance
        # comparison logic in check_config_handler, so we hack
        # it back to the value it had before the delete
        test.name = 'foobar'    
        self.check_config_handler('DELETE', test, None, None)


class CompoundKeyModelTest(TestCase):
    
    TEST_DATA = [
        {'name': 'foo', 'index': 3},
        {'name': 'foo', 'index': 7},
        {'name': 'bar', 'index': 2, 'extra': 'abc'},
        {'name': 'bar', 'index': 4, 'extra': 'test'},
    ]
    
    def setUp(self):
        for data in self.TEST_DATA:
            test_put_rest_model_data(data, 'rest-compound-key')
    
    def check_query_results(self, actual_results, expected_results):
        self.assertEqual(len(actual_results), len(expected_results))
        actual_results = get_sorted_list(actual_results, 'id', False)
        expected_results = copy.deepcopy(expected_results)
        for expected_result in expected_results:
            expected_result['id'] = expected_result['name'] + '|' + str(expected_result['index'])
        expected_results = get_sorted_list(expected_results, 'id')
        
        for actual_result, expected_result in zip(actual_results, expected_results):
            self.assertEqual(actual_result['id'], expected_result['id'])
            self.assertEqual(actual_result['name'], expected_result['name'])
            self.assertEqual(actual_result['index'], expected_result['index'])
            expected_extra = expected_result.get('extra', 'dummy')
            self.assertEqual(actual_result['extra'], expected_extra)
                
    def test_set_up(self):
        response = test_get_rest_model_data('rest-compound-key')
        self.assertEqual(response.status_code, 200)
        actual_results = simplejson.loads(response.content)
        self.check_query_results(actual_results, self.TEST_DATA)
        
    def test_delete_by_id(self):
        test_delete_rest_model_data('rest-compound-key', {'id': 'foo|3'})
        test_delete_rest_model_data('rest-compound-key', {'id': 'bar|4'})
        response = test_get_rest_model_data('rest-compound-key')
        self.assertEqual(response.status_code, 200)
        actual_results = simplejson.loads(response.content)
        self.check_query_results(actual_results, self.TEST_DATA[1:-1])
    
    def test_delete_by_fields(self):
        test_delete_rest_model_data('rest-compound-key', {'name': 'bar', 'index': 4})
        response = test_get_rest_model_data('rest-compound-key')
        self.assertEqual(response.status_code, 200)
        actual_results = simplejson.loads(response.content)
        self.check_query_results(actual_results, self.TEST_DATA[:-1])
        
class UserDataTest(TestCase):
    
    TEST_DATA = [
        # name, data, binary, content-type
        ('test/foobar', 'hello world\nanother line', False, None),
        ('test/json-test', '[0,1,2]', False, 'application/json'),
        ('test/binary-test', '\x01\x02\x03\x04\x55', True, None),
    ]
    
    MORE_TEST_DATA = [
        ('moretests/foo1', 'moretest1', False, None),
        ('moretests/foo2', 'moretest2', False, None),
    ]
    
    def add_user_data(self, client, user_data):
        for name, data, binary, content_type in user_data:
            if not content_type:
                if binary:
                    content_type = 'application/octet-stream'
                else:
                    content_type = 'text/plain'
            url = test_construct_rest_url('data/' + name, {'binary':binary})
            response = client.put(url, data, content_type)
            self.assertEquals(response.status_code, 200)

    def check_expected_info_list(self, info_list, expected_info_list):
        self.assertEqual(type(info_list), list)
        info_list = get_sorted_list(info_list, 'name', True)
        expected_info_list = get_sorted_list(expected_info_list, None, True)
        self.assertEqual(len(info_list), len(expected_info_list))
        for i in range(len(info_list)):
            data = info_list[i]
            expected_data = expected_info_list[i]
            expected_name = expected_data[0]
            expected_url_path = 'rest/v1/data/' + expected_name + '/'
            self.assertEqual(data['name'], expected_name)
            self.assertEqual(data['url_path'], expected_url_path)

    def get_info_list(self, client, query_params=None):
        url = test_construct_rest_url('data', query_params)
        get_response = client.get(url)
        self.assertEqual(get_response.status_code, 200)
        info_list = simplejson.loads(get_response.content)
        
        # The view for listing user data items hard-codes inclusion of some
        # *magic* user data items corresponding to the startup-config
        # and update-config, depending on the presence of certain files
        # in the file system. To make the unit tests work correctly if/when
        # either/both of those items are added we do this filtering pass
        # over the items returned from the REST API. Ugly hack!
        info_list = [item for item in info_list if '-config' not in item['name']]
        
        return info_list

    def test_user_data_basic(self):
        c = Client()
        self.add_user_data(c, self.TEST_DATA)
        for name, data, binary, content_type in self.TEST_DATA:
            if not content_type:
                if binary:
                    content_type = 'application/octet-stream'
                else:
                    content_type = 'text/plain'
            url = test_construct_rest_url('data/' + name)
            get_response = c.get(url)
            self.assertEqual(get_response.content, data)
            self.assertEqual(get_response['Content-Type'], content_type)

    def test_user_data_delete(self):
        c = Client()
        
        self.add_user_data(c, self.TEST_DATA)
        self.add_user_data(c, self.MORE_TEST_DATA)
        
        # Do a delete with a query filter
        url = test_construct_rest_url('data', {'name__startswith':'moretests/'})
        response = c.delete(url)
        self.assertEqual(response.status_code, 200)
        
        info_list = self.get_info_list(c)
        expected_info_list = self.TEST_DATA
        self.check_expected_info_list(info_list, expected_info_list)
        
        # Do a delete of a specific user data element
        url = test_construct_rest_url('data/' + self.TEST_DATA[0][0])
        response = c.delete(url)
        self.assertEqual(response.status_code, 200)

        info_list = self.get_info_list(c)
        expected_info_list = self.TEST_DATA[1:]
        self.check_expected_info_list(info_list, expected_info_list)

    def test_user_data_info(self):

        c = Client()
        
        self.add_user_data(c, self.TEST_DATA)
        self.add_user_data(c, self.MORE_TEST_DATA)
        
        info_list = self.get_info_list(c)
        expected_info_list = self.TEST_DATA + self.MORE_TEST_DATA
        self.check_expected_info_list(info_list, expected_info_list)
        
        info_list = self.get_info_list(c, {'name__startswith':'test/'})
        expected_info_list = self.TEST_DATA
        self.check_expected_info_list(info_list, expected_info_list)
