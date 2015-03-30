jrrd2
=====

A thread-safe rewrite of jrrd.

Building
========

Requires maven (tested with 3.1.1) and cmake (tested with 2.8.12.2)

```sh
./build.sh
```

The dist/ folder should now contain both jrrd2.jar and libjrrd2.so.

Debian Notes
------------
On Debian 7.0, the environment can be setup with:
```sh
apt-get install openjdk-7-jdk build-essential cmake make pkg-config librrd-dev
export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64
```

Installation
============

1. Copy the contents of the dist/ folder to /opt/jrrd2/
1. Set the following properties in $OPENNMS_HOME/etc/rrd-configuration.properties

        org.opennms.rrd.strategyClass=org.opennms.netmgt.rrd.rrdtool.MultithreadedJniRrdStrategy
        org.opennms.rrd.interfaceJar=/opt/jrrd2/jrrd2.jar
        opennms.library.jrrd2=/opt/jrrd2/libjrrd2.so

1. Restart OpenNMS
