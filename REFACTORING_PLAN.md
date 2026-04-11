# JConsole Booster — Refactoring Plan

## Goal

Split the flat `com.druvu.jconsole` package into well-separated sub-packages with clear
boundaries between JMX protocol logic, UI rendering, and orchestration. Introduce
abstractions so that UI classes do not directly depend on concrete JMX implementation
classes.

**Rule**: after every phase, `mvn clean package` must succeed before starting the next.

---

## Target Package Layout

```
com.druvu.jconsole/
├── launcher/        JConsole (JFrame+main), ArgumentParser, JConsoleOptions
├── jmx/             JMXConnectionManager (NEW), ProxyClient, LocalVirtualMachine,
│                    MemoryPoolProxy, MemoryPoolStat
├── jmx/api/         JmxDataAccess (NEW interface)
├── ui/
│   ├── core/        VMPanel, VMInternalFrame, Tab (abstract), VMUpdateCoordinator (NEW)
│   ├── tabs/        MemoryTab, ThreadTab, ClassTab, MBeansTab, SummaryTab, OverviewTab
│   ├── dialogs/     ConnectDialog, AboutDialog, CreateMBeanDialog, InternalDialog,
│   │                SheetDialog
│   ├── graphics/    Plotter, PlotterPanel, OverviewPanel
│   └── components/  BorderedComponent, HTMLPane, LabeledComponent,
│                    MaximizableInternalFrame, OutputViewer, TimeComboBox,
│                    VariableGridLayout
├── inspector/       (existing classes stay; MBeanService interface added)
│   └── api/         MBeanService (NEW interface)
├── plugins/         ExceptionSafePlugin
│   └── jtop/        (unchanged)
├── extra/           JConsoleEx (unchanged)
└── util/            Worker, Formatter, Resources, Messages, Version, Utilities
```

---

## New Abstractions Summary

| New type | Kind | Extracted from | Purpose |
|---|---|---|---|
| `jmx.api.JmxDataAccess` | interface | `ProxyClient` (methods) | Replaces `VMPanel` reference in `Tab`; lets tabs access JMX data without knowing about `ProxyClient` |
| `jmx.JMXConnectionManager` | class | `ProxyClient` (connection setup) | Pure JMX: connector creation, SSL check, RMI stub validation — no Swing |
| `ui.core.VMUpdateCoordinator` | class | `VMPanel` (timer + worker loop) | Owns `Timer`, schedules per-tab and per-plugin `SwingWorker`s |
| `inspector.api.MBeanService` | interface | `MBeansTab` (connection pass-through) | Lets inspector classes (`XTree`, `XSheet`, `XMBean`) talk to the MBean server without a hard dependency on `MBeansTab` |

---

## Phase 1 — Mechanical Package Split (no logic changes)

**Goal**: Create the target sub-packages, move files, fix imports. No behavioral change.

### Steps

#### 1.1 — Create `util` package

Move to `com.druvu.jconsole.util`:

- `Worker`
- `Formatter`
- `Resources`
- `Messages`
- `Version`

> `Utilities` is intentionally **not** moved here yet — it imports `inspector.XTree`
> (see Phase 1.6). Move it only after that coupling is resolved.

#### 1.2 — Create `model` package inside `jmx`

Move to `com.druvu.jconsole.jmx`:

- `MemoryPoolStat` (pure value object, no deps)

`JConsoleOptions` stays in the root for now — it is referenced by `ArgumentParser` and
`JConsoleEx` from `extra`, so moving it will cascade into Phase 5 cleanly.

#### 1.3 — Create `ui/components` package

Move to `com.druvu.jconsole.ui.components`:

- `BorderedComponent`
- `HTMLPane`
- `LabeledComponent`
- `MaximizableInternalFrame`
- `OutputViewer`
- `TimeComboBox`
- `VariableGridLayout`

#### 1.4 — Create `ui/dialogs` package

Move to `com.druvu.jconsole.ui.dialogs`:

- `InternalDialog`
- `AboutDialog`
- `SheetDialog`
- `CreateMBeanDialog`

> `ConnectDialog` references `LocalVirtualMachine` and will be moved later
> (Phase 1.7) once the `jmx` package exists.

#### 1.5 — Create `ui/graphics` package

Move to `com.druvu.jconsole.ui.graphics`:

- `Plotter`
- `PlotterPanel`
- `OverviewPanel`

