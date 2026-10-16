import os
import urllib.parse
import re
import unicodedata

import firebase_admin
from firebase_admin import credentials, firestore
from fastapi import FastAPI
from pydantic import BaseModel

from capabilities import CAPABILITIES

cred_path = os.getenv("FIREBASE_CREDENTIALS_PATH")
if not firebase_admin._apps:
    cred = credentials.Certificate(cred_path) if cred_path else None
    options = {"projectId": os.environ["FIREBASE_PROJECT_ID"]} if os.getenv("FIREBASE_PROJECT_ID") else None
    firebase_admin.initialize_app(cred, options=options)
db = firestore.client()

app = FastAPI(title="TelegramAgent")


class ExecuteRequest(BaseModel):
    tool: str
    params: dict
    internal_user_id: str = ""


def _get_contacts(internal_user_id: str) -> dict:
    doc = db.collection("contacts").document(internal_user_id).get()
    return doc.to_dict() if doc.exists else {}


def _normalize(value: str) -> str:
    value = "".join(c for c in unicodedata.normalize("NFD", value) if unicodedata.category(c) != "Mn")
    return " ".join(value.casefold().split())


def _list_contacts(internal_user_id: str) -> list[dict]:
    contacts = []
    for key, value in _get_contacts(internal_user_id).items():
        if isinstance(value, str):
            contacts.append({"name": key, "username": value, "aliases": []})
        elif isinstance(value, dict) and value.get("username"):
            contacts.append({"name": value.get("name", key), "username": value["username"], "aliases": value.get("aliases", [])})
    return sorted(contacts, key=lambda contact: _normalize(contact["name"]))


def _add_contact(internal_user_id: str, name: str, username: str, aliases: list[str] | None = None) -> str:
    name = name.strip()
    clean_username = username.strip().lstrip("@")
    if not name or len(name) > 80:
        raise ValueError("Escribe un nombre de entre 1 y 80 caracteres.")
    if not re.fullmatch(r"[A-Za-z][A-Za-z0-9_]{0,31}", clean_username):
        raise ValueError("Escribe el usuario de Telegram sin enlaces ni espacios.")
    if aliases is not None and (not isinstance(aliases, list) or any(not isinstance(alias, str) for alias in aliases)):
        raise ValueError("Los alias deben ser una lista de nombres.")
    aliases = list(dict.fromkeys(alias.strip() for alias in (aliases or []) if alias.strip()))
    if len(aliases) > 8 or any(len(alias) > 80 for alias in aliases):
        raise ValueError("Usa hasta ocho alias cortos para este contacto.")
    names = {_normalize(name), *(_normalize(alias) for alias in aliases)}
    for contact in _list_contacts(internal_user_id):
        if _normalize(contact["name"]) == _normalize(name):
            continue
        existing_names = {_normalize(contact["name"]), *(_normalize(alias) for alias in contact["aliases"])}
        if names & existing_names:
            raise ValueError("Ese nombre o alias ya corresponde a otro contacto.")
    storage_key = name.lower()
    for key, value in _get_contacts(internal_user_id).items():
        existing_name = value.get("name", key) if isinstance(value, dict) else key
        if _normalize(existing_name) == _normalize(name):
            storage_key = key
            break
    db.collection("contacts").document(internal_user_id).set(
        {storage_key: {"name": name, "username": clean_username, "aliases": aliases}}, merge=True
    )
    return f"Guardé el contacto '{name}' (@{clean_username})."


def _build_send_link(internal_user_id: str, contact: str, message: str) -> dict:
    contacts = _list_contacts(internal_user_id)
    matches = [item for item in contacts if _normalize(contact) in {_normalize(item["name"]), *(_normalize(alias) for alias in item["aliases"])}]
    if len(matches) != 1:
        return {"error": f"No encuentro un contacto único llamado '{contact}'. Revisa tus contactos."}
    if not message.strip() or len(message) > 4000:
        return {"error": "El mensaje debe tener entre 1 y 4000 caracteres."}
    username = matches[0]["username"]
    if not re.fullmatch(r"[A-Za-z][A-Za-z0-9_]{0,31}", username):
        return {"error": "El usuario de Telegram guardado no es válido. Edita el contacto."}
    encoded = urllib.parse.quote(message)
    link = f"https://t.me/{username}?text={encoded}"
    return {"result": f"El mensaje para {matches[0]['name']} está preparado. Ábrelo en Telegram y toca Enviar.", "action_url": link}


@app.get("/")
def health_check():
    return {"status": "agent_telegram alive", "capabilities": list(CAPABILITIES.keys())}


@app.post("/execute")
def execute(req: ExecuteRequest):
    try:
        if not req.internal_user_id:
            return {"error": "Falta identificar al usuario."}
        if req.tool == "contact_list":
            contacts = _list_contacts(req.internal_user_id)
            return {"result": "Tus contactos están actualizados.", "contacts": contacts}
        if req.tool == "contact_add":
            p = req.params
            result = _add_contact(req.internal_user_id, p["name"], p["username"], p.get("aliases", []))
            return {"result": result, "contacts": _list_contacts(req.internal_user_id)}
        if req.tool == "telegram_send":
            p = req.params
            return _build_send_link(req.internal_user_id, p["contact"], p["message"])
        return {"error": f"capability '{req.tool}' not declared"}
    except Exception as e:
        return {"error": f"No pude completar la acción de Telegram: {e}"}
