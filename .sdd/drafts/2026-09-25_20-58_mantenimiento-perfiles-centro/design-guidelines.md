---
type: design-guidelines
---

- La iniciativa solo puede tocar el paquete `com.educaflow.subsystem.security`. Única excepción autorizada: el fichero común de menús `secretariavirtual/menus/menus.xml`, solo para añadir las dos entradas de menú («Mi centro → Perfiles de trámites» y «Administración → Perfiles de trámites por centro»).
- En particular, **no** se tocan las condiciones de los permisos de lectura del paquete `subsystem/expedientes` (`auth-expedientes.xml`) ni el cálculo de perfiles (`PerfilesUsuarioService` / `AceProfileCentroRepository.findPerfiles`): por eso tipo de usuario, cargo y usuario son mutuamente excluyentes en cada fila, y la semántica del cálculo no cambia.
- El «centro del supervisor» son **todos** los centros en los que el usuario tiene el tipo de usuario `SUPERVISOR`, no `User.centroActivo`.
