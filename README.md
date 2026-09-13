# Reservas de Salas — mini projeto de arquitetura

Projeto didático para praticar, em Java 25 + Spring Boot 4, os seguintes padrões **juntos**:

| Padrão | Como aparece aqui |
|---|---|
| **Hexagonal (Ports & Adapters)** | pacotes `input` → `core` ← `output`; o core não conhece framework |
| **UseCase** | um por endpoint; orquestra ports e domínio; nunca contém regra de negócio; nunca chama outro UseCase |
| **Command** | todo UseCase expõe apenas `execute(XxxCommand)`; Command é `record` imutável e valida só forma |
| **Result** | todo port de saída retorna `Result<T>`; adapters nunca lançam exceção |
| **DDD / domínio rico** | agregados `Sala` e `Reserva` com comportamento; Value Objects como `record`; invariantes no construtor |

As regras são verificadas por **testes de arquitetura (ArchUnit)** em `src/test/java/.../ArchitectureTest.java`. Se `mvn test` passa, a arquitetura está respeitada.

---

## 1. Como rodar

```bash
docker compose up -d          # Postgres 17 com o schema de docker/init.sql já criado
mvn test                      # testes de arquitetura + Result
mvn spring-boot:run
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Banco: `jdbc:postgresql://localhost:5432/reservas` (usuário/senha `reservas`)

A aplicação sobe com `spring.jpa.hibernate.ddl-auto=validate`: **as entidades JPA que você escrever precisam bater com o DDL**. Para recriar o banco do zero: `docker compose down -v && docker compose up -d`.

---

## 2. O que já está pronto e o que você implementa

**Pronto (não altere):**
- Estrutura de pacotes com `package-info.java` explicando cada responsabilidade
- `shared.Result<T>` — o contrato dos ports
- `ArchitectureTest` — o contrato da arquitetura
- `docker-compose.yml` + `docker/init.sql` (DDL e carga de salas)
- `config.OpenApiConfig` e `config.UseCaseConfig` (este último você preenche com `@Bean`)
- `application.yml`

**Você implementa tudo o mais:**
- `core.domain`: Value Objects, agregados, enum de status, exceções de domínio
- `core.usecase`: 4 UseCases, 4 Commands, exceções de aplicação
- `core.port`: interfaces dos ports de saída
- `output.persistence`: entidades JPA, repositórios, adapters, mappers
- `output.notification` e `output.time`: adapters
- `input.rest`: controllers, DTOs, mappers de saída, `@ControllerAdvice`
- `config.UseCaseConfig`: registro dos UseCases como `@Bean`
- Testes unitários de domínio e de UseCase (ver seção 9)

---

## 3. Estrutura de pacotes

```
brenner.edu.reservas
├── input
│   └── rest
│       ├── controller    um endpoint = um UseCase; só conhece usecase/command/dto/mapper
│       ├── dto           request/response (Bean Validation e anotações OpenAPI permitidas)
│       ├── mapper        domínio → DTO de resposta (único lugar de input que vê core.domain)
│       └── handler       @ControllerAdvice: exceção → status HTTP
├── core
│   ├── domain            agregados, VOs, enum, DomainException (Java puro)
│   ├── usecase           UseCases, Commands, *NotFoundException, *UnavailableException
│   └── port              interfaces *Port; todo método retorna Result<T>
├── output
│   ├── persistence       *Entity (JPA), *Repository (Spring Data), adapters, mappers
│   ├── notification      adapter que loga
│   └── time              adapter de data/hora atual por fuso
├── config                @Bean dos UseCases, OpenAPI
└── shared                Result
```

Regras de dependência (todas testadas):

- `core` não depende de `input`, `output` nem `config`, e não importa Spring/JPA/Bean Validation/Jackson. **Única exceção:** `org.springframework.transaction.annotation.Transactional` no UseCase.
- `input` e `output` não se conhecem.
- `core.domain` não depende de `core.usecase` nem de `core.port`, e não conhece `Result`.
- Controllers não acessam `core.domain` nem `core.port`.

---

## 4. O contrato `Result<T>`

Um `Result` está em exatamente um de três estados:

| Estado | Como o adapter cria | O que o UseCase recebe |
|---|---|---|
| Falha técnica | `Result.failure(e)` | `getAvailableValueOrElseThrow(...)` lança a exceção que você mapear |
| Sucesso vazio | `Result.empty()` | `Optional.empty()` |
| Sucesso com valor | `Result.of(valor)` | `Optional.of(valor)` |

Forma canônica de consumo no UseCase:

```java
final var reserva = getReservaPort.getById(reservaId)
        .getAvailableValueOrElseThrow(GetReservaUnavailableException::new)   // 503
        .orElseThrow(() -> new ReservaNotFoundException(reservaId));         // 404
```

