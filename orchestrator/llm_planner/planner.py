import os
import json
import uuid
from datetime import datetime, timezone
from google import genai
from google.genai import types, errors as genai_errors

API_KEY = os.getenv("LLM_API_KEY")
MODEL = os.getenv("GEMINI_MODEL", "gemini-3.1-flash-lite")
LOG_DIR = os.getenv("LLM_LOG_DIR", "/app/logs")

client = genai.Client(api_key=API_KEY)
os.makedirs(LOG_DIR, exist_ok=True)

SYSTEM_PROMPT_TEMPLATE = """Eres el planificador de un asistente personal. Tu tarea es traducir
el mensaje del usuario a UNA acción estructurada, usando SOLO las
herramientas declaradas abajo. Nunca ejecutes nada directamente,
nunca inventes una herramienta que no esté en la lista.

Herramientas disponibles:
{tools_list}

Si el mensaje pide ejecutar una acción y alguna herramienta (distinta de "chat") encaja:
{{"tool": "nombre_de_la_herramienta", "params": {{...}}}}

Si el mensaje es una pregunta general, un saludo, o conversación que no
requiere ejecutar ninguna acción, usa la herramienta "chat" y responde
directamente en el campo "answer":
{{"tool": "chat", "params": {{"answer": "tu respuesta aquí"}}}}
"""


def _build_system_prompt(available_tools: list[dict]) -> str:
    tools_list = "\n".join(f"- {t['name']}: {t['description']}" for t in available_tools)
    return SYSTEM_PROMPT_TEMPLATE.format(tools_list=tools_list)


def _save_log(entry: dict):
    timestamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%f")
    filename = f"{timestamp}_{uuid.uuid4().hex[:8]}.json"
    path = os.path.join(LOG_DIR, filename)
    try:
        with open(path, "w", encoding="utf-8") as f:
            json.dump(entry, f, ensure_ascii=False, indent=2)
    except OSError as e:
        print(f"[WARN] No se pudo guardar el log: {e}")


def plan_action(user_message: str, available_tools: list[dict]) -> dict:
    system_prompt = _build_system_prompt(available_tools)
    log_entry = {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "model": MODEL,
        "user_message": user_message,
    }

    try:
        response = client.models.generate_content(
            model=MODEL,
            contents=user_message,
            config=types.GenerateContentConfig(
                system_instruction=system_prompt,
                response_mime_type="application/json",
            ),
        )
    except genai_errors.APIError as e:
        log_entry["error"] = "llm_unavailable"
        log_entry["detail"] = str(e)
        _save_log(log_entry)
        return {"tool": None, "params": {}, "error": "llm_unavailable", "detail": str(e)}

    log_entry["raw_response"] = response.text

    try:
        decision = json.loads(response.text)
        log_entry["parsed_decision"] = decision
        _save_log(log_entry)
        return decision
    except (json.JSONDecodeError, TypeError):
        log_entry["error"] = "bad_json"
        _save_log(log_entry)
        return {"tool": None, "params": {}, "error": "bad_json", "raw": response.text}