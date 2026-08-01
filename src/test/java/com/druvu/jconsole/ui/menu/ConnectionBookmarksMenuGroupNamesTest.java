package com.druvu.jconsole.ui.menu;

import java.io.StringReader;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Covers the group list that feeds the "Add bookmark" group picker: file order is preserved and a group repeated in the
 * file appears once, so the operator does not see the same name twice in the dropdown.
 */
public class ConnectionBookmarksMenuGroupNamesTest {

    @Test
    public void keepsFileOrderAndCollapsesRepeatedGroups() throws Exception {
        String src = """
                [Prod]
                p1@prod-1:7091

                [Local]
                lo@localhost:7091

                [Prod]
                p2@prod-2:7091
                """;

        List<String> names = ConnectionBookmarksMenu.groupNames(ConnectionBookmarksLoader.parse(new StringReader(src)));

        Assert.assertEquals(names, List.of("Prod", "Local"));
    }

    @Test
    public void emptyWhenTheFileDefinesNoGroups() throws Exception {
        String src = """
                # nothing but comments
                """;

        List<String> names = ConnectionBookmarksMenu.groupNames(ConnectionBookmarksLoader.parse(new StringReader(src)));

        Assert.assertTrue(names.isEmpty(), "expected no groups, got " + names);
    }
}
