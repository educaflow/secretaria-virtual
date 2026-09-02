# Tests E2E

Tests concretos end-to-end materializados a partir de los escenarios (`ESC-NNN`) de las historias de usuario del `specification.md` y de las V/R/U del diseño.

Cada test es **independiente** (no depende del estado dejado por otro) y **trazable** (declara qué `ESC-NNN` materializa y qué V/R/U verifica). `/sdd-debug-with-test-e2e-desc` lo ejecuta contra la aplicación real tras la implementación (bucle de auto-corrección).

---

## Estado inicial de la base de datos

Estado previo (datos maestros gestionados por otros subsistemas) del que parten **todos** los tests. Ningún test puede presuponer más estado que este; cada test lo referencia en sus `Precondiciones`.

- Los dos centros educativos de demo: «CIPFP Mislata» (código `46019660`) y «CIPFP Batoi» (código `03012165`).
- Los usuarios de demo de `usuarios-demo.xml`, con su DNI y su centro. Los que usan estos tests son el director y el secretario de CIPFP Mislata, más el administrador de la aplicación.
- El certificado digital de pruebas que viaja dentro de la aplicación: recurso de classpath `firma/mi_certificado.p12`, con contraseña `nadanada`. **No** hay ninguna entrada de certificado digital dada de alta en la base de datos: cada test que la necesita la crea.
- El PDF de ejemplo que viaja dentro de la aplicación (`data-demo/input/documento_ejemplo_firma.pdf`), de una página y con espacio libre para el recuadro de la firma.
- Las **ocho tareas de firma precargadas**, todas en estado «Pendiente de firmar», con su recuadro y su página ya fijados y con el PDF de ejemplo como documento a firmar:
  - Del firmante `director@mislata.es` (DNI `85432016B`): «Firma de prueba 1», «Firma de prueba 2», «Firma de prueba 4» y «Firma de prueba 5» con **un** documento cada una, y «Firma de prueba 3» con **dos** documentos.
  - Del firmante `secretario@mislata.es` (DNI `29050788V`): «Firma de prueba del secretario», con un documento.
  - Del firmante `admin` (que **no tiene DNI**): «Firma de prueba del administrador 1» y «Firma de prueba del administrador 2», con un documento cada una.

> **Reejecución.** Una tarea de firma resuelta (firmada o rechazada) no vuelve a «Pendiente de firmar». Los tests que resuelven una tarea (T-001, T-002, T-003, T-009, T-011) solo se pueden volver a pasar tras **recrear la base de datos**, que es lo que recarga los datos de demo. En cambio, **todo** test cuyo resultado dependa del certificado digital del firmante —tanto los que necesitan que exista como los que necesitan que **no** exista— es reejecutable tal cual e **independiente del orden**, porque **MUST** empezar borrando la entrada del DNI que va a usar antes de dejarla en el estado que necesita (mismo patrón que los tests de `src/test/e2e/subsystem/criptografia/`).

**Usuarios de acceso** (login y contraseña que `/sdd-debug-with-test-e2e-desc` usará para iniciar sesión):

| Login | Contraseña | Rol / Tipo | Centro |
|---|---|---|---|
| `admin` | `admin` | Administrador de la aplicación (su usuario **no tiene DNI**) | — |
| `director@mislata.es` | `demo1234` | Profesor con cargo Director (DNI `85432016B`) | CIPFP Mislata |
| `secretario@mislata.es` | `demo1234` | Profesor con cargo Secretario (DNI `29050788V`) | CIPFP Mislata |

---

## T-001 — Firma en el servidor con la contraseña del certificado ya guardada

**Origen ESC:** ESC-001
**Verifica:** V-TareaFirma-001, V-TareaFirma-002, R-TareaFirma-001, R-TareaFirma-002, R-TareaFirma-004, U-documentos-pendientes-de-firma-004, U-documentos-pendientes-de-firma-013, U-documentos-pendientes-de-firma-016
**Pantalla principal:** screen-documentos-pendientes-de-firma.md
**Tipo:** happy

