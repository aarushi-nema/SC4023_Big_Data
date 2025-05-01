import java.io.*;
import java.util.*;

/**
 * A column-oriented data store for property transaction records
 * Optimized for querying housing property data across different dimensions
 */
public class PropertyDataStore {
    // Column-oriented data storage
    private ArrayList<Column> columns;
    
    // Query parameters
    private String targetLocation;
    private ArrayList<Integer> targetMonths;
    private ArrayList<String> targetMonthsFormatted;
    private String targetYear;
    
    // Constants
    private static final double AREA_THRESHOLD = 80.0;
    
    // Data indexing structures
    private ArrayList<Short> encodedRecords;
    private Map<String, Integer> locationCodeMap;
    private Map<String, Integer> dateCodeMap;
    private MultiKeyIndex mki;
    private ZoneMap zoneMap;
    
    // Statistics output formatting
    private String outputPrefix;
    
    // Column indices for easy access
    private static final int COL_DATE = 0;
    private static final int COL_LOCATION = 1;
    private static final int COL_PROPERTY_TYPE = 2;
    private static final int COL_BLOCK_NUM = 3;
    private static final int COL_STREET = 4;
    private static final int COL_LEVEL_RANGE = 5;
    private static final int COL_AREA = 6;
    private static final int COL_MODEL = 7;
    private static final int COL_LEASE_START = 8;
    private static final int COL_PRICE = 9;
    
    /**
     * Constructor initializes the data store and loads data
     */
    public PropertyDataStore() {
        // Initialize column structure
        columns = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            columns.add(new Column());
        }
        
        // Initialize compression lists
        townCompressList = new ArrayList<>();
        dateCompressList = new ArrayList<>();
        
