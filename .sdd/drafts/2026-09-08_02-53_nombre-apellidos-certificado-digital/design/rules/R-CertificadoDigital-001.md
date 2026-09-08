# R-CertificadoDigital-001 — Asignación del titular del certificado a partir de su DNI

**Entidad:** CertificadoDigital
**Origen spec:** RN-CertificadoDigital-001, RN-CertificadoDigital-002, CC-CertificadoDigital-001
**Operación:** insert
**Momento:** Antes de `repository.save`
**Servicio host:** com.educaflow.subsystem.criptografia.service.impl.CertificadoDigitalServiceImpl
**Método host:** fireActionRule_AsignarTitular(CertificadoDigital certificado)

> **Por qué esta regla tiene fichero propio.** Cumple el criterio «necesita **tipos propios** del dominio de la regla (DTOs, value objects, records…) que no son entidades JPA y no existen ya» de `reglas-complejas.md` §1: introduce el `record DatosTitular`. Es además la única regla del diseño con **dos llamantes** (el guardado y la acción de pantalla) y con casos de error propios que no caben en el comentario de un método.

---

## Análisis de la regla

**Qué se dispara y cuándo.** Al dar de alta un certificado digital, antes de persistirlo, el sistema decide de quién es ese certificado: si el DNI escrito corresponde a un usuario de la aplicación, el nombre y los apellidos los pone el servidor copiándolos de la ficha de ese usuario y quedan congelados (RN-CertificadoDigital-001); si no corresponde a ninguno, se conservan los que escribió el administrador y quedan editables (RN-CertificadoDigital-002). En ambos casos se marca el campo calculado `nombreTomadoDelUsuario` (CC-CertificadoDigital-001), que es el que después gobierna la inmutabilidad (R-CertificadoDigital-003) y la obligatoriedad (V-CertificadoDigital-003/-004).

**El mismo cálculo lo consume también la pantalla.** La acción escalar `getDatosTitularByDni(String dni)`, disparada por el `onChange` del campo DNI, muestra en el formulario el resultado del mismo cálculo (U-certificados-digitales-001/-002). Es **una sola** función de dominio con dos llamantes, no dos implementaciones parecidas: si divergieran, la pantalla podría prometer un titular distinto del que el servidor acabaría persistiendo. De ahí el helper privado `resolverDatosTitular(String dni)` y el tipo propio que devuelve.

**Qué información lee y de dónde.** Únicamente la ficha del usuario cuyo documento coincide con el DNI: `UserRepository.findByDni(dni)` (finder ya declarado en `subsystem/common/domains/User.xml`), y de él los campos `nombre` y `apellidos` de la extensión de `User` del mismo fichero. No hay ningún enlace vivo con el usuario: el spec declara fuera de alcance mantener sincronizados nombre y apellidos con la ficha después del alta, así que se copian una vez y no se vuelve a mirar.

**Qué acciones realiza y en qué orden.**
1. Resolver el titular del DNI (`resolverDatosTitular`).
2. Si el titular existe, sobrescribir **incondicionalmente** `nombre` y `apellidos` del certificado con los del record; si no existe, no tocarlos (son del administrador y ya los validó V-CertificadoDigital-001/-002).
3. Asignar **incondicionalmente**, en las dos ramas, `nombreTomadoDelUsuario` con el valor del record.

**Efectos colaterales y transaccionalidad.** Ninguno fuera del propio bean: la regla escribe sobre el registro que se está insertando y por eso su momento es **Antes** de `repository.save`. No hay commit/rollback parcial, ni idempotencia que garantizar, ni locks: si la transacción del alta se deshace, no queda rastro. La consulta al `UserRepository` es de solo lectura.

**Seguridad.** `nombreTomadoDelUsuario` es un campo **`servidor`**: está fuera de `allowPropertiesInsert` y su asignación aquí es **incondicional**. **MUST NOT** añadirse ninguna guarda del tipo `if (certificado.getNombreTomadoDelUsuario() == null)`: por el endpoint REST automático `/ws/rest/<FQN>` un atacante podría colar el flag a `true` y convertir en inmutables unos datos que él mismo escribió (`k-secure-coding` §3.3). Por el mismo motivo, `nombre` y `apellidos` se sobrescriben sin mirar lo que trajera el cliente cuando hay usuario titular.

---

## Diseño detallado

### Clases nuevas

Ninguna clase de comportamiento: la regla vive entera en el `*ServiceImpl` (el helper privado `resolverDatosTitular` y el `fireActionRule_AsignarTitular` que lo invoca). Lo único nuevo es el tipo de datos de abajo.

### Interfaces

Ninguna. La regla no necesita estrategias ni adaptadores.

### Tipos propios

- `com.educaflow.subsystem.criptografia.service.DatosTitular` (**record**, fichero nuevo `service/DatosTitular.java`, junto a `SituacionFirma`)
  - Campos: `String nombre`, `String apellidos`, `boolean tomadoDelUsuario`.
  - Semántica: el titular resuelto de un DNI. `tomadoDelUsuario = true` significa «existe un usuario de la aplicación con ese documento y estos son el nombre y los apellidos de su ficha»; `false` significa «no existe ninguno» y entonces `nombre` y `apellidos` son `null`.
  - Es un value object inmutable, sin identidad y sin persistencia: **no** es una entidad JPA y **MUST NOT** declararse en ningún `domains/*.xml`.
  - Factoría estática: `public static DatosTitular sinUsuario()` — devuelve `new DatosTitular(null, null, false)`. Existe para que las dos ramas del cálculo se lean simétricas y para que ningún llamante tenga que recordar qué valores lleva la rama «no hay usuario».
  - Va en el paquete `service` (no en `service.impl`) porque forma parte del contrato público: es el tipo de retorno de `CertificadoDigitalService.getDatosTitularByDni(String)`.

