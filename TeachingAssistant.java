
package core;

public class TeachingAssistant extends Thread {

    private final int taId;
    private final OfficeMonitor monitor;
    private final long helpDuration;

    public TeachingAssistant(int taId, OfficeMonitor monitor, long helpDuration) {

        if (monitor == null) {
            throw new IllegalArgumentException("Monitor cannot be null.");
        }

        if (helpDuration < 0) {
            throw new IllegalArgumentException("Help duration cannot be negative.");
        }

        this.taId = taId;
        this.monitor = monitor;
        this.helpDuration = helpDuration;

        setName("TA-" + taId);
    }

    @Override
    public void run() {

        while (!Thread.currentThread().isInterrupted()) {

            try {
                OfficeMonitor.Ticket current =
                        monitor.waitForStudent();

                while (current != null) {

                    Thread.sleep(helpDuration);

                    current = monitor.finishAndGetNext(taId, current);
                }

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                return;
            }
        }
    }
}
