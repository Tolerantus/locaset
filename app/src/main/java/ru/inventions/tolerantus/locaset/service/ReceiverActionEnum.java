package ru.inventions.tolerantus.locaset.service;

/**
 * Created by Aleksandr on 05.03.2017.
 */

public enum ReceiverActionEnum {
    CANCEL("cancelAllNotifications"), PLAN("plan");
    private String description;

    ReceiverActionEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static ReceiverActionEnum from(String description) {
        for (ReceiverActionEnum actionEnum : ReceiverActionEnum.values()) {
            if (actionEnum.description.equals(description)) {
                return actionEnum;
            }
        }
        throw new IllegalArgumentException("incorrect description for receiver action");
    }

    @Override
    public String toString() {
        return description;
    }
}
