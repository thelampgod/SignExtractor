package com.github.thelampgod;

import net.querz.mca.MCAFile;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.ListTag;

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

        writer.write("x,y,z,line1,line2,line3,line4,color,glowing\n");
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
                            CompoundTag front_text = tile.getCompound("front_text");
                            ListTag messages = front_text.getList("messages");
                            String formatted = String.format("%d,%d,%d,%s,%s,%s,%s,%s,%s",
                                    tile.getInt("x"),
                                    tile.getInt("y"),
                                    tile.getInt("z"),
                                    messages.getString(0),
                                    messages.getString(1),
                                    messages.getString(2),
                                    messages.getString(3),
                                    front_text.getString("color"),
                                    front_text.getBoolean("has_glowing_text")
                            );
                            try {
                                writer.write(formatted + "\n");
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
