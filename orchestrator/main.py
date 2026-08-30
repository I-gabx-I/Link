from fastapi import FastAPI

app = FastAPI(title="Orchestrator")

@app.get("/")
def health_check():
    return {"status": "orchestrator alive"}