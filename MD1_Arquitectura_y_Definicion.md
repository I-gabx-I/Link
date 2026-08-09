# MD 1 — Arquitectura y Definición del Sistema

## 1. Visión general del sistema

El sistema es un **asistente personal multi-agente**: el usuario escribe (o habla) en lenguaje natural desde una interfaz de chat, y el sistema interpreta la intención, decide si puede/debe ejecutarla, la ejecuta a través de un agente aislado, y responde — dejando todo registrado.

La idea central que debe sostenerse en cualquier decisión de diseño:

> **El LLM decide qué se *quiere* intentar. Nunca decide ni ejecuta directamente sobre el sistema.**

Entre "lo que el LLM propone" y "lo que realmente pasa" siempre hay una capa de control (Policy Engine) que no depende del LLM y que no puede saltarse.

## 2. Diagrama de componentes

```
      Telegram / Discord (Fase 1-3)
                  │
                  ▼
          Identity Gateway
      (¿quién eres? / autenticación)
                  │
                  ▼
            Orchestrator
                  │
        ┌─────────┴─────────┐
        ▼                   ▼
    LLM Planner         Policy Engine
  (propone acción)   (¿está permitido?
        │              ¿requiere confirmación?)
        └─────────┬─────────┘
                  ▼
             Tool Router
                  │
      ┌───────────┼───────────────┐
      ▼           ▼               ▼
  Telegram     Calendar        File Agent
   Agent        Agent
      │           │               │
   Telegram    Calendar API   Sistema de archivos
     API                       restringido
                  │
                  ▼
        Auditoría (log append-only)
```

**Componentes:**

| Componente | Responsabilidad | Lo que NUNCA hace |
|---|---|---|
| **Identity Gateway** | Identifica y autentica al usuario que escribe, sin importar la plataforma (Telegram, Discord) | No ejecuta acciones ni conoce lógica de negocio |
| **Orchestrator** | Recibe el mensaje ya identificado, coordina el flujo completo (planificación → política → ejecución → respuesta → auditoría) | No tiene credenciales de servicios externos |
| **LLM Planner** | Traduce lenguaje natural a una intención estructurada (acción + parámetros) | No ejecuta nada, no tiene acceso a tokens ni al sistema operativo |
| **Policy Engine** | Evalúa la intención propuesta contra permisos del usuario y nivel de riesgo de la acción; decide: ejecutar / confirmar / bloquear | No interpreta lenguaje natural, no ejecuta la acción en sí |
| **Tool Router** | Una vez aprobada la acción, la dirige al agente correspondiente | No decide si algo está permitido — solo enruta |
| **Agentes** (Telegram, Calendar, File, etc.) | Ejecutan la acción real usando las credenciales correspondientes, aislados unos de otros | No tienen acceso a nada fuera de sus capacidades declaradas |
| **Auditoría** | Registra cada acción ejecutada, quién la autorizó y por qué | No puede ser modificada ni borrada por el sistema (append-only) |

## 3. Contrato de capacidades por agente

Cada agente declara explícitamente qué puede hacer. Nada implícito, nada "acceso total a X".

Ejemplo — `TelegramAgent`:

```
CAPABILITIES
--------------
send_message(contact, text)
read_recent_messages(contact, limit)
search_contact(name)
```

Ejemplo — `CalendarAgent`:

```
CAPABILITIES
--------------
read_events(date_range)
create_event(title, datetime, description)
```

Ejemplo — `FileAgent`:

```
CAPABILITIES
--------------
search_file(query, scope: "Documentos/Universidad")
read_file(file_id)
```

Lo que el LLM Planner produce nunca es "haz lo que necesites con Telegram" — siempre es una llamada a una de estas capacidades declaradas, con sus parámetros:

```json
{
  "tool": "telegram.send_message",
  "recipient": "Luis",
  "message": "Llegaré tarde"
}
```

## 4. Modelo de permisos

Inspirado en el modelo de permisos de Android: el usuario otorga permisos por agente/capacidad, no acceso general al sistema.

Al vincular un agente, el usuario ve exactamente qué se le está pidiendo:

```
TelegramAgent solicita permiso para:
☐ Leer contactos
☐ Enviar mensajes

FileAgent solicita permiso para:
☐ Leer carpeta Documentos/Universidad
   (NO acceso completo al disco)
```

Los permisos pueden otorgarse con distintos niveles de duración:

```
Permitir acceso a Documentos/Universidad

○ Una vez
○ Durante 1 hora
○ Siempre (revocable en cualquier momento)
```

## 5. Clasificación de acciones por nivel de riesgo

| Nivel | Ejemplo | Comportamiento del sistema |
|---|---|---|
| 0 | Consultar calendario | Ejecutar directo |
| 1 | Buscar archivo | Ejecutar directo |
| 2 | Crear recordatorio | Ejecutar y notificar |
| 3 | Enviar mensaje | Pedir confirmación |
| 4 | Modificar/eliminar archivo | Confirmación fuerte (mostrar detalle exacto de la acción) |
| 5 | Pago, eliminación irreversible, acción sensible | Bloquear, o requerir autenticación adicional (PIN/biométrico) |

Ejemplo de confirmación en nivel 3:

