# Guía de modelos de dominio en EducaFlow

Referencia del modelo de dominio XML de Axelor. Define la estructura de entidades, atributos, relaciones y validaciones para representar la información en el sistema.

La descripción exacta del XML está descrita en [references/models.md](references/models.md) y en [references/repositories.md](references/repositories.md).

`../axelor-open-platform/axelor-core/src/main/resources/domain-models.xsd` — **schema XSD oficial de Axelor 8.1**: fuente de verdad para verificar qué atributos y etiquetas son válidos en cualquier elemento de dominio (`<entity>`, `<string>`, `<integer>`, `<many-to-one>`, `<one-to-many>`, `<enum>`, `<finder-method>`, `<extra-code>`, `<extra-code-model>`, etc.) y si un fichero XML está bien formado. Consultar este fichero ante cualquier duda sobre si un atributo existe o cuáles son sus valores permitidos.

Un ejemplo de modelo de la entidad TareaFirma es el siguiente fichero TareaFirma.xml:

```
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xsi:schemaLocation="http://axelor.com/xml/ns/domain-models https://axelor.com/xml/ns/domain-models/domain-models_8.1.xsd">

    <module name="firma" package="com.educaflow.subsystem.firma.db"/>
    <entity name="TareaFirma" >
        <many-to-one name="firmante" ref="com.axelor.auth.db.User" required="true" />
        <one-to-many name="documentosFirma" ref="DocumentoFirma" mappedBy="tareaFirma"  />
        <datetime name="fechaSolicitud" title="Fecha de la solicitud" required="true" />
        <datetime name="fechaResolucion" title="Fecha de la resolución" />
        <boolean name="firmaRapida" title="Firma rápida" help="La firma se permite que se haga inmediatamente sin intervención del usuario si existe su certificado" />
        <enum name="estadoTareaFirma" ref="EstadoTareaFirma" title="Estado" required="true" />
        <string name="motivoFirma" required="true" />
        <string name="motivoRechazo" large="true" multiline="true" title="Motivo del rechazo de la firma de los documentos" />
        <string name="fqcnFirmaNotifier" title="FQCN donde notificar" />
        <string name="fqcnCallBackData" title="FQCN del datos de vuelta"  />
        <string name="callBackData" large="true" multiline="true" title="Datos de vuelta" />
        <decimal name="x" precision="8" scale="2" required="true"/>
        <decimal name="y" precision="8" scale="2" required="true"/>
        <decimal name="width" precision="8" scale="2" title="Ancho" required="true"/>
        <decimal name="height" precision="8" scale="2" title="Alto" required="true"/>
        <integer name="page" title="Página" required="true"/>
    </entity>


    <enum name="EstadoTareaFirma">
        <item name="PENDIENTE" title="Pendiente de firmar" description="Está pendiente la firma de los documentos"/>
        <item name="FIRMADO" title="Firmado" description="Están firmados los documentos"/>
        <item name="RECHAZADO" title="Rechazada la firma" description="Se ha rechazado la firma de los documentos"/>
    </enum>



</domain-models>
```

Como se puede ver cada entidad está en su propio fichero XML y se definen los atributos con su tipo, título, ayuda, validaciones, etc. Además se pueden definir relaciones entre entidades (many-to-one, one-to-many) y enumerados.
Tambien cada entidad está en un paquete concreto definido en el atributo package del módulo. En este caso la entidad TareaFirma está en el paquete com.educaflow.subsystem.firma.db

## REGLA CRÍTICA — Campos rellenados por el sistema NO llevan `required="true"`

Los campos que rellena el propio sistema dentro de `service.insert(...)` / `service.update(...)` (estados de máquina, fechas de creación/última actualización, contadores de intentos, motivos de fallo automáticos, usuario auditado, etc.) **NUNCA** deben declararse con `required="true"` en el dominio XML, aunque al final del flujo nunca queden a null.

### Por qué

