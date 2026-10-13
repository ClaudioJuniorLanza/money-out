# 0001 — Stack e versões

- **Status:** Proposta (versões verificadas em 2026-10-10; ver `docs/experiments/fase-0.md`)
- **Data:** 2026-10-10
- **Fase:** 0

## Contexto
O objetivo é aprender engenharia de pagamentos, e a stack deve ser empregável no mercado de fintechs e ter ferramentas maduras para cada padrão estudado. O plano original usava Kotlin 1.9.6 e Spring Boot 3.2, ambas já fora de suporte.

## Opções consideradas
1. **Kotlin + Spring Boot** — tipagem forte (sealed classes, value classes) ajuda a modelar domínio; ecossistema maduro (Resilience4j, Micrometer, Testcontainers); alta demanda em bancos/fintechs.
2. **TypeScript + NestJS** — menos opções maduras para resiliência/observabilidade e modelagem de domínio.
3. **Java puro + Spring Boot** — mesma plataforma, porém mais verboso para sealed hierarchies e data classes.

## Decisão
- Java 25 (LTS) como toolchain, com Kotlin 2.3.21. Começar na LTS mais recente evita ficar preso a bibliotecas defasadas por incompatibilidade ao longo do projeto e alinha com o `platform-eda`, que também usa 25. Compatibilidade conferida na fonte em 2026-10-13: Kotlin gera bytecode Java 25 desde a 2.3.0, Gradle suporta Java 25 desde a 9.1.0 e Spring Boot 4.1.1 é compatível até o Java 26 (evidências em `docs/experiments/fase-0.md` F0-05).
- Spring Boot 4.1.1, usando o BOM `spring-boot-dependencies` como fonte da verdade de versões (Kotlin, JUnit, Testcontainers, Flyway, drivers). Só entram no catálogo as versões que o BOM não gerencia (Kotest 6.2.5, ArchUnit 1.5.1, ktlint).
- Versões exatas, nunca `latest`, nunca milestone/RC; atualizações via ADR ou commit dedicado.
- Gradle 9.8.1 (wrapper) com Kotlin DSL; PostgreSQL + Flyway; Apache Kafka (ver ADR-0008); JUnit 5, Kotest (property-based), Testcontainers, ArchUnit; Micrometer + OpenTelemetry; Docker Compose; GitHub Actions.
- Sem Axon: event sourcing é implementado à mão para fins didáticos.

## Consequências
- Ganho: ferramentas maduras e foco em conceitos, não em configurar a stack.
- Custo: Spring esconde parte da mecânica; mitigamos mantendo o domínio livre de framework (ArchUnit).
- O `platform-eda` (repositório do autor) usa Quarkus; a integração é por contrato (Kafka + HTTP), então as stacks podem divergir. Ver ADR-0008.
