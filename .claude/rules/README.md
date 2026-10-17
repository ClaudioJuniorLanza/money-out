# Regras de código do money-out

Adaptação do harness pessoal do autor (guias Kotlin/Quarkus de arquitetura hexagonal, DDD, persistência e testes) para a stack e as decisões deste projeto: **Spring Boot + MVC + JDBC + virtual threads, Event Sourcing, dinheiro em minor units e a estrutura do ADR-0011**. O harness original é genérico e fica fora deste repositório; o que está aqui é a versão que vale para o money-out.

## Precedência

1. `CLAUDE.md`
2. `docs/PLANO.md` e ADRs em `docs/adr/`
3. Estas regras
4. Skills (ex.: `tlc-spec-driven`)

Quando uma regra pedir uma decisão que não está em nenhum desses lugares, **pare e pergunte** antes de gerar código (combinado do `CLAUDE.md`).

## O que ler para cada tipo de tarefa

| Tarefa | Leia antes de escrever código |
|---|---|
| Qualquer código Kotlin | [`kotlin.md`](kotlin.md) |
| Agregado, value object, evento de domínio, state machine | [`domain-model.md`](domain-model.md) + [`event-sourcing.md`](event-sourcing.md) |
| Command, caso de uso, handler | [`command-handler.md`](command-handler.md) + [`domain-model.md`](domain-model.md) |
| Endpoint HTTP, job agendado, consumer, cliente de API externa, adapter de banco | [`hexagonal.md`](hexagonal.md) |
| Event store, projeção, outbox, upcaster | [`event-sourcing.md`](event-sourcing.md) |
| Testes de qualquer camada | [`tests.md`](tests.md) + [`tests/gherkin.md`](tests/gherkin.md) |

## Diferenças em relação ao harness original (e por quê)

| Tema | Harness original | money-out | Fonte |
|---|---|---|---|
| Framework | Quarkus, CDI | Spring Boot 4.1 | ADR-0001 |
| I/O | reativo (Vert.x/Mutiny) + `suspend` | MVC + JDBC bloqueante + virtual threads; sem `suspend` | ADR-0012 |
| Dinheiro | `BigDecimal` | `Money(minorUnits: Long, currency)` | ADR-0002 |
| Pacotes | `application/{commands,handlers,ports,domain/models}`, `driver/`, `driven/` | `<ctx>/domain/{model,event,port/input,port/output}`, `application/`, `infrastructure/adapter/{input,output}` | ADR-0011 |
| Persistência | estado + outbox (event-persistence ou aggregate-persistence) | **Event Sourcing**: o event store é a verdade; estado é derivado | ADR-0003 |
| Defaults de evento | `UUID.randomUUID()`, `Instant.now()` | proibidos no domínio; tempo vem do `Clock`, ids do `IdGenerator` | ADR-0011 + ArchUnit |
| Invariantes de VO | `require` → `IllegalArgumentException` | `DomainException` (ou subclasse), mapeável para HTTP 422 | `sharedkernel/DomainException.kt` |
| Nomes de teste | inglês, Gherkin em pt-BR | **igual ao harness**: nome em inglês com crases, Gherkin pt-BR obrigatório em todo teste | `tests/gherkin.md` |

O que foi mantido: o domínio rico, a nomenclatura de portas (`Transfers`, não `TransferRepository`), um handler por command, portas mockadas à mão, fixtures centralizadas, comentários Gherkin em pt-BR, `when` exaustivo sem `else`, rotas versionadas no path, a facade de integrações externas e a regra de que nada externo é publicado fora da transação de negócio.
