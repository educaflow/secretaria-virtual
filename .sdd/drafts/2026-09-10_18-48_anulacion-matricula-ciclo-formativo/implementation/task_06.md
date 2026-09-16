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

Materializa el modelo de dominio del tipo de expediente.

El fichero **ya está materializado** en `design/domains.xml`. **Cópialo literalmente** a la ruta destino

```
src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/domains.xml
```

**sobrescribiendo** el esqueleto que dejó `CreateFilesTask`. **MUST NOT** modificarlo, reescribirlo ni regenerarlo: el XML del diseño es **contrato fijo**.

**CRITICAL** — **MUST NOT** añadirse ningún método al `<extra-code-model>`: el build **sustituye el bloque entero** por el que genera su plantilla, así que lo que se escriba ahí a mano se pierde en la primera compilación. El código propio del tipo va en las clases de la raíz de la versión (tarea 09).

Las tablas de `## 4. Modelo` y `## 5. Documentos PDF` van abajo **verbatim** como contrato de lo que ese XML declara: la columna «quién lo rellena» de cada campo, los enums y las constantes del enum `TipoDocumentoPdf`. **MUST NOT** añadirse ningún campo, enum ni constante que no esté en ellas.

## Fila de la tabla `## 6. Ficheros a crear o modificar` del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/domains.xml` | Crear | `k-tipo-expediente` | Copia de `design/domains.xml` |

## Paso del diseño (verbatim)

### Paso 6 — `domains.xml`

El fichero está materializado en `design/domains.xml`. **Cópialo literalmente** a `…/v1/domains.xml`, **sobrescribiendo** el esqueleto que dejó `CreateFilesTask`. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

**Verificación:** `diff` vacío; la entidad `AnulacionMatriculaCicloFormativoV1` es la **primera** `<entity>`, extiende `Expediente`, ningún campo lleva `required="true"` y el `<extra-code-model>` declara exactamente `SOLICITUD` y `RESOLUCION` (el build lo reescribirá con esas mismas dos constantes si el paso 14 se hizo bien). **MUST NOT** añadirse ningún método al `<extra-code-model>`: el build **sustituye el bloque entero** por el que genera su plantilla, así que lo que se escriba ahí a mano se pierde en la primera compilación — el código propio del tipo va en las clases de la raíz de la versión (Paso 9). Tras `./run.sh`, `grep -c esRechazo …/v1/domains.xml` **MUST** devolver `0`.


## `## 4. Modelo` (verbatim, completa)

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
- **`esRechazo(…)` no es un campo de la entidad, y por eso no está en la tabla: es una FUNCIÓN ESTÁTICA de la raíz de la versión** (`ReglasAnulacionMatricula.esRechazo(expediente)`, §9.0.3). Es el dueño único **en servidor** de la clasificación «la revisión rechaza si y solo si `sentidoRevision == RECHAZAR`» —de la que se siguen sus dos consecuencias: la resolución **desestima** y el **motivo del rechazo** aplica—. Lo consultan sus **cuatro** consumidores de servidor —la acción 4 de `triggerEnviarAFirma` (§9.2), la rama del validador de `ENVIAR_A_FIRMA` (§10.2) y **las dos** expresiones del documento `resolucion.xml`, la de la fórmula de estimación/desestimación y la del motivo (§5 y §14 nota 5)—, y ninguno de ellos vuelve a comparar contra el enum. **CRITICAL — MUST NOT** vivir en el `<extra-code-model>` del `domains.xml`: ese bloque **lo reescribe entero el build** en cada compilación (`RichDomainXmlTask` → `DomainXmlFile.addExtraCodeToDomainXml` sustituye el bloque de la entity con `extends="Expediente"` por el que emite `extra-code-domain-xml.template`, que solo genera el enum `TipoDocumentoPdf` y `getDocumentoPdf`), así que un método escrito ahí se pierde en el primer build y se lleva por delante a sus cuatro consumidores: el trigger y el validador no compilarían y las dos expresiones Groovy del PDF fallarían **en silencio** (log + campo vacío), dejando la resolución sin la fórmula y sin el motivo (ESC-014, ESC-015 y ESC-030). Por eso el predicado vive en una clase de la raíz de la versión, como `ControlDeAcceso` y `DevolucionDelDirector` (§9.0). **MUST NOT** convertirse tampoco en un `<string transient>` ni en un `<boolean transient>` con `depends`: no lo consume ninguna vista —los tres literales que se evalúan en el navegador leen `sentidoRevision` directamente, §11— y un campo calculado en servidor del que el cliente no puede fiarse es peor que no tenerlo (`decisiones.md` D10). Al no ser una propiedad, tampoco aparece en ninguna `AllowProperties`.

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
| `service/impl/NivelServiceImpl.java` | Añadir `"nombreCorto"` a la whitelist de `allowPropertiesEditables()`, el método privado del que salen `allowPropertiesInsert()` y `allowPropertiesUpdate()`. Hoy es `AllowProperties.createAllowProperties(Map.of("code", …, "name", …, "grado", …))`, es decir una whitelist **cerrada**: toda propiedad que no esté en ella se **descarta en silencio** al guardar. **CRITICAL — sin esta fila, `nombreCorto` no se puede dar de alta ni editar NUNCA desde `Main-Nivel.xml`**, aunque el campo exista en el modelo y se pinte en la pantalla. No lo destapa sembrar los tres nombres cortos en el data-init, porque el data-init **no** pasa por el `ModelService` |
| `views/Ref-Ciclo.xml` | Añadir las columnas `grado` y `nivel` al grid `subsysSistemaEducativo.Ref@Ciclo-grid`, para que la ventana de búsqueda del ciclo permita filtrar por ellas (RUI-DATOS_SOLICITUD-CREADOR-008). La fila de filtros por columna se pinta sola en el popup; el autocompletado al teclear sigue buscando solo por `name` |

