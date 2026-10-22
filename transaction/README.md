# Transaction Service

Microsserviço de **transações financeiras**: receitas e despesas pagas na conta ou no cartão de crédito, à vista,
parceladas ou agendadas. É ele que avisa os outros serviços (conta e cartão) para acertarem o saldo, o limite e a
fatura.

- Porta local: `8006`
- Stack: Java 21, Spring Boot 3.3, PostgreSQL (Neon), MongoDB (Atlas), Kafka, Redis e Keycloak (JWT)
- Não está no `docker-compose.yml`: roda pelo IntelliJ ou pelo Maven

---

## 1. Responsabilidades

| O serviço faz | O serviço não faz |
|---------------|-------------------|
| CRUD das transações do usuário autenticado | Guardar o saldo das contas (serviço `account`) |
| Exclusão lógica e restauração | Controlar limite e faturas do cartão (serviço `creditcard`) |
| Agendar transações para uma data futura e executá-las | Cadastrar as categorias (serviço `category`) |
| Evitar transação duplicada (idempotência) | Autenticar usuários (serviço `auth` e Keycloak) |
| Avisar os outros serviços de cada mudança (Kafka) | |
| Criar a transação do pagamento de uma fatura | |
| Expor um resumo da transação para outros serviços | |

### Modelo de dados

**Transação** (`transaction`)

| Campo | Descrição |
|-------|-----------|
| `accountId` | conta onde a transação acontece |
| `creditCardId` | cartão usado (vazio se for pago direto na conta) |
| `categoryId` | categoria (vazia no pagamento de fatura) |
| `description`, `amount` | descrição e valor |
| `installmentsCount` | número de parcelas (só para cartão; 1 é à vista) |
| `scheduled`, `date` | se é agendada e para quando |
| `status` | `PENDING` (agendada, aguardando a data), `PAID` ou `FAILED` |
| `type` | `INCOME` (receita) ou `EXPENSE` (despesa) |
| `idempotencyKey` | chave que impede criar a mesma transação duas vezes |
| `deleted` | exclusão lógica |

### Regras de negócio

- **Criar:** a conta precisa ser do usuário; a categoria precisa ser do mesmo tipo da transação (receita ou despesa);
  despesa em conta precisa de saldo suficiente.
- **Cartão:** só aceita despesa, o valor precisa caber no limite disponível, o cartão precisa ser da conta informada
  e só o cartão aceita parcelas. Transação de cartão não pode ser alterada depois de criada.
- **Agendada:** precisa de data, e a data não pode estar no passado. Nasce `PENDING` e vira `PAID` quando a data chega.
  Transação não agendada não aceita data e nasce `PAID`.
- **Só a transação `PAID` avisa os outros serviços.** A agendada avisa quando for executada.
- **Restaurar:** só vale para transação excluída (e, se for de cartão, o limite precisa comportar o valor de novo).
- **Idempotência:** a mesma transação (mesmo usuário, conta, valor, data e descrição) não é criada duas vezes em
  2 minutos. A chave é um hash dos dados e fica travada no Redis; a chave também é gravada na transação.

---

## 2. API

Todos os endpoints exigem JWT (Keycloak), e o dono da transação é sempre o usuário do token.

### Transações (`/transactions`)

| Método | Rota | O que faz |
|--------|------|-----------|
| `POST` | `/transactions` | cria a transação |
| `GET` | `/transactions/{id}` | busca uma transação |
| `GET` | `/transactions?accountId=&categoryId=&creditCardId=&greaterValueThan=&valueLessThan=&scheduled=&date=&status=&type=&page=&size=&sort=` | lista com filtros e paginação |
| `GET` | `/transactions/deleted` | lista as transações excluídas |
| `PUT` | `/transactions/{id}` | altera a transação (não vale para cartão) |
| `DELETE` | `/transactions/{id}` | exclui logicamente |
| `POST` | `/transactions/restore/{id}` | restaura uma transação excluída |

### Interna (`/internal/transactions`, usada entre serviços)

| Método | Rota | O que faz |
|--------|------|-----------|
| `GET` | `/internal/transactions/summary/{id}` | resumo da transação (categoria, valor, status e tipo) |

---

## 3. Arquitetura

Escrita e leitura são separadas (CQRS), e um Outbox garante que o que acontece depois de salvar não se perde:
atualizar o Mongo e avisar os outros serviços pelo Kafka.

