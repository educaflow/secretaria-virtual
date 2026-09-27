---
type: specification
template: system
---

# Objetivo

Permitir que el supervisor de un centro y el administrador mantengan (ver, crear, modificar y borrar) los perfiles que un centro concede sobre cada trámite: qué perfil (Creador, Tramitador, Director…) recibe, en un centro y para un trámite, un tipo de usuario, un cargo o un usuario concreto de ese centro. Es una ampliación del **subsistema** de seguridad, que es quien decide qué perfiles tiene cada usuario sobre un trámite; depende funcionalmente de los centros, usuarios, tipos de usuario y cargos, y del catálogo de trámites.

**Modifica:** subsystem/security

# Actores

- **Supervisor**: usuario con el tipo de usuario «Supervisor del centro» en uno o varios centros; gestiona los perfiles de trámites de los centros de los que es supervisor.
- **Administrador**: administrador global de la aplicación; gestiona los perfiles de trámites de cualquier centro.

# Historias de usuario

## HU-001 — Como Supervisor quiero dar perfiles sobre los trámites en mis centros para decidir quién puede crear y tramitar cada trámite en ellos

- ESC-001 — Alta de un perfil por cargo:
  1. El supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
  2. Abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
  3. Elige el centro «CIPFP Mislata», el trámite «Trámite de prueba», el perfil «Tramitador» y el cargo «Jefe de estudios», sin rellenar tipo de usuario ni usuario.
  4. Pulsa «Guardar».
  5. Vuelve al listado.
  6. El sistema muestra una fila con centro «CIPFP Mislata», trámite «Trámite de prueba», perfil «Tramitador» y cargo «Jefe de estudios», con el tipo de usuario y el usuario vacíos.
- ESC-002 — Alta de un perfil para un usuario concreto:
  1. El supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
  2. Abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
  3. Elige el centro «CIPFP Mislata», el trámite «Justificación de falta del profesorado», el perfil «Colaborador» y el usuario «Profesor1 CIPFP Mislata», sin rellenar tipo de usuario ni cargo.
  4. Pulsa «Guardar».
  5. Vuelve al listado.
  6. El sistema muestra una fila con centro «CIPFP Mislata», trámite «Justificación de falta del profesorado», perfil «Colaborador» y usuario «Profesor1 CIPFP Mislata», con el tipo de usuario y el cargo vacíos.
- ESC-003 — Modificación del perfil de una fila:
  1. El supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
  2. Abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
  3. Elige el centro «CIPFP Mislata», el trámite «Trámite de prueba», el perfil «Creador» y el tipo de usuario «Alumno», sin rellenar cargo ni usuario.
  4. Pulsa «Guardar».
  5. Vuelve al listado.
  6. Abre la fila con centro «CIPFP Mislata», trámite «Trámite de prueba», perfil «Creador» y tipo de usuario «Alumno».
  7. El sistema muestra el centro «CIPFP Mislata» sin posibilidad de cambiarlo.
  8. Cambia el perfil a «Afectado».
  9. Pulsa «Guardar».
  10. Vuelve al listado.
  11. El sistema muestra la fila del centro «CIPFP Mislata» con trámite «Trámite de prueba», perfil «Afectado» y tipo de usuario «Alumno», y ya no muestra ninguna fila con perfil «Creador» y tipo de usuario «Alumno».
- ESC-004 — Borrado de una fila:
  1. El supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
  2. Abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
  3. Elige el centro «CIPFP Mislata», el trámite «Anulación de matrícula en ciclo formativo», el perfil «Auditor» y el tipo de usuario «Administrativo», sin rellenar cargo ni usuario.
  4. Pulsa «Guardar».
  5. Vuelve al listado.
  6. Abre la fila con centro «CIPFP Mislata», trámite «Anulación de matrícula en ciclo formativo», perfil «Auditor» y tipo de usuario «Administrativo».
  7. Pulsa «Borrar».
  8. Confirma el borrado.
  9. El sistema borra la fila y el listado ya no muestra ninguna fila con trámite «Anulación de matrícula en ciclo formativo», perfil «Auditor» y tipo de usuario «Administrativo».
- ESC-005 — El supervisor solo ve y usa sus centros:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Administración → Perfiles de trámites por centro» y pulsa «Nuevo».
  3. Elige el centro «CIPFP Batoi», el trámite «Trámite de prueba», el perfil «Director» y el cargo «Director», sin rellenar tipo de usuario ni usuario.
  4. Pulsa «Guardar».
  5. El administrador cierra sesión.
  6. El supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
  7. Abre «Mi centro → Perfiles de trámites».
  8. El sistema no muestra ninguna fila del centro «CIPFP Batoi».
  9. Pulsa «Nuevo» y despliega el selector de centro.
  10. El sistema ofrece el centro «CIPFP Mislata» y no ofrece «CIPFP Batoi».
