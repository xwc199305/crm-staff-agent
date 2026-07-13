# Salesforce Platform Naming Conventions Reference

## Table of Contents
1. [Custom Objects](#custom-objects)
2. [Custom Fields](#custom-fields)
3. [Validation Rules](#validation-rules)
4. [Platform Events](#platform-events)
5. [Apex Classes](#apex-classes)
6. [Apex Triggers](#apex-triggers)
7. [Apex Test Classes](#apex-test-classes)
8. [Apex Methods](#apex-methods)
9. [Apex Constants](#apex-constants)

---

## Custom Objects

### Naming Rules

| Rule | Description |
|------|-------------|
| Unique | Object name must be unique |
| Uppercase Start | Begin with uppercase letter |
| Whole Words | Use complete words, limit acronyms |
| Singular | Use singular form (Review, not Reviews) |
| No Underscores | No underscores in custom part |
| Label Match | Label should resemble object name |

### Examples

| Object Name | API Name | Reason |
|-------------|----------|--------|
| `CustomerAsset` | `CustomerAsset__c` | Unique, readable, singular |
| `Order` | `Order__c` | Singular form |
| `QrCode` | `QrCode__c` | Whole words used |

### Anti-patterns

```
❌ Customer_Assets__c    (underscore, plural)
❌ orders__c             (lowercase, plural)
❌ QRC__c                (too abbreviated)
```

---

## Custom Fields

### Naming Rules

| Rule | Description |
|------|-------------|
| Unique | Field name must be unique |
| Uppercase Start | Begin with uppercase letter |
| Whole Words | Use complete words |
| No Underscores | No underscores in custom part |
| Description | Fill Description field unless self-explanatory |

### Examples

| Field Name | API Name | Description Required? |
|------------|----------|----------------------|
| `CountryCode` | `CountryCode__c` | No - commonly understood |
| `CompletionDate` | `CompletionDate__c` | Yes - business rules to explain |
| `NumberOfUser` | `NumberOfUser__c` | No - follows standard pattern |

### Anti-patterns

```
❌ country_code__c       (lowercase, underscore)
❌ compDate__c           (abbreviated)
❌ NumUsers__c           (abbreviated)
```

---

## Validation Rules

### Naming Rules

| Rule | Description |
|------|-------------|
| Include Object | Object name in rule name |
| Intuitive | Clear without being restrictive |
| CamelCase | Standard CamelCase format |

### Format

```
{ObjectName}{FieldOrRuleDescription}
```

### Examples

| Validation Rule Name | Object | Purpose |
|---------------------|--------|---------|
| `AccountStreetAddressLength` | Account | Street address length check |
| `AccountZipcodePresence` | Account | Zipcode required check |
| `OrderCompletionDateValidation` | Order | Completion date validation |

### Anti-patterns

```
❌ Street_Length         (missing object prefix)
❌ zipcode_required      (lowercase, underscore)
❌ validate_date         (too generic)
```

---

## Platform Events

### Naming Rules

| Rule | Description |
|------|-------------|
| Event Suffix | Must end with 'Event' |
| CamelCase | Standard CamelCase |
| Uppercase Start | Start with uppercase letter |
| Field Lowercase | Field names start with lowercase |

### Examples

| Event Name | Fields |
|------------|--------|
| `AddToCartEvent` | `cartId`, `productId`, `quantity` |
| `OrderCompletedEvent` | `orderId`, `completedAt` |
| `PaymentProcessedEvent` | `paymentId`, `status` |

### Anti-patterns

```
❌ AddToCart             (missing Event suffix)
❌ orderCompletedEvent   (lowercase start)
❌ Payment_Processed     (underscore, no Event suffix)
```

---

## Apex Classes

### Naming Rules

| Rule | Description |
|------|-------------|
| Unique | Class name must be unique |
| Uppercase Start | Begin with uppercase letter |
| No Underscores | No underscores allowed |
| No Spaces | No spaces allowed |
| CamelCase | Initial uppercase, internal words capitalized |
| Whole Words | Use complete words |
| Controller Suffix | Controllers end with 'Controller' |
| Extension Suffix | Controller extensions end with 'XController' |

### Class Types

| Type | Suffix | Example |
|------|--------|---------|
| Regular Class | None | `Customer`, `AddressHandler` |
| Custom Controller | Controller | `CustomerController` |
| Controller Extension | XController | `OrderExtensionXController` |

### Examples

| Class Name | Type | Reason |
|------------|------|--------|
| `Customer` | Regular | Full word, uppercase start |
| `AddressHandler` | Regular | Multiple words concatenated |
| `CustomerController` | Controller | Controller suffix |
| `AccountExtensionXController` | Extension | XController suffix |

### Anti-patterns

```
❌ customer              (lowercase)
❌ Address_Handler       (underscore)
❌ CustomerCtrl          (abbreviated)
❌ OrderExtController    (wrong extension suffix)
```

---

## Apex Triggers

### Naming Rules

| Rule | Description |
|------|-------------|
| Format | `{ObjectName}{Operation}Trigger` or `{ObjectName}Trigger` |
| Operations | Insert, Update, Delete, Undelete |
| Single Trigger | One trigger per object (recommended) |
| CamelCase | Standard CamelCase |

### Naming Formats

```
{ObjectName}Trigger                    // Single trigger for all operations
{ObjectName}{Operation}Trigger         // Operation-specific trigger
```

### Examples

| Trigger Name | Object | Operations | Recommendation |
|--------------|--------|------------|----------------|
| `AccountTrigger` | Account | All | ✅ Preferred |
| `AccountUpdateTrigger` | Account | Update only | ⚠️ Avoid if possible |
| `OrderInsertTrigger` | Order | Insert only | ⚠️ Avoid if possible |

### Best Practice

```java
// ✅ Recommended - Single trigger per object
trigger AccountTrigger on Account (
    before insert, 
    before update, 
    before delete,
    after insert, 
    after update, 
    after delete, 
    after undelete
) {
    // Delegate to handler class
    AccountTriggerHandler.handle(Trigger.operationType);
}
```

### Anti-patterns

```
❌ account_trigger       (lowercase, underscore)
❌ Account_Update_Trigger (underscore)
❌ orderInsert           (missing Trigger suffix)
```

---

## Apex Test Classes

### Naming Rules

| Rule | Description |
|------|-------------|
| Unique | Class name must be unique |
| Uppercase Start | Begin with uppercase letter |
| No Spaces | No spaces allowed |
| CamelCase | Initial uppercase, internal words capitalized |
| Whole Words | Use complete words |
| Test Suffix | End with 'Test' (not TEST prefix) |

### Examples

| Class Name | Class Being Tested | Reason |
|------------|-------------------|--------|
| `CustomerManagementTest` | CustomerManagement | Test suffix, alphabetical order |
| `OrderServiceTest` | OrderService | Test suffix |
| `AccountHandlerTest` | AccountHandler | Test suffix |

### Anti-patterns

```
❌ TESTCustomerManagement  (TEST prefix)
❌ OrderService_Tests      (underscore)
❌ TestAccountHandler      (Test prefix)
```

### Why Suffix Over Prefix?

```
Alphabetical listing:
✅ AccountHandler
✅ AccountHandlerTest

❌ TESTAccountHandler      (appears in T section, not near class)
```

---

## Apex Methods

### Naming Rules

| Rule | Description |
|------|-------------|
| Verbs | Should be action words |
| Mixed Case | First letter lowercase |
| CamelCase | Internal words capitalized |
| Whole Words | Use complete words |
| Descriptive | Longer names OK if needed for clarity |

### Examples

| Method Name | Action | Clarity |
|-------------|--------|---------|
| `ammortizationCalculation()` | Calculate amortization | Clear |
| `repaginateDocument()` | Repaginate document | Clear |
| `getCustomerDetail()` | Get customer details | Clear |
| `numberOfTransactionsInQ1()` | Count Q1 transactions | Long but clear |

### Anti-patterns

```
❌ AmmortizationCalculation()    (uppercase start)
❌ Repaginate_Document()         (underscore)
❌ get_customer_detail()         (underscore)
❌ numTxnsQ1()                   (abbreviated)
```

### Common Method Patterns

| Operation | Pattern | Example |
|-----------|---------|---------|
| Get single | `get{Object}By{Field}` | `getAccountById()` |
| Get list | `get{Objects}` | `getActiveOrders()` |
| Create | `create{Object}` | `createCustomer()` |
| Update | `update{Object}` | `updateOrderStatus()` |
| Delete | `delete{Object}` | `deleteAccount()` |
| Validate | `validate{Object}` | `validateOrder()` |
| Calculate | `calculate{Metric}` | `calculateTotalAmount()` |
| Process | `process{Object}` | `processPayment()` |

---

## Apex Constants

### Naming Rules

| Rule | Description |
|------|-------------|
| Uppercase | All letters uppercase |
| Underscores | Words separated by underscores |
| GlobalConstants | Common constants in GlobalConstants class |
| Minimal Scope | Keep scope minimal (prefer private) |

### Examples

| Constant Name | Value | Scope | Location |
|--------------|-------|-------|----------|
| `MAX_CHARACTERS` | 255 | Private | Usage class |
| `DEFAULT_PAGE_SIZE` | 20 | Public | GlobalConstants |
| `API_VERSION` | 'v52.0' | Public | GlobalConstants |
| `MAX_RETRY_ATTEMPTS` | 3 | Private | Service class |

### GlobalConstants Pattern

```java
public class GlobalConstants {
    // API Configuration
    public static final String API_VERSION = 'v52.0';
    public static final Integer API_TIMEOUT = 30000;
    
    // Pagination
    public static final Integer DEFAULT_PAGE_SIZE = 20;
    public static final Integer MAX_PAGE_SIZE = 2000;
    
    // Limits
    public static final Integer MAX_BATCH_SIZE = 200;
    
    // Private constructor to prevent instantiation
    private GlobalConstants() {}
}
```

### Anti-patterns

```
❌ maxCharacters             (lowercase)
❌ MaxCharacters             (mixed case)
❌ MAXCHARACTERS             (no separator)
❌ max_characters            (lowercase with underscore)
```

### Scope Best Practices

```java
public class OrderService {
    // ✅ Private - only used in this class
    private static final Integer MAX_RETRIES = 3;
    
    // ✅ Private - only used in this class
    private static final String STATUS_PENDING = 'Pending';
    
    public void processOrder(Order order) {
        // Use private constants
        for (Integer i = 0; i < MAX_RETRIES; i++) {
            // ...
        }
    }
}
```

---

## Quick Reference Card

### Summary Table

| Element | Case | Suffix | Example |
|---------|------|--------|---------|
| Custom Object | PascalCase | `__c` | `CustomerAsset__c` |
| Custom Field | PascalCase | `__c` | `CountryCode__c` |
| Validation Rule | PascalCase | None | `AccountZipcodePresence` |
| Platform Event | PascalCase | `Event` | `AddToCartEvent` |
| Apex Class | PascalCase | varies | `CustomerController` |
| Apex Trigger | PascalCase | `Trigger` | `AccountTrigger` |
| Apex Test Class | PascalCase | `Test` | `OrderServiceTest` |
| Apex Method | camelCase | None | `getCustomerDetail()` |
| Apex Constant | UPPER_SNAKE | None | `MAX_CHARACTERS` |

### Common Suffixes

| Suffix | Usage |
|--------|-------|
| `__c` | Custom object/field API name |
| `Controller` | Custom controller class |
| `XController` | Controller extension class |
| `Trigger` | Apex trigger |
| `Test` | Apex test class |
| `Event` | Platform event |
| `Handler` | Utility/handler class |
| `Service` | Service layer class |
