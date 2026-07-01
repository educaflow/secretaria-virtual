# Tests E2E

Tests concretos end-to-end materializados a partir de los escenarios (`ESC-NNN`) de las historias de usuario del `specification.md` y de las V/R/U del diseño.

Cada test es **independiente** (no depende del estado dejado por otro) y **trazable** (declara qué `ESC-NNN` materializa y qué V/R/U verifica). `/sdd-debug-with-test-e2e-desc` lo ejecuta contra la aplicación real tras la implementación (bucle de auto-corrección).

---

## Estado inicial de la base de datos

Estado previo del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- La tabla `SmokeTest` arranca vacía; cada test crea sus propios registros y (si procede) los borra al final o asume que la UI los filtra correctamente.
- No hay datos maestros de otros subsistemas de los que dependa este subsistema.

**Usuarios de acceso** (login y contraseña que `/sdd-debug-with-test-e2e-desc` usará para iniciar sesión):

| Login | Contraseña | Rol / Tipo | Centro |
|-------|------------|------------|--------|
| admin | admin | Administrador | — |

---

## T-001 — Alta correcta: el servidor sella las fechas de creación y última modificación

**Origen ESC:** ESC-001
**Verifica:** R-SmokeTest-001, R-SmokeTest-002
**Pantalla principal:** screen-smoke-test.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos (tabla vacía, usuario `admin`/`admin`).

### Pasos
1. **Dado** que el usuario `admin` inicia sesión con contraseña `admin`.
2. **Cuando** abre el menú «Desarrollador» → «Smoke test».
3. **Y** pulsa el botón «Añadir un nuevo smoke test».
4. **Y** escribe «Prueba de humo 1» en el campo «Texto».
5. **Y** pulsa el botón «Guardar».

### Resultado esperado
- El sistema guarda el registro y muestra el formulario con el texto «Prueba de humo 1».
- El campo «Fecha de creación» tiene un valor de fecha y hora no vacío igual o anterior al momento actual.
- El campo «Fecha de última modificación» tiene un valor de fecha y hora no vacío igual o anterior al momento actual.
- Ninguno de los dos campos de fecha fue introducido por el administrador.

---

## T-002 — Consulta del registro en el listado

**Origen ESC:** ESC-002
**Verifica:** —
**Pantalla principal:** screen-smoke-test.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el usuario `admin` inicia sesión con contraseña `admin`.
2. **Cuando** abre el menú «Desarrollador» → «Smoke test».
3. **Y** pulsa «Añadir un nuevo smoke test», escribe «Prueba de humo 2» en «Texto» y pulsa «Guardar».
4. **Y** pulsa «Cancelar» para volver al listado.

### Resultado esperado
- El listado muestra una fila con el texto «Prueba de humo 2».
- La columna «Fecha de creación» de esa fila tiene un valor no vacío.

---

## T-003 — Modificación actualiza la fecha de última modificación

**Origen ESC:** ESC-003
**Verifica:** R-SmokeTest-002
**Pantalla principal:** screen-smoke-test.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el usuario `admin` inicia sesión con contraseña `admin`.
2. **Cuando** abre el menú «Desarrollador» → «Smoke test».
3. **Y** pulsa «Añadir un nuevo smoke test», escribe «Prueba de humo 3» en «Texto» y pulsa «Guardar».
4. **Y** anota el valor del campo «Fecha de última modificación» mostrado (llamémoslo `FechaMod1`).
5. **Y** cambia el campo «Texto» a «Prueba de humo 3 editada».
6. **Y** pulsa «Guardar».

### Resultado esperado
- El campo «Texto» muestra «Prueba de humo 3 editada».
- El campo «Fecha de creación» conserva el mismo valor que en el alta.
- El campo «Fecha de última modificación» tiene un valor igual o posterior a `FechaMod1`.

---

## T-004 — Borrado de un registro

**Origen ESC:** ESC-004
**Verifica:** —
**Pantalla principal:** screen-smoke-test.md
**Tipo:** happy

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el usuario `admin` inicia sesión con contraseña `admin`.
2. **Cuando** abre el menú «Desarrollador» → «Smoke test».
3. **Y** pulsa «Añadir un nuevo smoke test», escribe «Prueba de humo 4» en «Texto» y pulsa «Guardar».
4. **Y** pulsa «Cancelar» para volver al listado.
5. **Y** pulsa sobre la fila del registro «Prueba de humo 4» para abrir su formulario.
6. **Y** pulsa el botón «Borrar» y confirma el borrado.

### Resultado esperado
- El sistema elimina el registro y muestra el listado sin la fila «Prueba de humo 4».

---

## T-005 — Alta sin texto rechazada con mensaje de error

**Origen ESC:** ESC-005
**Verifica:** V-SmokeTest-001
**Pantalla principal:** screen-smoke-test.md
**Tipo:** error

### Precondiciones
- Estado inicial de la base de datos.

### Pasos
1. **Dado** que el usuario `admin` inicia sesión con contraseña `admin`.
2. **Cuando** abre el menú «Desarrollador» → «Smoke test».
3. **Y** pulsa «Añadir un nuevo smoke test».
4. **Y** deja el campo «Texto» vacío.
5. **Y** pulsa el botón «Guardar».

### Resultado esperado
- El sistema muestra el mensaje «El texto es obligatorio».
- No se crea ningún registro nuevo en la base de datos.
