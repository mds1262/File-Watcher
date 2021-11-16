#!/bin/sh
#
# chmod +x MediaWatcher2.sh
# mv MediaWatcher2.sh MediaWatcher2
# service MediaWatcher2 start
#

export MEDIA_WATCHER2_HOME=/home/kollus/MediaWatcher2

# See how we were called.
case "$1" in
  start)
  echo -n "Starting MediaWatcher2: "
  cd $MEDIA_WATCHER2_HOME
  ./MediaWatcher2.kill.sh
  ./MediaWatcher2.sh
  echo
  ;;
  stop)
  echo -n "Shutting down MediaWatcher2: "
  cd $MEDIA_WATCHER2_HOME
  ./MediaWatcher2.kill.sh
  echo
  ;;
  restart)
  $0 stop
  sleep 2
  $0 start
  ;;
  *)
  echo "Usage: $0 {start|stop|restart}"
  exit 1
esac
exit 0
