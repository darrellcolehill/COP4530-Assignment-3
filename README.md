# COP4530-Assignment-3


## Problem 1: The Birthday Presents Party (50 points)

The Minotaur’s birthday party was a success. The Minotaur received a lot of presents from his guests. The next day he decided to sort all of his presents and start writing “Thank you” cards. Every present had a tag with a unique number that was associated with the guest who gave it. Initially all of the presents were thrown into a large bag with no particular order. The Minotaur wanted to take the presents from this unordered bag and create a chain of presents hooked to each other with special links (similar to storing elements in a linked-list). In this chain (linked-list) all of the presents had to be ordered according to their tag numbers in increasing order. The Minotaur asked 4 of his servants to help him with creating the chain of presents and writing the cards to his guests. Each servant would do one of three actions in no particular order: 1. Take a present from the unordered bag and add it to the chain in the correct location by hooking it to the predecessor’s link. The servant also had to make sure that the newly added present is also linked with the next present in the chain. 2. Write a “Thank you” card to a guest and remove the present from the chain. To do so, a servant had to unlink the gift from its predecessor and make sure to connect the predecessor’s link with the next gift in the chain. 3. Per the Minotaur’s request, check whether a gift with a particular tag was present in the chain or not; without adding or removing a new gift, a servant would scan through the chain and check whether a gift with a particular tag is already added to the ordered chain of gifts or not. As the Minotaur was impatient to get this task done quickly, he instructed his servants not to wait until all of the presents from the unordered bag are placed in the chain of linked and ordered presents. Instead, every servant was asked to alternate adding gifts to the ordered chain and writing “Thank you” cards. The servants were asked not to stop or even take a break until the task of writing cards to all of the Minotaur’s guests was complete. After spending an entire day on this task the bag of unordered presents and the chain of ordered presents were both finally empty! Unfortunately, the servants realized at the end of the day that they had more presents than “Thank you” notes. What could have gone wrong? Can we help the Minotaur and his servants improve their strategy for writing “Thank you” notes? Design and implement a concurrent linked-list that can help the Minotaur’s 4 servants with this task. In your test, simulate this concurrent “Thank you” card writing scenario by dedicating 1 thread per servant and assuming that the Minotaur received 500,000 presents from his guests.

### How to run the solution program

#### Compile
``` javac Problem1.java ```
#### Run
``` java Problem1 ```


### Correctness

The correctness of this implementation is fairly straight forward since it utilizes coarse grained locking. Essentially, the solution was formed by generating the methods to make a sorted linked list, then enforcing safety by forcing each thread to acquire a lock before making changes to the linked list. This allows each thread to make changes to the linked list without having to worry if the change will result in a segmentation fault for another thread by removing a node it was trying ot access. 

The error that the servents most likely made when they first wrote thankyou cards was not enforcing strict rules when who makes changes to the list.

This coarse grained locking (while clunky) was implemented becuase the problem statement specified that correctness was of the most importance, since we wanted to ensure that the correct number of thank-you cards where written.

To make sure that the correct number of cards are written, whenver a gift (node) is removed, the message is displayed to the screen and a counter keeping track of all the cards written is incremented. 


### Efficiency

As previously stated, this is not the most efficient way of solving the problem, as it requires a lot of overhead for maintaining who has control of the lock. Essentially, the efficiency of this solution is comparable (and most likely slower for large datasets) to just using a single servent (thread) since using a single thread would not require complex lock management systems. 


### Experimental evaluation

Experimental evaluation was conducted by changing the number of gifts and by examining the output to ensure that each thank you card was written and that none where left out. 



## Problem 2: Atmospheric Temperature Reading Module (50 points)

