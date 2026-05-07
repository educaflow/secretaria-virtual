---
name: k-validaciones/campo
description: Validaciones de campo individual — obligatoriedad condicional, tipos de dato, longitud, formatos habituales, rangos numéricos y de fechas, dominios, partición de equivalencia, valores límite, caracteres y dígito de control
---

# Validaciones de Campo Individual

## 1A. Obligatoriedad

Un campo puede ser:
- **Siempre obligatorio:** debe tener valor en cualquier circunstancia.
- **Siempre opcional:** nunca es obligatorio.
- **Condicionalmente obligatorio:** obligatorio solo si se cumple una condición.

La obligatoriedad condicional es la más frecuente y la que más se olvida documentar.

### Tabla de documentación de obligatoriedad

| Campo | Obligatorio | Condición de obligatoriedad |
|-------|-------------|----------------------------|
| Nombre | Siempre | — |
| Apellidos | Siempre | — |
| Email | Condicional | Obligatorio si Teléfono está vacío |
| Teléfono | Condicional | Obligatorio si Email está vacío |
| Motivo de baja | Condicional | Obligatorio si Estado = 'Baja' |
| Representante legal | Condicional | Obligatorio si el alumno es menor de edad |
| Observaciones | Nunca | — |

### Patrones frecuentes de obligatoriedad condicional

```
SI [campo_X] = [valor]       → [campo_Y] es obligatorio
SI [campo_X] está vacío      → [campo_Y] es obligatorio
SI [campo_X] está relleno    → [campo_Y] es obligatorio
SI [entidad] es menor de edad → [campo_Z] es obligatorio
SI [estado] es uno de [lista] → [campo_W] es de solo lectura (ya no editable)
```

---

## 1B. Tipo de dato

| Tipo | Descripción | Presentación en UI habitual |
|------|-------------|----------------------------|
| Texto libre | Cadena de caracteres | Campo de texto o área de texto |
| Número entero | Sin decimales | Campo numérico sin decimales |
| Número decimal | Con decimales | Campo numérico con N decimales |
| Monetario | Decimal con símbolo de moneda | Campo numérico con 2 decimales y símbolo |
| Fecha | Día/Mes/Año | Selector de fecha (date picker) |
| Hora | Hora:Minuto | Selector de hora |
| Fecha y hora | Fecha + hora | Selector combinado |
| Booleano | Sí/No, Verdadero/Falso | Checkbox o radio button |
| Lista cerrada | Valor de un conjunto fijo | Desplegable (select) |
| Lista abierta | Sugerencias + valor libre | Autocomplete |
| Referencia | ID de otro registro | Selector con búsqueda |
| Archivo | Documento adjunto | Control de subida de fichero |
| Imagen | Fichero de imagen | Control de imagen |

El tipo de dato condiciona el widget de entrada y previene la mayoría de errores de tipo. Cuando el tipo se presenta con el control correcto (fecha con date picker, lista con select), los errores de tipo prácticamente desaparecen.

---

## 1C. Longitud

| Variante | Especificación |
|----------|---------------|
| Longitud mínima | El campo debe tener al menos N caracteres |
| Longitud máxima | El campo no puede superar N caracteres |
| Longitud exacta | El campo debe tener exactamente N caracteres |

Documentar siempre tanto el mínimo como el máximo, aunque uno sea 0 o ilimitado. Los códigos con longitud exacta suelen tener también validación de dígito de control (ver 1I).

Longitudes exactas habituales:
- NIF/NIE: 9 caracteres
- CIF: 9 caracteres
- IBAN ES: 24 caracteres
- Código postal ES: 5 caracteres
- Teléfono ES: 9 dígitos
- Matrícula vehículo ES actual: 7 caracteres

---

## 1D. Formato / Patrón

El dato debe seguir una estructura concreta. El analista describe el patrón en lenguaje natural; el implementador construye la expresión regular o la lógica de validación.

### Formatos habituales en aplicaciones empresariales españolas

