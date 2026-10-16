"""Exercise the real gateway/orchestrator/Telegram agent with fake Firebase and LLM.

No real credentials, external requests, or actual Telegram messages are used.
Run from the repository root: python -m unittest discover -s tests -v
"""
import copy
import importlib.util
import os
from pathlib import Path
import sys
import types
import unittest
from unittest.mock import MagicMock, patch

import httpx
from fastapi.testclient import TestClient

ROOT = Path(__file__).resolve().parents[1]


class Snapshot:
    def __init__(self, reference):
        self.id = reference.id
        self.reference = reference
        self.exists = reference.id in reference.records
        self.value = copy.deepcopy(reference.records.get(reference.id))

    def to_dict(self):
        return copy.deepcopy(self.value)


class Document:
    def __init__(self, records, identifier):
        self.records, self.id = records, identifier

    def get(self):
        return Snapshot(self)

    def set(self, value, merge=False):
        self.records[self.id] = {**self.records.get(self.id, {}), **copy.deepcopy(value)} if merge else copy.deepcopy(value)

    def create(self, value):
        if self.id in self.records:
            from google.api_core.exceptions import AlreadyExists
            raise AlreadyExists("exists")
        self.set(value)

    def delete(self):
        self.records.pop(self.id, None)


class Collection:
    def __init__(self, records):
        self.records = records
        self.criteria = None
        self.maximum = None

    def document(self, identifier=None):
        return Document(self.records, identifier or f"generated-{len(self.records)}")

    def where(self, field, operator, value):
        self.criteria = (field, value)
        return self

    def limit(self, maximum):
        self.maximum = maximum
        return self

    def stream(self):
        result = [self.document(key).get() for key, value in self.records.items() if not self.criteria or value.get(self.criteria[0]) == self.criteria[1]]
        return iter(result[:self.maximum])


class Database:
    def __init__(self):
        self.data = {}

    def collection(self, name):
        return Collection(self.data.setdefault(name, {}))


database = Database()
firebase = types.ModuleType("firebase_admin")
firebase._apps = {"test": object()}
firebase.credentials = types.SimpleNamespace(Certificate=lambda path: object())
firebase.firestore = types.SimpleNamespace(client=lambda: database)
planner = types.ModuleType("llm_planner.planner")
planner.plan_action = MagicMock()


def load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    sys.modules[name] = module
    spec.loader.exec_module(module)
    return module


sys.path[:0] = [str(ROOT / "gateway"), str(ROOT / "orchestrator"), str(ROOT / "agents/agent_telegram")]
with patch.dict(sys.modules, {"firebase_admin": firebase, "llm_planner.planner": planner}), patch.dict(os.environ, {
    "APP_SHARED_SECRET": "test-connection-secret", "TELEGRAM_BOT_TOKEN": "",
    "ENABLED_TOOLS": "chat,contact_list,contact_add,telegram_send",
}):
    agent = load("demo_test_agent", ROOT / "agents/agent_telegram/main.py")
    orchestrator = load("demo_test_orchestrator", ROOT / "orchestrator/main.py")
    gateway = load("demo_test_gateway", ROOT / "gateway/main.py")

real_async_client = httpx.AsyncClient


