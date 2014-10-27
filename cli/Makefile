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

SOURCE = $(shell pwd)
WORKSPACE = $(SOURCE)/workspace
VIRTUALENV = $(WORKSPACE)/ve
TOOLS = $(WORKSPACE)/tools
BUILD_DIR = $(SOURCE)/build
#FL_DIR = $(SOURCE)/sdnplatform

all: build-sdnplatform

clean-python:
#	printf 'import setuptools\nsetuptools.setup(name="django_cassandra", version="0.1", packages=["django_cassandra", "django_cassandra.db"])' >$(SOURCE)/django_cassandra_backend/setup.py
	echo '$(SOURCE)' >$$(python -c 'import distutils; print distutils.sysconfig.get_python_lib()')/sdnplatform.pth
	set -e; \
	cd $(SOURCE); \
#	for p in django_cassandra_backend/setup.py sdncon/setup.py cli/setup.py; do
	for p in sdncon/setup.py cli/setup.py; do \
	  echo "+++ Preparing Python virtualenv for $$p"; \
	  ( cd $$(dirname $$p); rm -rf *.egg-info; ARCHFLAGS="-arch i386 -arch x86_64" python $$(basename $$p) develop ); \
	done
	( cd $(SOURCE)/cli ; tools/rebuild_model.sh )

clean-sdnplatform:
#	cd $(FL_DIR); ant clean

clean: reset-cassandra clean-sdnplatform clean-python

stop-cassandra:
#	$(BUILD_DIR)/kill-port-wait.sh -p 9160
#	@echo "+++ Cassandra stopped"

reset-cassandra: stop-cassandra
#	rm -rf $(VIRTUALENV)/cassandra
#	@echo "+++ Cassandra db deleted"

cassandra: start-cassandra
start-cassandra:
#	$(BUILD_DIR)/start-and-wait-for-port.sh -p 9160 -l $(VIRTUALENV)/log/cassandra.log $(TOOLS)/cassandra/bin/cassandra
#	@echo "+++ Cassandra running"

stop-sdncon:
	$(BUILD_DIR)/kill-port-wait.sh -p 8000
	@echo "+++ sdncon stopped"

reset-sdncon: stop-sdncon reset-cassandra start-cassandra

start-sdncon: start-cassandra
	if ! lsof -iTCP:8000 -sTCP:LISTEN >/dev/null; then \
	( \
	  cd $(SOURCE)/sdncon; \
	  [ -d $(VIRTUALENV)/cassandra/data/sdncon ] || python manage.py syncdb --noinput; \
	  $(BUILD_DIR)/start-and-wait-for-port.sh -p 8000 -l $(VIRTUALENV)/log/sdncon.log python manage.py runserver 0.0.0.0:8000; \
	); \
	fi
	@echo "+++ sdncon running"

stop-sdnplatform:
#	while pid=$$(lsof -t -iTCP:6633 -sTCP:LISTEN); do kill $${pid}; sleep 1; done
#	@echo "+++ SDN Platform stopped"

start-sdnplatform: start-cassandra
#	if ! lsof -iTCP:6633 -sTCP:LISTEN >/dev/null; then \
#	  ( cd $(SOURCE)/sdnplatform; ant run &>$(VIRTUALENV)/log/sdnplatform.log & ); \
#	fi
#	while ! nc -z localhost 6633; do sleep 1; done
#	@echo "+++ SDN Platform running"

eclipse:
#	cd $(FL_DIR); ant eclipse

build-sdnplatform:
#	cd $(FL_DIR); ant dist
clean-srdb: stop-sdncon
	rm -fr sdncon/sdncon
	make clean
	make start-sdncon
