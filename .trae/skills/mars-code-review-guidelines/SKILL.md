---
name: mars-code-review-guidelines
description: |
  Comprehensive MARS code review guidelines covering PR preparation, review standards,
  coding conventions, and review reception. Includes Java/Spring conventions,
  Python development standards, CRM platform naming standards, and handling review
  feedback with technical rigor. Use when preparing code for review, conducting
  reviews as Tier1/Tier2 reviewer, reviewing Java/Spring/Python/CRM platform code,
  receiving review feedback, or mentoring developers on review process and coding standards.
  NOT for: general coding questions, technical implementation help, or non-MARS projects.
metadata:
  version: 1.1.0
  author: Mars Agent Skills
  tags: [code-review, guidelines, mars, pr-review, team-process, java, spring, python, crm-platform, naming-conventions, mcp, agent-tools]
---

# MARS Code Review Guidelines Skill

## Overview

This skill provides comprehensive MARS code review guidelines covering:
- PR preparation guidelines for authors
- Review standards for Tier1/Tier2 reviewers
- PR size recommendations and review timing
- Review comment best practices
- Team review process workflow
- **MARS Coding Conventions** (Java/Spring/API)
- **Python Development Standards** (Project structure, tooling, best practices)
- **CRM Platform Naming Conventions** (Apex/Objects/Fields)
- **MCP Tools Review Guidelines** (Agent-friendly design, envelope pattern, error handling)
- **Receiving Code Review** (handling feedback with technical rigor)

## Workflow

### 0. Initial Setup - Determine Review Strictness Level

Before starting the review, determine the desired comment level based on user preference.

**Load `references/guidelines.md` for complete level configuration.**

**Quick Reference:**
```
Level 1 (STRICT):   [CRITICAL] only
Level 2 (NORMAL):   [CRITICAL] + [SUGGESTION] (default)
Level 3 (RELAXED):  Above + [NIT]
Level 4 (ALL):      All prefixes including [PRAISE]
```

**Apply Level Filter:**
```
FOR each potential comment:
  IF comment.level >= user_selected_level:
    INCLUDE in output
  ELSE:
    EXCLUDE (silently skip)
```

### 1. Language Detection & Reference Selection

When conducting code review, first identify the programming language(s) involved:

```
IF code contains Java/Spring patterns:
  → Load references/java-conventions.md
  → Follow Section 7: MARS Coding Conventions Review

IF code contains Python patterns:
  → Load references/python-conventions.md
  → Follow Section 8: Python Development Standards Review

IF code contains CRM platform/Apex patterns:
  → Load references/crm-platform-conventions.md
  → Follow Section 9: CRM Platform Naming Conventions Review

IF code contains MCP patterns (@Tool, @ToolParam, MCP SDK, McpServer):
  → Load references/mcp-tools-conventions.md
  → Follow Section 10: MCP Tools Review Guidelines

IF multiple languages present:
  → Load all relevant reference files
  → Apply corresponding review sections
```

**Language Detection Hints:**
| Language/Type | File Extensions | Key Indicators |
|----------|----------------|----------------|
| Java | `.java` | `@RestController`, `class XxxService`, `import org.springframework` |
| Python | `.py` | `def`, `class`, `import`, `async def`, type hints |
| CRM Platform | `.cls`, `.trigger` | `global class`, `ApexPages`, `sObject`, `@AuraEnabled` |
| **MCP Tools** | `.java` | `@Tool`, `@ToolParam`, `McpServer`, `ToolExecutionException`, `McpToolResponse` |

### 2. PR Preparation (For Author)

#### 2.1 Check PR Size

Determine PR size by line count:

```
< 100     Small      ✅ Preferred
100-300   Medium     ✅ Preferred
300-666   Large      ⚠️ Try to split
> 666     Extra Large ❌ Must split
```

**Why Small/Medium PRs are favored:**
- Reviewed more quickly
- Reviewed more thoroughly
- Less likely to introduce bugs
- Less wasted work if rejected
- Easier to merge and roll back
- Less blocking on reviews

