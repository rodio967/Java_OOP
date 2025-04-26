package org.model;


public class Column {
    private String name;
    private String type;
    private boolean isUnique;
    private boolean isNotNull;

    public Column(String name, String type, boolean isUnique, boolean isNotNull) {
        this.name = name;
        this.type = type;
        this.isUnique = isUnique;
        this.isNotNull = isNotNull;
    }

    public String getName() {return name;}

    public String getType() {return type;}

    public boolean getIsUnique() {return isUnique;}

    public boolean getIsNotNull() {return isNotNull;}
}