- ESC-020 — Otros usuarios del centro no ven las pantallas de perfiles:
  1. El director inicia sesión con usuario «director@mislata.es» y contraseña «demo1234».
  2. Despliega el menú.
  3. El sistema no muestra la opción «Mi centro → Perfiles de trámites» ni la opción «Administración → Perfiles de trámites por centro».
- ESC-021 — El supervisor no ve la pantalla del administrador:
  1. El supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
  2. Despliega el menú.
  3. El sistema muestra la opción «Mi centro → Perfiles de trámites» y no muestra la opción «Administración → Perfiles de trámites por centro».
- ESC-022 — El perfil concedido surte efecto:
  1. El profesor inicia sesión con usuario «profesor1@mislata.es» y contraseña «demo1234».
  2. Abre «Mis trámites → Nuevo trámite» y elige el centro «CIPFP Mislata».
  3. El sistema no ofrece el trámite «Anulación de matrícula en ciclo formativo».
  4. El profesor cierra sesión.
  5. El supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
  6. Abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
  7. Elige el centro «CIPFP Mislata», el trámite «Anulación de matrícula en ciclo formativo», el perfil «Creador» y el usuario «Profesor1 CIPFP Mislata», sin rellenar tipo de usuario ni cargo.
  8. Pulsa «Guardar».
  9. Vuelve al listado.
  10. El sistema muestra una fila con centro «CIPFP Mislata», trámite «Anulación de matrícula en ciclo formativo», perfil «Creador» y usuario «Profesor1 CIPFP Mislata».
  11. El supervisor cierra sesión.
  12. El profesor inicia sesión con usuario «profesor1@mislata.es» y contraseña «demo1234».
  13. Abre «Mis trámites → Nuevo trámite» y elige el centro «CIPFP Mislata».
  14. El sistema ofrece el trámite «Anulación de matrícula en ciclo formativo».

## HU-002 — Como Supervisor quiero que el sistema me impida guardar perfiles mal formados para que cada fila diga sin ambigüedad a quién se da el perfil

- ESC-006 — Sin destinatario:
  1. El supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
  2. Abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
  3. Elige el centro «CIPFP Mislata», el trámite «Trámite de prueba» y el perfil «Tramitador», sin rellenar tipo de usuario, cargo ni usuario.
  4. Pulsa «Guardar».
  5. El sistema no guarda la fila y muestra «Indica a quién se da el perfil: un tipo de usuario, un cargo o un usuario».
- ESC-007 — Más de un destinatario:
  1. El supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
  2. Abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
  3. Elige el centro «CIPFP Mislata», el trámite «Trámite de prueba», el perfil «Tramitador», el tipo de usuario «Profesor» y el cargo «Jefe de estudios».
  4. Pulsa «Guardar».
  5. El sistema no guarda la fila y muestra «Indica solo uno: un tipo de usuario, un cargo o un usuario».
- ESC-008 — Solo se ofrecen usuarios del centro de la fila:
  1. El supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
  2. Abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
  3. Elige el centro «CIPFP Mislata».
  4. Despliega el selector de usuario y escribe «Profesor1» en la búsqueda.
  5. El sistema ofrece «Profesor1 CIPFP Mislata» y no ofrece «Profesor1 CIPFP Batoi».
- ESC-009 — Asignación repetida:
  1. El supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
  2. Abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
  3. Elige el centro «CIPFP Mislata», el trámite «Trámite de prueba», el perfil «Creador» y el tipo de usuario «Profesor», sin rellenar cargo ni usuario.
  4. Pulsa «Guardar».
  5. Vuelve al listado y pulsa «Nuevo».
  6. Elige exactamente los mismos valores: centro «CIPFP Mislata», trámite «Trámite de prueba», perfil «Creador» y tipo de usuario «Profesor», sin rellenar cargo ni usuario.
  7. Pulsa «Guardar».
  8. El sistema no guarda la segunda fila y muestra «Ya existe esa asignación de perfil».
  9. Pulsa «Cancelar» y vuelve al listado.
  10. El sistema muestra una sola fila con centro «CIPFP Mislata», trámite «Trámite de prueba», perfil «Creador» y tipo de usuario «Profesor».
