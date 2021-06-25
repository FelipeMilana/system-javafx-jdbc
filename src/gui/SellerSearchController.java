package gui;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import db.DbException;
import gui.listeners.DataChangeListener;
import gui.util.Alerts;
import gui.util.Constraints;
import gui.util.Utils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert.AlertType;
import javafx.util.Callback;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import model.entities.Department;
import model.entities.Seller;
import model.exceptions.ValidationException;
import model.services.DepartmentService;
import model.services.SellerService;

public class SellerSearchController implements Initializable{
	
	//associations
	private Seller entity;
	private SellerService service;
	private DepartmentService dpService;
	
	private List<DataChangeListener> dataChangeListeners = new ArrayList<DataChangeListener>();
	
	@FXML
	private TextField txtName;
	
	@FXML
	private TextField txtEmail;
	
	@FXML
	private DatePicker dpBirthDate;
	
	@FXML
	private TextField txtBaseSalary;
	
	@FXML
	private ComboBox<Department> comboBoxDepartment;
	
	@FXML
	private Label labelError;
	
	@FXML
	private Button btFilter;
	
	@FXML
	private Button btCancel;
	
	private ObservableList<Department> obsList;
	
	@FXML
	public void onBtFilterAction(ActionEvent event) {
		if(entity == null) {
			throw new IllegalStateException("Entity was null");
		}
		if(service == null) {
			throw new IllegalStateException("Service was null");
		}
		try {
			//instantiating department from search
			entity = getSearchData();
			
			//use database to find this department
			List <Seller> list = service.findSellers(entity);
			
			if(list.isEmpty()) {
				Alerts.showAlert("Error finding seller", null, "No seller was found", AlertType.ERROR);
				notifyDataChangeListeners();
			}
			
			else {
				//send the event, because the tableview was changed
				notifyDataChangeListeners(list);
			}
			
			//close window
			Utils.currentStage(event).close();
		}
		catch(DbException e) {
			Alerts.showAlert("Error saving object", null, e.getMessage(), AlertType.ERROR);
		}
		catch(ValidationException e) {
			setErrorsMessages(e.getErrors());
		}
	}
	
	@FXML
	public void onBtCancelAction(ActionEvent event) {
		//close window
		Utils.currentStage(event).close();
	}
	
	public void subscribeDataChangeListener(DataChangeListener listener) {
		dataChangeListeners.add(listener);
	}
	
	public void setServices(SellerService service, DepartmentService dpService) {
		this.service = service;
		this.dpService = dpService;
	}
	
	public void setSeller(Seller entity) {
		this.entity = entity;
	}
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {	
		initializeNodes();
	}

	private void initializeNodes() {
		Constraints.setTextFieldMaxLength(txtName, 70);
		Constraints.setTextFieldDouble(txtBaseSalary);
		Constraints.setTextFieldMaxLength(txtEmail, 60);
		Utils.formatDatePicker(dpBirthDate, "dd/MM/yyyy");
		initializeComboBoxDepartment();
	}
	
	public void updateSearchData() {
		if(entity == null) {
			throw new IllegalStateException("Entity was null");
		}
		txtName.setText(entity.getName());
		txtEmail.setText(entity.getEmail());

		Locale.setDefault(Locale.US);
		txtBaseSalary.setText(String.format("%.2f", entity.getBaseSalary()));

		// we need data local format, this way, we change date.util to instant and
		// select a zone
		if (entity.getBirthDate() != null) {
			dpBirthDate.setValue(LocalDate.ofInstant(entity.getBirthDate().toInstant(), ZoneId.systemDefault()));
		}
		
		if(entity.getDepartment() == null ) {
			comboBoxDepartment.getSelectionModel().clearSelection();
		}
		else {
			comboBoxDepartment.setValue(entity.getDepartment());
		}
	}
	
	//create a combobox with a list of departments
	public void loadAssociatedObjects() {
		if (dpService == null) {
			throw new IllegalStateException("Department service was null");
		}

		List<Department> list = dpService.findAll();
		obsList = FXCollections.observableArrayList(list);
		comboBoxDepartment.setItems(obsList);
	}
	
	private Seller getSearchData() {
		Seller obj = new Seller();
		
		ValidationException exception = new ValidationException("Validation error");
		
		if(txtName.getText() == null || txtName.getText().trim().equals("")) {
			exception.addError("name", "Field can't be empty");
		}
		obj.setName(txtName.getText());
		
		if(txtEmail.getText() == null || txtEmail.getText().trim().equals("")) {
			exception.addError("email", "Field can't be empty");
		}
		obj.setEmail(txtEmail.getText());
		
		//catch date picker value
		if(dpBirthDate.getValue() == null) {
			exception.addError("birthDate", "Field can't be empty");
		}
		else {
			Instant instant = Instant.from(dpBirthDate.getValue().atStartOfDay(ZoneId.systemDefault()));
			obj.setBirthDate(Date.from(instant));
		}
		
		if(txtBaseSalary.getText() == null || txtBaseSalary.getText().trim().equals("")) {
			exception.addError("baseSalary", "Field can't be empty");
		}
		obj.setBaseSalary(Utils.tryParsetoDouble(txtBaseSalary.getText()));
		
		if(comboBoxDepartment.getValue() == null) {
			exception.addError("department", "Please select a department");
		}
		obj.setDepartment(comboBoxDepartment.getValue());
		
		if (exception.getErrors().size() >= 5) {
			exception.addError("filterError", "You must fill out \n at least one field");
			throw exception;
		}

		return obj;
	}
	
	private void notifyDataChangeListeners() {
		for(DataChangeListener listener: dataChangeListeners) {
			listener.onDataChanged();
		}
	}
	
	private void notifyDataChangeListeners(List<Seller> list) {
		for(DataChangeListener listener: dataChangeListeners) {
			listener.onDataChangedSearch(list);
		}
	}
	
	private void setErrorsMessages(Map<String, String> errors) {
		Set<String> fields = errors.keySet();
		
		if(fields.contains("filterError")) {
			labelError.setText(errors.get("filterError"));
		}
	}
	
	// necessary to initialize comboBox
	private void initializeComboBoxDepartment() {
		Callback<ListView<Department>, ListCell<Department>> factory = lv -> new ListCell<Department>() {
			@Override
			protected void updateItem(Department item, boolean empty) {
				super.updateItem(item, empty);
				setText(empty ? "" : item.getName());
			}
		};
		comboBoxDepartment.setCellFactory(factory);
		comboBoxDepartment.setButtonCell(factory.call(null));
	}
}
