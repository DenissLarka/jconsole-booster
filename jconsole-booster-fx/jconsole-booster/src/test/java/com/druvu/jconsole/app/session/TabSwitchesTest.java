package com.druvu.jconsole.app.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Covers the tab switches file: missing means on, the Swing UI's format and ids kept, a broken file reads as on. */
public class TabSwitchesTest {

    private Path home;
    private Path file;

    @BeforeMethod
    public void createHome() throws IOException {
        home = Files.createTempDirectory("jcb-switches");
        file = home.resolve("plugins.txt");
    }

    @AfterMethod(alwaysRun = true)
    public void deleteHome() throws IOException {
        Files.deleteIfExists(file);
        Files.deleteIfExists(home);
    }

    @Test
    public void withoutAFileEveryTabIsOn() {
        assertThat(new TabSwitches(file).isOn("summary")).isTrue();
    }

    @Test
    public void readsTheSwingUisFormat() throws IOException {
        Files.write(file, List.of("# comment", "default-tabs=on", "jtop = OFF", "summary=off", "", "broken line"));

        TabSwitches switches = new TabSwitches(file);

        assertThat(switches.isOn("jtop")).isFalse();
        assertThat(switches.isOn("summary")).isFalse();
        assertThat(switches.isOn("default-tabs")).isTrue();
        assertThat(switches.isOn("mbeans")).isTrue();
    }

    /** 1.1 switched its five classic tabs as one group; a user who turned them off keeps them off after the update. */
    @Test
    public void theGroupSwitchOf11TurnsTheFiveClassicTabsOff() throws IOException {
        Files.write(file, List.of("default-tabs=off", "jtop=on"));

        TabSwitches switches = new TabSwitches(file);

        assertThat(List.of("overview", "memory", "threads", "classes", "summary"))
                .allSatisfy(id -> assertThat(switches.isOn(id)).as(id).isFalse());
        assertThat(switches.isOn("mbeans")).as("mbeans").isTrue();
        assertThat(switches.isOn("jtop")).as("jtop").isTrue();
    }

    @Test
    public void aClassicTabsOwnLineWinsOverTheGroupSwitch() throws IOException {
        Files.write(file, List.of("default-tabs=off", "memory=on"));

        TabSwitches switches = new TabSwitches(file);

        assertThat(switches.isOn("memory"))
                .as("memory, switched on in Settings")
                .isTrue();
        assertThat(switches.isOn("threads")).as("threads, still in the group").isFalse();
    }

    @Test
    public void aChangeKeepsEveryOtherIdOfTheFile() throws IOException {
        Files.write(file, List.of("default-tabs=on", "jtop=off"));

        new TabSwitches(file).set("summary", false);

        assertThat(Files.readAllLines(file))
                .filteredOn(line -> !line.startsWith("#"))
                .containsExactly("default-tabs=on", "jtop=off", "summary=off");
    }

    @Test
    public void aTabSwitchedBackOnIsOn() {
        TabSwitches switches = new TabSwitches(file);

        switches.set("summary", false);
        switches.set("summary", true);

        assertThat(switches.isOn("summary")).isTrue();
    }
}
