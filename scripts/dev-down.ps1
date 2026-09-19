Write-Host "Deteniendo contenedores..." -ForegroundColor Cyan
docker compose down

Write-Host "Deteniendo el túnel..." -ForegroundColor Cyan
Get-Process cloudflared -ErrorAction SilentlyContinue | Stop-Process -Force

Write-Host "Todo detenido." -ForegroundColor Green