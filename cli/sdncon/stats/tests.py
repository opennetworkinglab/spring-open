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

from django.test import TestCase, Client
import urllib
from django.utils import simplejson
from .data import set_use_test_keyspace, flush_stats_db
import time
from sdncon.controller.config import get_local_controller_id

def setUpModule():
    set_use_test_keyspace()
    
def test_construct_rest_url(path, query_params=None):
    url = '/rest/%s' % path
    if query_params:
        url += '?'
        url += urllib.urlencode(query_params)
    return url

def test_get_rest_data(path, query_params=None):
    url = test_construct_rest_url(path, query_params)
    c = Client()
    response = c.get(url)
    return response

def test_put_rest_data(obj, path, query_params=None):
    url = test_construct_rest_url(path, query_params)
    data = simplejson.dumps(obj)
    c = Client()
    response = c.put(url , data, 'application/json')
    return response

def test_delete_rest_data(path, query_params=None):
    url = test_construct_rest_url(path, query_params)
    c = Client()
    response = c.delete(url)
    return response

BASE_TIME = 1297278987000
SECONDS_CONVERT = 1000
MINUTES_CONVERT = 60 * SECONDS_CONVERT
HOURS_CONVERT = 60 * MINUTES_CONVERT
DAYS_CONVERT = 24 * HOURS_CONVERT

def make_timestamp(day, hour=0, minute=0, second=0, milliseconds=0):
    timestamp = BASE_TIME + (day * DAYS_CONVERT) + (hour * HOURS_CONVERT) + \
        (minute * MINUTES_CONVERT) + (second * SECONDS_CONVERT) + milliseconds
    return timestamp

class StatsTestCase(TestCase):
    def tearDown(self):
        flush_stats_db()
        
