
package core;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class OfficeMonitor {

    public enum Result {
        WOKE_TA, SEATED, NO_CHAIR
    }

    // student's request
    public static class Ticket {

        public final int studentId;
        public final Result result;

        private final Semaphore done = new Semaphore(0);

        Ticket(int studentId, Result result) {
            this.studentId = studentId;
            this.result = result;
        }

        // Student wait
        public void awaitDone() throws InterruptedException {
            done.acquire();
        }
    }

    // Synchronization
    private final ReentrantLock mutex = new ReentrantLock();

    private final Semaphore chairs;
    private final Semaphore wake = new Semaphore(0);

    // Queues (protected by mutex)
    private final Queue<Ticket> directQueue = new LinkedList<>();
    private final Queue<Ticket> chairQueue = new LinkedList<>();

    // Students left(no chair)
    private final Set<Integer> awayStudents = new HashSet<>();

    // Shared counters (protected by mutex)
    private int sleepingTAs;
    private int workingTAs;
    private int waiting;
    private int comeLater;

    private int totalHelped;
    private int totalTurnedAway;

    // GUI 
    private volatile StateListener listener;

   
    private final ExecutorService notifier =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "monitor-notifier");
                t.setDaemon(true);
                return t;
            });

    // Constructor
    public OfficeMonitor(int numTAs, int numChairs) {

        if (numTAs <= 0 || numChairs < 0) {
            throw new IllegalArgumentException(
                    "Number of TAs must be positive and chairs cannot be negative."
            );
        }

        chairs = new Semaphore(numChairs);
        sleepingTAs = numTAs;
    }

    // Connect GUI listener
    public void setListener(StateListener l) {

        mutex.lock();
        try {
            listener = l;
            notifyListener(null);
        } finally {
            mutex.unlock();
        }
    }

    // student

    public Ticket requestHelp(int studentId) {

        mutex.lock();

        try {

            // Case 1: A TA is sleeping
            if (sleepingTAs > 0) {

                sleepingTAs--;
                workingTAs++;

                Ticket t = new Ticket(studentId, Result.WOKE_TA);

                directQueue.add(t);

                wake.release();

                notifyListener(
                        "Student " + studentId + " woke up a TA"
                );

                return t;
            }

            // Case 2: No sleeping TA but a chair is available
            if (chairs.tryAcquire()) {

                waiting++;

                Ticket t = new Ticket(studentId, Result.SEATED);

                chairQueue.add(t);

                notifyListener(
                        "Student " + studentId + " is waiting on a chair"
                );

                return t;
            }

            // Case 3: No available chair
            comeLater++;
            totalTurnedAway++;

            awayStudents.add(studentId);

            notifyListener(
                    "Student " + studentId +
                    " found no chair, will come later"
            );

            return new Ticket(studentId, Result.NO_CHAIR);

        } finally {
            mutex.unlock();
        }
    }

   
    public void studentBackFromLeaving(int studentId) {

        mutex.lock();

        try {

          
            if (awayStudents.remove(studentId)) {

                comeLater--;

                notifyListener(
                        "Student " + studentId +
                        " is back and tries again"
                );
            }

        } finally {
            mutex.unlock();
        }
    }

    //ta

    // TA waits until a student wakes him
    public Ticket waitForStudent() throws InterruptedException {

        wake.acquire();

        mutex.lock();

        try {
            return directQueue.poll();
        } finally {
            mutex.unlock();
        }
    }

    // Finish current student 
    public Ticket finishAndGetNext(int taId, Ticket current) {

        if (current == null) {
            throw new IllegalArgumentException(
                    "Current ticket cannot be null."
            );
        }

        mutex.lock();

        try {

            totalHelped++;

            // Get next student from the wait
            Ticket next = chairQueue.poll();

            if (next != null) {

                // The student leaves waiting chair
                chairs.release();

                waiting--;

                notifyListener(
                        "TA " + taId +
                        " takes student " + next.studentId +
                        " from a chair"
                );

            } else {

                // No students are waiting
                workingTAs--;
                sleepingTAs++;

                notifyListener(
                        "TA " + taId + " goes to sleep"
                );
            }

            
            current.done.release();

            return next;

        } finally {
            mutex.unlock();
        }
    }

    //getters

    public int getTotalHelped() {

        mutex.lock();

        try {
            return totalHelped;
        } finally {
            mutex.unlock();
        }
    }

    public int getTotalTurnedAway() {

        mutex.lock();

        try {
            return totalTurnedAway;
        } finally {
            mutex.unlock();
        }
    }

    

    private void notifyListener(String logMessage) {

        final StateListener l = listener;

        if (l == null) {
            return;
        }

  
        final int w = workingTAs;
        final int s = sleepingTAs;
        final int wt = waiting;
        final int c = comeLater;

        notifier.execute(() -> {

            l.onUpdate(w, s, wt, c);

            if (logMessage != null) {
                l.onLog(logMessage);
            }
        });
    }

    public void shutdown() {
        notifier.shutdown();
    }
}
