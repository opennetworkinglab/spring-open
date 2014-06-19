#!/bin/bash

### Env vars used by this script. (default value) ###
# $ONOS_HOME       : path of root directory of ONOS repository (this script's dir)
# $ONOS_CONF_DIR   : path of ONOS config directory (~/ONOS/conf)
# $ONOS_CONF       : path of ONOS node config file (~/ONOS/conf/onos_node.`hostname`.conf or onos_node.conf)
# $ONOS_PROPS      : path of ONOS properties file (~/ONOS/conf/onos.properties)
# $ONOS_LOGBACK    : path of logback config file (~/ONOS/conf/logback.`hostname`.xml)
# $ONOS_LOGDIR     : path of log output directory (~/ONOS/onos-logs)
# $ONOS_LOGBASE    : base name of log output file (onos.`hostname`)
# $RAMCLOUD_HOME   : path of root directory of RAMCloud repository (~/ramcloud)
# $RAMCLOUD_BRANCH : branch name of RAMCloud to use (master)
# $ZK_HOME         : path of root directory of ZooKeeper (~/zookeeper-3.4.6)
# $ZK_LIB_DIR      : path of ZooKeeper library (/var/lib/zookeeper)
# $ZK_LOG_DIR      : path of ZooKeeper log output directory (~/ONOS/onos-logs/zk-`hostname`)
# $JVM_OPTS        : JVM options ONOS starts with
# $ZK_CONF         : path of ZooKeeper config file (~/ONOS/conf/zoo.cfg)
# $HC_CONF         : path of Hazelcast config file (~/ONOS/conf/hazelcast.xml)
# $RAMCLOUD_CONF   : path of RAMCloud config file (~/ONOS/conf/ramcloud.conf)
#####################################################

ONOS_HOME=${ONOS_HOME:-$(cd `dirname $0`; pwd)}
ONOS_CONF_DIR=${ONOS_CONF_DIR:-${ONOS_HOME}/conf}
ONOS_CONF=${ONOS_CONF:-${ONOS_CONF_DIR}/onos_node.`hostname`.conf}

source ${ONOS_HOME}/scripts/common/utils.sh

confirm-if-root

if [ ! -f ${ONOS_CONF} ]; then
  # falling back to default config file
  ONOS_CONF=${ONOS_CONF_DIR}/onos_node.conf
  if [ ! -f ${ONOS_CONF} ]; then
    echo "${ONOS_CONF} not found."
    exit 1
  fi
fi

### Variables read from ONOS config file ###
ONOS_CLUSTER_NAME=$(read-conf ${ONOS_CONF}  onos.cluster.name             "onos")
ONOS_HOST_NAME=$(read-conf ${ONOS_CONF}     host.name                     `hostname`)
ONOS_HOST_IP=$(read-conf ${ONOS_CONF}       host.ip)
ONOS_HOST_ROLE=$(read-conf ${ONOS_CONF}     host.role)
ONOS_HOST_BACKEND=$(read-conf ${ONOS_CONF}  host.backend)
ZK_HOSTS=$(read-conf ${ONOS_CONF}           zookeeper.hosts               ${ONOS_HOST_NAME})
RC_COORD_PROTOCOL=$(read-conf ${ONOS_CONF}  ramcloud.coordinator.protocol "fast+udp")
RC_COORD_IP=$(read-conf ${ONOS_CONF}        ramcloud.coordinator.ip       ${ONOS_HOST_IP})
RC_COORD_PORT=$(read-conf ${ONOS_CONF}      ramcloud.coordinator.port     12246)
RC_SERVER_PROTOCOL=$(read-conf ${ONOS_CONF} ramcloud.server.protocol      "fast+udp")
RC_SERVER_IP=$(read-conf ${ONOS_CONF}       ramcloud.server.ip            ${ONOS_HOST_IP})
RC_SERVER_PORT=$(read-conf ${ONOS_CONF}     ramcloud.server.port          12242)
HC_HOST_PORT=$(read-conf ${ONOS_CONF}       hazelcast.host.port           5701)
HC_TCPIP_MEMBERS=$(read-conf ${ONOS_CONF}   hazelcast.tcp-ip.members)
HC_MULTICAST_GROUP=$(read-conf ${ONOS_CONF} hazelcast.multicast.group     "224.2.2.3")
HC_MULTICAST_PORT=$(read-conf ${ONOS_CONF}  hazelcast.multicast.port      54327)
############################################


############## Other variables #############
ONOS_TEMPLATE_DIR=${ONOS_CONF_DIR}/template

LOGDIR=${ONOS_LOGDIR:-${ONOS_HOME}/onos-logs}

ZK_HOME=${ZK_HOME:-~/zookeeper-3.4.6}
ZK_CONF=${ZK_CONF:-${ONOS_CONF_DIR}/zoo.cfg}
ZK_CONF_TEMPLATE=${ONOS_TEMPLATE_DIR}/zoo.cfg.template
# Adding ONOS_HOST_NAME dir since file name (zookeeper.out) cannot be controlled.
ZK_LOG_DIR=${ZK_LOG_DIR:-${ONOS_HOME}/onos-logs/zk-${ONOS_HOST_NAME}}
ZK_LIB_DIR=${ZK_LIB_DIR:-/var/lib/zookeeper}
ZK_MY_ID=${ZK_LIB_DIR}/myid

