package controller.drivefx;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import DAO.ObjectDAO;
import DAO.TokenDAO;
import controller.WorkOrderHandler;
import controller.twinfield.SoapHandler;
import object.Settings;
import object.Token;
import object.workorder.Address;
import object.workorder.Employee;
import object.workorder.HourType;
import object.workorder.Material;
import object.workorder.Project;
import object.workorder.Relation;
import object.workorder.WorkOrder;
import object.workorder.WorkPeriod;

public class DriveFxHandler {
	final String softwareName = "DriveFx";
	private final static Logger logger = Logger.getLogger(SoapHandler.class.getName());
	// Change to environment variable
	private static String host = System.getenv("DRIVEFX_HOST");
	
	public static Object postJSON(String accessToken, String path, JSONObject params, String returnType)
			throws IOException, URISyntaxException {
		String link = host + path;
		Object object = null;
		byte[] postData = params.toString().getBytes(StandardCharsets.UTF_8);
		int postDataLength = postData.length;
		
		try {
			URL url = new URL(link);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			// conn.setDoInput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("charset", "utf-8");
			conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
			conn.setRequestProperty("Authorization", accessToken);
			conn.setUseCaches(false);
			
			try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
				wr.write(postData);
			}
			BufferedReader br = new BufferedReader(
					new InputStreamReader((conn.getInputStream()), StandardCharsets.UTF_8));
			System.out.println("PARAMETERS " + params);
			String jsonString;
			while ((jsonString = br.readLine()) != null) {
				switch (returnType) {
				case "array":
					object = new JSONArray(jsonString);
					break;
				case "object":
					object = new JSONObject(jsonString);
					break;
				}
			}
			conn.disconnect();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return object;
	}
	
	public String getCurrentDate(String date) {
		String timestamp;
		ZonedDateTime za = ZonedDateTime.now(ZoneId.of("Europe/Paris"));
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		if (date != null) {
			timestamp = date;
		} else {
			timestamp = za.format(formatter);
		}
		return timestamp;
	}
	
