---
type: implementation-task
---

# Tarea 11 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit

## Filas de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/resources/data-demo/input/firmas-demo.xml` | Crear | k-datainit | Las ocho tareas de firma precargadas |
| `src/main/resources/data-demo/input-config.xml` | Modificar | k-datainit | Añade el `<input>` de `firmas-demo.xml` |

Los dos ficheros van **juntos** en esta tarea porque son un único componente lógico: el `<input>` de
`input-config.xml` describe exactamente el binding de `firmas-demo.xml` y ninguno de los dos tiene sentido sin
el otro. `firmas-demo.xml` es `Acción: Crear`; `input-config.xml` es `Acción: Modificar` (el fichero **ya
existe**: se le **añade** el `<input>` al final del `<xml-inputs>` **conservando íntegros** todos los `<input>`
que ya tiene, sin borrar ni reordenar ninguno).

Ninguno de los dos está materializado en `design/`: se escriben a partir del texto del diseño que sigue.

## Texto del diseño (verbatim)

### Paso 10 — Datos de demo: las ocho tareas de firma precargadas

**Ficheros:**
`src/main/java/com/educaflow/secretariavirtual/datademo/TareaFirmaDemoNotifier.java` (Crear)
`src/main/java/com/educaflow/secretariavirtual/datademo/TareaFirmaDemoLoader.java` (Crear)
`src/main/resources/data-demo/input/firmas-demo.xml` (Crear)
`src/main/resources/data-demo/input-config.xml` (Modificar)

Son **datos de demo**, no datos iniciales: van en `src/main/resources/data-demo/` junto a `usuarios-demo.xml`
(lo pide `design-guidelines.md`) y por tanto solo se cargan con `data.import.demo-data = true`.
**MUST NOT** ponerlos en la `data-init` del subsistema.

#### 10.1 `firmas-demo.xml` — los datos

Raíz `<datos>`, con un nodo `<tareasFirma>` que contiene ocho `<tareaFirma>`, cada uno con estos atributos:
`firmante` (el `code` del `User`, que en este proyecto es su email), `motivoFirma` (el nombre visible de la
tarea) y `numeroDocumentos`.

| `firmante` | `motivoFirma` | `numeroDocumentos` | Lo usa |
|---|---|---|---|
| `director@mislata.es` | Firma de prueba 1 | 1 | ESC-001 |
| `director@mislata.es` | Firma de prueba 2 | 1 | ESC-002 |
| `director@mislata.es` | Firma de prueba 3 | **2** | ESC-003 |
| `director@mislata.es` | Firma de prueba 4 | 1 | ESC-011 |
| `director@mislata.es` | Firma de prueba 5 | 1 | ESC-004, ESC-005, ESC-012, ESC-013, ESC-010 |
| `secretario@mislata.es` | Firma de prueba del secretario | 1 | ESC-006, ESC-007, ESC-010 |
| `admin` | Firma de prueba del administrador 1 | 1 | ESC-008, ESC-010 |
| `admin` | Firma de prueba del administrador 2 | 1 | ESC-009 |

#### 10.2 `input-config.xml` — el binding (delta)

Se **añade** un `<input>` al final del `<xml-inputs>` existente, **conservando íntegros todos los `<input>` que
el fichero ya tiene** (los de `centros-demo.xml`, los de `usuarios-demo.xml` y los de `permisos-demo.xml`): el
delta es **solo la adición**, no se borra ni se reordena ninguno. Es también el **último** del fichero a
propósito, porque la tarea de firma necesita que su firmante (`User`) ya exista:

- `file="firmas-demo.xml"`, `root="datos"`.
- `<bind node="tareasFirma/tareaFirma" type="com.educaflow.subsystem.firmas.db.TareaFirma"`
  `search="self.motivoFirma = :motivoFirma AND self.firmante.code = :firmanteCode" create="true" update="false"`
  `call="com.educaflow.secretariavirtual.datademo.TareaFirmaDemoLoader:crearDocumentos"`.
  El `search` por (motivo, firmante) es la clave natural: recargar los datos de demo no duplica tareas, y
  `update="false"` impide que una recarga pise una tarea que un test ya resolvió.
- Binds internos: `@motivoFirma` → `motivoFirma`; `@firmante` como alias `firmanteCode` + `<bind to="firmante"`
  `type="com.axelor.auth.db.User" search="self.code = :firmanteCode" create="false" update="false"/>`;
  `@numeroDocumentos` como alias `numeroDocumentos` (solo lo consume el `call`); y valores fijos por `eval`
  para `estadoTareaFirma` (`PENDIENTE`), `fechaSolicitud` (la fecha/hora actual), `x` (75), `y` (200),
  `width` (400), `height` (60) y `page` (1) — el mismo recuadro que el PDF de ejemplo deja libre.
- El `motivoFirma` de la tarea es el nombre por el que el firmante la identifica en el listado, que es lo que
  usan todos los escenarios.

**MUST** respetar la firma exacta que exige el data-import para el `call=`: `(Object bean, Map values)`
devolviendo el bean (`k-datainit/input-config.md`).

**Verificación:** con `data.import.demo-data = true` y la BD recreada, arrancar y comprobar en `psql`:
```sql
SELECT t.motivo_firma, u.code, count(d.id)
FROM firmas_tarea_firma t
JOIN auth_user u ON u.id = t.firmante
LEFT JOIN firmas_documento_firma d ON d.tarea_firma = t.id
GROUP BY t.motivo_firma, u.code ORDER BY 1;
```
Ocho filas; «Firma de prueba 3» con 2 documentos y el resto con 1.

### Notas y supuestos aplicables

9. **Reintentos y datos de demo.** Los escenarios que **resuelven** una tarea (ESC-001, 002, 003, 009, 011) la
   dejan en `FIRMADO`/`RECHAZADO` y una tarea resuelta no vuelve a `PENDIENTE`; por eso el spec da una tarea
   distinta a cada uno. Reejecutar la tanda completa de tests E2E exige **recrear la base de datos** para que
   los datos de demo vuelvan a cargarse. Los escenarios que dan de alta un certificado sí son reejecutables
   porque empiezan borrando la entrada del DNI, siguiendo el patrón de
   `src/test/e2e/subsystem/criptografia/`.
