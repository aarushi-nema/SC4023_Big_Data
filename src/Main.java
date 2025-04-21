import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * Main application class for property data analysis
 * Combines individual processing with shared scan functionality
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("======================================================");
        System.out.println("            PROPERTY DATA ANALYSIS SYSTEM             ");
        System.out.println("======================================================");
        
        PropertyDataStore db = new PropertyDataStore();
        
        // Process multiple matriculation numbers for analysis
        String[] matriculationNumbers = new String[]{"U2120814C", "U2120304J", "U2122495G"};
    
        System.out.println("\n------------------------------------------------------");
        System.out.println("INITIALIZATION");
        System.out.println("------------------------------------------------------");
        
        // Extract identifier codes from all matriculation numbers
        ArrayList<Integer> all_lastThreeChars = new ArrayList<>();
        for (String matNumber : matriculationNumbers) {
            Integer lastThreeChars = Integer.valueOf(matNumber.substring(matNumber.length() - 4, matNumber.length() - 1));
            all_lastThreeChars.add(lastThreeChars);
            System.out.println("Matriculation number: " + matNumber + " (Code: " + lastThreeChars + ")");
        }
    
        // Prepare data structures for efficient querying
        System.out.println("Preparing data structures...");
        System.out.println("→ Compressing town and date data");
        db.compressTownDate();
        System.out.println("→ Sorting by compressed data");
        db.sortByCompressedData();
        System.out.println("→ Building index");
        db.buildIndex();
        System.out.println("→ Creating zone map");
        db.createZoneMap();
        
        // Prepare shared scan query specifications
        ArrayList<QuerySpec> fullQuerySpec = db.sharedScanQuerySpec(all_lastThreeChars);

        //Doing Shared Scan
        long startTime = System.nanoTime();
        Map<QuerySpec, ArrayList<Integer>> AllposArray= db.sharedScanQueryDB(fullQuerySpec);
        long stopTime = System.nanoTime();
        long timeforSharedScan = stopTime - startTime;
        
        // Process individual methods for each matriculation number
        for (String matNumber : matriculationNumbers) {
            System.out.println("\n------------------------------------------------------");
            System.out.println("PROCESSING: " + matNumber);
            System.out.println("------------------------------------------------------");
            
            // Extract identifier code from matriculation number
            String lastThreeChars = matNumber.substring(matNumber.length() - 4, matNumber.length() - 1);
            int code = Integer.valueOf(lastThreeChars);
            
            // Initialize query specification for this matriculation number
            System.out.println("Initializing query specification (Code: " + code + ")");
            db.initQuerySpec(code);
            
            // Set current output file
            // System.out.println("Setting output file: ScanResult_" + matNumber);
            db.createOutputFile("ScanResult_" + matNumber);
            
            // Calculate default statistics
            // System.out.println("Calculating default statistics...");
            db.calculateDefault();
            
            // Execute methods in a 2x2 grid
            System.out.println("\n-----------------------------------------  -----------------------------------------");
            System.out.println("-> NORMAL METHOD                          -> INDEX METHOD");
            
            // Normal method
            long startTimeNormal = System.nanoTime();
            ArrayList<Integer> posArrayNormal = db.queryDB();
            long endTimeNormal = System.nanoTime();
            
            // Index method
            long startTimeIndex = System.nanoTime();
            ArrayList<Integer> posArrayIndex = db.queryDBIndex();
            long endTimeIndex = System.nanoTime();
            
            try {
                System.out.printf("   Records found: %-24d   Records found: %d%n", 
                        posArrayNormal.size(), posArrayIndex.size());
                System.out.printf("   Time taken: %-26d   Time taken: %d ns%n", 
                        (endTimeNormal - startTimeNormal), (endTimeIndex - startTimeIndex));
            } catch (Exception e) {
                System.out.println("   Error displaying results: " + e.getMessage());
            }
            
            System.out.println("\n----------------------------------------- ");
            System.out.println("-> COMPRESSED+ZONEMAP+SORTED METHOD       -> SHARED SCAN METHOD");
            
            // Compressed method
            long startTimeComp = System.nanoTime();
            ArrayList<Integer> posArrayComp = db.queryCompressedDB();
            long endTimeComp = System.nanoTime();
            
            try {
                // Find this matric number's corresponding query spec
                int index = 0;
                for (int i = 0; i < matriculationNumbers.length; i++) {
                    if (matriculationNumbers[i].equals(matNumber)) {
                        index = i;
                        break;
                    }
                }
                
                System.out.printf("   Records found: %-24d  Records found: %d%n", 
                        posArrayComp.size(), AllposArray.get(fullQuerySpec.get(index)).size());
                System.out.printf("   Time taken: %-26d     Time taken: %d ns%n", 
                        (endTimeComp - startTimeComp), (timeforSharedScan));
            } catch (Exception e) {
                System.out.println("   Error displaying results: " + e.getMessage());
            }
            
            // Close the output file
            System.out.println("\nClosing output file...");
            db.closeOutputFile();
        }
        
        System.out.println("\n======================================================");
        System.out.println("       All processing completed successfully");
        System.out.println("======================================================");
    }
    
}