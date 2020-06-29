package no.elg.infiniteBootleg.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.StreamUtils;
import com.google.common.io.Files;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URI;
import java.util.Deque;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    public static void zip(@NotNull FileHandle directory, @NotNull FileHandle zipFile) throws IOException {
        URI base = directory.file().toURI();
        Deque<FileHandle> queue = new LinkedList<>();
        queue.push(directory);
        if (!zipFile.exists()) {
            zipFile.parent().mkdirs();
        }
        OutputStream out = new FileOutputStream(zipFile.file());
        Closeable res = out;
        try {
            ZipOutputStream zout = new ZipOutputStream(out);
            res = zout;
            while (!queue.isEmpty()) {
                directory = queue.pop();
                for (FileHandle kid : directory.list()) {
                    String name = base.relativize(kid.file().toURI()).getPath();
                    if (kid.isDirectory()) {
                        queue.push(kid);
                        name = name.endsWith("/") ? name : name + "/";
                        zout.putNextEntry(new ZipEntry(name));
                    }
                    else {
                        zout.putNextEntry(new ZipEntry(name));
                        //noinspection UnstableApiUsage
                        Files.copy(kid.file(), zout);
                        zout.closeEntry();
                    }
                }
            }
        } finally {
            res.close();
        }
    }

    public static void unzip(@NotNull FileHandle directory, @NotNull FileHandle zipFile) {
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.file()));
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {
                if (!ze.isDirectory()) {
                    FileHandle newFile = directory.child(ze.getName());
                    newFile.parent().mkdirs();
                    StreamUtils.copyStream(zis, newFile.write(false));
                }
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            zis.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
