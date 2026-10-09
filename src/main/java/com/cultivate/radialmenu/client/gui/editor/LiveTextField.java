package com.cultivate.radialmenu.client.gui.editor;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.cleanroommc.modularui.value.StringValue;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

/**
 * A text field that hands its text to the setter while it is being typed in.
 *
 * <p>
 * ModularUI2 2.2.18 only writes a text field's value back when the field loses focus, and clicking a button runs
 * before that, so a Save button would see the old text. This field passes its text on every tick while focused, and
 * {@link #commit()} passes it on at once.
 */
final class LiveTextField extends TextFieldWidget {

    private final Consumer<String> setter;

    LiveTextField(Supplier<String> getter, Consumer<String> setter) {
        this.setter = setter;
        value(new StringValue.Dynamic(getter, setter));
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (isFocused()) {
            commit();
        }
    }

    /**
     * Passes the text currently in the field to the setter.
     */
    void commit() {
        setter.accept(getText());
    }
}
