import java.util.ArrayList;


public class Main {

    public static void main(String[] args) {
        Database db = new Database();
        // Change array to include all matriculation numbers to be tested
        // You can replace these with your actual group members' matriculation numbers
        String[] mat_numbers = new String[]{"U2120814C", "U2121505H", "U2121165H"};
        
        for (String mat_number : mat_numbers) {
            System.out.println("\nQuery for: " + mat_number);
            // Extract the last 3 digits for query specification
            // db.initQuerySpec(Integer.valueOf(mat_number.substring(mat_number.length() - 3)));
            String lastThreeChars = mat_number.substring(mat_number.length() - 4, mat_number.length() - 1);
            db.initQuerySpec(Integer.valueOf(lastThreeChars));

            // Setup and optimize the database
            db.compressTownDate();
            db.sortByCompressedData();
            db.buildIndex();
            db.createZoneMap();
            
            // Create output file with required format
            db.createOutputFile("ScanResult_" + mat_number);
            db.calculateDefault();
            db.closeOutputFile();

            // Compare performance of different query methods
            ArrayList<Integer> posArray = null;
            long startTime, stopTime;
            String[] methodArr = new String[]{"Normal", "Index", "Compressed+ZoneMap+Sorted"};
            
            for (int i=0; i < methodArr.length; i++) {
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
                
                // Print time taken for each method
                System.out.println("Time taken for " + methodArr[i] + " method: " + (stopTime - startTime) + " ns");
                try {
                    System.out.println("Returned PosArray Size:" + posArray.size());
                } catch (Exception NullPointerException) {
                    System.out.println("No records fit criteria");
                }
            }
        }
    }
}