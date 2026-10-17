# Fase 0 — Fundações Validation

**Date**: 2026-10-17 (re-verificação, iteração 1 de 3)
**Spec**: `.specs/features/fase0-fundacoes/spec.md`
**Diff range**: `ac13255..HEAD` (HEAD = 6c4b587; inclui ba22981 `fix(sharedkernel): allocate nao da centavo extra a parte ja exata`)
**Verifier**: independent sub-agent (author ≠ verifier)
**Verdict**: **PASS** (ACs de Dinheiro seguro e Tempo e ids com evidência; nenhum mutante relevante sobrevive; AC3 de Fechamento/entrevista fora do escopo)

Escopo do Verifier: ACs 1–6 + 3 edge cases de "Dinheiro seguro", ACs 1–3 de "Tempo e ids injetáveis", ACs 1–2 de "Fechamento didático" (por leitura). AC 3 de "Fechamento" (entrevista, F0-07/T7) está **fora do escopo / pendente** (`docs/entrevistas/fase0.md` não existe).

---

## Task Completion

| Task | Status | Notes |
| ---- | ------ | ----- |
| T1 | Done | `TimeAndIdConfiguration.kt` + `TimeAndIdConfigurationTest.kt` |
| T2 | Done | regra `pureCodeDoesNotGenerateRandomIds`; prova F0-06 em `docs/experiments/fase-0.md:57` |
| T3 | Done | `Money.kt` aritmética segura |
| T4 | Done | `allocate` + `MoneyAllocateTest`; corrigido por ba22981 |
| T5 | Done (ressalva) | ADRs com status Aceita; commit ee2968f tem autor "Claude", não o autor humano (T5 é Owner 🧑). Ver gap 4 |
| T6 | Done | seção "Exercícios de quebra" |
| T7 | Pendente | entrevista fora do escopo do Verifier |

---

## Spec-Anchored Acceptance Criteria

### P1: Dinheiro seguro

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --------- | -------------------- | ----------------------- | ------ |
| AC1 soma/subtração mesma moeda | resultado exato em minor units | `src/test/kotlin/com/moneyout/sharedkernel/MoneyTest.kt:31` `result shouldBe brl(11_345)` (10_050+1_295); `MoneyTest.kt:44` `result shouldBe brl(-1_500)`; `MoneyTest.kt:163` property `(a+b)-b shouldBe a` | ✅ PASS (mutantes M1, M2, M18 mortos) |
| AC2 moedas diferentes ⇒ DomainException (soma, subtração, comparação) | `DomainException` | sem teste: só existe `Currency.BRL` (`Currency.kt`), premissa aceita na spec (Assumptions, linha 31); registrado no KDoc `MoneyTest.kt:14-17`. Guard existe: `Money.kt:71-73` `requireSameCurrency`, chamado em `plus` (`:17`), `minus` (`:22`), `compareTo` (`:30`) | ⚠️ Gap conhecido/aceito (não é falha). Mutantes M7/M8 sobrevivem por serem equivalentes com 1 moeda |
| AC3 overflow soma/subtração/negação ⇒ DomainException | `DomainException` | `MoneyTest.kt:53` `shouldThrow<DomainException> { max + brl(1) }`; `MoneyTest.kt:65` `shouldThrow<DomainException> { min - brl(1) }`; `MoneyTest.kt:77` `shouldThrow<DomainException> { -min }` | ✅ PASS (M1, M2, M3 mortos) |
| AC4 allocate: mesmo tamanho, soma == total | lista do mesmo tamanho, soma exata | `MoneyAllocateTest.kt:142` `parts.sumOf { it.minorUnits.toBigInteger() } shouldBe big(total)` (property, bordas incl. MIN/MAX); `MoneyAllocateTest.kt:155` `parts.size shouldBe ratios.size`; exemplo `MoneyAllocateTest.kt:45` `parts shouldBe listOf(brl(3_334), brl(3_333), brl(3_333))` | ✅ PASS (M10, M11, M17 mortos) |
| AC5 diferença < 1 minor unit; resto a partir das primeiras partes | erro `< 1`; resto distribuído do início | `MoneyAllocateTest.kt:170-171` `error = |part*soma - total*ratio|` e `(error < sum) shouldBe true` (erro estritamente < 1 em aritmética inteira); `MoneyAllocateTest.kt:45` (centavo na 1ª parte); `MoneyAllocateTest.kt:57` (negativo); `MoneyAllocateTest.kt:69` `parts shouldBe listOf(brl(1), brl(1), brl(0))` (caso do ba22981, ratios `[2,1,1]` sobre 2); `MoneyAllocateTest.kt:185` ratios iguais diferem ≤ 1 | ✅ PASS (M4 morto, M9 morto = o teste de regressão do ba22981 discrimina). Ver spec-precision gap A |
| AC6 ratios vazio ou ≤ 0 ⇒ DomainException | `DomainException` | `MoneyAllocateTest.kt:109` `shouldThrow<DomainException> { total.allocate(emptyList()) }`; `:121` ratio 0; `:127` ratio -3; property `:197` `shouldThrow<DomainException>` para qualquer lista inválida | ✅ PASS (M5, M6 mortos) |

