@echo off

REM  Licensed to the Apache Software Foundation (ASF) under one or more
REM  contributor license agreements.  See the NOTICE file distributed with
REM  this work for additional information regarding copyright ownership.
REM  The ASF licenses this file to You under the Apache License, Version 2.0
REM  (the "License"); you may not use this file except in compliance with
REM  the License.  You may obtain a copy of the License at
REM 
REM      http://www.apache.org/licenses/LICENSE-2.0
REM 
REM  Unless required by applicable law or agreed to in writing, software
REM  distributed under the License is distributed on an "AS IS" BASIS,
REM  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
REM  See the License for the specific language governing permissions and
REM  limitations under the License.

REM environment variable changes local to the batch file
if "%OS%" == "Windows_NT" setlocal

REM load user ant configuration
if exist "%USERPROFILE%\.easyant\easyant_pre.bat" call "%USERPROFILE%\.easyant\easyant_pre.bat"

rem By default we use the ant distribution shipped with easyant
set _USE_CUSTOM_ANT=no

if not exist "%EASYANT_HOME%\bin\easyant.bat" goto noEasyAntHome
goto run

:noEasyAntHome
echo EASYANT_HOME is set incorrectly or easyant could not be located. Please set EASYANT_HOME.
goto end

:run
rem set the default parameters for easyant
set EASYANT_ARGS=-lib "%EASYANT_HOME%\lib" -main org.apache.easyant.core.EasyAntMain %EASYANT_ARGS%
set EASYANT_OPTS=-Deasyant.home="%EASYANT_HOME%" %EASYANT_OPTS%


rem This part is fully inspirated by ant's script with some easyant customisation

if "%HOME%"=="" goto homeDrivePathPre
if exist "%HOME%\antrc_pre.bat" call "%HOME%\antrc_pre.bat"

:homeDrivePathPre
if "%HOMEDRIVE%%HOMEPATH%"=="" goto userProfilePre
if "%HOMEDRIVE%%HOMEPATH%"=="%HOME%" goto userProfilePre
if exist "%HOMEDRIVE%%HOMEPATH%\antrc_pre.bat" call "%HOMEDRIVE%%HOMEPATH%\antrc_pre.bat"

:userProfilePre
if "%USERPROFILE%"=="" goto alpha
if "%USERPROFILE%"=="%HOME%" goto alpha
if "%USERPROFILE%"=="%HOMEDRIVE%%HOMEPATH%" goto alpha
if exist "%USERPROFILE%\antrc_pre.bat" call "%USERPROFILE%\antrc_pre.bat"

:alpha

if "%OS%"=="Windows_NT" @setlocal
if "%OS%"=="WINNT" @setlocal



rem Slurp the command line arguments. This loop allows for an unlimited number
rem of arguments (up to the command line limit, anyway).
:setupArgs
if ""%1""=="""" goto doneStart
if ""%1""==""--use-custom-ant"" goto usecustomant
if ""%1""==""-noclasspath"" goto clearclasspath
set ANT_CMD_LINE_ARGS=%ANT_CMD_LINE_ARGS% %1
shift
goto setupArgs

rem here is there is a -noclasspath in the options
:clearclasspath
set _USE_CLASSPATH=no
shift
goto setupArgs

:usecustomant
set _USE_CUSTOM_ANT=yes
shift
goto setupArgs

rem This label provides a place for the argument list loop to break out
rem and for NT handling to skip to.

:doneStart

if "%_USE_CUSTOM_ANT%"=="no" goto useShippedAntDistro

if "%ANT_HOME%"=="" goto setDefaultAntHome

:stripAntHome
if not _%ANT_HOME:~-1%==_\ goto checkClasspath
set ANT_HOME=%ANT_HOME:~0,-1%
goto stripAntHome

:setDefaultAntHome
rem %~dp0 is expanded pathname of the current script under NT
set ANT_HOME=%~dp0..

:useShippedAntDistro
if not "%ANT_HOME%"=="" echo "WARNING:  ANT_HOME environment variable detected. If you want to use this ant installation, you must start easyant with '-use-custom-ant'."
set ANT_HOME=%EASYANT_HOME%
goto findAntHome

:checkClasspath
set _USE_CLASSPATH=yes
rem CLASSPATH must not be used if it is equal to ""
if "%CLASSPATH%"=="""" set _USE_CLASSPATH=no
if "%CLASSPATH%"=="" set _USE_CLASSPATH=no

if "%_USE_CLASSPATH%"=="no" goto findAntHome

:stripClasspath
if not _%CLASSPATH:~-1%==_\ goto findAntHome
set CLASSPATH=%CLASSPATH:~0,-1%
goto stripClasspath

:findAntHome
rem find ANT_HOME if it does not exist due to either an invalid value passed
rem by the user or the %0 problem on Windows 9x
if exist "%ANT_HOME%\lib\ant.jar" goto checkJava

rem check for ant in Program Files
if not exist "%ProgramFiles%\ant" goto checkSystemDrive
set ANT_HOME=%ProgramFiles%\ant
goto checkJava

:checkSystemDrive
rem check for ant in root directory of system drive
if not exist %SystemDrive%\ant\lib\ant.jar goto checkCDrive
set ANT_HOME=%SystemDrive%\ant
goto checkJava

:checkCDrive
rem check for ant in C:\ant for Win9X users
if not exist C:\ant\lib\ant.jar goto noAntHome
set ANT_HOME=C:\ant
goto checkJava

:noAntHome
echo ANT_HOME is set incorrectly or ant could not be located. Please set ANT_HOME.
goto end

:checkJava
set _JAVACMD=%JAVACMD%

