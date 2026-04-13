package ch.uzh.ifi.hase.soprafs26.service;

import ch.uzh.ifi.hase.soprafs26.repository.GuestUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

class GuestUserCleanupServiceTest {

    @Mock
    private GuestUserRepository guestUserRepository;

    private GuestUserCleanupService guestUserCleanupService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        guestUserCleanupService = new GuestUserCleanupService(guestUserRepository);
    }

    @Test
    void deleteExpiredGuests_callsRepositoryDelete() {
        Mockito.when(guestUserRepository.deleteByExpiresAtBefore(any())).thenReturn(2L);

        guestUserCleanupService.deleteExpiredGuests();

        Mockito.verify(guestUserRepository, times(1)).deleteByExpiresAtBefore(any());
    }

    @Test
    void deleteExpiredGuests_whenNothingExpired_stillCallsRepositoryDelete() {
        Mockito.when(guestUserRepository.deleteByExpiresAtBefore(any())).thenReturn(0L);

        guestUserCleanupService.deleteExpiredGuests();

        Mockito.verify(guestUserRepository, times(1)).deleteByExpiresAtBefore(any());
    }
}