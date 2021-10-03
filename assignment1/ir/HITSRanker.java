/**
 *   Computes the Hubs and Authorities for an every document in a query-specific
 *   link graph, induced by the base set of pages.
 *
 *   @author Dmytro Kalpakchi
 */

package ir;

import java.util.*;
import java.io.*;

public class HITSRanker {

    private double hubs_coefficient = 0.3;
    private double authorities_coefficient = 0.7;

    /**
     * Max number of iterations for HITS
     */
    final static int MAX_NUMBER_OF_STEPS = 1000;

    /**
     * Convergence criterion: hub and authority scores do not change more that
     * EPSILON from one iteration to another.
     */
    final static double EPSILON = 0.001;

    /**
     * The inverted index
     */
    Index index;

    /**
     * Mapping from the titles to internal document ids used in the links file
     */
    HashMap<String, Integer> titleToId = new HashMap<String, Integer>();

    /**
     * Sparse vector containing hub scores
     */
    HashMap<Integer, Double> hubs = new HashMap<Integer, Double>();

    /**
     * Sparse vector containing authority scores
     */
    HashMap<Integer, Double> authorities = new HashMap<Integer, Double>();

    // add
    final static int MAX_NUMBER_OF_DOCS = 2000000;
    HashMap<Integer, HashMap<Integer, Boolean>> link = new HashMap<Integer, HashMap<Integer, Boolean>>();
    int[] out = new int[MAX_NUMBER_OF_DOCS];

    HashMap<String, Set<Integer>> inlinks = new HashMap<String, Set<Integer>>();
    HashMap<String, Set<Integer>> outlinks = new HashMap<String, Set<Integer>>();
    HashMap<Integer, String> externalID_to_name = new HashMap<Integer,String>();

    /* --------------------------------------------- */

    /**
     * Constructs the HITSRanker object
     * 
     * A set of linked documents can be presented as a graph. Each page is a node in
     * graph with a distinct nodeID associated with it. There is an edge between two
     * nodes if there is a link between two pages.
     * 
     * Each line in the links file has the following format:
     * nodeID;outNodeID1,outNodeID2,...,outNodeIDK This means that there are edges
     * between nodeID and outNodeIDi, where i is between 1 and K.
     * 
     * Each line in the titles file has the following format: nodeID;pageTitle
     * 
     * NOTE: nodeIDs are consistent between these two files, but they are NOT the
     * same as docIDs used by search engine's Indexer
     *
     * @param linksFilename  File containing the links of the graph
     * @param titlesFilename File containing the mapping between nodeIDs and pages
     *                       titles
     * @param index          The inverted index
     */
    public HITSRanker(String linksFilename, String titlesFilename, Index index) {
        this.index = index;
        readDocs(linksFilename, titlesFilename);
    }

    /* --------------------------------------------- */

    /**
     * A utility function that gets a file name given its path. For example, given
     * the path "davisWiki/hello.f", the function will return "hello.f".
     *
     * @param path The file path
     *
     * @return The file name.
     */
    private String getFileName(String path) {
        String result = "";
        StringTokenizer tok = new StringTokenizer(path, "\\/");
        while (tok.hasMoreTokens()) {
            result = tok.nextToken();
        }
        return result;
    }

    /**
     * Reads the files describing the 0.0001 * pageNumgraph of the given set of
     * pages.
     *
     * @param linksFilename  File containing the links of the graph
     * @param titlesFilename File containing the mapping between nodeIDs and pages
     *                       titles
     */
    void readDocs(String linksFilename, String titlesFilename) {
        //
        // YOUR CODE HERE
        //

        // read titles file
        int fileIndex = 0;
        try {
            System.err.print("Reading titles file... ");
            BufferedReader in = new BufferedReader(new FileReader(titlesFilename));
            String line;
            while ((line = in.readLine()) != null && fileIndex < MAX_NUMBER_OF_DOCS) {
                int index = line.indexOf(";");
                Integer docID = Integer.valueOf(line.substring(0, index));
                String docTitle = line.substring(index + 1);
                titleToId.put(docTitle, docID);
                externalID_to_name.put(docID,docTitle);
                fileIndex++;
            }
            if (fileIndex >= MAX_NUMBER_OF_DOCS) {
                System.err.print("stopped reading since documents table is full. ");
            } else {
                System.err.print("done. ");
            }
        } catch (FileNotFoundException e) {
            System.err.println("File " + titlesFilename + " not found!");
        } catch (IOException e) {
            System.err.println("Error reading file " + titlesFilename);
        }
        System.err.println("Read " + fileIndex + " number of documents");

        // read linksFile
        fileIndex = 0;
        try {
            System.err.print("Reading links file... ");
            BufferedReader in = new BufferedReader(new FileReader(linksFilename));
            String line;
            while ((line = in.readLine()) != null && fileIndex < MAX_NUMBER_OF_DOCS) {
                int index = line.indexOf(";");
                Integer fromDocID = Integer.valueOf(line.substring(0, index));
                if (link.get(fromDocID) == null) {
                    link.put(fromDocID, new HashMap<Integer, Boolean>());
                }
                if (!outlinks.containsKey(fromDocID))
                    outlinks.put(externalID_to_name.get(fromDocID), new HashSet<Integer>());

                fileIndex++;

                // Check all outlinks.
                StringTokenizer tok = new StringTokenizer(line.substring(index + 1), ",");
                while (tok.hasMoreTokens() && fileIndex < MAX_NUMBER_OF_DOCS) {
                    String file = tok.nextToken();
                    Integer otherDocID = Integer.valueOf(file);

                    if (!inlinks.containsKey(externalID_to_name.get(otherDocID)))
                        inlinks.put(externalID_to_name.get(otherDocID), new HashSet<Integer>());
                    inlinks.get(externalID_to_name.get(otherDocID)).add(fromDocID);
                    outlinks.get(externalID_to_name.get(fromDocID)).add(otherDocID);

                    // Set the probability to 0 for now, to indicate that there is
                    // a link from fromdoc to otherDoc.
                    if (link.get(fromDocID).get(otherDocID) == null) {
                        link.get(fromDocID).put(otherDocID, true);
                        out[fromDocID]++;
                    }
                }
            }
            if (fileIndex >= MAX_NUMBER_OF_DOCS) {
                System.err.print("stopped reading since documents table is full. ");
            } else {
                System.err.print("done. ");
            }
        } catch (FileNotFoundException e) {
            System.err.println("File " + linksFilename + " not found!");
        } catch (IOException e) {
            System.err.println("Error reading file " + linksFilename);
        }
        System.err.println("Read " + fileIndex + " number of documents---links");
    }

