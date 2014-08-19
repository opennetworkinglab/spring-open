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

from cassandra.ttypes import KsDef, CfDef, InvalidRequestException, TTransport, \
    SlicePredicate, SliceRange, ColumnParent, ConsistencyLevel, ColumnPath, \
    Mutation, Deletion, KeyRange, Column, ColumnOrSuperColumn, SuperColumn
from django.conf import settings
from .utils import CassandraConnection
import random
import datetime
import time
from sdncon.controller.config import get_local_controller_id

CONTROLLER_STATS_NAME = 'controller'
SWITCH_STATS_NAME = 'switch'
PORT_STATS_NAME = 'port'
TARGET_INDEX_NAME = 'target_index'
STATS_TYPE_INDEX_NAME = 'stats_type_index'

STATS_COLUMN_FAMILY_NAME_SUFFIX = '_stats'
STATS_TARGET_TYPE_PUT_DATA_SUFFIX = '-stats'

STATS_BUCKET_PERIOD = 60*60*24*1000    # 1 day in milliseconds
STATS_PADDED_COLUMN_TIME_LENGTH = len(str(STATS_BUCKET_PERIOD))

EVENTS_COLUMN_FAMILY_NAME = 'events'
EVENTS_BUCKET_PERIOD = 60*60*24*1000    # 1 day in milliseconds
EVENTS_PADDED_COLUMN_TIME_LENGTH = len(str(EVENTS_BUCKET_PERIOD))

THIRTY_SECOND_INTERVAL = 30 * 1000
ONE_MINUTE_INTERVAL = 60 * 1000
FIVE_MINUTE_INTERVAL = 5 * ONE_MINUTE_INTERVAL
TEN_MINUTE_INTERVAL = 10 * ONE_MINUTE_INTERVAL
ONE_HOUR_INTERVAL = 60 * ONE_MINUTE_INTERVAL
FOUR_HOUR_INTERVAL = 4 * ONE_HOUR_INTERVAL
ONE_DAY_INTERVAL = 24 * ONE_HOUR_INTERVAL
ONE_WEEK_INTERVAL = 7 * ONE_DAY_INTERVAL
FOUR_WEEK_INTERVAL = 4 * ONE_WEEK_INTERVAL

DOWNSAMPLE_INTERVALS = (ONE_MINUTE_INTERVAL, TEN_MINUTE_INTERVAL,
                        ONE_HOUR_INTERVAL, FOUR_HOUR_INTERVAL,
                        ONE_DAY_INTERVAL, ONE_WEEK_INTERVAL,
                        FOUR_WEEK_INTERVAL)

WINDOW_INTERVALS = (THIRTY_SECOND_INTERVAL, ONE_MINUTE_INTERVAL,
                    FIVE_MINUTE_INTERVAL, TEN_MINUTE_INTERVAL)

VALUE_DATA_FORMAT = 'value'
RATE_DATA_FORMAT = 'rate'


class StatsException(Exception):
    pass


class StatsInvalidStatsDataException(StatsException):
    def __init__(self):
        super(StatsInvalidStatsDataException,self).__init__(
            'Error adding stats data with incorrect format')


class StatsDatabaseConnectionException(StatsException):
    def __init__(self):
        super(StatsDatabaseConnectionException,self).__init__(
            'Error connecting to stats database')


class StatsDatabaseAccessException(StatsException):
    def __init__(self):
        super(StatsDatabaseAccessException,self).__init__(
            'Error accessing stats database')
  
class StatsNonnumericValueException(StatsException):
    def __init__(self, value):
        super(StatsNonnumericValueException,self).__init__(
            'Invalid non-numeric stat value for rate or '
            'average value computation: ' + str(value))


class StatsRateComputationException(StatsException):
    def __init__(self):
        super(StatsRateComputationException,self).__init__(
            'Error computing rate; not enough raw data')


class StatsInvalidDataFormatException(StatsException):
    def __init__(self, data_format):
        super(StatsInvalidDataFormatException,self).__init__(
            'Invalid data format: ' + str(data_format))


class StatsInvalidStatsTimeRangeException(StatsException):
    def __init__(self, start_time, end_time):
        super(StatsInvalidStatsTimeRangeException,self).__init__(
            'Invalid stats time range; start = %s; end = %s' %
            (str(start_time), str(end_time)))


class StatsInvalidStatsTypeException(StatsException):
    def __init__(self, stats_type):
        super(StatsInvalidStatsTypeException,self).__init__(
            'Invalid stats type; name = %s' % str(stats_type))


class StatsInvalidStatsMetadataException(StatsException):
    def __init__(self, file_name):
        super(StatsInvalidStatsMetadataException,self).__init__(
            'Invalid stats metadata from file \"%s\"' % str(file_name))


class StatsInternalException(StatsException):
    def __init__(self, message):
        super(StatsInternalException,self).__init__(
            'Stats internal error: \"%s\"' % str(message))

class StatsCreateColumnFamilyException(StatsException):
    def __init__(self, name):
        super(StatsCreateColumnFamilyException,self).__init__(
            'Error creating column family; name = ' % name)
        
# The following code is a hack to get the stats code to use a freshly
# created test keyspace when we're running the unit tests. I'm guessing
# there must be some way to detect if we're running under the Django
# (or Python) unit test framework, but I couldn't find any info about
# how to do that. So for now, we just provide this function that the
# unit tests must call before they make any stats call to get the stats
# code to use the test keyspace instead of the normal production
# keyspace. Bother.

use_test_keyspace = False

def set_use_test_keyspace():
    global use_test_keyspace
    use_test_keyspace = True
    

stats_db_connection = None

