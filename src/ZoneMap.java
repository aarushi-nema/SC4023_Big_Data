import java.util.ArrayList;

public class ZoneMap {
    private ArrayList<Short> zoneLargest;
    private ArrayList<Integer> zoneIndex;

    public ZoneMap() {
        zoneLargest = new ArrayList<Short>();
        zoneIndex = new ArrayList<>();
    }

    public void addZone(short largestVal, int lastIndex) {
        zoneLargest.add(largestVal);
        zoneIndex.add(lastIndex);
    }

    public void printZones() {
        System.out.println("Zone Largest Arr:");
        for (int i=0; i < zoneLargest.size();i++) {
            System.out.printf("%d,", zoneLargest.get(i));
        }
        System.out.printf("\n");
    }

    public int[] getZone(short start, short end) {
        int[] rtnArr = new int[4];
        rtnArr[0] = -1; //Start Value:Start Finding Range
        rtnArr[1] = -1; //Start Value:End Finding Range
        rtnArr[2] = -1; //End Value:Start Finding Range
        rtnArr[3] = -1; //End Value:End Finding Range
        int i;

        // Get Start Index
        int startIndex = findZone(0, this.zoneLargest.size()-1, start);
        int endIndex = findZone(startIndex, this.zoneLargest.size()-1, end);
        if (startIndex > 0) {
            rtnArr[0] = this.zoneIndex.get(startIndex-1) + 1;
        }else {
            rtnArr[0] = 0;
        }
        rtnArr[1] = this.zoneIndex.get(startIndex);

        // Get End Index
        if (endIndex > 0) {
            if (endIndex > 0) {
                // Get Current Zone's Starting point
                rtnArr[2] = this.zoneIndex.get(endIndex-1) +1;
            }else {
                rtnArr[2] = 0;
            }
        }
        rtnArr[3] = this.zoneIndex.get(endIndex);

        return rtnArr;
    }

    private int findZone(int start, int end, short value) {
        int startFind = start;
        int endFind = end;
        int findAreaSize, mid;
        while (true) {
            findAreaSize = endFind-startFind;
            if (findAreaSize <= 50) {
                for (int i=startFind; i <= end;i++) {
                    if (value <= this.zoneLargest.get(i)) {
                        return i;
                    }
                }
                return start;
            }else {
                mid = startFind + (findAreaSize/2);
                if (value < this.zoneIndex.get(mid)) {
                    if (value > this.zoneIndex.get(mid-1)) {
                        return mid;
                    }else {
                        // Search bottom half
                        endFind = mid;
                    }
                }else {
                    // Search top half
                    startFind = mid;
                }
            }
        }
    }
}
