---
name: vistas-reviewer
description: Revisa que los ficheros XML de vistas de Axelor creados o modificados cumplen todas las reglas de /vistas-knowledge — namespace, organización de ficheros, nomenclatura y coherencia con el modelo de dominio.
---

# vistas-reviewer

## Propósito

Verificar que los ficheros XML de vistas creados o modificados siguen las reglas definidas en `/vistas-knowledge`. Este reviewer actúa como punto de entrada; para verificar el contenido específico de grids, formularios, acciones y menús, delegar en los reviewers especializados.

## Qué leer

1. El fichero XML de vistas a revisar.
2. Los ficheros de dominio de la entidad (en `domains/`) para verificar que las vistas tienen un modelo detrás.
3. El skill `/vistas-knowledge` para tener presentes todas las reglas.

## Namespace y declaración del fichero

- [ ] El fichero empieza exactamente con:
  ```xml
  <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
  <object-views xmlns="http://axelor.com/xml/ns/object-views"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://axelor.com/xml/ns/object-views
  https://axelor.com/xml/ns/object-views/object-views_8.1.xsd">
  ```
- [ ] El namespace es `object-views` (no `domain-models`).
- [ ] El fichero termina con `</object-views>`.

## Relación con el modelo de dominio

- [ ] Existe un modelo de dominio (fichero XML en `domains/`) para cada entidad referenciada en las vistas.
- [ ] No hay vistas para entidades que no existen en el modelo de dominio.
- [ ] El atributo `model` de cada `<grid>`, `<form>` y `<action-view>` coincide con el FQCN de una clase Java generada a partir del dominio.

## Organización de ficheros

- [ ] El fichero de vistas está en la carpeta `views/` del sistema/subsistema correspondiente.
- [ ] El nombre del fichero sigue la convención `<NombreEntidad>.xml` o está agrupado por funcionalidad.
- [ ] Los ficheros `i18n_es.csv` e `i18n_ca.csv` NO se han creado ni modificado manualmente.
- [ ] Los `<menuitem>` globales están en `secretariavirtual/menus/`, no dentro de ficheros en `views/`.

## Nomenclatura de las vistas

Los nombres deben seguir el patrón `{Prefijo}.{Entidad}[.{EntidadHija}]*@{Nombre}-{tipo}`:

- [ ] El prefijo es `subsys{Subsistema}` o `sys{Sistema}` (PascalCase sin separador).
- [ ] El tipo al final del nombre es: `-grid`, `-form` o `-action` según el elemento.
- [ ] El identificador de contexto (`@Main`, `@Search`, `@View`, `@Pendiente`, etc.) es apropiado.

## Verificación de contenido — delegar en reviewers especializados

- [ ] Los grids (`<grid>`) cumplen las reglas → usar `/grids-reviewer`.
- [ ] Los formularios (`<form>`) cumplen las reglas → usar `/formularios-reviewer`.
- [ ] Las acciones (`<action-*>`) cumplen las reglas → usar `/actions-reviewer`.
- [ ] Los menús (`<menuitem>` en `secretariavirtual/menus/`) cumplen las reglas → usar `/menus-reviewer`.
- [ ] La coherencia de referencias entre todas las vistas → usar `/checkvistas-knowledge`.

## Checklist final

- [ ] El namespace `object-views` es correcto y el fichero tiene la declaración XML exacta
- [ ] Existe modelo de dominio para cada entidad referenciada
- [ ] El fichero está en `views/` del sistema/subsistema correcto
- [ ] No se han creado ni modificado `i18n_*.csv` manualmente
- [ ] Los `<menuitem>` globales están en `secretariavirtual/menus/`, no en `views/`
- [ ] Todos los nombres de vistas siguen la convención `{Prefijo}.{Entidad}@{Nombre}-{tipo}`

## Resultado

Si todos los checks del checklist final están bien, mostrar únicamente: **OK-No hay problemas**
