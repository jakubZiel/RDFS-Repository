package com.rdfsonto.elastic.service;

import lombok.Builder;


@Builder(setterPrefix = "with")
public record SearchAfterParams(double accuracyScore, String uri)
{
}
