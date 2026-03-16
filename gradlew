#!/bin/sh
# Gradle start up script for UNIX

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Detect OS and set PATH sep
case "`uname`" in
  CYGWIN*) APP_HOME=`cygpath --path --mixed "$APP_HOME"`
esac

GRADLE_OPTS="$GRADLE_OPTS \"-Dorg.gradle.appname=$APP_BASE_NAME\""

exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
  "-Dorg.gradle.appname=$APP_BASE_NAME" \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
