---
type: design-guidelines
---

- El efecto de la entrada deshabilitada debe aplicarse en la resolución del almacén de claves por DNI (`AlmacenClaveResolver.getByDNI`, que delega en `CertificadoDigitalService.getAlmacenClaveByDni`): una entrada con `enabled = false` se comporta exactamente igual que cuando no existe entrada para ese DNI (mismo error). Los demás métodos de `AlmacenClaveResolver` (director, secretario, dummy) no se tocan.
- El booleano se llama `enabled` en el modelo, con valor por defecto `true`.
