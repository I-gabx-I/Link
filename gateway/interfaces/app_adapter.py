import os
import asyncio
import httpx
from identity import resolve_app_user

ORCHESTRATOR_URL = os.getenv("ORCHESTRATOR_URL", "http://orchestrator:8001")


async def handle_app_message(install_id: str, message: str) -> dict:
    internal_user_id = await asyncio.to_thread(resolve_app_user, install_id)

    try:
        async with httpx.AsyncClient(timeout=90.0) as client:
            response = await client.post(
                f"{ORCHESTRATOR_URL}/process",
                json={"message": message, "internal_user_id": internal_user_id},
            )
            response.raise_for_status()
            return response.json()
    except httpx.HTTPError:
        return {"type": "error", "reply": "No pude comunicarme con el asistente. Intentá de nuevo."}


async def handle_app_contacts(install_id: str, contact: dict | None = None) -> dict:
    internal_user_id = await asyncio.to_thread(resolve_app_user, install_id)
    path = "/contacts/save" if contact is not None else "/contacts/list"
    payload = {"internal_user_id": internal_user_id, "params": contact or {}}
    try:
        async with httpx.AsyncClient(timeout=20.0) as client:
            response = await client.post(f"{ORCHESTRATOR_URL}{path}", json=payload)
            response.raise_for_status()
            return response.json()
    except (httpx.HTTPError, ValueError):
        return {"type": "error", "reply": "No pude acceder a los contactos. Comprueba que el backend esté funcionando."}
