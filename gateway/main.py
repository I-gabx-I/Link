from fastapi import FastAPI, Request
from dotenv import load_dotenv
from interfaces.telegram_adapter import handle_update

load_dotenv()

app = FastAPI(title="Gateway")


@app.get("/")
def health_check():
    return {"status": "gateway alive"}


@app.post("/webhook/telegram")
async def telegram_webhook(request: Request):
    data = await request.json()
    await handle_update(data)
    return {"ok": True}