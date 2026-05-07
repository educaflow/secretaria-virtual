---
name: k-validaciones/tipos
description: Taxonomía completa de los 5 niveles de validación en análisis funcional + distinción validación vs. regla de negocio + tabla de correspondencia tipo/dimensión/severidad
---

# Taxonomía de Validaciones

## Distinción fundamental: validación vs. regla de negocio

Esta distinción es crítica para el analista funcional:

**Validación:** función determinista y autocontenida. Dado el valor del campo (o de varios campos del mismo formulario), siempre produce el mismo resultado: válido/inválido. No depende del estado de la base de datos ni de otros registros.
- "El email debe tener formato válido" → validación
- "La fecha de fin debe ser posterior a la de inicio" → validación cruzada
- "El NIF no puede estar vacío" → validación de obligatoriedad

**Regla de negocio:** puede depender del contexto del sistema, de otros registros, del rol del usuario, del estado del proceso o de datos externos.
- "Un cliente no puede hacer un pedido si tiene deudas vencidas" → regla de negocio (necesita consultar otras facturas)
- "Solo el Director puede aprobar expedientes de más de 10.000€" → regla de autorización
- "No se pueden crear expedientes el 25 de diciembre" → regla temporal

En la práctica, ambas se documentan con el mismo nivel de detalle. La diferencia importa para saber cuándo se verifica: las validaciones se comprueban en el formulario; las reglas de negocio se comprueban al intentar una operación (guardar, enviar, aprobar).

---

## Nivel 1 — Validaciones de campo individual

Se aplican a un único campo, de forma independiente de los demás.

| Subtipo | Descripción | Ejemplo |
|---------|-------------|---------|
| **1A Obligatoriedad** | El campo debe tener valor (siempre, nunca o condicionalmente) | "Nombre es obligatorio"; "Motivo es obligatorio si Estado = Baja" |
| **1B Tipo de dato** | El valor debe ser del tipo correcto | "Edad debe ser un número entero"; "Fecha de alta debe ser una fecha" |
| **1C Longitud** | El valor debe tener entre N y M caracteres | "Nombre: 1-100 caracteres"; "Código postal: exactamente 5 dígitos" |
| **1D Formato/patrón** | El valor debe seguir una estructura concreta | "Email: usuario@dominio.extensión"; "NIF: 8 dígitos + 1 letra" |
| **1E Rango numérico** | El valor debe estar entre un mínimo y un máximo | "Descuento: 0% a 100%"; "Cantidad: mínimo 1" |
| **1F Rango de fechas** | La fecha debe estar dentro de un rango permitido | "Fecha nacimiento: no puede ser futura"; "Fecha contrato: solo a partir de hoy" |
| **1G Dominio/lista** | El valor debe pertenecer a un conjunto definido | "Estado: Activo, Inactivo, Suspendido"; "País: lista de países ISO" |
| **1H Caracteres permitidos** | Solo pueden usarse ciertos caracteres | "Código: solo alfanumérico sin tildes ni ñ"; "Nombre: letras, espacios, guion y apóstrofe" |
| **1I Dígito de control** | El valor contiene un dígito verificador calculado | "NIF: la letra se calcula con módulo 23"; "IBAN: dígitos de control ISO 13616" |

---

## Nivel 2 — Validaciones entre campos (cross-field)

La validez de un campo depende del valor de otro campo del mismo registro.

| Subtipo | Descripción | Ejemplo |
|---------|-------------|---------|
| **2A Consistencia temporal** | Una fecha debe ser anterior/posterior a otra | "Fecha fin >= Fecha inicio"; "Fecha incorporación > Fecha baja" |
| **2B Consistencia numérica** | Un número debe guardar relación matemática con otro | "Importe pagado <= Importe total"; "Descuento euros <= Precio total" |
| **2C Consistencia de dominio** | El valor de un campo filtra los valores válidos de otro | "El municipio debe pertenecer a la provincia seleccionada" |
| **2D Requerimiento/exclusión mutua** | La presencia de un campo implica obligatoriedad o exclusión de otro | "Si hay Fecha de fin, la Fecha de inicio es obligatoria"; "Solo puede seleccionarse un método de pago" |
| **2E Totales cruzados** | La suma de varios campos debe cuadrar con otro | "Suma de líneas = Total pedido"; "% partes = 100%" |

---

## Nivel 3 — Validaciones de integridad

Verifican la coherencia del registro respecto al sistema en su conjunto.

