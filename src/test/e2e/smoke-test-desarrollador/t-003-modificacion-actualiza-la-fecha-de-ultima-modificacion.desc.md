---
type: test-e2e
id: T-003
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-06-30_11-18_smoke-test-desarrollador/test-e2e-desc/t-003-modificacion-actualiza-la-fecha-de-ultima-modificacion.desc.md
     Test: T-003  |  Origen ESC: ESC-003
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-003 — Modificación actualiza la fecha de última modificación

**Origen ESC:** ESC-003
**Verifica:** R-SmokeTest-002
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
- Estado inicial de la base de datos.

## Pasos
1. **Dado** que el usuario `admin` inicia sesión con contraseña `admin`.
2. **Cuando** abre el menú «Desarrollador» → «Smoke test».
3. **Y** pulsa «Añadir un nuevo smoke test», escribe «Prueba de humo 3» en «Texto» y pulsa «Guardar».
4. **Y** anota el valor del campo «Fecha de última modificación» mostrado (llamémoslo `FechaMod1`).
5. **Y** cambia el campo «Texto» a «Prueba de humo 3 editada».
6. **Y** pulsa «Guardar».

## Resultado esperado
- El campo «Texto» muestra «Prueba de humo 3 editada».
- El campo «Fecha de creación» conserva el mismo valor que en el alta.
- El campo «Fecha de última modificación» tiene un valor igual o posterior a `FechaMod1`.
