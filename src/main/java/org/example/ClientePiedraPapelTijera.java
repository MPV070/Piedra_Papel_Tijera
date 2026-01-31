package org.example;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

// Cliente del juego Piedra-Papel-Tijeras.
// Este programa se conecta al servidor por TCP y juega contra él.
// El usuario elige su jugada y el servidor responde con la suya.
public class ClientePiedraPapelTijera {

    // Aquí pongo la IP del servidor. Esta IP la tiene que dar la persona que ejecuta el servidor.
    // Si estuviéramos en el mismo PC sería "localhost", pero como es en red, pongo su IPv4.
    private static final String IP_SERVIDOR = "192.168.0.26";

    // Puerto que usa el servidor. Tiene que ser el mismo en ambos programas.
    private static final int PUERTO_SERVIDOR = 5000;

    public static void main(String[] args) {
        Socket socket = null;
        DataInputStream entrada = null;
        DataOutputStream salida = null;
        Scanner teclado = new Scanner(System.in);

        try {
            // Intento conectarme al servidor usando su IP y puerto.
            System.out.println("Conectando con el servidor...");
            socket = new Socket(IP_SERVIDOR, PUERTO_SERVIDOR);
            System.out.println("Conexión establecida correctamente.");

            // Creo los flujos para enviar y recibir datos.
            entrada = new DataInputStream(socket.getInputStream());
            salida = new DataOutputStream(socket.getOutputStream());

            int puntosCliente = 0;
            int puntosServidor = 0;

            // Mientras ninguno llegue a 3 puntos, seguimos jugando.
            while (puntosCliente < 3 && puntosServidor < 3) {

                // Pido al usuario que elija piedra, papel o tijeras.
                int jugadaCliente = pedirJugadaUsuario(teclado);

                // Envío la jugada al servidor.
                salida.writeInt(jugadaCliente);
                salida.flush();

                // Recibo la jugada del servidor.
                int jugadaServidor = entrada.readInt();
                System.out.println("El servidor ha elegido: " + convertirJugadaTexto(jugadaServidor));

                // Comparo las jugadas para ver quién gana la ronda.
                int resultado = calcularResultado(jugadaCliente, jugadaServidor);

                if (resultado == 1) {
                    puntosCliente++;
                    System.out.println("Has ganado esta ronda.");
                } else if (resultado == -1) {
                    puntosServidor++;
                    System.out.println("Has perdido esta ronda.");
                } else {
                    System.out.println("Empate.");
                }

                // Muestro el marcador para que se vea cómo va la partida.
                System.out.println("Marcador -> Tú: " + puntosCliente + " | Servidor: " + puntosServidor);
                System.out.println("-------------------------------------------");
            }

            // Cuando alguien llega a 3 puntos, mostramos el ganador.
            if (puntosCliente == 3) {
                System.out.println("¡Has ganado la partida!");
            } else {
                System.out.println("El servidor ha ganado la partida.");
            }

            // Cierro la conexión cuando termina el juego.
            System.out.println("Cerrando conexión...");
            socket.close();
            System.out.println("Conexión cerrada.");

        } catch (IOException e) {
            // Si algo falla al conectar o enviar/recibir datos, lo muestro.
            System.out.println("Error al conectar con el servidor: " + e.getMessage());
        } finally {
            // Cierro todo por si acaso.
            try {
                if (entrada != null) entrada.close();
                if (salida != null) salida.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {}
            teclado.close();
        }
    }

    // Esta función pide al usuario una jugada válida (1, 2 o 3).
    private static int pedirJugadaUsuario(Scanner teclado) {
        int jugada = 0;
        boolean valida = false;

        while (!valida) {
            System.out.println("Elige tu jugada:");
            System.out.println("1 = Piedra");
            System.out.println("2 = Papel");
            System.out.println("3 = Tijeras");
            System.out.print("Introduce un número (1-3): ");

            if (teclado.hasNextInt()) {
                jugada = teclado.nextInt();
                if (jugada >= 1 && jugada <= 3) {
                    valida = true;
                } else {
                    System.out.println("Ese número no vale, tiene que ser 1, 2 o 3.");
                }
            } else {
                System.out.println("Eso no es un número. Inténtalo otra vez.");
                teclado.next(); // Limpio la entrada incorrecta.
            }
        }

        System.out.println("Has elegido: " + convertirJugadaTexto(jugada));
        return jugada;
    }

    // Convierte el número de jugada a texto para mostrarlo más claro.
    private static String convertirJugadaTexto(int jugada) {
        switch (jugada) {
            case 1: return "Piedra";
            case 2: return "Papel";
            case 3: return "Tijeras";
            default: return "Desconocido";
        }
    }

    // Aquí comparo las jugadas y devuelvo quién gana.
    // 1 = gana el cliente, -1 = gana el servidor, 0 = empate.
    private static int calcularResultado(int cliente, int servidor) {
        if (cliente == servidor) return 0;

        if ((cliente == 1 && servidor == 3) ||
            (cliente == 2 && servidor == 1) ||
            (cliente == 3 && servidor == 2)) {
            return 1;
        } else {
            return -1;
        }
    }
}
