---
name: k-validaciones
description: Cómo escribir reglas de validación al analizar un modelo. Formato de regla, catálogo de tipos, redacción de mensajes, casos especiales (estados, campos calculados) y trazabilidad al diseño.
---

# k-validaciones

Cada validación se documenta como una **regla**:

| ID    | Campo(s)  | Tipo                  | Condición de aplicación     | Mensaje al usuario                                                     |
|-------|-----------|-----------------------|-----------------------------|------------------------------------------------------------------------|
| V-001 | email     | Formato               | Siempre                     | "El email debe tener el formato usuario@dominio.com"                   |
| V-002 | nif       | Formato + dígito ctrl | Siempre                     | "El NIF '{valor}' no es válido. Compruebe la letra verificadora"       |
| V-003 | fecha_fin | Consistencia temporal | Si fecha_inicio tiene valor | "La fecha de fin ({fin}) debe ser posterior a la de inicio ({inicio})" |
| V-004 | nif       | Unicidad              | Al guardar                  | "Ya existe una persona con el NIF {valor}"                             |

---

## Tipos de validación

**Sobre el propio campo** *(cliente — `action-validate` / `action-condition`)*
- Obligatoriedad — siempre / nunca / condicional
- Tipo de dato — entero, decimal, fecha, booleano, lista, referencia, archivo
- Longitud — mín / máx / exacta
- Formato — email, NIF, IBAN, teléfono… *(ver catálogo abajo)*
- Rango numérico — mín, máx, decimales, negativos
- Rango de fechas — mín/máx (absoluta o relativa), pasada/futura
- Dominio — lista cerrada / abierta / cascada / referencia a otra entidad
- Caracteres permitidos — solo dígitos, sin tildes, ASCII…
- Dígito de control

**Entre campos del mismo registro** *(cliente)*
- Consistencia temporal / numérica / de dominio
- Requerimiento mutuo — si hay X, también Y (AND)
- Alternativa requerida — al menos uno de {X, Y, …} (OR)
- Exclusión mutua — NIF y CIF no a la vez
- Totales cruzados — suma líneas = total
- Condicional — `SI tipo = JURIDICA ENTONCES CIF obligatorio`

**Entre registros — requiere BD** *(servidor — `validateInsert` / `validateUpdate`)*
- Unicidad — clave única en un ámbito (global / por centro / por año / combinación)
- Integridad referencial — al borrar el padre: RESTRICT (bloquea), CASCADE (borra hijos), SET NULL (deja huérfano)
- Cardinalidad — `1..*`, `0..1`, `N..M`; verificar al cambiar de estado
- Registros maestros — debe existir la configuración previa

**¿En qué modelo se documenta?** La regla pertenece al modelo en cuyo XML vive el campo o relación que dispara la validación:
- Integridad referencial al borrar (RESTRICT/CASCADE/SET NULL): se documenta en el **padre** (el que se borra), no en el hijo. *"No se puede borrar `FamiliaProfesional` con ciclos asociados"* es regla de `FamiliaProfesional`, aunque mencione `Ciclo`.
- Unicidad y formato: en el modelo que tiene el campo.
- Validaciones cruzadas entre dos entidades (coherencia centro↔expediente): en el modelo que dispara la operación; si hay duda, en el que tiene la responsabilidad funcional.

**De negocio** *(servidor)*
- Restricciones — "cliente con deudas no pide"
- Autorizaciones — "descuentos > 20% requieren director"
- Reglas temporales — "matrícula del 1 al 30 sept"
- Cálculos / derivaciones — *(ver "Campos calculados" abajo)*

Las reglas de servidor pueden duplicarse en cliente para mejor UX, pero **siempre** deben estar en servidor.

---

## El mensaje

Incluir el valor recibido y, en servidor con dominio finito, los valores válidos:

> "El alias '{alias}' no existe en el slot {slot}. Disponibles: {lista}."

| Mal | Bien |
|-----|------|
| "Campo obligatorio" | "Introduzca el nombre del solicitante" |
| "Formato inválido" | "El email debe tener el formato usuario@dominio.com" |
| "Valor fuera de rango" | "La cantidad debe estar entre 1 y 999" |
| "Error de consistencia" | "La fecha de fin (15/03/2024) no puede ser anterior a la de inicio (20/03/2024)" |
| "Registro duplicado" | "Ya existe un alumno con NIF '12345678Z'. ¿Desea ver su ficha?" |

Empezar por el campo o el valor (no por "Error:"). Sin tecnicismos. Sin culpar al usuario.

**El mensaje es para el usuario final.** Nada de jerga técnica ni referencias internas:

- ❌ "El asunto no puede superar 255 caracteres (longitud por defecto Axelor)" — el usuario no sabe qué es Axelor.
- ✅ "El asunto no puede superar 255 caracteres."

Notas para el implementador (origen del valor, default del framework, "ver issue X", "regla configurable") van en columnas auxiliares de la tabla o en notas al pie, **nunca** en el texto que verá el usuario.

## Reglas configurables vs constantes técnicas

