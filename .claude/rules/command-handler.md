# Regra: commands e handlers (casos de uso de escrita)

## Command

- Intenção de mudança, no imperativo: verbo + agregado (`RegisterTransfer`, `ApproveTransfer`, `ReserveFunds`).
- `data class` imutável, sem lógica, já com tipos de domínio (VOs), não com `String` crua.
- Mora em `<ctx>/domain/port/input/` junto da interface do caso de uso.
- Opcional: `sealed interface TransferCommand` para impedir que um handler aceite command de outro contexto.

## Porta de entrada

```kotlin
// <ctx>/domain/port/input/ApproveTransferUseCase.kt — exemplo ilustrativo
fun interface ApproveTransferUseCase {
    fun handle(command: ApproveTransfer): TransferId
}
```

Sem `suspend` (ADR-0012). Um caso de uso por command.

## Handler

Fica em `<ctx>/application/`, chama-se `<Command>Handler` e implementa a porta de entrada. É Kotlin puro: as dependências (portas de saída, `Clock`, `IdGenerator`) chegam pelo construtor e o wiring é feito em `infrastructure/config`.

Fluxo padrão com Event Sourcing:

```kotlin
// Exemplo ilustrativo
class ApproveTransferHandler(
    private val transfers: Transfers,   // porta de saída (interface)
    private val clock: Clock,
) : ApproveTransferUseCase {
    override fun handle(command: ApproveTransfer): TransferId {
        val transfer = transfers.load(command.transferId)           // 1. rehidrata
        val event = transfer.approve(at = clock.instant())         // 2. agregado decide
        transfers.append(transfer.id, transfer.version, listOf(event)) // 3. append otimista
        return transfer.id
    }
}
```

1. **Carregar** o agregado pela porta de saída (rehidratação). Inexistente ⇒ exceção de domínio (`TransferNotFound`) mapeada para `404`.
2. **Delegar** a decisão ao agregado.
3. **Anexar** os eventos com `expectedVersion = versão carregada`. Conflito ⇒ `ConcurrencyConflict`.
4. **Retry de conflito** (Fase 2, ADR-0006): reler e reaplicar o command, com limite pequeno e explícito (constante nomeada). Só é seguro porque o command é idempotente.

## Regras

- **Sem regra de negócio no handler.** Nada de `if (transfer.status == ...)`, cálculo de saldo ou `copy(...)`: isso é do agregado.
- Handler depende de **interfaces**, nunca de adapters concretos.
- Um command, um handler; nada de handler genérico com `when (command)`.
- Tempo: `clock.instant()` no handler, passado ao agregado. Ids: `IdGenerator`.
- Operações que movem dinheiro são idempotentes (ADR-0004): a decisão de idempotência (Fase 3) é uma função pura testada à parte, e o registro da chave é gravado na **mesma transação** do efeito.
- Dois agregados no mesmo caso de uso (`Transfer` + `Account` na Fase 3) só pelo atalho consciente do ADR-0006, com porta `Transactions`; a Fase 7 troca isso por um process manager.

## Handler com chamada externa (Fase 6)

Chamar o TSP e depois gravar é *dual write*. A ordem correta é:

1. gravar a intenção (`TransferSubmitted` com `endToEndId`) e commitar;
2. chamar o TSP com `endToEndId` como chave de idempotência;
3. registrar o resultado como novo evento (`TransferCompleted`, `TransferFailed` ou `TransferUncertain`).

Timeout ou resposta ambígua ⇒ `UNCERTAIN`, nunca `FAILED`. Só falha **definitiva** libera saldo.

## Versionamento

Rotas novas nascem em `/v1/`. Uma `v2` convive temporariamente com a `v1`; a conversão entre versões fica no adapter HTTP/`application`, e o domínio só conhece o modelo atual.
