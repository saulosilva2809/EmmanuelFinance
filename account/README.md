# Account Service

Microsserviço de **contas financeiras**. Cuida do cadastro, da consulta, da edição e da exclusão (com restauração)
das contas do usuário, e mantém o saldo de cada uma atualizado a partir das transações publicadas pelos outros
serviços.

- Porta local: `8002` (no Docker, `8002:8080`)
- Stack: Java 21, Spring Boot 3.3, PostgreSQL (Supabase), MongoDB (Atlas), Kafka, Redis e Keycloak (JWT)

---

## 1. Responsabilidades

| O serviço faz | O serviço não faz |
|---------------|-------------------|
| CRUD das contas do usuário autenticado | Registrar transações (serviço `transaction`) |
| Exclusão lógica e restauração | Cartões de crédito (serviço `creditcard`) |
| Atualizar o saldo a partir dos eventos de transação | Autenticar usuários (serviço `auth` e Keycloak) |
| Avisar os outros serviços quando uma conta é criada, excluída ou restaurada | |
| Expor endpoints internos para os outros serviços | |

### Modelo da conta

| Campo | Descrição |
|-------|-----------|
| `name` | nome da conta |
| `type` | `CHECKING`, `SAVINGS`, `INVESTMENT` ou `CASH` |
| `bank` | banco (enum compartilhado, no módulo `shared`) |
| `initialBalance` / `currentBalance` | saldo inicial e saldo atual (o atual começa igual ao inicial) |
| `deleted` | exclusão lógica |
| `createdAt` / `updatedAt` | datas de criação e de atualização |

---

## 2. API

Todos os endpoints exigem JWT (Keycloak), e o dono da conta é sempre o usuário do token.

### Pública (`/accounts`)

| Método | Rota | O que faz |
|--------|------|-----------|
| `POST` | `/accounts` | cria a conta (`201`) |
| `GET` | `/accounts/{id}` | busca uma conta |
| `GET` | `/accounts?name=&type=&page=&size=&sort=` | lista com filtros e paginação (padrão: `createdAt` decrescente) |
| `GET` | `/accounts/deleted` | lista as contas excluídas |
| `PUT` | `/accounts/{id}` | altera nome e/ou tipo |
| `DELETE` | `/accounts/{id}` | exclui logicamente (`204`) |
| `POST` | `/accounts/restore/{id}` | restaura uma conta excluída (`204`, ou `409` se ela não estiver excluída) |

### Interna (`/internal/accounts`, usada entre serviços)

| Método | Rota | O que faz |
|--------|------|-----------|
| `GET` | `/internal/accounts/summary/{id}` | resumo da conta (inclui as excluídas) |
| `GET` | `/internal/accounts/internal-summary/{id}` | resumo com banco e saldo |
| `GET` | `/internal/accounts/{accountId}/ownership` | informa se a conta pertence ao usuário do token |

---

## 3. Arquitetura

Escrita e leitura são separadas (CQRS), e um Outbox garante que o que acontece depois de salvar a conta não se
perde.

```
                    ESCRITA                                              LEITURA
 POST/PUT/DELETE ──▶ AccountService ──┐                       GET ──▶ AccountQueryService ──▶ MongoDB
                                      │ (uma transação)
                                      ▼
                         ┌── account (Postgres)
                         └── outbox_event (Postgres)
                                      │ commit
                                      ▼
                                 OutboxRelay
                     ┌────────────────┼─────────────────┐
                     ▼                ▼                 ▼
            Kafka: account-events  AccountProjector  AccountCache
            (shared, creditcard...)   ──▶ MongoDB      ──▶ Redis

 Kafka (transaction-*) ──▶ AccountListener ──▶ AccountBalanceService ──▶ atualiza o saldo (mesmo caminho acima)
```

### Pacotes

| Pacote | Papel |
|--------|-------|
| `controllers` | camada HTTP: validação de entrada e códigos de resposta |
| `services` | regras de escrita: `AccountService`, `AccountBalanceService`, `AccountInternalService`, `AccountValidatorService` |
| `query` | lado de leitura: `AccountQueryService` (consultas), `AccountSpecification` (filtros da listagem), `AccountDocument` (modelo do Mongo) e `AccountProjector` (grava no Mongo) |
| `outbox` | `OutboxService` (registra), `OutboxEventFactory` (monta as linhas), `AccountOutboxDispatcher` (entrega cada linha ao destino), `OutboxEvent` (registro da tabela) e `OutboxConfig` (liga o relay) |
| `kafka` | `AccountProducer` (publica os avisos da conta no Kafka, esperando a confirmação), consumo dos eventos de transação e tratamento de erros |
| `dto`, `enums`, `exceptions` | contratos de entrada e saída, tipos e erros de domínio |

---

## 4. Conceitos aplicados

### CQRS
Ler e escrever têm necessidades diferentes, então cada lado usa seu próprio banco:

