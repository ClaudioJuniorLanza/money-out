# Entrevista — Fase 0 (Fundações)

Data: 2026-10-17 · Entrevistado: autor · Entrevistador: agente de IA (Claude)

Formato: 6 perguntas, uma por vez. As respostas abaixo são do autor, transcritas como foram dadas (sem edição). O feedback do agente veio só depois de cada resposta. Classificação: ✅ sólida · 🟡 parcial (volta na Fase 1 ou na fase indicada).

Contexto do fechamento: a revisão dos ADRs 0001, 0002, 0004, 0011 e 0012 (T5) foi feita pelo autor no PR #4, confirmado por ele na sessão; o commit de aceite tem autor "Claude" porque a parte mecânica foi aplicada pelo agente a pedido do autor.

---

## 1. `double` × `BigDecimal` × `Long` em centavos — 🟡

**Pergunta.** Compare as três opções para guardar dinheiro e diga o que responderia a um colega que propõe `double`.

**Resposta.** "Double provavelmente nos faria perder precisão nas casas decimais. A forma de armazenamento desse ponto flutuante poderia causar arredondamentos indesejáveis devido ao seu modo de armazenamento na memória. Já Bigdecimal garante a precisão dos valores. Como o bigdecimal trata os decimais como "string", já não ocorre o mesmo problema do double. Na sua tratativa, o bigdecimal trata numero a número, podendo ser mais lento o cálculo mas garantindo a precisão. O Long tratando em centavos, armazenaria os valores como inteiros de centavos, por exemplo, o que também não perderíamos a precisão. O ponto flutuante pode ser definido após o cálculo, conservando a fração correta."

**Feedback.** Certo: `double` perde precisão pelo armazenamento; `BigDecimal` é preciso e mais lento; `Long` em centavos é inteiro e a formatação fica na borda. Faltou: a causa do `double` (representação binária, `0.1 + 0.2 != 0.3`); `BigDecimal` não é `String`, é inteiro de precisão arbitrária com escala, e o `equals` considera a escala (`10.0` ≠ `10.00`), a divisão exige `RoundingMode` e a moeda não vem junto; o `Long` tem overflow (`Math.addExact`), escala que depende da moeda e política explícita para divisão. Um sênior fecharia com a decisão defendida (ADR-0002).

**Voltar na Fase 1:** interno e `equals` por escala do `BigDecimal`; custos do `Long`.

## 2. Dividir R$ 100,00 em 3 sem perder centavo — 🟡

**Pergunta.** Como dividir, e qual propriedade do property test garante que nunca se perde ou cria centavo?

**Resposta.** "Pensaria numa solução de rateio. 100/3 aconteceria uma dízima periódica, então Dividiria o total pelo número de partes e distribuiria o resto."

**Feedback.** Certo: rateio e distribuição do resto. Faltou: trabalhar em inteiros (10.000 centavos, piso 3.333, resto 1 para a primeira parte: 33,34 + 33,33 + 33,33); a fórmula geral por ratio; a política determinística de quem recebe o resto; bordas (negativo, overflow de `total × ratio`); e a **segunda metade da pergunta ficou sem resposta**: a propriedade é a conservação (`soma das partes == total`), com tamanho, proximidade < 1 e ratios iguais diferindo em no máximo 1. No exercício de quebra, a proximidade sozinha não pegou o `allocate` sem resto; a soma pegou.

**Voltar na Fase 1:** propriedades do `allocate` e por que a conservação é a que protege o centavo.

## 3. Por que injetar o `Clock` — 🟡

**Pergunta.** Por que o `Clock` é injetado e o que quebraria nos testes e na rehidratação se o domínio lesse o relógio global?

**Resposta.** "Eventos e timestemps são gravados em UTC. Tempo vêm do container, nunca chamados globais.Sem a injeção do relógio no handler, não seria possível asserções de tempo nos testes, uma vez que now() não teria como ser verificado."

**Feedback.** Certo: sem injeção não há asserção de tempo previsível; o tempo vem do container. Faltou separar a zona (UTC, detalhe de representação) do motivo (determinismo); na rehidratação, o `apply` precisa ser puro e o `occurredAt` é um fato gravado no evento, senão o replay daria estado diferente do original; e regras temporais (SLA do `UNCERTAIN`, TTL de idempotência) testáveis com `Clock.fixed`, sem `sleep`.

**Voltar na Fase 1:** replay e `rehydrate` determinísticos.

## 4. UUID v4 × v7 como chave primária — 🟡

