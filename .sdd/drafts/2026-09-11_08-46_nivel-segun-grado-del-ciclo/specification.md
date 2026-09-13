---
type: specification
template: system
---

# Objetivo

Que el nivel de un ciclo se exija exactamente cuando el grado del ciclo tenga niveles, y quede vacío cuando no los tenga, de forma que un curso de especialización no pueda registrarse con nivel y un ciclo formativo no pueda registrarse sin él.
Para conseguirlo, el catálogo de niveles pasa a colgar del catálogo de grados: cada nivel dice a qué grado pertenece, y cada grado sabe si admite nivel porque tiene alguno.
Es un subsistema y no depende funcionalmente de ningún otro.

**Modifica:** subsystem/sistemaeducativo

# Actores

- **Administrador**: mantiene los catálogos del sistema educativo (grados, niveles, familias profesionales, ciclos, cursos y módulos) para todos los centros. Es el único actor que interviene en esta iniciativa.

# Historias de usuario

## HU-001 — Como Administrador quiero que el nivel solo se pida en los ciclos cuyo grado tiene niveles para que ningún ciclo quede con un nivel que no le corresponde

- ESC-001 — Alta de un ciclo formativo con su nivel:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú Sistema educativo → Ciclos y pulsa «Añadir un nuevo ciclo».
  3. Rellena el código con «TSPRL» y el nombre con «Prevención de Riesgos Profesionales», y elige la familia profesional «Seguridad y Medio Ambiente».
  4. Elige el grado «Ciclo formativo».
  5. El sistema muestra el campo Nivel y lo señala como obligatorio.
  6. Elige el nivel «Ciclos Formativos de Grado Superior».
  7. Pulsa «Guardar».
  8. El sistema guarda el ciclo y vuelve al listado, donde «Prevención de Riesgos Profesionales» aparece con grado «Ciclo formativo» y nivel «Ciclos Formativos de Grado Superior».
- ESC-002 — Alta de un curso de especialización sin nivel:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú Sistema educativo → Ciclos y pulsa «Añadir un nuevo ciclo».
  3. Rellena el código con «CEIABD» y el nombre con «Inteligencia Artificial y Big Data (especialización)», y elige la familia profesional «Informática y Comunicaciones».
  4. Elige el grado «Curso de especialización».
  5. El sistema no muestra el campo Nivel.
  6. Pulsa «Guardar».
  7. El sistema guarda el ciclo y vuelve al listado, donde «Inteligencia Artificial y Big Data (especialización)» aparece con grado «Curso de especialización» y la columna Nivel vacía.
- ESC-003 — Un ciclo formativo sin nivel no se puede guardar:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú Sistema educativo → Ciclos y pulsa «Añadir un nuevo ciclo».
  3. Rellena el código con «TSAF» y el nombre con «Automatización y Robótica Industrial», y elige la familia profesional «Electricidad y Electrónica».
  4. Elige el grado «Ciclo formativo» y deja el nivel vacío.
  5. Pulsa «Guardar».
  6. El sistema no guarda el ciclo y muestra el error «El nivel es obligatorio para el grado indicado».
  7. Abre el menú Sistema educativo → Ciclos.
  8. El listado no muestra ninguna fila «Automatización y Robótica Industrial».
- ESC-004 — Al cambiar el grado a uno sin niveles, el nivel se vacía:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú Sistema educativo → Ciclos y pulsa «Añadir un nuevo ciclo».
  3. Rellena el código con «TSMER» y el nombre con «Mecatrónica Industrial», y elige la familia profesional «Instalación y Mantenimiento».
  4. Elige el grado «Ciclo formativo» y el nivel «Ciclos Formativos de Grado Superior».
  5. Pulsa «Guardar».
  6. El sistema guarda el ciclo y vuelve al listado, donde «Mecatrónica Industrial» aparece con grado «Ciclo formativo» y nivel «Ciclos Formativos de Grado Superior».
  7. Pulsa la fila «Mecatrónica Industrial».
  8. El sistema abre el formulario del ciclo y muestra el grado «Ciclo formativo» y el campo Nivel con el valor «Ciclos Formativos de Grado Superior».
  9. Cambia el grado a «Curso de especialización».
  10. El sistema vacía el nivel y deja de mostrar el campo Nivel.
  11. Pulsa «Guardar».
  12. El sistema guarda el ciclo y vuelve al listado, donde la fila «Mecatrónica Industrial» muestra el grado «Curso de especialización» y la columna Nivel vacía.
  13. Pulsa la fila «Mecatrónica Industrial».
  14. El sistema abre el formulario del ciclo con el grado «Curso de especialización» y no muestra el campo Nivel.
