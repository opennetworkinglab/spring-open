ONOS (Open Networking Operating System)
=======================================

ONOS (Open Networking Operating System) is an experimental distributed
SDN OS. Currently, it is under active development. ONOS was announced
and demonstrated at ONS'13, '14.

License
=======
Apache 2.0


Steps to download and setup a development Virtual Machine
---------------------------------------------------------

https://wiki.onlab.us:8443/display/onosdocs/Getting+Started+with+ONOS

Building ONOS
-------------

1. Cleanly build ONOS

        $ cd ${ONOS_HOME}/
        $ mvn clean
        $ mvn compile

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

3. RAMCloud

    Run setup-ramcloud.sh to download and install RAMCloud to `~/ramcloud`.  
    Installation path can be changed by specifing `RAMCLOUD_HOME` environment variable.

        $ cd ${ONOS_HOME}/
        $ ./setup-ramcloud.sh
    
Configuration
-------------
`./onos.sh setup` script is used to generate ONOS related configuration files.
This script read configuration from "${ONOS_CONF_DIR}/onos_node.\`hostname\`.conf".

Copy the file "${ONOS_HOME}/conf/onos_node.conf" to match the hostname and configure 
the content appropriately.  
 e.g., To use RAMCloud as data store change `host.backend` to `ramcloud`

Once you're done with required configuration run following to generate configuration files.

        $ cd ${ONOS_HOME}/
        $ ./onos.sh setup


Running ONOS and required components
------------------------------------
To start ZooKeeper, RAMCloud (if enabled in configuration) and ONOS core.

        $ cd ${ONOS_HOME}/
        $ ./onos.sh start single-node

To stop all the above

        $ cd ${ONOS_HOME}/
        $ ./onos.sh stop

If you need to use the REST APIs, follow the instruction for
"Start ONOS REST API server" in next section.

Running ONOS and required components one by one
-----------------------------------------------
You can manually start/stop individual ONOS components as follows:

1. Start Zookeeper

        $ cd ${ONOS_HOME}/
        $ ./onos.sh zk start

        ## Confirm Zookeeper is running:
        $ ./onos.sh zk status

2. Start RAMCloud Coordinator (only on one of the node in cluster)

        $ cd ${ONOS_HOME}/
        $ ./onos.sh rc-coord start

        ## Confirm RAMCloud Coordinator is running:
        $ ./onos.sh rc-coord status

3. Start RAMCloud Server

        $ cd ${ONOS_HOME}/
        $ ./onos.sh rc-server start

        ## Confirm RAMCloud Server is running:
        $ ./onos.sh rc-server status

4. Start ONOS

        $ cd ${ONOS_HOME}/
        $ ./onos.sh core start

        ## Confirm ONOS is running:
        $ ./onos.sh core status

5. Start ONOS REST API server

        $ cd ${ONOS_HOME}/
        $ ./start-rest.sh start

        ## Confirm the REST API server is running:
        $ ./start-rest.sh status


Developing ONOS in offline environment (Optional)
---------------------------------------------------------------------------

Maven need the Internet connection to download required dependencies and plugins,
when they're used for the first time.

If you need to develop ONOS in an Internet unreachable environment
you may want to run the following helper script before you go offline,
so that required dependencies and plugins for frequently used maven target will be
downloaded to your local environment.

        $ ./prep-for-offline.sh
