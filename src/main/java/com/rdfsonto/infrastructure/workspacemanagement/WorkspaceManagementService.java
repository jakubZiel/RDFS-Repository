package com.rdfsonto.infrastructure.workspacemanagement;

import java.nio.file.Path;


public interface WorkspaceManagementService
{
    void clearWorkspace(Path file);
}
