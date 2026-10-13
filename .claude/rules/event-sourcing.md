# Regra: Event Sourcing, projeções e publicação

Substitui as capabilities de persistência do harness original (estado + outbox): no money-out **o event store é a fonte da verdade** (ADR-0003). O estado de `Transfer` e `Account` é sempre derivado dos eventos.

## Porta de saída por agregado

```kotlin
// <ctx>/domain/port/output/Transfers.kt — exemplo ilustrativo
interface Transfers {
    /** Rehidrata o agregado; lança TransferNotFound se o stream não existe. */
    fun load(id: TransferId): Transfer

    /** Anexa eventos se a versão atual do stream for [expectedVersion]; senão lança ConcurrencyConflict. */
    fun append(id: TransferId, expectedVersion: Long, events: List<TransferEvent>)
}
```

- Criação de stream: `expectedVersion = 0`.
- `ConcurrencyConflict` é exceção de domínio (no `sharedkernel` quando o segundo agregado precisar dela; critérios do ADR-0011).

## Event store (Fase 2)

Tabela append-only no Postgres. Colunas mínimas:

| Coluna | Para quê |
|---|---|
| `global_position BIGSERIAL` | leitura ordenada pelas projeções (atenção às lacunas e a commits fora de ordem: Fase 4) |
| `stream_id`, `stream_type` | agregado e tipo (`Transfer`, `Account`) |
| `version BIGINT` | posição no stream; `UNIQUE(stream_id, version)` é o controle otimista |
| `event_id UUID` | id do evento (UUIDv7 via `IdGenerator`); dedup nos consumidores |
| `event_type TEXT`, `schema_version INT` | nome estável do evento e versão do formato (upcasting) |
| `payload JSONB` | delta do evento |
| `metadata JSONB` | `correlationId`, `causationId`, `actor`, `traceparent` |
| `occurred_at`, `recorded_at TIMESTAMPTZ` | tempo do fato × tempo da gravação |

- Imutável: nenhum `UPDATE`/`DELETE` (garantido no banco com trigger ou permissões, conforme o critério da fase).
- Um append = um `INSERT` multi-linha numa transação. Violação de `UNIQUE(stream_id, version)` ⇒ `ConcurrencyConflict`.

## Envelope e serialização (infraestrutura)

- O adapter monta o envelope (`event_id`, `version`, metadados) em volta do evento de domínio. O domínio não conhece o envelope.
- **Nome de evento estável e explícito**: um mapa `TransferApproved::class ⇄ "TransferApproved"`, nunca `javaClass.name` (renomear a classe quebraria o histórico).
- Serialização com Jackson só no adapter, com DTOs/mappers explícitos por evento (`when` exaustivo sem `else`). Dinheiro como `{ "minorUnits": 10000, "currency": "BRL" }`.

## Rehidratação, snapshots e upcasting

- `load` lê o stream ordenado por `version`, faz upcast para o formato atual e chama `Transfer.rehydrate(history)`.
- **Upcaster** (`vN → vN+1`) é uma função pura no adapter, testada com JSON antigo real. Adicionar campo com default não muda o `schema_version`; renomear ou remover campo muda.
- Snapshot só quando a medição justificar; é cache descartável, nunca a verdade.

## PII (ADR-0007, Fase 2)

Evento é imutável e a LGPD exige poder eliminar dado pessoal. Não grave CPF/CNPJ/chave PIX em claro no payload: use uma referência para um cofre de PII ou crypto-shredding (decisão do ADR-0007).

## Projeções (Fase 4)

- Consomem o event store por `global_position` com **checkpoint**; são **idempotentes** (reprocessar não duplica) e **reconstruíveis** (rebuild do zero dá o mesmo resultado).
- O query side lê só as tabelas de projeção, nunca o event store.
- A contabilização é uma projeção: cada evento vira lançamentos de dupla entrada balanceados (Σ débitos = Σ créditos), e o estorno é um novo lançamento (ADR-0010).

## Publicação para fora (Fase 5, ADR-0008)

- Proibido publicar no broker dentro do handler ou depois do commit no mesmo fluxo (*dual write*).
- O evento sai do banco por CDC (Debezium) ou por um relay que lê o próprio event store. Se o event store serve como outbox ou se existe uma tabela separada com envelope `{event, aggregate.snapshot}` é **questão aberta do ADR-0008**: não decida sozinho, pergunte.
- `key = streamId`; `sequenceNumber = version` (ordem por transfer).

## Testes obrigatórios do adapter

- append + load devolvem o mesmo agregado;
- append com versão velha ⇒ `ConcurrencyConflict` e nada gravado;
- stream inexistente ⇒ `TransferNotFound`;
- evento v1 gravado em JSON antigo é lido pelo código novo (upcaster);
- tentativa de `UPDATE`/`DELETE` no event store falha (quando a imutabilidade no banco estiver no critério da fase).
