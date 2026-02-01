# Manual del proyecto – Piedra, Papel, Tijeras en red

**Autora:** María Pérez Vaca  
**Fecha:** 31/01/2026

---

## 1. Descripción general

Este proyecto implementa la parte del servidor de un sistema cliente‑servidor en Java que permite jugar a Piedra, Papel, Tijeras a través de sockets TCP.  

El servidor:
*   Selecciona automáticamente un puerto libre dentro del rango permitido.
*   Crea un `ServerSocket` y espera a que se conecten dos clientes.
*   Coordina la partida entre **GameHost** (servidor), **Player1** y **Player2**.
*   Gestiona la lógica completa del juego.
*   Controla puntuaciones, eliminaciones y final de partida.
*   Cierra las conexiones de forma ordenada.

> **Nota:** Los clientes existen, pero su implementación corresponde a mis compañeros. Este documento se centra exclusivamente en el funcionamiento del servidor.

## 2. Estructura del proyecto

*   **`Servidor.java`**: Contiene toda la lógica del servidor:
    *   Selección de puerto.
    *   Creación del socket.
    *   Aceptación de conexiones.
    *   Bucle de juego.
    *   Resolución de rondas.
    *   Sistema de puntuaciones y eliminaciones.
*   **`App.java`**: Archivo placeholder sin relevancia para la ejecución del juego.

## 3. Selección automática de puerto

El servidor no utiliza un puerto fijo. En su lugar, selecciona automáticamente un puerto libre dentro del rango **1024–49151**.

### 3.1. Motivos del rango elegido

*   Los puertos **<1024** son puertos reservados del sistema.
*   Los puertos **49152–65535** son puertos efímeros asignados automáticamente por el sistema operativo.
*   El rango **1024–49151** es el más adecuado para aplicaciones personalizadas sin interferir con servicios del sistema ni con puertos efímeros.

### 3.2. Proceso de selección

El servidor realiza una verificación doble:

1.  **Consulta de puertos ocupados**: Ejecuta internamente `netstat -an` y extrae los puertos en estado `LISTENING`.
2.  **Verificación real mediante bind**: Para cada puerto candidato:
    *   Si no aparece en `netstat`, intenta abrir un `ServerSocket` temporal.
    *   Si el *bind* funciona, el puerto está libre y se utiliza.

Este método evita condiciones de carrera y garantiza que el puerto realmente está disponible.

El servidor muestra finalmente:
```
Servidor escuchando en:
IP: <IP_LOCAL>
Puerto: <PUERTO_SELECCIONADO>
```

## 4. Lógica del juego

El servidor coordina una partida entre tres participantes:
1.  **GameHost** (el propio servidor)
2.  **Player1** (cliente 1)
3.  **Player2** (cliente 2)

Las jugadas se codifican como:
*   **1**: Piedra
*   **2**: Papel
*   **3**: Tijeras

### 4.1. Flujo de cada ronda

1.  **Entrada de jugadas**:
    *   El **GameHost** introduce su jugada por consola.
    *   El servidor envía `JUEGA` a cada cliente y espera su respuesta.
2.  **Determinación del resultado**:
    *   Se aplican las reglas básicas del juego.
    *   Se contemplan los casos multijugador: triple empate, los tres distintos, dos iguales contra uno distinto.
3.  **Actualización de puntuaciones**:
    *   **+1** para el ganador.
    *   **−1** para el perdedor.
    *   **0** en caso de empate.
4.  **Eliminaciones**:
    *   Un jugador queda eliminado al llegar a **−3 puntos**.
5.  **Fin de partida**:
    *   La partida termina cuando un jugador alcanza **+3 puntos** o solo queda un jugador no eliminado.
6.  **Comunicación**:
    *   El servidor envía a los clientes el marcador actualizado y el estado de la partida.

## 5. Ejecución del servidor (paso a paso)

A continuación se explica cómo compilar y ejecutar el servidor.

### 5.1. Requisitos previos

*   Java (JDK)
*   Maven
*   Windows (la lógica de `netstat` está adaptada a su salida)
*   Opcional: `telnet` para pruebas manuales

### 5.2. Compilar el proyecto

Desde la raíz del proyecto:

```bash
mvn -q package
```

### 5.3. Ejecutar el servidor

```bash
java -cp target/classes org.example.Servidor
```

Ejemplo de salida:
```
Servidor escuchando en:
IP: 192.168.1.42
Puerto: 34567
```
En este punto, el servidor está listo para aceptar conexiones de los clientes.

### 5.4. Comprobación opcional con telnet

Antes de ejecutar los clientes, se puede comprobar que el servidor está escuchando:

```bash
telnet 192.168.1.42 34567
```

Si la conexión es correcta:
```
Conectado a 192.168.1.42.
Escape character is '^]'.
```

## 6. Comportamiento esperado

*   El servidor selecciona un puerto libre automáticamente.
*   Acepta dos conexiones de cliente.
*   Ejecuta rondas de juego hasta que se cumple una condición de victoria o eliminación.
*   Anuncia el ganador.
*   Cierra las conexiones correctamente.

## 7. Responsabilidades por archivo

*   **`Servidor.java`**: Lógica del juego, comunicación por sockets, selección de puerto.
