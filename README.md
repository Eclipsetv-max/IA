# IA en Laberinto Gigante (Java)

Simulación en Java de una IA que:

- vive en un **laberinto muy grande**,
- tiene **enemigos**,
- maneja **oxígeno** limitado,
- busca y aprende a usar un **tanque de oxígeno**,
- **expresa sentimientos** según su estado,
- y **aprende con cada muerte** usando Q-learning, guardando conocimiento entre episodios.

## Ejecutar

```bash
javac -d out src/mazeai/LabyrinthSimulation.java
java -cp out mazeai.LabyrinthSimulation
```

## Qué verás

- Métricas de progreso por episodios (victorias, muertes, ratio de éxito).
- Estado emocional de la IA (ansiedad, esperanza, frustración, determinación, y reflexión sobre la simulación).
- Un **plano local del laberinto** (minimapa con `A` para la IA) para observar el progreso.
- Mensaje de **maestría** cuando la IA alcanza una condición avanzada de aprendizaje.

## Ajustes rápidos

Dentro de `main`, en `SimulationConfig`, puedes modificar:

- tamaño del laberinto,
- cantidad de enemigos,
- cantidad de nodos de oxígeno,
- episodios máximos,
- pasos máximos por episodio.