**Los ocho ciclos que necesita la suite E2E NO entran en el catálogo maestro: son configuración del entorno de pruebas.** El catálogo de demo tiene hoy siete ciclos y con ellos no caben las parejas (alumno, ciclo) disjuntas que los 56 tests E2E necesitan para no pisarse entre sí bajo la regla `SinOtraSolicitudEnCursoParaElMismoCiclo` (§10.0). Esa es una necesidad **de la suite de pruebas**, y la especificación declara **fuera de alcance** «el mantenimiento del catálogo de ciclos … común a todos los centros y no forma parte de este trámite», así que **MUST NOT** resolverse dando de alta filas en `subsystem/sistemaeducativo/data-init/input/Ciclo.xml`, que es dato maestro de producción compartido por todos los trámites y todos los centros.

Se resuelven como **configuración del entorno de pruebas**, exactamente igual que los certificados custodiados de §14 nota 11: se dan de alta **por la propia aplicación**, en la pantalla de mantenimiento «Sistema educativo → Ciclos» (`subsysSistemaEducativo.Main@Ciclo-action`, visible para `admins`), antes de lanzar la suite. **No** son un fichero de este proyecto y **no** figuran en la tabla de ficheros de §6. La lista exacta, con su familia profesional, su grado y su nivel, vive en la sección «Aislamiento entre tests» de `test-e2e-desc.md`, que es su única fuente de verdad; ninguno es de la familia `190` «Informática y Comunicaciones» ni de grado `E`, para no alterar lo que ven la ventana de búsqueda de T-002 (familia «Informática y Comunicaciones» + nivel `GS`), el autocompletado de T-047 (ningún nombre nuevo contiene «Microinform») ni el filtro por grado de T-048 (sigue habiendo un único curso de especialización).

**MUST NOT** convertirse esa configuración en un alta de catálogo «porque es más cómodo»: el día en que el catálogo de ciclos se mantenga de verdad (una iniciativa propia, la que la especificación deja fuera de alcance), esos ocho ciclos entrarán por ahí y esta configuración de entorno dejará de hacer falta sin que haya que quitar nada del data-init.

Con `gradoNivel` y los nombres cortos, «Desarrollo de Aplicaciones Web» (grado `D`, nivel `GS`) imprime «Superior», «Sistemas Microinformáticos y Redes» (nivel `GM`) imprime «Medio» e «Inteligencia Artificial y Big Data» (grado `E`, sin nivel) imprime «Curso de especialización», que es el `name` de su grado.

El fichero materializado del modelo del tipo: `domains.xml`.


## `## 5. Documentos PDF` (verbatim, completa) — de aquí salen las constantes del enum `TipoDocumentoPdf`

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
- Recuadros de firma (las constantes viven en el `PhaseEventManagerImpl` de su fase, §9). Los cuatro números de cada uno están **medidos sobre el PDF generado** por `Xml2Pdf` (§14 nota 12), no elegidos a ojo, y caen dentro de la celda del documento a la que pertenecen:
  - solicitud: `Rectangulo(320, 418, 240, 32)`, página `1` — dentro de la celda «Signatura:/Firma:» de la sección «Lugar, fecha y firma», que ocupa `x 28,35 → 566,93` e `y 415,52 → 472,22`; queda a la derecha y **por debajo** de los dos rótulos (líneas base `y 462,70` en valenciano y `y 454,58` en castellano). Son **los mismos ocho argumentos** que la `<action-method>` de la vista de la fase `SOLICITUD`.
  - resolución: `Rectangulo(150, 317, 300, 53)`, página `1` — dentro del recuadro vacío que hay bajo «El director / la directora del centro», que ocupa `x 28,35 → 566,93` e `y 315,28 → 372,00`.
  - En los dos, `x + ancho ≤ 566,93` (el borde derecho del marco del documento) e `y + alto` queda por debajo del borde superior de su celda. **MUST** recomprobarse si se toca la maquetación del documento: cualquier fila que se añada o se quite mueve esas celdas.