#### 1.6 — Fix `Utilities` → `XTree` coupling, then move `Utilities`

`Utilities.setTransparency()` has a special case for `XTree` (sets cell renderer
background to near-transparent for Windows LAF). This is the only cross-package
coupling that blocks moving `Utilities` to `util`.

**Fix**: override `setOpaque(boolean)` in `XTree` to self-apply the cell renderer
tweak when called with `false`. Remove the `XTree` branch from `Utilities`.

```java
// XTree.java — add:
@Override
public void setOpaque(boolean isOpaque) {
    super.setOpaque(isOpaque);
    if (!isOpaque) {
        DefaultTreeCellRenderer cr = (DefaultTreeCellRenderer) getCellRenderer();
        cr.setBackground(null);
        cr.setBackgroundNonSelectionColor(new Color(0, 0, 0, 1));
        setCellRenderer(cr);
    }
}
```

Then remove the `if (child instanceof XTree)` branch from `Utilities.setTransparency()`
and its import of `XTree`.

After this change, move `Utilities` to `com.druvu.jconsole.util`.

#### 1.7 — Create `jmx` package

Move to `com.druvu.jconsole.jmx`:

- `LocalVirtualMachine`
- `MemoryPoolProxy`

> `ProxyClient` is **not** moved yet — it is too deeply referenced everywhere.
> It will move in Phase 2 after its internal structure is clarified.

#### 1.8 — Create `ui/tabs` package

Move to `com.druvu.jconsole.ui.tabs`:

- `MemoryTab`
- `ThreadTab`
- `ClassTab`
- `MBeansTab`
- `SummaryTab`
- `OverviewTab`

#### 1.9 — Create `ui/core` package

Move to `com.druvu.jconsole.ui.core`:

- `Tab`
- `VMPanel`
- `VMInternalFrame`

#### 1.10 — Create `launcher` package

Move to `com.druvu.jconsole.launcher`:

- `JConsole` (the JFrame + `main()` entry point)
- `ArgumentParser`
- `JConsoleOptions`

Move `ConnectDialog` to `com.druvu.jconsole.ui.dialogs` now (after `jmx` package
exists for `LocalVirtualMachine`).

#### 1.11 — Update `module-info.java`

No new exports are required (only `extra` is exported). Add any necessary `opens`
directives if reflection is used across packages. Verify no split-package issues.

#### Verification

```bash
mvn clean package
```

All imports update is mechanical — IDE refactor or `sed` over `*.java`. The build
must be green before proceeding.

---

## Phase 2 — Extract `JMXConnectionManager` from `ProxyClient`

**Goal**: Give `ProxyClient` a pure-JMX helper that handles all connector creation,
SSL checks, and RMI stub validation with zero Swing dependency.

### What stays in `ProxyClient`

- `SwingPropertyChangeSupport` + `addPropertyChangeListener` / `removePropertyChangeListener`
- `implements JConsoleContext` (JDK interface — `getConnectionState()`, `getMBeanServerConnection()`)
- MXBean proxy cache fields and getters (`getMemoryMXBean()` etc.)
- `SnapshotMBeanServerConnection` inner class
- `hasPlatformMXBeans`, `hasHotSpotDiagnosticMXBean`, etc. flags
- Public `connect()` / `disconnect()` that delegate to `JMXConnectionManager`

### What moves to `JMXConnectionManager`

- `checkStub(Remote, Class)` — RMI stub security check (static utility)
- `checkSslConfig()` — SSL/RMI registry detection
- The RMI connector creation and SSL negotiation logic inside `connect()`
- Local VM attach logic (`lvm.startManagementAgent()` path inside `connect()`)
- `findLocalVirtualMachine()` static lookup

### New class skeleton

```java
// com.druvu.jconsole.jmx.JMXConnectionManager
package com.druvu.jconsole.jmx;

/** Pure JMX — no Swing. Creates JMXConnector for different connection types. */
public final class JMXConnectionManager {

    /** Result record — everything ProxyClient needs after a successful connect. */
    public record ConnectionResult(
        JMXConnector connector,
        MBeanServerConnection connection,
        boolean hasPlatformMXBeans,
        boolean hasHotSpotDiagnosticMXBean,
        boolean hasCompilationMXBean,
        boolean supportsLockUsage
    ) {}

    public static ConnectionResult connect(LocalVirtualMachine lvm) throws IOException { ... }
    public static ConnectionResult connect(JMXServiceURL url, String user, String pass,
                                            boolean sslRegistry) throws IOException { ... }

    private static void checkStub(Remote stub, Class<? extends Remote> stubClass) { ... }
    private static boolean checkSslConfig(String host, int port) { ... }
}
```

