package ao.skool.app;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Load-bearing test: enforces the modular-monolith seams described in INITIAL_PLAN.md §3.
 *
 * A module may only depend on:
 *   - its own packages
 *   - common-* packages
 *   - other modules' `api` packages
 *
 * It may never reach into another module's `internal.*` package. If this test fails,
 * the offending import breaks the eventual microservice split — fix it before merging.
 */
class ModuleBoundaryTest {

    private static final String[] MODULES = {
            "identity", "academic_structure", "sis", "staff",
            "attendance", "grading", "assessment", "assignments",
            "forum", "subject_board", "fees", "notifications",
            "documents", "reporting"
    };

    private static JavaClasses production() {
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("ao.skool");
    }

    @Test
    void modulesMustNotReachIntoAnotherModulesInternalPackage() {
        JavaClasses classes = production();
        for (String module : MODULES) {
            String selfInternal = "ao.skool." + module + ".internal..";
            for (String other : MODULES) {
                if (module.equals(other)) continue;
                String otherInternal = "ao.skool." + other + ".internal..";
                ArchRule rule = noClasses()
                        .that().resideInAPackage(selfInternal)
                        .should().dependOnClassesThat().resideInAPackage(otherInternal)
                        .as(module + " must not depend on " + other + ".internal")
                        .allowEmptyShould(true);
                rule.check(classes);
            }
        }
    }

    @Test
    void commonModulesMustNotDependOnDomainModules() {
        JavaClasses classes = production();
        ArchRule rule = noClasses()
                .that().resideInAPackage("ao.skool.common..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "ao.skool.identity..",
                        "ao.skool.academic_structure..",
                        "ao.skool.sis..",
                        "ao.skool.staff..",
                        "ao.skool.attendance..",
                        "ao.skool.grading..",
                        "ao.skool.assessment..",
                        "ao.skool.assignments..",
                        "ao.skool.forum..",
                        "ao.skool.subject_board..",
                        "ao.skool.fees..",
                        "ao.skool.notifications..",
                        "ao.skool.documents..",
                        "ao.skool.reporting.."
                )
                .as("common-* must be a leaf — domain modules depend on common, not the reverse")
                .allowEmptyShould(true);
        rule.check(classes);
    }
}
