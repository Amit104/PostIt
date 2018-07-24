package com.example.amwadatk.postit;


public class Groupdata
{
    String name;
    int counter;

    public Groupdata(){}
    public Groupdata(String name)
    {
        this.name = name;
        this.counter = 0;
    }

    public int getCounter() {
        return counter;
    }

    public String getName() {
        return name;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public void setName(String name) {
        this.name = name;
    }
}
