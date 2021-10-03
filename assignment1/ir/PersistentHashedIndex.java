/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, KTH, 2018
 */  

package ir;

import java.io.*;
import java.util.*;
import java.nio.charset.*;


/*
 *   Implements an inverted index as a hashtable on disk.
 *   
 *   Both the words (the dictionary) and the data (the postings list) are
 *   stored in RandomAccessFiles that permit fast (almost constant-time)
 *   disk seeks. 
 *
 *   When words are read and indexed, they are first put in an ordinary,
 *   main-memory HashMap. When all words are read, the index is committed
 *   to disk.
 */
public class PersistentHashedIndex implements Index {

    /** The directory where the persistent index files are stored. */
    public static final String INDEXDIR = "./index";

    /** The dictionary file name */
    public static final String DICTIONARY_FNAME = "dictionary";

    /** The dictionary file name */
    public static final String DATA_FNAME = "data";

    /** The terms file name */
    public static final String TERMS_FNAME = "terms";

    /** The doc info file name */
    public static final String DOCINFO_FNAME = "docInfo";

    /** The dictionary hash table on disk can fit this many entries. */
    public static final long TABLESIZE = 611953L;

    /** The dictionary hash table is stored in this file. */
    RandomAccessFile dictionaryFile;

    /** The data (the PostingsLists) are stored in this file. */
    RandomAccessFile dataFile;

    /** Pointer to the first free memory cell in the data file. */
    long free = 0L;

    /** The cache as a main-memory hash map. */
    HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();

    private ArrayList<Long> dict_entries_taken = new ArrayList<Long>();
    private int max_key_size = 20; // 100

    // ===================================================================

    /**
     *   A helper class representing one entry in the dictionary hashtable.
     */ 
    public class Entry {
        //
        //  YOUR CODE HERE
        //
        long ptr;
        int size;
        int sum;
        public Entry(long ptr,int sum,int size){
        // public Entry(long ptr,int size){
            this.ptr = ptr;
            this.size = size;
            this.sum = sum;
        }

        @Override
        public String toString() {
            String temp = String.valueOf(this.get_ptr()) + "," + String.valueOf(this.get_sum()) + "," + String.valueOf(this.get_size());
            // String temp = String.valueOf(this.get_ptr()) + "," + String.valueOf(this.get_size());
            if(temp.getBytes().length<max_key_size) {
                temp+=",";
                while(temp.length()<max_key_size) temp+='n';
            }
            return temp;
        }

        public long get_ptr(){
            return this.ptr;
        }

        public int get_size(){
            return this.size;
        }

        public int get_sum(){
            return this.sum;
        }
    }

