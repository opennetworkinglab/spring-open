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

import json
import logging
import sys
import time
import random
import urllib2
import re
import urllib
from optparse import OptionParser, OptionGroup

DEFAULT_CONTROLLER_ID = 'localhost'

DEFAULT_BATCH = 5
DEFAULT_SAMPLING_INTERVAL = 5

STATS_CPU = 'cpu'
STATS_MEM = 'mem'
STATS_SWAP = 'swap'
STATS_OF = 'of'
STATS_LOG = 'log'

STATS = [STATS_CPU, STATS_MEM, STATS_SWAP, STATS_OF, STATS_LOG]

BASE_TIME = 1297278987000
SECONDS_CONVERT = 1000
MINUTES_CONVERT = 60 * SECONDS_CONVERT
HOURS_CONVERT = 60 * MINUTES_CONVERT
DAYS_CONVERT = 24 * HOURS_CONVERT

numSamples = 0
samplingInterval = 0
batchSize = 0
statsTypes = [] 
numSwitches = 0
switchIDs = []
seed = 0
controller = ''
logger = 0
debugLevel = logging.INFO
logfile = 'filler.log'

def initLogger():
    # create a statFiller logger
    logger = logging.getLogger("filler")
    logger.setLevel(debugLevel)

    formatter = logging.Formatter("%(asctime)s [%(name)s] %(levelname)s %(message)s")
    # Add a file handler
    rf_handler = logging.FileHandler(logfile)
    rf_handler.setFormatter(formatter)
    logger.addHandler(rf_handler)

    # Add a console handler
    co_handler = logging.StreamHandler()
    co_handler.setFormatter(formatter)
    co_handler.setLevel(logging.WARNING)
    logger.addHandler(co_handler)

    return logger

def intToDpid(intValue):
    import string
    ret = []
    if intValue > (2**128-1):
        intValue = 2**128-1
    for i in range(16):
        mask = 0x0f<<(i*4)
        tmp = (intValue&mask)>>(i*4)
        ret.insert(0,string.hexdigits[tmp])
        if i != 15 and i%2 == 1:
            ret.insert(0, ':')

    return ''.join(ret)

def getRandomInc():
    """ 
    Randomly create an integer from 1 to 100
    """
    return random.randint(1,100)

def getRandomInt(max):
    """ 
    Randomly create an integer from 1 to max
    """
    if max <= 1:
        return 1
    else:
        return random.randint(1,max)

def getRandomPercentage(max):
    """ 
    Randomly create a two-decimal percentage
    """
    percentMax = int(max * 100)
    if percentMax < 2:
        return 0.01
    else:
        try:
            v = float(random.randint(1, percentMax))
            return v/100
        except ValueError as e:
            logger.error ("error: %s, percentMax=%d"%(e, percentMax))
            return 0.01

