package controller.teamleader;

import DAO.ObjectDAO;
import DAO.TokenDAO;
import controller.Authenticate;
import object.Settings;
import object.Token;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class OAuthTeamleader extends Authenticate {
	
	@Override
	public void authenticate(String softwareToken, HttpServletRequest req, HttpServletResponse resp)
			throws ClientProtocolException, IOException, ServletException, SQLException {
		RequestDispatcher rd = null;
		String softwareName = (String) req.getSession().getAttribute("softwareName");
		Token dbToken = null;
		req.getSession().setAttribute("api_secret", null);
		req.getSession().setAttribute("api_group", null);
		req.getSession().setAttribute("errorMessage", null);
		
		// Get token from database
		try {
			dbToken = TokenDAO.getToken(softwareToken, softwareName);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		// First login
		if (dbToken == null) {
			if (req.getParameterMap().containsKey("api_group") && req.getParameterMap().containsKey("api_secret")) {
				String api_secret = req.getParameter("api_secret");
				String api_group = req.getParameter("api_group");
				
				Token tokenObject = new Token();
				tokenObject.setAccessSecret(api_group);
				tokenObject.setAccessToken(api_secret);
				tokenObject.setSoftwareName(softwareName);
				tokenObject.setSoftwareToken(softwareToken);
				
				try {
					TeamleaderHandler.handleRequest("helloWorld.php", tokenObject, null, String.class);
					TokenDAO.saveToken(tokenObject);
					req.getSession().setAttribute("errorMessage", "true");
					req.getSession().setAttribute("api_secret", api_secret);
					req.getSession().setAttribute("api_group", api_group);
				} catch (HttpResponseException e) {
					req.getSession().setAttribute("errorMessage", e.getMessage());
				}
			}
			// If token is already used
		} else if (dbToken.getAccessSecret().equals("invalid")) {
			req.getSession().setAttribute("softwareToken", null);
			req.getSession().setAttribute("errorMessage",
					"softwareToken is al in gebruik door " + dbToken.getSoftwareName());
			// Login successfull
		} else {
			req.getSession().setAttribute("softwareToken", dbToken.getSoftwareToken());
			req.getSession().setAttribute("api_secret", dbToken.getAccessToken());
			req.getSession().setAttribute("api_group", dbToken.getAccessSecret());
			ArrayList<Map<String, String>> allLogs = ObjectDAO.getLogs(softwareToken);
			if (allLogs != null && !allLogs.isEmpty()) {
				req.getSession().setAttribute("logs", allLogs);
			}
			if (ObjectDAO.getProgress(softwareToken) == 2) {
				ObjectDAO.saveProgress(3, softwareToken);
			}
			// Get settings from database
			Settings set = ObjectDAO.getSettings(softwareToken);
			if (set != null) {
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
			}
		}
		req.getRequestDispatcher("teamleader.jsp").forward(req, resp);
	}
	
}