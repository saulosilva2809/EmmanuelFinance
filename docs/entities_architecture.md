# Arquitetura de Entidades (Data Model) - EmmanuelFinance

Este documento descreve a modelagem de entidades relacional para o EmmanuelFinance. O modelo foi projetado utilizando UUIDs como chaves primárias para garantir a escalabilidade, independência de ambiente e segurança dos dados, sendo ideal para bancos de dados como PostgreSQL ou CockroachDB.

---

## 1. Módulo de Autenticação e Usuário

### Entity: `User`
Centraliza as credenciais de acesso, informações de segurança e metadados do usuário. É a entidade raiz do sistema.

| Atributo | Tipo | Restrições | Descrição |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PK, Unique | Identificador único universal do usuário. |
| `name` | String(150) | Not Null | Nome completo do usuário. |
| `email` | String(255) | Not Null, Unique | Email utilizado para login e notificações. |
| `password_hash` | String(255) | Not Null | Hash seguro da senha (ex: BCrypt/Argon2). |
| `phone_number` | String(20) | Unique, Nullable | Número de telefone (formato E.164) para integração com WhatsApp. |
| `mfa_secret` | String(128) | Nullable | Chave secreta para Autenticação de Dois Fatores (2FA/TOTP). |
| `created_at` | Timestamp | Not Null, Default NOW | Data e hora de criação da conta. |
| `updated_at` | Timestamp | Not Null, Default NOW | Data e hora da última alteração dos dados. |

---

## 2. Módulo de Contas e Cartões

### Entity: `Account` ✅
Representa qualquer local onde o usuário possui saldo ou movimentação financeira líquida (contas correntes, poupanças, carteiras físicas ou de investimento).

| Atributo | Tipo | Restrições | Descrição |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PK, Unique | Identificador único da conta. |
| `user_id` | UUID | FK (`User.id`), Not Null | Usuário proprietário da conta. |
| `name` | String(100) | Not Null | Nome customizado (ex: "Itaú Corrente", "Wallet BTC"). |
| `type` | Enum | Not Null | Tipo da conta: `CHECKING`, `SAVINGS`, `INVESTMENT`, `CASH`. |
| `currency` | String(3) | Not Null, Default 'BRL'| Código ISO da moeda da conta. |
| `initial_balance`| Decimal(15,2) | Not Null, Default 0.00| Saldo inicial cadastrado pelo usuário. |
| `current_balance`| Decimal(15,2) | Not Null, Default 0.00| Saldo atualizado em tempo real pelas transações. |
| `created_at` | Timestamp | Not Null, Default NOW | Data de criação do registro. |

### Entity: `CreditCard` ✅
Entidade especializada para cartões de crédito. Ao invés de saldo líquido, gerencia limites, datas de fechamento e vencimento de faturas.

| Atributo | Tipo | Restrições | Descrição |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PK, Unique | Identificador único do cartão. |
| `account_id` | UUID | FK (`Account.id`), Not Null | Conta bancária de onde sairá o débito para pagar a fatura. |
| `name` | String(100) | Not Null | Nome do cartão (ex: "Nubank Ultravioleta"). |
| `limit` | Decimal(15,2) | Not Null | Limite total de crédito aprovado. |
| `closing_day` | Integer | Not Null (1-31) | Dia do mês em que a fatura fecha. |
| `due_day` | Integer | Not Null (1-31) | Dia do mês em que a fatura vence. |
| `created_at` | Timestamp | Not Null, Default NOW | Data de criação do registro. |

---

## 3. Módulo de Categorias e Orçamentos

### Entity: `Category` ✅
Estrutura em árvore (auto-relacionamento) para permitir categorias e subcategorias ilimitadas para classificação de fluxos.

| Atributo | Tipo | Restrições | Descrição |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PK, Unique | Identificador único da categoria. |
| `user_id` | UUID | FK (`User.id`), Not Null | Usuário proprietário da categoria. |
| `name` | String(100) | Not Null | Nome da categoria (ex: "Alimentação", "Supermercado"). |
| `icon` | String(50) | Nullable | String identificadora do ícone no frontend. |
| `color` | String(7) | Nullable | Código Hexadecimal da cor para UI (ex: `#FF5733`). |
| `parent_id` | UUID | FK (`Category.id`), Nullable| ID da categoria pai (se for uma subcategoria). |
| `type` | Enum | Not Null | Natureza do fluxo: `INCOME` (Receita) ou `EXPENSE` (Despesa). |

### Entity: `Budget`
Define limites financeiros e metas de teto de gastos por categorias e períodos específicos.

| Atributo | Tipo | Restrições | Descrição |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PK, Unique | Identificador único do orçamento. |
| `user_id` | UUID | FK (`User.id`), Not Null | Usuário proprietário do orçamento. |
| `category_id` | UUID | FK (`Category.id`), Not Null | Categoria monitorada pelo teto de gastos. |
| `amount_limit` | Decimal(15,2) | Not Null | Valor máximo estipulado para gastos. |
| `current_spent` | Decimal(15,2) | Not Null, Default 0.00| Valor total já consumido no período atual. |
| `period` | Enum | Not Null | Periodicidade: `MONTHLY`, `WEEKLY`, `YEARLY`. |
| `start_date` | Date | Not Null | Data de início da vigência do orçamento. |
| `end_date` | Date | Not Null | Data de término da vigência do orçamento. |

---

## 4. Módulo de Movimentações Financeiras

### Entity: `Transaction`
O registro principal de movimentação financeira. Pode ser associado diretamente a uma conta líquida ou a um cartão de crédito.

