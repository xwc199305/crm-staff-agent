## Context

The project currently has two separate package structures:
1. `com.example.reactagent` - Contains a standalone ReactAgent wrapper class and a demo Main class
2. `com.example.userguide` - Contains the main Spring Boot application with a layered architecture (controller/service/dto/exception)

The ReactAgent class in `com.example.reactagent` provides a simple wrapper around AgentScope's ReActAgent, but the application already has a more sophisticated implementation in `ReactAgentServiceImpl` that uses AgentScope directly with proper Spring Boot integration (configuration injection, logging, error handling).

This creates duplication and confusion. The `userguide` package name is also generic and doesn't align with the project artifact name (`userguide-staff-agent`). Following Java naming conventions and Mars code review guidelines, the package structure should be refactored to be more semantic and maintainable.

## Goals / Non-Goals

**Goals:**
- Remove non-production code (Main.java demo class)
- Consolidate agent-related functionality into a single, coherent package structure
- Rename packages to align with project purpose and naming conventions
- Maintain existing functionality without breaking changes to the API layer
- Follow Java naming conventions (lowercase package names) and Mars code review guidelines
- Apply domain-driven design principles to package organization

**Non-Goals:**
- Refactoring internal implementation of ReactAgentServiceImpl
- Adding new features or capabilities
- Changing the REST API endpoints or behavior
- Modifying configuration properties structure
- Updating dependencies in pom.xml

## Decisions

### Decision 1: Relocate ReactAgent.java

**Choice:** RELOCATE `com.example.reactagent.ReactAgent.java` to `com.example.staffagent.agent` package

**Rationale:**
- The user explicitly requested to move ReactAgent.java to the new package structure
- ReactAgent provides a simplified, standalone wrapper around AgentScope's ReActAgent that may be useful for different use cases
- Placing it in the `com.example.staffagent.agent` package keeps agent-related utilities organized together
- It can coexist with ReactAgentServiceImpl, serving different purposes:
  - ReactAgent: Simple, lightweight wrapper for direct agent interactions
  - ReactAgentServiceImpl: Spring Boot integrated service with configuration, logging, and error handling

**Alternatives Considered:**
- Delete ReactAgent.java: Rejected because user explicitly requested relocation
- Merge ReactAgent into ReactAgentServiceImpl: Not needed since they serve different purposes

### Decision 2: New Package Structure

**Choice:** Use `com.example.staffagent` as base package with feature-based subpackages

**Rationale:**
- Aligns with Maven artifact name (`userguide-staff-agent` → `staffagent`)
- Follows Java naming conventions (lowercase, no underscores)
- Matches Mars code review guidelines for package naming
- Domain-driven structure reflects the application's purpose (staff agent system)

**Package Layout:**
```
com.example.staffagent
├── StaffAgentApplication.java
├── agent/           (agent-related classes - ReactAgent)
│   └── ReactAgent.java
├── controller/     (REST controllers)
├── service/         (service interfaces and implementations)
│   └── impl/
├── dto/             (data transfer objects)
└── exception/       (custom exceptions and handlers)
```

**Alternatives Considered:**
- Keep `com.example.userguide`: Rejected because it's generic and doesn't reflect application purpose
- Use `com.example.reactagent`: Rejected because it's too narrow (only describes agent component, not whole application)
- Use feature-based packages (e.g., `com.example.staffagent.chat`): Overkill for current application size; could be reconsidered if application grows

### Decision 3: Application Class Naming

**Choice:** Rename `UserGuideApplication` to `StaffAgentApplication`

**Rationale:**
- Consistency with new package name
- Better semantic meaning
- Follows Spring Boot convention of naming application class after the application purpose

### Decision 4: Migration Strategy

**Choice:** Direct package restructuring with IDE refactoring support

**Rationale:**
- Modern IDEs (IntelliJ IDEA, Eclipse) provide safe refactoring tools
- Automatic import statement updates
- Compile-time verification of all references
- No need for intermediate migration steps