# FIXME: Should ideally create the column families on demand as the data
# is added, so the different target types don't need to be predefined here.
# Probably not a big problem for right now, though.
COLUMN_FAMILY_INFO_LIST = (
    {'name': TARGET_INDEX_NAME,
     'column_type': 'Super',
     'comparator_type': 'UTF8Type',
     'subcomparator_type': 'UTF8Type'},
    {'name': STATS_TYPE_INDEX_NAME,
     'column_type': 'Super',
     'comparator_type': 'UTF8Type',
     'subcomparator_type': 'UTF8Type'},
    {'name': CONTROLLER_STATS_NAME + STATS_COLUMN_FAMILY_NAME_SUFFIX,
     'comparator_type': 'UTF8Type'},
    {'name': SWITCH_STATS_NAME + STATS_COLUMN_FAMILY_NAME_SUFFIX,
     'comparator_type': 'UTF8Type'},
    {'name': PORT_STATS_NAME + STATS_COLUMN_FAMILY_NAME_SUFFIX,
     'comparator_type': 'UTF8Type'},
    {'name': EVENTS_COLUMN_FAMILY_NAME,
     'column_type': 'Super',
     'comparator_type': 'UTF8Type',
     'subcomparator_type': 'UTF8Type'},
)
        

def init_stats_db_connection(host, port, keyspace, user, password,
                             replication_factor, column_family_def_default_settings):
    global stats_db_connection
    if not stats_db_connection:
        if use_test_keyspace:
            keyspace = "test_" + keyspace
            
        try:
            stats_db_connection = CassandraConnection(host, port, keyspace, user, password)
            stats_db_connection.connect()
        except Exception:
            stats_db_connection = None
            raise StatsException("Error connecting to Cassandra daemon")
        
        if use_test_keyspace:
            try:
                stats_db_connection.get_client().system_drop_keyspace(keyspace)
            except Exception:
                pass
            
        try:
            stats_db_connection.set_keyspace()
            create_keyspace = False
        except Exception:
            create_keyspace = True
        
        if create_keyspace:
            keyspace_def = KsDef(name=keyspace,
                                 strategy_class='org.apache.cassandra.locator.SimpleStrategy',
                                 replication_factor=replication_factor,
                                 cf_defs=[])
            try:
                stats_db_connection.get_client().system_add_keyspace(keyspace_def)
                stats_db_connection.set_keyspace()
            except Exception, _e:
                stats_db_connection = None
                raise StatsException("Error creating stats keyspace")
        
        for column_family_info in COLUMN_FAMILY_INFO_LIST:
            try:
                column_family_def_settings = column_family_def_default_settings.copy()
                column_family_def_settings.update(column_family_info)
                column_family_def_settings['keyspace'] = keyspace
                # pylint: disable=W0142
                stats_db_connection.get_client().system_add_column_family(
                    CfDef(**column_family_def_settings))
            except InvalidRequestException, _e:
                # Assume this is because the column family already exists.
                # FIXME. Could check exception message for specific string
                pass
            except Exception, _e:
                stats_db_connection = None
                raise StatsCreateColumnFamilyException(column_family_info.get('name'))


def get_stats_db_connection():
    return stats_db_connection


# The following function is mainly intended to be used by the unit tests. It lets
# you clear out all of the data from the database. Note that since the stats DB
# is not managed by the normal Django DB mechanism you don't get the automatic
# DB flushing from the Django TestCase code, so it has to be done explicitly.
# There's a StatsTestCase subclass of TestCase in the stats unit tests that
# implements the tearDown method to call flush_stats_db after each test.
def flush_stats_db():
    if stats_db_connection is not None:
        for column_family_info in COLUMN_FAMILY_INFO_LIST:
            stats_db_connection.get_client().truncate(column_family_info['name'])

def call_cassandra_with_reconnect(fn, *args, **kwargs):
    try:
        try:
            results = fn(*args, **kwargs)
        except TTransport.TTransportException:
            stats_db_connection.reconnect()
            results = fn(*args, **kwargs)
    except TTransport.TTransportException, _e:
        raise StatsDatabaseConnectionException()
    except Exception, _e:
        raise StatsDatabaseAccessException()

    return results


def get_stats_padded_column_part(column_part):
    """
    For the columns to be sorted correctly by time we need to pad with
    leading zeroes up to the maximum range of the bucket
    """
    column_part = str(column_part)
    leading_zeroes = ('0'*(STATS_PADDED_COLUMN_TIME_LENGTH-len(column_part)))
    column_part = leading_zeroes + column_part
    return column_part


def split_stats_timestamp(timestamp):
    key_part = timestamp / STATS_BUCKET_PERIOD
    column_part = timestamp % STATS_BUCKET_PERIOD
    return (key_part, column_part)


def construct_stats_key(cluster, target_id, stats_type, timestamp_key_part):
    """
    Constructs the keys for the controller or switch stats.
    For the controller stats the target_id is the controller node id.
    For switch stats the target_id is the dpid of the switch.
    """
    return cluster + '|' + target_id + '|' + stats_type + '|' + str(timestamp_key_part)


def append_stats_results(get_results, values, timestamp_key_part):
    shifted_timestamp_key_part = int(timestamp_key_part) * STATS_BUCKET_PERIOD
    for item in get_results:
        timestamp_column_part = int(item.column.name)
        value = item.column.value
        timestamp = shifted_timestamp_key_part + timestamp_column_part
        values.append((timestamp, value))


def get_stats_slice_predicate(column_start, column_end):
    if column_start != '':
        column_start = get_stats_padded_column_part(column_start)
    if column_end != '':
        column_end = get_stats_padded_column_part(column_end)
    slice_predicate = SlicePredicate(slice_range=SliceRange(
        start=column_start, finish=column_end, count=1000000))
    return slice_predicate


def check_time_range(start_time, end_time):
    if int(end_time) < int(start_time):
        raise StatsInvalidStatsTimeRangeException(start_time, end_time)
    
    
