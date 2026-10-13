# Money Out — Plano v2

> Laboratório de aprendizado em **engenharia de pagamentos** (payout no Brasil).
> O produto é secundário: o objetivo é entender, quebrar e consertar os problemas que fazem dinheiro sumir, duplicar ou ficar preso.

## 1. Como trabalhamos (modo colaborativo)

Legenda usada nas fases:

| Ícone | Significado |
|---|---|
| 🧑 | **Sua trilha** — você escreve o código/decisão e commita. Cada tarefa tem tempo estimado. |
| 🤖 | **Trilha do agente de IA** — scaffolding, adapters, infraestrutura, integração, guias, revisão, entrevista. Rode o agente local ou na nuvem: o protocolo é o mesmo. Tasks que exigem conversa com você são marcadas **interativas**. |
| 📋 | **Código para digitar** — eu entrego o código em `docs/fases/faseN-guia.md` no início de cada fase; você digita (não copia) e commita. |
| 💥 | **Exercício de quebra** — provocamos a falha de propósito para ver o padrão resolver. |
| 🎤 | **Entrevista técnica** — ao fim da fase, conceitos que um entrevistador realmente cobra (nada de sintaxe). |
| 📝 | **ADR** — decisão registrada em `docs/adr/`. |

> Este documento é o **roteiro** (o porquê e o quê aprender). As tasks executáveis de cada fase, com dono, dependências, testes e gate, ficam em `.specs/features/faseN-*/` (skill `tlc-spec-driven`). O protocolo de execução (owner, branch, PR) está em [`fluxo-de-trabalho.md`](fluxo-de-trabalho.md).

**Ritmo de cada fase**

1. Eu apresento a **história de falha** (o bug real que o padrão previne), abro a fase em `.specs/` (spec → design → tasks, aprovados por você) e gero o guia.
2. Você faz a **trilha 🧑** (≈ 1,5–2 h por fase; as tarefas estão ordenadas por prioridade — se faltar tempo, pare em qualquer ponto após as marcadas ⭐).
3. A trilha do agente roda em paralelo (um PR por task), e o agente revisa o que você commitou quando você pedir.
4. Fazemos os **💥 exercícios de quebra** e registramos as evidências (saídas de comando, métricas) em `docs/experiments/faseN.md`.
5. **🎤 Entrevista** no chat: de 5 a 8 perguntas, uma por vez; você responde com as suas palavras e só então eu dou feedback (o que estava certo, o que faltou, o que um entrevistador sênior esperaria). Perguntas fracas voltam nas entrevistas seguintes (revisão espaçada). O registro fica em `docs/entrevistas/faseN.md`.
6. Atualizamos os **📝 ADRs**.

**Regra de ouro:** nenhuma fase termina sem (a) critérios de aceitação verdes, (b) ADRs atualizados e (c) entrevista feita.

## 2. Escopo

**Dentro:** payout (saída de dinheiro) no Brasil, **BRL**, PIX como método principal e TED como secundário, contas pré-pagas (prefund), contabilização, TSP/PSP simulado, devoluções, reconciliação.

**Fora (por ora):** câmbio/multi-moeda, boleto, cartões, split de pagamento, multi-tenant, front-end, deploy em nuvem. `Money` já carrega moeda para não fecharmos a porta, mas só BRL é aceita.

**Contexto regulatório** (conceitual; sem implementar conformidade real): LGPD (Lei 13.709), PLD/FT (Lei 9.613 e normas do BCB), regras do PIX (BCB). Detalhes a confirmar nas fontes oficiais quando chegarmos nas fases 6 e 8.

## 3. Stack

Decisões completas em [`adr/0001`](adr/0001-stack-e-versoes.md). Resumo: Java 25 / Kotlin 2.3, Spring Boot 4.1, Gradle 9.8 (Kotlin DSL), PostgreSQL + Flyway, Apache Kafka (ver [ADR-0008](adr/0008-kafka-e-integracao-com-platform-eda.md)); I/O com MVC + JDBC + virtual threads ([ADR-0012](adr/0012-modelo-de-io-mvc-jdbc-virtual-threads.md)), JUnit 5 + Kotest (property-based) + Testcontainers + ArchUnit, Micrometer/OpenTelemetry, Docker Compose, GitHub Actions.

