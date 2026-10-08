# Modelo: Adjunto

Un fichero que viaja adjunto a un correo.
Pertenece siempre a un **Correo** (no a la notificación en general, porque el SMS no admite adjuntos) y es una copia independiente del fichero original.
Solo se añade dentro del formulario de alta de su correo, a la vez que el correo se crea; después no se modifica ni se borra.
Lo puede descargar quien puede ver su correo.

## Campos

- **nombre del fichero** — el nombre con el que el destinatario recibe el fichero
- **contenido** — el fichero en sí
- **correo** — el correo al que pertenece

## Restricciones

- RES-Adjunto-001 — El adjunto pertenece a un correo
  - mensaje: "El adjunto debe pertenecer a un correo"
- RES-Adjunto-002 — El nombre del fichero está indicado (un nombre solo con espacios cuenta como vacío)
  - mensaje: "El nombre del fichero es obligatorio"
- RES-Adjunto-003 — El contenido está indicado
  - mensaje: "El contenido del adjunto es obligatorio"
- RES-Adjunto-004 — Un adjunto ya creado no se modifica
  - mensaje: "El adjunto es inmutable tras su creación."
- RES-Adjunto-005 — Un adjunto no se borra nunca
  - mensaje: "Los adjuntos no se pueden borrar."

## Acción: Crear

**Input AllowProperties:** nombre del fichero, contenido, correo

**Validaciones:**

- VAL-Adjunto-001 — El correo al que pertenece es de un centro del usuario
  - condición: el correo está indicado y el usuario no es administrador
  - mensaje: "No puede añadir adjuntos a correos de un centro que no es suyo"
- VAL-Adjunto-002 — El correo al que pertenece se está dando de alta en esta misma operación (no se añaden adjuntos a un correo ya existente)
  - condición: el correo está indicado
  - mensaje: "No se pueden añadir adjuntos a un correo ya existente"
- VAL-Adjunto-003 — El fichero no supera los 10 MB
  - condición: el contenido está indicado
  - mensaje: "El adjunto no puede superar los 10 MB"
- VAL-Adjunto-004 — El nombre del fichero no contiene los caracteres «/» ni «\» ni caracteres de control
  - condición: el nombre del fichero está indicado
  - mensaje: "El nombre del fichero no puede contener los caracteres / \ ni caracteres de control"
- VAL-Adjunto-005 — El nombre del fichero no supera 255 caracteres
  - condición: el nombre del fichero está indicado
  - mensaje: "El nombre del fichero no puede superar 255 caracteres"
- VAL-Adjunto-006 — El nombre del fichero no es «.» ni «..»
  - condición: el nombre del fichero está indicado
  - mensaje: "El nombre del fichero no puede ser «.» ni «..»"
- VAL-Adjunto-007 — El fichero adjunto no está vacío (tiene al menos un byte)
  - condición: el contenido está indicado
  - mensaje: "El fichero adjunto está vacío"

**Reglas de negocio:**

- RN-Adjunto-001 — Al crear el adjunto se guarda una copia propia del fichero subido, de modo que lo que viaja con el correo y lo que luego se descarga no cambia aunque el fichero original se modifique o se elimine
  - fase: antes_de_commit
- RN-Adjunto-002 — Los adjuntos se guardan en la misma operación que el alta de su correo: si el correo no llega a crearse no queda guardado ninguno de sus adjuntos, y si alguno de los adjuntos no se puede guardar tampoco se crea el correo
  - fase: antes_de_commit
- RN-Adjunto-003 — Al descargar un adjunto, el fichero se entrega con el nombre del fichero indicado en el adjunto, no con el nombre que tenía el fichero al subirlo
  - fase: después_de_commit

## Acción: Modificar

**Input AllowProperties:** (ninguna — un adjunto es inmutable tras su creación)