def check_valid_data_format(data_format):
    if data_format != VALUE_DATA_FORMAT and data_format != RATE_DATA_FORMAT:
        raise StatsInvalidDataFormatException(data_format)


def get_window_range(raw_stats_values, index, window):

    if window == 0:
        return (index, index)
    
    # Get start index
    timestamp = raw_stats_values[index][0]
    start_timestamp = timestamp - (window / 2)
    end_timestamp = timestamp + (window / 2)
    
    start_index = index
    while start_index > 0:
        next_timestamp = raw_stats_values[start_index - 1][0]
        if next_timestamp < start_timestamp:
            break
        start_index -= 1
    
    end_index = index
    while end_index < len(raw_stats_values) - 1:
        next_timestamp = raw_stats_values[end_index + 1][0]
        if next_timestamp > end_timestamp:
            break
        end_index += 1
        
    return (start_index, end_index)


def convert_stat_string_to_value(stat_string):
    try:
        stat_value = int(stat_string)
    except ValueError:
        try:
            stat_value = float(stat_string)
        except ValueError:
            stat_value = stat_string
    return stat_value


def get_rate_over_stats_values(stats_values):
    
    if len(stats_values) < 2:
        return None
    
    start_stat = stats_values[0]
    end_stat = stats_values[-1]
    
    timestamp_delta = end_stat[0] - start_stat[0]
    # NOTE: In computing the value_delta here it's safe to assume floats
    # rather than calling convert_stat_string_to_value because we're going
    # to be converting to float anyway when we do the rate calculation later.
    # So there's no point in trying to differentiate between int and float
    # and rate doesn't make sense for any other type of stat data (e.g. string)
    value_delta = float(end_stat[1]) - float(start_stat[1])
    if timestamp_delta == 0:
        rate = float('inf' if value_delta > 0  else '-inf')
    else:
        rate = value_delta / timestamp_delta
    
    return rate


def get_rate_over_window(raw_stats_values, index, window):
    if len(raw_stats_values) < 2:
        return None
    
    if window == 0:
        if index == 0:
            start_index = 0
            end_index = 1
        else:
            start_index = index - 1
            end_index = index
    else:
        start_index, end_index = get_window_range(raw_stats_values, index, window)
    
    return get_rate_over_stats_values(raw_stats_values[start_index:end_index + 1])


def get_average_over_stats_values(stats_values):
    
    total = 0
    count = 0
    for stat_value in stats_values:
        # FIXME: Should we just always convert to float here?
        # This would give a more accurate result for the average calculation
        # but would mean that the data type is different for a
        # zero vs. non-zero window size.
        value = convert_stat_string_to_value(stat_value[1])
        if type(value) not in (int,float):
            raise StatsNonnumericValueException(value)
        total += value
        count += 1
    
    return (total / count) if count > 0 else None


def get_average_value_over_window(raw_stats_values, index, window):
    start_index, end_index = get_window_range(raw_stats_values, index, window)
    stats_values = raw_stats_values[start_index:end_index + 1]
    return get_average_over_stats_values(stats_values)


def reverse_stats_data_generator(cluster, target_type, target_id, stats_type,
                                 start_time=None, end_time=None,
                                 chunk_interval=3600000):
    if start_time is None:
        start_time = int(time.time() * 1000)
    if end_time is None:
        # By default, don't go back past 1/1/2011. This was before we had stats support
        # in the controller, so we shouldn't have any data earlier than that (except if
        # the clock on the controller was set incorrectly).
        end_time = int(time.mktime(datetime.datetime(2011,1,1).timetuple()) * 1000)
    end_key_part, _end_column_part = split_stats_timestamp(end_time)
    key_part, column_part = split_stats_timestamp(start_time)
    column_family = target_type + STATS_COLUMN_FAMILY_NAME_SUFFIX
    column_parent = ColumnParent(column_family)
    # FIXME: Should add support for chunk_interval to be either iterable or a
    # list/tuple to give a sequence of chunk intervals to use. The last available
    # chunk interval from the list/tuple/iterator would then be used for any
    # subsequent cassandra calls
    #chunk_interval_iter = (chunk_interval if isinstance(chunk_interval, list) or
    #   isinstance(chunk_interval, tuple) else [chunk_interval])
    while key_part >= 0:
        key = construct_stats_key(cluster, target_id, stats_type, key_part)
        
        while True:
            column_start = column_part - chunk_interval
            if column_start < 0:
                column_start = 0
            slice_predicate = get_stats_slice_predicate(column_start, column_part)
            for attempt in (1,2):
                try:
                    get_results = stats_db_connection.get_client().get_slice(key,
                        column_parent, slice_predicate, ConsistencyLevel.ONE)
                    for item in reversed(get_results):
                        timestamp = (key_part * STATS_BUCKET_PERIOD) + int(item.column.name)
                        value = item.column.value
                        yield (timestamp, value)
                    break
                except TTransport.TTransportException:
                    # Only retry once, so if it's the second time through,
                    # propagate the exception
                    if attempt == 2:
                        raise StatsDatabaseConnectionException()
                    stats_db_connection.reconnect()
                except Exception:
                    raise StatsDatabaseAccessException()

            column_part = column_start
            if column_part == 0:
                break
        
        if key_part == end_key_part:
            break
        key_part -= 1
        column_part = STATS_BUCKET_PERIOD - 1


