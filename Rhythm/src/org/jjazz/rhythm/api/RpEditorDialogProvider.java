package org.jjazz.rhythm.api;

/**
 * A RhythmParameter capability : provide a dialog to edit their value.
 *
 * @param <E> The type of value of the RhythmParameter.
 */
public interface RpEditorDialogProvider<E>
{

    /**
     * Show a modal dialog to edit a RhythmParameter value.
     *
     * @param initValue Initialize the editor with this value.
     * @return The result value of the editing. Null means the edit was cancelled by user.
     */
    E editValueWithCustomDialog(E initValue);
}
