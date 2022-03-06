#!/bin/bash
flag=`ps -ef | grep Ddragon_Streaming |grep -v grep|wc -l`


if [ ${flag} == 0 ]
        then
                echo "STREAMING SERVER is Not Running ..."
        else
                ps -ef | grep Ddragon_Streaming |grep -v grep |awk '{print "kill -9 "$2}' | sh -x 
				
				flag=`ps -ef | grep Ddragon_Streaming |grep -v grep|wc -l`
		
				if [ ${flag} == 0 ]
						then
								echo "[SUCCESS] STREAMING SERVER is Stopped "
						else
								echo "[FAIL] STREAMING SERVER could not be stopped ..."
				fi			
		
fi





