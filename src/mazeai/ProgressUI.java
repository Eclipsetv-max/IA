package mazeai;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ProgressUI {
    private final JFrame frame;
    private final GridCanvas gridCanvas;
    private final BrainPanel brainPanel;
    private final JTextArea log;
    private final JSlider speed;

    public ProgressUI(SimulationConfig config) {
        frame = new JFrame("IA Cuadradito - Ejercicios Mentales");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        gridCanvas = new GridCanvas();
        brainPanel = new BrainPanel();
        log = new JTextArea();
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        speed = new JSlider(config.minSpeedMs(), config.maxSpeedMs(), config.initialSpeedMs());
        speed.setPaintTicks(true);
        speed.setPaintLabels(true);
        speed.setMajorTickSpacing(40);

        JPanel right = new JPanel(new BorderLayout());
        right.add(new JScrollPane(log), BorderLayout.CENTER);
        right.add(brainPanel, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(gridCanvas), right);
        split.setResizeWeight(0.65);

        frame.setLayout(new BorderLayout());
        frame.add(speed, BorderLayout.NORTH);
        frame.add(split, BorderLayout.CENTER);
        frame.setSize(1350, 850);
        frame.setLocationRelativeTo(null);
        SwingUtilities.invokeLater(() -> frame.setVisible(true));
    }

    public int speedMs() {
        return speed.getValue();
    }

    public void update(String map, double[] signals, String thought, List<String> thoughts, String status) {
        SwingUtilities.invokeLater(() -> {
            gridCanvas.setMap(map);
            brainPanel.setSignals(signals);
            StringBuilder sb = new StringBuilder();
            sb.append(status).append("\n\nPensamiento actual:\n").append(thought).append("\n\nHistorial:\n");
            for (String t : thoughts) sb.append("- ").append(t).append('\n');
            log.setText(sb.toString());
        });
    }

    static class GridCanvas extends JPanel {
        private String[] rows = new String[0];

        GridCanvas() {
            setBackground(Color.DARK_GRAY);
        }

        void setMap(String map) {
            rows = map.split("\\n");
            int w = 1;
            for (String r : rows) w = Math.max(w, r.length());
            setPreferredSize(new Dimension(w * 16, Math.max(1, rows.length) * 16));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            for (int y = 0; y < rows.length; y++) {
                String row = rows[y];
                for (int x = 0; x < row.length(); x++) {
                    char c = row.charAt(x);
                    g.setColor(color(c));
                    g.fillRect(x * 14, y * 14, 14, 14);
                    g.setColor(Color.GRAY);
                    g.drawRect(x * 14, y * 14, 14, 14);
                }
            }
        }

        private Color color(char c) {
            return switch (c) {
                case '#': yield Color.BLACK;
                case 'A': yield new Color(50, 100, 255);
                case 'X': yield new Color(220, 60, 60);
                case 'G': yield new Color(60, 220, 90);
                case 'S': yield Color.WHITE;
                case 'M': yield new Color(250, 0, 200);
                case '*': yield new Color(255, 220, 70);
                default: yield new Color(75, 75, 75);
            };
        }
    }
}
