from fastapi import FastAPI
from pydantic import BaseModel
from capabilities import CAPABILITIES

app = FastAPI(title="EchoAgent")

class ExecuteRequest(BaseModel):
    tool: str
    params: dict

@app.get("/")
def health_check():
    return {"status": "agent_echo alive", "capabilities": list(CAPABILITIES.keys())}

@app.post("/execute")
def execute(req: ExecuteRequest):
    if req.tool == "echo":
        return {"result": req.params.get("message", "")}
    return {"error": f"capability '{req.tool}' not declared"}