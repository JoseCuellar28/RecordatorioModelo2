# Configuración de Firebase para RecordatorioModelo2

## Información del Proyecto
- **Package Name:** `com.example.recordatoriomodelo2`
- **SHA-1 Debug:** `E0:30:4E:B2:9A:8B:1D:52:DD:68:F0:F0:92:94:96:CF:EA:3B:22:1E`

## Pasos para configurar Firebase

### 1. Crear Proyecto en Firebase Console
1. Ve a [Firebase Console](https://console.firebase.google.com/)
2. Haz clic en "Agregar proyecto"
3. Nombre del proyecto: `RecordatorioModelo2`
4. Habilita Google Analytics (opcional)
5. Acepta los términos y crea el proyecto

### 2. Agregar Aplicación Android
1. En el proyecto de Firebase, haz clic en el ícono de Android
2. Registra la aplicación:
   - **Nombre del paquete de Android:** `com.example.recordatoriomodelo2`
   - **Alias de la aplicación:** `RecordatorioModelo2`
   - **Certificado de firma de depuración SHA-1:** `E0:30:4E:B2:9A:8B:1D:52:DD:68:F0:F0:92:94:96:CF:EA:3B:22:1E`

### 3. Descargar google-services.json
1. Descarga el archivo `google-services.json`
2. Reemplaza el archivo actual en `app/google-services.json`

### 4. Habilitar Authentication
1. En Firebase Console, ve a "Authentication"
2. Haz clic en "Comenzar"
3. En la pestaña "Sign-in method", habilita:
   - **Email/Password**
   - **Google** (opcional, para login con Google)

### 5. Configurar Firestore Database
1. En Firebase Console, ve a "Firestore Database"
2. Haz clic en "Crear base de datos"
3. Selecciona "Comenzar en modo de prueba"
4. Elige la ubicación más cercana

## Servicios a Habilitar
- ✅ Authentication (Email/Password)
- ✅ Firestore Database
- ⚠️ Cloud Functions (para envío de emails temporales)

## Notas Importantes
- El archivo `google-services.json` actual es genérico y debe ser reemplazado
- Asegúrate de que el SHA-1 coincida con el generado por tu keystore
- Para producción, necesitarás generar un SHA-1 de release

## Estado Actual
- ❌ Proyecto Firebase creado
- ❌ google-services.json actualizado
- ✅ Dependencias agregadas al build.gradle
- ❌ Authentication habilitado
- ❌ Firestore configurado