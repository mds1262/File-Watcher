#!/bin/bash

JAR_FILENAME=`ls MediaWatcher2_*.jar | sort -r | head -1`
ln -nfs $JAR_FILENAME MediaWatcher2.jar

echo \> MediaWatcher2.jar New Version symlink
echo \> $JAR_FILENAME -\> MediaWatcher2.jar

if [ "$1" == "restart" ]; then
    echo ""
    echo "> service restart : MediaWatcher2..."
    sudo service MediaWatcher2 restart
    echo ""
fi