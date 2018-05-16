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
<title>Visma.net Connection</title>
<!-- datepicker CSS -->
<link
	href="//cdn.rawgit.com/Eonasdan/bootstrap-datetimepicker/e8bddc60e73c1ec2475f827be36e1957af72e2ea/build/css/bootstrap-datetimepicker.css"
	rel="stylesheet">
<!-- Custom CSS -->
<link href="css/custom.css" rel="stylesheet">
<link href="css/custom2.css" rel="stylesheet">
<link href="css/loading.css" rel="stylesheet">
<script type="text/javascript"
	src="//code.jquery.com/jquery-2.1.1.min.js"></script>
<script
	src="//cdnjs.cloudflare.com/ajax/libs/moment.js/2.9.0/moment-with-locales.js"></script>
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
<script src="sweetalert2/sweetalert2.min.js" type="text/javascript"></script>
<link rel="stylesheet" href="sweetalert2/sweetalert2.min.css">
<!-- Latest compiled and minified CSS -->
<link rel="stylesheet"
	href="https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.12.4/css/bootstrap-select.min.css">

<!-- Latest compiled and minified JavaScript -->
<script
	src="https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.12.4/js/bootstrap-select.min.js"></script>
<body>
	<div class="affixImg" data-spy="affix">
		<img src="./img/loading.gif" height="60" width="60" id="loading"
			style="display: none;" />
		<h6 id="loadingLabel" style="display: none;">synchronizeren...</h6>
	</div>
	<!-- Settings Section -->
	<div id="WBA-section">
		<img src="./img/werkbonapp.png" height="60" width="170" id="WBA_logo" />
		<img src="./img/logo_visma.png" height="60" width="200"
			id="boekhoud_logo" />
	</div>
	<div class="settings">

		<div class="panel-group">
			<div id="loginModal" class="modal">
				<!-- Modal content -->
				<div class="modal-content" id="loginContent">
					<form action="OAuth.do">
						<input type="hidden" value="${softwareToken}" name="token"
							id="softwareToken" />
						<input type="hidden" value="Visma" name="softwareName"
							id="softwareName" />
						<input type="hidden" value="${validLogin}" id="validLogin" />
						<table>
							<tr>
								<th>
									<h2>Login</h2>
								</th>
							</tr>
							<tr>
								<td><label id="errorMessage">${errorMessage}</label></td>
							</tr>
						</table>
						<br>
						<input type="submit" id="loginButton" value="Login"
							class="btn btn-success btn-lg" />
					</form>
				</div>
			</div>
			<!-- The Help Modal -->
			<div id="myModal" class="modal">
				<!-- Modal content -->
				<div class="modal-content">
					<div class="modal-header">
						<span class="close">&times;</span>
						<h2>
							Welkom <small>bij de Visma.net koppeling</small>
						</h2>
					</div>
					<div class="modal-body">
						<h4>Data mapping & Filters</h4>
						<table class="table table-hover">
							<thead>
								<tr>
									<th>WerkbonApp</th>
									<th>Visma.net</th>
									<th>Filter</th>
								</tr>
							</thead>
							<tbody>
								<tr>
									<th colspan="2">Import</th>
								</tr>
								<tr>
									<td>Medewerkers</td>
									<td>Medewerkers</td>
									<td>Lastupdate groter dan synchroniseerdatum</td>
								</tr>
								<tr>
									<td>Materialen</td>
									<td>Artikelen</td>
									<td>Lastupdate groter dan synchroniseerdatum en eenheid is <b>geen</b> UUR of HOUR </td>
								</tr>
								<tr>
									<td>Uursoorten</td>
									<td>Artikelen</td>
									<td>Lastupdate groter dan synchroniseerdatum en eenheid <b>is</b> UUR of HOUR</td>
								</tr>
								<tr>
									<td>Projecten</td>
									<td>Projecten</td>
									<td>Optioneel als dit pakket is ingeschakeld</td>
								</tr>
								<tr>
									<td>Relaties</td>
									<td>Debiteuren</td>
									<td>Lastupdate groter dan synchroniseerdatum</td>
								</tr>
								<tr>
									<td>Werkbonnen</td>
									<td>VerkoopOrders</td>
									<td>Verkooporders met Status 'Open' en met de geselecteerd ordersoorten worden geimporteerd </td>
								</tr>
								<tr>
									<th colspan="3">Export</th>
								</tr>
								<tr>
									<td>Werkbonnen</td>
									<td>Verkoopfacturen</td>
									<td>Status compleet</td>
								</tr>
							</tbody>
						</table>
						<br>
					</div>
					<div class="modal-footer">
						<p>WorkOrderApp B.V.</p>
					</div>
				</div>
			</div>
			<input type="hidden" value="${errorMessage}" id="error" />
			<input type="hidden" value="${saved}" id="saved" name="saved" />
			<form action="settings.do" id="saveVisma">
				<div class="panel panel-success">
					<div class="panel-heading" id="import-panel">Import
						instellingen</div>

					<div class="panel-body">
						<div class="row control-group">
							<div class="form-group col-xs-12 floating-label controls">
								<input type="hidden" value="${softwareName}" name="softwareName" />
								<input type="hidden" value="${softwareToken}"
									name="softwareToken" id="client" />
								<div class='[ col-xs-20 col-sm-10 ]'>
									<label>Bedrijfsnaam</label>
									<img src="./img/vraagteken.jpg" height="13" width="13"
										data-toggle="tooltip"
										title="Selecteer de betreffende administratie" />
									<br>
									<div class="[ form-group ]" id="office">
										<select name="offices" class="selectpicker" data-width="60%"
											required>
											<option disabled selected value>-- Selecteer een
												administratie --</option>

											<c:forEach items="${offices}" var="office">
												<option value="${office.key}"
													${office.key == importOffice ? 'selected="selected"' : ''}>
													${office.value}</option>
											</c:forEach>
										</select>
										<hr />
									</div>
									<label>Synchroniseer datum</label>
									<img src="./img/vraagteken.jpg" height="13" width="13"
										data-toggle="tooltip"
										title="Alle gegevens vanaf deze datum worden gesynchroniseerd" />
									<br>
									<div class="[ form-group ]" >
										<div class="input-group" data-width="60%">
											<input type='text' class="form-control" id='datetimepicker1'
												name="syncDate" value="${savedDate}" data-width="60%"/>
											<span class="input-group-addon"> <i
												class="glyphicon glyphicon-calendar"></i>
											</span>
										</div>
										<hr />
									</div>
									<label>Selecteer objecten om te importeren</label>
									<img src="./img/vraagteken.jpg" data-toggle="tooltip"
										title="Selecteer de objecten die je wilt importeren van Visma.net naar WerkbonApp"
										height="13" width="13" />
									<br>
									<div class="[ form-group ]">
										<input type="checkbox" name="importType"
											id="fancy-checkbox-default" value="employees"
											${"selected" == checkboxes.employees  ? 'checked' : ''}
											autocomplete="off" />
										<div class="[ btn-group ]">
											<label for="fancy-checkbox-default"
												class="[ btn btn-primary ]">
												<span class="[ glyphicon glyphicon-ok ]"></span> <span> </span>
											</label>
											<label for="fancy-checkbox-default"
												class="[ btn btn-default active ]"> Werknemers </label>
										</div>
									</div>
									<div class="[ form-group ]">
										<input type="checkbox" name="importType" value="materials"
											${"selected" == checkboxes.materials  ? 'checked' : ''}
											id="fancy-checkbox-primary" autocomplete="off" />
										<div class="[ btn-group ]">
											<label for="fancy-checkbox-primary"
												class="[ btn btn-primary ]">
												<span class="[ glyphicon glyphicon-ok ]"></span> <span> </span>
											</label>
											<label for="fancy-checkbox-primary"
												class="[ btn btn-default active ]"> Materialen </label>
										</div>
									</div>
									<div class="[ form-group ]" id="hourtypes">
										<input type="checkbox" name="importType" value="hourtypes"
											${"selected" == checkboxes.hourtypes  ? 'checked' : ''}
											id="hourtypesLabel" autocomplete="off" />
										<div class="[ btn-group ]">
											<label for="hourtypesLabel" class="[ btn btn-primary ]">
												<span class="[ glyphicon glyphicon-ok ]"></span> <span> </span>
											</label>
											<label for="hourtypesLabel"
												class="[ btn btn-default active ]"> Uursoorten </label>
										</div>
									</div>
									<div class="[ form-group ]" id="projects">
										<input type="checkbox" name="importType" value="projects"
											${"selected" == checkboxes.projects  ? 'checked' : ''}
											id="fancy-checkbox-success" autocomplete="off" />
										<div class="[ btn-group ]">
											<label for="fancy-checkbox-success"
												class="[ btn btn-primary ]">
												<span class="[ glyphicon glyphicon-ok ]"></span> <span> </span>
											</label>
											<label for="fancy-checkbox-success"
												class="[ btn btn-default active ]"> Projecten </label>
										</div>
									</div>
									<div class="[ form-group ]">
										<input type="checkbox" name="importType" value="relations"
											${"selected" == checkboxes.relations  ? 'checked' : ''}
											id="fancy-checkbox-warning" autocomplete="off" />
										<div class="[ btn-group ]">
											<label for="fancy-checkbox-warning"
												class="[ btn btn-primary ]">
												<span class="[ glyphicon glyphicon-ok ]"></span> <span> </span>
											</label>
											<label for="fancy-checkbox-warning"
												class="[ btn btn-default active ]"> Klanten </label>
										</div>
									</div>
									<div class="[ form-group ]">
										<input type="checkbox" name="importType" value="workorder"
											${"selected" == checkboxes.workorder  ? 'checked' : ''}
											id="workorder" autocomplete="off"/>
										<div class="[ btn-group ]">
											<label for="workorder" class="[ btn btn-primary ]">
												<span class="[ glyphicon glyphicon-ok ]"></span> <span> </span>
											</label>
											<label for="workorder" class="[ btn btn-default active ]">
												Verkooporders </label>
										</div>
									</div>
									<div class="[ form-group ]" id="salesOrderTypes" data-toggle="tooltip"
										title="Selecteer de ordersoort die geimporteerd mag worden">
										<select  name="salesOrderTypes" class="selectpicker"
											data-width="60%">
											<c:forEach items="${salesOrderTypes}" var="type">
												<option value="${type.key}"
													${type.value == 'selected' ? 'selected="selected"' : ''}>
													${type.key}</option>
											</c:forEach>
										</select>
									</div>
								</div>
							</div>
						</div>
					</div>
				</div>
				<div class="panel panel-success">
					<div class="panel-heading">Export instellingen</div>
					<div class="panel-body">
						<div class="row control-group">
							<div class="form-group col-xs-12 floating-label controls">
								<div class='[ col-xs-20 col-sm-10 ]'>
									<label>Werkbonstatus</label>
									<img src="./img/vraagteken.jpg" data-toggle="tooltip"
										title="De werkbonnen met status compleet worden opgehaald"
										height="13" width="13" />
									<br> <select name="factuurType" class="selectpicker"
										id="status" data-width="60%" required>
										<option selected value="compleet"
											${"compleet" == factuur ? 'selected="selected"' : ''}>Compleet</option>
										<option value="geen"
											${"geen" == factuur ? 'selected="selected"' : ''}>Geen</option>
									</select>
									<hr />
									<label>Werkbon opties</label>
									<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="Nieuwe materialen of relaties op een werkbon worden aangemaakt in Visma.net"
									height="13" width="13" />
									<div class="[ form-group ]">
										<input type="checkbox" value="materials" id="exportMaterial"
											${"selected" == exportCheckboxes.materials  ? 'checked' : ''}
											name="exportType" id="exportMaterial">
										<div class="[ btn-group ]">
											<label for="exportMaterial" class="[ btn btn-primary ]">
												<span class="[ glyphicon glyphicon-ok ]"></span> <span> </span>
											</label>
											<label for="exportMaterial" class="[ btn btn-default active ]">
												Nieuw materiaal </label>
										</div>
									</div>
									<div class="[ form-group ]" id="materialType" data-toggle="tooltip"
										title="Selecteer de artikeltype voor het aanmaken van een nieuw artikel">
										<select  name="materialType" class="selectpicker"
											data-width="60%">
											<c:forEach items="${materialTypes}" var="type">
												<option value="${type.key}"
													${type.value == 'selected' ? 'selected="selected"' : ''}>
													${type.key}</option>
											</c:forEach>
										</select>
									</div>
									<div class="[ form-group ]">
										<input type="checkbox" value="relations"
											${"selected" == exportCheckboxes.relations  ? 'checked' : ''}
											name="exportType" id="exportRelation">
										<div class="[ btn-group ]">
											<label for="exportRelation" class="[ btn btn-primary ]">
												<span class="[ glyphicon glyphicon-ok ]"></span> <span> </span>
											</label>
											<label for="exportRelation" class="[ btn btn-default active ]">
												Nieuwe relatie </label>
										</div>
									</div>
									<label>Werkbon versturen als</label>
									<img src="./img/vraagteken.jpg" data-toggle="tooltip"
										title="De werkbon wordt als verkoopfactuur verstuurd naar Visma.net"
										height="13" width="13" />

									<div class="funkyradio">
										<div class="funkyradio-primary">
											<input type="radio" name="exportWerkbon" id="radio1"
												value="salesorder"
												${"selected" == exportWerkbonType.salesorder  ? 'checked' : ''}
												checked />
											<label for="radio1">Verkooporder</label>
										</div>
										<div class="funkyradio-primary">
											<input type="radio" name="exportWerkbon" id="radio2"
												value="factuur"
												${"selected" == exportWerkbonType.factuur  ? 'checked' : ''} />
											<label for="radio2">Verkoopfactuur</label>
										</div>
									</div>
									<hr />
									<div id="urensectie">
										<label>Afronding uren</label>
										<img src="./img/vraagteken.jpg" data-toggle="tooltip"
											title="Selecteer het aantal minuten waarop de gewerkte uren moeten worden afgerond"
											height="13" width="13" />
										<br> <select name="roundedHours" class="selectpicker"
											id="uren" data-width="60%" required>
											<option selected value="1"
												${"1" == roundedHours ? 'selected="selected"' : ''}>Geen
												afronding</option>
											<option value="5"
												${"5" == roundedHours ? 'selected="selected"' : ''}>5
												minuten</option>
											<option value="15"
												${"15" == roundedHours ? 'selected="selected"' : ''}>15
												minuten</option>
											<option value="30"
												${"30" == roundedHours ? 'selected="selected"' : ''}>30
												minuten</option>
										</select>
									</div>
								</div>
							</div>
						</div>
					</div>
					<div id="affix" data-spy="affix">
						<input type="submit" class="btn btn-success btn-lg"
							value="Start synchronisatie" id="syncbutton" />
						<input type="submit" class="btn btn-success btn-lg"
							value="Instellingen opslaan" name="category" id="savebutton" />
						<button type="button" id="help" class="btn btn-success btn-lg">Help</button>
					</div>
					<br> <br> <br>
				</div>
			</form>
		</div>
	</div>
	<!-- this form will be validated after syncbutton is pressed  -->
	<form action="sync.do" id="sync">
		<input type="hidden" value="${softwareToken}" name="token" />
		<input type="hidden" value="${softwareName}" name="softwareName" />
	</form>
	<div class="settings">
		<div class="panel panel-success">
			<div class="panel-heading">
				Log
				<img src="./img/refresh_button.png" data-toggle="tooltip"
					title="Click this button to reload page" height="30" width="30"
					id="refresh_knop" />
			</div>
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
	<script src="vendor/jquery/jquery.min.js" type="text/javascript"></script>
	<!-- 	 <script src="http://code.jquery.com/ui/1.10.3/jquery-ui.js"></script> -->
	<!-- Bootstrap Core JavaScript -->
	<script src="vendor/bootstrap/js/bootstrap.min.js"
		type="text/javascript"></script>
	<!-- Plugin JavaScript -->
	<script
		src="https://cdnjs.cloudflare.com/ajax/libs/jquery-easing/1.3/jquery.easing.min.js"
		type="text/javascript"></script>
	<!-- Contact Form JavaScript -->
	<script src="js/jqBootstrapValidation.js" type="text/javascript"></script>
	<script src="js/contact_me.js" type="text/javascript"></script>
	<!-- Theme JavaScript -->

	<script src="js/freelancer.min.js" type="text/javascript"></script>
	<script type="text/javascript" src="js/vkbeautify.js"></script>
	<script type="text/javascript" src="js/bootstrap-datetimepicker.js"></script>
	<script type="text/javascript" src="js/visma.js"></script>
</body>
</html>
