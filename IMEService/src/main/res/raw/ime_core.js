var isSupportTouch = "ontouchend" in document ? true : false;
var processbar1=$("#processbar1");
var processbar2=$("#processbar2");
var tabs = $('.tab-content');
var keyActionTimer = null;
var curKeyState = 0;
var curKeyCode = "";
var curPath = "";
var selectedPaths = [];
var selectedPathId = 0;
var fileOperItems = $('.file-oper-items');

function formatSize(size){
	if(size < 1024){
		return size + " B";
	}else if(size < (1024 * 1024)){
		return (size / 1024).toFixed(1) + " KB";
	}else if(size < (1024 * 1024 * 1024)){
		return (size / (1024 * 1024)).toFixed(1) + " MB";
	}else{
		return (size / (1024 * 1024 * 1024)).toFixed(1) + " GB";
	}
}
function parseTVData(text){
	var tv = [];
	var lines = text.split('\n');
	var offset = 0;
	while(offset < lines.length){
		var line = lines[offset].trim();
		if(line.length > 2 && line.substr(0, 1) == "[" && line.substr(line.length - 1, 1) == "]"){
			var name = line.substring(1, line.length - 1);
			var urls = [];
			offset ++;
			while(offset < lines.length){
				line = lines[offset].trim();
				if(line.length > 1){
					var c = line.substr(0, 1);
					if(c == '[') break;
					if(c != '#'){
						var p = line.indexOf('=');
						if(p > 0){
							var item = {
								"name":line.substring(0, p).trim(),
								"url": line.substring(p + 1).trim()
							};
							if(item.name.length && item.url.length) urls.push(item);
						}
					}
				}
				offset ++;
			}
			if(urls.length)tv.push({"name":name, "urls":urls});
		}else{
			offset ++;
		}
	}
	return tv;
}
function postKeyCode(keyCode){
	$.post("/key",{code:keyCode},function(data){
		console.log(data);
	});
}
function postKeyActionCode(keyCode, keyAction){
	curKeyCode = keyCode;
	curKeyState = keyAction;
	var action = function(){
		var path = keyAction == 1 ? "/keydown" : "/keyup";
		$.post(path,{code:keyCode},function(data){
			console.log(data);
			if(curKeyState == 1 && curKeyCode == keyCode){
				keyActionTimer = setTimeout(action, 100);
			}else{
				keyActionTimer = null;
			}
		});
	}
	if(keyAction == 2){
		if(keyActionTimer){
			clearTimeout(keyActionTimer);
			keyActionTimer = null;
		}
	}
	action();
}
function clickApp(id,type){
	var app=$("#app-"+id);
	if(2!=type||confirm("是否确认要卸载应用["+app.text()+"]？")){
		$.post(1==type?"/run":"/uninstall",{packageName:app.attr("data-packageName")},function(data){
			if("ok"==data&&2==type){
				setTimeout(reloadAppList,15e3);
			}
		});
	}
}
function postFileAction(action){
	if(selectedPaths.length == 0) return;

	var title = action == "copy" ? "是否确认要将所有选择的目录或者文件复制到当前目录下？" :
		        action == "cut" ? "是否确认要将所有选择的目录或者文件剪切到当前目录下？" :
		                          "是否确认要删除所有选择的目录或者文件？不可恢复！";
	if(confirm(title)){
		if(action == "delete" && !confirm("请再次确认是否要删除所有选择的目录或者文件？不可恢复！"))return;
		$.post("/file/" + action,{targetPath : curPath, paths:selectedPaths.join('|')},function(data){
			if("ok"==data){
				selectedPaths = [];
				fileOperItems.empty();
				$('.file-oper').addClass('hide');
				selectedPathId = 0;
				setTimeout(function(){
					loadFileList(curPath);
				},1000);
			}
		});
	}
}
function getDiskSpace(){
	$.get("/sdcard_stat", null, function(data){
		$('#diskSpace').html('存储总容量：' + formatSize(data.totalBytes) + '，可用容量：' + formatSize(data.availableBytes));
	});
}
function reloadAppList(){
	$.post("/apps",{system:$("#cbListSystem")[0].checked},function(data){
		var appList=$(".app-list");
		appList.empty();
		var uninstallChecked = $("#cbUninstall")[0].checked;
		var html=[];
		for(var i=0;i<data.length;i++){
			var app=data[i];
			html.push('<div class="app-item">');
			html.push('<img src="/icon/'+app.packageName+'" class="app-icon" />');
			html.push('<div class="app-name'+(app.isSysApp?" blue":"")+'" id="app-'+i+'" data-packageName="'+app.packageName+'">'+app.lable+"</div>");
			html.push('<div class="app-btn">');
			if(app.isSysApp){
				html.push('   <input type="button" value="运行" class="btn" onclick="clickApp('+i+', 1);" />');
			}else{
				html.push('   <input type="button" value="运行" class="btn1 app-btn1' + (uninstallChecked ? ' hide' : '') + '" onclick="clickApp('+i+', 1);" />');
				html.push('\t  <input type="button" value="卸载" class="btn2 app-btn1' + (uninstallChecked ? '' : ' hide') + '" onclick="clickApp('+i+', 2);" />');
			}
			html.push("</div>");
			html.push("</div>");
		}
		for(i=0;i<3;i++){
			html.push('<div class="app-item item-empty"></div>');
		}
		appList.html(html.join("\r\n"));
	});
}
function loadFileList(path){
	curPath = path;
	$('#curPath').text(curPath == '' ? '/' : curPath);
	$.get("/file/dir/" + encodeURIComponent(path),null,function(data){
		var fileList=$(".file-list");
		fileList.empty();
		var html=[];
		var fileDeleteChecked = $("#cbFileSelect")[0].checked;
		if(data.parent != undefined){
			html.push('<div class="file-item"><div class="file-icon-panel">');
			html.push('<img src="/ic_dl_folder.png" class="file-icon" onclick="loadFileList(\''+data.parent+'\');" />');
			html.push('</div><div class="file-name">..</div>');
			html.push('</div>');
		}
		for(var i=0;i<data.dirs.length;i++){
			var file=data.dirs[i];
			html.push('<div class="file-item"><div class="file-icon-panel">');
			html.push('<img src="/ic_dl_folder.png" class="file-icon" onclick="loadFileList(\''+file.path+'\');" />');
			html.push('</div><div class="file-name">'+file.name+'</div>');
			html.push('<div class="app-btn">');
			html.push('<input type="button" value="选择" class="fbtn2 app-btn1' + (fileDeleteChecked ? '' : ' hide') + '" onclick="addFile(1,\''+file.name+'\',\''+file.path+'\');" />');
			html.push("</div>");
			html.push('</div>');
		}
		for(var i=0;i<data.files.length;i++){
			var file=data.files[i];
			html.push('<div class="file-item"><div class="file-icon-panel">');
			if(file.isMedia){
				html.push('<img src="ic_dl_video.png" class="file-icon" border="0" onclick="playMedia(this)" uri="' + file.fullPath + '" />');
			}else{
				html.push('<img src="ic_dl_other.png" class="file-icon" border="0" />');
			}
			html.push('<div class="' + (file.isMedia ? 'media-size' : 'file-size') + '">' + formatSize(file.size) + '</div>');
			html.push('</div><div class="file-name">'+file.name+'</div>');
			html.push('<div class="app-btn">');
			html.push('<a href="/file/download/' + file.path + '" target="_blank" class="' + (fileDeleteChecked ? ' hide' : '') + '">');
			html.push('<input type="button" value="下载" class="fbtn1 app-btn1" />');	
			html.push("</a>");
			html.push('\t  <input type="button" value="选择" class="fbtn2 app-btn1' + (fileDeleteChecked ? '' : ' hide') + '" onclick="addFile(2,\''+file.name+'\',\''+file.path+'\');" />');
			html.push("</div>");
			html.push('</div>');
		}
		for(i=0;i<4;i++){
			html.push('<div class="file-item item-empty"></div>');
		}
		fileList.html(html.join("\r\n"));
	});
}
function addFile(type, name, path){
	for(var i=0; i<selectedPaths.length; i++){
		if(selectedPaths[i] == path) return;
	}
	selectedPaths.push(path);
	selectedPathId ++;
	var html = [];
	html.push('<div class="file-oper-item" onclick="removeFile(' + selectedPathId + ',\'' + path + '\');" id="fileOperItem' + selectedPathId + '">');
	html.push('<div class="file-oper-name">');
	html.push(type == 1 ? "目录" : "文件");
	html.push("：");
	html.push(name);
	html.push('</div><div class="file-oper-del">X</div></div>');
	fileOperItems.append(html.join(''));
	$('.file-oper').removeClass('hide');
}
function removeFile(id, path){
	var rid = -1;
	for(var i=0; i<selectedPaths.length; i++){
		if(selectedPaths[i] == path){
			rid = i;
			break;
		}
	}
	if(rid != -1){
		selectedPaths.splice(rid, 1);
	}
	$('#fileOperItem' + id).remove();
	if(selectedPaths.length == 0){
		selectedPathId = 0;
		$('.file-oper').addClass('hide');
	}
}
function loadTVList(){
	$.get("/tv.txt",null,function(text){
		var tvItems=$(".tv-items");
		tvItems.empty();
		$('#tvData').val(text);
		var data = parseTVData(text);
		var html=[];
		for(var i=0;i<data.length;i++){
			var tv=data[i];
			html.push('<div class="tv-item">');
			html.push(tv.name);
			html.push('<br />');
			for(var j=0; j<tv.urls.length; j++){
				html.push('<a class="tv-source" data-video="' + tv.urls[j].url + '" onclick="playTV(this)">' + tv.urls[j].name + '</a>');
			}
			html.push('</div>');
		}
		tvItems.html(html.join("\r\n"));
	}, "text");
}
$("#confirm").on("click",function(){
	var $input=$("#inputarea");
	var text=$input.val();
	if(""!=text){
		$input.val("");
		$.post("/text",{text:text},function(data){
			console.log(data);
		});
	}
})
$("#cbUninstall").on("click",function(){
	if(this.checked){
		$(".btn1").addClass("hide");
		$(".btn2").removeClass("hide");
	}
	else{
		$(".btn1").removeClass("hide");
		$(".btn2").addClass("hide");
	}
})
$('#btnShowTVEdit,#btnTVEdit,#btnTVCancel').on('click',function(){
	switch(this.id){
		case 'btnShowTVEdit':
			$(".tv-list").addClass("hide");
			$(".tv-edit").removeClass("hide");
			break;
		case 'btnTVCancel':
			$(".tv-edit").addClass("hide");
			$(".tv-list").removeClass("hide");
			break;
		case 'btnTVEdit':
			$.post("/tv.txt",{text:$('#tvData').val()},function(data){
				console.log(data);
				if(data == "ok"){
					alert('电视直播源已修改成功！');
					loadTVList();
				}else{
					alert('电视直播源修改失败！');
				}
				$(".tv-edit").addClass("hide");
				$(".tv-list").removeClass("hide");
			});
			break;
	}
});
$("#cbFileSelect").on("click",function(){
	if(this.checked){
		$(".fbtn1").addClass("hide");
		$(".fbtn2").removeClass("hide");
	}
	else{
		$(".fbtn1").removeClass("hide");
		$(".fbtn2").addClass("hide");
	}
})
// Tab切换 - 支持新的button.tab结构
$("button.tab, div.tab").on("click", function(){
	var o = $(this);
	$(".tab.active, .cur").removeClass("active cur");
	tabs.addClass("hidden hide");
	var tabName = o.attr('data-tab') || o.attr('data-rel');
	tabs.filter('[data-tab="' + tabName + '"]').removeClass("hidden hide");
	o.addClass('active cur');
})
$("#btnCls").on("click",function(){
	postKeyCode($(this).attr("data-key"))
})
// 方向键 - 支持长按重复发送
$(".direction-btn, .direction, #btnDel").on(isSupportTouch ? "touchstart" : "mousedown",function(){
		var o=$(this);
		$("#direction-btns").css({"background-position":o.attr("data-bp")});
		postKeyActionCode(o.attr("data-key"), 1);
		console.log("onkeydown:" + o.attr("data-key"));
})
$(".direction-btn, .direction, #btnDel").on(isSupportTouch ? "touchend" : "mouseup",function(){
		var o=$(this);
		postKeyActionCode(o.attr("data-key"), 2);
		console.log("onkeyup:" + o.attr("data-key"));
})
// 控制按钮（返回、菜单、主页、音量等）- 使用click事件发送单次按键
$(".control-btn").on("click", function(){
	var o=$(this);
	var keyCode = o.attr("data-key");
	console.log("control-btn click:" + keyCode);
	postKeyCode(keyCode);
})
$(".otherbtn").on(isSupportTouch ? "touchstart" : "mousedown", function() {
	var o = $(this);
	o.css({
		"background-position": o.attr("data-bp")
	});
	postKeyCode(o.attr("data-key"));
})
$(".direction-btn,.direction,.otherbtn").on(isSupportTouch ? "touchend touchmove" : "mouseup mousemove", function() {
	$("#direction-btns,.direction-btn,.direction,.otherbtn").css({
		"background-position": ""
	});
})
$("#cbListSystem").on("click", reloadAppList);
$("#showSettings").on("click", function() {
	$.post("/runSystem", {
		packageName: 'android.settings.SETTINGS'
	}, function(data) {
		console.log(data)
	})
})
$("#btnPlay").on("click", function() {
	var url = $('#playUrl').val();
	if (url.length > 0 && (url.indexOf('http://') == 0 
		|| url.indexOf('https://') == 0 
		|| url.indexOf('thunder://') == 0 
		|| url.indexOf('ed2k://') == 0 
		|| url.indexOf('ftp://') == 0 
		|| url.indexOf('rtmp://') == 0 
		|| url.indexOf('rtmps://') == 0
		|| url.indexOf('mms://') == 0)){
			$.post("/play", {playUrl: url, "useSystem":$('#playUseSystem')[0].checked}, function(data) {
				console.log(data)
			})
	}else{
		alert('请输入正确的网络视频地址，只支持http/ftp/thunder/ed2k/rtmp/mms。');
	}
})
function playMedia(obj){
	$.post("/play", {playUrl: $(obj).attr('uri'), "useSystem":$('#playUseSystem')[0].checked}, function(data) {
		console.log(data)
	})
}
$('#stopPlay').on("click", function(){
	$.post("/playStop",null, function(data) {
		console.log(data)
	})
});
$('#playUseSystem').on("click", function(){
	if(this.checked){
		alert('调用外部播放器不会自动结束边下边播任务，结束播放后请手动点击停止！');
	}
	var time = new Date(9998, 1,1);
	if(!this.checked)time = new Date(1900, 1, 1);
	document.cookie = "playUseSystem=" + (this.checked ? "1" : "0") + "; expires=" + time.toGMTString();
});
$(function(){
	$('#playUseSystem')[0].checked = document.cookie.indexOf('playUseSystem=1') != -1;
});

