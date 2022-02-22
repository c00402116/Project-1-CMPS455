package com.company;

import java.util.Scanner;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

public class Main {

    public static class Reader extends Thread {
        public static String threadName;
        public static int readcount;
        public static int N;
        public static Semaphore mutex;
        public static Semaphore area;
        public static Semaphore barrierR;
        public static Semaphore barrierW;
        public static Semaphore numR;
        public static Semaphore numW;

        public Reader(String threadName, int readcount, int N, Semaphore mutex, Semaphore area, Semaphore barrierR, Semaphore barrierW, Semaphore numR, Semaphore numW) {
            super(threadName);
            Reader.threadName = threadName;
            Reader.readcount = readcount;
            Reader.N = N;
            Reader.mutex = mutex;
            Reader.area = area;
            Reader.barrierR = barrierR;
            Reader.barrierW = barrierW;
            Reader.numR = numR;
            Reader.numW = numW;
        }

        @Override
        public void run() {

            //first step should be to wait() on barrierR. This will be released 1) right as all the threads are created so
            //readers will go before a writer and 2) when a writer is finished, letting readers know it is their turn.
            //also, if reacount is between 0 and N, we know that the reader can go because there are no writers.

            try {
                barrierR.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //check the mutex to make sure two readers aren't changing readcount at the same time.
            try {
                mutex.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            readcount++;
            //System.out.println("Readcount: " + readcount);
            if (readcount == 1 && barrierR.availablePermits() == 0) {
                try {
                    area.acquire();
                    for (int i = 0; i < (N-1); i++) {
                        barrierR.release();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mutex.release();

            System.out.println("Reader " + Integer.parseInt(currentThread().getName()) + " is reading.");
            for (int i = 0; i < (int)((Math.random() * 1000) + 1000); i++) {
                //beep boop. Reading I guess. maybe an audiobook would be better.
            }

            //when the reader is done we need to decrement readcount AND the number of readers remaining. (If readers remaining = 0
            //then we know we can just let all the writers go.
            try {
                numR.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                mutex.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            readcount--;

            try {
                currentThread().sleep((int)((Math.random()) * 5) + 5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("Reader " + Integer.parseInt(currentThread().getName()) + " is finished reading.");
            area.release();


            //if readcount == 0 after this reader leaves, the writer can go
            if (readcount == 0 && barrierW.availablePermits() < 1 && numR.availablePermits() > 0) {
                barrierW.release();
                //System.out.println("ddd" + Integer.parseInt(currentThread().getName()));
            } else if (readcount == 0 && barrierW.availablePermits() < 1 && numR.availablePermits() == 0) {
                for (int i = 0; i < numW.availablePermits(); i++) {
                    barrierW.release();
                }
            }
            mutex.release();
        }
    }

    public static class Writer extends Thread {
        public static String threadName;
        public static Semaphore area;
        public static Semaphore barrierR;
        public static Semaphore barrierW;
        public static Semaphore numR;
        public static Semaphore numW;

        public Writer(String threadName, Semaphore area, Semaphore barrierR, Semaphore barrierW, Semaphore numR, Semaphore numW) {
            super(threadName);
            Writer.threadName = threadName;
            Writer.area = area;
            Writer.barrierR = barrierR;
            Writer.barrierW = barrierW;
            Writer.numR = numR;
            Writer.numW = numW;
        }

        @Override
        public void run() {

            //writers should not go while readers are still in area. the readers will release barrierW when they are done
            //or else, if 0 readers are left, then all the writers can go.
            if (numR.availablePermits() == 0) {
                barrierW.release();
            }

            try {
                barrierW.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                area.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //System.out.println("www" + Integer.parseInt(currentThread().getName()));

            System.out.println("Writer " + Integer.parseInt(currentThread().getName()) + " is writing.");
            //doing some writing. I am the next JK Rowling.
            System.out.println("Writer " + Integer.parseInt(currentThread().getName()) + " is finished writing.");

            //decrement the number of remaining writers.
            try {
                numW.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            area.release();

            //now that the writer is done, readers can go.
            if (numR.availablePermits() > 0) {
                barrierR.release();
            }
        }
    }

    public static class PostOfficePerson extends Thread {
        public static String threadName;
        public static String[][] mailboxes;
        public static Semaphore[] boxSemaphores;
        public static int S;
        public static String[] messages;
        public static Semaphore messagesLeft;

        public PostOfficePerson(String threadname, String[][] mailboxes, Semaphore[] boxSemaphores, int S, String[] messages, Semaphore messagesLeft) {
            super(threadname);
            PostOfficePerson.threadName = threadname;
            PostOfficePerson.mailboxes = mailboxes;
            PostOfficePerson.boxSemaphores = boxSemaphores;
            PostOfficePerson.S = S;
            PostOfficePerson.messages = messages;
            PostOfficePerson.messagesLeft = messagesLeft;
        }

        @Override
        public void run() {

            boolean cont = true;

            while (messagesLeft.availablePermits() > 0 || cont) {
                cont = false;
                System.out.println("Person " + Integer.parseInt(currentThread().getName()) + " has entered the post office.");

                //first thing is to check this person's mailbox for messages. The mailboxes have S permits, so if .availablePermits() < S, then
                //we know there is a message to read, otherwise there is none.
                if (boxSemaphores[Integer.parseInt(currentThread().getName())].availablePermits() == S) {
                    System.out.println("Person " + Integer.parseInt(currentThread().getName()) + " has no messages.");
                }
                while (boxSemaphores[Integer.parseInt(currentThread().getName())].availablePermits() < S) {
                    boolean found = false;
                    for (int i = 0; i < S; i++) {
                        if (mailboxes[Integer.parseInt(currentThread().getName())][i] != "") {
                            found = true;
                            cont = true;
                        }
                        if (found == true) {
                            //if a message is found, read it and release the associated semaphore to free up a slot.
                            System.out.println("Person " + Integer.parseInt(currentThread().getName()) + " is reading a message... \"" + mailboxes[Integer.parseInt(currentThread().getName())][i] + "\"");
                            //System.out.println("\"" + mailboxes[Integer.parseInt(currentThread().getName())][i] + "\"");
                            mailboxes[Integer.parseInt(currentThread().getName())][i] = ""; //empty the space
                            boxSemaphores[Integer.parseInt(currentThread().getName())].release();

                            break;
                        }
                    }
                    currentThread().yield();
                }

                //now we've check the mailbox, it's time to compose a new message!
                //this is the random value of the person who will receive the message and the random index of the message text.

                if (messagesLeft.availablePermits() > 0) {
                    int recipient = ThreadLocalRandom.current().nextInt(0, boxSemaphores.length);
                    int messageIndex = ThreadLocalRandom.current().nextInt(0, messages.length);
                    while (boxSemaphores[Integer.parseInt(currentThread().getName())].availablePermits() < 1 || recipient == Integer.parseInt(currentThread().getName())) {
                        recipient = ThreadLocalRandom.current().nextInt(0, boxSemaphores.length);
                    }

                    try {
                        boxSemaphores[recipient].acquire();
                        messagesLeft.acquire();
                        System.out.println("Person " + Integer.parseInt(currentThread().getName()) + " is composing a message to person " + recipient + ". \"" + messages[messageIndex] + "\"");
                        int boxSlot = 0;
                        //make sure we insert the message into an empty slot.
                        for (int i = 0; i < S; i++) {
                            if (mailboxes[recipient][i] == "") {
                                boxSlot = i;
                                break;
                            }
                        }
                        mailboxes[recipient][boxSlot] = messages[messageIndex];
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                for (String[] mailbox : mailboxes) {
                    for (String s : mailbox) {
                        if (s != "") cont = true;
                        break;
                    }
                }

                System.out.println("Person " + Integer.parseInt(currentThread().getName()) + " is leaving the post office.");
                if (cont) currentThread().yield();

            }

        }
    }

    public static class Philosopher extends Thread {
        public static String threadName;
        public static Semaphore sitBarrier;
        public static Semaphore leaveBarrier;
        public static Semaphore[] chopsticks;
        public static Semaphore meals;
        //public static int which;

        public Philosopher(String threadName, Semaphore sitBarrier, Semaphore leaveBarrier, Semaphore[] chopsticks, Semaphore meals) {
            super(threadName);
            Philosopher.threadName = threadName;
            Philosopher.sitBarrier = sitBarrier;
            Philosopher.leaveBarrier = leaveBarrier;
            Philosopher.chopsticks = chopsticks;
            Philosopher.meals = meals;
        }

        @Override
        public void run() {

            //System.out.println(currentThread().getName() + "thread");

            //all philosophers acquire() on philosopherBarrier so that they sit at the same time and then release immediately
            //so that the next thread continues.
            try {
                sitBarrier.acquire();
                System.out.println("Philosopher " + currentThread().getName() + " is sitting down to eat.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //this section should only run when there are more than 0 meals left, otherwise they should skip to leaving the table.
            while (leaveBarrier.availablePermits() < 1 && meals.availablePermits() > 0) {

                //for some reason helps to make sure they are all sitting after sitBarrier ir released.
                try {
                    currentThread().sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                while (chopsticks[Integer.parseInt(currentThread().getName())].availablePermits() == 0 || chopsticks[(Integer.parseInt(currentThread().getName()) + 1) % chopsticks.length].availablePermits() == 0) {
                    try {
                        currentThread().sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (chopsticks[Integer.parseInt(currentThread().getName())].availablePermits() > 0 && chopsticks[(Integer.parseInt(currentThread().getName()) + 1) % chopsticks.length].availablePermits() > 0) {

                    try {
                        Philosopher.chopsticks[Integer.parseInt(currentThread().getName())].acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    System.out.println("Philosopher " + currentThread().getName() + " is picking up left chopstick #" + Integer.parseInt(currentThread().getName()));

                    currentThread().yield();

                    try {
                        Philosopher.chopsticks[(Integer.parseInt(currentThread().getName()) + 1) % chopsticks.length].acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    System.out.println("Philosopher " + currentThread().getName() + " is picking up right chopstick #" + (Integer.parseInt(currentThread().getName()) + 1) % chopsticks.length);

                }

                try {
                    meals.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("Philosopher " + currentThread().getName() + " is starting to eat.  (Meals left: " + meals.availablePermits() + ") ");
                for (int i = 0; i < ((int) (Math.random() * 3) + 3); i++) {
                    //looping random number of times to give time for philosopher to eat.
                    currentThread().yield();
                }
                System.out.println("Philosopher " + currentThread().getName() + " is finished eating.");

                //philosopher has finished eating. Now it's time to put down the chopsticks.
                //left
                Philosopher.chopsticks[Integer.parseInt(currentThread().getName())].release();
                System.out.println("Philosopher " + currentThread().getName() + " is putting down left chopstick #" + Integer.parseInt(currentThread().getName()));
                //right
                Philosopher.chopsticks[(Integer.parseInt(currentThread().getName()) + 1) % chopsticks.length].release();
                System.out.println("Philosopher " + currentThread().getName() + " is putting down right chopstick #" + (Integer.parseInt(currentThread().getName()) + 1) % chopsticks.length);

                //philosopher will now enter a food coma and *think*
                System.out.println("Philosopher " + currentThread().getName() + " is starting to think.");
                for (int i = 0; i < ((int) (Math.random() * 3) + 3); i++) {
                    //looping random number of times to give time for philosopher to think.
                    currentThread().yield();
                }

                try {
                    currentThread().sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


            //we want the philosophers to stay at the table until all the meals are gone, so once meals=0 we can
            //finally release the leaveBarrier. Then they will all be able to acquire it.
            boolean allFinished = true;
            for (int i = 0; i < chopsticks.length; i++) {
                if (chopsticks[i].availablePermits() < 0 && meals.availablePermits() > 0) {
                    allFinished = false;
                }
            }

            if (meals.availablePermits() == 0 && allFinished) {
                leaveBarrier.release();

                //we have only gotten here if meals == 0 and all chopsticks are down which means we
                try {
                    leaveBarrier.acquire();
                    System.out.println("Philosopher " + currentThread().getName() + " is leaving the table.");
                    leaveBarrier.release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public static void main(String[] args) {
        if (args.length == 1) {
            switch (args[0]) {
                case "D":
                    Semaphore sitBarrier = new Semaphore(0);
                    Semaphore leaveBarrier = new Semaphore(0);
                    diningPhilosophers(sitBarrier, leaveBarrier);
                    break;
                case "P":
                    postOfficeSimulation();
                    break;
                case "R":
                    ReadersWriters();
                    break;
                default:
                    System.out.println("Input not recognized. \nUse D for Dining Philosophers\nUse P for Post Office Simulation\nUse R for Readers-Writers Problem");
                    break;
            }
        } else {
            System.out.println("Input not recognized. \nUse D for Dining Philosophers\nUse P for Post Office Simulation\nUse R for Readers-Writers Problem");
        }
    }

    public static void ReadersWriters() {

        Scanner in = new Scanner(System.in);

        int R = 0;
        int W = 0;
        int N = 0;
        int readcount = 0;

        boolean validR = false;
        boolean validW = false;
        boolean validN = false;

        //standard naming conventions: mutex is the semaphore for readers.
        //Semaphore mutex = new Semaphore(1);
        Semaphore mutex = new Semaphore(1);
        Semaphore area = new Semaphore(1);
        Semaphore barrierW = new Semaphore(0);
        Semaphore barrierR = new Semaphore(0);

        System.out.println("How many readers will there be? (Integer 5-500)");
        try {
            R = in.nextInt();
        } catch (Exception e) {
            System.out.println("Invalid input format. Exiting.");
            System.exit(0);
        }

        if (R > 4 && R < 501) {
            validR = true;
        }

        while (!validR) {
            System.out.println("R in not valid! Must be an integer from 5-500.");
            try {
                R = in.nextInt();
            } catch (Exception e) {
                System.out.println("Invalid input format. Exiting.");
                System.exit(0);
            }

            if (R > 4 && R < 501) {
                validR = true;
            }
        }

        System.out.println("How many writers will there be? (Integer 5-500)");
        try {
            W = in.nextInt();
        } catch (Exception e) {
            System.out.println("Invalid input format. Exiting.");
            System.exit(0);
        }

        if (W > 4 && W < 501) {
            validW = true;
        }

        while (!validW) {
            System.out.println("W in not valid! Must be an integer from 5-500.");
            try {
                W = in.nextInt();
            } catch (Exception e) {
                System.out.println("Invalid input format. Exiting.");
                System.exit(0);
            }

            if (W > 4 && W < 501) {
                validW = true;
            }
        }

        System.out.println("How many readers at one time are allowed? (Integer 1-500)");
        try {
            N = in.nextInt();
        } catch (Exception e) {
            System.out.println("Invalid input format. Exiting.");
            System.exit(0);
        }

        if (N > 0 && N < 501) {
            validN = true;
        }

        while (!validN) {
            System.out.println("N in not valid! Must be an integer from 2-500.");
            try {
                N = in.nextInt();
            } catch (Exception e) {
                System.out.println("Invalid input format. Exiting.");
                System.exit(0);
            }

            if (N > 0 && N < 501) {
                validN = true;
            }
        }

        Semaphore numR = new Semaphore(R);
        Semaphore numW = new Semaphore(W);

        Reader[] readers = new Reader[R];
        for (int i = 0; i < readers.length; i++) {
            String name = Integer.toString(i);
            readers[i] = new Reader(name, readcount, N, mutex ,area, barrierR, barrierW, numR, numW);
            readers[i].start();
        }

        Writer[] writers = new Writer[W];
        for (int i = 0; i < writers.length; i++) {
            String name = Integer.toString(i);
            writers[i] = new Writer(name, area, barrierR, barrierW, numR, numW);
            writers[i].start();
        }

        barrierR.release();
    }

    public static void postOfficeSimulation() {

        Scanner in = new Scanner(System.in);

        boolean validN = false;
        boolean validS = false;
        boolean validM = false;

        int N = 0;
        int S = 0;
        int M = 0;

        //get inputs before starting threads.
        System.out.println("How many people are at the post office? (Integer 2-500)");
        try {
            N = in.nextInt();
        } catch (Exception e) {
            System.out.println("Invalid input format. Exiting.");
            System.exit(0);
        }

        if (N > 1 && N < 501) {
            validN = true;
        }

        while (!validN) {
            System.out.println("N in not valid! Must be an integer from 2-500.");
            try {
                N = in.nextInt();
            } catch (Exception e) {
                System.out.println("Invalid input format. Exiting.");
                System.exit(0);
            }

            if (N > 1 && N < 501) {
                validN = true;
            }
        }

        System.out.println("How many messages can a mailbox hold? (Integer 1-50)");
        try {
            S = in.nextInt();
        } catch (Exception e) {
            System.out.println("Invalid input format. Exiting.");
            System.exit(0);
        }

        if (S > 0 && S < 51) {
            validS = true;
        }

        while (!validS) {
            System.out.println("S is not valid! Must be an integer from 1-50.");
            try {
                S = in.nextInt();
            } catch (Exception e) {
                System.out.println("Invalid input format. Exiting.");
                System.exit(0);
            }

            if (S > 1 && S < 51) {
                validS = true;
            }
        }

        System.out.println("How many messages will be sent in total? (Integer 5-500)");
        try {
            M = in.nextInt();
        } catch (Exception e) {
            System.out.println("Invalid input format. Exiting.");
            System.exit(0);
        }

        if (M > 4 && M < 501) {
            validM = true;
        }

        while (!validM) {
            System.out.println("M is not valid! Must be an integer from 5-500.");
            try {
                M = in.nextInt();
            } catch (Exception e) {
                System.out.println("Invalid input format. Exiting.");
                System.exit(0);
            }

            if (M > 4 && M < 501) {
                validM = true;
            }
        }

        //all inputs have been gotten and checked. Now we need to start instantiating our threads and mailboxes.
        //mailboxes[][] is a 2D array where one dimension is the person ID and the other is the messages array.

        String[][] mailboxes = new String[N][S];
        for (int i = 0; i < mailboxes.length; i++) {
            for (int j = 0; j < mailboxes[i].length; j++) {
                mailboxes[i][j] = "";
            }
        }


        Semaphore[] boxSemaphores = new Semaphore[N];
        for (int i = 0; i < boxSemaphores.length; i++) {
            boxSemaphores[i] = new Semaphore(S);
        }

        //semaphore for the total number of messages
        Semaphore messagesLeft = new Semaphore(M);

        //we will also create the array of messages and pass it to each Person. I thought it would be more efficient
        //than creating the array in every instance of the thread.
        String[] messages = new String[8];
        messages[0] = "Money is the reason we exist. Everybody knows it. It's a fact.";
        messages[1] = "When the world was at war before we just kept dancing.";
        messages[2] = "Don't you think that it's boring how people talk?";
        messages[3] = "You're so hot you're hurting my feelings.";
        messages[4] = "They'll hang us in the Louvre. Down the back but who cares? Still the Louvre.";
        messages[5] = "Under the chemtrails over the country club.";
        messages[6] = "We need the leader of a new regime.";
        messages[7] = "If I have a daughter will she have my waist or my widow's peak?";

        PostOfficePerson[] people = new PostOfficePerson[N];
        for (int i = 0; i < people.length; i++) {
            String name = Integer.toString(i);
            people[i] = new PostOfficePerson(name, mailboxes, boxSemaphores, S, messages, messagesLeft);
        }

        for (int i = 0; i < people.length; i++) {
            people[i].start();
        }
    }

    public static void diningPhilosophers(Semaphore sitBarrier, Semaphore leaveBarrier) {

        Scanner in = new Scanner(System.in);

        boolean validP = false;
        boolean validM = false;

        int P = 0;
        int M = 0;

        //getting basic input and checking for validity. If input is invalid it will loop
        //indefinitely until valid input is given.

        System.out.println("How any philosphers are there? (integer 2-1000): ");
        try {
            P = in.nextInt();
        } catch (Exception e) {
            System.out.println("Invalid input format. Exiting.");
            System.exit(0);
        }

        if (P > 2 && P <= 1000) {
            validP = true;
        }

        while (!validP) {
            System.out.println("P is not valid! Enter another (integer 2-1000): ");
            try {
                P = in.nextInt();
            } catch (Exception e) {
                System.out.println("Invalid input format. Exiting.");
                System.exit(0);
            }

            if (P > 2 && P <= 1000) {
                validP = true;
            }
        }

        System.out.println("How any meals will be eaten? (integer 1-1000): ");
        try {
            M = in.nextInt();
        } catch (Exception e) {
            System.out.println("Invalid input format. Exiting.");
            System.exit(0);
        }

        if (M > 1 && M <= 1000) {
            validM = true;
        }

        while (!validM) {
            System.out.println("M is not valid! Enter another (integer 1-1000): ");
            try {
                M = in.nextInt();
            } catch (Exception e) {
                System.out.println("Invalid input format. Exiting.");
                System.exit(0);
            }

            if (M > 1 && M <= 1000) {
                validM = true;
            }
        }

        //because I don't want to pass a variable M to each thread, as they would each have a separate copy of M meals
        //we instead create a semaphore with M permits. Philosphers will acquire() to eat a meal and will never release().
        Semaphore meals = new Semaphore(M);

        //create one chopstick per philosopher
        Semaphore[] chopsticks = new Semaphore[P];
        for (int i = 0; i < P; i++) {
            chopsticks[i] = new Semaphore(1);
        }

        //create philosophers
        Philosopher[] philosophers = new Philosopher[P];
        for (int i = 0; i < P; i++) {
            String name = Integer.toString(i);
            //System.out.println(i + "main");
            philosophers[i] = new Philosopher(name, sitBarrier, leaveBarrier, chopsticks, meals);
        }
        long start = System.nanoTime();
        //finally, we run all the philosopher threads.
        for (int i = 0; i < P; i++) {
            philosophers[i].start();
        }
        long end = System.nanoTime();

        //only after all philosophers are instantiated can they sit. sitBarrier is what keeps this from happening early.
        for (int i = 0; i < P; i++) {
            sitBarrier.release();
        }

        System.out.println(end-start);
    }
}
