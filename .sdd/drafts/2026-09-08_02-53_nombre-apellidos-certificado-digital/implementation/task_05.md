---
type: implementation-task
template: system
---

# Tarea 05 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-vistas

## Fichero ya materializado en el diseño — se COPIA LITERALMENTE

La vista **ya está materializada y validada con `xmllint`** por el diseñador en:

`/workspace/secretaria-virtual/.sdd/drafts/2026-09-08_02-53_nombre-apellidos-certificado-digital/design/views/Main-CertificadoDigital.xml`

**MUST** copiarse **literalmente** (byte a byte, fichero completo resultante) a su ruta destino:

`/workspace/secretaria-virtual/src/main/java/com/educaflow/subsystem/criptografia/views/Main-CertificadoDigital.xml`

**MUST NOT** regenerarla, reescribirla ni reinterpretarla a partir del texto de abajo: el texto del diseño es la explicación del delta, el XML del `design/` es el contrato.

La fila es `Acción: Modificar`: el fichero destino **ya existe**. Antes de sobrescribirlo aplica la **comprobación de conservación** de `implementation.md` §3 (el «Resumen estructural» de abajo enumera todo lo preexistente que el fichero resultante debe conservar; si algo preexistente no estuviera en el XML del diseño, **detente y repórtalo** en vez de fusionar a mano).

