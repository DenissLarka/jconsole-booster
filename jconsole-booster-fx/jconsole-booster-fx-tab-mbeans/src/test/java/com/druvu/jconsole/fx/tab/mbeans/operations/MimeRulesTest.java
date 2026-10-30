package com.druvu.jconsole.fx.tab.mbeans.operations;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/** The rules for a returned file: the declared MIME type, what may be opened, and a safe file name. */
public class MimeRulesTest {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Test
    public void theMimeTypeComesFromTheReturnsHintInLowerCase() {
        assertThat(MimeRules.mimeOf("Monthly report {{returns:mime=application/pdf}}"))
                .contains("application/pdf");
        assertThat(MimeRules.mimeOf("X {{returns:mime=Application/PDF}}")).contains("application/pdf");
    }

    @Test
    public void noMimeWithoutAUsableHint() {
        assertThat(MimeRules.mimeOf("plain")).isEmpty();
        assertThat(MimeRules.mimeOf("")).isEmpty();
        assertThat(MimeRules.mimeOf(null)).isEmpty();
        assertThat(MimeRules.mimeOf("X {{returns:format=json}}")).isEmpty();
        assertThat(MimeRules.mimeOf("X {{returns:mime=}}")).isEmpty();
    }

    @Test
    public void theSuggestedNameIsReadInAnyOrder() {
        assertThat(MimeRules.suggestedName("Trades {{returns:mime=text/csv,filename=trades-2026-07.csv}}"))
                .contains("trades-2026-07.csv");
        assertThat(MimeRules.suggestedName("X {{returns:filename=report.pdf,mime=application/pdf}}"))
                .contains("report.pdf");
        assertThat(MimeRules.suggestedName("X {{returns:mime=text/csv,filename=}}"))
                .isEmpty();
    }

    @Test
    public void onlyWhitelistedTypesOpen() {
        assertThat(MimeRules.isOpenable("application/pdf")).isTrue();
        assertThat(MimeRules.isOpenable("APPLICATION/PDF")).isTrue();
        assertThat(MimeRules.isOpenable(XLSX)).isTrue();
        assertThat(MimeRules.isOpenable("application/vnd.ms-excel")).isTrue();
        assertThat(MimeRules.isOpenable("application/octet-stream")).isFalse();
        assertThat(MimeRules.isOpenable("application/x-msdownload")).isFalse();
        assertThat(MimeRules.isOpenable(null)).isFalse();
    }

    @Test
    public void theExtensionComesFromTheType() {
        assertThat(MimeRules.extensionFor("image/jpeg")).isEqualTo(".jpg");
        assertThat(MimeRules.extensionFor("TEXT/CSV")).isEqualTo(".csv");
        assertThat(MimeRules.extensionFor(XLSX)).isEqualTo(".xlsx");
        assertThat(MimeRules.extensionFor("application/zip")).isEqualTo(".zip");
        assertThat(MimeRules.extensionFor("video/x-arbitrary")).isEqualTo(".bin");
        assertThat(MimeRules.extensionFor(null)).isEqualTo(".bin");
    }

    @Test
    public void theServersBaseNameKeepsTheTypesExtension() {
        assertThat(MimeRules.fileName("export", "text/csv", "trades-2026-07.csv"))
                .isEqualTo("trades-2026-07.csv");
        assertThat(MimeRules.fileName("gen", XLSX, "book")).isEqualTo("book.xlsx");
    }

    @Test
    public void theOperationNamesTheFileWhenTheServerDoesNot() {
        assertThat(MimeRules.fileName("generatePdfReport", "application/pdf", null))
                .isEqualTo("generatePdfReport.pdf");
        assertThat(MimeRules.fileName("gen", "text/csv", "   ")).isEqualTo("gen.csv");
        assertThat(MimeRules.fileName("gen", "text/csv", "***")).isEqualTo("___.csv");
    }

    @Test
    public void aServerNameCannotClimbOutOfTheFolder() {
        assertThat(MimeRules.fileName("op", "text/csv", "../../../../etc/passwd"))
                .isEqualTo("passwd.csv");
        assertThat(MimeRules.fileName("op", "text/csv", "/etc/shadow")).isEqualTo("shadow.csv");
        assertThat(MimeRules.fileName("op", "application/pdf", "C:\\Windows\\System32\\evil"))
                .isEqualTo("evil.pdf");
        assertThat(MimeRules.fileName("op", "text/csv", "a b;c.txt")).isEqualTo("a_b_c.csv");
    }

    @Test
    public void aServerExtensionIsNeverKept() {
        assertThat(MimeRules.fileName("op", "application/pdf", "invoice.exe")).isEqualTo("invoice.pdf");
        assertThat(MimeRules.fileName("op", "text/csv", "data.bat")).isEqualTo("data.csv");
    }
}
