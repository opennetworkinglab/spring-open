#!/bin/bash

#
# A helper script to download all the dependencies beforehand, rather than
# Maven lazily downloading them when they're needed.
#

if [ -z "${MVN}" ]; then
    MVN="mvn"
fi

# download package dependencies
# run goals to download required plugins
${MVN} -T 1C dependency:go-offline clean verify pmd:pmd pmd:cpd -DskipTests -Dcheckstyle.skip -Dfindbugs.skip -Dpmd.skip -Dcpd.skip