class StatsFiller():
  
    statsData = {}
    logData = {}
    
    of_packetin = 0 
    of_flowmod = 0 
    of_activeflow = 0 
    hostID = 'localhost'

    def __init__(self, numSamples, samplingInterval, batchSize, switchIDs, statsTypes, hostID, cluster, components, seed, logger):
        self.numSamples = numSamples
        self.samplingInterval = samplingInterval
        self.batchSize = batchSize
        self.switchIDs = switchIDs
        self.statsTypes = statsTypes
        self.controllerID = hostID
        self.cluster = cluster
        self.components = components
        self.seed = seed
        self.logger = logger

    def repr(self):
        return str(self.statsData)
 
    def initStatsData(self):
        if STATS_CPU in self.statsTypes or STATS_MEM in self.statsTypes or STATS_SWAP in self.statsTypes:
            self.statsData['controller-stats'] = {}
            self.statsData['controller-stats'][self.hostID] = {}

            if STATS_CPU in self.statsTypes: 
                self.statsData['controller-stats'][self.hostID]['cpu-idle'] = []
                self.statsData['controller-stats'][self.hostID]['cpu-nice'] = []
                self.statsData['controller-stats'][self.hostID]['cpu-user'] = []
                self.statsData['controller-stats'][self.hostID]['cpu-system'] = []
            if  STATS_MEM in self.statsTypes:
                self.statsData['controller-stats'][self.hostID]['mem-used'] = []
                self.statsData['controller-stats'][self.hostID]['mem-free'] = []
            if  STATS_SWAP in self.statsTypes:
                self.statsData['controller-stats'][self.hostID]['swap-used'] = []
        
        if STATS_OF in self.statsTypes:
            self.statsData['switch-stats'] = {}
            for dpid in switchIDs:
                self.statsData['switch-stats'][dpid] = {}

            if STATS_OF in self.statsTypes: 
                for dpid in switchIDs:
                    self.statsData['switch-stats'][dpid]['OFPacketIn'] = []
                    self.statsData['switch-stats'][dpid]['OFFlowMod'] = []
                    self.statsData['switch-stats'][dpid]['OFActiveFlow'] = []
 
        if STATS_LOG in self.statsTypes:
            self.logData[self.hostID] = []
            
    def generate_a_sw_stat(self, timestamp, dpid, statsTypes, value):
        sample = {'timestamp':timestamp, 'value':value}
        self.statsData['switch-stats'][dpid][statsTypes].append(sample)

    def generate_a_controller_stat(self, timestamp, statsTypes, value):
        sample = {'timestamp':timestamp, 'value':value}
        self.statsData['controller-stats'][self.hostID][statsTypes].append(sample)

    def generate_log_event(self, timestamp, component, log_level, message):
        event = {'timestamp':timestamp, 'component':component, 'log-level':log_level,'message':message}
        self.logData[self.hostID].append(event)
        
    def generate_a_batch(self, startTime, batchSize):
        for i in range(batchSize):
            # Get the sample timestamp in ms
            ts = int(startTime + i * self.samplingInterval)*1000
            # controller stats
            if STATS_CPU in self.statsTypes:
                max = 100.00
                v = getRandomPercentage(max)
                self.generate_a_controller_stat(ts, 'cpu-idle', round(v, 2))
                max -= v
                v = getRandomPercentage(max)
                self.generate_a_controller_stat(ts, 'cpu-nice', round(v, 2))
                max -= v
                v = getRandomPercentage(max)
                self.generate_a_controller_stat(ts, 'cpu-user', round(v, 2))
                max -= v
                self.generate_a_controller_stat(ts, 'cpu-system', round(v, 2))
            if  STATS_MEM in self.statsTypes:
                max = getRandomInt(1000000000)
                v = getRandomInt(max)
                self.generate_a_controller_stat(ts, 'mem-used', v)
                max -= v
                self.generate_a_controller_stat(ts, 'mem-free', max)
            if  STATS_SWAP in self.statsTypes:
                max = getRandomInt(1000000000)
                v = getRandomInt(max)
                self.generate_a_controller_stat(ts, 'swap-used', v)
                
            # switch stats
            if STATS_OF in self.statsTypes:
                for dpid in self.switchIDs:
                    #print "add stats for %s"%dpid
                    self.of_packetin = getRandomInt(100) 
                    self.generate_a_sw_stat(ts, dpid, 'OFPacketIn', self.of_packetin)
                    self.of_flowmod = getRandomInt(100) 
                    self.generate_a_sw_stat(ts, dpid, 'OFFlowMod', self.of_flowmod)
                    self.of_activeflow = getRandomInt(100) 
                    self.generate_a_sw_stat(ts, dpid, 'OFActiveFlow', self.of_activeflow)

            if STATS_LOG in self.statsTypes:
                for component in components:
                    self.generate_log_event(ts, component, 'Error', 'Another log message')
                    
    def constructRestRrls(self):
        """
        Construct the REST URL for the given host/statsPath, including
        the items in the query_params dictionary as URL-encoded query parameters
        """
        self.statsUrl = 'http://%s:8000/rest/v1/stats/data/%s'%(self.controllerID, self.cluster)
        self.logUrl = 'http://%s:8000/rest/v1/events/data/%s'%(self.controllerID, self.cluster)

    def printRestErrorInfo(self, e):
        """
        Extract the error information and print it.
        This is mainly intended to demonstrate how to extract the
        error info from the exception. It may or may not make sense
        to print out this information, depending on the application.
        """
        # Extract the info from the exception
        error_code = e.getcode()
        response_text = e.read()
        try:
            # Print the error info
            logger.error('HTTP error: code = %d, %s'%(error_code, response_text))

            obj = json.loads(response_text)
            error_type = obj.get('error_type')
            description = obj.get('description')

            # Print the error info
            logger.error('HTTP error code = %d; error_type = %s; description = %s'%(error_code, str(error_type), description))

            # Print the optional validation error info
            model_error = obj.get('model_error')
            if model_error:
                logger.error('model_error = %s'%str(model_error))
            field_errors = obj.get('field_errors')
            if field_errors:
                logger.error('field_errors = %s'%str(field_errors))
        except ValueError as e:
            logger.error(e)


    def putRestData(self, url, obj):
        """
        Put the given object data to the given type/id/params at the given host.
        If both the id and query_param_dict are empty, then a new item is created.
        Otherwise, existing data items are updated with the object data.
        """

        logger.debug("URL: %s"%url)
        logger.debug("Sending: %s"%obj)
        request = urllib2.Request(url, obj, {'Content-Type':'application/json'})
        request.get_method = lambda: 'PUT'
        try:
            response = urllib2.urlopen(request)
            ret = response.read()
            logger.debug("Got response: %s"%str(ret))
            return ret
        except urllib2.HTTPError as e:
            logger.error("Got Exception: %s"%str(e))
            self.printRestErrorInfo(e)


    def postData(self):
        """ 
        Put the given object data to the given type/id/params at the given host.
        """

        self.constructRestRrls()

        if self.statsData:
            output = json.JSONEncoder().encode(self.statsData)
            retMsg = self.putRestData(self.statsUrl, output)
            logger.info("Put rest call for stats data returns: %s"%retMsg)
        if self.logData:
            output = json.JSONEncoder().encode(self.logData)
            retMsg = self.putRestData(self.logUrl, output)
            logger.info("Put rest call for log data returns: %s"%retMsg)
            
    def fill(self):
        endTime = time.time()
        startTime = endTime - self.numSamples * self.samplingInterval
        remainingSamples = self.numSamples 
        batchSize = 0
        while remainingSamples > 0:
            logger.info("starttime = %s(%d), endtime = %s(%d)"%(time.ctime(startTime),startTime,time.ctime(endTime),endTime))
            self.initStatsData()
            if remainingSamples < self.batchSize:
                batchSize = remainingSamples
            else:
                batchSize = self.batchSize
            remainingSamples -= batchSize
            self.generate_a_batch(startTime, batchSize)
            startTime += self.samplingInterval * batchSize
            self.postData()
            sys.stdout.write("%0.2f%%\r"%(float(self.numSamples-remainingSamples)*100/self.numSamples))