### Precondiciones
- El estado inicial de la base de datos descrito arriba.

### Pasos
1. **Dado** que el Administrador inicia sesión con usuario «admin» y contraseña «admin».
2. **Y** abre la pantalla de certificados digitales y, si ya existe una entrada para el DNI «85432016B», la borra.
3. **Cuando** pulsa «Añadir certificado digital».
4. **Y** rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12» y «Contraseña» con «nadanada», y deja marcado «Habilitado».
5. **Y** pulsa «Guardar».
6. **Y** cierra sesión.
7. **Y** el director inicia sesión con «director@mislata.es» y contraseña «demo1234».
8. **Y** abre «Firmar documentos → Pendientes» y entra en la tarea de firma «Firma de prueba 1».
9. **Y** pulsa «Firmar todos los documentos».
10. **Entonces** el sistema muestra el texto «Los documentos se firmarán en el servidor con su certificado digital.», no muestra ningún campo «PIN» ni ningún campo «Contraseña», y muestra el botón «Firmar todos los documentos y finalizar» sin mostrar el botón «Firmar todos los documentos con AutoFirma y finalizar».
11. **Cuando** pulsa «Firmar todos los documentos y finalizar».
12. **Entonces** el sistema devuelve al firmante al listado de «Firmar documentos → Pendientes».

### Resultado esperado
- El listado de «Firmar documentos → Pendientes» ya no muestra «Firma de prueba 1».
- En «Firmar documentos → Firmados», «Firma de prueba 1» aparece con estado «Firmado».
- Al entrar en «Firma de prueba 1» y abrir su documento, este tiene versión firmada (la pestaña «Documento firmado» está disponible).

---

## T-002 — Firma en el servidor tecleando la contraseña del certificado

**Origen ESC:** ESC-002
**Verifica:** V-TareaFirma-006, R-TareaFirma-001, R-TareaFirma-002, R-TareaFirma-003, U-documentos-pendientes-de-firma-005, U-documentos-pendientes-de-firma-008, U-documentos-pendientes-de-firma-010, U-documentos-pendientes-de-firma-013
**Pantalla principal:** screen-documentos-pendientes-de-firma.md
**Tipo:** happy

### Precondiciones
- El estado inicial de la base de datos descrito arriba.

### Pasos
1. **Dado** que el Administrador inicia sesión con usuario «admin» y contraseña «admin».
2. **Y** abre la pantalla de certificados digitales y, si ya existe una entrada para el DNI «85432016B», la borra.
3. **Cuando** pulsa «Añadir certificado digital».
4. **Y** rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12», **deja «Contraseña» vacía**, y deja marcado «Habilitado».
5. **Y** pulsa «Guardar».
6. **Y** cierra sesión.
7. **Y** el director inicia sesión con «director@mislata.es» y contraseña «demo1234».
8. **Y** abre «Firmar documentos → Pendientes» y entra en la tarea de firma «Firma de prueba 2».
9. **Y** pulsa «Firmar todos los documentos».
10. **Entonces** el sistema muestra el texto «Los documentos se firmarán en el servidor con su certificado digital. Introduzca la contraseña de su certificado.» junto a un campo «Contraseña» vacío, y no muestra el botón «Firmar todos los documentos con AutoFirma y finalizar».
11. **Cuando** escribe «nadanada» en «Contraseña».
12. **Y** pulsa «Firmar todos los documentos y finalizar».
13. **Entonces** el sistema devuelve al firmante al listado de «Firmar documentos → Pendientes».

### Resultado esperado
- El listado de «Firmar documentos → Pendientes» ya no muestra «Firma de prueba 2».
- En «Firmar documentos → Firmados», «Firma de prueba 2» aparece con estado «Firmado».
- Al entrar en «Firma de prueba 2» y abrir su documento, este tiene versión firmada.

---

## T-003 — Una contraseña incorrecta no firma ningún documento y se puede reintentar