El flujo de guardado de Axelor en `com.axelor.rpc.Resource.save` ejecuta los pasos en este orden:

1. `JPA.manage(bean)` → internamente llama a `em().persist(bean)` + `em().flush()`. **Aquí se ejecuta la validación de Jakarta Bean Validation y la INSERT en BD.**
2. `modelService.insert(bean)` → solo se ejecuta **después** de la INSERT. Aquí es donde nuestro código pone `estado=PENDIENTE`, `fechaCreacion=now`, etc.
3. `repository.save(bean)` dentro de `modelService.insert` (sea el heredado de `DefaultModelService` o tu sobrescritura) hace un `merge` que vuelca los campos del sistema con un UPDATE adicional.

Si un campo del sistema lleva `required="true"`:

- El dominio generado pone `@NotNull` en el atributo Java.
- La columna SQL se crea `NOT NULL`.
- Jakarta Bean Validation **falla en el paso 1**, antes de que `modelService.insert` tenga oportunidad de rellenar el valor.
- Resultado: `ConstraintViolationException` con mensaje "no debe ser nulo" en pleno `save` aunque el código posterior fuera a poner el valor correcto.

Sin `required="true"`:

- El paso 1 inserta una fila con `null` en esos campos (transitorio).
- El paso 2-3 los rellena y emite el UPDATE.
- El estado final en BD es correcto.

### Cuándo SÍ usar `required="true"`

Solo en campos **introducidos por el usuario** a través del formulario, cuya ausencia es un error funcional (asunto del correo, DNI del destinatario, cuerpo del mensaje, fichero a importar, etc.). En esos casos `required="true"` es la validación declarativa correcta y bloquea el guardado con el mensaje estándar.

### Patrón estándar para campos del sistema

```xml
<!-- Dominio: NO declarar required en estos campos -->
<entity name="TareaCorreo">
    <string name="asunto" required="true" title="Asunto"/>            <!-- usuario → required SÍ -->
    <string name="cuerpo" required="true" large="true" title="Cuerpo"/>  <!-- usuario → required SÍ -->

    <datetime name="fechaCreacion" title="Fecha de creación"/>        <!-- sistema → SIN required -->
    <integer  name="numeroIntentos" title="Número de intentos"/>      <!-- sistema → SIN required -->
    <enum     name="estado" ref="EstadoTareaCorreo" title="Estado"/>  <!-- sistema → SIN required -->
</entity>
```

```java
// Servicio: rellena los campos del sistema en insert ANTES de repository.save.
// La inicialización es una regla de negocio (R-XXX) y vive en su propio
// fireActionRule_* — no se hace inline en `insert`. Ver [[k-validaciones]] §reglas R-XXX.
// ASIGNACIÓN INCONDICIONAL dentro del fireActionRule_: sin `if (campo == null) ...`.
// Ver [[k-secure-coding]] §2.
@Override
public TareaCorreo insert(TareaCorreo entidad) {
    validateInsert(entidad).ifPresent(BusinessMessages::throwIfInvalid);
    fireActionRule_AsignarValoresIniciales(entidad);   // R-TareaCorreo-001
    return repository.save(entidad);                   // NUNCA super.insert (ver [[k-sistemas]] servicios.md)
}

private void fireActionRule_AsignarValoresIniciales(TareaCorreo entidad) {
    entidad.setEstado(EstadoTareaCorreo.PENDIENTE);
    entidad.setFechaCreacion(LocalDateTime.now());
    entidad.setNumeroIntentos(0);
}
```

> **Antipatrón a evitar:** suplir la inicialización con un `<action-record>` ejecutado desde `onNew` en la vista. Es frágil (depende de que la creación pase por esa vista concreta), duplica la lógica entre Java y XML y mezcla responsabilidades. La inicialización de campos del sistema vive en el servicio; la vista no debe saber del estado interno del dominio.

