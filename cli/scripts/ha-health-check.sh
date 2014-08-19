#!/bin/bash
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
# This script is invoked as part of VRRP track_script function
# The goal is to do a health check of sdnplatform through a rest API
# invocation, and if health is not good, then return with error code
#
if [ -e "/opt/sdnplatform/health-check-error-message" ]
then
  rm -f /opt/sdnplatform/health-check-error-message
fi

FAILOVER=$3

if [ -e "/opt/sdnplatform/force-one-time-health-check-failure" ]
then
  logger "$0: force-one-time-health-check-failure file exists -- returning error exit code"
  rm -f /opt/sdnplatform/force-one-time-health-check-failure
  echo "User requested failover." > /opt/sdnplatform/health-check-error-message
  exit 1
fi

if [ "$FAILOVER" ] ; then
    exit 0
fi

if [ -e "/var/run/sdnplatform-healthcheck-disabled" ]; then
   OLD=`cat /var/run/sdnplatform-healthcheck-disabled`
   NEW=`date +%s`
   if [ $NEW -gt $OLD ]; then
       # if new timestamp is greater, remove the file and do health check
       rm -f /var/run/sdnplatform-healthcheck-disabled 
   else
       # skip the health check and exit
       exit 0
   fi
fi

COUNT=$1
STEP=$2
TIMER=0
start="$(date +%s%N)"
while [ $TIMER -le $COUNT ] ; do
    curl -f -s "http://localhost:8080/wm/core/health/json"
    ERROR=$?
    if [ $ERROR -eq 0 ]; then
        break
    fi
    sleep $STEP
    TIMER=$((STEP+TIMER))
done
duration="$(($(date +%s%N)-start))"
duration="$((duration/1000000))"
logger "ha-health checker took $duration milliseconds"
if [ $ERROR -ne 0 ]; then
  logger "$0: sdnplatform health check failed with error code $ERROR"
  echo "Controller health check failed." > /opt/sdnplatform/health-check-error-message
  exit 1
fi

exit 0