    // ==================================================================

    
    /**
     *  Constructor. Opens the dictionary file and the data file.
     *  If these files don't exist, they will be created. 
     */
    public PersistentHashedIndex() {
        try {
            dictionaryFile = new RandomAccessFile( INDEXDIR + "/" + DICTIONARY_FNAME, "rw" );
            dataFile = new RandomAccessFile( INDEXDIR + "/" + DATA_FNAME, "rw" );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

        try {
            readDocInfo();
        } catch ( FileNotFoundException e ) {
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     *  Writes data to the data file at a specified place.
     *
     *  @return The number of bytes written.
     */ 
    int writeData( String dataString, long ptr ) {
        try {
            dataFile.seek( ptr ); 
            byte[] data = dataString.getBytes();
            dataFile.write( data );
            return data.length;
        } catch ( IOException e ) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     *  Reads data from the data file
     */ 
    String readData( long ptr, int size ) {
        try {
            dataFile.seek( ptr );
            byte[] data = new byte[size];
            dataFile.readFully( data );
            return new String(data);
        } catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================
    //
    //  Reading and writing to the dictionary file.

    /*
     *  Writes an entry to the dictionary hash table file. 
     *
     *  @param entry The key of this entry is assumed to have a fixed length
     *  @param ptr   The place in the dictionary file to store the entry
     */
    void writeEntry( Entry entry, long ptr ) {
        //
        //  YOUR CODE HERE
        //
        try {
            dictionaryFile.seek( ptr );
            dictionaryFile.writeBytes(entry.toString());
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    /**
     *  Reads an entry from the dictionary file.
     *
     *  @param ptr The place in the dictionary file where to start reading.
     */
    Entry readEntry( long ptr ) {
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE 
        //

        try{
            dictionaryFile.seek(ptr);
            byte[] data = new byte[max_key_size];
            dictionaryFile.readFully( data );
            String result = new String(data);
            List<String> entry_list = Arrays.asList(result.split(","));
            Entry entry = new Entry(Math.abs(Long.parseLong(entry_list.get(0))),Integer.parseInt(entry_list.get(1)),Integer.parseInt(entry_list.get(2)));
            // Entry entry = new Entry(Math.abs(Long.parseLong(entry_list.get(0))),Integer.parseInt(entry_list.get(1)));
            return entry;
        
        } catch(IOException e){
            e.printStackTrace();
            return null;
        }
    }


    // ==================================================================

    /**
     *  Writes the document names and document lengths to file.
     *
     * @throws IOException  { exception_description }
     */
    private void writeDocInfo() throws IOException {
        FileOutputStream fout = new FileOutputStream( INDEXDIR + "/docInfo" );
        for (Map.Entry<Integer,String> entry : docNames.entrySet()) {
            Integer key = entry.getKey();
            String docInfoEntry = key + ";" + entry.getValue() + ";" + docLengths.get(key) + "\n";
            fout.write(docInfoEntry.getBytes());
        }
        fout.close();
    }


    /**
     *  Reads the document names and document lengths from file, and
     *  put them in the appropriate data structures.
     *
     * @throws     IOException  { exception_description }
     */
    private void readDocInfo() throws IOException {
        File file = new File( INDEXDIR + "/docInfo" );
        FileReader freader = new FileReader(file);
        try (BufferedReader br = new BufferedReader(freader)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(";");
                docNames.put(new Integer(data[0]), data[1]);
                docLengths.put(new Integer(data[0]), new Integer(data[2]));
                docIDs.put(getFileName(data[1]), new Integer(data[0]));
            }
        }
        freader.close();
    }

    /**
     *  Write the index to files.
     */
    public void writeIndex() {
        System.out.println("Writing index");
        int collisions = 0;
        try {
            // Write the 'docNames' and 'docLengths' hash maps to a file
            writeDocInfo();
            int track = 0;

            // Write the dictionary and the postings list

            for(String w : this.index.keySet()){
                PostingsList w_list = this.index.get(w);
                String w_list_string = w_list.get_string_list();
                long ptr = this.free;
                int size = writeData(w_list_string,this.free);
                this.free += size;

                int sum = 0;
                for(char c: w.toCharArray()) sum += (int)c;
                Entry entry = new Entry(ptr,sum,size);
                // Entry entry = new Entry(ptr,size);

                long target_bucket = this.get_bucket(w);

                while(this.dict_entries_taken.contains(target_bucket)) {
                    collisions+=1;
                    target_bucket+=max_key_size;
                    // target_bucket%=TABLESIZE;
                }

                this.writeEntry(entry, target_bucket);
                this.dict_entries_taken.add(target_bucket);

                // if(w.equals("what")) print("Found the word what at index"+String.valueOf(target_bucket)+" pointing to entry: "+String.valueOf(ptr));

                print("Finished "+String.valueOf(track)+" entries.");
                track+=1;
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        System.err.println( collisions + " collisions." );
    }

    // ==================================================================


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        //
        //  REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        long target_bucket = this.get_bucket(token);

        Entry entry = readEntry(target_bucket);

        while(!Objects.isNull(entry)){

            int sum = 0;
            for(char c: token.toCharArray()) sum += (int)c;

            if(entry.get_sum()==sum){
                String postings_list = readData(entry.get_ptr(), entry.get_size());
                List<String> posting_entries = Arrays.asList(postings_list.split(","));
                PostingsList result = new PostingsList();
                for(String s: posting_entries){
                    s = s.trim();
                    List<String> entries = Arrays.asList(s.split(" "));
                    int docID = Integer.parseInt(entries.get(0));
                    int offset = Integer.parseInt(entries.get(1));
                    result.insert(new PostingsEntry(docID,offset));
                }
                return result;
            } else{
                target_bucket += max_key_size;
                entry = readEntry(target_bucket);
            }
        }

        return null;
    }


    /**
     *  Inserts this token in the main-memory hashtable.
     */
    public void insert( String token, int docID, int offset ) {
        //
        //  YOUR CODE HERE
        //
        if(!this.index.containsKey(token)) this.index.put(token, new PostingsList());
        this.index.get(token).insert(new PostingsEntry(docID,offset));
    }


    /**
     *  Write index to file after indexing is done.
     */
    public void cleanup() {
        System.err.println( index.keySet().size() + " unique words" );
        System.err.print( "Writing index to disk..." );
        writeIndex();
        System.err.println( "done!" );
        print("\n");
    }

    public void print(Object s){
        System.out.println(s);
    }

    public int get_size(){
        return this.index.size();
    }

    public long get_bucket(String w){
        long hash = Long.valueOf(w.hashCode());

        // long target_bucket = (hash * 33L + 11L)%TABLESIZE;
        long target_bucket = hash % TABLESIZE;

        if(target_bucket<0) target_bucket+=TABLESIZE;
        
        target_bucket = (target_bucket * 33L + 11L);

        target_bucket -= target_bucket%max_key_size;
        
        return target_bucket;
    }
    
    public String getFileName(String path) {
        String result = "";
        StringTokenizer tok = new StringTokenizer(path, "\\/");
        while (tok.hasMoreTokens()) {
            result = tok.nextToken();
        }
        return result;
    }
    
}