- ESC-016 — Sin trámite:
  1. El supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
  2. Abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
  3. Elige el centro «CIPFP Mislata», el perfil «Tramitador» y el cargo «Jefe de estudios», sin elegir trámite.
  4. Pulsa «Guardar».
  5. El sistema no guarda la fila y muestra «El trámite es obligatorio».
- ESC-017 — Sin perfil:
  1. El supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
  2. Abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
  3. Elige el centro «CIPFP Mislata», el trámite «Trámite de prueba» y el cargo «Jefe de estudios», sin elegir perfil.
  4. Pulsa «Guardar».
  5. El sistema no guarda la fila y muestra «El perfil es obligatorio».
- ESC-018 — Sin centro:
  1. El supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
  2. Abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
  3. El sistema muestra el centro ya rellenado con «CIPFP Mislata», porque el supervisor solo lo es de ese centro.
  4. Vacía el centro y elige el trámite «Trámite de prueba», el perfil «Tramitador» y el cargo «Jefe de estudios».
  5. Pulsa «Guardar».
  6. El sistema no guarda la fila y muestra «El centro es obligatorio».
- ESC-019 — Asignación repetida al modificar:
  1. El supervisor inicia sesión con usuario «supervisor1@mislata.es» y contraseña «demo1234».
  2. Abre «Mi centro → Perfiles de trámites» y pulsa «Nuevo».
  3. Elige el centro «CIPFP Mislata», el trámite «Trámite de prueba», el perfil «Creador» y el tipo de usuario «Alumno», y pulsa «Guardar».
  4. Vuelve al listado y pulsa «Nuevo».
  5. Elige el centro «CIPFP Mislata», el trámite «Trámite de prueba», el perfil «Afectado» y el tipo de usuario «Alumno», sin rellenar cargo ni usuario.
  6. Pulsa «Guardar».
  7. Vuelve al listado y abre la fila con perfil «Afectado» y tipo de usuario «Alumno».
  8. Cambia el perfil a «Creador» y pulsa «Guardar».
  9. El sistema no guarda el cambio y muestra «Ya existe esa asignación de perfil».
  10. Pulsa «Cancelar» y vuelve al listado.
  11. El sistema muestra la fila con perfil «Afectado» y tipo de usuario «Alumno», y una sola fila con perfil «Creador» y tipo de usuario «Alumno».

## HU-003 — Como Administrador quiero gestionar los perfiles de trámites de cualquier centro para configurar centros que no tienen supervisor o corregir sus asignaciones

- ESC-010 — Alta y consulta en varios centros:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Administración → Perfiles de trámites por centro» y pulsa «Nuevo».
  3. Elige el centro «CIPFP Batoi», el trámite «Trámite de prueba», el perfil «Secretario» y el cargo «Secretario», sin rellenar tipo de usuario ni usuario.
  4. Pulsa «Guardar».
  5. Pulsa «Nuevo».
  6. Elige el centro «CIPFP Mislata», el trámite «Trámite de prueba», el perfil «Secretario» y el cargo «Secretario», sin rellenar tipo de usuario ni usuario.
  7. Pulsa «Guardar».
  8. Vuelve al listado.
  9. El sistema muestra las dos filas: una del centro «CIPFP Batoi» y otra del centro «CIPFP Mislata», ambas con trámite «Trámite de prueba», perfil «Secretario» y cargo «Secretario».
- ESC-011 — Modificación de una fila por el administrador:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Administración → Perfiles de trámites por centro» y pulsa «Nuevo».
  3. Elige el centro «CIPFP Batoi», el trámite «Trámite de prueba», el perfil «Creador» y el tipo de usuario «Profesor», sin rellenar cargo ni usuario.
  4. Pulsa «Guardar».
  5. Vuelve al listado y abre la fila recién creada.
  6. El sistema muestra el centro «CIPFP Batoi» sin posibilidad de cambiarlo.
  7. Cambia el perfil a «Colaborador» y pulsa «Guardar».
  8. El sistema guarda el cambio y, al volver al listado, la fila del centro «CIPFP Batoi» muestra trámite «Trámite de prueba», perfil «Colaborador» y tipo de usuario «Profesor».
- ESC-012 — Borrado de una fila por el administrador:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Administración → Perfiles de trámites por centro» y pulsa «Nuevo».
  3. Elige el centro «CIPFP Batoi», el trámite «Anulación de matrícula en ciclo formativo», el perfil «Auditor» y el cargo «Secretario», y pulsa «Guardar».
  4. Vuelve al listado y abre la fila del centro «CIPFP Batoi» con trámite «Anulación de matrícula en ciclo formativo», perfil «Auditor» y cargo «Secretario».
  5. Pulsa «Borrar».
  6. Confirma el borrado cuando el sistema pide confirmación.
  7. El sistema borra la fila y el listado ya no muestra ninguna fila del centro «CIPFP Batoi» con trámite «Anulación de matrícula en ciclo formativo», perfil «Auditor» y cargo «Secretario».
