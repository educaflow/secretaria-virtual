---
type: design
---

# Diseño: Grupos y Notas

**Objetivo:** Construir el sistema `grupos`, que permite a la secretaría de un centro (Supervisor) definir grupos de alumnos ligados a un curso del catálogo educativo, registrar la nota final de cada alumno en cada módulo del grupo (a partir de las actas), calcular la nota media de cada alumno y cerrar/reabrir grupos para fijar las calificaciones. El Administrador hace lo mismo sobre cualquier centro y además puede reabrir grupos cerrados. El Alumno consulta en solo lectura sus grupos, sus notas por módulo y su nota media.

**Capa:** system/grupos
**Especificación de origen:** .sdd/drafts/2026-06-16_01-44_grupos-y-notas/specification.md
**Skills necesarios para la implementación:** k-sistemas, k-code-quality, k-secure-coding, k-vistas, k-validaciones

---

## Decisiones de modelado

### Valor de la Nota (enum + integer)
El valor de la Nota tiene tres formas («No evaluado», entero 1..10, «Matrícula de Honor»). Se modela con **dos campos**:
- `tipoValor` — enum `TipoValorNota {NO_EVALUADO, NUMERICA, MATRICULA_HONOR}` (discriminador).
- `valorNumerico` — `<integer min="1" max="10">`, solo significativo cuando `tipoValor == NUMERICA`.

Justificación: un único entero necesitaría valores mágicos (0 = no evaluado, 11 = MH) frágiles e incompatibles con `min/max`; un único enum de 12 valores complica `min/max`, el conteo de MH (VAL-017) y el cálculo de la media (CC-001). Dos campos permiten validación declarativa, conteo limpio de MH y un cálculo de media robusto. La columna «valor» de los grids se muestra con el campo derivado `valorTexto`.

### Curso académico = integer (no existe entidad CursoAcademico)
En el modelo real, «curso académico» es un `integer` (año). En `Centro` es el campo `curso`. Por tanto `Grupo.cursoAcademico` es `<integer>`. Para el supervisor lo asigna el servidor desde `SecurityUtil.getUser().getCentroActivo().getCurso()` (R-Grupo-002); el administrador lo introduce. La presentación «2024/2025» de los escenarios es un formato de UI fuera de alcance del modelo.

### Roles
- **Administrador**: `com.educaflow.base.util.SecurityUtil.isAdmin(user)`. Menú con `groups="admins"`.
- **Supervisor**: `User.getTiposUsuarioActivos()` contiene `TipoUsuario` con `codigo == "SUPERVISOR"`. Menú con `if="__user__?.tiposUsuarioActivos?.any { it.codigo == 'SUPERVISOR' }"`.
- **Alumno**: `codigo == "ALUMNO"`. Menú con `if=...'ALUMNO'`.

### Nota media y valorTexto (campos derivados, momento: lectura)
`AlumnoGrupo.notaMedia` (CC-001) y `Nota.valorTexto` se modelan como campos `transient` con cuerpo `<![CDATA[]]>` que delega en el helper estático `com.educaflow.system.grupos.service.NotaMediaCalculator` (patrón real del proyecto, cf. `Centro.xml`). No se persisten ni requieren R-Antes.

---

## Ficheros a crear o modificar

| Fichero | Acción | Skill | Descripción |
|---|---|---|---|
| `src/main/java/com/educaflow/system/grupos/domains/Grupo.xml` | Crear | k-sistemas | Entidad Grupo + enum EstadoGrupo, unique-constraint RES-001, finder VAL-003/005 |
| `src/main/java/com/educaflow/system/grupos/domains/ModuloGrupo.xml` | Crear | k-sistemas | Entidad ModuloGrupo, unique-constraint RES-003 |
| `src/main/java/com/educaflow/system/grupos/domains/AlumnoGrupo.xml` | Crear | k-sistemas | Entidad AlumnoGrupo, notaMedia (CC-001), unique-constraint RES-005, finder VAL-013 |
| `src/main/java/com/educaflow/system/grupos/domains/Nota.xml` | Crear | k-sistemas | Entidad Nota + enum TipoValorNota, valorTexto, unique-constraint RES-006, finder VAL-017 |
| `src/main/java/com/educaflow/system/grupos/service/GrupoService.java` | Crear | k-sistemas, k-secure-coding | Interfaz ModelService<Grupo> + tripletas cerrar/reabrir |
| `src/main/java/com/educaflow/system/grupos/service/impl/GrupoServiceImpl.java` | Crear | k-sistemas, k-secure-coding, k-validaciones | Lógica de Grupo (VAL-001..009, RN-001..004, estado inicial) |
| `src/main/java/com/educaflow/system/grupos/service/AlumnoGrupoService.java` | Crear | k-sistemas | Interfaz ModelService<AlumnoGrupo> |
| `src/main/java/com/educaflow/system/grupos/service/impl/AlumnoGrupoServiceImpl.java` | Crear | k-sistemas, k-secure-coding, k-validaciones | Lógica de AlumnoGrupo (VAL-010..014, RN-005) |
| `src/main/java/com/educaflow/system/grupos/service/NotaService.java` | Crear | k-sistemas | Interfaz ModelService<Nota> |
| `src/main/java/com/educaflow/system/grupos/service/impl/NotaServiceImpl.java` | Crear | k-sistemas, k-secure-coding, k-validaciones | Lógica de Nota (VAL-015..017, CC-002/003) |
| `src/main/java/com/educaflow/system/grupos/service/ModuloGrupoService.java` | Crear | k-sistemas | Interfaz ModelService<ModuloGrupo> (vacía, para descubrimiento) |
| `src/main/java/com/educaflow/system/grupos/service/impl/ModuloGrupoServiceImpl.java` | Crear | k-sistemas | Impl ModuloGrupo (sin AllowProperties cliente) |
| `src/main/java/com/educaflow/system/grupos/service/NotaMediaCalculator.java` | Crear | k-code-quality | Helper estático: cálculo de la nota media (CC-001) y texto de la nota (valorTexto) |
| `src/main/java/com/educaflow/system/grupos/controller/GrupoController.java` | Crear | k-sistemas, k-secure-coding | validateSave/validateDelete + cerrar + reabrir |
| `src/main/java/com/educaflow/system/grupos/controller/AlumnoGrupoController.java` | Crear | k-sistemas, k-secure-coding | validateSave/validateDelete de AlumnoGrupo |
| `src/main/java/com/educaflow/system/grupos/controller/NotaController.java` | Crear | k-sistemas, k-secure-coding | validateSave de Nota |
| `src/main/java/com/educaflow/system/grupos/views/Grupo.xml` | Crear | k-vistas | Pantalla supervisor (action-view @Main + cadena de vistas anidadas) |
| `src/main/java/com/educaflow/system/grupos/views/Grupo-admin.xml` | Crear | k-vistas | Pantalla administración (action-view @Admin + reabrir) |
| `src/main/java/com/educaflow/system/grupos/views/AlumnoGrupo-alumno.xml` | Crear | k-vistas | Pantalla «Mis notas» del alumno (action-view @Alumno, solo lectura) |
| `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` | Modificar | k-vistas (menus.md) | Añadir raíz «Notas» (Grupos supervisor / Mis notas alumno) y hoja «Grupos (administración)» bajo «Administración SV» |

