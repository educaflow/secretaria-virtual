package com.educaflow.ordenmetodos.support;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

import javax.lang.model.element.Modifier;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Un fichero {@code .java} de producción leído como <b>código fuente</b>: los métodos de su tipo
 * principal en el orden en que están escritos, sus headers de bloque y sus líneas.
 *
 * <p>Se parsea con el compilador del JDK ({@code com.sun.source}), sin resolver tipos. Hace falta el
 * fuente y no el bytecode porque lo que estas reglas miran no sobrevive a la compilación: los
 * comentarios de header, las líneas en blanco y el orden de declaración de una interfaz.
 */
public final class FuenteJava {

    public static final Path RAIZ = Path.of("src/main/java");

    private static final Pattern LINEA_DE_HEADER = Pattern.compile("^\\s*/\\*{10,}.*\\*{5,}/\\s*$");

    /**
     * Un método del tipo principal. {@code accesible} es {@code public} o {@code protected} (o
     * cualquier método de una interfaz); {@code llamadas} son los nombres de los métodos que invoca
     * con receptor ({@code algo.nombre(...)}), en el orden en que aparecen en su cuerpo.
     */
    public record Metodo(String nombre, boolean accesible, String retorno, long linea, long lineaFin, List<String> llamadas) {

        public boolean esValidacion() {
            return nombre.startsWith("validate") || retorno.contains("Optional<BusinessMessages>");
        }

        public boolean esAllowProperties() {
            return nombre.startsWith("allowProperties");
        }

        public boolean esActionRule() {
            return nombre.startsWith("fireActionRule_");
        }
    }

    /** Un header de bloque: las tres líneas {@code /*****…*****}{@code /} con el título en la del medio. */
    public record Header(String titulo, long linea, boolean bienFormado) {
    }

    private final Path path;
    private final boolean interfaz;
    private final List<Metodo> metodos;
    private final List<Header> headers;
    private final List<String> lineas;

    private FuenteJava(Path path, boolean interfaz, List<Metodo> metodos, List<Header> headers, List<String> lineas) {
        this.path = path;
        this.interfaz = interfaz;
        this.metodos = metodos;
        this.headers = headers;
        this.lineas = lineas;
    }

