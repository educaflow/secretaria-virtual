---
type: design-guidelines
---

- La situación de firma del firmante se obtiene **reutilizando lo que ya existe** en el subsistema de criptografía: el enumerado `TipoAlmacenClave` (`com.educaflow.subsystem.criptografia.service.TipoAlmacenClave`) y el método `getTipoAlmacenClaveByDni(String dni)` de `CertificadoDigitalService`, que ya devuelve `null` cuando la persona no tiene certificado habilitado. No hay que inventar otra forma de averiguarlo.
- El almacén de claves con el que firmar en el servidor se obtiene con `getAlmacenClaveByDni(String dni)` del mismo servicio.
- **Un panel de vista distinto por cada valor del enumerado**, más uno para `null` (sin certificado) y otro para el firmante sin DNI: seis paneles excluyentes, aunque dos de ellos digan lo mismo. Es una decisión deliberada del usuario por simplicidad, en lugar de un panel único con etiquetas variables.
- El panel de **`DISPOSITIVO_SIN_PIN` se construye aunque hoy sea inalcanzable** (el campo `pin` de `DispositivoCriptografico` es `required="true"`, así que `getTipoAlmacenClaveByDni` nunca devuelve ese valor). Se hace a propósito, preparado para cuando el PIN deje de ser obligatorio. **No** hay que hacer opcional el PIN en esta iniciativa.
- El campo de la clave de firma es un campo **transitorio del formulario**: no se persiste nunca. Conviene revisar `k-secure-coding` al decidir cómo viaja y cómo se descarta, y cuidar que no acabe en ningún log ni en la respuesta al cliente.
- La comprobación de la situación de firma **la hace el servidor** en el momento de firmar; lo que la pantalla tuviera pintado no es de fiar. La `AllowProperties` de la acción nueva es la defensa contra que el cliente dicte otros campos.
- Las tareas de firma precargadas y el PDF de ejemplo son **datos de demo**, así que su sitio natural es `src/main/resources/data-demo/` (junto a `usuarios-demo.xml`), no la `data-init` del subsistema.
- El certificado de prueba y su contraseña (`firma/mi_certificado.p12` / `nadanada`) y el alta por «Ruta classpath» ya están ejercitados por los tests E2E de `src/test/e2e/subsystem/criptografia/`; conviene mirarlos como referencia al construir los tests de esta iniciativa, incluido su patrón de borrar la entrada del DNI `85432016B` al empezar para poder reejecutar.
