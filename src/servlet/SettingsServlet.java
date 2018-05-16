package servlet;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.*;

import DAO.ObjectDAO;
import object.Settings;

public class SettingsServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private String redirect = System.getenv("REDIRECT");
	private String softwareName = null, factuurType = null, user = null, token = null;
	private String importOffice = null, exportOffice = null, exportWerkbonType = null, syncDate = null,
			materialCode = null;
	private int roundedHours = 1;
	private String[] exportTypes = null;
	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		syncDate = req.getParameter("syncDate");
		factuurType = req.getParameter("factuurType");
		softwareName = req.getParameter("softwareName");
		token = req.getParameter("softwareToken");
		user = req.getParameter("users");
		String[] importTypes = req.getParameterValues("importType");
		String[] materialGroupParameters = null;
		String[] projectFilterParameters = null;
		String[] materialTypesParameters = null;
		String[] statusGroupParameters = null; // processing status voor
												// TopDesk, opgeslagen in
												// import_office in DB
		String[] operatorGroupParameters = null; // operator groups voor
													// TopDesk, opgeslagen in
													// user in DB
		// For each integration another case;
		switch (softwareName) {
		case "Twinfield":
			importOffice = req.getParameter("offices");
			exportOffice = importOffice;
			exportTypes = req.getParameterValues("exportType");
			break;
		case "WeFact":
			exportWerkbonType = req.getParameter("exportWerkbon");
			roundedHours = Integer.parseInt(req.getParameter("roundedHours"));
			exportOffice = req.getParameter("hourDescription");
			break;
		case "eAccounting":
			exportWerkbonType = req.getParameter("exportWerkbon");
			roundedHours = Integer.parseInt(req.getParameter("roundedHours"));
			materialCode = req.getParameter("materialCode");
			importOffice = req.getParameter("typeofwork");
			exportOffice = req.getParameter("paymentmethod");
			req.getSession().setAttribute("ErrorMessage", "");
			break;
		case "Moloni":
			importOffice = req.getParameter("offices");
			exportWerkbonType = req.getParameter("exportWerkbon");
			roundedHours = Integer.parseInt(req.getParameter("roundedHours"));
			exportOffice = req.getParameter("typeofwork");
			// exportOffice = req.getParameter("paymentmethod");
			req.getSession().setAttribute("ErrorMessage", "");
			break;
		case "DriveFx":
			exportWerkbonType = req.getParameter("exportWerkbon");
			roundedHours = Integer.parseInt(req.getParameter("roundedHours"));
			exportOffice = req.getParameter("typeofwork");
			exportOffice = req.getParameter("paymentmethod");
			req.getSession().setAttribute("ErrorMessage", "");
			break;
		case "SageOne":
			exportWerkbonType = req.getParameter("exportWerkbon");
			roundedHours = Integer.parseInt(req.getParameter("roundedHours"));
			importOffice = req.getParameter("typeofwork");
			exportOffice = req.getParameter("paymentmethod");
			exportTypes = req.getParameterValues("exportType");
			req.getSession().setAttribute("ErrorMessage", "");
			break;
		case "SnelStart_Online":
			materialCode = req.getParameter("materialCode");
			exportWerkbonType = req.getParameter("exportWerkbon");
			roundedHours = Integer.parseInt(req.getParameter("roundedHours"));
			exportTypes = req.getParameterValues("exportType");
			req.getSession().setAttribute("ErrorMessage", "");
			break;
		case "Bouwsoft":
			materialGroupParameters = req.getParameterValues("materialGroups");
			exportWerkbonType = req.getParameter("exportWerkbon");
			roundedHours = Integer.parseInt(req.getParameter("roundedHours"));
			projectFilterParameters = req.getParameterValues("projectFilter");
			exportTypes = req.getParameterValues("exportType");
			req.getSession().setAttribute("ErrorMessage", "");
			break;
		case "Offective":
			materialCode = req.getParameter("materialCode");
			exportWerkbonType = req.getParameter("exportWerkbon");
			roundedHours = Integer.parseInt(req.getParameter("roundedHours"));
			exportTypes = req.getParameterValues("exportType");
			req.getSession().setAttribute("ErrorMessage", "");
			break;
		case "Teamleader":
			exportWerkbonType = req.getParameter("exportWerkbon");
			roundedHours = Integer.parseInt(req.getParameter("roundedHours"));
			exportTypes = req.getParameterValues("exportType");
			req.getSession().setAttribute("ErrorMessage", "");
			break;
		case "Visma":
			materialGroupParameters = req.getParameterValues("salesOrderTypes");
			materialTypesParameters = req.getParameterValues("materialType");
			exportWerkbonType = req.getParameter("exportWerkbon");
			roundedHours = Integer.parseInt(req.getParameter("roundedHours"));
			exportTypes = req.getParameterValues("exportType");
			exportOffice = req.getParameter("offices");
			exportTypes = req.getParameterValues("exportType");
			req.getSession().setAttribute("errorMessage", "");
		case "TopDesk":
			statusGroupParameters = req.getParameterValues("statusGroups");
			operatorGroupParameters = req.getParameterValues("operatorGroupSelect");
			exportWerkbonType = req.getParameterValues("factuurType")[0];
			exportOffice = req.getParameterValues("completeStatus")[0];
			
			// Check if a processing status is selected, to prevent nullpointer
			if (statusGroupParameters != null && statusGroupParameters.length > 0) {
				for (String statusGroup : statusGroupParameters) {
					
					// Check if given status for synced and completed orders
						// isn't selected as a status to import
					if (statusGroup.equals(exportWerkbonType) || statusGroup.equals(exportOffice)) {
						String errorMessage = "syncStatusError";
						
						// Redirect with error set
						RequestDispatcher rd = req.getRequestDispatcher("topDesk.jsp");
						req.getSession().setAttribute("errorMessage", errorMessage);
						req.getSession().setAttribute("clientToken", errorMessage);
						rd.forward(req, resp);
						// Set token to null to prevent the rest of the code
						// from executing
						token = null;
					}
				}
			}
			
			break;
		}
		
		if (token != null) {
			ArrayList<String> impTypes = new ArrayList<>();
			ArrayList<String> expTypes = new ArrayList<>();
			ArrayList<String> materialGroupsArray = new ArrayList<>();
			ArrayList<String> materialTypesArray = new ArrayList<>();
			Settings oldSettings = null;
			ArrayList<String> projectFilterArray = new ArrayList<>();
			try {
				oldSettings = ObjectDAO.getSettings(token);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String message = "";
			// Check if settings are changed for response message
			if (oldSettings != null && importTypes != null) {
				ArrayList<String> impTypesCheck = new ArrayList<>(Arrays.asList(importTypes));
				
				if (importOffice != null && !importOffice.equals(oldSettings.getImportOffice())
						&& softwareName.equals("Twinfield")) {
					message = "Administratie is opgeslagen<br />";
				}
				if (importOffice != null && !importOffice.equals(oldSettings.getImportOffice())
						&& softwareName.equals("EAccounting")) {
					message = "Worktype saved<br />";
				}
				if (exportOffice != null && !exportOffice.equals(oldSettings.getExportOffice())
						&& softwareName.equals("EAccounting")) {
					message += "Paymentmethod saved<br />";
				}
				if (importTypes != null && !impTypesCheck.equals(oldSettings.getImportObjects())) {
					message += "Import objects saved<br />";
				}
				if (importOffice != null && user != null && !user.equals(oldSettings.getUser())
						&& softwareName.equals("Twinfield")) {
					message += "Medewerker voor uurboeking is opgeslagen<br />";
				}
				if (exportWerkbonType != null && !exportWerkbonType.equals(oldSettings.getExportWerkbontype())) {
					message += "Workorder type saved<br />";
				}
				if (roundedHours != oldSettings.getRoundedHours()) {
					message += "Rounded hours saved<br />";
				}
				if (syncDate != null && !syncDate.equals(oldSettings.getSyncDate())) {
					message += "Synchronisation date saved<br />";
				}
				if (materialCode != null && !materialCode.equals(oldSettings.getMaterialCode())) {
					message += "Article number saved<br />";
				}
				if (factuurType != null && !factuurType.equals(oldSettings.getFactuurType())) {
					message += "WerkbonStatus saved<br />";
				}
				// First time saving settings
			} else {
				message = "Settings saved<br />";
			}
			req.getSession().setAttribute("checkSaved", message);
			if (exportTypes != null) {
				expTypes.addAll(Arrays.asList(exportTypes));
			} else {
				expTypes.add("");
			}
			if (materialGroupParameters != null) {
				for (String group : materialGroupParameters) {
					materialGroupsArray.add(group);
					importOffice = materialGroupsArray + "";
				}
			} else {
				materialGroupsArray.add("");
			}
			if (materialTypesParameters != null) {
				for (String group : materialTypesParameters) {
					materialTypesArray.add(group);
					user = materialTypesArray + "";
				}
			} else {
				materialTypesArray.add("");
				if (statusGroupParameters != null) {
					
					importOffice = "";
					for (String status : statusGroupParameters) {
						importOffice += status + ";";
					}
				}
				if (operatorGroupParameters != null) {
					user = "";
					for (String opGroup : operatorGroupParameters) {
						user += opGroup + ";";
					}
				}
				if (projectFilterParameters != null) {
					for (String filter : projectFilterParameters) {
						projectFilterArray.add(filter);
						exportOffice = projectFilterArray + "";
					}
				} else {
					projectFilterArray.add("");
				}
				if (importTypes != null) {
					impTypes.addAll(Arrays.asList(importTypes));
					Settings set = new Settings(importOffice, exportOffice, factuurType, impTypes, user,
							exportWerkbonType, roundedHours, syncDate, materialCode, expTypes);
					try {
						ObjectDAO.saveSettings(set, token);
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					// employees, projects, materials, relations and/or
					// hourtypes
					Settings checkbox = null;
					try {
						checkbox = ObjectDAO.getSettings(token);
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					ArrayList<String> checkboxes = null;
					if (checkbox != null) {
						checkboxes = checkbox.getImportObjects();
						if (checkboxes != null) {
							Settings set = new Settings(importOffice, exportOffice, factuurType, checkboxes, user,
									exportWerkbonType, roundedHours, syncDate, materialCode, expTypes);
							try {
								ObjectDAO.saveSettings(set, token);
							} catch (SQLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					} else {
						Settings set = new Settings(importOffice, exportOffice, factuurType, null, user,
								exportWerkbonType, roundedHours, syncDate, materialCode, expTypes);
						try {
							ObjectDAO.saveSettings(set, token);
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				if (redirect != null) {
					resp.sendRedirect(redirect + "OAuth.do?token=" + token + "&softwareName=" + softwareName);
				} else {
					resp.sendRedirect("https://www.localhost:8080/connect/OAuth.do?token=" + token + "&softwareName="
							+ softwareName);
				}
			}
		}
	}
}
