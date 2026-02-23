# 03 - Especificación del mapa

## Dimensiones por defecto

- Ancho: `120`
- Alto: `120`

## Tipos de celdas

- `#` pared
- `.` vacío
- `S` inicio
- `G` meta
- `O` nodo de oxígeno
- `T` tanque de oxígeno
- `X` enemigo
- `A` posición actual de la IA en minimapa

## Garantía de jugabilidad

El sistema abre un corredor principal entre inicio y meta para asegurar que exista al menos una ruta potencial de victoria.