def parseLogLevel(level):
    if 'debug'.startswith(level):
        return logging.DEBUG
    elif 'info'.startswith(level):
        return logging.INFO
    elif 'warning'.startswith(level):
        return logging.WARNING
    elif 'error'.startswith(level):
        return logging.ERROR
    elif 'critical'.startswith(level):
        return logging.CRITICAL
    else:
        return None

def processOptions(options):
    """
    Process the command line arguments
    """

    global numSamples
    global samplingInterval
    global batchSize
    global statsTypes
    global numSwitches
    global switchIDs
    global seed
    global controller
    global cluster
    global components
    global debugLevel
    global logfile

    if options.numSamples:
        numSamples = options.numSamples

    if options.period:
        m = re.search("([0-9]*)([A-Za-z]*)$", options.period)
        (value, unit) = m.groups()
        if value:
            value = int(value)
        if unit:
            if 'minutes'.startswith(unit):
                value = 60*value
            elif 'hours'.startswith(unit):
                value = 60*60*value
            elif 'days'.startswith(unit):
                value = 24*60*60*value
            elif not 'seconds'.startswith(unit):
                raise ValueError("Invalid period: %s"%options.period)
        numSamples = value

    if options.sampleInterval:
        samplingInterval = options.sampleInterval
    else:
        samplingInterval = DEFAULT_SAMPLING_INTERVAL

    numSamples /= samplingInterval
    
    if options.batchSize:
        batchSize = options.batchSize
    else:
        batchSize = DEFAULT_BATCH

    if options.numSwitches:
        numSwitches = options.numSwitches

    if options.statsTypes:
        statsTypes = options.statsTypes.split(',')
        for stat in statsTypes:
            if stat not in STATS:
                raise ValueError("Invalid stat: %s"%stat)

    if options.seed:
        seed = options.seed
    else:
        seed = random.random()

    if options.controller:
        controller = options.controller
    else:
        controller = 'localhost'

    if options.cluster:
        cluster = options.cluster
    else:
        cluster = 'default'

    components = options.components.split(',')
    
    if options.debugLevel:
        debugLevel = parseLogLevel(options.debugLevel)
    else:
        debugLevel = logging.INFO

    if not debugLevel:
        raise ValueError("Incorrect debug level, %s."%options.debugLevel)

    if options.logfile:
        logfile = options.logfile
    else:
        logfile = 'filler.log'


    if len(statsTypes) == 0:
        raise ValueError("statsTypes is required.")

    if STATS_OF in statsTypes:
        if numSwitches == 0:
            raise ValueError("numSwitches must be nonzero to generate of stats.")
        else:
            for i in range(numSwitches):
                switchIDs.append(intToDpid(i))

    if numSamples == 0:
        raise ValueError("numSamples or period is required")