class BasicStatsTest(StatsTestCase):
    
    STATS_DATA = {
        'controller-stats': {
            '192.168.1.1': {
                'cpu-system': [
                    {'timestamp':make_timestamp(1,0),'value':1},
                    {'timestamp':make_timestamp(1,1),'value':2},
                    {'timestamp':make_timestamp(1,2),'value':3},
                    {'timestamp':make_timestamp(1,3),'value':4},
                    {'timestamp':make_timestamp(1,4),'value':5},
                    {'timestamp':make_timestamp(2,1),'value':6},
                    {'timestamp':make_timestamp(2,2),'value':7},
                    {'timestamp':make_timestamp(3,5),'value':8},
                    {'timestamp':make_timestamp(3,8),'value':9},
                    {'timestamp':make_timestamp(4,10),'value':10},
                    {'timestamp':make_timestamp(4,11),'value':11},
                    {'timestamp':make_timestamp(10,11),'value':12},
                ],
                'cpu-idle': [
                    {'timestamp':make_timestamp(1,1),'value':80},
                    {'timestamp':make_timestamp(1,2),'value':83},
                    {'timestamp':make_timestamp(1,3),'value':82},
                    {'timestamp':make_timestamp(1,4),'value':79},
                    {'timestamp':make_timestamp(1,5),'value':85},
                ]
            }
        },
        'switch-stats': {
            '00:01:02:03:04:05': {
                'flow-count': [
                    {'timestamp':make_timestamp(1,0),'value':60},
                    {'timestamp':make_timestamp(1,1),'value':88},
                    {'timestamp':make_timestamp(1,2),'value':102},
                ],
                'packet-count': [
                    {'timestamp':make_timestamp(1,0),'value':100},
                    {'timestamp':make_timestamp(1,1),'value':120},
                    {'timestamp':make_timestamp(1,2),'value':160},
                ],
                'packet-count__arp': [
                    {'timestamp':make_timestamp(1,0),'value':20},
                    {'timestamp':make_timestamp(1,3),'value':25},
                ],
                'packet-count__lldp': [
                    {'timestamp':make_timestamp(1,0),'value':30},
                    {'timestamp':make_timestamp(1,4),'value':15},
                ]
            }
        }
    }

    def check_stats_results(self, returned_results, expected_results, message=None):
        self.assertEqual(len(returned_results), len(expected_results), message)
        for i in range(len(returned_results)):
            expected_timestamp = expected_results[i]['timestamp']
            returned_timestamp = returned_results[i][0]
            expected_value = expected_results[i]['value']
            returned_value = returned_results[i][1]
            self.assertEqual(expected_timestamp, returned_timestamp, message)
            self.assertEqual(expected_value, returned_value, message)
    
    def setUp(self):
        response = test_put_rest_data(self.STATS_DATA, 'v1/stats/data/local')
        self.assertEqual(response.status_code, 200)
        
    def test_get_stats(self):
        # Get all of the cpu-system stat data
        response = test_get_rest_data('v1/stats/data/local/controller/192.168.1.1/cpu-system', {'start-time':make_timestamp(1,0),'end-time':make_timestamp(10,11), 'sample-interval':0})
        self.assertEqual(response.status_code, 200)
        results = simplejson.loads(response.content)
        self.check_stats_results(results, self.STATS_DATA['controller-stats']['192.168.1.1']['cpu-system'])
        
        # Get just one days data of the cpu-system stat
        response = test_get_rest_data('v1/stats/data/local/controller/192.168.1.1/cpu-system', {'start-time':make_timestamp(1,0),'end-time':make_timestamp(1,4), 'sample-interval':0})
        self.assertEqual(response.status_code, 200)
        results = simplejson.loads(response.content)
        self.check_stats_results(results, self.STATS_DATA['controller-stats']['192.168.1.1']['cpu-system'][:5])

        # Get two day range for cpu-system
        response = test_get_rest_data('v1/stats/data/local/controller/192.168.1.1/cpu-system', {'start-time':make_timestamp(1,2,10),'end-time':make_timestamp(2,2,20), 'sample-interval':0})
        self.assertEqual(response.status_code, 200)
        results = simplejson.loads(response.content)
        self.check_stats_results(results, self.STATS_DATA['controller-stats']['192.168.1.1']['cpu-system'][3:7])
        
        # Get all of the flow-count switch stat data
        response = test_get_rest_data('v1/stats/data/local/switch/00:01:02:03:04:05/flow-count', {'start-time':make_timestamp(1,0),'end-time':make_timestamp(2,0), 'sample-interval':0})
        self.assertEqual(response.status_code, 200)
        results = simplejson.loads(response.content)
        self.check_stats_results(results, self.STATS_DATA['switch-stats']['00:01:02:03:04:05']['flow-count'])

        # Get part of the packet-count switch stat data
        response = test_get_rest_data('v1/stats/data/local/switch/00:01:02:03:04:05/packet-count', {'start-time':make_timestamp(1,0),'end-time':make_timestamp(1,1), 'sample-interval':0})
        self.assertEqual(response.status_code, 200)
        results = simplejson.loads(response.content)
        self.check_stats_results(results, self.STATS_DATA['switch-stats']['00:01:02:03:04:05']['packet-count'][:2])

    def test_delete_stats(self):
        # Delete all but the first 2 and last 2 sample points
        response = test_delete_rest_data('v1/stats/data/local/controller/192.168.1.1/cpu-system', {
            'start-time': self.STATS_DATA['controller-stats']['192.168.1.1']['cpu-system'][2]['timestamp'],
            'end-time':self.STATS_DATA['controller-stats']['192.168.1.1']['cpu-system'][-3]['timestamp']})
        self.assertEquals(response.status_code, 200)

        response = test_get_rest_data('v1/stats/data/local/controller/192.168.1.1/cpu-system', {'start-time':make_timestamp(1,0),'end-time':make_timestamp(10,11), 'sample-interval':0})
        self.assertEqual(response.status_code, 200)
        results = simplejson.loads(response.content)
        self.check_stats_results(results, self.STATS_DATA['controller-stats']['192.168.1.1']['cpu-system'][:2] + self.STATS_DATA['controller-stats']['192.168.1.1']['cpu-system'][-2:])

    def test_stats_target_types(self):
        
        local_controller_id = get_local_controller_id()
        
        # Check getting the list of all target types
        response = test_get_rest_data('v1/stats/target/local/')
        self.assertEqual(response.status_code, 200)
        results = simplejson.loads(response.content)
        self.assertEqual(len(results.keys()), 2)
        self.assertTrue('controller' in results)
        self.assertTrue('switch' in results)
        
        # Check getting the info for the controller target type
        response = test_get_rest_data('v1/stats/target/local/controller')
        self.assertEqual(response.status_code, 200)
        results = simplejson.loads(response.content)
        self.assertEqual(len(results.keys()), 1)
        self.assertTrue('192.168.1.1' in results)
        controller_info = results['192.168.1.1']
        #self.assertEqual(controller_info['controller'], local_controller_id)
        self.assertEqual(controller_info['last-updated'], make_timestamp(10,11))

        # Check getting the info for the switch target type
        response = test_get_rest_data('v1/stats/target/local/switch')
        self.assertEqual(response.status_code, 200)
        results = simplejson.loads(response.content)
        self.assertEqual(len(results.keys()), 1)
        self.assertTrue('00:01:02:03:04:05' in results)
        switch_info = results['00:01:02:03:04:05']
        self.assertEqual(switch_info['controller'], local_controller_id)
        self.assertEqual(switch_info['last-updated'], make_timestamp(1,4))
    
    def check_stats_type_attributes(self, attributes, expected_last_updated,
                                    expected_target_type):
        last_updated = attributes.get('last-updated')
        self.assertEqual(last_updated, expected_last_updated)
        target_type = attributes.get('target-type')
        self.assertEqual(target_type, expected_target_type)
        
    def test_stats_type_index(self):
        response = test_get_rest_data('v1/stats/index/local/controller/192.168.1.1')
        self.assertEqual(response.status_code, 200)
        results = simplejson.loads(response.content)
        self.assertEqual(len(results), 2)
        attributes = results['cpu-system']
        self.assertEqual(len(attributes), 1)
        self.assertEqual(attributes['last-updated'], make_timestamp(10,11))
        attributes = results['cpu-idle']
        self.assertEqual(len(attributes), 1)
        self.assertEqual(attributes['last-updated'], make_timestamp(1,5))

        response = test_get_rest_data('v1/stats/index/local/switch/00:01:02:03:04:05')
        self.assertEqual(response.status_code, 200)
        results = simplejson.loads(response.content)
        self.assertEqual(len(results), 2)
        attributes = results['flow-count']
        self.assertEqual(len(attributes), 1)
        self.assertEqual(attributes['last-updated'], make_timestamp(1,2))
        attributes = results['packet-count']
        self.assertEqual(len(attributes), 2)
        self.assertEqual(attributes['last-updated'], make_timestamp(1,2))
        parameters = attributes['parameters']
        self.assertEqual(len(parameters), 2)
        attributes = parameters['arp']
        self.assertEqual(len(attributes), 1)
        self.assertEqual(attributes['last-updated'], make_timestamp(1,3))
        attributes = parameters['lldp']
        self.assertEqual(len(attributes), 1)
        self.assertEqual(attributes['last-updated'], make_timestamp(1,4))

        response = test_get_rest_data('v1/stats/index/local/controller/192.168.1.1/cpu-system')
        self.assertEqual(response.status_code, 200)
        attributes = simplejson.loads(response.content)
        self.assertEqual(len(attributes), 1)
        self.assertEqual(attributes['last-updated'], make_timestamp(10,11))
        