```
Voy a enviar este mensaje a Luis:

"Renuncio al proyecto."

¿Enviar?  [Sí]  [No]
```

Esta tabla no es definitiva — se ajusta según los agentes reales que se implementen, pero el principio de "a mayor riesgo, mayor fricción intencional" se mantiene fijo.

## 6. Secretos y seguridad de credenciales

Regla fija: **el LLM nunca ve contraseñas, tokens ni API keys.**

- Los tokens de cada integración (Telegram, Google, etc.) se guardan en un almacenamiento de secretos separado del flujo de planificación (ej. variables de entorno gestionadas de forma segura, o un vault si se justifica).
- El LLM Planner solo produce la intención estructurada (`{"tool": "...", "params": {...}}`).
- Un servicio separado (el agente correspondiente) es el único que lee el secreto y ejecuta la llamada real a la API externa.

## 7. Auditoría y explicabilidad

Todo lo que el sistema ejecuta debe quedar registrado y ser consultable por el usuario:

```
Hoy

20:14  Telegram Agent — Envió mensaje a Luis — Autorizado por usuario
18:32  Calendar Agent — Consultó calendario — Ejecución automática
15:08  File Agent — Accedió a /Universidad/Redes/
```

Y el sistema debe poder responder por qué hizo algo:

> "Abrí ese documento porque me pediste buscar la presentación que usaste ayer, y fue el archivo con mayor coincidencia."

Esto no es un "nice to have" — es parte de la pregunta de investigación del proyecto (ver sección 10).

## 8. Kill switch

Un control simple y visible: **"Desactivar agentes"**, que de forma inmediata:

- Revoca todas las sesiones activas.
- Detiene los workers en ejecución.
- Revoca todos los tokens.
- Bloquea cualquier ejecución hasta que el usuario vuelva a activar el sistema manualmente.

Sencillo de implementar, pero es una de las piezas que más transmite confianza ante la terna.

## 9. Los 5 casos de uso del MVP

Este es el catálogo mínimo que se debe demostrar de punta a punta (lenguaje natural → intención → autorización → agente → ejecución → respuesta → auditoría) antes de sumar nada más:

1. **Consultar calendario** — "¿Qué tengo mañana?" → nivel de riesgo 0, ejecución directa.
2. **Crear un recordatorio** — "Recuérdame entregar el proyecto el viernes" → nivel 2, ejecuta y notifica.
3. **Buscar un documento local** — "Busca el PDF de Arquitectura que usé ayer" → nivel 1, ejecución directa.
4. **Buscar contacto y enviar mensaje por Telegram** — "Avísale a Luis que llego tarde" → nivel 3, requiere confirmación.
5. **Consultar información académica** (API simulada o real de la universidad) — "¿Ya pagué la universidad este mes?" → nivel 0-1 dependiendo de si hay datos sensibles de por medio.

Una vez estos 5 funcionen de punta a punta, el sistema también debe poder demostrar (no son casos de uso nuevos, son pruebas de robustez de la arquitectura):

- Una acción bloqueada por el Policy Engine.
- Una acción que requiere confirmación y el usuario la rechaza.
- Revocación de un permiso ya otorgado.
- Caída de un worker/agente sin que el resto del sistema se caiga.
- Incorporación de un agente nuevo sin modificar el Orchestrator (prueba de que la arquitectura plug-and-play realmente funciona).

## 10. Los dos perfiles de usuario

**Usuario estándar** (ej. estudiante): acceso a más agentes y automatización, interfaz de chat normal.

**Usuario asistido** (ej. adulto mayor): interfaz simplificada, potencialmente solo:

```
┌───────────────────────────────┐
│                               │
│       ¿Qué necesitas?         │
│                               │
│          🎤 HABLAR            │
│                               │
└───────────────────────────────┘
```

Con un familiar/cuidador autorizado que configura, para ese usuario asistido:

```
María (hija) — permisos configurados para papá:

✓ WhatsApp
✓ Llamadas
✓ Ubicación
✓ Calendario
✗ Transferencias bancarias — Bloqueadas
```

Y niveles de autonomía graduales para acciones sensibles (en vez de asumir "agente inteligente" = "agente autónomo"):

```
Informar → Guiar → Preparar acción → Solicitar confirmación → Ejecutar
```

Ejemplo: si el usuario asistido dice "quiero pagar la luz", el sistema no ejecuta el pago — responde algo como "encontré el portal de pago, puedo abrirlo y guiarte paso a paso."

## 11. Pregunta de investigación del proyecto

Todo lo anterior (capabilities, permisos, niveles de riesgo, confirmación, auditoría, kill switch) existe para responder una sola pregunta, que es la que le da peso académico al proyecto más allá de "hicimos un bot":

> **¿Cómo permitimos que una IA actúe en nombre de una persona sin obligarla a confiar ciegamente en ella?**

Esta pregunta debería aparecer explícitamente en la introducción del informe final y ser el hilo conductor de la defensa ante la terna.

## 12. Lo que este documento NO define (a propósito)

- Stack tecnológico específico, estructura de carpetas y lenguajes por componente → **MD 3**.
- Orden de implementación y qué se considera "fase terminada" → **MD 2**.
- Detalles de UI de la interfaz propia (si se llega a construir) → se define más adelante, sin bloquear el resto.
