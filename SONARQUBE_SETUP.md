# Configuración de SonarQube para Recordatorio Modelo 2

## Prerrequisitos

1. **SonarQube Server**: Asegúrate de que SonarQube esté ejecutándose en `http://localhost:9000`
2. **Token de acceso**: Ya tienes tu token generado
3. **Java 11+**: Requerido para el análisis

## Archivos de Configuración

### 1. `sonar-project.properties`
Archivo de configuración principal con las propiedades del proyecto.

### 2. `build.gradle.kts` (Root)
Configurado con el plugin de SonarQube y propiedades básicas.

### 3. `run-sonar-analysis.ps1`
Script de PowerShell para ejecutar el análisis fácilmente.

## Cómo Ejecutar el Análisis

### Opción 1: Usando el Script de PowerShell (Recomendado)
```powershell
.\run-sonar-analysis.ps1 -Token "tu-token-aqui"
```

### Opción 2: Usando Gradle directamente
```bash
# Configurar el token como variable de entorno
$env:SONAR_TOKEN = "tu-token-aqui"

# Ejecutar el análisis
./gradlew sonar
```

### Opción 3: Con parámetros en línea de comandos
```bash
./gradlew sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=tu-token-aqui
```

## Configuración Adicional

### Para usar un servidor SonarQube diferente:
```powershell
.\run-sonar-analysis.ps1 -Token "tu-token-aqui" -SonarUrl "http://tu-servidor:9000"
```

### Para configurar cobertura de código:
1. Agrega el plugin JaCoCo al `app/build.gradle.kts`:
```kotlin
plugins {
    // ... otros plugins
    jacoco
}

jacoco {
    toolVersion = "0.8.8"
}

tasks.register<JacocoReport>("testDebugUnitTestCoverage") {
    dependsOn("testDebugUnitTest")
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    
    val fileFilter = listOf(
        "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
        "**/*Test*.*", "android/**/*.*", "**/data/local/Converters.*"
    )
    
    val debugTree = fileTree("${buildDir}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }
    
    val mainSrc = "${project.projectDir}/src/main/java"
    
    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(buildDir) {
        include("jacoco/testDebugUnitTest.exec")
    })
}
```

## Verificación

Después de ejecutar el análisis:

1. Ve a `http://localhost:9000`
2. Busca el proyecto "recordatorio-modelo2"
3. Revisa los resultados del análisis

## Problemas Comunes

### Error de conexión
- Verifica que SonarQube esté ejecutándose
- Comprueba la URL del servidor

### Error de autenticación
- Verifica que el token sea correcto
- Asegúrate de que el token tenga permisos de análisis

### Error de compilación
- Ejecuta `./gradlew clean build` antes del análisis
- Verifica que no haya errores de compilación

## Configuración de CI/CD

Para integrar con GitHub Actions, Jenkins u otros sistemas CI/CD, usa:

```yaml
# Ejemplo para GitHub Actions
- name: SonarQube Analysis
  run: |
    ./gradlew sonar \
      -Dsonar.host.url=${{ secrets.SONAR_HOST_URL }} \
      -Dsonar.token=${{ secrets.SONAR_TOKEN }}
```