```
                    ESCRITA                                              LEITURA
 POST/PUT/DELETE ──▶ TransactionService ──┐                 GET ──▶ TransactionQueryService ──▶ MongoDB
 execução agendada ─▶ ...ExecutionService ┤ (uma transação)
 falha / pagamento ─▶ Listener Kafka ─────┘
                                   ▼
                      ┌── transaction (Postgres)
                      └── outbox_event (Postgres)
                                   │ commit
                                   ▼
                              OutboxRelay
                      ┌────────────┴─────────────┐
                      ▼                          ▼
        TransactionOutboxDispatcher   TransactionEventOutboxDispatcher
        (foto da transação ──▶ MongoDB)   (aviso ──▶ Kafka)

 Chamadas entre serviços (/internal/...) ──▶ TransactionServiceInternal ──▶ Postgres (nunca o Mongo)
```

### Pacotes

| Pacote | Papel |
|--------|-------|
| `transaction` | a entidade, o mapper, o repositório, o selector e a recuperação das agendadas ao iniciar |
| `controllers`, `dtos`, `exceptions` | entrada HTTP, objetos de entrada e saída e erros de domínio |
| `services` | regras: `TransactionService` (escrita), `TransactionValidatorService`, `IdempotencyService`, `TransactionEventsService` (monta os avisos) e `TransactionServiceInternal` |
| `services.scheduler` | agendamento e execução das transações futuras |
| `query` | `TransactionQueryService` (consultas), `TransactionSpecification` (filtros), `TransactionDocument` (modelo do Mongo), `TransactionProjector` (grava no Mongo) e `TransactionMongoConfig` |
| `kafka` | `TransactionProducer` (publica) e `TransactionListener` (consome) |
| `outbox` | `OutboxService` (registra), `OutboxEventFactory` (monta as linhas), os dispatchers (entregam), `OutboxEvent` e `OutboxConfig`; os tipos de evento ficam em `outbox.enums`, um enum por assunto |

---

## 4. Conceitos aplicados

### CQRS
- **Escrita:** PostgreSQL, com transações e controle de concorrência.
- **Leitura:** MongoDB, um documento por transação (coleção `transaction`), com índice em
  `userId + deleted + createdAt`. `GET /transactions` nunca consulta o Postgres.

Isso traz consistência eventual: logo depois de criar ou alterar uma transação, a leitura pode levar alguns
instantes para refletir a mudança.

O valor (`amount`) é guardado no Mongo como `Decimal128`, e não como texto (o padrão do Spring para `BigDecimal`).
Texto não compara como número (`"100"` fica maior que `"1000"`), e os filtros de valor mínimo e máximo precisam
comparar de verdade. A conversão está em `TransactionMongoConfig`.

### Transactional Outbox
Salvar no Postgres e depois atualizar o Mongo ou avisar o Kafka são escritas em sistemas diferentes, e nenhuma
transação cobre todos eles. Para não perder nada, o serviço grava, na mesma transação do dado, uma linha na tabela
`outbox_event` descrevendo o que ainda precisa acontecer. Depois do commit, o `OutboxRelay` (classe genérica do
módulo `shared`) entrega cada linha ao destino e a apaga. Se o destino estiver fora do ar, a linha continua pendente
e é tentada de novo.

Há um único Outbox no serviço, com dois assuntos (dois enums) e seis tipos de linha:

| Tipo | Destino | Conteúdo |
|------|---------|----------|
| `TRANSACTION_CHANGED` | MongoDB | foto completa da transação naquele instante (`deleted = true` quando excluída) |
| `TRANSACTION_CREATED` | Kafka (`transaction-created-topic`) | transação paga criada |
| `INVOICE_PAYMENT_CREATED` | Kafka (`invoice-payment-created-topic`) | transação criada pelo pagamento de uma fatura |
| `TRANSACTION_UPDATED` | Kafka (`transaction-updated-topic`) | valores de antes e de depois da alteração |
| `TRANSACTION_DELETED` | Kafka (`transaction-deleted-topic`) | transação paga excluída |
| `TRANSACTION_RESTORED` | Kafka (`transaction-restore-topic`) | transação paga restaurada |

O tipo é gravado como texto na coluna `type`. Por isso o relay pergunta a cada dispatcher `supports(tipo)` e a
comparação é feita pelo nome do tipo (`OutboxEventType.is`), não por `equals`.

O que registra cada linha:

