---
type: test-e2e
id: T-004
---
<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-08-10_23-21_deshabilitar-certificado-digital/test-e2e-desc/t-004-alta-con-habilitado-desmarcado.desc.md
     Test: T-004  |  Origen ESC: ESC-004
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-004 — Alta con «Habilitado» desmarcado

**Origen ESC:** ESC-004
**Verifica:** —
**Pantalla principal:** screen-certificados-digitales.md
**Tipo:** happy

## Estado inicial de la base de datos

Estado previo (datos maestros gestionados por otros subsistemas) del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- El fichero de certificado de test que los escenarios suben como certificado de tipo «Subir un fichero con el certificado para guardarlo en la base de datos» (FICHERO_BD): `test1.p12`, de la carpeta `src/test/resources/firma/test/` del repositorio (fichero del repositorio, no un dato de BD ni un recurso del WAR), con contraseña «demo1234». Es un certificado de test emitido por la CA de demo y no es de ningún usuario.
- El usuario de demostración `sincertificado1@mislata.es`, con documento «94307933K», nombre «SinCertificado1» y apellidos «CIPFP Mislata». Es exclusivo de estos tests y no tiene certificado propio.
- Los certificados digitales de demostración (uno por cada usuario de demo que tiene certificado; `sincertificado1@mislata.es` no tiene). Por eso el listado «Certificados digitales» no está vacío y pagina: antes de mirar sus filas, cada test **filtra el listado por su DNI** escribiéndolo en el «Buscar...» de la columna «DNI» y pulsando Intro (y lo vuelve a filtrar si el filtro se pierde, p. ej. tras recargar la página), de modo que solo se ven las filas de ese DNI o «No se encontraron registros.».
- No existe ninguna entrada de certificado digital con el DNI «94307933K». Como todos los tests usan ese mismo DNI y algunos dejan la entrada creada al terminar, cada test restablece esta precondición al empezar: si el listado «Certificados digitales» muestra una fila con el DNI «94307933K», el administrador la abre, pulsa «Borrar» y confirma, antes de ejecutar sus pasos.

**Usuarios de acceso** (login y contraseña que `/sdd-debug-with-test-e2e-desc` usará para iniciar sesión):

| Login | Contraseña | Rol / Tipo | Centro |
|---|---|---|---|
| admin | admin | Administrador | — |

## Precondiciones

- El usuario `admin` ha iniciado sesión.
- No existe ninguna entrada con el DNI «94307933K» (si existe de una ejecución anterior, se borra desde el listado como describe el «Estado inicial de la base de datos»).

## Pasos

1. **Dado** que el administrador está en la pantalla «Certificados digitales» (menú «Criptografía» → «Certificados digitales»).
2. **Cuando** pulsa «Añadir certificado digital».
3. **Y** rellena «DNI» con «94307933K», elige en «Tipo de certificado» la opción «Subir un fichero con el certificado para guardarlo en la base de datos», sube en el campo «Fichero» el fichero «test1.p12», rellena «Nueva contraseña» con «demo1234».
4. **Y** desmarca la casilla «Habilitado».
5. **Y** pulsa «Guardar».

## Resultado esperado

- El sistema guarda la entrada y vuelve al listado «Certificados digitales».
- El listado muestra la fila del DNI «94307933K» con la columna «Habilitado» sin marcar.
