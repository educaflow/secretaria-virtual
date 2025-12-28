#!/bin/bash

./inicializar_dispositivo.sh 123456 12345678


./crear_certificado_ficticio.sh "Sara Directora" ./director.p12
./load_certificate.sh 01 Director  123456 ./director.p12 


./crear_certificado_ficticio.sh "Marcos Secretario" ./secretario.p12
./load_certificate.sh 02 Secretario  123456 ./secretario.p12