$('#speedInterval').on("change", function(){
	$.post("/changePlayFFI", {speedInterval: this.value}, function(data) {
		console.log(data)
	})
});
function playTV(o){
	$.post("/play", {playUrl: $(o).attr('data-video'), "useSystem":$('#playUseSystem')[0].checked}, function(data) {
		console.log(data)
	})
}
$("#btnClear").on("click", function() {
	if(confirm("是否要删除所有传送的文件？")){
		$.post("/clearCache", null, function(data) {
			console.log(data);
			alert("传送的文件都已清除完毕！")
		})
	}
})
$("#upfile,#upfile2,#upfile3").change(function() {
	var id = this.id;
	var formData = new FormData;
	var file = this.files[0];
	var processbar = null;
	if(id == "upfile2"){
		formData.append("path", curPath);
		processbar = processbar2;
	}else if(id == "upfile"){
		formData.append("autoInstall", $('#cbAutoInstall')[0].checked);
		formData.append("useSystem", $('#playUseSystem')[0].checked);
		processbar = processbar1;
	}
	formData.append("file", file, encodeURI(file.uploadName || file.name));
	$.ajax({
		type: "POST",
		url: id == "upfile2" ? "/file/upload" : (id == "upfile3" ? "/torrent/upload" : "/upload"),
		dataType: "json",
		data: formData,
		processData: false,
		contentType: false,
		xhr: function() {
			var xhr = $.ajaxSettings.xhr();
			if(xhr.upload){
				xhr.upload.addEventListener("progress", function(e) {
					if(processbar){
						var p = Math.floor(100 * e.loaded / e.total) + "%";
						processbar.css({
							width: p
						}).text(p);
					}
					if(e.loaded == e.total) $(id).val("");
				}, false);
			}
			return xhr;
		},
		beforeSend: function() {
			if(processbar){
				processbar.css({
					width: "1%"
				}).text("")
			}
		},
		success: function(data) {
			if(data.success){
				if(id == "upfile2"){
					loadFileList(curPath);
					alert("文件已成功上传到当前目录。");
				}else if(id == "upfile3"){
					alert("种子文件已上传并解析，请选择要播放的视频文件。");
					addTorrentItems(data);
				}else{
					if($('#cbAutoInstall')[0].checked && file.name.indexOf(".apk") != -1){
						alert("APK包已传送到TV盒子并执行安装，请留意TV屏幕的安装请求。");
					}else{
						alert("文件已成功传送到TV盒子，存储于：" + data.filePath);
					}
				}
			}else{
				alert("抱歉，文件上传失败！");
			}
		}
	});
});
$('#btnPlayTorrent').on('click', function(){
	var torrentItems = $("#torrentItems");
	var videoIndex = torrentItems.val();
	if(videoIndex != ''){
		$.post('/torrent/play', {"videoIndex":videoIndex, "useSystem":$('#playUseSystem')[0].checked}, function(data){
			console.log(data);
		});
	}
});
function addTorrentItems(data){
	var torrentItems = $("#torrentItems");
	torrentItems.empty();
	if(data.files && data.files.length){
		for(var i=0; i<data.files.length; i++){
			var f = data.files[i];
			torrentItems.append("<option value='" + f.index + "'>" + f.name + "(" + formatSize(f.size) + ")</option>");
		}
		torrentItems.val(data.files[0].index);
	}
}
function loadTorrentItems(){
	$.post('/torrent/data', null, function(data){
		if(data.success)addTorrentItems(data);
	});
}

