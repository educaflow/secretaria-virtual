---
type: test-e2e
id: T-013
---

# T-013 — Segundo certificado del mismo DNI, deshabilitado

**Origen ESC:** ESC-006
**Verifica:** V-CertificadoDigital-005, R-CertificadoDigital-001, U-certificados-digitales-007
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
- No existe ningún certificado digital con DNI «29050788V».

## Pasos
1. **Dado** que el administrador está en la pantalla «Certificados digitales» (menú «Administración SV» → «Certificados digitales»).
2. **Cuando** pulsa «Añadir certificado digital», escribe en «DNI» «29050788V», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», escribe en «Ruta classpath» «firma/mi_certificado.p12» y pulsa «Guardar».
3. **Entonces** el sistema guarda el certificado y vuelve al listado.
4. **Cuando** pulsa «Añadir certificado digital», escribe en «DNI» «29050788V», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», escribe en «Ruta classpath» «firma/instalar_certificado_criptografico/secretario.p12» y desmarca la casilla «Habilitado».
5. **Y** pulsa «Guardar».

## Resultado esperado
- El sistema guarda el certificado y vuelve al listado «Certificados digitales».
- El listado muestra dos filas con DNI «29050788V», las dos con «Nombre» «Secretario» y «Apellidos» «CIPFP Mislata».
- De esas dos filas, la **primera** tiene «Habilitado» marcado (es la creada en el paso 2) y la **segunda** tiene «Habilitado» sin marcar (la creada en el paso 4): la ordenación por defecto pone los habilitados antes que los deshabilitados dentro del mismo DNI.
