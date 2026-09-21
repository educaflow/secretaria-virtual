# Tests E2E

Tests concretos end-to-end materializados a partir de los escenarios (`ESC-NNN`) de las historias de usuario del `specification.md` y de las V/R/U del diseño.

Cada test es **independiente** (no depende del estado dejado por otro) y **trazable** (declara qué `ESC-NNN` materializa y qué V/R/U verifica). `/sdd-debug-with-test-e2e-desc` lo ejecuta contra la aplicación real tras la implementación (bucle de auto-corrección).

> **En esta iniciativa estos tests son solo descriptivos.** Las guías de diseño prohíben generarlos y ejecutarlos: los perfiles de un usuario salen de `PerfilesUsuarioServiceImpl`, que aún no está terminada, así que **MUST NOT** lanzarse `/sdd-debug-with-test-e2e-desc` ni `/sdd-create-tests-e2e` sobre este fichero hasta que lo esté (ver `decisiones.md`, D6).
> La numeración arranca en `T-001` porque `src/test/e2e/system/expedientes/` no existe todavía.

---

## Estado inicial de la base de datos

Estado previo (datos maestros gestionados por otros subsistemas) del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- Centros «CIPFP Mislata» y «CIPFP Batoi».
- Trámite «Anulación de matrícula en ciclo formativo», que **no** admite presentar en representación, con su texto de ayuda y su tipo de expediente vigente `AnulacionMatriculaCicloFormativoV1`, cuya primera fase es «Solicitud de anulación» y cuyo primer estado es «Datos de la solicitud».
- Trámite «Justificación de falta del profesorado», con su texto de ayuda y su tipo de expediente vigente, que **no** admite presentar en representación.
- Los tipos de expediente y los trámites los registra la aplicación al arrancar (spec, «Recursos y datos iniciales»).
- Perfiles concedidos sobre los trámites (los usuarios de la tabla):
  - perfil de creador sobre «Anulación de matrícula en ciclo formativo» para los alumnos en sus centros (en CIPFP Mislata a `alumnofamiliar@mislata.es` por dos vías: como alumno y como familiar);
  - perfil de creador sobre «Justificación de falta del profesorado» para `vicedirector@mislata.es` en CIPFP Mislata;
  - perfil de tramitador (y no el de creador) sobre «Anulación de matrícula en ciclo formativo» para `supervisor1@mislata.es` en CIPFP Mislata;
  - ningún perfil de creador ni de tramitador sobre «Anulación de matrícula en ciclo formativo» para `director@mislata.es`, que sí ve el trámite.
- Ningún expediente de «Anulación de matrícula en ciclo formativo» pendiente para los usuarios de la tabla.

**Usuarios de acceso** (login y contraseña que `/sdd-debug-with-test-e2e-desc` usará para iniciar sesión):

| Login | Contraseña | Rol / Tipo | Centro |
|---|---|---|---|
| alumno1@mislata.es | demo1234 | Alumno (perfil de creador) | CIPFP Mislata |
| alumnodoscentros@mislata.es | demo1234 | Alumno (perfil de creador en los dos centros) | CIPFP Batoi y CIPFP Mislata |
| alumno1@batoi.es | demo1234 | Alumno (perfil de creador) | CIPFP Batoi |
| alumnofamiliar@mislata.es | demo1234 | Alumno y Familiar (perfil de creador por las dos vías) | CIPFP Mislata |
| vicedirector@mislata.es | demo1234 | Profesor (perfil de creador en «Justificación de falta del profesorado») | CIPFP Mislata |
| supervisor1@mislata.es | demo1234 | Supervisor (solo perfil de tramitador) | CIPFP Mislata |
| director@mislata.es | demo1234 | Profesor con cargo Director (sin perfil de inicio) | CIPFP Mislata |
| admin | admin | Administrador | — |

---

## T-001 — Alta con un único centro, sin ninguna pregunta

