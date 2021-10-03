/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  


package ir;

import java.util.HashMap;
import java.util.Iterator;


/**
 *   Implements an inverted index as a Hashtable from words to PostingsLists.
 */
public class HashedIndex implements Index {


    /** The index as a hashtable. */
    public HashMap<String,PostingsList> index = new HashMap<String,PostingsList>();


    /**
     *  Inserts this token in the hashtable.
     */
    public void insert( String token, int docID, int offset ) {
        //
        // YOUR CODE HERE
        //
        if(!this.index.containsKey(token)) this.index.put(token, new PostingsList());
        this.getPostings(token).insert(new PostingsEntry(docID, offset));
    }


    /**
     *  Returns the postings for a specific term, or null
     *  if the term is not in the index.
     */
    public PostingsList getPostings( String token ) {
        //
        // REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        return this.index.get(token);
    }

    /**
     *  No need for cleanup in a HashedIndex.
     */
    public void cleanup() {
    }

    public int get_size(){
        return this.index.size();
    }

    public String getFileName(String path) {
        return "";
    }
}