| Campo | Descripción del formato | Ejemplo válido | Ejemplo inválido |
|-------|------------------------|----------------|------------------|
| Email | `texto@texto.dominio`, sin espacios | `usuario@empresa.com` | `usuario empresa.com` |
| NIF | 8 dígitos seguidos de 1 letra verificadora | `12345678Z` | `1234567Z` |
| NIE | X/Y/Z + 7 dígitos + 1 letra verificadora | `X1234567L` | `X123456L` |
| CIF | 1 letra + 7 dígitos + 1 letra o dígito de control | `A12345678` | `12345678` |
| IBAN ES | `ES` + 2 dígitos de control + 20 dígitos | `ES9121000418450200051332` | `ES912100041845020005133` |
| Teléfono ES | 9 dígitos, empieza por 6/7/8/9 | `612345678` | `12345678` |
| Código postal ES | 5 dígitos, rango 01000-52999 | `46001` | `4600` |
| Matrícula actual ES | 4 dígitos + espacio + 3 letras consonantes (sin AEIOUÑQ) | `1234 BCD` | `1234 ABC1` |
| Matrícula antigua ES | 1-4 letras (provincia) + 4 dígitos + 2 letras | `V 1234 AB` | Depende del municipio |
| NSS (Seg. Social) | 2 dígitos provincia + 8 dígitos número + 2 dígitos control | `281234567840` | — |
| Fecha | `DD/MM/AAAA` | `15/03/2024` | `15-3-24` |
| Hora | `HH:MM` | `14:30` | `2:30 PM` |
| Latitud | Número decimal -90 a 90 | `39.4699` | `91.0` |
| URL | Empieza por `http://` o `https://` | `https://web.com` | `web.com` |
| Código SWIFT/BIC | 8 u 11 caracteres alfanuméricos | `CAIXESBBXXX` | `CAIX` |

---

## 1E. Rango numérico

Para cada campo numérico, especificar:

| Parámetro | Descripción | Ejemplo |
|-----------|-------------|---------|
| Valor mínimo | El menor valor permitido | 0 |
| Mínimo incluido | ¿El mínimo es válido o debe superarse? | Sí (>= 0) o No (> 0) |
| Valor máximo | El mayor valor permitido | 100 |
| Máximo incluido | ¿El máximo es válido o debe ser menor? | Sí (<= 100) |
| Decimales | Número de posiciones decimales | 2 |
| Negativos | ¿Se permiten valores negativos? | No |

### Tabla de rangos de ejemplo

| Campo | Min | Min incl. | Max | Max incl. | Decimales | Negativos |
|-------|-----|-----------|-----|-----------|-----------|-----------|
| Cantidad pedido | 1 | Sí | 9999 | Sí | 0 | No |
| Descuento % | 0 | Sí | 100 | Sí | 2 | No |
| Precio unitario | 0 | No | sin límite | — | 2 | No |
| Temperatura | -273.15 | No | sin límite | — | 2 | Sí |
| Edad | 0 | Sí | 150 | Sí | 0 | No |
| Nota académica | 0 | Sí | 10 | Sí | 2 | No |
| Porcentaje IVA | 0 | Sí | 100 | Sí | 0 | No |

### Partición de equivalencia para rangos numéricos

La técnica de partición de equivalencia divide los posibles valores en clases con el mismo comportamiento. Para el rango 1-100:

| Clase | Rango | Tipo | Comportamiento |
|-------|-------|------|----------------|
| CE1 | < 1 | Inválida | Rechazar con error |
| CE2 | 1 a 100 | Válida | Aceptar |
| CE3 | > 100 | Inválida | Rechazar con error |

### Análisis de valores límite

Los errores se concentran en los bordes. Documentar siempre:

| Punto | Valor | Tipo | Comportamiento |
|-------|-------|------|----------------|
| Mínimo - 1 | 0 | Inválida | Rechazar: "La cantidad debe ser al menos 1" |
| Mínimo | 1 | Válida | Aceptar |
| Valor nominal | 50 | Válida | Aceptar |
| Máximo | 100 | Válida | Aceptar |
| Máximo + 1 | 101 | Inválida | Rechazar: "La cantidad no puede superar 100" |

**Casos especiales que siempre hay que documentar:**
- ¿El cero es válido o es igual que vacío?
- ¿Null/vacío se trata como 0 o como error diferente?
- ¿Se permiten decimales aunque el tipo sea entero? (introduce ambigüedad: 1.5 ¿es 1 o error?)

---

## 1F. Rango de fechas

Para cada campo de fecha, especificar:

| Parámetro | Descripción |
|-----------|-------------|
| Fecha mínima | Puede ser absoluta (`01/01/1900`) o relativa (`hoy`, `fecha_nacimiento`, `+30 días`) |
| Fecha máxima | Puede ser absoluta o relativa |
| ¿Puede ser pasada? | Sí / No |
| ¿Puede ser futura? | Sí / No |
| ¿Se permiten festivos? | Sí / No / Advertencia |
| ¿Se permiten fines de semana? | Sí / No / Advertencia |

### Tabla de rangos de fechas de ejemplo

