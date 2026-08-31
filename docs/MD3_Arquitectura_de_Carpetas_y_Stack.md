# MD 3 — Arquitectura de Carpetas y Tecnologías (El "Cómo")

## 1. Visión tecnológica general

El stack debe cumplir tres condiciones no negociables: **distribución real** (servicios aislados, no un monolito disfrazado), **seguridad por diseño** (no por convención), y **costo cero** (capas gratuitas de cada servicio), manteniendo una barrera de entrada baja para que Marcos y Samuel avancen en paralelo sin bloquearse.

- **Lenguaje principal (backend):** Python 3.11+. Es el estándar de facto para orquestación de LLMs (LangChain) y APIs rápidas — acelera tanto el Orquestador como los Agentes.
- **Base de datos / autenticación:** Firebase (Firestore + Auth), plan Spark (gratis). Cubre Identity Gateway, almacenamiento de permisos y el log de auditoría.
- **Comunicación interna:** API REST (FastAPI) para comunicación síncrona entre Orquestador, Gateway y Agentes. Se deja de lado el socket TCP/UDP manual como columna vertebral del sistema — si quieren demostrar ese conocimiento de Redes/SO explícitamente, se puede aislar como una pieza puntual y opcional, no como base de todo el producto.
- **Contenedores:** Docker + Docker Compose, para levantar todo el ecosistema con un solo comando y garantizar que ambos corren exactamente lo mismo.

## 2. Decisiones por componente

| Componente | Tecnología | Justificación |
|---|---|---|
| **Orquestador / Tool Router** | Python + FastAPI | `async/await` nativo, ideal para esperar respuestas del LLM y de los agentes sin bloquear el servidor. |
| **LLM Planner** | LangChain (Python) + proveedor gratuito por defecto | LangChain abstrae el contrato de "tools/capabilities", lo que permite cambiar de modelo sin reescribir el Orquestador. Con presupuesto cero, el proveedor por defecto debe ser gratuito (ej. Gemini API free tier o Groq); Anthropic/OpenAI quedan como alternativa si en algún momento se justifica gastar. |
| **Policy Engine** | Lógica propia en Python (middleware de FastAPI) | Se necesita control total sobre niveles de riesgo, confirmación y kill switch — una herramienta externa (ej. OPA) agrega complejidad que no aporta para el MVP. |
| **Identity Gateway** | Firebase Authentication | Vincula cuentas de Telegram/Discord a un `internal_user_id` único, sin construir un sistema de auth desde cero. |
| **Agentes (Workers)** | Python + FastAPI, un microservicio por agente | Cada agente es una API independiente, con su propio contenedor, sus propias dependencias y su propio acceso a secretos — aislamiento real, no solo lógico. |
| **Base de datos / estado** | Firebase Firestore (NoSQL) | Almacena permisos por usuario, perfil (estándar/asistido) e historial de auditoría. Ver sección 5 sobre cómo forzar que la auditoría sea realmente inmutable. |
| **Interfaces (MVP)** | APIs oficiales de Telegram y Discord, vía webhooks | Cero costo de UI, acceso nativo a notificaciones y multiplataforma sin construir frontend propio. |

## 3. Arquitectura del repositorio (monorepo)

Con dos personas, múltiples repos solo fragmentan el trabajo. Se usa un **monorepo** con carpetas delimitadas por servicio — así cada pieza se despliega como contenedor independiente (arquitectura distribuida real) desde una sola fuente de verdad.

```
/multi-agent-orchestrator
├── .github/                  # Actions, templates de PRs
├── docs/                     # MD0, MD1, MD2, MD3
├── orchestrator/             # 🧠 Orquestador: FastAPI, LangChain, Policy Engine
│   ├── main.py
│   ├── llm_planner/          # Traducción de lenguaje natural a intención estructurada
│   ├── policy_engine/        # Niveles de riesgo, permisos, confirmación, kill switch
│   ├── tool_router/          # Enrutamiento hacia los agentes
│   ├── models/                # Modelos de datos compartidos (Pydantic)
│   ├── Dockerfile
│   └── requirements.txt
├── gateway/                  # 🛡️ Identity Gateway: interfaces + autenticación
│   ├── main.py                # Recibe webhooks, resuelve internal_user_id contra Firebase
│   ├── interfaces/            # telegram_adapter.py, discord_adapter.py
│   ├── Dockerfile
│   └── requirements.txt
├── agents/                   # 👷 Agentes: microservicios aislados
│   ├── agent_echo/            # Fase 1 — agente de prueba
│   ├── agent_calendar/        # Fase 4 — Google Calendar
│   ├── agent_files/           # Fase 4 — sistema de archivos restringido
│   └── agent_telegram/        # Fase 4 — envío de mensajes
│       ├── main.py
│       ├── capabilities.py    # Contrato de capacidades del agente (ver MD1 sección 3)
│       ├── secrets_manager/   # Acceso a SUS PROPIOS tokens, no compartido
│       └── Dockerfile
├── database/                 # 🗄️ Esquemas y reglas de Firestore
│   ├── firestore_schema.md    # Colecciones: usuarios, permisos, audit_logs
│   └── firestore.rules        # Reglas de seguridad, incluyendo inmutabilidad de audit_logs
├── .env.example
├── docker-compose.yml         # Levanta todo el ecosistema; redes internas separadas (ver sección 5)
├── .gitignore
└── README.md                  # Instrucciones de setup — Fase 0
```

