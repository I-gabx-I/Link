# Link — Asistente Personal Multi-Agente

Sistema distribuido que orquesta múltiples agentes de IA para automatizar tareas cotidianas (calendario, mensajes, archivos, trámites académicos) a través de una interfaz conversacional, con un modelo de seguridad basado en permisos, niveles de riesgo y auditoría.

Proyecto de Seminario Profesional — ver `docs/` para la documentación completa de arquitectura (MD0-MD3).

## Estado del proyecto

Ver la tabla de estado en `docs/MD2_Plan_de_Fases.md` para saber en qué fase va cada componente.

## Requisitos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado y corriendo.
- Git.

## Cómo levantar el entorno (Fase 0)

1. Clonar el repositorio:
git clone https://github.com/I-gabx-I/Link.git
cd Link


2. Crear tu archivo de variables de entorno local (nunca se sube a Git):

copy .env.example .env

   *(en Mac/Linux: `cp .env.example .env`)*

3. Levantar todos los servicios:

docker compose up --build


4. Verificar que los servicios responden:
   - Gateway: http://localhost:8000
   - Orchestrator: http://localhost:8001
   - `agent_echo` **no** debe ser accesible desde el navegador (vive en una red interna aislada, por diseño — ver `docs/MD3_Arquitectura_de_Carpetas_y_Stack.md` sección 5).

5. Para detener todo: `Ctrl + C` en la terminal donde corre.

## Estructura del proyecto

orchestrator/ # Orquestador: planificación (LLM) + Policy Engine + Tool Router
gateway/ # Identity Gateway: interfaces de chat (Telegram/Discord) + autenticación
agents/ # Microservicios aislados, uno por agente (capacidades declaradas)
database/ # Esquemas y reglas de Firestore
docs/ # Documentación de arquitectura (MD0 a MD3)


## Flujo de trabajo

Cada fase del proyecto vive en su propia rama (`fase-N-nombre`) y solo se fusiona a `main` cuando cumple su Definición de Terminado (ver `docs/MD2_Plan_de_Fases.md`). Convención de commits:

- `Fase N: [nombre] — probada y terminada`
- `Fase N: [nombre] — WIP, falta [detalle]`


## Fase 1 — Probar el bot de Telegram localmente

Como el webhook de Telegram necesita una URL pública, en desarrollo local se usa un túnel:

1. `docker compose up --build` (en una terminal, déjala corriendo).
2. En otra terminal: `cloudflared tunnel --url http://localhost:8000`
3. Copia la URL `https://....trycloudflare.com` que te da.
4. Registra el webhook (una sola vez por sesión de túnel, ya que la URL cambia cada vez):
```powershell
   $token = (Get-Content .env | Select-String "TELEGRAM_BOT_TOKEN=").ToString().Split("=")[1]
   $url = "TU_URL_DE_CLOUDFLARED_AQUI"
   Invoke-RestMethod -Uri "https://api.telegram.org/bot$token/setWebhook?url=$url/webhook/telegram"
```
5. Escríbele a tu bot en Telegram para probar.

Nota: se usó Cloudflare Tunnel en vez de ngrok por conflictos recurrentes con Windows Defender (falso positivo conocido de ngrok).