## Why

The current project is a basic ReactAgent framework lacking professional intent recognition capabilities for e-commerce customer service scenarios. E-commerce customer service needs to handle a large volume of user inquiries covering product usage, warranty, aftersales, orders, and other domains. An intent recognition module can accurately classify user requests and route them to corresponding processing logic, improving customer service efficiency and user experience.

## What Changes

- **Add intent recognition module**: Create a dedicated intent recognition service supporting four core intent categories
- **Intent classification**: Implement recognition logic for four intent categories: product usage consultation, warranty policy, aftersales process, and order inquiry
- **Intent enumeration**: Define IntentType enum class for unified intent category management
- **Intent recognition service**: Create IntentRecognizer service interface and implementation class
- **Intent handlers**: Create corresponding handlers for each intent type with specific business logic
- **Conversation flow management**: Add conversation context management supporting multi-turn conversations
- **Update controller**: Modify ReactAgentController to integrate intent recognition functionality
- **DTO updates**: Add intent recognition related data transfer objects

## Capabilities

### New Capabilities
- `intent-recognition`: E-commerce customer service intent recognition capability, supporting automatic classification and routing of four core intents
- `product-consultation`: Product usage consultation intent handling capability, answering product usage related questions
- `warranty-policy`: Warranty policy intent handling capability, providing warranty terms and duration information
- `aftersales-process`: Aftersales process intent handling capability, guiding users through return and exchange processes
- `order-inquiry`: Order inquiry intent handling capability, querying order status and logistics information

### Modified Capabilities
- `agent-integration`: Integrate intent recognition module into existing Agent services, enabling intelligent conversation routing

## Impact

**Affected Code:**
- `src/main/java/com/example/staffagent/controller/ReactAgentController.java` - Need to update to support intent recognition
- `src/main/java/com/example/staffagent/service/ReactAgentServiceImpl.java` - Need to integrate intent recognition service

**New Files:**
- `src/main/java/com/example/staffagent/intent/IntentType.java` - Intent type enumeration
- `src/main/java/com/example/staffagent/intent/IntentRecognizer.java` - Intent recognition interface
- `src/main/java/com/example/staffagent/intent/impl/IntentRecognizerImpl.java` - Intent recognition implementation
- `src/main/java/com/example/staffagent/handler/IntentHandler.java` - Intent handler interface
- `src/main/java/com/example/staffagent/handler/ProductConsultationHandler.java` - Product consultation handler
- `src/main/java/com/example/staffagent/handler/WarrantyPolicyHandler.java` - Warranty policy handler
- `src/main/java/com/example/staffagent/handler/AftersalesProcessHandler.java` - Aftersales process handler
- `src/main/java/com/example/staffagent/handler/OrderInquiryHandler.java` - Order inquiry handler
- `src/main/java/com/example/staffagent/dto/IntentResult.java` - Intent recognition result DTO
- `src/main/java/com/example/staffagent/dto/ChatResponse.java` - Chat response DTO

**Configuration:**
- `application.properties` - Add intent recognition related configuration

**Dependencies:**
- No new external dependencies required, use existing AgentScope model for intent recognition