Forma canônica de um adapter:

```java
@Override
public Result<Reserva> getById(ReservaId id) {
    try {
        return repository.findById(id.value())
                .map(mapper::toDomain)
                .map(Result::of)
                .orElseGet(Result::empty);
    } catch (Exception e) {
        return Result.failure(e);
    }
}
```

Regras testadas: todo método de port retorna `Result`; `Result.of/empty/failure` só são chamados em `output`; adapters não declaram `throws`; o domínio não conhece `Result`.

---

## 5. Domínio

### 5.1 Value Objects (`record`, validação no construtor compacto, lançam `DomainException`)

| VO | Invariantes |
|---|---|
| `SalaId(UUID)` / `ReservaId(UUID)` | não nulo; fábrica `novo()` gera `UUID.randomUUID()` |
| `Email(String)` | não vazio, formato `algo@dominio.tld`, normalizado para minúsculas |
| `Capacidade(int)` | `> 0` |
| `QuantidadeParticipantes(int)` | `> 0` |
| `FusoHorario(ZoneId)` | fábrica `de(String)` que lança exceção de domínio se o ID IANA for inválido |
| `DataHora(Instant instante, FusoHorario fuso)` | não nulos; expõe `data()`, `hora()`, `mais(Duration)`, `menos(Duration)`, `antesDe(DataHora)`, `depoisDe(DataHora)`, `entre(DataHora, DataHora)` |
| `Periodo(DataHora inicio, DataHora fim)` | `fim` estritamente depois de `inicio`; mesmo fuso; duração entre **30 min e 4 h**; início e fim no **mesmo dia local**; início alinhado a **múltiplos de 15 min**; contido no horário de funcionamento **08:00–20:00 local**; expõe `duracao()` e `sobrepoe(Periodo)` |

`StatusReserva` é um `enum`: `PENDENTE`, `CONFIRMADA`, `CANCELADA`, `EXPIRADA`.

### 5.2 Agregado `Sala` (raiz)

Atributos: `SalaId id`, `String nome`, `Capacidade capacidade`, `FusoHorario fuso`, `boolean ativa`.

Comportamentos:

- `Reserva reservar(Email organizador, Periodo periodo, QuantidadeParticipantes participantes, List<Reserva> reservasAtivas, DataHora agora)`
  - sala inativa → `SalaInativaException`
  - `participantes > capacidade` → `CapacidadeExcedidaException`
  - `periodo.inicio` a menos de **1 hora** de `agora` → `AntecedenciaMinimaException`
  - `periodo` sobrepõe qualquer reserva de `reservasAtivas` → `ConflitoDeHorarioException`
  - devolve `Reserva` nova com status `PENDENTE` e `criadaEm = agora`
- `List<Periodo> horariosLivresEm(LocalDate data, List<Reserva> reservasDoDia)`
  - calcula os intervalos livres entre 08:00 e 20:00 no fuso da sala, descontando os períodos das reservas `PENDENTE`/`CONFIRMADA`

### 5.3 Agregado `Reserva` (raiz)

Atributos: `ReservaId id`, `SalaId salaId`, `Email organizador`, `Periodo periodo`, `QuantidadeParticipantes participantes`, `StatusReserva status`, `DataHora criadaEm`.

Comportamentos:

- `void cancelar(Email solicitante, DataHora agora)`
  - status não é `PENDENTE` nem `CONFIRMADA` → `TransicaoDeStatusInvalidaException`
  - `solicitante` ≠ `organizador` → `SolicitanteNaoEOrganizadorException`
  - `agora` a menos de **2 horas** do início → `PrazoDeCancelamentoExpiradoException`
  - muda status para `CANCELADA`
- `void realizarCheckIn(DataHora agora)`
  - status ≠ `PENDENTE` → `TransicaoDeStatusInvalidaException`
  - `agora` antes de `inicio − 10 min` → `CheckInAntecipadoException`
  - `agora` depois de `inicio + 15 min` → status vira `EXPIRADA` **e** lança `ReservaExpiradaException`
  - senão status vira `CONFIRMADA`
- `boolean estaAtiva()` → `PENDENTE` ou `CONFIRMADA`

Regras testadas: sem setters, campos privados, agregados `final`, VOs `record`, exceções de domínio são `RuntimeException` com sufixo `Exception`. Crie uma classe base abstrata `DomainException` para o handler mapear tudo para 422.

---

## 6. Endpoints (um UseCase por endpoint)

Datas nas requisições e respostas são **locais ao fuso da sala**, no formato ISO `yyyy-MM-dd'T'HH:mm`. É o UseCase que, com a `Sala` em mãos, converte para `DataHora`. Toda resposta inclui o campo `fuso`.

