# 📧 Configuración de Servicio de Correo

## Opciones Disponibles

### 1. Gmail con App Password (Recomendada)

**Ventajas:**
- Gratuito
- Confiable
- Fácil de configurar

**Pasos:**
1. Ve a [myaccount.google.com](https://myaccount.google.com)
2. Seguridad → Verificación en 2 pasos (habilitarla si no está activa)
3. Seguridad → Contraseñas de aplicaciones
4. Selecciona "Aplicación personalizada" → "RecordatorioApp"
5. Copia la contraseña generada (16 caracteres)

**Configuración en EmailService.kt:**
```kotlin
private const val SMTP_HOST = "smtp.gmail.com"
private const val SMTP_PORT = "587"
private const val EMAIL_FROM = "tu-email@gmail.com"
private const val EMAIL_PASSWORD = "app-password-generada"
```

### 2. Outlook/Hotmail

**Ventajas:**
- Más simple (no necesita App Password)
- Gratuito

**Configuración:**
```kotlin
private const val SMTP_HOST = "smtp-mail.outlook.com"
private const val SMTP_PORT = "587"
private const val EMAIL_FROM = "tu-email@outlook.com"
private const val EMAIL_PASSWORD = "tu-contraseña-normal"
```

### 3. SendGrid (Para producción)

**Ventajas:**
- 100 correos gratis por día
- Muy confiable
- Estadísticas detalladas

**Pasos:**
1. Registrarse en [sendgrid.com](https://sendgrid.com)
2. Crear API Key
3. Verificar dominio/email

## Configuración Segura

Para mayor seguridad, las credenciales deberían estar en:

**gradle.properties (local):**
```properties
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_FROM=tu-email@gmail.com
EMAIL_PASSWORD=tu-app-password
```

**build.gradle.kts:**
```kotlin
buildConfigField("String", "EMAIL_HOST", "\"${project.findProperty("EMAIL_HOST")}\"")
buildConfigField("String", "EMAIL_FROM", "\"${project.findProperty("EMAIL_FROM")}\"")
buildConfigField("String", "EMAIL_PASSWORD", "\"${project.findProperty("EMAIL_PASSWORD")}\"")
```

## ¿Cuál elegir?

- **Para desarrollo/pruebas:** Gmail con App Password
- **Para producción pequeña:** Outlook
- **Para producción seria:** SendGrid

## Próximos pasos

1. Decide qué servicio usar
2. Proporciona las credenciales
3. Actualizo la configuración
4. Probamos el envío