package mazeai;

public class MainApp {
    public static void main(String[] args) {
        SimulationConfig config = new SimulationConfig(70, 70, 120, 5000, 700, 20, 5, 180, 1200);
        Simulation simulation = new Simulation(config);
        simulation.run();
    }
}
