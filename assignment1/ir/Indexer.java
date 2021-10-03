/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.*;


/**
 *   Processes a directory structure and indexes all PDF and text files.
 */
public class Indexer {

    /** The index to be built up by this Indexer. */
    Index index;

    /** K-gram index to be built up by this Indexer */
    KGramIndex kgIndex;

    /** The next docID to be generated. */
    private int lastDocID = 0;

    /** The patterns matching non-standard words (e-mail addresses, etc.) */
    String patterns_file;

    Set<String> terms = new HashSet<String>();

    /* ----------------------------------------------- */


    /** Constructor */
    public Indexer( Index index, KGramIndex kgIndex, String patterns_file ) {
        this.index = index;
        this.kgIndex = kgIndex;
        this.patterns_file = patterns_file;
    }


    /** Generates a new document identifier as an integer. */
    private int generateDocID() {
        return lastDocID++;
    }



    /**
     *  Tokenizes and indexes the file @code{f}. If <code>f</code> is a directory,
     *  all its files and subdirectories are recursively processed.
     */
    public void processFiles( File f, boolean is_indexing ) {
        // do not try to index fs that cannot be read
        if (is_indexing) {
            if ( f.canRead() ) {
                if ( f.isDirectory() ) {
                    String[] fs = f.list();
                    // an IO error could occur
                    if ( fs != null ) {
                        for ( int i=0; i<fs.length; i++ ) {
                            processFiles( new File( f, fs[i] ), is_indexing );
                        }
                    }
                } else {
                    // First register the document and get a docID
                    int docID = generateDocID();
                    if ( docID%1000 == 0 ) System.err.println( "Indexed " + docID + " files" );
                    try {
                        Reader reader = new InputStreamReader( new FileInputStream(f), StandardCharsets.UTF_8 );
                        Tokenizer tok = new Tokenizer( reader, true, false, true, patterns_file );
                        
                        int offset = 0;
                        while ( tok.hasMoreTokens() ) {
                            String token = tok.nextToken();
                            insertIntoIndex( docID, token, offset++ );

                            terms.add(token);
                        }
                        index.docNames.put( docID, f.getPath() );
                        index.docLengths.put( docID, offset );
                        index.docIDs.put( getFileName(f.getPath()), docID);
                        reader.close();
                    } catch ( IOException e ) {
                        System.err.println( "Warning: IOException during indexing." );
                    }
                }
            }
        }
    }

    public String getFileName(String path) {
        String result = "";
        StringTokenizer tok = new StringTokenizer(path, "\\/");
        while (tok.hasMoreTokens()) {
            result = tok.nextToken();
        }
        return result;
    }

    public void write_lengths_to_disk(){
        Map<Integer, HashMap<String,Integer>> dict = new HashMap<Integer, HashMap<String,Integer>>();
        for (String term: terms){
            PostingsList list = this.index.getPostings(term);
            for(int i=0;i<list.size();i++){
                int docID = list.get(i).docID;
                if (!dict.containsKey(docID)) {
                    HashMap<String,Integer> map = new HashMap<>();
                    map.put(term, 1);
                    dict.put(docID,map);
                }
                else {
                    if(!dict.get(docID).containsKey(term)) dict.get(docID).put(term, 1);
                    else{
                        int val = dict.get(docID).get(term);
                        dict.get(docID).put(term, val+1);
                    }
                }
            }
        }

        Map<Integer,Double> map = new HashMap<Integer,Double>();

        int N = this.index.docLengths.size();
        
        for(int docID: dict.keySet()) {
            double temp = 0;

            for(String term: dict.get(docID).keySet()){

                int tf_dt = dict.get(docID).get(term);

                int df_t = this.index.getPostings(term).get_filtered_list().size();
                double idf_t = Math.log(N / df_t);

                double tfidf_term = tf_dt * idf_t;

                temp += Math.pow(tfidf_term, 2);
            }

            temp = Math.sqrt(temp);
            map.put(docID,temp);
        }

        // print(map);

        File file = new File("lengths.txt");
        BufferedWriter bf = null; 
        try { 
  
            // create new BufferedWriter for the output file 
            bf = new BufferedWriter(new FileWriter(file)); 
  
            // iterate map entries 
            Iterator it = map.entrySet().iterator();
            while ( it.hasNext() ) { 

                Map.Entry entry = (Map.Entry)it.next();

                // put key and value separated by a colon 
                bf.write(entry.getKey() + ":" + entry.getValue());
                // new line 
                bf.newLine(); 
            } 
  
            bf.flush(); 
        } 
        catch (IOException e) { 
            e.printStackTrace(); 
        } 
        finally { 
            try { 
                bf.close(); 
            } 
            catch (Exception e) { 
            } 
        } 

    }

    /* ----------------------------------------------- */


    /**
     *  Indexes one token.
     */
    public void insertIntoIndex( int docID, String token, int offset ) {
        index.insert( token, docID, offset );
        if (kgIndex != null)
            kgIndex.insert(token);
    }

    public int get_size(){
        return index.get_size();
    }

    public void print(Object o){
        System.out.println(o);
    }
}