if __name__ == '__main__':
    parser = OptionParser()
    group = OptionGroup(parser, "Commonly Used Options")
    group.add_option("-n", "--numSamples", dest="numSamples", type="long", help="Number of samples to be generated. Can NOT be used with timePeriod option.") 
    group.add_option("-p", "--timePeriod", dest="period", help="The time period to fill the stats data. "
                            "The format can be in seconds, minutes, hours, or days. e.g. 100s(econds), 15m(inutes), 2h(ours), 3d(ays). "
                            "Can NOT be used with numSamples option.") 
    group.add_option("-t", "--samplingInterval", dest="sampleInterval", type = "int", help="The sampling interval in seconds") 
    group.add_option("-b", "--batchSize", dest="batchSize", type = "int", help="The number of samples for each rest put") 
    group.add_option("-s", "--numSwitches", dest="numSwitches", type = "int", help="The number of switches for OF stats. The dpids start with "
                                "00:00:00:00:00:00:00:01 and increment to the number of switches.") 
    group.add_option("-m", "--statsTypes", dest="statsTypes", help="A comma separated statsTypes, Options are cpu, mem, swap, of, and log." 
                            " e.g. cpu,mem")
    group.add_option("-c", "--controller", dest="controller", help="The IP address of the controller") 
    group.add_option("-u", "--cluster", dest="cluster", help="cluster ID")
    group.add_option("-z", "--components", dest="components", default="sdnplatform,cassandra", help="A comma-separated list of component names for log events")
    parser.add_option_group(group)

    lc_group = OptionGroup(parser, "Less Commonly Used Options")
    lc_group.add_option("-r", "--seed", dest="seed", type = "int", help="Same data can be recreated by setting the same seed for the randomizer") 
    lc_group.add_option("-d", "--debugLevel", dest="debugLevel", help="Set the log level for logging: debug, info, warning, critical, error") 
    lc_group.add_option("-f", "--logfile", dest="logfile", help="The logfile that keeps the logs. Default is filler.log")
    parser.add_option_group(lc_group)

    (options, args) = parser.parse_args()
    if len(args) != 0:
        parser.error("incorrect number of arguments: %s"%args)


    try:
        processOptions(options)
        logger = initLogger()
        logger.debug("numSample:%d, samplingInterval:%d, batchSize=%d, statsTypes=%s, numSwitches=%d switchIDs=%s seed=%f cluster=%s components=%s"%
                    (numSamples, samplingInterval, batchSize, statsTypes, numSwitches, switchIDs, seed, cluster, components)) 
    except ValueError as e:
        print("Error: %s"%e)
        sys.exit()

    filler = StatsFiller(numSamples, samplingInterval, batchSize, switchIDs, statsTypes, controller, cluster, components, seed, logger)
    filler.fill()
