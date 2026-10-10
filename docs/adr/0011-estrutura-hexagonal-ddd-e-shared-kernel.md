# 0011 — Estrutura hexagonal/DDD por bounded context e Shared Kernel explícito

- **Status:** Proposta
- **Data:** 2026-10-10
- **Fase:** 0

## Contexto
O plano v2 descrevia padrões (DDD, hexagonal) sem fixar a estrutura de pacotes, e o scaffolding inicial colocou `Currency`, `DomainException` e o gerador de UUID em `shared/domain`. Um pacote `shared` genérico vira depósito de código sem dono e esconde acoplamento; além disso, um gerador de UUID é detalhe técnico, não domínio, e `Money` tem invariantes (é domínio).

## Opções consideradas
1. **`shared` / `common` / `utils` genérico** — simples, mas sem critério de entrada e propenso a virar lixeira.
2. **Sem código compartilhado** — cada contexto com seu próprio `Money`; evita acoplamento, mas duplica invariantes críticas (dinheiro) e abre risco de divergência.
3. **Shared Kernel explícito e mínimo** — pacote `sharedkernel` com o subconjunto do modelo que os contextos concordam em compartilhar (Evans), com critérios de entrada, ADR e regras ArchUnit.

## Decisão
Estrutura de pacotes, por bounded context (`transfer`, `account`, `accounting`, ...):

```
com.moneyout
├── sharedkernel/                  # Money, Currency, DomainException, portas mínimas (IdGenerator)
├── infrastructure/                # adaptadores técnicos transversais (ex.: id/UuidV7Generator)
└── <contexto>/
    ├── domain/                    # model (agregado, VOs, entidades), event, port/input, port/output
    ├── application/               # use cases/handlers que implementam as portas de entrada
    └── infrastructure/
        ├── adapter/input/         # HTTP, cron, consumers (portas primárias)
        ├── adapter/output/        # banco, APIs externas, broker (portas secundárias)
        └── config/
```

- **Portas** são definidas no domínio: `port/input` (casos de uso) e `port/output` (repositórios, relógio, TSP...). As implementações ficam na infraestrutura.
- **Nomes** (herdados do harness do autor): o command (`RegisterTransfer`) mora em `port/input` junto da interface do caso de uso; o handler (`RegisterTransferHandler`) mora em `application`; portas de saída têm nome de domínio no plural (`Transfers`, não `TransferRepository`). Detalhes em `.claude/rules/hexagonal.md`.
- **Dependências:** `infrastructure → application → domain`; o domínio e o `sharedkernel` não conhecem camadas externas nem frameworks (ArchUnit).
- **Critérios para entrar no `sharedkernel`:** (1) mais de um contexto precisa do conceito; (2) tem invariantes de domínio ou é uma porta mínima; (3) mudanças são coordenadas entre contextos; (4) nunca depende de nenhum contexto. Utilitários genéricos **não** entram.
- `Money` e `Currency` nascem no `sharedkernel` porque `transfer`, `account` e `accounting` vão usá-los.
- `IdGenerator` é uma porta no `sharedkernel`; `UuidV7Generator` é um adaptador em `com.moneyout.infrastructure.id`.

## Consequências
- Fronteiras verificáveis no build; o aprendizado de hexagonal/DDD fica concreto.
- O `sharedkernel` exige disciplina: cada inclusão nova deve ser justificada contra os critérios acima.
- Contextos diferentes podem divergir em linguagem ubíqua; integração entre eles é por eventos/contratos, não por classes compartilhadas além do kernel.
