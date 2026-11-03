package cfr.gui.ui;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public final class SimpleDocumentListener implements DocumentListener {
    private final Runnable action;

    public SimpleDocumentListener(Runnable action) {
        this.action = action;
    }

    @Override public void insertUpdate(DocumentEvent event) { action.run(); }
    @Override public void removeUpdate(DocumentEvent event) { action.run(); }
    @Override public void changedUpdate(DocumentEvent event) { action.run(); }
}
