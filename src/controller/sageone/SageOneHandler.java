package controller.sageone;

import java.io.BufferedReader;
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
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import DAO.ObjectDAO;
import controller.WorkOrderHandler;
import controller.twinfield.SoapHandler;
import object.Settings;
import object.Token;
import object.sageone.Nonce;
import object.sageone.SageoneSigner;
import object.workorder.Address;
import object.workorder.HourType;
import object.workorder.Material;
import object.workorder.Relation;
import object.workorder.WorkOrder;
import object.workorder.WorkPeriod;

public class SageOneHandler {
	final String softwareName = "SageOne";
	private final static Logger logger = Logger.getLogger(SoapHandler.class.getName());
	// Change to environment variable
	protected static String host = System.getenv("SAGEONE_API_HOST");
	private static String signingSecret = System.getenv("SAGEONE_SIGNING_SECRET");
	private int newRelation = 0;
	
	public boolean checkAccessToken(Token t) throws Exception {
		JSONObject jsonObj = new JSONObject();
		String path = "/accounts/v2/tax_rates";
		String parameters = "";
		boolean b = true;
		jsonObj = (JSONObject) getJSON(t.getAccessToken(), parameters, path, "object", null);
		logger.info("contacts response JSONObject " + jsonObj);
		if (jsonObj == null) {
			b = false;
		}
		return b;
	}
	
	public static SageoneSigner OAuthSignature(String parameters, String accessToken, String path, String method,
			String newToken) {
		TreeMap<String, String> params;
		if (newToken == null) {
			newToken = signingSecret;
		}
		// Generate the signature
		if (parameters == null) {
			params = new TreeMap<String, String>();
		} else {
			params = new Gson().fromJson(parameters, new TypeToken<TreeMap<String, String>>() {
			}.getType());
			
		}
		Nonce n = new Nonce();
		String nonce = n.generateNonce();
		
		String link = "https://" + host + path;
		SageoneSigner s = new SageoneSigner(method, link, params, nonce, newToken, accessToken);
		
		return s;
	}
	
