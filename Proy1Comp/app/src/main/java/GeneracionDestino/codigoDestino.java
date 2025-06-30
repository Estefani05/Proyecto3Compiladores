package GeneracionDestino;

import java.util.*;
import java.util.regex.*;


public class codigoDestino {

    private List<String> intermedio;
    private List<String> dataSection;
    private List<String> textSection;
    private Map<String, String> tipoTemporal;
    private Map<String, String> registroTemporal;
    private Deque<String> registrosEnterosDisponibles;
    private Deque<String> registrosFlotantesDisponibles;
    private int contadorStrings = 0;
    private  HashMap<String, ArrayList<String>> tablaSimbolos;
    private Map<String, Map<String, String>> parametrosPorFuncion = new HashMap<>();
    private String funcionActual = null;
    private String ultimaVariableAsignada = null;

    // Para análisis de vida: temporal -> [primeraLinea, ultimaLinea]
    private Map<String, int[]> vidaTemporales;

    // Patrones precompilados
    private static final Pattern enteroPattern = Pattern.compile("^(t\\d+) = (\\d+);$");
    private static final Pattern booleanPattern = Pattern.compile("^(t\\d+) = (true|false);$");
    private static final Pattern charPattern = Pattern.compile("^(t\\d+) = '(.)';$");
    private static final Pattern stringPattern = Pattern.compile("^(t\\d+) = \"(.*)\";$");
    private static final Pattern idPattern = Pattern.compile("^(t\\d+) = ([a-zA-Z_][a-zA-Z0-9_]*);$");
    private static final Pattern tempPattern = Pattern.compile("t\\d+");
    private static final Pattern declaracionGlobalPattern = Pattern.compile("^declaracion_global_\\d+:\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*(t\\d+);$");
    private static final Pattern declaracionPattern = Pattern.compile("^declaracion_\\d+: ([a-zA-Z_][a-zA-Z0-9_]*) = (t\\d+);$");
    private static final Pattern operacionAritPattern = Pattern.compile("^(t\\d+) = (t\\d+) ([+\\-*/^]) (t\\d+);$");
    private static final Pattern operacionBinariaPattern = Pattern.compile("^(t\\d+) = (t\\d+) (&&|\\|\\|) (t\\d+);$");
    private static final Pattern declaracionArray1DPattern = Pattern.compile("^declaracion_array_\\d+: ([a-zA-Z_][a-zA-Z0-9_]*) = new int\\[(t\\d+)\\];$");
    private static final Pattern declaracionArray2DPattern = Pattern.compile("^declaracion_array_\\d+: ([a-zA-Z_][a-zA-Z0-9_]*) = new int\\[(t\\d+)\\]\\[(t\\d+)\\];$");
    private static final Pattern accesoArray1DPattern = Pattern.compile("^(t\\d+) = ([a-zA-Z_][a-zA-Z0-9_]*)\\[(t\\d+)\\];$");
    private static final Pattern accesoArray2DPattern = Pattern.compile("^(t\\d+) = ([a-zA-Z_][a-zA-Z0-9_]*)\\[(t\\d+)\\]\\[(t\\d+)\\];$");
    private static final Pattern asignacionArray1DPattern = Pattern.compile("^asig_array_\\d+: ([a-zA-Z_][a-zA-Z0-9_]*)\\[(t\\d+)\\] = (t\\d+);$");
    private static final Pattern asignacionArray2DPattern = Pattern.compile("^asig_array_\\d+: ([a-zA-Z_][a-zA-Z0-9_]*)\\[(t\\d+)\\]\\[(t\\d+)\\] = (t\\d+);$");
    private static final Pattern leerPattern = Pattern.compile("^leer_\\d+: leer ([a-zA-Z_][a-zA-Z0-9_]*);$");
    private static final Pattern printPattern = Pattern.compile("^llamada_\\d+: t\\d+ = call print\\(\\[(t\\d+)\\]\\);$");
    private static final Pattern asignacionVarPattern = Pattern.compile("^asig_\\d+: ([a-zA-Z_][a-zA-Z0-9_]*) = (t\\d+);$");
    private static final Pattern relacionalPattern = Pattern.compile("^(t\\d+) = (t\\d+) (>|<|>=|<=|==|!=) (t\\d+);$");
    private static final Pattern ifGotoPattern = Pattern.compile("^if \\((t\\d+)\\) goto ([a-zA-Z0-9_]+);$");
    private static final Pattern llamadaFuncionPattern = Pattern.compile("^llamada_\\d+: (t\\d+) = call (\\w+)\\(\\[(.*?)\\]\\);$");
    private static final Pattern returnPattern = Pattern.compile("^return_\\d+: return (t\\d+);$");
    private static final Pattern gotoPattern = Pattern.compile("^goto ([a-zA-Z_][a-zA-Z0-9_]*);$");
    private static final Pattern parametrosFuncionPattern = Pattern.compile("^Parametros_funcion_(\\w+): \\[(.*)\\]$");
    private static final Pattern inicioFor = Pattern.compile("^INICIO_for_(\\d+):$");
    private static final Pattern incrementoFor = Pattern.compile("^INCREMENTO_for_ \\((t\\d+)\\)$");
    private static final Pattern incrementoForPattern = Pattern.compile("^INCREMENTO_for_ \\((t\\d+)\\)$");