> Los ficheros XML materializados en `design/domains`, `design/views` y `design/menus.xml` son los **mismos** que `sdd-implementer-system` copiará a `src/main/java/com/educaflow/system/grupos/...` (los menús se fusionan con el `menus.xml` único del proyecto). **No** se crean repositorios personalizados ni módulo Guice (los `ModelService` los descubre `ModelServiceFactory`; los finders se declaran en el dominio y se invocan casteando el `repository` al repo generado).

---

## Pasos

### Paso 1 — Recursos
No hay ficheros estáticos. Los enums viven en sus dominios (`EstadoGrupo` en `Grupo.xml`, `TipoValorNota` en `Nota.xml`). i18n se genera por script (no se crean csv a mano).

### Paso 2 — Dominios

Ver el XML completo en `design/domains/*.xml` (validados contra `domain-models.xsd`). Resumen estructural:

- **`design/domains/Grupo.xml`** — entidad `Grupo` (módulo `grupos`): `nombre` (string, required, cliente), `curso` (m2o Curso, required, cliente, inmutable), `cursoAcademico` (integer, cliente/servidor según rol, SIN required), `centro` (m2o Centro, cliente/servidor según rol, SIN required), `estado` (enum EstadoGrupo, servidor, SIN required), `fechaCierre` (datetime, servidor, SIN required), `modulos` (o2m ModuloGrupo, orphanRemoval), `alumnos` (o2m AlumnoGrupo, orphanRemoval). `unique-constraint(centro,cursoAcademico,nombre)` [RES-001]. `finder-method findByCentroAndCursoAcademicoAndNombre` [VAL-003/005]. Enum `EstadoGrupo {ABIERTO, CERRADO}`.
- **`design/domains/ModuloGrupo.xml`** — `grupo` (m2o, required, servidor), `modulo` (m2o Modulo, required, servidor), `notas` (o2m Nota, orphanRemoval). `unique-constraint(grupo,modulo)` [RES-003].
- **`design/domains/AlumnoGrupo.xml`** — `grupo` (m2o, required, servidor), `alumno` (m2o User, required, cliente), `notas` (o2m Nota, orphanRemoval), `notaMedia` (string transient CDATA → `NotaMediaCalculator.calcular(this)`) [CC-001]. `unique-constraint(grupo,alumno)` [RES-005]. `finder-method findByAlumnoAndCentroAndCursoAcademico` (all) [VAL-013/RES-004].
- **`design/domains/Nota.xml`** — `moduloGrupo` (m2o, required, servidor), `alumnoGrupo` (m2o, required, servidor), `tipoValor` (enum TipoValorNota, SIN required, servidor al crear / cliente al modificar), `valorNumerico` (integer min=1 max=10, SIN required, cliente al modificar), `fechaCalificacion` (datetime, servidor) [CC-002], `fechaUltimaModificacion` (datetime, servidor) [CC-003], `valorTexto` (string transient CDATA → `NotaMediaCalculator.texto(this)`). `unique-constraint(moduloGrupo,alumnoGrupo)` [RES-006]. `finder-method findByModuloGrupoAndTipoValor` (all) [VAL-017]. Enum `TipoValorNota {NO_EVALUADO, NUMERICA, MATRICULA_HONOR}`.

**Cascadas de borrado (composición del spec):** borrar Grupo → orphanRemoval de `modulos` y `alumnos`; quitar alumno → orphanRemoval de sus `notas` (ESC-005); borrar módulo del grupo → orphanRemoval de sus `notas`. La Nota tiene `mappedBy` desde dos padres (`moduloGrupo` y `alumnoGrupo`), ambos con `orphanRemoval`; Hibernate puede emitir borrados redundantes pero idempotentes (verificar en implementación; alternativa: borrar las notas del módulo en un `fireActionRule_*` de `ModuloGrupo.remove`).

