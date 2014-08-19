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

from cassandra.ttypes import *
from django.conf import settings
from django.http import HttpResponse
from django.utils import simplejson
from functools import wraps
import time
from .data import StatsException, StatsInvalidStatsDataException, \
    StatsInvalidStatsTypeException, \
    get_stats_db_connection, init_stats_db_connection, \
    get_stats_metadata, get_stats_type_index, \
    get_stats_target_types, get_stats_targets, delete_stats_data, \
    get_stats_data, get_latest_stat_data, put_stats_data, \
    get_closest_sample_interval, get_closest_window_interval, \
    get_log_event_data, put_log_event_data, delete_log_event_data, \
    VALUE_DATA_FORMAT

from sdncon.rest.views import RestException, \
    RestInvalidPutDataException, RestMissingRequiredQueryParamException,\
    RestInvalidMethodException, RestDatabaseConnectionException,\
    RestInternalException, RestResourceNotFoundException, \
    safe_rest_view, JSON_CONTENT_TYPE, get_successful_response
from sdncon.controller.config import get_local_controller_id


class RestStatsException(RestException):
    def __init__(self, stats_exception):
        super(RestStatsException,self).__init__('Error accessing stats: ' + str(stats_exception))
        
class RestStatsInvalidTimeDurationUnitsException(RestException):
    def __init__(self, units):
        super(RestStatsInvalidTimeDurationUnitsException,self).__init__('Invalid time duration units: ' + str(units))


class RestStatsInvalidTimeRangeException(RestException):
    def __init__(self):
        super(RestStatsInvalidTimeRangeException,self).__init__('Invalid time range specified in stats REST API. '
            '2 out of 3 of start-time, end-time, and duration params must be specified.')


@safe_rest_view
def safe_stats_rest_view(func, *args, **kwargs):
    try:
        request = args[0]
        response = func(*args, **kwargs)
    except RestException:
        raise
    except StatsInvalidStatsDataException:
        raise RestInvalidPutDataException()
    except StatsInvalidStatsTypeException:
        raise RestResourceNotFoundException(request.path)
    except StatsException, e:
        raise RestStatsException(e)
    except Exception, e:
        raise RestInternalException(e)
    return response


def safe_stats_view(func):
    """
    This is a decorator that takes care of exception handling for the
    stats views so that stats exceptions are converted to the appropriate
    REST exception.
    """
    @wraps(func)
    def _func(*args, **kwargs):
        response = safe_stats_rest_view(func, *args, **kwargs)
        return response
    
    return _func


def init_db_connection():
    db_connection = get_stats_db_connection()
    if not db_connection:
        try:
            stats_db_settings = settings.STATS_DATABASE
        except Exception:
            stats_db_settings = {}
            
        host = stats_db_settings.get('HOST', 'localhost')
        port = stats_db_settings.get('PORT', 9160)
        keyspace = stats_db_settings.get('NAME', 'sdnstats')
        user = stats_db_settings.get('USER')
        password = stats_db_settings.get('PASSWORD')
        replication_factor = stats_db_settings.get('CASSANDRA_REPLICATION_FACTOR', 1)
        column_family_def_default_settings = stats_db_settings.get('CASSANDRA_COLUMN_FAMILY_DEF_DEFAULT_SETTINGS', {})
        
        init_stats_db_connection(host, port, keyspace, user, password, replication_factor, column_family_def_default_settings)
            
        db_connection = get_stats_db_connection()
        assert(db_connection is not None)

START_TIME_QUERY_PARAM = 'start-time'
END_TIME_QUERY_PARAM = 'end-time'
DURATION_QUERY_PARAM = 'duration'
SAMPLE_INTERVAL_QUERY_PARAM = 'sample-interval'
SAMPLE_COUNT_QUERY_PARAM = 'sample-count'
SAMPLE_WINDOW_QUERY_PARAM = 'sample-window'
DATA_FORMAT_QUERY_PARAM = 'data-format'
LIMIT_QUERY_PARAM = 'limit'
INCLUDE_PK_TAG_QUERY_PARAM = 'include-pk-tag'

