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
package io.kvision.form.select

import com.github.snabbdom.VNode
import io.kvision.core.AttributeSetBuilder
import io.kvision.core.ClassSetBuilder
import io.kvision.core.Container
import io.kvision.core.StringPair
import io.kvision.form.FormInput
import io.kvision.form.InputSize
import io.kvision.form.ValidationStatus
import io.kvision.html.TAG
import io.kvision.html.Tag
import io.kvision.panel.SimplePanel
import io.kvision.state.MutableState
import io.kvision.state.ObservableState
import io.kvision.state.bind
import io.kvision.utils.set

internal const val KVNULL = "#kvnull"

/**
 * Simple select component.
 *
 * @constructor
 * @param options an optional list of options (value to label pairs) for the select control
 * @param value select input value
 * @param emptyOption determines if an empty option is automatically generated
 * @param multiple allows multiple value selection (multiple values are comma delimited)
 * @param selectSize the number of visible options
 * @param classes a set of CSS class names
 * @param init an initializer extension function
 */
open class SimpleSelectInput(
    options: List<StringPair>? = null, value: String? = null, emptyOption: Boolean = false,
    multiple: Boolean = false,
    selectSize: Int? = null,
    classes: Set<String> = setOf(), init: (SimpleSelectInput.() -> Unit)? = null
) : SimplePanel(classes + "form-control"), FormInput, MutableState<String?> {

    protected val observers = mutableListOf<(String?) -> Unit>()

    /**
     * A list of options (value to label pairs) for the select control.
     */
    var options by refreshOnUpdate(options) { setChildrenFromOptions() }

    /**
     * Text input value.
     */
    var value by refreshOnUpdate(value) { refreshState(); observers.forEach { ob -> ob(it) } }

    /**
     * The value of the selected child option.
     *
     * This value is placed directly in the generated HTML code, while the [value] property is dynamically
     * bound to the select component.
     */
    var startValue by refreshOnUpdate(value) { this.value = it; selectOption() }

    /**
     * The name attribute of the generated HTML input element.
     */
    override var name: String? by refreshOnUpdate()

    /**
     * Determines if the field is disabled.
     */
    override var disabled by refreshOnUpdate(false)

    /**
     * Determines if the text input is automatically focused.
     */
    var autofocus: Boolean? by refreshOnUpdate()

    /**
     * Determines if an empty option is automatically generated.
     */
    var emptyOption by refreshOnUpdate(emptyOption) { setChildrenFromOptions() }

    /**
     * Determines if multiple value selection is allowed.
     */
    var multiple by refreshOnUpdate(multiple)

    /**
     * The number of visible options.
     */
    var selectSize: Int? by refreshOnUpdate(selectSize)

    /**
     * The size of the input.
     */
    override var size: InputSize? by refreshOnUpdate()

    /**
     * The validation status of the input.
     */
    override var validationStatus: ValidationStatus? by refreshOnUpdate()

    /**
     * The index of currently selected option or -1 if none.
     */
    @Suppress("UnsafeCastFromDynamic")
    var selectedIndex: Int
        get() = getElement()?.asDynamic()?.selectedIndex
            ?: value?.let { v ->
                val emptyIndex = if (emptyOption) 1 else 0
                options?.map(StringPair::first)?.indexOf(v)?.let { it + emptyIndex }
            } ?: -1
        set(value) {
            getElement()?.asDynamic()?.selectedIndex = value
            if (value == -1) this.value = null
            options?.getOrNull(value)?.let {
                this.value = it.first
            }
        }

    init {
        setChildrenFromOptions()
        this.setInternalEventListener<SimpleSelectInput> {
            change = {
                val v = getElementJQuery()?.`val`()
                self.value = v?.let {
                    calculateValue(it)
                }
            }
        }
        @Suppress("LeakingThis")
        init?.invoke(this)
    }

    protected open fun calculateValue(v: Any): String? {
        return if (this.multiple) {
            @Suppress("UNCHECKED_CAST")
            val arr = v as? Array<String>
            if (arr != null && arr.isNotEmpty()) {
                arr.filter { it != "" }.joinToString(",")
            } else {
                null
            }
        } else {
            val vs = v as String?
            if (vs != null && vs != "" && vs != KVNULL) {
                vs
            } else {
                null
            }
        }
    }

    override fun render(): VNode {
        return render("select", childrenVNodes())
    }

    private fun setChildrenFromOptions() {
        super.removeAll()
        if (emptyOption) {
            super.add(Tag(TAG.OPTION, "", attributes = mapOf("value" to KVNULL)))
        }
        val valueSet = if (this.multiple) value?.split(",") ?: emptySet() else setOf(value)
        options?.let {
            val c = it.map {
                val attributes = if (valueSet.contains(it.first)) {
                    mapOf("value" to it.first, "selected" to "selected")
                } else {
                    mapOf("value" to it.first)
                }
                Tag(TAG.OPTION, it.second, attributes = attributes)
            }
            super.addAll(c)
        }
    }

    private fun selectOption() {
        val valueSet = if (this.multiple) value?.split(",") ?: emptySet() else setOf(value)
        children.forEach { child ->
            if (child is Tag && child.type == TAG.OPTION) {
                if (valueSet.contains(child.getAttribute("value"))) {
                    child.setAttribute("selected", "selected")
                } else {
                    child.removeAttribute("selected")
                }
            }
        }
    }

    override fun buildClassSet(classSetBuilder: ClassSetBuilder) {
        super.buildClassSet(classSetBuilder)
        classSetBuilder.add(validationStatus)
        classSetBuilder.add(size)
    }

    override fun buildAttributeSet(attributeSetBuilder: AttributeSetBuilder) {
        super.buildAttributeSet(attributeSetBuilder)
        name?.let {
            attributeSetBuilder.add("name", it)
        }
        if (multiple) {
            attributeSetBuilder.add("multiple")
        }
        selectSize?.let {
            attributeSetBuilder.add("size", "$it")
        }
        autofocus?.let {
            if (it) {
                attributeSetBuilder.add("autofocus")
            }
        }
        if (disabled) {
            attributeSetBuilder.add("disabled")
        }
    }

    override fun afterInsert(node: VNode) {
        refreshState()
    }

    protected open fun refreshState() {
        value?.let {
            if (this.multiple) {
                getElementJQuery()?.`val`(it.split(",").toTypedArray())
            } else {
                getElementJQuery()?.`val`(it)
            }
        } ?: getElementJQueryD()?.`val`(null)
    }

    /**
     * Makes the input element focused.
     */
    override fun focus() {
        getElementJQuery()?.focus()
    }

    /**
     * Makes the input element blur.
     */
    override fun blur() {
        getElementJQuery()?.blur()
    }

    override fun getState(): String? = value

    override fun subscribe(observer: (String?) -> Unit): () -> Unit {
        observers += observer
        observer(value)
        return {
            observers -= observer
        }
    }

    override fun setState(state: String?) {
        value = state
    }
}

