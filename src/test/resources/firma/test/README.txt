CERTIFICADOS DE TEST
====================

Estos .p12 son certificados de usuario final emitidos por la CA FALSA de demo
(src/main/resources/firma/demo/, "ACCV DEMO EducaFlow - NO VALIDA").
Solo son de confianza en los entornos que usan el almacén de demo (firma/demo/almacen/truststore-demo.jks): en producción no sirven para nada.

NO son de ningún usuario de demo: los usan los tests que dan de alta, modifican o borran filas de
CertificadoDigital (los tests unitarios y los E2E de src/test/e2e/subsystem/criptografia), para que esos tests
nunca toquen los certificados de los usuarios de demo.
Están fuera de src/main a propósito: no van dentro del WAR.

Contraseña de todos: demo1234

Cómo se crea uno nuevo (no sobrescribe uno que ya exista):
    src/main/resources/firma/demo/crear_certificado_test.sh <nombre>
El script genera un DNI de demo que no es de ningún usuario de demo, deja el certificado en
src/test/resources/firma/test/<nombre>.p12 y añade su línea al final de este fichero.

Aviso sobre los DNI: se han generado aleatoriamente, no pertenecen a ninguna persona real y cualquier
coincidencia con el DNI de una persona real es casualidad. Empiezan todos por 9 solo para reconocerlos
de un vistazo como DNI de demo.

Certificados (fichero, DNI y titular):
test1.p12  DNI=96545584W  titular=test1 Certificado Test
test2.p12  DNI=97980298E  titular=test2 Certificado Test
