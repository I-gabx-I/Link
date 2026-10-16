# Trabajo y prueba de la demo Android

Esta carpeta contiene el backend y la app del mismo proyecto. La rama local de trabajo es `codex/demo-telegram`, basada en `fase-app-android`, con el diseño actualizado de `mockup/link-demo` y la integración del flujo de mensajes.

## Cómo trabajamos

- Codex edita y revisa los archivos en esta carpeta.
- Android Studio abre la subcarpeta `android/`, sincroniza Gradle y ejecuta la app en el teléfono o emulador.
- Usamos este mismo chat para coordinar cambios y los resultados de las pruebas. No necesitas copiar código entre dos proyectos.
- Git se maneja desde la carpeta raíz `Link/`, donde también están `gateway/`, `orchestrator/` y `agents/`.

## Qué incluye este avance

1. Micrófono con reconocimiento real de Android y manejo de permisos/errores.
2. Entrada escrita para comprobar la conexión y la interpretación por separado.
3. Configuración de la dirección del backend y una clave temporal para la demo.
4. Alta y consulta de contactos en Firestore, con nombres y alias como “mamá”.
5. Interpretación con Gemini en el backend, usando los nombres y alias guardados.
6. Pregunta cuando falta un destinatario o el contenido del mensaje.
7. Confirmación o cancelación, respetando los permisos del servidor.
8. Apertura del chat de Telegram con el texto preparado. El usuario toca Enviar en Telegram.

La lista de contactos que muestra Android se actualiza desde el backend y conserva una copia local de la última consulta correcta. Si la conexión falla, la app muestra el error y conserva esa copia; no inventa contactos ni respuestas.

## Primero: abrir la app

Abre `android/` en Android Studio y espera a que termine la sincronización. El proyecto requiere Android SDK Platform 36.1, Gradle 9.5 y Java 21 para el proceso de Gradle; el wrapper y la selección de Java están declarados en el proyecto. Android Studio puede descargar las dependencias que falten.

Selecciona el módulo `app`, un teléfono con depuración USB o un emulador, y ejecuta Run.

La pantalla inicial muestra el micrófono. También puedes usar “Escribir una solicitud” y “Mis contactos”. Desde “Cuidador” puedes revisar la conexión.

## Después: configurar el backend local

La demo puede arrancarse con Python, sin Docker. Desde la raíz del repositorio, en PowerShell, con Python 3.11 o posterior:

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements-demo.txt
```

Crea `.env` copiando `.env.example`, si aún no existe, y completa en tu computadora:

```dotenv
LLM_API_KEY=TU_CLAVE_DE_GEMINI
FIREBASE_PROJECT_ID=link-multiagent
FIREBASE_CREDENTIALS_PATH=./database/serviceAccountKey.json
APP_SHARED_SECRET=UNA_CLAVE_ALEATORIA_DE_AL_MENOS_32_CARACTERES
GEMINI_MODELS=gemini-3.1-flash-lite
ENABLED_TOOLS=chat,contact_list,contact_add,telegram_send
```

La clave de Gemini se mantiene en el backend. Android recibe únicamente la clave temporal `APP_SHARED_SECRET`, que protege la conexión de esta demo. Para generar esa clave localmente:

```powershell
.\.venv\Scripts\python.exe -c "import secrets; print(secrets.token_urlsafe(32))"
```

Guarda la credencial administrativa de Firebase dentro de `database/serviceAccountKey.json`. Puede reutilizarse una credencial válida del proyecto que ya usan. Las instrucciones oficiales para obtener y configurar la credencial están en [Firebase Admin SDK](https://firebase.google.com/docs/admin/setup).

`.env` y los archivos de `database/` están excluidos de Git. No hace falta enviar estas claves al chat.

Inicia los tres servicios:

```powershell
.\.venv\Scripts\python.exe scripts/run_demo.py
```

El lanzador comprueba la configuración, arranca el agente de Telegram en el puerto 8006 y el orquestador en el 8001, ambos accesibles solo desde esta computadora. El gateway escucha en el puerto 8000 para permitir la conexión del teléfono. Mantén abierta la terminal; Ctrl+C detiene los tres procesos.

El primer arranque puede tardar mientras se cargan las dependencias. El lanzador espera hasta 180 segundos por servicio y avisa cuál está iniciando. Si un servicio se cierra, muestra el fallo de inmediato.

Para este recorrido no necesitas configurar un bot de Telegram. La acción utiliza la cuenta personal que ya tiene abierta la app Telegram del teléfono.

## Conectar Android

Desde la pantalla del micrófono, toca **Cuidador**, arriba a la derecha. En el panel del cuidador, toca **Conexión del backend**. Se abrirá la pantalla titulada **Conectar con el asistente**, con los campos **Dirección del backend** y **Clave de conexión**. También puedes llegar desde **Mis contactos → Revisar conexión**.

- Emulador de Android Studio: `http://10.0.2.2:8000`.
- Teléfono físico: `http://IP_LOCAL_DE_LA_COMPUTADORA:8000`, con ambos equipos en la misma red.
- Clave de conexión: el valor de `APP_SHARED_SECRET` de tu `.env`.