`ProxyClient.connect()` becomes:

```java
public void connect() {
    JMXConnectionManager.ConnectionResult result = ...connect...;
    this.jmxc  = result.connector();
    this.mbsc  = result.connection();
    this.server = new SnapshotMBeanServerConnection(result.connection());
    this.hasPlatformMXBeans = result.hasPlatformMXBeans();
    // ... set other flags ...
    setConnectionState(ConnectionState.CONNECTED); // fires Swing event
}
```

### Steps

1. Create `com/druvu/jconsole/jmx/JMXConnectionManager.java` with the skeleton above.
2. Extract `checkStub` into it (direct copy, make static).
3. Extract `checkSslConfig` into it (direct copy).
4. Extract connector-creation logic from `ProxyClient.connect()` into
   `JMXConnectionManager.connect(...)`, return `ConnectionResult`.
5. Simplify `ProxyClient.connect()` to call `JMXConnectionManager.connect(...)`.
6. Move `ProxyClient` itself to `com.druvu.jconsole.jmx` package.

#### Verification

```bash
mvn clean package
```

---

## Phase 3 — Introduce `JmxDataAccess` interface; decouple `Tab` from `VMPanel`

**Goal**: `Tab` and all its subclasses must not hold a `VMPanel` reference for the
purpose of reaching JMX data. They should program to an interface.

### New interface

```java
// com.druvu.jconsole.jmx.api.JmxDataAccess
package com.druvu.jconsole.jmx.api;

public interface JmxDataAccess {

    // Connection state
    JConsoleContext.ConnectionState getConnectionState();
    boolean isConnected();
    boolean hasPlatformMXBeans();
    boolean hasHotSpotDiagnosticMXBean();
    boolean hasCompilationMXBean();
    boolean supportsLockUsage();

    // MBean server
    MBeanServerConnection getMBeanServerConnection() throws IOException;

    // Standard MXBean proxies
    ClassLoadingMXBean   getClassLoadingMXBean()   throws IOException;
    CompilationMXBean    getCompilationMXBean()    throws IOException;
    MemoryMXBean         getMemoryMXBean()         throws IOException;
    OperatingSystemMXBean getOperatingSystemMXBean() throws IOException;
    RuntimeMXBean        getRuntimeMXBean()        throws IOException;
    ThreadMXBean         getThreadMXBean()         throws IOException;

    // HotSpot / sun extensions
    com.sun.management.OperatingSystemMXBean getSunOperatingSystemMXBean() throws IOException;
    HotSpotDiagnosticMXBean getHotSpotDiagnosticMXBean() throws IOException;

    // Memory pools and GC beans
    List<MemoryPoolProxy> getMemoryPoolProxies() throws IOException;
    List<GarbageCollectorMXBean> getGarbageCollectorMXBeans() throws IOException;

    // Property change support (fires on EDT via SwingPropertyChangeSupport)
    void addPropertyChangeListener(PropertyChangeListener l);
    void removePropertyChangeListener(PropertyChangeListener l);
}
```

`ProxyClient` implements `JmxDataAccess` (adding `implements JmxDataAccess` — all
methods already exist, just need the interface declaration and correct signatures).

### Changes to `Tab`

```java
// Before
protected VMPanel vmPanel;
public Tab(VMPanel vmPanel, String name) { ... }
public void update() {
    final ProxyClient proxyClient = vmPanel.getProxyClient();
    if (!proxyClient.hasPlatformMXBeans()) { ... }
}

// After
protected final JmxDataAccess dataAccess;
protected VMPanel vmPanel;  // still kept — needed for getOverviewPanels(), vmIF access

public Tab(VMPanel vmPanel, JmxDataAccess dataAccess, String name) {
    this.vmPanel    = vmPanel;
    this.dataAccess = dataAccess;
}

public void update() {
    if (!dataAccess.hasPlatformMXBeans()) { ... }
}
```

`VMPanel` creates tabs by passing itself and `proxyClient`:

```java
// VMPanel.addTab(TabInfo ti)
Tab tab = constructor.newInstance(this, proxyClient, ti.name);
//                                ^VMPanel  ^JmxDataAccess
```

### Changes to each `Tab` subclass

