import java.io.*;
import java.util.*;


public class Database {

    private ArrayList<ArrayList<String>> db;
    private ArrayList<Short> compressedList;

    // Query Specs
    private String filter_town;
    private ArrayList<Integer> filter_months;
    private ArrayList<String> filter_months_str;
    private String filter_years;
 
    // Minimum area requirement for all queries
    private static final double MIN_AREA_REQUIREMENT = 80.0;

    // Compression
    private int smallestYear = 9999;
    private int largestYear = 0;
    private ArrayList<String> townCompressList;
    private ArrayList<String> dateCompressList;

    // Zone Map
    private ZoneMap zoneMap;

    // MultiKeyIndex
    private MultiKeyIndex mki;

    // Append to file details
    String resultOutputAppend = "";

    public Database() {
        townCompressList = new ArrayList<String>();
        dateCompressList = new ArrayList<String>();
        try {
            this.loadDB();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void initQuerySpec(int last_3_mat_digit){
        // Split the 3 digits into separate digits
        int town_digit = last_3_mat_digit / 100;
        int month_digit = (last_3_mat_digit / 10) % 10;
        int year_digit = last_3_mat_digit % 10;

        // Town mapper string array based on Table 1
        String[] town_mapper = {
                "BEDOK",               // 0
                "BUKIT PANJANG",       // 1
                "CLEMENTI",            // 2
                "CHOA CHU KANG",       // 3
                "HOUGANG",             // 4
                "JURONG WEST",         // 5
                "PASIR RIS",           // 6
                "TAMPINES",            // 7
                "WOODLANDS",           // 8
                "YISHUN"               // 9
        };

        // Month mapper - now for 2 consecutive months as required
        int[][] monthMapper = {
                {10, 11},  // October, November (0 represents October)
                {1, 2},    // January, February
                {2, 3},    // February, March
                {3, 4},    // March, April
                {4, 5},    // April, May
                {5, 6},    // May, June
                {6, 7},    // June, July
                {7, 8},    // July, August
                {8, 9},    // August, September
                {9, 10}    // September, October
        };

        String[][] monthMapperStr = {
                {"10", "11"},  // October, November
                {"01", "02"},  // January, February
                {"02", "03"},  // February, March
                {"03", "04"},  // March, April
                {"04", "05"},  // April, May
                {"05", "06"},  // May, June
                {"06", "07"},  // June, July
                {"07", "08"},  // July, August
                {"08", "09"},  // August, September
                {"09", "10"}   // September, October
        };

        // Year mapper based on last digit
        int[] yearMapper = {
                2020,
                2021,
                2022,
                2023,
                2014,
                2015,
                2016,
                2017,
                2018,
                2019,
        };
        
        // Print and store query parameters
        System.out.println("Town: " + town_mapper[town_digit]);
        System.out.println("Months: " + monthMapper[month_digit][0] + ", " + monthMapper[month_digit][1]);
        System.out.println("Year: " + yearMapper[year_digit]);

        // Set all values needed for query (town, months, years)
        this.filter_town = town_mapper[town_digit];
        this.filter_months = new ArrayList<>(List.of(monthMapper[month_digit][0], monthMapper[month_digit][1]));
        this.filter_years = Integer.toString(yearMapper[year_digit]);
        this.filter_months_str = new ArrayList<>(List.of(monthMapperStr[month_digit][0], monthMapperStr[month_digit][1]));

        // Set Output File Append String
        resultOutputAppend = String.format("%d,%s,%s", yearMapper[year_digit], monthMapperStr[month_digit][0], town_mapper[town_digit]);
    }

    public void loadDB() throws IOException {
        //Function reads off csv and insert into column store as an Array list. The columns are aggregated together as
        //2D array initialized as db
        //If fails, it throws I/O exception
        // Read ResalePricesSingapore.csv
        String filename = "ResalePricesSingapore.csv";
        int currLineNum = 0;

        // Create array to store data
        // Read header and create arrays depending on the header names
        // month,town,flat_type,block,street_name,storey_range,floor_area_sqm,flat_model,lease_commence_date,resale_price
        ArrayList<String> monthCol = new ArrayList<String>();
        ArrayList<String> townCol = new ArrayList<String>();
        ArrayList<String> flatTypeCol = new ArrayList<String>();
        ArrayList<String> blockCol = new ArrayList<String>();
        ArrayList<String> streetNameCol = new ArrayList<String>();
        ArrayList<String> storeyRangeCol = new ArrayList<String>();
        ArrayList<String> floorAreaCol = new ArrayList<String>();
        ArrayList<String> flatModelCol = new ArrayList<String>();
        ArrayList<String> leaseCommenceDateCol = new ArrayList<String>();
        ArrayList<String> resalePriceCol = new ArrayList<String>();

        // Create an array to store all the arraylists
        this.db = new ArrayList<>(Arrays.asList(monthCol, townCol, flatTypeCol, blockCol, streetNameCol,
                storeyRangeCol, floorAreaCol, flatModelCol, leaseCommenceDateCol, resalePriceCol));

        // Parse CSV file into lines
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line = null;
        while((line = reader.readLine()) != null) {
            currLineNum++;
            // For every line, split string using delimiter
            char delimiter = ',';
            String[] split = line.split(String.valueOf(delimiter));

            // Skip headers
            if (currLineNum==1) {
                continue;
            } else {
                // Check if any are null
                if (Arrays.stream(split).anyMatch(Objects::isNull) || Arrays.stream(split).anyMatch(String::isEmpty)) {
                    // Check for NULL or empty string values
                    System.out.println(Arrays.toString(split));
                }

                // Append each part into the respective array
                monthCol.add(split[0]);
                townCol.add(split[1]);
                flatTypeCol.add(split[2]);
                blockCol.add(split[3]);
                streetNameCol.add(split[4]);
                storeyRangeCol.add(split[5]);
                floorAreaCol.add(split[6]);
                flatModelCol.add(split[7]);
                leaseCommenceDateCol.add(split[8]);
                resalePriceCol.add(split[9]);
            }
            compareDate(split[0]);
            // Check town indexed
            if (townCompressList.indexOf(split[1]) == -1) {
                townCompressList.add(split[1]);
            }
        }

        // Check if all data is correct read
        // Compare length of all columns arrays
        if (monthCol.size() == townCol.size() && townCol.size() == flatTypeCol.size() &&
                flatTypeCol.size() == blockCol.size() && blockCol.size() == streetNameCol.size() &&
                streetNameCol.size() == storeyRangeCol.size() && storeyRangeCol.size() == floorAreaCol.size() &&
                floorAreaCol.size() == flatModelCol.size() && flatModelCol.size() == leaseCommenceDateCol.size() &&
                leaseCommenceDateCol.size() == resalePriceCol.size()) {
            System.out.println("Column store database created successfully!");
        } else {
            // If not, print error message
            System.out.println("Error: Columns have different lengths");
            System.exit(0);
        }
    }

    // Remaining compression and indexing methods (unchanged)
    public void compareDate(String newDate) {
        int year = Integer.parseInt(newDate.split("-")[0]);
        if (year > this.largestYear) {
            this.largestYear = year;
        }else if (year < this.smallestYear){
            this.smallestYear = year;
        }
    }

    public void compressTownDate() {
        // Init dataCompressList
        for (int y=this.smallestYear; y <= this.largestYear;y++) {
            for (int m=1;m <= 12; m++) {
                String d = String.format("%d-%02d",y,m);
                dateCompressList.add(d);
            }
        }
        // Loop through
        this.compressedList = new ArrayList<Short>();
        for (int i = 0; i < this.db.get(0).size(); i++) {
            // Create compression values
            // Towns are indexed by 1000s
            // Date is indexed from 0 - 999, sufficient to cover 83 years of data
            String dateString = this.db.get(0).get(i);
            String town = this.db.get(1).get(i);
            short compressValue = getCompressValue(town, dateString);
            compressedList.add(compressValue);
        }
    }

    private short getCompressValue(String town, String dateString) {
        return (short) ((townCompressList.indexOf(town) * 1000) + (dateCompressList.indexOf(dateString)));
    }

    // Sorting methods (unchanged)
    public void sortSwapArrayIndex(int index, int swapPos) {
        for (int k=0; k < this.db.size(); k++) {
            Collections.swap(this.db.get(k), index, swapPos);
        }
        Collections.swap(compressedList, index, swapPos);
    }

    public void sortByCompressedData() {
        int size = this.compressedList.size()-1;

        // Build heap from Compressed Data Arraylist
        for (int i = ((size/2) - 1); i >= 0; i--) {
            // Split
            this.heapSort(size, i);
        }

        // Extract elements from heap 1by1
        for (int i = size-1; i > 0; i--) {
            // Swap root with end
            sortSwapArrayIndex(0,i);
            this.heapSort(i, 0);
        }
    }

    private void heapSort(int size, int root) {
        int largest = root;
        int leftChild =  (2*root) + 1;
        int rightChild = (2*root) + 2;

        short largestVal = this.compressedList.get(largest);

        // Check if left child largest
        if (leftChild < size) {
            short leftChildVal = this.compressedList.get(leftChild);

            if (leftChildVal > largestVal) {
                largest = leftChild;
                largestVal = leftChildVal;
            }
        }

        // Check if right child largest
        if (rightChild < size) {
            short rightChildVal = this.compressedList.get(rightChild);

            if (rightChildVal > largestVal) {
                largest = rightChild;
            }
        }

        // Check if largest is still root
        if (largest != root) {
            // Largest not root, swap largest with root
            sortSwapArrayIndex(root,largest);

            // HeapSort Subtree that was changed
            this.heapSort(size, largest);
        }
    }

    public void buildIndex(){
        // 10 years, 12 months, 10 towns
        MultiKeyIndex mki = new MultiKeyIndex(10,12,10);

        // Loop through all records
        for (int i = 0; i < this.db.get(0).size(); i++) {
            // For each record, get the year, month, and town
            String dateString = this.db.get(0).get(i);
            String town = this.db.get(1).get(i);
            // Split date string
            String[] parts = dateString.split("-");
            // Get last digit of year
            int yearDigit = Integer.parseInt(parts[0])%10;
            // Get month
            int month = Integer.parseInt(parts[1]);

            // Insert into index
            mki.addValue(yearDigit, month, mapTownToIndex(town), i);
        }

        // Set MultiKeyIndex
        this.mki = mki;
        System.out.println("Multi-Key Index built, size: " + mki.size());
    }

    public void createZoneMap() {
        zoneMap = new ZoneMap();
        short largest = Short.MIN_VALUE;
        int zoneSize = 18; // 3 Values per zone, eg. 1, 2, 3 = 1 Zone
        int count = 0;
        for (int i=0;i < this.compressedList.size();i++) {
            if (this.compressedList.get(i) > largest) {
                count++;
                // 3 Values in 1 zone
                if (count > zoneSize) {
                    count = 1;
                    zoneMap.addZone(largest, i-1);
                }

                largest = this.compressedList.get(i);
            }
        }

        // Last Zone
        if (count > 0) {
            zoneMap.addZone(largest, this.compressedList.size()-1);
        }
    }

    public int mapTownToIndex(String town) {
        String[] town_mapper = {
                "BEDOK",               // 0
                "BUKIT PANJANG",       // 1
                "CLEMENTI",            // 2
                "CHOA CHU KANG",       // 3
                "HOUGANG",             // 4
                "JURONG WEST",         // 5
                "PASIR RIS",           // 6
                "TAMPINES",            // 7
                "WOODLANDS",           // 8
                "YISHUN"               // 9
        };
        return Arrays.asList(town_mapper).indexOf(town);
    }

    // Query Functions 
    public ArrayList<Integer> queryDB(){
        //Function to querydb
        // Init pos array
        ArrayList<Integer> posArray = new ArrayList<Integer>();
        // Init final pos array
        ArrayList<Integer> finalPosArray = new ArrayList<Integer>();

        // Step 1: Try to query with the most constraining criteria (Year & Month) -> To optimize indexes to lookup
        for (int i = 0; i < this.db.get(0).size(); i++) {
            // Split monthCol into month and year
            String dateString = this.db.get(0).get(i);
            // Split date string
            String[] parts = dateString.split("-");
            // Extract month and year, converted
            String year = parts[0];
            int month = Integer.parseInt(parts[1]);

            // If year and month matches with querySpecs
            if (filter_years.equals(year)){
                if (filter_months.contains(month)) {
                    posArray.add(i);
                }
            }
        }

        // Step 2: Filter by town and area requirement
        for (int i : posArray) {
            // Get town and floor area
            String town = this.db.get(1).get(i);
            double floorArea = Double.parseDouble(this.db.get(6).get(i));

            // Filter by town and area >= 80 sq meters
            if (town.equals(filter_town) && floorArea >= MIN_AREA_REQUIREMENT) {
                finalPosArray.add(i);
            }
        }

        return finalPosArray;
    }

    public ArrayList<Integer> queryDBIndex(){
        // Init pos array
        ArrayList<Integer> posArray = new ArrayList<Integer>();
        ArrayList<Integer> finalPosArray = new ArrayList<Integer>();

        int townIndex = mapTownToIndex(this.filter_town);
        int yearDigit = Integer.parseInt(this.filter_years) % 10;
        for (Integer filterMonth : this.filter_months) {
            posArray.addAll(this.mki.queryIndex(yearDigit, filterMonth, townIndex));
        }

        // Apply area filter
        for (int i : posArray) {
            double floorArea = Double.parseDouble(this.db.get(6).get(i));
            if (floorArea >= MIN_AREA_REQUIREMENT) {
                finalPosArray.add(i);
            }
        }

        return finalPosArray;
    }

    public ArrayList<Integer> queryCompressedDB(){
        // Init final pos array
        ArrayList<Integer> tmpPosArray = new ArrayList<Integer>();
        ArrayList<Integer> finalPosArray = new ArrayList<Integer>();
        
        // Query Parameters
        String startMonth = new StringBuilder(filter_years).append("-").append(this.filter_months_str.get(0)).toString();
        String endMonth = new StringBuilder(filter_years).append("-").append(this.filter_months_str.get(1)).toString();

        short compressedValueStart = getCompressValue(filter_town, startMonth);
        short compressedValueEnd = getCompressValue(filter_town, endMonth);

        int startIndex, endIndex;
        int[] indexArr = zoneMap.getZone(compressedValueStart, compressedValueEnd);

        startIndex = findValueStartPositionInCompressedDb(indexArr[0], indexArr[1], compressedValueStart);
        endIndex = findValueEndPositionInCompressedDb(indexArr[2], indexArr[3], compressedValueEnd);

        // Pass 1
        for (int i = startIndex; i <= endIndex; i++) {
            tmpPosArray.add(i);
        }
        
        // Apply area filter
        for (int i : tmpPosArray) {
            double floorArea = Double.parseDouble(this.db.get(6).get(i));
            if (floorArea >= MIN_AREA_REQUIREMENT) {
                finalPosArray.add(i);
            }
        }

        return finalPosArray;
    }

    public int findValueStartPositionInCompressedDb(int startFind, int endFind, int searchValue) {
        // Implementation unchanged
        int middle, midVal, findAreaSize;
        int endBackup = endFind;

        // searchValue is at start of list
        midVal = this.compressedList.get(0);
        if (startFind == midVal) {
            return 0;
        }

        while (true) {
            findAreaSize = endFind - startFind;
            if (findAreaSize <= 50) {
                // Area small enough, do sequential search
                for (int i = startFind; i <= endBackup; i++) {
                    midVal = this.compressedList.get(i);
                    if (midVal == searchValue) {
                        return i;
                    }
                }
                return startFind;
            } else {
                middle = startFind + (findAreaSize / 2);
                midVal = this.compressedList.get(middle);
                if (midVal >= searchValue) {
                    // Too Big, Search Lower Half
                    endFind = middle;
                } else {
                    // Too Smaller, Search Upper Half
                    startFind = middle;
                }
            }
        }
    }

    public int findValueEndPositionInCompressedDb(int startFind, int endFind, int searchValue) {
        // Implementation unchanged
        int middle, midVal, findAreaSize;
        int endBackup = endFind;

        midVal = this.compressedList.get(endFind);
        if (searchValue == midVal) {
            return endFind;
        }

        while (true) {
            findAreaSize = endFind - startFind;
            if (findAreaSize <= 50) {
                // Area small enough, do sequential search
                for (int i = startFind; i <= endBackup; i++) {
                    midVal = this.compressedList.get(i);
                    if (midVal > searchValue) {
                        return i - 1;
                    }
                }
                return endFind;
            } else {
                middle = startFind + (findAreaSize / 2);
                midVal = this.compressedList.get(middle);
                if (midVal > searchValue) {
                    // Too Big, Search Lower Half
                    endFind = middle;
                } else {
                    // Too Smaller, Search Upper Half
                    startFind = middle;
                }
            }
        }
    }

    // Statistics calculation methods
    private FileWriter fw;
    public void createOutputFile (String filename) {
        filename = filename + ".csv";
        File resultsCSVFile = new File(filename);
        try {
            if (resultsCSVFile.createNewFile()) {
                // File Created Successfully
            } else {
                // File Exist
            }
            fw = new FileWriter(filename);
            fw.write("Year,Month,Town,Category,Value\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void closeOutputFile () {
        try {
            fw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void writeResults(int type, double value) {
        final String[] typeText = {
            "Minimum Price", 
            "Average Price", 
            "Standard Deviation of Price",
            "Minimum Price per Square Meter"
        };
        String writeContent = String.format("%s,%s,%f\n", resultOutputAppend, typeText[type], value);
        try {
            fw.write(writeContent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void calculateMinPrice(String method) {
        ArrayList<Integer> posArray = null;
        if (method.equals("Index")){
            posArray = this.queryDBIndex();
        } else if (method.equals("Compressed+ZoneMap+Sorted")) {
            posArray = this.queryCompressedDB();
        } else if (method.equals("Normal")){
            posArray = this.queryDB();
        }

        if (posArray == null || posArray.isEmpty()){
            this.writeResults(0, 0);
            return;
        }

        double minPrice = Double.MAX_VALUE;
        double tempVal;
        for (int i : posArray) {
            tempVal = Double.parseDouble(this.db.get(9).get(i));
            if (minPrice > tempVal) {
                minPrice = tempVal;
            }
        }
        this.writeResults(0, minPrice);
    }

    public void calculateAvgPrice(String method) {
        ArrayList<Integer> posArray = null;
        if (method.equals("Index")){
            posArray = this.queryDBIndex();
        } else if (method.equals("Compressed+ZoneMap+Sorted")) {
            posArray = this.queryCompressedDB();
        } else if (method.equals("Normal")){
            posArray = this.queryDB();
        }

        if (posArray == null || posArray.isEmpty()){
            this.writeResults(1, 0);
            return;
        }

        double totalPrice = 0;
        double tempVal;
        for (int i : posArray) {
            tempVal = Double.parseDouble(this.db.get(9).get(i));
            totalPrice += tempVal;
        }
        this.writeResults(1, (totalPrice/posArray.size()));
    }

    public void calculatePriceSD(String method) {
        ArrayList<Integer> posArray = null;
        if (method.equals("Index")) {
            posArray = this.queryDBIndex();
        } else if (method.equals("Compressed+ZoneMap+Sorted")) {
            posArray = this.queryCompressedDB();
        } else if (method.equals("Normal")) {
            posArray = this.queryDB();
        }

        if (posArray == null || posArray.isEmpty()) {
            this.writeResults(2, 0);
            return;
        }

        // Calculate mean
        double totalPrice = 0;
        double[] prices = new double[posArray.size()];
        int index = 0;
        
        for (int i : posArray) {
            double price = Double.parseDouble(this.db.get(9).get(i));
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
        
        this.writeResults(2, stdDev);
    }

    public void calculateMinPricePerSqm(String method) {
        ArrayList<Integer> posArray = null;
        if (method.equals("Index")) {
            posArray = this.queryDBIndex();
        } else if (method.equals("Compressed+ZoneMap+Sorted")) {
            posArray = this.queryCompressedDB();
        } else if (method.equals("Normal")) {
            posArray = this.queryDB();
        }

        if (posArray == null || posArray.isEmpty()) {
            this.writeResults(3, 0);
            return;
        }

        double minPricePerSqm = Double.MAX_VALUE;
        
        for (int i : posArray) {
            double price = Double.parseDouble(this.db.get(9).get(i));
            double area = Double.parseDouble(this.db.get(6).get(i));
            double pricePerSqm = price / area;
            
            if (pricePerSqm < minPricePerSqm) {
                minPricePerSqm = pricePerSqm;
            }
        }
        
        this.writeResults(3, minPricePerSqm);
    }

    public void calculateDefault() {
        this.calculateMinPrice("Normal");
        this.calculateAvgPrice("Normal");
        this.calculatePriceSD("Normal");
        this.calculateMinPricePerSqm("Normal");
    }
}