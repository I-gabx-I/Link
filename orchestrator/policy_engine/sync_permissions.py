"""
Corré esto cada vez que agreguen un tool nuevo a RISK_LEVELS, para que
los usuarios que ya existían en Firestore lo reciban sin editarlos
uno por uno a mano.

Uso: docker compose exec orchestrator python -m policy_engine.sync_permissions
"""
from policy_engine.policy import db, RISK_LEVELS


def sync_all_users():
    all_tools = set(RISK_LEVELS.keys())
    docs = list(db.collection("permissions").stream())
    updated = 0
    for doc in docs:
        data = doc.to_dict()
        current = set(data.get("granted_tools", []))
        missing = all_tools - current
        if missing:
            new_granted = sorted(current | missing)
            doc.reference.update({"granted_tools": new_granted})
            print(f"[sync] {doc.id}: +{sorted(missing)}")
            updated += 1
    print(f"[sync] listo. {updated} de {len(docs)} usuario(s) actualizados.")


if __name__ == "__main__":
    sync_all_users()