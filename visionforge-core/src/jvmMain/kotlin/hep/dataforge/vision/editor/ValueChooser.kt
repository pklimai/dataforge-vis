/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hep.dataforge.vision.editor

import hep.dataforge.context.Context
import hep.dataforge.context.Named
import hep.dataforge.meta.Meta
import hep.dataforge.meta.descriptors.ValueDescriptor
import hep.dataforge.names.toName
import hep.dataforge.provider.Type
import hep.dataforge.provider.provideByType
import hep.dataforge.values.Null
import hep.dataforge.values.Value
import hep.dataforge.vision.widget
import hep.dataforge.vision.widgetType
import javafx.beans.property.ObjectProperty
import javafx.beans.value.ObservableValue
import javafx.scene.Node
import tornadofx.*

/**
 * A value chooser object. Must have an empty constructor to be invoked by
 * reflections.
 *
 * @author [Alexander Nozik](mailto:altavir@gmail.com)
 */
interface ValueChooser {

    /**
     * Get or create a Node that could be later inserted into some parent
     * object.
     *
     * @return
     */
    val node: Node

    /**
     * The descriptor property for this value. Could be null
     *
     * @return
     */
    val descriptorProperty: ObjectProperty<ValueDescriptor?>
    var descriptor: ValueDescriptor?

    val valueProperty: ObjectProperty<Value?>
    var value: Value?


    /**
     * Set display value but do not notify listeners
     *
     * @param value
     */
    fun setDisplayValue(value: Value)


    fun setDisabled(disabled: Boolean) {
        //TODO replace by property
    }

    fun setCallback(callback: ValueCallback)

    @Type("hep.dataforge.vis.fx.valueChooserFactory")
    interface Factory : Named {
        operator fun invoke(meta: Meta = Meta.EMPTY): ValueChooser
    }

    companion object {

        private fun findWidgetByType(context: Context, type: String): Factory? {
            return when (type.toName()) {
                TextValueChooser.name -> TextValueChooser
                ColorValueChooser.name -> ColorValueChooser
                ComboBoxValueChooser.name -> ComboBoxValueChooser
                else -> context.provideByType(type)//Search for additional factories in the plugin
            }
        }

        private fun build(context: Context, descriptor: ValueDescriptor?): ValueChooser {
            return if (descriptor == null) {
                TextValueChooser();
            } else {
                val widgetType = descriptor.widgetType
                val chooser: ValueChooser = when {
                    widgetType != null -> {
                        findWidgetByType(
                            context,
                            widgetType
                        )?.invoke(
                            descriptor.widget
                        ) ?: TextValueChooser()
                    }
                    descriptor.allowedValues.isNotEmpty() -> ComboBoxValueChooser()
                    else -> TextValueChooser()
                }
                chooser.descriptor = descriptor
                chooser
            }
        }

        fun build(
            context: Context,
            value: ObservableValue<Value?>,
            descriptor: ValueDescriptor? = null,
            setter: (Value) -> Unit
        ): ValueChooser {
            val chooser = build(context, descriptor)
            chooser.setDisplayValue(value.value ?: Null)
            value.onChange {
                chooser.setDisplayValue(it ?: Null)
            }
            chooser.setCallback { result ->
                if (descriptor?.isAllowedValue(result) != false) {
                    setter(result)
                    ValueCallbackResponse(true, result, "OK")
                } else {
                    ValueCallbackResponse(
                        false,
                        value.value ?: Null,
                        "Not allowed"
                    )
                }
            }
            return chooser
        }
    }
}