def get_latest_stat_data(cluster, target_type, target_id, stats_type,
                         window=0, data_format=VALUE_DATA_FORMAT):
    
    check_valid_data_format(data_format)
    
    minimum_data_points = 2 if data_format == RATE_DATA_FORMAT else 1
    stats_data_window = []
    latest_stat_timestamp = None
    
    start_time = int(time.time() * 1000)
    # Limit how far back we'll look for the latest stat value.
    # 86400000 is 1 day in ms
    end_time = start_time - 86400000
    for stat_data_point in reverse_stats_data_generator(cluster,
            target_type, target_id, stats_type, start_time, end_time):
        current_stat_timestamp = stat_data_point[0]
        if latest_stat_timestamp is None:
            latest_stat_timestamp = current_stat_timestamp
        
        # NOTE: For stats operations we treat the window for the rate or
        # average calculation to be centered around the current timestamp.
        # For the latest stat case there is no data after the current point.
        # We could extend the window back further so that the current timestamp
        # is the end of the window range instead of the middle, but then that
        # would be inconsistent with the other cases, so instead we just go
        # back to half the window size. I haven't been able to convince myself
        # strongly one way or the other which is better (or how much it matters)
        outside_window = (latest_stat_timestamp - current_stat_timestamp) > (window / 2)
        if len(stats_data_window) >= minimum_data_points and outside_window:
            break
        
        stats_data_window.insert(0, stat_data_point)
    
        if (window == 0) and (len(stats_data_window) >= minimum_data_points):
            break
        
    stat_data_point = None
    
    if latest_stat_timestamp is not None:
        if data_format == VALUE_DATA_FORMAT:
            value = get_average_over_stats_values(stats_data_window)
        else:
            assert data_format == RATE_DATA_FORMAT, "Invalid data format"
            value = get_rate_over_stats_values(stats_data_window)
        if value is not None:
            stat_data_point = (latest_stat_timestamp, value)

    return stat_data_point


def get_stats_data(cluster, target_type, target_id, stats_type,
                   start_time, end_time, sample_interval=0, window=0,
                   data_format=VALUE_DATA_FORMAT, limit=None):
    
    check_time_range(start_time, end_time)
    check_valid_data_format(data_format)
    # FIXME: Add validation of other arguments
    
    start_key_part, start_column_part = split_stats_timestamp(int(start_time))
    end_key_part, end_column_part = split_stats_timestamp(int(end_time))
    
    raw_stats_values = []
    column_family = target_type + STATS_COLUMN_FAMILY_NAME_SUFFIX
    column_parent = ColumnParent(column_family)
    
    for key_part in range(start_key_part, end_key_part+1):
        current_start = start_column_part if key_part == start_key_part else ''
        current_end = end_column_part if key_part == end_key_part else ''
        # FIXME: How big can the count be?
        slice_predicate = get_stats_slice_predicate(current_start, current_end)
        key = construct_stats_key(cluster, target_id, stats_type, key_part)
        for attempt in (1,2):
            try:
                get_results = stats_db_connection.get_client().get_slice(key,
                    column_parent, slice_predicate, ConsistencyLevel.ONE)
                break
            except TTransport.TTransportException:
                # Only retry once, so if it's the second time through,
                # propagate the exception
                if attempt == 2:
                    raise StatsDatabaseConnectionException()
                stats_db_connection.reconnect()
            except Exception:
                raise StatsDatabaseAccessException()

        append_stats_results(get_results, raw_stats_values, key_part)
    
        # FIXME: This logic to handle the limit argument isn't complete.
        # It doesn't account for a non-zero window or dpwnsampling.
        if (limit != None and sample_interval == 0 and window == 0 and
            len(raw_stats_values) > limit):
            raw_stats_values = raw_stats_values[:limit]
            break
    
    stats_values = []
    last_downsample_index = None
    for i in range(0, len(raw_stats_values)):
        # Handle downsampling
        if sample_interval != 0:
            downsample_index = raw_stats_values[i][0] / sample_interval
            if downsample_index == last_downsample_index:
                continue
            last_downsample_index = downsample_index
        
        # Get the value based for the specified data format
        if data_format == VALUE_DATA_FORMAT:
            if sample_interval == 0:
                value = convert_stat_string_to_value(raw_stats_values[i][1])
            else:
                value = get_average_value_over_window(raw_stats_values, i, window)
        else:
            assert data_format == RATE_DATA_FORMAT, "Invalid data format"
            value = get_rate_over_window(raw_stats_values, i, window)
            
        if value is not None:
            stats_values.append((raw_stats_values[i][0], value))
    
    return stats_values


def delete_stats_data(cluster, target_type, target_id, stats_type,
                      start_time, end_time):
    
    check_time_range(start_time, end_time)
    # FIXME: Add validation of other arguments
    
    start_key_part, start_column_part = split_stats_timestamp(int(start_time))
    end_key_part, end_column_part = split_stats_timestamp(int(end_time))
    
    column_family = target_type + STATS_COLUMN_FAMILY_NAME_SUFFIX
    column_parent = ColumnParent(column_family)
    # The Cassandra timestamps are in microseconds, not milliseconds,
    # so we convert to microseconds. The Cassandra timestamp is derived
    # from the stat timestamp (i.e. same time converted to microseconds),
    # so we use the end_time + 1, since that's guaranteed to be greater
    # than any of the timestamps for the sample points we're deleting.
    timestamp = (int(end_time) * 1000) + 1
    for key_part in range(start_key_part, end_key_part+1):
        key = construct_stats_key(cluster, target_id, stats_type, key_part)
        current_start = start_column_part if key_part == start_key_part else ''
        current_end = end_column_part if key_part == end_key_part else ''
        if current_start == '' and current_end == '':
            call_cassandra_with_reconnect(stats_db_connection.get_client().remove,
                key, ColumnPath(column_family=column_family), timestamp,
                ConsistencyLevel.ONE)
        else:
            # grrr. Cassandra currently doesn't support doing deletions via a
            # slice range (i.e. a column start and end). You need to give it a
            # list of columns. So we do a get_slice with the slice range and then
            # extract the individual column names from the result of that and
            # build up the column list that we can use to delete the column
            # using batch_mutate.
            slice_predicate = get_stats_slice_predicate(current_start, current_end)
            get_results = call_cassandra_with_reconnect(
                stats_db_connection.get_client().get_slice,
                key, column_parent, slice_predicate, ConsistencyLevel.ONE)
            column_names = []
            for item in get_results:
                column_names.append(item.column.name)
            
            deletion = Deletion(timestamp=timestamp, predicate=SlicePredicate(column_names=column_names))
            mutation_map = {key: {column_family: [Mutation(deletion=deletion)]}}
            call_cassandra_with_reconnect(stats_db_connection.get_client().batch_mutate,
                                          mutation_map, ConsistencyLevel.ONE)


