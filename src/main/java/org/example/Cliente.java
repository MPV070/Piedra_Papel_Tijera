/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Cliente {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        String ip;
        int puerto;

        // ===== VERIFICACIÓN DE IP =====
        while (true) {
            System.out.print("Introduce la IP del servidor: ");
            ip = sc.nextLine();

            try {
                InetAddress.getByName(ip);
                break; // IP válida
            } catch (UnknownHostException e) {
                System.out.println("❌ IP no válida. Inténtalo de nuevo.");
            }
        }

        // ===== VERIFICACIÓN DE PUERTO =====
        while (true) {
            System.out.print("Introduce el puerto del servidor: ");
            try {
                puerto = Integer.parseInt(sc.nextLine());

                if (puerto >= 1024 && puerto <= 49151) {
                    break;
                } else {
                    System.out.println("❌ El puerto debe estar entre 1024 y 49151.");
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Debes introducir un número.");
            }
        }

        // ===== CONEXIÓN AL SERVIDOR =====
        try (Socket socket = new Socket(ip, puerto);
             BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter salida = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println("✅ Conectado al servidor.");

            // El servidor controla la lógica y las puntuaciones. El cliente solo
            // responde a comandos de texto que envía el servidor.
            String linea;
            boolean continuar = true;

            while (continuar && (linea = entrada.readLine()) != null) {
                // Mostrar cualquier mensaje no reconocido
                if (linea.equalsIgnoreCase("JUEGA")) {
                    int eleccion = 0;
                    while (eleccion < 1 || eleccion > 3) {
                        System.out.print("Elige (1=Piedra, 2=Papel, 3=Tijeras): ");
                        try {
                            eleccion = Integer.parseInt(sc.nextLine().trim());
                        } catch (NumberFormatException ex) {
                            System.out.println("no se ha introducido un número, por favor introduce un número entre el 1 y el 3");
                        }
                    }
                    // Enviar la jugada como texto para concordar con el servidor
                    salida.println(String.valueOf(eleccion));
                } else if (linea.equalsIgnoreCase("ELIMINADO")) {
                    System.out.println("Has sido eliminado de la partida.");
                    continuar = false;
                } else if (linea.equalsIgnoreCase("OPPONENT_DISCONNECTED")) {
                    System.out.println("Tu oponente se ha desconectado.");
                    // En modo 1vs1 el servidor puede continuar contra el GameHost.
                } else if (linea.startsWith("ERROR:")) {
                    // Mostrar errores de validación enviados por el servidor tal cual
                    System.out.println(linea.substring("ERROR:".length()).trim());
                } else {
                    // Mensajes informativos u otros del servidor
                    System.out.println("Servidor: " + linea);
                }
            }

        } catch (IOException e) {
            System.out.println("❌ No se pudo conectar con el servidor: " + e.getMessage());
        }

        sc.close();
    }
}