HC_CONF=${HC_CONF:-${ONOS_CONF_DIR}/hazelcast.xml}
HC_CONF_TEMPLATE=${ONOS_TEMPLATE_DIR}/hazelcast.xml.template

RAMCLOUD_HOME=${RAMCLOUD_HOME:-~/ramcloud}
RAMCLOUD_COORD_LOG=${LOGDIR}/ramcloud.coordinator.${ONOS_HOST_NAME}.log
RAMCLOUD_SERVER_LOG=${LOGDIR}/ramcloud.server.${ONOS_HOST_NAME}.log
RAMCLOUD_BRANCH=${RAMCLOUD_BRANCH:-master}
RAMCLOUD_CONF=${RAMCLOUD_CONF:-${ONOS_CONF_DIR}/ramcloud.conf}

export LD_LIBRARY_PATH=${ONOS_HOME}/lib:${RAMCLOUD_HOME}/obj.${RAMCLOUD_BRANCH}:$LD_LIBRARY_PATH

## Because the script change dir to $ONOS_HOME, we can set ONOS_LOGBACK and LOGDIR relative to $ONOS_HOME
ONOS_LOGBACK=${ONOS_LOGBACK:-${ONOS_CONF_DIR}/logback.${ONOS_HOST_NAME}.xml}
ONOS_LOGBACK_BACKUP=${ONOS_LOGBACK}.bak
ONOS_LOGBACK_TEMPLATE=${ONOS_TEMPLATE_DIR}/logback.xml.template
LOGDIR=${ONOS_LOGDIR:-${ONOS_HOME}/onos-logs}
LOGBASE=${ONOS_LOGBASE:-onos.${ONOS_HOST_NAME}}
ONOS_LOG="${LOGDIR}/${LOGBASE}.log"
ONOS_LOG_ROLLING_PATTERN="${LOGDIR}/${LOGBASE}.%i.log.gz"
ONOS_STDOUT_LOG="${LOGDIR}/${LOGBASE}.stdout"
ONOS_STDERR_LOG="${LOGDIR}/${LOGBASE}.stderr"
PCAP_LOG="${LOGDIR}/${LOGBASE}.pcap"
LOGS="$ONOS_LOG $ONOS_STDOUT_LOG $ONOS_STDERR_LOG $PCAP_LOG"

ONOS_PROPS=${ONOS_PROPS:-${ONOS_CONF_DIR}/onos.properties}
JMX_PORT=${JMX_PORT:-7189}

# Set JVM options
JVM_OPTS="${JVM_OPTS:-}"
JVM_OPTS="$JVM_OPTS -server -d64"
#JVM_OPTS="$JVM_OPTS -XX:+TieredCompilation -XX:InitialCodeCacheSize=512m -XX:ReservedCodeCacheSize=512m"

# Uncomment or specify appropriate value as JVM_OPTS environment variables.
#JVM_OPTS="$JVM_OPTS -Xmx4g -Xms4g -Xmn800m"
#JVM_OPTS="$JVM_OPTS -Xmx2g -Xms2g -Xmn800m"
#JVM_OPTS="$JVM_OPTS -Xmx1g -Xms1g -Xmn800m"

#JVM_OPTS="$JVM_OPTS -XX:+UseParallelGC"
JVM_OPTS="$JVM_OPTS -XX:+UseConcMarkSweepGC"
JVM_OPTS="$JVM_OPTS -XX:+AggressiveOpts"

# We may want to remove UseFastAccessorMethods option: http://bugs.java.com/view_bug.do?bug_id=6385687
JVM_OPTS="$JVM_OPTS -XX:+UseFastAccessorMethods"

JVM_OPTS="$JVM_OPTS -XX:MaxInlineSize=8192"
JVM_OPTS="$JVM_OPTS -XX:FreqInlineSize=8192"
JVM_OPTS="$JVM_OPTS -XX:CompileThreshold=1500"

JVM_OPTS="$JVM_OPTS -XX:OnError=crash-logger" ;# For dumping core

# Workaround for Thread Priority http://tech.stolsvik.com/2010/01/linux-java-thread-priorities-workaround.html
JVM_OPTS="$JVM_OPTS -XX:+UseThreadPriorities -XX:ThreadPriorityPolicy=42"

JVM_OPTS="$JVM_OPTS -XX:+UseCompressedOops"

JVM_OPTS="$JVM_OPTS -Dcom.sun.management.jmxremote.port=$JMX_PORT"
JVM_OPTS="$JVM_OPTS -Dcom.sun.management.jmxremote.ssl=false"
JVM_OPTS="$JVM_OPTS -Dcom.sun.management.jmxremote.authenticate=false"

JVM_OPTS="$JVM_OPTS -Dhazelcast.logging.type=slf4j"

# Uncomment to dump final JVM flags to stdout
#JVM_OPTS="$JVM_OPTS -XX:+PrintFlagsFinal"

# Set ONOS core main class
MAIN_CLASS="net.onrc.onos.core.main.Main"

MVN=${MVN:-mvn -o}
############################################


