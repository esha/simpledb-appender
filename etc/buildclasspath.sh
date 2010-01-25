#!/bin/bash

if [ -z $1 ]; then
    echo "Usage: `basename $0` path-to-libs [optional classpath to append]"
    exit 1
fi
LIBDIR=$1
shift
OPTIONAL_JARS=$1
set -o nounset

# this will find all the files and create a colon-delimited string
# (the call into SED removes the trailing colon)
LIBCLASSPATH=`find $LIBDIR -type f -name '*.jar' | tr '\n' ':' | sed '/:/ s/.$//'`

if [ ! -z $OPTIONAL_JARS ]; then
    LIBCLASSPATH=$LIBCLASSPATH:$OPTIONAL_JARS
fi

echo "$LIBCLASSPATH"
