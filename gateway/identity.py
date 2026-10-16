import os
import hashlib
import firebase_admin
from firebase_admin import credentials, firestore

cred_path = os.getenv("FIREBASE_CREDENTIALS_PATH")
if not firebase_admin._apps:
    cred = credentials.Certificate(cred_path) if cred_path else None
    options = {"projectId": os.environ["FIREBASE_PROJECT_ID"]} if os.getenv("FIREBASE_PROJECT_ID") else None
    firebase_admin.initialize_app(cred, options=options)

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
    # Reuse profiles already created by the first app adapter.
    users = db.collection("users")
    existing = list(users.where("app_install_id", "==", install_id).limit(1).stream())
    if existing:
        return existing[0].id
    # Stable document IDs prevent duplicate profiles from concurrent first requests.
    user = users.document("app_" + hashlib.sha256(install_id.encode()).hexdigest())
    if not user.get().exists:
        from google.api_core.exceptions import AlreadyExists
        try:
            user.create({"app_install_id": install_id, "profile": "standard"})
        except AlreadyExists:
            pass
    return user.id