## Fila de la tabla «Ficheros a crear o modificar»

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/criptografia/views/Main-CertificadoDigital.xml` | Modificar | k-vistas (grids.md, forms.md, actions.md) | Columnas y campos de nombre/apellidos, orden del listado, `readonlyIf`/`requiredIf`, `onChange` del DNI y su `action-method` |

## Texto del diseño (verbatim)

### Paso 6 — Vista `Main-CertificadoDigital.xml`

**Fichero del diseño:** `design/views/Main-CertificadoDigital.xml` → `src/main/java/com/educaflow/subsystem/criptografia/views/Main-CertificadoDigital.xml` (**Modificar**, fichero completo resultante). Un solo `<action-view>`, en su propio fichero, con nomenclatura `{Variante}-{Entidad}.xml`. Las cinco PI `sv-*` aparecen una vez cada una y en orden.

**Resumen estructural:**

- **Preexistente (se conserva):** el `<action-view>` `subsysCriptografia.Main@CertificadoDigital-action` con sus dos `<view>` y sus dos `<view-param>`; el `<grid>` con todos sus atributos salvo `orderBy`; el `<form>` con sus atributos (`can*` a `false`, `canBackOnSave="true"`, `onNew`), los cuatro paneles condicionales por tipo de certificado (`panelFicheroBD`, `panelPkcs11`, `panelClasspath`, `panelSistemaArchivos`) intactos, el `buttons-panel` con `btnDelete`/`btnCancel`/`btnSave`, sus tres `<action-group>` (el de `btnSave` con `remote-validationSave-action` → `save` → `back`), el `action-group` `onChange-dispositivoCriptografico`, el `onNew` y los dos `<action-record>`.
- **Delta:**
  - `<grid>`: `orderBy="dni"` → `orderBy="dni,-enabled"` (DNI ascendente y, dentro del mismo DNI, los habilitados primero — `-` = descendente sobre un booleano, y `true` ordena antes que `false`). Columnas nuevas `nombre` y `apellidos` entre `dni` y `tipoCertificado`, en el orden que fija `screen-certificados-digitales.md`.
  - `<form>`: panel nuevo `panelDatosCalculados`, `hidden="true"`, `showFrame="false"`, con el único campo `nombreTomadoDelUsuario` (**sin** `showIf`). **Es una decisión de maquetación del diseño**, no un panel del spec: `screen-certificados-digitales.md` solo declara el panel «Certificado digital», y este es un panel **técnico** sin ningún campo de negocio visible. Existe porque el campo debe estar declarado en la vista para que los `readonlyIf`/`requiredIf` de `nombre` y `apellidos` puedan leer su valor, tanto al cargar como tras el `onChange` del DNI. Un panel oculto **colapsa por completo** y no reserva columnas (a diferencia de un campo con `showIf`), y es el patrón que ya usa `gestion-centro-cambio-curso.xml`. **Un solo mecanismo de ocultación**: el `hidden` del panel; añadirle además `showIf="false"` al campo sería redundante y ruidoso, así que el campo va sin él.
  - `<form>` / panel `CertificadoDigital`: campos `nombre` y `apellidos` nuevos, junto al DNI y antes del tipo de certificado. `dni` gana `readonlyIf="id != null"` (U-003) y `onChange` (U-001/U-002). `nombre` y `apellidos` llevan `readonlyIf="nombreTomadoDelUsuario"` (U-001, U-004) y `requiredIf="(dni != null) &amp;&amp; (nombreTomadoDelUsuario != true)"` (U-002, U-005). `enabled` pasa de `colOffset="1"` a `colOffset="3"` porque el DNI se ha llevado sus 2 columnas a la fila de arriba; `tipoCertificado` y `enabled` mantienen su `colSpan`.
  - `<?sv-primary-actions?>`: `action-group` nuevo `…-onChange-dni-action` (los eventos MUST referenciar siempre un `action-group`, nunca una acción suelta).
  - `<?sv-primary-actions?>` / `…-btnDelete-action`: se le antepone `<action name="remote-validationDelete-action"/>` a `<action name="delete"/>`. **Corrección de conformidad sobre lo preexistente**: `vistas.md` §1.5 exige que el `action-group` de `btnDelete` del form principal incluya `remote-validationDelete-action` antes de `delete`, y §1.8 aplica esa auditoría al fichero **resultante**, que es el que se copia verbatim a `src/main/...`. El fichero base no lo llevaba.
  - `<?sv-remotes?>`: `action-method` nuevo `…-Remote-getDatosTitularByDni-action` → `CertificadoDigitalController.getDatosTitularByDni`. El segmento `Remote-{X}` coincide con el `method="X"` del `<call>` (regla `Remote-{nombreFuncionJava}` de `k-vistas/actions.md`).
  - **Sin** `<action-method>` de validación por entidad: guardar y borrar siguen usando las acciones globales `remote-validationSave-action` / `remote-validationDelete-action`.

**Acciones declaradas en el fichero** (las nueve; el propósito de cada una, incluidas las preexistentes que el fichero copia verbatim):

| Acción | Tipo | Preexistente | Propósito | Campos / condiciones que intervienen |
|---|---|---|---|---|
| `…-btnDelete-action` | `action-group` | sí (**modificada**: + `remote-validationDelete-action`) | Borrar el certificado abierto: valida el borrado en servidor (`validateRemove`) y, si pasa, ejecuta `delete` | Botón `btnDelete`, visible con `showIf="(id!=null) \|\| (cid!=null)"` |
| `…-btnCancel-action` | `action-group` | sí | Salir del formulario sin guardar (`back`) | Botón `btnCancel` |
| `…-btnSave-action` | `action-group` | sí | Guardar: `remote-validationSave-action` (V-001…V-005 en el servidor) → `save` → `back` | Botón `btnSave`; el `back` final cierra la ventana aunque `save` sea un no-op |
| `…-onChange-dni-action` | `action-group` | **no (delta)** | Envolver la llamada remota que resuelve el titular al cambiar el DNI (los eventos referencian siempre un `action-group`) | `onChange` del campo `dni` |
| `…-onChange-dispositivoCriptografico-action` | `action-group` | sí | Al cambiar de dispositivo criptográfico, invalidar el alias elegido | `onChange` del campo `dispositivoCriptografico` |
| `…-onNew-action` | `action-group` | sí | Inicializar el formulario de alta | `onNew` del `<form>` |
| `…-set-alias-null-action` | `action-record` | sí | Pone `alias` a null (el alias anterior pertenecía a otro dispositivo) | Campo `alias` |
| `…-set-enabled-true-action` | `action-record` | sí | Marca `enabled = true` en el alta (U-certificados-digitales-006) | Campo `enabled` |
| `…-Remote-getDatosTitularByDni-action` | `action-method` | **no (delta)** | Llama a `CertificadoDigitalController.getDatosTitularByDni`, que devuelve nombre, apellidos y el flag `nombreTomadoDelUsuario` del DNI tecleado (U-001/U-002) | Lee `dni` del contexto; escribe `nombre`, `apellidos`, `nombreTomadoDelUsuario` |

> **CRITICAL — la vista NO es la defensa.** Los `readonlyIf` de `dni`, `nombre` y `apellidos`, los `requiredIf` de `nombre`/`apellidos` y el `hidden` del panel `panelDatosCalculados` son **solo UX**: el cliente puede saltárselos por el endpoint REST automático `/ws/rest/<FQN>` que Axelor publica para toda entidad (ver el apartado PENDIENTE de `CLAUDE.md`). La defensa real de esos mismos campos son las whitelists `allowPropertiesInsert`/`allowPropertiesUpdate` y las action rules R-CertificadoDigital-001/-002/-003 del Paso 4, más las validaciones V-CertificadoDigital-001…-005. Ver `k-secure-coding` §3.

**ASCII Layout — panel `CertificadoDigital`** (un dibujo por estado del `showIf` de los paneles anidados; el panel `panelDatosCalculados` es `hidden` y colapsa, no aparece en ningún dibujo):

```
Estado 1 — tipoCertificado = FICHERO_BD
ddnnnnaaaaaa   ← dni(2) + nombre(4) + apellidos(6)                    [identificación del titular]
tttttt...eee   ← tipoCertificado(6) + colOffset(3) + enabled(3)       [ubicación del certificado + estado]
── panelFicheroBD ─────────────────────────────
ppppffff····   ← password(4) + fichero(4)                             [preexistente, sin tocar]

