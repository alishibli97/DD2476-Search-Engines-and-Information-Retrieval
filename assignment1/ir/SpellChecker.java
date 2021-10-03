/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import java.util.*;

public class SpellChecker {
    /** The regular inverted index to be used by the spell checker */
    Index index;

    /** K-gram index to be used by the spell checker */
    KGramIndex kgIndex;

    Searcher searcher;

    QueryType queryType = QueryType.INTERSECTION_QUERY;

    RankingType rankingType = RankingType.TF_IDF;

    NormalizationType normalizationType = NormalizationType.NUMBER_OF_WORDS;

    /**
     * The auxiliary class for containing the value of your ranking function for a
     * token
     */
    class KGramStat implements Comparable {
        double score;
        String token;

        KGramStat(String token, double score) {
            this.token = token;
            this.score = score;
        }

        public String getToken() {
            return token;
        }

        public int compareTo(Object other) {
            if (this.score == ((KGramStat) other).score)
                return 0;
            return this.score < ((KGramStat) other).score ? -1 : 1;
        }

        public String toString() {
            return token + ";" + score;
        }
    }

    /**
     * The threshold for Jaccard coefficient; a candidate spelling correction should
     * pass the threshold in order to be accepted
     */
    private static final double JACCARD_THRESHOLD = 0.4;

    /**
     * The threshold for edit distance for a candidate spelling correction to be
     * accepted.
     */
    private static final int MAX_EDIT_DISTANCE = 2;

    public SpellChecker(Index index, KGramIndex kgIndex, Searcher searcher) {
        this.index = index;
        this.kgIndex = kgIndex;
        this.searcher = searcher;
    }

    /**
     * Computes the Jaccard coefficient for two sets A and B, where the size of set
     * A is <code>szA</code>, the size of set B is <code>szB</code> and the
     * intersection of the two sets contains <code>intersection</code> elements.
     */
    private double jaccard(int szA, int szB, int intersection) {
        //
        // YOUR CODE HERE
        //
        double jaccard = Double.valueOf(intersection) / Double.valueOf(szA + szB - intersection);
        return jaccard;
    }

