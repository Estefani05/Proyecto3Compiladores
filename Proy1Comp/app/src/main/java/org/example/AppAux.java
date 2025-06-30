package org.example;
import java.nio.file.*;

public class AppAux {
    // Globales - Rutas constantes
    private static final String INPUT_FILE = "Proy1Comp/app/src/main/resources/ejemplo2.txt";
    private static final String OUTPUT_FILE = "Proy1Comp/app/src/main/resources/output.txt";
    private static MainFlexCup mfjc = new MainFlexCup();


    public static void generarLexerParser() throws Exception {
        mfjc.GenerarLexerParser();
    }

    //Funcion puente que llama a la funcion que hace el analisis lexico en el archivo
    public static void ejecutarLexer() throws Exception {
        String basePath = System.getProperty("user.dir");
        String path = Paths.get(basePath, INPUT_FILE).toString();
        /* 
        File f = new File(path);
        System.out.println(f.exists());
        */
        mfjc.AnalizadorLexico();
    }

    //Funcion puente que llama a la funcion que hace el analisis sintactico en el archivo
    public static void ejecutarLexerParser() throws Exception {
        mfjc.AnalizadorLexicoSintactico();

        // 4. Escribir resultados
        FileManager.writeFile(OUTPUT_FILE, "Análisis completado correctamente");
        System.out.println("\nTokens registrados en tokens.log " );
    }

}
