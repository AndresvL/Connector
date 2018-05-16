var interval = null;
$(document).ready(
		function() {
			$.ajax({
				type : "GET",
				url : "ProgressBarServlet",
				success : function(data) {
					if (data === 1) {
						$('#loading').show();
						$('#loadingLabel').show();
						sync();
					}
				}
			});
			function sync() {
				interval = setInterval(function() {
					$.ajax({
						type : "GET",
						url : "ProgressBarServlet",
						success : function(data) {
							if (data === 1) {
								$('#loading').show();
								$('#loadingLabel').show();
							} else if (data === 2) {
								$('#loading').hide();
								$('#loadingLabel').hide();
								location.reload();
							} else {
								clearInterval(interval);
								$('#loading').hide();
								$('#loadingLabel').hide();
							}
						}
					});
				}, 4000)
			}
	  		$('[data-toggle="tooltip"]').tooltip(); 
	  		
			<!-- reload page every 1,5 - 2 min -->
			setTimeout(function(){
				  location.reload();
			  },300000)
			// Date
		    $('#datetimepicker1').datetimepicker({ format:'DD-MM-YYYY HH:mm:ss'
		    });
			
			var modal = document.getElementById('loginModal');
			if ($('#client').val() === "" || $('#client').val() === null) {
				modal.style.display = "block";
			}else {
				if($('#client').val() === "true"){
					swal({
						title : 'Success',
						text : "Je bent ingelogd",
						type : 'success'
					})
					$('#client').val("pending");
				}
				if ($('#saved').val() !== "" && $('#saved').val() !== false) {
					swal({
						title : 'Success',
						text : $('#saved').val(),
						type : 'success'
					})
				}
				$('#saved').val("false");
			}		
			
			$(".showDetails").click(function() {
				var errorDetails = $(this).data("href");
				if(errorDetails !== null && errorDetails !== true && errorDetails !== false && errorDetails !== ""){
					alert(errorDetails);
				} 
			});
		});
		$("#show").click(function() {
			$("#mappingTable").toggle();
		});
		$("#help").click(function() {
			var modal = document.getElementById('myModal');
			modal.style.display = "block";
			// Get the <span> element that closes the modal
			var span = document.getElementsByClassName("close")[0];
			// When the user clicks on <span> (x), close the modal
			span.onclick = function() {
				modal.style.display = "none";
			}

			// When the user clicks anywhere outside of the modal, close it
			window.onclick = function(event) {
				if (event.target == modal) {
					modal.style.display = "none";
				}
			}
		});
		$("#syncbutton")
		.click(
				function(event) {
					$.ajax({
						type : "POST",
						url : "ProgressBarServlet",
						data : "status=1",
						success : function(data) {
							startSync();
						}
					});
					event.preventDefault();
					function startSync() {
						if ($("#status").val() === "error") {
							swal(
									{
										title : 'Let op',
										text : 'Werkbonstatus staat op Error. Hierdoor zullen geen werkbonnen gesynchroniseerd worden',
										imageUrl : 'WBA.png',
										imageWidth : 250,
										imageHeight : 220,
										showConfirmButton : true,
										showCancelButton : true,
										cancelButtonText : 'Verander status'
									}).then(function() {
								$("#sync").submit();
							}, function(dismiss) {
								// dismiss can be 'cancel', 'overlay',
								// 'close', and 'timer'
								if (dismiss === 'cancel') {
									$.ajax({
										type : "POST",
										url : "ProgressBarServlet",
										data : "status=3",
										success : function(data) {

										}
									});
									$('#status').val("compleet");
									$("#saveBouwsoft").submit();
								} else {
									$.ajax({
										type : "POST",
										url : "ProgressBarServlet",
										data : "status=3",
										success : function(data) {

										}
									});
								}
							})
						} else {
							swal(
									{
										title : 'Synchroniseren',
										text : 'Op de achtergrond zal de synchronisatie plaatsvinden. Kom over een paar minuten terug',
										imageUrl : 'WBA.png',
										imageWidth : 250,
										imageHeight : 220,
										showConfirmButton : true
									}).then(function() {
								$("#sync").submit();
							}, function(dismiss) {
								// dismiss can be 'cancel', 'overlay',
								// 'close', and 'timer'
								if (dismiss === 'overlay') {
									$.ajax({
										type : "POST",
										url : "ProgressBarServlet",
										data : "status=3",
										success : function(data) {

										}
									});
								}
							})
						}
					}
				});
		$(".exportRelation").on('change', function() {
			if ($("input[name='exportRelations']:checked").val() == "yes") {
				$('#relations').attr('checked', true);
			}
		});