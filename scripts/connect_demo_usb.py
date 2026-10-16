"""Keep Link's USB port mapping available after the phone reconnects."""
import argparse
import os
from pathlib import Path
import shutil
import subprocess
import time


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", help="Select a phone when several devices are attached.")
    parser.add_argument("--once", action="store_true", help="Configure once instead of watching reconnects.")
    args = parser.parse_args()
    sdk = Path(os.environ.get("ANDROID_HOME") or Path(os.environ.get("LOCALAPPDATA", "")) / "Android/Sdk")
    adb = shutil.which("adb") or str(sdk / "platform-tools/adb.exe")
    if not Path(adb).is_file():
        raise SystemExit("No encuentro adb. Instala Android SDK Platform-Tools o configura ANDROID_HOME.")

    def run(*arguments):
        result = subprocess.run([adb, *arguments], capture_output=True, text=True, timeout=15)
        if result.returncode:
            raise RuntimeError("ADB no pudo completar la conexion. Revisa el cable y la autorizacion USB.")
        return result.stdout

    previous = None
    print("Conserva el backend encendido. En la app usa http://127.0.0.1:8000.", flush=True)
    try:
        while True:
            ready = False
            try:
                devices = []
                for line in run("devices", "-l").splitlines()[1:]:
                    fields = line.split()
                    if len(fields) >= 2 and ("usb:" in line or ":" not in fields[0]) and not fields[0].startswith("emulator-"):
                        if not args.serial or fields[0] == args.serial:
                            devices.append((fields[0], fields[1]))
                if not devices:
                    status = "Esperando telefono USB: desbloquealo, conecta un cable de datos y activa USB debugging."
                elif len(devices) > 1:
                    status = "Hay varios dispositivos. Ejecuta este script con --serial para elegir uno."
                elif devices[0][1] != "device":
                    status = "Telefono detectado: acepta Allow USB debugging en su pantalla."
                else:
                    serial = devices[0][0]
                    mappings = [line.split() for line in run("-s", serial, "reverse", "--list").splitlines()]
                    existing = next((entry[-1] for entry in mappings if len(entry) >= 3 and entry[-2] == "tcp:8000"), None)
                    if existing and existing != "tcp:8000":
                        status = "El puerto USB 8000 ya apunta a otro servicio. No se modifico."
                    else:
                        if not existing:
                            run("-s", serial, "reverse", "--no-rebind", "tcp:8000", "tcp:8000")
                        status = "Enlace USB listo. En Link toca Usar conexion USB y Guardar y comprobar."
                        ready = True
            except (RuntimeError, subprocess.TimeoutExpired, OSError) as error:
                status = str(error)
            if status != previous:
                print(status, flush=True)
                previous = status
            if args.once:
                return 0 if ready else 1
            time.sleep(3)
    except KeyboardInterrupt:
        print("\nMonitor detenido. No se retiraron los enlaces USB existentes.")
        return 0


if __name__ == "__main__":
    raise SystemExit(main())
