@echo off
REM ===================================================================
REM  Genera el instalador .exe de la aplicación de gestión (Windows).
REM  Requisitos: JDK 21 (incluye jpackage) y WiX Toolset 3.x en el PATH
REM  (sin WiX, cambia --type exe por --type app-image: crea una carpeta
REM  con "SF Archive.exe" lista para usar sin instalador).
REM ===================================================================
call mvn -q clean package -DskipTests || exit /b 1
if exist dist rmdir /s /q dist
mkdir build-exe
copy /y desktop-admin\target\sfa-gestion.jar build-exe\ >nul
jpackage --type exe --name "SF Archive" --app-version 1.0.0 ^
  --vendor "San Francisco Archive" --description "Sistema de Gestion de Anomalias" ^
  --input build-exe --main-jar sfa-gestion.jar --main-class com.sfarchive.desktop.Launcher ^
  --dest dist --win-menu --win-shortcut --win-dir-chooser
rmdir /s /q build-exe
echo Instalador generado en dist\
