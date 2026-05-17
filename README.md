# TFG Prototype (CLI)

Versión CLI / prototipo del proyecto [TFG](https://github.com/hibjan/TFG).

Esta aplicación permite explorar colecciones multimedia a través de metadatos y enlaces relacionales, utilizando una interfaz de línea de comandos. Lee un dataset JSON en memoria y permite navegar entre entidades, aplicar filtros y seguir enlaces entre colecciones.

## Requisitos

- Java JDK 17+
- Maven

## Compilar y ejecutar

```bash
mvn clean package
java -jar target/tfg-prototype-1.0.0.jar
```

## Uso

Al iniciar, se presentan las colecciones disponibles (ej. People, Movies, Studios). Se elige una, y se puede navegar con los siguientes comandos:

| Comando | Descripción |
|---|---|
| `select env` | Muestra las entidades del entorno actual |
| `select ent=ID` | Muestra los detalles de una entidad |
| `add_mfilter [-n] tag=valor` | Añade un filtro de metadatos (ej. `Genre=Action`) |
| `rm_mfilter [-n] tag=valor` | Elimina un filtro de metadatos |
| `add_rfilter [-n] env->tag=valor` | Añade un filtro de referencia (ej. `2->Director=108`) |
| `rm_rfilter [-n] env->tag=valor` | Elimina un filtro de referencia |
| `link env-reason` | Navega a otra colección por un motivo (ej. `2-Director`) |
| `union` | Guarda las entidades actuales y selecciona otro entorno |
| `restore` | Restaura el estado anterior |
| `goback` | Vuelve al último entorno visitado |
| `exit` | Sale del programa |

La opción `-n` invierte el filtro (NOT).

## Dataset

Por defecto se carga `src/main/resources/test_pms.json`, un dataset pequeño de prueba con People, Movies y Studios. Se puede cambiar el dataset modificando la ruta en `Main.java`.
