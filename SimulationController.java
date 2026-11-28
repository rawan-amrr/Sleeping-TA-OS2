package simulation;
import core.OfficeMonitor;
import core.StateListener;
import core.Student;
import core.TeachingAssistant;

import java.util.ArrayList;
import java.util.List;

public class SimulationController {
   
    private OfficeMonitor monitor;

    private final List<Student> students = new ArrayList<>();
    private final List<TeachingAssistant> tas = new ArrayList<>();

    private volatile boolean running = false;

    
    public synchronized void startSimulation(
            int numStudents,
            int numChairs,
            int numTAs,
            long retryDelay,
            long helpDuration,
            StateListener listener) {

        if (running) {
            throw new IllegalStateException(
                    "Simulation is already running.");
        }

        if (numStudents <= 0 || numTAs <= 0 ||
                numChairs < 0 || retryDelay < 0 ||
                helpDuration < 0) {
            throw new IllegalArgumentException(
                    "Invalid simulation settings.");
        }

        students.clear();
        tas.clear();

        monitor = new OfficeMonitor(numTAs, numChairs);

        if (listener != null) {
            monitor.setListener(listener);
        }

        running = true;

        
        for (int i = 1; i <= numTAs; i++) {
            TeachingAssistant ta =
                    new TeachingAssistant(i, monitor, helpDuration);

            tas.add(ta);
        }

        
        for (int i = 1; i <= numStudents; i++) {
            Student student =
                    new Student(i, monitor, retryDelay);

            students.add(student);
        }

        
        for (TeachingAssistant ta : tas) {
            ta.start();
        }

        
        for (Student student : students) {
            student.start();
        }
    }

    
    public void waitForStudents() throws InterruptedException {
        for (Student student : students) {
            student.join();
        }
    }

    
    public void stopSimulation() {

        if (!running) {
            return;
        }

        running = false;

        for (Student student : students) {
            student.interrupt();
        }

        for (TeachingAssistant ta : tas) {
            ta.interrupt();
        }

        boolean interrupted = false;

        List<Thread> allThreads = new ArrayList<>();
        allThreads.addAll(students);
        allThreads.addAll(tas);

        for (Thread thread : allThreads) {
            while (thread.isAlive()) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        }

        if (monitor != null) {
            monitor.shutdown();
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    // Check if simulation is running
    public boolean isRunning() {
        return running;
    }

    // Get statistics
    public int getTotalHelped() {
        return monitor == null ? 0 : monitor.getTotalHelped();
    }

    public int getTotalTurnedAway() {
        return monitor == null ? 0 : monitor.getTotalTurnedAway();
    }
}

