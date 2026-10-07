---
type: specification
template: free
---

# Objetivo

Permitir al administrador publicar un aviso de mantenimiento que vean todos los usuarios de la aplicación, de todos los centros, en todas las pantallas, mientras esté activo.

# Contexto y alcance

**Por qué es una feature libre:** el aviso se muestra en el marco común de la aplicación (encima de cualquier pantalla), así que no pertenece a ningún sistema concreto ni es un expediente; toca la base común y la administración.

**Partes de la aplicación afectadas** (solo lo que cambia):

- **Marco común de todas las pantallas**: cuando hay un aviso activo, aparece un banner con su texto en la parte superior, en cualquier pantalla.
- **Administración**: una pantalla nueva «Avisos globales» donde el administrador crea, modifica y desactiva avisos.
- **Inicio de sesión**: no cambia; el aviso se ve después de entrar, nunca en la pantalla de acceso.

**Dependencias funcionales:** *(ninguna)*.

# Actores

- **Administrador**: crea, modifica y desactiva los avisos.
- **Cualquier usuario con sesión iniciada** (todos los tipos de usuario y cargos): ve el aviso activo.

# Historias de usuario

## HU-001 — Como Administrador quiero publicar un aviso de mantenimiento para que todos los usuarios lo vean antes de que ocurra

- ESC-001 — Publicar un aviso y que lo vea un usuario de otro centro:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla «Avisos globales» y pulsa «Nuevo».
  3. Rellena el texto con «Mantenimiento el viernes a las 20:00» y marca «Activo».
  4. Pulsa «Guardar» y cierra sesión.
  5. El profesor «profesor1@batoi.es» inicia sesión con contraseña «demo1234».
  6. El sistema muestra en la parte superior un banner con el texto «Mantenimiento el viernes a las 20:00».
  7. El profesor abre la pantalla «Mis firmas».
  8. El banner sigue visible.
  9. El profesor cierra sesión.
  10. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  11. Abre la pantalla «Avisos globales» y abre el aviso «Mantenimiento el viernes a las 20:00».
  12. Desmarca «Activo» y pulsa «Guardar».
- ESC-002 — No se puede guardar un aviso sin texto:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla «Avisos globales» y pulsa «Nuevo».
  3. Deja el texto vacío, marca «Activo» y pulsa «Guardar».
  4. El sistema no guarda y muestra «El texto del aviso es obligatorio».

## HU-002 — Como Administrador quiero desactivar un aviso para que deje de mostrarse cuando ya no haga falta

- ESC-003 — Un aviso desactivado no se muestra:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre la pantalla «Avisos globales» y pulsa «Nuevo».
  3. Rellena el texto con «Aviso de prueba desactivado» y deja «Activo» sin marcar.
  4. Pulsa «Guardar».
  5. Cierra sesión.
  6. El supervisor «supervisor1@mislata.es» inicia sesión con contraseña «demo1234».
  7. El sistema no muestra ningún banner con el texto «Aviso de prueba desactivado».

## HU-003 — Como usuario de la aplicación quiero que todo siga funcionando igual cuando no hay avisos para no notar la feature

- ESC-004 — Sin avisos activos no aparece nada (no-regresión):
  1. El alumno «alumno1@mislata.es» inicia sesión con contraseña «demo1234».
  2. El sistema muestra la pantalla de inicio sin ningún banner de aviso y con el mismo contenido que antes.
  3. El alumno abre la pantalla «Mis expedientes».
  4. La pantalla se muestra sin banner y funciona como siempre.

# Requisitos

- REQ-001 — Solo el administrador puede crear, modificar o desactivar un aviso global.
  - etiqueta: seguridad
- REQ-002 — Un aviso activo se muestra a todos los usuarios con sesión iniciada, de todos los centros, en todas las pantallas excepto la de inicio de sesión.
  - etiqueta: comportamiento
- REQ-003 — Si el texto del aviso está vacío al guardar, el sistema no guarda y avisa.
  - mensaje: «El texto del aviso es obligatorio»
- REQ-004 — El texto del aviso no supera los 500 caracteres.
  - mensaje: «El texto del aviso no puede superar 500 caracteres»
- REQ-005 — Si hay varios avisos activos se muestran todos, el más reciente primero.
  - etiqueta: comportamiento
- REQ-006 — Un usuario que ya tenía sesión iniciada ve el aviso nuevo (o deja de verlo) al cambiar de pantalla, sin tener que volver a entrar.
  - etiqueta: datos existentes
- REQ-007 — Sin ningún aviso activo, la aplicación se ve y funciona exactamente como antes de esta feature.
  - etiqueta: compatibilidad
- REQ-008 — El sistema guarda quién creó o modificó cada aviso y cuándo; esos datos los fija el sistema y no se envían desde la interfaz (`AllowProperties` de crear y modificar: solo texto y activo).
  - etiqueta: seguridad

# Seguridad

- **Administrador:** crea, modifica y desactiva avisos; alcance: todos los centros (los avisos son globales, no pertenecen a ningún centro).
- **Todos los usuarios con sesión iniciada:** ven los avisos activos; no pueden crearlos ni modificarlos.

# Recursos y datos iniciales

*(no aplica)*

# Fuera de alcance

- Avisos por centro: todos los avisos son globales.
- Programar la publicación y la caducidad por fecha: el administrador activa y desactiva a mano.
- Que el usuario pueda cerrar u ocultar el banner.
- Mostrar el aviso en la pantalla de inicio de sesión.
