package com.rdfsonto.infrastructure.security.aspect;

import static com.rdfsonto.classnode.service.ClassNodeExceptionErrorCode.UNAUTHORIZED_RESOURCE_ACCESS;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;

import com.github.jsonldjava.shaded.com.google.common.collect.Streams;
import com.rdfsonto.classnode.service.ClassNodeException;
import com.rdfsonto.infrastructure.security.service.AuthService;
import com.rdfsonto.project.database.ProjectNode;

import lombok.RequiredArgsConstructor;


@Aspect
@Component
@RequiredArgsConstructor
class ProtectedResourceAspect
{
    private final static String PROJECT_ID_ARG_NAME = "projectId";
    private final static String USER_ID_ARG_NAME = "userId";
    private final AuthService authService;

    @Before("@annotation(ProtectedResource)")
    void aroundProtectedResource(final JoinPoint joinPoint)
    {
        final var signature = (MethodSignature) joinPoint.getSignature();

        final var parameters = signature.getParameterNames();
        final var args = joinPoint.getArgs();

        final var boundArgs = Streams.zip(Arrays.stream(parameters), Arrays.stream(args), Pair::of)
            .toList();

        final long projectId = boundArgs.stream()
            .filter(boundArg -> boundArg.getFirst().equals(PROJECT_ID_ARG_NAME))
            .map(boundArg -> (long) boundArg.getSecond())
            .findFirst()
            .orElseThrow(() ->
                new IllegalStateException("ProtectedResource annotation is applied on wrong method - missing %s.".formatted(PROJECT_ID_ARG_NAME)));

        final long userId = boundArgs.stream()
            .filter(boundArg -> boundArg.getFirst().equals(USER_ID_ARG_NAME))
            .map(boundArg -> (long) boundArg.getSecond())
            .findFirst()
            .orElseThrow(() ->
                new IllegalStateException("ProtectedResource annotation is applied on wrong method - missing %s.".formatted(USER_ID_ARG_NAME)));

        final var isOwned = authService.validateResourceRights(projectId, userId, ProjectNode.class);

        if (!isOwned)
        {
            throw new ClassNodeException("Unauthenticated request by user.", UNAUTHORIZED_RESOURCE_ACCESS);
        }
    }
}