	public String convertDate(String string) {
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		Date date = null;
		try {
			// String to date
			date = format.parse(string);
			// Create Calender to edit time
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			cal.add(Calendar.DAY_OF_MONTH, 0);
			date = cal.getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		// Date to String
		Format formatter = new SimpleDateFormat("dd-MM-yyyy");
		String s = formatter.format(date);
		return s;
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
		Format formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		String s = formatter.format(date);
		return s;
	}
	
	private JSONObject searchEntities(String entityName, String date) {
		JSONObject jsonObject = null;
		
		try {
			jsonObject = new JSONObject();
			JSONArray array = new JSONArray();
			if (date != null) {
				System.out.println("DATE " + date);
				JSONObject filterItem = new JSONObject();
				filterItem.put("filterItem", "ousrdata");
				// Comparison 3 = GREATER OR EQUAL
				filterItem.put("comparison", 3);
				filterItem.put("valueItem", date);
				filterItem.put("groupItem", 0);
				array.put(filterItem);
			}
			
			JSONObject queryObject = new JSONObject();
			queryObject.put("distinct", true);
			queryObject.put("groupByItems", new JSONArray());
			queryObject.put("orderByItems", new JSONArray());
			queryObject.put("SelectItems", new JSONArray());
			queryObject.put("filterItems", array);
			queryObject.put("entityName", entityName);
			jsonObject.put("queryObject", queryObject);
		} catch (JSONException e) {
			e.printStackTrace();
			
		}
		System.out.println("SearchEntityObject " + jsonObject);
		return jsonObject;
		
	}
	
	public String[] getEmployees(Token t, String date) throws Exception {
		String entityName = "User";
		boolean checkUpdate = false;
		String errorMessage = "";
		int importCount = 0;
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		ArrayList<Employee> emp = new ArrayList<Employee>();
		String path = "searchEntities";
		JSONObject parameters = searchEntities(entityName, null);
		
		// Search for all users
		jsonObj = (JSONObject) postJSON(t.getAccessToken(), path, parameters, "object");
		System.out.println("EMPLOYEEJSON " + jsonObj);
		int code = jsonObj.getInt("code");
		if (code == 0) {
			jsonArray = jsonObj.getJSONArray("entities");
			for (int i = 0; i < jsonArray.length(); i++) {
				importCount++;
				System.out.println("Employee " + i + " importeren");
				JSONObject json = jsonArray.getJSONObject(i);
				String[] fullName = json.getString("username").split("\\s+");
				String firstName = fullName[0];
				int j = 1;
				String lastName = "";
				while (j < fullName.length) {
					lastName += " " + fullName[j];
					j++;
				}
				if (lastName.equals("")) {
					lastName = " ";
				}
				String number = json.getInt("userno") + "";
				Employee e = new Employee(firstName, lastName, number);
				emp.add(e);
			}
		}
		
		if (!emp.isEmpty()) {
			// Post data to WorkorderApp
			int successAmount = (int) WorkOrderHandler.addData(t.getSoftwareToken(), emp, "employees", softwareName);
			if (successAmount > 0) {
				ObjectDAO.saveEmployees(emp, t.getSoftwareToken());
				errorMessage += importCount + " employees imported<br>";
				checkUpdate = false;
			} else {
				errorMessage += "Something went wrong with employees<br />";
			}
		} else {
			errorMessage += "No employees for import<br>";
		}
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	// Get materials and hourtypes
	public String[] getMaterials(Token t, String date) throws Exception {
		String entityName = "St";
		boolean checkUpdate = false;
		String errorMessage = "";
		int importCount = 0;
		int importCount2 = 0;
		int editCount = 0;
		int editCount2 = 0;
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		ArrayList<Material> materials = new ArrayList<Material>();
		ArrayList<HourType> hourtypes = new ArrayList<HourType>();
		String path = "searchEntities";
		JSONObject parameters = searchEntities(entityName, getDateMinHour(date));
		
		// Search for all materials
		jsonObj = (JSONObject) postJSON(t.getAccessToken(), path, parameters, "object");
		System.out.println("MaterialJSON " + jsonObj);
		int code = jsonObj.getInt("code");
		if (code == 0) {
			jsonArray = jsonObj.getJSONArray("entities");
			for (int i = 0; i < jsonArray.length(); i++) {
				System.out.println("Materiaal " + (i + 1) + " importeren");
				JSONObject json = jsonArray.getJSONObject(i);
				String productCode = json.getString("ref");
				String id = json.getString("ststamp");
				String unitName = json.getString("unidade");
				String description = json.getString("design");
				Double price = json.getDouble("epv1");
				String modified = json.getString("usrdata");
				
				// Check if object has unit hour
				if (unitName.equals("Hour") || unitName.equals("Hora")) {
					String dbModifiedHourtype = ObjectDAO.getModifiedDate(t.getSoftwareToken(), null, productCode,
							"hourtypes");
					if (dbModifiedHourtype == null || date == null) {
						importCount2++;
					} else {
						editCount2++;
					}
					HourType h = new HourType(productCode, description, 0, 1, 0, price, 1, modified, id);
					hourtypes.add(h);
				} else {
					String dbModified = ObjectDAO.getModifiedDate(t.getSoftwareToken(), null, productCode, "materials");
					if (dbModified == null || date == null) {
						importCount++;
					} else {
						editCount++;
					}
					Material m = new Material(productCode, null, unitName, description, price, null, modified, id);
					materials.add(m);
				}
			}
		} else {
			System.out.println("CODE " + code + " ERROR");
		}
		
		// Materials log message
		if (!materials.isEmpty()) {
			int successAmount = (int) WorkOrderHandler.addData(t.getSoftwareToken(), materials, "materials",
					softwareName);
			if (successAmount > 0) {
				ObjectDAO.saveMaterials(materials, t.getSoftwareToken());
				errorMessage += importCount + " materials imported<br>";
				errorMessage += "and " + editCount + " materials edited<br>";
				checkUpdate = true;
			} else {
				errorMessage += "Something went wrong with materials<br>";
			}
		} else {
			errorMessage += "No materials for import<br>";
		}
		// Hourtypes log message
		if (!hourtypes.isEmpty()) {
			int successAmount2 = (int) WorkOrderHandler.addData(t.getSoftwareToken(), hourtypes, "hourtypes",
					softwareName);
			if (successAmount2 > 0) {
				ObjectDAO.saveHourTypes(hourtypes, t.getSoftwareToken());
				errorMessage += importCount2 + " hourtypes imported<br>";
				errorMessage += "and " + editCount2 + " hourtypes edited<br>";
				checkUpdate = true;
			} else {
				errorMessage += "Something went wrong with hourtypes<br>";
			}
		} else {
			errorMessage += "No hourtypes for import<br>";
		}
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	public String[] getRelations(Token t, String date) throws Exception {
		String entityName = "Cl";
		boolean checkUpdate = false;
		String errorMessage = "";
		int importCount = 0;
		int editCount = 0;
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		ArrayList<Relation> relations = new ArrayList<Relation>();
		String path = "searchEntities";
		JSONObject parameters = searchEntities(entityName, getDateMinHour(date));
		
		// Search for all users
		jsonObj = (JSONObject) postJSON(t.getAccessToken(), path, parameters, "object");
		logger.info("Relation response JSON " + jsonObj);
		int code = jsonObj.getInt("code");
		if (code == 0) {
			jsonArray = jsonObj.getJSONArray("entities");
			for (int i = 0; i < jsonArray.length(); i++) {
				ArrayList<Address> address = new ArrayList<Address>();
				JSONObject customerDetails = jsonArray.getJSONObject(i);
				String id = customerDetails.getString("clstamp");
				String modified = customerDetails.optString("ousrdata", "");
				String debtorNr = customerDetails.optInt("no") + "";
				String dbModified = ObjectDAO.getModifiedDate(t.getSoftwareToken(), "invoice", debtorNr, "relations");
				// Check if data is modified
				if (dbModified == null || date == null) {
					importCount++;
				} else {
					editCount++;
				}
				// Invoice
				String firstName = customerDetails.optString("cnome1", "<empty>");
				String companyName = customerDetails.optString("nome", "<empty>");
				String contact = firstName;
				String mobileNr = customerDetails.optString("tlmvl", "");
				String phoneNr = customerDetails.optString("telefone", "");
				if (!mobileNr.equals("")) {
					phoneNr = mobileNr;
				}
				String email = customerDetails.optString("email", "");
				String contactEmail = customerDetails.optString("cemail1", "");
				String street = customerDetails.optString("morada", "<empty>");
				String postalCode = customerDetails.optString("codpost", "<empty>");
				String city = customerDetails.optString("local", "<empty>");
				String remark = customerDetails.optString("obs", "");
				Address invoice = new Address(contact, phoneNr, contactEmail, street, "", postalCode, city, remark,
						"invoice", 1);
				address.add(invoice);
				
				Relation r = new Relation(companyName, debtorNr, contact, email, address, modified, id);
				relations.add(r);
			}
		}
		if (!relations.isEmpty()) {
			int successAmount = (int) WorkOrderHandler.addData(t.getSoftwareToken(), relations, "relations",
					softwareName);
			if (successAmount > 0) {
				ObjectDAO.saveRelations(relations, t.getSoftwareToken());
				errorMessage += importCount + " relations imported<br>";
				errorMessage += "and " + editCount + " relations edited<br>";
				checkUpdate = true;
			} else {
				errorMessage += "Something went wrong with relations<br>";
			}
		} else {
			errorMessage += "No relations for import<br>";
		}
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	// Create invoice
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
			exportAmount++;
			JSONObject = factuurJSON(w, t, set.getRoundedHours());
			String error = (String) JSONObject.opt("Error");
			if (error != null) {
				errorDetails += error;
				errorAmount++;
			} else {
				logger.info("REQUEST " + JSONObject);
				JSONObject response = null;
				String path = "createDocument";
				response = (JSONObject) postJSON(t.getAccessToken(), path, JSONObject, "object");
				logger.info("RESPONSE " + response);
				if (response.getInt("code") == 0) {
					successAmount++;
					// Boolean b = null;
					// try {
					// b = setAttachement(t.getSoftwareToken(),
					// setVerkoopFactuur.getString("Id"), w.getPdfUrl());
					// } catch (Exception e) {
					// e.printStackTrace();
					// }
					
					// System.out.println("setAttachement = " + b);
					WorkOrderHandler.setWorkorderStatus(w.getId(), w.getWorkorderNr(), true, "GetWorkorder",
							t.getSoftwareToken(), softwareName);
				}
			}
		}
		if (successAmount > 0) {
			errorMessage += successAmount + " workorders exported.<br>";
		}
		if (errorAmount > 0) {
			errorMessage += errorAmount + " out of " + exportAmount
					+ " workorders(factuur) have errors. Click for details<br>";
		}
		return new String[] { errorMessage, errorDetails };
	}
	
	public JSONObject factuurJSON(WorkOrder w, Token t, int roundedHours) throws JSONException {
		JSONArray JSONArrayMaterials = null;
		String error = "";
		
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
					SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'");
					workEndDate = dt1.format(formatDate1);
				}
				if (workDate.equals("")) {
					workDate = getCurrentDate(null);
				} else {
					Date formatDate = dt.parse(w.getWorkDate());
					SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'Z'");
					workDate = dt1.format(formatDate);
				}
				
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
			JSONArrayMaterials = new JSONArray();
			for (Material m : w.getMaterials()) {
				JSONObject materialObject = new JSONObject();
				materialObject.put("reference", m.getCode());
				materialObject.put("designation", m.getDescription());
				double price = 0;
				DecimalFormat df = new DecimalFormat("#.##");
				String formatted = df.format(m.getPrice());
				price = Double.parseDouble(formatted.toString().replaceAll(",", "."));
				materialObject.put("unitPrice", price);
				materialObject.put("quantity", Double.parseDouble(m.getQuantity()));
				materialObject.put("unitCode", m.getUnit());
				JSONArrayMaterials.put(materialObject);
				
			}
			
			for (WorkPeriod p : w.getWorkPeriods()) {
				HourType h = ObjectDAO.getHourType(t.getSoftwareToken(), p.getHourType());
				// Get ID from db(hourtype)
				if (h == null) {
					error += "Hourtype " + p.getHourType() + " not found in DriveFx or hourtype is not synchronized\n";
					return new JSONObject().put("Error", error);
				} else {
					JSONObject hourtypeObject = new JSONObject();
					
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
					DecimalFormat df1 = new DecimalFormat("#.##");
					String formatted1 = df1.format(h.getSalePrice());
					Double unitPrice = Double.parseDouble(formatted1.toString().replaceAll(",", "."));
					hourtypeObject.put("reference", h.getCode());
					hourtypeObject.put("unitPrice", unitPrice);
					hourtypeObject.put("designation", h.getName());
					hourtypeObject.put("quantity", quantity);
					hourtypeObject.put("unitCode", "Hora");
					JSONArrayMaterials.put(hourtypeObject);
				}
			}
			JSONObject.put("products", JSONArrayMaterials);
			JSONObject documentObject = new JSONObject();
			documentObject.put("docType", 1);
			documentObject.put("documentDate", getCurrentDate(null));
			// JSONObject customerObject = new JSONObject();
			for (Relation r : w.getRelations()) {
				Address a = r.getAddressess().get(0);
				if (a.getType().equals("invoice")) {
					documentObject.put("customerNumber", Integer.parseInt(r.getDebtorNumber()));
					// Relation dbRelation =
					// ObjectDAO.getRelation(t.getSoftwareToken(),
					// w.getCustomerDebtorNr(), "invoice");
					// Address aDB = dbRelation.getAddressess().get(0);
					// if(dbRelation != null){
					// customerObject.put("email", aDB.getEmail());
					// }
					// customerObject.put("number", r.getDebtorNumber());
					// customerObject.put("name", r.getCompanyName());
					// customerObject.put("morada", a.getStreet());
					// customerObject.put("codpost", a.getPostalCode());
					// customerObject.put("local", a.getCity());
					// // HARDCODED
					// customerObject.put("pais", "PT");
					// customerObject.put("ncont", "12345678");
				}
			}
			JSONObject.put("document", documentObject);
			
		} catch (JSONException | SQLException e) {
			e.printStackTrace();
		}
		if (error.equals("")) {
			return JSONObject;
		} else {
			return new JSONObject().put("Error", error);
		}
		
	}
}
