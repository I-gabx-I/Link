import os
import secrets
from fastapi import Depends, FastAPI, Request, Header, HTTPException
from pydantic import BaseModel, Field
from dotenv import load_dotenv

load_dotenv()

from interfaces.telegram_adapter import handle_update
from interfaces.app_adapter import handle_app_message, handle_app_contacts

APP_SHARED_SECRET = os.getenv("APP_SHARED_SECRET", "")

app = FastAPI(title="Gateway")


class AppMessage(BaseModel):
    install_id: str = Field(min_length=16, max_length=128, pattern=r"^[A-Za-z0-9_-]+$")
    message: str = Field(min_length=1, max_length=4000)


class AppContact(BaseModel):
    install_id: str = Field(min_length=16, max_length=128, pattern=r"^[A-Za-z0-9_-]+$")
    name: str = Field(min_length=1, max_length=80)
    username: str = Field(min_length=1, max_length=40)
    aliases: list[str] = Field(default_factory=list, max_length=8)


def require_app_secret(x_app_secret: str = Header(default="")):
    if not APP_SHARED_SECRET or not secrets.compare_digest(x_app_secret, APP_SHARED_SECRET):
        raise HTTPException(status_code=401, detail="Revisa la clave de conexión del backend.")


@app.get("/")
def health_check():
    return {"status": "gateway alive"}


@app.post("/webhook/telegram")
async def telegram_webhook(request: Request):
    if not os.getenv("TELEGRAM_BOT_TOKEN"):
        raise HTTPException(status_code=503, detail="El bot de Telegram no está configurado.")
    data = await request.json()
    await handle_update(data)
    return {"ok": True}


@app.post("/app/message")
async def app_message(body: AppMessage, _: None = Depends(require_app_secret)):
    if not body.message.strip():
        raise HTTPException(status_code=422, detail="Escribe o dicta una solicitud.")
    return await handle_app_message(body.install_id, body.message)


@app.get("/app/contacts")
async def app_contacts(install_id: str, _: None = Depends(require_app_secret)):
    if not 16 <= len(install_id) <= 128 or not all(c.isascii() and (c.isalnum() or c in "_-") for c in install_id):
        raise HTTPException(status_code=422, detail="Identificador de instalación inválido.")
    return await handle_app_contacts(install_id)


@app.post("/app/contacts")
async def save_app_contact(body: AppContact, _: None = Depends(require_app_secret)):
    return await handle_app_contacts(body.install_id, body.model_dump(exclude={"install_id"}))
