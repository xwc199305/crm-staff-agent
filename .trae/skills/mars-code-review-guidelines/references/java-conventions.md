# MARS Coding Conventions Reference

## Table of Contents
1. [API Naming Convention](#api-naming-convention)
2. [Java Naming Convention](#java-naming-convention)
3. [Camel Case Rules](#camel-case-rules)
4. [Java Tips](#java-tips)
5. [Testing Conventions](#testing-conventions)
6. [Spring Framework Conventions](#spring-framework-conventions)
7. [Exception Handling](#exception-handling)
8. [Domain Model Layers](#domain-model-layers)

---

## API Naming Convention

### Core Principles
All API names should be:
- **Simple**
- **Intuitive**
- **Consistent**

### Standard Methods

| Verb | Noun | Method Name | Request | Response |
|------|------|-------------|---------|----------|
| List | {Resource}s (plural) | ListCustomers | ListCustomersRequest | ListCustomersResponse |
| Get | {Resource} (singular) | GetCustomer | GetCustomerRequest | Customer |
| Create | {Resource} (singular) | CreateCustomer | CreateCustomerRequest | Customer |
| Update | {Resource} (singular) | UpdateCustomer | UpdateCustomerRequest | Customer |
| Rename | {Resource} (singular) | RenameCustomer | RenameCustomerRequest | RenameCustomerResponse |
| Delete | {Resource} (singular) | DeleteCustomer | DeleteCustomerRequest | Empty |

### Method Naming Rules

1. **Use imperative mood** (commands, not questions)
   - ✅ `CheckCustomerStatus`
   - ❌ `IsCustomerActive`

2. **No prepositions** in method names
   - ✅ `CreateWechatCustomer`
   - ❌ `CreateCustomerFromWechat`

3. **Standard method noun rules:**
   - Plural for List: `ListCustomers`
   - Singular for others: `GetCustomer`, `CreateCustomer`

### Request/Response Naming

Format: `{MethodName}Request` / `{MethodName}Response`

Exceptions (no suffix needed):
- Empty object
- Resource type
- Operation resource

Applies to: Get, Create, Update, Delete methods

### Field Naming (JSON)

Format: `lower_case_underscore`

Rules:
- ❌ No prepositions: `error_reason` not `reason_for_error`
- ❌ No postpositive adjectives: `collected_items` not `items_collected`
- ✅ Include units: `xxx_bytes`, `width_pixels`, `node_count`

### Name Abbreviations

Use abbreviations in API, full spelling in documentation:

| Abbreviation | Full Form |
|--------------|-----------|
| config | configuration |
| id | identifier |
| spec | specification |
| stats | statistics |

---

## Java Naming Convention

### Package Names
- lowercase letters and digits only
- No underscores
- Consecutive words concatenated

✅ `com.example.deepspace`
❌ `com.example.deepSpace`
❌ `com.example.deep_space`

### Class Names
- UpperCamelCase
- Nouns or noun phrases
- Interface: nouns, adjectives, or adjective phrases

Examples:
- `Character`
- `ImmutableList`
- `List` (interface)
- `Readable` (interface)

### Method Names
- lowerCamelCase
- Verbs or verb phrases

Examples:
- `sendMessage`
- `stop`

Test methods: underscores allowed for separation
- `transferMoney_deductsFromSource`

### Constant Names
- UPPER_SNAKE_CASE
- Must be `static final`
- Deeply immutable

**✅ Constants:**
```java
static final int NUMBER = 5;
static final ImmutableList<String> NAMES = ImmutableList.of("Ed", "Ann");
static final Map<String, Integer> AGES = ImmutableMap.of("Ed", 35, "Ann", 32);
static final Joiner COMMA_JOINER = Joiner.on(',');
```

**❌ NOT Constants:**
```java
static String nonFinal = "non-final";
final String nonStatic = "non-static";
static final Set<String> mutableCollection = new HashSet<>();
static final Logger logger = Logger.getLogger(MyClass.getName());
static final String[] nonEmptyArray = {"these", "can", "change"};
```

### Test Class Names
- End with `Test`
- Single class coverage: `{ClassName}Test`

Examples:
- `HashIntegrationTest`
- `HashImplTest`

---

## Camel Case Rules

| Prose Form | Correct | Incorrect |
|------------|---------|-----------|
| "XML HTTP request" | `XmlHttpRequest` | `XMLHTTPRequest` |
| "new customer ID" | `newCustomerId` | `newCustomerID` |
| "inner stopwatch" | `innerStopwatch` | `innerStopWatch` |
| "supports IPv6 on iOS?" | `supportsIpv6OnIos` | `supportsIPv6OnIOS` |

---

## Java Tips

### Exception Handling
- Treat `Throwable`, `Exception`, `Error`, `RuntimeException` as abstract
- Don't construct or throw directly
- Don't declare `throws Exception` as shortcut

### Static Usage
- Default to `static` for every nested class
- Mark methods `static` when possible
- Key question: Can method behavior be fully specified by input/output only?

### Type Selection
- **Parameters**: Most general type
- **Return types**: Most specific type (e.g., `ImmutableList` not `List`)

### Code Complexity
**Refactor when:**
- Nesting > 2 levels deep
- Multiple blocks requiring separate comments
- Different exception handling in multiple places
- Can't fit on screen

### Javadoc
- Purpose: Explain intended meaning/purpose
- Not for: Implementation details
- Required for: Public APIs

---

## Testing Conventions

### Test Philosophy
- **Test behaviors, not methods** - Focus on what the code does, not its structure
- **Favor Fake Data over Mock Data** - Use real or simplified implementations

### AAA Pattern (Arrange-Act-Assert)
Every test must have three distinct steps:

```java
@Test
void shouldDecreaseBalance_whenWithdrawalAmount() {
    // Arrange
    Account account = new Account(100);
    
    // Act
    account.withdraw(30);
    
    // Assert
    assertEquals(70, account.getBalance());
}
```

### Setup Method Guidelines
- Setup methods should only define **default values**
- Tests must **NOT explicitly rely** on behavior defined in setup
- Use for construction details irrelevant to current test

### Local Variables vs Class Constants
Convert to class-wide constants when:
- Used across multiple tests
- Construction contains many irrelevant details
- Inlining is preferable for very simple values

### Test Naming
Use descriptive names explaining behavior:
- Format 1: `should{ExpectedBehavior}_{when/under}_{Condition}`
- Format 2: `methodName_condition_expectedResult`
- Format 3: `lowerCamelCase_with_underscores_for_separation`

---

## Spring Framework Conventions

### Dependency Management
- Use **custom BOM** to maintain third-party dependencies

### Controller Design Principles

Controllers must be:

1. **Stateless**
   - Controllers are singletons
   - Any state causes concurrency issues
   - ❌ `private int counter = 0;`

2. **Thin**
   - No business logic
   - Delegate to services
   - ❌ Complex calculations in controller

3. **HTTP-focused**
   - Handle HTTP layer only
   - Don't pass HTTP concerns to services
   - ❌ Passing HttpServletRequest to service

4. **Use-case oriented**
   - Design around business capabilities
   - One endpoint per use case

### Dependency Injection

Use `@RequiredArgsConstructor` (Lombok) instead of `@Autowired`:

```java
// ✅ Recommended
@RestController
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;
}

// ❌ Avoid
@RestController
public class CustomerController {
    @Autowired
    private CustomerService customerService;
}
```

Benefits:
- Better testability
- Immutability (final fields)
- Clear dependencies

### JSON Naming Strategy

Use **SnakeCase** for JSON properties:

```yaml
# application.yml
spring:
  jackson:
    property-naming-strategy: SNAKE_CASE
```

Input/Output:
```json
{
  "customer_id": "123",
  "created_at": "2024-01-01"
}
```

### Immutability

**Favor immutable objects** to avoid concurrency issues:

```java
// ✅ Immutable
public class Money {
    private final BigDecimal amount;
    private final Currency currency;
    
    public Money add(Money other) {
        return new Money(this.amount.add(other.amount), this.currency);
    }
}

// ❌ Mutable
public class Money {
    private BigDecimal amount;
    
    public void add(Money other) {
        this.amount = this.amount.add(other.amount);
    }
}
```

### Directory Structure

```
config/       - Property file readers
constants/    - Constant definitions
controller/   - Controller classes (stateless, thin)
exception/    - Exception classes
model/        - POJO classes
cache/        - Caching mechanism
security/     - Security classes
service/      - Service implementations
util/         - Utility classes
validation/   - Validator classes
dependency/   - External accessors
```

---

## Exception Handling

### Exception Types for RPC/REST

**Throw runtime exceptions** when necessary:

```java
// ✅ Recommended
public Customer getCustomer(String id) {
    return repository.findById(id)
        .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + id));
}

// ❌ Avoid forcing callers to handle checked exceptions
public Customer getCustomer(String id) throws CustomerNotFoundException;
```

Callers typically don't want to handle checked exceptions at every layer.

Reference: https://www.baeldung.com/exception-handling-for-rest-with-spring

### Exception Logging Pattern

**Business Layer** - Log context WITHOUT stack trace:

```java
try {
    processOrder(order);
} catch (Exception e) {
    // ✅ Correct - log context only
    log.error("Process failed, orderId: {}, orgId: {}", orderId, orgId);
    log.error("Process failed, message: {}, orderId: {}", e.getMessage(), orderId);
    
    // Re-throw or convert
    throw new OrderProcessingException("Failed to process order: " + orderId, e);
}
```

```java
try {
    processOrder(order);
} catch (Exception e) {
    // ❌ Incorrect - logs stack trace in business layer
    log.error("Process failed, orderId: {}", orderId, e);
    throw e;
}
```

**Entry Points** - Log WITH stack trace:

```java
@RestController
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    
    @PostMapping("/orders")
    public ResponseEntity<Order> create(@RequestBody OrderRequest request) {
        try {
            return ResponseEntity.ok(orderService.create(request));
        } catch (Exception e) {
            // ✅ Stack trace logged at entry point
            log.error("Create order failed, request: {}", request, e);
            throw e;
        }
    }
}
```

Entry points include:
- Web controllers
- MQ listeners
- Scheduled jobs
- External API callbacks

---

## Domain Model Layers

### Layer Definitions

| Layer | Full Name | Purpose | Flow Direction |
|-------|-----------|---------|----------------|
| **DO** | Data Object | Database table structure | DAO → Up |
| **DTO** | Data Transfer Object | Service/Manager layer transfer | Service → Up |
| **BO** | Business Object | Business logic encapsulation | Service → Up |
| **Query** | Query Object | Query request from upper layers | Upper → Down |
| **VO** | View Object | Display/View layer | Web layer |

### Usage Guidelines

**DO (Data Object)**
```java
// Maps directly to database table
public class CustomerDO {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
}
```

**DTO (Data Transfer Object)**
```java
// Transferred between services
public class CustomerDTO {
    private String customerId;
    private String displayName;
    private AccountStatus status;
}
```

**Query Object**
```java
// Query parameters with type safety
public class CustomerQuery {
    private String name;
    private CustomerStatus status;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
}
```

**❌ Prohibit Map for Queries**

```java
// ❌ Avoid - more than 2 conditions
public List<Customer> findByConditions(Map<String, Object> params);

// ✅ Use dedicated Query object
public List<Customer> findByConditions(CustomerQuery query);
```

### Layer Transformation

```
Controller (VO) 
    ↓ convert
Service (DTO/BO)
    ↓ convert  
DAO/Repository (DO)
    ↓
Database
```

Each layer should transform to its appropriate type:
- Controller receives VO, converts to DTO for service
- Service works with DTO/BO, converts to DO for DAO
- DAO returns DO, service converts to DTO

---

## Tooling

### IntelliJ Code Formatter
Install: `intellij-java-mars-style.xml`

Location: https://bitbucket.sfdcbt-cn.net:8443/projects/SMB/repos/mars-decision-records/browse/intellij-java-mars-style.xml
