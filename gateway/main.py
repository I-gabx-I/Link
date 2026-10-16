import os
import secrets
from fastapi import FastAPI, Request, Header, HTTPException
from pydantic import BaseModel
from dotenv import load_dotenv
from interfaces.telegram_adapter import handle_update
from interfaces.app_adapter import handle_app_message

load_dotenv()

APP_SHARED_SECRET = os.getenv("APP_SHARED_SECRET", "")

app = FastAPI(title="Gateway")


class AppMessage(BaseModel):
    install_id: str
    message: str


@app.get("/")
def health_check():
    return {"status": "gateway alive"}


@app.post("/webhook/telegram")
async def telegram_webhook(request: Request):
    data = await request.json()
    await handle_update(data)
    return {"ok": True}


@app.post("/app/message")
async def app_message(body: AppMessage, x_app_secret: str = Header(default="")):
    if not APP_SHARED_SECRET or not secrets.compare_digest(x_app_secret, APP_SHARED_SECRET):
        raise HTTPException(status_code=401, detail="unauthorized")
    return await handle_app_message(body.install_id, body.message)