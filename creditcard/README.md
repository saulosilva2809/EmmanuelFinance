# Credit Card Service

Microsserviço de **cartões de crédito e faturas**. Cuida do cadastro dos cartões, do limite disponível, das
faturas (geradas a partir das compras feitas no cartão) e do pagamento da fatura com o saldo de uma conta.

- Porta local: `8005`
- Stack: Java 21, Spring Boot 3.3, PostgreSQL (Neon), MongoDB (Atlas), Kafka, Redis e Keycloak (JWT)
- Não está no `docker-compose.yml`: roda pelo IntelliJ ou pelo Maven

---

## 1. Responsabilidades

| O serviço faz | O serviço não faz |
|---------------|-------------------|
| CRUD dos cartões do usuário autenticado | Registrar a transação da compra (serviço `transaction`) |
| Exclusão lógica e restauração de cartões | Guardar o saldo das contas (serviço `account`) |
| Controlar o limite disponível a cada compra | Autenticar usuários (serviço `auth` e Keycloak) |
| Gerar as faturas e os itens (parcelas) das compras | |
| Pagar a fatura e devolver o limite | |
| Expor endpoints internos para os outros serviços | |

### Modelo de dados

**Cartão** (`credit_card`)

| Campo | Descrição |
|-------|-----------|
| `accountId` | conta que paga a fatura |
| `name`, `bank` | nome e banco do cartão |
| `creditLimit` | limite total |
| `availableLimit` | limite ainda disponível (começa igual ao limite total) |
| `closingDay`, `dueDay` | dia de fechamento e dia de vencimento |
| `deleted` | exclusão lógica |

**Fatura** (`invoice`)

| Campo | Descrição |
|-------|-----------|
| `creditCardId`, `month`, `year` | cartão e mês da fatura |
| `totalAmount` | soma dos itens |
| `amountPaid` | valor já pago |
| `status` | `OPEN`, `CLOSED` ou `PAID` |

**Item da fatura** (`invoice_item`): uma parcela de uma compra (`transactionId`, `installmentNumber`,
`totalInstallments` e `amount`).

### Regras de negócio

- **Criar cartão:** a conta precisa ser do usuário e o banco do cartão precisa ser o mesmo da conta. O limite
  disponível começa igual ao limite total.
- **Editar o limite:** o limite disponível muda pela diferença entre o limite novo e o antigo. Nenhum dos dois pode
  ficar negativo.
- **Restaurar:** só vale para cartão excluído e cuja conta não esteja excluída.
- **Compra no cartão:** o valor sai do limite disponível. Uma compra parcelada em `n` vezes vira `n` itens, e a
  parcela `i` cai na fatura do mês da compra mais `i` meses (o valor é dividido com 2 casas decimais).
- **Pagar a fatura:** só o pagamento total, e só se o saldo da conta cobrir a fatura. A fatura vira `PAID`, e o limite
  é devolvido, sem passar do limite total do cartão.

---

## 2. API

Todos os endpoints exigem JWT (Keycloak), e o dono do cartão é sempre o usuário do token.

### Cartões (`/credit-card`)

| Método | Rota | O que faz |
|--------|------|-----------|
| `POST` | `/credit-card` | cria o cartão |
| `GET` | `/credit-card/{id}` | busca um cartão |
| `GET` | `/credit-card?accountId=&name=&bank=&page=&size=&sort=` | lista com filtros e paginação |
| `GET` | `/credit-card/deleted` | lista os cartões excluídos |
| `PUT` | `/credit-card/{id}` | altera o cartão (inclusive o limite) |
| `DELETE` | `/credit-card/{id}` | exclui logicamente |
| `POST` | `/credit-card/restore/{id}` | restaura um cartão excluído |

### Faturas e pagamento

