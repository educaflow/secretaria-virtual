---
type: design
template: expediente
---

# Diseño: Anulación de matrícula en ciclo formativo

## 1. Objetivo

Permitir que un alumno mayor de edad solicite y firme la anulación de su matrícula en un ciclo formativo del centro, que la secretaría la revise y que el director firme la resolución, dejando constancia registral de la solicitud y de la resolución.

## 2. Identidad del trámite y del tipo

| Dato | Valor |
|---|---|
| `code` del trámite | `AnulacionMatriculaCicloFormativo` |
| Nombre visible (`<name>`) | Anulación de matrícula en ciclo formativo |
| `tipoTramite` | `ALUMNO` (existe en `subsystem/expedientes/data-init/input/TipoTramites.xml`) |
| Carpeta del trámite | `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/` |
| Carpeta de la versión | `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/` |
| `<defaultTipoExpediente>` | `v1` |
| `code` / entidad del tipo | `AnulacionMatriculaCicloFormativoV1` |
| FQN de la entidad | `com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1` |
| `basePackageName` | `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1` |
| Clase `States` (generada) | `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.States` |
| Form plantilla | `exp-AnulacionMatriculaCicloFormativoV1-Templates` |

La carpeta del trámite y su `TramiteInstance.xml` **ya existen** en el árbol (con el `<help>` erróneo y el `TipoExpedienteInstance.xml` vacío de fases), así que esas dos filas de la tabla de ficheros van con acción `Modificar`. No es una iniciativa de modificación de una versión existente: la versión `v1` no tiene todavía ni modelo, ni clases, ni vistas, ni expedientes vivos.

## 3. Máquina de estados

### Fase SOLICITUD — Solicitud de anulación

| estado | title | perfil | eventos | inicial | closed |
|---|---|---|---|---|---|
| `DATOS_SOLICITUD` | Datos de la solicitud | `CREADOR` | `DELETE,CONTINUAR` | sí | — |
| `PENDIENTE_FIRMA` | Pendiente de firma y presentación | `CREADOR` | `VOLVER,PRESENTAR` | — | — |

### Fase REVISION — Revisión de la secretaría

| estado | title | perfil | eventos | inicial | closed |
|---|---|---|---|---|---|
| `PENDIENTE_REVISION` | Pendiente de revisión | `SECRETARIO` | `ENVIAR_A_FIRMA,SUBSANAR` | — | — |

### Fase RESOLUCION — Resolución del centro

| estado | title | perfil | eventos | inicial | closed |
|---|---|---|---|---|---|
| `PENDIENTE_FIRMA_DIRECTOR` | Pendiente de la firma del director | `DIRECTOR` | `FIRMAR,DEVOLVER` | — | — |
| `ACEPTADA` | Anulación aceptada | `RESPONSABLE` | *(vacío)* | — | sí |
| `RECHAZADA` | Anulación rechazada | `RESPONSABLE` | *(vacío)* | — | sí |

**Por qué los dos estados cerrados declaran `profile="RESPONSABLE"` aunque en ellos no actúe nadie.** Las bandejas genéricas de la plataforma («Expedientes Esperando», «Expedientes Cerrados» y la búsqueda, en `tramites/views/`) fijan literalmente `_profile=RESPONSABLE` en su `action-view`, y `ExpedienteController.checkProfileDelTipoExpediente` **lanza una excepción** si el perfil que llega no lo usa ningún estado del tipo: sin esta declaración, abrir un expediente de este tipo desde esas bandejas reventaría. El `profile` de un estado **sin eventos es inerte para la autorización** —`Tramitador.checkPerfilDelEstado` solo se ejecuta al disparar un evento, y estos estados no tienen ninguno—, así que no contradice que en ellos «no actúa nadie»: solo declara `RESPONSABLE` como perfil del tipo. `RESPONSABLE` es, en este trámite, el perfil de **consulta** (§12), el que la especificación describe como «cualquier otro usuario con acceso al expediente». Es además lo que ya hace el trámite de referencia del árbol.

### Tabla de transiciones

| fase origen | estado origen | evento | guarda | fase destino | estado destino |
|---|---|---|---|---|---|
| `[*]` | `[*]` | — | — | `SOLICITUD` | `DATOS_SOLICITUD` |
| `SOLICITUD` | `DATOS_SOLICITUD` | `CONTINUAR` | — | `SOLICITUD` | `PENDIENTE_FIRMA` |
| `SOLICITUD` | `DATOS_SOLICITUD` | `DELETE` | — | `[*]` | `[*]` |
| `SOLICITUD` | `PENDIENTE_FIRMA` | `VOLVER` | — | `SOLICITUD` | `DATOS_SOLICITUD` |
| `SOLICITUD` | `PENDIENTE_FIRMA` | `PRESENTAR` | — | `REVISION` | `PENDIENTE_REVISION` |
| `REVISION` | `PENDIENTE_REVISION` | `ENVIAR_A_FIRMA` | — | `RESOLUCION` | `PENDIENTE_FIRMA_DIRECTOR` |
| `REVISION` | `PENDIENTE_REVISION` | `SUBSANAR` | — | `SOLICITUD` | `DATOS_SOLICITUD` |
| `RESOLUCION` | `PENDIENTE_FIRMA_DIRECTOR` | `FIRMAR` | `sentidoRevision=ACEPTAR` | `RESOLUCION` | `ACEPTADA` |
| `RESOLUCION` | `PENDIENTE_FIRMA_DIRECTOR` | `FIRMAR` | `sentidoRevision=RECHAZAR` | `RESOLUCION` | `RECHAZADA` |
| `RESOLUCION` | `PENDIENTE_FIRMA_DIRECTOR` | `DEVOLVER` | — | `REVISION` | `PENDIENTE_REVISION` |

`ENVIAR_A_FIRMA` y `SUBSANAR` tienen **un solo destino cada uno**, así que no llevan guarda: lo que la especificación describe como «condición» de cada uno (que el sentido sea aceptar/rechazar o que sea subsanar) es una **validación** que impide disparar el evento, no una ramificación de la transición, y vive en el DSL del validador (§10). El único evento ramificado es `FIRMAR`, cuyo discriminador es `sentidoRevision`; sus dos ramas cubren los dos únicos valores posibles en ese estado (`SUBSANAR` no puede llegar aquí, porque el validador de `ENVIAR_A_FIRMA` lo rechaza) y el `default` del `switch` es un error de programación.

Los ficheros materializados: `TipoExpedienteInstance.xml` y `estados.puml`.

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

## 6. Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/TramiteInstance.xml` | Modificar | `k-tramite` | Copia de `design/TramiteInstance.xml` (corrige la errata del `<help>` con el texto de ayuda de la especificación) |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/TipoExpedienteInstance.xml` | Modificar | `k-tipo-expediente` | Copia de `design/TipoExpedienteInstance.xml` (hoy tiene `<fases>` vacío) |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/domains.xml` | Crear | `k-tipo-expediente` | Copia de `design/domains.xml` |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/views.xml` | Crear | `k-tipo-expediente` | Copia de `design/views.xml` |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/estados.puml` | Crear | `k-tipo-expediente` | Copia de `design/estados.puml` |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/InitialEventManagerImpl.java` | Crear | `k-tipo-expediente` | Lo especifica `## 8. Especificación del InitialEventManagerImpl` |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/ControlDeAcceso.java` | Crear | `k-tipo-expediente`, `k-secure-coding` | Lo especifica `## 9. Especificación de los PhaseEventManagerImpl` (§9.0.1): las **tres** guardas de identidad del tipo — `exigeSerElCreador`, `exigeMismoCentroQueElExpediente` y `exigeOstentarElPerfilDelEstado`, que no lleva ningún literal de perfil: pregunta cuál exige el estado a `States` y si el usuario lo ostenta a `PerfilesUsuarioService.getPerfilesSobreExpediente` |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/DevolucionDelDirector.java` | Crear | `k-tipo-expediente`, `k-code-quality` | Lo especifica `## 9. Especificación de los PhaseEventManagerImpl` (§9.0.2): dueño único de «la devolución del director» |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/ReglasAnulacionMatricula.kt` | Crear | `k-tipo-expediente`, `k-secure-coding`, `k-code-quality` | **Dos** declaraciones de la raíz de la versión: la regla del DSL `SinOtraSolicitudEnCursoParaElMismoCiclo`, que especifica `## 10. Especificación de los StateEventValidatorImpl` (§10.0), y el `object ReglasAnulacionMatricula` con el predicado `esRechazo(expediente)`, dueño único en servidor de «la revisión rechaza la anulación», que especifica §9.0.3. Las dos están aquí, y no en el `<extra-code-model>` del `domains.xml`, porque el build reescribe ese bloque entero en cada compilación |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/solicitud/PhaseEventManagerImpl.java` | Crear | `k-tipo-expediente` | Lo especifica `## 9. Especificación de los PhaseEventManagerImpl` (fase `SOLICITUD`) |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/solicitud/StateEventValidatorImpl.kt` | Crear | `k-tipo-expediente`, `k-secure-coding` | Lo especifica `## 10. Especificación de los StateEventValidatorImpl` (fase `SOLICITUD`) |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/solicitud/views.xml` | Crear | `k-tipo-expediente` | Copia de `design/fases/solicitud/views.xml` |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/revision/PhaseEventManagerImpl.java` | Crear | `k-tipo-expediente` | Lo especifica `## 9. Especificación de los PhaseEventManagerImpl` (fase `REVISION`) |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/revision/StateEventValidatorImpl.kt` | Crear | `k-tipo-expediente`, `k-secure-coding` | Lo especifica `## 10. Especificación de los StateEventValidatorImpl` (fase `REVISION`) |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/revision/views.xml` | Crear | `k-tipo-expediente` | Copia de `design/fases/revision/views.xml` |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/resolucion/PhaseEventManagerImpl.java` | Crear | `k-tipo-expediente` | Lo especifica `## 9. Especificación de los PhaseEventManagerImpl` (fase `RESOLUCION`) |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/resolucion/StateEventValidatorImpl.kt` | Crear | `k-tipo-expediente`, `k-secure-coding` | Lo especifica `## 10. Especificación de los StateEventValidatorImpl` (fase `RESOLUCION`) |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/resolucion/views.xml` | Crear | `k-tipo-expediente` | Copia de `design/fases/resolucion/views.xml` |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/documentospdf/solicitud.xml` | Crear | `k-tipo-expediente` | Copia de `design/documentospdf/solicitud.xml` |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/documentospdf/resolucion.xml` | Crear | `k-tipo-expediente` | Copia de `design/documentospdf/resolucion.xml` |
| `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1/documentospdf/modelo.pdf` | Modificar | `k-tipo-expediente` | **Moverlo** (`git mv`) a `…/v1/documentospdf/originales/modelo.pdf`. Motivo: todo `.pdf` directo en `documentospdf/` genera una constante en `TipoDocumentoPdf`, y el modelo escaneado del ANEXO VII no es un documento del trámite |
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/domains/Nivel.xml` | Modificar | `k-sistemas` | Campo `nombreCorto` (§4, «Cambios en el catálogo») |
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/service/impl/NivelServiceImpl.java` | Modificar | `k-sistemas`, `k-secure-coding` | Añadir `nombreCorto` a la whitelist de `allowPropertiesInsert`/`allowPropertiesUpdate` (el `Map.of` de `allowPropertiesEditables()`). Sin esto el campo se descarta en silencio al guardar y el mantenimiento de niveles no puede darle valor (§4, «Cambios en el catálogo», y Paso 4) |
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/domains/Ciclo.xml` | Modificar | `k-sistemas`, `k-validaciones` | Campo derivado `gradoNivel` (§4, «Cambios en el catálogo») |
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/data-init/input-config.xml` | Modificar | `k-datainit` | Bind de `@nombreCorto` en el `<input>` de `Nivel.xml` |
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/data-init/input/Nivel.xml` | Modificar | `k-datainit` | `nombreCorto` de `GB`, `GM` y `GS` |
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/data-init/input/Ciclo.xml` | Modificar | `k-datainit` | Alta **solo** del curso de especialización `IABD`, el único que exige la especificación (§4, «Cambios en el catálogo»). Los ocho ciclos que necesita el reparto de parejas (alumno, ciclo) de `test-e2e-desc.md` **NO** van aquí: son configuración del entorno de pruebas (§4 y §14 nota 23) |
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/views/Main-Nivel.xml` | Modificar | `k-vistas` | Mostrar `nombreCorto` en el mantenimiento de niveles |
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/views/Ref-Ciclo.xml` | Modificar | `k-vistas` | Columnas `grado` y `nivel` en `subsysSistemaEducativo.Ref@Ciclo-grid` |
| `src/main/java/com/educaflow/base/infrastructure/validation/rules/RequiredRules.kt` | Modificar | `k-validaciones` | `Required` pasa a `data class Required(val mensaje: String = "Es requerido")`. El parámetro sustituye **solo** el mensaje de la rama «valor ausente»; las ramas «No puede estar vacío» y «No puede ser cero» conservan el suyo. Los tres textos pasan además por `I18n.get(...)` (Paso 5) |
| `src/main/java/com/educaflow/base/infrastructure/validation/rules/StringRules.kt` | Modificar | `k-validaciones` | Mensaje opcional en `Pattern` (valor por defecto = el literal actual) y en `MinLength`/`MaxLength` (`mensaje: String? = null`; con `null` se sigue devolviendo el mensaje **calculado** actual). Los textos pasan además por `I18n.get(...)` (Paso 5) |
| `src/main/java/com/educaflow/base/infrastructure/validation/rules/ConditionalRules.kt` | Modificar | `k-validaciones` | Regla **nueva** del catálogo común `NoAdmitido(mensaje)`: falla siempre con ese mensaje; se usa **solo** dentro de una rama `ifValueIn`/`ifValueNotIn`, junto a las cuales vive (Paso 5, `decisiones.md` D7 y §14 nota 21) |
| `src/main/java/com/educaflow/base/infrastructure/validation/rules/PdfRules.kt` | Modificar | `k-validaciones` | Segundo parámetro opcional `mensajeFirmaInvalida` en `FirmaPdf`, con `null` por defecto (§10.1 y Paso 5) |
| `src/main/java/com/educaflow/tramites/util/bandeja/BandejaPorPerfilController.java` | Crear | `k-sistemas`, `k-secure-coding`, `k-vistas` | Punto de consulta de las dos bandejas nuevas: `@CallMethod List<Long> idsExpedientesConPerfil(nombrePerfil, tramiteCode)`, que **pregunta** a `PerfilesUsuarioService.getPerfilesSobreExpediente` en vez de reescribir su JPQL en la vista. Lo especifica el **Paso 16.0** |
| `src/main/java/com/educaflow/tramites/views/Expediente-revision.xml` | Crear | `k-vistas` | Bandeja de la administrativa: `action-view` + `grid` con `_profile=SECRETARIO` y `<domain>self.id IN (:expedientesConPerfil)</domain>` alimentado por el punto de consulta de 16.0 (abiertos, de este trámite, de su centro y con el perfil `SECRETARIO`). Escrito entero en el **Paso 16.1** |
| `src/main/java/com/educaflow/tramites/views/Expediente-firma.xml` | Crear | `k-vistas` | Bandeja del director: lo mismo con `_profile=DIRECTOR` y una condición propia más en el `<domain>`, `self.codeState = 'PENDIENTE_FIRMA_DIRECTOR'`. Escrito entero en el **Paso 16.2** |
| `src/main/java/com/educaflow/tramites/views/Expediente-pendiente.xml` | Modificar | `k-vistas` | Filtro de centro en el `<domain>`, **sin** excepción para el administrador: es la única genérica que fija `_profile=CREADOR` (§12.2 y Paso 16.3 (a)) |
| `src/main/java/com/educaflow/tramites/views/Abierto-Expediente.xml` | Modificar | `k-vistas` | Filtro de centro en los dos `<node domain=…>` del árbol; su `action-view` no tiene `<domain>` (§12) |
| `src/main/java/com/educaflow/tramites/views/Cerrado-Expediente.xml` | Modificar | `k-vistas` | Filtro de centro en el `<domain>`, con excepción para el administrador (§12) |
| `src/main/java/com/educaflow/tramites/views/Expediente-search.xml` | Modificar | `k-vistas` | Filtro de centro en los dos `<node domain=…>` de la búsqueda (§12) |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | `k-vistas` | Dos `menuitem` nuevos bajo «Expedientes» para las dos bandejas |
| `src/main/resources/data-demo/input/permisos-demo.xml` | **Modificar** | `k-datainit` | Fusión del fragmento `design/permisos.xml` (§12) |
| `src/main/resources/data-demo/input/usuarios-demo.xml` | **Modificar** | `k-datainit` | Una línea `<centroUsuario>` para `admin`, sin la cual el administrador no puede abrir ningún expediente (§12.1 y §14 nota 13) |

## 7. Pasos

### Paso 1 — `TramiteInstance.xml`

El fichero está materializado en `design/TramiteInstance.xml`. **Cópialo literalmente** a `src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/TramiteInstance.xml`, **sobrescribiendo** el actual. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

**Verificación:** el fichero existe y `diff` contra `design/TramiteInstance.xml` es vacío; el `<help>` ya no contiene «permite a ja un alumno».

### Paso 2 — `TipoExpedienteInstance.xml` completo

El fichero está materializado en `design/TipoExpedienteInstance.xml`. **Cópialo literalmente** a `…/v1/TipoExpedienteInstance.xml`, **sobrescribiendo** el actual (que tiene `<fases>` vacío). **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

**Verificación:** `diff` vacío; están las tres fases con sus seis estados y todos los `events` escritos, incluido `events=""` en los dos cerrados.

### Paso 3 — Ejecutar `CreateFilesTask`

```
./gradlew -q CreateFilesTask -Ptipo=src/main/java/com/educaflow/tramites/alumnos/anulacion_matricula_ciclo_formativo/v1
```

- La tarea **lee** el `TipoExpedienteInstance.xml`, así que el paso 2 tiene que estar **completo** antes: cada `<fase>` declarada produce su subcarpeta.
- Crea, en la raíz de la versión, `domains.xml`, `views.xml` e `InitialEventManagerImpl.java`; y en **cada** una de las carpetas `solicitud/`, `revision/` y `resolucion/`, su `PhaseEventManagerImpl.java`, su `StateEventValidatorImpl.kt` y su `views.xml`.
- Imprime una línea `CREADO <ruta>` por fichero creado: la verificación es que aparezca **una línea `CREADO` por cada uno de esos doce ficheros**.
- Es **idempotente** y nunca pisa lo ya escrito.
- **MUST NOT** usarse `-Pfase` (acota a una fase y entonces no genera los ficheros de la raíz de la versión); si alguna vez se usara, **MUST** ir siempre junto con `-Ptipo`.
- **MUST NOT** engancharse al build: compilar no debe escribir en `src/main/java`.

### Paso 4 — Catálogo del sistema educativo

Aplica los **ocho** cambios de §4 «Cambios en el catálogo del sistema educativo» sobre `subsystem/sistemaeducativo`: `domains/Nivel.xml`, `domains/Ciclo.xml`, `data-init/input-config.xml`, `data-init/input/Nivel.xml`, `data-init/input/Ciclo.xml`, `views/Main-Nivel.xml`, `views/Ref-Ciclo.xml` y `service/impl/NivelServiceImpl.java`. Va **antes** del modelo y de las vistas del tipo porque `Ciclo.gradoNivel` es lo que ambos consultan.

**CRITICAL — el campo nuevo `nombreCorto` NO basta con declararlo y pintarlo: hay que abrirle la whitelist del `ModelService`.** `NivelServiceImpl.allowPropertiesEditables()` —de donde salen `allowPropertiesInsert()` y `allowPropertiesUpdate()`— devuelve hoy `AllowProperties.createAllowProperties(Map.of("code", …, "name", …, "grado", …))`, una whitelist **cerrada**: lo que no está en ella se descarta **en silencio** al guardar, sin error ni aviso. Si `nombreCorto` no entra ahí, el campo se pinta en `Main-Nivel.xml`, el usuario lo escribe, el formulario guarda «bien»… y el valor no llega nunca a la fila. **MUST** añadirse `"nombreCorto", Map.of()` a ese `Map.of`, dejando intacto el resto del servicio (sus `validateInsert`/`validateUpdate` no cambian: el campo **no** es obligatorio, §4). El fallo **no** lo destapa comprobar los nombres cortos que siembra el data-init, porque el data-init **no** pasa por el `ModelService`.

**CRITICAL — este paso toca un catálogo compartido que YA tiene tests E2E persistidos.** `src/test/e2e/subsystem/sistemaeducativo/` contiene **16** ficheros `.spec.ts` —`t-001`…`t-015` **más** `crear-ley-educativa.spec.ts`, que el comando de abajo también ejecuta— y pilotan justo las pantallas y los datos que este paso modifica: el mantenimiento de niveles (`Main-Nivel.xml`, que gana `nombreCorto` en el grid y en el form), el selector de ciclo (`Ref-Ciclo.xml`, que gana las columnas `grado` y `nivel`) y el catálogo de ciclos y niveles del data-init. **Los ejecuta Playwright contra la aplicación arrancada, así que `./run.sh` NO los ejecuta**: hay que lanzarlos aparte.

```
npx playwright test src/test/e2e/subsystem/sistemaeducativo
```

- Se lanzan con la aplicación **arrancada** (el `baseURL` de `playwright.config.ts` es `http://localhost:8080`).
- Los **16 MUST** quedar en verde. Este paso da de alta **un** ciclo nuevo (`IABD`; los ocho del reparto de tests son configuración del entorno, §4 y §14 nota 23), así que lo primero que hay que comprobar es que ninguno de los 16 cuenta filas: ninguno afirma en su código el número total de ciclos ni el número de columnas del grid de niveles —el único `toHaveCount` sobre columnas es el de `t-012`, y va sobre `Main@Ciclo-grid`, que este paso **no** toca—, así que lo esperado es que pasen sin cambios. Si alguno falla, el fallo es una **regresión de este paso** y se arregla en el código de la aplicación.
- **MUST NOT** editarse ningún fichero de `src/test/e2e/**` para hacerlos pasar: son snapshots «as-tested» de otra iniciativa y llevan escrito «NO editar a mano». Lo que sí queda desactualizado es **prosa** de sus `.desc.md`, y de eso habla §14 nota 22.

**Verificación:** compila; el cuerpo de `Ciclo.gradoNivel` lee los **campos** `nivel` y `grado` (nunca sus getters); `NivelServiceImpl.allowPropertiesEditables()` lista **cuatro** propiedades (`code`, `name`, `nombreCorto`, `grado`) y, con la aplicación arrancada, **dar de alta un Nivel nuevo con nombre corto desde «Sistema educativo → Niveles» (`Main-Nivel.xml`) y luego editárselo deja el valor persistido** tras recargar la ficha —es la única comprobación que ve el agujero de la whitelist, porque un descarte de propiedad no da error—; el grid `subsysSistemaEducativo.Ref@Ciclo-grid` tiene cuatro columnas; tras arrancar, el catálogo tiene **ocho** ciclos —los siete de siempre más el curso de especialización `IABD`— y los tres niveles con su nombre corto; y `npx playwright test src/test/e2e/subsystem/sistemaeducativo` termina con los **16 ficheros `.spec.ts` en verde**. Los ocho ciclos del reparto de tests **MUST NOT** aparecer en `data-init/input/Ciclo.xml`: se dan de alta por la pantalla de mantenimiento antes de lanzar la suite de este trámite (§4 y §14 nota 23).

### Paso 5 — Reglas de validación de `base`

Aplica los cuatro cambios de la tabla §6 sobre `base/infrastructure/validation/rules/`: mensaje opcional en `Required`, `Pattern`, `MinLength` y `MaxLength`, regla nueva `NoAdmitido(mensaje)` que falla siempre con ese mensaje, y en `PdfRules.kt` la ampliación **aditiva** de `FirmaPdf`.

**Todos los mensajes de estas reglas —el recibido por parámetro y el literal por defecto— se devuelven con `I18n.get(...)`**, igual que hace `ClaveCertificadoValida`. Hoy `Required`, `Pattern`, `MinLength` y `MaxLength` devuelven sus literales **sin** `I18n.get`, así que esto **sí es un cambio de comportamiento** y **MUST** declararse como tal en vez de negarlo:

- **Qué NO cambia:** ninguna llamada existente cambia de **firma** (los parámetros son opcionales y con valor por defecto, así que `Required()`, `Pattern("…")`, `MinLength(5)`, `MaxLength(10)` y `FirmaPdf(model::getX)` siguen compilando) ni de **texto en castellano** (el literal es su propia clave de traducción: sin entrada de traducción, `I18n.get` devuelve el mismo texto).
- **Qué SÍ cambia, y es deliberado:** en **valenciano**, esos mensajes pasan a salir traducidos en lugar de en castellano. Es lo que `k-i18n` exige de todo texto que ve el usuario, y arreglarlo aquí es gratis porque el cambio ya toca esas líneas. **MUST NOT** dejarse la mitad del catálogo pasando por `I18n.get` y la otra mitad no.
- **Los literales no se tocan**: «Es requerido», «No puede estar vacío», «No puede ser cero», «El valor no cumple con el patrón especificado» y los dos mensajes calculados de longitud siguen siendo **exactamente** los mismos textos; lo único que se les añade es el paso por el traductor.

**`NoAdmitido` — la única regla NUEVA del catálogo, y dónde va.** `data class NoAdmitido(val mensaje: String) : ValidationRule`, con `validate(value, bean)` devolviendo **siempre** `BusinessMessages.single(I18n.get(mensaje))`, sin mirar el valor.

- Va en **`ConditionalRules.kt`**, junto a `IfValueIn`/`IfValueNotIn`, que son las únicas con las que tiene sentido: quien abra ese fichero para escribir una rama se la encuentra al lado.
- **MUST** llevar un KDoc que declare su restricción de uso: *se usa solo dentro de una rama `ifValueIn`/`ifValueNotIn`; la rama dice cuándo aplica y esta regla dice con qué mensaje se rechaza*. Suelta rechazaría cualquier valor.
- Es **aditiva**: es una clase nueva, ninguna llamada existente la ve.
- El porqué, con las alternativas descartadas, está en `decisiones.md` **D7** y en §14 nota 21.

**`Required` — qué mensaje sustituye exactamente.** La regla actual devuelve **tres** mensajes distintos según el valor: «Es requerido» (valor `null`, `String` en blanco o `MetaFile` sin `fileName`), «No puede estar vacío» (`MetaFile` de tamaño 0) y «No puede ser cero» (`Number` a 0). Pasa a `data class Required(val mensaje: String = "Es requerido")` y el parámetro sustituye **ÚNICAMENTE** el mensaje de la rama de **valor ausente** (las tres condiciones que hoy devuelven «Es requerido»).

- **MUST** quedar **intactos** los literales «No puede estar vacío» y «No puede ser cero»: dicen otra cosa (el dato está, pero vacío o a cero) y taparlos con el texto de la spec sería una regresión de comportamiento en llamadas existentes.
- **MUST NOT** darse un parámetro por rama: ninguna llamada de este trámite lo necesita y multiplicaría la API por tres.
- Con el valor por defecto, un `Required()` existente sigue devolviendo exactamente los tres mensajes de hoy.

**`Pattern`, `MinLength` y `MaxLength` — dos formas distintas, porque su mensaje actual no es el mismo tipo de cosa.**

- `Pattern` pasa a `data class Pattern(val regex: String, val mensaje: String = "El valor no cumple con el patrón especificado")`: su mensaje actual es un literal **constante**, así que sí es expresable como valor por defecto.
- `MinLength` y `MaxLength` pasan a `data class MinLength(val min: Int, val mensaje: String? = null)` y `data class MaxLength(val max: Int, val mensaje: String? = null)`. Su mensaje actual **se construye en tiempo de validación con el valor** («Debe tener como mínimo una longitud de $min pero tiene ${value.length}»), así que **MUST NOT** intentar ponerse como valor por defecto del parámetro: no es una constante. Con `mensaje == null` la regla devuelve el mensaje **calculado** de hoy, y solo cuando no es nulo devuelve el literal recibido. Es el mismo patrón que `FirmaPdf` en este mismo paso.

`FirmaPdf` pasa a ser `data class FirmaPdf(val documentoOriginalField: KCallable<*>, val mensajeFirmaInvalida: String? = null)`:

- Cuando `mensajeFirmaInvalida` **no** es nulo sustituye **solo** el mensaje de la rama de **firma inválida** — el que hoy devuelve `errorMessage.get()` de `DocumentoPdfUtil.validateFirmaPdf` (firma ausente, certificado no confiable, texto alterado, DNI distinto…) — por ese texto.
- **MUST** quedar **intacto** el mensaje de la otra rama, la de «No es posible comprobar la firma porque su usuario no tiene un documento de identidad válido…»: es otra cosa (un problema de la cuenta, no de la firma) y **MUST NOT** taparse con el literal de la spec.
- Con `null` por defecto ninguna llamada existente cambia: la de `justificacion_falta_profesorado` sigue compilando y comportándose igual.

**Verificación:** compila y los trámites existentes siguen compilando **sin tocarlos** (`Required()`, `Pattern("…")`, `MinLength(5)`, `MaxLength(10)`, `FirmaPdf(model::getX)` siguen siendo llamadas válidas). **En castellano** el texto es el de hoy, literal a literal: un `Required()` sobre un `MetaFile` de tamaño 0 sigue diciendo «No puede estar vacío»; sobre un número a 0, «No puede ser cero»; un `MinLength(5)` sobre un texto de 3 sigue diciendo «Debe tener como mínimo una longitud de 5 pero tiene 3»; un `MaxLength` desbordado, «Debe tener como máximo una longitud de … pero tiene …»; un `Pattern` incumplido, «El valor no cumple con el patrón especificado»; y la llamada `FirmaPdf(model::getX)` de `justificacion_falta_profesorado` sigue devolviendo el motivo concreto que calcula `DocumentoPdfUtil.validateFirmaPdf`. **En valenciano** esos mismos mensajes salen traducidos: es el cambio de comportamiento declarado arriba, y **MUST** comprobarse que no falta ninguna traducción (si `apertium` no supiera traducir una palabra, el build falla, §14 nota 7). Y `NoAdmitido` existe en `ConditionalRules.kt`, implementa `ValidationRule`, devuelve su mensaje para **cualquier** valor (incluido `null`) y lleva el KDoc con su restricción de uso.

### Paso 6 — `domains.xml`

El fichero está materializado en `design/domains.xml`. **Cópialo literalmente** a `…/v1/domains.xml`, **sobrescribiendo** el esqueleto que dejó `CreateFilesTask`. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

**Verificación:** `diff` vacío; la entidad `AnulacionMatriculaCicloFormativoV1` es la **primera** `<entity>`, extiende `Expediente`, ningún campo lleva `required="true"` y el `<extra-code-model>` declara exactamente `SOLICITUD` y `RESOLUCION` (el build lo reescribirá con esas mismas dos constantes si el paso 14 se hizo bien). **MUST NOT** añadirse ningún método al `<extra-code-model>`: el build **sustituye el bloque entero** por el que genera su plantilla, así que lo que se escriba ahí a mano se pierde en la primera compilación — el código propio del tipo va en las clases de la raíz de la versión (Paso 9). Tras `./run.sh`, `grep -c esRechazo …/v1/domains.xml` **MUST** devolver `0`.

### Paso 7 — `views.xml` de la raíz de la versión

El fichero está materializado en `design/views.xml`. **Cópialo literalmente** a `…/v1/views.xml`, **sobrescribiendo** el esqueleto. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

**Resumen estructural** — un único form plantilla `exp-AnulacionMatriculaCicloFormativoV1-Templates` (`model` = la entidad, `width="large"`, `groups="admins,users"`) con 19 paneles. ASCII Layout de los paneles no triviales (rejilla de 12 columnas, cada fila suma 12):

