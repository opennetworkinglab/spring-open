#!/bin/bash

# run this script, when RAMCloud java binding is updated

set -x

export ONOS_HOME=${ONOS_HOME:-$(cd `dirname $0`; pwd)}
export RAMCLOUD_HOME=${RAMCLOUD_HOME:-~/ramcloud}

source ${ONOS_HOME}/scripts/common/utils.sh

confirm-if-root

${ONOS_HOME}/ramcloud-build-scripts/build_jni_so.sh