### Paso 3 — Servicios

#### `com.educaflow.system.grupos.service.NotaMediaCalculator` (helper estático, no es ModelService)
```java
// Helper de cálculo de campos derivados de solo lectura (CC-001 y valorTexto).
public final class NotaMediaCalculator {
    // CC-001: media de las notas del alumno en el grupo. Recorre alumnoGrupo.getNotas():
    //   excluye NO_EVALUADO; MATRICULA_HONOR cuenta como 10; NUMERICA cuenta valorNumerico.
    //   Devuelve la media redondeada al entero más cercano (HALF_UP) como String, o "Sin nota"
    //   si no hay ningún módulo evaluado. NO persiste.
    public static String calcular(AlumnoGrupo alumnoGrupo);
    // Texto presentable del valor de una nota: "No evaluado" / el número / "Matrícula de Honor".
    public static String texto(Nota nota);
}
```

#### `com.educaflow.system.grupos.service.GrupoService`
```java
public interface GrupoService extends ModelService<Grupo> {
    // Acción propia: cerrar el grupo (RN-003).
    Grupo cerrar(Grupo grupo);
    Optional<BusinessMessages> validateCerrar(Grupo grupo);
    AllowProperties allowPropertiesCerrar();

    // Acción propia: reabrir el grupo (RN-004), solo administrador (VAL-008).
    Grupo reabrir(Grupo grupo);
    Optional<BusinessMessages> validateReabrir(Grupo grupo);
    AllowProperties allowPropertiesReabrir();
}
```

#### `com.educaflow.system.grupos.service.impl.GrupoServiceImpl extends DefaultModelService<Grupo> implements GrupoService`
```java
public GrupoServiceImpl(Class<Grupo> model, Repository<Grupo> repository) { super(model, repository); }
```

**(1) Acciones**
```java
// insert: valida (VAL-001/002/003) como primera línea; aplica R-Grupo-002 (override
//   centro/cursoAcademico para supervisor) y R-Grupo-003 (estado inicial ABIERTO) ANTES de
//   persistir, y R-Grupo-001 (generar ModuloGrupo desde curso.modulos) también ANTES.
//   Persiste con repository.save (NUNCA super.insert).
@Override public Grupo insert(Grupo grupo);

// update: valida (VAL-004 grupo ABIERTO, VAL-005 nombre único). Persiste con repository.save.
@Override public Grupo update(Grupo grupo, Grupo original);

// remove: valida (VAL-009 grupo ABIERTO). repository.remove; la cascada borra módulos/alumnos/notas.
@Override public void remove(Grupo grupo);

// cerrar: valida (VAL-006). Aplica R-Grupo-004 (estado=CERRADO, fechaCierre=now). repository.save.
@Override public Grupo cerrar(Grupo grupo);

// reabrir: valida (VAL-007, VAL-008 solo admin). Aplica R-Grupo-005 (estado=ABIERTO,
//   fechaCierre=null). repository.save.
@Override public Grupo reabrir(Grupo grupo);
```

**(2) Métodos de Validación**
```java
// V-Grupo-001 (VAL-001): nombre vacío → mensaje "el nombre del grupo es obligatorio" (transmite el campo).
// V-Grupo-002 (VAL-002): curso null → mensaje "el curso es obligatorio".
// V-Grupo-003 (VAL-003, RES-001): ((GrupoRepository)repository).findByCentroAndCursoAcademicoAndNombre(
//   centro, cursoAcademico, nombre) != null (con centro/cursoAcademico ya resueltos por R-Grupo-002)
//   → mensaje de duplicado (transmite nombre+centro+curso académico).
@Override public Optional<BusinessMessages> validateInsert(Grupo grupo);

// V-Grupo-004 (VAL-004): estado != ABIERTO → "no se puede modificar un grupo cerrado".
// V-Grupo-005 (VAL-005, RES-001): findByCentroAndCursoAcademicoAndNombre devuelve otro grupo con
//   id != grupo.id → mensaje de duplicado.
@Override public Optional<BusinessMessages> validateUpdate(Grupo grupo, Grupo original);

// V-Grupo-009 (VAL-009): estado != ABIERTO → "no se puede borrar un grupo cerrado".
@Override public Optional<BusinessMessages> validateRemove(Grupo grupo);

// V-Grupo-006 (VAL-006): estado != ABIERTO → "el grupo ya está cerrado".
public Optional<BusinessMessages> validateCerrar(Grupo grupo);

// V-Grupo-007 (VAL-007): estado != CERRADO → "el grupo ya está abierto".
// V-Grupo-008 (VAL-008): !SecurityUtil.isAdmin(SecurityUtil.getUser()) → "no tiene permisos para reabrir el grupo".
public Optional<BusinessMessages> validateReabrir(Grupo grupo);
```

