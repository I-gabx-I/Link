import asyncio
import os
import unicodedata

import httpx
from fastapi import FastAPI
from pydantic import BaseModel, Field

from llm_planner.planner import plan_action
from llm_planner.tools_registry import AVAILABLE_TOOLS
from policy_engine.policy import evaluate as policy_evaluate
from policy_engine import pending_actions as pending

app = FastAPI(title="Orchestrator")
AGENT_URLS = {
    "echo": os.getenv("AGENT_ECHO_URL", "http://agent_echo:8003"),
    "calendar_read": os.getenv("AGENT_CALENDAR_URL", "http://agent_calendar:8004"),
    "calendar_create": os.getenv("AGENT_CALENDAR_URL", "http://agent_calendar:8004"),
    "file_read": os.getenv("AGENT_FILE_URL", "http://agent_file:8005"),
    "contact_list": os.getenv("AGENT_TELEGRAM_URL", "http://agent_telegram:8006"),
    "contact_add": os.getenv("AGENT_TELEGRAM_URL", "http://agent_telegram:8006"),
    "telegram_send": os.getenv("AGENT_TELEGRAM_URL", "http://agent_telegram:8006"),
}


class IncomingMessage(BaseModel):
    message: str = Field(min_length=1, max_length=4000)
    internal_user_id: str = Field(min_length=1)


class ContactRequest(BaseModel):
    internal_user_id: str = Field(min_length=1)
    params: dict = Field(default_factory=dict)


@app.get("/")
def health_check():
    return {"status": "orchestrator alive"}


def _normalize(value: str) -> str:
    return " ".join("".join(c for c in unicodedata.normalize("NFD", value) if unicodedata.category(c) != "Mn").casefold().split())


def _summarize_action(tool: str, params: dict) -> str:
    if tool == "calendar_create":
        return f"Crear el evento '{params.get('summary')}' el {params.get('date')} a las {params.get('time')}."
    if tool == "telegram_send":
        return f"Preparar un mensaje para {params['contact']}: «{params['message']}»."
    return f"Ejecutar '{tool}'."


async def _call_agent(tool: str, params: dict, user_id: str) -> dict:
    if tool not in AGENT_URLS:
        return {"error": "La herramienta no está disponible."}
    try:
        async with httpx.AsyncClient(timeout=20.0) as client:
            response = await client.post(
                AGENT_URLS[tool].rstrip("/") + "/execute",
                json={"tool": tool, "params": params, "internal_user_id": user_id},
            )
            response.raise_for_status()
            result = response.json()
            if not isinstance(result, dict):
                return {"error": "El agente devolvió una respuesta incompleta."}
            return result
    except (httpx.HTTPError, ValueError):
        return {"error": "No pude comunicarme con ese agente. Comprueba que el backend esté funcionando."}


async def _execute_tool(tool: str, params: dict, user_id: str) -> dict:
    result = await _call_agent(tool, params, user_id)
    if result.get("error"):
        return {"type": "error", "reply": result["error"]}
    return {"type": "result", "reply": result.get("result", "Listo."),
            "action_url": result.get("action_url"), "contacts": result.get("contacts")}


async def _allowed(tool: str, user_id: str) -> bool:
    policy = await asyncio.to_thread(policy_evaluate, tool, user_id)
    return policy["allowed"] is not False


@app.post("/contacts/list")
async def list_contacts(request: ContactRequest):
    if not await _allowed("contact_list", request.internal_user_id):
        return {"type": "denied", "reply": "No tienes permiso para consultar los contactos."}
    return await _execute_tool("contact_list", {}, request.internal_user_id)


@app.post("/contacts/save")
async def save_contact(request: ContactRequest):
    if not await _allowed("contact_add", request.internal_user_id):
        return {"type": "denied", "reply": "No tienes permiso para guardar contactos."}
    return await _execute_tool("contact_add", request.params, request.internal_user_id)


REQUIRED = {
    "telegram_send": {"contact": "¿A quién quieres escribirle?", "message": "¿Qué mensaje quieres enviar?"},
    "contact_add": {"name": "¿Cómo se llama el contacto?", "username": "¿Cuál es su usuario de Telegram?"},
    "calendar_create": {"summary": "¿Cuál es el título de la cita?", "date": "¿Qué día será la cita?", "time": "¿A qué hora será la cita?"},
    "file_read": {"query": "¿Qué archivo quieres buscar?"},
}


