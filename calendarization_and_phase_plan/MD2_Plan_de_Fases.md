# MD 2 — Plan de Fases

## 1. Cómo se usa este documento

Este documento es la fuente de verdad de **qué se ha hecho, qué se está haciendo y qué falta**. La idea es que tanto ustedes como una IA (analizando el repo + este archivo) puedan saber en cualquier momento qué está donde, quién lo tiene en manos y si está bloqueado.

Para que eso funcione, dos reglas fijas:

1. **La tabla de estado de la sección 2 se actualiza cada vez que algo cambia de estado.** No se actualiza "al final", se actualiza en el momento.
2. **Cada fase vive en su propia rama** (`fase-N-nombre-corto`) y no se fusiona a `main` hasta que cumple su "Definición de Terminado". Si algo no se logró completar, se sube igual a esa rama (no se pierden los cambios), y la tabla de estado queda en `Bloqueada` (con la razón en una nota) hasta que se resuelva.

## 2. Tabla de estado (actualizar aquí primero, siempre)

| Fase | Nombre | Responsable | Estado | Rama |
|---|---|---|---|---|
| 0 | Setup del entorno | Marcos + Samuel | Pendiente | `fase-0-setup` |
| 1 | Esqueleto (Orquestador + interfaz + agente eco) | Marcos | Pendiente | `fase-1-esqueleto` |
| 2 | Identity Gateway + Policy Engine básico | Samuel | Pendiente | `fase-2-identidad-policy` |
| 3 | Modelo de permisos + niveles de riesgo + confirmación | Marcos | Pendiente | `fase-3-permisos-riesgo` |
| 4 | Agentes reales (Calendar, File, Telegram send) | Samuel | Pendiente | `fase-4-agentes-reales` |
| 5 | Auditoría + explicabilidad + kill switch | Marcos | Pendiente | `fase-5-auditoria-killswitch` |
| 6 | Caso de uso académico + pruebas de robustez | Samuel | Pendiente | `fase-6-robustez` |
| 7 | Perfil asistido / adulto mayor | Marcos + Samuel | Pendiente | `fase-7-perfil-asistido` |
| 8 | Interfaz propia (opcional, si alcanza tiempo) | A definir | Pendiente | `fase-8-interfaz-propia` |

**Estados posibles:** `Pendiente` / `En progreso` / `Bloqueada` (nota abajo del porqué) / `Terminada y probada`.

La asignación de responsables es un punto de partida — se puede intercambiar, lo importante es que quede siempre claro en esta tabla quién la tiene en manos ahora mismo. Y aunque una fase tenga un responsable principal, la otra persona puede meter mano en cualquier momento (no es exclusiva).

**Notas sobre bloqueos:** (dejar aquí si alguna fase está bloqueada)

---

## 3. Convención de commits y ramas

- **Rama por fase:** `fase-N-nombre-corto` (minúsculas, guiones, sin espacios).
- **Commit al terminar y probar una fase:**
  ```
  Fase N: [nombre] — probada y terminada
  ```
- **Commit si se sube incompleta para que el otro la retome:**
  ```
  Fase N: [nombre] — WIP, falta [descripción concreta del problema]
  ```
- **Merge a `main`:** solo cuando la fase cumple su Definición de Terminado (sección 5). El PR en GitHub debe listar los criterios cumplidos, copiando el checklist de la fase correspondiente.

Esto es lo que le permite a cualquiera (ustedes o una IA analizando el repo) mirar `main` y saber que todo lo que está ahí ya fue probado — y mirar las ramas abiertas para saber qué está en progreso o bloqueado.

---

## 4. Dependencias entre fases

```
Fase 0 (Setup)
   │
   ▼
Fase 1 (Esqueleto: Orquestador + interfaz + agente eco)
   │
   ▼
Fase 2 (Identity Gateway + Policy Engine básico)
   │
   ▼
Fase 3 (Permisos reales + niveles de riesgo + confirmación)
   │
   ▼
Fase 4 (Agentes reales: Calendar, File, Telegram)
   │
   ▼
Fase 5 (Auditoría + kill switch)
   │
   ▼
Fase 6 (Caso académico + pruebas de robustez)
   │
   ▼
Fase 7 (Perfil asistido)
   │
   ▼
Fase 8 (Interfaz propia — opcional)
```

