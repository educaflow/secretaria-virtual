---
type: test-e2e
id: T-016
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-09-08_02-53_nombre-apellidos-certificado-digital/test-e2e-desc/t-016-cambiar-el-certificado-vigente-de-una-persona.desc.md
     Iniciativa: 2026-09-08_02-53_nombre-apellidos-certificado-digital
     Test: T-016  |  Origen ESC: ESC-009
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-016 — Cambiar el certificado vigente de una persona

**Origen ESC:** ESC-009
**Verifica:** V-CertificadoDigital-005, U-certificados-digitales-007
**Pantalla principal:** screen-certificados-digitales.md
**Tipo:** happy

## Estado inicial de la base de datos

Estado previo (datos maestros gestionados por otros subsistemas) del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- Los dos ficheros de certificado de test que los escenarios suben como certificado de tipo «Subir un fichero con el certificado para guardarlo en la base de datos» (FICHERO_BD): `test1.p12` y `test2.p12`, de la carpeta `src/test/resources/firma/test/` del repositorio (ficheros del repositorio, no datos de BD ni recursos del WAR). Son certificados de test emitidos por la CA de demo, no son de ningún usuario, y su contraseña es «demo1234» (los escenarios no la escriben: el alta no la exige).
- Los usuarios y centros de demostración; en particular el usuario `sincertificado2@mislata.es`, con documento «96931712Y», nombre «SinCertificado2» y apellidos «CIPFP Mislata».
- No existe ningún usuario de la aplicación cuyo documento sea «91851115V».
- Los certificados digitales de demostración (uno por cada usuario de demo que tiene certificado; `sincertificado2@mislata.es` no tiene). Por eso el listado «Certificados digitales» no está vacío y pagina: antes de mirar sus filas, cada test **filtra el listado por su DNI** escribiéndolo en el «Buscar...» de la columna «DNI» y pulsando Intro (y lo vuelve a filtrar si el filtro se pierde, p. ej. tras recargar la página), de modo que solo se ven las filas de ese DNI o «No se encontraron registros.».
- No existe ningún certificado digital con DNI «96931712Y» ni con DNI «91851115V». Como todos los tests usan uno de esos dos DNI y algunos dejan certificados creados al terminar, **cada test restablece esta precondición al empezar**: si el listado «Certificados digitales» muestra alguna fila con el DNI que va a usar, el administrador la abre, pulsa «Borrar», confirma, y repite hasta que no quede ninguna, antes de ejecutar sus pasos.

**Usuarios de acceso** (login y contraseña que `/sdd-debug-with-test-e2e-desc` usará para iniciar sesión):

| Login | Contraseña | Rol / Tipo | Centro |
|---|---|---|---|
| admin | admin | Administrador | — |

> Nota sobre el tipo de certificado: los escenarios usan siempre la opción **«Subir un fichero con el certificado para guardarlo en la base de datos»** (FICHERO_BD). Con ese tipo el formulario muestra el campo «Fichero» (donde se sube el `.p12` y, una vez subido, aparece su nombre) y el campo «Nueva contraseña». Los certificados de test no están dentro del WAR, así que no se pueden dar de alta con el tipo que lee el certificado del WAR.

> **Nota sobre las columnas del listado.** El listado «Certificados digitales» tiene exactamente cinco columnas: **DNI**, **Nombre**, **Apellidos**, **Tipo de certificado** y **Habilitado**. El **nombre del fichero subido NO es una columna del listado**: lo muestra el campo «Fichero» del formulario y solo se ve al abrir una fila. Por eso, cuando un test deja dos certificados con el mismo DNI, las dos filas se distinguen **por su casilla «Habilitado»** y **por su posición** (la ordenación por defecto es `dni,-enabled`: dentro de un mismo DNI, la habilitada va primero). Si un test necesita confirmar el fichero de un certificado concreto (`test1.p12` o `test2.p12`), lo hace **dentro del formulario ya abierto**, nunca leyéndolo del listado.

## Precondiciones
- El usuario `admin` ha iniciado sesión con usuario «admin» y contraseña «admin».
- No existe ningún certificado digital con DNI «96931712Y».

## Pasos
1. **Dado** que el administrador está en la pantalla «Certificados digitales» (menú «Criptografía» → «Certificados digitales»).
2. **Cuando** pulsa «Añadir certificado digital», escribe en «DNI» «96931712Y», elige en «Tipo de certificado» la opción «Subir un fichero con el certificado para guardarlo en la base de datos», sube en el campo «Fichero» el fichero «test1.p12» y pulsa «Guardar».
3. **Y** pulsa «Añadir certificado digital», escribe en «DNI» «96931712Y», elige en «Tipo de certificado» la opción «Subir un fichero con el certificado para guardarlo en la base de datos», sube en el campo «Fichero» el fichero «test2.p12», desmarca la casilla «Habilitado» y pulsa «Guardar».
4. **Y** pulsa la **primera** fila del DNI «96931712Y», que es la que tiene «Habilitado» marcado (al abrirla, el campo «Fichero» del formulario muestra «test1.p12»), desmarca la casilla «Habilitado» y pulsa «Guardar».
5. **Entonces** el sistema guarda el cambio y vuelve al listado, donde las dos filas del DNI «96931712Y» aparecen con «Habilitado» sin marcar.
6. **Cuando** pulsa la fila del DNI «96931712Y» cuyo formulario muestra en el campo «Fichero» el fichero «test2.p12» (con las dos deshabilitadas, la ordenación no las distingue: se identifica abriendo una fila y comprobando ese campo **en el formulario**; si no es la buscada, se vuelve al listado y se abre la otra).
7. **Y** marca la casilla «Habilitado».
8. **Y** pulsa «Guardar».

## Resultado esperado
- El sistema guarda el cambio y vuelve al listado «Certificados digitales».
- De las dos filas del DNI «96931712Y», la **primera** tiene «Habilitado» marcado y la **segunda** no (la ordenación `dni,-enabled` ha intercambiado sus posiciones respecto al paso 5).
- Al abrir esa primera fila, el campo «Fichero» del formulario muestra «test2.p12».
