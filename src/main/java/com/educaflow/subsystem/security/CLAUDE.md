# `subsystem/security` — qué perfiles tiene un usuario

Este subsistema responde a una sola pregunta: **qué perfiles (`Profile`) tiene un usuario sobre un trámite o sobre un expediente**.
La máquina de estados la usa para autorizar: cada estado declara el perfil de quien lo atiende, y solo puede disparar sus eventos quien tenga ese perfil.

La respuesta sale de las tablas `AceProfile*`.
Cada fila dice: **a quién** se da el perfil, **qué perfil** es y **sobre qué**.

## Las tablas

Están partidas por **sobre qué** se da el perfil, porque cada una se rellena en un sitio distinto.

| Tabla | Sobre qué | A quién (exactamente uno) | Quién la rellena |
|---|---|---|---|
| `AceProfileGlobal` | todos los trámites de todos los centros | `tipoUsuario` o `cargo` | data-init de este subsistema |
| `AceProfileTipoTramite` | los trámites de un `tipoTramite`, en todos los centros | `tipoUsuario` o `cargo` | data-init de este subsistema |
| `AceProfileTramite` | un `tramite`, en todos los centros | `tipoUsuario` o `cargo` | el `<aces>` del `TramiteInstance.xml` (`/k-tramite`) |
| `AceProfileTipoExpediente` | un `tipoExpediente`, en todos los centros | `tipoUsuario` o `cargo` | el `<aces>` del `TipoExpedienteInstance.xml` (`/k-tipo-expediente`) |
| `AceProfileCentro` | un `tramite` en un `centro` | `tipoUsuario`, `cargo` o `usuario` | una pantalla del centro, en tiempo de ejecución: «Mi centro → Perfiles de trámites» (el supervisor, en los centros que supervisa) y «Administración → Perfiles de trámites por centro» (el administrador, en cualquier centro), las dos a través de `AceProfileCentroService`, que valida que haya un único destinatario |
| `AceProfileExpediente` | un `expediente` | `usuario` | la tramitación, en tiempo de ejecución |

- Las cuatro primeras se definen al compilar, sin saber qué centros ni qué usuarios hay.
  Por eso no tienen `centro` ni `usuario`: valen en cualquier centro, y solo pueden dar el perfil por tipo de usuario o por cargo.
- `AceProfileCentro` y `AceProfileExpediente` dan el perfil a un `User`, no a un `CentroUsuario`.
  El centro ya lo fija la fila (`centro`, o el centro del expediente), así que guardar el `CentroUsuario` duplicaría el centro y permitiría que las dos copias no coincidieran.
- Que en cada fila haya exactamente un «a quién» no lo puede expresar el XML de dominio.
  - En los `<aces>` de `TramiteInstance.xml` y `TipoExpedienteInstance.xml` lo valida el build (el generador de `EducaFlowBuildTools` aborta).
  - En el data-init de este subsistema **no** lo valida nadie: un `<ace>` con `tipoUsuario` y `cargo` a la vez entra por los dos `<input>` y crea dos filas, así que **MUST** escribirse con uno solo.
  - En las tablas de tiempo de ejecución **MUST** validarlo el servicio que las escriba.

## Cómo se calculan los perfiles

Lo hace `PerfilesUsuarioService`, juntando (unión) los perfiles de todas las tablas que alcanzan al usuario.

- **Sobre un trámite** (para poder crear un expediente): Global + TipoTramite del trámite + Tramite + Centro del centro elegido + TipoExpediente del **tipo activo** del trámite (`defaultTipoExpediente`), que es el que se va a crear.
- **Sobre un expediente**: Global + TipoTramite + Tramite + Centro del centro del expediente + TipoExpediente del **tipo del propio expediente** + Expediente.
  - **MUST** usarse el tipo del expediente y no el activo del trámite: al activar una versión nueva, sus perfiles no se aplican a los expedientes de versiones anteriores.
  - El `CREADOR` de Global, TipoTramite, Tramite, Centro y TipoExpediente **MUST NOT** contar sobre un expediente: solo habilita a crear. Si contara, cualquier alumno sería `CREADOR` de los expedientes de los demás alumnos.
  - Sobre un expediente es `CREADOR` quien lo registró (`usuarioRegistrador`), aunque no tenga ninguna fila, y quien tenga una fila `CREADOR` en `AceProfileExpediente`.
- Una fila por `tipoUsuario` o `cargo` alcanza al usuario si tiene ese tipo de usuario o ese cargo **en el centro** del que se pregunta (sus `CentroUsuarioTipoUsuario` / `CentroUsuarioCargo`).
- Quien no pertenece al centro (no tiene `CentroUsuario` en él) no recibe ningún perfil, **ni siquiera** `CREADOR` sobre un expediente que registró: solo se crean expedientes en un centro al que se pertenece.

Las consultas van en un repositorio propio por tabla (`db/repo/`), nunca en el servicio.

## Borrado al arrancar

El data-init de Axelor solo hace upsert: quitar una fila del XML no la borra de la base de datos.
Por eso las cuatro tablas que salen de XML se vacían en cada arranque (`tablasIncluidas` de `DataBaseStartup`) y se vuelven a cargar enteras.

- **MUST NOT** añadir ahí `AceProfileCentro` ni `AceProfileExpediente`: se rellenan en tiempo de ejecución y se perderían en cada arranque.
- Una tabla `AceProfile*` nueva que se cargue desde XML **MUST** añadirse a esa lista.

## Datos iniciales

- `AceProfileGlobal` y `AceProfileTipoTramite` se cargan desde `data-init/input/` de este subsistema, con un fichero por tabla y el mismo formato para las dos: `<aces>` con un `<ace perfil="…" tipoUsuario="…"/>` o `<ace perfil="…" cargo="…"/>` por fila.
- El `input-config.xml` tiene una `priority` menor que la de `common` (`TipoUsuario`, `Cargo`) y que la de `expedientes` (`TipoTramite`), porque los referencia.
- `AceProfileTramite` y `AceProfileTipoExpediente` no tienen data-init en `src`: lo genera el build a partir del `<aces>` de cada fichero maestro.

## Permisos de Axelor

Las condiciones de los permisos de lectura de `Tramite`, `TipoExpediente` y `Expediente` (`subsystem/expedientes/data-init/input/auth-expedientes.xml`) consultan las mismas tablas `AceProfile*` con las mismas reglas, para que lo que un usuario **ve** coincida con lo que **puede hacer**.
- Hay un permiso por tabla, porque la `condition` de un permiso está limitada a 1024 caracteres.
- Si cambia cómo se calculan los perfiles, **MUST** cambiar también allí.

Los permisos propios de este subsistema se enlazan al grupo `users` desde su propio `data-init/input/auth-security.xml`, no desde el `auth.xml` global.
- Lo hace el `<bind node="group">` del `<input>` de `auth-security.xml` en su `input-config.xml`.
- Ese enlace suma permisos al grupo, no los reemplaza, así que no choca con el `auth.xml` global.

Excepción `Tramite.supervisor`: el supervisor **lee** todos los trámites aunque no tenga ningún perfil sobre ellos.
- Existe solo para que pueda elegirlos al asignarlos en «Mi centro → Perfiles de trámites».
- Por eso, en `Tramite`, lo que el supervisor **ve** no coincide con lo que **puede hacer**.
- No cambia qué trámites le ofrece «Nuevo trámite» (la ventanilla filtra por perfiles), qué expedientes ve ni cómo se calculan los perfiles.