**Origen ESC:** ESC-003
**Verifica:** R-TareaFirma-001, U-documentos-pendientes-de-firma-005, U-documentos-pendientes-de-firma-017, U-documentos-pendientes-de-firma-018, U-documentos-pendientes-de-firma-011
**Pantalla principal:** screen-documentos-pendientes-de-firma.md
**Tipo:** error

### Precondiciones
- El estado inicial de la base de datos descrito arriba.
- «Firma de prueba 3» tiene **dos** documentos.

### Pasos
1. **Dado** que el Administrador inicia sesión con usuario «admin» y contraseña «admin».
2. **Y** abre la pantalla de certificados digitales y, si ya existe una entrada para el DNI «85432016B», la borra.
3. **Cuando** pulsa «Añadir certificado digital».
4. **Y** rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12», deja «Contraseña» vacía, y deja marcado «Habilitado».
5. **Y** pulsa «Guardar» y cierra sesión.
6. **Y** el director inicia sesión con «director@mislata.es» y contraseña «demo1234».
7. **Y** abre «Firmar documentos → Pendientes» y entra en la tarea de firma «Firma de prueba 3».
8. **Y** pulsa «Firmar todos los documentos».
9. **Entonces** el sistema muestra el texto «Los documentos se firmarán en el servidor con su certificado digital. Introduzca la contraseña de su certificado.» junto a un campo «Contraseña» vacío.
10. **Cuando** escribe «claveequivocada» en «Contraseña» y pulsa «Firmar todos los documentos y finalizar».
11. **Entonces** el sistema muestra un error que empieza por «No se han podido firmar los documentos:», deja la pantalla en el paso de firmar y conserva «claveequivocada» escrita en el campo «Contraseña».
12. **Cuando** pulsa «Atrás».
13. **Entonces** el sistema vuelve al primer paso y la tarea sigue mostrando el estado «Pendiente de firmar».
14. **Cuando** entra en el primer documento del listado «Documentos a firmar», comprueba que **no** tiene versión firmada y vuelve al formulario de la tarea.
15. **Y** entra en el segundo documento del listado «Documentos a firmar», comprueba que **tampoco** tiene versión firmada y vuelve al formulario de la tarea.
16. **Y** pulsa «Firmar todos los documentos».
17. **Entonces** el sistema vuelve a mostrar el campo «Contraseña» **vacío**.
18. **Cuando** escribe «nadanada» en «Contraseña» y pulsa «Firmar todos los documentos y finalizar».
19. **Entonces** el sistema devuelve al firmante al listado de «Firmar documentos → Pendientes».

### Resultado esperado
- Tras el intento fallido: ninguno de los dos documentos tiene versión firmada y la tarea sigue «Pendiente de firmar» (garantía todo-o-nada de R-TareaFirma-001).
- Tras el intento correcto: el listado de «Firmar documentos → Pendientes» ya no muestra «Firma de prueba 3»; en «Firmar documentos → Firmados» aparece con estado «Firmado» y **sus dos documentos** tienen versión firmada.

---

## T-004 — La contraseña se deja en blanco

**Origen ESC:** ESC-004
**Verifica:** V-TareaFirma-006, U-documentos-pendientes-de-firma-005, U-documentos-pendientes-de-firma-010, U-documentos-pendientes-de-firma-017
**Pantalla principal:** screen-documentos-pendientes-de-firma.md
**Tipo:** error

### Precondiciones
- El estado inicial de la base de datos descrito arriba.

