import xml.etree.ElementTree as ET
import base64
import os

def extract_x509_certificates(xml_file_path, output_dir="certs"):
    """
    Extrae certificados X.509 de un archivo XML TSL y los guarda como archivos .cer.
    """
    if not os.path.exists(output_dir):
        os.makedirs(output_dir)

    try:
        tree = ET.parse(xml_file_path)
        root = tree.getroot()
    except Exception as e:
        print(f"Error al parsear el archivo XML: {e}")
        return

    # Namespace del documento TSL
    namespaces = {
        'tsl': 'http://uri.etsi.org/02231/v2#'
    }

    certs_found = 0
    # Buscar todas las etiquetas X509Certificate en el XML
    for cert_element in root.findall(".//tsl:X509Certificate", namespaces):
        cert_b64 = cert_element.text.strip()
        if cert_b64:
            try:
                # Decodificar el certificado Base64
                cert_der = base64.b64decode(cert_b64)
                cert_file_name = f"ca_cert_{certs_found + 1}.cer"
                cert_file_path = os.path.join(output_dir, cert_file_name)

                with open(cert_file_path, "wb") as f:
                    f.write(cert_der)
                print(f"Certificado guardado: {cert_file_path}")
                certs_found += 1
            except Exception as e:
                print(f"Error al decodificar o guardar el certificado: {e}")
    
    if certs_found == 0:
        print("No se encontraron certificados X.509 en el archivo XML.")
    else:
        print(f"Extracción completada. Se guardaron {certs_found} certificados en el directorio '{output_dir}'.")

if __name__ == "__main__":
    tsl_xml_file = "TSL.xml"  # Asegúrate de que este sea el nombre de tu archivo TSL.xml
    extract_x509_certificates(tsl_xml_file)
