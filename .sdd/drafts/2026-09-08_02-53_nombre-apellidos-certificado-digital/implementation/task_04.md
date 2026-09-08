---
type: implementation-task
template: system
---

# Tarea 04 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-sistemas
- k-secure-coding
- k-code-quality

## Fila de la tabla «Ficheros a crear o modificar»

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/criptografia/controller/CertificadoDigitalController.java` | Crear | k-sistemas (controladores.md) | Controlador de la entidad con el `@CallMethod` `getDatosTitularByDni` que alimenta el `onChange` del DNI |

## Texto del diseño (verbatim)

### Paso 5 — Controlador `CertificadoDigitalController`

**Clase:** `com.educaflow.subsystem.criptografia.controller.CertificadoDigitalController` (**Crear** — hoy el subsistema solo tiene `DispositivoCriptograficoController`; la regla «un controlador por entidad» obliga a uno propio para `CertificadoDigital`).

```java
// Clase: com.educaflow.subsystem.criptografia.controller.CertificadoDigitalController
// Campo:
@Inject private ModelServiceFactory modelServiceFactory;

// Método:
@CallMethod
public void getDatosTitularByDni(ActionRequest actionRequest, ActionResponse actionResponse);
//   Punto de entrada de la acción de vista
//   `subsysCriptografia.Main@CertificadoDigital-Remote-getDatosTitularByDni-action`, disparada por
//   el onChange del campo `dni` del formulario. Implementa la parte de servidor de
//   U-certificados-digitales-001 y U-certificados-digitales-002.
//   Es un @CallMethod de los que NO construyen la entidad (acción escalar), como el
//   `getClaveFirma` de TareaFirmaController (línea 149) o PdfUtilitiesController.
//   Secuencia:
//     1. Resuelve el servicio:
//        (CertificadoDigitalService) modelServiceFactory.resolve(CertificadoDigital.class)
//     2. ActionRequestHelper<CertificadoDigital> con CertificadoDigital.class y
//        ActionResponseHelper con actionResponse.
//     3. Lee el DNI del contexto de la petición:
//        Object valorDni = actionRequestHelper.getRequestData().get("dni");
//        String dni = (valorDni == null) ? null : valorDni.toString();
//        MUST NOT usar actionRequestHelper.getModel(...): con `id` en el request devuelve la
//        entidad GESTIONADA por JPA (jpaRepository.find(id)) con el mapa del cliente copiado
//        encima, y escribir sobre ella arriesga un flush no querido en la transacción de la
//        petición. Al ser una acción escalar aquí no hace falta ninguna entidad.
//     4. service.validateGetDatosTitularByDni(dni); si devuelve mensajes,
//        actionResponseHelper.doResponseBusinessMessagesAsError(...) y return.
//     5. DatosTitular datos = service.getDatosTitularByDni(dni)
//     6. actionResponse.setValue("nombre", datos.nombre()),
//        actionResponse.setValue("apellidos", datos.apellidos()) y
//        actionResponse.setValue("nombreTomadoDelUsuario", datos.tomadoDelUsuario()).
//        Los tres son valores de formulario: no se persiste nada aquí.
//   Sin @Transactional: la acción no escribe en base de datos.
//   No hace ninguna comprobación de rol ni de inmutabilidad: eso vive en el servicio y en las
//   whitelists de insert/update (k-sistemas/controladores.md, anti-patrones).
//   Parámetros nombrados `actionRequest` y `actionResponse`, en camelCase completo.
```

**MUST NOT** añadir `@CallMethod` para `insert`/`update`/`remove` ni un `validateSave`/`validateDelete`: los expone el endpoint REST automático y las acciones globales `remote-validation*` de `DefaultModelController`.

**Verificar:** `./gradlew compileJava`; y `grep -n 'method="getDatosTitularByDni"' src/main/java/com/educaflow/subsystem/criptografia/views/Main-CertificadoDigital.xml` coincide con el nombre del método del controlador y con el segmento `Remote-getDatosTitularByDni` del nombre de la acción (regla `Remote-{nombreFuncionJava}` de `k-vistas/actions.md`).

### Frontera de confianza — la acción que este controlador expone (verbatim)

#### `CertificadoDigitalServiceImpl.getDatosTitularByDni` (invocado desde `CertificadoDigitalController.getDatosTitularByDni`)

**Sin tabla: la acción es ESCALAR y por tanto NO declara whitelist.** Recibe un `String dni` leído del contexto de la petición (`actionRequestHelper.getRequestData().get("dni")`) y devuelve un `DatosTitular`; no construye la entidad desde el request, así que no hay mapa del cliente que filtrar y un `allowPropertiesGetDatosTitularByDni()` no protegería nada — `k-sistemas/servicios.md` §`allowPropertiesXxx` lo marca explícitamente como ❌, con el ejemplo de la `getSituacionFirmaByDni(String dni)` que ya existe en este mismo servicio.

Superficie de la acción sobre la entidad: **ninguna**. No lee ningún campo del bean, no escribe ninguno y no persiste nada; los tres valores que devuelve (`nombre`, `apellidos`, `nombreTomadoDelUsuario`) viajan al formulario como `actionResponse.setValue(...)` y solo se convierten en datos guardados si el administrador pulsa «Guardar», momento en el que vuelven a pasar por `allowPropertiesInsert`/`allowPropertiesUpdate` y por las action rules de abajo. Que la pantalla muestre el titular no lo hace confiable: lo que se persiste lo vuelve a calcular el servidor en `fireActionRule_AsignarTitular`.

### Notas y supuestos que aplican a esta tarea (verbatim)

10. **Ningún cableado Guice nuevo.** `CertificadoDigitalService` es un `ModelService` y lo descubre `ModelServiceFactory` por convención de nombre y paquete; el controlador solo inyecta `ModelServiceFactory`. `CriptografiaModule` no se toca y no se carga `k-guice`.

12. **La acción de pantalla es ESCALAR y comparte el cálculo con la regla de guardado.** Se descartó la forma «recibe y devuelve la entidad» (`rellenarTitular(CertificadoDigital)`) por tres motivos, todos verificados contra el código real: (a) obligaría a declarar un `allowProperties` que `k-sistemas/servicios.md` marca como ❌ para acciones escalares; (b) el controlador tendría que construir el bean con `ActionRequestHelper.getModel(...)`, que cuando el request trae `id` devuelve `jpaRepository.find(id)` —una entidad **gestionada** por JPA, con el mapa del cliente copiado encima—, de modo que asignarle el titular arriesgaría persistir esos valores en el flush de la transacción de la petición; hoy solo sería inocuo por un efecto colateral de la UI (el `readonlyIf="id != null"` del DNI impide que el `onChange` se dispare en un registro ya guardado), y una dependencia así no debe quedar implícita; y (c) duplicaría en dos sitios la lógica «hay usuario / no hay usuario», con lo que la pantalla podría prometer un titular distinto del que el servidor acabaría persistiendo. Con la forma escalar, `getDatosTitularByDni` y `fireActionRule_AsignarTitular` invocan **el mismo** helper privado `resolverDatosTitular(String dni)` y no pueden divergir. El mecanismo de vista no cambia: sigue siendo un `action-method` colgado del `onChange` del DNI, y U-001/U-002 se materializan igual.
