/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.io.Serializable;

public class PostingsEntry implements Comparable<PostingsEntry>, Serializable {

    public int docID;
    public double score = 0;
    public int offset;

    /**
     *  PostingsEntries are compared by their score (only relevant
     *  in ranked retrieval).
     *
     *  The comparison is defined so that entries will be put in 
     *  descending order.
     */
    public int compareTo( PostingsEntry other ) {
       return Double.compare( other.score, score );
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return String.valueOf(this.docID)+" "+String.valueOf(this.offset);
    }

    //
    // YOUR CODE HERE
    //
    public PostingsEntry(int docID,int offset){
        this.docID = docID;
        this.offset = offset;
    }

    public PostingsEntry(int docID){
        this.docID = docID;
    }

    public void addScore(double score){
        this.score = score;
    }
}

