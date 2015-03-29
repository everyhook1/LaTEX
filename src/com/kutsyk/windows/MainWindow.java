package com.kutsyk.windows;

import com.kurpiak.styling.StyledDocument;
import com.kutsyk.TextEditor.TextLineNumber;
import com.kutsyk.convertors.Translator;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.HashMap;

/*
 * @author Kutsyk Vasyl
 * The Class MainWindow.
 */


// TODO: Auto-generated Javadoc
/**
 * The Class MainWindow.
 */
public class MainWindow extends JFrame {

    public static String mainPath = "";
    /**
     * The dir path.
     */
    private static String fileName = "";
    private static PrintWriter errorLogFile = null;

    /**
     * The dir.
     */
    private static File dir = new File(mainPath + "/LaTEXtoXML");
    private static boolean wasAnyLaTEXProceeded = false;
    private static JTextArea console;
    private static Thread translationThread;

    /**
     * The iso trie.
     */
    private static HashMap<String, String> isoTrie;

    /**
     * The main method.
     *
     * @param args the arguments
     */
    public static void main(String[] args) {
        mainPath = System.getProperty("user.dir");
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager
                            .getSystemLookAndFeelClassName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                new MainWindow().setVisible(true);
            }
        });
    }

    public MainWindow() {
        initComponents();
        initLineTextEditor();
        initLabels();
        initProgressBar();
        redirectSystemStreams();
        createIsoTree();
    }

    private void initProgressBar() {
        ImageIcon prog = new ImageIcon(mainPath + "/LaTEXbin/images/pleasewait.gif");
        progressBar.setIcon(prog);
        progressBar.setIcon(prog);
    }

    private void redirectSystemStreams() {
        try {
            errorLogFile = new PrintWriter(mainPath + "/errorLogLaTEX.txt");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        console = new JTextArea();
        console.setFont(new Font("Arial", 14, 14));
        console.setEditable(false);
        console.setWrapStyleWord(true);
        console.setDragEnabled(true);
        console.setAutoscrolls(true);

        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                errorLogFile.println(String.valueOf((char) b));
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                errorLogFile.println(new String(b, off, len));
            }
        };