STATS_METADATA_VARIABLE_NAME = 'STATS_METADATA'

stats_metadata = None

def init_stats_metadata(_cluster):
    """
    Initialize the dictionary of stats metadata. Currently this is initialized
    from a directory of metadata files that contain the metadata. Currently,
    there is no differentiation in the stats types that are supported across
    clusters, so we ignore the cluster argument and we just maintain a
    global map of stat type metadata.
    """
    global stats_metadata
    if not stats_metadata:
        stats_metadata = {}
        for module_name in settings.STATS_METADATA_MODULES:
            metadata_module = __import__(module_name,
                fromlist=[STATS_METADATA_VARIABLE_NAME])
            if not metadata_module:
                # FIXME: log error
                continue
            
            if STATS_METADATA_VARIABLE_NAME not in dir(metadata_module):
                # FIXME: log error
                continue
            
            metadata_list = getattr(metadata_module, STATS_METADATA_VARIABLE_NAME)
            
            if type(metadata_list) is dict:
                metadata_list = [metadata_list]

            if type(metadata_list) is not list and type(metadata_list) is not tuple:
                raise StatsInvalidStatsMetadataException(module_name)
            
            for metadata in metadata_list:
                if type(metadata) is not dict:
                    raise StatsInvalidStatsMetadataException(module_name)
                    
                name = metadata.get('name')
                if not name:
                    raise StatsInvalidStatsMetadataException(module_name)
                name = str(name)
                
                # Auto-set the verbose_name to the name if it's not set explicitly
                verbose_name = metadata.get('verbose_name')
                if not verbose_name:
                    metadata['verbose_name'] = name
                    
                # FIXME: Validate other contents of metadata.
                # e.g. flag name conflicts between files.
                
                stats_metadata[name] = metadata
                
def get_stats_metadata(cluster, stats_type=None):
    init_stats_metadata(cluster)
    # If no stat_type is specified return the entire dictionary of stat types
    metadata = stats_metadata.get(stats_type) if stats_type else stats_metadata
    if metadata is None:
        raise StatsInvalidStatsTypeException(stats_type)
    return metadata


STATS_INDEX_ATTRIBUTE_TYPES = {
    'last-updated': int
}
def stats_type_slice_to_index_data(stats_type_slice):
    index_data = {}
    for super_column in stats_type_slice:
        name = super_column.super_column.name
        column_list = super_column.super_column.columns
        if name == 'base':
            insert_map = index_data
        elif name.startswith('param:'):
            colon_index = name.find(':')
            parameter_name = name[colon_index+1:]
            parameter_map = index_data.get('parameters')
            if not parameter_map:
                parameter_map = {}
                index_data['parameters'] = parameter_map
            insert_map = {}
            parameter_map[parameter_name] = insert_map
        else:
            raise StatsInternalException('Invalid stats type index name: ' + str(name))
        
        for column in column_list:
            value = column.value
            attribute_type = STATS_INDEX_ATTRIBUTE_TYPES.get(column.name)
            if attribute_type is not None:
                value = attribute_type(value)
            insert_map[column.name] = value
    
    return index_data


def get_stats_type_index(cluster, target_type, target_id, stats_type=None):
    column_parent = ColumnParent(STATS_TYPE_INDEX_NAME)
    slice_predicate = SlicePredicate(slice_range=SliceRange(
        start='', finish='', count=1000000))
    key_prefix = cluster + ':' + target_type + ':' + target_id
    if stats_type is None:
        key_range = KeyRange(start_key=key_prefix+':', end_key=key_prefix+';', count=100000)
        key_slice_list = call_cassandra_with_reconnect(
            stats_db_connection.get_client().get_range_slices,
            column_parent, slice_predicate, key_range, ConsistencyLevel.ONE)
        stats_index_data = {}
        for key_slice in key_slice_list:
            key = key_slice.key
            colon_index = key.rfind(':')
            if colon_index < 0:
                raise StatsInternalException('Invalid stats type index key: ' + str(key))
            stats_type = key[colon_index+1:]
            stats_index_data[stats_type] = stats_type_slice_to_index_data(key_slice.columns)
    else:
        key = key_prefix + ':' + stats_type
        stats_type_slice = call_cassandra_with_reconnect(
            stats_db_connection.get_client().get_slice, key, column_parent,
            slice_predicate, ConsistencyLevel.ONE)
        stats_index_data = stats_type_slice_to_index_data(stats_type_slice)
        
    return stats_index_data


def get_stats_target_types(cluster):
    column_parent = ColumnParent(TARGET_INDEX_NAME)
    slice_predicate = SlicePredicate(column_names=[])
    key_range = KeyRange(start_key=cluster+':', end_key=cluster+';', count=100000)
    key_slice_list = call_cassandra_with_reconnect(
        stats_db_connection.get_client().get_range_slices,
        column_parent, slice_predicate, key_range, ConsistencyLevel.ONE)
    
    target_types = {}
    for key_slice in key_slice_list:
        target_type = key_slice.key[len(cluster)+1:]
        
        target_types[target_type] = {}
    
    return target_types