### Pasos
1. **Dado** que el Administrador inicia sesión con usuario «admin» y contraseña «admin».
2. **Y** abre la pantalla de certificados digitales y, si ya existe una entrada para el DNI «85432016B», la borra.
3. **Cuando** pulsa «Añadir certificado digital».
4. **Y** rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12», deja «Contraseña» vacía, y deja marcado «Habilitado».
5. **Y** pulsa «Guardar» y cierra sesión.
6. **Y** el director inicia sesión con «director@mislata.es» y contraseña «demo1234».
7. **Y** abre «Firmar documentos → Pendientes» y entra en la tarea de firma «Firma de prueba 5».
8. **Y** pulsa «Firmar todos los documentos».
9. **Entonces** el sistema muestra el texto «Los documentos se firmarán en el servidor con su certificado digital. Introduzca la contraseña de su certificado.» junto a un campo «Contraseña» vacío.
10. **Cuando**, sin escribir nada en «Contraseña», pulsa «Firmar todos los documentos y finalizar».
11. **Entonces** el sistema muestra «La contraseña es obligatoria» y no firma nada.
12. **Cuando** pulsa «Atrás».
13. **Entonces** el sistema vuelve al primer paso y la tarea sigue mostrando el estado «Pendiente de firmar».

### Resultado esperado
- El documento del listado «Documentos a firmar» **no** tiene versión firmada.
- «Firma de prueba 5» sigue apareciendo en «Firmar documentos → Pendientes».

---

## T-005 — Al salir del paso de firmar la clave tecleada no se conserva

**Origen ESC:** ESC-005
**Verifica:** U-documentos-pendientes-de-firma-011, U-documentos-pendientes-de-firma-005
**Pantalla principal:** screen-documentos-pendientes-de-firma.md
**Tipo:** UI

### Precondiciones
- El estado inicial de la base de datos descrito arriba.

### Pasos
1. **Dado** que el Administrador inicia sesión con usuario «admin» y contraseña «admin».
2. **Y** abre la pantalla de certificados digitales y, si ya existe una entrada para el DNI «85432016B», la borra.
3. **Cuando** pulsa «Añadir certificado digital».
4. **Y** rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12», deja «Contraseña» vacía, y deja marcado «Habilitado».
5. **Y** pulsa «Guardar» y cierra sesión.
6. **Y** el director inicia sesión con «director@mislata.es» y contraseña «demo1234».
7. **Y** abre «Firmar documentos → Pendientes» y entra en la tarea de firma «Firma de prueba 5».
8. **Y** pulsa «Firmar todos los documentos».
9. **Entonces** el sistema muestra el campo «Contraseña» vacío.
10. **Cuando** escribe «nadanada» en «Contraseña».
11. **Y** pulsa «Atrás».
12. **Entonces** el sistema vuelve al primer paso, donde se ofrecen «Rechazar firmar» y «Firmar todos los documentos».
13. **Cuando** pulsa «Firmar todos los documentos».

### Resultado esperado
- El sistema muestra el campo «Contraseña» **vacío**: lo tecleado antes no se ha conservado.

---

## T-006 — Sin certificado en el servidor se ofrece AutoFirma

**Origen ESC:** ESC-006
**Verifica:** U-documentos-pendientes-de-firma-001, U-documentos-pendientes-de-firma-012, U-documentos-pendientes-de-firma-016
**Pantalla principal:** screen-documentos-pendientes-de-firma.md
**Tipo:** UI

### Precondiciones
- El estado inicial de la base de datos descrito arriba.

### Pasos
1. **Dado** que el Administrador inicia sesión con usuario «admin» y contraseña «admin».
2. **Y** abre la pantalla de certificados digitales y, si ya existe una entrada para el DNI «29050788V», la borra: así el secretario queda **sin** certificado digital en el servidor sea cual sea el orden en que se ejecuten los tests.
3. **Y** cierra sesión.
4. **Cuando** el secretario inicia sesión con «secretario@mislata.es» y contraseña «demo1234».
5. **Y** abre «Firmar documentos → Pendientes» y entra en la tarea de firma «Firma de prueba del secretario».
6. **Y** pulsa «Firmar todos los documentos».

### Resultado esperado
- El sistema muestra en el paso de firmar el aviso de que para firmar hace falta tener AutoFirma instalada y un certificado digital válido, junto con el enlace de descarga de AutoFirma.
- El sistema muestra el botón «Firmar todos los documentos con AutoFirma y finalizar» y **no** muestra el botón «Firmar todos los documentos y finalizar».
- El sistema **no** muestra ningún campo «PIN» ni ningún campo «Contraseña».

