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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _userPhotoUri = MutableStateFlow<String?>(null)
    val userPhotoUri: StateFlow<String?> = _userPhotoUri.asStateFlow()

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

    private var generateJob: Job? = null

    init {
        viewModelScope.launch {
            userProfileUseCase.getProfile().collect { profile ->
                if (_userPhotoUri.value == null && profile?.profileImage != null) {
                    _userPhotoUri.value = profile.profileImage
                }
            }
        }
    }

    fun importPersonPhoto(uri: String) {
        viewModelScope.launch {
            runCatching {
                val path = executeTryOnUseCase.persistPersonPhoto(uri)
                _userPhotoUri.value = path
                _photoError.value = null
            }.onFailure {
                _photoError.value = it.message ?: "Failed to import photo"
            }
        }
    }

    fun importGarmentPhoto(uri: String, category: String) {
        viewModelScope.launch {
            try {
                val path = executeTryOnUseCase.persistGarmentPhoto(uri)
                val garment = SelectedTryOnGarment(
                    slot = category,
                    name = "Custom $category",
                    category = category,
                    imageUri = path
                )
                addGarmentInternal(garment)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _errorMessage.value = error.message ?: "Could not import the selected garment photo."
            }
        }
    }

    fun setPhotoError(msg: String?) {
        _photoError.value = msg
    }

    fun rotatePersonPhoto(degrees: Int) {
        val current = _userPhotoUri.value ?: return
        viewModelScope.launch {
            runCatching {
                val path = executeTryOnUseCase.rotatePersonPhoto(current, degrees)
                _userPhotoUri.value = path
            }
        }
    }

    fun clearUserPhoto() {
        _userPhotoUri.value = null
    }

    fun addGarment(item: ClothingItem) {
        val garment = SelectedTryOnGarment(
            slot = item.category,
            name = item.name,
            category = item.category,
            imageRes = item.imageRes,
            imageUri = item.imageUri,
            clothingItemId = item.id,
            imageUrl = item.imageUrl,
            catalogAssetName = item.catalogAssetName
        )
        addGarmentInternal(garment)
    }

    private fun addGarmentInternal(garment: SelectedTryOnGarment) {
        val current = _garments.value.toMutableList()
        current.removeAll { it.category.equals(garment.category, true) }
        current.add(garment)
        _garments.value = current
    }

    fun applyOutfit(outfit: Outfit) {
        val list = outfit.items.map { item ->
            SelectedTryOnGarment(
                slot = item.category,
                name = item.name,
                category = item.category,
                imageRes = item.imageRes,
                imageUri = item.imageUri,
                clothingItemId = item.id,
                imageUrl = item.imageUrl,
                catalogAssetName = item.catalogAssetName
            )
        }
        _garments.value = list
    }

    fun removeGarment(garment: SelectedTryOnGarment) {
        _garments.value = _garments.value.filterNot { it.name == garment.name && it.category == garment.category }
    }

    fun setFitType(fit: String) { _fitType.value = fit }
    fun setPose(pose: String) { _pose.value = pose }
    fun setLighting(lighting: String) { _lighting.value = lighting }

    fun generateTryOn() {
        if (_isGenerating.value) return
        generateJob = viewModelScope.launch {
            _isGenerating.value = true
            _statusMessage.value = "Synthesizing AI virtual try-on..."
            _errorMessage.value = null
            try {
                val preview = executeTryOnUseCase.execute(
                    outfitId = 0L,
                    fitType = _fitType.value,
                    pose = _pose.value,
                    lighting = _lighting.value,
                    userPhotoUri = _userPhotoUri.value.orEmpty(),
                    garments = _garments.value
                )
                _currentPreview.value = preview
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _errorMessage.value = error.message ?: "Try-on generation failed"
            } finally {
                _isGenerating.value = false
                _statusMessage.value = null
            }
        }
    }

    fun cancelGeneration() {
        generateJob?.cancel()
        _isGenerating.value = false
        _statusMessage.value = null
    }

    fun clearError() { _errorMessage.value = null }

    fun toggleSavedLook() {
        val current = _currentPreview.value ?: return
        val next = !current.isSavedLook
        _currentPreview.value = current.copy(isSavedLook = next)
        viewModelScope.launch {
            executeTryOnUseCase.setSavedLook(current.id, next)
        }
    }

    suspend fun saveToGallery(): Uri? {
        val path = _currentPreview.value?.generatedImagePath ?: return null
        return runCatching { executeTryOnUseCase.saveGeneratedToGallery(path) }.getOrNull()
    }

    fun shareUriOrNull(): Uri? {
        val path = _currentPreview.value?.generatedImagePath ?: return null
        return runCatching { executeTryOnUseCase.shareUriForGenerated(path) }.getOrNull()
    }

    fun selectHistoryPreview(preview: TryOnPreview) {
        _currentPreview.value = preview
    }

    fun deleteHistoryEntry(preview: TryOnPreview) {
        deleteHistoryEntry(preview.id)
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
        val current = _userPhotoUri.value ?: return
        viewModelScope.launch {
            runCatching {
                val cropped = executeTryOnUseCase.cropPersonPhoto(current, left, top, right, bottom)
                _userPhotoUri.value = cropped
            }
        }
    }

    fun acceptPrivacy() {
        _privacyAccepted.value = true
    }
}
