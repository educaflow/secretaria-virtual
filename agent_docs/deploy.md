# Deploy y entorno de desarrollo — secretaría virtual

Cómo compilar, probar, arrancar la app y gestionar la base de datos en el entorno **host**
(`/home/logongas/Documentos/desarrollo/educaflow/secretaria-virtual`).

> **CRITICAL — contenedor Docker `/build`: NO tocar.** Existe **otra** instancia de la app
> corriendo en un contenedor Docker con el repo montado en `/build/secretaria-virtual` (es del
> usuario). **MUST NOT** pararla, matar sus procesos ni interferir con ella. Tu gestión (arrancar
> con `./run.sh`, parar por puerto) se limita **solo** a la instancia host. Antes de matar algo
> por puerto (`fuser -k 8080/tcp`, `lsof`), confirma que el PID dueño del socket pertenece a la
> ruta host (`/home/logongas/...`) y no a `/build/...` — matar por puerto es ciego.

## Compilar y arrancar la app

- Para compilar **y arrancar** la app lanza **siempre** `./run.sh`. Hace `./gradlew clean build`
  (compila Y ejecuta los tests; si fallan, imprime qué tests fallaron y **NO** arranca la app) y
  luego arranca con la config privada
  (`--config ../secretaria-virtual-private/axelor-config.dev.properties`) en el puerto de `ports.env`
  (la primera vez en un worktree, `./run.sh --new`; ver [Puertos y BD por worktree](#puertos-y-bd-por-worktree-portsenv)),
  o en el **8080** si no hay fichero.
- **NO** invoques `gradlew run` a mano ni añadas `--debug-jvm`: ese flag suspende la JVM esperando
  un depurador, así que la app nunca llega a responder; no usarlo para arrancar de forma desatendida.
- Si solo necesitas **compilar sin arrancar**: `./gradlew clean build --info`.
- Compilar solo el código (sin tests): `./gradlew compileJava` (o `compileTestJava` para los tests).
- **MUST** hacer siempre `clean` antes de `build` (`./gradlew clean build`): sin `clean` el build falla.

## Probar los tests

- `./gradlew clean build` compila Y ejecuta los tests JUnit (es lo que hace `./run.sh` antes de arrancar).
- Solo los tests, sin arrancar: `./gradlew test`.
- Un test o clase concretos: `./gradlew test --tests 'com.educaflow.architecture.*'` (patrón por FQN).
- Resultados (fuente de verdad):
  - Informe HTML: `build/reports/tests/test/index.html`.
  - XML por test: `build/test-results/test/*.xml` (cada `<testcase>` con `<failure>`/`<error>` es un fallo).

### Mutation testing (PIT)

- Mide la **calidad** de los tests unitarios con [PIT](https://pitest.org/): introduce cambios pequeños
  en el bytecode (mutantes: invertir un `if`, devolver `null`, quitar una llamada…) y comprueba si algún
  test falla. Un mutante que sobrevive es código cuyo comportamiento ningún test está comprobando.
- **No** forma parte de `build` ni de `./run.sh` (es lento): se lanza a mano con `./gradlew pitest`.
- Informe HTML: `build/reports/pitest/index.html` (XML en `build/reports/pitest/mutations.xml`).
- Solo muta el código escrito a mano (excluye lo generado: `*.db.*`, `States`, numeradores) y solo ejecuta
  los tests unitarios de `base`, `system` y `subsystem`; los de `architecture`, `views` y
  `tiposexpedientes` comprueban estructura/XML y no matan mutantes. La configuración está en el bloque
  `pitest { }` de `build.gradle`.
- Las métricas que importan: **Test strength** (mutantes matados entre los que algún test cubre: lo bueno que
  es el test que hay) y **Mutations with no coverage** (código sin ningún test unitario que lo toque).

### Análisis estático (Error Prone)

- [Error Prone](https://errorprone.info/) detecta errores de programación habituales **dentro de javac**, en cada compilación.
  Va enganchado a `compileJava` y `compileTestJava`, así que `./gradlew build` y `./run.sh` ya lo pasan: no hay ninguna tarea aparte que lanzar.
- Cada aviso lleva el nombre del check entre corchetes (`warning: [JavaTimeDefaultTimeZone] ...`) y un enlace a su documentación.
  Si una compilación falla con un mensaje de esa forma, el fallo viene de Error Prone, no de javac.
- Los checks de severidad **ERROR rompen la compilación**; los **WARNING solo avisan** por consola.
  No genera informe: los avisos salen en la salida de la compilación.
  Como Gradle no recompila lo que está al día, para volver a verlos todos hay que forzar la recompilación (`./gradlew compileJava compileTestJava --rerun-tasks`).
- Que un WARNING no rompa la compilación no lo hace opcional: **un build limpio es un build sin warnings de Error Prone**.
  Cada `warning: [Check]` en un fichero de `src/` **MUST** corregirse en el código, en el mismo cambio que lo produjo, siguiendo la sugerencia del check (suele traer un `Did you mean …`).
  Solo se deja sin corregir un falso positivo justificado, y entonces se silencia como dice el punto siguiente.
- Solo analiza **Java**: el código Kotlin no pasa por él, y el código generado (`build/src-gen`, `build/src-gen-states`) está excluido a propósito.
- Un falso positivo se silencia **en el sitio concreto** con `@SuppressWarnings("NombreDelCheck")`, nunca desactivando el check para todo el proyecto.
  La configuración (plugin `net.ltgt.errorprone`, versión fija de `error_prone_core`, exclusiones) está en `build.gradle`.
- Excepción deliberada: `UnusedVariable` está **desactivado** para todo el proyecto.
  Hay muchos campos declarados y sin usar todavía (`logger`, `repository` inyectado, servicios) que están ahí a propósito como plantilla del idioma con que se obtienen, y el check no permite distinguir campos de locales o parámetros.
  **MUST NOT** borrar esos campos como limpieza.

### Métrica CRAP (`crapCheck`)

- El build tiene un paso más, `crapCheck`, que calcula el CRAP de cada método: `CRAP = CC² × (1 − cobertura)³ + CC`.
  La complejidad ciclomática (CC) y la cobertura salen de JaCoCo (`jacocoTestReport`) con los tests unitarios; el código generado (`*.db.*`, `States`) no se mide.
- Va enganchado a `check`, así que `./gradlew build` y `./run.sh` ya lo pasan: **si algún método supera la constante `crapUmbral` del `build.gradle`, el build falla y `./run.sh` no arranca la app**, aunque compile y pasen todos los tests.
  Se lanza suelto con `./gradlew -q crapCheck`.
- El error lista cada método infractor como `ruta:línea` con su CRAP, CC y cobertura.
  Los informes quedan siempre en `build/reports/crap/crap.csv` (todos los métodos) y `build/reports/crap/crap.md` (resumen).
- Se arregla añadiendo tests unitarios al método o troceándolo para bajar su CC (con cobertura total `CRAP = CC`, así que si la CC ya supera el umbral los tests no bastan).
  El skill `developer-reduce-crap` automatiza ese trabajo.

## Base de datos

- PostgreSQL **12.22**. Conexión por defecto (en `src/main/resources/axelor-config.properties`,
  `db.default.*`): `jdbc:postgresql://localhost:5432/educaflow`, usuario `educaflow`, contraseña `educaflow`.
- El esquema lo gestiona Axelor automáticamente (`db.default.ddl = update`), pero **además** `DataBaseStartup.startup()`
  ejecuta **Flyway** en cada arranque, sobre `classpath:com/educaflow/secretariavirtual/startup/database`. Hoy esa
  carpeta está vacía, así que no hay ninguna migración que aplicar; si necesitas una, ése es su sitio.
- En cada arranque `DataBaseStartup.truncateTables()` vacía con `TRUNCATE ... CASCADE` las tablas `meta_*` y `auth_*` (salvo `auth_user`, `auth_group`, `meta_file`, `meta_sequence` y `meta_filter`) y las cuatro `security_ace_profile_*` que se cargan desde XML, para que los metadatos se regeneren limpios.
  Los usuarios **no** se borran.
  El `CASCADE` arrastra a toda tabla con una FK hacia una truncada, excluida o no: que ninguna entidad excluida apunte a una tabla truncada lo comprueba `TablasExcluidasDelTruncadoTest`.

### Puertos y BD por worktree (`ports.env`)

Cada worktree de Git (carpetas hermanas, p. ej. `secretaria-virtual` y `secretaria-virtual-ws-notificacion`) tiene **su propio par de puertos fijo y su propio contenedor de BD**, para que varios puedan arrancar a la vez sin chocar.
Los puertos viven en `ports.env` en la raíz del worktree (`APP_PORT=…` y `DB_PORT=…`, formato `source` de bash).
Está en `.gitignore`: Git no lo copia al crear un worktree, así que cada uno acaba con el suyo.

- **La primera vez** en un worktree: `./run.sh --new`.
  Busca el primer puerto libre a partir de 8080 (app) y 5432 (BD), descartando los que ya tenga el `ports.env` de otro worktree aunque esté parado, escribe `ports.env` y arranca.
- **Después**: `./run.sh` a secas. Usa los puertos del fichero, siempre; no vuelve a buscar.
- **Al arrancar** imprime el modo, los puertos y el contenedor que usa.
  La app queda en `http://localhost:<APP_PORT>`.
- **Cambiar de puertos**: borra `ports.env` y vuelve a hacer `./run.sh --new`.
- **Sin `ports.env`** (modo por defecto): `run.sh` se comporta como siempre, 8080/5432, sin tocar Docker ni sobrescribir propiedades.
  Es el modo de producción (`../secretaria-virtual-devops`), donde `ports.env` nunca debe existir.
  Si el repositorio tiene **más de un worktree**, el modo por defecto falla antes de compilar y pide `./run.sh --new`: evita arrancar sin querer contra la BD de otro worktree con `ddl = update`.
  Si el mensaje lista un worktree que ya no existe (se borró con `rm -rf`), `git worktree prune`.
- **Puerto ocupado**: `run.sh` falla diciendo qué lo ocupa (contenedor, o PID y comando) y nunca mata procesos.
  El puerto de la app falla siempre que esté ocupado, aunque sea una instancia anterior del mismo worktree: párala tú.

En modo worktree `run.sh` sobrescribe al arrancar las propiedades que apuntan a recursos compartidos, sin tocar los ficheros de configuración:
- `db.default.url` → `localhost:<DB_PORT>`.
- `data.upload.dir`, `data.upload.temp-dir` y `logging.path` → `<worktree>/.axelor-data/{attachments,temp,logs}` (en `.gitignore`), en vez de `/opt/secretariavirtual/data`, que es común a todos los worktrees: con la carpeta compartida cada BD referenciaría adjuntos que otro worktree puede borrar.
  Los adjuntos viven y mueren con el worktree, igual que su BD.
Las tres primeras van como variables de entorno `AXELOR_CONFIG_*`, que AOP lee por encima de `axelor-config.properties` y del `--config`; `data.upload.temp-dir` lleva guion, que esa vía no puede expresar (AOP convierte `_` en `.`), así que va como propiedad de sistema `-Daxelor.config.data.upload.temp-dir` en `JAVA_TOOL_OPTIONS` solo del `gradlew run`.
`playwright.config.ts` (`baseURL`) y el MCP de PostgreSQL (`.mcp.json`) leen `ports.env` si existe y usan 8080/5432 si no.

### La BD del worktree (Docker)

`run.sh` la gestiona solo en modo worktree: contenedor `educaflow-db-<DB_PORT>` (`postgres:12.22`, usuario/contraseña/BD `educaflow`, puerto `<DB_PORT>:5432`) con la etiqueta `educaflow.worktree=<ruta absoluta del worktree>`.
No se crea con `--rm`: un contenedor parado (p. ej. tras reiniciar la máquina) se rearranca con `docker start` conservando los datos.

- **Por defecto no se reinicia**: si está corriendo se usa tal cual y si está parado se arranca; en ambos casos los datos se conservan.
- **`./run.sh --reset-db`**: borra el contenedor y lo crea de nuevo con la BD vacía (Axelor recrea el esquema con `ddl = update` y recarga los datos demo).
  **No lo uses por defecto**: borra los datos que el agente haya creado para probar.
  Solo para empezar de cero o recargar los datos demo.
- **Sin `ports.env`** falla: solo aplica al modo worktree.
- **Si el puerto de la BD lo ocupa un contenedor con la etiqueta de este worktree**, se usa; si lo ocupa cualquier otra cosa, falla.

**Contenedores huérfanos.** Al borrar un worktree (`git worktree remove`, `/git-flow finalizar` o un `rm -rf`) su contenedor se queda.
No hay hook de Git para eso, así que la limpieza es automática la siguiente vez que **cualquier** worktree arranca en modo worktree o con `--new`: `run.sh` lista los contenedores con la etiqueta `educaflow.worktree` y borra (`docker rm -f`) los que apunten a una ruta que ya no existe como directorio, imprimiendo `Borrado contenedor huérfano …`.
Solo mira contenedores con esa etiqueta y solo borra por ausencia del directorio.
Los datos de un worktree borrado se pierden; si la misma rama se vuelve a abrir en la misma ruta antes de que ningún otro worktree arranque, el contenedor sigue ahí y se reutiliza.

### Acceder con el CLI de PostgreSQL (`psql`)

Con el `DB_PORT` de `ports.env` (5432 en modo por defecto):

- Desde el host (pide contraseña `educaflow`, o pásala con `PGPASSWORD`):

  ```bash
  [ -f ports.env ] && . ./ports.env; PGPASSWORD=educaflow psql -h localhost -p "${DB_PORT:-5432}" -U educaflow -d educaflow
  ```

- Desde dentro del contenedor:

  ```bash
  . ./ports.env; docker exec -it "educaflow-db-$DB_PORT" psql -U educaflow -d educaflow
  ```

- Para consultas puntuales de **solo lectura** desde el asistente, está el MCP de PostgreSQL
  (`mcp__postgres__query`); ver [`mcp.md`](mcp.md).

## Configuración

Las propiedades van en `src/main/resources/axelor-config.properties` (`db.default.*`, `mail.*`,
`quartz.*`, etc.). Las propiedades **privadas** (credenciales reales, secretos) van en el fichero
externo `../secretaria-virtual-private/axelor-config.dev.properties`, que se pasa con `--config` al
arrancar y **sobrescribe** los valores del primero.

### Propiedades que declara cada entorno

La configuración privada tiene un fichero por entorno: `axelor-config.dev.properties` (desarrollo, el que usa `./run.sh`), `axelor-config.pre.properties` (preproducción) y `axelor-config.pro.properties` (producción).
Cada uno declara **explícitamente** estas tres propiedades, también producción aunque coincida con el valor de `axelor-config.properties` (las dos primeras son `entornoCriptografico.almacenCertificadosConfiables.path` y `.pathListaCRLs`):

| Fichero | `…path` | `…pathListaCRLs` | `data.import.demo-data` |
|---|---|---|---|
| `axelor-config.dev.properties` | `firma/demo/almacen/truststore-demo.jks` | `firma/demo/almacen/crls-demo.xml` | `true` |
| `axelor-config.pre.properties` | `firma/demo/almacen/truststore-demo.jks` | `firma/demo/almacen/crls-demo.xml` | `true` |
| `axelor-config.pro.properties` | `firma/AlmacenCertificadosConfiables/truststore.jks` | `firma/AlmacenCertificadosConfiables/crl/crls.xml` | `false` |

- `axelor-config.properties` deja los valores seguros (los de producción), por si un entorno olvida declararlas.
  Si falta `data.import.demo-data`, AOP la toma como `false` (no carga la demo); aun así se declara explícitamente para no depender del valor por defecto de la versión de AOP.
- El almacén de demo está todo en `firma/demo/almacen/` y no toca nada de `firma/AlmacenCertificadosConfiables/` (`src/main/resources/firma/demo/README.txt`).
  `truststore-demo.jks` es el almacén oficial más la CA FALSA de demo, con la que están emitidos los certificados de los usuarios de demo, y `crls-demo.xml` lista las CRL oficiales (con ruta relativa a `firma/AlmacenCertificadosConfiables/crl/`, sin copiarlas) más la CRL de esa CA: sin ella la validación rechaza los certificados de demo, que no tienen OCSP ni CRLDP.
  Así en desarrollo y preproducción se puede probar también con certificados reales.
  `firma/demo/crear_truststore_demo.sh` hay que relanzarlo a mano cada vez que se regenere `truststore.jks` o `crls.xml`.
  Van siempre juntos y solo se admiten en un entorno con `data.import.demo-data = true`, y nunca en producción: lo comprueba el test `ConfiguracionEntornosTest`, que lee estos tres ficheros (si no está la carpeta `../secretaria-virtual-private`, esa parte se omite) y que `axelor-config.properties` deja los valores seguros.
- Si falla cualquier cosa al inicializar el entorno criptográfico (`CriptografiaStartup`), la aplicación **no arranca**.
- Los datos de demo (`data-demo`) solo se cargan al **instalar** el módulo: en una BD ya instalada, un cambio en `data-demo` no entra al rearrancar y hay que resetearla (ver «Resetear desde cero»).

### Cifrado de campos (`encryption.password`)

- Los campos del modelo con `encrypted="true"` (p. ej. `CertificadoDigital.password` y `DispositivoCriptografico.pin`) solo se cifran en la BD si está definida `encryption.password`; sin ella se guardan en claro.
- `encryption.password` **MUST** definirse en la config privada, nunca en `axelor-config.properties`, y es obligatoria en producción.
- Al definirla por primera vez, las filas ya guardadas siguen en claro hasta cifrarlas con la migración `database encrypt` del CLI `axelor` de AOP, que se genera con `./gradlew installDist`:

  ```bash
  ./gradlew installDist
  build/install/secretaria-virtual/bin/axelor -c <config privada> database encrypt
  ```

- Para cambiar la clave, la anterior se pone en `encryption.old-password` (y `encryption.old-algorithm` si cambia el algoritmo), se lanza la misma migración y después se quita `encryption.old-password` de la config.
- Conviene lanzarla con la app parada, porque reescribe las columnas cifradas de todas las entidades.