- ESC-013 — Solo se ofrecen usuarios del centro de la fila (administrador):
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Administración → Perfiles de trámites por centro» y pulsa «Nuevo».
  3. Elige el centro «CIPFP Batoi» y despliega el selector de usuario buscando «Profesor1».
  4. El sistema ofrece «Profesor1 CIPFP Batoi» y no ofrece «Profesor1 CIPFP Mislata».
- ESC-014 — Filtrar el listado por centro:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Administración → Perfiles de trámites por centro» y pulsa «Nuevo».
  3. Elige el centro «CIPFP Batoi», el trámite «Trámite de prueba», el perfil «Tramitador» y el cargo «Jefe de estudios», y pulsa «Guardar».
  4. Pulsa «Nuevo», elige el centro «CIPFP Mislata», el trámite «Trámite de prueba», el perfil «Tramitador» y el cargo «Jefe de estudios», y pulsa «Guardar».
  5. Vuelve al listado.
  6. En la búsqueda del listado, filtra por centro escribiendo «CIPFP Batoi» y aplica el filtro.
  7. El sistema muestra la fila del centro «CIPFP Batoi» con trámite «Trámite de prueba», perfil «Tramitador» y cargo «Jefe de estudios» y no muestra ninguna fila del centro «CIPFP Mislata».
- ESC-015 — Usuario que no pertenece al centro de la fila:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre «Administración → Perfiles de trámites por centro» y pulsa «Nuevo».
  3. Elige el centro «CIPFP Mislata», el trámite «Trámite de prueba», el perfil «Colaborador» y el usuario «Profesor1 CIPFP Mislata».
  4. Cambia el centro a «CIPFP Batoi» sin cambiar el usuario.
  5. Pulsa «Guardar».
  6. El sistema no guarda la fila y muestra «El usuario no pertenece al centro».

# Modelos

| Fichero | Modelo | Qué representa |
|---|---|---|
| [entity-AceProfileCentro.md](./entity-AceProfileCentro.md) | AceProfileCentro | Perfil que un centro concede sobre un trámite a un tipo de usuario, a un cargo o a un usuario concreto del centro. |

Cada AceProfileCentro referencia un centro, un trámite y exactamente uno de estos destinatarios: un tipo de usuario, un cargo o un usuario. Todos ellos son entidades externas e independientes que existen por su cuenta; borrar una fila de perfil no afecta a ninguna de ellas.

# Pantallas

| Fichero | Pantalla | Para qué sirve |
|---|---|---|
| [screen-perfiles-tramites-mi-centro.md](./screen-perfiles-tramites-mi-centro.md) | Perfiles de trámites (mi centro) | El supervisor mantiene los perfiles de trámites de los centros de los que es supervisor. |
| [screen-perfiles-tramites-todos-centros.md](./screen-perfiles-tramites-todos-centros.md) | Perfiles de trámites por centro (administración) | El administrador mantiene los perfiles de trámites de cualquier centro. |

# Seguridad

- **Supervisor:** puede ver, crear, modificar y borrar los perfiles de trámites **solo de los centros en los que es supervisor** (todos ellos, no solo el que tenga activo). Al crear, solo puede elegir uno de esos centros; no ve ni puede modificar ni borrar filas de otros centros.
- **Administrador:** puede ver, crear, modificar y borrar los perfiles de trámites de **todos los centros**.

# Recursos y datos iniciales

*(no aplica)*

Los escenarios usan los centros, usuarios, tipos de usuario y cargos de los datos de demo y los trámites que ya existen en la aplicación («Trámite de prueba», «Justificación de falta del profesorado» y «Anulación de matrícula en ciclo formativo»). La tabla de perfiles por centro parte vacía.

# Fuera de alcance

- Dar un perfil a la combinación de un tipo de usuario **y** un cargo a la vez: cada fila tiene un único destinatario.
- Cambiar la forma en que se calculan los perfiles de un usuario, o qué trámites y expedientes puede ver cada usuario.
- Mantener las demás tablas de perfiles (globales, por tipo de trámite, por trámite, por tipo de expediente y por expediente).
- Dar acceso a esta pantalla a cualquier otro rol o cargo (Director, Secretario, Jefe de estudios…).
- Restringir qué perfiles puede conceder el supervisor: puede conceder cualquiera de los siete.
- Cambiar el centro de una fila ya creada: para eso se borra y se crea de nuevo.
