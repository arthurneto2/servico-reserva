# Reservas de Salas — arquitetura hexagonal em Java

API REST de reserva de salas em **Java 25 + Spring Boot 4 + PostgreSQL**, construída para praticar, juntos e de ponta a ponta, arquitetura hexagonal, casos de uso com Command, um tipo `Result` nos ports e domínio rico (DDD). As regras de arquitetura não ficam só na documentação: são verificadas por testes **ArchUnit** a cada `mvn test`.

O domínio é pequeno, mas tem dois pontos que costumam dar errado em sistemas reais: **fuso horário** (cada sala tem o seu) e **um caso de uso que precisa gravar um estado e falhar ao mesmo tempo** (o check-in atrasado).

## Status

Os quatro casos de uso estão implementados, com adapters de persistência, notificação e relógio, camada REST documentada no Swagger e **mais de 300 testes** (domínio, casos de uso, commands, controllers, mappers, adapters e arquitetura).

## Arquitetura

```
input.rest  ──►  core  ◄──  output
(controllers,    (domain,    (JPA, notificação,
 DTOs, handler)   usecase,    relógio por fuso)
                  port)
```

| Padrão | Como aparece no código |
|---|---|
| **Hexagonal (Ports & Adapters)** | pacotes `input → core ← output`; o `core` não importa Spring, JPA, Jackson nem Bean Validation (exceção única: `@Transactional` nos casos de uso) |
| **UseCase + Command** | um caso de uso por endpoint, com um único método `execute(XxxCommand)`; o Command é um `record` imutável que valida só a forma (nulos, números positivos) |
| **Result nos ports** | todo port de saída devolve `Result<T>` com três estados: falha técnica, vazio ou valor; os adapters nunca lançam exceção |
| **Domínio rico (DDD)** | agregados `Sala` e `Reserva` sem setters, Value Objects como `record` com invariantes no construtor (`Email`, `Periodo`, `DataHora`, `FusoHorario`…) |

As **23 regras ArchUnit** em `ArchitectureTest` garantem, entre outras coisas, que `input` e `output` não se conhecem, que controllers não acessam o domínio nem os ports, que todo port retorna `Result`, que adapters não declaram exceções e que o domínio não conhece `Result`.

## Decisões técnicas

### 1. O check-in atrasado grava e falha ao mesmo tempo

Se o organizador chega mais de 15 minutos depois do início, a reserva precisa virar `EXPIRADA` (para liberar a agenda) **e** a requisição precisa responder com erro (422). Isso exige duas providências juntas em `RealizarCheckInUseCase`:

- gravar a reserva expirada **antes** de propagar a exceção;
- `@Transactional(noRollbackFor = ReservaExpiradaException.class)`, porque sem isso o Spring desfaria a transação ao ver a exceção e a gravação iria junto.

Se a própria gravação falhar, sobe 503 em vez de 422: não se anuncia como definitivo um estado que não foi persistido. Alternativas consideradas (devolver um resultado em vez de lançar, ou expirar em um processo agendado) estão documentadas no Javadoc da classe.

### 2. "Agora" é uma dependência externa

O horário atual depende do fuso da sala (São Paulo, Londres, Nova York), então ele vem de um port, `GetDataHoraAtualPort`, que recebe o fuso e devolve `Result<DataHora>`. O domínio recebe o "agora" como parâmetro e nunca consulta o relógio, o que torna as regras de antecedência, cancelamento e check-in testáveis sem mock de tempo.

### 3. Dois mecanismos de erro, cada um com seu papel

Falha técnica (banco fora, relógio indisponível) chega como `Result.failure` e vira 503. Regra de negócio violada é exceção de domínio e vira 422. Separar os dois evita que um problema de infraestrutura apareça para o cliente como "regra violada", e vice-versa.

| Origem | Status |
|---|---|
| Command ou DTO inválido | 400 |
| `NotFoundException` (sala ou reserva) | 404 |
| `DomainException` e subclasses | 422 |
| `UnavailableException` (port indisponível) | 503 |

Todas as respostas de erro seguem `application/problem+json` (RFC 7807).

## Regras de negócio

- Período entre 30 min e 4 h, no mesmo dia local, começando em múltiplos de 15 min e dentro do horário 08:00–20:00 da sala.
- Reserva com pelo menos 1 h de antecedência, sem conflito com reservas ativas e dentro da capacidade da sala.
- Cancelamento só pelo organizador e até 2 h antes do início.
- Check-in de 10 min antes até 15 min depois do início; depois disso a reserva expira.

## Endpoints

| Método | Rota | Caso de uso |
|---|---|---|
| `POST` | `/reservas` | `CriarReservaUseCase` |
| `DELETE` | `/reservas/{id}` (header `X-Solicitante`) | `CancelarReservaUseCase` |
| `POST` | `/reservas/{id}/check-in` | `RealizarCheckInUseCase` |
| `GET` | `/salas/{id}/disponibilidade?data=AAAA-MM-DD` | `ConsultarDisponibilidadeUseCase` |

Datas de entrada e saída são locais ao fuso da sala (`yyyy-MM-dd'T'HH:mm`), e toda resposta informa o campo `fuso`. No banco, os horários ficam como `TIMESTAMPTZ`.

Exemplo de criação:

```json
POST /reservas
{
  "salaId": "11111111-1111-1111-1111-111111111111",
  "organizador": "ana@empresa.com",
  "inicio": "2026-09-15T14:00",
  "fim": "2026-09-15T15:30",
  "quantidadeParticipantes": 6
}
```

Uma boa demonstração é criar a mesma reserva local (14:00–15:30) na Sala Londres e na Sala Nova York e ver que viram instantes diferentes no banco.

## Como executar

Pré-requisitos: Java 25, Maven e Docker.

```bash
docker compose up -d          # PostgreSQL 17 com schema e salas de exemplo (docker/init.sql)
mvn test                      # testes de domínio, casos de uso, REST, adapters e arquitetura
mvn spring-boot:run
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI: http://localhost:8080/v3/api-docs

A aplicação usa `ddl-auto=validate`: o schema vem de `docker/init.sql` e as entidades JPA precisam bater com ele. Para recriar o banco: `docker compose down -v && docker compose up -d`.

## Estrutura

```
src/main/java/brenner/edu/reservas
├── input/rest      controllers, DTOs, mappers de saída e @ControllerAdvice
├── core
│   ├── domain      agregados Sala e Reserva, Value Objects, exceções de domínio
│   ├── usecase     4 casos de uso, Commands e exceções de aplicação
│   └── port        interfaces de saída, todas retornando Result<T>
├── output
│   ├── persistence entidades JPA, repositórios, adapters e mappers
│   ├── notification
│   └── time        relógio por fuso horário
├── config          registro dos casos de uso como @Bean e OpenAPI
└── shared          Result<T>
```

## Autor

Arthur Roberto de Paula Neto — [GitHub](https://github.com/arthurneto2) · [LinkedIn](https://linkedin.com/in/arthurneto-dev)