**(3) AllowProperties**
```java
// Crear: cliente = nombre, curso, centro, cursoAcademico, alumnos. estado/fechaCierre/modulos
//   son servidor → fuera de la whitelist. (centro/cursoAcademico en la whitelist porque el admin
//   los envía; R-Grupo-002 los sobrescribe condicionalmente para el supervisor.)
@Override public AllowProperties allowPropertiesInsert(); // createAllowProperties(nombre,curso,centro,cursoAcademico,alumnos)
// Modificar: cliente = SOLO nombre. curso/centro/cursoAcademico inmutables; estado/fechaCierre servidor.
@Override public AllowProperties allowPropertiesUpdate(); // createAllowProperties(nombre)
// Cerrar/Reabrir: ningún campo cliente (solo cambian estado/fechaCierre en el servidor).
public AllowProperties allowPropertiesCerrar();  // createAllowProperties(Map.of()) — whitelist vacía
public AllowProperties allowPropertiesReabrir(); // createAllowProperties(Map.of()) — whitelist vacía
```

**(4) Action Rules**
```java
// R-Grupo-002 (RN-002, Antes de Crear): si !SecurityUtil.isAdmin(user) asigna INCONDICIONALMENTE
//   grupo.setCentro(user.getCentroActivo()) y grupo.setCursoAcademico(user.getCentroActivo().getCurso()),
//   ignorando lo recibido. Si es admin, respeta lo recibido. Override CONDICIONAL por rol
//   (k-secure-coding §4): para el no-admin estos campos son de facto servidor; MUST NOT añadir
//   guarda if(==null) en la rama del supervisor.
private void fireActionRule_AsignarCentroYCursoAcademico(Grupo grupo);

// R-Grupo-003 (estado inicial, Antes de Crear): grupo.setEstado(EstadoGrupo.ABIERTO) INCONDICIONAL
//   (campo servidor; k-secure-coding §3.3). Origen spec: "Estado inicial: ABIERTO".
private void fireActionRule_AsignarEstadoInicial(Grupo grupo);

// R-Grupo-001 (RN-001, Antes de Crear, RES-002): por cada CursoModulo de grupo.getCurso().getModulos()
//   crea un ModuloGrupo(grupo, modulo) y lo añade a grupo.getModulos(). Garantiza que los módulos del
//   grupo coinciden con los del curso.
private void fireActionRule_GenerarModulosDelCurso(Grupo grupo);

// R-Grupo-004 (RN-003, Antes de cerrar): estado=CERRADO, fechaCierre=LocalDateTime.now() INCONDICIONAL.
private void fireActionRule_RegistrarCierre(Grupo grupo);

// R-Grupo-005 (RN-004, Antes de reabrir): estado=ABIERTO, fechaCierre=null INCONDICIONAL.
private void fireActionRule_RegistrarReapertura(Grupo grupo);
```

**(5) Otras funciones:** `boolean esAdmin()` → `SecurityUtil.isAdmin(SecurityUtil.getUser())`. Alta de los alumnos enviados al crear (si los hay) delegando en `modelServiceFactory.resolve(AlumnoGrupo.class)`.

#### `com.educaflow.system.grupos.service.ModuloGrupoService` / `ModuloGrupoServiceImpl`
Interfaz vacía (`extends ModelService<ModuloGrupo>`). Impl con el constructor obligatorio; no expone acciones ni AllowProperties cliente (los módulos los crea el sistema). Existe solo para que `ModelServiceFactory` lo descubra.

#### `com.educaflow.system.grupos.service.AlumnoGrupoService` / `AlumnoGrupoServiceImpl`
```java
public AlumnoGrupoServiceImpl(Class<AlumnoGrupo> model, Repository<AlumnoGrupo> repository) { super(model, repository); }
```
**(1) Acciones**
```java
// insert: valida (VAL-010/011/012/013). Aplica R-AlumnoGrupo-001 (crear Nota NO_EVALUADO por cada
//   ModuloGrupo del grupo) DESPUÉS de persistir el AlumnoGrupo. repository.save.
@Override public AlumnoGrupo insert(AlumnoGrupo alumnoGrupo);
// remove: valida (VAL-014 grupo ABIERTO). repository.remove; la cascada borra sus notas (ESC-005).
@Override public void remove(AlumnoGrupo alumnoGrupo);
```
**(2) Métodos de Validación**
```java
// V-AlumnoGrupo-001 (VAL-010): alumno null → "debe elegir un alumno".
// V-AlumnoGrupo-002 (VAL-011): grupo.estado != ABIERTO → "no se pueden añadir alumnos a un grupo cerrado".
// V-AlumnoGrupo-003 (VAL-012): alumno.getTiposUsuarioActivos() no contiene code "ALUMNO" o el centro
//   activo del alumno != grupo.getCentro() → "el alumno debe ser un usuario de tipo Alumno del centro del grupo"
//   (transmite el alumno y el centro).
// V-AlumnoGrupo-004 (VAL-013, RES-004): findByAlumnoAndCentroAndCursoAcademico(alumno, grupo.centro,
//   grupo.cursoAcademico) contiene alguna pertenencia de otro grupo → "el alumno ya pertenece a otro
//   grupo de este curso académico". (RES-005 «mismo alumno dos veces en el mismo grupo» lo bloquea el
//   unique-constraint(grupo,alumno) — ESC-019.)
@Override public Optional<BusinessMessages> validateInsert(AlumnoGrupo alumnoGrupo);
// V-AlumnoGrupo-005 (VAL-014): grupo.estado != ABIERTO → "no se pueden quitar alumnos de un grupo cerrado".
@Override public Optional<BusinessMessages> validateRemove(AlumnoGrupo alumnoGrupo);
```
**(3) AllowProperties**
```java
// Crear: cliente = SOLO alumno. grupo es servidor (lo inyecta el modal onNew __parent__) → fuera de la whitelist.
@Override public AllowProperties allowPropertiesInsert(); // createAllowProperties(alumno)
```
**(4) Action Rules**
```java
// R-AlumnoGrupo-001 (RN-005, Después de Crear): por cada ModuloGrupo de alumnoGrupo.getGrupo().getModulos()
//   crea una Nota(moduloGrupo, alumnoGrupo, tipoValor=NO_EVALUADO) y la persiste
//   (modelServiceFactory.resolve(Nota.class) o repository propio de Nota). Efecto colateral → fase Después.
private void fireActionRule_CrearNotasNoEvaluado(AlumnoGrupo alumnoGrupo);
```

