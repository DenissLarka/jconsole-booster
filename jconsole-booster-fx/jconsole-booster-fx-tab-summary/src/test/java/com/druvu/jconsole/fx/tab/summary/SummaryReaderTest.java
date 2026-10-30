package com.druvu.jconsole.fx.tab.summary;

import static org.assertj.core.api.Assertions.assertThat;

import com.druvu.jconsole.core.text.Messages;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Optional;
import org.testng.annotations.Test;

/** Covers reading the summary from a JVM: the groups in order, real values, the paths, and a JVM without MXBeans. */
public class SummaryReaderTest {

    @Test
    public void readsTheGroupsInPageOrder() throws IOException {
        VmSummary summary = readLocal();

        assertThat(summary.sections())
                .extracting(SummarySection::title)
                .containsExactly(
                        "Virtual machine", "Threads", "Classes", "Memory", "Garbage collectors", "Operating system");
        assertThat(summary.readAt()).isNotBlank();
    }

    @Test
    public void readsTheJvmItIsConnectedTo() throws IOException {
        VmSummary summary = readLocal();

        String vmName = ManagementFactory.getRuntimeMXBean().getVmName();
        assertThat(valueOf(summary, "Virtual machine", Messages.VIRTUAL_MACHINE))
                .startsWith(vmName);
        assertThat(valueOf(summary, "Virtual machine", Messages.CONNECTION_NAME))
                .isEqualTo("local test JVM");
        assertThat(valueOf(summary, "Threads", Messages.LIVE_THREADS)).isNotEqualTo(Messages.UNAVAILABLE);
        assertThat(valueOf(summary, "Memory", Messages.CURRENT_HEAP_SIZE)).endsWith("kbytes");
        assertThat(section(summary, "Garbage collectors").rows())
                .hasSameSizeAs(ManagementFactory.getGarbageCollectorMXBeans());
    }

    @Test
    public void readsTheArgumentsAndPathsOneEntryPerLine() throws IOException {
        VmSummary summary = readLocal();

        assertThat(summary.paths())
                .extracting(SummaryRow::label)
                .containsExactly(
                        Messages.VM_ARGUMENTS, Messages.CLASS_PATH, Messages.LIBRARY_PATH, Messages.BOOT_CLASS_PATH);
        String firstClassPathEntry = ManagementFactory.getRuntimeMXBean()
                .getClassPath()
                .split(SummaryFormat.pathSeparatorOf(System.getProperty("os.name")))[0];
        assertThat(summary.paths().get(1).value().lines()).contains(firstClassPathEntry);
    }

    @Test
    public void aJvmWithoutPlatformMXBeansHasNoSummary() throws IOException {
        Optional<VmSummary> summary = new SummaryReader(new LocalJvmConnection(false), "x").read();

        assertThat(summary).isEmpty();
    }

    private static VmSummary readLocal() throws IOException {
        return new SummaryReader(new LocalJvmConnection(true), "local test JVM")
                .read()
                .orElseThrow();
    }

    private static SummarySection section(VmSummary summary, String title) {
        return summary.sections().stream()
                .filter(section -> section.title().equals(title))
                .findFirst()
                .orElseThrow();
    }

    private static String valueOf(VmSummary summary, String title, String label) {
        List<SummaryRow> rows = section(summary, title).rows();
        return rows.stream()
                .filter(row -> row.label().equals(label))
                .findFirst()
                .orElseThrow()
                .value();
    }
}