---

## T-007 — Al darle de alta un certificado deja de ofrecerse AutoFirma

**Origen ESC:** ESC-007
**Verifica:** U-documentos-pendientes-de-firma-001, U-documentos-pendientes-de-firma-004, U-documentos-pendientes-de-firma-012, U-documentos-pendientes-de-firma-013
**Pantalla principal:** screen-documentos-pendientes-de-firma.md
**Tipo:** happy

### Precondiciones
- El estado inicial de la base de datos descrito arriba.

### Pasos
1. **Dado** que el Administrador inicia sesión con usuario «admin» y contraseña «admin».
2. **Y** abre la pantalla de certificados digitales y, si ya existe una entrada para el DNI «29050788V», la borra.
3. **Y** cierra sesión.
4. **Cuando** el secretario inicia sesión con «secretario@mislata.es» y contraseña «demo1234».
5. **Y** abre «Firmar documentos → Pendientes», entra en «Firma de prueba del secretario» y pulsa «Firmar todos los documentos».
6. **Entonces** el sistema muestra el botón «Firmar todos los documentos con AutoFirma y finalizar».
7. **Cuando** pulsa «Atrás» y cierra sesión.
8. **Y** el Administrador inicia sesión con usuario «admin» y contraseña «admin».
9. **Y** abre la pantalla de certificados digitales y pulsa «Añadir certificado digital».
10. **Y** rellena «DNI» con «29050788V», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12» y «Contraseña» con «nadanada», y deja marcado «Habilitado».
11. **Y** pulsa «Guardar» y cierra sesión.
12. **Y** el secretario vuelve a iniciar sesión con «secretario@mislata.es» y contraseña «demo1234».
13. **Y** abre «Firmar documentos → Pendientes», entra en «Firma de prueba del secretario» y pulsa «Firmar todos los documentos».

### Resultado esperado
- El sistema muestra el texto «Los documentos se firmarán en el servidor con su certificado digital.» y el botón «Firmar todos los documentos y finalizar».
- El sistema ya **no** muestra el botón «Firmar todos los documentos con AutoFirma y finalizar».

---

## T-008 — Un firmante sin DNI no puede firmar

**Origen ESC:** ESC-008
**Verifica:** U-documentos-pendientes-de-firma-006, U-documentos-pendientes-de-firma-014, U-documentos-pendientes-de-firma-015
**Pantalla principal:** screen-documentos-pendientes-de-firma.md
**Tipo:** error

### Precondiciones
- El estado inicial de la base de datos descrito arriba: el usuario «admin» no tiene DNI.

### Pasos
1. **Dado** que el Administrador inicia sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú «Firmar documentos → Pendientes».
3. **Entonces** el listado muestra la tarea de firma «Firma de prueba del administrador 1».
4. **Cuando** pulsa la fila «Firma de prueba del administrador 1» y se abre el formulario de firma.
5. **Y** pulsa «Firmar todos los documentos».
6. **Entonces** el sistema muestra «No es posible firmar los documentos porque su usuario no tiene un DNI. Póngase en contacto con el administrador.» y no muestra ningún botón de firmar: ni «Firmar todos los documentos con AutoFirma y finalizar» ni «Firmar todos los documentos y finalizar».
7. **Cuando** pulsa «Atrás».
8. **Entonces** el sistema vuelve al primer paso de la pantalla, donde se elige entre rechazar y firmar.

### Resultado esperado
- Al volver a abrir «Firmar documentos → Pendientes», «Firma de prueba del administrador 1» sigue apareciendo en el listado, sin firmar.

---

## T-009 — Un firmante sin DNI sí puede rechazar la firma

**Origen ESC:** ESC-009
**Verifica:** —
**Pantalla principal:** screen-documentos-pendientes-de-firma.md
**Tipo:** happy

### Precondiciones
- El estado inicial de la base de datos descrito arriba.

