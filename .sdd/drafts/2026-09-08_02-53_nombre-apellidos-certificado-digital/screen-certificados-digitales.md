# Pantalla: Certificados digitales

**Pantalla existente:** sí

## Identidad

- **Quién la usa:** Administrador, en edición.
- **Qué muestra:** todos los certificados digitales dados de alta, de todas las personas, y el formulario de cada uno. Cambia: el listado y el formulario incorporan el nombre y los apellidos del titular, y el formulario refleja que el DNI y, en su caso, el nombre y los apellidos no se pueden cambiar.

## Menú

- Administración SV → Certificados digitales — lo ve el Administrador; lleva a esta pantalla (sin cambios).

## Estructura jerárquica de las vistas

```
Listado de certificados digitales
└── Formulario de certificado digital  (se abre al pulsar una fila o con «Añadir certificado digital»)
```

---

## Vista: Listado de certificados digitales

- **Slug:** listado
- **Tipo:** listado
- **Qué muestra:** todos los certificados digitales, en lectura.
- **Se abre desde:** es la vista de entrada de la pantalla.

### Propiedades

- **Columnas (en orden):** DNI, nombre, apellidos, tipo de certificado, habilitado
- **Ordenación por defecto:** por DNI ascendente y, dentro del mismo DNI, los habilitados antes que los deshabilitados
- **Búsqueda / filtros:** sin cambios respecto a la pantalla actual
- **Al pulsar una fila abre:** el formulario de certificado digital

### Botones

- **Añadir certificado digital** (barra superior) — abre el formulario de alta (sin cambios).

## Vista: Formulario de certificado digital

- **Slug:** formulario
- **Tipo:** formulario
- **Qué muestra:** los datos de un certificado digital, en edición.
- **Se abre desde:** el listado de certificados digitales, al pulsar una fila o «Añadir certificado digital».

### Propiedades

- **Modo:** editable, salvo el DNI (solo lectura una vez guardado el certificado) y el nombre y los apellidos cuando se tomaron de la ficha de un usuario (solo lectura siempre).

### Paneles

- **Certificado digital** (normal) — se añaden nombre y apellidos junto al DNI, antes del tipo de certificado; el resto del panel no cambia.

### Botones

*(solo los botones estándar: Guardar, Cancelar, Borrar)*

### Reglas de UI

- RUI-certificados-digitales-formulario-001 — Al escribir un DNI que corresponde a un usuario de la aplicación, el nombre y los apellidos se rellenan con los de la ficha de ese usuario y quedan de solo lectura
  - disparador: al cambiar DNI
  - condición: existe un usuario de la aplicación cuyo documento es el DNI escrito
- RUI-certificados-digitales-formulario-002 — Al escribir un DNI que no corresponde a ningún usuario de la aplicación, el nombre y los apellidos se vacían y quedan editables y marcados como obligatorios
  - disparador: al cambiar DNI
  - condición: no existe ningún usuario de la aplicación cuyo documento sea el DNI escrito
- RUI-certificados-digitales-formulario-003 — El DNI es de solo lectura en un certificado ya guardado
  - disparador: al cargar
  - condición: el certificado ya existe
- RUI-certificados-digitales-formulario-004 — El nombre y los apellidos son de solo lectura en un certificado ya guardado cuyo nombre se tomó del usuario
  - disparador: al cargar
  - condición: el certificado ya existe y tiene «nombre tomado del usuario» a «sí»
- RUI-certificados-digitales-formulario-005 — El nombre y los apellidos son editables y están marcados como obligatorios en un certificado ya guardado cuyo nombre escribió el administrador (incluidos los certificados anteriores a este cambio)
  - disparador: al cargar
  - condición: el certificado ya existe y tiene «nombre tomado del usuario» a «no»
- RUI-certificados-digitales-formulario-006 — Al crear un certificado, «Habilitado» viene marcado por defecto (sin cambios)
  - disparador: al crear
  - condición: Siempre
