package com.rdfsonto.rdfsonto.infrastructure.workspacemanagement;

import java.nio.file.Path;


public interface WorkspaceManagementService
{
    void clearWorkspace(Path file);
}
