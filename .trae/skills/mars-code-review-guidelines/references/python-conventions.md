# Python Development Standards

## Quick Reference

### Python Version
- **MUST**: Python 3.10.x - 3.12.x
- **SHOULD NOT**: < 3.10 (missing features) or too new (compatibility issues)

### Tooling Stack
| Purpose | Tool | Command |
|---------|------|---------|
| Package Manager | `uv` | `uv add`, `uv sync` |
| Formatter | `ruff` | `uv run ruff format` |
| Linter | `ruff` | `uv run ruff check` |
| Type Checker | `mypy` | `uv run mypy` |
| Testing | `pytest` | `uv run pytest` |
| Pre-commit | `pre-commit` | `pre-commit run --all-files` |

### Project Structure
```
├── README.md
├── main.py              # Service entry point
├── src/                 # Project root
│   ├── package_1/       # Business modules
│   ├── common/          # Shared utilities
│   ├── models/          # Data models
│   └── forms/           # Web forms
├── tests/               # Test cases
├── pyproject.toml       # Project config
├── uv.lock             # Lock file
├── .venv/              # Virtual environment
├── .pre-commit-config.yaml
└── Dockerfile
```

### Naming Conventions
| Element | Style | Example |
|---------|-------|---------|
| Class | CamelCase | `DummyWorker` |
| Function | snake_case | `get_record_by_id()` |
| Variable | snake_case | `user_name` |
| Module | snake_case | `book_views.py` |
| Package | snake_case | `forms/`, `models/` |
| Constant | UPPER_SNAKE | `MAX_RETRY_COUNT` |

---

## Development Environment

### Virtual Environment

**MUST**: Use virtual environment for every project

```bash
# Create with uv
uv venv

# Or with Python
python -m venv .venv
```

**NEVER**: Install dependencies in global Python environment

### Package Management

**MUST**: Use `uv` as package manager

```bash
# Add production dependency
uv add fastapi httpx

# Add development dependency
uv add --dev ruff mypy pytest

# Sync environment
uv sync
```

**pyproject.toml** structure:
```toml
[project]
name = "my-project"
version = "0.1.0"
dependencies = [
    "fastapi>=0.100.0",
    "httpx>=0.24.0",
]

[project.optional-dependencies]
dev = ["ruff", "mypy", "pytest", "coverage"]
test = ["pytest", "pytest-cov"]
```

---

## Code Style

### Ruff Configuration

```toml
[tool.ruff]
target-version = "py310"
line-length = 88

[tool.ruff.lint]
select = ["E", "F", "I", "N", "W", "UP"]
```

### MyPy Configuration

```toml
[tool.mypy]
python_version = "3.10"
warn_return_any = true
warn_unused_configs = true
disallow_untyped_defs = true
check_untyped_defs = true
```

### Pre-commit Hooks

```yaml
repos:
  - repo: local
    hooks:
      - id: ruff-check
        name: ruff check
        entry: uv run ruff check --fix
        language: system
        types: [python]
      
      - id: ruff-format
        name: ruff format
        entry: uv run ruff format
        language: system
        types: [python]
      
      - id: mypy
        name: mypy check
        entry: uv run mypy
        language: system
        types: [python]
```

---

## Documentation

### Docstring Format (reStructuredText)

```python
def get_user_by_id(user_id: int) -> User:
    """Retrieve a user by their ID.
    
    :param user_id: The unique identifier of the user
    :type user_id: int
    :return: The user object if found
    :rtype: User
    :raises UserNotFoundError: If user with given ID doesn't exist
    """
    pass
```

### When to Write Docstrings

**SHOULD** write for:
- Public functions and methods
- Classes and modules
- Complex algorithms

**Content**:
- Purpose/what it does
- Parameters (name, type, description)
- Return value (type, description)
- Exceptions raised

---

## Testing

### pytest Configuration

```toml
[tool.pytest.ini_options]
testpaths = ["tests"]
addopts = "-v --strict-markers --tb=short"
```

### Test Structure

```
tests/
├── __init__.py
├── conftest.py          # Shared fixtures
├── test_module_1/
│   ├── __init__.py
│   ├── test_function_a.py
│   └── test_function_b.py
└── integration/
    └── test_api.py
```

### Testing Approach

- **Integration tests**: Preferred for business logic
- **Unit tests**: Supplement for complex algorithms
- **Coverage**: Track with `pytest-cov`

---

## Data Validation

### Pydantic Models

```python
from pydantic import BaseModel, Field

class UserCreate(BaseModel):
    """User creation form."""
    name: str = Field(..., min_length=1, max_length=100)
    email: str = Field(..., pattern=r"^[^@]+@[^@]+$")
    age: int = Field(..., ge=0, le=150)
```

**Benefits**:
- Automatic validation
- Type conversion
- Clear error messages
- JSON serialization

---

## Logging

### Loguru Setup

```python
from loguru import logger

# Basic usage
logger.info("User {} logged in", user_id)
logger.error("Failed to process order: {}", order_id)

# Structured logging
logger.bind(user_id=user_id, action="login").info("Login successful")
```

**Why loguru over standard logging**:
- Simpler API
- Automatic file rotation
- Structured logging support
- Better performance

---

## Concurrency

### GIL Awareness

Python's Global Interpreter Lock (GIL) means:
- Only one thread executes Python bytecode at a time
- **I/O-bound**: Use `asyncio` coroutines
- **CPU-bound**: Use `multiprocessing`

### Async Example

```python
import asyncio
import httpx

async def fetch_data(url: str) -> dict:
    async with httpx.AsyncClient() as client:
        response = await client.get(url)
        return response.json()

async def main():
    urls = ["http://api1.com", "http://api2.com"]
    results = await asyncio.gather(*[fetch_data(url) for url in urls])
```

---

## Best Practices

### Pythonic Code

```python
# Good: List comprehension
squares = [x**2 for x in range(10)]

# Good: Context managers
with open("file.txt") as f:
    content = f.read()

# Good: Generator expressions
sum(x**2 for x in range(1000000))

# Good: Unpacking
first, *rest = [1, 2, 3, 4, 5]
```

### Error Handling

```python
# Good: Specific exceptions
try:
    process_data(data)
except ValueError as e:
    logger.error("Invalid data format: {}", e)
    raise BusinessError("Data validation failed") from e

# Good: Don't catch all exceptions blindly
except Exception:  # Avoid this
    pass
```

### Type Hints

```python
from typing import Optional, List, Dict

def process_users(
    users: List[User],
    options: Optional[Dict[str, bool]] = None
) -> ProcessResult:
    ...
```

---

## Recommended Frameworks

| Use Case | Framework |
|----------|-----------|
| Web API | FastAPI |
| MCP | FastMCP |
| HTTP Client | httpx (async) / requests (sync) |
| Data Analysis | pandas, NumPy |
| Testing | pytest |
| Logging | loguru |

---

## References

- [PEP 8 - Style Guide](https://peps.python.org/pep-0008/)
- [PEP 621 - Project Metadata](https://peps.python.org/pep-0621/)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [pytest Documentation](https://docs.pytest.org/)
- [Pydantic Documentation](https://docs.pydantic.dev/)
