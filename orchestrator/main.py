from fastapi import FastAPI
from pydantic import BaseModel
import httpx

from llm_planner.planner import plan_action
from llm_planner.tools_registry import AVAILABLE_TOOLS
from policy_engine.policy import evaluate as policy_evaluate
from policy_engine import pending_actions as pending

app = FastAPI(title="Orchestrator")

AGENT_URLS = {
    "echo": "http://agent_echo:8003",
    "calendar_read": "http://agent_calendar:8004",
    "calendar_create": "http://agent_calendar:8004",
    "file_read": "http://agent_file:8005",
    "contact_add": "http://agent_telegram:8006",
    "telegram_send": "http://agent_telegram:8006",


}




class IncomingMessage(BaseModel):
    message: str
    internal_user_id: str


@app.get("/")
def health_check():
    return {"status": "orchestrator alive"}


def _summarize_action(tool: str, params: dict) -> str:
    if tool == "calendar_create":
        return f"Crear el evento '{params.get('summary')}' el {params.get('date')} a las {params.get('time')}."
    if tool == "telegram_send":
        return f"Enviarle un mensaje a {params.get('contact')}: \"{params.get('message')}\"."
    return f"Ejecutar '{tool}'."

async def _execute_tool(tool: str, params: dict, internal_user_id: str) -> dict:
    if tool not in AGENT_URLS:
        return {"reply": "No encontré una acción para hacer eso todavía."}
    agent_url = AGENT_URLS[tool]
    async with httpx.AsyncClient(timeout=15.0) as client:
        agent_response = await client.post(
            f"{agent_url}/execute",
            json={"tool": tool, "params": params, "internal_user_id": internal_user_id},
        )
    result = agent_response.json()
    if "error" in result:
        return {"reply": f"Hubo un problema ejecutando la acción: {result['error']}"}
    return {"reply": result.get("result", "Sin respuesta del agente")}

@app.post("/process")
async def process(msg: IncomingMessage):
    pending_action = pending.get_pending(msg.internal_user_id)
    if pending_action:
        verdict = pending.classify_confirmation(msg.message)
        if verdict == "yes":
            pending.clear_pending(msg.internal_user_id)
            return await _execute_tool(pending_action["tool"], pending_action["params"], msg.internal_user_id)
        if verdict == "no":
            pending.clear_pending(msg.internal_user_id)
            return {"reply": "Listo, cancelé esa acción."}
        return {"reply": f"Tenés una acción pendiente de confirmar: {pending_action['summary']} Respondé 'sí' o 'no'."}

    decision = plan_action(msg.message, AVAILABLE_TOOLS)

    if decision.get("error") == "llm_unavailable":
        return {"reply": "El asistente está saturado en este momento, por favor intenta de nuevo en unos segundos."}

    if decision.get("out_of_scope"):
        return {"reply": "Ese tema está fuera de lo que este asistente puede ayudarte a resolver."}

    tool = decision.get("tool")
    if not tool:
        return {"reply": "No encontré una acción para hacer eso todavía."}

    policy = policy_evaluate(tool, msg.internal_user_id)

    if policy["allowed"] is False:
        if policy["reason"] == "sin_permiso":
            return {"reply": f"No tienes permiso otorgado para usar '{tool}' en este momento."}
        if policy["reason"] == "riesgo_bloqueado":
            return {"reply": "Esa acción está bloqueada por su nivel de riesgo."}
        return {"reply": "No encontré una acción para hacer eso todavía."}

    params = decision.get("params", {})

    if policy["allowed"] == "requiere_confirmacion":
        summary = _summarize_action(tool, params)
        pending.save_pending(msg.internal_user_id, tool, params, summary)
        return {"reply": f"¿Confirmás esta acción? {summary} Respondé 'sí' o 'no'."}

    if tool == "chat":
        answer = params.get("answer", "¡Hola! ¿En qué puedo ayudarte?")
        return {"reply": answer}

    return await _execute_tool(tool, params, msg.internal_user_id)