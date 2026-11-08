# Arquitectura y estructura

## Arquitectura actual observada

El código principal está concentrado actualmente en:

`app/src/main/java/com/example/admin/MainActivity.kt`

En ese archivo se encuentran:
- Modelos `Debt` e `Installment`.
- Actividad principal y composición de pantallas.
- Tarjetas de deuda y detalle.
- Diálogo de confirmación de pago.
- Selección y visualización de comprobantes.
- Generación de cuotas.
- Conversión y formato de importes.
- Copia de comprobantes al almacenamiento interno.
- Lectura y escritura de datos JSON en SharedPreferences.

## Tecnologías detectadas

- Kotlin.
- Android Jetpack Compose.
- Material 3.
- Android Activity Result API para seleccionar imágenes.
- `BigDecimal` y centavos enteros para importes.
- `SharedPreferences` + JSON para persistencia actual.
- Archivos internos en `filesDir/receipts` para comprobantes.

## Diseño objetivo

A medida que el proyecto crezca, separar responsabilidades en paquetes o archivos:
- `ui/`: pantallas, componentes y tema.
- `data/model/`: entidades de deuda, cuota y pago.
- `data/local/`: Room, DAO y migraciones.
- `domain/installments/`: generación de cuotas y reglas de calendario.
- `domain/money/`: cálculos y formato monetario.
- `domain/payments/`: reglas de pago y validaciones.
- `data/receipts/`: almacenamiento y recuperación de comprobantes.

No realizar una reorganización grande sin necesidad. Primero conservar el comportamiento existente, añadir pruebas y migrar datos de forma segura.

## Reglas de integridad

- Cada cuota debe tener un identificador estable.
- Cada comprobante debe quedar vinculado a una sola cuota.
- Un pago confirmado no debe duplicarse.
- Los cambios de configuración no deben destruir pagos históricos.
- Los cálculos monetarios deben ser exactos.
- Las migraciones de almacenamiento deben conservar los datos del usuario.