## 4. Princípios inegociáveis de pagamentos

São a "espinha" do plano; toda fase volta a eles.

1. **Dinheiro nunca é `float`/`double`** — inteiro em unidade mínima (centavos) + moeda.
2. **Timeout ≠ falha.** É *resultado desconhecido*; reenviar às cegas duplica pagamento.
3. **Toda operação com efeito externo é idempotente** (API, consumer, chamada ao TSP, webhook).
4. **Fatos são imutáveis.** Eventos e lançamentos contábeis (dupla entrada) só são acrescentados; correção = novo fato, nunca `UPDATE`/`DELETE`.
5. **Gravar a intenção antes de agir** (write-intent-before-act) para sobreviver a crashes.
6. **At-least-once + consumidor idempotente = efeito exactly-once.**
7. **Reconciliar sempre:** nossos registros (eventos e contabilidade) × extrato do TSP. Divergência é esperada; não detectá-la é o bug.
8. **Tudo auditável, PII protegida** (quem fez o quê, quando, e sem CPF em log).

## 4.1 Invariantes que o sistema deve provar

Inspiradas nas invariantes do `platform-eda`; cada uma é provada por teste, experimento ou auditor, não só por código.

| # | Invariante | Como será provada |
|---|---|---|
| INV-1 | **Nenhum pagamento duplicado**: um payout gera no máximo uma saída de dinheiro, mesmo com retries, reentregas e webhooks repetidos. | Fases 3, 5, 6 (testes de concorrência e cenários do TSP fake) |
| INV-2 | **Nenhum pagamento perdido**: todo payout aceito termina em estado terminal ou em `UNCERTAIN` visível e alertado. | Fases 6, 7 (matriz de crash) |
| INV-3 | **Conservação**: depósitos − saídas − holds = saldo; saldo do `Account` = saldo contábil. | Fases 2, 4, 7 |
| INV-4 | **Ordem por transfer** preservada, inclusive durante retries. | Fase 5 |
| INV-5 | **Falha é contida**: um transfer/TSP problemático não bloqueia os demais. | Fases 5, 6 |
| INV-6 | **Nada fica invisível**: tudo retido, incerto ou divergente é observável e reprocessável. | Fases 5, 7, 8 |
| INV-7 | **Auditável e sem PII em claro** em logs e eventos. | Fases 2, 8 |

## 5. Mapa de fases

| Fase | Tema | Máquina 🤖 | Você 🧑 |
|---|---|---:|---:|
| 0 | Fundações: Money, Clock, IDs, CI, ADRs | 2 h | 0,5 h |
| 1 | Domínio `Transfer` + State Machine (DDD/Hexagonal) | 2,5 h | 2 h |
| 2 | Event Sourcing: Event Store + `Account` + concorrência | 3,5 h | 2 h |
| 3 | API idempotente + observabilidade base | 3 h | 1,5 h |
| 4 | CQRS: projeções + contabilização pós-transacional | 3 h | 2 h |
| 5 | Mensageria confiável: Kafka + integração com `platform-eda` | 2 h | 1,5 h |
| 6 | TSP assíncrono (PIX): incerteza, webhooks, resiliência | 4 h | 2 h |
| 7 | Saga/Process Manager + Reconciliação | 4 h | 2 h |
| 8 | Segurança, compliance e operação | 3 h | 1,5 h |
| | **Total** | **≈ 26,5 h** | **≈ 15 h** |

Mudanças em relação ao plano v1: state machine foi para a Fase 1 (os estados já existiam lá); **Event Sourcing** (agora também no saldo da conta) e **API idempotente** vieram *antes* de mensageria; a **contabilização** (lançamentos de dupla entrada) é uma projeção pós-transacional (Fase 4); o TSP deixou de ser síncrono; observabilidade é transversal; segurança/compliance ganhou fase própria.

---

## FASE 0 — Fundações

**Objetivo:** base técnica sólida e primeiros ADRs.

**💥 História de falha:** `0.1 + 0.2 != 0.3`; um teste que passa às 23h e falha à meia-noite porque usa `now()`; um domínio que importa Spring sem ninguém perceber.