var upgradeScript = null;

function upgrade() {
	$.get('/version', function(version){
		$('#curVer').html(version);
	});
	if(null != upgradeScript){
		document.body.removeChild(upgradeScript);
	}
	var upgradeScript = document.createElement("script");
	upgradeScript.type = "text/javascript";
	upgradeScript.src = "http://tvremoteime-1255402058.cos.ap-guangzhou.myqcloud.com/upgrade.js";
	document.body.appendChild(upgradeScript);
}
reloadAppList();
loadFileList("");
getDiskSpace();
loadTVList();
loadTorrentItems();
upgrade();
setInterval(function() {
	upgrade()
}, 18e5);

// ==================== 触摸板功能 ====================

var touchpad = {
	element: null,
	tracking: false,
	lastX: 0,
	lastY: 0,
	sensitivity: 1.5,
	fingers: 0,
	tapStartTime: 0,
	tapStartX: 0,
	tapStartY: 0,
	moveThreshold: 10,  // 移动阈值，超过此值不算点击
	hasMoved: false,
	lastMoveTime: 0,
	throttleInterval: 16  // 约60fps的节流
};

// 初始化触摸板
function initTouchpad() {
	touchpad.element = document.getElementById('touchpad');
	if (!touchpad.element) return;

	// 触摸事件
	touchpad.element.addEventListener('touchstart', onTouchpadStart, { passive: false });
	touchpad.element.addEventListener('touchmove', onTouchpadMove, { passive: false });
	touchpad.element.addEventListener('touchend', onTouchpadEnd, { passive: false });
	touchpad.element.addEventListener('touchcancel', onTouchpadEnd, { passive: false });

	// 鼠标事件 (用于PC端测试)
	touchpad.element.addEventListener('mousedown', onTouchpadStart);
	touchpad.element.addEventListener('mousemove', onTouchpadMove);
	touchpad.element.addEventListener('mouseup', onTouchpadEnd);
	touchpad.element.addEventListener('mouseleave', onTouchpadEnd);

	// 阻止右键菜单
	touchpad.element.addEventListener('contextmenu', function(e) {
		e.preventDefault();
	});

	// 鼠标按钮
	$('#mouse-left').on('click touchend', function(e) {
		e.preventDefault();
		mouseClick(0);
	});
	$('#mouse-middle').on('click touchend', function(e) {
		e.preventDefault();
		mouseClick(2);
	});
	$('#mouse-right').on('click touchend', function(e) {
		e.preventDefault();
		mouseClick(1);
	});

	// 灵敏度调节
	$('#sensitivity').on('input', function() {
		touchpad.sensitivity = parseFloat(this.value);
		$('#sensitivity-value').text(touchpad.sensitivity.toFixed(1));
	});

	console.log('Touchpad initialized');
}

