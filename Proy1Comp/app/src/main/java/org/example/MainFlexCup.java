package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.*;

//import org.example.ErrorHandler;
import org.example.ParserLexer.BasicLexerCup;
import org.example.ParserLexer.parser;
import java_cup.internal_error;
import java_cup.runtime.Symbol;
import jflex.exceptions.SilentExit;

public class MainFlexCup {

    private static final String INPUT_FILE = "Proy1Comp/app/src/main/resources/ejemplo2.txt";
    private static final String ERROR_FILE = "Proy1Comp/app/src/main/resources/errors.log";
    private static String basePath = System.getProperty("user.dir");
    private static final String path = Paths.get(basePath, INPUT_FILE).toString();

    public void iniLexerParser(String rutaLexer, String rutaParser) throws internal_error, Exception {
        GenerateLexer(rutaLexer);
        Generateparser(rutaParser);
    }

    //Genera el archivo del lexer
    public void GenerateLexer(String ruta) throws IOException, SilentExit{
        String[] strArr = {ruta};
        jflex.Main.generate(strArr);
    }

    public parser crearParser(BasicLexerCup lexer) throws Exception {
        parser p = new parser(lexer);
        return p;
    }

    //Genera los archivos del parser
    public void Generateparser(String ruta) throws internal_error, IOException, Exception{
        String[] strArr = {ruta};
        java_cup.Main.main(strArr);
    }

    //Genera solo el lexer
    public void AnalizadorLexico() throws IOException
    {
        Reader reader = new BufferedReader(new FileReader (path));
        reader.read();
        BasicLexerCup lex = new BasicLexerCup(reader);
        int i = 0;
        Symbol token;
        while(true)
        {
            token = lex.next_token();
            if(token.sym != 0){
                //System.out.println("Token: "+token.sym+ ", Valor: "+(token.value==null?lex.yytext():token.value.toString()));
            }
            else{        
                //System.out.println("Cantidad de lexemas encontrados: "+i);
                return;
            }
            i++;
        }
    }

    //Genera lexer y parser
    public static void AnalizadorLexicoSintactico() throws Exception {
        // Limpiar archivo de tokens previo si existe
        
        // Leer el código fuente
        String sourceCode = FileManager.readFile(INPUT_FILE);
        /* 
        System.out.println("=== CÓDIGO FUENTE ===");
        System.out.println(sourceCode);
        System.out.println("=====================");
        */

        // Crear el ErrorHandler compartido
        ErrorHandler errorHandler = new ErrorHandler(ERROR_FILE);
        errorHandler.setContinueOnError(true); 
        
        try (Reader reader = new StringReader(sourceCode)) {
            // Crear el lexer
            BasicLexerCup lexer = new BasicLexerCup(reader);
            lexer.setErrorHandler(errorHandler);
            
            // Crear el parser y conectarlo con el lexer
            parser p = new parser(lexer);
            p.setErrorHandler(errorHandler);
            
            try {
                System.out.println("Iniciando análisis sintáctico...");
                Symbol parseResult = p.parse();
                System.out.println("Análisis sintáctico completado exitosamente.");
            } catch (Exception e) {
                System.out.println("Análisis sintáctico completado con errores: " + e.getMessage());
            }
            
            // Mostrar resultados y estadísticas de errores
            
            /*System.out.println("\n=== RESULTADOS DEL ANÁLISIS ===");
            System.out.println("Archivo analizado: " + inputFile);
            System.out.println("Log de tokens: " + Paths.get(tokensFile).toAbsolutePath());
            System.out.println("Log de errores: " + Paths.get(errorsFile).toAbsolutePath());
            System.out.println(errorHandler.getErrorSummary());
            */
        }

    }

     //Funcion que llama a la creacion de los archivos necesarios para el lexer y el parser, y los mueve de posicion
    public void GenerarLexerParser() throws Exception {
        // 1. Generar analizadores (esto solo crea las clases)
        String basePath = System.getProperty("user.dir");
        //System.out.println("Directorio base: " + basePath);
        
        String fullPathLexer = Paths.get(basePath, "Proy1Comp","app", "src", "Adicionales", "BasicLexerCup.jflex").toString();
        String fullPathParser = Paths.get(basePath, "Proy1Comp","app", "src", "Adicionales", "BasicParser.cup").toString();
        iniLexerParser(fullPathLexer, fullPathParser);
        
        // 2. Mover archivos generados
        Path destDir = Paths.get(basePath, "Proy1Comp","app", "src", "main", "java", "org", "example", "ParserLexer");
        Files.createDirectories(destDir);
        
        Files.move(Paths.get(basePath, "sym.java"), destDir.resolve("sym.java"), StandardCopyOption.REPLACE_EXISTING);
        Files.move(Paths.get(basePath, "parser.java"), destDir.resolve("parser.java"), StandardCopyOption.REPLACE_EXISTING);
        Files.move(Paths.get(basePath, "Proy1Comp","app", "src", "Adicionales", "BasicLexerCup.java"), 
                 destDir.resolve("BasicLexerCup.java"), StandardCopyOption.REPLACE_EXISTING);
    }
}

