# STATE

## Decisions

### AD-001
- **Decision**: Toda task tem um Owner — 🧑 autor (local), ☁️ agente na nuvem, 🤖 agente local em sessão com o autor — e agentes nunca implementam tasks 🧑.
- **Reason**: O projeto é de aprendizado: as tasks 🧑 são o exercício do autor; delegar o resto à nuvem só funciona se o limite for explícito em cada task.
- **Trade-off**: Tasks ☁️ podem ficar bloqueadas esperando uma task 🧑; aceita-se o gargalo em troca do aprendizado.
- **Scope**: Todas as fases; `docs/fluxo-de-trabalho.md`.
- **Date**: 2026-10-10
- **Status**: superseded by AD-004

### AD-002
- **Decision**: Agregados são escritos no formato event-sourced (decidir → evento; `apply`; `rehydrate`; eventos com delta + `occurredAt`) desde a Fase 1, mesmo com persistência in-memory.
- **Reason**: ADR-0003 faz do event store a verdade; escrever o agregado no formato estado + outbox do harness original obrigaria a reescrevê-lo na Fase 2.
- **Trade-off**: A Fase 1 fica um pouco mais abstrata (rehidratação antes de existir event store).
- **Scope**: `transfer`, `account` e qualquer agregado futuro; `.claude/rules/domain-model.md`, `.claude/rules/event-sourcing.md`.
- **Date**: 2026-10-10
- **Status**: active

### AD-003
- **Decision**: As regras de código para agentes vivem em `.claude/rules/`, adaptadas do harness pessoal do autor para Spring + Event Sourcing + minor units + ADR-0011; o harness original não é copiado para o repositório.
- **Reason**: Agentes na nuvem seguem o texto literalmente; um harness Quarkus/reativo com exceções geraria código que quebra o build ou os ADRs.
- **Trade-off**: Melhorias futuras no harness original precisam ser portadas à mão.
- **Scope**: Todo código gerado por agentes; precedência em `.claude/rules/README.md`.
- **Date**: 2026-10-10
- **Status**: active

### AD-004
- **Decision**: O Owner de uma task tem dois valores — 🧑 autor ou 🤖 agente de IA. Onde o agente roda (local ou nuvem) não é atributo da task; tasks que exigem o autor na sessão são marcadas **interativas**.
- **Reason**: O autor delega tanto à nuvem quanto ao agente local; para o agente o protocolo (branch, gate, commit, PR) é idêntico, então separar ☁️ de 🤖 só criava duas regras para a mesma coisa.
- **Trade-off**: A task não diz mais onde rodar; o autor decide no momento de delegar (as interativas, na prática, numa sessão em que ele está presente).
- **Scope**: Todas as fases; `docs/fluxo-de-trabalho.md`, `CLAUDE.md`, `tasks.md` de cada fase.
- **Date**: 2026-10-13
- **Status**: active

### AD-005
- **Decision**: Na Fase 0 o autor não implementa código: `Money`, `allocate` e os exercícios de quebra são do agente; o autor revisa os ADRs e faz a entrevista.
- **Reason**: Implementar os VOs do `sharedkernel` não traz ganho relevante de conhecimento ao autor; o valor está nas decisões (ADRs) e em defendê-las.
- **Trade-off**: O autor aprende `Money`/`allocate` lendo o PR e o property test, não escrevendo; o guia da fase vira roteiro de revisão.
- **Scope**: `.specs/features/fase0-fundacoes/`, `docs/fases/fase0-guia.md`, `docs/PLANO.md` (Fase 0).
- **Date**: 2026-10-13
- **Status**: active

## Handoff

- **Feature**: `.specs/features/fase0-fundacoes/` (fechamento); `.specs/features/fase1-transfer/` em Draft
- **Phase / Task**: Fase 0 — specs e tasks revisadas pelo autor, ajustes de ownership aplicados (AD-004, AD-005); aguardando aprovação final
- **Completed**: nenhuma task das specs; scaffolding da Fase 0 já entregue (`docs/experiments/fase-0.md` F0-01..F0-04)
- **In-progress** (file:line): none
- **Next step**: autor aprova as duas specs; integra `chore/organiza-planejamento` no `main`; delega Fase 0 T1–T4 e T6 a um agente; revisa os ADRs (T5).
- **Blockers**: o scaffolding da Fase 0 (commits 243d404 e 598f63d) ainda não está no `main`, e os agentes partem do `main`.
- **Uncommitted files**: none
- **Branch**: `chore/organiza-planejamento`