**Entregas**
- 🤖 Projeto Gradle, estrutura de pacotes hexagonal, Postgres no `docker compose`, GitHub Actions (build + test), regras **ArchUnit** (domínio não depende de framework; adapters não são acessados pelo domínio).
- 🤖 `sharedkernel`: `Currency`, `DomainException`, porta `IdGenerator`; infraestrutura: `UuidV7Generator`; `java.time.Clock` injetado (com regra ArchUnit que proíbe `now()` no domínio). (`Document` fica para a Fase 1, onde você implementa a validação.)
- 🤖 (pendente) `Money` e `Money.allocate` (com property test), beans de `Clock` UTC e `IdGenerator`, regra ArchUnit que proíbe `UUID.randomUUID()` no código puro e os 💥 exercícios de quebra. Tasks em `.specs/features/fase0-fundacoes/`.
- 📝 ADR-0012 (modelo de I/O: MVC + JDBC + virtual threads).
- 📝 ADR-0011 (estrutura hexagonal/DDD por bounded context e Shared Kernel explícito).
- 📝 ADR-0001 (stack/versões — eu valido as versões vigentes), ADR-0002 (dinheiro em minor units).

**🧑 Sua trilha** (decisão do autor: implementar os VOs do `sharedkernel` não traz ganho relevante de conhecimento; ver `docs/fases/fase0-guia.md`)
- ⭐ (30 min) Revisar e editar com suas palavras os ADRs 0001, 0002, 0004, 0011 e 0012, usando o roteiro de perguntas do guia.
- (opcional, 10 min) Ler os PRs de `Money`/`allocate` e os exercícios de quebra como material de estudo.

**Critérios de aceitação**
1. `./gradlew build test` verde local e no CI.
2. ArchUnit falha se o domínio importar `org.springframework`.
3. `Money` não permite somar moedas distintas; `allocate` nunca perde/ganha centavo (property test).
4. Nenhum `now()` direto no domínio (usa `Clock`).

**🎤 Entrevista** — `float` vs `BigDecimal` vs minor units (trade-offs) · como dividir R$ 100 em 3 sem perder centavo · por que injetar `Clock` · UUID v4 vs v7 como PK no Postgres · para que serve um ADR.

---

## FASE 1 — Domínio `Transfer` + State Machine (DDD/Hexagonal)

**Objetivo:** modelar o ciclo de vida de um payout PIX/TED com invariantes fortes.

**💥 História de falha:** uma transferência `COMPLETED` volta para `REGISTERED`; duas threads aprovam a mesma transfer; "status" é uma string solta que ninguém valida.

**Modelo**
- Aggregate `Transfer`: `Money`, `Beneficiary` (chave PIX **ou** conta bancária: ISPB, agência, conta, tipo), `method` (PIX/TED), `endToEndId` (PIX: `E` + ISPB 8 dígitos + data/hora + 11 alfanuméricos = 32 chars — confirmar no manual do BCB), `Document` (CPF/CNPJ **com dígitos verificadores**; CNPJ alfanumérico vigente desde julho/2026 — confirmar a regra oficial).
- Estados: `REGISTERED → RISK_ANALYSIS → APPROVED | REJECTED`; `APPROVED → SUBMITTED → COMPLETED | FAILED | UNCERTAIN`; `UNCERTAIN → COMPLETED | FAILED` (após consulta ao TSP); `COMPLETED → RETURNED` (devolução).
- State machine **declarativa e custom** (tabela de transições + guards + actions que emitem eventos de domínio) — 📝 ADR-0005 explica por que não Spring State Machine.
- Eventos de domínio como `sealed interface`; ports de entrada/saída; adapters in-memory.
- O agregado já nasce no formato de Event Sourcing (decidir → evento; `apply`; `rehydrate`), para a Fase 2 só trocar o adapter (`.specs/STATE.md` AD-002). Tasks em `.specs/features/fase1-transfer/`.

**🧑 Sua trilha**
- ⭐ (40 min) Escrever **a matriz de transições** (qual estado aceita qual gatilho) e o teste parametrizado que percorre **todos** os pares (estado × gatilho) afirmando que só os permitidos passam.
- ⭐ (40 min) Implementar `Cpf`/`Cnpj` a partir dos casos de teste que eu fornecer no guia (dígito verificador, rejeitar sequências repetidas, alfanumérico).
- (30 min) Property test: qualquer sequência aleatória de gatilhos nunca deixa a máquina em estado inválido e estados terminais são absorventes.
- (opcional) Guard de devolução: só `COMPLETED` e dentro do prazo (usa `Clock`).

