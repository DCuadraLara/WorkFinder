@echo off
setlocal
set "WORKFINDER_JAVA=java"
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "WORKFINDER_JAVA=%JAVA_HOME%\bin\java.exe"
"%WORKFINDER_JAVA%" -version >nul 2>&1
if errorlevel 1 (
    echo WorkFinder necesita Java 17. Instala un JDK 17 y configura JAVA_HOME o PATH.
    pause
    exit /b 1
)
if not exist "%~dp0workfinder.jar" (
    echo Extrae todo el ZIP antes de abrir WorkFinder.cmd.
    pause
    exit /b 1
)
"%WORKFINDER_JAVA%" --module-path "%~dp0javafx" --add-modules javafx.controls -cp "%~dp0workfinder.jar;%~dp0lib\*" com.davidcuadralara.workfinder.Main %*
set "WORKFINDER_EXIT=%ERRORLEVEL%"
if not "%WORKFINDER_EXIT%"=="0" (
    echo No se pudo iniciar WorkFinder. Comprueba Java 17 y conserva las carpetas lib y javafx junto al JAR.
    pause
)
exit /b %WORKFINDER_EXIT%
