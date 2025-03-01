@echo off
REM Prompt for JAVA_HOME
set /p JAVA_HOME="Enter JAVA_HOME path: "

REM Prompt for agent JAR file
set /p AGENT_JAR="Enter agent JAR file path: "

REM Prompt for configuration file
set /p CONFIG_FILE="Enter configuration file path: "

REM Prompt for process ID
set /p PID="Enter process ID: "

REM Execute the Java command
"%JAVA_HOME%\bin\java" -cp ".;%AGENT_JAR%;%JAVA_HOME%\lib\tools.jar" com.asm.eb.attach.AgentAttachCLI -agentJar "%AGENT_JAR%" -configurationFile "%CONFIG_FILE%" -pid "%PID%"