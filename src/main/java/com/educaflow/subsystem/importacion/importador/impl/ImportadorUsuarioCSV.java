package com.educaflow.subsystem.importacion.importador.impl;

import com.axelor.auth.AuthUtils;
import com.axelor.db.modelservice.ModelServiceFactory;
import com.axelor.inject.Beans;
import com.axelor.meta.MetaFiles;
import com.axelor.meta.db.MetaFile;
import com.educaflow.base.util.DniUtil;
import com.educaflow.subsystem.common.db.Centro;
import com.educaflow.subsystem.common.db.TipoUsuario;
import com.educaflow.subsystem.common.service.TipoUsuarioService;
import com.educaflow.subsystem.importacion.db.TipoFicheroImportacion;
import com.educaflow.subsystem.importacion.exception.ImportadorException;
import com.educaflow.subsystem.importacion.importador.ImportadorFichero;
import com.educaflow.subsystem.importacion.importador.ResultadoImportacion;
import com.educaflow.subsystem.registrousuario.db.UsuarioAutorizado;
import com.educaflow.subsystem.registrousuario.service.UsuarioAutorizadoService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ImportadorUsuarioCSV implements ImportadorFichero {

    private final MetaFile fichero;
    private final TipoFicheroImportacion tipoFichero;

    public ImportadorUsuarioCSV(MetaFile fichero, TipoFicheroImportacion tipoFichero) {
        this.fichero = fichero;
        this.tipoFichero = tipoFichero;
    }

    @Override
    public ResultadoImportacion importar() throws ImportadorException {
        ContextoImportacion ctx = resolverContexto();
        List<String> lineas = leerLineas();
        return procesarLineas(lineas, ctx);
    }

    private record ContextoImportacion(Centro centro, Integer curso, TipoUsuario tipoUsuario) {}

    private static final class ContadorImportacion {
        int creados;
        int ignorados;
        int errores;
        final List<String> entradasLog = new ArrayList<>();

        void incrementarCreados() { creados++; }
        void incrementarIgnorados() { ignorados++; }
        void incrementarErrores() { errores++; }
        void añadirEntradaLog(String entrada) { entradasLog.add(entrada); }
    }

    private ContextoImportacion resolverContexto() throws ImportadorException {
        Centro centro = AuthUtils.getUser().getCentroActivo();
        if (centro == null) {
            throw new ImportadorException(
                "Importación abortada: el importador no tiene centro activo asignado.");
        }
        Integer curso = centro.getCurso();
        if (curso == null) {
            throw new ImportadorException(
                "Importación abortada: el centro '" + centro.getName() + "' no tiene curso activo asignado.");
        }
        TipoUsuarioService tipoUsuarioSvc =
            (TipoUsuarioService) Beans.get(ModelServiceFactory.class).resolve(TipoUsuario.class);
        TipoUsuario tipoUsuario = tipoUsuarioSvc.findByCodigo(tipoFichero.name())
            .orElseThrow(() -> new ImportadorException(
                "Importación abortada: error de configuración, no existe el tipo de usuario con código '" + tipoFichero.name() + "'."));
        return new ContextoImportacion(centro, curso, tipoUsuario);
    }

    private List<String> leerLineas() throws ImportadorException {
        try {
            Path path = Beans.get(MetaFiles.class).getPath(fichero);
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new ImportadorException(
                "No se ha podido leer el fichero CSV. Motivo: " + ex.getMessage() + ".");
        }
    }

    private ResultadoImportacion procesarLineas(List<String> lineas, ContextoImportacion ctx) {
        UsuarioAutorizadoService usuarioAutorizadoService =
            (UsuarioAutorizadoService) Beans.get(ModelServiceFactory.class).resolve(UsuarioAutorizado.class);
        ContadorImportacion contador = new ContadorImportacion();
        for (int i = 0; i < lineas.size(); i++) {
            String linea = lineas.get(i);
            if (linea.isBlank()) continue;
            procesarLinea(i + 1, linea, ctx, usuarioAutorizadoService, contador);
        }
        String log = componerLog(contador);
        return new ResultadoImportacion(
            contador.creados, contador.ignorados, contador.errores, log, ctx.centro(), ctx.curso());
    }

    private void procesarLinea(int numeroLinea, String linea,
                                ContextoImportacion ctx,
                                UsuarioAutorizadoService usuarioAutorizadoService,
                                ContadorImportacion contador) {
        String dniLeido = linea.trim();
        String dniNormalizado = DniUtil.clean(dniLeido);
        try {
            if (!DniUtil.isValid(dniNormalizado)) {
                contador.incrementarErrores();
                contador.añadirEntradaLog("Línea " + numeroLinea + " [" + dniLeido + "]: DNI no válido.");
                return;
            }
            procesarDni(numeroLinea, dniLeido, dniNormalizado, ctx, usuarioAutorizadoService, contador);
        } catch (Exception ex) {
            contador.incrementarErrores();
            contador.añadirEntradaLog(
                "Línea " + numeroLinea + " [" + dniLeido + "]: Error inesperado: " + ex.getMessage() + ".");
        }
    }

    private void procesarDni(int numeroLinea, String dniLeido, String dniNormalizado,
                              ContextoImportacion ctx,
                              UsuarioAutorizadoService usuarioAutorizadoService,
                              ContadorImportacion contador) {
        Optional<UsuarioAutorizado> existente = usuarioAutorizadoService
            .findByCentroDniTipoUsuarioCurso(ctx.centro(), dniNormalizado, ctx.tipoUsuario(), ctx.curso());
        if (existente.isPresent()) {
            contador.incrementarIgnorados();
            contador.añadirEntradaLog("Línea " + numeroLinea + " [" + dniLeido + "]: Ya existe.");
        } else {
            UsuarioAutorizado ua = new UsuarioAutorizado();
            ua.setCentro(ctx.centro());
            ua.setDni(dniNormalizado);
            ua.setTipoUsuario(ctx.tipoUsuario());
            ua.setCurso(ctx.curso());
            ua.setFechaExportacion(LocalDateTime.now());
            usuarioAutorizadoService.insert(ua);
            contador.incrementarCreados();
        }
    }

    private String componerLog(ContadorImportacion contador) {
        StringBuilder sb = new StringBuilder();
        sb.append("Creados: ").append(contador.creados)
          .append("\nIgnorados: ").append(contador.ignorados)
          .append("\nErrores: ").append(contador.errores);
        for (String entrada : contador.entradasLog) {
            sb.append("\n").append(entrada);
        }
        return sb.toString();
    }
}