- ESC-005 — El nivel solo se elige entre los del grado del ciclo:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú Sistema educativo → Grados y pulsa «Nuevo Grado».
  3. Rellena el código con «G» y el nombre con «Certificado de profesionalidad».
  4. Pulsa «Guardar».
  5. Abre el menú Sistema educativo → Niveles y pulsa «Añadir un nuevo nivel».
  6. Rellena el código con «CPR1» y el nombre con «Certificado de profesionalidad de nivel 1», y elige el grado «Certificado de profesionalidad».
  7. Pulsa «Guardar».
  8. Abre el menú Sistema educativo → Ciclos y pulsa «Añadir un nuevo ciclo».
  9. Rellena el código con «TSDAW» y el nombre con «Desarrollo Web Avanzado», y elige la familia profesional «Informática y Comunicaciones».
  10. Elige el grado «Ciclo formativo».
  11. Abre el selector del campo Nivel.
  12. El sistema ofrece exactamente «Ciclos formativos de grado Básico», «Ciclos Formativos de Grado Medio» y «Ciclos Formativos de Grado Superior».
  13. El selector no ofrece «Certificado de profesionalidad de nivel 1», que pertenece a otro grado.
- ESC-012 — El nivel de un ciclo debe pertenecer al grado del ciclo:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú Sistema educativo → Grados y pulsa «Nuevo Grado».
  3. Rellena el código con «H» y el nombre con «Certificado profesional avanzado».
  4. Pulsa «Guardar».
  5. El sistema guarda el grado y vuelve al listado, donde aparece la fila «Certificado profesional avanzado».
  6. Pulsa «Nuevo Grado», rellena el código con «I» y el nombre con «Certificado profesional experto», y pulsa «Guardar».
  7. Abre el menú Sistema educativo → Niveles y pulsa «Añadir un nuevo nivel».
  8. Rellena el código con «CPA1» y el nombre con «Certificado profesional avanzado de nivel 1», elige el grado «Certificado profesional avanzado» y pulsa «Guardar».
  9. Pulsa «Añadir un nuevo nivel», rellena el código con «CPA2» y el nombre con «Certificado profesional avanzado de nivel 2», elige el grado «Certificado profesional avanzado» y pulsa «Guardar».
  10. Abre el menú Sistema educativo → Ciclos y pulsa «Añadir un nuevo ciclo».
  11. Rellena el código con «CPAINF» y el nombre con «Certificado profesional avanzado de informática», y elige la familia profesional «Informática y Comunicaciones».
  12. Elige el grado «Certificado profesional avanzado».
  13. El sistema muestra el campo Nivel y lo señala como obligatorio.
  14. Elige el nivel «Certificado profesional avanzado de nivel 1» y pulsa «Guardar».
  15. El sistema guarda el ciclo y vuelve al listado, donde «Certificado profesional avanzado de informática» aparece con grado «Certificado profesional avanzado» y nivel «Certificado profesional avanzado de nivel 1».
  16. Abre el menú Sistema educativo → Niveles y pulsa la fila «Certificado profesional avanzado de nivel 1».
  17. Cambia el grado a «Certificado profesional experto» y pulsa «Guardar».
  18. El sistema guarda el nivel y vuelve al listado, donde «Certificado profesional avanzado de nivel 1» aparece con grado «Certificado profesional experto» y «Certificado profesional avanzado de nivel 2» conserva el grado «Certificado profesional avanzado».
  19. Abre el menú Sistema educativo → Ciclos y pulsa la fila «Certificado profesional avanzado de informática».
  20. El sistema abre el formulario del ciclo, muestra el grado «Certificado profesional avanzado», sigue mostrando el campo Nivel —porque ese grado conserva el nivel «Certificado profesional avanzado de nivel 2»— y en ese campo muestra el valor guardado «Certificado profesional avanzado de nivel 1».
  21. Cambia el nombre a «Certificado profesional avanzado de informática (LOFP)».
  22. Pulsa «Guardar».
  23. El sistema no guarda el ciclo y muestra el error «El nivel indicado no pertenece al grado del ciclo».
  24. Abre el menú Sistema educativo → Ciclos.
  25. El listado sigue mostrando la fila con el nombre «Certificado profesional avanzado de informática», sin el cambio que no se pudo guardar.