############# Common functions #############
function print_usage {
  local scriptname=`basename $0`
  local filename=`basename ${ONOS_CONF}`
  local usage="Usage: setup/start/stop ONOS on this server.
 \$ ${scriptname} setup [-f]
    Set up ONOS node using ${ONOS_CONF} .
      - generate and replace config file of ZooKeeper.
      - create myid in ZooKeeper datadir.
      - generate and replace config file for Hazelcast.
      - generate and replace config file for RAMCloud.
      - generate and replace logback.${ONOS_HOST_NAME}.xml
    If -f option is used, all existing files will be overwritten without confirmation.
 \$ ${scriptname} start [single-node|coord-node|server-node|coord-and-server-node]
    Start ONOS node with specific RAMCloud entities
      - single-node: start ONOS with stand-alone RAMCloud
      - coord-node : start ONOS with RAMCloud coordinator
      - server-node: start ONOS with RAMCloud server
      - coord-and-server-node: start ONOS with RAMCloud coordinator and server
      * Default behavior can be defined by ${filename}
 \$ ${scriptname} stop
    Stop all ONOS-related processes
 \$ ${scriptname} restart
    Stop and start currently running ONOS-related processes
 \$ ${scriptname} status
    Show status of ONOS-related processes
 \$ ${scriptname} {zk|rc-coord|rc-server|core} {start|stop|restart|status}
    Control specific ONOS-related process
 \$ ${scriptname} rc deldb
    Delete data in RAMCloud"
  
  echo "${usage}"
}

# rotate-log [log-filename] [max rotations]
# Example:
#  foobar.log -> foobar.log.1
#  foobar.log.1 -> foobar.log.2
#  foobar.log.gz -> foobar.log.1.gz
function rotate-log {
  local logfile=$1
  local nr_max=${2:-10}
  if [ -f $logfile ]; then
    # TODO treating only .gz now. probably want more generic solution
    local basename=${logfile%%.gz}
    local append=""
    if [ "$basename" != "$logfile" ]; then
      append=".gz"
    fi
    for i in `seq $(expr $nr_max - 1) -1 1`; do
      if [ -f ${basename}.${i}${append} ]; then
        mv -f ${basename}.${i}${append} ${basename}.`expr $i + 1`${append}
      fi
    done
    mv ${basename}${append} ${basename}.1${append}
  fi
}

# kill-processes {module-name} {array of pids}
function kill-processes {
  # Kill the existing processes
  local pids=$2
  if [ ! -z "$pids" ]; then
    echo -n "Stopping $1 ... "
  fi
  for p in ${pids}; do
    if [ x$p != "x" ]; then
      # Check if target process is accesible from current user
      kill -0 $p
      if [ "$?" -ne 0 ]; then
        # Error exit code here means "failed to send signal".
        # Supposedly because of permission error.
        echo "Failed to kill process (pid: $p)."
        echo "Check if current user is the same as process owner."
        exit 1
      fi
      
      (
        # Ask process with SIGTERM first, if that did not kill the process
        # wait 1s and if process still exist, force process to be killed.
        kill -TERM $p && kill -0 $p && sleep 1 && kill -0 $p && kill -KILL $p
      ) 2> /dev/null
      
      # Ensure process is killed.
      kill -0 $p 2> /dev/null
      if [ "$?" -ne 0 ]; then
        # Error exit code here means "process not found", i.e. "kill succeeded".
        echo "Killed existing process (pid: $p)"
      else
        # Process still exists. Some unexpected error occurs.
        echo "Failed to kill process (pid: $p)."
        echo "Unexpected error occurs during process termination."
        exit 1
      fi
    fi
  done
}

function handle-error {
  set -e
  
  revert-confs
  
  set +e
  
  exit 1
}

# revert-confs [error message]
function revert-confs {
  echo -n "ERROR occurred ... "
  
  revert-file `basename ${ZK_CONF}`
  revert-file `basename ${HC_CONF}`

  echo "EXIT"
  
  if [ ! -z "$1" ]; then
    echo $1
  fi
}

function create-zk-conf {
  echo -n "Creating ${ZK_CONF} ... "
  
  # Create the ZooKeeper lib directory
  if [[ ! ( -w ${ZK_LIB_DIR} && -d ${ZK_LIB_DIR} ) ]]; then
      local SUDO=${SUDO:-sudo}
      local whoami=`whoami`
      {
          ${SUDO} mkdir -p ${ZK_LIB_DIR}
          ${SUDO} chown ${whoami} ${ZK_LIB_DIR}
      } || {
          echo "FAILED"
          echo "[ERROR] Failed to create directory ${ZK_LIB_DIR}."
          echo "[ERROR] Please retry after setting \"env SUDO=sudo\""
          exit 1
      }
  fi

  # creation of ZooKeeper config
  local temp_zk=`begin-conf-creation ${ZK_CONF}`
  
  hostarr=`echo ${ZK_HOSTS} | tr "," " "`
  
  local i=1
  local myid=
  for host in ${hostarr}; do
    if [ "${host}" = "${ONOS_HOST_NAME}" -o "${host}" = "${ONOS_HOST_IP}" ]; then
      myid=$i
      break
    fi
    i=`expr $i + 1`
  done
  
  if [ -z "${myid}" ]; then
    local filename=`basename ${ONOS_CONF}`
    revert-confs "[ERROR] In ${filename}, zookeeper.hosts must have hostname \"${ONOS_HOST_NAME}\" or IP address"
  fi
  
  if [ -f "${ZK_MY_ID}" ]; then
    # sudo will be needed if ZK_MY_ID is already created by other (old) script
    local SUDO=${SUDO:-}
    {
      ${SUDO} mv -f ${ZK_MY_ID} ${ZK_MY_ID}.old
    } || {
      echo "FAILED"
      echo "[ERROR] Failed to rename ${ZK_MY_ID}."
      echo "[ERROR] Please retry after setting \"env SUDO=sudo\""
      exit 1
    }
  fi
  
  echo ${myid} > ${ZK_MY_ID}
  
  echo -n "myid is assigned to ${myid} ... "
  
  while read line; do
    if [[ $line =~ ^__HOSTS__$ ]]; then
      i=1
      for host in ${hostarr}; do
        # TODO: ports might be configurable
        local hostline="server.${i}=${host}:2888:3888"
        echo $hostline
        i=`expr $i + 1`
      done
    elif [[ $line =~ __DATADIR__ ]]; then
      echo $line | sed -e "s|__DATADIR__|${ZK_LIB_DIR}|"
    else
      echo $line
    fi
  done < ${ZK_CONF_TEMPLATE} > ${temp_zk}
  
  end-conf-creation ${ZK_CONF}
  
  echo "DONE"
}