    /**
     * Perform HITS iterations until convergence
     *
     * @param titles The titles of the documents in the root set
     */
    private void iterate(String[] titles) {
        //
        // YOUR CODE HERE
        //
        int num_docs = titles.length;

        for (int i = 0; i < num_docs; i++) {
            authorities.put(i, 1.0);
            hubs.put(i, 1.0);
        }

        double error_hubs = Integer.MAX_VALUE;
        double error_authorities = Integer.MAX_VALUE;

        double[] hubs_new = new double[num_docs];
        double[] authorities_new = new double[num_docs];

        int iter = 0;
        while (((error_hubs > EPSILON) || (error_authorities > EPSILON)) && (iter < MAX_NUMBER_OF_STEPS)) {

            // double norm = 0;
            // for(int i=0;i<num_docs;i++){
            // authorities_new[i] = 0;
            // int external_docID = titleToId.get(titles[i]);
            // if(inlinks.containsValue(external_docID)){
            // print("Entered 1");
            // for(int j: inlinks.get(external_docID)) authorities_new[i] += hubs_new[j];
            // }
            // norm += Math.pow(authorities_new[i],2);
            // }
            // norm = Math.sqrt(norm);
            // for(int i=0;i<num_docs;i++) authorities_new[i]/=norm;

            // print(norm);

            // norm = 0;
            // for(int i=0;i<num_docs;i++){
            // hubs_new[i] = 0;
            // int external_docID = titleToId.get(titles[i]);
            // if(outlinks.containsKey(external_docID)){
            // // print("Entered 2");
            // for(int j: outlinks.get(external_docID)) hubs_new[j] += authorities_new[i];
            // }
            // norm += Math.pow(hubs_new[i],2);
            // }
            // norm = Math.sqrt(norm);
            // for(int i=0;i<num_docs;i++) hubs_new[i]/=norm;

            // print(norm);

            // error_hubs = 0;
            // error_authorities = 0;
            // for (int i = 0; i < num_docs; i++) {
            // error_hubs += Math.abs(hubs_new[i] - hubs.get(i));
            // error_authorities += Math.abs(authorities_new[i] - authorities.get(i));
            // hubs.put(i, hubs_new[i]);
            // authorities.put(i, authorities_new[i]);
            // }

            for (int i = 0; i < num_docs; i++) {
                hubs_new[i] = 0;
                authorities_new[i] = 0;
            }

            for (int i = 0; i < num_docs; i++) {
                // int linkId_i = titleToId.get(titles[i]);
                int linkId_i = i;
                for (int j = 0; j < num_docs; j++) {
                    // int linkId_j = titleToId.get(titles[j]);
                    int linkId_j = j;
                    if (out[linkId_i] != 0) {
                        if (link.get(linkId_i).get(linkId_j) != null) {
                            hubs_new[i] += authorities.get(j);
                            authorities_new[j] += hubs.get(i);
                        }
                    }
                }
            }

            double hub_sum = 0;
            double authority_sum = 0;
            for (int k = 0; k < num_docs; k++) {
                hub_sum += Math.pow(hubs_new[k], 2);
                authority_sum += Math.pow(authorities_new[k], 2);
            }

            error_hubs = 0;
            error_authorities = 0;
            for (int k = 0; k < num_docs; k++) {
                hubs_new[k] /= Math.sqrt(hub_sum);
                authorities_new[k] /= Math.sqrt(authority_sum);
                error_hubs += Math.abs(hubs_new[k] - hubs.get(k));
                error_authorities += Math.abs(authorities_new[k] - authorities.get(k));
                hubs.put(k, hubs_new[k]);
                authorities.put(k, authorities_new[k]);
            }

            print("Iteration: " + iter + " | Hubs error: " + error_hubs + " | Authorities error: " + error_authorities);
            iter++;
        }
    }