- **El ancho de un campo inline nunca es `;12`.** El widget se dibuja en `x + PAD`, así que un campo inline que pida la rejilla entera se sale del marco: medido sobre el PDF generado, con `;12` su borde derecho cae en `x = 569,79` frente al borde derecho de la tabla en `x = 566,93`. Las cuatro expresiones de ancho completo de `resolucion.xml` (la fórmula y el motivo, en sus dos idiomas) llevan por eso `;11.8` — el `colspan="12"` de la fila **sí** se queda en 12, porque es la celda y no el widget. El separador acepta decimales (`([0-9]+(?:\.[0-9]+)?)`). En `solicitud.xml` no hace falta: su campo más ancho llega a `x = 561,86`.
- **Las fechas en valenciano NO llevan el literal `'de'` en el patrón.** En catalán/valenciano el nombre de mes en **contexto de formato** (`MMMM` con `Locale.forLanguageTag("ca")`) ya trae la preposición con la elisión resuelta —«de gener», «d’abril», «d’agost», «d’octubre»—, así que el patrón correcto es `"d MMMM 'de' yyyy"`: con `"d 'de' MMMM 'de' yyyy"` saldría «1 de de gener de 2026» los meses consonánticos y «1 de d’abril de 2026» los vocálicos. El castellano **sí** conserva `"d 'de' MMMM 'de' yyyy"`, porque su `MMMM` es solo el mes. Afecta a las **tres** fechas en valenciano: la de la solicitud y las dos de la resolución (presentación y resolución).
- Las expresiones Groovy de los documentos **fallan en silencio** (log + campo vacío): el paso final de verificación exige abrir los dos PDF generados en runtime y comprobar que ningún hueco queda en blanco, que las tres fechas en valenciano salen bien escritas y que ningún campo inline sobresale del marco.


## `## 11. Reparto de reglas` — filas que ubican una regla en el modelo (verbatim)

| Tipo de regla | Capa | Cómo se escribe |
|---|---|---|
| Tipo, longitud máxima de columna, referencia, enumerado | **modelo XML** (`domains.xml`) | atributos de `<string>`/`<date>`/`<enum>`/`<many-to-one>` |
| Obligatoriedad de un campo **en un evento** | **DSL del validador** | `+Required(mensaje)` en la pareja (estado, evento) |
| Formato, rango, longitud, firma | **DSL del validador** | `Pattern`, `MinLength`/`MaxLength`, `FirmaPdf` |
| Obligatoriedad **condicional** | **DSL del validador** | `ifValueIn(...) { +Required(...) }` |
| Valor no admitido en un evento | **DSL del validador** | `ifValueIn`/`ifValueNotIn` + `NoAdmitido(mensaje)`, regla **nueva** del catálogo común que **MUST** ir siempre dentro de una rama: la rama pone la condición, la regla el mensaje (`decisiones.md` D7, §14 nota 21) |
| Qué campos puede dictar el cliente en un evento | **DSL del validador** | el conjunto de `field(...)` de esa pareja |
| Efectos: generar PDF, firmar, registrar, transicionar, limpiar, calcular | **`trigger*`** del `PhaseEventManagerImpl` | listas de acciones de §9 |
| Inicialización del expediente | **`triggerInitialEvent`** | §8 |
| Quién es el usuario autenticado (autoría, centro, **perfil sobre el expediente**) | **guarda en la primera línea del `trigger*`** | `ControlDeAcceso.exige…` → `BusinessException`. La de perfil no reescribe **ninguna** de sus dos preguntas: *qué* perfil se exige se lo pregunta al `<state>` (vía `States`) y *si el usuario lo ostenta*, a `PerfilesUsuarioService.getPerfilesSobreExpediente` (§9.0.1) |
| Mostrar/ocultar/deshabilitar, ayudas, confirmaciones | **vista** | `showIf`/`readonly`, `<help>`, `prompt`, `<action-record>` — **solo UX, NUNCA defensa** |
| Campo calculado de **lectura** | **modelo XML del catálogo** | `Ciclo.gradoNivel`, campo derivado `transient` |
| Clasificación que consultan **varias capas de servidor** (trigger + validador + documento) | **función estática de una clase de la raíz de la versión** | `ReglasAnulacionMatricula.esRechazo(expediente)` (§9.0.3); es una función, no un campo, porque ninguna vista lo consume (ver debajo), y vive fuera del `<extra-code-model>` porque el build reescribe ese bloque entero |

| Regla | Capa |
|---|---|
| CC-014 | campo derivado `Ciclo.gradoNivel` del catálogo (§4) |

Reglas duras respetadas: ningún `required="true"` en el `domains.xml`; ninguna validación de datos de usuario en un `trigger*`; ninguna lógica de negocio en el validador; ninguna inicialización del expediente en un `PhaseEventManagerImpl`.
