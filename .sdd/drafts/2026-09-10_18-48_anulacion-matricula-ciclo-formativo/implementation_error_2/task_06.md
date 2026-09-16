---
type: implementation-task
template: expediente
---

# Tarea 06 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-tipo-expediente
- k-validaciones
- k-secure-coding

## Qué hay que hacer

Materializar el modelo de dominio del tipo de expediente.

**Origen:** `design/domains.xml`
**Destino:** `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/domains.xml`

**Cópialo literalmente**, **sobrescribiendo** el esqueleto que dejó `CreateFilesTask`. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

El texto del diseño que va debajo es la **especificación** del fichero: se lee para entender y verificar lo copiado (qué campo lo rellena el usuario y cuál el servidor, qué enums lleva, qué constantes tiene el `<extra-code-model>`), **no** para reescribir el XML.

## Fila de la tabla `## 6. Ficheros a crear o modificar` del diseño (verbatim)

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/domains.xml` | Crear | `k-tipo-expediente` | Copia de `design/domains.xml` |

## Paso del diseño (verbatim)

### Paso 6 — `domains.xml`

El fichero está materializado en `design/domains.xml`. **Cópialo literalmente** a `…/v1/domains.xml`, **sobrescribiendo** el esqueleto que dejó `CreateFilesTask`. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

**Verificación:** `diff` vacío; la entidad `AnulacionMatriculaCicloFormativoV1` es la **primera** `<entity>`, extiende `Expediente`, ningún campo lleva `required="true"` y el `<extra-code-model>` declara exactamente `SOLICITUD` y `RESOLUCION` (el build lo reescribirá con esas mismas dos constantes si el paso 14 se hizo bien).

## `## 4. Modelo` del diseño, completa (verbatim)

## 4. Modelo

### Tabla de campos de la entidad

| nombre | tipo | ref | title | para qué sirve | quién lo rellena |
|---|---|---|---|---|---|
| `nia` | `string` | — | NIA | Número de identificación del alumno; se imprime en los dos documentos | `usuario` |
| `direccion` | `string` | — | Dirección | Domicilio del alumno; se imprime en la solicitud | `usuario` |
| `telefono` | `string` | — | Teléfono | Teléfono de contacto; se imprime en la solicitud | `usuario` |
| `poblacion` | `string` | — | Población | Localidad del alumno; se imprime en la solicitud | `usuario` |
| `provincia` | `string` | — | Provincia | Provincia del alumno; se imprime en la solicitud | `usuario` |
| `codigoPostal` | `string` | — | Código postal | CP del alumno; se imprime en la solicitud | `usuario` |
| `ciclo` | `many-to-one` | `com.educaflow.subsystem.sistemaeducativo.db.Ciclo` | Ciclo formativo | El ciclo cuya matrícula se anula; de él se derivan el nombre y el grado o nivel que imprimen los documentos | `usuario` |
| `cursoAcademico` | `string` | — | Curso académico | Curso en vigor del centro al crear el expediente, con formato «2024/2025»; se imprime en los dos documentos | `servidor` |
| `nombreCentro` | `string` | — | Centro | Nombre del centro, congelado al crear el expediente | `servidor` |
| `localidadCentro` | `string` | — | Localidad del centro | Localidad del centro, congelada al crear el expediente; es también el **lugar** de la solicitud y de la resolución | `servidor` |
| `fechaSolicitud` | `date` | — | Fecha de la solicitud | Fecha que se estampa al pie de la solicitud; se reescribe en cada `CONTINUAR` | `servidor` |
| `fechaHoraPresentacion` | `datetime` | — | Fecha y hora de presentación | Momento de la presentación; desde él surte efecto la anulación | `servidor` |
| `sentidoRevision` | `enum` | `SentidoRevisionAnulacionMatriculaCicloFormativoV1` | Sentido de la revisión | Qué decide la secretaría; discrimina la transición de `FIRMAR` | `usuario` |
| `motivoRechazo` | `string` (large) | — | Motivo del rechazo | Texto que se imprime en la resolución cuando se desestima | `usuario` |
| `textoSubsanacion` | `string` (large) | — | Qué hay que subsanar | Texto que la secretaría le escribe al alumno | `usuario` |
| `fechaRevision` | `date` | — | Fecha de la revisión | Día en que la secretaría decidió | `servidor` |
| `revisadoPor` | `many-to-one` | `com.axelor.auth.db.User` | Revisado por | Quién hizo la revisión | `servidor` |
| `motivoDevolucion` | `string` (large) | — | Motivo de la devolución | Por qué el director devuelve la resolución a secretaría | `usuario` |
| `fechaDevolucion` | `date` | — | Fecha de la devolución | Día en que el director devolvió | `servidor` |
| `devueltoPor` | `many-to-one` | `com.axelor.auth.db.User` | Devuelto por | Quién devolvió | `servidor` |
| `fechaResolucion` | `date` | — | Fecha de la resolución | Día en que el director firma; se imprime en la resolución | `servidor` |
| `firmadoPor` | `many-to-one` | `com.axelor.auth.db.User` | Firmado por | Quién firmó la resolución | `servidor` |
| `pdfSolicitud` | `many-to-one` | `com.axelor.meta.db.MetaFile` | Solicitud de anulación | La solicitud generada, sin firmar | `servidor` |
| `pdfSolicitudFirmada` | `many-to-one` | `com.axelor.meta.db.MetaFile` | Solicitud de anulación firmada | La solicitud firmada por el alumno | `servidor` **(excepción en el validador, ver abajo)** |
| `pdfJustificanteRegistroEntrada` | `many-to-one` | `com.axelor.meta.db.MetaFile` | Justificante de presentación | El resguardo sellado que devuelve el registro de entrada | `servidor` |
| `pdfResolucion` | `many-to-one` | `com.axelor.meta.db.MetaFile` | Resolución sin firmar | La resolución que el director revisa antes de firmar | `servidor` |
| `pdfResolucionFirmada` | `many-to-one` | `com.axelor.meta.db.MetaFile` | Resolución | La resolución firmada y registrada de salida | `servidor` |