**Origen ESC:** ESC-001
**Verifica:** V-NuevoExpediente-001, V-NuevoExpediente-002, R-NuevoExpediente-001, R-NuevoExpediente-002, R-NuevoExpediente-003, R-NuevoExpediente-004, R-NuevoExpediente-005, R-ContextoTramitacion-001, U-nuevo-expediente-001, U-nuevo-expediente-003, U-nuevo-expediente-004, U-nuevo-expediente-006, U-nuevo-expediente-011, U-nuevo-expediente-012
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el alumno inicia sesión con usuario «alumno1@mislata.es» y contraseña «demo1234».
2. **Cuando** abre el menú Expedientes → Trámites y elige el trámite «Anulación de matrícula en ciclo formativo».
3. **Entonces** se abre la ventana «Nuevo expediente» con el nombre del trámite «Anulación de matrícula en ciclo formativo» y su texto de ayuda en la cabecera.
4. **Y** el campo centro muestra «CIPFP Mislata» y no se puede cambiar.
5. **Y** no se muestran el interruptor «Presentado el expediente a partir de un documento en papel» ni la pregunta «¿Para quién es el expediente?».
6. **Cuando** pulsa «Crear expediente».

### Resultado esperado
- La ventana «Nuevo expediente» se cierra.
- Se abre el expediente recién creado de «Anulación de matrícula en ciclo formativo» en su primer estado, en el centro «CIPFP Mislata», presentado telemáticamente y para el propio alumno.

---

## T-002 — Alta eligiendo entre dos centros

**Origen ESC:** ESC-002
**Verifica:** R-NuevoExpediente-003, U-nuevo-expediente-002, U-nuevo-expediente-003, U-nuevo-expediente-004, U-nuevo-expediente-015
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el alumno inicia sesión con usuario «alumnodoscentros@mislata.es» y contraseña «demo1234».
2. **Cuando** abre el menú Expedientes → Trámites y elige el trámite «Anulación de matrícula en ciclo formativo».
3. **Entonces** se abre la ventana «Nuevo expediente» con el campo centro vacío y editable.
4. **Cuando** despliega el campo centro.
5. **Entonces** se ofrecen únicamente «CIPFP Batoi» y «CIPFP Mislata», en ese orden.
6. **Cuando** elige «CIPFP Batoi» y pulsa «Crear expediente».

### Resultado esperado
- La ventana «Nuevo expediente» se cierra.
- Se abre el expediente recién creado en su primer estado, en el centro «CIPFP Batoi».

---

## T-003 — Intento de alta sin indicar el centro

**Origen ESC:** ESC-003
**Verifica:** U-nuevo-expediente-005
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el alumno inicia sesión con usuario «alumnodoscentros@mislata.es» y contraseña «demo1234».
2. **Cuando** abre el menú Expedientes → Trámites y elige el trámite «Anulación de matrícula en ciclo formativo».
3. **Y** sin elegir ningún centro pulsa «Crear expediente».

### Resultado esperado
- No se crea ningún expediente.
- La ventana «Nuevo expediente» sigue abierta y el campo centro aparece señalado como obligatorio.

---

## T-004 — Cancelar no crea nada

**Origen ESC:** ESC-004
**Verifica:** U-nuevo-expediente-018
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** UI

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el alumno inicia sesión con usuario «alumno1@mislata.es» y contraseña «demo1234».
2. **Cuando** abre el menú Expedientes → Trámites y elige el trámite «Anulación de matrícula en ciclo formativo».
3. **Y** en la ventana «Nuevo expediente» pulsa «Cancelar».
4. **Entonces** la ventana se cierra sin preguntar nada.
5. **Cuando** abre el menú Expedientes → Expedientes Pendientes.

### Resultado esperado
- No aparece ningún expediente de «Anulación de matrícula en ciclo formativo».

---

## T-005 — Alta como tramitador, deducida sin preguntar

