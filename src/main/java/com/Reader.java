package com;

import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.function.Consumer;

@Service
public class Reader {

    @SneakyThrows
    public void start(Consumer<String> onReadLine) {
        ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", "tail -f -n 10000 /home/sherbel/Downloads/aic-cart-state-machine.log");
//        ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", "ssh tracxpoint@master-cart17 -C 'tail -f -n 10000 /opt/tracxpoint/cart-state-machine/logs/aic-cart-state-machine.log'");
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            onReadLine.accept(line);
        }
    }
}
