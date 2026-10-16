import os
import firebase_admin
from firebase_admin import credentials, firestore

cred_path = os.getenv("FIREBASE_CREDENTIALS_PATH")
cred = credentials.Certificate(cred_path)
firebase_admin.initialize_app(cred)

db = firestore.client()

def _resolve_by(field: str, value) -> str:
    """
    Busca al usuario por un identificador externo (Telegram, app, etc.).
    Si no existe, lo crea. Devuelve el internal_user_id (doc ID de Firestore).
    """
    users_ref = db.collection("users")
    existing = list(users_ref.where(field, "==", value).limit(1).stream())

    if existing:
        return existing[0].id

    new_user = users_ref.document()
    new_user.set({
        field: value,
        "profile": "standard",  # estándar por defecto; "assisted" se asigna manualmente después
    })
    return new_user.id


def resolve_user(telegram_user_id: int) -> str:
    return _resolve_by("telegram_user_id", telegram_user_id)


def resolve_app_user(install_id: str) -> str:
    return _resolve_by("app_install_id", install_id)