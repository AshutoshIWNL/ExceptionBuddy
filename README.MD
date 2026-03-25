# ExceptionBuddy

ExceptionBuddy is a Java agent for comprehensive exception monitoring and logging through runtime instrumentation. It captures exceptions across the JVM, including caught exceptions, to improve debugging and production diagnostics.

## Features

- Complete exception capture across the JVM
- Non-invasive instrumentation (no application code changes)
- Configurable stack-trace filtering
- Optional class-loader tracing
- Runtime exception statistics monitoring
- Runtime attach support via Attach API
- Optional JVM system property dump
- Optional environment variable dump

## How It Works

ExceptionBuddy instruments `java.lang.Throwable` using the Java Instrumentation API. By instrumenting the base exception type, it captures exception creation events regardless of where they are later handled.

## Getting Started

### Prerequisites

- Java 8 or higher
- A target JVM application

### Launch-Time Agent

```bash
java -javaagent:C:\tools\ExceptionBuddy-1.0-SNAPSHOT.jar=configurationFile=C:\tools\ebConfig.json -jar your-application.jar
```

### Runtime Attach

```bash
java -cp ExceptionBuddy-1.0-SNAPSHOT.jar com.asm.eb.attach.AgentAttachCLI --agentJar C:\tools\ExceptionBuddy-1.0-SNAPSHOT.jar --configurationFile C:\tools\ebConfig.json --pid <target-jvm-pid>
```

## Configuration

Create `ebConfig.json`:

```json
{
  "useFilters": true,
  "filters": ["org.apache.logging.log4j"],
  "logFilePath": "C:\\tools\\eb.log",
  "classLoaderTracing": false,
  "exceptionMonitoring": false,
  "cnfSkipString": "java.lang.ClassLoader.loadClass(ClassLoader.java:406)",
  "printJVMSysProps": false,
  "printEnvironmentVariables": false
}
```

Notes:
- `classLoaderTracing` can produce high log volume and should be enabled only for focused diagnostics.
- `printJVMSysProps` and `printEnvironmentVariables` can expose secrets; keep them disabled by default in production.
- If `useFilters` is `true`, `filters` must contain at least one non-empty entry.

## Building from Source

```bash
git clone https://github.com/AshutoshIWNL/ExceptionBuddy.git
cd ExceptionBuddy
mvn clean test package
```

## Contributing

Contributions are welcome through issues and pull requests.
