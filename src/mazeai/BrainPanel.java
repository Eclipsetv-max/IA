package mazeai;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class BrainPanel extends JPanel {
    private double[] signals = new double[10];

    public BrainPanel() {
        setPreferredSize(new Dimension(360, 260));
        setBackground(new Color(15, 15, 28));
    }

    public void setSignals(double[] signals) {
        this.signals = Arrays.copyOf(signals, signals.length);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int[][] nodes = {
                {60, 70}, {130, 45}, {210, 45}, {285, 70},
                {80, 145}, {150, 120}, {225, 120}, {300, 145},
                {130, 210}, {225, 210}
        };

        int[][] edges = {
                {0, 1}, {1, 2}, {2, 3}, {0, 4}, {1, 5}, {2, 6}, {3, 7},
                {4, 5}, {5, 6}, {6, 7}, {4, 8}, {5, 8}, {6, 9}, {7, 9}, {8, 9}
        };

        for (int[] e : edges) {
            double intensity = (Math.abs(signals[e[0]]) + Math.abs(signals[e[1]])) / 2.0;
            int glow = 40 + (int) (215 * intensity);
            g2.setColor(new Color(glow, glow, 255));
            g2.setStroke(new BasicStroke(2 + (float) (2 * intensity)));
            g2.drawLine(nodes[e[0]][0], nodes[e[0]][1], nodes[e[1]][0], nodes[e[1]][1]);
        }

        for (int i = 0; i < nodes.length; i++) {
            int glow = 70 + (int) (185 * Math.abs(signals[i]));
            g2.setColor(new Color(255, glow, 120));
            g2.fillOval(nodes[i][0] - 9, nodes[i][1] - 9, 18, 18);
            g2.setColor(Color.WHITE);
            g2.drawOval(nodes[i][0] - 9, nodes[i][1] - 9, 18, 18);
        }

        g2.setColor(Color.WHITE);
        g2.drawString("Cerebro de la IA: líneas iluminadas mientras piensa", 18, 20);
    }
}