function create-hazelcast-conf {
  echo -n "Creating ${HC_CONF} ... "
  
  local temp_hc=`begin-conf-creation ${HC_CONF}`
  
  # To keep indent of XML file, change IFS
  local IFS=''
  while read line; do
    if [[ $line =~ __HC_NETWORK__ ]]; then
      if [ ! -z "${HC_TCPIP_MEMBERS}" ]; then
        # temporary change
        IFS=' '
        local memberarr=`echo ${HC_TCPIP_MEMBERS} | tr "," " "`
        echo '<multicast enabled="false" />'
        echo '<tcp-ip enabled="true">'
        for member in ${memberarr}; do
          echo "  <member>${member}</member>"
        done
        echo '</tcp-ip>'
        IFS=''
      else
        echo '<multicast enabled="true">'
        echo "  <multicast-group>${HC_MULTICAST_GROUP}</multicast-group>"
        echo "  <multicast-port>${HC_MULTICAST_PORT}</multicast-port>"
        echo '</multicast>'
        echo '<tcp-ip enabled="false" />'
      fi
    elif [[ $line =~ __HC_PORT__ ]]; then
      echo $line | sed -e "s|__HC_PORT__|${HC_HOST_PORT}|"
    elif [[ $line =~ __HC_CLUSTER__ ]]; then
      echo $line | sed -e "s|__HC_CLUSTER__|${ONOS_CLUSTER_NAME}|"
    else
      echo "${line}"
    fi
  done < ${HC_CONF_TEMPLATE} > ${temp_hc}
  
  end-conf-creation ${HC_CONF}
  
  echo "DONE"
}

function create-ramcloud-conf {
  echo -n "Creating ${RAMCLOUD_CONF} ... "

  local temp_rc=`begin-conf-creation ${RAMCLOUD_CONF}`

  local rc_cluster_name=$(read-conf ${ONOS_CONF} ramcloud.clusterName ${ONOS_CLUSTER_NAME})

  # TODO make ZooKeeper address configurable.
  echo "ramcloud.locator=zk:localhost:2181" > ${temp_rc}
  echo "#ramcloud.locator=zk:localhost:2181,otherhost:2181" >> ${temp_rc}
  echo "ramcloud.clusterName=${rc_cluster_name}" >> ${temp_rc}

  end-conf-creation ${RAMCLOUD_CONF}

  echo "DONE"
}

function create-logback-conf {
  echo -n "Creating ${ONOS_LOGBACK} ... "
  
  # creation of logback config
  local temp_lb=`begin-conf-creation ${ONOS_LOGBACK}`

  sed -e "s|__FILENAME__|${ONOS_LOG}|" \
      -e "s|__ROLLING_PATTERN__|${ONOS_LOG_ROLLING_PATTERN}|" ${ONOS_LOGBACK_TEMPLATE} > ${temp_lb}
  
  end-conf-creation ${ONOS_LOGBACK}
  
  echo "DONE"
}

function create-confs {
  local key
  local filename
  
  trap handle-error ERR

  echo "Config file : ${ONOS_CONF}"
  
  if [ "$1" == "-f" ]; then
    create-zk-conf
    create-hazelcast-conf
    create-ramcloud-conf
    create-logback-conf
  else
    create-conf-interactive ${ZK_CONF} create-zk-conf
    create-conf-interactive ${HC_CONF} create-hazelcast-conf
    create-conf-interactive ${RAMCLOUD_CONF} create-ramcloud-conf
    create-conf-interactive ${ONOS_LOGBACK} create-logback-conf
  fi
  
  trap - ERR
}
############################################


###### Functions related to ZooKeeper ######
function zk {
  case "$1" in
    start)
      start-zk
      ;;
    stop)
      stop-zk
      ;;
    stat*) # <- status
      status-zk
      ;;
    re*)   # <- restart
      stop-zk
      start-zk
      ;;
    *)
      print_usage
      exit 1
  esac
}

function load-zk-cfg {
  if [ -f "${ZK_CONF}" ]; then
    local filename=`basename ${ZK_CONF}`
    local dirname=`dirname ${ZK_CONF}`
    
    # Run ZooKeeper with our configuration
    export ZOOCFG=${filename}
    export ZOOCFGDIR=${dirname}
  fi
}

