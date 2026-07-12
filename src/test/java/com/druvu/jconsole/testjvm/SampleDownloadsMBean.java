package com.druvu.jconsole.testjvm;

/**
 * Operations that return {@code byte[]} payloads, exercising JCB's download handling (the {@code {{returns:mime=…}}}
 * hint routed through {@code MimeHandler}). Kept on its own bean so the download UX has a dedicated, uncluttered
 * target; exposed via {@link SampleDownloadsStandardMBean} so the descriptions reach the wire.
 */
public interface SampleDownloadsMBean {

    /** Whitelisted MIME, no filename hint → dialog offers Open/Download, name falls back to the operation. */
    byte[] generatePdfReport();

    /** Whitelisted MIME + a {@code filename} hint → Open/Download, named {@code monthly-trades.csv}. */
    byte[] generateCsvExport();

    /** Non-whitelisted MIME (octet-stream) → Download only, with the unknown/unsafe-type warning. */
    byte[] generateUnknownBlob();

    /** {@code byte[]} with NO hint → falls through to the generic array viewer (not a download). */
    byte[] generateNoHint();
}
