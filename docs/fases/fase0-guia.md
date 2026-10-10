# Fase 0 — Guia da sua trilha 🧑

Tempo estimado: ≈ 1 h. O scaffolding (Gradle, CI, ArchUnit, `Currency`, `IdGenerator`/`UuidV7Generator`) já está pronto — **sem** `Money`, que é seu. Leia antes o [ADR-0011](../adr/0011-estrutura-hexagonal-ddd-e-shared-kernel.md): explica por que `Money` mora no `sharedkernel` e por que o gerador de UUID mora na infraestrutura.

Antes de começar:

```bash
./gradlew build        # deve passar sem o Money
```

Regras do jogo: **digite** o código (não copie/cole), escreva os testes com as suas palavras e commite em passos pequenos (Conventional Commits: `feat: ...`, `test: ...`, `docs: ...`).

---

## 1. ⭐ Digite o `Money` (≈ 30 min com os testes)

Arquivo: `src/main/kotlin/com/moneyout/sharedkernel/Money.kt`

📋

```kotlin
package com.moneyout.sharedkernel

/**
 * Dinheiro como inteiro em unidade mínima + moeda (ADR-0002).
 * É assinado de propósito: diferenças e ajustes podem ser negativos.
 * Quem exige valor positivo (ex.: o valor de uma Transfer) valida isso no seu próprio domínio.
 */
data class Money(val minorUnits: Long, val currency: Currency) : Comparable<Money> {
    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(addExact(minorUnits, other.minorUnits), currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return Money(subtractExact(minorUnits, other.minorUnits), currency)
    }

    operator fun unaryMinus(): Money = Money(subtractExact(0L, minorUnits), currency)

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return minorUnits.compareTo(other.minorUnits)
    }

    fun isZero(): Boolean = minorUnits == 0L

    fun isPositive(): Boolean = minorUnits > 0L

    fun isNegative(): Boolean = minorUnits < 0L

    /**
     * Divide este valor em partes proporcionais a [ratios] SEM perder nem criar centavos.
     * Exemplo: R$ 100,00 em 3 partes iguais => 33,34 + 33,33 + 33,33.
     * Veja a sua tarefa 2.
     */
    fun allocate(ratios: List<Int>): List<Money> {
        TODO("sua tarefa 2")
    }

    private fun requireSameCurrency(other: Money) {
        if (currency != other.currency) {
            throw DomainException("Moedas diferentes: $currency e ${other.currency}")
        }
    }

    companion object {
        fun zero(currency: Currency): Money = Money(0L, currency)

        private fun addExact(a: Long, b: Long): Long =
            try {
                Math.addExact(a, b)
            } catch (e: ArithmeticException) {
                throw DomainException("Estouro ao somar valores monetários")
            }

        private fun subtractExact(a: Long, b: Long): Long =
            try {
                Math.subtractExact(a, b)
            } catch (e: ArithmeticException) {
                throw DomainException("Estouro ao subtrair valores monetários")
            }
    }
}
```

Perguntas para você responder para si mesmo enquanto digita (voltam na entrevista):
- Por que `data class` com `Long` e não `BigDecimal`? O que o `equals` de `BigDecimal("10.0")` vs `BigDecimal("10.00")` faria aqui?
- Por que `Math.addExact` em vez de `+`?
- Por que `compareTo` lança erro para moedas diferentes em vez de comparar os números?

### Seus testes (escreva com as suas palavras)

Arquivo: `src/test/kotlin/com/moneyout/sharedkernel/MoneyTest.kt`. Cenários mínimos, um teste por linha:

1. soma e subtração simples;
2. somar moedas diferentes lança `DomainException` (hoje só existe BRL: pense em como testar isso — dica: o que o compilador deixa você fazer com uma moeda só? Se não der, registre no seu commit por que o teste fica para quando entrar a 2ª moeda);
3. overflow em `plus` e em `minus` lança `DomainException`;
4. `-Money(Long.MIN_VALUE)` lança `DomainException` (por quê?);
5. zero, positivo, negativo;
6. igualdade: `Money(100, BRL) == Money(100, BRL)`.