> **Antipatrón a evitar:** dentro de `insert(entidad)`, guardar la inicialización detrás de `if (entidad.getId() == null) { ... }`. Cuando el flujo de `Resource.save` invoca `modelService.insert`, ya ha pasado por `JPA.manage(bean)` → `em.persist(bean)`, así que **`entidad.getId()` ya NO es null** dentro de `insert`. El `if` parece una guarda defensiva pero realmente nunca se cumple y la inicialización queda muerta. `insert` solo se llama al crear, así que la guarda sobra — quita el `if` y ejecuta la inicialización directamente.

> **Antipatrón CRÍTICO de seguridad:** guardar la inicialización de un campo del sistema detrás de `if (entidad.getCampo() == null) { entidad.setCampo(valor); }`. El endpoint REST genérico `/ws/rest/<FQN>` permite al cliente enviar el campo ya relleno; el `if` lo respeta y el atacante falsifica `fechaCreacion`, `estado`, contadores, etc. **MUST** asignar **incondicionalmente** (`entidad.setCampo(valor)` sin `if`). Ver `[[k-secure-coding]]` §2 — es un fallo de seguridad explotable, no de estilo.

### Checklist al añadir un campo nuevo a un dominio

- [ ] ¿Quién rellena el valor? El **usuario** (formulario) → `required="true"` si la regla de negocio lo exige. El **sistema** (servicio/job/scheduler) → **sin** `required="true"`.
- [ ] Si es del sistema, ¿el servicio (`insert`/`update` o un método de transición) lo rellena en algún camino? Si no, es un bug: el campo se quedará null permanentemente.
- [ ] No usar `<action-record>` en `onNew` como sustituto de la inicialización en el servicio.

---

## REGLA CRÍTICA — `name` del `<module>` debe coincidir con el final del paquete

El atributo `name` del elemento `<module>` **siempre** tiene que ser idéntico al último segmento del `package` justo antes del sufijo `.db`. Es decir, el paquete tiene que acabar siempre en `.{name}.db`.

- Paquete `com.educaflow.subsystem.firma.db` → `name="firma"` ✅
- Paquete `com.educaflow.system.actas.db` → `name="actas"` ✅
- Paquete `com.educaflow.subsystem.registroentradasalida.db` → `name="registroentradasalida"` ✅
- Paquete `com.educaflow.subsystem.firma.db` → `name="firmas"` ❌ (no coincide con `firma`)
- Paquete `com.educaflow.subsystem.firma` → cualquier `name` ❌ (el paquete no acaba en `.db`)

### Checklist al crear/modificar el `<module>` de un dominio

- [ ] El `package` acaba en `.db`.
- [ ] El segmento inmediatamente anterior al `.db` coincide **exacto** con el `name` (mismas letras, mismo singular/plural, sin guiones bajos extra).
- [ ] El `name` y el segmento del paquete coinciden con el nombre de la carpeta del sistema/subsistema (`subsystem/<nombre>/` o `system/<nombre>/`).

Los enumerados se definen con el elemento <enum> y sus items con el elemento <item>, indicando su nombre, título y descripción.

Cada fichero de dominio suele estar definido en carpeta "domains" , aunque hay excepciones.

Un atributo importante es "extends" que está en el tag "entity" y que indica que la entidad hereda de otra. Por ejemplo, si queremos crear una entidad Expediente que herede de la entidad base Expediente, el tag sería:

```<entity name="MiExpedienteconcreto" extends="com.educaflow.base.db.Expediente">```

Si la clase desde la que heredamos está en el mismo paquete, se puede omitir el package y poner solo el nombre de la clase:

```<entity name="MiExpedienteconcreto" extends="Expediente">```

