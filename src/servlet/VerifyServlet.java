package servlet;

import java.io.IOException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import DAO.TokenDAO;
import controller.WorkOrderHandler;
import controller.eaccouting.OAuthEAccounting;
import controller.moloni.MoloniHandler;
import controller.moloni.OAuthMoloni;
import controller.sageone.OAuthSageOne;
import controller.twinfield.OAuthTwinfield;
import controller.twinfield.SoapHandler;
import controller.visma.OAuthVisma;
import object.Token;
import object.twinfield.Search;

public class VerifyServlet extends HttpServlet {
	
	private String redirect = System.getenv("REDIRECT");
	private static final long serialVersionUID = 1L;
	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String softwareName = (String) req.getSession().getAttribute("softwareName");
		System.out.println("SOFTWARENAME = " + softwareName);
		String softwareToken = null;
		String code = null;
		String error = null;
		Token t = null;
		switch (softwareName) {
		case "Twinfield":
			String temporaryToken = req.getParameter("oauth_token");
			String temporaryVerifier = req.getParameter("oauth_verifier");
			OAuthTwinfield oauth = new OAuthTwinfield();
			
			Token token = oauth.getAccessToken(temporaryToken, temporaryVerifier, softwareName);
			String sessionID = null;
			String cluster = null;
			String[] array = SoapHandler.getSession(token, sessionID, cluster);
			sessionID = array[0];
			cluster = array[1];
			@SuppressWarnings("unchecked")
			ArrayList<String> offices = (ArrayList<String>) SoapHandler.createSOAPXML(sessionID, cluster,
					"<list><type>offices</type></list>", "office");
			// get all users
			ArrayList<Map<String, String>> users = new ArrayList<Map<String, String>>();
			Search searchObject = new Search("USR", "*", 0, 1, 100, null);
			ArrayList<String> responseArray = SoapHandler.createSOAPFinder(sessionID, cluster, searchObject);
			for (String s : responseArray) {
				Map<String, String> allUsers = new HashMap<String, String>();
				String[] split = s.split(",");
				allUsers.put("code", split[0]);
				allUsers.put("name", split[1]);
				users.add(allUsers);
			}
			
			req.getSession().setAttribute("offices", offices);
			req.getSession().setAttribute("users", users);
			req.getSession().setAttribute("softwareToken", token.getSoftwareToken());
			req.getSession().setAttribute("session", sessionID);
			req.getSession().setAttribute("cluster", cluster);
			if (redirect != null) {
				resp.sendRedirect(
						redirect + "OAuth.do?token=" + token.getSoftwareToken() + "&softwareName=" + softwareName);
			} else {
				resp.sendRedirect("https://www.localhost:8080/connect/OAuth.do?token=" + token.getSoftwareToken()
						+ "&softwareName=" + softwareName);
			}
			break;
		case "eAccounting":
			softwareToken = (String) req.getSession().getAttribute("softwareToken");
			code = req.getParameter("code");
			error = req.getParameter("error");
			if (code != null) {
				System.out.println("CODE " + code);
				t = OAuthEAccounting.getAccessToken(code, null, softwareName, softwareToken);
			} else if (error != null) {
				System.out.println("Error EAccounting authentication: " + error);
				break;
			}
			System.out.println("accessToken " + t.getAccessToken());
			System.out.println("refreshToken(secret) " + t.getAccessSecret());
			System.out.println("clientID " + t.getConsumerToken());
			System.out.println("clientSecret " + t.getConsumerSecret());
			System.out.println("softwareToken " + t.getSoftwareToken());
			System.out.println("softwareName " + t.getSoftwareName());
			
			if (redirect != null) {
				resp.sendRedirect(
						redirect + "OAuth.do?token=" + t.getSoftwareToken() + "&softwareName=" + softwareName);
			} else {
				resp.sendRedirect("https://www.localhost:8080/connect/OAuth.do?token=" + t.getSoftwareToken()
						+ "&softwareName=" + softwareName);
			}
			req.getSession().setAttribute("ErrorMessage", "true");
			break;
		case "Moloni":
			softwareToken = (String) req.getSession().getAttribute("softwareToken");
			code = req.getParameter("code");
			System.out.println("CODE " + code);
			error = req.getParameter("error");
			if (code != null) {
				t = OAuthMoloni.getAccessToken(code, null, softwareName, softwareToken);
			} else if (error != null) {
				System.out.println("Error Moloni authentication: " + error);
				break;
			}
			ArrayList<Map<String, String>> offices1 = (ArrayList<Map<String, String>>) MoloniHandler.getOffices(t);
			req.getSession().setAttribute("offices", offices1);
			System.out.println("accessToken " + t.getAccessToken());
			System.out.println("refreshToken(secret) " + t.getAccessSecret());
			System.out.println("clientID " + t.getConsumerToken());
			System.out.println("clientSecret " + t.getConsumerSecret());
			System.out.println("softwareToken " + t.getSoftwareToken());
			System.out.println("softwareName " + t.getSoftwareName());
			if (redirect != null) {
				resp.sendRedirect(
						redirect + "OAuth.do?token=" + t.getSoftwareToken() + "&softwareName=" + softwareName);
			} else {
				resp.sendRedirect("https://www.localhost:8080/connect/OAuth.do?token=" + t.getSoftwareToken()
						+ "&softwareName=" + softwareName);
			}
			req.getSession().setAttribute("ErrorMessage", "true");
			break;
		case "SageOne":
			softwareToken = (String) req.getSession().getAttribute("softwareToken");
			code = req.getParameter("code");
			error = req.getParameter("error");
			if (code != null) {
				System.out.println("CODE " + code);
				t = OAuthSageOne.getAccessToken(code, null, softwareName, softwareToken);
			} else if (error != null) {
				System.out.println("Error SageOne authentication: " + error);
				break;
			}
			System.out.println("accessToken " + t.getAccessToken());
			System.out.println("refreshToken(secret) " + t.getAccessSecret());
			System.out.println("clientID " + t.getConsumerToken());
			System.out.println("clientSecret " + t.getConsumerSecret());
			System.out.println("softwareToken " + t.getSoftwareToken());
			System.out.println("softwareName " + t.getSoftwareName());
			
			if (redirect != null) {
				resp.sendRedirect(
						redirect + "OAuth.do?token=" + t.getSoftwareToken() + "&softwareName=" + softwareName);
			} else {
				resp.sendRedirect("https://www.localhost:8080/connect/OAuth.do?token=" + t.getSoftwareToken()
						+ "&softwareName=" + softwareName);
			}
			req.getSession().setAttribute("ErrorMessage", "true");
			break;
		case "Bouwsoft":
			softwareToken = (String) req.getSession().getAttribute("softwareToken");
			System.out.println("bouwsoft " + softwareToken);
			String refreshToken = req.getParameter("refreshtoken");
			String clientNr = req.getParameter("clientnr");
			String serverName = req.getParameter("servername");
			String serverIp = req.getParameter("serverip");
			String accessToken = req.getParameter("accesstoken");
			String validUntil = req.getParameter("validuntil");
			if (refreshToken != null) {
				Token dbToken = new Token();
				dbToken.setAccessToken(accessToken);
				dbToken.setAccessSecret(refreshToken);
				dbToken.setConsumerToken(clientNr);
				dbToken.setConsumerSecret(serverName);
				dbToken.setSoftwareName(softwareName);
				dbToken.setSoftwareToken(softwareToken);
				try {
					TokenDAO.saveToken(dbToken);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("refreshToken " + refreshToken);
				System.out.println("clientNr " + clientNr);
				System.out.println("serverName " + serverName);
				System.out.println("serverIp " + serverIp);
				System.out.println("accessToken " + accessToken);
				System.out.println("validUntil " + validUntil);
			}
			
			if (redirect != null) {
				resp.sendRedirect(redirect + "OAuth.do?token=" + softwareToken + "&softwareName=" + softwareName);
			}
			req.getSession().setAttribute("ErrorMessage", "true");
			break;
		case "Visma":
			softwareToken = (String) req.getSession().getAttribute("softwareToken");
			code = req.getParameter("code");
			error = req.getParameter("error");
			if (code != null) {
				System.out.println("CODE " + code);
				t = OAuthVisma.getAccessToken(code, null, softwareName, softwareToken);
				JSONArray JSONArray = new JSONArray();
				JSONObject JSONObject = null;
				try {
					
					JSONObject = new JSONObject();
					JSONObject.put("sta_code", "0");
					JSONObject.put("sta_name", "Niet naar Visma");
					JSONArray.put(JSONObject);
					
					JSONObject = new JSONObject();
					JSONObject.put("sta_code", "1");
					JSONObject.put("sta_name", "Naar Visma");
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
			} else if (error != null) {
				System.out.println("Error Visma authentication: " + error);
				break;
			}
			System.out.println("accessToken " + t.getAccessToken());
			System.out.println("refreshToken(secret) " + t.getAccessSecret());
			System.out.println("clientID " + t.getConsumerToken());
			System.out.println("clientSecret " + t.getConsumerSecret());
			System.out.println("softwareToken " + t.getSoftwareToken());
			System.out.println("softwareName " + t.getSoftwareName());
			
			if (redirect != null) {
				resp.sendRedirect(
						redirect + "OAuth.do?token=" + t.getSoftwareToken() + "&softwareName=" + softwareName);
			} else {
				resp.sendRedirect("https://www.localhost:8080/connect/OAuth.do?token=" + t.getSoftwareToken()
						+ "&softwareName=" + softwareName);
			}
			req.getSession().setAttribute("validLogin", "ok");
			req.getSession().setAttribute("errorMessage", "true");
			break;
		}
	}
}
