# Regra: documentação Gherkin nos testes

Complemento de estilo de `../tests.md`. Não é BDD real (sem `.feature`, sem Cucumber): são comentários `/** */` com tags Gherkin que deixam qualquer pessoa ler o cenário sem entender o Kotlin.

## Obrigatoriedade

**Todo teste** (unit, property, integração, `@WebMvcTest`) tem os blocos Gherkin, inclusive teste de value object de uma linha. Única exceção: regras do ArchUnit, que não são funções `@Test`.

Mínimo por teste: `@Quando` e `@Então`; `@Dado` sempre que houver pré-condição (estado, fixture, entrada). Não há exceção por tamanho do teste.

## Tags

| Tag | Uso |
|---|---|
| `@Dado` | pré-condição / estado inicial |
| `@Quando` | ação sob teste |
| `@Então` | resultado esperado |
| `@E` | condição ou verificação adicional |
| `@Mas` | exceção a uma condição anterior (raro) |

## Regras

- Comentários Gherkin em **pt-BR**; **nome da função de teste em inglês** descritivo, com crases: `` `Should reject allocation when ratios are empty` ``.
- Cada bloco precede **imediatamente** o código que descreve; sem comentário solto, sem tag depois do código.
- **Asserções só depois de `@Então`/`@E`**, nunca antes.
- Um comentário por bloco lógico, não por linha.
- Exceção esperada: `shouldThrow` envolve a ação; o `@Quando` fica dentro do lambda e o `@Então` antes dele (a exceção é o resultado), seguido de `@E` para a mensagem.
- Property test: `@Dado` descreve o domínio dos geradores; `@Quando` a operação; `@Então` a propriedade verificada, dentro do `checkAll`.
- Teste parametrizado: Gherkin descreve o cenário genérico, não cada linha de dados.

## Exemplos

```kotlin
@Test
fun `Should not approve a completed transfer`() {
    /** @Dado uma transfer já concluída */
    val transfer = COMPLETED_TRANSFER

    /** @Então uma exceção de transição inválida deve ser lançada */
    val exception = shouldThrow<InvalidTransferTransition> {
        /** @Quando a aprovação for solicitada */
        transfer.approve(at = NOW)
    }

    /** @E a mensagem não deve conter dados pessoais */
    exception.message shouldBe "Invalid transition from COMPLETED on APPROVE"
}

@Test
fun `Should keep the sum of parts equal to the total`() =
    runBlocking<Unit> {
        /** @Dado qualquer total (incluindo bordas) e qualquer lista de ratios positivos */
        checkAll(totals, ratios) { total, ratios ->
            /** @Quando o total é alocado */
            val parts = brl(total).allocate(ratios)

            /** @Então nenhum centavo é perdido ou criado */
            parts.sumOf { it.minorUnits.toBigInteger() } shouldBe total.toBigInteger()
        }
    }
```

## Anti-padrões

```kotlin
// ❌ comentário sem tag
// cria o pedido

// ❌ tag depois do código
val result = order.cancel()
/** @Então o status deve ser CANCELLED */

// ❌ comentário Gherkin em inglês
/** @Given a valid command */

// ❌ asserção antes de @Então
parts shouldBe expected
/** @Quando ... */

// ❌ linha a linha: agrupe em um bloco lógico
/** @Dado o id */ ...
/** @Dado o customer */ ...
```
