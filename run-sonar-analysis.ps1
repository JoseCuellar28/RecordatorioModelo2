# Script para ejecutar análisis de SonarQube
# Uso: .\run-sonar-analysis.ps1 -Token "tu-token-aqui"

param(
    [Parameter(Mandatory=$true)]
    [string]$Token,
    
    [Parameter(Mandatory=$false)]
    [string]$SonarUrl = "http://localhost:9000"
)

Write-Host "Iniciando análisis de SonarQube..." -ForegroundColor Green
Write-Host "URL de SonarQube: $SonarUrl" -ForegroundColor Yellow

# Verificar que SonarQube esté ejecutándose
try {
    $response = Invoke-WebRequest -Uri $SonarUrl -Method Head -TimeoutSec 10
    Write-Host "✓ SonarQube está ejecutándose" -ForegroundColor Green
}
catch {
    Write-Host "✗ Error: SonarQube no está ejecutándose en $SonarUrl" -ForegroundColor Red
    Write-Host "Asegúrate de que SonarQube esté iniciado antes de ejecutar el análisis" -ForegroundColor Yellow
    exit 1
}

# Limpiar builds anteriores
Write-Host "Limpiando builds anteriores..." -ForegroundColor Yellow
./gradlew clean

# Compilar el proyecto
Write-Host "Compilando el proyecto..." -ForegroundColor Yellow
./gradlew assembleDebug

# Ejecutar tests (opcional, pero recomendado para cobertura)
Write-Host "Ejecutando tests unitarios..." -ForegroundColor Yellow
./gradlew testDebugUnitTest

# Ejecutar análisis de SonarQube
Write-Host "Ejecutando análisis de SonarQube..." -ForegroundColor Yellow
$env:SONAR_TOKEN = $Token
./gradlew sonar -Dsonar.host.url=$SonarUrl -Dsonar.token=$Token

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Análisis de SonarQube completado exitosamente!" -ForegroundColor Green
    Write-Host "Puedes ver los resultados en: $SonarUrl/dashboard?id=recordatorio-modelo2" -ForegroundColor Cyan
}
else {
    Write-Host "✗ Error durante el análisis de SonarQube" -ForegroundColor Red
    exit 1
}