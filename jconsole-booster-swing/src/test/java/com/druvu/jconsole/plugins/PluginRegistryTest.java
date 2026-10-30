package com.druvu.jconsole.plugins;

import com.druvu.jconsole.plugins.PluginRegistry.PluginDescriptor;
import com.sun.tools.jconsole.JConsolePlugin;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class PluginRegistryTest {

    private Path tempDir;

    @AfterMethod
    public void cleanup() throws Exception {
        if (tempDir != null && Files.exists(tempDir)) {
            try (var stream = Files.walk(tempDir)) {
                stream.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {
                    }
                });
            }
        }
    }

    private Path pluginsFile() throws Exception {
        tempDir = Files.createTempDirectory("plugin-registry-test-");
        return tempDir.resolve("plugins.txt");
    }

    @Test
    public void missingFileMeansNothingDisabled() throws Exception {
        Assert.assertTrue(PluginRegistry.loadDisabled(pluginsFile()).isEmpty());
    }

    @Test
    public void storeLoadRoundTrip() throws Exception {
        Path file = pluginsFile();
        PluginRegistry.store(file, Set.of("jtop"));
        Assert.assertEquals(PluginRegistry.loadDisabled(file), Set.of("jtop"));
    }

    @Test
    public void storeEmptyWritesAllOn() throws Exception {
        Path file = pluginsFile();
        PluginRegistry.store(file, Set.of());
        List<String> lines = Files.readAllLines(file);
        Assert.assertTrue(lines.contains("jtop=on"));
        Assert.assertTrue(lines.contains(PluginRegistry.DEFAULT_TABS_ID + "=on"));
        Assert.assertTrue(PluginRegistry.loadDisabled(file).isEmpty());
    }

    @Test
    public void defaultTabsToggleRoundTrip() throws Exception {
        Path file = pluginsFile();
        PluginRegistry.store(file, Set.of(PluginRegistry.DEFAULT_TABS_ID));
        Assert.assertEquals(PluginRegistry.loadDisabled(file), Set.of(PluginRegistry.DEFAULT_TABS_ID));
        Assert.assertTrue(Files.readAllLines(file).contains(PluginRegistry.DEFAULT_TABS_ID + "=off"));
    }

    @Test
    public void unknownDisabledIdSurvivesRewrite() throws Exception {
        Path file = pluginsFile();
        PluginRegistry.store(file, Set.of("jtop", "mystery"));
        Assert.assertEquals(PluginRegistry.loadDisabled(file), Set.of("jtop", "mystery"));
    }

    @Test
    public void commentsBlanksAndMalformedLinesIgnored() throws Exception {
        Path file = pluginsFile();
        Files.write(file, List.of("# comment", "", "no-equals-sign", "=off", "jtop = OFF ", "other=on"));
        Assert.assertEquals(PluginRegistry.loadDisabled(file), Set.of("jtop"));
    }

    @Test
    public void disabledPluginIsNeverInstantiated() {
        AtomicInteger enabledCalls = new AtomicInteger();
        AtomicInteger disabledCalls = new AtomicInteger();
        List<PluginDescriptor> descriptors = List.of(
                new PluginDescriptor("on-plugin", "On", () -> {
                    enabledCalls.incrementAndGet();
                    return new DummyPlugin();
                }),
                new PluginDescriptor("off-plugin", "Off", () -> {
                    disabledCalls.incrementAndGet();
                    return new DummyPlugin();
                }));

        List<JConsolePlugin> instances = PluginRegistry.instantiate(descriptors, Set.of("off-plugin"));

        Assert.assertEquals(instances.size(), 1);
        Assert.assertEquals(enabledCalls.get(), 1);
        Assert.assertEquals(disabledCalls.get(), 0);
    }

    @Test
    public void bundledContainsJTop() {
        Assert.assertTrue(PluginRegistry.bundled().stream().anyMatch(d -> d.id().equals("jtop")));
    }

    private static final class DummyPlugin extends JConsolePlugin {
        @Override
        public Map<String, JPanel> getTabs() {
            return Map.of();
        }

        @Override
        public SwingWorker<?, ?> newSwingWorker() {
            return null;
        }
    }
}
