# Tests E2E — Justificación de falta del profesorado (`JustificacionFaltaProfesoradoV1`)

Tests en lenguaje de negocio, Given/When/Then, materializados a partir de los escenarios (`ESC-NNN`) de la especificación y de las transiciones que el delta atraviesa. **Sin código Playwright y sin selectores.**

Esta es una iniciativa de **modificación** de una versión existente, así que la cobertura se mide **solo sobre el delta**: la acción `PRESENTAR` del estado `RECEPCION / PENDIENTE_PRESENTACION`, la acción `BACK` desde ese mismo estado y la pantalla del perfil `CREADOR` de ese estado, más el camino existente que el delta atraviesa (`GUARDAR_DATOS`), que se ejercita como no-regresión. Nada de la fase `TRAMITACION` se vuelve a probar: el delta no la toca.

> **CRITICAL — en esta iniciativa no se escriben tests.** Las guías de diseño excluyen expresamente escribir tests (unitarios, de arquitectura y E2E). Este fichero es el **criterio de aceptación** de la verificación en runtime del último paso del diseño; **MUST NOT** ejecutarse `/sdd-debug-with-test-e2e-desc` ni `/sdd-create-tests-e2e` sobre esta iniciativa, ni generarse ningún `.spec.ts`.

> **Cómo se leen aquí los mensajes de error.** El pie del formulario antepone **siempre** la etiqueta del campo del que cuelga la regla, que en este estado es `Firma de la solicitud` (`design.md` §4.1 y §14): un error citado como «La contraseña es obligatoria» se ve en pantalla como «Firma de la solicitud: La contraseña es obligatoria». Cada `Then` comprueba que el texto citado **aparece** en el mensaje, no que sea la línea entera.

## Actores

| Login | Contraseña | Tipo / Cargo | Centro | Perfil | Vía |
|---|---|---|---|---|---|
| `admin` | `admin` | Administrador (ve todos los centros) | — | — (no interviene en el expediente) | — |
| `director@mislata.es` | `demo1234` | tipo de usuario `PROFESOR`, cargo `DIRECTOR`, documento de identidad `85432016B` | CIPFP Mislata (`46019660`) | `CREADOR` | `tramiteCode` |
| `secretario@mislata.es` | `demo1234` | tipo de usuario `PROFESOR`, cargo `SECRETARIO`, documento de identidad `29050788V` | CIPFP Mislata (`46019660`) | `CREADOR` | `tramiteCode` |
| `jefeestudios1@mislata.es` | `demo1234` | tipo de usuario `PROFESOR`, cargo `JEFE_ESTUDIOS`, documento de identidad `15519084H` | CIPFP Mislata (`46019660`) | `RESPONSABLE` | `tipoExpedienteCode` |

`admin` solo se usa para dar de alta, modificar o deshabilitar certificados digitales en la pantalla de certificados digitales: no crea, no firma y no tramita ningún expediente.

## Datos de demo

Estado previo del que parten **todos** los tests: la carga de demo (`data.import.demo-data = true`) con sus centros, usuarios y perfiles, más el trámite «Justificación de falta del profesorado» publicado en el árbol de trámites del centro CIPFP Mislata. Ningún test puede presuponer más estado que este.

En particular, **ningún test puede presuponer que ya existe un certificado digital**: el trámite no precarga ninguno. Cada test que necesite uno lo da de alta él mismo con `admin` en su `Given`, y cada test que necesite que **no** exista lo comprueba en el listado.

### Juego de datos válido — fase `RECEPCION`, estado `ENTRADA_DATOS`

| campo | valor |
|---|---|
| «Días» | el número que indique cada test (uno distinto por test, para poder localizar el expediente después) |
| «Mes» | `Octubre` |
| «Año» | el año en curso, que es el que propone el sistema |
| «Tipo de jornada» | `Toda la jornada` |
| «Motivo» | `Enfermedad común` |
| «Foto o PDF del justificante» | `justificante.pdf`, un documento PDF de menos de 5 MB |

### Certificado digital de ejemplo

El que ya viaja dentro de la aplicación. Se da de alta desde la pantalla de certificados digitales con:

| campo | valor |
|---|---|
| «DNI» | el documento de identidad del profesor al que se le quiere dar (`85432016B` el director, `29050788V` el secretario) |
| «Tipo de certificado» | `Usar un fichero con el certificado que ya está dentro del del WAR` |
| «Ruta classpath» | `firma/mi_certificado.p12` |
| «Contraseña» | `nadanada` si el test quiere que la secretaría virtual la custodie; vacía si quiere que se la pida al profesor; `claveequivocada` si quiere una clave custodiada que no abre el certificado |
| «Habilitado» | marcado, salvo el test que lo desmarca a propósito |

### Juego de datos — fase `RECEPCION`, estado `PENDIENTE_PRESENTACION`

| campo | valor |
|---|---|
| «Contraseña» | `nadanada` (la del certificado de ejemplo); `claveequivocada` en el test de contraseña incorrecta; vacía en el test de contraseña en blanco |
| «PIN» | no se usa en ningún test: exige un dispositivo criptográfico físico, que no existe en el entorno de pruebas |

## Cobertura de transiciones

| # | fase origen | estado origen | evento | guarda | fase destino | estado destino | perfil | test |
|---|---|---|---|---|---|---|---|---|
| 1 | `RECEPCION` | `ENTRADA_DATOS` | `GUARDAR_DATOS` | — | `RECEPCION` | `PENDIENTE_PRESENTACION` | `CREADOR` | T-008 (no-regresión); lo atraviesan además T-001…T-007 y T-009…T-013 |
| 2 | `RECEPCION` | `PENDIENTE_PRESENTACION` | `PRESENTAR` | existe certificado digital habilitado para el documento de identidad del profesor (firma en el servidor) | `TRAMITACION` | `PENDIENTE_RESOLUCION` | `CREADOR` | T-001, T-002, T-003 |
| 3 | `RECEPCION` | `PENDIENTE_PRESENTACION` | `PRESENTAR` | no existe certificado digital habilitado para el documento de identidad del profesor (firma en el equipo del profesor) | `TRAMITACION` | `PENDIENTE_RESOLUCION` | `CREADOR` | T-013 (manual) |
| 4 | `RECEPCION` | `PENDIENTE_PRESENTACION` | `BACK` | — | `RECEPCION` | `ENTRADA_DATOS` | `CREADOR` | T-005 |

Estados que el delta atraviesa y que los tests mencionan: `RECEPCION / ENTRADA_DATOS` (partida y vuelta), `RECEPCION / PENDIENTE_PRESENTACION` (la pantalla que cambia, con y sin el turno) y `TRAMITACION / PENDIENTE_RESOLUCION` (llegada). `TRAMITACION / ACEPTADO` y `TRAMITACION / RECHAZADO` no se prueban: el delta no toca la fase de tramitación ni ninguna de sus transiciones, y volver a probarlos sería re-especificar lo que la especificación deja fuera de alcance.

Tests de validación fallida: T-003, T-004, T-007. Cubren la única pareja (estado, evento) cuyas reglas cambia el delta, `(PENDIENTE_PRESENTACION, PRESENTAR)`. La pareja `(PENDIENTE_PRESENTACION, BACK)` no tiene test de validación fallida porque su validador es `rules { }` vacío, y `DELETE` está exento de validación.
Tests de vistas genéricas de solo lectura: T-012.
Tests **manuales** (no automatizables): T-013.

---

## T-001 — Se firma en el servidor con la contraseña del certificado ya guardada

**Origen ESC:** ESC-001
**Perfil:** `CREADOR` (login `director@mislata.es`)
**Desde:** `RECEPCION` / `PENDIENTE_PRESENTACION`
**Evento:** `PRESENTAR` — botón «Firmar y Presentar la solicitud»
**Hasta:** `TRAMITACION` / `PENDIENTE_RESOLUCION`
**Tipo:** happy
**Manual:** no

