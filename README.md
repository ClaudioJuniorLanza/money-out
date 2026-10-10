# Money Out

Laboratório de aprendizado em engenharia de pagamentos (payout no Brasil: PIX/TED, event sourcing, idempotência, resiliência, reconciliação).

- Plano completo: [`PLANO.md`](PLANO.md)
- Decisões arquiteturais: [`docs/adr`](docs/adr/README.md)
- Combinados de trabalho: [`CLAUDE.md`](CLAUDE.md)

## Como rodar

Requisitos: JDK 21 e Docker (Docker só a partir da Fase 2).

```bash
./gradlew build          # compila, lint (ktlint), testes e regras de arquitetura (ArchUnit)
./gradlew ktlintFormat   # formata o código
```

Status: **Fase 0 (fundações)** em andamento — veja [`docs/fases/fase0-guia.md`](docs/fases/fase0-guia.md).
