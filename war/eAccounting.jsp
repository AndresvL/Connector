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
<title>eAccounting Connection</title>
<!-- datepicker CSS -->
<link
	href="//cdn.rawgit.com/Eonasdan/bootstrap-datetimepicker/e8bddc60e73c1ec2475f827be36e1957af72e2ea/build/css/bootstrap-datetimepicker.css"
	rel="stylesheet">
<!-- Custom CSS -->
<link href="css/custom.css" rel="stylesheet">
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
<!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries -->
<!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
<!--[if lt IE 9]>
		        <script src="https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js"></script>
		        <script src="https://oss.maxcdn.com/libs/respond.js/1.4.2/respond.min.js"></script>
		    <![endif]-->

</head>
<body>
	<!-- Settings Section -->
	<div id="WBA-section">
		<img src="./img/werkbonapp.png" height="60" width="170" />
		<img src="./img/Visma_Logo_Screen.png" height="60" width="220"
			id="boekhoud_logo" />
	</div>
	<div class="settings">
		<div class="panel-group">
			<!-- The Help Modal -->
			<div id="myModal" class="modal">
				<!-- Modal content -->
				<div class="modal-content">
					<div class="modal-header">
						<span class="close">&times;</span>
						<h2>
							Welkom <small>bij de eAccounting koppeling</small>
						</h2>
					</div>
					<div class="modal-body">
						<h4>Belangrijk</h4>
						<h5>Let op!</h5>
						<ul>
							<li>Bij het aanschaffen van deze koppeling is het verstandig
								om <mark>alle werkbonnen</mark> in WerkbonApp op <mark>status
									afgehandeld</mark> te zetten
							</li>
							<li>De <mark>import settings</mark> mogen alleen in
								eAccounting worden gewijzigd.
							</li>
							<li>Bij een <mark>foutmelding</mark> kan je op het
								bericht(log) klikken om meer details te zien.
							<li>Als een <mark>werkbon(factuur) geëxporteerd</mark> is,
								kan je op het bericht(log) klikken om meer details te zien.
							</li>
							<li>(Nieuwe) Relaties en materialen op een werkbon worden
								geëxporteerd naar eAccounting.</li>
						</ul>
						<br>

						<h4>Informatie</h4>
						<ul>
							<li>Op deze pagina is het mogelijk om de <mark>import</mark>
								en <mark>export</mark> gegevens tussen WerkbonApp en eAccounting
								in te stellen.
							</li>
							<li>Elke 15 minuten zal er een <mark>automatische
									synchronisatie</mark> plaatsvinden aan de hand van deze instellingen.
							</li>
							<li>Het is mogelijk om <mark>handmatig een
									synchronisatie uit te voeren</mark> door onderaan op de knop Synch te
								klikken.
							</li>
						</ul>
						<br>

						<h4>Mogelijkheden</h4>
						<ul>
							<li><mark>Artikelen, debiteuren, verkooporders,
									projecten en artikelen met eenheid uur</mark> worden geimporteerd
								vanuit eAccounting.</li>
							<li><mark>Werkbonnen met nieuwe relaties en/of
									materialen</mark> zullen geexporteerd worden, hierbij worden nieuwe
								relaties en/of materialen aangemaakt binnen eAccounting voordat
								de werkbon opgestuurd wordt.</li>
						</ul>
						<br>
						<button type="button" id="show" class="btn btn-info">Show</button>
						<h4>Data Mapping</h4>
						<div id="mappingTable" style="display: none;">
							<table class="table table-hover">
								<thead>
									<tr>
										<th>WerkbonApp</th>
										<th>eAccounting</th>
									</tr>
								</thead>
								<tbody>
									<tr>
										<th colspan="2">Import</th>
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
										<td>Artikelen(uur)</td>
									</tr>
									<tr>
										<td>Projecten</td>
										<td>Projecten</td>
									</tr>
									<tr>
										<td>Verkooporders</td>
										<td>Werkbonnen</td>
									</tr>
									<tr>
										<th colspan="2">Export</th>
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
			<input type="hidden" value="${errorMessage}" id="error" />
			<input type="hidden" value="${saved}" id="saved" name="saved" />
			<form action="settings.do" id="saveeAccounting">
				<div class="panel panel-success">
					<div class="panel-heading">Import instellingen</div>
					<div class="panel-body">
						<div class="row control-group">
							<div class="form-group col-xs-12 floating-label controls">
								<input type="hidden" value="${softwareName}" name="softwareName" />
								<input type=hidden value="${softwareToken}"
									name="softwareToken" />
								<label>Selecteer objecten om te importeren</label>
								<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="Selecteer de objecten die je wilt importeren van eAccounting naar WerkbonApp"
									height="13" width="13" />
								<div class="checkbox">
									<label>
										<input type="checkbox" value="materials"
											${"selected" == checkboxes.materials  ? 'checked' : ''}
											name="importType">
										Materialen/Uursoorten
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
										<input type="checkbox" value="projects"
											${"selected" == checkboxes.projects  ? 'checked' : ''}
											name="importType">
										Projecten
									</label>
								</div>
								<div class="checkbox">
									<label>
										<input type="checkbox" value="verkooporders"
											${"selected" == checkboxes.verkooporders  ? 'checked' : ''}
											name="importType" id="verkooporders">
										Verkooporders
									</label>
								</div>
								<div id="verkooporders_extra">
									<label>Selecteer een type werkzaamheid</label>
									<img src="./img/vraagteken.jpg" data-toggle="tooltip"
										title="Dit type werkzaamheid wordt meegestuurd bij het importeren van verkooporders"
										height="13" width="13" />
									<select name="typeofwork" class="form-control" id="typeofwork"
										required>
										<c:forEach items="${types}" var="type">
											<option value="${type.key}"
												${type.value == 'selected' ? 'selected="selected"' : ''}>
												${type.key}</option>
										</c:forEach>

									</select>
									<br>
									<label>Selecteer een betaalmethode</label>
									<img src="./img/vraagteken.jpg" data-toggle="tooltip"
										title="Deze betaalmethode wordt meegestuurd bij het importeren van verkooporders"
										height="13" width="13" />
									<select name="paymentmethod" class="form-control"
										id="paymentmethod" required>
										<c:forEach items="${paymentmethods}" var="payment">
											<option value="${payment.key}"
												${payment.value == 'selected' ? 'selected="selected"' : ''}>
												${payment.key}</option>
										</c:forEach>
									</select>
								</div>
								<br>
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

								<button type="button" id="help" class="btn btn-info btn-lg">Help</button>
								<input type="submit" class="btn btn-success btn-lg" value="Save"
									name="category" id="savebutton" />
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
								<img src="./img/vraagteken.jpg" height="13" width="13"
									data-toggle="tooltip"
									title="Werkbonnen met status compleet worden opgehaald" />
								<input class="form-control" type="text" disabled
									value="Compleet" />
								<input class="form-control" type="hidden" name="factuurType"
									value="Compleet" />
								<br>
								<label>Werkbon exporteren als</label>
								<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="De werkbon wordt als verkoopfactuur verstuurd naar eAccounting"
									height="13" width="13" />
								<br>
								<input type="radio" name="exportWerkbon" value="factuur"
									${"selected" == exportWerkbonType.factuur  ? 'checked' : ''}
									checked>
								Verkoopfactuur<br><br>
								<label>Afronding uren</label>
								<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="Selecteer het aantal minuten waarop de gewerkte uren moeten worden afgerond"
									height="13" width="13" />
								<br>
								<select name="roundedHours" class="form-control" id="uren"
									required>
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
								<br>
								<label>Universeel materiaal</label>
								<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="Op dit artikelnummer in eAccounting worden de onbekende materialen vanuit WerkbonApp geboekt"
									height="13" width="13" />
								<br>
								<input type="text" class="form-control" name="materialCode"
									value="${materialCode}" required>
								<br>
							</div>
						</div>
					</div>
					<div class="row">
						<div class="form-group col-xs-12">
							<input type="submit" class="btn btn-success btn-lg" value="Sync"
								id="syncbutton" />
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
	<script type="text/javascript" src="js/bootstrap-datetimepicker.js"></script>
	<script src="js/freelancer.min.js" type="text/javascript"></script>
	<script type="text/javascript" src="js/vkbeautify.js"></script>
	<script type="text/javascript" src="js/eaccounting.js"></script>
</body>
</html>
