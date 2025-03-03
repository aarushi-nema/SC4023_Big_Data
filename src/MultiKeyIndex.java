import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MultiKeyIndex {
    // Multi key index implementation taught in Database System Principles
    // 1st level keys = Year
    // 2nd level keys = Month
    // 3rd level keys = Town
    private final int townMapperLength;
    private final int monthMapperLength;
    private final int yearMapperLength;
    private ArrayList<Integer>[][][] index;


    // Constructor
    public MultiKeyIndex(int yearLength, int monthLength, int townLength) {
        this.townMapperLength = townLength;
        this.monthMapperLength = monthLength;
        this.yearMapperLength = yearLength;

        // 3d array containing arrayList
        this.index = new ArrayList[yearLength][monthLength][townLength];
    }
    public int size() {
        int size = 0;
        for (int i = 0; i < this.yearMapperLength; i++) {
            for (int j = 0; j < this.monthMapperLength; j++) {
                for (int k = 0; k < this.townMapperLength; k++) {
                    if (this.index[i][j][k] != null) {
                        size += this.index[i][j][k].size();
                    }
                }
            }
        }
        return size;
    }
    // Add a value to the index
    public void addValue(int year, int month, int town, int colIndex) {
        // Data does not need to be indexed because not in scope of query
        if (town == -1) {
            return;
        }

        if (this.index[year][month-1][town] == null) {
            this.index[year][month-1][town] = new ArrayList<Integer>();
        }
        this.index[year][month-1][town].add(colIndex);
    }
    public ArrayList<Integer> queryIndex(int year, int month, int town) {
        if (this.index[year][month-1][town] == null) {
            return new ArrayList<Integer>();
        }
        return this.index[year][month-1][town];
    }

}
