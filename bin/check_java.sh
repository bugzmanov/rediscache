#!/usr/bin/env bash

if type -p java; then
    echo found java executable in PATH
else
  JAVA_HOME=~/jdk && ./bin/install-jdk.sh -f 11 -v --target $JAVA_HOME
  export PATH=$JAVA_HOME/bin:$PATH
fi


