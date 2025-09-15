# 🔑 Configuración de Google Services - RecordatorioModelo2

## 📋 Resumen
Este documento explica paso a paso cómo configurar los servicios de Google necesarios para que la aplicación funcione correctamente.

## 🎯 Servicios Requeridos

1. **Google Sign-In API** - Para autenticación de usuarios
2. **Google Classroom API** - Para importar cursos y tareas
3. **Firebase** - Para configuración de proyecto (opcional)

## 🚀 Paso 1: Crear Proyecto en Google Cloud Console

### 1.1 Acceder a Google Cloud Console
1. Ir a [https://console.cloud.google.com/](https://console.cloud.google.com/)
2. Iniciar sesión con tu cuenta de Google
3. Crear un nuevo proyecto o seleccionar uno existente

### 1.2 Configurar el Proyecto
1. **Nombre del proyecto**: `RecordatorioModelo2` (o el que prefieras)
2. **ID del proyecto**: Se genera automáticamente
3. **Ubicación**: Seleccionar la más cercana

## 🔧 Paso 2: Habilitar APIs

### 2.1 Google Sign-In API
1. En Google Cloud Console, ir a **"APIs & Services" > "Library"**
2. Buscar "Google Sign-In API"
3. Hacer clic en "Enable"

### 2.2 Google Classroom API
1. En la misma biblioteca, buscar "Google Classroom API"
2. Hacer clic en "Enable"
3. **Importante**: Esta API requiere verificación si planeas publicar la app

## 🔐 Paso 3: Configurar Credenciales OAuth 2.0

### 3.1 Crear Credenciales
1. Ir a **"APIs & Services" > "Credentials"**
2. Hacer clic en **"Create Credentials" > "OAuth 2.0 Client IDs"**

### 3.2 Configurar OAuth Consent Screen
1. **User Type**: External (para desarrollo)
2. **App name**: RecordatorioModelo2
3. **User support email**: Tu email
4. **Developer contact information**: Tu email
5. **Scopes**: Agregar los siguientes:
   - `https://www.googleapis.com/auth/userinfo.email`
   - `https://www.googleapis.com/auth/userinfo.profile`
   - `https://www.googleapis.com/auth/classroom.courses.readonly`
   - `https://www.googleapis.com/auth/classroom.coursework.me.readonly`
   - `https://www.googleapis.com/auth/classroom.coursework.students.readonly`

### 3.3 Crear OAuth 2.0 Client ID
1. **Application type**: Android
2. **Package name**: `com.example.recordatoriomodelo2`
3. **SHA-1 certificate fingerprint**: Obtener con el siguiente comando

### 3.4 Obtener SHA-1 Fingerprint

#### Para Windows:
```cmd
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

#### Para macOS/Linux:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

#### Para Release (cuando tengas tu keystore):
```bash
keytool -list -v -keystore [ruta-a-tu-keystore] -alias [tu-alias] -storepass [tu-password] -keypass [tu-password]
```

### 3.5 Descargar Credenciales
1. Después de crear el OAuth 2.0 Client ID, descargar el archivo JSON
2. Renombrar a `client_secret_[tu-proyecto-id].json`
3. Colocar en la carpeta `app/` del proyecto

## 🔥 Paso 4: Configurar Firebase (Opcional)

### 4.1 Crear Proyecto Firebase
1. Ir a [https://console.firebase.google.com/](https://console.firebase.google.com/)
2. Crear proyecto con el mismo nombre que en Google Cloud
3. **Importante**: Usar el mismo proyecto de Google Cloud

### 4.2 Agregar Aplicación Android
1. En Firebase Console, hacer clic en **"Add app" > "Android"**
2. **Package name**: `com.example.recordatoriomodelo2`
3. **App nickname**: RecordatorioModelo2
4. **Debug signing certificate SHA-1**: Usar el mismo SHA-1 de antes

### 4.3 Descargar google-services.json
1. Descargar el archivo `google-services.json`
2. Colocar en la carpeta `app/` del proyecto

## 📁 Estructura de Archivos

Después de la configuración, tu proyecto debe tener esta estructura:

```
Android/
├── app/
│   ├── google-services.json                    # Firebase config
│   ├── client_secret_[proyecto-id].json       # OAuth credentials
│   └── ...
├── README.md
├── REQUERIMIENTOS.md
├── CONFIGURACION_GOOGLE.md
└── RESUMEN_PROYECTO.txt
```

## ⚠️ Problemas Comunes

### Error: "Google Sign-In failed"
- Verificar que el SHA-1 fingerprint coincida exactamente
- Confirmar que las APIs estén habilitadas
- Verificar que el package name sea correcto

### Error: "Classroom API not enabled"
- Ir a Google Cloud Console
- Habilitar Google Classroom API
- Esperar unos minutos para que se active

### Error: "OAuth consent screen not configured"
- Completar la configuración del OAuth consent screen
- Agregar todos los scopes necesarios
- Guardar los cambios

### Error: "Invalid client"
- Verificar que el archivo `client_secret_*.json` esté en `app/`
- Confirmar que el nombre del archivo sea correcto
- Verificar que el contenido del archivo sea válido

## 🔍 Verificación

### 1. Verificar APIs Habilitadas
En Google Cloud Console > APIs & Services > Enabled APIs, debes ver:
- Google Sign-In API
- Google Classroom API

### 2. Verificar Credenciales
En Google Cloud Console > APIs & Services > Credentials, debes ver:
- OAuth 2.0 Client ID para Android
- Con el package name correcto
- Con el SHA-1 fingerprint correcto

### 3. Verificar Archivos
En tu proyecto Android:
- `app/google-services.json` existe
- `app/client_secret_*.json` existe
- Ambos archivos tienen contenido válido

## 📞 Soporte

Si encuentras problemas:

1. **Verificar logs**: Usar `adb logcat` para ver errores detallados
2. **Revisar configuración**: Confirmar todos los pasos anteriores
3. **Probar en emulador**: Asegurarte de que funciona en emulador antes de probar en dispositivo físico
4. **Verificar internet**: Confirmar que el dispositivo tiene conexión a internet

## 🔒 Notas de Seguridad

- **Nunca** subir los archivos de credenciales a repositorios públicos
- Usar `.gitignore` para excluir archivos sensibles
- Para producción, usar keystore de release, no debug
- Considerar usar Firebase App Check para mayor seguridad

---

**Importante**: Esta configuración es necesaria para que la aplicación funcione correctamente. Sin estos archivos, la autenticación con Google y la integración con Classroom no funcionarán. 