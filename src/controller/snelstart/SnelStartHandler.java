package controller.snelstart;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
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
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import DAO.ObjectDAO;
import controller.WorkOrderHandler;
import object.Settings;
import object.Token;
import object.workorder.Address;
import object.workorder.HourType;
import object.workorder.Material;
import object.workorder.Relation;
import object.workorder.WorkOrder;
import object.workorder.WorkPeriod;

public class SnelStartHandler {
	final String softwareName = "SnelStart_Online";
	
	private static String oauthHost = System.getenv("SNELSTART_ONLINE_OAUTH_HOST");
	private static String apiHost = System.getenv("SNELSTART_ONLINE_API_HOST");
	private static String secret = System.getenv("SNELSTART_ONLINE_PRIMARY_KEY");
	private final static Logger logger = Logger.getLogger(SnelStartHandler.class.getName());
	private int newRelation = 0, newMaterial = 0;
	
	public String getAccessToken(String base64Key) {
		String token = null;
		JSONObject json = null;
		try {
			json = sendOAuthRequest(base64Key);
			token = json.optString("access_token", null);
			System.out.println("TOKENREQUEST " + json);
		} catch (Exception e) {
			token = null;
		}
		return token;
	}
	
	public boolean checkAccessToken(String accessToken, String softwareToken) throws IOException {
		Object obj = null;
		boolean token = false;
		String path = "/v1/relaties";
		try {
			obj = sendGetRequest(accessToken, path, true, softwareToken);
			if (obj != null) {
				token = (boolean) obj;
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return token;
	}
	
	public JSONObject sendOAuthRequest(String clientkey) throws IOException, JSONException {
		String jsonString;
		String parameters = null;
		parameters = "grant_type=clientkey&clientkey=" + clientkey;
		byte[] postData = parameters.getBytes();
		int postDataLength = postData.length;
		// Set the OAUTH connection
		URL url = new URL(oauthHost);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setInstanceFollowRedirects(false);
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		conn.setRequestProperty("charset", "utf-8");
		conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
		conn.setUseCaches(true);
		
		// Send request to SnelStart
		try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
			wr.write(postData);
		}
		if (conn.getResponseCode() > 200 && conn.getResponseCode() < 405) {
			System.out.println("Response message " + conn.getResponseMessage());
			BufferedReader br = new BufferedReader(
					new InputStreamReader((conn.getErrorStream()), StandardCharsets.UTF_8));
			JSONObject json = null;
			while ((jsonString = br.readLine()) != null) {
				json = new JSONObject(jsonString);
				System.out.println("Response body " + json);
			}
			return json;
		}
		BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream()), StandardCharsets.UTF_8));
		JSONObject json = null;
		while ((jsonString = br.readLine()) != null) {
			json = new JSONObject(jsonString);
		}
		return json;
	}
	
	public static Object sendGetRequest(String accessToken, String path, boolean checkToken, String softwareToken)
			throws IOException, JSONException {
		String jsonString;
		JSONArray array = null;
		URL url = new URL(apiHost + path);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setInstanceFollowRedirects(false);
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Authorization", "Bearer " + accessToken);
		conn.setRequestProperty("Ocp-Apim-Subscription-Key", secret);
		conn.setRequestProperty("charset", "utf-8");
		conn.setUseCaches(true);
		System.out.println("Authorization: Bearer " + accessToken);
		System.out.println("Ocp-Apim-Subscription-Key: " + secret);
		System.out.println("conn.getResponseCode() " + conn.getResponseCode());
		System.out.println("conn.getResponseMessage() " + conn.getResponseMessage());
		BufferedReader br = null;
		if (checkToken) {
			if (conn.getResponseCode() > 201) {
				br = new BufferedReader(new InputStreamReader((conn.getErrorStream()), StandardCharsets.UTF_8));
				while ((jsonString = br.readLine()) != null) {
					System.out.println("JSONSTRING Error " + jsonString);
				}
				return false;
			} else {
				br = new BufferedReader(new InputStreamReader((conn.getInputStream()), StandardCharsets.UTF_8));
				while ((jsonString = br.readLine()) != null) {
					array = new JSONArray(jsonString);
					System.out.println("JSONSTRING " + jsonString);
				}
				return true;
			}
		} else {
			br = new BufferedReader(new InputStreamReader((conn.getInputStream()), StandardCharsets.UTF_8));
			while ((jsonString = br.readLine()) != null) {
				array = new JSONArray(jsonString);
			}
		}
		return array;
	}
	
	public static Object sendPostRequest(String accessToken, String path, String data)
			throws IOException, JSONException {
		String jsonString;
		JSONObject array = null;
		// Data to bytes
		byte[] postData = data.getBytes(StandardCharsets.UTF_8);
		URL url = new URL(apiHost + path);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setDoInput(true);
		conn.setInstanceFollowRedirects(false);
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Authorization", "Bearer " + accessToken);
		conn.setRequestProperty("Ocp-Apim-Subscription-Key", secret);
		conn.setRequestProperty("charset", "utf-8");
		conn.setUseCaches(true);
		try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
			wr.write(postData);
		}
		
		BufferedReader br = null;
		Boolean success = false;
		if (conn.getResponseCode() > 201) {
			br = new BufferedReader(new InputStreamReader((conn.getErrorStream()), StandardCharsets.UTF_8));
		} else {
			br = new BufferedReader(new InputStreamReader((conn.getInputStream()), StandardCharsets.UTF_8));
			success = true;
		}
		
		while ((jsonString = br.readLine()) != null) {
			array = new JSONObject();
			System.out.println("JSONSTRING " + jsonString);
			if (jsonString.startsWith("Het ingelogde account")) {
				array.put("error", jsonString);
			} else if (success) {
				array = new JSONObject(jsonString);
			} else {
				String errorString = "";
				
				try {
					JSONArray ar = new JSONArray(jsonString);
					for (int i = 0; i < ar.length(); i++) {
						errorString += ar.getJSONObject(i).getString("message") + "\n";
						System.out.println("errorString POSTREQUEST " + errorString);
						array.put("error", errorString);
					}
				} catch (Exception e) {
					array = new JSONObject(jsonString);
					// e.printStackTrace();
				}
				
			}
		}
		return array;
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
		Format formatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
		String s = formatter.format(date);
		return s;
	}
	
	public String convertDate(String string) {
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		Date date = null;
		try {
			// String to date
			date = format.parse(string);
			// Create Calender to edit time
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			cal.add(Calendar.DAY_OF_MONTH, 30);
			date = cal.getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		// Date to String
		Format formatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
		String s = formatter.format(date);
		return s;
	}
	
	public String getCurrentDate(String date) {
		String timestamp;
		ZonedDateTime za = ZonedDateTime.now(ZoneId.of("Europe/Paris"));
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		if (date != null) {
			timestamp = date;
		} else {
			timestamp = za.format(formatter);
		}
		return timestamp;
	}
	
	// Producten
	public String[] getMaterials(Token t, String date, int skip, int successAmount, ArrayList<Material> mat)
			throws Exception {
		boolean checkUpdate = false;
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		String path = "/v1/artikelen";
		String errorMessage = "";
		ArrayList<Material> materials = new ArrayList<Material>();
		Boolean hasContent = ObjectDAO.hasContent(t.getSoftwareToken(), "materials");
		if (date != null && hasContent) {
			// yyyy-MM-dd hh:mm:ss
			path += "?$filter=" + URLEncoder.encode("ModifiedOn gt datetime", "utf-8") + "'" + getDateMinHour(date)
					+ "'" + URLEncoder.encode(" and IsNonActief eq false", "utf-8") + "&$skip=" + skip + "&$top=500";
		} else {
			path += "?$filter=" + URLEncoder.encode("IsNonActief eq false", "utf-8") + "&$skip=" + skip + "&$top=500";
		}
		System.out.println("PATH " + path);
		jsonArray = (JSONArray) sendGetRequest(t.getAccessToken(), path, false, t.getSoftwareToken());
		logger.info("Materials response " + jsonArray);
		if (jsonArray != null) {
			if (mat == null) {
				mat = new ArrayList<Material>();
			}
			for (int i = 0; i < jsonArray.length(); i++) {
				jsonObj = jsonArray.getJSONObject(i);
				String id = jsonObj.getString("id");
				String modified = jsonObj.getString("modifiedOn");
				String productCode = jsonObj.getString("artikelcode");
				String description = jsonObj.getString("omschrijving");
				Double salePrice = jsonObj.getDouble("verkoopprijs");
				Material m = new Material(productCode, null, "", description, salePrice, null, modified, id);
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
					// ObjectDAO.saveMaterials(materials, t.getSoftwareToken());
				}
				return getMaterials(t, date, skip += 500, successAmount, mat);
				
			} else {
				if (success > 0) {
					successAmount += success;
					System.out.println("MATERIALS SIZE" + mat.size());
					ObjectDAO.saveMaterials(mat, t.getSoftwareToken());
					errorMessage += successAmount + " materials imported<br>";
					checkUpdate = true;
				} else {
					errorMessage += "Something went wrong with materials<br>";
				}
			}
			
		} else {
			if (successAmount > 0) {
				System.out.println("MATERIALS SIZE " + mat.size());
				ObjectDAO.saveMaterials(mat, t.getSoftwareToken());
				errorMessage += successAmount + " materials imported<br>";
				checkUpdate = true;
			} else {
				errorMessage += "No materials for import<br>";
			}
			
		}
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	// Debiteuren
	public String[] getRelations(Token t, String date, int skip, int successAmount, ArrayList<Relation> rel)
			throws Exception {
		boolean checkUpdate = false;
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		String path = "/v1/relaties";
		String errorMessage = "";
		ArrayList<Relation> relations = new ArrayList<Relation>();
		Boolean hasContent = ObjectDAO.hasContent(t.getSoftwareToken(), "relations");
		if (date != null && hasContent) {
			// yyyy-MM-dd hh:mm:ss
			path += "?$filter=" + URLEncoder.encode("ModifiedOn gt datetime", "utf-8") + "'" + getDateMinHour(date)
					+ "'" + URLEncoder.encode(" and Nonactief eq false and Relatiesoort/any(r:r eq 'Klant')", "utf-8")
					+ "&$skip=" + skip + "&$top=500";
		} else {
			path += "?$filter=" + URLEncoder.encode("Nonactief eq false and Relatiesoort/any(r:r eq 'Klant')", "utf-8")
					+ "&$skip=" + skip + "&$top=500";
		}
		System.out.println("PATH " + path);
		jsonArray = (JSONArray) sendGetRequest(t.getAccessToken(), path, false, t.getSoftwareToken());
		logger.info("Relation response " + jsonArray);
		System.out.println("RELATION LENGTH " + jsonArray.length());
		if (jsonArray != null) {
			if (rel == null) {
				rel = new ArrayList<Relation>();
			}
			for (int i = 0; i < jsonArray.length(); i++) {
				jsonObj = jsonArray.getJSONObject(i);
				JSONArray tempArray = jsonObj.getJSONArray("relatiesoort");
				String tempJson = (String) tempArray.get(0);
				String id = jsonObj.getString("id");
				String companyName = jsonObj.optString("naam", "<empty>");
				String debtorNumber = jsonObj.getInt("relatiecode") + "";
				String modified = jsonObj.optString("modifiedOn");
				
				ArrayList<Address> addresses = new ArrayList<Address>();
				// Invoice
				JSONObject invoiceAddress = jsonObj.getJSONObject("vestigingsAdres");
				String invoiceContact = invoiceAddress.optString("contactpersoon");
				String invoiceStreet = invoiceAddress.optString("straat");
				String invoicePostcode = invoiceAddress.optString("postcode");
				String invoiceCity = invoiceAddress.optString("plaats");
				
				// Delivery
				JSONObject deliveryAddress = jsonObj.getJSONObject("correspondentieAdres");
				String deliveryContact = deliveryAddress.optString("contactpersoon");
				String deliveryStreet = deliveryAddress.optString("straat");
				String deliveryPostcode = deliveryAddress.optString("postcode");
				String deliveryCity = deliveryAddress.optString("plaats");
				
				String phoneNr = jsonObj.optString("telefoon");
				String description = jsonObj.optString("memo");
				String mobileNr = jsonObj.optString("mobieleTelefoon");
				if (phoneNr == null) {
					phoneNr = mobileNr;
				}
				String email = jsonObj.optString("email");
				JSONObject factuurEmail = jsonObj.getJSONObject("factuurEmailVersturen");
				String invoiceEmail = factuurEmail.optString("email");
				
				Address invoice = new Address(invoiceContact, phoneNr, email, invoiceStreet, "", invoicePostcode,
						invoiceCity, description, "invoice", 1);
				Address delivery = new Address(deliveryContact, phoneNr, email, deliveryStreet, "", deliveryPostcode,
						deliveryCity, description, "postal", 2);
				addresses.add(invoice);
				
				Relation r = new Relation(companyName, debtorNumber, invoiceContact, invoiceEmail, addresses, modified,
						id);
				relations.add(r);
				rel.add(r);
			}
		}
		if (!relations.isEmpty()) {
			int success = (int) WorkOrderHandler.addData(t.getSoftwareToken(), relations, "relations", softwareName);
			
			if (relations.size() == 500) {
				if (success > 0) {
					successAmount += success;
					// ObjectDAO.saveRelations(relations, t.getSoftwareToken());
				} else {
					errorMessage += "Something went wrong with relations<br>";
				}
				return getRelations(t, date, skip += 500, successAmount, rel);
			} else {
				if (success > 0) {
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
	
	// Create verkoopOrders
	public String[] setInvoice(Token t, Settings set, String date)
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
			int statusCode = 0;
			if (w.getWorkStatus() != null && !w.getWorkStatus().equals("")) {
				statusCode = Integer.parseInt(w.getWorkStatus());
			}
			if (statusCode >= 1 && statusCode != 99) {
				exportAmount++;
				// Create JSON Object for salesinvoice post
				JSONObject = invoiceJSON(w, t, set, statusCode);
				String error = (String) JSONObject.opt("Error");
				if (error != null) {
					errorDetails += error;
					errorAmount++;
					WorkOrderHandler.setWorkorderPlusStatus(w.getId(), w.getWorkorderNr(), "99", "UpdateWorkorder",
							t.getSoftwareToken(), softwareName);
				} else {
					logger.info("REQUEST " + JSONObject);
					JSONObject response = null;
					String path = "/v1/verkooporders";
					response = (JSONObject) sendPostRequest(t.getAccessToken(), path, JSONObject + "");
					logger.info("RESPONSE " + response);
					String responseError = (String) response.opt("error");
					String responseMessage = (String) response.optString("message", null);
					if (responseError != null) {
						errorMessage += responseError + "<br>";
						WorkOrderHandler.setWorkorderPlusStatus(w.getId(), w.getWorkorderNr(), "99", "UpdateWorkorder",
								t.getSoftwareToken(), softwareName);
						errorAmount++;
					} else if (responseMessage != null) {
						errorMessage += responseMessage + "<br>";
						System.out.println("response message: " + responseMessage);
						WorkOrderHandler.setWorkorderPlusStatus(w.getId(), w.getWorkorderNr(), "99", "UpdateWorkorder",
								t.getSoftwareToken(), softwareName);
						errorAmount++;
					} else {
						JSONArray array = response.optJSONArray("$diagnoses");
						if (array != null) {
							JSONObject json = array.getJSONObject(0);
							errorAmount++;
							errorDetails += json.optString("$message");
							
						} else {
							successAmount++;
							WorkOrderHandler.setWorkorderStatus(w.getId(), w.getWorkorderNr(), true, "GetWorkorder",
									t.getSoftwareToken(), softwareName);
						}
					}
				}
			}
		}
		if (successAmount > 0) {
			if (newRelation > 0) {
				errorMessage += successAmount + " workorder(s) exported. Click for more details<br>";
				errorDetails += newRelation + " new relation(s) created in SnelStart\n";
				
			} else if (newMaterial > 0) {
				errorMessage += successAmount + " workorder(s) exported. Click for more details<br>";
				errorDetails += newMaterial + " new material(s) created in SnelStart\n";
			} else {
				errorMessage += successAmount + " workorder(s) exported. <br>";
			}
		}
		if (errorAmount > 0) {
			errorMessage += errorAmount + " out of " + exportAmount
					+ " workorders(factuur) have errors. Click for details<br>";
		}
		return new String[] { errorMessage, errorDetails };
	}
	
	public JSONObject invoiceJSON(WorkOrder w, Token t, Settings set, int statusCode)
			throws JSONException, IOException {
		String error = "";
		JSONObject salesOrder = new JSONObject();
		try {
			if (w.getMaterials().size() == 0 && w.getWorkPeriods().size() == 0) {
				error += "No materials/workperiods found on workorder " + w.getWorkorderNr() + "\n";
				return new JSONObject().put("Error", error);
			}
			JSONArray JSONArrayMaterials = new JSONArray();
			JSONObject materials = null;
			if (statusCode == 1) {
				for (WorkPeriod p : w.getWorkPeriods()) {
					Material dbUniversalMaterial = ObjectDAO.getMaterial(t.getSoftwareToken(), set.getMaterialCode());
					String materialId = null;
					if (dbUniversalMaterial == null) {
						return new JSONObject().put("Error",
								"Universal material " + set.getMaterialCode() + " not found\n");
					} else {
						materialId = dbUniversalMaterial.getId();
					}
					
					HourType hourtype = WorkOrderHandler.getHourTypes(t.getSoftwareToken(), p.getHourType(),
							softwareName);
					if (hourtype != null) {
						materials = new JSONObject();
						JSONObject artikel = new JSONObject();
						artikel.put("id", materialId);
						materials.put("artikel", artikel);
						materials.put("omschrijving", hourtype.getName());
						double price = 0;
						DecimalFormat df = new DecimalFormat("#.##");
						String formatted = df.format(hourtype.getSalePrice());
						price = Double.parseDouble(formatted.toString().replaceAll(",", "."));
						materials.put("stuksprijs", price);
						double number = p.getDuration();
						double hours = set.getRoundedHours();
						double urenInteger = (number % hours);
						if (urenInteger < (hours / 2)) {
							number = number - urenInteger;
						} else {
							number = number - urenInteger + hours;
						}
						double quantity = (number / 60);
						formatted = df.format(quantity);
						quantity = Double.parseDouble(formatted.toString().replaceAll(",", "."));
						materials.put("aantal", quantity);
						JSONArrayMaterials.put(materials);
						System.out.println("Hourtypes " + JSONArrayMaterials);
					} else {
						return new JSONObject().put("Error",
								"Hourtype on workorder " + w.getWorkorderNr() + " does not exist in WorkOrderApp\n");
					}
				}
			}
			for (Material m : w.getMaterials()) {
				String materialId = null;
				Material dbMaterial = ObjectDAO.getMaterial(t.getSoftwareToken(), m.getCode());
				if (Double.parseDouble(m.getQuantity()) == 0) {
					error += "The quantity of material " + m.getCode() + " on workorder " + w.getWorkorderNr()
							+ " has to be greater then 0\n";
					return new JSONObject().put("Error", error);
				}
				if (dbMaterial == null) {
					if (set.getExportObjects() != null && set.getExportObjects().contains("materials")) {
						if (!isInteger(m.getCode())) {
							System.out.println("MaterialCode + " + m.getCode());
							error += "Materialscodes on workorder " + w.getWorkorderNr() + " can not contain text\n";
							return new JSONObject().put("Error", error);
						} else {
							JSONObject object = setMaterial(t, m);
							materialId = object.optString("id");
							newMaterial++;
						}
					} else {
						error += "Material " + m.getCode() + " on workorder " + w.getWorkorderNr()
								+ " not found in SnelStart or this material is not synchronized\n";
						return new JSONObject().put("Error", error);
					}
					
				} else {
					materialId = dbMaterial.getId();
				}
				materials = new JSONObject();
				JSONObject artikel = new JSONObject();
				artikel.put("id", materialId);
				materials.put("artikel", artikel);
				materials.put("omschrijving", m.getDescription());
				double price = 0;
				DecimalFormat df = new DecimalFormat("#.##");
				String formatted = df.format(m.getPrice());
				price = Double.parseDouble(formatted.toString().replaceAll(",", "."));
				materials.put("stuksprijs", price);
				materials.put("aantal", Double.parseDouble(m.getQuantity()));
				JSONArrayMaterials.put(materials);
			}
			salesOrder.put("regels", JSONArrayMaterials);
			salesOrder.put("verkooporderBtwIngaveModel", "Exclusief");
			Relation dbRelation = ObjectDAO.getRelation(t.getSoftwareToken(), w.getCustomerDebtorNr(), "invoice");
			String id = null;
			if (dbRelation == null) {
				if (set.getExportObjects() != null && set.getExportObjects().contains("relations")) {
					JSONObject object = setRelation(t, w);
					id = object.optString("id");
					newRelation++;
				} else {
					error += "Relation " + w.getCustomerDebtorNr() + " on workorder " + w.getWorkorderNr()
							+ " not found in SnelStart or relation is not synchronized\n";
					return new JSONObject().put("Error", error);
				}
			} else {
				id = dbRelation.getId();
			}
			JSONObject relatieId = new JSONObject();
			relatieId.put("id", id);
			salesOrder.put("relatie", relatieId);
			salesOrder.put("datum", getCurrentDate(null));
			// Max 50 characters
			String omschrijving = w.getWorkDescription().substring(0, Math.min(w.getWorkDescription().length(), 50));
			salesOrder.put("omschrijving", omschrijving);
			
			for (Relation r : w.getRelations()) {
				Address a = r.getAddressess().get(0);
				if (a.getType().equals("invoice")) {
					JSONObject factuuradres = new JSONObject();
					factuuradres.put("contactpersoon", a.getName());
					factuuradres.put("straat", a.getStreet());
					factuuradres.put("postcode", a.getPostalCode());
					factuuradres.put("plaats", a.getCity());
					salesOrder.put("factuuradres", factuuradres);
				}
				if (a.getType().equals("postal")) {
					JSONObject afleveradres = new JSONObject();
					afleveradres.put("contactpersoon", a.getName());
					afleveradres.put("straat", a.getStreet());
					afleveradres.put("postcode", a.getPostalCode());
					afleveradres.put("plaats", a.getCity());
					salesOrder.put("afleveradres", afleveradres);
				}
			}
			
		} catch (JSONException |
				
				SQLException e) {
			e.printStackTrace();
		}
		return salesOrder;
	}
	
	public JSONObject setRelation(Token t, WorkOrder w) throws IOException {
		JSONObject setRelationResponse = null;
		// Get BTW id default is 21%
		String path = "/v1/relaties";
		JSONObject JSONObject = new JSONObject();
		try {
			for (Relation r : w.getRelations()) {
				Address a = r.getAddressess().get(0);
				if (a.getType().equals("invoice")) {
					JSONArray relatieSoort = new JSONArray();
					relatieSoort.put("Klant");
					JSONObject.put("relatiesoort", relatieSoort);
					JSONObject.put("naam", r.getCompanyName());
					JSONObject factuurAdres = new JSONObject();
					factuurAdres.put("contactpersoon", a.getName());
					factuurAdres.put("straat", a.getStreet());
					factuurAdres.put("postcode", a.getPostalCode());
					factuurAdres.put("plaats", a.getCity());
					factuurAdres.put("contactpersoon", a.getName());
					JSONObject.put("vestigingsAdres", factuurAdres);
					JSONObject.put("telefoon", a.getPhoneNumber());
					JSONObject.put("memo", a.getRemark());
				} else if (a.getType().equals("postal")) {
					JSONObject afleverAdres = new JSONObject();
					afleverAdres.put("contactpersoon", a.getName());
					afleverAdres.put("straat", a.getStreet());
					afleverAdres.put("postcode", a.getPostalCode());
					afleverAdres.put("plaats", a.getCity());
					afleverAdres.put("contactpersoon", a.getName());
					JSONObject.put("correspondentieAdres", afleverAdres);
				}
			}
			logger.info("exportRelationRequest " + JSONObject);
			setRelationResponse = (JSONObject) sendPostRequest(t.getAccessToken(), path, JSONObject + "");
			logger.info("exportRelationResponse " + setRelationResponse);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return setRelationResponse;
	}
	
	public JSONObject setMaterial(Token t, Material m) throws IOException {
		JSONObject setMaterialResponse = null;
		String path = "/v1/artikelen";
		try {
			String omzetId = null;
			String path2 = "/v1/artikelomzetgroepen";
			JSONArray artikelOmzetGroep = (JSONArray) sendGetRequest(t.getAccessToken(), path2, false,
					t.getSoftwareToken());
			for (int i = 0; i < artikelOmzetGroep.length(); i++) {
				JSONObject artikelGroep = artikelOmzetGroep.getJSONObject(i);
				if (artikelGroep.getInt("nummer") == 1) {
					omzetId = artikelGroep.getString("id");
					break;
				}
			}
			JSONObject materiaal = new JSONObject();
			materiaal.put("artikelcode", m.getCode());
			materiaal.put("omschrijving", m.getDescription());
			materiaal.put("verkoopprijs", m.getPrice());
			JSONObject artikelGroep = new JSONObject();
			artikelGroep.put("id", omzetId);
			materiaal.put("artikelOmzetgroep", artikelGroep);
			logger.info("exportMaterialRequest " + materiaal);
			setMaterialResponse = (JSONObject) sendPostRequest(t.getAccessToken(), path, materiaal + "");
			logger.info("exportMaterialResponse " + setMaterialResponse);
		} catch (
		
		JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return setMaterialResponse;
	}
	
	public static JSONObject setUniversalMaterial(Token t, String materialCode) throws IOException {
		JSONObject setMaterialResponse = null;
		String path = "/v1/artikelen";
		try {
			String omzetId = null;
			String path2 = "/v1/artikelomzetgroepen";
			JSONArray artikelOmzetGroep = (JSONArray) sendGetRequest(t.getAccessToken(), path2, false,
					t.getSoftwareToken());
			for (int i = 0; i < artikelOmzetGroep.length(); i++) {
				JSONObject artikelGroep = artikelOmzetGroep.getJSONObject(i);
				if (artikelGroep.getInt("nummer") == 1) {
					omzetId = artikelGroep.getString("id");
					break;
				}
			}
			JSONObject materiaal = new JSONObject();
			if (isInteger(materialCode)) {
				materiaal.put("artikelcode", materialCode);
				materiaal.put("omschrijving", "Uursoorten");
				materiaal.put("verkoopprijs", 0);
				JSONObject artikelGroep = new JSONObject();
				artikelGroep.put("id", omzetId);
				materiaal.put("artikelOmzetgroep", artikelGroep);
				
				setMaterialResponse = (JSONObject) sendPostRequest(t.getAccessToken(), path, materiaal + "");
				
			}
		} catch (
		
		JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return setMaterialResponse;
	}
	
	public static boolean isInteger(String s) {
		try {
			Long.parseLong(s);
		} catch (NumberFormatException e) {
			return false;
		} catch (NullPointerException e) {
			return false;
		}
		// Only get here if we didn't return false
		return true;
	}
}