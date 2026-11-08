# Requisitos funcionales de ADMIN

Este documento registra el comportamiento solicitado. Especificar un requisito aquí no significa que ya esté implementado; consultar `TECHNICAL_STATUS.md`.

## 1. Pantalla principal

- Aplicación vertical, en español, con diseño blanco, minimalista y botones celestes redondeados.
- Mostrar solamente cuatro tarjetas: ISUR, CAJA, BCP y KAELA.
- Cada tarjeta muestra el nombre, el importe de deuda pendiente y la etiqueta «Deuda pendiente».
- Mostrar por separado «DEUDA TOTAL», calculada como la suma de las deudas pendientes.
- No mostrar en la pantalla principal fechas, cuotas, historial, vouchers, gráficos ni información adicional.

## 2. Detalle de cada deuda

Cada tarjeta abre su propio detalle, sin mezclar datos entre deudas. Debe permitir configurar:
- Número de cuotas: entero positivo.
- Monto por cuota: importe en soles con dos decimales.
- Fecha inicial: selector de fecha.
- Botón Guardar.

Generar cuotas mensuales automáticamente. Conservar el día original siempre que exista; si un mes no tiene ese día, usar el último día de ese mes y volver al día original cuando sea posible. Ejemplo: 31 de enero → 28/29 de febrero → 31 de marzo → 30 de abril.

Cada cuota debe mostrar número, fecha, monto y estado.

## 3. Pago de una cuota y comprobante

- Una cuota pendiente muestra el botón «CANCELAR» y permite adjuntar un recibo o voucher.
- El diálogo de confirmación debe mostrar deuda, número de cuota, monto, opción para adjuntar comprobante, vista previa y confirmación.
- No permitir confirmar el pago sin una imagen. Mostrar exactamente: «Debes adjuntar el comprobante o voucher antes de confirmar el pago».
- Si falta el comprobante, mantener abierto el diálogo.
- Registrar cada pago una sola vez. Una cuota pagada no se puede volver a pagar.
- Guardar la imagen de forma persistente y asociarla exclusivamente al identificador de esa cuota.
- Al confirmar, actualizar la deuda pendiente, la deuda total, el historial y el estado de la cuota.
- La cuota pagada debe identificarse visualmente con texto tachado rojo fino.
- Al tocar una cuota pagada, abrir su comprobante. Permitir verlo a tamaño completo y con zoom, y compartirlo, copiarlo, guardarlo o exportarlo mediante las opciones disponibles de Android.
- Si no existe una imagen asociada, informar que no hay comprobante disponible.

## 4. Cálculos y dinero

- Mostrar importes en soles con dos decimales.
- Evitar errores de coma flotante; usar una representación exacta, por ejemplo centavos enteros o BigDecimal.
- Deuda original = número de cuotas × monto por cuota.
- Deuda pendiente = suma de los importes de las cuotas no pagadas.
- Deuda total = suma de las deudas pendientes.

## 5. Historial y persistencia

- Mantener historial por deuda y cuota: identificador, número, monto, fecha programada, estado y ruta o referencia al comprobante.
- No exigir que el usuario introduzca manualmente la fecha del pago.
- Los datos y comprobantes deben persistir después de cerrar o reiniciar la aplicación.
- Funcionamiento local/offline: sin inicio de sesión, servidor, anuncios ni suscripción.

## 6. Edición y protección de datos

- Rechazar campos vacíos, cero, valores negativos y datos inválidos.
- Evitar duplicados y cruces de información entre ISUR, CAJA, BCP y KAELA.
- Si se edita una deuda que ya tiene cuotas pagadas, proteger el historial y sus comprobantes.
- Advertir y pedir confirmación antes de cualquier cambio que pueda afectar cuotas ya pagadas.
- Nunca eliminar ni modificar pagos o comprobantes silenciosamente.

## 7. Arquitectura deseada

Separar interfaz, configuración de deuda, generación de cuotas, cálculos, registro de pagos, gestión de comprobantes, persistencia e historial. Room es la opción recomendada para la base de datos local; los comprobantes deben guardarse en almacenamiento interno persistente.

## 8. Pruebas necesarias

Crear y ejecutar pruebas para, como mínimo:
1. Pantalla principal con las cuatro deudas y total correcto.
2. Cálculo de deuda original y pendiente.
3. Generación de cuotas mensuales.
4. Fechas al final de mes y años bisiestos.
5. Validación de campos vacíos, cero, negativos y formatos incorrectos.
6. Pago bloqueado cuando no hay voucher.
7. Pago exitoso con voucher.
8. Imposibilidad de pagar dos veces la misma cuota.
9. Comprobante asociado a la cuota correcta.
10. Persistencia después de cerrar y volver a abrir la app.
11. Edición de una deuda con pagos existentes sin perder historial.
12. Independencia de los datos entre las cuatro deudas.
