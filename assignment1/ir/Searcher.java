/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.*;
import java.io.*;

/**
 * Searches an index for results of a query.
 */
public class Searcher {

    /** The index to be searched by this Searcher. */
    Index index;

    /** The k-gram index to be searched by this Searcher */
    KGramIndex kgIndex;

    class Document implements Comparable<Document> {

        String docNumber;
        double docScore;

        Document(String docNumber, double docScore) {
            this.docNumber = docNumber;
            this.docScore = docScore;
        }

        public int compareTo(Document doc) {

            if (this.docScore == doc.docScore)
                return 0;
            else if (this.docScore < doc.docScore)
                return 1;
            else
                return -1;
        }

        @Override
        public String toString() {
            // TODO Auto-generated method stub
            return String.valueOf(this.docNumber) + ": " + String.valueOf(this.docScore);
        }

    }

    ArrayList<Document> docs = new ArrayList<Document>();

    Map<Integer, Double> docLengths = new HashMap<Integer, Double>();

    /** Constructor */
    public Searcher(Index index, KGramIndex kgIndex) {
        this.index = index;
        this.kgIndex = kgIndex;

        try {
            Scanner s = new Scanner(new File("/home/ali/Desktop/SE_IR/assignment1/ir/output.txt"));

            while (s.hasNext()) {
                String[] temp = s.next().split(",");
                this.docs.add(new Document(temp[0], Double.parseDouble(temp[1])));
            }
            s.close();
        } catch (Exception e) {
            // TODO: handle exception
        }

        try {
            Scanner s = new Scanner(new File("/home/ali/Desktop/SE_IR/assignment1/ir/lengths.txt"));

            while (s.hasNext()) {
                String[] temp = s.next().split(":");
                this.docLengths.put(Integer.parseInt(temp[0]), Double.parseDouble(temp[1]));
            }

        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    /**
     * Searches the index for postings matching the query.
     * 
     * @return A postings list representing the result of the query.
     */
    public PostingsList search(Query query, QueryType queryType, RankingType rankingType,
            NormalizationType normalizationType) {
        //
        // REPLACE THE STATEMENT BELOW WITH YOUR CODE
        //
        if (query.queryterm.size() == 0)
            return null;
        else if (queryType == QueryType.INTERSECTION_QUERY) {
            return this.search_by_intersection(query);
        } else if (queryType == QueryType.PHRASE_QUERY) {
            return this.search_by_phrase(query);
        } else if (queryType == QueryType.RANKED_QUERY) {
            // query.relevanceFeedback(results, docIsRelevant, engine);
            return this.search_by_rank(query, rankingType, normalizationType);
        } else { // one word query
            PostingsList list = this.index.getPostings(query.queryterm.get(0).term);
            ArrayList<Integer> docIDs = new ArrayList<Integer>();
            PostingsList result = new PostingsList();

            for (int i = 0; i < list.size(); i++) {
                int docID = list.get(i).docID;
                if (!docIDs.contains(docID)) {
                    result.insert(list.get(i));
                    docIDs.add(docID);
                }
            }
            return result;
        }
    }

    public PostingsList search_by_intersection(Query query) {
        ArrayList<String> terms = new ArrayList<>();
        int num_terms = query.queryterm.size();
        for (int i = 0; i < num_terms; i++)
            terms.add(query.queryterm.get(i).term);
        ArrayList<ArrayList<PostingsEntry>> lists = new ArrayList<ArrayList<PostingsEntry>>();
        for (String term : terms) {
            try {
                if (term.contains("*")) {
                    Query expanded_term = kgIndex.expand_query(term);
                    Set<Integer> docIDs = new HashSet<Integer>();
                    for (int k = 0; k < expanded_term.size(); k++) {
                        // print(expanded_term.queryterm.get(k).term);
                        docIDs.addAll(index.getPostings(expanded_term.queryterm.get(k).term).get_IDs());
                    }
                    ArrayList<Integer> docIDsList = new ArrayList<Integer>(docIDs);
                    Collections.sort(docIDsList);
                    ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();
                    for (int id : docIDsList)
                        list.add(new PostingsEntry(id));
                    lists.add(list);
                } else {
                    ArrayList<PostingsEntry> list = index.getPostings(term).get_filtered_list();
                    if (!list.isEmpty() && !lists.contains(list))
                        lists.add(list);
                }
            } catch (Exception e) {
                return null;
            }
        }
        int num_lists = lists.size();
        if (num_lists == 0)
            return null;
        // for(int i=0;i<num_lists;i++) print(lists.get(i).size());

        PostingsList answers = new PostingsList();
        ArrayList<Integer> indexes = new ArrayList<>(Collections.nCopies(num_lists, 0));
        ArrayList<Integer> list_sizes = new ArrayList<>();
        for (ArrayList<PostingsEntry> l : lists)
            list_sizes.add(l.size());

        while (indexes.stream().allMatch(i -> i < list_sizes.get(indexes.indexOf(i)))) {

            ArrayList<Integer> ids = new ArrayList<>();
            for (int i = 0; i < num_lists; i++)
                ids.add(lists.get(i).get(indexes.get(i)).docID);

            if (ids.stream().distinct().count() <= 1) {

                answers.insert(lists.get(0).get(indexes.get(0)));
                for (int i = 0; i < num_lists; i++)
                    indexes.set(i, indexes.get(i) + 1);
            } else {
                int max = Collections.max(ids);
                for (int i = 0; i < num_lists; i++) {
                    int j = 0;
                    while (j < list_sizes.get(i) && indexes.get(i) < list_sizes.get(i)
                            && lists.get(i).get(indexes.get(i)).docID < max) {
                        indexes.set(i, indexes.get(i) + 1);
                        j++;
                    }
                }
            }
        }

        return answers;
    }

    public PostingsList search_by_phrase(Query query) {

        PostingsList result = new PostingsList();

        HashMap<Integer, ArrayList<Integer>> docIDs_offsets = new HashMap<Integer, ArrayList<Integer>>();

        for (int i = 0; i < query.size(); i++) {
            String term = query.queryterm.get(i).term;

            HashMap<Integer, ArrayList<Integer>> docID_offsets = new HashMap<Integer, ArrayList<Integer>>();
            PostingsList plist = new PostingsList();
            if (term.contains("*")) {
                Query expanded_query = kgIndex.expand_query(term);
                for (int j = 0; j < expanded_query.size(); j++) {
                    String term_new = expanded_query.queryterm.get(j).term;
                    plist = index.getPostings(term_new);
                    // write map between docID and offsets
                    for(int k=0;k<plist.size();k++){
                        PostingsEntry entry = plist.get(k);
                        if(!docID_offsets.containsKey(entry.docID)) docID_offsets.put(entry.docID, new ArrayList<Integer>());
                        docID_offsets.get(entry.docID).add(entry.offset);
                    }
                }
            } else {
                plist = index.getPostings(term);
                // write map between docID and offsets
                for(int k=0;k<plist.size();k++){
                    PostingsEntry entry = plist.get(k);
                    if(!docID_offsets.containsKey(entry.docID)) docID_offsets.put(entry.docID, new ArrayList<Integer>());
                    docID_offsets.get(entry.docID).add(entry.offset);
                }
            }

            if (docIDs_offsets.size() == 0) docIDs_offsets = docID_offsets;
            else docIDs_offsets = this.intersect(docIDs_offsets, docID_offsets);
        }

        for(int docID: docIDs_offsets.keySet()) result.insert(new PostingsEntry(docID));

        return result;
    }


        // ArrayList<String> terms = new ArrayList<>();
        // ArrayList<String> terms_big = new ArrayList<>();
        // ArrayList<ArrayList<String>> terms_ordered = new ArrayList<>();
        // ArrayList<HashSet<Integer>> keys_big = new ArrayList<>();
        // int num_terms = query.queryterm.size();
        // for (int i = 0; i < num_terms; i++) {
        //     String term = query.queryterm.get(i).term;
        //     terms.add(term);
        //     // PostingsList temp;
        //     terms_ordered.add(new ArrayList<String>());
        //     if(term.contains("*")){
        //         HashSet<Integer> union = new HashSet<>();
        //         Query expanded_term = kgIndex.getWordofWildcard(term);
        //         for(int j=0;j<expanded_term.queryterm.size();j++){
        //             String new_term = expanded_term.queryterm.get(i).term;
        //             terms_ordered.get(i).add(new_term);
        //             if(!terms_big.contains(new_term)) terms_big.add(new_term);
        //             if(union.size()==0) union = new HashSet<>(index.getPostings(new_term).get_IDs());
        //             else union.addAll(new HashSet<>(index.getPostings(new_term).get_IDs()));
        //         }
        //         keys_big.add(union);
        //     } else{
        //         terms_ordered.get(i).add(term);
        //         keys_big.add(new HashSet<>(index.getPostings(term).get_IDs()));
        //         if(!terms_big.contains(term)) terms_big.add(term);
        //     }
        // }

        // PostingsList answers = new PostingsList();

        // Set<Integer> keys;
        // try {
        //     // keys = new HashSet<Integer>(this.index.getPostings(terms.get(0)).get_IDs());
        //     // for(String term : terms) {
        //     //     keys.retainAll(index.getPostings(term).get_IDs()); // INSTEAD OF DOING INTERSECTION OVER ALL ID LISTS, JUST DO ON TWO LISTS AT ONCE..
        //     // }
        //     keys = keys_big.get(0);
        //     for(int i=0;i<keys_big.size();i++) keys.retainAll(keys_big.get(i));
        // } catch (Exception e) {
        //     return null;
        // }

        // /*
        // Map between each term and another 
        //         map between docID and arraylist of offsets(positions) in the doc
        // Example:
        //     "eat":{
        //         0: [2,10,11,40],
        //         1: [2, 44, 55, 60],
        //         9: [12,24,45,60]
        //     }
        // */
        // Map<String, Map<Integer, ArrayList<Integer>>> map = new HashMap<String, Map<Integer, ArrayList<Integer>>>();
        // for (String term : terms_big) {
        //     map.put(term, new HashMap<Integer, ArrayList<Integer>>());
        //     PostingsList plist = this.index.getPostings(term);
        //     for (int k = 0; k < plist.size(); k++) {
        //         PostingsEntry entry = plist.get(k);
        //         int id = entry.docID;
        //         if (keys.contains(id)) {
        //             if (!map.get(term).containsKey(id))
        //                 map.get(term).put(id, new ArrayList<Integer>());
        //             map.get(term).get(id).add(entry.offset);
        //         }
        //     }
        // }
        // List<Integer> keys_list = new ArrayList<Integer>(keys);

        // for (int k = 0; k < keys.size(); k++) {
        //     /*
        //     lists is an arraylist of offsets lists for each word
        //     */
        //     ArrayList<ArrayList<Integer>> lists = new ArrayList<ArrayList<Integer>>();
            
        //     for(ArrayList<String> _terms: this.generate(terms_ordered)){
        //         for (String term : _terms) {
        //             lists.add(map.get(term).get(keys_list.get(k)));
        //         }
        //         if (this.phrase_is_in_this_document(lists))
        //             answers.insert(new PostingsEntry(k));
        //     }
        // }

        // return answers;
    
    //     ArrayList<String> terms = new ArrayList<>();
    //     int num_terms = query.queryterm.size();
    //     for (int i = 0; i < num_terms; i++) {
    //         String term = query.queryterm.get(i).term;
    //         terms.add(term);
    //     }

    //     PostingsList answers = new PostingsList();

    //     Set<Integer> keys;
    //     try {
    //         keys = new HashSet<Integer>(this.index.getPostings(terms.get(0)).get_IDs());

    //         for (String term : terms) {
    //             keys.retainAll(index.getPostings(term).get_IDs()); // INSTEAD OF DOING INTERSECTION OVER ALL ID LISTS, JUST DO ON TWO LISTS AT ONCE..
    //         }
    //     } catch (Exception e) {
    //         return null;
    //     }

    //     Map<String, Map<Integer, ArrayList<Integer>>> map = new HashMap<String, Map<Integer, ArrayList<Integer>>>();
    //     for (String term : terms) {
    //         map.put(term, new HashMap<Integer, ArrayList<Integer>>());
    //         PostingsList plist = this.index.getPostings(term);
    //         for (int k = 0; k < plist.size(); k++) {
    //             PostingsEntry entry = plist.get(k);
    //             int id = entry.docID;
    //             if (keys.contains(id)) {
    //                 if (!map.get(term).containsKey(id))
    //                     map.get(term).put(id, new ArrayList<Integer>());
    //                 map.get(term).get(id).add(entry.offset);
    //             }
    //         }
    //     }
    //     List<Integer> keys_list = new ArrayList<Integer>(keys);

    //     for (int k = 0; k < keys.size(); k++) {
    //         ArrayList<ArrayList<Integer>> lists = new ArrayList<ArrayList<Integer>>();
    //         for (String term : terms) {
    //             lists.add(map.get(term).get(keys_list.get(k)));
    //         }
    //         if (this.phrase_is_in_this_document(lists))
    //             answers.insert(new PostingsEntry(k));
    //     }

    //     return answers;
    // }

    /*
     * ArrayList<ArrayList<String>> terms_big = new ArrayList<>(); int num_terms =
     * query.queryterm.size(); for (int i = 0; i < num_terms; i++) { String term =
     * query.queryterm.get(i).term; ArrayList<String> temp = new ArrayList<>(); if
     * (term.contains("*")){ Query expanded_term = kgIndex.getWordofWildcard(term);
     * for(int k=0;k<expanded_term.size();k++) { String new_term =
     * expanded_term.queryterm.get(k).term; /////////////////// LOOP OVER
     * (x1,y1),(x2,y2),...(xn,yn) temp.add(new_term); } } else{ temp.add(term); }
     * terms_big.add(temp); }
     * 
     * for(ArrayList<String> temp: terms_big) print(temp.size());
     * 
     * PostingsList answers = new PostingsList();
     * 
     * int solutions = 1; for(int i = 0; i < terms_big.size(); solutions *=
     * terms_big.get(i).size(), i++); print("Total is: "+solutions); for(int i = 0;
     * i < solutions; i++) { print("Starting iteration "+i); int j = 1;
     * ArrayList<String> terms = new ArrayList<>(); for(ArrayList<String> set :
     * terms_big) { terms.add(set.get((i/j)%set.size())); j *= set.size(); } // HERE
     * WE GET ALL THE TERMS, TERM BY TERM
     * 
     * Set<Integer> keys; try{ keys = new
     * HashSet<Integer>(this.index.getPostings(terms.get(0)).get_IDs());
     * 
     * for (String term: terms){ keys.retainAll(index.getPostings(term).get_IDs());
     * ////////////////////////// INSTEAD OF DOING INTERSECTION OVER ALL ID LISTS,
     * JUST DO ON TWO LISTS AT ONCE.. }
     * 
     * } catch(Exception e){ return null; }
     * 
     * Map<String,Map<Integer,ArrayList<Integer>>> map = new
     * HashMap<String,Map<Integer,ArrayList<Integer>>>(); for(String term: terms){
     * map.put(term, new HashMap<Integer,ArrayList<Integer>>()); PostingsList plist
     * = this.index.getPostings(term); for(int k=0;k<plist.size();k++){
     * PostingsEntry entry = plist.get(k); int id = entry.docID;
     * if(keys.contains(id)) { if(!map.get(term).containsKey(id))
     * map.get(term).put(id, new ArrayList<Integer>());
     * map.get(term).get(id).add(entry.offset); } } } List keys_list = new
     * ArrayList<Integer>(keys);
     * 
     * for(int k=0;k<keys.size();k++){ ArrayList<ArrayList<Integer>> lists = new
     * ArrayList<ArrayList<Integer>>(); for(String term: terms){
     * lists.add(map.get(term).get(keys_list.get(k))); }
     * if(this.phrase_is_in_this_document(lists) ) answers.insert(new
     * PostingsEntry(k)); } }
     * 
     * return answers;
     */

    public HashMap<Integer, ArrayList<Integer>> intersect(HashMap<Integer, ArrayList<Integer>> map_big, HashMap<Integer, ArrayList<Integer>> map_new) {

        HashMap<Integer, ArrayList<Integer>> result = new HashMap<Integer, ArrayList<Integer>>();

        if(map_big.size() < map_new.size()) {
            for (Map.Entry<Integer, ArrayList<Integer>> entry : map_big.entrySet()) {
                // add only the offsets of the docs whose ids are in the intersection
                if (map_new.containsKey(entry.getKey())) {
                    ArrayList<Integer> offsets = map_new.get(entry.getKey());
                    // check sequential order
                    for (int n = 0; n < offsets.size(); n++) {
                        if (entry.getValue().contains(offsets.get(n) - 1)) {
                            if(!result.containsKey(entry.getKey())) result.put(entry.getKey(), new ArrayList<Integer>());
                            result.get(entry.getKey()).add(offsets.get(n));
                        }
                    }
                }
            }
        } else {
            for (Map.Entry<Integer, ArrayList<Integer>> entry : map_new.entrySet()) {
                // add only the offsets of the docs whose ids are in the intersection
                if (map_big.containsKey(entry.getKey())) {
                    ArrayList<Integer> offsets = map_big.get(entry.getKey());
                    // check sequential order
                    for (int n = 0; n < offsets.size(); n++) {
                        if (entry.getValue().contains(offsets.get(n) + 1)) {
                            if(!result.containsKey(entry.getKey())) result.put(entry.getKey(), new ArrayList<Integer>());
                            result.get(entry.getKey()).add(offsets.get(n) + 1);
                        }
                    }
                }
            }
        }
        return result;
    }
    
    public ArrayList<ArrayList<String>> generate(ArrayList<ArrayList<String>> sets) {
        ArrayList<ArrayList<String>> result = new ArrayList<>();
        int solutions = 1;
        for(int i = 0; i < sets.size(); solutions *= sets.get(i).size(), i++);
        for(int i = 0; i < solutions; i++) {
            int j = 1;
            result.add(new ArrayList<String>());
            for(ArrayList<String> set : sets) {
                String term = set.get((i/j)%set.size());
                result.get(i).add(term);
                // System.out.print(set.get((i/j)%set.size()) + " ");
                j *= set.size();
            }
            // System.out.println();
        }
        return result;
    }

    private boolean phrase_is_in_this_document(ArrayList<ArrayList<Integer>> lists) {
        for (int val : lists.get(0)) {
            boolean found = true;
            for (int i = 0; i < lists.size(); i++) {
                if (!lists.get(i).contains(val + i)) {
                    found = false;
                    break;
                }
            }
            if (found)
                return true;
        }
        return false;
    }

    public PostingsList search_by_rank(Query query, RankingType rankingType, NormalizationType normalizationType) {

        if (rankingType == RankingType.TF_IDF)
            return this.get_result_tfidf(query, normalizationType);
        else if (rankingType == RankingType.PAGERANK)
            return this.get_result_pagerank(query);
        else if (rankingType == RankingType.COMBINATION)
            return get_result_combined(query, normalizationType);
        else
            return get_HITS(query);
    }

    public PostingsList get_result_tfidf(Query query, NormalizationType normalizationType) {

        ArrayList<String> terms = new ArrayList<>();
        int num_terms = query.queryterm.size();
        for (int i = 0; i < num_terms; i++)
            terms.add(query.queryterm.get(i).term);

        PostingsList result = new PostingsList();

        Map<String, Integer> docs_contain_term = new HashMap<String, Integer>();
        Map<String, Map<Integer, Integer>> occurences = new HashMap<String, Map<Integer, Integer>>();

        ArrayList<String> terms_new = new ArrayList<>();

        for (String term : terms) {
            if (term.contains("*")) {
                Query expanded_term = kgIndex.expand_query(term);
                for (int k = 0; k < expanded_term.size(); k++) {
                    String new_term = expanded_term.queryterm.get(k).term;
                    PostingsList list = this.index.getPostings(new_term);
                    docs_contain_term.put(new_term, list.get_filtered_list().size());
                    // print(new_term+" has pl of length "+list.size());
                    for (PostingsEntry entry : list.get_list()) {
                        if (!occurences.containsKey(new_term))
                            occurences.put(new_term, new HashMap<Integer, Integer>());
                        if (!occurences.get(new_term).containsKey(entry.docID))
                            occurences.get(new_term).put(entry.docID, 1);
                        else
                            occurences.get(new_term).put(entry.docID, occurences.get(new_term).get(entry.docID) + 1);
                        if (!result.check_docID(entry.docID))
                            result.insert(entry);
                    }
                    if (!terms_new.contains(new_term))
                        terms_new.add(new_term);
                }
            } else {
                PostingsList list = this.index.getPostings(term);
                docs_contain_term.put(term, list.get_filtered_list().size());
                for (PostingsEntry entry : list.get_list()) {
                    if (!occurences.containsKey(term))
                        occurences.put(term, new HashMap<Integer, Integer>());
                    if (!occurences.get(term).containsKey(entry.docID))
                        occurences.get(term).put(entry.docID, 1);
                    else
                        occurences.get(term).put(entry.docID, occurences.get(term).get(entry.docID) + 1);
                    if (!result.check_docID(entry.docID))
                        result.insert(entry);
                }
                if (!terms_new.contains(term))
                    terms_new.add(term);
            }
        }

        int N = this.index.docNames.size();

        PostingsList result_new = new PostingsList();

        for (PostingsEntry entry : result.get_list()) {
            double score = 0.0;
            for (String term : terms_new) {
                if (occurences.get(term).containsKey(entry.docID)) {
                    double tf_df = occurences.get(term).get(entry.docID);
                    double df_t = docs_contain_term.get(term);

                    double len_d = 0.0;
                    if (normalizationType == NormalizationType.NUMBER_OF_WORDS) {
                        len_d = this.index.docLengths.get(entry.docID);
                    } else { // EUCLIDEAN
                        len_d = this.docLengths.get(entry.docID);
                    }

                    double tf_idf_df = tf_df * Math.log(N / df_t) / len_d;

                    score += tf_idf_df;
                }
            }
            PostingsEntry entry_new = new PostingsEntry(entry.docID);
            entry_new.addScore(score);
            result_new.insert(entry_new);
        }

        Collections.sort(result_new.get_list());

        return result_new;
    }

    public PostingsList get_result_pagerank(Query query) {

        ArrayList<String> terms = new ArrayList<>();
        int num_terms = query.queryterm.size();
        for (int i = 0; i < num_terms; i++)
            terms.add(query.queryterm.get(i).term);

        PostingsList result = new PostingsList();
        ArrayList<Integer> docIDs = new ArrayList<Integer>();

        for (String term : terms) {
            try {
                PostingsList list = this.index.getPostings(term);

                for (PostingsEntry entry : list.get_filtered_list()) {
                    if (!docIDs.contains(entry.docID)) {
                        double docScore = this.docs.get(entry.docID).docScore;
                        entry.addScore(docScore);
                        result.insert(entry);
                        docIDs.add(entry.docID);
                    }
                }
            } catch (Exception e) {
                // TODO: handle exception
            }
        }

        if (result.size() == 0)
            return null;

        Collections.sort(result.get_list());

        return result;
    }

    public PostingsList get_result_combined(Query query, NormalizationType normalizationType) {
        PostingsList result_tfidf = this.get_result_tfidf(query, normalizationType);
        PostingsList result_pagerank = this.get_result_pagerank(query);

        PostingsList result_combined = new PostingsList();

        double a = 0.5; // tfidf
        double b = 1 - a; // page rank

        for (int i = 0; i < result_tfidf.size(); i++) {
            double score1 = result_tfidf.get(i).score;
            double score2 = result_pagerank.get(i).score;

            double score_combined = a * score1 + b * score2;

            PostingsEntry entry_combined = new PostingsEntry(result_tfidf.get(i).docID);
            entry_combined.addScore(score_combined);
            result_combined.insert(entry_combined);
        }
        return result_combined;
    }

    private PostingsList get_HITS(Query query) {

        ArrayList<String> terms = new ArrayList<>();
        int num_terms = query.queryterm.size();
        for (int i = 0; i < num_terms; i++)
            terms.add(query.queryterm.get(i).term);

        PostingsList result = new PostingsList();
        ArrayList<Integer> docIDs = new ArrayList<Integer>();

        for (String term : terms) {
            try {
                PostingsList list = this.index.getPostings(term);

                for (PostingsEntry entry : list.get_filtered_list()) {
                    if (!docIDs.contains(entry.docID)) {
                        result.insert(entry);
                        docIDs.add(entry.docID);
                    }
                }

            } catch (Exception e) {
                // TODO: handle exception
            }
        }

        if (result.size() == 0)
            return null;

        String linksFileLoc = "pagerank/linksDavis.txt";
        String titlesFileLoc = "pagerank/davisTitles.txt";

        HITSRanker ranker = new HITSRanker(linksFileLoc, titlesFileLoc, null);

        result = ranker.rank(result);

        Collections.sort(result.get_list());

        return result;
    }

    public void print(Object w) {
        System.out.println(w);
    }
}
