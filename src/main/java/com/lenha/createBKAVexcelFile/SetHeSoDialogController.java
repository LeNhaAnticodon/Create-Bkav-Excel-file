package com.lenha.createBKAVexcelFile;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.net.URL;
import java.util.ResourceBundle;

public class SetHeSoDialogController implements Initializable {
    @FXML
    public TextField hesoTf;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // đặt giới hạn cho textField chỉ cho nhập giá trị số thực
        hesoTf.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            return newText.matches("-?\\d*(\\.\\d*)?") ? change : null;
        }));
    }

    /**
     * trả về giá trị hệ số
     * @return
     */
    public double processResult() {
        return Double.parseDouble(hesoTf.getText());
    }
}