**Edge cases**

| Edge case | Spec-defined outcome | `file:line` + assertion | Result |
| --------- | -------------------- | ----------------------- | ------ |
| `allocate` com total negativo | mesmo contrato com sinal | `MoneyAllocateTest.kt:57` `parts shouldBe listOf(brl(-3_334), brl(-3_333), brl(-3_333))`; propriedades (`:142`, `:171`) usam `Arb.long()` negativo | ✅ PASS (M10, M11 mortos) |
| `total * ratio` estoura Long | sem overflow ou DomainException | `MoneyAllocateTest.kt:81` soma `shouldBe big(Long.MAX_VALUE)` e `:84` valores exatos com `Int.MAX_VALUE` ratios | ✅ PASS |
| `-Money(Long.MIN_VALUE)` | DomainException | `MoneyTest.kt:77` `shouldThrow<DomainException> { -min }`; `allocate(MIN_VALUE)` em `MoneyAllocateTest.kt:97,100` | ✅ PASS (M3 morto) |

### P1: Tempo e ids injetáveis

| Criterion | Spec-defined outcome | `file:line` + assertion | Result |
| --------- | -------------------- | ----------------------- | ------ |
| AC1 exatamente um bean `Clock` com zona UTC | 1 bean, `zone == UTC` | `src/test/kotlin/com/moneyout/infrastructure/config/TimeAndIdConfigurationTest.kt:26` `clocks.size shouldBe 1` e `:27` `clocks.values.single().zone shouldBe ZoneOffset.UTC` | ✅ PASS (M13 morto) |
| AC2 bean `IdGenerator` = `UuidV7Generator` **usando esse Clock** | tipo + ligação ao Clock | `TimeAndIdConfigurationTest.kt:36` `generators.size shouldBe 1`, `:37` `shouldBeInstanceOf<UuidV7Generator>()`; ligação ao Clock: `:47` `TimeAndIdConfiguration().idGenerator(clock).next()` e `:50` `(uuid.mostSignificantBits ushr TIMESTAMP_SHIFT) shouldBe fixedInstant.toEpochMilli()` | ✅ PASS (M14 e M15 mortos após o commit 6c4b587) |
| AC3 `UUID.randomUUID()` em `..domain..`/`..sharedkernel..` ⇒ build falha | `ArchitectureTest` falha | regra `src/test/kotlin/com/moneyout/architecture/ArchitectureTest.kt:77-82` `noClasses().that().resideInAnyPackage(DOMAIN, KERNEL).should().callMethod(UUID::class.java, "randomUUID")`. Verifier injetou `Bad` no `sharedkernel` em cópia: `ArchitectureTest > pureCodeDoesNotGenerateRandomIds FAILED`, 38 testes, 1 falha | ✅ PASS (M19 morto; evidência também em `docs/experiments/fase-0.md:57-70`) |

### P2: Fechamento didático

| Criterion | Spec-defined outcome | Evidência (leitura) | Result |
| --------- | -------------------- | ------------------- | ------ |
| AC1 ADRs 0001, 0002, 0004, 0011, 0012 Aceita/Rejeitada, revisados pelo autor | status final | `docs/adr/0001-stack-e-versoes.md:3`, `0002-...md:3`, `0004-...md:3`, `0011-...md:3`, `0012-...md:3` = "Aceita"; `docs/adr/README.md:7,8,10,12,13` coerente | ✅ PASS no status; "revisados pelo autor" não verificável pelo git (gap 4) |
| AC2 seção "Exercícios de quebra" preenchida com a regra que falhou | seção com regra por exercício | `docs/experiments/fase-0.md:72` seção + tabela com ArchUnit `pureCodeDoesNotUseFloatingPoint`, `pureCodeDoesNotReadGlobalClock`, `pureCodeHasNoFrameworkDependency`, `MoneyAllocateTest` | ✅ PASS |
| AC3 entrevista 5–8 perguntas | `docs/entrevistas/fase0.md` | arquivo inexistente (`docs/entrevistas/` ausente) | ⏭️ Pendente / fora do escopo do Verifier (T7/F0-07) |

**Spec-precision gaps**

