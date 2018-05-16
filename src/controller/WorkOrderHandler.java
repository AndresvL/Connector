package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import DAO.ObjectDAO;
import object.trackjack.Location;
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

/**
 * The Universale Integration Adapter integrates with third-party invoice
 * systems and WorkOrderApp This application handles API requests and responses
 * and maps the data with the data in WorkOrderApp.
 *
 * @author Andres van Lummel
 * @version 1.0
 * @since 2017-07-19
 */
// In this class you'll find all the methodes that are used to communicate with
// WBA
public class WorkOrderHandler {
	private static String version = "8";
	// WorkOrder Api key
	// Set softwareToken for local use if null system.getenv() will be called
	final static String softwareToken = null;
	
	/**
	 * Returns a code that will be used to check if the token is valid.
	 * <p>
	 * This method sends a request to the WOA API with parameters: version,
	 * object, token and software_token. The object in this case is employees.
	 * 
	 * @param token
	 *            softwareToken of the current user
	 * @param softwareName
	 *            the name of the third-party application
	 * @return the status code of the JSON response
	 */
	public static int checkWorkOrderToken(String token, String softwareName) {
		String link = "https://www.werkbonapp.nl/openapi/" + version + "/employees/?token=" + token + "&software_token="
				+ softwareToken;
		if (softwareToken == null) {
			link = "https://www.werkbonapp.nl/openapi/" + version + "/employees/?token=" + token + "&software_token="
					+ System.getenv("SOFTWARETOKEN_" + softwareName.toUpperCase());
			link = link.trim();
			System.out.println("LINK + " + link);
		}
		int code = 0;
		String output = null;
		try {
			URL url = new URL(link);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/json");
			// Read inputsteam
			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			while ((output = br.readLine()) != null) {
				// Write inputstream to json
				JSONObject json = new JSONObject(output);
				code = json.getInt("code");
			}
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
		return code;
		
	}
	
	/**
	 * This method sets the workorder status from complete to handled if
	 * parameter status is true
	 * 
	 * @param id
	 *            row_id of the current workorder
	 * @param workorderNr
	 *            number of the current workorder
	 * @param status
	 *            true if workorder status needs to be changed to handled
	 * @param type
	 *            the API method/call type
	 * @param token
	 *            softwareToken of the current user
	 * @param softwareName
	 *            the name of the third-party application
	 */
	public static void setWorkorderStatus(String id, String workorderNr, Boolean status, String type, String token,
			String softwareName) {
		String link = "https://www.werkbonapp.nl/openapi/" + version + "/" + type + "/?token=" + token
				+ "&software_token=" + softwareToken + "&row_id=" + id + "&update_status=" + status;
		if (softwareToken == null) {
			link = "https://www.werkbonapp.nl/openapi/" + version + "/" + type + "/?token=" + token + "&software_token="
					+ System.getenv("SOFTWARETOKEN_" + softwareName.toUpperCase()) + "&row_id=" + id + "&update_status="
					+ status;
		}
		URL url;
		try {
			url = new URL(link);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/json");
			// Get inputsteam
			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			String output;
			// No need to print the output.
			while ((output = br.readLine()) != null) {
				// System.out.println("setWorkorderStatus " + output);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * This method sets status to 99 when workorder has errors
	 * 
	 * @param id
	 *            row_id of the current workorder
	 * @param workorderNr
	 *            number of the current workorder
	 * @param status
	 *            true if workorder status needs to be changed to handled
	 * @param type
	 *            the API method/call type
	 * @param token
	 *            softwareToken of the current user
	 * @param softwareName
	 *            the name of the third-party application
	 */
	public static void setWorkorderPlusStatus(String id, String workorderNr, String status, String type, String token,
			String softwareName) {
		String link = "https://www.werkbonapp.nl/openapi/" + version + "/" + type + "/?token=" + token
				+ "&software_token=" + softwareToken + "&row_id=" + id + "&workstatus=" + status;
		if (softwareToken == null) {
			link = "https://www.werkbonapp.nl/openapi/" + version + "/" + type + "/?token=" + token + "&software_token="
					+ System.getenv("SOFTWARETOKEN_" + softwareName.toUpperCase()) + "&row_id=" + id + "&workstatus="
					+ status;
		}
		System.out.println("LINK " + link);
		URL url;
		try {
			url = new URL(link);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/json");
			// Get inputsteam
			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			String output;
			// No need to print the output.
			while ((output = br.readLine()) != null) {
				System.out.println("setWorkorderStatus " + output);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * This method returns all hourtypes from the WOA API.
	 * 
	 * /** This method returns all hourtypes from the WOA API. \ Returns an
	 * hourtype object
	 * 
	 * @param token
	 *            softwareToken of the current user
	 * @param hourtype
	 *            the hourtype code
	 * @param softwareName
	 *            the name of the third-party application
	 * 
	 * @return an hourtype object
	 */
	public static HourType getHourTypes(String token, String hourtype, String softwareName) {
		String link = "https://www.werkbonapp.nl/openapi/" + version + "/hourtypes/?token=" + token + "&software_token="
				+ softwareToken;
		if (softwareToken == null) {
			link = "https://www.werkbonapp.nl/openapi/" + version + "/hourtypes/?token=" + token + "&software_token="
					+ System.getenv("SOFTWARETOKEN_" + softwareName.toUpperCase());
		}
		HourType h = null;
		try {
			URL url = new URL(link);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/json");
			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			String output = null;
			JSONObject json = null;
			while ((output = br.readLine()) != null) {
				json = new JSONObject(output);
			}
			br.close();
			JSONArray jsonArray = json.getJSONArray("response");
			
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject object = jsonArray.getJSONObject(i);
				if (object.getString("code").equals(hourtype)) {
					String name = object.getString("name");
					double salePrice = object.getDouble("sale_price");
					double costPrice = object.getDouble("cost_price");
					int costBooking = object.getInt("cost_booking");
					int saleBooking = object.getInt("sale_booking");
					h = new HourType(hourtype, name, costBooking, saleBooking, costPrice, salePrice, 1, null, null);
				}
			}
		} catch (IOException | JSONException e) {
			e.printStackTrace();
		}
		return h;
	}
	
	/**
	 * This method returns all types of work or payment methods from the WOA
	 * API. Returns a standaard arraylist of types of work and payment methods
	 * if the API call returns an empty array
	 * 
	 * @param token
	 *            softwareToken of the current user
	 * @param softwareName
	 *            the name of the third-party application
	 * @param type
	 *            the API method/call type
	 * @return an arraylist with the types of work or payment methods
	 */
	public static ArrayList<String> getTypeofwork(String token, String softwareName, String type, String language) {
		String link = "https://www.werkbonapp.nl/openapi/" + version + "/" + type + "/?token=" + token
				+ "&software_token=" + softwareToken;
		if (softwareToken == null) {
			link = "https://www.werkbonapp.nl/openapi/" + version + "/" + type + "/?token=" + token + "&software_token="
					+ System.getenv("SOFTWARETOKEN_" + softwareName.toUpperCase());
		}
		ArrayList<String> types = new ArrayList<String>();
		try {
			URL url = new URL(link);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/json");
			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			String output = null;
			JSONObject json = null;
			while ((output = br.readLine()) != null) {
				json = new JSONObject(output);
			}
			br.close();
			JSONArray jsonArray = json.getJSONArray("response");
			// Return ArrayList with worktype or paymentmethod
			switch (type) {
			case "worktypes":
				Boolean activeBoolean = false;
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject object = jsonArray.getJSONObject(i);
					int active = object.getInt("wrt_active");
					if (active > 0) {
						activeBoolean = true;
						String name = object.getString("wrt_name");
						types.add(name);
					}
				}
				if (jsonArray.length() == 0 || !activeBoolean) {
					switch (language) {
					case "NL":
						types.add("Installatie");
						types.add("Garantie");
						types.add("Levering");
						types.add("Onderhoud");
						types.add("Project");
						types.add("Regie");
						types.add("Reparatie");
						types.add("Service");
						types.add("Storing");
						types.add("Verkoop");
						types.add("Verhuur");
						break;
					case "PT":
						types.add("Instala��o");
						types.add("Garantia");
						types.add("Entrega");
						types.add("Manuten��o");
						types.add("Projeto");
						types.add("Diretor");
						types.add("Repara��o");
						types.add("Servi�o");
						types.add("Falha");
						types.add("Venda");
						types.add("Aluguer");
						break;
					}
				}
				break;
			case "paymentmethods":
				if (jsonArray.length() == 0) {
					switch (language) {
					case "NL":
						types.add("op rekening");
						types.add("niet van toepassing");
						types.add("contant voldaan");
						types.add("pin betaling");
						types.add("conform offerte");
						break;
					case "PT":
						types.add("Em conta");
						types.add("n�o aplic�vel");
						types.add("Pagamento em dinheiro");
						types.add("Multibanco");
						types.add("De acordo com proposta");
						break;
					}
					
				} else {
					for (int i = 0; i < jsonArray.length(); i++) {
						JSONObject object = jsonArray.getJSONObject(i);
						String name = object.getString("pmd_description");
						types.add(name);
					}
				}
				break;
			}
			
		} catch (IOException |
				
				JSONException e) {
			e.printStackTrace();
		}
		return types;
	}
	
	/**
	 * This method sends a request with Returns all WorkOrders with status
	 * complete.
	 * 
	 * @param token
	 *            softwareToken of the current user
	 * @param type
	 *            the API method/call type
	 * @param stat
	 *            workorder status (klaargezet, opgehaald, compleet or
	 *            afgehandeld)
	 * @param updateStatus
	 *            true if all workorder statusses need to be changed to
	 *            afgehandeld
	 * @param softwareName
	 *            the name of the third-party application
	 * @return an arraylist with all WorkOrders
	 * @see WorkOrder
	 */
	public static ArrayList<WorkOrder> getData(String token, String type, String stat, boolean updateStatus,
			String softwareName) {
		String projectNr, workOrderNr, workDate = null, customerEmailInvoice, customerEmail, customerDebtorNr, status,
				paymentMethod, creationDate, id, orderNr, workTime, workEndDate, workEndTime, externProjectNr,
				customerName, customerStreet, customerStreetNo, customerZIP, customerCity, customerContactPerson,
				customerPhone, customerRemark, customerNameInvoice, customerDebtorNrInvoice, customerStreetInvoice,
				customerStreetNoInvoice, customerZIPInvoice, customerContactPersonInvoice, customerPhoneInvoice,
				customerRemarkInvoice, typeOfWork, workDescription, beginTime, endTime, customerCityInvoice, pdfUrl,
				workStatus;
		String employeeNr = null, hourType = null, description = null, duration = null;
		String materialCode, materialNr, materialUnit, materialName;
		double materialPrice;
		
		// Request link with parameters version, type, token, softwareToken and
		// status
		String link = "https://www.werkbonapp.nl/openapi/" + version + "/" + type + "/?token=" + token
				+ "&software_token=" + softwareToken + "&status=" + stat + "&update_status=" + updateStatus;
		if (softwareToken == null) {
			link = "https://www.werkbonapp.nl/openapi/" + version + "/" + type + "/?token=" + token + "&software_token="
					+ System.getenv("SOFTWARETOKEN_" + softwareName.toUpperCase()) + "&status=" + stat
					+ "&update_status=" + updateStatus;
		}
		String output = null;
		ArrayList<WorkOrder> allData = null;
		try {
			URL url = new URL(link);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/json");
			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			while ((output = br.readLine()) != null) {
				WorkOrder w = null;
				JSONObject json = new JSONObject(output);
				// Check if response is successful
				if (json.getInt("code") == 200) {
					allData = new ArrayList<WorkOrder>();
					JSONArray array = json.getJSONArray("response");
					for (int i = 0; i < array.length(); i++) {
						JSONObject object = array.getJSONObject(i);
						// Only check for recent workorders(not archieved or
						// deleted)
						if (object.getInt("Archived") == 0) {
							
							id = object.getString("id");
							orderNr = object.getString("OrderNr");
							workOrderNr = object.getString("WorkorderNo");
							workDate = object.getString("WorkDate");
							workTime = object.getString("WorkTime");
							workEndDate = object.getString("WorkEndDate");
							workEndTime = object.getString("WorkEndTime");
							projectNr = object.getString("ProjectNr");
							externProjectNr = object.getString("ExternProjectNr");
							customerName = object.getString("CustomerName");
							customerDebtorNr = object.getString("CustomerDebtorNr");
							customerStreet = object.getString("CustomerStreet");
							customerStreetNo = object.getString("CustomerStreetNo");
							customerEmail = object.getString("CustomerEmail");
							customerZIP = object.getString("CustomerZIP");
							customerCity = object.getString("CustomerCity");
							customerContactPerson = object.getString("CustomerContactPerson");
							customerPhone = object.getString("CustomerPhone");
							customerRemark = object.getString("CustomerRemark");
							customerNameInvoice = object.getString("CustomerNameInvoice");
							customerDebtorNrInvoice = object.getString("CustomerDebtorNrInvoice");
							customerStreetInvoice = object.getString("CustomerStreetInvoice");
							customerStreetNoInvoice = object.getString("CustomerStreetNoInvoice");
							customerEmailInvoice = object.getString("CustomerEmailInvoice");
							customerZIPInvoice = object.getString("CustomerZIPInvoice");
							customerCityInvoice = object.getString("CustomerCityInvoice");
							customerContactPersonInvoice = object.getString("CustomerContactPersonInvoice");
							customerPhoneInvoice = object.getString("CustomerPhoneInvoice");
							customerRemarkInvoice = object.getString("CustomerRemarkInvoice");
							typeOfWork = object.getString("TypeOfWork");
							workDescription = object.getString("WorkDescription");
							paymentMethod = object.getString("PaymentMethod");
							creationDate = object.getString("CreationDate");
							employeeNr = object.getString("EmployeeNr");
							status = object.getString("status");
							creationDate = object.getString("CreationDate");
							pdfUrl = object.getString("PdfUrl");
							workStatus = object.getString("WorkStatus");
							ArrayList<Relation> allRelations = new ArrayList<Relation>();
							ArrayList<Address> invoiceAddress = new ArrayList<Address>();
							// id 1 is invoice
							Address invoice = new Address(customerContactPersonInvoice, customerPhoneInvoice,
									customerEmailInvoice, customerStreetInvoice, customerStreetNoInvoice,
									customerZIPInvoice, customerCityInvoice, customerRemarkInvoice, "invoice", 1);
							invoiceAddress.add(invoice);
							Relation customerRelationInvoice = new Relation(customerNameInvoice,
									customerDebtorNrInvoice, customerContactPersonInvoice, customerEmailInvoice,
									invoiceAddress, null, null);
							
							ArrayList<Address> postaleAddress = new ArrayList<Address>();
							// id 2 is postal
							Address postal = new Address(customerContactPerson, customerPhone, customerEmail,
									customerStreet, customerStreetNo, customerZIP, customerCity, customerRemark,
									"postal", 2);
							postaleAddress.add(postal);
							Relation customerRelationPostal = new Relation(customerName, customerDebtorNr,
									customerContactPerson, customerEmail, postaleAddress, null, null);
							// add addresses to Relation
							allRelations.add(customerRelationInvoice);
							allRelations.add(customerRelationPostal);
							// workperiods
							ArrayList<WorkPeriod> allWorkPeriods = new ArrayList<WorkPeriod>();
							JSONArray periods = object.getJSONArray("Workperiods");
							if (periods.length() > 0) {
								for (int j = 0; j < periods.length(); j++) {
									WorkPeriod work = null;
									JSONObject period = periods.getJSONObject(j);
									beginTime = period.getString("BeginTime");
									duration = period.getString("TotalTime");
									description = period.getString("WorkRemark");
									workDate = period.getString("WorkDate");
									endTime = period.getString("EndTime");
									employeeNr = period.getString("EmployeeNr");
									hourType = period.getString("HourType");
									work = new WorkPeriod(employeeNr, hourType, workDate, projectNr, description,
											duration, id, beginTime, endTime);
									allWorkPeriods.add(work);
								}
							}
							// materials
							ArrayList<Material> alleMaterials = new ArrayList<Material>();
							JSONArray materials = object.getJSONArray("Materials");
							for (int j = 0; j < materials.length(); j++) {
								JSONObject material = materials.getJSONObject(j);
								materialCode = material.optString("MaterialCode", "");
								materialNr = material.getString("MaterialNr");
								materialUnit = material.getString("MaterialUnit");
								materialName = material.getString("MaterialName");
								materialPrice = material.getDouble("MaterialPrice");
								// Get material from db
								Material sub = null;
								if (softwareName == "Twinfield") {
									sub = ObjectDAO.getMaterial(token, materialCode);
								}
								Material m = null;
								if (sub != null) {
									// Check if material has subCode
									if (sub.getSubCode() != null) {
										m = new Material(sub.getCode(), materialCode, materialUnit, materialName,
												materialPrice, materialNr, null, null);
									} else {
										m = new Material(materialCode, null, materialUnit, materialName, materialPrice,
												materialNr, null, null);
									}
								} else {
									m = new Material(materialCode, null, materialUnit, materialName, materialPrice,
											materialNr, null, null);
								}
								
								alleMaterials.add(m);
							}
							
							w = new WorkOrder(projectNr, workDate, customerEmailInvoice, customerEmail,
									customerDebtorNr, status, paymentMethod, alleMaterials, creationDate, id, orderNr,
									allWorkPeriods, allRelations, workTime, workEndDate, workEndTime, externProjectNr,
									typeOfWork, workDescription, workOrderNr, pdfUrl, workStatus, employeeNr);
							allData.add(w);
						}
					}
				}
			}
		} catch (IOException | JSONException | SQLException e) {
			e.printStackTrace();
		}
		return allData;
		
	}
	
	/**
	 * This method sends a JSONObject to the WOA webservice JSONObjects that can
	 * be sent are: Employees - Projects - Relations - Materials - Hourtypes -
	 * WorkOrders - WorkOrderStatus
	 * 
	 * @param token
	 *            softwareToken of the current user
	 * @param array
	 *            an ArrayList filled with information from one of the above
	 *            objects
	 * @param type
	 *            the API method/call type
	 * @param softwareName
	 *            the name of the third-party application
	 * @return an JSONObject with responseObject or an amount of workstatusses
	 *         added to WOA
	 * @throws IOException
	 *             IOException
	 */
	
	public static Object addData(String token, Object array, String type, String softwareName) throws IOException {
		String input = null;
		// set type of employeeExtended to employee
		switch (type) {
		case "employees":
			// Create JSONObject with Employee objects
			input = employeeInput(array, false) + "";
			break;
		case "employeesExtended":
			// Create JSONObject with EmployeeExtended objects
			input = employeeInput(array, true) + "";
			type = "employees";
			break;
		case "projects":
			// Create JSONObject with Project objects
			input = projectInput(array) + "";
			break;
		case "relations":
			// Create JSONObject with Relation objects
			input = relationInput(array) + "";
			break;
		case "contactpersons":
			// Create JSONObject with new relation object objects
			input = contactPersonInput(array) + "";
			break;
		case "addresses":
			// Create JSONObject with new relation object objects
			input = addressInput(array) + "";
			break;
		case "materials":
			// Create JSONObject with Material objects
			input = materialInput(array) + "";
			break;
		case "hourtypes":
			// Create JSONObject with Hourtype objects
			input = hourtypeInput(array) + "";
			break;
		case "PostWorkorders":
			// Create JSONObject with Workorder objects
			input = workorderInput(array) + "";
			break;
		case "PostWorkordersExtended":
			// Create JSONObject with WorkorderExtended objects
			input = workorderExtendedInput(array) + "";
			type = "PostWorkorders";
			break;
		case "workstatusses":
			// Array is JSONObject
			input = array + "";
			break;
		case "categories":
			// Array is JSONObject
			input = categoryInput(array) + "";
			break;
		case "material_categories":
			// Array is JSONObject
			input = materialCategoryInput(array) + "";
			break;
		case "objects":
			// Array is JSONObject
			input = objectInput(array) + "";
			type = "objects";
			break;
		case "locations":
			// Array is JSONObject
			input = locationsInput(array) + "";
			break;
		}
		
		Object amount = 0;
		
		// Empty JSONArrays and JSONObjects don't need to be sent.
		if ("".equals(input) || "[]".equals(input) || "{}".equals(input)) {
			return amount;
		}
		
		String link = "https://www.werkbonapp.nl/openapi/" + version + "/" + type + "/?token=" + token
				+ "&software_token=" + softwareToken;
		if (softwareToken == null) {
			link = "https://www.werkbonapp.nl/openapi/" + version + "/" + type + "/?token=" + token + "&software_token="
					+ System.getenv("SOFTWARETOKEN_" + softwareName.toUpperCase());
		}
		URL url = new URL(link);
		System.out.println("WBA LINK " + link);
		
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		
		// Send JSONObject to WOA webservice
		OutputStream os = conn.getOutputStream();
		os.write(input.getBytes("UTF-8"));
		os.flush();
		// get InputStream
		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
		String output;
		// Read Response
		while ((output = br.readLine()) != null) {
			System.out.println("OUTPUT: " + output + " - TYPE: " + type);
			try {
				JSONObject json = new JSONObject(output);
				if (!json.isNull("response")) {
					// Check if array exist
					if (json.optJSONArray("response") != null) {
						// Set amount with response JSONArray
						amount = json.getJSONArray("response");
					} else {
						// Set amount of workstatusses
						amount = json.getInt("response");
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		conn.disconnect();
		return amount;
		
	}
	
	/**
	 * This method creates a JSONObject with Employee data
	 * 
	 * @param obj
	 *            an ArrayList with Employee objects
	 * @return a JSONARRay with Employee JSONObjects
	 * @see Employee
	 */
	public static JSONArray employeeInput(Object obj, boolean extended) {
		JSONArray JSONArray = new JSONArray();
		JSONObject JSONObject = null;
		if (extended) {
			@SuppressWarnings("unchecked")
			ArrayList<EmployeeExtended> array = (ArrayList<EmployeeExtended>) obj;
			
			for (EmployeeExtended emp : array) {
				JSONObject = new JSONObject();
				try {
					JSONObject.put("firstname", emp.getFirstName());
					JSONObject.put("lastname", emp.getLastName());
					JSONObject.put("number", emp.getCode());
					JSONArray.put(JSONObject);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		} else {
			@SuppressWarnings("unchecked")
			ArrayList<Employee> array = (ArrayList<Employee>) obj;
			
			for (Employee emp : array) {
				JSONObject = new JSONObject();
				try {
					JSONObject.put("firstname", emp.getFirstName());
					JSONObject.put("lastname", emp.getLastName());
					JSONObject.put("number", emp.getCode());
					JSONArray.put(JSONObject);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		return JSONArray;
	}
	
	/**
	 * This method creates a JSONObject with Project data
	 * 
	 * @param obj
	 *            an ArrayList with Project objects
	 * @return a JSONARRay with Project JSONObjects
	 * @see Project
	 */
	public static JSONArray projectInput(Object obj) {
		JSONArray JSONArray = new JSONArray();
		JSONObject JSONObject = null;
		@SuppressWarnings("unchecked")
		ArrayList<Project> array = (ArrayList<Project>) obj;
		for (Project p : array) {
			JSONObject = new JSONObject();
			try {
				JSONObject.put("code", p.getCode());
				JSONObject.put("code_ext", p.getCodeExt());
				JSONObject.put("debtor_number", p.getDebtorNumber());
				JSONObject.put("status", p.getStatus());
				JSONObject.put("name", p.getName());
				JSONObject.put("description", p.getDescription());
				JSONObject.put("progress", p.getProgress());
				JSONObject.put("date_start", p.getDateStart());
				JSONObject.put("date_end", p.getDateEnd());
				JSONObject.put("active", p.getActive());
				JSONArray.put(JSONObject);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
		}
		return JSONArray;
	}
	
	/**
	 * This method creates a JSONObject with Relation data For every Address a
	 * new JSONObject will be created within the Relation JSONObject
	 * 
	 * @param obj
	 *            an ArrayList with Relation objects
	 * @return a JSONArray with Relation JSONObjects
	 * @see Relation
	 */
	public static JSONArray relationInput(Object obj) {
		JSONArray JSONArray = new JSONArray();
		JSONObject JSONObject = null;
		@SuppressWarnings("unchecked")
		ArrayList<Relation> array = (ArrayList<Relation>) obj;
		for (Relation r : array) {
			if (r != null) {
				// Create JSONObject for every other Address
				for (Address a : r.getAddressess()) {
					JSONObject = new JSONObject();
					if (!a.getType().equals("contact")) {
						try {
							JSONObject.put("name", r.getCompanyName());
							JSONObject.put("debtor_number", r.getDebtorNumber());
							JSONObject.put("contact", a.getName());
							JSONObject.put("phone_number", a.getPhoneNumber());
							JSONObject.put("email", a.getEmail());
							JSONObject.put("email_workorder", r.getEmailWorkorder());
							JSONObject.put("street", a.getStreet());
							JSONObject.put("house_number", a.getHouseNumber());
							JSONObject.put("postal_code", a.getPostalCode());
							JSONObject.put("city", a.getCity());
							JSONObject.put("remark", a.getRemark());
							JSONArray.put(JSONObject);
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		return JSONArray;
	}
	
	/**
	 * This method creates a JSONObject with Contactperson data from relation
	 * object.
	 * 
	 * @param obj
	 *            an ArrayList with Relation objects
	 * @return a JSONArray with Contactperson JSONObjects
	 * @see Relation
	 */
	public static JSONArray contactPersonInput(Object obj) {
		JSONArray JSONArray = new JSONArray();
		JSONObject JSONObject = null;
		@SuppressWarnings("unchecked")
		ArrayList<Relation> array = (ArrayList<Relation>) obj;
		for (Relation r : array) {
			// Create JSONObject for every other Address
			if (r != null) {
				for (Address a : r.getAddressess()) {
					if (a.getType().equals("contact") && (!a.getName().equals(""))) {
						JSONObject = new JSONObject();
						try {
							if (a.getAddressId() == -1) {
								JSONObject.put("cpn_code", a.getHouseNumber());
							} else {
								JSONObject.put("cpn_code", a.getAddressId());
							}
							JSONObject.put("cpn_debtor_nr", r.getDebtorNumber());
							JSONObject.put("cpn_name", a.getName());
							JSONObject.put("cpn_phone", a.getPhoneNumber());
							JSONObject.put("cpn_email", a.getEmail());
							JSONArray.put(JSONObject);
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		System.out.println("JSONArrayContacts " + JSONArray);
		return JSONArray;
	}
	
	/**
	 * This method creates a JSONObject with Address data from relation object.
	 * 
	 * @param obj
	 *            an ArrayList with Relation objects
	 * @return a JSONArray with Address JSONObjects
	 * @see Relation
	 */
	public static JSONArray addressInput(Object obj) {
		JSONArray JSONArray = new JSONArray();
		JSONObject JSONObject = null;
		@SuppressWarnings("unchecked")
		ArrayList<Relation> array = (ArrayList<Relation>) obj;
		for (Relation r : array) {
			// Create JSONObject for every other Address
			if (r != null) {
				for (Address a : r.getAddressess()) {
					if (a.getType().equals("contact") && (!a.getName().equals(""))) {
						JSONObject = new JSONObject();
						try {
							JSONObject.put("adr_debtor_nr", r.getDebtorNumber());
							JSONObject.put("adr_code", a.getAddressId());
							JSONObject.put("adr_cpn_code", a.getAddressId());
							JSONObject.put("adr_line_1", a.getStreet());
							JSONObject.put("adr_zip", a.getPostalCode());
							JSONObject.put("adr_city", a.getCity());
							JSONObject.put("adr_remark", a.getRemark());
							JSONArray.put(JSONObject);
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		System.out.println("JSONArrayAddresses " + JSONArray);
		return JSONArray;
	}
	
	/**
	 * This method creates a JSONObject with Material data If a material
	 * contains a sub-article, the sub-materialCode will be used as materialCode
	 * 
	 * @param obj
	 *            an ArrayList with Material objects
	 * @return a JSONARRay with Material JSONObjects
	 * @see Material
	 */
	public static JSONArray materialInput(Object obj) {
		JSONArray JSONArray = new JSONArray();
		JSONObject JSONObject = null;
		@SuppressWarnings("unchecked")
		ArrayList<Material> array = (ArrayList<Material>) obj;
		for (Material m : array) {
			JSONObject = new JSONObject();
			String code = null;
			// Set code with subCode if subCode != null
			if (m != null) {
				if (m.getSubCode() != null && !m.getSubCode().equals("")) {
					code = m.getSubCode();
				} else {
					code = m.getCode();
				}
				
				try {
					JSONObject.put("code", code);
					JSONObject.put("description", m.getDescription());
					JSONObject.put("price", m.getPrice());
					JSONObject.put("unit", m.getUnit());
					JSONArray.put(JSONObject);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		return JSONArray;
	}
	
	/**
	 * This method creates a JSONObject with material category data
	 * 
	 * @param obj
	 *            an ArrayList with Category objects
	 * @return a JSONARRay with Category JSONObjects
	 * @see MaterialCategory
	 */
	public static JSONArray categoryInput(Object obj) {
		JSONArray JSONArray = new JSONArray();
		JSONObject JSONObject = null;
		@SuppressWarnings("unchecked")
		ArrayList<MaterialCategory> array = (ArrayList<MaterialCategory>) obj;
		for (MaterialCategory c : array) {
			JSONObject = new JSONObject();
			try {
				JSONObject.put("cat_code", c.getCode());
				JSONObject.put("cat_name", c.getNaam());
				JSONObject.put("cat_display", c.getActive());
				JSONArray.put(JSONObject);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return JSONArray;
	}
	
	/**
	 * This method creates a JSONObject with material category mapping data
	 * 
	 * @param obj
	 *            an ArrayList with materialcode and categorycode
	 * @return a JSONARRay with materialcode and categorycode
	 */
	public static JSONArray materialCategoryInput(Object obj) {
		JSONArray JSONArray = new JSONArray();
		JSONObject JSONObject = null;
		@SuppressWarnings("unchecked")
		ArrayList<Material> array = (ArrayList<Material>) obj;
		for (Material m : array) {
			JSONObject = new JSONObject();
			try {
				JSONObject.put("mct_mat_code", m.getCode());
				JSONObject.put("mct_cat_code", m.getId());
				JSONArray.put(JSONObject);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return JSONArray;
	}
	
	/**
	 * This method creates a JSONObject with Hourtype data
	 * 
	 * @param obj
	 *            an ArrayList with Hourtype objects
	 * @return a JSONARRay with Hourtype JSONObjects
	 * @see HourType
	 */
	public static JSONArray hourtypeInput(Object obj) {
		JSONArray JSONArray = new JSONArray();
		JSONObject JSONObject = null;
		@SuppressWarnings("unchecked")
		ArrayList<HourType> array = (ArrayList<HourType>) obj;
		for (HourType h : array) {
			JSONObject = new JSONObject();
			try {
				JSONObject.put("code", h.getCode());
				JSONObject.put("name", h.getName());
				JSONObject.put("cost_booking", h.getCostBooking());
				JSONObject.put("sale_booking", h.getSaleBooking());
				JSONObject.put("cost_price", h.getCostPrice());
				JSONObject.put("sale_price", h.getSalePrice());
				JSONObject.put("active", h.getActive());
				JSONArray.put(JSONObject);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return JSONArray;
	}
	
	/**
	 * This method creates a JSONObject with WorkOrder data
	 * 
	 * @param obj
	 *            an ArrayList with WorkOrder objects
	 * @return a JSONARRay with WorkOrder JSONObjects
	 * @see WorkOrder
	 */
	public static JSONArray workorderInput(Object obj) {
		JSONArray JSONArray = new JSONArray();
		JSONArray JSONArrayMaterials = null;
		JSONObject JSONObject = null;
		JSONObject JSONObjecMaterial = null;
		@SuppressWarnings("unchecked")
		ArrayList<WorkOrder> allWorkorders = (ArrayList<WorkOrder>) obj;
		for (WorkOrder w : allWorkorders) {
			Relation r = w.getRelations().get(0);
			JSONObject = new JSONObject();
			try {
				JSONObject.put("OrderNr", w.getWorkorderNr());
				JSONObject.put("WorkorderNo", w.getWorkorderNr());
				String projectNr = "";
				if (w.getProjectNr() != null) {
					projectNr = w.getProjectNr();
				}
				JSONObject.put("ProjectNr", projectNr);
				JSONObject.put("ExternProjectNr", w.getExternProjectNr());
				Address postal = null;
				Address invoice = null;
				if (r.getAddressess().size() > 1) {
					// Postal and invoice address
					postal = r.getAddressess().get(1);
					invoice = r.getAddressess().get(0);
				} else {
					// Invoice
					invoice = r.getAddressess().get(0);
				}
				if (postal != null) {
					JSONObject.put("CustomerName", postal.getName());
					JSONObject.put("CustomerStreet", postal.getStreet());
					JSONObject.put("CustomerZIP", postal.getPostalCode());
					JSONObject.put("CustomerCity", postal.getCity());
					JSONObject.put("CustomerPhone", postal.getPhoneNumber());
					JSONObject.put("CustomerRemark", postal.getRemark());
				} else {
					JSONObject.put("CustomerName", invoice.getName());
					JSONObject.put("CustomerStreet", invoice.getStreet());
					JSONObject.put("CustomerZIP", invoice.getPostalCode());
					JSONObject.put("CustomerCity", invoice.getCity());
					JSONObject.put("CustomerPhone", invoice.getPhoneNumber());
					JSONObject.put("CustomerRemark", invoice.getRemark());
				}
				
				JSONObject.put("CustomerDebtorNr", w.getCustomerDebtorNr());
				JSONObject.put("CustomerEmail", w.getCustomerEmail());
				JSONObject.put("CustomerContactPerson", r.getContact());
				JSONObject.put("CustomerNameInvoice", r.getCompanyName());
				JSONObject.put("CustomerDebtorNrInvoice", r.getDebtorNumber());
				JSONObject.put("CustomerStreetInvoice", invoice.getStreet());
				JSONObject.put("CustomerEmailInvoice", w.getCustomerEmail());
				JSONObject.put("CustomerZIPInvoice", invoice.getPostalCode());
				JSONObject.put("CustomerCityInvoice", invoice.getCity());
				JSONObject.put("CustomerContactPersonInvoice", r.getContact());
				JSONObject.put("CustomerPhoneInvoice", invoice.getPhoneNumber());
				JSONObject.put("CustomerRemarkInvoice", invoice.getRemark());
				
				JSONObject.put("EmployeeNr", w.getEmployeeNr());
				JSONObject.put("TypeOfWork", w.getTypeOfWork());
				JSONObject.put("WorkDescription", w.getWorkDescription());
				JSONObject.put("PaymentMethod", w.getPaymentMethod());
				JSONObject.put("WorkDate", w.getWorkDate());
				JSONObject.put("WorkTime", w.getWorkTime());
				JSONArrayMaterials = new JSONArray();
				if (w.getMaterials() != null) {
					for (Material m : w.getMaterials()) {
						JSONObjecMaterial = new JSONObject();
						JSONObjecMaterial.put("MaterialCode", m.getCode());
						JSONObjecMaterial.put("MaterialNr", m.getQuantity());
						JSONObjecMaterial.put("MaterialPrice", m.getPrice());
						JSONObjecMaterial.put("MaterialName", m.getDescription());
						JSONObjecMaterial.put("MaterialUnit", m.getUnit());
						JSONArrayMaterials.put(JSONObjecMaterial);
					}
					JSONObject.put("Materials", JSONArrayMaterials);
				}
				JSONArray.put(JSONObject);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		System.out.println("JSONARRAY " + JSONArray);
		return JSONArray;
	}
	
	/**
	 * This method creates a JSONObject with WorkOrder data
	 * 
	 * @param obj
	 *            an ArrayList with WorkOrder objects
	 * @return a JSONArray with WorkOrder JSONObjects
	 * @see WorkOrder
	 */
	public static JSONArray workorderExtendedInput(Object obj) {
		JSONArray JSONArray = new JSONArray();
		JSONArray JSONArrayMaterials = null;
		JSONArray JSONArrayWorkperiods = null;
		JSONArray JSONArrayObjects = null;
		JSONObject JSONObject = null;
		JSONObject JSONObjectMaterial = null;
		JSONObject JSONObjectWorkperiods = null;
		JSONObject JSONObjectObject = null;
		@SuppressWarnings("unchecked")
		ArrayList<WorkOrderExtended> allWorkorders = (ArrayList<WorkOrderExtended>) obj;
		for (WorkOrderExtended w : allWorkorders) {
			Relation r = w.getRelations().get(0);
			JSONObject = new JSONObject();
			try {
				JSONObject.put("WorkorderNo", w.getWorkorderNr());
				String projectNr = "";
				if (w.getProjectNr() != null) {
					projectNr = w.getProjectNr();
				}
				JSONObject.put("ProjectNr", projectNr);
				JSONObject.put("ExternProjectNr", w.getExternProjectNr());
				Address postal = null;
				Address invoice = null;
				if (r.getAddressess().size() > 1) {
					// Postal and invoice address
					postal = r.getAddressess().get(1);
					invoice = r.getAddressess().get(0);
				} else {
					// Invoice
					invoice = r.getAddressess().get(0);
				}
				if (postal != null) {
					JSONObject.put("CustomerName", postal.getName());
					JSONObject.put("CustomerStreet", postal.getStreet());
					JSONObject.put("CustomerZIP", postal.getPostalCode());
					JSONObject.put("CustomerCity", postal.getCity());
					JSONObject.put("CustomerPhone", postal.getPhoneNumber());
					JSONObject.put("CustomerRemark", postal.getRemark());
				} else {
					JSONObject.put("CustomerName", invoice.getName());
					JSONObject.put("CustomerStreet", invoice.getStreet());
					JSONObject.put("CustomerZIP", invoice.getPostalCode());
					JSONObject.put("CustomerCity", invoice.getCity());
					JSONObject.put("CustomerPhone", invoice.getPhoneNumber());
					JSONObject.put("CustomerRemark", invoice.getRemark());
				}
				
				JSONObject.put("CustomerDebtorNr", w.getCustomerDebtorNr());
				JSONObject.put("CustomerEmail", w.getCustomerEmail());
				JSONObject.put("CustomerContactPerson", r.getContact());
				JSONObject.put("CustomerNameInvoice", r.getCompanyName());
				JSONObject.put("CustomerDebtorNrInvoice", r.getDebtorNumber());
				JSONObject.put("CustomerStreetInvoice", invoice.getStreet());
				JSONObject.put("CustomerEmailInvoice", w.getCustomerEmail());
				JSONObject.put("CustomerZIPInvoice", invoice.getPostalCode());
				JSONObject.put("CustomerCityInvoice", invoice.getCity());
				JSONObject.put("CustomerContactPersonInvoice", r.getContact());
				JSONObject.put("CustomerPhoneInvoice", invoice.getPhoneNumber());
				JSONObject.put("CustomerRemarkInvoice", invoice.getRemark());
				
				JSONObject.put("EmployeeNr", w.getEmployeeNr());
				JSONObject.put("TypeOfWork", w.getTypeOfWork());
				JSONObject.put("WorkDescription", w.getWorkDescription());
				JSONObject.put("PaymentMethod", w.getPaymentMethod());
				JSONObject.put("WorkDate", w.getWorkDate());
				JSONObject.put("WorkTime", w.getWorkTime());
				
				if (w.getMaterials() != null) {
					JSONArrayMaterials = new JSONArray();
					for (Material m : w.getMaterials()) {
						JSONObjectMaterial = new JSONObject();
						JSONObjectMaterial.put("MaterialCode", m.getCode());
						JSONObjectMaterial.put("MaterialNr", m.getQuantity());
						JSONObjectMaterial.put("MaterialPrice", m.getPrice());
						JSONObjectMaterial.put("MaterialName", m.getDescription());
						JSONObjectMaterial.put("MaterialUnit", m.getUnit());
						JSONArrayMaterials.put(JSONObjectMaterial);
					}
					JSONObject.put("Materials", JSONArrayMaterials);
				}
				
				if (w.getWorkPeriods() != null) {
					JSONArrayWorkperiods = new JSONArray();
					for (WorkPeriod period : w.getWorkPeriods()) {
						JSONObjectWorkperiods = new JSONObject();
						JSONObjectWorkperiods.put("BeginTime", period.getBeginTime());
						JSONObjectWorkperiods.put("EndTime", period.getEndTime());
						JSONObjectWorkperiods.put("WorkDate", period.getWorkDate());
						if (!period.getEmployeeNr().equals("leeg")) {
							JSONObjectWorkperiods.put("EmployeeNr", period.getEmployeeNr());
						}
						JSONObjectWorkperiods.put("HourType", period.getHourType());
						JSONObjectWorkperiods.put("TotalTime", period.getDuration());
						JSONArrayWorkperiods.put(JSONObjectWorkperiods);
					}
					JSONObject.put("Workperiods", JSONArrayWorkperiods);
					
				}
				if (w.getObjects() != null) {
					JSONArrayObjects = new JSONArray();
					for (WOAObject o : w.getObjects()) {
						JSONArrayObjects.put(o.getObjCode());
					}
					JSONObject.put("WorkObjects", JSONArrayObjects);
				}
				JSONArray.put(JSONObject);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		System.out.println("JSONArray " + JSONArray);
		return JSONArray;
	}
	
	/**
	 * This method creates a JSONObject with WorkOrderApp object data
	 * 
	 * @param obj
	 *            an ArrayList with WorkOrderApp object objects
	 * @return a JSONARRay with WorkOrderApp object JSONObjects
	 * @see WOAObject
	 */
	public static JSONArray objectInput(Object obj) {
		JSONArray JSONArray = new JSONArray();
		JSONObject JSONObject = null;
		@SuppressWarnings("unchecked")
		ArrayList<WOAObject> array = (ArrayList<WOAObject>) obj;
		for (WOAObject o : array) {
			JSONObject = new JSONObject();
			try {
				JSONObject.put("obj_code", o.getObjCode());
				JSONObject.put("obj_debiteur_nummer", o.getObjDebiteurNummer());
				JSONObject.put("obj_description", o.getObjDescription());
				JSONArray.put(JSONObject);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
		}
		return JSONArray;
	}
	
	/**
	 * This method creates a JSONObject with WorkOrderApp location data
	 * 
	 * @param obj
	 *            an ArrayList with WorkOrderApp location objects
	 * @return a JSONARRay with WorkOrderApp location JSONObjects
	 * @see Location
	 */
	public static JSONArray locationsInput(Object obj) {
		JSONArray JSONArray = new JSONArray();
		JSONObject JSONObject = null;
		@SuppressWarnings("unchecked")
		ArrayList<Location> array = (ArrayList<Location>) obj;
		for (Location o : array) {
			JSONObject = new JSONObject();
			try {
				JSONObject.put("lat", o.getLatitude());
				JSONObject.put("long", o.getLongitude());
				JSONObject.put("identifier", o.getId());
				JSONObject.put("type", o.getType());
				JSONArray.put(JSONObject);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
		}
		System.out.println("JSONArray locations" + JSONArray);
		return JSONArray;
	}
}