- **Given** el Administrador ha iniciado sesión con «admin» / «admin», ha abierto la pantalla de certificados digitales, ha añadido un certificado con «DNI» `85432016B`, «Tipo de certificado» «Usar un fichero con el certificado que ya está dentro del del WAR», «Ruta classpath» `firma/mi_certificado.p12`, «Contraseña» `nadanada` y «Habilitado» marcado, ha comprobado que aparece así en el listado y ha cerrado sesión; y el director ha iniciado sesión con «director@mislata.es» / «demo1234», ha creado un expediente nuevo del trámite «Justificación de falta del profesorado», ha rellenado el juego de datos válido con «Días» `3` y ha pulsado «Siguiente», con lo que el expediente ha quedado en `RECEPCION` / `PENDIENTE_PRESENTACION` mostrando la solicitud generada.
- **And** la pantalla muestra el aviso «La solicitud se firmará en el servidor con su certificado digital.», no muestra ningún campo «Contraseña» ni ningún campo «PIN», muestra el botón «Firmar y Presentar la solicitud» y no muestra el botón «Firmar con AutoFirma y Presentar la solicitud».
- **When** pulsa el botón «Firmar y Presentar la solicitud» (evento `PRESENTAR`) y confirma el aviso «¿Esta seguro que desea presentar la documentación? No podrá deshacer esta acción».
- **Then** el expediente pasa a la fase `TRAMITACION`, estado `PENDIENTE_RESOLUCION`, y la cabecera muestra «Tramitación» y «Pendiente de resolución».
- **And** al volver a abrir el expediente desde la lista de sus expedientes de «Justificación de falta del profesorado» —el de `3` días de «Octubre»— la solicitud presentada aparece **firmada**, y el expediente tiene guardado el justificante del registro de entrada.

---

## T-002 — Se firma en el servidor tecleando la contraseña del certificado

**Origen ESC:** ESC-002
**Perfil:** `CREADOR` (login `director@mislata.es`)
**Desde:** `RECEPCION` / `PENDIENTE_PRESENTACION`
**Evento:** `PRESENTAR` — botón «Firmar y Presentar la solicitud»
**Hasta:** `TRAMITACION` / `PENDIENTE_RESOLUCION`
**Tipo:** happy
**Manual:** no

- **Given** el Administrador ha dado de alta con «admin» / «admin» un certificado con «DNI» `85432016B`, «Tipo de certificado» «Usar un fichero con el certificado que ya está dentro del del WAR», «Ruta classpath» `firma/mi_certificado.p12`, **«Contraseña» vacía** y «Habilitado» marcado, lo ha visto en el listado y ha cerrado sesión; y el director ha iniciado sesión con «director@mislata.es» / «demo1234», ha creado un expediente del trámite, ha rellenado el juego de datos válido con «Días» `4` y ha pulsado «Siguiente», con lo que el expediente ha quedado en `RECEPCION` / `PENDIENTE_PRESENTACION`.
- **And** la pantalla muestra el aviso «La solicitud se firmará en el servidor con su certificado digital. Introduzca la contraseña de su certificado.» junto a un campo «Contraseña» vacío, y no muestra el botón «Firmar con AutoFirma y Presentar la solicitud».
- **When** escribe `nadanada` en «Contraseña», pulsa «Firmar y Presentar la solicitud» (evento `PRESENTAR`) y confirma el aviso «¿Esta seguro que desea presentar la documentación? No podrá deshacer esta acción».
- **Then** el expediente pasa a la fase `TRAMITACION`, estado `PENDIENTE_RESOLUCION`.
- **And** al volver a abrir el expediente desde la lista de sus expedientes —el de `4` días de «Octubre»— la solicitud presentada aparece **firmada**.

---

## T-003 — Una contraseña incorrecta no presenta nada y se puede reintentar

**Origen ESC:** ESC-003
**Perfil:** `CREADOR` (login `director@mislata.es`)
**Desde:** `RECEPCION` / `PENDIENTE_PRESENTACION`
**Evento:** `PRESENTAR` — botón «Firmar y Presentar la solicitud»
**Hasta:** `RECEPCION` / `PENDIENTE_PRESENTACION` en el intento fallido; `TRAMITACION` / `PENDIENTE_RESOLUCION` en el reintento
**Tipo:** error
**Manual:** no

