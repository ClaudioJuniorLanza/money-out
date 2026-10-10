# Fluxo de trabalho: local × nuvem

Como uma fase sai do plano e vira código, quem faz cada parte e onde. Vale para o autor, para o agente local e para agentes na nuvem.

## Mapa dos artefatos

| Artefato | Papel |
|---|---|
| [`docs/PLANO.md`](PLANO.md) | Roteiro e didática: objetivos, histórias de falha, conceitos, entrevistas. **Não** é lista de tarefas. |
| `.specs/features/faseN-*/spec.md` | O **quê**: requisitos rastreáveis (IDs, critérios EARS, premissas). |
| `.specs/features/faseN-*/design.md` | O **como** (só em fases com decisões de arquitetura). |
| `.specs/features/faseN-*/tasks.md` | Tasks atômicas com **Owner**, dependências, testes e gate. É de onde se escolhe o que fazer. |
| `.specs/STATE.md` | Decisões de processo (AD-NNN) e o handoff entre sessões. |
| `docs/adr/` | Decisões de arquitetura (ADR-NNNN). |
| `.claude/rules/` | Regras de código que todo agente lê antes de gerar código. |
| `.claude/skills/tlc-spec-driven/` | Skill de spec-driven development (vendorizada; licença CC-BY-4.0, autor no frontmatter). |
| `docs/fases/faseN-guia.md` | 📋 Guia da trilha do autor (código para digitar, casos de teste, perguntas). |
| `docs/experiments/faseN.md`, `docs/entrevistas/faseN.md` | Evidências e registro da entrevista. |

Os títulos de seção das specs/tasks ficam em inglês ("Problem Statement", "Task Breakdown", ...) e os critérios usam as palavras-chave EARS (`WHEN`, `IF`, `THEN`, `SHALL`) porque os validadores da skill dependem delas; todo o resto é pt-BR.

## Ciclo de uma fase

1. **Abrir (🤖 local, com o autor):** spec → (design) → tasks com a skill `tlc-spec-driven`. Rodar os validadores e o autor aprovar (o `Status` vira `Approved`). Gerar o guia `docs/fases/faseN-guia.md`.
2. **Executar em paralelo:**
   - **🧑 autor, local:** faz as tasks 🧑 na ordem; o agente local revisa quando pedido.
   - **☁️ nuvem:** o autor delega tasks ☁️ desbloqueadas (protocolo abaixo).
3. **Integrar:** cada task vira um PR para o `main`; o autor revisa e faz o merge.
4. **Fechar (🤖 local, com o autor):** Verifier independente (`validation.md`), 💥 exercícios de quebra, 🎤 entrevista, ADRs atualizados. Só então a próxima fase abre (regra de ouro do plano).

## Quando uma task está desbloqueada

Todas as tasks do `Depends on` estão marcadas como concluídas no `tasks.md` **do `main`**, e o pré-requisito da fase (se houver) está cumprido. Task 🧑 pendente bloqueia as dependentes, e a nuvem não contorna isso.

## Protocolo do agente na nuvem (☁️)

Prompt sugerido para delegar:

> Execute a task **T<N>** de `.specs/features/<faseN-...>/tasks.md` seguindo `docs/fluxo-de-trabalho.md`.

O agente deve:

1. Partir do `main` atualizado e criar a branch `claude/<faseN>-t<N>-<slug>`.
2. Ler `CLAUDE.md`, `.specs/STATE.md`, a spec, o design (se houver), a task e as regras de `.claude/rules/` indicadas em **Tools**.
3. Conferir se a task é ☁️ e está desbloqueada. Se não estiver, **parar** e responder o motivo, sem implementar nada.
4. Ativar a skill `tlc-spec-driven` e seguir o Execute: testes derivados dos ACs, gate `./gradlew ktlintFormat && ./gradlew build` verde, contagem de testes que só cresce.
5. Marcar a task como concluída no `tasks.md` (e a rastreabilidade na spec) **no mesmo commit** da task. Um commit por task, Conventional Commits em pt-BR.
6. Fazer push **só da própria branch** e abrir PR para o `main` com: task, ACs cobertos, saída resumida do gate e desvios. Nunca push no `main`, force-push ou reescrita de histórico. O prompt de delegação autoriza só esse push e o PR.
7. Diante de decisão de design não coberta pela spec, design, ADRs ou regras: **parar e perguntar no PR**, sem decidir sozinho.

Batches: o autor pode delegar várias tasks ☁️ consecutivas de uma mesma fase num único prompt ("T4, T7, T8 e T9"), desde que nenhuma dependa de task 🧑 pendente. Mesmo assim, é um commit por task.

## Protocolo do autor (🧑, local)

- Trabalhar numa branch local (`autor/<faseN>-t<N>-<slug>`) a partir do `main`; digitar o código do guia (não copiar).
- Mesmo gate (`./gradlew ktlintFormat && ./gradlew build`) e marcar a task no `tasks.md` no mesmo commit.
- Pedir revisão ao agente local quando quiser; integrar no `main` por PR ou merge local, e push quando decidir.
- Antes de delegar tasks ☁️ que dependem de uma task sua, garantir que ela já está no `main`.

## Agente local (🤖)

Faz o que exige conversa com o autor: abrir a fase (spec/design/tasks), gerar o guia, revisar commits do autor, rodar o Verifier, os exercícios de quebra e a entrevista. Segue o `CLAUDE.md`: não implementa task 🧑, não faz push sem pedido.

## Validadores (rodar da raiz do repositório)

```bash
S=.claude/skills/tlc-spec-driven/scripts
python3 $S/validate_spec.py  .specs/features/<fase>/spec.md
python3 $S/validate_tasks.py .specs/features/<fase>/tasks.md
python3 $S/check_commit.py --message "feat(transfer): ..."
python3 $S/validate_state.py <fase>      # só no fechamento, depois do validation.md
```
