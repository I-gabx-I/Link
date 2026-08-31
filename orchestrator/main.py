from fastapi import FastAPI
from pydantic import BaseModel
import httpx

app = FastAPI(title="Orchestrator")

AGENT_ECHO_URL = "http://agent_echo:8003"


class IncomingMessage(BaseModel):
    message: str
    telegram_user_id: int


@app.get("/")
def health_check():
    return {"status": "orchestrator alive"}


@app.post("/process")
async def process(msg: IncomingMessage):
    async with httpx.AsyncClient(timeout=10.0) as client:
        agent_response = await client.post(
            f"{AGENT_ECHO_URL}/execute",
            json={"tool": "echo", "params": {"message": msg.message}},
        )
    result = agent_response.json()
    return {"reply": result.get("result", "Sin respuesta del agente")}