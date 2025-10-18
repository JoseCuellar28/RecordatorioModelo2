package com.example.recordatoriomodelo2.ui.screens.profile

import com.example.recordatoriomodelo2.ui.UserProfile
import com.example.recordatoriomodelo2.ui.userProfile
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileEditViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ProfileEditViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Setup initial user profile
        userProfile = UserProfile(
            fullName = "Juan Pérez",
            email = "juan.perez@test.com",
            phone = "+1234567890",
            institution = "Universidad Test",
            profileImageUrl = "https://test.com/image.jpg"
        )
        
        viewModel = ProfileEditViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should load current profile`() = runTest {
        // Given - setup is done in @Before
        
        // When
        val initialState = viewModel.uiState.first()
        
        // Then
        assertThat(initialState.fullName).isEqualTo("Juan Pérez")
        assertThat(initialState.email).isEqualTo("juan.perez@test.com")
        assertThat(initialState.phone).isEqualTo("+1234567890")
        assertThat(initialState.institution).isEqualTo("Universidad Test")
        assertThat(initialState.profileImageUrl).isEqualTo("https://test.com/image.jpg")
        assertThat(initialState.hasChanges).isFalse()
        assertThat(initialState.isLoading).isFalse()
    }

    @Test
    fun `updateFullName should update state and detect changes`() = runTest {
        // Given
        val newName = "María García"
        
        // When
        viewModel.updateFullName(newName)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.first()
        assertThat(state.fullName).isEqualTo(newName)
        assertThat(state.hasChanges).isTrue()
        assertThat(state.fullNameError).isEmpty()
    }

    @Test
    fun `updateEmail should update state and detect changes`() = runTest {
        // Given
        val newEmail = "maria.garcia@test.com"
        
        // When
        viewModel.updateEmail(newEmail)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.first()
        assertThat(state.email).isEqualTo(newEmail)
        assertThat(state.hasChanges).isTrue()
        assertThat(state.emailError).isEmpty()
    }

    @Test
    fun `updatePhone should update state and detect changes`() = runTest {
        // Given
        val newPhone = "+9876543210"
        
        // When
        viewModel.updatePhone(newPhone)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.first()
        assertThat(state.phone).isEqualTo(newPhone)
        assertThat(state.hasChanges).isTrue()
        assertThat(state.phoneError).isEmpty()
    }

    @Test
    fun `updateInstitution should update state and detect changes`() = runTest {
        // Given
        val newInstitution = "Nueva Universidad"
        
        // When
        viewModel.updateInstitution(newInstitution)
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.first()
        assertThat(state.institution).isEqualTo(newInstitution)
        assertThat(state.hasChanges).isTrue()
        assertThat(state.institutionError).isEmpty()
    }

    @Test
    fun `resetToOriginal should restore original values`() = runTest {
        // Given
        viewModel.updateFullName("Nuevo Nombre")
        viewModel.updateEmail("nuevo@email.com")
        advanceUntilIdle()
        
        // When
        viewModel.resetToOriginal()
        advanceUntilIdle()
        
        // Then
        val state = viewModel.uiState.first()
        assertThat(state.fullName).isEqualTo("Juan Pérez")
        assertThat(state.email).isEqualTo("juan.perez@test.com")
        assertThat(state.hasChanges).isFalse()
    }

    @Test
    fun `updateFullName should clear error when called`() = runTest {
        // Given
        viewModel.updateFullName("")
        advanceUntilIdle()
        
        // When
        val state = viewModel.uiState.first()
        
        // Then
        assertThat(state.fullName).isEqualTo("")
        assertThat(state.fullNameError).isEmpty()
    }

    @Test
    fun `updateEmail should clear error when called`() = runTest {
        // Given
        viewModel.updateEmail("invalid-email")
        advanceUntilIdle()
        
        // When
        val state = viewModel.uiState.first()
        
        // Then
        assertThat(state.email).isEqualTo("invalid-email")
        assertThat(state.emailError).isEmpty()
    }

    @Test
    fun `updateInstitution should clear error when called`() = runTest {
        // Given
        viewModel.updateInstitution("")
        advanceUntilIdle()
        
        // When
        val state = viewModel.uiState.first()
        
        // Then
        assertThat(state.institution).isEqualTo("")
        assertThat(state.institutionError).isEmpty()
    }
}