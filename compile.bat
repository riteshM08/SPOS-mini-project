@echo off
setlocal
cd /d "%~dp0"
set "JAVA_HOME=C:\Users\manth\.jdk-17"
set "PATH=%JAVA_HOME%\bin;%PATH%"

if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo Error: JDK 17 was not found at %JAVA_HOME%.
    pause
    exit /b 1
)

if exist "bin" rmdir /s /q "bin"
mkdir bin

echo Compiling project...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$files = Get-ChildItem -Path '.\src' -Filter '*.java' -Recurse | ForEach-Object { $_.FullName }; if ($files.Count -eq 0) { Write-Error 'No Java source files found.'; exit 1 }; & 'C:\Users\manth\.jdk-17\bin\javac.exe' -encoding UTF-8 -cp 'lib\*' -d 'bin' $files; exit $LASTEXITCODE"

if errorlevel 1 (
    echo Compilation failed.
    pause
    exit /b 1
)

echo Compilation successful.
pause
endlocal
