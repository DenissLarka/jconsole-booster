package com.druvu.jconsole.inspector.operations;

import java.util.Optional;
import org.testng.Assert;
import org.testng.annotations.Test;

public class MimeHandlerTest {

    @Test
    public void mimeFromDescriptionExtractsHint() {
        Assert.assertEquals(
                MimeHandler.mimeFromDescription("Monthly report {{returns:mime=application/pdf}}"),
                Optional.of("application/pdf"));
        Assert.assertEquals(
                MimeHandler.mimeFromDescription("Trades {{returns:mime=text/csv}}"),
                Optional.of("text/csv"));
    }

    @Test
    public void mimeValueIsLowercased() {
        // The markup spec uses lowercase keys, but the value (the MIME type)
        // is normalized so the whitelist comparison is robust.
        Assert.assertEquals(
                MimeHandler.mimeFromDescription("X {{returns:mime=Application/PDF}}"),
                Optional.of("application/pdf"));
    }

    @Test
    public void noMimeHintReturnsEmpty() {
        Assert.assertEquals(MimeHandler.mimeFromDescription("plain"), Optional.empty());
        Assert.assertEquals(MimeHandler.mimeFromDescription(""), Optional.empty());
        Assert.assertEquals(MimeHandler.mimeFromDescription(null), Optional.empty());
    }

    @Test
    public void returnsTagWithoutMimeKeyReturnsEmpty() {
        // {{returns:format=json}} is a different sub-key; mime extractor must skip it.
        Assert.assertEquals(MimeHandler.mimeFromDescription("X {{returns:format=json}}"), Optional.empty());
    }

    @Test
    public void blankMimeValueReturnsEmpty() {
        Assert.assertEquals(MimeHandler.mimeFromDescription("X {{returns:mime=}}"), Optional.empty());
    }

    @Test
    public void whitelistContainsExpectedTypes() {
        Assert.assertTrue(MimeHandler.isOnWhitelist("application/pdf"));
        Assert.assertTrue(MimeHandler.isOnWhitelist("text/csv"));
        Assert.assertTrue(MimeHandler.isOnWhitelist("image/png"));
        Assert.assertTrue(MimeHandler.isOnWhitelist("APPLICATION/PDF")); // case-insensitive
    }

    @Test
    public void whitelistRejectsUntrustedTypes() {
        Assert.assertFalse(MimeHandler.isOnWhitelist("application/x-msdownload"));
        Assert.assertFalse(MimeHandler.isOnWhitelist("application/octet-stream"));
        Assert.assertFalse(MimeHandler.isOnWhitelist("application/x-executable"));
        Assert.assertFalse(MimeHandler.isOnWhitelist(null));
    }

    @Test
    public void extensionForKnownTypes() {
        Assert.assertEquals(MimeHandler.extensionFor("application/pdf"), ".pdf");
        Assert.assertEquals(MimeHandler.extensionFor("application/json"), ".json");
        Assert.assertEquals(MimeHandler.extensionFor("application/xml"), ".xml");
        Assert.assertEquals(MimeHandler.extensionFor("application/zip"), ".zip");
        Assert.assertEquals(MimeHandler.extensionFor("text/plain"), ".txt");
        Assert.assertEquals(MimeHandler.extensionFor("text/csv"), ".csv");
        Assert.assertEquals(MimeHandler.extensionFor("text/html"), ".html");
        Assert.assertEquals(MimeHandler.extensionFor("image/png"), ".png");
        Assert.assertEquals(MimeHandler.extensionFor("image/jpeg"), ".jpg");
        Assert.assertEquals(MimeHandler.extensionFor("image/gif"), ".gif");
        Assert.assertEquals(MimeHandler.extensionFor("image/svg+xml"), ".svg");
    }

    @Test
    public void extensionForUnknownTypes() {
        Assert.assertEquals(MimeHandler.extensionFor("application/octet-stream"), ".bin");
        Assert.assertEquals(MimeHandler.extensionFor("video/x-arbitrary"), ".bin");
        Assert.assertEquals(MimeHandler.extensionFor(null), ".bin");
    }

    @Test
    public void extensionForIsCaseInsensitive() {
        Assert.assertEquals(MimeHandler.extensionFor("Application/PDF"), ".pdf");
        Assert.assertEquals(MimeHandler.extensionFor("TEXT/CSV"), ".csv");
    }
}