#### `com.educaflow.system.grupos.service.NotaService` / `NotaServiceImpl`
```java
public NotaServiceImpl(Class<Nota> model, Repository<Nota> repository) { super(model, repository); }
```
**(1) Acciones**
```java
// update: valida (VAL-015/016/017). Aplica R-Nota-001 (CC-002) y R-Nota-002 (CC-003) ANTES de persistir.
//   repository.save. (No hay insert por el cliente: las notas las crea R-AlumnoGrupo-001.)
@Override public Nota update(Nota nota, Nota original);
```
**(2) Métodos de Validación**
```java
// V-Nota-001 (VAL-015): nota.getModuloGrupo().getGrupo().estado != ABIERTO → "no se pueden modificar las
//   notas de un grupo cerrado".
// V-Nota-002 (VAL-016): combinación inválida (NO_EVALUADO/ MH con valorNumerico no nulo, o NUMERICA sin
//   entero 1..10) → "la nota debe ser No evaluado, un número entero del 1 al 10 o Matrícula de Honor".
// V-Nota-003 (VAL-017): si tipoValor==MATRICULA_HONOR y findByModuloGrupoAndTipoValor(moduloGrupo,
//   MATRICULA_HONOR) ya tiene 3 (excluyendo la propia nota por id) → "no se pueden poner más de 3
//   matrículas de honor en un módulo".
@Override public Optional<BusinessMessages> validateUpdate(Nota nota, Nota original);
```
**(3) AllowProperties**
```java
// Modificar: cliente = tipoValor, valorNumerico (componen "valor"). moduloGrupo/alumnoGrupo/
//   fechaCalificacion/fechaUltimaModificacion son servidor → fuera de la whitelist.
@Override public AllowProperties allowPropertiesUpdate(); // createAllowProperties(tipoValor, valorNumerico)
```
**(4) Action Rules**
```java
// R-Nota-001 (CC-002, Antes de update): si la nota pasa de NO_EVALUADO a evaluada por primera vez
//   (original.tipoValor == NO_EVALUADO y nota.tipoValor != NO_EVALUADO) asigna fechaCalificacion = now.
//   Campo servidor; la comparación usa el estado persistido (original), no datos del cliente.
private void fireActionRule_RegistrarFechaCalificacion(Nota nota, Nota original);
// R-Nota-002 (CC-003, Antes de update): si la nota YA estaba evaluada (original.fechaCalificacion != null)
//   y el valor cambia, asigna fechaUltimaModificacion = now. Campo servidor.
private void fireActionRule_RegistrarFechaUltimaModificacion(Nota nota, Nota original);
```

### Paso 4 — Repositorios
No se crean repositorios personalizados. Los finders se declaran con `<finder-method>` en los dominios y se invocan casteando el `repository` heredado al repo generado: `((GrupoRepository) repository).findByCentroAndCursoAcademicoAndNombre(...)`, `((AlumnoGrupoRepository) repository).findByAlumnoAndCentroAndCursoAcademico(...)`, `((NotaRepository) repository).findByModuloGrupoAndTipoValor(...)`.

### Paso 5 — Controladores

#### `com.educaflow.system.grupos.controller.GrupoController`
```java
@Inject private ModelServiceFactory modelServiceFactory;

// Decide insert/update por la presencia de id; extrae el bean con allowPropertiesInsert()/Update()
//   del servicio y ejecuta validateInsert/validateUpdate; entrega los BusinessMessages como error.
@CallMethod public void validateSave(ActionRequest actionRequest, ActionResponse actionResponse);
// Extrae con allowPropertiesRemove(); ejecuta validateRemove (VAL-009).
@CallMethod public void validateDelete(ActionRequest actionRequest, ActionResponse actionResponse);
// Extrae con allowPropertiesCerrar() y llama a service.cerrar(...); errores como BusinessMessages.
@Transactional @CallMethod public void cerrar(ActionRequest actionRequest, ActionResponse actionResponse);
// Extrae con allowPropertiesReabrir() y llama a service.reabrir(...). La autorización (solo admin)
//   la impone VAL-008 en el servicio, NO el controlador.
@Transactional @CallMethod public void reabrir(ActionRequest actionRequest, ActionResponse actionResponse);
```

#### `com.educaflow.system.grupos.controller.AlumnoGrupoController`
```java
// validateSave (VAL-010..013) y validateDelete (VAL-014). Extrae con allowPropertiesInsert()/Remove().
@CallMethod public void validateSave(ActionRequest actionRequest, ActionResponse actionResponse);
@CallMethod public void validateDelete(ActionRequest actionRequest, ActionResponse actionResponse);
```

#### `com.educaflow.system.grupos.controller.NotaController`
```java
// validateSave (VAL-015..017). Extrae con allowPropertiesUpdate().
@CallMethod public void validateSave(ActionRequest actionRequest, ActionResponse actionResponse);
```

