#!/bin/bash

sudo docker stop mediawatcher2
sudo docker rm -f mediawatcher2
sleep 1

sudo docker run -h "$HOSTNAME" -d\
-u kollus \
--name mediawatcher2 \
-p 8088:8088 \
-v /home/kollus/MediaWatcher2/logs:/home/kollus/MediaWatcher2/logs \
-v /home/kollus/MediaWatcher2/conf:/home/kollus/MediaWatcher2/conf \
-v /home/kollus/upload:/home/kollus/upload \
-v /home/kollus/http_upload:/home/kollus/http_upload \
-v /home/kollus/http_endpoint_upload:/home/kollus/http_endpoint_upload \
-v /mnt/medianas:/mnt/medianas \
61.255.239.152/kollus/mediawatcher2:1.6

sleep 1
sudo docker ps -a
