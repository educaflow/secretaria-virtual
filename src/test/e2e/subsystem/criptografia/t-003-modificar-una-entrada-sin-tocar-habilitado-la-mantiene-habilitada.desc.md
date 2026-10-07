---
type: test-e2e
id: T-003
---
<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-08-10_23-21_deshabilitar-certificado-digital/test-e2e-desc/t-003-modificar-una-entrada-sin-tocar-habilitado-la-mantiene-habilitada.desc.md
     Test: T-003  |  Origen ESC: ESC-003
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-003 — Modificar una entrada sin tocar «Habilitado» la mantiene habilitada

**Origen ESC:** ESC-003
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
2. **Cuando** pulsa «Añadir certificado digital», rellena «DNI» con «94307933K», elige en «Tipo de certificado» la opción «Subir un fichero con el certificado para guardarlo en la base de datos», sube en el campo «Fichero» el fichero «test1.p12», rellena «Nueva contraseña» con «demo1234», y pulsa «Guardar».
3. **Entonces** el sistema guarda la entrada y vuelve al listado.
4. **Cuando** abre la fila del DNI «94307933K».
5. **Y** escribe «otraclave» en el campo «Nueva contraseña», que aparece vacío (la contraseña guardada en el alta no se muestra), sin tocar la casilla «Habilitado».
6. **Y** pulsa «Guardar».
7. **Cuando** vuelve a abrir la fila del DNI «94307933K».

## Resultado esperado

- El sistema guarda el cambio y vuelve al listado «Certificados digitales».
- El listado muestra la fila del DNI «94307933K» con la columna «Habilitado» marcada.
- Al reabrir la fila, la casilla «Habilitado» sigue marcada y el campo «Nueva contraseña» aparece vacío: la contraseña es un secreto de solo escritura y el servidor nunca la devuelve al navegador, ni la anterior ni la recién escrita.
- No se comprueba desde la pantalla que la contraseña guardada sea «otraclave», porque no hay forma de leerla sin exponer el secreto; que un «Nueva contraseña» relleno sustituye a la guardada y uno vacío la conserva lo cubren los tests unitarios de `CertificadoDigitalServiceImpl`.