class StatsMetadataTest(StatsTestCase):
    
    def check_metadata(self, stats_type, stats_metadata):
        # The name in the metadata should match the stats_type
        name = stats_metadata.get('name')
        self.assertEqual(stats_type, name)
        
        # The target_type is a required value, so it should be present in the metadata
        target_type = stats_metadata.get('target_type')
        self.assertNotEqual(target_type, None)
        
        # NOTE: The following assertion works for now, since we only support these
        # two target types. Eventually we may support other target types (in which
        # case we'd need to add the new ones to the tuple) or else custom target
        # types can be added (in which case we'd maybe want to remove this assertion.
        self.assertTrue(target_type in ('controller','switch'))
        
        # verbose_name is set automatically by the REST code if it isn't set
        # explicitly in the metadata, so it should always be present.
        verbose_name = stats_metadata.get('verbose_name')
        self.assertNotEqual(verbose_name, None)
        
    def test_stats_metadata(self):
        response = test_get_rest_data('v1/stats/metadata/default')
        self.assertEqual(response.status_code, 200)
        stats_metadata_dict = simplejson.loads(response.content)
        self.assertEqual(type(stats_metadata_dict), dict)
        for stats_type, stats_metadata in stats_metadata_dict.items():
            # Check that the metadata looks reasonable
            self.check_metadata(stats_type, stats_metadata)
            
            # Fetch the metadata for the individual stats type and check that it matches
            response2 = test_get_rest_data('v1/stats/metadata/default/' + stats_type)
            self.assertEqual(response2.status_code, 200)
            stats_metadata2 = simplejson.loads(response2.content)
            self.assertEqual(stats_metadata, stats_metadata2)
    
    def test_invalid_stats_type(self):
        # Try getting a stats type that doesn't exist
        response = test_get_rest_data('v1/stats/metadata/default/foobar')
        self.assertEqual(response.status_code, 404)
        error_result = simplejson.loads(response.content)
        self.assertEqual(error_result.get('error_type'), 'RestResourceNotFoundException')
        
