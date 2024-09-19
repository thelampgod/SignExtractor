package com.github.thelampgod;

import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.querz.mca.Chunk;
import net.querz.mca.MCAFile;
import net.querz.mca.ParserHandler;
import net.querz.mca.parsers.BlockParser;
import net.querz.nbt.CompoundTag;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class BlockStats {

    private static final Object2LongOpenHashMap<String> blockOccurences = new Object2LongOpenHashMap<>();

    public static void main(String... args) throws IOException {
        if (args == null || args.length < 2) {
            System.err.println("usage: <world> <output.txt>");
            System.exit(1);
        }
        final String world = args[0];
        final String output = args[1];

        Arrays.stream(new File(world + "/region/").listFiles())
                .parallel()
                .filter(file -> file.getName().endsWith(".mca"))
                .map(MCAFile::new)
                .forEach(region -> {
                    try {
                        region.load();
                        long now = System.currentTimeMillis();
                        count(region);
                        System.out.println("Counted in " + (System.currentTimeMillis() - now) + "ms");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        FileWriter out = new FileWriter(output);
        blockOccurences.object2LongEntrySet().stream()
                .sorted(Comparator.comparingLong((Map.Entry<String, Long> entry) -> entry.getValue()).reversed())
                .forEach((entry) -> {
                    String block = entry.getKey();
                    long count = entry.getLongValue();
                    try {
                        out.write(block + "," + count + "\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        out.close();
    }

    private static void count(MCAFile region) {
        region.forEach(chunk -> {
            for (CompoundTag section1 : chunk.getSectionParser()) {
                if (section1 == null) continue;

                BlockParser<?> parser1 = ParserHandler.getBlockParser(chunk.getDataVersion(), section1);
                try {
                    for (int x = 0; x < 16; ++x) {
                        for (int z = 0; z < 16; ++z) {
                            for (int y = 0; y < 16; ++y) {
                                CompoundTag block1 = parser1.getBlockAt(x, y, z);
                                String block1Name = block1.getString("Name");
                                blockOccurences.addTo(block1Name, 1);
                            }
                        }
                    }
                } catch (Exception e) {
                }
            }
        });
    }

}