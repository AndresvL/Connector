package controller.snelstart;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import DAO.ObjectDAO;
import DAO.TokenDAO;
import controller.Authenticate;
import controller.WorkOrderHandler;
import object.Settings;
import object.Token;

public class OAuthSnelStart extends Authenticate {
	private Token tokenObject = null;
	
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
			if (!req.getParameterMap().containsKey("clientToken")) {
				req.getSession().setAttribute("clientToken", null);
				rd = req.getRequestDispatcher("snelStart.jsp");
				rd.forward(req, resp);
			} else {
				SnelStartHandler snelstart = new SnelStartHandler();
				String base64Key = req.getParameter("clientToken");
				String accessToken = snelstart.getAccessToken(base64Key);
				if (accessToken != null) {
					tokenObject = new Token();
					tokenObject.setAccessToken(accessToken);
					tokenObject.setAccessSecret(base64Key);
					tokenObject.setSoftwareName(softwareName);
					tokenObject.setSoftwareToken(softwareToken);
					// Save token to database
					try {
						TokenDAO.saveToken(tokenObject);
					} catch (SQLException e1) {
						e1.printStackTrace();
					}
					req.getSession().setAttribute("clientToken", "true");
					req.getSession().setAttribute("errorMessage", "true");
					// Set workstatusses once in WorkOrdeApp
					JSONArray JSONArray = new JSONArray();
					JSONObject JSONObject = null;
					try {
						JSONObject = new JSONObject();
						JSONObject.put("sta_code", "0");
						JSONObject.put("sta_name", "Niet naar SnelStart Online");
						JSONArray.put(JSONObject);
						JSONObject = new JSONObject();
						JSONObject.put("sta_code", "1");
						JSONObject.put("sta_name", "Naar SnelStart Online met uren");
						JSONArray.put(JSONObject);
						JSONObject.put("sta_code", "2");
						JSONObject.put("sta_name", "Naar SnelStart Online zonder uren");
						JSONArray.put(JSONObject);
						JSONObject = new JSONObject();
						JSONObject.put("sta_code", "99");
						JSONObject.put("sta_name", "Error");
						JSONArray.put(JSONObject);
						WorkOrderHandler.addData(softwareToken, JSONArray, "workstatusses", softwareName);
						// Set universal material for unknown hourtypes from WBA
						SnelStartHandler.setUniversalMaterial(tokenObject, "1000");
						req.getSession().setAttribute("materialCode", "1000");
					} catch (JSONException e) {
						e.printStackTrace();
					}
				} else {
					// Login page
					req.getSession().setAttribute("errorMessage", "Something went wrong");
					req.getSession().setAttribute("clientToken", null);
				}
				rd = req.getRequestDispatcher("snelStart.jsp");
				rd.forward(req, resp);
			}
		} else if (dbToken.getAccessSecret().equals("invalid")) {
			rd = req.getRequestDispatcher("snelStart.jsp");
			req.getSession().setAttribute("softwareToken", null);
			req.getSession().setAttribute("errorMessage",
					"softwareToken is already in use by: " + dbToken.getSoftwareName());
			rd.forward(req, resp);
		} else {
			req.getSession().setAttribute("softwareToken", dbToken.getSoftwareToken());
			req.getSession().setAttribute("clientToken", "true");
			ArrayList<Map<String, String>> allLogs = ObjectDAO.getLogs(softwareToken);
			if (!allLogs.isEmpty() || allLogs != null) {
				req.getSession().setAttribute("logs", null);
				req.getSession().setAttribute("logs", allLogs);
			}
			// get all typeofwork and paymendmethods
			ArrayList<String> typeofwork = (ArrayList<String>) WorkOrderHandler.getTypeofwork(softwareToken,
					softwareName, "worktypes", "NL");
			
			ArrayList<String> paymentMethod = (ArrayList<String>) WorkOrderHandler.getTypeofwork(softwareToken,
					softwareName, "paymentmethods", "NL");
			
			Settings set = ObjectDAO.getSettings(softwareToken);
			if (set != null) {
				req.getSession().setAttribute("clientToken", "pending");
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
				req.getSession().setAttribute("materialCode", set.getMaterialCode());
				req.getSession().setAttribute("errorMessage", "");
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
			rd = req.getRequestDispatcher("snelStart.jsp");
			rd.forward(req, resp);
		}
		
	}
}