@echo off
REM ===================================================================
REM  Genera el instalador .exe de la aplicación de gestión (Windows).
REM  Requisitos: JDK 21, Maven 3.9+ en el PATH y WiX 3.x para el instalador.
REM  Sin WiX: empaquetar-exe.bat portable crea una carpeta app-image.
REM ===================================================================
where mvn >nul 2>nul
if errorlevel 1 goto :missingMaven
where jpackage >nul 2>nul
if errorlevel 1 goto :missingJpackage

set "PACKAGE_TYPE=exe"
if /I "%~1"=="portable" set "PACKAGE_TYPE=app-image"
if /I not "%PACKAGE_TYPE%"=="exe" goto :build
where candle >nul 2>nul
if errorlevel 1 goto :missingWix
where light >nul 2>nul
if errorlevel 1 goto :missingWix

:build
call mvn -q clean package -DskipTests
if errorlevel 1 exit /b 1

if exist build-exe rmdir /s /q build-exe
if exist dist rmdir /s /q dist
mkdir build-exe
if errorlevel 1 exit /b 1
copy /y desktop-admin\target\sfa-gestion.jar build-exe\ >nul
if errorlevel 1 goto :error

if /I "%PACKAGE_TYPE%"=="app-image" goto :portable
jpackage --type exe --name "SF Archive" --app-version 1.0.0 ^
  --vendor "San Francisco Archive" --description "Sistema de Gestion de Anomalias" ^
  --input build-exe --main-jar sfa-gestion.jar --main-class com.sfarchive.desktop.Launcher ^
  --dest dist --win-menu --win-shortcut --win-dir-chooser
if errorlevel 1 goto :error
rmdir /s /q build-exe
echo Instalador generado en dist\SF Archive-1.0.0.exe
exit /b 0

:portable
jpackage --type app-image --name "SF Archive" --app-version 1.0.0 ^
  --vendor "San Francisco Archive" --description "Sistema de Gestion de Anomalias" ^
  --input build-exe --main-jar sfa-gestion.jar --main-class com.sfarchive.desktop.Launcher ^
  --dest dist
if errorlevel 1 goto :error
rmdir /s /q build-exe
echo App portable generada en dist\SF Archive\
exit /b 0

:error
echo ERROR: no se completo el empaquetado. Revisa el mensaje anterior.
exit /b 1

:missingMaven
echo ERROR: instala Maven 3.9+ y anade mvn al PATH.
exit /b 1

:missingJpackage
echo ERROR: instala JDK 21 y anade su carpeta bin al PATH.
exit /b 1

:missingWix
echo ERROR: falta WiX Toolset 3.x (candle.exe y light.exe) en el PATH.
echo Para crear una carpeta portable ejecuta: empaquetar-exe.bat portable
exit /b 1
