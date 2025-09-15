# 📱 Requerimientos para Ejecutar RecordatorioModelo2

## 🛠️ Requerimientos del Sistema

### Sistema Operativo
- **Windows 10/11** (recomendado)
- **macOS 10.14+** 
- **Linux Ubuntu 18.04+**

### Hardware Mínimo
- **RAM**: 8 GB (16 GB recomendado)
- **Almacenamiento**: 10 GB de espacio libre
- **Procesador**: Intel i5 o AMD equivalente

## 🚀 Entorno de Desarrollo

### Android Studio
- **Versión**: Android Studio Hedgehog | 2023.1.1 o superior
- **Descarga**: [https://developer.android.com/studio](https://developer.android.com/studio)
- **Configuración**: 
  - Instalar Android SDK
  - Configurar variables de entorno ANDROID_HOME y ANDROID_SDK_ROOT

### Java Development Kit (JDK)
- **Versión**: JDK 11 o superior
- **Descarga**: [https://adoptium.net/](https://adoptium.net/) (Eclipse Temurin recomendado)
- **Configuración**: Configurar JAVA_HOME en variables de entorno

### Gradle
- **Versión**: 8.0+ (incluido con Android Studio)
- **Configuración**: Automática con Android Studio

## 📋 Dependencias del Proyecto

### Versiones de Android
- **compileSdk**: 36 (Android 14)
- **minSdk**: 24 (Android 7.0)
- **targetSdk**: 36 (Android 14)

### Librerías Principales
```kotlin
// Jetpack Compose
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-tooling-preview")

// Navegación
implementation("androidx.navigation:navigation-compose")

// Base de datos
implementation("androidx.room:room-runtime")
implementation("androidx.room:room-ktx")
ksp("androidx.room:room-compiler")

// ViewModel y Lifecycle
implementation("androidx.lifecycle:lifecycle-viewmodel-compose")
implementation("androidx.lifecycle:lifecycle-runtime-ktx")

// Google Services
implementation("com.google.android.gms:play-services-auth:21.1.0")
implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
```

## 🔑 Configuración de Google Services

### 1. Google Cloud Console
1. Ir a [Google Cloud Console](https://console.cloud.google.com/)
2. Crear un nuevo proyecto o seleccionar uno existente
3. Habilitar las siguientes APIs:
   - Google Sign-In API
   - Google Classroom API

### 2. Configurar OAuth 2.0
1. En Google Cloud Console, ir a "Credentials"
2. Crear credenciales OAuth 2.0 Client ID
3. Configurar:
   - **Application type**: Android
   - **Package name**: com.example.recordatoriomodelo2
   - **SHA-1 certificate fingerprint**: Obtener con el comando:
     ```bash
     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
     ```

### 3. Archivos de Configuración
Colocar los siguientes archivos en `app/`:
- `google-services.json` (Firebase)
- `client_secret_*.json` (Google OAuth)

## 📱 Dispositivo/Emulador

### Opción 1: Emulador Android
1. En Android Studio, ir a "AVD Manager"
2. Crear nuevo dispositivo virtual:
   - **API Level**: 24 o superior
   - **RAM**: 2 GB mínimo
   - **Internal Storage**: 4 GB mínimo

### Opción 2: Dispositivo Físico
1. Habilitar "Opciones de desarrollador" en el dispositivo
2. Activar "Depuración USB"
3. Conectar dispositivo vía USB
4. Instalar drivers si es necesario

## 🚀 Pasos para Ejecutar

### 1. Clonar/Descargar el Proyecto
```bash
git clone [URL_DEL_REPOSITORIO]
cd Android
```

### 2. Configurar Google Services
1. Copiar `google-services.json` a `app/`
2. Copiar `client_secret_*.json` a `app/`

### 3. Sincronizar Gradle
1. Abrir Android Studio
2. Abrir el proyecto
3. Esperar a que Gradle sincronice (puede tomar varios minutos)

### 4. Ejecutar la Aplicación
1. Seleccionar dispositivo/emulador
2. Presionar "Run" (▶️) o `Shift + F10`
3. Esperar a que se compile e instale

## ⚠️ Problemas Comunes y Soluciones

### Error de Gradle Sync
```bash
# Limpiar proyecto
./gradlew clean

# Invalidar caches en Android Studio
File > Invalidate Caches and Restart
```

### Error de Google Services
- Verificar que `google-services.json` esté en `app/`
- Confirmar que el SHA-1 fingerprint coincida
- Verificar que las APIs estén habilitadas en Google Cloud Console

### Error de Permisos
```xml
<!-- Verificar que estén en AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### Error de Compilación
- Verificar versión de JDK (debe ser 11+)
- Verificar versión de Android Studio
- Limpiar y rebuild el proyecto

## 🔧 Configuración Adicional

### Variables de Entorno (Opcional)
```bash
# Windows
set ANDROID_HOME=C:\Users\[USER]\AppData\Local\Android\Sdk
set ANDROID_SDK_ROOT=C:\Users\[USER]\AppData\Local\Android\Sdk

# macOS/Linux
export ANDROID_HOME=$HOME/Library/Android/sdk
export ANDROID_SDK_ROOT=$HOME/Library/Android/sdk
```

### Configuración de ProGuard (Para Release)
```proguard
# app/proguard-rules.pro
-keep class com.example.recordatoriomodelo2.** { *; }
-keep class com.google.android.gms.** { *; }
```

## 📞 Soporte

### Credenciales de Prueba
- **Usuario local**: admin
- **Contraseña**: 1234

### Logs de Debug
```bash
# Ver logs en tiempo real
adb logcat | grep "RecordatorioModelo2"
```

### Contacto
Si encuentras problemas:
1. Revisar logs de Android Studio
2. Verificar configuración de Google Services
3. Confirmar que todas las dependencias estén actualizadas

---

**Nota**: Este proyecto requiere conexión a internet para funcionar correctamente, especialmente para la integración con Google Classroom. 