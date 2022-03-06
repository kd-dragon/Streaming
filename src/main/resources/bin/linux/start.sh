#!/bin/bash
SCHED_HOME=/home/kdy/demo-live/streaming/trunk
JAVA_HOME=/home/kdy/java/jdk1.8.0_291/jre/bin
var=$(ps -ef | grep ${SCHED_HOME})
sched_min_heap=128M
sched_max_heap=512M
LOG_CONFIG=${SCHED_HOME}/config
JAVA_OPT=-Xms${sched_min_heap}" -Xmx"${sched_max_heap}
IFS=' ' read -r -a array <<< $var
i=0
flag=0

while [ $i -lt ${#array[@]} ]
do
        if [ ${array[${i}]} == "java" ]
        then
                flag=1
                index=$(($i-6))
                pid=${array[${index}]}
                echo "process id : ${pid}"
        fi
        i=$(($i+1))
done

if [ ${flag} == 1 ]
        then
                echo "STREAMING SERVER is Already Started..."
        else
				nohup $JAVA_HOME/java -DDdragon_Streaming $JAVA_OPT -jar ${SCHED_HOME}/Ddragon_Streaming-0.0.1-SNAPSHOT.jar --spring.config.name=application --spring.profiles.active=main --logging.config=file:${LOG_CONFIG}/logback-spring.xml &
				#nohup $JAVA_HOME/java -DDdragon_Streaming $JAVA_OPT -jar ${SCHED_HOME}/Ddragon_Streaming-0.0.1-SNAPSHOT.jar --spring.config.name=application --spring.profiles.active=main --logging.config=file:${LOG_CONFIG}/logback-spring.xml 1>/dev/null 2>&1 &
fi

