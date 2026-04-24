---
name: sistemas-task
description: Pasos para construir un sistema o subsistema completo en EducaFlow — modelo, servicios, controladores y vistas.
---

# sistemas-task

## Pasos para construir un sistema o subsistema

Antes de crear nada, leer el skill `/sistemas-knowledge` para entender la estructura de carpetas y las reglas de dependencia entre capas.

---

## Paso 0 — Decidir dónde va

- ¿Es reutilizable por otros sistemas o subsistemas? → **subsystem/**
- ¿Es funcionalidad concreta para el usuario, sin reutilización prevista? → **system/**

Consultar `/sistemas-knowledge` para la distinción completa.

---

## Paso 1 — Crear el modelo de datos

**Skill:** `/modelos-task`

Para cada entidad del sistema/subsistema:

1. Crear el fichero XML en `domains/<NombreEntidad>.xml` con el namespace `domain-models`.
2. Definir campos, tipos, relaciones y validaciones básicas.
3. Verificar que los nombres de entidad y campo siguen las convenciones del proyecto.

El build generará automáticamente las clases Java en `db/`. No crear esas clases a mano.

---

## Paso 2 — Crear los servicios

**Skill:** `/servicios-task`

Para cada entidad que necesite lógica de negocio (validaciones, efectos secundarios, operaciones complejas):

1. Crear la interfaz `service/<NombreEntidad>Service.java` extendiendo `ModelService<NombreEntidad>`.
2. Si el insert necesita parámetros especiales, crear `service/<NombreEntidad>InsertDTO.java` (record Java).
3. Crear la implementación `service/impl/<NombreEntidad>ServiceImpl.java` extendiendo `DefaultModelService<NombreEntidad>`.
4. Añadir el constructor obligatorio `(Class<NombreEntidad> model, Repository repository)`.
5. Implementar los métodos `validate*` devolviendo `Optional<BusinessMessages>`.
6. Implementar los métodos `fireActionRule_*` para efectos secundarios.

Si la entidad no necesita lógica de negocio adicional, no hace falta servicio propio — `ModelServiceFactory` usará el `DefaultModelService` genérico.

---

## Paso 3 — Crear los controladores

**Skill:** `/controladores-task`

Solo si las vistas necesitan lógica disparada por botones o eventos de formulario:

1. Crear la clase `controller/<NombreEntidad>Controller.java`.
2. Inyectar `ModelServiceFactory` con `@Inject`.
3. Por cada acción de vista (botón, onSave, onChange, onLoad):
   - Crear un método `@CallMethod` con la firma adecuada (type1, type2 o type3).
   - Resolver el servicio con `modelServiceFactory.resolve(NombreEntidad.class)`.
   - Usar `ActionRequestHelper` para extraer datos de la request.
   - Usar `ActionResponseHelper` para devolver errores o resultados.
   - Añadir `@Transactional` solo si el método escribe en base de datos.

---

## Paso 4 — Crear las vistas

**Skills:** `/vistas-knowledge`, `/grids-task`, `/formularios-task`, `/actions-task`  
**Referencia de patrones:** `/sistemas-knowledge`

Para cada entidad:

1. Crear el fichero de vistas `views/<NombreEntidad>.xml`.
2. Crear el **grid principal** (`@Main-grid`) con los campos más relevantes.
3. Crear el **formulario principal** (`@Main-form`) con todos los campos editables.
4. Crear la **action-view principal** (`@Main-action`) que abre el grid desde el menú.
5. Si hay relaciones one-to-many, aplicar el patrón Maestro-Detalle con `<panel-related>`.
6. Si hay botones, crear las `action-method` correspondientes que referencien el controlador.
7. Si hay campos many-to-one con selector personalizado, añadir `grid-view="...@Search-grid"`.

Convención de nombres de vistas y actions: consultar `/sistemas-knowledge` (sección "Nombre de las vistas y acciones").

> Los ficheros `i18n_es.csv` e `i18n_ca.csv` se generan automáticamente — no crearlos a mano.

---

## Paso 5 — Crear el menú (si es necesario)

**Skill:** `/menus-task`

Si el sistema/subsistema necesita entradas de menú visibles al usuario:

1. Crear o editar el fichero `secretariavirtual/menus/{NNN}_menuitem_{nombre}.xml`.
2. Definir el menuitem raíz (sin `action`) y los menuitems hijo apuntando a las `action-view` del paso 4.
3. Verificar que el orden (`NNN`) es coherente con el resto de menús.

---

## Paso 6 — Verificar coherencia

**Skill:** `/checkvistas-knowledge`

1. Comprobar que todos los nombres de vistas son correctos y existen.
2. Comprobar que todas las referencias a actions desde botones y menús apuntan a actions que existen.
3. Comprobar que las referencias a grids y forms en `panel-related` y `action-view` son correctas.
4. Compilar el proyecto para confirmar que no hay errores de generación de código ni bindings rotos.

---

## Checklist final

- [ ] Los ficheros de dominios están en `domains/` y siguen la convención XML de Axelor
- [ ] Los servicios están en `service/` (interfaz) y `service/impl/` (implementación)
- [ ] Los controladores están en `controller/` y solo exponen lógica hacia las vistas
- [ ] Las vistas están en `views/` con nombres que siguen la convención `{Prefijo}{Entidad}@{Nombre}-{tipo}`
- [ ] El menú (si existe) está en `secretariavirtual/menus/` y referencia actions que existen en `views/`
- [ ] No se han creado ficheros `i18n_*.csv` a mano
- [ ] No se han editado ficheros en `db/` salvo los de `db/repo/` (repositorios y listeners propios)
- [ ] El sistema/subsistema no importa de otra capa que viole las reglas de dependencia
