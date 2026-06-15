package com.ht.common.enums;

/**
 * Represents the action performed on a cart item.
 * ADD       - purchasing a new item
 * MODIFY    - upgrading/changing an existing item (e.g. bigger subscription plan)
 * DELETE    - removing/cancelling an item (e.g. cancelling a subscription)
 */
public enum ActionType {
    ADD,
    MODIFY,
    DELETE
}