**Origen ESC:** ESC-005
**Verifica:** R-NuevoExpediente-001, R-NuevoExpediente-004, R-NuevoExpediente-005, R-ContextoTramitacion-001, V-NuevoExpediente-002, U-nuevo-expediente-003, U-nuevo-expediente-006, U-nuevo-expediente-007, U-nuevo-expediente-011, U-nuevo-expediente-012
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
2. **Cuando** abre el menú Expedientes → Trámites y elige el trámite «Anulación de matrícula en ciclo formativo».
3. **Entonces** se abre la ventana «Nuevo expediente» con el centro «CIPFP Mislata» ya elegido y sin posibilidad de cambiarlo.
4. **Y** no se muestran el interruptor «Presentado el expediente a partir de un documento en papel» ni la pregunta de para quién es.
5. **Cuando** pulsa «Crear expediente».

### Resultado esperado
- La ventana «Nuevo expediente» se cierra.
- Se abre el expediente recién creado en su primer estado, en el centro «CIPFP Mislata», marcado como presentado en papel.

---

## T-006 — Usuario que ve el trámite pero no puede iniciarlo

**Origen ESC:** ESC-006
**Verifica:** V-NuevoExpediente-001
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el director inicia sesión con usuario «director@mislata.es» y contraseña «demo1234».
2. **Cuando** abre el menú Expedientes → Trámites y elige el trámite «Anulación de matrícula en ciclo formativo».

### Resultado esperado
- El sistema muestra el error «No puede crear expedientes de este trámite en ninguno de sus centros».
- No se crea ningún expediente.

---

## T-007 — Consulta de tipos de expediente en solo lectura

**Origen ESC:** ESC-007
**Verifica:** —
**Pantalla principal:** screen-tipos-de-expediente.md
**Tipo:** UI

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el administrador inicia sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú Expedientes → Tipos Expedientes.
3. **Entonces** ve el listado de tipos de expediente con las columnas código, nombre y trámite, sin botón «Nuevo».
4. **Cuando** pulsa la fila cuyo código es «AnulacionMatriculaCicloFormativoV1».

### Resultado esperado
- Se abre el formulario con el código, el nombre y el trámite en solo lectura.
- No hay botones de guardar ni de borrar.

---

## T-008 — Quien no es administrador no ve la pantalla de tipos

**Origen ESC:** ESC-008
**Verifica:** —
**Pantalla principal:** screen-tipos-de-expediente.md
**Tipo:** UI

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
2. **Cuando** despliega el menú Expedientes.

### Resultado esperado
- No aparece la opción «Tipos Expedientes».

---

## T-009 — El expediente recién creado aparece en «Expedientes Pendientes» y se puede reabrir

**Origen ESC:** ESC-009
**Verifica:** R-NuevoExpediente-001, V-NuevoExpediente-002
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el alumno inicia sesión con usuario «alumno1@mislata.es» y contraseña «demo1234».
2. **Cuando** abre el menú Expedientes → Trámites y elige el trámite «Anulación de matrícula en ciclo formativo».
3. **Y** en la ventana «Nuevo expediente», con el centro «CIPFP Mislata» ya elegido, pulsa «Crear expediente».
4. **Entonces** la ventana se cierra y se abre el expediente recién creado en su primer estado.
5. **Cuando** abre el menú Expedientes → Expedientes Pendientes.
6. **Entonces** aparece una única fila del trámite «Anulación de matrícula en ciclo formativo», en la fase «Solicitud de anulación» y en el estado «Datos de la solicitud».
7. **Cuando** pulsa esa fila.

### Resultado esperado
- Se abre el mismo expediente, en el estado «Datos de la solicitud» y en el centro «CIPFP Mislata».

---

## T-010 — Un alumno de otro centro solo puede crear el expediente en su centro

**Origen ESC:** ESC-010
**Verifica:** R-NuevoExpediente-003, U-nuevo-expediente-002, U-nuevo-expediente-003, U-nuevo-expediente-004
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el alumno inicia sesión con usuario «alumno1@batoi.es» y contraseña «demo1234».
2. **Cuando** abre el menú Expedientes → Trámites y elige el trámite «Anulación de matrícula en ciclo formativo».
3. **Entonces** se abre la ventana «Nuevo expediente» con el centro «CIPFP Batoi» ya elegido y sin posibilidad de cambiarlo.
4. **Y** «CIPFP Mislata» no se ofrece en ningún momento.
5. **Cuando** pulsa «Crear expediente».