**🤖 Minha trilha:** esqueleto do aggregate, eventos, ports, adapters in-memory, regras ArchUnit, ADR-0005, guia 📋.

**Critérios de aceitação**
1. Nenhuma transição fora da matriz é possível (teste exaustivo).
2. Domínio sem dependência de framework (ArchUnit).
3. Value Objects imutáveis; `Document` valida dígitos verificadores.
4. Cada transição válida emite exatamente um evento de domínio.

**🎤 Entrevista** — fronteira de aggregate e por que `Transfer` é um · invariante vs validação de entrada · por que `UNCERTAIN` existe · hexagonal vs camadas (o que ganha) · guard vs action · modelo anêmico: sintomas e custo.

---

## FASE 2 — Event Sourcing: Event Store, `Account` e concorrência

**Objetivo:** tornar eventos a fonte da verdade (aggregates `Transfer` e `Account`) e resolver a concorrência sobre o saldo com controle otimista de versão. Substitui o `DepositAccount` com saldo mutável do plano v1.

**💥 História de falha:** duas requisições de R$ 80 contra saldo de R$ 100 passam as duas; um `UPDATE saldo` sem rastro; "por que esta transferência está `FAILED`?" — ninguém sabe.

**Conceitos**
- Event store append-only no Postgres (`aggregate_id`, `version`, `type`, `payload JSONB`, metadados: correlation, causation, actor), `UNIQUE(aggregate_id, version)` como controle otimista.
- *Rehydration* (fold de eventos), snapshots (quando valem), **versionamento/upcasting** de eventos.
- `Transfer` passa a ser event-sourced (reaproveita os eventos da Fase 1).
- Aggregate **`Account`** (prefund): eventos `FundsDeposited`, `FundsReserved` (hold), `HoldCaptured`, `HoldReleased`; invariante "saldo disponível ≥ 0" decidida **dentro do aggregate**; saldo é derivado dos eventos.
- Conflito de versão ⇒ *retry* do comando (releitura + reaplicação) com limite; comandos idempotentes.
- Consistência entre aggregates (`Transfer` × `Account`): atalho consciente na Fase 3, process manager na Fase 7.
- **PII em eventos imutáveis:** não gravar CPF em claro; referência/crypto-shredding. 📝 ADR-0007.
- 📝 ADR-0006 (estratégia de concorrência: otimista por versão vs lock pessimista).

**🧑 Sua trilha**
- ⭐ (50 min) `rehydrate`/`apply` de `Account` + testes **Given events / When command / Then events**.
- ⭐ (40 min) **Teste de concorrência**: N threads reservando saldo; assertar *saldo disponível nunca negativo* e *conservação* (depósitos − saques − holds = saldo).
- (30 min) Escrever um **upcaster** v1→v2 (ex.: novo campo em evento antigo).

**🤖 Minha trilha:** `PostgresEventStore`, migração do `Transfer` para ES, aggregate `Account`, retry de comandos, snapshots simples, Testcontainers, ADR-0003/0006/0007.

**💥 Quebras:** duas escritas concorrentes na mesma versão (uma deve falhar e refazer); desligar o controle de versão e ver o double-spend acontecer; ler evento antigo sem campo novo.

**Critérios de aceitação**
1. Estado de `Transfer` e `Account` 100 % reconstruível dos eventos.
2. Conflito de versão lança exceção e não corrompe; retry converge.
3. Saldo disponível nunca negativo e conservação mantida sob concorrência.
4. Nenhum CPF em claro nos eventos.
5. Evento v1 antigo é lido pelo código novo via upcaster.

**🎤 Entrevista** — ES vs CRUD + audit log (quando compensa) · custos reais de ES · concorrência otimista vs pessimista (e deadlock) · evolução de schema de eventos · snapshots · hold vs débito direto · ES × LGPD.

---

## FASE 3 — API idempotente + observabilidade base

**Objetivo:** expor o sistema de forma segura contra retentativas, com rastreabilidade desde o primeiro dia.

**💥 História de falha:** o cliente tem timeout, reenvia o `POST`, e o payout é executado duas vezes.