- **Given** el Administrador ha dado de alta con «admin» / «admin» un certificado con «DNI» `85432016B`, tipo «Usar un fichero con el certificado que ya está dentro del del WAR», «Ruta classpath» `firma/mi_certificado.p12`, «Contraseña» vacía y «Habilitado» marcado, y ha cerrado sesión; y el director ha iniciado sesión con «director@mislata.es» / «demo1234», ha creado un expediente del trámite, ha rellenado el juego de datos válido con «Días» `5`, ha pulsado «Siguiente» y el expediente ha quedado en `RECEPCION` / `PENDIENTE_PRESENTACION` con el campo «Contraseña» vacío.
- **When** escribe `claveequivocada` en «Contraseña», pulsa «Firmar y Presentar la solicitud» (evento `PRESENTAR`) y confirma el aviso «¿Esta seguro que desea presentar la documentación? No podrá deshacer esta acción».
- **Then** el sistema muestra el error «La contraseña indicada no es correcta» y el expediente **sigue** en la fase `RECEPCION`, estado `PENDIENTE_PRESENTACION`: la solicitud no se ha firmado y no se ha dejado constancia de ninguna entrada.
- **And** el campo «Contraseña» vuelve a mostrarse **vacío**: la clave del intento fallido no se conserva.
- **And** al escribir después `nadanada` en «Contraseña», pulsar «Firmar y Presentar la solicitud» y confirmar, el expediente pasa a la fase `TRAMITACION`, estado `PENDIENTE_RESOLUCION`, con la solicitud firmada y la constancia de la entrada.

---

## T-004 — La contraseña se deja en blanco

**Origen ESC:** ESC-004
**Perfil:** `CREADOR` (login `director@mislata.es`)
**Desde:** `RECEPCION` / `PENDIENTE_PRESENTACION`
**Evento:** `PRESENTAR` — botón «Firmar y Presentar la solicitud»
**Hasta:** `RECEPCION` / `PENDIENTE_PRESENTACION` (no transiciona)
**Tipo:** error
**Manual:** no

- **Given** el Administrador ha dado de alta con «admin» / «admin» un certificado con «DNI» `85432016B`, tipo «Usar un fichero con el certificado que ya está dentro del del WAR», «Ruta classpath» `firma/mi_certificado.p12`, «Contraseña» vacía y «Habilitado» marcado, y ha cerrado sesión; y el director ha iniciado sesión con «director@mislata.es» / «demo1234», ha creado un expediente del trámite, ha rellenado el juego de datos válido con «Días» `6`, ha pulsado «Siguiente» y el expediente ha quedado en `RECEPCION` / `PENDIENTE_PRESENTACION` con el campo «Contraseña» vacío.
- **When** deja el campo «Contraseña» vacío, pulsa «Firmar y Presentar la solicitud» (evento `PRESENTAR`) y confirma el aviso «¿Esta seguro que desea presentar la documentación? No podrá deshacer esta acción».
- **Then** el sistema muestra el error «La contraseña es obligatoria» y el expediente **sigue** en la fase `RECEPCION`, estado `PENDIENTE_PRESENTACION`.
- **And** no se ha firmado nada y no se ha dejado constancia de ninguna entrada.

---

## T-005 — La contraseña tecleada no se conserva al volver atrás

**Origen ESC:** ESC-005
**Perfil:** `CREADOR` (login `director@mislata.es`)
**Desde:** `RECEPCION` / `PENDIENTE_PRESENTACION`
**Evento:** `BACK` — botón «Atrás»
**Hasta:** `RECEPCION` / `ENTRADA_DATOS`
**Tipo:** happy
**Manual:** no

