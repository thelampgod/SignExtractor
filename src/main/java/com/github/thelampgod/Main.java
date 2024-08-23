package com.github.thelampgod;

import net.querz.mca.MCAFile;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.io.snbt.SNBTWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {

    public static void main(String... args) throws IOException {
        if (args == null || args.length == 0) {
            System.err.println("specify region folder");
            System.exit(1);
        }
        String regionFolder = args[0];
        String output = ".";
        if (args.length == 2) {
            output = args[1];
        }
        final FileWriter writer = new FileWriter(output + "/signs.txt");
        final SNBTWriter nbt = new SNBTWriter();

        AtomicInteger chunks = new AtomicInteger();
        Arrays.asList(new File(regionFolder).listFiles()).stream()
                .filter(file -> file.getName().endsWith(".mca"))
                .parallel()
                .forEach(file -> {
                    MCAFile mca = new MCAFile(file);
                    try {
                        mca.load();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println("Processing " + mca.getName());
                    mca.forEach(chunk -> {
                        chunk.getData().getList("block_entities").stream().map(tag -> (CompoundTag) tag).forEach(tile -> {
                            if (!tile.getString("id").equals("minecraft:sign")) return;
                            try {
                                writer.write(nbt.toString(tile) + "\n");
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                        chunks.incrementAndGet();
                    });
                });
        System.out.printf("Processed %s chunks", chunks);
        writer.close();
    }
}
