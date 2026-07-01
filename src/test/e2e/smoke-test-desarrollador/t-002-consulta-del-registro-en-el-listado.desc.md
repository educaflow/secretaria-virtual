---
type: test-e2e
id: T-002
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-06-30_11-18_smoke-test-desarrollador/test-e2e-desc/t-002-consulta-del-registro-en-el-listado.desc.md
     Test: T-002  |  Origen ESC: ESC-002
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-002 — Consulta del registro en el listado

**Origen ESC:** ESC-002
**Verifica:** —
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
3. **Y** pulsa «Añadir un nuevo smoke test», escribe «Prueba de humo 2» en «Texto» y pulsa «Guardar».
4. **Y** pulsa «Cancelar» para volver al listado.

## Resultado esperado
- El listado muestra una fila con el texto «Prueba de humo 2».
- La columna «Fecha de creación» de esa fila tiene un valor no vacío.
