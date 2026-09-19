AVAILABLE_TOOLS = [
    {
        "name": "echo",
        "description": "Repite el mensaje recibido. Agente de prueba, útil para confirmar que el sistema responde.",
    },
    {
        "name": "chat",
        "description": "Responde SOLO saludos, cortesías, o preguntas sobre qué puede hacer este asistente. No se usa para conocimiento general, programación, ni temas ajenos a gestionar tareas personales.",
    },

    {
        "name": "calendar_read",
        "description": "Consulta los próximos eventos del calendario del usuario. Úsalo cuando pregunten qué tienen agendado, su agenda, o próximos eventos.",
    },
    {
        "name": "calendar_create",
        "description": (
            "Crea un evento nuevo en el calendario. Requiere params: "
            "'summary' (título del evento), 'date' (formato YYYY-MM-DD), "
            "'time' (formato 24 horas HH:MM). Opcional: 'duration_minutes' (por defecto 60). "
            "Si el usuario da una fecha relativa como 'mañana' o 'el jueves', calculala usando la fecha de hoy que te doy en tus instrucciones."
        ),
    },

    {
        "name": "file_read",
        "description": "Busca y lee un archivo por nombre o palabra clave dentro de los documentos guardados del usuario. Requiere params: 'query' (nombre o palabra clave a buscar).",
    },


    {
        "name": "contact_add",
        "description": "Guarda un contacto nuevo para poder enviarle mensajes después. Requiere params: 'name' (cómo lo va a llamar el usuario) y 'username' (su usuario de Telegram, sin el @).",
    },
    {
        "name": "telegram_send",
        "description": "Genera un link para enviarle un mensaje a un contacto YA GUARDADO por Telegram. Requiere params: 'contact' (el nombre guardado) y 'message' (el texto). Si el contacto no está guardado, primero hay que usar 'contact_add'.",
    },


]