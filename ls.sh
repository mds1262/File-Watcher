#!/bin/sh
#
export LANG=en_US.UTF-8

P=$1
F=$2

/bin/ls -lRGQ --time-style="+%Y-%m-%d_%H:%M:%S" $P > $F

exit $?
