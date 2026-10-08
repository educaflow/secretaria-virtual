DECISION: B
=== JUSTIFICACION ===
Nivel (2): el nivel 1 no decide, porque ninguna alternativa cambia la funcionalidad implementada. La propia spec (specification.md L751, «Recursos y datos iniciales») hace depender la firma de demo de «otra iniciativa», y t-028-…desc.md L22/L52 declara que T-028/029/075/076 «no son automatizables hasta que se fusione» esa iniciativa.
Por convenciones decide B. El material que falta es de e8cb3ab, que solo está en develop (git show --stat e8cb3ab), y el desajuste lo crea la config privada del entorno (CLAUDE.md §Configuración), no el código de la iniciativa.
A tiene dos variantes y ninguna es aceptable:
  si se versiona, importa un entregable de otra iniciativa sin la protección que lo acompaña (README de firma/demo: el almacén de demo solo se admite con data.import.demo-data, comprobado por ConfiguracionEntornosTest, que no existe en esta rama);
  si no se versiona, create-tests persistiría regresiones que fallan en un checkout limpio, y eso acaba en FIN-STOP-REGRESION, que es un STOP real (.claude/skills/sdd/SKILL.md L84, L402).
B es lo que el motor prevé para un FAIL que no se puede corregir: el pipeline sigue, y create-tests solo persiste los [x] y los [-]. No es destructiva, queda dentro del alcance y se revierte reejecutando esos 4 tests cuando se integre develop.