- ESC-013 — Editar un ciclo sin tocar el grado ni el nivel:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú Sistema educativo → Ciclos y pulsa la fila «Administración de Sistemas Informáticos en Red».
  3. El sistema muestra el código «ASIR», la familia profesional «Informática y Comunicaciones», el grado «Ciclo formativo» y el nivel «Ciclos Formativos de Grado Superior».
  4. Cambia el nombre a «Administración de Sistemas Informáticos en Red (LOFP)».
  5. Pulsa «Guardar».
  6. El sistema guarda el ciclo y vuelve al listado, donde la fila aparece con el nombre «Administración de Sistemas Informáticos en Red (LOFP)» y conserva el grado «Ciclo formativo» y el nivel «Ciclos Formativos de Grado Superior».

## HU-002 — Como Administrador quiero que cada nivel diga a qué grado pertenece para que sea el propio catálogo el que decida qué ciclos llevan nivel

- ESC-006 — Alta de un nivel indicando su grado:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú Sistema educativo → Niveles y pulsa «Añadir un nuevo nivel».
  3. Rellena el código con «GMD» y el nombre con «Ciclos Formativos de Grado Medio Dual», y elige el grado «Ciclo formativo».
  4. Pulsa «Guardar».
  5. El sistema guarda el nivel y vuelve al listado, donde «Ciclos Formativos de Grado Medio Dual» aparece con el grado «Ciclo formativo».
  6. Pulsa la fila «Ciclos Formativos de Grado Medio Dual» y pulsa «Borrar».
  7. El sistema borra el nivel y el listado vuelve a mostrar solo «Ciclos formativos de grado Básico», «Ciclos Formativos de Grado Medio» y «Ciclos Formativos de Grado Superior», los tres con el grado «Ciclo formativo».
- ESC-007 — Un nivel sin grado no se puede guardar:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú Sistema educativo → Niveles y pulsa «Añadir un nuevo nivel».
  3. Rellena el código con «GSD» y el nombre con «Ciclos Formativos de Grado Superior Dual», y deja el grado vacío.
  4. Pulsa «Guardar».
  5. El sistema no guarda el nivel y muestra el error «El grado es obligatorio».
  6. Pulsa «Cancelar» y vuelve al listado de niveles.
  7. El listado no muestra ninguna fila «Ciclos Formativos de Grado Superior Dual».
- ESC-008 — Un grado empieza sin niveles y pasa a exigirlos en cuanto se le crea uno:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú Sistema educativo → Grados y pulsa «Nuevo Grado».
  3. Rellena el código con «F» y el nombre con «Certificado profesional».
  4. Pulsa «Guardar».
  5. El sistema guarda el grado y vuelve al listado, donde aparece la fila «Certificado profesional» con el código «F».
  6. Abre el menú Sistema educativo → Ciclos y pulsa «Añadir un nuevo ciclo».
  7. Rellena el código con «CPINF» y el nombre con «Certificado profesional de informática», y elige la familia profesional «Informática y Comunicaciones».
  8. Abre el selector del campo Grado.
  9. El sistema ofrece, además de «Ciclo formativo» y «Curso de especialización», el grado «Certificado profesional».
  10. Elige el grado «Certificado profesional».
  11. El sistema no muestra el campo Nivel.
  12. Pulsa «Guardar».
  13. El sistema guarda el ciclo y vuelve al listado, donde «Certificado profesional de informática» aparece con el grado «Certificado profesional» y la columna Nivel vacía.
  14. Abre el menú Sistema educativo → Niveles y pulsa «Añadir un nuevo nivel».
  15. Rellena el código con «CP1» y el nombre con «Certificado profesional de nivel 1», y elige el grado «Certificado profesional».
  16. Pulsa «Guardar».
  17. El sistema guarda el nivel y vuelve al listado, donde «Certificado profesional de nivel 1» aparece con el grado «Certificado profesional».
  18. Abre el menú Sistema educativo → Ciclos y pulsa la fila «Certificado profesional de informática».
  19. El sistema muestra ahora el campo Nivel, vacío, y lo señala como obligatorio.
  20. Pulsa «Guardar» sin elegir ningún nivel.
  21. El sistema no guarda el ciclo y muestra el error «El nivel es obligatorio para el grado indicado».
