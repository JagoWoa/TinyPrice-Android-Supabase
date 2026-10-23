package com.example.tinyprice_android_supabase.presentation.deals

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tinyprice_android_supabase.data.repository.PreviewDealsRepository
import com.example.tinyprice_android_supabase.ui.theme.TinyPriceAndroidSupabaseTheme
import org.junit.Rule
import org.junit.Test

class DealsAppTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun startScreenShowsSearchAndSuggestion() {
        composeRule.setContent {
            TinyPriceAndroidSupabaseTheme {
                DealsApp(state = DealsUiState(), onEvent = {})
            }
        }

        composeRule.onNodeWithText("Buscar").assertIsDisplayed()
        composeRule.onNodeWithText("Nintendo Switch OLED").assertIsDisplayed()
    }

    @Test fun emptySearchShowsValidation() {
        renderDemo()
        composeRule.onNodeWithText("Buscar", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Escribe un producto para iniciar la búsqueda.").assertIsDisplayed()
    }

    @Test fun demoSearchOpensDetailsAndReturnsToResults() {
        renderDemo()
        composeRule.onNodeWithTag("search-input").performTextInput("Nintendo Switch OLED")
        composeRule.onNodeWithText("Buscar", useUnmergedTree = true).performClick()
        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodes(androidx.compose.ui.test.hasText("3 resultados de demostración"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Nintendo Switch OLED · Blanca").performClick()
        composeRule.onNodeWithText("Detalle de oferta").assertIsDisplayed()
        composeRule.onNodeWithText("Fuente").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("‹  Volver").performClick()
        composeRule.onNodeWithText("3 resultados de demostración").assertIsDisplayed()
    }

    @Test fun liveStateDoesNotShowDemoLabel() {
        composeRule.setContent {
            TinyPriceAndroidSupabaseTheme {
                DealsApp(state = DealsUiState(isPreviewData = false), onEvent = {})
            }
        }
        composeRule.onNodeWithText("EN VIVO · BÚSQUEDA WEB").assertIsDisplayed()
    }

    private fun renderDemo() {
        composeRule.setContent {
            val viewModel = remember { DealsViewModel(PreviewDealsRepository) }
            val state by viewModel.state.collectAsStateWithLifecycle()
            TinyPriceAndroidSupabaseTheme {
                DealsApp(state = state, onEvent = viewModel::onEvent)
            }
        }
    }
}
