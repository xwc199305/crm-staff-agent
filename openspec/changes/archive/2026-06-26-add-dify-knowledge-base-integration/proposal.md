## Why

Current intent handlers use hardcoded static knowledge bases that cannot be dynamically updated and extended. Integrating Dify knowledge base allows the customer service Agent to access more comprehensive and up-to-date product knowledge, warranty policies, aftersales processes, etc., improving answer quality and user experience.

## What Changes

- **New Dify Knowledge Base Client**: Create DifyClient wrapper for Dify API calls
- **New Knowledge Base Service**: Create DifyKnowledgeBaseService to provide knowledge base query capability
- **Update Intent Handlers**: Modify four intent handlers to prioritize Dify knowledge base queries
- **Add Configuration**: Add Dify API related configuration in application.properties
- **Add Dependencies**: Add Spring WebClient or RestTemplate dependency
- **Create DTO**: Create Dify API response data structure

## Capabilities

### New Capabilities
- `dify-knowledge-base`: Dify knowledge base query capability, supports sending queries to Dify knowledge base and getting answers

### Modified Capabilities
- `product-consultation`: Use Dify knowledge base to answer product consultation questions
- `warranty-policy`: Use Dify knowledge base to answer warranty policy questions
- `aftersales-process`: Use Dify knowledge base to answer aftersales process questions
- `order-inquiry`: Use Dify knowledge base to answer order inquiry questions

## Impact

**New Files:**
- `src/main/java/com/example/staffagent/dify/DifyClient.java` - Dify API client
- `src/main/java/com/example/staffagent/dify/DifyKnowledgeBaseService.java` - Knowledge base service
- `src/main/java/com/example/staffagent/dify/Dto/DifyResponse.java` - Dify API response DTO

**Modified Files:**
- `src/main/java/com/example/staffagent/handler/impl/ProductConsultationHandler.java` - Integrate knowledge base query
- `src/main/java/com/example/staffagent/handler/impl/WarrantyPolicyHandler.java` - Integrate knowledge base query
- `src/main/java/com/example/staffagent/handler/impl/AftersalesProcessHandler.java` - Integrate knowledge base query
- `src/main/java/com/example/staffagent/handler/impl/OrderInquiryHandler.java` - Integrate knowledge base query

**Configuration:**
- `application.properties` - Add Dify API configuration items

**Dependencies:**
- Spring WebClient (already included in Spring Boot Starter Web)