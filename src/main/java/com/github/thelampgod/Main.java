package com.github.thelampgod;

import net.querz.mca.MCAFile;
import net.querz.nbt.CompoundTag;
import net.querz.nbt.NBTUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Scanner;

public class Main {

    public static void main(String... args) throws IOException {
        if (args == null || args.length == 0) {
            System.err.println("specify world folder");
            System.exit(1);
        }
        System.out.println("WARNING! This will overwrite your files, no copy will be made by the program.");
        System.out.println("Please backup your world before running in case of error");
        Scanner scanner = new Scanner(System.in);
        System.out.println("Are you sure you want to continue? (Y/N)");
        char ans = scanner.nextLine().charAt(0);
        if (ans != 'Y' && ans != 'y') {
            System.out.println("Stopping!");
            return;
        }
        scanner.close();

        String worldFolder = args[0];
        fixLevelDat(worldFolder);
        fixDimension(worldFolder);
        fixDimension(worldFolder + "/DIM1");
        fixDimension(worldFolder + "/DIM-1");
    }

    private static void fixDimension(String worldFolder) {
        fixEntities(worldFolder);
        fixRegions(worldFolder);
    }

    private static void fixRegions(String worldFolder) {
        File region = new File(worldFolder + "/region/");
        if (!region.exists()) return;

//        AtomicInteger chunks = new AtomicInteger();
        Arrays.asList(region.listFiles()).stream()
                .filter(file -> file.getName().endsWith(".mca"))
                .parallel()
                .forEach(file -> {
                    try {
                        if (file.length() == 0) {
                            System.out.println("Deleting empty file");
                            Files.delete(file.toPath());
                            return;
                        }

                        MCAFile mca = new MCAFile(file);
                        mca.load();
                        System.out.println("Cleaning " + mca.getName());
                        mca.forEach(chunk -> {
                            if (chunk == null) return;
                            CompoundTag data = chunk.getData();
                            data.remove("CaptureTimestamp");
                            data.remove("Author");
                        });

                        mca.save();
                        System.out.println("Cleaned " + mca.getName());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
//        System.out.printf("Processed %s chunks", chunks);
    }

    private static void fixEntities(String worldFolder) {
        File region = new File(worldFolder + "/entities/");
        if (!region.exists()) return;

//        AtomicInteger chunks = new AtomicInteger();
        Arrays.asList(region.listFiles()).stream()
                .filter(file -> file.getName().endsWith(".mca"))
                .parallel()
                .forEach(file -> {
                    try {
                        if (file.length() == 0) {
                            System.out.println("Deleting empty file");
                            Files.delete(file.toPath());
                            return;
                        }

                        MCAFile mca = new MCAFile(file);
                        mca.load();
                        System.out.println("Cleaning " + mca.getName());
                        mca.forEach(chunk -> {
                            if (chunk == null) return;
                            CompoundTag data = chunk.getData();
                            data.getList("Entities").stream().map(tag -> (CompoundTag) tag)
                                    .forEach(tag -> {
                                        data.remove("CaptureTimestamp");
                                        data.remove("Author");
                                    });
                        });

                        mca.save();
                        System.out.println("Cleaned " + mca.getName());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
//        System.out.printf("Processed %s chunks", chunks);
    }

    private static void fixLevelDat(String worldFolder) throws IOException {
        File level = new File(worldFolder + "/level.dat");
        if (!level.exists()) return;

        System.out.println("Cleaning " + level.getName() + "...");
        CompoundTag tag = (CompoundTag) NBTUtil.read(level);
        tag.remove("Author");
        NBTUtil.write(level, tag);
        System.out.println("Cleaned " + level.getName() + "!");
    }
}
