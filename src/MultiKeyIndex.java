import java.util.ArrayList;
import java.util.List;

/**
 * Multi-dimensional index for efficient querying of property records
 * - Dimensions: year, month, location
 */
public class MultiKeyIndex {
    private final int yearDimension;
    private final int monthDimension;
    private final int locationDimension;
    
    // Three-dimensional array to store record indices
    private ArrayList<Integer>[][][] indexStructure;
    
    /**
     * Constructor to initialize the index with specified dimensions
     */
    @SuppressWarnings("unchecked")
    public MultiKeyIndex(int yearDim, int monthDim, int locationDim) {
        this.yearDimension = yearDim;
        this.monthDimension = monthDim;
        this.locationDimension = locationDim;
        
        // Initialize 3D array structure
        this.indexStructure = new ArrayList[yearDim][monthDim][locationDim];
    }
    
    /**
     * Returns the total number of indexed records
     */
    public int size() {
        int totalEntries = 0;
        
        for (int y = 0; y < yearDimension; y++) {
            for (int m = 0; m < monthDimension; m++) {
                for (int l = 0; l < locationDimension; l++) {
                    if (indexStructure[y][m][l] != null) {
                        totalEntries += indexStructure[y][m][l].size();
                    }
                }
            }
        }
        
        return totalEntries;
    }
    
    /**
     * Adds a record to the index
     * 
     * @param year Last digit of the year
     * @param month Month number (1-12)
     * @param location Location code
     * @param recordIndex Index of the record in the data store
     */
    public void addValue(int year, int month, int location, int recordIndex) {
        // Skip records that don't need indexing
        if (location < 0 || location >= locationDimension) {
            return;
        }
        
        // Ensure month is zero-indexed for array access
        int monthIndex = month - 1;
        
        // Initialize list if not already created
        if (indexStructure[year][monthIndex][location] == null) {
            indexStructure[year][monthIndex][location] = new ArrayList<Integer>();
        }
        
        // Add record to the index
        indexStructure[year][monthIndex][location].add(recordIndex);
    }
    
    /**
     * Queries the index for records matching the specified criteria
     * 
     * @param year Last digit of the year
     * @param month Month number (1-12)
     * @param location Location code
     * @return List of record indices matching the criteria
     */
    public ArrayList<Integer> queryIndex(int year, int month, int location) {
        // Ensure month is zero-indexed for array access
        int monthIndex = month - 1;
        
        // Return empty list if no matching records
        if (indexStructure[year][monthIndex][location] == null) {
            return new ArrayList<Integer>();
        }
        
        // Return list of matching record indices
        return new ArrayList<>(indexStructure[year][monthIndex][location]);
    }
}