# MD 5 — Plan de Producto (v2)

## 1. Por qué existe este documento

El 29 de octubre de 2026 se replanificó el proyecto. En la revisión del 16 de octubre, el ingeniero indicó que el avance era insuficiente (de calificarse ese día, la nota habría estado entre 30 y 40 puntos). Para presentarse ante la terna no basta con una arquitectura bien diseñada y documentada: hay que presentar un **producto** funcional. De ahí salieron el manual de branding y la app **Guardian**.

El MD 2 sigue siendo válido como historial. Sus Fases 0 a 4 están terminadas y son la base de todo lo que sigue. Las Fases 5 a 8 del MD 2 no se descartan: quedan absorbidas y reordenadas en las fases de este documento (ver columna "Origen" en la tabla).

## 2. Principios de la replanificación

1. **Se usa lo que ya existe.** Esto incluye el backend de las Fases 0 a 4, el endpoint `/app/message` con el campo `type` y el mockup de Android con el branding Guardian. Lo que sobre se elimina y lo que falte se agrega en el camino. No se reescribe nada que ya funcione.
2. **Las garantías del MD 1 no se negocian.**
    - El Orchestrator y el Policy Engine siguen siendo los únicos que deciden qué se ejecuta.
    - La app es solo una interfaz más, igual que Telegram.
    - Ningún secreto vive en el cliente.
    - Toda acción con riesgo 3 o mayor pide confirmación.
3. **Todo código integrado lo entienden y pueden explicar ambos integrantes.** Las herramientas de IA se usan como asistentes, y su uso se documenta (sección 6).
4. **Una rama por fase.** Cada fase tiene su Definición de Terminado y solo se mergea a `main` cuando la cumple.

## 3. Tabla de estado

| Fase | Nombre | Origen (MD 2) | Estado | Rama |
|---|---|---|---|---|
| P1 | Base de la app | Fase 8 | Terminada y probada | `fase-app-android` (ya existe) |
| P2 | Cuentas y autenticación | Fase 7 | Pendiente | `p2-cuentas-auth` |
| P3 | Servicios | Fase 4 ampliada | Pendiente | `p3-servicios` |
| P4 | Supervisión | Fase 5 | Pendiente | `p4-supervision` |
| P5 | Voz | Nueva | Pendiente | `p5-voz` |
| P6 | Despliegue y robustez | Fase 6 | Pendiente | `p6-despliegue` |
| P7 | Multiplataforma (opcional) | Nueva | Pendiente | `p7-multiplataforma` |

## 4. Detalle de cada fase

### P1 — Base de la app

**Qué se construye:** la app deja de ser un mockup y se convierte en un cliente real del backend.

- **Inventario de pantallas del mockup.** Se decide en el camino cuáles se quedan, cuáles se eliminan y cuáles faltan.
    - Configuración inicial: tipo de cuenta, datos del adulto mayor, contactos, apps, permisos, listo.
    - Cuidador: panel, configuración, pausar asistente, ajustes de contactos, apps y permisos.
    - Adulto mayor: inicio, escuchando, confirmación.
- **Arquitectura de la app:** pantallas → ViewModel → Repository → HTTP. Ninguna pantalla llama a la red directamente.
- **Conexión por texto a `/app/message`.** La pantalla que se muestra depende del `type` de la respuesta (`confirmation | result | denied | info | error`).
- **Secreto compartido temporal.** Se usa `APP_SHARED_SECRET` mientras tanto; P2 lo reemplaza.

**Definición de Terminado:**
- [X] El inventario de pantallas está documentado: qué se usa y qué se eliminó.
- [X] La app envía una solicitud escrita y muestra la respuesta del backend según su `type`.
- [X] El flujo de confirmación funciona desde la app: pantalla de confirmación → sí/no → resultado.
- [X] Ninguna pantalla llama a la red directamente; todo pasa por ViewModel y Repository.
- [X] La dirección del backend se configura sin recompilar la app.

**Resultado del inventario (29 de octubre):**
- **Se quedan todas las pantallas del mockup**, porque casi todas corresponden a algo del backend:
  - "Aplicaciones" = `granted_tools`;
  - "Permisos" = los niveles de riesgo (0 a 2 directo, 3 y 4 con confirmación, 5 bloqueado);
  - "Pausar asistente" = kill switch (P4);
  - "Escuchando" = voz (P5).
