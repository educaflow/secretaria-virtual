# Tests E2E

Tests concretos end-to-end materializados a partir de los escenarios (`ESC-NNN`) de las historias de usuario del `specification.md` y de las V/R/U del diseño.

Cada test es **independiente** (no depende del estado dejado por otro) y **trazable** (declara qué `ESC-NNN` materializa y qué V/R/U verifica). `/sdd-debug-with-test-e2e-desc` lo ejecuta contra la aplicación real tras la implementación (bucle de auto-corrección).

---

## Estado inicial de la base de datos

Estado previo (datos maestros gestionados por otros subsistemas) del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- La tabla de smoke test arranca **vacía** (el spec lo indica en "Recursos y datos iniciales": no hay datos precargados; los propios escenarios crean y borran sus datos).
- Existe el usuario administrador por defecto de la aplicación (grupo `admins`, alcance global). No se requiere ningún centro, usuario adicional, ni catálogo: `SmokeTest` no se relaciona con centros, usuarios ni expedientes.

**Usuarios de acceso** (login y contraseña que `/sdd-debug-with-test-e2e-desc` usará para iniciar sesión):

| Login | Contraseña | Rol / Tipo | Centro |
|---|---|---|---|
| admin | admin | Administrador (grupo `admins`, alcance global) | — |

---

## T-001 — Alta correcta con fechas selladas por el servidor

**Origen ESC:** ESC-001
**Verifica:** V-SmokeTest-001, R-SmokeTest-001, R-SmokeTest-002, U-smoke-test-001
**Pantalla principal:** screen-smoke-test.md
**Tipo:** happy

### Precondiciones
- El usuario `admin` ha iniciado sesión.
- Parte del "Estado inicial de la base de datos" (tabla de smoke test vacía).

### Pasos
1. **Dado** que el administrador está autenticado en la aplicación.
2. **Cuando** abre el menú "Desarrollador" → "Smoke test".
3. **Y** pulsa el botón "Nuevo".
4. **Y** escribe "Prueba de humo 1" en el campo "Texto".
5. **Y** deja "Fecha de creación" y "Fecha de última modificación" sin tocar (se muestran como solo lectura).
6. **Y** pulsa el botón "Guardar".

### Resultado esperado
- El registro se guarda y muestra el texto "Prueba de humo 1".
- "Fecha de creación" queda rellena automáticamente con la fecha y hora actuales del servidor (el administrador no la introdujo).
- "Fecha de última modificación" queda rellena automáticamente con la fecha y hora actuales del servidor (el administrador no la introdujo).
- Los campos de fecha se muestran como solo lectura.

---

## T-002 — Consulta del registro en el listado

**Origen ESC:** ESC-002
**Verifica:** R-SmokeTest-001
**Pantalla principal:** screen-smoke-test.md
**Tipo:** happy

### Precondiciones
- El usuario `admin` ha iniciado sesión.
- Parte del "Estado inicial de la base de datos".

### Pasos
1. **Dado** que el administrador está en el menú "Desarrollador" → "Smoke test".
2. **Cuando** pulsa "Nuevo", escribe "Prueba de humo 2" en el campo "Texto" y pulsa "Guardar".
3. **Y** vuelve al listado de smoke test.

### Resultado esperado
- El listado muestra una fila con el texto "Prueba de humo 2" y su "Fecha de creación".

---

## T-003 — Modificación que refresca la fecha de última modificación

**Origen ESC:** ESC-003
**Verifica:** R-SmokeTest-002, R-SmokeTest-001
**Pantalla principal:** screen-smoke-test.md
**Tipo:** happy

### Precondiciones
- El usuario `admin` ha iniciado sesión.
- Parte del "Estado inicial de la base de datos".

### Pasos
1. **Dado** que el administrador crea un registro con el texto "Prueba de humo 3" y pulsa "Guardar".
2. **Y** anota la "Fecha de última modificación" que muestra el sistema.
3. **Cuando** abre ese mismo registro desde el listado.
4. **Y** cambia el campo "Texto" a "Prueba de humo 3 editada".
5. **Y** pulsa "Guardar".

### Resultado esperado
- El texto pasa a "Prueba de humo 3 editada".
- La "Fecha de creación" se mantiene igual que en el alta (no cambia).
- La "Fecha de última modificación" se actualiza a la fecha y hora actuales del servidor (igual o posterior a la anotada).

---

## T-004 — Borrado de un registro

**Origen ESC:** ESC-004
**Verifica:** —
**Pantalla principal:** screen-smoke-test.md
**Tipo:** happy

### Precondiciones
- El usuario `admin` ha iniciado sesión.
- Parte del "Estado inicial de la base de datos".

### Pasos
1. **Dado** que el administrador crea un registro con el texto "Prueba de humo 4" y pulsa "Guardar".
2. **Y** vuelve al listado de smoke test.
3. **Cuando** selecciona la fila del registro "Prueba de humo 4".
4. **Y** pulsa "Eliminar".
5. **Y** confirma el borrado.

### Resultado esperado
- El registro "Prueba de humo 4" desaparece del listado.

---

## T-005 — Alta sin texto rechazada por el servidor

**Origen ESC:** ESC-005
**Verifica:** V-SmokeTest-001
**Pantalla principal:** screen-smoke-test.md
**Tipo:** error

### Precondiciones
- El usuario `admin` ha iniciado sesión.
- Parte del "Estado inicial de la base de datos".

### Pasos
1. **Dado** que el administrador está en el menú "Desarrollador" → "Smoke test".
2. **Cuando** pulsa "Nuevo".
3. **Y** deja el campo "Texto" vacío.
4. **Y** pulsa "Guardar".

### Resultado esperado
- El sistema no crea el registro.
- Se muestra el mensaje "El texto es obligatorio".
