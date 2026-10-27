@echo off
rem ---------------------------------------------------------------------------
rem  Builds a stand-alone Windows version of the PRM80 control software
rem  (program folder with PRM80.exe and its own Java runtime, no Java
rem  installation needed on the target PC).
rem
rem  Before: build the project once ("Clean and Build" in NetBeans or
rem          "mvn package"), so that target\prm80-control-software-*.jar exists.
rem  Result: target\dist\PRM80\PRM80.exe  (copy or zip the whole PRM80 folder)
rem
rem  Optional: "package-windows.bat msi" builds an installer instead,
rem            this needs the WiX Toolset (https://wixtoolset.org).
rem ---------------------------------------------------------------------------
setlocal
cd /d "%~dp0"

set TYPE=app-image
if not "%~1"=="" set TYPE=%~1

set EXTRA=
rem fixed upgrade code: a new version replaces the installed one (do not change)
set UPGRADE_UUID=b01a3249-bb30-47bd-a72b-51498d72a3df
if not "%TYPE%"=="app-image" set EXTRA=--win-menu --win-menu-group PRM80 --win-shortcut --win-dir-chooser --win-upgrade-uuid %UPGRADE_UUID%

rem jpackage of JAVA_HOME, if JAVA_HOME is a JDK (an old JRE in JAVA_HOME,
rem e.g. Java 8, has no jpackage: then the one in the PATH is used)
set JPACKAGE=
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jpackage.exe" set JPACKAGE="%JAVA_HOME%\bin\jpackage.exe"
if not defined JPACKAGE (
    where jpackage >nul 2>nul
    if errorlevel 1 (
        echo jpackage not found - install a JDK 21 or newer and add its bin folder
        echo to the PATH, or set JAVA_HOME to the JDK folder.
        exit /b 1
    )
    set JPACKAGE=jpackage
)

set JAR=
for %%f in (target\prm80-control-software-*.jar) do set JAR=%%~nxf
if "%JAR%"=="" (
    echo target\prm80-control-software-*.jar not found - build the project first.
    exit /b 1
)

rem version from the jar name (= <version> in pom.xml), e.g. 2.0.0
set VERSION=%JAR:prm80-control-software-=%
set VERSION=%VERSION:.jar=%
set VERSION=%VERSION:-SNAPSHOT=%

if exist target\jpackage-input rmdir /s /q target\jpackage-input
if exist target\dist rmdir /s /q target\dist
mkdir target\jpackage-input
copy "target\%JAR%" target\jpackage-input\ >nul

%JPACKAGE% --type %TYPE% --name PRM80 --app-version %VERSION% --vendor F4FEZ ^
    --description "PRM80 control software" --icon packaging\PRM80.ico ^
    --input target\jpackage-input --main-jar %JAR% ^
    --main-class f4fez.prm80x0.PRM80X0App ^
    --add-modules java.base,java.desktop,java.logging,java.prefs ^
    --jlink-options "--strip-debug --no-header-files --no-man-pages" ^
    --java-options "--enable-native-access=ALL-UNNAMED" ^
    %EXTRA% --dest target\dist
if errorlevel 1 exit /b 1

echo.
echo Done: target\dist (version %VERSION%)
