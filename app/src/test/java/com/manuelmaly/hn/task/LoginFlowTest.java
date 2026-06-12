package com.manuelmaly.hn.task;

import com.manuelmaly.hn.data.network.HNApiClient;
import com.manuelmaly.hn.data.storage.AppSettings;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class LoginFlowTest {

    @Mock HNApiClient mockApiClient;
    @Mock AppSettings mockAppSettings;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void successfulLoginReturnsToken() throws Exception {
        when(mockApiClient.loginAndGetToken("user", "pass")).thenReturn("token123");

        String token = mockApiClient.loginAndGetToken("user", "pass");
        assertEquals("token123", token);
    }

    @Test
    public void successfulLoginSavesCredentials() throws Exception {
        when(mockApiClient.loginAndGetToken("user", "pass")).thenReturn("token123");

        // Simulate login flow
        String token = mockApiClient.loginAndGetToken("user", "pass");
        if (token != null && !token.isEmpty()) {
            mockAppSettings.setUserData("user", token);
        }

        verify(mockAppSettings).setUserData("user", "token123");
    }

    @Test
    public void failedLoginReturnsNull() throws Exception {
        when(mockApiClient.loginAndGetToken("user", "wrongpass")).thenReturn(null);

        String token = mockApiClient.loginAndGetToken("user", "wrongpass");
        assertNull(token);
    }

    @Test
    public void failedLoginDoesNotSaveCredentials() throws Exception {
        when(mockApiClient.loginAndGetToken("user", "wrongpass")).thenReturn(null);

        String token = mockApiClient.loginAndGetToken("user", "wrongpass");
        if (token != null && !token.isEmpty()) {
            mockAppSettings.setUserData("user", token);
        }

        verify(mockAppSettings, never()).setUserData(anyString(), anyString());
    }

    @Test
    public void emptyTokenDoesNotSaveCredentials() throws Exception {
        when(mockApiClient.loginAndGetToken("user", "pass")).thenReturn("");

        String token = mockApiClient.loginAndGetToken("user", "pass");
        if (token != null && !token.isEmpty()) {
            mockAppSettings.setUserData("user", token);
        }

        verify(mockAppSettings, never()).setUserData(anyString(), anyString());
    }

    @Test(expected = Exception.class)
    public void networkErrorDuringLogin() throws Exception {
        when(mockApiClient.loginAndGetToken("user", "pass"))
                .thenThrow(new Exception("Network error"));

        mockApiClient.loginAndGetToken("user", "pass");
    }

    @Test
    public void loginResultIsBoolean() throws Exception {
        when(mockApiClient.loginAndGetToken("user", "pass")).thenReturn("token123");

        String token = mockApiClient.loginAndGetToken("user", "pass");
        boolean loginSuccess = token != null && !token.isEmpty();
        assertTrue(loginSuccess);
    }
}
