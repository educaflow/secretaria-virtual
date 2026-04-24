---
name: modelos-reviewer
description: Revisa que los ficheros XML de modelo de dominio de Axelor creados o modificados cumplen todas las reglas de /modelos-knowledge — namespace, entidades, atributos, tipos, relaciones, enumerados y paquetes.
---

# modelos-reviewer

## Propósito

Verificar que los ficheros XML de modelo de dominio creados o modificados siguen las reglas definidas en `/modelos-knowledge`.

## Qué leer

1. El fichero XML de dominio a revisar (en la carpeta `domains/`).
2. Otros ficheros de dominio del mismo subsistema/sistema si hay relaciones entre ellos.
3. El skill `/modelos-knowledge` para tener presentes todas las reglas.

## Cabecera y namespace

- [ ] El fichero empieza con `<domain-models xmlns="http://axelor.com/xml/ns/domain-models" ...>` con el namespace correcto.
- [ ] El elemento `<module name="..." package="..."/>` está presente y el paquete sigue la convención `com.educaflow.{layer}.{subsistema}.db`.
- [ ] El fichero está ubicado en la carpeta `domains/` del sistema/subsistema correcto.

## Entidades (`<entity>`)

- [ ] El nombre de cada entidad está en PascalCase.
- [ ] Si la entidad hereda de otra, el atributo `extends` usa el FQCN completo cuando está en otro paquete, o solo el nombre de la clase si está en el mismo paquete.
- [ ] Hay un fichero XML separado por cada entidad (o una agrupación justificada).

## Atributos

- [ ] Los nombres de atributos están en camelCase.
- [ ] El tipo XML de cada atributo es correcto para su naturaleza: `string`, `integer`, `decimal`, `boolean`, `date`, `datetime`, `many-to-one`, `one-to-many`, `many-to-many`.
- [ ] Los atributos `required="true"` están marcados en todos los campos obligatorios del negocio.
- [ ] Los atributos `string` con texto largo tienen `large="true"` y/o `multiline="true"` si procede.
- [ ] Los atributos `decimal` tienen `precision` y `scale` definidos.
- [ ] El atributo `title` de cada campo es descriptivo y orientado al usuario.

## Relaciones

- [ ] Las relaciones `one-to-many` tienen `mappedBy` apuntando al campo `many-to-one` de la entidad hija.
- [ ] Las relaciones `many-to-one` tienen `ref` apuntando a la entidad correcta (FQCN si está en otro paquete).
- [ ] Las relaciones bidireccionales son consistentes: el `mappedBy` del `one-to-many` coincide con el nombre del campo `many-to-one` en la otra entidad.
- [ ] No hay relaciones circulares sin `cascade` o `lazy` configurados correctamente.

## Enumerados (`<enum>`)

- [ ] Cada `<enum>` referenciado en una entidad está definido en el mismo fichero o en un fichero del mismo módulo.
- [ ] Cada `<item>` del enum tiene `name` en UPPER_CASE, `title` descriptivo y opcionalmente `description`.
- [ ] El nombre del enum está en PascalCase.

## Finders y extra-code

- [ ] Los `<finder>` tienen `name` en camelCase, `filter` con sintaxis JPQL válida y `using` con los parámetros correctos.
- [ ] El código en `<extra-code>` (repositorio) y `<extra-code-model>` (dominio) no contiene lógica de negocio compleja — solo helpers simples.
- [ ] Si hay `<extra-imports-model>`, los imports son necesarios para el código en `<extra-code-model>`.

## Checklist final

- [ ] El namespace `domain-models` y el `<module package="..."/>` son correctos
- [ ] Todos los nombres de entidades están en PascalCase y los de atributos en camelCase
- [ ] Los tipos XML de atributos son correctos para cada campo
- [ ] Los campos obligatorios de negocio llevan `required="true"`
- [ ] Las relaciones bidireccionales (`one-to-many`/`many-to-one`) tienen `mappedBy` coherente
- [ ] Los enumerados referenciados están definidos en el mismo fichero
- [ ] El paquete del `<module>` es `com.educaflow.{layer}.{subsistema}.db`
- [ ] No se han editado manualmente ficheros en `db/` (solo en `db/repo/` si es un repositorio o listener)