    /**
     * Computing Levenshtein edit distance using dynamic programming. Allowed
     * operations are: => insert (cost 1) => delete (cost 1) => substitute (cost 2)
     */
    private int editDistance(String s1, String s2) {
        //
        // YOUR CODE HERE
        //

        int[][] dist_matrix = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) dist_matrix[i][j] = j;
                else if (j == 0) dist_matrix[i][j] = i;
                else  dist_matrix[i][j] = min(dist_matrix[i - 1][j - 1]+ costOfSubstitution(s1.charAt(i - 1), s2.charAt(j - 1)), dist_matrix[i - 1][j] + 1, dist_matrix[i][j - 1] + 1);
            }
        }
        return dist_matrix[s1.length()][s2.length()];
    }

    public static int costOfSubstitution(char a, char b) {
        return a == b ? 0 : 1;
    }

    private int min(int a, int b, int c) {
        if (a <= b && a <= c) return a;
        else if (b <= c && b <= a) return b;
        else return c;
    }

    /**
     * Checks spelling of all terms in <code>query</code> and returns up to
     * <code>limit</code> ranked suggestions for spelling correction.
     */
    public String[] check(Query query, int limit) {
        //
        // YOUR CODE HERE
        //
        ArrayList<String> terms = new ArrayList<>();
        int num_terms = query.queryterm.size();
        for (int i = 0; i < num_terms; i++) terms.add(query.queryterm.get(i).term);

        List<List<KGramStat>> qCorrections = new ArrayList<>();

        for(String term: terms){
            if(index.getPostings(term)!=null) {
                List<KGramStat> temp = new ArrayList<>();
                temp.add(new KGramStat(term, 1.0));
                qCorrections.add(temp);
            }
            else{
                ArrayList<String> kgrams = kgIndex.getKgrams(term);
                ArrayList<String> setA = kgIndex.getKgrams(term);
                int szA = setA.size();
                List<KGramStat> temp = new ArrayList<>();
                List<String> added_terms = new ArrayList<>();
                for(String kgram: kgrams){

                    List<KGramPostingsEntry> plist = kgIndex.getPostings(kgram);

                    for(KGramPostingsEntry entry: plist){
                        String term_new = kgIndex.getTermByID(entry.tokenID);
                        
                        ArrayList<String> setB = kgIndex.getKgrams(term_new);
                        int szB = setB.size();

                        setB.retainAll(setA);
                        int intersection = setB.size();

                        double jacc_sim = jaccard(szA, szB, intersection);

                        if(jacc_sim>=JACCARD_THRESHOLD){
                            int editDistance = editDistance(term, term_new);
                            if(editDistance<=MAX_EDIT_DISTANCE){
                                double score = index.getPostings(term_new).size();
                                if(!added_terms.contains(term_new)) {
                                    temp.add(new KGramStat(term_new, score));
                                    added_terms.add(term_new);
                                }
                            }
                        }
                    }
                }
                qCorrections.add(temp);
            }
        }

        if(qCorrections.size()==0) return null;

        List<KGramStat> resultList = mergeCorrections(qCorrections, limit);
        Collections.sort(resultList,Collections.reverseOrder());

        print(resultList);

        String[] result = new String[resultList.size()]; // limit
        for (int i = 0; i < resultList.size(); i++) result[i] = resultList.get(i).token;

        return result;
    }

    /**
     * Merging ranked candidate spelling corrections for all query terms available
     * in <code>qCorrections</code> into one final merging of query phrases. Returns
     * up to <code>limit</code> corrected phrases.
     */
    private List<KGramStat> mergeCorrections(List<List<KGramStat>> qCorrections, int limit) {
        //
        // YOUR CODE HERE
        //

        List<KGramStat> query = new ArrayList<KGramStat>();

        for (int i = 0; i < qCorrections.size(); i++) {
            int list_size = qCorrections.get(i).size();

            // initialize
            if (i == 0) {
                if (list_size > 1) {
                    if (list_size < limit) {
                        for (int j = 0; j < list_size; j++) {
                            String candidateToken = qCorrections.get(i).get(j).token;
                            query.add(new KGramStat(candidateToken, 1.0));
                        }
                    } else {
                        for (int j = 0; j < limit; j++) {
                            String candidateToken = qCorrections.get(i).get(j).token;
                            query.add(new KGramStat(candidateToken, 1.0));
                        }
                    }
                } else {
                    String candidateToken = qCorrections.get(i).get(0).token;

                    query.add(new KGramStat(candidateToken, 1.0));
                }
            } else {
                if (list_size > 1) {
                    List<KGramStat> currentList = new ArrayList<KGramStat>();
                    for (int j = 0; j < list_size; j++) {
                        String candidateToken = qCorrections.get(i).get(j).token;
                        for (int m = 0; m < query.size(); m++) {
                            String query_candidate = query.get(m).token + " " + candidateToken;
                            PostingsList searcher_new = new PostingsList();
                            Query query_new = new Query(query_candidate);
                            searcher_new = searcher.search(query_new, queryType, rankingType, normalizationType);
                            currentList.add(new KGramStat(query_candidate, searcher_new.size()));
                        }
                    }
                    Collections.sort(currentList);
                    // System.err.println(currentList.size());
                    if (currentList.size() < limit) {
                        query.clear();
                        for (int n = 0; n < currentList.size(); n++) {
                            // add is ok here
                            query.add(currentList.get(n));
                        }
                    } else {
                        query.clear();
                        for (int n = 0; n < limit; n++) {
                            query.add(currentList.get(n));
                        }
                    }
                } else {
                    for (int m = 0; m < query.size(); m++) {
                        String candidateToken = qCorrections.get(i).get(0).token;
                        String query_candidate = query.get(m).token + " " + candidateToken;
                        query.set(m, new KGramStat(query_candidate, 1.0));
                    }
                    if (i == qCorrections.size() - 1) {
                        List<KGramStat> currentList = new ArrayList<KGramStat>();
                        for (int m = 0; m < query.size(); m++) {
                            String query_candidate = query.get(m).token;
                            PostingsList searcher_new = new PostingsList();
                            Query query_new = new Query(query_candidate);
                            searcher_new = searcher.search(query_new, queryType, rankingType, normalizationType);
                            // set score;
                            currentList.add(new KGramStat(query_candidate, searcher_new.size()));
                        }
                        Collections.sort(currentList);
                        if (currentList.size() < limit) {
                            query.clear();
                            for (int n = 0; n < currentList.size(); n++) {
                                query.add(currentList.get(n));
                            }
                        } else {
                            query.clear();
                            for (int n = 0; n < limit; n++) {
                                query.add(currentList.get(n));
                            }
                        }
                    }
                }
            }
        }

        return query;

    }

    private void print(Object o) {
        System.out.println(String.valueOf(o));
    }
}