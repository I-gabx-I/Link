import os
from datetime import datetime, timedelta, timezone

from fastapi import FastAPI
from pydantic import BaseModel
from google.oauth2 import service_account
from googleapiclient.discovery import build

from capabilities import CAPABILITIES

CREDENTIALS_PATH = os.getenv("GOOGLE_CALENDAR_CREDENTIALS_PATH")
CALENDAR_ID = os.getenv("GOOGLE_CALENDAR_ID", "primary")
SCOPES = ["https://www.googleapis.com/auth/calendar.events"]

app = FastAPI(title="CalendarAgent")


class ExecuteRequest(BaseModel):
    tool: str
    params: dict


def _get_service():
    creds = service_account.Credentials.from_service_account_file(CREDENTIALS_PATH, scopes=SCOPES)
    return build("calendar", "v3", credentials=creds)


def _list_upcoming_events(max_results: int = 5) -> str:
    service = _get_service()
    now = datetime.now(timezone.utc).isoformat()
    events_result = service.events().list(
        calendarId=CALENDAR_ID,
        timeMin=now,
        maxResults=max_results,
        singleEvents=True,
        orderBy="startTime",
    ).execute()
    events = events_result.get("items", [])

    if not events:
        return "No tienes eventos próximos en tu calendario."

    lines = []
    for event in events:
        start = event["start"].get("dateTime", event["start"].get("date"))
        lines.append(f"- {event.get('summary', '(sin título)')}: {start}")
    return "Tus próximos eventos:\n" + "\n".join(lines)


def _create_event(summary: str, date: str, time: str, duration_minutes: int = 60) -> str:
    service = _get_service()
    start_str = f"{date}T{time}:00"
    start_dt = datetime.strptime(start_str, "%Y-%m-%dT%H:%M:%S")
    end_dt = start_dt + timedelta(minutes=duration_minutes)

    event = {
        "summary": summary,
        "start": {"dateTime": start_dt.strftime("%Y-%m-%dT%H:%M:%S"), "timeZone": "America/Guatemala"},
        "end": {"dateTime": end_dt.strftime("%Y-%m-%dT%H:%M:%S"), "timeZone": "America/Guatemala"},
    }
    created = service.events().insert(calendarId=CALENDAR_ID, body=event).execute()
    return f"Evento creado: '{summary}' el {date} a las {time}."


@app.get("/")
def health_check():
    return {"status": "agent_calendar alive", "capabilities": list(CAPABILITIES.keys())}


@app.post("/execute")
def execute(req: ExecuteRequest):
    try:
        if req.tool == "calendar_read":
            return {"result": _list_upcoming_events()}
        if req.tool == "calendar_create":
            p = req.params
            return {"result": _create_event(p["summary"], p["date"], p["time"], p.get("duration_minutes", 60))}
        return {"error": f"capability '{req.tool}' not declared"}
    except Exception as e:
        return {"error": f"No pude completar la acción de calendario: {e}"}