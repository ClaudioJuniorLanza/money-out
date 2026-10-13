# Regra: modelo de domínio (DDD tático)

## Conceitos

- **Agregado:** fronteira de consistência. Toda mudança passa por um método do agregado; invariantes vivem nele, nunca no handler nem no adapter.
- **Entidade:** tem identidade que persiste no tempo (ex.: `Beneficiary` dentro de `Transfer`, se tiver id próprio).
- **Value object:** definido pelos atributos, imutável, valida-se na construção (`Money`, `Document`, `PixKey`, `EndToEndId`, `TransferId`).
- **Evento de domínio:** fato consumado, no passado, na linguagem ubíqua (`TransferRegistered`, `FundsReserved`). Imutável.
- **Domínio rico:** o agregado decide; o handler só orquestra. Sintoma de domínio anêmico: `if (transfer.status == ...)` ou `transfer.copy(status = ...)` dentro de handler.

## Agregado event-sourced: decidir × evoluir

Desde a Fase 1 o agregado é escrito no formato de Event Sourcing (ADR-0003), mesmo com persistência in-memory. Assim, a Fase 2 só troca o adapter, sem reescrever o domínio.

```kotlin
// Exemplo ilustrativo — copie a estrutura, não os nomes
data class Order(
    val id: OrderId,
    val status: OrderStatus,
    val version: Long,
) {
    // DECIDIR: valida a regra e devolve o(s) evento(s). Não muda estado.
    fun cancel(reason: CancelReason, at: Instant): OrderCancelled {
        if (!OrderStateMachine.allows(status, OrderTrigger.CANCEL)) {
            throw InvalidOrderTransition(from = status, trigger = OrderTrigger.CANCEL)
        }
        return OrderCancelled(orderId = id, reason = reason, occurredAt = at)
    }

    // EVOLUIR: aplica um fato já aceito. Puro, sem validação, sem exceção de regra.
    fun apply(event: OrderEvent): Order =
        when (event) {
            is OrderPlaced -> throw CorruptedStream() // OrderPlaced só pode iniciar o stream
            is OrderCancelled -> copy(status = OrderStatus.CANCELLED, version = version + 1)
        }

    companion object {
        fun place(id: OrderId, at: Instant): OrderPlaced = OrderPlaced(orderId = id, occurredAt = at)

        fun rehydrate(history: List<OrderEvent>): Order {
            val first = history.first() as? OrderPlaced ?: throw CorruptedStream()
            return history.drop(1).fold(Order(first.orderId, OrderStatus.PLACED, version = 1)) { o, e -> o.apply(e) }
        }
    }
}
```

Regras:

- Métodos de decisão recebem tempo (`at: Instant`) e ids como parâmetro; nunca leem relógio global.
- `apply` é total sobre a `sealed interface` (`when` sem `else`) e não valida: o evento é um fato que já aconteceu.
- `version` = número de eventos aplicados; é o `expectedVersion` do append (ver `event-sourcing.md`).
- Transição inválida lança uma **exceção de domínio específica** (subclasse de `DomainException`), nunca `IllegalStateException`.

## State machine (Fase 1, ADR-0005)

- Declarativa e custom: uma tabela `(estado, gatilho) → estado destino` + guards. É a única fonte de verdade das transições; os métodos do agregado a consultam.
- Estados terminais são absorventes (nenhum gatilho os tira de lá, exceto `COMPLETED → RETURNED`, que é regra explícita).
- Cada transição válida produz **exatamente um** evento.
- O teste exaustivo percorre todos os pares `(estado × gatilho)` a partir da tabela.

## Value objects

```kotlin
// Exemplo ilustrativo
@JvmInline
value class TransferId(val value: UUID)

data class PixKey private constructor(val type: PixKeyType, val value: String) {
    companion object {
        fun of(type: PixKeyType, raw: String): PixKey {
            val normalized = raw.trim()
            if (!type.accepts(normalized)) throw InvalidPixKey(type) // sem PII na mensagem
            return PixKey(type, normalized)
        }
    }
}
```

- `data class` (ou `@JvmInline value class` para ids) com `val`.
- Valide na construção: `init {}` ou factory com construtor privado quando há normalização.
- Lance `DomainException` (ou subclasse), não `require`/`IllegalArgumentException`, para o adapter HTTP mapear para `422`.
- Mensagens de exceção **sem PII**: diga "CPF com dígito verificador inválido", nunca o CPF.
- Regras que dependem de fonte externa (BCB, Receita) citam a fonte num KDoc e no `docs/experiments/faseN.md`.

## Eventos de domínio

```kotlin
// Exemplo ilustrativo
sealed interface OrderEvent {
    val orderId: OrderId
    val occurredAt: Instant
}

data class OrderCancelled(
    override val orderId: OrderId,
    val reason: CancelReason,
    override val occurredAt: Instant,
) : OrderEvent
```

- Carregam só o **delta** do fato (o que mudou), não o agregado inteiro: num event store o estado é derivado, e evento com snapshot vira contrato frágil.
- Sem `eventId`, `version`, `correlationId` nem `causationId` no evento de domínio: esses metadados ficam no envelope montado pela infraestrutura (ver `event-sourcing.md`).
- Sem defaults de `Instant.now()` ou `UUID.randomUUID()`.
- PII nunca em claro (ADR-0007, Fase 2): o evento guarda referência/token, não o CPF.

## Nomenclatura

| Elemento | Padrão | Exemplo |
|---|---|---|
| Agregado | substantivo | `Transfer`, `Account` |
| Id | Agregado + `Id` | `TransferId` |
| Evento | Agregado + particípio | `TransferApproved`, `FundsReserved` |
| Base de eventos | Agregado + `Event` | `TransferEvent` |
| Gatilho da state machine | imperativo | `APPROVE`, `SUBMIT` |
| Exceção de domínio | a violação | `InvalidTransferTransition`, `InsufficientFunds` |

## Imutabilidade

Agregados, entidades, VOs e eventos são imutáveis; “mudança” é `copy()` dentro de `apply`. Nenhum `var` no domínio.