    /**
     * Rank the documents in the subgraph induced by the documents present in the
     * postings list `post`.
     *
     * @param post The list of postings fulfilling a certain information need
     *
     * @return A list of postings ranked according to the hub and authority scores.
     */
    PostingsList rank(PostingsList post) {
        //
        // YOUR CODE HERE
        //

        PostingsList post_new = preprocess(post);

        // post_new = post;

        String[] titles = new String[post_new.size()];

        for (int i = 0; i < post_new.size(); i++)
            titles[i] = Index.docNames.get(post_new.get(i).docID);

        iterate(titles);

        PostingsList result = new PostingsList();

        for (int i = 0; i < post_new.size(); i++) {
            double score_hub = hubs.get(i);
            double score_auth = authorities.get(i);
            double score_final = hubs_coefficient * score_hub + score_auth * authorities_coefficient;
            PostingsEntry entry = new PostingsEntry(post_new.get(i).docID);
            entry.addScore(score_final);
            result.insert(entry);
        }
        return result;
    }

    PostingsList preprocess(PostingsList post) {
        
        Set<Integer> docIDs = new HashSet<Integer>();
        for (int i = 0; i < post.size(); i++) {
            docIDs.add(post.get(i).docID);
        }

        print(Index.docIDs.size());

        for (int i = 0; i < post.size(); i++) {
            int internalID = post.get(i).docID;
            String filename = getFileName(Index.docNames.get(internalID));
            if(outlinks.containsKey(filename)){
                for(int externalID: outlinks.get(filename)){
                    if(Index.docIDs.containsKey(externalID_to_name.get(externalID))) {
                        docIDs.add(Index.docIDs.get(externalID_to_name.get(externalID)));
                    }
                }
            }
            if(inlinks.containsKey(filename)){
                for(int externalID: inlinks.get(filename)){
                    if(Index.docIDs.containsKey(externalID_to_name.get(externalID))) {
                        docIDs.add(Index.docIDs.get(externalID_to_name.get(externalID)));
                    }
                }
            }
        }

        PostingsList post_new = new PostingsList();

        print(docIDs.size());

        for (int docID : docIDs) {
            String filename = getFileName(Index.docNames.get(docID));
            // print(docID);
            if (titleToId.containsKey(filename))
                post_new.insert(new PostingsEntry(docID));
        }

        return post_new;
    }

    /**
     * Sort a hash map by values in the descending order
     *
     * @param map A hash map to sorted
     *
     * @return A hash map sorted by values
     */
    private HashMap<Integer, Double> sortHashMapByValue(HashMap<Integer, Double> map) {
        if (map == null) {
            return null;
        } else {
            List<Map.Entry<Integer, Double>> list = new ArrayList<Map.Entry<Integer, Double>>(map.entrySet());

            Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
                public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
                    return (o2.getValue()).compareTo(o1.getValue());
                }
            });

            HashMap<Integer, Double> res = new LinkedHashMap<Integer, Double>();
            for (Map.Entry<Integer, Double> el : list) {
                res.put(el.getKey(), el.getValue());
            }
            return res;
        }
    }

    /**
     * Write the first `k` entries of a hash map `map` to the file `fname`.
     *
     * @param map   A hash map
     * @param fname The filename
     * @param k     A number of entries to write
     */
    void writeToFile(HashMap<Integer, Double> map, String fname, int k) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fname));

            if (map != null) {
                int i = 0;
                for (Map.Entry<Integer, Double> e : map.entrySet()) {
                    i++;
                    writer.write(e.getKey() + ": " + String.format("%.5g%n", e.getValue()));
                    if (i >= k)
                        break;
                }
            }
            writer.close();
        } catch (IOException e) {
        }
    }

    /**
     * Rank all the documents in the links file. Produces two files: hubs_top_30.txt
     * with documents containing top 30 hub scores authorities_top_30.txt with
     * documents containing top 30 authority scores
     */
    void rank() {
        iterate(titleToId.keySet().toArray(new String[0]));
        HashMap<Integer, Double> sortedHubs = sortHashMapByValue(hubs);
        HashMap<Integer, Double> sortedAuthorities = sortHashMapByValue(authorities);
        writeToFile(sortedHubs, "hubs_top_30.txt", 30);
        writeToFile(sortedAuthorities, "authorities_top_30.txt", 30);
    }

    /* --------------------------------------------- */

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Please give the names of the link and title files");
        } else {
            HITSRanker hr = new HITSRanker(args[0], args[1], null);
            hr.rank();
        }
    }

    public void print(Object o) {
        System.out.println(o);
    }
}
