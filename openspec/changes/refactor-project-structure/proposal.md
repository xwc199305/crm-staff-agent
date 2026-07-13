## Why

The current project structure contains non-production test code (Main.java) and uses inconsistent package naming that doesn't follow Java naming conventions and Mars code review guidelines. The `userguide` package name is generic and doesn't reflect the application's purpose, while the `reactagent` package duplicates functionality that should be integrated into the main application architecture. This refactoring is needed now to establish a clean, maintainable codebase before further development.

## What Changes

- **Remove non-essential test class**: Delete `src/main/java/com/example/reactagent/Main.java` - this is a demo/console application class that should not be part of the production codebase
- **Relocate ReactAgent**: Move `ReactAgent.java` from `com.example.reactagent` package to `com.example.staffagent.agent` package to integrate it into the main application architecture
- **Rename base package**: Replace `com.example.userguide` with `com.example.staffagent` to align with the project artifact name and provide more semantic meaning
- **Reorganize package structure**: Restructure packages following Java naming conventions and domain-driven design principles:
  - `com.example.staffagent` - base package
  - `com.example.staffagent.agent` - for agent-related classes (ReactAgent)
  - `com.example.staffagent.controller` - for REST controllers
  - `com.example.staffagent.service` - for service interfaces and implementations
  - `com.example.staffagent.dto` - for data transfer objects
  - `com.example.staffagent.exception` - for custom exceptions and handlers
- **Update all imports**: Refactor all import statements to reflect the new package structure
- **Update application main class**: Rename `UserGuideApplication` to `StaffAgentApplication` for consistency

## Capabilities

### New Capabilities
- `agent-integration`: Proper integration of ReactAgent into the main application architecture with clear separation of concerns
- `standard-package-structure`: Java naming convention-compliant package structure following Mars code review guidelines

### Modified Capabilities
None - this is a structural refactoring that doesn't change functional requirements.

## Impact

**Affected Code:**
- All Java classes in `src/main/java/com/example/reactagent/` (to be removed or relocated)
- All Java classes in `src/main/java/com/example/userguide/` (to be renamed and relocated)
- Import statements across all classes will need updates

**Configuration:**
- No changes to `pom.xml` required
- No changes to `application.properties` required

**Breaking Changes:**
- **BREAKING**: All package names will change, requiring updates to any external dependencies or documentation referencing the old package structure

**Dependencies:**
- No impact on external dependencies
- Spring Boot application structure will remain the same, only package names change

**Build & Deployment:**
- Maven build process unchanged
- Compiled class locations will change (automatic with package restructuring)