**When Large PRs are Okay:**
- Deletion of entire files
- Automatic refactoring by trusted tools

**Separate Refactoring:**
- Move/renaming classes: separate PR from bug fixes
- Small cleanups (variable names): can be included in feature PR
- Use judgment for larger refactorings

#### 2.2 Write Good PR Description

Follow Conventional Commits format.

**Bad Descriptions:**
- "Fix bug"
- "Fix build"
- "Add patch"
- "Moving code from A to B"

**Good Description Structure:**

```
<type>: <what the PR does>

<why this change is being made>
<specific implementation details>
<context and future direction>
```

**Examples:**

Functionality change:
```
feat: remove size limit on RPC server message freelist

Servers like FizzBuzz have very large messages and would benefit from reuse.
Make the freelist larger, and add a goroutine that frees the freelist entries
slowly over time, so that idle servers eventually release all freelist entries.
```

Refactoring:
```
refactor: Continuing the long-range goal of refactoring the Borglet Hierarchy

Construct a Task with a TimeKeeper to use its TimeStr and Now methods.
Add a Now method to Task, so the borglet() getter method can be removed.
This replaces the methods on Borglet that delegate to a TimeKeeper.
```

#### 2.3 Find Reviewers

**Tier1/Tier2 Reviewer Model:**
- Find **two reviewers** familiar with the codebase
- Typically one Tier1 reviewer
- Avoid more than 3 reviewers (often unproductive)

**Reviewer Responsibilities:**
- **Tier2 Reviewer:** First review, approves before Tier1
- **Tier1 Reviewer:** Final approval required for merge
- **Promotion:** Tier2 → Tier1 after 10+ mid/large PRs without critical comments

### 3. Code Review (For Reviewer)

#### 3.1 Review Standards

**Core Principle:**
- Favor approving PRs that improve overall code health
- No "perfect" code—only *better* code
- Balance forward progress vs. suggested changes
- Seek *continuous improvement*

**Nit Comments:**
- Prefix minor suggestions with "**Nit:**"
- Indicates polish that author can choose to ignore
- Example: "**Nit:** Consider renaming this variable for clarity"

**Mentoring:**
- OK to leave educational comments
- Prefix with "**Nit:**" if purely educational
- Sharing knowledge improves code health over time

#### 3.2 Review Principles

1. **Technical facts and data** overrule opinions
2. **Style guide** is absolute authority for style
3. **Software design** is based on principles, not personal preference
4. **Consistency** with current codebase if no other rule applies

#### 3.3 What to Look For

Checklist for reviewers:

- [ ] **Design:** Interactions make sense? Belongs in codebase?
- [ ] **Functionality:** Good for users? Edge cases handled?
- [ ] **Concurrency:** Safe parallel programming? No deadlocks/race conditions?
- [ ] **Complexity:** Not more complex than needed?
- [ ] **Future-proofing:** Not implementing speculative features?
- [ ] **UI Changes:** Sensible and look good?
- [ ] **Tests:** Appropriate unit tests present?
- [ ] **Test Design:** Tests are well-designed?
- [ ] **Naming:** Clear names for everything?
- [ ] **Comments:** Clear, useful, explain *why* not *what*?
- [ ] **Style:** Conforms to style guides?

**Review Every Line:**
- Look at context
- Ensure code health improvement
- **Compliment** developers on good things

#### 3.4 Efficient Review Process

1. **Does the change make sense?** Good PR description?
2. **Look at most important part first**
   - If too large, ask developer what to look at first
   - Or ask to split into multiple PRs
   - If major design problems, comment immediately
3. **Review rest in appropriate sequence**
   - Sometimes read tests first to understand expected behavior

### 4. Review Timing

**Maximum Response Time:** One business day

**Daily Schedule:**
- Block **two time slots** daily for reviews (morning, before end of day)
- Don't ignore reviews for more than 2 days
- Don't interrupt focused work; wait for natural break points

### 5. Handling Large PRs