**Entregas**
- `POST /v1/payouts` com header **`Idempotency-Key`** e tabela `idempotency_keys` (chave + cliente, hash do request, status `IN_PROGRESS|COMPLETED`, resposta gravada, expiração). Regras: mesma chave + mesmo hash → *replay* da resposta; mesma chave + hash diferente → `422`; chave em andamento → `409`.
- Criação do payout = registrar `Transfer` + reservar saldo (`FundsReserved` em `Account`) na **mesma transação de banco**, anexando aos dois streams. É um **atalho consciente** (dois aggregates em uma transação); a Fase 7 o substitui por um process manager. 📝 registrar no ADR-0006.
- `GET /v1/payouts/{id}`, listagem paginada; erros em **RFC 9457** (`application/problem+json`); validação; OAuth2/JWT resource server com escopos; versionamento por path.
- Logs JSON com `correlationId`/`traceId`, métricas (Micrometer), health liveness/readiness, tracing OpenTelemetry. 📝 ADR-0004 (idempotência).

**🧑 Sua trilha**
- ⭐ (50 min) Escrever a **função pura de decisão da idempotência** `(registro existente?, request) → Decisão` com testes tabelados.
- ⭐ (40 min) Teste de concorrência: 50 requisições simultâneas com a mesma chave ⇒ exatamente 1 `Transfer` e 1 reserva.

**🤖 Minha trilha:** controllers, filtros, persistência, segurança, observabilidade, docker compose (Jaeger/Prometheus/Grafana), MockMvc/integração.

**Critérios de aceitação**
1. Reenvio com mesma chave nunca duplica efeito; corpo diferente → 422.
2. Concorrência com mesma chave → 1 efeito.
3. Erros seguem RFC 9457 com `correlationId`.
4. Traces e métricas visíveis localmente; logs sem PII.

**🎤 Entrevista** — idempotency key vs chave natural · o que devolver no replay de um erro 500 · por que hashear o corpo · `201` vs `202` · TTL das chaves · onde gravar (mesma transação do efeito) e por quê · correlation id vs trace id.

---

## FASE 4 — CQRS: projeções e contabilização pós-transacional

**Objetivo:** construir o lado de leitura e a **contabilização** como consumidores dos eventos, depois da transação de negócio — o modelo que você já conhece do trabalho.

**💥 História de falha:** um bug na listagem exige reprocessar todo o histórico; o financeiro pergunta "de onde vem este saldo?" e a resposta precisa fechar no centavo com a contabilidade.

**Conceitos**
- Projeções com **checkpoint** e **rebuild**; o problema de ordem global (`BIGSERIAL` com lacunas e commits fora de ordem) e como ler com segurança.
- Query side (`TransferView`, saldos) que não lê o event store; consistência eventual na API (devolver versão; read-your-writes quando necessário).
- **Contabilização:** projeção que traduz eventos de negócio em **lançamentos contábeis de dupla entrada** imutáveis (Σ débitos = Σ créditos), com plano de contas simples (cliente/prefund, em-trânsito no TSP, tarifas, ajustes). Estorno = novo lançamento.
- **Reconciliação interna:** saldo do aggregate `Account` × saldo contábil; divergência é alerta, não correção silenciosa.
- Projeção idempotente (reprocessar o mesmo evento não duplica lançamento).
- 📝 ADR-0010 (plano de contas e mapeamento evento → lançamento).

**🧑 Sua trilha**
- ⭐ (50 min) **Tabela evento → lançamentos** (qual evento gera quais débitos/créditos) + `requireBalanced()` com testes e property test.
- ⭐ (40 min) Teste de **rebuild**: apagar projeções, reconstruir e comparar com o resultado original (inclusive contábil).
- (30 min) Migration SQL dos lançamentos com imutabilidade garantida no banco (eu reviso).

**🤖 Minha trilha:** runner de projeções + checkpoint, `TransferView`, projeção contábil, job de reconciliação interna, Testcontainers.

**💥 Quebras:** derrubar a projeção no meio e retomar; reprocessar o mesmo lote duas vezes; corromper um lançamento e ver a reconciliação interna acusar.

**Critérios de aceitação**
1. Todo lançamento é balanceado; soma global é zero.
2. Rebuild reproduz exatamente o mesmo resultado.
3. Reprocessar eventos não duplica lançamentos.
4. Query side não lê o event store.
5. Reconciliação interna detecta divergência injetada.

