package ai.teammates.rayachat.core.adapters

import ai.teammates.rayachat.core.models.ImageAsset

/** Pluggable adapter for image selection. Image/paperclip button hidden if not provided. */
interface ImagePickerAdapter {
    suspend fun pickImages(maxCount: Int): List<ImageAsset>
}