El orden es lineal a propósito: cada fase depende de que la anterior esté realmente terminada y probada, no a medias. No conviene adelantar trabajo de una fase futura mientras la actual está `Bloqueada` o a medias (a menos que ambos decidan explícitamente).

---

## 5. Detalle de cada fase

### Fase 0 — Setup del entorno

**Qué se construye:** nada de producto todavía — se deja listo el entorno para que ambos trabajen igual.

- Repo creado en GitHub con estructura de carpetas inicial (definida en MD 3).
- `.env.example` y `.gitignore` configurados.
- Docker y Docker Compose corriendo un "hola mundo" (un contenedor que levanta y responde).
- README raíz con instrucciones de cómo levantar el entorno desde cero.

**Definición de Terminado:**
- [ ] Ambos pueden clonar el repo y levantar el entorno con los mismos pasos documentados en el README, sin ayuda del otro.
- [ ] `docker compose up` levanta algo (aunque sea un placeholder) sin errores en ambas máquinas.

---

### Fase 1 — Esqueleto: Orquestador + interfaz + agente eco

**Qué se construye:** la columna vertebral mínima, sin seguridad todavía. El objetivo es probar que el flujo completo de mensaje-respuesta funciona de punta a punta.

- Bot de Telegram conectado.
- Orchestrator (FastAPI) que recibe el mensaje del bot.
- Un agente de prueba ("EchoAgent") que simplemente repite o responde algo fijo — sin LLM todavía.
- Flujo: usuario escribe en Telegram → Orchestrator recibe → EchoAgent responde → Telegram muestra la respuesta.

**Definición de Terminado:**
- [ ] Un mensaje enviado por Telegram llega al Orchestrator (log visible).
- [ ] El Orchestrator llama al EchoAgent y obtiene una respuesta.
- [ ] La respuesta vuelve al usuario en Telegram.
- [ ] Todo corre vía Docker Compose con un solo comando.

---

### Fase 2 — Identity Gateway + Policy Engine básico

**Qué se construye:** empieza la parte de seguridad, todavía simple.

- Identity Gateway: identifica de qué usuario viene cada mensaje (aunque sea un usuario "hardcodeado" de prueba por ahora).
- Policy Engine básico: reglas fijas en código (ej. "todo se ejecuta directo, nada se bloquea") — la estructura existe, la lógica se enriquece en la Fase 3.
- El LLM Planner entra aquí por primera vez: traduce el mensaje de texto libre a una intención estructurada (JSON con `tool` + `params`), pero contra un solo agente de prueba.

**Definición de Terminado:**
- [ ] El sistema identifica de qué usuario viene cada mensaje.
- [ ] El LLM Planner convierte un mensaje en lenguaje natural a JSON estructurado correctamente para al menos 3 frases de prueba distintas.
- [ ] El Policy Engine recibe esa intención y decide "ejecutar" (aunque la regla sea simple todavía).

---

### Fase 3 — Modelo de permisos + niveles de riesgo + confirmación

**Qué se construye:** el Policy Engine deja de ser una regla fija y se vuelve el "guardián" real descrito en el MD 1.

- Tabla de niveles de riesgo (0-5) implementada.
- Sistema de permisos por capability (el usuario debe otorgar permiso a un agente antes de que este pueda ejecutar algo).
- Flujo de confirmación para acciones de riesgo 3+ (el sistema pregunta "¿enviar?" antes de ejecutar).
- Flujo de bloqueo para riesgo 5.

**Definición de Terminado:**
- [ ] Una acción de riesgo 0-1 se ejecuta directo, sin fricción.
- [ ] Una acción de riesgo 3 pide confirmación explícita al usuario antes de ejecutar.
- [ ] Una acción de riesgo 5 se bloquea (o pide autenticación adicional).
- [ ] Existe al menos un permiso que el usuario puede otorgar/revocar y el sistema lo respeta.

---

### Fase 4 — Agentes reales

**Qué se construye:** se reemplaza el EchoAgent por los agentes de verdad, cubriendo los casos de uso 1-4 del MD 1.

- `CalendarAgent`: consultar y crear eventos (API de Google Calendar u otra).
- `FileAgent`: buscar y leer archivos en una carpeta restringida.
- `TelegramAgent`: buscar contacto y enviar mensaje (ya con el flujo de confirmación de la Fase 3 aplicado).

