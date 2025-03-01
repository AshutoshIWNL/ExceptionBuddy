# ExceptionBuddy

ExceptionBuddy is a powerful Java agent that provides comprehensive exception monitoring and logging capabilities by instrumenting Java's exception handling at runtime. It captures all exceptions that occur within the JVM, including those that are caught and suppressed, making it an invaluable tool for debugging and monitoring Java applications.

## Features

- **Complete Exception Capture**: Captures all exceptions in the JVM, including caught, suppressed, and initialization errors
- **Non-invasive Implementation**: Works without modifying your application code through Java instrumentation
- **Configurable Filtering**: Support for custom exception filters to focus on exceptions that are invoked from interested classes
- **Detailed Class Loading Tracing**: Optional tracing of class loader hierarchies for debugging class loading issues
- **Low Overhead**: Minimal performance impact on your running application
- **Runtime Exception Monitoring**: Real-time monitoring of exception patterns and frequencies
- **Runtime Attach**: Supports attach at Runtime through Attach API

## How It Works

ExceptionBuddy uses the Java Instrumentation API to modify the bytecode of `java.lang.Throwable` at runtime. By instrumenting the base class of all exceptions, it ensures that every exception instance, regardless of where or how it occurs, is captured and logged. This approach provides several advantages:

- Catches exceptions before they're caught by application code
- Records exceptions that might be suppressed or swallowed
- Maintains a complete audit trail of exception occurrences

## Getting Started

### Prerequisites

- Java 8 or higher
- A Java application you want to monitor

### Installation

1. Download the latest ExceptionBuddy JAR from the releases page
2. Add the agent to your Java command line:

```bash
java -javaagent:C:\tools\ExceptionBuddy-1.0-SNAPSHOT.jar=configurationFile=C:\tools\ebConfig.json -jar your-application.jar
```

### Configuration

Create a `ebConfig.json` file with your desired settings:

```properties
useFilters: Whether to use the filters to limit the exceptions that are dumped - (true/false)
filters: Define what filters to use - (org.apache.log4j)
logFilePath: Path of the log file that the agent will use - (C:\\eb.log)
classLoadTracing: Prints classloading information - (true/false)
exceptionMonitoring: Prints exception statistics every 60 seconds - (true/false)
cnfSkipString: This is a special property which helps avoid false positve ClassNotFoundExceptions and is JDK version dependent - (java.lang.ClassLoader.loadClass(ClassLoader.java:411))
"printJVMSysProps" : Prints current JVM system properties - (true/false)
"printEnvironmentVariables": Prints environment variables picked up by the JVM - (true/false)
```

Example:
```properties
{
  "useFilters": true,
  "filters": [
    "org.apache.logging.log4j"
  ],
  "logFilePath": "C:\\tools\\eb.log",
  "classLoaderTracing" : true,
  "exceptionMonitoring" : false,
  "cnfSkipString" : "java.lang.ClassLoader.loadClass(ClassLoader.java:406)",
  "printJVMSysProps" : true,
  "printEnvironmentVariables": true
}
```

## Building from Source

```bash
git clone https://github.com/AshutoshIWNL/ExceptionBuddy.git
cd ExceptionBuddy
mvn clean package
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.


## Author

Ashutosh Mishra 【=◈︿◈=】

## Support and Feedback

- Create an issue for bug reports or feature requests
- For general questions, please use the Discussions tab

## Acknowledgments

- Special thanks to the Javassist team for their excellent bytecode manipulation library