Toca “Guardar y comprobar”, debajo de los campos. Si el teclado lo cubre, ciérralo o desplaza el contenido hacia arriba para ver la parte inferior de la pantalla. Debe aparecer “Conexión lista. Contactos actualizados.”

Si el teléfono no conecta, abre esa dirección en el navegador del teléfono. La raíz del gateway debe responder `gateway alive`. Revisa que el backend esté encendido, que estés usando la IP local correcta y que el firewall permita la conexión por la red privada. El APK de desarrollo permite HTTP local; una distribución de producción requiere HTTPS.

### Regla de Wi-Fi en Windows

Si la computadora alcanza el gateway y el telefono no, el script `scripts/allow_demo_wifi.ps1` permite crear una regla limitada al ejecutable de Python del backend, TCP 8000, la IP local de la computadora y la IP de ese telefono. Solo se aplica a una conexion marcada como **Private** en Windows. No modifica otras reglas ni cambia el perfil de la red.

Usa esta regla solo en una red propia de confianza, como tu hotspot; que Windows marque la red universitaria como Private no la convierte en una red privada. Desde la raiz del repositorio puedes revisar el cambio sin aplicarlo, reemplazando ambos valores por las IP actuales:

```powershell
.\scripts\allow_demo_wifi.ps1 -ComputerAddress IP_DE_LA_PC -PhoneAddress IP_DEL_TELEFONO -WhatIf
```

Comprueba las direcciones de nuevo si cambias de red. Para aplicarlo, ejecuta el mismo comando sin `-WhatIf` en **PowerShell como administrador**. Windows exige ese permiso para modificar el firewall.

Para retirar exclusivamente esta regla temporal, tambien como administrador:

```powershell
.\scripts\allow_demo_wifi.ps1 -Remove
```

Si la red Wi-Fi bloquea la comunicacion entre sus clientes, esta regla no elimina ese bloqueo. En ese caso usa el enlace USB o una red que permita la comunicacion local.

### Alternativa por USB

Si el navegador del teléfono tampoco alcanza el backend por Wi-Fi, puedes usar ADB por USB con el APK ya instalado. No hace falta compilar otra vez ni iniciar un emulador.

