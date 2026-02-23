# 06 - Modelo de aprendizaje (Q-learning)

## Estado

El estado codifica:

- posición `x, y`
- bucket de oxígeno
- dirección relativa a la meta
- si tiene tanque
- si sabe usar tanque

## Acción

- `UP`, `DOWN`, `LEFT`, `RIGHT`

## Actualización

Se usa una versión estándar de Bellman:

`Q(s,a) = Q(s,a) + alpha * (reward + gamma * max(Q(s')) - Q(s,a))`

## Política

- Epsilon-greedy para equilibrar exploración y explotación.
