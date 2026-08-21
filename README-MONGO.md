# Configuración de MongoDB Atlas para ECO-MAP

Si deseas usar MongoDB en lugar de Firebase Firestore, sigue estos pasos:

1. **Crear Cuenta**: Regístrate en [mongodb.com/atlas](https://www.mongodb.com/atlas).
2. **Crear Clúster**: Crea un clúster gratuito (M0) y llámalo "Cluster0".
3. **Habilitar Data API**:
   - Ve a "Data API" en el menú lateral.
   - Habilítala para tu clúster.
   - Genera una "API Key" y cópiala.
   - Copia la "URL Endpoint".
4. **Configurar App**:
   - Abre `secrets.properties`.
   - Cambia `USE_MONGO=true`.
   - Pega tu clave en `MONGO_API_KEY`.
   - Pega tu URL en `MONGO_URL`.
5. **Base de Datos**: La app creará automáticamente la base de datos "ecomap" y la colección "usuarios" al registrar el primer usuario.

*Nota: Firebase Auth se sigue usando para la autenticación básica (email/password), pero el perfil del usuario se guardará en MongoDB.*
