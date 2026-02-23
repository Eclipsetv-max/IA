package mazeai;

public record SimulationConfig(
        int width,
        int height,
        int enemies,
        int episodes,
        int maxSteps,
        int initialSpeedMs,
        int minSpeedMs,
        int maxSpeedMs,
        int ideaCount) {
}
