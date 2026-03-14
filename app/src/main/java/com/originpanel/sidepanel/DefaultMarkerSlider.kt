package com.originpanel.sidepanel

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.slider.Slider

/**
 * A custom Material Slider.
 */
class DefaultMarkerSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.sliderStyle
) : Slider(context, attrs, defStyleAttr)
