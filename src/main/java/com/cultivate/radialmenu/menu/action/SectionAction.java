package com.cultivate.radialmenu.menu.action;

/**
 * What happens when a section is clicked.
 */
public interface SectionAction {

    ActionType getType();

    /**
     * Runs the action. Called on the client thread at the start of a client tick, after the radial menu has closed.
     */
    void run();

    /**
     * @return a short human-readable summary for the editor, e.g. {@code /home} or {@code Tap: Open Inventory}
     */
    String describe();
}
