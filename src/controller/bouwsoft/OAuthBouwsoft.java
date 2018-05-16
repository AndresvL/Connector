package controller.bouwsoft;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
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
import object.Settings;
import object.Token;
import object.workorder.MaterialCategory;

public class OAuthBouwsoft extends Authenticate {
	private static String refreshTokenUrl = System.getenv("BOUWSOFT_REFRESHTOKEN_URL");
	private static String callback = System.getenv("CALLBACK");
	private static String appKey = System.getenv("BOUWSOFT_APPKEY");
	
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
			req.getSession().setAttribute("clientToken", null);
			resp.sendRedirect(getRedirectURL());
			// If token is already used
		} else if (dbToken.getAccessSecret().equals("invalid")) {
			if (softwareName.equals("Bouwsoft")) {
				rd = req.getRequestDispatcher("bouwsoft.jsp");
				req.getSession().setAttribute("softwareToken", null);
				req.getSession().setAttribute("errorMessage",
						"softwareToken is al in gebruik door " + dbToken.getSoftwareName());
				// Groensoft
			} else {
				rd = req.getRequestDispatcher("groensoft.jsp");
				req.getSession().setAttribute("softwareToken", null);
				req.getSession().setAttribute("errorMessage",
						"softwareToken is al in gebruik door " + dbToken.getSoftwareName());
			}
			// Login successfull
		} else {
			req.getSession().setAttribute("softwareToken", dbToken.getSoftwareToken());
			req.getSession().setAttribute("clientToken", "true");
			ArrayList<Map<String, String>> allLogs = ObjectDAO.getLogs(softwareToken);
			if (!allLogs.isEmpty() || allLogs != null) {
				req.getSession().setAttribute("logs", null);
				req.getSession().setAttribute("logs", allLogs);
			}
			BouwsoftHandler bouwsoft = new BouwsoftHandler();
			// Get all material lists
			ArrayList<MaterialCategory> materialList = null;
			try {
				materialList = (ArrayList<MaterialCategory>) bouwsoft
						.getMaterialList(BouwsoftHandler.checkAccessToken(dbToken), null, false);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (ObjectDAO.getProgress(softwareToken) == 2) {
				ObjectDAO.saveProgress(3, softwareToken);
			}
			// Get settings from database
			Settings set = ObjectDAO.getSettings(softwareToken);
			if (set != null) {
				Map<String, String> categoriesSelected = new HashMap<String, String>();
				ArrayList<String> dbMaterialGroups = null;
				// ImportOffice is used to store materialGroups
				if (set.getImportOffice() != null) {
					String list = set.getImportOffice().replace("[", "").replace("]", "");
					String[] strValues = list.split(",\\s");
					dbMaterialGroups = new ArrayList<String>(Arrays.asList(strValues));
					for (MaterialCategory mc : materialList) {
						if (dbMaterialGroups.contains(mc.getCode())) {
							categoriesSelected.put(mc.getCode(), "selected");
						} else {
							categoriesSelected.put(mc.getCode(), "");
						}
					}
				} else {
					for (MaterialCategory mc : materialList) {
						categoriesSelected.put(mc.getCode(), "");
					}
				}
				req.getSession().setAttribute("materialGroups", categoriesSelected);
				// ExportOffice is used to store projectFilters
				Map<String, String> projectFilterSelected = new HashMap<String, String>();
				if (set.getExportOffice() != null) {
					String list = set.getExportOffice().replace("[", "").replace("]", "");
					String[] strValues = list.split(",\\s");
					ArrayList<String> dbProjectFilters = new ArrayList<String>(Arrays.asList(strValues));
					for (String s : dbProjectFilters) {
						projectFilterSelected.put(s, "selected");
					}
				}
				req.getSession().setAttribute("projectFilters", projectFilterSelected);
				req.getSession().setAttribute("errorMessage", "");
				Map<String, String> allImports = new HashMap<String, String>();
				for (String s : set.getImportObjects()) {
					allImports.put(s, "selected");
				}
				Map<String, String> exportWerkbonType = new HashMap<String, String>();
				exportWerkbonType.put(set.getExportWerkbontype(), "selected");
				
				req.getSession().setAttribute("savedDate", set.getSyncDate());
				req.getSession().setAttribute("checkboxes", allImports);
				req.getSession().setAttribute("exportWerkbonType", exportWerkbonType);
				req.getSession().setAttribute("roundedHours", set.getRoundedHours());
				req.getSession().setAttribute("factuur", set.getFactuurType());
			} else {
				Map<String, String> categoriesSelected = new HashMap<String, String>();
				for (MaterialCategory mc : materialList) {
					categoriesSelected.put(mc.getCode(), "");
				}
				req.getSession().setAttribute("materialGroups", categoriesSelected);
				
			}
			if (softwareName.equals("Bouwsoft")) {
				rd = req.getRequestDispatcher("bouwsoft.jsp");
				// Groensoft
			} else {
				rd = req.getRequestDispatcher("groensoft.jsp");
			}
			rd.forward(req, resp);
		}
		
	}
	
	public String getRedirectURL() {
		String output = null;
		String requestURL = null;
		try {
			URL url = new URL(refreshTokenUrl + "RefreshToken");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setInstanceFollowRedirects(false);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("AppKey", appKey);
			conn.setRequestProperty("redirecturl", callback);
			conn.setUseCaches(true);
			BufferedReader br = null;
			if (conn.getResponseCode() > 200 && conn.getResponseCode() < 405) {
				br = new BufferedReader(new InputStreamReader((conn.getErrorStream())));
			} else {
				br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			}
			
			while ((output = br.readLine()) != null) {
				try {
					JSONObject json = new JSONObject(output);
					requestURL = json.getString("RequestURL");
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			conn.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return requestURL;
	}
	
}
