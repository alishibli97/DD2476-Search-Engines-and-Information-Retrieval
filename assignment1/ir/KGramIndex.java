/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import java.nio.charset.StandardCharsets;

public class KGramIndex {

    /** Mapping from term ids to actual term strings */
    HashMap<Integer, String> id2term = new HashMap<Integer, String>();

    /** Mapping from term strings to term ids */
    HashMap<String, Integer> term2id = new HashMap<String, Integer>();

    /** Index from k-grams to list of term ids that contain the k-gram */
    HashMap<String, List<KGramPostingsEntry>> index = new HashMap<String, List<KGramPostingsEntry>>();

    /** The ID of the last processed term */
    int lastTermID = -1;

    /** Number of symbols to form a K-gram */
    int K = 3;

    public KGramIndex(int k) {
        K = k;
        if (k <= 0) {
            System.err.println("The K-gram index can't be constructed for a negative K value");
            System.exit(1);
        }
    }

    /** Generate the ID for an unknown term */
    private int generateTermID() {
        return ++lastTermID;
    }

    public int getK() {
        return K;
    }

    /**
     * Get intersection of two postings lists
     */
    public List<KGramPostingsEntry> intersect(List<KGramPostingsEntry> p1, List<KGramPostingsEntry> p2) {
        //
        // YOUR CODE HERE
        //

        List<KGramPostingsEntry> result = new ArrayList<KGramPostingsEntry>();

        if (p2 == null)
            return p1;

        int i = 0;
        int j = 0;
        while (i < p1.size() && j < p2.size()) {
            if (p1.get(i).tokenID < p2.get(j).tokenID)
                i++;
            else if (p1.get(i).tokenID > p2.get(j).tokenID)
                j++;
            else {
                result.add(new KGramPostingsEntry(p1.get(i).tokenID));
                i++;
                j++;
            }
        }

        return result;
    }

    /** Inserts all k-grams from a token into the index. */

    List<Integer> ids = new ArrayList<Integer>();
    public void insert(String token) {
        //
        // YOUR CODE HERE
        //

        if(!term2id.containsKey(token)) {

            int termID = generateTermID();
            term2id.put(token, termID);
            id2term.put(termID, token);

            KGramPostingsEntry entry = new KGramPostingsEntry(termID);    

            int k = getK();
            int num_kgrams = token.length() + 3 - k;
            String token_new = "^" + token + "$";
            for (int i = 0; i < num_kgrams; i++) {
                String key = token_new.substring(i, i + k);
                if (!index.containsKey(key))
                    index.put(key, new ArrayList<KGramPostingsEntry>());
                if (!index.get(key).contains(entry)){
                    index.get(key).add(entry);
                }
            }
        }
    }

    /** Get postings for the given k-gram */
    public List<KGramPostingsEntry> getPostings(String kgram) {
        //
        // YOUR CODE HERE
        //
        if (index.containsKey(kgram))
            return index.get(kgram);
        return null;
    }

    /** Get id of a term */
    public Integer getIDByTerm(String term) {
        return term2id.get(term);
    }

    /** Get a term by the given id */
    public String getTermByID(Integer id) {
        return id2term.get(id);
    }

    /** Processing Wildcard Queries */    

    public Query expand_query(String term){

        Query query = new Query();
        List<KGramPostingsEntry> postings = null;
        String newToken = "^" + term + "$";
        int kgramNum = term.length() + 3 - getK();
        String kgram;

        for (int i = 0; i < kgramNum; i++) {
            kgram = newToken.substring(i, i + getK());

            if (postings == null) {
                postings = getPostings(kgram);
            } else {
                postings = intersect(postings, getPostings(kgram));
            }
        }

        String regexToken = term.replace("*", ".*");
        for (int i = 0; i < postings.size(); i++) {
            String term_new = getTermByID(postings.get(i).tokenID);
            if (Pattern.matches(regexToken, term_new)) {
                query.add_term(term_new);
            }
        }
        return query;
    }

    public ArrayList<String> getKgrams(String token){
        int kgramNum = token.length() + 3 - getK();
        String token_new = "^"+token+"$";
        ArrayList<String> kgrams = new  ArrayList<String>();
        String kgram;
        for(int i = 0; i < kgramNum; i++){
            kgram = token_new.substring(i, i + getK());
            kgrams.add(kgram);
        }
        return kgrams;
    }


    private static HashMap<String, String> decodeArgs(String[] args) {
        HashMap<String, String> decodedArgs = new HashMap<String, String>();
        int i = 0, j = 0;
        while (i < args.length) {
            if ("-p".equals(args[i])) {
                i++;
                if (i < args.length) {
                    decodedArgs.put("patterns_file", args[i++]);
                }
            } else if ("-f".equals(args[i])) {
                i++;
                if (i < args.length) {
                    decodedArgs.put("file", args[i++]);
                }
            } else if ("-k".equals(args[i])) {
                i++;
                if (i < args.length) {
                    decodedArgs.put("k", args[i++]);
                }
            } else if ("-kg".equals(args[i])) {
                i++;
                if (i < args.length) {
                    decodedArgs.put("kgram", args[i++]);
                }
            } else {
                System.err.println("Unknown option: " + args[i]);
                break;
            }
        }
        return decodedArgs;
    }

    public static void main(String[] arguments) throws FileNotFoundException, IOException {
        HashMap<String, String> args = decodeArgs(arguments);

        int k = Integer.parseInt(args.getOrDefault("k", "3"));
        KGramIndex kgIndex = new KGramIndex(k);

        File f = new File(args.get("file"));
        Reader reader = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8);
        Tokenizer tok = new Tokenizer(reader, true, false, true, args.get("patterns_file"));
        while (tok.hasMoreTokens()) {
            String token = tok.nextToken();
            kgIndex.insert(token);
        }

        String[] kgrams = args.get("kgram").split(" ");
        List<KGramPostingsEntry> postings = null;
        for (String kgram : kgrams) {
            if (kgram.length() != k) {
                System.err.println(
                        "Cannot search k-gram index: " + kgram.length() + "-gram provided instead of " + k + "-gram");
                System.exit(1);
            }

            if (postings == null) {
                postings = kgIndex.getPostings(kgram);
            } else {
                postings = kgIndex.intersect(postings, kgIndex.getPostings(kgram));
            }
        }
        if (postings == null) {
            System.err.println("Found 0 posting(s)");
        } else {
            int resNum = postings.size();
            System.err.println("Found " + resNum + " posting(s)");
            if (resNum > 10) {
                System.err.println("The first 10 of them are:");
                resNum = 10;
            }
            for (int i = 0; i < resNum; i++) {
                System.err.println(kgIndex.getTermByID(postings.get(i).tokenID));
            }
        }
    }

    public void print(Object o) {
        System.out.println(String.valueOf(o));
    }
}