// 获取事件坐标
function getEventPos(e) {
	if (e.touches && e.touches.length > 0) {
		return { x: e.touches[0].clientX, y: e.touches[0].clientY };
	} else if (e.changedTouches && e.changedTouches.length > 0) {
		return { x: e.changedTouches[0].clientX, y: e.changedTouches[0].clientY };
	} else {
		return { x: e.clientX, y: e.clientY };
	}
}

// 触摸开始
function onTouchpadStart(e) {
	e.preventDefault();
	var pos = getEventPos(e);
	touchpad.tracking = true;
	touchpad.lastX = pos.x;
	touchpad.lastY = pos.y;
	touchpad.tapStartX = pos.x;
	touchpad.tapStartY = pos.y;
	touchpad.tapStartTime = Date.now();
	touchpad.hasMoved = false;
	touchpad.fingers = e.touches ? e.touches.length : 1;
}

// 触摸移动
function onTouchpadMove(e) {
	if (!touchpad.tracking) return;
	e.preventDefault();

	// 节流
	var now = Date.now();
	if (now - touchpad.lastMoveTime < touchpad.throttleInterval) return;
	touchpad.lastMoveTime = now;

	var pos = getEventPos(e);
	var dx = (pos.x - touchpad.lastX) * touchpad.sensitivity;
	var dy = (pos.y - touchpad.lastY) * touchpad.sensitivity;

	// 检查是否超过移动阈值
	var totalMove = Math.abs(pos.x - touchpad.tapStartX) + Math.abs(pos.y - touchpad.tapStartY);
	if (totalMove > touchpad.moveThreshold) {
		touchpad.hasMoved = true;
	}

	// 更新手指数量
	if (e.touches) {
		touchpad.fingers = e.touches.length;
	}

	if (Math.abs(dx) > 0.5 || Math.abs(dy) > 0.5) {
		if (touchpad.fingers >= 2) {
			// 双指滚动
			mouseScroll(Math.round(dy));
		} else {
			// 单指移动
			mouseMove(Math.round(dx), Math.round(dy));
		}
	}

	touchpad.lastX = pos.x;
	touchpad.lastY = pos.y;
}