DEFAULT_SAMPLE_COUNT = 50

def convert_time_point(time_point):
    
    if time_point is None:
        return None
    
    if time_point:
        time_point = time_point.lower()
        if time_point in ('now', 'current'):
            time_point = int(time.time() * 1000)
        else:
            time_point = int(time_point)
    
    return time_point


UNIT_CONVERSIONS = (
    (('h', 'hour', 'hours'), 3600000),
    (('d', 'day', 'days'), 86400000),
    (('w', 'week', 'weeks'), 604800000),
    (('m', 'min', 'mins', 'minute', 'minutes'), 60000),
    (('s', 'sec', 'secs', 'second', 'seconds'), 1000),
    (('ms', 'millisecond', 'milliseconds'), 1)
)

def convert_time_duration(duration):
    
    if duration is None:
        return None
    
    value = ""
    for c in duration:
        if not c.isdigit():
            break
        value += c
    
    units = duration[len(value):].lower()
    value = int(value)
    
    if units:
        converted_value = None
        for conversion in UNIT_CONVERSIONS:
            if units in conversion[0]:
                converted_value = value * conversion[1]
                break
        if converted_value is None:
            raise RestStatsInvalidTimeDurationUnitsException(units)
        
        value = converted_value
        
    return value


def get_time_range(start_time, end_time, duration):
    
    if not start_time and not end_time and not duration:
        return (None, None)
    
    start_time = convert_time_point(start_time)
    end_time = convert_time_point(end_time)
    duration = convert_time_duration(duration)
    
    if start_time:
        if not end_time and duration:
            end_time = start_time + duration
    elif end_time and duration:
        start_time = end_time - duration
        
    if not start_time or not end_time:
        raise RestStatsInvalidTimeRangeException()
    
    return (start_time, end_time)


def get_time_range_from_request(request):
    start_time = request.GET.get(START_TIME_QUERY_PARAM)
    end_time = request.GET.get(END_TIME_QUERY_PARAM)
    duration = request.GET.get(DURATION_QUERY_PARAM)
    
    return get_time_range(start_time, end_time, duration)

#def get_stats_time_range(request):
#    start_time = request.GET.get(START_TIME_QUERY_PARAM)
#    end_time = request.GET.get(END_TIME_QUERY_PARAM)
#    
#    if not start_time and not end_time:
#        return None
#
#    if not start_time:
#        raise RestMissingRequiredQueryParamException(START_TIME_QUERY_PARAM)
#    if not end_time:
#        raise RestMissingRequiredQueryParamException(END_TIME_QUERY_PARAM)
#    
#    return (start_time, end_time)


@safe_stats_view
def do_get_stats(request, cluster, target_type, target_id, stats_type):
    
    # FIXME: Hack to handle the old hard-coded controller id value
    if target_type == 'controller' and target_id == 'localhost':
        target_id = get_local_controller_id()
    
    # Get the time range over which we're getting the stats
    start_time, end_time = get_time_range_from_request(request)
    
    init_db_connection()
    
    if request.method == 'GET':
        window = request.GET.get(SAMPLE_WINDOW_QUERY_PARAM, 0)
        if window:
            window = convert_time_duration(window)
        if window != 0:
            window = get_closest_window_interval(int(window))
        # FIXME: Error checking on window value
                    
        data_format = request.GET.get(DATA_FORMAT_QUERY_PARAM, VALUE_DATA_FORMAT)
        # FIXME: Error checking on data_format value
        
        limit = request.GET.get(LIMIT_QUERY_PARAM)
        if limit:
            limit = int(limit)
        # FIXME: Error checking on limit value
    
        if start_time is not None and end_time is not None:
            # FIXME: Error checking on start_time and end_time values
            sample_interval = request.GET.get(SAMPLE_INTERVAL_QUERY_PARAM)
            if not sample_interval:
                # FIXME: Error checking on sample_period value
                sample_count = request.GET.get(SAMPLE_COUNT_QUERY_PARAM, DEFAULT_SAMPLE_COUNT)
                # FIXME: Error checking on sample_count value
                    
                sample_interval = (end_time - start_time) / int(sample_count)
            else:
                sample_interval = convert_time_duration(sample_interval)
            
            if sample_interval != 0:
                sample_interval = get_closest_sample_interval(sample_interval)
            
            stats_data = get_stats_data(cluster, target_type, target_id,
                stats_type, start_time, end_time, sample_interval, window, data_format, limit)
        else:
            stats_data = get_latest_stat_data(cluster, target_type, target_id, stats_type, window, data_format)
            
        response_data = simplejson.dumps(stats_data)
        response = HttpResponse(response_data, JSON_CONTENT_TYPE)
        
    elif request.method == 'DELETE':
        delete_stats_data(cluster, target_type, target_id, stats_type,
                      start_time, end_time)
        response = get_successful_response()
    else:
        raise RestInvalidMethodException()
        
    return response
    

