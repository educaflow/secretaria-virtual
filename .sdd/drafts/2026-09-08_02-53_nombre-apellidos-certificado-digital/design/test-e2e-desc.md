# Tests E2E

Tests concretos end-to-end materializados a partir de los escenarios (`ESC-NNN`) de las historias de usuario del `specification.md` y de las V/R/U del diseño.

Cada test es **independiente** (no depende del estado dejado por otro) y **trazable** (declara qué `ESC-NNN` materializa y qué V/R/U verifica). `/sdd-debug-with-test-e2e-desc` lo ejecuta contra la aplicación real tras la implementación (bucle de auto-corrección).

> Numeración: la carpeta `src/test/e2e/subsystem/criptografia/` ya tiene persistidos `t-001` … `t-006` de una iniciativa anterior, así que este bloque arranca en **T-007** y es contiguo.

---

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

---

## T-007 — Alta con el DNI de un usuario de la aplicación

**Origen ESC:** ESC-001
**Verifica:** R-CertificadoDigital-001, U-certificados-digitales-001, U-certificados-digitales-007
**Pantalla principal:** screen-certificados-digitales.md
**Tipo:** happy

### Precondiciones
- El usuario `admin` ha iniciado sesión con usuario «admin» y contraseña «admin».
- No existe ningún certificado digital con DNI «29050788V» (si quedaran de una ejecución anterior, se borran desde el listado como describe el «Estado inicial de la base de datos»).

### Pasos
1. **Dado** que el administrador está en la pantalla «Certificados digitales» (menú «Administración SV» → «Certificados digitales»).
2. **Cuando** pulsa «Añadir certificado digital».
3. **Y** escribe en el campo «DNI» el valor «29050788V».
4. **Entonces** el campo «Nombre» muestra «Secretario» y el campo «Apellidos» muestra «CIPFP Mislata», y los dos están de solo lectura.
5. **Cuando** elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR».
6. **Y** escribe en el campo «Ruta classpath» el valor «firma/mi_certificado.p12».
7. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema guarda el certificado y vuelve al listado «Certificados digitales».
- El listado muestra una fila con DNI «29050788V», «Nombre» «Secretario», «Apellidos» «CIPFP Mislata», «Tipo de certificado» «Usar un fichero con el certificado que ya está dentro del del WAR» y «Habilitado» marcado.

---

## T-008 — Alta con un DNI que no es de ningún usuario

**Origen ESC:** ESC-002
**Verifica:** R-CertificadoDigital-001, U-certificados-digitales-002
**Pantalla principal:** screen-certificados-digitales.md
**Tipo:** happy

### Precondiciones
- El usuario `admin` ha iniciado sesión con usuario «admin» y contraseña «admin».
- No existe ningún certificado digital con DNI «12345678Z».

### Pasos
1. **Dado** que el administrador está en la pantalla «Certificados digitales» (menú «Administración SV» → «Certificados digitales»).
2. **Cuando** pulsa «Añadir certificado digital».
3. **Y** escribe en el campo «DNI» el valor «12345678Z».
4. **Entonces** los campos «Nombre» y «Apellidos» están vacíos y son editables.
5. **Cuando** escribe en «Nombre» el valor «Ana» y en «Apellidos» el valor «García López».
6. **Y** elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR».
7. **Y** escribe en el campo «Ruta classpath» el valor «firma/mi_certificado.p12».
8. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema guarda el certificado y vuelve al listado «Certificados digitales».
- El listado muestra una fila con DNI «12345678Z», «Nombre» «Ana», «Apellidos» «García López», «Tipo de certificado» «Usar un fichero con el certificado que ya está dentro del del WAR» y «Habilitado» marcado.

---

## T-009 — Alta sin el nombre cuando lo escribe el administrador

**Origen ESC:** ESC-003
**Verifica:** V-CertificadoDigital-001
**Pantalla principal:** screen-certificados-digitales.md
**Tipo:** error

### Precondiciones
- El usuario `admin` ha iniciado sesión con usuario «admin» y contraseña «admin».
- No existe ningún certificado digital con DNI «12345678Z».