```
datos-alumno
aaaaabbbbccc   ← apellidos(5) + nombre(4) + NIA(3)
dddeeeeeefff   ← DNI/NIE(3) + dirección(6) + teléfono(3)
gggggHHHHiii   ← población(5) + provincia(4) + código postal(3)

datos-alumno-resumen          (el director solo necesita identificar al alumno)
aaaabbbccddd   ← apellidos(4) + nombre(3) + NIA(2) + DNI/NIE(3)

matricula
aaabbbbbbbbb   ← curso académico(3) + centro(9)
cccccccccddd   ← ciclo(9) + grado o nivel(3)

matricula-director            (añade la fecha de presentación y su indicación)
                              (+ `sentidoRevision` oculto: el campo del que depende su showIf; no ocupa rejilla)
  · con sentidoRevision == ACEPTAR
aaabbbbbbbbb   ← curso académico(3) + centro(9)
cccccccccddd   ← ciclo(9) + grado o nivel(3)
eeeeffffffff   ← fecha y hora de presentación(4) + help «Si firma…»(8, showIf ACEPTAR)
  · con sentidoRevision != ACEPTAR (en la práctica, RECHAZAR)
aaabbbbbbbbb   ← curso académico(3) + centro(9)
cccccccccddd   ← ciclo(9) + grado o nivel(3)
eeee........   ← fecha y hora de presentación(4); el help desaparece y la fila queda en 4/12

presentacion
aaaa........   ← fecha y hora de presentación(4); el resto de la fila queda libre porque el visor ocupa 12
bbbbbbbbbbbb   ← visor del justificante de presentación (iframe)

presentacion-descarga
aaaabbbbbbbb   ← fecha y hora de presentación(4) + justificante como descarga(8)

presentacion-descarga-aceptada   (gemelo del anterior solo para ACEPTADA: la indicación de
                                  RUI-ACEPTADA-GENERICA-002 acompaña a la fecha, no al aviso del pie)
aaaabbbbbbbb   ← fecha y hora de presentación(4) + help «La anulación… desde esta fecha»(8)
cccccccccccc   ← justificante como descarga(12)

revision
  · con sentidoRevision == RECHAZAR
aaaaaaaa....   ← sentido de la revisión(8, SwitchSelect); nada más cabe en su fila: los dos textos de abajo son de 12
bbbbbbbbbbbb   ← motivo del rechazo(12, showIf RECHAZAR)
  · con sentidoRevision == SUBSANAR
aaaaaaaa....   ← sentido de la revisión(8)
cccccccccccc   ← qué hay que subsanar(12, showIf SUBSANAR)
  · con sentidoRevision == ACEPTAR o sin elegir (estado inicial de la pantalla)
aaaaaaaa....   ← sentido de la revisión(8); el panel queda en una sola fila

devolucion-view
aaaaaaaaaaaa   ← motivo de la devolución(12)
bbbbcccccccc   ← fecha de la devolución(4) + devuelto por(8)

decision-secretaria
  · con sentidoRevision == RECHAZAR
aaaaabbbcccc   ← sentido de la revisión(5) + fecha de la revisión(3) + revisado por(4)
dddddddddddd   ← motivo del rechazo(12, showIf RECHAZAR)
  · con sentidoRevision != RECHAZAR
aaaaabbbcccc   ← sentido de la revisión(5) + fecha de la revisión(3) + revisado por(4)
               (la fila del motivo desaparece entera; la que queda sigue sumando 12)

resolucion-firmada
                              (+ `sentidoRevision` oculto: el campo del que depende su showIf; no ocupa rejilla)
  · con sentidoRevision == RECHAZAR (estado RECHAZADA)
aaaaaaaaaaaa   ← motivo del rechazo(12, showIf RECHAZAR)
bbbbcccccccc   ← fecha de la resolución(4) + firmado por(8)
dddddddddddd   ← visor de la resolución firmada (iframe)
  · con sentidoRevision != RECHAZAR (estado ACEPTADA)
bbbbcccccccc   ← fecha de la resolución(4) + firmado por(8)
dddddddddddd   ← visor de la resolución firmada (iframe)
               (la fila del motivo desaparece entera; las que quedan siguen sumando 12)
```

**Las dos filas que quedan por debajo de 12 al no cumplirse su condición, y por qué se quedan así.**

- `matricula-director`, tercera fila con `sentidoRevision != ACEPTAR`: quedan los 4 de `fechaHoraPresentacion` y 8 libres. **No se remaqueta.** Ensanchar el campo a 12 exigiría cambiar su `colSpan` según el sentido —es decir, un `<action-attrs>` más y **una decisión de layout con dos dueños** (el XML y una acción)— para ganar solo que una fecha ocupe el ancho de la pantalla, que además la leería peor: la tabla de proporcionalidad de `k-vistas/forms.md` da 4 a una fecha con hora, no 12. El hueco a la derecha de una fecha es el mismo que ya tienen `presentacion-descarga` (4 + 8) y `devolucion-view` (4 + 8) cuando el segundo campo está vacío.
- `revision`, primera fila: `sentidoRevision` ocupa 8 **en todos los estados**, no solo cuando la condición no se cumple. Es un `SwitchSelect` con tres opciones de texto largo («Aceptar la anulación», «Rechazar la anulación», «Pedir subsanación»), que necesita ese ancho para no partir las etiquetas; y los dos campos que podrían acompañarlo son textos largos de 12 que van en su propia fila. Subirlo a 12 estiraría los botones sin ganar nada. Es el hueco justificado que `vistas.md` §2.7 permite, no un campo corto abandonado a mitad de fila.

Los paneles cuyo `showIf` está en el **propio panel** (`subsanacion` y `devolucion-view`) tienen solo dos estados —el panel entero se ve con el dibujo de arriba, o no se ve—, así que un único dibujo los describe por completo.

Los ocho paneles restantes son triviales: `subsanacion` (un texto a 12), los cinco visores/descargas de documentos (`solicitud-visor`, `solicitud-descarga`, `solicitud-firmada-visor`, `solicitud-firmada-descarga`, `justificante-descarga`), `resolucion-a-firmar` (un `<help>` a 12 + el visor a 12) y `devolucion` (un texto a 12).

`resolucion-firmada` sirve a los **dos** estados cerrados: el motivo del rechazo aparece o no según `sentidoRevision`, así que no hace falta un gemelo por estado.

**INVARIANTE — un panel MUST ser autosuficiente.** Toda expresión `showIf`/`hideIf`/`requiredIf` de un panel del form plantilla **MUST** citar **solo campos que ese mismo panel pinta**; si el campo no tiene por qué verse, se pinta con `hidden="true"`. **Motivo, y por qué no es estilo sino corrección:** los campos que el cliente pide al servidor salen de `meta.fields`, que se construye con los `<field>` del form final (`axelor-front/src/views/form/form.tsx`, `fetchRecord` → `Object.keys(meta.fields)`), y **ni `MetaService` ni el preprocesador recolectan los nombres escritos dentro del literal de un `showIf`**. Un panel cuya condición cita un campo que ningún panel del form pinta recibe `undefined` y la condición sale **siempre falsa, sin error**: el hueco queda vacío y nadie se entera. Como los paneles se comparten entre estados y fases (§2.3 de `vistas.md`), depender de que «otro panel del mismo form ya pinta ese campo» es una trampa que se arma sola en cuanto se recompone la lista de `<include-panels>` de cualquier estado. Los dos paneles que condicionan por un campo que no es suyo —`matricula-director` (el `<help>` de la fecha, por `sentidoRevision`) y `resolucion-firmada` (el motivo del rechazo, por `sentidoRevision`)— llevan por eso ese campo declarado `hidden="true"` en el propio panel. Un campo `hidden` **no ocupa rejilla** (el widget no se monta, `form-widget.tsx` devuelve `null` cuando `hidden`), así que no altera ningún ASCII Layout. La decisión, con las alternativas descartadas, está en `decisiones.md` **D12**. **MUST NOT** añadirse a este tipo ningún `showIf`/`hideIf`/`requiredIf` que cite un campo que su propio panel no pinte.

**Verificación:** `diff` vacío; hay exactamente un `<form name="exp-AnulacionMatriculaCicloFormativoV1-Templates">`; no hay ningún `<form state=…>` en este fichero; todos los paneles tienen `name` y son hijos directos del form plantilla.

### Paso 8 — `InitialEventManagerImpl.java`

Fichero: `…/v1/InitialEventManagerImpl.java`. FQCN: `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.InitialEventManagerImpl`.

Su especificación quirúrgica está en **`## 8. Especificación del InitialEventManagerImpl`**; **MUST** leerse el `design.md` entero, no solo este paso.

Supertipo: `implements InitialEventManager<AnulacionMatriculaCicloFormativoV1>` (parámetro de tipo obligatorio: es lo que `ExpedienteLocator.getModelClass` lee en runtime).

**Verificación:** compila; declara **exactamente un** `void triggerInitialEvent(AnulacionMatriculaCicloFormativoV1, EventContext)`; existe una asignación por cada fila de la tabla de §8, en ese orden; `personaSolicitante` y `personaInteresada` quedan no nulos.

### Paso 9 — Clases auxiliares de la versión

Ficheros: `…/v1/ControlDeAcceso.java` (FQCN `…v1.ControlDeAcceso`), `…/v1/DevolucionDelDirector.java` (FQCN `…v1.DevolucionDelDirector`) y `…/v1/ReglasAnulacionMatricula.kt` (paquete `…v1`).

Sus especificaciones quirúrgicas están en **`## 9. Especificación de los PhaseEventManagerImpl`** (§9.0.1 `ControlDeAcceso`, §9.0.2 `DevolucionDelDirector` y §9.0.3 `ReglasAnulacionMatricula.esRechazo`) y en **`## 10. Especificación de los StateEventValidatorImpl`** (§10.0 `SinOtraSolicitudEnCursoParaElMismoCiclo`). El fichero `ReglasAnulacionMatricula.kt` lleva **las dos** declaraciones de §9.0.3 y §10.0. Van antes de los `PhaseEventManagerImpl` y de los validadores porque ambos las usan.

**CRITICAL:** ninguna de estas piezas **MUST** escribirse dentro del `<extra-code-model>` del `domains.xml` (Paso 6): el build reescribe ese bloque entero en cada compilación y lo que se escriba ahí a mano desaparece.

**Verificación:** compilan; `ControlDeAcceso` y `DevolucionDelDirector` no tienen estado ni constructor público; `ControlDeAcceso` declara **tres** métodos (`exigeSerElCreador`, `exigeMismoCentroQueElExpediente` y `exigeOstentarElPerfilDelEstado`), el tercero **no recibe ningún `Profile` por parámetro** —obtiene el perfil exigido con `States.INSTANCE.getState(expediente.getCodePhase(), expediente.getCodeState())` y `getProfile()`, y no exige nada si el estado no declara perfil—, resuelve `PerfilesUsuarioService` con `Beans.get(...)` y **no** contiene ninguna consulta propia sobre `Ace`; `grep -n "Profile\.\(SECRETARIO\|DIRECTOR\|CREADOR\|RESPONSABLE\)" ` sobre los tres `PhaseEventManagerImpl` y sobre `ControlDeAcceso.java` devuelve **0 líneas**: ningún literal de perfil se escribe en código; `DevolucionDelDirector` declara **un solo** método (`borrar`) y ningún `trigger*` vuelve a escribir la lista `motivoDevolucion, fechaDevolucion, devueltoPor`; `SinOtraSolicitudEnCursoParaElMismoCiclo` implementa `ValidationRule`; `ReglasAnulacionMatricula.esRechazo` es `@JvmStatic`, se invoca sin problemas desde Java (`ReglasAnulacionMatricula.esRechazo(expediente)`) y `grep -c esRechazo` sobre el `domains.xml` ya materializado devuelve **0** después de compilar.

### Paso 10 — `PhaseEventManagerImpl.java` de cada fase

Ficheros: `…/v1/solicitud/PhaseEventManagerImpl.java`, `…/v1/revision/PhaseEventManagerImpl.java` y `…/v1/resolucion/PhaseEventManagerImpl.java` (FQCN = paquete de la fase + `.PhaseEventManagerImpl`).

Su especificación quirúrgica está en **`## 9. Especificación de los PhaseEventManagerImpl`**, con una subsección por fase.

Supertipo: `extends PhaseEventManager<AnulacionMatriculaCicloFormativoV1>`, con constructor `@Inject` que recibe `AnulacionMatriculaCicloFormativoV1Repository` y llama a `super(AnulacionMatriculaCicloFormativoV1.class)`.

**Verificación:** compilan; por cada fase hay **un** `@WhenEvent trigger<Evento>` por cada evento de su lista de cobertura (§9.4) y **un** `@OnEnterState onEnter<Estado>` por cada estado de esa lista, y ninguno de más; ningún `triggerInitialEvent` ni `triggerExit`; el `import` de `States` es el de esta misma versión; y **los ocho `trigger*` abren con una guarda de `ControlDeAcceso`** — los cuatro de `SOLICITUD` con `exigeSerElCreador` y los cuatro de `REVISION`/`RESOLUCION` con `exigeMismoCentroQueElExpediente` **seguida de** `exigeOstentarElPerfilDelEstado` (§11, «Las ocho guardas de identidad»), **ninguna de ellas con un literal de perfil como argumento** y sin `import` de `Profile` en ningún `PhaseEventManagerImpl`.

### Paso 11 — `StateEventValidatorImpl.kt` de cada fase

Ficheros: `…/v1/solicitud/StateEventValidatorImpl.kt`, `…/v1/revision/StateEventValidatorImpl.kt` y `…/v1/resolucion/StateEventValidatorImpl.kt` (paquete de la fase).

Su especificación quirúrgica está en **`## 10. Especificación de los StateEventValidatorImpl`**, con una subsección por fase. Interfaz: `StateEventValidator`.

**Verificación:** compilan; hay exactamente un `@BeanValidationRulesForStateAndEvent getForState<Estado>InEvent<Evento>()` por cada fila «sí» o «`rules { }` vacío» de la tabla de cobertura de §10, y **ninguno** para `DELETE`; ningún `field(...)` menciona un campo clasificado `servidor` en §4 (salvo `pdfSolicitudFirmada`, la excepción documentada).

### Paso 12 — `views.xml` de cada fase

Los ficheros están materializados en `design/fases/solicitud/views.xml`, `design/fases/revision/views.xml` y `design/fases/resolucion/views.xml`. **Cópialos literalmente** a `…/v1/solicitud/views.xml`, `…/v1/revision/views.xml` y `…/v1/resolucion/views.xml`, **sobrescribiendo** los esqueletos. **MUST NOT** modificarlos, reescribirlos ni regenerarlos.

**Resumen estructural por fase** (`(estado, perfil) → paneles → botones`):

| fase | estado | profile | paneles incluidos | botones (izq / der) |
|---|---|---|---|---|
| SOLICITUD | `DATOS_SOLICITUD` | `CREADOR` | `-subsanacion`, `datos-alumno`, `matricula` | `DELETE` / `CONTINUAR` |
| SOLICITUD | `DATOS_SOLICITUD` | — | `-subsanacion`, `-datos-alumno`, `-matricula` | — / `EXIT` |
| SOLICITUD | `PENDIENTE_FIRMA` | `CREADOR` | `-subsanacion`, `-solicitud-visor` + paneles de situación de firma | `VOLVER` / `PRESENTAR` ×2 |
| SOLICITUD | `PENDIENTE_FIRMA` | — | `-datos-alumno`, `-matricula`, `-solicitud-descarga` | — / `EXIT` |
| REVISION | `PENDIENTE_REVISION` | `SECRETARIO` | `-devolucion-view`, `-datos-alumno`, `-matricula`, `-presentacion-descarga`, `-solicitud-firmada-visor`, `revision` | `SUBSANAR` / `ENVIAR_A_FIRMA` |
| REVISION | `PENDIENTE_REVISION` | — | `-datos-alumno`, `-matricula`, `-presentacion`, `-solicitud-firmada-descarga` | — / `EXIT` |
| RESOLUCION | `PENDIENTE_FIRMA_DIRECTOR` | `DIRECTOR` | `-datos-alumno-resumen`, `-matricula-director`, `-decision-secretaria`, `-resolucion-a-firmar`, `-solicitud-firmada-descarga`, `-justificante-descarga`, `devolucion` | `DEVOLVER` / `FIRMAR` |
| RESOLUCION | `PENDIENTE_FIRMA_DIRECTOR` | — | `-datos-alumno`, `-matricula`, `-presentacion`, `-solicitud-firmada-descarga` | — / `EXIT` |
| RESOLUCION | `ACEPTADA` | — | `-datos-alumno`, `-matricula`, `-presentacion-descarga-aceptada`, `-resolucion-firmada`, `-solicitud-firmada-descarga` | — / `EXIT` |
| RESOLUCION | `RECHAZADA` | — | `-datos-alumno`, `-matricula`, `-presentacion-descarga`, `-resolucion-firmada`, `-solicitud-firmada-descarga` | — / `EXIT` |

Cada form lleva además, **fuera** del `<include-panels>`, un `<panel showFrame="false">` con el `<help>` del aviso permanente que la especificación fija para esa pantalla. **Excepción:** la indicación de RUI-ACEPTADA-GENERICA-002 **no** va en ese panel de avisos, sino dentro del panel `presentacion-descarga-aceptada`, junto a la fecha y la hora de presentación a las que acompaña (mismo patrón que `matricula-director` para RUI-PENDIENTE_FIRMA_DIRECTOR-DIRECTOR-007).

Las acciones propias van en el `views.xml` de su fase: en `solicitud/`, las cuatro de la firma más el `<action-attrs>` `…-DATOS_SOLICITUD-onLoad-action` que marca como obligatorios en pantalla los siete campos del alumno (RUI-DATOS_SOLICITUD-CREADOR-002); en `revision/`, la que limpia el motivo/el texto al cambiar el sentido.

**Verificación:** `diff` vacío en los tres; ningún `<button name="">` sin rellenar; cada estado tiene su form genérico con botón `EXIT`; los estados con `profile` y eventos tienen además su form de perfil; la suma de `colSpan` de los botones de cada footer no pasa de 12.

### Paso 13 — `estados.puml`

El fichero está materializado en `design/estados.puml`. **Cópialo literalmente** a `…/v1/estados.puml`. **MUST NOT** modificarlo, reescribirlo ni regenerarlo.

**Verificación:** `diff` vacío; aparecen los seis estados con alias `<FASE>_<ESTADO>` y las diez transiciones de §3. El `.png` lo genera `GenerateDocs` (enganchada a `build`): **MUST NOT** crearse a mano.

### Paso 14 — `documentospdf/`

1. **Mueve** `…/v1/documentospdf/modelo.pdf` a `…/v1/documentospdf/originales/modelo.pdf` (`git mv`), para que el escaneo del enum `TipoDocumentoPdf` no lo tome por un documento del trámite.
2. Los dos XML están materializados en `design/documentospdf/`. **Cópialos literalmente** a `…/v1/documentospdf/` (`solicitud.xml`, `resolucion.xml`). **MUST NOT** modificarlos, reescribirlos ni regenerarlos. **MUST NOT** crearse ningún fragmento `_*.xml`: los dos documentos no comparten ningún trozo (§5).

**Verificación:** en `documentospdf/` solo hay dos `.xml` (`solicitud.xml` y `resolucion.xml`, ningún `_*.xml`) y la subcarpeta `originales/`; el build genera `solicitud.pdf` y `resolucion.pdf`; el `<extra-code-model>` que reescribe el build sigue teniendo exactamente las dos constantes `SOLICITUD` y `RESOLUCION`.

### Paso 15 — Datos de demo: permisos y centro del administrador

1. **`permisos-demo.xml`** — es una **fusión, no una copia**: añade a `src/main/resources/data-demo/input/permisos-demo.xml` los elementos de `design/permisos.xml`, **dentro del bloque que corresponda a cada uno**, **conservando todo lo preexistente**. Los perfiles `CREADOR` y `RESPONSABLE` ya están declarados allí: **MUST NOT** duplicarlos; se añaden solo `SECRETARIO` y `DIRECTOR`.

2. **`usuarios-demo.xml`** — añade al bloque `<centroUsuarios>`, junto a los de CIPFP Mislata, la línea:

```xml
<centroUsuario usuarioCode="admin" centroCode="46019660"/>
```

   El usuario `admin` lo crea el bootstrap de Axelor, no la demo, y **hoy no tiene ninguna fila `CentroUsuario`**, así que su `centroActivo` es `null`. `ExpedienteController.getEventContext` llama a `getCentroFromCurrentUser()`, que lanza `RuntimeException("El centro activo es null para el usuario: …")` cuando lo es: sin esta línea el administrador **no puede abrir ningún expediente** y ESC-025 (T-038) revienta antes de pintar la vista. El binding de `data-demo/input-config.xml` busca el `User` por `self.code = :usuarioCode` con `create="false"`, así que **reutiliza** el admin existente y solo le fija el `centroActivo`. El centro elegido no cambia lo que ESC-025 le pide ver —consulta por «Expedientes Esperando», «Expedientes Cerrados» y la búsqueda, que le exceptúan del filtro de centro—, pero **sí** acota lo que ve en «Expedientes Pendientes», que a propósito **no** le exceptúa (Paso 16.3 (a) y §14 notas 8(d) y 13).

**Verificación:** `permisos-demo.xml` conserva las asignaciones de `JustificacionFaltaProfesorado`, `<perfiles>` tiene cuatro `<perfil>` y hay tres asignaciones nuevas por tipo de usuario y tres por cargo; tras arrancar, el usuario `admin` tiene centro activo (`select centro_activo from auth_user where code='admin'` no es nulo) y abre un expediente sin excepción. Comprobación adicional, **sin cambio de ficheros** (el municipio de los centros ya lo siembra el catálogo `common`, §14 nota 16): `select c.name, m.name from centro c left join municipio m on m.id = c.municipio` devuelve «Mislata» y «Alcoy/Alcoi», de modo que un expediente nuevo nace con «Mislata» en «Localidad del centro». **MUST NOT** resetearse la base de datos para este paso.

### Paso 16 — Bandejas de `SECRETARIO` y `DIRECTOR`, filtro de centro y menús

Este paso toca **seis** ficheros de vista del árbol más el `menus.xml`, y crea además la pieza de servidor a la que las dos bandejas nuevas preguntan (16.0). Los **dos ficheros de vista nuevos** van escritos enteros en 16.1 y 16.2: se crean **con ese contenido exacto**, no se interpretan. En los **cuatro existentes** y en el `menus.xml` se inserta **solo** el fragmento que se indica abajo, sobre la expresión real que hoy tiene cada uno, conservando todo lo demás (16.3 y 16.4). El apartado 16.3-bis declara la decisión de arquitectura que esto abre: dónde viven las vistas y los menús de un trámite concreto.

#### 16.0 — La condición de perfil de las dos bandejas nuevas

**El problema que resuelve.** Las dos bandejas nuevas fijan literalmente `_profile=SECRETARIO` y `_profile=DIRECTOR` (§12.2), y su `menuitem` las enseña a **todo** usuario: `VAR-10.1` fija el atributo `groups` a exactamente `admins`, `admins,users` o `users`, así que **no existe** la variante «restringir el menú a un grupo propio» — un `groups="secretario"` rompería los tests de `com.educaflow.views` (Categoría 10). Y `ExpedienteController.getEventContext` solo comprueba, con `checkProfileDelTipoExpediente`, que el perfil lo **use algún estado del tipo**; nunca que el usuario lo **ostente**. Sin más filtro, cualquiera con lectura sobre el expediente —el propio alumno por `Expediente.creador`, el secretario o el vicesecretario por `Expediente.porTipoExpediente`, el supervisor por `Expediente.porTramite`— que entrase por «Anulaciones de matrícula de mi centro» abriría `PENDIENTE_REVISION` en la pantalla **editable** del `SECRETARIO` (el sentido de la revisión, el motivo del rechazo y el texto de la subsanación), y quien entrase por «Anulaciones de matrícula pendientes de mi firma» abriría `PENDIENTE_FIRMA_DIRECTOR` en la del `DIRECTOR` (la decisión de secretaría y la resolución sin firmar). Eso contradice literalmente RUI-PENDIENTE_REVISION-GENERICA-001, RUI-PENDIENTE_FIRMA_DIRECTOR-GENERICA-001, ESC-008 paso 6, ESC-009 paso 8, ESC-020 paso 4, ESC-024 paso 4 y ESC-029 paso 11: «la decisión de secretaría no se enseña a nadie hasta que el director firma».

**La condición: se le PREGUNTA al dueño, no se reescribe.** La pregunta «¿ostenta este usuario el perfil P sobre este expediente?» **ya tiene un dueño único en el servidor**: `PerfilesUsuarioService.getPerfilesSobreExpediente` (`subsystem/security`), implementado sobre `AceRepository.findNombresPerfilesByExpediente` con su constante `FILTRO_USUARIO`, y es lo que `Tramitador.checkPerfilDelEstado` usa para **autorizar** cada evento. Las dos bandejas nuevas **no** vuelven a escribir esa lógica: **le preguntan**. El `<domain>` de cada una queda reducido a una referencia al resultado:

```xml
        <domain>self.id IN (:expedientesConPerfil)</domain>
        <context name="expedientesConPerfil" expr="call:com.educaflow.tramites.util.bandeja.BandejaPorPerfilController:idsExpedientesConPerfil('&lt;PERFIL&gt;','AnulacionMatriculaCicloFormativo')"/>
```

**MUST NOT** aparecer en ninguno de los dos `<domain>` una sola línea de JPQL sobre `Ace`, ni sobre `CentroUsuarioTipoUsuario`, ni sobre `CentroUsuarioCargo`: esa era una **tercera escritura** del dueño, dentro de dos ficheros de vista, sostenida por una nota de «mantenlo sincronizado a mano» que ningún test destapaba. Con el punto de consulta, quien toque `AceRepository.findNombresPerfilesByExpediente` —sus vías de asignación, sus vías de pertenencia o su tratamiento del centro— cambia el comportamiento de las dos bandejas **automáticamente**, porque pasan por él.

**Dónde vive el punto de consulta.** En `tramites/util/bandeja/`, **no** en `subsystem/expedientes` (que este diseño **MUST NOT** ampliar) y **no** duplicado en la carpeta de versión. Cumple las seis condiciones de entrada de `tramites/util/CLAUDE.md`: lo necesita **cualquier** trámite que declare perfiles propios (1), necesita `subsystem/security` y `subsystem/expedientes` para responder (2), no es dominio de ninguno de los dos —la pregunta de seguridad la sigue respondiendo `subsystem/security`; esto es el **pegamento** que la proyecta a una bandeja, igual que `tramites/util/firma/` es el pegamento entre `subsystem/criptografia` y un `trigger*` (3)—, el motor de expedientes no la necesita (4), no se acopla a ninguna entidad concreta —trabaja sobre `Expediente` y recibe el perfil y el trámite **como parámetros** (5)— y abre un subpaquete por propósito, `bandeja/` (6).

**Especificación de `BandejaPorPerfilController`** — fichero `src/main/java/com/educaflow/tramites/util/bandeja/BandejaPorPerfilController.java`:

```java
package com.educaflow.tramites.util.bandeja;

public class BandejaPorPerfilController { … }
```

- Dependencia: `@Inject PerfilesUsuarioService perfilesUsuarioService;` (campo, `jakarta.inject.Inject`, igual que `PerfilesUsuarioServiceImpl` inyecta su repositorio). Su binding **ya existe** (`SecurityModule`: `bind(PerfilesUsuarioService.class).to(PerfilesUsuarioServiceImpl.class)`), así que **MUST NOT** tocarse ningún módulo Guice.
- Constante: `private static final List<Long> NINGUNO = List.of(-1L);` — la lista que se devuelve cuando no hay nada que listar. **MUST NOT** devolverse una lista vacía: `self.id IN (:lista)` con la lista vacía no es portable, y `-1` no es el `id` de ninguna fila.
- Un único método público, anotado `@CallMethod` (`com.axelor.meta.CallMethod`), igual que `FirmaServidorController`:

  ```java
  @CallMethod
  public List<Long> idsExpedientesConPerfil(String nombrePerfil, String tramiteCode)
  ```

  1. `User user = SecurityUtil.getUser()` — **siempre** el usuario autenticado, **nunca** un usuario que venga por parámetro. Si `user` es nulo o `user.getCentroActivo()` es nulo → `return NINGUNO` (**falla cerrado**).
  2. Candidatos: los expedientes **abiertos** de ese trámite en el **centro activo** del usuario — `JPA.all(Expediente.class).filter("self.abierto = true AND self.tipoExpediente.tramite.code = :tramiteCode AND self.centro = :centro")` con los dos `bind`. Es el único JPQL de la pieza, y **no** habla de perfiles: son las tres condiciones que la bandeja ya prometía en su título (abiertos, de este trámite, de mi centro).
  3. Se queda con aquellos para los que `perfilesUsuarioService.getPerfilesSobreExpediente(expediente, user)` **contiene** `nombrePerfil`. Esta es la consulta al dueño, y es la **única** línea que decide el perfil.
  4. Devuelve sus `id`; si no queda ninguno, `NINGUNO`.

- **Seguridad (`k-secure-coding`).** El método es invocable por cualquier usuario autenticado, y no pasa nada: solo responde **sobre el usuario autenticado**, resuelto con `SecurityUtil.getUser()` dentro del propio método. Quien lo llamara con otro perfil o con otro trámite obtendría los expedientes sobre los que **él mismo** ostenta ese perfil, que es justamente lo que ya puede ver. **MUST NOT** añadírsele un parámetro de usuario, de centro ni de estado.
- **Coste, declarado.** Pregunta al dueño **una vez por expediente candidato**, en vez de resolverlo todo en una sola consulta. El conjunto candidato está acotado por construcción (los expedientes **abiertos** de **un** trámite en **un** centro), así que es el tamaño de la propia bandeja. Si algún día ese coste molestara, el arreglo **MUST** hacerse en el **dueño** —un método masivo en `PerfilesUsuarioService`/`AceRepository`, donde vive el `FILTRO_USUARIO`—, **nunca** devolviendo el JPQL a los `<domain>` de las vistas.

Notas de lectura:

- **CRITICAL — el literal que viaja en la llamada es el nombre del PERFIL, no el de un cargo.** En este trámite coinciden en el texto: existe el **perfil** `SECRETARIO` (que ostenta el tipo de usuario `ADMINISTRATIVO`) y existe el **cargo** `SECRETARIO` del centro, que en cambio ostenta el perfil `RESPONSABLE` (§12.1). `getPerfilesSobreExpediente` devuelve **nombres de perfil**, así que el secretario del centro **no** entra por la bandeja de secretaría — y es lo correcto: la spec le da consulta, no revisión.
- **Falla cerrado**: sin centro activo (o sin `centroUsuarioActivo`, que es lo que el dueño necesita para resolver las vías de pertenencia) no se lista nada. Es la dirección segura y coincide con lo que el usuario podría hacer: sin centro activo `ExpedienteController.getCentroFromCurrentUser` ni siquiera le deja abrir un expediente (§14 nota 13).
- **Estas dos bandejas NO llevan la excepción del administrador** (`esAdministrador`) que sí llevan **tres** de las cuatro genéricas del Paso 16.3 —todas menos «Expedientes Pendientes», que tampoco la lleva (16.3 (a))—, y es deliberado: el administrador **no tiene filas `Ace`**, así que no ostenta `SECRETARIO` ni `DIRECTOR` sobre ningún expediente y las dos bandejas le salen **vacías**. Es exactamente lo que la spec quiere — en ella el administrador **solo consulta** (ESC-025, T-038) y consulta por las bandejas genéricas, que sí le exceptúan del centro y le abren el expediente en la vista de solo lectura. Exceptuarle aquí le pintaría la pantalla **editable** del secretario o la del director, y hasta se la pintaría con los botones puestos: una pantalla que ningún escenario le concede. **Disparar** desde ella no podría —para eso están las cuatro guardas de perfil de §9.2 y §9.3, que existen precisamente porque `checkPerfilDelEstado` le exime (`SecurityUtil.isAdmin`)—, pero cada botón le fallaría con un mensaje de negocio, que es peor interfaz que no listarle el expediente. Por eso tampoco hace falta exceptuarle del centro: el filtro de centro lo aplica el punto de consulta para todos, y para el único usuario al que se exceptuaría la pregunta del perfil ya decide que no.
- **Es interfaz, no defensa** —igual que el filtro de centro (§14 nota 9)—: lo que de verdad impide **actuar** sin el perfil es `checkPerfilDelEstado`, que ya lo comprueba en el servidor en cada evento **para todo el mundo menos para el administrador**, al que exime (`Tramitador.java` línea 232); a él le para la guarda de perfil `ControlDeAcceso.exigeOstentarElPerfilDelEstado` que llevan los cuatro `trigger*` de `REVISION` y `RESOLUCION`, que exige **el perfil que el `<state>` declara** —se lo pregunta a `States`, no lo repite— (§9.0.1, §11 «Las ocho guardas de identidad», y `decisiones.md` D13). Lo que esta condición evita es la **exhibición**: que la pantalla de otro perfil llegue a pintarse. La solución de fondo sigue siendo que la bandeja pase el perfil real del usuario, que es del motor y está declarada en §14 nota 8 como **decisión de arquitectura pendiente de aprobar**; el día en que se apruebe, **desaparecen** la condición, el `<context>` y esta pieza de `tramites/util/bandeja/`.
- **REQUIRED — comprobación en runtime de las dos mecánicas que esta pieza estrena.** Ningún `action-view` del proyecto usa hoy un `<context expr="call:…">` (el precedente de `call:` que sí existe es un `<action-record>`: `subsysFirmas.Pendiente@TareaFirma-set-situacionFirma-action`), ni ningún `<domain>` recibe una **lista** por parámetro. Las dos las soporta la plataforma —`ActionView.evaluate` pasa cada `<context>` por `ActionHandler.evaluate`, que trata el prefijo `call:`, y un `IN (:lista)` se enlaza como cualquier otro parámetro—, pero **MUST** comprobarse abriendo las dos bandejas: si el `call:` no se resolviera o la lista no se enlazara, la bandeja saldría vacía o lanzaría al resolver el parámetro, y **MUST NOT** darse el paso por bueno sin haberlo visto listar. El resultado se calcula **al abrir la bandeja**: un expediente creado después no aparece hasta que se vuelve a abrir, igual que el `centroActivoId` de las genéricas.

#### 16.1 — `tramites/views/Expediente-revision.xml` (nuevo)

