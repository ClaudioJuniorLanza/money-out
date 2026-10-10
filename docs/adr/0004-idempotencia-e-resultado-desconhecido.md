# 0004 — Idempotência em todas as bordas e tratamento de resultado desconhecido

- **Status:** Proposta
- **Data:** 2026-10-10
- **Fase:** 3 e 6

## Contexto
Redes falham depois de o efeito ocorrer. Clientes reenviam requisições, brokers reentregam mensagens e provedores confirmam em duplicidade. Tratar timeout como falha leva a pagamento duplicado ou a estorno indevido.

## Decisão
1. **API:** header `Idempotency-Key` obrigatório em operações que movem dinheiro; registro gravado na mesma transação do efeito; mesma chave + mesmo hash ⇒ replay; hash diferente ⇒ 422; em andamento ⇒ 409.
2. **Mensageria:** consumidores idempotentes via inbox (insert-first + efeito na mesma transação).
3. **TSP:** identificador (`endToEndId`) gerado e persistido **antes** da chamada e usado como chave de idempotência no provedor.
4. **Timeout/resposta ambígua ⇒ estado `UNCERTAIN`**, nunca falha: resolve-se consultando o provedor. Compensação (liberar saldo) só ocorre após falha *definitiva* confirmada.

## Consequências
- Mais estados e jobs de consulta; em troca, elimina a classe mais cara de bugs (pagamento duplicado/perdido).
- `UNCERTAIN` precisa de SLA e alerta para não virar dinheiro preso (Fase 8).
