---
type: implementation-task
template: system
---

# Tarea 08 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

**Alcance de esta tarea:** crear la interfaz `TipoExpedienteService` y su implementación `TipoExpedienteServiceImpl` (dos filas `Crear`, un único componente: servicio con su impl). Es Java: se materializa delegando en `developer-code-implementer` (`implementation.md` §2). **Superficie cerrada:** solo lo que el diseño lista (la interfaz es vacía a propósito). El paquete `service/` (singular) en el motor es **deliberado**: lo impone `ModelServiceFactory` (D5); la Nota 1 del diseño pide que el humano lo confirme, pero **no** bloquea la implementación (decisión de esta descomposición: se implementa tal como está diseñado).

**Del diseño — filas de la tabla «Ficheros a crear o modificar» (verbatim)**

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/expedientes/service/TipoExpedienteService.java` | Crear | k-sistemas (servicios.md) | `ModelService` de `TipoExpediente` (Paso 4) |
| `src/main/java/com/educaflow/subsystem/expedientes/service/impl/TipoExpedienteServiceImpl.java` | Crear | k-sistemas (servicios.md), k-secure-coding §9.2 | Rechaza toda escritura de tipos de expediente (Paso 4) |

**Del diseño — Paso 4 (verbatim)**

### Paso 4 — `TipoExpediente` de solo consulta en el servidor

**Crear** en `subsystem/expedientes` (la ubicación la impone `ModelServiceFactory`: `…expedientes.db.TipoExpediente` → `…expedientes.service[.impl]`; D5):

```java
// Clase: com.educaflow.subsystem.expedientes.service.TipoExpedienteService
public interface TipoExpedienteService extends ModelService<TipoExpediente> {}
//   Sin acciones propias: solo existe para que ModelServiceFactory encuentre la implementación.

// Clase: com.educaflow.subsystem.expedientes.service.impl.TipoExpedienteServiceImpl
public class TipoExpedienteServiceImpl extends DefaultModelService<TipoExpediente> implements TipoExpedienteService {

    public TipoExpedienteServiceImpl(Class<TipoExpediente> model, Repository<TipoExpediente> repository);
    //   super(model, repository).

    @Override public TipoExpediente insert(TipoExpediente tipoExpediente);
    @Override public TipoExpediente update(TipoExpediente tipoExpediente, TipoExpediente original);
    @Override public void remove(TipoExpediente tipoExpediente);
    //   Los tres lanzan UnsupportedOperationException incondicional (k-secure-coding §9.2): la entidad nunca
    //   admite la operación, así que no hay flujo normal; es el cinturón si alguien se salta el validate*.

    @Override public Optional<BusinessMessages> validateInsert(TipoExpediente tipoExpediente);
    @Override public Optional<BusinessMessages> validateUpdate(TipoExpediente tipoExpediente, TipoExpediente original);
    @Override public Optional<BusinessMessages> validateRemove(TipoExpediente tipoExpediente);
    //   V-TipoExpediente-001 (Origen spec: RES-TipoExpediente-001): los tres devuelven SIEMPRE un mensaje, sin if.
    //   Mensaje debe transmitir: los tipos de expediente los registra la aplicación al arrancar y no se pueden
    //   crear/modificar/borrar desde la aplicación (tampoco el Administrador).