    public codigoDestino(String rutaArchivo, Map<String, String> tiposCI,  HashMap<String, ArrayList<String>> tablaSimbolos) {
        this.intermedio = cargarCodigoIntermedio(rutaArchivo);
        this.tipoTemporal = tiposCI;
        this.dataSection = new ArrayList<>();
        this.textSection = new ArrayList<>();
        this.registroTemporal = new HashMap<>();
        this.registrosEnterosDisponibles = new ArrayDeque<>();
        this.registrosFlotantesDisponibles = new ArrayDeque<>();
        this.vidaTemporales = new HashMap<>();
        this.tablaSimbolos = tablaSimbolos;

        // Inicializar registros enteros $t0 - $t9
        for (int i = 0; i <= 9; i++) {
            registrosEnterosDisponibles.add("$t" + i);
        }

        // Inicializar registros flotantes $f0 - $f31 
        for (int i = 0; i <= 31; i++) {
            registrosFlotantesDisponibles.add("$f" + i);
        }
    }

    private List<String> cargarCodigoIntermedio(String ruta) {
        List<String> lineas = new ArrayList<>();
        try (Scanner scanner = new Scanner(new java.io.File(ruta))) {
            while (scanner.hasNextLine()) {
                lineas.add(scanner.nextLine());
            }
        } catch (Exception e) {
            System.err.println(" Error al leer el archivo de código intermedio: " + e.getMessage());
        }
        return lineas;
    }

    public String generarMIPS() {
        // 1. Analizar vida de temporales en todo el código intermedio
        analizarVidaTemporales();

        // 2. Traducir línea por línea y liberar registros cuando corresponda
        for (int i = 0; i < intermedio.size(); i++) {
            String linea = intermedio.get(i).trim();
            procesarLinea(linea);

            // Liberar registros que terminan su vida en esta línea
            liberarTemporalesEnLinea(i);
        }

        // Construir salida MIPS
        StringBuilder sb = new StringBuilder();
        sb.append(".data\n");
        for (String d : dataSection) {
            sb.append(d).append("\n");
        }

        sb.append("\n.text\n.globl main\nmain:\n");
        for (String t : textSection) {
            sb.append(t).append("\n");
        }

        // syscall para terminar el programa
        sb.append("li $v0, 10      # Exit syscall\n");
        sb.append("syscall\n");

        String codigoFinal = sb.toString();
        guardarCodigoMIPS(codigoFinal, "salida_mips.s");

        return codigoFinal;
    }

    private void guardarCodigoMIPS(String codigo, String nombreArchivo) {
        try (java.io.PrintWriter writer = new java.io.PrintWriter(nombreArchivo)) {
            writer.write(codigo);
            System.out.println(" Código MIPS guardado en: " + nombreArchivo);
        } catch (Exception e) {
            System.err.println(" Error al guardar el archivo: " + e.getMessage());
        }
    }

    private void analizarVidaTemporales() {
        for (int i = 0; i < intermedio.size(); i++) {
            String linea = intermedio.get(i);
            Matcher matcher = tempPattern.matcher(linea);
            while (matcher.find()) {
                String temp = matcher.group();
                if (!vidaTemporales.containsKey(temp)) {
                    vidaTemporales.put(temp, new int[]{i, i});
                } else {
                    int[] rango = vidaTemporales.get(temp);
                    if (i > rango[1]) {
                        rango[1] = i;
                        vidaTemporales.put(temp, rango);
                    }
                }
            }
        }
    }

