# Modelo: Adjunto

Representa un fichero adjunto de un correo. Cada adjunto es una **copia independiente** del fichero original y pertenece a un único correo, junto al que se crea. Como un correo nunca se borra, sus adjuntos tampoco. Los adjuntos los añade el administrador al crear el correo y los descargan quienes pueden consultar el correo (administrador, supervisor del centro y destinatario).

## Campos

- **nombre del fichero** — el nombre con el que se ve y se descarga el adjunto.
- **contenido** — el fichero adjunto en sí (una copia independiente del original).
- **correo** — el correo al que pertenece el adjunto.

## Acción: Crear

**Input AllowProperties:** nombre del fichero, contenido, correo

**Validaciones:**

- VAL-009 — El correo al que pertenece el adjunto está indicado
  - mensaje: "El adjunto debe pertenecer a un correo"
- VAL-010 — El usuario tiene permiso sobre el centro del correo al que se añade el adjunto
  - actor: cualquier rol distinto de Administrador (el Administrador puede añadir adjuntos a correos de cualquier centro)
  - mensaje: "No puede añadir adjuntos a correos de un centro que no es suyo"

## Acción: Modificar

**Input AllowProperties:** (ninguna — los adjuntos son inmutables una vez creados; el correo nunca se borra ni se edita)
