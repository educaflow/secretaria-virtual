---
type: test-e2e
id: T-007
---

<!-- ARTEFACTO GENERADO por /sdd-create-tests-e2e — NO editar a mano.
     Snapshot "as-tested": copia de la descripción que pasó al depurar con /sdd-debug-with-test-e2e-desc.
     Fuente: .sdd/drafts/2026-09-08_02-53_nombre-apellidos-certificado-digital/test-e2e-desc/t-007-alta-con-el-dni-de-un-usuario-de-la-aplicacion.desc.md
     Iniciativa: 2026-09-08_02-53_nombre-apellidos-certificado-digital
     Test: T-007  |  Origen ESC: ESC-001
     Para regenerar: /sdd-create-tests-e2e (sobrescribe desde la fuente). -->

# T-007 — Alta con el DNI de un usuario de la aplicación

**Origen ESC:** ESC-001
**Verifica:** R-CertificadoDigital-001, U-certificados-digitales-001, U-certificados-digitales-007
**Pantalla principal:** screen-certificados-digitales.md
**Tipo:** happy

## Estado inicial de la base de datos

Estado previo (datos maestros gestionados por otros subsistemas) del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- Los dos ficheros de certificado que ya vienen dentro de la aplicación y que los escenarios usan como certificado de tipo «Usar un fichero con el certificado que ya está dentro del del WAR»: ruta classpath `firma/mi_certificado.p12` y ruta classpath `firma/instalar_certificado_criptografico/secretario.p12` (recursos del WAR, no datos de BD).
- Los usuarios y centros de demostración; en particular el usuario `secretario@mislata.es`, con documento «29050788V», nombre «Secretario» y apellidos «CIPFP Mislata».
- No existe ningún usuario de la aplicación cuyo documento sea «12345678Z».
- No existe ningún certificado digital con DNI «29050788V» ni con DNI «12345678Z». Como todos los tests usan uno de esos dos DNI y algunos dejan certificados creados al terminar, **cada test restablece esta precondición al empezar**: si el listado «Certificados digitales» muestra alguna fila con el DNI que va a usar, el administrador la abre, pulsa «Borrar», confirma, y repite hasta que no quede ninguna, antes de ejecutar sus pasos.

**Usuarios de acceso** (login y contraseña que `/sdd-debug-with-test-e2e-desc` usará para iniciar sesión):

| Login | Contraseña | Rol / Tipo | Centro |
|---|---|---|---|
| admin | admin | Administrador | — |

> Nota sobre el literal del tipo de certificado: el título real de la opción en la aplicación es **«Usar un fichero con el certificado que ya está dentro del del WAR»** (con «del del», errata preexistente que este delta no corrige). Los pasos de abajo usan ese literal, que es el que se ve en el navegador.

> **Nota sobre las columnas del listado.** El listado «Certificados digitales» tiene exactamente cinco columnas: **DNI**, **Nombre**, **Apellidos**, **Tipo de certificado** y **Habilitado**. La **«Ruta classpath» NO es una columna del listado**: es un campo del formulario y solo se ve al abrir una fila. Por eso, cuando un test deja dos certificados con el mismo DNI, las dos filas se distinguen **por su casilla «Habilitado»** y **por su posición** (la ordenación por defecto es `dni,-enabled`: dentro de un mismo DNI, la habilitada va primero). Si un test necesita confirmar la ruta de un certificado concreto, lo hace **dentro del formulario ya abierto**, nunca leyéndola del listado.

## Precondiciones
- El usuario `admin` ha iniciado sesión con usuario «admin» y contraseña «admin».
- No existe ningún certificado digital con DNI «29050788V» (si quedaran de una ejecución anterior, se borran desde el listado como describe el «Estado inicial de la base de datos»).

## Pasos
1. **Dado** que el administrador está en la pantalla «Certificados digitales» (menú «Administración SV» → «Certificados digitales»).
2. **Cuando** pulsa «Añadir certificado digital».
3. **Y** escribe en el campo «DNI» el valor «29050788V».
4. **Entonces** el campo «Nombre» muestra «Secretario» y el campo «Apellidos» muestra «CIPFP Mislata», y los dos están de solo lectura.
5. **Cuando** elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR».
6. **Y** escribe en el campo «Ruta classpath» el valor «firma/mi_certificado.p12».
7. **Y** pulsa «Guardar».

## Resultado esperado
- El sistema guarda el certificado y vuelve al listado «Certificados digitales».
- El listado muestra una fila con DNI «29050788V», «Nombre» «Secretario», «Apellidos» «CIPFP Mislata», «Tipo de certificado» «Usar un fichero con el certificado que ya está dentro del del WAR» y «Habilitado» marcado.
