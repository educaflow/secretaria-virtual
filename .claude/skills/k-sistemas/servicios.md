# Guía para desarrollar servicios de negocio en EducaFlow Secretaría Virtual

**NOTA: Aunque vamos a usar ejemplos de Subsistemas, todo lo explicado aquí es aplicable a cualquier capa.**

Un servicio de negocio en EducaFlow se compone de dos ficheros Java:
- **Interfaz** (`NombreService.java`) — define el contrato público, extiende `ModelService<T>`
- **Implementación** (`impl/NombreServiceImpl.java`) — extiende `DefaultModelService<T>` e implementa la interfaz

## Descubrimiento automático — sin registro en módulo

`ModelServiceFactory` descubre la implementación **por convención de nombre y paquete** — instancia la clase por reflexión sobre FQN fijos. Para la entidad `com.pkg.db.MiEntidad` busca **exactamente** estas cuatro clases, en este orden:

1. `com.pkg.service.MiEntidadService`
2. `com.pkg.service.MiEntidadServiceImpl`
3. `com.pkg.service.impl.MiEntidadService`
4. `com.pkg.service.impl.MiEntidadServiceImpl`

**CRITICAL** — el nombre de la clase de implementación **MUST** ser `<Entidad>ServiceImpl`. La factoría construye los FQN candidatos concatenando el nombre simple de la entidad + el sufijo `Service`/`ServiceImpl`; si la clase se llama de otra forma, la factoría **NO** la encuentra, `resolve(...)` devuelve `null` y el subsistema falla en runtime **sin error de compilación**.

El prefijo `Default` pertenece **solo** a la clase padre (`DefaultModelService<T>`). **MUST NOT** anteponerlo al nombre de la implementación concreta.

- ✅ CORRECTO: `CorreoServiceImpl extends DefaultModelService<Correo> implements CorreoService`
- ❌ INCORRECTO: `DefaultCorreoService` (lleva el prefijo `Default` del padre y le falta el sufijo `Impl` — la factoría no lo encuentra)
- ❌ INCORRECTO: `DefaultCorreoServiceImpl` (mezcla el prefijo `Default` del padre con el sufijo `Impl` de la implementación — la factoría no lo encuentra)
- ❌ INCORRECTO: `CorreoSvcImpl` (sufijo abreviado no contemplado por la factoría)

**No hace falta ningún fichero de módulo ni binding explícito.** La implementación **MUST** estar en uno de esos cuatro paquetes con **ese nombre exacto** y tener el constructor obligatorio (ver abajo).

## Estructura de la interfaz

```java
package com.educaflow.subsystem.SUBSYSTEM.service;

import com.axelor.db.modelservice.ModelService;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.subsystem.SUBSYSTEM.db.MiEntidad;

import java.util.Optional;

public interface MiEntidadService extends ModelService<MiEntidad> {

    // Acciones propias del subsistema (además de insert/update/remove, ya
    // expuestos por Axelor vía /ws/rest/<FQN> y heredados de ModelService<T>).
    MiEntidad hacerAlgoEspecial(MiEntidad entidad, MiEntidad original);

    // Validaciones — UNA por cada acción propia del subsistema. NO se
    // declaran validateInsert/Update/Remove aquí: DefaultModelService ya las
    // implementa por defecto (Optional.empty()).
    Optional<BusinessMessages> validateHacerAlgoEspecial(MiEntidad entidad, MiEntidad original);

    // AllowProperties — UNA por cada acción del subsistema que sea invocada
    // desde un @CallMethod del controlador propio. NO se declara para
    // insert/update/remove: el endpoint REST automático de Axelor no pasa
    // por el controlador propio, y DefaultModelService ya trae sus defaults.
    AllowProperties allowPropertiesHacerAlgoEspecial();
}
```

