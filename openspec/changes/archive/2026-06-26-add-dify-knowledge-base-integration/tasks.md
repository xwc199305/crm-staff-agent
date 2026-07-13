## 1. Create Dify Client and Service

- [x] 1.1 Create DifyResponse DTO
- [x] 1.2 Create DifyClient wrapper for Dify API calls
- [x] 1.3 Create DifyKnowledgeBaseService interface
- [x] 1.4 Create DifyKnowledgeBaseServiceImpl implementation

## 2. Update Intent Handlers

- [x] 2.1 Modify ProductConsultationHandler to integrate knowledge base query
- [x] 2.2 Modify WarrantyPolicyHandler to integrate knowledge base query
- [x] 2.3 Modify AftersalesProcessHandler to integrate knowledge base query
- [x] 2.4 Modify OrderInquiryHandler to integrate knowledge base query

## 3. Configuration Updates

- [x] 3.1 Update application.properties to add Dify configuration items
- [x] 3.2 Add environment variable support

## 4. Verification Testing

- [x] 4.1 Compilation verification (mvn clean compile)
- [x] 4.2 Startup verification (mvn spring-boot:run)
- [x] 4.3 Test knowledge base query functionality
- [x] 4.4 Test fallback mechanism