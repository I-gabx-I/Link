import os
from fastapi import FastAPI
from pydantic import BaseModel

from capabilities import CAPABILITIES

BASE_DIR = "/app/shared_files"
MAX_CHARS = 1500

app = FastAPI(title="FileAgent")


class ExecuteRequest(BaseModel):
    tool: str
    params: dict



def _read_file_content(full_path: str) -> str:
    lower = full_path.lower()
    if lower.endswith(".pdf"):
        from pypdf import PdfReader
        reader = PdfReader(full_path)
        return "\n".join((page.extract_text() or "") for page in reader.pages)
    if lower.endswith(".docx"):
        from docx import Document
        doc = Document(full_path)
        return "\n".join(p.text for p in doc.paragraphs)
    with open(full_path, "r", encoding="utf-8", errors="ignore") as f:
        return f.read()

    
def _search_and_read(query: str) -> str:
    query_lower = query.strip().lower()
    if not os.path.isdir(BASE_DIR):
        return "No hay carpeta de archivos configurada."

    matches = [
        f for f in os.listdir(BASE_DIR)
        if query_lower in f.lower() and os.path.isfile(os.path.join(BASE_DIR, f))
    ]

    if not matches:
        return f"No encontré ningún archivo que coincida con '{query}'."

    filename = matches[0]
    full_path = os.path.join(BASE_DIR, filename)

    try:
        content = _read_file_content(full_path)
    except Exception as e:
        return f"Encontré '{filename}' pero no pude leerlo: {e}"

    

    truncated = content[:MAX_CHARS]
    suffix = "\n[...contenido recortado...]" if len(content) > MAX_CHARS else ""
    extra = f"\n\n(También coinciden: {', '.join(matches[1:5])})" if len(matches) > 1 else ""

    return f"Encontré '{filename}':\n\n{truncated}{suffix}{extra}"


@app.get("/")
def health_check():
    return {"status": "agent_file alive", "capabilities": list(CAPABILITIES.keys())}


@app.post("/execute")
def execute(req: ExecuteRequest):
    if req.tool == "file_read":
        try:
            return {"result": _search_and_read(req.params.get("query", ""))}
        except Exception as e:
            return {"error": f"No pude buscar el archivo: {e}"}
    return {"error": f"capability '{req.tool}' not declared"}