> ⚠️ **Regla obligatoria**: la interfaz **MUST** declarar cada acción propia del subsistema `miAccion(...)` **junto con su validador** `validateMiAccion(...)` y, si esa acción se invoca desde el controlador propio, también **junto con su** `allowPropertiesMiAccion()`. La tripleta `acción + validador + allowProperties` es el contrato público de la acción.
>
> **¿Y para `insert` / `update` / `remove`?** **MUST NOT** re-declararlas, ni a ellas ni a sus `validateInsert/Update/Remove` ni a sus `allowPropertiesInsert/Update/Remove`. Estas acciones se invocan **siempre** desde el endpoint REST automático de Axelor (`/ws/rest/<FQN>`), nunca desde un `@CallMethod` del controlador propio. `ModelService<T>` ya declara las firmas y `DefaultModelService<T>` provee implementaciones por defecto (`Optional.empty()` para los `validate*`, defaults razonables para los `allowProperties*`, y el patrón `validate → repository.save/remove` dentro de `insert/update/remove`). Solo se sobrescribe en la `*Impl` lo que **realmente** se quiera cambiar.
>
> **MUST NOT** olvidar declarar el `validateMiAccion(...)` ni el `allowPropertiesMiAccion()` correspondientes a una acción nueva del subsistema invocada desde el controlador propio. Sin ellos el contrato queda incompleto y el controlador no puede aplicar la whitelist.

> **Nota sobre `BusinessMessage` / `BusinessMessages`**: son clases del framework Axelor (`com.axelor.db.modelservice.*`), no del proyecto. Siempre se importan desde ese paquete.

Los métodos declarados en `ModelService<T>` que ya hereda la interfaz son:
- `T insert(T entity)`
- `T update(T entity, T original)`
- `void remove(T entity)`
- `Optional<BusinessMessages> validateInsert(T entity)`
- `Optional<BusinessMessages> validateUpdate(T entity, T original)`
- `Optional<BusinessMessages> validateRemove(T entity)`
- `AllowProperties allowPropertiesInsert()`
- `AllowProperties allowPropertiesUpdate()`
- `AllowProperties allowPropertiesRemove()`
- `Map<String, Object> validate(Map<String, Object> json, Map<String, Object> context)`

Todas estas firmas se heredan tal cual desde `ModelService<T>` y vienen implementadas por defecto en `DefaultModelService<T>`. **MUST NOT** re-declararlas en el interface del subsistema ni sobrescribirlas en la `*Impl` salvo que se quiera añadir lógica específica.

## Patrón validate + throw

**MUST** que **toda** acción que escribas en la `*Impl` —tanto las acciones propias del subsistema como las sobrescrituras de `insert`/`update`/`remove`— empiece llamando a su validador correspondiente y dispare la excepción de negocio si la validación falla, usando la utilidad `BusinessMessages::throwIfInvalid`.

**Motivo**: una acción puede ser invocada desde dos vías distintas:
- **Caso A — código directo** (otro servicio, job, etc.): nada valida automáticamente; debe hacerlo el propio servicio.
- **Caso B — controller `@CallMethod`**: el UI llamó (o debería haber llamado) a `<action-validate>` antes; si llega al servicio con datos inválidos es un bug del controlador o un intento malicioso.

**¿Y para `insert` / `update` / `remove`?** Hay dos situaciones:
- **No los sobrescribes** → los hereda `DefaultModelService`, que ya hace `validateXxx(...).ifPresent(throwIfInvalid)` + `repository.save/remove` por ti. **MUST NOT** sobrescribirlos solo para replicar eso.
- **Los sobrescribes** (para añadir action rules, decorar el bean, etc.) → como ya **NO** se llama a `super.insert/update/remove` (ver §"Persistir: siempre `repository`, nunca `super.*`"), **MUST** poner tú mismo `validateXxx(...).ifPresent(throwIfInvalid)` como primera línea y persistir con `repository.save/remove`. Aplican exactamente el mismo patrón que cualquier otra acción.

**Forma canónica** (toda acción de la `*Impl`):

```java
@Override
public MiEntidad miAccion(MiEntidad entidad, MiEntidad entidadOriginal) {
    validateMiAccion(entidad, entidadOriginal).ifPresent(BusinessMessages::throwIfInvalid);

    // …action rules…
    entidad = repository.save(entidad);   // persiste con repository, NUNCA super.insert/update/remove
    // …action rules…
    return entidad;
}
```

✅ CORRECTO: la primera línea ejecutable del método es `validateMiAccion(...).ifPresent(BusinessMessages::throwIfInvalid)`.
✅ CORRECTO: la acción persiste con `repository.save(...)` / `repository.remove(...)` (ver §"Persistir: siempre `repository`, nunca `super.*`").
❌ INCORRECTO: la acción modifica el bean antes de validar; un valor inválido pasa por la validación parcialmente modificado.
❌ INCORRECTO: reimplementar el patrón con `throw new IllegalArgumentException(...)` — `BusinessMessages::throwIfInvalid` ya lo encapsula y mantiene la semántica de errores de negocio.
❌ INCORRECTO: persistir con `super.insert/update/remove` — está prohibido en la `*Impl` (ver §"Persistir: siempre `repository`, nunca `super.*`").

