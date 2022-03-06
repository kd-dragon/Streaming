#!/bin/bash
flag=`ps -ef | grep Ddragon_Streaming |grep -v grep|wc -l`

if [ ${flag} == 0 ]
        then
                echo "STREAMING SERVER is Not Running ..."
        else
                echo "STREAMING SERVER is Running ..."
fi

