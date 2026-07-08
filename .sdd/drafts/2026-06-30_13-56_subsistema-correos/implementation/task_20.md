---
type: implementation-task
---

# Tarea 20 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-code-quality
- k-sistemas

Genera el código de los tests unitarios descritos en `design/test-unit-desc.md`
para la clase `com.educaflow.subsystem.correos.service.impl.AdjuntoServiceImpl`.

- La descripción es el contrato: implementa EXACTAMENTE los tests que describe (nombre, propósito, mocks,
  acción, aserción/mensaje esperado, y la regla V/R/CC que verifica). **MUST NOT** inventar tests
  que la descripción no liste ni omitir ninguno.
- Ubicación de salida: `src/test/java/com/educaflow/subsystem/correos/service/impl/AdjuntoServiceImplTest.java`.
- Stack: JUnit 5/Jupiter + Mockito.
- Las clases de producción y los XML ya están en el árbol (las tareas previas las materializaron): los tests
  se escriben CONTRA ellas. La descripción y el código **MUST** cuadrar en AMBOS sentidos; si NO cuadran,
  **detente y reporta** (BLOCKED) en vez de adaptar el test. Reporta BLOCKED si:
    - una clase/método que la descripción cita **no existe** en el código, o
    - el código expone una **firma o nombre distinto** del que la descripción cita, o
    - el código expone **clases/métodos públicos que la descripción no lista** (superficie de más).
  **MUST NOT** "adaptar" los tests al código divergente.

## Convenciones (verbatim, `design/test-unit-desc.md`)

- JUnit 5 (Jupiter) + Mockito (`@ExtendWith(MockitoExtension.class)`).
- Nombres de test: `metodo_condicion_resultadoEsperado`.
- **Aislar una violación de regla:** cuando un método `validateInsert`/`validateUpdate`/`validateRemove` aplica varias reglas en secuencia, cada test de rama de error construye una entidad **válida en todos los campos salvo el que se quiere probar**.
- **Mensaje de una validación fallida:** se comprueba con `resultado.get().get(0).getMessage()`.
- **`SecurityUtil`** (`com.educaflow.base.util.SecurityUtil`): `isAdmin(...)`/`getUser()` se mockean con `Mockito.mockStatic(SecurityUtil.class)`. Para "el centro pertenece al usuario", se instancia un `com.axelor.auth.db.User` real con `setCentroUsuarios(List.of(centroUsuario))`, cada `com.educaflow.subsystem.common.db.CentroUsuario` con `setCentro(...)`.
- **`I18n.get(...)`**: se mockea con `Mockito.mockStatic(I18n.class, withSettings().strictness(Strictness.LENIENT))` devolviendo el propio argumento.

## Sección concreta de `design/test-unit-desc.md` a implementar (verbatim)

### Clase: `com.educaflow.subsystem.correos.service.impl.AdjuntoServiceImpl`  —  servicio

**Responsabilidad:** validar el alta de `Adjunto` (pertenencia a un correo en creación, unicidad de nombre, permiso de centro) e impedir su modificación/borrado.
**Colaboradores a mockear:** `SecurityUtil` (estático), `I18n` (estático).
**Origen diseño:** `design.md` Paso 3 (servicios).

### Método: `Adjunto update(Adjunto nuevo, Adjunto original)`

- **`update_siempre_lanzaUnsupportedOperationException`** — Tipo: error. Verifica: V-Adjunto-007 (patrón gemelo).
  - **Arrange:** dos `Adjunto` cualesquiera.
  - **Act:** `assertThrows(UnsupportedOperationException.class, () -> service.update(nuevo, original))`.
  - **Assert:** mensaje `"Los adjuntos son inmutables tras su creación."` (mensaje inferido por el `test-unitarios`: la spec/diseño no fijan un texto literal para esta acción — ver Notas de este fichero al final).

### Método: `void remove(Adjunto adjunto)`

- **`remove_siempre_lanzaUnsupportedOperationException`** — Tipo: error. Verifica: V-Adjunto-008 (patrón gemelo, análogo a RES-Correo-003).
  - **Arrange:** un `Adjunto` cualquiera.
  - **Act:** `assertThrows(UnsupportedOperationException.class, () -> service.remove(adjunto))`.
  - **Assert:** mensaje `"Los adjuntos no se pueden borrar."` (mensaje inferido, ver Notas).

### Método: `Optional<BusinessMessages> validateInsert(Adjunto adjunto)`

Cada test construye un `Adjunto` **válido salvo en el campo probado** (ver Convenciones).

- **`validateInsert_todoValido_devuelveOptionalVacio`** — Tipo: happy. Verifica: V-Adjunto-001 a V-Adjunto-006 (todas superadas).
  - **Arrange:** `Adjunto` con `correo` no nulo (con `fechaCreacion = null`, `centro = centroA`, `adjuntos = List.of(esteAdjunto)`), `nombreFichero = "doc.pdf"`, `contenido` un `MetaFile` no nulo; `SecurityUtil.isAdmin(...)` → `true`.
  - **Act/Assert:** `Optional.isEmpty()`.
- **`validateInsert_correoNulo_devuelveMensajeDebePertenecerAUnCorreo`** — Tipo: error. Verifica: V-Adjunto-001.
  - **Arrange:** `correo = null`.
  - **Act/Assert:** mensaje `"El adjunto debe pertenecer a un correo"`.
