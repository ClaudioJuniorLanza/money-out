# Fase 0 — Fundações (fechamento) Tasks

## Execution Protocol (MANDATORY -- do not skip)

Implement these tasks with the `tlc-spec-driven` skill (vendorizada em `.claude/skills/tlc-spec-driven/`): **activate it by name and follow its Execute flow and Critical Rules.** Do not search for skill files by filesystem path.

**If the skill cannot be activated, STOP and tell the user - do not proceed without it.**

Antes de começar qualquer task, leia [`docs/fluxo-de-trabalho.md`](../../../docs/fluxo-de-trabalho.md): ele define quem executa cada task (**Owner**), onde (local × nuvem) e o protocolo de branch/PR.

- **Owner 🧑** — do autor, na máquina local. Agentes **não** implementam; podem oferecer dicas, testes e revisão.
- **Owner ☁️** — delegável a um agente na nuvem.
- **Owner 🤖** — agente local, em sessão com o autor (guias, revisão, entrevista).

---

**Spec**: `.specs/features/fase0-fundacoes/spec.md`
**Status**: Draft — aguardando aprovação do autor

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

Duas trilhas independentes: a ☁️ pode rodar em paralelo à 🧑 (arquivos diferentes, sem conflito). A entrevista fecha a fase.

```
Trilha ☁️:  T1    T2                    (independentes)
Trilha 🧑:  T3 -> T4 -> T6
            T5                          (independente)
Fecho 🤖:   T7  (depois de T1..T6)
```

---

## Task Breakdown

### Phase 1: Trilha da nuvem

```
T1
T2
```

#### T1: Beans de `Clock` e `IdGenerator`

**Owner**: ☁️
**What**: Configuração Spring que expõe `Clock.systemUTC()` e `UuidV7Generator(clock)` como `IdGenerator`.
**Where**: `src/main/kotlin/com/moneyout/infrastructure/config/TimeAndIdConfiguration.kt`
**Depends on**: None
**Reuses**: `infrastructure/id/UuidV7Generator.kt`, `MoneyOutApplicationTests.kt` (padrão de teste de contexto)
**Requirement**: F0-03

**Tools**:

- MCP: NONE
- Skill: `tlc-spec-driven`; ler `.claude/rules/kotlin.md` e `.claude/rules/tests.md`

**Done when**:

- [ ] Teste de contexto em `src/test/kotlin/com/moneyout/infrastructure/config/TimeAndIdConfigurationTest.kt` afirma: um único bean `Clock`, `zone == ZoneOffset.UTC`; bean `IdGenerator` é `UuidV7Generator`
- [ ] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`
- [ ] Test count: 12 → ≥ 14 (nenhum teste removido)

**Tests**: integration
**Gate**: build

**Commit**: `feat(infra): expoe Clock UTC e IdGenerator como beans`

---

#### T2: Regra ArchUnit contra `UUID.randomUUID()` no código puro

**Owner**: ☁️
**What**: Nova regra `pureCodeDoesNotGenerateRandomIds` em `ArchitectureTest` e evidência de que ela falha.
**Where**: `src/test/kotlin/com/moneyout/architecture/ArchitectureTest.kt`
**Depends on**: None
**Reuses**: regra `pureCodeDoesNotReadGlobalClock` (mesmo formato `callMethod`)
**Requirement**: F0-04

**Tools**:

- MCP: NONE
- Skill: `tlc-spec-driven`; ler `.claude/rules/tests.md` (seção Arquitetura)

**Done when**:

- [ ] Regra cobre `..domain..` e `..sharedkernel..`
- [ ] Prova numa cópia descartável (nunca no working tree real): classe no `sharedkernel` chamando `UUID.randomUUID()` derruba só a regra nova; resultado registrado como F0-05 em `docs/experiments/fase-0.md`
- [ ] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`
- [ ] Test count: +1 regra ArchUnit, nenhuma removida

**Tests**: unit
**Gate**: build

**Commit**: `test(arch): proibe UUID.randomUUID no codigo puro`

---

### Phase 2: Trilha do autor

```
T3 -> T4 -> T6
T5
```

#### T3: Digitar `Money` e escrever os testes de borda

**Owner**: 🧑
**What**: `Money` conforme o 📋 do guia + `MoneyTest` com os seis cenários do guia.
**Where**: `src/main/kotlin/com/moneyout/sharedkernel/Money.kt`
**Depends on**: None
**Reuses**: `docs/fases/fase0-guia.md` §1, `CurrencyTest.kt`
**Requirement**: F0-01

**Done when**:

- [ ] ACs 1–3 de "Dinheiro seguro" cobertos em `MoneyTest.kt` (um teste por cenário do guia)
- [ ] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`

**Tests**: unit
**Gate**: build

**Commit**: `feat(sharedkernel): adiciona Money em minor units`

---

#### T4: Implementar `Money.allocate` com property test

**Owner**: 🧑
**What**: `allocate(ratios)` sem perder centavo + `MoneyAllocateTest` com uma `checkAll` por propriedade.
**Where**: `src/main/kotlin/com/moneyout/sharedkernel/Money.kt`
**Depends on**: T3
**Reuses**: `docs/fases/fase0-guia.md` §2 (contrato e esqueleto)
**Requirement**: F0-02

**Done when**:

- [ ] ACs 4–6 e os três edge cases da spec cobertos em `MoneyAllocateTest.kt`
- [ ] Exemplo concreto: R$ 100,00 em 3 ⇒ 33,34 + 33,33 + 33,33
- [ ] Gate check passes: `./gradlew ktlintFormat && ./gradlew build`

**Tests**: unit
**Gate**: build

**Commit**: `feat(sharedkernel): Money.allocate sem perda de centavos`

---

#### T5: Revisar os ADRs da fase

**Owner**: 🧑
**What**: Ler e editar com as próprias palavras os ADRs 0001, 0002, 0004, 0011 e 0012; mudar status para Aceita (ou Rejeitada) e atualizar o índice.
**Where**: `docs/adr/`
**Depends on**: None
**Reuses**: `docs/fases/fase0-guia.md` §3
**Requirement**: F0-05

**Done when**:

- [ ] Cinco ADRs com status final e índice coerente

**Tests**: none
**Gate**: build

**Commit**: `docs(adr): revisa e aceita ADRs da Fase 0`

---

#### T6: Exercícios de quebra

**Owner**: 🧑
**What**: Fazer os quatro 💥 do guia e registrar o que falhou e por quê.
**Where**: `docs/experiments/fase-0.md` (seção "Exercícios de quebra do autor")
**Depends on**: T4
**Reuses**: `docs/fases/fase0-guia.md` §💥
**Requirement**: F0-06

**Done when**:

- [ ] Cada exercício com a regra que falhou e a explicação
- [ ] Working tree limpo depois (`git status` sem sobras do exercício)

**Tests**: none
**Gate**: build

**Commit**: `docs(experiments): registra exercicios de quebra da Fase 0`

---

### Phase 3: Fechamento

```
T7
```

#### T7: Revisão final e entrevista da Fase 0

**Owner**: 🤖 (sessão local com o autor)
**What**: Revisar T1–T6 contra a spec (Verifier da skill), conduzir a entrevista (5–8 perguntas, uma por vez, feedback só depois da resposta) e registrá-la.
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