Each subclass currently does `vmPanel.getProxyClient().getXxxMXBean()` inside its
`SwingWorker.doInBackground()`. Change all such calls to `dataAccess.getXxxMXBean()`.

Affected files: `MemoryTab`, `ThreadTab`, `ClassTab`, `SummaryTab`, `MBeansTab`.
`OverviewTab` does not call JMX directly — only cosmetic constructor signature change.

### Steps

1. Create `com/druvu/jconsole/jmx/api/JmxDataAccess.java`.
2. Add `implements JmxDataAccess` to `ProxyClient`.
3. Update `Tab` constructor and `update()`.
4. Update each `Tab` subclass to use `dataAccess` for JMX access.
5. Update `VMPanel.addTab()` to pass `proxyClient` as `JmxDataAccess`.

#### Verification

```bash
mvn clean package
```

---

## Phase 4 — Extract `VMUpdateCoordinator` from `VMPanel`

**Goal**: `VMPanel` currently owns the `Timer`, the `TimerTask`, and the
`SwingWorker` lifecycle for both tabs and plugins. Extract that into a dedicated
coordinator so `VMPanel` focuses on UI layout and connection lifecycle.

### New class

```java
// com.druvu.jconsole.ui.core.VMUpdateCoordinator
package com.druvu.jconsole.ui.core;

/**
 * Owns the periodic update Timer and schedules SwingWorker per Tab and per plugin.
 * Has no Swing component hierarchy — it is not a JComponent.
 */
class VMUpdateCoordinator {

    VMUpdateCoordinator(int updateIntervalMs,
                        List<Tab> tabs,
                        Map<ExceptionSafePlugin, SwingWorker<?,?>> plugins) { ... }

    void start()  { /* schedule TimerTask */ }
    void stop()   { /* cancel timer */ }
    void update() { /* called by TimerTask: iterate tabs, call tab.update();
                       iterate plugins, schedule plugin workers */ }

    void setTabs(List<Tab> tabs)   { ... }
    void setPlugins(Map<ExceptionSafePlugin, SwingWorker<?,?>> plugins) { ... }
}
```

`VMPanel` creates a `VMUpdateCoordinator` in its constructor and delegates all
timer/worker logic to it. `VMPanel` keeps `connect()`, `disconnect()`, connection
state UI (progress bar, option pane), and tab add/remove UI.

### Fields moving from `VMPanel` to `VMUpdateCoordinator`

- `timer` (`java.util.Timer`)
- `wasConnected`, `initialUpdate` (update-lifecycle booleans)
- Plugin `SwingWorker` tracking map (partial — `VMPanel` still holds the plugin list
  but passes it to the coordinator)

### Steps

1. Create `VMUpdateCoordinator.java` with skeleton above.
2. Move timer creation, `startUpdateTimer()`, `stopUpdateTimer()`, and
   `TimerTask` inner class into it.
3. Move `SwingWorker` scheduling loop for tabs and plugins into `update()`.
4. Move `wasConnected` / `initialUpdate` flags into coordinator (or pass as callbacks).
5. `VMPanel` calls `coordinator.start()` on connect, `coordinator.stop()` on disconnect.

#### Verification

```bash
mvn clean package
```

---

## Phase 5 — Introduce `MBeanService` interface; decouple inspector from `MBeansTab`

**Goal**: `XMBean`, `XTree`, and `XSheet` currently hold a `MBeansTab` reference
solely to reach `getMBeanServerConnection()`. Replace that with a narrow interface.

### Current coupling chain

```
XMBean   → MBeansTab.getMBeanServerConnection()
XTree    → MBeansTab.getMBeanServerConnection()
XSheet   → MBeansTab.getMBeanServerConnection()  (via XMBean or directly)
XMBean   → ProxyClient.SnapshotMBeanServerConnection  (inner-class import!)
```

The import of `ProxyClient.SnapshotMBeanServerConnection` in `XMBean` is the worst:
it reaches into a private inner class of an unrelated package.

### New interface