### Paso 6 — Vistas

Ver el XML completo en `design/views/*.xml` (validados contra `object-views.xsd`). Un `<action-view>` por fichero.

- **`design/views/Grupo.xml`** (pantalla supervisor): action-view `sysGrupos.Grupo@Main-action` con `<domain>self.centro = :centroActivoUsuario</domain>` (multi-centro). Grids `@Main` de Grupo (cursoAcademico, curso.ciclo, nombre, estado), ModuloGrupo, Nota (alumnoGrupo, valorTexto, fechas) y AlumnoGrupo (alumno, notaMedia). Forms `@Main`: Grupo (paneles Datos / Módulos / Alumnos; botones Guardar, Cerrar grupo [showIf ABIERTO — RUI-003], Borrar, Cancelar; readonlyIf CERRADO — RUI-004; onNew rellena estado/centro/cursoAcademico — RUI-001/002), ModuloGrupo (modal solo lectura con panel-related notas), Nota (modal; valor editable solo si grupo ABIERTO — RUI-005), AlumnoGrupo (modal; selector de alumno con domain por centro+tipo — ESC-020). Action-groups btnSave/btnDelete/btnCerrar/onNew, action-condition Local-validate (VAL-001/002/010/016 UX), action-record/action-attrs (valores iniciales, grupo padre, domain alumno) y action-method Remote-*.
- **`design/views/Grupo-admin.xml`** (pantalla administración): action-view `sysGrupos.Grupo@Admin-action` SIN domain de centro (ve todos). Igual cadena con discriminador `@Admin`, columna `centro` en el grid, `centro`/`cursoAcademico` editables al crear y readonly tras crear (RUI-006), botones Cerrar (RUI-007) y Reabrir (RUI-008), readonlyIf CERRADO salvo Reabrir (RUI-009), nota editable solo si ABIERTO (RUI-010).
- **`design/views/AlumnoGrupo-alumno.xml`** (pantalla «Mis notas»): action-view `sysGrupos.AlumnoGrupo@Alumno-action` con `<domain>self.alumno = :usuarioActual</domain>`. Grid de AlumnoGrupo (grupo.cursoAcademico, grupo.curso.ciclo, grupo.nombre, notaMedia) y grid de Nota (modulo, valorTexto, fechaCalificacion). Forms solo lectura (readonlyIf="true"); el alumno no crea ni modifica nada.

### Paso 7 — Menús
Modificar `src/main/java/com/educaflow/secretariavirtual/menus/menus.xml` fusionando la porción de `design/menus.xml`: raíz `notas-menuitem` («Notas», order 35) con hoja «Grupos» (action `sysGrupos.Grupo@Main-action`, `if` SUPERVISOR) y hoja «Mis notas» (action `sysGrupos.AlumnoGrupo@Alumno-action`, `if` ALUMNO); y hoja `administracionSv-grupos-menuitem` («Grupos (administración)», action `sysGrupos.Grupo@Admin-action`, `groups="admins"`) bajo la raíz existente `administracionSv-menuitem`.

### Paso 8 — Seguridad
Ver la sección «Frontera de confianza — AllowProperties por acción». Visibilidad por rol en los menús (`if`/`groups`). El filtrado multi-centro vive en los `<domain>` de los action-view (supervisor/alumno) y en las validaciones del servidor (VAL-012, R-Grupo-002).

### Paso 9 — Datos iniciales
Ninguno propio. El tipo de usuario `SUPERVISOR` y `ALUMNO` ya existen en `data-init/input/tiposUsuario.xml`.

### Paso 10 — Verificación final
Compilar con `./gradlew clean build`. Debe compilar sin errores y arrancar (`./run.sh`).

---

## Frontera de confianza — AllowProperties por acción

### `GrupoServiceImpl` (insert vía `GrupoController.validateSave` + `/ws/rest/Grupo`)
Entidad `Grupo`. **Forma elegida**: `createAllowProperties`. **Origen spec:** `Input AllowProperties` de la acción *Crear* de `entity-Grupo.md`.

| Campo | Origen | En whitelist (insert) | Justificación / Ubicación |
|---|---|---|---|
| `nombre` | cliente | sí | Input directo del usuario. |
| `curso` | cliente | sí | Input directo; inmutable tras crear. |
| `centro` | cliente | sí | Lo envía el admin; para el supervisor lo sobrescribe R-Grupo-002 (override condicional por rol). |
| `cursoAcademico` | cliente | sí | Ídem `centro`. |
| `alumnos` | cliente | sí | Input AllowProperties de *Crear* (alta inline opcional). El flujo canónico añade alumnos por el panel tras guardar (cada uno pasa por `AlumnoGrupoServiceImpl`). |
| `estado` | servidor | **NO** | R-Grupo-003 lo fija a ABIERTO incondicionalmente en `insert`. |
| `fechaCierre` | servidor | **NO** | Solo lo toca cerrar/reabrir. |
| `modulos` | servidor | **NO** | R-Grupo-001 los genera desde el curso. |

### `GrupoServiceImpl.update` (vía `GrupoController.validateSave` + `/ws/rest/Grupo`)
**Forma**: `createAllowProperties`. **Origen spec:** *Modificar* de `entity-Grupo.md`.

