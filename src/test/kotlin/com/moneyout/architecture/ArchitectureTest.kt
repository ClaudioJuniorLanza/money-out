package com.moneyout.architecture

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime

/**
 * Fronteiras de arquitetura executáveis. Se uma destas regras falhar, o build falha.
 * Princípios: domínio sem framework, sem relógio global, sem ponto flutuante (PLANO.md, seção 4).
 */
@AnalyzeClasses(packages = ["com.moneyout"], importOptions = [ImportOption.DoNotIncludeTests::class])
class ArchitectureTest {
    @ArchTest
    val domainHasNoFrameworkDependency: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat(
                DescribedPredicate.describe("framework (Spring, Jakarta, Jackson)") { type: JavaClass ->
                    val name = type.packageName
                    name.startsWith("org.springframework") ||
                        name.startsWith("jakarta") ||
                        name.startsWith("javax") ||
                        name.startsWith("com.fasterxml")
                },
            )

    @ArchTest
    val domainDoesNotDependOnOuterLayers: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..application..", "..infrastructure..")

    @ArchTest
    val domainDoesNotReadGlobalClock: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .callMethod(Instant::class.java, "now")
            .orShould()
            .callMethod(LocalDate::class.java, "now")
            .orShould()
            .callMethod(LocalDateTime::class.java, "now")
            .orShould()
            .callMethod(OffsetDateTime::class.java, "now")
            .orShould()
            .callMethod(ZonedDateTime::class.java, "now")
            .orShould()
            .callMethod(System::class.java, "currentTimeMillis")

    @ArchTest
    val domainDoesNotUseFloatingPoint: ArchRule =
        noFields()
            .that()
            .areDeclaredInClassesThat()
            .resideInAPackage("..domain..")
            .should()
            .haveRawType(Double::class.javaPrimitiveType)
            .orShould()
            .haveRawType(Float::class.javaPrimitiveType)
            .orShould()
            .haveRawType(Double::class.javaObjectType)
            .orShould()
            .haveRawType(Float::class.javaObjectType)

    @ArchTest
    val applicationDoesNotDependOnInfrastructure: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
}
