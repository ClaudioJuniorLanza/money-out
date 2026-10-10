# Money Out — combinados de trabalho

Projeto de aprendizado em engenharia de pagamentos (payout, Brasil). O plano está em `PLANO.md`.

- Idioma: português do Brasil para documentação, ADRs e conversa; código e identificadores em inglês.
- Modo colaborativo: tarefas marcadas 🧑 no plano são do autor. Não as implemente sem pedido explícito; ofereça testes, dicas e revisão.
- Uma fase por vez, na ordem do plano. Diante de dúvida ou decisão de design não coberta, pare e pergunte.
- Ao iniciar uma fase, gerar `docs/fases/faseN-guia.md` com os blocos 📋 (código para o autor digitar).
- Decisões arquiteturais viram ADR em `docs/adr/`. Exercícios de quebra e evidências ficam em `docs/experiments/faseN.md`.
- Verifique antes de afirmar: versões, normas do BCB e propriedades de bibliotecas marcadas "a confirmar" devem ser checadas na fonte.
- Ao fechar uma fase: critérios de aceitação verdes, ADRs atualizados e entrevista registrada em `docs/entrevistas/faseN.md`.
- Entrevista: 5 a 8 perguntas, uma por vez, sobre conceitos e trade-offs (nunca sintaxe). O agente nunca escreve as respostas pelo autor e só dá feedback depois da tentativa (certo / faltou / o que um entrevistador sênior esperaria). Perguntas fracas voltam nas fases seguintes.
- Dinheiro nunca em `float`/`double`. Domínio sem dependência de framework (ArchUnit). Nunca usar tags `latest` em imagens Docker.
- Não incluir informações de empregadores do autor no repositório (ele é público). O `platform-eda` é privado: referenciar por nome e contrato, nunca copiar seu código ou conteúdo.
- Git: push e qualquer reescrita de histórico só com pedido explícito.
