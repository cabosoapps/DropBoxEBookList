package com.example.ebooklistingdb;

public class EBook {
    public String time;
    public String title;
    public String path;
    
    /**
     * Class EBook cover the file,time and path of a EPUB file. 
     * **/   
    public EBook(String time, String title,String path) {     
        this.time = time;
        this.title = title;
        this.path = path;
    }
}