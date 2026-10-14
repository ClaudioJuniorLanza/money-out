# Experimentos — Fase 0

## F0-01 — Versões verificadas na fonte (2026-10-10)

Consultadas em `repo.maven.apache.org` (metadata XML) e `services.gradle.org/versions/current`. Só versões GA (sem milestone/RC).

| Item | Versão | Observação |
|---|---|---|
| Spring Boot | 4.1.1 | `4.2.0-M2` existe, mas é milestone |
| Kotlin | 2.3.21 | a mesma gerenciada pelo `spring-boot-dependencies:4.1.1` (existe 2.4.x, mas o BOM manda) |
| JUnit | 6.0.3 | gerenciada pelo BOM |
| Testcontainers | 2.0.5 | gerenciada pelo BOM (entra na Fase 2) |
| Flyway | 12.4.0 | gerenciada pelo BOM (entra na Fase 2); o Central já tem 13.x |
| Kotest | 6.2.5 | fora do BOM; fixada no catálogo |
| ArchUnit | 1.5.1 | fora do BOM; fixada no catálogo |
| Gradle | 9.8.1 | wrapper |
| JDK | 21 (toolchain) | LTS; o ambiente de CI/sessão tem 21 |

Decisão: seguir o BOM do Spring Boot como fonte da verdade (política do ADR-0001).

## F0-02 — Build verde

`./gradlew build` ⇒ BUILD SUCCESSFUL. 12 testes: `CurrencyTest` (2), `UuidV7GeneratorTest` (4), `ArchitectureTest` (5), `MoneyOutApplicationTests` (1).

Achados durante o build:
- O compilador (`allWarningsAsErrors`) pegou um erro real: em um `enum`, a inicialização de uma propriedade não pode usar o `companion object` (ainda não inicializado). Solução: propriedade calculada.
- O ktlint rejeitou nomes de propriedade com crases (`val \`nome\``) nas regras do ArchUnit. Solução: camelCase nas propriedades; os nomes em português com crases continuam valendo para funções de teste.
- O Maven Central devolveu `429 Too Many Requests` em várias resoluções de dependência neste ambiente de sessão; repetir o build com pausa resolveu (o Gradle guarda em cache o que já baixou). Não foi usado nenhum espelho alternativo.

## F0-03 — As regras de arquitetura não são vazias

Numa cópia descartável do projeto, plantamos uma classe com `@Component` (Spring), `Instant.now()` e um campo `Double` em `..domain..`. Resultado: as regras `domainHasNoFrameworkDependency`, `domainDoesNotReadGlobalClock` e `domainDoesNotUseFloatingPoint` falharam; as demais continuaram verdes, como esperado. Também confirmamos que o código do `docs/fases/fase0-guia.md` (Money e esqueleto do teste de `allocate`) compila.

## F0-04 — Reestruturação para hexagonal/DDD (ADR-0011)

Motivo: o autor questionou o pacote `shared/domain` (o `Money` tem invariantes; `shared` genérico vira lixeira). Mudanças: `Currency`, `DomainException` e a porta `IdGenerator` foram para `sharedkernel`; `UuidV7Generator` virou adaptador em `com.moneyout.infrastructure.id`; as regras do ArchUnit passaram a cobrir `..domain..` e `..sharedkernel..` e ganharam `sharedKernelDoesNotDependOnAnyContext`.

Verificação (cópias descartáveis): `Bad` no `sharedkernel` com `@Component`, `Instant.now()` e `Double` ⇒ falharam `pureCodeHasNoFrameworkDependency`, `pureCodeDoesNotReadGlobalClock` e `pureCodeDoesNotUseFloatingPoint`. Uma classe do `sharedkernel` usando `com.moneyout.transfer.Anything` ⇒ falhou `sharedKernelDoesNotDependOnAnyContext`. (Uma primeira tentativa não falhou porque o `import` estava sem uso, logo sem dependência no bytecode; o teste da regra estava errado, não a regra.) O código do guia da Fase 0 compila no pacote novo.

## F0-05 — Migração da toolchain para Java 25 (2026-10-13)

Motivo: começar na LTS mais recente para não ficar preso a bibliotecas defasadas ao longo do projeto e alinhar com o `platform-eda`. Fontes consultadas:

