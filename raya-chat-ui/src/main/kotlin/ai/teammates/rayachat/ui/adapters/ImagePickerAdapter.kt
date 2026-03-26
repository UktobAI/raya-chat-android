package ai.teammates.rayachat.ui.adapters

import ai.teammates.rayachat.core.models.ImageAsset

/** Pluggable adapter for image selection. Hidden if not provided. */
interface ImagePickerAdapter {
    suspend fun pickImages(maxCount: Int): List<ImageAsset>
}
