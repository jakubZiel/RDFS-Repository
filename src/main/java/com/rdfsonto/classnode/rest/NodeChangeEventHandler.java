package com.rdfsonto.classnode.rest;

import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
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

    public List<NodeChangeEventResponse> handleEvents(List<NodeChangeEvent> events, final Long projectId)
    {
        return events.stream().map(event -> {
            switch (event.type())
            {
                case CREATE ->
                {
                    return handleCreate(event, projectId);
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
        classNodeService.deleteById(deleteEvent.nodeId());
        // TODO / TO_DELETE final var isDeleted = classNodeService.findById(deleteEvent.nodeId()).isEmpty();

        return NodeChangeEventResponse.builder()
            .withEvent(deleteEvent)
            //.withFailed(isDeleted)
            .build();
    }

    private NodeChangeEventResponse handleCreate(final NodeChangeEvent createEvent, final Long projectId)
    {
        final var savedNode = classNodeService.save(projectId, createEvent.body());

        final var responseBuilder = NodeChangeEventResponse.builder()
            .withEvent(createEvent)
            .withBody(savedNode)
            .withFailed(false);

        return responseBuilder.build();
    }

    private NodeChangeEventResponse handleUpdate(final NodeChangeEvent updateEvent)
    {
        throw new NotImplementedException();

       /* final var updatedNode = classNodeService.save(updateEvent.body(), 0);

        final var responseBuilder = NodeChangeEventResponse.builder()
            .withEvent(updateEvent)
            .withBody(updatedNode)
            .withFailed(false);

        return responseBuilder.build();*/
    }

    private NodeChangeEventResponse failedEvent(final NodeChangeEvent changeEvent)
    {
        return NodeChangeEventResponse.builder()
            .withEvent(changeEvent)
            .withFailed(true)
            .build();
    }
}