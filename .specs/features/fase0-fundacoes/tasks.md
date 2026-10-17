# Fase 0 — Fundações (fechamento) Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill (vendorizada em `.claude/skills/tlc-spec-driven/`): **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path.

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

Antes de começar qualquer task, leia [`docs/fluxo-de-trabalho.md`](../../../docs/fluxo-de-trabalho.md): ele define o **Owner** de cada task e o protocolo de branch/PR.

- **Owner 🧑** — do autor. Agentes **não** implementam; podem oferecer dicas e revisão.
- **Owner 🤖** — de um agente de IA, rodando local ou na nuvem (o protocolo é o mesmo). Tasks marcadas **interativa** exigem o autor na sessão.

---

**Spec**: `.specs/features/fase0-fundacoes/spec.md`
**Status**: Approved (autor, 2026-10-13)

---

## Test Coverage Matrix

> Generated from codebase, project guidelines, and spec - confirm before Execute. Guidelines found: `.claude/rules/tests.md`, `.claude/rules/kotlin.md`, `CLAUDE.md`, `build.gradle.kts` (ktlint + `allWarningsAsErrors`), `.github/workflows/ci.yml`.

| Code Layer | Required Test Type | Coverage Expectation | Location Pattern | Run Command |
|---|---|---|---|---|
| `sharedkernel` (VOs) | unit + property | todos os ramos; 1:1 com os ACs; todas as bordas listadas | `src/test/kotlin/com/moneyout/sharedkernel/*Test.kt` | `./gradlew test` |
| Configuração Spring (`infrastructure/config`) | integration (contexto Spring) | cada bean exigido pela spec existe e tem a configuração do AC | `src/test/kotlin/com/moneyout/infrastructure/**/*Test.kt` | `./gradlew test` |
| Regras de arquitetura | ArchUnit + prova de falha | regra nova falha numa cópia descartável (evidência em `docs/experiments/`) | `src/test/kotlin/com/moneyout/architecture/ArchitectureTest.kt` | `./gradlew test` |
| Documentação (ADRs, experimentos, entrevista) | none | - (revisão humana) | `docs/**` | - |

## Gate Check Commands

> Generated from codebase - confirm before Execute.

| Gate Level | When to Use | Command |
|---|---|---|
| Quick | Tasks com testes unitários | `./gradlew test` |
| Full | Tasks com teste de contexto Spring | `./gradlew test` |
| Build | Fim de toda task e de toda fase (é o que o CI roda) | `./gradlew ktlintFormat && ./gradlew build` |

---

## Execution Plan

O agente implementa e prova; o autor revisa as decisões (ADRs) em paralelo; a entrevista fecha a fase.

- **Phase 1 — implementação (🤖):** T1 e T2 independentes; T3 → T4 → T6.
- **Phase 2 — revisão (🧑):** T5, que pode andar em paralelo à Phase 1.
- **Phase 3 — fechamento (🤖, interativa):** T7, depois de T1–T6.

---

## Task Breakdown

### Phase 1: Implementação

```
T1
T2
T3 -> T4 -> T6
```

#### T1: Beans de `Clock` e `IdGenerator`

**Owner**: 🤖
**What**: Configuração Spring que expõe `Clock.systemUTC()` e `UuidV7Generator(clock)` como `IdGenerator`.
**Where**: `src/main/kotlin/com/moneyout/infrastructure/config/TimeAndIdConfiguration.kt`
**Depends on**: None
**Reuses**: `infrastructure/id/UuidV7Generator.kt`, `MoneyOutApplicationTests.kt` (padrão de teste de contexto)
**Requirement**: F0-03

**Tools**:

- Skill: `tlc-spec-driven`; ler `.claude/rules/kotlin.md` e `.claude/rules/tests.md`

**Done when**:

- [x] Teste de contexto em `src/test/kotlin/com/moneyout/infrastructure/config/TimeAndIdConfigurationTest.kt` afirma: um único bean `Clock`, `zone == ZoneOffset.UTC`; bean `IdGenerator` é `UuidV7Generator`
- [x] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`
- [x] Test count: 13 → ≥ 15 (nenhum teste removido)

**Tests**: integration
**Gate**: build

**Commit**: `feat(infra): expoe Clock UTC e IdGenerator como beans`

---

#### T2: Regra ArchUnit contra `UUID.randomUUID()` no código puro

**Owner**: 🤖
**What**: Nova regra `pureCodeDoesNotGenerateRandomIds` em `ArchitectureTest` e evidência de que ela falha.
**Where**: `src/test/kotlin/com/moneyout/architecture/ArchitectureTest.kt`
**Depends on**: None
**Reuses**: regra `pureCodeDoesNotReadGlobalClock` (mesmo formato `callMethod`)
**Requirement**: F0-04

**Tools**:

- Skill: `tlc-spec-driven`; ler `.claude/rules/tests.md` (seção Arquitetura)

**Done when**:

- [x] Regra cobre `..domain..` e `..sharedkernel..`
- [x] Prova numa cópia descartável (nunca no working tree real): classe no `sharedkernel` chamando `UUID.randomUUID()` derruba só a regra nova; resultado registrado como F0-06 em `docs/experiments/fase-0.md`
- [x] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`
- [x] Test count: +1 regra ArchUnit, nenhuma removida

