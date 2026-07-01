---
type: test-e2e
id: T-001
---

# T-001 — Alta correcta: el servidor sella las fechas de creación y última modificación

**Origen ESC:** ESC-001
**Verifica:** R-SmokeTest-001, R-SmokeTest-002
**Pantalla principal:** screen-smoke-test.md
**Tipo:** happy

## Estado inicial de la base de datos

Estado previo del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- La tabla `SmokeTest` arranca vacía; cada test crea sus propios registros y (si procede) los borra al final o asume que la UI los filtra correctamente.
- No hay datos maestros de otros subsistemas de los que dependa este subsistema.

**Usuarios de acceso** (login y contraseña que `/sdd-debug-with-test-e2e-desc` usará para iniciar sesión):

| Login | Contraseña | Rol / Tipo | Centro |
|-------|------------|------------|--------|
| admin | admin | Administrador | — |

## Precondiciones
- Estado inicial de la base de datos (tabla vacía, usuario `admin`/`admin`).

## Pasos
1. **Dado** que el usuario `admin` inicia sesión con contraseña `admin`.
2. **Cuando** abre el menú «Desarrollador» → «Smoke test».
3. **Y** pulsa el botón «Añadir un nuevo smoke test».
4. **Y** escribe «Prueba de humo 1» en el campo «Texto».
5. **Y** pulsa el botón «Guardar».

## Resultado esperado
- El sistema guarda el registro y muestra el formulario con el texto «Prueba de humo 1».
- El campo «Fecha de creación» tiene un valor de fecha y hora no vacío igual o anterior al momento actual.
- El campo «Fecha de última modificación» tiene un valor de fecha y hora no vacío igual o anterior al momento actual.
- Ninguno de los dos campos de fecha fue introducido por el administrador.
