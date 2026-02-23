# IA en Laberinto Gigante (Java)

Simulación en Java donde una IA:

- vive en un **laberinto muy grande**,
- enfrenta **enemigos**,
- aprende tras cada muerte (Q-learning),
- muestra sus **pensamientos y emociones**,
- permite **guardar/cargar memoria** para no perder progreso,
- y muestra una vista gráfica con:
  - plano en **cuadraditos**,
  - panel lateral de **“red neuronal interna”** (aproximación con valores Q por acción),
  - historial de pensamientos propios.

> Se eliminó el sistema de oxígeno: ahora el foco es IA vs enemigos.

## Ejecutar

```bash
javac -d out src/mazeai/LabyrinthSimulation.java
java -cp out mazeai.LabyrinthSimulation
```

## Guardar y cargar memoria

- Guardar en ruta personalizada:

```bash
java -cp out mazeai.LabyrinthSimulation --save memory/mi_ia.dat
```

- Cargar memoria anterior y seguir entrenando:

```bash
java -cp out mazeai.LabyrinthSimulation --load memory/mi_ia.dat --save memory/mi_ia.dat
```

## Velocidad de avance

En la ventana gráfica puedes **aumentar o disminuir** la velocidad con el slider superior (ms por frame).

## Qué verás

- Plano animado del entorno (IA, enemigos, paredes, ruta y punto de muerte).
- Estado del episodio (en curso / victoria / muerte).
- Pensamiento actual y pensamientos recientes de su “mente”.
- Panel de red neuronal interna aproximada para interpretar decisiones por acción.
