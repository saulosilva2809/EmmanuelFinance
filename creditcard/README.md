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
- **Pagar a fatura:** total ou parcial, e só se o saldo da conta cobrir o valor pago. O pagamento total paga o que
  ainda falta (o total menos o que já foi pago). No pagamento parcial o valor precisa ser maior que zero e não pode
  passar do que falta pagar (senão `DATA_INVALID_VALUE`, 400). Os pagamentos se somam em `amountPaid`; a fatura vira
  `PAID` quando o valor pago chega ao total e continua `OPEN` enquanto falta. O limite é devolvido na mesma medida,
  sem passar do limite total do cartão. Cada pagamento sai com um `paymentId` próprio, que o serviço `transaction`
  usa para registrar o pagamento uma única vez (sem confundir dois pagamentos da mesma fatura).

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
| `POST` | `/credit-card/{creditCardId}/payment/{invoiceId}/partial` | paga parte da fatura (corpo com `accountId` e `amount`) |

### Interna (`/internal/credit-card`, usada entre serviços)

| Método | Rota | O que faz |
|--------|------|-----------|
| `GET` | `/internal/credit-card/summary/{id}` | resumo do cartão |
| `GET` | `/internal/credit-card/internal-summary/{id}` | resumo com conta, limite disponível e dias de fechamento e vencimento |

---

## 3. Arquitetura

Escrita e leitura de cartões, faturas e itens de fatura são separadas (CQRS), e um Outbox garante que o que acontece
depois de salvar não se perde.

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
              ┌──────────────┬────┴─────────┬──────────────────┐
              ▼              ▼              ▼                  ▼
   CreditCardOutbox-   InvoiceOutbox-  InvoiceItemOutbox-  PaymentOutbox-
   Dispatcher          Dispatcher      Dispatcher          Dispatcher
   (cartão ─▶ Mongo)   (fatura ─▶ Mongo) (item ─▶ Mongo)   (pagamento ─▶ Kafka)

 (a leitura de faturas e itens vem do Mongo: InvoiceQueryService e InvoiceItemQueryService)

 Chamadas entre serviços (/internal/...) ──▶ CreditCardInternalService ──▶ Postgres (nunca o Mongo)
```

### Pacotes

| Pacote | Papel |
|--------|-------|
| `creditcard` | o cartão: `controllers`, `services` (escrita), `kafka` (consumo dos eventos), `query` (leitura no Mongo), `dto` e `exceptions` |
| `creditcard.query` | `CreditCardQueryService` (consultas), `CreditCardSpecification` (filtros), `CreditCardDocument` (modelo do Mongo) e `CreditCardProjector` (grava no Mongo) |
| `invoice` | faturas e itens (escrita no Postgres): controllers, serviços, repositórios e `InvoiceMapper` |
| `invoice.query.invoice` | `InvoiceQueryService` (consulta as faturas), `InvoiceSpecification` (filtros), `InvoiceDocument` e `InvoiceProjector` |
| `invoice.query.invoiceitem` | o mesmo para os itens: `InvoiceItemQueryService`, `InvoiceItemSpecification`, `InvoiceItemDocument` e `InvoiceItemProjector` |
| `payment` | pagamento da fatura e o produtor Kafka do evento de pagamento |
| `outbox` | `OutboxService` (registra), `OutboxEventFactory` (monta as linhas), um dispatcher por destino (entregam), `OutboxEvent` e `OutboxConfig`; os tipos de evento ficam em `outbox.enums`, um enum por assunto |

---

## 4. Conceitos aplicados

### CQRS (cartões, faturas e itens de fatura)
- **Escrita:** PostgreSQL, com transações e controle de concorrência.
- **Leitura:** MongoDB, um documento por registro, em três coleções: `credit_card`, `invoice` e `invoice_item`.
  `GET /credit-card`, `GET /credit-card/invoices` e `GET /credit-card/invoice-item/{invoiceId}` nunca consultam o
  Postgres. A lista de itens também lê a fatura do Mongo, para mostrar o resumo dela e conferir que é do usuário.

Isso traz consistência eventual: logo depois de criar ou alterar um registro, a leitura pode levar alguns instantes
para refletir a mudança.

### Transactional Outbox
Salvar no Postgres e depois atualizar o Mongo ou avisar o Kafka são escritas em sistemas diferentes, e nenhuma
transação cobre todos eles. Para não perder nada, o serviço grava, na mesma transação do dado, uma linha na tabela
`outbox_event` descrevendo o que ainda precisa acontecer. Depois do commit, o `OutboxRelay` (classe genérica do
módulo `shared`) entrega cada linha ao destino e a apaga. Se o destino estiver fora do ar, a linha continua pendente
e é tentada de novo.

Há um único Outbox no serviço (uma tabela, uma entidade, um `OutboxService`), com quatro tipos de linha, um enum
por assunto:

| Tipo | Destino | Conteúdo |
|------|---------|----------|
| `CREDIT_CARD_CHANGED` | MongoDB | foto completa do cartão naquele instante (`deleted = true` quando excluído) |
| `INVOICE_CHANGED` | MongoDB | foto completa da fatura |
| `INVOICE_ITEM_CHANGED` | MongoDB | foto completa do item da fatura |
| `INVOICE_PAYMENT` | Kafka (`created-invoice-payment-topic`) | dados do pagamento da fatura |

O tipo é gravado como texto na coluna `type`. Por isso o relay pergunta a cada dispatcher `supports(tipo)` e a
comparação é feita pelo nome do tipo (`OutboxEventType.is`), não por `equals`.

O que registra cada linha:

| Operação | Cartão | Fatura | Item | Pagamento (Kafka) |
|----------|--------|--------|------|-------------------|
| Criar, editar, excluir ou restaurar cartão | sim | não | não | não |
| Conta excluída (desativa os cartões dela) | sim, um por cartão | não | não | não |
| Compra criada (muda o limite, cria ou soma a fatura, cria os itens) | sim | sim | sim | não |
| Compra excluída (devolve o limite, subtrai da fatura, exclui os itens) | sim | sim; `deleted = true` se a fatura zerou | sim, com `deleted = true` | não |
| Compra restaurada | sim | sim | sim | não |
| Pagar a fatura (fatura vira `PAID`, devolve o limite) | sim | sim | não | sim |

Como a foto é sempre o registro inteiro, o Mongo só precisa sobrescrever o documento: receber a mesma foto duas
vezes não muda nada, e uma foto perdida é corrigida pela seguinte. O relay entrega "pelo menos uma vez": se o
serviço cair entre entregar e apagar a linha, ela é entregue de novo.

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
- **Exclusão lógica** com `@SQLDelete` para cartões, faturas e itens, e listagem separada dos cartões excluídos.
- **Migrations** versionadas com Flyway (`V1` a `V11`) e `ddl-auto: validate`.
- **DTOs** imutáveis (`record`), Bean Validation na borda e MapStruct nos mapeamentos.
- **Segurança:** OAuth2 Resource Server (JWT), com tudo bloqueado por padrão. A configuração é a `JwtSecurityConfig` do
  módulo `shared` (sem CSRF, sem sessão): o token precisa ter assinatura e validade corretas e ter sido emitido pelo
  `issuer-uri` configurado. Só o `auth` tem uma configuração própria, porque libera o login.
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