- ESC-014 — Los niveles ya cargados pertenecen al grado «Ciclo formativo»:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú Sistema educativo → Niveles.
  3. El sistema muestra las filas «Ciclos formativos de grado Básico», «Ciclos Formativos de Grado Medio» y «Ciclos Formativos de Grado Superior», las tres con el grado «Ciclo formativo».
  4. Pulsa la fila «Ciclos Formativos de Grado Medio».
  5. El sistema muestra el código «GM», el nombre «Ciclos Formativos de Grado Medio» y el grado «Ciclo formativo».
  6. Cambia el nombre a «Ciclos Formativos de Grado Medio (LOFP)».
  7. Pulsa «Guardar».
  8. El sistema guarda el nivel y vuelve al listado, donde la fila aparece con el nombre «Ciclos Formativos de Grado Medio (LOFP)» y conserva el grado «Ciclo formativo».
  9. Pulsa de nuevo esa fila, cambia el nombre a «Ciclos Formativos de Grado Medio» y pulsa «Guardar».
  10. El sistema guarda el nivel y el listado vuelve a mostrar la fila «Ciclos Formativos de Grado Medio» con el grado «Ciclo formativo».

## HU-003 — Como Administrador quiero ver el grado y el nivel de cada ciclo, y no ver un nivel vacío en los ciclos que no lo llevan, para saber de un vistazo de qué tipo es cada ciclo

- ESC-009 — El listado de ciclos muestra el grado y el nivel:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú Sistema educativo → Ciclos.
  3. El sistema muestra las columnas en el orden código, nombre, familia profesional, grado y nivel.
  4. La fila «Desarrollo de Aplicaciones Web» muestra el grado «Ciclo formativo» y el nivel «Ciclos Formativos de Grado Superior».
  5. La fila «Sistemas Microinformáticos y Redes» muestra el grado «Ciclo formativo» y el nivel «Ciclos Formativos de Grado Medio».
- ESC-010 — Al consultar un ciclo, el nivel solo se ve si el ciclo lo lleva:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú Sistema educativo → Ciclos y pulsa «Añadir un nuevo ciclo».
  3. Rellena el código con «CEIA» y el nombre con «Inteligencia Artificial y Big Data», y elige la familia profesional «Informática y Comunicaciones».
  4. Elige el grado «Curso de especialización» y pulsa «Guardar».
  5. El sistema guarda el ciclo y vuelve al listado, donde «Inteligencia Artificial y Big Data» aparece con el grado «Curso de especialización».
  6. Abre el menú Sistema educativo → Cursos y pulsa «Nuevo curso».
  7. Rellena el código con «CEIA1» y el nombre con «1º CEIA», y elige el ciclo «Inteligencia Artificial y Big Data» y la ley educativa «Ley Orgánica 3/2022, de 31 de marzo, de Ordenación e Integración de la Formación Profesional».
  8. Pulsa «Guardar».
  9. El sistema guarda el curso y vuelve al listado, donde «1º CEIA» aparece con el ciclo «Inteligencia Artificial y Big Data».
  10. Pulsa la fila «1º CEIA».
  11. Pulsa sobre el ciclo «Inteligencia Artificial y Big Data» ya elegido en el curso.
  12. El sistema abre la consulta del ciclo en solo lectura, muestra el nombre, la familia profesional y el grado «Curso de especialización», y no muestra el campo Nivel.
  13. Cierra la consulta y vuelve al formulario del curso «1º CEIA».
  14. Cambia el ciclo del curso a «Guía, Información y Asistencia Turística».
  15. Pulsa sobre el ciclo «Guía, Información y Asistencia Turística» ya elegido en el curso.
  16. El sistema abre la consulta del ciclo y muestra además el campo Nivel con el valor «Ciclos Formativos de Grado Superior».
- ESC-011 — Los cursos de un ciclo se siguen manteniendo desde el propio ciclo:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú Sistema educativo → Ciclos y pulsa la fila «Desarrollo de Aplicaciones Web».
  3. El panel Cursos muestra los cursos que el ciclo trae de los datos iniciales: «1º DAW» y «2º DAW».
  4. En el panel Cursos pulsa «Añadir un nuevo curso».
  5. Rellena el código con «DAW3» y el nombre con «3º DAW», y elige la ley educativa «Ley Orgánica 3/2022, de 31 de marzo, de Ordenación e Integración de la Formación Profesional».
  6. Pulsa «Guardar» en el formulario del curso.
  7. Pulsa «Guardar» en el formulario del ciclo.
  8. El sistema guarda el ciclo y vuelve al listado.
  9. Pulsa de nuevo la fila «Desarrollo de Aplicaciones Web».
  10. El panel Cursos muestra «1º DAW», «2º DAW» y «3º DAW».
  11. El formulario del ciclo sigue mostrando el grado «Ciclo formativo» y el nivel «Ciclos Formativos de Grado Superior».
