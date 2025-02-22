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

import io.kvision.core.Component
import io.kvision.core.Container
import io.kvision.core.ExperimentalNonDslContainer
import io.kvision.core.WidgetWrapper
import io.kvision.html.Align
import io.kvision.html.TAG
import io.kvision.html.Tag
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

/**
 * The container with support for Bootstrap responsive grid layout.
 *
 * @constructor
 * @param gridSize grid size
 * @param rows number of rows
 * @param cols number of columns
 * @param align text align of grid cells
 * @param classes a set of CSS class names
 * @param init an initializer extension function
 */
@ExperimentalNonDslContainer
open class BasicResponsiveGridPanel(
    private val gridSize: GridSize = GridSize.MD,
    private var rows: Int = 0, private var cols: Int = 0, align: Align? = null,
    classes: Set<String> = setOf(), init: (BasicResponsiveGridPanel.() -> Unit)? = null
) : BasicPanel(classes + "container-fluid") {

    /**
     * Text align of grid cells.
     */
    var align by refreshOnUpdate(align) { refreshRowContainers() }

    internal val map = mutableMapOf<Int, MutableMap<Int, WidgetParam>>()
    private var auto: Boolean = true

    init {
        @Suppress("LeakingThis")
        init?.invoke(this)
    }

    /**
     * Adds child component to the grid.
     * @param child child component
     * @param col column number
     * @param row row number
     * @param size cell size (colspan)
     * @param offset cell offset
     * @return this container
     */
    open fun add(child: Component, col: Int, row: Int, size: Int = 0, offset: Int = 0): BasicResponsiveGridPanel {
        val cRow = maxOf(row, 1)
        val cCol = maxOf(col, 1)
        if (cRow > rows) rows = cRow
        if (cCol > cols) cols = cCol
        map.getOrPut(cRow) { mutableMapOf() }[cCol] = WidgetParam(child, size, offset)
        if (size > 0 || offset > 0) auto = false
        refreshRowContainers()
        return this
    }

    /**
     * DSL function to add components with additional options.
     * @param builder DSL builder function
     */
    open fun options(
        col: Int, row: Int, size: Int = 0, offset: Int = 0,
        builder: Container.() -> Unit
    ) {
        object : Container by this@BasicResponsiveGridPanel {
            override fun add(child: Component): Container {
                return add(child, col, row, size, offset)
            }
        }.builder()
    }

    override fun add(child: Component): BasicResponsiveGridPanel {
        return this.add(child, this.cols, 0)
    }

    override fun addAll(children: List<Component>): BasicResponsiveGridPanel {
        children.forEach { this.add(it) }
        return this
    }

    @Suppress("NestedBlockDepth")
    override fun remove(child: Component): BasicResponsiveGridPanel {
        map.values.forEach { row ->
            row.filterValues { it.widget == child }
                .forEach { (i, _) -> row.remove(i) }
        }
        refreshRowContainers()
        return this
    }

    /**
     * Removes child component at given location (column, row).
     * @param col column number
     * @param row row number
     * @return this container
     */
    open fun removeAt(col: Int, row: Int): BasicResponsiveGridPanel {
        map[row]?.remove(col)
        refreshRowContainers()
        return this
    }

    @Suppress("ComplexMethod", "NestedBlockDepth")
    private fun refreshRowContainers() {
        singleRender {
            disposeAll()
            val num = MAX_COLUMNS / cols
            for (i in 1..rows) {
                val rowContainer = SimplePanel(setOf("row"))
                val row = map[i]
                if (row != null) {
                    (1..cols).map { row[it] }.forEach { wp ->
                        if (auto) {
                            val widget = wp?.widget?.let {
                                WidgetWrapper(it, setOf("col-" + gridSize.size + "-" + num))
                            } ?: Tag(TAG.DIV, classes = setOf("col-" + gridSize.size + "-" + num))
                            align?.let {
                                widget.addCssClass(it.className)
                            }
                            rowContainer.add(widget)
                        } else {
                            if (wp != null) {
                                val s = if (wp.size > 0) wp.size else num
                                val widget = WidgetWrapper(wp.widget, setOf("col-" + gridSize.size + "-" + s))
                                if (wp.offset > 0) {
                                    widget.addCssClass("offset-" + gridSize.size + "-" + wp.offset)
                                }
                                align?.let {
                                    widget.addCssClass(it.className)
                                }
                                rowContainer.add(widget)
                            }
                        }
                    }
                }
                addInternal(rowContainer)
            }
        }
    }
}