- **Given** el Administrador ha dado de alta con «admin» / «admin» un certificado con «DNI» `85432016B`, tipo «Usar un fichero con el certificado que ya está dentro del del WAR», «Ruta classpath» `firma/mi_certificado.p12`, «Contraseña» vacía y «Habilitado» marcado, y ha cerrado sesión; y el director ha iniciado sesión con «director@mislata.es» / «demo1234», ha creado un expediente del trámite, ha rellenado el juego de datos válido con «Días» `7`, ha pulsado «Siguiente» y el expediente ha quedado en `RECEPCION` / `PENDIENTE_PRESENTACION` con el campo «Contraseña» vacío.
- **When** escribe `nadanada` en «Contraseña» y pulsa el botón «Atrás» (evento `BACK`).
- **Then** el expediente vuelve a la fase `RECEPCION`, estado `ENTRADA_DATOS`, y muestra los datos de la falta tal como se rellenaron: «Días» `7`, «Mes» «Octubre», el año en curso en «Año», «Tipo de jornada» «Toda la jornada», «Motivo» «Enfermedad común» y el justificante adjunto.
- **And** al pulsar «Siguiente» (evento `GUARDAR_DATOS`) el expediente vuelve a `RECEPCION` / `PENDIENTE_PRESENTACION` y el campo «Contraseña» se muestra **vacío**.

---

## T-006 — La contraseña tecleada no se ve en pantalla

**Origen ESC:** ESC-006
**Perfil:** `CREADOR` (login `director@mislata.es`)
**Desde:** `RECEPCION` / `PENDIENTE_PRESENTACION`
**Evento:** — (no se dispara ningún evento)
**Hasta:** `RECEPCION` / `PENDIENTE_PRESENTACION` (no transiciona)
**Tipo:** solo-lectura
**Manual:** no

- **Given** el Administrador ha dado de alta con «admin» / «admin» un certificado con «DNI» `85432016B`, tipo «Usar un fichero con el certificado que ya está dentro del del WAR», «Ruta classpath» `firma/mi_certificado.p12`, «Contraseña» vacía y «Habilitado» marcado, y ha cerrado sesión; y el director ha iniciado sesión con «director@mislata.es» / «demo1234», ha creado un expediente del trámite, ha rellenado el juego de datos válido con «Días» `8` y ha pulsado «Siguiente».
- **And** el expediente está en `RECEPCION` / `PENDIENTE_PRESENTACION` y la pantalla muestra el aviso «La solicitud se firmará en el servidor con su certificado digital. Introduzca la contraseña de su certificado.» junto a un campo «Contraseña» vacío.
- **When** escribe `nadanada` en el campo «Contraseña».
- **Then** el contenido del campo «Contraseña» se muestra **oculto**: en la pantalla no se lee `nadanada`, sino caracteres enmascarados.
- **And** el expediente sigue en la fase `RECEPCION`, estado `PENDIENTE_PRESENTACION`.

---

## T-007 — La clave que custodia la secretaría virtual no abre el certificado

**Origen ESC:** ESC-010
**Perfil:** `CREADOR` (login `director@mislata.es`)
**Desde:** `RECEPCION` / `PENDIENTE_PRESENTACION`
**Evento:** `PRESENTAR` — botón «Firmar y Presentar la solicitud»
**Hasta:** `RECEPCION` / `PENDIENTE_PRESENTACION` (no transiciona)
**Tipo:** error
**Manual:** no

- **Given** el Administrador ha dado de alta con «admin» / «admin» un certificado con «DNI» `85432016B`, tipo «Usar un fichero con el certificado que ya está dentro del del WAR», «Ruta classpath» `firma/mi_certificado.p12`, **«Contraseña» `claveequivocada`** y «Habilitado» marcado, lo ha visto en el listado y ha cerrado sesión; y el director ha iniciado sesión con «director@mislata.es» / «demo1234», ha creado un expediente del trámite, ha rellenado el juego de datos válido con «Días» `12` y ha pulsado «Siguiente».
- **And** el expediente está en `RECEPCION` / `PENDIENTE_PRESENTACION`, la pantalla muestra el aviso «La solicitud se firmará en el servidor con su certificado digital.» y no muestra ningún campo «Contraseña».
- **When** pulsa «Firmar y Presentar la solicitud» (evento `PRESENTAR`) y confirma el aviso «¿Esta seguro que desea presentar la documentación? No podrá deshacer esta acción».
- **Then** el sistema muestra el error «La clave guardada de su certificado digital no es correcta. Póngase en contacto con el administrador» y el expediente **sigue** en la fase `RECEPCION`, estado `PENDIENTE_PRESENTACION`.
- **And** no se ha firmado la solicitud y no se ha dejado constancia de ninguna entrada.

---