Distinguir tres orígenes posibles de un valor en una regla:

- **Constante de negocio** — el negocio fija el número y no varía (ej. "DNI español tiene 8 dígitos + letra"). Va literal en la regla.
- **Parámetro de configuración** — el administrador puede cambiarlo en App Settings sin tocar código (ej. tamaño máximo de adjunto, lista de tipos MIME, ventana temporal de matrícula). El mensaje usa placeholder; la regla nombra el parámetro: *"Parámetro: `correos.anexos.tamañoMaxMB`, configurable por administrador en App Settings"*. En "Asunciones a confirmar" separar el valor por defecto propuesto (requiere confirmación) de la mecánica configurable (decisión de diseño).
- **Constante técnica** — la impone el formato, el protocolo o el ORM (ej. dimensión máxima de un PDF = 14400 puntos, longitud máxima de email RFC = 254, INTEGER de SQL = 2³¹−1). No es configurable y no se discute con el cliente. Documentarla como tal: *"Constante técnica del formato PDF; no procede configurar"*. Si se documenta como regla, el origen es **Catálogo** o **Modelo**, no Negocio.

No tratar como configurable lo que no es elegible. Si una regla menciona "valor por defecto X" pero X viene fijado por la tecnología, no es configurable: es una constante técnica que conviene declarar para que el implementador no se invente otra.

## Solape entre reglas agregadas y específicas

Si una regla "general" cubre lógicamente a otra "específica" (ej. *"el registro completo es inmutable tras el estado final"* hace innecesario *"la colección de hijos es inmutable tras el estado final"*), conservar **solo la general**. Una regla específica únicamente añade valor cuando dice algo que la general no dice (un mensaje distinto, una condición distinta, un campo permitido como excepción). Si se mantienen ambas, justificar la diferencia en una nota.

---

## Máquina de estados

Si la entidad tiene estados, además de las reglas habituales documentar:

- **Lista de estados** — cuál es el inicial y cuáles son finales.
- **Transiciones permitidas** — origen → destino, condición, rol, acción posterior (notificación, número, fecha…).
- **Campos editables por estado** — `E` editable, `R` solo lectura, `N` no visible, `Auto` calculado.
- **Validaciones que solo aplican en cierto estado** (ej. en `PENDIENTE` revisor ≠ solicitante).
- **Transiciones inválidas explícitas** y su mensaje.

Patrón típico: en `BORRADOR` se valida lo introducido; al `ENVIAR` se exige completitud, cruzadas y cardinalidad.

**Numeración única:** las reglas que dependen del estado (inmutabilidad tras un estado final, condiciones por estado) **comparten la misma secuencia `V-XXX`** que las del resto. La tabla principal mantiene la condición "Si estado = X" en su columna correspondiente. No abrir tablas paralelas con su propia numeración dentro de la sección de estados.

---

## Campos calculados

Para cada campo calculado documentar: **fórmula**, **dependencias**, **cuándo se recalcula** (tiempo real / al guardar / derivado del sistema), si es **editable manualmente**.

Cuidado con dependencias circulares (A depende de B y B depende de A): identificar cuál introduce el usuario y reformular.

---

## Catálogo de formatos españoles

| Campo | Formato | Ejemplo | Dígito de control |
|-------|---------|---------|-------------------|
| NIF | 8 dígitos + letra | `12345678Z` | módulo 23 → tabla TRWAGMYFPDXBNJZSQVHLCKE |
| NIE | X/Y/Z + 7 dígitos + letra | `X1234567L` | igual que NIF tras X→0/Y→1/Z→2 |
| CIF | letra + 7 dígitos + control | `A12345678` | letra o dígito según fórmula |
| IBAN ES | `ES` + 22 dígitos | `ES9121000418450200051332` | módulo 97 = 1 |
| Teléfono ES | 9 dígitos, empieza por 6/7/8/9 | `612345678` | — |
| Código postal ES | 5 dígitos (01000-52999) | `46001` | — |
| Matrícula actual ES | 4 dígitos + 3 letras consonantes | `1234 BCD` | — |
| NSS Seg. Social | 2 + 8 + 2 dígitos | `281234567840` | fórmula sobre los 10 primeros |
| Email | `texto@texto.dominio` | `usuario@empresa.com` | — |
| Fecha / Hora | `DD/MM/AAAA` / `HH:MM` | `15/03/2024` / `14:30` | — |

El analista indica que el campo tiene dígito de control; el implementador aplica el algoritmo.

---

## Origen de cada regla

Cada regla nace de uno de tres sitios. Marcar el origen en una columna o etiqueta evita mezclar lo que el modelo exige con lo que el analista supone:

- **Modelo** — derivada directa del XML/anotaciones (tipo, `required`, `unique`, `<many-to-one>`…). No requiere confirmación.
- **Catálogo** — formato/dígito de control del catálogo de abajo (NIF, IBAN, CP…). No requiere confirmación.
- **Negocio (asumida)** — el analista la deduce del dominio pero no está en el modelo (ej. "al menos un identificador entre DNI/NIA/NRP", "el cp debe coincidir con el del municipio"). **Marcar con `*` y listar al final en "Asunciones a confirmar"**.

