@echo off
setlocal

set "JAVA_CMD="

if exist "C:\Users\Admin\.jdks\temurin-21.0.12.1\bin\java.exe" (
    set "JAVA_CMD=C:\Users\Admin\.jdks\temurin-21.0.12.1\bin\java.exe"
    goto CHECK_BUILD
)

where java >nul 2>&1
if %ERRORLEVEL% equ 0 (
    set "JAVA_CMD=java"
    goto CHECK_BUILD
)

echo [ERROR] Java 21 runtime not found.
pause
exit /b 1

:CHECK_BUILD
if not exist "bin\com\dodging\game\Main.class" (
    echo [INFO] Binaries not found. Building project...
    call build.bat
    if %ERRORLEVEL% neq 0 (
        echo [ERROR] Build failed.
        pause
        exit /b 1
    )
)

echo Starting Ball Dodging Game...
start "" "%JAVA_CMD%" -cp bin com.dodging.game.Main
