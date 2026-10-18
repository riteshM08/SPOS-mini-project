@echo off
setlocal
title SkyWings Airline Reservation System
set "JAVA_HOME=C:\Users\manth\.jdk-17"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "%~dp0"

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Error: Java 17 was not found at %JAVA_HOME%.
    pause
    exit /b 1
)

if not exist "bin\com\airline\AirlineServer.class" (
    call compile.bat
    if errorlevel 1 exit /b 1
)

echo Starting Airline Reservation System.
echo Open http://localhost:8080 in your browser.
echo Press Ctrl+C to stop the server.

java -cp "bin;lib/*" com.airline.AirlineServer
pause
endlocal