Fichero nuevo `src/main/java/com/educaflow/tramites/views/Expediente-revision.xml`, con este contenido **exacto**:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<object-views xmlns="http://axelor.com/xml/ns/object-views"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://axelor.com/xml/ns/object-views https://axelor.com/xml/ns/object-views/object-views_8.1.xsd">

    <action-view name="subsysExpedientes.Expediente@Revision-action" title="Anulaciones de matrícula de mi centro" model="com.educaflow.subsystem.expedientes.db.Expediente">
        <view type="grid" name="subsysExpedientes.Expediente@Revision-grid"/>
        <domain>self.id IN (:expedientesConPerfil)</domain>
        <context name="_profile" expr="SECRETARIO"/>
        <context name="expedientesConPerfil" expr="call:com.educaflow.tramites.util.bandeja.BandejaPorPerfilController:idsExpedientesConPerfil('SECRETARIO','AnulacionMatriculaCicloFormativo')"/>
    </action-view>

    <grid name="subsysExpedientes.Expediente@Revision-grid" title="Expedientes" model="com.educaflow.subsystem.expedientes.db.Expediente"
          groups="admins,users"
          editable="false" edit-icon="false" x-selector="none" canNew="false" canEdit="false" canDelete="false" canSave="false"
          action="subsysExpedientes-event-view-action"
    >
        <field name="tipoExpediente.tramite.name" />
        <field name="numeroExpediente" width="110px"/>
        <field name="namePhase" width="150px"/>
        <field name="nameState" width="200px"/>
        <field name="fechaUltimoEstado" width="150px" />
    </grid>

</object-views>
```

**Qué decide cada parte, para que no se «mejore» ninguna por su cuenta:** el `_profile=SECRETARIO` es lo que hace que `PENDIENTE_REVISION` se abra en la pantalla de la administrativa; el `<context name="expedientesConPerfil">` es la consulta al dueño del Paso 16.0, y con ella viajan las tres condiciones que esta bandeja promete —abiertos, de **este** trámite y del **centro activo**—, que por eso **ya no** se repiten en el `<domain>`. **MUST NOT** volver a escribirse en el `<domain>` `self.abierto`, el `tramite.code` ni `self.centro`: se duplicaría el literal del trámite dentro del mismo fichero y volvería a haber dos sitios diciendo lo mismo.

#### 16.2 — `tramites/views/Expediente-firma.xml` (nuevo)

Fichero nuevo `src/main/java/com/educaflow/tramites/views/Expediente-firma.xml`, con este contenido **exacto**:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<object-views xmlns="http://axelor.com/xml/ns/object-views"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://axelor.com/xml/ns/object-views https://axelor.com/xml/ns/object-views/object-views_8.1.xsd">

    <action-view name="subsysExpedientes.Expediente@Firma-action" title="Anulaciones de matrícula pendientes de mi firma" model="com.educaflow.subsystem.expedientes.db.Expediente">
        <view type="grid" name="subsysExpedientes.Expediente@Firma-grid"/>
        <domain>self.id IN (:expedientesConPerfil) AND self.codeState = 'PENDIENTE_FIRMA_DIRECTOR'</domain>
        <context name="_profile" expr="DIRECTOR"/>
        <context name="expedientesConPerfil" expr="call:com.educaflow.tramites.util.bandeja.BandejaPorPerfilController:idsExpedientesConPerfil('DIRECTOR','AnulacionMatriculaCicloFormativo')"/>
    </action-view>

    <grid name="subsysExpedientes.Expediente@Firma-grid" title="Expedientes" model="com.educaflow.subsystem.expedientes.db.Expediente"
          groups="admins,users"
          editable="false" edit-icon="false" x-selector="none" canNew="false" canEdit="false" canDelete="false" canSave="false"
          action="subsysExpedientes-event-view-action"
    >
        <field name="tipoExpediente.tramite.name" />
        <field name="numeroExpediente" width="110px"/>
        <field name="namePhase" width="150px"/>
        <field name="nameState" width="200px"/>
        <field name="fechaUltimoEstado" width="150px" />
    </grid>

</object-views>
```

**CRITICAL — la bandeja del director lleva además `AND self.codeState = 'PENDIENTE_FIRMA_DIRECTOR'`, y esa sí va en el `<domain>`.** Es la única condición propia de **esta** bandeja y no de la pregunta del perfil, así que es suya: su título promete «pendientes de mi firma», y sin ella listaría **todos** los expedientes abiertos del trámite, también los que están en `DATOS_SOLICITUD`, `PENDIENTE_FIRMA` y `PENDIENTE_REVISION`, que no están pendientes de nada suyo. Es además el único estado en el que existe un form para `profile="DIRECTOR"`: en cualquier otro, entrar por esta bandeja llevaría a la vista genérica de solo lectura. La bandeja de la administrativa **no** lleva condición de estado, y su título tampoco la promete: tiene que ver los expedientes de su centro en cualquier estado (ESC-045, T-039).

**CRITICAL — la acotación al trámite sigue siendo obligatoria en las dos, y ahora viaja en el argumento de la llamada.** Sin ella la bandeja listaría expedientes de **cualquier** tipo, y abrir desde ella uno cuyo tipo no declare `SECRETARIO`/`DIRECTOR` lanza «El perfil … no lo usa ningún estado del tipo de expediente …» (`ExpedienteController.checkProfileDelTipoExpediente`). Ver §14 nota 8(c).

**CRITICAL — MUST NOT** replicarse en estos `<domain>` la condición del permiso `Ace` de `auth-expedientes.xml` (la de «quién puede **leer** el expediente»). Sería la misma decisión de autorización con **tres dueños** (el permiso, el dominio de la bandeja y la guarda del trigger). El `<domain>` acota **qué se lista** y con qué perfil se abre; quién puede **leer** lo decide el permiso y quién puede **actuar**, `checkPerfilDelEstado` del motor más la guarda de centro del trigger (§9.0.1).

#### 16.3 — Filtro de centro en las cuatro bandejas existentes

Estas cuatro llevan **solo** el filtro de centro: no fijan ningún perfil propio de este trámite (pasan `CREADOR` o `RESPONSABLE`, que son los perfiles genéricos de la plataforma), así que la condición de perfil de §16.0 **MUST NOT** añadírseles.

**El `<context name="centroActivoId">` va en las cuatro; el `esAdministrador`, solo en tres.** El segundo se llama `esAdministrador` —el **hecho**, no una de sus consecuencias— para que se lea igual que la exención equivalente del servidor (`SecurityUtil.isAdmin` en `checkPerfilDelEstado`) y para no tener dos nombres del mismo hecho. Los dos `<context>` van dentro del `<action-view>`, junto al `_profile` que ya tiene:

```xml
        <context name="centroActivoId" expr="eval: __user__?.centroActivo?.id"/>
        <context name="esAdministrador" expr="eval: __user__?.group?.code == 'admins'"/>
```

**CRITICAL — «Expedientes Pendientes» (a) lleva SOLO el primero de esos dos `<context>` y su `<domain>` NO exceptúa al administrador.** Es la única de las cuatro que fija `_profile=CREADOR` (`Expediente-pendiente.xml` línea 10); las otras tres fijan `_profile=RESPONSABLE`, un perfil que este tipo declara en `ACEPTADA` y `RECHAZADA` pero para el que **ninguna fase escribe un `<form … profile="RESPONSABLE">`**, así que por ellas el expediente se abre siempre en la vista genérica de solo lectura. `CREADOR`, en cambio, **sí** tiene form en `DATOS_SOLICITUD` y en `PENDIENTE_FIRMA`, y `PhaseEventManager.getViewName` (`subsystem/expedientes/services/eventmanager/PhaseEventManager.java`) devuelve el form del perfil recibido en cuanto existe: exceptuar ahí al administrador le abriría la pantalla **editable** del creador en **cualquier** centro. **Disparar** desde ella ya no puede: los **cuatro** eventos de esos dos estados —`CONTINUAR`, `DELETE`, `VOLVER` y `PRESENTAR`— llevan la guarda `ControlDeAcceso.exigeSerElCreador` (§9.1 y §11, «Las cuatro guardas de autoría»), que es lo único que le para porque `checkPerfilDelEstado` le exime. Sin la excepción, además, la **exhibición** de esa pantalla queda acotada a los expedientes de su centro activo — **y no desaparece**: dentro de ese centro la pantalla editable se le sigue pintando, igual que a todo el que pueda leer el expediente (§12.2 y §14 nota 8(d)). Ningún escenario lo pide por esta bandeja: el administrador consulta por «Expedientes Esperando», «Expedientes Cerrados» y la búsqueda (ESC-025, T-038), que sí le exceptúan.

**(a) `Expediente-pendiente.xml`** — en `subsysExpedientes.Expediente@Pendiente-action`, el `<domain>` pasa de `self.abierto=true ` a:

```xml
        <domain>self.abierto=true AND self.centro.id = :centroActivoId</domain>
```

   **Sin excepción de administrador, y por tanto con un solo `<context>` nuevo** (`centroActivoId`): en esta bandeja **MUST NOT** añadirse el `<context name="esAdministrador">`, porque no se usa. El motivo está arriba, en el `CRITICAL` de este apartado.

**(b) `Cerrado-Expediente.xml`** — en `subsysExpedientes.Expediente@Cerrados-action`, el `<domain>` pasa de `self.abierto=false ` a:

```xml
        <domain>self.abierto=false AND (self.centro.id = :centroActivoId OR :esAdministrador = true)</domain>
```

**(c) `Abierto-Expediente.xml`** — su `action-view` `subsysExpedientes.Expediente@Esperando-action` **no tiene `<domain>`**: filtra en los dos `<node>` del `<tree>`, y ahí es donde hay que tocar (además de los dos `<context>` en el `action-view`):

```xml
        <node model="com.educaflow.subsystem.expedientes.db.Tramite" domain="EXISTS (SELECT 1 FROM Expediente e WHERE e.abierto=true AND (e.centro.id = :centroActivoId OR :esAdministrador = true) AND e.tipoExpediente.tramite=self)" >
```

```xml
        <node model="com.educaflow.subsystem.expedientes.db.Expediente" domain="abierto=true AND (centro.id = :centroActivoId OR :esAdministrador = true)" parent="tipoExpediente.tramite" draggable="false" onClick="subsysExpedientes-event-view-action">
```

   **CRITICAL — el segundo `<node>` NO lleva el prefijo `self.`.** Su `domain` real es hoy `domain="abierto=true"`, sin alias, y este paso **solo añade** la condición de centro conservando esa sintaxis; el `<node>` hermano de `Tramite` sí usa alias (`e.abierto`) porque el suyo es una subconsulta con su propio `FROM`. Cambiarlo a `self.` sería reescribir calladamente una expresión que hoy funciona, con una sintaxis que ese nodo no usa. **MUST NOT** «uniformarse» con los `<domain>` de los `action-view`.

**(d) `Expediente-search.xml`** — la búsqueda de expedientes es **una lista más** por la que un usuario de otro centro ve el expediente ajeno, y ESC-021 exige que no aparezca «ni en esa lista ni en ninguna otra lista de expedientes que la administrativa de CIPFP Batoi pueda abrir». Los dos `<context>` van en `subsysExpedientes.Expediente@PruebaSearch-buscar-action` (que ya fija `_profile=RESPONSABLE`), y sus dos `<node>` **conservan** su condición actual sobre `abierto` y `fechaUltimoEstado`:

```xml
        <node model="com.educaflow.subsystem.expedientes.db.Tramite" domain="EXISTS (SELECT 1 FROM Expediente e WHERE e.abierto=:estado and ((:anyo is null) OR (:anyo=0) OR (YEAR(e.fechaUltimoEstado)=:anyo)) AND (e.centro.id = :centroActivoId OR :esAdministrador = true) AND e.tipoExpediente.tramite=self)" >
```

```xml
        <node model="com.educaflow.subsystem.expedientes.db.Expediente" domain="(self.abierto=:estado) and ((:anyo is null) OR (:anyo=0) OR (YEAR(self.fechaUltimoEstado)=:anyo)) and (self.centro.id = :centroActivoId OR :esAdministrador = true)" parent="tipoExpediente.tramite" draggable="false" onClick="subsysExpedientes-event-view-action">
```

**MUST NOT** acotarse por trámite ninguna de estas cuatro: son bandejas **genéricas** de la plataforma y sirven a todos los tipos de expediente. Solo se les añade el centro.

#### 16.3-bis — DECISIÓN DE ARQUITECTURA PENDIENTE DE APROBAR: dónde viven las vistas y los menús de un trámite concreto

**Qué se está haciendo, dicho sin rodeos.** Este paso crea **dos ficheros de vista específicos de este trámite** (`<domain>` con `tramite.code = 'AnulacionMatriculaCicloFormativo'`, `codeState = 'PENDIENTE_FIRMA_DIRECTOR'`, títulos «Anulaciones de matrícula…») dentro de `src/main/java/com/educaflow/tramites/views/`, que `tramites/util/CLAUDE.md` describe como **recursos XML compartidos** y donde hoy los cinco ficheros existentes son **genéricos**; y añade al `menus.xml` **global** dos `menuitem` de este trámite, visibles para todo usuario de todo centro. Es exactamente la señal que `subsystem/expedientes/CLAUDE.md` marca como «esta pieza no es de aquí» —«nombra un trámite, una fase, un estado, un evento, un perfil o un campo concretos» → «la necesita un solo tipo de expediente → su carpeta de versión»—, y roza la misma regla que este diseño enuncia en §16.3 y en §14 nota 8(d) para prohibir parchear desde un trámite una bandeja compartida.

**Por qué se hace igualmente.** No hay hoy ningún otro sitio donde puedan vivir: las vistas de la carpeta de versión están en el **formato preprocesado** de un tipo de expediente (`<form state= profile=>`, `<include-panels>`, `<footer>`), que no admite un `action-view` ni un `grid` de Axelor; y un `menuitem` **MUST** estar en un `menus.xml` que el build cargue, y el único que existe es el global. Es decir: el patrón no es una preferencia del diseño, es lo único disponible.

**Qué cuesta.** Cada trámite que declare perfiles propios añadirá **dos ficheros y dos menús** al común, y cada uno de esos menús lo verá todo usuario de todos los centros (con la lista vacía si no ostenta el perfil, §16.0 y 16.4). Con N trámites así, `tramites/views/` deja de ser «los recursos compartidos» y pasa a ser un cajón mezclado, y el menú «Expedientes» crece con una entrada por trámite y perfil.

**Cuál sería su sitio definitivo.** El mismo que cierra la nota 8: cuando la bandeja pase el **perfil real del usuario** en vez de fijarlo, deja de hacer falta una bandeja por perfil y por trámite, y estas dos vistas y estos dos menús **desaparecen** sin sustituto. Si esa solución tardara, el sitio propio sería una carpeta de vistas **por trámite** —`tramites/<tramite>/views/`, cargada por el build igual que `tramites/views/`— y un mecanismo de menús por trámite; las dos cosas son capacidades del **motor** y del build, no de este trámite, así que **MUST NOT** darse por hechas aquí.

**Este diseño no aprueba el patrón: lo declara.** Queda escrito también en §14 nota 26 y en `decisiones.md` **D11**, con el mismo formato de «decisión de arquitectura pendiente de aprobar» que se usa para el `_profile`.

#### 16.4 — Los dos `menuitem`

En `secretariavirtual/menus/menus.xml`, a continuación de `expedientes-pruebaBusqueda-menuitem` y con la **misma sangría de cuatro espacios** que sus hermanos (`order` 1…6 ya usados, así que estos van con 7 y 8):

```xml
    <menuitem name="expedientes-anulacionesMiCentro-menuitem" parent="expedientes-menuitem" title="Anulaciones de matrícula de mi centro" action="subsysExpedientes.Expediente@Revision-action" groups="admins,users" order="7"/>
    <menuitem name="expedientes-anulacionesMiFirma-menuitem" parent="expedientes-menuitem" title="Anulaciones de matrícula pendientes de mi firma" action="subsysExpedientes.Expediente@Firma-action" groups="admins,users" order="8"/>
```

`menus.xml` **sí** está sujeto a `agent_docs/view-rules.md` (Categoría 10): de ahí el sufijo `-menuitem`, el prefijo del padre en el `name`, el `groups` canónico `admins,users`, el `order` entero único entre hermanos, el orden fijo de atributos y una sola línea por menú.

**CRITICAL — los dos menús los ve TODO usuario, y es correcto que así sea.** `VAR-10.1` admite en `groups` exactamente `admins`, `admins,users` o `users`, así que **no se puede** restringir un menú a «los que ostentan el perfil `SECRETARIO`»: un `groups` con un grupo propio rompería los tests de `com.educaflow.views`. Quien no ostenta el perfil ve la entrada de menú y abre **una lista vacía**, porque la condición de perfil de §16.0 no le lista ningún expediente. Es exactamente lo que hace falta: lo que había que impedir no es ver el menú, sino **abrir un expediente con un perfil que no se tiene**. **MUST NOT** cambiarse el `groups` de estos dos `menuitem` para intentar ocultarlos.

**REQUIRED — comprobación en runtime de que los `<context>` llegan al `domain` de un `<node>` de `<tree>`.** Ningún `<node>` del proyecto usa hoy un parámetro procedente de un `<context>` del `action-view`: los dos únicos parámetros que aparecen en un `<node domain=…>` (`:estado` y `:anyo`, en `Expediente-search.xml`) vienen del `searchModel` del propio `<tree>`, no del `action-view`. Este paso lo da por hecho en **dos** ficheros (`Abierto-Expediente.xml` 16.3 (c) y `Expediente-search.xml` 16.3 (d)), así que **MUST** comprobarse abriendo «Expedientes Esperando» y la búsqueda: si `:centroActivoId` o `:esAdministrador` no llegaran al nodo, el árbol saldría vacío o lanzaría al resolver el parámetro, y entonces el filtro de centro de esas dos listas **MUST** rehacerse por otra vía (por ejemplo, resolviendo el valor en la propia expresión en vez de por parámetro) **antes** de dar el paso por bueno. Es una de las **dos** partes del Paso 16 sin precedente en el árbol; la otra es el `<context expr="call:…">` con lista de ids de las dos bandejas nuevas (16.0), que se comprueba igual.

**Verificación:** compila y arranca; los tests de `com.educaflow.views` siguen en verde (los `tramites/views/*.xml` están **exentos** de esas reglas —`ViewFiles.PAQUETES_EXENTOS` incluye `tramites`—, pero `menus.xml` **no** lo está, y las reglas de la Categoría 10 se le aplican enteras); la administrativa ve su bandeja y el director la suya, y en las dos solo aparecen expedientes de este trámite —y en la del director, **solo** los que están en «Pendiente de la firma del director»: un expediente en «Pendiente de revisión» no aparece en ella—; un usuario de otro centro no ve los expedientes ajenos en **ninguna** de las seis listas (las dos nuevas, «Expedientes Pendientes», «Expedientes Esperando», «Expedientes Cerrados» y la búsqueda); el administrador ve los de los **dos** centros en las **tres genéricas de consulta** («Expedientes Esperando», «Expedientes Cerrados» y la búsqueda), en «Expedientes Pendientes» ve **solo los de su centro activo** (16.3 (a): esa bandeja no le exceptúa) y las dos nuevas le salen vacías a propósito (Paso 16.0), porque en esta spec solo consulta y lo hace en solo lectura (ESC-025, T-038).

**REQUIRED — la condición de perfil de §16.0 se comprueba en runtime**, porque ningún test de build la ve: con un expediente en «Pendiente de revisión», el **alumno que lo creó**, el **secretario del centro** (cargo `SECRETARIO`, perfil `RESPONSABLE`) y el **supervisor** abren las dos bandejas nuevas y las ven **vacías**; entrando por «Expedientes Esperando» siguen viendo el expediente, en la pantalla genérica de solo lectura y sin el sentido de la revisión ni el motivo del rechazo. La administrativa y el director, en cambio, **sí** lo ven en su bandeja y lo abren en su pantalla. Es T-056.

### Paso 17 — Verificación final

```
./run.sh
```

- `BUILD SUCCESSFUL`, con los tests de `com/educaflow/tiposexpedientes` y `com/educaflow/views` en verde (los ejecuta ese mismo build).
- Se regeneró `…/v1/estados.png` (`GenerateDocs` va enganchada a `build` con `finalizedBy`).
- **REQUIRED — comprobación en runtime.** Los tests cubren la forma, no el comportamiento. Hay que recorrer **todos** los estados con usuarios de los perfiles adecuados y comprobar en particular lo que nada verifica en build:
  - Que al presentar no salta un NPE: `personaSolicitante` y `personaInteresada` los rellena el `triggerInitialEvent` (§8) y `createRegistroEntrada` revienta si son nulos.
  - Que las diez transiciones de §3 llevan donde dice el `.puml`.
  - Que los dos PDF generados **no tienen huecos en blanco**: las expresiones Groovy de `documentospdf/` fallan en silencio (log + campo vacío). Revisar en especial `self.ciclo.gradoNivel`, las dos líneas de lugar y fecha y la fórmula de estimación/desestimación de la resolución.
  - Que la firma cae dentro del recuadro «Firma:» de la solicitud (celda `y 415,52 → 472,22`) y dentro del recuadro vacío bajo «El director / la directora del centro» en la resolución (celda `y 315,28 → 372,00`); si no, ajustar las constantes `Rectangulo` **en los dos sitios** (el trigger y la `<action-method>`, para la solicitud). Ver §5 y §14 nota 12.
  - Que **ningún campo inline se sale del marco** del documento (borde derecho en `x = 566,93`): son los cuatro de ancho completo de `resolucion.xml`, que por eso llevan `;11.8` y no `;12` (§5).
  - Que las **tres fechas en valenciano** salen bien escritas —«1 de gener de 2026», «1 d’abril de 2026», «1 d’agost de 2026», «1 d’octubre de 2026»— y **no** «1 de de gener de 2026»: el patrón del valenciano es `"d MMMM 'de' yyyy"`, sin el literal `'de'` (§5).
  - Que el `<extra-code-model>` del `…/v1/domains.xml` **sigue siendo el que genera el build**: `grep -c esRechazo …/v1/domains.xml` devuelve `0` y el predicado vive en `ReglasAnulacionMatricula.kt` (§9.0.3).
  - Que el campo punteado `ciclo.gradoNivel` se refresca en la pantalla del alumno **al cambiar el ciclo**, sin guardar.
  - Que el usuario `admin` **puede abrir** un expediente (de los dos centros, entrando por «Expedientes Esperando»): si el Paso 15 no le dio centro activo, `getCentroFromCurrentUser` lanza y la pantalla no llega a pintarse (§14 nota 13).
  - Que el `admin` ve los expedientes de los **dos** centros en «Expedientes Esperando», «Expedientes Cerrados» y la búsqueda, y **solo los de su centro activo** en «Expedientes Pendientes» (16.3 (a) no le exceptúa). Esa asimetría es **deliberada**: **MUST NOT** «arreglarse» añadiendo `OR :esAdministrador = true` a esa bandeja.
  - Que se confirma el agujero **conocido y no mitigado** de §14 nota 8(d), para no confundirlo con una regresión: entrando por «Expedientes Pendientes» (que fija `_profile=CREADOR`) a un expediente **de su centro** en `DATOS_SOLICITUD` o en `PENDIENTE_FIRMA`, al `admin` —y a cualquiera que pueda leerlo— se le **pinta** el formulario editable del creador. Lo que **MUST** comprobarse a continuación es que no puede **actuar**: los **cuatro** botones de esos dos estados —«Siguiente», «Borrar el expediente», «Atrás» y «Firmar y presentar la solicitud»— le tienen que fallar con el mensaje de negocio de la guarda `exigeSerElCreador` («Solo puede modificar sus propias solicitudes», «Solo puede borrar sus propias solicitudes», «Solo puede volver atrás en sus propias solicitudes» y «Solo puede presentar sus propias solicitudes»), y el expediente **MUST** quedar exactamente en el estado en que estaba. Que la pantalla se pinte **MUST NOT** taparse desde este trámite: la solución es del motor.
  - Que el `admin` **tampoco puede actuar en `REVISION` ni en `RESOLUCION`**, que es lo que cierran las cuatro guardas de perfil (§9.2, §9.3 y §11). No hay bandeja que le lleve ahí —las dos nuevas le salen vacías (16.0)—, así que la comprobación se hace **pidiendo la vista con el `_profile` a mano**, que es justo el camino que sigue abierto (§14 nota 8(d)): se abre un expediente de **su centro activo** en `PENDIENTE_REVISION` con `_profile=SECRETARIO` y otro en `PENDIENTE_FIRMA_DIRECTOR` con `_profile=DIRECTOR`, y los cuatro botones —«Enviar a la firma del director», «Pedir subsanación al alumno», «Firmar la resolución» y «Devolver a la secretaría»— **MUST** fallar con el mensaje de negocio de la guarda («Solo la secretaría del centro puede revisar esta solicitud», «Solo el director del centro puede firmar la resolución» y «Solo el director del centro puede devolver la resolución a la secretaría»), quedando el expediente exactamente en el estado en que estaba y **sin** resolución firmada ni registro de salida. Sin estas guardas el administrador **pasa** la de centro y firma: es el caso que declara §14 nota 25 y `decisiones.md` D13, y **ningún test de build lo ve**.
  - Que las dos bandejas nuevas **solo** listan expedientes de este trámite, y que ninguna de las seis listas (las dos nuevas, «Expedientes Pendientes», «Expedientes Esperando», «Expedientes Cerrados» y la búsqueda) muestra expedientes de otro centro a un usuario **que no sea el administrador** (a él, y solo a él, se los muestran las tres de consulta; ver el punto siguiente).
  - Que las dos bandejas nuevas **listan algo**: es la comprobación de que el `<context expr="call:…">` se resuelve y de que la lista de ids se enlaza en el `<domain>` (Paso 16.0, dos mecánicas sin precedente en el árbol). Si el `call:` fallara, la bandeja saldría vacía **para todos** —incluida la administrativa, que sí ostenta el perfil— o lanzaría al resolver el parámetro; **MUST NOT** confundirse ese vacío con el vacío legítimo del punto siguiente.
  - Que las dos bandejas nuevas **solo** las ve con contenido quien ostenta su perfil: al alumno que creó el expediente, al secretario del centro y al supervisor les salen **vacías**, aunque los tres puedan leer el expediente por las bandejas genéricas (condición de perfil del Paso 16.0; es T-056). Es lo único que les corta el **camino normal** hacia la pantalla editable del `SECRETARIO` o la del `DIRECTOR`, y **ningún test de build lo ve**. **MUST NOT** leerse como que esa pantalla ya no se les puede pintar: el `_profile` viaja en la petición y nada en el servidor comprueba que el usuario ostente el perfil para **elegir** la vista, así que pedirla con ese `_profile` fuera de la bandeja sigue abriéndola (residuo declarado en §14 nota 8(d) y en las dos filas `RUI-*-GENERICA-*` de §11).
- **REQUIRED — no-regresión de la suite E2E ya persistida del catálogo.** Con la aplicación arrancada:

  ```
  npx playwright test src/test/e2e/subsystem/sistemaeducativo
  ```

  Los **16** ficheros `.spec.ts` (`t-001`…`t-015` más `crear-ley-educativa.spec.ts`) **MUST** quedar en verde. Es la misma comprobación del Paso 4, repetida al final porque entre medias se tocan el data-init (`IABD`, `nombreCorto`) y las vistas del catálogo, y `./run.sh` **no** ejecuta los tests Playwright. Un fallo aquí es una regresión de esta iniciativa y se arregla en el código de la aplicación: **MUST NOT** editarse ningún fichero de `src/test/e2e/**` (§14 nota 22).

## 8. Especificación del InitialEventManagerImpl

Fichero: `…/v1/InitialEventManagerImpl.java`, **en la raíz de la carpeta de versión**.

```java
package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1;

public class InitialEventManagerImpl implements InitialEventManager<AnulacionMatriculaCicloFormativoV1> {
    @Override
    public void triggerInitialEvent(AnulacionMatriculaCicloFormativoV1 expediente, EventContext eventContext) throws BusinessException { … }
}
```

Asignaciones, **en este orden** (el orden es normativo):

| # | Setter | Fuente del valor | Por qué |
|---|---|---|---|
| 1 | *(construir)* `Persona persona` | `new Persona()` con `setNombre(expediente.getUsuarioRegistrador().getNombre())`, `setApellidos(…getApellidos())` y `setDni(…getDni())` | Los datos de identidad del alumno quedan congelados en el expediente aunque después cambien en su ficha (CC-001) |
| 2 | `setPersonaInteresada(persona)` | la `Persona` del paso 1 | El interesado es el propio alumno; `createRegistroEntrada` lo lee del expediente y **revienta con NPE si es nulo** |
| 3 | `setPersonaSolicitante(persona)` | la **misma** `Persona` del paso 1 | El solicitante es el propio alumno (se da por supuesto que es mayor de edad y firma él mismo) |
| 4 | `setCursoAcademico(...)` | `expediente.getCentro().getCurso()` formateado como `curso + "/" + (curso + 1)`; `null` si el centro no tiene curso | CC-003: el impreso lo pide y el alumno no debe poder elegir otro |
| 5 | `setNombreCentro(expediente.getCentro().getName())` | el nombre del centro del expediente | CC-004: se imprime en «Expone» y en el pie dirigido al director |
| 6 | `setLocalidadCentro(...)` | `expediente.getCentro().getMunicipio()` → su `getName()`; `null` si el centro no tiene municipio | CC-004: se imprime en «Expone» y es el **lugar** de la línea de lugar y fecha (CC-005). Los dos centros de demo ya tienen municipio, que les da el catálogo `Centro.xml` del subsistema `common` (§14 nota 16) |

Reglas explícitas:

- Lo que el `Tramitador` ya rellenó **antes** de llamar a este método y que **MUST NOT** reasignarse: `tipoExpediente`, `centro`, `usuarioRegistrador`, `name` y `numeroExpediente`.
- Lo que hace **después** y que tampoco es cosa de este método: fijar el estado inicial, crear el `HistorialEstado` y llamar al `onEnterState` del estado inicial.
- **CRITICAL** — este tipo **sí** crea registro de entrada (`PRESENTAR`), así que las asignaciones 2 y 3 son obligatorias: `createRegistroEntrada` lanza **NPE** si `personaSolicitante` o `personaInteresada` son nulos, y **nada lo verifica en build**.
- **MUST NOT** llamar a `eventContext.updateState(...)`: el estado inicial lo fija el `Tramitador`.
- **El NIA, la dirección, el teléfono, la población, la provincia y el código postal NO se precargan** (CC-002). En el modelo actual **no existe ninguna «ficha del alumno»**: `User` solo guarda nombre, apellidos y DNI, y las filas `Persona` son fotos que crea cada expediente, no datos maestros. El expediente nace con esos seis campos vacíos y el alumno los teclea. Ver §14.

Dependencias a inyectar: **ninguna**.

## 9. Especificación de los PhaseEventManagerImpl

### 9.0 Clases auxiliares compartidas de la versión

**Tres** piezas sin estado en la **raíz de la versión** (fuera de las carpetas de fase), a las que los `trigger*` de las tres fases —y, la tercera, también el validador y los documentos PDF— **delegan**: dos clases `final` Java con constructor privado (§9.0.1 y §9.0.2) y un `object` Kotlin (§9.0.3), este último en el mismo fichero `ReglasAnulacionMatricula.kt` que la regla del DSL de §10.0. **MUST NOT** convertirse ninguna en una superclase: el dispatcher usa `getDeclaredMethods()` (§9.1.4).

**CRITICAL — la raíz de la versión es el sitio, y el `<extra-code-model>` del `domains.xml` NO lo es.** Todo código propio del tipo que no quepa en un `trigger*`, en un `onEnter*` o en el validador vive en una de estas clases. **MUST NOT** escribirse dentro del `<extra-code-model>`: el build lo **reescribe entero** en cada compilación (`RichDomainXmlTask` → `DomainXmlFile.addExtraCodeToDomainXml`), de modo que solo sobreviven ahí el enum `TipoDocumentoPdf` y `getDocumentoPdf`, que son lo que genera su plantilla.

#### 9.0.1 `ControlDeAcceso`

Fichero `…/v1/ControlDeAcceso.java`. Concentra las comprobaciones de **quién es el usuario autenticado**: si es el creador, si actúa sobre un expediente de su propio centro y si **ostenta el perfil** que el estado declara. La usan los **ocho** `trigger*` de las tres fases como **guarda en su primera línea**; ninguna vive en el validador (ver §11 y `decisiones.md` D4 y D13).

```java
public final class ControlDeAcceso {
    public static void exigeSerElCreador(AnulacionMatriculaCicloFormativoV1 expediente, String mensaje) throws BusinessException;
    public static void exigeMismoCentroQueElExpediente(AnulacionMatriculaCicloFormativoV1 expediente, String mensaje) throws BusinessException;
    public static void exigeOstentarElPerfilDelEstado(AnulacionMatriculaCicloFormativoV1 expediente, String mensaje) throws BusinessException;
}
```

