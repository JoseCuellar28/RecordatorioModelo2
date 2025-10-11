# 🧪 Guía de Pruebas Unitarias - RecordatorioModelo2

## 📋 Resumen

Este proyecto implementa pruebas unitarias usando **JUnit + MockK** para garantizar la calidad del código en las funcionalidades reales implementadas como operaciones CRUD, validaciones de seguridad, y gestión de sincronización.

## 🛠️ Tecnologías Utilizadas

- **JUnit 4** - Framework principal de testing
- **MockK** - Librería de mocking para Kotlin
- **Coroutines Test** - Para testing de código asíncrono
- **Truth** - Assertions más legibles (opcional)

## 📁 Estructura de Pruebas

```
app/src/test/java/com/example/recordatoriomodelo2/
├── data/
│   ├── repository/
│   │   └── TaskRepositoryTest.kt
│   └── sync/
│       └── SyncManagerTest.kt
└── ExampleUnitTest.kt (archivo original)
```

## 🚀 Cómo Ejecutar las Pruebas

### Opción 1: Desde Android Studio
1. **Ejecutar todas las pruebas:**
   - Clic derecho en `app/src/test/java`
   - Seleccionar "Run 'Tests in java'"

2. **Ejecutar pruebas específicas:**
   - Abrir `SyncManagerTest.kt` o `TaskRepositoryTest.kt`
   - Clic en el ícono ▶️ junto a la clase o método específico

### Opción 2: Desde Terminal/Línea de Comandos
```bash
# Ejecutar todas las pruebas unitarias
./gradlew test

# Ejecutar solo pruebas de debug
./gradlew testDebugUnitTest

# Ejecutar con reporte detallado
./gradlew test --info

# Ejecutar pruebas específicas
./gradlew test --tests "*SyncManagerTest*"
./gradlew test --tests "*TaskRepositoryTest*"
```

### Opción 3: Desde PowerShell (Windows)
```powershell
# Navegar al directorio del proyecto
cd "E:\Gestion_proyectos\RecordatorioModelo2"

# Ejecutar todas las pruebas
.\gradlew.bat test

# Ver resultados detallados
.\gradlew.bat test --info
```

## 📊 Interpretando los Resultados

### ✅ Pruebas Exitosas
```
BUILD SUCCESSFUL in 15s
4 actionable tasks: 4 executed
```

### ❌ Pruebas Fallidas
```
TaskRepositoryTest > deleteTask should call markTaskAsDeleted before deletion FAILED
    Expected: exactly 1 call
    Actual: 0 calls
```

### 📈 Reporte HTML
Los resultados detallados se generan en:
```
app/build/reports/tests/testDebugUnitTest/index.html
```

## 🧪 Pruebas Implementadas

### SyncManagerTest.kt
Prueba las **funcionalidades core de sincronización**:

1. **`isNetworkAvailable should return correct network status`**
   - Verifica la detección del estado de red
   - Prueba tanto conexión como desconexión

2. **`syncState should start as IDLE`**
   - Verifica el estado inicial del manager
   - Asegura configuración correcta

3. **`syncedTasksCount should start at zero`**
   - Verifica contador inicial de tareas sincronizadas
   - Prueba estado limpio al inicio

4. **`lastError should be null initially`**
   - Verifica que no hay errores al inicio
   - Asegura estado limpio

5. **`stopRealtimeSync should stop active synchronization`**
   - Verifica que se puede detener la sincronización
   - Prueba limpieza de recursos

6. **`forceSyncNow should fail without network`**
   - Verifica manejo de errores sin conexión
   - Prueba validación de red

7. **`restartSync should re-establish synchronization`**
   - Verifica capacidad de reiniciar sincronización
   - Prueba recuperación de conexión

8. **`cleanup should release all resources`**
   - Verifica liberación correcta de recursos
   - Prueba limpieza al finalizar

### TaskRepositoryTest.kt
Prueba las **operaciones CRUD y validaciones de seguridad**:

1. **`insertTask should save task in Room and Firestore`**
   - Verifica inserción en ambas fuentes de datos
   - Prueba validaciones de seguridad

2. **`insertTask should fail when security validation fails`**
   - Verifica manejo de errores de seguridad
   - Prueba protección contra acceso no autorizado

3. **`updateTask should modify task in Room and Firestore`**
   - Verifica actualización en ambas fuentes
   - Prueba validaciones de acceso

4. **`updateTask should fail when user is not authorized`**
   - Verifica protección contra modificaciones no autorizadas
   - Prueba validación de propietario

