# MARS MCP Tools Review Guidelines

## Overview

This document defines the review standards for all MCP (Model Context Protocol) related PRs within the MARS team. It ensures that our MCP tools and servers are well-structured, agent-friendly, secure, and maintainable.

**Primary reference:** [AWS Labs MCP Design Guidelines](https://github.com/awslabs/mcp/blob/main/DESIGN_GUIDELINES.md)
**Supplementary reference:** [MARS-SDS-0416] MCP Tools Design Principles (internal)

---

# For Author

## Project Structure

MCP servers should follow the standard Maven/Gradle layout:

```
mcp-server-project/
├── README.md
├── CHANGELOG.md
├── pom.xml / build.gradle
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/mars/mcp/yourserver/
│   │   │       ├── YourMcpServerApplication.java
│   │   │       ├── config/
│   │   │       ├── constants/
│   │   │       ├── controller/
│   │   │       ├── model/
│   │   │       ├── service/
│   │   │       ├── exception/
│   │   │       ├── util/
│   │   │       └── validation/
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/
│           └── com/mars/mcp/yourserver/
```

**Key rules:**
- **Single entry point**: Standard Spring Boot `main()` method
- **Separation of concerns**: DTOs, tools, resources, business logic in separate packages
- **Constants**: Dedicated class with `UPPER_SNAKE_CASE`, grouped and documented
- **Package naming**: Only lowercase letters and digits (e.g., `com.mars.mcp.yourserver`)

## Tool Naming Conventions

### Required Rules
- Maximum **64 characters** for fully qualified name
- Must **start with a letter** (a-z, A-Z)
- Use only **alphanumeric, underscores (`_`), or hyphens (`-`)**
- **No spaces, commas, or special characters**
- **Case-sensitive** and **unique within namespace**

### Naming Style

| Style | Example | Status |
|---|---|---|
| `snake_case` | `query_accounts`, `create_case` | Recommended |
| `kebab-case` | `query-accounts`, `create-case` | Accepted |
| `PascalCase` | `QueryAccounts`, `CreateCase` | Accepted |

**Stay consistent within your MCP server. Do not mix naming styles.**

### Best Practices
- Use **descriptive, action-oriented names** following `verb_noun` pattern
- Keep fully qualified name **under 64 characters**
- Reference: [MCP Tool Naming Specification (SEP-986)](https://modelcontextprotocol.io/community/seps/986-specify-format-for-tool-names.md)

## Type Definitions and Data Models

- Use **Java records or POJOs** with comprehensive type annotations
- Define **enums** for constrained values using `UpperCamelCase`
- Include **Bean Validation annotations** (`@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Max`)
- Document models with **detailed Javadoc**
- **Class naming**: `UpperCamelCase` (e.g., `ImageGenerationConfig`)
- **JSON field naming**: `lower_case_underscore` (e.g., `error_reason`)
- **Object names**: Short and concise, avoid unnecessary adjectives

## Tool Parameter Design

- Use `@ToolParam` with clear descriptions
- **Required parameters**: `@NotNull` / `@NotBlank`
- **Optional parameters**: `@Nullable` with sensible defaults
- **Validation**: Bean Validation annotations (`@Min`, `@Max`, `@Size`, `@Pattern`)
- **AI instructions**: Use `CRITICAL` or `IMPORTANT` keywords in descriptions when needed
- **Method naming**: `VerbNoun` pattern in `lowerCamelCase` (e.g., `queryAccounts`)

## Response Design: Envelope Pattern

> **From [MARS-SDS-0416]:** Raw JSON != Agent-friendly interface. Agent needs **stable schema**, not complete schema.

### Why Envelope, Not Raw Passthrough

Tool responses **must not** directly return raw upstream API JSON because:
- **Schema Drift**: Raw API fields change across versions
- **Token Inefficiency**: Excessive irrelevant fields waste LLM tokens
- **Non-recoverable Errors**: Raw errors lack categorization and retry hints
- **Control Plane Gap**: No audit/trace, metering, or policy enforcement
- **LLM Reasoning Mismatch**: LLMs reason better over flat structures

### Envelope Design Principles

1. **Task-oriented, not Data-oriented** - Output describes "what was accomplished"
2. **Family-consistent, not Global-uniform** - Same family shares consistent shapes
3. **Minimal but Sufficient** - Smallest useful field set
4. **Explicit over Implicit** - Success/failure, count/pagination always explicit
5. **Agent-first, not Backend-first** - Design for Agent consumption and chaining
6. **Raw data optional, not default** - Preserve for debugging, never as primary contract

### Envelope Example

```json
{
  "content": [
    {
      "type": "text",
      "text": "Found 2 accounts"
    },
    {
      "type": "json",
      "json": {
        "accounts": [
          { "id": "001", "name": "Acme" },
          { "id": "002", "name": "Beta" }
        ],
        "count": 2,
        "nextPageToken": "abc123",
        "meta": {
          "traceId": "trace-123"
        }
      }
    }
  ]
}
```

### Request and Response Objects

Following MARS Naming Conventions, name request/response objects after method names:

| Method Name | Request Object | Response Object |
|-------------|----------------|-----------------|
| `queryAccounts` | `QueryAccountsRequest` | `QueryAccountsResponse` |
| `createCase` | `CreateCaseRequest` | `CreateCaseResponse` |
| `getCustomer` | `GetCustomerRequest` | `Customer` |

## Error Handling

### Error Handling Guidelines

- Use **try-catch blocks** at tool boundary
- Log exceptions with **appropriate context** (request IDs, operation details)
- Use MCP SDK's error reporting mechanism
- Provide **meaningful error messages**
- Categorize errors: `validation`, `permission`, `retryable`, `not_found`, `rate_limit`
- **Exception class naming**: `UpperCamelCase` with descriptive suffixes

### Recoverable Error Model

> **From [MARS-SDS-0416]:** Agent cannot auto-recover from raw API errors. MCP errors must be classifiable, retryable, and actionable.

- Include **`retryable: true/false`** indicator
- Provide **`suggestedAction`** or **`hint`** field
- **Do not pass through raw upstream error JSON**

## Security Practices

### Input Validation
- Validate all user inputs at **tool parameter boundary** using Bean Validation
- Mitigate **injection risks** (SOQL injection, command injection) with parameterized queries

### Timeouts
- All external API calls must have **configurable timeouts** (connect + read)
- Set **reasonable defaults** in `application.yml`
- Handle timeouts **gracefully** with clear error messages

### General Security
- **No sensitive data leakage** in error messages or logs
- **Clean up resources** (connections, streams, temp files) using try-with-resources

## Concurrency

- Use **reactive/async patterns** (`CompletableFuture`, `Mono`/`Flux`) for non-blocking I/O
- Use **`CompletableFuture.allOf()`** or **`Flux.merge()`** for concurrent operations
- Ensure thread safety for shared state; prefer immutable data structures

## Response Formatting

- Return **JSON-serialized strings** via Jackson `ObjectMapper`
- Use **URI format** for file paths (e.g., `file:///path/to/file`)
- Define **DTOs (records or POJOs)** for consistent response structure

## Logging

- Use **SLF4J + Logback** with configurable log levels
- Log **important operations** at service boundaries
- Include **context** via MDC (request IDs, trace IDs, tool name)
- Use appropriate **log levels**: `DEBUG`, `INFO`, `WARN`, `ERROR`
- **No sensitive data in logs**

## Configuration

- Externalize via **`application.yml`** and **`@ConfigurationProperties`**
- Use **`UPPER_SNAKE_CASE`** for environment variable overrides
- Provide **sensible defaults** for optional configuration
- Document all properties in **README**
- **No hardcoded secrets** - use environment variables, Vault, or secure stores
- **Configuration class naming**: `UpperCamelCase` with `Config` suffix

## Documentation

### Tool and Resource Javadoc

All tool methods must include **comprehensive Javadoc**:
- **Purpose**: What the tool does
- **Usage requirements**: Prerequisites
- **Tips**: How to get best results
- **Output format**: What the response looks like
- **Interpretation guidance**: How to use the results

### README
- Project description and setup instructions
- All available tools and resources
- All configuration properties with descriptions and defaults
- Usage examples

## Testing

- **Unit tests** for service methods, validators, and DTOs (JUnit 5 + Mockito)
- **Integration tests** against mocked upstream services (WireMock / MockServer)
- **Edge case coverage**: empty results, pagination, errors, timeouts, boundaries
- **No real credentials in tests** - use mocked services

## Code Style and Linting

- Follow **MARS Coding Conventions** and **MARS Naming Conventions**
- Use team **IntelliJ Code Formatter**
- Run **Checkstyle** / **SpotBugs** / **PMD** as configured
- Ensure **no compiler warnings** in changed code

---

# For Reviewer

## The Standard of MCP Tool Review

Reviewers have ownership and responsibility over MCP tools they review. Goal: ensure tools are agent-friendly, secure, well-structured, and maintainable.

Favor **approving** PRs that improve overall health, even if not perfect. Seek *continuous improvement*, not perfection.

### Principles
- Technical facts and data overrule opinions
- **This guideline and team naming conventions** are authority for style
- If author demonstrates multiple valid approaches, accept their preference

## What to Look for in an MCP Tool Review

### 1. Project Structure and Code Organization
- Does project follow **standard MCP server layout**?
- Proper **separation of concerns**?
- Constants in dedicated class with `UPPER_SNAKE_CASE`?
- Package names use only lowercase letters and digits?

### 2. Tool Naming and Definitions
- Fully qualified tool name **<= 64 characters**?
- Starts with letter, using only alphanumeric, underscores, or hyphens?
- **Naming style consistent** across all tools?
- Tool names follow **verb-noun pattern**?

### 3. Type Safety and Parameter Design
- Data models use **Java records or well-structured POJOs**?
- Constrained values as **enums** using `UpperCamelCase`?
- Parameters use **`@ToolParam`** with clear descriptions?
- Required: **`@NotNull`/`@NotBlank`**, Optional: **`@Nullable`**?
- **Bean Validation** applied where appropriate?
- AI instructions use **`CRITICAL`/`IMPORTANT`** keywords?
- Method names follow **`VerbNoun` pattern** in `lowerCamelCase`?
- Request/response objects named with **`Request`/`Response` suffix**?
- JSON field names use **`lower_case_underscore`**?
- Class names use **`UpperCamelCase`**?

### 4. Response Design (Envelope) ⚠️ CRITICAL

This is the most critical area per MARS team design principles.

- **No raw API passthrough**: Wrapped in MCP envelope with semantic fields?
- **Task-oriented**: Output describes "what was accomplished"?
- **Stable contract**: Fixed, predictable field patterns?
- **Minimal and bounded**: Unnecessary metadata stripped?
- **Natural-language summary**: Human-readable `text` summary provided?
- **Raw data optional**: Preserved in optional/debug field only?

### 5. Error Handling
- Errors categorized (`validation`, `permission`, `retryable`, `not_found`, `rate_limit`)?
- Retryable errors include **`retryable` indicator** and retry guidance?
- **`suggestedAction`** or **`hint`** field for agent self-healing?
- Raw upstream errors wrapped in MCP error envelope?
- Error messages **meaningful** without leaking sensitive internals?

### 6. Security
- **Timeouts enforced** on all external API calls?
- User inputs **validated at system boundaries**?
- Injection risks mitigated?
- Error messages and logs **avoid exposing** credentials, paths, PII?
- Resources **properly cleaned up** on all code paths?

### 7. Concurrency
- Non-blocking patterns used where applicable?
- Shared state handled in **thread-safe** manner?
- Concurrent operations properly coordinated?

### 8. Logging and Observability
- Structured logging with **SLF4J + Logback**?
- Logs include **operation context** via MDC?
- Log levels **appropriate**?
- Response metadata include **`traceId`** for observability?
- No **sensitive data** in logs?

### 9. Configuration
- Configuration externalized via **`application.yml`** and **`@ConfigurationProperties`**?
- All properties **documented in README**?
- **Sensible defaults** for optional config?
- Credentials from **environment variables or secure stores**?
- Environment variables using **`UPPER_SNAKE_CASE`**?
- Configuration classes named with **`Config` suffix**?

### 10. Documentation
- Tool methods have **comprehensive Javadoc**?
- MCP server includes **instructions block** for consuming LLM?
- MCP resources include **example response and usage guidance**?
- **README** complete (setup, tools, configuration, examples)?

### 11. Testing
- **Unit tests** for service methods and DTOs (JUnit 5 + Mockito)?
- **Integration tests** against mocked upstream services?
- **Edge cases** covered (empty results, pagination, errors, timeouts)?
- Tests free of **real credentials**?

### 12. Code Style
- Follows **MARS Coding Conventions** and **MARS Naming Conventions**?
- **IntelliJ MARS style formatter** applied?
- **Checkstyle / SpotBugs / PMD** checks pass?
- **No compiler warnings** in changed code?
- Constants using **`UPPER_SNAKE_CASE`**?
- Package names using **only lowercase letters and digits**?

---

## Review Priority Matrix

| Area | Must Block PR | Should Fix Before Merge | Nice to Have |
|---|---|---|---|
| Raw API passthrough (no envelope) | Yes | | |
| No error categorization / recovery | Yes | | |
| Security vulnerability (injection, data leakage) | Yes | | |
| Tool name invalid (> 64 chars, special chars) | Yes | | |
| Missing input validation on tool parameters | Yes | | |
| Missing DTOs / no type safety (raw Map/JsonNode) | | Yes | |
| Missing `traceId` / observability metadata | | Yes | |
| Inconsistent naming style within server | | Yes | |
| Missing retry hints on retryable errors | | Yes | |
| Token-inefficient response (unbounded payload) | | Yes | |
| Missing comprehensive tool Javadoc | | Yes | |
| Missing unit / integration tests | | Yes | |
| Missing natural-language summary in response | | | Yes |
| Latency metadata in response | | | Yes |
| CHANGELOG entry | | | Yes |
