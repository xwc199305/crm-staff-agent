## 1. Preparation

- [ ] 1.1 Verify working directory is clean (all changes committed)
- [ ] 1.2 Create backup branch if needed (`git checkout -b backup-before-refactor`)

## 2. Package Restructuring

- [ ] 2.1 Create new base package directory structure (`src/main/java/com/example/staffagent/`)
- [ ] 2.2 Create subdirectories under new base package:
  - `agent/`
  - `controller/`
  - `service/impl/`
  - `dto/`
  - `exception/`
- [ ] 2.3 Move `UserGuideApplication.java` to new base package and rename to `StaffAgentApplication.java`
- [ ] 2.4 Update package declaration in `StaffAgentApplication.java` to `com.example.staffagent`
- [ ] 2.5 Move all files from `com.example.userguide.controller` to `com.example.staffagent.controller`
- [ ] 2.6 Update package declarations in controller classes to `com.example.staffagent.controller`
- [ ] 2.7 Move `ReactAgentService.java` to `com.example.staffagent.service`
- [ ] 2.8 Update package declaration in `ReactAgentService.java` to `com.example.staffagent.service`
- [ ] 2.9 Move `ReactAgentServiceImpl.java` to `com.example.staffagent.service.impl`
- [ ] 2.10 Update package declaration in `ReactAgentServiceImpl.java` to `com.example.staffagent.service.impl`
- [ ] 2.11 Move `ApiResponse.java` to `com.example.staffagent.dto`
- [ ] 2.12 Update package declaration in `ApiResponse.java` to `com.example.staffagent.dto`
- [ ] 2.13 Move `BusinessException.java` to `com.example.staffagent.exception`
- [ ] 2.14 Update package declaration in `BusinessException.java` to `com.example.staffagent.exception`
- [ ] 2.15 Move `GlobalExceptionHandler.java` to `com.example.staffagent.exception`
- [ ] 2.16 Update package declaration in `GlobalExceptionHandler.java` to `com.example.staffagent.exception`

## 3. Import Statement Updates

- [ ] 3.1 Update imports in `StaffAgentApplication.java` (if any)
- [ ] 3.2 Update imports in `ReactAgentController.java` to reference `com.example.staffagent.service` and `com.example.staffagent.dto`
- [ ] 3.3 Update imports in `ReactAgentServiceImpl.java` to reference `com.example.staffagent.service`
- [ ] 3.4 Update imports in `GlobalExceptionHandler.java` (if any internal references)
- [ ] 3.5 Verify all import statements use new package structure

## 4. Code Cleanup

- [ ] 4.1 Delete `src/main/java/com/example/reactagent/Main.java`
- [ ] 4.2 Move `src/main/java/com/example/reactagent/ReactAgent.java` to `src/main/java/com/example/staffagent/agent/`
- [ ] 4.3 Update package declaration in `ReactAgent.java` from `com.example.reactagent` to `com.example.staffagent.agent`
- [ ] 4.4 Delete empty directory `src/main/java/com/example/reactagent/`
- [ ] 4.5 Delete empty directory `src/main/java/com/example/userguide/` (after all files moved)
- [ ] 4.6 Verify no remaining files in old package structure

## 5. Verification

- [ ] 5.1 Run `mvn clean compile` to verify compilation succeeds
- [ ] 5.2 Run `mvn test` to verify all tests pass (if tests exist)
- [ ] 5.3 Run `mvn spring-boot:run` to verify application starts successfully
- [ ] 5.4 Test REST endpoint `POST /api/agent/chat` with sample request
- [ ] 5.5 Test REST endpoint `GET /api/agent/name` to verify functionality
- [ ] 5.6 Verify no errors in application logs during startup and operation

## 6. Documentation Update

- [ ] 6.1 Update README.md if it references old package names or classes
- [ ] 6.2 Update any inline documentation or comments referencing old structure
- [ ] 6.3 Verify git commit message clearly documents all package name changes

## 7. Final Cleanup

- [ ] 7.1 Remove backup branch if created (`git branch -d backup-before-refactor`)
- [ ] 7.2 Verify IDE has no compilation errors or warnings
- [ ] 7.3 Verify `target/` directory contains compiled classes in new package structure