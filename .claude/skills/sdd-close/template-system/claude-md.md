# Contrato — `CLAUDE.md` de un sistema/subsistema

Lo lee el **documentador**. Define el formato exacto del `CLAUDE.md` que regenera en la raíz del sistema. Regla rectora: el `CLAUDE.md` responde a **"¿qué necesita saber un agente que nunca ha visto esta carpeta para trabajar en ella sin leer todo el código?"**.

---

## 1. Criterio de inclusión

- **CRITERIO**: si un agente experimentado con Axelor pero que nunca ha visto esta carpeta podría inferirlo en **30 segundos** leyendo el código → **NO** lo incluyas. Si tardaría **10 minutos** o requiere contexto externo → **SÍ**.
- **MUST NOT** incluir:
  - Campos de entidades (ya están en los `domains/*.xml` y en `modelo.puml`).
  - Que existe "un servicio" o "un repositorio" (es la arquitectura estándar).
  - Javadoc de métodos privados, getters/setters, métodos heredados (`DefaultModelService`, `JpaRepository`).
  - Cualquier cosa que sea consecuencia directa del nombre de la carpeta.
- **MUST** incluir lo no obvio: desviaciones del patrón estándar, restricciones ocultas, workarounds, decisiones contraintuitivas, dependencias con otros sistemas.

---

## 2. Formato literal del `CLAUDE.md`

Genera el fichero con esta estructura exacta. Una sección cuya tabla quedaría vacía se **OMITE entera** (no se deja el encabezado con la tabla vacía).

```markdown
## ¿Para qué sirve esto?
[1-2 frases. Lo que no se infiere del nombre de la carpeta.]

## Modelo de datos
Ver `modelo.puml` / `modelo.png` (esquema de entidades de este sistema, generado desde `domains/*.xml`).
[1-2 frases SOLO si hay algo no evidente del esquema: invariantes entre entidades, por qué una relación
es como es, una entidad que no es lo que su nombre sugiere. Si el esquema se explica solo, deja solo la
línea de referencia. OMITE esta sección entera si el sistema no tiene `domains/*.xml`.]

## Lo no obvio
[Solo si hay algo que se desvía de la arquitectura estándar (DefaultModelService, @CallMethod,
action-views en XML, etc.), restricciones ocultas, workarounds, otras clases además de controladores y
servicios, decisiones contraintuitivas. Si todo sigue el patrón estándar, OMITE esta sección.]

## Controladores y métodos (una tabla por controlador)
| Método | Qué hace en una línea |
|---|---|
| `NombreControlador.metodo(params)` | descripción |
[Solo métodos públicos relevantes — no getters/setters.]

## Servicios y métodos públicos (una tabla por servicio)
| Método | Qué hace en una línea |
|---|---|
| `NombreService.metodo(params)` | descripción |
[Solo métodos públicos relevantes — no getters/setters, no heredados de DefaultModelService.]

## Repositorios y métodos públicos (una tabla por repositorio)
| Método | Qué hace en una línea |
|---|---|
| `NombreRepository.metodo(params)` | descripción |
[Solo métodos públicos relevantes — no getters/setters, no heredados de JpaRepository.]

## Vistas (una tabla por vista)
| Vista | Para qué |
|---|---|
| `nombre-vista` | descripción |

## Dependencias
Tabla con dependencias con otros subsistemas:
| Subsistema | Para qué |
|---|---|
| `nombre-subsistema` | motivo |

Tabla con dependencias con infraestructura:
| Infraestructura | Para qué |
|---|---|
| `nombre` | motivo |
```

---

## 3. Reglas de escritura

1. **MUST** escribir el `CLAUDE.md` en la **raíz** del sistema, sobrescribiendo si ya existe.
2. **MUST** empezar siempre por `## ¿Para qué sirve esto?`.
3. La sección `## Modelo de datos` solo aparece si el sistema tiene `domains/*.xml` (entonces es **obligatoria** y referencia `modelo.puml`/`modelo.png`).
4. Cuerpo en **español**; nombres de clase/método/vista en `código`.
5. **MUST NOT** superar lo que el criterio de §1 justifica: un `CLAUDE.md` largo de cosas obvias es peor que uno corto.

---

## 4. Checklist del documentador (parte `CLAUDE.md`)

Antes de devolver el token, **MUST** auto-verificar:

- [ ] ¿Existe el `CLAUDE.md` recién escrito en la raíz del sistema?
- [ ] ¿Empieza por `## ¿Para qué sirve esto?`?
- [ ] ¿No quedó trivialmente vacío (tiene contenido real, no solo encabezados)?
- [ ] ¿Toda tabla con encabezado tiene al menos una fila, o se omitió la sección entera?
- [ ] ¿Si el sistema tiene `domains/*.xml`, está la sección `## Modelo de datos` referenciando `modelo.puml`/`modelo.png`?
- [ ] ¿No incluye campos de entidad, getters/setters ni "existe un servicio/repositorio"?

**LIMIT**: máximo 3 iteraciones de corrección. Si tras la 3ª sigue fallando, devuelve `BLOQUEADO` con el motivo.
