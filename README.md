# ECO-MAP

Aplicación Android en español para conectar ciudadanos, recolectores, centros de acopio y administradores en la gestión de materiales reciclables.

## Requisitos

- Android Studio, JDK 17 y Android SDK 34.
- Kotlin 2.x, Gradle 8.7.

## Configuración

1. Copia `secrets.properties.example` como `secrets.properties`.
2. Añade los tokens de Mapbox y Gemini sin subirlos al repositorio.
3. Reemplaza `app/google-services.json` con el archivo descargado desde Firebase Console.

## Compilación

```bash
./gradlew assembleDebug
```

Los datos sensibles y el modelo TensorFlow Lite son placeholders hasta configurar los servicios correspondientes.
