# Category Service

Microsserviço de **categorias**. Cuida do cadastro, da consulta, da edição e da exclusão (com restauração) das
categorias de receita e despesa do usuário. As transações, os tetos de gasto e os outros serviços usam as categorias
por aqui.

- Porta local: `8004` (no Docker, `8004:8080`)
- Stack: Java 21, Spring Boot 3.3, PostgreSQL, MongoDB, Kafka, Redis e Keycloak (JWT)

---

## 1. Responsabilidades

| O serviço faz | O serviço não faz |
|---------------|-------------------|
| CRUD das categorias do usuário autenticado | Registrar transações (serviço `transaction`) |
| Exclusão lógica e restauração | Contas (serviço `account`) |
| Impedir categorias repetidas (mesmo nome e tipo) | Autenticar usuários (serviço `auth` e Keycloak) |
| Expor endpoints internos para os outros serviços | |

### Modelo da categoria

| Campo | Descrição |
|-------|-----------|
| `name` | nome da categoria |
| `type` | `INCOME` (receita) ou `EXPENSE` (despesa) |
| `deleted` | exclusão lógica |
| `createdAt` / `updatedAt` | datas de criação e de atualização |

---

## 2. API

Todos os endpoints exigem JWT (Keycloak), e o dono da categoria é sempre o usuário do token.

### Pública (`/categories`)

| Método | Rota | O que faz |
|--------|------|-----------|
| `POST` | `/categories` | cria a categoria (`409` se já existir uma com o mesmo nome e tipo) |
| `GET` | `/categories/{id}` | busca uma categoria |
| `GET` | `/categories?name=&type=&page=&size=&sort=` | lista com filtros e paginação |
| `GET` | `/categories/deleted` | lista as categorias excluídas |
| `PUT` | `/categories/{id}` | altera nome e/ou tipo |
| `DELETE` | `/categories/{id}` | exclui logicamente (`204`) |
| `POST` | `/categories/restore/{id}` | restaura uma categoria excluída (`204`, ou `409` se ela não estiver excluída) |

### Interna (`/internal/categories`, usada entre serviços)

| Método | Rota | O que faz |
|--------|------|-----------|
| `GET` | `/internal/categories/summary/{id}` | resumo da categoria (`id`, `name` e `deleted`) |
| `GET` | `/internal/categories/internal-summary/{id}` | resumo com `id` e `type` |

---

## 3. Arquitetura

Escrita e leitura são separadas (CQRS), e um Outbox garante que a cópia da categoria no Mongo não se perde.

```
                    ESCRITA                                              LEITURA
 POST/PUT/DELETE ──▶ CategoryService ──┐                      GET ──▶ CategoryQueryService ──▶ MongoDB
                                       │ (uma transação)
                                       ▼
                          ┌── category (Postgres)
                          └── outbox_event (Postgres)
                                       │ commit
                                       ▼
                                  OutboxRelay
                                       │
                                       ▼
                              CategoryProjector ──▶ MongoDB
```

### Pacotes

| Pacote | Papel |
|--------|-------|
| `controllers` | camada HTTP: validação de entrada e códigos de resposta |
| `services` | regras de escrita: `CategoryService`, `CategoryValidatorService` e `CategoryInternalService` |
| `query` | lado de leitura: `CategoryQueryService` (consultas), `CategorySpecification` (filtros da listagem), `CategoryDocument` (modelo do Mongo) e `CategoryProjector` (grava no Mongo) |
| `outbox` | `OutboxService` (registra), `OutboxEventFactory` (monta as linhas), `CategoryOutboxDispatcher` (entrega a linha ao destino), `OutboxEvent` (registro da tabela) e `OutboxConfig` (liga o relay) |
| `dto`, `enums`, `exceptions` | contratos de entrada e saída, tipos e erros de domínio |

---

## 4. Conceitos aplicados

### CQRS
Ler e escrever têm necessidades diferentes, então cada lado usa seu próprio banco:

- **Escrita:** PostgreSQL, com transações e as regras de unicidade.
- **Leitura:** MongoDB, com um documento por categoria e um índice em `userId + deleted + createdAt`.

O `GET /categories` nunca consulta o Postgres. Isso traz consistência eventual: logo depois de um `POST`, a categoria
pode levar alguns instantes para aparecer na leitura.

Os endpoints **internos** fazem o contrário: leem do Postgres, que é a fonte da verdade. Quem decide algo com base
na categoria (como o `transaction` ao validar uma transação) não pode usar um dado que esteja atrasado. Um teste
(`shouldNotDependOnTheMongoReadModel`) falha se o `CategoryInternalService` passar a depender do pacote `query`.

