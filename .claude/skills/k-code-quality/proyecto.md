# Reglas específicas del proyecto

## Controladores Axelor

Los métodos `@CallMethod` de controladores deben:

- Nombrar los parámetros exactamente `actionRequest` y `actionResponse`. Cualquier variante (`req`, `resp`, `request`, `response`, `ar`, `aReq`…) es incorrecta.
- Delegar en el servicio a través de `ModelServiceFactory` o `@Inject` — nunca instanciar implementaciones directamente.
- Resolver el `ModelService` en una variable local al principio de **cada método que lo usa**: `final MiEntidadService miEntidadService = (MiEntidadService) modelServiceFactory.resolve(MiEntidad.class);`. Es un idioma del proyecto: **MUST NOT** envolverse en un método privado `getMiEntidadService()` ni guardarse en un campo (ver `metodos.md`, «Extraer solo lo que aporta»).
- Usar `ActionRequestHelper<T>` para extraer el modelo, el id o los datos del request.
- Obtener el `AllowProperties` llamando a `miEntidadService.allowPropertiesMiAccion()` del servicio (la whitelist vive en el servicio, no se construye inline con `Map.of(...)` en el controlador). Regla de decisión entre `createAllowProperties` (whitelist), `createAllowAllProperties` (abierto) y `createDenyAllProperties` (cerrado) en `[[k-secure-coding]]` §3.

---

## Fronteras entre subsistemas

Cualquier acceso a entidades de otro subsistema — lectura, escritura o eliminación — debe hacerse a través del servicio de ese subsistema (`*Service`), nunca llamando directamente a su repositorio. El repositorio es un detalle de implementación interno del subsistema propietario.

**Violación:** acceder a `UsuarioAutorizadoRepository` desde `subsystem/importacion`, ya sea para leer o para escribir.

**Correcto:** exponer en el servicio del subsistema propietario los métodos que necesiten los subsistemas clientes — tanto de consulta como de mutación — y llamarlos desde fuera: `usuarioAutorizadoService.findByCentroDniTipoUsuarioCurso(...)` o `.insert(...)`. Las validaciones de negocio (`validateInsert`, `fireActionRule_*`) se ejecutan siempre, independientemente del origen de la llamada.

---

## Capa de servicio

**JPQL en el repositorio, nunca en el servicio.** Todo código con `.all().filter().bind().fetch*()` pertenece al repositorio. En el servicio solo se llaman métodos nombrados del repositorio: `repository.findByDni(dni)`, nunca `repository.all().filter("self.dni = :dni").bind(...)`. Y siempre con parámetros nombrados (`:param`); **MUST NOT** concatenar input del usuario en filtros JPQL — ver `[[k-secure-coding]]` §5.

**Lógica de negocio en el servicio, nunca en listeners JPA.** Los listeners JPA se reservan para auditoría externa o sincronización con sistemas de terceros. La lógica de negocio va siempre en métodos `fireActionRule_*` dentro del `*ServiceImpl`, llamados desde `insert()`/`update()`/`remove()`.

**Asignación de campos `servidor` en `*ServiceImpl.insert/update`.** **MUST** ser incondicional (`entidad.setCampo(valor)` sin `if (campo == null)`). El anti-patrón `if (campo == null) setCampo(...)` permite mass-assignment vía el endpoint REST genérico `/ws/rest/<FQN>`. Ver `[[k-secure-coding]]` §3.3.

**`@Transactional` de Guice.** Importar siempre de `com.google.inject.persist`, nunca de `jakarta.transaction`.

---

## DI y módulos Guice

`ModelServiceFactory` descubre automáticamente cualquier clase en el paquete `service.impl.*ServiceImpl`. No crear módulos Guice para registrar implementaciones de `ModelService` — el servicio quedaría registrado dos veces y rompería la factoría.

Solo crear un módulo (`AxelorModule`) cuando hay bindings que genuinamente no pueden descubrirse por convención: interfaces no relacionadas con `ModelService`, decoradores, servicios de infraestructura.

Los `AxelorModule` los descubre y carga Axelor automáticamente al arrancar. Nunca instalarlos manualmente en `SecretariaVirtualModule` ni en ningún otro módulo.    

---

## Zona horaria

Toda conversión entre un instante y una fecha/hora local, y todo «ahora», **MUST** usar la zona de la aplicación `Convert.defaultZoneId` (`com.educaflow.base.util.Convert`), en Java y en Kotlin.
La zona del servidor (JVM) no es la del centro: en un contenedor suele ser UTC y desplaza las fechas cerca de medianoche.

- ✅ CORRECTO: `LocalDate.now(Convert.defaultZoneId)`, `offsetDateTime.atZoneSameInstant(Convert.defaultZoneId).toLocalDate()`, `instant.atZone(Convert.defaultZoneId)`
- ❌ INCORRECTO: `ZoneId.systemDefault()` (zona de la JVM, no la de la aplicación)
- ❌ INCORRECTO: `LocalDate.now()`, `LocalDateTime.now()`, `Year.now()` (sin argumento usan la zona de la JVM)
- ❌ INCORRECTO: `ZoneId.of("Europe/Madrid")` (duplica la constante; solo vive en `Convert`)
- ❌ INCORRECTO: `TimeZone.getDefault()`, `Calendar.getInstance()` sin zona (misma causa)

En los tests se aplica igual: el valor esperado se calcula con `Convert.defaultZoneId`, nunca con `ZoneId.systemDefault()` (el test pasaría en local y fallaría en un servidor con otra zona).

---

## Análisis estático (Error Prone)

- El código Java compila **sin warnings de Error Prone**: cada `warning: [Check]` que sale al compilar un fichero de `src/` es un defecto del cambio que lo produjo y se corrige en ese mismo cambio, no un aviso ignorable.
- Se corrige el código siguiendo la sugerencia del check. `@SuppressWarnings("Check")` va en el sitio concreto y solo para un falso positivo justificado; nunca se desactiva un check para todo el proyecto (la única excepción, `UnusedVariable`, ya está decidida en `build.gradle`).
- Qué es Error Prone, cómo se ve su salida y cómo forzar la recompilación para ver todos los avisos: `agent_docs/deploy.md`, «Análisis estático».
