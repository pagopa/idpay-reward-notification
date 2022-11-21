package it.gov.pagopa.reward.notification.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@Slf4j
public final class ZipUtils {
    private ZipUtils(){}

    // region zip thresholds to avoid zip bomb attacks
    private static final int THRESHOLD_SIZE = 1000000000; // 1 GB
    private static final int THRESHOLD_ENTRIES = 10000;
    // endregion

    /** To zip a list of files */
    public static void zip(String zipFilePath, List<File> files) {
        try (FileOutputStream outputStream = new FileOutputStream(zipFilePath);
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
             BufferedOutputStream bufferOutStream = new BufferedOutputStream(zipOutputStream)) {
            files.forEach(file -> {
                byte[] buffer = new byte[1024];
                try (FileInputStream fis = new FileInputStream(file);
                     BufferedInputStream bufferInStream = new BufferedInputStream(fis)) {
                    ZipEntry zipEntry = new ZipEntry(file.getName());
                    zipOutputStream.putNextEntry(zipEntry);
                    int bytes;
                    while ((bytes = bufferInStream.read(buffer)) > 0) {
                        bufferOutStream.write(buffer, 0, bytes);
                        bufferOutStream.flush();
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Something gone wrong while zipping file %s into %s".formatted(file, zipFilePath), e);
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Something gone wrong while zipping files %s into %s".formatted(files, zipFilePath), e);
        }
    }

    /** To unzip a file */
    public static void unzip(String zipFilePath, String destDirPath) {
        Path destDir = Path.of(destDirPath);
        if(!Files.exists(destDir)){
            try {
                Files.createDirectories(destDir);
            } catch (IOException e) {
                throw new IllegalStateException("Cannot create destination dir %s where to unzipping %s".formatted(destDirPath, zipFilePath), e);
            }
        }

        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            if(zipFile.size()>THRESHOLD_ENTRIES){
                throw new IllegalArgumentException("Zip archive contains too many files!"); // to preserve from zip bomb attack
            }
            Stream<? extends ZipEntry> stream = zipFile.stream();
            int[] totalSizeArchive = new int[]{0};
            stream.forEach(zipEntry -> {
                totalSizeArchive[0]+=unzipZipEntry(destDirPath, zipFile, zipEntry);
                if(totalSizeArchive[0]>THRESHOLD_SIZE){
                    throw new IllegalArgumentException("Zip archive too large!"); // to preserve from zip bomb attack
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException("Something gone wrong while unzipping %s into %s".formatted(zipFilePath, destDirPath), e);
        }
    }

    /** To unzip a {@link ZipEntry} */
    public static int unzipZipEntry(String destDirPath, ZipFile zipFile, ZipEntry zipEntry) {
        try (InputStream inputStream = new BufferedInputStream(zipFile.getInputStream(zipEntry));
             BufferedInputStream bufferInStream = new BufferedInputStream(inputStream);
             BufferedOutputStream bufferOutStream = new BufferedOutputStream(
                     new FileOutputStream(destDirPath + File.separator + zipEntry.getName()))) {
            int bytes;

            int totalSizeEntry = 0;
            while ((bytes = bufferInStream.read()) > 0) {
                bufferOutStream.write(bytes);
                bufferOutStream.flush();

                totalSizeEntry += bytes;
            }

            if(totalSizeEntry>THRESHOLD_SIZE){
                throw new IllegalArgumentException("Zip file too large!"); // to preserve from zip bomb attack
            }

            return totalSizeEntry;
        } catch (IOException e) {
            throw new IllegalStateException("Something gone wrong while unzipping entry file %s into %s".formatted(zipEntry, destDirPath), e);
        }
    }

}
