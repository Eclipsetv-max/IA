package mazeai;

import java.util.*;

public class Agent {
    private final Random random;
    private final Map<String, double[]> qTable = new HashMap<>();
    private final List<String> ideas;
    private final List<String> thoughtLog = new ArrayList<>();
    private final double[] exerciseWeights = new double[10];

    private double epsilon = 0.95;
    private final double alpha = 0.14;
    private final double gamma = 0.965;

    public int wins;
    public int deaths;
    public int enemyContacts;

    public Agent(Random random, int ideaCount) {
        this.random = random;
        this.ideas = buildIdeas(ideaCount);
        Arrays.fill(exerciseWeights, 0.2);
    }

    private List<String> buildIdeas(int n) {
        List<String> list = new ArrayList<>();
        String[] feelings = {"curiosidad", "miedo", "determinación", "calma", "enfoque", "esperanza"};
        String[] intents = {"avanzar", "esquivar", "analizar", "explorar", "predecir", "aprender"};
        for (int i = 0; i < n; i++) {
            list.add("Idea " + (i + 1) + ": Siento " + feelings[i % feelings.length]
                    + " y decido " + intents[(i / 3) % intents.length] + " con patrón " + (i % 97));
        }
        return list;
    }

    public String state(Position p, Position goal) {
        int dx = Integer.compare(goal.x() - p.x(), 0);
        int dy = Integer.compare(goal.y() - p.y(), 0);
        return p.x() + ":" + p.y() + ":" + dx + ":" + dy;
    }

    public Action choose(String state) {
        if (random.nextDouble() < epsilon) {
            return Action.values()[random.nextInt(4)];
        }
        double[] q = qTable.computeIfAbsent(state, k -> new double[4]);
        int best = 0;
        for (int i = 1; i < 4; i++) if (q[i] > q[best]) best = i;
        return Action.values()[best];
    }

    public void learn(String state, Action a, double reward, String nextState, boolean done) {
        double[] q = qTable.computeIfAbsent(state, k -> new double[4]);
        double[] nextQ = qTable.computeIfAbsent(nextState, k -> new double[4]);
        double maxNext = Math.max(Math.max(nextQ[0], nextQ[1]), Math.max(nextQ[2], nextQ[3]));
        int ai = a.ordinal();
        q[ai] += alpha * ((reward + (done ? 0 : gamma * maxNext)) - q[ai]);
        epsilon = Math.max(0.02, epsilon * 0.9994);
    }

    public void reinforceThinking(int index, double value) {
        exerciseWeights[index] += value;
    }

    public double[] brainSignals(String state) {
        double[] q = qTable.computeIfAbsent(state, k -> new double[4]);
        double[] signals = new double[10];
        for (int i = 0; i < signals.length; i++) {
            signals[i] = Math.tanh(exerciseWeights[i] + q[i % 4]);
        }
        return signals;
    }

    public String thought(boolean died, boolean win, int distToGoal) {
        String base;
        if (win) base = "Gané. Mi estrategia fue eficiente.";
        else if (died) base = "Morí, pero detecté una nueva forma de moverme.";
        else if (distToGoal < 8) base = "Estoy cerca. Debo pensar con precisión.";
        else base = "Sigo procesando rutas y riesgos.";

        String idea = ideas.get(random.nextInt(ideas.size()));
        String thought = base + " " + idea;
        thoughtLog.add(thought);
        if (thoughtLog.size() > 30) thoughtLog.remove(0);
        return thought;
    }

    public List<String> thoughtLog() {
        return new ArrayList<>(thoughtLog);
    }

    public int ideaCount() {
        return ideas.size();
    }
}