### Pasos
1. **Dado** que el administrador está en la pantalla «Certificados digitales» (menú «Administración SV» → «Certificados digitales»).
2. **Cuando** pulsa «Añadir certificado digital».
3. **Y** escribe en el campo «DNI» el valor «12345678Z».
4. **Y** elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR».
5. **Y** escribe en el campo «Ruta classpath» el valor «firma/mi_certificado.p12».
6. **Y** deja el campo «Nombre» vacío y escribe en «Apellidos» el valor «García López».
7. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema muestra el error «El nombre es obligatorio».
- No se guarda nada: el listado «Certificados digitales» no muestra ninguna fila con DNI «12345678Z».

---

## T-010 — Alta sin los apellidos cuando los escribe el administrador

**Origen ESC:** ESC-003
**Verifica:** V-CertificadoDigital-002
**Pantalla principal:** screen-certificados-digitales.md
**Tipo:** error

### Precondiciones
- El usuario `admin` ha iniciado sesión con usuario «admin» y contraseña «admin».
- No existe ningún certificado digital con DNI «12345678Z».

### Pasos
1. **Dado** que el administrador está en la pantalla «Certificados digitales» (menú «Administración SV» → «Certificados digitales»).
2. **Cuando** pulsa «Añadir certificado digital».
3. **Y** escribe en el campo «DNI» el valor «12345678Z».
4. **Y** elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR».
5. **Y** escribe en el campo «Ruta classpath» el valor «firma/mi_certificado.p12».
6. **Y** escribe en «Nombre» el valor «Ana» y deja el campo «Apellidos» vacío.
7. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema muestra el error «Los apellidos son obligatorios».
- No se guarda nada: el listado «Certificados digitales» no muestra ninguna fila con DNI «12345678Z».

---

## T-011 — Corregir el nombre escrito a mano

**Origen ESC:** ESC-004
**Verifica:** R-CertificadoDigital-002, U-certificados-digitales-003, U-certificados-digitales-005
**Pantalla principal:** screen-certificados-digitales.md
**Tipo:** happy

### Precondiciones
- El usuario `admin` ha iniciado sesión con usuario «admin» y contraseña «admin».
- No existe ningún certificado digital con DNI «12345678Z».

### Pasos
1. **Dado** que el administrador está en la pantalla «Certificados digitales» (menú «Administración SV» → «Certificados digitales»).
2. **Cuando** pulsa «Añadir certificado digital», escribe en «DNI» «12345678Z», en «Nombre» «Ana», en «Apellidos» «García López», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», escribe en «Ruta classpath» «firma/mi_certificado.p12» y pulsa «Guardar».
3. **Entonces** el sistema guarda el certificado y vuelve al listado.
4. **Cuando** pulsa la fila del DNI «12345678Z».
5. **Entonces** el formulario muestra el campo «DNI» de solo lectura y los campos «Nombre» y «Apellidos» editables.
6. **Cuando** cambia el campo «Apellidos» a «García Pérez».
7. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema guarda el cambio y vuelve al listado «Certificados digitales».
- La fila del DNI «12345678Z» muestra «Apellidos» «García Pérez» y «Nombre» «Ana».

---

## T-012 — El nombre tomado del usuario no se puede cambiar

**Origen ESC:** ESC-005
**Verifica:** U-certificados-digitales-003, U-certificados-digitales-004
**Pantalla principal:** screen-certificados-digitales.md
**Tipo:** UI

### Precondiciones
- El usuario `admin` ha iniciado sesión con usuario «admin» y contraseña «admin».
- No existe ningún certificado digital con DNI «29050788V».

### Pasos
1. **Dado** que el administrador está en la pantalla «Certificados digitales» (menú «Administración SV» → «Certificados digitales»).
2. **Cuando** pulsa «Añadir certificado digital», escribe en «DNI» «29050788V», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», escribe en «Ruta classpath» «firma/mi_certificado.p12» y pulsa «Guardar».
3. **Entonces** el sistema guarda el certificado y vuelve al listado.
4. **Cuando** pulsa la fila del DNI «29050788V».

### Resultado esperado
- El formulario muestra el campo «DNI» con «29050788V», el campo «Nombre» con «Secretario» y el campo «Apellidos» con «CIPFP Mislata», los tres de solo lectura.
- Los demás campos del formulario («Tipo de certificado», «Ruta classpath», «Contraseña», «Habilitado») siguen siendo editables.

---

## T-013 — Segundo certificado del mismo DNI, deshabilitado