## T-008 — Sin certificado custodiado se sigue ofreciendo la firma en el equipo del profesor

**Origen ESC:** ESC-007
**Perfil:** `CREADOR` (login `secretario@mislata.es`)
**Desde:** `RECEPCION` / `ENTRADA_DATOS`
**Evento:** `GUARDAR_DATOS` — botón «Siguiente»
**Hasta:** `RECEPCION` / `PENDIENTE_PRESENTACION`
**Tipo:** happy
**Manual:** no

Es además el test de **no-regresión** del camino que el delta atraviesa: la entrada de datos y el paso a «Pendiente de presentación» siguen funcionando exactamente igual que antes del cambio.

- **Given** el secretario ha iniciado sesión con «secretario@mislata.es» / «demo1234», no hay ningún certificado digital dado de alta para su documento de identidad `29050788V`, ha creado un expediente nuevo del trámite «Justificación de falta del profesorado» y ha rellenado el juego de datos válido con «Días» `9`.
- **When** pulsa el botón «Siguiente» (evento `GUARDAR_DATOS`).
- **Then** el expediente pasa a la fase `RECEPCION`, estado `PENDIENTE_PRESENTACION`, y muestra la solicitud generada.
- **And** la pantalla muestra el aviso «Para presentar la solicitud debe tener la aplicación de AutoFirma instalada y un certificado digital válido» y el botón «Firmar con AutoFirma y Presentar la solicitud», y **no** muestra el botón «Firmar y Presentar la solicitud», ni ningún campo «Contraseña», ni ningún campo «PIN».

---

## T-009 — Al darle de alta un certificado deja de ofrecerse la firma en el equipo

**Origen ESC:** ESC-008
**Perfil:** `CREADOR` (login `secretario@mislata.es`)
**Desde:** `RECEPCION` / `PENDIENTE_PRESENTACION`
**Evento:** — (no se dispara ningún evento)
**Hasta:** `RECEPCION` / `PENDIENTE_PRESENTACION` (no transiciona)
**Tipo:** solo-lectura
**Manual:** no

- **Given** el secretario ha iniciado sesión con «secretario@mislata.es» / «demo1234», ha creado un expediente del trámite, ha rellenado el juego de datos válido con «Días» `10`, ha pulsado «Siguiente», el expediente ha quedado en `RECEPCION` / `PENDIENTE_PRESENTACION` mostrando el botón «Firmar con AutoFirma y Presentar la solicitud», y ha cerrado sesión.
- **And** el Administrador ha iniciado sesión con «admin» / «admin», ha dado de alta en la pantalla de certificados digitales un certificado con «DNI» `29050788V`, «Tipo de certificado» «Usar un fichero con el certificado que ya está dentro del del WAR», «Ruta classpath» `firma/mi_certificado.p12`, «Contraseña» `nadanada` y «Habilitado» marcado, lo ha visto en el listado y ha cerrado sesión.
- **When** el secretario vuelve a iniciar sesión con «secretario@mislata.es» / «demo1234», abre la lista de sus expedientes de «Justificación de falta del profesorado» y abre el que dejó en «Pendiente de presentación», el de `10` días de «Octubre».
- **Then** el expediente sigue en la fase `RECEPCION`, estado `PENDIENTE_PRESENTACION`, y la pantalla muestra el aviso «La solicitud se firmará en el servidor con su certificado digital.» y el botón «Firmar y Presentar la solicitud».
- **And** ya **no** muestra el botón «Firmar con AutoFirma y Presentar la solicitud» ni el aviso «Para presentar la solicitud debe tener la aplicación de AutoFirma instalada y un certificado digital válido».

---

## T-010 — Al deshabilitar su certificado se vuelve a ofrecer la firma en el equipo

**Origen ESC:** ESC-009
**Perfil:** `CREADOR` (login `director@mislata.es`)
**Desde:** `RECEPCION` / `PENDIENTE_PRESENTACION`
**Evento:** — (no se dispara ningún evento)
**Hasta:** `RECEPCION` / `PENDIENTE_PRESENTACION` (no transiciona)
**Tipo:** solo-lectura
**Manual:** no

