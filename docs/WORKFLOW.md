# Flujo de trabajo: GitHub, Android Studio y emulador

## Repositorio

- URL: https://github.com/renzodavidpizarroduran304-stack/app
- Rama de trabajo: `admin-app`
- Proyecto local usado en la sesión: `C:\Users\SOLUTIONS\Downloads\app-admin-app`
- Nombre del proyecto en Android Studio: ADMIN

## Flujo actual conocido

1. El asistente modifica los archivos de la rama `admin-app` en GitHub.
2. El usuario debe obtener los cambios en su copia local mediante la sincronización/actualización de GitHub en Android Studio o descargando de nuevo el repositorio.
3. Android Studio sincroniza Gradle y compila el módulo `app`.
4. Se inicia el emulador y se ejecuta la aplicación para probarla.
5. Cuando la versión esté validada, se genera una APK release firmada.

## Automatización

El objetivo futuro es que los pasos de obtener cambios, compilar e instalar sean automáticos o requieran el mínimo de clics. **No está configurado ni probado todavía.** No afirmar que un push a GitHub actualiza automáticamente Android Studio o el emulador.

Posibles etapas de automatización:
- Configurar un flujo CI en GitHub Actions para compilar cada cambio.
- Publicar APK de prueba como artefacto de CI.
- Configurar un mecanismo fiable para sincronizar la copia local.
- Automatizar instalación/ejecución en emulador local, si el entorno lo permite.
- Publicar una GitHub Release cuando una versión esté probada y lista.

La compilación en GitHub Actions no instala por sí sola la app en el emulador que está abierto en el PC del usuario. Para eso hace falta un puente local o una acción manual.

## Compilación local

En Android Studio:
1. Abrir el proyecto descargado desde GitHub, no el proyecto antiguo de prueba.
2. Esperar a que termine la sincronización de Gradle.
3. Seleccionar el emulador Small Phone (Android 14/API 34) cuando esté iniciado.
4. Ejecutar la configuración `app` con el botón Run.
5. Para generar APK debug, usar Build > Build Bundle(s) / APK(s) > Build APK(s), y localizar el archivo que Android Studio indique.

La APK debug sirve para pruebas. Antes de distribuir una versión final se debe generar una APK release firmada y probarla.

## Notificaciones

La configuración de notificaciones del repositorio está ajustada para recibir notificaciones de Releases. Eso no equivale a recibir una notificación por cada push o commit. Publicar una Release solo cuando exista una versión preparada y verificada.
