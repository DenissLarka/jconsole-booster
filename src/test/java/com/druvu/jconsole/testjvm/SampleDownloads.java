package com.druvu.jconsole.testjvm;

import java.nio.charset.StandardCharsets;
import java.util.Random;

public class SampleDownloads implements SampleDownloadsMBean {

    @Override
    public byte[] generatePdfReport() {
        // Minimal valid PDF — a one-page empty document. Source: hand-rolled,
        // approx 470 bytes, parseable by every PDF viewer.
        return ("""
                %PDF-1.4
                1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj
                2 0 obj<</Type/Pages/Count 1/Kids[3 0 R]>>endobj
                3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]/Contents 4 0 R/Resources<</Font<</F1 5 0 R>>>>>>endobj
                4 0 obj<</Length 44>>stream
                BT /F1 18 Tf 72 720 Td (Sample report) Tj ET
                endstream
                endobj
                5 0 obj<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>endobj
                xref
                0 6
                0000000000 65535 f
                0000000009 00000 n
                0000000054 00000 n
                0000000099 00000 n
                0000000189 00000 n
                0000000283 00000 n
                trailer<</Size 6/Root 1 0 R>>
                startxref
                344
                %%EOF
                """).getBytes(StandardCharsets.US_ASCII);
    }

    @Override
    public byte[] generateCsvExport() {
        return ("""
                symbol,side,quantity,price
                EURUSD,BUY,1000000,1.0850
                USDCHF,SELL,500000,0.9130
                GBPUSD,BUY,250000,1.2640
                """).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] generateUnknownBlob() {
        byte[] bytes = new byte[64];
        new Random(42).nextBytes(bytes);
        return bytes;
    }

    @Override
    public byte[] generateNoHint() {
        return new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    }
}