### Diagrama de secuencia

```
insert(certificado)
  ├─ validateInsert(certificado)                      → Optional<BusinessMessages>  (V-001…V-005)
  ├─ fireActionRule_AsignarTitular(certificado)
  │    └─ resolverDatosTitular(certificado.getDni())  → DatosTitular
  │         └─ findUsuarioTitular(dni)
  │              └─ userRepository.findByDni(dni)     → User | null
  │         ├─ [User != null]  → new DatosTitular(user.getNombre(), user.getApellidos(), true)
  │         └─ [User == null]  → DatosTitular.sinUsuario()
  │    ├─ [datos.tomadoDelUsuario()] certificado.setNombre(datos.nombre()); certificado.setApellidos(datos.apellidos())
  │    └─ (siempre)              certificado.setNombreTomadoDelUsuario(datos.tomadoDelUsuario())
  └─ repository.save(certificado)                     → CertificadoDigital persistido

getDatosTitularByDni(dni)   ← acción de pantalla (onChange del DNI), MISMO cálculo
  ├─ validateGetDatosTitularByDni(dni)                → Optional.empty() siempre
  └─ resolverDatosTitular(dni)                        → DatosTitular  (se devuelve tal cual al formulario)
```

### Errores

| Condición | Origen | Tratamiento |
|-----------|--------|-------------|
| `dni` nulo o en blanco | `resolverDatosTitular` / `findUsuarioTitular` | `findUsuarioTitular` devuelve `null` **sin consultar** el repositorio, y la regla toma la rama «no hay usuario» (`DatosTitular.sinUsuario()`). No lanza ni acumula mensajes: el DNI vacío o mal formado ya lo rechaza V-CertificadoDigital (helper `validateCertificado`, `DniUtil.isValid`) antes de llegar aquí en el guardado; y en la acción de pantalla es un estado legítimo (el administrador está tecleando). |
| Existe **más de un** usuario con el mismo documento (`User.findByDni` es un finder de resultado único) | `userRepository.findByDni(dni)` | El finder generado lanza `NonUniqueResultException` (excepción de JPA, no un `BusinessMessages`). **MUST NOT** capturarse ni convertirse en un mensaje de validación: es una **incoherencia de los datos maestros de usuarios**, no un error del administrador de certificados, y silenciarla escogiendo «uno cualquiera» dejaría el certificado con el titular equivocado. Se deja propagar para que quede en el log del servidor. Si en el futuro se admitieran documentos repetidos entre usuarios, el criterio de desempate tendría que decidirlo el spec, no este diseño. |
| El usuario existe pero su ficha tiene `nombre` o `apellidos` vacíos o nulos | `resolverDatosTitular` | Se copian **tal cual** (incluido el `null`) y `tomadoDelUsuario` queda a `true`. Es deliberado: el spec dice que cuando existe usuario el nombre lo pone el sistema y V-CertificadoDigital-001/-002 «no aplican», así que el certificado se guarda con lo que haya en la ficha; corregirlo es tarea del mantenimiento de usuarios, no de esta pantalla, donde los campos quedan además de solo lectura. **MUST NOT** añadirse un *fallback* al nombre escrito por el administrador: reintroduciría por la puerta de atrás el valor del cliente que RN-CertificadoDigital-001 manda descartar. |
| El DNI cambia entre lo que se mostró en el `onChange` y lo que se envía al guardar | `insert` | Irrelevante por construcción: la regla **recalcula** el titular en el servidor a partir del `dni` del bean que se está guardando y no se fía de lo que llegue en `nombre`/`apellidos`/`nombreTomadoDelUsuario`. La respuesta de la acción de pantalla es solo UX. |

### Contenido del método `fireActionRule_*`

```java
// Firma:
private void fireActionRule_AsignarTitular(CertificadoDigital certificado);
//   Implementa R-CertificadoDigital-001 (Origen spec: RN-CertificadoDigital-001,
//   RN-CertificadoDigital-002, CC-CertificadoDigital-001). Diseño detallado en
//   design/rules/R-CertificadoDigital-001.md. Momento: ANTES de repository.save, dentro de insert.
//   Secuencia:
//     1. DatosTitular datos = resolverDatosTitular(certificado.getDni())
//     2. Si datos.tomadoDelUsuario(): asignación INCONDICIONAL de certificado.setNombre(datos.nombre())
//        y certificado.setApellidos(datos.apellidos()) — se descarta lo que enviara el cliente
//        (RN-CertificadoDigital-001).
//        Si no: `nombre` y `apellidos` se conservan tal y como llegaron (RN-CertificadoDigital-002),
//        ya validados como no vacíos por V-CertificadoDigital-001/-002.
//     3. En LAS DOS ramas, asignación INCONDICIONAL de
//        certificado.setNombreTomadoDelUsuario(datos.tomadoDelUsuario()) — campo `servidor`,
//        fuera de allowPropertiesInsert. MUST NOT añadir ninguna guarda `if (campo == null)`
//        (k-secure-coding §3.3).
```

```java
// Firma del cálculo compartido:
private DatosTitular resolverDatosTitular(String dni);
//   Cálculo ÚNICO del titular de un DNI, compartido por fireActionRule_AsignarTitular (al guardar)
//   y por la acción de pantalla getDatosTitularByDni (al teclear el DNI).
//   Secuencia:
//     1. User titular = findUsuarioTitular(dni)   — userRepository.findByDni(dni), null si el DNI
//        es nulo o está en blanco. MUST NOT loguear el DNI (k-secure-coding §6).
//     2. Si titular != null → new DatosTitular(titular.getNombre(), titular.getApellidos(), true)
//     3. Si titular == null → DatosTitular.sinUsuario()
```
