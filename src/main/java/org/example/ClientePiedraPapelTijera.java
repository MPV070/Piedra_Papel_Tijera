package org.example;
// Importamos las clases necesarias para trabajar con sockets, entrada/salida y leer por teclado.
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

// Este programa es el CLIENTE del juego "Piedra, Papel o Tijeras".
// Se conecta a un servidor por TCP, juega varias rondas y muestra quién gana.
public class ClientePiedraPapelTijera {

    // IP del servidor. Si jugamos en el mismo ordenador, usamos "localhost".
    // Si jugamos en red, hay que poner la IP del otro ordenador (se ve con ipconfig).
    private static final String IP_SERVIDOR = "localhost";

    // Puerto que usa el servidor. Debe estar libre y entre 1024 y 49151.
    private static final int PUERTO_SERVIDOR = 5000;

    public static void main(String[] args) {
        Socket socket = null;
        DataInputStream entrada = null;
        DataOutputStream salida = null;
        Scanner teclado = new Scanner(System.in);

        try {
            // Intentamos conectar con el servidor usando IP y puerto.
            System.out.println("Conectando con el servidor...");
            socket = new Socket(IP_SERVIDOR, PUERTO_SERVIDOR);
            System.out.println("Conexión establecida.");

            // Creamos los canales para enviar y recibir datos.
            entrada = new DataInputStream(socket.getInputStream());
            salida = new DataOutputStream(socket.getOutputStream());

            int puntosCliente = 0;
            int puntosServidor = 0;

            // Bucle del juego: seguimos jugando hasta que alguien llegue a 3 puntos.
            while (puntosCliente < 3 && puntosServidor < 3) {

                // Pedimos al usuario que elija piedra, papel o tijeras.
                int jugadaCliente = pedirJugadaUsuario(teclado);

                // Enviamos la jugada al servidor.
                salida.writeInt(jugadaCliente);
                salida.flush();

                // Recibimos la jugada del servidor.
                int jugadaServidor = entrada.readInt();
                System.out.println("El servidor ha elegido: " + convertirJugadaTexto(jugadaServidor));

                // Comparamos las jugadas y vemos quién gana esta ronda.
                int resultado = calcularResultado(jugadaCliente, jugadaServidor);

                if (resultado == 1) {
                    puntosCliente++;
                    System.out.println("¡Ganaste esta ronda!");
                } else if (resultado == -1) {
                    puntosServidor++;
                    System.out.println("Perdiste esta ronda.");
                } else {
                    System.out.println("Empate.");
                }

                // Mostramos el marcador actual.
                System.out.println("Marcador -> Tú: " + puntosCliente + " | Servidor: " + puntosServidor);
                System.out.println("-------------------------------------------");
            }

            // Mensaje final según quién haya ganado.
            if (puntosCliente == 3) {
                System.out.println("¡Has ganado la partida!");
            } else {
                System.out.println("El servidor ha ganado la partida.");
            }

            // Cerramos la conexión con el servidor.
            System.out.println("Cerrando conexión...");
            socket.close();
            System.out.println("Conexión cerrada.");

        } catch (IOException e) {
            System.out.println("Error al conectar con el servidor: " + e.getMessage());
        } finally {
            // Cerramos todo por si acaso.
            try {
                if (entrada != null) entrada.close();
                if (salida != null) salida.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                // No hacemos nada si falla el cierre.
            }
            teclado.close();
        }
    }

    // Esta función pide al usuario que elija una jugada válida (1, 2 o 3).
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
                    System.out.println("Número incorrecto. Debe ser 1, 2 o 3.");
                }
            } else {
                System.out.println("Eso no es un número. Intenta otra vez.");
                teclado.next(); // Limpiamos la entrada incorrecta.
            }
        }

        System.out.println("Has elegido: " + convertirJugadaTexto(jugada));
        return jugada;
    }

    // Convierte el número de jugada a texto para mostrarlo por pantalla.
    private static String convertirJugadaTexto(int jugada) {
        switch (jugada) {
            case 1: return "Piedra";
            case 2: return "Papel";
            case 3: return "Tijeras";
            default: return "Desconocido";
        }
    }

    // Compara las dos jugadas y devuelve el resultado:
    // 1 si gana el cliente, -1 si gana el servidor, 0 si empate.
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
