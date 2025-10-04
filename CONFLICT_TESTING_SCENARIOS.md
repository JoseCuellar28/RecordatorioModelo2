# Escenarios de Prueba para Conflictos de Sincronización

## Resumen de la Implementación

La aplicación ahora incluye un sistema completo de detección y resolución de conflictos de sincronización que incluye:

### Componentes Implementados

1. **ConflictResolver**: Detecta y resuelve conflictos automáticamente
2. **SyncManager**: Integra detección automática durante la sincronización
3. **ConflictDialog**: UI para mostrar y resolver conflictos manualmente
4. **SyncIndicator**: Indicador visual de conflictos pendientes
5. **TaskRepository**: Manejo de conflictos en sincronización en segundo plano

### Tipos de Conflictos Soportados

- **CONTENT_MODIFIED**: Modificación simultánea de contenido
- **DELETED_LOCALLY**: Tarea eliminada localmente pero modificada remotamente
- **DELETED_REMOTELY**: Tarea eliminada remotamente pero modificada localmente
- **CREATION_CONFLICT**: Conflicto en la creación de tareas

### Estrategias de Resolución

- **PREFER_LOCAL**: Mantener versión local
- **PREFER_REMOTE**: Mantener versión remota
- **MERGE_CONTENT**: Fusionar contenido (automático)
- **PREFER_NEWEST**: Usar la versión más reciente
- **ASK_USER**: Solicitar decisión del usuario

## Escenarios de Prueba

### Escenario 1: Modificación Simultánea de Contenido

**Objetivo**: Verificar que se detectan conflictos cuando la misma tarea se modifica en dos dispositivos.

**Pasos**:
1. Crear una tarea "Estudiar Matemáticas" en el Dispositivo A
2. Sincronizar con Firestore
3. Modificar el título a "Estudiar Álgebra" en el Dispositivo A (sin sincronizar)
4. Modificar el título a "Estudiar Cálculo" en el Dispositivo B y sincronizar
5. Intentar sincronizar en el Dispositivo A

**Resultado Esperado**:
- Se detecta un conflicto de tipo CONTENT_MODIFIED
- Aparece el ConflictDialog mostrando ambas versiones
- El usuario puede elegir entre "Estudiar Álgebra" (local) o "Estudiar Cálculo" (remoto)

### Escenario 2: Eliminación vs Modificación (Local)

**Objetivo**: Verificar conflictos cuando una tarea se elimina localmente pero se modifica remotamente.

**Pasos**:
1. Crear una tarea "Proyecto de Física" en ambos dispositivos
2. Sincronizar ambos dispositivos
3. Eliminar la tarea en el Dispositivo A (sin sincronizar)
4. Modificar la descripción de la tarea en el Dispositivo B y sincronizar
5. Intentar sincronizar en el Dispositivo A

**Resultado Esperado**:
- Se detecta un conflicto de tipo DELETED_LOCALLY
- El ConflictDialog muestra la opción de mantener eliminada o restaurar con cambios remotos

### Escenario 3: Eliminación vs Modificación (Remoto)

**Objetivo**: Verificar conflictos cuando una tarea se elimina remotamente pero se modifica localmente.

**Pasos**:
1. Crear una tarea "Ensayo de Historia" en ambos dispositivos
2. Sincronizar ambos dispositivos
3. Modificar la fecha de vencimiento en el Dispositivo A (sin sincronizar)
4. Eliminar la tarea en el Dispositivo B y sincronizar
5. Intentar sincronizar en el Dispositivo A

**Resultado Esperado**:
- Se detecta un conflicto de tipo DELETED_REMOTELY
- El ConflictDialog permite mantener la versión local modificada o confirmar eliminación

### Escenario 4: Conflicto de Creación

**Objetivo**: Verificar conflictos cuando se crean tareas con el mismo ID en dispositivos diferentes.

**Pasos**:
1. Desconectar ambos dispositivos de internet
2. Crear una tarea con el mismo título en ambos dispositivos
3. Reconectar y sincronizar el Dispositivo A
4. Sincronizar el Dispositivo B

**Resultado Esperado**:
- Se detecta un conflicto de tipo CREATION_CONFLICT
- El sistema maneja automáticamente asignando IDs únicos o solicita resolución manual

### Escenario 5: Resolución Automática por Fecha

**Objetivo**: Verificar que la resolución automática funciona correctamente.

**Pasos**:
1. Crear una tarea "Laboratorio de Química" en ambos dispositivos
2. Sincronizar ambos dispositivos
3. Modificar solo la descripción en el Dispositivo A (timestamp más antiguo)
4. Modificar solo el estado a completado en el Dispositivo B (timestamp más reciente)
5. Sincronizar ambos dispositivos

**Resultado Esperado**:
- El sistema resuelve automáticamente usando PREFER_NEWEST
- La versión del Dispositivo B prevalece sin mostrar diálogo de conflicto

### Escenario 6: Resolución Masiva de Conflictos

**Objetivo**: Verificar el manejo de múltiples conflictos simultáneos.

**Pasos**:
1. Crear 5 tareas en ambos dispositivos
2. Sincronizar ambos dispositivos
3. Modificar todas las tareas en el Dispositivo A de diferentes maneras
4. Modificar las mismas tareas en el Dispositivo B de maneras diferentes
5. Sincronizar ambos dispositivos

**Resultado Esperado**:
- Se detectan múltiples conflictos
- El ConflictDialog muestra todos los conflictos en una lista
- Los botones "Mantener Todo Local" y "Mantener Todo Remoto" funcionan correctamente

## Verificación de la UI

### Indicadores Visuales

1. **SyncIndicator**: 
   - Muestra icono de advertencia (⚠️) cuando hay conflictos pendientes
   - Color rojo para indicar estado de error
   - Botón para abrir el diálogo de conflictos

2. **ConflictDialog**:
   - Lista scrolleable de conflictos
   - Comparación lado a lado de versiones local y remota
   - Botones individuales para cada conflicto
   - Botones de resolución masiva

3. **Estados de Sincronización**:
   - ERROR cuando hay conflictos pendientes
   - SYNCING durante la detección de conflictos
   - SUCCESS cuando todos los conflictos se resuelven

## Logs de Depuración

Para monitorear la funcionalidad, revisar estos logs:

```bash
adb logcat -s TaskRepository TasksViewModel SyncManager ConflictResolver
```

### Mensajes Clave:

- `ConflictResolver: Conflicto detectado: CONTENT_MODIFIED`
- `SyncManager: Conflictos detectados automáticamente: X`
- `TaskRepository: Conflictos detectados durante sincronización automática: X`
- `TasksViewModel: Resolviendo conflicto: [ID] con resolución: [TIPO]`

## Notas de Implementación

1. **Detección Automática**: Se ejecuta durante cada sincronización en `TaskRepository.syncTasksInBackground()`
2. **Resolución Manual**: Disponible a través del `ConflictDialog` accesible desde el `SyncIndicator`
3. **Persistencia**: Los conflictos se mantienen en memoria hasta su resolución
4. **Estrategias**: Combinación de resolución automática y manual según el tipo de conflicto

## Estado Actual

✅ **Completado**:
- Detección automática de conflictos
- UI completa para resolución manual
- Integración con sistema de sincronización
- Indicadores visuales
- Manejo de errores

🔄 **Funcional**: La aplicación compila correctamente y está lista para pruebas de conflictos.