**Origen ESC:** ESC-006
**Verifica:** V-CertificadoDigital-005, R-CertificadoDigital-001, U-certificados-digitales-007
**Pantalla principal:** screen-certificados-digitales.md
**Tipo:** happy

### Precondiciones
- El usuario `admin` ha iniciado sesión con usuario «admin» y contraseña «admin».
- No existe ningún certificado digital con DNI «29050788V».

### Pasos
1. **Dado** que el administrador está en la pantalla «Certificados digitales» (menú «Administración SV» → «Certificados digitales»).
2. **Cuando** pulsa «Añadir certificado digital», escribe en «DNI» «29050788V», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», escribe en «Ruta classpath» «firma/mi_certificado.p12» y pulsa «Guardar».
3. **Entonces** el sistema guarda el certificado y vuelve al listado.
4. **Cuando** pulsa «Añadir certificado digital», escribe en «DNI» «29050788V», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», escribe en «Ruta classpath» «firma/instalar_certificado_criptografico/secretario.p12» y desmarca la casilla «Habilitado».
5. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema guarda el certificado y vuelve al listado «Certificados digitales».
- El listado muestra dos filas con DNI «29050788V», las dos con «Nombre» «Secretario» y «Apellidos» «CIPFP Mislata».
- De esas dos filas, la **primera** tiene «Habilitado» marcado (es la creada en el paso 2) y la **segunda** tiene «Habilitado» sin marcar (la creada en el paso 4): la ordenación por defecto pone los habilitados antes que los deshabilitados dentro del mismo DNI.

---

## T-014 — Segundo certificado del mismo DNI, habilitado, rechazado

**Origen ESC:** ESC-007
**Verifica:** V-CertificadoDigital-005
**Pantalla principal:** screen-certificados-digitales.md
**Tipo:** error

### Precondiciones
- El usuario `admin` ha iniciado sesión con usuario «admin» y contraseña «admin».
- No existe ningún certificado digital con DNI «29050788V».

### Pasos
1. **Dado** que el administrador está en la pantalla «Certificados digitales» (menú «Administración SV» → «Certificados digitales»).
2. **Cuando** pulsa «Añadir certificado digital», escribe en «DNI» «29050788V», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», escribe en «Ruta classpath» «firma/mi_certificado.p12» y pulsa «Guardar».
3. **Entonces** el sistema guarda el certificado y vuelve al listado.
4. **Cuando** pulsa «Añadir certificado digital», escribe en «DNI» «29050788V», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», escribe en «Ruta classpath» «firma/instalar_certificado_criptografico/secretario.p12» y deja la casilla «Habilitado» marcada (viene marcada por defecto).
5. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema muestra el error «Ya existe un certificado digital habilitado para el DNI 29050788V».
- No se guarda nada: el listado «Certificados digitales» sigue mostrando una sola fila con DNI «29050788V».

---

## T-015 — Habilitar un certificado cuando otro del mismo DNI ya está habilitado

**Origen ESC:** ESC-008
**Verifica:** V-CertificadoDigital-005
**Pantalla principal:** screen-certificados-digitales.md
**Tipo:** error

### Precondiciones
- El usuario `admin` ha iniciado sesión con usuario «admin» y contraseña «admin».
- No existe ningún certificado digital con DNI «29050788V».

### Pasos
1. **Dado** que el administrador está en la pantalla «Certificados digitales» (menú «Administración SV» → «Certificados digitales»).
2. **Cuando** pulsa «Añadir certificado digital», escribe en «DNI» «29050788V», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», escribe en «Ruta classpath» «firma/mi_certificado.p12» y pulsa «Guardar».
3. **Y** pulsa «Añadir certificado digital», escribe en «DNI» «29050788V», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», escribe en «Ruta classpath» «firma/instalar_certificado_criptografico/secretario.p12», desmarca la casilla «Habilitado» y pulsa «Guardar».
4. **Entonces** el listado muestra dos filas con DNI «29050788V»: la primera con «Habilitado» marcado y la segunda sin marcar.
5. **Cuando** pulsa la **segunda** fila del DNI «29050788V», la que tiene «Habilitado» sin marcar (al abrirla, el campo «Ruta classpath» del formulario muestra «firma/instalar_certificado_criptografico/secretario.p12»).
6. **Y** marca la casilla «Habilitado».
7. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema muestra el error «Ya existe un certificado digital habilitado para el DNI 29050788V».
- No se guarda nada: en el listado siguen las dos filas del DNI «29050788V», la primera con «Habilitado» marcado y la segunda sin marcar.

