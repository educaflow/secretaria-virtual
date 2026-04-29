package com.educaflow.subsystem.importacion.service.impl;

import com.axelor.db.JpaRepository;
import com.axelor.inject.Beans;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessage;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.base.util.DniUtil;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.base.util.XMLUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.common.db.repo.CentroRepository;
import com.educaflow.subsystem.importacion.db.TareaImportacion;
import com.educaflow.subsystem.importacion.service.ImportadorTipoFichero;
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
import java.util.Optional;

public class ImportadorUsuarioXml implements ImportadorTipoFichero {

    private final String nodoItem;
    private final String tipoUsuarioCode;
    private final String schemaPath;

    public ImportadorUsuarioXml(String nodoItem, String tipoUsuarioCode, String schemaPath) {
        this.nodoItem = nodoItem;
        this.tipoUsuarioCode = tipoUsuarioCode;
        this.schemaPath = schemaPath;
    }

    @Override
    public BusinessMessages importar(byte[] contenido, Centro centro, Integer curso) throws BusinessException {
        validarEsquema(contenido);
        Centro centroActivo = SecurityUtil.getUser().getCentroActivo();
        if (centroActivo == null) {
            throw new BusinessException(new BusinessMessage("No tienes un centro activo asignado en tu perfil"));
        }
        if (!centroActivo.getCode().equals(centro.getCode())) {
            throw new BusinessException(new BusinessMessage(
                    "El fichero pertenece al centro '" + centro.getName() + "', pero tu centro activo es '" + centroActivo.getName() + "'"));
        }
        TipoUsuario tipoUsuario = obtenerTipoUsuario();
        UsuarioAutorizadoRepository repo = (UsuarioAutorizadoRepository) JpaRepository.of(UsuarioAutorizado.class);

        Document doc = XMLUtil.getDocument(contenido);
        Element root = doc.getDocumentElement();
        NodeList items = root.getElementsByTagName(nodoItem);

        int creados = 0;
        int existentes = 0;
        List<String> errores = new ArrayList<>();

        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            String documentoRaw = item.getAttribute("documento");
            try {
                String dni = DniUtil.clean(documentoRaw);
                if (!DniUtil.isValid(dni)) {
                    throw new IllegalArgumentException("DNI inválido: " + documentoRaw);
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
                errores.add("Fila " + (i + 1) + " [" + documentoRaw + "]: " + e.getMessage());
            }
        }

        return construirResumen(creados, existentes, errores, items.getLength());
    }

    @Override
    public BusinessMessages importar(byte[] contenido) throws BusinessException {
        Element root = XMLUtil.getDocument(contenido).getDocumentElement();
        String codigoCentro = XMLUtil.getStringAttribute(root, "codigo", null);
        String cursoStr = XMLUtil.getStringAttribute(root, "curso", null);
        String centroActivo = SecurityUtil.getUser().getCentroActivo() != null ? SecurityUtil.getUser().getCentroActivo().getCode() : "ninguno";
        if (codigoCentro != null && !codigoCentro.equals(centroActivo)) {
            throw new BusinessException(new BusinessMessage(
                    "El fichero pertenece al centro '" + codigoCentro + "', pero tu centro activo es '" + centroActivo + "'"));
        }
        Centro centro = codigoCentro != null ? Beans.get(CentroRepository.class).findByCode(codigoCentro) : null;
        Integer curso = cursoStr != null ? Integer.parseInt(cursoStr) : null;
        return importar(contenido, centro, curso);
    }

    private void validarEsquema(byte[] contenido) throws BusinessException {
        try (InputStream schema = getClass().getResourceAsStream(schemaPath);
             InputStream data = new ByteArrayInputStream(contenido)) {
            if (schema == null) {
                throw new BusinessException(new BusinessMessage("Esquema de validación no encontrado: " + schemaPath));
            }
            Optional<String> error = XMLUtil.validarConSchema(data, schema);
            if (error.isPresent()) {
                throw new BusinessException(new BusinessMessage("El fichero XML no es válido: " + error.get()));
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(new BusinessMessage("Error leyendo el esquema de validación: " + schemaPath));
        }
    }

    private TipoUsuario obtenerTipoUsuario() throws BusinessException {
        TipoUsuario tipoUsuario = JpaRepository.of(TipoUsuario.class).all()
                .filter("self.code = :code")
                .bind("code", tipoUsuarioCode)
                .fetchOne();
        if (tipoUsuario == null) {
            throw new BusinessException(new BusinessMessage("TipoUsuario no encontrado: " + tipoUsuarioCode));
        }
        return tipoUsuario;
    }

    private BusinessMessages construirResumen(int creados, int existentes, List<String> errores, int total) {
        BusinessMessages result = new BusinessMessages();
        result.add(new BusinessMessage(String.format(
                "Nuevos: %d | Ya existían: %d | Errores: %d | Total: %d",
                creados, existentes, errores.size(), total)));
        errores.forEach(msg -> result.add(new BusinessMessage(msg)));
        return result;
    }
}