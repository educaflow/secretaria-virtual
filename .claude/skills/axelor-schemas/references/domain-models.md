# Domain Models — Referencia XSD Axelor 8.1

Fuente: `domain-models.xsd` (namespace `http://axelor.com/xml/ns/domain-models`)

---

## Estructura del fichero

```xml
<?xml version="1.0"?>
<domain-models xmlns="http://axelor.com/xml/ns/domain-models"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://axelor.com/xml/ns/domain-models
    https://axelor.com/xml/ns/domain-models/domain-models_8.1.xsd">

  <module name="mymodule" package="com.example.mymodule.db"/>

  <entity name="MyEntity">
    <!-- campos -->
  </entity>
</domain-models>
```

---

## `<entity>` — atributos

| Atributo | Tipo | Descripción |
|---|---|---|
| `name` | string ✓ | Nombre (empieza por mayúscula, p.ej. `MyEntity`) |
| `table` | string | Nombre de tabla en BD (defecto: prefijo + nombre en snake_case) |
| `extends` | string | Clase base (FQN o nombre simple si mismo paquete) |
| `implements` | string | Interfaces a implementar (CSV) |
| `strategy` | enum | Estrategia de herencia (ver abajo) |
| `repository` | enum | Tipo de repositorio generado (ver abajo) |
| `persistable` | boolean | Si `false`, genera `@MappedSuperclass` (no tiene tabla propia) |
| `cacheable` | boolean | Habilitar caché JPA |
| `logUpdates` | boolean | Guardar createdOn/updatedOn/createdBy/updatedBy (defecto: true) |
| `jsonAttrs` | boolean | Añadir campo `attrs` JSON genérico |
| `equalsIncludeAll` | boolean | Incluir todos los campos simples en equals() |
| `sequential` | boolean | DEPRECADO — usar secuencia por entidad |
| `allocationSize` | integer | Tamaño de bloque en secuencias |

### strategy (herencia)

- `SINGLE` — tabla única con discriminador (defecto)
- `JOINED` — tabla por clase con JOIN (⚠️ navegar campos de subclase en JPQL requiere subselect)
- `CLASS` — tabla por clase concreta

### repository

- `default` — genera repositorio Java concreto
- `abstract` — genera repositorio abstract (la entidad SÍ es concreta, solo el repo es abstract)
- `none` — no genera repositorio

> ℹ️ `repository="abstract"` NO significa que la entidad sea abstracta. La entidad tiene tabla y se puede instanciar. Solo el repositorio Java es abstract.

---

## Tipos de campo

Todos los campos comparten atributos base (`name` ✓, `title`, `help`, `required`, `readonly`, `hidden`, `nullable`, `unique`, `column`, `default`, `initParam`, `formula`, `transient`, `json`, `equalsInclude`, `copy`).

### Campos simples

| Elemento | Tipo Java | Notas |
|---|---|---|
| `<string>` | String | + `large`, `encrypted`, `password`, `translatable`, `selection`, `max`, `min`, `pattern`, `multiline` |
| `<integer>` | Integer | + `min`, `max`, `selection` |
| `<long>` | Long | + `min`, `max`, `selection` |
| `<decimal>` | BigDecimal | + `min`, `max`, `precision`, `scale`, `selection` |
| `<boolean>` | Boolean | |
| `<date>` | LocalDate | |
| `<time>` | LocalTime | |
| `<datetime>` | ZonedDateTime/LocalDateTime | + `tz` (boolean, defecto true para ZonedDateTime) |
| `<binary>` | byte[] | + `image` |
| `<enum>` | Java Enum | + `ref` (nombre del enum declarado) |

### Campos relacionales

| Elemento | Relación JPA | Atributos clave |
|---|---|---|
| `<many-to-one>` | @ManyToOne | `ref` ✓ (FQN clase objetivo), `column` |
| `<one-to-one>` | @OneToOne | `ref` ✓, `mappedBy`, `orphanRemoval` |
| `<one-to-many>` | @OneToMany | `ref` ✓, `mappedBy` (obligatorio si bidireccional) |
| `<many-to-many>` | @ManyToMany | `ref` ✓, `mappedBy`, tabla intermedia automática |

#### Atributos comunes en relacionales

| Atributo | Descripción |
|---|---|
| `ref` | FQN de la clase destino (obligatorio) |
| `mappedBy` | Campo en la clase destino (lado inverso) |
| `cascade` | Operaciones en cascada: `persist`, `merge`, `remove`, `all` |
| `fetch` | `lazy` (defecto) o `eager` |
| `orphanRemoval` | Eliminar huérfanos (o2o, o2m) |
| `orderBy` | Ordenación por defecto del resultado |

---

## Atributos de campo especiales

| Atributo | Descripción |
|---|---|
| `formula` | Si `true`, el campo es calculado con SQL puro (no persistido). ⚠️ Bypassa el sistema de permisos JPA |
| `transient` | Campo no persistido (calculado en Java) |
| `json` | Almacenado en la columna JSON `attrs` |
| `selection` | Nombre de la `<selection>` de opciones |
| `copy` | Si `false`, se excluye al duplicar el registro |
| `equalsInclude` | Incluir/excluir en equals() |
| `column` | Nombre de columna en BD |
| `nullable` | Permitir NULL en BD |

---

## Elementos dentro de `<entity>`

| Elemento | Descripción |
|---|---|
| `<unique-constraint columns="..."/>` | Restricción UNIQUE multi-columna |
| `<index columns="..."/>` | Índice en columna(s) |
| `<finder-method>` | Genera método finder en el repositorio |
| `<extra-imports>` | Imports adicionales en la clase generada |
| `<extra-code>` | Código Java adicional insertado en la clase |
| `<extra-imports-model>` | Imports en la clase modelo (custom) |
| `<extra-code-model>` | Código adicional en la clase modelo |
| `<track>` | Configuración de seguimiento de cambios |
| `<entity-listener>` | Listener de ciclo de vida JPA |

---

## `<enum>` (tipo de enumeración)

```xml
<enum name="EstadoExpediente">
  <item name="BORRADOR" title="Borrador" value="BORRADOR"/>
  <item name="TRAMITACION" title="En tramitación"/>
  <item name="RESUELTO" title="Resuelto" hidden="false"/>
</enum>
```

### Atributos de `<enum>`

| Atributo | Descripción |
|---|---|
| `name` | Nombre del enum (mayúscula) |

### Atributos de `<item>`

| Atributo | Descripción |
|---|---|
| `name` | Constante (`[A-Z][A-Z0-9_]*[A-Z0-9]`) |
| `title` | Texto mostrado en UI |
| `value` | Valor personalizado almacenado |
| `help` | Texto de ayuda |
| `icon` | Icono |
| `order` | Orden de aparición |
| `hidden` | Ocultar de la selección UI |
| `data-description` | Descripción para widget Stepper |

---

## Patrones JPQL del proyecto (lecciones aprendidas)

- `:__user__` es el objeto `User` completo (no un Long) → usar `cu.usuario = :__user__` sin `.id`
- `self` en subconsultas EXISTS puede no correlacionarse → usar `self.id IN (SELECT ...)`
- Entidades con herencia JOINED (`TipoUsuario`, `CentroUsuario`) → navegar a campos de subtipo puede fallar; usar subselect explícito:
  ```jpql
  t.tipoUsuario IN (SELECT tu FROM TipoUsuario tu WHERE tu.code = 'ADMINISTRADOR')
  ```