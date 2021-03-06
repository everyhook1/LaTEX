package com.kutsyk.convertors;

import com.kutsyk.convertors.BibTEX.BibXML;
import com.kutsyk.convertors.LATEX.ToXML;
import com.kutsyk.grammar.BibTEX.BibPlosLexer;
import com.kutsyk.grammar.BibTEX.BibPlosParser;
import com.kutsyk.grammar.LaTEX.LaTEXLexer;
import com.kutsyk.grammar.LaTEX.LaTEXParser;
import com.kutsyk.windows.MainWindow;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.*;
import java.util.HashMap;

/*
 *  @author Kutsyk Vasyl
 * The Class Translator.
 */
public class Translator {

    /**
     * The commands.
     */
    private static HashMap<String, String> commands;

    /**
     * The writer.
     */
    public static PrintWriter writer;

    /**
     * The bib references.
     */
    public static HashMap<String, Integer> bibReferences;

    /*
     * The tex file.
     */
    protected static String texFile;

    public Translator(String fileName) throws Exception {
        texFile = fileName;
        writer = new PrintWriter(MainWindow.mainPath
                + "/LaTEXtoXML/bodyAndBottom.xml");
        getBibReferences(texFile);
        if (changeMainFile(texFile))
            ToXML.writer.close();

        writer.close();
    }
    /**
     * Gets the bib references.
     *
     * @param inputFile the input file
     * @return the bib references
     * @throws Exception the exception
     */
    private void getBibReferences(String inputFile) throws Exception {
        bibReferences = new HashMap<String, Integer>();
        StringBuilder referenceString = new StringBuilder();
        StringBuilder documentText = new StringBuilder();

        InputStream is = new FileInputStream(inputFile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = reader.readLine()) != null) {
                line.replace("\n", "");
                documentText.append(line);
        }
        is.close();
        cutReferencesFromText(documentText, referenceString);
        String listWithouWhiteSpaces = referenceString.toString().replace(" ",
                "");
        createBibRefList(listWithouWhiteSpaces);
    }

    /**
     * Cut references from text.
     *
     * @param text            the text
     * @param referenceString the reference string
     */
    private void cutReferencesFromText(StringBuilder text,
                                       StringBuilder referenceString) {
        String[] references = {"\\cite{", "\\citep{", "\\citet{",
                "\\citemain{"};
        for (String ref : references) {
            StringBuilder textCopy = text;
            while (textCopy.indexOf(ref) != -1) {
                int startIndex = textCopy.indexOf(ref);
                int finishIndex = textCopy.substring(startIndex).indexOf("}")
                        + startIndex;
                referenceString.append(textCopy.substring(
                        startIndex + ref.length(), finishIndex));
                referenceString.append(",");
                textCopy = new StringBuilder(textCopy.substring(finishIndex));
            }
        }
    }

    /**
     * Creates the bib ref list.
     *
     * @param refList the ref list
     */
    private void createBibRefList(String refList) {
        String[] references = refList.split(",");
        int counter = 0;
        for (String ref : references) {
            if (!bibReferences.containsKey(ref))
                bibReferences.put(ref, ++counter);
        }
    }

    /**
     * Change main file.
     *
     * @param inputFile the input file
     * @return true, if successful
     * @throws Exception the exception
     */
    private boolean changeMainFile(String inputFile) throws Exception {
        commands = new HashMap<String, String>();
        getCommandsFromFile(inputFile);
        createMainFile(inputFile);
        cutBibliographyFromFile(inputFile);
        bodyProcessing(MainWindow.mainPath + "/LaTEXtoXML/mainFile.tex");
        backProcessing(MainWindow.mainPath + "/LaTEXtoXML/back.tex");
        return true;
    }

    /**
     * Gets the commands names.
     *
     * @param inputFile the input file
     * @return the commands names
     * @throws Exception the exception
     */
    private void getCommandsFromFile(String inputFile) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        String line;
        while((line = reader.readLine()) != null){
            if(line.contains("\\newcommand")){
                String command = line = line.substring(line.indexOf("{")+1, line.indexOf("}"));
                String macros = "";
                if(line.contains("}"))
                    macros = line.substring(line.indexOf("}")+2, line.lastIndexOf("}"));
                else macros = "TODO";
                commands.put(command, macros);
            }
            if(line.contains("\\begin{document}"))
                break;
        }
        reader.close();
    }

    private void createMainFile(String inputFile) throws IOException {
        String dataFile = MainWindow.mainPath + "/LaTEXtoXML/mainFile.tex";
        File file = new File(inputFile);
        PrintWriter os = new PrintWriter(dataFile);
        FileInputStream fin = new FileInputStream(file);
        byte fileContent[] = new byte[(int) file.length()];
        fin.read(fileContent);
        String s = new String(fileContent);
        String[] content = s.split("\n");

        boolean skip = true;
        for (String line : content) {
            if(line.contains("\\begin{document}"))
                skip = false;
            if(line.toLowerCase().contains("\\section") && line.toLowerCase().contains("acknowledgments"))
                skip = true;
            if(line.contains("\\end{thebibliography}")){
                skip = false;
                continue;
            }
            if(line.contains("\\end{document}")) {
                os.print(line);
                break;
            }
            if(!skip)
                if(line.endsWith("\n"))
                    os.print(replaceCommandIfFound(line));
                else os.println(replaceCommandIfFound(line));
        }
        fin.close();
        os.close();
    }


    private void cutBibliographyFromFile(String inputFile) throws IOException {
        String dataFile = MainWindow.mainPath + "/LaTEXtoXML/back.tex";
        System.out.println(inputFile);
        File file = new File(inputFile);
        PrintWriter os = new PrintWriter(dataFile);

        FileInputStream fin = new FileInputStream(file);
        byte fileContent[] = new byte[(int) file.length()];
        fin.read(fileContent);
        String s = new String(fileContent);
        String[] content = s.split("\n");

        boolean skip = true;
        for (String line : content) {
            if(line.startsWith("%"))
                continue;
            if(line.toLowerCase().contains("\\section") && line.toLowerCase().contains("acknowledgments"))
                skip = false;
            if(line.contains("\\end{document}"))
                break;
            if(!skip)
                os.print(replaceCommandIfFound(line));
            if(line.contains("\\end{thebibliography}")
                    || line.contains("\\end{document}"))
                break;
        }
        fin.close();
        os.close();
    }

    /**
     * Replace LaTex command if found in the line.
     *
     * @param line the line
     * @return the line with replaced command equivalents
     */
    private String replaceCommandIfFound(String line) {
        String res = line;
        for (String key : commands.keySet()) {
            String replaceString = commands.get(key);
            if (line.contains(key) || line.contains(key)
                    || line.endsWith(key))
                line = res = line.replace(key+"\\", replaceString);
        }
        return res;
    }

    /**
     * Body processing.
     *
     * @param inputFile the input file
     * @throws Exception the exception
     */
    private void bodyProcessing(String inputFile) throws Exception {
        InputStream is = new FileInputStream(inputFile);
        ANTLRInputStream mainInput = new ANTLRInputStream(is);
        LaTEXLexer mainLexer = new LaTEXLexer(mainInput);
        CommonTokenStream mainTokens = new CommonTokenStream(mainLexer);
        LaTEXParser mainParser = new LaTEXParser(mainTokens);
        ParseTree mainTree = mainParser.compilationUnit();// parse
        ParseTreeWalker walker = new ParseTreeWalker(); // create standard

        writer = new PrintWriter(MainWindow.mainPath
                + "/LaTEXtoXML/bodyAndBottom.xml");
        ToXML translator = new ToXML(writer);
        walker.walk(translator, mainTree); // initiate walk of tree with
        // listener
        writer.close();
    }

    /**
     * Back processing.
     *
     * @param inputFile the input file
     * @throws Exception the exception
     */
    private void backProcessing(String inputFile) throws Exception {
        InputStream is = new FileInputStream(inputFile);
        ANTLRInputStream mainInput = new ANTLRInputStream(is);
        BibPlosLexer mainLexer = new BibPlosLexer(mainInput);
        CommonTokenStream mainTokens = new CommonTokenStream(mainLexer);
        BibPlosParser mainParser = new BibPlosParser(mainTokens);
        ParseTree mainTree = mainParser.compilationUnit();// parse
        ParseTreeWalker walker = new ParseTreeWalker(); // create standard

        writer = new PrintWriter(MainWindow.mainPath
                + "/LaTEXtoXML/back.xml");
        BibXML translator = new BibXML(writer);
        walker.walk(translator, mainTree); // initiate walk of tree with
        // listener
        writer.close();
    }

}
