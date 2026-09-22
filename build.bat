@echo off
setlocal

set "JAVA_CMD="
set "JAVAC_CMD="

if exist "C:\Users\Admin\.jdks\temurin-21.0.12.1\bin\javac.exe" (
    set "JAVAC_CMD=C:\Users\Admin\.jdks\temurin-21.0.12.1\bin\javac.exe"
    set "JAVA_CMD=C:\Users\Admin\.jdks\temurin-21.0.12.1\bin\java.exe"
    goto COMPILE
)

where javac >nul 2>&1
if %ERRORLEVEL% equ 0 (
    set "JAVAC_CMD=javac"
    set "JAVA_CMD=java"
    goto COMPILE
)

echo [ERROR] JDK 21 (javac) not found.
exit /b 1

:COMPILE
if not exist "bin" mkdir bin

echo Compiling Java source files...
"%JAVAC_CMD%" -d bin -encoding UTF-8 src\com\dodging\game\model\*.java src\com\dodging\game\ui\*.java src\com\dodging\game\*.java

if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Build completed successfully.
    exit /b 0
) else (
    echo [ERROR] Compilation failed.
    exit /b %ERRORLEVEL%
)