## Persistir: siempre `repository`, nunca `super.*`

> **CRITICAL** — En un `*ServiceImpl` **MUST NOT** llamar **nunca** a `super.insert` / `super.update` / `super.remove`. **Toda** acción que persista —las sobrescrituras de `insert`/`update`/`remove`, sus overloads (`insert(MiEntidadInsertDTO)`) y las acciones propias (`marcarComoFirmada`, `cambiarEstado`, `reenviar`, …)— sigue el **mismo patrón**: `validateXxx(...).ifPresent(throwIfInvalid)` como primera línea y luego `repository.save(entidad)` (alta/modificación) o `repository.remove(entidad)` (baja).

**Motivo 1 — coherencia y no duplicar la validación.** `super.insert/update/remove` **no** son un "persistir" neutro: en `DefaultModelService` su cuerpo es `validateXxx(...).ifPresent(throwIfInvalid)` **+** `repository.save/remove`. Si sobrescribes el método, tú ya ejecutas el `validateXxx` en la primera línea; llamar después a `super.*` **repetiría** esa misma validación. Persistiendo siempre con `repository` queda **una sola forma** de escribir cualquier acción: validas y llamas al repositorio.

**Motivo 2 — cada acción tiene su propio validador.** Una acción propia tiene su **propia tripleta** `acción + validador + allowProperties`. `marcarComoFirmada` ya valida con `validateMarcarComoFirmada` (primera línea) y restringe el mass-assignment con `allowPropertiesMarcarComoFirmada`. Si llamara a `super.update(...)`, le impondría además `validateUpdate` — la validación de **otra** acción (la genérica `update`), diseñada para un contrato distinto. Aplicar la validación de otra acción no tiene sentido y puede bloquear el flujo legítimo o validar campos que esta acción nunca toca.

> **CRITICAL — responsabilidad del autor**: como ya **no** llamas a `super.*`, la "salvaguarda" automática de `DefaultModelService` (validar antes de persistir) **solo** existe para los métodos que **no** sobrescribes. En cuanto sobrescribes `insert`/`update`/`remove`, **MUST** poner tú el `validateXxx(...).ifPresent(throwIfInvalid)` como primera línea: si lo olvidas, persistes sin validar.

**No hay riesgo de mass-assignment** por usar `repository.save(...)`: la única vía por la que un **cliente** puede invocar una acción es el `@CallMethod` del controlador (que filtra el bean con `allowPropertiesXxx()`) o el endpoint REST genérico (que pasa por `validate(json, context)`, el cual también filtra con `allowPropertiesInsert/Update`). Si la acción la invoca código servidor de confianza (otro servicio, un job), no hay cliente que filtrar. En todos los casos al `repository.save(...)` le llega un bean ya seguro; `repository.save(...)` solo persiste.

- ✅ CORRECTO: `insert` sobrescrito → `validateInsert(entidad).ifPresent(BusinessMessages::throwIfInvalid); ... return repository.save(entidad);`
- ✅ CORRECTO: dentro de `marcarComoFirmada(...)` → `tareaFirma = repository.save(tareaFirma);`
- ✅ CORRECTO: dentro de `insert(MiEntidadInsertDTO dto)` → `return repository.save(entidad);` (tras `validateInsert(dto).ifPresent(...)`).
- ❌ INCORRECTO: dentro de un `insert`/`update`/`remove` sobrescrito → `super.insert/update/remove(...)` (prohibido; usa `repository` + tu `validateXxx` al inicio).
- ❌ INCORRECTO: dentro de `marcarComoFirmada(...)` → `super.update(tareaFirma, original);` (arrastra `validateUpdate`, validación de otra acción).
- ❌ INCORRECTO: dentro de una acción `borrarBorrador(...)` → `super.remove(entidad);` (debe ser `repository.remove(entidad)`).

## Estructura de la implementación

La implementación **MUST** ordenar sus métodos en estos cinco bloques, en este orden:

