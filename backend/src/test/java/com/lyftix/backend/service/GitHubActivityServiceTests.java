package com.lyftix.backend.service;

import com.lyftix.backend.dto.CreateGitHubActivityRequest;
import com.lyftix.backend.dto.GitHubActivityResponse;
import com.lyftix.backend.exception.InvalidGitHubActivityFilterException;
import com.lyftix.backend.model.GitHubActivity;
import com.lyftix.backend.repository.GitHubActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubActivityServiceTests {

    private static final Instant START = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-09-30T00:00:00Z");

    @Mock
    private GitHubActivityRepository gitHubActivityRepository;

    private GitHubActivityService gitHubActivityService;

    @BeforeEach
    void setUp() {
        gitHubActivityService = new GitHubActivityService(gitHubActivityRepository);
    }

    @Test
    void createsValidGitHubActivity() {
        GitHubActivity savedActivity = savedActivity();
        when(gitHubActivityRepository.save(any(GitHubActivity.class))).thenReturn(savedActivity);

        GitHubActivityResponse response = gitHubActivityService.createGitHubActivity(validRequest());

        assertThat(response.activityType()).isEqualTo("PullRequestOpened");
        assertThat(response.repositoryName()).isEqualTo("lyftix");
        assertThat(response.repositoryOwner()).isEqualTo("octocat");
        verify(gitHubActivityRepository).save(any(GitHubActivity.class));
    }

    @Test
    void mapsIdentityAndAuditFieldsIntoResponse() {
        GitHubActivity savedActivity = savedActivity();
        when(gitHubActivityRepository.save(any(GitHubActivity.class))).thenReturn(savedActivity);

        GitHubActivityResponse response = gitHubActivityService.createGitHubActivity(validRequest());

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.externalId()).isEqualTo("github-event-42");
        assertThat(response.createdAt()).isEqualTo(Instant.parse("2026-09-01T12:00:01Z"));
        assertThat(response.updatedAt()).isEqualTo(Instant.parse("2026-09-01T12:00:02Z"));
    }

    @Test
    void filtersByValidDateRange() {
        when(gitHubActivityRepository.findByOccurredAtGreaterThanEqualAndOccurredAtLessThan(eq(START), eq(END), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<GitHubActivityResponse> response = gitHubActivityService.filterGitHubActivities(
                null, START, END, 0, 10, "occurredAt"
        );

        assertThat(response).isEmpty();
        verify(gitHubActivityRepository).findByOccurredAtGreaterThanEqualAndOccurredAtLessThan(eq(START), eq(END), any(Pageable.class));
    }

    @Test
    void rejectsReversedDateRange() {
        assertThatThrownBy(() -> gitHubActivityService.filterGitHubActivities(
                null, END, START, 0, 10, "occurredAt"
        ))
                .isInstanceOf(InvalidGitHubActivityFilterException.class)
                .hasMessage("start must be before end");
        verifyNoInteractions(gitHubActivityRepository);
    }

    @Test
    void rejectsEqualDateRangeBoundaries() {
        assertThatThrownBy(() -> gitHubActivityService.filterGitHubActivities(
                null, START, START, 0, 10, "occurredAt"
        ))
                .isInstanceOf(InvalidGitHubActivityFilterException.class)
                .hasMessage("start must be before end");
        verifyNoInteractions(gitHubActivityRepository);
    }

    @Test
    void filtersByActivityType() {
        when(gitHubActivityRepository.findByActivityType(eq("PushEvent"), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<GitHubActivityResponse> response = gitHubActivityService.filterGitHubActivities(
                "PushEvent", null, null, 0, 10, "occurredAt"
        );

        assertThat(response).isEmpty();
        verify(gitHubActivityRepository).findByActivityType(eq("PushEvent"), any(Pageable.class));
        verify(gitHubActivityRepository, never())
                .findByOccurredAtGreaterThanEqualAndOccurredAtLessThan(any(), any(), any(Pageable.class));
    }

    private CreateGitHubActivityRequest validRequest() {
        return new CreateGitHubActivityRequest(
                "PullRequestOpened",
                "lyftix",
                "octocat",
                Instant.parse("2026-09-01T12:00:00Z"),
                "github-event-42",
                "Open pull request #42"
        );
    }

    private GitHubActivity savedActivity() {
        GitHubActivity activity = mock(GitHubActivity.class);
        when(activity.getId()).thenReturn(42L);
        when(activity.getActivityType()).thenReturn("PullRequestOpened");
        when(activity.getRepositoryName()).thenReturn("lyftix");
        when(activity.getRepositoryOwner()).thenReturn("octocat");
        when(activity.getOccurredAt()).thenReturn(Instant.parse("2026-09-01T12:00:00Z"));
        when(activity.getExternalId()).thenReturn("github-event-42");
        when(activity.getTitle()).thenReturn("Open pull request #42");
        when(activity.getCreatedAt()).thenReturn(Instant.parse("2026-09-01T12:00:01Z"));
        when(activity.getUpdatedAt()).thenReturn(Instant.parse("2026-09-01T12:00:02Z"));
        return activity;
    }
}
