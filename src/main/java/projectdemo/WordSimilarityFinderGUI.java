package projectdemo;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
// Removed unused imports for training
// import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
// import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
// import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
// import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
// import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

public class WordSimilarityFinderGUI extends JFrame {

    private static final Logger log = LoggerFactory.getLogger(WordSimilarityFinderGUI.class);
    private Word2Vec vec; // This will hold the loaded model
    private final JTextField inputField;
    private final JTextArea outputArea;
    private final JButton findButton;
    private final JButton showVocabButton;
    private final JLabel statusLabel;
    private final AtomicBoolean isModelReady = new AtomicBoolean(false); // Renamed from isModelTrained
    private static final int NUM_NEIGHBORS = 5;

    public WordSimilarityFinderGUI() {
        super("Word2Vec Similarity Finder (DL4J)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        inputField = new JTextField(25);
        findButton = new JButton("Find Similar Words");
        findButton.setEnabled(false);
        showVocabButton = new JButton("Show Vocabulary");
        showVocabButton.setEnabled(false);
        inputPanel.add(new JLabel("Enter Word:"));
        inputPanel.add(inputField);
        inputPanel.add(findButton);
        inputPanel.add(showVocabButton);
        statusLabel = new JLabel("STATUS: Loading pre-trained model. This will take a few minutes...");
        statusLabel.setForeground(Color.BLUE);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        outputArea = new JTextArea(15, 50);
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        outputArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        outputArea.setText("Welcome! The pre-trained Word2Vec model is loading...\n"
            + "This is a one-time load and can take 1-3 minutes as the file is very large.\n");

        JScrollPane scrollPane = new JScrollPane(outputArea);
        setLayout(new BorderLayout(10, 10));
        add(inputPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        findButton.addActionListener(e -> findSimilarWords());
        inputField.addActionListener(e -> findSimilarWords());
        showVocabButton.addActionListener(e -> showVocabulary());
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        startModelLoading();
    }
    private void findSimilarWords() {
        if (!isModelReady.get()) {
            outputArea.setText("Model is not loaded yet. Please wait.");
            return;
        }
        String word = inputField.getText().trim().toLowerCase();
        if (word.isEmpty()) {
            outputArea.setText("Please enter a word.");
            return;
        }
        if (!vec.hasWord(word)) {
            outputArea.setText("Word '" + word + "' was not found in the vocabulary.");
            return;
        }
        Collection<String> similarWords = vec.wordsNearest(word, NUM_NEIGHBORS);
        StringBuilder results = new StringBuilder();
        results.append("Words similar to '").append(word).append("':\n");
        results.append("--------------------------------------\n");
        for (String s : similarWords) {
            results.append("- ").append(s).append("\n");
        }
        outputArea.setText(results.toString());
        inputField.selectAll();
    }

    private void showVocabulary() {
        if (!isModelReady.get()) {
            outputArea.setText("Model is not loaded yet. Please wait.");
            return;
        }
       
        Collection<String> vocabCollection = vec.vocab().words();
        
        JTextArea vocabArea = new JTextArea(25, 30);
        vocabArea.setEditable(false);
        vocabArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        vocabArea.setText("Vocabulary loaded successfully.\n");
        vocabArea.append("Total words in model: " + vocabCollection.size() + "\n\n");
        vocabArea.append("(Listing all 3 million words is disabled to prevent crashing.)");
        
        vocabArea.setCaretPosition(0);
        JScrollPane vocabScrollPane = new JScrollPane(vocabArea);
        JDialog vocabDialog = new JDialog(this, "Model Vocabulary", false);
        vocabDialog.setLayout(new BorderLayout());
        vocabDialog.add(vocabScrollPane, BorderLayout.CENTER);
        vocabDialog.pack();
        vocabDialog.setLocationRelativeTo(this);
        vocabDialog.setVisible(true);
    }
    private void startModelLoading() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                log.info("Background thread: Starting model loading...");
                File modelFile = new File("GoogleNews-vectors-negative300.bin");

                if (!modelFile.exists()) {
                    log.error("Model file not found!");
                    throw new java.io.FileNotFoundException(
                        "Model file not found at: " + modelFile.getAbsolutePath()
                        + "\nPlease download 'GoogleNews-vectors-negative300.bin.gz', "
                        + "unzip it, and place it in the project root folder."
                    );
                }
                vec = WordVectorSerializer.readWord2VecModel(modelFile);
                
                log.info("Background thread: Model loading complete.");
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); 
                    isModelReady.set(true);
                    findButton.setEnabled(true);
                    showVocabButton.setEnabled(true);
                    statusLabel.setText("STATUS: Model loaded successfully. Ready.");
                    statusLabel.setForeground(new Color(0, 128, 0)); // Green color
                    outputArea.append("\n...Model is loaded and ready!");
                } catch (Exception e) {
                    log.error("Failed to load pre-trained model", e);
                    statusLabel.setText("STATUS: Error loading model. See logs.");
                    statusLabel.setForeground(Color.RED);
                    outputArea.setText("Failed to load model:\n" + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new WordSimilarityFinderGUI();
        });
    }
}