- A. AC5 não define o que acontece com partes de fração exata (ratios `[2,1,1]`): "centavos restantes distribuídos a partir das primeiras partes" lida literalmente colide com "diferença < 1". A correção ba22981 escolhe pular partes exatas (restrição < 1 prevalece); o teste `MoneyAllocateTest.kt:69` fixa essa leitura. Recomenda-se explicitar na spec ("a partir das primeiras partes de fração não inteira"). O KDoc (`Money.kt:40-47`) documenta.
- B. AC2 (Tempo): "usando esse Clock" não diz como observar a ligação (ex.: Clock fixo injetado). Falta critério verificável.
- C. AC2 (Dinheiro) e "comparados": sem segunda moeda o AC é inverificável por teste (aceito).

**Status**: ⚠️ ACs de Dinheiro seguro todos com evidência (AC2 aceito); ❌ Tempo e ids AC2 agora com evidência (ligação ao Clock, `TimeAndIdConfigurationTest.kt:50`).

---

## Discrimination Sensor

Cópia descartável em `scratchpad/verifier_copy` (tar sem `.git/build/.gradle/.kotlin`), `./gradlew test --continue` por mutante. Tier: P0 (dinheiro), ≥ 5 mutantes manuais; foram 19.

| # | Arquivo | Mutação | Killed? |
| - | ------- | ------- | ------- |
| M1 | `Money.kt:18` | `plus` com `+` (sem `addExact`) | ✅ `MoneyTest > Should throw DomainException when adding beyond Long MAX_VALUE` |
| M2 | `Money.kt:23` | `minus` com `-` (sem `subtractExact`) | ✅ `...subtracting below Long MIN_VALUE` |
| M3 | `Money.kt:27` | `unaryMinus` com `-minorUnits` | ✅ `...negating Long MIN_VALUE` |
| M4 | `Money.kt:63` | resto distribuído do fim para o início | ✅ 4 testes (exemplos 33,34 / negativo / exata / overflow) |
| M5 | `Money.kt:53` | remove validação `ratio <= 0` | ✅ 2 testes (exemplo + property) |
| M6 | `Money.kt:53` | `ratio <= 0` → `ratio < 0` (aceita zero) | ✅ 2 testes |
| M7 | `Money.kt:30` | `compareTo` ignora moeda | ❌ Sobrevive (equivalente com só BRL; gap aceito) |
| M8 | `Money.kt:17` | `plus` sem `requireSameCurrency` | ❌ Sobrevive (idem) |
| M9 | `Money.kt:64` | reverte ba22981 (`isFractional = true`) | ✅ `Should not give an extra minor unit to a part whose exact fraction is already whole` |
| M10 | `Money.kt:60` | allocate sem aplicar sinal | ✅ 6+ testes |
| M11 | `Money.kt:55` | allocate sem `abs()` | ✅ 6+ testes |
| M12 | `Money.kt:36` | `isPositive` `>` → `>=` | ✅ `Should classify the sign...` |
| M13 | `TimeAndIdConfiguration.kt:16` | Clock com zona America/Sao_Paulo | ✅ `Should expose exactly one Clock in UTC` |
| M14 | `TimeAndIdConfiguration.kt:19` | `UuidV7Generator(Clock.systemUTC())` em vez do `clock` injetado | ✅ `Should build the IdGenerator from the injected Clock` (sobrevivia na iteração 0; morto após 6c4b587) |
| M15 | `TimeAndIdConfiguration.kt:19` | IdGenerator anônimo com `UUID.randomUUID()` | ✅ `Should expose an IdGenerator implemented by UuidV7Generator` e `Should build the IdGenerator from the injected Clock` |
| M16 | `ArchitectureTest.kt:82` | método da regra renomeado (`"randomUUIDx"`) | ❌ Sobrevive, esperado: um teste de regra não detecta a própria neutralização; a prova é o M19 |
| M17 | `Money.kt:65` | resto nunca distribuído | ✅ 5 testes |
| M18 | `Money.kt:23` | `minus` com operandos invertidos | ✅ 2 testes |
| M19 | `sharedkernel/Bad.kt` (novo) | classe com `UUID.randomUUID()` | ✅ `ArchitectureTest > pureCodeDoesNotGenerateRandomIds FAILED` |

**Sensor depth**: P0-full manual (19 mutantes). **Result**: 16 mortos, 3 sobreviventes, todos aceitos (M7 e M8 equivalentes com só BRL; M16 inerente à natureza da regra, provada por M19). Na re-verificação M13, M14 e M15 foram reexecutados em cópia nova: todos mortos. ✅ PASS.

Isolamento: `git status --porcelain` da árvore real vazio antes e depois (idêntico); cópia removida.

---

## Code Quality / conformidade com `tests.md` e `tests/gherkin.md`