class LatestStatTest(StatsTestCase):

    def do_latest_stat(self, target_type, target_id):
        current_timestamp = int(time.time() * 1000)
        for i in range(23,-1,-1):
            # Try with different offsets. Sort of arbitrary list here. Potentially add
            # new offsets to test specific edge cases
            offset_list = [0, 3599999, 100, 30000, 400000, 3000000]
            timestamp = current_timestamp - (i * 3600000) - offset_list[(i+1)%len(offset_list)]
            stats_data = {target_type + '-stats': {target_id: {'test-stat': [{'timestamp': timestamp, 'value': i}]}}}
            response = test_put_rest_data(stats_data, 'v1/stats/data/local')
            self.assertEqual(response.status_code, 200)
            response = test_get_rest_data('v1/stats/data/local/%s/%s/test-stat' % (target_type, target_id))
            self.assertEqual(response.status_code, 200)
            results = simplejson.loads(response.content)
            self.assertEqual(timestamp, results[0])
            self.assertEqual(i, results[1])
    
    def test_controller_latest_stat(self):
        self.do_latest_stat('controller', '192.168.1.1')
    
    def test_switch_latest_stat(self):
        self.do_latest_stat('switch', '00:01:02:03:04:05')

class BasicEventsTest(StatsTestCase):
    
    EVENTS_DATA = {
        '192.168.1.1': [
            {'timestamp': make_timestamp(1,0), 'component': 'sdnplatform', 'log-level': 'Error', 'message': 'Something bad happened'},
            {'timestamp': make_timestamp(1,1), 'component': 'sdnplatform', 'log-level': 'Info', 'message': 'Something else happened', 'package': 'net.sdnplatformcontroller.core'},
            {'timestamp': make_timestamp(1,4), 'component': 'sdnplatform', 'log-level': 'Info', 'message': 'Switch connected: 01:02:03:04:45:56', 'package': 'net.sdnplatformcontroller.core', 'dpid': '01:02:03:04:45:56'},
            {'timestamp': make_timestamp(2,4), 'component': 'django', 'log-level': 'Info', 'message': 'GET: /rest/v1/model/foo'},
            {'timestamp': make_timestamp(4,10), 'component': 'cassandra', 'log-level': 'Info', 'message': 'Compaction occurred'},
            {'timestamp': make_timestamp(4,11), 'component': 'cassandra', 'log-level': 'Info', 'message': 'One more compaction occurred'},
            {'timestamp': make_timestamp(7,10), 'component': 'cassandra', 'log-level': 'Info', 'message': 'Another compaction occurred'},
        ]
    }

    TAGGED_EVENTS_DATA = {
        '192.168.1.1': [
            {'timestamp': make_timestamp(10,0), 'pk-tag':'1234', 'component': 'sdnplatform', 'log-level': 'Error', 'message': 'Something bad happened'},
            {'timestamp': make_timestamp(10,1), 'pk-tag':'5678', 'component': 'sdnplatform', 'log-level': 'Info', 'message': 'Something else happened', 'package': 'net.sdnplatformcontroller.core'},
        ]
    }


    def check_events_results(self, returned_results, expected_results, message=None):
        self.assertEqual(expected_results, returned_results, message)
        #self.assertEqual(len(returned_results), len(expected_results), message)
        #for i in range(len(returned_results)):
        #    self.assertEqual(returned_results[i], expected_results[i])
    
    def test_events(self):
        response = test_put_rest_data(self.EVENTS_DATA, 'v1/events/data/default')
        self.assertEqual(response.status_code, 200)
        
        # Get all of the data
        response = test_get_rest_data('v1/events/data/default/192.168.1.1', {'start-time':make_timestamp(1,0),'end-time':make_timestamp(7,10)})
        self.assertEqual(response.status_code, 200)
        results = simplejson.loads(response.content)
        self.check_events_results(results, self.EVENTS_DATA['192.168.1.1'])
        
        # Get just one days data
        response = test_get_rest_data('v1/events/data/default/192.168.1.1', {'start-time':make_timestamp(1,0),'end-time':make_timestamp(1,4)})
        self.assertEqual(response.status_code, 200)
        results = simplejson.loads(response.content)
        self.check_events_results(results, self.EVENTS_DATA['192.168.1.1'][:3])

        # Get two day range
        response = test_get_rest_data('v1/events/data/default/192.168.1.1', {'start-time':make_timestamp(1,2),'end-time':make_timestamp(4,11)})
        self.assertEqual(response.status_code, 200)
        results = simplejson.loads(response.content)
        self.check_events_results(results, self.EVENTS_DATA['192.168.1.1'][2:6])

    def test_tagged_events(self):
        response = test_put_rest_data(self.TAGGED_EVENTS_DATA, 'v1/events/data/default')
        self.assertEqual(response.status_code, 200)
        
        response = test_get_rest_data('v1/events/data/default/192.168.1.1', {'start-time':make_timestamp(10,0),'end-time':make_timestamp(10,2),'include-pk-tag':'true'})
        self.assertEqual(response.status_code, 200)
        results = simplejson.loads(response.content)
        self.check_events_results(results, self.TAGGED_EVENTS_DATA['192.168.1.1'])
    
    def test_delete_events(self):
        response = test_put_rest_data(self.EVENTS_DATA, 'v1/events/data/default')
        self.assertEqual(response.status_code, 200)

        # Delete all but the first 2 and last 2 events
        response = test_delete_rest_data('v1/events/data/default/192.168.1.1', {
            'start-time': self.EVENTS_DATA['192.168.1.1'][2]['timestamp'],
            'end-time':self.EVENTS_DATA['192.168.1.1'][-3]['timestamp']})
        self.assertEquals(response.status_code, 200)

        response = test_get_rest_data('v1/events/data/default/192.168.1.1', {'start-time':make_timestamp(1,0),'end-time':make_timestamp(7,10)})
        self.assertEqual(response.status_code, 200)
        results = simplejson.loads(response.content)
        self.check_events_results(results, self.EVENTS_DATA['192.168.1.1'][:2] + self.EVENTS_DATA['192.168.1.1'][-2:])
        
