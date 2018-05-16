package controller.visma;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import DAO.ObjectDAO;
import controller.WorkOrderHandler;
import controller.twinfield.SoapHandler;
import object.Settings;
import object.Token;
import object.workorder.Address;
import object.workorder.EmployeeExtended;
import object.workorder.HourType;
import object.workorder.Material;
import object.workorder.MaterialCategory;
import object.workorder.Project;
import object.workorder.Relation;
import object.workorder.WorkOrder;
import object.workorder.WorkPeriod;

public class VismaHandler {
	private static String apiUrl = System.getenv("VISMA_API_URL");
	final String softwareName = "Visma";
	private Boolean checkUpdate = false;
	String getSalesorderError = "";
	private final static Logger logger = Logger.getLogger(SoapHandler.class.getName());
	
	public boolean checkAccessToken(String accessToken) throws IOException {
		String link = "https://" + apiUrl + "/controller/api/v1/employee";
		
		try {
			URL url = new URL(link);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			// conn.setDoInput(true);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Authorization", "Bearer " + accessToken);
			conn.setUseCaches(false);
			if (conn.getResponseCode() > 200 && conn.getResponseCode() < 500) {
				conn.disconnect();
				return false;
			} else {
				conn.disconnect();
				return true;
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public Object getJSON(String accessToken, String parameters, String path, String method, Settings set)
			throws IOException, URISyntaxException {
		String jsonString = null;
		Object object = null;
		try {
			URI uri = new URI("https", apiUrl, path, parameters, null);
			String request = uri.toASCIIString();
			URL url = new URL(request);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			// conn.setDoInput(true);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Authorization", "Bearer " + accessToken);
			if (set != null) {
				conn.setRequestProperty("ipp-company-id", set.getExportOffice());
			}
			conn.setRequestProperty("ipp-application-type", "Visma.net Financials");
			conn.setUseCaches(false);
			if (conn.getResponseCode() > 200 && conn.getResponseCode() < 500) {
				System.out.println("Response message " + conn.getResponseMessage());
				BufferedReader br = new BufferedReader(
						new InputStreamReader((conn.getErrorStream()), StandardCharsets.UTF_8));
				while ((jsonString = br.readLine()) != null) {
					System.out.println(jsonString);
					JSONObject object1 = new JSONObject(jsonString);
					getSalesorderError = object1.optString("message", "");
				}
				return null;
			}
			BufferedReader br = new BufferedReader(
					new InputStreamReader((conn.getInputStream()), StandardCharsets.UTF_8));
			while ((jsonString = br.readLine()) != null) {
				switch (method) {
				case "array":
					object = new JSONArray(jsonString);
					break;
				case "object":
					object = new JSONObject(jsonString);
					break;
				}
			}
			conn.disconnect();
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
		return object;
	}
	
	public Object postJSON(String accessToken, JSONObject json, String path, String method, String requestMethod,
			Settings set) {
		String jsonString = null;
		String jsonRequestString = json + "";
		byte[] postData = jsonRequestString.getBytes(StandardCharsets.UTF_8);
		
		int postDataLength = postData.length;
		try {
			URI uri = new URI("https", apiUrl, path, "", null);
			String request = uri.toASCIIString();
			URL url = new URL(request);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			// conn.setDoInput(true);
			conn.setRequestMethod(requestMethod);
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Authorization", "Bearer " + accessToken);
			conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
			if (set != null) {
				conn.setRequestProperty("ipp-company-id", set.getExportOffice());
			}
			conn.setRequestProperty("ipp-application-type", "Visma.net Financials");
			conn.setUseCaches(false);
			try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
				wr.write(postData);
			}
			if (conn.getResponseCode() > 204 && conn.getResponseCode() <= 500) {
				System.out.println("RESPONSEMESSAGE " + conn.getResponseMessage());
				BufferedReader br = new BufferedReader(
						new InputStreamReader((conn.getErrorStream()), StandardCharsets.UTF_8));
				JSONObject jsonObject = null;
				while ((jsonString = br.readLine()) != null) {
					System.out.println("BAD RESPONSE OBJECT " + jsonString);
					jsonObject = new JSONObject(jsonString);
				}
				conn.disconnect();
				return jsonObject;
				
			} else {
				conn.disconnect();
				return true;
			}
		} catch (IOException | URISyntaxException | JSONException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	public String getDateMinHour(String string) {
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		Date date = null;
		try {
			// String to date
			date = format.parse(string);
			// Create Calender to edit time
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			cal.add(Calendar.HOUR_OF_DAY, 1);
			date = cal.getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		// Date to String
		Format formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		String s = formatter.format(date);
		return s;
	}
	
	public String convertDate(String string, String formatResponse) {
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Date date = null;
		try {
			// String to date
			date = format.parse(string);
			// Create Calender to edit time
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			cal.add(Calendar.HOUR_OF_DAY, 0);
			date = cal.getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		// Date to String
		Format formatter = new SimpleDateFormat(formatResponse);
		String s = formatter.format(date);
		return s;
	}
	
	public String getCurrentDate(String date) {
		String timestamp;
		ZonedDateTime za = ZonedDateTime.now(ZoneId.of("Europe/Paris"));
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		if (date != null) {
			timestamp = date;
		} else {
			timestamp = za.format(formatter);
		}
		return timestamp;
	}
	
	// Medewerkers
	public String[] getEmployees(Token t, String date, Settings set) throws Exception {
		String errorMessage = "";
		String path = "/API/controller/api/v1/employee";
		String parameters = "";
		ArrayList<EmployeeExtended> employees = new ArrayList<EmployeeExtended>();
		Boolean hasContent = ObjectDAO.hasContent(t.getSoftwareToken(), "employees");
		if (date != null && hasContent) {
			// yyyy-MM-dd hh:mm:ss
			parameters += "&lastModifiedDateTime=" + getDateMinHour(date) + "&lastModifiedDateTimeCondition=>";
		}
		
		JSONArray jsonArray = (JSONArray) getJSON(t.getAccessToken(), parameters, path, "array", set);
		logger.info("Employees response " + jsonArray);
		if (jsonArray != null) {
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject object = jsonArray.getJSONObject(i);
				int id = object.getInt("employeeId");
				JSONObject contact = object.getJSONObject("contact");
				String firstName = contact.optString("firstName");
				String midName = contact.optString("midName", "");
				String lastName = contact.optString("lastName");
				
				String code = object.getString("employeeNumber");
				EmployeeExtended m = new EmployeeExtended(id, firstName, midName + " " + lastName, code, "");
				employees.add(m);
			}
		}
		if (!employees.isEmpty()) {
			int successAmount = (int) WorkOrderHandler.addData(t.getSoftwareToken(), employees, "employeesExtended",
					softwareName);
			if (successAmount > 0) {
				ObjectDAO.saveEmployeesExtended(employees, t.getSoftwareToken());
				errorMessage += successAmount + " employees imported<br>";
				checkUpdate = true;
			} else {
				errorMessage += "Something went wrong with employees<br>";
			}
		} else {
			errorMessage += "No employees for import<br>";
		}
		
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	// Relaties
	public String[] getRelations(Token t, String date, int skip, int successAmount, ArrayList<Relation> rel,
			Settings set) throws Exception {
		String errorMessage = "";
		String parameters = "";
		Relation r = null;
		String path = "/API/controller/api/v1/customer";
		ArrayList<Relation> relations = new ArrayList<Relation>();
		Boolean hasContent = ObjectDAO.hasContent(t.getSoftwareToken(), "relations");
		if (date != null && hasContent) {
			// yyyy-MM-dd hh:mm:ss
			// Check if relation is client or supplier
			parameters = "skipRecords=" + skip + "&numberToRead=500&lastModifiedDateTime=" + getDateMinHour(date)
					+ "&lastModifiedDateTimeCondition=>";
		} else {
			// Check if address is client and not supplier
			parameters = "skipRecords=" + skip + "&numberToRead=500";
		}
		System.out.println("PARAM " + parameters);
		JSONArray jsonArray = (JSONArray) getJSON(t.getAccessToken(), parameters, path, "array", set);
		logger.info("Relations response " + jsonArray);
		if (jsonArray != null) {
			if (rel == null) {
				rel = new ArrayList<Relation>();
			}
			for (int i = 0; i < jsonArray.length(); i++) {
				ArrayList<Address> addresses = new ArrayList<Address>();
				JSONObject object = jsonArray.getJSONObject(i);
				String id = object.getInt("internalId") + "";
				String code = object.getInt("number") + "";
				String name = object.getString("name");
				// MainAddress
				JSONObject mainAddress = object.getJSONObject("mainAddress");
				int addressId = mainAddress.getInt("addressId");
				String street = mainAddress.getString("addressLine1");
				String city = mainAddress.getString("city");
				String postalCode = mainAddress.getString("postalCode");
				// MainContact
				JSONObject mainContact = object.getJSONObject("mainContact");
				String contact = mainContact.optString("name");
				String telefone = mainContact.optString("phone1");
				String mobile = mainContact.optString("phone2");
				String email = mainContact.optString("email");
				if (telefone.equals("")) {
					telefone = mobile;
				}
				Address mainAddressObject = new Address(contact, telefone, email, street, "", postalCode, city, null,
						"main", addressId);
				addresses.add(mainAddressObject);
				
				// InvoiceAddress
				JSONObject invoiceAddress = object.getJSONObject("invoiceAddress");
				int invoiceAddressId = invoiceAddress.getInt("addressId");
				String invoiceStreet = invoiceAddress.optString("addressLine1");
				String invoiceCity = invoiceAddress.optString("city");
				String invoicePostalCode = invoiceAddress.optString("postalCode");
				// InvoiceContact
				JSONObject invoiceContact = object.getJSONObject("invoiceContact");
				String invoiceContactName = invoiceContact.getString("name");
				String invoiceTelefone = invoiceContact.optString("phone1");
				String invoiceMobile = invoiceContact.optString("phone2");
				String invoiceEmail = invoiceContact.optString("email");
				String invoiceRemark = "Factuuradres";
				if (invoiceTelefone.equals("")) {
					invoiceTelefone = invoiceMobile;
				}
				
				Address invoiceAddressObject = new Address(invoiceContactName, invoiceTelefone, invoiceEmail,
						invoiceStreet, "", invoicePostalCode, invoiceCity, invoiceRemark, "contact", invoiceAddressId);
				addresses.add(invoiceAddressObject);
				
				// deliveryAddress
				JSONObject deliveryAddress = object.getJSONObject("deliveryAddress");
				int deliveryAddressId = deliveryAddress.getInt("addressId");
				String deliveryStreet = deliveryAddress.optString("addressLine1");
				String deliveryCity = deliveryAddress.optString("city");
				String deliveryPostalCode = deliveryAddress.getString("postalCode");
				// InvoiceContact
				JSONObject deliveryContact = object.getJSONObject("deliveryContact");
				String deliveryContactName = deliveryContact.getString("name");
				String deliveryTelefone = deliveryContact.optString("phone1");
				String deliveryMobile = deliveryContact.optString("phone2");
				String deliveryEmail = deliveryContact.optString("email");
				if (deliveryTelefone.equals("")) {
					deliveryTelefone = deliveryMobile;
				}
				String deliveryRemark = "Afleveradres";
				Address deliveryAddressObject = new Address(deliveryContactName, deliveryTelefone, deliveryEmail,
						deliveryStreet, "", deliveryPostalCode, deliveryCity, deliveryRemark, "contact",
						deliveryAddressId);
				addresses.add(deliveryAddressObject);
				String modified = object.getString("lastModifiedDateTime");
				r = new Relation(name, code, contact, email, addresses, modified, id);
				relations.add(r);
				rel.add(r);
			}
		}
		if (!relations.isEmpty()) {
			int success = (int) WorkOrderHandler.addData(t.getSoftwareToken(), relations, "relations", softwareName);
			
			if (relations.size() == 500) {
				if (success > 0) {
					successAmount += success;
					int successAddresses = (int) WorkOrderHandler.addData(t.getSoftwareToken(), relations, "addresses",
							softwareName);
					int successContacts = (int) WorkOrderHandler.addData(t.getSoftwareToken(), relations,
							"contactpersons", softwareName);
				} else {
					errorMessage += "Something went wrong with relations<br>";
				}
				return getRelations(t, date, skip += 500, successAmount, rel, set);
				
			} else {
				if (success > 0) {
					int successAddresses = (int) WorkOrderHandler.addData(t.getSoftwareToken(), relations, "addresses",
							softwareName);
					int successContacts = (int) WorkOrderHandler.addData(t.getSoftwareToken(), relations,
							"contactpersons", softwareName);
					successAmount += success;
					ObjectDAO.saveRelations(rel, t.getSoftwareToken());
					errorMessage += successAmount + " relations imported<br>";
					checkUpdate = true;
				} else {
					errorMessage += "Something went wrong with relations<br>";
				}
			}
		} else {
			if (successAmount > 0) {
				ObjectDAO.saveRelations(rel, t.getSoftwareToken());
				errorMessage += successAmount + " relations imported<br>";
				checkUpdate = true;
			} else {
				errorMessage += "No relations for import<br>";
			}
		}
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	// Materialen
	public String[] getMaterials(Token t, String date, int skip, int successAmount, ArrayList<Material> mat,
			Settings set) throws Exception {
		String errorMessage = "";
		String parameters = "";
		String path = "/API/controller/api/v1/inventory";
		ArrayList<Material> materials = new ArrayList<Material>();
		Boolean hasContent = ObjectDAO.hasContent(t.getSoftwareToken(), "materials");
		if (date != null && hasContent) {
			// yyyy-MM-dd hh:mm:ss
			// Check if relation is client or supplier
			parameters = "skipRecords=" + skip + "&numberToRead=500&lastModifiedDateTime=" + getDateMinHour(date)
					+ "&lastModifiedDateTimeCondition=>";
		} else {
			// Check if address is client and not supplier
			parameters = "skipRecords=" + skip + "&numberToRead=500";
		}
		JSONArray jsonArray = (JSONArray) getJSON(t.getAccessToken(), parameters, path, "array", set);
		logger.info("Material response " + jsonArray);
		if (jsonArray != null) {
			if (mat == null) {
				mat = new ArrayList<Material>();
			}
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject object = jsonArray.getJSONObject(i);
				String unit = object.getString("baseUnit");
				String type = object.optString("type");
				if (!((type.equals("LaborItem") || type.equals("ServiceItem"))
						&& (unit.equals("UUR") || unit.equals("HOUR")))) {
					String id = object.getInt("inventoryId") + "";
					String code = object.getString("inventoryNumber");
					String description = object.getString("description");
					// Price without taxes...
					double salesPrice = object.getDouble("defaultPrice");
					String modified = object.getString("lastModifiedDateTime");
					Material m = new Material(code, null, unit, description, salesPrice, null, modified, id);
					materials.add(m);
					mat.add(m);
				}
			}
		}
		// Materials log message
		if (!materials.isEmpty()) {
			int success = (int) WorkOrderHandler.addData(t.getSoftwareToken(), materials, "materials", softwareName);
			
			if (materials.size() == 500) {
				if (success > 0) {
					successAmount += success;
				}
				return getMaterials(t, date, skip += 500, successAmount, mat, set);
				
			} else {
				if (success > 0) {
					successAmount += success;
					ObjectDAO.saveMaterials(mat, t.getSoftwareToken());
					errorMessage += successAmount + " materials imported<br>";
					checkUpdate = true;
				} else {
					errorMessage += "Something went wrong with materials<br>";
				}
			}
			
		} else {
			if (successAmount > 0) {
				ObjectDAO.saveMaterials(mat, t.getSoftwareToken());
				errorMessage += successAmount + " materials imported<br>";
				checkUpdate = true;
			} else {
				errorMessage += "No materials for import<br>";
			}
		}
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	// Materialen
	public String[] getHourtypes(Token t, String date, int skip, int successAmount, ArrayList<HourType> hour,
			Settings set) throws Exception {
		String errorMessage = "";
		String parameters = "";
		String path = "/API/controller/api/v1/inventory";
		ArrayList<HourType> hourtypes = new ArrayList<HourType>();
		Boolean hasContent = ObjectDAO.hasContent(t.getSoftwareToken(), "hourtypes");
		if (date != null && hasContent) {
			// yyyy-MM-dd hh:mm:ss
			// Check if relation is client or supplier
			parameters = "skipRecords=" + skip + "&numberToRead=500&lastModifiedDateTime=" + getDateMinHour(date)
					+ "&lastModifiedDateTimeCondition=>";
		} else {
			// Check if address is client and not supplier
			parameters = "skipRecords=" + skip + "&numberToRead=500";
		}
		JSONArray jsonArray = (JSONArray) getJSON(t.getAccessToken(), parameters, path, "array", set);
		logger.info("HourType response " + jsonArray);
		if (jsonArray != null) {
			if (hour == null) {
				hour = new ArrayList<HourType>();
			}
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject object = jsonArray.getJSONObject(i);
				String unit = object.getString("baseUnit");
				String type = object.optString("type");
				if ((type.equals("LaborItem") || type.equals("ServiceItem"))
						&& (unit.equals("UUR") || unit.equals("HOUR"))) {
					String id = object.getInt("inventoryId") + "";
					String code = object.getString("inventoryNumber");
					String description = object.getString("description");
					// Price without taxes...
					double salesPrice = object.getDouble("defaultPrice");
					double costPrice = object.getDouble("currentCost");
					String modified = object.getString("lastModifiedDateTime");
					HourType h = new HourType(code, description, 1, 1, costPrice, salesPrice, 1, modified, id);
					hourtypes.add(h);
					hour.add(h);
				}
			}
		}
		// Materials log message
		if (!hourtypes.isEmpty()) {
			int success = (int) WorkOrderHandler.addData(t.getSoftwareToken(), hourtypes, "hourtypes", softwareName);
			
			if (hourtypes.size() == 500) {
				if (success > 0) {
					successAmount += success;
				}
				return getHourtypes(t, date, skip += 500, successAmount, hour, set);
				
			} else {
				if (success > 0) {
					successAmount += success;
					ObjectDAO.saveHourTypes(hour, t.getSoftwareToken());
					errorMessage += successAmount + " hourtypes imported<br>";
					checkUpdate = true;
				} else {
					errorMessage += "Something went wrong with hourtypes<br>";
				}
			}
			
		} else {
			if (successAmount > 0) {
				ObjectDAO.saveHourTypes(hour, t.getSoftwareToken());
				errorMessage += successAmount + " hourtypes imported<br>";
				checkUpdate = true;
			} else {
				errorMessage += "No hourtypes for import<br>";
			}
		}
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	// Projects
	public String[] getProjects(Token t, String date, Settings set) throws Exception {
		boolean checkUpdate = false;
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		String path = "/API/controller/api/v1/project";
		String parameters = "status=Active";
		String errorMessage = "";
		ArrayList<Project> projects = new ArrayList<Project>();
		Boolean hasContent = ObjectDAO.hasContent(t.getSoftwareToken(), "projects");
		if (date != null && hasContent) {
			parameters += "&lastModifiedDateTime=" + getDateMinHour(date) + "&lastModifiedDateTimeCondition=>";
		}
		
		jsonArray = (JSONArray) getJSON(t.getAccessToken(), parameters, path, "array", set);
		logger.info("Projects response JSONArray " + jsonArray);
		if (jsonArray != null) {
			for (int i = 0; i < jsonArray.length(); i++) {
				jsonObj = jsonArray.getJSONObject(i);
				String id = jsonObj.getString("internalID");
				String number = jsonObj.optString("projectID");
				String name = jsonObj.getString("description");
				String startDate = jsonObj.getString("startDate");
				String endDate = jsonObj.optString("endDate", "");
				String description = jsonObj.getString("Notes");
				JSONObject customerObject = jsonObj.getJSONObject("customer");
				String debtorId = customerObject.optString("number", "");
				if (!endDate.equals("")) {
					endDate = convertDate(endDate, "yyyyMMdd");
				}
				JSONObject managerObject = jsonObj.getJSONObject("projectManager");
				String employee = managerObject.optString("employeeNumber", "");
				Project p = new Project(number, id, debtorId, "Lopend", name, convertDate(startDate, "yyyyMMdd"),
						endDate, description, 0, 1, employee);
				projects.add(p);
			}
		}
		if (!projects.isEmpty()) {
			int successAmount = (int) WorkOrderHandler.addData(t.getSoftwareToken(), projects, "projects",
					softwareName);
			if (successAmount > 0) {
				ObjectDAO.saveProjects(projects, t.getSoftwareToken());
				errorMessage += successAmount + " projects imported<br>";
				checkUpdate = true;
			} else {
				errorMessage += "Something went wrong with projects<br>";
			}
		} else {
			errorMessage += "No projects for import<br>";
		}
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	// SalesOrderTypes
	public ArrayList<MaterialCategory> getSalesOrderTypes(Token t, Settings set) throws Exception {
		String parameters = "";
		String path = "/API/controller/api/v1/salesordertype";
		ArrayList<MaterialCategory> salesOrderTypes = new ArrayList<MaterialCategory>();
		JSONArray jsonArray = (JSONArray) getJSON(t.getAccessToken(), parameters, path, "array", set);
		logger.info("SalesOrderTypes response JSONArray " + jsonArray);
		if (jsonArray != null) {
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject object = jsonArray.getJSONObject(i);
				if (object.getBoolean("active")) {
					String code = object.getString("orderType");
					String name = object.getString("description");
					MaterialCategory type = new MaterialCategory(code, name, 1, null);
					salesOrderTypes.add(type);
				}
			}
		}
		return salesOrderTypes;
	}
	
	// MaterialTypes
	public ArrayList<MaterialCategory> getMaterialTypes(Token t, Settings set) throws Exception {
		String parameters = "";
		String path = "/API/controller/api/v1/inventory/itemClass";
		ArrayList<MaterialCategory> materialTypes = new ArrayList<MaterialCategory>();
		JSONArray jsonArray = (JSONArray) getJSON(t.getAccessToken(), parameters, path, "array", set);
		logger.info("MaterialTypes response JSONArray " + jsonArray);
		if (jsonArray != null) {
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject object = jsonArray.getJSONObject(i);
				String code = object.getString("id");
				String name = object.getString("description");
				MaterialCategory type = new MaterialCategory(code, name, 1, null);
				materialTypes.add(type);
			}
		}
		return materialTypes;
	}
	
	// Companies
	public ArrayList<MaterialCategory> getCompanies(Token t, Settings set) throws Exception {
		String parameters = "";
		String path = "/API/resources/v1/context";
		ArrayList<MaterialCategory> companies = new ArrayList<MaterialCategory>();
		JSONArray jsonArray = (JSONArray) getJSON(t.getAccessToken(), parameters, path, "array", set);
		if (jsonArray != null) {
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject object = jsonArray.getJSONObject(i);
				String code = object.getInt("id") + "";
				String name = object.getString("name");
				MaterialCategory type = new MaterialCategory(code, name, 1, null);
				companies.add(type);
			}
		}
		return companies;
	}
	
	// Verkooporders
	public String[] getOrders(Token t, String date, Settings set) throws Exception {
		boolean checkUpdate = false;
		String errorMessage = "";
		ArrayList<String> orderTypes = null;
		if (set.getImportOffice() != null) {
			String list = set.getImportOffice().replace("[", "").replace("]", "");
			String[] strValues = list.split(",\\s");
			orderTypes = new ArrayList<String>(Arrays.asList(strValues));
		}
		ArrayList<WorkOrder> workorders = getSalesOrdersArray(t, date, set);
		if (!workorders.isEmpty()) {
			JSONArray responseArray = (JSONArray) WorkOrderHandler.addData(t.getSoftwareToken(), workorders,
					"PostWorkorders", softwareName);
			for (int i = 0; i < responseArray.length(); i++) {
				JSONObject object = responseArray.getJSONObject(i);
				String id = object.optString("workorder_no", "");
				setOrderStatus(t, id, true, set, orderTypes);
			}
			int successAmount = responseArray.length();
			
			if (successAmount > 0) {
				errorMessage += successAmount + " salesorders imported<br>";
				checkUpdate = true;
			} else {
				errorMessage += "Something went wrong with salesorders<br>";
			}
		} else {
			errorMessage += "No salesorders for import<br>";
		}
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	public ArrayList<WorkOrder> getSalesOrdersArray(Token t, String date, Settings set) throws Exception {
		WorkOrder w = null;
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		String path = "/API/controller/api/v1/salesorder";
		String parameters = "";
		ArrayList<String> orderTypes = null;
		if (set.getImportOffice() != null) {
			String list = set.getImportOffice().replace("[", "").replace("]", "");
			String[] strValues = list.split(",\\s");
			orderTypes = new ArrayList<String>(Arrays.asList(strValues));
		}
		ArrayList<WorkOrder> workorders = new ArrayList<WorkOrder>();
		jsonArray = (JSONArray) getJSON(t.getAccessToken(), parameters, path, "array", set);
		logger.info("Order response JSONArray " + jsonArray);
		if (jsonArray != null) {
			for (int i = 0; i < jsonArray.length(); i++) {
				ArrayList<Relation> relations = new ArrayList<Relation>();
				ArrayList<Address> address = new ArrayList<Address>();
				jsonObj = jsonArray.getJSONObject(i);
				String status = jsonObj.getString("status");
				String orderType = jsonObj.getString("orderType");
				String referentie = jsonObj.optString("customerRefNo");
				// Status is open
				if (status.equals("Open") && orderTypes.contains(orderType) && !referentie.equals("WBA verzonden")
						&& !referentie.startsWith("Vanuit WBA")) {
					String id = jsonObj.getString("orderNo");
					String projectNumber = jsonObj.getInt("project") + "";
					if (jsonObj.getInt("project") == 0) {
						projectNumber = "";
					}
					String workDescription = jsonObj.optString("description");
					
					// Relation name and number
					JSONObject customerDetails = jsonObj.getJSONObject("customer");
					String debtorId = customerDetails.optInt("internalId") + "";
					String debtorNr = customerDetails.optString("number") + "";
					String companyName = customerDetails.optString("name") + "";
					
					// Invoice contact and address details
					JSONObject invoiceContact = jsonObj.getJSONObject("soBillingContact");
					JSONObject invoiceAddress = jsonObj.getJSONObject("soBillingAddress");
					String invoiceContactName = invoiceContact.optString("name", "<leeg>");
					String invoiceEmail = invoiceContact.optString("email");
					String invoicePhone = invoiceContact.optString("phone1");
					String invoiceMobile = invoiceContact.optString("phone2");
					if (invoicePhone.equals("")) {
						invoicePhone = invoiceMobile;
					}
					String invoiceStreet = invoiceAddress.optString("addressLine1", "<leeg>");
					String invoicePostalCode = invoiceAddress.optString("postalCode", "");
					String invoiceCity = invoiceAddress.optString("city", "<leeg>");
					
					Address invoice = new Address(invoiceContactName, invoicePhone, invoiceEmail, invoiceStreet, "",
							invoicePostalCode, invoiceCity, null, "invoice", 1);
					address.add(invoice);
					
					// Delivery
					JSONObject deliveryContact = jsonObj.getJSONObject("soShippingContact");
					JSONObject deliveryAddress = jsonObj.getJSONObject("soShippingAddress");
					String deliveryContactName = invoiceContact.optString("name", "<leeg>");
					String deliveryEmail = deliveryContact.optString("email");
					String deliveryPhone = deliveryContact.optString("phone1");
					String deliveryMobile = deliveryContact.optString("phone2");
					if (deliveryPhone.equals("")) {
						deliveryPhone = deliveryMobile;
					}
					String deliveryStreet = deliveryAddress.optString("addressLine1", "<leeg>");
					String deliveryPostalCode = deliveryAddress.optString("postalCode", "");
					String deliveryCity = deliveryAddress.optString("city", "<leeg>");
					
					Address delivery = new Address(deliveryContactName, deliveryPhone, deliveryEmail, deliveryStreet,
							"", deliveryPostalCode, deliveryCity, null, "postal", 2);
					address.add(delivery);
					Relation r = new Relation(companyName, debtorNr, invoiceContactName, invoiceEmail, address, null,
							debtorId);
					relations.add(r);
					
					// Materials
					ArrayList<Material> materials = new ArrayList<Material>();
					ArrayList<WorkPeriod> periods = new ArrayList<WorkPeriod>();
					Material m = null;
					WorkPeriod period = null;
					JSONArray rows = jsonObj.getJSONArray("lines");
					for (int j = 0; j < rows.length(); j++) {
						JSONObject rowDetails = rows.getJSONObject(j);
						JSONObject materialObject = rowDetails.getJSONObject("inventory");
						// materialObject
						String productCode = materialObject.optString("number");
						String description = materialObject.optString("description");
						// line
						String unit = rowDetails.optString("uom");
						String lineNbr = rowDetails.optInt("lineNbr") + "";
						Double price = rowDetails.getDouble("unitPrice");
						String quantity = rowDetails.getInt("quantity") + "";
						String workPeriodDate = rowDetails.optString("requestedOn");
						if (unit.equals("UUR") || unit.equals("HOUR")) {
							period = new WorkPeriod(null, productCode, convertDate(workPeriodDate, "dd-MM-yyyy"), null,
									description, quantity, null, null, null);
							periods.add(period);
						} else {
							m = new Material(productCode, lineNbr, unit, description, price, quantity, null, null);
							materials.add(m);
						}
					}
					String workDate = jsonObj.getString("date");
					w = new WorkOrder(projectNumber, convertDate(workDate, "dd-MM-yyyy"), invoiceEmail, invoiceEmail,
							debtorNr, status, "niet van toepassing", materials, convertDate(workDate, "dd-MM-yyyy"), id,
							id, periods, relations, null, null, null, null, "Verkoop", workDescription, null, null,
							null, null);
					workorders.add(w);
				}
			}
		}
		return workorders;
	}
	
	public WorkOrder getSalesOrder(Token t, Settings set, WorkOrder workorder) throws Exception {
		WorkOrder w = null;
		JSONObject jsonObj = new JSONObject();
		ArrayList<String> orderTypes = null;
		if (set.getImportOffice() != null) {
			String list = set.getImportOffice().replace("[", "").replace("]", "");
			String[] strValues = list.split(",\\s");
			orderTypes = new ArrayList<String>(Arrays.asList(strValues));
		}
		String dbOrderType = orderTypes.get(0);
		String path = "/API/controller/api/v1/salesorder/" + dbOrderType + "/" + workorder.getModified();
		String parameters = "";
		
		jsonObj = (JSONObject) getJSON(t.getAccessToken(), parameters, path, "object", set);
		logger.info("Order response JSONArray " + jsonObj);
		if (jsonObj != null) {
			ArrayList<Relation> relations = new ArrayList<Relation>();
			ArrayList<Address> address = new ArrayList<Address>();
			String status = jsonObj.getString("status");
			String orderType = jsonObj.getString("orderType");
			String referentie = jsonObj.optString("customerRefNo");
			// Status is open
			String id = jsonObj.getString("orderNo");
			String projectNumber = jsonObj.getInt("project") + "";
			if (jsonObj.getInt("project") == 0) {
				projectNumber = "";
			}
			String workDescription = jsonObj.optString("description");
			
			// Relation name and number
			JSONObject customerDetails = jsonObj.getJSONObject("customer");
			String debtorId = customerDetails.optInt("internalId") + "";
			String debtorNr = customerDetails.optString("number") + "";
			String companyName = customerDetails.optString("name") + "";
			
			// Invoice contact and address details
			JSONObject invoiceContact = jsonObj.getJSONObject("soBillingContact");
			JSONObject invoiceAddress = jsonObj.getJSONObject("soBillingAddress");
			String invoiceContactName = invoiceContact.optString("name", "<leeg>");
			String invoiceEmail = invoiceContact.optString("email");
			String invoicePhone = invoiceContact.optString("phone1");
			String invoiceMobile = invoiceContact.optString("phone2");
			if (invoicePhone.equals("")) {
				invoicePhone = invoiceMobile;
			}
			String invoiceStreet = invoiceAddress.optString("addressLine1", "<leeg>");
			String invoicePostalCode = invoiceAddress.optString("postalCode", "");
			String invoiceCity = invoiceAddress.optString("city", "<leeg>");
			
			Address invoice = new Address(invoiceContactName, invoicePhone, invoiceEmail, invoiceStreet, "",
					invoicePostalCode, invoiceCity, null, "invoice", 1);
			address.add(invoice);
			
			// Delivery
			JSONObject deliveryContact = jsonObj.getJSONObject("soShippingContact");
			JSONObject deliveryAddress = jsonObj.getJSONObject("soShippingAddress");
			String deliveryContactName = invoiceContact.optString("name", "<leeg>");
			String deliveryEmail = deliveryContact.optString("email");
			String deliveryPhone = deliveryContact.optString("phone1");
			String deliveryMobile = deliveryContact.optString("phone2");
			if (deliveryPhone.equals("")) {
				deliveryPhone = deliveryMobile;
			}
			String deliveryStreet = deliveryAddress.optString("addressLine1", "<leeg>");
			String deliveryPostalCode = deliveryAddress.optString("postalCode", "");
			String deliveryCity = deliveryAddress.optString("city", "<leeg>");
			
			Address delivery = new Address(deliveryContactName, deliveryPhone, deliveryEmail, deliveryStreet, "",
					deliveryPostalCode, deliveryCity, null, "postal", 2);
			address.add(delivery);
			Relation r = new Relation(companyName, debtorNr, invoiceContactName, invoiceEmail, address, null, debtorId);
			relations.add(r);
			
			// Materials
			ArrayList<Material> materials = new ArrayList<Material>();
			ArrayList<WorkPeriod> periods = new ArrayList<WorkPeriod>();
			Material m = null;
			WorkPeriod period = null;
			JSONArray rows = jsonObj.getJSONArray("lines");
			for (int j = 0; j < rows.length(); j++) {
				JSONObject rowDetails = rows.getJSONObject(j);
				JSONObject materialObject = rowDetails.getJSONObject("inventory");
				// materialObject
				String productCode = materialObject.optString("number");
				String description = materialObject.optString("description");
				// line
				String unit = rowDetails.optString("uom");
				String lineNbr = rowDetails.optInt("lineNbr") + "";
				Double price = rowDetails.getDouble("unitPrice");
				String quantity = rowDetails.getInt("quantity") + "";
				String workPeriodDate = rowDetails.optString("requestedOn");
				if (unit.equals("UUR") || unit.equals("HOUR")) {
					period = new WorkPeriod(null, productCode, convertDate(workPeriodDate, "dd-MM-yyyy"), null,
							description, quantity, lineNbr, null, null);
					periods.add(period);
				} else {
					m = new Material(productCode, lineNbr, unit, description, price, quantity, null, null);
					materials.add(m);
				}
			}
			String workDate = jsonObj.getString("date");
			w = new WorkOrder(projectNumber, convertDate(workDate, "dd-MM-yyyy"), invoiceEmail, invoiceEmail, debtorNr,
					status, "niet van toepassing", materials, convertDate(workDate, "dd-MM-yyyy"), id, id, periods,
					relations, null, null, null, null, "Verkoop", workDescription, null, null, null, null);
		}
		return w;
	}
	
	// Set verkooporder status after sending it to WorkOrderApp
	public void setOrderStatus(Token t, String id, Boolean accepted, Settings set, ArrayList<String> orderTypes)
			throws JSONException, IOException, URISyntaxException {
		String path = "/API/controller/api/v1/salesorder/" + id;
		for (String o : orderTypes) {
			JSONObject JSONObject = new JSONObject();
			try {
				JSONObject orderType = new JSONObject();
				orderType.put("value", o);
				JSONObject.put("orderType", orderType);
				JSONObject referantie = new JSONObject();
				referantie.put("value", "WBA verzonden");
				JSONObject.put("customerRefNo", referantie);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			Object setOrderResponse = postJSON(t.getAccessToken(), JSONObject, path, "object", "PUT", set);
		}
	}
	
	// Create verkoopfactuur
	public String[] setFactuur(Token t, Settings set, String date)
			throws JSONException, IOException, URISyntaxException {
		// Get WorkOrders
		String errorMessage = "", errorDetails = "";
		int exportAmount = 0;
		int successAmount = 0;
		int errorAmount = 0;
		JSONObject JSONObject = new JSONObject();
		ArrayList<WorkOrder> allData = WorkOrderHandler.getData(t.getSoftwareToken(), "GetWorkorders",
				set.getFactuurType(), false, softwareName);
		for (WorkOrder w : allData) {
			if (w.getWorkStatus().equals("1") && (w.getModified().equals("") || w.getModified() == null)) {
				exportAmount++;
				// Set jsonObject with salesInvoice data
				JSONObject = factuurJSON(w, t, set.getRoundedHours(), set);
				String error = (String) JSONObject.opt("Error");
				if (error != null) {
					errorDetails += error;
					errorAmount++;
					WorkOrderHandler.setWorkorderPlusStatus(w.getId(), w.getWorkorderNr(), "99", "UpdateWorkorder",
							t.getSoftwareToken(), softwareName);
				} else {
					logger.info("REQUEST salesinvoice" + JSONObject);
					String path = "/API/controller/api/v1/customerinvoice";
					Object response = postJSON(t.getAccessToken(), JSONObject, path, "object", "POST", set);
					logger.info("RESPONSE salesinvoice" + response);
					if (response instanceof Boolean) {
						successAmount++;
						WorkOrderHandler.setWorkorderStatus(w.getId(), w.getWorkorderNr(), true, "GetWorkorder",
								t.getSoftwareToken(), softwareName);
					} else {
						WorkOrderHandler.setWorkorderPlusStatus(w.getId(), w.getWorkorderNr(), "99", "UpdateWorkorder",
								t.getSoftwareToken(), softwareName);
						JSONObject errorResponse = (JSONObject) response;
						errorMessage += errorResponse.getString("message") + "<br>";
						errorAmount++;
					}
				}
			}
		}
		if (successAmount > 0) {
			errorMessage += "<br>" + successAmount + " workorders exported.<br>";
		}
		if (errorAmount > 0) {
			errorMessage += errorAmount + " out of " + exportAmount + " workorders have errors. Click for details<br>";
		}
		return new String[] { errorMessage, errorDetails };
	}
	
	public JSONObject factuurJSON(WorkOrder w, Token t, int roundedHours, Settings set) throws JSONException {
		
		JSONArray JSONArrayMaterials = null;
		String error = "";
		
		if (w.getMaterials().size() == 0 && w.getWorkPeriods().size() == 0) {
			error += "No materials/workperiods found on workorder " + w.getWorkorderNr() + "\n";
			return new JSONObject().put("Error", error);
		}
		JSONObject JSONObject = new JSONObject();
		try {
			// Map date
			String workDate = w.getWorkDate();
			String workEndDate = w.getWorkEndDate();
			try {
				SimpleDateFormat dt = new SimpleDateFormat("dd-MM-yyyy");
				
				if (workEndDate.equals("")) {
					workEndDate = getCurrentDate(null);
				} else {
					Date formatDate1 = dt.parse(w.getWorkEndDate());
					SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
					workEndDate = dt1.format(formatDate1);
				}
				if (workDate.equals("")) {
					workDate = getCurrentDate(null);
				} else {
					Date formatDate = dt.parse(w.getWorkDate());
					SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
					workDate = dt1.format(formatDate);
				}
				
			} catch (ParseException e) {
				e.printStackTrace();
			}
			JSONObject object = new JSONObject();
			object.put("value", workEndDate);
			JSONObject.put("documentDueDate", object);
			
			object = new JSONObject();
			object.put("value", "Vanuit WBA - " + w.getWorkorderNr());
			JSONObject.put("externalReference", object);
			
			if (w.getProjectNr() != null && !w.getProjectNr().equals("")) {
				object = new JSONObject();
				object.put("value", w.getProjectNr());
				JSONObject.put("project", object);
			}
			
			object = new JSONObject();
			object.put("value", w.getCustomerDebtorNr());
			JSONObject.put("customerNumber", object);
			
			Relation dbRelation = ObjectDAO.getRelation(t.getSoftwareToken(), w.getCustomerDebtorNr(), "contact");
			String id = null;
			if (dbRelation == null) {
				if (set.getExportObjects() != null && set.getExportObjects().contains("relations")) {
					// JSONObject object = setRelation(t, w);
					// id = object.optString("id");
					// newRelation++;
				} else {
					error += "Relation " + w.getCustomerDebtorNr() + " on workorder " + w.getWorkorderNr()
							+ " not found in Visma.net or relation is not synchronized\n";
					return new JSONObject().put("Error", error);
				}
			} else {
				id = dbRelation.getId();
			}
			JSONArrayMaterials = new JSONArray();
			int lineNumber = 0;
			for (Material m : w.getMaterials()) {
				Material dbMaterial = ObjectDAO.getMaterial(t.getSoftwareToken(), m.getCode());
				if (Double.parseDouble(m.getQuantity()) == 0) {
					error += "The quantity of material " + m.getCode() + " on workorder " + w.getWorkorderNr()
							+ " has to be greater then 0\n";
					return new JSONObject().put("Error", error);
				}
				if (dbMaterial == null) {
					if (set.getExportObjects() != null && set.getExportObjects().contains("materials")) {
						// JSONObject object = setMaterial(t, m);
						// materialId = object.optString("id");
						// newMaterial++;
					} else {
						error += "Material " + m.getCode() + " on workorder " + w.getWorkorderNr()
								+ " not found in Visma.net or this material is not synchronized\n";
						return new JSONObject().put("Error", error);
					}
					
				}
				// else {
				// materialId = dbMaterial.getId();
				// }
				JSONObject json = new JSONObject();
				json.put("operation", "Insert");
				object = new JSONObject();
				object.put("value", m.getCode());
				json.put("inventoryNumber", object);
				
				object = new JSONObject();
				object.put("value", lineNumber);
				json.put("lineNumber", object);
				
				object = new JSONObject();
				object.put("value", m.getDescription());
				json.put("description", object);
				
				object = new JSONObject();
				object.put("value", m.getQuantity());
				json.put("quantity", object);
				
				object = new JSONObject();
				object.put("value", m.getUnit());
				json.put("uom", object);
				
				object = new JSONObject();
				object.put("value", m.getPrice());
				json.put("unitPriceInCurrency", object);
				
				JSONArrayMaterials.put(json);
				lineNumber++;
				
			}
			for (WorkPeriod p : w.getWorkPeriods()) {
				HourType h = ObjectDAO.getHourType(t.getSoftwareToken(), p.getHourType());
				// Get ID from db(hourtype)
				if (h == null) {
					error += "Hourtype " + p.getHourType()
							+ " not found in Visma.net or hourtype is not synchronized\n";
					return new JSONObject().put("Error", error);
				} else {
					JSONObject json = new JSONObject();
					double number = p.getDuration();
					double hours = roundedHours;
					double urenInteger = (number % hours);
					if (urenInteger < (hours / 2)) {
						number = number - urenInteger;
					} else {
						number = number - urenInteger + hours;
					}
					double quantity = (number / 60);
					DecimalFormat df = new DecimalFormat("#.##");
					String formatted = df.format(quantity);
					quantity = Double.parseDouble(formatted.toString().replaceAll(",", "."));
					
					json.put("operation", "Insert");
					object = new JSONObject();
					object.put("value", p.getHourType());
					json.put("inventoryNumber", object);
					
					object = new JSONObject();
					object.put("value", lineNumber);
					json.put("lineNumber", object);
					
					object = new JSONObject();
					if (p.getDescription().equals("")) {
						object.put("value", h.getName());
					} else {
						object.put("value", p.getDescription());
					}
					json.put("description", object);
					
					object = new JSONObject();
					object.put("value", quantity);
					json.put("quantity", object);
					
					object = new JSONObject();
					object.put("value", "UUR");
					json.put("uom", object);
					
					DecimalFormat df1 = new DecimalFormat("#.##");
					String formatted1 = df1.format(h.getSalePrice());
					Double unitPrice = Double.parseDouble(formatted1.toString().replaceAll(",", "."));
					
					object = new JSONObject();
					object.put("value", unitPrice);
					json.put("unitPriceInCurrency", object);
					
					JSONArrayMaterials.put(json);
					lineNumber++;
				}
			}
			JSONObject.put("invoiceLines", JSONArrayMaterials);
		} catch (JSONException | SQLException e) {
			e.printStackTrace();
		}
		if (error.equals("")) {
			return JSONObject;
		} else {
			return new JSONObject().put("Error", error);
		}
	}
	
	// Create verkoopfactuur
	public String[] setSalesOrder(Token t, Settings set, String date) throws Exception {
		// Get WorkOrders
		String errorMessage = "", errorDetails = "";
		int exportAmount = 0;
		int successAmount = 0;
		int errorAmount = 0;
		JSONObject JSONObject = new JSONObject();
		ArrayList<WorkOrder> allData = WorkOrderHandler.getData(t.getSoftwareToken(), "GetWorkorders",
				set.getFactuurType(), false, softwareName);
		for (WorkOrder w : allData) {
			Object response = null;
			if (w.getWorkStatus().equals("1")) {
				exportAmount++;
				// Update existing salesorder
				if (w.getModified() != null && !w.getModified().equals("")) {
					// w.getModified(); = extern werkbonnummer (salesorder
					// nummer)
					String path = "/API/controller/api/v1/salesorder/" + w.getModified();
					// Get JSONObject with workorder data for PUT request
					JSONObject = putSalesorderJSON(w, t, set.getRoundedHours(), set);
					String error = (String) JSONObject.opt("Error");
					if (error != null) {
						errorDetails += error;
						errorAmount++;
					} else {
						logger.info("REQUEST salesorder" + JSONObject);
						response = postJSON(t.getAccessToken(), JSONObject, path, "object", "PUT", set);
						logger.info("RESPONSE salesorder" + response);
					}
					// Create new salesorder
				} else {
					// Set jsonObject with salesInvoice data
					JSONObject = postSalesorderJSON(w, t, set.getRoundedHours(), set);
					String error = (String) JSONObject.opt("Error");
					if (error != null) {
						errorDetails += error;
						errorAmount++;
					} else {
						logger.info("REQUEST salesorder" + JSONObject);
						String path = "/API/controller/api/v1/salesorder";
						response = postJSON(t.getAccessToken(), JSONObject, path, "object", "POST", set);
						logger.info("RESPONSE salesorder" + response);
					}
				}
				
				if (response instanceof Boolean) {
					successAmount++;
					WorkOrderHandler.setWorkorderStatus(w.getId(), w.getWorkorderNr(), true, "GetWorkorder",
							t.getSoftwareToken(), softwareName);
				} else {
					WorkOrderHandler.setWorkorderPlusStatus(w.getId(), w.getWorkorderNr(), "99", "UpdateWorkorder",
							t.getSoftwareToken(), softwareName);
					if (response != null) {
						JSONObject errorResponse = (JSONObject) response;
						errorMessage += errorResponse.optString("message") + "<br>";
						errorAmount++;
					}
				}
			}
		}
		if (successAmount > 0) {
			errorMessage += "<br>" + successAmount + " workorders exported.<br>";
		}
		if (errorAmount > 0) {
			if (errorDetails.equals("") || errorDetails == null) {
				errorMessage += errorAmount + " out of " + exportAmount + " workorders have errors.<br>";
			} else {
				errorMessage += errorAmount + " out of " + exportAmount
						+ " workorders have errors. Click for details<br>";
			}
		}
		return new String[] { errorMessage, errorDetails };
	}
	
	public JSONObject putSalesorderJSON(WorkOrder w, Token t, int roundedHours, Settings set) throws Exception {
		JSONArray JSONArrayMaterials = null;
		String error = "";
		
		if (w.getMaterials().size() == 0 && w.getWorkPeriods().size() == 0) {
			error += "No materials/workperiods found on workorder " + w.getWorkorderNr() + "\n";
			return new JSONObject().put("Error", error);
		}
		JSONObject JSONObject = new JSONObject();
		try {
			// Map date
			String workDate = w.getWorkDate();
			String workEndDate = w.getWorkEndDate();
			try {
				SimpleDateFormat dt = new SimpleDateFormat("dd-MM-yyyy");
				
				if (workEndDate.equals("")) {
					workEndDate = getCurrentDate(null);
				} else {
					Date formatDate1 = dt.parse(w.getWorkEndDate());
					SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
					workEndDate = dt1.format(formatDate1);
				}
				if (workDate.equals("")) {
					workDate = getCurrentDate(null);
				} else {
					Date formatDate = dt.parse(w.getWorkDate());
					SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
					workDate = dt1.format(formatDate);
				}
				
			} catch (ParseException e) {
				e.printStackTrace();
			}
			JSONObject object = new JSONObject();
			if (w.getProjectNr() != null && !w.getProjectNr().equals("")) {
				object.put("value", w.getProjectNr());
				JSONObject.put("project", object);
			}
			
			object = new JSONObject();
			object.put("value", "Vanuit WBA - " + w.getWorkorderNr());
			JSONObject.put("customerRefNo", object);
			
			ArrayList<String> orderTypes = null;
			if (set.getImportOffice() != null) {
				String list = set.getImportOffice().replace("[", "").replace("]", "");
				String[] strValues = list.split(",\\s");
				orderTypes = new ArrayList<String>(Arrays.asList(strValues));
			}
			
			object = new JSONObject();
			object.put("value", orderTypes.get(0));
			JSONObject.put("orderType", object);
			
			object = new JSONObject();
			object.put("value", w.getWorkDescription());
			JSONObject.put("description", object);
			
			if (w.getProjectNr() != null && !w.getProjectNr().equals("")) {
				object = new JSONObject();
				object.put("value", w.getProjectNr());
				JSONObject.put("project", object);
			}
			// Get current salesorder from Visma
			JSONArrayMaterials = new JSONArray();
			WorkOrder salesorder = getSalesOrder(t, set, w);
			if (salesorder == null) {
				error += getSalesorderError;
				return new JSONObject().put("Error", error);
			}
			
			ArrayList<Material> materials = new ArrayList<Material>();
			ArrayList<String> tempMaterials = new ArrayList<String>();
			ArrayList<Material> otherMaterials = w.getMaterials();
			for (Material m : w.getMaterials()) {
				Material dbMaterial = ObjectDAO.getMaterial(t.getSoftwareToken(), m.getCode());
				if (Double.parseDouble(m.getQuantity()) == 0) {
					error += "The quantity of material " + m.getCode() + " on workorder " + w.getWorkorderNr()
							+ " has to be greater then 0\n";
					return new JSONObject().put("Error", error);
				}
				if (dbMaterial == null) {
					if (set.getExportObjects() != null && set.getExportObjects().contains("materials")) {
						Object response = setMaterial(t, set, m);
						if (response instanceof Boolean) {
						} else {
							JSONObject errorResponse = (JSONObject) response;
							error += errorResponse.getString("message") + "<br>";
							return new JSONObject().put("Error", error);
						}
						// materialId = object.optString("id");
						// newMaterial++;
					} else {
						error += "Material " + m.getCode() + " on workorder " + w.getWorkorderNr()
								+ " not found in Visma.net or this material is not synchronized\n";
						return new JSONObject().put("Error", error);
					}
				}
				// else {
				// materialId = dbMaterial.getId();
				// }
				
				for (Material orderMaterial : salesorder.getMaterials()) {
					if (orderMaterial.getCode().equals(m.getCode()) && !tempMaterials.contains(m.getCode())) {
						JSONObject tempJson = new JSONObject();
						object = new JSONObject();
						object.put("value", orderMaterial.getSubCode());
						tempJson.put("lineNbr", object);
						
						tempJson.put("operation", "Update");
						object = new JSONObject();
						object.put("value", m.getCode());
						tempJson.put("inventoryNumber", object);
						
						object = new JSONObject();
						object.put("value", m.getDescription());
						tempJson.put("lineDescription", object);
						
						object = new JSONObject();
						object.put("value", m.getQuantity());
						tempJson.put("quantity", object);
						
						object = new JSONObject();
						object.put("value", m.getPrice());
						tempJson.put("unitPrice", object);
						JSONArrayMaterials.put(tempJson);
						materials.add(m);
						// add m.getCode to check for multiple materials
						tempMaterials.add(m.getCode());
						break;
					}
				}
			}
			otherMaterials.removeAll(materials);
			for (Material orderMaterial : otherMaterials) {
				JSONObject tempJson = new JSONObject();
				tempJson.put("operation", "Insert");
				object = new JSONObject();
				object.put("value", orderMaterial.getCode());
				tempJson.put("inventoryNumber", object);
				
				object = new JSONObject();
				object.put("value", orderMaterial.getDescription());
				tempJson.put("lineDescription", object);
				
				object = new JSONObject();
				object.put("value", orderMaterial.getQuantity());
				tempJson.put("quantity", object);
				
				object = new JSONObject();
				object.put("value", orderMaterial.getPrice());
				tempJson.put("unitPrice", object);
				JSONArrayMaterials.put(tempJson);
			}
			ArrayList<WorkPeriod> workperiods = new ArrayList<WorkPeriod>();
			ArrayList<WorkPeriod> otherWorkperiods = w.getWorkPeriods();
			ArrayList<String> tempWorkperiods = new ArrayList<String>();
			for (WorkPeriod p : w.getWorkPeriods()) {
				HourType h = ObjectDAO.getHourType(t.getSoftwareToken(), p.getHourType());
				// Get ID from db(hourtype)
				if (h == null) {
					error += "Hourtype " + p.getHourType()
							+ " not found in Visma.net or hourtype is not synchronized\n";
					return new JSONObject().put("Error", error);
				}
				for (WorkPeriod orderPeriod : salesorder.getWorkPeriods()) {
					if (orderPeriod.getHourType().equals(p.getHourType())
							&& !tempWorkperiods.contains(p.getHourType())) {
						JSONObject tempJson = new JSONObject();
						object = new JSONObject();
						object.put("value", orderPeriod.getId());
						tempJson.put("lineNbr", object);
						double number = p.getDuration();
						double hours = roundedHours;
						double urenInteger = (number % hours);
						if (urenInteger < (hours / 2)) {
							number = number - urenInteger;
						} else {
							number = number - urenInteger + hours;
						}
						double quantity = (number / 60);
						DecimalFormat df = new DecimalFormat("#.##");
						String formatted = df.format(quantity);
						quantity = Double.parseDouble(formatted.toString().replaceAll(",", "."));
						
						tempJson.put("operation", "Update");
						object = new JSONObject();
						object.put("value", p.getHourType());
						tempJson.put("inventoryNumber", object);
						
						object = new JSONObject();
						if (p.getDescription().equals("")) {
							object.put("value", h.getName());
						} else {
							object.put("value", p.getDescription());
						}
						tempJson.put("lineDescription", object);
						
						object = new JSONObject();
						object.put("value", quantity);
						tempJson.put("quantity", object);
						
						JSONArrayMaterials.put(tempJson);
						workperiods.add(p);
						tempWorkperiods.add(p.getHourType());
						break;
					}
				}
			}
			otherWorkperiods.removeAll(workperiods);
			for (WorkPeriod period : otherWorkperiods) {
				double number = period.getDuration();
				double hours = roundedHours;
				double urenInteger = (number % hours);
				if (urenInteger < (hours / 2)) {
					number = number - urenInteger;
				} else {
					number = number - urenInteger + hours;
				}
				double quantity = (number / 60);
				DecimalFormat df = new DecimalFormat("#.##");
				String formatted = df.format(quantity);
				quantity = Double.parseDouble(formatted.toString().replaceAll(",", "."));
				
				JSONObject tempJson = new JSONObject();
				tempJson.put("operation", "Insert");
				object = new JSONObject();
				object.put("value", period.getHourType());
				tempJson.put("inventoryNumber", object);
				
				HourType h = ObjectDAO.getHourType(t.getSoftwareToken(), period.getHourType());
				object = new JSONObject();
				if (period.getDescription().equals("")) {
					object.put("value", h.getName());
				} else {
					object.put("value", period.getDescription());
				}
				tempJson.put("lineDescription", object);
				
				object = new JSONObject();
				object.put("value", quantity);
				tempJson.put("quantity", object);
				
				JSONArrayMaterials.put(tempJson);
			}
			JSONObject.put("lines", JSONArrayMaterials);
		} catch (JSONException | SQLException e) {
			e.printStackTrace();
		}
		if (error.equals("")) {
			return JSONObject;
		} else {
			return new JSONObject().put("Error", error);
		}
	}
	
	public JSONObject postSalesorderJSON(WorkOrder w, Token t, int roundedHours, Settings set) throws Exception {
		JSONArray JSONArrayMaterials = null;
		String error = "";
		
		if (w.getMaterials().size() == 0 && w.getWorkPeriods().size() == 0) {
			error += "No materials/workperiods found on workorder " + w.getWorkorderNr() + "\n";
			return new JSONObject().put("Error", error);
		}
		JSONObject JSONObject = new JSONObject();
		try {
			// Map date
			String workDate = w.getWorkDate();
			String workEndDate = w.getWorkEndDate();
			try {
				SimpleDateFormat dt = new SimpleDateFormat("dd-MM-yyyy");
				
				if (workEndDate.equals("")) {
					workEndDate = getCurrentDate(null);
				} else {
					Date formatDate1 = dt.parse(w.getWorkEndDate());
					SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
					workEndDate = dt1.format(formatDate1);
				}
				if (workDate.equals("")) {
					workDate = getCurrentDate(null);
				} else {
					Date formatDate = dt.parse(w.getWorkDate());
					SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
					workDate = dt1.format(formatDate);
				}
				
			} catch (ParseException e) {
				e.printStackTrace();
			}
			JSONObject object = new JSONObject();
			if (w.getProjectNr() != null && !w.getProjectNr().equals("")) {
				object.put("value", w.getProjectNr());
				JSONObject.put("project", object);
			}
			
			object = new JSONObject();
			object.put("value", "Vanuit WBA - " + w.getWorkorderNr());
			JSONObject.put("customerRefNo", object);
			
			ArrayList<String> orderTypes = null;
			if (set.getImportOffice() != null) {
				String list = set.getImportOffice().replace("[", "").replace("]", "");
				String[] strValues = list.split(",\\s");
				orderTypes = new ArrayList<String>(Arrays.asList(strValues));
			}
			
			object = new JSONObject();
			object.put("value", orderTypes.get(0));
			JSONObject.put("orderType", object);
			
			object = new JSONObject();
			object.put("value", w.getWorkDescription());
			JSONObject.put("description", object);
			
			object = new JSONObject();
			object.put("value", w.getCustomerDebtorNr());
			JSONObject.put("customer", object);
			
			if (w.getProjectNr() != null && !w.getProjectNr().equals("")) {
				object = new JSONObject();
				object.put("value", w.getProjectNr());
				JSONObject.put("project", object);
			}
			
			JSONArrayMaterials = new JSONArray();
			JSONObject json = new JSONObject();
			for (Material m : w.getMaterials()) {
				if (Double.parseDouble(m.getQuantity()) == 0) {
					error += "The quantity of material " + m.getCode() + " on workorder " + w.getWorkorderNr()
							+ " has to be greater then 0\n";
					return new JSONObject().put("Error", error);
				}
				Material dbMaterial = ObjectDAO.getMaterial(t.getSoftwareToken(), m.getCode());
				if (dbMaterial == null) {
					if (set.getExportObjects() != null && set.getExportObjects().contains("materials")) {
						Object response = setMaterial(t, set, m);
						if (response instanceof Boolean) {
						} else {
							JSONObject errorResponse = (JSONObject) response;
							error += errorResponse.getString("message") + "<br>";
							return new JSONObject().put("Error", error);
						}
					} else {
						error += "Material " + m.getCode() + " on workorder " + w.getWorkorderNr()
								+ " not found in Visma.net or this material is not synchronized\n";
						return new JSONObject().put("Error", error);
					}
				}
				// else {
				// materialId = dbMaterial.getId();
				// }
				object = new JSONObject();
				
				json.put("operation", "Insert");
				object = new JSONObject();
				object.put("value", m.getCode());
				json.put("inventoryNumber", object);
				
				object = new JSONObject();
				object.put("value", m.getDescription());
				json.put("lineDescription", object);
				
				object = new JSONObject();
				object.put("value", m.getQuantity());
				json.put("quantity", object);
				
				object = new JSONObject();
				object.put("value", m.getPrice());
				json.put("unitPrice", object);
				JSONArrayMaterials.put(json);
				
			}
			for (WorkPeriod p : w.getWorkPeriods()) {
				HourType h = ObjectDAO.getHourType(t.getSoftwareToken(), p.getHourType());
				// Get ID from db(hourtype)
				if (h == null) {
					error += "Hourtype " + p.getHourType()
							+ " not found in Visma.net or hourtype is not synchronized\n";
					return new JSONObject().put("Error", error);
				} else {
					json = new JSONObject();
					double number = p.getDuration();
					double hours = roundedHours;
					double urenInteger = (number % hours);
					if (urenInteger < (hours / 2)) {
						number = number - urenInteger;
					} else {
						number = number - urenInteger + hours;
					}
					double quantity = (number / 60);
					DecimalFormat df = new DecimalFormat("#.##");
					String formatted = df.format(quantity);
					quantity = Double.parseDouble(formatted.toString().replaceAll(",", "."));
					
					object = new JSONObject();
					object.put("value", p.getHourType());
					json.put("inventoryNumber", object);
					
					object = new JSONObject();
					if (p.getDescription().equals("")) {
						object.put("value", h.getName());
					} else {
						object.put("value", p.getDescription());
					}
					json.put("lineDescription", object);
					
					object = new JSONObject();
					object.put("value", quantity);
					json.put("quantity", object);
					
					DecimalFormat df1 = new DecimalFormat("#.##");
					String formatted1 = df1.format(h.getSalePrice());
					Double unitPrice = Double.parseDouble(formatted1.toString().replaceAll(",", "."));
					
					object = new JSONObject();
					object.put("value", unitPrice);
					json.put("unitPrice", object);
					json.put("operation", "Insert");
					JSONArrayMaterials.put(json);
				}
			}
			JSONObject.put("lines", JSONArrayMaterials);
		} catch (JSONException | SQLException e) {
			e.printStackTrace();
		}
		if (error.equals("")) {
			return JSONObject;
		} else {
			return new JSONObject().put("Error", error);
		}
	}
	
	// Create new material
	public Object setMaterial(Token t, Settings set, Material m) throws Exception {
		JSONObject materiaal = new JSONObject();
		JSONObject object = new JSONObject();
		
		object.put("value", m.getCode());
		materiaal.put("inventoryNumber", object);
		
		object = new JSONObject();
		object.put("value", m.getDescription());
		materiaal.put("description", object);
		
		object = new JSONObject();
		object.put("value", "Active");
		materiaal.put("status", object);
		
		object = new JSONObject();
		object.put("value", m.getPrice());
		materiaal.put("defaultPrice", object);
		
		object = new JSONObject();
		object.put("value", m.getUnit());
		materiaal.put("baseUnit", object);
		
		String type = null;
		if (set.getUser() != null) {
			String list = set.getUser().replace("[", "").replace("]", "");
			String[] strValues = list.split(",\\s");
			ArrayList<String> dbMaterialTypes = new ArrayList<String>(Arrays.asList(strValues));
			type = dbMaterialTypes.get(0);
		}
		object = new JSONObject();
		object.put("value", type);
		materiaal.put("itemClass", object);
		
		object = new JSONObject();
		object.put("value", type);
		materiaal.put("postingClass", object);
		
		String path = "/API/controller/api/v1/inventory";
		Object response = postJSON(t.getAccessToken(), materiaal, path, "object", "POST", set);
		return response;
	}
}