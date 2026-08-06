# Especificação de Arquitetura e Regras de Negócio por Microsserviço

Este documento detalha o mapeamento completo do ecossistema de Finanças Avançadas. O sistema é baseado em uma **Arquitetura de Microsserviços**, onde cada componente opera de forma isolada, resiliente e se comunica de maneira assíncrona utilizando o **Apache Kafka** como barramento de eventos (Event Bus).

---

## 1. Visão Geral do Ecossistema e Fluxo de Eventos (Kafka)

Cada microsserviço gerencia sua própria persistência e se comunica com os demais através de tópicos dedicados no Kafka. Isso garante acoplamento zero e alta performance.

* **Tópico `auth.events`:** Publica eventos de criação de conta e alertas de segurança (ex: logins suspeitos).
* **Tópico `transaction.events`:** Canal de alta criticidade. Toda inserção, alteração ou remoção de transação gera um evento aqui para atualizar Dashboards, Budgets e Auditorias.
* **Tópico `budget.alerts`:** Publica mensagens quando um usuário atinge limites críticos de gastos.
* **Tópico `report.requests`:** Filas de processamento assíncrono para geração de arquivos pesados.

---

## 2. Detalhes dos Microsserviços, Features e Regras de Negócio

### 2.1 Auth & Identity Service
Responsável pelo ciclo de vida de segurança, autenticação e controle de acesso dos usuários.

* **Register (Cadastro)**✅ 
* **Login (Autenticação)**✅
* **Recover Password (Recuperação de Senha):**
* **Me (Validação de Perfil)**

### 2.2 Dashboard & Analytics Service
Microsserviço especializado puramente em leitura e agregação de dados de alta performance. Ele nunca escreve no banco de dados transacional.

* **Métricas Gerais do Sistema:**
    * *Regra de Negócio:* É terminantemente **proibido** executar queries agregadas (`SUM`, `COUNT`, `GROUP BY`) diretamente no banco de dados relacional de transações em tempo real. Este microsserviço mantém sua própria base de dados otimizada para leitura. Ele escuta continuamente o tópico `transaction.events` do Kafka e atualiza Visões Materializadas e agregados de forma assíncrona. O cache de resposta de endpoints do Dashboard deve ter expiração mínima de 60 segundos para evitar sobrecarga.

### 2.3 Core Ledger & Transact Service
O núcleo transacional do sistema. Garante consistência absoluta de saldo, extratos e a conformidade das regras contábeis.

* **Accounts (CRUD + Filtros de URL)** ✅
* **Categories (CRUD + Filtros de URL)** ✅
* **Transactions (CRUD + Filtros de URL)**
* **Transfers (CRUD + Filtros de URL)**
* **Credit Card (CRUD + Filtros de URL)**
* **Recurring Transactions (CRUD + Filtros de URL)**

### 2.4 Budget & Planning Service
Responsável pelo monitoramento de metas de teto de gastos configuradas pelo usuário.

* **Budgets (CRUD + Filtros de URL)**

### 2.5 Export & Report Service
Microsserviço totalmente isolado e desacoplado, projetado para tarefas pesadas que exigem alto consumo de memória e CPU.

* **Exportadores (XLS, CSV, PDF, JSON)**

### 2.6 AI Engine & Gateway
Centraliza o processamento inteligente do sistema, inteligência conversacional e conexões com canais terceiros.

* **Categorização Automática**
* **Chat e Relatórios Personalizados**
* **Integração IA + WhatsApp**