        try {
            loadDB();
        } catch (IOException e) {
            System.err.println("Failed to load data: " + e.getMessage());
            throw new RuntimeException("Data loading failed", e);
        }
    }
    
    public static ArrayList<QuerySpec> sharedScanQuerySpec(List<Integer> identifierDigitsList) {
        String[] locations = {
            "BEDOK", "BUKIT PANJANG", "CLEMENTI", "CHOA CHU KANG", "HOUGANG",
            "JURONG WEST", "PASIR RIS", "TAMPINES", "WOODLANDS", "YISHUN"
        };

        int[][] monthPairs = {
            {10, 11}, {1, 2}, {2, 3}, {3, 4}, {4, 5},
            {5, 6}, {6, 7}, {7, 8}, {8, 9}, {9, 10}
        };

        String[][] formattedMonths = {
            {"10", "11"}, {"01", "02"}, {"02", "03"}, {"03", "04"}, {"04", "05"},
            {"05", "06"}, {"06", "07"}, {"07", "08"}, {"08", "09"}, {"09", "10"}
        };

        int[] yearMapping = {2020, 2021, 2022, 2023, 2014, 2015, 2016, 2017, 2018, 2019};

        ArrayList<QuerySpec> specList = new ArrayList<>();

        for (int identifierDigits : identifierDigitsList) {
            int locationCode = identifierDigits / 100;
            int monthCode = (identifierDigits / 10) % 10;
            int yearCode = identifierDigits % 10;

            String targetLocation = locations[locationCode];
            ArrayList<Integer> targetMonths = new ArrayList<>(Arrays.asList(
                monthPairs[monthCode][0], monthPairs[monthCode][1]
            ));
            ArrayList<String> targetMonthsFormatted = new ArrayList<>(Arrays.asList(
                formattedMonths[monthCode][0], formattedMonths[monthCode][1]
            ));
            String targetYear = String.valueOf(yearMapping[yearCode]);

            System.out.println("Town: " + targetLocation);
            System.out.println("Months: " + targetMonths.get(0) + ", " + targetMonths.get(1));
            System.out.println("Year: " + targetYear);
            System.out.println("");
            specList.add(new QuerySpec(targetLocation, targetMonths, targetMonthsFormatted, targetYear));
        }

        return specList;
    }

    /**
     * Configures query parameters based on the user's matriculation number
     */
    public void initQuerySpec(int identifierDigits) {
        // Extract individual parameters from the 3-digit code
        int locationCode = identifierDigits / 100;
        int monthCode = (identifierDigits / 10) % 10;
        int yearCode = identifierDigits % 10;
        
        // Define locations mapping
        String[] locations = {
            "BEDOK", "BUKIT PANJANG", "CLEMENTI", "CHOA CHU KANG", "HOUGANG",
            "JURONG WEST", "PASIR RIS", "TAMPINES", "WOODLANDS", "YISHUN"
        };
        
        // Define month pairs (consecutive months for analysis)
        int[][] monthPairs = {
            {10, 11}, {1, 2}, {2, 3}, {3, 4}, {4, 5},
            {5, 6}, {6, 7}, {7, 8}, {8, 9}, {9, 10}
        };
        
        // Month formatting with leading zeros
        String[][] formattedMonths = {
            {"10", "11"}, {"01", "02"}, {"02", "03"}, {"03", "04"}, {"04", "05"},
            {"05", "06"}, {"06", "07"}, {"07", "08"}, {"08", "09"}, {"09", "10"}
        };
        
        // Years mapping
        int[] yearMapping = {2020, 2021, 2022, 2023, 2014, 2015, 2016, 2017, 2018, 2019};
        
        // Set query parameters
        targetLocation = locations[locationCode];
        targetMonths = new ArrayList<>(Arrays.asList(monthPairs[monthCode][0], monthPairs[monthCode][1]));
        targetMonthsFormatted = new ArrayList<>(Arrays.asList(formattedMonths[monthCode][0], formattedMonths[monthCode][1]));
        targetYear = String.valueOf(yearMapping[yearCode]);
        
        // Log query parameters
        System.out.println("Town: " + targetLocation);
        System.out.println("Months: " + targetMonths.get(0) + ", " + targetMonths.get(1));
        System.out.println("Year: " + targetYear);
        
        // Create output file naming format
        outputPrefix = String.format("%s,%s,%s", targetYear, targetMonthsFormatted.get(0), targetLocation);
    }
    
    /**
     * Loads data from CSV file into column store
     */
    private void loadDB() throws IOException {
        String filePath = "ResalePricesSingapore.csv";
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        int lineCount = 0;
        
        while ((line = reader.readLine()) != null) {
            lineCount++;
            if (lineCount == 1) {
                // Skip header row
                continue;
            }
            
            // Parse CSV row
            String[] fields = line.split(",");
            
            // Validate row data
            if (hasEmptyFields(fields)) {
                System.out.println("Warning: Row with empty fields: " + Arrays.toString(fields));
                continue;
            }
            
            // Add data to columns
            for (int i = 0; i < 10; i++) {
                columns.get(i).add(fields[i]);
            }
            
            // Track date range and locations for encoding
            String date = fields[COL_DATE];
            String location = fields[COL_LOCATION];
            
            if (!townCompressList.contains(location)) {
                townCompressList.add(location);
            }
            
            // Extract year for date encoding
            int year = Integer.parseInt(date.split("-")[0]);
            compareDate(year);
        }
        
        reader.close();
        System.out.println("Data loaded: " + (lineCount - 1) + " records");
        
        // Verify column consistency
        if (allColumnsEqualLength()) {
            System.out.println("Column store database created successfully!");
        } else {
            System.out.println("Error: Columns have different lengths");
            System.exit(0);
        }
    }
    
    // Track year range for date encoding
    private int smallestYear = 9999;
    private int largestYear = 0;
    
    // Track compression mappings
    private ArrayList<String> townCompressList;
    private ArrayList<String> dateCompressList;
    
    private void compareDate(int year) {
        if (year > this.largestYear) {
            this.largestYear = year;
        } 
        if (year < this.smallestYear) {
            this.smallestYear = year;
        }
    }
    
    /**
     * Check for empty or null fields in a data row
     */
    private boolean hasEmptyFields(String[] fields) {
        for (String field : fields) {
            if (field == null || field.isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Verify all columns have the same length
     */
    private boolean allColumnsEqualLength() {
        int firstColSize = columns.get(0).size();
        for (int i = 1; i < columns.size(); i++) {
            if (columns.get(i).size() != firstColSize) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Creates encoded values for efficient querying
     */
    public void compressTownDate() {
        // Build date encoding map if not already built
        if (dateCompressList.isEmpty()) {
            // Create date codes for all possible months in range
            for (int y = this.smallestYear; y <= this.largestYear; y++) {
                for (int m = 1; m <= 12; m++) {
                    String dateFormat = String.format("%d-%02d", y, m);
                    dateCompressList.add(dateFormat);
                }
            }
        }
        
        // Encode each record
        encodedRecords = new ArrayList<>(columns.get(0).size());
        for (int i = 0; i < columns.get(0).size(); i++) {
            String date = columns.get(COL_DATE).get(i);
            String location = columns.get(COL_LOCATION).get(i);
            
            short encodedValue = getCompressValue(location, date);
            encodedRecords.add(encodedValue);
        }
    }
    
    /**
     * Encodes a location and date into a compact representation
     */
    private short getCompressValue(String town, String dateString) {
        int townIndex = townCompressList.indexOf(town);
        int dateIndex = dateCompressList.indexOf(dateString);
        return (short)((townIndex * 1000) + dateIndex);
    }
    
    /**
     * Sorts data by encoded values for faster range queries
     */
    public void sortByCompressedData() {
        int recordCount = encodedRecords.size() - 1;
        
        // Build heap structure
        for (int i = ((recordCount / 2) - 1); i >= 0; i--) {
            heapSort(recordCount, i);
        }
        
        // Extract elements from heap
        for (int i = recordCount - 1; i > 0; i--) {
            sortSwapArrayIndex(0, i);
            heapSort(i, 0);
        }
    }
    
    /**
     * Helper function for heap sort algorithm
     */
    private void heapSort(int heapSize, int rootIndex) {
        int largest = rootIndex;
        int leftChild = 2 * rootIndex + 1;
        int rightChild = 2 * rootIndex + 2;
        
        short largestVal = encodedRecords.get(largest);
        
        // Check if left child is larger than root
        if (leftChild < heapSize) {
            short leftChildVal = encodedRecords.get(leftChild);
            if (leftChildVal > largestVal) {
                largest = leftChild;
                largestVal = leftChildVal;
            }
        }
        
        // Check if right child is larger than root
        if (rightChild < heapSize) {
            short rightChildVal = encodedRecords.get(rightChild);
            if (rightChildVal > largestVal) {
                largest = rightChild;
            }
        }
        
        // If largest is not root
        if (largest != rootIndex) {
            sortSwapArrayIndex(rootIndex, largest);
            heapSort(heapSize, largest);
        }
    }
    
    /**
     * Swaps two records across all columns
     */
    public void sortSwapArrayIndex(int i, int j) {
        // Swap encoded value
        Collections.swap(encodedRecords, i, j);
        
        // Swap values in all columns
        for (int k = 0; k < columns.size(); k++) {
            columns.get(k).swap(i, j);
        }
    }
    
    /**
     * Builds a multi-dimensional index for faster querying
     */
    public void buildIndex() {
        mki = new MultiKeyIndex(10, 12, 10);
        
        for (int i = 0; i < columns.get(0).size(); i++) {
            String dateStr = columns.get(COL_DATE).get(i);
            String town = columns.get(COL_LOCATION).get(i);
            
            // Parse date components
            String[] dateParts = dateStr.split("-");
            int yearDigit = Integer.parseInt(dateParts[0]) % 10;  // Last digit of year
            int month = Integer.parseInt(dateParts[1]);
            
            // Get location code
            int locationCode = mapTownToIndex(town);
            
            // Add record index to the spatial index
            mki.addValue(yearDigit, month, locationCode, i);
        }
        
        System.out.println("Multi-Key Index built, size: " + mki.size());
    }
    
    /**
     * Creates spatial partitions for improved range queries
     */
    public void createZoneMap() {
        zoneMap = new ZoneMap();
        short largest = Short.MIN_VALUE;
        int zoneSize = 18;  // Optimal partition size
        int count = 0;
        
        for (int i = 0; i < encodedRecords.size(); i++) {
            if (encodedRecords.get(i) > largest) {
                count++;
                if (count > zoneSize) {
                    count = 1;
                    zoneMap.addZone(largest, i - 1);
                }
                largest = encodedRecords.get(i);
            }
        }
        
        // Add final zone
        if (count > 0) {
            zoneMap.addZone(largest, encodedRecords.size() - 1);
        }
    }
    
    /**
     * Maps location string to index
     */
    public int mapTownToIndex(String town) {
        String[] locations = {
            "BEDOK", "BUKIT PANJANG", "CLEMENTI", "CHOA CHU KANG", "HOUGANG",
            "JURONG WEST", "PASIR RIS", "TAMPINES", "WOODLANDS", "YISHUN"
        };
        return Arrays.asList(locations).indexOf(town);
    }
    
    public Map<QuerySpec, ArrayList<Integer>> sharedScanQueryDB(ArrayList<QuerySpec> specs) {
        Map<QuerySpec, ArrayList<Integer>> resultMap = new HashMap<>();
        Map<QuerySpec, ArrayList<Integer>> dateFilteredMap = new HashMap<>();

        // Initialize both maps per spec
        for (QuerySpec spec : specs) {
            resultMap.put(spec, new ArrayList<>());
            dateFilteredMap.put(spec, new ArrayList<>());
        }

        // Step 1: Shared loop over all records (filter by date for each spec)
        for (int i = 0; i < columns.get(0).size(); i++) {
            String dateString = columns.get(COL_DATE).get(i);
            String[] dateParts = dateString.split("-");
            String year = dateParts[0];
            int month = Integer.parseInt(dateParts[1]);

            for (QuerySpec spec : specs) {
                if (year.equals(spec.targetYear) && spec.targetMonths.contains(month)) {
                    dateFilteredMap.get(spec).add(i); // Append to this spec's list
                }
            }
        }   

        // Step 2: For each spec, apply town + area filtering
        for (QuerySpec spec : specs) {
            ArrayList<Integer> finalResults = resultMap.get(spec);
            ArrayList<Integer> dateFiltered = dateFilteredMap.get(spec);

            for (int i : dateFiltered) {
                String town = columns.get(COL_LOCATION).get(i);
                double floorArea = Double.parseDouble(columns.get(COL_AREA).get(i));

                if (town.equals(spec.targetLocation) && floorArea >= AREA_THRESHOLD) {
                    finalResults.add(i);
                }
            }
        }

        return resultMap;
    }


    /**
     * Basic query method - sequential scan
     */
    public ArrayList<Integer> queryDB() {
        ArrayList<Integer> posArray = new ArrayList<>();
        ArrayList<Integer> finalPosArray = new ArrayList<>();
        
        // First filter by date (most selective)
        for (int i = 0; i < columns.get(0).size(); i++) {
            String dateString = columns.get(COL_DATE).get(i);
            String[] dateParts = dateString.split("-");
            String year = dateParts[0];
            int month = Integer.parseInt(dateParts[1]);
            
            // Check date criteria
            if (targetYear.equals(year) && targetMonths.contains(month)) {
                posArray.add(i);
            }
        }
        
        // Then filter by location and area
        for (int i : posArray) {
            String town = columns.get(COL_LOCATION).get(i);
            double floorArea = Double.parseDouble(columns.get(COL_AREA).get(i));
            
            // Check location and area criteria
            if (town.equals(targetLocation) && floorArea >= AREA_THRESHOLD) {
                finalPosArray.add(i);
            }
        }
        
        return finalPosArray;
    }
    
    /**
     * Query using the multi-dimensional index
     */
    // public ArrayList<Integer> queryDBIndex() {
    //     ArrayList<Integer> posArray = new ArrayList<>();
    //     ArrayList<Integer> finalPosArray = new ArrayList<>();
        
    //     // Get location index
    //     int townIndex = mapTownToIndex(targetLocation);
        
    //     // Get year index (last digit)
    //     int yearDigit = Integer.parseInt(targetYear) % 10;
        
    //     // Query index for each target month
    //     for (Integer month : targetMonths) {
    //         posArray.addAll(mki.queryIndex(yearDigit, month, townIndex));
    //     }
        
    //     // Apply area filter
    //     for (int i : posArray) {
    //         double floorArea = Double.parseDouble(columns.get(COL_AREA).get(i));
    //         if (floorArea >= AREA_THRESHOLD) {
    //             finalPosArray.add(i);
    //         }
    //     }
        
    //     return finalPosArray;
    // }

    public ArrayList<Integer> queryDBIndex() {
        ArrayList<Integer> finalPosArray = new ArrayList<>();
    
        // Get location index
        int townIndex = mapTownToIndex(targetLocation);
    
        // Get year index (last digit) – keep it for querying but fix filtering below
        int yearDigit = Integer.parseInt(targetYear) % 10;
    
        // Query index for each target month
        for (Integer month : targetMonths) {
            ArrayList<Integer> posArray = mki.queryIndex(yearDigit, month, townIndex);
    
            // Additional filtering to match full year and area
            for (int i : posArray) {
                String fullDate = columns.get(COL_DATE).get(i);
                String yearFromData = fullDate.split("-")[0];
    
                if (yearFromData.equals(targetYear)) {
                    double floorArea = Double.parseDouble(columns.get(COL_AREA).get(i));
                    if (floorArea >= AREA_THRESHOLD) {
                        finalPosArray.add(i);
                    }
                }
            }
        }
    
        return finalPosArray;
    }
    
    
    /**
     * Query using encoded values and spatial partitions
     * Performance-optimized while maintaining correctness
     */
    public ArrayList<Integer> queryCompressedDB() {
        ArrayList<Integer> finalPosArray = new ArrayList<>();
        
        // Create encoded search values
        String startMonth = targetYear + "-" + targetMonthsFormatted.get(0);
        String endMonth = targetYear + "-" + targetMonthsFormatted.get(1);
        
        // Store town list as a class variable during compression for reuse
        if (townCompressList == null || townCompressList.isEmpty()) {
            // This should have been populated during compressTownDate, but recreate if needed
            townCompressList = new ArrayList<>();
            for (int i = 0; i < columns.get(0).size() && townCompressList.size() < 20; i++) {
                String town = columns.get(COL_LOCATION).get(i);
                if (!townCompressList.contains(town)) {
                    townCompressList.add(town);
                }
            }
        }
        
        // Store date list as a class variable during compression for reuse
        if (dateCompressList == null || dateCompressList.isEmpty()) {
            // This should have been populated during compressTownDate, but recreate if needed
            dateCompressList = new ArrayList<>();
            for (int y = this.smallestYear; y <= this.largestYear; y++) {
                for (int m = 1; m <= 12; m++) {
                    String dateFormat = String.format("%d-%02d", y, m);
                    dateCompressList.add(dateFormat);
                }
            }
        }
        
        // Calculate compressed values once
        short compressedValueStart = getCompressValue(targetLocation, startMonth);
        short compressedValueEnd = getCompressValue(targetLocation, endMonth);
        
        // Get zone map locations - fast path for performance
        int[] indexArr = zoneMap.getZone(compressedValueStart, compressedValueEnd);
        
        // Use simplified boundary search that's still accurate but faster
        int startIndex = indexArr[0]; 
        int endIndex = indexArr[3];
        
        // Perform a direct binary search if zone bounds are too large
        if (endIndex - startIndex > 1000) {
            startIndex = findCompressedValueFast(encodedRecords, startIndex, endIndex, compressedValueStart, true);
            endIndex = findCompressedValueFast(encodedRecords, startIndex, endIndex, compressedValueEnd, false);
        }
        
        // Apply area filter directly - with bounds checking for safety
        if (startIndex >= 0 && endIndex >= 0 && startIndex < encodedRecords.size() && endIndex < encodedRecords.size()) {
            for (int i = startIndex; i <= endIndex; i++) {
                if (i >= encodedRecords.size()) break;
                
                short encoded = encodedRecords.get(i);
                if (encoded >= compressedValueStart && encoded <= compressedValueEnd) {
                    double floorArea = Double.parseDouble(columns.get(COL_AREA).get(i));
                    if (floorArea >= AREA_THRESHOLD) {
                        finalPosArray.add(i);
                    }
                }
            }
        }
        
        return finalPosArray;
    }
    
    /**
     * Fast binary search for compressed values
     * @param values The list to search in
     * @param start Start index
     * @param end End index
     * @param target Target value
     * @param findFirst If true, find first occurrence, otherwise find last
     * @return The index of the target or the appropriate insertion point
     */
    private int findCompressedValueFast(ArrayList<Short> values, int start, int end, short target, boolean findFirst) {
        // Standard binary search with optimization for finding boundaries
        int left = start;
        int right = end;
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            
            if (mid >= values.size()) {
                return values.size() - 1; // Safety check
            }
            
            short midVal = values.get(mid);
            
            if (midVal < target) {
                left = mid + 1;
            } else if (midVal > target) {
                right = mid - 1;
            } else {
                // Found match, now find boundary
                if (findFirst) {
                    // Find first occurrence
                    if (mid == 0 || values.get(mid - 1) < target) {
                        return mid;
                    }
                    right = mid - 1;
                } else {
                    // Find last occurrence
                    if (mid == values.size() - 1 || values.get(mid + 1) > target) {
                        return mid;
                    }
                    left = mid + 1;
                }
            }
        }
        
        return findFirst ? left : right;
    }
    
    /**
     * Finds the start position of a value range
     */
    public int findValueStartPositionInCompressedDb(int startFind, int endFind, int searchValue) {
        int middle, midVal, findAreaSize;
        int endBackup = endFind;

        // Check if search value is at start
        if (startFind < encodedRecords.size()) {
            midVal = encodedRecords.get(0);
            if (startFind == midVal) {
                return 0;
            }
        }

        while (true) {
            findAreaSize = endFind - startFind;
            if (findAreaSize <= 50) {
                // Small range - use sequential search
                for (int i = startFind; i <= endBackup && i < encodedRecords.size(); i++) {
                    midVal = encodedRecords.get(i);
                    if (midVal == searchValue) {
                        return i;
                    }
                }
                return startFind;
            } else {
                // Large range - use binary search
                middle = startFind + (findAreaSize / 2);
                if (middle < encodedRecords.size()) {
                    midVal = encodedRecords.get(middle);
                    if (midVal >= searchValue) {
                        // Value in lower half
                        endFind = middle;
                    } else {
                        // Value in upper half
                        startFind = middle;
                    }
                } else {
                    // Handle index out of bounds
                    endFind = middle;
                }
            }
        }
    }
    
    /**
     * Finds the end position of a value range
     */
    public int findValueEndPositionInCompressedDb(int startFind, int endFind, int searchValue) {
        int middle, midVal, findAreaSize;
        int endBackup = endFind;

        // Check if search value is at end
        if (endFind < encodedRecords.size()) {
            midVal = encodedRecords.get(endFind);
            if (searchValue == midVal) {
                return endFind;
            }
        }

        while (true) {
            findAreaSize = endFind - startFind;
            if (findAreaSize <= 50) {
                // Small range - use sequential search
                for (int i = startFind; i <= endBackup && i < encodedRecords.size(); i++) {
                    midVal = encodedRecords.get(i);
                    if (midVal > searchValue) {
                        return i - 1;
                    }
                }
                return endFind;
            } else {
                // Large range - use binary search
                middle = startFind + (findAreaSize / 2);
                if (middle < encodedRecords.size()) {
                    midVal = encodedRecords.get(middle);
                    if (midVal > searchValue) {
                        // Value in lower half
                        endFind = middle;
                    } else {
                        // Value in upper half
                        startFind = middle;
                    }
                } else {
                    // Handle index out of bounds
                    endFind = middle;
                }
            }
        }
    }
    
    // File output handling
    private FileWriter fw;
    
    /**
     * Creates output file for statistics
     */
    public void createOutputFile(String filename) {
        filename = filename + ".csv";
        try {
            File outputFile = new File(filename);
            outputFile.createNewFile();
            fw = new FileWriter(filename);
            fw.write("Year,Month,Town,Category,Value\n");
        } catch (IOException e) {
            System.err.println("Failed to create output file: " + e.getMessage());
            throw new RuntimeException("Output file creation failed", e);
        }
    }
    
    /**
     * Closes output file
     */
    public void closeOutputFile() {
        try {
            if (fw != null) {
                fw.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing output file: " + e.getMessage());
            throw new RuntimeException("Failed to close output file", e);
        }
    }
    
    /**
     * Writes a result to the output file
     */
    public void writeResults(int type, double value) {
        final String[] typeText = {
            "Minimum Price", 
            "Average Price", 
            "Standard Deviation of Price",
            "Minimum Price per Square Meter"
        };
        
        String writeContent = String.format("%s,%s,%f\n", 
                outputPrefix, typeText[type], value);
        
        try {
            fw.write(writeContent);
        } catch (IOException e) {
            System.err.println("Error writing to output file: " + e.getMessage());
            throw new RuntimeException("Failed to write results", e);
        }
    }
    
    /**
     * Calculates minimum price statistic
     */
    public void calculateMinPrice(String method) {
        ArrayList<Integer> posArray = getQueryResult(method);
        
        if (posArray == null || posArray.isEmpty()) {
            this.writeResults(0, 0);
            return;
        }
        
        double minPrice = Double.MAX_VALUE;
        for (int i : posArray) {
            double tempVal = Double.parseDouble(columns.get(COL_PRICE).get(i));
            if (minPrice > tempVal) {
                minPrice = tempVal;
            }
        }
        
        writeResults(0, minPrice);
    }
    
    /**
     * Calculates average price statistic
     */
    public void calculateAvgPrice(String method) {
        ArrayList<Integer> posArray = getQueryResult(method);
        
        if (posArray == null || posArray.isEmpty()) {
            this.writeResults(1, 0);
            return;
        }
        
        double totalPrice = 0;
        for (int i : posArray) {
            double tempVal = Double.parseDouble(columns.get(COL_PRICE).get(i));
            totalPrice += tempVal;
        }
        
        writeResults(1, totalPrice / posArray.size());
    }
    
    /**
     * Calculates price standard deviation statistic
     */
    public void calculatePriceSD(String method) {
        ArrayList<Integer> posArray = getQueryResult(method);
        
        if (posArray == null || posArray.isEmpty()) {
            this.writeResults(2, 0);
            return;
        }
        
        // Calculate mean
        double totalPrice = 0;
        double[] prices = new double[posArray.size()];
        int index = 0;
        
        for (int i : posArray) {
            double price = Double.parseDouble(columns.get(COL_PRICE).get(i));
            totalPrice += price;
            prices[index++] = price;
        }
        
        double mean = totalPrice / posArray.size();
        
        // Calculate variance
        double sumSquaredDiff = 0;
        for (double price : prices) {
            sumSquaredDiff += Math.pow(price - mean, 2);
        }
        
        double variance = sumSquaredDiff / (posArray.size() - 1); 
        double stdDev = Math.sqrt(variance);
        
        writeResults(2, stdDev);
    }
    
    /**
     * Calculates minimum price per square meter statistic
     */
    public void calculateMinPricePerSqm(String method) {
        ArrayList<Integer> posArray = getQueryResult(method);
        
        if (posArray == null || posArray.isEmpty()) {
            this.writeResults(3, 0);
            return;
        }
        
        double minPricePerSqm = Double.MAX_VALUE;
        
        for (int i : posArray) {
            double price = Double.parseDouble(columns.get(COL_PRICE).get(i));
            double area = Double.parseDouble(columns.get(COL_AREA).get(i));
            double pricePerSqm = price / area;
            
            if (pricePerSqm < minPricePerSqm) {
                minPricePerSqm = pricePerSqm;
            }
        }
        
        writeResults(3, minPricePerSqm);
    }
    
    /**
     * Gets matching records based on query method
     */
    private ArrayList<Integer> getQueryResult(String method) {
        if (method.equals("Index")) {
            return queryDBIndex();
        } else if (method.equals("Compressed+ZoneMap+Sorted")) {
            return queryCompressedDB();
        } else {
            return queryDB();
        }
    }
    
    /**
     * Calculates all statistics using basic query method
     */
    public void calculateDefault() {
        calculateMinPrice("Normal");
        calculateAvgPrice("Normal");
        calculatePriceSD("Normal");
        calculateMinPricePerSqm("Normal");
    }
    
    /**
     * Column class for the column store
     */
    private class Column {
        private ArrayList<String> values;
        
        public Column() {
            values = new ArrayList<>();
        }
        
        public void add(String value) {
            values.add(value);
        }
        
        public String get(int index) {
            return values.get(index);
        }
        
        public int size() {
            return values.size();
        }
        
        public void swap(int i, int j) {
            String temp = values.get(i);
            values.set(i, values.get(j));
            values.set(j, temp);
        }
    }
}
