package controller.wefact;

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

public class OAuthWeFact extends Authenticate {
	private Token tokenObject;
	
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
		WeFactHandler we = new WeFactHandler();
		if (dbToken == null) {
			// If parameter clientToken doesn't exist
			if (!req.getParameterMap().containsKey("clientToken")) {
				rd = req.getRequestDispatcher("weFact.jsp");
				req.getSession().setAttribute("clientToken", null);
				// If clientToken exist
			} else {
				String clientToken = req.getParameter("clientToken");
				Object obj = we.checkClientToken(clientToken);
				// Check if object is boolean; if true object is always true
				if (obj instanceof Boolean && (boolean) obj) {
					tokenObject = new Token();
					tokenObject.setAccessToken(clientToken);
					tokenObject.setSoftwareName(softwareName);
					tokenObject.setSoftwareToken(softwareToken);
					// Save token to database
					try {
						TokenDAO.saveToken(tokenObject);
					} catch (SQLException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					rd = req.getRequestDispatcher("weFact.jsp");
					req.getSession().setAttribute("clientToken", clientToken);
					System.out.println("Session clientToken " + clientToken);
					req.getSession().setAttribute("errorMessage", "true");
					// Set workstatusses once in WorkOrdeApp
					JSONArray JSONArray = new JSONArray();
					JSONObject JSONObject = null;
					try {
						
						JSONObject = new JSONObject();
						JSONObject.put("sta_code", "0");
						JSONObject.put("sta_name", "Concept factuur");
						JSONArray.put(JSONObject);
						
						JSONObject = new JSONObject();
						JSONObject.put("sta_code", "2");
						JSONObject.put("sta_name", "Verzonden");
						JSONArray.put(JSONObject);
						
						JSONObject = new JSONObject();
						JSONObject.put("sta_code", "99");
						JSONObject.put("sta_name", "Error");
						JSONArray.put(JSONObject);
						
						System.out.println(JSONArray);
						WorkOrderHandler.addData(softwareToken, JSONArray, "workstatusses", softwareName);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				} else {
					// Login page
					rd = req.getRequestDispatcher("weFact.jsp");
					req.getSession().setAttribute("errorMessage", obj);
					req.getSession().setAttribute("clientToken", null);
				}
			}
		} else if (dbToken.getAccessSecret().equals("invalid")) {
			rd = req.getRequestDispatcher("weFact.jsp");
			req.getSession().setAttribute("clientToken", null);
			req.getSession().setAttribute("errorMessage",
					"softwareToken is al in gebruik door " + dbToken.getSoftwareName());
		} else {
			Object obj = we.checkClientToken(dbToken.getAccessToken());
			req.getSession().setAttribute("errorMessage", "");
			if (obj instanceof Boolean && (boolean) obj) {
				req.getSession().setAttribute("clientToken", dbToken.getAccessToken());
			} else {
				req.getSession().setAttribute("errorMessage", obj);
				req.getSession().setAttribute("clientToken", null);
				if (obj.toString().startsWith("De API sleutel")) {
					try {
						TokenDAO.deleteToken(softwareToken);
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
			ArrayList<Map<String, String>> allLogs = ObjectDAO.getLogs(softwareToken);
			if (!allLogs.isEmpty() || allLogs != null) {
				req.getSession().setAttribute("logs", null);
				req.getSession().setAttribute("logs", allLogs);
			}
			
			Settings set = ObjectDAO.getSettings(softwareToken);
			if (set != null) {
				Map<String, String> allImports = new HashMap<String, String>();
				for (String s : set.getImportObjects()) {
					allImports.put(s, "selected");
				}
				Map<String, String> exportWerkbonType = new HashMap<String, String>();
				exportWerkbonType.put(set.getExportWerkbontype(), "selected");
				req.getSession().setAttribute("exportWerkbonType", exportWerkbonType);
				
				Map<String, String> hourDescription = new HashMap<String, String>();
				hourDescription.put(set.getExportOffice(), "selected");
				req.getSession().setAttribute("hourDescription", hourDescription);
				
				req.getSession().setAttribute("savedDate", set.getSyncDate());
				req.getSession().setAttribute("checkboxes", allImports);
				req.getSession().setAttribute("roundedHours", set.getRoundedHours());
				req.getSession().setAttribute("factuur", set.getFactuurType());
			}
			rd = req.getRequestDispatcher("weFact.jsp");
		}
		rd.forward(req, resp);
		
	}
	
}
