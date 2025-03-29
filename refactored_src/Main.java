import java.util.ArrayList;

/**
 * Main application class for property data analysis
 */
public class Main {
    
    public static void main(String[] args) {
        PropertyDataStore db = new PropertyDataStore();
        
        // Process multiple matriculation numbers for analysis
        // You can replace these with your actual group members' matriculation numbers
        String[] matriculationNumbers = new String[]{"U2120814C", "U2121505H", "U2121165H"};
        
        // Process each matriculation number
        for (String matNumber : matriculationNumbers) {
            System.out.println("\nQuery for: " + matNumber);
            
            // Extract identifier code from matriculation number (last 3 digits before final character)
            String lastThreeChars = matNumber.substring(matNumber.length() - 4, matNumber.length() - 1);
            db.initQuerySpec(Integer.valueOf(lastThreeChars));
            
            // Prepare data structures for efficient querying
            db.compressTownDate();
            db.sortByCompressedData();
            db.buildIndex();
            db.createZoneMap();
            
            // Create output file and calculate statistics
            db.createOutputFile("ScanResult_" + matNumber);
            db.calculateDefault();
            db.closeOutputFile();
            
            // Benchmark different query methods
            ArrayList<Integer> posArray = null;
            long startTime, stopTime;
            String[] methodArr = new String[]{"Normal", "Index", "Compressed+ZoneMap+Sorted"};
            
            System.out.println("\nPerformance comparison:");
            for (int i = 0; i < methodArr.length; i++) {
                startTime = System.nanoTime();
                
                switch(i) {
                    case 0:
                        posArray = db.queryDB();
                        break;
                    case 1:
                        posArray = db.queryDBIndex();
                        break;
                    case 2:
                        posArray = db.queryCompressedDB();
                        break;
                }
                
                stopTime = System.nanoTime();
                
                // Report performance statistics
                System.out.println("Time taken for " + methodArr[i] + " method: " + (stopTime - startTime) + " ns");
                
                try {
                    System.out.println("Returned PosArray Size:" + posArray.size());
                } catch (Exception e) {
                    System.out.println("No records fit criteria");
                }
            }
        }
    }
}