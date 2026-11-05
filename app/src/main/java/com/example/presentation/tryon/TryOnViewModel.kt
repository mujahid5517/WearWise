package com.example.presentation.tryon

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.ClothingItem
import com.example.domain.model.Outfit
import com.example.domain.model.SelectedTryOnGarment
import com.example.domain.model.TryOnPreview
import com.example.domain.repository.OutfitRepository
import com.example.domain.usecase.ExecuteTryOnUseCase
import com.example.domain.usecase.ManageWardrobeUseCase
import com.example.domain.usecase.UserProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TryOnViewModel(
    private val executeTryOnUseCase: ExecuteTryOnUseCase,
    private val manageWardrobeUseCase: ManageWardrobeUseCase,
    private val userProfileUseCase: UserProfileUseCase,
    private val outfitRepository: OutfitRepository
) : ViewModel() {

    private val _currentPreview = MutableStateFlow<TryOnPreview?>(null)
    val currentPreview: StateFlow<TryOnPreview?> = _currentPreview.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _userPhotoUri = MutableStateFlow("")
    val userPhotoUri: StateFlow<String> = _userPhotoUri.asStateFlow()

    private val _fitType = MutableStateFlow("Tailored")
    val fitType: StateFlow<String> = _fitType.asStateFlow()

    private val _pose = MutableStateFlow("Confident Stance")
    val pose: StateFlow<String> = _pose.asStateFlow()

    private val _lighting = MutableStateFlow("Warm Daylight")
    val lighting: StateFlow<String> = _lighting.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _garments = MutableStateFlow<List<SelectedTryOnGarment>>(emptyList())
    val garments: StateFlow<List<SelectedTryOnGarment>> = _garments.asStateFlow()

    val wardrobeItems: StateFlow<List<ClothingItem>> = manageWardrobeUseCase.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val outfits: StateFlow<List<Outfit>> = outfitRepository.getAllOutfits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tryOnHistory: StateFlow<List<TryOnPreview>> = executeTryOnUseCase.getHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _photoError = MutableStateFlow<String?>(null)
    val photoError: StateFlow<String?> = _photoError.asStateFlow()

    private val _privacyAccepted = MutableStateFlow(true)
    val privacyAccepted: StateFlow<Boolean> = _privacyAccepted.asStateFlow()

    init {
        viewModelScope.launch {
            userProfileUseCase.getProfile().collect { profile ->
                val img = profile?.profileImagePath ?: profile?.profileImage
                if (img != null && _userPhotoUri.value.isBlank()) {
                    _userPhotoUri.value = img
                }
            }
        }
    }

    fun importPersonPhoto(uri: String) {
        viewModelScope.launch {
            try {
                val persisted = executeTryOnUseCase.persistPersonPhoto(uri)
                _userPhotoUri.value = persisted
                _photoError.value = null
            } catch (e: Exception) {
                _photoError.value = e.message ?: "Failed to load person photo"
            }
        }
    }

    fun importGarmentPhoto(uri: String, category: String) {
        viewModelScope.launch {
            try {
                executeTryOnUseCase.persistGarmentPhoto(uri)
                val newItem = SelectedTryOnGarment(
                    slot = category,
                    name = "Custom $category",
                    category = category,
                    imageRes = null
                )
                _garments.value = _garments.value + newItem
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun setPhotoError(msg: String?) {
        _photoError.value = msg
    }

    fun rotatePersonPhoto(degrees: Int) {
        viewModelScope.launch {
            if (_userPhotoUri.value.isNotBlank()) {
                val rotated = executeTryOnUseCase.rotatePersonPhoto(_userPhotoUri.value, degrees)
                _userPhotoUri.value = rotated
            }
        }
    }

    fun clearUserPhoto() {
        _userPhotoUri.value = ""
    }

    fun addGarment(item: ClothingItem) {
        val selected = SelectedTryOnGarment(
            slot = item.category,
            name = item.name,
            category = item.category,
            imageRes = item.imageRes
        )
        if (!_garments.value.any { it.name == item.name }) {
            _garments.value = _garments.value + selected
        }
    }

    fun applyOutfit(outfit: Outfit) {
        val mapped = outfit.items.map {
            SelectedTryOnGarment(
                slot = it.category,
                name = it.name,
                category = it.category,
                imageRes = it.imageRes
            )
        }
        _garments.value = mapped
    }

    fun removeGarment(garment: SelectedTryOnGarment) {
        _garments.value = _garments.value.filterNot { it.name == garment.name }
    }

    fun setFitType(ft: String) {
        _fitType.value = ft
    }

    fun setPose(p: String) {
        _pose.value = p
    }

    fun setLighting(l: String) {
        _lighting.value = l
    }

    fun generateTryOn() {
        viewModelScope.launch {
            _isGenerating.value = true
            _errorMessage.value = null
            _statusMessage.value = "Generating virtual try-on with Gemini..."
            try {
                val preview = executeTryOnUseCase.execute(
                    outfitId = 0L,
                    fitType = _fitType.value,
                    pose = _pose.value,
                    lighting = _lighting.value,
                    userPhotoUri = _userPhotoUri.value,
                    garments = _garments.value
                )
                _currentPreview.value = preview
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Try-on generation failed."
            } finally {
                _isGenerating.value = false
                _statusMessage.value = null
            }
        }
    }

    fun cancelGeneration() {
        _isGenerating.value = false
        _statusMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
        _photoError.value = null
    }

    fun toggleSavedLook() {
        val prev = _currentPreview.value ?: return
        val updated = !prev.isSavedLook
        viewModelScope.launch {
            executeTryOnUseCase.setSavedLook(prev.id, updated)
            _currentPreview.value = prev.copy(isSavedLook = updated)
        }
    }

    suspend fun saveToGallery(): Result<Uri> {
        val path = _currentPreview.value?.generatedImagePath ?: return Result.failure(IllegalStateException("No generated image"))
        return runCatching {
            executeTryOnUseCase.saveGeneratedToGallery(path)
        }
    }

    fun saveToGallery(onSaved: (Uri?) -> Unit) {
        val path = _currentPreview.value?.generatedImagePath ?: run { onSaved(null); return }
        viewModelScope.launch {
            val uri = try {
                executeTryOnUseCase.saveGeneratedToGallery(path)
            } catch (_: Exception) {
                null
            }
            onSaved(uri)
        }
    }

    fun shareUriOrNull(): Uri? {
        val path = _currentPreview.value?.generatedImagePath ?: return null
        return try {
            executeTryOnUseCase.shareUriForGenerated(path)
        } catch (_: Exception) {
            null
        }
    }

    fun selectHistoryPreview(preview: TryOnPreview) {
        _currentPreview.value = preview
    }

    fun deleteHistoryEntry(id: Long) {
        viewModelScope.launch {
            executeTryOnUseCase.deleteHistoryEntry(id)
            if (_currentPreview.value?.id == id) {
                _currentPreview.value = null
            }
        }
    }

    fun cropPersonPhoto(left: Float, top: Float, right: Float, bottom: Float) {
        viewModelScope.launch {
            if (_userPhotoUri.value.isNotBlank()) {
                val cropped = executeTryOnUseCase.cropPersonPhoto(_userPhotoUri.value, left, top, right, bottom)
                _userPhotoUri.value = cropped
            }
        }
    }

    fun acceptPrivacy() {
        _privacyAccepted.value = true
    }
}