**Risks / Trade-offs:**
- Requires careful verification that all imports are updated
- May break external documentation or scripts that reference old package names
- Need to update any reflection-based code (none found in current codebase)

## Risks / Trade-offs

### Risk 1: Breaking External References
**Risk:** Any external documentation, scripts, or configurations referencing old package names will break
**Mitigation:**
- Document the package name changes clearly in commit message
- Verify no external configuration files reference old class names (checked: none found)
- No published artifacts exist, so no downstream dependencies affected

### Risk 2: Missed Import Updates
**Risk:** Some import statements may not be automatically updated during refactoring
**Mitigation:**
- Use IDE refactoring tools for reliable bulk updates
- Run full Maven build after refactoring to catch compilation errors
- Execute all tests (if available) to verify runtime behavior

### Risk 3: Configuration Property Mismatches
**Risk:** Spring Boot configuration properties might reference old class names
**Mitigation:**
- Review `application.properties` for any class-specific configuration (none found)
- Configuration uses property keys, not class names, so minimal risk

### Risk 4: Loss of Agent Demo Functionality
**Risk:** Deleting Main.java removes a working console demo
**Mitigation:**
- Main.java is not production code and should not be in main source tree
- If demo is needed, it should be in `src/test/java` or a separate examples module
- Document the removal in case team needs to recreate demo functionality

## Migration Plan

### Phase 1: Preparation
1. Ensure working directory is clean (all changes committed)
2. Create backup branch if needed

### Phase 2: Package Restructuring (Recommended: IDE Refactoring)
**Using IntelliJ IDEA (recommended):**
1. Right-click on `com.example.userguide` package → Refactor → Rename → `com.example.staffagent`
2. IDE will automatically update all import statements
3. Right-click on `UserGuideApplication.java` → Refactor → Rename → `StaffAgentApplication`
4. Review and apply all refactoring suggestions

**Manual approach (if IDE refactoring not available):**
1. Create new directory structure: `src/main/java/com/example/staffagent/`
2. Move all files from `com.example.userguide` to new package
3. Update package declarations in all Java files
4. Update all import statements
5. Delete old `com.example.userguide` directory

### Phase 3: Code Cleanup
1. Delete `src/main/java/com/example/reactagent/Main.java`
2. Move `src/main/java/com/example/reactagent/ReactAgent.java` to `src/main/java/com/example/staffagent/agent/`
3. Update package declaration in ReactAgent.java from `com.example.reactagent` to `com.example.staffagent.agent`
4. Delete empty `src/main/java/com/example/reactagent/` directory
5. Verify `com.example.reactagent` package is completely removed

### Phase 4: Verification
1. Run `mvn clean compile` to verify compilation
2. Run `mvn test` to verify all tests pass (if tests exist)
3. Run `mvn spring-boot:run` to verify application starts successfully
4. Test REST endpoints (e.g., `POST /api/agent/chat`)

### Phase 5: Documentation Update
1. Update README.md if it references old package names
2. Update any inline documentation or comments

### Rollback Strategy
If issues are discovered after refactoring:
1. Use git to revert the commit: `git revert <commit-hash>`
2. Or reset to previous state: `git reset --hard <previous-commit>`
3. All changes are reversible through version control

## Open Questions

**Q1: Should we keep a console-based demo application?**
- Current decision: Delete Main.java
- Alternative: Move to `src/test/java/com/example/staffagent/demo/` or create separate examples module
- Resolution needed: None - Main.java is demo code not needed for production

**Q2: Should agent-related utilities have their own package?**
- Current decision: Create `com.example.staffagent.agent` package (empty for now)
- Alternative: Only create packages when needed (YAGNI principle)
- Resolution: Keep empty package structure for future extensibility, but document that it's reserved for agent-specific utilities

**Q3: Should we add integration tests during refactoring?**
- Current decision: Not in scope for this refactoring
- Alternative: Add basic integration tests for REST endpoints
- Resolution: Out of scope - this is a structural refactoring only. Add tests in separate task if needed.