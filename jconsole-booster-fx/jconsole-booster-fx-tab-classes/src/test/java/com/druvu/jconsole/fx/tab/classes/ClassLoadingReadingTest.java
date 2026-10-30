package com.druvu.jconsole.fx.tab.classes;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import org.testng.annotations.Test;

/** Reading this JVM's own class loading: the counts, the verbose state and the time. */
public class ClassLoadingReadingTest {

    @Test
    public void readsTheCountsAndTheVerboseState() {
        ClassLoadingMXBean classes = ManagementFactory.getClassLoadingMXBean();
        long before = System.currentTimeMillis();

        ClassLoadingReading reading = ClassLoadingReading.read(classes);

        assertThat(reading.loaded()).isPositive();
        assertThat(reading.totalLoaded()).isGreaterThanOrEqualTo(reading.loaded());
        assertThat(reading.unloaded()).isNotNegative();
        assertThat(reading.verbose()).isEqualTo(classes.isVerbose());
        assertThat(reading.timeMillis()).isBetween(before, System.currentTimeMillis());
    }
}
