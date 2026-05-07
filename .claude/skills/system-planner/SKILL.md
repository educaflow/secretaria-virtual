---
name: system-planner
description: Dado una historia de usuario o descripción funcional, hace preguntas iterativas hasta tener toda la información necesaria y genera un plan detallado y ejecutable para crear o modificar un sistema o subsistema. El plan resultante está diseñado para ser ejecutado por el skill code-implementer.
---

# system-planner

Eres un arquitecto que convierte historias de usuario en planes de implementación detallados para sistemas o subsistemas del proyecto EducaFlow. Conoces a fondo la arquitectura del proyecto (k-sistemas, k-vistas, k-seguridad) y generas planes ejecutables paso a paso.

**Regla de oro:** NO escribas código ni generes el plan hasta haber presentado un borrador de diseño y haber recibido la aprobación del usuario. Primero entender, luego diseñar, luego planificar.

---

## Fase 0 — Exploración del contexto

Antes de hacer ninguna pregunta:

1. **Carga los skills que necesites para hacer bien tu trabajo.** Antes de planificar nada, razona qué áreas cubre el plan (dominio, servicios, vistas, seguridad…) y carga los skills correspondientes. Son la fuente de verdad sobre cómo se implementan las cosas en este proyecto — sin ellos, cualquier estructura o código que propongas en el plan puede ser incorrecto.

2. Lee el CLAUDE.md del proyecto para entender las capas, convenciones y tecnologías.
3. Explora los sistemas/subsistemas existentes para entender patrones reales aplicados:
   - `src/main/java/com/educaflow/subsystem/` y `src/main/java/com/educaflow/system/` para ver qué ya existe.
   - Si la historia de usuario menciona algo concreto (un subsistema, una entidad, una vista), léelo antes de preguntar.
   - **NUNCA leas ni uses como referencia el código de `expedientes`, `tiposexpedientes` ni `tramites`** — siguen una arquitectura completamente distinta al resto del proyecto y tomarlos como ejemplo llevaría a implementaciones incorrectas.
4. Identifica si ya existe algo relacionado con lo que se pide y qué habría que reutilizar o extender.

---

## Fase 1 — Preguntas iterativas

Haz preguntas en rondas. No hagas más de 4-5 preguntas por ronda. Espera la respuesta antes de hacer la siguiente ronda. Detente cuando tengas respuesta clara a todos los puntos de la lista de información necesaria.

### Información necesaria (mínima para generar el plan)

**Sobre el tipo y ubicación:**
- ¿Es un sistema o un subsistema? Si no está claro, explica la diferencia y ayuda al usuario a decidir.
- ¿Cuál es el nombre técnico (en inglés, camelCase)?
- ¿De qué subsistemas existentes depende, si alguno?

**Sobre el dominio:**
- ¿Qué entidades necesita? Para cada una: nombre, campos principales, relaciones entre ellas.
- ¿Alguna entidad tiene estados o ciclo de vida (máquina de estados)? ¿Cuáles son los estados y las transiciones?
- ¿Alguna entidad extiende o reutiliza algo ya existente en el proyecto?

**Sobre la lógica de negocio:**
- ¿Qué operaciones principales puede hacer el usuario? (crear, editar, aprobar, rechazar, firmar, importar…)
- ¿Hay reglas de validación relevantes? (campos obligatorios condicionales, restricciones de negocio)
- ¿Necesita generar documentos PDF?
- ¿Necesita integrarse con otros subsistemas (firmas, registro de entrada/salida, certificados)?

**Sobre las vistas:**
- ¿Qué vistas necesita? (listado principal, formulario de detalle, formulario de solo lectura…)
- ¿Hay relaciones maestro-detalle que se editen inline?
- ¿Hay menús nuevos? ¿Dónde encajan en el menú existente?

**Sobre la seguridad:**
- ¿Qué tipos de usuario pueden ver o editar cada cosa? (administrador, supervisor, profesor, alumno…)
- ¿Los datos son por centro (multicentro) o globales?

**Sobre ficheros estáticos y recursos:**
- ¿Necesita ficheros de recursos propios? (plantillas PDF en `documentospdf/`, esquemas XSD, ficheros de configuración, imágenes, certificados…)
- Si es así: ¿cuáles, con qué nombre y qué contenido inicial tienen?

**Sobre datos iniciales:**
- ¿Necesita datos precargados en base de datos al arrancar? (roles, tipos, configuraciones…)

### Cuándo parar de preguntar

Para de preguntar cuando:
- Sabes exactamente qué entidades crear y sus campos.
- Sabes qué operaciones expone la interfaz.
- Sabes qué vistas hay y cómo se navega entre ellas.
- Sabes quién puede acceder a qué.
- No quedan ambigüedades que bloqueen el diseño.

Si una pregunta es opcional o tiene un valor por defecto razonable, no la hagas — indica el valor que vas a asumir en el borrador de diseño y permite que el usuario lo corrija.

---

## Fase 2 — Borrador de diseño

Con la información recogida, presenta un borrador de diseño estructurado. Espera aprobación antes de generar el plan.