### Resultado esperado
- La ventana «Nuevo expediente» se cierra.
- Se abre el expediente recién creado en su primer estado, en el centro «CIPFP Batoi».

---

## T-011 — Dos tipos de usuario en el mismo centro: el centro aparece una sola vez y no se pregunta nada

**Origen ESC:** ESC-011
**Verifica:** R-NuevoExpediente-003, R-NuevoExpediente-004, R-NuevoExpediente-005, U-nuevo-expediente-003, U-nuevo-expediente-004, U-nuevo-expediente-006, U-nuevo-expediente-011
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el usuario inicia sesión con usuario «alumnofamiliar@mislata.es» y contraseña «demo1234».
2. **Cuando** abre el menú Expedientes → Trámites y elige el trámite «Anulación de matrícula en ciclo formativo».
3. **Entonces** se abre la ventana «Nuevo expediente» con el centro «CIPFP Mislata» ya elegido y sin posibilidad de cambiarlo.
4. **Y** no se muestra el interruptor «Presentado el expediente a partir de un documento en papel».
5. **Y** no se muestra la pregunta «¿Para quién es el expediente?».
6. **Cuando** pulsa «Crear expediente».

### Resultado esperado
- La ventana «Nuevo expediente» se cierra.
- Se abre el expediente recién creado en su primer estado, en el centro «CIPFP Mislata», presentado telemáticamente y para el propio usuario.

---

## T-012 — Cambiar de centro antes de crear: el expediente se crea en el último centro elegido

**Origen ESC:** ESC-012
**Verifica:** R-NuevoExpediente-004, R-NuevoExpediente-005, U-nuevo-expediente-006, U-nuevo-expediente-007, U-nuevo-expediente-008, U-nuevo-expediente-011, U-nuevo-expediente-012
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el alumno inicia sesión con usuario «alumnodoscentros@mislata.es» y contraseña «demo1234».
2. **Cuando** abre el menú Expedientes → Trámites y elige el trámite «Anulación de matrícula en ciclo formativo».
3. **Y** en la ventana «Nuevo expediente» elige el centro «CIPFP Batoi».
4. **Entonces** siguen sin mostrarse el interruptor «Presentado el expediente a partir de un documento en papel» y la pregunta «¿Para quién es el expediente?».
5. **Cuando** cambia el centro a «CIPFP Mislata».
6. **Entonces** siguen sin mostrarse el interruptor y la pregunta.
7. **Cuando** pulsa «Crear expediente».

### Resultado esperado
- La ventana «Nuevo expediente» se cierra.
- Se abre el expediente recién creado en su primer estado, en el centro «CIPFP Mislata», presentado telemáticamente y para el propio alumno.

---

## T-013 — Alta de otro trámite: la ventana muestra el nombre y la ayuda del trámite elegido

**Origen ESC:** ESC-013
**Verifica:** R-NuevoExpediente-002, U-nuevo-expediente-001, U-nuevo-expediente-014, U-nuevo-expediente-017
**Pantalla principal:** screen-nuevo-expediente.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el profesor inicia sesión con usuario «vicedirector@mislata.es» y contraseña «demo1234».
2. **Cuando** abre el menú Expedientes → Trámites y elige el trámite «Justificación de falta del profesorado».
3. **Entonces** se abre la ventana «Nuevo expediente» con el nombre del trámite «Justificación de falta del profesorado» en la cabecera y, debajo, su texto de ayuda.
4. **Y** el centro «CIPFP Mislata» aparece ya elegido y sin posibilidad de cambiarlo.
5. **Y** no se muestran el interruptor «Presentado el expediente a partir de un documento en papel» ni la pregunta «¿Para quién es el expediente?».
6. **Cuando** pulsa «Crear expediente».

### Resultado esperado
- La ventana «Nuevo expediente» se cierra.
- Se abre el expediente recién creado de «Justificación de falta del profesorado» en su primer estado, en el centro «CIPFP Mislata», presentado telemáticamente y para el propio profesor.