// 触摸结束
function onTouchpadEnd(e) {
	if (!touchpad.tracking) return;
	e.preventDefault();

	var tapDuration = Date.now() - touchpad.tapStartTime;

	// 快速点击且没有明显移动 = 点击
	if (tapDuration < 300 && !touchpad.hasMoved) {
		if (touchpad.fingers >= 2) {
			mouseClick(1);  // 双指 = 右键
		} else {
			mouseClick(0);  // 单指 = 左键
		}
	}

	touchpad.tracking = false;
	touchpad.fingers = 0;
}

// 发送鼠标移动
function mouseMove(dx, dy) {
	if (dx === 0 && dy === 0) return;
	$.post("/mouse/move", { dx: dx, dy: dy }, function(data) {
		if (data && data.indexOf('ok:') === 0) {
			var pos = data.substring(3).split(',');
			$('#mouse-pos').text('位置: ' + pos[0] + ', ' + pos[1]);
		}
	});
}

// 发送鼠标点击
function mouseClick(button) {
	$.post("/mouse/click", { button: button }, function(data) {
		console.log('click:', data);
		if (data === 'ok') {
			// 视觉反馈
			var btnId = button === 0 ? '#mouse-left' : (button === 1 ? '#mouse-right' : '#mouse-middle');
			$(btnId).css('transform', 'scale(0.95)');
			setTimeout(function() {
				$(btnId).css('transform', '');
			}, 100);
		} else if (data && data.indexOf('error:') === 0) {
			$('#adb-status').text('ADB: 未连接').removeClass('connected').addClass('disconnected');
		}
	});
}

// 发送鼠标滚动
function mouseScroll(dy) {
	if (dy === 0) return;
	$.post("/mouse/scroll", { dy: dy }, function(data) {
		console.log('scroll:', data);
	});
}

// 检查 ADB 状态
function checkAdbStatus() {
	$.post("/mouse/move", { dx: 0, dy: 0 }, function(data) {
		if (data && data.indexOf('ok:') === 0) {
			$('#adb-status').text('ADB: 已连接').removeClass('disconnected').addClass('connected');
		} else {
			$('#adb-status').text('ADB: 未连接').removeClass('connected').addClass('disconnected');
		}
	}).fail(function() {
		$('#adb-status').text('ADB: 未连接').removeClass('connected').addClass('disconnected');
	});
}

// 页面加载完成后初始化触摸板
$(document).ready(function() {
	initTouchpad();
	// 定期检查 ADB 状态
	checkAdbStatus();
	setInterval(checkAdbStatus, 10000);
});