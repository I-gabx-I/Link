from datetime import datetime, timezone
from policy_engine.policy import db

COLLECTION = "pending_actions"

AFFIRMATIVE = {"si", "sí", "confirmo", "dale", "ok", "correcto", "adelante", "sip", "yes"}
NEGATIVE = {"no", "cancela", "cancelar", "negativo", "nel"}


def save_pending(internal_user_id: str, tool: str, params: dict, summary: str):
    db.collection(COLLECTION).document(internal_user_id).set({
        "tool": tool,
        "params": params,
        "summary": summary,
        "created_at": datetime.now(timezone.utc).isoformat(),
    })


def get_pending(internal_user_id: str):
    doc = db.collection(COLLECTION).document(internal_user_id).get()
    return doc.to_dict() if doc.exists else None


def clear_pending(internal_user_id: str):
    db.collection(COLLECTION).document(internal_user_id).delete()


def classify_confirmation(message: str) -> str:
    """Devuelve 'yes', 'no', o 'unclear'."""
    normalized = message.strip().lower()
    if normalized in AFFIRMATIVE:
        return "yes"
    if normalized in NEGATIVE:
        return "no"
    return "unclear"