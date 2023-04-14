import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicIntegerArray;



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

        int sensorReadingDelay = 30000; // 60000; // 60000 = 1 minute?
        int reportGenerationDelay = 60000; // 3600000 = 1 hour?

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
            for (int i = 0; i < bufferSize; i++) {
                readings[i] = buffersList.get(currentHoursBuffer).get(i);
            }

            // sort readings
            Arrays.sort(readings); 


            // Gets the top 5 largest for the hour, by utilizing the sorted property of the buffer
            int highestIndex = bufferSize - 1;
            // get the first index that is not Integer.max and make that the highest index
            for(int i = 0; i < bufferSize; i++)
            {
                if(readings[bufferSize - i - 1] != Integer.MAX_VALUE)
                {
                    highestIndex = bufferSize - i - 1;
                    break;
                }
            }


            int[] highest = new int[5];
            for (int i = 0; i < 5; i++) {
                highest[i] = readings[highestIndex--];
            }

            // Gets the top 5 lowest for the hour, by utilizing the sorted property of the buffer
            int lowestIndex = 0;
            int[] lowest = new int[5];
            for (int i = 0; i < 5; i++) {
                lowest[i] = readings[lowestIndex++];
            }


            // cut the reading array into segments for each 10 minutes
            // for each 10 minites, calculate the difference between the min and max
            // find the segment with the largest difference
            int maxDifference = 0;
            int maxDifferenceIndex = 0;
            // for (int i = 0; i < bufferSize - 10; i++) {
            //     int difference = readings[i + 10] - readings[i];
            //     if (difference > maxDifference) {
            //         maxDifference = difference;
            //         maxDifferenceIndex = i;
            //     }
            // }

            int maxDifferenceStart = maxDifferenceIndex + 1;
            int maxDifferenceEnd = maxDifferenceIndex + 10;

            System.out.println("Report for hour: " + System.currentTimeMillis() / 3600000);
            System.out.println("Top 5 highest temperatures: " + Arrays.toString(highest));
            System.out.println("Top 5 lowest temperatures: " + Arrays.toString(lowest));
            // System.out.println("Largest temperature difference: " + maxDifference + "F between " + maxDifference);
        }
    }
}
