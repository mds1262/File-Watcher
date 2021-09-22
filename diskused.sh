#!/bin/sh
#
export LANG=en_US.UTF-8

# centos 6.3
# find $1 -maxdepth 1 -type d | xargs du -h --max-depth=0 --block-size=1K > $2

# ubuntu 12.10
#find $1 -maxdepth 1 -type d | xargs du -h --max-depth=1 --block-size=1K > $2

#ceph
ls -lk $1 | awk '{print $5"\t"$9}' > $2

exit $?