1. **Acciones** (sin header) — métodos `public` que ejecutan la lógica de negocio: las acciones propias del subsistema y, **solo si se quieren sobrescribir**, `insert` / `update` / `remove`. **Toda** acción —propia o sobrescritura de `insert/update/remove`— **MUST** empezar con el patrón validate + throw (ver §"Patrón validate + throw") y persistir con `repository.save/remove`, **nunca** con `super.insert/update/remove` (ver §"Persistir: siempre `repository`, nunca `super.*`").
2. **Métodos de Validación** (con header) — métodos `public` que devuelven `Optional<BusinessMessages>`. Uno por cada acción propia del subsistema declarada en el interface. **NO** se sobrescriben `validateInsert/Update/Remove` salvo que se quieran añadir reglas: el default `Optional.empty()` ya viene de `DefaultModelService`.
3. **AllowProperties** (con header) — métodos `public` que devuelven `AllowProperties`. Uno por cada acción propia del subsistema **invocada desde un `@CallMethod` del controlador propio**. **NO** se sobrescriben `allowPropertiesInsert/Update/Remove` salvo que se quieran restringir: los defaults vienen de `DefaultModelService`. Las reglas de qué forma usar (`createAllowProperties` vs `createAllowAllProperties`) y de cómo tratar los campos `servidor` en la acción están en `[[k-secure-coding]]` §3 — **CRITICAL**.
4. **Action Rules** (con header) — métodos `private` cuyo nombre empieza por `fireActionRule_`. Encapsulan las reglas de negocio que ejecuta cada acción.
5. **Otras funciones** (con header) — helpers `private` que no son ni validaciones, ni allow-properties, ni action rules: utilidades internas, conversiones, builders, métodos compartidos.

Los headers son tres líneas de comentario `/************...************/`. **MUST** que las 3 líneas de un mismo bloque tengan **exactamente el mismo número de caracteres** (ajustar con `*` si difieren). Verifica con:

```bash
awk '/\/\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*/{print NR": "length($0)}' <fichero>
```

Cada header puede tener una longitud distinta de los otros — lo que importa es la coherencia interna de las 3 líneas de cada bloque.

```java
package com.educaflow.subsystem.SUBSYSTEM.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.db.modelservice.BusinessMessage;
import com.axelor.db.modelservice.BusinessMessages;
import com.educaflow.base.util.AllowProperties;
import com.educaflow.subsystem.SUBSYSTEM.db.MiEntidad;
import com.educaflow.subsystem.SUBSYSTEM.service.MiEntidadService;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.Optional;

public class MiEntidadServiceImpl extends DefaultModelService<MiEntidad> implements MiEntidadService {

    // Repositorios adicionales (NO servicios) se pueden inyectar como campos con @Inject
    @Inject
    OtroRepositorio otroRepositorio;

    // Constructor obligatorio — ModelServiceFactory lo invoca por reflexión
    public MiEntidadServiceImpl(Class<MiEntidad> model, Repository<MiEntidad> repository) {
        super(model, repository);
    }

    // NOTA: NO se sobrescriben insert/update/remove salvo que se quiera añadir
    // lógica propia (p.ej. disparar una action rule, decorar el bean). Si NO los
    // sobrescribes, DefaultModelService ya hace `validate + repository.save/remove`.
    // Si SÍ los sobrescribes, MUST poner tú el validateXxx().ifPresent(throwIfInvalid)
    // como primera línea y persistir con repository — NUNCA super.insert/update/remove.

    @Override
    public MiEntidad update(MiEntidad entidad, MiEntidad entidadOriginal) {
        validateUpdate(entidad, entidadOriginal).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_AsignarCampoCalculado(entidad);
        entidad = repository.save(entidad);   // sobrescritura de update → repository, NUNCA super.update
        fireActionRule_NotificarCambio(entidad);
        return entidad;
    }

    @Override
    public MiEntidad hacerAlgoEspecial(MiEntidad entidad, MiEntidad entidadOriginal) {
        validateHacerAlgoEspecial(entidad, entidadOriginal).ifPresent(BusinessMessages::throwIfInvalid);

        fireActionRule_AsignarCampoCalculado(entidad);
        entidad = repository.save(entidad);   // acción propia → repository, NUNCA super.*
        fireActionRule_NotificarCambio(entidad);
        return entidad;
    }

    /****************************************************************************************/
    /******************************** Métodos de Validación *********************************/
    /****************************************************************************************/

    // NOTA: NO se sobrescriben validateInsert/Update/Remove salvo que haya reglas.
    // DefaultModelService devuelve Optional.empty() por defecto.

    @Override
    public Optional<BusinessMessages> validateHacerAlgoEspecial(MiEntidad entidad, MiEntidad entidadOriginal) {
        BusinessMessages messages = new BusinessMessages();

        if (entidad.getCampoA() == null) {
            messages.add(new BusinessMessage("campoA", "Es requerido"));
        }
        if (entidad.getCampoB() != null && entidad.getCampoB().isBlank()) {
            messages.add(new BusinessMessage("campoB", "No puede estar vacío"));
        }

        return messages.isValid() ? Optional.empty() : Optional.of(messages);
    }

    /**************************************************************************************/
    /********************************   AllowProperties   *********************************/
    /**************************************************************************************/

    // NOTA: NO se sobrescriben allowPropertiesInsert/Update/Remove salvo que se quiera
    // restringir. Reglas de elección en [[k-secure-coding]] §3.

    @Override
    public AllowProperties allowPropertiesHacerAlgoEspecial() {
        return AllowProperties.createAllowProperties(Map.of(
            "campoA", Map.of(),
            "campoB", Map.of()
        ));
    }

    /*************************************************************************************/
    /********************************    Action Rules    *********************************/
    /*************************************************************************************/

    private void fireActionRule_AsignarCampoCalculado(MiEntidad entidad) {
        // efecto secundario: asignar un valor derivado de otros campos
    }

    private void fireActionRule_NotificarCambio(MiEntidad entidad) {
        // efecto secundario: notificar tras persistir
    }

    /*************************************************************************************/
    /********************************    Otras funciones    ******************************/
    /*************************************************************************************/

    private String formatearCampoX(MiEntidad entidad) {
        // helper interno: ni validación ni regla de negocio
        return entidad.getCampoX() == null ? "" : entidad.getCampoX().trim().toUpperCase();
    }

    private boolean cumpleCondicionTecnica(MiEntidad entidad) {
        // otro helper interno
        return entidad.getCampoY() != null;
    }
}
```

