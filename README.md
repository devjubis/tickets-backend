# Sistema de Gerenciamento de Chamados - Backend

API RESTful responsável pelo gerenciamento de chamados (tickets), desenvolvida com arquitetura limpa e práticas modernas.

## Funcionalidades

- Criação e gerenciamento do ciclo de vida de Chamados
- Persistência relacional robusta e escalável
- Versionamento automatizado de banco de dados via Flyway
- Arquitetura "Package-by-Context"
- Configuração baseada em variáveis de ambiente (12-Factor App)

## Requisitos

- Java 21 (Eclipse Temurin)
- Docker e Docker Compose
- Git

## Setup

### 1. Clonar o repositório

```bash
git clone [https://github.com/devjubis/tickets-backend.git](https://github.com/devjubis/tickets-backend.git)
cd tickets-backend
```

### 2. Configurar variáveis de ambiente

```bash
cp .env.example .env
# O .env padrão já contém as credenciais locais para o banco de dados
```

### 3. Iniciar o Banco de Dados localmente

```bash
docker compose up -d
```

### 4. Rodar a aplicação (Desenvolvimento)

```bash
./mvnw spring-boot:run
```

## Scripts Disponíveis

| Script | Descrição |
| :--- | :--- |
| `./mvnw spring-boot:run` | Inicia a aplicação em modo de desenvolvimento |
| `./mvnw clean package` | Compila o projeto e gera o arquivo .jar para produção |
| `./mvnw test` | Executa testes unitários e de integração |
| `docker compose up -d` | Inicia o container do PostgreSQL em background |
| `docker compose down` | Para e remove o container do banco de dados |

## Estrutura do Projeto

```text
tickets-backend/
├── src/
│   ├── main/
│   │   ├── java/com/devjubis/tickets/   # Módulo principal
│   │   │   └── TicketsApplication.java  # Entry point
│   │   └── resources/
│   │       ├── application.properties   # Configurações do Spring
│   │       └── db/migration/            # Scripts SQL do Flyway
│   └── test/                            # Testes automatizados
├── .mvn/                                # Maven Wrapper
├── docker-compose.yml                   # Configuração do banco local
├── .env.example                         # Template de variáveis
├── pom.xml                              # Dependências do projeto
└── PLANO-MODERNIZACAO.md                # Doc. de arquitetura
```

## API Endpoints

### Auth
*(Endpoints de autenticação serão documentados aqui futuramente)*

### Tickets
- `POST /tickets` - Criar chamado
- `GET /tickets` - Listar chamados

## Swagger

Documentação estará disponível em: `será colocado aqui futuramente` 