Estado 2 — tipoCertificado = DISPOSITIVO_PKCS11
ddnnnnaaaaaa
tttttt...eee
── panelPkcs11 ────────────────────────────────
ccccaaaaaaaa   ← dispositivoCriptografico(4) + alias(8)               [preexistente, sin tocar]

Estado 3 — tipoCertificado = CLASSPATH
ddnnnnaaaaaa
tttttt...eee
── panelClasspath ─────────────────────────────
ppppcccccccc   ← password(4) + rutaClasspath(8)                       [preexistente, sin tocar]

Estado 4 — tipoCertificado = SISTEMA_ARCHIVOS
ddnnnnaaaaaa
tttttt...eee
── panelSistemaArchivos ───────────────────────
ppppssssssss   ← password(4) + rutaSistemaArchivos(8)                 [preexistente, sin tocar]

Estado 5 — tipoCertificado sin valor (ningún panel condicional visible)
ddnnnnaaaaaa
tttttt...eee
```

Las dos filas nuevas suman exactamente 12. El borde de columna 6|7 es común a las dos (`apellidos` arranca donde acaba `tipoCertificado`), y `apellidos` y `enabled` terminan los dos en la columna 12. `nombre` y `apellidos` van en la misma fila y en el orden convencional del dominio (nombre antes que apellidos), pegados al DNI porque los tres identifican al titular; `apellidos` recibe más columnas que `nombre` porque su valor típico es más largo. `dni(2)`, `tipoCertificado(6)` y `enabled(3)` conservan el `colSpan` que ya tenían (mínima intrusión); lo único que se recalcula es el `colOffset` de `enabled`, forzado por el hueco que deja el DNI al subir de fila.

**ASCII Layout — `buttons-panel`** (sin cambios respecto al fichero real):

```
bb......ccgg   ← btnDelete(2) + colOffset(6) + btnCancel(2) + btnSave(2)
```

Secundario (Borrar) a la izquierda, principales (Cancelar, Guardar) pegados al borde derecho; `btnDelete` es condicional pero está al principio del panel y sin `colOffset`, que es la excepción documentada en `k-vistas/forms.md`.

**Verificar:** al abrir «Certificados digitales» el listado muestra las cinco columnas en orden; al teclear un DNI de usuario en el alta, nombre y apellidos se rellenan solos y quedan en gris; al teclear uno que no lo es, se vacían y salen marcados como obligatorios.

### Reglas de UI `U-certificados-digitales-NNN` (verbatim)

| Regla | Origen spec | Ubicación | Mecanismo |
|---|---|---|---|
| U-certificados-digitales-001 | RUI-certificados-digitales-formulario-001 | `views/Main-CertificadoDigital.xml`: `onChange` del campo `dni` → `…-onChange-dni-action` → `…-Remote-getDatosTitularByDni-action`; más `readonlyIf="nombreTomadoDelUsuario"` en `nombre` y `apellidos` | `action-group` + `action-method` (rama «existe usuario»: rellena nombre y apellidos y pone el flag a `true`, lo que los deja de solo lectura) |
| U-certificados-digitales-002 | RUI-certificados-digitales-formulario-002 | Misma acción `…-Remote-getDatosTitularByDni-action`; más `requiredIf="(dni != null) && (nombreTomadoDelUsuario != true)"` en `nombre` y `apellidos` | `action-method` (rama «no existe usuario»: vacía nombre y apellidos y pone el flag a `false`, lo que los deja editables y obligatorios) |
| U-certificados-digitales-003 | RUI-certificados-digitales-formulario-003 | `views/Main-CertificadoDigital.xml`: atributo `readonlyIf="id != null"` del campo `dni` | Atributo inline, reevaluado de forma continua (cubre el disparador «al cargar») |
| U-certificados-digitales-004 | RUI-certificados-digitales-formulario-004 | `views/Main-CertificadoDigital.xml`: atributo `readonlyIf="nombreTomadoDelUsuario"` de `nombre` y `apellidos` | Atributo inline; el campo `nombreTomadoDelUsuario` está declarado en el panel oculto `panelDatosCalculados`, así que su valor llega también al cargar |
| U-certificados-digitales-005 | RUI-certificados-digitales-formulario-005 | `views/Main-CertificadoDigital.xml`: atributo `requiredIf="(dni != null) && (nombreTomadoDelUsuario != true)"` de `nombre` y `apellidos` | Atributo inline; el valor nulo del flag en las filas anteriores al cambio se evalúa como «no tomado del usuario» |
| U-certificados-digitales-006 | RUI-certificados-digitales-formulario-006 | `views/Main-CertificadoDigital.xml`: `onNew` → `…-onNew-action` → `…-set-enabled-true-action` | `action-group` + `action-record` **preexistentes**, sin cambios |
| U-certificados-digitales-007 | — | `views/Main-CertificadoDigital.xml`: atributo `orderBy="dni,-enabled"` del `<grid>` | Añadida por el diseño para materializar la propiedad «Ordenación por defecto» de `screen-certificados-digitales.md` (el spec no la numera como `RUI-`) |

### Detectores mecánicos del Paso 10 que aplican a esta tarea (verbatim)

```bash
# 3. Botones del formulario: la toolbar nativa de Axelor MUST estar apagada (vistas.md §1.4/§3.b).
#    Esperado: SIN resultados.
grep -nE '<form .*can(Back|Delete|Save)="true"' src/main/java/com/educaflow/subsystem/criptografia/views/Main-CertificadoDigital.xml

