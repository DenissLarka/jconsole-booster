package com.druvu.jconsole.console;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import org.testng.annotations.Test;

/** {@link ConsoleIO} on piped input, as Windows shells send it: a byte-order mark, a trailing space, CRLF. */
public class ConsoleIOTest {

    @Test
    public void pipedPasswordFromWindowsPowerShellLosesItsByteOrderMark() throws IOException {
        assertThat(piped("﻿admin\r\no\r\n").readPassword("Password: ")).isEqualTo("admin");
    }

    @Test
    public void pipedPasswordFromCmdEchoLosesItsTrailingSpace() throws IOException {
        assertThat(piped("admin \r\no\r\n").readPassword("Password: ")).isEqualTo("admin");
    }

    @Test
    public void aPipedFirstCommandLosesTheByteOrderMarkToo() throws IOException {
        ConsoleIO io = piped("﻿beans\r\nquit\r\n");

        assertThat(io.readLine()).isEqualTo("beans");
        assertThat(io.readLine()).isEqualTo("quit");
    }

    @Test
    public void aByteOrderMarkCountsOnlyAtTheStartOfTheStream() throws IOException {
        ConsoleIO io = piped("first\n﻿second\n");

        assertThat(io.readLine()).isEqualTo("first");
        assertThat(io.readLine()).isEqualTo("﻿second");
    }

    private static ConsoleIO piped(String input) {
        return new ConsoleIO(
                new BufferedReader(new StringReader(input)),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
    }
}
