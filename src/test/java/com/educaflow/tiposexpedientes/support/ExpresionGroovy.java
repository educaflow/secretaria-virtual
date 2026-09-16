package com.educaflow.tiposexpedientes.support;

import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.messages.ExceptionMessage;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.transform.stc.StaticTypesMarker;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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
 * son esas dos variables <b>con su tipo</b>, y se compila. Del resultado salen dos cosas:
 *
 * <ul>
 *   <li>El <b>error de compilación</b>, si lo hay. El comprobador estático de Groovy resuelve cada
 *       propiedad contra los getters reales de la entidad y cada FQCN contra el classpath, de modo
 *       que rechaza en tiempo de test lo que en runtime sería un campo vacío en silencio: la
 *       propiedad que no existe, el enum de la versión anterior que se quedó en el XML al duplicarla,
 *       el método con los argumentos mal, el error de sintaxis.</li>
 *   <li>Las <b>navegaciones inseguras</b>: cada {@code .} (propiedad o método) cuyo receptor es una
 *       propiedad del modelo que puede ser {@code null} y que no se navega con {@code ?.}. El
 *       compilador no analiza nulabilidad, así que esto se hace recorriendo el AST ya tipado: el
 *       tipo de cada receptor lo dejó anotado el comprobador estático, y si la propiedad puede ser
 *       {@code null} lo dice el modelo, porque Axelor genera {@code @NotNull} en los campos con
 *       {@code required="true"} (y un primitivo nunca es {@code null}). Como el resultado de un
 *       {@code ?.} es nulo cuando su receptor lo es, todo paso posterior a un {@code ?.} también ha
 *       de ser seguro.</li>
 * </ul>
 *
 * <p>Lo que sigue sin verse es lo que depende de los valores y no de los tipos: un patrón de
 * {@code DateTimeFormatter} inválido, o el resultado de un método, que no se analiza (solo las
 * propiedades del modelo tienen una nulabilidad declarada).
 */
public final class ExpresionGroovy {

    private static final String FQCN_NOW = "java.time.LocalDateTime";
    private static final String NOMBRE_METODO = "__expresion";

    private static final ClassLoader loader = ExpresionGroovy.class.getClassLoader();

    /**
     * Una navegación sin {@code ?.} sobre una propiedad que puede ser {@code null}.
     *
     * @param receptor  el texto de la propiedad que puede ser null, p.ej. {@code self.personaInteresada}.
     * @param propiedad su nombre, p.ej. {@code personaInteresada}.
     * @param clase     el FQCN de la clase que la declara.
     * @param paso      el texto de la navegación completa que revienta, p.ej. {@code self.personaInteresada.nombre}.
     */
    public record NavegacionInsegura(String receptor, String propiedad, String clase, String paso) {}

    /** El resultado de compilar una expresión: o un error, o la lista (quizá vacía) de navegaciones inseguras. */
    public record Compilacion(Optional<String> error, List<NavegacionInsegura> navegacionesInseguras) {}

    private ExpresionGroovy() {}

    /**
     * Compila la expresión con {@code self} del tipo dado y {@code now} como {@code LocalDateTime}.
     * Una expresión en blanco compila sin más: el runtime la salta sin evaluar.
     */
    public static Compilacion compilar(String expresion, String fqcnSelf) {
        if (expresion == null || expresion.isBlank()) {
            return new Compilacion(Optional.empty(), List.of());
        }

        String script = "@groovy.transform.TypeChecked\n"
                + "Object " + NOMBRE_METODO + "(" + fqcnSelf + " self, " + FQCN_NOW + " now) {\n"
                + "    return (\n" + expresion + "\n    )\n"
                + "}\n";

        CompilationUnit unidad = new CompilationUnit(new CompilerConfiguration(), null, new GroovyClassLoader(loader));
        unidad.addSource("expresion.groovy", script);
        try {
            // Hasta generar la clase: el comprobador estático corre en INSTRUCTION_SELECTION y deja
            // en cada nodo del AST el tipo inferido, que es lo que lee el análisis de nulabilidad.
            unidad.compile(Phases.CLASS_GENERATION);
        } catch (MultipleCompilationErrorsException ex) {
            return new Compilacion(Optional.of(ex.getErrorCollector().getErrors().stream()
                    .map(ExpresionGroovy::texto)
                    .collect(Collectors.joining("\n"))), List.of());
        } catch (CompilationFailedException ex) {
            return new Compilacion(Optional.of(ex.getMessage()), List.of());
        }

        return new Compilacion(Optional.empty(), navegacionesInseguras(unidad));
    }

    // ------------------------------------------------------------------ nulabilidad

