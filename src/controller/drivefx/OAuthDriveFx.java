package controller.drivefx;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import DAO.ObjectDAO;
import DAO.TokenDAO;
import controller.Authenticate;
import controller.WorkOrderHandler;
import object.Settings;
import object.Token;

public class OAuthDriveFx extends Authenticate {
	private static String appId = System.getenv("DRIVEFX_APPID");
	private static String backendUrl = System.getenv("DRIVEFX_BACKENDURL");
	private static String host = System.getenv("DRIVEFX_HOST");
	
	@Override
	public void authenticate(String softwareToken, HttpServletRequest req, HttpServletResponse resp)
			throws ClientProtocolException, IOException, ServletException, SQLException {
		
		RequestDispatcher rd = null;
		String softwareName = req.getParameter("softwareName");
		
		Token dbToken = null;
		// Get token from database
		try {
			dbToken = TokenDAO.getToken(softwareToken, softwareName);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// First time login
		if (dbToken == null) {
			String userCode = null;
			String password = null;
			System.out.println("dbToken empty");
			if (!req.getParameterMap().containsKey("userCode")) {
				req.getSession().setAttribute("loggedIn", false);
			} else {
				userCode = req.getParameter("userCode");
				password = req.getParameter("password");
				Token t = getAccessToken(backendUrl, appId, userCode, password, "", "Never", softwareName,
						softwareToken);
				if (t.getAccessSecret() != null) {
					req.getSession().setAttribute("errorMessage", t.getAccessSecret());
					System.out.println("ERROR " + t.getAccessSecret());
					
				} else {
					req.getSession().setAttribute("loggedIn", true);
					req.getSession().setAttribute("errorMessage", "true");
				}
			}
		} else if (dbToken.getAccessSecret().equals("invalid")) {
			req.getSession().setAttribute("softwareToken", null);
			req.getSession().setAttribute("errorMessage",
					"SoftwareToken is already in use by " + dbToken.getSoftwareName());
		} else {
			req.getSession().setAttribute("softwareToken", dbToken.getSoftwareToken());
			req.getSession().setAttribute("loggedIn", "pending");
			ArrayList<Map<String, String>> allLogs = ObjectDAO.getLogs(softwareToken);
			if (!allLogs.isEmpty() || allLogs != null) {
				req.getSession().setAttribute("logs", null);
				req.getSession().setAttribute("logs", allLogs);
			}
			// get all typeofwork and paymendmethods
			ArrayList<String> typeofwork = (ArrayList<String>) WorkOrderHandler.getTypeofwork(softwareToken,
					softwareName, "worktypes", "PT");
			
			ArrayList<String> paymentMethod = (ArrayList<String>) WorkOrderHandler.getTypeofwork(softwareToken,
					softwareName, "paymentmethods", "PT");
			
			Settings set = ObjectDAO.getSettings(softwareToken);
			if (set != null) {
				
				req.getSession().setAttribute("errorMessage", "");
				// To set typeofwork selected in html page
				Map<String, String> typeofworkSelected = new HashMap<String, String>();
				for (String s : typeofwork) {
					if (set.getImportOffice() != null && set.getImportOffice().equals(s)) {
						typeofworkSelected.put(s, "selected");
					} else {
						typeofworkSelected.put(s, "");
					}
				}
				req.getSession().setAttribute("types", typeofworkSelected);
				
				// To set paymentmethod selected in html page
				Map<String, String> paymentmethodSelected = new HashMap<String, String>();
				for (String s : paymentMethod) {
					if (set.getExportOffice() != null && set.getExportOffice().equals(s)) {
						paymentmethodSelected.put(s, "selected");
					} else {
						paymentmethodSelected.put(s, "");
					}
				}
				req.getSession().setAttribute("paymentmethods", paymentmethodSelected);
				
				// To set import checkbox in html page
				Map<String, String> allImports = new HashMap<String, String>();
				for (String s : set.getImportObjects()) {
					allImports.put(s, "selected");
				}
				Map<String, String> exportWerkbonType = new HashMap<String, String>();
				exportWerkbonType.put(set.getExportWerkbontype(), "selected");
				// get all administrations
				// ArrayList<Map<String, String>> offices =
				// (ArrayList<Map<String, String>>) DriveFxHandler
				// .getOffices(dbToken);
				
				// req.getSession().setAttribute("offices", offices);
				req.getSession().setAttribute("importOffice", set.getImportOffice());
				req.getSession().setAttribute("savedDate", set.getSyncDate());
				req.getSession().setAttribute("checkboxes", allImports);
				req.getSession().setAttribute("exportWerkbonType", exportWerkbonType);
				req.getSession().setAttribute("roundedHours", set.getRoundedHours());
				req.getSession().setAttribute("factuur", set.getFactuurType());
				
			} else {
				Map<String, String> typeofworkSelected = new HashMap<String, String>();
				for (String s : typeofwork) {
					typeofworkSelected.put(s, "");
				}
				req.getSession().setAttribute("types", typeofworkSelected);
				
				Map<String, String> paymentmethodSelected = new HashMap<String, String>();
				for (String s : paymentMethod) {
					paymentmethodSelected.put(s, "");
				}
				req.getSession().setAttribute("paymentmethods", paymentmethodSelected);
			}
		}
		
		rd = req.getRequestDispatcher("driveFx.jsp");
		rd.forward(req, resp);
	}
	
	public static Token getAccessToken(String backendUrl, String appId, String userCode, String password,
			String company, String tokenLife, String softwareName, String softwareToken) {
		String path = "generateAccessToken";
		String link = host + path;
		System.out.println("getAccessToken Test " + userCode);
		Token dbToken = new Token();
		dbToken.setSoftwareName(softwareName);
		dbToken.setSoftwareToken(softwareToken);
		JSONObject jsonObject = null;
		if (userCode != null) {
			try {
				jsonObject = new JSONObject();
				JSONObject jsonCredentials = new JSONObject();
				jsonCredentials.put("backendUrl", backendUrl);
				jsonCredentials.put("appId", appId);
				jsonCredentials.put("userCode", userCode);
				jsonCredentials.put("password", password);
				jsonCredentials.put("tokenLifeTime", tokenLife);
				if (!company.equals("") && company != null) {
					jsonCredentials.put("company", company);
				}
				jsonObject.put("credentials", jsonCredentials);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		byte[] postData = jsonObject.toString().getBytes(StandardCharsets.UTF_8);
		int postDataLength = postData.length;
		
		// String auth = token + ":" + secret;
		// byte[] encodedBytes = Base64.encodeBase64(auth.getBytes());
		// String encoding = new String(encodedBytes);
		String output = null;
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
			// conn.setRequestProperty("Authorization", "Basic " + encoding);
			conn.setUseCaches(false);
			
			try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
				wr.write(postData);
			}
			
			BufferedReader br = null;
			br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			
			System.out.println("Output from Server .... \n");
			
			while ((output = br.readLine()) != null) {
				System.out.println(output);
				try {
					JSONObject json = new JSONObject(output);
					String accessToken = null;
					int code = json.getInt("code");
					if (code == 0) {
						accessToken = json.getString("token");
					} else {
						dbToken.setAccessSecret(json.getString("message"));
					}
					
					dbToken.setAccessToken(accessToken);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			conn.disconnect();
			if (dbToken.getAccessSecret() == null) {
				TokenDAO.saveToken(dbToken);
			}
		} catch (IOException | SQLException e) {
			e.printStackTrace();
		}
		
		return dbToken;
	}
	
}
