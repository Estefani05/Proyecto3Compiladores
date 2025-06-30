
package org.example.ParserLexer;

import java_cup.runtime.*;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.example.ErrorHandler;

%%
%public
%class BasicLexerCup
%cup
%unicode
%line
%column

%{
    StringBuffer string = new StringBuffer();

    private FileWriter tokenWriter;

    // Método para inicializar el logger de tokens
    // Entrada: Ninguna
    // Salida: Archivo "tokens.log"
    private void initTokenLogger() {
        try {
            tokenWriter = new FileWriter("Proy1Comp/app/src/main/resources/tokens.log");
            tokenWriter.write("=== TOKENS ENCONTRADOS ===\n");
            tokenWriter.write(String.format("%-20s %-20s %-10s %-10s\n", 
                                "TOKEN", "LEXEMA", "LINEA", "COLUMNA"));
            tokenWriter.flush();
        } catch (IOException e) {
            System.err.println("Error inicializando archivo de tokens: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void logToken(String tokenType, String lexeme) {
        if (tokenWriter != null) {
            try {
                tokenWriter.write(String.format("%-20s %-20s %-10d %-10d\n", 
                    tokenType, lexeme, yyline + 1, yycolumn + 1));
                tokenWriter.flush();
            } catch (IOException e) {
                System.err.println("Error escribiendo token: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


    // Método para crear un símbolo con tipo dado
    // Entrada: tipo del token (int)
    // Salida: objeto Symbol con línea y columna actuales
    private Symbol symbol(int type) {
        String tokenName = (type >= 0 && type < sym.terminalNames.length) ? sym.terminalNames[type] : "UNKNOWN";
        logToken(tokenName, yytext());
        return new Symbol(type, yyline, yycolumn);
    }

    private Symbol symbol(int type, Object value) {
        String tokenName = (type >= 0 && type < sym.terminalNames.length) ? sym.terminalNames[type] : "UNKNOWN";
        logToken(tokenName, yytext());
        return new Symbol(type, yyline, yycolumn, value);
    }


  // Método para cerrar el logger de tokens y exportar la tabla de símbolos
  // Entrada: ninguna
  // Salida: archivo de tokens cerrado y tabla de símbolos exportada
    public void closeTokenLogger() {
        if (tokenWriter != null) {
            try {
                tokenWriter.close();
            } catch (IOException e) {
                System.err.println("Error cerrando archivo de tokens: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    


    private ErrorHandler errorHandler;

    public void setErrorHandler(ErrorHandler handler) {
        this.errorHandler = handler;
    }
    

    // Método handleError CORREGIDO
    
    private Symbol handleLexicalError() {
        String mensaje = "Caracter ilegal '" + yytext() + "'";
        int linea = yyline + 1;
        int columna = yycolumn + 1;

        if (errorHandler != null) {
            errorHandler.reportError(linea, columna, mensaje, "LEXICO");
        } else {
            System.err.println("Error lexico: " + mensaje + " en la linea " + linea + ", columna " + columna);
        }

        yybegin(YYINITIAL);  // Reinicia análisis
        return new Symbol(sym.error, yyline, yycolumn, yytext());
    }
    
%}

%init{
    System.out.println("Inicializando analizador lexico...");
    initTokenLogger();
    System.out.println("Analizador lexico inicializado correctamente.");
%init}

%eof{
    System.out.println("Cerrando analizador lexico...");
    closeTokenLogger();
    System.out.println("Analizador lexico cerrado correctamente.");
%eof}

LineTerminator = \r|\n|\r\n
InputCharacter = [^\r\n]
WhiteSpace     = {LineTerminator} | [ \t\f\r\n]

Comment = {TraditionalComment} | {EndOfLineComment} 
TraditionalComment   = "{" [^}] ~"}" | "{" + "}"
EndOfLineComment     = "@" {InputCharacter}* {LineTerminator}?

Identifier = [a-zA-Z]([a-zA-Z0-9])*
DecIntegerLiteral = 0 | [1-9][0-9]*
FloatingPointLiteral = [0-9]+ "." [0-9]+
CharLiteral = "'" ( [^'\n\r\\'] | "\\'" ) "'"


%state STRING

%%

    /* keywords */
  // Estructuras de control
  <YYINITIAL> "if"       { return symbol(sym.IF); }
  <YYINITIAL> "elif"     { return symbol(sym.ELIF); }
  <YYINITIAL> "else"     { return symbol(sym.ELSE); }
  <YYINITIAL> "do"      { return symbol(sym.DO); }
  <YYINITIAL> "while"    { return symbol(sym.WHILE); }
  <YYINITIAL> "for"      { return symbol(sym.FOR); }
  <YYINITIAL> "in"       { return symbol(sym.IN); }
  <YYINITIAL> "break"    { return symbol(sym.BREAK); }
  <YYINITIAL> "case"     { return symbol(sym.CASE); }
  <YYINITIAL> "default"  { return symbol(sym.DEFAULT); }
  <YYINITIAL> "Switch"   { return symbol(sym.SWITCH); }

  //funciones, variables y entrada y salida
  <YYINITIAL> "return"   { return symbol(sym.RETURN); }
  <YYINITIAL> "leer"    { return symbol(sym.LEER); }
  <YYINITIAL> "imprimir" { return symbol(sym.IMPRIMIR); }
  <YYINITIAL> "global"   { return symbol(sym.GLOBAL); }
  <YYINITIAL> "function" { return symbol(sym.FUNCTION); }
  <YYINITIAL> "param"    { return symbol(sym.PARAM); }
  <YYINITIAL> "main"     { return symbol(sym.MAIN); }

  //Estructuras de datos
  <YYINITIAL> "arrx"     { return symbol(sym.ARRX); }
  <YYINITIAL> "matrx"    { return symbol(sym.MATRX); }

  //Tipos de datos
  <YYINITIAL> "void"     { return symbol(sym.VOID); }
  <YYINITIAL> "int"       { return symbol(sym.INTEGER_T); }
  <YYINITIAL> "float"     { return symbol(sym.FLOAT_T); }
  <YYINITIAL> "char"      { return symbol(sym.CHAR_T); }
  <YYINITIAL> "string"    { return symbol(sym.STRING_T); }
  <YYINITIAL> "struct"   { return symbol(sym.STRUCT); }
  <YYINITIAL> "bool"     { return symbol(sym.BOOL); }
  <YYINITIAL> "sol"      { return symbol(sym.FALSE); }
  <YYINITIAL> "luna"     { return symbol(sym.TRUE); }

  //Operadores Logicos
  <YYINITIAL> "<"        { return symbol(sym.MENOR); }
  <YYINITIAL> ">"        { return symbol(sym.MAYOR); }
  <YYINITIAL> "<="       { return symbol(sym.MENORIGUAL); }
  <YYINITIAL> ">="       { return symbol(sym.MAYORIGUAL); }
  <YYINITIAL> "!="       { return symbol(sym.DIFERENTE); }
  <YYINITIAL> "^"        { return symbol(sym.CONJUNCION); }
  <YYINITIAL> "#"        { return symbol(sym.DISYUNCION); }
  <YYINITIAL> "!"        { return symbol(sym.NEGACION); }
  <YYINITIAL> "=="        { return symbol(sym.COMPARACION); }
  <YYINITIAL> "="         { return symbol(sym.ASIGNA); }

  //Operadores aritmeticos
  <YYINITIAL> "+"         { return symbol(sym.SUMA); }
  <YYINITIAL> "-"         { return symbol(sym.RESTA); }
  <YYINITIAL> "*"         { return symbol(sym.MULTIPLICA); }
  <YYINITIAL> "//"         { return symbol(sym.DIVIDE); }
  <YYINITIAL> "~"         { return symbol(sym.MODULO); }   
  <YYINITIAL> "**"       { return symbol(sym.POTENCIA); }
  <YYINITIAL> "++"        { return symbol(sym.INCREMENTO); }
  <YYINITIAL> "--"        { return symbol(sym.DECREMENTO); }
  
  //Delimitadores y simbolos
  <YYINITIAL> "."        { return symbol(sym.PUNTO); }
  <YYINITIAL> ","        { return symbol(sym.COMA); }
  <YYINITIAL> ":"        { return symbol(sym.DOSPUNTOS); }
  <YYINITIAL> "ʃ"        { return symbol(sym.ESH_IZ); }
  <YYINITIAL> "ʅ"        { return symbol(sym.EZH_DE); }
  <YYINITIAL> "["        { return symbol(sym.BRACEIZQ); }
  <YYINITIAL> "]"        { return symbol(sym.BRACEDER); }
  <YYINITIAL> "|"        { return symbol(sym.PIPE); }
  <YYINITIAL> "?"         { return symbol(sym.FINLINEA); }
  <YYINITIAL> "{"        { return symbol(sym.INIT_COMMENT); }
  <YYINITIAL> "}"        { return symbol(sym.END_COMMENT); }
  <YYINITIAL> "(("         { return symbol(sym.INIT_BLOC); }
  <YYINITIAL> "/"         { return symbol(sym.END_BLOC); }

<YYINITIAL> {
  
  /* identifiers */ 
  {Identifier} { return symbol(sym.IDENTIFIER, yytext()); }

  /* literals */
  {DecIntegerLiteral}  { return symbol(sym.INTEGER_LITERAL, yytext()); }
  \"                   { string.setLength(0); yybegin(STRING); }

  {CharLiteral}    { return new Symbol(sym.CHAR_LITERAL); }

  /* comments */
  {Comment}            {  }

  /* whitespace */
  {WhiteSpace}         {  }
}

<STRING> {
  \"                   { yybegin(YYINITIAL); 
                         return symbol(sym.STRING_LITERAL, string.toString()); }
  [^\n\r\"\\]+         { string.append(yytext()); }
  \\t                  { string.append('\t'); }
  \\n                  { string.append('\n'); }
  \\r                  { string.append('\r'); }
  \\\"                 { string.append('\"'); }
  \\                   { string.append('\\'); }
}

[^] { 
    return handleLexicalError(); 
} 