@safe_stats_view
def do_get_stats_metadata(request, cluster, stats_type=None):
    metadata = get_stats_metadata(cluster, stats_type)
    response_data = simplejson.dumps(metadata)
    return HttpResponse(response_data, JSON_CONTENT_TYPE)


@safe_stats_view
def do_get_stats_type_index(request, cluster, target_type, target_id, stats_type=None):
    # FIXME: Hack to handle the old hard-coded controller id value
    if target_type == 'controller' and target_id == 'localhost':
        target_id = get_local_controller_id()
    init_db_connection()
    index_data = get_stats_type_index(cluster, target_type, target_id, stats_type)
    response_data = simplejson.dumps(index_data)
    return HttpResponse(response_data, JSON_CONTENT_TYPE)


@safe_stats_view
def do_get_stats_target_types(request, cluster):
    init_db_connection()
    target_type_data = get_stats_target_types(cluster)
    response_data = simplejson.dumps(target_type_data)
    return HttpResponse(response_data, JSON_CONTENT_TYPE)


@safe_stats_view
def do_get_stats_targets(request, cluster, target_type=None):
    init_db_connection()
    target_data = get_stats_targets(cluster, target_type)
    response_data = simplejson.dumps(target_data)
    return HttpResponse(response_data, JSON_CONTENT_TYPE)


@safe_stats_view
def do_put_stats(request, cluster):
    if request.method != 'PUT':
        raise RestInvalidMethodException()
    
    init_db_connection()
    
    stats_data = simplejson.loads(request.raw_post_data)
    put_stats_data(cluster, stats_data)
    
    response = get_successful_response()
    
    return response


@safe_stats_view
def do_get_events(request, cluster, node_id):
    # FIXME: Hack to handle the old hard-coded controller id value
    if node_id == 'localhost':
        node_id = get_local_controller_id()
    
    # Get the time range over which we're getting the events
    start_time, end_time = get_time_range_from_request(request)
    
    init_db_connection()
    
    if request.method == 'GET':
        include_pk_tag_param = request.GET.get(INCLUDE_PK_TAG_QUERY_PARAM, 'false')
        include_pk_tag = include_pk_tag_param.lower() == 'true'
        events_list = get_log_event_data(cluster, node_id, start_time, end_time, include_pk_tag)
        response_data = simplejson.dumps(events_list)
        response = HttpResponse(response_data, JSON_CONTENT_TYPE)
    elif request.method == 'DELETE':
        delete_log_event_data(cluster, node_id, start_time, end_time)
        response = get_successful_response()
    else:
        raise RestInvalidMethodException()
        
    return response

@safe_stats_view
def do_put_events(request, cluster):
    if request.method != 'PUT':
        raise RestInvalidMethodException()
    
    init_db_connection()
    
    events_data = simplejson.loads(request.raw_post_data)
    put_log_event_data(cluster, events_data)
    
    response = get_successful_response()
    
    return response

