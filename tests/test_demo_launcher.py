"""Prevent false readiness when another backend already owns the demo ports."""
import importlib.util
from pathlib import Path
import socket
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("demo_launcher", Path(__file__).resolve().parents[1] / "scripts/run_demo.py")
launcher = importlib.util.module_from_spec(spec)
spec.loader.exec_module(launcher)


class DemoLauncher(unittest.TestCase):
    def test_occupied_port_prevents_any_child_process(self):
        with tempfile.TemporaryDirectory() as folder, socket.socket() as listener:
            root = Path(folder)
            (root / "credential.json").write_text("{}")
            (root / ".env").write_text(
                "LLM_API_KEY=test\nFIREBASE_PROJECT_ID=test\n"
                "FIREBASE_CREDENTIALS_PATH=credential.json\nAPP_SHARED_SECRET=" + "x" * 32
            )
            listener.bind(("127.0.0.1", 0))
            listener.listen()
            occupied_port = listener.getsockname()[1]
            check = launcher.require_free_ports
            with patch.object(launcher, "ROOT", root), patch.object(launcher.subprocess, "Popen") as spawn:
                with patch.object(launcher, "require_free_ports", side_effect=lambda services: check([("existing-service", "127.0.0.1", str(occupied_port))])):
                    with self.assertRaisesRegex(SystemExit, "No inicié otra copia"):
                        launcher.main()
                spawn.assert_not_called()
            self.assertNotEqual(listener.fileno(), -1)

    def test_available_ports_are_released_after_check(self):
        with socket.socket() as probe:
            probe.bind(("127.0.0.1", 0))
            port = probe.getsockname()[1]
        launcher.require_free_ports([("test-service", "127.0.0.1", str(port))])
        with socket.socket() as replacement:
            replacement.bind(("127.0.0.1", port))


if __name__ == "__main__":
    unittest.main()