- `exigeSerElCreador` compara el `id` de `expediente.getUsuarioRegistrador()` con el de `SecurityUtil.getUser()`; si no coinciden (o alguno es nulo) lanza `new BusinessException(I18n.get(mensaje))`.
- `exigeMismoCentroQueElExpediente` compara el `id` de `expediente.getCentro()` con el de `SecurityUtil.getUser().getCentroActivo()`; si no coinciden (o alguno es nulo) lanza `new BusinessException(I18n.get(mensaje))`.
- `exigeOstentarElPerfilDelEstado` **no recibe ningún perfil: lo pregunta**. Son **dos** preguntas seguidas, cada una a su dueño, y la guarda no responde ninguna de las dos por su cuenta:
  1. **«¿Qué perfil atiende este estado?»** → su dueño es el atributo `profile` del `<state>` del `TipoExpedienteInstance.xml`, proyectado en la clase generada `States`. Se consulta con `States.INSTANCE.getState(expediente.getCodePhase(), expediente.getCodeState())` y, sobre el `State` que devuelve, `getProfile()` — **exactamente** la misma llamada que hace `Tramitador.checkPerfilDelEstado` (`State.getProfile()`, `subsystem/expedientes/services/eventmanager/State.java`). **MUST NOT** escribirse aquí ningún literal de perfil: si mañana `PENDIENTE_REVISION` pasa de `profile="SECRETARIO"` a otro valor, la guarda exige el nuevo sin que nadie toque el trigger, igual que le pasa al motor.
  2. **«¿Ostenta este usuario ese perfil sobre este expediente?»** → su dueño es `PerfilesUsuarioService.getPerfilesSobreExpediente(expediente, SecurityUtil.getUser())` (`subsystem/security`), **el mismo** al que pregunta `checkPerfilDelEstado` para autorizar cada evento y el punto de consulta de las dos bandejas nuevas (Paso 16.0): la comprobación es `getPerfilesSobreExpediente(…).contains(perfilDelEstado.name()) == false → lanza`. **MUST NOT** reescribirse aquí ninguna consulta sobre `Ace`, `CentroUsuarioTipoUsuario` ni `CentroUsuarioCargo`: sería una cuarta escritura del dueño.

  Si la respuesta a (2) es que no, lanza `new BusinessException(I18n.get(mensaje))`.
  - **Estado sin `profile`: no exige nada y devuelve el control**, que es literalmente lo que hace el motor («Un estado **sin perfil** no exige ninguno», `checkPerfilDelEstado`). Hoy los tres estados con eventos declaran perfil, así que esa rama no se ejecuta en ningún `trigger*` de este tipo; se escribe para que la guarda siga diciendo lo mismo que el motor el día en que un estado deje de declararlo, en vez de fallar cerrado sobre una exigencia que el XML ya no hace.
  - **La pareja `(codePhase, codeState)` que no resuelva a ningún estado es un error de programación, no un caso de negocio**: `States.INSTANCE.getState(…)` devuelve `Optional<State>` y, si viene vacío, la guarda lanza `IllegalStateException` (no `BusinessException`). No puede ocurrir por el camino normal — el `Tramitador` acaba de resolver ese mismo estado para autorizar el evento y llegar hasta el trigger—, y **MUST NOT** tratarse como «no hay perfil que exigir»: eso sería el retorno defensivo que abre la puerta justo cuando falta el dato con el que decidir.
  - **`States` no se importa**: la clase generada de esta versión vive en el **mismo paquete** que `ControlDeAcceso` (`…v1`). Lo que sí usa el fichero son `com.educaflow.subsystem.expedientes.services.eventmanager.State` y `com.educaflow.subsystem.expedientes.db.Profile`, los dos como **tipos internos** del método; ningún llamante los nombra.
  - El servicio se resuelve dentro del método con `Beans.get(PerfilesUsuarioService.class)` —su binding ya existe (`SecurityModule`), así que **MUST NOT** tocarse ningún módulo Guice—, igual que hace `CertificadoDigitalHelper` (`subsystem/criptografia/util`), que es el precedente del proyecto para un helper estático que necesita un bean. Así las tres guardas se llaman igual desde los ocho `trigger*` y ningún `PhaseEventManagerImpl` gana un `@Inject` solo para esto.
  - **NO exime al administrador, y ese es exactamente su motivo de existir**: `Tramitador.checkPerfilDelEstado` hace `return` para el administrador en **cualquier** estado (`SecurityUtil.isAdmin`, `subsystem/expedientes/services/tramitacion/Tramitador.java` línea 232), así que el perfil que el estado declara **no** es una barrera para él (§11, §14 nota 25). La guarda es, por tanto, **la misma exigencia del motor sin la exención**: pregunta lo mismo, a los mismos dos dueños, y la única diferencia es que no hace `return` para el administrador. Esa es también la razón de que su nombre diga «el perfil del estado» y no un perfil concreto — y de que **MUST NOT** ganar nunca un parámetro de perfil: en cuanto lo tuviera, el trámite tendría una segunda opinión sobre qué perfil atiende cada estado.
  - **Es complementaria de `exigeMismoCentroQueElExpediente`, no la sustituye ni la duplica**, y por eso los cuatro `trigger*` de `REVISION` y `RESOLUCION` llevan las **dos**: los `Ace` que el data-init crea a partir de un tipo de usuario o de un cargo llevan `centro` **a nulo**, y `AceRepository.findNombresPerfilesByExpediente` los acota con `aa.centro IS NULL OR aa.centro = :centro` contra el **centro activo del usuario**, nunca contra el **centro del expediente**. Es decir: la administrativa de otro centro **sí** ostenta `SECRETARIO` sobre un expediente ajeno, y quien la para es la guarda de centro (§14 nota 9). Al revés, el administrador **sí** pasa la guarda de centro en cuanto su centro activo es el del expediente, y quien le para es la guarda de perfil. Ninguna de las dos sabe nada de la otra.
- **MUST** usarse `SecurityUtil.getUser()`, nunca `AuthUtils.getUser()`.
- Las tres son **totales**: para cualquier entrada, o dejan pasar o fallan con motivo; no devuelven «vale» ante un dato ausente. En `exigeOstentarElPerfilDelEstado` eso incluye el usuario sin sesión, sin centro activo o sin `centroUsuarioActivo`: el dueño de (2) devuelve entonces un conjunto vacío y la guarda **falla cerrado**, igual que el punto de consulta del Paso 16.0. El estado sin `profile` **no** es una excepción a esto: ahí no hay ninguna exigencia que evaluar porque el XML no la declara, no un dato que falte.
- El mensaje llega por parámetro porque la especificación fija uno distinto en cada evento; la **decisión** (quién puede) está en un solo sitio.
- **Por qué `exigeSerElCreador` sigue existiendo aparte y NO se funde con la guarda de perfil.** La tentación es evidente: los dos estados de `SOLICITUD` declaran `profile="CREADOR"` y `PerfilesUsuarioServiceImpl.getPerfilesSobreExpediente` añade `CREADOR` **por autoría** (compara `expediente.getUsuarioRegistrador().getId()` con el del usuario), así que hoy las dos guardas dejan pasar exactamente a la misma persona. No se funden por dos motivos, y el primero es de código real, no de estilo: `AceRepository.findNombresPerfilesByExpediente` excluye `CREADOR` **solo en la rama del trámite** (`aa.tramite = :tramite AND aa.perfil.name <> 'CREADOR'`); las ramas `aa.expediente = :expediente` y `aa.tipoExpediente = :tipoExpediente` **no lo excluyen**, y el data-init tiene bindings para crear esos `Ace` (`asignacionesTipoUsuarioTipoExpediente` y `asignacionesCargoTipoExpediente` de `data-demo/input-config.xml`). Una sola fila con `perfilName="CREADOR"` y `tipoExpedienteCode` haría que «ostenta CREADOR» dejara de significar «es el autor» — y la guarda de `DELETE` pasaría a dejar borrar la solicitud de otro **sin que nadie tocara el trigger**. Hoy no ocurre porque `permisos.xml` asigna `CREADOR` por `tramiteCode`, que es justo la rama que lo excluye; pero eso es una propiedad del **dato**, no del código. El segundo motivo es que la spec enuncia VAL-DATOS_SOLICITUD-CONTINUAR-016 y VAL-DATOS_SOLICITUD-BORRADO-001 sobre la **autoría** («sus propias solicitudes»), cuyo dueño es `Expediente.usuarioRegistrador`, no sobre el perfil que atiende el estado: fundirlas le daría a la autoría un segundo dueño —el data-init de permisos— en vez de quitarle uno. El razonamiento completo, con las dos alternativas, está en `decisiones.md` **D13**.

#### 9.0.2 `DevolucionDelDirector`

Fichero `…/v1/DevolucionDelDirector.java`, con la misma forma que `ControlDeAcceso`. Es el **dueño único** de qué campos forman «la devolución del director»: la terna `motivoDevolucion` + `fechaDevolucion` + `devueltoPor`, que cuatro `trigger*` de tres fases distintas tienen que dejar limpia.

```java
public final class DevolucionDelDirector {
    public static void borrar(AnulacionMatriculaCicloFormativoV1 expediente);
}
```

- **Por qué se llama así.** La clase nombra **la cosa** de la que es dueña —la devolución que el director hace a secretaría— y el método dice **qué le hace**: `DevolucionDelDirector.borrar(expediente)` se lee entero en la línea de llamada, sin abrir la clase. **MUST NOT** nombrarse con una palabra del vocabulario del dominio de este trámite —`ciclo`, `Ciclo`, `grado`, `nivel`, `matrícula`— porque aquí «ciclo» es siempre **el ciclo formativo** que se anula (campo `ciclo`, entidad `Ciclo`, panel «Matrícula que se anula», regla `SinOtraSolicitudEnCursoParaElMismoCiclo`): un nombre como `CicloDeRevision` se leería espontáneamente como «algo del ciclo formativo» y obligaría a abrir el fichero para descubrir que no lo es.
- `borrar` pone a `null` `motivoDevolucion`, `fechaDevolucion` y `devueltoPor`. Nada más: no transiciona, no valida y no lanza.
- Es **idempotente** y **total**: si el expediente no venía de una devolución, esos campos ya están vacíos y volver a limpiarlos no cambia nada, así que ningún llamante necesita preguntarse si le toca.
- Lo llaman `triggerPresentar` (RN-007), `triggerEnviarAFirma` (RN-011), `triggerSubsanar` (RN-015) y `triggerFirmar` (RN-020). **El motivo de que exista es exactamente ese**: sin él la lista de los tres campos estaría escrita literalmente en cuatro sitios y quien añadiera un cuarto dato a la devolución tendría que acordarse de los cuatro, fallando en silencio si se dejara uno.
- **MUST NOT** añadírsele un segundo método para limpiar la decisión de secretaría (ni equivalente): esa limpieza tiene **un solo** punto de llamada y, además, no habla de la devolución del director, que es lo único de lo que esta clase es dueña. Una operación entra aquí cuando trata de la devolución del director **y** la comparten **dos o más** triggers.

#### 9.0.3 `ReglasAnulacionMatricula.esRechazo` — el dueño de «la revisión rechaza la anulación»

Fichero `…/v1/ReglasAnulacionMatricula.kt` (el mismo de la regla del DSL de §10.0, que es Kotlin y vive también en la raíz de la versión). Es el **dueño único en servidor** de la clasificación «la revisión rechaza si y solo si `sentidoRevision == RECHAZAR`», de la que se siguen sus dos consecuencias: la resolución **desestima** y el **motivo del rechazo** aplica.

```kotlin
object ReglasAnulacionMatricula {
    @JvmStatic
    fun esRechazo(expediente: AnulacionMatriculaCicloFormativoV1): Boolean =
        expediente.sentidoRevision == SentidoRevisionAnulacionMatriculaCicloFormativoV1.RECHAZAR
}
```

- **Por qué aquí y no en el `<extra-code-model>` de la entidad.** Ese bloque lo **reescribe entero el build** en cada compilación: `RichDomainXmlTask` (`MainModelXml` → `DomainXmlFile.addExtraCodeToDomainXml`) localiza la entity con `extends="Expediente"`, sustituye su `<extra-code-model>` por el que genera `extra-code-domain-xml.template` —que solo emite el enum `TipoDocumentoPdf` y `getDocumentoPdf`— y reescribe el `domains.xml` en `src/main/java`. Un método escrito ahí **desaparece en el primer build**, y con él se caen sus cuatro consumidores: el `trigger*` y el validador dejan de compilar y las dos expresiones Groovy del PDF fallan **en silencio** (log + campo vacío), dejando la resolución sin fórmula y sin motivo (ESC-014, ESC-015, ESC-030). Por eso el predicado vive en una clase de la raíz de la versión, que el build no toca.
- **`@JvmStatic` es obligatorio**, porque hay tres tipos de consumidor: Java (`ReglasAnulacionMatricula.esRechazo(expediente)` en el `trigger*`), el DSL del validador (`ifValueIn(ReglasAnulacionMatricula::esRechazo, listOf(true))`, que `IfValueIn` admite porque hace `dependentField.call(bean)` sobre cualquier `KFunction`: una función de un parámetro vale igual que un getter) y Groovy, en las dos expresiones del documento, que la invocan por su **FQCN** porque en un `${…}` no hay `import`: `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.ReglasAnulacionMatricula.esRechazo(self)`.
- Es **total** y sin estado: para cualquier expediente devuelve `true` o `false` y no lanza; con el sentido sin elegir devuelve `false`, que es lo que sus cuatro consumidores esperan.
- Imports del fichero: la entidad `AnulacionMatriculaCicloFormativoV1` y el enum `SentidoRevisionAnulacionMatriculaCicloFormativoV1`, los dos de `com.educaflow.subsystem.expedientes.db` (el paquete de la clase generada), además de los que ya necesita la regla del DSL de §10.0.
- Lleva el porqué en su KDoc: que es el dueño único en servidor de la clasificación, cuáles son sus cuatro consumidores, y que **MUST NOT** moverse al `<extra-code-model>` porque el build lo reescribe.
- **MUST NOT** aparecer en ninguna `AllowProperties`: no es una propiedad de la entidad, así que el DSL no lo mete en la whitelist del evento (`AllowPropertiesFactory` solo recoge los `field(...)`).
- **MUST NOT** duplicarse la comparación contra el enum en ninguno de los cuatro consumidores (§11), ni convertirse el predicado en un campo `transient` con `depends` (`decisiones.md` D10).

### 9.1 Fase SOLICITUD

```java
package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.solicitud;

public class PhaseEventManagerImpl extends PhaseEventManager<AnulacionMatriculaCicloFormativoV1> {
    @Inject
    public PhaseEventManagerImpl(AnulacionMatriculaCicloFormativoV1Repository repository) {
        super(AnulacionMatriculaCicloFormativoV1.class);
        this.repository = repository;
    }
}
```

- FQCN: `…v1.solicitud.PhaseEventManagerImpl`.
- Dependencias a inyectar:
  - `AnulacionMatriculaCicloFormativoV1Repository repository` — por constructor, como exige el patrón.
  - `@Inject FirmaServidorHelper firmaServidorHelper` — firma la solicitud en el servidor cuando el alumno tiene certificado custodiado.
- Constantes de clase:
  - `private static final Rectangulo POSICION_FIRMA_SOLICITUD = new Rectangulo(320, 418, 240, 32);` — medido sobre el PDF generado: la celda «Firma:» va de `y 415,52` a `y 472,22` y de `x 28,35` a `x 566,93`, y los rótulos ocupan sus dos primeras líneas (§5 y §14 nota 12)
  - `private static final int PAGINA_FIRMA_SOLICITUD = 1;`
  - **MUST** ser exactamente los cuatro números y la página que la `<action-method>` `exp-AnulacionMatriculaCicloFormativoV1-firmarSolicitud-action` pasa a `firmarDocumento`, para que la firma caiga en el mismo sitio se firme donde se firme.
- `import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.States;` — el `States` de **esta** versión.

**`triggerContinuar`**

1. `ERROR_NEGOCIO(ControlDeAcceso.exigeSerElCreador(expediente, "Solo puede modificar sus propias solicitudes"))` — VAL-DATOS_SOLICITUD-CONTINUAR-016
2. `ASIGNAR(fechaSolicitud = LocalDate.now(Convert.defaultZoneId))` — RN-001 / CC-005 (el **lugar** es `localidadCentro`, que ya se fijó al crear el expediente)
3. `GENERAR_PDF(SOLICITUD)`
4. `CREAR_METAFILE → pdfSolicitud` — RN-002 / CC-006 (sustituye a la de un intento anterior)
5. `LIMPIAR(pdfSolicitudFirmada)` — al regenerar la solicitud la firma anterior deja de corresponder al documento, así que **MUST** invalidarse: si no, tras `SUBSANAR` → `CONTINUAR` (o tras `VOLVER` → `CONTINUAR`) el expediente llegaría a `PENDIENTE_FIRMA` con la solicitud nueva y la firma vieja puesta, y en la rama de AutoFirma `PRESENTAR` la daría por buena sin volver a firmar. Sin condición: si no había firma, limpiarla es idempotente
6. `UPDATE_STATE(States.Solicitud.PENDIENTE_FIRMA)`

**`triggerVolver`**

1. `ERROR_NEGOCIO(ControlDeAcceso.exigeSerElCreador(expediente, "Solo puede volver atrás en sus propias solicitudes"))` — **guarda de autoría del diseño** (§11, «Las cuatro guardas de autoría»): el motor exime al administrador de `checkPerfilDelEstado`, así que sin ella el administrador puede disparar `VOLVER` sobre la solicitud de otro alumno, contra HU-010/ESC-025 (solo consulta) y ESC-020/ESC-032/ESC-045 (los no creadores la ven en solo lectura)
2. `ASIGNAR(claveCertificado = null)` — la clave del certificado no debe sobrevivir en el objeto
3. `UPDATE_STATE(States.Solicitud.DATOS_SOLICITUD)`

**`triggerPresentar`**

1. `ERROR_NEGOCIO(ControlDeAcceso.exigeSerElCreador(expediente, "Solo puede presentar sus propias solicitudes"))` — **guarda de autoría del diseño** (§11, «Las cuatro guardas de autoría»): el motor exime al administrador de `checkPerfilDelEstado`, así que es la única capa que le impide presentar la solicitud de otro alumno. Va **la primera**, antes incluso de leer la situación de firma, para que ningún efecto ocurra si el usuario no es el creador
2. `SERVICIO(CertificadoDigitalHelper.getSituacionFirmaByDni(SecurityUtil.getUser().getDni())) → situacionFirma` — la situación de firma del usuario autenticado, en una **variable local** del trigger (notación de enlace del resultado declarada en §14 nota 24); **MUST NOT** leerse del formulario
3. `SERVICIO(firmaServidorHelper.firmarEnServidor(dni, situacionFirma, expediente.getClaveCertificado(), expediente.getPdfSolicitud(), POSICION_FIRMA_SOLICITUD, PAGINA_FIRMA_SOLICITUD)) → pdfSolicitudFirmada` — el destino es el **campo** de la entidad; **solo si** `situacionFirma.isFirmaEnServidor()` (las dos notaciones, el enlace del resultado y la condición, están declaradas en §14 nota 24); en la rama de AutoFirma el campo ya llegó validado por `FirmaPdf` y no hay nada que firmar
4. `REGISTRO_ENTRADA(documento=pdfSolicitudFirmada, anexos=[]) → pdfJustificanteRegistroEntrada` — RN-003 y RN-004; el trámite no aporta anexos
5. `ASIGNAR(fechaHoraPresentacion = LocalDateTime.now(Convert.defaultZoneId))` — RN-005 / CC-007
6. `LIMPIAR(textoSubsanacion)` — RN-006; se hace **sin condición**: si el expediente no venía de una subsanación ese campo ya está vacío y limpiarlo es idempotente, así que no hace falta una rama que decida cuándo aplica
7. `LIMPIAR(sentidoRevision, motivoRechazo)` y, a continuación, `SERVICIO(DevolucionDelDirector.borrar(expediente))` — RN-007 (la decisión de secretaría y la devolución del director dejan de aplicar a la solicitud que vuelve a presentarse), las dos sin condición y con el mismo carácter idempotente
8. `UPDATE_STATE(States.Revision.PENDIENTE_REVISION)`
9. `ASIGNAR(claveCertificado = null)` — **en un `finally`** que envuelve los pasos 3 a 8 (notación de control de flujo declarada en §14 nota 24)

**`triggerDelete`**

1. `ERROR_NEGOCIO(ControlDeAcceso.exigeSerElCreador(expediente, "Solo puede borrar sus propias solicitudes"))` — VAL-DATOS_SOLICITUD-BORRADO-001. Es el **único** sitio posible: el `Tramitador` se salta la validación para `DELETE`, pero **sí** llama a `triggerDelete` antes de `repository.remove`, y una `BusinessException` aquí aborta el borrado.
2. `UPDATE_STATE: ninguno` — el expediente se elimina justo después y el `onEnterState` no llega a ejecutarse.

**`onEnter*`**

- `onEnterDatosSolicitud` — vacío.
- `onEnterPendienteFirma` — vacío.

#### 9.1.4 Lista de cobertura de la fase SOLICITUD

- Eventos (unión de los `events` de `DATOS_SOLICITUD` y `PENDIENTE_FIRMA`): `DELETE` → `triggerDelete`; `CONTINUAR` → `triggerContinuar`; `VOLVER` → `triggerVolver`; `PRESENTAR` → `triggerPresentar`.
- Estados: `DATOS_SOLICITUD` → `onEnterDatosSolicitud`; `PENDIENTE_FIRMA` → `onEnterPendienteFirma`.
- **MUST NOT** factorizarse ningún `trigger*`/`onEnter*` en una superclase: el dispatcher usa `getDeclaredMethods()`.

### 9.2 Fase REVISION

```java
package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.revision;

public class PhaseEventManagerImpl extends PhaseEventManager<AnulacionMatriculaCicloFormativoV1> { … }
```

- FQCN: `…v1.revision.PhaseEventManagerImpl`.
- Dependencias a inyectar: `AnulacionMatriculaCicloFormativoV1Repository repository` (por constructor). Ninguna más.
- Constantes de clase: ninguna.
- `import …v1.States;`, `import …v1.ReglasAnulacionMatricula;` (el dueño del predicado de §9.0.3) e `import …v1.ControlDeAcceso;`. **MUST NOT** aparecer `import com.educaflow.subsystem.expedientes.db.Profile;`: la guarda de perfil de las dos acciones 1 **no recibe ningún perfil** —se lo pregunta al `<state>` a través de `States` (§9.0.1)—, así que ningún `PhaseEventManagerImpl` nombra el enum.

**`triggerEnviarAFirma`**

1. `ERROR_NEGOCIO(ControlDeAcceso.exigeMismoCentroQueElExpediente(expediente, "Solo puede revisar solicitudes de su propio centro"))` — VAL-PENDIENTE_REVISION-ENVIAR_A_FIRMA-005 — y, a continuación, `ERROR_NEGOCIO(ControlDeAcceso.exigeOstentarElPerfilDelEstado(expediente, "Solo la secretaría del centro puede revisar esta solicitud"))` — **guarda de perfil del diseño** (§11, «Las ocho guardas de identidad»): el motor exime al administrador de `checkPerfilDelEstado` en **cualquier** estado, así que sin ella el administrador con centro activo en el del expediente —el que le da el Paso 15— podría enviar a la firma la solicitud de otro, contra HU-010/ESC-025 (solo consulta). **No se le pasa ningún perfil**: la guarda le pregunta a `States` cuál declara `PENDIENTE_REVISION` (hoy `SECRETARIO`) y exige ese (§9.0.1), de modo que el `TipoExpedienteInstance.xml` sigue siendo el único sitio donde se dice qué perfil atiende el estado. Las **dos** van en la acción 1, antes de cualquier efecto, y son complementarias (§9.0.1)
2. `ASIGNAR(fechaRevision = LocalDate.now(Convert.defaultZoneId))` — RN-008 / CC-009
3. `ASIGNAR(revisadoPor = SecurityUtil.getUser())` — RN-008 / CC-009
4. `LIMPIAR(motivoRechazo)` **si** `ReglasAnulacionMatricula.esRechazo(expediente) == false` (notación condicional declarada en §14 nota 24) — RN-009; va **antes** de generar el PDF para que la resolución no arrastre el motivo de un ciclo anterior. **La condición NO se escribe aquí: se le pregunta a su dueño.** «El motivo del rechazo aplica si y solo si el sentido es `RECHAZAR`» es una sola decisión y su dueño único en servidor es la función estática `ReglasAnulacionMatricula.esRechazo(…)` de la raíz de la versión (§9.0.3, §4 y §11); este trigger, el validador (§10.2) y las **dos** expresiones del PDF de la resolución (la fórmula de estimación/desestimación y el motivo, §5 y §14 nota 5) la **consultan**, y ninguno de los cuatro vuelve a comparar contra el enum. **MUST NOT** escribirse aquí `sentidoRevision != RECHAZAR` ni, mucho menos, `== ACEPTAR`: lo primero duplica al dueño y lo segundo, además, cambia el significado en cuanto exista un cuarto sentido. **MUST NOT** buscarse el predicado como método de la entidad (`expediente.esRechazo()`): el `<extra-code-model>` lo reescribe el build y por eso el dueño vive en la raíz de la versión (§9.0.3)
5. `LIMPIAR(textoSubsanacion)` — RN-010, sin condición (idempotente)
6. `SERVICIO(DevolucionDelDirector.borrar(expediente))` — RN-011, sin condición (idempotente)
7. `GENERAR_PDF(RESOLUCION)`
8. `CREAR_METAFILE → pdfResolucion` — RN-012 / CC-010 (sustituye a la de un envío anterior)
9. `UPDATE_STATE(States.Resolucion.PENDIENTE_FIRMA_DIRECTOR)`

**`triggerSubsanar`**

1. `ERROR_NEGOCIO(ControlDeAcceso.exigeMismoCentroQueElExpediente(expediente, "Solo puede revisar solicitudes de su propio centro"))` — VAL-PENDIENTE_REVISION-SUBSANAR-004 — y, a continuación, `ERROR_NEGOCIO(ControlDeAcceso.exigeOstentarElPerfilDelEstado(expediente, "Solo la secretaría del centro puede revisar esta solicitud"))` — **guarda de perfil del diseño** (§11, «Las ocho guardas de identidad»), por el mismo motivo y con la misma forma —**sin literal de perfil**, preguntándole a `States` el que declara `PENDIENTE_REVISION`—: sin ella el administrador podría devolver al alumno la solicitud de otro para subsanarla
2. `ASIGNAR(fechaRevision = LocalDate.now(Convert.defaultZoneId))` — RN-013 / CC-009
3. `ASIGNAR(revisadoPor = SecurityUtil.getUser())` — RN-013 / CC-009
4. `LIMPIAR(motivoRechazo)` — RN-014, sin condición (idempotente)
5. `SERVICIO(DevolucionDelDirector.borrar(expediente))` — RN-015, sin condición (idempotente)
6. `LIMPIAR(pdfResolucion)` — RN-016, sin condición (idempotente): los datos van a cambiar
7. `UPDATE_STATE(States.Solicitud.DATOS_SOLICITUD)`

**`onEnter*`**

- `onEnterPendienteRevision` — vacío.

#### 9.2.3 Lista de cobertura de la fase REVISION

- Eventos: `ENVIAR_A_FIRMA` → `triggerEnviarAFirma`; `SUBSANAR` → `triggerSubsanar`.
- Estados: `PENDIENTE_REVISION` → `onEnterPendienteRevision`.

### 9.3 Fase RESOLUCION

```java
package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.resolucion;

public class PhaseEventManagerImpl extends PhaseEventManager<AnulacionMatriculaCicloFormativoV1> { … }
```

- FQCN: `…v1.resolucion.PhaseEventManagerImpl`.
- Dependencias a inyectar:
  - `AnulacionMatriculaCicloFormativoV1Repository repository` — por constructor.
  - `@Inject AlmacenClaveResolver almacenClaveResolver` — el certificado del Director del centro.
- Constantes de clase:
  - `private static final Rectangulo POSICION_FIRMA_RESOLUCION = new Rectangulo(150, 317, 300, 53);` — medido sobre el PDF generado: el recuadro vacío bajo «El director / la directora del centro» va de `y 315,28` a `y 372,00` (§5 y §14 nota 12)
- `import …v1.States;` e `import …v1.ControlDeAcceso;`. **MUST NOT** aparecer `import com.educaflow.subsystem.expedientes.db.Profile;`: la guarda de perfil de las dos acciones 1 **no recibe ningún perfil** —se lo pregunta al `<state>` a través de `States` (§9.0.1)—, así que ningún `PhaseEventManagerImpl` nombra el enum.

**`triggerFirmar`**

1. `ERROR_NEGOCIO(ControlDeAcceso.exigeMismoCentroQueElExpediente(expediente, "Solo puede firmar resoluciones de su propio centro"))` — VAL-PENDIENTE_FIRMA_DIRECTOR-FIRMAR-001 — y, a continuación, `ERROR_NEGOCIO(ControlDeAcceso.exigeOstentarElPerfilDelEstado(expediente, "Solo el director del centro puede firmar la resolución"))` — **guarda de perfil del diseño** (§11, «Las ocho guardas de identidad»). Es la guarda **más crítica de las cuatro nuevas**: `FIRMAR` firma la resolución con el certificado del Director del centro, la registra de salida y cierra el expediente, y sin ella el administrador —al que `checkPerfilDelEstado` exime— podría resolver por el director cualquier expediente de su centro activo, contra HU-010/ESC-025. **No se le pasa ningún perfil**: le pregunta a `States` el que declara `PENDIENTE_FIRMA_DIRECTOR` (hoy `DIRECTOR`) y exige ese (§9.0.1). Las **dos** van en la acción 1, antes de resolver el almacén de claves
2. `SERVICIO(almacenClaveResolver.getDirector(expediente.getCentro())) → almacenDirector` (**variable local** del trigger, §14 nota 24); `ERROR_NEGOCIO(la resolución del almacén falla, "El centro no tiene configurada la firma del Director; avise al administrador")`; `ERROR_NEGOCIO(almacenDirector == null, "El centro no tiene configurada la firma del Director; avise al administrador")` — VAL-PENDIENTE_FIRMA_DIRECTOR-FIRMAR-002

   - «La resolución del almacén falla» es el caso en que `getDirector` termina con `RuntimeException` porque **el recurso del certificado del cargo falta o no se puede leer** (`AlmacenClaveFichero` rechaza un `InputStream` nulo con «El fileCertificate no puede ser null»). Ese fallo se transmite al usuario con ese mensaje, conservando la excepción original como causa.
   - **El ámbito del fallo capturado es SOLO esta resolución**: no cubre el `FIRMAR_SERVIDOR` de la acción 5 ni ninguna otra acción del trigger, que fallan con su propio mensaje. **MUST NOT** ampliarse para envolver la firma.
   - **Las dos comprobaciones son complementarias y las dos MUST escribirse.** `almacenClaveResolver.getDirector(centro)` **nunca devuelve `null`** hoy —construye siempre un `AlmacenClaveFichero`—, así que escribir **solo** la comparación con `null` dejaría una condición inalcanzable y la regla de la especificación de hecho sin implementar; y escribir solo la primera dejaría sin cubrir el día en que el resolver pase a depender del centro y devuelva `null` para un centro sin certificado configurado.
   - El almacén se resuelve **una sola vez** y se reutiliza en la acción 5.
3. `ASIGNAR(fechaResolucion = LocalDate.now(Convert.defaultZoneId))` y `ASIGNAR(firmadoPor = SecurityUtil.getUser())` — RN-017 / CC-012; van **antes** de generar el PDF porque la fecha se estampa en él
4. `GENERAR_PDF(RESOLUCION)` — RN-022: se **regenera** (no se recupera la guardada) para que lleve la fecha de la resolución recién anotada
5. `FIRMAR_SERVIDOR(cargo=DIRECTOR, rect=POSICION_FIRMA_RESOLUCION)` — RN-018: sobre el `DocumentoPdf` del paso 4, con el `almacenDirector` del paso 2 (**no** se vuelve a resolver)
6. `CREAR_METAFILE → pdfTemporal` — RN-018 (la resolución firmada queda guardada en el expediente; el campo que la recibe es `pdfResolucionFirmada`, acción 7)
7. `REGISTRO_SALIDA(documento=pdfTemporal, anexos=[]) → pdfResolucionFirmada` — RN-019; el campo recibe `registroSalida.getDocumento()`, la resolución ya registrada
8. `LIMPIAR(pdfResolucion)` — RN-023: en el expediente cerrado queda una única versión de la resolución
9. `SERVICIO(DevolucionDelDirector.borrar(expediente))` — RN-020, sin condición (idempotente)
10. `UPDATE_STATE segun sentidoRevision: ACEPTAR → States.Resolucion.ACEPTADA; RECHAZAR → States.Resolucion.RECHAZADA; SUBSANAR → error; default → error` — el `default` lanza `IllegalArgumentException`: llegar aquí con `SUBSANAR` o con el sentido vacío es un error de programación, porque el validador de `ENVIAR_A_FIRMA` solo deja pasar `ACEPTAR` y `RECHAZAR`

- **`pdfTemporal` (acciones 6 y 7) es una variable local del trigger**, el `MetaFile` intermedio que se le pasa al registro de salida: **MUST NOT** declararse en `domains.xml` ni aparecer en la tabla de campos de §4. El único campo de la entidad que se asigna aquí es `pdfResolucionFirmada`, que recibe `registroSalida.getDocumento()`.

**`triggerDevolver`**

1. `ERROR_NEGOCIO(ControlDeAcceso.exigeMismoCentroQueElExpediente(expediente, "Solo puede devolver resoluciones de su propio centro"))` — VAL-PENDIENTE_FIRMA_DIRECTOR-DEVOLVER-003 — y, a continuación, `ERROR_NEGOCIO(ControlDeAcceso.exigeOstentarElPerfilDelEstado(expediente, "Solo el director del centro puede devolver la resolución a la secretaría"))` — **guarda de perfil del diseño** (§11, «Las ocho guardas de identidad»), por el mismo motivo que `FIRMAR` y con la misma forma, **sin literal de perfil**
2. `ASIGNAR(fechaDevolucion = LocalDate.now(Convert.defaultZoneId))` y `ASIGNAR(devueltoPor = SecurityUtil.getUser())` — RN-021 / CC-011
3. `UPDATE_STATE(States.Revision.PENDIENTE_REVISION)`

