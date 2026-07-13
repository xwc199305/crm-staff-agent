# CRM Staff Agent

A ReAct pattern AI Agent based on Spring Boot + AgentScope Java, providing standard Web API interfaces.

## What is ReAct Agent?

ReAct (Reasoning + Acting) is a classic Agent pattern that solves problems through the following loop:

1. **Reasoning**: Analyze the current state and decide the next action
2. **Acting**: Execute the decided action (call tools)
3. **Observing**: Observe the result of the action
4. **Repeat**: Continue the loop until the task is completed

## Project Features

- ✅ Based on Spring Boot 3.x standard architecture
- ✅ Controller-Service-Repository layered design
- ✅ Unified API response format
- ✅ Global exception handling
- ✅ Based on AgentScope Java 1.0.11
- ✅ Integrated DashScope (Alibaba Cloud Tongyi Qianwen) model
- ✅ RESTful API interfaces
- ✅ Intent recognition system
- ✅ Conversation memory with vector database (Qdrant)
- ✅ RAG (Retrieval-Augmented Generation) support
- ✅ Knowledge base integration with Dify

## Prerequisites

- **JDK 17** or higher
- **Maven 3.6+**
- **DashScope API Key** (Get from [Alibaba Cloud DashScope Console](https://dashscope.console.aliyun.com/apiKey))
- **Optional**: Qdrant vector database for conversation memory

## Quick Start

### 1. Configure API Key

Method 1: Set environment variable (Recommended)
```bash
export DASHSCOPE_API_KEY=your-api-key-here
```

Method 2: Configure application.properties
```properties
agent.api-key=your-api-key-here
```

### 2. Compile Project

```bash
mvn clean compile
```

### 3. Run Project

```bash
mvn spring-boot:run
```

Or:
```bash
mvn clean package
java -jar target/crm-staff-agent-1.0-SNAPSHOT.jar
```

### 4. Use API

After startup, the project runs at `http://localhost:8080` by default.

#### Send Chat Request

```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Hello, please introduce yourself."}'
```

#### Get Agent Name

```bash
curl http://localhost:8080/api/agent/name
```

## Project Structure

```
crm-staff-agent/
├── pom.xml                                    # Maven configuration
├── README.md                                  # This document
├── src/
│   └── main/
│       ├── java/com/example/staffagent/
│       │   ├── StaffAgentApplication.java     # Main entry class
│       │   ├── controller/                    # REST controllers
│       │   ├── service/                       # Service layer
│       │   ├── repository/                    # Data access layer
│       │   ├── model/                         # Entity classes
│       │   ├── dto/                           # Data transfer objects
│       │   ├── config/                        # Configuration classes
│       │   ├── exception/                     # Exception handling
│       │   ├── agent/                         # Agent implementation
│       │   ├── intent/                        # Intent recognition
│       │   ├── handler/                       # Intent handlers
│       │   ├── context/                       # Conversation context
│       │   ├── vector/                        # Vector database
│       │   ├── dify/                          # Dify integration
│       │   └── tool/                          # Tool services
│       └── resources/
│           └── application.properties         # Configuration file
└── openspec/
    └── changes/                               # OpenSpec change documents
```

## API Documentation

### Chat API

**Endpoint**: `POST /api/agent/chat`

**Request Body**:
```json
{
  "message": "Hello, please introduce yourself."
}
```

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": "Hello! I'm React Assistant, a helpful AI assistant..."
}
```

### Get Name API

**Endpoint**: `GET /api/agent/name`

**Response**:
```json
{
  "code": 200,
  "message": "success",
  "data": "React Assistant"
}
```

## Configuration

### application.properties

```properties
# Server port
server.port=8080

# Application name
spring.application.name=staff-agent

# Agent configuration
agent.name=E-commerce Customer Service Assistant
agent.api-key= # Or use environment variable DASHSCOPE_API_KEY

# Intent recognition
intent.confidence-threshold=0.7

# Conversation memory
memory.vector-dimensions=1536
memory.max-history-count=50

# Qdrant configuration
qdrant.host=localhost
qdrant.port=6333
```

## Tech Stack

- **Java 17**
- **Spring Boot 3.2.0** - Web framework
- **AgentScope 1.0.11** - AI Agent framework
- **Qdrant** - Vector database
- **OpenFeign** - HTTP client
- **Maven** - Build tool
- **SLF4J + Logback** - Logging framework

## Related Resources

- [Spring Boot Official Website](https://spring.io/projects/spring-boot)
- [AgentScope Java GitHub](https://github.com/agentscope-ai/agentscope-java)
- [AgentScope Documentation](https://java.agentscope.io/)
- [DashScope API Key Application](https://dashscope.console.aliyun.com/apiKey)
- [Java Development Standards](./.trae/rules/java/README.md)

## License

This project uses the same license as AgentScope.
