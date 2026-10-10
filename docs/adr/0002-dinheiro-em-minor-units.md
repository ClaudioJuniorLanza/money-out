# 0002 — Dinheiro como inteiro em unidade mínima + moeda

- **Status:** Proposta
- **Data:** 2026-10-10
- **Fase:** 0

## Contexto
Erros de arredondamento em pagamentos geram divergências contábeis difíceis de rastrear. Valores precisam ser exatos, comparáveis e não misturáveis entre moedas.

## Opções consideradas
1. `Double`/`Float` — descartado: não representa decimais exatamente.
2. `BigDecimal` com escala livre — exato, mas permite escalas inconsistentes (`10.0` ≠ `10.00` em `equals`) e fácil esquecer a moeda.
3. **`Long` em unidade mínima (centavos) + `Currency`** — exato, rápido, armazenado como `BIGINT`; escala definida pela moeda.

## Decisão
`Money(minorUnits: Long, currency: Currency)`. Operações entre moedas diferentes lançam erro de domínio. Divisão via `allocate(ratios)`, que distribui o resto sem perder centavos. Apenas BRL aceita por ora.

## Consequências
- Conversões para exibição/serialização ficam explícitas nas bordas.
- Overflow de `Long` precisa ser tratado (`Math.addExact`).
- Cálculos percentuais (taxas) exigem política de arredondamento explícita — será um ADR próprio quando surgirem tarifas.
