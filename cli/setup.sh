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

SOURCE=$(pwd)
WORKSPACE=${SOURCE}/workspace
VIRTUALENV=${WORKSPACE}/ve
TOOLS=${WORKSPACE}/tools
DEPS=${SOURCE}/deps

DJANGO_PKG=django-nonrel-20111024-be48c152abc6.zip
DJANGO_TOOLBOX_PKG=djangotoolbox-20111024-957ea5f68040.zip
#CASSANDRA_VERSION=1.0.10
#CASSANDRA_PKG=apache-cassandra-${CASSANDRA_VERSION}-bin.tar.gz
THRIFT_VERSION=0.7.0
THRIFT_PKG=thrift-${THRIFT_VERSION}.tar.gz

rm -rf ${WORKSPACE}
mkdir -p ${TOOLS}

unzip -q -d ${TOOLS}/.tmp ${DEPS}/${DJANGO_PKG}
cp ${DEPS}/${DJANGO_PKG} ${TOOLS}/
unzip -q -d ${TOOLS}/.tmp ${DEPS}/${DJANGO_TOOLBOX_PKG}
cp ${DEPS}/${DJANGO_TOOLBOX_PKG} ${TOOLS}/
#tar -z -x -C ${TOOLS} -f ${DEPS}/${CASSANDRA_PKG}
#mv ${TOOLS}/apache-cassandra-${CASSANDRA_VERSION} ${TOOLS}/cassandra
tar -z -x -C ${TOOLS} -f ${DEPS}/${THRIFT_PKG}
mv ${TOOLS}/thrift-${THRIFT_VERSION} ${TOOLS}/thrift

rm -rf ${VIRTUALENV} python
virtualenv ${VIRTUALENV} --system-site-packages
mkdir -p ${VIRTUALENV}/log
ln -snf ${VIRTUALENV} ${WORKSPACE}/python
ln -snf bin ${VIRTUALENV}/sbin
source ${VIRTUALENV}/bin/activate
(
  cd ${TOOLS}/.tmp/django-nonrel
  patch -p3 < ${SOURCE}/build/django-autoreload-notty.patch
  python setup.py -q install
)
(
  cd ${TOOLS}/.tmp/djangotoolbox
  python setup.py -q install
)
(
  cd ${TOOLS}/thrift
  chmod +x ./configure
  ./configure --with-cpp=no --with-erlang=no --with-perl=no --with-php=no --with-php_extension=no --with-ruby=no --with-haskell=no
  ARCHFLAGS="-arch i386 -arch x86_64" make -j4 all
  cd lib/py
  python setup.py -q sdist -f --dist-dir=${TOOLS}
  python setup.py -q install
)
#(
#  cd ${TOOLS}/cassandra/interface
#  ${TOOLS}/thrift/compiler/cpp/thrift --gen py --gen java cassandra.thrift
#  cd gen-py
#  echo -e "import distutils.core\ndistutils.core.setup(name=\"cassandra-py\", version=\"${THRIFT_VERSION}\", packages=[\"cassandra\"])" >setup.py
#  python setup.py -q sdist -f --dist-dir=${TOOLS}
#  python setup.py -q install
#  cd ../gen-java
#  cp ${TOOLS}/thrift/lib/java/build/libthrift-${THRIFT_VERSION}.jar .
#  SLF4J_JAR=`ls -1 ${TOOLS}/thrift/lib/java/build/lib | grep slf4j-api`
#  cp ${TOOLS}/thrift/lib/java/build/lib/${SLF4J_JAR} .
#  mkdir -p build/classes
#  javac -d build/classes -cp "${SLF4J_JAR}:libthrift-${THRIFT_VERSION}.jar" org/apache/cassandra/thrift/*.java
#  cd build
#  jar cf cassandra-thrift-${CASSANDRA_VERSION}.jar -C classes org
#  #mkdir -p ${SOURCE}/sdnplatform/lib
#  #cp cassandra-thrift-${CASSANDRA_VERSION}.jar ${SOURCE}/sdnplatform/lib
#)
#sed -i.old 's#/var/lib/cassandra/#'${VIRTUALENV}'/cassandra/#;s#^rpc_address: .*#rpc_address: 0.0.0.0#;s#^partitioner: .*#partitioner: org.apache.cassandra.dht.OrderPreservingPartitioner#' ${TOOLS}/cassandra/conf/cassandra.yaml
#sed -i.old 's#/var/log/cassandra/system.log#'${VIRTUALENV}'/log/cassandra.log#' ${TOOLS}/cassandra/conf/log4j-server.properties
#sed -i.old 's#^JMX_PORT=.*#JMX_PORT="8085"#' ${TOOLS}/cassandra/conf/cassandra-env.sh
#echo 'JVM_OPTS="$JVM_OPTS -Xss160k"' >> ${TOOLS}/cassandra/conf/cassandra-env.sh

make clean

echo "+++ Python virtualenv set up in ${VIRTUALENV}"
echo "+++ To activate in the current shell, run:"
echo "+++   source ${WORKSPACE}/ve/bin/activate"
