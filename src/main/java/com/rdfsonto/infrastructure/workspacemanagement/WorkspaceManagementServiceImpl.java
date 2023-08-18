package com.rdfsonto.infrastructure.workspacemanagement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
public class WorkspaceManagementServiceImpl implements WorkspaceManagementService
{
    @Value("${rdf4j.downloader.workspace}")
    private String WORKSPACE_DIR;

    @Override
    public void clearWorkspace(final UUID exportId)
    {
        try (final var filePaths = Files.list(Path.of(WORKSPACE_DIR)))
        {
            filePaths.filter(path -> path.getFileName().toString().startsWith(exportId.toString()))
                .forEach(this::deleteFile);
        }
        catch (IOException e)
        {
            log.error("Failed to delete files for export ID: {}", exportId);
        }
    }

    public void clearWorkspace(final String fileName)
    {
        final var baseName = FilenameUtils.getBaseName(fileName);
        if (baseName == null)
        {
            return;
        }
        try (final var filePaths = Files.list(Path.of(WORKSPACE_DIR)))
        {
            filePaths.filter(path -> path.getFileName().toString().startsWith(baseName))
                .forEach(this::deleteFile);
        }
        catch (IOException e)
        {
            log.error("Failed to delete files for export: {}", fileName);
        }
    }

    private void deleteFile(final Path path)
    {
        try
        {
            Files.deleteIfExists(path);
        }
        catch (IOException e)
        {
            log.error("Failed to delete file: {}.", path);
        }
    }
}