**`onEnter*`**

- `onEnterPendienteFirmaDirector` — vacío.
- `onEnterAceptada` — vacío.
- `onEnterRechazada` — vacío.

#### 9.3.3 Lista de cobertura de la fase RESOLUCION

- Eventos: `FIRMAR` → `triggerFirmar`; `DEVOLVER` → `triggerDevolver`.
- Estados: `PENDIENTE_FIRMA_DIRECTOR` → `onEnterPendienteFirmaDirector`; `ACEPTADA` → `onEnterAceptada`; `RECHAZADA` → `onEnterRechazada` (los estados cerrados también necesitan su `onEnter`).

## 10. Especificación de los StateEventValidatorImpl

### 10.0 Regla propia del tipo — `SinOtraSolicitudEnCursoParaElMismoCiclo`

Fichero `…/v1/ReglasAnulacionMatricula.kt` (raíz de la versión). Es una regla del DSL, no lógica de negocio: comprueba el **valor de `ciclo`** contra el resto de expedientes del alumno.

Ese **mismo fichero** declara además el `object ReglasAnulacionMatricula` con el predicado `esRechazo(expediente)` (§9.0.3), que no es una regla del DSL sino el dueño único en servidor de «la revisión rechaza la anulación». Los dos están aquí por el mismo motivo: son código propio del tipo que **MUST NOT** vivir en el `<extra-code-model>` del `domains.xml`, porque el build lo reescribe entero en cada compilación.

```kotlin
data class SinOtraSolicitudEnCursoParaElMismoCiclo(
    val mensaje: String = "Ya tiene una solicitud de anulación en curso para este ciclo"
) : ValidationRule {
    override fun validate(value: Any?, bean: Any): BusinessMessages? { … }
}
```

Comportamiento exigido:

- Sin ciclo no existe ningún duplicado posible, así que la regla es **total**: devuelve válido por su propio significado, no porque otra regla cubra el caso. Que el ciclo sea obligatorio es una exigencia independiente, declarada aparte con `Required` en el mismo `field`.
- Si hay ciclo, busca con `JpaRepository.of(AnulacionMatriculaCicloFormativoV1::class.java).all().filter(...)` los expedientes que cumplan **todo**: `abierto = true`, mismo `usuarioRegistrador` que el del expediente, mismo `ciclo`, mismo `cursoAcademico` y `id` distinto del propio expediente. Si existe alguno, devuelve `BusinessMessages.single(I18n.get(mensaje))`; si no, `null`.
- **Qué hace si `cursoAcademico` es nulo o está en blanco.** VAL-DATOS_SOLICITUD-CONTINUAR-017 exige «mismo ciclo **y mismo curso académico**», y `cursoAcademico` lo rellena el servidor al crear el expediente a partir de `centro.getCurso()`, que puede no estar informado (§8 asignación 4). Sin ese dato la regla **no puede decidir**, y `self.cursoAcademico = :curso` con `:curso` a `null` **no casaría con ninguna fila**: devolvería «válido» en silencio justo cuando le falta lo que necesita, y el duplicado pasaría sin que nadie se entere. Por eso la regla **MUST** fallar explícitamente en ese caso, con `BusinessMessages.single(I18n.get("Su centro no tiene configurado el curso académico; avise a la secretaría del centro"))`. Es un centro mal configurado, no un dato del alumno, y la regla sigue siendo **total**: para cualquier entrada, o deja pasar, o falla con motivo; nunca devuelve «válido» por no saber. Ver §14 nota 15.
- El filtro **MUST** usar **parámetros con nombre** (`:usuario`, `:ciclo`, `:curso`, `:id`), nunca concatenación de cadenas (`k-secure-coding`).
- Un expediente cerrado (`abierto = false`) **no** cuenta: la especificación solo impide dos solicitudes **en curso**.

### 10.1 Fase SOLICITUD

```kotlin
package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.solicitud

import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1 as model
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.SinOtraSolicitudEnCursoParaElMismoCiclo
import com.educaflow.tramites.util.firma.ClaveCertificadoValida
import com.educaflow.tramites.util.firma.ifSituacionFirma

class StateEventValidatorImpl : StateEventValidator { … }
```

Tabla de cobertura:

| estado | evento | método | ¿reglas? |
|---|---|---|---|
| `DATOS_SOLICITUD` | `CONTINUAR` | `getForStateDatosSolicitudInEventContinuar` | sí |
| `DATOS_SOLICITUD` | `DELETE` | **ninguno** (exento) | — |
| `PENDIENTE_FIRMA` | `VOLVER` | `getForStatePendienteFirmaInEventVolver` | `rules { }` vacío |
| `PENDIENTE_FIRMA` | `PRESENTAR` | `getForStatePendienteFirmaInEventPresentar` | sí |

**`getForStateDatosSolicitudInEventContinuar`**

```kotlin
field(model::getNia) {
    +Required("Debe indicar su NIA")
    +Pattern("^\\d{8}$", "El NIA debe tener 8 dígitos")
}
field(model::getDireccion) {
    +Required("Debe indicar su dirección")
    +MinLength(5, "La dirección debe tener entre 5 y 150 caracteres")
    +MaxLength(150, "La dirección debe tener entre 5 y 150 caracteres")
}
field(model::getTelefono) {
    +Required("Debe indicar un teléfono de contacto")
    +Pattern("^[6789]\\d{8}$", "El teléfono debe tener 9 dígitos y empezar por 6, 7, 8 o 9")
}
field(model::getPoblacion) {
    +Required("Debe indicar su población")
    +MinLength(2, "La población debe tener entre 2 y 100 caracteres")
    +MaxLength(100, "La población debe tener entre 2 y 100 caracteres")
}
field(model::getProvincia) {
    +Required("Debe indicar su provincia")
    +MinLength(2, "La provincia debe tener entre 2 y 50 caracteres")
    +MaxLength(50, "La provincia debe tener entre 2 y 50 caracteres")
}
field(model::getCodigoPostal) {
    +Required("Debe indicar su código postal")
    +Pattern("^\\d{5}$", "El código postal debe tener 5 dígitos")
}
field(model::getCiclo) {
    +Required("Debe indicar el ciclo formativo")
    +SinOtraSolicitudEnCursoParaElMismoCiclo()
}
```

**`getForStatePendienteFirmaInEventVolver`** → `rules { }` (vacío, declarado explícitamente: el evento no lleva datos).

**`getForStatePendienteFirmaInEventPresentar`**

```kotlin
field(model::getClaveCertificado) {
    +ifSituacionFirma({ it.isFirmaEnServidor() }) {
        +ClaveCertificadoValida()
    }
}
field(model::getPdfSolicitudFirmada) {
    +ifSituacionFirma({ !it.isFirmaEnServidor() }) {
        +Required("Debe firmar la solicitud antes de presentarla")
        +FirmaPdf(model::getPdfSolicitud, "La firma no es válida o no corresponde a su documento de identidad")
    }
}
```

- Las dos ramas son **complementarias por construcción** (`isFirmaEnServidor()` / su negación), así que toda situación de firma cae en exactamente una; un usuario sin DNI cae en la de AutoFirma y `FirmaPdf` la rechaza con el mensaje **propio de esa rama** («su usuario no tiene un documento de identidad válido»), que el segundo argumento **no** sustituye: solo sustituye el de firma inválida (Paso 5).
- **Los dos `field(...)` MUST estar siempre**, aunque el trámite «solo vaya a usar» un camino: son la whitelist del evento y la rama que aplica depende del usuario que presente.
- Ningún otro campo entra en la whitelist de `PRESENTAR`: los datos ya se validaron en `CONTINUAR` y en este estado no se editan.

### 10.2 Fase REVISION

```kotlin
package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.revision

import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1 as model
import com.educaflow.subsystem.expedientes.db.SentidoRevisionAnulacionMatriculaCicloFormativoV1 as SentidoRevision
import com.educaflow.base.infrastructure.validation.rules.NoAdmitido
import com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.ReglasAnulacionMatricula
```

Tabla de cobertura:

| estado | evento | método | ¿reglas? |
|---|---|---|---|
| `PENDIENTE_REVISION` | `ENVIAR_A_FIRMA` | `getForStatePendienteRevisionInEventEnviarAFirma` | sí |
| `PENDIENTE_REVISION` | `SUBSANAR` | `getForStatePendienteRevisionInEventSubsanar` | sí |

**`getForStatePendienteRevisionInEventEnviarAFirma`**

```kotlin
field(model::getSentidoRevision) {
    +Required("Debe indicar el sentido de la revisión")
    +ifValueIn(model::getSentidoRevision, listOf(SentidoRevision.SUBSANAR)) {
        +NoAdmitido("Para pedir una subsanación use el botón «Pedir subsanación al alumno»")
    }
}
field(model::getMotivoRechazo) {
    +ifValueIn(ReglasAnulacionMatricula::esRechazo, listOf(true)) {
        +Required("Debe indicar el motivo del rechazo")
        +MinLength(10, "El motivo del rechazo debe tener entre 10 y 1000 caracteres")
        +MaxLength(1000, "El motivo del rechazo debe tener entre 10 y 1000 caracteres")
    }
}
field(model::getTextoSubsanacion) {
}
```

- **La rama de `motivoRechazo` pregunta al dueño, no reescribe la condición.** `ifValueIn(ReglasAnulacionMatricula::esRechazo, listOf(true))` consulta la función estática `esRechazo(…)` de la raíz de la versión (§9.0.3, §4 y §11), que es el dueño único en servidor de «cuándo aplica el motivo del rechazo»; **MUST NOT** volver a escribirse aquí `ifValueIn(model::getSentidoRevision, listOf(SentidoRevision.RECHAZAR))`, ni buscarse el predicado como método de la entidad (`model::esRechazo`), que el build borraría del `<extra-code-model>`. El DSL lo admite sin cambios: `IfValueIn` recibe un `KFunction<*>` cualquiera y lo invoca con `dependentField.call(bean)`, así que una función de **un** parámetro —el expediente— vale igual que un getter; `ReglasAnulacionMatricula` es un `object`, de modo que `ReglasAnulacionMatricula::esRechazo` es ya una referencia ligada al objeto cuyo único argumento es el bean. Y **no** toca la whitelist del evento: `AllowPropertiesFactory` solo recoge los `field(...)`, nunca el campo del que depende una rama, así que `esRechazo` —que además no es una propiedad— no entra en `AllowProperties`. La rama de `sentidoRevision` de este mismo método sigue usando `getSentidoRevision`, porque expresa **otra** decisión (que `SUBSANAR` no se admite en este evento), no la del motivo del rechazo.
- El `field(model::getTextoSubsanacion)` **sin reglas** está a propósito: el formulario de la secretaría permite escribir ese texto y, si no entrara en la whitelist del evento, lo que el usuario escribiera se perdería en silencio. No lleva ninguna regla porque en este evento no se exige nada de él (el trigger lo limpia, RN-010).
- La condición de cuándo aplica cada regla se lee **en la rama**, no dentro de la regla.

**`getForStatePendienteRevisionInEventSubsanar`**

```kotlin
field(model::getSentidoRevision) {
    +ifValueNotIn(model::getSentidoRevision, listOf(SentidoRevision.SUBSANAR)) {
        +NoAdmitido("Para pedir una subsanación elija el sentido «Pedir subsanación»")
    }
}
field(model::getTextoSubsanacion) {
    +ifValueIn(model::getSentidoRevision, listOf(SentidoRevision.SUBSANAR)) {
        +Required("Debe indicar al alumno qué tiene que subsanar")
        +MinLength(10, "El texto de la subsanación debe tener entre 10 y 1000 caracteres")
        +MaxLength(1000, "El texto de la subsanación debe tener entre 10 y 1000 caracteres")
    }
}
field(model::getMotivoRechazo) {
}
```

- `ifValueNotIn` cubre también el caso «sentido sin elegir»: `null` no está en la lista, así que la rama aplica y `NoAdmitido` falla con el mensaje de la especificación.
- Las tres reglas de `textoSubsanacion` van **dentro de la rama del sentido** por el mismo motivo, y no sueltas: si el sentido elegido no es «Pedir subsanación» la pantalla ni siquiera muestra ese texto (`showIf` del panel de revisión), así que exigirlo además de rechazar el sentido produciría **dos** mensajes cuando la especificación fija **uno** («Para pedir una subsanación elija el sentido «Pedir subsanación»», ESC-043 / T-027). El campo sigue en la whitelist del evento; lo que la rama declara es **cuándo** aplican VAL-PENDIENTE_REVISION-SUBSANAR-002 y 003, que es exactamente cuando la spec las pide. La condición de cuándo aplica una regla se lee **en la rama**, no dentro de la regla.
- `motivoRechazo` entra en la whitelist sin reglas por el mismo motivo que `textoSubsanacion` en el evento anterior: la pantalla lo puede llevar escrito.

### 10.3 Fase RESOLUCION

```kotlin
package com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.resolucion

import com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1 as model
```

Tabla de cobertura:

| estado | evento | método | ¿reglas? |
|---|---|---|---|
| `PENDIENTE_FIRMA_DIRECTOR` | `FIRMAR` | `getForStatePendienteFirmaDirectorInEventFirmar` | `rules { }` vacío |
| `PENDIENTE_FIRMA_DIRECTOR` | `DEVOLVER` | `getForStatePendienteFirmaDirectorInEventDevolver` | sí |

**`getForStatePendienteFirmaDirectorInEventFirmar`** → `rules { }` (vacío, declarado explícitamente). El director **no envía ningún dato** al firmar: la firma la pone el centro y él solo la autoriza con el botón. Dejarlo vacío es también lo que impide que el cliente dicte ningún campo en este evento.

**`getForStatePendienteFirmaDirectorInEventDevolver`**

```kotlin
field(model::getMotivoDevolucion) {
    +Required("Debe indicar a secretaría por qué devuelve la resolución")
    +MinLength(10, "El motivo de la devolución debe tener entre 10 y 1000 caracteres")
    +MaxLength(1000, "El motivo de la devolución debe tener entre 10 y 1000 caracteres")
}
```

### 10.4 Frontera de confianza

- Ningún `field(...)` menciona un campo clasificado `servidor` en §4 — ni `codePhase`, `codeState`, `abierto`, `centro` o `usuarioRegistrador`, ni ninguno de los cinco `MetaFile` que rellena el servidor, ni las fechas y autores de revisión, devolución y resolución. La **única excepción** es `pdfSolicitudFirmada`, que **también está clasificado `servidor`** y aparece en el validador únicamente porque es el destino de la firma en cliente y ese `field(...)` es el único sitio donde se puede comprobar la firma (§4).
- Cada campo editable en la vista de un estado aparece en el `field(...)` del evento que se dispara desde esa vista: `nia`, `direccion`, `telefono`, `poblacion`, `provincia`, `codigoPostal` y `ciclo` en `CONTINUAR`; `claveCertificado` y `pdfSolicitudFirmada` en `PRESENTAR`; `sentidoRevision`, `motivoRechazo` y `textoSubsanacion` en `ENVIAR_A_FIRMA` y en `SUBSANAR`; `motivoDevolucion` en `DEVOLVER`.
- **CRITICAL** — esta puerta **NO** protege el endpoint REST automático `POST /ws/rest/com.educaflow.subsystem.expedientes.db.AnulacionMatriculaCicloFormativoV1`, que Axelor publica para toda entidad y que **no pasa por el `Tramitador`**. Son dos puertas distintas: **MUST NOT** darse por protegida esta entidad porque su tramitación valide por evento, y **MUST NOT** introducirse un `ModelService` deny-all como parche (se retiró a propósito, ver `CLAUDE.md`).
- `readonly`, `showIf` y `hidden` de las vistas son **UX, nunca defensa**.

## 11. Reparto de reglas

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

**Dueño único de «la revisión rechaza la anulación» — un predicado en el servidor, no una convención de redacción.** La decisión es una sola —*la revisión rechaza si y solo si `sentidoRevision == RECHAZAR`*, de donde se siguen sus dos consecuencias: la resolución **desestima** la solicitud y el **motivo del rechazo** aplica— y su **dueño** es la función estática `ReglasAnulacionMatricula.esRechazo(expediente)`, declarada en `…/v1/ReglasAnulacionMatricula.kt`, en la raíz de la versión (§9.0.3 y §4). **MUST NOT** declararse en el `<extra-code-model>` del `domains.xml`: el build reescribe ese bloque entero en cada compilación y el predicado desaparecería, dejando sin compilar al trigger y al validador y en silencio a las dos expresiones del PDF. Los **cuatro** puntos que se evalúan en el **servidor** (en tres ficheros: `resolucion.xml` lo consulta dos veces, una por cada consecuencia) no la escriben: se la **preguntan**.

| Sitio | Capa | Cómo consulta al dueño |
|---|---|---|
| acción 4 de `triggerEnviarAFirma` (§9.2) | Java, servidor | `if (ReglasAnulacionMatricula.esRechazo(expediente) == false) { … }` |
| rama de `motivoRechazo` en `getForStatePendienteRevisionInEventEnviarAFirma` (§10.2) | DSL del validador, servidor | `ifValueIn(ReglasAnulacionMatricula::esRechazo, listOf(true)) { … }` |
| expresión de la **fórmula** de estimación/desestimación en `documentospdf/resolucion.xml` (§5, §14 nota 5) | Groovy, servidor | `${<FQCN>.esRechazo(self) ? "…desestima…" : "…estima…"}`, con `<FQCN>` = `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.ReglasAnulacionMatricula` (en un `${…}` no hay `import`) |
| expresión del **motivo** en `documentospdf/resolucion.xml` (§5) | Groovy, servidor | `${<FQCN>.esRechazo(self) ? … : ""}`, con el mismo `<FQCN>` |

**MUST NOT** aparecer en ninguno de esos cuatro una comparación contra el enum (`sentidoRevision == RECHAZAR`, `!= RECHAZAR` y, mucho menos, `== ACEPTAR`): eso devolvería la decisión a varios dueños. Que el DSL lo admita no es una suposición — `IfValueIn` recibe un `KFunction<*>` y lo invoca con `dependentField.call(bean)`, así que un método sin argumentos vale igual que un getter; y `AllowPropertiesFactory` solo recoge los `field(...)`, nunca el dependiente de una rama, así que consultar el predicado **no** mete nada en la whitelist del evento.

**Los TRES literales de cliente sobre el RECHAZO, y por qué son tres y no dos.** Son exactamente los que se evalúan **en el navegador, sobre el registro del formulario sin guardar**, mientras el usuario está cambiando `sentidoRevision`. **Esta lista es el inventario completo, y hay que mirarla entera**: **MUST NOT** existir en el diseño ningún literal de cliente sobre `sentidoRevision` que no esté aquí o en el párrafo «El par simétrico de `SUBSANAR`» de más abajo, y quien añada uno **MUST** añadirlo a la lista que le corresponda.

1. el `showIf="sentidoRevision=='RECHAZAR'"` de los paneles que enseñan el motivo del rechazo (`revision`, `decision-secretaria` y `resolucion-firmada` del `views.xml` de la raíz, Paso 7), con el `requiredIf="sentidoRevision=='RECHAZAR'"` de `motivoRechazo` en `revision` — RUI-PENDIENTE_REVISION-SECRETARIO-004 y las RUI de las pantallas que muestran la decisión;
2. el `if="sentidoRevision != 'RECHAZAR'"` del primer `<field>` del `<action-record>` `…-limpiar-revision-action` (`fases/revision/views.xml`, Paso 12) — RUI-PENDIENTE_REVISION-SECRETARIO-009, y
3. el `showIf="sentidoRevision=='ACEPTAR'"` del `<help>` «Si firma la resolución, la anulación surtirá efecto desde esta fecha» del panel `matricula-director` del `views.xml` de la raíz de la versión (Paso 7) — **RUI-PENDIENTE_FIRMA_DIRECTOR-DIRECTOR-007**, cuya condición la especificación fija literalmente como «el sentido de la revisión es *Aceptar la anulación*».

Ahí el predicado del servidor **no sirve**: `ReglasAnulacionMatricula.esRechazo(…)` se calcula sobre el estado persistido y no está recalculado mientras el usuario teclea, y convertirlo en un `transient` con `depends` para que el cliente lo recibiera crearía justo lo que se quiere evitar — un campo del modelo en el que el cliente no puede confiar (`decisiones.md` D10). Por eso el dueño es una **función** y no un campo: no lo ve ninguna vista.

**El par simétrico de `SUBSANAR`, que proyecta OTRA clasificación y por eso no tiene dueño en servidor.** `showIf="sentidoRevision=='SUBSANAR'"` y `requiredIf="sentidoRevision=='SUBSANAR'"` en `textoSubsanacion` (panel `revision`, Paso 7) y el `if="sentidoRevision != 'SUBSANAR'"` del **segundo** `<field>` del `<action-record>` `…-limpiar-revision-action` (Paso 12) — RUI-PENDIENTE_REVISION-SECRETARIO-006 y -010. No dicen «la revisión rechaza» sino «la revisión pide subsanación», que es una clasificación **distinta** y que **ningún sitio de servidor consulta**: en el servidor la subsanación no se pregunta, se **dispara** — es el evento `SUBSANAR`, cuyo `trigger*` y cuyo método del validador ya son de ese caso y no tienen nada que clasificar (§9.2 y §10.2). Por eso no hay un `esSubsanacion()`: crearlo sería un predicado sin consumidores. Se formulan alrededor de su propio valor, `SUBSANAR`, por la misma razón que los del rechazo lo hacen alrededor de `RECHAZAR`, y quedan inventariados aquí para que un cuarto sentido de revisión los haga revisar también.

**La convención de redacción del cliente, y su única excepción declarada.** Los literales que proyectan la clasificación del **dueño de servidor** —*la revisión rechaza*— se formulan **siempre alrededor de `RECHAZAR`** (`== 'RECHAZAR'` o `!= 'RECHAZAR'`) y **MUST NOT** formularse alrededor de `ACEPTAR`, para que añadir un cuarto sentido no le cambie el significado a nadie sin que se note. Eso cubre los sitios **1** y **2**. El sitio **3 es la excepción, y lo es porque no proyecta esa clasificación sino otra distinta**: no habla del rechazo ni del motivo, sino de cuándo la anulación surtirá efecto, y la especificación fija su condición como «el sentido de la revisión es *Aceptar la anulación*» (RUI-PENDIENTE_FIRMA_DIRECTOR-DIRECTOR-007). Reescribirlo como `!= 'RECHAZAR'` **cambiaría lo que dice la spec** —lo enseñaría también con `SUBSANAR` y con el sentido vacío—, así que **MUST** quedarse alrededor de `ACEPTAR`. La convención, por tanto, no es universal sobre el campo: **es universal sobre la clasificación del dueño de servidor**, y el sitio 3 queda declarado aquí como el único literal de cliente que no la sigue, con su motivo.

**Qué hay que tocar si se añade un cuarto sentido de revisión:** la función `ReglasAnulacionMatricula.esRechazo(…)` —que es donde vive la clasificación de servidor— y, si el nuevo sentido cambiara lo que ve el usuario, los **tres** literales de cliente de la lista de arriba, **los tres**: el `showIf` de los paneles del motivo (1), el `if` del `<action-record>` (2) y el `showIf` del `<help>` de `matricula-director` (3) — este último es el que más fácilmente se olvida, porque está formulado alrededor de `ACEPTAR` y vive en el `views.xml` de la raíz, no en el de una fase. Y, si el nuevo sentido tocara además la subsanación, el par simétrico de `SUBSANAR` del párrafo anterior. Los **cuatro** consumidores de servidor no se tocan. Ese reparto —un dueño en servidor y tres literales de cliente declarados, dos de ellos siguiendo la convención y el tercero con su excepción escrita— es el que registra `decisiones.md` **D10**.

   **Y para que los literales 1 y 3 puedan evaluarse, el campo tiene que estar en el form:** los paneles que condicionan por `sentidoRevision` lo **pintan ellos mismos** —`revision` y `decision-secretaria` visible, `resolucion-firmada` y `matricula-director` con `hidden="true"`—, porque una condición de vista que cita un campo que ningún panel del form pinta sale siempre falsa en silencio (invariante del Paso 7 y §14 nota 29). Eso **no** añade ningún sitio de cliente más: el campo oculto no formula la condición, solo trae el dato con el que se evalúa.

**Las ocho guardas de identidad: cuatro de autoría y cuatro de perfil, y por qué las ocho.** La exención del administrador es **global** —`Tramitador.checkPerfilDelEstado` hace `return` para él en **cualquier** estado (`SecurityUtil.isAdmin`, `Tramitador.java` línea 232)—, así que el `profile` del estado no basta como barrera en **ninguno** de los tres estados con eventos. Por eso **los ocho eventos del tipo llevan en su acción 1 una guarda que comprueba en el servidor quién dispara**: los cuatro de los estados `CREADOR` (`CONTINUAR`, `DELETE`, `VOLVER`, `PRESENTAR`) con `ControlDeAcceso.exigeSerElCreador`, y los cuatro de los estados `SECRETARIO` y `DIRECTOR` (`ENVIAR_A_FIRMA`, `SUBSANAR`, `FIRMAR`, `DEVOLVER`) con `ControlDeAcceso.exigeOstentarElPerfilDelEstado(expediente, mensaje)`, **además** de la guarda de centro que la especificación sí numera. Sin las cuatro de perfil, el administrador —que pasa la guarda de centro en cuanto su centro activo es el del expediente, y el Paso 15 le asigna precisamente CIPFP Mislata— podría revisar, pedir subsanación, devolver y, sobre todo, **firmar**: cerrar el expediente con la resolución firmada con el certificado del Director y registrada de salida. Eso contradice HU-010/ESC-025 igual que lo contradecía el caso de `VOLVER`/`PRESENTAR`, y es el mismo olor **«ramas que no cubren todos los casos»** de `k-code-quality`: la guarda de centro deja fuera exactamente al único usuario al que el motor exime. **Las cuatro de perfil no escriben ningún perfil: preguntan por los dos lados.** «Qué perfil atiende este estado» tiene un dueño único y ya existente —el atributo `profile` del `<state>` del `TipoExpedienteInstance.xml`, proyectado en la clase generada `States`—, y «si el usuario lo ostenta» tiene otro —`PerfilesUsuarioService.getPerfilesSobreExpediente`—; la guarda consulta a los dos, exactamente como hace `Tramitador.checkPerfilDelEstado` (`State.getProfile()` + el conjunto de perfiles), y su única diferencia con el motor es que **no exime al administrador**. Por eso se llama `exigeOstentarElPerfilDelEstado` y **no recibe ningún parámetro `Profile`**: si lo recibiera, cambiar `profile="SECRETARIO"` por otro valor en el XML dejaría a las dos guardas de `PENDIENTE_REVISION` exigiendo el perfil viejo —divergiendo **en silencio** de lo que el motor exige a todo el mundo menos al administrador, que es justo el caso que la guarda existe para cubrir— y ningún test del build lo vería. **MUST NOT** reintroducirse el literal, ni como argumento ni como constante del `PhaseEventManagerImpl`. La decisión, con sus alternativas, está en `decisiones.md` **D13**.

**Las cuatro guardas de autoría, y por qué son cuatro y no dos.** `CONTINUAR`, `DELETE`, `VOLVER` y `PRESENTAR` —los cuatro eventos de los dos estados cuyo `profile` es `CREADOR`— llevan **todos** la guarda `ControlDeAcceso.exigeSerElCreador` en su primera línea. Las dos primeras las pide la especificación (VAL-DATOS_SOLICITUD-CONTINUAR-016 y VAL-DATOS_SOLICITUD-BORRADO-001); las dos últimas las **añade el diseño**, y la razón es la misma que hace falta para las dos primeras: `Tramitador.checkPerfilDelEstado` **exime al administrador** (`SecurityUtil.isAdmin`), así que el perfil del estado no basta para garantizar que quien dispara el evento sea el creador. Sin ellas, un administrador que abriera la solicitud de otro alumno por «Expedientes Pendientes» —que fija `_profile=CREADOR`— podría disparar `VOLVER` y `PRESENTAR` sobre ella, contra HU-010/ESC-025 (el administrador solo consulta, en solo lectura) y ESC-020/ESC-032/ESC-045 (los no creadores ven la pantalla en solo lectura). **Y siguen siendo una guarda distinta de la de perfil, no un caso particular suyo:** aunque los dos estados declaren `profile="CREADOR"` y el dueño de los perfiles conceda `CREADOR` por autoría, «ser el autor» y «ostentar el perfil que el estado declara» solo coinciden mientras ninguna fila de permisos asigne `CREADOR` por tipo de expediente o por expediente —las dos ramas de `findNombresPerfilesByExpediente` que **no** lo excluyen—, y la spec enuncia estas dos reglas sobre la autoría, cuyo dueño es `Expediente.usuarioRegistrador` (§9.0.1 y `decisiones.md` D13). **Esto anula expresamente la indicación de `design-guidelines.md`** («no se han añadido comprobaciones de autoría a VOLVER ni a PRESENTAR: serían una copia de lo que el motor ya garantiza»): el motor **no** lo garantiza para el administrador, que es exactamente el caso abierto, y la propia guía reconoce esa excepción al justificar por qué sí se conservan las otras dos. La desviación queda declarada aquí y en §14 nota 25.

**Por qué las comprobaciones de identidad no van al validador.** El DSL cuelga cada regla de un campo, y esas comprobaciones —autoría, centro y perfil— no hablan de ningún campo. Además, dos de ellas caen donde el validador no llega o no debe crecer: `DELETE` se salta la validación entera en el `Tramitador` (su método del validador no existe y nunca se invocaría), y `FIRMAR` no admite ningún dato del formulario, así que darle un `field(...)` solo para colgar la regla metería ese campo en la whitelist del evento. La regla de reparto queda enunciable en una frase: **si la regla habla del valor de un campo, validador; si habla de quién eres, guarda del trigger**. Las cuatro guardas de perfil caen en la misma frase y por el mismo lado: preguntan quién eres, no qué has enviado. Ver `decisiones.md` D4 y D13.

### Dónde va cada regla de la especificación