Si una regla es Negocio asumida, el diseño no avanza hasta que el cliente la confirme o descarte.

## Lo que NO se documenta como validación

El framework ya lo cubre — no añade información:

- Que un `many-to-one` apunte a un registro existente (JPA lo garantiza).
- Que un campo `<integer>` no acepte texto, o `<date>` no acepte basura (parser del tipo).
- Longitudes por defecto del framework (Axelor `<string>` = 255) **salvo** que el negocio imponga un límite distinto. Si se documenta una longitud por defecto, indicar explícitamente "longitud por defecto Axelor".

Para `required="true"` y `unique="true"` declarados en el XML: **sí se documenta una regla**, porque el modelo solo dice *que* falla, no *qué mensaje* mostrar. La regla aporta el mensaje. No inventar una "obligatoriedad funcional" separada de la técnica: es **la misma regla**.

## Una regla, un campo, una cosa

- **No agrupar campos en una sola regla** ("introduzca CCAA, provincia y municipio") salvo que la condición sea genuinamente cruzada (requerimiento mutuo, exclusión). Si tres campos son cada uno obligatorio, son tres reglas — así el mensaje señala el campo concreto que falta.
- **No emitir reglas que se implican entre sí**. Si pides "rango 2000-2100" para un entero, no añadas también "longitud 4 dígitos": el rango ya lo implica. Una sola regla cubre ambos casos.
- **No partir una regla en cliente y servidor como si fueran dos reglas**. Es la misma regla; el documento de diseño decide dónde se ejecuta (ver "Trazabilidad").

## Modelos sin UI (infraestructura interna)

Algunos modelos no se editan por vista — solo los toca un servicio interno (numeradores, logs, contadores, semáforos, configuraciones de sistema). En esos casos:

- **No documentar reglas de cliente**: no hay vista que dispare `action-validate`. Cualquier mensaje "para el usuario final" es ficción.
- **Reformular las reglas como invariantes que el servicio debe garantizar**, no como mensajes de UX. El "mensaje" pasa a ser texto técnico de excepción/log para el desarrollador, redactado como invariante violado: *"El último número no puede decrecer: actual={anterior}, propuesto={nuevo}."*
- **Las reglas que aportan información son las que el XML no expresa por sí solo**: monotonía de un contador, inmutabilidad de la clave lógica, formato de un campo `String` que apunta lógicamente a otra entidad. Las que ya están en el XML (`required`, `unique-constraint`) se incluyen para fijar el mensaje técnico, no porque aporten una validación nueva.
- **Trazabilidad**: las reglas caen en el servicio (`FooService`) o en `validateInsert/Update` del repositorio. Nunca en cliente.

## Reglas vs no-reglas

La tabla `V-XXX` solo contiene reglas que **se aplicarán**. Hay tres cosas que confunden y no deben entrar como filas:

- **Decisiones de "esto NO se valida"** (ej. "se permite que solicitante e interesado coincidan"). No es regla: es ausencia de regla. Si es relevante dejarlo por escrito, va a "Asunciones a confirmar" o a una nota, no a la tabla con un mensaje vacío.
- **Comportamientos del sistema** (autogeneración de número, asignación automática del usuario desde sesión). No son reglas de validación; son lógica de creación. Si el negocio exige que el usuario no pueda alterarlos, *eso sí* es regla, y se redacta como **inmutabilidad / readonly**: el mensaje describe que el campo lo gestiona el sistema y rechazar el cambio, no la generación en sí.
- **Documentación del modelo** (qué significa el campo, para qué sirve). Va al análisis funcional, no a la tabla de reglas.

Para inmutabilidad / readonly: la regla se documenta una sola vez, con el mensaje del rechazo. La doble protección (vista `readonly` + servidor que rechaza cambios) es decisión de diseño, no son dos reglas.

## Ámbito de las reglas de unicidad

Toda regla de unicidad debe declarar su **ámbito** explícitamente. No basta con "unicidad" a secas. Posibilidades típicas:

- **Global** — único en todo el sistema (ej. DNI de persona física).
- **Por centro** — único dentro del centro (ej. código de aula dentro de un centro).
- **Por año / curso académico** — único dentro de un periodo.
- **Combinación** — único para la tupla (campo1, campo2, …).

El mensaje debe reflejar el ámbito: *"Ya existe un alumno con NIA '{valor}' en el centro {centro}"* es distinto de *"Ya existe una persona con NIA '{valor}'"*. Si el ámbito no es obvio del modelo, listarlo como asunción a confirmar.

## Trazabilidad: del análisis al diseño

- Cada regla `V-XXX` del análisis aparece en al menos un paso del diseño.
- Cada paso del diseño que implementa validaciones lista qué `V-XXX` cubre. Ejemplo: *"Paso 5 — `FooService.validateInsert`. Cubre V-002, V-004."*
- Antes de cerrar el diseño, construir la matriz `V-XXX → paso(s)`. Ninguna fila puede quedar vacía.