function start-zk {
  echo -n "Starting ZooKeeper ... "
  
  export ZOO_LOG_DIR=${ZK_LOG_DIR}
  mkdir -p ${ZK_LOG_DIR}
  
  load-zk-cfg

  # log4j.properties is read from classpath if not found in CWD.
  # Using the ZooKeeper supplied default in ZooKeeper conf dir.
  # TODO: To explicitly specify our customized log4j config file:
  #  export SERVER_JVMFLAGS="-Dlog4j.configuration=${ZK_LOG4J}"
  env CLASSPATH="${ZK_HOME}/conf:${CLASSPATH}" ${ZK_HOME}/bin/zkServer.sh start
}

function stop-zk {
  echo -n "Stopping ZooKeeper ... "
  load-zk-cfg

  ${ZK_HOME}/bin/zkServer.sh stop
}

function status-zk {
  load-zk-cfg
  
  ${ZK_HOME}/bin/zkServer.sh status
}

function check-zk {
  # assumption here is that ZK status script is the last command in status-zk.
  status-zk &> /dev/null
  local zk_status=$?
  if [ "$zk_status" -ne 0 ]; then
    return 1;
  fi
  return 0
}

function check-and-start-zk {
  check-zk
  local zk_status=$?
  if [ "$zk_status" -ne 0 ]; then
    start-zk
  fi
}

# wait-zk-or-die {timeout-sec}
function wait-zk-or-die {
  local retries=${1:-5}
  # do-while retries >= 0
  while true; do
    check-zk
    local zk_status=$?
    if [ "$zk_status" -eq 0 ]; then
      return 0
    fi
    sleep 1;
    ((retries -= 1))
    (( retries >= 0 )) || break
  done
  echo "ZooKeeper is not running."
  exit 1
}

############################################


####### Functions related to RAMCloud ######

# Check if this host should be running RAMCloud coordinator:
function is-coord-role {
  if [ "${ONOS_HOST_BACKEND}" = "ramcloud" ]; then
    case "${ONOS_HOST_ROLE}" in
      single-node)
        echo "true"
        return 0
        ;;
      coord-node)
        echo "true"
        return 0
        ;;
      server-node)
        echo "false"
        return 1
        ;;
      coord-and-server-node)
        echo "true"
        return 0
        ;;
      *)
        echo "false"
        return 1
        ;;
    esac
  else
    echo "false"
    return 1
  fi
}

# Check if this host should be running RAMCloud server:
function is-server-role {
  if [ "${ONOS_HOST_BACKEND}" = "ramcloud" ]; then
    case "${ONOS_HOST_ROLE}" in
      single-node)
        echo "true"
        return 0
        ;;
      coord-node)
        echo "false"
        return 1
        ;;
      server-node)
        echo "true"
        return 0
        ;;
      coord-and-server-node)
        echo "true"
        return 0
        ;;
      *)
        echo "false"
        return 1
        ;;
    esac
  else
    echo "false"
    return 1
  fi
}

function start-backend {
  if [ "${ONOS_HOST_BACKEND}" = "ramcloud" ]; then
    if [ $1 == "coord" ]; then
      rc-coord startifdown
    elif [ $1 == "server" ]; then
      rc-server startifdown
    fi
  fi
}

function stop-backend {
  rcsn=`pgrep -f obj.${RAMCLOUD_BRANCH}/server | wc -l`
  if [ $rcsn != 0 ]; then
    rc-server stop
  fi
  
  rccn=`pgrep -f obj.${RAMCLOUD_BRANCH}/coordinator | wc -l`
  if [ $rccn != 0 ]; then
    rc-coord stop
  fi
}


### Functions related to RAMCloud coordinator
function rc-coord-addr {
  local host=${RC_COORD_IP}
  if [ -z "${host}" ]; then
    # falling back to 0.0.0.0
    host="0.0.0.0"
  fi
  echo "${RC_COORD_PROTOCOL}:host=${host},port=${RC_COORD_PORT}"
}

function rc-server-addr {
  local host=${RC_SERVER_IP}
  if [ -z "${host}" ]; then
    # falling back to 0.0.0.0
    host="0.0.0.0"
  fi
  echo "${RC_SERVER_PROTOCOL}:host=${host},port=${RC_SERVER_PORT}"
}

function rc-coord {
  case "$1" in
    start)
      stop-coord
      start-coord
      ;;
    startifdown)
      local n=`pgrep -f obj.${RAMCLOUD_BRANCH}/coordinator | wc -l`
      if [ $n == 0 ]; then
        start-coord
      else
        echo "$n instance of RAMCloud coordinator running"
      fi
      ;;
    stop)
      stop-coord
      ;;
    deldb)
      stop-backend
      del-coord-info
      ;;
    stat*) # <- status
      local n=`pgrep -f obj.${RAMCLOUD_BRANCH}/coordinator | wc -l`
      echo "$n RAMCloud coordinator running"
      ;;
    *)
      print_usage
      exit 1
  esac
}