### Pasos
1. **Dado** que el Administrador inicia sesión con usuario «admin» y contraseña «admin».
2. **Cuando** abre el menú «Firmar documentos → Pendientes».
3. **Entonces** el listado muestra la tarea de firma «Firma de prueba del administrador 2».
4. **Cuando** pulsa la fila «Firma de prueba del administrador 2» y se abre el formulario de firma.
5. **Y** pulsa «Rechazar firmar».
6. **Y** escribe «No me corresponde firmar estos documentos» en el motivo del rechazo.
7. **Y** pulsa «Finalizar».

### Resultado esperado
- El sistema deja la tarea en estado «Rechazada la firma».
- Al volver a abrir «Firmar documentos → Pendientes», el listado ya no muestra «Firma de prueba del administrador 2».

---

## T-010 — Cada firmante solo ve sus propias tareas pendientes

**Origen ESC:** ESC-010
**Verifica:** —
**Pantalla principal:** screen-documentos-pendientes-de-firma.md
**Tipo:** UI

### Precondiciones
- El estado inicial de la base de datos descrito arriba.

### Pasos
1. **Dado** que el director inicia sesión con «director@mislata.es» y contraseña «demo1234».
2. **Cuando** abre «Firmar documentos → Pendientes».
3. **Entonces** el listado muestra «Firma de prueba 5» y no muestra ni «Firma de prueba del secretario» ni ninguna «Firma de prueba del administrador».
4. **Cuando** el director cierra sesión y el secretario inicia sesión con «secretario@mislata.es» y contraseña «demo1234».
5. **Y** abre «Firmar documentos → Pendientes».
6. **Entonces** el listado muestra «Firma de prueba del secretario» y no muestra ninguna «Firma de prueba» numerada del director ni ninguna «Firma de prueba del administrador».
7. **Cuando** el secretario cierra sesión y el Administrador inicia sesión con usuario «admin» y contraseña «admin».
8. **Y** abre «Firmar documentos → Pendientes».

### Resultado esperado
- El listado del Administrador muestra «Firma de prueba del administrador 1» y, pese a ser administrador, no muestra ninguna «Firma de prueba» numerada del director ni «Firma de prueba del secretario».

---

## T-011 — El rechazo sigue funcionando para un firmante con certificado en el servidor

**Origen ESC:** ESC-011
**Verifica:** R-TareaFirma-004
**Pantalla principal:** screen-documentos-pendientes-de-firma.md
**Tipo:** happy

### Precondiciones
- El estado inicial de la base de datos descrito arriba.

### Pasos
1. **Dado** que el Administrador inicia sesión con usuario «admin» y contraseña «admin».
2. **Y** abre la pantalla de certificados digitales y, si ya existe una entrada para el DNI «85432016B», la borra.
3. **Cuando** pulsa «Añadir certificado digital».
4. **Y** rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12» y «Contraseña» con «nadanada», y deja marcado «Habilitado».
5. **Y** pulsa «Guardar» y cierra sesión.
6. **Y** el director inicia sesión con «director@mislata.es» y contraseña «demo1234».
7. **Y** abre «Firmar documentos → Pendientes» y entra en la tarea de firma «Firma de prueba 4».
8. **Y** pulsa «Rechazar firmar».
9. **Y** escribe «Estos documentos no me corresponden» en el motivo del rechazo.
10. **Y** pulsa «Finalizar».

### Resultado esperado
- El sistema deja la tarea «Firma de prueba 4» en estado «Rechazada la firma».
- «Firma de prueba 4» ya no aparece en «Firmar documentos → Pendientes».
- En «Firmar documentos → Rechazados», al entrar en «Firma de prueba 4» y abrir su documento, este **no** tiene versión firmada.

---

## T-012 — Al deshabilitar su certificado se vuelve a ofrecer AutoFirma

**Origen ESC:** ESC-012
**Verifica:** U-documentos-pendientes-de-firma-001, U-documentos-pendientes-de-firma-004, U-documentos-pendientes-de-firma-012, U-documentos-pendientes-de-firma-013
**Pantalla principal:** screen-documentos-pendientes-de-firma.md
**Tipo:** happy

