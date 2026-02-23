# 02 - Arquitectura del sistema

## Componentes principales

1. `Maze`
   - Crea y mantiene la grilla del laberinto.
   - Coloca paredes, inicio, meta, enemigos, oxígeno y tanque.

2. `Agent`
   - Mantiene Q-table.
   - Elige acciones con exploración/explotación.
   - Aprende con rewards y estados sucesivos.

3. `Simulation`
   - Coordina episodios y pasos.
   - Evalúa reglas de muerte, victoria y maestría.
   - Imprime métricas y minimapa.

4. `Emotion Model`
   - Traduce variables internas a texto emocional.

## Flujo de ejecución

1. Se genera laberinto.
2. Inicia episodio.
3. IA observa estado y actúa.
4. Entorno responde (enemigo, oxígeno, choque con pared, avance).
5. IA actualiza Q-table.
6. Repetición hasta muerte o victoria.
7. Nuevo episodio con memoria persistente.
