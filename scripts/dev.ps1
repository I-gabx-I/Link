param([string]$TunnelUrl)

$gatewayLocalUrl = "http://localhost:8000"
$webhookPath     = "/webhook/telegram"

function Cleanup {
    param([int]$TunnelPid)
    Write-Host ""
    Write-Host "Apagando todo..." -ForegroundColor Cyan
    docker compose down
    if ($TunnelPid) {
        Stop-Process -Id $TunnelPid -Force -ErrorAction SilentlyContinue
    }
    Write-Host "Todo detenido." -ForegroundColor Green
}

function Get-BotToken {
    if (-not (Test-Path ".env")) {
        Write-Host "No encuentro el archivo .env en este directorio." -ForegroundColor Red
        return $null
    }
    $line = Get-Content .env |
        Where-Object { $_ -match '^\s*TELEGRAM_BOT_TOKEN\s*=' } |
        Select-Object -First 1

    if (-not $line) {
        Write-Host "No encuentro TELEGRAM_BOT_TOKEN en .env" -ForegroundColor Red
        return $null
    }
    return $line.Split('=', 2)[1].Trim().Trim('"').Trim("'")
}

function Get-WebhookSecret {
    $line = Get-Content .env | Where-Object { $_ -match '^\s*TELEGRAM_WEBHOOK_SECRET\s*=' } | Select-Object -First 1
    if ($line) { return $line.Split('=', 2)[1].Trim().Trim('"').Trim("'") }
    return $null
}

# ---------------------------------------------------------------------
#  Compuerta de DNS publico.
#
#  El error "Failed to resolve host" de setWebhook lo produce el resolver
#  de TELEGRAM, no el nuestro: el hostname aleatorio del quick tunnel
#  tarda en publicarse globalmente. Si le preguntamos a Telegram antes de
#  tiempo, su resolver cachea el NXDOMAIN por el TTL del SOA (30-60s) y
#  los reintentos inmediatos fallan aunque el dominio ya exista.
#
#  Por eso preguntamos primero a resolvers publicos (1.1.1.1 / 8.8.8.8),
#  que es lo mas parecido a lo que hara Telegram, y solo despues
#  registramos el webhook.
# ---------------------------------------------------------------------
function Wait-PublicDns {
    param([string]$Url, [int]$TimeoutSec = 180)

    # OJO: $host es una variable de solo lectura en PowerShell.
    $hostName  = ([uri]$Url).Host
    $resolvers = @("1.1.1.1", "8.8.8.8")
    $deadline  = (Get-Date).AddSeconds($TimeoutSec)

    Write-Host "Esperando a que $hostName resuelva en DNS publico..." -ForegroundColor Cyan

    while ((Get-Date) -lt $deadline) {
        $allOk = $true
        foreach ($r in $resolvers) {
            try {
                $ans = Resolve-DnsName -Name $hostName -Server $r -Type A -DnsOnly -ErrorAction Stop
                if (-not ($ans | Where-Object { $_.QueryType -eq 'A' })) { $allOk = $false }
            } catch { $allOk = $false }
            if (-not $allOk) { break }
        }
        if ($allOk) {
            Write-Host "  DNS publico OK." -ForegroundColor Green
            return $true
        }
        Start-Sleep -Seconds 5
    }

    Write-Host "  El dominio no resolvio en DNS publico tras ${TimeoutSec}s." -ForegroundColor Yellow
    return $false
}

function Test-PublicUrl {
    param([string]$Url)
    try {
        Invoke-WebRequest -Uri $Url -TimeoutSec 10 -UseBasicParsing -ErrorAction Stop | Out-Null
        return $true
    } catch {
        # Cualquier status que venga del origen (404, 405, 401...) significa
        # que el tunel ya esta pasando trafico. Solo 502/503/504 y los fallos
        # sin respuesta indican que el edge todavia no sirve el hostname.
        $code = $null
        if ($_.Exception.Response) {
            try { $code = [int]$_.Exception.Response.StatusCode } catch { }
        }
        if ($code -and $code -notin @(502, 503, 504)) { return $true }
        return $false
    }
}

