# Pantalla: Adjunto en consulta

## Identidad

- **Quién la usa:** cualquiera que pueda ver el correo al que pertenece el adjunto (el Administrador, el gestor del centro o el destinatario), en lectura.
- **Qué muestra:** un adjunto de un correo ya guardado, en solo lectura, para ver su nombre y descargar el fichero.

## Menú

- No cuelga de ningún menú: se abre al pulsar una fila del listado de adjuntos de un correo ya guardado en las pantallas «Todas las notificaciones», «Notificaciones del centro» y «Notificaciones recibidas».

## Estructura jerárquica de las vistas

```
Formulario de adjunto en consulta
```

---

## Vista: Formulario de adjunto en consulta

- **Slug:** formulario
- **Tipo:** formulario
- **Qué muestra:** el nombre del fichero y el fichero del adjunto, en lectura.
- **Se abre desde:** el listado de adjuntos de un correo ya guardado, al pulsar una fila.

### Propiedades

- **Modo:** siempre de solo lectura.

### Paneles

- **Adjunto** (normal) — nombre del fichero, contenido (al pulsarlo se descarga el fichero)
- **Botones** (botonera) — Salir

### Botones

- **Salir** — Cierra el adjunto y vuelve al correo.
- Sin Guardar ni Borrar: un adjunto no se modifica ni se borra nunca.