//		System.setOut(new PrintStream(out, true));
//		System.setErr(new PrintStream(out, true));
    }

    /**
     * Clear.
     */
    private static void clear() {
        deleteFile("result.xml");
        deleteFile("mainFile.tex");
        deleteFile("bodyAndBottom.xml");
        deleteFile("back.xml");
        deleteFile("back.tex");
        deleteFile("newCommands.tex");

        clearDirectory("bibliography");
        clearDirectory("figures");
        clearDirectory("suppFigures");
        clearDirectory("tables");
    }

    /**
     * Delete file.
     *
     * @param deletedFile the deleted file
     */
    private static void deleteFile(String deletedFile) {
        File file = new File(mainPath + "/LaTEXtoXML/" + deletedFile);
        file.deleteOnExit();
    }

    /**
     * Clear directory.
     *
     * @param directoryName the directory name
     */
    private static void clearDirectory(String directoryName) {
        File dir = new File(mainPath + "/LaTEXtoXML/" + directoryName);
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            for (int i = 0; i < files.length; ++i)
                files[i].deleteOnExit();
        }
        dir.deleteOnExit();
    }

    /**
     * Creates the iso - xml map trie.
     *
     * @throws Exception the exception
     */
    private void createIsoTree() {
        if (isoTrie != null) return;
        BufferedReader reader = null;
        try {
            isoTrie = new HashMap<String, String>();
            System.out.println("Here: " + mainPath);
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(mainPath + "/LaTEXbin/ISOENT.csv"))));
            String line;
            while ((line = reader.readLine()) != null) {
                String[] entity = line.split(",");
                isoTrie.put(entity[0], entity[1]);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (Exception e) {
            }
            ;
        }
    }

    public static HashMap<String, String> getIsoTrie() {
        return isoTrie;
    }

    /**
     * Creates the folders and files if need.
     */
    private static void createFoldersAndFilesIfNeed() {
        createFolders();
        createFiles();
    }

    /**
     * Creates the folders.
     */
    private static void createFolders() {
        if (dir.mkdir())
            ;

        String[] folders = {"bibliography"};
        File dir = new File(mainPath + "/LaTEXtoXML/");
        if (dir.mkdir())
            ;
        for (int i = 0; i < folders.length; ++i) {
            dir = new File(mainPath + "/LaTEXtoXML/" + folders[i]);
            if (dir.mkdir())
                ;
        }
    }

    /**
     * Creates the files.
     */
    private static void createFiles() {
        String[] files = {"result.xml", "bodyAndBottom.xml", "mainFile.tex", "back.tex",
                "newCommands.tex"};
        for (String file : files) {
            dir = new File(mainPath + "/LaTEXtoXML/" + file);
            try {
                if (dir.createNewFile())
                    ;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Translate action.
     */
    private void translateAction() {
        clear();
        translationThread = new Thread(new Runnable() {
            public void run() {
                Translator translator;
                try {
                    translator = new Translator(fileName);
                    createResult();
                    wasAnyLaTEXProceeded = true;
                    File result = new File(mainPath + "/LaTEXtoXML/result.xml");
                    result.deleteOnExit();
                    if (result.exists())
                        Desktop.getDesktop().open(result);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                JOptionPane.showMessageDialog(null, "Translated");
            }
        });
        translationThread.start();
    }

    /**
     * Creates the result.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void createResult() throws IOException {
        PrintWriter resultXml = new PrintWriter(mainPath
                + "/LaTEXtoXML/result.xml");
        InputStream in = null;
        resultXml.append("<article>");
        writePartToResult(resultXml, in, "bodyAndBottom");
        resultXml.append("</body><back>");
        writePartToResult(resultXml, in, "back");
        resultXml.append("</back>");
        resultXml.append("</article>");
        resultXml.close();
    }

    /**
     * Write part to result.
     *
     * @param resultXml the result xml
     * @param in        the in
     * @param part      the part
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void writePartToResult(PrintWriter resultXml,
                                          InputStream in, String part) throws IOException {
        in = new FileInputStream(new File(mainPath + "/LaTEXtoXML/" + part
                + ".xml"));
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        while ((line = reader.readLine()) != null)
            resultXml.print(line);
        in.close();
        reader.close();
    }

    private void FileChooseButtonActionPerformed(ActionEvent e) {
        wasAnyLaTEXProceeded = false;
        FileNameExtensionFilter filter = new FileNameExtensionFilter("TEX file", "tex", "tex");
        @SuppressWarnings("serial")
//		JFileChooser chooser = new JFileChooser(new File("D:\\Charlesworth\\plos_template")) {
                JFileChooser chooser = new JFileChooser(new File("D:\\Charlesworth\\Testing documents\\latex\\latex")) {
            public void approveSelection() {
                super.approveSelection();
            }
        };
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.addChoosableFileFilter(filter);
        chooser.setFileFilter(filter);
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            console.setText("");
            directoryChoosed(chooser.getSelectedFile().getAbsolutePath());
        }
//		directoryChoosed(dirPath);
    }

    private void directoryChoosed(String fileName) {
        boolean canBeProcced = fileName.endsWith(".tex");
        if (!canBeProcced) {
            System.err
                    .println("[ERROR] : Choose folder that contains all necessary files");
            JOptionPane
                    .showMessageDialog(
                            null,
                            "You choosed directory that doesn't contain all necessary files.",
                            "Folder warning", JOptionPane.WARNING_MESSAGE);
        }
        this.fileName = fileName;
        createFoldersAndFilesIfNeed();
        writeDocumentToPane(fileName);
    }

    private void writeDocumentToPane(String fileName) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(fileName)));
            String line;
            StringBuilder content = new StringBuilder();
            while ((line = reader.readLine()) != null)
                content.append(line).append('\n');
            reader.close();
            documentText.setText(content.toString());
            documentText.setCaretPosition(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void translateButtonActionPerformed(ActionEvent e) {
        if (fileName.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Choose neccesary files");
            return;
        }
        try {
            translateAction();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    private void ExitButtonActionPerformed(ActionEvent e) {
        closeLogFile();
        clear();
        System.exit(0);
    }

    private void SaveXmlResultActionPerformed(ActionEvent e) {
        if (!wasAnyLaTEXProceeded) {
            JOptionPane.showMessageDialog(null,
                    "You have not proceeded any LaTEX file");
            return;
        }

        int save = JOptionPane.showConfirmDialog(null,
                "Do you want to save this file?", "Close Alert",
                JOptionPane.YES_NO_OPTION);

        if (save == JOptionPane.YES_OPTION) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setSelectedFile(new File(fileName
                    + " - final.xml"));
            fileChooser.showSaveDialog(this);
            File file = fileChooser.getSelectedFile();
            try {
                PrintWriter writer = new PrintWriter(file);
                InputStream in = new FileInputStream(mainPath
                        + "/LaTEXtoXML/result.xml");
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null)
                    writer.print(line);

                in.close();
                writer.close();
            } catch (IOException exp) {
            }
        }

    }

    public TextLineNumber lineNumber;
    private static JScrollPane scrollPane;
    private static Highlighter highlighter;

    private void initLabels() {
        Dimension minDim = new Dimension(350, 150);
        documentTab.setMinimumSize(minDim);
    }

    private void initLineTextEditor() {
        documentText.setStyledDocument(StyledDocument.getInstance());
        documentText.setFont(new Font("Arial", 14, 14));
        lineNumber = new TextLineNumber(documentText);
    }

    private static boolean uselessLine(String line) {
        return line.replace(" ", "").startsWith("%")
                || line.replace(" ", "").isEmpty();
    }

    private static boolean hasRightStructure(String inputFile) throws Exception {
        InputStream is = new FileInputStream(inputFile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;

        boolean figureSection = false;
        boolean supportingSection = false;

        boolean wasSupportingFigureDeclared = false;
        boolean supportingFigureDeclared = false;
        boolean figureStarted = false;
        boolean figureDeclared = false;

        int startOfFigureSection = 0;
        int startOfSupportingSection = 0;
        int i = 0;
        while ((line = reader.readLine()) != null) {
            ++i;
            if (uselessLine(line))
                continue;

			/*
             * Checks if figure section has started or not
			 */
            if (!figureSection) {
                figureSection = (line.contains("\\section"))
                        && (line.contains("Figure Legends") || line
                        .contains("Figures"));
                if (figureSection)
                    startOfFigureSection = i;
            }

			/*
             * Checks if supporting figure section has started or not
			 */
            if (!supportingSection) {
                supportingSection = (line.contains("\\section") && (line
                        .contains("Supporting Information")))
                        || ((line.contains("\\renewcommand") && line
                        .contains("S\\arabic")));
                if (supportingSection)
                    startOfSupportingSection = i;
            }

			/*
			 * If the figure section has started and no figures had been
			 * declared then we are looking for some
			 */
            if (figureSection && !figureDeclared) {
                if (line.contains("\\begin{figure*}")
                        || line.contains("\\begin{figure}"))
                    figureStarted = true;

				/*
				 * If the figure block ends it means that there is the right
				 * figure structure so the figure is declared
				 */
                if (line.contains("\\end{figure*}")
                        || line.contains("\\end{figure}"))
                    if (figureStarted) {
                        figureDeclared = true;
                        figureSection = false;
                    }
            }

			/*
			 * If the supporting figure section has started and no figures had
			 * been declared then we are looking for some
			 */
            if (supportingSection && !supportingFigureDeclared) {
                if (line.contains("\\begin{suppfigure*}")
                        || line.contains("\\begin{suppfigure}"))
                    wasSupportingFigureDeclared = true;
				/*
				 * If the supporting figure block ends it means that there is
				 * the right supporting figure structure so the supporting
				 * figure is declared
				 */
                if (line.contains("\\end{suppfigure*}")
                        || line.contains("\\end{suppfigure}"))
                    if (wasSupportingFigureDeclared) {
                        supportingFigureDeclared = true;
                        supportingSection = false;
                    }
            }
        }

        is.close();
		/*
		 * If the figure section is declared and no figures declared than we
		 * throwing error
		 */
        if (figureSection && !figureDeclared) {
            highlightError(startOfFigureSection, 1);
            scrollToLine(startOfFigureSection);
            return false;
        }
		/*
		 * If the supporting figure section is declared and no supporting
		 * figures declared, than we throwing error
		 */
        if (supportingSection && !supportingFigureDeclared) {
            highlightError(startOfSupportingSection, 2);
            scrollToLine(startOfSupportingSection);
            return false;
        }
        return true;
    }

    private static void highlightError(int line, int errorType) {
        Document doc = documentText.getDocument();
        Element map = doc.getDefaultRootElement();
        Element startLine = map.getElement(line - 1);

        highlighter = documentText.getHighlighter();
        highlighter.removeAllHighlights();

        DefaultHighlighter.DefaultHighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(
                new Color(255, 178, 178));

        try {
            if (errorType == 1)
                console.append("[ERROR]: Figure section found, but figure declaration missing"
                        + "\n");
            if (errorType == 2)
                console.append("[ERROR]: Supporting section found, but supporting materials declaration missing"
                        + "\n");
            highlighter.addHighlight(startLine.getStartOffset(),
                    startLine.getEndOffset(), painter);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private static void scrollToLine(final int line) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
                Document doc = documentText.getDocument();
                Element map = doc.getDefaultRootElement();
                Element l = map.getElement(line - 1);
                documentText.setCaretPosition(l.getStartOffset());
                Rectangle rec = null;
                try {
                    rec = documentText.modelToView(l.getStartOffset());
                    if (rec == null)
                        return;
                    verticalBar.setValue((int) rec.getY());
                } catch (BadLocationException e1) {
                    e1.printStackTrace();
                }

            }

        });
    }

    private void SaveButtonActionPerformed(ActionEvent e) {
        try {
            String content = documentText.getText();
            content = content.replaceAll("(?!\\r)\\n", "\r\n");

            File file = new File(fileName);
            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();

        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private void HowToUseButtonActionPerformed(ActionEvent e) {
        try {
            File sourceFolder = new File(mainPath
                    + "/LaTEXbin/documentation/index.html");
            Desktop.getDesktop().open(sourceFolder);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private void thisWindowClosing(WindowEvent e) {
        closeLogFile();
        clear();
    }

    private void closeLogFile() {
        if (translationThread != null)
            translationThread.stop();
        if (errorLogFile != null)
            errorLogFile.close();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY
        // //GEN-BEGIN:initComponents
		menuBar1 = new JMenuBar();
		MainMenu = new JMenu();
		chooseFileMenu = new JMenuItem();
		helpItem = new JMenuItem();
		exitItem = new JMenuItem();
		spliPaneWithDoc = new JSplitPane();
		documentTab = new JTabbedPane();
		panel7 = new JPanel();
		scrollPane3 = new JScrollPane();
		documentText = new JTextPane();
		xmlPane = new JTabbedPane();
		panel6 = new JPanel();
		scrollPane2 = new JScrollPane();
		xmlDocument = new JTextPane();
		tabbedPane1 = new JTabbedPane();
		panel1 = new JPanel();
		translateButton = new JButton();
		saveDocumentButton = new JButton();
		progressBar = new JLabel();
		panel2 = new JPanel();
		splitPane1 = new JSplitPane();
		panel3 = new JPanel();
		button1 = new JButton();
		xmlFileName = new JTextField();
		label1 = new JLabel();
		panel4 = new JPanel();
		checkBox1 = new JCheckBox();

		//======== this ========
		setTitle("LaTEX > XML");
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setFocusable(false);
		setBackground(Color.white);
		setIconImage(new ImageIcon(getClass().getResource("/images/blue-home-icon.png")).getImage());
		setForeground(Color.white);
		setFont(new Font("Calibri", Font.PLAIN, 14));
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				thisWindowClosing(e);
			}
		});
		Container contentPane = getContentPane();

		//======== menuBar1 ========
		{

			//======== MainMenu ========
			{
				MainMenu.setText("File");
				MainMenu.setFont(new Font("Calibri", Font.PLAIN, 14));

				//---- chooseFileMenu ----
				chooseFileMenu.setText("Choose file");
				chooseFileMenu.setFont(new Font("Calibri", Font.PLAIN, 14));
				MainMenu.add(chooseFileMenu);
				MainMenu.addSeparator();

				//---- helpItem ----
				helpItem.setText("Help");
				helpItem.setFont(new Font("Calibri", Font.PLAIN, 14));
				MainMenu.add(helpItem);

				//---- exitItem ----
				exitItem.setText("Exit");
				exitItem.setFont(new Font("Calibri", Font.PLAIN, 14));
				MainMenu.add(exitItem);
			}
			menuBar1.add(MainMenu);
		}
		setJMenuBar(menuBar1);

		//======== spliPaneWithDoc ========
		{
			spliPaneWithDoc.setOneTouchExpandable(true);

			//======== documentTab ========
			{

				//======== panel7 ========
				{
					panel7.setLayout(new BoxLayout(panel7, BoxLayout.X_AXIS));

					//======== scrollPane3 ========
					{
						scrollPane3.setViewportView(documentText);
					}
					panel7.add(scrollPane3);
				}
				documentTab.addTab("Document:", panel7);

			}
			spliPaneWithDoc.setLeftComponent(documentTab);

			//======== xmlPane ========
			{

				//======== panel6 ========
				{
					panel6.setLayout(new BorderLayout());

					//======== scrollPane2 ========
					{
						scrollPane2.setViewportView(xmlDocument);
					}
					panel6.add(scrollPane2, BorderLayout.CENTER);
				}
				xmlPane.addTab("XML:", panel6);

			}
			spliPaneWithDoc.setRightComponent(xmlPane);
		}

		//======== tabbedPane1 ========
		{
			tabbedPane1.setFont(new Font("Calibri", Font.PLAIN, 14));

			//======== panel1 ========
			{

				//---- translateButton ----
				translateButton.setText("Translate");
				translateButton.setFont(new Font("Calibri", Font.PLAIN, 14));
				translateButton.setIcon(new ImageIcon(getClass().getResource("/images/blue-document-plus-icon.png")));

				//---- saveDocumentButton ----
				saveDocumentButton.setText("Save document");
				saveDocumentButton.setFont(new Font("Calibri", Font.PLAIN, 14));
				saveDocumentButton.setIcon(new ImageIcon(getClass().getResource("/images/blue-disk-icon.png")));

				GroupLayout panel1Layout = new GroupLayout(panel1);
				panel1.setLayout(panel1Layout);
				panel1Layout.setHorizontalGroup(
					panel1Layout.createParallelGroup()
						.addGroup(panel1Layout.createSequentialGroup()
							.addContainerGap()
							.addComponent(translateButton)
							.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
							.addComponent(saveDocumentButton)
							.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
							.addComponent(progressBar)
							.addContainerGap(457, Short.MAX_VALUE))
				);
				panel1Layout.setVerticalGroup(
					panel1Layout.createParallelGroup()
						.addGroup(panel1Layout.createSequentialGroup()
							.addContainerGap()
							.addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
								.addComponent(translateButton)
								.addComponent(saveDocumentButton)
								.addComponent(progressBar))
							.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
				);
			}
			tabbedPane1.addTab("Main", panel1);


			//======== panel2 ========
			{

				//======== splitPane1 ========
				{

					//======== panel3 ========
					{

						//---- button1 ----
						button1.setText("Set XML file name");
						button1.setFont(new Font("Calibri", Font.PLAIN, 14));

						//---- xmlFileName ----
						xmlFileName.setFont(new Font("Calibri", xmlFileName.getFont().getStyle(), 14));

						//---- label1 ----
						label1.setText("XML result file name:");
						label1.setFont(new Font("Calibri", label1.getFont().getStyle(), 14));

						GroupLayout panel3Layout = new GroupLayout(panel3);
						panel3.setLayout(panel3Layout);
						panel3Layout.setHorizontalGroup(
							panel3Layout.createParallelGroup()
								.addGroup(panel3Layout.createSequentialGroup()
									.addContainerGap()
									.addComponent(label1)
									.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
									.addComponent(xmlFileName, GroupLayout.PREFERRED_SIZE, 191, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
									.addComponent(button1)
									.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
						);
						panel3Layout.setVerticalGroup(
							panel3Layout.createParallelGroup()
								.addGroup(panel3Layout.createSequentialGroup()
									.addContainerGap()
									.addGroup(panel3Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
										.addComponent(label1)
										.addComponent(xmlFileName, GroupLayout.DEFAULT_SIZE, 27, Short.MAX_VALUE)
										.addComponent(button1))
									.addGap(25, 25, 25))
						);
					}
					splitPane1.setLeftComponent(panel3);

					//======== panel4 ========
					{

						//---- checkBox1 ----
						checkBox1.setText("Replace symbol using ISO standarts");
						checkBox1.setFont(new Font("Calibri", Font.PLAIN, 14));

						GroupLayout panel4Layout = new GroupLayout(panel4);
						panel4.setLayout(panel4Layout);
						panel4Layout.setHorizontalGroup(
							panel4Layout.createParallelGroup()
								.addGroup(panel4Layout.createSequentialGroup()
									.addContainerGap()
									.addComponent(checkBox1)
									.addContainerGap(28, Short.MAX_VALUE))
						);
						panel4Layout.setVerticalGroup(
							panel4Layout.createParallelGroup()
								.addGroup(panel4Layout.createSequentialGroup()
									.addContainerGap()
									.addComponent(checkBox1)
									.addContainerGap(16, Short.MAX_VALUE))
						);
					}
					splitPane1.setRightComponent(panel4);
				}

				GroupLayout panel2Layout = new GroupLayout(panel2);
				panel2.setLayout(panel2Layout);
				panel2Layout.setHorizontalGroup(
					panel2Layout.createParallelGroup()
						.addComponent(splitPane1, GroupLayout.DEFAULT_SIZE, 737, Short.MAX_VALUE)
				);
				panel2Layout.setVerticalGroup(
					panel2Layout.createParallelGroup()
						.addGroup(panel2Layout.createSequentialGroup()
							.addComponent(splitPane1, GroupLayout.PREFERRED_SIZE, 50, GroupLayout.PREFERRED_SIZE)
							.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
				);
			}
			tabbedPane1.addTab("Settings", panel2);

		}

		GroupLayout contentPaneLayout = new GroupLayout(contentPane);
		contentPane.setLayout(contentPaneLayout);
		contentPaneLayout.setHorizontalGroup(
			contentPaneLayout.createParallelGroup()
				.addComponent(tabbedPane1, GroupLayout.DEFAULT_SIZE, 742, Short.MAX_VALUE)
				.addGroup(contentPaneLayout.createSequentialGroup()
					.addComponent(spliPaneWithDoc, GroupLayout.DEFAULT_SIZE, 732, Short.MAX_VALUE)
					.addContainerGap())
		);
		contentPaneLayout.setVerticalGroup(
			contentPaneLayout.createParallelGroup()
				.addGroup(contentPaneLayout.createSequentialGroup()
					.addComponent(tabbedPane1, GroupLayout.PREFERRED_SIZE, 85, GroupLayout.PREFERRED_SIZE)
					.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
					.addComponent(spliPaneWithDoc, GroupLayout.DEFAULT_SIZE, 316, Short.MAX_VALUE))
		);
		setSize(750, 455);
		setLocationRelativeTo(getOwner());
        // //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY
    // //GEN-BEGIN:variables
	private JMenuBar menuBar1;
	private JMenu MainMenu;
	private JMenuItem chooseFileMenu;
	private JMenuItem helpItem;
	private JMenuItem exitItem;
	private JSplitPane spliPaneWithDoc;
	private JTabbedPane documentTab;
	private JPanel panel7;
	private JScrollPane scrollPane3;
	private JTextPane documentText;
	private JTabbedPane xmlPane;
	private JPanel panel6;
	private JScrollPane scrollPane2;
	private JTextPane xmlDocument;
	private JTabbedPane tabbedPane1;
	private JPanel panel1;
	private JButton translateButton;
	private JButton saveDocumentButton;
	private JLabel progressBar;
	private JPanel panel2;
	private JSplitPane splitPane1;
	private JPanel panel3;
	private JButton button1;
	private JTextField xmlFileName;
	private JLabel label1;
	private JPanel panel4;
	private JCheckBox checkBox1;
    // JFormDesigner - End of variables declaration //GEN-END:variables
}