| Regla | Capa |
|---|---|
| VAL-DATOS_SOLICITUD-CONTINUAR-001 … 013 | DSL del validador, `getForStateDatosSolicitudInEventContinuar` |
| VAL-DATOS_SOLICITUD-CONTINUAR-014 y 015 | *(eliminadas por la propia especificación: el nombre del ciclo ya no se escribe a mano)* |
| VAL-DATOS_SOLICITUD-CONTINUAR-016 | guarda de `triggerContinuar` (`ControlDeAcceso.exigeSerElCreador`) |
| VAL-DATOS_SOLICITUD-CONTINUAR-017 | DSL del validador, regla `SinOtraSolicitudEnCursoParaElMismoCiclo` sobre `ciclo` |
| VAL-DATOS_SOLICITUD-BORRADO-001 | guarda de `triggerDelete` (único sitio posible: `DELETE` no pasa por el validador) |
| *(sin identificador en la spec)* — autoría en `VOLVER` | guarda de `triggerVolver` (`ControlDeAcceso.exigeSerElCreador`). **La añade el diseño**, no la spec: cierra el único evento de `PENDIENTE_FIRMA` que el administrador podía disparar sobre la solicitud de otro, porque el motor le exime del perfil del estado (§11, «Las cuatro guardas de autoría», y §14 nota 25) |
| *(sin identificador en la spec)* — autoría en `PRESENTAR` | guarda de `triggerPresentar` (`ControlDeAcceso.exigeSerElCreador`). **La añade el diseño**, por el mismo motivo: sin ella, el administrador podría presentar la solicitud de otro alumno (§11, «Las cuatro guardas de autoría», y §14 nota 25) |
| VAL-PENDIENTE_FIRMA-PRESENTAR-001 y 002 | DSL del validador, rama de AutoFirma de `getForStatePendienteFirmaInEventPresentar` |
| VAL-PENDIENTE_REVISION-ENVIAR_A_FIRMA-001 … 004 | DSL del validador, `getForStatePendienteRevisionInEventEnviarAFirma` |
| VAL-PENDIENTE_REVISION-ENVIAR_A_FIRMA-005 | guarda de centro de `triggerEnviarAFirma` (`ControlDeAcceso.exigeMismoCentroQueElExpediente`) |
| *(sin identificador en la spec)* — perfil en `ENVIAR_A_FIRMA` | guarda de `triggerEnviarAFirma` (`ControlDeAcceso.exigeOstentarElPerfilDelEstado(…)`, que exige el perfil que `PENDIENTE_REVISION` declara —hoy `SECRETARIO`— sin repetirlo). **La añade el diseño**, no la spec: el motor exime al administrador del perfil del estado y la guarda de centro no le para (§11, «Las ocho guardas de identidad», §14 nota 25 y `decisiones.md` D13) |
| VAL-PENDIENTE_REVISION-SUBSANAR-001 … 003 | DSL del validador, `getForStatePendienteRevisionInEventSubsanar` |
| VAL-PENDIENTE_REVISION-SUBSANAR-004 | guarda de centro de `triggerSubsanar` (`ControlDeAcceso.exigeMismoCentroQueElExpediente`) |
| *(sin identificador en la spec)* — perfil en `SUBSANAR` | guarda de `triggerSubsanar` (`ControlDeAcceso.exigeOstentarElPerfilDelEstado(…)`, mismo estado y mismo perfil declarado). **La añade el diseño**, por el mismo motivo |
| VAL-PENDIENTE_FIRMA_DIRECTOR-FIRMAR-001 y 002 | guardas de `triggerFirmar` (identidad y disponibilidad del certificado del centro; ninguna habla de un campo del formulario). La **002** se implementa capturando el fallo de la resolución del almacén, no comparando con `null`, porque `getDirector` nunca devuelve `null` (§9.3 acción 2 y §14 nota 14) |
| VAL-PENDIENTE_FIRMA_DIRECTOR-DEVOLVER-001 y 002 | DSL del validador, `getForStatePendienteFirmaDirectorInEventDevolver` |
| VAL-PENDIENTE_FIRMA_DIRECTOR-DEVOLVER-003 | guarda de centro de `triggerDevolver` (`ControlDeAcceso.exigeMismoCentroQueElExpediente`) |
| *(sin identificador en la spec)* — perfil en `FIRMAR` y en `DEVOLVER` | guarda de `triggerFirmar` y de `triggerDevolver` (`ControlDeAcceso.exigeOstentarElPerfilDelEstado(…)`, que exige el perfil que `PENDIENTE_FIRMA_DIRECTOR` declara —hoy `DIRECTOR`— sin repetirlo). **Las añade el diseño**, por el mismo motivo; la de `FIRMAR` es la que impide que el administrador cierre el expediente firmando la resolución con el certificado del Director |
| RN-001 … RN-007 | acciones de `triggerContinuar` y `triggerPresentar` (§9.1) |
| RN-008 … RN-016 | acciones de `triggerEnviarAFirma` y `triggerSubsanar` (§9.2) |
| RN-017 … RN-023 | acciones de `triggerFirmar` y `triggerDevolver` (§9.3) |
| CC-001, CC-003, CC-004 | `triggerInitialEvent` (§8) |
| CC-002 | **sin origen de datos hoy**: no existe ninguna ficha del alumno de la que copiar; los campos nacen vacíos (§8 y §14) |
| CC-005 … CC-013 | acciones de los `trigger*` (§9) |
| CC-014 | campo derivado `Ciclo.gradoNivel` del catálogo (§4) |
| RUI-DATOS_SOLICITUD-CREADOR-004 y 005 | *(eliminadas por la propia especificación: el nombre del ciclo ya no se escribe a mano y el grado ya no se elige aparte, lo lleva puesto el ciclo)* |
| RUI-DATOS_SOLICITUD-CREADOR-001 … 003 y 006 … 012 | vista: `showIf` del panel `subsanacion`, `readonly` de los campos congelados, `help` de NIA/teléfono/CP/ciclo, `grid-view`/`form-view` del ciclo y campo punteado `ciclo.gradoNivel` |
| RUI-DATOS_SOLICITUD-CREADOR-002 | vista: `<action-attrs>` `exp-AnulacionMatriculaCicloFormativoV1-DATOS_SOLICITUD-onLoad-action`, en el `onLoad` **solo** de `DATOS_SOLICITUD`/`CREADOR`, que pone `required=true` a `nia`, `direccion`, `telefono`, `poblacion`, `provincia`, `codigoPostal` y `ciclo`. No se marca en los `<field>` del form plantilla porque `datos-alumno` y `matricula` los comparten ocho forms de solo lectura. La exigencia real está en el validador |
| RUI-DATOS_SOLICITUD-GENERICA-001 y 002 | vista genérica de `DATOS_SOLICITUD` |
| RUI-PENDIENTE_FIRMA-CREADOR-001 y 002 | vista `PENDIENTE_FIRMA` del `CREADOR` (visor a 900 px y panel `subsanacion`) |
| RUI-PENDIENTE_FIRMA-GENERICA-001 | vista genérica de `PENDIENTE_FIRMA` |
| RUI-PENDIENTE_REVISION-SECRETARIO-001 … 006 y 009 … 014 | *(no existen las 007 y 008: la numeración de la spec salta de 006 a 009)* vista `PENDIENTE_REVISION` del `SECRETARIO` y `<action-record>` `…-limpiar-revision-action`. En concreto, la obligatoriedad visual del **002** es `required="true"` en `sentidoRevision` y la del **004** y el **006** son `requiredIf="sentidoRevision=='RECHAZAR'"` en `motivoRechazo` y `requiredIf="sentidoRevision=='SUBSANAR'"` en `textoSubsanacion`, las tres en el panel `revision` (que solo usa este form). La exigencia real está en el validador |
| RUI-PENDIENTE_REVISION-GENERICA-001 y 002 | vista genérica de `PENDIENTE_REVISION`: no incluye ningún panel de la decisión de secretaría. **NO está garantizado que todo el que no sea la administrativa caiga en ella**: la condición de perfil de la bandeja de secretaría (Paso 16.0) solo **quita el expediente de las dos bandejas nuevas**, es decir, corta el camino normal de navegación; no elige la vista. El `_profile` **viaja en la petición** y en el servidor **nada comprueba que el usuario ostente ese perfil** para elegir la vista (`ExpedienteController.viewExpediente` toma el `_profile` del contexto con `actionRequestHelper.getProfileName()` y solo lo pasa por `checkProfileDelTipoExpediente`, que comprueba que el perfil lo **use** algún estado del tipo; `PhaseEventManager.getViewName` devuelve entonces el form de ese perfil), así que cualquiera con lectura sobre el expediente puede invocar `subsysExpedientes-event-view-action` con `_profile=SECRETARIO` sin pasar por la bandeja. Es el residuo **NO mitigado** de §14 nota 8(d), y cerrarlo es del motor |
| RUI-PENDIENTE_FIRMA_DIRECTOR-DIRECTOR-001 … 007 | vista `PENDIENTE_FIRMA_DIRECTOR` del `DIRECTOR` |
| RUI-PENDIENTE_FIRMA_DIRECTOR-GENERICA-001 | vista genérica de `PENDIENTE_FIRMA_DIRECTOR`. Igual que la anterior: la condición de perfil de la bandeja de firma (Paso 16.0) solo **saca el expediente de esa bandeja**; **no garantiza** que todo el que no sea el director caiga en la genérica, porque el `_profile` viaja en la petición y nada en el servidor comprueba que el usuario **ostente** `DIRECTOR` para elegir la vista. Esta regla dice literalmente que «la decisión de secretaría no se enseña a nadie hasta que el director firma la resolución»: es una regla que debe **impedir** algo y hoy se apoya solo en la capa de vista. Residuo **NO mitigado** de §14 nota 8(d); cerrarlo es del motor |
| RUI-ACEPTADA-GENERICA-001 … 003 y RUI-RECHAZADA-GENERICA-001 y 002 | vistas de `ACEPTADA` y `RECHAZADA`. La **002** de `ACEPTADA` es el `<help>` «La anulación de la matrícula surte efecto desde esta fecha» **dentro** del panel `presentacion-descarga-aceptada`, junto a `fechaHoraPresentacion`; ese panel solo lo incluye `ACEPTADA` |

Reglas duras respetadas: ningún `required="true"` en el `domains.xml`; ninguna validación de datos de usuario en un `trigger*`; ninguna lógica de negocio en el validador; ninguna inicialización del expediente en un `PhaseEventManagerImpl`.

## 12. Asignación de perfiles

### 12.1 Tabla

| perfil | actor | tipo de actor | vía | bloque de `permisos-demo.xml` |
|---|---|---|---|---|
| `CREADOR` | `ALUMNO` | `TipoUsuario` | `tramiteCode="AnulacionMatriculaCicloFormativo"` | `<asignacionesTipoUsuario>` |
| `SECRETARIO` | `ADMINISTRATIVO` | `TipoUsuario` | `tramiteCode="AnulacionMatriculaCicloFormativo"` | `<asignacionesTipoUsuario>` |
| `RESPONSABLE` | `SUPERVISOR` | `TipoUsuario` | `tramiteCode="AnulacionMatriculaCicloFormativo"` | `<asignacionesTipoUsuario>` |
| `DIRECTOR` | `DIRECTOR` | `Cargo` | `tipoExpedienteCode="AnulacionMatriculaCicloFormativoV1"` | `<asignacionesCargoTipoExpediente>` |
| `RESPONSABLE` | `SECRETARIO` | `Cargo` | `tipoExpedienteCode="AnulacionMatriculaCicloFormativoV1"` | `<asignacionesCargoTipoExpediente>` |
| `RESPONSABLE` | `VICESECRETARIO` | `Cargo` | `tipoExpedienteCode="AnulacionMatriculaCicloFormativoV1"` | `<asignacionesCargoTipoExpediente>` |

- El perfil del estado **inicial** (`CREADOR`) va por `tramiteCode`, como exige el motor: al crear todavía no hay expediente y el `Tramitador` contrasta el perfil contra los `Ace` **sobre el trámite**.
- `SECRETARIO` va también por `tramiteCode` (sobrevive a las versiones). `DIRECTOR`, `SECRETARIO` (cargo) y `VICESECRETARIO` van por `tipoExpedienteCode` porque **no existe** bloque de cargo por trámite: hay que repetir esas filas en cada versión nueva.
- El perfil `SECRETARIO` del trámite lo ostenta el **tipo de usuario** `ADMINISTRATIVO`, no el cargo `SECRETARIO` del centro: son cosas distintas y en `cargos.xml` no hay ningún cargo de administrativo.
- `RESPONSABLE` es el perfil de **consulta**: lo ostentan el secretario y el vicesecretario del centro (por cargo) y el supervisor (por tipo de usuario). Ningún estado abierto tiene formulario para él, así que siempre cae en la vista genérica de solo lectura; y como no coincide con el `profile` de ningún estado con eventos, `checkPerfilDelEstado` le impide disparar nada.
- El **exalumno** no tiene ninguna asignación, así que no puede crear expedientes (ESC-023); sigue viendo los que creó siendo alumno gracias al permiso `Expediente.creador` de `auth-expedientes.xml`, cuya condición es `self.usuarioRegistrador = ?` y **no** depende del tipo de usuario. **MUST** comprobarse en runtime que ese acceso sigue funcionando tras dejar de ser `ALUMNO`.
- El **administrador** no necesita `Ace`: `Tramitador.checkPerfilDelEstado` le exime (`subsystem/expedientes/services/tramitacion/Tramitador.java` línea 232, `SecurityUtil.isAdmin`) y los permisos no le aplican. **Esa exención es global —vale para los tres estados con eventos, no solo para los dos del `CREADOR`—, y por eso los ocho eventos del tipo llevan guarda de identidad en el servidor** (§11, «Las ocho guardas de identidad»): sin la de perfil, el administrador podría revisar, subsanar, devolver y firmar sobre los expedientes de su centro activo, que es justo lo que HU-010/ESC-025 le niegan. **Pero sí necesita un centro activo**: `ExpedienteController.getEventContext` llama a `getCentroFromCurrentUser()`, que **lanza** si `centroActivo` es nulo, así que sin él no puede abrir **ningún** expediente; por eso el Paso 15 le da una fila `<centroUsuario>` en `usuarios-demo.xml`. Ver §14 nota 13.
- **CRITICAL — la vista que se le resuelve depende de la bandeja por la que entre, y MUST NOT afirmarse que «ninguna vista de perfil se resuelve para él» ni que «cae siempre en la genérica de solo lectura».** Por las **tres** bandejas genéricas de **consulta** sí cae en la genérica: «Expedientes Esperando» (`src/main/java/com/educaflow/tramites/views/Abierto-Expediente.xml` línea 10), «Expedientes Cerrados» (`Cerrado-Expediente.xml` línea 9) y la búsqueda (`Expediente-search.xml` línea 23) fijan `_profile=RESPONSABLE`, y aunque `ACEPTADA` y `RECHAZADA` declaren ese perfil en el `TipoExpedienteInstance.xml` (que es lo que hace pasar a `checkProfileDelTipoExpediente`, §14 nota 8(b)), **ninguna fase escribe un `<form … profile="RESPONSABLE">`**, así que `getViewName` cae siempre en el form sin perfil. Es por ahí por donde consulta (ESC-025, T-038). **Pero «Expedientes Pendientes» fija `_profile=CREADOR`** (`src/main/java/com/educaflow/tramites/views/Expediente-pendiente.xml` línea 10: `<context name="_profile" expr="CREADOR"/>`), y `CREADOR` **sí** tiene form en `DATOS_SOLICITUD` y en `PENDIENTE_FIRMA` (`fases/solicitud/views.xml`): `ExpedienteController.getEventContext` solo comprueba con `checkProfileDelTipoExpediente` (línea 239) que el perfil lo **use algún estado del tipo**, nunca que el usuario lo **ostente**, y `PhaseEventManager.getViewName` (`subsystem/expedientes/services/eventmanager/PhaseEventManager.java`) devuelve el form del perfil recibido en cuanto existe. Por esa bandeja, por tanto, en esos dos estados se abre la pantalla **editable** del creador —con «Siguiente»/«Borrar el expediente» o «Atrás»/«Firmar y presentar la solicitud»— a **cualquiera que pueda leer el expediente**: el administrador y también el secretario, el vicesecretario o el supervisor, que leen por `Expediente.porTipoExpediente` / `Expediente.porTramite` (`subsystem/expedientes/data-init/input/auth-expedientes.xml`). La diferencia del administrador sería que además podría **disparar** desde ella, porque `checkPerfilDelEstado` le exime; por eso los **cuatro** eventos de esos dos estados —`CONTINUAR`, `DELETE`, `VOLVER` y `PRESENTAR`— llevan la guarda `ControlDeAcceso.exigeSerElCreador` (§9.1 y §11, «Las cuatro guardas de autoría»). Con las cuatro puestas, **nadie que no sea el creador puede actuar** sobre la solicitud, administrador incluido: lo que queda abierto es solo la **exhibición** de la pantalla editable. **Y como la exención del motor no se acaba en esos dos estados**, los otros cuatro eventos —`ENVIAR_A_FIRMA`, `SUBSANAR`, `FIRMAR` y `DEVOLVER`— llevan además la guarda de perfil `ControlDeAcceso.exigeOstentarElPerfilDelEstado` (§9.2, §9.3 y §11, «Las ocho guardas de identidad»), que exige el perfil que su `<state>` declara —se lo pregunta a `States`, no lo repite— y sin la cual la guarda de centro le habría dejado paso en su propio centro.
- **Qué se hace con eso: cerrar la acción en el servidor y acotar la exhibición.** Se hacen dos cosas distintas y **MUST NOT** confundirse. **(1) Actuar: cerrado, y en los ocho eventos del tipo.** Los cuatro eventos de `DATOS_SOLICITUD` y `PENDIENTE_FIRMA` llevan la guarda `ControlDeAcceso.exigeSerElCreador` en la primera línea de su `trigger*`, así que ni el administrador ni nadie que no sea el creador puede disparar `CONTINUAR`, `DELETE`, `VOLVER` ni `PRESENTAR` sobre la solicitud de otro; y los cuatro de `PENDIENTE_REVISION` y `PENDIENTE_FIRMA_DIRECTOR` llevan, junto a la guarda de centro, la guarda de perfil `ControlDeAcceso.exigeOstentarElPerfilDelEstado`, así que tampoco el administrador puede `ENVIAR_A_FIRMA`, `SUBSANAR`, `FIRMAR` ni `DEVOLVER` sobre un expediente de su centro activo. Es defensa de servidor, no de vista. **(2) Que la pantalla editable se pinte: solo acotado.** El Paso 16.0 no lo resuelve —vacía las dos bandejas **nuevas**, donde el administrador no tiene `Ace`, y no toca la genérica— y el filtro de centro tampoco lo cierra: el Paso 16.3(a) añade a «Expedientes Pendientes» el filtro de centro **sin** excepción para el administrador, de modo que esa pantalla solo se le pinta para los expedientes de **su centro activo**. Que dentro de ese centro se siga pintando —a él y a todo el que pueda leer el expediente— es la parte **no mitigada**, declarada en §14 nota 8(d); cerrarla de verdad es la solución de fondo del motor (que la bandeja pase el perfil real del usuario). Lo que ya **no** queda abierto es que nadie distinto del creador **modifique** la solicitud.
- La `<permission name="AnulacionMatriculaCicloFormativoV1.all">` la genera el build en el `auth-<Code>.xml` del data-init del tipo: el diseño **MUST NOT** escribirla en ningún `auth-*.xml`. Se genera con `create/read/write/remove` **sin `condition`**: es el agujero conocido que documenta `CLAUDE.md`, y este diseño **no** intenta taparlo por su cuenta.

### 12.2 Bandejas

La vista que ve cada actor la elige la pareja `(estado, _profile)`, y el `_profile` **lo fija el `action-view` de la bandeja**, no el usuario. Las bandejas existentes solo pasan `CREADOR` (Trámites y «Expedientes Pendientes») y `RESPONSABLE` («Expedientes Esperando», «Expedientes Cerrados» y la búsqueda), así que este trámite necesita dos más:

| Bandeja (nueva) | `_profile` | Dominio | Quién la usa |
|---|---|---|---|
| «Anulaciones de matrícula de mi centro» (`subsysExpedientes.Expediente@Revision-action`) | `SECRETARIO` | `self.id IN (:expedientesConPerfil)`, donde la lista la calcula el punto de consulta del Paso 16.0: abiertos **de este trámite**, en el centro activo y **sobre los que el usuario ostenta el perfil `SECRETARIO`** | la administrativa |
| «Anulaciones de matrícula pendientes de mi firma» (`subsysExpedientes.Expediente@Firma-action`) | `DIRECTOR` | lo mismo con el perfil `DIRECTOR`, **más** la condición propia de la bandeja `self.codeState = 'PENDIENTE_FIRMA_DIRECTOR'` | el director |

La del director va además acotada a `self.codeState = 'PENDIENTE_FIRMA_DIRECTOR'`, que es lo que su título anuncia y el único estado con form para `DIRECTOR`. Las dos van **acotadas al trámite** (`self.tipoExpediente.tramite.code = 'AnulacionMatriculaCicloFormativo'`) y llevan el trámite en el título, porque su `_profile` es un perfil que otros tipos de expediente pueden no declarar: listar expedientes ajenos haría reventar al abrirlos (§14 nota 8(c)). El Paso 16 las da escritas enteras.

**Las dos van acotadas además a quien OSTENTA el perfil que fijan** (la condición del Paso 16.0: `self.id IN (:expedientesConPerfil)`, alimentada por el punto de consulta que **pregunta** al dueño de esa pregunta en el servidor). Sin ella, cualquiera con lectura sobre el expediente —el propio alumno por `Expediente.creador`, el secretario o el vicesecretario por `Expediente.porTipoExpediente`, el supervisor por `Expediente.porTramite`— abriría por esas bandejas la pantalla **editable** del `SECRETARIO` o la del `DIRECTOR` y vería la decisión de secretaría antes de que el director firme, contra RUI-PENDIENTE_REVISION-GENERICA-001, RUI-PENDIENTE_FIRMA_DIRECTOR-GENERICA-001, ESC-008, ESC-009, ESC-020, ESC-024 y ESC-029: `ExpedienteController` comprueba que el perfil lo **use** el tipo, nunca que el usuario lo **tenga**. Restringir en cambio el `menuitem` a un grupo propio **no es una opción**: `VAR-10.1` fija el `groups` de todo menú a `admins`, `admins,users` o `users` (Paso 16.4). El **dueño** de la pregunta «¿ostenta este usuario el perfil P sobre este expediente?» es `PerfilesUsuarioService.getPerfilesSobreExpediente` en el servidor, que es lo que `Tramitador.checkPerfilDelEstado` usa para autorizar cada evento, y las dos bandejas **le preguntan**: su `<domain>` es `self.id IN (:expedientesConPerfil)` y la lista la calcula `tramites/util/bandeja/BandejaPorPerfilController` llamando a ese servicio (Paso 16.0). **MUST NOT** reescribirse esa lógica en JPQL dentro de las vistas. Es **interfaz y no defensa**, y la pieza entera —`<context>`, `<domain>` y controlador— desaparece el día en que se apruebe la solución de fondo de §14 nota 8 (que la bandeja pase el perfil real del usuario). **Y solo cubre el camino normal de navegación:** quien pida la vista con `_profile=SECRETARIO` o `DIRECTOR` **sin pasar por la bandeja** la sigue obteniendo, porque el `_profile` viaja en la petición y `ExpedienteController` nunca comprueba que el usuario **ostente** ese perfil para elegir la vista. Por eso las dos `RUI-*-GENERICA-*` de §11 **no** quedan garantizadas por esta condición y el residuo está declarado en §14 nota 8(d).

Y las **cuatro** bandejas genéricas existentes de `tramites/views/` añaden el **filtro por centro** (ESC-021, ESC-024, ESC-025): «Expedientes Pendientes» (`Expediente-pendiente.xml`) y «Expedientes Cerrados» (`Cerrado-Expediente.xml`) en su `<domain>`, y «Expedientes Esperando» (`Abierto-Expediente.xml`) y la **búsqueda de expedientes** (`Expediente-search.xml`) en los `domain` de sus dos `<node>`, que es donde filtran. La búsqueda entra en la lista por derecho propio: fija `_profile=RESPONSABLE` y hoy no filtra por centro, así que es otra lista por la que un usuario de otro centro vería el expediente ajeno, y ESC-021 exige que no aparezca **en ninguna**. Ese filtro es **interfaz, no defensa**: la condición de servidor que de verdad aislaría los centros vive en `auth-expedientes.xml`, que es del motor — ver §14 nota 9.

**La excepción del administrador va en TRES de esas cuatro, no en las cuatro** (Paso 16.3). La llevan las tres de **consulta** —«Expedientes Esperando», «Expedientes Cerrados» y la búsqueda—, que fijan `_profile=RESPONSABLE` y por las que, al no existir ningún `<form … profile="RESPONSABLE">` en este tipo, el expediente se le abre siempre en la vista genérica de solo lectura: es exactamente lo que ESC-025 pide y lo que comprueba T-038. **MUST NOT** llevarla «Expedientes Pendientes», que fija `_profile=CREADOR` (`Expediente-pendiente.xml` línea 10) — un perfil que **sí** tiene form en `DATOS_SOLICITUD` y en `PENDIENTE_FIRMA`—, porque `ExpedienteController` solo comprueba que el perfil lo **use** el tipo y `PhaseEventManager.getViewName` (`subsystem/expedientes/services/eventmanager/PhaseEventManager.java`) devuelve entonces el form del `CREADOR`: exceptuar ahí al administrador le **pintaría** la pantalla editable del creador de **cualquier centro**. Actuar desde ella no puede: los cuatro eventos de esos dos estados llevan la guarda `ControlDeAcceso.exigeSerElCreador` (§9.1 y §11), que es lo único que le para, porque `checkPerfilDelEstado` le exime (`Tramitador.java` línea 232) — y lo mismo hacen, con la guarda de perfil, los cuatro eventos de `REVISION` y `RESOLUCION`. Con el filtro sin excepción, la exhibición queda acotada a los expedientes de su centro activo; ningún escenario pierde nada, porque el administrador consulta por las tres de arriba.

**Y esa pantalla editable se le pinta igualmente dentro de su centro, y a todo el que pueda leer el expediente** —el secretario, el vicesecretario y el supervisor por `Expediente.porTipoExpediente` / `porTramite`—: el filtro de centro solo **acota** el problema, no lo cierra. Lo que sí está cerrado es **actuar**, y lo está en **los ocho eventos del tipo**: a esos tres les rechaza el evento `checkPerfilDelEstado` por no ostentar el perfil del estado, y al administrador —al que el motor exime en **todos** los estados— le rechazan los cuatro eventos de `CREADOR` las guardas de autoría de §9.1 y los cuatro de `SECRETARIO`/`DIRECTOR` las guardas de perfil de §9.2 y §9.3. Queda abierta **solo la exhibición**, que es la parte **no mitigada** de §14 nota 8(d) y **MUST NOT** darse por resuelta: la solución de fondo es del motor.

### 12.3 Fragmento

El fragmento a fusionar está materializado en `design/permisos.xml`: declara solo los perfiles `SECRETARIO` y `DIRECTOR` (que **no** existen todavía en `permisos-demo.xml`) y las seis asignaciones de §12.1. `CREADOR` y `RESPONSABLE` ya están declarados y **MUST NOT** duplicarse.

## 13. Tests

- **E2E:** `design/test-e2e-desc.md` — 56 tests en Given/When/Then, con la tabla de actores y credenciales reales de la demo, un juego de datos válido por fase, el **reparto de la tripleta (alumno, ciclo, curso académico)** que aísla unos tests de otros sobre la base de datos compartida —con los ocho ciclos que ese reparto necesita declarados como **configuración del entorno de pruebas**, no como alta en el catálogo maestro (§4, §14 nota 23 y `decisiones.md` D9)—, la tabla de cobertura de las diez transiciones, un test de validación fallida por cada pareja (estado, evento) con reglas, la vista genérica de solo lectura de todos los estados y los cinco tests de aislamiento (otro centro, familiar, otro alumno, exalumno y —T-056— quien puede leer el expediente pero no ostenta el perfil `SECRETARIO`/`DIRECTOR` de las dos bandejas nuevas). La pareja (`PENDIENTE_FIRMA`, `PRESENTAR`) tiene **dos ramas complementarias** en el validador y las dos están cubiertas: la de AutoFirma__!! con T-019 y T-020 (marcados `Manual: sí`, porque exigen firmar en el equipo del alumno con la aplicación de firma del ciudadano) y la de **firma en servidor** con T-055 (`Manual: no`, contraseña del certificado custodiado incorrecta), que es la única automatizable de esa pareja.
- **Unitarios:** `design/test-unit-desc.md` — lo escribe el rol `test-unitarios` del motor, no el diseñador.
- **No-regresión de una suite E2E ajena ya persistida:** `src/test/e2e/subsystem/sistemaeducativo/` (**16** ficheros `.spec.ts`: `t-001`…`t-015` más `crear-ley-educativa.spec.ts`, que el comando también ejecuta). El Paso 4 toca el catálogo que esos tests pilotan (el alta de `IABD`, el `nombreCorto` de los niveles, `Main-Nivel.xml` y `Ref-Ciclo.xml`), y `./run.sh` **no** ejecuta Playwright, así que la reejecución (`npx playwright test src/test/e2e/subsystem/sistemaeducativo`) es una comprobación explícita del Paso 4 y del Paso 17. **MUST NOT** editarse ninguno de esos ficheros; la prosa de sus `.desc.md` que el alta de `IABD` deja desactualizada se declara en §14 nota 22.

Los invariantes de forma de este tipo (que cada fase tiene su `PhaseEventManagerImpl` y su `StateEventValidatorImpl` con exactamente los métodos que su XML declara, que la clase `States` concuerda con el XML, que cada estado tiene sus formularios y cada evento su botón, y que el `.puml` dibuja exactamente los estados del XML) los cubren los tests ya existentes de `src/test/java/com/educaflow/tiposexpedientes/`, que recorren **todos** los tipos del árbol. **MUST NOT** proponerse tocarlos ni ampliarlos.

## 14. Notas y supuestos

1. **El trámite ya existe a medias en el árbol.** `TramiteInstance.xml` (con la errata «permite a ja un alumno») y un `TipoExpedienteInstance.xml` con `<fases>` vacío ya están versionados, igual que `documentospdf/modelo.pdf`. Por eso esas tres filas de la tabla §6 van con acción `Modificar`, y no es una iniciativa de modificación de una versión existente: la versión no tiene todavía ni modelo, ni clases, ni vistas, ni expedientes vivos.

2. **CC-002 no se puede satisfacer hoy.** La especificación y las guías piden precargar el NIA, la dirección, el teléfono, la población y el código postal «desde la ficha del alumno / la `Persona` del usuario, si constan». Al leer el modelo real **no existe ninguna relación entre `User` y `Persona`**: `User` guarda nombre, apellidos y DNI, y las filas `Persona` solo las crea cada expediente como foto de sus datos. Se asume la lectura literal de «si constan»: hoy no consta ninguno y los campos nacen vacíos. Reutilizar la `Persona` de un expediente anterior del mismo DNI se descartó expresamente (`decisiones.md` D5). Cuando exista una ficha de alumno de verdad, la precarga se añade en el `triggerInitialEvent`, que ya es donde vive el resto de la inicialización.

3. **El «lugar» de la solicitud no es un campo propio.** CC-005 pide anotar el lugar y la fecha al continuar; el lugar es siempre la localidad del centro, que ya queda congelada en `localidadCentro` al crear el expediente (CC-004). Un campo aparte con el mismo valor solo añadiría un sitio más que mantener, así que la línea de lugar y fecha del documento lee `self.localidadCentro` y solo `fechaSolicitud` se reescribe en cada `CONTINUAR`.

4. **Cada documento imprime exactamente los datos que su ficha declara, y por eso no hay fragmento compartido.** La **solicitud** abre con el bloque A completo del impreso oficial: apellidos, nombre, NIA, DNI/NIE, dirección, teléfono, población, provincia y CP, los nueve en una sola sección con el rótulo «Datos de identificación del alumno/a», tal como los lista `documentos.md`. La **resolución** abre con la identificación del destinatario y **solo** con los cuatro datos que su propia ficha lista: apellidos, nombre, documento de identidad y NIA; **no** imprime dirección, teléfono, población, provincia ni CP. Se descartó el fragmento compartido `_template.xml` que llevaba el bloque entero: hacía que la resolución imprimiera cinco datos que su ficha no declara, y partirlo en dos para compartir solo la mitad habría roto el bloque A del impreso oficial en dos secciones con letra distinta (la especificación fija como texto fijo los rótulos de los bloques **A**, **B** y **C** de la solicitud). Lo que los dos documentos comparten es el concepto, no la forma, así que cada uno declara su propia `<seccion>` y la única duplicación son cuatro nombres de campo.

   **La «referencia de la matrícula» tampoco se factoriza, y esta vez el motivo es el mecanismo.** `documentos.md` la declara como tercer trozo común, y es cierto que la línea «en el Cicle Formatiu de Grau `${self.ciclo.gradoNivel}` denominat `${self.ciclo.name}`» / «en el Ciclo Formativo de Grado … denominado …» es **byte a byte idéntica** en los dos ficheros y en los dos idiomas. Pero un fragmento (`_*.xml`) **solo puede incluirse como hijo directo de `<documento>`**: `<include>` **MUST NOT** ir dentro de una `<seccion>` (`k-tipo-expediente/documentos.md` §2.5), y esa línea es un `<texto>` **dentro** de una sección que en cada documento es distinta y lleva alrededor texto distinto — el bloque B «Expone» de la solicitud (donde va detrás del curso, el centro y la localidad) y la sección «Vistos» de la resolución (donde va detrás de la fecha de presentación). Factorizarla exigiría sacarla a una `<seccion>` propia en los dos documentos, lo que partiría en dos el bloque B del impreso oficial —cuyos rótulos A, B y C fija la especificación como texto fijo— y añadiría a la resolución una sección que su ficha no declara. Es decir: el fragmento no puede expresar lo que aquí se comparte. Se asume, por tanto, la duplicación de **una** línea, y **MUST** tocarse en los dos ficheros a la vez; queda escrito aquí para que quien la cambie lo sepa. Si algún día `<include>` admitiera trozos de sección, este es el primer sitio donde aplicarlo.

5. **La fórmula de estimación o desestimación de la resolución es bilingüe, pero se resuelve dentro de la expresión.** Depende de que la revisión rechace o no (y arrastra el rótulo «Por el siguiente motivo:» y el motivo cuando se desestima), así que **MUST** resolverse en una expresión y no como texto fijo: con dos `<check>` bilingües aparecerían las dos frases y solo cambiaría la casilla marcada, que es justo lo que los escenarios comprueban que no pase. Lo que **no** puede hacerse es dejarla en un `<campo nombreCampo="…">`, porque ese atributo es **uno solo** para los dos idiomas y la resolución saldría medio monolingüe pese a emitirse en valenciano y en castellano. Se escribe, por tanto, como **campo inline** `${expresión;n}` dentro de un `<texto>` con sus dos hijos `<valenciano>`/`<castellano>`, cada uno con **la misma expresión y los literales en su idioma**: el generador los trata como dos campos distintos (el nombre del campo es la expresión) y el traductor del build no toca los `${…}`. El único texto que no se traduce es el **motivo del rechazo**, que lo escribió la secretaría y se imprime tal cual.

   **Y la fórmula pregunta al mismo dueño que el motivo:** la condición de las dos expresiones es `com.educaflow.tramites.alumnos.anulacion_matricula_ciclo_formativo.v1.ReglasAnulacionMatricula.esRechazo(self)` —`${<FQCN>.esRechazo(self) ? "…desestima…" : "…estima…"}` y `${<FQCN>.esRechazo(self) ? "…motivo…" : ""}`—, no una comparación contra `SentidoRevisionAnulacionMatriculaCicloFormativoV1`. Las dos proyectan la **misma** clasificación, cuyo dueño único en servidor es la función estática `ReglasAnulacionMatricula.esRechazo(…)` de la raíz de la versión (§9.0.3, §4, §11 y `decisiones.md` D10), así que **MUST NOT** volver a aparecer en este documento ninguna comparación contra el enum —y menos aún `== ACEPTAR`, que además dejaría la rama `else` absorbiendo `SUBSANAR` y el sentido vacío e imprimiendo «Se desestima la solicitud» por valores que nadie ha decidido—. **Se escribe con el FQCN entero y NO como `self.esRechazo()`**, por dos motivos: en un `${…}` no hay `import`, y el predicado **no** es un método de la entidad —no puede serlo, porque el único sitio de la entidad donde cabría escribirlo es el `<extra-code-model>`, que el build reescribe entero (§9.0.3)—. Formulada alrededor de `esRechazo(…)`, la rama `else` es exactamente el complemento de una decisión declarada, y un cuarto sentido de revisión no puede cambiar el significado del documento sin pasar antes por su dueño.