STATS_TARGET_ATTRIBUTE_TYPES = {
    'last-updated': int
}

def get_stats_targets(cluster, target_type):
    key = cluster + ':' + target_type
    column_parent = ColumnParent(TARGET_INDEX_NAME)
    slice_predicate = SlicePredicate(slice_range=SliceRange(
        start='', finish='', count=1000000))
    super_column_list = call_cassandra_with_reconnect(
        stats_db_connection.get_client().get_slice, key, column_parent,
        slice_predicate, ConsistencyLevel.ONE)
    target_list = {}
    for item in super_column_list:
        target = {}
        for column in item.super_column.columns:
            value = column.value
            attribute_type = STATS_TARGET_ATTRIBUTE_TYPES.get(column.name)
            if attribute_type is not None:
                value = attribute_type(value)
            target[column.name] = value
        target_list[item.super_column.name] = target
        
    return target_list


# FIXME: Should update the code below to use these constants
# instead of string literals
LAST_UPDATED_ATTRIBUTE_NAME = 'last-updated'
CONTROLLER_ATTRIBUTE_NAME = 'controller'
BASE_SUPER_COLUMN_NAME = 'base'
PARAMETERS_SUPER_COLUMN_NAME = 'parameters'
PARAM_SUPER_COLUMN_NAME_PREFIX = 'param:'

def append_attributes_to_mutation_list(attributes, supercolumn_name, mutation_list):
    column_list = []
    for name, info in attributes.iteritems():
        timestamp, value = info
        column = Column(name=name, value=str(value), timestamp=timestamp*1000)
        column_list.append(column)
    mutation = Mutation(column_or_supercolumn=ColumnOrSuperColumn(
        super_column=SuperColumn(name=supercolumn_name, columns=column_list)))
    mutation_list.append(mutation)


def add_stat_type_index_info_to_mutation_map(cluster, target_type,
        stats_type_index, mutation_map):
    for key, stats_type_info in stats_type_index.iteritems():
        separator_index = key.find(':')
        assert separator_index >= 0
        base_stat_type_name = key[:separator_index]
        target_id = key[separator_index + 1:]
        stats_type_base_attributes, stats_type_params = stats_type_info
        mutation_list = []
        append_attributes_to_mutation_list(stats_type_base_attributes,
            'base', mutation_list)
        for name, attributes in stats_type_params.iteritems():
            append_attributes_to_mutation_list(attributes,
                'param:' + name, mutation_list)
        mutation_key = cluster + ':' + target_type + ':' + target_id + ':' + base_stat_type_name
        mutation_map[mutation_key] = {STATS_TYPE_INDEX_NAME: mutation_list}


def add_target_id_list_to_mutation_map(cluster, target_type,
                                       target_id_list, mutation_map):
    mutation_list = []
    for target_id, attributes in target_id_list:
        append_attributes_to_mutation_list(attributes, target_id, mutation_list)
    key = cluster + ':' + target_type
    mutation_map[key] = {TARGET_INDEX_NAME: mutation_list}


def _put_stats_data(cluster, target_type, stats_data):
    try:
        controller_id = get_local_controller_id()
        mutation_map = {}
        target_id_list = []
        stats_type_index = {}
        column_family = target_type + STATS_COLUMN_FAMILY_NAME_SUFFIX
        for (target_id, target_id_stats) in stats_data.iteritems():
            # Map 'localhost' controller to the actual ID for the local controller
            # FIXME: Eventually we should fix up the other components (e.g. statd)
            # that invoke this REST API to not use localhost and instead use the
            # REST API to obtain the real ID for the local controller, but for now
            # this works to ensure we're not using localhost in any of the data we
            # store in the DB (unless, of course, the uuid version of the controller
            # ID hasn't been written to the boot-config file, in which case it will
            # default to the old localhost value).
            if target_type == 'controller' and target_id == 'localhost':
                target_id = controller_id
            latest_id_timestamp = None
            for (stats_type, stats_data_array) in target_id_stats.iteritems():
                # Check if it's a parameterized type and extract the base
                # stat type and parameter name.
                parameter_separator = stats_type.find('__')
                if parameter_separator >= 0:
                    stats_type_base = stats_type[:parameter_separator]
                    stats_type_parameter = stats_type[parameter_separator+2:]
                else:
                    stats_type_base = stats_type
                    stats_type_parameter = None
                
                latest_stat_type_timestamp = None
                
                # Add the stats values to the mutation map
                for stats_value in stats_data_array:
                    timestamp = int(stats_value['timestamp'])
                    if latest_stat_type_timestamp is None or timestamp > latest_stat_type_timestamp:
                        latest_stat_type_timestamp = timestamp
                    if latest_id_timestamp is None or timestamp > latest_id_timestamp:
                        latest_id_timestamp = timestamp
                    value = stats_value['value']
                    timestamp_key_part, timestamp_column_part = split_stats_timestamp(timestamp)
                    key = construct_stats_key(cluster, target_id, stats_type, timestamp_key_part)
                    key_entry = mutation_map.get(key)
                    if not key_entry:
                        mutation_list = []
                        mutation_map[key] = {column_family: mutation_list}
                    else:
                        mutation_list = key_entry[column_family]
                    
                    # Note: convert the Cassandra timestamp value to microseconds to
                    # be consistent with standard Cassandra timestamp format.
                    mutation = Mutation(column_or_supercolumn=ColumnOrSuperColumn(
                        column=Column(name=get_stats_padded_column_part(timestamp_column_part),
                        value=str(value), timestamp=timestamp*1000)))
                    mutation_list.append(mutation)
                
                # Update the stat type index info.
                # There can be multiple parameterized types for each base stats type,
                # so we need to be careful about checking for existing data for
                # the index_entry. Because of the dictionary nature of the put data
                # and the way this is serialized into a Python dictionary, though,
                # we are guaranteed that there won't be multiple entries for a 
                # specific parameters stats type or the base stats type, so we don't
                # need to handle duplicates for those.
                if latest_stat_type_timestamp is not None:
                    stats_type_index_key = stats_type_base + ':' + target_id
                    stats_type_info = stats_type_index.get(stats_type_index_key)
                    if not stats_type_info:
                        # This is a tuple of two dictionaries: the attributes for
                        # the base stat type and a dictionary of the parameterized
                        # types that have been seen for that stat type. The
                        # parameterized type dictionary is keyed by the name of
                        # the parameterized type and the value is the associated
                        # attribute dictionary.
                        stats_type_info = ({},{})
                        stats_type_index[stats_type_index_key] = stats_type_info
                    stats_type_base_attributes, stats_type_params = stats_type_info
                    if stats_type_parameter is None:
                        attributes = stats_type_base_attributes
                    else:
                        attributes = stats_type_params.get(stats_type_parameter)
                        if attributes is None:
                            attributes = {}
                            stats_type_params[stats_type_parameter] = attributes
                    last_updated_entry = attributes.get('last-updated')
                    if last_updated_entry is None or latest_stat_type_timestamp > last_updated_entry[0]:
                        attributes['last-updated'] = (latest_stat_type_timestamp, latest_stat_type_timestamp)
                    
            # Update the target index
            if latest_id_timestamp is not None:
                # FIXME: Always set the controller attributes for now.
                # This could/should be optimized to not set this for stats
                # whose target type is 'controller' since those will
                # always be coming from the same controller (i.e. itself).
                # But that change requires some changes (albeit minor) to
                # syncd to work correctly which I don't want to mess with
                # right now.
                #attributes = {'last-updated': (latest_id_timestamp, latest_id_timestamp)}
                #if target_type != 'controller':
                #    attributes['controller'] = controller_id
                attributes = {'last-updated': (latest_id_timestamp, latest_id_timestamp),
                              'controller': (latest_id_timestamp, controller_id)}
                target_id_list.append((target_id, attributes))
    except Exception, _e:
        raise StatsInvalidStatsDataException()
    
    add_stat_type_index_info_to_mutation_map(cluster, target_type, stats_type_index, mutation_map)
    add_target_id_list_to_mutation_map(cluster, target_type, target_id_list, mutation_map)
    
    call_cassandra_with_reconnect(stats_db_connection.get_client().batch_mutate,
                                  mutation_map, ConsistencyLevel.ONE)


