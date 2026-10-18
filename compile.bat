@echo off
title SkyWings ARS - Compiler
color 0A

echo ======================================================================
echo         AIRLINE RESERVATION SYSTEM - JAVA COMPILER
echo ======================================================================
echo.

:: Set Java Home
set "JAVA_HOME=C:\Users\manth\.jdk-17"
set "PATH=%JAVA_HOME%\bin;%PATH%"

:: Check javac
javac -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] javac not found at %JAVA_HOME%\bin
    echo         Please ensure JDK 17 is extracted to C:\Users\manth\.jdk-17
    pause
    exit /b 1
)

:: Change to project directory
cd /d "%~dp0"

:: Clean output directory
if exist "bin" (
    echo [INFO] Cleaning bin\ directory...
    rmdir /s /q bin
)
mkdir bin

:: Collect all Java sources
echo [INFO] Collecting Java source files...
dir /s /b src\*.java > sources.txt
set /p COUNT=<sources.txt

:: Compile
echo [INFO] Compiling all Java sources...
javac -encoding UTF-8 -cp "lib\*" -d bin @sources.txt

if errorlevel 1 (
    echo.
    echo [FAILED] Compilation errors encountered.
    del sources.txt
    pause
    exit /b 1
)

del sources.txt
echo.
echo [SUCCESS] Compilation complete! Classes written to bin\
echo.