6. **El mensaje de firma inválida es el literal que fija la especificación.** VAL-PENDIENTE_FIRMA-PRESENTAR-002 lo fija como «La firma no es válida o no corresponde a su documento de identidad» y ESC-028 lo comprueba palabra por palabra, así que `FirmaPdf` recibe ese texto como segundo argumento (Paso 5 y §10.1) en lugar del motivo concreto que calcula `DocumentoPdfUtil.validateFirmaPdf`. Es el mismo patrón que `decisiones.md` D3 eligió para `Required`, `Pattern`, `MinLength` y `MaxLength` —el mensaje es un argumento más de la regla—, aplicado a una cuarta regla del mismo catálogo. El mensaje de la rama «su usuario no tiene un documento de identidad válido» **no** se toca: habla de un problema de la cuenta, no de la firma.

7. **Los textos nuevos pasan por el traductor automático del build.** Los mensajes de validación, los títulos de campo y los textos de los documentos se traducen al valenciano en el build; si `apertium` no sabe traducir alguna palabra el build falla y se arregla marcándola con `__!!` o escribiendo el valenciano a mano. **MUST NOT** crearse ningún `i18n_*.csv` a mano.

8. **DECISIÓN DE ARQUITECTURA PENDIENTE DE APROBAR — el `_profile` lo fija la bandeja, no el usuario.** `ExpedienteController` exige un `_profile` en la petición, lo toma literalmente del `action-view` por el que se entró y **lanza una excepción si ese perfil no lo usa ningún estado del tipo**. Consecuencia: (a) cada perfil nuevo necesita su propia bandeja, y este diseño añade dos en `tramites/views/`; (b) todo tipo de expediente está obligado a declarar `RESPONSABLE` en algún estado para que las bandejas genéricas no revienten al abrir sus expedientes; (c) una bandeja de un perfil lista expedientes de tipos que quizá no lo usen, y al abrirlos falla; (d) **una bandeja abre con el perfil que fija a cualquiera que pueda leer el expediente, tenga o no ese perfil** — `ExpedienteController.getEventContext` solo comprueba con `checkProfileDelTipoExpediente` que el perfil lo **use algún estado del tipo**, nunca que el usuario lo **ostente**, y `PhaseEventManager.getViewName` (`subsystem/expedientes/services/eventmanager/PhaseEventManager.java`) resuelve entonces el form de ese perfil. **Mitigaciones aplicadas:** para (c), las dos bandejas nuevas se acotan al trámite `AnulacionMatriculaCicloFormativo` —el segundo argumento de la llamada del Paso 16.0—, así que solo listan expedientes cuyo tipo declara `SECRETARIO`/`DIRECTOR` y nunca se llega a abrir uno ajeno con un perfil que no usa; para (d), esas dos bandejas llevan además la **condición de perfil** del Paso 16.0 —`self.id IN (:expedientesConPerfil)`, con la lista calculada por `tramites/util/bandeja/BandejaPorPerfilController`, que **pregunta** al dueño de la pregunta (`PerfilesUsuarioService.getPerfilesSobreExpediente`) en vez de reescribir su JPQL en la vista—, sin la cual el propio alumno (por `Expediente.creador`), el secretario o el vicesecretario (por `Expediente.porTipoExpediente`) y el supervisor (por `Expediente.porTramite`) abrirían la pantalla editable del `SECRETARIO` o la del `DIRECTOR` y verían la decisión de secretaría antes de la firma, contra RUI-PENDIENTE_REVISION-GENERICA-001, RUI-PENDIENTE_FIRMA_DIRECTOR-GENERICA-001, ESC-008, ESC-009, ESC-020, ESC-024 y ESC-029. Ocultar el menú **no** era una alternativa: `VAR-10.1` fija el `groups` de todo `menuitem` a `admins`, `admins,users` o `users` (Paso 16.4). El **dueño** de la pregunta que esa condición proyecta sigue siendo `PerfilesUsuarioService.getPerfilesSobreExpediente` en el servidor —lo que `Tramitador.checkPerfilDelEstado` ya usa para autorizar cada evento—, así que la condición del `<domain>` es **interfaz, no defensa**: impide la **exhibición** de la pantalla ajena, mientras que **actuar** sin el perfil ya lo bloquea el motor — salvo al **administrador**, al que `checkPerfilDelEstado` exime en **cualquier** estado (`Tramitador.java` línea 232), y para él lo bloquean las **ocho** guardas de identidad de los `trigger*`: `exigeSerElCreador` en los cuatro eventos de los estados `CREADOR` y `exigeOstentarElPerfilDelEstado` en los cuatro de los estados `SECRETARIO`/`DIRECTOR` (§11, «Las ocho guardas de identidad», y `decisiones.md` D13). Es decir: **actuar está cerrado para todos, administrador incluido, en los ocho eventos del tipo**; lo que sigue abierto es solo la exhibición, y de eso trata el resto de esta nota. **Qué queda mitigado de (d) y qué NO — MUST NOT confundirse.** Mitigado **solo el camino normal de navegación**: las **dos bandejas nuevas** de este trámite (`SECRETARIO` y `DIRECTOR`), que con la condición de perfil no **listan** el expediente a quien no ostenta ese perfil (T-056). **Lo que esa condición NO hace es elegir la vista**, y por eso **MUST NOT** darse por garantizada con ella ninguna `RUI-*-GENERICA-*` (§11): el `_profile` **viaja en la petición**, `ExpedienteController.viewExpediente` lo toma del contexto (`actionRequestHelper.getProfileName()`) y solo lo contrasta con `checkProfileDelTipoExpediente`, que comprueba que el perfil lo **use** algún estado del tipo, nunca que el usuario lo **ostente**; `PhaseEventManager.getViewName` devuelve entonces el form de ese perfil. Así que cualquiera con lectura sobre el expediente —el alumno por `Expediente.creador`; el secretario, el vicesecretario o el supervisor por `Expediente.porTipoExpediente`/`porTramite`— puede invocar `subsysExpedientes-event-view-action` con `_profile=SECRETARIO` o `DIRECTOR` **sin pasar por la bandeja** y obtener la pantalla editable con la decisión de secretaría. Eso **queda abierto**, es la misma raíz que el resto de (d) y **MUST** decidirse como parte de la solución de fondo del motor descrita al final de esta nota —que la vista se resuelva con el **perfil real del usuario**, el que `PerfilesUsuarioService.getPerfilesSobreExpediente` ya sabe calcular—, no como una mitigación aplicada por este trámite. **NO mitigado —y solo en un sentido—: la EXHIBICIÓN por la bandeja genérica «Expedientes Pendientes»**, que fija `_profile=CREADOR` (`src/main/java/com/educaflow/tramites/views/Expediente-pendiente.xml` línea 10) y que en `DATOS_SOLICITUD` y en `PENDIENTE_FIRMA` —los dos estados con `<form … profile="CREADOR">`— **pinta** el formulario editable del creador a **cualquiera que pueda leer el expediente** aunque no lo haya creado: el administrador, el secretario, el vicesecretario y el supervisor. **Actuar, en cambio, sí está cerrado**: a los tres últimos les rechaza el evento `checkPerfilDelEstado` por no ostentar `CREADOR`, y al **administrador** —al que `Tramitador.java` línea 232 exime— le rechazan los **cuatro** eventos de esos dos estados las guardas `ControlDeAcceso.exigeSerElCreador` de `triggerContinuar`, `triggerDelete`, `triggerVolver` y `triggerPresentar` (§9.1 y §11, «Las cuatro guardas de autoría»). Esas dos últimas las **añade este diseño** precisamente porque el motor exime al administrador, anulando expresamente la indicación en contrario de `design-guidelines.md` (§14 nota 25). **Y lo mismo, con la guarda de perfil, en los otros dos estados**: `ENVIAR_A_FIRMA`, `SUBSANAR`, `FIRMAR` y `DEVOLVER` llevan `ControlDeAcceso.exigeOstentarElPerfilDelEstado` —el perfil se lo pregunta al `<state>`, no lo repite—, sin la cual la exención del motor le habría dejado tramitar —y firmar— cualquier expediente de su centro activo (§9.2, §9.3, §11 y `decisiones.md` D13). Lo que queda abierto es solo que la pantalla **se pinte**, y sobre eso lo único que se hace aquí es **acotar el alcance**, no cerrarlo: el Paso 16.3 (a) deja esa bandeja **sin** la excepción `esAdministrador`, así que al administrador solo se le abre esa pantalla para los expedientes de **su centro activo**. Esa bandeja es **compartida** (`src/main/java/com/educaflow/tramites/views/`) y la usan todos los tipos de expediente, así que **MUST NOT** parchearse desde este trámite quitándole el `_profile` ni añadiéndole condiciones propias de un trámite (el Paso 16.3 solo le añade el filtro de centro, que vale para todos): se cierra con la misma solución de fondo —que la bandeja pase el perfil real del usuario sobre cada expediente—. **MUST NOT** darse por protegido este trámite porque las dos bandejas nuevas sí lo estén. Las dos son mitigaciones **por trámite**, no la solución: cada trámite nuevo con perfiles propios tendrá que repetirlas, y seguirá haciéndolo mientras el motor no pase el perfil real del usuario sobre cada expediente. La solución de fondo es que la bandeja pase el **perfil real del usuario sobre cada expediente** —lo que ya sabe calcular `PerfilesUsuarioServiceImpl.getPerfilesSobreExpediente`—, y eso es una capacidad del **motor** (`subsystem/expedientes`); el día en que se apruebe, la condición de perfil del Paso 16.0 **desaparece** de las dos bandejas. **Este diseño no la aplica**: queda escrita aquí para que se decida aparte.

9. **DECISIÓN DE ARQUITECTURA PENDIENTE DE APROBAR — el aislamiento multicentro de la lectura no está en el servidor.** Los `Ace` que el data-init crea a partir de un tipo de usuario o de un cargo llevan `centro` a nulo, y las condiciones `Expediente.porTramite` y `Expediente.porTipoExpediente` de `subsystem/expedientes/data-init/input/auth-expedientes.xml` **no filtran por el centro del expediente**: conceden lectura sobre todos los expedientes del trámite a cualquiera que tenga ese tipo de usuario o ese cargo en cualquier centro. Este diseño cubre los escenarios de la especificación filtrando por centro en el `<domain>` de **todas** las bandejas (§12.2) y bloqueando en el servidor cualquier **acción** sobre un expediente de otro centro (las guardas de §9), pero el filtro de la bandeja es **interfaz, no defensa**. El arreglo real —añadir `AND self.centro = ?` a esas condiciones— vive en el motor y **MUST** decidirse junto con el agujero ya documentado en `CLAUDE.md` sobre el `auth-<Code>.xml` que el build genera sin `condition`.

   Y **no hay arreglo posible desde el trámite**: `AuthSecurity` combina con **OR** los filtros de todas las permissions aplicables (`Filter.or(filters)`) y una permission **sin** `condition` no aporta ninguna restricción (`filters.isEmpty()` ⇒ sin filtro), así que un permiso nuevo solo puede **ampliar** el acceso, nunca restarlo; y el `auth-<Code>.xml` que el build genera por cada tipo de expediente, **sin `condition`**, ya concede `read` sobre todas las filas. Declarar en este trámite un permiso propio **con** condición de centro no quitaría nada: sería trabajo perdido y, peor, daría la falsa impresión de haber cerrado el agujero. Por eso la única corrección real es añadir `AND self.centro = ?` a las condiciones de `Expediente.*` de `auth-expedientes.xml`, en el motor.

10. **Verificación pendiente del campo punteado `ciclo.gradoNivel`.** El precedente del proyecto (`Ref@Ciclo-form` usa `grado.admiteNivel`, transient y calculado, con `depends`) hace esperar que funcione, pero que se **refresque al cambiar el ciclo sin guardar** solo se puede confirmar en runtime: está incluido en el paso 17. Si no se refrescara, el arreglo **MUST** seguir teniendo un único dueño (el catálogo), nunca duplicar el cálculo en el trámite.

11. **Certificados de demo para automatizar la firma.** Ningún alumno de la demo tiene hoy un `CertificadoDigital` dado de alta, así que su situación de firma es `SIN_CERTIFICADO` y la presentación exige la aplicación de firma del ciudadano. Para que la suite E2E sea ejecutable, el entorno de pruebas **MUST** dejar los certificados custodiados (pantalla de mantenimiento de certificados digitales) en **tres** situaciones distintas, porque el validador de (`PENDIENTE_FIRMA`, `PRESENTAR`) tiene dos ramas y la de servidor se subdivide según haga falta clave o no:

    - `alumno1@mislata.es`, `alumno5@mislata.es`, `alumno6@mislata.es` y `alumno1@batoi.es` — certificado custodiado **con** la contraseña guardada: firman en el servidor sin que la pantalla les pida nada. `alumno1@mislata.es` es el alumno del camino feliz y `alumno1@batoi.es` el del otro centro; los de `alumno5` y `alumno6` (documentos «69807058B» y «80364400P») son **nuevos** y hacen falta porque el reparto de parejas (alumno, ciclo) de `test-e2e-desc.md` necesita **tres** alumnos de CIPFP Mislata capaces de presentar sin intervención: con uno solo no hay ciclos suficientes en el catálogo para que los tests no se pisen.
    - `alumno2@mislata.es` — certificado custodiado **sin** la contraseña guardada: la pantalla le pide la «Contraseña», que es el único modo de probar en el navegador una clave incorrecta (T-055, la única validación fallida automatizable de esa pareja).
    - `alumno3@mislata.es` y `alumno4@mislata.es` — **sin** certificado custodiado: firma con la aplicación de firma del ciudadano. El primero es el actor de T-019 y T-020 (los dos `Manual: sí`) y, además, el de los tests que se quedan en la fase `SOLICITUD` sin llegar a presentar, que no necesitan certificado; del segundo solo se usa su documento de identidad, como el del certificado ajeno con el que se firma en T-020.

    Es configuración del entorno, no un fichero del proyecto; la tabla exacta está en la «Precondición de firma» de `test-e2e-desc.md`. **Los ocho ciclos que necesita el reparto de parejas (alumno, ciclo) son configuración del entorno por el mismo motivo y de la misma forma** (§4 y nota 23): se dan de alta por la pantalla «Sistema educativo → Ciclos» antes de lanzar la suite, no por el data-init del catálogo.

12. **Los recuadros de firma están MEDIDOS sobre el PDF generado, no estimados.** Se renderizaron los dos documentos con el generador real (`Xml2Pdf`) y se midieron las celdas de la maquetación; estas son las medidas, en puntos, con el origen abajo a la izquierda y el marco del documento entre `x 28,35` y `x 566,93`:

    - **Solicitud** — la celda «Signatura:/Firma:» de la sección «Lugar, fecha y firma» ocupa `y 415,52 → 472,22`, y sus dos rótulos están en las líneas base `y 462,70` (valenciano) y `y 454,58` (castellano). La firma se estampa debajo y a la derecha: `Rectangulo(320, 418, 240, 32)` — borde derecho `320 + 240 = 560 ≤ 566,93`, borde superior `418 + 32 = 450`, por debajo del rótulo, y borde inferior `418`, por encima de la línea de la celda (`415,52`).
    - **Resolución** — el recuadro vacío que hay bajo «El director / la directora del centro» (cuyos rótulos están en `y 384,41` y `y 376,29`, en la celda de arriba) ocupa `y 315,28 → 372,00`: `Rectangulo(150, 317, 300, 53)` cae entero dentro, con `150 + 300 = 450 ≤ 566,93` y `317 + 53 = 370 < 372,00`.

    **`DocumentoPdfImplIText.getRectangle` hace crecer el alto HACIA ARRIBA cuando el `CampoFirma` lleva imagen** (`height = mensajeHeight + imageHeight`, con `(x, y)` como esquina inferior izquierda). Hoy ninguna de las dos firmas lleva imagen —las dos construyen el `CampoFirma` solo con el rectángulo—, así que el alto es el declarado; si algún día se le pone una rúbrica, **MUST** rebajarse el `y` o el alto para que el recuadro no invada la fila de arriba.

    Se recomprueban abriendo los dos PDF en runtime (paso 17) y, en el caso de la solicitud, **MUST** cambiarse **en los dos sitios a la vez**: la constante `POSICION_FIRMA_SOLICITUD` del trigger y los ocho argumentos de la `<action-method>` `exp-AnulacionMatriculaCicloFormativoV1-firmarSolicitud-action`.

13. **El usuario `admin` no tiene centro activo y sin él no puede abrir ningún expediente.** `ExpedienteController.getEventContext` llama a `getCentroFromCurrentUser()`, que hace `throw new RuntimeException("El centro activo es null para el usuario: " + user.getName())` cuando `user.getCentroActivo()` es nulo. El usuario `admin` lo crea el bootstrap de Axelor, no la demo, y en `data-demo/input/usuarios-demo.xml` **no hay ninguna fila `<centroUsuario>` para él**: hoy ESC-025 (T-038) y cualquier consulta del administrador reventarían antes de pintar la vista. Por eso el Paso 15 añade `<centroUsuario usuarioCode="admin" centroCode="46019660"/>`; el binding de `data-demo/input-config.xml` busca el `User` por `self.code = :usuarioCode` con `create="false"`, así que reutiliza el admin existente y solo le fija el `centroActivo`. Qué centro sea es indiferente para **lo que ESC-025 le pide ver**: consulta por «Expedientes Esperando», «Expedientes Cerrados» y la búsqueda, y el `<domain>` de esas tres le exceptúa con `esAdministrador` (§12.2), así que ve los dos centros con independencia del suyo. **Sí importa, en cambio, en «Expedientes Pendientes»**, que a propósito **no** le exceptúa (Paso 16.3 (a)): ahí ve solo los expedientes del centro que esta línea le asigna, y es justo lo que acota el agujero de §14 nota 8(d). Es un supuesto del **motor** sobre todo usuario que abra un expediente, no algo de este trámite, pero se arregla aquí porque es aquí donde lo destapa un escenario.

14. **La comprobación de que el centro dispone de la firma del Director tiene que poder fallar.** `AlmacenClaveResolver.getDirector(Centro)` **ignora hoy el centro** y devuelve **siempre** un `AlmacenClaveFichero` nuevo construido sobre el recurso `/firma/instalar_certificado_criptografico/director.p12`: nunca devuelve `null`. Escribir VAL-PENDIENTE_FIRMA_DIRECTOR-FIRMAR-002 como `getDirector(...) == null` daría una condición **inalcanzable** y un mensaje que nadie vería jamás, es decir, la regla sin implementar con apariencia de implementada. Por eso §9.3 acción 2 la especifica **capturando el fallo**: `AlmacenClaveFichero` lanza `RuntimeException` si el recurso del certificado no está (`"El fileCertificate no puede ser null"`) o no se puede leer, y ese es exactamente el caso «el centro no tiene configurada la firma del Director» que hoy puede darse. La comparación con `null` se conserva **además**, para el día en que el resolver pase a depender del centro. La captura del fallo alcanza **solo** a la resolución del almacén: un fallo al firmar (acción 5) **MUST NOT** salir con este mensaje. Que el resolver deba pasar a depender del centro es una carencia del subsistema de criptografía, no de este trámite, y **MUST NOT** arreglarse aquí.

15. **La regla de duplicados falla si el centro no tiene curso académico.** `SinOtraSolicitudEnCursoParaElMismoCiclo` filtra por `cursoAcademico`, que el servidor congela al crear el expediente desde `centro.getCurso()` y que puede quedar nulo si el centro no lo tiene informado. Con `cursoAcademico` nulo la consulta no casaría con ninguna fila y la regla devolvería «válido» **en silencio** justo cuando le falta el dato con el que decidir, dejando pasar el duplicado que VAL-DATOS_SOLICITUD-CONTINUAR-017 prohíbe. Se decide que la regla **falle** con «Su centro no tiene configurado el curso académico; avise a la secretaría del centro» (§10.0). Es un mensaje que la especificación no fija, porque describe una **configuración incorrecta del centro**, no un error del alumno; con la demo cargada no aparece nunca (los dos centros tienen `curso="2024"`). La alternativa —exigir que `cursoAcademico` no pueda ser nulo al llegar a `CONTINUAR`— se descartó: haría falta una guarda más para decir lo mismo, y la regla seguiría sin poder explicarse sola.

16. **La localidad del centro ya viene del catálogo de datos maestros: no hay que tocar la demo.** `localidadCentro` sale de `centro.getMunicipio()?.getName()`, y los dos centros de la demo los siembra `src/main/java/com/educaflow/subsystem/common/data-init/input/Centro.xml`, que los declara con `municipio="46169"` (CIPFP Mislata) y `municipio="03009"` (CIPFP Batoi → «Alcoy/Alcoi»); su `<input>` en `subsystem/common/data-init/input-config.xml` lleva el bind raíz `search="self.code = :code" create="true" **update="true"**` y el hijo `<bind node="@municipio" to="municipio" search="self.code = :municipio" create="false" update="false"/>`, así que el municipio se fija —y se refresca en cada arranque— sobre el `Centro`. Además `ModuleManager.install` ejecuta `dataLoader.load(module, update)` **antes** que `demoLoader.load(module, update)`, de modo que las carpetas `data-init` se cargan antes que `data-demo`: cuando la demo llega, los dos `Centro` ya existen con municipio, y el `update="false"` del bind raíz de `src/main/resources/data-demo/input/centros-demo.xml` hace que la demo **no los toque** (ni se lo borre). Los dos códigos están en el catálogo común `subsystem/common/data-init/input/Municipio.xml` (`46169` «Mislata», `03009` «Alcoy/Alcoi»).

    **Consecuencia para este diseño:** `localidadCentro` **no** nace nulo en la demo, ESC-034 (T-047) puede leer literalmente «Mislata» en la línea de lugar y fecha tal cual está el árbol hoy, y por tanto **MUST NOT** tocarse `data-demo/input/centros-demo.xml` ni `data-demo/input-config.xml` (no figuran en la tabla §6), ni añadirse ningún `<bind to="municipio">` a la demo, ni **resetearse la base de datos**: el reseteo que versiones anteriores de este diseño exigían partía del supuesto —falso— de que `centros-demo.xml` era el único origen de los centros. Tampoco se toca el dominio `Centro` (ya tiene el `many-to-one` a `Municipio`) ni el catálogo de municipios.

17. **La cabecera de la resolución no repite el nombre del centro.** La ficha del documento pide «la cabecera con el nombre del centro». El formato de `documentospdf/` **no tiene ningún elemento de cabecera**: la cabecera corporativa la pinta el generador (logo de la GVA__!! + el `<titulo>` bilingüe centrado) y todo lo demás **MUST** ir dentro de una `<seccion>`, que se rotula con letra automática (A, B, C…). Añadir una sección solo para repetir el nombre del centro correría las letras de las secciones que la especificación sí declara y metería en el documento un bloque que su ficha no lista. Se asume, por tanto, que la cabecera institucional es la que aporta el generador y que el nombre del centro se imprime **una vez**, en el cuerpo, dentro de «Vistos» (`self.nombreCentro`), que es donde la ficha también lo pide. La solicitud, en cambio, sí lo imprime dos veces (en «Expone» y en el pie «DIRECTOR/A DEL ____»), porque eso sí son dos textos del cuerpo.

18. **La pieza de firma amplía la pantalla de `PENDIENTE_FIRMA` respecto a lo que describe la especificación.** `pantallas-solicitud.md` describe esa pantalla con dos bloques y «Qué puede rellenar: *(ninguno)*». La receta `recetas/firma.md` §1 —que las guías de diseño mandan seguir **tal cual**— añade: un panel oculto con los dos campos de vista que rellena el `onLoad` (`situacionFirma` y `firmaEnServidor`), **seis** paneles de «Firma de la solicitud» excluyentes por situación, un campo **rellenable** `claveCertificado` (el PIN o la contraseña del certificado, con `widget="password"`) en las dos situaciones que lo piden, y **dos** botones «Firmar y presentar la solicitud» mutuamente excluyentes (el de AutoFirma__!!, encadenado con `serial:`, y el de firma en servidor). Es una desviación visible y deliberada: sin ella el alumno con certificado custodiado no podría presentar. El campo `claveCertificado` es **transient** y heredado de `Expediente`, no queda guardado, y el trigger lo borra en un `finally` (§9.1).

    **Qué ve el alumno con `situacionFirma == 'SIN_DNI'`** (un usuario sin documento de identidad válido en su ficha): el panel de aviso «No es posible firmar la solicitud porque su usuario no tiene un documento de identidad. Póngase en contacto con el administrador» y **ningún** botón «Firmar y presentar la solicitud» —los dos lo condicionan a otra situación—, así que sus únicas salidas son «Atrás» y salir del expediente. La especificación no contempla ese caso; la pantalla lo resuelve diciéndolo en vez de ofrecer un botón que fallaría siempre. **MUST NOT** interpretarse como una pantalla sin salida: `VOLVER` sigue disponible y el expediente puede borrarse desde `DATOS_SOLICITUD`.

19. **El botón de firma en servidor invierte a propósito el orden de su cadena `serial:`.** `vistas.md` §4 (Y3) pide que toda cadena `serial:` **termine** en `subsysExpedientes-event-action`, y la del segundo botón `PRESENTAR` es `serial:subsysExpedientes-event-action,exp-AnulacionMatriculaCicloFormativoV1-set-claveCertificado-null-action`, que termina en la `<action-record>` que vacía la clave. Es **el orden que prescribe `recetas/firma.md` §1.3**: la clave del certificado tiene que llegar al evento y borrarse de la pantalla **después**, no antes. La cadena **sí contiene** `subsysExpedientes-event-action`, que es lo que el test real comprueba. Queda anotado como excepción consciente, también en el checklist §15: **MUST NOT** «corregirse» el orden, porque dejaría la firma en servidor sin clave.

20. **Las clases auxiliares de la raíz de la versión son un patrón nuevo.** `ControlDeAcceso` y `DevolucionDelDirector` (§9.0) no las contempla ninguna receta de `k-tipo-expediente`: el skill describe el `PhaseEventManagerImpl`, el `StateEventValidatorImpl` y el `InitialEventManagerImpl`, no clases auxiliares propias del tipo. Se declaran aquí y en `decisiones.md` D4 con **la misma regla de promoción** que la regla de validación propia: *nacen en la raíz de la carpeta de versión; en cuanto las necesite un segundo tipo de expediente suben a `tramites/util/<propósito>/` cumpliendo las seis condiciones de su `CLAUDE.md`*. **MUST NOT** subirlas ahora: con un solo tipo que las use, el común no gana nada y el motor tampoco es su sitio. La tercera guarda, `ControlDeAcceso.exigeOstentarElPerfilDelEstado` (§9.0.1), nace en la misma clase y con la misma regla de promoción, y es **la más general de las tres**: «actuar solo si se ostenta el perfil que el estado declara» es literalmente lo que el motor ya hace para todo el mundo menos para el administrador — y la guarda lo dice **igual que lo dice el motor**, preguntándole el perfil al `<state>` a través de `States` en vez de repetirlo como literal, que es lo que le permite ser genérica de verdad y no una copia con el nombre de este trámite dentro; así que su sitio definitivo no es ni la carpeta de versión ni `tramites/util/`, sino el **motor** — el día en que se decida si `checkPerfilDelEstado` debe seguir eximiendo al administrador (o eximirle solo para leer), esta guarda desaparece de los cuatro `trigger*` sin sustituto. Mientras esa decisión no se tome, **MUST** quedarse aquí: es la única capa que hay. Y se señala expresamente que **`ControlDeAcceso.exigeMismoCentroQueElExpediente` no tiene nada de específico de este trámite**: «actuar solo sobre expedientes del propio centro» lo necesita **todo** expediente multicentro, así que es la primera candidata a subir a `tramites/util/` en cuanto aparezca el segundo trámite con guardas de centro — y, a más largo plazo, la señal de que esa comprobación debería vivir en el motor junto con el arreglo de `auth-expedientes.xml` de la nota 9. `exigeSerElCreador` está en el mismo caso, aunque con menos recorrido: el motor ya concede `CREADOR` por autoría y solo hace falta explícitamente porque exime al administrador. **Y no se funde con la guarda de perfil pese a que hoy dejan pasar a la misma persona**: `AceRepository.findNombresPerfilesByExpediente` excluye `CREADOR` **solo** en la rama del trámite, así que una fila de permisos con `perfilName="CREADOR"` y `tipoExpedienteCode` bastaría para que «ostenta CREADOR» dejara de significar «es el autor»; el razonamiento está en §9.0.1 y en `decisiones.md` D13.

21. **`NoAdmitido` es una regla NUEVA del catálogo común, no un mensaje para una regla que ya existía.** `base/infrastructure/validation/rules/ConditionalRules.kt` gana `NoAdmitido(mensaje)`, una regla que **falla siempre** con ese mensaje y que **MUST** usarse **solo dentro** de una rama `ifValueIn`/`ifValueNotIn`: la rama dice **cuándo** aplica y la regla dice **qué mensaje** sale, de modo que la condición sigue leyéndose en la rama y no dentro de la regla (§11). Es lo único que permite expresar «este valor no vale en este evento» —los dos rechazos de sentido de `ENVIAR_A_FIRMA` y `SUBSANAR` (§10.2)— sin mentir con un `Required` ni llevar al `trigger*` una comprobación que habla del valor de un campo. Se decide aparte de D3 porque D3 solo da **mensaje** a reglas existentes, y esto **amplía el catálogo** que comparte toda la aplicación: el razonamiento completo, con las alternativas descartadas (entre ellas una regla autocontenida `ValorNoAdmitido(valores, mensaje)`), está en `decisiones.md` **D7**. Vive junto a `IfValueIn`/`IfValueNotIn` porque son las únicas con las que tiene sentido, y su restricción de uso **MUST** quedar escrita en el KDoc de la propia clase. El cambio es **aditivo**: ninguna llamada existente lo nota.

22. **El alta de `IABD` deja desactualizada la prosa del «Estado inicial de la base de datos» de los `.desc.md` del catálogo, y este diseño NO la toca.** Esos `.desc.md` de `src/test/e2e/subsystem/sistemaeducativo/` repiten un bloque «Estado inicial de la base de datos» que enumera **siete** ciclos y afirma que «**Todos** son del grado «Ciclo formativo»; todos menos «Sistemas Microinformáticos y Redes» … llevan el nivel «Ciclos Formativos de Grado Superior»», y que describe los tres niveles sin mencionar ningún nombre corto. Tras el Paso 4 eso deja de ser cierto en dos cosas: el catálogo pasa a tener **ocho** ciclos, y el nuevo (`IABD`, «Inteligencia Artificial y Big Data») es del grado **«Curso de especialización»** (`E`), sin nivel; y los tres niveles pasan a tener `nombreCorto` («Básico», «Medio», «Superior»), que además se ve como columna y como campo en el mantenimiento de niveles. Los **ocho ciclos del reparto de tests no entran en esa cuenta**: son configuración del entorno de pruebas de **este** trámite (§4 y nota 23), no un cambio del catálogo versionado, así que quien ejecute la suite del catálogo sobre un entorno limpio sigue viendo ocho ciclos.

    **Qué se hace con eso:** nada dentro de esta iniciativa. Esos ficheros son **snapshots «as-tested»** que generó `/sdd-create-tests-e2e` para la iniciativa `2026-09-11_08-46_nivel-segun-grado-del-ciclo` y llevan escrito en su cabecera «ARTEFACTO GENERADO … NO editar a mano»; su fuente es el `test-e2e-desc/` de aquella iniciativa, y se regeneran ejecutando de nuevo ese skill sobre ella, no editando el `.md`. **MUST NOT** tocarlos aquí (ni a mano ni regenerándolos), porque este diseño no es el dueño de esa iniciativa y el pipeline SDD solo escribe en la suya.

    **Por qué no rompe nada ejecutable:** lo desactualizado es **prosa de contexto**, no aserciones. Ninguno de los 16 `.spec.ts` cuenta ciclos, ni afirma que todos sean de grado «Ciclo formativo», ni fija el número de columnas del grid de niveles; el único `toHaveCount` sobre cabeceras es el de `t-012`, y va sobre `Main@Ciclo-grid`, que este diseño no modifica. Por eso la comprobación que **sí** se exige es **ejecutarlos** (Paso 4 y Paso 17): si alguno se pusiera en rojo, sería una regresión real del código de la aplicación, y se arregla ahí.

    **Quién actualiza la prosa:** el dueño de esa suite, en una iniciativa propia —rehacer el «Estado inicial» del catálogo para que hable de los ocho ciclos, del grado `E` y de los nombres cortos—. Queda anotado aquí para que quien lea esos `.desc.md` después de implementar este trámite sepa por qué la descripción y la base de datos no coinciden.