async def _clarify(user_id: str, tool: str, params: dict, question: str) -> dict:
    await asyncio.to_thread(pending.save_draft, user_id, {"tool": tool, "params": params})
    return {"type": "clarification", "reply": question}


@app.post("/process")
async def process(message: IncomingMessage):
    user_id = message.internal_user_id
    queued = await asyncio.to_thread(pending.get_pending, user_id)
    if queued:
        verdict = pending.classify_confirmation(message.message)
        if verdict == "no":
            await asyncio.to_thread(pending.clear_pending, user_id)
            return {"type": "info", "reply": "Listo, cancelé esa acción."}
        if verdict != "yes":
            return {"type": "confirmation", "reply": "Confirma o cancela la acción pendiente antes de continuar.", "summary": queued["summary"]}
        if queued["tool"] not in {item["name"] for item in AVAILABLE_TOOLS} or not await _allowed(queued["tool"], user_id):
            await asyncio.to_thread(pending.clear_pending, user_id)
            return {"type": "denied", "reply": "La acción ya no está permitida; la cancelé."}
        await asyncio.to_thread(pending.clear_pending, user_id)
        return await _execute_tool(queued["tool"], queued["params"], user_id)

    draft = await asyncio.to_thread(pending.get_draft, user_id)
    if draft and _normalize(message.message) in {"cancelar", "cancela", "no", "olvidalo"}:
        await asyncio.to_thread(pending.clear_draft, user_id)
        return {"type": "info", "reply": "Listo, cancelé la solicitud."}

    contacts_result = await _call_agent("contact_list", {}, user_id) if await _allowed("contact_list", user_id) else {"contacts": [], "error": "No tienes permiso para consultar los contactos."}
    contacts = contacts_result.get("contacts", [])
    context = {"contacts": [{"name": item["name"], "aliases": item.get("aliases", [])} for item in contacts], "draft": draft}
    decision = await asyncio.to_thread(plan_action, message.message, AVAILABLE_TOOLS, context)
    if decision.get("error"):
        return {"type": "error", "reply": "No pude interpretar la solicitud. Intenta de nuevo en un momento."}
    if decision.get("out_of_scope"):
        return {"type": "info", "reply": "Por ahora puedo ayudarte con las acciones que tienes habilitadas. Prueba pedir un mensaje para uno de tus contactos."}
    tool = decision.get("tool")
    if tool not in {item["name"] for item in AVAILABLE_TOOLS}:
        return {"type": "info", "reply": "No encontré una acción disponible para esa solicitud."}
    if not await _allowed(tool, user_id):
        return {"type": "denied", "reply": "No tienes permiso para realizar esa acción."}
    params = decision.get("params", {})
    for key, question in REQUIRED.get(tool, {}).items():
        if not isinstance(params.get(key), str) or not params[key].strip():
            return await _clarify(user_id, tool, params, question)
        params[key] = params[key].strip()

    if tool == "telegram_send":
        if contacts_result.get("error"):
            return {"type": "error", "reply": contacts_result["error"]}
        matches = [item for item in contacts if _normalize(params["contact"]) in {_normalize(item["name"]), *(_normalize(alias) for alias in item.get("aliases", []))}]
        if len(matches) != 1:
            requested = params.pop("contact")
            return await _clarify(user_id, tool, params, f"No encuentro un contacto único llamado '{requested}'. Agrega el contacto o dime el nombre con el que lo guardaste.")
        params["contact"] = matches[0]["name"]

    await asyncio.to_thread(pending.clear_draft, user_id)
    if tool == "chat":
        return {"type": "info", "reply": params.get("answer", "Hola. ¿A quién quieres escribirle?")}
    policy = await asyncio.to_thread(policy_evaluate, tool, user_id)
    if policy["allowed"] is False:
        return {"type": "denied", "reply": "La acción ya no está permitida."}
    if policy["allowed"] == "requiere_confirmacion":
        summary = _summarize_action(tool, params)
        await asyncio.to_thread(pending.save_pending, user_id, tool, params, summary)
        return {"type": "confirmation", "reply": "Revisa la acción antes de confirmar.", "summary": summary}
    return await _execute_tool(tool, params, user_id)
