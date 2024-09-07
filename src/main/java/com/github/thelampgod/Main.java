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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Main {

    private static Mode mode;

    /**
     * DEL means blocks that have been removed are left. block1 == block2 -> set to air
     * STAY means blocks that stayed the same are left. block1 != block2 -> set to air
     * ADD means blocks that have been added are left. block1 == block2 -> set to air, but the world input order is switched.
     */
    private enum Mode {
        ADD,
        DEL,
        STAY
    }

    private static final Object2LongOpenHashMap<String> additions = new Object2LongOpenHashMap<>();
    private static final Object2LongOpenHashMap<String> removals = new Object2LongOpenHashMap<>();

    public static void main(String... args) throws IOException {
        if (args == null || args.length < 4) {
            System.err.println("usage: <world1> <world2> <output> <mode(ADD, DEL, STAY)>");
            System.out.println("oldest world should be specified first.");
            System.exit(1);
        }
        mode = getMode(args[3]);

        String world1 = args[0];
        String world2;
        if (mode.equals(Mode.ADD)) {
            world1 = args[1];
            world2 = args[0];
        } else {
            world2 = args[1];
        }
        final String output = args[2];
        Files.createDirectories(Path.of(output + "/region/"));

        final Set<String> alreadyConverted = new HashSet<>(Arrays.asList(new File(output + "/region/").list()));

        Arrays.stream(new File(world1 + "/region/").listFiles())
                .parallel()
                .filter(file -> !alreadyConverted.contains(file.getName()))
                .filter(file -> file.getName().endsWith(".mca"))
                .forEach(file -> {
                    try {
                        File file2 = new File(String.format("%s/region/%s", world2, file.getName()));
                        if (!file2.exists()) return;

                        MCAFile r2 = new MCAFile(file2);
                        MCAFile r1 = new MCAFile(file);
                        r2.load();
                        r1.load();
                        System.out.println(file.getAbsolutePath());
                        System.out.println("Diffing region " + file.getName());
                        long now = System.currentTimeMillis();
                        diff(r1, r2, output);
                        System.out.println("Diffed in " + (System.currentTimeMillis() - now) + "ms");
                        System.out.println("Saving region " + file.getName());
//                        out.save();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        FileWriter add = new FileWriter(output + "/additions.csv");
        FileWriter remove = new FileWriter(output + "/removals.csv");
        additions.object2LongEntrySet().stream()
                .sorted(Comparator.comparingLong((Map.Entry<String, Long> entry) -> entry.getValue()).reversed())
                .forEach((entry) -> {
                    String block = entry.getKey();
                    long count = entry.getLongValue();
                    try {
                        add.write(block + "," + count + "\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        removals.object2LongEntrySet().stream()
                .sorted(Comparator.comparingLong((Map.Entry<String, Long> entry) -> entry.getValue()).reversed())
                .forEach((entry) -> {
                    String block = entry.getKey();
                    long count = entry.getLongValue();
                    try {
                        remove.write(block + "," + count + "\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        add.close();
        remove.close();
    }

    private static void diff(MCAFile r1, MCAFile r2, String output) {
        r1.forEach(chunk -> {
            Chunk chunk2 = r2.getChunkAt(chunk.getX(), chunk.getZ());
            if (chunk2 == null) return;

            for (CompoundTag section1 : chunk.getSectionParser()) {
                if (section1 == null) continue;
                CompoundTag section2 = chunk2.getSection(section1.getByte("Y"));
                if (section2 == null) continue;

                BlockParser<?> parser1 = ParserHandler.getBlockParser(chunk.getDataVersion(), section1);
                BlockParser<?> parser2 = ParserHandler.getBlockParser(chunk2.getDataVersion(), section2);
                try {
                    for (int x = 0; x < 16; ++x) {
                        for (int z = 0; z < 16; ++z) {
                            for (int y = 0; y < 16; ++y) {
                                CompoundTag block1 = parser1.getBlockAt(x, y, z);
                                CompoundTag block2 = parser2.getBlockAt(x, y, z);
                                String block1Name = block1.getString("Name");
                                String block2Name = block2.getString("Name");

                                if (!block1Name.equals(block2Name)) {
                                    removals.addTo(block1Name, 1);
                                    additions.addTo(block2Name, 1);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    continue;
                }
            }
        });
    }

    private static Mode getMode(String arg) {
        switch (arg) {
            case "ADD" -> {
                return Mode.ADD;
            }
            case "DEL" -> {
                return Mode.DEL;
            }
            case "STAY" -> {
                return Mode.STAY;
            }
        }
        return Mode.ADD;
    }

}