- **No se eliminó ninguna pantalla.** Los datos fijos ("Don Carlos", "María") se reemplazan por datos reales en P2.
- **Se agregaron:**
  - "Escribir una solicitud": la solicitud y la respuesta según su `type`, con la confirmación;
  - "Conexión del asistente": temporal.

**Decisiones técnicas:**
- **Cliente HTTP:** Ktor, con kotlinx.serialization. Es multiplataforma, así que la capa de red se reutiliza tal cual si se migra a Compose Multiplatform (P7).
- **`compileSdk` 37:** lo exige OkHttp 5.5, que usa Ktor por dentro. No cambia `minSdk` ni `targetSdk`.

**Elementos temporales:**
- **La clave compartida** se lee de `local.properties` y entra a la app como `BuildConfig`. Se elimina en P2.
- **La pantalla "Conexión del asistente"**, con la URL manual. Se elimina en P6.

---

### P2 — Cuentas y autenticación

**Qué se construye:** saber de verdad quién usa el sistema, y que el cuidador configure la cuenta del adulto mayor.

- **Cuenta del cuidador:** Firebase Authentication, con correo o con Google. Antes de empezar se verifican los límites del plan gratuito.
- **Vinculación del adulto mayor sin contraseña.** El cuidador genera un código desde su panel y lo ingresa una sola vez en el teléfono del adulto mayor.
- **Verificación en el Gateway.** El Gateway verifica el token de Firebase y deriva de él el `internal_user_id`. Con esto se elimina `APP_SHARED_SECRET`.
- **Roles en Firestore** (cuidador y adulto mayor), con la relación entre el cuidador y el o los adultos mayores que tiene a cargo.
- **El panel del cuidador configura lo real:** los contactos, los servicios habilitados (`granted_tools`) y las acciones que piden confirmación.

**Decisión (29 de octubre):** se diseña para dos teléfonos, porque el cuidador administra a distancia y así el historial y el kill switch funcionan sin tener el teléfono del adulto mayor en la mano. Se construye primero en un solo teléfono, con el cuidador y el adulto mayor como identidades separadas desde el inicio, y después se agrega la vinculación del segundo teléfono.

**Definición de Terminado:**
- [ ] Un cuidador crea su cuenta e inicia sesión.
- [ ] El cuidador vincula el teléfono del adulto mayor con un código, sin que el adulto mayor use contraseña.
- [ ] El Gateway rechaza toda solicitud sin un token válido, y el secreto compartido ya no existe en el código.
- [ ] Lo que el cuidador configura cambia de verdad lo que el sistema le permite al adulto mayor.

---

### P3 — Servicios

**Qué se construye:** la mayor cantidad posible de servicios útiles para el adulto mayor.

**Regla para cada servicio nuevo:**
- tiene su capacidad declarada en un agente, según el contrato del MD 1;
- tiene un nivel de riesgo en `RISK_LEVELS`;
- aparece en la pantalla "Apps" del cuidador para habilitarlo o deshabilitarlo.

**Ronda de investigación al inicio de la fase.** Se documenta en el MD 4: servicio, cómo se implementa, nivel de riesgo y lote.

**Ideas del ingeniero** (de la revisión): correo, cámara, archivos, Spotify, notas, llamadas, y en general "todo dentro de la aplicación".

**Lote 1 — Deep links.** Son baratos y de alto impacto en una demo. El sistema prepara la acción y la persona la confirma:
- llamadas (`tel:`)
- WhatsApp (`wa.me`)
- Telegram (`t.me`; falta que el agente devuelva `action_url`)
- correo
- música (Spotify)
- navegación ("llévame a…")

**Lote 2 — Datos propios:**
- calendario (ya existe en el backend)
- recordatorios de medicinas con notificación
- notas

**Lote 3 — Funciones nativas del teléfono:**
- cámara y fotos
- archivos del teléfono

**Para investigar:** un botón SOS que llame al cuidador y le comparta la ubicación del adulto mayor.

**Definición de Terminado:**
- [ ] La ronda de investigación está documentada en el MD 4.
- [ ] El Lote 1 funciona completo de punta a punta desde la app.
- [ ] Cada servicio nuevo tiene documentados su contrato de capacidades y su nivel de riesgo.
- [ ] El alcance de los Lotes 2 y 3 está decidido según el tiempo restante y anotado aquí.

---

### P4 — Supervisión

