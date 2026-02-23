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

- Una **pantalla (ventana Swing)** con progreso en **cuadraditos de colores** para IA, enemigos y laberinto.
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


## Documentación adicional (9 archivos de especificación)

Se añadieron 9 documentos en `docs/` para detallar arquitectura, reglas y evolución del proyecto:

1. `docs/01_resumen_general.md`
2. `docs/02_arquitectura_del_sistema.md`
3. `docs/03_especificacion_del_mapa.md`
4. `docs/04_sistema_de_oxigeno.md`
5. `docs/05_sistema_de_enemigos.md`
6. `docs/06_modelo_de_aprendizaje_qlearning.md`
7. `docs/07_modelo_emocional.md`
8. `docs/08_planos_y_metricas_de_progreso.md`
9. `docs/09_roadmap_de_mejoras.md`


## Nota sobre la pantalla

Si ejecutas en un entorno sin interfaz gráfica (headless), el sistema sigue funcionando en consola.


- En la vista gráfica cada celda del mapa se dibuja como un cuadrado de color.
