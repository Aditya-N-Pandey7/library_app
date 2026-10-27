#!/usr/bin/env bash
set -e

mkdir -p target/classes

M2_REPO="${USERPROFILE:-$HOME}/.m2/repository"
OJDBC_JAR="$M2_REPO/com/oracle/database/jdbc/ojdbc11/23.2.0.0/ojdbc11-23.2.0.0.jar"
H2_JAR="$M2_REPO/com/h2database/h2/2.2.224/h2-2.2.224.jar"

# Convert paths and separator for Windows JVM when running under Git Bash (MINGW/MSYS)
if [[ "$OSTYPE" == "msys"* || "$OSTYPE" == "cygwin"* || "$OSTYPE" == "win32"* ]]; then
    OJDBC_WIN=$(cygpath -m "$OJDBC_JAR")
    H2_WIN=$(cygpath -m "$H2_JAR")
    CP="target/classes;${OJDBC_WIN};${H2_WIN}"
    echo "Compiling com.exam.App..."
    javac --release 21 -d target/classes src/main/java/com/exam/App.java
    echo "Running com.exam.App..."
    MSYS_NO_PATHCONV=1 java -cp "$CP" com.exam.App
else
    CP="target/classes:${OJDBC_JAR}:${H2_JAR}"
    echo "Compiling com.exam.App..."
    javac --release 21 -d target/classes src/main/java/com/exam/App.java
    echo "Running com.exam.App..."
    java -cp "$CP" com.exam.App
fi
