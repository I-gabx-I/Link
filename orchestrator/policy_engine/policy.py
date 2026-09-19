import os
import firebase_admin
from firebase_admin import credentials, firestore

cred_path = os.getenv("FIREBASE_CREDENTIALS_PATH")

if not firebase_admin._apps:
    cred = credentials.Certificate(cred_path)
    firebase_admin.initialize_app(cred)

db = firestore.client()

# Fase 3: tabla de riesgo. Se amplía cuando se agreguen agentes reales.
RISK_LEVELS = {
    "chat": 0,
    "echo": 1,
}

DEFAULT_GRANTED_TOOLS = list(RISK_LEVELS.keys())


def _get_or_create_permissions(internal_user_id: str) -> list:
    doc_ref = db.collection("permissions").document(internal_user_id)
    doc = doc_ref.get()
    if doc.exists:
        return doc.to_dict().get("granted_tools", [])
    doc_ref.set({"granted_tools": DEFAULT_GRANTED_TOOLS})
    return DEFAULT_GRANTED_TOOLS


def evaluate(tool: str, internal_user_id: str) -> dict:
    if tool not in RISK_LEVELS:
        return {"allowed": False, "reason": "herramienta_no_reconocida", "risk": None}

    granted_tools = _get_or_create_permissions(internal_user_id)
    if tool not in granted_tools:
        return {"allowed": False, "reason": "sin_permiso", "risk": RISK_LEVELS[tool]}

    risk = RISK_LEVELS[tool]
    if risk >= 5:
        return {"allowed": False, "reason": "riesgo_bloqueado", "risk": risk}
    if risk >= 3:
        return {"allowed": "requiere_confirmacion", "reason": None, "risk": risk}

    return {"allowed": True, "reason": None, "risk": risk}