**If PR is too large:**
1. Ask developer to split into smaller PRs that build on each other
2. If can't be split, write comments on overall design
3. Send back for improvement
4. Goal: Always unblock developer quickly without sacrificing code health

**Over Time:**
- Strict reviews → faster process over time
- Developers learn requirements → better initial PRs
- Reviewers respond quickly → less latency
- Don't compromise standards for imagined velocity

### 6. Writing Comments

**Guidelines:**
- **Be kind**
- **Explain your reasoning**
- **Balance:** Give explicit directions vs. point out problems and let developer decide
- **Encourage simplification** or adding comments instead of explaining complexity
- **Respect Level Filter:** Only output comments at or above the selected strictness level (see Section 0)

**Comment Format:**

```
[SUGGESTION] Brief description

Explanation of why this change is recommended.

Optional: Code example showing improvement.
```

**Categories:**
- `[CRITICAL]` - Must fix before merge
- `[SUGGESTION]` - Should consider
- `[NIT]` - Optional polish
- `[PRAISE]` - Good practice to encourage

**Reference:** See `references/guidelines.md` for complete level-based filtering guide.

### 7. MARS Coding Conventions Review (Java/Spring)

When reviewing Java code, check:

#### 7.1 API Naming
- Method names: **VerbNoun** pattern (e.g., `ListCustomers`, `GetCustomer`)
- Use imperative mood, no prepositions
- Request/Response: `{MethodName}Request/Response`
- JSON fields: `lower_case_underscore`, no prepositions

#### 7.2 Java Naming
| Element | Convention | Example |
|---------|------------|---------|
| Package | lowercase | `com.example.deepspace` |
| Class | UpperCamelCase | `CustomerService` |
| Method | lowerCamelCase | `sendMessage` |
| Constant | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |

#### 7.3 Spring Conventions
- Controllers must be **stateless** (no instance variables)
- Use `@RequiredArgsConstructor` not `@Autowired`
- JSON naming: **SnakeCase**
- Favor immutable objects

#### 7.4 Exception Handling
- Business layer: log context WITHOUT stack trace
- Entry points: log WITH stack trace
- Use runtime exceptions for RPC/REST

#### 7.5 Domain Model Layers
- **DO** - Data Object (database table)
- **DTO** - Data Transfer Object (service layer)
- **BO** - Business Object (business logic)
- **Query** - Query parameters (typed, no Map for >2 conditions)
- **VO** - View Object (display layer)

### 8. Python Development Standards Review

When reviewing Python code, check:

#### 8.1 Python Version
- MUST use Python 3.10.x - 3.12.x
- SHOULD NOT use versions < 3.10 (missing features) or too new (compatibility issues)

#### 8.2 Virtual Environment
- MUST use virtual environment for dependency isolation
- SHOULD use `uv` for environment and package management
- NEVER use global Python environment for dependencies

#### 8.3 Package Management
- MUST use `uv` as package manager
- MUST declare dependencies in `pyproject.toml`
- SHOULD use `project.optional-dependencies` for grouping (dev, test, etc.)
- MUST NOT manually edit `uv.lock`

#### 8.4 Project Structure
```
├── README.md
├── main.py              # Service entry point
├── src/                 # Project root (or app/)
│   ├── package_1/       # Business modules
│   ├── common/          # Shared utilities
│   ├── models/          # Data models
│   └── forms/           # Web forms
├── tests/               # Test cases
│   └── package_1/
├── pyproject.toml       # Project config & dependencies
├── uv.lock             # Lock file
├── .venv/              # Virtual environment
├── .pre-commit-config.yaml
└── Dockerfile
```

#### 8.5 Naming Conventions
- **Class names**: MUST use CamelCase (e.g., `DummyWorker`)
- **Functions/variables/modules**: MUST use snake_case (e.g., `get_record_by_id`, `book_views.py`)
- **Modules/directories**: Case by case for singular/plural
  - Plural: `forms/`, `models/`, `views/`, `scripts/`
  - Context-dependent: `users/` or `user/`