| Operação | Foto (Mongo) | Aviso (Kafka) |
|----------|--------------|---------------|
| Criar transação | sim | `TRANSACTION_CREATED`, se estiver `PAID` |
| Alterar transação | sim | `TRANSACTION_UPDATED`, se estiver `PAID` |
| Excluir transação | sim, com `deleted = true` | `TRANSACTION_DELETED`, se estava `PAID` |
| Restaurar transação | sim | `TRANSACTION_RESTORED`, se estava `PAID` |
| Executar transação agendada (vira `PAID`) | sim | `TRANSACTION_CREATED` |
| Falha avisada pelo cartão (vira `FAILED`) | sim | não |
| Pagamento de fatura recebido | sim | `INVOICE_PAYMENT_CREATED` |

Como a foto é sempre a transação inteira, o Mongo só precisa sobrescrever o documento: receber a mesma foto duas
vezes não muda nada, e uma foto perdida é corrigida pela seguinte. O relay entrega "pelo menos uma vez": se o
serviço cair entre entregar e apagar a linha, ela é entregue de novo. O `TransactionProducer` espera a confirmação do
Kafka (até 5 segundos) e deixa o erro subir, e assim a linha continua pendente se o Kafka falhar.

### Chamadas entre serviços leem do Postgres
O Mongo recebe os dados pelo Outbox, então ele pode estar atrasado. O endpoint `/internal/...` é usado pelos outros
serviços, por isso o `TransactionServiceInternal` lê sempre do Postgres. Um teste garante que ele não dependa de
nenhuma classe do lado de leitura (`query`).

### Event-driven com Kafka

| Tópico | Direção | O que faz |
|--------|---------|-----------|
| `transaction-created-topic` | publica (pelo Outbox) | conta e cartão acertam saldo, limite e fatura |
| `invoice-payment-created-topic` | publica (pelo Outbox) | a conta desconta o saldo do pagamento da fatura |
| `transaction-updated-topic` | publica (pelo Outbox) | a conta acerta o saldo pela diferença |
| `transaction-deleted-topic` | publica (pelo Outbox) | conta e cartão desfazem o efeito da transação |
| `transaction-restore-topic` | publica (pelo Outbox) | conta e cartão refazem o efeito da transação |
| `transaction-failed-topic` | consome | o cartão avisa que não conseguiu processar; a transação vira `FAILED` |
| `created-invoice-payment-topic` | consome | o cartão avisou que uma fatura foi paga; cria a despesa do pagamento (uma só por fatura) |

### Transações agendadas
Cada transação agendada vira uma tarefa em memória (`ThreadPoolTaskScheduler`, 10 threads) para a data escolhida.
Quando a data chega, a transação vira `PAID` e o aviso sai pelo Outbox. Como a tarefa fica só em memória, ao iniciar
o serviço (`RecoverScheduledTransactions`) as pendentes futuras são agendadas de novo, e as que já passaram da data
são executadas na hora.

### Outros
- **Exclusão lógica** com `@SQLDelete`, e listagem separada das excluídas.
- **Migrations** versionadas com Flyway (`V1` a `V7`) e `ddl-auto: validate`.
- **DTOs** imutáveis (`record`), Bean Validation na borda e MapStruct nos mapeamentos.
- **Segurança:** OAuth2 Resource Server (JWT), com tudo bloqueado por padrão.
- **Cache no Redis** dos resumos de conta, cartão e categoria que os serviços consultam entre si.

---

## 5. Configuração

Variáveis de ambiente (nas *run configurations* do IntelliJ):

| Variável | Para que serve |
|----------|----------------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL |
| `MONGODB_URI`, `MONGODB_DATABASE` | MongoDB |
| `KAFKA_BOOTSTRAP_SERVER` | Kafka |
| `REDIS_HOST`, `REDIS_PORT` | Redis |
| `KC_AUTH_SERVER_URL`, `KC_REALM` | validação do JWT |
| `ACCOUNT_SERVICE_URL`, `CREDIT_CARD_SERVICE_URL`, `CATEGORY_SERVICE_URL` | chamadas para os outros serviços |

## 6. Como rodar e testar

```bash
# testes do módulo
./mvnw -pl transaction -am test
```

Os testes usam H2 e não dependem de Postgres, Mongo, Kafka nem Redis.

Para acompanhar o Outbox, procure no log as linhas que começam com `Outbox:`. Altere o valor de uma transação e veja
o registro, o commit e a sincronização com o Mongo. Para ver que nada se perde, tire o Mongo do ar (ou bloqueie o seu
acesso), altere uma transação e volte o acesso: a linha pendente é entregue na próxima varredura, em até 30 segundos.