**🎤 Entrevista** — CQRS sem ES é possível? · consistência eventual na UX · projeção idempotente · por que contabilização pós-transacional · dupla entrada: por quê · como corrigir um lançamento errado · saldo derivado vs armazenado.

---

## FASE 5 — Mensageria confiável (Kafka + integração com `platform-eda`)

**Objetivo:** comunicação assíncrona sem perder nem duplicar efeitos, usando Kafka. O money-out é um **produto**; o `platform-eda` (repositório do autor, privado) é a **plataforma** que entrega eventos e executa policies. A integração é por contrato: tópicos/envelope + comandos HTTP idempotentes. Não copiamos código do `platform-eda` para este repositório.

**Portão de entrada (decidido no ADR-0008):** se o `platform-eda` tiver concluído a Fase 3b (outbox + Debezium) e a 4c (runner com retry, retenção e ordem), usamos a plataforma (**Trilha A**). Caso contrário, fazemos um relay Kafka mínimo no próprio money-out (**Trilha B**) e migramos depois. As Fases 0 a 4 não dependem disso.

**💥 História de falha:** commit no banco ok, broker fora do ar ⇒ evento perdido (*dual write*). Consumer processa a mesma mensagem duas vezes ⇒ notificação/crédito duplicado. Um evento com falha permanente trava a fila inteira de todos os transfers.

**Conceitos**
- Tópico por agregado, `key = aggregateId`, versão do agregado como `sequenceNumber` (ordem por transfer); `auto.create.topics.enable=false`; breaking change ⇒ tópico `.v2`.
- **Outbox**: o próprio event store pode servir de outbox lido por CDC (Debezium), sem tabela extra e sem dual write; decisão e formato do envelope no ADR-0008.
- Consumo **at-least-once**; efeito idempotente no destino (inbox ou `Idempotency-Key`); retry com backoff, retenção (*withhold*) e *head-of-line blocking* consciente por transfer.
- Comandos assíncronos: um policy-runner (Trilha A) ou um consumer local (Trilha B) chama endpoints HTTP idempotentes do money-out (ex.: `POST /v1/transfers/{id}/submission`) — os mesmos da Fase 3.
- Replay: reconstruir projeções a partir do log (complementa a Fase 4).
- Kafka vs RabbitMQ (alternativa rejeitada, ADR-0008) e quando RabbitMQ seria melhor.

**🧑 Sua trilha**
- ⭐ (50 min) Implementar o **handler idempotente do comando de submissão** (função pura de decisão + teste "mesmo `Idempotency-Key` 5× ⇒ 1 efeito"), reaproveitando a decisão da Fase 3.
- ⭐ (30 min) Definir o **contrato de eventos** (tópico, key, headers, envelope) para `Transfer` e `Account` e mapear cada evento do event store para ele.
- (30 min) Escrever o **runbook de mensagem retida** (como investigar, corrigir e reprocessar).

**🤖 Minha trilha:** configuração Kafka/Debezium (ou relay mínimo na Trilha B), endpoint de comando, testes com Testcontainers, ajustes no `docker compose`, ADR-0008.

**💥 Quebras:** matar o broker durante a publicação; matar o relay/conector entre publicar e confirmar; entregar o mesmo comando 3×; evento-veneno (falha permanente) bloqueando só o transfer afetado.

**Critérios de aceitação**
1. Nenhum evento perdido após queda do broker ou do relay/conector.
2. Duplicatas não geram efeito duplicado.
3. Evento com falha permanente fica retido, sem bloquear outros transfers, e pode ser reprocessado.
4. Ordem por `transferId` preservada, inclusive durante retries.

**🎤 Entrevista** — dual write · "exactly-once existe?" · outbox vs CDC · garantias de ordem no Kafka (partição, key, rebalance) · Kafka vs RabbitMQ · retry e retenção sem perder a ordem · por que o destino precisa ser idempotente.

---

## FASE 6 — TSP assíncrono (PIX): incerteza, webhooks e resiliência

**Objetivo:** integrar com um provedor externo que falha de formas interessantes.

**💥 História de falha:** timeout na chamada ao PSP; reenviamos; o beneficiário recebeu dois PIX. O webhook de confirmação chega *antes* da resposta da chamada.