#### 8.6 Code Style & Tools
- **Formatter**: Use `ruff` for code formatting
- **Type checking**: Use `mypy` for static type checking
- **Pre-commit**: MUST configure `.pre-commit-config.yaml`
  - Code style (ruff)
  - Static checking (mypy)
  - Unit tests (pytest)

#### 8.7 Documentation
- **Docstrings**: SHOULD write for public interfaces
- **Format**: MUST use reStructuredText style (official Python standard)
- Include: purpose, parameters, return values, exceptions

#### 8.8 Testing
- **Framework**: MUST use `pytest`
- **Coverage**: Use `pytest + coverage`
- **Test type**: Integration tests preferred, unit tests for supplementary

#### 8.9 Data Validation
- **Pydantic**: SHOULD use Pydantic for form validation and data transfer
- Industry standard for Python data validation

#### 8.10 Logging
- **Recommendation**: Use `loguru` instead of standard `logging`
- Benefits: Easier to use, log safety, structured logging support

#### 8.11 Concurrency
- **GIL awareness**: Python's GIL limits true parallelism
- **I/O-bound**: Use coroutines (asyncio) within processes
- **CPU-bound**: Use multiprocessing

#### 8.12 Pythonic Code
Follow Zen of Python principles:
- Beautiful is better than ugly
- Explicit is better than implicit
- Simple is better than complex
- Readability counts
- There should be one obvious way to do it

### 9. CRM Platform Naming Conventions Review

When reviewing CRM platform code, check:

#### 9.1 Custom Objects
- Singular form, no underscores (e.g., `CustomerAsset` not `Customer_Assets`)
- Starts with uppercase, whole words

#### 9.2 Custom Fields
- PascalCase, no underscores (e.g., `CountryCode` not `Country_Code`)

#### 9.3 Apex Classes
- PascalCase, no underscores
- Controllers: suffix `Controller`
- Extensions: suffix `XController`

#### 9.4 Apex Triggers
- Format: `{ObjectName}Trigger` or `{ObjectName}{Operation}Trigger`
- Prefer single trigger per object

#### 9.5 Apex Test Classes
- Suffix `Test` (e.g., `CustomerServiceTest`)
- Not `TEST` prefix

#### 9.6 Apex Methods
- camelCase, verbs (e.g., `getCustomerDetail`)

#### 9.7 Apex Constants
- UPPER_SNAKE_CASE (e.g., `MAX_CHARACTERS`)

### 10. MCP Tools Review Guidelines

When reviewing MCP (Model Context Protocol) tools, check:

#### 10.1 Project Structure
- Standard MCP server layout with proper package organization
- Single entry point with Spring Boot `main()` method
- **Separation of concerns**: DTOs, tools, resources, business logic in separate packages
- Constants in dedicated class with `UPPER_SNAKE_CASE`
- Package names: only lowercase letters and digits

