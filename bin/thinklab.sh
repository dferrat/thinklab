#!/bin/sh

case "`uname`" in
  Darwin*) if [ -z "$JAVA_HOME" ] ; then
             JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Home
           fi
           ;;
esac

PRG="$0"
CWD=`pwd`

if [ -z "$THINKLAB_HOME" ] ; then
	THINKLAB_HOME=`dirname "$PRG"`/..
	# make it fully qualified
	THINKLAB_HOME=`cd "$THINKLAB_HOME" && pwd`
fi

if [ ! -f "$THINKLAB_HOME/lib/im-boot.jar" ] ; then
  echo "Error: Could not find $THINKLAB_HOME/lib/im-boot.jar"  
  exit 1
fi

if [ -z "$JAVACMD" ] ; then
  if [ -n "$JAVA_HOME"  ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD="$JAVA_HOME/jre/sh/java"
    else
      JAVACMD="$JAVA_HOME/bin/java"
    fi
  else
    JAVACMD=`which java 2> /dev/null `
    if [ -z "$JAVACMD" ] ; then
        JAVACMD=java
    fi
  fi
fi

if [ ! -x "$JAVACMD" ] ; then
  echo "Error: JAVA_HOME is not defined correctly."
  echo "  We cannot execute $JAVACMD"
  exit 1
fi

if [ -z "$THINKLAB_INST" ] ; then
  THINKLAB_INST=$THINKLAB_HOME
fi

cd $THINKLAB_HOME

THINKLAB_CMD="$JAVACMD $JAVA_OPTS -Djava.library.path=$THINKLAB_HOME/plugins/org.integratedmodelling.thinklab.riskwiz/common -Djpf.boot.config=$THINKLAB_HOME/boot.properties -Dthinklab.library.path=$THINKLAB_LIBRARY_PATH -Dthinklab.plugins=$THINKLAB_PLUGINS -Dthinklab.inst=$THINKLAB_INST -Djava.endorsed.dirs=$THINKLAB_HOME/lib/endorsed -jar $THINKLAB_HOME/lib/im-boot.jar org.java.plugin.boot.Boot"

if [ "$1" = "start" ] ; then
  cd $THINKLAB_INST
  mkdir -p $THINKLAB_INST/var/log
  sh -c "exec $THINKLAB_CMD $@ $THINKLAB_ARGS >> $THINKLAB_INST/var/log/thinklab.out 2>&1"
else
  exec $THINKLAB_CMD $@ $THINKLAB_ARGS
fi

cd $CWD
