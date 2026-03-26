---
name: modelos de Axelor
description: Crear un fichero XML de modelo de datos para Axelor a partir de una descripción en lenguaje natural o según un PDF con un formulario de datos
---

Este skill permite generar un fichero XML de modelo de datos para Axelor a partir de una descripción en lenguaje natural o según un PDF con un formulario de datos. 
El modelo de datos define las entidades, atributos, relaciones y validaciones necesarias para representar la información en el sistema Axelor.

La descripción exacta del XML está descrita en [references/axelor-modelos.md](references/axelor-modelos.md).

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

Los enumerados se definen con el elemento <enum> y sus items con el elemento <item>, indicando su nombre, título y descripción.

Cada fichero de dominio suele estar definido en carpeta "domains" , aunque hay excepciones.

Un atributo importante es "extends" que está en el tag "entity" y que indica que la entidad hereda de otra. Por ejemplo, si queremos crear una entidad Expediente que herede de la entidad base Expediente, el tag sería:

```<entity name="MiExpedienteconcreto" extends="com.educaflow.base.db.Expediente">```

Si la clase desde la que heredamos está en el mismo paquete, se puede omitir el package y poner solo el nombre de la clase:

```<entity name="MiExpedienteconcreto" extends="Expediente">```

Si para indicar los campos pasamos un PDF habrá que analizar el PDF para extraer los campos del formulario, sus tipos, títulos, ayudas, validaciones, etc. y luego generar el XML correspondiente.
Normalmente el nombre de estos campos del PDF tiene un nombre que se podrá usar como nombre de atributo en el XML, es que no que no lo tenga, por lo que habrá que generar un nombre de atributo válido a partir de las etiquetas "cercanas" a cada campo del PDF o de las sugerencias del campo. En las sugarencias suele estar el nombre en valenciano y castellano separado por una barra. Por ejemplo, si el PDF tiene un campo con etiqueta "Nombre completo del solicitante", el nombre del atributo en el XML podría ser "nombreCompletoSolicitante". 

Los nombres de los campos van a seguir la norma de camelCase, es decir, la primera letra de cada palabra va en mayúscula excepto la primera palabra que va en minúscula. Además, se eliminarán los espacios y caracteres especiales. Todo esto es así porque van a seguir las normas de propiedades de Java ya que con este XML de dominio que crea un Bean de Java para usar en JPA.
