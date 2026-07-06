# Modelo: Adjunto

Representa un fichero adjunto de un correo. Cada adjunto es una **copia independiente** del fichero original y pertenece a un único correo, junto al que se crea. Como un correo nunca se borra, sus adjuntos tampoco. Los adjuntos los añade el administrador al crear el correo y los descargan quienes pueden consultar el correo (administrador, supervisor del centro y destinatario).

## Campos

- **nombre del fichero** — el nombre con el que se ve y se descarga el adjunto.
- **contenido** — el fichero adjunto en sí (una copia independiente del original).
- **correo** — el correo al que pertenece el adjunto.

## Restricciones

- RES-Adjunto-001 — El nombre del fichero es único dentro de un mismo correo: un correo no puede tener dos adjuntos con el mismo nombre de fichero.

## Acción: Crear

**Input AllowProperties:** nombre del fichero, contenido, correo

**Validaciones:**

- VAL-Adjunto-001 — El correo al que pertenece el adjunto está indicado
  - mensaje: "El adjunto debe pertenecer a un correo"
- VAL-Adjunto-002 — El usuario tiene permiso sobre el centro del correo al que se añade el adjunto
  - actor: cualquier rol distinto de Administrador (el Administrador puede añadir adjuntos a correos de cualquier centro)
  - mensaje: "No puede añadir adjuntos a correos de un centro que no es suyo"
- VAL-Adjunto-003 — El correo al que pertenece el adjunto no está ya creado (los adjuntos solo se añaden durante el alta del correo)
  - mensaje: "No se pueden añadir adjuntos a un correo ya existente"
- VAL-Adjunto-004 — El nombre del fichero está indicado
  - mensaje: "El nombre del fichero es obligatorio"
- VAL-Adjunto-005 — El contenido del adjunto está indicado
  - mensaje: "Debe adjuntar el fichero"

## Acción: Modificar

**Input AllowProperties:** (ninguna — los adjuntos son inmutables una vez creados; el correo nunca se borra ni se edita)
