# Fase 0 — Roteiro de revisão dos ADRs (T5)

Material de apoio para a T5 (🧑). Nada nos ADRs foi alterado: os status continuam **Proposta** e a T5 continua aberta no `tasks.md`. A decisão (aceitar, editar, rejeitar) e o texto com as suas palavras são seus.

Como usar: para cada ADR, leia a decisão, responda às perguntas do [guia](fase0-guia.md#1-revise-os-adrs-t5) no próprio texto do ADR e confira os **pontos verificados** abaixo, que cruzei com o código e com `docs/experiments/fase-0.md`. Quando concordar, mude o status para **Aceita** e atualize `docs/adr/README.md`; os cinco ADRs da T5 são 0001, 0002, 0004, 0011 e 0012.

Legenda: ✅ confere com o repositório · ⚠️ desatualizado ou inconsistente (sugestão de edição, a decidir por você).

---

## ADR-0001 — Stack e versões

- ✅ Spring Boot 4.1.1, Kotlin 2.3.21, Kotest 6.2.5, ArchUnit 1.5.1 e ktlint no catálogo (`gradle/libs.versions.toml`); Java 25 no `build.gradle.kts` (`jvmToolchain(25)`).
- ⚠️ Cita "JUnit 5", mas o BOM do Spring Boot 4.1.1 gerencia o **JUnit 6.0.3** (`docs/experiments/fase-0.md` F0-01). Sugestão: trocar por "JUnit (versão do BOM)".
- ⚠️ A tabela F0-01 diz "JDK 21 (toolchain)"; o F0-05 superseda isso (Java 25). O ADR já está certo, o experimento F0-01 é histórico.
- ⚠️ A pergunta do guia ("o que você perde ao não usar Kotlin 2.4?") não está respondida no ADR: ele diz que o BOM manda, mas não diz o custo de ficar atrás. Vale uma linha em Consequências.
- ⚠️ O F0-05 registra que a tabela do plugin Kotlin lista Gradle só até 9.3.0, e o projeto usa 9.8.1 sem garantia formal. O ADR não menciona esse risco aceito.

## ADR-0002 — Dinheiro em minor units

- ✅ `Money(minorUnits: Long, currency)` implementado; moedas diferentes lançam `DomainException`; `allocate` distribui o resto sem perder centavo (PR #3).
- ⚠️ A decisão não diz **por que `Long` assinado**. O KDoc de `Money` diz (mesmo tipo para saldo, estorno e lançamento) e que "valor > 0" mora no agregado que recebe o pagamento, não no VO. Responde duas perguntas do guia; vale promover isso ao ADR.
- ⚠️ "Overflow precisa ser tratado (`Math.addExact`)" está certo, mas incompleto: `allocate` usa `BigInteger` porque `total * ratio` estoura `Long`, e `-Long.MIN_VALUE` é tratado com `negateExact`.
- ⚠️ A verificação de "moedas diferentes" não tem teste, pois só existe BRL (registrado na T3). O ADR poderia dizer que o teste entra com a 2ª moeda.

## ADR-0004 — Idempotência e resultado desconhecido

- ✅ Os status HTTP batem com `.claude/rules/hexagonal.md`: replay com mesmo hash, 422 para hash diferente, 409 em andamento.
- ✅ A ordem "gravar `endToEndId` antes de chamar o TSP" bate com `command-handler.md` (Fase 6).
- ⚠️ É o único dos cinco que **ainda não tem código** (Fases 3 e 6). Aceitar agora é aceitar a decisão, não a implementação. Vale ter claro o que muda se, na Fase 3, a chave de idempotência precisar de TTL ou escopo por cliente: o ADR não fala de expiração das chaves.
- Pergunta aberta do guia: "o que acontece com o dinheiro se você compensar um `UNCERTAIN`?" O ADR responde só pelo lado de "só compensa após falha definitiva". Vale uma frase sobre o cenário ruim (pagamento duplicado/saldo liberado de um pagamento que saiu).

## ADR-0011 — Estrutura hexagonal/DDD e Shared Kernel

- ✅ A estrutura de pacotes e as regras de dependência estão cobertas pela `ArchitectureTest` (7 regras, incluindo a nova de `UUID.randomUUID()`).
- ⚠️ Diz que `port/output` contém "repositórios, **relógio**, TSP". Na prática o relógio é o `java.time.Clock` injetado (bean da T1), não uma porta própria; `IdGenerator` é a porta. Sugestão: tirar "relógio" ou explicar que `Clock` é injetado diretamente.
- ⚠️ A lista de `sharedkernel` no ADR (`Money, Currency, DomainException, IdGenerator`) bate com o código. Pergunta do guia sobre `ConcurrencyConflict`: `event-sourcing.md` diz que ele vai ao `sharedkernel` quando o segundo agregado precisar, aplicando os critérios do ADR; confira se os 4 critérios realmente o admitem (ele não é "modelo", é erro técnico de persistência otimista).
- ⚠️ Os critérios 1 e 3 ("mais de um contexto") são verificados por revisão humana, não pela ArchUnit. Só o 4 ("não depende de contexto") é executável (`sharedKernelDoesNotDependOnAnyContext`).

## ADR-0012 — MVC + JDBC + virtual threads

- ✅ Alinhado com `.claude/rules/kotlin.md` e `command-handler.md` (sem `suspend`; `TransactionTemplate` na `infrastructure`).
- ✅ A afirmação sobre pinning (JEP 491, JDK 24) foi verificada no F0-05.
- ⚠️ Cabeçalho "Fase 0 (vale a partir das Fases 2 e 3)" e o texto "virtual threads quando o servidor web entrar (Fase 3)": o `spring.threads.virtual.enabled` só será ligado na Fase 3. Se a Fase 2 já tiver JDBC com concorrência (testes com N threads), decida se isso muda algo.
- Pergunta aberta do guia: "quando reativo valeria a pena num sistema de payout?" O ADR só descreve por que não agora; uma frase sobre o limite (ex.: fan-out alto de I/O sem transação) ajuda a defender na entrevista.

---

## Antes de fechar a T5

1. Edite os ADRs com as suas palavras e mude o status de cada um.
2. Atualize a tabela de `docs/adr/README.md`.
3. Marque a T5 no `tasks.md` e F0-05 na spec (`Pending` → `Done`) **no mesmo commit** (`docs(adr): revisa e aceita ADRs da Fase 0`), com `./gradlew ktlintFormat && ./gradlew build` verde.
4. Divergências que virem ADR substituto ou pedido de mudança precisam ser resolvidas antes da T7.
