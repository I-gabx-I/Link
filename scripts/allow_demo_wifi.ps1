[CmdletBinding(SupportsShouldProcess = $true)]
param(
    [string]$ComputerAddress,
    [string]$PhoneAddress,
    [switch]$Remove
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$taskRuleName = 'LinkDemoAndroid8000'
$taskRuleGroup = 'Link local demo'

function Assert-Administrator {
    $taskIdentity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $taskPrincipal = [Security.Principal.WindowsPrincipal]::new($taskIdentity)
    if (-not $taskPrincipal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        throw 'Windows requiere PowerShell como administrador para modificar el firewall.'
    }
}

function Get-OwnedRule {
    $taskExisting = Get-NetFirewallRule -Name $taskRuleName -ErrorAction SilentlyContinue
    if ($taskExisting -and $taskExisting.Group -ne $taskRuleGroup) {
        throw 'Existe una regla con ese nombre que no pertenece a la demo. No se modifico.'
    }
    return $taskExisting
}

if ($Remove) {
    if ($PSCmdlet.ShouldProcess($taskRuleName, 'Retirar exclusivamente la regla temporal de Link')) {
        Assert-Administrator
        $taskExisting = Get-OwnedRule
        if ($taskExisting) { $taskExisting | Remove-NetFirewallRule }
        Write-Host 'Regla temporal de Link retirada.'
    }
    return
}

foreach ($taskAddress in @($ComputerAddress, $PhoneAddress)) {
    $taskParsed = $null
    if (-not [Net.IPAddress]::TryParse($taskAddress, [ref]$taskParsed) -or
        $taskParsed.AddressFamily -ne [Net.Sockets.AddressFamily]::InterNetwork -or
        $taskParsed.ToString() -ne $taskAddress) {
        throw 'Indica -ComputerAddress y -PhoneAddress con sus direcciones IPv4 completas.'
    }
}

$taskRepository = Split-Path -Parent $PSScriptRoot
$taskPython = Join-Path $taskRepository '.venv/Scripts/python.exe'
if (-not (Test-Path -LiteralPath $taskPython)) {
    throw 'No encuentro el entorno .venv del backend. Preparalo antes de abrir el puerto.'
}
$taskPythonProgram = (& $taskPython -c 'import sys; print(sys._base_executable)').Trim()
if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $taskPythonProgram)) {
    throw 'No pude determinar el ejecutable de Python que usa el backend.'
}

$taskTarget = "$PhoneAddress -> ${ComputerAddress}:8000/TCP; solo red privada; programa $taskPythonProgram"
if ($PSCmdlet.ShouldProcess($taskTarget, 'Permitir la conexion de este telefono al backend de Link')) {
    Assert-Administrator
    $taskInterface = Get-NetIPAddress -AddressFamily IPv4 -IPAddress $ComputerAddress
    $taskProfile = Get-NetConnectionProfile -InterfaceIndex $taskInterface.InterfaceIndex
    if ($taskProfile.NetworkCategory.ToString() -ne 'Private') {
        throw 'Esta regla solo se prepara para una conexion configurada como Private en Windows.'
    }
    $taskExisting = Get-OwnedRule
    if ($taskExisting) { $taskExisting | Remove-NetFirewallRule }
    New-NetFirewallRule -Name $taskRuleName -DisplayName 'Link demo - Android TCP 8000' `
        -Group $taskRuleGroup -Description 'Regla temporal creada por scripts/allow_demo_wifi.ps1.' `
        -Direction Inbound -Action Allow -Enabled True -Profile Private -Protocol TCP `
        -LocalAddress $ComputerAddress -LocalPort 8000 -RemoteAddress $PhoneAddress `
        -Program $taskPythonProgram | Out-Null
    Write-Host "Regla aplicada para $PhoneAddress y http://${ComputerAddress}:8000. Falta comprobar el acceso desde el telefono."
    Write-Host 'Para retirarla, ejecuta este mismo script como administrador con -Remove.'
}
