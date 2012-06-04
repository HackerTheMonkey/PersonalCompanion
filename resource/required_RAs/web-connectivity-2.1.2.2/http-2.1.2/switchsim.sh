#!/bin/sh

SIMULATOR_JAR=lib/http-loadgenerator.jar

#The URL assumes that Rhino and this examples package are on the same host.
URL="http://localhost:8000"

HEAP_SIZE=256m

GCOPTIONS="-XX:+UseParNewGC \
       -XX:+UseConcMarkSweepGC \
       -XX:MaxNewSize=32m -XX:NewSize=32m \
       -Xms${HEAP_SIZE} -Xmx${HEAP_SIZE} \
       -XX:SurvivorRatio=128 \
       -XX:MaxTenuringThreshold=0 \
       -XX:CMSInitiatingOccupancyFraction=60 \
       -Dsun.rmi.dgc.server.gcInterval=0x7FFFFFFFFFFFFFFE \
       -Dsun.rmi.dgc.client.gcInterval=0x7FFFFFFFFFFFFFFE \
       -XX:+DisableExplicitGC \
       -verbose:gc"
       
#HPROFOPTS="-Xrunhprof"

java $GCOPTIONS $HPROFOPTS -jar $SIMULATOR_JAR -u $URL "$@"
