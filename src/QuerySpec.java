import java.util.ArrayList;
import java.util.Objects;

public class QuerySpec {
    public String targetLocation;
    public ArrayList<Integer> targetMonths;
    public ArrayList<String> targetMonthsFormatted;
    public String targetYear;
    public String outputPrefix;
    public ArrayList<String> targetYears;

    public QuerySpec(String targetLocation, ArrayList<Integer> targetMonths,
                     ArrayList<String> targetMonthsFormatted, String targetYear) {
        this.targetLocation = targetLocation;
        this.targetMonths = targetMonths;
        this.targetMonthsFormatted = targetMonthsFormatted;
        this.targetYear = targetYear;
        this.outputPrefix = String.format("%s,%s,%s", targetYear, targetMonthsFormatted.get(0), targetLocation);
    }

    // Optional: Override equals and hashCode so you can use this as a key in HashMap
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QuerySpec)) return false;
        QuerySpec that = (QuerySpec) o;
        return Objects.equals(targetLocation, that.targetLocation)
                && Objects.equals(targetMonths, that.targetMonths)
                && Objects.equals(targetYear, that.targetYear);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetLocation, targetMonths, targetYear);
    }
}
