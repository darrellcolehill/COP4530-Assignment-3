import java.util.Random;
import java.util.concurrent.locks.*;


class OrderedLinkedList {
    
    private Node head;
    private Lock lock = new ReentrantLock();


    private static class Node {
        int data;
        Node next;
        
        Node(int data) {
            this.data = data;
            this.next = null;
        }
    }
    

    public void insert(int data) {
        lock.lock();
        try{
            Node newNode = new Node(data);
        
            if (head == null || data < head.data) {
                newNode.next = head;
                head = newNode;
                return;
            }
            
            Node current = head;
            while (current.next != null && current.next.data < data) {
                current = current.next;
            }
            newNode.next = current.next;
            current.next = newNode;
        } finally {
            lock.unlock();
        }
    }
    

    public void delete(int data) {
        lock.lock();
        try {
            if (head == null) {
                return;
            }
            
            if (head.data == data) {
                head = head.next;
                return;
            }
            
            Node current = head;
            while (current.next != null && current.next.data != data) {
                current = current.next;
            }
            
            if (current.next != null) {
                current.next = current.next.next;
            }
        } finally {
            lock.unlock();
        }

    }
    

    public boolean search(int data) {
        lock.lock();
        try {
            Node current = head;
            while (current != null && current.data <= data) {
                if (current.data == data) {
                    System.out.println("Found gift with tag " + data);
                    return true;
                }
                current = current.next;
            }
            System.out.println("Did not find gift with tag " + data);
            return false;
        } finally{
            lock.unlock();
        }

    }


    public int removeHead() {
        lock.lock();
        try {
            if (this.head == null) {
                return -1; // empty list
            } else {
                int oldHeadData = this.head.data;
                this.head = this.head.next; // the next node becomes the new head
                return oldHeadData;
            }
        } finally {
            lock.unlock();
        }
    }
}




public class Problem1 {

    public static int numberOfGifts = 500000;
    public static int[] unorderedGifts = new int[numberOfGifts];
    public static Random random = new Random();
    public static OrderedLinkedList orderedGiftList = new OrderedLinkedList();
    public static int numberOfCards = 0;


    public static synchronized void incrementCardCounter()
    {
        numberOfCards += 1;
    }

    // Simulates the random selection of a servent to look through the list. 
    public static boolean determineIfServentShouldLook()
    {
        int randomNumber = random.nextInt(100);

        if(randomNumber == 33)
        {
            return true;
        }
        
        return false;
    }

    
    public static void main(String[] args)
    {
        // initialized the unordered pile of gifts
        // Initialize array with values from 1 to 500000
        for (int i = 0; i < unorderedGifts.length; i++) {
            unorderedGifts[i] = i + 1;
        }
        
        // Shuffle array using Fisher-Yates algorithm
        for (int i = unorderedGifts.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = unorderedGifts[i];
            unorderedGifts[i] = unorderedGifts[j];
            unorderedGifts[j] = temp;
        }


        Thread t1 = new Thread(() -> {
            for (int i = 0; i <= 124999; i++) {
                orderedGiftList.insert(unorderedGifts[(i)]);
                int result = orderedGiftList.removeHead();
                if(result != -1)
                {
                    incrementCardCounter();
                    System.out.println("Thannk for gift " + result);
                }

                if(determineIfServentShouldLook())
                {
                    orderedGiftList.search(random.nextInt(numberOfGifts));
                }

            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 125000; i <= 249999; i++) {
                orderedGiftList.insert(unorderedGifts[(i)]);                
                int result = orderedGiftList.removeHead();
                if(result != -1)
                {
                    incrementCardCounter();
                    System.out.println("Thank you for gift " + result);
                }

                if(determineIfServentShouldLook())
                {
                    orderedGiftList.search(random.nextInt(numberOfGifts));
                }
            }
        });

        Thread t3 = new Thread(() -> {
            for (int i = 250000; i <= 374999; i++) {
                orderedGiftList.insert(unorderedGifts[(i)]);                
                int result = orderedGiftList.removeHead();

                if(result != -1)
                {
                    incrementCardCounter();
                    System.out.println("Thank you for gift " + result);
                }

                if(determineIfServentShouldLook())
                {
                    orderedGiftList.search(random.nextInt(numberOfGifts));
                }
            }
        });

        Thread t4 = new Thread(() -> {
            for (int i = 375000; i < 500000; i++) {
                orderedGiftList.insert(unorderedGifts[(i)]);                
                int result = orderedGiftList.removeHead();
                if(result != -1)
                {
                    incrementCardCounter();
                    System.out.println("Thank you for gift " + result);
                }

                if(determineIfServentShouldLook())
                {
                    orderedGiftList.search(random.nextInt(numberOfGifts));
                }
            }
        });


        t1.start();
        t2.start();
        t3.start();
        t4.start();
        try {
            t1.join();
            t2.join();
            t3.join();
            t4.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        

        System.out.println("Number of cards sent = " + numberOfCards);
    }


}