- **Escrita:** PostgreSQL, com transações e controle de concorrência.
- **Leitura:** MongoDB, com um documento por conta já no formato que a tela precisa e um índice em
  `userId + deleted + createdAt`.

O `GET` nunca consulta o Postgres. Isso traz consistência eventual: logo depois de um `POST`, a conta pode levar
alguns instantes para aparecer na leitura.

### Transactional Outbox
Salvar no Postgres e depois avisar o Kafka e o Mongo são escritas em sistemas diferentes, e nenhuma transação
cobre todos eles (o problema conhecido como *dual write*). Se o Kafka cair depois do commit, a conta existe e
ninguém é avisado. Se o commit falhar depois do aviso, o aviso é sobre uma conta que não existe.

A solução é gravar, na mesma transação da conta, linhas na tabela `outbox_event` descrevendo o que ainda precisa
acontecer. Conta e linhas são confirmadas juntas, ou nenhuma é. Depois do commit, o `OutboxRelay` entrega cada
linha ao destino e a apaga.

Cada linha tem um único destino, e uma operação registra até três, porque os destinatários precisam de coisas
diferentes:

Os tipos de linha são `ACCOUNT_CREATED`, `ACCOUNT_DELETED` e `ACCOUNT_RESTORED` (aviso para o Kafka, um por evento),
`ACCOUNT_CHANGED` (foto para o Mongo), `ACCOUNT_OWNER_CACHED` e `ACCOUNT_OWNER_REMOVED` (gravar e tirar o dono da
conta no Redis). O envio ao Kafka não fica
no dispatcher: ele chama o `AccountProducer`, que espera a confirmação do Kafka (até 5 segundos) e deixa o erro
subir, e assim a linha continua pendente se o Kafka falhar.

| Operação | Aviso para o Kafka (`account-events`) | Foto da conta para o Mongo | Dono da conta no Redis |
|----------|---------------------------------------|----------------------------|------------------------|
| Criar | `CREATED` | sim | sim, grava a chave |
| Editar | não | sim | não |
| Excluir | `DELETED` | sim, com `deleted = true` | sim, remove a chave |
| Restaurar | `RESTORE` | sim, com `deleted = false` | não |
| Saldo alterado | não | sim | não |

- O **aviso** é curto (`accountId`, `userId` e `status`) e é o contrato com outros serviços, como o `creditcard`,
  que desativa os cartões de uma conta excluída.
- A **foto** (*snapshot*) é a conta inteira naquele instante. O Mongo apenas sobrescreve o documento, então receber a
  mesma foto duas vezes não muda nada, e uma foto perdida é corrigida pela seguinte.
- O **dono da conta** é gravado no Redis (`AccountCache`) para que os outros serviços validem a posse sem
  consultar o `account`. Como passa pelo Outbox, só é gravado depois do commit e é repetido se o Redis estiver fora
  do ar. Na exclusão, o próprio Outbox remove a chave (`ACCOUNT_OWNER_REMOVED`), sem passar pelo Kafka. Por garantia,
  a chave também expira em 24 horas; depois de expirar, o validador consulta o `account` e grava de novo.

O relay só apaga a linha depois que o destino confirma. Se o serviço cair entre entregar e apagar, a linha é
entregue de novo. Por isso os destinos aceitam repetição: o Mongo e o Redis gravam por cima, e os consumidores do
Kafka só marcam ou removem dados.

### Cache (Redis)
Há dois tipos de cache, e é importante não confundi-los, porque cada um é apagado de um jeito:

| Cache | Chave no Redis | Quem grava | Validade | Quem apaga |
|-------|----------------|------------|----------|------------|
| Dono da conta | `account:<id>` | Outbox (`AccountCache`) e o validador de posse do `shared` | 24 h | o Outbox (`ACCOUNT_OWNER_REMOVED`), na exclusão |
| Resposta de `GET /accounts/{id}` | `account_view::<userId>:<id>` | `AccountQueryService` | 10 h | o `AccountProjector` |
| Respostas de `GET /accounts` e `/accounts/deleted` | `account_list::<ativas ou excluídas>:<userId>:<filtros>:<página>` | `AccountQueryService` | 10 h | o `AccountProjector` (apaga todas as listas) |
| Resumos da conta nos outros serviços | `accounts::<id>` e `accounts-internal::<id>` | `AccountClientCacheService` (`shared`), no `creditcard` e no `transaction` | 10 h | o `AccountProjector` |

- **Por que a limpeza fica no `AccountProjector`:** ele roda depois do commit, quando o Outbox entrega a foto da conta
  ao Mongo, e isso acontece em toda mudança (criar, editar, excluir, restaurar e alterar o saldo). Limpar o cache antes
  disso deixaria uma leitura recolocar o dado antigo no cache. Se o Mongo estiver fora do ar, a limpeza espera junto
  com a foto e acontece quando ela for entregue.
