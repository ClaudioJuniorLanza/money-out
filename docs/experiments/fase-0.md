# Experimentos — Fase 0

## F0-01 — Versões verificadas na fonte (2026-10-10)

Consultadas em `repo.maven.apache.org` (metadata XML) e `services.gradle.org/versions/current`. Só versões GA (sem milestone/RC).

| Item | Versão | Observação |
|---|---|---|
| Spring Boot | 4.1.1 | `4.2.0-M2` existe, mas é milestone |
| Kotlin | 2.3.21 | a mesma gerenciada pelo `spring-boot-dependencies:4.1.1` (existe 2.4.x, mas o BOM manda) |
| JUnit | 6.0.3 | gerenciada pelo BOM |
| Testcontainers | 2.0.5 | gerenciada pelo BOM (entra na Fase 2) |
| Flyway | 12.4.0 | gerenciada pelo BOM (entra na Fase 2); o Central já tem 13.x |
| Kotest | 6.2.5 | fora do BOM; fixada no catálogo |
| ArchUnit | 1.5.1 | fora do BOM; fixada no catálogo |
| Gradle | 9.8.1 | wrapper |
| JDK | 21 (toolchain) | LTS; o ambiente de CI/sessão tem 21 |

Decisão: seguir o BOM do Spring Boot como fonte da verdade (política do ADR-0001).

## F0-02 — Build verde

`./gradlew build` ⇒ BUILD SUCCESSFUL. 12 testes: `CurrencyTest` (2), `UuidV7GeneratorTest` (4), `ArchitectureTest` (5), `MoneyOutApplicationTests` (1).

Achados durante o build:
- O compilador (`allWarningsAsErrors`) pegou um erro real: em um `enum`, a inicialização de uma propriedade não pode usar o `companion object` (ainda não inicializado). Solução: propriedade calculada.
- O ktlint rejeitou nomes de propriedade com crases (`val \`nome\``) nas regras do ArchUnit. Solução: camelCase nas propriedades; os nomes em português com crases continuam valendo para funções de teste.
- O Maven Central devolveu `429 Too Many Requests` em várias resoluções de dependência neste ambiente de sessão; repetir o build com pausa resolveu (o Gradle guarda em cache o que já baixou). Não foi usado nenhum espelho alternativo.

## F0-03 — As regras de arquitetura não são vazias

Numa cópia descartável do projeto, plantamos uma classe com `@Component` (Spring), `Instant.now()` e um campo `Double` em `..domain..`. Resultado: as regras `domainHasNoFrameworkDependency`, `domainDoesNotReadGlobalClock` e `domainDoesNotUseFloatingPoint` falharam; as demais continuaram verdes, como esperado. Também confirmamos que o código do `docs/fases/fase0-guia.md` (Money e esqueleto do teste de `allocate`) compila.

## Exercícios de quebra do autor

> Preencha após fazer os 💥 do `docs/fases/fase0-guia.md` (o que observou, qual regra falhou e por quê).