## Constructor obligatorio

`ModelServiceFactory` instancia el servicio **por reflexión** buscando exactamente este constructor:

```java
public MiEntidadServiceImpl(Class<MiEntidad> model, Repository<MiEntidad> repository) {
    super(model, repository);
}
```

Reglas del constructor:
- `Repository` **siempre** lleva el tipo genérico: `Repository<MiEntidad>` (nunca `Repository` sin tipo).
- El `super()` recibe el parámetro `model` tal cual — **no** hardcodear `MiEntidad.class` ni hacer `(MiEntidadRepository) repository`. La forma canónica es exactamente `super(model, repository);`.
- Si el constructor no existe, la factoría lanza `IllegalStateException`.
- Las dependencias adicionales **no van en el constructor**: se declaran como campos `@Inject` y Guice las inyecta después de construir el objeto.

## Usar `repository` en los métodos

El `repository` pasado al constructor queda disponible como campo protegido heredado de `DefaultModelService`. **Úsalo directamente** — no vuelvas a crearlo con `JpaRepository.of(MiEntidad.class)`:

```java
// MAL — crear otro repository para la misma entidad
List<MiEntidad> activos = JpaRepository.of(MiEntidad.class).all()
        .filter("self.activo = true").fetch();

// BIEN — delegar en un finder del repositorio heredado
List<MiEntidad> activos = ((MiEntidadRepository) repository).findActivos();
```

`JpaRepository.of(OtraEntidad.class)` sí es válido cuando necesitas consultar una entidad **diferente** a la que gestiona el servicio.

## Las consultas con filtros van en el repositorio, nunca en el servicio

**REGLA CRÍTICA:** Cualquier consulta JPA que use `.filter()` / `.bind()` **no va inline en el servicio**. Pertenece al repositorio como un `<finder-method>` en el XML de dominio o como un método en el repositorio personalizado.

```java
// MAL — consulta JPA inline en el servicio
CertificadoDigital certificado = repository.all()
        .filter("self.dni = :dni")
        .bind("dni", dni)
        .fetchOne();

// BIEN — delegar en un finder definido en el repositorio (con cast al tipo específico)
CertificadoDigital certificado = ((CertificadoDigitalRepository) repository).findByDni(dni);
```

