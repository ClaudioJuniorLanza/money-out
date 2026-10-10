# 0003 — Event Sourcing como fonte da verdade; contabilização como projeção pós-transacional

- **Status:** Aceita
- **Data:** 2026-10-10
- **Fase:** 2 e 4

## Contexto
O plano original aplicava Event Sourcing (ES) ao domínio e representava saldo como campo mutável em `DepositAccount`, sem tratar contabilidade. Uma versão anterior deste ADR propunha um ledger de dupla entrada como fonte da verdade do dinheiro e ES apenas em `Transfer`. O autor tem experiência com um modelo em que os movimentos contábeis ("ledger") são escritos **após** a transação de negócio, e decidiu manter ES como pilar do projeto.

## Opções consideradas
1. **ES em tudo; contabilização como projeção pós-transacional** — eventos são a verdade; saldo da conta é decidido no aggregate `Account` (concorrência otimista); lançamentos de dupla entrada são derivados dos eventos. Alinha-se à experiência do autor e exercita ES/CQRS de ponta a ponta.
2. **Ledger de dupla entrada como verdade do dinheiro + ES só em `Transfer`** — garantias contábeis fortes no momento da escrita; menos ES no projeto.
3. **CRUD + tabela de auditoria** — simples, mas não exercita ES/CQRS.

## Decisão
Opção 1.
- `Transfer` e `Account` são aggregates event-sourced; o invariante "saldo disponível ≥ 0" vive em `Account` e é protegido por `UNIQUE(aggregate_id, version)` + retry.
- A contabilização (Fase 4) consome os eventos e grava lançamentos imutáveis e balanceados; é eventualmente consistente.
- Reconciliação interna compara o saldo de `Account` com o saldo contábil.

## Consequências
- Risco aceito: a contabilidade pode ficar momentaneamente atrás dos eventos; por isso a reconciliação interna é obrigatória.
- Custos de ES assumidos: versionamento de eventos, projeções com rebuild, PII em eventos imutáveis (ADR-0007).
- Operações que envolvem dois aggregates (`Transfer` e `Account`) não são atômicas por natureza; na Fase 3 usamos uma transação de banco como atalho consciente e, na Fase 7, um process manager.
