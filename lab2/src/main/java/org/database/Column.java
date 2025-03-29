package org.database;


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

    public boolean getisUnique() {return isUnique;}

    public boolean getisNotNull() {return isNotNull;}
}

