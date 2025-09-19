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
- Git configurado con tu cuenta de GitHub

### Pasos
1. **Clonar el repositorio**
   ```bash
   git clone https://github.com/[TU_USUARIO]/RecordatorioModelo2.git
   cd RecordatorioModelo2
   ```

2. **Configurar Google Services**
   - Copiar `google-services.json` a `app/`
   - Copiar `client_secret_*.json` a `app/`

3. **Ejecutar en Android Studio**
   - Abrir proyecto en Android Studio
   - Sincronizar Gradle
   - Presionar ▶️ Run

## 👥 Flujo de Trabajo para el Equipo

### 🔒 Protección de la Rama Principal
Este repositorio tiene configurada la **protección de la rama `main`** con las siguientes reglas:
- ❌ **No se permiten pushes directos** a `main`
- ✅ **Obligatorio crear Pull Requests** para todos los cambios
- 👥 **Requiere al menos 1 aprobación** antes del merge
- 🔄 **La rama debe estar actualizada** antes del merge
- 💬 **Todos los comentarios deben resolverse** antes del merge

### 🛠️ Proceso de Desarrollo

#### 1. Configuración Inicial (Solo la primera vez)
```bash
# Clonar el repositorio
git clone https://github.com/[TU_USUARIO]/RecordatorioModelo2.git
cd RecordatorioModelo2

# Configurar tu información (si no lo has hecho)
git config user.name "Tu Nombre"
git config user.email "tu.email@ejemplo.com"
```

#### 2. Para Cada Nueva Funcionalidad
```bash
# 1. Asegúrate de estar en main y actualizado
git checkout main
git pull origin main

# 2. Crear una nueva rama para tu funcionalidad
git checkout -b feature/nombre-de-tu-funcionalidad
# Ejemplos:
# git checkout -b feature/login-improvements
# git checkout -b feature/notification-system
# git checkout -b bugfix/classroom-sync-error

# 3. Realizar tus cambios y commits
git add .
git commit -m "Descripción clara de los cambios"

# 4. Subir tu rama al repositorio
git push origin feature/nombre-de-tu-funcionalidad
```

#### 3. Crear Pull Request
1. Ve a GitHub.com y navega al repositorio
2. Verás un botón **"Compare & pull request"** - haz clic
3. Completa la información:
   - **Título**: Descripción clara de los cambios
   - **Descripción**: Explica qué cambios hiciste y por qué
   - **Reviewers**: Asigna a otros miembros del equipo
4. Haz clic en **"Create pull request"**

#### 4. Revisión y Merge
- Los miembros del equipo revisarán tu código
- Responde a comentarios y realiza cambios si es necesario
- Una vez aprobado, el PR se puede hacer merge a `main`

### 📋 Convenciones del Equipo

#### Nombres de Ramas
- `feature/descripcion-funcionalidad` - Para nuevas funcionalidades
- `bugfix/descripcion-error` - Para corrección de errores
- `hotfix/descripcion-urgente` - Para correcciones urgentes
- `refactor/descripcion-mejora` - Para refactorización de código

#### Mensajes de Commit
```bash
# Formato recomendado:
# tipo: descripción breve

# Ejemplos:
git commit -m "feat: agregar autenticación con Google"
git commit -m "fix: corregir error en sincronización de tareas"
git commit -m "refactor: separar componentes de navegación"
git commit -m "docs: actualizar README con instrucciones"
```

#### Tipos de Commit
- `feat:` - Nueva funcionalidad
- `fix:` - Corrección de errores
- `refactor:` - Refactorización de código
- `docs:` - Cambios en documentación
- `style:` - Cambios de formato (espacios, etc.)
- `test:` - Agregar o modificar tests

### 🚨 Reglas Importantes
1. **NUNCA** hagas push directo a `main`
2. **SIEMPRE** crea una rama para tus cambios
3. **SIEMPRE** crea un Pull Request
4. **REVISA** el código de tus compañeros
5. **MANTÉN** tus ramas actualizadas con `main`
6. **ELIMINA** las ramas después del merge

### 🔧 Comandos Útiles
```bash
# Ver el estado actual
git status

# Ver todas las ramas
git branch -a

# Cambiar a main y actualizar
git checkout main && git pull origin main

# Eliminar rama local después del merge
git branch -d feature/nombre-rama

# Ver historial de commits
git log --oneline

# Deshacer último commit (mantener cambios)
git reset --soft HEAD~1
```

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