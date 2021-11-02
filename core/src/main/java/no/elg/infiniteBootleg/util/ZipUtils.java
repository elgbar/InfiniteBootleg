package no.elg.infiniteBootleg.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.StreamUtils;
import com.google.common.io.Files;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Deque;
import java.util.LinkedList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.jetbrains.annotations.NotNull;

public final class ZipUtils {

  private ZipUtils() {}

  public static void zip(@NotNull FileHandle directory, @NotNull FileHandle zipFile)
      throws IOException {
    URI base = directory.file().toURI();
    Deque<FileHandle> queue = new LinkedList<>();
    queue.push(directory);
    if (!zipFile.exists()) {
      zipFile.parent().mkdirs();
    }

    try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(zipFile.file()))) {
      while (!queue.isEmpty()) {
        directory = queue.pop();
        for (FileHandle kid : directory.list()) {
          String name = base.relativize(kid.file().toURI()).getPath();
          if (kid.isDirectory()) {
            queue.push(kid);
            name = name.endsWith("/") ? name : name + "/";
            zip.putNextEntry(new ZipEntry(name));
          } else {
            zip.putNextEntry(new ZipEntry(name));
            //noinspection UnstableApiUsage
            Files.copy(kid.file(), zip);
            zip.closeEntry();
          }
        }
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  public static void unzip(@NotNull FileHandle directory, @NotNull FileHandle zipFile) {
    try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile.file()))) {
      ZipEntry entry = zipInputStream.getNextEntry();
      if (entry == null) {
        return;
      }
      do {
        if (!entry.isDirectory()) {
          FileHandle newFile = directory.child(entry.getName());
          newFile.parent().mkdirs();
          StreamUtils.copyStream(zipInputStream, newFile.write(false));
        }
        entry = zipInputStream.getNextEntry();
      } while (entry != null);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
