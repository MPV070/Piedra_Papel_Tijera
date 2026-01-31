package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

// Este programa se conecta al servidor usando la IP y el puerto que él mismo muestra al arrancar.
// Una vez conectado, el cliente envía su jugada y recibe la del servidor hasta que la partida termina.
public class ClientePiedraPapelTijera {

    public static void main(String[] args) {

        Scanner teclado = new Scanner(System.in);

        try {
            // Antes de conectarme, pido al usuario que escriba la IP y el puerto que muestra el servidor.
            // Esto lo hago porque el servidor elige un puerto aleatorio cada vez.
            System.out.print("Introduce la IP del servidor: ");
            String ipServidor = teclado.nextLine();

            System.out.print("Introduce el puerto del servidor: ");
            int puertoServidor = teclado.nextInt();
            teclado.nextLine();  

            // Intento conectarme al servidor con los datos que me ha dado el usuario.
            System.out.println("Conectando con el servidor...");
            Socket socket = new Socket(ipServidor, puertoServidor);
            System.out.println("Conexión establecida correctamente.");

            // Creo los canales para enviar y recibir mensajes.
            // Uso BufferedReader y PrintWriter porque el servidor también usa texto.
            BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter salida = new PrintWriter(socket.getOutputStream(), true);

            // Este bucle se mantiene mientras el servidor siga enviando mensajes.
            // Si el servidor cierra la conexión, entrada.readLine() devolverá null.
            while (true) {

                // El servidor siempre envía primero un mensaje indicando qué hacer.
                String mensajeServidor = entrada.readLine();

                // Si el servidor cierra la conexión, salimos del bucle.
                if (mensajeServidor == null) {
                    System.out.println("El servidor ha cerrado la conexión.");
                    break;
                }

                // Muestro el mensaje del servidor para que el usuario sepa qué está pasando.
                System.out.println("Servidor: " + mensajeServidor);

                // Si el servidor me pide una jugada, entonces tengo que pedirla por teclado.
                if (mensajeServidor.contains("Introduce tu jugada")) {

                    System.out.println("1 = Piedra");
                    System.out.println("2 = Papel");
                    System.out.println("3 = Tijeras");
                    System.out.print("Tu elección: ");

                    String jugada = teclado.nextLine();

                    // Envío la jugada al servidor.
                    salida.println(jugada);
                }
            }

            // Cierro todo cuando termina la partida.
            socket.close();
            entrada.close();
            salida.close();
            teclado.close();

        } catch (IOException e) {
            // Si algo falla, lo muestro por pantalla.
            System.out.println("Error al conectar o comunicar con el servidor: " + e.getMessage());
        }
    }
}
