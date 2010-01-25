#!/bin/bash

XMLDIR=trivial
if [ $# -eq 1 ]; then
  XMLDIR=$1
fi

SCRIPTDIR=`dirname $0`
LIBCLASSPATH=`$SCRIPTDIR/buildclasspath.sh $SCRIPTDIR/Lib $SCRIPTDIR/xml/$XMLDIR/`

java -classpath $LIBCLASSPATH com.kikini.logging.LogFromStdin
