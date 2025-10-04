# Solución: Problema de Índices en Firestore

## Problema Identificado

Las tareas se estaban guardando correctamente en la base de datos local (Room) con el `userId` apropiado, pero **no se sincronizaban con Firestore** debido a un error de índices faltantes.

### Error Específico
```
FAILED_PRECONDITION: The query requires an index. You can create it here: https://console.firebase.google.com/v1/r/project/recordatoriotareas-6a384/firestore/indexes?create_composite=...
```

### Consulta Problemática
En `TaskRepository.kt`, línea 78:
```kotlin
firestore.collection(TASKS_COLLECTION)
    .whereEqualTo("userId", userId)
    .orderBy("createdAt", Query.Direction.DESCENDING)
```

Esta consulta requiere un **índice compuesto** en Firestore para los campos `userId` y `createdAt`.

## Solución Implementada

### 1. Archivo de Configuración de Índices
Se creó `firestore.indexes.json` con los índices necesarios:

```json
{
  "indexes": [
    {
      "collectionGroup": "tasks",
      "queryScope": "COLLECTION",
      "fields": [
        {
          "fieldPath": "userId",
          "order": "ASCENDING"
        },
        {
          "fieldPath": "createdAt",
          "order": "DESCENDING"
        }
      ]
    }
  ]
}
```

### 2. Script de Despliegue
Se creó `deploy-firestore-indexes.bat` para automatizar el despliegue de índices.

## Pasos para Resolver el Problema

### Opción 1: Usar Firebase CLI (Recomendado)

#### Paso 1: Instalar Node.js (si no está instalado)
1. Descargar Node.js desde: https://nodejs.org/
2. Instalar la versión LTS recomendada
3. Verificar instalación:
   ```bash
   node --version
   npm --version
   ```

#### Paso 2: Instalar Firebase CLI
```bash
npm install -g firebase-tools
```

#### Paso 3: Autenticarse en Firebase
```bash
firebase login
```
Esto abrirá el navegador para autenticarte con tu cuenta de Google.

#### Paso 4: Inicializar el proyecto (si no está inicializado)
```bash
firebase init firestore
```
- Seleccionar el proyecto: `recordatoriotareas-6a384`
- Usar `firestore.rules` existente
- Usar `firestore.indexes.json` existente

#### Paso 5: Desplegar los índices
```bash
firebase deploy --only firestore:indexes
```

O ejecutar el script:
```bash
deploy-firestore-indexes.bat
```

### Opción 2: Crear Índices Manualmente en la Consola
1. Ir a [Firebase Console](https://console.firebase.google.com/project/recordatoriotareas-6a384/firestore/indexes)
2. Hacer clic en "Create Index"
3. Configurar:
   - **Collection ID**: `tasks`
   - **Fields**:
     - `userId` (Ascending)
     - `createdAt` (Descending)
4. Hacer clic en "Create"

### Opción 3: Usar el Enlace del Error
El error proporciona un enlace directo para crear el índice:
```
https://console.firebase.google.com/v1/r/project/recordatoriotareas-6a384/firestore/indexes?create_composite=...
```

## Verificación

Después de crear los índices:

1. **Esperar** unos minutos para que los índices se construyan
2. **Reiniciar la aplicación**
3. **Crear una nueva tarea**
4. **Verificar** que aparezca en Firestore Console
5. **Limpiar memoria** de la app y verificar que las tareas se cargan desde Firestore

## Estado Actual

✅ **Problema identificado**: Falta de índices en Firestore
✅ **Solución preparada**: Archivos de configuración creados
⏳ **Pendiente**: Desplegar índices a Firestore
⏳ **Pendiente**: Verificar funcionamiento completo

## Archivos Creados/Modificados

- `firestore.indexes.json` - Configuración de índices
- `deploy-firestore-indexes.bat` - Script de despliegue
- `SOLUCION_INDICES_FIRESTORE.md` - Esta documentación

## Próximos Pasos

1. Desplegar los índices usando uno de los métodos anteriores
2. Probar la sincronización de tareas
3. Verificar que las tareas persisten después de limpiar memoria
4. Implementar detección de conectividad para modo offline