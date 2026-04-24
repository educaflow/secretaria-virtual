package com.educaflow.subsystem.importacion.service.impl;

import com.axelor.db.Repository;
import com.axelor.db.modelservice.DefaultModelService;
import com.axelor.inject.Beans;
import com.educaflow.base.infrastructure.importer.DataImport;
import com.educaflow.base.infrastructure.importer.FileImporter;
import com.educaflow.base.infrastructure.importer.FileImporterFactory;
import com.educaflow.base.infrastructure.validation.messages.BusinessException;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessage;
import com.educaflow.base.infrastructure.validation.messages.BusinessMessages;
import com.educaflow.base.util.MetaFileUtil;
import com.educaflow.base.util.SecurityUtil;
import com.educaflow.base.util.XmlUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.repo.CentroRepository;
import com.educaflow.subsystem.importacion.db.TareaImportacion;
import com.educaflow.subsystem.importacion.service.TareaImportacionService;
import com.educaflow.subsystem.importacion.service.TipoFicheroConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

public class TareaImportacionServiceImpl extends DefaultModelService<TareaImportacion> implements TareaImportacionService {

    private final Logger logger = LoggerFactory.getLogger(TareaImportacionServiceImpl.class);

    public TareaImportacionServiceImpl(Class<TareaImportacion> model, Repository repository) {
        super(model, repository);
    }

    @Override
    public BusinessMessages importar(TareaImportacion tareaImportacion) throws BusinessException {
        FileImporter fileImporter = FileImporterFactory.getFileImporter();
        try {
            BusinessMessages businessMessages = new BusinessMessages();

            TipoFicheroConfig.Rutas rutas = TipoFicheroConfig.getRutas(tareaImportacion.getTipoFichero());
            if (rutas == null || rutas.configPath() == null) {
                throw new BusinessException(new BusinessMessage("Tipo de fichero sin configuración de importación: " + tareaImportacion.getTipoFichero()));
            }

            byte[] contenido = MetaFileUtil.downloadContent(tareaImportacion.getFichero());

            extraerYValidarCentro(tareaImportacion, contenido);

            DataImport dataImport = new DataImport(contenido, rutas.configPath(), rutas.schemaPath());
            List<String> result = fileImporter.importFile(dataImport);
            for (String message : result) {
                businessMessages.add(new BusinessMessage(message));
            }
            return businessMessages;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error al importar el fichero", e);
            throw new BusinessException(new BusinessMessage("Error al importar el fichero: " + e.getMessage()));
        }
    }

    private void extraerYValidarCentro(TareaImportacion tareaImportacion, byte[] contenido) throws BusinessException {
        Map<String, String> atributos = XmlUtil.leerAtributosNodo(new ByteArrayInputStream(contenido), "/centro");

        if (atributos.isEmpty()) {
            tareaImportacion.setCentro(null);
            tareaImportacion.setCurso(null);
            return;
        }

        String codigoCentro = atributos.get("codigo");
        String cursoStr = atributos.get("curso");

        Centro centro = codigoCentro != null ? Beans.get(CentroRepository.class).findByCode(codigoCentro) : null;
        Integer curso = cursoStr != null ? Integer.parseInt(cursoStr) : null;

        tareaImportacion.setCentro(centro);
        tareaImportacion.setCurso(curso);

        if (!SecurityUtil.esAdmin(tareaImportacion.getUsuario())) {
            Centro centroActivo = tareaImportacion.getUsuario().getCentroActivo();
            if (centroActivo == null || !centroActivo.getCode().equals(codigoCentro)) {
                String centroActivoCodigo = centroActivo != null ? centroActivo.getCode() : "ninguno";
                throw new BusinessException(new BusinessMessage(
                    "El fichero pertenece al centro '" + codigoCentro + "', pero tu centro activo es '" + centroActivoCodigo + "'"
                ));
            }
        }
    }
}