### Transactional Outbox
Salvar no Postgres e depois atualizar o Mongo são escritas em sistemas diferentes, e nenhuma transação cobre os
dois (o problema conhecido como *dual write*). Se o Mongo estiver fora do ar depois do commit, a categoria existe e
a leitura nunca fica sabendo.

A solução é gravar, na mesma transação da categoria, uma linha na tabela `outbox_event` com a **foto**
(*snapshot*) da categoria. Categoria e linha são confirmadas juntas, ou nenhuma é. Depois do commit, o `OutboxRelay`
entrega a linha ao `CategoryProjector`, que grava a foto no Mongo, e apaga a linha.

Há um único tipo de linha, `CATEGORY_CHANGED`, para todas as operações:

| Operação | Foto enviada ao Mongo |
|----------|-----------------------|
| Criar | a categoria nova |
| Editar | a categoria com os dados novos |
| Excluir | a categoria com `deleted = true` |
| Restaurar | a categoria com `deleted = false` |

A foto da exclusão é montada com `deleted = true` à mão, porque a categoria excluída continua com `deleted = false`
na entidade até o `@SQLDelete` rodar no banco.

O relay só apaga a linha depois que o Mongo confirma. Se o serviço cair entre entregar e apagar, a linha é entregue
de novo. Isso não faz mal: o Mongo apenas sobrescreve o documento, então receber a mesma foto duas vezes não muda
nada, e uma foto perdida é corrigida pela seguinte.

O serviço que escreve nunca chama o Mongo direto. O `OutboxService` só aceita ser chamado dentro de uma transação
(`MANDATORY`), e por isso os métodos de `CategoryService` usam `saveAndFlush` antes de registrar a foto: assim o
`updatedAt` e os outros campos já estão preenchidos quando a foto é tirada.

### Outros
- **Exclusão lógica** com `@SQLDelete`, e listagem separada das categorias excluídas.
- **Migrations** versionadas com Flyway (`V1` a `V11`) e `ddl-auto: validate`. A `V10` criou a tabela do Outbox com uma
  coluna `user_id` obrigatória que a aplicação nunca preenche; a `V11` a remove (o dono já vai dentro da foto). A `V10`
  não foi alterada porque o Flyway guarda o checksum das migrations já aplicadas.
- **DTOs** imutáveis (`record`), Bean Validation na borda e MapStruct nos mapeamentos.
- **Segurança:** OAuth2 Resource Server (JWT), com tudo bloqueado por padrão, usando a `JwtSecurityConfig` do módulo
  `shared`: o token precisa ter assinatura e validade corretas e ter sido emitido pelo `issuer-uri` configurado.

---

## 5. Outbox sem Debezium

O `OutboxRelay` (classe genérica do módulo `shared`, usada também pelo `account`, `creditcard` e `transaction`) roda
dentro do próprio serviço, numa thread única, e lê a tabela em dois momentos:

- **Logo depois de cada commit** que grava no Outbox (`@TransactionalEventListener(AFTER_COMMIT)`).
- **Numa varredura de segurança**, na inicialização e a cada 30 segundos, que recolhe o que sobrou se o Mongo estava
  fora do ar ou se o serviço caiu logo após um commit.

Se o Mongo falha, a linha continua pendente e só as próximas da mesma categoria esperam por ela, para manter a
ordem das fotos. Cada linha presa é tentada de novo na varredura seguinte. O motivo de não usar o Debezium está no
README do `account`.

---

## 6. Configuração

Variáveis de ambiente (arquivo `.env` na raiz do projeto):

| Variável | Para que serve |
|----------|----------------|
| `DB_CATEGORY_URL`, `DB_CATEGORY_USERNAME`, `DB_CATEGORY_PASSWORD` | PostgreSQL |
| `MONGODB_URI`, `MONGODB_DATABASE` | MongoDB (lado de leitura) |
| `KAFKA_BOOTSTRAP_SERVER` | Kafka |
| `REDIS_HOST`, `REDIS_PORT` | Redis |
| `KC_AUTH_SERVER_URL`, `KC_EXTERNAL_AUTH_SERVER_URL`, `KC_REALM` | validação do JWT |

> As categorias criadas antes do Outbox existir estão só no Postgres. Elas só aparecem no `GET` depois de serem
> alteradas (o que gera uma foto) ou de uma carga manual no Mongo.

## 7. Como rodar e testar

```bash
# sobe o serviço junto com as dependências
docker compose up -d category-server

# testes do módulo
./mvnw -pl category -am test
```

Os testes usam H2 (perfil `test`) com o Flyway ligado e `ddl-auto: validate`, e não dependem de Postgres, Mongo,
Kafka nem Redis.

Para acompanhar o Outbox, procure no log as linhas que começam com `Outbox:` e `Categoria ... sincronizada no Mongo`.
