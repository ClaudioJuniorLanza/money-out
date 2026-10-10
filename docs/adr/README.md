# Architecture Decision Records

Decisões arquiteturais do projeto. Use [`0000-template.md`](0000-template.md) para novas decisões.

| ADR | Título | Status |
|---|---|---|
| [0001](0001-stack-e-versoes.md) | Stack e versões | Proposta |
| [0002](0002-dinheiro-em-minor-units.md) | Dinheiro em minor units | Proposta |
| [0003](0003-event-sourcing-e-contabilizacao-pos-transacional.md) | Event Sourcing como verdade; contabilização pós-transacional | Aceita |
| [0004](0004-idempotencia-e-resultado-desconhecido.md) | Idempotência e resultado desconhecido | Proposta |
| [0008](0008-kafka-e-integracao-com-platform-eda.md) | Kafka e integração com o `platform-eda` | Proposta |
| [0011](0011-estrutura-hexagonal-ddd-e-shared-kernel.md) | Estrutura hexagonal/DDD por contexto e Shared Kernel | Proposta |

ADRs previstos nas próximas fases: 0005 (state machine custom), 0006 (concorrência no `Account` e atalho de transação entre aggregates), 0007 (PII em eventos), 0009 (orquestração vs coreografia), 0010 (plano de contas e mapeamento evento → lançamento).