```java
// com.druvu.jconsole.inspector.api.MBeanService
package com.druvu.jconsole.inspector.api;

/**
 * Provides MBean server operations to the inspector UI.
 * Implementations hide the underlying JMX connection details.
 */
public interface MBeanService {

    MBeanServerConnection getConnection();

    MBeanInfo getMBeanInfo(ObjectName name)
        throws InstanceNotFoundException, IntrospectionException,
               ReflectionException, IOException;

    Object getAttribute(ObjectName name, String attribute)
        throws ... IOException;

    AttributeList getAttributes(ObjectName name, String[] attributes)
        throws ... IOException;

    void setAttribute(ObjectName name, Attribute attribute)
        throws ... IOException;

    Object invoke(ObjectName name, String operationName,
                  Object[] params, String[] signature)
        throws ... IOException;

    void addNotificationListener(ObjectName name, NotificationListener listener,
                                  NotificationFilter filter, Object handback)
        throws InstanceNotFoundException, IOException;

    void removeNotificationListener(ObjectName name, NotificationListener listener)
        throws ... IOException;

    boolean isNotificationBroadcaster(ObjectName name) throws IOException;
}
```

### Adapter in `MBeansTab`

```java
// MBeansTab implements MBeanService
// All methods delegate to getDataAccess().getMBeanServerConnection() calls
public MBeanInfo getMBeanInfo(ObjectName name) throws ... {
    return dataAccess.getMBeanServerConnection().getMBeanInfo(name);
}
// ... etc.
```

### Changes to inspector classes

- `XMBean(ObjectName, MBeansTab)` → `XMBean(ObjectName, MBeanService)`
- Remove `import com.druvu.jconsole.ProxyClient.SnapshotMBeanServerConnection` from
  `XMBean` — use `service.getConnection()` instead.
- `XTree`: change field `MBeansTab mbeansTab` → `MBeanService mbeanService`
- `XSheet`: same substitution

### Steps

1. Create `com/druvu/jconsole/inspector/api/MBeanService.java`.
2. Implement `MBeanService` in `MBeansTab` (delegation to `dataAccess`).
3. Update `XMBean` constructor, remove `SnapshotMBeanServerConnection` import.
4. Update `XTree` and `XSheet` field/constructor.
5. All `XMBean` / `XTree` / `XSheet` construction sites (inside `MBeansTab`) pass
   `this` (which now is a `MBeanService`).

#### Verification

```bash
mvn clean package
```

---

## Phase 6 — Move `JConsoleOptions` and `ArgumentParser` to `launcher`; tidy extras

**Goal**: Clean up the remaining root-package stragglers.

- Move `JConsoleOptions` → `com.druvu.jconsole.launcher`
- Move `ArgumentParser`  → `com.druvu.jconsole.launcher`
- Update `extra.JConsoleEx` imports accordingly
- Move `ExceptionSafePlugin` → `com.druvu.jconsole.plugins`
- Verify root package `com.druvu.jconsole` is now empty (no `.java` files remain)

#### Verification

```bash
mvn clean package
```

---

## Phase 7 — Write baseline tests

Now that boundaries are clear, add minimal tests that would have caught regressions
during the refactor:

- Unit test `JMXConnectionManager` SSL/stub validation logic (no Swing, no live JVM).
- Unit test `ArgumentParser` URL shorthand expansion.
- Unit test `JmxDataAccess` — create a mock implementation, pass to a `Tab`,
  verify `update()` calls the interface (not `VMPanel`).
- Integration smoke test: start `JConsoleEx` with `--help` / `--version` and assert
  exit code.

---

## Dependency Matrix After Refactoring

```
launcher   → ui.core, jmx, util
jmx        → util                    (no Swing)
jmx.api    → jmx                     (interfaces only)
ui.core    → jmx.api, ui.*, plugins
ui.tabs    → jmx.api, ui.core, ui.graphics, ui.components
ui.dialogs → jmx, ui.components
ui.graphics→ util
inspector  → inspector.api
inspector.api → (javax.management only)
plugins    → jmx.api, ui.core
extra      → launcher, util
util       → (nothing in com.druvu.jconsole)
```

`jmx` never imports anything from `ui.*` — this is the hard invariant.

---

## Execution Checklist

- [x] **Phase 1** — Mechanical package split (7 sub-steps + build)
- [x] **Phase 2** — Extract `JMXConnectionManager`, move `ProxyClient` to `jmx`
- [x] **Phase 3** — Introduce `JmxDataAccess`, decouple `Tab` hierarchy
- [x] **Phase 4** — Extract `VMUpdateCoordinator` from `VMPanel`
- [x] **Phase 5** — Introduce `MBeanService`, decouple inspector from `MBeansTab`
- [x] **Phase 6** — Final root-package cleanup
- [x] **Phase 7** — Baseline test coverage