El campo `repository` heredado de `DefaultModelService` es de tipo `Repository<T>` (interfaz genérica). El método `findByDni` se genera en la clase concreta `MiEntidadRepository` del paquete `db/repo/`. Por eso es necesario el cast: `((MiEntidadRepository) repository).findByMethod(param)`.

Para definir el finder, añade `<finder-method>` en el XML de dominio de la entidad:

```xml
<finder-method name="findByDni" using="String:dni" filter="self.dni = :dni" />
```

Axelor genera automáticamente el método `findByDni(String dni)` en `MiEntidadRepository` (en `build/src-gen/.../db/repo/`). Si el finder necesita lógica más compleja que no cabe en el XML, crea un repositorio personalizado en `src/.../db/repo/MiEntidadRepository.java` que extienda el generado y añade el método allí.

## DTO de inserción (cuando el insert necesita datos especiales)

Cuando la creación necesita parámetros que no coinciden exactamente con la entidad, se usa un `record` DTO en el mismo paquete del servicio:

```java
// MiEntidadInsertDTO.java — junto a MiEntidadService.java
package com.educaflow.subsystem.SUBSYSTEM.service;

import java.util.Objects;

public record MiEntidadInsertDTO(String campo1, OtraEntidad relacion) {

    public MiEntidadInsertDTO {
        Objects.requireNonNull(campo1, "campo1 no puede ser null");
        Objects.requireNonNull(relacion, "relacion no puede ser null");
    }
}
```

La interfaz del servicio declara la acción **y su validador**:

```java
public interface MiEntidadService extends ModelService<MiEntidad> {
    MiEntidad insert(MiEntidadInsertDTO dto);
    Optional<BusinessMessages> validateInsert(MiEntidadInsertDTO dto);
}
```

La implementación aplica el patrón validate + throw con el validador de **esta** acción (`validateInsert(dto)`), construye la entidad y persiste con `repository.save()` — **nunca** `super.insert` (ver §"Persistir: siempre `repository`, nunca `super.*`"). La `validateInsert(MiEntidad)` (validador del overload de entidad) **no** corre aquí: el validador propio de esta acción es `validateInsert(dto)`, ya ejecutado:

```java
@Override
public MiEntidad insert(MiEntidadInsertDTO dto) {
    validateInsert(dto).ifPresent(BusinessMessages::throwIfInvalid);

    MiEntidad entidad = new MiEntidad();
    entidad.setCampo1(dto.campo1());
    entidad.setRelacion(dto.relacion());
    // ...
    return repository.save(entidad);
}

@Override
public Optional<BusinessMessages> validateInsert(MiEntidadInsertDTO dto) {
    BusinessMessages messages = new BusinessMessages();
    // Validaciones específicas del DTO (formato, presencia de campos no nullables, etc.)
    return messages.isValid() ? Optional.empty() : Optional.of(messages);
}
```

> **Nota**: `validateInsert(MiEntidadInsertDTO)` y `validateInsert(MiEntidad)` son overloads distintos. Cada uno valida la entrada de su correspondiente acción. Ambos van en el interface y en la impl.

## Convenciones clave

### Nombres y emparejamiento acción ↔ validador ↔ allowProperties
- Por cada acción propia del subsistema `miAccion(parametros)` **MUST** existir un método `validateMiAccion(parametros)` con **la misma firma de parámetros** y retorno `Optional<BusinessMessages>`. Ambos van en el interface; ambos se implementan en el `*ServiceImpl`.
- Si esa acción se invoca desde un `@CallMethod` del controlador propio, **MUST** además existir `allowPropertiesMiAccion()` (sin parámetros, retorno `AllowProperties`). También en el interface y en la `*Impl`.
- Las acciones `insert` / `update` / `remove` heredadas de `ModelService<T>` ya tienen su validador (`validateInsert`/`validateUpdate`/`validateRemove`) y su `allowProperties*` con defaults en `DefaultModelService`. **MUST NOT** re-declararlos en el interface ni sobrescribirlos en la `*Impl` salvo que se añada lógica real.
- `fireActionRule_NombreRegla(...)` — efecto secundario (asignar datos, notificar, callback). Se llama antes o después de persistir, **dentro de la acción correspondiente**.