23. **Los 56 tests E2E comparten una base de datos que nadie resetea, y eso es parte del diseño, no un detalle de ejecución.** `SinOtraSolicitudEnCursoParaElMismoCiclo` (§10.0) impide que un alumno tenga dos expedientes **abiertos** para el mismo ciclo y curso académico, y 54 de los 56 tests terminan dejando el suyo abierto: sin ninguna medida, un test le quita a otro la tripleta que necesita y la suite no puede pasar entera. **La regla NO se toca** —es literalmente lo que fijan la especificación (ESC-033, ESC-036, ESC-037, ESC-038) y §10.0—; lo que se organiza es el **dato de prueba**: cada test tiene reservada su pareja (alumno, ciclo), y la tabla de reparto vive en la sección «Aislamiento entre tests» de `test-e2e-desc.md`, que es su única fuente de verdad. De ahí salen dos exigencias, y las **dos** son **configuración del entorno de pruebas**, no cambios del proyecto: los **ocho ciclos** que el reparto necesita (§4 y nota 11) y los **dos certificados custodiados** nuevos (nota 11). Los ocho ciclos **MUST NOT** darse de alta en `subsystem/sistemaeducativo/data-init/input/Ciclo.xml`: la especificación declara **fuera de alcance** el mantenimiento del catálogo de ciclos —que es común a todos los centros y a todos los trámites— y sería la suite de pruebas dictando el contenido de un dato maestro de producción. Se dan de alta por la propia aplicación, en «Sistema educativo → Ciclos», exactamente igual que los certificados; el único ciclo que sí entra en el data-init es `IABD`, porque lo exige «Datos iniciales» de la especificación. El razonamiento y las alternativas descartadas —limpieza final por test y acoplamiento declarado entre tests— están en `decisiones.md` **D9**. **MUST NOT** «arreglarse» un test que choque con esta regla debilitando el validador ni tocando filas de la base de datos a mano: se le da una pareja libre del reparto.

24. **Ampliación declarada de la notación de acciones de los `trigger*`.** El vocabulario de `design-contract.md` §11.2 solo contempla ramificación en `UPDATE_STATE segun`, y este diseño usa **cinco** construcciones más. Se declaran aquí **todas**, con su significado exacto, en vez de darlas por obvias: esta lista es el **inventario completo**, y quien escriba un trigger nuevo no puede usar nada que no esté aquí o en §11.2.

    - **`<ACCIÓN> — solo si <condición>` / `<ACCIÓN> si <condición>`**: la acción se ejecuta **si y solo si** la condición es cierta en ese punto del trigger; si es falsa no se ejecuta **nada** de esa acción y el trigger continúa por la siguiente. La condición **MUST** ser evaluable con lo que el trigger ya tiene (el expediente o un valor obtenido en una acción anterior) y **MUST NOT** validar un dato del usuario —eso es del validador (§11)—. La usan `triggerPresentar` acción 3 (la firma en servidor solo cuando la situación de firma lo es) y `triggerEnviarAFirma` acción 4 (limpiar el motivo del rechazo cuando `ReglasAnulacionMatricula.esRechazo(expediente)` es falso; la condición se le **pregunta** a su dueño, §11, y **MUST NOT** reescribirse comparando contra el enum).
    - **`<ACCIÓN> — en un `finally` que envuelve los pasos N a M`**: la acción se ejecuta **siempre** al salir de ese tramo, tanto si termina bien como si alguna de esas acciones lanza, y **MUST NOT** tragarse la excepción. La usa `triggerPresentar` acción 9 para borrar `claveCertificado` del objeto pase lo que pase.
    - **`ERROR_NEGOCIO(<llamada a una guarda de ControlDeAcceso>)`, con UN solo argumento**: variante de `ERROR_NEGOCIO(<condición>, <qué transmite el mensaje>)` en la que **la condición y el mensaje viajan dentro de la llamada**. La guarda (§9.0 y §11) comprueba ella misma y lanza `BusinessException` con el mensaje que recibe como segundo parámetro, así que repetir la condición fuera sería escribirla dos veces y arriesgarse a que las dos dejen de decir lo mismo. Se usa así, y solo así, en las **doce** llamadas a una guarda: `triggerContinuar` 1, `triggerVolver` 1, `triggerPresentar` 1 y `triggerDelete` 1 (`ControlDeAcceso.exigeSerElCreador`); `triggerEnviarAFirma` 1, `triggerSubsanar` 1, `triggerFirmar` 1 y `triggerDevolver` 1 (`ControlDeAcceso.exigeMismoCentroQueElExpediente`); y esas **mismas cuatro acciones 1**, a continuación, `ControlDeAcceso.exigeOstentarElPerfilDelEstado(expediente, …)` —**sin** argumento de perfil: la guarda se lo pregunta al `<state>` (§9.0.1)—. Una acción puede llevar, por tanto, **dos** `ERROR_NEGOCIO(…)` de una sola guarda cada uno, escritos en el orden en que se ejecutan: son dos comprobaciones complementarias (§9.0.1) y **MUST NOT** fundirse en una. La forma de **dos** argumentos del contrato sigue siendo la normal para cualquier otro error de negocio, y este diseño la usa en `triggerFirmar` 2.
    - **`SERVICIO(<tipo>.<método>(…)) → <destino>`**: al `SERVICIO(…)` del contrato se le añade el **enlace del resultado** — lo que devuelve la llamada se guarda en `<destino>`, y el sitio donde se escribe **MUST** decir si ese destino es un **campo de la entidad** o una **variable local del trigger**. La usan `triggerPresentar` 2 (`→ situacionFirma`, **variable local**, que consume la condición de la acción 3), `triggerPresentar` 3 (`→ pdfSolicitudFirmada`, **campo** de la entidad) y `triggerFirmar` 2 (`→ almacenDirector`, **variable local**, reutilizada en la acción 5). Un `SERVICIO(…)` **sin** flecha es una llamada cuyo resultado no se usa (`DevolucionDelDirector.borrar`).
    - **`CREAR_METAFILE → <variable local>`**: variante de `CREAR_METAFILE → <campo>` en la que el `MetaFile` creado **no se asigna a ningún campo** de la entidad, sino a una variable local que consume una acción posterior. La usa `triggerFirmar` 6 (`→ pdfTemporal`, que consume el `REGISTRO_SALIDA` de la acción 7; el único campo que se asigna ahí es `pdfResolucionFirmada`, por la flecha de ese registro). Una variable local así **MUST NOT** declararse en `domains.xml` ni aparecer en la tabla de campos de §4, y el sitio donde se escribe **MUST** declararlo, como hace la viñeta que cierra §9.3.

    **MUST NOT** introducirse ninguna otra construcción sin declararla aquí igual.

25. **DESVIACIÓN DECLARADA respecto a `design-guidelines.md` — los OCHO eventos llevan guarda de identidad en el servidor: `VOLVER` y `PRESENTAR`, de autoría; `ENVIAR_A_FIRMA`, `SUBSANAR`, `FIRMAR` y `DEVOLVER`, de perfil.** Las guías dicen literalmente: «**no** se han añadido comprobaciones de autoría a VOLVER ni a PRESENTAR: serían una copia de lo que el motor ya garantiza». Este diseño **las añade igualmente** (§9.1 y §11, «Las cuatro guardas de autoría»), y el motivo es el que la propia guía reconoce dos frases después al justificar por qué sí conserva las de `CONTINUAR` y `DELETE`: **el motor exime al administrador** (`Tramitador.checkPerfilDelEstado` → `SecurityUtil.isAdmin`). Es decir, para el administrador el motor **no** garantiza nada, así que la guarda no es una copia: es la única capa que existe. Dejarlo como pedían las guías dejaba un caso sin validar en el servidor —el administrador disparando `VOLVER` o `PRESENTAR` sobre la solicitud de otro alumno— que contradice HU-010/ESC-025 (el administrador solo consulta, en solo lectura) y ESC-020/ESC-032/ESC-045 (los no creadores ven la pantalla en solo lectura), y que además es el olor **«defensa solo en la vista»** de `k-code-quality`: la pieza que lo cierra ya existe en el trámite (`ControlDeAcceso.exigeSerElCreador`) y se usa dos veces. Coste: dos líneas más, ningún concepto nuevo. **MUST NOT** quitarse «porque las guías decían otra cosa» sin reabrir esta nota.

    **Y el mismo argumento, hasta el final: los otros CUATRO eventos también.** La frase de las guías que justifica las guardas de `CONTINUAR` y `DELETE` —«el motor **exime al administrador**, que en esta spec solo tiene consulta»— **no dice nada de los estados `CREADOR`**: `checkPerfilDelEstado` hace `return` para el administrador en **cualquier** estado (`Tramitador.java` línea 232). Aplicarla solo a los dos estados del `CREADOR` dejaba abiertos `ENVIAR_A_FIRMA`, `SUBSANAR` (perfil `SECRETARIO`) y `FIRMAR`, `DEVOLVER` (perfil `DIRECTOR`), cuya única guarda era la de **centro**, que el administrador **pasa** en cuanto su `centroActivo` es el del expediente — y el Paso 15 le asigna precisamente CIPFP Mislata (nota 13). Sobre los expedientes de ese centro podía, por tanto, revisar, pedir subsanación, devolver y **firmar**: firmar la resolución con el certificado del Director, registrarla de salida y cerrar el expediente. Era el **mismo caso sin validar** que esta nota ya había corregido para `VOLVER` y `PRESENTAR`, dejado abierto en los cuatro eventos restantes. Por eso los cuatro llevan, junto a la guarda de centro, `ControlDeAcceso.exigeOstentarElPerfilDelEstado(expediente, mensaje)` (§9.0.1, §9.2, §9.3 y §11, «Las ocho guardas de identidad»), que **pregunta a los dos dueños** en vez de reescribir ninguna de las dos preguntas: *qué* perfil se exige, al `<state>` del `TipoExpedienteInstance.xml` a través de la clase generada `States` —y por eso la guarda **no recibe ningún `Profile`**: un literal aquí haría que cambiar el `profile` del estado dejara la guarda exigiendo el viejo, en silencio—; y *si el usuario lo ostenta*, a `PerfilesUsuarioService.getPerfilesSobreExpediente`, el mismo al que pregunta `checkPerfilDelEstado`. Coste: cuatro líneas y un método más en una clase que ya existe; ningún `@Inject` nuevo en ningún `PhaseEventManagerImpl` y ningún módulo Guice tocado. La decisión, con sus alternativas, está en `decisiones.md` **D13**. **MUST NOT** afirmarse en ningún sitio del diseño que «actuar está cerrado» apoyándose solo en el perfil del estado: para el administrador el perfil del estado no es una barrera en **ninguno** de los tres estados con eventos.

26. **DECISIÓN DE ARQUITECTURA PENDIENTE DE APROBAR — vistas y menús de un trámite concreto dentro del común compartido.** Este diseño coloca **dos ficheros de vista específicos de este trámite** en `src/main/java/com/educaflow/tramites/views/` —que `tramites/util/CLAUDE.md` describe como recursos XML **compartidos**, con cinco ficheros hoy todos genéricos— y **dos `menuitem` de este trámite** en el `menus.xml` **global**, visibles para todo usuario de todo centro. Es la misma señal de «esta pieza no es de aquí» que `subsystem/expedientes/CLAUDE.md` describe (un artefacto que nombra un trámite, un estado y un perfil concretos), y roza la regla que este mismo diseño enuncia en la nota 8(d) para prohibir parchear desde un trámite una bandeja compartida. Se hace porque **hoy no hay otro sitio**: las vistas de la carpeta de versión están en el formato preprocesado de un tipo de expediente y no admiten un `action-view` ni un `grid`, y un `menuitem` **MUST** estar en un `menus.xml` que el build cargue, y el único que existe es el global. **Coste:** cada trámite con perfiles propios añade dos ficheros y dos menús al común, así que `tramites/views/` deja de ser «lo compartido» y el menú «Expedientes» crece con una entrada por trámite y perfil. (La pieza de servidor a la que esas dos bandejas preguntan, `tramites/util/bandeja/BandejaPorPerfilController`, **no** entra en esta nota: `tramites/util/` es el sitio declarado del código común a varios tipos de expediente y la pieza cumple sus seis condiciones de entrada, Paso 16.0.) **Sitio definitivo:** el mismo que cierra la nota 8 — cuando la bandeja pase el **perfil real del usuario**, estas dos vistas, estos dos menús y esa pieza desaparecen sin sustituto; y si esa solución tardara, harían falta una carpeta de vistas **por trámite** (`tramites/<tramite>/views/`, cargada por el build) y un mecanismo de menús por trámite, que son capacidades del **motor** y del build y **MUST NOT** darse por hechas aquí. Está escrito también en el Paso 16.3-bis y en `decisiones.md` **D11**. **Este diseño no aprueba el patrón: lo declara.**

27. **DESVIACIÓN DECLARADA respecto a `design-guidelines.md` — no hay `_template.xml`.** Las guías enumeran como XML del trámite «`solicitud.xml`, `resolucion.xml` y `_template.xml` bilingüe `<valenciano>/<castellano>`», es decir, dan por supuesto un **fragmento compartido**. Este diseño **no lo crea**, y la justificación completa está en §5 y en la nota 4: de los tres trozos que `documentos.md` declara comunes, uno lo aporta el generador (la cabecera institucional), otro **no imprime lo mismo** en los dos documentos (la identificación: nueve datos en la solicitud, cuatro en la resolución) y el tercero —la línea de referencia de la matrícula, que sí es idéntica— **no es expresable como fragmento**, porque `<include>` solo puede ir como hijo directo de `<documento>` y esa línea vive dentro de una `<seccion>` distinta en cada documento. Recuperar el fragmento obligaría a imprimir de más en la resolución o a partir en dos los bloques A y B del impreso oficial, cuyos rótulos la especificación fija como texto fijo. Se asume, por tanto, la duplicación de **una** línea, que **MUST** tocarse en los dos ficheros a la vez. Queda declarado aquí como anulación expresa de una indicación de las guías, no como olvido.

28. **NO hay ninguna ampliación de la estructura de salida de `design/`: la subcarpeta `tramites-views/` se retiró.** Una versión anterior de este diseño materializaba los dos XML nuevos de `tramites/views/` (`Expediente-revision.xml` y `Expediente-firma.xml`) en una subcarpeta `design/tramites-views/`, declarada como ampliación. `design-contract.md` §1 solo contempla ficheros de la carpeta de **versión** del trámite, y esos dos no son de ahí (nota 26), así que la subcarpeta quedaba fuera del inventario del contrato — un fichero «de más» que el contrato no reconoce y que esta iniciativa **no puede** legalizar, porque ampliar el contrato de la plantilla está fuera de su alcance. Se cierra por el otro lado: **la subcarpeta no existe** y los dos ficheros van escritos **enteros** dentro del `design.md`, en los Pasos 16.1 y 16.2, exactamente igual que los fragmentos XML de los cuatro ficheros de vista existentes que toca el Paso 16.3. El principio del contrato («todo XML se materializa verbatim en `design/`») sigue cumpliéndose para lo que él gobierna, que es la carpeta de versión. **MUST NOT** volver a crearse una subcarpeta de XML en `design/` para ficheros de fuera de la carpeta de versión mientras `design-contract.md` §1 no la contemple.

    **Lo que NO se materializa, y por qué:** los cambios de 16.3 (las cuatro bandejas genéricas) y de 16.4 (los dos `menuitem`) son **inserciones dentro de ficheros que ya existen**, no ficheros. Un `<node domain=…>` suelto o dos `<menuitem/>` sueltos no forman un documento XML bien formado, así que materializarlos como `.xml` produciría ficheros que nada puede validar ni copiar. Se quedan, por tanto, como fragmentos literales dentro del Paso 16.3 y del 16.4, que además declaran expresamente **sobre qué expresión existente** se inserta cada uno.

29. **INVARIANTE DE VISTAS — un panel MUST ser autosuficiente en sus condiciones.** Toda expresión `showIf`/`hideIf`/`requiredIf` de un panel del form plantilla **MUST** citar **solo campos que ese mismo panel pinta**; si el campo no debe verse, se declara en el panel con `hidden="true"`. No es una preferencia de estilo: los campos que el cliente pide al servidor salen de `meta.fields` —los `<field>` del form ya preprocesado— y **nadie recolecta los nombres que aparecen dentro del literal de un `showIf`**, así que una condición que cita un campo que ningún panel del form pinta evalúa contra `undefined` y sale **siempre falsa, en silencio**. En este tipo afectaba a dos paneles: `resolucion-firmada`, cuyo motivo del rechazo no se habría visto **nunca** —tampoco en `RECHAZADA`, contra RUI-RECHAZADA-GENERICA y ESC-030 / T-033—, y `matricula-director`, que funcionaba **por accidente** porque el único form que lo incluye trae además `-decision-secretaria`, que sí pinta `sentidoRevision`: en cuanto se recompusiera esa lista de `<include-panels>`, el `<help>` desaparecería sin aviso. Los dos declaran ahora `<field name="sentidoRevision" hidden="true"/>`; un campo `hidden` no monta widget y por tanto **no ocupa rejilla**, así que ningún ASCII Layout del Paso 7 cambia. El bug se encontró ejecutando T-033 contra la aplicación real, y **no tenía arreglo fuera del XML de vistas**: el motor no reescribe vistas en runtime, `showIf` es 100 % cliente y el dato en base de datos ya era correcto (`sentidoRevision = RECHAZAR`), así que **MUST NOT** buscarse la causa en el `PhaseEventManagerImpl`, el `StateEventValidatorImpl` ni el `InitialEventManagerImpl`. La decisión, con las dos alternativas descartadas (desdoblar el panel por estado; condicionar por `codeState`), está en `decisiones.md` **D12**, y la regla queda escrita también en el Paso 7 y en el checklist §15.

## 15. Checklist del diseñador

**Estructura y materialización**

- [x] ¿Existen todos los ficheros de §1 del contrato y **ninguno más**? ¿Ni un `.java`, ni un `.kt`, ni un `i18n_*.csv`, ni un `estados.png`, ni un `.pdf` dentro de `design/`? Sí, y **sin ninguna ampliación**: los dos XML de `tramites/views/` que este diseño crea no son de la carpeta de versión, así que **no** se materializan en una subcarpeta propia — van escritos enteros dentro del `design.md`, en los Pasos 16.1 y 16.2 (§14 nota 28).
- [x] ¿Hay un `design/fases/<fase>/views.xml` por **cada** fase declarada (`solicitud`, `revision`, `resolucion`), con la fase en minúsculas?
- [x] ¿Hay un `design/documentospdf/<doc>.xml` por cada documento (`solicitud`, `resolucion`) y ninguno de más? Sí, y **ningún** `_<fragmento>.xml`: de los tres trozos que la spec declara comunes, uno lo aporta el generador, otro no imprime lo mismo en los dos documentos y el tercero no es expresable como fragmento porque iría dentro de una `<seccion>` (§5 y §14 notas 4 y 17). Esto **anula expresamente** la indicación de `design-guidelines.md`, que nombra un `_template.xml`; la desviación queda declarada en §14 nota 27.
- [x] ¿Todos los XML materializados están **completos**, sin `TODO`, sin `...`, sin `<button name="">`?
- [x] ¿El `design.md` tiene el frontmatter `type: design` con la clave `template:` copiada de la spec, y las 15 secciones, con esos títulos y en ese orden?
- [x] **Iniciativa de MODIFICACIÓN:** no aplica — la línea «Versión» de la spec dice «la primera versión del trámite», así que no hay fila `| Modificación de | … |`.
- [x] ¿La fila `| Modificación de | … |` está si y solo si la spec declara una modificación? Sí: no está, porque no la declara.

**Máquina de estados**

- [x] ¿Exactamente un estado `initial` en todo el tipo? (`SOLICITUD/DATOS_SOLICITUD`)
- [x] ¿Todo `<state>` lleva escrito su `events`, aunque sea `events=""`?
- [x] ¿`EXIT` **no** aparece en ningún `events`?
- [x] ¿Todo `profile` de un estado es un valor del enum `Profile` (`CREADOR`, `SECRETARIO`, `DIRECTOR`, `RESPONSABLE`)?
- [x] ¿Ningún nombre de estado ni de evento produce un método que pise la API base?
- [x] ¿La tabla de transiciones tiene una fila por cada pareja (estado, evento) declarada, más el arranque, más la de `DELETE` hacia `[*]`? (10 filas)
- [x] ¿Todo evento ramificado declara sus guardas y su `default`? (`FIRMAR`)

**Diagrama**

- [x] ¿El `estados.puml` dibuja los seis estados, cada uno con alias `<FASE>_<ESTADO>`, agrupados por fase como estado compuesto?
- [x] ¿No hay ningún alias que no corresponda a un estado declarado?
- [x] ¿Cada transición del `.puml` coincide con una fila de la tabla de transiciones, guarda incluida? ¿Y cada `closed` está anotado?

**Modelo**

- [x] ¿La entidad es la **primera** `<entity>` del `domains.xml`, con `extends="Expediente"` y `name="AnulacionMatriculaCicloFormativoV1"`?
- [x] ¿El `<module>` es `expedientes` / `com.educaflow.subsystem.expedientes.db`?
- [x] ¿Ningún campo heredado de `Expediente` está redeclarado?
- [x] ¿Todo enum propio lleva la entidad como sufijo?
- [x] ¿Ningún campo lleva `required="true"`?
- [x] ¿Todo campo de la tabla §4 tiene su columna «quién lo rellena» resuelta?
- [x] ¿Todo campo `servidor` está asignado por alguna acción de §8 o §9? Sí, uno a uno; `pdfSolicitudFirmada` lo rellena el trigger en la rama de servidor y el cliente en la de AutoFirma.
- [x] ¿El `<extra-code-model>` existe (hay dos documentos PDF), con una constante por documento y su ruta de recurso correcta, y **solo** con lo que su plantilla genera? Sí: el enum `TipoDocumentoPdf` y `getDocumentoPdf`, nada escrito a mano — el build **reescribe ese bloque entero** en cada compilación (`RichDomainXmlTask` → `DomainXmlFile.addExtraCodeToDomainXml`), así que el predicado `esRechazo(…)` vive en `ReglasAnulacionMatricula.kt`, en la raíz de la versión (§9.0.3).

**Clases**

- [x] ¿El `InitialEventManagerImpl` está en la **raíz** de la versión, parametrizado con la entidad, con exactamente un `triggerInitialEvent`, y sin reasignar lo que ya rellena el `Tramitador`?
- [x] Hay `createRegistroEntrada`: ¿deja `personaSolicitante` y `personaInteresada` no nulos? Sí, pasos 2 y 3 de §8.
- [x] Por cada fase: ¿un `trigger<Evento>` por cada evento de la unión de los `events` de sus estados, y ninguno de más?
- [x] Por cada fase: ¿un `onEnter<Estado>` por **cada** estado, incluidos los `closed`, y ninguno de más?
- [x] ¿Ningún `PhaseEventManagerImpl` declara `triggerInitialEvent` ni `triggerExit`?
- [x] ¿Cada `trigger*` tiene su lista **numerada** de acciones, y el `UPDATE_STATE` de cada una coincide con la tabla de transiciones?
- [x] ¿`triggerDelete` **no** llama a `UPDATE_STATE`?
- [x] ¿Cada fase declara sus `@Inject`, sus constantes y el `import` de `States` de **su propia** versión?
- [x] ¿Ningún `trigger*`/`onEnter*` se factoriza en una superclase compartida? (La lógica común vive en `ControlDeAcceso` —con sus **tres** guardas: autoría, centro y perfil—, `DevolucionDelDirector` y `ReglasAnulacionMatricula`, tres piezas de la raíz de la versión a las que los métodos **delegan**, no superclases.)

**Validador**

- [x] Por cada fase: ¿un `getForState<Estado>InEvent<Evento>` por **cada pareja** (estado, evento) **salvo `DELETE`**, y ninguno de más? (3 + 2 + 2 métodos)
- [x] ¿Cada método tiene sus `field(...)` con las reglas y **argumentos literales**, o un `rules { }` vacío declarado explícitamente?
- [x] ¿Ningún `field(...)` menciona un campo `servidor`, salvo la excepción documentada `pdfSolicitudFirmada`?
- [x] ¿Todo campo editable en la vista de un estado aparece en el `field(...)` del evento que se dispara desde esa vista? (§10.4)
- [x] ¿Los enums usados en `ifValueIn`/`ifValueNotIn` tienen su `import` declarado? (`SentidoRevision…` con alias)

**Vistas**

- [x] ¿El diseño pasa el checklist de vistas de `vistas.md` §8? Sí: un único form plantilla con el `<Entidad>` propio y `model`/`width`/`groups`; los 19 paneles con `name` e hijos directos; ninguno sin incluir; ningún gemelo innecesario (los tres casos de layout distinto son `datos-alumno-resumen` y `matricula-director`, que el director necesita con otros campos, y `presentacion-descarga-aceptada`, que lleva la indicación de RUI-ACEPTADA-GENERICA-002 junto a la fecha y solo la usa `ACEPTADA`); ASCII Layout de los paneles no triviales en el paso 7; ningún `<form state=…>` en la raíz; X1, X2 y X3 cumplidas; Y1 e Y2 cumplidas; `EXIT` fuera de los `events`; footers por debajo de 12; `<buttons-left/>` vacío donde no hay botones; `header` nunca `includeHeader`; ningún `views.xml` de fase vacío; las `<action-method>`/`<action-record>`/`<action-attrs>` propias en el `views.xml` de su fase; visores de PDF con el campo real en `depends`; firma en cliente con sus tres piezas y sus ocho argumentos; ningún `menus.xml` del tipo de expediente.
- [x] ¿**Toda** expresión `showIf`/`hideIf`/`requiredIf` cita solo campos que **el propio panel pinta**? Sí, panel a panel: `subsanacion` y `devolucion-view` condicionan por el campo que enseñan; `revision` y `decision-secretaria` pintan `sentidoRevision`; `matricula-director` y `resolucion-firmada` lo pintan con `hidden="true"` porque no debe verse ahí. En los `views.xml` de fase, los seis paneles de situación de firma y los dos botones `PRESENTAR` condicionan por `situacionFirma`/`firmaEnServidor`, que pinta el panel `firmaSolicitudSituacion` **del mismo form**, y el `if` del `<action-record>` `…-limpiar-revision-action` se evalúa en el servidor sobre el contexto del form `PENDIENTE_REVISION`/`SECRETARIO`, que incluye el panel `revision`. Invariante y motivo en el Paso 7 y en §14 nota 29; decisión en `decisiones.md` D12.
- [x] **Y3 — cumplida con UNA excepción declarada.** Todos los `onClick` incluyen `subsysExpedientes-event-action`. El segundo botón `PRESENTAR` de `PENDIENTE_FIRMA`/`CREADOR` (el de firma en servidor) **no termina** en ella: su cadena es `serial:subsysExpedientes-event-action,exp-AnulacionMatriculaCicloFormativoV1-set-claveCertificado-null-action`, porque `recetas/firma.md` §1.3 exige vaciar la clave **después** del evento. Excepción consciente, explicada en §14 nota 19; **MUST NOT** invertirse.

**Permisos y pasos**

- [x] ¿El perfil del estado inicial se asigna por `tramiteCode`?
- [x] ¿Todo perfil usado por algún estado tiene actor? (`CREADOR`, `SECRETARIO`, `DIRECTOR` y `RESPONSABLE`)
- [x] ¿`design/permisos.xml` es un fragmento con solo lo nuevo, sin duplicar `<perfil>` preexistentes?
- [x] ¿Cada bandeja que fija un `_profile` propio del trámite lista **solo** los expedientes sobre los que el usuario **ostenta** ese perfil, y lo decide **su dueño**? Sí: las dos bandejas nuevas llevan `self.id IN (:expedientesConPerfil)` y la lista la calcula `tramites/util/bandeja/BandejaPorPerfilController` **preguntando** a `PerfilesUsuarioService.getPerfilesSobreExpediente` (Paso 16.0); ningún `<domain>` reescribe el JPQL de `Ace`. Las cuatro genéricas **no** la llevan porque no fijan ningún perfil propio del trámite. Ocultar el menú no era alternativa (`VAR-10.1` fija `groups`), y la condición queda declarada como interfaz —no defensa— en §12.2 y §14 nota 8(d), con su decisión en `decisiones.md` D8 y su regresión en T-056.
- [x] ¿Está declarado qué **no** queda mitigado, y en el sitio donde se afirma la mitigación? Sí, y son **dos** residuos, los dos de la misma raíz (§14 nota 8(d)): **(i)** que el `_profile` viaje en la petición y nadie compruebe en el servidor que el usuario lo **ostente** para **elegir la vista**, de modo que la condición de perfil del Paso 16.0 solo corta el camino normal de navegación —por eso ninguna `RUI-*-GENERICA-*` de §11 se da por garantizada con un filtro de `<domain>`—, y **(ii)** que la bandeja genérica «Expedientes Pendientes» fija `_profile=CREADOR` y **pinta** en `DATOS_SOLICITUD` y `PENDIENTE_FIRMA` el formulario editable del creador a todo el que pueda leer el expediente —el administrador incluido—, y que la condición de perfil del Paso 16.0 **no** lo cubre. Lo que sí queda cerrado, y también está dicho ahí, es **actuar**, y en los **ocho** eventos del tipo: los cuatro de esos dos estados llevan la guarda `ControlDeAcceso.exigeSerElCreador` y los cuatro de `PENDIENTE_REVISION`/`PENDIENTE_FIRMA_DIRECTOR` llevan `ControlDeAcceso.exigeOstentarElPerfilDelEstado` junto a la de centro, así que tampoco el administrador puede revisar, subsanar, devolver ni firmar. **MUST NOT** reaparecer en §12.1 la afirmación de que al administrador «ninguna vista de perfil se le resuelve».
- [x] ¿Todo evento de un estado cuyo `profile` es `CREADOR` lleva guarda de autoría en el servidor? Sí, los **cuatro** (`CONTINUAR`, `DELETE`, `VOLVER`, `PRESENTAR`), porque el motor exime al administrador de `checkPerfilDelEstado`; las dos últimas las añade el diseño anulando expresamente una indicación de `design-guidelines.md`, declarado en §11 y en §14 nota 25.
- [x] ¿Y **todo** evento de un estado con `profile`, sea cual sea ese perfil, lleva guarda de identidad en el servidor? Sí: la exención del administrador es **global** (`Tramitador.java` línea 232 hace `return` en cualquier estado), así que los **ocho** eventos del tipo la llevan — los cuatro de `CREADOR` con `exigeSerElCreador` y los cuatro de `SECRETARIO`/`DIRECTOR` (`ENVIAR_A_FIRMA`, `SUBSANAR`, `FIRMAR`, `DEVOLVER`) con `exigeOstentarElPerfilDelEstado`, **además** de la guarda de centro, que al administrador no le para dentro de su centro activo. Las cuatro de perfil **preguntan a los dos dueños** —al `<state>` (vía `States`) qué perfil se exige y a `PerfilesUsuarioService.getPerfilesSobreExpediente` si el usuario lo ostenta— y **no llevan ningún literal de perfil**: ni como argumento, ni como constante, ni como `import` de `Profile` en el `PhaseEventManagerImpl` (§9.0.1, §11 «Las ocho guardas de identidad», §14 notas 20 y 25 y `decisiones.md` D13).
- [x] ¿La excepción `esAdministrador` está solo donde no abre una pantalla de perfil? Sí: en las **tres** genéricas que fijan `_profile=RESPONSABLE` («Expedientes Esperando», «Expedientes Cerrados» y la búsqueda), no en «Expedientes Pendientes» (Paso 16.3 (a)), que por eso queda filtrada por centro también para el administrador. ESC-025 y T-038 siguen cumpliéndose porque consulta por las tres primeras; **MUST NOT** devolverse el `OR :esAdministrador = true` a 16.3 (a).
- [x] ¿La tabla «Ficheros a crear o modificar» lista **todos** los ficheros reales, con `permisos-demo.xml` como `Modificar`, y ninguno generado?
- [x] ¿Los pasos siguen el orden obligatorio, con el paso de **`CreateFilesTask` en la posición 3** y su comando exacto? Sí; los pasos añadidos (catálogo, reglas de `base`, clases auxiliares, bandejas) se intercalan sin alterar el orden relativo de los canónicos.
- [x] ¿Cada paso de un XML dice «cópialo literalmente» con origen y destino, y cada paso de un `.java`/`.kt` apunta a su sección de especificación sin duplicarla?
- [x] ¿El paso final lleva `./run.sh` y la comprobación en runtime de los agujeros que el build no ve?
- [x] **Iniciativa de MODIFICACIÓN:** ¿hay **exactamente un paso de fichero por fila** de la tabla §6, en su orden relativo y renumerados sin huecos, sin `CreateFilesTask` salvo que el delta añada fases nuevas, y con `./run.sh` como último paso (§9)? No aplica — no es una iniciativa de modificación: la versión `v1` se crea entera, el Paso 3 es `CreateFilesTask` en su posición canónica y la tabla §6 lista ficheros nuevos además de los tocados.

**Tests**

- [x] ¿`design/test-e2e-desc.md` existe, menciona **cada estado** y **cada transición**, y no lleva código ni selectores?
- [x] **Iniciativa de MODIFICACIÓN:** no aplica; la carpeta espejo `src/test/e2e/tramites/…` no existe, así que los `T-NNN` empiezan en `001`.
- [x] **Tests E2E supersedidos:** no aplica; no es una iniciativa de modificación, así que esa subsección **no** se escribe.