Erros seguem o formato `application/problem+json` (`ProblemDetail`): `status`, `title`, `detail`, `timestamp`.

### 6.1 `POST /reservas` — `CriarReservaUseCase(CriarReservaCommand)`

Request:
```json
{
  "salaId": "11111111-1111-1111-1111-111111111111",
  "organizador": "ana@empresa.com",
  "inicio": "2026-09-15T14:00",
  "fim": "2026-09-15T15:30",
  "quantidadeParticipantes": 6
}
```

Resposta `201 Created` (+ header `Location: /reservas/{id}`):
```json
{
  "id": "…", "salaId": "…", "organizador": "ana@empresa.com",
  "inicio": "2026-09-15T14:00", "fim": "2026-09-15T15:30",
  "fuso": "America/Sao_Paulo", "quantidadeParticipantes": 6,
  "status": "PENDENTE", "criadaEm": "2026-09-12T09:03"
}
```

Fluxo do UseCase:
1. `GetSalaPort.getById` → 503 se indisponível, 404 se ausente
2. `GetDataHoraAtualPort.get(sala.fuso())` → 503 se indisponível
3. `GetReservasAtivasDaSalaPort.get(salaId, inicioDoDia, fimDoDia)` → 503 se indisponível
4. `sala.reservar(...)` → regras de domínio (422)
5. `SaveReservaPort.save(reserva)` → 503 se indisponível
6. `NotificarOrganizadorPort.notificar(reserva)` → 503 se indisponível (a transação reverte)

Erros: `400` forma inválida; `404` sala; `422` sala inativa, capacidade, antecedência, conflito, período inválido; `503` port indisponível.

### 6.2 `DELETE /reservas/{id}` — `CancelarReservaUseCase(CancelarReservaCommand)`

Header obrigatório: `X-Solicitante: ana@empresa.com`.

Resposta `204 No Content`.

Fluxo: buscar reserva → buscar sala (para o fuso) → obter agora → `reserva.cancelar(solicitante, agora)` → salvar.

Erros: `400`; `404` reserva; `422` status inválido, solicitante não é o organizador, prazo expirado; `503`.

### 6.3 `POST /reservas/{id}/check-in` — `RealizarCheckInUseCase(RealizarCheckInCommand)`

Sem corpo. Resposta `200 OK` com a reserva (mesmo JSON do 6.1, status `CONFIRMADA`).

Fluxo: buscar reserva → buscar sala → obter agora → `reserva.realizarCheckIn(agora)` → salvar.

Atenção ao caso de expiração: o domínio muda o status para `EXPIRADA` **e** lança exceção. A reserva expirada **deve ser persistida** mesmo com a exceção. Isso exige salvar antes de propagar e configurar `@Transactional(noRollbackFor = ReservaExpiradaException.class)`. Este é o ponto mais sutil do projeto; documente sua solução no UseCase.

Erros: `400`; `404`; `422` status inválido, check-in antecipado, reserva expirada; `503`.

### 6.4 `GET /salas/{id}/disponibilidade?data=2026-09-15` — `ConsultarDisponibilidadeUseCase(ConsultarDisponibilidadeCommand)`

Resposta `200 OK`:
```json
{
  "salaId": "…", "nome": "Sala Pirapora", "fuso": "America/Sao_Paulo",
  "data": "2026-09-15",
  "horariosLivres": [
    { "inicio": "2026-09-15T08:00", "fim": "2026-09-15T14:00" },
    { "inicio": "2026-09-15T15:30", "fim": "2026-09-15T20:00" }
  ],
  "reservas": [
    { "id": "…", "inicio": "2026-09-15T14:00", "fim": "2026-09-15T15:30", "status": "PENDENTE" }
  ]
}
```

Fluxo: buscar sala → obter agora → `data` anterior a `agora.data()` → `DataNoPassadoException` (422) → buscar reservas ativas do dia → `sala.horariosLivresEm(data, reservas)`.

Uma consulta também é um UseCase, recebe um Command e passa pelos mesmos ports. Não há atalho "só leitura".

---

## 7. Commands

`record` imutável em `core.usecase`. Construtor compacto valida **apenas forma** (não nulo, formato parseável, número positivo) lançando `IllegalArgumentException`/`NullPointerException` → o handler converte em `400`. Nada de regra de negócio aqui e nada de Value Object: Commands carregam `UUID`, `String`, `LocalDateTime`, `LocalDate`, `int`.

