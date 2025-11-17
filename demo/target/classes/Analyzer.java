
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.trees.Tree;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI; // Import a new class
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.stream.Collectors;

public class Analyzer {

    // This is the one-time, expensive setup.
    private static StanfordCoreNLP pipeline;

    public static void main(String[] args) throws IOException {
        // --- 1. Check for command-line arguments ---
        if (args.length < 2) {
            System.err.println("Usage: java NlpAnalyzer <ACTION> <URL>");
            System.err.println("Example: java NlpAnalyzer POS https://www.gutenberg.org/files/1661/1661-0.txt");
            System.err.println("Actions: POS, CONSTITUENCY, DEPENDENCY");
            return;
        }

        String action = args[0].toUpperCase();
        String url = args[1];

        // --- 2. Initialize the Stanford CoreNLP pipeline ---
        // We configure it to do all the tasks we might need:
        // 'tokenize' (split text into words)
        // 'ssplit' (split text into sentences)
        // 'pos' (part-of-speech tagging)
        // 'parse' (constituency parsing)
        // 'depparse' (dependency parsing)
        System.out.println("Initializing Stanford CoreNLP (this may take a minute)...");
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, parse, depparse");
        pipeline = new StanfordCoreNLP(props);
        System.out.println("Pipeline initialized.");

        // --- 3. Process the single action and URL ---
        processUrl(action, url);
    }

    /**
     * Fetches, cleans, and processes the text from a single URL.
     */
    private static void processUrl(String action, String url) {
        System.out.printf("\n--- Task: %s | URL: %s ---\n", action, url);
        try {
            // --- 4. Fetch the text content from the URL ---
            String rawText = fetchTextFromUrl(url);

            // --- 5. Clean and sample the text ---
            // Gutenberg texts are huge. We'll clean the headers/footers
            // and take only the first 10 lines of the main content for this demo.
            String cleanText = cleanGutenbergText(rawText);
            String sampleText = getFirstNLines(cleanText, 10);
            
            System.out.println("Analyzing sample text:\n\"" + sampleText + "\"\n...");

            // --- 6. Run the NLP pipeline on the sample text ---
            CoreDocument document = new CoreDocument(sampleText);
            pipeline.annotate(document);

            // --- 7. Perform the requested action ---
            switch (action) {
                case "POS":
                    printPosTags(document);
                    break;
                case "CONSTITUENCY":
                    printConstituencyTrees(document);
                    break;
                case "DEPENDENCY":
                    printDependencyGraphs(document);
                    break;
                default:
                    System.err.println("Unknown action: " + action);
            }

        } catch (IOException e) {
            System.err.println("Failed to process URL " + url + ": " + e.getMessage());
        }
    }

    /**
     * Prints the Part-of-Speech tag for each word.
     */
    private static void printPosTags(CoreDocument document) {
        System.out.println("[Part-of-Speech (POS) Tags]");
        for (CoreSentence sentence : document.sentences()) {
            for (CoreLabel token : sentence.tokens()) {
                String word = token.word();
                String pos = token.tag();
                System.out.printf("  %s [%s]\n", word, pos);
            }
            System.out.println("  --- (end of sentence) ---");
        }
    }

    /**
     * Prints the constituency parse tree for each sentence.
     * This shows how words group into phrases.
     */
    private static void printConstituencyTrees(CoreDocument document) {
        System.out.println("[Constituency Parse Trees]");
        // 
        for (CoreSentence sentence : document.sentences()) {
            Tree tree = sentence.constituencyParse();
            // The tree.toString() is the standard (ROOT (S (NP ...) ...)) format
            System.out.println(tree);
        }
    }

    /**
     * Prints the dependency graph for each sentence.
     * This shows the grammatical relationships between words.
     */
    private static void printDependencyGraphs(CoreDocument document) {
        System.out.println("[Dependency Parse Graphs]");
        // 
        for (CoreSentence sentence : document.sentences()) {
            SemanticGraph dependencies = sentence.dependencyParse();
            // We'll print in the "list" format, e.g., "nsubj(cat-2, The-1)"
            System.out.println(dependencies.toString(SemanticGraph.OutputFormat.LIST));
        }
    }

    /**
     * Simple utility to get the first N lines of a text block.
     */
    private static String getFirstNLines(String text, int n) {
        return text.lines().limit(n).collect(Collectors.joining("\n"));
    }

    /**
     * A very basic utility to strip the Project Gutenberg headers/footers.
     */
    private static String cleanGutenbergText(String rawText) {
        String startMarker = "*** START OF THIS PROJECT GUTENBERG EBOOK";
        String endMarker = "*** END OF THIS PROJECT GUTENBERG EBOOK";

        int startIndex = rawText.indexOf(startMarker);
        if (startIndex != -1) {
            // Find the end of that line
            startIndex = rawText.indexOf('\n', startIndex);
        }
        if (startIndex == -1) {
            startIndex = 0; // Not found, just use the start
        }

        int endIndex = rawText.indexOf(endMarker);
        if (endIndex == -1) {
            endIndex = rawText.length(); // Not found, use the end
        }

        return rawText.substring(startIndex, endIndex).trim();
    }

    /**
     * Fetches raw text from a given URL.
     * Handles HTTP redirects (e.g., http to https).
     */
    private static String fetchTextFromUrl(String urlString) throws IOException {
        try {
            // Fix for deprecation warning: Use URI.create().toURL() instead of new URL()
            URL url = URI.create(urlString).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(true); // Follow redirects
        try (InputStream inputStream = conn.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            
            return reader.lines().collect(Collectors.joining("\n"));
        }
        } catch (java.net.URISyntaxException e) {
            // Handle the case where the URL string is invalid (part of the fix)
            throw new IOException("Invalid URL syntax: " + urlString, e);
        }
    }
}