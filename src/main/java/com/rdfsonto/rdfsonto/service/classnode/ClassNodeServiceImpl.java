package com.rdfsonto.rdfsonto.service.classnode;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.rdfsonto.rdfsonto.repository.classnode.ClassNodeNeo4jDriverRepository;
import com.rdfsonto.rdfsonto.repository.classnode.ClassNodeRepository;
import com.rdfsonto.rdfsonto.repository.classnode.ClassNodeVo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class ClassNodeServiceImpl implements ClassNodeService
{
    private final ClassNodeRepository classNodeRepository;
    private final ClassNodeNeo4jDriverRepository classNodeNeo4jDriverRepository;
    private final ClassNodeMapper classNodeMapper;

    @Override
    public List<ClassNode> getClassNodesByIds(final List<Long> ids)
    {
        final var notHydratedNodes = classNodeRepository.findAllById(ids);

        if (notHydratedNodes.size() != ids.size())
        {
            throw new IllegalStateException("Not all nodes exist");
        }

        final var incoming = classNodeRepository.findAllIncomingNeighbours(ids);
        final var outgoing = classNodeNeo4jDriverRepository.findAllOutgoingNeighbours(ids);

        final var groupedIncoming = incoming.stream().collect(Collectors.groupingBy(ClassNodeVo::getSource));
        final var groupedOutgoing = outgoing.stream().collect(Collectors.groupingBy(ClassNodeVo::getSource));

        return notHydratedNodes.stream()
            .map(node ->
                classNodeMapper.mapToDomain(node,
                    groupedIncoming.get(node.getId()),
                    groupedOutgoing.get(node.getId())))
            .collect(Collectors.toList());
    }

    @Override
    public Optional<ClassNode> getClassNodeById(final Long id)
    {
        final var notHydratedNode = classNodeRepository.findById(id);

        if (notHydratedNode.isEmpty())
        {
            return Optional.empty();
        }

        final var incoming = classNodeRepository.findAllIncomingNeighbours(id);
        final var outgoing = classNodeRepository.findAllOutgoingNeighbours(id);

        return Optional.of(classNodeMapper.mapToDomain(notHydratedNode.get(), incoming, outgoing));
    }
}
