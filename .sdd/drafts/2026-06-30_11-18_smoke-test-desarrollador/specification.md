---
type: specification
---

# Smoke test

## Objetivo

Crear un **subsistema** nuevo llamado **smoke test** cuyo único fin es servir de prueba rápida: ofrece una tabla **sin valor de negocio** sobre la que el Administrador puede **crear, leer, modificar y borrar** registros para comprobar, de forma rápida y **sin riesgo en producción**, que la aplicación funciona y que se accede correctamente al servidor (el servidor sella cada registro con la fecha en que se crea y se modifica).

Como parte de esta iniciativa se crea además un **menú de primer nivel «Desarrollador»**, visible solo para el Administrador, que agrupa la opción de **Smoke test** y el menú ya existente de **Utilidades de PDF** (que deja de estar en primer nivel y pasa a colgar de «Desarrollador»).

No tiene dependencias funcionales de otros subsistemas de negocio: la tabla de smoke test no se relaciona con centros, usuarios ni expedientes.

## Actores

- **Administrador** — único actor de esta funcionalidad. Usa la pantalla de smoke test para verificar que la aplicación y el acceso al servidor funcionan. Tiene alcance global (no está limitado a un centro).

## Historias de usuario

## HU-001 — Como Administrador quiero crear, consultar, modificar y borrar registros de smoke test para comprobar rápidamente que la aplicación y el acceso al servidor funcionan

- ESC-001 — Alta correcta con fechas puestas por el servidor:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «Desarrollador» → «Smoke test» y pulsa «Nuevo».
  3. Escribe en el campo texto el valor «Prueba de humo 1» y pulsa «Guardar».
  4. El sistema guarda el registro, muestra el texto «Prueba de humo 1» y rellena automáticamente la fecha de creación y la fecha de última modificación con la fecha y hora actuales del servidor (el administrador no introdujo ninguna de las dos fechas).
- ESC-002 — Consulta del registro en el listado:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «Desarrollador» → «Smoke test», pulsa «Nuevo», escribe «Prueba de humo 2» en el campo texto y pulsa «Guardar».
  3. Vuelve al listado de smoke test.
  4. El sistema muestra en el listado una fila con el texto «Prueba de humo 2» y su fecha de creación.
- ESC-003 — Modificación que refresca la fecha de última modificación:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Crea un registro con el texto «Prueba de humo 3» y pulsa «Guardar»; anota la fecha de última modificación que muestra el sistema.
  3. Abre ese mismo registro, cambia el texto a «Prueba de humo 3 editada» y pulsa «Guardar».
  4. El sistema guarda el cambio: el texto pasa a «Prueba de humo 3 editada», la fecha de creación se mantiene igual que en el alta y la fecha de última modificación se actualiza a la fecha y hora actuales del servidor (igual o posterior a la anotada).
- ESC-004 — Borrado de un registro:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Crea un registro con el texto «Prueba de humo 4» y pulsa «Guardar».
  3. Selecciona ese registro en el listado, pulsa «Eliminar» y confirma el borrado.
  4. El sistema elimina el registro y deja de mostrarlo en el listado.
- ESC-005 — Alta sin texto rechazada por el servidor:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú «Desarrollador» → «Smoke test» y pulsa «Nuevo».
  3. Deja el campo texto vacío y pulsa «Guardar».
  4. El sistema no crea el registro y muestra el mensaje «El texto es obligatorio».

## Modelos

| Modelo | Qué representa |
|---|---|
| [SmokeTest](entity-SmokeTest.md) | Registro de prueba sin valor de negocio, con un texto y dos fechas selladas por el servidor. |

Relaciones entre modelos: no hay. `SmokeTest` es un modelo único e independiente, sin relaciones con otras entidades.

## Pantallas

| Pantalla | Para qué sirve y a quién |
|---|---|
| [Smoke test](screen-smoke-test.md) | Listado y formulario con los que el Administrador da de alta, consulta, modifica y borra los registros de prueba. |

## Seguridad

- **Administrador** — puede **crear, ver, modificar y borrar** todos los registros de smoke test. Alcance **global**: el smoke test no está asociado a ningún centro, por lo que ve y gestiona todos los registros. Es el **único** rol con acceso al subsistema.
- **Cambio de acceso en el menú de Utilidades de PDF** — al moverse bajo el nuevo menú «Desarrollador», las opciones de Utilidades de PDF (Información, Posiciones Firma, Posición Autofirma) quedan restringidas también **solo al Administrador**, perdiendo el acceso que tuvieran otros perfiles.

## Recursos y datos iniciales

*(no aplica)* — la tabla de smoke test arranca vacía; los propios escenarios crean y borran sus datos.

## Fuera de alcance

- El subsistema **no** representa ningún dato real: la tabla no se vincula a centros, usuarios ni expedientes y **no debe usarse para nada más que pruebas**.
- **No** se modifican las pantallas de Utilidades de PDF (Información, Posiciones Firma, Posición Autofirma): solo cambia su ubicación en el menú y su acceso.
- **No** hay asociación a centro ni filtrado multicentro para el smoke test.