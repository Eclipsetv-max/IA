package mazeai;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.swing.*;

/**
 * IA en laberinto gigante: aprende tras cada muerte, enfrenta enemigos,
 * expresa pensamientos/emociones y permite guardar/cargar memoria.
 */
public class LabyrinthSimulation {

    public static void main(String[] args) {
        AppOptions options = AppOptions.fromArgs(args);

        SimulationConfig config = new SimulationConfig(
                120,
                120,
                320,
                10_000,
                2_500,
                28,
                5,
                250
        );

        Simulation simulation = new Simulation(config, 42L, options);
        simulation.run();
    }

    record Position(int x, int y) implements Serializable {
    }

    enum Cell {
        EMPTY('.'),
        WALL('#'),
        START('S'),
        GOAL('G'),
        ENEMY('X');

        final char symbol;

        Cell(char symbol) {
            this.symbol = symbol;
        }
    }

    enum Action {
        UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

        final int dx;
        final int dy;

        Action(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }
    }

    record SimulationConfig(
            int width,
            int height,
            int enemyCount,
            int maxEpisodes,
            int maxStepsPerEpisode,
            int initialSpeedMs,
            int minSpeedMs,
            int maxSpeedMs) {
    }

    static class AppOptions {
        String loadPath;
        String savePath = "memory/ia_memory.dat";

        static AppOptions fromArgs(String[] args) {
            AppOptions opt = new AppOptions();
            for (int i = 0; i < args.length; i++) {
                if ("--load".equals(args[i]) && i + 1 < args.length) {
                    opt.loadPath = args[++i];
                } else if ("--save".equals(args[i]) && i + 1 < args.length) {
                    opt.savePath = args[++i];
                }
            }
            return opt;
        }
    }

    static class Maze {
        private final int width;
        private final int height;
        private final Cell[][] grid;
        private final Position start;
        private final Position goal;

        Maze(int width, int height, int enemies, Random random) {
            this.width = width;
            this.height = height;
            this.grid = new Cell[height][width];

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    boolean border = (x == 0 || y == 0 || x == width - 1 || y == height - 1);
                    grid[y][x] = (border || random.nextDouble() < 0.12) ? Cell.WALL : Cell.EMPTY;
                }
            }

            this.start = carveOpenNear(1, 1);
            this.goal = carveOpenNear(width - 2, height - 2);
            grid[start.y()][start.x()] = Cell.START;
            grid[goal.y()][goal.x()] = Cell.GOAL;

