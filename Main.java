
import simulation.SimulationController;

public class Main {

    public static void main(String[] args) {

        SimulationController controller =
                new SimulationController();

        // Simulation settings
        int numStudents = 10;
        int numChairs = 3;
        int numTAs = 2;

        long retryDelay = 1000;
        long helpDuration = 1500;

        System.out.println("Starting Sleeping TA Simulation...");

        try {

            // Start simulation
            controller.startSimulation(
                    numStudents,
                    numChairs,
                    numTAs,
                    retryDelay,
                    helpDuration,
                    null
            );

            // Wait for students
            controller.waitForStudents();

            // Display results
            System.out.println("\nSimulation Results:");

            System.out.println(
                    "Total Helped: " +
                    controller.getTotalHelped()
            );

            System.out.println(
                    "Total Turned Away: " +
                    controller.getTotalTurnedAway()
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            System.out.println("Simulation interrupted.");

        } finally {

            controller.stopSimulation();

            System.out.println("Simulation Finished.");
        }
    }
}
