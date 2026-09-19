import os
import urllib.parse

import firebase_admin
from firebase_admin import credentials, firestore
from fastapi import FastAPI
from pydantic import BaseModel

from capabilities import CAPABILITIES

cred_path = os.getenv("FIREBASE_CREDENTIALS_PATH")
if not firebase_admin._apps:
    cred = credentials.Certificate(cred_path)
    firebase_admin.initialize_app(cred)
db = firestore.client()

app = FastAPI(title="TelegramAgent")


class ExecuteRequest(BaseModel):
    tool: str
    params: dict
    internal_user_id: str = ""


def _get_contacts(internal_user_id: str) -> dict:
    doc = db.collection("contacts").document(internal_user_id).get()
    return doc.to_dict() if doc.exists else {}


def _add_contact(internal_user_id: str, name: str, username: str) -> str:
    clean_username = username.strip().lstrip("@")
    db.collection("contacts").document(internal_user_id).set(
        {name.strip().lower(): clean_username}, merge=True
    )
    return f"Guardé el contacto '{name}' (@{clean_username})."


def _build_send_link(internal_user_id: str, contact: str, message: str) -> str:
    contacts = _get_contacts(internal_user_id)
    username = contacts.get(contact.strip().lower())
    if not username:
        return (
            f"No tengo guardado un contacto llamado '{contact}'. "
            f"Agregalo primero, por ejemplo: 'guarda el contacto {contact} con usuario @su_usuario_de_telegram'."
        )
    encoded = urllib.parse.quote(message)
    link = f"https://t.me/{username}?text={encoded}"
    return f"Tocá este link para enviarle el mensaje a {contact}: {link}"


@app.get("/")
def health_check():
    return {"status": "agent_telegram alive", "capabilities": list(CAPABILITIES.keys())}


@app.post("/execute")
def execute(req: ExecuteRequest):
    try:
        if req.tool == "contact_add":
            p = req.params
            return {"result": _add_contact(req.internal_user_id, p["name"], p["username"])}
        if req.tool == "telegram_send":
            p = req.params
            return {"result": _build_send_link(req.internal_user_id, p["contact"], p["message"])}
        return {"error": f"capability '{req.tool}' not declared"}
    except Exception as e:
        return {"error": f"No pude completar la acción de Telegram: {e}"}