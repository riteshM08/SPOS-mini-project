@echo off
title SkyWings Airline Reservation System
color 0B

echo ======================================================================
echo         AIRLINE RESERVATION SYSTEM (ARS) - STARTUP
echo         SkyWings Airways - Java + JDBC + PostgreSQL/SQLite
echo ======================================================================
echo.

:: Set Java Home - uses bundled JDK 17
set "JAVA_HOME=C:\Users\manth\.jdk-17"
set "PATH=%JAVA_HOME%\bin;%PATH%"

:: Check Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found at %JAVA_HOME%
    echo         Please ensure JDK 17 is extracted to C:\Users\manth\.jdk-17
    pause
    exit /b 1
)

:: Change to project directory
cd /d "%~dp0"

:: Check if classes need recompile
if not exist "bin\com\airline\AirlineServer.class" (
    echo [INFO] Compiled classes not found. Recompiling...
    call compile.bat
    if errorlevel 1 (
        echo [ERROR] Compilation failed. Cannot start server.
        pause
        exit /b 1
    )
)

echo [INFO] Starting ARS Java Server...
echo [INFO] Open your browser at: http://localhost:8080
echo [INFO] Press Ctrl+C to stop the server
echo.

java -cp "bin;lib/*" com.airline.AirlineServer

pause
