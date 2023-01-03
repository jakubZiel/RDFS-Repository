package com.rdfsonto.classnode.rest;

import java.util.List;

import org.springframework.stereotype.Component;

import com.rdfsonto.classnode.service.ClassNodeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
public class NodeChangeEventHandler
{
    private final ClassNodeService classNodeService;

    public List<NodeChangeEventResponse> handleEvents(List<NodeChangeEvent> events)
    {
        return events.stream().map(event -> {
            switch (event.type())
            {
                case CREATE ->
                {
                    return handleCreate(event);
                }
                case DELETE ->
                {
                    return handleDelete(event);
                }
                case UPDATE ->
                {
                    return handleUpdate(event);
                }
                default ->
                {
                    log.warn("Invalid request type: {}", event.type());
                    return failedEvent(event);
                }
            }
        }).toList();
    }

    private NodeChangeEventResponse handleDelete(final NodeChangeEvent deleteEvent)
    {
        final var deleted = classNodeService.deleteById(deleteEvent.nodeId());

        return NodeChangeEventResponse.builder()
            .withEvent(deleteEvent)
            .withFailed(!deleted)
            .build();
    }

    private NodeChangeEventResponse handleCreate(final NodeChangeEvent createEvent)
    {
        final var savedNode = classNodeService.save(createEvent.body());

        final var responseBuilder = NodeChangeEventResponse.builder()
            .withEvent(createEvent)
            .withFailed(savedNode.isEmpty());

        savedNode.ifPresent(responseBuilder::withBody);

        return responseBuilder.build();
    }

    private NodeChangeEventResponse handleUpdate(final NodeChangeEvent updateEvent)
    {
        final var updatedNode = classNodeService.update(updateEvent.body());

        final var responseBuilder = NodeChangeEventResponse.builder()
            .withEvent(updateEvent)
            .withFailed(updatedNode.isEmpty());

        updatedNode.ifPresent(responseBuilder::withBody);

        return responseBuilder.build();
    }

    private NodeChangeEventResponse failedEvent(final NodeChangeEvent changeEvent)
    {
        return NodeChangeEventResponse.builder()
            .withEvent(changeEvent)
            .withFailed(true)
            .build();
    }
}