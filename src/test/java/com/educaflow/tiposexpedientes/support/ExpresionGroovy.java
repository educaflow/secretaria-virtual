package com.educaflow.tiposexpedientes.support;

import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.messages.ExceptionMessage;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Comprobación <b>estática</b> de una expresión Groovy de un documento PDF contra la entidad del
 * tipo de expediente, sin evaluarla y sin ninguna instancia.
 *
 * <p>En runtime la expresión se evalúa con {@code GroovyShell} sobre un binding con dos variables:
 * {@code self}, la entidad del expediente, y {@code now}, un {@code LocalDateTime}
 * ({@code ExpedienteUtil.getDocumentoPdf}). Groovy es dinámico, así que ahí una propiedad
 * inexistente o un FQCN mal escrito no se saben hasta que se evalúa, y entonces
 * {@code EvaluatorImplGroovy} se traga la excepción y deja el campo vacío.
 *
 * <p>Aquí se envuelve la expresión en un método anotado con {@code @TypeChecked} cuyos parámetros
 * son esas dos variables <b>con su tipo</b>, y se compila. El comprobador estático de Groovy
 * resuelve entonces cada propiedad contra los getters reales de la entidad y cada FQCN contra el
 * classpath, de modo que rechaza en tiempo de test lo que en runtime sería un campo vacío en
 * silencio: la propiedad que no existe, el enum de la versión anterior que se quedó en el XML al
 * duplicarla, el método con los argumentos mal, el error de sintaxis. Lo que sigue sin ver es lo
 * que depende de los valores: una relación a {@code null} en mitad de una cadena, o un patrón
 * de fecha inválido.
 */
public final class ExpresionGroovy {

    private static final String FQCN_NOW = "java.time.LocalDateTime";

    private static final GroovyShell shell = new GroovyShell(ExpresionGroovy.class.getClassLoader());

    private ExpresionGroovy() {}

    /**
     * El error de compilación de la expresión con {@code self} del tipo dado, o vacío si compila.
     * Una expresión en blanco compila: el runtime la salta sin evaluar.
     */
    public static Optional<String> errorDeCompilacion(String expresion, String fqcnSelf) {
        if (expresion == null || expresion.isBlank()) {
            return Optional.empty();
        }

        String script = "@groovy.transform.TypeChecked\n"
                + "Object __expresion(" + fqcnSelf + " self, " + FQCN_NOW + " now) {\n"
                + "    return (\n" + expresion + "\n    )\n"
                + "}\n";
        try {
            shell.parse(script, "expresion.groovy");
            return Optional.empty();
        } catch (MultipleCompilationErrorsException ex) {
            return Optional.of(ex.getErrorCollector().getErrors().stream()
                    .map(ExpresionGroovy::texto)
                    .collect(Collectors.joining("\n")));
        } catch (CompilationFailedException ex) {
            return Optional.of(ex.getMessage());
        }
    }

    /** El mensaje de un error del compilador sin la posición ni el listado del script envoltorio. */
    private static String texto(Message mensaje) {
        if (mensaje instanceof SyntaxErrorMessage error) {
            return error.getCause().getOriginalMessage().trim();
        }
        if (mensaje instanceof ExceptionMessage error) {
            return error.getCause().toString().trim();
        }
        StringWriter sw = new StringWriter();
        mensaje.write(new PrintWriter(sw));

        return sw.toString().trim();
    }
}