function start-coord {
  check-and-start-zk
  wait-zk-or-die

  if [ ! -d ${LOGDIR} ]; then
    mkdir -p ${LOGDIR}
  fi
  if [ -f $RAMCLOUD_COORD_LOG ]; then
    rotate-log $RAMCLOUD_COORD_LOG
  fi
  
  local coord_addr=`rc-coord-addr`

  # TODO Make ONOS_CONF readable from ONOS process then eliminate RAMCLOUD_CONF

  # Configuration for ZK address, port
  local rc_locator=$(read-conf ${RAMCLOUD_CONF} ramcloud.locator "zk:localhost:2181")

  # RAMCloud cluster name
  local rc_cluster_name=$(read-conf ${RAMCLOUD_CONF} ramcloud.clusterName ${ONOS_CLUSTER_NAME})

  # RAMCloud transport timeout
  local rc_timeout=$(read-conf ${ONOS_CONF} ramcloud.timeout 1000)
  # RAMCloud option deadServerTimeout
  # (note RC default is 250ms, setting relaxed ONOS default to 1000ms)
  local rc_coord_deadServerTimeout=$(read-conf ${ONOS_CONF} ramcloud.coordinator.deadServerTimeout 1000)

  # NOTE RAMCloud document suggests to use -L to specify listen address:port,
  #      but actual RAMCloud code only uses -C argument now.
  #      (FYI: -C is documented to be deprecated in the document)

  local coord_args="-C ${coord_addr}"
  coord_args="${coord_args} --externalStorage ${rc_locator}"
  coord_args="${coord_args} --clusterName ${rc_cluster_name}"
  coord_args="${coord_args} --timeout ${rc_timeout}"
  coord_args="${coord_args} --deadServerTimeout ${rc_coord_deadServerTimeout}"

  # Read environment variables if set
  coord_args="${coord_args} ${RC_COORDINATOR_OPTS}"

  if [ "${ONOS_HOST_ROLE}" == "single-node" ]; then
    # Note: Following reset is required, since RC restart is considered node failure,
    # and tries recovery of server, which will never succeed after restart.
    echo "Role configured to single-node mode. RAMCloud cluster will be reset on each start-up."
    coord_args="${coord_args} --reset"
  fi

  # Run ramcloud 
  echo -n "Starting RAMCloud coordinator ... "
  ${RAMCLOUD_HOME}/obj.${RAMCLOUD_BRANCH}/coordinator ${coord_args} > $RAMCLOUD_COORD_LOG 2>&1 &
  echo "STARTED"
}

function del-coord-info {
  check-and-start-zk
  wait-zk-or-die

  if [ ! -d ${LOGDIR} ]; then
    mkdir -p ${LOGDIR}
  fi
  if [ -f $RAMCLOUD_COORD_LOG ]; then
    rotate-log $RAMCLOUD_COORD_LOG
  fi

  local coord_addr=`rc-coord-addr`

  # Configuration for ZK address, port
  local rc_locator=$(read-conf ${RAMCLOUD_CONF} ramcloud.locator "zk:localhost:2181")
  # RAMCloud cluster name
  local rc_cluster_name=$(read-conf ${RAMCLOUD_CONF} ramcloud.clusterName ${ONOS_CLUSTER_NAME})
  # RAMCloud option deadServerTimeout
  # (note RC default is 250ms, setting relaxed ONOS default to 1000ms)
  local rc_coord_deadServerTimeout=$(read-conf ${ONOS_CONF} ramcloud.coordinator.deadServerTimeout 1000)

  # NOTE RAMCloud document suggests to use -L to specify listen address:port,
  #      but actual RAMCloud code only uses -C argument now.
  #      (FYI: -C is documented to be deprecated in the document)

  local coord_args="-C ${coord_addr}"
  coord_args="${coord_args} --externalStorage ${rc_locator}"
  coord_args="${coord_args} --clusterName ${rc_cluster_name}"

  # Note: --reset will reset ZK stored info and start running as acoordinator.
  echo -n "Deleting RAMCloud cluster coordination info ... "
  ${RAMCLOUD_HOME}/obj.${RAMCLOUD_BRANCH}/coordinator ${coord_args} --reset &> $RAMCLOUD_COORD_LOG &

  # TODO Assuming 5 sec is enough. To be sure monitor log?
  sleep 5
  # kill coordinator
  (pkill -f ${RAMCLOUD_HOME}/obj.${RAMCLOUD_BRANCH}/coordinator &> /dev/null)

  echo "DONE"
}

function stop-coord {
  kill-processes "RAMCloud coordinator" `pgrep -f ${RAMCLOUD_HOME}/obj.${RAMCLOUD_BRANCH}/coordinator`
}

### Functions related to RAMCloud server
function rc-server {
  case "$1" in
    start)
      stop-server
      start-server
      ;;
    startifdown)
      local n=`pgrep -f ${RAMCLOUD_HOME}/obj.${RAMCLOUD_BRANCH}/server | wc -l`
      if [ $n == 0 ]; then
        start-server
      else
        echo "$n instance of RAMCloud server running"
      fi
      ;;
    stop)
      stop-server
      ;;
    deldb)
      stop-server
      del-server-backup
      ;;
    stat*) # <- status
      n=`pgrep -f ${RAMCLOUD_HOME}/obj.${RAMCLOUD_BRANCH}/server | wc -l`
      echo "$n RAMCloud server running"
      ;;
    *)
      print_usage
      exit 1
  esac
}

