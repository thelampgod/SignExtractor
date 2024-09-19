package com.github.thelampgod;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class test {
    private static final Object2LongOpenHashMap<String> blockOccurences = new Object2LongOpenHashMap<>();


    public static void main(String[] args) throws IOException {
        Files.readAllLines(Path.of("/home/thela/Desktop/post/BlockCounts_Feb6th_2024.csv")).forEach(line -> {
            String[] split = line.split(",");
            blockOccurences.put(split[0], Long.parseLong(split[1]));
        });

        Files.readAllLines(Path.of("/home/thela/Desktop/post/BlockCounts_Sept7th_2024.csv")).forEach(line -> {
            String[] split = line.split(",");
            long past = blockOccurences.getLong(split[0]);
            long now = Long.parseLong(split[1]);
            long diff = (now - past);

            // Cast to double to avoid integer division
            double percentage = ((double) now / past) * 100 - 100;

            System.out.println(split[0].split("minecraft:")[1] + ": " + now + " (" + diff + " | " + String.format("%.2f", percentage) + "%)");
        });
    }
}