def put_stats_data(cluster, stats_data):
    for target_type, target_stats_data in stats_data.items():
        if target_type.endswith(STATS_TARGET_TYPE_PUT_DATA_SUFFIX):
            # Strip off the '-stats' suffix
            target_type = target_type[:-len(STATS_TARGET_TYPE_PUT_DATA_SUFFIX)]
        _put_stats_data(cluster, target_type, target_stats_data)
        

def get_events_padded_column_part(column_part):
    """
    For the columns to be sorted correctly by time we need to pad with
    leading zeroes up to the maximum range of the bucket
    """
    column_part = str(column_part)
    leading_zeroes = ('0'*(EVENTS_PADDED_COLUMN_TIME_LENGTH-len(column_part)))
    column_part = leading_zeroes + column_part
    return column_part


def split_events_timestamp(timestamp):
    key_part = timestamp / EVENTS_BUCKET_PERIOD
    column_part = timestamp % EVENTS_BUCKET_PERIOD
    return (key_part, column_part)


def construct_log_events_key(cluster, node_id, timestamp_key_part):
    return cluster + '|' + node_id + '|' + str(timestamp_key_part)


def get_events_slice_predicate(column_start, column_end):
    if column_start != '':
        column_start = get_events_padded_column_part(column_start)
    if column_end != '':
        # For the final key in the range of keys we want all of the
        # supercolumns whose name starts with end_column_part.
        # If the event has includes a pk tag then the format of the
        # supercolumn name is <timestamp-part>:<pk-tag>.
        # Otherwise it's just the <timestamp-part>. To get both of these
        # cases we set the column end value to be the <timestamp-part>
        # suffixed with ';' (which has an ordinal value 1 greater than
        # ':'. So this will get all of the events with the given column
        # end regardless of whether or not they include the pk tag.
        column_end = get_events_padded_column_part(column_end) + ';'
    slice_predicate = SlicePredicate(slice_range=SliceRange(
        start=column_start, finish=column_end, count=1000000))
    return slice_predicate


def append_log_events_results(event_results, event_list, timestamp_key_part, include_pk_tag=False):
    shifted_timestamp_key_part = int(timestamp_key_part) * EVENTS_BUCKET_PERIOD
    for item in event_results:
        event = {}
        super_column_name = item.super_column.name
        colon_index = super_column_name.find(":")
        if colon_index >= 0:
            if include_pk_tag:
                pk_tag = super_column_name[colon_index:]
                event['pk-tag'] = pk_tag
            timestamp_column_part = super_column_name[:colon_index]
        else:
            timestamp_column_part = super_column_name
        timestamp = shifted_timestamp_key_part + int(timestamp_column_part)
        event['timestamp'] = timestamp
        for column in item.super_column.columns:
            event[column.name] = column.value
        event_list.append(event)

     
