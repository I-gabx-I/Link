"""
Fase 1 — Prueba mínima del bot de Telegram (modo polling, solo para desarrollo local).
Cuando integremos el Orquestador, esto pasa a modo webhook dentro de gateway/main.py.
"""
import os
from dotenv import load_dotenv
from telegram import Update
from telegram.ext import ApplicationBuilder, MessageHandler, ContextTypes, filters

load_dotenv()
TOKEN = os.getenv("TELEGRAM_BOT_TOKEN")


async def echo(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text(f"Recibido: {update.message.text}")


if __name__ == "__main__":
    if not TOKEN:
        raise RuntimeError("Falta TELEGRAM_BOT_TOKEN en tu archivo .env")

    app = ApplicationBuilder().token(TOKEN).build()
    app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, echo))

    print("Bot corriendo... Ctrl+C para detener.")
    app.run_polling()