| Princípio | Status |
| --------- | ------ |
| Nome de teste em inglês com crases | ✅ todos os 38 testes |
| Gherkin pt-BR em todo teste, `@Quando`/`@Então` presentes | ✅ (com ressalvas abaixo) |
| Property tests via `runBlocking { checkAll }` com bordas (`Long.MIN/MAX`, `Int.MAX_VALUE`) | ✅ `MoneyAllocateTest.kt:23-32` |
| Contagem de testes só cresce | ✅ 13 → 38 (T1 pedia ≥ 15; nenhum removido) |
| Domínio sem `Double`/`Instant.now()`/`UUID.randomUUID()` | ✅ ArchUnit verde |
| Cada teste mapeia a AC/edge case | ✅ (ver tabelas) |

Desvios menores (não bloqueiam):

1. `TimeAndIdConfigurationTest.kt:230,240` juntam `@Dado ..., @Quando ...` num único comentário (gherkin.md: "um comentário por bloco"; tags separadas).
2. `MoneyOutApplicationTests.kt` (`Should start the application context`): corpo vazio com um único comentário `@Quando ..., @Então ...`; sem `@Dado` nem código após as tags (aceitável, é teste de fumaça).
3. `UuidV7GeneratorTest.kt` (teste "order ids by time"): comentário `//` solto antes do `@Então` (gherkin.md: "sem comentário solto"); não é tag Gherkin, só nota explicativa.
4. `MoneyTest.kt` (property `Should return the original value...`) coloca `val safe` entre o `@Dado` e o `checkAll`, correto; sem desvio.
5. `MoneyAllocateTest.kt:165-166`: `val sum` e `val parts` dentro do `@Quando` (cálculo auxiliar da asserção misturado à ação); estilístico.

---

## Gate Check

- **Gate command**: `./gradlew -Dorg.gradle.java.installations.paths=<jdk25> ktlintFormat build --console=plain` (JDK 25 local) e `./gradlew test --rerun`
- **Result**: BUILD SUCCESSFUL; 39 tests, 0 failed, 0 skipped, 0 errors
- **Test count before feature** (ac13255): 13 (tasks.md T1) · **after**: 39 · **Delta**: +26
- Por classe: MoneyAllocateTest 12, MoneyTest 10, ArchitectureTest 7, UuidV7GeneratorTest 4, TimeAndIdConfigurationTest 3, CurrencyTest 2, MoneyOutApplicationTests 1
- `ktlintFormat` não alterou arquivos.

---

## Gaps e observações

1. ~~Major: M14 sobrevive~~ **Resolvido** em 6c4b587 (`TimeAndIdConfigurationTest.kt:41-51`); M14 morto na re-verificação.
2. Observação para o autor (spec-precision A, AC5, `spec.md:52`): explicitar que o resto vai às primeiras partes de fração não inteira (consequência do ba22981; fixado por `MoneyAllocateTest.kt:69`).
3. Observação para o autor (spec-precision B, AC2 de Tempo): definir como "usando esse Clock" é observado (agora observado por `TimeAndIdConfigurationTest.kt:50`).
4. Observação para o autor (processo): ADRs com status Aceita, mas o commit ee2968f tem autor "Claude"; a revisão pelo autor não é comprovável pelo git.
5. Gap aceito: AC2 de Dinheiro sem teste (só BRL); guard em `Money.kt:17,22,30,71`. Com a 2ª moeda, adicionar testes (M7/M8).
6. Gherkin: os desvios menores da iteração 0 foram corrigidos em 6c4b587 (blocos separados em `TimeAndIdConfigurationTest.kt:22,32`, `MoneyOutApplicationTests` com `@Quando`/`@Então`, comentário solto removido de `UuidV7GeneratorTest`).
7. Pendente, fora da responsabilidade do Verifier: AC3 de Fechamento (entrevista, T7/F0-07; `docs/entrevistas/fase0.md` inexistente).

---

## Requirement Traceability Update

| Requirement | Previous | New |
| ----------- | -------- | --- |
| F0-01 | Done | ✅ Verified (AC2 aceito sem teste) |
| F0-02 | Done | ✅ Verified |
| F0-03 | Done | ✅ Verified |
| F0-04 | Done | ✅ Verified |
| F0-05 | Done | ⚠️ Status verificado; revisão pelo autor não comprovável |
| F0-06 | Done | ✅ Verified |
| F0-07 | Pending | ⏭️ Fora do escopo do Verifier |

---

## Summary

**Overall**: ✅ Ready (escopo do Verifier; entrevista F0-07 pendente)

**Spec-anchored check**: todos os ACs de código com evidência (AC2 de Dinheiro aceito sem teste); 2 spec-precision gaps como observação
**Sensor**: 16/19 mortos; 3 sobreviventes aceitos (M7, M8, M16)
**Gate**: 39 passed

**What works**: aritmética exata e overflow; `allocate` correto incluindo `[2,1,1]`; Clock UTC e IdGenerator ligado ao Clock; regra ArchUnit provada por violação (M19).

**Next steps**: T7 (entrevista).
