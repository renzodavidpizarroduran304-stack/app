# Estado técnico real

Última revisión documental: 2026-11-08.  
Rama revisada: `admin-app`.

## Confirmado en el repositorio

- Proyecto Android con módulo `app`.
- Kotlin y Jetpack Compose con Material 3.
- Nombre visible: ADMIN.
- Paquete: `com.example.admin`.
- Pantalla principal con tarjetas ISUR, CAJA, BCP y KAELA y suma de pendientes.
- Modelo de datos `Debt` e `Installment`.
- Configuración de número de cuotas, monto y fecha inicial mediante campos de texto.
- Generación de cuotas mensuales que conserva el día original cuando es posible.
- Importes representados en centavos `Long` y parseados con `BigDecimal`.
- Selección de imagen para comprobante y copia a `filesDir/receipts`.
- Pago con validación de comprobante obligatorio y mensaje solicitado.
- Persistencia de deudas como JSON en `SharedPreferences`.
- Visualización básica de imagen de comprobante en un diálogo.

## Confirmado por el usuario en la sesión de trabajo

- Android Studio terminó una compilación local correctamente y generó `app/build/outputs/apk/debug/app-debug.apk`.
- La APK todavía no se ha validado funcionalmente en el emulador.
- Se estaba creando un emulador nuevo Small Phone, Android 14 (API 34), Google Play, x86_64, resolución 720×1280, 320 dpi.
- El emulador anterior Android 17/API 37 presentó bloqueos de Pixel Launcher.
- El usuario indicó que la descarga de la imagen del nuevo emulador terminó y que el dispositivo Small Phone aparece en Device Manager; después inició su arranque. No consta todavía que haya llegado a la pantalla de inicio ni que ADMIN se haya instalado y probado.

## Pendiente o no verificado

- Confirmar que el nuevo emulador arranca y que ADMIN se instala y abre.
- Ejecutar pruebas reales de los 12 escenarios de `REQUIREMENTS_ADMIN.md`.
- Reemplazar el campo de fecha escrito manualmente por un selector de fecha.
- Verificar el comportamiento de las fechas de fin de mes y años bisiestos.
- Proteger explícitamente las cuotas pagadas al editar una deuda; el código actual conserva pagos por número de cuota al regenerar, pero no muestra una confirmación previa ni garantiza que todos los cambios sean seguros.
- Añadir historial consultable por deuda.
- Añadir zoom y acciones del sistema para compartir, copiar, guardar o exportar comprobantes.
- Evaluar migrar el almacenamiento JSON en SharedPreferences a Room, con migración segura de datos.
- Añadir pruebas unitarias y de interfaz.
- Verificar manejo de errores, accesibilidad, rotación y ciclo de vida.
- Preparar y probar una APK release firmada; la APK debug no equivale a una versión final de distribución.
- Configurar y probar automatización de sincronización, compilación e instalación. Actualmente no hay evidencia de que Android Studio obtenga y compile automáticamente cada cambio de GitHub.
- Configurar publicación de Releases de GitHub cuando exista una versión que se considere lista. La suscripción de notificaciones del usuario está configurada para Releases, no para cada commit.

## Regla de actualización

Cada vez que se complete una función, indicar qué archivo cambió, qué prueba se ejecutó y el resultado. Mover un elemento de pendiente a confirmado solo con evidencia del código y/o una prueba realizada.
