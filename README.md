ONOS for SPRING-OPEN
====

ONOS (Open Networking Operating System) was release publicly Dec 5th -- ONOS Avocet.
For details see here:

https://wiki.onosproject.org/display/ONOS/Downloads

The segment routing use case is built on an older internal release of ONOS (Sept 2014). The following instructions do not apply to ONOS 1.0.0 (Avocet), and are meant only for the SPRING-OPEN project.


Getting Started with SPRING-OPEN
-------------------------

Following URL has the instructions how to get started with ONOS for spring-open, starting from 
downloading the pre-built VM:

https://wiki.onosproject.org/display/ONOS/Installation+Guide

Using the instructions on the wiki is recommended. 

The instructions below are kept only for historical purposes.


Building ONOS
-------------

1. Cleanly build ONOS

        $ cd ${ONOS_HOME}/
        $ mvn clean compile

External Dependencies
---------------------
1. Required packages

        $ sudo apt-get install maven python-flask python-cmd2 python-pyparsing

2. ZooKeeper

    Download and install apache-zookeeper-3.4.6:
    http://zookeeper.apache.org/releases.html
    
    By default ONOS expect ZooKeeper to be installed in `~/zookeeper-3.4.6`.  
    This can be changed by specifing the path using `ZK_HOME` environment variable.
    
    Data directory ZooKeeper uses by default is `/var/lib/zookeeper`.
    You will need to give current user permission to write to this directory.
    
    This directory can be changed using specifying `ZK_LIB_DIR` environment variable and 
    running `./onos.sh setup` to generate `${ONOS_HOME}/conf/zoo.cfg`.
    
    See Configuration for details about `./onos.sh setup`.


    
Configuration
-------------
`./onos.sh setup` script is used to generate ONOS related configuration files.
This script read configuration from "${ONOS_CONF_DIR}/onos_node.\`hostname\`.conf".

Copy the file "${ONOS_HOME}/conf/onos_node.conf" to match the hostname and configure 
the content appropriately.  


Once you're done with required configuration run following to generate configuration files.

        $ cd ${ONOS_HOME}/
        $ ./onos.sh setup


Running ONOS and required components
------------------------------------
To start ZooKeeper,  and ONOS core.

        $ cd ${ONOS_HOME}/
        $ ./onos.sh start 

To stop all the above

        $ cd ${ONOS_HOME}/
        $ ./onos.sh stop


Running ONOS components one by one
----------------------------------
You can manually start/stop individual ONOS components as follows:

1. Start Zookeeper

        $ cd ${ONOS_HOME}/
        $ ./onos.sh zk start

        ## Confirm Zookeeper is running:
        $ ./onos.sh zk status


2. Start ONOS

        $ cd ${ONOS_HOME}/
        $ ./onos.sh core start

        ## Confirm ONOS is running:
        $ ./onos.sh core status


Running unit tests
------------------
Unit tests bundled with ONOS source code, can be executed by using the following:

        $ cd ${ONOS_HOME}/
        $ mvn test

Some of the unit tests, which take longer time to execute are excluded from the above goal.
To force running all the unit tests, use the following commands:

        $ cd ${ONOS_HOME}/
        $ mvn test -P all-tests

To run only a subset of the unit tests, use the following commands:

        $ cd ${ONOS_HOME}/
        $ mvn test -Dtest=PortNumberTest

Comma and wildcards can be used to specify multiple test cases.
See [maven-surefire-plugin website](http://maven.apache.org/surefire/maven-surefire-plugin/examples/single-test.html) for details.

Running static analysis
-----------------------
ONOS utilizes several [static analysis tools](https://wiki.onlab.us/display/onosdocs/ONOS+Coding+Style#ONOSCodingStyle-Codestaticanalysistools) to detect programmatic and formatting errors.
To run some of the analysis against the code, use the following commands:

        $ cd ${ONOS_HOME}
        $ mvn clean verify -P error-prone


Downloading dependencies (Optional)
-----------------------------------

Maven need the Internet connection to download required dependencies and plugins,
when they're used for the first time.

If you need to develop ONOS in an Internet unreachable environment
you may want to run the following helper script before you go offline,
so that required dependencies and plugins for frequently used maven target will be
downloaded to your local environment.

        $ ./prep-for-offline.sh

License
-------
Apache 2.0