- **O dono entra na chave** do `view` e das listas, para que um usuário nunca receba a resposta em cache de outro.
- **Por que as listas são apagadas todas de uma vez:** não dá para saber quais filtros e páginas incluem a conta que
  mudou. Em troca, o cache das listas é descartado a cada mudança em qualquer conta.
- A validade de 10 horas (`RedisConfig`, no módulo `config`) vale para todos os caches do Spring. A resposta não encontrada
  (404) não é guardada.

### Event-driven com Kafka
- **Publica** `account-events` (`CREATED`, `DELETED` e `RESTORE`).
- **Consome** `transaction-created-topic`, `invoice-payment-created-topic`, `transaction-updated-topic`,
  `transaction-deleted-topic` e `transaction-restore-topic` para recalcular o saldo.
- Quando o consumo falha, a mensagem é tentada mais 3 vezes, com 2 segundos entre elas, e depois vai para a DLQ
  (`<tópico>.DLT`).

### Outros
- **Exclusão lógica** com `@SQLDelete`, e listagem separada das contas excluídas.
- **Controle otimista** (`@Version`): dois ajustes simultâneos no mesmo saldo não se sobrescrevem em silêncio.
- **Migrations** versionadas com Flyway (`V1` a `V5`) e `ddl-auto: validate`.
- **DTOs** imutáveis (`record`), Bean Validation na borda e MapStruct nos mapeamentos.
- **Segurança:** OAuth2 Resource Server (JWT), com tudo bloqueado por padrão. A configuração é a `JwtSecurityConfig` do
  módulo `shared` (sem CSRF, sem sessão): o token precisa ter assinatura e validade corretas e ter sido emitido pelo
  `issuer-uri` configurado. Só o `auth` tem uma configuração própria, porque libera o login.

---

## 5. Outbox sem Debezium

O jeito mais comum de entregar um Outbox é com o Debezium, que lê o log de transações do Postgres (WAL) e publica
no Kafka sem precisar consultar a tabela. Ele não foi usado aqui por causa do banco:

- Ler o WAL exige uma conexão direta com o Postgres. A replicação lógica não passa pelo pooler (Supavisor), que é
  o endereço que a aplicação usa.
- No Supabase, o endereço direto (`db.<projeto>.supabase.co`) só tem IPv6, a menos que se contrate o add-on de
  IPv4, que o plano atual não inclui. Sem isso, o Debezium não alcança o banco.
- Subir um Postgres local só para ter replicação não resolve, porque os dados estão no Supabase.

### O que foi usado no lugar

O `OutboxRelay` (classe genérica do módulo `shared`, usada também pelo `creditcard`) roda dentro do próprio serviço, numa thread única, e lê a tabela em dois momentos:

- **Logo depois de cada commit** que grava no Outbox, por meio de um `@TransactionalEventListener(AFTER_COMMIT)`.
  A entrega acontece em milissegundos e só depois que a transação foi confirmada.
- **Numa varredura de segurança**, na inicialização e a cada 30 segundos. Ela recolhe o que sobrou se o Kafka ou o
  Mongo estavam fora do ar, ou se o serviço caiu logo após um commit.

Assim não há consulta a cada segundo: em repouso é uma a cada 30 segundos, e no uso normal a entrega é disparada
pelo próprio commit. Como uma única thread processa tudo na ordem de criação, as mensagens de uma mesma conta
nunca se invertem.

Se um destino falha (por exemplo, o Mongo fora do ar), a linha continua pendente e só as próximas do mesmo destino
e da mesma conta esperam por ela, para manter a ordem. As linhas dos outros destinos seguem normalmente: um Mongo
fora do ar não trava os avisos do Kafka. Cada linha presa é tentada de novo na varredura seguinte.

---

## 6. Configuração

Variáveis de ambiente (arquivo `.env` na raiz do projeto):

| Variável | Para que serve |
|----------|----------------|
| `DB_ACCOUNT_URL`, `DB_ACCOUNT_USERNAME`, `DB_ACCOUNT_PASSWORD` | PostgreSQL |
| `MONGODB_URI`, `MONGODB_DATABASE` | MongoDB |
| `KAFKA_BOOTSTRAP_SERVER` | Kafka |
| `REDIS_HOST`, `REDIS_PORT` | Redis |
| `KC_AUTH_SERVER_URL`, `KC_EXTERNAL_AUTH_SERVER_URL`, `KC_REALM` | validação do JWT |
| `USER_SERVICE_URL` | serviço de usuários |

## 7. Como rodar e testar

```bash
# sobe o serviço junto com as dependências (Kafka, Redis, Keycloak)
docker compose up -d account-server

# testes do módulo
./mvnw -pl account -am test
```

Para acompanhar o Outbox, procure no log as linhas que começam com `Outbox:`. Crie uma conta e veja o registro, o
commit, a entrega ao Kafka e a sincronização com o Mongo. Para ver que nada se perde, derrube o Kafka
(`docker stop kafka`), altere uma conta, suba o Kafka de novo e espere até 30 segundos: a mensagem pendente é
entregue sozinha.