function Set-TelegramWebhook {
    param([string]$Url)

    $token = Get-BotToken
    if (-not $token) { return $false }

    $secret = Get-WebhookSecret
    $target = "$Url$webhookPath"
    $body = @{ url = $target }
    if ($secret) { $body["secret_token"] = $secret }

    $registered = $false
    for ($attempt = 1; $attempt -le 4; $attempt++) {
        try {
            $result = Invoke-RestMethod `
                -Uri "https://api.telegram.org/bot$token/setWebhook" `
                -Method Post `
                -Body $body `
                -TimeoutSec 15 `
                -ErrorAction Stop

            Write-Host "Telegram acepto el registro: $($result.description)" -ForegroundColor Cyan
            $registered = $true
            break
        } catch {
            $msg = $_.ErrorDetails.Message
            if ($msg -match "Failed to resolve host") {
                # 20s y no 8s: hay que salirse del TTL negativo del resolver
                # de Telegram, no reconfirmarlo.
                Write-Host "  Telegram aun no resuelve el dominio (intento $attempt de 4). Esperando 20s..." -ForegroundColor Yellow
                Start-Sleep -Seconds 20
                continue
            }
            Write-Host "Fallo registrando el webhook: $($_.Exception.Message)" -ForegroundColor Red
            if ($msg) { Write-Host "  Detalle: $msg" -ForegroundColor Red }
            return $false
        }
    }

    if (-not $registered) {
        Write-Host "Telegram no logro resolver el dominio tras varios intentos." -ForegroundColor Red
        return $false
    }

    Start-Sleep -Seconds 2
    try {
        $info = Invoke-RestMethod `
            -Uri "https://api.telegram.org/bot$token/getWebhookInfo" `
            -TimeoutSec 15 -ErrorAction Stop

        Write-Host "  URL registrada : $($info.result.url)" -ForegroundColor DarkGray
        Write-Host "  Pendientes     : $($info.result.pending_update_count)" -ForegroundColor DarkGray

        if ($info.result.last_error_message) {
            Write-Host "  Telegram reporta error: $($info.result.last_error_message)" -ForegroundColor Red
        } else {
            Write-Host "Webhook activo, sin errores reportados." -ForegroundColor Green
        }
    } catch {
        Write-Host "  No pude consultar getWebhookInfo: $($_.Exception.Message)" -ForegroundColor Yellow
    }

    return $true
}

Write-Host "Verificando Docker..." -ForegroundColor Cyan
docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker Desktop no esta corriendo. Abrelo y espera a 'Engine running'." -ForegroundColor Red
    exit 1
}

Write-Host "Levantando contenedores..." -ForegroundColor Cyan
docker compose up -d --build

Write-Host "Confirmando que el gateway responde localmente..." -ForegroundColor Cyan
$localOk = $false
for ($i = 1; $i -le 10; $i++) {
    try {
        Invoke-RestMethod -Uri "$gatewayLocalUrl/" -TimeoutSec 3 -ErrorAction Stop | Out-Null
        $localOk = $true
        break
    } catch { Start-Sleep -Seconds 2 }
}
if (-not $localOk) {
    Write-Host "El gateway no responde en $gatewayLocalUrl. Revisa 'docker compose logs gateway'." -ForegroundColor Red
    exit 1
}
Write-Host "Gateway local respondiendo bien." -ForegroundColor Green

$tunnelProc = $null
$finalUrl = $null

if ($TunnelUrl) {
    Write-Host "Usando tunel manual ya existente: $TunnelUrl" -ForegroundColor Cyan
    $finalUrl = $TunnelUrl.TrimEnd('/')
} else {
    $tunnelLogFile = "$env:TEMP\cloudflared_link.log"
    $maxAttempts = 3

    for ($attempt = 1; $attempt -le $maxAttempts; $attempt++) {
        Write-Host ""
        Write-Host "=== Intento de tunel $attempt de $maxAttempts ===" -ForegroundColor Cyan
        Remove-Item $tunnelLogFile -ErrorAction SilentlyContinue

        $tunnelProc = Start-Process -FilePath "cloudflared" -PassThru -ArgumentList @(
            "tunnel", "--url", $gatewayLocalUrl,
            "--metrics", "127.0.0.1:20241",
            "--logfile", $tunnelLogFile, "--loglevel", "info"
        )
        Write-Host "Ventana de cloudflared abierta (PID $($tunnelProc.Id)). Esperando /ready..." -ForegroundColor Cyan

        $ready = $false
        for ($i = 1; $i -le 10; $i++) {
            try {
                $status = Invoke-RestMethod -Uri "http://127.0.0.1:20241/ready" -TimeoutSec 3 -ErrorAction Stop
                if ($status.readyConnections -gt 0) { $ready = $true; break }
            } catch {}
            Start-Sleep -Seconds 3
        }

        if (-not $ready) {
            Write-Host "No reporto listo en ~30s. Cerrando este intento." -ForegroundColor Yellow
            Stop-Process -Id $tunnelProc.Id -Force -ErrorAction SilentlyContinue
            $tunnelProc = $null
            if ($attempt -lt $maxAttempts) {
                Write-Host "Esperando 25s antes de reintentar (sin rafaga)..." -ForegroundColor DarkGray
                Start-Sleep -Seconds 25
            }
            continue
        }

        Write-Host "Tunel listo (edge confirmado via /ready)." -ForegroundColor Green
        $content = Get-Content $tunnelLogFile -Raw
        $candidate = $null
        if ($content -match "https://[a-z0-9\-]+\.trycloudflare\.com") { $candidate = $matches[0] }

        if (-not $candidate) {
            Write-Host "El tunel conecto pero no encontre la URL en el log." -ForegroundColor Yellow
            Stop-Process -Id $tunnelProc.Id -Force -ErrorAction SilentlyContinue
            $tunnelProc = $null
            continue
        }

        Write-Host "URL del tunel: $candidate" -ForegroundColor Green

        # --- Compuerta: no le preguntamos a Telegram hasta que el mundo
        #     de afuera pueda resolver y servir este hostname. ---
        if (-not (Wait-PublicDns -Url $candidate -TimeoutSec 180)) {
            Write-Host "Descartando este tunel: nunca se publico en DNS." -ForegroundColor Yellow
            Stop-Process -Id $tunnelProc.Id -Force -ErrorAction SilentlyContinue
            $tunnelProc = $null
            if ($attempt -lt $maxAttempts) {
                Write-Host "Esperando 25s antes de reintentar con otro hostname..." -ForegroundColor DarkGray
                Start-Sleep -Seconds 25
            }
            continue
        }

        Write-Host "Confirmando que la URL publica responde..." -ForegroundColor Cyan
        $served = $false
        for ($i = 1; $i -le 10; $i++) {
            if (Test-PublicUrl $candidate) { $served = $true; break }
            Start-Sleep -Seconds 5
        }
        if (-not $served) {
            Write-Host "El hostname resuelve pero el edge no sirve trafico todavia." -ForegroundColor Yellow
            Stop-Process -Id $tunnelProc.Id -Force -ErrorAction SilentlyContinue
            $tunnelProc = $null
            if ($attempt -lt $maxAttempts) { Start-Sleep -Seconds 25 }
            continue
        }

        Write-Host "URL publica sirviendo trafico." -ForegroundColor Green
        $finalUrl = $candidate
        break
    }

    if (-not $finalUrl) {
        Write-Host ""
        Write-Host "Ningun tunel quedo utilizable tras $maxAttempts intentos. Docker sigue arriba." -ForegroundColor Red
        Write-Host "Opcion 1: corre 'cloudflared tunnel --url $gatewayLocalUrl' a mano, luego:" -ForegroundColor Yellow
        Write-Host "  .\scripts\dev.ps1 -TunnelUrl https://esa-url.trycloudflare.com" -ForegroundColor Yellow
        Write-Host "Opcion 2: espera unos minutos y corre este script de nuevo tal cual." -ForegroundColor Yellow
        exit 1
    }
}

Write-Host "Registrando webhook..." -ForegroundColor Cyan
Set-TelegramWebhook -Url $finalUrl | Out-Null

Write-Host ""
Write-Host "=====================================================" -ForegroundColor Green
Write-Host " Todo arriba." -ForegroundColor Green
Write-Host " URL publica: $finalUrl" -ForegroundColor Green
if ($tunnelProc) {
    Write-Host " La ventana de cloudflared quedo abierta aparte (PID $($tunnelProc.Id))." -ForegroundColor Green
}
Write-Host " [r] reconstruir tras un cambio de codigo" -ForegroundColor Green
Write-Host " [q] o Ctrl+C para apagar todo y salir" -ForegroundColor Green
Write-Host "=====================================================" -ForegroundColor Green
Write-Host ""

try {
    $dockerLogFile = "$env:TEMP\docker_link.log"
    Remove-Item $dockerLogFile -ErrorAction SilentlyContinue
    Start-Process -FilePath "cmd.exe" -ArgumentList "/c docker compose logs -f > `"$dockerLogFile`" 2>&1" -WindowStyle Hidden

    $lastLen = 0
    while ($true) {
        if (Test-Path $dockerLogFile) {
            $content = Get-Content $dockerLogFile -Raw
            if ($content -and $content.Length -gt $lastLen) {
                Write-Host -NoNewline $content.Substring($lastLen)
                $lastLen = $content.Length
            }
        }
        if ([Console]::KeyAvailable) {
            $key = [Console]::ReadKey($true)
            if ($key.KeyChar -eq 'r') {
                Write-Host "`n=== Reconstruyendo... ===" -ForegroundColor Cyan
                docker compose up -d --build
            } elseif ($key.KeyChar -eq 'q') { break }
        }
        Start-Sleep -Milliseconds 300
    }
} finally {
    if ($tunnelProc) { Cleanup -TunnelPid $tunnelProc.Id } else { Cleanup }
}