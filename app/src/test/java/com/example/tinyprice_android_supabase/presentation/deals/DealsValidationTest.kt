package com.example.tinyprice_android_supabase.presentation.deals

import com.example.tinyprice_android_supabase.data.model.ProductOffer
import com.example.tinyprice_android_supabase.data.repository.DealsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DealsValidationTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test fun invalidLengthsNeverReachRepository() = runTest {
        var calls = 0
        val viewModel = DealsViewModel(repository { calls++; emptyList() })
        for (query in listOf("x", "x".repeat(201))) {
            viewModel.onEvent(DealsEvent.QueryChanged(query))
            viewModel.onEvent(DealsEvent.SubmitSearch)
            advanceUntilIdle()
            assertNotNull(viewModel.state.value.errorMessage)
        }
        assertEquals(0, calls)
    }

    @Test fun trimsQueryBeforeRequest() = runTest {
        var received = ""
        val viewModel = DealsViewModel(repository { received = it; emptyList() })
        viewModel.onEvent(DealsEvent.UseSuggestion("  Switch OLED  "))
        advanceUntilIdle()
        assertEquals("Switch OLED", received)
        assertEquals("Switch OLED", viewModel.state.value.submittedQuery)
    }

    @Test fun blankSubmissionCancelsPendingSearch() = runTest {
        var completed = false
        val viewModel = DealsViewModel(repository { delay(1000); completed = true; emptyList() })
        viewModel.onEvent(DealsEvent.UseSuggestion("Switch OLED"))
        runCurrent()
        assertTrue(viewModel.state.value.isLoading)
        viewModel.onEvent(DealsEvent.QueryChanged(""))
        viewModel.onEvent(DealsEvent.SubmitSearch)
        advanceUntilIdle()
        assertFalse(completed)
        assertFalse(viewModel.state.value.isLoading)
        assertNotNull(viewModel.state.value.errorMessage)
    }

    @Test fun latestSearchCancelsPreviousRequest() = runTest {
        val completed = mutableListOf<String>()
        val viewModel = DealsViewModel(repository { query -> delay(1000); completed += query; emptyList() })
        viewModel.onEvent(DealsEvent.UseSuggestion("Primera"))
        runCurrent()
        viewModel.onEvent(DealsEvent.UseSuggestion("Segunda"))
        advanceUntilIdle()
        assertEquals(listOf("Segunda"), completed)
        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test fun editingInputDoesNotRelabelPreviousResults() = runTest {
        val viewModel = DealsViewModel(repository { emptyList() })
        viewModel.onEvent(DealsEvent.UseSuggestion("Switch OLED"))
        advanceUntilIdle()
        viewModel.onEvent(DealsEvent.QueryChanged("Laptop"))
        assertEquals("Switch OLED", viewModel.state.value.submittedQuery)
        assertEquals("Laptop", viewModel.state.value.query)
    }

    @Test fun unexpectedFailureLeavesRetryableState() = runTest {
        val viewModel = DealsViewModel(repository { error("unexpected") })
        viewModel.onEvent(DealsEvent.UseSuggestion("Switch"))
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isLoading)
        assertNotNull(viewModel.state.value.errorMessage)
    }

    private fun repository(block: suspend (String) -> List<ProductOffer>) = object : DealsRepository {
        override val isPreviewData = false
        override suspend fun search(query: String) = block(query)
    }
}
