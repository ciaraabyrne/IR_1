package org.example;
import java.io.File;
import java.io.IOException;

import java.util.ArrayList;

import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;


// import com.sun.tools.javac.util.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;

// for ( all chars in string){
// if ( string == .I){ article number }
// if ( string == .T) { article title)
// if ( string == .A) { article author}
// if ( string == .B) { article journal }
// if ( string == .W) { article abstract}
//}

public class CreateIndex {
    // Directory where the search index will be saved
    private static String INDEX_DIRECTORY = "../index";
    private static int MAX_RESULTS = 10;

    public static void indexInput(String[] file) throws IOException {
        // Analyzer that is used to process TextField
        Analyzer analyzer = new StandardAnalyzer();

        // ArrayList of documents in the corpus
        ArrayList<Document> documents = new ArrayList<Document>();

        // Open the directory that contains the search index
        Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));

        // Set up an index writer to add process and save documents to the index
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter iwriter = new IndexWriter(directory, config);

        for (String arg : file) {
            if(arg.endsWith("cran.all.1400")){
            System.out.printf("Indexing \"%s\"\n", arg);
            String content = new String(Files.readAllBytes(Paths.get(arg)));
            String[] tokens = content.split("(?=\\.I\\s*)");
            //  System.out.print(Arrays.toString(stngArray));

            for (String i : tokens) {
                //System.out.print(Arrays.toString((i).toCharArray()));
                // "(?<=[\s*\.[TABW]\s*])"
                //     String splits[] = i.split("\\s*\\.[TABW]\\s*"); // split if theres a dot followed by tab
                //  System.out.println(Arrays.toString(splits));
                // Load the contents of the file
                String splits[] = i.split("(?=\\s*.[TABW]\\s*)");
                int p = 0;

                Document doc = new Document();
                int y = 0;
                for (String x : splits) {

                    if (x.startsWith(".I")) {
                        // System.out.println(x);
                        doc.add(new TextField(".I", x, Field.Store.YES));
                    } else if (x.startsWith(".T")) {
                        doc.add(new TextField(".T", x, Field.Store.YES));
                    } else if (x.startsWith(".A")) {
                        doc.add(new TextField(".A", x, Field.Store.YES));
                    } else if (x.startsWith(".B")) {
                        doc.add(new TextField(".B", x, Field.Store.YES));
                        y++;
                    } else if (x.startsWith(".W")) {
                        doc.add(new TextField(".W", x, Field.Store.YES));
                    }
                }
                // Add the file to our linked list
                documents.add(doc);
            }
        }
    }
        // Write all the documents in the linked list to the search index
        //   System.out.println(documents.toString());
        iwriter.addDocuments(documents);

        // Commit everything and close
        iwriter.close();
        directory.close();
    }

    public static void queryIndexInteractive() throws IOException, ParseException {
        // Analyzer used by the query parser.
        // Must be the same as the one used when creating the index
        Analyzer analyzer = new StandardAnalyzer();

        // Open the folder that contains our search index
        Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));

        // create objects to read and search across the index
        DirectoryReader ireader = DirectoryReader.open(directory);
        IndexSearcher isearcher = new IndexSearcher(ireader);

        // Create the query parser. The default search field is "content", but
        // we can use this to search across any field
        String[] fields = {".I", ".T", ".A", ".B", ".W"};

        QueryParser parser = new MultiFieldQueryParser(fields, analyzer);

        String queryString = "";
        Scanner scanner = new Scanner(System.in);
        do {
            // trim leading and trailing whitespace from the query
            queryString = queryString.trim();

            // if the user entered a querystring
            if (queryString.length() > 0) {
                // parse the query with the parser
                Query query = parser.parse(queryString);

                // Get the set of results
                ScoreDoc[] hits = isearcher.search(query, MAX_RESULTS).scoreDocs;

                // Print the results
                System.out.println("Documents: " + hits.length);
                for (int i = 0; i < hits.length; i++) {
                    Document hitDoc = isearcher.doc(hits[i].doc);
                    System.out.println(i + ") " + hitDoc.get(".I") + " " + hits[i].score);
                }

                System.out.println();
            }

            // prompt the user for input and quit the loop if they escape
            System.out.print(">>> ");
            queryString = scanner.nextLine();
        } while (!queryString.equals("\\q"));

        // close everything and quit
        ireader.close();
        directory.close();

    }

    public static void queryIndex(String[] qryfile) throws IOException, ParseException {
        Analyzer analyzer = new StandardAnalyzer();

        // Open the folder that contains our search index
        Directory directory = FSDirectory.open(Paths.get(INDEX_DIRECTORY));

        // create objects to read and search across the index
        DirectoryReader ireader = DirectoryReader.open(directory);
        IndexSearcher isearcher = new IndexSearcher(ireader);

        // Create the query parser. The default search field is "content", but
        // we can use this to search across any field
        String[] fields = {".I", ".W"};

        QueryParser parser = new MultiFieldQueryParser(fields, analyzer);
       // String queryString = "";
        String[] tokens;
        parser.setAllowLeadingWildcard(true); // can be expensive to allow this
        //   Scanner scanner = new Scanner(System.in);
        for (String arg : qryfile) {
            if (arg.endsWith("cran.qry")) {
                System.out.printf("Indexing Queries\"%s\"\n", arg);
                String content = new String(Files.readAllBytes(Paths.get(arg)));
                tokens = content.split("(?=\\.I\\s*)");
                for (String i : tokens) {
                    String splits[] = i.split("\\s*.[W]\\s*");
                 //   System.out.println(Arrays.toString(splits));
                    for (String x : splits) {

                        if (x.startsWith(".I")) {

                            System.out.println("----I.=" + x);
                        } else {

                            // trim leading and trailing whitespace from the query
                            x = x.trim();

                            // if the user entered a querystring
                            if (x.length() > 0) {
                                // parse the query with the parser
                                Query query = parser.parse(x);

                                // Get the set of results
                                ScoreDoc[] hits = isearcher.search(query, MAX_RESULTS).scoreDocs;

                                // Print the results
                                System.out.println("Documents: " + hits.length);
                                for (int j = 0; j < hits.length; j++) {
                                    Document hitDoc = isearcher.doc(hits[j].doc);
                                    System.out.println(j + ") " + hitDoc.get(".I") + " " + hits[j].score);
                                }

                                System.out.println();
                            }


                        }
                    }

                    }

                }
            }

        // close everything and quit
        ireader.close();
        directory.close();

        }


    public static void main(String[] args) throws IOException, ParseException {
        // Make sure we were given something to index
        if (args.length <= 0) {
            System.out.println("Expected corpus as input");
            System.exit(1);
        }
        System.out.println("Argument count: " + args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.println("Argument " + i + ": " + args[i]);

            }

            indexInput(args);
            queryIndex(args);

        }




      //  queryIndexInteractive();

    }



