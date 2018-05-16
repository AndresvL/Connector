<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="description" content="">
<meta name="author" content="">
<title>Twinfield Koppeling</title>
<!-- Custom CSS -->
<link href="css/custom.css" rel="stylesheet">
<!-- Bootstrap Core CSS -->
<link href="vendor/bootstrap/css/bootstrap.min.css" rel="stylesheet">
<!-- Theme CSS -->
<link href="css/freelancer.min.css" rel="stylesheet">
<!-- Custom Fonts -->
<link href="vendor/font-awesome/css/font-awesome.min.css"
	rel="stylesheet" type="text/css">
<link href="https://fonts.googleapis.com/css?family=Montserrat:400,700"
	rel="stylesheet" type="text/css">
<link
	href="https://fonts.googleapis.com/css?family=Lato:400,700,400italic,700italic"
	rel="stylesheet" type="text/css">
<!-- Sweet Alert -->
<script src="sweetalert2/sweetalert2.min.js"></script>
<link rel="stylesheet" href="sweetalert2/sweetalert2.min.css">
<!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries -->
<!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
<!--[if lt IE 9]>
<script src="https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js"></script>
<script src="https://oss.maxcdn.com/libs/respond.js/1.4.2/respond.min.js"></script> 
 <![endif]-->
<style>
.settings {
	width: 50%;
	float: left;
}

#help {
	float: right;
}

#show {
	float: right;
}

#savebutton {
	margin-right: 10px;
	float: right;
}