    /** Todos los {@code .java} de producción cuyo nombre de fichero acaba en {@code sufijo}. */
    public static List<FuenteJava> conSufijo(String sufijo) {
        try (Stream<Path> paths = Files.walk(RAIZ)) {
            return paths.filter(p -> p.getFileName().toString().endsWith(sufijo))
                    .sorted()
                    .map(FuenteJava::leer)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static FuenteJava leer(Path path) {
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
                JavacTask task = (JavacTask) compiler.getTask(null, fileManager, diagnostic -> { }, List.of("-proc:none"), null,
                        fileManager.getJavaFileObjects(path));
                CompilationUnitTree unit = task.parse().iterator().next();
                SourcePositions positions = Trees.instance(task).getSourcePositions();
                ClassTree tipo = (ClassTree) unit.getTypeDecls().stream()
                        .filter(ClassTree.class::isInstance)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Sin tipo principal: " + path));
                boolean interfaz = tipo.getKind() == Tree.Kind.INTERFACE;
                List<String> lineas = Files.readAllLines(path, StandardCharsets.UTF_8);

                return new FuenteJava(path, interfaz, leerMetodos(tipo, unit, positions, interfaz), leerHeaders(lineas), lineas);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String ruta() {
        return path.toString();
    }

    public boolean esInterfaz() {
        return interfaz;
    }

    public List<Metodo> metodos() {
        return metodos;
    }

    public List<Header> headers() {
        return headers;
    }

    /** El texto de la línea {@code numero} (la primera es la 1). */
    public String linea(long numero) {
        return lineas.get((int) numero - 1);
    }

    /** El título del último header escrito antes del método, si lo hay. */
    public Optional<String> bloqueDe(Metodo metodo) {
        return headers.stream()
                .filter(header -> header.linea() < metodo.linea())
                .max(Comparator.comparingLong(Header::linea))
                .map(Header::titulo);
    }

    /**
     * La interfaz {@code <Entidad>Service} hermana de este {@code <Entidad>ServiceImpl} o
     * {@code <Entidad>Controller}: la de la carpeta {@code service} de su mismo sistema o
     * subsistema. Vacío si no existe o si no es una interfaz.
     */
    public Optional<FuenteJava> interfazDelServicio(String sufijoPropio) {
        String fichero = path.getFileName().toString();
        String entidad = fichero.substring(0, fichero.length() - sufijoPropio.length());
        Path carpeta = path.getParent();
        Path carpetaService = carpeta.getFileName().toString().equals("impl") ? carpeta.getParent() : carpeta.resolveSibling("service");
        Path candidata = carpetaService.resolve(entidad + "Service.java");
        if (!Files.exists(candidata)) {
            return Optional.empty();
        }

        return Optional.of(leer(candidata)).filter(FuenteJava::esInterfaz);
    }

    private static List<Metodo> leerMetodos(ClassTree tipo, CompilationUnitTree unit, SourcePositions positions, boolean interfaz) {
        LineMap lineMap = unit.getLineMap();
        List<Metodo> metodos = new ArrayList<>();
        for (Tree miembro : tipo.getMembers()) {
            if (!(miembro instanceof MethodTree metodo) || metodo.getReturnType() == null) {
                continue;
            }
            Set<Modifier> modificadores = metodo.getModifiers().getFlags();
            boolean accesible = interfaz || modificadores.contains(Modifier.PUBLIC) || modificadores.contains(Modifier.PROTECTED);
            metodos.add(new Metodo(metodo.getName().toString(), accesible, metodo.getReturnType().toString(),
                    lineMap.getLineNumber(positions.getStartPosition(unit, metodo)),
                    lineMap.getLineNumber(positions.getEndPosition(unit, metodo)),
                    leerLlamadas(metodo, unit, positions)));
        }

        return metodos;
    }

    private static List<String> leerLlamadas(MethodTree metodo, CompilationUnitTree unit, SourcePositions positions) {
        TreeMap<Long, String> llamadas = new TreeMap<>();
        new TreeScanner<Void, Void>() {
            @Override
            public Void visitMethodInvocation(MethodInvocationTree invocacion, Void unused) {
                if (invocacion.getMethodSelect() instanceof MemberSelectTree seleccion) {
                    llamadas.put(positions.getEndPosition(unit, seleccion), seleccion.getIdentifier().toString());
                }
                return super.visitMethodInvocation(invocacion, null);
            }
        }.scan(metodo.getBody(), null);

        return List.copyOf(llamadas.values());
    }

    private static List<Header> leerHeaders(List<String> lineas) {
        List<Header> headers = new ArrayList<>();
        int i = 0;
        while (i < lineas.size()) {
            if (!LINEA_DE_HEADER.matcher(lineas.get(i)).matches()) {
                i++;
                continue;
            }
            int inicio = i;
            while (i < lineas.size() && LINEA_DE_HEADER.matcher(lineas.get(i)).matches()) {
                i++;
            }
            List<String> grupo = lineas.subList(inicio, i);
            boolean bienFormado = grupo.size() == 3
                    && titulo(grupo.get(0)).isEmpty() && !titulo(grupo.get(1)).isEmpty() && titulo(grupo.get(2)).isEmpty()
                    && grupo.stream().map(String::length).distinct().count() == 1;
            String titulo = grupo.stream().map(FuenteJava::titulo).filter(t -> !t.isEmpty()).findFirst().orElse("");
            headers.add(new Header(titulo, inicio + 1L, bienFormado));
        }

        return headers;
    }

    private static String titulo(String lineaDeHeader) {
        return lineaDeHeader.replace('*', ' ').replace('/', ' ').trim();
    }
}