# 4. Validación remota por entidad: MUST usarse las acciones globales, no un action-method
#    propio de save/delete (vistas.md §1.5/§3.c). Esperado: SIN resultados.
grep -nE 'Remote-validate(Save|Delete)-action' src/main/java/com/educaflow/subsystem/criptografia/views/Main-CertificadoDigital.xml

# 5. Validación remota global de borrado presente en el action-group de btnDelete (vistas.md §1.5).
#    Esperado: 1 línea.
grep -c 'remote-validationDelete-action' src/main/java/com/educaflow/subsystem/criptografia/views/Main-CertificadoDigital.xml
```

### Notas y supuestos que aplican a esta tarea (verbatim)

3. **Mecanismo elegido para RUI-001/RUI-002: `action-method` (type1), no `action-record` con `call:`.** La regla toca **tres** campos a la vez (`nombre`, `apellidos`, `nombreTomadoDelUsuario`) y `k-vistas/actions.md` reserva el `call:` de `<action-record>` para cuando el servidor solo calcula el valor de **un** campo; además, con `call:` harían falta tres métodos de controlador y tres búsquedas del usuario por cada cambio de DNI. El `readonly`/`required` que esas mismas reglas piden **no** viaja en la respuesta del controlador: se resuelve de forma declarativa con `readonlyIf`/`requiredIf` sobre el campo `nombreTomadoDelUsuario`, que es un campo real del modelo. Así la misma expresión cubre el disparador «al cambiar DNI» (U-001/U-002) y el disparador «al cargar» (U-004/U-005) sin duplicar lógica.

4. **`requiredIf` lleva la guarda `dni != null`.** Sin ella, un formulario recién abierto marcaría el nombre y los apellidos como obligatorios antes de que el administrador haya escrito ningún DNI, que no es lo que describe RUI-002 («al escribir un DNI que no corresponde a ningún usuario»). La obligatoriedad real la impone el servidor en V-CertificadoDigital-001/-002 y -003/-004; el `requiredIf` es solo UX.

7. **El título del enum `CLASSPATH` contiene la errata «dentro del del WAR».** El spec la escribe corregida («dentro del WAR») en los escenarios, pero el título es preexistente y corregirlo no está en el delta, así que se conserva verbatim (mínima intrusión). `test-e2e-desc.md` usa el literal **real** de la vista, que es el que verá el navegador.

8. **Numeración de las `U-`.** Se numeran 001…006 en correspondencia 1:1 con las `RUI-certificados-digitales-formulario-001…006` del spec, para que la trazabilidad se lea de un vistazo. Los `.desc.md` ya persistidos de una iniciativa anterior citan `U-certificados-digitales-001` con otro significado (el «Habilitado por defecto», que aquí es U-certificados-digitales-006); son artefactos congelados «as-tested» y no se reescriben.
