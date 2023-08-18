package com.rdfsonto.exportonto.service;

import java.io.InputStream;

import lombok.Builder;


@Builder(setterPrefix = "with")
public record SnapshotExport(InputStream fileInputStream, String fileName, Long snapshotTime)
{
}