### Precondiciones
- El estado inicial de la base de datos descrito arriba.

### Pasos
1. **Dado** que el Administrador inicia sesión con usuario «admin» y contraseña «admin».
2. **Y** abre la pantalla de certificados digitales y, si ya existe una entrada para el DNI «85432016B», la borra.
3. **Cuando** pulsa «Añadir certificado digital».
4. **Y** rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12» y «Contraseña» con «nadanada», y deja marcado «Habilitado».
5. **Y** pulsa «Guardar» y cierra sesión.
6. **Y** el director inicia sesión con «director@mislata.es» y contraseña «demo1234».
7. **Y** abre «Firmar documentos → Pendientes», entra en «Firma de prueba 5» y pulsa «Firmar todos los documentos».
8. **Entonces** el sistema muestra el texto «Los documentos se firmarán en el servidor con su certificado digital.» y el botón «Firmar todos los documentos y finalizar».
9. **Cuando** pulsa «Atrás» y cierra sesión.
10. **Y** el Administrador inicia sesión con usuario «admin» y contraseña «admin».
11. **Y** abre la pantalla de certificados digitales, entra en el certificado del DNI «85432016B», desmarca «Habilitado» y pulsa «Guardar».
12. **Y** cierra sesión.
13. **Y** el director vuelve a iniciar sesión con «director@mislata.es» y contraseña «demo1234».
14. **Y** abre «Firmar documentos → Pendientes», entra en «Firma de prueba 5» y pulsa «Firmar todos los documentos».

### Resultado esperado
- El sistema muestra el aviso de que para firmar hace falta tener AutoFirma instalada y un certificado digital válido, junto con el enlace de descarga de AutoFirma.
- El sistema muestra el botón «Firmar todos los documentos con AutoFirma y finalizar».
- El sistema ya **no** muestra el botón «Firmar todos los documentos y finalizar» ni el texto «Los documentos se firmarán en el servidor con su certificado digital.».

---

## T-013 — La clave tecleada no se ve en pantalla

**Origen ESC:** ESC-013
**Verifica:** U-documentos-pendientes-de-firma-009, U-documentos-pendientes-de-firma-005, U-documentos-pendientes-de-firma-008
**Pantalla principal:** screen-documentos-pendientes-de-firma.md
**Tipo:** UI

### Precondiciones
- El estado inicial de la base de datos descrito arriba.

### Pasos
1. **Dado** que el Administrador inicia sesión con usuario «admin» y contraseña «admin».
2. **Y** abre la pantalla de certificados digitales y, si ya existe una entrada para el DNI «85432016B», la borra.
3. **Cuando** pulsa «Añadir certificado digital».
4. **Y** rellena «DNI» con «85432016B», elige en «Tipo de certificado» la opción «Usar un fichero con el certificado que ya está dentro del del WAR», rellena «Ruta classpath» con «firma/mi_certificado.p12», deja «Contraseña» vacía, y deja marcado «Habilitado».
5. **Y** pulsa «Guardar» y cierra sesión.
6. **Y** el director inicia sesión con «director@mislata.es» y contraseña «demo1234».
7. **Y** abre «Firmar documentos → Pendientes» y entra en la tarea de firma «Firma de prueba 5».
8. **Y** pulsa «Firmar todos los documentos».
9. **Entonces** el sistema muestra el texto «Los documentos se firmarán en el servidor con su certificado digital. Introduzca la contraseña de su certificado.» junto a un campo «Contraseña» vacío.
10. **Cuando** escribe «nadanada» en el campo «Contraseña».

### Resultado esperado
- El sistema muestra el contenido del campo «Contraseña» oculto: en la pantalla no se lee «nadanada», sino caracteres enmascarados.
- El valor real del campo «Contraseña» sí es «nadanada» (se comprueba sobre el valor del campo, no sobre el texto visible).
