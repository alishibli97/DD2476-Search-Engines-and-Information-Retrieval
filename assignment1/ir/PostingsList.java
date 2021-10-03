/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */  

package ir;

import java.util.*;
import java.util.stream.Collectors;

public class PostingsList {
    
    /** The postings list */
    private ArrayList<PostingsEntry> list = new ArrayList<PostingsEntry>();


    private ArrayList<PostingsEntry> filtered_list = new ArrayList<PostingsEntry>();

    private ArrayList<Integer> docIDsList = new ArrayList<Integer>();
    private ArrayList<Integer> docIDsList_all = new ArrayList<Integer>();

    // private String list_string = "";

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        String result = "";
        for(PostingsEntry entry: this.list){
            String docID = String.valueOf(entry.docID);
            String offset = String.valueOf(entry.offset);
            result += docID + " " + offset+",";
        }
        return result;
        // return super.toString();
    }

    public ArrayList<PostingsEntry> get_list(){
        return this.list;
    }

    public String get_string_list(){
        return this.list.stream().map(Object::toString).collect(Collectors.joining(", "));
    }

    /** Number of postings in this list. */
    public int size() {
    return list.size();
    }

    /** Returns the ith posting. */
    public PostingsEntry get( int i ) {
        return list.get( i );
    }

    // 
    //  YOUR CODE HERE
    //
    public void insert(PostingsEntry entry){
        this.list.add(entry);
        this.docIDsList_all.add(entry.docID);
        if(!docIDsList.contains(entry.docID)){
            this.filtered_list.add(entry);
            this.docIDsList.add(entry.docID);
        }
        // this.list_string = this.list_string.concat(entry.docID + " " + entry.offset+",");
    }

    public boolean check_docID(int docID){
        return this.docIDsList.contains(docID);
    }

    public ArrayList<PostingsEntry> get_filtered_list(){
        return this.filtered_list;
    }

    public ArrayList<Integer> get_IDs(){
        Collections.sort(this.docIDsList);
        return this.docIDsList;
    }

    public ArrayList<Integer> get_all_IDs(){
        Collections.sort(this.docIDsList_all);
        return this.docIDsList_all;
    }

    public int get_entry_offset_by_ID(int docID){
        for(PostingsEntry entry: this.list){
            if(entry.docID==docID) {
                this.list.remove(entry);
                return entry.offset;
            }
        }
        return 0;
    }
}

