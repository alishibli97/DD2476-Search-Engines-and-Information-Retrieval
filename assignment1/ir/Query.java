/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.util.*;
import java.nio.charset.*;
import java.io.*;


/**
 *  A class for representing a query as a list of words, each of which has
 *  an associated weight.
 */
public class Query {

    /**
     *  Help class to represent one query term, with its associated weight. 
     */
    class QueryTerm {
        String term;
        double weight;
        QueryTerm( String t, double w ) {
            term = t;
            weight = w;
        }
    }

    /** 
     *  Representation of the query as a list of terms with associated weights.
     *  In assignments 1 and 2, the weight of each term will always be 1.
     */
    public ArrayList<QueryTerm> queryterm = new ArrayList<QueryTerm>();

    /**  
     *  Relevance feedback constant alpha (= weight of original query terms). 
     *  Should be between 0 and 1.
     *  (only used in assignment 3).
     */
    double alpha = 0.2;

    /**  
     *  Relevance feedback constant beta (= weight of query terms obtained by
     *  feedback from the user). 
     *  (only used in assignment 3).
     */
    double beta = 1 - alpha;
    
    
    /**
     *  Creates a new empty Query 
     */
    public Query() {
    }
    
    
    /**
     *  Creates a new Query from a string of words
     */
    public Query( String queryString  ) {
        StringTokenizer tok = new StringTokenizer( queryString );
        while ( tok.hasMoreTokens() ) {
            queryterm.add( new QueryTerm(tok.nextToken(), 1.0) );
        }    
    }
    
    public void add_term(String term){
        this.queryterm.add(new QueryTerm(term, 1.0));
    }
    
    /**
     *  Returns the number of terms
     */
    public int size() {
        return queryterm.size();
    }
    
    
    /**
     *  Returns the Manhattan query length
     */
    public double length() {
        double len = 0;
        for ( QueryTerm t : queryterm ) {
            len += t.weight; 
        }
        return len;
    }
    
    
    /**
     *  Returns a copy of the Query
     */
    public Query copy() {
        Query queryCopy = new Query();
        for ( QueryTerm t : queryterm ) {
            queryCopy.queryterm.add( new QueryTerm(t.term, t.weight) );
        }
        return queryCopy;
    }
    
    
    /**
     *  Expands the Query using Relevance Feedback
     *
     *  @param results The results of the previous query.
     *  @param docIsRelevant A boolean array representing which query results the user deemed relevant.
     *  @param engine The search engine object
     */
    public void relevanceFeedback( PostingsList results, boolean[] docIsRelevant, Engine engine ) {
        //
        //  YOUR CODE HERE
        //
        
        int numOfRelevantDoc = 0;
        for (int i = 0; i < docIsRelevant.length; i++) {
            if (docIsRelevant[i]) {
                numOfRelevantDoc++;
            }
        }

        Map<Integer,HashMap<String,Integer>> occurences = new HashMap<Integer,HashMap<String,Integer>>();

        for(int i=0;i<this.queryterm.size();i++){
            String term = this.queryterm.get(i).term;

            PostingsList list = engine.index.getPostings(term);
            for(PostingsEntry entry: list.get_list()){
                if(!occurences.containsKey(entry.docID)) occurences.put(entry.docID,new HashMap<String,Integer>());
                if(!occurences.get(entry.docID).containsKey(term)) occurences.get(entry.docID).put(term, 1);
                else occurences.get(entry.docID).put(term, occurences.get(entry.docID).get(term)+1);
            }
        }

        HashMap<String, Double> term_weight_map = new HashMap<String, Double>();
        for(int i=0;i<10;i++){
            if(docIsRelevant[i]){
                int docID = results.get(i).docID;
                HashMap<String, Integer> term_tf_map = occurences.get(docID);
                int docLength = Index.docLengths.get(docID);
                for (Map.Entry<String, Integer> tf : term_tf_map.entrySet()) {
                    String term = tf.getKey();
                    double weight;
                    weight = beta * (1.0 / numOfRelevantDoc) * (Double.valueOf(tf.getValue()) / Double.valueOf(docLength));
                    if (!term_weight_map.containsKey(term)) {
                        term_weight_map.put(term, weight);
                    } else {
                        weight = weight + term_weight_map.get(term);
                        term_weight_map.put(term, weight);
                    }
                }
            }
        }

        for (Map.Entry<String, Double> q : term_weight_map.entrySet()) {
            QueryTerm term_weight_term = new QueryTerm(q.getKey(),1.0);
            if (!queryterm.contains(term_weight_term)) {
                queryterm.add(new QueryTerm(q.getKey(), q.getValue()));
            }else{
                int n = queryterm.indexOf(term_weight_term);
                queryterm.get(n).weight = q.getValue()+alpha;
            }
        }
    }

    public void print(Object o){
        System.out.println(String.valueOf(o));
    }
}