**Conceitos**
- **Anti-Corruption Layer** para o TSP. A chamada ao TSP fica **dentro do money-out** (precisa de `endToEndId` persistido, `UNCERTAIN` e consulta de status); o runner/consumer só dispara o comando HTTP idempotente de submissão. Resiliência no cliente do TSP com Resilience4j (complementa o circuit breaker por partição do `platform-eda`, que atua no consumo).
- `endToEndId` gerado e **persistido antes** da chamada (write-intent-before-act) e usado como chave de idempotência no TSP.
- **Classificação de erros:** definitivo / transitório / **desconhecido** → só o transitório sofre retry; o desconhecido entra em `UNCERTAIN` e um job **consulta o status** com backoff.
- Webhooks: assinatura HMAC, tolerância de timestamp (anti-replay), deduplicação, eventos fora de ordem.
- Resiliência: timeout, retry com backoff + **jitter** (só operação idempotente), circuit breaker, bulkhead; **ordem dos decorators** (Retry por fora do CircuitBreaker, e o porquê). Fallback = fila durável, nunca "marcar como falha".
- **TSP fake com cenários**: sucesso, falha definitiva, falha transitória, *timeout-mas-executou*, *timeout-não-executou*, webhook duplicado/atrasado/fora de ordem, indisponibilidade.
- Contexto PIX: operação 24/7, devolução (prazo e regras a confirmar no manual do BCB), limites noturnos.

**🧑 Sua trilha**
- ⭐ (50 min) **Tabela de classificação de erros** (código/exceção → definitivo/transitório/desconhecido) com justificativa de negócio de cada linha + testes. Decisão sua, eu contesto.
- ⭐ (40 min) Verificação de assinatura HMAC com tolerância de timestamp e teste de replay.
- (30 min) Teste de matriz de cenários do TSP fake (tabela cenário → estado final esperado).

**🤖 Minha trilha:** port + adapter + fake, decorators Resilience4j, job de consulta, endpoint de webhook, Toxiproxy, ADR de classificação de erros.

**💥 Quebras:** injetar latência com Toxiproxy; webhook antes da resposta; mesmo webhook 3×; derrubar o processo entre "gravar intenção" e "chamar TSP".

**Critérios de aceitação**
1. Nenhum cenário do fake gera pagamento duplicado ou dinheiro preso.
2. Timeout ⇒ `UNCERTAIN` ⇒ resolvido por consulta.
3. Webhook inválido/replay é rejeitado; duplicado é inócuo.
4. Circuit breaker abre/fecha; fallback não perde o payout.

**🎤 Entrevista** — timeout ≠ falha · como evitar PIX duplicado com um PSP · segurança de webhook · eventos fora de ordem · pitfalls de retry + circuit breaker (ordem, tempestade de retries) · polling vs webhook · write-intent-before-act.

---

## FASE 7 — Saga/Process Manager + Reconciliação

**Objetivo:** orquestrar o fluxo completo de forma recuperável e provar que não há dinheiro perdido.

**💥 História de falha:** o processo morre entre reservar o saldo e chamar o TSP ⇒ dinheiro preso. O TSP diz que pagou, nossos registros dizem que falhou ⇒ diferença de R$ X no fim do dia.

**Conceitos**
- Process manager **persistido e retomável**: reservar (hold) → submeter → confirmar → capturar hold / liberar hold. Timers e varredura de processos parados.
- **Compensar somente falha definitiva**; `UNCERTAIN` bloqueia compensação até resolução, com alerta/SLA; compensações idempotentes.
- *Late success* (TSP confirma depois de já compensado) e devolução (return flow) via lançamentos inversos.
- **Reconciliação:** ingestão de extrato do TSP (arquivo fake), *matching* por `endToEndId`, categorias de divergência (só aqui / só lá / valor / status), lançamentos de ajuste com aprovação humana, relatório.
- Orquestração vs coreografia e saga vs 2PC. 📝 ADR-0009.

**🧑 Sua trilha**
- ⭐ (60 min) **Algoritmo de matching da reconciliação**, puro, com testes tabelados (cada categoria de divergência).
- ⭐ (40 min) **Tabela de decisão da saga**: (estado da saga × resultado do passo) → ação (continuar / compensar / aguardar / alertar).
- (20 min) Escrever o roteiro do "game day" (quais falhas injetar e o que esperar).

