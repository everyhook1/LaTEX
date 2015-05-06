package com.kutsyk.windows;

import com.kutsyk.security.AES;
import net.sf.saxon.functions.Parse;

import java.awt.*;
import java.io.*;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import javax.swing.*;
import javax.swing.GroupLayout;
import javax.swing.LayoutStyle;
import javax.swing.border.*;
/*
 * Created by JFormDesigner on Tue May 05 16:26:31 EEST 2015
 */


/**
 * @author kutsyk
 */
public class TrialWindow extends JFrame {

    private File securityDateFile;
    private String dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private String key = "1a25s8fe5dsg65ad";
//    private String key = "WjgJ8Td76u2Ah42A";
    private byte[] byte_key;

    public TrialWindow() {
        byte_key = key.getBytes();
        trialVersionCheck();
        initComponents();
    }

    private void trialVersionCheck() {
        writeDateToFile();
        checkSecurity();
    }

    private void writeDateToFile() {
        try {
            File appFolder = new File(System.getenv("APPDATA") + "/Charlesworth");
            if (!appFolder.exists())
                appFolder.mkdir();
            securityDateFile = new File(System.getenv("APPDATA") + "/Charlesworth/.security");
            if (!securityDateFile.exists())
                securityDateFile.createNewFile();
            addDateToFile(securityDateFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addDateToFile(File securityDateFile) throws IOException{
        PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(securityDateFile, true)));
        writer.append("LaTEX"+encryptDate());
        writer.close();
    }

    private String encryptDate(){
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        return sdf.format(today);
    }

    private boolean checkSecurity() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(securityDateFile));
            String line;
            StringBuilder content = new StringBuilder();
            while ((line = reader.readLine()) != null)
                content.append(line);
            reader.close();
            String datesLine = content.toString();
            if (datesLine.isEmpty())
                return true;
            else
                return checkDates(datesLine);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean checkDates(String datesLine) {
        String[] stringDates = datesLine.split("LaTEX");
        ArrayList<Date> dates = new ArrayList<>();
        for (int i=1;i<stringDates.length;++i) {
            String date = stringDates[i];
            try {
                dates.add(dateFromString(date));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        for (Date d : dates)
            System.out.println(d.toString());
        return false;
    }

    private Date decryptDate(String date) throws ParseException{
        return dateFromString(date);
    }

    private Date dateFromString(String strDate) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        return sdf.parse(strDate);
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        trialProgressBar = new JProgressBar();
        okButton = new JButton();

        //======== this ========
        setTitle("Trial LaTEX");
        setFont(new Font("Times New Roman", Font.PLAIN, 14));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Container contentPane = getContentPane();

        //---- okButton ----
        okButton.setText("Ok");
        okButton.setFont(new Font("Times New Roman", Font.PLAIN, 14));

        GroupLayout contentPaneLayout = new GroupLayout(contentPane);
        contentPane.setLayout(contentPaneLayout);
        contentPaneLayout.setHorizontalGroup(
                contentPaneLayout.createParallelGroup()
                        .addGroup(GroupLayout.Alignment.TRAILING, contentPaneLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(contentPaneLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                                        .addComponent(okButton, GroupLayout.PREFERRED_SIZE, 106, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(trialProgressBar, GroupLayout.DEFAULT_SIZE, 372, Short.MAX_VALUE))
                                .addContainerGap())
        );
        contentPaneLayout.setVerticalGroup(
                contentPaneLayout.createParallelGroup()
                        .addGroup(contentPaneLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(trialProgressBar, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(okButton, GroupLayout.DEFAULT_SIZE, 30, Short.MAX_VALUE)
                                .addContainerGap())
        );
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JProgressBar trialProgressBar;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
