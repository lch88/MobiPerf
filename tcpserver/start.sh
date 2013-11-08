#!/bin/bash

cd out

dl_port=26001
ul_port=26002
config_port=26003

for i in Downlink Uplink ServerConfig
do
	echo "Running $i ..."
	java -Xmx128M -jar $i.jar &
        sleep 1
done

echo "Verifying on ports ..."
for port in $dl_port $ul_port $config_port
do
	is_up=$(netstat -atup | grep "$port" | wc -l)
	if [ $is_up == 0 ];then
		echo "Port $port doesn't start properly"
		echo "Try \"./stop.sh; ./start.sh\" one more time"
		exit 1
	fi
done
echo "Success deploying and running server"