/**
 * DSL builder extension function.
 *
 * It takes the same parameters as the constructor of the built component.
 */
fun Container.simpleSelectInput(
    options: List<StringPair>? = null, value: String? = null, emptyOption: Boolean = false,
    multiple: Boolean = false,
    selectSize: Int? = null,
    classes: Set<String>? = null,
    className: String? = null,
    init: (SimpleSelectInput.() -> Unit)? = null
): SimpleSelectInput {
    val simpleSelectInput =
        SimpleSelectInput(
            options,
            value,
            emptyOption,
            multiple,
            selectSize,
            classes ?: className.set, init
        )
    this.add(simpleSelectInput)
    return simpleSelectInput
}

/**
 * DSL builder extension function for observable state.
 *
 * It takes the same parameters as the constructor of the built component.
 */
fun <S> Container.simpleSelectInput(
    state: ObservableState<S>,
    options: List<StringPair>? = null, value: String? = null, emptyOption: Boolean = false,
    multiple: Boolean = false,
    selectSize: Int? = null,
    classes: Set<String>? = null,
    className: String? = null,
    init: (SimpleSelectInput.(S) -> Unit)
) = simpleSelectInput(
    options,
    value,
    emptyOption,
    multiple,
    selectSize,
    classes, className
).bind(state, true, init)

/**
 * Bidirectional data binding to the MutableState instance.
 * @param state the MutableState instance
 * @return current component
 */
fun <T : SimpleSelectInput> T.bindTo(state: MutableState<String?>): T {
    bind(state, false) {
        if (value != it) value = it
    }
    addBeforeDisposeHook(subscribe {
        state.setState(it)
    })
    return this
}

/**
 * Bidirectional data binding to the MutableState instance.
 * @param state the MutableState instance
 * @return current component
 */
fun <T : SimpleSelectInput> T.bindTo(state: MutableState<String>): T {
    bind(state, false) {
        if (value != it) value = it
    }
    addBeforeDisposeHook(subscribe {
        state.setState(it ?: "")
    })
    return this
}
