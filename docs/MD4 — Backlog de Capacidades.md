# MD4 — Backlog de Capacidades (Visión de Producto Completo)

## 1. Por qué existe este documento

El MD2 define lo que el semestre **exige**: 9 fases, con 5 casos de uso de MVP, siguiendo el enfoque que pidió el profesor. Este documento es distinto — es el mapa de **hacia dónde puede crecer el producto** después de eso, sin mezclar ambas cosas y sin comprometer el enfoque que necesita el semestre.

Ninguna línea de este documento es una promesa de que se va a construir en el semestre. Es una lista priorizada para no perder de vista el panorama completo cada vez que surge una idea nueva — y para que "pensar en grande" no se sienta como caos, sino como una lista ordenada con su lugar específico.

## 2. Regla de oro que no cambia, sin importar cuántas capacidades se agreguen

Toda capacidad nueva —por simple que se sienta— sigue el mismo patrón ya validado en las Fases 0-2: contrato de capacidades explícito, nivel de riesgo declarado, agente aislado, registrado en `tools_registry`. Ninguna excepción. La disciplina no se negocia; lo que crece es la lista de agentes, no las reglas del juego.

## 3. Dos superficies técnicas distintas (la aclaración clave)

- **Bot de chat** (Telegram / Discord / WhatsApp, vía Bot API o webhook): solo puede leer lo que el usuario escribe y llamar APIs externas (calendario, archivos en la nube, deep links). No tiene ninguna forma de tocar el sistema operativo del teléfono.
- **App nativa propia** (Fase 8): la única superficie que puede pedir permisos reales del sistema operativo — galería, contactos nativos, alarmas del reloj, abrir otras apps. Es trabajo real y distinto a "hacer una interfaz bonita" — es lo que habilita todo el bloque B de abajo.

Esta distinción decide en qué categoría cae cada idea nueva que se les ocurra — es la primera pregunta a hacerse antes de discutir riesgo o prioridad.

## 4. Backlog — Categoría A: viable con el bot actual, sin esperar a la Fase 8

| Capacidad | Cómo se resuelve | Riesgo estimado |
|---|---|---|
| Consultar calendario | Google Calendar API | 0 |
| Crear recordatorio | Firestore + notificación programada por el bot | 2 |
| Buscar un documento (en una carpeta en la nube, ej. Drive) | Google Drive API | 1 |
| Enviar mensaje a un contacto | Deep link (Telegram/WhatsApp precargado; Discord solo abre chat) | 3 |
| **Llamar a un contacto** | Deep link `tel:+numero` — más simple que mensajería, un solo tap, funciona igual en cualquier teléfono | 3 |
| Consultar información académica | API institucional o simulada | 0-1 |
| Responder preguntas generales | Gemini (`chat`, ya construido) | 0 |
| Traducir o resumir un documento | Gemini | 0-1 |

| Leer contenido de imágenes (OCR o modelo con visión) | pytesseract (requiere binario tesseract en el Dockerfile) o mandarla a un modelo con visión (Gemini la soporta nativo) | 0-1 |

*(La capability de llamadas es una que no habían considerado y es más fácil de lo que parece — casi gratis agregarla una vez tengan el patrón de deep links funcionando.)*

## 5. Backlog — Categoría B: requiere la app nativa (Fase 8), no antes

| Capacidad | Qué necesita | Riesgo estimado |
|---|---|---|
| Buscar el último archivo/foto guardado en el teléfono | Permiso de galería (Android/iOS) | 3-4 |
| Crear una alarma del sistema | Permiso de alarmas | 2 |
| Abrir otra aplicación instalada | Intent/deep link nativo del sistema | 2 |
| Leer contactos nativos del teléfono (resolver nombres sin pedir username a mano) | Permiso de contactos | 3 |
| Enviar mensajes con cero toques | Accessibility Service | 5 — máxima cautela, revisión extra de Play Store |

## 6. Orden sugerido

1. **Terminar la Fase 3** (Policy Engine) sobre los 5 casos de uso ya definidos en el MD1 — no cambia, es la base de todo lo que sigue.
2. **Fase 4 ampliada:** en vez de limitarse a 3 agentes, construir todos los de la Categoría A, uno por uno, con el mismo ritmo y disciplina que ya llevan (capability contract + riesgo + prueba end-to-end por cada uno).
3. **Fases 5-7:** igual que en el MD2 (auditoría, kill switch, pruebas de robustez, perfil asistido), ahora demostradas sobre una lista más rica de agentes reales.
4. **Fase 8:** construir la app nativa, y recién ahí abrir la Categoría B — con el mismo rigor de permisos y niveles de riesgo, nunca saltándose el modelo de seguridad solo porque cambió la superficie técnica.

## 7. Nota sobre el ritmo del equipo

Si las fases se están cerrando en aproximadamente un día cada una, este backlog es la forma correcta de aprovechar esa velocidad: más agentes construidos con el mismo patrón, no un cambio en las reglas ni un salto directo a funciones que requieren la app nativa antes de tiempo. La velocidad se invierte en ampliar el ancho (más capacidades) del bloque A, no en saltarse el orden.