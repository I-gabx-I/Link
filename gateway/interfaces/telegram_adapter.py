import os
import httpx
from telegram import Update, Bot

TOKEN = os.getenv("TELEGRAM_BOT_TOKEN")
ORCHESTRATOR_URL = os.getenv("ORCHESTRATOR_URL", "http://orchestrator:8001")

bot = Bot(token=TOKEN)


async def handle_update(data: dict):
    update = Update.de_json(data, bot)

    if not (update.message and update.message.text):
        return

    user_message = update.message.text
    telegram_user_id = update.message.from_user.id
    chat_id = update.message.chat_id

    try:
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.post(
                f"{ORCHESTRATOR_URL}/process",
                json={"message": user_message, "telegram_user_id": telegram_user_id},
            )
            response.raise_for_status()
            reply = response.json().get("reply", "El orquestador no devolvió respuesta.")
    except httpx.HTTPError as e:
        reply = f"Error hablando con el orquestador: {e}"

    await bot.send_message(chat_id=chat_id, text=reply)