- **`pdfSolicitudFirmada` es un campo `servidor` que, excepcionalmente, sí aparece en el `field(...)` de `PRESENTAR`.** A efectos del modelo es `servidor`: no es un dato que el cliente pueda dictar con normalidad, y en la rama de firma en servidor lo rellena el trigger (§9.1, acción 2). La **única** razón de que aparezca en el validador es que en la rama de AutoFirma el PDF firmado **llega desde el cliente** y el `field(...)` de `PRESENTAR` es el único sitio donde se puede comprobar la firma (`+Required()` + `+FirmaPdf(model::getPdfSolicitud, …)`). Es la **única excepción** a «solo campos `usuario` en el validador» y queda documentada aquí como tal, tal y como exige la plantilla de vistas §6 y el patrón de `k-tipo-expediente/recetas/firma.md` §1.
- `claveCertificado` **no aparece en esta tabla**: se hereda de `Expediente` (transient, `password="true"`) y **MUST NOT** redeclararse; entra en la whitelist de `PRESENTAR` por la rama de firma en servidor.
- Tampoco se redeclaran `centro`, `usuarioRegistrador`, `personaSolicitante`, `personaInteresada`, `numeroExpediente`, `codePhase`/`codeState`, `abierto` ni `historialEstados`.
- **Ningún campo lleva `required="true"`**: el expediente existe en BD desde el estado inicial con todos vacíos; la obligatoriedad es por pareja (estado, evento) y vive en el DSL del validador.
- **El grado o nivel del ciclo (CC-014) NO es un campo de esta entidad.** Es un campo derivado del **catálogo**: `Ciclo.gradoNivel` (ver «Cambios en el catálogo del sistema educativo»). Lo consultan la pantalla (`<field name="ciclo.gradoNivel">`) y los dos documentos (`self.ciclo.gradoNivel`): un solo dueño y dos consumidores.

### Tabla de enums

| enum | numeric | ítem | title | value |
|---|---|---|---|---|
| `SentidoRevisionAnulacionMatriculaCicloFormativoV1` | — | `ACEPTAR` | Aceptar la anulación | — |
| `SentidoRevisionAnulacionMatriculaCicloFormativoV1` | — | `RECHAZAR` | Rechazar la anulación | — |
| `SentidoRevisionAnulacionMatriculaCicloFormativoV1` | — | `SUBSANAR` | Pedir subsanación | — |

### Cambios en el catálogo del sistema educativo (`subsystem/sistemaeducativo`)

Son del catálogo, no del trámite, y cualquier otro trámite que imprima un ciclo los reutiliza:

