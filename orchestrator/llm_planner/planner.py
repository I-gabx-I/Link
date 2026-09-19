import os
import json
from google import genai
from google.genai import types, errors as genai_errors

API_KEY = os.getenv("LLM_API_KEY")
MODEL_CANDIDATES = [m.strip() for m in os.getenv(
    "GEMINI_MODELS", "gemini-3.1-flash-lite,gemini-3-flash"
).split(",") if m.strip()]
LOG_DIR = os.getenv("LLM_LOG_DIR", "/app/logs")

client = genai.Client(api_key=API_KEY)
os.makedirs(LOG_DIR, exist_ok=True)

SYSTEM_PROMPT_TEMPLATE = """Eres el planificador de un asistente personal para tareas cotidianas
(calendario, mensajes, archivos, recordatorios). Tu tarea es traducir el
mensaje del usuario a UNA acción estructurada, usando SOLO las herramientas
declaradas abajo. Nunca ejecutes nada directamente, nunca inventes una
herramienta que no esté en la lista.

Herramientas disponibles:
{tools_list}

Si el mensaje pide ejecutar una acción y alguna herramienta (distinta de "chat") encaja:
{{"tool": "nombre_de_la_herramienta", "params": {{...}}}}

Usa "chat" SOLO para saludos, cortesías, o preguntas sobre qué puede hacer
este asistente:
{{"tool": "chat", "params": {{"answer": "tu respuesta breve aquí"}}}}

Para CUALQUIER otro tema fuera de gestionar tareas personales, responde:
{{"tool": null, "params": {{}}, "out_of_scope": true}}
"""


def _build_system_prompt(available_tools: list[dict]) -> str:
    tools_list = "\n".join(f"- {t['name']}: {t['description']}" for t in available_tools)
    return SYSTEM_PROMPT_TEMPLATE.format(tools_list=tools_list)


def _save_log(entry: dict):
    import uuid
    from datetime import datetime, timezone
    timestamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%f")
    filename = f"{timestamp}_{uuid.uuid4().hex[:8]}.json"
    path = os.path.join(LOG_DIR, filename)
    try:
        with open(path, "w", encoding="utf-8") as f:
            json.dump(entry, f, ensure_ascii=False, indent=2)
    except OSError as e:
        print(f"[WARN] No se pudo guardar el log: {e}")


def _call_gemini(user_message: str, system_prompt: str):
    """Intenta cada modelo de MODEL_CANDIDATES en orden. Devuelve (texto, modelo_usado)."""
    last_error = None
    for model_name in MODEL_CANDIDATES:
        try:
            response = client.models.generate_content(
                model=model_name,
                contents=user_message,
                config=types.GenerateContentConfig(
                    system_instruction=system_prompt,
                    response_mime_type="application/json",
                ),
            )
            return response.text, model_name
        except genai_errors.APIError as e:
            print(f"[WARN] Modelo '{model_name}' falló, probando el siguiente: {e}")
            last_error = e
            continue
    raise last_error


def plan_action(user_message: str, available_tools: list[dict]) -> dict:
    system_prompt = _build_system_prompt(available_tools)
    from datetime import datetime, timezone
    log_entry = {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "models_disponibles": MODEL_CANDIDATES,
        "user_message": user_message,
    }

    try:
        raw_text, model_used = _call_gemini(user_message, system_prompt)
    except genai_errors.APIError as e:
        log_entry["error"] = "llm_unavailable"
        log_entry["detail"] = str(e)
        _save_log(log_entry)
        return {"tool": None, "params": {}, "error": "llm_unavailable", "detail": str(e)}

    log_entry["model_used"] = model_used
    log_entry["raw_response"] = raw_text

    try:
        decision = json.loads(raw_text)
        log_entry["parsed_decision"] = decision
        _save_log(log_entry)
        return decision
    except (json.JSONDecodeError, TypeError):
        log_entry["error"] = "bad_json"
        _save_log(log_entry)
        return {"tool": None, "params": {}, "error": "bad_json", "raw": raw_text}