- **`validateInsert_usuarioNoAdminDeOtroCentro_devuelveMensajeCentroNoSuyo`** — Tipo: error. Verifica: V-Adjunto-002.
  - **Arrange:** `correo.centro = centroB`; `SecurityUtil.isAdmin(...)` → `false`; `SecurityUtil.getUser()` → `User` con `centroUsuarios` solo de `centroA`.
  - **Act/Assert:** mensaje `"No puede añadir adjuntos a correos de un centro que no es suyo"`.
- **`validateInsert_correoYaExistente_devuelveMensajeNoSePuedenAnadirAUnoExistente`** — Tipo: error. Verifica: V-Adjunto-003.
  - **Arrange:** `correo.fechaCreacion = <una fecha ya fijada>` (simula un correo ya persistido de antes).
  - **Act/Assert:** mensaje `"No se pueden añadir adjuntos a un correo ya existente"`.
- **`validateInsert_correoEnCreacion_esValido`** — Tipo: borde. Verifica: V-Adjunto-003 (rama OK).
  - **Arrange:** `correo.fechaCreacion = null`.
  - **Act/Assert:** no se añade el mensaje de V-Adjunto-003 (puede seguir habiendo `Optional` vacío si el resto es válido).
- **`validateInsert_nombreFicheroNulo_devuelveMensajeObligatorio`** — Tipo: error. Verifica: V-Adjunto-004.
  - **Arrange:** `nombreFichero = null`.
  - **Act/Assert:** mensaje `"El nombre del fichero es obligatorio"`.
- **`validateInsert_contenidoNulo_devuelveMensajeDebeAdjuntarFichero`** — Tipo: error. Verifica: V-Adjunto-005.
  - **Arrange:** `contenido = null`.
  - **Act/Assert:** mensaje `"Debe adjuntar el fichero"`.
- **`validateInsert_nombreFicheroDuplicadoEntreHermanos_devuelveMensajeYaExiste`** — Tipo: error. Verifica: V-Adjunto-006.
  - **Arrange:** `correo.adjuntos = List.of(hermanoConMismoNombreFichero("doc.pdf"), esteAdjunto("doc.pdf"))`.
  - **Act/Assert:** mensaje `"ya existe un adjunto con ese nombre en el correo"`.
- **`validateInsert_nombreFicheroUnicoEntreHermanos_esValido`** — Tipo: borde. Verifica: V-Adjunto-006 (rama OK).
  - **Arrange:** `correo.adjuntos = List.of(hermano("otro.pdf"), esteAdjunto("doc.pdf"))`.
  - **Act/Assert:** no se añade el mensaje de V-Adjunto-006.

### Método: `Optional<BusinessMessages> validateUpdate(Adjunto nuevo, Adjunto original)`

- **`validateUpdate_siempre_devuelveMensajeInmutabilidad`** — Tipo: error. Verifica: V-Adjunto-007.
  - **Arrange:** dos `Adjunto` cualesquiera.
  - **Act/Assert:** `Optional` presente con mensaje `"Los adjuntos son inmutables tras su creación."` (mismo mensaje inferido que `update()`).

### Método: `Optional<BusinessMessages> validateRemove(Adjunto adjunto)`

- **`validateRemove_siempre_devuelveMensajeNoSePuedenBorrar`** — Tipo: error. Verifica: V-Adjunto-008.
  - **Arrange:** un `Adjunto` cualquiera.
  - **Act/Assert:** `Optional` presente con mensaje `"Los adjuntos no se pueden borrar."` (mismo mensaje inferido que `remove()`).

### Método: `AllowProperties allowPropertiesInsert()`

- **`allowPropertiesInsert_permiteNombreFicheroContenidoYCorreo`** — Tipo: happy. Verifica: `—` (frontera de confianza).
  - **Arrange:** ninguno.
  - **Act:** `AllowProperties result = service.allowPropertiesInsert()`.
  - **Assert:** `result.allowProperty("nombreFichero")`, `result.allowProperty("contenido")` y `result.allowProperty("correo")` son `true` (Adjunto no tiene ningún campo servidor que deba estar en `false`).

### Nota de este fichero aplicable (verbatim, `design/test-unit-desc.md`)

1. **Mensajes de `AdjuntoServiceImpl.update()`/`validateUpdate()` y `remove()`/`validateRemove()`.** `entity-Adjunto.md` y `design.md` fijan que estas cuatro operaciones "SIEMPRE rechazan" pero, a diferencia de las equivalentes de `Correo`, no citan un texto literal de mensaje. Se ha decidido un texto coherente con el estilo del resto de mensajes del subsistema y con la intro de `entity-Adjunto.md` ("Como un correo nunca se borra, sus adjuntos tampoco"): `"Los adjuntos son inmutables tras su creación."` y `"Los adjuntos no se pueden borrar."`. Si al implementar se elige otro texto, basta ajustar el literal esperado en estos cuatro tests (`update_siempre_lanzaUnsupportedOperationException`, `validateUpdate_siempre_devuelveMensajeInmutabilidad`, `remove_siempre_lanzaUnsupportedOperationException`, `validateRemove_siempre_devuelveMensajeNoSePuedenBorrar`), sin cambiar nada más de este fichero.

**Ninguna otra clase se testea en esta tarea.**
