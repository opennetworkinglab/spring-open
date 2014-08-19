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

usage() {
    cat <<EOF >&2

$0 - start process in the background, wait 15 secs for port to come up

Call syntax:

 $0 -p <port> [OPTS] <cmd> [arg1] [arg2...]

OPTIONS:
   -p  <port>     wait for port <port> to listen on
   -l  <logfile>  logfile to use for nohup output
   -t  <time>     wait <time> seconds for port to show up

   -h            this help

EOF
}


PROGNAME="$0"
SHORTOPTS="hp:l:t:" 

ARGS=$(getopt $SHORTOPTS  "$@" )

if [ $? -gt 0 ]; then
    echo "Error parsing options" >&2
    exit 1
fi
timeout=15

eval set -- "$ARGS" 

while true; do 
    case $1 in 
        -h)
            usage 
            exit 0 
            ;; 
        -p)
            port="$2"
            shift
            ;;
        -l)
            log="$2"
            shift
            ;;
        -t)
            timeout="$2"
            shift
            ;;
        --) 
            shift
            break
            ;;
        -*) 
            echo "$0: error - unrecognized option $1" 1>&2
            exit 1
            ;;
        *) 
            break
            ;;
    esac
    shift
done

if [ ! "$port" -o ! "$*" ]; then
    usage
    exit 1
fi

if [ ! "$log" ]; then
    if [ "$VIRTUALENV" ]; then
        logdir="$VIRTUALENV/log"
    else
        logdir="/tmp"
    fi
    log="$logdir/$(basename $1).log"
fi

if pid=$(lsof -t -iTCP:$port -sTCP:LISTEN); then
    echo "$* already running on port $port" >&1
    exit 0
fi

echo "Launching $* (log: $log)" >&2

nohup "$@" >$log 2>&1 &
if [ $? -gt 0 ]; then 
    echo "Error: Launch of $@ failed with exit code $?" >&2 
    exit 11
fi

echo -n "Waiting for port $port..." >&2
for((i=0; i < $timeout; i++ )); do
    if pid=$(lsof -t -iTCP:$port -sTCP:LISTEN); then
        echo " OK"
        exit 0
    fi
    sleep 1
    echo -n "."
done

echo " FAIL: Launched program $@ failed to bring up port within $timeout seconds" >&2
exit 10
