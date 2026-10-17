"""Run the Android demo backend locally; no Docker or Firebase Auth required."""
import os
from pathlib import Path
import socket
import subprocess
import sys
import time
import urllib.error
import urllib.request

from dotenv import dotenv_values

ROOT = Path(__file__).resolve().parents[1]


def require_free_ports(services):
    """Reject existing listeners before starting any child process."""
    reservations = []
    occupied = []
    try:
        for directory, host, port in services:
            reservation = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            reservations.append(reservation)
            if hasattr(socket, "SO_EXCLUSIVEADDRUSE"):
                reservation.setsockopt(socket.SOL_SOCKET, socket.SO_EXCLUSIVEADDRUSE, 1)
            try:
                reservation.bind((host, int(port)))
            except OSError:
                occupied.append(f"{port} ({directory})")
        if occupied:
            raise SystemExit(
                "No inicié otra copia: estos puertos ya están ocupados: " + ", ".join(occupied)
                + ".\nSi el backend de Link ya está encendido, continúa con connect_demo_usb.py y la app."
                + "\nPara reiniciarlo, usa Ctrl+C en la terminal que lo está ejecutando y vuelve a intentar."
                + "\nSi esos puertos pertenecen a otro programa, revísalo antes de cerrar procesos."
            )
    finally:
        for reservation in reservations:
            reservation.close()


def main():
    config_file = ROOT / ".env"
    if not config_file.exists():
        raise SystemExit("Copia .env.example a .env y configura las claves antes de iniciar.")
    config = {key: value for key, value in dotenv_values(config_file).items() if value is not None}
    missing = [key for key in ("LLM_API_KEY", "FIREBASE_PROJECT_ID", "FIREBASE_CREDENTIALS_PATH", "APP_SHARED_SECRET") if not config.get(key)]
    if missing:
        raise SystemExit("Falta configurar en .env: " + ", ".join(missing))
    if len(config["APP_SHARED_SECRET"]) < 32 or not config["APP_SHARED_SECRET"].isascii():
        raise SystemExit("APP_SHARED_SECRET debe ser una clave aleatoria ASCII de al menos 32 caracteres.")
    credential_path = Path(config["FIREBASE_CREDENTIALS_PATH"])
    if not credential_path.is_absolute():
        credential_path = ROOT / credential_path
    if not credential_path.is_file():
        raise SystemExit("No encuentro el archivo de credenciales indicado en FIREBASE_CREDENTIALS_PATH.")
    config["FIREBASE_CREDENTIALS_PATH"] = str(credential_path.resolve())
    config["ORCHESTRATOR_URL"] = "http://127.0.0.1:8001"
    config["AGENT_TELEGRAM_URL"] = "http://127.0.0.1:8006"
    config["LLM_LOG_DIR"] = str(ROOT / "logs")
    config.setdefault("ENABLED_TOOLS", "chat,contact_list,contact_add,telegram_send")
    startup_timeout = float(config.get("DEMO_STARTUP_TIMEOUT", "180"))
    environment = {**os.environ, **config, "PYTHONUNBUFFERED": "1"}
    services = [("agents/agent_telegram", "127.0.0.1", "8006"), ("orchestrator", "127.0.0.1", "8001"), ("gateway", "0.0.0.0", "8000")]
    require_free_ports(services)
    processes = []
    try:
        for directory, host, port in services:
            print(f"Iniciando {directory}...", flush=True)
            process = subprocess.Popen([sys.executable, "-m", "uvicorn", "main:app", "--host", host, "--port", port], cwd=ROOT / directory, env=environment)
            processes.append(process)
            deadline = time.monotonic() + startup_timeout
            while time.monotonic() < deadline:
                if process.poll() is not None:
                    raise RuntimeError(f"{directory} se detuvo al iniciar. Revisa el error anterior.")
                try:
                    with urllib.request.urlopen(f"http://127.0.0.1:{port}/", timeout=1) as response:
                        if response.status == 200:
                            if process.poll() is not None:
                                raise RuntimeError(f"{directory} se detuvo al iniciar. Revisa el error anterior.")
                            break
                except (urllib.error.URLError, TimeoutError):
                    time.sleep(0.25)
            else:
                raise RuntimeError(f"{directory} no respondió a tiempo.")
        print("\nBackend listo en el puerto 8000. Mantén esta terminal abierta.", flush=True)
        print("USB: http://127.0.0.1:8000 después de iniciar scripts/connect_demo_usb.py", flush=True)
        print("Emulador: http://10.0.2.2:8000 | Teléfono: http://IP_LOCAL_DE_ESTA_PC:8000", flush=True)
        print("Usa APP_SHARED_SECRET como clave de conexión en el panel del cuidador.", flush=True)
        print("Ctrl+C detiene los tres servicios. No publiques esta demo como servicio multiusuario.", flush=True)
        while all(process.poll() is None for process in processes):
            time.sleep(0.5)
        raise RuntimeError("Un servicio se detuvo. Revisa los errores anteriores.")
    except KeyboardInterrupt:
        print("\nDeteniendo la demo…", flush=True)
    except RuntimeError as error:
        print(str(error), file=sys.stderr, flush=True)
        return 1
    finally:
        for process in reversed(processes):
            if process.poll() is None:
                process.terminate()
        for process in reversed(processes):
            try:
                process.wait(timeout=5)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()


if __name__ == "__main__":
    raise SystemExit(main())
