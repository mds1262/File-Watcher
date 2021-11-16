#!/bin/bash

bash -c $HOME/MediaWatcher2/jar_rename_restart.sh
sudo docker rm -f mediawatcher2
sudo docker build --tag 61.255.239.152/kollus/mediawatcher2:1.6 .
sudo docker rmi $(sudo docker images | grep "<none>" | awk '{print $3}')
sudo docker push 61.255.239.152/kollus/mediawatcher2:1.6
sudo docker images