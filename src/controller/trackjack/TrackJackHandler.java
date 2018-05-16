package controller.trackjack;

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
import java.util.Base64;
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

public class TrackJackHandler {
	private static String apiUrl = System.getenv("TRACKJACK_API_URL");
	final String softwareName = "TrackJack";
	private Boolean checkUpdate = false;
	private final static Logger logger = Logger.getLogger(SoapHandler.class.getName());
	
	public String checkCredentials(String username, String password) throws IOException {
		String link = apiUrl + "Device/GetAllDevices";
		byte[] byteLogin = (username + ":" + password).getBytes();
		byte[] encodedBytes = Base64.getEncoder().encode(byteLogin);
		String base64Login = new String(encodedBytes, "UTF-8");
		System.out.println("base64Login " + base64Login);
		try {
			URL url = new URL(link);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("authorization", "Basic " + base64Login);
			conn.setUseCaches(false);
			if (conn.getResponseCode() > 200 && conn.getResponseCode() < 500) {
				return null;
			} else {
				return base64Login;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public JSONArray getJsonResponse(Token t, String path, String parameters)
			throws IOException, JSONException, URISyntaxException {
		String link = apiUrl + path;
		JSONArray jsonArrayResponse = null;
		try {
			URL url = new URL(link);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setInstanceFollowRedirects(false);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("authorization", "Basic " + t.getAccessToken());
			conn.setUseCaches(false);
			BufferedReader br = null;
			if (conn.getResponseCode() > 200 && conn.getResponseCode() < 500) {
				JSONObject errorObject = new JSONObject();
				errorObject.put("error", conn.getResponseMessage());
				jsonArrayResponse = new JSONArray().put(errorObject);
			} else {
				br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
				String output;
				while ((output = br.readLine()) != null) {
					System.out.println("OUTPUT " + output);
					jsonArrayResponse = new JSONArray(output);
				}
			}
			conn.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return jsonArrayResponse;
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
	
	// Uursoorten
	public String[] getLocations(Token t) throws Exception {
		String errorMessage = "";
		String parameters = "";
		String path = "position/GetLastPosition";
		ArrayList<Location> locations = new ArrayList<Location>();
		
		JSONArray JSONArray = (JSONArray) getJsonResponse(t, path, parameters);
		logger.info("Locations response " + JSONArray);
		if (JSONArray != null) {
			for (int i = 0; i < JSONArray.length(); i++) {
				JSONObject object = JSONArray.getJSONObject(i);
				String lat = object.getDouble("Latitude") + "";
				String lon = object.getDouble("Longitude") + "";
				String id = object.getString("Name");
				int type = object.getInt("Mode");
				Location loc = new Location(lat, lon, id, type);
				locations.add(loc);
			}
		}
		// Locations log message
		if (!locations.isEmpty()) {
			int success = (int) WorkOrderHandler.addData(t.getSoftwareToken(), locations, "locations", softwareName);
			if (success > 0) {
				errorMessage += success + " locations imported<br>";
				checkUpdate = true;
			}
		} else {
			errorMessage += "No locations for import<br>";
		}
		return new String[] { errorMessage, checkUpdate + "" };
	}
	
}