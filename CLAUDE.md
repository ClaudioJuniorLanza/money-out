# Money Out — combinados de trabalho

Projeto de aprendizado em engenharia de pagamentos (payout, Brasil). O roteiro está em `docs/PLANO.md`; as tasks executáveis, em `.specs/features/`.

- Idioma: português do Brasil para documentação, ADRs e conversa; código e identificadores em inglês.
- Modo colaborativo: toda task tem **Owner** — 🧑 autor (local), ☁️ agente na nuvem, 🤖 agente local. Nunca implemente uma task 🧑 sem pedido explícito; ofereça testes, dicas e revisão. Protocolo completo (branch, PR, desbloqueio): `docs/fluxo-de-trabalho.md`.
- Uma fase por vez, na ordem do plano. Diante de dúvida ou decisão de design não coberta, pare e pergunte.
- Planejamento com a skill `tlc-spec-driven` (em `.claude/skills/`): ao abrir uma fase, criar `.specs/features/faseN-*/` (spec → design, se houver decisão de arquitetura → tasks), rodar os validadores e obter aprovação do autor; gerar também `docs/fases/faseN-guia.md` com os blocos 📋 (código para o autor digitar).
- Decisões arquiteturais viram ADR em `docs/adr/`; decisões de processo, AD-NNN em `.specs/STATE.md`. Exercícios de quebra e evidências ficam em `docs/experiments/faseN.md`.
- Verifique antes de afirmar: versões, normas do BCB e propriedades de bibliotecas marcadas "a confirmar" devem ser checadas na fonte.
- Ao fechar uma fase: critérios de aceitação verdes, `validation.md` com PASS, ADRs atualizados e entrevista registrada em `docs/entrevistas/faseN.md`.
- Entrevista: 5 a 8 perguntas, uma por vez, sobre conceitos e trade-offs (nunca sintaxe). O agente nunca escreve as respostas pelo autor e só dá feedback depois da tentativa (certo / faltou / o que um entrevistador sênior esperaria). Perguntas fracas voltam nas fases seguintes.
- Arquitetura: DDD + hexagonal por bounded context (ADR-0011): `domain` (modelo + portas input/output), `application`, `infrastructure` (adapters input/output). `sharedkernel` é mínimo e explícito; nunca criar `shared`/`common`/`utils` genéricos.
- Regras de código: **antes de gerar código, leia `.claude/rules/README.md` e as regras que ele indica para o tipo de tarefa** (Kotlin, domínio, handlers, hexagonal, event sourcing, testes). Precedência: `CLAUDE.md` > `docs/PLANO.md`/ADRs > `.claude/rules/` > skills. As regras já são a versão Spring + Event Sourcing do harness do autor; o harness original (Quarkus) não se aplica.
- I/O: Spring MVC + JDBC + virtual threads, sem `suspend` (ADR-0012).
- Dinheiro nunca em `float`/`double`. Domínio sem dependência de framework (ArchUnit). Nunca usar tags `latest` em imagens Docker.
- Gate de toda task: `./gradlew ktlintFormat && ./gradlew build`; um commit por task (Conventional Commits, pt-BR), com a task marcada no `tasks.md` no mesmo commit.
- Não incluir informações de empregadores do autor no repositório (ele é público). O `platform-eda` é privado: referenciar por nome e contrato, nunca copiar seu código ou conteúdo. O arquivo local `mini-payout-system.md` (plano v1) nunca é commitado.
- Git: push e qualquer reescrita de histórico só com pedido explícito. Exceção: um agente na nuvem que recebeu uma task ☁️ pode fazer push da **própria branch** `claude/...` e abrir PR para o `main` (nunca push no `main`).
