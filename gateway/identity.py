import os
import firebase_admin
from firebase_admin import credentials, firestore

cred_path = os.getenv("FIREBASE_CREDENTIALS_PATH")
cred = credentials.Certificate(cred_path)
firebase_admin.initialize_app(cred)

db = firestore.client()


def resolve_user(telegram_user_id: int) -> str:
    """
    Busca al usuario por su telegram_user_id. Si no existe, lo crea.
    Devuelve el internal_user_id (por ahora, el mismo doc ID de Firestore).
    """
    users_ref = db.collection("users")
    query = users_ref.where("telegram_user_id", "==", telegram_user_id).limit(1).stream()
    existing = list(query)

    if existing:
        return existing[0].id

    new_user = users_ref.document()
    new_user.set({
        "telegram_user_id": telegram_user_id,
        "profile": "standard",  # estándar por defecto; "assisted" se asigna manualmente después
    })
    return new_user.id