# MD 0 — Visión y Entorno de Trabajo

## 1. ¿Qué vamos a construir?

Un **asistente personal multi-agente**, accesible por chat (Telegram y Discord inicialmente), capaz de ejecutar tareas cotidianas — consultar calendario, enviar mensajes, buscar archivos, gestionar recordatorios, consultar información académica — a través de una arquitectura distribuida y segura, en la que:

- Un **Orquestador** (código propio) recibe los mensajes del usuario y coordina todo el flujo.
- Un **LLM** se usa únicamente para *planificar* (decidir qué acción se quiere intentar), nunca para ejecutar directamente sobre el sistema.
- Un **Policy Engine** decide si esa acción está permitida, si necesita confirmación del usuario, o si se bloquea.
- **Agentes** (microservicios aislados) son los únicos que ejecutan la acción real, cada uno con un contrato de capacidades explícito (qué puede y qué no puede hacer).
- Todo queda registrado en una **auditoría** consultable por el usuario ("¿por qué hiciste esto?").

El proyecto tiene dos dominios de demostración:

1. **Estudiante** — gestión académica (calendario, documentos, mensajes, trámites universitarios).
2. **Adulto mayor / accesibilidad** — perfil asistido, interfaz simplificada (un botón de "hablar"), con un familiar/cuidador que configura qué puede hacer el agente.

## 2. ¿Para qué lo construimos?

Este proyecto es la actividad de Seminario Profesional: de tres ideas propuestas por el equipo, esta fue la aceptada por el profesor. El objetivo del curso es desarrollarla lo más lejos posible durante el semestre (entrega en diciembre) para poder presentarla ante una terna como **examen privado** de graduación. No es el privado en sí — es la preparación para que la terna lo acepte como tal.

Por eso el estándar no es "que funcione para la clase", sino "que resista preguntas técnicas de un jurado de graduación": arquitectura defendible, decisiones de seguridad justificadas, y un MVP que demuestre ingeniería real (no una serie de integraciones superficiales).

## 3. Equipo

| Integrante | Rol |
|---|---|
| Marcos | *(a definir — ej. Orquestador / Policy Engine)* |
| Samuel | *(a definir — ej. Agentes / Integraciones)* |

Al ser solo dos personas, la división de trabajo debe ser clara para no pisarse el código, pero también flexible — ambos deben entender el sistema completo para poder defenderlo ante la terna, no solo su parte.

## 4. Principios que guían las decisiones técnicas

Estos ya salieron del análisis previo y conviene dejarlos escritos para no perderlos en el camino:

- **El LLM nunca tiene acceso directo al sistema operativo ni a credenciales.** Solo propone acciones; el Orquestador y el Policy Engine deciden si se ejecutan.
- **Cada agente declara sus capacidades explícitamente** (ej. `send_message`, `read_calendar`) — no acceso genérico "haz lo que quieras con X".
- **Las acciones se clasifican por nivel de riesgo** (0 a 5), desde "ejecutar directo" hasta "bloquear o requerir confirmación fuerte".
- **Los secretos (tokens, API keys) están separados del LLM**, en un almacenamiento seguro aparte.
- **Todo es auditable**: el usuario puede ver qué hizo el sistema y por qué.
- **Existe un kill switch**: revocar todos los permisos y detener agentes de inmediato.
- **No se usan herramientas de automatización visual (n8n, Zapier, etc.) para el núcleo del sistema.** El Orquestador, el Policy Engine y los Agentes se construyen en código propio, porque es lo que demuestra la ingeniería ante la terna.

## 5. Dónde vive el proyecto

- **Repositorio:** GitHub (privado mientras se desarrolla). Un solo repo, no vale la pena separar en múltiples repos con solo 2 personas — pero sí carpetas bien separadas por servicio (esto se detalla en el MD 3).
- **Documentación:** vive dentro del repo, en una carpeta `/docs`, en Markdown. Así el código y la documentación nunca se desincronizan — cada cambio de arquitectura se refleja en el mismo PR que el código que lo implementa.
- **Ramas:** `main` protegida (siempre debe estar en un estado funcional/demostrable). Trabajo por rama de feature o de fase (ej. `fase-1-esqueleto`), con PR hacia `main` antes de fusionar. Esto conecta directo con el plan de fases del MD 2: cada fase se sube probada, no a medias.
- **Gestión de tareas:** un tablero simple (GitHub Projects, que ya viene integrado al repo sin herramienta extra) con las tareas de cada fase.
- **Variables de entorno y secretos:** nunca se suben al repo. Se usa un archivo `.env.example` con las variables necesarias (sin valores reales) y cada quien mantiene su propio `.env` local, ignorado por Git.

## 6. Herramientas a instalar

| Herramienta | Para qué | Prioridad |
|---|---|---|
| **Git** | Control de versiones | Día 1 |
| **Cuenta de GitHub** + acceso al repo compartido | Repositorio del proyecto | Día 1 |
| **Python 3.11+** | Orquestador, Policy Engine, Agentes | Día 1 |
| **VS Code** (o editor de preferencia) | Entorno de desarrollo | Día 1 |
| **Docker Desktop** | Levantar todos los servicios juntos de forma consistente | Antes de Fase 2 (no urgente para las primeras líneas de código) |
| **Postman** o **Thunder Client** (extensión VS Code) | Probar endpoints del Orquestador/Agentes sin necesidad de la interfaz de chat | Desde que exista el primer endpoint |
| **Cuenta de bot en Telegram** (vía BotFather) | Primera interfaz de pruebas | Fase 1 |
| **Cuenta de bot en Discord** (Discord Developer Portal) | Segunda interfaz de pruebas | Fase 1 o 2 |
| **Cuenta de Firebase** (plan Spark, gratis) | Base de datos (Firestore) para usuarios, permisos, auditoría | Fase 2 |
| **Redis** (vía Docker) | Cache / cola de mensajes si se necesita | Cuando se defina en MD 3 |

No hace falta instalar todo el primer día — la tabla ya está ordenada por cuándo se vuelve necesario, alineado con las fases del MD 2.

## 7. Lo que este documento NO define (a propósito)

Quedan pendientes para los siguientes documentos, y está bien que no se resuelvan aquí:

- Los 5 casos de uso concretos del MVP → **MD 1**
- El orden exacto de fases y qué se considera "terminado" en cada una → **MD 2**
- Estructura de carpetas, lenguajes por componente, base de datos, mensajería → **MD 3**
- Si la interfaz final propia será app móvil o de escritorio → decisión pospuesta a fases finales, sin costo para la arquitectura porque la interfaz está desacoplada del backend.

Este documento es la base — se puede ajustar en el camino, pero los principios de la sección 4 son intencionalmente los que menos deberían cambiar, porque son el corazón de lo que hace al proyecto defendible ante la terna.
