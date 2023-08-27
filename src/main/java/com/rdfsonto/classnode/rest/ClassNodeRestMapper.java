package com.rdfsonto.classnode.rest;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.rdfsonto.classnode.service.ClassNode;

import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class ClassNodeRestMapper
{
    ClassNode mapTypes(final ClassNode node)
    {
        final var parsedProps = node.properties().entrySet().stream()
            .map(prop -> {
                final var values = (List<Object>) prop.getValue();
                return Map.entry(prop.getKey(), (Object) values.stream()
                    .map(value -> {
                        try
                        {
                            final var parsed = LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(value.toString())).toInstant(ZoneOffset.UTC);
                            return OffsetDateTime.ofInstant(parsed, ZoneOffset.UTC);
                        }
                        catch (final Exception exception)
                        {
                            return value;
                        }
                    }).toList());
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return node.toBuilder()
            .withProperties(parsedProps)
            .build();
    }
}