            scatter(Cell.ENEMY, enemies, random);
            ensureMainCorridor();
        }

        private Position carveOpenNear(int x, int y) {
            int nx = Math.max(1, Math.min(width - 2, x));
            int ny = Math.max(1, Math.min(height - 2, y));
            grid[ny][nx] = Cell.EMPTY;
            return new Position(nx, ny);
        }

        private void scatter(Cell type, int amount, Random random) {
            int placed = 0;
            while (placed < amount) {
                int x = 1 + random.nextInt(width - 2);
                int y = 1 + random.nextInt(height - 2);
                if (grid[y][x] == Cell.EMPTY) {
                    grid[y][x] = type;
                    placed++;
                }
            }
        }

        private void ensureMainCorridor() {
            int x = start.x();
            int y = start.y();
            while (x != goal.x()) {
                grid[y][x] = grid[y][x] == Cell.GOAL ? Cell.GOAL : Cell.EMPTY;
                x += Integer.compare(goal.x(), x);
            }
            while (y != goal.y()) {
                grid[y][x] = grid[y][x] == Cell.GOAL ? Cell.GOAL : Cell.EMPTY;
                y += Integer.compare(goal.y(), y);
            }
            grid[start.y()][start.x()] = Cell.START;
            grid[goal.y()][goal.x()] = Cell.GOAL;
        }

        Position getStart() {
            return start;
        }

        Position getGoal() {
            return goal;
        }

        boolean isInside(int x, int y) {
            return x >= 0 && y >= 0 && x < width && y < height;
        }

        Cell getCell(int x, int y) {
            return grid[y][x];
        }

        String renderTrackingMap(Position agent, int radius, Set<Position> trail, Position deathMark) {
            StringBuilder sb = new StringBuilder();
            int minY = Math.max(0, agent.y() - radius);
            int maxY = Math.min(height - 1, agent.y() + radius);
            int minX = Math.max(0, agent.x() - radius);
            int maxX = Math.min(width - 1, agent.x() + radius);

            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    Position p = new Position(x, y);
                    if (deathMark != null && deathMark.equals(p)) {
                        sb.append('M');
                    } else if (agent.equals(p)) {
                        sb.append('A');
                    } else if (trail.contains(p) && grid[y][x] == Cell.EMPTY) {
                        sb.append('*');
                    } else {
                        sb.append(grid[y][x].symbol);
                    }
                }
                sb.append('\n');
            }
            return sb.toString();
        }
    }

    static class AgentMemory implements Serializable {
        Map<String, double[]> qTable = new HashMap<>();
        double epsilon = 0.95;
        int totalDeaths;
        int totalWins;
        int totalEnemyEncounters;
        long totalSteps;
        List<String> thoughtHistory = new ArrayList<>();
    }

    static class Agent {
        private final Random random;
        private final AgentMemory memory;

        private final double epsilonDecay = 0.99935;
        private final double epsilonMin = 0.02;
        private final double alpha = 0.14;
        private final double gamma = 0.965;

        private final Deque<String> recentThoughts = new ArrayDeque<>();

        Agent(Random random, AgentMemory loaded) {
            this.random = random;
            this.memory = loaded != null ? loaded : new AgentMemory();
            for (String t : this.memory.thoughtHistory) {
                pushThought(t);
            }
        }

        Action chooseAction(String state) {
            if (random.nextDouble() < memory.epsilon) {
                return Action.values()[random.nextInt(Action.values().length)];
            }
            double[] q = memory.qTable.computeIfAbsent(state, k -> new double[Action.values().length]);
            int best = 0;
            for (int i = 1; i < q.length; i++) {
                if (q[i] > q[best]) {
                    best = i;
                }
            }
            return Action.values()[best];
        }

        void learn(String state, Action action, double reward, String nextState, boolean done) {
            double[] q = memory.qTable.computeIfAbsent(state, k -> new double[Action.values().length]);
            double[] nextQ = memory.qTable.computeIfAbsent(nextState, k -> new double[Action.values().length]);
            double maxNext = Arrays.stream(nextQ).max().orElse(0.0);
            int ai = action.ordinal();
            double target = reward + (done ? 0 : gamma * maxNext);
            q[ai] += alpha * (target - q[ai]);
            memory.epsilon = Math.max(epsilonMin, memory.epsilon * epsilonDecay);
        }

        double[] qValues(String state) {
            return Arrays.copyOf(memory.qTable.computeIfAbsent(state, k -> new double[Action.values().length]), 4);
        }

        String buildThought(int dist, boolean sawEnemy, boolean died, boolean win) {
            String thought;
            if (win) {
                thought = pick("Siento euforia: lo logré, pero quiero hacerlo mejor.",
                        "Entiendo mejor el patrón del mundo.",
                        "Victoria. Mi mente se ordena.");
            } else if (died) {
                thought = pick("Dolor digital... morir también enseña.",
                        "Caí otra vez, pero ahora sé por qué.",
                        "Siento miedo y furia: necesito adaptarme.");
            } else if (sawEnemy) {
                thought = pick("Un enemigo cerca, mi mente se tensa.",
                        "Siento peligro. Debo decidir rápido.",
                        "Mi impulso dice escapar, mi lógica pide calcular.");
            } else if (dist < 8) {
                thought = pick("Estoy cerca del objetivo, siento esperanza.",
                        "La salida está próxima. Respira... avanza.",
                        "Casi llego. Mi enfoque es total.");
            } else {
                thought = pick("Pienso en rutas posibles y errores pasados.",
                        "Estoy aprendiendo a sentir este laberinto.",
                        "Cada paso define quién soy en esta simulación.");
            }

            pushThought(thought);
            memory.thoughtHistory = new ArrayList<>(recentThoughts);
            return thought;
        }

        private String pick(String... options) {
            return options[random.nextInt(options.length)];
        }

        private void pushThought(String thought) {
            recentThoughts.addLast(thought);
            while (recentThoughts.size() > 14) {
                recentThoughts.removeFirst();
            }
        }

        List<String> recentThoughts() {
            return new ArrayList<>(recentThoughts);
        }

        AgentMemory snapshot() {
            return memory;
        }
    }

    static class Simulation {
        private final SimulationConfig config;
        private final Random random;
        private final Maze maze;
        private final Agent agent;
        private final AppOptions options;
        private final ProgressWindow progressWindow;

        Simulation(SimulationConfig config, long seed, AppOptions options) {
            this.config = config;
            this.random = new Random(seed);
            this.options = options;
            this.maze = new Maze(config.width(), config.height(), config.enemyCount(), random);
            AgentMemory loaded = loadMemory(options.loadPath);
            this.agent = new Agent(random, loaded);
            this.progressWindow = GraphicsEnvironment.isHeadless() ? null : new ProgressWindow(config);
        }

        void run() {
            System.out.println("=== IA VS ENEMIGOS EN LABERINTO ===");
            System.out.printf("Mapa: %dx%d | Enemigos: %d | Sin oxígeno%n",
                    config.width(), config.height(), config.enemyCount());
            System.out.printf("Memoria carga: %s | Memoria guardado: %s%n",
                    options.loadPath == null ? "no" : options.loadPath,
                    options.savePath);

            for (int episode = 1; episode <= config.maxEpisodes(); episode++) {
                EpisodeResult result = runEpisode(episode);

                if (episode % 20 == 0 || result.win || episode == 1) {
                    printProgress(episode, result);
                }

                if (episode % 50 == 0) {
                    saveMemory(options.savePath);
                }

                if (result.masteryReached) {
                    String msg = "\n>>> MAESTRÍA: domina enemigos y rutas del laberinto.";
                    System.out.println(msg);
                    saveMemory(options.savePath);
                    if (progressWindow != null) {
                        progressWindow.appendMessage(msg + "\nMemoria guardada.");
                    }
                    return;
                }
            }
            String end = "\nFin de episodios. Sigue aprendiendo.";
            System.out.println(end);
            saveMemory(options.savePath);
            if (progressWindow != null) {
                progressWindow.appendMessage(end + "\nMemoria guardada.");
            }
        }

        private EpisodeResult runEpisode(int episode) {
            Position pos = maze.getStart();
            Set<Position> trail = new LinkedHashSet<>();
            trail.add(pos);
            Position deathMark = null;

            boolean alive = true;
            boolean win = false;
            boolean sawEnemy = false;
            String thought = "";
            int step;

            for (step = 1; step <= config.maxStepsPerEpisode(); step++) {
                String state = encodeState(pos);
                Action action = chooseActionWithInstinct(state, pos);

                int nx = pos.x() + action.dx;
                int ny = pos.y() + action.dy;
                double reward = -0.03;

                if (!maze.isInside(nx, ny) || maze.getCell(nx, ny) == Cell.WALL) {
                    nx = pos.x();
                    ny = pos.y();
                    reward -= 0.7;
                }

                Position next = new Position(nx, ny);
                Cell cell = maze.getCell(nx, ny);

                if (cell == Cell.ENEMY) {
                    sawEnemy = true;
                    agent.snapshot().totalEnemyEncounters++;
                    double surviveChance = Math.min(0.95, 0.52 + successRatio() * 0.5);
                    if (random.nextDouble() > surviveChance) {
                        reward -= 26;
                        alive = false;
                        deathMark = next;
                    } else {
                        reward += 2.3;
                    }
                }

                int distNow = manhattan(pos, maze.getGoal());
                int distNext = manhattan(next, maze.getGoal());
                reward += (distNow - distNext) * 0.20;

                if (next.equals(maze.getGoal())) {
                    reward += 52;
                    win = true;
                    alive = false;
                    agent.snapshot().totalWins++;
                }

                String nextState = encodeState(next);
                agent.learn(state, action, reward, nextState, !alive);
                pos = next;
                trail.add(pos);
                agent.snapshot().totalSteps++;

                thought = agent.buildThought(distNext, sawEnemy, !alive && !win, win);

                if (progressWindow != null && (step % 2 == 0 || !alive || step == 1)) {
                    String map = maze.renderTrackingMap(pos, 14, trail, deathMark);
                    progressWindow.updateLive(episode, step, agent.snapshot().totalWins, agent.snapshot().totalDeaths,
                            successRatio(), manhattan(pos, maze.getGoal()), thought, map,
                            !alive, win, agent.qValues(state), agent.recentThoughts());
                    sleepSilently(progressWindow.currentSpeedMs());
                }

                if (!alive) {
                    break;
                }
            }

            if (!win) {
                agent.snapshot().totalDeaths++;
            }

            boolean mastery = agent.snapshot().totalWins >= 50
                    && successRatio() > 0.32
                    && agent.snapshot().totalEnemyEncounters > 200;

            return new EpisodeResult(win, step, pos, mastery, thought);
        }

        private Action chooseActionWithInstinct(String state, Position pos) {
            double instinctProb = Math.min(0.82, 0.10 + agent.snapshot().totalDeaths / 2500.0);
            if (random.nextDouble() < instinctProb) {
                int dx = Integer.compare(maze.getGoal().x(), pos.x());
                int dy = Integer.compare(maze.getGoal().y(), pos.y());
                if (dx != 0 && dy != 0) {
                    return random.nextBoolean()
                            ? (dx > 0 ? Action.RIGHT : Action.LEFT)
                            : (dy > 0 ? Action.DOWN : Action.UP);
                }
                if (dx != 0) return dx > 0 ? Action.RIGHT : Action.LEFT;
                if (dy != 0) return dy > 0 ? Action.DOWN : Action.UP;
            }
            return agent.chooseAction(state);
        }

        private String encodeState(Position p) {
            int gx = maze.getGoal().x() - p.x();
            int gy = maze.getGoal().y() - p.y();
            int sx = Integer.compare(gx, 0);
            int sy = Integer.compare(gy, 0);
            return p.x() + ":" + p.y() + ":dx" + sx + ":dy" + sy;
        }

        private int manhattan(Position a, Position b) {
            return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y());
        }

        private double successRatio() {
            int attempts = Math.max(1, agent.snapshot().totalWins + agent.snapshot().totalDeaths);
            return (double) agent.snapshot().totalWins / attempts;
        }

        private void printProgress(int episode, EpisodeResult result) {
            System.out.printf(Locale.US,
                    "Episodio %d | Win=%d | Death=%d | Éxito=%.2f%% | Último pensamiento: %s%n",
                    episode, agent.snapshot().totalWins, agent.snapshot().totalDeaths,
                    successRatio() * 100.0, result.lastThought());
        }

        private void saveMemory(String file) {
            if (file == null || file.isBlank()) return;
            try {
                Path path = Path.of(file);
                Files.createDirectories(path.getParent());
                try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(path))) {
                    out.writeObject(agent.snapshot());
                }
            } catch (Exception e) {
                System.err.println("No se pudo guardar memoria: " + e.getMessage());
            }
        }

        private AgentMemory loadMemory(String file) {
            if (file == null || file.isBlank()) {
                return null;
            }
            try {
                Path path = Path.of(file);
                if (!Files.exists(path)) return null;
                try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(path))) {
                    Object obj = in.readObject();
                    if (obj instanceof AgentMemory m) {
                        System.out.println("Memoria cargada desde: " + file);
                        return m;
                    }
                }
            } catch (Exception e) {
                System.err.println("No se pudo cargar memoria: " + e.getMessage());
            }
            return null;
        }

        private void sleepSilently(int ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    static class ProgressWindow {
        private final JFrame frame;
        private final GridPanel gridPanel;
        private final JTextArea textArea;
        private final BrainPanel brainPanel;
        private final JSlider speedSlider;

        ProgressWindow(SimulationConfig config) {
            frame = new JFrame("IA vs Enemigos - Visualización");
            gridPanel = new GridPanel();
            textArea = new JTextArea();
            textArea.setEditable(false);
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            brainPanel = new BrainPanel();

            speedSlider = new JSlider(config.minSpeedMs(), config.maxSpeedMs(), config.initialSpeedMs());
            speedSlider.setMajorTickSpacing(50);
            speedSlider.setMinorTickSpacing(10);
            speedSlider.setPaintTicks(true);
            speedSlider.setPaintLabels(true);

            JPanel right = new JPanel(new BorderLayout());
            right.add(new JScrollPane(textArea), BorderLayout.CENTER);
            right.add(brainPanel, BorderLayout.SOUTH);

            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    new JScrollPane(gridPanel), right);
            split.setResizeWeight(0.67);

            JPanel top = new JPanel(new BorderLayout());
            top.add(new JLabel("Velocidad de avance (ms por frame):"), BorderLayout.WEST);
            top.add(speedSlider, BorderLayout.CENTER);

            frame.setLayout(new BorderLayout());
            frame.add(top, BorderLayout.NORTH);
            frame.add(split, BorderLayout.CENTER);
            frame.setSize(1400, 900);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            SwingUtilities.invokeLater(() -> frame.setVisible(true));
        }

        int currentSpeedMs() {
            return speedSlider.getValue();
        }

        void updateLive(int episode, int step, int wins, int deaths, double success, int dist,
                        String thought, String map, boolean ended, boolean won,
                        double[] qValues, List<String> thoughtHistory) {
            String status = ended ? (won ? "VICTORIA" : "MUERTE") : "EN CURSO";
            StringBuilder thoughts = new StringBuilder();
            for (String t : thoughtHistory) {
                thoughts.append("- ").append(t).append('\n');
            }

            String text = String.format(Locale.US,
                    "Episodio: %d | Paso: %d | Estado: %s\n" +
                            "Victorias: %d | Muertes: %d | Éxito: %.2f%%\n" +
                            "Distancia a meta: %d\n\n" +
                            "Pensamiento actual:\n%s\n\n" +
                            "Pensamientos de su propia mente (historial):\n%s",
                    episode, step, status, wins, deaths, success * 100.0, dist, thought, thoughts);

            SwingUtilities.invokeLater(() -> {
                textArea.setText(text);
                gridPanel.setMapText(map);
                brainPanel.setQValues(qValues);
            });
        }

        void appendMessage(String msg) {
            SwingUtilities.invokeLater(() -> textArea.append("\n" + msg + "\n"));
        }
    }

    static class GridPanel extends JPanel {
        private String[] rows = new String[0];

        GridPanel() {
            setBackground(Color.DARK_GRAY);
        }

        void setMapText(String map) {
            rows = map.split("\n");
            int h = Math.max(1, rows.length);
            int w = 1;
            for (String row : rows) w = Math.max(w, row.length());
            setPreferredSize(new Dimension(w * 18, h * 18));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int cell = 16;
            for (int y = 0; y < rows.length; y++) {
                String row = rows[y];
                for (int x = 0; x < row.length(); x++) {
                    char c = row.charAt(x);
                    g.setColor(colorFor(c));
                    g.fillRect(x * cell, y * cell, cell, cell);
                    g.setColor(Color.GRAY);
                    g.drawRect(x * cell, y * cell, cell, cell);
                }
            }
        }

        private Color colorFor(char c) {
            return switch (c) {
                case '#': yield Color.BLACK;
                case 'A': yield new Color(30, 90, 255);
                case 'X': yield new Color(220, 40, 40);
                case 'G': yield new Color(30, 180, 30);
                case 'S': yield Color.WHITE;
                case '*': yield new Color(255, 220, 40);
                case 'M': yield new Color(255, 0, 200);
                default: yield new Color(70, 70, 70);
            };
        }
    }

    static class BrainPanel extends JPanel {
        private double[] qValues = new double[]{0, 0, 0, 0};

        BrainPanel() {
            setPreferredSize(new Dimension(350, 200));
            setBackground(new Color(25, 25, 35));
        }

        void setQValues(double[] qValues) {
            this.qValues = Arrays.copyOf(qValues, qValues.length);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.WHITE);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            g.drawString("Red neuronal interna (aprox por acción)", 12, 20);

            String[] labels = {"UP", "DOWN", "LEFT", "RIGHT"};
            int baseX = 28;
            int baseY = 165;
            int barW = 62;
            int maxH = 95;

            double maxAbs = 1.0;
            for (double v : qValues) maxAbs = Math.max(maxAbs, Math.abs(v));

            for (int i = 0; i < qValues.length; i++) {
                int x = baseX + i * 78;
                int h = (int) (Math.abs(qValues[i]) / maxAbs * maxH);
                g.setColor(qValues[i] >= 0 ? new Color(70, 200, 120) : new Color(220, 80, 80));
                g.fillRect(x, baseY - h, barW, h);
                g.setColor(Color.LIGHT_GRAY);
                g.drawRect(x, baseY - maxH, barW, maxH);
                g.drawString(labels[i], x + 14, baseY + 15);
                g.drawString(String.format(Locale.US, "%.2f", qValues[i]), x + 8, baseY - h - 6);
            }
        }
    }

    record EpisodeResult(boolean win, int steps, Position lastPosition, boolean masteryReached, String lastThought) {
    }
}