| Campo | Origen | En whitelist (update) | Justificación |
|---|---|---|---|
| `nombre` | cliente | sí | Único campo modificable. |
| `curso` / `centro` / `cursoAcademico` | cliente | **NO** | Inmutables tras crear (de ellos dependen módulos y notas). |
| `estado` / `fechaCierre` / `modulos` / `alumnos` | servidor | **NO** | Gestionados por el servidor / sus propias acciones. |

### `GrupoServiceImpl.cerrar` y `.reabrir` (vía `GrupoController.cerrar`/`reabrir`)
**Forma**: `createAllowProperties(Map.of())` (whitelist vacía). No aceptan ningún campo del cliente: `estado` y `fechaCierre` los asignan R-Grupo-004/005 en el servidor.

### `AlumnoGrupoServiceImpl.insert` (vía `AlumnoGrupoController.validateSave` + `/ws/rest/AlumnoGrupo`)
**Forma**: `createAllowProperties`. **Origen spec:** *Crear* de `entity-AlumnoGrupo.md`.

| Campo | Origen | En whitelist | Justificación |
|---|---|---|---|
| `alumno` | cliente | sí | Input directo del usuario (validado por VAL-012/013). |
| `grupo` | servidor | **NO** | Lo inyecta el modal hijo (`onNew __parent__`); el servidor lo toma del contexto. |
| `notas` | servidor | **NO** | R-AlumnoGrupo-001 las crea. |
| `notaMedia` | servidor (derivado) | **NO** | Campo transient calculado. |

### `NotaServiceImpl.update` (vía `NotaController.validateSave` + `/ws/rest/Nota`)
**Forma**: `createAllowProperties`. **Origen spec:** *Modificar* de `entity-Nota.md` (`valor`).

| Campo | Origen | En whitelist | Justificación |
|---|---|---|---|
| `tipoValor` | cliente | sí | Componente de «valor». |
| `valorNumerico` | cliente | sí | Componente de «valor». |
| `moduloGrupo` / `alumnoGrupo` | servidor | **NO** | Fijados al crear; no se cambian. |
| `fechaCalificacion` | servidor | **NO** | R-Nota-001 (CC-002). |
| `fechaUltimaModificacion` | servidor | **NO** | R-Nota-002 (CC-003). |

---

## Trazabilidad Origen spec → V/R/U → ubicación

### Validaciones (V)
| V | Origen spec | Ubicación |
|---|---|---|
| V-Grupo-001 | VAL-001 | `GrupoServiceImpl.validateInsert` (+ `nombre required` en el modelo) |
| V-Grupo-002 | VAL-002 | `GrupoServiceImpl.validateInsert` (+ `curso required`) |
| V-Grupo-003 | VAL-003, RES-001 | `GrupoServiceImpl.validateInsert` (finder) |
| V-Grupo-004 | VAL-004 | `GrupoServiceImpl.validateUpdate` |
| V-Grupo-005 | VAL-005, RES-001 | `GrupoServiceImpl.validateUpdate` (finder, excluye id) |
| V-Grupo-006 | VAL-006 | `GrupoServiceImpl.validateCerrar` |
| V-Grupo-007 | VAL-007 | `GrupoServiceImpl.validateReabrir` |
| V-Grupo-008 | VAL-008 | `GrupoServiceImpl.validateReabrir` (SecurityUtil.isAdmin) |
| V-Grupo-009 | VAL-009 | `GrupoServiceImpl.validateRemove` |
| V-AlumnoGrupo-001 | VAL-010 | `AlumnoGrupoServiceImpl.validateInsert` |
| V-AlumnoGrupo-002 | VAL-011 | `AlumnoGrupoServiceImpl.validateInsert` |
| V-AlumnoGrupo-003 | VAL-012 | `AlumnoGrupoServiceImpl.validateInsert` (+ domain UX en `AlumnoGrupo@*-form`) |
| V-AlumnoGrupo-004 | VAL-013, RES-004 | `AlumnoGrupoServiceImpl.validateInsert` (finder) |
| V-AlumnoGrupo-005 | VAL-014 | `AlumnoGrupoServiceImpl.validateRemove` |
| V-Nota-001 | VAL-015 | `NotaServiceImpl.validateUpdate` |
| V-Nota-002 | VAL-016 | `NotaServiceImpl.validateUpdate` (+ `valorNumerico min/max`) |
| V-Nota-003 | VAL-017 | `NotaServiceImpl.validateUpdate` (finder de conteo) |

### Reglas de negocio (R)
| R | Origen spec | Ubicación | Momento |
|---|---|---|---|
| R-Grupo-001 | RN-001, RES-002 | `GrupoServiceImpl.fireActionRule_GenerarModulosDelCurso` | Antes de Crear |
| R-Grupo-002 | RN-002 | `GrupoServiceImpl.fireActionRule_AsignarCentroYCursoAcademico` | Antes de Crear |
| R-Grupo-003 | — (Estado inicial ABIERTO) | `GrupoServiceImpl.fireActionRule_AsignarEstadoInicial` | Antes de Crear |
| R-Grupo-004 | RN-003 | `GrupoServiceImpl.fireActionRule_RegistrarCierre` | Antes de cerrar |
| R-Grupo-005 | RN-004 | `GrupoServiceImpl.fireActionRule_RegistrarReapertura` | Antes de reabrir |
| R-AlumnoGrupo-001 | RN-005 | `AlumnoGrupoServiceImpl.fireActionRule_CrearNotasNoEvaluado` | Después de Crear |
| R-Nota-001 | CC-002 | `NotaServiceImpl.fireActionRule_RegistrarFechaCalificacion` | Antes de update |
| R-Nota-002 | CC-003 | `NotaServiceImpl.fireActionRule_RegistrarFechaUltimaModificacion` | Antes de update |

