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

# Map the keepalived state name to the corresponding sdnplatform role
NEW_STATE=$3

logger "notify_role.sh: Called with '$1', '$2', '$3'"


case "$NEW_STATE" in
"MASTER")
  ROLE=MASTER
  MESSAGE="Master re-election or failover."
  ;;
"BACKUP")
  MESSAGE="System start up or  master re-election."
  ROLE=SLAVE
  ;;
"FAULT")
  MESSAGE="Master re-election"
  ROLE=SLAVE
  ;;
*)
  logger "notify_role.sh: Unexpected keepalived state: '$NEW_STATE'"
  exit 1
  ;;
esac

if [ -e "/opt/sdnplatform/health-check-error-message" ]
then
  MESSAGE=$(</opt/sdnplatform/health-check-error-message)
  #echo $MESSAGE
  # Only error code is used since encoding a string into json
  # is error prone until a better way found to send the rest call/json
  # There are 3 conditions that may triger failover
  # 1) user request failover
  # 2) ha-health-check.sh failed with non-zero
  # 3) keepalived detects failure and initiate faili over
  #    currently keepalived does not post detailed information
  #    for this failover. More investigation needs to be done.
  logger "last health check with error $MESSAGE"
  rm -f /opt/sdnplatform/health-check-error-message
fi

logger "notify_role.sh: Updating role file with $ROLE"
# Update the current_role file
echo "sdnplatform.role=$ROLE" > /opt/sdnplatform/current_role

logger "notify_role.sh: Updating Sdnplatform REST with $ROLE"
# Call the sdnplatform REST API to notify about the new role
curl -X POST -d "{\"role\":\"$ROLE\",\"change-description\":\"$MESSAGE\"}" -H "Content-Type: application/json" "http://localhost:8080/wm/core/role/json"

exit 0
