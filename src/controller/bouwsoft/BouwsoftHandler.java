package controller.bouwsoft;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.DateFormat;
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
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

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
import object.workorder.EmployeeExtended;
import object.workorder.HourType;
import object.workorder.Material;
import object.workorder.MaterialCategory;
import object.workorder.Project;
import object.workorder.Relation;
import object.workorder.WOAObject;
import object.workorder.WorkOrder;
import object.workorder.WorkOrderExtended;
import object.workorder.WorkPeriod;

public class BouwsoftHandler {
	private static String refreshTokenUrl = System.getenv("BOUWSOFT_REFRESHTOKEN_URL");
	final String softwareName = "Bouwsoft";
	private Boolean checkUpdate = false;
	private final static Logger logger = Logger.getLogger(SoapHandler.class.getName());
	
	public static Token checkAccessToken(Token t) throws IOException {
		Boolean valid = false;
		String link = "https://" + t.getConsumerSecret() + "/api/v1/Employees/";
		try {
			URL url = new URL(link);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Clientnr", t.getConsumerToken());
			conn.setRequestProperty("AccessToken", t.getAccessToken());
			conn.setUseCaches(false);
			// Check if accessToken is valid
			if (conn.getResponseCode() == 401) {
				valid = false;
			} else {
				valid = true;
			}
			conn.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Create new accessToken with refresh token if current one is invalid
		if (!valid) {
			link = refreshTokenUrl + "AccessToken";
			try {
				URL url = new URL(link);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setDoOutput(true);
				conn.setInstanceFollowRedirects(false);
				conn.setRequestMethod("GET");
				conn.setRequestProperty("Content-Type", "application/json");
				conn.setRequestProperty("clientnr", t.getConsumerToken());
				conn.setRequestProperty("refreshToken", t.getAccessSecret());
				conn.setUseCaches(false);
				BufferedReader br = null;
				if (conn.getResponseCode() > 200 && conn.getResponseCode() < 405) {
					br = new BufferedReader(new InputStreamReader((conn.getErrorStream())));
				} else {
					br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
				}
				String output;
				while ((output = br.readLine()) != null) {
					try {
						JSONObject json = new JSONObject(output);
						t.setAccessToken(json.getString("AccessToken"));
						t.setConsumerSecret(json.getString("ServerName"));
						try {
							TokenDAO.updateToken(t);
						} catch (SQLException e) {
							e.printStackTrace();
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				
				conn.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return t;
	}
	
	public JSONArray getJsonResponse(Token t, String path, String parameters)
			throws IOException, JSONException, URISyntaxException {
		String link = "https://" + t.getConsumerSecret() + "/api/v1" + path + parameters;
		JSONArray records = null;
		try {
			URL url = new URL(link);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("clientnr", t.getConsumerToken());
			conn.setRequestProperty("accessToken", t.getAccessToken());
			conn.setUseCaches(false);
			BufferedReader br = null;
			if (conn.getResponseCode() > 200 && conn.getResponseCode() < 405) {
				br = new BufferedReader(new InputStreamReader((conn.getErrorStream())));
			} else {
				br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			}
			String output;
			
			while ((output = br.readLine()) != null) {
				JSONObject json = new JSONObject(output);
				records = json.optJSONArray("Records");
			}
			
			conn.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return records;
	}
	
	public JSONArray postJsonResponse(Token t, String path, String parameters, String jsonRequest)
			throws IOException, JSONException, URISyntaxException {
		String link = "https://" + t.getConsumerSecret() + "/api/v1" + path + parameters;
		byte[] postData = jsonRequest.getBytes(StandardCharsets.UTF_8);
		int postDataLength = postData.length;
		JSONArray records = null;
		try {
			URL url = new URL(link);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("clientnr", t.getConsumerToken());
			conn.setRequestProperty("accessToken", t.getAccessToken());
			conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
			conn.setUseCaches(false);
			try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
				wr.write(postData);
			}
			
			BufferedReader br = null;
			if (conn.getResponseCode() > 200 && conn.getResponseCode() < 405) {
				br = new BufferedReader(new InputStreamReader((conn.getErrorStream())));
			} else {
				br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			}
			String output;
			
			while ((output = br.readLine()) != null) {
				JSONObject json = new JSONObject(output);
				records = json.optJSONArray("Records");
			}
			
			conn.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return records;
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
		Format formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		String s = formatter.format(date);
		return s;
	}
	
	public String convertDate(String string, String formatResponse) {
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
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
	
	// Categories
	public ArrayList<MaterialCategory> getMaterialList(Token t, String date, Boolean array) throws Exception {
		String parameters = "";
		String path = "/Products/";
		ArrayList<MaterialCategory> categories = new ArrayList<MaterialCategory>();
		parameters = "?columns=list&distinct=true";
		JSONArray JSONArray = getJsonResponse(t, path, parameters);
		if (JSONArray != null) {
			for (int i = 0; i < JSONArray.length(); i++) {
				JSONObject object = JSONArray.getJSONObject(i);
				String naam = object.getString("list");
				String code = object.getString("list");
				MaterialCategory category = new MaterialCategory(code, naam, 1, null);
				categories.add(category);
			}
		}
		return categories;
	}
	
	// Medewerkers
	public String[] getEmployees(Token t, String date) throws Exception {
		String errorMessage = "";
		int importCount = 0;
		String parameters = "";
		String path = "/Employees/";
		ArrayList<EmployeeExtended> employees = new ArrayList<EmployeeExtended>();
		Boolean hasContent = ObjectDAO.hasContent(t.getSoftwareToken(), "employees");
		if (date != null && hasContent) {
			// yyyy-MM-dd hh:mm:ss
			parameters = "?filter=ts_lastupdate gt '" + getDateMinHour(date) + "'";
			parameters = parameters.replace(" ", "%20");
		}
		
		JSONArray JSONArray = getJsonResponse(t, path, parameters);
		logger.info("Employees response " + JSONArray);
		if (JSONArray != null) {
			for (int i = 0; i < JSONArray.length(); i++) {
				JSONObject object = JSONArray.getJSONObject(i);
				if (!object.getString("firstname").equals("")) {
					String firstName = object.getString("firstname");
					String lastName = object.getString("surname");
					if (lastName.equals("")) {
						lastName = "<leeg>";
					}
					String code = object.getInt("id") + "";
					String telephone = "";
					importCount++;
					EmployeeExtended m = new EmployeeExtended(Integer.parseInt(code), firstName, lastName, code,
							telephone);
					employees.add(m);
				}
				
			}
		}
		if (!employees.isEmpty()) {
			int successAmount = (int) WorkOrderHandler.addData(t.getSoftwareToken(), employees, "employeesExtended",
					softwareName);
			if (successAmount > 0) {
				ObjectDAO.saveEmployeesExtended(employees, t.getSoftwareToken());
				errorMessage += importCount + " employees imported<br>";
				checkUpdate = true;
			} else {
				errorMessage += "Something went wrong with employees<br>";
			}
		} else {
			errorMessage += "No employees for import<br>";
		}
		
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	// Materialen
	public String[] getMaterials(Token t, String date, int skip, int successAmount, ArrayList<Material> mat,
			Settings set) throws Exception {
		t = checkAccessToken(t);
		String errorMessage = "";
		String parameters = "";
		String path = "/Products/";
		ArrayList<Material> materials = new ArrayList<Material>();
		ArrayList<String> dbMaterialGroups = null;
		String lists = "";
		if (set.getImportOffice() != null) {
			String list = set.getImportOffice().replace("[", "").replace("]", "");
			String[] strValues = list.split(",\\s");
			dbMaterialGroups = new ArrayList<String>(Arrays.asList(strValues));
			for (int i = 0; i < dbMaterialGroups.size(); i++) {
				lists += "list eq '" + dbMaterialGroups.get(i) + "'";
				if (i + 1 != dbMaterialGroups.size()) {
					lists += " OR ";
				}
			}
		}
		if (date != null) {
			lists = " AND (" + lists + ")";
			// yyyy-MM-dd hh:mm:ss
			parameters = "?offset=" + skip + "&limit=500" + "&filter=ts_lastupdate gt '" + getDateMinHour(date) + "'";
			parameters = parameters.replace(" ", "%20");
		} else {
			lists = "&filter=" + lists;
			parameters = "?offset=" + skip + "&limit=500";
		}
		if (!lists.equals("&")) {
			parameters += lists;
			parameters = parameters.replace(" ", "%20");
		}
		JSONArray JSONArray = getJsonResponse(t, path, parameters);
		logger.info("Material response " + JSONArray);
		if (JSONArray != null) {
			if (mat == null) {
				mat = new ArrayList<Material>();
			}
			for (int i = 0; i < JSONArray.length(); i++) {
				JSONObject object = JSONArray.getJSONObject(i);
				String code = object.getInt("id") + "";
				String unit = object.getString("unit");
				String list = object.getString("list");
				String description = object.getString("description");
				// Price without taxes...
				double salesPrice = object.getDouble("price_catalogue");
				String modified = object.getString("ts_lastupdate");
				Material m = new Material(code, null, unit, description, salesPrice, null, modified, list);
				materials.add(m);
				mat.add(m);
				
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
	
	// Categories
	public String[] getCategories(Token t, String date, Boolean array) throws Exception {
		String errorMessage = "";
		String parameters = "";
		String path = "/Products/";
		ArrayList<MaterialCategory> categories = new ArrayList<MaterialCategory>();
		if (date != null) {
			// yyyy-MM-dd hh:mm:ss
			parameters = "?filter=ts_lastupdate gt '" + getDateMinHour(date)
					+ "'&columns=list|ts_lastupdate&distinct=true";
			parameters = parameters.replace(" ", "%20");
		} else {
			parameters = "?columns=list|ts_lastupdate&distinct=true";
		}
		
		JSONArray JSONArray = getJsonResponse(t, path, parameters);
		logger.info("Categories response " + JSONArray);
		if (JSONArray != null) {
			for (int i = 0; i < JSONArray.length(); i++) {
				JSONObject object = JSONArray.getJSONObject(i);
				String naam = object.getString("list");
				String code = object.getString("list");
				String lastUpdate = object.getString("ts_lastupdate");
				MaterialCategory category = new MaterialCategory(code, naam, 1, lastUpdate);
				categories.add(category);
			}
		}
		// Categories log message
		if (!categories.isEmpty()) {
			int success = (int) WorkOrderHandler.addData(t.getSoftwareToken(), categories, "categories", softwareName);
			if (success > 0) {
				
				// ObjectDAO.saveMaterials(mat, t.getSoftwareToken());
				errorMessage += "- " + success + " categories imported<br>";
				checkUpdate = true;
			} else {
				errorMessage += "Something went wrong with categories<br>";
			}
		} else {
			errorMessage += "No categories for import<br>";
		}
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	// Uursoorten
	public String[] getHourtypes(Token t, String date) throws Exception {
		String errorMessage = "";
		String parameters = "";
		String path = "/Jobs/";
		ArrayList<HourType> hourtypes = new ArrayList<HourType>();
		
		JSONArray JSONArray = getJsonResponse(t, path, parameters);
		logger.info("Hourtype response " + JSONArray);
		if (JSONArray != null) {
			for (int i = 0; i < JSONArray.length(); i++) {
				JSONObject object = JSONArray.getJSONObject(i);
				String id = object.getInt("id") + "";
				String name = object.optString("name");
				Double price = object.optDouble("price_hour");
				if (name != null && !name.equals("")) {
					HourType h = new HourType(id, name, 0, 1, 0, price, 1, null, id);
					hourtypes.add(h);
				}
				
			}
		}
		// Hourtype log message
		if (!hourtypes.isEmpty()) {
			int success = (int) WorkOrderHandler.addData(t.getSoftwareToken(), hourtypes, "hourtypes", softwareName);
			if (success > 0) {
				ObjectDAO.saveHourTypes(hourtypes, t.getSoftwareToken());
				errorMessage += success + " hourtypes imported<br>";
				checkUpdate = true;
			} else {
				errorMessage += "Something went wrong with hourtypes<br>";
			}
		} else {
			errorMessage += "No hourtypes for import<br>";
		}
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	// Relaties
	public String[] getRelations(Token t, String date, int skip, int successAmount, ArrayList<Relation> rel,
			Settings set) throws Exception {
		t = checkAccessToken(t);
		String errorMessage = "";
		String parameters = "";
		Relation r = null;
		String path = "/Addresses/";
		ArrayList<Relation> relations = new ArrayList<Relation>();
		Boolean hasContent = ObjectDAO.hasContent(t.getSoftwareToken(), "relations");
		if (date != null && hasContent) {
			// yyyy-MM-dd hh:mm:ss
			// Check if relation is client or supplier
			parameters = "?offset=" + skip + "&limit=500&filter=ts_lastupdate gt '" + getDateMinHour(date)
					+ "' and clientnr gt 0 and name ne ' '";
			parameters = parameters.replace(" ", "%20");
		} else {
			// Check if address is client and not supplier
			parameters = "?offset=" + skip + "&limit=500&filter=clientnr gt 0 and name ne ' '";
			parameters = parameters.replace(" ", "%20");
		}
		JSONArray JSONArray = getJsonResponse(t, path, parameters);
		logger.info("Relations response " + JSONArray);
		if (JSONArray != null) {
			if (rel == null) {
				rel = new ArrayList<Relation>();
			}
			for (int i = 0; i < JSONArray.length(); i++) {
				ArrayList<Address> addresses = new ArrayList<Address>();
				JSONObject object = JSONArray.getJSONObject(i);
				String id = object.getInt("id") + "";
				String code = object.getInt("clientnr") + "";
				String name = object.getString("name");
				String street = object.getString("address");
				String city = object.getString("city");
				String postalCode = object.getString("zipcode");
				String telefone1 = object.getString("telephone1");
				String mobile = object.getString("gsm");
				if (telefone1.equals("")) {
					telefone1 = mobile;
				}
				String email = object.getString("email");
				String modified = object.getString("ts_lastupdate");
				Address mainAddress = new Address("", telefone1, email, street, "", postalCode, city, null, "main",
						Integer.parseInt(id));
				addresses.add(mainAddress);
				r = new Relation(name, id, "", email, addresses, modified, id);
				relations.add(r);
				rel.add(r);
			}
		}
		if (!relations.isEmpty()) {
			int success = (int) WorkOrderHandler.addData(t.getSoftwareToken(), relations, "relations", softwareName);
			if (relations.size() == 500) {
				if (success > 0) {
					successAmount += success;
				} else {
					errorMessage += "Something went wrong with relations<br>";
				}
				System.out.println("recursive");
				return getRelations(t, date, skip += 500, successAmount, rel, set);
				
			} else {
				if (success > 0) {
					successAmount += success;
					
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
	
	// Relaties
	public String[] getContacts(Token t, String date, int skip, int successContactsAmount, int successAddressesAmount,
			ArrayList<Relation> rel) throws Exception {
		t = checkAccessToken(t);
		String errorMessage = "";
		String parameters = "";
		Relation r = null;
		String path = "/AddressContacts/";
		ArrayList<Relation> relations = new ArrayList<Relation>();
		
		// Check if address is client and not supplier
		parameters = "?offset=" + skip + "&limit=500&filter=contact_name ne ' '";
		parameters = parameters.replace(" ", "%20");
		
		JSONArray JSONArray = getJsonResponse(t, path, parameters);
		logger.info("Contacts response " + JSONArray);
		if (JSONArray != null) {
			if (rel == null) {
				rel = new ArrayList<Relation>();
			}
			String contactName = "";
			for (int j = 0; j < JSONArray.length(); j++) {
				ArrayList<Address> addresses = new ArrayList<Address>();
				JSONObject contactJson = JSONArray.getJSONObject(j);
				int contactId = contactJson.getInt("id");
				int addressId = contactJson.getInt("address_id");
				contactName = contactJson.getString("contact_name");
				String contactStreet = contactJson.getString("contact_address");
				String contactPostalCode = contactJson.getString("contact_zipcode");
				String contactCity = contactJson.getString("contact_city");
				String contactEmail = contactJson.getString("contact_email");
				String contactTelephone = contactJson.getString("contact_telephone");
				String contactMobile = contactJson.getString("contact_gsm");
				
				if (contactTelephone.equals("")) {
					contactTelephone = contactMobile;
				}
				Address contact = new Address(contactName, contactTelephone, contactEmail, contactStreet, "",
						contactPostalCode, contactCity, null, "contact", contactId);
				addresses.add(contact);
				r = new Relation(null, addressId + "", contactName, null, addresses, null, addressId + "");
				relations.add(r);
				rel.add(r);
			}
		}
		if (!relations.isEmpty()) {
			int successContacts = 0, successAddresses = 0;
			successContacts = (int) WorkOrderHandler.addData(t.getSoftwareToken(), relations, "contactpersons",
					softwareName);
			successContactsAmount += successContacts;
			successAddresses = (int) WorkOrderHandler.addData(t.getSoftwareToken(), relations, "addresses",
					softwareName);
			successAddressesAmount += successAddresses;
			
			if (relations.size() == 500) {
				if (successContacts == 0) {
					errorMessage += "Something went wrong with contactpersons<br>";
				}
				System.out.println("recursive");
				return getContacts(t, date, skip += 500, successContactsAmount, successAddressesAmount, rel);
				
			} else {
				if (successContactsAmount > 0) {
					errorMessage += "- " + successContactsAmount + " contacts imported<br>";
				}
				if (successAddressesAmount > 0) {
					errorMessage += "- " + successAddressesAmount + " addresses imported<br>";
				}
				checkUpdate = true;
			}
			
		} else {
			if (successContactsAmount > 0) {
				errorMessage += "- " + successContactsAmount + " contacts imported<br>";
				checkUpdate = true;
			}
			if (successAddressesAmount > 0) {
				errorMessage += "- " + successAddressesAmount + " addresses imported<br>";
				checkUpdate = true;
			}
		}
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	// Projecten
	public String[] getProjects(Token t, String date, Settings set, int skip, int successAmount, ArrayList<Project> pro)
			throws Exception {
		String errorMessage = "";
		String parameters = "";
		String path = "/Projects/";
		ArrayList<Project> projects = new ArrayList<Project>();
		Boolean hasContent = ObjectDAO.hasContent(t.getSoftwareToken(), "projects");
		if (set.getExportOffice() != null) {
			if (date != null && hasContent) {
				// yyyy-MM-dd hh:mm:ss
				parameters = "?filter=ts_lastupdate gt '" + getDateMinHour(date) + "'&offset=" + skip + "&limit=500";
				parameters = parameters.replace(" ", "%20");
			}
		} else {
			if (date != null && hasContent) {
				// yyyy-MM-dd hh:mm:ss
				// Check if project is not completed and approved as default
				// filter
				parameters = "?filter=ts_lastupdate gt '" + getDateMinHour(date)
						+ "' and approved eq true and completed eq false&offset=" + skip + "&limit=500";
				parameters = parameters.replace(" ", "%20");
			} else {
				// Check if project is not completed and approved as default
				// filter
				parameters = "?filter=approved eq true and completed eq false&offset=" + skip + "&limit=500";
				parameters = parameters.replace(" ", "%20");
			}
		}
		
		JSONArray JSONArray = getJsonResponse(t, path, parameters);
		logger.info("Projects response " + JSONArray);
		if (JSONArray != null) {
			for (int i = 0; i < JSONArray.length(); i++) {
				if (pro == null) {
					pro = new ArrayList<Project>();
				}
				JSONObject object = JSONArray.getJSONObject(i);
				String debtorNr = null;
				String id = object.getInt("id") + "";
				String name = object.getString("name");
				if (name != null && !name.equals("")) {
					String code = object.getInt("pnr") + "";
					String externNr = object.getInt("unr") + "";
					String reference = object.getString("yourreference");
					String clientId = object.getInt("client_addressid") + "";
					// String status = object.getString("status");
					String dateStart = object.getString("date_approved");
					// String dateEnd = object.getString("date_end");
					
					if (!clientId.equals("0")) {
						debtorNr = clientId;
					} else {
						debtorNr = "";
					}
					Project p = new Project(code, externNr, debtorNr, "active", name, dateStart, null, reference, 0, 1,
							null);
					projects.add(p);
					pro.add(p);
				}
			}
		}
		if (!projects.isEmpty()) {
			int success = (int) WorkOrderHandler.addData(t.getSoftwareToken(), projects, "projects", softwareName);
			successAmount += success;
			if (projects.size() == 500) {
				if (success == 0) {
					errorMessage += "Something went wrong with projects<br>";
				}
				System.out.println("recursive");
				return getProjects(t, date, set, skip += 500, successAmount, pro);
				
			} else {
				if (success > 0) {
					ObjectDAO.saveProjects(pro, t.getSoftwareToken());
					errorMessage += successAmount + " projects imported<br>";
					checkUpdate = true;
				} else {
					errorMessage += "Something went wrong with projects<br>";
				}
			}
		} else {
			if (successAmount > 0) {
				ObjectDAO.saveProjects(pro, t.getSoftwareToken());
				errorMessage += successAmount + " projects imported<br>";
				checkUpdate = true;
			}
		}
		
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	public String getCurrentDate() {
		ZonedDateTime za = ZonedDateTime.now(ZoneId.of("Europe/Paris"));
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		return za.format(formatter);
	}
	
	// Onderhoudsfiche
	// public String[] getMaintenance(Token t, String date) throws Exception {
	// String errorMessage = "";
	// String parameters = "";
	// String path = "/MaintenanceSheet/";
	// ArrayList<WorkOrderExtended> workorders = new
	// ArrayList<WorkOrderExtended>();
	// if (date != null) {
	// // yyyy-MM-dd hh:mm:ss
	// parameters = "?filter=ts_lastupdate gt '" + getDateMinHour(date) + "' and
	// address_id ne 0";
	// parameters = parameters.replace(" ", "%20");
	// } else {
	// parameters = "?filter=address_id ne 0";
	// parameters = parameters.replace(" ", "%20");
	// }
	// JSONArray JSONArray = getJsonResponse(t, path, parameters);
	// logger.info("MaintenanceSheet response " + JSONArray);
	// WorkOrderExtended w = null;
	// if (JSONArray != null) {
	// for (int i = 0; i < JSONArray.length(); i++) {
	// ArrayList<Relation> relations = new ArrayList<Relation>();
	// ArrayList<Address> address = new ArrayList<Address>();
	// JSONObject object = JSONArray.getJSONObject(i);
	// String id = object.getInt("id") + "";
	// String code = object.getInt("nr1") + "";
	// String description = object.getString("description");
	//
	// String addressId = object.optInt("address_id") + "";
	// String debtorNr = null;
	// Relation dbRelation = ObjectDAO.getRelationById(t.getSoftwareToken(),
	// addressId, "main");
	// String invoiceCompanyName = object.optString("address_name", "");
	// if (dbRelation == null) {
	// errorMessage += "Relation " + invoiceCompanyName
	// + " not synced, so this maintenancesheet is not imported<br>";
	// } else {
	// debtorNr = dbRelation.getDebtorNumber();
	//
	//
	// String invoiceDebtorNr = debtorNr;
	// String invoiceEmail = object.optString("address_email", "");
	// String invoicestreet = object.optString("address_address", "");
	// String invoicepostalCode = object.optString("address_zipcode", "");
	// String invoicecity = object.optString("address_city", "");
	// String mobileNr = object.optString("address_gsm", "");
	// String phoneNr = object.optString("address_telephone", "");
	// if (phoneNr.equals("")) {
	// phoneNr = mobileNr;
	// }
	// Address invoice = new Address(invoiceCompanyName, phoneNr, invoiceEmail,
	// invoicestreet, "",
	// invoicepostalCode, invoicecity, null, "invoice", 1);
	// Address postal = new Address(invoiceCompanyName, phoneNr, invoiceEmail,
	// invoicestreet, "",
	// invoicepostalCode, invoicecity, null, "postal", 2);
	// address.add(invoice);
	// address.add(postal);
	// String contact = dbRelation.getContact();
	// if (contact == null || contact.equals("")) {
	// contact = "<leeg>";
	// }
	// String projectNr = object.optInt("project_pnr") + "";
	// String employeeNr = object.optInt("employee_id") + "";
	// String addressEmail = object.getString("address_email");
	// String workDate = object.getString("nextmaintenance");
	// if (workDate != null && !workDate.equals("")) {
	// workDate = convertDate(workDate, "dd-MM-yyyy");
	// }
	// String typeofwork = "Installatie";
	// String paymentMethod = "leeg";
	// String modified = object.getString("ts_lastupdate");
	// Relation r = new Relation(invoiceCompanyName, invoiceDebtorNr,
	// dbRelation.getContact(),
	// invoiceEmail, address, null, null);
	// relations.add(r);
	// // Devices
	// ArrayList<WOAObject> objects = getMaintenanceDevice(t, id, debtorNr);
	//
	// w = new WorkOrderExtended(projectNr, workDate, addressEmail, null,
	// debtorNr, null, paymentMethod,
	// null, getCurrentDate(), id, code, null, relations, null, null, null,
	// null, typeofwork,
	// description, modified, null, null, employeeNr, objects);
	// workorders.add(w);
	// }
	// }
	// }
	// if (!workorders.isEmpty()) {
	// JSONArray responseArray = (JSONArray)
	// WorkOrderHandler.addData(t.getSoftwareToken(), workorders,
	// "PostWorkordersExtended", softwareName, null);
	// int successAmount = responseArray.length();
	// if (successAmount > 0) {
	// errorMessage += successAmount + " maintenance sheets imported<br>";
	// checkUpdate = true;
	// } else {
	// errorMessage += "Something went wrong with maintenance sheets<br>";
	// }
	// } else {
	// errorMessage += "No maintenance sheets for import<br>";
	// }
	//
	// return new String[] { errorMessage, checkUpdate + "" };
	// }
	//
	// // Onderhoudsfiche
	// public ArrayList<WOAObject> getMaintenanceDevice(Token t, String id,
	// String debtorNr) throws Exception {
	// // Get devices
	// String parameters = "";
	// String path = "/Devices/";
	// ArrayList<WOAObject> objects = new ArrayList<WOAObject>();
	// parameters = "?filter=maintenancesheet_id eq " + id;
	// parameters = parameters.replace(" ", "%20");
	// JSONArray JSONArray = getJsonResponse(t, path, parameters);
	// logger.info("Devices response " + JSONArray);
	// WOAObject WAOObject = null;
	// if (JSONArray != null) {
	// for (int j = 0; j < JSONArray.length(); j++) {
	// JSONObject deviceObject = JSONArray.getJSONObject(j);
	// String deviceId = deviceObject.getInt("id") + "";
	// String deviceDescription = deviceObject.getString("description") + "";
	// WAOObject = new WOAObject(deviceId, debtorNr, deviceDescription);
	// objects.add(WAOObject);
	// }
	// }
	// return objects;
	// }
	
	// Get Opdrachtbon
	public String[] getAssignment(Token t, String date) throws Exception {
		String errorMessage = "";
		String parameters = "";
		String path = "/Assignments/";
		ArrayList<WorkOrderExtended> workorders = new ArrayList<WorkOrderExtended>();
		if (date != null) {
			// yyyy-MM-dd hh:mm:ss
			parameters = "?filter=ts_lastupdate gt '" + getDateMinHour(date)
					+ "' and address_id ne 0 and execute eq true";
			parameters = parameters.replace(" ", "%20");
		} else {
			parameters = "?filter=address_id ne 0 and execute eq true";
			parameters = parameters.replace(" ", "%20");
		}
		JSONArray JSONArray = getJsonResponse(t, path, parameters);
		logger.info("Assignment response " + JSONArray);
		WorkOrderExtended w = null;
		if (JSONArray != null) {
			for (int i = 0; i < JSONArray.length(); i++) {
				JSONObject object = JSONArray.getJSONObject(i);
				String id = object.getInt("id") + "";
				// Client code
				String addressId = object.optInt("address_id") + "";
				ArrayList<Relation> allRelations = new ArrayList<Relation>();
				allRelations = getAddressById(t, addressId);
				Relation r = allRelations.get(0);
				String email = r.getEmailWorkorder();
				
				// Materialen
				ArrayList<Material> materials = new ArrayList<Material>();
				materials = getAssignmentProducts(t, id);
				
				// WAOObjecten
				ArrayList<WOAObject> WOAObjects = new ArrayList<WOAObject>();
				WOAObjects = getAssignmentMachines(t, id, addressId);
				
				// Werkperiodes
				ArrayList<WorkPeriod> workPeriods = new ArrayList<WorkPeriod>();
				workPeriods = getAssignmentLabor(t, id);
				
				String workDate = object.getString("date");
				
				String assignmentCode = object.getInt("nr") + "";
				String projectId = object.getInt("project_id") + "";
				String projectCode = object.getInt("project_pnr") + "";
				String employeeId = object.getInt("employee_id") + "";
				if (employeeId.equals("0")) {
					employeeId = null;
				}
				String lastUpdate = object.getString("ts_lastupdate");
				String listDescription = "";
				String maintenanceId = object.getInt("maintenancesheet_id") + "";
				path = "/maintenance_sheet/" + maintenanceId;
				JSONArray JSONArraySheet = getJsonResponse(t, path, "");
				if (JSONArraySheet != null) {
					for (int j = 0; j < JSONArraySheet.length(); j++) {
						JSONObject objectSheet = JSONArraySheet.getJSONObject(j);
						maintenanceId = objectSheet.getInt("nr1") + "";
						listDescription = objectSheet.getString("list");
						String description = objectSheet.getString("description");
						if (!description.equals("") && description != null) {
							listDescription = listDescription + " - " + description;
						}
					}
				}
				w = new WorkOrderExtended(projectId, convertDate(workDate, "dd-MM-yyyy"), email, email, addressId, null,
						"", materials, getCurrentDate(), id, maintenanceId + " - " + assignmentCode, workPeriods,
						allRelations, null, null, null, projectCode, "Onderhoud", listDescription, lastUpdate, null,
						null, employeeId, WOAObjects);
				workorders.add(w);
				
			}
		}
		if (!workorders.isEmpty()) {
			JSONArray responseArray = (JSONArray) WorkOrderHandler.addData(t.getSoftwareToken(), workorders,
					"PostWorkordersExtended", softwareName);
			int successAmount = responseArray.length();
			if (successAmount > 0) {
				errorMessage += successAmount + " assignments imported<br>";
				checkUpdate = true;
			} else {
				errorMessage += "Something went wrong with assignments<br>";
			}
		} else {
			errorMessage += "No assignments for import<br>";
		}
		
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	private ArrayList<Relation> getAddressById(Token t, String addressId) throws Exception {
		String parameters = "";
		String path = "/Addresses/" + addressId;
		JSONArray JSONArray = getJsonResponse(t, path, parameters);
		logger.info("address id response " + JSONArray);
		ArrayList<Relation> relations = null;
		Relation r = null;
		if (JSONArray != null) {
			for (int i = 0; i < JSONArray.length(); i++) {
				relations = new ArrayList<Relation>();
				ArrayList<Address> addresses = new ArrayList<Address>();
				JSONObject object = JSONArray.getJSONObject(i);
				
				String id = object.getInt("id") + "";
				String code = object.getInt("clientnr") + "";
				String name = object.getString("name");
				String street = object.getString("address");
				String city = object.getString("city");
				String postalCode = object.getString("zipcode");
				String telefone1 = object.getString("telephone1");
				String mobile = object.getString("gsm");
				if (telefone1.equals("")) {
					telefone1 = mobile;
				}
				String email = object.getString("email");
				String modified = object.getString("ts_lastupdate");
				Address invoiceAddress = new Address(name, telefone1, email, street, "", postalCode, city, null,
						"invoice", Integer.parseInt(code));
				addresses.add(invoiceAddress);
				r = new Relation(name, code, name, email, addresses, modified, id);
				relations.add(r);
			}
		}
		return relations;
		
	}
	
	private ArrayList<Material> getAssignmentProducts(Token t, String assignmentId) throws Exception {
		String parameters = "";
		String path = "/AssignmentProducts/";
		
		parameters = "?filter=assignment_id eq " + assignmentId;
		parameters = parameters.replace(" ", "%20");
		
		JSONArray JSONArray = getJsonResponse(t, path, parameters);
		logger.info("AssignmentProducts response " + JSONArray);
		ArrayList<Material> materials = null;
		if (JSONArray != null) {
			for (int i = 0; i < JSONArray.length(); i++) {
				materials = new ArrayList<Material>();
				JSONObject object = JSONArray.getJSONObject(i);
				String productId = object.getInt("product_id") + "";
				String name = object.getString("description") + "";
				String unit = object.getString("unit") + "";
				int quantity = object.getInt("quantity");
				double price = 0d;
				if (!productId.equals("0")) {
					path = "/Products/" + productId;
					parameters = "";
					JSONArray JSONArrayProduct = getJsonResponse(t, path, parameters);
					for (int j = 0; j < JSONArrayProduct.length(); j++) {
						object = JSONArrayProduct.getJSONObject(j);
						price = object.getDouble("price_sales1");
					}
				}
				Material m = new Material(productId, null, unit, name, price, quantity + "", null, productId);
				materials.add(m);
			}
		}
		return materials;
		
	}
	
	private ArrayList<WOAObject> getAssignmentMachines(Token t, String assignmentId, String relatieId)
			throws Exception {
		String errorMessage = "";
		String parameters = "";
		String path = "/AssignmentMachines/";
		
		parameters = "?filter=assignment_id eq " + assignmentId;
		parameters = parameters.replace(" ", "%20");
		
		JSONArray JSONArray = getJsonResponse(t, path, parameters);
		logger.info("AssignmentMachines response " + JSONArray);
		ArrayList<WOAObject> objects = null;
		if (JSONArray != null) {
			objects = new ArrayList<WOAObject>();
			for (int i = 0; i < JSONArray.length(); i++) {
				JSONObject object = JSONArray.getJSONObject(i);
				String machineId = object.getInt("machine_id") + "";
				String name = object.getString("description") + "";
				String unit = object.getString("unit") + "";
				int quantity = object.getInt("quantity");
				
				// path = "/Machines/" + machineId;
				// parameters = "";
				// JSONArray JSONArrayMachine = getJsonResponse(t, path,
				// parameters);
				// for (int j = 0; j < JSONArrayMachine.length(); j++) {
				// JSONObject machineObject = JSONArrayMachine.getJSONObject(j);
				// }
				WOAObject WOAObject = new WOAObject(machineId + " - " + relatieId, relatieId, name);
				objects.add(WOAObject);
			}
		}
		if (objects != null && !objects.isEmpty()) {
			int success = (int) WorkOrderHandler.addData(t.getSoftwareToken(), objects, "objects", softwareName);
			if (success > 0) {
				errorMessage += success + " objects imported<br>";
				checkUpdate = true;
			} else {
				errorMessage += "Something went wrong with objects<br>";
			}
		}
		return objects;
		
	}
	
	private ArrayList<WorkPeriod> getAssignmentLabor(Token t, String assignmentId) throws Exception {
		String errorMessage = "";
		String parameters = "";
		String path = "/AssignmentLabor/";
		
		parameters = "?filter=assignment_id eq " + assignmentId;
		parameters = parameters.replace(" ", "%20");
		
		JSONArray JSONArray = getJsonResponse(t, path, parameters);
		logger.info("AssignmentLabor response " + JSONArray);
		ArrayList<WorkPeriod> workPeriods = null;
		if (JSONArray != null) {
			for (int i = 0; i < JSONArray.length(); i++) {
				workPeriods = new ArrayList<WorkPeriod>();
				JSONObject object = JSONArray.getJSONObject(i);
				String id = object.getInt("id") + "";
				String employeeNr = object.getInt("employee_id") + "";
				if (employeeNr.equals("0")) {
					employeeNr = null;
				}
				String hourTypeNr = object.getInt("job_id") + "";
				if (hourTypeNr.equals("0")) {
					hourTypeNr = null;
				}
				String workDate = object.optString("date");
				double start = object.optDouble("from");
				double end = object.optDouble("to");
				double breakTime = object.optDouble("break");
				int duration = (int) ((end - start) - breakTime);
				String startTime = doubleToDate(start);
				String endTime = doubleToDate(end);
				WorkPeriod p = new WorkPeriod(employeeNr, hourTypeNr, convertDate(workDate, "dd-MM-yyyy"), null, null,
						duration + "", id, startTime, endTime);
				workPeriods.add(p);
			}
		}
		return workPeriods;
	}
	
	private String doubleToDate(double start) {
		int totalMinutes = (int) (start * 60);
		int minutes = totalMinutes % 60;
		int hours = (totalMinutes - minutes) / 60;
		String dateTime = String.format("%02d:%02d", hours, minutes);
		return dateTime;
	}
	
}