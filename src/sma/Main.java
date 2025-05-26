package sma;

import sma.util.ScenarioLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    // constant interval for a step
    private static final long T = 1000L;

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        if (args.length != 1) {
            LOGGER.severe("Usage: java -jar simulation.jar <scenario-file>.yaml");
            System.exit(1);
        }
        Path scenarioPath = Path.of(args[0]);
        Scenario scenario;
        try {
            scenario = ScenarioLoader.load(scenarioPath);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load scenario", e);
            return;
        }

        Simulation simulation = new Simulation(scenario);
        simulation.runIndefinitely(T);
    }
}