| Campo | Fecha mín | Fecha máx | Pasada | Futura | Festivos |
|-------|-----------|-----------|--------|--------|----------|
| Fecha de nacimiento | 01/01/1900 | Hoy | Sí | No | Sí |
| Fecha inicio contrato | Hoy | +10 años | No | Sí | Advertencia |
| Fecha fin contrato | Fecha inicio | Fecha inicio +50 años | No | Sí | Sí |
| Fecha de alta escolar | 01/09/año actual | 31/10/año actual | Sí | Sí | No |
| Fecha documento | -100 años | Hoy | Sí | No | Sí |
| Fecha reunión | Mañana | +1 año | No | Sí | No |

**Fechas especiales que hay que considerar siempre:**
- 29/02: solo existe en años bisiestos. ¿Qué pasa al introducir 29/02/2023?
- Días 29, 30, 31 de meses que no los tienen: ¿error o truncar al último día del mes?
- Cambio de hora (horario verano/invierno): ¿afecta a campos de fecha+hora?

---

## 1G. Dominio / Lista de valores permitidos

### Tipos de dominio

| Tipo | Descripción | Ejemplo |
|------|-------------|---------|
| **Enumerado cerrado** | Lista fija de valores; el usuario solo puede elegir de ella | Estado: Activo, Inactivo, Suspendido |
| **Enumerado abierto** | El sistema sugiere valores pero acepta texto libre | Profesión (con sugerencias pero editable) |
| **No enumerado** | El valor se define por una descripción, no por una lista | Cualquier número entre 0 y 100 |
| **De referencia** | Los valores válidos son registros de otra entidad | Campo "Alumno" → tabla de alumnos activos |

### Documentar dominios enumerados

Para cada valor del dominio, especificar:

| Código | Etiqueta en UI | Descripción | Estado inicial | Estado final | Visible para usuario |
|--------|---------------|-------------|---------------|-------------|---------------------|
| BOZ | Borrador | En redacción | Sí | No | Sí |
| PEN | Pendiente | Enviado, esperando revisión | No | No | Sí |
| APR | Aprobado | Revisado y aprobado | No | No | Sí |
| REC | Rechazado | Revisado y rechazado | No | Sí | Sí |
| ARC | Archivado | Proceso finalizado | No | Sí | Solo admins |

### Dominios dependientes (listas en cascada)

Cuando el dominio de un campo depende del valor de otro:
- "Los municipios disponibles se filtran por la provincia seleccionada"
- "Al cambiar la provincia, el campo municipio se limpia automáticamente"
- "Los centros disponibles dependen del tipo de usuario seleccionado"

Documentar siempre el comportamiento cuando cambia el campo padre: ¿se limpia el campo hijo? ¿se mantiene si sigue siendo válido?

---

## 1H. Caracteres permitidos

Más restrictivo que el formato: define qué caracteres individuales pueden aparecer.

| Restricción | Descripción | Caso de uso |
|-------------|-------------|-------------|
| Solo dígitos | 0-9 únicamente | Código postal, teléfono (solo el número) |
| Solo alfanumérico sin tildes | A-Z, a-z, 0-9 | Códigos para sistemas legacy sin soporte UTF-8 |
| Alfanumérico con tildes y ñ | Letras españolas completas | Nombres propios, localidades |
| Letras, espacios, guion, apóstrofe | Sin números ni otros especiales | Nombre de persona |
| Sin caracteres de control | Sin tabulador, salto de línea, etc. | Campos de una sola línea |
| Solo ASCII imprimible | 32-126 del código ASCII | Campos de contraseña, códigos de integración |

---

## 1I. Dígito de control

Algunos campos tienen un carácter verificador calculado a partir del resto del campo. Documentar el algoritmo o referenciar el estándar.

| Documento | Algoritmo de verificación |
|-----------|--------------------------|
| **NIF** | El número (8 dígitos) módulo 23 determina la letra según la tabla: TRWAGMYFPDXBNJZSQVHLCKE |
| **NIE** | X→0, Y→1, Z→2; luego mismo algoritmo que NIF |
| **CIF** | Suma de dígitos pares + función sobre impares; el resultado puede ser letra o dígito |
| **IBAN** | Trasladar los 4 primeros caracteres al final, convertir letras a números, verificar que módulo 97 = 1 |
| **EAN-13** | Suma ponderada de 12 dígitos (alternando peso 1 y 3), el 13º es el complemento a 10 |
| **Número tarjeta crédito** | Algoritmo de Luhn: duplicar dígitos en posiciones pares, sumar todos, múltiplo de 10 |
| **NSS (Seg. Social)** | Los 2 últimos dígitos se verifican con una fórmula sobre los 10 anteriores |
| **ISBN-13** | Misma fórmula que EAN-13 |

El analista no necesita conocer la fórmula exacta, pero sí debe indicar que el campo tiene dígito de control y cuál es el estándar (o a qué normativa referirse). El implementador aplicará el algoritmo correcto.