#### 10.2 Tool Naming
- Maximum **64 characters** for fully qualified name
- Starts with letter, alphanumeric/underscores/hyphens only
- **Consistent naming style** within server (prefer `snake_case`)
- Follow **verb-noun pattern** (e.g., `query_accounts`, `create_case`)
- Reference: [MCP Tool Naming Specification (SEP-986)](https://modelcontextprotocol.io/community/seps/986-specify-format-for-tool-names.md)

#### 10.3 Type Safety and Parameters
- Use **Java records or POJOs** with type annotations
- **Enums** for constrained values (`UpperCamelCase`)
- **Bean Validation** (`@NotNull`, `@Min`, `@Max`, `@Size`, `@Pattern`)
- `@ToolParam` with clear descriptions
- **Required**: `@NotNull`/`@NotBlank`, **Optional**: `@Nullable`
- AI instructions use **`CRITICAL`/`IMPORTANT`** keywords when needed
- Method names: `VerbNoun` in `lowerCamelCase` (e.g., `queryAccounts`)
- Request/Response objects: `{MethodName}Request`/`{MethodName}Response`
- JSON fields: `lower_case_underscore`

#### 10.4 Response Design: Envelope Pattern ⚠️ CRITICAL

**MUST NOT** return raw upstream API JSON directly. Use envelope pattern:

```json
{
  "content": [
    { "type": "text", "text": "Found 2 accounts" },
    {
      "type": "json",
      "json": {
        "accounts": [...],
        "count": 2,
        "nextPageToken": "abc123",
        "meta": { "traceId": "trace-123" }
      }
    }
  ]
}
```

**Key Principles:**
- **Task-oriented**: Describe "what was accomplished"
- **Stable contract**: Predictable field patterns (`records`, `count`, `nextPageToken`)
- **Minimal but sufficient**: Strip unnecessary metadata
- **Natural-language summary**: Provide human-readable text alongside data
- **Raw data optional**: Only for debugging, never as primary contract

#### 10.5 Error Handling
- **Error categorization**: `validation`, `permission`, `retryable`, `not_found`, `rate_limit`
- **Retryable errors**: Include `retryable: true/false` and `suggestedAction`/`hint`
- **No raw upstream error JSON** - wrap in MCP error envelope
- Meaningful error messages without leaking sensitive data
- Exception classes: `UpperCamelCase` with descriptive suffixes

#### 10.6 Security
- **Timeouts** on all external API calls (connect + read)
- **Input validation** at tool parameter boundary
- **Mitigate injection risks** (SOQL, command injection)
- **No sensitive data** in error messages or logs
- **Resource cleanup** using try-with-resources

#### 10.7 Concurrency
- Use **reactive/async patterns** (`CompletableFuture`, `Mono`/`Flux`)
- Thread-safe shared state; prefer immutable data structures
- Coordinate concurrent operations properly

#### 10.8 Logging and Observability
- **SLF4J + Logback** with configurable log levels
- **MDC context**: request IDs, trace IDs, tool name
- Appropriate log levels: `DEBUG`, `INFO`, `WARN`, `ERROR`
- **No sensitive data** in logs
- Include **`traceId`** in response metadata

#### 10.9 Configuration
- Externalize via **`application.yml`** and **`@ConfigurationProperties`**
- **`UPPER_SNAKE_CASE`** for environment variables
- **Sensible defaults** for optional configuration
- **No hardcoded secrets** - use env vars, Vault, or secure stores
- Configuration classes: `UpperCamelCase` with `Config` suffix
- Document all properties in **README**

#### 10.10 Documentation
- **Comprehensive Javadoc** for all tool methods:
  - Purpose, usage requirements, tips, output format, interpretation guidance
- **MCP server instructions block** for consuming LLM
- **README** complete: setup, tools, configuration, examples

#### 10.11 Testing
- **Unit tests** for service methods and DTOs (JUnit 5 + Mockito)
- **Integration tests** against mocked upstream services (WireMock/MockServer)
- **Edge cases**: empty results, pagination, errors, timeouts, boundaries
- **No real credentials** in tests

### 11. Receiving Code Review Feedback

When receiving review feedback:

#### 11.1 Response Pattern
```
1. READ: Complete feedback without reacting
2. UNDERSTAND: Restate requirement (or ask)
3. VERIFY: Check against codebase reality
4. EVALUATE: Technically sound for THIS codebase?
5. RESPOND: Technical acknowledgment or pushback
6. IMPLEMENT: One item at a time, test each
```

#### 11.2 Forbidden Responses
❌ "You're absolutely right!"
❌ "Great point!"
❌ "Let me implement that now" (before verification)

✅ Restate technical requirement
✅ Ask clarifying questions
✅ Push back with technical reasoning
✅ Just start working

#### 11.3 When to Push Back
- Suggestion breaks existing functionality
- Reviewer lacks full context
- Violates YAGNI (unused feature)
- Technically incorrect for this stack
- Conflicts with architectural decisions

#### 11.4 Handling Unclear Feedback
```
IF any item is unclear:
  STOP - do not implement
  ASK for clarification
```

#### 11.5 Implementation Order
1. Clarify unclear items FIRST
2. Implement in order:
   - Blocking issues (breaks, security)
   - Simple fixes (typos, imports)
   - Complex fixes (refactoring, logic)
3. Test each fix individually
4. Verify no regressions

#### 11.6 Acknowledging Correct Feedback
✅ "Fixed. [Brief description]"
✅ "Good catch - [specific issue]. Fixed in [location]."
✅ [Just fix it and show in code]

❌ Any gratitude expressions ("Thanks", "Great point")

## Examples

### Example 0: Review Level Control

**Scenario 1: Strict Review (Level 1)**
```
[User] Do a strict review, only show critical issues

[AI - Level 1 Filter Applied]
[CRITICAL] Potential null pointer exception in line 45

Variable 'user' is not checked for null before calling getName().
Add null check or use Optional.

(No [SUGGESTION], [NIT], or [PRAISE] comments output)
```

**Scenario 2: Standard Review (Level 2 - Default)**
```
[User] Review this PR

[AI - Level 2 Filter Applied]
[CRITICAL] Potential null pointer exception in line 45
...

[SUGGESTION] Consider extracting validation logic
...

(No [NIT] or [PRAISE] comments output)
```

**Scenario 3: Thorough Review (Level 3)**
```
[User] Thorough review with all feedback including nits

[AI - Level 3 Filter Applied]
[CRITICAL] Potential null pointer exception...
[SUGGESTION] Consider extracting validation logic...
[NIT] Variable name 'data' could be more descriptive
...
```

### Example 1: PR Too Large

```
[Author] Please review my PR (850 lines changed)

[Reviewer] This PR is quite large. Could you please split it into:
1. Refactoring of the data model (separate PR)
2. The new feature implementation (this PR)

This will make it easier to review thoroughly and reduce the risk of introducing bugs.
```

### Example 2: Good Review Comment

```
[SUGGESTION] Consider extracting this logic into a separate method

The current method is doing multiple things (validation, transformation, and persistence).
Extracting the validation logic would improve readability and make it easier to test.

Suggested(Java):
```
private ValidationResult validateOrder(Order order) {
    // validation logic
}
```
```

### Example 3: Nit Comment

```
[NIT] Consider using a more descriptive variable name

`data` → `customerOrderData`

This would make the code more self-documenting. Not blocking for this PR.
```

### Example 4: Tier1/Tier2 Workflow

```
[Developer] Assigns to Reviewer A (Tier2) and Reviewer B (Tier1)

[Reviewer A - Tier2] Reviews first, approves with comments

[Reviewer B - Tier1] Reviews after Tier2 approval, provides final approval

[Developer] Addresses comments, PR merged after Tier1 approval
```

### Example 5: Praising Good Practice

```
[PRAISE] Great use of early returns here

The refactoring to use early returns makes the control flow much clearer.
Nice work!
```

## Resources

Load reference materials based on detected language (see Section 0):

**Universal:**
- `references/guidelines.md` - Complete MARS code review guidelines
- `references/receiving-reviews.md` - Handling review feedback

**Language-Specific (auto-select):**
- `references/java-conventions.md` - Java/Spring coding conventions
- `references/python-conventions.md` - Python development standards
- `references/crm-platform-conventions.md` - CRM platform naming conventions
- `references/mcp-tools-conventions.md` - MCP Tools review guidelines

**External References:**
- [AWS Labs MCP Design Guidelines](https://github.com/awslabs/mcp/blob/main/DESIGN_GUIDELINES.md)
- [MCP Tool Naming Specification (SEP-986)](https://modelcontextprotocol.io/community/seps/986-specify-format-for-tool-names.md)

## Notes

- This skill consolidates MARS team review process, coding conventions, and feedback handling
- Replaces separate skills: `mars-coding-conventions`, `crm-platform-naming-conventions`, `receiving-code-review`
- **New**: Includes MCP Tools Review Guidelines for agent-friendly tool development
- For general code quality checks (non-MARS specific), use `code-review` skill
- Tier1/Tier2 model is specific to MARS team structure
- Python standards based on MARS Python project guidelines (PEP 8, PEP 621, etc.)
- MCP guidelines based on AWS Labs MCP Design Guidelines and MARS-SDS-0416
