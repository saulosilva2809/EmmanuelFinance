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

* **Register (Cadastro):**
    * *Regra de Negócio:* Senhas devem obrigatoriamente passar por um algoritmo de hashing adaptável de alto custo (como Argon2id ou bcrypt com fator de custo mínimo de 12) antes de tocar o banco de dados. O sistema valida de forma estrita a unicidade do e-mail e higieniza os dados de entrada.
    * *Integração Kafka:* Dispara o evento `UserCreatedEvent` para o tópico `auth.events`, permitindo que outros microsserviços provisionem dados iniciais (como categorias padrão) de forma assíncrona.
* **Login (Autenticação):**
    * *Regra de Negócio:* Implementação obrigatória de Rate Limiting a nível de servidor por IP e por conta (máximo de 5 tentativas por minuto). O bloqueio de conta é temporário após falhas sucessivas (proteção contra brute-force). Emite tokens JWT com tempo de expiração curto (ex: 15 minutos) e Refresh Tokens rotativos persistidos em cache de sessão.
* **Recover Password (Recuperação de Senha):**
    * *Regra de Negócio:* Geração de um token criptográfico de uso único (OTP) com expiração rígida de 10 minutos. O envio do link ou código de recuperação deve ser despachado em background para não travar a thread de resposta da API.
* **Me (Validação de Perfil):**
    * *Regra de Negócio:* Interceptado no API Gateway para validação em tempo real de tokens na lista de revogação (blacklist) antes que a requisição seja encaminhada para o serviço.

### 2.2 Dashboard & Analytics Service
Microsserviço especializado puramente em leitura e agregação de dados de alta performance. Ele nunca escreve no banco de dados transacional.

* **Métricas Gerais do Sistema:**
    * *Regra de Negócio:* É terminantemente **proibido** executar queries agregadas (`SUM`, `COUNT`, `GROUP BY`) diretamente no banco de dados relacional de transações em tempo real. Este microsserviço mantém sua própria base de dados otimizada para leitura. Ele escuta continuamente o tópico `transaction.events` do Kafka e atualiza Visões Materializadas e agregados de forma assíncrona. O cache de resposta de endpoints do Dashboard deve ter expiração mínima de 60 segundos para evitar sobrecarga.

### 2.3 Core Ledger & Transact Service
O núcleo transacional do sistema. Garante consistência absoluta de saldo, extratos e a conformidade das regras contábeis.

* **Accounts (CRUD + Filtros de URL):** ✅
    * *Regra de Negócio:* Uma conta bancária nunca pode ser removida fisicamente do banco de dados se possuir qualquer transação histórica atrelada (aplicação obrigatória de *Soft Delete*). Filtros de URL para busca de contas devem passar por sanitização restrita contra injeções.
* **Categories (CRUD + Filtros de URL):**
    * *Regra de Negócio:* Suporta árvore recursiva de categorias e subcategorias. O limite de aninhamento rígido no servidor é de até 3 níveis para prevenir estouro de pilha (stack overflow) e lentidão em consultas recursivas.
* **Transactions (CRUD + Filtros de URL):**
    * *Regra de Negócio:* Cada alteração contábil dispara um mecanismo de *Pessimistic Locking* (trava de linha) no registro da conta correspondente para recalcular e atualizar o saldo atual (`current_balance`). Alterações retroativas que superem 90 dias são bloqueadas nativamente, exigindo privilégios especiais de auditoria.
    * *Integração Kafka:* Toda criação, edição ou deleção envia imediatamente mensagens detalhadas para o tópico `transaction.events`.
* **Transfers (CRUD + Filtros de URL):**
    * *Regra de Negócio:* A transferência interna exige atomicidade absoluta no nível de isolamento `SERIALIZABLE` do banco de dados. O débito na conta de origem e o crédito na conta de destino precisam ocorrer sob o mesmo bloco transacional. Qualquer inconsistência ou falha no meio do processo força um `Rollback` completo para evitar duplicação ou desaparecimento de dinheiro.
* **Credit Card (CRUD + Filtros de URL):**
    * *Regra de Negócio:* Despesas lançadas no cartão reduzem o limite disponível imediatamente. Se uma nova despesa estourar o limite aprovado somado a uma margem de segurança configurada (ex: 10%), o servidor rejeita a transação respondendo com código HTTP 422 (Unprocessable Entity).