| Subtipo | Descripción | Ejemplo |
|---------|-------------|---------|
| **3A Unicidad** | El valor (o combinación de valores) no puede repetirse en el sistema | "NIF único global"; "Número expediente único por año y centro" |
| **3B Integridad referencial** | La referencia a otra entidad debe existir | "El alumno seleccionado debe estar dado de alta"; "El curso debe existir en el catálogo" |
| **3C Cardinalidad** | El número de registros relacionados debe estar dentro de un rango | "Un expediente debe tener al menos 1 documento para ser enviado"; "Un alumno no puede estar matriculado dos veces en la misma asignatura" |
| **3D Registros maestros** | Deben existir los datos de configuración necesarios para operar | "No se puede crear un expediente si no existe configurado el tipo de tramitación" |

---

## Nivel 4 — Validaciones de estado y ciclo de vida

Se aplican en función del estado actual del registro o de la transición que se está intentando.

| Subtipo | Descripción | Ejemplo |
|---------|-------------|---------|
| **4A Transiciones válidas** | Solo pueden realizarse ciertas transiciones entre estados | "Un expediente Aprobado no puede volver a Borrador"; "Solo se puede Rechazar desde Pendiente de revisión" |
| **4B Campos editables por estado** | Algunos campos solo son editables en ciertos estados | "La descripción es de solo lectura una vez enviado el expediente"; "El motivo de rechazo solo puede editarse en estado Pendiente" |
| **4C Validaciones propias del estado** | Cada estado tiene sus propias reglas de completitud | "En Borrador se puede guardar incompleto; al Enviar todos los campos obligatorios deben estar rellenos" |

---

## Nivel 5 — Reglas de negocio como validaciones

Restricciones, cálculos y derivaciones que expresan política del negocio.

| Subtipo | Descripción | Ejemplo |
|---------|-------------|---------|
| **5A Restricciones** | Condiciones que el sistema no debe permitir violar | "Un cliente con deudas vencidas no puede hacer nuevos pedidos"; "No se puede autorizar más del presupuesto asignado" |
| **5B Cálculos/derivaciones** | El valor de un campo se deriva de otros mediante una fórmula | "Total = suma de líneas con IVA"; "Edad = años entre fecha_nacimiento y hoy" |
| **5C Autorizaciones** | Ciertos valores o acciones requieren aprobación | "Descuentos > 20% requieren autorización del director"; "Pedidos > 10.000€ requieren dos firmas" |
| **5D Reglas temporales** | La validez de una operación depende de la fecha o periodo | "Solo se puede matricular entre el 1 y el 30 de septiembre"; "No se pueden anular facturas del año anterior después del 31 de enero" |

---

## Tabla de correspondencia

| Tipo de validación | Dimensión de calidad que protege | Severidad habitual | Verificación |
|-------------------|----------------------------------|-------------------|-------------|
| 1A Obligatoriedad | Completitud | Error bloqueante | En formulario |
| 1B Tipo de dato | Exactitud | Error bloqueante | En formulario |
| 1C Longitud | Exactitud | Error bloqueante | En formulario |
| 1D Formato/patrón | Exactitud | Error bloqueante | En formulario |
| 1E Rango numérico | Exactitud, Validez | Error bloqueante | En formulario |
| 1F Rango de fechas | Exactitud, Validez | Error bloqueante | En formulario |
| 1G Dominio/lista | Validez | Error bloqueante | En formulario |
| 1H Caracteres | Exactitud | Error bloqueante | En formulario |
| 1I Dígito de control | Exactitud | Error bloqueante | En formulario |
| 2A-2E Cruzadas | Consistencia | Error bloqueante | Al guardar o en tiempo real |
| 3A Unicidad | Unicidad | Error bloqueante | Al guardar |
| 3B Integridad referencial | Integridad | Error bloqueante | Al guardar |
| 3C Cardinalidad | Integridad | Error bloqueante o advertencia | Al cambiar estado |
| 3D Registros maestros | Integridad | Error bloqueante | Al intentar operar |
| 4A Transiciones | Validez | Error bloqueante | Al intentar la transición |
| 4B Campos editables | Validez | Informativa (campo bloqueado) | Siempre visible |
| 4C Por estado | Completitud, Validez | Error bloqueante | Al intentar la transición |
| 5A Restricciones | Validez | Error bloqueante o advertencia | Al intentar la operación |
| 5B Cálculos | Exactitud | No aplica (es automático) | En tiempo real |
| 5C Autorizaciones | Validez | Advertencia con flujo de aprobación | Al intentar la operación |
| 5D Reglas temporales | Validez | Error bloqueante | Al intentar la operación |