---

## T-016 — Cambiar el certificado vigente de una persona

**Origen ESC:** ESC-009
**Verifica:** V-CertificadoDigital-005, U-certificados-digitales-007
**Pantalla principal:** screen-certificados-digitales.md
**Tipo:** happy

### Precondiciones
- El usuario `admin` ha iniciado sesión con usuario «admin» y contraseña «admin».
- No existe ningún certificado digital con DNI «29050788V».

### Pasos
1. **Dado** que el administrador está en la pantalla «Certificados digitales» (menú «Administración SV» → «Certificados digitales»).
2. **Cuando** pulsa «Añadir certificado digital», escribe en «DNI» «29050788V», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», escribe en «Ruta classpath» «firma/mi_certificado.p12» y pulsa «Guardar».
3. **Y** pulsa «Añadir certificado digital», escribe en «DNI» «29050788V», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», escribe en «Ruta classpath» «firma/instalar_certificado_criptografico/secretario.p12», desmarca la casilla «Habilitado» y pulsa «Guardar».
4. **Y** pulsa la **primera** fila del DNI «29050788V», que es la que tiene «Habilitado» marcado (al abrirla, el campo «Ruta classpath» del formulario muestra «firma/mi_certificado.p12»), desmarca la casilla «Habilitado» y pulsa «Guardar».
5. **Entonces** el sistema guarda el cambio y vuelve al listado, donde las dos filas del DNI «29050788V» aparecen con «Habilitado» sin marcar.
6. **Cuando** pulsa la fila del DNI «29050788V» cuyo formulario muestra en «Ruta classpath» el valor «firma/instalar_certificado_criptografico/secretario.p12» (con las dos deshabilitadas, la ordenación no las distingue: se identifica abriendo una fila y comprobando ese campo **en el formulario**; si no es la buscada, se vuelve al listado y se abre la otra).
7. **Y** marca la casilla «Habilitado».
8. **Y** pulsa «Guardar».

### Resultado esperado
- El sistema guarda el cambio y vuelve al listado «Certificados digitales».
- De las dos filas del DNI «29050788V», la **primera** tiene «Habilitado» marcado y la **segunda** no (la ordenación `dni,-enabled` ha intercambiado sus posiciones respecto al paso 5).
- Al abrir esa primera fila, el campo «Ruta classpath» del formulario muestra «firma/instalar_certificado_criptografico/secretario.p12».

---

## T-017 — Borrar uno de los certificados de una persona

**Origen ESC:** ESC-010
**Verifica:** —
**Pantalla principal:** screen-certificados-digitales.md
**Tipo:** happy

### Precondiciones
- El usuario `admin` ha iniciado sesión con usuario «admin» y contraseña «admin».
- No existe ningún certificado digital con DNI «29050788V».

### Pasos
1. **Dado** que el administrador está en la pantalla «Certificados digitales» (menú «Administración SV» → «Certificados digitales»).
2. **Cuando** pulsa «Añadir certificado digital», escribe en «DNI» «29050788V», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», escribe en «Ruta classpath» «firma/mi_certificado.p12» y pulsa «Guardar».
3. **Y** pulsa «Añadir certificado digital», escribe en «DNI» «29050788V», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», escribe en «Ruta classpath» «firma/instalar_certificado_criptografico/secretario.p12», desmarca la casilla «Habilitado» y pulsa «Guardar».
4. **Y** pulsa la **segunda** fila del DNI «29050788V», que es la que tiene «Habilitado» sin marcar (la ordenación `dni,-enabled` deja la habilitada primero).
5. **Y** pulsa «Borrar».
6. **Y** confirma el borrado si el sistema lo pide.

### Resultado esperado
- El sistema borra ese certificado y vuelve al listado «Certificados digitales».
- Queda una sola fila con DNI «29050788V», con «Habilitado» marcado.
- Al abrir esa fila, el campo «Ruta classpath» del formulario muestra «firma/mi_certificado.p12» (la ruta no es columna del listado; se comprueba dentro del formulario).