| Item | Resultado | Fonte |
|---|---|---|
| Kotlin 2.3.21 | gera bytecode Java 25 desde a 2.3.0 | [What's new in Kotlin 2.3.0](https://kotlinlang.org/docs/whatsnew23.html) |
| Gradle 9.8.1 | toolchain e execução em Java 25 desde a 9.1.0 | [Gradle compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html) |
| Spring Boot 4.1.1 | Java 17 até 26 | [System requirements](https://docs.spring.io/spring-boot/system-requirements.html) |
| Virtual threads | `synchronized` não prende mais a carrier thread desde o JDK 24; pinning só via código nativo | [JEP 491](https://openjdk.org/jeps/491) |

Observação: a tabela de compatibilidade do Kotlin Gradle Plugin lista Gradle até 9.3.0 para o KGP 2.3.21 ([Kotlin docs](https://kotlinlang.org/docs/gradle-configure-project.html)). O projeto já roda com Gradle 9.8.1 (build verde, F0-02 a F0-04): versões acima da máxima testada funcionam, mas sem garantia formal. Fica para revisar quando o BOM do Spring Boot trouxer um Kotlin mais novo.

Verificação: `./gradlew build` ⇒ BUILD SUCCESSFUL com JDK Temurin 25.0.4; 13 testes; `javap` mostra `major version: 69` (Java 25) nas classes compiladas.

Mudanças: `jvmToolchain(25)` no `build.gradle.kts`, `java-version: 25` no CI, ADR-0001, ADR-0012, `.claude/rules/kotlin.md`, `README.md` e `docs/PLANO.md`.

## F0-06 — Regra contra `UUID.randomUUID()` no código puro (2026-10-14)

Regra nova `pureCodeDoesNotGenerateRandomIds` na `ArchitectureTest`, cobrindo `..domain..` e `..sharedkernel..` (em `application` o `IdGenerator` é injetado, então a regra global fica só no código puro; spec, premissa 3).

Prova (cópia descartável do projeto, fora do working tree): classe `Bad` em `com.moneyout.sharedkernel` com `fun id(): UUID = UUID.randomUUID()`. Resultado do `./gradlew test --tests '*ArchitectureTest*'`: 7 testes, 1 falha, e a falha é só a regra nova:

```text
ArchitectureTest > pureCodeDoesNotGenerateRandomIds FAILED
Rule 'no classes that reside in any package ['..domain..', '..sharedkernel..'] should call method UUID.randomUUID()' was violated (1 times)
```

As outras 6 regras continuaram verdes. No working tree real a regra passa (`git status` limpo após a prova).

Ambiente: a sessão de nuvem só tinha JDK 21; o JDK 25 (Oracle, `download.oracle.com`) foi baixado fora do repositório e passado ao Gradle com `-Dorg.gradle.java.installations.paths`. O Central voltou a responder `429` em algumas resoluções; repetir resolveu.

## Exercícios de quebra

Cada exercício foi feito numa **cópia descartável** do projeto (a mutação nunca tocou o working tree real; `git status` igual antes e depois). Comando em cada cópia: `./gradlew test --continue`. Os exercícios 1–3 rodaram sobre 31 testes e o 4 sobre 37: entre eles entrou a correção descrita em "Achado extra", que fez 6 property tests passarem a executar (as regras do ArchUnit não dependem disso).

| # | Quebra | Quem pegou | Resultado |
|---|---|---|---|
| 1a | `Money.minorUnits: Long` → `Double` | compilador | `compileKotlin FAILED`, dezenas de `Argument type mismatch: actual type is 'Long', but 'Double' was expected` / `Math.addExact` não aceita `Double` |
| 1b | classe `BadMoney(val minorUnits: Double)` no `sharedkernel` | ArchUnit `pureCodeDoesNotUseFloatingPoint` | 31 testes, 1 falha (só essa regra) |
| 2 | `Instant.now()` no `sharedkernel` | ArchUnit `pureCodeDoesNotReadGlobalClock` | 31 testes, 1 falha |
| 3 | `@Component` (Spring) no `sharedkernel` | ArchUnit `pureCodeHasNoFrameworkDependency` | 31 testes, 1 falha |
| 4 | `allocate` que ignora o resto (`val part = floor`) | `MoneyAllocateTest` (property `a soma das partes e exatamente o total`, mais 3 exemplos) | 37 testes, 4 falhas |

### 1 — dinheiro em ponto flutuante

Como o `Money` do projeto usa `Math.addExact`/`subtractExact`/`negateExact`, trocar o tipo para `Double` nem compila: a aritmética exata só existe para inteiros. Foi o **tipo** que protegeu, antes de qualquer teste. A regra do ArchUnit (1b) cobre o caso em que alguém não toca no `Money`, mas cria outro VO com `double`/`float`; ela olha campos declarados no código puro, então não pegaria um `Double` só em variável local. Linguagem de entrevista: `0.1 + 0.2 != 0.3` em binário; centavos em inteiro somam exatamente, e o sistema de tipos impede a regressão no ponto mais barato.

### 2 — relógio global no código puro

`Instant.now()` dentro do `sharedkernel` derruba `pureCodeDoesNotReadGlobalClock`. O motivo é determinismo: um domínio que lê o relógio não é reproduzível em teste nem em rehidratação. O tempo entra por parâmetro, vindo do `Clock` injetado (bean da T1).

### 3 — framework no núcleo

`@Component` no `sharedkernel` derruba `pureCodeHasNoFrameworkDependency`. A dependência que importa é a que fica no **bytecode** (a regra analisa bytecode): um `import` sem uso não conta, como já visto no F0-04. Linguagem de entrevista: o domínio não conhece o container; quem depende de quem aponta para dentro (ADR-0011).

### 4 — `allocate` sem o resto

Sem distribuir o resto, R$ 100,00 em 3 vira 33,33 + 33,33 + 33,33 = 99,99: um centavo some. Falharam a propriedade principal (`soma == total`, com `Property failed after 1 attempts` e seed reproduzível) e os exemplos concretos (`expected:<3334L> but was:<3333L>`, positivo e negativo, e o caso de `Long.MAX_VALUE`). A propriedade "cada parte a menos de 1 minor unit da fração exata" **continuou verde**: o piso da fração também satisfaz esse limite, então ele sozinho não detecta o resto perdido. Quem protege o centavo é a invariante da soma, não a de proximidade; as duas juntas caracterizam o algoritmo.

### Achado extra: property tests que não rodavam

Ao preparar o exercício 4, a mutação só derrubava 3 exemplos e **nenhuma** propriedade. A causa: testes escritos como `fun x() = runBlocking { checkAll(...) }` devolvem `PropertyContext`, e o JUnit só executa métodos de teste `void`. Os 6 property tests de `MoneyTest`/`MoneyAllocateTest` eram descobertos como inexistentes (31 testes em vez de 37) e o build ficava verde. Correção: `runBlocking<Unit>` (commit `test(sharedkernel): executa os property tests de money`). Lição: um teste que nunca falha pode ser um teste que nunca roda; a contagem de testes e a mutação deliberada são o que desmascara isso. A `tests.md` já prescreve `runBlocking { checkAll }`; vale lembrar do `<Unit>` ao copiar o padrão.