**Definición de Terminado:**
- [ ] Caso de uso 1 (consultar calendario) funciona de punta a punta.
- [ ] Caso de uso 3 (buscar documento local) funciona de punta a punta.
- [ ] Caso de uso 4 (buscar contacto y enviar mensaje) funciona de punta a punta, con confirmación incluida.
- [ ] Cada agente respeta estrictamente su contrato de capacidades (no puede hacer nada fuera de lo declarado en el MD 1).

---

### Fase 5 — Auditoría + explicabilidad + kill switch

**Qué se construye:** las piezas que generan confianza, descritas en el MD 1 secciones 7 y 8.

- Log append-only de cada acción ejecutada (quién, qué, cuándo, autorizado por quién).
- Endpoint/consulta para que el usuario vea su historial reciente.
- Respuesta explicativa básica ("hice X porque me pediste Y").
- Kill switch: un comando/acción que revoca sesiones, tokens y detiene ejecución inmediatamente.

**Definición de Terminado:**
- [ ] Cada acción ejecutada en las fases anteriores queda registrada y es consultable.
- [ ] El usuario puede preguntar "¿por qué hiciste esto?" y recibir una respuesta coherente basada en el log.
- [ ] Activar el kill switch bloquea efectivamente cualquier acción posterior hasta reactivar el sistema.

---

### Fase 6 — Caso académico + pruebas de robustez

**Qué se construye:** el último caso de uso del MVP, más las pruebas que demuestran que la arquitectura realmente es lo que dice ser.

- Caso de uso 5: consulta de información académica (API simulada si no hay una real disponible).
- Prueba de acción bloqueada.
- Prueba de acción con confirmación rechazada por el usuario.
- Prueba de revocación de un permiso ya otorgado.
- Prueba de caída de un agente/worker sin que el resto del sistema falle.
- Prueba de incorporación de un agente nuevo **sin modificar el Orchestrator** (la prueba clave de que el diseño plug-and-play funciona de verdad).

**Definición de Terminado:**
- [ ] Caso de uso 5 funciona de punta a punta.
- [ ] Las 5 pruebas de robustez están documentadas (video corto o pasos reproducibles) y pasan.

---

### Fase 7 — Perfil asistido / adulto mayor

**Qué se construye:** el segundo dominio de demostración del MD 1, sección 10.

- Perfil de usuario asistido con interfaz simplificada (aunque sea dentro de Telegram/Discord por ahora, sin app propia todavía).
- Configuración de permisos por parte de un familiar/cuidador.
- Niveles de autonomía graduales (informar → guiar → preparar → confirmar → ejecutar) para al menos un caso de uso sensible (ej. "quiero pagar la luz").

**Definición de Terminado:**
- [ ] Existe una distinción real (no solo cosmética) entre usuario estándar y usuario asistido en el comportamiento del sistema.
- [ ] Un cuidador puede configurar permisos para el usuario asistido y el sistema los respeta.
- [ ] Al menos un caso de uso demuestra los niveles graduales de autonomía en vez de ejecutar directo.

---

### Fase 8 — Interfaz propia (opcional, si alcanza el tiempo)

**Qué se construye:** solo si las fases 0-7 están sólidas y sobra tiempo antes de diciembre.

- App móvil (Flutter) o de escritorio (Tauri), decisión pospuesta hasta este punto.
- Deep links a WhatsApp/Discord/Telegram con mensaje pre-armado.
- Pantallas mínimas: "¿qué necesitas?" con botón de hablar, historial/auditoría, gestión de permisos, perfil estándar vs. asistido.

**Definición de Terminado:** se define cuando se llegue a esta fase, en función del tiempo real disponible.

---

## 6. Qué hacer cuando una fase queda bloqueada

1. Subir el trabajo tal cual está a la rama de esa fase (no perder el avance).
2. Actualizar la tabla de estado a `Bloqueada`, con una línea corta de la causa.
3. Dejar un comentario o issue en GitHub describiendo específicamente qué falla (error, decisión pendiente, dependencia externa) para que el otro pueda retomarlo sin tener que reconstruir el contexto.
4. No avanzar a la siguiente fase saltándose la bloqueada, salvo que ambos decidan explícitamente que es necesario y lo anoten aquí.
