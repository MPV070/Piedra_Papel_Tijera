package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Cliente2 {

    public static void main(String[] args) {

        Scanner teclado = new Scanner(System.in);

        System.out.println("=== CLIENTE 2 (Player2) ===");

        // ---------------------------------------------------------
        // Pedimos la IP del servidor con validación sencilla
        // ---------------------------------------------------------
        String ip;
        boolean ipValida;

        do {
            System.out.print("Introduce la IP del servidor: ");
            ip = teclado.nextLine().trim();
            ipValida = true;

            try {
                // Si el usuario mete solo números, claramente no es una IP válida
                if (ip.matches("\\d+")) {
                    throw new IllegalArgumentException();
                }
            } catch (Exception e) {
                System.out.println("IP no válida. Escríbela correctamente (ej: 192.168.1.10).");
                ipValida = false;
            }

        } while (!ipValida);

        // ---------------------------------------------------------
        // Pedimos el puerto con control de errores
        // ---------------------------------------------------------
        int puerto = 0;
        boolean puertoValido;

        do {
            System.out.print("Introduce el puerto del servidor: ");
            String entrada = teclado.nextLine().trim();
            puertoValido = true;

            try {
                puerto = Integer.parseInt(entrada);
                if (puerto < 1 || puerto > 65535) {
                    throw new NumberFormatException();
                }
            } catch (Exception e) {
                System.out.println("Puerto no válido. Introduce un número entre 1 y 65535.");
                puertoValido = false;
            }

        } while (!puertoValido);

        // ---------------------------------------------------------
        // Intentamos conectar con el servidor
        // ---------------------------------------------------------
        try (Socket socket = new Socket(ip, puerto)) {

            System.out.println("Conectado al servidor.");

            BufferedReader lector = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter escritor = new PrintWriter(socket.getOutputStream(), true);

            boolean seguir = true;

            while (seguir) {

                String mensaje = lector.readLine();

                if (mensaje == null) {
                    System.out.println("El servidor se ha desconectado.");
                    break;
                }

                switch (mensaje) {

                    case "JUEGA":
                        int jugada = pedirJugada(teclado);
                        escritor.println(jugada);
                        break;

                    case "ELIMINADO":
                        System.out.println("Has sido eliminado. Espera al final de la partida...");
                        seguir = esperarFinPartida(lector);
                        break;

                    case "OPPONENT_DISCONNECTED":
                        System.out.println("El otro jugador se ha desconectado.");
                        break;

                    default:
                        // Si el servidor manda el marcador, lo procesamos
                        if (mensaje.startsWith("MARCADOR:")) {
                            procesarMarcador(mensaje);
                        } else {
                            System.out.println("Servidor: " + mensaje);
                        }
                        break;
                }
            }

        } catch (Exception e) {
            System.out.println("Error en el cliente: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------
    // Pide la jugada al usuario y controla entradas incorrectas
    // ---------------------------------------------------------
    private static int pedirJugada(Scanner teclado) {
        int eleccion = 0;
        boolean valida;

        do {
            System.out.println("\nElige tu jugada:");
            System.out.println("1 = Piedra");
            System.out.println("2 = Papel");
            System.out.println("3 = Tijeras");
            System.out.print("Tu elección: ");

            String entrada = teclado.nextLine().trim();
            valida = true;

            try {
                eleccion = Integer.parseInt(entrada);
                if (eleccion < 1 || eleccion > 3) {
                    throw new NumberFormatException();
                }
            } catch (Exception e) {
                System.out.println("Introduce un número válido (1, 2 o 3).");
                valida = false;
            }

        } while (!valida);

        return eleccion;
    }

    // ---------------------------------------------------------
    // Si estás eliminado, simplemente esperas al final de partida
    // ---------------------------------------------------------
    private static boolean esperarFinPartida(BufferedReader lector) {
        try {
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (linea.startsWith("MARCADOR:")) {
                    procesarMarcador(linea);
                }
                if (linea.contains("GANADOR") || linea.contains("FIN")) {
                    System.out.println(linea);
                    return false;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    // ---------------------------------------------------------
    // Procesa el marcador enviado por el servidor
    // ---------------------------------------------------------
    private static void procesarMarcador(String linea) {
        try {
            String datos = linea.substring("MARCADOR:".length());
            String[] p = datos.split(",");

            int host = Integer.parseInt(p[0]);
            int p1 = Integer.parseInt(p[1]);
            int p2 = Integer.parseInt(p[2]);
            boolean elim1 = Boolean.parseBoolean(p[3]);
            boolean elim2 = Boolean.parseBoolean(p[4]);
            int dH = Integer.parseInt(p[5]);
            int dP1 = Integer.parseInt(p[6]);
            int dP2 = Integer.parseInt(p[7]);
            int ronda = Integer.parseInt(p[8]);

            mostrarMarcador(host, p1, p2, elim1, elim2, dH, dP1, dP2, ronda);

        } catch (Exception e) {
            System.out.println("Marcador recibido con formato inesperado: " + linea);
        }
    }

    // ---------------------------------------------------------
    // Marcador (misma función que el servidor)
    // ---------------------------------------------------------
    private static void mostrarMarcador(
            int gameHostPuntos, int player1Puntos, int player2Puntos,
            boolean eliminatedPlayer1, boolean eliminatedPlayer2,
            int deltaHost, int deltaPlayer1, int deltaPlayer2,
            int rondas) {

        System.out.println("\n--- MARCADOR (Ronda " + rondas + ") ---");
        System.out.println("Host: " + gameHostPuntos + " (" + formatDelta(deltaHost) + ")");
        System.out.println("Player1: " + player1Puntos + " (" + formatDelta(deltaPlayer1) + ")"
                + (eliminatedPlayer1 ? " (ELIMINADO)" : ""));
        System.out.println("Player2: " + player2Puntos + " (" + formatDelta(deltaPlayer2) + ")"
                + (eliminatedPlayer2 ? " (ELIMINADO)" : ""));
        System.out.println("----------------\n");
    }

    private static String formatDelta(int d) {
        if (d > 0) return "+" + d;
        if (d < 0) return String.valueOf(d);
        return "+0";
    }
}
