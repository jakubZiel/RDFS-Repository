package com.rdfsonto.infrastructure.workspacemanagement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
public class WorkspaceManagementServiceImpl implements WorkspaceManagementService
{
    @Override
    public void clearWorkspace(final Path filePath)
    {
        try
        {
            Files.deleteIfExists(filePath);
        }
        catch (IOException e)
        {
            log.error("Failed to delete file: {}", filePath);
        }
    }
}
