#!/bin/bash

export LANG="ko_KR.UTF-8"

JAVA_HOME=/vol3/jdk1.8
SERVER_HOME=/logichome/tbs/logs/server

cd $SERVER_HOME
nohup $JAVA_HOME/bin/java -jar -Dname=Real -Dlog4jdbc.dump.sql.maxlinelength=0 -Dfile.encoding=UTF-8 -Dspring.profiles.active=real -Dapp.log.home=$SERVER_HOME/logs $SERVER_HOME/com.realtime.jar > /dev/null 2>&1 &
