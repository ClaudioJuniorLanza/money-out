# Regra: Kotlin no money-out

## Convenções obrigatórias

- `val` sobre `var`; nenhum `var` em `..domain..` nem em `..sharedkernel..`.
- `data class` para value objects, eventos, commands e DTOs.
- `sealed interface` para hierarquias de eventos, commands e resultados; `when` **exaustivo e sem `else`**, para que um caso novo quebre a compilação.
- Null safety: `?.`, `?.let {}`, `?:`. Nada de `!!` (em teste, `shouldNotBeNull()`).
- Sem números mágicos: constantes nomeadas em `private companion object` ou `const val`.
- Código e identificadores em inglês; KDoc e comentários em pt-BR; nomes de função de teste em pt-BR com crases.
- Comentários explicam o **porquê** (a regra de pagamento, o bug que evitam), não o quê.
- `allWarningsAsErrors = true` e `-Xjsr305=strict` já estão no `build.gradle.kts`: não desligue.

## Dinheiro e tempo (inegociáveis)

- Dinheiro é `com.moneyout.sharedkernel.Money` (inteiro em minor units + `Currency`), **nunca** `Double`/`Float`/`BigDecimal` no domínio. A ArchUnit derruba campos de ponto flutuante em `..domain..` e `..sharedkernel..`.
- Nada de `Instant.now()`, `LocalDateTime.now()`, `System.currentTimeMillis()` nem `UUID.randomUUID()` no domínio. O tempo chega como parâmetro (`Instant` vindo de um `java.time.Clock` injetado no handler); ids chegam como parâmetro ou do `IdGenerator`.
- Em JSON, valores monetários vão como `minorUnits` (inteiro) + `currency` (código ISO), nunca como decimal.

## Stack e dependências

- Spring Boot 4.1, Kotlin 2.3, Java 21 (toolchain), Gradle Kotlin DSL. As versões vêm do BOM do Spring Boot; só o que ele não gerencia entra em `gradle/libs.versions.toml` (ADR-0001). Versão nova = conferir na fonte e registrar em `docs/experiments/faseN.md`.
- Web: Spring MVC (`spring-boot-starter-web`/`webmvc`, conforme o nome vigente no Boot 4 — confira antes de adicionar). Nada de WebFlux nem coroutines na aplicação (ADR-0012).
- Banco: PostgreSQL + Flyway + `JdbcClient` (Spring JDBC). Sem JPA/Hibernate: o event store e as projeções são SQL explícito.
- Concorrência: virtual threads (`spring.threads.virtual.enabled: true`) quando entrar o servidor web.
- Serialização: Jackson **só em `..infrastructure..`** (a ArchUnit proíbe `com.fasterxml` no domínio).
- Imagens Docker com tag fixa (ex.: `postgres:17.6`), nunca `latest`.

## Spring

- Domínio e `application` não usam anotações Spring. Handlers são classes Kotlin puras; o wiring fica em `<ctx>/infrastructure/config/*Configuration.kt` com `@Bean`.
- Adapters (controllers, repositórios JDBC, clientes HTTP) ficam em `infrastructure` e podem usar `@RestController`, `@Component`, `@Repository`.
- Transação: `@Transactional` só em `infrastructure`. Se um caso de uso precisar de atomicidade entre dois agregados (atalho da Fase 3, ADR-0006), a `application` usa uma porta de saída (ex.: `Transactions`) implementada com `TransactionTemplate`.
- Configuração em `src/main/resources/application.yaml`; segredos por variável de ambiente; nada sensível versionado (`.env.example` documenta as variáveis).

## Banco

- Migrações em `src/main/resources/db/migration/V{n}__{acao}_{objeto}.sql` (ex.: `V1__create_event_store.sql`).
- Fatos são imutáveis: tabelas de eventos e lançamentos contábeis não recebem `UPDATE`/`DELETE` (garanta no banco quando a fase pedir).
- Nomes de tabela e coluna em `snake_case` inglês, sem prefixos de tipo.

## Logs

- Estruturados (JSON) com `traceId` e `correlationId` a partir da Fase 3.
- Nunca logar PII (CPF, CNPJ, nome, chave PIX, conta) nem segredos. `DomainException` nunca carrega PII na mensagem.

## Qualidade

- `./gradlew ktlintFormat` antes de commitar; `./gradlew build` é o gate (compila, ktlint, testes, ArchUnit).
- Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`), mensagem em pt-BR.
