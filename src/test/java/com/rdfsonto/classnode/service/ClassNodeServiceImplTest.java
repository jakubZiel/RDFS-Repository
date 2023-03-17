package com.rdfsonto.classnode.service;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.NotImplementedException;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rdfsonto.classnode.database.ClassNodeNeo4jDriverRepository;
import com.rdfsonto.classnode.database.ClassNodeRepository;
import com.rdfsonto.classnode.database.ClassNodeVo;
import com.rdfsonto.classnode.database.ClassNodeVoMapper;
import com.rdfsonto.project.service.ProjectService;


@ExtendWith({MockitoExtension.class, SoftAssertionsExtension.class})
class ClassNodeServiceImplTest
{
    private static final ClassNodeRepository classNodeRepository = Mockito.mock(ClassNodeRepository.class);
    private static final ClassNodeNeo4jDriverRepository classNodeNeo4jDriverRepository = Mockito.mock(ClassNodeNeo4jDriverRepository.class);
    private static final ProjectService projectService = Mockito.mock(ProjectService.class);
    private static final ClassNodeVoMapper classNodeVoMapper = new ClassNodeVoMapper();
    private static final ClassNodeMapper classNodeMapper = new ClassNodeMapper();
    private static ClassNodeServiceImpl classNodeService;

    @BeforeAll
    static void setup()
    {
        classNodeService = new ClassNodeServiceImpl(
            classNodeRepository,
            classNodeNeo4jDriverRepository,
            classNodeMapper,
            classNodeVoMapper,
            projectService,
            new UriUniquenessHandler(projectService, new UniqueUriIdHandler()));

        when(classNodeRepository.findAllById(any())).thenReturn(List.of(
            ClassNodeVo.builder().withId(1L).withUri("node-1").build(),
            ClassNodeVo.builder().withId(2L).withUri("node-2").build()
        ));

        when(classNodeRepository.findAllIncomingNeighbours(any())).thenReturn(List.of(
            ClassNodeVo.builder().withId(5L).withSource(1L).withRelation("REL-A").build(),
            ClassNodeVo.builder().withId(3L).withSource(1L).withRelation("REL-A").build(),
            ClassNodeVo.builder().withId(3L).withSource(1L).withRelation("REL-B").build(),

            ClassNodeVo.builder().withId(4L).withSource(2L).withRelation("REL-A").build(),
            ClassNodeVo.builder().withId(5L).withSource(2L).withRelation("REL-B").build()
        ));

        when(classNodeNeo4jDriverRepository.findAllOutgoingNeighbours(any())).thenReturn(List.of(
            ClassNodeVo.builder().withId(6L).withSource(1L).withRelation("REL-A").build(),
            ClassNodeVo.builder().withId(7L).withSource(1L).withRelation("REL-A").build(),
            ClassNodeVo.builder().withId(8L).withSource(1L).withRelation("REL-B").build(),

            ClassNodeVo.builder().withId(8L).withSource(2L).withRelation("REL-A").build(),
            ClassNodeVo.builder().withId(8L).withSource(2L).withRelation("REL-A").build()
        ));
    }

    @Test
    void testFindByIds(final SoftAssertions softly)
    {
        // given
        final var ids = List.of(1L, 2L);

        // when
        final var found = classNodeService.findByIds(ids);

        // then
        softly.assertThat(found.size()).isEqualTo(2);
    }

    @Test
    void testFindById(final SoftAssertions softly)
    {
        // given
        final var id = 1L;

        when(classNodeRepository.findById(any())).thenReturn(Optional.of(
            ClassNodeVo.builder().withId(1L).build()));

        // when
        final var found = classNodeService.findById(id);

        // then
        softly.assertThat(found).isNotEmpty();
    }

    @Test
    void testFindById_whenNodeNotFound(final SoftAssertions softly)
    {
        // given
        final var id = 1L;

        when(classNodeRepository.findById(any())).thenReturn(Optional.empty());

        // when
        final var found = classNodeService.findById(id);

        // then
        softly.assertThat(found).isEmpty();
    }

    @Test
    void testFindNeighbours_whenTooManyNeighbours()
    {
        // given
        final var id = 1L;
        final var distance = 2;

        when(classNodeRepository.countAllNeighbours(distance, id)).thenReturn(1001);

        // when & then
        assertThrows(NotImplementedException.class, () -> classNodeService.findNeighbours(id, distance, List.of()));
    }

    @Test
    void testSave(final SoftAssertions softly)
    {
        // given
        final var outgoing = List.of();
        final var incoming = List.of();

        final var toSave = ClassNode.builder().withUri("uri-1").build();

        when(classNodeRepository.save(any())).thenReturn(ClassNodeVo.builder().withId(1L).build());

        when(classNodeRepository.findAllById(List.of(1L, 2L))).thenReturn(null);
        when(classNodeRepository.findAllById(List.of(2L, 3L))).thenReturn(null);

        // when
        final var saved = classNodeService.save(toSave, 1L);

        // then
        softly.assertThat(saved).isNotNull();
    }

    @Test
    void testUpdate(final SoftAssertions softly)
    {
        final var toUpdate = ClassNode.builder().build();

        // when
        final var updated = classNodeService.save(toUpdate, 1L);

        // then
        softly.assertThat(updated).isNotNull();
    }
}