You are tasked with the design of the module responsible for measuring the atmospheric temperature of the next generation Mars Rover, equipped with a multicore CPU and 8 temperature sensors. The sensors are responsible for collecting temperature readings at regular intervals and storing them in shared memory space. The atmospheric temperature module has to compile a report at the end of every hour, comprising the top 5 highest temperatures recorded for that hour, the top 5 lowest temperatures recorded for that hour, and the 10-minute interval of time when the largest temperature difference was observed. The data storage and retrieval of the shared memory region must be carefully handled, as we do not want to delay a sensor and miss the interval of time when it is supposed to conduct temperature reading. Design and implement a solution using 8 threads that will offer a solution for this task. Assume that the temperature readings are taken every 1 minute. In your solution, simulate the operation of the temperature reading sensor by generating a random number from -100F to 70F at every reading. In your report, discuss the efficiency, correctness, and progress guarantee of your program.


### How to run the solution program

#### Compile
``` javac Problem2.java ```
#### Run
``` java Problem2 ```

#### Stop
``` ctrl + c ```

### Correctness

The correctness of this problem depends on decreasing the possibiility of a dead-lock occuring and to limit the time a thread spends waiting for the shared resource as possible. To do this, I implemented a list of AtomicIntegerArrays, where each array in the list represents the readings for 1 hour by all 8 threads. 

The program begins by adding an initialized AtomicIntegerArray to the buffers list at index 0, then we start each of the eight threads. Each thread performs the following:

1. Generates a random temperature reading
2. Indexes the proper AtomicIntegerArray for the current hour, and continually performs compareAndSet until an open spot in the array is found and inserts the new temperature reading
3. sleeps for one minute

At this point, if any temperature reading took longer than 1 minute, then there would be errors in the data that would cause it to lose its time-sequence, however, since we are simply generating 8 random integers, this is not a concern.

Once all the threads (representing the sensors) have started, the main thread sleeps for 1 hour, then once the hour has passed, it performs the following:

1. Temporarily store the value of the buffers index tracker
2. Creates a AtomicIntegerArray and adds it to the buffer
3. Increments the variable storing the most recent buffer that the sensors should use
4. Creates a copy of the data stored at the buffer in question (not the newly created one, as it is for the next hour)
5. Creates a sorted copy of the copy of the buffer (sorry, that was a mouthful)
6. Uses the sorted copy to find the five smallest/largest values for the hour
7. Uses the unsorted value to find the 10 minute segment with the largest difference in temperature. 
8. Displays the results 

Steps 1 through 3 have 1 minute to complete before it results in incorrect storing of data. This is because we need to create a new buffer for the new hour and specify what index in the buffers list it is stored, before the next minute of that new hour begins, so the sensors store their readings in the correct location. Since this process is simply creating one array and incrementing a counter variable with no contention from another thread, we can assume that this process will happen in less than a minute. 

Steps 1-8, must occur within 1 hour before the results cause errors. However, this is not a concern, since the bottle neck with this is the sorting of the copy. Since we are keeping the buffer size small, by making the individual buffers arrays only large enough to store the readings for one hour, sorting is not a concern. Should a large amount of sensors be added, then it could grow to be a concern, and more efficient methods of sorting should be used. 

### Progress guarantee 
By utilizing AtomicIntegerArrays, we can ensure thread progress since no locking is required. We simple check if a spot contains the initial value (Integer.MAX_VALUE), and if so, then we set the new reading at that location. In the worst case, it would perform 8 compares before finding a location to set the new value. 


### Efficiency
The efficiency of this solution is greatly determined by the sorting algorithm used to sort the copy of the previous hour's readings, and the number of compares must be performed before a thread can set their reading into the specified AtomicIntegerArray. The max number of compares will be the number of sensors in use. The runtime for the sorting is determined by (number of sensors in use)*(readings per hour), both of there are relatively low (since we are only using 8 sensors and 60 readings per hour). 

### Experimental evaluation
Evaluation was conducted by greatly decreasing the delay between sensor readings and reports. Specifically, the solution was tested by setting the delay between minutes to .5 seconds, and the delay between reports to 1 minute. The general idea was if the algorithm worked with an even smaller time-frame, then it is logical that it will work with a larger time-frame, since the number of operations at each delay remains constant (i.e. at the end of every sensor reading delay, the program gets/stores 8 readings, and at the end of each report delay, a report is generated)
