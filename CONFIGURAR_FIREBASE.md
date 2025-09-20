# 🔥 CONFIGURACIÓN URGENTE DE FIREBASE

## ⚠️ PROBLEMA ACTUAL
El error `SecurityException: Unknown calling package name 'com.google.android.gms'` indica que Firebase no reconoce tu aplicación.

## ✅ DATOS VERIFICADOS
- **Package Name:** `com.example.recordatoriomodelo2`
- **SHA-1 Debug:** `E0:30:4E:B2:9A:8B:1D:52:DD:68:F0:F0:92:94:96:CF:EA:3B:22:1E`

## 🚀 PASOS PARA RESOLVER (URGENTE)

### 1. Ir a Firebase Console
1. Ve a: https://console.firebase.google.com/
2. Si no tienes proyecto, crea uno nuevo llamado `RecordatorioModelo2`

### 2. Agregar Aplicación Android
1. En tu proyecto Firebase, haz clic en ⚙️ **Configuración del proyecto**
2. Baja hasta **Tus aplicaciones**
3. Haz clic en **Agregar aplicación** → **Android**
4. Completa EXACTAMENTE:
   ```
   Nombre del paquete de Android: com.example.recordatoriomodelo2
   Alias de la aplicación: RecordatorioModelo2
   Certificado SHA-1: E0:30:4E:B2:9A:8B:1D:52:DD:68:F0:F0:92:94:96:CF:EA:3B:22:1E
   ```

### 3. Descargar google-services.json
1. Descarga el nuevo archivo `google-services.json`
2. **IMPORTANTE:** Reemplaza el archivo en `app/google-services.json`

### 4. Habilitar Authentication
1. Ve a **Authentication** → **Sign-in method**
2. Habilita **Email/contraseña**

### 5. Configurar Firestore
1. Ve a **Firestore Database**
2. **Crear base de datos** → **Modo de prueba**
3. Selecciona ubicación: **us-central1**

## 🔧 DESPUÉS DE CONFIGURAR FIREBASE

Ejecuta estos comandos para probar:

```bash
# Limpiar y reconstruir
./gradlew clean
./gradlew build

# Instalar en emulador/dispositivo
./gradlew installDebug
```

## 📱 VERIFICAR EN EL EMULADOR

1. Abre la aplicación
2. Ve a la pantalla de registro
3. Intenta registrar un usuario
4. Revisa los logs en Android Studio para ver si aparecen los mensajes de Google Play Services

## ⚡ SI SIGUE EL ERROR

Si después de configurar Firebase correctamente sigue el error:

1. **Reinicia el emulador**
2. **Actualiza Google Play Services** en el emulador:
   - Settings → Apps → Google Play Services → Update
3. **Usa un emulador con Google APIs** (no solo Android)

## 📋 CHECKLIST
- [ ] Proyecto Firebase creado
- [ ] Aplicación Android agregada con SHA-1 correcto
- [ ] google-services.json descargado y reemplazado
- [ ] Authentication habilitado
- [ ] Firestore configurado
- [ ] Aplicación reconstruida e instalada