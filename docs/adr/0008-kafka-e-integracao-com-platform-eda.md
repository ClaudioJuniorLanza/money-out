# 0008 — Kafka como broker e integração com o `platform-eda`

- **Status:** Proposta (portão de entrada avaliado no início da Fase 5)
- **Data:** 2026-10-10
- **Fase:** 5

## Contexto
O plano v2 previa RabbitMQ sem justificar a escolha. O autor mantém em paralelo o `platform-eda` (repositório próprio), uma plataforma de eventos com Kafka (KRaft), Debezium (outbox via CDC), Schema Registry, um policy-runner (poll manual, retry com backoff, retenção, ordem por agregado, circuit breaker por partição) e projectors. A convenção de tópicos dessa plataforma é `{fct|cmd}.<escopo>.<domínio>.<agregado>.v{n}`, com `key = aggregateId` e headers padronizados. O money-out usa Event Sourcing (ADR-0003), então a ordem por agregado e o replay do log são requisitos centrais.

## Opções consideradas
1. **RabbitMQ (quorum queues)** — roteamento flexível, retry/DLQ simples; ordem por agregado exige um consumidor por fila ou consistent-hash; sem replay; seria um segundo broker a estudar.
2. **Kafka com relay mínimo próprio** — ordem por partição (`key = transferId`), replay, consumer groups por projeção; exige implementar relay e consumer idempotente no money-out.
3. **Kafka usando o `platform-eda` como plataforma** — mesmos benefícios, sem reimplementar outbox/retry/retenção; depende do progresso do `platform-eda` e do contrato entre os repositórios.

## Decisão
- Kafka substitui RabbitMQ. RabbitMQ fica registrado como alternativa rejeitada (útil quando se precisa de roteamento complexo por mensagem, TTL por mensagem e filas de trabalho sem ordem).
- **Trilha A (preferida):** o money-out publica eventos pelo contrato do `platform-eda` e recebe comandos HTTP idempotentes (`Idempotency-Key: {policy}:{eventId}`) disparados pelo policy-runner. **Trilha B (fallback):** relay Kafka mínimo e consumer local.
- **Portão:** na entrada da Fase 5, usamos a Trilha A se o `platform-eda` tiver concluído outbox + Debezium (sua Fase 3b) e retry/retenção/ordem no runner (sua Fase 4c); caso contrário, Trilha B.
- A chamada ao TSP permanece **dentro do money-out** (precisa de `endToEndId` persistido, estado `UNCERTAIN` e consulta de status — ADR-0004). O runner apenas dispara comandos de submissão.
- Spring Boot permanece como framework do money-out (ADR-0001); não compartilhamos código com o `platform-eda`, apenas o contrato.
- Questão em aberto para a Fase 5: usar a tabela do event store como outbox (CDC direto) ou uma tabela de outbox separada com envelope `{event, aggregate.snapshot}` como o `platform-eda` espera.

## Consequências
- Acoplamento por contrato entre dois repositórios: mudanças de tópico/envelope exigem versionamento (`.v2`).
- O resultado "desconhecido" (timeout) não existe hoje no runner (que trata timeout como retentável); por isso a chamada ao TSP e a consulta de status ficam no money-out.
- Se o `platform-eda` atrasar, a Trilha B mantém o money-out andando, ao custo de reimplementar um relay simples que será descartado.