| Fichero | Cambio |
|---|---|
| `domains/Nivel.xml` | Campo nuevo `<string name="nombreCorto" title="Nombre corto"/>` (no `required`: es un campo **nuevo** sobre una tabla que ya tiene filas, que quedarían en violación de la restricción). Es el texto que se imprime en la línea «en el Ciclo Formativo de Grado ____»: «Básico», «Medio», «Superior» |
| `domains/Ciclo.xml` | Campo derivado nuevo `<string name="gradoNivel" transient="true" title="Grado o nivel">` con cuerpo Java: si el ciclo **tiene** nivel devuelve su `nombreCorto` (o su `name` si el nombre corto está vacío), y si **no** tiene nivel devuelve el `name` de su grado. **CRITICAL — el cuerpo MUST leer los CAMPOS `nivel` y `grado` directamente, nunca `getNivel()`/`getGrado()`**: `Mapper.findComputeDependencies` recorre el bytecode y solo registra como dependencia las instrucciones `GETFIELD`, igual que documenta `Grado.admiteNivel` en `subsystem/sistemaeducativo/domains/Grado.xml` |
| `data-init/input-config.xml` | Añadir `<bind node="@nombreCorto" to="nombreCorto"/>` al `<input>` de `Nivel.xml` |
| `data-init/input/Nivel.xml` | `nombreCorto="Básico"` en `GB`, `"Medio"` en `GM`, `"Superior"` en `GS` |
| `data-init/input/Ciclo.xml` | **Una** fila nueva: `<ciclo code="IABD" name="Inteligencia Artificial y Big Data" familiaProfesional="190" grado="E"/>` (sin `nivel`), que es la única que exige la especificación en «Datos iniciales». **MUST NOT** darse de alta aquí ningún otro ciclo: el mantenimiento del catálogo está **fuera del alcance** de esta iniciativa (ver debajo de la tabla) |
| `views/Main-Nivel.xml` | Mostrar `nombreCorto` en el grid y en el form de mantenimiento de niveles, para que se pueda dar de alta |
| `views/Ref-Ciclo.xml` | Añadir las columnas `grado` y `nivel` al grid `subsysSistemaEducativo.Ref@Ciclo-grid`, para que la ventana de búsqueda del ciclo permita filtrar por ellas (RUI-DATOS_SOLICITUD-CREADOR-008). La fila de filtros por columna se pinta sola en el popup; el autocompletado al teclear sigue buscando solo por `name` |

**Los ocho ciclos que necesita la suite E2E NO entran en el catálogo maestro: son configuración del entorno de pruebas.** El catálogo de demo tiene hoy siete ciclos y con ellos no caben las parejas (alumno, ciclo) disjuntas que los 56 tests E2E necesitan para no pisarse entre sí bajo la regla `SinOtraSolicitudEnCursoParaElMismoCiclo` (§10.0). Esa es una necesidad **de la suite de pruebas**, y la especificación declara **fuera de alcance** «el mantenimiento del catálogo de ciclos … común a todos los centros y no forma parte de este trámite», así que **MUST NOT** resolverse dando de alta filas en `subsystem/sistemaeducativo/data-init/input/Ciclo.xml`, que es dato maestro de producción compartido por todos los trámites y todos los centros.

Se resuelven como **configuración del entorno de pruebas**, exactamente igual que los certificados custodiados de §14 nota 11: se dan de alta **por la propia aplicación**, en la pantalla de mantenimiento «Sistema educativo → Ciclos» (`subsysSistemaEducativo.Main@Ciclo-action`, visible para `admins`), antes de lanzar la suite. **No** son un fichero de este proyecto y **no** figuran en la tabla de ficheros de §6. La lista exacta, con su familia profesional, su grado y su nivel, vive en la sección «Aislamiento entre tests» de `test-e2e-desc.md`, que es su única fuente de verdad; ninguno es de la familia `190` «Informática y Comunicaciones» ni de grado `E`, para no alterar lo que ven la ventana de búsqueda de T-002 (familia «Informática y Comunicaciones» + nivel `GS`), el autocompletado de T-047 (ningún nombre nuevo contiene «Microinform») ni el filtro por grado de T-048 (sigue habiendo un único curso de especialización).

**MUST NOT** convertirse esa configuración en un alta de catálogo «porque es más cómodo»: el día en que el catálogo de ciclos se mantenga de verdad (una iniciativa propia, la que la especificación deja fuera de alcance), esos ocho ciclos entrarán por ahí y esta configuración de entorno dejará de hacer falta sin que haya que quitar nada del data-init.

Con `gradoNivel` y los nombres cortos, «Desarrollo de Aplicaciones Web» (grado `D`, nivel `GS`) imprime «Superior», «Sistemas Microinformáticos y Redes» (nivel `GM`) imprime «Medio» e «Inteligencia Artificial y Big Data» (grado `E`, sin nivel) imprime «Curso de especialización», que es el `name` de su grado.

