package controller.topdesk;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import object.workorder.Employee;
import object.workorder.Relation;
import object.workorder.WorkOrder;
import object.workorder.WorkOrderExtended;

public class TopdeskHandler {
	private static String BASEURL = "";
	private final static Logger logger = Logger.getLogger(SoapHandler.class.getName());
	
	private String basicAuth = null;
	private Boolean checkUpdate = false;
	
	public TopdeskHandler(String operatorName, String clientToken) {
		String userCredentials = operatorName + ":" + clientToken;
		this.basicAuth = "Basic " + new String(new Base64().encode(userCredentials.getBytes()));
	}
	
	public TopdeskHandler() {
	}
	//
	// public TopdeskHandler(String clientDomain) {
	// this.BASEURL = "https://" + clientDomain + "/tas/api/";
	// }
	
	public static Token checkAccessToken(Token t) {
		return t;
	}
	
	public HttpURLConnection getConnection(int postDataLength, String jsonRequest, String endpoint) throws IOException {
		URL url = new URL(BASEURL + endpoint);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setInstanceFollowRedirects(false);
		
		if (postDataLength == 0) {
			conn.setRequestMethod("GET");
		} else {
			conn.setRequestMethod("PUT");
		}
		
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("authorization", basicAuth);
		conn.setUseCaches(false);
		return conn;
	}
	