### Patrón validate + throw en cada acción
- **Toda** acción `public` (propia o sobrescritura de `insert/update/remove`) **MUST** empezar con `validateXxx(...).ifPresent(BusinessMessages::throwIfInvalid);`. Ver §"Patrón validate + throw" arriba.
- Si sobrescribes `insert/update/remove`, **MUST** poner tú el `validateXxx(...).ifPresent(throwIfInvalid)` como primera línea: como ya **no** llamas a `super.*`, nadie más lo hace por ti.
- **MUST NOT** usar `throw new IllegalArgumentException(...)` para este patrón — la forma canónica es `BusinessMessages::throwIfInvalid`.

### Errores de negocio
Los métodos de validación **nunca lanzan `BusinessException`**: acumulan en `BusinessMessages` y devuelven `Optional`. El controlador decide cómo mostrar los errores.

Un solo error:
```java
messages.add(new BusinessMessage("campo", "Mensaje del error"));
```

Varios errores:
```java
messages.add(new BusinessMessage("campoA", "Es requerido"));
messages.add(new BusinessMessage("campoB", "No puede estar vacío"));
return messages.isValid() ? Optional.empty() : Optional.of(messages);
```

### Obtener otro servicio desde un servicio

**Esta regla aplica ÚNICAMENTE a `ModelService`** (servicios que extienden `ModelService<T>` / `DefaultModelService<T>` y viven en `service.impl`). Para clases que **no** son `ModelService` (infraestructura, callbacks, helpers, implementaciones de interfaces de terceros, etc.) sí está permitido inyectar con `@Inject` y registrar el binding en un módulo Guice del paquete `module/` del subsistema.

**NUNCA** inyectar un `ModelService` con `@Inject` — ni dentro de otro servicio ni en un controlador. Siempre se usa `ModelServiceFactory`:

```java
// MAL — prohibido para ModelService
@Inject
OtroModelService otroServicio;

// BIEN — inyectar ModelServiceFactory y resolver en el método
@Inject
ModelServiceFactory modelServiceFactory;

// Dentro del método que lo necesite:
final OtroModelService otroServicio = (OtroModelService) modelServiceFactory.resolve(OtraEntidad.class);
```

Para clases que **no son `ModelService`** (p. ej. `MailSender`, un `AlmacenClaveLoader`, un cliente HTTP de terceros…) el patrón normal de Guice es válido:

```java
// BIEN — clase que NO es ModelService: @Inject + binding en el módulo del subsistema
@Inject
private MailSender mailSender;
```

Con su binding correspondiente en `module/<Subsistema>Module.java` (`bind(MailSender.class).to(MailSenderImpl.class)`, `@Provides`, etc.) — sin necesidad de registrarlo en `SecretariaVirtualModule`: el módulo extiende `AxelorModule` y Axelor lo descubre automáticamente.

## `allowPropertiesXxx` y campos `servidor`

> **CRITICAL** → la decisión sobre qué forma usar (`createAllowProperties` vs `createAllowAllProperties`) y cómo asignar incondicionalmente los campos `servidor` en la acción está descrita en `[[k-secure-coding]]` §3. Es lectura **obligatoria** al implementar o revisar cualquier `allowPropertiesXxx` o cualquier acción que toque campos `servidor`.

Estructuralmente:

- El interface declara `AllowProperties allowPropertiesMiAccion()` para cada acción invocada desde un `@CallMethod`.
- La `*Impl` ubica su implementación en el bloque (3) `AllowProperties` (ver §"Estructura de la implementación").
- El controlador la consume con `service.allowPropertiesMiAccion()` (nunca construye el `AllowProperties` inline con `Map.of(...)`).


## Checklist 
Checklist única para desarrollar y revisar `*Service` / `*ServiceImpl`. Cada ítem es un tipo de problema concreto observado en revisiones reales o una regla obligatoria del patrón.

### Estructura básica

- [ ] La interfaz extiende `ModelService<T>` del paquete `com.axelor.db.modelservice`.
- [ ] La implementación extiende `DefaultModelService<T>` e implementa la interfaz.
- [ ] La implementación está en `service.impl.MiEntidadServiceImpl` para que `ModelServiceFactory` la descubra sin registro explícito.
- [ ] El constructor tiene la firma `(Class<T> model, Repository<T> repository)` y llama a `super(model, repository)`.

### Interface (`*Service`)

