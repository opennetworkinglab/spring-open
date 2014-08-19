SDN Platform
============

This repository contains an advanced SDN controller and platform for
network virtualization.

# Building #
Install prerequisites.  You'll just need a basic functioning build
environment plus a few build-time dependencies.  On Ubuntu, you can
run:

    sudo apt-get install unzip python-dev python-virtualenv \
    	 git openjdk-7-jdk ant build-essential

Note that on Ubuntu 12.04, you may want to remove java6:

    sudo apt-get remove openjdk-6-jre-lib openjdk-6-jre-headless

To build the controller:

1. Clone from repository
2. `./setup.sh`
3. `make`

# Running #

## Sdncon ##
To run the controller and the cli, you need to be running a working
instance of cassandra and sdncon.  The setup script will have created
a python virtualenv for you to make it easy to run the python
components.  You must first activate the virtualenv in your current
shell by running

    source ./workspace/ve/bin/activate

Now you can easily run any of the python commands.

The make targets `start-cassandra` and `start-sdncon` will
automatically start a local copy of cassandra and sdncon.  There are
corresponding stop commands as well.  These commands require an
activated virtualenv.  If you run

    make stop-sdncon reset-cassandra start-sdncon

This will stop any existing sdncon and cassandra, reset their
databases to zero, and start a new sdncon with a fresh database.  The
output from these commands will go to a log file in your
`workspace/ve/logs` directory.

## Controller ##

Now you're ready to run a copy of sdnplatform.  The easiest way is to
run

    make start-sdnplatform

or you can do this manually with output to standard out by running
from the `sdnplatform` directory

    java -jar target/sdnplatform.jar

You can specify your own configuration file with `-cf [path]` or use
the default.

## CLI ##

The CLI depends on a running instance of sdncon.  To run the CLI, just
run it from the command line from the CLI directory:

    ./cli.py

The CLI has online help and tab completion on its commands.

## Web-Dashboard ##

The example web-dashboard application depends on a running instance of sdncon and a web server to serve content. The web-dashboard files are divided into two parts:

    document-root: Static HTML, javascript, and css files.
    cgi-bin-root : Python based CGI scripts.

To run the web-dashboard, copy the files into the an apropriate webserver directory for the content type.

On Ubuntu 12.04 with a default Apache2 install:

1. Move `document-root` contents into /var/www
2. Move `cgi-bin-root` contents into /usr/lib/cgi-bin

If the webserver does not use `/cgi-bin` for CGI file access, modify the `CGI_PATH` variable in the dashboard.html file to the apropriate path.

# Eclipse environment #

1) At the shell, create the .project files with `make eclipse`
% make eclipse

2) In Eclipse, create a new workspace:
Click File -> Switch WorkSpace -> Other and then enter the name of
the new workspace, e.g. "Workspace.net-virt-platform"

3) Once in the new workspace, import all of the eclipse projects
Click File -> Import -> General -> Existing Projects into Workspace
-> Next in the "Select Root Directory" dialog, type in or navigate
to your checkout of the net-virt-platform base directory, e.g.,
~/git/net-virt-platform .

Eclipse should automatically find four eclipse projects: 
* cli
* django_cassandra_backend
* sdncon
* sdnplatform
Make sure they are all checked (the default) and click "Finish"

If you are looking to do python development, it is recommended that
you install the Eclipse "pydev" modules, as documented at http://pydev.org.