	public boolean checkCredentials(String operatorName, String clientToken, String clientDomain) throws IOException {
		BASEURL = "https://" + clientDomain + "/tas/api/";
		boolean b = false;
		JSONObject json = null;
		
		String userCredentials = operatorName + ":" + clientToken;
		basicAuth = "Basic " + new String(new Base64().encode(userCredentials.getBytes()));
		String endpoint = "version";
		
		try {
			json = (JSONObject) getJsonResponse(endpoint, null);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		if (!json.has("errors")) {
			b = true;
		}
		
		return b;
	}
	
	public Object getJsonResponse(String endpoint, String jsonRequest) throws IOException, JSONException {
		String jsonString;
		Object json = null;
		int postDataLength = 0;
		
		try {
			// Sets up the rest call;
			HttpURLConnection conn = getConnection(postDataLength, jsonRequest, endpoint);
			BufferedReader br = null;
			// Send request to TopDesk
			
			if (conn.getResponseCode() == 204) {
				System.out.println("Response message: no content");
			} else if (conn.getResponseCode() != 200 && conn.getResponseCode() != 206) {
				System.out.println("Response message " + conn.getResponseMessage());
				br = new BufferedReader(new InputStreamReader((conn.getErrorStream())));
			} else {
				br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			}
			
			if (conn.getResponseCode() == 204) {
				json = new JSONObject();
				((JSONObject) json).put("errors", "no content");
			} else {
				while ((jsonString = br.readLine()) != null) {
					if (conn.getResponseCode() == 200 || conn.getResponseCode() == 206) {
						if (jsonString.startsWith("{")) {
							json = new JSONObject(jsonString);
						} else if (jsonString.startsWith("[")) {
							json = new JSONArray(jsonString);
						}
					} else {
						JSONArray errors = new JSONArray();
						errors.put(jsonString);
						json = new JSONObject();
						((JSONObject) json).put("errors", errors);
					}
					System.out.println(json);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return json;
	}
	
	public Object putJsonResponse(Token t, String endpoint, String jsonRequest) throws IOException, JSONException {
		String link = "https://" + t.getConsumerSecret() + "/tas/api/" + endpoint;
		String jsonString;
		byte[] postData = jsonRequest.getBytes(StandardCharsets.UTF_8);
		
		URL url = new URL(link);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setInstanceFollowRedirects(false);
		conn.setRequestMethod("PUT");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("authorization", basicAuth);
		conn.setUseCaches(false);
		
		try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
			wr.write(postData);
		}
		
		BufferedReader br = null;
		if (conn.getResponseCode() != 200 && conn.getResponseCode() != 206) {
			System.out.println("Response message " + conn.getResponseMessage());
			br = new BufferedReader(new InputStreamReader((conn.getErrorStream())));
		} else {
			br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
		}
		
		Object json = null;
		while ((jsonString = br.readLine()) != null) {
			if (conn.getResponseCode() == 200 || conn.getResponseCode() == 206) {
				if (jsonString.startsWith("{")) {
					json = new JSONObject(jsonString);
				} else if (jsonString.startsWith("[")) {
					json = new JSONArray(jsonString);
				}
			} else {
				JSONArray errors = new JSONArray();
				errors.put(jsonString);
				json = new JSONObject();
				((JSONObject) json).put("errors", errors);
			}
			System.out.println(json);
		}
		return json;
	}
	
	public JSONArray getProcessingStatus(String operatorName, String clientToken) throws IOException, JSONException {
		JSONArray statusOptions = null;
		
		basicAuth = setAuthorization(operatorName, clientToken);
		String endpoint = "incidents/processing_status";
		
		try {
			statusOptions = (JSONArray) getJsonResponse(endpoint, null);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return statusOptions;
	}
	
	public JSONArray getOperatorGroups(Token t) throws IOException {
		JSONArray operatorGroups = null;
		String endpoint = "operatorgroups";
		
		try {
			operatorGroups = (JSONArray) getJsonResponse(endpoint, null);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return operatorGroups;
	}
	
	public String[] getOperators(Token t) throws SQLException, IOException, JSONException {
		String errorMessage = "";
		JSONArray operators = null;
		String endpoint = "operators?page_size=100";
		String[] operatorGroups;
		ArrayList<Employee> employees = new ArrayList<>();
		
		Settings set = ObjectDAO.getSettings(t.getSoftwareToken());
		// check if operator group is set
		if (set.getUser() != null && !set.getUser().equals("")) {
			operatorGroups = set.getUser().split(";"); // Get selected operator
														// groups
			
			return getOperatorsByGroup(t, operatorGroups);
		}
		
		try {
			operators = (JSONArray) getJsonResponse(endpoint, null);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		Employee employee = null;
		if (operators != null) {
			for (int i = 0; i < operators.length(); i++) {
				JSONObject operator = operators.getJSONObject(i);
				
				String code = operator.getString("id");
				String firstname = !(operator.getString("firstName").equals("")) ? operator.getString("firstName")
						: "-";
				String lastname = operator.getString("surName");
				
				employee = new Employee(firstname, lastname, code);
				employees.add(employee);
			}
		}
		
		if (employees != null && !employees.isEmpty()) {
			int successAmount = (int) WorkOrderHandler.addData(t.getSoftwareToken(), employees, "employees",
					t.getSoftwareName());
			if (successAmount > 0) {
				errorMessage += successAmount + " employees imported<br>";
				checkUpdate = true;
			} else {
				errorMessage += "Something went wrong with employees<br>";
			}
		} else {
			errorMessage += "No employees imported";
		}
		
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	public String[] getOperatorsByGroup(Token t, String[] operatorGroups) throws IOException, JSONException {
		String errorMessage = "";
		String endpoint = "";
		JSONArray operators = null;
		ArrayList<Employee> employees = new ArrayList<>();
		
		for (String operatorGroup : operatorGroups) {
			endpoint = "operatorgroups/id/" + operatorGroup + "/operators";
			operators = (JSONArray) getJsonResponse(endpoint, null);
			
			if (operators != null && operators.length() > 0) {
				for (int i = 0; i < operators.length(); i++) {
					JSONObject operatorShort = operators.getJSONObject(i);
					String operatorId = operatorShort.getString("id");
					
					endpoint = "operators/id/" + operatorId;
					JSONObject operatorFull = (JSONObject) getJsonResponse(endpoint, null);
					
					String code = operatorFull.getString("id");
					String firstname = !(operatorFull.getString("firstName").equals(""))
							? operatorFull.getString("firstName") : "-";
					String lastname = operatorFull.getString("surName");
					
					employees.add(new Employee(firstname, lastname, code));
				}
			}
		}
		
		if (employees != null && !employees.isEmpty()) {
			int successAmount = 0;
			Object response = WorkOrderHandler.addData(t.getSoftwareToken(), employees, "employees",
					t.getSoftwareName());
			
			if (response instanceof Integer) {
				successAmount = (Integer) response;
			}
			
			if (successAmount > 0) {
				errorMessage += successAmount + " employees imported<br>";
				checkUpdate = true;
			}
		} else {
			errorMessage = "No employees imported";
		}
		
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	public String[] getIncidents(Token t) throws SQLException, IOException, JSONException {
		String errorMessage = "";
		JSONArray incidents = null;
		String endpoint = "incidents?page_size=100";
		String[] statusOptions;
		String[] operatorGroups;
		basicAuth = setAuthorization(t.getAccessSecret(), t.getAccessToken());
		ArrayList<WorkOrder> workorders = new ArrayList<WorkOrder>();
		
		Settings set = ObjectDAO.getSettings(t.getSoftwareToken());
		statusOptions = set.getImportOffice().split(";"); // get selected status
															// options
		for (String status : statusOptions) {
			endpoint += "&processing_status=" + status;
		}
		
		// check if a operator group is set
		if (set.getUser() != null && !set.getUser().equals("")) {
			operatorGroups = set.getUser().split(";");
			// if operator groups set, add each as a parameter
			for (String opGroup : operatorGroups) {
				endpoint += "&operator_group=" + opGroup;
			}
		}
		
		if (set.getSyncDate() != null && !set.getSyncDate().equals("")) {
			try {
				Date syncDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(set.getSyncDate());
				String formattedDate = new SimpleDateFormat("yyyy-MM-dd").format(syncDate);
				
				endpoint += "&modification_date_start=" + formattedDate;
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		try {
			Object incidentResponse = getJsonResponse(endpoint, null);
			if (!(incidentResponse instanceof JSONArray)) {
				return new String[] { errorMessage, checkUpdate + "No calls returned" };
			} else {
				incidents = (JSONArray) incidentResponse;
			}
			incidents = (JSONArray) getJsonResponse(endpoint, null);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		WorkOrder order = null;
		if (incidents != null) {
			for (int i = 0; i < incidents.length(); i++) {
				JSONObject incident = (JSONObject) incidents.get(i);
				
				if (incident.getJSONObject("caller").isNull("id")
						|| incident.getJSONObject("caller").getJSONObject("branch").isNull("id")) {
					continue;
				}
				
				String caller_id = incident.getJSONObject("caller").getString("id");
				endpoint = "persons/id/" + caller_id;
				JSONObject person = (JSONObject) getJsonResponse(endpoint, null);
				
				String branch_id = incident.getJSONObject("caller").getJSONObject("branch").getString("id");
				endpoint = "branches/id/" + branch_id;
				JSONObject branch = (JSONObject) getJsonResponse(endpoint, null);
				
				if (branch.isNull("name") || branch.isNull("id") || branch.getJSONObject("address").isNull("street")
						|| branch.getJSONObject("address").isNull("postcode")
						|| branch.getJSONObject("address").isNull("city")) {
					continue;
				}
				
				ArrayList<Relation> relations = new ArrayList<Relation>();
				String contactName = person.getString("firstName") + " " + person.getString("surName");
				String companyName = branch.getString("name");
				String debtor_number = branch.getString("id");
				String branchMail = branch.getString("email");
				String modified = branch.getString("modificationDate");
				Relation rel = new Relation(companyName, debtor_number, contactName, branchMail, null, modified, null);
				
				ArrayList<Address> addresses = new ArrayList<Address>();
				String phoneNumber = person.getString("phoneNumber").equals("") ? person.getString("mobileNumber")
						: person.getString("phoneNumber");
				String street = branch.getJSONObject("address").getString("street");
				String housenumber = branch.getJSONObject("address").getString("number");
				String postcode = branch.getJSONObject("address").getString("postcode");
				String city = branch.getJSONObject("address").getString("city");
				Address address = new Address(contactName, phoneNumber, branchMail, street + " " + housenumber,
						housenumber, postcode, city, null, "invoice", 0);
				
				addresses.add(address);
				rel.setAddresses(addresses);
				relations.add(rel);
				
				String targetDate = !(incident.isNull("targetDate")) ? incident.getString("targetDate") : "";
				String email = person.getString("email");
				String branchId = branch.getString("id");
				String creationDate = incident.getString("creationDate");
				String incidentId = incident.getString("id");
				String completedDate = !(incident.isNull("completedDate")) ? incident.getString("completedDate") : "";
				String employeeNr = !(incident.getJSONObject("operator").isNull("id"))
						? incident.getJSONObject("operator").getString("id") : "";
				
				String briefDesciption = incident.getString("briefDescription");
				String modificationDate = incident.getString("modificationDate");
				
				order = new WorkOrder(null, targetDate, email, email, branchId, null, "", null, creationDate,
						incidentId, incidentId, null, relations, null, completedDate, null, null, "Project",
						briefDesciption, modificationDate, null, null, employeeNr);
				workorders.add(order);
				updateStatus(t, order, "Sync");
			}
		}
		if (workorders != null) {
			JSONArray responseArray;
			int successAmount = 0;
			Object response = WorkOrderHandler.addData(t.getSoftwareToken(), workorders, "PostWorkorders",
					t.getSoftwareName());
			if (response instanceof JSONArray) {
				responseArray = (JSONArray) response;
				successAmount = responseArray.length();
			}
			
			if (successAmount > 0) {
				errorMessage += successAmount + " orders imported<br>";
				checkUpdate = true;
			}
		}
		
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	public String[] getBranches(Token t) throws SQLException, IOException, JSONException {
		String errorMessage = "";
		int successAmount = 0;
		JSONArray branches = null;
		String endpoint = "branches";
		basicAuth = setAuthorization(t.getAccessSecret(), t.getAccessToken());
		ArrayList<Relation> relations = new ArrayList<Relation>();
		
		try {
			branches = (JSONArray) getJsonResponse(endpoint, null);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		Relation relation = null;
		if (branches != null) {
			
			for (int i = 0; i < branches.length(); i++) {
				JSONObject branch = getBranchById(t, branches.getJSONObject(i).getString("id"));
				ArrayList<Address> addresses = new ArrayList<Address>();
				
				// check for mandatory field. Skip iteration if field is missing
				if (!branch.has("name") || !branch.has("id") || !branch.has("address")
						|| !branch.getJSONObject("address").has("street")
						|| !branch.getJSONObject("address").has("postcode")
						|| !branch.getJSONObject("address").has("city")) {
					continue;
				}
				
				String name = branch.getString("name");
				String debtor_number = branch.getString("id");
				String phone_number = branch.getString("phone");
				String email = branch.getString("email");
				String email_workorder;
				String street = branch.getJSONObject("address").getString("street");
				String house_number = branch.getJSONObject("address").getString("number");
				String postal_code = branch.getJSONObject("address").getString("postcode");
				String city = branch.getJSONObject("address").getString("city");
				String modified = branch.getString("modificationDate");
				
				Address mainAddress = new Address("", phone_number, email, street, house_number, postal_code, city,
						null, "main", 0);
				addresses.add(mainAddress);
				
				relation = new Relation(name, debtor_number, null, email, null, modified, null);
				relation.setAddresses(addresses);
				relations.add(relation);
			}
		}
		if (relations.size() > 0) {
			int success = (int) WorkOrderHandler.addData(t.getSoftwareToken(), relations, "relations",
					t.getSoftwareName());
			if (success > 0) {
				successAmount += success;
				errorMessage += successAmount + " relations imported<br>";
				checkUpdate = true;
			} else {
				errorMessage += "Something went wrong with relations<br>";
			}
			ObjectDAO.saveRelations(relations, t.getSoftwareToken());
		} else {
			errorMessage += "No relations for import<br>";
		}
		
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	public JSONObject getBranchById(Token t, String id) throws IOException {
		JSONObject branch = null;
		String endpoint = "branches/id/" + id;
		basicAuth = setAuthorization(t.getAccessSecret(), t.getAccessToken());
		
		try {
			branch = (JSONObject) getJsonResponse(endpoint, null);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return branch;
	}
	
	public String[] getPersons(Token t, int start) throws IOException, JSONException {
		String errorMessage = "";
		JSONArray persons = null;
		String endpoint = "persons?page_size=100&start=" + start;
		
		ArrayList<Relation> relations = new ArrayList<Relation>();
		
		try {
			persons = (JSONArray) getJsonResponse(endpoint, null);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		Address contact = null;
		if (persons != null) {
			for (int i = 0; i < persons.length(); i++) {
				ArrayList<Address> contacts = new ArrayList<Address>();
				JSONObject person = persons.getJSONObject(i);
				
				if (!person.has("branch") || !(person.get("branch") instanceof JSONObject)) {
					continue;
				}
				
				String cpn_debtor_nr = person.getJSONObject("branch").getString("id");
				String cpn_code = person.getString("id");
				String cpn_name = person.getString("firstName") + " " + person.getString("surName");
				String cpn_phone = person.getString("phoneNumber");
				String cpn_email = person.getString("email");
				String mobileNumber = person.getString("mobileNumber");
				
				if (cpn_phone.equals("") && mobileNumber.length() > 0) {
					cpn_phone = mobileNumber;
				}
				
				contact = new Address(cpn_name, cpn_phone, cpn_email, null, cpn_code, null, null, null, "contact", -1);
				contacts.add(contact);
				Relation rel = new Relation(null, cpn_debtor_nr, null, null, contacts, null, null);
				relations.add(rel);
			}
		} else {
			errorMessage += "No contacts found to synchronize.";
		}
		
		if (relations != null) {
			int contactSuccess = 0;
			contactSuccess = (int) WorkOrderHandler.addData(t.getSoftwareToken(), relations, "contactpersons",
					t.getSoftwareName());
			
			if (contactSuccess == 0) {
				errorMessage += "Something went wrong with contactpersons<br>";
			} else if (contactSuccess > 0) {
				errorMessage += "- " + contactSuccess + " contacts imported<br>";
				checkUpdate = true;
			}
		}
		
		if (persons.length() == 100) {
			return getPersons(t, start += 100);
		}
		
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	public String[] updateStatus(Token t, WorkOrder w, String action) throws JSONException, SQLException, IOException {
		String errorMessage = "";
		String endpoint = "incidents/id/";
		Settings set = ObjectDAO.getSettings(t.getSoftwareToken());
		JSONObject response = null;
		
		String statusOption = "";
		if (action.equals("Compleet")) {
			statusOption = set.getExportOffice();
			endpoint += w.getModified();
		} else if (action.equals("Sync")) {
			statusOption = set.getExportWerkbontype();
			endpoint += w.getId();
		}
		
		JSONObject processingStatus = new JSONObject();
		processingStatus.put("id", statusOption);
		JSONObject wrapper = new JSONObject();
		wrapper.put("processingStatus", processingStatus);
		
		try {
			response = (JSONObject) putJsonResponse(t, endpoint, wrapper.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		if (response != null) {
			checkUpdate = true;
		}
		
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	public String setAuthorization(String operatorName, String clientToken) {
		String userCredentials = operatorName + ":" + clientToken;
		return "Basic " + new String(new Base64().encode(userCredentials.getBytes()));
	}
	
}