El fichero materializado del modelo del tipo: `domains.xml`.

## `## 5. Documentos PDF` del diseño, completa (verbatim) — de aquí salen las constantes del enum `TipoDocumentoPdf` del `<extra-code-model>`

## 5. Documentos PDF

| fichero | constante | en qué transición se genera | en qué campo se guarda | quién lo firma | se registra |
|---|---|---|---|---|---|
| `documentospdf/solicitud.xml` | `SOLICITUD` | `SOLICITUD.DATOS_SOLICITUD` + `CONTINUAR` | `pdfSolicitud` (`servidor`) | `cliente (AutoFirma__!!)` o, si el alumno tiene certificado custodiado, `servidor: DNI SecurityUtil.getUser().getDni()` | `entrada` (al presentar, sobre `pdfSolicitudFirmada`) |
| `documentospdf/resolucion.xml` | `RESOLUCION` | `REVISION.PENDIENTE_REVISION` + `ENVIAR_A_FIRMA`, y de nuevo en `RESOLUCION.PENDIENTE_FIRMA_DIRECTOR` + `FIRMAR` | `pdfResolucion` (`servidor`); la firmada, en `pdfResolucionFirmada` (`servidor`) | `servidor: DIRECTOR` | `salida` (al firmar) |

Fragmentos: **ninguno**. Esto **anula expresamente** una indicación de `design-guidelines.md`, que enumera un `_template.xml` bilingüe como tercer XML del trámite; la desviación está declarada en §14 nota 27. Los tres trozos que `documentos.md` declara comunes se resuelven así:

- **Cabecera institucional** — no es contenido del documento: la pinta el generador (logo + `<titulo>`), no hay nada que compartir (§14 nota 17).
- **Identificación del alumno** — los dos documentos abren con una sección de identificación, pero **no imprimen lo mismo**: la solicitud imprime el **bloque A completo del impreso oficial** (apellidos, nombre, NIA, DNI/NIE, dirección, teléfono, población, provincia y CP) y la resolución solo los cuatro datos del destinatario (apellidos, nombre, DNI/NIE y NIA) que su ficha declara. Compartir obligaría a imprimir de más en la resolución o a partir el bloque A en dos (§14 nota 4).
- **Referencia de la matrícula** — aquí sí hay **una línea duplicada literalmente**, pero **no es factorizable con el mecanismo disponible**: un `<include>` solo va como hijo directo de `<documento>` y esa línea es un `<texto>` **dentro** de una sección distinta en cada documento («Expone» / «Vistos»). Se asume la duplicación y **MUST** tocarse en los dos ficheros a la vez (§14 nota 4).

Cada documento declara por tanto su propia `<seccion>` y **MUST NOT** existir ningún fichero con prefijo `_` en `documentospdf/`.

Notas:

- El PDF escaneado del impreso oficial que hoy está en `documentospdf/modelo.pdf` **MUST moverse** a `documentospdf/originales/modelo.pdf`: todo `.pdf` que cuelgue directamente de `documentospdf/` genera una constante en el enum `TipoDocumentoPdf`, y un modelo visual no es un documento del trámite. La subcarpeta `originales/` no se escanea (precedente: `justificacion_falta_profesorado`).
- Recuadros de firma (las constantes viven en el `PhaseEventManagerImpl` de su fase, §9):
  - solicitud: `Rectangulo(320, 60, 250, 70)`, página `1` — **los mismos ocho argumentos** que la `<action-method>` de la vista de la fase `SOLICITUD`.
  - resolución: `Rectangulo(150, 220, 300, 60)`, página `1`.
- Las expresiones Groovy de los documentos **fallan en silencio** (log + campo vacío): el paso final de verificación exige abrir los dos PDF generados en runtime y comprobar que ningún hueco queda en blanco.

## Filas de `## 11. Reparto de reglas` que ubican una regla en el modelo (verbatim)

| Tipo de regla | Capa | Cómo se escribe |
|---|---|---|
| Tipo, longitud máxima de columna, referencia, enumerado | **modelo XML** (`domains.xml`) | atributos de `<string>`/`<date>`/`<enum>`/`<many-to-one>` |
| Campo calculado de **lectura** | **modelo XML del catálogo** | `Ciclo.gradoNivel`, campo derivado `transient` |

Reglas duras respetadas: ningún `required="true"` en el `domains.xml`; ninguna validación de datos de usuario en un `trigger*`; ninguna lógica de negocio en el validador; ninguna inicialización del expediente en un `PhaseEventManagerImpl`.
