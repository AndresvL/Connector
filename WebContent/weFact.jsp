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
<title>WeFact Connection</title>
<!-- datepicker CSS -->
<link
	href="//cdn.rawgit.com/Eonasdan/bootstrap-datetimepicker/e8bddc60e73c1ec2475f827be36e1957af72e2ea/build/css/bootstrap-datetimepicker.css"
	rel="stylesheet">
<!-- Custom CSS -->
<link href="css/custom.css" rel="stylesheet">
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
<body>
	<div class="affixImg" data-spy="affix">
		<img src="./img/loading.gif" height="60" width="60" id="loading"
			style="display: none;" />
		<h6 id="loadingLabel" style="display: none;">synchronizeren...</h6>
	</div>
	<!-- Settings Section -->
	<div id="WBA-section">
		<img src="./img/werkbonapp.png" height="60" width="170" id="WBA_logo" />
		<img src="./img/logo-wefact.png" height="80" width="150"
			id="boekhoud_logo" />
	</div>
	<div class="settings">

		<div class="panel-group">
			<!-- The login Modal -->
			<div id="loginModal" class="modal">
				<!-- Modal content -->
				<div class="modal-content" id="loginContent">
					<form action="OAuth.do">
						<input type="hidden" value="${softwareToken}" name="token"
							id="softwareToken" />
						<input type="hidden" value="WeFact" name="softwareName"
							id="softwareName" />
						<input type="hidden" value="${clientToken}" id="client" />
						<table>
							<tr>
								<th>
									<h2>Authentication</h2> klik <a
									href="pdf/WeFact_documentatie.pdf" target="_blank">hier</a> om
									de documentatie te openen
								</th>
							</tr>
							<tr>
								<td><label>${errorMessage}</label></td>
							</tr>
							<tr>
								<td><label>Securitycode WeFact</label> <img
										src="./img/vraagteken.jpg" data-toggle="tooltip"
										title="Login bij WeFact en navigeer naar Instellingen - API - beveiligingscode"
										height="13" width="13" /></td>
							</tr>
							<tr>
								<td><input type="text" class="form-control"
										id="clientToken"
										placeholder="5a5a5fbbcecdd585aa62812119d0721e"
										name="clientToken" required /></td>
							</tr>
						</table>
						<br>
						<input type="submit" id="loginButton" value="Submit"
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
							Welkom <small>bij de WeFact koppeling</small>
						</h2>
					</div>
					<div class="modal-body">
						<h3>Belangrijk</h3>
						<p>
							klik <a href="pdf/WeFact_documentatie.pdf" target="_blank">hier</a>
							om de documentatie te openen
						</p>
						<br>
						<h4>Let op!</h4>
						<ul>
							<li>Bij het aanschaffen van deze koppeling is het verstandig
								om alle werkbonnen die in WerkbonApp staan op status afgehandeld
								te zetten</li>
							<li>
								Bij een <abbr>foutmelding</abbr> kan op het bericht(log) geklikt
								worden om meer details te zien.
							<li>(Nieuwe) Relaties en materialen op een werkbon worden
								geëxporteerd naar WeFact.</li>
						</ul>
						<br>

						<h4>Informatie</h4>
						<ul>
							<li>
								Op deze pagina is het mogelijk om de
								<mark>import</mark>
								en
								<mark>export</mark>
								gegevens tussen WerkbonApp en WeFact in te stellen.
							</li>
							<li>
								Elke 15 minuten zal er een
								<mark>automatische synchronisatie</mark>
								plaatsvinden aan de hand van deze instellingen.
							</li>
							<li>
								Het is mogelijk om
								<mark>handmatig een synchronisatie uit te voeren</mark>
								door onderaan op de knop Synch te klikken.
							</li>
						</ul>
						<br>

						<h4>Mogelijkheden</h4>
						<ul>
							<li>
								<abbr>Producten, debiteuren, offertes en producten in
									productengroep Uursoorten</abbr> worden geimporteerd vanuit WeFact.
							</li>
							<li>
								<abbr>Werkbonnen met nieuwe relaties en/of materialen</abbr>
								zullen geexporteerd worden, hierbij worden nieuwe relaties en/of
								materialen aangemaakt binnen WeFact voordat de werkbon
								opgestuurd wordt.
							</li>
							<li>
								Bij het openen van een werkbon kan de <abbr>werkstatus</abbr>
								aangepast worden. Deze werkstatus komt overeen met de status van
								de factuur in WeFact.
							</li>
						</ul>
						<br>
						<button type="button" id="show" class="btn btn-info">Show</button>
						<h4>Data Mapping</h4>
						<div id="mappingTable" style="display: none;">
							<table class="table table-hover">
								<thead>
									<tr>
										<th>WerkbonApp</th>
										<th>WeFact</th>
									</tr>
								</thead>
								<tbody>
									<tr>
										<th colspan="2">Import</th>
									</tr>
									<tr>
										<td>Materialen</td>
										<td>Producten</td>
									</tr>
									<tr>
										<td>Relaties</td>
										<td>Debiteuren</td>
									</tr>
									<tr>
										<td>Uursoorten</td>
										<td>Producten(Uursoorten)</td>
									</tr>
									<tr>
										<td>Offertes</td>
										<td>Werkbonnen</td>
									</tr>
									<tr>
										<th colspan="2">Export</th>
									</tr>
									<tr>
										<td>Werkbon</td>
										<td>Offerte</td>
									</tr>
									<tr>
										<td>Werkbon</td>
										<td>Factuur</td>
									</tr>
								</tbody>
							</table>
						</div>
						<br>
					</div>
					<div class="modal-footer">
						<p>WorkOrderApp B.V.</p>
					</div>
				</div>
			</div>
			<input type="hidden" value="${saved}" id="saved" name="saved" />
			<input type="hidden" value="${errorMessage}" id="error" />
			<form action="settings.do" id="saveWeFact">
				<div class="panel panel-success">
					<div class="panel-heading" id="import-panel">Import
						instellingen</div>
					<div class="panel-body">
						<div class="row control-group">
							<div class="form-group col-xs-12 floating-label controls">
								<input type="hidden" value="${softwareName}" name="softwareName" />
								<input type="hidden" value="${softwareToken}"
									name="softwareToken" />
								<label>Selecteer objecten om te importeren</label>
								<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="Selecteer de objecten die je wilt importeren van WeFact naar WerkbonApp"
									height="13" width="13" />
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
											name="importType">
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
								<div class="checkbox">
									<label>
										<input type="checkbox" value="offertes"
											${"selected" == checkboxes.offertes  ? 'checked' : ''}
											name="importType" id="offertes">
										Offertes
									</label>
								</div>

								<div class="row">
									<div class='col-sm-7'>
										<div class="form-group">
											<label>Synchroniseer datum</label>
											<img src="./img/vraagteken.jpg" height="13" width="13"
												data-toggle="tooltip"
												title="Alle gegevens vanaf deze datum worden gesynchroniseerd" />

											<input type='text' class="form-control" id='datetimepicker1'
												name="syncDate" value="${savedDate}" />

										</div>
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
								<label>Werkbonstatus</label>
								<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="De werkbonnen met status compleet worden opgehaald"
									height="13" width="13" />
								<select name="factuurType" class="form-control" id="status"
									required>
									<option selected value="compleet"
										${"compleet" == factuur ? 'selected="selected"' : ''}>Compleet</option>
									<option value="error"
										${"error" == factuur ? 'selected="selected"' : ''}>Error</option>
									<option value="geen"
										${"geen" == factuur ? 'selected="selected"' : ''}>Geen</option>
								</select> <br>
								<label>Werkbon type</label>
								<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="De werkbon wordt als factuur of offerte verstuurd naar WeFact"
									height="13" width="13" />
								<br>
								<input type="radio" name="exportWerkbon" value="factuur"
									${"selected" == exportWerkbonType.factuur  ? 'checked' : ''}
									checked>
								Factuur<br>
								<input type="radio" name="exportWerkbon" value="offerte"
									${"selected" == exportWerkbonType.offerte  ? 'checked' : ''}>
								Offerte<br><br>
								<label>Omschrijving</label>
								<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="Omschrijving van de werkperiode meesturen ja of nee?"
									height="13" width="13" />
								<br>
								<input type="radio" name="hourDescription" value="yes"
									${"selected" == hourDescription.yes  ? 'checked' : ''} checked>
								Ja<br>
								<input type="radio" name="hourDescription" value="no"
									${"selected" == hourDescription.no  ? 'checked' : ''}>
								Nee<br> <br>

								<label>Afronding uren</label>
								<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="Selecteer het aantal minuten waarop de gewerkte uren moeten worden afgerond"
									height="13" width="13" />
								<br> <select name="roundedHours" class="form-control"
									id="uren" required>
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
								</select><br>
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
	<script src="vendor/jquery/jquery.min.js" type="text/javascript"></script>
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
	<script type="text/javascript" src="js/wefact.js"></script>
</body>
</html>
