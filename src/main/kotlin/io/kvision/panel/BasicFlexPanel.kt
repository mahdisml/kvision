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

import io.kvision.core.*
import io.kvision.utils.px

/**
 * The container with CSS flexbox layout support.
 *
 * This container is not annotated with a @WidgetMarker.
 * It should be used only as a base class for other components.
 *
 * @constructor
 * @param direction flexbox direction
 * @param wrap flexbox wrap
 * @param justify flexbox content justification
 * @param alignItems flexbox items alignment
 * @param alignContent flexbox content alignment
 * @param spacing spacing between columns/rows
 * @param noWrappers do not use additional div wrappers for child items
 * @param classes a set of CSS class names
 * @param init an initializer extension function
 */
@Suppress("LeakingThis")
@ExperimentalNonDslContainer
open class BasicFlexPanel(
    direction: FlexDirection? = null,
    wrap: FlexWrap? = null,
    justify: JustifyContent? = null,
    alignItems: AlignItems? = null,
    alignContent: AlignContent? = null,
    spacing: Int? = null,
    private val noWrappers: Boolean = false,
    classes: Set<String> = setOf(),
    init: (BasicFlexPanel.() -> Unit)? = null
) : BasicPanel(classes) {

    /**
     * The spacing between columns/rows.
     */
    var spacing by refreshOnUpdate(spacing) { refreshSpacing(); refresh() }

    init {
        this.display = Display.FLEX
        this.flexDirection = direction
        this.flexWrap = wrap
        this.justifyContent = justify
        this.alignItems = alignItems
        this.alignContent = alignContent
        init?.invoke(this)
    }

    /**
     * Adds a component to the flexbox container.
     * @param child child component
     * @param order child flexbox ordering
     * @param grow child flexbox grow
     * @param shrink child flexbox shrink
     * @param basis child flexbox basis
     * @param alignSelf child self alignment
     * @param classes a set of CSS class names
     */
    @Suppress("LongParameterList")
    fun add(
        child: Component, order: Int? = null, grow: Int? = null, shrink: Int? = null,
        basis: CssSize? = null, alignSelf: AlignItems? = null, classes: Set<String> = setOf()
    ): BasicFlexPanel {
        val wrapper = if (noWrappers) {
            child
        } else {
            WidgetWrapper(child, classes)
        }
        (wrapper as? Widget)?.let {
            applySpacing(it)
            it.order = order
            it.flexGrow = grow
            it.flexShrink = shrink
            it.flexBasis = basis
            it.alignSelf = alignSelf
        }
        addInternal(wrapper)
        return this
    }

    /**
     * DSL function to add components with additional options.
     * @param builder DSL builder function
     */
    open fun options(
        order: Int? = null, grow: Int? = null, shrink: Int? = null,
        basis: CssSize? = null, alignSelf: AlignItems? = null, classes: Set<String> = setOf(),
        builder: Container.() -> Unit
    ) {
        object : Container by this@BasicFlexPanel {
            override fun add(child: Component): Container {
                return add(child, order, grow, shrink, basis, alignSelf, classes)
            }
        }.builder()
    }

    private fun refreshSpacing() {
        getChildren().filterIsInstance<Widget>().map { applySpacing(it) }
    }

    private fun applySpacing(wrapper: Widget): Widget {
        if (!noWrappers) {
            wrapper.marginTop = null
            wrapper.marginRight = null
            wrapper.marginBottom = null
            wrapper.marginLeft = null
        }
        spacing?.let {
            when (flexDirection) {
                FlexDirection.COLUMN -> wrapper.marginBottom = it.px
                FlexDirection.ROWREV -> {
                    if (justifyContent == JustifyContent.FLEXEND) wrapper.marginRight = it.px else wrapper.marginLeft =
                        it.px
                }
                FlexDirection.COLUMNREV -> wrapper.marginTop = it.px
                else -> {
                    if (justifyContent == JustifyContent.FLEXEND) wrapper.marginLeft = it.px else wrapper.marginRight =
                        it.px
                }
            }
        }
        return wrapper
    }

    override fun add(child: Component): BasicFlexPanel {
        return add(child, null)
    }

    override fun addAll(children: List<Component>): BasicFlexPanel {
        children.forEach { add(it, null) }
        return this
    }

    override fun remove(child: Component): BasicFlexPanel {
        if (children.contains(child)) {
            super.remove(child)
        } else {
            children.find { (it as? WidgetWrapper)?.wrapped == child }?.let {
                super.remove(it)
                it.dispose()
            }
        }
        return this
    }

    override fun removeAll(): BasicFlexPanel {
        children.map {
            it.clearParent()
            (it as? WidgetWrapper)?.dispose()
        }
        children.clear()
        refresh()
        return this
    }

    override fun disposeAll(): BasicFlexPanel {
        children.map {
            (it as? WidgetWrapper)?.let {
                it.wrapped?.dispose()
            }
        }
        return removeAll()
    }

    override fun dispose() {
        children.map {
            (it as? WidgetWrapper)?.let {
                it.wrapped?.dispose()
            }
        }
        super.dispose()
    }
}
