package com.example.ebooklistingdb;

import java.util.ArrayList;


/**
 * BufferEPUBList, this class encapsulate sync methods to access to a buffer object (ArrayList<EBook>)
 * 
 * **/
public class BufferEPUBList {

    private ArrayList<EBook> arrayListEBookList;
    private boolean  completed;
    private boolean newdata;
    
    
    
    
    BufferEPUBList(){
    	this.arrayListEBookList = new ArrayList<EBook>();
    	this.completed = false;
    	this.newdata = false;
    }
    
    public synchronized int getEBooksNumber(){
    	return arrayListEBookList.size();

    }
    
    public  void clearBuffer(){
    	arrayListEBookList.clear();
    	completed = false;
    	newdata = false;
    }
    
    
    private synchronized void setnewdataPresent(boolean nd){
    	this.newdata = nd;    	
    }
    
    
    public synchronized boolean newdataPresent(){
    	return this.newdata;    	
    }
           
    
    public boolean iscomplete(){	    	
    	return completed;
    }
    
    public void setcompleted(boolean state){	    	
    	completed = state;
    }
    
    
    public void addnewEBook(EBook ebook) {
        synchronized(arrayListEBookList) {
        	arrayListEBookList.add(ebook);
        	this.setnewdataPresent(true);
        }
    }

    public ArrayList<EBook> getEBookList(){
    	synchronized(arrayListEBookList) {
    		this.setnewdataPresent(false);
    		return arrayListEBookList;
    	}
    	
    }
    
    

}
