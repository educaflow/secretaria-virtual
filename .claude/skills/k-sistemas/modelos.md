# Guía de modelos de dominio en EducaFlow

Referencia del modelo de dominio XML de Axelor. Define la estructura de entidades, atributos, relaciones y validaciones para representar la información en el sistema.

La descripción exacta del XML está descrita en [references/models.md](references/models.md) y en [references/repositories.md](references/repositories.md).

`references/domain-models.xsd` — **schema XSD oficial de Axelor 8.1**: fuente de verdad para verificar qué atributos y etiquetas son válidos en cualquier elemento de dominio (`<entity>`, `<string>`, `<integer>`, `<many-to-one>`, `<one-to-many>`, `<enum>`, `<finder-method>`, `<extra-code>`, `<extra-code-model>`, etc.) y si un fichero XML está bien formado. Consultar este fichero ante cualquier duda sobre si un atributo existe o cuáles son sus valores permitidos.

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