## 4. Flujo de datos (ejemplo completo)

Ejemplo: *"Avísale a Luis que llego tarde"*, para ver cómo interactúan las carpetas y tecnologías.

1. **Entrada:** el usuario escribe el mensaje en Telegram.
2. **Gateway:** el webhook en `/gateway` lo recibe, extrae el `telegram_user_id`, lo resuelve contra Firebase para obtener el `internal_user_id`, y pasa texto + ID al Orquestador.
3. **LLM Planner:** genera la intención estructurada:
   ```json
   {"tool": "telegram_send", "params": {"to": "Luis", "msg": "Llegaré tarde"}}
   ```
4. **Policy Engine:** clasifica la acción como **Riesgo 3** (MD1, sección 5) y verifica en Firebase que el usuario haya otorgado permiso al `agent_telegram`.
5. **Confirmación:** por ser riesgo 3, el Orquestador pausa y pide al Gateway enviar: *"¿Enviar mensaje a Luis: 'Llegaré tarde'?"*
6. **Ejecución:** si el usuario confirma, el `tool_router` llama vía HTTP interno (`http://agent_telegram:8003/execute`) al contenedor del agente.
7. **Agente:** `agent_telegram` recibe la orden, lee su propio token (aislado, ver sección 5) y ejecuta el envío real.
8. **Auditoría:** el Orquestador escribe el resultado en la colección `audit_logs` de Firestore.

## 5. Gestión de secretos (implementación concreta)

Aplicando el principio del MD1 ("el LLM nunca ve secretos") a nivel de infraestructura, no solo de código:

- En desarrollo local, los secretos se inyectan como variables de entorno vía `docker-compose.yml`, **por servicio**, nunca de forma global.
- **El contenedor del Orquestador NO tiene acceso a las variables de entorno de los agentes** (`TELEGRAM_BOT_TOKEN`, `GOOGLE_CALENDAR_CREDENTIALS`, etc.). Cada agente recibe únicamente las suyas.
- **Aislamiento también a nivel de red:** los contenedores de `/agents` viven en una red interna de Docker no expuesta al exterior — solo son alcanzables desde el `tool_router` del Orquestador, nunca desde fuera del sistema.
- **La auditoría debe ser inmutable de verdad, no solo de nombre:** en `database/firestore.rules` se declara explícitamente que la colección `audit_logs` permite `create` pero **no** `update` ni `delete`. Sin esta regla, "log append-only" es solo una intención, no una garantía.

## 6. Próximos pasos (conexión con Fase 0)

Para dar por terminada la Fase 0 (MD2), falta:

- [ ] Crear el repositorio en GitHub con la estructura de `/multi-agent-orchestrator` de la sección 3.
- [ ] Escribir el `docker-compose.yml` base que levante contenedores vacíos para `gateway`, `orchestrator` y `agent_echo`.
- [ ] Configurar la red interna de Docker para los agentes (sección 5), aunque todavía no haya secretos reales que proteger — mejor dejarlo bien desde el principio que parcharlo después.
- [ ] Confirmar que ambos pueden ejecutar `docker compose up` sin errores, siguiendo únicamente el README.

Con esto, los 4 documentos base (MD0, MD1, MD2, MD3) quedan completos y consistentes entre sí.

## 7. Mejoras futuras (no bloquean el MVP)

- **Fallback entre modelos de Gemini:** si el modelo configurado en `GEMINI_MODEL`
  está saturado (503), reintentar automáticamente con otro modelo de Gemini antes
  de fallar.
- **Fallback entre proveedores de LLM:** si Gemini completo no responde, reintentar
  con otro proveedor gratuito (ej. Groq) antes de devolver error al usuario.
- **Modelo local como última red de seguridad:** correr un modelo pequeño vía
  Ollama, para cuando no haya internet o todos los proveedores en la nube fallen.
  Candidato natural para el cierre del proyecto (última fase), ya que el trabajo
  del `llm_planner` es una tarea de clasificación ligera, bien adecuada para un
  modelo local pequeño.