5. **`deleteTask should remove task from Room and Firestore`**
   - Verifica eliminación en ambas fuentes
   - Prueba marcado para prevenir "resurrección"

6. **`deleteTask should call operations in correct order`**
   - Verifica orden: markAsDeleted → Firestore → Room
   - Asegura integridad del proceso

7. **`deleteTask should fail when user is not authorized`**
   - Verifica protección contra eliminaciones no autorizadas
   - Prueba validación de seguridad

8. **`deleteTask should handle multiple deletions`**
   - Prueba eliminaciones en lote
   - Verifica que cada eliminación se procese correctamente

9. **`insertTask should assign correct userId from security validation`**
   - Verifica asignación correcta de usuario
   - Prueba integridad de datos de seguridad

10. **`updateTask should reject task with wrong userId`**
    - Verifica rechazo de tareas de otros usuarios
    - Prueba protección de datos

## 🔧 Comandos Útiles para Desarrollo

### Limpiar y Ejecutar Pruebas
```bash
# Limpiar proyecto y ejecutar pruebas
./gradlew clean test

# Ejecutar pruebas con stacktrace completo
./gradlew test --stacktrace
```

### Debugging de Pruebas
```bash
# Ejecutar con logs detallados
./gradlew test --debug

# Ejecutar pruebas específicas con logs
./gradlew test --tests "*SyncManagerTest*" --info
```

### Generar Reportes de Cobertura
```bash
# Ejecutar con reporte de cobertura (si está configurado)
./gradlew testDebugUnitTestCoverage
```

## 📝 Cómo Agregar Nuevas Pruebas

### 1. Crear Nueva Clase de Prueba
```kotlin
package com.example.recordatoriomodelo2.tu.paquete

import io.mockk.*
import org.junit.*
import kotlinx.coroutines.test.runTest

class TuClaseTest {
    
    @Before
    fun setUp() {
        // Configuración antes de cada prueba
    }
    
    @After
    fun tearDown() {
        clearAllMocks()
    }
    
    @Test
    fun `tu prueba descriptiva`() = runTest {
        // Given - Configuración
        
        // When - Acción
        
        // Then - Verificación
        assertTrue("Mensaje descriptivo", condicion)
    }
}
```

### 2. Patrones de Naming
- **Clases:** `[ClaseOriginal]Test.kt`
- **Métodos:** `[metodo] should [comportamiento esperado]`
- **Estructura:** Given-When-Then

### 3. Usar MockK Efectivamente
```kotlin
// Crear mock
val mockObject = mockk<TuClase>()

// Configurar comportamiento
every { mockObject.metodo() } returns valor
coEvery { mockObject.metodoSuspend() } returns valor

// Verificar llamadas
verify { mockObject.metodo() }
coVerify { mockObject.metodoSuspend() }
```

## 🎯 Funcionalidades Probadas vs No Probadas

### ✅ Funcionalidades Probadas
- Operaciones CRUD básicas (insert, update, delete)
- Validaciones de seguridad y autorización
- Sincronización dual (Room + Firestore)
- Estado de red y conectividad
- Gestión de recursos y limpieza
- Orden de operaciones
- Manejo de errores de autorización

### ❌ Funcionalidades No Probadas (Aún No Implementadas)
- Filtrado inteligente de tareas eliminadas (feature en desarrollo)
- Resolución automática de conflictos
- Sincronización en tiempo real completa
- Recuperación de errores de red

## 🚨 Troubleshooting

### Error: "MockK could not find a suitable constructor"
```kotlin
// Solución: Usar relaxed mocks
val mock = mockk<TuClase>(relaxed = true)
```

### Error: "Coroutine test framework not found"
```kotlin
// Asegúrate de usar runTest para código con corrutinas
@Test
fun `tu prueba`() = runTest {
    // tu código aquí
}
```

### Error: "Firebase not initialized"
```kotlin
// Mockear Firebase completamente
val mockFirestore = mockk<FirebaseFirestore>()
every { mockFirestore.collection(any()) } returns mockk()
```

## 📚 Recursos Adicionales

- [Documentación oficial de JUnit](https://junit.org/junit4/)
- [Documentación de MockK](https://mockk.io/)
- [Testing en Android](https://developer.android.com/training/testing)
- [Coroutines Testing](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/)

---

**¡Felicidades!** 🎉 Ahora tienes un sistema de pruebas unitarias robusto que te ayudará a mantener la calidad de tu código y cumplir con los requisitos de QA de tu curso.