from fastapi import FastAPI
from pydantic import BaseModel
import httpx

from llm_planner.planner import plan_action
from llm_planner.tools_registry import AVAILABLE_TOOLS

app = FastAPI(title="Orchestrator")

AGENT_URLS = {
    "echo": "http://agent_echo:8003",
}


class IncomingMessage(BaseModel):
    message: str
    internal_user_id: str


@app.get("/")
def health_check():
    return {"status": "orchestrator alive"}


@app.post("/process")
async def process(msg: IncomingMessage):
    decision = plan_action(msg.message, AVAILABLE_TOOLS)

    if decision.get("error") == "llm_unavailable":
        return {"reply": "El asistente está saturado en este momento, por favor intenta de nuevo en unos segundos."}

    tool = decision.get("tool")

    if tool == "chat":
        answer = decision.get("params", {}).get("answer", "No tengo una respuesta para eso.")
        return {"reply": answer}

    if not tool or tool not in AGENT_URLS:
        return {"reply": "No encontré una acción para hacer eso todavía."}

    agent_url = AGENT_URLS[tool]
    params = decision.get("params", {})

    async with httpx.AsyncClient(timeout=10.0) as client:
        agent_response = await client.post(
            f"{agent_url}/execute",
            json={"tool": tool, "params": params},
        )
    result = agent_response.json()
    return {"reply": result.get("result", "Sin respuesta del agente")}