function start-server {
  check-and-start-zk
  wait-zk-or-die

  if [ ! -d ${LOGDIR} ]; then
    mkdir -p ${LOGDIR}
  fi
  if [ -f $RAMCLOUD_SERVER_LOG ]; then
    rotate-log $RAMCLOUD_SERVER_LOG
  fi
  
  local coord_addr=`rc-coord-addr`
  local server_addr=`rc-server-addr`

  local masterServiceThreads=$(read-conf ${ONOS_CONF} ramcloud.server.masterServiceThreads 5)
  local logCleanerThreads=$(read-conf ${ONOS_CONF}    ramcloud.server.logCleanerThreads    1)
  local detectFailures=$(read-conf ${ONOS_CONF}       ramcloud.server.detectFailures       0)

  # Configuration for ZK address, port
  local rc_locator=$(read-conf ${RAMCLOUD_CONF} ramcloud.locator "zk:localhost:2181")
  # RAMCloud cluster name
  local rc_cluster_name=$(read-conf ${RAMCLOUD_CONF} ramcloud.clusterName ${ONOS_CLUSTER_NAME})
  # RAMCloud transport timeout
  local rc_timeout=$(read-conf ${ONOS_CONF} ramcloud.timeout 1000)
  # replication factor (-r) config
  local rc_replicas=$(read-conf ${ONOS_CONF} ramcloud.server.replicas 0)
  # backup file path (-f) config
  local rc_datafile=$(read-conf ${ONOS_CONF} ramcloud.server.file "/var/tmp/ramclouddata/backup.${ONOS_HOST_NAME}.log")
  mkdir -p `dirname ${rc_datafile}`

  local server_args="-L ${server_addr}"
  server_args="${server_args} --externalStorage ${rc_locator}"
  server_args="${server_args} --clusterName ${rc_cluster_name}"
  server_args="${server_args} --timeout ${rc_timeout}"
  server_args="${server_args} --masterServiceThreads ${masterServiceThreads}"
  server_args="${server_args} --logCleanerThreads ${logCleanerThreads}"
  server_args="${server_args} --detectFailures ${detectFailures}"
  server_args="${server_args} --replicas ${rc_replicas}"
  server_args="${server_args} --file ${rc_datafile}"

  # Read environment variables if set
  server_args="${server_args} ${RC_SERVER_OPTS}"

  # Run ramcloud
  echo -n "Starting RAMCloud server ... "
  ${RAMCLOUD_HOME}/obj.${RAMCLOUD_BRANCH}/server ${server_args} > $RAMCLOUD_SERVER_LOG 2>&1 &
  echo "STARTED"
}

function del-server-backup {
  echo -n "Delete RAMCloud backup server data [y/N]? "
  while [ 1 ]; do
    read key
    if [ "${key}" == "Y" -o "${key}" == "y" ]; then
      break
    elif [ -z "${key}" -o "${key}" == "N" -o "${key}" == "n" ]; then
      echo "Cancelled."
      return
    fi
    echo "[y/N]? "
  done

  echo -n "Removing RAMCloud backup server data ... "
  local rc_datafile=$(read-conf ${ONOS_CONF} ramcloud.server.file "/var/tmp/ramclouddata/backup.${ONOS_HOST_NAME}.log")
  rm -f ${rc_datafile}
  echo "DONE"
}

function stop-server {
  kill-processes "RAMCloud server" `pgrep -f ${RAMCLOUD_HOME}/obj.${RAMCLOUD_BRANCH}/server`
}
############################################


## Functions related to ONOS core process ##
function onos {
  CPFILE=${ONOS_HOME}/.javacp.${ONOS_HOST_NAME}
  if [ ! -f ${CPFILE} ]; then
    echo "ONOS core needs to be built"
    ${MVN} -f ${ONOS_HOME}/pom.xml compile
  fi
  JAVA_CP=`cat ${CPFILE}`
  JAVA_CP="${JAVA_CP}:${ONOS_HOME}/target/classes"

  case "$1" in
    start)
      stop-onos
      start-onos
      ;;
    startnokill)
      start-onos
      ;;
    startifdown)
      n=`jps -l | grep "${MAIN_CLASS}" | wc -l`
      if [ $n == 0 ]; then
        start-onos
      else
        echo "$n instance of onos running"
      fi
      ;;
    stop)
      stop-onos
      ;;
    stat*) # <- status
      n=`jps -l | grep "${MAIN_CLASS}" | wc -l`
      echo "$n instance of onos running"
      ;;
    *)
      print_usage
      exit 1
  esac
}

