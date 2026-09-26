# JConsole Booster

[![CI](https://github.com/DenissLarka/jconsole-booster/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/DenissLarka/jconsole-booster/actions/workflows/ci.yml)
[![License: GPLv2 + Classpath](https://img.shields.io/badge/license-GPLv2%20%2B%20Classpath-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25%2B-orange.svg)](https://openjdk.org/)
[![Platform](https://img.shields.io/badge/platform-Windows%20%7C%20macOS%20%7C%20Linux-lightgrey.svg)](https://druvu.com/downloads/jconsole-booster.html)
[![Release](https://img.shields.io/github/v/release/DenissLarka/jconsole-booster)](https://github.com/DenissLarka/jconsole-booster/releases)

**A maintained, modern JConsole fork for JVM ops engineers who rely on JMX in production.**

[![Download for Windows](https://img.shields.io/badge/Windows-.msix-0078D4?logo=windows&logoColor=white)](https://github.com/DenissLarka/jconsole-booster/releases/latest/download/JConsoleBooster.msix)
[![Download for macOS](https://img.shields.io/badge/macOS-.dmg-000000?logo=apple&logoColor=white)](https://github.com/DenissLarka/jconsole-booster/releases/latest/download/JConsoleBooster.dmg)
[![Download for Linux](https://img.shields.io/badge/Linux-.AppImage-FCC624?logo=linux&logoColor=black)](https://github.com/DenissLarka/jconsole-booster/releases/latest/download/JConsoleBooster.AppImage)
[![All downloads](https://img.shields.io/badge/druvu.com-downloads-4147d5)](https://druvu.com/downloads/jconsole-booster.html)

The Java runtime is bundled — nothing else to install.

## What you get

JConsole Booster is an OpenJDK 25 fork of the standard JConsole, modernised with a Nimbus look-and-feel, configurable color theming, and a markup system that turns plain `MBeanInfo` descriptions into rich form widgets — dropdowns, date pickers, file pickers, multi-line areas. Connection bookmarks, parameter persistence, and a MIME-aware return handler round out the upgrade.

The markup system is **fully opt-in**: servers that don't add `{{...}}` markup to their descriptions behave exactly as they would under vanilla JConsole. Adding markup is safe — every other JMX client (JConsole, VisualVM, Mission Control) just shows the description as plain text.

- **Markup-driven Operations form.** Embed `{{combo:EUR,USD,GBP}}`, `{{date:dd.MM.yyyy}}`, `{{text:rows=10}}`, or `{{file:*.csv}}` in `MBeanParameterInfo.getDescription()` and the right widget renders automatically. Server-side opt-in; non-aware tooling shows the description verbatim.
- **MIME-aware `byte[]` returns.** `{{returns:mime=application/pdf}}` on an operation makes the result open in your PDF viewer instead of showing as a hex blob. Whitelist of safe content types; everything else falls through to a save dialog with an explicit warning.
- **Per-parameter value persistence.** Last-used parameter values are remembered per `(MBean class, operation, parameter)` and pre-filled on next open. Stop retyping the same JSON payload forty times per debugging session.
- **Connection bookmarks menu.** A hand-editable text file at `~/.druvu.com/jconsole-booster/connections.txt` populates a bookmarks menu with grouping, bold/colored items, and the same `host:port` shorthand you use on the command line. Syncable across machines via Dropbox / git / your tool of choice.
- **Smarter `TabularData` viewer.** `TabularData` results are sorted by the columns named in JMX-canonical `TabularType.getIndexNames()`, with key columns rendered in italics. No more 200-row scroll-fests in insertion order.
- **Modern Nimbus UI with one-line color theming.** Cross-platform consistent rendering; pass `-c=#RRGGBBAA` on launch to set the theme color.
- **JMXMP transport.** Single TCP port, tunnel-friendly (perfect for SSH-forwarded production debugging), no RMI dynamic-port surprises through firewalls.
- **OpenJDK 25 ready, JPMS module.** Runs on the latest JDK; ships as a proper module (`com.druvu.jconsole`).

![JConsole Booster connected to a JVM with a custom color theme](docs/images/hero.png)

## Quick start

Launch JConsole Booster against a JVM you've prepared with JMXMP enabled (see the next section):

```
jconsole-booster localhost:7091
```

The bare `host:port` form expands to `service:jmx:jmxmp://host:port`. Multiple targets connect in tiled MDI panels:

```
jconsole-booster localhost:7091 prod-host:7091 staging-host:7091
```

Apply a custom color theme:

```
jconsole-booster -c=#5B9BD5 localhost:7091
```

## Configuring your JVM

JConsole Booster speaks **JMXMP** (JMX Messaging Protocol) by default — the bare `host:port` shorthand expands to `service:jmx:jmxmp://…`, and full `service:jmx:` URLs are passed through unchanged. JMXMP is the default deliberately, for properties that matter in production:

- **Single TCP port.** No RMI second-port dynamics; firewall rules are one line.
- **Tunnel-friendly.** `ssh -L 7091:localhost:7091 prod-host` and you are done.
- **No registry round-trip.** One connection, one socket.

The standard `-Dcom.sun.management.jmxremote.port=…` system property starts an **RMI** connector, not JMXMP, and will not work with JConsole Booster's `host:port` shorthand. Local-process attach is intentionally unsupported — every connection is an explicit JMX URL, and there are no surprises about which JVM you just connected to.

To start a JMXMP connector server in your target JVM, add `jmxremote_optional` to the classpath and start the connector explicitly:

```java
JMXConnectorServerFactory.newJMXConnectorServer(
    new JMXServiceURL("service:jmx:jmxmp://0.0.0.0:7091"),
    null,
    ManagementFactory.getPlatformMBeanServer()
).start();
```

For the server side, use [druvu-lib-jmxmp](https://github.com/DenissLarka/druvu-lib-jmxmp) — the maintained, modular JMXMP implementation developed alongside JConsole Booster (TLS by default, mandatory authentication, JPMS module; on Maven Central):

```xml
<dependency>
    <groupId>com.druvu</groupId>
    <artifactId>druvu-lib-jmxmp</artifactId>
    <version>2.0.0</version>
</dependency>
```

It enforces authentication and encrypts by default, so the bare-bones snippet above gains a couple of lines — see [its README](https://github.com/DenissLarka/druvu-lib-jmxmp#readme) for the complete server setup. The snippet as shown runs against the classic unmaintained repackage (`org.glassfish.main.external:jmxremote_optional-repackaged:5.0`), which JConsole Booster also connects to — the wire protocol is the same.

## Markup reference

JConsole Booster scans `MBeanParameterInfo.getDescription()` and `MBeanOperationInfo.getDescription()` for a small markup vocabulary in `{{tag:options}}` form. A description may contain at most one tag; everything outside the `{{...}}` is treated as the human-readable description and shown as the tooltip — the markup itself is stripped, never leaked into the UI.

### Parameter widgets

| Tag                                    | Widget                              | Example                                          |
|----------------------------------------|-------------------------------------|--------------------------------------------------|
| `{{combo:A,B,C}}`                      | Dropdown of fixed values            | `"Currency pair {{combo:EURUSD,USDCHF,GBPUSD}}"` |
| `{{date:format}}`                      | Date picker                         | `"Settle date {{date:dd.MM.yyyy}}"`              |
| `{{text}}` / `{{text:rows=N}}`         | Multi-line text area (default 8×60) | `"Payload {{text:rows=10}}"`                     |
| `{{file}}` / `{{file:*.csv,*.json}}`   | File picker                         | `"Upload {{file:*.csv}}"`                        |

`boolean`-typed parameters render as checkboxes automatically — no markup needed.

`{{file}}` reads the picked file as bytes when the parameter type is `byte[]`, or as a UTF-8 string when the parameter type is `String`. The filter pattern is comma-separated globs.

`{{combo}}` values cannot themselves contain commas (commas are the value separator).

![Operations tab with markup widgets](docs/images/operations-markup.png)
*An operation parameter described as `"Currency pair {{combo:EURUSD,USDCHF,GBPUSD}}"` renders as a dropdown — the markup itself is invisible, only the prose remains in the tooltip.*

### Operation result hints

| Tag                       | Effect                                              | Example                                             |
|---------------------------|-----------------------------------------------------|-----------------------------------------------------|
| `{{returns:format=json}}` | Pretty-print the result in a monospace area         | `"Server config {{returns:format=json}}"`           |
| `{{returns:mime=<type>}}` | Open `byte[]` result with the OS handler            | `"Monthly report {{returns:mime=application/pdf}}"` |

`{{returns:format=json}}` applies when the return type is `String` / `CharSequence`. Failed parse falls back to the raw string with a one-line warning above the area.

`{{returns:mime=...}}` applies when the return type is `byte[]`. The whitelist for auto-open is:

```
application/pdf       application/json      application/xml
application/zip       text/plain            text/csv
text/html             image/png             image/jpeg
image/gif             image/svg+xml
```

Anything outside the whitelist triggers a confirmation dialog warning that files of unknown type may be unsafe; on confirm, a `JFileChooser` is shown with a suggested extension.

### Example: a markup-aware MBean

To carry custom descriptions into `MBeanInfo`, use a `StandardMBean` subclass that overrides `getDescription(...)`. With a plain interface-based Standard MBean, JMX introspection generates default descriptions like `p1`, `p2`, … and your markup never reaches the wire.

```java
public class OrderService extends StandardMBean implements OrderServiceMBean {

    public OrderService() throws NotCompliantMBeanException {
        super(OrderServiceMBean.class);
    }

    @Override
    protected String getDescription(MBeanOperationInfo op, MBeanParameterInfo p, int seq) {
        return switch (op.getName()) {
            case "setPair"     -> "Currency pair {{combo:EURUSD,USDCHF,GBPUSD,USDJPY}}";
            case "scheduleAt"  -> "Run-at date {{date:dd.MM.yyyy}}";
            case "uploadCsv"   -> "CSV file {{file:*.csv}}";
            default            -> super.getDescription(op, p, seq);
        };
    }

    @Override
    protected String getDescription(MBeanOperationInfo op) {
        return switch (op.getName()) {
            case "generateReport" -> "Monthly report {{returns:mime=application/pdf}}";
            case "getConfig"      -> "Server config {{returns:format=json}}";
            default               -> super.getDescription(op);
        };
    }

    public String setPair(String pair) { /* ... */ }
    public String scheduleAt(String date) { /* ... */ }
    public String uploadCsv(byte[] payload) { /* ... */ }
    public byte[] generateReport() { /* ... */ }
    public String getConfig() { /* ... */ }
}
```

A `DynamicMBean` that hand-builds `MBeanInfo` with explicit `MBeanParameterInfo(name, type, description)` is the alternative.

## Connection bookmarks

The bookmarks menu is populated from a plain text file:

```
~/.druvu.com/jconsole-booster/connections.txt
```

A default file is written on the first launch with documented examples. Format:

```
# Comments start with #. Empty lines are ignored.

[PRODUCTION]
order-service@prod-orders:7091
*high-traffic*@prod-mkt:7091
[red ALERT host]@prod-edge:7091
---
billing@prod-billing:7091

[STAGING]
order-service@staging-orders:7091
billing@staging-billing:7091

[LOCAL]
local@localhost:7091
```

| Line                            | Effect                          |
|---------------------------------|---------------------------------|
| `[GROUP NAME]`                  | Submenu header                  |
| `name@host:port`                | Menu item                       |
| `*name*@host:port`              | Bold menu item                  |
| `[<color> name]@host:port`      | Colored menu item               |
| `---`                           | Separator within a group        |

Allowed colors: `red`, `blue`, `green`, `orange`, `gray`, `black`, `purple`. Unknown color names render verbatim with a one-line `WARN` log.

URLs accept the same shorthand the rest of the app accepts: `host:port` is expanded to JMXMP, full `service:jmx:…` URLs are passed through unchanged. Malformed lines log a warning naming the line number rather than failing silently.

![Connection bookmarks menu](docs/images/bookmarks-menu.png)
*A `connections.txt` with grouping, bold items, and inline color tags rendered into the menu.*

## Files & paths

JConsole Booster keeps its state in a single hidden vendor directory under your home:

```
~/.druvu.com/jconsole-booster/
├── connections.txt                                       ← bookmarks
└── operation-state/
    └── <fully.qualified.MBeanClassName>.properties      ← last-used parameter values
```

Cross-platform without conditionals — `~` (i.e. `System.getProperty("user.home")`) resolves correctly on macOS, Windows, and Linux. The directory is created lazily on first need; deleting it resets that state without breaking the app.

To relocate the directory (e.g. point it at a Dropbox / iCloud / OneDrive synced path), set the **`JCONSOLE_BOOSTER_HOME`** environment variable. If set, it overrides the default path entirely.

## CLI reference

```
jconsole-booster [options] [target ...]
```

### Targets

| Form                       | Expands to                                     |
|----------------------------|------------------------------------------------|
| `host:port`                | `service:jmx:jmxmp://host:port`                |
| `service:jmx:…`            | passed through unchanged (JMXMP, RMI, custom)  |

Multiple targets open in tiled MDI panels (use `-notile` to disable). Bare process IDs are not supported — every target is an explicit URL.

### Options

| Flag                | Description                                                        |
|---------------------|--------------------------------------------------------------------|
| `-c=#RRGGBB[AA]`    | Apply a Nimbus color theme (sets the `nimbusBlueGrey` base color). |
| `-interval=N`       | Refresh interval in seconds. Default: `4`.                         |
| `-notile`           | Don't tile windows when multiple targets are passed.               |
| `-debug`            | Enable debug logging.                                              |
| `-version`          | Print version and exit.                                            |
| `-fullversion`      | Print full version (with build metadata) and exit.                 |
| `-h`, `-help`, `-?` | Print usage and exit.                                              |
| `--console`         | Interactive command-line mode (no GUI). See below.                 |
| `-e=<cmd>`          | Run a console command non-interactively (repeatable); implies `--console`. |
| `-u=<user>`         | Username for `-e` script-mode connect (password read from stdin).  |

### Console mode

`--console` drops into a headless text REPL instead of the GUI — connect over JMXMP with the **same** adaptive-TLS, trust-on-first-use and credential-over-plaintext protection as the GUI, browse MBeans, and invoke operations. Handy over SSH or on a bastion where no display is available (no existing CLI JMX tool speaks JMXMP + TLS).

The drill-down is numbered: pick a bean, pick an operation, and you're prompted for each argument by name and type. Commands: `open [--strict] <target> [user]`, `beans [filter]`, `bean <n|objectName>`, `ops`, `call <n|opName> [args…]`, `invoke <objectName> <op> [args…]`, `threads [file]`, `close`, `version`, `help`, `quit`.

```
# interactive — connect, drill down, invoke
jconsole-booster --console
jcb> open localhost:7091 admin
jcb> beans Cache
jcb> bean com.example:type=Cache
jcb Cache> call clear

# one-shot / scriptable — exit 0 on success, 1 on failure
echo mypassword | jconsole-booster -u=admin \
    -e="invoke com.example:type=Cache clear" localhost:7091
```

**Thread dumps.** `threads` is the console counterpart of the GUI's Threads tab — it writes a full dump of the **target** JVM to a file on the **local** machine (the one running the console, not the one being dumped), so it works unchanged through an SSH tunnel from a bastion:

```
jcb> threads
thread dump written to /home/ops/threaddump-prod--7091-20260824-134705.txt (19758 chars, via …)

# unattended capture into a known path
echo mypassword | jconsole-booster -u=admin --strict \
    -e="threads /var/log/incident-4711.txt" prod-host:7091
```

With no argument the file lands in the current directory as `threaddump-<target>-<timestamp>.txt`. On HotSpot targets the content is genuine `jstack -l` output — deadlock analysis, native ids and lock sections included — because the dump is taken through the `Thread.print` diagnostic command; any thread-dump analyzer reads it as-is. On non-HotSpot targets it falls back to `ThreadMXBean.dumpAllThreads` and rebuilds the same jstack shape, with full stacks (no 8-frame truncation) and a deadlock summary.

For fully unattended use, pin the server certificate and pass `--strict` so no trust prompt blocks on stdin.

## JConsole Booster vs vanilla JConsole

|                            | Vanilla JConsole                                        | JConsole Booster                                                       |
|----------------------------|---------------------------------------------------------|------------------------------------------------------------------------|
| Look-and-feel              | OS-default (often dated)                                | Nimbus, cross-platform consistent                                      |
| Color theming              | None                                                    | `-c=#RRGGBBAA`                                                         |
| Operations form            | Plain text fields only                                  | `{{markup}}` → dropdowns, date pickers, file pickers, multi-line areas |
| `byte[]` operation returns | `[B@1a2b3c]`                                            | MIME-aware open / save with extension hint                             |
| `TabularData` viewer       | Insertion order, no key cue                             | Sorted by `TabularType.getIndexNames()`, italic key columns            |
| Parameter persistence      | None                                                    | Last-used values per `(MBean class, op, param)`                        |
| Connection bookmarks       | None                                                    | Text-file driven, groupable, colorable                                 |
| Transport                  | RMI (multi-port, hostile to firewalls)                  | JMXMP (single port, tunnel-friendly)                                   |
| JDK                        | Bundled with JDK 8/11/17 (deprecated and removed in 9+) | OpenJDK 25 fork, modern Java                                           |
| Local-process attach       | Yes                                                     | No (explicit URLs only — no surprise connections)                      |

## Compatibility

- **Java runtime.** OpenJDK 25 or later. Bundled with the installer — no separate install needed.
- **Operating systems.**
  - Windows 10 / 11 (x64)
  - macOS 12 Monterey or later (Apple Silicon; Intel via build-from-source)
  - Linux x64 (any modern glibc-based distribution)
- **Target JVMs.** Any JVM exposing JMX over JMXMP — JDK 8 through latest. Markup features require the target's MBeans to populate `MBeanInfo` descriptions accordingly (`StandardMBean` subclass overriding `getDescription(...)`, or a `DynamicMBean` hand-building `MBeanInfo`).
- **JPMS.** Ships as the `com.druvu.jconsole` module.

## Maven artifact (for embedding / extending)

The `com.druvu:jconsole-booster` JAR is published to **GitHub Packages**. This is only relevant if you want to embed JConsole Booster in another Maven project or write a JConsole plugin against its APIs — end users should use the installers above.

GitHub Packages requires authentication even for public packages. Generate a [Personal Access Token](https://github.com/settings/tokens) with the `read:packages` scope, then add a server entry to `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_PAT_WITH_read:packages</password>
    </server>
  </servers>
</settings>
```

Add the repository and dependency to your consumer project's `pom.xml`:

```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/DenissLarka/jconsole-booster/</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.druvu</groupId>
  <artifactId>jconsole-booster</artifactId>
  <version>1.1.0</version>
</dependency>
```

## Which direction next?

If JMX is part of your day, I'd genuinely like to hear what still fights you — [open an issue](https://github.com/DenissLarka/jconsole-booster/issues/new). Bug reports, questions and ideas are all welcome; **Help → Feedback** inside the app lands in the same place.

## Building from source

Requires JDK 25+ and Maven 3.9+.

```bash
git clone https://github.com/DenissLarka/jconsole-booster.git
cd jconsole-booster
mvn clean package
mvn exec:exec@start
```

### Single-jar build (no installer)

An opt-in profile shades everything into one plain (non-JPMS) executable jar — handy to `scp` onto a server or bastion and use [console mode](#console-mode) where no installer or display is available:

```bash
mvn -Puberjar package
java -jar target/dist/jconsole-booster.jar            # GUI
java -jar target/dist/jconsole-booster.jar --console  # headless REPL
```

Requires a full JDK 25+ at runtime (the jar does not bundle one, unlike the installers).

## License

JConsole Booster is licensed under the **GNU General Public License v2 with the Classpath Exception**, inherited from upstream OpenJDK JConsole. See [LICENSE](LICENSE) for the full text.

---

JConsole Booster is a [druvu](https://druvu.com) product.
