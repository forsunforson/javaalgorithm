package swingWorker;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * 依据《Java核心技术I》所做的练习
 * @version 0.01 2018-09-28
 * @author yeweiyi
 */

public class SwingWorkerTest {
    public static void main(String[] args) throws Exception {
        EventQueue.invokeLater(() -> {
            JFrame frame = new SwingWorkerFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
        });
    }
}

/**
 * frame用于显示文本文件，并在打开过程中显示进度
 */
class SwingWorkerFrame extends JFrame {
    private JFileChooser chooser;
    private JTextArea textArea;
    private JLabel statusLine;
    private JMenuItem openItem;
    private JMenuItem cancelItem;
    private SwingWorker<StringBuilder, ProgressData> textReader;
    public static final int TEXT_ROWS = 20;
    public static final int TEXT_COLS = 60;

    public  SwingWorkerFrame() {
        chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("."));

        textArea = new JTextArea(TEXT_ROWS, TEXT_COLS);
        add(new JScrollPane(textArea));

        statusLine = new JLabel(" ");
        add(statusLine, BorderLayout.SOUTH);

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu menu = new JMenu("File");
        menu.setMnemonic('F');
        menuBar.add(menu);

        openItem = new JMenuItem("Open");
        openItem.setAccelerator(KeyStroke.getKeyStroke("ctrl O"));
        menu.add(openItem);
        openItem.addActionListener(event -> {
            int result = chooser.showOpenDialog(null);

            if(result == JFileChooser.APPROVE_OPTION) {
                textArea.setText("");
                openItem.setEnabled(false);
                textReader = new TextReader(chooser.getSelectedFile());
                textReader.execute();
                cancelItem.setEnabled(true);
            }
        });

        cancelItem = new JMenuItem("Cancel");
        cancelItem.setAccelerator(KeyStroke.getKeyStroke("ctrl C"));
        menu.add(cancelItem);
        cancelItem.setEnabled(false);
        cancelItem.addActionListener(event -> textReader.cancel(true));

        pack();
    }

    private class ProgressData {
        public int number;
        public String line;
    }

    private class TextReader extends SwingWorker<StringBuilder, ProgressData> {
        private File file;
        private StringBuilder text = new StringBuilder();

        public TextReader(File file) {
            this.file = file;
        }

        @Override
        public StringBuilder doInBackground() throws IOException, InterruptedException {
            int lineNumber = 0;
            try (Scanner in = new Scanner(new FileInputStream(file), "UTF-8")) {
                while(in.hasNext()) {
                    String line = in.nextLine();
                    lineNumber++;
                    text.append(line).append("\n");
                    ProgressData data = new ProgressData();
                    data.number = lineNumber;
                    data.line = line;
                    publish(data);
                    Thread.sleep(1);
                }
            }
            return text;
        }

        @Override
        public void process(List<ProgressData> data) {
            if(isCancelled()) return;
            StringBuilder b = new StringBuilder();
            statusLine.setText("" + data.get(data.size()-1).number);
            for (ProgressData d : data) b.append(d.line).append("\n");
            textArea.append(b.toString());
        }

        @Override
        public  void done() {
            try {
                StringBuilder result = get();
                textArea.setText(result.toString());
                statusLine.setText("Done");
            }
            catch (InterruptedException ex) {}
            catch (CancellationException ex) {
                textArea.setText("");
                statusLine.setText("Cancelled");
            }
            catch (ExecutionException ex) {
                statusLine.setText(""+ex.getCause());
            }

            cancelItem.setEnabled(false);
            openItem.setEnabled(true);
        }
    }
}