**Tests**: unit
**Gate**: build

**Commit**: `test(arch): proibe UUID.randomUUID no codigo puro`

---

#### T3: `Money` com aritmética segura

**Owner**: 🤖
**What**: `data class Money(minorUnits: Long, currency: Currency) : Comparable<Money>` com `plus`, `minus`, `unaryMinus`, `compareTo`, `isZero`, `isPositive`, `isNegative` e `Money.zero(currency)`; overflow via `Math.addExact`/`subtractExact` convertido em `DomainException`; moedas diferentes ⇒ `DomainException`.
**Where**: `src/main/kotlin/com/moneyout/sharedkernel/Money.kt`
**Depends on**: None
**Reuses**: `sharedkernel/Currency.kt`, `sharedkernel/DomainException.kt`; KDoc explicando o porquê de `Long` assinado (ADR-0002)
**Requirement**: F0-01

**Tools**:

- Skill: `tlc-spec-driven`; ler `.claude/rules/kotlin.md`, `.claude/rules/domain-model.md`, `.claude/rules/tests.md`

**Done when**:

- [x] `MoneyTest.kt` cobre ACs 1–3 de "Dinheiro seguro" e o edge case de `-Money(Long.MIN_VALUE)`
- [x] Teste de "moedas diferentes": se não for possível com uma moeda só, o motivo fica registrado no KDoc do teste e no commit (premissa da spec)
- [x] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`

**Tests**: unit
**Gate**: build

**Commit**: `feat(sharedkernel): adiciona Money em minor units`

---

#### T4: `Money.allocate` com property test

**Owner**: 🤖
**What**: `allocate(ratios: List<Int>): List<Money>` sem perder nem criar centavo, com o resto distribuído de 1 em 1 a partir das primeiras partes, sinal aplicado no fim e sem overflow em `total * ratio`.
**Where**: `src/main/kotlin/com/moneyout/sharedkernel/Money.kt`
**Depends on**: T3
**Requirement**: F0-02

**Done when**:

- [x] `MoneyAllocateTest.kt` com o exemplo concreto (R$ 100,00 em 3 ⇒ 33,34 + 33,33 + 33,33) e uma `checkAll` por propriedade: soma == total; tamanho == `ratios.size`; cada parte a menos de 1 minor unit da fração exata; ratios iguais ⇒ partes diferem no máximo em 1; entrada inválida ⇒ `DomainException`
- [x] Os três edge cases da spec cobertos (total negativo, `total * ratio` grande, `Long.MIN_VALUE`)
- [x] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`

**Tests**: unit
**Gate**: build

**Commit**: `feat(sharedkernel): Money.allocate sem perda de centavos`

---

#### T6: Exercícios de quebra

**Owner**: 🤖
**What**: Executar os 💥 da fase numa cópia descartável e registrar qual regra/teste falhou e por quê: `minorUnits` como `Double`; `Instant.now()` no `sharedkernel`; import de Spring no `sharedkernel`; `allocate` que ignora o resto (o property test precisa pegar).
**Where**: `docs/experiments/fase-0.md`
**Depends on**: T4
**Requirement**: F0-06

**Done when**:

- [x] Seção "Exercícios de quebra" com, para cada exercício, a saída relevante do build e a explicação em linguagem de entrevista
- [x] Working tree real intacto (`git status` igual ao de antes do exercício)

**Tests**: none
**Gate**: build

**Commit**: `docs(experiments): registra exercicios de quebra da Fase 0`

---

### Phase 2: Revisão do autor

```
T5
```

#### T5: Revisar os ADRs da fase

**Owner**: 🧑
**What**: Ler e editar com as próprias palavras os ADRs 0001, 0002, 0004, 0011 e 0012, usando o roteiro de revisão do `docs/fases/fase0-guia.md`; mudar status para Aceita (ou Rejeitada) e atualizar o índice.
**Where**: `docs/adr/README.md`
**Depends on**: None
**Requirement**: F0-05

**Done when**:

- [x] Cinco ADRs com status final e índice coerente
- [x] Divergências viram ADR substituto ou pedido de mudança antes do fechamento

**Tests**: none
**Gate**: build

**Commit**: `docs(adr): revisa e aceita ADRs da Fase 0`

---

### Phase 3: Fechamento

```
T7
```

#### T7: Validação e entrevista da Fase 0

**Owner**: 🤖 (**interativa**: precisa do autor)
**What**: Verifier independente contra a spec (com discrimination sensor) e entrevista (5–8 perguntas, uma por vez, feedback só depois da resposta), registrada.
**Where**: `docs/entrevistas/fase0.md`
**Depends on**: T1, T2, T3, T4, T5, T6
**Reuses**: perguntas sugeridas em `docs/PLANO.md` (Fase 0, 🎤)
**Requirement**: F0-07

**Done when**:

- [ ] `.specs/features/fase0-fundacoes/validation.md` com veredito PASS e evidência `arquivo:linha` por AC
- [ ] Entrevista registrada; perguntas fracas anotadas para voltarem na Fase 1
- [ ] `python3 .claude/skills/tlc-spec-driven/scripts/validate_state.py fase0-fundacoes` passa

**Tests**: none
**Gate**: build

**Commit**: `docs(fase0): validacao e entrevista de fechamento`
