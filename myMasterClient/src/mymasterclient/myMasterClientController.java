

package myMasterClient;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

public class myMasterClientController {
  
 
  @FXML
  private void onStartNewClient(ActionEvent event) {
    
      try 
      {
        Parent root = FXMLLoader.load(getClass().getResource("myClient.fxml")); 
        Scene scene = new Scene(root);
        Stage stage = new Stage();
        stage.setScene(scene);
        stage.show();
        
        //dont let user close window without disconnecting cleanly
        stage.setOnCloseRequest(e -> 
        {
            Alert alert = new Alert(AlertType.INFORMATION); //alert window popup
            alert.setTitle("Important Instruction");
            alert.setContentText("Press the \"Close\" button inside Client GUI to close client window");
            alert.showAndWait();
            e.consume(); 
        });
        
      }
      catch (Exception e) {
        e.printStackTrace();
      }
      
      
    
  }
}
