#!/bin/bash

JAR_FILENAME=`ls MediaWatcher2_*.jar | sort -r | head -1`
scp $JAR_FILENAME kollus@vm-deploy:~/deploy/MediaWatcher2/$JAR_FILENAME
