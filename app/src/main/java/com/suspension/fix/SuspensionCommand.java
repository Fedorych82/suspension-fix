package com.suspension.fix;

/**
 * Набор поддерживаемых команд подвески: связывает строку intent-action
 * с CAN-кадром, который нужно отправить в шину.
 */
enum SuspensionCommand {
    LOCK("com.suspension.fix.LOCK_SUSPENSION",     "69 08 02 00 00 16 00 00 00 00", "блокировка"),
    UNLOCK("com.suspension.fix.UNLOCK_SUSPENSION", "69 08 01 00 00 16 00 00 00 00", "разблокировка");

    private final String action;
    private final String frame;
    private final String label;

    SuspensionCommand(String action, String frame, String label) {
        this.action = action;
        this.frame = frame;
        this.label = label;
    }

    String frame() {
        return frame;
    }

    /** Короткое человекочитаемое название команды (для UI/тостов). */
    String label() {
        return label;
    }

    /** Возвращает команду для данного action или null, если action чужой. */
    static SuspensionCommand forAction(String action) {
        for (SuspensionCommand command : values()) {
            if (command.action.equals(action)) {
                return command;
            }
        }
        return null;
    }
}
