#!/bin/sh
set -eu
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if [ -n "${JAVA_HOME:-}" ]; then
    JAVA_COMMAND="$JAVA_HOME/bin/java"
else
    JAVA_COMMAND=java
fi
exec "$JAVA_COMMAND" -classpath "$PROJECT_DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