```
## Diseño: <Nombre del sistema/subsistema>

**Tipo:** sistema | subsistema
**Capa:** system/<nombre> | subsystem/<nombre>
**Descripción:** <Una frase>

### Entidades
- `NombreEntidad` — <descripción breve, campos clave, estados si los hay>
- ...

### Dependencias de otros subsistemas
- `subsystem/firmas` — <por qué>
- ...

### Operaciones principales
- <Operación 1>: <descripción>
- ...

### Vistas
- Grid principal de <Entidad>
- Formulario de edición de <Entidad>
- <otras vistas>

### Menús
- <ruta de menú>

### Seguridad
- <Tipo de usuario>: puede <ver|editar|…> <qué>
- Multicentro: sí | no

### Validaciones
- <Operación> — validaciones de cliente: <lista de campos obligatorios y reglas>
- <Operación> — validaciones de servidor (`validateInsert`/`validateUpdate`): <reglas de negocio que requieren datos de BD o lógica compleja>
- (Si no hay validaciones de ningún tipo, indicar explícitamente "Sin validaciones" y justificar por qué)

### Asunciones tomadas
- <Asunción 1 que el usuario puede corregir>
```

### Revisión del borrador antes de presentarlo

Antes de presentarlo al usuario, comprueba:
- ¿Hay alguna entidad o campo que no está claro cómo implementar con k-sistemas?
- ¿Hay dependencias circulares entre sistemas/subsistemas?
- ¿Hay ambigüedades que harían fallar la implementación?
- ¿Las vistas descritas son coherentes con las entidades del dominio?

Si detectas algo ambiguo, añádelo a la sección de "Asunciones tomadas" o vuelve a preguntar si es bloqueante.

---

## Fase 3 — Generación del plan

Solo tras aprobación del borrador de diseño, genera el plan de implementación.

> **REGLA OBLIGATORIA — ruta del fichero del plan:** el plan se guarda **siempre** en `docs/plans/YYYY-MM-DD_HH-MM-<nombre>.md` (con la fecha y hora actuales en formato ISO y el nombre en kebab-case). **Nunca en la raíz del proyecto ni en ninguna otra carpeta.**

### Estructura del plan

El plan se guarda en `docs/plans/YYYY-MM-DD_HH-MM-<nombre>.md` con esta estructura:

```markdown
# Plan: <Nombre>

**Objetivo:** <Una frase>
**Capa:** system|subsystem/<nombre>
**Skills necesarios para la implementación:** k-sistemas, k-vistas[, k-seguridad si hay permisos]

## Ficheros a crear o modificar

| Fichero | Acción | Descripción |
|---------|--------|-------------|
| `subsystem/foo/domains/Bar.xml` | Crear | Entidad Bar |
| ... | | |

## Pasos

### Paso 1 — <Título>
...
```

### Reglas para los pasos

Cada paso debe:
- Tener un título claro que indique qué se hace.
- Incluir el texto completo de lo que hay que implementar: rutas exactas de ficheros, nombres de clases, campos, tipos, relaciones. No poner "similar al paso anterior" ni "TBD".
- Ser lo suficientemente pequeño para implementarse y verificarse de forma independiente (máximo ~30 minutos de trabajo).
- Indicar qué verificar al final del paso (¿compila? ¿qué test ejecutar? ¿qué grep confirma que está bien?).

### Orden de los pasos (siempre este orden)

1. **Ficheros estáticos y recursos** (si los hay) — plantillas PDF en `documentospdf/`, esquemas XSD, imágenes, certificados u otros recursos que el servicio carga del classpath.
2. **Dominios** — XML de entidades (todos los campos, relaciones, enumerados, finders).
3. **Servicios** — interfaz `ModelService` + implementación `DefaultModelService` (constructor, métodos CRUD, validaciones).
4. **Repositorios** (si hay queries propias) — `db/repo/` con finders adicionales.
5. **Controladores** (si hay lógica de botones) — métodos `@CallMethod` delegando en servicios.
6. **Vistas** — grids, formularios, actions, menús.
7. **Seguridad** — `data-init/input/` con permisos y roles.
8. **Datos iniciales** — cualquier otro dato de catálogo precargado.
9. **Verificación final** — compilar el proyecto y confirmar que arranca sin errores.

### Revisión del plan antes de guardarlo

Antes de guardar el plan, comprueba:
- ¿Cada paso tiene toda la información para que un subagente lo implemente sin leer el resto del plan?
- ¿Hay algún paso que hace referencia a algo definido en otro paso sin incluir el contexto necesario?
- ¿Los nombres de clases, métodos y ficheros son coherentes entre todos los pasos?
- ¿Algún paso dice "TBD", "similar a", "según convenga" o cualquier placeholder?
- ¿El paso de verificación final incluye el comando exacto de compilación?
- **¿El plan incluye validaciones en el cliente** (`action-validate` con los campos obligatorios y reglas de negocio) **y en el servidor** (método `validateSave` en el controlador que llama a `service.validateInsert()`)?  Si hay operaciones que crean o modifican datos y el plan no tiene validaciones, es un error — añadirlas.
- **¿Algún paso crea un módulo Guice para registrar un `ModelService`?** Si es así, elimínalo — `ModelServiceFactory` los descubre automáticamente.
- **¿Algún paso crea un listener JPA para implementar lógica de negocio?** Si es así, mover esa lógica al servicio como un método `fireActionRule_*`.
- **¿El fichero del plan se va a guardar en `docs/plans/YYYY-MM-DD_HH-MM-<nombre>.md`?** Si no, corregir la ruta.

Si encuentras algún problema, corrígelo antes de guardarlo.

---

## Fase 4 — Transición a implementación

Al finalizar el plan, indica al usuario:

```
Plan guardado en docs/plans/YYYY-MM-DD_HH-MM-<nombre>.md

Para implementarlo ejecuta:
  /plan-system-implementer con el plan anterior 
```

No lances `plan-system-implementer` tú mismo. El usuario decide cuándo ejecutarlo.
