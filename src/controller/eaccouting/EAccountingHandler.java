package controller.eaccouting;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import DAO.ObjectDAO;
import controller.WorkOrderHandler;
import controller.twinfield.SoapHandler;
import object.Settings;
import object.Token;
import object.workorder.Address;
import object.workorder.HourType;
import object.workorder.Material;
import object.workorder.Project;
import object.workorder.Relation;
import object.workorder.WorkOrder;
import object.workorder.WorkPeriod;

public class EAccountingHandler {
	final String softwareName = "EAccounting";
	private final static Logger logger = Logger.getLogger(SoapHandler.class.getName());
	// Change to environment variable
	protected static String host = System.getenv("EACCOUNTING_API_HOST");
	
	public boolean checkAccessToken(String accessToken) throws IOException {
		String link = "https://" + host + "/v1/articles";
		
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
			if (conn.getResponseCode() > 200 && conn.getResponseCode() < 405) {
				return false;
			}
			conn.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	public static Object getJSON(String accessToken, String parameters, String path, String method)
			throws IOException, URISyntaxException {
		String jsonString = null;
		Object object = null;
		try {
			URI uri = new URI("https", host, path, parameters, null);
			String request = uri.toASCIIString();
			URL url = new URL(request);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			// conn.setDoInput(true);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Authorization", "Bearer " + accessToken);
			conn.setUseCaches(false);
			if (conn.getResponseCode() > 200 && conn.getResponseCode() < 405) {
				System.out.println("Response message " + conn.getResponseMessage());
				return null;
			}
			BufferedReader br = new BufferedReader(
					new InputStreamReader((conn.getInputStream()), StandardCharsets.UTF_8));
			// if (conn.getResponseCode() > 201 && conn.getResponseCode() < 405)
			// {
			// System.out.println("RESPONSEMESSAGE " +
			// conn.getResponseMessage());
			// return null;
			// }
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
	
	public static Object putJSON(String accessToken, JSONObject json, String path, String method,
			String requestMethod) {
		String jsonString = null;
		Object object = null;
		String jsonRequestString = json + "";
		byte[] postData = jsonRequestString.getBytes(StandardCharsets.UTF_8);
		
		int postDataLength = postData.length;
		try {
			URI uri = new URI("https", host, path, "", null);
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
			conn.setUseCaches(false);
			try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
				wr.write(postData);
			}
			
			if (conn.getResponseCode() > 201 && conn.getResponseCode() <= 409) {
				System.out.println("RESPONSEMESSAGE " + conn.getResponseMessage());
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
		} catch (IOException | JSONException | URISyntaxException e) {
			e.printStackTrace();
		}
		return object;
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
			cal.add(Calendar.HOUR_OF_DAY, 2);
			date = cal.getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		// Date to String
		Format formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
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
	
	public static JSONObject setUniversalMaterial(Token t, String materialCode) {
		JSONObject setMaterialResponse = null;
		// Get BTW id default is 21%
		String path = "/v1/articleaccountcodings";
		JSONArray codingsArray, unitArray;
		JSONObject accCodings, unitObject;
		String btwId = "";
		String unitId = "";
		try {
			codingsArray = (JSONArray) getJSON(t.getAccessToken(), "", path, "array");
			System.out.println("CodingsArray " + codingsArray);
			for (int i = 0; i < codingsArray.length(); i++) {
				accCodings = codingsArray.getJSONObject(i);
				String type = accCodings.getString("Type");
				String vat = accCodings.getString("VatRate");
				if (type.equals("Goods") && vat.equals("21%")) {
					logger.info("BTW RESPONSE " + accCodings);
					btwId = accCodings.getString("Id");
				}
			}
			// Get Unit id default is Stuk
			path = "/v1/units";
			unitArray = (JSONArray) getJSON(t.getAccessToken(), "", path, "array");
			for (int i = 0; i < unitArray.length(); i++) {
				unitObject = unitArray.getJSONObject(i);
				String code = unitObject.getString("Code");
				
				// Stuk
				if (code.equals("PCE")) {
					logger.info("UNITID RESPONSE " + unitObject);
					unitId = unitObject.getString("Id");
				}
			}
			
			JSONObject JSONObject = new JSONObject();
			JSONObject.put("IsActive", true);
			JSONObject.put("Number", materialCode);
			JSONObject.put("Name", "Material onbekend");
			JSONObject.put("CodingId", btwId);
			JSONObject.put("UnitId", unitId);
			path = "/v1/articles";
			logger.info("setMaterialRequest " + JSONObject);
			setMaterialResponse = (JSONObject) putJSON(t.getAccessToken(), JSONObject, path, "object", "POST");
			logger.info("setMaterialResponse " + setMaterialResponse);
		} catch (IOException | URISyntaxException | JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return setMaterialResponse;
	}
	
	// Producten
	// This method also getHourtypes if unit is uur
	public String[] getMaterials(Token t, String date) throws Exception {
		boolean checkUpdate = false;
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		String path = "/v1/articles";
		String parameters = "showPricesWithTwoDecimals=true";
		String errorMessage = "";
		int importCount = 0;
		int importCount2 = 0;
		int editCount = 0;
		int editCount2 = 0;
		ArrayList<Material> materials = new ArrayList<Material>();
		ArrayList<HourType> hourtypes = new ArrayList<HourType>();
		Boolean hasContent = ObjectDAO.hasContent(t.getSoftwareToken(), "materials");
		if (date != null && hasContent) {
			// yyyy-MM-dd hh:mm:ss
			parameters += "&changedFromDate=" + getDateMinHour(date);
		}
		
		jsonArray = (JSONArray) getJSON(t.getAccessToken(), parameters, path, "array");
		logger.info("Materials response JSONArray " + jsonArray);
		if (jsonArray != null) {
			for (int i = 0; i < jsonArray.length(); i++) {
				jsonObj = jsonArray.getJSONObject(i);
				String id = jsonObj.getString("Id");
				String modified = jsonObj.getString("ChangedUtc");
				String productCode = jsonObj.getString("Number");
				String description = jsonObj.getString("Name");
				Double price = jsonObj.getDouble("NetPrice");
				String unit = jsonObj.getString("UnitName");
				
				String dbModified = ObjectDAO.getModifiedDate(t.getSoftwareToken(), null, productCode, "materials");
				String dbModifiedHourtype = ObjectDAO.getModifiedDate(t.getSoftwareToken(), null, productCode,
						"hourtypes");
				// Check if data is modified
				if (unit.equals("Uur")) {
					if (dbModifiedHourtype == null || date == null) {
						importCount2++;
					} else {
						editCount2++;
					}
					HourType h = new HourType(productCode, description, 0, 1, 0, price, 1, modified, id);
					hourtypes.add(h);
				} else {
					if (dbModified == null || date == null) {
						importCount++;
					} else {
						editCount++;
					}
					Material m = new Material(productCode, null, unit, description, price, null, modified, id);
					materials.add(m);
				}
			}
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
	
	// Debiteuren
	public String[] getRelations(Token t, String date) throws Exception {
		boolean checkUpdate = false;
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		String path = "/v1/customerlistitems";
		String parameters = "";
		String errorMessage = "";
		int importCount = 0;
		int editCount = 0;
		ArrayList<Relation> relations = new ArrayList<Relation>();
		Boolean hasContent = ObjectDAO.hasContent(t.getSoftwareToken(), "relations");
		if (date != null && hasContent) {
			parameters += "changedFromDate=" + getDateMinHour(date);
		}
		jsonArray = (JSONArray) getJSON(t.getAccessToken(), parameters, path, "array");
		logger.info("Relation response JSONArray " + jsonArray);
		if (jsonArray != null) {
			for (int i = 0; i < jsonArray.length(); i++) {
				jsonObj = jsonArray.getJSONObject(i);
				String id = jsonObj.getString("Id");
				path = "/v1/customers/" + id;
				parameters = "";
				JSONObject debtorDetails = (JSONObject) getJSON(t.getAccessToken(), parameters, path, "object");
				logger.info("Relation response debtorDetails " + debtorDetails);
				ArrayList<Address> address = new ArrayList<Address>();
				String modified = debtorDetails.optString("ChangedUtc", "");
				String debtorNr = debtorDetails.optString("CustomerNumber", "");
				String dbModified = ObjectDAO.getModifiedDate(t.getSoftwareToken(), "invoice", debtorNr, "relations");
				// Check if data is modified
				if (dbModified == null || date == null) {
					importCount++;
				} else {
					editCount++;
				}
				// Postal
				String firstName = debtorDetails.optString("ContactPersonName", "<leeg>");
				String companyName = debtorDetails.optString("DeliveryCustomerName", "<leeg>");
				String contact = firstName;
				String mobileNr = debtorDetails.optString("MobilePhone", "");
				String phoneNr = debtorDetails.optString("Telephone", "");
				if (phoneNr.equals("")) {
					phoneNr = mobileNr;
				}
				String email = debtorDetails.optString("EmailAddress", "");
				String street = debtorDetails.optString("DeliveryAddress1", "<leeg>");
				String postalCode = debtorDetails.optString("DeliveryPostalCode", "<leeg>");
				String city = debtorDetails.optString("DeliveryCity", "<leeg>");
				String remark = debtorDetails.optString("Note");
				// Postal address is not used!!
				Address postal = new Address(contact, phoneNr, email, street, "", postalCode, city, remark, "postal",
						2);
				// address.add(postal);
				// Invoice
				String invoiceFirstName = debtorDetails.optString("ContactPersonName");
				String invoiceCompanyName = debtorDetails.optString("Name", "<leeg>");
				if (!invoiceCompanyName.equals("<leeg>")) {
					companyName = invoiceCompanyName;
				}
				String invoiceContact = invoiceFirstName;
				String invoiceEmail = debtorDetails.optString("EmailAddress");
				String contactEmail = debtorDetails.optString("ContactPersonEmail");
				String invoicestreet = debtorDetails.optString("InvoiceAddress1", "<leeg>");
				String invoicepostalCode = debtorDetails.optString("InvoicePostalCode", "<leeg>");
				String invoicecity = debtorDetails.optString("InvoiceCity", "<leeg>");
				if (!invoicecity.equals("<leeg>")) {
					Address invoice = new Address(invoiceContact, phoneNr, contactEmail, invoicestreet, "",
							invoicepostalCode, invoicecity, remark, "invoice", 1);
					address.add(invoice);
				}
				Relation r = new Relation(companyName, debtorNr, contact, invoiceEmail, address, modified, id);
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
	
	// Verkooporders
	public String[] getOrders(Token t, String date, Settings set) throws Exception {
		boolean checkUpdate = false;
		WorkOrder w = null;
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		String path = "/v1/orderlistitems";
		String parameters = "";
		String errorMessage = "";
		int importCount = 0;
		
		ArrayList<WorkOrder> workorders = new ArrayList<WorkOrder>();
		JSONObject orderDetails = null;
		jsonArray = (JSONArray) getJSON(t.getAccessToken(), parameters, path, "array");
		logger.info("Order response JSONArray " + jsonArray);
		if (jsonArray != null) {
			for (int i = 0; i < jsonArray.length(); i++) {
				ArrayList<Relation> relations = new ArrayList<Relation>();
				ArrayList<Address> address = new ArrayList<Address>();
				jsonObj = jsonArray.getJSONObject(i);
				int status = jsonObj.getInt("Status");
				// Status is verzonden
				if (status == 2) {
					String id = jsonObj.getString("Id");
					path = "/v1/orders/" + id;
					parameters = "";
					// OrderDetails
					orderDetails = (JSONObject) getJSON(t.getAccessToken(), parameters, path, "object");
					logger.info("orderDetails response " + orderDetails);
					
					String customerId = orderDetails.getString("CustomerId");
					path = "/v1/customers/" + customerId;
					// Debiteur
					JSONObject debtorDetails = (JSONObject) getJSON(t.getAccessToken(), parameters, path, "object");
					logger.info("orderDetails - debtorDetails response " + orderDetails);
					// Get relations
					// Invoice
					String invoiceCompanyName = orderDetails.optString("InvoiceCustomerName", "<leeg>");
					String invoiceDebtorNr = debtorDetails.optInt("CustomerNumber") + "";
					String invoiceEmail = debtorDetails.optString("EmailAddress", "");
					String invoicestreet = debtorDetails.optString("InvoiceAddress1", "<leeg>");
					String invoicepostalCode = debtorDetails.optString("InvoicePostalCode", "<leeg>");
					String invoicecity = debtorDetails.optString("InvoiceCity", "<leeg>");
					String mobileNr = debtorDetails.optString("MobilePhone", "");
					String phoneNr = debtorDetails.optString("Telephone", "");
					if (phoneNr.equals("")) {
						phoneNr = mobileNr;
					}
					String remark = debtorDetails.optString("Note", "");
					if (!invoicecity.equals("<leeg>")) {
						Address invoice = new Address(invoiceCompanyName, phoneNr, invoiceEmail, invoicestreet, "",
								invoicepostalCode, invoicecity, remark, "invoice", 1);
						address.add(invoice);
					}
					// Postal
					String companyName = orderDetails.optString("DeliveryCustomerName", invoiceCompanyName);
					String email = debtorDetails.optString("EmailAddress", invoiceEmail);
					String street = debtorDetails.optString("DeliveryAddress1", invoicestreet);
					String postalCode = debtorDetails.optString("DeliveryPostalCode", invoicepostalCode);
					String city = debtorDetails.optString("DeliveryCity", invoicecity);
					
					if (!invoicecity.equals("<leeg>")) {
						Address postal = new Address(companyName, phoneNr, invoiceEmail, street, "", postalCode, city,
								remark, "postal", 2);
						address.add(postal);
					}
					String contact = orderDetails.optString("YourReference", "");
					Relation r = new Relation(invoiceCompanyName, invoiceDebtorNr, contact, invoiceEmail, address, null,
							null);
					relations.add(r);
					
					// Materials
					ArrayList<Material> materials = new ArrayList<Material>();
					Material m = null;
					JSONArray rows = orderDetails.getJSONArray("Rows");
					String workDescription = null;
					String projectNr = null;
					for (int j = 0; j < rows.length(); j++) {
						JSONObject rowDetails = rows.getJSONObject(j);
						boolean isTextRow = rowDetails.getBoolean("IsTextRow");
						if (!isTextRow) {
							String projectId = rowDetails.optString("ProjectId", "");
							Project dbProject = ObjectDAO.getProjectById(t.getSoftwareToken(), projectId);
							
							if (dbProject != null) {
								projectNr = dbProject.getCode();
							}
							String productId = rowDetails.getString("ArticleId");
							path = "/v1/articles/" + productId;
							JSONObject materialDetails = (JSONObject) getJSON(t.getAccessToken(), parameters, path,
									"object");
							logger.info("orderDetails - materialDetails response " + materialDetails);
							String productCode = materialDetails.optString("Number", "<leeg>");
							String unit = materialDetails.optString("UnitName");
							String description = materialDetails.optString("Name");
							Double price = materialDetails.getDouble("NetPrice");
							String quantity = rowDetails.getInt("Quantity") + "";
							m = new Material(productCode, null, unit, description, price, quantity, null, null);
							materials.add(m);
						} else {
							workDescription += rowDetails.getString("Text");
						}
					}
					
					String workDate = orderDetails.getString("OrderDate");
					String externProjectNr = orderDetails.optInt("Number") + "";
					String typeofwork = set.getImportOffice();
					String paymentMethod = set.getExportOffice();
					w = new WorkOrder(projectNr, convertDate(workDate, "dd-MM-yyyy"), email, email, invoiceDebtorNr,
							status + "", paymentMethod, materials, workDate, null, id, null, relations, null, null,
							null, externProjectNr, typeofwork, workDescription, null, null, null, null);
					
					importCount++;
					workorders.add(w);
				}
			}
		}
		if (!workorders.isEmpty()) {
			JSONArray responseArray = (JSONArray) WorkOrderHandler.addData(t.getSoftwareToken(), workorders,
					"PostWorkorders", softwareName);
			for (int i = 0; i < responseArray.length(); i++) {
				JSONObject object = responseArray.getJSONObject(i);
				String id = object.optString("workorder_no", "");
				setOrderStatus(t, id, true);
			}
			int successAmount = responseArray.length();
			
			if (successAmount > 0) {
				// ObjectDAO.saveRelations(relations, t.getSoftwareToken());
				errorMessage += importCount + " verkooporders imported<br>";
				checkUpdate = true;
			} else {
				errorMessage += "Something went wrong with verkooporders<br>";
			}
		} else {
			errorMessage += "No verkooporders for import<br>";
		}
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	// Projects
	public String[] getProjects(Token t, String date) throws Exception {
		boolean checkUpdate = false;
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		String path = "/v1/projects";
		String parameters = "includeFinished=false";
		String errorMessage = "";
		int importCount = 0;
		int editCount = 0;
		ArrayList<Project> projects = new ArrayList<Project>();
		Boolean hasContent = ObjectDAO.hasContent(t.getSoftwareToken(), "projects");
		if (date != null && hasContent) {
			parameters += "&changedFromDate=" + getDateMinHour(date);
		}
		
		jsonArray = (JSONArray) getJSON(t.getAccessToken(), parameters, path, "array");
		logger.info("Projects response JSONArray " + jsonArray);
		if (jsonArray != null) {
			for (int i = 0; i < jsonArray.length(); i++) {
				jsonObj = jsonArray.getJSONObject(i);
				String id = jsonObj.getString("Id");
				String number = jsonObj.optString("Number");
				String name = jsonObj.getString("Name");
				String startDate = jsonObj.getString("StartDate");
				String endDate = jsonObj.optString("EndDate", "");
				// String modified = jsonObj.getString("ModifiedUtc");
				String description = jsonObj.getString("Notes");
				// String status = jsonObj.getInt("Status") + "";
				String debtorId = jsonObj.optString("CustomerId", "<leeg>");
				String debtorNr = "<leeg>";
				if (!debtorId.equals("<leeg>")) {
					path = "/v1/customers/" + debtorId;
					parameters = "";
					JSONObject debtorDetails = (JSONObject) getJSON(t.getAccessToken(), parameters, path, "object");
					logger.info("Projects response debtorDetails " + debtorDetails);
					debtorNr = debtorDetails.getString("CustomerNumber");
				}
				if (!endDate.equals("")) {
					endDate = convertDate(endDate, "yyyyMMdd");
				}
				Project p = new Project(number, id, debtorNr, "Lopend", name, convertDate(startDate, "yyyyMMdd"),
						endDate, description, 0, 1, null);
				projects.add(p);
				importCount++;
			}
		}
		if (!projects.isEmpty()) {
			int successAmount = (int) WorkOrderHandler.addData(t.getSoftwareToken(), projects, "projects",
					softwareName);
			if (successAmount > 0) {
				ObjectDAO.saveProjects(projects, t.getSoftwareToken());
				errorMessage += importCount + " projects imported<br>";
				errorMessage += "and " + editCount + " projects edited<br>";
				checkUpdate = true;
			} else {
				errorMessage += "Something went wrong with projects<br>";
			}
		} else {
			errorMessage += "No projects for import<br>";
		}
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	// Set verkooporder status after sending it to WerkbonApp
	public void setOrderStatus(Token t, String id, Boolean accepted)
			throws JSONException, IOException, URISyntaxException {
		String path = "/v1/orders/" + id;
		String parameters = "";
		// OrderDetails
		JSONObject orderDetails = (JSONObject) getJSON(t.getAccessToken(), parameters, path, "object");
		logger.info("orderDetails response " + orderDetails);
		
		// int status = orderDetails.getInt("Status");
		Double amount = orderDetails.getDouble("Amount");
		String customerId = orderDetails.getString("CustomerId");
		double vatAmount = orderDetails.getDouble("VatAmount");
		int roundingsAmount = orderDetails.getInt("RoundingsAmount");
		boolean euThirdParty = orderDetails.getBoolean("EuThirdParty");
		boolean customerIsPrivatePerson = orderDetails.getBoolean("CustomerIsPrivatePerson");
		String orderDate = orderDetails.getString("OrderDate");
		boolean reverseChargeOnConstructionServices = orderDetails.getBoolean("ReverseChargeOnConstructionServices");
		String currencyCode = orderDetails.getString("CurrencyCode");
		String invoiceCity = orderDetails.getString("InvoiceCity");
		String invoiceCountryCode = orderDetails.getString("InvoiceCountryCode");
		String invoiceCustomerName = orderDetails.getString("InvoiceCustomerName");
		String invoicePostalCode = orderDetails.getString("InvoicePostalCode");
		int rotReducedInvoicingType = orderDetails.getInt("RotReducedInvoicingType");
		JSONArray rows = orderDetails.getJSONArray("Rows");
		ArrayList<Material> materials = new ArrayList<Material>();
		for (int j = 0; j < rows.length(); j++) {
			JSONObject rowDetails = rows.getJSONObject(j);
			
			// Abuse material object to set data
			String productId = rowDetails.optString("ArticleId", "null");
			String isWorkCost = rowDetails.getBoolean("IsWorkCost") + "";
			String isVatFree = rowDetails.getBoolean("IsVatFree") + "";
			String text = rowDetails.getString("Text");
			String isTextRow = rowDetails.getBoolean("IsTextRow") + "";
			Double quantity = rowDetails.getDouble("Quantity");
			Double price = rowDetails.getDouble("UnitPrice");
			Material m = new Material(productId, isWorkCost, isVatFree, text, price, quantity + "", isTextRow, null);
			materials.add(m);
			
		}
		JSONObject JSONObject = new JSONObject();
		try {
			// Set status to 3
			JSONObject.put("Status", 3);
			JSONObject.put("Amount", amount);
			JSONObject.put("CustomerId", customerId);
			JSONObject.put("VatAmount", vatAmount);
			JSONObject.put("RoundingsAmount", roundingsAmount);
			JSONObject.put("EuThirdParty", euThirdParty);
			JSONObject.put("CustomerIsPrivatePerson", customerIsPrivatePerson);
			JSONObject.put("OrderDate", orderDate);
			JSONObject.put("ReverseChargeOnConstructionServices", reverseChargeOnConstructionServices);
			JSONObject.put("CurrencyCode", currencyCode);
			JSONObject.put("InvoiceCity", invoiceCity);
			JSONObject.put("InvoiceCountryCode", invoiceCountryCode);
			JSONObject.put("InvoiceCustomerName", invoiceCustomerName);
			JSONObject.put("InvoicePostalCode", invoicePostalCode);
			JSONObject.put("RotReducedInvoicingType", rotReducedInvoicingType);
			JSONObject.put("OrderDate", orderDate);
			JSONObject.put("ShippedDateTime", getCurrentDate(null));
			JSONArray jsonArray = new JSONArray();
			int lineNumber = 0;
			for (Material m : materials) {
				JSONObject json = new JSONObject();
				json.put("ArticleId", m.getCode());
				json.put("LineNumber", lineNumber);
				json.put("IsWorkCost", Boolean.parseBoolean(m.getSubCode()));
				json.put("IsVatFree", Boolean.parseBoolean(m.getUnit()));
				json.put("IsTextRow", Boolean.parseBoolean(m.getModified()));
				json.put("Text", m.getDescription());
				json.put("DeliveredQuantity", m.getQuantity());
				json.put("UnitPrice", m.getPrice());
				json.put("Quantity", m.getQuantity());
				jsonArray.put(json);
				lineNumber++;
			}
			JSONObject.put("Rows", jsonArray);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		JSONObject setOrderResponse = (JSONObject) putJSON(t.getAccessToken(), JSONObject, path, "object", "PUT");
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
			exportAmount++;
			Boolean b = false;
			JSONObject = factuurJSON(w, t, set.getRoundedHours());
			String error = (String) JSONObject.opt("Error");
			if (error != null) {
				errorDetails += error;
				errorAmount++;
			} else {
				logger.info("REQUEST " + JSONObject);
				JSONObject response = null;
				String path = null;
				if (!b) {
					path = "/v1/customerinvoicedrafts";
				} else {
					path = "/v1/vouchers";
				}
				response = (JSONObject) putJSON(t.getAccessToken(), JSONObject, path, "object", "POST");
				logger.info("RESPONSE " + response);
				if (response.optString("Id") != null) {
					successAmount++;
					
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
					SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
					workEndDate = dt1.format(formatDate1);
				}
				if (workDate.equals("")) {
					workDate = getCurrentDate(null);
				} else {
					Date formatDate = dt.parse(w.getWorkDate());
					SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
					workDate = dt1.format(formatDate);
				}
				
			} catch (ParseException e) {
				e.printStackTrace();
			}
			JSONObject.put("EuThirdParty", false);
			JSONObject.put("CurrencyCode", "EUR");
			
			JSONObject.put("CustomerIsPrivatePerson", false);
			JSONObject.put("CustomerEmail", w.getCustomerEmail());
			
			JSONArrayMaterials = new JSONArray();
			int lineNumber = 0;
			for (Material m : w.getMaterials()) {
				JSONObject json = new JSONObject();
				// Get ID from db(material)
				Settings set = ObjectDAO.getSettings(t.getSoftwareToken());
				Material dbMaterial = ObjectDAO.getMaterial(t.getSoftwareToken(), m.getCode());
				if (dbMaterial == null) {
					Material dbUniversalMaterial = ObjectDAO.getMaterial(t.getSoftwareToken(), set.getMaterialCode());
					String materialId = null;
					if (dbUniversalMaterial == null) {
						return new JSONObject().put("Error",
								"Universal material " + set.getMaterialCode() + " not found\n");
					} else {
						materialId = dbUniversalMaterial.getId();
					}
					json.put("ArticleId", materialId);
					json.put("ArticleNumber", m.getCode());
				} else {
					String materialId = dbMaterial.getId();
					json.put("ArticleId", materialId);
					json.put("ArticleNumber", m.getCode());
				}
				json.put("LineNumber", lineNumber);
				json.put("isTextRow", false);
				json.put("Text", m.getDescription());
				// Calculate price incl 21% BTW and round price with 2 decimals
				double price = 0;
				DecimalFormat df = new DecimalFormat("#.##");
				String formatted = df.format(m.getPrice());
				price = Double.parseDouble(formatted.toString().replaceAll(",", "."));
				json.put("UnitPrice", price);
				json.put("UnitName", m.getUnit());
				json.put("Quantity", m.getQuantity());
				json.put("IsWorkCost", false);
				json.put("IsVatFree", false);
				Project dbProject = ObjectDAO.getProjectByCode(t.getSoftwareToken(), w.getProjectNr());
				if (dbProject != null) {
					json.put("ProjectId", dbProject.getCodeExt());
				}
				JSONArrayMaterials.put(json);
				lineNumber++;
				
			}
			for (WorkPeriod p : w.getWorkPeriods()) {
				HourType h = ObjectDAO.getHourType(t.getSoftwareToken(), p.getHourType());
				// Get ID from db(hourtype)
				if (h == null) {
					error += "Hourtype " + p.getHourType()
							+ " not found in eAccounting or hourtype is not synchronized\n";
					return new JSONObject().put("Error", error);
				} else {
					JSONObject json = new JSONObject();
					json.put("ArticleId", h.getId());
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
					json.put("ArticleNumber", p.getHourType());
					json.put("LineNumber", lineNumber);
					json.put("isTextRow", false);
					if (p.getDescription().equals("")) {
						json.put("Text", h.getName());
					} else {
						json.put("Text", p.getDescription());
					}
					DecimalFormat df1 = new DecimalFormat("#.##");
					String formatted1 = df1.format(h.getSalePrice());
					Double unitPrice = Double.parseDouble(formatted1.toString().replaceAll(",", "."));
					json.put("UnitPrice", unitPrice);
					json.put("UnitName", "Uur");
					json.put("Quantity", quantity);
					json.put("IsWorkCost", false);
					json.put("IsVatFree", false);
					Project dbProject = ObjectDAO.getProjectByCode(t.getSoftwareToken(), w.getProjectNr());
					if (dbProject != null) {
						json.put("ProjectId", dbProject.getCodeExt());
					}
					JSONArrayMaterials.put(json);
					lineNumber++;
				}
			}
			JSONObject.put("Rows", JSONArrayMaterials);
			// Get ID from db
			Relation dbRelation = ObjectDAO.getRelation(t.getSoftwareToken(), w.getCustomerDebtorNr(), "invoice");
			if (dbRelation == null) {
				// error += "Relation " + w.getCustomerDebtorNr()
				// + " not found in eAccounting or relation is not
				// synchronized\n";
				// Create new relation
				JSONObject object = setRelation(t, w);
				if (object != null) {
					JSONObject.put("CustomerId", object.getString("Id"));
				} else {
					if (w.getCustomerDebtorNr().equals("") || w.getCustomerDebtorNr() == null) {
						error = "Something is wrong with relation " + w.getRelations().get(0).getCompanyName() + "\n";
					} else {
						error = "Relation " + w.getCustomerDebtorNr() + " is not synchronized\n";
					}
				}
			} else {
				String customerId = dbRelation.getId();
				JSONObject.put("CustomerId", customerId);
			}
			
			JSONObject.put("InvoiceDate", workDate);
			JSONObject.put("DueDate", workEndDate);
			JSONObject.put("RotReducedInvoicingType", "Normal");
			JSONObject.put("ReverseChargeOnConstructionServices", false);
			for (Relation r : w.getRelations()) {
				Address a = r.getAddressess().get(0);
				if (a.getType().equals("invoice")) {
					JSONObject.put("YourReference", r.getContact());
					JSONObject.put("InvoiceCustomerName", r.getCompanyName());
					JSONObject.put("InvoiceAddress1", a.getStreet());
					JSONObject.put("InvoicePostalCode", a.getPostalCode());
					JSONObject.put("InvoiceCity", a.getCity());
					JSONObject.put("InvoiceCountryCode", "NL");
				}
				if (a.getType().equals("postal")) {
					JSONObject.put("DeliveryCustomerName", r.getCompanyName());
					JSONObject.put("DeliveryAddress1", a.getStreet());
					JSONObject.put("DeliveryPostalCode", a.getPostalCode());
					JSONObject.put("DeliveryCity", a.getCity());
					JSONObject.put("DeliveryCountryCode", "NL");
				}
			}
			
			System.out.println("SETFACTUUR JSONOBJECT " + JSONObject);
		} catch (JSONException | SQLException e) {
			e.printStackTrace();
		}
		if (error.equals("")) {
			return JSONObject;
		} else {
			return new JSONObject().put("Error", error);
		}
		
	}
	
	public static JSONObject setRelation(Token t, WorkOrder w) {
		JSONObject setRelationResponse = null;
		// Get BTW id default is 21%
		String path = "/v1/termsofpayment";
		JSONArray termsofpaymentArray;
		JSONObject termsofpayment;
		String id = "";
		JSONObject JSONObject = new JSONObject();
		try {
			termsofpaymentArray = (JSONArray) getJSON(t.getAccessToken(), "", path, "array");
			for (int i = 0; i < termsofpaymentArray.length(); i++) {
				termsofpayment = termsofpaymentArray.getJSONObject(i);
				String type = termsofpayment.getString("TermsOfPaymentTypeText");
				int numberDays = termsofpayment.getInt("NumberOfDays");
				if (type.equals("Normal") && numberDays == 30) {
					logger.info("PaymentMethod RESPONSE " + termsofpayment);
					id = termsofpayment.getString("Id");
				}
			}
			JSONObject.put("TermsOfPaymentId", id);
			JSONObject.put("IsPrivatePerson", false);
			for (Relation r : w.getRelations()) {
				Address a = r.getAddressess().get(0);
				if (a.getType().equals("invoice")) {
					if (w.getCustomerDebtorNr() != null && !w.getCustomerDebtorNr().equals("")) {
						JSONObject.put("CustomerNumber", w.getCustomerDebtorNr());
					}
					JSONObject.put("EmailAddress", r.getEmailWorkorder());
					JSONObject.put("InvoiceAddress1", a.getStreet());
					JSONObject.put("InvoiceCity", a.getCity());
					JSONObject.put("InvoicePostalCode", a.getPostalCode());
					JSONObject.put("MobilePhone", a.getPhoneNumber());
					JSONObject.put("Name", r.getCompanyName());
					JSONObject.put("Note", a.getRemark());
				} else if (a.getType().equals("postal")) {
					JSONObject.put("ContactPersonEmail", a.getEmail());
					if (!a.getName().equals("") && a.getName() != null) {
						JSONObject.put("ContactPersonName", a.getName());
					}
					JSONObject.put("DeliveryCustomerName", r.getCompanyName());
					JSONObject.put("DeliveryAddress1", a.getStreet());
					JSONObject.put("DeliveryCity", a.getCity());
					JSONObject.put("DeliveryPostalCode", a.getPostalCode());
				}
				
			}
			path = "/v1/customers";
			logger.info("setRelationRequest " + JSONObject);
			setRelationResponse = (JSONObject) putJSON(t.getAccessToken(), JSONObject, path, "object", "POST");
			logger.info("setRelationResponse " + setRelationResponse);
		} catch (IOException | URISyntaxException | JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return setRelationResponse;
	}
	
	private String encodeFileToBase64Binary(String pdfUrl) throws IOException {
		URL url = new URL(pdfUrl);
		InputStream in = url.openStream();
		Files.copy(in, Paths.get("Werkbon.pdf"), StandardCopyOption.REPLACE_EXISTING);
		in.close();
		File file = new File("Werkbon.pdf");
		byte[] bytes = loadFile(file);
		byte[] encoded = Base64.encodeBase64(bytes);
		String encodedString = new String(encoded);
		return encodedString;
	}
	
	private static byte[] loadFile(File file) throws IOException {
		InputStream is = new FileInputStream(file);
		
		long length = file.length();
		if (length > Integer.MAX_VALUE) {
			// File is too large
		}
		byte[] bytes = new byte[(int) length];
		
		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
			offset += numRead;
		}
		
		if (offset < bytes.length) {
			throw new IOException("Could not completely read file " + file.getName());
		}
		is.close();
		return bytes;
	}
	
	public Boolean setAttachemen(String accessToken, String id, String url) throws Exception {
		JSONObject JSONObject = new JSONObject();
		Boolean b;
		String base64 = encodeFileToBase64Binary(url);
		try {
			JSONObject.put("ThumbNail", id);
			JSONObject.put("ContentType", "application/pdf");
			JSONObject.put("Filename", "Werkbon_" + id + ".pdf");
			JSONObject.put("Data", base64);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		String path = "/v1/attachments";
		JSONObject setAttachment = (JSONObject) putJSON(accessToken, JSONObject, path, "object", "POST");
		System.out.println("JSONObject attachement " + JSONObject);
		System.out.println("JSONResponse attachement " + setAttachment);
		logger.info("Attachement response " + "Leeg");
		// String status = jsonList.getString("status");
		// if (status.equals("success")) {
		// b = true;
		// } else {
		// b = false;
		// }
		return true;
	}
}
