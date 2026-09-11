# iniciar-demo.ps1 — Levanta la demo pública de LUMURA conectando:
#   servidor Spring Boot (java -jar) + túnel público temporal (cloudflared) + SMTP real (Gmail).
#
# Las credenciales SMTP se piden en el prompt como SecureString (o se toman de las variables
# de entorno del proceso) y SOLO viven en la sesión: no se guardan en ningún archivo.
#
# Uso:
#   ./scripts/iniciar-demo.ps1
#   ./scripts/iniciar-demo.ps1 -SkipTunnel      (usar una APP_BASE_URL ya existente o localhost)
[CmdletBinding()]
param(
    [string]$JarPath = "$PSScriptRoot\..\target\primeraApi-0.0.1-SNAPSHOT.jar",
    [string]$WorkingDir = "$PSScriptRoot\..",
    [string]$Cloudflared = "C:\Program Files (x86)\cloudflared\cloudflared.exe",
    [string]$TempDir = "$env:TEMP\opencode\lumura_demo",
    [string]$SmtpUser = $env:SMTP_USER,
    [string]$MailFrom = $env:MAIL_FROM,
    [switch]$SkipTunnel
)

$ErrorActionPreference = 'Stop'

$JarPath = (Resolve-Path $JarPath -ErrorAction SilentlyContinue).Path
if (-not $JarPath) { throw "No se encontró el jar en: $JarPath" }
New-Item -ItemType Directory -Force -Path $TempDir | Out-Null

# ---- 1) Credenciales SMTP (solo runtime) ----
if (-not $SmtpUser) {
    $SmtpUser = Read-Host "Dirección Gmail remitente (SMTP_USER/MAIL_FROM)"
}
if (-not $env:SMTP_PASSWORD) {
    $sec = Read-Host -AsSecureString "Clave de aplicación Gmail (no se mostrará en pantalla)"
    $bstr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec)
    try { $env:SMTP_PASSWORD = [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr) }
    finally { [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr) }
}
if (-not $MailFrom) { $MailFrom = $SmtpUser }

# ---- 2) Túnel público temporal ----
$tun = $null
$publicUrl = $null
if (-not $SkipTunnel) {
    Write-Host "Levantando túnel público de cloudflared..." -ForegroundColor Cyan
    $tunOut = Join-Path $TempDir "tunnel.out.log"
    $tunErr = Join-Path $TempDir "tunnel.err.log"
    Remove-Item $tunOut, $tunErr -ErrorAction SilentlyContinue
    $tun = Start-Process -FilePath $Cloudflared `
        -ArgumentList @("tunnel", "--url", "http://localhost:8080", "--protocol", "http2", "--no-autoupdate") `
        -RedirectStandardOutput $tunOut -RedirectStandardError $tunErr -WindowStyle Hidden -PassThru
    $deadline = (Get-Date).AddSeconds(60)
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Milliseconds 800
        $coincidencia = Select-String -Path $tunOut, $tunErr -Pattern "https://[a-z0-9\-]+\.trycloudflare\.com" -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($coincidencia -and $coincidencia.Matches.Count -gt 0) { $publicUrl = $coincidencia.Matches[0].Value; break }
        if ($tun.HasExited) { break }
    }
    if (-not $publicUrl) {
        Get-Content $tunErr -Tail 20 -ErrorAction SilentlyContinue
        throw "No se pudo obtener la URL pública del túnel. Revisa $tunErr"
    }
    Write-Host "URL pública obtenida: $publicUrl" -ForegroundColor Green
} else {
    $publicUrl = Read-Host "APP_BASE_URL pública (Enter para http://localhost:8080)"
    if ([string]::IsNullOrWhiteSpace($publicUrl)) { $publicUrl = "http://localhost:8080" }
}

# ---- 3) Precheck puerto 8080 ----
$ocupado = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
if ($ocupado) {
    Write-Host "AVISO: hay un proceso escuchando en 8080 (PID $($ocupado.OwningProcess))." -ForegroundColor Yellow
    Write-Host "Detenlo antes de continuar o el servidor nuevo no podrá arrancar." -ForegroundColor Yellow
}

# ---- 4) Variables de entorno (runtime, no persisten en disco) ----
$env:MAIL_ENABLED = "true"
$env:SMTP_HOST = "smtp.gmail.com"
$env:SMTP_PORT = "587"
$env:SMTP_USER = $SmtpUser
$env:MAIL_FROM = $MailFrom
$env:APP_BASE_URL = $publicUrl

# ---- 5) Servidor ----
$outLog = Join-Path $TempDir "server.out.log"
$errLog = Join-Path $TempDir "server.err.log"
Remove-Item $outLog, $errLog -ErrorAction SilentlyContinue
Write-Host "Arrancando servidor LUMURA (java -jar)..." -ForegroundColor Cyan
$proc = Start-Process -FilePath "java" `
    -ArgumentList @("-jar", "`"$JarPath`"") `
    -WorkingDirectory (Resolve-Path $WorkingDir) `
    -RedirectStandardOutput $outLog -RedirectStandardError $errLog -WindowStyle Hidden -PassThru

$deadline = (Get-Date).AddSeconds(90)
$ok = $false
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Milliseconds 1500
    if (Select-String -Path $outLog -Pattern "Started PrimeraApiApplication" -Quiet -ErrorAction SilentlyContinue) { $ok = $true; break }
    if ($proc.HasExited) { break }
}
if (-not $ok) {
    Get-Content $errLog -Tail 20 -ErrorAction SilentlyContinue
    throw "El servidor no arrancó en 90s. Revisa $errLog"
}

Write-Host ""
Write-Host "================ DEMO LUMURA lista ================" -ForegroundColor Green
Write-Host "Local   : http://localhost:8080"
Write-Host "Pública : $publicUrl"
Write-Host "PID servidor  : $($proc.Id)"
if ($tun) { Write-Host "PID cloudflared: $($tun.Id)" }
Write-Host "Logs en: $TempDir"
Write-Host "La URL pública deja de existir al cerrar cloudflared."
Write-Host "====================================================" -ForegroundColor Green