(The file `c:\Users\mpvlm\Desktop\Clases\PSP\Piedra_Papel_Tijera\Manual.md` exists, but is empty)
**Resumen**

 - **Proyecto:** Juego "Piedra, Papel, Tijeras" con servidor que coordina partidas para dos clientes.
 - **Objetivo del manual:** explicar el código del servidor y del programa auxiliar (`App`), detallar la lógica de selección de puerto (rango 1024–49151) y ofrecer instrucciones para ejecutar y probar la aplicación.

**Archivos principales**
 - **Código del servidor:** [src/main/java/org/example/Servidor.java](src/main/java/org/example/Servidor.java#L1-L400)
 - **Programa auxiliar/placeholder:** [src/main/java/org/example/App.java](src/main/java/org/example/App.java#L1-L20)

**Explicación general del proyecto**
 - **`Servidor`**: programa que crea un `ServerSocket`, acepta dos conexiones de cliente (Player1 y Player2), y ejecuta un bucle de juego en el que el servidor (GameHost) y ambos clientes envían jugadas (1=piedra, 2=papel, 3=tijeras). Controla puntuaciones, eliminaciones y anuncia el ganador.

**Explicación detallada de `Servidor`**
 - **Inicio y configuración:**
	 - El servidor ahora elige automáticamente un puerto libre en el rango 1024–49151. El punto de entrada es `main` en `Servidor.java`.
 - **Selección de puerto:** método `seleccionarPuertoLibre()`:
	 - Ejecuta `netstat -an` en Windows (con `cmd /c netstat -an`) y lee la salida.
	 - Usa una expresión regular para extraer números de puerto en líneas que contienen `LISTENING`.
	 - Construye un `Set<Integer>` con los puertos ya en escucha.
	 - Genera candidatos a puerto aleatorios entre 1024 y 49151; para cada candidato no listado, intenta crear un `ServerSocket` temporal en ese puerto. Si el bind tiene éxito, devuelve ese puerto.
	 - Si no encuentra puerto tras muchos intentos, lanza una excepción.
 - **Motivación de la verificación doble (netstat + bind):**
	 - `netstat` permite conocer puertos ocupados en el sistema; sin embargo, entre el momento de leer `netstat` y el de abrir el socket puede haber una condición de carrera. Por eso, además de evitar candidatos que `netstat` mostró ocupados, el método intenta enlazar momentáneamente el puerto: sólo si el `ServerSocket` se crea con éxito se asume que el puerto está libre.
 - **Lógica del juego:**
	 - Comunicación por `BufferedReader`/`PrintWriter` con cada cliente.
	 - Métodos importantes:
		 - `pedirJugadaServidor(Scanner)`: solicita jugada al usuario-host por consola.
		 - `pedirJugadaCliente(BufferedReader, PrintWriter, boolean, String)`: solicita jugada al cliente; si está eliminado devuelve -1.
		 - `determinarGanador(int a, int b)`: compara dos jugadas (1/2/3) y devuelve 1/0/-1.
		 - `procesarRonda*` (varios): lógica para tres jugadores, parejas contra tercero, 1vs1, actualizando puntuaciones.
		 - `comprobarEliminacion(...)` y `comprobarGanador(...)`: control de eliminaciones y fin de partida.

**Por qué se eligió el rango de puertos 1024–49151**
 - Los puertos por debajo de 1024 son puertos conocidos/reservados (privilegiados); evitamos requerir privilegios de administrador.
 - El rango 1024–49151 corresponde a los puertos "Registered" (no bien conocidos) y es apropiado para servicios que no usan puertos concretos reservados. Los puertos efímeros/dinámicos suelen estar en 49152–65535 según RFC 6335, por eso se eligió 49151 como límite superior para evitar interferir con la asignación dinámica del sistema.
 - En resumen: 1024–49151 ofrece equilibrio entre evitar puertos privilegiados y no interferir con la asignación efímera del SO.

**Por qué no se usa un puerto fijo**
 - Evita conflictos con otros servicios que puedan estar escuchando en el equipo del usuario.
 - Hace la ejecución más robusta en entornos de desarrollo donde muchos servicios pueden ocupar puertos estáticos.

**Notas sobre la implementación de `seleccionarPuertoLibre()`**
 - Se usa `netstat -an` porque la petición original pedía consultar `netstat` para ver puertos ocupados.
 - La regex captura el último número tras `:` en cada línea y filtra aquellas con `LISTENING` (Windows). Esto cubre salidas típicas como:

```
	TCP    0.0.0.0:135            0.0.0.0:0              LISTENING
	TCP    [::]:445               [::]:0                 LISTENING
```

 - Limitaciones: la salida y formato de `netstat` es dependiente del SO; la implementación actual está pensada para Windows (`LISTENING`). En otros sistemas (Linux/macOS) `netstat` o `ss` cambian formato y keywords.

**Ejecución y prueba**
 - Compilar:
 ```bash
 mvn -q package
 ```
 - Ejecutar el servidor (tras compilar):
 ```bash
 java -cp target/classes org.example.Servidor
 ```
 - Al arrancar, el servidor imprimirá la IP local y el puerto seleccionado, por ejemplo:
 ```
 Servidor escuchando en:
 IP: 192.168.1.42
 Puerto: 34567
 ```
 - Desde otra máquina o la misma, se puede conectar con `telnet` (si está instalado):
 ```bash
 telnet 192.168.1.42 34567
 ```

**Capturas / salida de `netstat -an` y `telnet`**
 - No es necesaria una captura externa: el programa ejecuta `netstat -an` internamente y muestra/usa su resultado para tomar la decisión. Si aún así quieres ver la salida manualmente, ejecuta en PowerShell:
 ```powershell
 netstat -an | findstr LISTENING
 ```
 - Ejemplo de salida `telnet` (texto, no imagen): si `telnet` conecta correctamente se verá algo parecido a:
 ```
 Conectado a 192.168.1.42.
 Escape character is '^]'.
 ```

**Comportamiento esperado y consideraciones**
 - Si un cliente no responde o envía datos malformados, el servidor lanzará excepciones que actualmente se capturan en el `try/catch` de `main` y terminan mostrando el mensaje de error. Se podría mejorar añadiendo manejo fino de excepciones por cliente para mantener el servidor activo ante fallos parciales.
 - En entornos multiusuario/productivos se recomienda asignar un puerto fijo y documentado, protección por firewall, y autenticación de clientes.

**Resumen de responsabilidades por archivo**
 - `Servidor.java`: toda la lógica del juego, comunicación de sockets y selección de puerto.
 - `App.java`: archivo de ejemplo/placeholder; no necesario para ejecutar el juego.

Fecha: 2026-01-31
