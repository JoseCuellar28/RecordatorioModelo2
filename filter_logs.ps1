# Script para filtrar logs de TaskRepository y SyncManager
$env:ANDROID_HOME = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk"
$adbPath = "$env:ANDROID_HOME\platform-tools\adb.exe"

if (Test-Path $adbPath) {
    Write-Host "🔍 Iniciando filtro de logs para TaskRepository y SyncManager..."
    Write-Host "📱 Presiona Ctrl+C para detener"
    Write-Host "=" * 60
    
    # Filtrar solo logs de TaskRepository y SyncManager
    & $adbPath logcat -s "TaskRepository:D" "SyncManager:D"
} else {
    Write-Host "❌ ADB no encontrado en: $adbPath"
    Write-Host "💡 Verifica que Android SDK esté instalado correctamente"
}