	public static Object getJSON(String accessToken, String parameters, String path, String method, String newToken)
			throws IOException, URISyntaxException {
		String jsonString = null;
		Object object = null;
		String params = "";
		try {
			if (parameters.equals("")) {
				parameters = null;
			} else {
				JsonParser parser = new JsonParser();
				JsonObject o = (JsonObject) parser.parse(parameters);
				params = jsonToUrlEncodedString(o, "");
			}
			URI uri = new URI("https", host, path, params, null);
			String request = uri.toASCIIString();
			URL url = new URL(request);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			// conn.setDoInput(true);
			
			SageoneSigner s = OAuthSignature(parameters, accessToken, path, "GET", newToken);
			String signature = s.signature();
			String nonce = s.getNonce();
			
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Authorization", "Bearer " + accessToken);
			conn.setRequestProperty("X-Signature", signature);
			conn.setRequestProperty("X-Nonce", nonce);
			conn.setRequestProperty("Accept", "*/*");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("User-Agent", "workorderapp");
			conn.setUseCaches(false);
			if (conn.getResponseCode() > 200 && conn.getResponseCode() < 500) {
				BufferedReader br = new BufferedReader(
						new InputStreamReader((conn.getErrorStream()), StandardCharsets.UTF_8));
				while ((jsonString = br.readLine()) != null) {
					System.out.println("Response BODY " + jsonString);
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
	
	public Object postJSON(String accessToken, JSONObject json, String path, String method, String requestMethod) {
		String jsonString = json + "";
		TreeMap<String, String> params;
		JSONObject jsonResponse = null;
		params = new Gson().fromJson(jsonString, new TypeToken<TreeMap<String, String>>() {
		}.getType());
		
		SageoneSigner s = OAuthSignature(jsonString, accessToken, path, requestMethod, null);
		try {
			URIBuilder builder = new URIBuilder("https://" + host + path);
			URI uri = builder.build();
			// Set the post body params
			ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
			for (Map.Entry<String, String> entry : params.entrySet()) {
				postParameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
			}
			HttpRequestBase request;
			JsonParser parser = new JsonParser();
			JsonObject o = (JsonObject) parser.parse(json + "");
			String urlencoded = jsonToUrlEncodedString(o, "");
			if (requestMethod.equals("POST")) {
				request = new HttpPost(uri);
				((HttpPost) request).setEntity(new UrlEncodedFormEntity(postParameters));
			} else {
				request = new HttpPut(uri);
				((HttpPut) request).setEntity(new UrlEncodedFormEntity(postParameters));
			}
			
			setRequestHeaders(s.getNonce(), accessToken, s.signature(), request);
			
			// Make Request
			HttpClient httpclient = HttpClients.createDefault();
			HttpResponse response = httpclient.execute(request);
			
			request.releaseConnection();
			HttpEntity entity = response.getEntity();
			jsonResponse = new JSONObject(EntityUtils.toString(entity));
			
		} catch (IOException | JSONException | URISyntaxException e) {
			e.printStackTrace();
		}
		return jsonResponse;
	}
	
	/* set the request headers */
	private void setRequestHeaders(String nonce, String accessToken, String signature, HttpRequestBase request) {
		request.addHeader("X-Signature", signature);
		request.addHeader("X-Nonce", nonce);
		request.addHeader("Authorization", "Bearer " + accessToken);
		request.addHeader("Accept", "*/*");
		request.addHeader("Content-Type", "application/x-www-form-urlencoded");
		request.addHeader("User-Agent", "workorderapp");
		System.out.println("X-Signature: " + signature);
		System.out.println("X-Nonce: " + nonce);
		System.out.println("Authorization: " + "Bearer " + accessToken);
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
	
	public String convertDate(String string) {
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
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
		Format formatter = new SimpleDateFormat("yyyy-MM-dd");
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
	public String[] getMaterials(Token t, String date) throws Exception {
		boolean checkUpdate = false;
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		String path = "/accounts/v2/products";
		String parameters = "";
		String errorMessage = "";
		int importCount = 0;
		ArrayList<Material> materials = new ArrayList<Material>();
		Boolean hasContent = ObjectDAO.hasContent(t.getSoftwareToken(), "materials");
		// if (date != null && hasContent) {
		// // yyyy-MM-dd hh:mm:ss
		// parameters += "&changedFromDate=" + getDateMinHour(date);
		// }
		JSONObject json = (JSONObject) getJSON(t.getAccessToken(), parameters, path, "object", null);
		logger.info("Materials response " + json);
		if (json != null) {
			jsonArray = json.getJSONArray("$resources");
			for (int i = 0; i < jsonArray.length(); i++) {
				jsonObj = jsonArray.getJSONObject(i);
				String id = jsonObj.getInt("id") + "";
				// String modified = jsonObj.getString("ChangedUtc");
				String productCode = jsonObj.getString("product_code");
				String description = jsonObj.getString("description");
				Double salePrice = jsonObj.getDouble("sales_price");
				Double costPrice = jsonObj.getDouble("cost_price");
				// String unit = jsonObj.getString("UnitName");
				Material m = new Material(productCode, null, "", description, salePrice, null, null, id);
				materials.add(m);
				
			}
		}
		// Materials log message
		if (!materials.isEmpty()) {
			int successAmount = (int) WorkOrderHandler.addData(t.getSoftwareToken(), materials, "materials",
					softwareName);
			if (successAmount > 0) {
				ObjectDAO.saveMaterials(materials, t.getSoftwareToken());
				errorMessage += successAmount + " materials imported<br>";
				checkUpdate = true;
			} else {
				errorMessage += "Something went wrong with materials<br>";
			}
		} else {
			errorMessage += "No materials for import<br>";
		}
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
	// Producten
	public String[] getHourtypes(Token t, String date) throws Exception {
		boolean checkUpdate = false;
		JSONArray jsonArray = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		String path = "/accounts/v2/services";
		String parameters = "";
		String errorMessage = "";
		int importCount = 0;
		ArrayList<HourType> hourtypes = new ArrayList<HourType>();
		Boolean hasContent = ObjectDAO.hasContent(t.getSoftwareToken(), "hourtypes");
		// if (date != null && hasContent) {
		// // yyyy-MM-dd hh:mm:ss
		// parameters += "&changedFromDate=" + getDateMinHour(date);
		// }
		JSONObject json = (JSONObject) getJSON(t.getAccessToken(), parameters, path, "object", null);
		logger.info("Hourtype response " + json);
		if (json != null) {
			jsonArray = json.getJSONArray("$resources");
			for (int i = 0; i < jsonArray.length(); i++) {
				jsonObj = jsonArray.getJSONObject(i);
				String id = jsonObj.getInt("id") + "";
				// String modified = jsonObj.getString("ChangedUtc");
				String productCode = jsonObj.getInt("id") + "";
				String description = jsonObj.getString("description");
				Double salePrice = jsonObj.getDouble("period_rate_price");
				Double costPrice = 0d;
				// String unit = jsonObj.getString("UnitName");
				HourType h = new HourType(productCode, description, 0, 1, costPrice, salePrice, 1, null, id);
				hourtypes.add(h);
				
			}
		}
		// Materials log message
		if (!hourtypes.isEmpty()) {
			int successAmount = (int) WorkOrderHandler.addData(t.getSoftwareToken(), hourtypes, "hourtypes",
					softwareName);
			if (successAmount > 0) {
				ObjectDAO.saveHourTypes(hourtypes, t.getSoftwareToken());
				errorMessage += successAmount + " hourtypes imported<br>";
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
		String path = "/accounts/v2/contacts";
		String parameters = "{'contact[contact_type]': 1}";
		String errorMessage = "";
		ArrayList<Relation> relations = new ArrayList<Relation>();
		Boolean hasContent = ObjectDAO.hasContent(t.getSoftwareToken(), "relations");
		// if (date != null && hasContent) {
		// parameters += "changedFromDate=" + getDateMinHour(date);
		// }
		JSONObject json = (JSONObject) getJSON(t.getAccessToken(), parameters, path, "object", null);
		logger.info("Relation response JSONArray " + json);
		if (json != null) {
			jsonArray = json.getJSONArray("$resources");
			for (int i = 0; i < jsonArray.length(); i++) {
				jsonObj = jsonArray.getJSONObject(i);
				String id = jsonObj.getInt("id") + "";
				
				ArrayList<Address> address = new ArrayList<Address>();
				JSONObject deliveryAddress = jsonObj.getJSONObject("delivery_address");
				int checkDelivery = deliveryAddress.optInt("$key", 0);
				if (checkDelivery == 0) {
					// Postal = invoice address
				}
				// Invoice
				String invoiceContact = jsonObj.optString("name", "<empty>");
				String phoneNr = jsonObj.optString("telephone");
				String mobileNr = jsonObj.optString("mobile");
				if (phoneNr == null) {
					phoneNr = mobileNr;
				}
				String remark = jsonObj.optString("notes");
				String invoiceCompanyName = jsonObj.optString("company", "<empty>");
				String invoiceEmail = jsonObj.optString("email");
				JSONObject invoiceAddress = jsonObj.getJSONObject("main_address");
				String invoicestreet = invoiceAddress.optString("street_one", "<empty>");
				String invoicepostalCode = invoiceAddress.optString("postcode", "<empty>");
				String invoicecity = invoiceAddress.optString("town", "<empty>");
				if (!invoicecity.equals("<empty>")) {
					Address invoice = new Address(invoiceContact, phoneNr, invoiceEmail, invoicestreet, "",
							invoicepostalCode, invoicecity, remark, "invoice", 1);
					address.add(invoice);
				}
				String modified = jsonObj.optString("updated_at");
				Relation r = new Relation(invoiceCompanyName, id, invoiceContact, invoiceEmail, address, modified, id);
				relations.add(r);
			}
		}
		if (!relations.isEmpty()) {
			int successAmount = (int) WorkOrderHandler.addData(t.getSoftwareToken(), relations, "relations",
					softwareName);
			if (successAmount > 0) {
				ObjectDAO.saveRelations(relations, t.getSoftwareToken());
				errorMessage += successAmount + " relations imported<br>";
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
			exportAmount++;
			JSONObject = invoiceJSON(w, t, set.getRoundedHours(), set.getExportObjects());
			String error = (String) JSONObject.opt("Error");
			if (error != null) {
				errorDetails += error;
				errorAmount++;
			} else {
				logger.info("REQUEST " + JSONObject);
				JSONObject response = null;
				String path = "/accounts/v2/sales_invoices";
				
				response = (JSONObject) postJSON(t.getAccessToken(), JSONObject, path, "object", "POST");
				logger.info("RESPONSE " + response);
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
		if (successAmount > 0) {
			if (newRelation > 0) {
				errorMessage += successAmount + " workorder(s) exported. Click for more details<br>";
				errorDetails += newRelation + " new relation(s) exported";
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
	
	public JSONObject invoiceJSON(WorkOrder w, Token t, int roundedHours, ArrayList<String> exportObjects)
			throws JSONException {
		String error = "";
		JSONObject JSONObject = new JSONObject();
		String debtorNr = null;
		
		try {
			int i = 0;
			if (w.getMaterials().size() == 0 && w.getWorkPeriods().size() == 0) {
				error += "No materials or workperiods found on workorder " + w.getWorkorderNr() + "\n";
				return new JSONObject().put("Error", error);
			}
			for (Material m : w.getMaterials()) {
				Material dbMaterial = ObjectDAO.getMaterial(t.getSoftwareToken(), m.getCode());
				if (Double.parseDouble(m.getQuantity()) == 0) {
					error += "The quantity of material " + m.getCode() + " on workorder " + w.getWorkorderNr()
							+ " has to be greater then 0\n";
					return new JSONObject().put("Error", error);
				}
				if (dbMaterial == null) {
					error += "Material " + m.getCode() + " on workorder " + w.getWorkorderNr()
							+ " not found in SageOne or this material is not synchronized\n";
					return new JSONObject().put("Error", error);
				} else {
					String lineItems = "sales_invoice[line_items_attributes][" + i + "]";
					i++;
					double price = 0;
					DecimalFormat df = new DecimalFormat("#.##");
					String formatted = df.format(m.getPrice());
					price = Double.parseDouble(formatted.toString().replaceAll(",", "."));
					JSONObject.put(lineItems + "[description]", m.getDescription());
					JSONObject.put(lineItems + "[product_code]", m.getCode());
					JSONObject.put(lineItems + "[quantity]", Double.parseDouble(m.getQuantity()));
					JSONObject.put(lineItems + "[unit_price]", price);
					JSONObject.put(lineItems + "[discount_percentage]", 0);
					JSONObject.put(lineItems + "[tax_rate_id]", 1);
				}
			}
			for (WorkPeriod p : w.getWorkPeriods()) {
				HourType h = ObjectDAO.getHourType(t.getSoftwareToken(), p.getHourType());
				String lineItems = "sales_invoice[line_items_attributes][" + i + "]";
				i++;
				// Get ID from db(hourtype)
				if (h == null) {
					error += "Hourtype " + p.getHourType() + " on workorder " + w.getWorkorderNr()
							+ " not found in SageOne or this hourtype is not synchronized\n";
					return new JSONObject().put("Error", error);
				} else {
					i++;
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
					JSONObject.put(lineItems + "[description]", h.getName());
					JSONObject.put(lineItems + "[product_code]", h.getCode());
					JSONObject.put(lineItems + "[quantity]", quantity);
					JSONObject.put(lineItems + "[unit_price]", unitPrice);
					JSONObject.put(lineItems + "[discount_percentage]", 0);
					JSONObject.put(lineItems + "[tax_rate_id]", 1);
				}
			}
			Relation dbRelation = ObjectDAO.getRelation(t.getSoftwareToken(), w.getCustomerDebtorNr(), "invoice");
			if (dbRelation == null) {
				if (exportObjects != null && exportObjects.contains("relations")) {
					JSONObject object = setRelation(t, w);
					debtorNr = object.getInt("id") + "";
					newRelation++;
				} else {
					error += "Relation " + w.getCustomerDebtorNr() + " on workorder " + w.getWorkorderNr()
							+ " not found in SageOne or relation is not synchronized\n";
					return new JSONObject().put("Error", error);
				}
			} else {
				debtorNr = w.getCustomerDebtorNr();
			}
			JSONObject.put("sales_invoice[contact_id]", debtorNr);
			JSONObject.put("sales_invoice[date]", getCurrentDate(null));
			JSONObject.put("sales_invoice[duedate]", convertDate(getCurrentDate(null)));
			String street = null;
			for (Relation r : w.getRelations()) {
				Address a = r.getAddressess().get(0);
				if (a.getType().equals("invoice")) {
					street = a.getStreet();
					JSONObject.put("sales_invoice[main_address_street_1]", street);
					JSONObject.put("sales_invoice[main_address_postcode]", a.getPostalCode());
					JSONObject.put("sales_invoice[main_address_locality]", a.getCity());
					JSONObject.put("sales_invoice[main_address_country_id]", "PT");
				}
				if (a.getType().equals("postal")) {
					JSONObject.put("sales_invoice[main_address_street_1]", street);
					JSONObject.put("sales_invoice[main_address_postcode]", a.getPostalCode());
					JSONObject.put("sales_invoice[main_address_locality]", a.getCity());
					JSONObject.put("sales_invoice[main_address_country_id]", "PT");
				}
			}
			
		} catch (JSONException | SQLException e) {
			e.printStackTrace();
		}
		return JSONObject;
	}
	
	public JSONObject setRelation(Token t, WorkOrder w) {
		JSONObject setRelationResponse = null;
		// Get BTW id default is 21%
		String path = "/accounts/v2/contacts";
		JSONObject JSONObject = new JSONObject();
		try {
			for (Relation r : w.getRelations()) {
				Address a = r.getAddressess().get(0);
				if (a.getType().equals("invoice")) {
					JSONObject.put("contact[name]", a.getName());
					// JSONObject.put("contact[id]", w.getCustomerDebtorNr());
					JSONObject.put("contact[company]", r.getCompanyName());
					JSONObject.put("contact[contact_type_id]", 1);
					JSONObject.put("contact[email]", r.getEmailWorkorder());
					JSONObject.put("contact[telephone]", a.getPhoneNumber());
					JSONObject.put("contact[notes]", a.getRemark());
					JSONObject.put("contact[tax_number]", 123456789);
					JSONObject.put("contact[main_address][street_one]", a.getStreet());
					JSONObject.put("contact[main_address][country_id]", "PT");
					JSONObject.put("contact[main_address][postcode]", a.getPostalCode());
					JSONObject.put("contact[main_address][town]", a.getCity());
				}
				if (a.getType().equals("postal")) {
					JSONObject.put("contact[delivery_address][street_one]", a.getStreet());
					JSONObject.put("contact[delivery_address][country_id]", "PT");
					JSONObject.put("contact[delivery_address][postcode]", a.getPostalCode());
					JSONObject.put("contact[delivery_address][town]", a.getCity());
				}
			}
			logger.info("setRelationRequest " + JSONObject);
			setRelationResponse = (JSONObject) postJSON(t.getAccessToken(), JSONObject, path, "object", "POST");
			logger.info("setRelationResponse " + setRelationResponse);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return setRelationResponse;
	}
	
	private static String jsonToUrlEncodedString(JsonObject jsonObject, String prefix) {
		String urlString = "";
		int i = 0;
		for (Map.Entry<String, JsonElement> item : jsonObject.entrySet()) {
			i++;
			if (item.getValue() != null && item.getValue().isJsonObject()) {
				urlString += jsonToUrlEncodedString(item.getValue().getAsJsonObject(),
						prefix.isEmpty() ? item.getKey() : prefix + "[" + item.getKey() + "]");
			} else {
				if (i == jsonObject.entrySet().size()) {
					urlString += prefix.isEmpty() ? item.getKey() + "=" + item.getValue().getAsString()
							: prefix + "[" + item.getKey() + "]=" + item.getValue().getAsString();
				} else {
					urlString += prefix.isEmpty() ? item.getKey() + "=" + item.getValue().getAsString() + "&"
							: prefix + "[" + item.getKey() + "]=" + item.getValue().getAsString() + "&";
				}
			}
		}
		return urlString;
	}
}