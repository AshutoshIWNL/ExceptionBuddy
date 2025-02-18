#!/bin/bash

# Prompt for JAVA_HOME
read -p "Enter JAVA_HOME path: " JAVA_HOME
export JAVA_HOME

# Prompt for agent JAR file
read -p "Enter agent JAR file path: " AGENT_JAR

# Prompt for configuration file
read -p "Enter configuration file path: " CONFIG_FILE

# Prompt for process ID
read -p "Enter process ID: " PID

# Execute the Java command
$JAVA_HOME/bin/java -cp ".:$AGENT_JAR:$JAVA_HOME/lib/tools.jar" com.asm.eb.attach.AgentAttachCLI -agentJar "$AGENT_JAR" -configurationFile "$CONFIG_FILE" -pid "$PID"