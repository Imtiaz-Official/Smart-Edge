package com.imi.smartedge.sidebar.panel

import android.content.Context
import android.graphics.drawable.Drawable
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey

/**
 * Custom model for Glide to load an app icon by its package name and icon pack.
 * Including iconPack in the model ensures Glide treats different packs as different images.
 */
data class AppIconRequest(val packageName: String, val iconPack: String)

/**
 * Fetches the actual Drawable from the PackageManager/IconPackManager.
 */
class AppIconDataFetcher(private val context: Context, private val request: AppIconRequest) : DataFetcher<Drawable> {
    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in Drawable>) {
        try {
            val repository = AppRepository(context)
            // The repository already uses panelPrefs.selectedIconPack, but we can pass it explicitly
            // if we want to be even more surgical. For now, loadIconForAppSync handles it.
            val icon = repository.loadIconForAppSync(request.packageName)
            if (icon != null) {
                callback.onDataReady(icon)
            } else {
                callback.onLoadFailed(Exception("Failed to load icon for ${request.packageName}"))
            }
        } catch (e: Exception) {
            callback.onLoadFailed(e)
        }
    }

    override fun cleanup() {}
    override fun cancel() {}
    override fun getDataClass(): Class<Drawable> = Drawable::class.java
    override fun getDataSource(): DataSource = DataSource.LOCAL
}

/**
 * Loader that links the AppIconRequest model to the AppIconDataFetcher.
 */
class AppIconModelLoader(private val context: Context) : ModelLoader<AppIconRequest, Drawable> {
    override fun buildLoadData(model: AppIconRequest, width: Int, height: Int, options: Options): ModelLoader.LoadData<Drawable> {
        // UNIQUE KEY: Package + IconPack ensures seamless switching without manual cache clearing
        val uniqueKey = "${model.packageName}|${model.iconPack}"
        return ModelLoader.LoadData(ObjectKey(uniqueKey), AppIconDataFetcher(context, model))
    }

    override fun handles(model: AppIconRequest): Boolean = true

    class Factory(private val context: Context) : ModelLoaderFactory<AppIconRequest, Drawable> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<AppIconRequest, Drawable> {
            return AppIconModelLoader(context)
        }
        override fun teardown() {}
    }
}
