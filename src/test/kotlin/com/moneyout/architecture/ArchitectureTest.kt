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
 * Fronteiras de arquitetura executáveis (ADR-0011). Se uma destas regras falhar, o build falha.
 *
 * Estrutura: `sharedkernel` (núcleo mínimo compartilhado) e, por bounded context,
 * `<contexto>.domain`, `<contexto>.application` e `<contexto>.infrastructure`.
 * Código "puro" = `..domain..` e `..sharedkernel..`: sem framework, sem relógio global, sem ponto flutuante.
 */
@AnalyzeClasses(packages = ["com.moneyout"], importOptions = [ImportOption.DoNotIncludeTests::class])
class ArchitectureTest {
    private companion object {
        const val DOMAIN = "..domain.."
        const val KERNEL = "..sharedkernel.."
        val CONTEXTS = arrayOf("..transfer..", "..account..", "..accounting..")
    }

    @ArchTest
    val pureCodeHasNoFrameworkDependency: ArchRule =
        noClasses()
            .that()
            .resideInAnyPackage(DOMAIN, KERNEL)
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
    val pureCodeDoesNotDependOnOuterLayers: ArchRule =
        noClasses()
            .that()
            .resideInAnyPackage(DOMAIN, KERNEL)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..application..", "..infrastructure..")

    @ArchTest
    val pureCodeDoesNotReadGlobalClock: ArchRule =
        noClasses()
            .that()
            .resideInAnyPackage(DOMAIN, KERNEL)
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
    val pureCodeDoesNotUseFloatingPoint: ArchRule =
        noFields()
            .that()
            .areDeclaredInClassesThat()
            .resideInAnyPackage(DOMAIN, KERNEL)
            .should()
            .haveRawType(Double::class.javaPrimitiveType)
            .orShould()
            .haveRawType(Float::class.javaPrimitiveType)
            .orShould()
            .haveRawType(Double::class.javaObjectType)
            .orShould()
            .haveRawType(Float::class.javaObjectType)

    @ArchTest
    val sharedKernelDoesNotDependOnAnyContext: ArchRule =
        noClasses()
            .that()
            .resideInAPackage(KERNEL)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(*CONTEXTS)

    @ArchTest
    val applicationDoesNotDependOnInfrastructure: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..application..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..infrastructure..")
}
