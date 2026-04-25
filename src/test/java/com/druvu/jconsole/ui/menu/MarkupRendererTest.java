package com.druvu.jconsole.ui.menu;

import org.testng.Assert;
import org.testng.annotations.Test;

public class MarkupRendererTest {

    @Test
    public void plainTextIsReturnedUnchanged() {
        Assert.assertEquals(MarkupRenderer.render("plain"), "plain");
        Assert.assertEquals(MarkupRenderer.render("host-1.example.com:7091"), "host-1.example.com:7091");
    }

    @Test
    public void boldIsWrappedInHtml() {
        Assert.assertEquals(MarkupRenderer.render("*hot*"), "<html><b>hot</b></html>");
    }

    @Test
    public void allowlistedColorsAreApplied() {
        String out = MarkupRenderer.render("[red ALERT]");
        Assert.assertTrue(out.startsWith("<html>"));
        Assert.assertTrue(out.contains("<font color=\"#c83232\">ALERT</font>"));
    }

    @Test
    public void unknownColorIsLeftVerbatim() {
        // Unknown color → output keeps the literal `[fuchsia x]`, no <html> wrap.
        Assert.assertEquals(MarkupRenderer.render("[fuchsia x]"), "[fuchsia x]");
    }

    @Test
    public void boldAndColorCanCombine() {
        String out = MarkupRenderer.render("*prod*-[blue 1]");
        Assert.assertTrue(out.startsWith("<html>"));
        Assert.assertTrue(out.contains("<b>prod</b>"));
        Assert.assertTrue(out.contains("<font color=\"#3264c8\">1</font>"));
    }

    @Test
    public void colorMatchIsCaseInsensitive() {
        String out = MarkupRenderer.render("[RED ALERT]");
        Assert.assertTrue(out.contains("<font color=\"#c83232\">ALERT</font>"));
    }
}
