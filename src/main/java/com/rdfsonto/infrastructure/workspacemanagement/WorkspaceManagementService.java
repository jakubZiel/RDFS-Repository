package com.rdfsonto.infrastructure.workspacemanagement;

import java.util.UUID;


public interface WorkspaceManagementService
{
    void clearWorkspace(UUID exportId);
    void clearWorkspace(String fileName);
}