- **Given** el Administrador ha dado de alta con «admin» / «admin» un certificado con «DNI» `85432016B`, tipo «Usar un fichero con el certificado que ya está dentro del del WAR», «Ruta classpath» `firma/mi_certificado.p12`, «Contraseña» `nadanada` y «Habilitado» marcado, y ha cerrado sesión; y el director ha iniciado sesión con «director@mislata.es» / «demo1234», ha creado un expediente del trámite, ha rellenado el juego de datos válido con «Días» `11`, ha pulsado «Siguiente», el expediente ha quedado en `RECEPCION` / `PENDIENTE_PRESENTACION` mostrando el aviso «La solicitud se firmará en el servidor con su certificado digital.» y el botón «Firmar y Presentar la solicitud», y ha cerrado sesión.
- **And** el Administrador ha vuelto a iniciar sesión con «admin» / «admin», ha entrado en el certificado del DNI `85432016B`, ha desmarcado «Habilitado», ha guardado, lo ha visto en el listado ya no marcado como habilitado y ha cerrado sesión.
- **When** el director vuelve a iniciar sesión con «director@mislata.es» / «demo1234», abre la lista de sus expedientes de «Justificación de falta del profesorado» y abre el que dejó en «Pendiente de presentación», el de `11` días de «Octubre».
- **Then** el expediente sigue en la fase `RECEPCION`, estado `PENDIENTE_PRESENTACION`, y la pantalla muestra el aviso «Para presentar la solicitud debe tener la aplicación de AutoFirma instalada y un certificado digital válido» y el botón «Firmar con AutoFirma y Presentar la solicitud».
- **And** ya **no** muestra el botón «Firmar y Presentar la solicitud» ni el aviso «La solicitud se firmará en el servidor con su certificado digital.».

---

## T-011 — El certificado de otra persona no habilita la firma en el servidor

**Origen ESC:** ESC-011
**Perfil:** `CREADOR` (login `secretario@mislata.es`)
**Desde:** `RECEPCION` / `ENTRADA_DATOS`
**Evento:** `GUARDAR_DATOS` — botón «Siguiente»
**Hasta:** `RECEPCION` / `PENDIENTE_PRESENTACION`
**Tipo:** happy
**Manual:** no

- **Given** el Administrador ha iniciado sesión con «admin» / «admin», ha comprobado en el listado de certificados digitales que **no** hay ningún certificado para el DNI `29050788V`, ha dado de alta uno con «DNI» `85432016B`, tipo «Usar un fichero con el certificado que ya está dentro del del WAR», «Ruta classpath» `firma/mi_certificado.p12`, «Contraseña» `nadanada` y «Habilitado» marcado, y ha cerrado sesión.
- **And** el secretario, cuyo documento de identidad es `29050788V`, ha iniciado sesión con «secretario@mislata.es» / «demo1234», ha creado un expediente nuevo del trámite y ha rellenado el juego de datos válido con «Días» `13`.
- **When** pulsa el botón «Siguiente» (evento `GUARDAR_DATOS`).
- **Then** el expediente pasa a la fase `RECEPCION`, estado `PENDIENTE_PRESENTACION`, y la pantalla muestra el aviso «Para presentar la solicitud debe tener la aplicación de AutoFirma instalada y un certificado digital válido» y el botón «Firmar con AutoFirma y Presentar la solicitud».
- **And** **no** muestra el aviso «La solicitud se firmará en el servidor con su certificado digital.», ni el botón «Firmar y Presentar la solicitud», ni ningún campo «Contraseña»: el certificado de otra persona no le sirve.

---

## T-012 — Quien no tiene el turno ve el expediente en solo consulta, sin el bloque de firma

**Origen ESC:** ESC-012
**Perfil:** `RESPONSABLE` (login `jefeestudios1@mislata.es`)
**Desde:** `RECEPCION` / `PENDIENTE_PRESENTACION`
**Evento:** — (solo el botón «Salir», que no es un evento del estado)
**Hasta:** `RECEPCION` / `PENDIENTE_PRESENTACION` (no transiciona)
**Tipo:** solo-lectura
**Manual:** no

