# Money Out

Laboratório de aprendizado em engenharia de pagamentos (payout no Brasil: PIX/TED, event sourcing, idempotência, resiliência, reconciliação).

- Plano (roteiro): [`docs/PLANO.md`](docs/PLANO.md)
- Tasks por fase: [`.specs/features/`](.specs/features/) · fluxo local × nuvem: [`docs/fluxo-de-trabalho.md`](docs/fluxo-de-trabalho.md)
- Regras de código para agentes: [`.claude/rules/`](.claude/rules/README.md)
- Decisões arquiteturais: [`docs/adr`](docs/adr/README.md)
- Combinados de trabalho: [`CLAUDE.md`](CLAUDE.md)

## Como rodar

Requisitos: JDK 25 e Docker (Docker só a partir da Fase 2).

```bash
./gradlew build          # compila, lint (ktlint), testes e regras de arquitetura (ArchUnit)
./gradlew ktlintFormat   # formata o código
```

Status: **Fase 0 (fundações)** em andamento — guia em [`docs/fases/fase0-guia.md`](docs/fases/fase0-guia.md), tasks em [`.specs/features/fase0-fundacoes/tasks.md`](.specs/features/fase0-fundacoes/tasks.md).
