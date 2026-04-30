package com.educaflow.subsystem.importacion.service.tipoimportador.impl;

import com.axelor.db.JpaRepository;
import com.educaflow.base.util.DniUtil;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.base.util.XMLUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.importacion.service.tipoimportador.ImportadorException;
import com.educaflow.subsystem.importacion.service.tipoimportador.ImportadorFichero;
import com.educaflow.subsystem.importacion.service.tipoimportador.ResultadoImportacion;
import com.educaflow.subsystem.importacion.service.tipoimportador.ResultadoImportacion.MensajeImportacion;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;
import com.educaflow.subsystem.registrousuario.db.repo.UsuarioAutorizadoRepository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;  // kept for XMLUtil.validarConSchema return type

public class ImportadorUsuarioXml implements ImportadorFichero {

    private final String nodoItem;
    private final String tipoUsuarioCode;
    private final String schemaPath;

    public ImportadorUsuarioXml(String nodoItem, String tipoUsuarioCode, String schemaPath) {
        this.nodoItem = nodoItem;
        this.tipoUsuarioCode = tipoUsuarioCode;
        this.schemaPath = schemaPath;
    }

    @Override
    public ResultadoImportacion importar(byte[] contenido, Centro centro, Integer curso) {
        int creados = 0;
        int existentes = 0;
        List<MensajeImportacion> mensajes = new ArrayList<>();

        validarEsquema(contenido);
        Centro centroActivo = SecurityUtil.getUser().getCentroActivo();
        if (centroActivo == null) {
            throw new ImportadorException("No tienes un centro activo asignado en tu perfil");
        }
        if (!centroActivo.getCode().equals(centro.getCode())) {
            throw new ImportadorException(
                    "El fichero pertenece al centro '" + centro.getName() + "', pero tu centro activo es '" + centroActivo.getName() + "'");
        }
        TipoUsuario tipoUsuario = obtenerTipoUsuario();
        UsuarioAutorizadoRepository repo = (UsuarioAutorizadoRepository) JpaRepository.of(UsuarioAutorizado.class);

        Document doc = XMLUtil.getDocument(contenido);
        Element root = doc.getDocumentElement();
        NodeList items = root.getElementsByTagName(nodoItem);

        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            String documentoRaw = item.getAttribute("documento");
            try {
                String dni = DniUtil.clean(documentoRaw);
                if (!DniUtil.isValid(dni)) {
                    mensajes.add(new MensajeImportacion(i + 1, documentoRaw, "DNI inválido"));
                    continue;
                }

                boolean existe = repo.all()
                        .filter("self.centro = :centro AND self.curso = :curso AND self.dni = :dni AND self.tipoUsuario = :tipoUsuario")
                        .bind("centro", centro)
                        .bind("curso", curso)
                        .bind("dni", dni)
                        .bind("tipoUsuario", tipoUsuario)
                        .count() > 0;

                if (existe) {
                    existentes++;
                } else {
                    UsuarioAutorizado ua = new UsuarioAutorizado();
                    ua.setCentro(centro);
                    ua.setCurso(curso);
                    ua.setDni(dni);
                    ua.setTipoUsuario(tipoUsuario);
                    repo.save(ua);
                    creados++;
                }
            } catch (Exception e) {
                mensajes.add(new MensajeImportacion(i + 1, documentoRaw, e.getMessage()));
            }
        }

        String resumen = String.format("Nuevos: %d | Ya existían: %d | Avisos: %d | Total: %d",
                creados, existentes, mensajes.size(), items.getLength());
        return new ResultadoImportacion(resumen, mensajes);
    }

    private void validarEsquema(byte[] contenido) {
        try (InputStream schema = getClass().getResourceAsStream(schemaPath);
             InputStream data = new ByteArrayInputStream(contenido)) {
            if (schema == null) {
                throw new ImportadorException("Esquema de validación no encontrado: " + schemaPath);
            }
            Optional<String> error = XMLUtil.validarConSchema(data, schema);
            if (error.isPresent()) {
                throw new ImportadorException("El fichero XML no es válido: " + error.get());
            }
        } catch (IOException e) {
            throw new ImportadorException("Error leyendo el esquema de validación: " + schemaPath);
        }
    }

    private TipoUsuario obtenerTipoUsuario() {
        TipoUsuario tipoUsuario = JpaRepository.of(TipoUsuario.class).all()
                .filter("self.code = :code")
                .bind("code", tipoUsuarioCode)
                .fetchOne();
        if (tipoUsuario == null) {
            throw new ImportadorException("TipoUsuario no encontrado: " + tipoUsuarioCode);
        }
        return tipoUsuario;
    }

}