function start-onos {
  if [ ! -d ${LOGDIR} ]; then
    mkdir -p ${LOGDIR}
  fi
  # Rotate log files
  for log in ${LOGS}; do
    if [ -f ${log} ]; then
      rotate-log ${log}
    fi
  done

  # Rotate logs rolled at runtime.
  local rolled_log_shellpat=`echo ${ONOS_LOG_ROLLING_PATTERN} | sed -e "s/%i/\\*/"`
  for rolled_log in ${rolled_log_shellpat}; do
    if [ -f ${rolled_log} ]; then
      rotate-log ${rolled_log}
    fi
  done

  if [ ! -f ${ONOS_LOGBACK} ]; then
    echo "[WARNING] ${ONOS_LOGBACK} not found."
    echo "          Run \"\$ $0 setup\" to create."
    exit 1
  fi

  if [ ! -f ${HC_CONF} ]; then
    echo "[WARNING] ${HC_CONF} not found."
    echo "          Run \"\$ $0 setup\" to create."
    exit 1
    fi

  # specify ZooKeeper(curator) namespace
  JVM_OPTS="${JVM_OPTS} -Dzookeeper.namespace=${ONOS_CLUSTER_NAME}"

  # specify hazelcast.xml to datagrid
  JVM_OPTS="${JVM_OPTS} -Dnet.onrc.onos.core.datagrid.HazelcastDatagrid.datagridConfig=${HC_CONF}"

  # specify backend config
  JVM_OPTS="${JVM_OPTS} -Dnet.onrc.onos.core.datastore.backend=${ONOS_HOST_BACKEND}"
  if [ "${ONOS_HOST_BACKEND}" = "ramcloud" ]; then
    JVM_OPTS="${JVM_OPTS} -Dramcloud.config.path=${RAMCLOUD_CONF}"
  elif [ "${ONOS_HOST_BACKEND}" = "hazelcast" ]; then
    JVM_OPTS="${JVM_OPTS} -Dnet.onrc.onos.core.datastore.hazelcast.baseConfig=${HC_CONF}"
  fi

  # Run ONOS

  # Need to cd ONOS_HOME. onos.properties currently specify hazelcast config path relative to CWD
  cd ${ONOS_HOME}

  echo -n "Starting ONOS controller ... "
  java ${JVM_OPTS} -Dlogback.configurationFile=${ONOS_LOGBACK} -cp ${JAVA_CP} ${MAIN_CLASS} -cf ${ONOS_PROPS} > ${LOGDIR}/${LOGBASE}.stdout 2>${LOGDIR}/${LOGBASE}.stderr &
  
  # We need to wait a bit to find out whether starting the ONOS process succeeded
  sleep 1
  
  n=`jps -l |grep "${MAIN_CLASS}" | wc -l`
  if [ $n -ge 1 ]; then
    echo " STARTED"
  else
    echo " FAILED"
  fi

#  echo "java ${JVM_OPTS} -Dlogback.configurationFile=${ONOS_LOGBACK} -jar ${ONOS_JAR} -cf ./onos.properties > /dev/null 2>&1 &"
#  sudo -b /usr/sbin/tcpdump -n -i eth0 -s0 -w ${PCAP_LOG} 'tcp port 6633' > /dev/null  2>&1
}

function stop-onos {
  kill-processes "ONOS controller" `jps -l | grep ${MAIN_CLASS} | awk '{print $1}'`
#  kill-processes "tcpdump" `ps -edalf |grep tcpdump |grep ${PCAP_LOG} | awk '{print $4}'`
}
############################################


################## Main ####################
case "$1" in
  setup)
    create-confs $2
    ;;
  start)
    mode_parameter=${ONOS_HOST_ROLE}
    if [ ! -z "$2" ]; then
      mode_parameter=$2
    fi
    
    case "${mode_parameter}" in
      single-node)
        zk start
        start-backend coord
        start-backend server
        onos startifdown
        ;;
      coord-node)
        zk start
        start-backend coord
        onos startifdown
        ;;
      server-node)
        zk start
        start-backend server
        onos startifdown
        ;;
      coord-and-server-node)
        zk start
        start-backend coord
        start-backend server
        onos startifdown
        ;;
      *)
        print_usage
        ;;
      esac
    echo
    ;;
  stop)
    on=`jps -l | grep "${MAIN_CLASS}" | wc -l`
    if [ $on != 0 ]; then
      onos stop
    fi
    
    stop-backend
    
    zkn=`jps -l | grep org.apache.zookeeper.server | wc -l`
    if [ $zkn != 0 ]; then
      zk stop
    fi
    echo
    ;;
  restart)
    on=`jps -l | grep "${MAIN_CLASS}" | wc -l`
    if [ $on != 0 ]; then
      onos stop
    fi

    rcsn=`pgrep -f obj.${RAMCLOUD_BRANCH}/server | wc -l`
    if [ $rcsn != 0 ]; then
      rc-server stop
    fi

    rccn=`pgrep -f obj.${RAMCLOUD_BRANCH}/coordinator | wc -l`
    if [ $rccn != 0 ]; then
      rc-coord stop
    fi

    zkn=`jps -l | grep org.apache.zookeeper.server | wc -l`
    if [ $zkn != 0 ]; then
      zk restart
    fi

    if [ $rccn != 0 ]; then
      rc-coord startifdown
    fi

    if [ $rcsn != 0 ]; then
      rc-server startifdown
    fi

    if [ $on != 0 ]; then
      onos startifdown
    fi
    echo
    ;;
  stat*) # <- status
    echo '[ZooKeeper]'
    zk status
    echo
    echo '[RAMCloud coordinator]'
    rc-coord status
    echo
    echo '[RAMCloud server]'
    rc-server status
    echo
    echo '[ONOS core]'
    onos status
    echo
    ;;
  zk)
    zk $2
    ;;
  rc-c*) # <- rc-coordinator
    rc-coord $2
    ;;
  rc-s*) # <- rc-server
    rc-server $2
    ;;
  rc)
    # XXX rc command ignores if the host is not configured to have those roles,
    # while rc-s or rc-c executes the task regardless of role definition.
    # This is a workaround since TestON does not take role in account
    # and always issue rc deldb right now.

    # TODO make deldb command more organized (clarify when it can work)
    if [ "`is-coord-role`" == "true" ]; then
      rc-coord $2
    fi
    if [ "`is-server-role`" == "true" ]; then
      rc-server $2
    fi
    ;;
  core)
    onos $2
    ;;
  *)
    print_usage
    exit 1
esac