    @Override public AllowProperties allowPropertiesInsert();
    @Override public AllowProperties allowPropertiesUpdate();
    //   Los dos devuelven AllowProperties.createDenyAllProperties().
    //   Origen spec: Input AllowProperties «ninguna» de las acciones Crear y Modificar de entity-TipoExpediente.md.
    //   Comentario: el endpoint REST automático no acepta ningún campo; complementa a V-TipoExpediente-001.
}
```

Por qué basta y no rompe nada: el data-init que registra los tipos al arrancar no pasa por `ModelServiceFactory` (en AOP solo lo usan `Resource` y `ModelServiceValidationWalker`) y ningún código del proyecto persiste `TipoExpediente` con un `ModelService`.
Es la defensa real también frente al Administrador, que se salta los permisos (`AuthUtils.isAdmin`).

Verificación: `grep -rn "class TipoExpedienteServiceImpl" src/main/java/com/educaflow/subsystem/expedientes/service/impl` encuentra la clase (compilación en el Paso 6).

**Del diseño — Frontera de confianza, `AllowProperties` de `TipoExpedienteServiceImpl` (verbatim)**

`allowPropertiesInsert()` y `allowPropertiesUpdate()` devuelven `AllowProperties.createDenyAllProperties()` en `NuevoExpedienteServiceImpl` (Origen spec: Input AllowProperties «ninguna» de `Modificar` en `entity-NuevoExpediente.md`: el modelo no se guarda) y en `TipoExpedienteServiceImpl` (Origen spec: Input AllowProperties «ninguna» de `Crear` y `Modificar` en `entity-TipoExpediente.md`), así que el endpoint REST automático no acepta ningún campo de ninguna de las dos entidades.
No es el parche de `Expediente` que `CLAUDE.md` prohíbe reintroducir: son una entidad de pantalla y una de catálogo, fuera de la tramitación.

**Del diseño — Trazabilidad de validaciones (V) (verbatim)**

### Validaciones (V)

| V | Origen spec | Ubicación |
|---|---|---|
| V-TipoExpediente-001 | RES-TipoExpediente-001 | `TipoExpedienteServiceImpl.validateInsert` / `validateUpdate` / `validateRemove` (+ `insert`/`update`/`remove` con `UnsupportedOperationException`) |

**Del diseño — Notas y supuestos aplicables (verbatim)**

1. **Toques al motor (`subsystem/expedientes`).** El diseño toca el motor solo donde el spec y las guías lo piden (sacar la ventana, validar en la puerta, reducir el contexto) y en `TipoExpedienteService` (D5).
   Ninguno añade una capacidad nueva (ni punto de extensión, ni tipo de acción, ni capacidad de la máquina de estados): `ContextoTramitacion` sustituye al modelo anterior y `FormaPresentacion` a la equivalencia perfil ↔ papel que hoy está repartida; los métodos nuevos de `ExpedienteService` quedan en `validateTriggerInitialEvent`, `getPerfilesDeInicio` y `getVistaExpediente` (la validación de la puerta que piden las guías y lógica movida desde las clases que salen; lo que solo necesita la ventana, como la lista de centros, vive en el sistema), y `TipoExpedienteService` protege un dato del propio motor.
   Aun así, `TipoExpedienteService` crea un paquete `service/` (singular) en el motor junto al `services/` (plural): el humano debería confirmarlo antes de implementar (el `CLAUDE.md` del motor pide consultar cualquier ampliación).
17. **Visibilidad de «Tipos de expediente» solo por menú (aceptado a propósito, no es un defecto a reportar).** El spec dice que la pantalla es «visible únicamente para el Administrador»; el diseño lo cumple solo ocultando el menú (`groups="admins"`, Paso 8), sin defensa en el servidor. Motivos: (a) el grupo `users` tiene que conservar la lectura `TipoExpediente.conAce` porque otras pantallas del expediente (tramitación, listados de expedientes) leen el tipo de expediente; (b) ese permiso ya filtra las filas a los tipos sobre los que el usuario tiene un `Ace` en uno de sus centros, así que abrir la acción por URL no enseña ningún dato que el usuario no pudiera leer ya por REST o por esas pantallas; (c) la pantalla es de solo lectura y la escritura la bloquea el servidor para todos (`TipoExpedienteServiceImpl`, V-TipoExpediente-001, D5). Una defensa real (un `domain` o permiso sobre la acción solo para `admins`) añadiría piezas sin proteger ningún dato. Si en el futuro la pantalla enseñara datos que `conAce` no da, habría que añadirla.
