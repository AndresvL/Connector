package controller.trackjack;

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

import DAO.ObjectDAO;
import DAO.TokenDAO;
import controller.Authenticate;
import object.Settings;
import object.Token;

public class OAuthTrackJack extends Authenticate {
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
		TrackJackHandler track = new TrackJackHandler();
		if (dbToken == null) {
			if (!req.getParameterMap().containsKey("username")) {
				rd = req.getRequestDispatcher("trackjack.jsp");
				req.getSession().setAttribute("validLogin", null);
				// If clientToken exist
			} else {
				String username = req.getParameter("username");
				String password = req.getParameter("password");
				Token tokenObject = null;
				// Encodes username and password to base64
				// Checks if username and password are valid
				String base64Credentials = track.checkCredentials(username, password);
				// base64Credentials != null when api response returns code 200
				if (base64Credentials != null) {
					tokenObject = new Token();
					tokenObject.setAccessToken(base64Credentials);
					tokenObject.setSoftwareName(softwareName);
					tokenObject.setSoftwareToken(softwareToken);
					// Save token to database
					try {
						TokenDAO.saveToken(tokenObject);
					} catch (SQLException e1) {
						e1.printStackTrace();
					}
					rd = req.getRequestDispatcher("trackjack.jsp");
					req.getSession().setAttribute("validLogin", "true");
					// // Set workstatusses once in WorkOrdeApp
					// JSONArray JSONArray = new JSONArray();
					// JSONObject JSONObject = null;
					// try {
					//
					// JSONObject = new JSONObject();
					// JSONObject.put("sta_code", "0");
					// JSONObject.put("sta_name", "Concept factuur");
					// JSONArray.put(JSONObject);
					//
					// JSONObject = new JSONObject();
					// JSONObject.put("sta_code", "2");
					// JSONObject.put("sta_name", "Verzonden");
					// JSONArray.put(JSONObject);
					//
					// JSONObject = new JSONObject();
					// JSONObject.put("sta_code", "99");
					// JSONObject.put("sta_name", "Error");
					// JSONArray.put(JSONObject);
					//
					// System.out.println(JSONArray);
					// WorkOrderHandler.addData(softwareToken, JSONArray,
					// "workstatusses", softwareName, clientToken);
					// } catch (JSONException e) {
					// e.printStackTrace();
					// }
				} else {
					// Login page
					rd = req.getRequestDispatcher("trackjack.jsp");
					req.getSession().setAttribute("errorMessage", "Username or password is incorrect");
					req.getSession().setAttribute("validLogin", null);
				}
			}
		} else if (dbToken.getAccessSecret().equals("invalid")) {
			rd = req.getRequestDispatcher("trackjack.jsp");
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
			if (ObjectDAO.getProgress(softwareToken) == 2) {
				ObjectDAO.saveProgress(3, softwareToken);
			}
			// Get settings from database
			Settings set = ObjectDAO.getSettings(softwareToken);
			if (set != null) {
				
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
			}
			rd = req.getRequestDispatcher("trackjack.jsp");
		}
		rd.forward(req, resp);
	}
}