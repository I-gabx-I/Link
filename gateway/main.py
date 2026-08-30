from fastapi import FastAPI

app = FastAPI(title="Gateway")

@app.get("/")
def health_check():
    return {"status": "gateway alive"}