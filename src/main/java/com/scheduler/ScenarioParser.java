package com.scheduler;

import java.io.*;
import java.util.*;

public class ScenarioParser {

    public static List<PCB> parse(File file) throws IOException {
        List<PCB> processes = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        int id = 0;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            String[] tokens = line.split("\\s+");

            // Format: <name> <arrivalTime> <priority> <C0> <I0> <C1> <I1> ... <Cn>
            String name = tokens[0];
            int arrivalTime = Integer.parseInt(tokens[1]);
            int priority = Integer.parseInt(tokens[2]);

            List<Integer> cpuBursts = new ArrayList<>();
            List<Integer> ioBursts = new ArrayList<>();

            // Tokens from index 3 onward alternate: CPU IO CPU IO ... CPU
            // CPU bursts are at even positions (0, 2, 4...) relative to index 3
            // IO bursts are at odd positions (1, 3, 5...) relative to index 3
            for (int i = 3; i < tokens.length; i++) {
                int value = Integer.parseInt(tokens[i]);
                if ((i - 3) % 2 == 0) {
                    cpuBursts.add(value);
                } else {
                    ioBursts.add(value);
                }
            }

            processes.add(new PCB(name, id, arrivalTime, priority,
                    cpuBursts, ioBursts));
            id++;
        }

        reader.close();
        return processes;
    }
}