| Atributo | Tipo | Restrições | Descrição |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PK, Unique | Identificador único da transação. |
| `user_id` | UUID | FK (`User.id`), Not Null | Usuário dono da transação. |
| `account_id` | UUID | FK (`Account.id`), Nullable | Conta de débito/crédito (Preenchido se não for cartão). |
| `credit_card_id` | UUID | FK (`CreditCard.id`), Nullable| Cartão de crédito (Preenchido se for compra no cartão). |
| `category_id` | UUID | FK (`Category.id`), Not Null | Classificação categórica da transação. |
| `recurring_id` | UUID | FK (`RecurringTransaction.id`), Nullável| Vinculado se a transação foi gerada por uma recorrência. |
| `description` | String(255) | Not Null | Descrição ou histórico da transação. |
| `amount` | Decimal(15,2) | Not Null | Valor financeiro da transação. |
| `date` | Date | Not Null | Data de competência do evento financeiro. |
| `status` | Enum | Not Null | Estado atual da transação: `PAID` (Efetivada) ou `PENDING`. |
| `type` | Enum | Not Null | Tipo de fluxo: `INCOME` ou `EXPENSE`. |
| `ai_categorized` | Boolean | Not Null, Default FALSE | Flag indicativo se a transação foi categorizada via IA. |
| `created_at` | Timestamp | Not Null, Default NOW | Data de criação do log de transação. |

### Entity: `Transfer`
Gerencia a transferência interna de valores entre contas do mesmo usuário, sem afetar o resultado global de receitas/despesas.

| Atributo | Tipo | Restrições | Descrição |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PK, Unique | Identificador único da transferência. |
| `user_id` | UUID | FK (`User.id`), Not Null | Usuário que executou a transferência. |
| `source_account_id`| UUID | FK (`Account.id`), Not Null | Conta de origem de onde o dinheiro sai. |
| `destination_account_id`| UUID | FK (`Account.id`), Not Null| Conta de destino onde o dinheiro entra. |
| `amount` | Decimal(15,2) | Not Null | Valor total transferido. |
| `date` | Date | Not Null | Data de execução da transferência. |
| `description` | String(255) | Nullable | Observação adicional sobre a transferência. |
| `created_at` | Timestamp | Not Null, Default NOW | Data de criação do registro. |

### Entity: `RecurringTransaction`
Templates e agendamentos configurados para gerar transações de forma automatizada pela engine cron do sistema.

| Atributo | Tipo | Restrições | Descrição |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PK, Unique | Identificador único do agendamento recorrente. |
| `user_id` | UUID | FK (`User.id`), Not Null | Usuário dono da recorrência. |
| `account_id` | UUID | FK (`Account.id`), Nullable | Conta padrão para as transações geradas. |
| `credit_card_id` | UUID | FK (`CreditCard.id`), Nullable| Cartão padrão para as transações geradas. |
| `category_id` | UUID | FK (`Category.id`), Not Null | Categoria padrão do lançamento. |
| `description` | String(255) | Not Null | Descrição padrão do lançamento. |
| `amount` | Decimal(15,2) | Not Null | Valor padrão a ser lançado. |
| `frequency` | Enum | Not Null | Frequência: `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`. |
| `start_date` | Date | Not Null | Data do primeiro lançamento automático. |
| `end_date` | Date | Nullable | Data limite para o encerramento da recorrência. |
| `next_execution_date`| Date | Not Null | Próxima data agendada para o gatilho rodar. |

---

## 5. Módulo de Inteligência Artificial e Log

### Entity: `AiChatSession`
Agrupa mensagens trocadas com o assistente inteligente para isolar o histórico e manter a memória contextual de curto prazo.

| Atributo | Tipo | Restrições | Descrição |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PK, Unique | Identificador único da sessão de chat. |
| `user_id` | UUID | FK (`User.id`), Not Null | Usuário participando da sessão. |
| `platform` | Enum | Not Null | Canal de origem: `WEB` ou `WHATSAPP`. |
| `created_at` | Timestamp | Not Null, Default NOW | Data e hora de início da sessão. |

### Entity: `AiChatMessage`
Registros granulares das mensagens enviadas e recebidas dentro de um contexto de atendimento com IA.

| Atributo | Tipo | Restrições | Descrição |
| :--- | :--- | :--- | :--- |
| `id` | UUID | PK, Unique | Identificador único da mensagem. |
| `session_id` | UUID | FK (`AiChatSession.id`), Not Null| Sessão de contexto à qual a mensagem pertence. |
| `sender` | Enum | Not Null | Origem da mensagem: `USER` ou `AI`. |
| `content` | Text | Not Null | Conteúdo textual da mensagem ou prompt retornado. |
| `created_at` | Timestamp | Not Null, Default NOW | Data e hora exata do envio/resposta. |

---

## 🚀 Estratégia de Indexação Recomendada para URLs & Filtros

Para garantir que os filtros dinâmicos de URL (Query Params) rodem em milissegundos mesmo com milhões de registros por usuário, crie os seguintes Índices Compostos (Composite Indexes):

1. **`idx_transactions_query`**: `Transaction(user_id, date, category_id)`
   - *Motivo:* Acelera instantaneamente as telas de extrato filtradas por período e categoria.
2. **`idx_transactions_account`**: `Transaction(account_id, date)`
   - *Motivo:* Otimiza a renderização de extratos específicos por conta bancária.
3. **`idx_budgets_lookup`**: `Budget(user_id, start_date, end_date)`
   - *Motivo:* Permite calcular se o usuário estourou as metas mensais na home do dashboard de forma performática.