def get_log_event_data(cluster, node_id, start_time, end_time, include_pk_tag=False):
    # FIXME: Add some validation of arguments
    start_key_part, start_column_part = split_events_timestamp(int(start_time))
    end_key_part, end_column_part = split_events_timestamp(int(end_time))
    
    event_list = []
    column_parent = ColumnParent(column_family=EVENTS_COLUMN_FAMILY_NAME)
    for key_part in range(start_key_part, end_key_part+1):
        current_start = start_column_part if key_part == start_key_part else ''
        current_end = end_column_part if key_part == end_key_part else ''
        # FIXME: How big can the count be?
        slice_predicate = get_events_slice_predicate(current_start, current_end)
        key = construct_log_events_key(cluster, node_id, key_part)
        for attempt in (1,2):
            try:
                results = stats_db_connection.get_client().get_slice(key,
                    column_parent, slice_predicate, ConsistencyLevel.ONE)
                break
            except TTransport.TTransportException:
                # Only retry once, so if it's the second time through,
                # propagate the exception
                if attempt == 2:
                    raise StatsDatabaseConnectionException()
                stats_db_connection.reconnect()
            except Exception:
                raise StatsDatabaseAccessException()
        append_log_events_results(results, event_list, key_part, include_pk_tag)
    
    return event_list


def put_log_event_data(cluster, log_events_data):
    try:
        mutation_map = {}
        for (node_id, node_events) in log_events_data.iteritems():
            for event in node_events:
                timestamp = event['timestamp']
                pk_tag = event.get('pk-tag')
                # If the entry in the put data does not specify a tag, then generate a random one.
                # This is so that we can have multiple events with the same timestamp.
                # FIXME: Is there something better we can do here?
                if not pk_tag:
                    pk_tag = random.randint(0,10000000000)
                timestamp = int(timestamp)
                timestamp_key_part, timestamp_column_part = split_events_timestamp(timestamp)
                key = construct_log_events_key(cluster, node_id, timestamp_key_part)
                key_entry = mutation_map.get(key)
                if not key_entry:
                    mutation_list = []
                    mutation_map[key] = {EVENTS_COLUMN_FAMILY_NAME: mutation_list}
                else:
                    mutation_list = key_entry[EVENTS_COLUMN_FAMILY_NAME]
                supercolumn_name = get_events_padded_column_part(timestamp_column_part)
                if pk_tag is not None:
                    supercolumn_name += (':' + str(pk_tag))
                # Build the list of columns in the supercolumn
                column_list = []
                for (name, value) in event.iteritems():
                    if name != 'timestamp':
                        column_list.append(Column(name=name, value=str(value),
                                                  timestamp=timestamp*1000))
                mutation = Mutation(column_or_supercolumn=ColumnOrSuperColumn(
                                    super_column=SuperColumn(name=supercolumn_name,
                                    columns=column_list)))
                mutation_list.append(mutation)
    except Exception:
        raise StatsInvalidStatsDataException()
    
    call_cassandra_with_reconnect(stats_db_connection.get_client().batch_mutate,
                                  mutation_map, ConsistencyLevel.ONE)

def delete_log_event_data(cluster, node_id, start_time, end_time):
    start_key_part, start_column_part = split_events_timestamp(int(start_time))
    end_key_part, end_column_part = split_events_timestamp(int(end_time))
    # The Cassandra timestamps are in microseconds, not milliseconds,
    # so we convert to microseconds. The Cassandra timestamp is derived
    # from the event timestamp (i.e. same time converted to microseconds),
    # so we use the end_time + 1, since that's guaranteed to be greater
    # than any of the timestamps for the sample points we're deleting.
    timestamp = (int(end_time) * 1000) + 1
    column_path = ColumnPath(column_family=EVENTS_COLUMN_FAMILY_NAME)
    column_parent = ColumnParent(column_family=EVENTS_COLUMN_FAMILY_NAME)
    for key_part in range(start_key_part, end_key_part+1):
        key = construct_log_events_key(cluster, node_id, key_part)
        current_start = start_column_part if key_part == start_key_part else ''
        current_end = end_column_part if key_part == end_key_part else ''
        if current_start == '' and current_end == '':
            call_cassandra_with_reconnect(stats_db_connection.get_client().remove,
                key, column_path, timestamp, ConsistencyLevel.ONE)
        else:
            # grrr. Cassandra currently doesn't support doing deletions via a
            # slice range (i.e. a column start and end). You need to give it a
            # list of columns. So we do a get_slice with the slice range and then
            # extract the individual column names from the result of that and
            # build up the column list that we can use to delete the column
            # using batch_mutate.
            slice_predicate = get_events_slice_predicate(current_start, current_end)
            get_results = call_cassandra_with_reconnect(
                stats_db_connection.get_client().get_slice,
                key, column_parent, slice_predicate, ConsistencyLevel.ONE)
            column_names = []
            for item in get_results:
                column_names.append(item.super_column.name)
            
            deletion = Deletion(timestamp=timestamp, predicate=SlicePredicate(column_names=column_names))
            mutation_map = {key: {EVENTS_COLUMN_FAMILY_NAME: [Mutation(deletion=deletion)]}}
            call_cassandra_with_reconnect(stats_db_connection.get_client().batch_mutate,
                                          mutation_map, ConsistencyLevel.ONE)


def get_closest_sample_interval(requested_sample_interval):
    for i in range(0, len(DOWNSAMPLE_INTERVALS)):
        if DOWNSAMPLE_INTERVALS[i] > requested_sample_interval:
            if i == 0:
                return requested_sample_interval
            downsample_interval = DOWNSAMPLE_INTERVALS[i - 1]
            break
    else:
        downsample_interval = DOWNSAMPLE_INTERVALS[-1]
    # Return the closest multiple of the downsampled interval
    return downsample_interval * (requested_sample_interval // downsample_interval)


def get_closest_window_interval(requested_window):
    for i in range(0, len(WINDOW_INTERVALS)):
        if WINDOW_INTERVALS[i] > requested_window:
            return WINDOW_INTERVALS[i - 1] if i > 0 else 0
    return WINDOW_INTERVALS[-1]
