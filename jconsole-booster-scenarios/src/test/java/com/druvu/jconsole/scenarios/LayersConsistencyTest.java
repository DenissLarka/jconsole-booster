package com.druvu.jconsole.scenarios;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.testng.annotations.Test;

/**
 * The module layers of JConsole Booster, checked on the compiled classes of every module: the core at the bottom, the
 * protocols found at run time, the console and the desktop app as fronts, the sample target outside them all.
 *
 * @author Deniss Larka <br>
 *     on 30 Oct 2026
 */
public class LayersConsistencyTest {

    // The Swing front's packages, the reference until the JavaFX app replaces it.
    private static final String[] DESKTOP = {
        "com.druvu.jconsole.inspector..",
        "com.druvu.jconsole.launcher..",
        "com.druvu.jconsole.plugins..",
        "com.druvu.jconsole.ui..",
        "com.druvu.jconsole.util.."
    };

    private static final JavaClasses JCB = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.druvu.jconsole");

    @Test
    public void eachModuleReachesOnlyTheModulesBelowIt() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .ensureAllClassesAreContainedInArchitecture()
                .layer("Core")
                .definedBy("com.druvu.jconsole.core..")
                .layer("Protocols")
                .definedBy("com.druvu.jconsole.protocol..")
                .layer("Console")
                .definedBy("com.druvu.jconsole.console..")
                .layer("Desktop")
                .definedBy(DESKTOP)
                .layer("Sample target")
                .definedBy("com.druvu.jconsole.sampletarget..")
                .whereLayer("Core")
                .mayOnlyBeAccessedByLayers("Protocols", "Console", "Desktop")
                // the core finds a protocol through druvu-lib-loader, so nothing names one
                .whereLayer("Protocols")
                .mayNotBeAccessedByAnyLayer()
                // the desktop launcher hands --console over
                .whereLayer("Console")
                .mayOnlyBeAccessedByLayers("Desktop")
                .whereLayer("Desktop")
                .mayNotBeAccessedByAnyLayer()
                .whereLayer("Sample target")
                .mayNotBeAccessedByAnyLayer()
                .check(JCB);
    }

    @Test
    public void theCoreTheProtocolsAndTheConsoleUseNoUiToolkit() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "com.druvu.jconsole.core..", "com.druvu.jconsole.protocol..", "com.druvu.jconsole.console..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("java.awt..", "javax.swing..", "javafx..", "java.beans..")
                .because(
                        "the console runs on boxes without a display, and the JavaFX app replaces Swing on the same core")
                .check(JCB);
    }

    @Test
    public void protocolsDoNotDependOnEachOther() {
        slices().matching("com.druvu.jconsole.protocol.(*)..")
                .should()
                .notDependOnEachOther()
                .check(JCB);
    }
}