    private static List<NavegacionInsegura> navegacionesInseguras(CompilationUnit unidad) {
        Expression expresion = expresionDelMetodo(unidad);
        List<NavegacionInsegura> inseguras = new ArrayList<>();

        expresion.visit(new CodeVisitorSupport() {
            @Override
            public void visitPropertyExpression(PropertyExpression paso) {
                if (!paso.isSafe()) {
                    comprobar(paso.getObjectExpression(), paso.getText(), inseguras);
                }
                super.visitPropertyExpression(paso);
            }

            @Override
            public void visitMethodCallExpression(MethodCallExpression paso) {
                if (!paso.isSafe() && !paso.isImplicitThis()) {
                    comprobar(paso.getObjectExpression(), paso.getText(), inseguras);
                }
                super.visitMethodCallExpression(paso);
            }
        });

        return inseguras;
    }

    /**
     * Si el receptor de un paso es una propiedad del modelo que puede ser null, lo apunta. Solo se
     * juzgan receptores que son propiedades: {@code self}, {@code now}, una clase (acceso estático)
     * o un literal nunca son null, y el resultado de un método o de un operador no se analiza.
     */
    private static void comprobar(Expression receptor, String paso, List<NavegacionInsegura> inseguras) {
        if (!(receptor instanceof PropertyExpression propiedad)
                || propiedad.getObjectExpression() instanceof ClassExpression) {
            return;
        }
        Class<?> duenyo = clase(propiedad.getObjectExpression());
        if (duenyo == null) {
            return;
        }
        String nombre = propiedad.getPropertyAsString();
        if (nombre == null) {
            return;
        }

        // Tras un ?. el valor es null si lo era su receptor, sea o no required la propiedad.
        if (propiedad.isSafe() || puedeSerNull(duenyo, nombre)) {
            inseguras.add(new NavegacionInsegura(propiedad.getText(), nombre, duenyo.getName(), paso));
        }
    }

    /** La clase Java del tipo que el comprobador estático infirió para la expresión, o null si no se puede cargar. */
    private static Class<?> clase(Expression expresion) {
        ClassNode tipo = expresion.getNodeMetaData(StaticTypesMarker.INFERRED_TYPE);
        if (tipo == null) {
            tipo = expresion.getType();
        }
        try {
            return Class.forName(tipo.getName(), false, loader);
        } catch (ClassNotFoundException | LinkageError ex) {
            return null;
        }
    }

    /**
     * Si la propiedad puede ser null según el modelo: no lo puede ser si es un primitivo o si su
     * campo o su getter llevan {@code @NotNull} (lo que Axelor genera para {@code required="true"}).
     */
    private static boolean puedeSerNull(Class<?> clase, String propiedad) {
        Method getter = getter(clase, propiedad);
        if (getter != null && (getter.getReturnType().isPrimitive() || tieneNotNull(getter))) {
            return false;
        }
        Field campo = campo(clase, propiedad);
        if (campo != null && (campo.getType().isPrimitive() || tieneNotNull(campo))) {
            return false;
        }

        return true;
    }

    private static boolean tieneNotNull(AnnotatedElement elemento) {
        for (var anotacion : elemento.getAnnotations()) {
            String nombre = anotacion.annotationType().getSimpleName();
            if (nombre.equals("NotNull") || nombre.equals("NonNull")) {
                return true;
            }
        }

        return false;
    }

    private static Method getter(Class<?> clase, String propiedad) {
        String sufijo = Character.toUpperCase(propiedad.charAt(0)) + propiedad.substring(1);
        for (String nombre : new String[] {"get" + sufijo, "is" + sufijo}) {
            try {
                return clase.getMethod(nombre);
            } catch (NoSuchMethodException ex) {
                // se prueba el siguiente
            }
        }

        return null;
    }

    private static Field campo(Class<?> clase, String propiedad) {
        for (Class<?> c = clase; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(propiedad);
            } catch (NoSuchFieldException ex) {
                // se sube a la superclase
            }
        }

        return null;
    }

    // ------------------------------------------------------------------ AST

    /** La expresión del {@code return} del método envoltorio. */
    private static Expression expresionDelMetodo(CompilationUnit unidad) {
        for (ClassNode clase : unidad.getAST().getModules().get(0).getClasses()) {
            List<MethodNode> metodos = clase.getDeclaredMethods(NOMBRE_METODO);
            if (!metodos.isEmpty()) {
                BlockStatement cuerpo = (BlockStatement) metodos.get(0).getCode();
                return ((ReturnStatement) cuerpo.getStatements().get(0)).getExpression();
            }
        }
        throw new IllegalStateException("El script envoltorio no tiene el método " + NOMBRE_METODO);
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