**🤖 Minha trilha:** persistência da saga, orquestrador, timers, recovery, ingestão do extrato, relatório, testes de crash.

**💥 Quebras:** `kill -9` em cada passo da saga (matriz de crash); comando duplicado; late success após compensação.

**Critérios de aceitação**
1. Após crash em qualquer passo, o sistema converge sem perda nem duplicação.
2. Compensações idempotentes; `UNCERTAIN` nunca é compensado automaticamente.
3. Reconciliação detecta todas as divergências injetadas.
4. Invariante global de conservação (saldos × contabilidade) verificada ao fim do game day.

**🎤 Entrevista** — saga vs 2PC · compensação não é rollback · orquestração vs coreografia · late success · por que reconciliar · como demonstrar que não há dinheiro perdido · idempotência de compensação.

---

## FASE 8 — Segurança, compliance e operação

**Objetivo:** o que separa um protótipo de algo operável.

**💥 História de falha:** CPF vaza em log; um operador aprova o próprio payout de R$ 500 mil; "quem alterou isso?" sem resposta.

**Conceitos**
- LGPD: minimização, mascaramento, criptografia de campos, retenção, eliminação × obrigações regulatórias.
- Auditoria imutável (quem/o quê/quando/origem); **maker-checker** acima de limite; limites e *velocity checks* (diário, por conta, noturno PIX).
- Triagem PLD/FT (stub: lista de sanções fake, valores atípicos ⇒ revisão manual) — conceitual.
- Segredos fora do repositório; OAuth2 client credentials/mTLS com parceiros; scan de dependências e SBOM.
- Operação: SLOs e alertas (taxa de `UNCERTAIN`, idade da outbox, lag de projeção, divergência de reconciliação), runbooks, backup/PITR (RPO/RTO), game day final.

**🧑 Sua trilha**
- ⭐ (40 min) Definir **3 SLOs** com alertas e escrever o runbook "TSP fora do ar".
- ⭐ (40 min) Mascaramento de PII + teste que varre logs gerados e falha se encontrar CPF/CNPJ.
- (20 min) Regra de maker-checker (limites e papéis) como tabela de decisão.

**🤖 Minha trilha:** auditoria, limites, maker-checker, mascaramento, scan de dependências, alertas.

**Critérios de aceitação**
1. Nenhuma PII em claro em logs/eventos (teste automatizado).
2. Payout acima do limite exige segundo aprovador diferente do solicitante.
3. Toda mutação sensível gera registro de auditoria imutável.
4. Game day final executado e documentado.

**🎤 Entrevista** — LGPD × dado imutável · maker-checker · como escolher SLOs · o que monitorar num sistema de pagamentos · RPO/RTO · modelagem de ameaças de um endpoint de payout.

---

## 6. Política de ADRs

- Local: `docs/adr/NNNN-titulo.md`, modelo em [`0000-template.md`](adr/0000-template.md).
- Estados: *Proposta → Aceita → (Substituída por NNNN | Rejeitada)*. ADRs não são editados depois de aceitos — são substituídos por um novo.
- Cada fase lista os ADRs que produz; sem ADR atualizado a fase não fecha.

## 7. Estrutura do repositório

```
money-out/
├── CLAUDE.md               # combinados de trabalho (lido por todo agente)
├── .claude/
│   ├── rules/              # regras de código (harness do autor adaptado a Spring + ES)
│   └── skills/             # tlc-spec-driven (spec → design → tasks → execute)
├── .specs/
│   ├── STATE.md            # decisões de processo (AD-NNN) + handoff entre sessões
│   └── features/faseN-*/   # spec.md, design.md, tasks.md, validation.md por fase
├── docs/
│   ├── PLANO.md            # este documento (roteiro)
│   ├── fluxo-de-trabalho.md# owner, branch e PR por task
│   ├── adr/                # decisões arquiteturais
│   ├── fases/              # faseN-guia.md (📋 código para digitar)
│   ├── experiments/        # faseN.md (evidências dos exercícios de quebra)
│   └── entrevistas/        # faseN.md (registro das entrevistas)
└── src/ ........ código Kotlin: `sharedkernel/`, `infrastructure/` e, por bounded context, `<contexto>/{domain,application,infrastructure}` (ADR-0011)
```
