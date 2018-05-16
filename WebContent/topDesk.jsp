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
<title>TopDesk Connection</title>
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
</head>
<body>
	<div class="affixImg" data-spy="affix">
		<img src="./img/loading.gif" height="60" width="60" id="loading"
			style="display: none;" />
		<h6 id="loadingLabel" style="display: none;">synchronizeren...</h6>
	</div>

	<!-- Settings Section -->
	<div id="WBA-section">
		<img src="./img/werkbonapp.png" height="60" width="170" id="WBA_logo" />
		<img src="./img/logo-topdesk.png" height="60" id="boekhoud_logo" />
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
						<input type="hidden" value="TopDesk" name="softwareName"
							id="softwareName" />
						<input type="hidden" value="${clientToken}" id="client" />
						<input type="hidden" value="${operatorName}" id="operator" />
						<input type="hidden" value="${clientDomain}" id="domain" />
						<table>
							<tr>
								<th>
									<h2>Authentication</h2> klik <a href="pdf/TopDesk_manual.pdf"
									target="_blank">hier</a> om de documentatie te openen
								</th>
							</tr>
							<tr>
								<td><label>${errorMessage}</label></td>
							</tr>
							<tr>
								<td><label>API password TopDesk</label> <img
										src="./img/vraagteken.jpg" data-toggle="tooltip"
										title="Login bij TopDesk en genereer een Application Password bij de gebruikers instellingen."
										height="13" width="13" /></td>
							</tr>
							<tr>
								<td><input type="text" class="form-control"
										id="operatorName" placeholder="Beheerder username"
										name="operatorName" required /></td>
							</tr>
							<tr>
								<td><input type="text" class="form-control"
										id="clientToken" placeholder="Applicatie wachtwoord"
										name="clientToken" required /></td>
							</tr>
							<tr>
								<td><input type="text" class="form-control"
										id="clientDomain" placeholder="domein.topdesk.net"
										name="clientDomain" required /></td>
							</tr>
						</table>
						<br>
						<input type="submit" id="loginButton" value="Submit"
							class="btn btn-success btn-lg" />
					</form>
				</div>
			</div>

			<!-- The Help Modal -->
			<div id="helpModal" class="modal">
				<!-- Modal content -->
				<div class="modal-content">
					<div class="modal-header">
						<span class="close">&times;</span>
						<h2>
							Welkom <small>bij de TopDesk koppeling</small>
						</h2>
					</div>
					<div class="modal-body">
						<h3>Belangrijk</h3>

						<p>
							klik <a href="pdf/TopDesk_manual.pdf" target="_blank">hier</a> om
							de documentatie te openen
						</p>

						<br>
						<h4>Let op!</h4>
						<ul>
							<li>De koppeling importeerd alle calls met een geselecteerde
								status door als een werkbon.</li>
							<li>Als verplichte gegevens ontbreken bij een call, kan deze
								niet doorgevoerd worden naar WBA. Deze worden overgeslagen door
								de koppeling</li>
							<li>Synchroniseren van relaties is momenteel enkel mogelijk
								van TopDesk naar WBA, niet andersom.</li>
							<li>
								De status van een call wordt in TopDesk aangepast na het
								synchroniseren. Dit om te voorkomen dat dit nogmaals gebeurd. <br>
								Hiervoor moet de geselecteerde
								<mark>Sync status</mark>
								verschillen van de geselecteerde
								<mark>Processing status</mark>
							</li>
						</ul>

						<br>
						<h4>Informatie</h4>
						<ul>
							<li>
								Op deze pagina is het mogelijk om de
								<mark>import</mark>
								en
								<mark>export</mark>
								gegevens tussen WerkbonApp en TopDesk in te stellen.
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
							<li>Relaties, contactpersonen & werkbonnen worden
								geïmporteerd vanuit TopDesk.</li>
						</ul>

						<br>
						<button type="button" id="show" class="btn btn-info">Show</button>
						<h4>Data Mapping</h4>
						<div id="mappingTable" style="display: none;">
							<table class="table table-hover">
								<thead>
									<tr>
										<th>WerkbonApp</th>
										<th>TopDesk</th>
									</tr>
								</thead>
								<tbody>
									<tr>
										<td>Relaties</td>
										<td>Vestiging</td>
									</tr>
									<tr>
										<td>Contactpersoon</td>
										<td>Persoon</td>
									</tr>
									<tr>
										<td>Werkbon</td>
										<td>Melding</td>
									</tr>
									<tr>
										<td>Werknemer</td>
										<td>Behandelaar</td>
									</tr>
								</tbody>

							</table>
						</div>

					</div>
					<div class="modal-footer">
						<p>WorkOrderApp B.V.</p>
					</div>
				</div>
			</div>
			<form action="settings.do" id="saveTopDesk">
				<input type="hidden"
					value="f7dacde67bcdb6517fb231ff809c0dac00d2ee30467b6cb20915b3d99f90c796"
					name="softwareToken" id="softwareToken" />
				<input type="hidden" value="${softwareName}" name="softwareName"
					id="softwareName" />
				<input type="hidden" value="${clientToken}" id="client" />
				<input type="hidden" value="${operatorName}" id="operator" />
				<input type="hidden" value="${clientDomain}" id="domain" />

				<div class="panel panel-success">
					<div class="panel-heading" id="import-panel">Import
						instellingen</div>

					<div class="panel-body">
						<div class="row control-group">
							<div class="form-group col-xs-12 floating-label controls">
								<div class='col-sm-7'>
									<label>Synchroniseer datum</label>
									<img src="./img/vraagteken.jpg" height="13" width="13"
										data-toggle="tooltip"
										title="Alle gegevens vanaf deze datum worden gesynchroniseerd" />
									<br>
									<div class="row">
										<div class="form-group">
											<div class="input-group">
												<input type='text' class="form-control" id='datetimepicker1'
													name="syncDate"
													value="<c:choose>
													<c:when test="${not empty savedDate}">
														${savedDate}
													</c:when>
													<c:otherwise>
														${currDate}
													</c:otherwise>
													</c:choose>" />
												<span class="input-group-addon"> <i
													class="glyphicon glyphicon-calendar"></i>
												</span>
											</div>
											<hr />
										</div>
									</div>
								</div>
								<hr />
								<div class="[ col-xs-20 col-sm-10 ]">
									<label>Selecteer gegevens om te importeren</label>
									<img src="./img/vraagteken.jpg" data-toggle="tooltip"
										title="Selecteer de gegevens die je wilt importeren van TopDesk naar WerkbonApp"
										height="13" width="13" />
									<br> <br>

									<div class="[ form-group ]">
										<input type="checkbox" name="importType" value="calls"
											<c:if test="${fn:contains(importTypes, 'calls')}"> checked </c:if>
											id="fancy-checkbox-primary" autocomplete="off" />
										<div class="[ btn-group ]">
											<label for="fancy-checkbox-primary"
												class="[ btn btn-primary ]">
												<span class="[ glyphicon glyphicon-ok ]"></span> <span>
												</span>
											</label>
											<label for="fancy-checkbox-primary"
												class="[ btn btn-default active ]"> Werkbonnen </label>
										</div>
									</div>

									<div id="orderOptionsParent">
										<div class="[ form-group ]" id="statusGroup">
											<label>Processing status</label>
											<img src="./img/vraagteken.jpg" data-toggle="tooltip"
												title="Meldingen met de geselecteerde statussen zullen geimporteerd worden naar de WerkbonApp"
												height="13" width="13" />
											<br> <select name="statusGroups" class="selectpicker"
												data-width="60%" multiple="multiple" id="statusGroupPicker">

												<c:forEach items="${processingStatus}" var="type">
													<option value="${type.id}"
														<c:if test="${fn:contains(savedStatus, type.id)}">selected="selected"</c:if>>
														${type.name}</option>
												</c:forEach>

											</select>
										</div>

										<div class="[ form-group ]" id="operatorGroup">
											<label>Behandelaarsgroepen</label>
											<img src="./img/vraagteken.jpg" data-toggle="tooltip"
												title="Meldingen van deze groepen worden geimporteerd. Laat dit leeg om calls van alle groepen te importeren"
												height="13" width="13" />
											<br> <select name="operatorGroupSelect"
												class="selectpicker" data-width="60%" multiple="multiple"
												id="opGroupPicker">

												<c:forEach items="${operatorGroups}" var="opGroup">
													<option value="${opGroup.id}"
														<c:if test="${fn:contains(savedOperators, opGroup.id)}">selected="selected"</c:if>>
														${opGroup.groupName}</option>
												</c:forEach>

											</select>
										</div>
									</div>

									<input type="hidden" value="${savedOperators}" id="hidOpGroup"
										name="hidOpGroup" />
									<input type="hidden" value="${savedStatus}" id="hidStatusGroup"
										name="hidStatusGroup" />

									<div class="[ form-group ]">
										<input type="checkbox" name="importType" value="branches"
											<c:if test="${fn:contains(importTypes, 'branches')}"> checked </c:if>
											id="fancy-checkbox-warning" autocomplete="off" />
										<div class="[ btn-group ]">
											<label for="fancy-checkbox-warning"
												class="[ btn btn-primary ]">
												<span class="[ glyphicon glyphicon-ok ]"></span> <span>
												</span>
											</label>
											<label for="fancy-checkbox-warning"
												class="[ btn btn-default active ]"> Relaties </label>
										</div>
									</div>

									<div class="[ form-group ]" id="contacts">
										<div class="[ form-group ]">
											<input type="checkbox" name="importType" value="persons"
												<c:if test="${fn:contains(importTypes, 'persons')}"> checked </c:if>
												id="fancy-checkbox-contacts" autocomplete="off" />
											<div class="[ btn-group ]">
												<label for="fancy-checkbox-contacts"
													class="[ btn btn-primary ]">
													<span class="[ glyphicon glyphicon-ok ]"></span> <span>
													</span>
												</label>
												<label for="fancy-checkbox-contacts"
													class="[ btn btn-default active ]"> Contacts </label>
											</div>
										</div>
									</div>

									<div class="[ form-group ]">
										<input type="checkbox" name="importType" value="operators"
											<c:if test="${fn:contains(importTypes, 'operators')}"> checked </c:if>
											id="fancy-checkbox-default" autocomplete="off" />
										<div class="[ btn-group ]">
											<label for="fancy-checkbox-default"
												class="[ btn btn-primary ]">
												<span class="[ glyphicon glyphicon-ok ]"></span> <span>
												</span>
											</label>
											<label for="fancy-checkbox-default"
												class="[ btn btn-default active ]"> Medewerkers </label>
										</div>
									</div>
								</div>
							</div>
						</div>
					</div>
				</div>
				<div class="panel panel-success" style="clear: both;">
					<div class="panel-heading">Export instellingen</div>
					<div class="panel-body">
						<div class="row control-group">
							<div class="form-group col-xs-12 floating-label controls">
								<div class="[ form-group ]" id="completeStatus">
									<label>Compleet status</label>
									<img src="./img/vraagteken.jpg" data-toggle="tooltip"
										title="De status die een call in TopDesk moet krijgen als deze vanuit WBA compleet is gemeld"
										height="13" width="13" />
									<br> <select name="completeStatus" class="selectpicker"
										data-width="60%">

										<c:forEach items="${processingStatus}" var="type">
											<option value="${type.id}"
												${completeStatusSaved == type.id ? 'selected="selected"' : ''}>
												${type.name}</option>
										</c:forEach>

									</select>
								</div>

								<label>Werkbonstatus</label>
								<img src="./img/vraagteken.jpg" data-toggle="tooltip"
									title="De werkbonnen met status compleet worden opgehaald"
									height="13" width="13" />
								<br> <select name="factuurType" class="selectpicker"
									data-width="60%" id="status" required>
									<option selected value="compleet"
										${"compleet" == factuur ? 'selected="selected"' : ''}>Compleet</option>
									<option value="error"
										${"error" == factuur ? 'selected="selected"' : ''}>Error</option>
									<option value="geen"
										${"geen" == factuur ? 'selected="selected"' : ''}>Geen</option>
								</select>
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
				<input type="hidden" value="${saved}" id="saved" name="saved" />
				<input type="hidden" value="${errorMessage}" id="error" />
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
	<script type="text/javascript" src="js/topdesk.js"></script>

</body>
</html>