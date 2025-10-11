@echo off
echo Desplegando indices de Firestore...
echo.

REM Verificar si Firebase CLI esta instalado
firebase --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Firebase CLI no esta instalado.
    echo Instala Firebase CLI con: npm install -g firebase-tools
    pause
    exit /b 1
)

REM Verificar si el usuario esta autenticado
firebase projects:list >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: No estas autenticado en Firebase.
    echo Ejecuta: firebase login
    pause
    exit /b 1
)

REM Desplegar indices
echo Desplegando indices de Firestore...
firebase deploy --only firestore:indexes

if %errorlevel% equ 0 (
    echo.
    echo ✅ Indices desplegados exitosamente!
    echo.
    echo Los indices pueden tardar unos minutos en estar disponibles.
    echo Puedes verificar el estado en:
    echo https://console.firebase.google.com/project/recordatoriotareas-6a384/firestore/indexes
) else (
    echo.
    echo ❌ Error al desplegar indices.
    echo Verifica tu configuracion de Firebase.
)

echo.
pause