1. Activa **Opciones de desarrollador → Depuración USB** en Android.
2. Conecta un cable USB de datos y acepta en el teléfono la autorización de esta computadora.
3. Con el backend encendido, ejecuta desde PowerShell:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices -l
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -d reverse tcp:8000 tcp:8000
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -d reverse --list
```

El dispositivo debe figurar como `device`. Si muestra `unauthorized`, desbloquéalo y acepta el aviso de depuración. Si no figura, revisa el cable, el modo USB y el controlador del fabricante.

En la app usa **http://127.0.0.1:8000**, conserva la misma clave `APP_SHARED_SECRET` y toca **Guardar y comprobar**. Esa dirección funciona en el teléfono después de preparar el túnel USB; `10.0.2.2` sigue siendo la dirección especial del emulador. Conserva el cable conectado durante la prueba.

Si se pierde la conexión USB, vuelve a conectar el teléfono, comprueba que aparezca como `device` y repite el comando `reverse tcp:8000 tcp:8000` antes de reintentar desde la app. El backend puede seguir encendido aunque el túnel del teléfono haya desaparecido.

Para restablecer ese enlace automaticamente cuando se reconecta el telefono, deja esta utilidad abierta en otra terminal, desde la raiz del repositorio:

```powershell
.\.venv\Scripts\python.exe -u scripts/connect_demo_usb.py
```

Usa `--once` para una sola comprobacion. Si hay varios dispositivos, indica `--serial SERIAL_DEL_TELEFONO`. La utilidad solo configura el puerto USB 8000; no cambia el firewall, no arranca el backend ni confirma acceso a Firebase. Si Windows solo carga el telefono y ADB no lo detecta, revisa el cable de datos, el puerto y la depuracion USB primero.

El APK **1.1-demo-usb** (versionCode 2) incluye **Usar conexión USB**, que selecciona `http://127.0.0.1:8000`. Despues toca **Guardar y comprobar**: primero comprueba el gateway y luego muestra **El backend responde. Verificando la clave y cargando contactos…**. El resultado final sigue siendo **Conexión lista. Contactos actualizados.** La lista tambien tiene **Actualizar contactos** para comprobar un guardado que haya terminado con tiempo de espera. Instala la actualizacion sobre la app existente para conservar su configuracion y su identificador.

Para retirar solo este túnel:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -d reverse --remove tcp:8000
```

Referencias: [conectar un dispositivo Android](https://developer.android.com/studio/run/device) y [comandos oficiales de ADB](https://android.googlesource.com/platform/packages/modules/adb/+/refs/heads/main/docs/user/adb.1.md).

## Prueba funcional que debemos completar en el teléfono

1. Agrega una persona real desde “Mis contactos”: nombre, su usuario de Telegram y un alias opcional.
2. Cierra y vuelve a abrir la app. Verifica que el contacto siga apareciendo.
3. Primero escribe una solicitud y comprueba la confirmación del servidor.
4. Cancela una solicitud y verifica que no aparezca un enlace para enviarla.
5. Pide un mensaje sin decir su contenido y completa la respuesta cuando el asistente pregunte.
6. Dicta una solicitud con otras palabras y comprueba el destinatario y el texto en la confirmación.
7. Confirma, toca “Abrir Telegram”, revisa y toca Enviar dentro de Telegram.
8. Comprueba la recepción desde la cuenta de la otra persona.

Las pruebas automáticas del repositorio usan Firebase y el modelo simulados para verificar las conexiones entre servicios, permisos y confirmaciones. No demuestran por sí solas la calidad de la interpretación de Gemini, el micrófono de un teléfono particular ni la entrega en Telegram. Esa comprobación requiere las claves locales y la prueba anterior.

Verificación local del 16 de octubre de 2026: APK de desarrollo compilado correctamente, revisión estática de Android sin errores (15 advertencias de actualizaciones y estilo) y 14 pruebas del backend aprobadas con servicios externos simulados. El APK compilado es `Link-demo-debug.apk`, en la carpeta `outputs`, junto al repositorio `Link`.

También se completó una prueba con los tres servicios locales y las credenciales reales de Firebase y Gemini: guardar y volver a leer un contacto, interpretar una solicitud natural y un alias, pedir confirmación, cancelar, preguntar por el texto faltante, completar el borrador y generar el enlace de Telegram tras confirmar. Se utilizó un perfil temporal y se eliminaron exclusivamente los documentos creados para esa prueba. No se envió ningún mensaje desde esa comprobación. Las 14 pruebas automáticas también pasaron en el entorno virtual local instalado.

El gateway responde desde la computadora tanto por la dirección de Wi-Fi como por localhost. En la comprobación posterior, el navegador de Android no pudo alcanzar la dirección de Wi-Fi; esa conexión sigue pendiente. Se estableció el túnel ADB por USB y se verificó HTTP 200 desde Android. La app, usando `http://127.0.0.1:8000`, mostró **Conexión lista. Contactos actualizados.** El dictado dentro de Android y la recepción real en Telegram siguen pendientes de completar en el dispositivo.

