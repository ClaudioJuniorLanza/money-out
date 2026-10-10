# 0001 — Stack e versões

- **Status:** Proposta (versões exatas serão validadas e fixadas na Fase 0)
- **Data:** 2026-10-10
- **Fase:** 0

## Contexto
O objetivo é aprender engenharia de pagamentos, e a stack deve ser empregável no mercado de fintechs e ter ferramentas maduras para cada padrão estudado. O plano original usava Kotlin 1.9.6 e Spring Boot 3.2, ambas já fora de suporte.

## Opções consideradas
1. **Kotlin + Spring Boot** — tipagem forte (sealed classes, value classes) ajuda a modelar domínio; ecossistema maduro (Resilience4j, Micrometer, Testcontainers); alta demanda em bancos/fintechs.
2. **TypeScript + NestJS** — menos opções maduras para resiliência/observabilidade e modelagem de domínio.
3. **Java puro + Spring Boot** — mesma plataforma, porém mais verboso para sealed hierarchies e data classes.

## Decisão
- Java 21+ (LTS) com Kotlin 2.x.
- Spring Boot na versão estável vigente na Fase 0 (a ser confirmada e fixada).
- Gradle com Kotlin DSL; PostgreSQL + Flyway; Apache Kafka (ver ADR-0008); JUnit 5, Kotest (property-based), Testcontainers, ArchUnit; Micrometer + OpenTelemetry; Docker Compose; GitHub Actions.
- Sem Axon: event sourcing é implementado à mão para fins didáticos.

## Consequências
- Ganho: ferramentas maduras e foco em conceitos, não em configurar a stack.
- Custo: Spring esconde parte da mecânica; mitigamos mantendo o domínio livre de framework (ArchUnit).
- O `platform-eda` (repositório do autor) usa Quarkus; a integração é por contrato (Kafka + HTTP), então as stacks podem divergir. Ver ADR-0008.