class DemoFlow(unittest.TestCase):
    INSTALL = "install-demo-000000001"
    HEADERS = {"X-App-Secret": "test-connection-secret"}

    def setUp(self):
        database.data.clear()
        planner.plan_action.reset_mock()
        planner.plan_action.side_effect = lambda *args: copy.deepcopy(planner.plan_action.return_value)
        planner.plan_action.return_value = {"tool": "chat", "params": {"answer": "Hola"}}
        self.calls = []

        async def call_agent(tool, params, user_id):
            self.calls.append(tool)
            return agent.execute(agent.ExecuteRequest(tool=tool, params=params, internal_user_id=user_id))

        self.agent_patch = patch.object(orchestrator, "_call_agent", side_effect=call_agent)
        self.agent_patch.start()
        self.transport_patch = patch.object(httpx, "AsyncClient", side_effect=lambda **kwargs: real_async_client(transport=httpx.ASGITransport(app=orchestrator.app), **kwargs))
        self.transport_patch.start()
        self.client = TestClient(gateway.app)

    def tearDown(self):
        self.client.close()
        self.transport_patch.stop()
        self.agent_patch.stop()

    def save(self, name="María", username="@maria_demo", aliases=None):
        return self.client.post("/app/contacts", headers=self.HEADERS, json={"install_id": self.INSTALL, "name": name, "username": username, "aliases": aliases or ["mamá"]}).json()

    def send(self, text):
        return self.client.post("/app/message", headers=self.HEADERS, json={"install_id": self.INSTALL, "message": text}).json()

    def prepare(self):
        self.save()
        planner.plan_action.return_value = {"tool": "telegram_send", "params": {"contact": "mamá", "message": "Llego a las ocho & llevo café"}}
        return self.send("Avísale a mi mamá que llego a las ocho y llevo café")

    def test_no_secret_cannot_create_or_read_profiles(self):
        for method, path, body in [
            ("POST", "/app/message", {"install_id": self.INSTALL, "message": "hola"}),
            ("POST", "/app/contacts", {"install_id": self.INSTALL, "name": "María", "username": "maria_demo"}),
            ("GET", "/app/contacts?install_id=" + self.INSTALL, None),
        ]:
            result = self.client.request(method, path, json=body)
            self.assertEqual(result.status_code, 401)
        self.assertFalse(database.data)

    def test_contact_is_saved_and_read_using_same_installation(self):
        self.assertEqual(self.save()["type"], "result")
        response = self.client.get("/app/contacts", params={"install_id": self.INSTALL}, headers=self.HEADERS).json()
        self.assertEqual(response["contacts"], [{"name": "María", "username": "maria_demo", "aliases": ["mamá"]}])
        self.assertEqual(len(database.data["users"]), 1)

    def test_other_installation_does_not_read_these_contacts(self):
        self.save()
        response = self.client.get("/app/contacts", params={"install_id": "install-demo-000000002"}, headers=self.HEADERS).json()
        self.assertEqual(response["contacts"], [])

    def test_model_gets_real_names_and_aliases_but_no_connection_secret(self):
        self.prepare()
        phrase, _, context = planner.plan_action.call_args.args
        self.assertIn("mi mamá", phrase)
        self.assertEqual(context["contacts"], [{"name": "María", "aliases": ["mamá"]}])
        self.assertNotIn("test-connection-secret", str(context))

    def test_message_requires_confirmation_before_url_is_created(self):
        response = self.prepare()
        self.assertEqual(response["type"], "confirmation")
        self.assertIn("María", response["summary"])
        self.assertNotIn("telegram_send", self.calls)
        result = self.send("sí")
        self.assertEqual(result["type"], "result")
        self.assertEqual(result["action_url"], "https://t.me/maria_demo?text=Llego%20a%20las%20ocho%20%26%20llevo%20caf%C3%A9")
        self.assertEqual(planner.plan_action.call_count, 1)

    def test_cancelling_does_not_prepare_telegram(self):
        self.prepare()
        response = self.send("no")
        self.assertEqual(response["type"], "info")
        self.assertNotIn("telegram_send", self.calls)
        self.assertFalse(database.data["pending_actions"])

    def test_missing_message_is_asked_and_draft_is_provided_to_next_turn(self):
        self.save()
        planner.plan_action.return_value = {"tool": "telegram_send", "params": {"contact": "María"}}
        response = self.send("Mándale un mensaje a María")
        self.assertEqual(response["type"], "clarification")
        self.assertNotIn("telegram_send", self.calls)
        planner.plan_action.return_value = {"tool": "telegram_send", "params": {"contact": "María", "message": "Voy saliendo"}}
        response = self.send("Voy saliendo")
        self.assertEqual(response["type"], "confirmation")
        self.assertEqual(planner.plan_action.call_args.args[2]["draft"]["params"], {"contact": "María"})

    def test_unknown_contact_never_produces_an_action_url(self):
        self.save()
        planner.plan_action.return_value = {"tool": "telegram_send", "params": {"contact": "Desconocido", "message": "Hola"}}
        response = self.send("Escríbele a Desconocido")
        self.assertEqual(response["type"], "clarification")
        self.assertNotIn("telegram_send", self.calls)

    def test_revoking_permission_before_confirmation_blocks_action(self):
        self.prepare()
        profile = next(iter(database.data["users"]))
        database.data["permissions"][profile]["granted_tools"].remove("telegram_send")
        self.assertEqual(self.send("sí")["type"], "denied")
        self.assertNotIn("telegram_send", self.calls)

    def test_editing_accented_name_does_not_duplicate_contact(self):
        self.save()
        self.save(name="Maria", username="maria_updated")
        response = self.client.get("/app/contacts", params={"install_id": self.INSTALL}, headers=self.HEADERS).json()
        self.assertEqual(len(response["contacts"]), 1)
        self.assertEqual(response["contacts"][0]["username"], "maria_updated")

    def test_duplicate_alias_is_not_assigned_to_another_person(self):
        self.save()
        response = self.save(name="Ana", username="ana_demo", aliases=["mamá"])
        self.assertEqual(response["type"], "error")
        self.assertIn("ya corresponde", response["reply"])

    def test_invalid_telegram_user_is_not_persisted(self):
        self.assertEqual(self.save(username="https://example.test")["type"], "error")
        self.assertFalse(any(database.data.get("contacts", {}).values()))

    def test_undeclared_tool_is_not_executed(self):
        planner.plan_action.return_value = {"tool": "arbitrary_shell", "params": {"command": "irrelevant"}}
        self.assertEqual(self.send("Haz algo fuera de las capacidades")["type"], "info")
        self.assertNotIn("arbitrary_shell", self.calls)

    def test_blank_message_is_rejected_before_model_call(self):
        response = self.client.post("/app/message", headers=self.HEADERS, json={"install_id": self.INSTALL, "message": "   "})
        self.assertEqual(response.status_code, 422)
        planner.plan_action.assert_not_called()


if __name__ == "__main__":
    unittest.main()
