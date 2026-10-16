import os
import httpx
from identity import resolve_app_user

ORCHESTRATOR_URL = os.getenv("ORCHESTRATOR_URL", "http://orchestrator:8001")


async def handle_app_message(install_id: str, message: str) -> dict:
    internal_user_id = resolve_app_user(install_id)

    try:
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(
                f"{ORCHESTRATOR_URL}/process",
                json={"message": message, "internal_user_id": internal_user_id},
            )
            response.raise_for_status()
            return response.json()
    except httpx.HTTPError:
        return {"type": "error", "reply": "No pude comunicarme con el asistente. Intentá de nuevo."}