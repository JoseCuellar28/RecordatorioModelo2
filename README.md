# 📚 RecordatorioModelo2 - Gestor de Tareas Académicas

[![Android](https://img.shields.io/badge/Android-API%2024+-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blue.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-1.5+-orange.svg)](https://developer.android.com/jetpack/compose)

Una aplicación Android moderna para gestionar tareas académicas con integración completa de Google Classroom.

## ✨ Características

- 🔐 **Autenticación dual**: Local y Google Sign-In
- 📚 **Integración con Google Classroom**: Importa cursos y tareas automáticamente
- 📝 **Gestión completa de tareas**: CRUD con recordatorios
- 🔔 **Sistema de notificaciones**: Recordatorios programables
- 🎨 **UI moderna**: Material 3 con Jetpack Compose
- 📊 **Dashboard intuitivo**: Estadísticas y acciones rápidas
- 👤 **Perfil de usuario**: Gestión de información personal

## 🚀 Instalación Rápida

### Prerrequisitos
- Android Studio Hedgehog (2023.1.1+)
- JDK 11+
- Dispositivo/Emulador Android API 24+

### Pasos
1. **Clonar el repositorio**
   ```bash
   git clone [URL_DEL_REPOSITORIO]
   cd Android
   ```

2. **Configurar Google Services**
   - Copiar `google-services.json` a `app/`
   - Copiar `client_secret_*.json` a `app/`

3. **Ejecutar en Android Studio**
   - Abrir proyecto en Android Studio
   - Sincronizar Gradle
   - Presionar ▶️ Run

## 📱 Capturas de Pantalla

| Login | Dashboard | Tareas | Perfil |
|-------|-----------|--------|--------|
| ![Login](screenshots/login.png) | ![Dashboard](screenshots/dashboard.png) | ![Tareas](screenshots/tasks.png) | ![Perfil](screenshots/profile.png) |

## 🛠️ Tecnologías

- **Arquitectura**: MVVM
- **UI**: Jetpack Compose + Material 3
- **Base de Datos**: Room + Kotlin Coroutines
- **Navegación**: Navigation Compose
- **Autenticación**: Google Sign-In API
- **APIs**: Google Classroom API
- **Notificaciones**: AlarmManager + BroadcastReceiver

## 📋 Funcionalidades

### Autenticación
- Login local con credenciales
- Login con Google
- Selector de método de autenticación

### Gestión de Tareas
- Crear, editar, eliminar tareas
- Marcar como completadas
- Fechas de vencimiento
- Recordatorios programables
- Colores por materia

### Google Classroom
- Importar cursos
- Importar tareas por curso
- Deduplicación automática
- Sincronización de fechas

### Perfil de Usuario
- Información personal
- Configuración de preferencias
- Interfaz moderna

## 🔧 Configuración Detallada

Para configuración completa, ver [REQUERIMIENTOS.md](REQUERIMIENTOS.md)

## 📊 Estado del Proyecto

- ✅ **Funcional**: Listo para uso y demostración
- ✅ **UI/UX**: Moderna y responsiva
- ✅ **Integración**: Google Classroom funcional
- ✅ **Base de datos**: Room implementado
- ⚠️ **Refactorización**: AppNavigation.kt necesita separación

## 🚧 Próximas Mejoras

- [ ] Refactorizar AppNavigation.kt
- [ ] Implementar tests unitarios
- [ ] Migrar perfil a base de datos
- [ ] Agregar búsqueda y filtros
- [ ] Implementar modo oscuro
- [ ] Sincronización automática

## 📞 Soporte

### Credenciales de Prueba
- **Usuario**: admin
- **Contraseña**: 1234

### Logs de Debug
```bash
adb logcat | grep "RecordatorioModelo2"
```

## 📄 Licencia

Este proyecto es parte de un trabajo académico.

## 👥 Autores

Desarrollado como proyecto de curso de desarrollo Android.

---

**Nota**: Requiere conexión a internet para la integración con Google Classroom. 