| Método | Rota | O que faz |
|--------|------|-----------|
| `GET` | `/credit-card/invoices` | lista as faturas, com filtros e paginação |
| `GET` | `/credit-card/invoice-item/{invoiceId}` | lista os itens de uma fatura |
| `POST` | `/credit-card/{creditCardId}/payment/{invoiceId}/total` | paga a fatura inteira |

### Interna (`/internal/credit-card`, usada entre serviços)

| Método | Rota | O que faz |
|--------|------|-----------|
| `GET` | `/internal/credit-card/summary/{id}` | resumo do cartão |
| `GET` | `/internal/credit-card/internal-summary/{id}` | resumo com conta, limite disponível e dias de fechamento e vencimento |

---

## 3. Arquitetura

Escrita e leitura dos cartões são separadas (CQRS), e um Outbox garante que o que acontece depois de salvar não se
perde. As faturas e os pagamentos ficam só no Postgres.

```
                    ESCRITA                                                LEITURA
 POST/PUT/DELETE ──▶ CreditCardService ──┐                      GET ──▶ CreditCardQueryService ──▶ MongoDB
 transação (Kafka) ─▶ ...BalanceKafkaService ┤ (uma transação)
 pagamento ────────▶ PaymentService ────┘
                                      ▼
                         ┌── credit_card, invoice... (Postgres)
                         └── outbox_event (Postgres)
                                      │ commit
                                      ▼
                                 OutboxRelay
                         ┌────────────┴─────────────┐
                         ▼                          ▼
              CreditCardOutboxDispatcher   PaymentOutboxDispatcher
              (foto do cartão ──▶ MongoDB)   (pagamento ──▶ Kafka)

 Chamadas entre serviços (/internal/...) ──▶ CreditCardInternalService ──▶ Postgres (nunca o Mongo)
```

### Pacotes

| Pacote | Papel |
|--------|-------|
| `creditcard` | o cartão: `controllers`, `services` (escrita), `kafka` (consumo dos eventos), `query` (leitura no Mongo), `dto` e `exceptions` |
| `creditcard.query` | `CreditCardQueryService` (consultas), `CreditCardSpecification` (filtros), `CreditCardDocument` (modelo do Mongo) e `CreditCardProjector` (grava no Mongo) |
| `invoice` | faturas e itens: controllers, serviços, repositórios e filtros |
| `payment` | pagamento da fatura e o produtor Kafka do evento de pagamento |
| `outbox` | `OutboxService` (registra), `OutboxEventFactory` (monta as linhas), os dispatchers (entregam), `OutboxEvent` e `OutboxConfig` |

---

## 4. Conceitos aplicados

### CQRS (só para os cartões)
- **Escrita:** PostgreSQL, com transações e controle de concorrência.
- **Leitura:** MongoDB, um documento por cartão (coleção `credit_card`), com índice em
  `userId + deleted + createdAt`. O `GET /credit-card` nunca consulta o Postgres.

Isso traz consistência eventual: logo depois de criar ou alterar um cartão, a leitura pode levar alguns instantes
para refletir a mudança.

### Transactional Outbox
Salvar no Postgres e depois atualizar o Mongo ou avisar o Kafka são escritas em sistemas diferentes, e nenhuma
transação cobre todos eles. Para não perder nada, o serviço grava, na mesma transação do dado, uma linha na tabela
`outbox_event` descrevendo o que ainda precisa acontecer. Depois do commit, o `OutboxRelay` (classe genérica do
módulo `shared`) entrega cada linha ao destino e a apaga. Se o destino estiver fora do ar, a linha continua pendente
e é tentada de novo.

Há um único Outbox no serviço, com dois tipos de linha:

| Tipo | Destino | Conteúdo |
|------|---------|----------|
| `CREDIT_CARD_CHANGED` | MongoDB | foto completa do cartão naquele instante (`deleted = true` quando excluído) |
| `INVOICE_PAYMENT` | Kafka (`created-invoice-payment-topic`) | dados do pagamento da fatura |

O que registra cada linha:

| Operação | Foto do cartão (Mongo) | Pagamento (Kafka) |
|----------|------------------------|-------------------|
| Criar cartão | sim | não |
| Editar cartão (inclui o limite) | sim | não |
| Excluir cartão | sim, com `deleted = true` | não |
| Restaurar cartão | sim | não |
| Conta excluída (desativa os cartões dela) | sim, uma por cartão | não |
| Compra criada, excluída ou restaurada (muda o limite) | sim | não |
| Pagar a fatura (devolve o limite) | sim | sim |

Como a foto é sempre o cartão inteiro, o Mongo só precisa sobrescrever o documento: receber a mesma foto duas vezes
não muda nada, e uma foto perdida é corrigida pela seguinte. O relay entrega "pelo menos uma vez": se o serviço
cair entre entregar e apagar a linha, ela é entregue de novo.

### Chamadas entre serviços leem do Postgres
O Mongo recebe os dados pelo Outbox, então ele pode estar atrasado. Os endpoints `/internal/...` são usados pelos
outros serviços para **decidir** (por exemplo, se há limite para uma compra), por isso o
`CreditCardInternalService` lê sempre do Postgres. Um teste garante que ele não dependa de nenhuma classe do lado de
leitura (`query`). A regra vale para consultas que mostram dados (Mongo) versus operações que tomam decisões
(Postgres).

### Event-driven com Kafka

| Tópico | Direção | O que faz |
|--------|---------|-----------|
| `account-events` | consome | quando uma conta é excluída (`DELETED`), desativa os cartões dela |
| `transaction-created-topic` | consome | compra no cartão: tira do limite disponível e gera os itens da fatura |
| `transaction-deleted-topic` | consome | devolve o limite e remove os itens da fatura |
| `transaction-restore-topic` | consome | tira o limite de novo e restaura os itens |
| `transaction-failed-topic` | publica | avisa que não foi possível processar a transação (compensação no `transaction`) |
| `created-invoice-payment-topic` | publica (pelo Outbox) | o `transaction` registra a transação do pagamento e publica `invoice-payment-created-topic`, que o `account` consome para descontar o saldo |

### Outros
- **Exclusão lógica** com `@SQLDelete` para cartões e faturas, e listagem separada dos excluídos.
- **Migrations** versionadas com Flyway (`V1` a `V11`) e `ddl-auto: validate`.
- **DTOs** imutáveis (`record`), Bean Validation na borda e MapStruct nos mapeamentos.
- **Segurança:** OAuth2 Resource Server (JWT), com tudo bloqueado por padrão.
- **Cache no Redis** dos resumos de conta e de cartão que os serviços consultam entre si.

---

## 5. Configuração

Variáveis de ambiente (nas *run configurations* do IntelliJ):

| Variável | Para que serve |
|----------|----------------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL |
| `MONGODB_URI`, `MONGODB_DATABASE` | MongoDB |
| `KAFKA_BOOTSTRAP_SERVER` | Kafka |
| `REDIS_HOST`, `REDIS_PORT` | Redis |
| `KC_AUTH_SERVER_URL`, `KC_EXTERNAL_AUTH_SERVER_URL`, `KC_REALM` | validação do JWT |
| `ACCOUNT_SERVICE_URL`, `CREDIT_CARD_SERVICE_URL`, `TRANSACTION_SERVICE_URL` | chamadas para os outros serviços |

## 6. Como rodar e testar

```bash
# testes do módulo
./mvnw -pl creditcard -am test
```

Os testes usam H2 e não dependem de Postgres, Mongo, Kafka nem Redis.

Para acompanhar o Outbox, procure no log as linhas que começam com `Outbox:`. Altere o limite de um cartão e veja o
registro, o commit e a sincronização com o Mongo. Para ver que nada se perde, tire o Mongo do ar (ou bloqueie o seu
acesso), altere um cartão e volte o acesso: a linha pendente é entregue na próxima varredura, em até 30 segundos.
