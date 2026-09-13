---
type: implementation-task
template: system
---

# Tarea 14 a implementar

## Skills a usar
Para hacer esta tarea vas a usar estos skills
- k-datainit

Actualiza los datos iniciales del propio subsistema, que es el dueño de las tablas. Los dos ficheros van juntos porque son un único componente lógico: el dato nuevo y el binding que lo sabe leer no tienen sentido por separado.

- `src/main/java/com/educaflow/subsystem/sistemaeducativo/data-init/input/Nivel.xml` (`Modificar`)
- `src/main/java/com/educaflow/subsystem/sistemaeducativo/data-init/input-config.xml` (`Modificar`)

**Acción `Modificar`: los dos ficheros YA EXISTEN.** A diferencia de los dominios y las vistas, estos **NO** están materializados en `design/`: el Paso 12 describe el delta y hay que aplicarlo sobre los ficheros reales, **conservando** todo lo demás (los demás `<input>`, los demás bindings y el resto de datos). **MUST NOT** reescribir los ficheros enteros.

**MUST NOT** crearse ninguna carpeta `data-init` nueva: el subsistema ya tiene la suya.

## Filas de la tabla «Ficheros a crear o modificar» del diseño

| Fichero | Acción | Skill | Descripción |
|---------|--------|-------|-------------|
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/data-init/input/Nivel.xml` | Modificar | k-datainit | Los tres niveles pasan a pertenecer al grado `D` («Ciclo formativo»). |
| `src/main/java/com/educaflow/subsystem/sistemaeducativo/data-init/input-config.xml` | Modificar | k-datainit | Binding del atributo `@grado` del nivel. |

## Paso 12 del diseño (verbatim)

### Paso 12 — Datos iniciales

Carpeta `data-init` del propio subsistema, que es el dueño de las tablas (`k-datainit`).

- `data-init/input/Nivel.xml`: cada uno de los tres niveles precargados (`GB`, `GM`, `GS`) pasa a llevar `grado="D"`, el código del grado «Ciclo formativo». El grado `E` («Curso de especialización») se queda sin ningún nivel, que es lo que hace que sus ciclos no lleven nivel.
- `data-init/input-config.xml`: en el `<input file="Nivel.xml">`, un binding más para el atributo `@grado` → propiedad `grado`, resuelto por `search="self.code = :grado"` con `create="false"` y `update="false"`, igual que ya hace el input de `Ciclo` con sus tres referencias. El `<input>` de grados ya va **antes** que el de niveles, así que el orden de dependencias se respeta sin tocarlo.
- No cambian ni los ciclos (todos de grado `D` y con su nivel), ni sus cursos, ni los módulos de cada curso, ni el resto de catálogos.
- Como los `<input>` llevan `update="true"`, una base de datos ya poblada también queda con los tres niveles colgando del grado `D` al arrancar. **Ojo: eso arregla los DATOS, no el ESQUEMA.** Si la columna `NOT NULL` de `Nivel.grado` no llegó a crearse (ver el aviso del Paso 2: `ddl = update` no puede añadir una columna `NOT NULL` a una tabla ya poblada), este data-init tampoco se puede aplicar y el fallo se ve aquí, no en el Paso 2. La precondición de este paso es, por tanto, la del Paso 2: haber **reseteado la base de datos** con el `docker stop` + `docker run` que allí se detalla.

**Verificación del paso:** tras `./run.sh`, `Sistema educativo → Niveles` muestra los tres niveles con el grado «Ciclo formativo». Si alguno sale sin grado (o la pantalla revienta), revisar primero el log de arranque en busca del error de `ALTER TABLE` de Hibernate: es el síntoma del esquema a medias.


## Paso 11 del diseño — seguridad, sin delta (verbatim; es la premisa de este paso)

### Paso 11 — Seguridad

Sin delta: esta iniciativa **no cambia ningún permiso**. Pero conviene dejar el estado de partida escrito con exactitud, porque es la premisa sobre la que descansa `decisiones.md` **D8**.

Las ocho `permission` de los catálogos del sistema educativo (`Ciclo.all`, `Curso.all`, `CursoModulo.all`, `FamiliaProfesional.all`, `Grado.all`, `LeyEducativa.all`, `Modulo.all`, `Nivel.all`) las **define** el `data-init` del propio subsistema, `src/main/java/com/educaflow/subsystem/sistemaeducativo/data-init/input/auth-sistemaeducativo.xml`, con `create/read/write/remove/export` y **sin `condition`** (y `src/main/resources/data-init/input/auth.xml` repite esas mismas definiciones en `:124-147`). Pero **definir no es asignar**: la **asignación a grupos** vive solo en `auth.xml`, y no es solo para `admins` (`:208-215`) — también las tiene el grupo **`users`** (`:261-268`), igual de sin `condition`. Es decir, el permiso de **datos** sobre estas entidades —y por tanto el endpoint REST automático `POST /ws/rest/<FQN>`— alcanza a cualquier usuario del grupo `users`; lo que está restringido a `admins` son las **pantallas**: los `<menuitem>` y las vistas de mantenimiento llevan `groups="admins"`.

Los catálogos del sistema educativo son comunes a toda la aplicación (no se reparten por centro), así que **no** se añade ningún filtro por centro ni ningún `<domain>` con `:__user__` en los `action-view`.

Regla de acceso en lenguaje natural: *solo el Administrador llega a las pantallas de grados, niveles y ciclos, y desde ellas ve, crea, edita y borra los de toda la aplicación; el resto de usuarios no tiene esas pantallas, aunque sus permisos de datos sobre esas tablas sí siguen concedidos por `auth.xml` al grupo `users`*. Cerrar ese desajuste **no** es objeto de esta iniciativa (ver `decisiones.md` D8, última línea: es el mismo asunto global del apartado PENDIENTE del `CLAUDE.md`).

