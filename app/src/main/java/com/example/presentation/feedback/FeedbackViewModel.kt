package com.example.presentation.feedback

import androidx.lifecycle.ViewModel
import com.example.domain.repository.OutfitFeedbackRepository

class FeedbackViewModel(
    val feedbackRepository: OutfitFeedbackRepository
) : ViewModel()
