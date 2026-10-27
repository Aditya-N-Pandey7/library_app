@echo off
setlocal

if not exist "target\classes" mkdir "target\classes"

set "M2_REPO=%USERPROFILE%\.m2\repository"
set "OJDBC_JAR=%M2_REPO%\com\oracle\database\jdbc\ojdbc11\23.2.0.0\ojdbc11-23.2.0.0.jar"
set "H2_JAR=%M2_REPO%\com\h2database\h2\2.2.224\h2-2.2.224.jar"
set "CP=target\classes;%OJDBC_JAR%;%H2_JAR%"

echo Compiling com.exam.App...
javac --release 21 -d target\classes src\main\java\com\exam\App.java
if errorlevel 1 (
    echo Compilation failed!
    exit /b 1
)

echo Running com.exam.App...
java -cp "%CP%" com.exam.App