### Campos calculados / derivados (CC)
| CC | Origen spec | Ubicación |
|---|---|---|
| CC-001 (nota media) | CC-001 | `AlumnoGrupo.notaMedia` (transient) → `NotaMediaCalculator.calcular` |
| CC-002 (fecha calificación) | CC-002 | campo `Nota.fechaCalificacion` (servidor) + R-Nota-001 |
| CC-003 (fecha últ. modificación) | CC-003 | campo `Nota.fechaUltimaModificacion` (servidor) + R-Nota-002 |

### Reglas de UI (U)
| U | Origen spec | Ubicación |
|---|---|---|
| U-grupos-supervisor-001 | RUI-001 | `Grupo.xml` onNew `set-valoresIniciales` (centro) + `field centro readonly` |
| U-grupos-supervisor-002 | RUI-002 | `Grupo.xml` onNew `set-valoresIniciales` (cursoAcademico) + `field cursoAcademico readonly` |
| U-grupos-supervisor-003 | RUI-003 | `Grupo.xml` botón `btnCerrar showIf estado=='ABIERTO'` |
| U-grupos-supervisor-004 | RUI-004 | `Grupo.xml` form `readonlyIf estado=='CERRADO'` |
| U-grupos-supervisor-005 | RUI-005 | `Grupo.xml` Nota: botón Guardar `showIf grupo ABIERTO` + valor `readonlyIf CERRADO` |
| U-grupos-administrador-001 | RUI-006 | `Grupo-admin.xml` centro/cursoAcademico `readonlyIf id!=null` (editables al crear) |
| U-grupos-administrador-002 | RUI-007 | `Grupo-admin.xml` botón `btnCerrar showIf ABIERTO` |
| U-grupos-administrador-003 | RUI-008 | `Grupo-admin.xml` botón `btnReabrir showIf CERRADO` |
| U-grupos-administrador-004 | RUI-009 | `Grupo-admin.xml` form `readonlyIf CERRADO` (Reabrir aparte) |
| U-grupos-administrador-005 | RUI-010 | `Grupo-admin.xml` Nota: Guardar `showIf ABIERTO` + valor `readonlyIf CERRADO` |

### Restricciones (RES) declarativas
| RES | Ubicación |
|---|---|
| RES-001 | `Grupo.xml` `unique-constraint(centro,cursoAcademico,nombre)` (+ V-Grupo-003/005) |
| RES-002 | Garantizada por R-Grupo-001 + módulos no editables |
| RES-003 | `ModuloGrupo.xml` `unique-constraint(grupo,modulo)` |
| RES-004 | V-AlumnoGrupo-004 (finder) |
| RES-005 | `AlumnoGrupo.xml` `unique-constraint(grupo,alumno)` |
| RES-006 | `Nota.xml` `unique-constraint(moduloGrupo,alumnoGrupo)` |

---

## Reglas del spec descartadas
Ninguna. Todas las `RES-`/`VAL-`/`RN-`/`RUI-`/`CC-` del spec están ubicadas en la matriz anterior.

---

## Notas de unificación
- **Override condicional de `centro`/`cursoAcademico`** (R-Grupo-002): son `cliente` (en la whitelist de insert porque el admin los envía) pero el servidor los sobrescribe para el supervisor. Es el patrón de k-secure-coding §4 (admin dropdown = cliente; no-admin = servidor); el override es por rol, no incondicional global.
- **`alumnos` en `allowPropertiesInsert` de Grupo**: incluido porque el spec lo lista en *Crear*. El flujo canónico (y los tests) añade alumnos por el panel tras guardar el grupo, de modo que cada `AlumnoGrupo` pasa por su servicio (VAL-010..013 y R-AlumnoGrupo-001). Si en el futuro se permite el alta en cascada de alumnos junto al grupo, hay que disparar esas validaciones desde `GrupoServiceImpl.insert`.
- **Doble `orphanRemoval` de `Nota`** (desde `ModuloGrupo` y `AlumnoGrupo`): necesario para que tanto «quitar alumno» como «borrar módulo» borren las notas. Verificar en implementación que Hibernate no emite errores de doble cascada; alternativa: dejar el `orphanRemoval` solo en `AlumnoGrupo.notas` y borrar las notas del módulo en `ModuloGrupoServiceImpl.remove`.
- **Población de campos transient** (`notaMedia`, `valorTexto`) en grids embebidos: se usa el patrón de campo transient con cuerpo `<![CDATA[]]>` (cf. `Centro.xml`). Si el render de un grid embebido no lo dispara, la alternativa documentada es un getter en `extra-code-model` o materializar `notaMedia` como campo persistido recalculado en una R-Después de `NotaServiceImpl.update`.
- **Binding del domain del selector de alumno** (ESC-020): se construye con `<action-attrs>` desde `onNew` usando `${__parent__?.centro?.id}`. Es UX; la garantía real es V-AlumnoGrupo-003 en el servidor.
- **Menús**: el spec ubica «Grupos» bajo «Notas» (supervisor) y «Mis notas» como entrada del alumno; se crea la raíz «Notas» con ambas hojas filtradas por rol y se reutiliza la raíz existente «Administración SV» para la pantalla de administración.