- [ ] Por cada acción propia del subsistema `miAccion(...)` está declarado su `validateMiAccion(...)` con la **misma firma de parámetros** y retorno `Optional<BusinessMessages>`.
- [ ] Por cada acción propia invocada desde un `@CallMethod` del controlador propio está declarado su `allowPropertiesMiAccion()` (retorno `AllowProperties`).
- [ ] **NO** re-declara `validateInsert/Update/Remove` ni `allowPropertiesInsert/Update/Remove`: vienen de `ModelService<T>` con defaults en `DefaultModelService<T>`.
- [ ] **NO** declara `validateXxx` / `allowPropertiesXxx` cuyo cuerpo en la `*Impl` será un stub vacío (`Optional.empty()` / default). Esos casos se quedan con el heredado.
- [ ] **NO** tiene acciones cuyo retorno `Optional<BusinessMessages>` las haga "validadores disfrazados" duplicando el `validate*` del par. O es acción, o es validador.
- [ ] Si el insert necesita parámetros especiales, se crea un `record` DTO en el paquete del servicio.

### Implementación (`*ServiceImpl`)

- [ ] **NO** sobrescribe `insert/update/remove` salvo que añada lógica real (`fireActionRule_*`, decoración del bean). Si no la añade, los hereda de `DefaultModelService` (que ya hace `validate + repository.save/remove`).
- [ ] Si sobrescribe `insert/update/remove`, **SÍ** pone `validateXxx().ifPresent(throwIfInvalid)` como primera línea y persiste con `repository.save/remove` — **NUNCA** `super.insert/update/remove`.
- [ ] Tiene `@Override` en cada método que sobrescribe del interface o de la clase padre (acciones, `validateXxx`, `allowPropertiesXxx`).
- [ ] Cada acción propia empieza con `validateMiAccion(...).ifPresent(BusinessMessages::throwIfInvalid);`. **NO** usa `throw new IllegalArgumentException(...)` ni `BusinessException` directamente para esto.
- [ ] La acción **NO** incluye comprobaciones inline (`if (...) throw new BusinessException(...)`) que pertenecen al validador. Se delegan a `validateMiAccion` y se acumulan en `BusinessMessages`.
- [ ] Las validaciones devuelven `Optional<BusinessMessages>`. **Nunca** lanzan `BusinessException` ni `IllegalArgumentException`.
- [ ] Cada acción que persiste lo hace de verdad (**NO** devuelve la entidad sin persistir) con `repository.save(...)` / `repository.remove(...)`. **NUNCA** llama a `super.insert/update/remove` (prohibido en toda la `*Impl`, también en las sobrescrituras de `insert/update/remove` y sus overloads). Ver §"Persistir: siempre `repository`, nunca `super.*`".
- [ ] Los `ModelService` adicionales **nunca** se inyectan con `@Inject` — se obtienen con `modelServiceFactory.resolve(OtraEntidad.class)`. Las dependencias que **no** son `ModelService` (infraestructura, terceros) sí pueden inyectarse con `@Inject`.
- [ ] Las consultas JPA con `.filter()/.bind()` **nunca** están inline — se definen como `<finder>` en el XML de dominio o como métodos en el repositorio.

### Orden de los métodos en la `*Impl`

- [ ] 5 bloques en este orden: (1) acciones (sin header), (2) `Métodos de Validación`, (3) `AllowProperties`, (4) `Action Rules`, (5) `Otras funciones`.
- [ ] No falta ningún header cuando el bloque tiene métodos. Ningún bloque está fuera de sitio.
- [ ] Los headers `/************…************/` tienen 3 líneas de la **misma longitud** dentro de cada bloque (verifica con `awk '/\/\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*\*/{print NR": "length($0)}' <fichero>`).
- [ ] Los helpers privados que devuelven `Optional<BusinessMessages>` están en `Métodos de Validación`, no en `Otras funciones`.
- [ ] Ningún método tiene `;` sobrante tras la `}` de cierre.
- [ ] Nomenclatura `fireActionRule_<NombreEnPascalCase>` consistente (no `fireActionRule_asignarCosa` con minúscula tras el guion bajo).

### Seguridad — `allowPropertiesXxx` y campos `servidor`

- [ ] Las reglas de elección (`createAllowProperties` whitelist vs `createAllowAllProperties` abierto) y de asignación incondicional de campos `servidor` están en `[[k-secure-coding]]` §3 y **MUST** aplicarse. Aplica los detectores mecánicos del §3.4 al revisar.