Si para indicar los campos pasamos un PDF habrá que analizar el PDF para extraer los campos del formulario, sus tipos, títulos, ayudas, validaciones, etc. y luego generar el XML correspondiente.
Normalmente el nombre de estos campos del PDF tiene un nombre que se podrá usar como nombre de atributo en el XML, es que no que no lo tenga, por lo que habrá que generar un nombre de atributo válido a partir de las etiquetas "cercanas" a cada campo del PDF o de las sugerencias del campo. En las sugarencias suele estar el nombre en valenciano y castellano separado por una barra. Por ejemplo, si el PDF tiene un campo con etiqueta "Nombre completo del solicitante", el nombre del atributo en el XML podría ser "nombreCompletoSolicitante". 

Los nombres de los campos van a seguir la norma de camelCase, es decir, la primera letra de cada palabra va en mayúscula excepto la primera palabra que va en minúscula. Además, se eliminarán los espacios y caracteres especiales. Todo esto es así porque van a seguir las normas de propiedades de Java ya que con este XML de dominio que crea un Bean de Java para usar en JPA.


# Repositorios
Tambien es posible crear una clase Java que actua como repositorio de JPA para cada entidad. Esta clase se suele llamar <NombreEntidad>Repository y suele extender de Abstract<NombreEntidad>Repository que es una clase generada automáticamente por Axelor a partir del XML de dominio. Por ejemplo, para la entidad TareaFirma, el repositorio se llamaría TareaFirmaRepository y extendería de AbstractTareaFirmaRepository. En este repositorio se pueden añadir métodos personalizados para realizar consultas o operaciones específicas sobre la entidad. Por ejemplo, se podría añadir un método para obtener todas las tareas de firma pendientes: 

Los repositorios los crea el desarrollador, no se generan automáticamente, aunque si se genera la clase Abstract<NombreEntidad>Repository a partir del XML de dominio. El repositorio se suele ubicar en el mismo paquete que la entidad, por ejemplo en "com.educaflow.subsystem.firma.db".

## Funciones de búsqueda personalizadas en repositorios

Tambien es posible definir nuevos métodos en el repositorio de una entidad para realizar consultas personalizadas. Para ello se puede usar el tag <finder> dentro del tag <entity> en el XML de dominio.

Por ejemplo, si queremos crear un método de búsqueda que permita buscar por correo y pais una lista de factura, podríamos añadir el siguiente finder al xml del modelo de la entidad Factura:

```xml
<finder-method name="findByEmailAndPais" using="email,String:pais"
  filter="self.email = :email and self.pais.code = :pais"
  all="true" />
```

Que creará el método findByEmailAndPais en el repositorio de la entidad Factura, con los parámetros email y pais, y que realizará la consulta definida en el filtro.

## Extra code en repositorios
También es posible añadir código extra a la clase repositorio de una entidad usando el tag <extra-code> dentro del tag <entity> en el XML de dominio. Esto permite añadir métodos o funcionalidades adicionales al repositorio sin tener que crear una clase repositorio personalizada desde cero. El código añadido en <extra-code> se incluirá en la clase Abstract<NombreEntidad>Repository generada automáticamente por Axelor a partir del XML de dominio.

Ejemplo de extra code en repositorio:

```xml
<entity name="DNI">
  <extra-code>
  <![CDATA[
public char getLetra(String dni) {
    if (dni == null || dni.length() != 9) {
        throw new IllegalArgumentException("DNI debe tener 9 caracteres");
    }
    return dni.charAt(8);
}
  ]]>
  </extra-code>
</entity>
```

# Extra code en dominio

También se puede añadir código directamente a la clase de dominio generada. Para ello se usan los tags
<extra-imports-model> y <extra-code-model> dentro de <entity>. Esto es útil para helpers simples de la entidad,
evitando meter lógica de negocio compleja en el modelo.

Ejemplo de extra code en dominio:

```xml
<entity name="Factura">
  <extra-imports-model>
  <![CDATA[
  import java.util.Locale;
  ]]>
  </extra-imports-model>

  <extra-code-model>
  <![CDATA[
  public String getCodigoNormalizado() {
    return this.codigo == null ? null : this.codigo.trim().toUpperCase(Locale.ROOT);
  }
  ]]>
  </extra-code-model>
</entity>
```


