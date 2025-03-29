import java.util.ArrayList;
import java.util.List;

/**
 * Spatial partitioning system for efficient range queries on encoded data
 * Works similarly to a zone map, dividing data into contiguous partitions
 */
public class ZoneMap{
    private ArrayList<Short> partitionMaxValues;
    private ArrayList<Integer> partitionEndIndices;
    
    /**
     * Constructor initializes partition lists
     */
    public ZoneMap() {
        partitionMaxValues = new ArrayList<>();
        partitionEndIndices = new ArrayList<>();
    }
    
    /**
     * Adds a partition with its maximum value and end index
     */
    public void addZone(short maxValue, int endIndex) {
        partitionMaxValues.add(maxValue);
        partitionEndIndices.add(endIndex);
    }
    
    /**
     * Displays partition information for debugging
     */
    public void printZones() {
        System.out.println("Zone Largest Arr:");
        for (int i = 0; i < partitionMaxValues.size(); i++) {
            System.out.printf("%d,", partitionMaxValues.get(i));
        }
        System.out.printf("\n");
    }
    
    /**
     * Gets partition boundaries for a value range query
     * Returns array with [startPartitionLower, startPartitionUpper, endPartitionLower, endPartitionUpper]
     */
    public int[] getZone(short startValue, short endValue) {
        int[] boundaries = new int[4];
        
        // Initialize with invalid values
        boundaries[0] = -1; // Start value: lower bound
        boundaries[1] = -1; // Start value: upper bound
        boundaries[2] = -1; // End value: lower bound
        boundaries[3] = -1; // End value: upper bound
        
        // Find partition containing start value
        int startPartition = findZone(0, partitionMaxValues.size() - 1, startValue);
        
        // Find partition containing end value
        int endPartition = findZone(startPartition, partitionMaxValues.size() - 1, endValue);
        
        // Calculate boundary indices for start value
        if (startPartition > 0) {
            // If not in first partition, start from end of previous partition + 1
            boundaries[0] = partitionEndIndices.get(startPartition - 1) + 1;
        } else {
            // If in first partition, start from index 0
            boundaries[0] = 0;
        }
        boundaries[1] = partitionEndIndices.get(startPartition);
        
        // Calculate boundary indices for end value
        if (endPartition > 0) {
            // If not in first partition, start from end of previous partition + 1
            boundaries[2] = partitionEndIndices.get(endPartition - 1) + 1;
        } else {
            // If in first partition, start from index 0
            boundaries[2] = 0;
        }
        boundaries[3] = partitionEndIndices.get(endPartition);
        
        return boundaries;
    }
    
    /**
     * Finds the partition containing a given value
     * Uses binary search for larger datasets and linear search for smaller ranges
     */
    private int findZone(int startIndex, int endIndex, short searchValue) {
        int current = startIndex;
        int last = endIndex;
        int rangeSize, mid;
        
        while (true) {
            rangeSize = last - current;
            
            if (rangeSize <= 5) {
                // Small range - use linear search for better performance
                for (int i = current; i <= endIndex; i++) {
                    if (searchValue <= partitionMaxValues.get(i)) {
                        return i;
                    }
                }
                // If we get here, value must be in the last partition
                return current;
            } else {
                // Larger range - use binary search
                mid = current + (rangeSize / 2);
                
                if (searchValue < partitionEndIndices.get(mid)) {
                    if (searchValue > partitionEndIndices.get(mid - 1)) {
                        return mid;
                    } else {
                        // Value in lower half
                        last = mid;
                    }
                } else {
                    // Value in upper half
                    current = mid;
                }
            }
        }
    }
}