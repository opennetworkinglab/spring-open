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

$0 - kill process listening on a port, wait for it to terminate

Call syntax:

 $0 -p <port> [OPTS]

OPTIONS:
   -p <port>     wait for port <port> to listen on
   -t <time>     wait <t> seconds for port to show up

   -h            this help

EOF
}

PROGNAME="$0"
SHORTOPTS="hp:t:"

ARGS=$(getopt $SHORTOPTS "$@" )

if [ $? -gt 0 ]; then
    echo "Error getting options" >&2
    exit 2
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

if [ ! "$port" ]; then
    usage
    exit 1
fi

for((i=0; i < 15; i++ )); do
    if ! pid=$(lsof -t -iTCP:$port -sTCP:LISTEN); then
        exit 0
    fi
    kill ${pid}
    sleep 1
done

echo "Error: Program on port $pid failed to exit" >&2
exit 10
