package mazeai;

import java.awt.GraphicsEnvironment;
import java.util.*;

public class Simulation {
    private final SimulationConfig config;
    private final Random random = new Random(44);
    private final GridWorld world;
    private final Agent agent;
    private final ProgressUI ui;

    private final PatternPredictionExercise ex1 = new PatternPredictionExercise();
    private final SequenceMemoryExercise ex2 = new SequenceMemoryExercise();
    private final SpatialRotationExercise ex3 = new SpatialRotationExercise();
    private final ThreatAssessmentExercise ex4 = new ThreatAssessmentExercise();
    private final PathCompressionExercise ex5 = new PathCompressionExercise();
    private final FocusStabilityExercise ex6 = new FocusStabilityExercise();
    private final StrategySwitchExercise ex7 = new StrategySwitchExercise();
    private final AbstractReasoningExercise ex8 = new AbstractReasoningExercise();
    private final CreativeAssociationExercise ex9 = new CreativeAssociationExercise();
    private final EmotionalRegulationExercise ex10 = new EmotionalRegulationExercise();

    public Simulation(SimulationConfig config) {
        this.config = config;
        this.world = new GridWorld(config.width(), config.height(), config.enemies(), random);
        this.agent = new Agent(random, Math.max(1001, config.ideaCount()));
        this.ui = GraphicsEnvironment.isHeadless() ? null : new ProgressUI(config);
    }

    public void run() {
        System.out.println("Ejercicios mentales: 10 | Archivos Java: 17 | Ideas: " + agent.ideaCount());
        for (int episode = 1; episode <= config.episodes(); episode++) {
            runEpisode(episode);
        }
    }

    private void runEpisode(int episode) {
        Position pos = world.start();
        Set<Position> trail = new LinkedHashSet<>();
        trail.add(pos);
        Position death = null;
        boolean alive = true;
        boolean win = false;

        trainMind();

        for (int step = 1; step <= config.maxSteps(); step++) {
            String state = agent.state(pos, world.goal());
            Action action = agent.choose(state);
            int nx = pos.x() + action.dx;
            int ny = pos.y() + action.dy;

            double reward = -0.03;
            if (!world.inside(nx, ny) || world.cell(nx, ny) == '#') {
                nx = pos.x();
                ny = pos.y();
                reward -= 0.7;
            }

            Position next = new Position(nx, ny);
            char c = world.cell(nx, ny);
            if (c == 'X') {
                agent.enemyContacts++;
                if (random.nextDouble() < 0.58) {
                    reward -= 18;
                    alive = false;
                    death = next;
                } else {
                    reward += 1.6;
                }
            }

            int oldDist = manhattan(pos, world.goal());
            int newDist = manhattan(next, world.goal());
            reward += (oldDist - newDist) * 0.2;

            if (next.equals(world.goal())) {
                reward += 32;
                win = true;
                alive = false;
                agent.wins++;
            }

            String nextState = agent.state(next, world.goal());
            agent.learn(state, action, reward, nextState, !alive);
            pos = next;
            trail.add(pos);

            String thought = agent.thought(!alive && !win, win, newDist);
            String map = world.render(pos, trail, death, 14);

            if (ui != null && (step % 2 == 0 || !alive || step == 1)) {
                String status = String.format(Locale.US,
                        "Ep:%d Paso:%d Win:%d Death:%d Contactos:%d Eficiencia:%.2f%% Ideas:%d",
                        episode, step, agent.wins, agent.deaths, agent.enemyContacts,
                        successRatio() * 100.0, agent.ideaCount());
                ui.update(map, agent.brainSignals(state), thought, agent.thoughtLog(), status);
                sleep(ui.speedMs());
            }

            if (!alive) break;
        }

        if (!win) agent.deaths++;
        if (episode % 20 == 0) {
            System.out.printf(Locale.US, "Ep %d | Win=%d Death=%d Eficiencia=%.2f%%%n",
                    episode, agent.wins, agent.deaths, successRatio() * 100.0);
        }
    }

    private void trainMind() {
        agent.reinforceThinking(0, ex1.run(agent, random));
        agent.reinforceThinking(1, ex2.run(agent, random));
        agent.reinforceThinking(2, ex3.run(agent, random));
        agent.reinforceThinking(3, ex4.run(agent, random));
        agent.reinforceThinking(4, ex5.run(agent, random));
        agent.reinforceThinking(5, ex6.run(agent, random));
        agent.reinforceThinking(6, ex7.run(agent, random));
        agent.reinforceThinking(7, ex8.run(agent, random));
        agent.reinforceThinking(8, ex9.run(agent, random));
        agent.reinforceThinking(9, ex10.run(agent, random));
    }

    private double successRatio() {
        int t = Math.max(1, agent.wins + agent.deaths);
        return (double) agent.wins / t;
    }

    private int manhattan(Position a, Position b) {
        return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y());
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