Es el test de la **vista genérica de solo lectura** del estado abierto `PENDIENTE_PRESENTACION`, que tiene perfil `CREADOR`: el expediente se abre por la **bandeja del perfil `RESPONSABLE`** (la lista de expedientes de «Justificación de falta del profesorado» del centro, que es la del jefe de estudios), y como no existe form para ese perfil en ese estado, el servidor cae en la vista genérica.

- **Given** el Administrador ha dado de alta con «admin» / «admin» un certificado con «DNI» `85432016B`, tipo «Usar un fichero con el certificado que ya está dentro del del WAR», «Ruta classpath» `firma/mi_certificado.p12`, «Contraseña» `nadanada` y «Habilitado» marcado, y ha cerrado sesión; y el director ha iniciado sesión con «director@mislata.es» / «demo1234», ha creado un expediente del trámite, ha rellenado el juego de datos válido con «Días» `14`, ha pulsado «Siguiente», el expediente ha quedado en `RECEPCION` / `PENDIENTE_PRESENTACION` mostrando el aviso «La solicitud se firmará en el servidor con su certificado digital.», y ha cerrado sesión.
- **When** el jefe de estudios inicia sesión con «jefeestudios1@mislata.es» / «demo1234», abre por su bandeja de `RESPONSABLE` la lista de expedientes de «Justificación de falta del profesorado» de su centro y abre el que el director dejó en «Pendiente de presentación», el de `14` días de «Octubre».
- **Then** el sistema muestra la solicitud generada en **solo consulta** y no muestra el aviso «La solicitud se firmará en el servidor con su certificado digital.», ni ningún campo «Contraseña», ni ningún campo «PIN», ni el botón «Firmar y Presentar la solicitud», ni el botón «Firmar con AutoFirma y Presentar la solicitud».
- **And** muestra un único botón «Salir»; al pulsarlo vuelve al listado y el expediente sigue en la fase `RECEPCION`, estado `PENDIENTE_PRESENTACION`, sin ningún cambio.

---

## T-013 — Sin certificado custodiado, la solicitud se firma y se presenta desde el equipo del profesor

**Origen ESC:** —
**Perfil:** `CREADOR` (login `secretario@mislata.es`)
**Desde:** `RECEPCION` / `PENDIENTE_PRESENTACION`
**Evento:** `PRESENTAR` — botón «Firmar con AutoFirma y Presentar la solicitud»
**Hasta:** `TRAMITACION` / `PENDIENTE_RESOLUCION`
**Tipo:** happy
**Manual:** sí — el paso «Firmar con AutoFirma y Presentar la solicitud» abre la aplicación de escritorio AutoFirma y exige que el certificado del profesor (documento de identidad `29050788V`) esté instalado en la máquina desde la que se prueba; ninguna automatización de navegador puede completarlo.

Cubre la rama de la transición `PRESENTAR` que se firma en el equipo del profesor: es el comportamiento que ya existía y que el delta conserva, ahora bajo la guarda «solo cuando no hay certificado custodiado». La especificación lo deja fuera de sus escenarios por este mismo motivo.

- **Given** no hay ningún certificado digital dado de alta para el documento de identidad `29050788V`; el secretario ha iniciado sesión con «secretario@mislata.es» / «demo1234», ha creado un expediente del trámite, ha rellenado el juego de datos válido con «Días» `15` y ha pulsado «Siguiente», con lo que el expediente ha quedado en `RECEPCION` / `PENDIENTE_PRESENTACION` mostrando el botón «Firmar con AutoFirma y Presentar la solicitud».
- **And** la máquina desde la que se prueba tiene instalada la aplicación de firma del ciudadano y un certificado digital válido cuyo documento de identidad es `29050788V`, el mismo con el que el expediente firma sus documentos de entrada.
- **When** pulsa «Firmar con AutoFirma y Presentar la solicitud» (evento `PRESENTAR`), firma la solicitud en su equipo y confirma el aviso «¿Esta seguro que desea presentar la documentación? No podrá deshacer esta acción».
- **Then** el expediente pasa a la fase `TRAMITACION`, estado `PENDIENTE_RESOLUCION`.
- **And** la solicitud presentada aparece firmada y el expediente tiene guardado el justificante del registro de entrada.
