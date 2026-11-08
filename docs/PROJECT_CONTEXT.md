# Contexto del proyecto

## Propósito

ADMIN es una aplicación Android nativa para gestionar deudas personales. El usuario describe los cambios que necesita en lenguaje natural; el asistente analiza los requisitos, modifica el código fuente y publica los cambios en GitHub. Android Studio debe obtener el código actualizado, compilarlo y permitir visualizarlo en un emulador. Cuando la versión esté lista, se genera una APK instalable.

## Objetivo de trabajo a largo plazo

Reducir al mínimo el trabajo manual del usuario:
1. El usuario explica qué aplicación quiere o qué cambio necesita.
2. El asistente revisa el repositorio y su documentación antes de programar.
3. El asistente modifica el código y actualiza la documentación.
4. Los cambios se publican en GitHub, en la rama correspondiente.
5. Android Studio obtiene los cambios, compila y permite probarlos en el emulador.
6. Tras validar la aplicación, se prepara una APK para instalar.

La sincronización, compilación, instalación y publicación automática no deben darse por configuradas hasta que exista y se pruebe un flujo automatizado real. Mientras tanto, indicar claramente qué paso requiere intervención del usuario.

## Reglas para cualquier asistente o desarrollador que continúe

- Leer esta documentación y revisar el código real antes de hacer cambios.
- No asumir que el contenido de conversaciones anteriores sigue disponible.
- No afirmar que una función está implementada solo porque aparece en los requisitos.
- Mantener intacta la rama `main`; el trabajo actual se realiza en `admin-app`.
- No borrar ni reemplazar proyectos locales del usuario sin autorización.
- Hacer cambios pequeños, verificables y con mensajes de commit descriptivos.
- Actualizar `TECHNICAL_STATUS.md`, `ARCHITECTURE.md` y `CHANGELOG.md` cuando corresponda.
- Explicar al usuario los pasos de Android Studio con instrucciones concretas y basadas en lo que ve en pantalla.

## Repositorio

- Repositorio: https://github.com/renzodavidpizarroduran304-stack/app
- Rama de trabajo actual: `admin-app`
- Proyecto Android: módulo `app`
- Identificador de aplicación: `com.example.admin`

## Decisiones que no deben perderse

- La aplicación debe ser Android nativa; no sustituirla por una web, PWA o maqueta.
- La APK final debe ser instalable y verificarse en un dispositivo o emulador.
- El usuario quiere poder crear futuras aplicaciones con el mismo método: describir, programar en el repositorio, probar en emulador y obtener la APK.
