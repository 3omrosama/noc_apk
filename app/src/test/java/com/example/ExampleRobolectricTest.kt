package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.User
import com.example.data.security.SecureSessionManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("InfraManager Mobile", appName)
  }

  @Test
  fun `secure session manager stores and clears session`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sessionManager = SecureSessionManager(context)

    sessionManager.saveServerUrl("https://noc.example.com")
    assertEquals("https://noc.example.com", sessionManager.getServerUrl())

    val testUser = User(id = "1", username = "admin", role = "admin")
    sessionManager.saveSession("jwt_test_token_123", "refresh_test_token_456", testUser)

    assertTrue(sessionManager.hasValidSession())
    assertEquals("jwt_test_token_123", sessionManager.getToken())
    assertEquals("refresh_test_token_456", sessionManager.getRefreshToken())
    assertEquals("admin", sessionManager.getUser()?.username)
    assertEquals("admin", sessionManager.getUser()?.role)

    sessionManager.clearSession()
    assertFalse(sessionManager.hasValidSession())
    assertNull(sessionManager.getToken())
  }
}

