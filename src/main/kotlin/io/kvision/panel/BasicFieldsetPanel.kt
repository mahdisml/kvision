/*
 * Copyright (c) 2017-present Robert Jaros
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.kvision.panel

import com.github.snabbdom.VNode
import io.kvision.core.ExperimentalNonDslContainer
import io.kvision.html.TAG
import io.kvision.html.Tag

/**
 * The HTML fieldset container.
 *
 * This container is not annotated with a @WidgetMarker.
 * It should be used only as a base class for other components.
 *
 * @constructor
 * @param legend the legend of the fieldset
 * @param classes a set of CSS class names
 * @param init an initializer extension function
 */
@ExperimentalNonDslContainer
open class BasicFieldsetPanel(
    legend: String? = null,
    classes: Set<String> = setOf(),
    init: (BasicFieldsetPanel.() -> Unit)? = null
) :
    BasicPanel(classes = classes + "kv_fieldset") {

    /**
     * The legend of the fieldset.
     */
    var legend
        get() = legendComponent.content
        set(value) {
            legendComponent.content = value
        }

    /**
     * The legend component.
     */
    protected val legendComponent = Tag(TAG.LEGEND, legend)

    init {
        @Suppress("LeakingThis")
        init?.invoke(this)
    }

    override fun render(): VNode {
        val childrenVNodes = childrenVNodes()
        childrenVNodes.asDynamic().unshift(legendComponent.renderVNode())
        return render("fieldset", childrenVNodes)
    }
}
