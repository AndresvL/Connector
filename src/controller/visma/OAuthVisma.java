package controller.visma;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
import object.Settings;
import object.Token;
import object.workorder.MaterialCategory;

public class OAuthVisma extends Authenticate {
	private static String oAuthUrl = System.getenv("VISMA_OAUTH_URL");
	private static String clientId = System.getenv("VISMA_CLIENT_ID");
	private static String clientSecret = System.getenv("VISMA_CLIENT_SECRET");
	private static String callback = System.getenv("CALLBACK");
	
	@Override
	public void authenticate(String softwareToken, HttpServletRequest req, HttpServletResponse resp)
			throws ClientProtocolException, IOException, ServletException, SQLException {
		RequestDispatcher rd = null;
		String softwareName = (String) req.getSession().getAttribute("softwareName");
		Token dbToken = null;
		// Get token from database
		try {
			dbToken = TokenDAO.getToken(softwareToken, softwareName);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// First login
		if (dbToken == null) {
			String uri = oAuthUrl + "/resources/oauth/authorize?client_id=" + clientId + "&redirect_uri=" + callback
					+ "&state=success&scope=financialstasks&response_type=code";
			resp.sendRedirect(uri);
		} else if (dbToken.getAccessSecret().equals("invalid")) {
			rd = req.getRequestDispatcher("visma.jsp");
			req.getSession().setAttribute("softwareToken", null);
			req.getSession().setAttribute("errorMessage",
					"softwareToken is al in gebruik door " + dbToken.getSoftwareName());
			req.getSession().setAttribute("validLogin", null);
			// Login successfull
		} else {
			req.getSession().setAttribute("softwareToken", dbToken.getSoftwareToken());
			req.getSession().setAttribute("validLogin", "ok");
			req.getSession().setAttribute("errorMessage", null);
			ArrayList<Map<String, String>> allLogs = ObjectDAO.getLogs(softwareToken);
			if (!allLogs.isEmpty() || allLogs != null) {
				req.getSession().setAttribute("logs", null);
				req.getSession().setAttribute("logs", allLogs);
			}
			VismaHandler visma = new VismaHandler();
			ArrayList<MaterialCategory> companies = null;
			try {
				companies = visma.getCompanies(dbToken, null);
			} catch (Exception e) {
				e.printStackTrace();
			}
			// Map for administrations
			Map<String, String> offices = new HashMap<String, String>();
			for (MaterialCategory m : companies) {
				offices.put(m.getCode(), m.getNaam());
			}
			req.getSession().setAttribute("offices", offices);
			// Set progress to 3 to stop interval in javascript
			if (ObjectDAO.getProgress(softwareToken) == 2) {
				ObjectDAO.saveProgress(3, softwareToken);
			}
			// Get settings from database
			Settings set = ObjectDAO.getSettings(softwareToken);
			ArrayList<MaterialCategory> salesOrderTypes = null;
			try {
				salesOrderTypes = visma.getSalesOrderTypes(dbToken, set);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			ArrayList<MaterialCategory> materialTypes = null;
			try {
				materialTypes = visma.getMaterialTypes(dbToken, set);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (set != null) {
				req.getSession().setAttribute("validLogin", "true");
				Map<String, String> salesOrderTypesSelected = new HashMap<String, String>();
				ArrayList<String> dbSalesOrderTypes = null;
				// ImportOffice is used to store SalesOrderTypes
				if (set.getImportOffice() != null) {
					String list = set.getImportOffice().replace("[", "").replace("]", "");
					String[] strValues = list.split(",\\s");
					dbSalesOrderTypes = new ArrayList<String>(Arrays.asList(strValues));
					for (MaterialCategory mc : salesOrderTypes) {
						if (dbSalesOrderTypes.contains(mc.getCode())) {
							salesOrderTypesSelected.put(mc.getCode(), "selected");
						} else {
							salesOrderTypesSelected.put(mc.getCode(), "");
						}
					}
				} else {
					for (MaterialCategory mc : salesOrderTypes) {
						salesOrderTypesSelected.put(mc.getCode(), "");
					}
				}
				req.getSession().setAttribute("salesOrderTypes", salesOrderTypesSelected);
				
				Map<String, String> materialTypesSelected = new HashMap<String, String>();
				ArrayList<String> dbMaterialTypes = null;
				// ImportOffice is used to store MaterialTypes
				if (set.getUser() != null) {
					String list = set.getUser().replace("[", "").replace("]", "");
					String[] strValues = list.split(",\\s");
					dbMaterialTypes = new ArrayList<String>(Arrays.asList(strValues));
					for (MaterialCategory mc : materialTypes) {
						if (dbMaterialTypes.contains(mc.getCode())) {
							materialTypesSelected.put(mc.getCode(), "selected");
						} else {
							materialTypesSelected.put(mc.getCode(), "");
						}
					}
				} else {
					for (MaterialCategory mc : materialTypes) {
						materialTypesSelected.put(mc.getCode(), "");
					}
				}
				req.getSession().setAttribute("materialTypes", materialTypesSelected);
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
				// exportOffice is used to store administration
				req.getSession().setAttribute("importOffice", set.getExportOffice());
				req.getSession().setAttribute("savedDate", set.getSyncDate());
				req.getSession().setAttribute("checkboxes", allImports);
				req.getSession().setAttribute("exportWerkbonType", exportWerkbonType);
				req.getSession().setAttribute("roundedHours", set.getRoundedHours());
				req.getSession().setAttribute("factuur", set.getFactuurType());
			} else {
				Map<String, String> salesOrderTypesSelected = new HashMap<String, String>();
				for (MaterialCategory mc : salesOrderTypes) {
					salesOrderTypesSelected.put(mc.getCode(), "");
				}
				req.getSession().setAttribute("salesOrderTypes", salesOrderTypesSelected);
				
				Map<String, String> materialTypesSelected = new HashMap<String, String>();
				for (MaterialCategory mc : materialTypes) {
					materialTypesSelected.put(mc.getCode(), "");
				}
				req.getSession().setAttribute("materialTypes", materialTypesSelected);
			}
			
			rd = req.getRequestDispatcher("visma.jsp");
			rd.forward(req, resp);
		}
		
	}
	
	// Called by verifyServlet
	public static Token getAccessToken(String authCode, String refresh, String softwareName, String softwareToken) {
		Token dbToken = new Token();
		dbToken.setSoftwareName(softwareName);
		dbToken.setSoftwareToken(softwareToken);
		String link = oAuthUrl + "/security/api/v2/token";
		String input = null;
		if (authCode != null) {
			input = "client_id=" + clientId + "&client_secret=" + clientSecret + "&code=" + authCode
					+ "&grant_type=authorization_code&redirect_uri=" + callback;
		} else {
			input = "client_id=" + clientId + "&client_secret=" + clientSecret + "&refresh_token=" + refresh
					+ "&grant_type=refresh_token&redirect_uri=" + callback;
		}
		byte[] postData = input.getBytes(StandardCharsets.UTF_8);
		int postDataLength = postData.length;
		
		String auth = clientId + ":" + clientSecret;
		byte[] encodedBytes = Base64.encodeBase64(auth.getBytes());
		String encoding = new String(encodedBytes);
		
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
			conn.setRequestProperty("Authorization", "Basic " + encoding);
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
				try {
					JSONObject json = new JSONObject(output);
					System.out.println(output);
					String accessToken = json.getString("token");
					dbToken.setAccessToken(accessToken);
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