package com.rdfsonto.prefix.service;

import java.util.Map;

import lombok.Builder;


@Builder(setterPrefix = "with", toBuilder = true)
public record PrefixMapping(Map<String, String> prefixToUri, Map<String, String> uriToPrefix)
{

}
