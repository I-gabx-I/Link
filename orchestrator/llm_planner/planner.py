import os
import json
from google import genai
from google.genai import types

API_KEY = os.getenv("LLM_API_KEY")
MODEL = os.getenv("GEMINI_MODEL", "gemini-3.1-flash-lite")

client = genai.Client(api_key=API_KEY)

SYSTEM_PROMPT_TEMPLATE = """Eres el planificador de un asistente personal. Tu única tarea
es traducir el mensaje del usuario a UNA acción estructurada, usando
SOLO las herramientas declaradas abajo. Nunca ejecutes nada, nunca
inventes una herramienta que no esté en la lista.

Herramientas disponibles:
{tools_list}

Responde SIEMPRE en este formato JSON, sin texto adicional:
{{"tool": "nombre_de_la_herramienta", "params": {{...}}}}

Si ninguna herramienta encaja con lo que pide el usuario, responde:
{{"tool": null, "params": {{}}}}
"""


def _build_system_prompt(available_tools: list[dict]) -> str:
    tools_list = "\n".join(f"- {t['name']}: {t['description']}" for t in available_tools)
    return SYSTEM_PROMPT_TEMPLATE.format(tools_list=tools_list)


def plan_action(user_message: str, available_tools: list[dict]) -> dict:
    system_prompt = _build_system_prompt(available_tools)

    response = client.models.generate_content(
        model=MODEL,
        contents=user_message,
        config=types.GenerateContentConfig(
            system_instruction=system_prompt,
            response_mime_type="application/json",
        ),
    )

    try:
        return json.loads(response.text)
    except (json.JSONDecodeError, TypeError):
        return {"tool": None, "params": {}, "error": "Respuesta no interpretable", "raw": response.text}