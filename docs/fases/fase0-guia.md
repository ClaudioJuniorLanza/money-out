# Fase 0 — Guia da sua trilha 🧑

Tempo estimado: ≈ 30–45 min. Nesta fase você **não implementa código**: `Money`, `allocate`, os beans de `Clock`/`IdGenerator`, a regra contra `UUID.randomUUID()` e os exercícios de quebra são tasks 🤖 (`.specs/features/fase0-fundacoes/tasks.md`). Implementar os VOs do `sharedkernel` não traz ganho relevante de conhecimento; o que importa aqui são as **decisões** e saber defendê-las na entrevista.

Sua trilha:

1. revisar os ADRs (T5);
2. ler os PRs do agente como material de estudo (opcional, ≈ 10 min);
3. fazer a entrevista de fechamento (T7, com o agente).

---

## 1. Revise os ADRs (T5)

Para cada ADR, responda às perguntas abaixo **com as suas palavras, direto no texto do ADR** (Contexto, Opções, Consequências). Se discordar, altere a decisão ou rejeite o ADR. Quando estiver de acordo, mude o status de **Proposta** para **Aceita** e atualize `docs/adr/README.md`.

| ADR | Perguntas para desafiar a decisão |
|---|---|
| [0001 — Stack e versões](../adr/0001-stack-e-versoes.md) | Por que seguir o BOM do Spring Boot em vez da versão mais nova de cada lib? O que você perde ao não usar Kotlin 2.4? |
| [0002 — Minor units](../adr/0002-dinheiro-em-minor-units.md) | `Long` × `BigDecimal`: o que o `equals` de `BigDecimal("10.0")` vs `BigDecimal("10.00")` faria num `data class`? Por que `Money` é assinado? Onde a regra "valor > 0" deveria morar? |
| [0004 — Idempotência e resultado desconhecido](../adr/0004-idempotencia-e-resultado-desconhecido.md) | Por que timeout não é falha? O que acontece com o dinheiro se você compensar um `UNCERTAIN`? |
| [0011 — Hexagonal/DDD e Shared Kernel](../adr/0011-estrutura-hexagonal-ddd-e-shared-kernel.md) | Os quatro critérios de entrada no `sharedkernel` bastam? O que você faria quando `account` precisar de `ConcurrencyConflict`? |
| [0012 — MVC + JDBC + virtual threads](../adr/0012-modelo-de-io-mvc-jdbc-virtual-threads.md) | Por que transação JDBC com coroutines é armadilha? Quando reativo valeria a pena num sistema de payout? |

## 2. Leia os PRs do agente (opcional)

Ao revisar os PRs de `Money` e `allocate`, procure as respostas no código e nos testes:

- Por que `Math.addExact` em vez de `+`? O que `-Money(Long.MIN_VALUE)` faria sem ele?
- Por que `compareTo` lança erro para moedas diferentes em vez de comparar os números?
- Como `allocate` divide R$ 100,00 em 3 sem perder centavo, e qual propriedade do property test pegaria um `allocate` que ignora o resto?
- Nos exercícios de quebra (`docs/experiments/fase-0.md`), qual regra da ArchUnit falhou em cada um e por quê?

## 3. Entrevista (T7)

Quando os ADRs estiverem revisados e as tasks 🤖 integradas, peça ao agente a entrevista da Fase 0. Temas: `float` × `BigDecimal` × minor units; dividir R$ 100 em 3; por que injetar `Clock`; UUID v4 × v7 como PK no Postgres; para que serve um ADR. As perguntas vêm uma por vez, e o feedback só depois da sua resposta.

Critérios da fase (do [`PLANO.md`](../PLANO.md)): build verde local e no CI; ArchUnit falha se o domínio importar Spring; `Money` não soma moedas distintas; `allocate` nunca perde/ganha centavo (property test); nenhum `now()` direto no domínio.