if "%JAVA_HOME%" == "" goto noJavaHome
if not exist "%JAVA_HOME%\bin\java.exe" goto noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=%JAVA_HOME%\bin\java.exe
goto checkJikes

:noJavaHome
if "%_JAVACMD%" == "" set _JAVACMD=java.exe

:checkJikes
if not "%JIKESPATH%"=="" goto runAntWithJikes

:runAnt
if "%_USE_CLASSPATH%"=="no" goto runAntNoClasspath
:runAntWithClasspath
"%_JAVACMD%" %ANT_OPTS% %EASYANT_OPTS% -classpath "%ANT_HOME%\lib\ant-launcher.jar" "-Dant.home=%ANT_HOME%" org.apache.tools.ant.launch.Launcher %ANT_ARGS% %EASYANT_ARGS% -cp "%CLASSPATH%" %ANT_CMD_LINE_ARGS%
rem Check the error code of the Ant build
if not "%OS%"=="Windows_NT" goto onError
set ANT_ERROR=%ERRORLEVEL%
goto end

:runAntNoClasspath
"%_JAVACMD%" %ANT_OPTS% %EASYANT_OPTS% -classpath "%ANT_HOME%\lib\ant-launcher.jar" "-Dant.home=%ANT_HOME%" org.apache.tools.ant.launch.Launcher %ANT_ARGS% %EASYANT_ARGS% %ANT_CMD_LINE_ARGS%
rem Check the error code of the Ant build
if not "%OS%"=="Windows_NT" goto onError
set ANT_ERROR=%ERRORLEVEL%
goto end

:runAntWithJikes

if not _%JIKESPATH:~-1%==_\ goto checkJikesAndClasspath
set JIKESPATH=%JIKESPATH:~0,-1%
goto runAntWithJikes

:checkJikesAndClasspath

if "%_USE_CLASSPATH%"=="no" goto runAntWithJikesNoClasspath

:runAntWithJikesAndClasspath
"%_JAVACMD%" %ANT_OPTS% %EASYANT_OPTS% -classpath "%ANT_HOME%\lib\ant-launcher.jar" "-Dant.home=%ANT_HOME%" "-Djikes.class.path=%JIKESPATH%" org.apache.tools.ant.launch.Launcher %ANT_ARGS% %EASYANT_ARGS%  -cp "%CLASSPATH%" %ANT_CMD_LINE_ARGS%
rem Check the error code of the Ant build
if not "%OS%"=="Windows_NT" goto onError
set ANT_ERROR=%ERRORLEVEL%
goto end

:runAntWithJikesNoClasspath
"%_JAVACMD%" %ANT_OPTS% %EASYANT_OPTS% -classpath "%ANT_HOME%\lib\ant-launcher.jar" "-Dant.home=%ANT_HOME%" "-Djikes.class.path=%JIKESPATH%" org.apache.tools.ant.launch.Launcher %ANT_ARGS% %EASYANT_ARGS% %ANT_CMD_LINE_ARGS%
rem Check the error code of the Ant build
if not "%OS%"=="Windows_NT" goto onError
set ANT_ERROR=%ERRORLEVEL%
goto end

:onError
rem Windows 9x way of checking the error code.  It matches via brute force.
for %%i in (1 10 100) do set err%%i=
for %%i in (0 1 2) do if errorlevel %%i00 set err100=%%i
if %err100%==2 goto onError200
if %err100%==0 set err100=
for %%i in (0 1 2 3 4 5 6 7 8 9) do if errorlevel %err100%%%i0 set err10=%%i
if "%err100%"=="" if %err10%==0 set err10=
:onError1
for %%i in (0 1 2 3 4 5 6 7 8 9) do if errorlevel %err100%%err10%%%i set err1=%%i
goto onErrorEnd
:onError200
for %%i in (0 1 2 3 4 5) do if errorlevel 2%%i0 set err10=%%i
if err10==5 for %%i in (0 1 2 3 4 5) do if errorlevel 25%%i set err1=%%i
if not err10==5 goto onError1
:onErrorEnd
set ANT_ERROR=%err100%%err10%%err1%
for %%i in (1 10 100) do set err%%i=

:end
rem bug ID 32069: resetting an undefined env variable changes the errorlevel.
if not "%_JAVACMD%"=="" set _JAVACMD=
if not "%_ANT_CMD_LINE_ARGS%"=="" set ANT_CMD_LINE_ARGS=

if "%ANT_ERROR%"=="0" goto mainEnd

rem Set the return code if we are not in NT.  We can only set
rem a value of 1, but it's better than nothing.
if not "%OS%"=="Windows_NT" echo 1 > nul | choice /n /c:1

rem Set the ERRORLEVEL if we are running NT.
if "%OS%"=="Windows_NT" color 00

goto omega

:mainEnd

rem If there were no errors, we run the post script.
if "%OS%"=="Windows_NT" @endlocal
if "%OS%"=="WINNT" @endlocal

if "%HOME%"=="" goto homeDrivePathPost
if exist "%HOME%\antrc_post.bat" call "%HOME%\antrc_post.bat"

:homeDrivePathPost
if "%HOMEDRIVE%%HOMEPATH%"=="" goto userProfilePost
if "%HOMEDRIVE%%HOMEPATH%"=="%HOME%" goto userProfilePost
if exist "%HOMEDRIVE%%HOMEPATH%\antrc_post.bat" call "%HOMEDRIVE%%HOMEPATH%\antrc_post.bat"

:userProfilePost
if "%USERPROFILE%"=="" goto omega
if "%USERPROFILE%"=="%HOME%" goto omega
if "%USERPROFILE%"=="%HOMEDRIVE%%HOMEPATH%" goto omega
if exist "%USERPROFILE%\antrc_post.bat" call "%USERPROFILE%\antrc_post.bat"

:omega