* **Recurring Transactions (CRUD + Filtros de URL):**
    * *Regra de Negócio:* Orquestrado por um agendador interno cron (Workers dedicados). Diariamente na madrugada, o servidor avalia o campo `next_execution_date`. Se coincidir com o dia atual, gera de forma automatizada a transação no banco e avança a data do próximo ciclo. Em caso de indisponibilidade ou falhas, o microsserviço aplica uma política de reconfiguração de tentativas (Retry) com espaçamento de tempo exponencial.

### 2.4 Budget & Planning Service
Responsável pelo monitoramento de metas de teto de gastos configuradas pelo usuário.

* **Budgets (CRUD + Filtros de URL):**
    * *Regra de Negócio:* Este serviço não consulta o banco do Core Ledger. Ele consome as mensagens do tópico `transaction.events` do Kafka, atualizando de forma assíncrona o total gasto acumulado de cada orçamento ativo no mês (`current_spent`).
    * *Integração Kafka:* Quando o consumo atinge os gatilhos de 80% e 100% estabelecidos pelo usuário, este serviço publica uma mensagem de alerta no tópico `budget.alerts`.

### 2.5 Export & Report Service
Microsserviço totalmente isolado e desacoplado, projetado para tarefas pesadas que exigem alto consumo de memória e CPU.

* **Exportadores (XLS, CSV, PDF, JSON):**
    * *Regra de Negócio:* Qualquer solicitação de geração de relatório contendo grandes volumes de dados ou filtros temporais amplos é estritamente **assíncrona**. A API recebe o comando, insere um job no tópico `report.requests` do Kafka e devolve instantaneamente um status HTTP 202 (Accepted) para o cliente. O worker processa os dados utilizando buffers de stream (evitando carregar milhões de linhas na memória RAM do servidor simultaneamente). O documento final é carregado em um armazenamento de objetos e um link seguro temporário (Pre-signed URL) com validade de 1 hora é gerado e disponibilizado para download.

### 2.6 AI Engine & Gateway
Centraliza o processamento inteligente do sistema, inteligência conversacional e conexões com canais terceiros.

* **Categorização Automática:**
    * *Regra de Negócio:* Ao receber descrições textuais cruas (vinda de importações manuais ou integrações automáticas), o servidor limpa o texto, remove caracteres especiais e aciona algoritmos de similaridade vetorial. A categoria só é aplicada automaticamente se o nível de confiança (confidence score) ultrapassar 85%. Caso contrário, a transação é criada com a tag "Não Categorizado" para revisão humana.
* **Chat e Relatórios Personalizados:**
    * *Regra de Negócio:* Adota estritamente o padrão RAG (Retrieval-Augmented Generation). Antes de despachar qualquer prompt para serviços externos de IA, o servidor intercepta a requisição, extrai via microsserviço de Analytics apenas os dados financeiros consolidados e anonimizados do usuário daquela sessão e anexa isso rigidamente como contexto seguro e delimitado no prompt.
* **Integração IA + WhatsApp:**
    * *Regra de Negócio:* O servidor expõe endpoints específicos de Webhook para receber os payloads da API oficial do WhatsApp. Cada requisição externa deve ter sua assinatura criptográfica validada na entrada do servidor para garantir autenticidade. Áudios recebidos passam por um pipeline de conversão para texto (Speech-to-Text). O motor de IA extrai três variáveis obrigatórias: `descrição`, `valor` e `data`. Uma transação provisória é estruturada no servidor e enviada de volta ao WhatsApp na forma de botões interativos de confirmação. A transação só é publicada e enviada ao barramento do Kafka após o clique de confirmação explícito do usuário.

---

## 3. Diretrizes de Comunicação e Segurança Global

1.  **Paginação e Filtros Dinâmicos de URL:** Todas as rotas de listagem de todos os microsserviços implementam obrigatoriamente paginação baseada no servidor (máximo de 100 registros por bloco). Filtros dinâmicos como intervalos de datas são validados contra schemas estritos na entrada do servidor antes de serem convertidos em consultas ao banco, eliminando vulnerabilidades de injeção.
2.  **Segurança Inter-serviços (Zero Trust):** A comunicação interna direta (síncrona) quando necessária ocorre via mTLS (Mutual TLS). Os microsserviços analíticos, de IA e de exportação não possuem, sob nenhuma hipótese, privilégios ou credenciais de gravação nas tabelas de banco de dados do `Core Ledger`. Toda e qualquer mutação de estado financeiro deve ser solicitada de forma padronizada através dos contratos expostos e autenticados do núcleo financeiro ou disparada por eventos validados.