#syncbutton {
	float: right;
}
</style>
</head>
<body>
	<!-- Settings Section -->
	<div id="WBA-section">
		<img src="./img/werkbonapp.png" height="60" width="170" id="WBA_logo" />
		<img src="./img/Twinfield-logo.png" height="80" width="180"
			id="boekhoud_logo" />
	</div>
	<!-- Settings Section -->
	<div class="settings" style="display: none;">
		<div class="panel-group">
			<!-- The Modal -->
			<div id="myModal" class="modal">
				<!-- Modal content -->
				<div class="modal-content">
					<div class="modal-header">
						<span class="close">&times;</span>
						<h2>
							Welkom <small>bij de Twinfield koppeling</small>
						</h2>
					</div>
					<div class="modal-body">
						<h4>Informatie</h4>
						- Op deze pagina is het mogelijk om de
						<mark>import</mark>
						en
						<mark>export</mark>
						gegevens tussen WerkbonApp en Twinfield in te stellen<br> -
						Elke 15 minuten zal er een
						<mark>automatische synchronisatie</mark>
						plaatsvinden aan de hand van deze instellingen.<br> - Het is
						mogelijk om
						<mark>handmatig een synchronisatie uit te voeren</mark>
						door onderaan op de knop Start Synchronisation te klikken.<br>
						<br>
						<h4>Belangrijk</h4>
						<b> Let op! Twinfield is leidend. <br>- De <abbr>import
								settings</abbr> mogen alleen in Twinfield worden gewijzigd. <br> -
							Bij een <abbr>foutmelding</abbr> kan je op het bericht(log)
							klikken om meer details te zien. <br> <br>
						</b>
						<button type="button" id="show" class="btn btn-info">Show</button>
						<h4>Data Mapping</h4>
						<div id="mappingTable" style="display: none;">
							<table class="table table-hover">
								<thead>
									<tr>
										<th>WerkbonApp</th>
										<th>Twinfield</th>
									</tr>
								</thead>
								<tbody>
									<tr>
										<th colspan="2">Import</th>
									</tr>
									<tr>
										<td>Medewerkers</td>
										<td>Gebruikers</td>
									</tr>
									<tr>
										<td>Projecten</td>
										<td>Projecten</td>
									</tr>
									<tr>
										<td>Materialen</td>
										<td>Artikelen</td>
									</tr>
									<tr>
										<td>Relaties</td>
										<td>Debiteuren</td>
									</tr>
									<tr>
										<td>Uursoorten</td>
										<td>Aktiviteiten</td>
									</tr>
									<tr>
										<th colspan="2">Export</th>
									</tr>
									<tr>
										<td>Werkbon (zonder projectNr)</td>
										<td>Factuur</td>
									</tr>
									<tr>
										<td>Werkbon (met projectNr)</td>
										<td>Urenboeking</td>
									</tr>
								</tbody>
							</table>
						</div>
						<br>
						<h4>Gebruikersrollen</h4>
						In Twinfield <abbr>moet</abbr> de volgende gebruikersrol worden
						toegevoegd.<br> <br>
						<div class="image">
							<img src="./img/rol.png" width="100%" height="100%"
								data-toggle="tooltip"
								title="Ga naar Projecten > Uren & onkosten > Tarieven & toegang" />
						</div>
					</div>
					<div class="modal-footer">
						<p>WorkOrderApp B.V.</p>
					</div>
				</div>
			</div>
			<input type="hidden" value="${session}" id="session" />
			<input type="hidden" value="${error}" id="error" />
			<input type="hidden" value="${saved}" id="saved" />
			<input type="hidden" value="Twinfield" id="softwareName" />
			<form action="settings.do" id="save">
				<div class="panel panel-success">
					<div class="panel-heading">Import instellingen</div>
					<div class="panel-body">
						<div class="row control-group">
							<div class="form-group col-xs-12 floating-label controls">
								<input type="hidden" value="${softwareName}" name="softwareName" />
								<input type="hidden" value="${softwareToken}"
									name="softwareToken" />
								<label>Administratie</label>
								<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="Kies een administratie waaruit de objecten geimporteerd moeten worden"
									height="13" width="13" />
								<select name="offices" class="form-control" id="officelist"
									required>
									<option disabled selected value>-- Selecteer een
										administratie --</option>
									<c:forEach items="${offices}" var="office">
										<option value="${office.code}"
											${office.code == importOffice ? 'selected="selected"' : ''}>
											${office.name}</option>
									</c:forEach>
								</select><br>
								<label>Selecteer objecten om te importeren</label>
								<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="Selecteer de objecten die je wilt importeren van Twinfield naar WerkbonApp"
									height="13" width="13" />
								<div class="checkbox">
									<label>
										<input type="checkbox" value="employees"
											${"selected" == checkboxes.employees  ? 'checked' : ''}
											name="importType" id="employees">
										Medewerkers
									</label>
								</div>
								<div class="checkbox">
									<label>
										<input type="checkbox" value="projects"
											${"selected" == checkboxes.projects  ? 'checked' : ''}
											name="importType" id="projects">
										Projecten
									</label>
								</div>
								<div class="checkbox">
									<label>
										<input type="checkbox" value="materials"
											${"selected" == checkboxes.materials  ? 'checked' : ''}
											name="importType">
										Materialen
									</label>
								</div>
								<div class="checkbox">
									<label>
										<input type="checkbox" value="relations"
											${"selected" == checkboxes.relations  ? 'checked' : ''}
											name="importType" id="relations">
										Relaties
									</label>
								</div>
								<div class="checkbox">
									<label>
										<input type="checkbox" value="hourtypes"
											${"selected" == checkboxes.hourtypes  ? 'checked' : ''}
											name="importType">
										Uursoorten
									</label>
								</div>
								<button type="button" id="help" class="btn btn-info btn-lg">Help</button>
							</div>
						</div>
					</div>
				</div>
				<div class="panel panel-success">
					<div class="panel-heading">Export instellingen</div>
					<div class="panel-body">
						<div class="row control-group">
							<div class="form-group col-xs-12 floating-label controls">
								<label>Administratie</label>
								<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="Export administratie is hetzelfde als import administratie"
									height="13" width="13" />
								<select name="exportOffices" class="form-control"
									id="officeExportList" disabled>
									<option>-- Selecteer een administratie --</option>
									<c:forEach items="${offices}" var="office">
										<option value="${office.code}"
											${office.code == exportOffice ? 'selected="selected"' : ''}>${office.name}
										</option>
									</c:forEach>
								</select><br>
								<label>Werkbon opties</label>
								<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="Onbekende relaties op een werkbon zullen nieuw aangemaakt worden in Twinfield, als deze optie is geselecteerd"
									height="13" width="13" />
								<div class="checkbox">
									<label>
										<input type="checkbox" class ="exportRelation" value="relations"
											${"selected" == exportCheckboxes.relations  ? 'checked' : ''}
											name="exportType">
										 relatie
									</label>
								</div>
								

								<label>Medewerker</label>
								<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="Deze medewerker wordt gekoppeld aan alle uurboekingen"
									height="13" width="13" />
								<select name="users" class="form-control" id="userList" required>
									<!-- <option disabled selected value>-- Selecteer een medewerker --</option> -->
									<option selected value="Geen"
										${"Geen" == setUser ? 'selected="selected"' : ''}>Geen</option>
									<c:forEach items="${users}" var="user">
										<option value="${user.code}"
											${user.code == setUser ? 'selected="selected"' : ''}>
											${user.name}</option>
									</c:forEach>
								</select>
								<input class="form-control" type="hidden" name="exportOffices"
									id="exportOfficeValue" />
							</div>
							<div class="form-group col-xs-12 floating-label controls">
								<label>Werkbonstatus</label>
								<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="De werkbonnen met status compleet worden opgehaald"
									height="13" width="13" />
								<select name="factuurType" class="form-control" id="uren"
									required>
									<option selected value="compleet"
										${"compleet" == factuur ? 'selected="selected"' : ''}>Compleet</option>
									<option value="afgehandeld"
										${"afgehandeld" == factuur ? 'selected="selected"' : ''}>Afgehandeld</option>
								</select>
							</div>
							<br>
						</div>
						<br>
						<div class="row">
							<div class="form-group col-xs-12">
								<input type="submit" class="btn btn-success btn-lg" value="Sync"
									id="syncbutton" />
								<input type="submit" class="btn btn-success btn-lg" value="Save"
									name="category" id="savebutton" />
							</div>
						</div>
					</div>
				</div>
			</form>
		</div>
	</div>
	<!-- this form will be validated after syncbutton is pressed  -->
	<form action="sync.do" id="sync">
		<input type="hidden" value="${softwareToken}" name="token" />
		<input type="hidden" value="${softwareName}" name="softwareName" />
	</form>
	<div class="settings" style="display: none;">
		<div class="panel panel-success">
			<div class="panel-heading">Log</div>
			<div class="panel-body">
				<div class="row control-group">
					<div class="form-group col-xs-12 floating-label controls">
						<table class="table table-hover">
							<thead>
								<tr>
									<th>Tijd</th>
									<th>Bericht</th>
								</tr>
							</thead>
							<tbody>
								<c:forEach items="${logs}" var="log">
									<tr class="showDetails" data-href='${log.details}'>
										<td>${log.timestamp}</td>
										<td>${log.message}</td>
									</tr>
								</c:forEach>
							</tbody>
						</table>
					</div>
				</div>
			</div>
		</div>
	</div>
	<!-- jQuery -->
	<script src="vendor/jquery/jquery.min.js"></script>
	<!-- Bootstrap Core JavaScript -->
	<script src="vendor/bootstrap/js/bootstrap.min.js"></script>
	<!-- Plugin JavaScript -->
	<script
		src="https://cdnjs.cloudflare.com/ajax/libs/jquery-easing/1.3/jquery.easing.min.js"></script>
	<!-- Contact Form JavaScript -->
	<script src="js/jqBootstrapValidation.js"></script>
	<script src="js/contact_me.js"></script>
	<!-- Theme JavaScript -->
	<script src="js/freelancer.min.js"></script>
	<script type="text/javascript" src="js/vkbeautify.js"></script>
	<script type="text/javascript" src="js/twinfield.js"></script>
</body>
</html>
