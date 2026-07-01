---
type: test-e2e
id: T-005
---

# T-005 — Alta sin texto rechazada con mensaje de error

**Origen ESC:** ESC-005
**Verifica:** V-SmokeTest-001
**Pantalla principal:** screen-smoke-test.md
**Tipo:** error

## Estado inicial de la base de datos

Estado previo del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- La tabla `SmokeTest` arranca vacía; cada test crea sus propios registros y (si procede) los borra al final o asume que la UI los filtra correctamente.
- No hay datos maestros de otros subsistemas de los que dependa este subsistema.

**Usuarios de acceso** (login y contraseña que `/sdd-debug-with-test-e2e-desc` usará para iniciar sesión):

| Login | Contraseña | Rol / Tipo | Centro |
|-------|------------|------------|--------|
| admin | admin | Administrador | — |

## Precondiciones
- Estado inicial de la base de datos.

## Pasos
1. **Dado** que el usuario `admin` inicia sesión con contraseña `admin`.
2. **Cuando** abre el menú «Desarrollador» → «Smoke test».
3. **Y** pulsa «Añadir un nuevo smoke test».
4. **Y** deja el campo «Texto» vacío.
5. **Y** pulsa el botón «Guardar».

## Resultado esperado
- El sistema muestra el mensaje «El texto es obligatorio».
- No se crea ningún registro nuevo en la base de datos.