- ESC-015 — Los módulos de un curso se siguen manteniendo desde el ciclo:
  1. El administrador inicia sesión con usuario «admin» y contraseña «admin».
  2. Abre el menú Sistema educativo → Ciclos y pulsa la fila «Desarrollo de Aplicaciones Web».
  3. En el panel Cursos pulsa la fila «1º DAW».
  4. El panel Módulos del curso muestra el módulo que el curso trae de los datos iniciales: «Itinerario Personal para la Empleabilidad I».
  5. En el panel Módulos pulsa «Añadir un nuevo módulo».
  6. Elige el módulo «Programación».
  7. Pulsa «Guardar» en el formulario del módulo.
  8. Pulsa «Guardar» en el formulario del curso.
  9. Pulsa «Guardar» en el formulario del ciclo.
  10. El sistema guarda el ciclo y vuelve al listado.
  11. Pulsa de nuevo la fila «Desarrollo de Aplicaciones Web» y, en el panel Cursos, la fila «1º DAW».
  12. El panel Módulos del curso muestra «Itinerario Personal para la Empleabilidad I» y «Programación».
  13. El formulario del ciclo sigue mostrando el grado «Ciclo formativo» y el nivel «Ciclos Formativos de Grado Superior».

# Modelos

| Fichero | Modelo | Qué representa |
|---|---|---|
| [entity-Grado.md](./entity-Grado.md) | Grado | El grado de una enseñanza (ciclo formativo, curso de especialización…); ahora sabe además si admite nivel. |
| [entity-Nivel.md](./entity-Nivel.md) | Nivel | El nivel dentro de un grado (básico, medio, superior); ahora dice a qué grado pertenece. |
| [entity-Ciclo.md](./entity-Ciclo.md) | Ciclo | La enseñanza que imparte un centro, con su grado y, si el grado lo admite, su nivel. |

Relaciones:

- Cada **nivel** pertenece a **un grado** y no puede existir sin él. Un grado puede tener varios niveles o ninguno; que tenga al menos uno es lo que hace que el grado admita nivel.
- Cada **ciclo** tiene **un grado**. Además tiene **un nivel** cuando su grado admite nivel, y ninguno cuando no lo admite; ese nivel es siempre uno de los del grado del ciclo.
- No hay borrado en cascada en ninguna de las dos relaciones: borrar un grado no borra sus niveles, y borrar un nivel no borra los ciclos que lo usan.
  Qué debe ocurrir al intentar borrar un grado que todavía tiene niveles, o un nivel que todavía usan ciclos, queda fuera de alcance.

# Pantallas

| Fichero | Pantalla | Para qué sirve |
|---|---|---|
| [screen-ciclos.md](./screen-ciclos.md) | Ciclos | Mantenimiento de los ciclos y de sus cursos y módulos; es donde se pide o se oculta el nivel. Solo Administrador. |
| [screen-niveles.md](./screen-niveles.md) | Niveles | Mantenimiento del catálogo de niveles, que ahora pide el grado al que pertenece cada nivel. Solo Administrador. |
| [screen-consulta-ciclo.md](./screen-consulta-ciclo.md) | Consulta de un ciclo | Ficha de solo lectura de un ciclo, que se abre desde otro formulario donde se ha elegido ese ciclo. |

# Seguridad

- **Administrador:** ve, crea, edita y borra los grados, los niveles y los ciclos desde el menú Sistema educativo, para todos los centros. Los catálogos del sistema educativo son comunes a toda la aplicación, no se reparten por centro.

# Recursos y datos iniciales

Los datos iniciales del subsistema ya cargan los catálogos de familias profesionales, grados, niveles, leyes educativas y módulos, los ciclos, los cursos de cada ciclo y los módulos asignados a cada curso.
Esta iniciativa cambia lo siguiente:

- Los tres niveles que se cargan —«Ciclos formativos de grado Básico», «Ciclos Formativos de Grado Medio» y «Ciclos Formativos de Grado Superior»— pasan a pertenecer al grado «Ciclo formativo».
- El grado «Curso de especialización» se queda sin ningún nivel, que es lo que hace que sus ciclos no lleven nivel.
- Los ciclos que se cargan no cambian: todos son del grado «Ciclo formativo» y ya traen su nivel. Tampoco cambian sus cursos ni los módulos de cada curso.

# Fuera de alcance

- Impedir que se cambie el grado de un nivel que ya usan ciclos, o corregir esos ciclos cuando ocurra. Se asume que el catálogo de grados y niveles no se toca una vez cargado.
- Mostrar en la pantalla de Grados si el grado admite nivel: el indicador lo usa el formulario de ciclo, pero no se añade a la pantalla de mantenimiento de grados.
- Repartir por centro los catálogos del sistema educativo.
