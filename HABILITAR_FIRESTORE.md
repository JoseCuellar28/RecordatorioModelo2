# 🔥 HABILITAR FIRESTORE API - URGENTE

## ❌ PROBLEMA ACTUAL
```
Cloud Firestore API has not been used in project recordatoriotareas-6a384 
before or it is disabled
```

## ✅ SOLUCIÓN PASO A PASO

### 1. **Abrir Firebase Console**
- Ve a: https://console.firebase.google.com/
- Selecciona proyecto: `recordatoriotareas-6a384`

### 2. **Habilitar Firestore**
- En el menú izquierdo, busca **"Firestore Database"**
- Haz clic en **"Crear base de datos"**
- Selecciona **"Comenzar en modo de prueba"** (para desarrollo)
- Elige una ubicación (recomendado: `us-central1`)

### 3. **Habilitar Authentication**
- Ve a **"Authentication"** en el menú izquierdo
- Haz clic en **"Comenzar"**
- En la pestaña **"Sign-in method"**
- Habilita **"Correo electrónico/contraseña"**

### 4. **Verificar APIs en Google Cloud**
- Ve a: https://console.developers.google.com/apis/api/firestore.googleapis.com/overview?project=recordatoriotareas-6a384
- Asegúrate de que esté **HABILITADA**

## 🚀 DESPUÉS DE CONFIGURAR

1. **Espera 2-3 minutos** para que se propaguen los cambios
2. **Reinstala la app:**
   ```bash
   ./gradlew clean
   ./gradlew installDebug
   ```
3. **Prueba el registro** nuevamente

## 📱 CONFIGURACIÓN RECOMENDADA

### Firestore Rules (modo desarrollo):
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### Authentication Settings:
- ✅ Email/Password habilitado
- ✅ Dominios autorizados: localhost, tu-dominio.com

## ⚠️ IMPORTANTE
- **NO uses modo producción** hasta que configures reglas de seguridad
- **Guarda las reglas** después de crearlas
- **Espera unos minutos** antes de probar