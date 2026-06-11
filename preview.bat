@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
if "%~1"=="" (
    python "%SCRIPT_DIR%preview.py" run
) else if "%~1"=="--device-id" (
    python "%SCRIPT_DIR%preview.py" %* run
) else if "%~1"=="--avd-name" (
    python "%SCRIPT_DIR%preview.py" %* run
) else if "%~1"=="--variant" (
    python "%SCRIPT_DIR%preview.py" %* run
) else if "%~1"=="--package-name" (
    python "%SCRIPT_DIR%preview.py" %* run
) else if "%~1"=="--activity-name" (
    python "%SCRIPT_DIR%preview.py" %* run
) else (
    python "%SCRIPT_DIR%preview.py" %*
)
set "EXIT_CODE=%ERRORLEVEL%"

endlocal & exit /b %EXIT_CODE%
