package io.github.danhjalmberg.dronephotoservice.support;

import io.github.danhjalmberg.dronephotoservice.models.snapshots.TaskExportData;
import io.github.danhjalmberg.dronephotoservice.models.tasks.TaskType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InterruptedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests robust publication of exported task images.
 */
class TaskImageExporterTest {

    @TempDir
    private Path temporaryDirectory;

    /**
     * Verifies unsafe task-name characters cannot create output outside the run
     * directory and that no temporary file remains after success.
     *
     * @throws Exception if directory inspection or image reading fails
     */
    @Test
    void sanitizesTaskNameAndPublishesOnlyCompletedFile() throws Exception {

        BufferedImage image = new BufferedImage(
                2,
                2,
                BufferedImage.TYPE_INT_RGB);
        TaskExportData task = new TaskExportData(
                "../unsafe:name",
                TaskType.PHOTO,
                List.of(image));

        File runDirectory = TaskImageExporter.saveImagesToDisk(
                List.of(task),
                temporaryDirectory.toFile(),
                null);

        List<Path> outputFiles;
        try (var paths = Files.list(runDirectory.toPath())) {
            outputFiles = paths.collect(Collectors.toList());
        }

        assertEquals(1, outputFiles.size());
        assertEquals(".._unsafe_name_PhotoTask.gif", outputFiles.get(0).getFileName().toString());
        assertNotNull(ImageIO.read(outputFiles.get(0).toFile()));
        assertTrue(outputFiles.get(0).toAbsolutePath().startsWith(
                runDirectory.toPath().toAbsolutePath()));
    }

    /**
     * Verifies cancellation does not publish a task output or leave a temporary
     * file behind.
     *
     * @throws Exception if directory inspection fails
     */
    @Test
    void cancellationLeavesNoTaskOutput() throws Exception {

        BufferedImage image = new BufferedImage(
                2,
                2,
                BufferedImage.TYPE_INT_RGB);
        TaskExportData task = new TaskExportData(
                "task_1_1",
                TaskType.PHOTO,
                List.of(image));

        Thread.currentThread().interrupt();
        try {
            assertThrows(
                    InterruptedIOException.class,
                    () -> TaskImageExporter.saveImagesToDisk(
                            List.of(task),
                            temporaryDirectory.toFile(),
                            null));
        } finally {
            Thread.interrupted();
        }

        Path runDirectory;
        try (var paths = Files.list(temporaryDirectory)) {
            runDirectory = paths.findFirst().orElseThrow();
        }

        try (var paths = Files.list(runDirectory)) {
            assertEquals(0, paths.count());
        }
    }
}
