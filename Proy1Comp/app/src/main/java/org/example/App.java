package org.example;
import java.io.IOException;

//RuntimeException Linea 942
import org.example.AppAux;

public class App {

    private static AppAux app = new AppAux();
    private static final String ERROR_FILE = "Proy1Comp/app/src/main/resources/errors.log";

    public String getGreeting() {
        return "Hello World!";
    }

    public static void main(String[] args) {
        
        try {
            /* Genera los archivos de la carpeta ParserLexer */
            //app.generarLexerParser();

            /* Hace solo LEXER */
            //app.ejecutarLexer();

            /* Hace LEXER Y PARSER */
            app.ejecutarLexerParser();
            System.out.println("Proceso completado exitosamente");
        } catch (Exception e) {
            try {
                FileManager.writeFile(ERROR_FILE, "Error: " + e.getMessage());
            } catch (IOException ioEx) {
                System.err.println("Error al escribir log: " + ioEx.getMessage());
            }
            System.err.println("Error durante el análisis: " + e.getMessage());
            e.printStackTrace();
        }
    }

}