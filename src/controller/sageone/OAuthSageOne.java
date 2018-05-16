package controller.sageone;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import DAO.ObjectDAO;
import DAO.TokenDAO;
import controller.Authenticate;
import controller.WorkOrderHandler;
import object.Settings;
import object.Token;
import object.sageone.Nonce;
import object.sageone.SageoneSigner;

public class OAuthSageOne extends Authenticate {
	private static String host = System.getenv("SAGEONE_OAUTH_HOST");
	private static String apiHost = System.getenv("SAGEONE_API_HOST");
	private static String token = System.getenv("SAGEONE_TOKEN");
	private static String callback = System.getenv("CALLBACK");
	private static String secret = System.getenv("SAGEONE_SECRET");
	
	@Override
	public void authenticate(String softwareToken, HttpServletRequest req, HttpServletResponse resp)
			throws ClientProtocolException, IOException, ServletException, SQLException {
		RequestDispatcher rd = null;
		System.out.println("HOST " + host);
		System.out.println("CALLBACK " + callback);
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
			// Online
			String uri = host + "/auth?client_id=" + token + "&redirect_uri=" + callback + "&response_type=code";
			System.out.println("uri " + uri);
			resp.sendRedirect(uri);
		} else if (dbToken.getAccessSecret().equals("invalid")) {
			rd = req.getRequestDispatcher("sageOne.jsp");
			req.getSession().setAttribute("softwareToken", null);
			req.getSession().setAttribute("errorMessage",
					"softwareToken is already in use by: " + dbToken.getSoftwareName());
			rd.forward(req, resp);
		} else {
			req.getSession().setAttribute("softwareToken", dbToken.getSoftwareToken());
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
				Map<String, String> typeofworkSelected = new HashMap<String, String>();
				for (String s : typeofwork) {
					if (set.getImportOffice() != null && set.getImportOffice().equals(s)) {
						typeofworkSelected.put(s, "selected");
					} else {
						typeofworkSelected.put(s, "");
					}
				}
				req.getSession().setAttribute("types", typeofworkSelected);
				Map<String, String> paymentmethodSelected = new HashMap<String, String>();
				for (String s : paymentMethod) {
					if (set.getExportOffice() != null && set.getExportOffice().equals(s)) {
						paymentmethodSelected.put(s, "selected");
					} else {
						paymentmethodSelected.put(s, "");
					}
				}
				req.getSession().setAttribute("paymentmethods", paymentmethodSelected);
				Map<String, String> allImports = new HashMap<String, String>();
				for (String s : set.getImportObjects()) {
					allImports.put(s, "selected");
				}
				Map<String, String> exportWerkbonType = new HashMap<String, String>();
				exportWerkbonType.put(set.getExportWerkbontype(), "selected");
				Map<String, String> allExports = new HashMap<String, String>();
				if (set.getExportObjects() != null) {
					for (String s : set.getExportObjects()) {
						allExports.put(s, "selected");
					}
				}
				req.getSession().setAttribute("exportCheckboxes", allExports);
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
			
			rd = req.getRequestDispatcher("sageOne.jsp");
			rd.forward(req, resp);
		}
		
	}
	
	// Called by verifyServlet
	public static Token getAccessToken(String authCode, String refresh, String softwareName, String softwareToken) {
		Token dbToken = new Token();
		dbToken.setConsumerToken(token);
		dbToken.setConsumerSecret(secret);
		dbToken.setSoftwareName(softwareName);
		dbToken.setSoftwareToken(softwareToken);
		String link = "https://" + apiHost + "/oauth2/token";
		String input = null;
		if (authCode != null) {
			input = "client_id=" + token + "&client_secret=" + secret + "&code=" + authCode
					+ "&grant_type=authorization_code&redirect_uri=" + callback;
		} else {
			input = "client_id=" + token + "&client_secret=" + secret + "&refresh_token=" + refresh
					+ "&grant_type=refresh_token&redirect_uri=" + callback;
		}
		System.out.println("LINK = " + link + "?" + input);
		byte[] postData = input.getBytes(StandardCharsets.UTF_8);
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
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("charset", "utf-8");
			conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
			// conn.setRequestProperty("Authorization", "Basic " + encoding);
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
			System.out.println("Output from Server .... \n");
			
			while ((output = br.readLine()) != null) {
				System.out.println("OUTPUT " + output);
				try {
					JSONObject json = new JSONObject(output);
					String refreshToken = json.getString("refresh_token");
					String accessToken = json.getString("access_token");
					dbToken.setAccessToken(accessToken);
					// refreshToken!
					dbToken.setAccessSecret(refreshToken);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			conn.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			if (authCode != null) {
				TokenDAO.saveToken(dbToken);
			} else {
				TokenDAO.updateToken(dbToken);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return dbToken;
	}
}
