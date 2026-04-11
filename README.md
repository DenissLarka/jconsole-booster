# jconsole-booster

OpenJDK 25 JConsole fork with some perks

## Supported connectors

JConsole Booster only speaks **JMXMP**. Target JVMs must explicitly start a
JMXMP connector server: the `jmxremote_optional` jar on the classpath plus an
explicit

```java
JMXConnectorServerFactory.newJMXConnectorServer(
    new JMXServiceURL("service:jmx:jmxmp://0.0.0.0:7091"),
    null,
    ManagementFactory.getPlatformMBeanServer()).start();
```

The standard `-Dcom.sun.management.jmxremote.port=…` flag gives you RMI, not
JMXMP, and will **not** work with this fork. Local-process attach is also not
supported — every connection goes through a JMXMP URL or the `host:port`
shorthand (which expands to `service:jmx:jmxmp://host:port`).
