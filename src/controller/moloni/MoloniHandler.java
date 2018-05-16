package controller.moloni;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import DAO.ObjectDAO;
import controller.WorkOrderHandler;
import controller.twinfield.SoapHandler;
import object.Settings;
import object.Token;
import object.workorder.Address;
import object.workorder.Employee;
import object.workorder.HourType;
import object.workorder.Material;
import object.workorder.Relation;
import object.workorder.WorkOrder;
import object.workorder.WorkPeriod;

public class MoloniHandler {
	final String softwareName = "Moloni";
	private final static Logger logger = Logger.getLogger(SoapHandler.class.getName());
	// Change to environment variable
	protected static String host = System.getenv("MOLONI_API_HOST");
	
	public static boolean checkAccessToken(String accessToken) throws IOException {
		String link = "https://" + host + "companies/getAll/?access_token=" + accessToken;
		
		try {
			URL url = new URL(link);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			// conn.setDoInput(true);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setUseCaches(false);
			if (conn.getResponseCode() > 200 && conn.getResponseCode() < 405) {
				return false;
			}
			conn.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	public static Object getJSON(String accessToken, String path, String parameters, String returnType)
			throws IOException, URISyntaxException {
		String jsonString = null;
		Object object = null;
		byte[] postData = parameters.getBytes(StandardCharsets.UTF_8);
		int postDataLength = postData.length;
		try {
			String link = "https://" + host + path + "/?access_token=" + accessToken;
			System.out.println("GETJSON LINK " + link + "/" + parameters);
			URL url = new URL(link);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			// conn.setDoInput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
			conn.setUseCaches(false);
			try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
				wr.write(postData);
			}
			if (conn.getResponseCode() > 200 && conn.getResponseCode() < 405) {
				System.out.println("Response message " + conn.getResponseMessage());
				return null;
			}
			
			BufferedReader br = new BufferedReader(
					new InputStreamReader((conn.getInputStream()), StandardCharsets.UTF_8));
			
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
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
		return object;
	}
	
	@SuppressWarnings("unchecked")
	public static Object postJSON(String accessToken, String path, Map<String, Object> params, String returnType)
			throws IOException, URISyntaxException {
		String jsonString = null;
		Object object = null;
		StringBuilder postData = new StringBuilder();
		for (Map.Entry<String, Object> param : params.entrySet()) {
			if (postData.length() != 0) {
				postData.append('&');
			}
			StringBuilder postDataArray = new StringBuilder();
			if (param.getValue() instanceof ArrayList) {
				System.out.println("Map contains another Array");
				System.out.println("MAP " + param.getValue().toString());
				System.out.println("MAP " + param.getKey().toString());
				String key = URLEncoder.encode(param.getKey(), "UTF-8");
				ArrayList<Map<String, Object>> allParams = (ArrayList<Map<String, Object>>) param.getValue();
				for (int i = 0; i < allParams.size(); i++) {
					Map<String, Object> params1 = allParams.get(i);
					System.out.println("AllParams " + allParams.get(i).toString());
					for (Map.Entry<String, Object> param1 : params1.entrySet()) {
						if (postDataArray.length() != 0) {
							postDataArray.append('&');
						}
						if (param1.getKey().equals("taxes")) {
							postDataArray.append(key + "[" + i + "][" + URLEncoder.encode(param1.getKey(), "UTF-8")
									+ "][0][tax_id]");
						} else {
							postDataArray
									.append(key + "[" + i + "][" + URLEncoder.encode(param1.getKey(), "UTF-8") + "]");
						}
						postDataArray.append('=');
						postDataArray.append(URLEncoder.encode(String.valueOf(param1.getValue()), "UTF-8"));
					}
				}
				postData.append(postDataArray.toString());
			} else {
				postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
				postData.append('=');
				postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
			}
		}
		
		System.out.println("POSTDATE.toString " + postData.toString());
		byte[] postDataBytes = postData.toString().getBytes(StandardCharsets.UTF_8);
		try {
			String link = "https://" + host + path + "/?access_token=" + accessToken;
			
			System.out.println("POSTJSON LINK " + link);
			URL url = new URL(link);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			// conn.setDoInput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
			conn.setUseCaches(false);
			try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
				wr.write(postDataBytes);
			}
			if (conn.getResponseCode() > 200 && conn.getResponseCode() < 409) {
				System.out.println("Response message " + conn.getResponseMessage());
				return null;
			}
			
			BufferedReader br = new BufferedReader(
					new InputStreamReader((conn.getInputStream()), StandardCharsets.UTF_8));
			
			while ((jsonString = br.readLine()) != null) {
				System.out.println("JSONPOST " + jsonString);
				switch (returnType) {
				case "array":
					object = new JSONArray(jsonString);
					break;
				case "object":
					try {
						object = new JSONObject(jsonString);
					} catch (Exception e) {
						object = new JSONArray(jsonString);
					}
					break;
				}
			}
			conn.disconnect();
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
		return object;
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
	
	public String convertDate(String string) {
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Date date = null;
		try {
			// String to date
			date = format.parse(string);
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			cal.add(Calendar.DAY_OF_MONTH, 1);
			date = cal.getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		// Date to String
		Format formatter = new SimpleDateFormat("yyyy-MM-dd");
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
			cal.add(Calendar.HOUR_OF_DAY, 0);
			date = cal.getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		// Date to String
		Format formatter = new SimpleDateFormat("dd-MM-yyyy");
		String s = formatter.format(date);
		return s;
	}
	
	public static ArrayList<Map<String, String>> getOffices(Token t) throws IOException {
		ArrayList<Map<String, String>> offices = new ArrayList<Map<String, String>>();
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		
		String path = "companies/getAll";
		try {
			jsonArray = (JSONArray) getJSON(t.getAccessToken(), path, "", "array");
			for (int i = 0; i < jsonArray.length(); i++) {
				jsonObj = jsonArray.getJSONObject(i);
				String officeCode = jsonObj.getInt("company_id") + "";
				String officeName = jsonObj.getString("name");
				Map<String, String> office = new HashMap<String, String>();
				office.put("code", officeCode);
				office.put("name", officeName);
				offices.add(office);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return offices;
		
	}
	
	public String[] getEmployees(Token t, String date, String office) throws Exception {
		boolean checkUpdate = false;
		String errorMessage = "";
		int importCount = 0;
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		ArrayList<Employee> emp = new ArrayList<Employee>();
		String path = "users/getAll";
		String parameters = "company_id=" + office;
		try {
			jsonArray = (JSONArray) getJSON(t.getAccessToken(), path, parameters, "array");
			for (int i = 0; i < jsonArray.length(); i++) {
				jsonObj = jsonArray.getJSONObject(i);
				importCount++;
				String userId = jsonObj.getInt("user_id") + "";
				String[] fullName = jsonObj.getString("name").split("\\s+");
				String firstName = fullName[0];
				int j = 1;
				String lastName = "";
				while (j < fullName.length) {
					lastName += " " + fullName[j];
					j++;
				}
				if (lastName == null || lastName.equals("")) {
					lastName = firstName;
				}
				Employee e = new Employee(firstName, lastName, userId);
				emp.add(e);
			}
		} catch (Exception e) {
			e.printStackTrace();
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
	
	// Products and hourtypes
	public String[] getMaterials(Token t, String date, String companyId, int offset, int success,
			ArrayList<Material> mat) throws Exception {
		boolean checkUpdate = false;
		JSONArray jsonArray = new JSONArray();
		JSONObject materialDetails = new JSONObject();
		String path = "products/getModifiedSince";
		String parameters = "company_id=" + companyId;
		String errorMessage = "";
		ArrayList<Material> materials = new ArrayList<Material>();
		Boolean hasContent = ObjectDAO.hasContent(t.getSoftwareToken(), "materials");
		if (date != null && hasContent) {
			parameters += "&lastmodified=" + getDateMinHour(date) + "&offset=" + offset;
		} else {
			parameters += "&lastmodified=" + "01-01-1900&offset=" + offset;
		}
		jsonArray = (JSONArray) getJSON(t.getAccessToken(), path, parameters, "array");
		logger.info("Materials response " + jsonArray);
		if (jsonArray != null) {
			if (mat == null) {
				mat = new ArrayList<Material>();
			}
			for (int i = 0; i < jsonArray.length(); i++) {
				materialDetails = jsonArray.getJSONObject(i);
				String id = materialDetails.getInt("product_id") + "";
				String productCode = materialDetails.getString("reference");
				String description = materialDetails.getString("name");
				Double price = materialDetails.getDouble("price");
				JSONObject jsonObject = materialDetails.optJSONObject("measurement_unit");
				String unitName = null;
				String unitNameShort = null;
				if (jsonObject != null) {
					unitName = jsonObject.getString("name");
					unitNameShort = jsonObject.getString("short_name");
					
					// Filter unit hour from other articles
					if (!unitNameShort.equals("Hrs")) {
						Material m = new Material(productCode, null, unitName, description, price, null, null, id);
						materials.add(m);
						mat.add(m);
					}
				}
			}
			
		}
		// Materials log message
		if (!materials.isEmpty()) {
			int successAmount = (int) WorkOrderHandler.addData(t.getSoftwareToken(), materials, "materials",
					softwareName);
			if (materials.size() == 50) {
				if (successAmount > 0) {
					success += successAmount;
				}
				return getMaterials(t, date, companyId, offset += 50, success, mat);
			} else {
				if (successAmount > 0) {
					success += successAmount;
					ObjectDAO.saveMaterials(mat, t.getSoftwareToken());
					errorMessage += success + " materials imported<br>";
					checkUpdate = true;
				} else {
					errorMessage += "Something went wrong with materials<br>";
				}
			}
		} else {
			if (success > 0) {
				ObjectDAO.saveMaterials(mat, t.getSoftwareToken());
				errorMessage += success + " materials imported<br>";
				checkUpdate = true;
			} else {
				errorMessage += "No materials for import<br>";
			}
		}
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	// Products and hourtypes
	public String[] getHourtypes(Token t, String date, String companyId, int offset, int success,
			ArrayList<HourType> hour) throws Exception {
		boolean checkUpdate = false;
		JSONArray jsonArray = new JSONArray();
		JSONObject materialDetails = new JSONObject();
		String path = "products/getModifiedSince";
		String parameters = "company_id=" + companyId;
		String errorMessage = "";
		ArrayList<HourType> hourtypes = new ArrayList<HourType>();
		Boolean hasContent = ObjectDAO.hasContent(t.getSoftwareToken(), "hourtypes");
		if (date != null && hasContent) {
			parameters += "&lastmodified=" + getDateMinHour(date) + "&offset=" + offset;
		} else {
			parameters += "&lastmodified=" + "01-01-1900&offset=" + offset;
		}
		jsonArray = (JSONArray) getJSON(t.getAccessToken(), path, parameters, "array");
		logger.info("Hourtype response " + jsonArray);
		if (jsonArray != null) {
			if (hour == null) {
				hour = new ArrayList<HourType>();
			}
			for (int i = 0; i < jsonArray.length(); i++) {
				materialDetails = jsonArray.getJSONObject(i);
				String id = materialDetails.getInt("product_id") + "";
				String productCode = materialDetails.getString("reference");
				String description = materialDetails.getString("name");
				Double price = materialDetails.getDouble("price");
				JSONObject jsonObject = materialDetails.optJSONObject("measurement_unit");
				String unitName = null;
				String unitNameShort = null;
				if (jsonObject != null) {
					unitName = jsonObject.getString("name");
					unitNameShort = jsonObject.getString("short_name");
					
					// Filter unit hour from other articles
					if (unitNameShort.equals("Hrs")) {
						HourType h = new HourType(productCode, description, 0, 1, 0, price, 1, null, id);
						hourtypes.add(h);
						hour.add(h);
					}
				}
			}
		}
		// Hourtypes log message
		if (!hourtypes.isEmpty()) {
			int successAmount = (int) WorkOrderHandler.addData(t.getSoftwareToken(), hourtypes, "hourtypes",
					softwareName);
			if (hourtypes.size() == 50) {
				if (successAmount > 0) {
					success += successAmount;
				}
				return getHourtypes(t, date, companyId, offset += 50, success, hour);
			} else {
				if (successAmount > 0) {
					success += successAmount;
					ObjectDAO.saveHourTypes(hour, t.getSoftwareToken());
					errorMessage += success + " hourtypes imported<br>";
					checkUpdate = true;
				} else {
					errorMessage += "Something went wrong with hourtypes<br>";
				}
			}
		} else {
			if (jsonArray.length() == 50) {
				return getHourtypes(t, date, companyId, offset += 50, success, hour);
			} else {
				if (success > 0) {
					ObjectDAO.saveHourTypes(hour, t.getSoftwareToken());
					errorMessage += success + " hourtypes imported<br>";
					checkUpdate = true;
				} else {
					errorMessage += "No hourtypes for import<br>";
				}
			}
			
		}
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	// Debiteuren
	public String[] getRelations(Token t, String date, String companyId) throws Exception {
		boolean checkUpdate = false;
		JSONArray jsonArray = new JSONArray();
		JSONObject debtorDetails = new JSONObject();
		String path = "customers/getModifiedSince";
		String parameters = "company_id=" + companyId;
		String errorMessage = "";
		int importCount = 0;
		ArrayList<Relation> relations = new ArrayList<Relation>();
		Boolean hasContent = ObjectDAO.hasContent(t.getSoftwareToken(), "relations");
		if (date != null && hasContent) {
			parameters += "&lastmodified=" + getDateMinHour(date);
		} else {
			parameters += "&lastmodified=" + "01-01-1900";
		}
		jsonArray = (JSONArray) getJSON(t.getAccessToken(), path, parameters, "array");
		logger.info("Relation responseArray " + jsonArray);
		if (jsonArray != null) {
			for (int i = 0; i < jsonArray.length(); i++) {
				importCount++;
				debtorDetails = jsonArray.getJSONObject(i);
				ArrayList<Address> address = new ArrayList<Address>();
				String debtorNr = debtorDetails.optString("number", "");
				String id = debtorDetails.getInt("customer_id") + "";
				
				// Invoice
				String firstName = debtorDetails.optString("contact_name", "<empty>");
				String companyName = debtorDetails.optString("name", "<empty>");
				String phoneNr = debtorDetails.optString("phone", "");
				String email = debtorDetails.optString("email", "");
				String contactEmail = debtorDetails.optString("contact_email", "");
				String street = debtorDetails.optString("address", "<empty>");
				String postalCode = debtorDetails.optString("zip_code", "<empty>");
				String city = debtorDetails.optString("city", "<empty>");
				String remark = debtorDetails.optString("notes", "");
				Address invoice = new Address(firstName, phoneNr, contactEmail, street, "", postalCode, city, remark,
						"invoice", 2);
				address.add(invoice);
				
				JSONObject alternateAddress = debtorDetails.optJSONObject("alternate_addresses");
				if (alternateAddress != null) {
					// Postal
					String postalName = debtorDetails.optString("contact_name", "<empty>");
					String postalPhone = debtorDetails.optString("phone", "");
					String postalEmail = debtorDetails.optString("email", "");
					String postalStreet = debtorDetails.optString("address", "<empty>");
					String postalCode2 = debtorDetails.optString("zip_code", "<empty>");
					String postalCity = debtorDetails.optString("city", "<empty>");
					String postalRemark = debtorDetails.optString("notes", "");
					Address postal = new Address(postalName, postalPhone, postalEmail, postalStreet, "", postalCode2,
							postalCity, postalRemark, "postal", 1);
					address.add(postal);
				}
				Relation r = new Relation(companyName, debtorNr, firstName, email, address, "", id);
				relations.add(r);
			}
		}
		if (!relations.isEmpty()) {
			int successAmount = (int) WorkOrderHandler.addData(t.getSoftwareToken(), relations, "relations",
					softwareName);
			if (successAmount > 0) {
				ObjectDAO.saveRelations(relations, t.getSoftwareToken());
				errorMessage += importCount + " relations imported<br>";
				checkUpdate = true;
			} else {
				errorMessage += "Something went wrong with relations<br>";
			}
		} else {
			errorMessage += "No relations for import<br>";
		}
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	// Create verkoopfactuur
	@SuppressWarnings("unchecked")
	public String[] setFactuur(Token t, Settings set, String date)
			throws JSONException, IOException, URISyntaxException {
		// Get WorkOrders
		String errorMessage = "", errorDetails = "";
		int exportAmount = 0;
		int successAmount = 0;
		int errorAmount = 0;
		ArrayList<WorkOrder> allData = WorkOrderHandler.getData(t.getSoftwareToken(), "GetWorkorders",
				set.getFactuurType(), false, softwareName);
		for (WorkOrder w : allData) {
			exportAmount++;
			// Create parameters
			Object object = factuurParameters(w, t, set);
			Map<String, Object> parameters = null;
			if (object instanceof Map<?, ?>) {
				parameters = (Map<String, Object>) object;
				
				logger.info("REQUEST " + parameters.toString());
				String path = "invoices/insert";
				// Post the request
				Object obj = postJSON(t.getAccessToken(), path, parameters, "object");
				JSONObject response = null;
				JSONArray response1 = null;
				try {
					response = (JSONObject) obj;
				} catch (Exception e) {
					response1 = (JSONArray) obj;
				}
				
				logger.info("RESPONSE " + response);
				if (response != null && response.getInt("valid") == 1) {
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
					
				} else {
					errorDetails += response1 + " (workorder " + w.getWorkorderNr() + ")\n";
					errorAmount++;
				}
			} else {
				errorDetails += object + " (workorder " + w.getWorkorderNr() + ")\n";
				errorAmount++;
			}
		}
		if (successAmount > 0) {
			errorMessage += successAmount + " workorders exported.<br>";
		}
		if (errorAmount > 0) {
			errorMessage += errorAmount + " out of " + exportAmount
					+ " workorders(invoice) have errors. Click for details<br>";
		}
		return new String[] { errorMessage, errorDetails };
	}
	
	public Object factuurParameters(WorkOrder w, Token t, Settings set) throws JSONException {
		ArrayList<Map<String, Object>> allParams = new ArrayList<Map<String, Object>>();
		Map<String, Object> params = new LinkedHashMap<>();
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
					SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd");
					workEndDate = dt1.format(formatDate1);
				}
				if (workDate.equals("")) {
					workDate = getCurrentDate(null);
				} else {
					Date formatDate = dt.parse(w.getWorkDate());
					SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd");
					workDate = dt1.format(formatDate);
				}
				
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
			params.put("company_id", set.getImportOffice());
			params.put("date", workEndDate);
			params.put("expiration_date", convertDate(workEndDate));
			JSONArray responseArray = (JSONArray) getJSON(t.getAccessToken(), "documentSets/getAll",
					"company_id=" + set.getImportOffice(), "array");
			JSONObject documentJSON = responseArray.getJSONObject(0);
			String documentId = documentJSON.getInt("document_set_id") + "";
			params.put("document_set_id", documentId);
			params.put("your_reference", w.getWorkorderNr());
			params.put("notes", w.getWorkDescription());
			Relation dbRelation = ObjectDAO.getRelation(t.getSoftwareToken(), w.getCustomerDebtorNr(), "invoice");
			if (dbRelation != null) {
				params.put("customer_id", dbRelation.getId());
			} else {
				return "Relation " + w.getCustomerDebtorNr() + " not found in Moloni or relation is not synchronized\n";
			}
			
			Material dbMaterial = null;
			for (Material m : w.getMaterials()) {
				Map<String, Object> materials = new LinkedHashMap<>();
				dbMaterial = ObjectDAO.getMaterial(t.getSoftwareToken(), m.getCode());
				if (Double.parseDouble(m.getQuantity()) == 0) {
					return "Material/hourtype " + m.getCode() + " has a quantity of 0\n";
				}
				if (dbMaterial != null) {
					String path = "products/getOne";
					String parameters = "company_id=" + set.getImportOffice() + "&product_id=" + dbMaterial.getId();
					JSONObject response = (JSONObject) getJSON(t.getAccessToken(), path, parameters, "object");
					
					materials.put("product_id", dbMaterial.getId());
					materials.put("name", m.getDescription());
					materials.put("qty", m.getQuantity());
					
					materials.put("price", m.getPrice());
					if (response != null && response.getJSONArray("taxes").length() > 0) {
						materials.put("taxes", response.getJSONArray("taxes").getJSONObject(0).getInt("tax_id"));
					} else {
						materials.put("exemption_reason", "taxes unknown");
					}
					allParams.add(materials);
				} else {
					return "Material " + m.getCode() + " not found in Moloni or material is not synchronized\n";
				}
				
			}
			for (WorkPeriod p : w.getWorkPeriods()) {
				Map<String, Object> materials = new LinkedHashMap<>();
				HourType h = ObjectDAO.getHourType(t.getSoftwareToken(), p.getHourType());
				if (h != null) {
					String path = "products/getOne";
					String parameters = "company_id=" + set.getImportOffice() + "&product_id=" + h.getId();
					JSONObject response = (JSONObject) getJSON(t.getAccessToken(), path, parameters, "object");
					// Get ID from db(hourtype)
					
					double number = p.getDuration();
					double hours = set.getRoundedHours();
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
					materials.put("product_id", h.getId());
					materials.put("name", h.getName());
					materials.put("summary", p.getDescription());
					materials.put("qty", quantity);
					materials.put("price", unitPrice);
					if (response != null) {
						materials.put("taxes", response.getJSONArray("taxes").getJSONObject(0).getInt("tax_id"));
					} else {
						materials.put("exemption_reason", "Taxes unknown");
					}
					allParams.add(materials);
				} else {
					return "Hourtype " + p.getHourType() + " not found in Moloni or hourtype is not synchronized\n";
				}
			}
			params.put("products", allParams);
			
		} catch (SQLException | IOException |
				
				URISyntaxException e) {
			e.printStackTrace();
		}
		return params;
	}
}