**Qué se construye:** las piezas de confianza del MD 1 (secciones 7 y 8), visibles en el panel del cuidador.

- **Log append-only** de cada acción ejecutada: quién, qué, cuándo y quién la autorizó.
- **Historial en el panel del cuidador**, en la sección "Actividad reciente" del mockup.
- **Kill switch** en la pantalla "Pausar asistente", con duración.
- **Explicación básica:** "hice X porque me pediste Y".

**Definición de Terminado:**
- [ ] Cada acción ejecutada queda registrada y el cuidador la ve en su panel.
- [ ] Pausar el asistente bloquea cualquier acción hasta que se reactiva o se vence el tiempo elegido.
- [ ] Ante "¿por qué hiciste esto?", el sistema responde con base en el log.

---

### P5 — Voz

**Qué se construye:** que el adulto mayor pueda hablarle al asistente y escuchar su respuesta.

- **Interfaz `SpeechEngine`**, con una primera implementación nativa basada en `SpeechRecognizer`.
- **Transcripción visible** en pantalla antes de enviar la solicitud.
- **Respuesta hablada** con `TextToSpeech`.
- **Más adelante, Gemini** como segundo motor de voz. Se conecta con ephemeral tokens o con Firebase AI Logic, y la llave de Gemini nunca va dentro del APK.

**Definición de Terminado:**
- [ ] Una solicitud por voz funciona de punta a punta, con la transcripción visible.
- [ ] La respuesta del asistente se escucha en voz alta.
- [ ] Cambiar de motor de voz no requiere tocar las pantallas.

---

### P6 — Despliegue y robustez

**Qué se construye:** el sistema deja de depender de la PC de desarrollo y se demuestra que la arquitectura es lo que dice ser.

- **Despliegue en Google Cloud Run.** El plan B es una VM `e2-micro` corriendo el mismo `docker compose`.
- **Secretos en Secret Manager**, y alerta de presupuesto en la cuenta de facturación.
- **Aislamiento de agentes:** Cloud Run no tiene `internal_net`, así que se reemplaza por ingress interno más autenticación IAM entre servicios.
- **Pruebas de robustez** (las de la Fase 6 del MD 2):
    - acción bloqueada;
    - confirmación rechazada;
    - revocación de un permiso;
    - caída de un agente;
    - agente nuevo sin modificar el Orchestrator.

**Definición de Terminado:**
- [ ] El backend es accesible por HTTPS con una URL fija, sin depender de la PC.
- [ ] Los agentes no son accesibles desde internet.
- [ ] Las 5 pruebas de robustez están documentadas y pasan.
- [ ] Se elimina la pantalla "Conexión del asistente": la URL de Cloud Run va fija en la app.

---

### P7 — Multiplataforma (opcional)

**Qué se construye:** solo si P1 a P6 están sólidas.

- **Migración a Compose Multiplatform** para tener versión de escritorio, reutilizando el código Kotlin y Compose existente.
- **Código específico de Android** (reconocimiento de voz, TextToSpeech, deep links): se queda en `androidMain`, detrás de `expect/actual`.

**Definición de Terminado:** se define al llegar a esta fase.

## 5. Decisiones pendientes

- [ ] Fecha de la próxima revisión. Define cuántas fases se alcanzan antes.
- [ ] ¿El adulto mayor siempre tiene un cuidador, o también existe un usuario estándar que usa la app solo? El MD 1 contempla ambos perfiles, y esto cambia el diseño de P2.
- [ ] Nombre del sistema: la app es **Guardian**. ¿El repositorio, la arquitectura y los MD se renombran también, o "Link" queda como nombre interno?
- [ ] Caso de uso 5 (información académica, Fase 6 del MD 2): ¿se mantiene o se descarta en el producto?
- [ ] Límites gratuitos de Firebase Authentication, sobre todo si se considera el login por SMS.

## 6. Uso de herramientas de IA

Durante el desarrollo se usan asistentes de IA como apoyo para diseñar, investigar y escribir código. Todo código que se integra a `main` lo revisan, prueban y pueden explicar ambos integrantes.

La rama `codex/demo-telegram`, generada casi por completo por un asistente, se conserva solo como referencia y no se integra al proyecto. Dos hallazgos de esa rama sí se reutilizan:
- la red de la universidad bloquea la comunicación entre dispositivos;
- `adb reverse` por USB es la alternativa para conectar el teléfono al backend local.