package com.fuelqueue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FuelQueueApp {

    public static void main(String[] args) {
        SpringApplication.run(FuelQueueApp.class, args);
        System.out.println("""

                ╔══════════════════════════════════════════════╗
                ║      Fuel Queue Backend is running!          ║
                ╠══════════════════════════════════════════════╣
                ║  API Base : http://localhost:8080/api        ║
                ║  H2 Console: http://localhost:8080/h2-console║
                ║  JDBC URL : jdbc:h2:mem:fuelqueue            ║
                ║  Username : sa   Password: (empty)           ║
                ╚══════════════════════════════════════════════╝
                """);
    }
}
