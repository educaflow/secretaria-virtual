---
type: test-e2e
id: T-006
---

# T-006 — Borrar una entrada deshabilitada sigue funcionando

**Origen ESC:** ESC-006
**Verifica:** —
**Pantalla principal:** screen-certificados-digitales.md
**Tipo:** happy

## Estado inicial de la base de datos

Estado previo (datos maestros gestionados por otros subsistemas) del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- El certificado de ejemplo que ya viene dentro de la aplicación: ruta classpath `firma/mi_certificado.p12`, contraseña `nadanada` (recurso del WAR, no un dato de BD).
- No existe ninguna entrada de certificado digital con el DNI «85432016B». Como todos los tests usan ese mismo DNI y algunos dejan la entrada creada al terminar, cada test restablece esta precondición al empezar: si el listado «Certificados digitales» muestra una fila con el DNI «85432016B», el administrador la abre, pulsa «Borrar» y confirma, antes de ejecutar sus pasos.

**Usuarios de acceso** (login y contraseña que `/sdd-debug-with-test-e2e-desc` usará para iniciar sesión):

| Login | Contraseña | Rol / Tipo | Centro |
|---|---|---|---|
| admin | admin | Administrador | — |

## Precondiciones

- El usuario `admin` ha iniciado sesión.
- No existe ninguna entrada con el DNI «85432016B» (si existe de una ejecución anterior, se borra desde el listado como describe el «Estado inicial de la base de datos»).

## Pasos

1. **Dado** que el administrador está en la pantalla «Certificados digitales» (menú «Administración SV» → «Certificados digitales»).
2. **Cuando** pulsa «Añadir certificado digital», rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12» y «Contraseña» con «nadanada», y pulsa «Guardar».
3. **Entonces** el sistema guarda la entrada y vuelve al listado.
4. **Cuando** abre la fila del DNI «85432016B», desmarca la casilla «Habilitado» y pulsa «Guardar».
5. **Entonces** el sistema guarda el cambio y vuelve al listado.
6. **Cuando** abre la fila del DNI «85432016B».
7. **Y** pulsa «Borrar».
8. **Entonces** el sistema pide confirmar el borrado.
9. **Cuando** el administrador confirma.

## Resultado esperado

- El sistema borra la entrada y vuelve al listado «Certificados digitales».
- El listado ya no muestra ninguna fila con el DNI «85432016B».
