package com.example.mindmap.view;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;

@FunctionalInterface
interface ContextMenuPresenter {
    void show(ContextMenu menu, Node anchor, double screenX, double screenY);
}
