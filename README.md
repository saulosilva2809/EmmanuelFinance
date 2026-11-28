# EmmanuelFinance

Sistema de finanças pessoais construído em microsserviços: contas, categorias, cartões de crédito, faturas, receitas,
despesas e transferências.

> Projeto de estudo, em desenvolvimento. Ainda não há interface gráfica: o uso é pela API.

## Funcionalidades

- Cadastro e autenticação de usuários.
- Contas bancárias, poupança, investimento e dinheiro, com saldo atualizado pelas movimentações.
- Categorias de receita e de despesa.
- Cartões de crédito com limite, compras parceladas, faturas e pagamento total ou parcial.
- Transações à vista ou agendadas.
- Transferências entre contas.
- Exclusão com restauração em todos os cadastros financeiros.

**Planejado:** painel de indicadores, transações recorrentes, orçamentos e relatórios.

## Serviços

| Serviço | Responsabilidade | Porta |
|---------|------------------|-------|
| `auth` | usuários e login | 8000 |
| `account` | contas e saldos | 8002 |
| `category` | categorias | 8004 |
| `creditcard` | cartões, faturas e pagamentos | 8005 |
| `transaction` | transações e transferências | 8006 |
| `dashboard` | painel (ainda sem funcionalidades) | 8003 |

Os módulos `shared` e `config` reúnem o código comum. O Keycloak atende na porta 8001.

## Tecnologias

Java 21, Spring Boot 3.3, Maven multi-módulo, PostgreSQL com Flyway, MongoDB, Apache Kafka, Redis, Keycloak (OAuth2
com JWT), OpenFeign, MapStruct, Lombok, JUnit e Docker Compose.

## Arquitetura

- **Um banco por serviço.** Os serviços conversam por Kafka (eventos) e por HTTP (consultas), sem acessar o banco um
  do outro.
- **CQRS** nos serviços financeiros: a escrita vai para o PostgreSQL, e a leitura vem do MongoDB.
- **Transactional Outbox:** o que precisa acontecer depois de salvar (publicar um evento, atualizar a leitura) é
  registrado na mesma transação do dado e entregue em seguida, sem se perder se algo estiver fora do ar.
- **Consistência entre serviços:** eventos repetidos são ignorados; quando uma operação falha, ela é tentada de novo,
  enviada para uma fila de mensagens mortas e desfeita no serviço de origem.
- **Cache** no Redis para as consultas.
- **Segurança:** todo acesso exige um token emitido pelo Keycloak, e cada pessoa só enxerga os próprios dados.

```
 cliente ──HTTP + token──▶ serviço ──▶ PostgreSQL (dado + outbox)
                                              │
                                              ├──▶ MongoDB (leitura)
                                              └──▶ Kafka ──▶ outros serviços
```

## Como rodar

### Pré-requisitos

- Docker com Docker Compose
- Java 21 (só para rodar um serviço fora do Docker ou rodar os testes)
- Bancos PostgreSQL e MongoDB para os serviços que não têm banco no Compose

### Configuração

Crie um arquivo `.env` na raiz com as variáveis usadas no [docker-compose.yml](docker-compose.yml). Os serviços
`creditcard` e `transaction` rodam fora do Compose e recebem as variáveis indicadas no `application.yaml` de cada um.
Nunca versione o `.env`.

No Keycloak, crie o realm e o client usados nas variáveis `KC_REALM`, `KC_CLIENT_ID` e `KC_CLIENT_SECRET`.

### Subir a infraestrutura e os serviços do Compose

```bash
docker compose up -d redis keycloak kafka db-auth auth-server account-server category-server
```

O código é compilado dentro do Docker e as imagens são reconstruídas a cada subida, então não é preciso gerar os
pacotes antes. A primeira vez demora alguns minutos, enquanto as dependências são baixadas. Ao iniciar, cada serviço
aplica as suas migrations no banco configurado.

Dentro do Compose os serviços usam o Kafka em `kafka:29092`; quem roda fora dele usa `localhost:9092`.

### Rodar um serviço fora do Compose

Com as variáveis de ambiente definidas na IDE ou no terminal:

```bash
./mvnw install -DskipTests
```

```bash
./mvnw -pl transaction spring-boot:run
```

## Testes

Os testes usam banco em memória e não dependem da infraestrutura.

```bash
./mvnw test
```

```bash
./mvnw -pl account -am test
```