    private void liberarTemporalesEnLinea(int lineaActual) {
        Iterator<Map.Entry<String, int[]>> it = vidaTemporales.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, int[]> entry = it.next();
            String temp = entry.getKey();
            int[] rango = entry.getValue();
            if (rango[1] == lineaActual) {
                liberarRegistro(temp);
                it.remove();
            }
        }
    }

    private void procesarLinea(String linea) {
        if (linea.contains("error")) {
            System.err.println("  Línea con error: " + linea);
            return;
        }
        if (linea.contains("sin_inicializar")) {
            System.err.println(" Línea con valor no inicializado: " + linea);
            return;
        }

        if (linea.startsWith("INICIO_funcion_")) {
            String nombreFuncion = linea.substring("INICIO_funcion_".length());
            if (nombreFuncion.endsWith(":")) {
                nombreFuncion = nombreFuncion.substring(0, nombreFuncion.length() - 1);
            }
            funcionActual = nombreFuncion;
            textSection.add(linea);
            return;
        }
        
        // Detectar fin de función
        if (linea.startsWith("FIN_funcion_")) {
            funcionActual = null;
            return;
        }

        if (linea.startsWith("FIN_main")) {
            textSection.add(linea);
            
            if (linea.equals("FIN_main:")) {
                textSection.add("li $v0, 10");
                textSection.add("syscall");
            }

            return;
        }

        if (linea.endsWith(":")) {
            // Agrega etiqueta directamente
            textSection.add(linea);
            return;
        }

        Matcher m;

        m = enteroPattern.matcher(linea);
        if (m.matches()) {
            String temp = m.group(1);
            String valor = m.group(2);
            traducirEntero(temp, valor);
            return;
        }

        m = booleanPattern.matcher(linea);
        if (m.matches()) {
            String temp = m.group(1);
            boolean valorBool = m.group(2).equals("true");
            traducirBooleano(temp, valorBool);
            return;
        }

        m = charPattern.matcher(linea);
        if (m.matches()) {
            String temp = m.group(1);
            char valor = m.group(2).charAt(0);
            traducirChar(temp, valor);
            return;
        }

        m = stringPattern.matcher(linea);
        if (m.matches()) {
            String temp = m.group(1);
            String valor = m.group(2);
            traducirString(temp, valor);
            return;
        }

        m = idPattern.matcher(linea);
        if (m.matches()) {
            String temp = m.group(1);
            String var = m.group(2);
            traducirIdentificador(temp, var);
            return;
        }

        m = declaracionGlobalPattern.matcher(linea);
        if (m.matches()) {
            String variable = m.group(1);
            String temp = m.group(2);
            traducirDeclaracionGlobal(variable, temp);
            return;
        }

        m = operacionAritPattern.matcher(linea);
        if (m.matches()) {
            String temp = m.group(1);
            String op1 = m.group(2);
            String operador = m.group(3);
            String op2 = m.group(4);
            traducirOperacionAritmetica(temp, op1, operador, op2);
            return;
        }

        m = declaracionPattern.matcher(linea);
        if (m.matches()) {
            String var = m.group(1);
            String temp = m.group(2);
            traducirDeclaracion(var, temp);
            return;
        }

        m = operacionBinariaPattern.matcher(linea);
        if (m.matches()) {
            String res = m.group(1);
            String op1 = m.group(2);
            String op = m.group(3);
            String op2 = m.group(4);
            traducirOperacionLogica(res, op1, op, op2);
            return;
        }

        m = declaracionArray2DPattern.matcher(linea);
        if (m.matches()) {
            String var = m.group(1);
            String dim1 = m.group(2);
            String dim2 = m.group(3);
            traducirDeclaracionArray2D(var, dim1, dim2);
            return;
        }

        m = declaracionArray1DPattern.matcher(linea);
        if (m.matches()) {
            String var = m.group(1);
            String tam = m.group(2);
            traducirDeclaracionArray1D(var, tam);
            return;
        }

        m = leerPattern.matcher(linea);
        if (m.matches()) {
            String var = m.group(1);
            traducirLectura(var);
            return;
        }

        m = printPattern.matcher(linea);
        if (m.matches()) {
            String tempArg = m.group(1);
            traducirPrint(tempArg);
            return;
        }

        m = asignacionVarPattern.matcher(linea);
        if (m.matches()) {
            String var = m.group(1);  
            String temp = m.group(2);  
            traducirAsignacionVar(var, temp);
            return;
        }

        m = relacionalPattern.matcher(linea);
        if (m.matches()) {
            String dest = m.group(1);
            String op1 = m.group(2);
            String operador = m.group(3);
            String op2 = m.group(4);
            traducirRelacional(dest, op1, op2, operador);
            return;
        }

        m = ifGotoPattern.matcher(linea);
        if (m.matches()) {
            String temp = m.group(1);       
            String etiqueta = m.group(2);   
            traducirIfGoto(temp, etiqueta);
            return;
        }

        m = llamadaFuncionPattern.matcher(linea);
        if (m.matches()) {
            String destino = m.group(1); 
            String funcion = m.group(2); 
            String argumentos = m.group(3); 
            traducirLlamadaFuncion(destino, funcion, argumentos);
            return;
        }

        m = returnPattern.matcher(linea);
        if (m.matches()) {
            String tempRetorno = m.group(1);
            traducirReturn(tempRetorno);
            return;
        }

        m = gotoPattern.matcher(linea);
        if (m.matches()) {
            String etiqueta = m.group(1);
            traducirGoto(etiqueta);
            return;
        }

        m = parametrosFuncionPattern.matcher(linea);
        if (m.matches()) {
            String nombreFuncion = m.group(1);
            String paramsStr = m.group(2).trim();
            traducirParametrosFuncion(nombreFuncion, paramsStr);
            return;
        }

        m = incrementoForPattern.matcher(linea);
        if (m.matches()) {
            String temp = m.group(1);
            traducirIncrementoFor(temp);
            return;
        }

        m = accesoArray1DPattern.matcher(linea);
        if (m.matches()) {
            String destino = m.group(1);
            String array = m.group(2);
            String indice = m.group(3);
            traducirAccesoArray1D(destino, array, indice);
            return;
        }

        // Acceso a array 2D: t1 = array[t2][t3];
        m = accesoArray2DPattern.matcher(linea);
        if (m.matches()) {
            String destino = m.group(1);
            String array = m.group(2);
            String indice1 = m.group(3);
            String indice2 = m.group(4);
            traducirAccesoArray2D(destino, array, indice1, indice2);
            return;
        }

        m = asignacionArray1DPattern.matcher(linea);
        if (m.matches()) {
            String array = m.group(1);
            String indice = m.group(2);
            String valor = m.group(3);
            traducirAsignacionArray1D(array, indice, valor);
            return;
        }

        m = asignacionArray2DPattern.matcher(linea);
        if (m.matches()) {
            String array = m.group(1);
            String indice1 = m.group(2);
            String indice2 = m.group(3);
            String valor = m.group(4);
            traducirAsignacionArray2D(array, indice1, indice2, valor);
            return;
        }


        System.err.println("  No se reconoce patrón para línea: " + linea);
    }

    private void traducirEntero(String temp, String valor) {
        asignarRegistro(temp);
        textSection.add("li " + registroTemporal.get(temp) + ", " + valor);
    }

    private void traducirBooleano(String temp, boolean valorBool) {
        asignarRegistro(temp);
        textSection.add("li " + registroTemporal.get(temp) + ", " + (valorBool ? "1" : "0"));
    }

    private void traducirChar(String temp, char valor) {
        int ascii = (int) valor;
        asignarRegistro(temp);
        textSection.add("li " + registroTemporal.get(temp) + ", " + ascii);
    }

    private void traducirString(String temp, String valor) {
        String etiqueta = "str_" + (++contadorStrings);
        dataSection.add(etiqueta + ": .asciiz \"" + valor + "\"");

        asignarRegistro(temp);
        textSection.add("la " + registroTemporal.get(temp) + ", " + etiqueta); 
    }

    private void traducirIdentificador(String temp, String var) {
        asignarRegistro(temp);
        
        // Verificar si es parámetro de función actual
        if (funcionActual != null && 
            parametrosPorFuncion.containsKey(funcionActual) &&
            parametrosPorFuncion.get(funcionActual).containsKey(var)) {
            
            String regParam = parametrosPorFuncion.get(funcionActual).get(var);
            textSection.add("move " + registroTemporal.get(temp) + ", " + regParam);
        } else {
            // Variable global
            textSection.add("lw " + registroTemporal.get(temp) + ", " + var);
        }
    }

    private void traducirDeclaracionGlobal(String variable, String temp) {
        asignarRegistro(temp);

        // En la sección de datos se debe declarar la variable global (si no existe)
        if (!dataSection.stream().anyMatch(line -> line.startsWith(variable + ":"))) {
            dataSection.add(variable + ": .word 0");
        }

        textSection.add("sw " + registroTemporal.get(temp) + ", " + variable);
    }

    private void traducirOperacionAritmetica(String temp, String op1, String operador, String op2) {
        asignarRegistro(temp);
        asignarRegistro(op1);
        asignarRegistro(op2);
        
        String regTemp = registroTemporal.get(temp);
        String regOp1 = registroTemporal.get(op1);
        String regOp2 = registroTemporal.get(op2);
        
        String instruccion;
        boolean instruccionGenerada = true;
        switch (operador) {
            case "+":
                instruccion = "add " + regTemp + ", " + regOp1 + ", " + regOp2;
                break;
            case "-":
                instruccion = "sub " + regTemp + ", " + regOp1 + ", " + regOp2;
                break;
            case "*":
                instruccion = "mul " + regTemp + ", " + regOp1 + ", " + regOp2;
                break;
            case "/":
                instruccion = "div " + regOp1 + ", " + regOp2;
                textSection.add(instruccion);
                instruccion = "mflo " + regTemp;
                break;
            case "^": 
                String etiquetaInicio = "POTENCIA_LOOP_" + contadorStrings++;
                String etiquetaFin = "FIN_POTENCIA_" + contadorStrings++;
                
                // Asignar registros temporales para el algoritmo
                String regContador = obtenerRegistroTemporal();
                String regBase = obtenerRegistroTemporal();
                
                // Manejar caso especial: exponente = 0
                String etiquetaCero = "POTENCIA_CERO_" + contadorStrings++;
                textSection.add("beqz " + regOp2 + ", " + etiquetaCero);
                
                // Inicializar valores
                textSection.add("li " + regTemp + ", 1");           // resultado = 1
                textSection.add("move " + regContador + ", " + regOp2);  // contador = exponente
                textSection.add("move " + regBase + ", " + regOp1);      // base = op1
                
                // Bucle principal
                textSection.add(etiquetaInicio + ":");
                textSection.add("blez " + regContador + ", " + etiquetaFin);
                textSection.add("mul " + regTemp + ", " + regTemp + ", " + regBase);
                textSection.add("addi " + regContador + ", " + regContador + ", -1");
                textSection.add("j " + etiquetaInicio);
                
                // Caso exponente = 0
                textSection.add(etiquetaCero + ":");
                textSection.add("li " + regTemp + ", 1");
                textSection.add("j " + etiquetaFin);
                
                textSection.add(etiquetaFin + ":");
                
                // Liberar registros temporales
                liberarRegistroTemporal(regContador);
                liberarRegistroTemporal(regBase);
                
                instruccionGenerada = false;
                return;
            default:
                throw new RuntimeException("Operador no soportado: " + operador);
        }
        
        if (instruccionGenerada) {
            textSection.add(instruccion);
        }
    }

    private Set<String> variablesDeclaradas = new HashSet<>();

    private void traducirDeclaracion(String var, String temp) {
        if (!variablesDeclaradas.contains(var)) {
            variablesDeclaradas.add(var);

            String tipo = obtenerTipo(var); 
            if (tipo.equals("string")) {
                dataSection.add(var + ": .word 0"); // puntero a string
            } else {
                dataSection.add(var + ": .word 0"); // otros tipos
            }
        }

        asignarRegistro(temp);
        String regTemp = registroTemporal.get(temp);
        textSection.add("sw " + regTemp + ", " + var);
    }

    private void traducirOperacionLogica(String resultado, String op1, String operador, String op2) {
        asignarRegistro(op1);
        asignarRegistro(op2);
        asignarRegistro(resultado);

        String regRes = registroTemporal.get(resultado);
        String regOp1 = registroTemporal.get(op1);
        String regOp2 = registroTemporal.get(op2);

        
        if (operador.equals("&&")) {
            textSection.add("and " + regRes + ", " + regOp1 + ", " + regOp2);
        } else if (operador.equals("||")) {
            textSection.add("or " + regRes + ", " + regOp1 + ", " + regOp2);
        }
    }

    private void traducirDeclaracionArray1D(String var, String tamTemp) {
        asignarRegistro(tamTemp);
        String regTam = registroTemporal.get(tamTemp);

        // Guardar el tamaño del array para uso posterior
        dataSection.add(var + "_size: .word 0");
        textSection.add("sw " + regTam + ", " + var + "_size");

        // Tamaño total en bytes (4 por entero)
        textSection.add("# Reservar memoria para arreglo 1D " + var);
        textSection.add("mul $a0, " + regTam + ", 4");
        textSection.add("li $v0, 9");       // syscall 9 = sbrk
        textSection.add("syscall");
        
        // Asignar registro para el array
        String regArray = obtenerRegistroDisponible();
        textSection.add("move " + regArray + ", $v0");   // Dirección base
        registroTemporal.put(var, regArray);
    }

    private void traducirDeclaracionArray2D(String var, String dim1Temp, String dim2Temp) {
        asignarRegistro(dim1Temp);
        asignarRegistro(dim2Temp);
        String r1 = registroTemporal.get(dim1Temp);
        String r2 = registroTemporal.get(dim2Temp);

        // Guardar las dimensiones del array para uso posterior
        dataSection.add(var + "_rows: .word 0");
        dataSection.add(var + "_cols: .word 0");
        textSection.add("sw " + r1 + ", " + var + "_rows");
        textSection.add("sw " + r2 + ", " + var + "_cols");

        // Calcular total de elementos: filas * columnas
        textSection.add("# Reservar memoria para arreglo 2D " + var);
        textSection.add("mul $t1, " + r1 + ", " + r2); // t1 = filas * columnas
        textSection.add("mul $a0, $t1, 4");            // bytes = elementos * 4
        textSection.add("li $v0, 9");                  // syscall malloc
        textSection.add("syscall");
        
        // Asignar registro para el array
        String regArray = obtenerRegistroDisponible();
        textSection.add("move " + regArray + ", $v0");
        registroTemporal.put(var, regArray);
    }

    private String obtenerTipo(String id) {
        if (tipoTemporal.containsKey(id)) return tipoTemporal.get(id);
        ArrayList<String> atributos = tablaSimbolos.get(id);
        return (atributos != null && !atributos.isEmpty()) ? atributos.get(0) : "int";
    }

    private void traducirLectura(String var) {
        // Intentamos obtener el tipo primero de temporales
        String tipo = obtenerTipo(var);

        switch (tipo) {
            case "int":
                textSection.add("li $v0, 5       # syscall para leer int");
                textSection.add("syscall");
                textSection.add("sw $v0, " + var);
                break;

            case "float":
                textSection.add("li $v0, 6       # syscall para leer float");
                textSection.add("syscall");
                textSection.add("s.s $f0, " + var);
                break;

            case "char":
                textSection.add("li $v0, 12      # syscall para leer char");
                textSection.add("syscall");
                textSection.add("sb $v0, " + var);
                break;

            default:
                System.err.println(" Tipo no soportado o no reconocido en lectura: '" + tipo + "' para variable '" + var + "'");
        }
    }

    private void traducirPrint(String temp) {
        // Buscar tipo en temporales o variables
        String tipo = obtenerTipo(temp);

        String reg = registroTemporal.get(temp);

        switch (tipo) {
            case "int":
            case "bool":  // bool representado como int
                textSection.add("move $a0, " + reg);
                textSection.add("li $v0, 1       # print int");
                textSection.add("syscall");
                break;

            case "float":
                textSection.add("mov.s $f12, " + reg);
                textSection.add("li $v0, 2       # print float");
                textSection.add("syscall");
                break;

            case "char":
                textSection.add("move $a0, " + reg);
                textSection.add("li $v0, 11      # print char");
                textSection.add("syscall");
                break;

            case "string":
                textSection.add("move $a0, " + reg);
                textSection.add("li $v0, 4       # print string");
                textSection.add("syscall");
                break;

            default:
                System.err.println(" Tipo no soportado para print: '" + tipo + "' de temp/var '" + temp + "'");
        }

        textSection.add("li $a0, 10      # ASCII newline");
        textSection.add("li $v0, 11      # print char");
        textSection.add("syscall");
    }

    private void traducirAsignacionVar(String var, String temp) {
        asignarRegistro(temp);
        String regTemp = registroTemporal.get(temp);
        ultimaVariableAsignada = var;
        
        // Verificar si es parámetro de función actual
        if (funcionActual != null && 
            parametrosPorFuncion.containsKey(funcionActual) &&
            parametrosPorFuncion.get(funcionActual).containsKey(var)) {
            
            String regParam = parametrosPorFuncion.get(funcionActual).get(var);
            textSection.add("move " + regParam + ", " + regTemp);
            textSection.add("# Asignando a parámetro " + var + " en " + regParam);
        } else {
            // Variable global
            textSection.add("sw " + regTemp + ", " + var);
            textSection.add("# Asignación: " + var + " = " + temp);
        }
    }
    private void traducirRelacional(String dest, String op1, String op2, String operador) {
        asignarRegistro(dest);
        asignarRegistro(op1);
        asignarRegistro(op2);

        switch (operador) {
            case ">":
                // op1 > op2  => slt dest, op2, op1
                textSection.add("slt " + registroTemporal.get(dest) + ", " + registroTemporal.get(op2) + ", " + registroTemporal.get(op1));
                break;
            case "<":
                // op1 < op2  => slt dest, op1, op2
                textSection.add("slt " + registroTemporal.get(dest) + ", " + registroTemporal.get(op1) + ", " + registroTemporal.get(op2));
                break;
            case ">=":
                // op1 >= op2  => not(op1 < op2)
                textSection.add("slt " + registroTemporal.get(dest) + ", " + registroTemporal.get(op1) + ", " + registroTemporal.get(op2));
                textSection.add("xori " + registroTemporal.get(dest) + ", " + registroTemporal.get(dest) + ", 1");
                break;
            case "<=":
                // op1 <= op2  => not(op1 > op2)
                textSection.add("slt " + registroTemporal.get(dest) + ", " + registroTemporal.get(op2) + ", " + registroTemporal.get(op1));
                textSection.add("xori " + registroTemporal.get(dest) + ", " + registroTemporal.get(dest) + ", 1");
                break;
            case "==":
                // op1 == op2  => seq dest, op1, op2 (no existe en MIPS, se simula)
                textSection.add("xor " + registroTemporal.get(dest) + ", " + registroTemporal.get(op1) + ", " + registroTemporal.get(op2));
                textSection.add("sltiu " + registroTemporal.get(dest) + ", " + registroTemporal.get(dest) + ", 1");
                break;
            case "!=":
                // op1 != op2 => xor dest, op1, op2; sltu dest, $zero, dest
                textSection.add("xor " + registroTemporal.get(dest) + ", " + registroTemporal.get(op1) + ", " + registroTemporal.get(op2));
                textSection.add("sltu " + registroTemporal.get(dest) + ", $zero, " + registroTemporal.get(dest));
                break;
            default:
                System.err.println(" Operador relacional no soportado: " + operador);
        }
    }

    private void traducirIfGoto(String temp, String etiqueta) {
        String reg = registroTemporal.get(temp);
        if (reg == null) {
            throw new RuntimeException(" Registro no asignado para " + temp);
        }
        // Branch if not equal to zero
        textSection.add("bne " + reg + ", $zero, " + etiqueta);
    }

    private void traducirLlamadaFuncion(String destino, String funcion, String argsStr) {
        // Guardar $ra en el stack antes de jal
        textSection.add("addi $sp, $sp, -4");
        textSection.add("sw $ra, 0($sp)");

        // Procesar argumentos
        if (!argsStr.trim().isEmpty()) {
            String[] args = argsStr.split(",");
            for (int i = 0; i < args.length && i < 4; i++) {
                String tempArg = args[i].trim();
                procesarArgumento(tempArg, i);
            }
        }

        textSection.add("jal INICIO_funcion_" + funcion);

        // Restaurar $ra del stack después de jal
        textSection.add("lw $ra, 0($sp)");
        textSection.add("addi $sp, $sp, 4");

        // Asignar el resultado al temporal destino
        asignarRegistro(destino);
        String tipoDestino = obtenerTipo(destino);
        
        if (tipoDestino.equals("float")) {
            textSection.add("mov.s " + registroTemporal.get(destino) + ", $f0");
        } else {
            textSection.add("move " + registroTemporal.get(destino) + ", $v0");
        }
    }

    private void traducirIncrementoFor(String temp) {
        asignarRegistro(temp);
        String regTemp = registroTemporal.get(temp);

        if (ultimaVariableAsignada != null) {
            textSection.add("# Incremento de variable en FOR");
            textSection.add("sw " + regTemp + ", " + ultimaVariableAsignada);
        } else {
            System.err.println("No se ha registrado la variable de control del for.");
        }
    }



    // Método auxiliar para buscar etiqueta de literal string en dataSection
    private String buscarEtiquetaLiteral(String literal) {
        String literalSinComillas = literal.substring(1, literal.length() - 1);
        for (String dataLine : dataSection) {
            if (dataLine.contains(".asciiz")) {
                // Buscar exactamente el contenido
                String patron = "\"" + Pattern.quote(literalSinComillas) + "\"";
                if (dataLine.matches(".*" + patron + ".*")) {
                    return dataLine.split(":")[0].trim();
                }
            }
        }
        return null;
    }

    private void traducirReturn(String temp) {
        // Obtener el tipo de retorno esperado
        String tipoRetorno = obtenerTipo(temp);
        
        // Verificar si el temporal tiene un registro asignado
        String reg = registroTemporal.get(temp);
        if (reg == null) {
            System.err.println("Registro no asignado para temporal de retorno: " + temp);
            
            // Intentar cargar el valor si es una variable
            if (tablaSimbolos.containsKey(temp)) {
                textSection.add("lw $v0, " + temp + "   # cargar valor de retorno");
            } else {
                textSection.add("li $v0, 0              # valor de retorno por defecto");
            }
        } else {
            // Mover el valor al registro de retorno
            if (tipoRetorno.equals("float")) {
                textSection.add("mov.s $f0, " + reg + "   # valor de retorno float");
            } else {
                textSection.add("move $v0, " + reg + "   # valor de retorno");
            }
        }
        
        textSection.add("jr $ra                  # regresar");
    }

    private void traducirGoto(String etiqueta) {
        textSection.add("j " + etiqueta + "   # salto incondicional");
    }

    private void traducirParametrosFuncion(String nombreFuncion, String paramsStr) {
        if (paramsStr.isEmpty()) return;
        
        Map<String, String> parametrosLocales = new HashMap<>();
        String[] params = paramsStr.split(",");
        
        for (int i = 0; i < params.length; i++) {
            String p = params[i].trim();
            String[] partes = p.split("_", 2);
            if (partes.length != 2) continue;
            
            String tipo = partes[0];
            String nombre = partes[1];
            
            parametrosLocales.put(nombre, "$a" + i);
            
            textSection.add("# Parámetro " + nombre + " recibido en $a" + i);
        }
        

        parametrosPorFuncion.put(nombreFuncion, parametrosLocales);
    }

    private void traducirAccesoArray1D(String destino, String array, String indice) {
        asignarRegistro(destino);
        asignarRegistro(indice);
        
        String regDestino = registroTemporal.get(destino);
        String regIndice = registroTemporal.get(indice);
        String regArray = registroTemporal.get(array);
        
        if (regArray == null) {
            throw new RuntimeException("Array " + array + " no tiene registro asignado");
        }
        
        textSection.add("# Acceso a array 1D: " + destino + " = " + array + "[" + indice + "]");
        textSection.add("sll $t9, " + regIndice + ", 2    # índice * 4 (tamaño de int)");
        textSection.add("add $t9, " + regArray + ", $t9   # dirección base + offset");
        textSection.add("lw " + regDestino + ", 0($t9)    # cargar valor del array");
    }

    private void traducirAccesoArray2D(String destino, String array, String indice1, String indice2) {
        asignarRegistro(destino);
        asignarRegistro(indice1);
        asignarRegistro(indice2);
        
        String regDestino = registroTemporal.get(destino);
        String regIndice1 = registroTemporal.get(indice1);
        String regIndice2 = registroTemporal.get(indice2);
        String regArray = registroTemporal.get(array);
        
        if (regArray == null) {
            throw new RuntimeException("Array 2D " + array + " no tiene registro asignado");
        }
        
        textSection.add("# Acceso a array 2D: " + destino + " = " + array + "[" + indice1 + "][" + indice2 + "]");
        
        // Necesitamos las dimensiones del array para calcular el offset
        // Fórmula: offset = (fila * columnas + columna) * 4
        
        textSection.add("# Calculando offset para array 2D");
        textSection.add("# Asumiendo que las dimensiones son iguales (cuadrado)");
        
        // Obtener las dimensiones guardadas del array 
        String regColumnas = obtenerRegistroTemporal();
        textSection.add("lw " + regColumnas + ", " + array + "_cols  # cargar número de columnas");
        
        textSection.add("mul $t8, " + regIndice1 + ", " + regColumnas + "  # fila * columnas");
        textSection.add("add $t8, $t8, " + regIndice2 + "               # + columna");
        textSection.add("sll $t8, $t8, 2                              # * 4 bytes");
        textSection.add("add $t8, " + regArray + ", $t8               # dirección base + offset");
        textSection.add("lw " + regDestino + ", 0($t8)                # cargar valor");
        
        liberarRegistroTemporal(regColumnas);
    }

    private void traducirAsignacionArray1D(String array, String indice, String valor) {
        asignarRegistro(indice);
        asignarRegistro(valor);
        
        String regIndice = registroTemporal.get(indice);
        String regValor = registroTemporal.get(valor);
        String regArray = registroTemporal.get(array);
        
        if (regArray == null) {
            throw new RuntimeException("Array " + array + " no tiene registro asignado");
        }
        
        textSection.add("# Asignación a array 1D: " + array + "[" + indice + "] = " + valor);
        textSection.add("sll $t9, " + regIndice + ", 2    # índice * 4");
        textSection.add("add $t9, " + regArray + ", $t9   # dirección base + offset");
        textSection.add("sw " + regValor + ", 0($t9)      # guardar valor en array");
    }

    private void traducirAsignacionArray2D(String array, String indice1, String indice2, String valor) {
        asignarRegistro(indice1);
        asignarRegistro(indice2);
        asignarRegistro(valor);
        
        String regIndice1 = registroTemporal.get(indice1);
        String regIndice2 = registroTemporal.get(indice2);
        String regValor = registroTemporal.get(valor);
        String regArray = registroTemporal.get(array);
        
        if (regArray == null) {
            throw new RuntimeException("Array 2D " + array + " no tiene registro asignado");
        }
        
        textSection.add("# Asignación a array 2D: " + array + "[" + indice1 + "][" + indice2 + "] = " + valor);
        
        // Calcular offset usando las dimensiones del array
        String regColumnas = obtenerRegistroTemporal();
        textSection.add("lw " + regColumnas + ", " + array + "_cols  # cargar número de columnas");
        
        textSection.add("mul $t8, " + regIndice1 + ", " + regColumnas + "  # fila * columnas");
        textSection.add("add $t8, $t8, " + regIndice2 + "               # + columna");
        textSection.add("sll $t8, $t8, 2                              # * 4 bytes");
        textSection.add("add $t8, " + regArray + ", $t8               # dirección base + offset");
        textSection.add("sw " + regValor + ", 0($t8)                  # guardar valor");
        
        liberarRegistroTemporal(regColumnas);
    }


    private void asignarRegistro(String temp) {
        if (registroTemporal.containsKey(temp)) return;
        String tipo = obtenerTipo(temp);

        if (tipo.equals("float")) {
            if (registrosFlotantesDisponibles.isEmpty())
                throw new RuntimeException("No hay registros flotantes disponibles");
            String reg = registrosFlotantesDisponibles.removeFirst();
            registroTemporal.put(temp, reg);
        } else {
            if (registrosEnterosDisponibles.isEmpty())
                throw new RuntimeException("No hay registros enteros disponibles");
            String reg = registrosEnterosDisponibles.removeFirst();
            registroTemporal.put(temp, reg);
        } 
    }

    private void liberarRegistro(String temp) {
    String reg = registroTemporal.remove(temp);
        if (reg != null) {
            String tipo = obtenerTipo(temp);
            if (tipo.equals("float")) {
                registrosFlotantesDisponibles.addLast(reg);
            } else {
                registrosEnterosDisponibles.addLast(reg);
            }
            System.out.println("Liberado registro " + reg + " para temporal " + temp);
        }
    }

    private String obtenerRegistroTemporal() {
        if (registrosEnterosDisponibles.isEmpty()) {
            throw new RuntimeException("No hay registros disponibles para operación temporal");
        }
        return registrosEnterosDisponibles.removeFirst();
    }

    private void liberarRegistroTemporal(String registro) {
        registrosEnterosDisponibles.addLast(registro);
    }

    private void procesarArgumento(String tempArg, int indice) {
        String regArg = registroTemporal.get(tempArg);
        
        if (regArg != null) {
            // Argumento es un temporal con registro asignado
            textSection.add("move $a" + indice + ", " + regArg);
        } else if (tempArg.startsWith("\"") && tempArg.endsWith("\"")) {
            // Es un string literal
            String etiqueta = buscarEtiquetaLiteral(tempArg);
            if (etiqueta != null) {
                textSection.add("la $a" + indice + ", " + etiqueta);
            } else {
                // Crear etiqueta si no existe
                String nuevaEtiqueta = "str_" + (++contadorStrings);
                String contenido = tempArg.substring(1, tempArg.length() - 1);
                dataSection.add(nuevaEtiqueta + ": .asciiz \"" + contenido + "\"");
                textSection.add("la $a" + indice + ", " + nuevaEtiqueta);
            }
        } else if (tempArg.matches("-?\\d+")) {
            // Es un número literal (incluyendo negativos)
            textSection.add("li $a" + indice + ", " + tempArg);
        } else if (tablaSimbolos.containsKey(tempArg)) {
            // Es una variable
            textSection.add("lw $a" + indice + ", " + tempArg);
        } else {
            System.err.println("Argumento no reconocido: " + tempArg);
            textSection.add("li $a" + indice + ", 0    # argumento no reconocido");
        }
    }

    // Método auxiliar para obtener un registro disponible para arrays
    private String obtenerRegistroDisponible() {
        if (registrosEnterosDisponibles.isEmpty()) {
            throw new RuntimeException("No hay registros disponibles para array");
        }
        return registrosEnterosDisponibles.removeFirst();
    }


}
