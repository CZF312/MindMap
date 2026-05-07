package com.example.mindmap.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Window;

import java.util.Optional;

public final class Dialogs {
    private Dialogs() {
    }

    public static void info(Window owner, String title, String message) {
        alert(owner, Alert.AlertType.INFORMATION, title, message).showAndWait();
    }

    public static void error(Window owner, String title, String message) {
        alert(owner, Alert.AlertType.ERROR, title, message).showAndWait();
    }

    public static boolean confirm(Window owner, String title, String message) {
        return alert(owner, Alert.AlertType.CONFIRMATION, title, message)
                .showAndWait()
                .filter(ButtonType.OK::equals)
                .isPresent();
    }

    public static Optional<String> input(Window owner, String title, String message, String initialValue) {
        TextInputDialog dialog = new TextInputDialog(initialValue == null ? "" : initialValue);
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(message);
        return dialog.showAndWait().map(String::trim);
    }

    public static SaveChoice confirmSave(Window owner) {
        Alert alert = alert(owner, Alert.AlertType.CONFIRMATION, "未保存的修改", "当前思维导图尚未保存，是否先保存？");
        ButtonType save = new ButtonType("保存", ButtonBar.ButtonData.YES);
        ButtonType discard = new ButtonType("不保存", ButtonBar.ButtonData.NO);
        ButtonType cancel = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(save, discard, cancel);
        return alert.showAndWait()
                .map(button -> button == save ? SaveChoice.SAVE : button == discard ? SaveChoice.DISCARD : SaveChoice.CANCEL)
                .orElse(SaveChoice.CANCEL);
    }

    private static Alert alert(Window owner, Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert;
    }

    public enum SaveChoice {
        SAVE,
        DISCARD,
        CANCEL
    }
}