```java
public record CriarReservaCommand(UUID salaId, String organizador, LocalDateTime inicio,
                                  LocalDateTime fim, int quantidadeParticipantes) {
    public CriarReservaCommand {
        Objects.requireNonNull(salaId, "salaId é obrigatório");
        Objects.requireNonNull(organizador, "organizador é obrigatório");
        Objects.requireNonNull(inicio, "inicio é obrigatório");
        Objects.requireNonNull(fim, "fim é obrigatório");
        if (quantidadeParticipantes <= 0) throw new IllegalArgumentException("quantidadeParticipantes deve ser > 0");
    }
}
```

O controller monta o Command a partir do DTO e chama `useCase.execute(command)`. O UseCase constrói os VOs (`new Email(command.organizador())`), e é aí que uma exceção de domínio pode surgir.

---

## 8. Ports sugeridos

Você define as interfaces, mas este conjunto atende os 4 endpoints:

```java
Result<Sala>          GetSalaPort.getById(SalaId id);
Result<Reserva>       GetReservaPort.getById(ReservaId id);
Result<List<Reserva>> GetReservasAtivasDaSalaPort.get(SalaId id, DataHora inicio, DataHora fim); // PENDENTE/CONFIRMADA
Result<Reserva>       SaveReservaPort.save(Reserva reserva);       // insert ou update
Result<Void>          NotificarOrganizadorPort.notificar(Reserva reserva);
Result<DataHora>      GetDataHoraAtualPort.get(FusoHorario fuso);
```

Para `Result<Void>` e `Result<List<…>>` o `Optional` retornado é apenas a confirmação de sucesso; use `getAvailableValueOrElseThrow(...)` mesmo assim, é ele que trata a falha.

Sobre o relógio: "agora" depende do fuso da sala (Londres, Nova York, Pirapora), e obter a hora é uma dependência externa. Por isso é um port com `Result`, e o domínio **recebe** `DataHora` como parâmetro, nunca chama `Instant.now()`.

---

## 9. Exceções e mapeamento HTTP

| Família | Onde mora | Status |
|---|---|---|
| `IllegalArgumentException` / `NullPointerException` de Command, `MethodArgumentNotValidException` de DTO | — | `400` |
| `*NotFoundException` (base abstrata `NotFoundException`) | `core.usecase` | `404` |
| `DomainException` e subclasses | `core.domain` | `422` |
| `*UnavailableException` (base abstrata `UnavailableException`, guarda a causa) | `core.usecase` | `503` |

O `@ControllerAdvice` fica em `input.rest.handler`, devolve `ProblemDetail` e **loga a causa** das `UnavailableException` (é a única forma de o operador descobrir o que aconteceu, já que o adapter engoliu a exceção).

---

## 10. Persistência

O DDL está em `docker/init.sql`. Mapeie `ReservaEntity` e `SalaEntity` (sufixo `Entity` é obrigatório) em `output.persistence`, com `TIMESTAMPTZ` ↔ `Instant`. O mapper `Entity ↔ domínio` reconstrói os VOs; se um dado no banco violar um invariante, o adapter captura e devolve `Result.failure`.

Dica para `SaveReservaPort`: `JpaRepository.save` faz upsert pelo id, então um único port serve para criar e atualizar.

---

## 11. Critérios de aceite

1. `mvn test` verde, incluindo todos os `ArchitectureTest`.
2. Os 4 endpoints funcionam pelo Swagger contra o banco do `docker compose`, com todos os status HTTP da seção 6.
3. Testes unitários **sem Spring** para:
   - cada Value Object (invariantes válidos e inválidos);
   - `Sala.reservar`, `Sala.horariosLivresEm`, `Reserva.cancelar`, `Reserva.realizarCheckIn` (incluindo a expiração);
   - cada UseCase, usando implementações fake dos ports (ex.: uma classe anônima que devolve `Result.failure`) para cobrir os três estados do `Result` — falha, vazio e valor.
4. Cada endpoint documentado com `@Operation` e `@ApiResponse` para todos os status.
5. Nenhum `Instant.now()`/`LocalDateTime.now()` fora do adapter `output.time`.

Cenário de demonstração sugerido: criar reserva na Sala Londres e outra na Sala Nova York com os mesmos valores locais de `inicio`/`fim`, e mostrar que são instantes diferentes no banco.

---

## 12. Perguntas para discutir em sala

- Por que `Result.failure` **e** exceção de domínio, e não um único mecanismo?
- O que aconteceria se `GetDataHoraAtualPort` fosse um `java.time.Clock` injetado? O que se perde e o que se ganha?
- `@Transactional` no UseCase viola a hexagonal? Qual seria a alternativa purista e ela vale o custo?
- O check-in que expira precisa persistir e falhar ao mesmo tempo. Quais outras formas de modelar isso existem?
- Se um novo endpoint precisasse "criar reserva e fazer check-in imediato", como fazer sem um UseCase chamar outro?
