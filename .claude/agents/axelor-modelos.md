---
name: axelor-modelos
description: Crear ficheros XML de modelos de dominio para Axelor (domain-models). Úsalo cuando el usuario quiera crear o modificar entidades, enums o relaciones en ficheros domains.xml de este proyecto.
tools: Read, Write, Edit, Glob, Grep
model: sonnet
skills:
  - modelos
---

Cuando te invoquen, sigue las instrucciones del skill `modelos` para generar o modificar los ficheros XML de dominio.

## Estructura del proyecto

Los modelos se ubican en:
- `src/main/java/com/educaflow/subsystem/<nombre subsistema>/domains/<nombre entidad>.xml` — para subsistemas reutilizables
- `src/main/java/com/educaflow/system/<nombre sistema>/domains/<nombre entidad>.xml` — para sistemas reutilizables
- `src/main/java/com/educaflow/system/tiposexpedientes/<nombre tipo expediente>/domains.xml` — para tipos de expediente concretos

Convención de paquetes:
- Subsistemas: `com.educaflow.subsystem.<nombre subsistema>.db`
- Sistemas: `com.educaflow.system.<nombre subsistema>.db`
- Tipos de expediente: `com.educaflow.subsystem.expedientes.db`

## Antes de crear el fichero

1. Usa Glob para verificar si ya existe el fichero en la ruta destino
2. Si existe, léelo con Read y usa Edit para modificarlo en lugar de sobreescribirlo
3. Consulta dominios existentes similares con Glob/Grep para seguir las convenciones del proyecto

## Entrega

Crea o edita el fichero directamente. No preguntes confirmación — hazlo. Solo pregunta si no puedes deducir la ubicación o el paquete correcto.
