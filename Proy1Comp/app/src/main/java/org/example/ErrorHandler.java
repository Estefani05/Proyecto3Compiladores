package org.example;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ErrorHandler {
    private final String errorLogPath;
    private final List<String> errors = new ArrayList<>();
    private boolean panicMode = false;
    private boolean continueOnError = false;
    
    public ErrorHandler(String logPath) {
        this.errorLogPath = logPath;
        initializeLogFile();
    }
    
    private void initializeLogFile() {
        try {
            // Crear directorios si no existen
            Paths.get(errorLogPath).getParent().toFile().mkdirs();
            
            // Limpiar archivo existente o crear nuevo
            Files.deleteIfExists(Paths.get(errorLogPath));
            Files.createFile(Paths.get(errorLogPath));
            
            // Escribir encabezado en el archivo
            try (FileWriter fw = new FileWriter(errorLogPath)) {
                fw.write("=== REGISTRO DE ERRORES ===\n");
                fw.write(String.format("%-10s %-5s %-5s %s\n", "TIPO", "LÍNEA", "COL", "MENSAJE"));
                fw.write("-----------------------------------------------------\n");
            }
            
            System.out.println("Archivo de errores inicializado en: " + 
                Paths.get(errorLogPath).toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error inicializando archivo de errores: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void reportError(int line, int column, String message, String errorType) {
        // Normalizar valores
        line = Math.max(line, 0);  // Evitar línea negativa
        column = Math.max(column, 0);  // Evitar columna negativa
        
        String errorMsg = String.format("[%s] Línea %d, Columna %d: %s", 
                              errorType, line, column, message);
        errors.add(errorMsg);
        
        // Formato para archivo de log
        String logMsg = String.format("%-10s %-5d %-5d %s", 
                           errorType, line, column, message);
        
        // Escribir en archivo con verificación explícita
        try {
            FileWriter fw = new FileWriter(errorLogPath, true);
            fw.write(logMsg + "\n");
            fw.flush(); // Forzar escritura inmediata
            fw.close();
            
            //System.out.println("Error registrado: " + errorMsg);
        } catch (IOException e) {
            System.err.println("Error crítico escribiendo en log:");
            System.err.println("Ruta intentada: " + Paths.get(errorLogPath).toAbsolutePath());
            e.printStackTrace();
        }
        
        //System.err.println(errorMsg);
    }

    public boolean checkFileAccess() {
        try {
            return Files.isWritable(Paths.get(errorLogPath));
        } catch (Exception e) {
            System.err.println("Error verificando permisos: " + e.getMessage());
            return false;
        }
    }

    public void enterPanicMode() {
        this.panicMode = true;
        System.err.println("¡ENTRANDO EN MODO PÁNICO! Errores graves detectados.");
    }

    public void exitPanicMode() {
        this.panicMode = false;
    }

    public boolean isInPanicMode() {
        return panicMode;
    }

    public String getErrorSummary() {
        if (errors.isEmpty()) {
            return "\n=== NO SE DETECTARON ERRORES ===\n";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== RESUMEN DE ERRORES ===\n");
        sb.append("Total errores: ").append(errors.size()).append("\n");
        
        // Contar errores por tipo
        int lexicoCount = 0;
        int sintacticoCount = 0;
        int semanticoCount = 0;
        
        for (String error : errors) {
            if (error.contains("[LÉXICO]")) lexicoCount++;
            if (error.contains("[SINTÁCTICO]")) sintacticoCount++;
            if (error.contains("[SEMÁNTICO]")) semanticoCount++;
        }
        
        sb.append("- Errores léxicos: ").append(lexicoCount).append("\n");
        sb.append("- Errores sintácticos: ").append(sintacticoCount).append("\n");
        sb.append("- Errores semánticos: ").append(semanticoCount).append("\n");
        
        return sb.toString();
    }

    public void setContinueOnError(boolean continueOnError) {
        this.continueOnError = continueOnError;
    }

    public boolean shouldContinue() {
        return continueOnError;
    }
    
    public int getErrorCount() {
        return errors.size();
    }
    
    public List<String> getErrors() {
        return new ArrayList<>(errors);  
    }
}