Al retomar en otra red el mismo dia, la computadora obtuvo `10.1.12.24/16` y el telefono `10.1.11.25`. Se reinicio el backend y el usuario verifico **gateway alive** desde el navegador del telefono en `http://10.1.12.24:8000`. No se aplico la regla de firewall preparada. Queda comprobar la conexion autenticada de la app en esa direccion, el contacto real y el recorrido de voz hasta Telegram.

En el intento posterior la app no pudo conectar, y el usuario aclaro que se trata de la red publica de la universidad. No se confirmo acceso desde la app por esa red ni se aplicaron cambios al firewall. Para continuar la demo se propuso una red propia mediante hotspot o el enlace USB ya comprobado; la politica de comunicacion entre clientes de la red universitaria sigue sin confirmarse.

Al retomar por USB se repitio con exito la prueba real del backend: clave, guardar y leer un contacto temporal en Firestore, interpretar la solicitud con Gemini, confirmar, cancelar, preguntar por texto faltante y preparar el enlace de Telegram. Se elimino exclusivamente el perfil temporal de prueba y no se envio un mensaje. El telefono aun no aparecia en ADB; el usuario reporto que el cable solo cargaba. La entrega real desde Android sigue pendiente.

En la siguiente comprobacion se compilo **1.1-demo-usb** sin reutilizar la compilacion incremental de Kotlin, se verifico el APK con versionCode 2 y lint termino con 0 errores y 15 advertencias. Se entrego `outputs/Link-demo-1.1-debug.apk` y se actualizo tambien `outputs/Link-demo-debug.apk`. El HONOR volvio a aparecer por USB, el usuario acepto la autorizacion y se instalo la actualizacion conservando los datos.

Para la prueba de Wi-Fi el usuario autorizo expresamente una regla limitada a su telefono, la IP de la laptop, Python y TCP 8000. La regla se aplico y se **retiro** al retomar USB; no se llego a confirmar acceso por la red de la U despues de aplicarla. El backend se habia detenido y se reinicio. Finalmente se comprobo HTTP 200 desde el HONOR por USB, con su clave y perfil reales, y una lista vacia de contactos. El usuario confirmo **Conexión lista. Contactos actualizados.** en el APK 1.1. El siguiente paso pendiente es guardar el contacto real desde la app y completar el dictado hasta Telegram.

## Alcance de seguridad y siguientes pasos

El usuario confirmo posteriormente que pudo guardar el contacto real desde Android y que aparece en la lista. Sigue pendiente confirmar en el telefono el recorrido de dictado, preparacion del mensaje, apertura de Telegram y recepcion real.

Este avance usa una clave compartida y un identificador de instalación persistente para una demo controlada. No implementa Firebase Authentication ni comprueba la identidad de una persona. Mantén esta configuración en una red local de confianza y evita distribuirla como servicio público o multiusuario.

El usuario de la app tiene su propio perfil en Firestore; no se vincula automáticamente al perfil que ya utilizaban en el bot. Crea el contacto desde esta app para probar el recorrido completo.

La implementación de este recorrido no activa el calendario, llamadas, pagos ni otras capacidades del mockup. Calendario sigue como siguiente integración. La auditoría completa y el kill switch siguen pendientes en el plan del proyecto.

Para ejecutar las pruebas del backend:

```powershell
.\.venv\Scripts\python.exe -m unittest discover -s tests -v
```