**Pergunta.** Diferença entre v4 e v7, efeito no índice e limitação conhecida do v7.

**Resposta.** "uuid v7 cresce com o tempo, inserts caem no final do índice, e no v4 espalha os inserts. O limite conhecido do v7 é que a ordem não é garantida dentro do mesmo milissegundo. No nosso caso, o clock é injetado, não sendo um now()"

**Feedback.** Certo: v7 cresce com o tempo e insere no fim do B-tree; o v4 espalha; a ordem dentro do mesmo milissegundo não é garantida. Faltou a estrutura (48 bits de timestamp + aleatório), o mecanismo do ganho (localidade, menos *page splits* e menos WAL) e os custos do v7 (vaza o instante de criação, contenção na folha da direita, relógios divergentes entre nós). A última frase não resolve a limitação: o relógio injetado ajuda nos testes, mas com `Clock.fixed` todos os ids têm o mesmo timestamp e só os bits aleatórios os distinguem. No event store, a ordem confiável vem de `version` e `global_position`, não do UUID.

**Voltar na Fase 2:** custos do v7 e papel do relógio injetado, quando o event store entrar.

## 5. Para que serve um ADR — ✅

**Pergunta.** Para que serve um ADR, exemplo da Fase 0 e o que acontece quando a decisão muda.

**Resposta.** "ADR nesse projeto servem para registrar as deciões de arquitetura tomadas no projeto. Nas ADR's registramos o contexto, a decisão e suas motivações, opções que foram consideradas e as consequências. É um histórico fundamentado das decisões tomadas. No caso da fase 0, a decisão de usar long ao inves de double por exemplo explicita a um colega a motivação da decisão, evitando um possível problema em alguma etapa do sistema que lida com valores monetários. Quando uma decisão muda no futuro, a ADR deve ser atualizada com seu status sendo Substituída por ABCD, ou seja, uma nova ADR é criada, contudo a ADR que registrava a decisão antiga é atualizada seu status"

**Feedback.** Resposta sólida. Refinamentos: o corpo do ADR antigo não é reescrito (só status e ponteiro mudam); o exemplo ficaria mais forte nomeando a ação que o ADR evita (trocar `Long` por `BigDecimal` "por precisão"); e decisões do ADR-0011 viram regras da ArchUnit, então o build as defende.

## 6. Prova de falha das regras e o achado dos property tests — 🟡

**Pergunta.** Por que toda regra nova da ArchUnit exige prova de que falha, e qual a lição do achado em que os property tests de `Money` nunca rodavam com o build verde?

**Resposta.** "É necessário para garantir a integridade entre as fronteiras da arquitetura. Se a regra falhar entre a violação entre camadas, o build falha. No caso do achado do exercício de quebra do Money mas nunca rodavam evidencia que mesmo o build estando verde, não é garantia de que os cenários estão sendo validados. Checks e linters são necessários para suprir esses casos de quebra quem passam pelos casos de builders verdes"

**Feedback.** Certo: a regra protege as fronteiras, e build verde não prova que os cenários são validados. Faltou o motivo central da prova de falha: uma regra que nunca falhou é indistinguível de uma regra vazia ou mal configurada. O projeto tem `archRule.failOnEmptyShould=false`, então uma regra com padrão de pacote errado passa em silêncio, e no F0-04 uma primeira tentativa não falhou porque o `import` sem uso não gera dependência no bytecode. Sobre o achado: nem o linter nem o compilador pegaram os testes ignorados (`= runBlocking { checkAll }` devolve `PropertyContext` e o JUnit só executa método `void`). Quem pegou foi a **mutação deliberada** (o `allocate` sem resto não derrubava nenhuma propriedade) e a **contagem de testes** (31 em vez de 37). A lição: "um teste que nunca falha pode ser um teste que nunca roda", e a defesa é quebrar de propósito e conferir que a contagem só cresce.

**Voltar na Fase 1:** `failOnEmptyShould=false` e o papel da contagem de testes; ao criar regra nova, mostrar a prova de falha.

---

## Resumo

| Tema | Avaliação | Volta em |
|---|---|---|
| `double` × `BigDecimal` × `Long` | 🟡 | Fase 1 |
| `allocate` e conservação | 🟡 | Fase 1 |
| `Clock` e replay | 🟡 | Fase 1 |
| UUID v4 × v7 | 🟡 | Fase 2 |
| ADR | ✅ | — |
| Prova de falha / testes que não rodam | 🟡 | Fase 1 |
