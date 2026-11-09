# Historial de cambios

Este archivo registra cambios relevantes del proyecto. Añadir una entrada por cada actualización funcional o técnica.

## 2026-11-08 — Documentación inicial del proyecto

- Se documentó el propósito de ADMIN y el flujo de trabajo deseado con ChatGPT, GitHub, Android Studio y emulador.
- Se registraron los requisitos funcionales solicitados.
- Se documentó la arquitectura observada en el código actual.
- Se separaron las funciones confirmadas de las pendientes y no verificadas.
- Se documentó el flujo local conocido y se aclaró que la automatización completa aún no está configurada.

## 2026-11-08 — Visor de comprobantes y navegación

- El visor de comprobantes ahora incluye cerrar (X), descargar mediante el selector de guardado de Android, copiar imagen al portapapeles y compartir mediante el menú nativo de Android.
- Se agregó FileProvider con acceso limitado a la carpeta privada de comprobantes para copiar y compartir imágenes.
- Se eliminó el botón interno «VOLVER» de la pantalla de deuda y se agregó navegación con el botón/gesto Atrás de Android.
- Cambios enviados a la rama `admin-app`; compilación e instalación de esta actualización aún pendientes de verificar.
