#!/bin/sh
set -eu
workfinder_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    workfinder_java="$JAVA_HOME/bin/java"
else
    workfinder_java=java
fi
if ! "$workfinder_java" -version >/dev/null 2>&1; then
    echo "WorkFinder necesita Java 17. Instala un JDK 17 y configura JAVA_HOME o PATH." >&2
    exit 1
fi
if [ ! -f "$workfinder_dir/workfinder.jar" ]; then
    echo "Extrae todo el ZIP antes de iniciar WorkFinder." >&2
    exit 1
fi
exec "$workfinder_java" --module-path "$workfinder_dir/javafx" --add-modules javafx.controls -cp "$workfinder_dir/workfinder.jar:$workfinder_dir/lib/*" com.davidcuadralara.workfinder.Main "$@"
