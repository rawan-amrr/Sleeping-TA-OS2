
package core;

public class Student extends Thread {

    private final int studentId;
    private final OfficeMonitor monitor;
    private final long retryDelay;

    public Student(int studentId, OfficeMonitor monitor, long retryDelay) {
        if (monitor == null) {
            throw new IllegalArgumentException("Monitor cannot be null.");
        }

        if (retryDelay < 0) {
            throw new IllegalArgumentException("Retry delay cannot be negative.");
        }

        this.studentId = studentId;
        this.monitor = monitor;
        this.retryDelay = retryDelay;

        setName("Student-" + studentId);
    }

    @Override
    public void run() {

        while (!Thread.currentThread().isInterrupted()) {

            try {
                OfficeMonitor.Ticket ticket =
                        monitor.requestHelp(studentId);

                if (ticket.result == OfficeMonitor.Result.NO_CHAIR) {

                    Thread.sleep(retryDelay);

                    monitor.studentBackFromLeaving(studentId);

                    continue;
                }

                ticket.awaitDone();

                return;

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                return;
            }
        }
    }
}
