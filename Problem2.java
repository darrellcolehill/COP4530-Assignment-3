import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.text.SimpleDateFormat;  
import java.util.Date;  


public class Problem2 {

    // buffer size for 1 hour of readings (60 readings per for 1 thread) 60 * 8 threads = 480
    private final static int bufferSize = 480; // size of the buffer
    private static int bufferIndexTracker = 0;
    private final static List<AtomicIntegerArray> buffersList = new ArrayList<AtomicIntegerArray>();
    

    public static synchronized void incrementBufferIndexTracker()
    {
        bufferIndexTracker = bufferIndexTracker + 1;
    }

    public static AtomicIntegerArray generateBuffer()
    {
        AtomicIntegerArray newBuffer = new AtomicIntegerArray(bufferSize);
        for(int i = 0; i < bufferSize; i++)
        {
            // initializes the buffer to a value that would not be read by the sensor
            newBuffer.set(i, Integer.MAX_VALUE); 
        }
        return newBuffer;
    }


    static class Sensor extends Thread {

        private final Random randomNumberGenerator; // random number generator
        private int sensorID;
        private int delayInterval;

        public Sensor(int delayInterval, int sensorID)
        {
            this.randomNumberGenerator = new Random();
            this.delayInterval = delayInterval;
            this.sensorID = sensorID;
        }

        public int getTemperatureReading()
        {
            // generate random temperature between -100F and 70F
            return randomNumberGenerator.nextInt(171) - 100;
        }

        public void run()
        {
            int index = 0;
            while (true) {
                int temperature = getTemperatureReading();
                int nextIndex = (index + 1) % bufferSize;
                while (!buffersList.get(bufferIndexTracker).compareAndSet(nextIndex, Integer.MAX_VALUE, temperature)) { // use atomic operations to store temperature in buffer
                    nextIndex = (nextIndex + 1) % bufferSize; // if the buffer is full, move to the next index
                }
                System.out.println("s" + this.sensorID + ": " + temperature);
                index = nextIndex;
                try {
                    Thread.sleep(delayInterval); // wait for 1 minute before taking the next reading
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }



    public static void main(String[] args) {

        int sensorReadingDelay = 60000; // 60000 = 1 minute
        int reportGenerationDelay = 3600000; // 3600000 = 1 hour

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");  

        // initializes the buffers list with a list for the first hour
        buffersList.add(generateBuffer());

        // Makes and starts all the threads
        Thread[] sensors = new Thread[8];
        for (int i = 0; i < 8; i++) {
            Sensor sensor = new Sensor(sensorReadingDelay, i);
            sensors[i] = new Thread(sensor);
            sensors[i].setDaemon(true); // ensure that the thread is terminated when the main thread is stopped 
            sensors[i].start();
        }


        // Main thread generates report for every hour
        while (true) {
            try {
                Thread.sleep(reportGenerationDelay); // wait for 1 hour
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            int currentHoursBuffer = bufferIndexTracker;


            // =============== This the part of code that results in the most contention ===============
            // creates new AtomicIntegerArray for buffer and increment buffer index tracker. 
            buffersList.add(generateBuffer());
            incrementBufferIndexTracker();
            // =========================================================================================


            int[] readings = new int[bufferSize];
            int[] sortedReadings = new int[bufferSize];
            for (int i = 0; i < bufferSize; i++) {
                readings[i] = buffersList.get(currentHoursBuffer).get(i);
                sortedReadings[i] = readings[i];
            }

            // sort readings in a new structure. Keeps the original in readings so that we can maintain the time-sequence
            Arrays.sort(sortedReadings); 


            // Gets the top 5 largest for the hour, by utilizing the sorted property of the buffer
            int highestIndex = bufferSize - 1;
            // get the first index that is not Integer.max and make that the highest index
            for(int i = 0; i < bufferSize; i++)
            {
                if(sortedReadings[bufferSize - i - 1] != Integer.MAX_VALUE)
                {
                    highestIndex = bufferSize - i - 1;
                    break;
                }
            }


            int[] highest = new int[5];
            for (int i = 0; i < 5; i++) {
                highest[i] = sortedReadings[highestIndex--];
            }

            // Gets the top 5 lowest for the hour, by utilizing the sorted property of the buffer
            int lowestIndex = 0;
            int[] lowest = new int[5];
            for (int i = 0; i < 5; i++) {
                lowest[i] = sortedReadings[lowestIndex++];
            }

            /*
             * sensor delay = 1 minute
             * report generation delay = 1 hour
             * 
             * in one hour, we will have 60 readings from each sensor
             * in total, 480.
             * 
             * each segment of 8 readings represents 1 minute worth of reads across all sensors
             * each segment of 80 readings represents 10 minutes work of reads across all sensors
             * 
             * the min and max in each segment of 80 readings is largest difference in temperature in that 10 minute segment
             */


            int[][] ten_minute_segment_differences = new int[6][3];
            int segmentIdxMaxDifference = 0;
            int maxDifference = 0;

            for (int i = 0; i < 6; i++) 
            {
                int startIndex = i * 80;
                int endIndex = startIndex + 79;
                
                int min = readings[startIndex];
                int max = readings[startIndex];
                
                for (int j = startIndex + 1; j <= endIndex; j++) {
                    if (readings[j] < min) {
                        min = readings[j];
                    }
                    if (readings[j] > max) {
                        max = readings[j];
                    }
                }
                
                ten_minute_segment_differences[i][0] = min;
                ten_minute_segment_differences[i][1] = max;
                ten_minute_segment_differences[i][2] = max - min;

                if(ten_minute_segment_differences[i][2] > maxDifference)
                {
                    segmentIdxMaxDifference = i;
                }
                
            }

            System.out.println("\n\n===============================================");
            Date date = new Date();  
            System.out.println("Current time: " + date);
            System.out.println("Report for hour");
            System.out.println("Top 5 highest temperatures: " + Arrays.toString(highest));
            System.out.println("Top 5 lowest temperatures: " + Arrays.toString(lowest));
            int segmentEnd = (segmentIdxMaxDifference + 1) * 10;
            int segmentStart = segmentEnd - 10;
            System.out.println("Largest temperature difference bewteen minute " + segmentStart + " and " + segmentEnd + " of current hour" );
            System.out.println("Largest temperature difference: " + maxDifference + "F");
            System.out.println("===============================================\n\n");

        }
    }
}