Template do formato (no estilo dos testes que já existem em `CurrencyTest`):

```kotlin
@Test
fun `soma duas quantias na mesma moeda`() {
    val total = Money(1050, Currency.BRL) + Money(250, Currency.BRL)

    total shouldBe Money(1300, Currency.BRL)
}
```

---

## 2. ⭐ Implemente `Money.allocate` (≈ 30 min)

Contrato:

- `ratios` não pode ser vazio, nem ter valor `<= 0` (senão `DomainException`);
- o resultado tem **o mesmo tamanho** de `ratios`, na mesma ordem;
- a **soma das partes é exatamente igual ao total**;
- cada parte difere da fração exata `total * ratio / somaDosRatios` em **menos de 1 centavo**;
- o resto (os centavos que sobram) é distribuído **de 1 em 1 centavo, começando pelas primeiras partes**;
- funciona para total negativo (dica: pense em aplicar o sinal só no final);
- cuidado com overflow em `total * ratio` (dica: o que acontece com `Long.MAX_VALUE`?).

Dicas, sem entregar a solução: calcule a parte "de piso" de cada ratio, some, e veja quanto sobrou; esse "sobrou" é sempre menor que `ratios.size`.

### Property test (o ponto central deste exercício)

Arquivo: `src/test/kotlin/com/moneyout/sharedkernel/MoneyAllocateTest.kt`. O esqueleto abaixo gera valores aleatórios; **você escreve as asserções** das propriedades do contrato (uma `checkAll` por propriedade).

📋

```kotlin
package com.moneyout.sharedkernel

import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class MoneyAllocateTest {
    @Test
    fun `R$ 100,00 em 3 partes iguais`() {
        // exemplo concreto primeiro: o que você espera exatamente?
    }

    @Test
    fun `a soma das partes e sempre o total`() =
        runBlocking {
            checkAll(Arb.long(0L..1_000_000_000_000L), Arb.list(Arb.int(1..1_000), 1..10)) { total, ratios ->
                val parts = Money(total, Currency.BRL).allocate(ratios)
                // TODO (sua): afirme a propriedade
            }
        }
}
```

Propriedades a cobrir (pelo menos): soma == total; tamanho == `ratios.size`; cada parte dentro de 1 centavo da fração exata; ratios iguais ⇒ partes diferem no máximo em 1 centavo; entrada inválida lança `DomainException`.

---

## 3. (15 min) Revise os ADRs

Leia `docs/adr/0001`, `0002`, `0004` e `0011` e edite com as suas palavras o que discordar. Quando estiver de acordo, mude o status de **Proposta** para **Aceita** (e atualize `docs/adr/README.md`).

---

## 4. Valide e commite

```bash
./gradlew ktlintFormat   # formata
./gradlew build          # compila, lint, testes e regras de arquitetura (ArchUnit)
```

Critérios da fase (do [`PLANO.md`](../PLANO.md)): build verde local e no CI; ArchUnit falha se o domínio importar Spring; `Money` não soma moedas distintas; `allocate` nunca perde/ganha centavo (property test); nenhum `now()` direto no domínio.

Quando terminar, me avise: eu reviso o seu código, rodamos os 💥 exercícios de quebra da fase e fazemos a 🎤 entrevista.

## 💥 Exercícios de quebra (depois que o build estiver verde)

1. No `Money.kt`, troque `val minorUnits: Long` por `Double` e rode `./gradlew build`: qual regra falha e por quê?
2. Em uma classe do `sharedkernel` (ou de um `..domain..`), chame `Instant.now()` e rode o build: qual regra falha?
3. Em `Currency.kt` (no `sharedkernel`), importe `org.springframework.stereotype.Component`: qual regra falha?
4. Desfaça tudo (`git checkout -- .`).

Registre o que observou em `docs/experiments/fase-0.md`.
