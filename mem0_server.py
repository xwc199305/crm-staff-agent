from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from typing import Optional, List, Dict, Any, Union
from mem0 import Memory
from mem0.configs.base import MemoryConfig, VectorStoreConfig, EmbedderConfig, LlmConfig
import uvicorn
import logging
import os

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="mem0 Memory Server", version="1.0")

DASHSCOPE_API_KEY = os.getenv("DASHSCOPE_API_KEY", os.getenv("agent.api-key", ""))
USE_DASHSCOPE = DASHSCOPE_API_KEY is not None and DASHSCOPE_API_KEY != ""

if USE_DASHSCOPE:
    logger.info("Using DashScope for LLM and embedding (model: qwen-max, embedding: text-embedding-v1)")
    embedder_config = EmbedderConfig(
        provider="openai",
        config={
            "model": "text-embedding-v1",
            "api_key": DASHSCOPE_API_KEY,
            "openai_base_url": "https://dashscope.aliyuncs.com/compatible-mode/v1",
            "embedding_dims": 1536
        }
    )
    llm_config = LlmConfig(
        provider="openai",
        config={
            "model": "qwen-max",
            "api_key": DASHSCOPE_API_KEY,
            "openai_base_url": "https://dashscope.aliyuncs.com/compatible-mode/v1",
            "temperature": 0.1
        }
    )
    vector_dims = 1536
else:
    logger.info("Using fastembed + ollama local models (no DashScope API key configured)")
    embedder_config = EmbedderConfig(
        provider="fastembed",
        config={}
    )
    llm_config = LlmConfig(
        provider="ollama",
        config={"model": "llama3.1", "ollama_base_url": "http://localhost:11434"}
    )
    vector_dims = 384

memory = Memory(
    config=MemoryConfig(
        vector_store=VectorStoreConfig(
            provider="chroma",
            config={
                "collection_name": "mem0",
                "path": "/tmp/chroma"
            }
        ),
        embedder=embedder_config,
        llm=llm_config,
        history_db_path="/tmp/mem0/history.db"
    )
)

logger.info("mem0 Memory Server initialized with local Chroma storage")


class AddMemoryRequest(BaseModel):
    messages: Union[str, List[Dict[str, str]]]
    user_id: str
    agent_id: Optional[str] = None
    run_id: Optional[str] = None
    metadata: Optional[Dict[str, Any]] = None
    infer: bool = True


class SearchMemoryRequest(BaseModel):
    query: str
    user_id: Optional[str] = None
    agent_id: Optional[str] = None
    run_id: Optional[str] = None
    filters: Optional[Dict[str, Any]] = None
    top_k: int = 5


class DeleteMemoryRequest(BaseModel):
    user_id: Optional[str] = None
    agent_id: Optional[str] = None
    run_id: Optional[str] = None


@app.post("/api/memories", response_model=Dict[str, Any])
async def add_memory(request: AddMemoryRequest):
    try:
        logger.info(f"Adding memory for user: {request.user_id}")
        result = memory.add(
            messages=request.messages,
            user_id=request.user_id,
            agent_id=request.agent_id,
            run_id=request.run_id,
            metadata=request.metadata,
            infer=request.infer
        )
        return {"status": "success", "result": result}
    except Exception as e:
        logger.error(f"Error adding memory: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/memories/{user_id}", response_model=Dict[str, Any])
async def get_user_memories(user_id: str):
    try:
        logger.info(f"Getting memories for user: {user_id}")
        memories = memory.search(
            query="",
            filters={"user_id": user_id},
            top_k=100
        )
        return {"status": "success", "user_id": user_id, "memories": memories}
    except Exception as e:
        logger.error(f"Error getting memories: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/memories/search", response_model=Dict[str, Any])
async def search_memories(request: SearchMemoryRequest):
    try:
        logger.info(f"Searching memories for user: {request.user_id}, query: {request.query[:50]}...")
        
        filters = request.filters or {}
        if request.user_id:
            filters["user_id"] = request.user_id
        if request.agent_id:
            filters["agent_id"] = request.agent_id
        if request.run_id:
            filters["run_id"] = request.run_id
        
        result = memory.search(
            query=request.query,
            filters=filters if filters else None,
            top_k=request.top_k
        )
        return {"status": "success", "query": request.query, "results": result}
    except Exception as e:
        logger.error(f"Error searching memories: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@app.delete("/api/memories/{memory_id}", response_model=Dict[str, Any])
async def delete_memory(memory_id: str):
    try:
        logger.info(f"Deleting memory: {memory_id}")
        result = memory.delete(memory_id=memory_id)
        return {"status": "success", "memory_id": memory_id, "result": result}
    except Exception as e:
        logger.error(f"Error deleting memory: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@app.delete("/api/memories/user/{user_id}", response_model=Dict[str, Any])
async def delete_user_memories(user_id: str):
    try:
        logger.info(f"Deleting all memories for user: {user_id}")
        result = memory.delete_all(user_id=user_id)
        return {"status": "success", "user_id": user_id, "result": result}
    except Exception as e:
        logger.error(f"Error deleting user memories: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/health")
async def health_check():
    return {"status": "healthy", "service": "mem0-server"}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8283)
