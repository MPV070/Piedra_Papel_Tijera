package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Random;
import java.io.IOException;

public class Servidor {

    public static void main(String[] args) {

        Scanner teclado = new Scanner(System.in);

        try {
            // ==========================================================
            // CONFIGURACIÓN INICIAL DEL SERVIDOR
            // ==========================================================
            // EL SERVIDOR ELIGE AUTOMÁTICAMENTE UN PUERTO LIBRE (1024-49151)
            System.out.println("Buscando un puerto libre entre 1024 y 49151...");
            int puerto = seleccionarPuertoLibre();
            // SE CREA EL SERVERSOCKET EN EL PUERTO SELECCIONADO
            // Este objeto escuchará las peticiones de conexión de los clientes
            ServerSocket servidor = new ServerSocket(puerto);

            // SE OBTIENE LA IP LOCAL DEL SERVIDOR PARA MOSTRARLA A LOS CLIENTES
            String ipLocal = java.net.InetAddress.getLocalHost().getHostAddress();

            // SE MUESTRA LA INFORMACIÓN DE CONEXIÓN
            System.out.println("Servidor escuchando en:");
            System.out.println("IP: " + ipLocal);
            System.out.println("Puerto: " + puerto);

            boolean continuarServidor = true;

            // ==========================================================
            // BUCLE PRINCIPAL DEL SERVIDOR
            // ==========================================================
            // Este bucle permite jugar múltiples partidas sin reiniciar la aplicación.
            while (continuarServidor) {
                // MENÚ DE SELECCIÓN DE MODO DE JUEGO
                System.out.println("\n------------------------------------------------");
                System.out.println(" MENÚ DEL SERVIDOR");
                System.out.println("------------------------------------------------");
                System.out.println("1. Jugador 1 vs Server (1 Cliente)");
                System.out.println("2. Jugadores 1 y 2 vs Server (2 Clientes)");
                System.out.println("0. Salir / Cerrar Servidor");
                System.out.print("Seleccione una opción: ");

                String opcion = teclado.nextLine().trim();
                boolean modoUnicoCliente = true;

                if (opcion.equals("0")) {
                    System.out.println("Cerrando servidor...");
                    continuarServidor = false;
                    continue; // Vuelve al inicio del while y termina porque continuarServidor es false
                } else if (opcion.equals("2")) {
                    modoUnicoCliente = false; // Se esperan 2 jugadores
                    System.out.println("Modo seleccionado: 2 Jugadores vs Server");
                } else {
                    // Por defecto o si es "1"
                    modoUnicoCliente = true; // Solo 1 jugador contra el servidor
                    System.out.println("Modo seleccionado: 1 Jugador vs Server");
                }

                // ==========================================================
                // FASE DE CONEXIÓN DE JUGADORES
                // ==========================================================
                System.out.println("Esperando a Player1...");
                // SE ACEPTA LA CONEXIÓN DEL PRIMER CLIENTE (PLAYER1)
                // El metodo .accept() bloquea la ejecución hasta que llega un cliente.
                Socket socketPlayer1 = servidor.accept();
                System.out.println("Player1 conectado desde la IP: " + socketPlayer1.getInetAddress());

                Socket socketPlayer2 = null;
                // Si el modo es de 2 jugadores, esperamos al segundo
                if (!modoUnicoCliente) {
                    System.out.println("Esperando a Player2...");
                    socketPlayer2 = servidor.accept();
                    System.out.println("Player2 conectado desde la IP: " + socketPlayer2.getInetAddress());
                }

                // ==========================================================
                // INICIALIZACIÓN DE CANALES DE COMUNICACIÓN (STREAMS)
                // ==========================================================
                // CANALES DE COMUNICACIÓN CON PLAYER1
                BufferedReader lectorPlayer1 = new BufferedReader(
                        new InputStreamReader(socketPlayer1.getInputStream())); // Para leer mensajes de P1
                PrintWriter escritorPlayer1 = new PrintWriter(socketPlayer1.getOutputStream(), true); // Para enviar
                                                                                                      // mensajes a P1

                // CANALES DE COMUNICACIÓN CON PLAYER2 (si existe)
                BufferedReader lectorPlayer2 = null;
                PrintWriter escritorPlayer2 = null;
                if (!modoUnicoCliente) {
                    lectorPlayer2 = new BufferedReader(
                            new InputStreamReader(socketPlayer2.getInputStream())); // Para leer mensajes de P2
                    escritorPlayer2 = new PrintWriter(socketPlayer2.getOutputStream(), true); // Para enviar mensajes a
                                                                                              // P2
                }

                // ==========================================================
                // INICIALIZACIÓN DEL JUEGO
                // ==========================================================
                // PUNTUACIONES INICIALES
                int gameHostPuntos = 0;
                int player1Puntos = 0;
                int player2Puntos = 0;

                // RONDAS Y DELTAS (deltaPuntos)
                // 'deltaPuntos' representa la variación de puntos en la última ronda.
                // Es decir: Puntos_Actuales - Puntos_Anteriores.
                // Si ganas, deltaPuntos será +1. Si pierdes, -1. Si empatas, 0.
                // Esto sirve para mostrarle al usuario qué pasó en la jugada reciente.
                int rondas = 0;
                int deltaPuntosHost = 0;
                int deltaPuntosPlayer1 = 0;
                int deltaPuntosPlayer2 = 0;

                // ESTADOS DE ELIMINACIÓN (cuando un jugador llega a una puntuación muy baja)
                boolean eliminatedPlayer1 = false;
                boolean eliminatedPlayer2 = false;

                // CONTROL DEL FIN DE LA PARTIDA ACTUAL
                boolean partidaTerminada = false;

                // ==========================================================
                // BUCLE DE LA PARTIDA (RONDAS)
                // ==========================================================
                while (!partidaTerminada) {

                    // 1. COMPROBAR SI YA HAY GANADOR GLOBAL
                    String ganador = comprobarGanador(
                            gameHostPuntos, player1Puntos, player2Puntos,
                            eliminatedPlayer1, eliminatedPlayer2);

                    if (ganador != null) {
                        anunciarGanador(ganador);
                        partidaTerminada = true; // Salimos del bucle de partida
                        continue;
                    }

                    // 2. OBTENER JUGADAS DE TODOS LOS PARTICIPANTES ACTIVOS

                    // EL SERVIDOR (GAMEHOST) ELIGE SU JUGADA
                    int gameHostMove = pedirJugadaServidor(teclado);

                    // PLAYER1 ELIGE SU JUGADA (SI NO ESTÁ ELIMINADO)
                    int player1Move = -1;
                    if (!eliminatedPlayer1) {
                        try {
                            player1Move = pedirJugadaCliente(
                                    lectorPlayer1, escritorPlayer1, eliminatedPlayer1, "Player1");
                        } catch (Exception e) {
                            // Gestión de desconexión abrupta de P1
                            System.out.println("Player1 disconnected or sent invalid data: " + e.getMessage());
                            eliminatedPlayer1 = true;
                            player1Move = -1;
                            try {
                                escritorPlayer1.println("ELIMINADO");
                            } catch (Exception ignored) {
                            }
                            try {
                                lectorPlayer1.close();
                            } catch (Exception ignored) {
                            }
                            try {
                                escritorPlayer1.close();
                            } catch (Exception ignored) {
                            }
                            try {
                                socketPlayer1.close();
                            } catch (Exception ignored) {
                            }
                            if (!modoUnicoCliente) {
                                try {
                                    escritorPlayer2.println("OPPONENT_DISCONNECTED");
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }

                    // PLAYER2 ELIGE SU JUGADA (SI NO ESTÁ ELIMINADO Y MODO 3 JUGADORES)
                    int player2Move = -1;
                    if (!modoUnicoCliente && !eliminatedPlayer2) {
                        try {
                            player2Move = pedirJugadaCliente(
                                    lectorPlayer2, escritorPlayer2, eliminatedPlayer2, "Player2");
                        } catch (Exception e) {
                            // Gestión de desconexión abrupta de P2
                            System.out.println("Player2 disconnected or sent invalid data: " + e.getMessage());
                            eliminatedPlayer2 = true;
                            try {
                                escritorPlayer2.println("ELIMINADO");
                            } catch (Exception ignored) {
                            }
                            try {
                                lectorPlayer2.close();
                            } catch (Exception ignored) {
                            }
                            try {
                                escritorPlayer2.close();
                            } catch (Exception ignored) {
                            }
                            if (socketPlayer2 != null) {
                                try {
                                    socketPlayer2.close();
                                } catch (Exception ignored) {
                                }
                            }
                            try {
                                if (!eliminatedPlayer1)
                                    escritorPlayer1.println("OPPONENT_DISCONNECTED");
                            } catch (Exception ignored) {
                            }
                        }
                    }

                    // 3. PROCESAR LA RONDA (CALCULAR GANADORES LOCALES Y ACTUALIZAR PUNTOS)
                    // Esta función devuelve un array con los NUEVOS puntos y los DELTAS.
                    // Los deltas nos dicen cuánto cambiaron los puntos en esta ronda específica.
                    int[] resultadoRonda = procesarRonda(
                            gameHostMove, player1Move, player2Move,
                            gameHostPuntos, player1Puntos, player2Puntos,
                            eliminatedPlayer1, eliminatedPlayer2, modoUnicoCliente);

                    // Actualizar variables con los resultados de la ronda
                    // Índices 0, 1, 2 -> Nuevas puntuaciones totales
                    gameHostPuntos = resultadoRonda[0];
                    player1Puntos = resultadoRonda[1];
                    player2Puntos = resultadoRonda[2];

                    // Índices 3, 4, 5 -> Deltas (Variación de puntos en esta ronda)
                    // Esto permite mostrar "+1", "-1" o "0" en el marcador
                    deltaPuntosHost = resultadoRonda[3];
                    deltaPuntosPlayer1 = resultadoRonda[4];
                    deltaPuntosPlayer2 = resultadoRonda[5];

                    // Incrementar contador de rondas
                    rondas++;

                    // 4. VERIFICAR ELIMINACIONES PUNTUALES
                    eliminatedPlayer1 = comprobarEliminacion(
                            player1Puntos, eliminatedPlayer1, escritorPlayer1, "Player1");
                    if (!modoUnicoCliente) {
                        eliminatedPlayer2 = comprobarEliminacion(
                                player2Puntos, eliminatedPlayer2, escritorPlayer2, "Player2");
                    }

                    // 5. MOSTRAR EL MARCADOR ACTUAL AL SERVIDOR Y A LOS CLIENTES
                    mostrarMarcador(
                            gameHostPuntos, player1Puntos, player2Puntos,
                            eliminatedPlayer1, eliminatedPlayer2,
                            deltaPuntosHost, deltaPuntosPlayer1, deltaPuntosPlayer2,
                            rondas, escritorPlayer1, escritorPlayer2);
                }

                // ==========================================================
                // FIN DE LA PARTIDA Y LIMPIEZA
                // ==========================================================
                // CIERRE DE CONEXIONES DE LOS JUGADORES (PERO NO DEL SERVIDOR PRINCIPAL)
                System.out.println("Fin de la partida. Cerrando conexiones con clientes...");
                try {
                    socketPlayer1.close();
                } catch (Exception e) {
                }
                if (socketPlayer2 != null) {
                    try {
                        socketPlayer2.close();
                    } catch (Exception e) {
                    }
                }
            }

            // ==========================================================
            // CIERRE DEL SERVIDOR
            // ==========================================================
            servidor.close();
            System.out.println("Servidor detenido completamente.");

        } catch (Exception e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        }
    }

    // Selecciona un puerto TCP libre entre 1024 y 49151 consultando netstat -an
    private static int seleccionarPuertoLibre() throws Exception {
        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "netstat -an");
        pb.redirectErrorStream(true);
        Process p = pb.start();

        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        Set<Integer> usados = new HashSet<>();
        Pattern patron = Pattern.compile(".*:(\\d+)\\s+.*LISTENING.*", Pattern.CASE_INSENSITIVE);

        while ((line = br.readLine()) != null) {
            Matcher m = patron.matcher(line);
            if (m.find()) {
                try {
                    int puerto = Integer.parseInt(m.group(1));
                    usados.add(puerto);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        try {
            p.waitFor();
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        Random rnd = new Random();
        int min = 1024;
        int max = 49151;

        for (int intento = 0; intento < 10000; intento++) {
            int puerto = min + rnd.nextInt(max - min + 1);
            if (usados.contains(puerto))
                continue;
            // Intentar enlazar rápidamente para evitar condiciones de carrera
            try (ServerSocket ss = new ServerSocket(puerto)) {
                // Si aquí no lanza excepción, el puerto está libre: ss se cierra
                // automáticamente
                return puerto;
            } catch (IOException e) {
                // No se pudo enlazar, probar otro
            }
        }

        throw new RuntimeException("No se encontró puerto libre en el rango 1024-49151.");
    }

    // DETERMINA EL GANADOR ENTRE DOS JUGADAS (1 = GAMEHOST GANA, -1 = GAMEHOST
    // PIERDE, 0 = EMPATE)
    private static int determinarGanador(int a, int b) {
        if (a == b)
            return 0;
        if ((a == 1 && b == 3) || (a == 2 && b == 1) || (a == 3 && b == 2))
            return 1;
        return -1;
    }

    // AJUSTA PUNTOS PARA UN JUGADOR ESPECÍFICO DENTRO DEL ARRAY [gameHost, player1,
    // player2]
    private static void cambiarPuntos(int[] puntos, String jugador, int deltaPuntos) {
        if ("GAMEHOST".equals(jugador))
            puntos[0] += deltaPuntos;
        else if ("PLAYER1".equals(jugador))
            puntos[1] += deltaPuntos;
        else if ("PLAYER2".equals(jugador))
            puntos[2] += deltaPuntos;
    }

    // PROCESA UNA RONDA COMPLETA SEGÚN CUÁNTOS JUGADORES SIGUEN VIVOS
    private static int[] procesarRonda(
            int gameHostMove, int player1Move, int player2Move,
            int gameHostPuntos, int player1Puntos, int player2Puntos,
            boolean eliminatedPlayer1, boolean eliminatedPlayer2,
            boolean modoUnicoCliente) {
        // CONTAR CUÁNTOS JUGADORES SIGUEN VIVOS (GAMEHOST SIEMPRE ESTÁ VIVO)
        // Si player2Move == -1, significa modo 1vs1 (Player2 no existe)
        int vivos = 1;
        if (!eliminatedPlayer1)
            vivos++;
        if (player2Move != -1 && !eliminatedPlayer2)
            vivos++;

        // Procesar y devolver también deltas por ronda
        int[] antes = new int[] { gameHostPuntos, player1Puntos, player2Puntos };
        int[] despues;

        // CASO: TRES JUGADORES VIVOS -> LÓGICA DE 3 JUGADORES
        if (vivos == 3) {
            despues = procesarRonda3Jugadores(
                    gameHostMove, player1Move, player2Move,
                    gameHostPuntos, player1Puntos, player2Puntos);
        }
        // CASO: DOS JUGADORES VIVOS → 1 VS 1
        else if (vivos == 2) {
            if (!eliminatedPlayer1) {
                despues = procesar1vs1(
                        gameHostMove, player1Move,
                        gameHostPuntos, player1Puntos, player2Puntos,
                        "PLAYER1", /* preventSubtract= */ modoUnicoCliente);
            } else {
                despues = procesar1vs1(
                        gameHostMove, player2Move,
                        gameHostPuntos, player1Puntos, player2Puntos,
                        "PLAYER2", /* preventSubtract= */ modoUnicoCliente);
            }
        }
        // CASO: SOLO GAMEHOST VIVO
        else {
            despues = new int[] { gameHostPuntos, player1Puntos, player2Puntos };
        }

        // CÁLCULO DE DELTAPUNTOS (LA CLAVE PARA EXPLICAR QUÉ PASÓ EN LA RONDA)
        // deltaPuntos = Puntos_Después_De_La_Ronda - Puntos_Antes_De_La_Ronda
        int deltaPuntosHost = despues[0] - antes[0];
        int deltaPuntosPlayer1 = despues[1] - antes[1];
        int deltaPuntosPlayer2 = despues[2] - antes[2];

        // Retornamos un array con TODA la información:
        // [0-2]: Puntuaciones Totales Actualizadas
        // [3-5]: Deltas (el cambio ocurrido en esta ronda)
        return new int[] { despues[0], despues[1], despues[2], deltaPuntosHost, deltaPuntosPlayer1,
                deltaPuntosPlayer2 };
    }

    // PROCESA UNA RONDA DE 3 JUGADORES
    private static int[] procesarRonda3Jugadores(
            int gameHostMove, int player1Move, int player2Move,
            int gameHostPuntos, int player1Puntos, int player2Puntos) {
        // CASO 1: LOS TRES ELIGEN LO MISMO → EMPATE
        if (gameHostMove == player1Move && player1Move == player2Move) {
            return new int[] { gameHostPuntos, player1Puntos, player2Puntos };
        }

        // CASO 2: LOS TRES ELIGEN COSAS DIFERENTES → EMPATE
        if (gameHostMove != player1Move &&
                gameHostMove != player2Move &&
                player1Move != player2Move) {
            return new int[] { gameHostPuntos, player1Puntos, player2Puntos };
        }

        // CASO 3A: GAMEHOST Y PLAYER1 IGUALES
        if (gameHostMove == player1Move) {
            return procesarParejaContraJugador(
                    gameHostMove, player2Move,
                    gameHostPuntos, player1Puntos, player2Puntos,
                    "PLAYER1", "PLAYER2");
        }

        // CASO 3B: GAMEHOST Y PLAYER2 IGUALES
        if (gameHostMove == player2Move) {
            return procesarParejaContraJugador(
                    gameHostMove, player1Move,
                    gameHostPuntos, player1Puntos, player2Puntos,
                    "PLAYER2", "PLAYER1");
        }

        // CASO 3C: PLAYER1 Y PLAYER2 IGUALES
        int resultado = determinarGanador(player1Move, gameHostMove);
        if (resultado == 1) {
            player1Puntos++;
            player2Puntos++;
            gameHostPuntos--;
        } else {
            player1Puntos--;
            player2Puntos--;
            gameHostPuntos++;
        }

        return new int[] { gameHostPuntos, player1Puntos, player2Puntos };
    }

    // PROCESA UNA PAREJA DE JUGADORES CONTRA EL TERCERO
    private static int[] procesarParejaContraJugador(
            int parejaMove, int terceroMove,
            int gameHostPuntos, int player1Puntos, int player2Puntos,
            String parejaB, String tercero) {
        // parejaA is always GAMEHOST in current game logic
        final String parejaA = "GAMEHOST";
        int resultado = determinarGanador(parejaMove, terceroMove);
        int[] puntos = new int[] { gameHostPuntos, player1Puntos, player2Puntos };

        if (resultado == 1) {
            // La pareja gana: ambos suman, el tercero resta
            cambiarPuntos(puntos, parejaA, +1);
            cambiarPuntos(puntos, parejaB, +1);
            cambiarPuntos(puntos, tercero, -1);
        } else {
            // El tercero gana: tercero suma, ambos de la pareja restan
            cambiarPuntos(puntos, tercero, +1);
            cambiarPuntos(puntos, parejaA, -1);
            cambiarPuntos(puntos, parejaB, -1);
        }

        return puntos;
    }

    // PROCESA UNA RONDA 1 VS 1 ENTRE GAMEHOST Y UNO DE LOS PLAYERS
    private static int[] procesar1vs1(
            int gameHostMove, int opponentMove,
            int gameHostPuntos, int player1Puntos, int player2Puntos,
            String opponent, boolean preventSubtract) {
        int resultado = determinarGanador(gameHostMove, opponentMove);
        int[] puntos = new int[] { gameHostPuntos, player1Puntos, player2Puntos };

        if (resultado == 1) {
            cambiarPuntos(puntos, "GAMEHOST", +1);
        } else if (resultado == -1) {
            // El oponente gana; sumar punto al oponente
            cambiarPuntos(puntos, opponent, +1);
            // Restar al GameHost sólo si no estamos en modo 1vs1 (preventSubtract == false)
            if (!preventSubtract) {
                cambiarPuntos(puntos, "GAMEHOST", -1);
            }
        }

        return puntos;
    }

    // COMPRUEBA SI HAY GANADOR GLOBAL SEGÚN PUNTOS Y ELIMINACIONES
    private static String comprobarGanador(
            int gameHostPuntos, int player1Puntos, int player2Puntos,
            boolean eliminatedPlayer1, boolean eliminatedPlayer2) {
        // VICTORIA POR PUNTOS
        if (gameHostPuntos >= 3)
            return "GAMEHOST";
        if (player1Puntos >= 3)
            return "PLAYER1";
        if (player2Puntos >= 3)
            return "PLAYER2";

        // VICTORIA POR ELIMINACIÓN
        if (eliminatedPlayer1 && eliminatedPlayer2)
            return "GAMEHOST";
        if (eliminatedPlayer1)
            return "PLAYER2";
        if (eliminatedPlayer2)
            return "PLAYER1";

        return null;
    }

    // ANUNCIA EL GANADOR POR CONSOLA
    private static void anunciarGanador(String ganador) {
        switch (ganador) {
            case "GAMEHOST":
                System.out.println("El servidor (GameHost) ha ganado la partida.");
                break;
            case "PLAYER1":
                System.out.println("Player1 ha ganado la partida.");
                break;
            case "PLAYER2":
                System.out.println("Player2 ha ganado la partida.");
                break;
        }
    }

    // PIDE LA JUGADA AL SERVIDOR (GAMEHOST)
    private static int pedirJugadaServidor(Scanner teclado) {
        int eleccion = 0;
        while (eleccion < 1 || eleccion > 3) {
            System.out.println("\nElige tu jugada:");
            System.out.println("1 = Piedra");
            System.out.println("2 = Papel");
            System.out.println("3 = Tijeras");
            System.out.print("Tu elección: ");
            String linea = teclado.nextLine().trim();
            try {
                eleccion = Integer.parseInt(linea);
                if (eleccion < 1 || eleccion > 3) {
                    System.out.println("Número fuera de rango. Introduce 1, 2 o 3.");
                }
            } catch (NumberFormatException e) {
                System.out.println("no se ha introducido un número, por favor introduce un número entre el 1 y el 3");
            }
        }
        return eleccion;
    }

    // PIDE LA JUGADA A UN CLIENTE (PLAYER1 O PLAYER2)
    private static int pedirJugadaCliente(
            BufferedReader lector, PrintWriter escritor,
            boolean eliminado, String nombre) throws Exception {
        if (eliminado)
            return -1;

        while (true) {
            // Indicar al cliente que debe jugar
            escritor.println("JUEGA");
            String linea = lector.readLine();
            if (linea == null) {
                // Cliente desconectado
                throw new IOException("Cliente desconectado");
            }
            linea = linea.trim();
            try {
                int jugada = Integer.parseInt(linea);
                if (jugada < 1 || jugada > 3) {
                    escritor.println("ERROR: número fuera de rango, por favor introduce 1, 2 o 3");
                    continue;
                }
                System.out.println(nombre + " eligió: " + jugada);
                return jugada;
            } catch (NumberFormatException nfe) {
                escritor.println(
                        "ERROR: no se ha introducido un número, por favor introduce un número entre el 1 y el 3");
                // pedir de nuevo
            }
        }
    }

    // COMPRUEBA SI UN JUGADOR HA SIDO ELIMINADO POR LLEGAR A -3 PUNTOS
    private static boolean comprobarEliminacion(
            int puntos, boolean eliminado, PrintWriter escritor, String nombre) {
        if (!eliminado && puntos <= -3) {
            System.out.println(nombre + " ha sido eliminado.");
            escritor.println("ELIMINADO");
            return true;
        }
        return eliminado;
    }

    // MUESTRA EL MARCADOR ACTUAL DE LA PARTIDA Y LO ENVÍA A LOS CLIENTES
    private static void mostrarMarcador(
            int gameHostPuntos, int player1Puntos, int player2Puntos,
            boolean eliminatedPlayer1, boolean eliminatedPlayer2,
            int deltaPuntosHost, int deltaPuntosPlayer1, int deltaPuntosPlayer2,
            int rondas, PrintWriter escritorPlayer1, PrintWriter escritorPlayer2) {

        System.out.println("\n--- MARCADOR (Ronda " + rondas + ") ---");
        System.out.println("Host: " + gameHostPuntos + " (" + formatDeltaPuntos(deltaPuntosHost) + ")");
        System.out.println("Player1: " + player1Puntos + " (" + formatDeltaPuntos(deltaPuntosPlayer1) + ")"
                + (eliminatedPlayer1 ? " (ELIMINADO)" : ""));
        System.out.println("Player2: " + player2Puntos + " (" + formatDeltaPuntos(deltaPuntosPlayer2) + ")"
                + (eliminatedPlayer2 ? " (ELIMINADO)" : ""));
        System.out.println("----------------\n");

        // CONSTRUIR EL MENSAJE DE PROTOCOLO PARA LOS CLIENTES
        // Se envía toda la información el estado del juego, incluyendo los deltas.
        // El cliente analizará los deltas para saber si ganó, perdió o empató la ronda.
        // Formato: MARCADOR:host,p1,p2,elim1,elim2,dH,dP1,dP2,ronda
        String mensaje = String.format("MARCADOR:%d,%d,%d,%b,%b,%d,%d,%d,%d",
                gameHostPuntos, player1Puntos, player2Puntos,
                eliminatedPlayer1, eliminatedPlayer2,
                deltaPuntosHost, deltaPuntosPlayer1, deltaPuntosPlayer2,
                rondas);

        // ENVIAR A PLAYER 1
        if (escritorPlayer1 != null) {
            escritorPlayer1.println(mensaje);
        }

        // ENVIAR A PLAYER 2
        if (escritorPlayer2 != null) {
            escritorPlayer2.println(mensaje);
        }
    }

    private static String formatDeltaPuntos(int dp) {
        if (dp > 0)
            return "+" + dp;
        if (dp < 0)
            return String.valueOf(dp);
        return "+0";
    }
}