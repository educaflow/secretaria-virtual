# Pantalla: Perfiles de trámites (mi centro)

## Identidad

- **Quién la usa:** el Supervisor, en edición.
- **Qué muestra:** los perfiles de trámites de los centros en los que el usuario es supervisor (todos ellos) y, al entrar en uno, su detalle para modificarlo o borrarlo; también permite dar de alta perfiles nuevos en esos centros.

## Menú

- Mi centro → Perfiles de trámites — lo ve el Supervisor; lleva a esta pantalla.

## Estructura jerárquica de las vistas

```
Listado de perfiles de trámites
└── Formulario de perfil de trámite   (se abre al pulsar una fila o con «Nuevo»)
```

---

## Vista: Listado de perfiles de trámites

- **Slug:** listado
- **Tipo:** listado
- **Qué muestra:** los perfiles de trámites de los centros en los que el usuario es supervisor, en lectura.
- **Se abre desde:** es la vista de entrada de la pantalla.

### Propiedades

- **Columnas (en orden):** centro, trámite, perfil, tipo de usuario, cargo, usuario
- **Ordenación por defecto:** por centro, después por trámite y después por perfil, ascendente
- **Búsqueda / filtros:** sí, por centro, trámite y perfil
- **Al pulsar una fila abre:** el formulario de perfil de trámite

### Botones

- **Nuevo** (barra superior) — Abre el formulario de alta de un perfil de trámite.

## Vista: Formulario de perfil de trámite

- **Slug:** formulario
- **Tipo:** formulario
- **Qué muestra:** los datos de un perfil de trámite de uno de los centros del supervisor, en edición.
- **Se abre desde:** el listado de perfiles de trámites, al pulsar una fila o «Nuevo».

### Propiedades

- **Modo:** editable; el centro solo se puede elegir en el alta y en detalle es de solo lectura.

### Paneles

- **Perfil** (normal) — centro (se elige entre los centros en los que el usuario es supervisor), trámite, perfil
- **A quién se da** (normal) — tipo de usuario, cargo, usuario (se elige entre los usuarios que pertenecen al centro de la fila)

### Botones

*(solo los botones estándar: Guardar, Cancelar, Borrar)*

### Reglas de UI

- RUI-perfiles-tramites-mi-centro-formulario-001 — El selector de centro solo ofrece los centros en los que el usuario es supervisor
  - disparador: continuo
  - condición: Siempre
- RUI-perfiles-tramites-mi-centro-formulario-002 — El centro es de solo lectura una vez creada la fila
  - disparador: al cargar
  - condición: la fila ya existe
- RUI-perfiles-tramites-mi-centro-formulario-003 — El selector de usuario solo ofrece los usuarios que pertenecen al centro elegido en la fila
  - disparador: continuo
  - condición: Siempre
- RUI-perfiles-tramites-mi-centro-formulario-004 — Los campos centro, trámite y perfil se muestran como obligatorios
  - disparador: continuo
  - condición: Siempre
- RUI-perfiles-tramites-mi-centro-formulario-005 — Al crear un perfil de trámite, si el usuario es supervisor de un único centro, el centro se rellena con ese centro
  - disparador: al crear
  - condición: el usuario es supervisor de un solo centro
  - actor: Supervisor
